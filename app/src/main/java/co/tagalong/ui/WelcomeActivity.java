package co.tagalong.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.topsecret.androidsnap.R;

import co.tagalong.ui.util.APIUtils;

public class WelcomeActivity extends ActionBarActivity implements View.OnClickListener{

    public static final String TAG = "WelcomeActivity";
    private Button mButtonLogin;
    private Button mButtonSignUp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String key = sharedPrefs.getString(getString(R.string.preference_api_key), "default");
        String username = sharedPrefs.getString(getString(R.string.preference_username), "default");

        Log.d(TAG, "username = " + username + " & key = " + key);
        if(!key.equals("default") && !username.equals("default")) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }

        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.hide();

        setContentView(R.layout.activity_welcome);

        mButtonLogin = (Button) findViewById(R.id.button_login);
        mButtonLogin.setOnClickListener(this);
        mButtonSignUp = (Button) findViewById(R.id.button_signUp);
        mButtonSignUp.setOnClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_welcome, menu);
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
            case R.id.button_login:
                Intent intentLogin = new Intent(this, LoginActivity.class);
                startActivity(intentLogin);
                break;
            case R.id.button_signUp:
                Intent intentSignUp = new Intent(this, SignUpActivity.class);
                startActivity(intentSignUp);
                break;
            default:
                break;
        }
    }
}
