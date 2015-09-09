package co.tagalong.ui;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.topsecret.androidsnap.R;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    public static final String TAG = "MainActivity";
    private Button mButtonOpenCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            String username = getIntent().getStringExtra("username");
            String key = getIntent().getStringExtra("api_key");

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sharedPref.edit();

            editor.putString(getString(R.string.preference_api_key), key);
            editor.putString(getString(R.string.preference_username), username);
            editor.commit();
            Log.d(TAG, "Shared preferences committed");
        }

        final ActionBar actionBar = getActionBar();
        setContentView(R.layout.activity_main);

        mButtonOpenCamera = (Button) findViewById(R.id.button_openCamera);
        mButtonOpenCamera.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

//    @Override
//    public void onBackPressed() {
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        intent.putExtra("EXIT", true);
//        startActivity(intent);
//    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.button_openCamera:
                Intent intent = new Intent(this, CameraActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }
}


