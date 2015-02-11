package co.tagalong.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.topsecret.androidsnap.R;
import co.tagalong.ui.util.UIUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "CameraActivity";

    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private Camera mCamera;
    private CameraPreview mPreview;

    private FrameLayout mFrameCameraPreview;
    private FrameLayout mFramePicturePreview;
    private ImageView mImageView;

    private Camera.PictureCallback mPicture;
    private static final String IMAGE_DIRECTORY_NAME = "TagAlong";

    private Uri mFileUri;

    private ImageButton mButtonTakePicture;
    private ImageButton mButtonCloseCameraPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UIUtils.hideStatusBar(this);
        setContentView(R.layout.activity_camera);

        mFrameCameraPreview = (FrameLayout) findViewById(R.id.cameraPreview_frame);
        mFramePicturePreview = (FrameLayout) findViewById(R.id.picturePreview_frame);

        mFrameCameraPreview.setVisibility(View.VISIBLE);
        mFramePicturePreview.setVisibility(View.GONE);

        mButtonTakePicture = (ImageButton) findViewById(R.id.button_takePicture);
        mButtonTakePicture.setOnClickListener(this);
        mButtonCloseCameraPicture = (ImageButton) findViewById(R.id.button_closePicturePreview);
        mButtonCloseCameraPicture.setOnClickListener(this);

        mPicture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                mCamera.stopPreview();
                mButtonTakePicture.setVisibility(View.GONE);
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

        try {
            mCamera = Camera.open();
            mPreview.setCamera(mCamera);
            mCamera.startPreview();

        } catch(Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        if(mCamera != null) {
            mCamera.stopPreview();
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
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
                mCamera.takePicture(null, null, mPicture);
                break;
            case R.id.button_closePicturePreview:
                Log.d(TAG, "Close picture preview button pressed");
                resetCameraPreview();
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

    private void resetCameraPreview() {
        try {
            mCamera.startPreview();
            mFramePicturePreview.setVisibility(View.GONE);
            mButtonTakePicture.setVisibility(View.VISIBLE);

        } catch(Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG).show();
        }
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
        } else {
            return null;
        }

        return mediaFile;
    }
}
