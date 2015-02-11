package co.tagalong.ui.util;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by piedt on 2/8/15.
 */
public class UIUtils {

    private static String TAG = "UIUtils";
    public static void hideStatusBar(Context context) {
        Activity activity = (Activity) context;

        if(Build.VERSION.SDK_INT < 16) {
            // android version lower than Jellybean
           activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = activity.getWindow().getDecorView();
            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            // Remember that you should never show the action bar if the
            // status bar is hidden, so hide that too if necessary.
            ActionBar actionBar = activity.getActionBar();
            if(actionBar != null) {
                actionBar.hide();
                Log.d(TAG, "Hiding action bar");
            }
        }
    }
}
