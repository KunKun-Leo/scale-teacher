package com.pcg.scaleteacher.main;

import android.content.Intent;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.helper.ARCoreCheckHelper;
import com.pcg.scaleteacher.helper.CameraPermissionHelper;
import com.pcg.scaleteacher.helper.DisplayRotationHelper;
import com.pcg.scaleteacher.helper.QuaternionHelper;
import com.pcg.scaleteacher.renderer.BackgroundRenderer;

import java.io.IOException;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MoveMeasureBase extends AppCompatActivity implements GLSurfaceView.Renderer, TextToSpeech.OnInitListener {

    protected Frame frame;

    protected Session session = null;
    protected DisplayRotationHelper displayRotationHelper;
    protected GLSurfaceView showGLSurface;
    protected BackgroundRenderer backgroundRenderer;

    protected TextToSpeech tts;

    //学习阶段
    protected enum StudyState {
        FIRST_TRY,  //初次尝试阶段
        CORRECTING,     //对初次尝试的纠正阶段
        PRACTICING,     //反复练习阶段
    }

    //监听模式
    protected enum WatchMode {
        END_CHECK,  //末端检查模式：当用户执行完毕之后再判断结果是否符合正确
        REAL_TIME   //实时检查模式：当用户执行过程中接近目标时予以提示
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new ARCoreCheckHelper().CheckARCore(this, session);
        frame = null;

        initTextToSpeech();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
        }

        //Init session
        showGLSurface.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (session!=null){
            showGLSurface.onPause();//GLSurfaceView onPause
            displayRotationHelper.onPause();
            session.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (session!=null){
            session.close();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //设置每一帧清屏颜色 传入参数为RGBA
        GLES20.glClearColor(0.5f,0.5f,0.5f,0f);
        backgroundRenderer = new BackgroundRenderer();
        try {
            backgroundRenderer.createOnGlThread(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //改变视口 方便 OpenGLES做 视口变换
        GLES20.glViewport(0,0,width,height);
        displayRotationHelper.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //清空彩色缓冲和深度缓冲  清空后的颜色为GLES20.glClearColor()时设置的颜色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        if (session == null)
            return;
        session.setCameraTextureName(backgroundRenderer.getTextureId());
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            frame = session.update();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
        //backgroundRenderer.draw(frame);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // setLanguage设置语言
            int result = tts.setLanguage(Locale.CHINESE);   //网上很多资料都用的是Locale.CHINA，会导致无法正常播放，原因不明
            // TextToSpeech.LANG_MISSING_DATA：表示语言的数据丢失
            // TextToSpeech.LANG_NOT_SUPPORTED：不支持
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "数据丢失或不支持", Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void initGLData(){
        showGLSurface=findViewById(R.id.gl_area);
        displayRotationHelper=new DisplayRotationHelper(this);

        // Set up renderer.
        showGLSurface.setPreserveEGLContextOnPause(true);
        showGLSurface.setEGLContextClientVersion(2);//OpenGL版本为2.0
        showGLSurface.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        showGLSurface.setRenderer(this);//实现Render接口
        showGLSurface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);//RENDERMODE_CONTINUOUSLY渲染模式为实时渲染。
    }

    private void initTextToSpeech() {
        tts = new TextToSpeech(this, this);
        tts.setPitch(1.0f);
        tts.setSpeechRate(0.5f);
    }
}

class SimplePose {
    public static final int ONLY_LOCATION = 0;
    public static final int ONLY_ANGLE = 1;

    private float x, y, z;
    private int yaw, pitch, roll;

    public SimplePose(float[] rawData, int constructMode ) {
        switch (constructMode) {
            case ONLY_LOCATION:
                x = rawData[0];
                y = rawData[1];
                z = rawData[2];
                yaw = pitch = roll = 0;
                break;
            case ONLY_ANGLE:
                int[] rotation = QuaternionHelper.ComputeEulerAngle(rawData);
                yaw = rotation[0];
                pitch = rotation[1];
                roll = rotation[2];
                x = y = z = 0;
                break;
            default:
                x = y = z = 0;
                yaw = pitch = roll = 0;
        }
    }

    public SimplePose(float[] translation, float[] quaternion) {
        x = translation[0];
        y = translation[1];
        z = translation[2];

        int[] rotation = QuaternionHelper.ComputeEulerAngle(quaternion);
        yaw = rotation[0];
        pitch = rotation[1];
        roll = rotation[2];
    }

    public float calDistanceSquare(SimplePose another) {
        //由于有些场景不一定需要计算出开平方后的距离，所以该方法只提供平方结果
        return (this.x - another.x) * (this.x - another.x)
                + (this.y - another.y) * (this.y - another.y)
                + (this.z - another.z) * (this.z - another.z);
    }

    /**********未完成************/
    public int[] calAngleChange(SimplePose original) {
        return new int[] {this.yaw - original.yaw, this.pitch - original.pitch, this.roll - original.roll};
    }
}
