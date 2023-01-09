package com.pcg.scaleteacher.base;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Toast;

import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.helper.ScreenReaderCheckHelper;
import com.pcg.scaleteacher.slam.GLView;

import java.util.HashMap;
import java.util.Locale;

import cn.easyar.CalibrationDownloadStatus;
import cn.easyar.CalibrationDownloader;
import cn.easyar.CameraDevice;
import cn.easyar.Engine;
import cn.easyar.FunctorOfVoidFromCalibrationDownloadStatusAndOptionalOfString;
import cn.easyar.ImmediateCallbackScheduler;
import cn.easyar.MotionTrackerCameraDevice;

/* 该类会试图实现所有的功能，其他具体Activity继承该类时会删减功能 */
public class CompletedFunctionBase extends ConstantBase implements TextToSpeech.OnInitListener {

    //当前活动的情景信息（将通过读取intent的附带信息确定）
    protected int currentBasicMode;
    protected int currentStudyContent;
    protected int currentMeasureMethod;

    //计时相关变量，用于检测各种动作是否达到了一定时间
    private Handler timeHandler;
    private Runnable timeRunnable;
    private static final int timerInterval = 50;    //每50毫秒启动一次定时任务

    //语音提示功能模块
    protected TextToSpeech tts;

    //振动功能模块
    protected Vibrator vibrator;
    protected boolean isVibrating;
    private Handler vibrationHandler;


    //需要额外启动的功能模块
    public static class FunctionType {
        public static final int UNKNOWN = 0;
        public static final int ALL = 1;
        public static final int SCREEN_TOUCHING = 2;    //屏幕手指
        public static final int MOTION_TRACKING = 3;    //运动跟踪（基于EasyAR）
        public static final int SINGLE_VISION = 4;      //单目视觉
    }
    protected int neededFunction = FunctionType.UNKNOWN;

    protected boolean hasScreenReader;      //指示当前是否开启了读屏（读屏会阻碍触摸时间正常运行，因此需要判断并进行特别处理）

    //手势检测所需要的相关变量
    protected FingerPosition currentFinger1;   //第一根手指当前位置
    protected FingerPosition currentFinger2;   //第二根手指当前位置
    protected FingerPosition originalFinger1;   //第一根手指原始位置
    protected FingerPosition originalFinger2;   //第二根手指原始位置
    protected int holdingTime = 0;      //手指在某一个位置上保持不动时间计数
    protected boolean isHolding;        //指示是否开始holdingTime计数
    protected int fingerCounter = 0;    //手指（不包括无障碍模式下额外使用的手指）个数
    protected static final int holdingPositionLimit = 100;   //判定未发生明显移动的距离要求
    protected static final int sweepPositionLimit = 200;    //判定下滑的距离要求
    protected static final int holdingTimeLimitA = (int) (3000 / timerInterval);    //时间计数要求A = 3000ms，启动手指测量的时间要求
    protected static final int holdingTimeLimitB = (int) (5000 / timerInterval);    //时间计数要求B = 5000ms，启动单手/双手/躯体移动测量的时间要求
    protected static final int holdingTimeLimitC = (int) (3000 / timerInterval);    //时间计数要求C = 3000ms，确认当前测距结果的时间要求
    protected static final int holdingTimeLimitD = (int) (5000 / timerInterval);    //时间计数要求D = 5000ms，矫正阶段保持稳定的时间要求

    //屏幕手指测量功能所需要的相关变量
    protected FingerPosition fingerPosition1;
    protected FingerPosition fingerPosition2;
    protected int dpi;
    public static final float fingerToleranceA = 0.5f;    //不超过此误差，就认为满足要求了）
    public static final float fingerToleranceB = 1.5f;    //超过此误差，就认为差得有点多了

    //运动跟踪功能需要的相关变量
    protected static String key = "kj+cN5YshCuOShdS83GW7ZDmU61NyOdUPoG0TKINqhyWHawBohC7TO1cowKuS/5e4Ub8LuZI/EC0EaJM+1yiD6QKqhycG7Yns1z1X/tcowe0G6Edsg3tVIwF7QyiEKsCsjerHfVElDP7XLkPpReuAKMN7VSMXKwBuhO6AL4KtkyKUu0eux+7CLgMoh31RJRMoBehCrgJvEz7XKIPtFySQvUToAqiEqod9USUTKQboR2yUIYDthmqOqUfrAW+EKhM+1y8C7kNqkCUEqAbsyyqDbgZoQejF6AA9VLtHbIQvAv5LKoNuAyrB7kZ7UL1DaoApBvhIbUUqg2jKr0PtBWmALBc40ykG6EdslCcG6UYrg2yKr0PtBWmALBc40ykG6EdslCcHrYMvAuEDq4avh+jI7YO7UL1DaoApBvhI7gKpgG5Kr0PtBWmALBc40ykG6EdslCLC7kNqj2nH7sHthKCD6dc40ykG6EdslCML5MqvQ+0FaYAsFySQvUbtx6+DKo6vhOqPaMfoh71RKEbuxLjTL4NgwG0H6NM7RiuAqQbskKsXK0buRqjC54avEztJe0NuBPhHrQZ4R20H6MLoxuuDb8bvUyKUu0YtgymD7kKvEztJe0NuBOiG7kXuxf1I+NMpxKuGrERvQOkXPU19R+hCqURpgr1I+NMuhGrG7sbvEztJe0dshC8C/k3og+wG5scth2kB7kZ7UL1DaoApBvhLbsRugqFG6wBsBCmGr4RoUz7XLwLuQ2qQIUbrAGlGqYAsFzjTKQboR2yUIAMvRusGoMMrg28F6EJ9VLtHbIQvAv5LbocsR+sC4MMrg28F6EJ9VLtHbIQvAv5Lb8PpQ2qPacfuwe2EoIPp1zjTKQboR2yUIIBoxegAIMMrg28F6EJ9VLtHbIQvAv5OqoApBucHrYKpg+7M64e9VLtHbIQvAv5PY4qgwyuDbwXoQn1I+NMsga/B6Ubmwe6G5wathO/TO0QugK7Uu0HpDKgDbYS7VSxH6MdsgPjFfUcugCzEqonsw3tVIxc7TP7XLkPpReuAKMN7VSMXKwBuhO6AL4KtkyKUu0eux+7CLgMoh31RJRMvhG8TIpS7QO4GroCsg3tVIxcvAu5DapAnhOuCbIqvQ+0FaYAsFzjTKQboR2yUIwCuAurPLIdoAm5F7sHuBDtQvUNqgCkG+E8sh2gHLMXoQn1Uu0dshC8C/kxrQSyHbs6pR+sBb4QqEz7XLwLuQ2qQIQLvQi2Hao6pR+sBb4QqEz7XLwLuQ2qQIQOrhykG5wetgqmD7szrh71Uu0dshC8C/kzoBq+EaE6pR+sBb4QqEz7XLwLuQ2qQJMboR2yLb8PoxeuApofv0z7XLwLuQ2qQJQ/izqlH6wFvhCoTIpS7QuvDqYcsiqmA7Ituw+6Du1UuQujAvtcph2bEawPu1z1CLYSvAuqI7J45/UTMdos/ZxNl3SuEOyeYqcSjv71OCIrnzGsxIQRrrKNVsa2fx0EyMyqOV+wKtQxUsBCyvS/THZr42L1rTi63KT7aw/ZIP/PjyxMtimvdW+4Z7F2CWbLLh934iQyEGn98kbZ5Z8vZTHuMYGALezVAyfE2qbY6N9uv0dAQGQpYtnSDW1dfeOP2yQu3j3Y6nSF1Wm1IiT+kDWDGNoQTwjC+ciQZjQzuDMHpscMbsjPjoqKfsp12+mDLEkAuihzeCYr4ltW4afef1J7+Bnx+mR+qoZGLvgRJvoeCsHrl8bkDUfU5DRhZasr/ageJUATkjkm3V9xjrj2HPEYLfvXfs9u";
    protected GLView glView;
    protected CalibrationDownloader downloader;
    protected SpatialPoseList recentPoses;  //缓存最近的几次运动跟踪结果，以减少误差
    protected SpatialPose originalPose;     //运动跟踪原始值（主要用于判断用户运动是否停下来了）
    protected SpatialPose currentPose;      //当前运动跟踪
    protected SpatialPose spatialPose1;
    protected SpatialPose spatialPose2;
    protected boolean isRealTimeTracking;   //开启之后，会不断向recentPose内置入当前跟踪的位姿
    protected int steadyTime = 0;   //在某一个位姿保持不动时间计数
    protected boolean isSteady;     //指示是否开始steadyTime计数
    //运动跟踪的误差实际上不是一个常数，需要Activity自己实现计算
    protected static final int steadyTImeLimitA = (int) (3000 / timerInterval);     //时间计数要求A = 5000ms，判定手机不再明显移动的时间要求
    protected float spatialToleranceA = 5f;
    protected float spatialToleranceB = 15f;

    public enum ActivationState {
        UNACTIVATED,    //未激活任何功能
        FINGER_ACTIVATING,      //手指在原处一直未发生明显移动，等待一定时间后将激活单指/双指测量
        MOVE_ACTIVATING,        //手指发生过移动并在新位置停留，等待一定时间后将激活,
        ACTIVATED       //成功激活了某一测量功能
    }
    protected ActivationState currentActivationState = ActivationState.UNACTIVATED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initTextToSpeech();
        initVibrator();
        identifyFunctionType();    //识别需要相应的功能模块
        initTimer();        //启动伪定时器
        initFingerDetection();      //初始化手势检测相关的变量
        initScreenTouching();       //按需启动屏幕触摸测距功能
        //initMotionTracking();     由于涉及获取界面元素，运动跟踪功能必须由具体Activity设置好布局之后再启动
        hasScreenReader = ScreenReaderCheckHelper.check(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

//        // 请求相机使用权限
//        if (!CameraPermissionHelper.hasCameraPermission(this)) {
//            CameraPermissionHelper.requestCameraPermission(this);
//        }

        if (glView != null) {
            glView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (glView != null) {
            glView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        if (downloader != null) {
            downloader.dispose();
            downloader = null;
        }

        if (timeHandler != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // setLanguage设置语言
            int result = tts.setLanguage(Locale.CHINESE);   //网上很多资料都用的是Locale.CHINA，会导致无法正常播放，原因不明
            // TextToSpeech.LANG_MISSING_DATA：表示语言的数据丢失
            // TextToSpeech.LANG_NOT_SUPPORTED：不支持
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "数据丢失或不支持", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int index = e.getActionIndex();
        switch (e.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                return onFirstFingerDown(e);
            case MotionEvent.ACTION_POINTER_DOWN:
                return onMoreFingersDown(e);
            case MotionEvent.ACTION_UP:
                return onAllFingersUp(e);
            case MotionEvent.ACTION_POINTER_UP:
                return onCertainFingerUp(e);
            case MotionEvent.ACTION_MOVE:
                return onFingerMove(e);
            default:
                break;
        }
        return super.onTouchEvent(e);
    }

    //接下来是一系列关于触摸事件处理的函数，需要由各个具体Activity去实现
    protected boolean onFirstFingerDown(MotionEvent e) {return false;}  //当第一根手指放下时触发
    protected boolean onMoreFingersDown(MotionEvent e) {return false;}  //当更多手指放下时触发
    protected boolean onAllFingersUp(MotionEvent e) {return false;}  //当手指全部抬走时触发
    protected boolean onCertainFingerUp(MotionEvent e) {return false;}  //当某一根手指抬走时触发
    protected boolean onFingerMove(MotionEvent e) {return false;}  //当手指移动时触发


    protected void initTextToSpeech() {
        tts = new TextToSpeech(this, this);
        tts.setPitch(1.0f);
        tts.setSpeechRate(1f);
    }

    private void initVibrator() {
        vibrator = (Vibrator) this.getSystemService(CompletedFunctionBase.VIBRATOR_SERVICE);
        isVibrating = false;
        vibrationHandler = new Handler();
    }

    protected void startVibrator() {
        if (!isVibrating) {
            isVibrating = true;
            if (vibrator == null)
                tts.speak(getString(R.string.vibrator_problem), TextToSpeech.QUEUE_FLUSH, null, "vibratorProblem");
            else
                vibrator.vibrate(1000);
            vibrationHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isVibrating = false;
                }
            }, 1000);
        }

    }

    private void identifyFunctionType() {
        //根据intent附带的extra信息鉴别需要的功能模块
        Intent intent = getIntent();
        currentBasicMode = intent.getIntExtra(basicModeTag, BasicMode.FORMAL_STYLE);
        currentStudyContent = intent.getIntExtra(studyContentTag, StudyContent.STUDY_SIZE);
        currentMeasureMethod = intent.getIntExtra(sizeMeasureMethodTag, SizeMeasureMethod.SINGLE_FINGER);

        //自由教学和测一测模式下需要所有附加功能开启
        if (currentBasicMode == BasicMode.FREE_STYLE
                || currentBasicMode == BasicMode.TEST)
            neededFunction = FunctionType.ALL;
            //正式教学模式下，如果是进行尺寸学习，需要根据尺寸学习方式启动不同的功能
        else if (currentBasicMode == BasicMode.FORMAL_STYLE
                && currentStudyContent == StudyContent.STUDY_SIZE) {
            switch (currentMeasureMethod) {
                //单指和双指需要启动屏幕触摸功能
                case SizeMeasureMethod.SINGLE_FINGER:
                case SizeMeasureMethod.TWO_FINGERS:
                    neededFunction = FunctionType.SCREEN_TOUCHING;
                    break;
                //单手测距和躯体移动测距需要启动运动跟踪功能
                case SizeMeasureMethod.ONE_HAND:
                case SizeMeasureMethod.BODY:
                    neededFunction = FunctionType.MOTION_TRACKING;
                    break;
                //双手测距需要启动单目视觉功能
                case SizeMeasureMethod.TWO_HANDS:
                    neededFunction = FunctionType.SINGLE_VISION;
                    break;
                //其他情形不需要启动额外功能
                default:
                    break;
            }
        }
    }

    private void initTimer() {
        timeHandler = new Handler();
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                onTimerTick();      //需要执行的定时任务，由具体Activity重写
                timeHandler.postDelayed(timeRunnable, timerInterval);
            }
        };
        timeHandler.postDelayed(timeRunnable, 0);
    }

    protected void onTimerTick() {
        if (isHolding)
            holdingTime++;
        if (isSteady)
            steadyTime++;
        if (isRealTimeTracking && glView != null && recentPoses != null) {
            recentPoses.addPose(new SpatialPose(glView.getTranslation(), glView.getQuaternion()));
        }
    }

    private void initFingerDetection() {
        currentFinger1 = new FingerPosition();
        currentFinger2 = new FingerPosition();
        originalFinger1 = new FingerPosition();
        originalFinger2 = new FingerPosition();
        holdingTime = 0;
        fingerCounter = 0;
    }

    //holdingTime开始计数
    protected void startHolding() {
        isHolding = true;
        holdingTime = 0;
    }

    //holdingTime结束计数
    protected void endHolding() {
        isHolding = false;
        holdingTime = 0;
    }

    //启动屏幕触碰功能的函数（由本基类实现即可）
    protected void initScreenTouching() {
        if (neededFunction != FunctionType.ALL && neededFunction != FunctionType.SCREEN_TOUCHING)
            return;
        fingerPosition1 = new FingerPosition();
        fingerPosition2 = new FingerPosition();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        dpi = metrics.densityDpi;
    }


    //启动运动跟踪功能的函数（必须在具体Activity设置好布局之后再调用！）
    protected void initMotionTracking() {
        //如果不需要运动跟踪功能，则不启用
        if (neededFunction != FunctionType.ALL && neededFunction != FunctionType.MOTION_TRACKING)
            return;

        if (!Engine.initialize(this, key)) {
            Log.e("MotionTracking", "Initialization Failed.");
            return;
        }
        if (!CameraDevice.isAvailable()) {
            return;
        }
        glView = new GLView(this);
        downloader = new CalibrationDownloader();
        downloader.download(10000, ImmediateCallbackScheduler.getDefault(), new FunctorOfVoidFromCalibrationDownloadStatusAndOptionalOfString() {
            @Override
            public void invoke(int status, String error) {
                //following code runs on a non-GUI thread
                if (status == CalibrationDownloadStatus.Successful) {
                    Log.i("MotionTracking", "Calibration file download successful.");
                } else if (status == CalibrationDownloadStatus.NotModified) {
                    Log.i("MotionTracking", "Calibration file is latest.");
                } else if (status == CalibrationDownloadStatus.ConnectionError) {
                    Log.i("MotionTracking", "Calibration file download connection error: " + error);
                } else if (status == CalibrationDownloadStatus.UnexpectedError) {
                    Log.i("MotionTracking", "Calibration file download unexpected error: " + error);
                } else {
                    Log.i("MotionTracking", "Calibration file download failed.");
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (downloader != null) {
                            downloader.dispose();
                            downloader = null;
                        }
                        if (isFinishing()) {
                            return;
                        }
                        if (!MotionTrackerCameraDevice.isAvailable()) {
                            Log.i("MotionTracking", "This device doesn't support motion tracking");
                            return;
                        }
                        requestCameraPermission(new PermissionCallback() {
                            @Override
                            public void onSuccess() {
                                ViewGroup preview = ((ViewGroup) findViewById(R.id.preview));
                                preview.addView(glView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                            }
                            @Override
                            public void onFailure() {
                            }
                        });
                    }
                });
            }
        });

        recentPoses = new SpatialPoseList(20);
        originalPose = new SpatialPose();
        currentPose = new SpatialPose();
        spatialPose2 = new SpatialPose();
    }

    public void startSteady() {
        isSteady = true;
        steadyTime = 0;
    }

    public void endSteady() {
        isSteady = false;
        steadyTime = 0;
    }

    public void startRealTimeMotionTracking() {
        isRealTimeTracking = true;
    }

    public void endRealTimeMotionTracking() {
        isRealTimeTracking = false;
        if (recentPoses != null)
            recentPoses.clear();
    }



    //以下若干是与运动跟踪功能有关的函数和变量
    private interface PermissionCallback
    {
        void onSuccess();
        void onFailure();
    }
    private HashMap<Integer, PermissionCallback> permissionCallbacks = new HashMap<Integer, PermissionCallback>();
    private int permissionRequestCodeSerial = 0;
    @TargetApi(23)
    private void requestCameraPermission(PermissionCallback callback)
    {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                int requestCode = permissionRequestCodeSerial;
                permissionRequestCodeSerial += 1;
                permissionCallbacks.put(requestCode, callback);
                requestPermissions(new String[]{Manifest.permission.CAMERA}, requestCode);
            } else {
                callback.onSuccess();
            }
        } else {
            callback.onSuccess();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (permissionCallbacks.containsKey(requestCode)) {
            PermissionCallback callback = permissionCallbacks.get(requestCode);
            permissionCallbacks.remove(requestCode);
            boolean executed = false;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    executed = true;
                    callback.onFailure();
                }
            }
            if (!executed) {
                callback.onSuccess();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
