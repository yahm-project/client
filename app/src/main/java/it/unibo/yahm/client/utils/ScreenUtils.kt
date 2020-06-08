package it.unibo.yahm.client.utils;

import android.app.Activity;
import android.view.WindowManager;

class ScreenUtils{
    companion object {

        fun setAlwaysOn(activity: Activity, enable: Boolean){
            if (enable)
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}




