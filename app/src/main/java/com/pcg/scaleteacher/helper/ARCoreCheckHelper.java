package com.pcg.scaleteacher.helper;

import android.app.Activity;
import android.app.Presentation;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.pcg.scaleteacher.main.MoveMeasureActivity;

public class ARCoreCheckHelper {

    final String TAG = "BASIC_CHECK";
    private boolean isInstallRequested;
    private Exception exception;
    private String msg;

    public ARCoreCheckHelper() {
        isInstallRequested = false;
        exception =null;
        msg =null;
    }

    //Pre-check for ARCore; use this when the application starts.
    public static void PreCheckARCore(Context context) {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(context);
        if (availability.isTransient()) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    PreCheckARCore(context);
                }
            }, 200);
        }

        //Determine to do something according to the availability. 根据支持情况来决定操作
        /* if (availability.isSupported()) {
            mArButton.setVisibility(View.VISIBLE);
            mArButton.setEnabled(true);
        } else { // The device is unsupported or unknown.
            mArButton.setVisibility(View.INVISIBLE);
            mArButton.setEnabled(false);
        } */
    }

    //Formal check for ARCore; use this when the certain activity starts.
    public Session CheckARCore(Activity activity, Session session) {
        //初始化Session
        if (session==null){
            try {
                //判断是否安装ARCore
                switch (ArCoreApk.getInstance().requestInstall(activity,!isInstallRequested)){
                    case INSTALL_REQUESTED:
                        isInstallRequested=true;
                        break;
                    case INSTALLED:
                        Log.i(TAG,"ARCore has been installed");
                        break;
                }
                session=new Session(activity);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                msg = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                msg = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                msg = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                msg = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                msg = "Failed to create AR session";
                exception = e;
            }
            //有异常说明不支持或者没安装ARCore
            if (msg != null) {
                Log.e(TAG, "Exception creating session", exception);
                return null;
            }
        }
        //该设备支持并且已安装ARCore
        try {
            //Session 恢复resume状态
            session.resume();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available. Please restart the app.");
            session = null;
        }
        return session;
    }
}
