package co.tagalong.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.topsecret.androidsnap.R;
import co.tagalong.ui.util.UIUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "CameraActivity";

    public static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private Animation anim;

    private Camera mCamera;
    private CameraPreview mPreview;

    private boolean longPress = false;

    private Handler handler = new Handler();
    private FrameLayout mFrameCameraPreview;
    private FrameLayout mFrameCameraButtons;
    private FrameLayout mFramePicturePreview;
    private ImageView mImageView;

    private ProgressBar mProgressBar;

    private int progressStatus = 0;

    private MediaRecorder mMediaRecorder;

    private Camera.PictureCallback mPicture;
    private static final String IMAGE_DIRECTORY_NAME = "TagAlong";

    private Uri mFileUri;
    private File mFile;

    private Thread mProgressThread;
    private ImageButton mButtonTakePicture;
    private ImageButton mButtonCloseCameraPicture;
    private ImageButton mButtonFlashOn;
    private ImageButton mButtonFlashOff;
    private ImageButton mButtonCameraRear;
    private ImageButton mButtonCameraFront;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UIUtils.hideStatusBar(this);
        setContentView(R.layout.activity_camera);

        mFrameCameraPreview = (FrameLayout) findViewById(R.id.cameraPreview_frame);
        mFrameCameraButtons = (FrameLayout) findViewById(R.id.frame_cameraButtons);
        mFramePicturePreview = (FrameLayout) findViewById(R.id.picturePreview_frame);

        anim = AnimationUtils.loadAnimation(this, R.anim.scale_larger);

        resetProgressThread();

        mProgressBar = (ProgressBar) findViewById(R.id.video_progressBar);
        mProgressBar.setVisibility(View.GONE);
        mFrameCameraPreview.setVisibility(View.VISIBLE);
        mFrameCameraButtons.setVisibility(View.VISIBLE);
        mFramePicturePreview.setVisibility(View.GONE);

        mButtonTakePicture = (ImageButton) findViewById(R.id.button_takePicture);
        mButtonTakePicture.setOnClickListener(this);
        mButtonTakePicture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if((event.getAction() == MotionEvent.ACTION_UP) && longPress) {

                    Log.d(TAG, "Camera button was released");
                    longPress = false;
                    mProgressBar.setVisibility(View.GONE);
                    try {
                        mMediaRecorder.stop();
                        if(mProgressThread != null) {
                            mProgressThread.interrupt();
                            mProgressThread = null;
                        }
                        releaseMediaRecorder();
                        mCamera.lock();

                        Uri.fromFile(mFile);
                        Intent intent = new Intent(getApplicationContext(), VideoPreviewActivity.class);
                        intent.putExtra("uri", Uri.fromFile(mFile).toString());
                        startActivity(intent);
                        return true;
                    } catch(RuntimeException stopExecution) {
                        Log.d(TAG, "Error stopping media recorder");
                        releaseMediaRecorder();
                        mCamera.lock();
                    }

                    return true;
                }
                return false;
            }
        });

        mButtonTakePicture.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                longPress = true;

                mProgressBar.setVisibility(View.VISIBLE);
                // initialize video camera
                if(prepareVideoRecorder()) {
                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mMediaRecorder.start();
                    mProgressThread.start();

                    return true;
                } else {
                    releaseMediaRecorder();
                }
                return false;
            }
        });
        mButtonCloseCameraPicture = (ImageButton) findViewById(R.id.button_closePicturePreview);
        mButtonCloseCameraPicture.setOnClickListener(this);
        mButtonFlashOn = (ImageButton) findViewById(R.id.button_flashOn);
        mButtonFlashOn.setOnClickListener(this);
        mButtonFlashOn.setVisibility(View.GONE);
        mButtonFlashOn.setEnabled(false);
        mButtonFlashOff = (ImageButton) findViewById(R.id.button_flashOff);
        mButtonFlashOff.setOnClickListener(this);
        mButtonCameraRear = (ImageButton) findViewById(R.id.button_cameraRear);
        mButtonCameraRear.setOnClickListener(this);
        mButtonCameraFront = (ImageButton) findViewById(R.id.button_cameraFront);
        mButtonCameraFront.setOnClickListener(this);
        mButtonCameraFront.setVisibility(View.GONE);
        mButtonCameraFront.setEnabled(false);

        mPicture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                mCamera.stopPreview();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mPreview.setFlash(false);
                mButtonTakePicture.setVisibility(View.GONE);
                mFrameCameraButtons.setVisibility(View.GONE);
                mFramePicturePreview.setVisibility(View.VISIBLE);
            }
        };
        mPreview = new CameraPreview(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview_frame);
        preview.addView(mPreview);
        preview.setKeepScreenOn(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        UIUtils.hideStatusBar(this);
        mProgressBar.setVisibility(View.GONE);
        mProgressBar.setProgress(0);
        resetProgressThread();
        try {
            mCamera = Camera.open();
            Log.d(TAG, "Camera opened");
            mPreview.setCamera(mCamera);
            Log.d(TAG, "Camera set");
            mCamera.startPreview();
            Log.d(TAG, "Preview started");

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        releaseCamera();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch(id){
            case R.id.button_takePicture:
                Log.d(TAG, "Picture button pressed");
                if(mButtonFlashOn.getVisibility() == View.VISIBLE) {
                    mPreview.setFlash(true);
                }
                mCamera.takePicture(null, null, mPicture);
                break;
            case R.id.button_closePicturePreview:
                Log.d(TAG, "Close picture preview button pressed");
                resetCameraPreview();
                break;
            case R.id.button_flashOn:
                mButtonFlashOn.setVisibility(View.GONE);
                mButtonFlashOn.setEnabled(false);
                mButtonFlashOff.setVisibility(View.VISIBLE);
                mButtonFlashOff.setEnabled(true);
                // mPreview.setFlash(false);
                break;
            case R.id.button_flashOff:
                mButtonFlashOff.setVisibility(View.GONE);
                mButtonFlashOff.setEnabled(false);
                mButtonFlashOn.setVisibility(View.VISIBLE);
                mButtonFlashOn.setEnabled(true);
                //mPreview.setFlash(true);
                break;
            case R.id.button_cameraRear:
                mButtonCameraRear.setVisibility(View.GONE);
                mButtonCameraRear.setEnabled(false);
                mButtonCameraFront.setVisibility(View.VISIBLE);
                mButtonCameraFront.setEnabled(true);
                switchToFrontCamera();
                break;
            case R.id.button_cameraFront:
                mButtonCameraFront.setVisibility(View.GONE);
                mButtonCameraFront.setEnabled(false);
                mButtonCameraRear.setVisibility(View.VISIBLE);
                mButtonCameraRear.setEnabled(true);
                switchToRearCamera();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Override back button so that when it's pressed, and the image preview is being
        // shown, it goes back to the camera preview.
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(mFramePicturePreview.getVisibility() == View.VISIBLE) {
                resetCameraPreview();
                return true;
            } else {
                finish();
                return true;
            }

        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void releaseCamera() {
        if(mCamera != null) {
            mCamera.stopPreview();
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private void resetCameraPreview() {
        try {
            mCamera.startPreview();
            mFramePicturePreview.setVisibility(View.GONE);
            mFrameCameraButtons.setVisibility(View.VISIBLE);
            mButtonTakePicture.setVisibility(View.VISIBLE);

        } catch(Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG).show();
        }
    }

    private void switchToFrontCamera() {
        int cameraId = findFrontFacingCamera();
        if(cameraId >= 0) {
            releaseCamera();

            try {
                mCamera = Camera.open(cameraId);
                mPreview.setCamera(mCamera);
                mPreview.refreshCamera();
                mCamera.startPreview();
                Log.d(TAG, "Switched to front facing camera");

            } catch(Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Front camera not found", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void switchToRearCamera() {
        if(mCamera != null) {
            releaseCamera();

            try{
                mCamera = Camera.open(0);
                mPreview.setCamera(mCamera);
                mPreview.refreshCamera();
                mCamera.startPreview();
            } catch(Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Rear camera not found", Toast.LENGTH_LONG).show();
            }
        }
    }
    private int findFrontFacingCamera() {
        int cameraId = -1;

        int numberOfCameras = Camera.getNumberOfCameras();
        for(int i =0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    private void releaseMediaRecorder() {
        if(mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }

    private boolean prepareVideoRecorder() {
        mMediaRecorder = new MediaRecorder();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(findFrontFacingCamera(), info);

        if(mButtonCameraFront.getVisibility() == View.VISIBLE) {
            mMediaRecorder.setOrientationHint(270);
        } else {
            mMediaRecorder.setOrientationHint(90);
        }
        mMediaRecorder.setMaxDuration(10000);

        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = params.getSupportedPreviewSizes();

        Camera.Size optimalSize = getBestPreviewSize(mPreview.getWidth(), mPreview.getHeight(), mSupportedPreviewSizes);
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;

        params.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mCamera.setParameters(params);

        // Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Set a CamcorderProfile
        mMediaRecorder.setProfile(profile);

        // Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        mMediaRecorder.setVideoFrameRate(30);

        // Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch(IOException e){
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private Camera.Size getBestPreviewSize(int width, int height, List<Camera.Size> previewSizes) {
        Camera.Size result = null;

        UIUtils.hideStatusBar(this);
        Display display = getWindowManager().getDefaultDisplay();
        Point s = new Point();
        display.getSize(s);

        int screenWidth = s.x;
        int screenHeight = s.y;

        Log.d(TAG, "Screen size = " + screenWidth + " x " + screenHeight);
        Log.d(TAG, "View size = " + width + " x " + height);
        for(Camera.Size size : previewSizes){
            if (size.width<=screenHeight && size.height<=screenWidth) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;

                    if (newArea>resultArea) {
                        result=size;
                    }
                }
            }
        }

        Log.d(TAG, "Size chosen = " + result.width + " x " + result.height);
        return result;
    }

    private void resetProgressThread() {
        mProgressThread = new Thread(new Runnable() {
            boolean shouldContinue = true;
            public void run() {
                mProgressBar.setProgress(0);
                progressStatus = 0;
                while((progressStatus < 100)) {
                    progressStatus += 1;

                    handler.post(new Runnable() {
                        public void run() {
                            mProgressBar.setProgress(progressStatus);
                        }
                    });

                    try {
                        Thread.sleep(100);
                    } catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    class  BitmapWorkerTask extends AsyncTask<byte[], Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private byte[] data = null;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(byte[]... params){
            data = params[0];
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            return rotated;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if(imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    public File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY_NAME);

        if(!mediaStorageDir.exists()) {
            if(!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Failed to create " + IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        }else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        mFile = mediaFile;
        return mediaFile;
    }
}
