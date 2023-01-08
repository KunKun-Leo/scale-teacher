//================================================================================================================================
//
// Copyright (c) 2015-2022 VisionStar Information Technology (Shanghai) Co., Ltd. All Rights Reserved.
// EasyAR is the registered trademark or trademark of VisionStar Information Technology (Shanghai) Co., Ltd in China
// and other countries for the augmented reality technology developed by VisionStar Information Technology (Shanghai) Co., Ltd.
//
//================================================================================================================================

package com.pcg.scaleteacher.slam;

import android.opengl.GLES30;

import java.nio.ByteBuffer;

import cn.easyar.Buffer;
import cn.easyar.CameraParameters;
import cn.easyar.DelayedCallbackScheduler;
import cn.easyar.Image;
import cn.easyar.InputFrame;
import cn.easyar.InputFrameToOutputFrameAdapter;
import cn.easyar.Matrix44F;
import cn.easyar.MotionTrackerCameraDevice;
import cn.easyar.MotionTrackerCameraDeviceFPS;
import cn.easyar.MotionTrackerCameraDeviceFocusMode;
import cn.easyar.MotionTrackerCameraDeviceResolution;
import cn.easyar.MotionTrackingStatus;
import cn.easyar.OutputFrame;
import cn.easyar.OutputFrameBuffer;
import cn.easyar.Vec3F;

public class HelloAR
{
    private DelayedCallbackScheduler scheduler;
    private MotionTrackerCameraDevice camera;
    private BGRenderer bgRenderer;
    private BoxRenderer boxRenderer;
    private InputFrameToOutputFrameAdapter adapter;
    private OutputFrameBuffer oFrameBuffer;
    private int previousInputFrameIndex = -1;
    private byte[] imageBytes = null;

    private float[] translation = new float[3];

    private InputFrame inputFrame;

    public HelloAR()
    {
        scheduler = new DelayedCallbackScheduler();
    }

    public void recreate_context()
    {
        if (bgRenderer != null) {
            bgRenderer.dispose();
            bgRenderer = null;
        }
        if (boxRenderer != null) {
            boxRenderer.dispose();
            boxRenderer = null;
        }
        previousInputFrameIndex = -1;
        bgRenderer = new BGRenderer();
        boxRenderer = new BoxRenderer();
    }

    public void initialize()
    {
        recreate_context();

        oFrameBuffer = OutputFrameBuffer.create();
        adapter = InputFrameToOutputFrameAdapter.create();
        camera = new MotionTrackerCameraDevice();
        camera.inputFrameSource().connect(adapter.input());
        adapter.output().connect(oFrameBuffer.input());
        //Camera_FPS_60 or Camera_FPS_30. If you do not call this function, the default is Camera_FPS_30.
        camera.setFrameRateType(MotionTrackerCameraDeviceFPS.Camera_FPS_60);
        //Continousauto or Medium. If you do not call this function, the default is Continousauto.
        camera.setFocusMode(MotionTrackerCameraDeviceFocusMode.Continousauto);
        //Resolution_1280 or Resolution_640. If you do not call this function, the default is Resolution_1280.
        camera.setFrameResolutionType(MotionTrackerCameraDeviceResolution.Resolution_640);
        //CameraDevice and rendering each require an additional buffer
        camera.setBufferCapacity(oFrameBuffer.bufferRequirement() + 2);
    }

    public void dispose()
    {
        if (bgRenderer != null) {
            bgRenderer.dispose();
            bgRenderer = null;
        }
        if (boxRenderer != null) {
            boxRenderer.dispose();
            boxRenderer = null;
        }
        if (camera != null) {
            camera.dispose();
            camera = null;
        }
        if (scheduler != null) {
            scheduler.dispose();
            scheduler = null;
        }
    }

    public boolean start()
    {
        boolean status = true;
        if (camera != null) {
            status &= camera.start();
        } else {
            status = false;
        }
        return status;
    }

    public void stop()
    {
        if (camera != null) {
            camera.stop();
        }
    }

    private Matrix44F inverseMatrix(Matrix44F matrix)
    {
        float[] m = matrix.data;
        float[] invOut = new float[16];
        float[] inv = new float[16];

        float det;
        int i;

        inv[0] = m[5]  * m[10] * m[15] -
                m[5]  * m[11] * m[14] -
                m[9]  * m[6]  * m[15] +
                m[9]  * m[7]  * m[14] +
                m[13] * m[6]  * m[11] -
                m[13] * m[7]  * m[10];

        inv[4] = -m[4]  * m[10] * m[15] +
                m[4]  * m[11] * m[14] +
                m[8]  * m[6]  * m[15] -
                m[8]  * m[7]  * m[14] -
                m[12] * m[6]  * m[11] +
                m[12] * m[7]  * m[10];

        inv[8] = m[4]  * m[9] * m[15] -
                m[4]  * m[11] * m[13] -
                m[8]  * m[5] * m[15] +
                m[8]  * m[7] * m[13] +
                m[12] * m[5] * m[11] -
                m[12] * m[7] * m[9];

        inv[12] = -m[4]  * m[9] * m[14] +
                m[4]  * m[10] * m[13] +
                m[8]  * m[5] * m[14] -
                m[8]  * m[6] * m[13] -
                m[12] * m[5] * m[10] +
                m[12] * m[6] * m[9];

        inv[1] = -m[1]  * m[10] * m[15] +
                m[1]  * m[11] * m[14] +
                m[9]  * m[2] * m[15] -
                m[9]  * m[3] * m[14] -
                m[13] * m[2] * m[11] +
                m[13] * m[3] * m[10];

        inv[5] = m[0]  * m[10] * m[15] -
                m[0]  * m[11] * m[14] -
                m[8]  * m[2] * m[15] +
                m[8]  * m[3] * m[14] +
                m[12] * m[2] * m[11] -
                m[12] * m[3] * m[10];

        inv[9] = -m[0]  * m[9] * m[15] +
                m[0]  * m[11] * m[13] +
                m[8]  * m[1] * m[15] -
                m[8]  * m[3] * m[13] -
                m[12] * m[1] * m[11] +
                m[12] * m[3] * m[9];

        inv[13] = m[0]  * m[9] * m[14] -
                m[0]  * m[10] * m[13] -
                m[8]  * m[1] * m[14] +
                m[8]  * m[2] * m[13] +
                m[12] * m[1] * m[10] -
                m[12] * m[2] * m[9];

        inv[2] = m[1]  * m[6] * m[15] -
                m[1]  * m[7] * m[14] -
                m[5]  * m[2] * m[15] +
                m[5]  * m[3] * m[14] +
                m[13] * m[2] * m[7] -
                m[13] * m[3] * m[6];

        inv[6] = -m[0]  * m[6] * m[15] +
                m[0]  * m[7] * m[14] +
                m[4]  * m[2] * m[15] -
                m[4]  * m[3] * m[14] -
                m[12] * m[2] * m[7] +
                m[12] * m[3] * m[6];

        inv[10] = m[0]  * m[5] * m[15] -
                m[0]  * m[7] * m[13] -
                m[4]  * m[1] * m[15] +
                m[4]  * m[3] * m[13] +
                m[12] * m[1] * m[7] -
                m[12] * m[3] * m[5];

        inv[14] = -m[0]  * m[5] * m[14] +
                m[0]  * m[6] * m[13] +
                m[4]  * m[1] * m[14] -
                m[4]  * m[2] * m[13] -
                m[12] * m[1] * m[6] +
                m[12] * m[2] * m[5];

        inv[3] = -m[1] * m[6] * m[11] +
                m[1] * m[7] * m[10] +
                m[5] * m[2] * m[11] -
                m[5] * m[3] * m[10] -
                m[9] * m[2] * m[7] +
                m[9] * m[3] * m[6];

        inv[7] = m[0] * m[6] * m[11] -
                m[0] * m[7] * m[10] -
                m[4] * m[2] * m[11] +
                m[4] * m[3] * m[10] +
                m[8] * m[2] * m[7] -
                m[8] * m[3] * m[6];

        inv[11] = -m[0] * m[5] * m[11] +
                m[0] * m[7] * m[9] +
                m[4] * m[1] * m[11] -
                m[4] * m[3] * m[9] -
                m[8] * m[1] * m[7] +
                m[8] * m[3] * m[5];

        inv[15] = m[0] * m[5] * m[10] -
                m[0] * m[6] * m[9] -
                m[4] * m[1] * m[10] +
                m[4] * m[2] * m[9] +
                m[8] * m[1] * m[6] -
                m[8] * m[2] * m[5];

        det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];

        if (det == 0)
            return matrix;

        det = 1.0f / det;

        for (i = 0; i < 16; i++)
            invOut[i] = inv[i] * det;
        matrix.data = invOut;
        return  matrix;
    }

    private void calWorldCoordinates(Matrix44F cameraToWorld) {
        float[] data = cameraToWorld.data;
        translation[0] = data[3];
        translation[1] = data[7];
        translation[2] = data[11];
    }

    public float[] getTranslation() {
        return translation;
    }

    private void calRotation() {
        //假设从世界坐标系到相机坐标系的变换为M4×4，则旋转矩阵就是左上角的3×3子矩阵
        //假设基于初始状态，状态1的旋转矩阵为R1，状态2的旋转矩阵为R2
        //那么，从状态1到状态2的旋转矩阵R3应当满足R2=R3R1，即R3=R1的逆R2
    }

    public void render(int width, int height, int screenRotation)
    {
        while (scheduler.runOne())
        {
        }

        GLES30.glViewport(0, 0, width, height);
        GLES30.glClearColor(0.f, 0.f, 0.f, 1.f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        OutputFrame oframe = oFrameBuffer.peek();
        if (oframe == null) { return;}
        InputFrame iframe = oframe.inputFrame();
        if (iframe == null) { oframe.dispose(); return; }
        CameraParameters cameraParameters = iframe.cameraParameters();
        if (cameraParameters == null) { oframe.dispose(); iframe.dispose(); return; }
        float viewport_aspect_ratio = (float)width / (float)height;
        Matrix44F imageProjection = cameraParameters.imageProjection(viewport_aspect_ratio, screenRotation, true, false);
        Image image = iframe.image();

        //Matrix44F transformMatrix = iframe.cameraTransform();
        //float[] data = transformMatrix.data;
        /*StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                stringBuffer.append(data[i * 4 + j]).append(" ");
            }
            stringBuffer.append("\n");
        }
        Log.e("Debug", stringBuffer.toString());*/

        //SimpleMatrix worldToCameraMatrix = new SimpleMatrix(4, 4, true, data);
        //SimpleMatrix cameraToWorldMatrix = worldToCameraMatrix.invert();
        //final float[] cameraCoordinatesData = new float[]{0, 0, 0, 1};
        //final SimpleMatrix cameraCoordinates = new SimpleMatrix(4, 1, true, cameraCoordinatesData);
        //SimpleMatrix worldCoordinates = cameraToWorldMatrix.mult(cameraCoordinates);
        //Log.e("Debug", worldToCameraMatrix.toString());



        try {
            if (iframe.index() != previousInputFrameIndex) {
                Buffer buffer = image.buffer();
                try {
                    if ((imageBytes == null) || (imageBytes.length != buffer.size())) {
                        imageBytes = new byte[buffer.size()];
                    }
                    buffer.copyToByteArray(imageBytes);
                    bgRenderer.upload(image.format(), image.width(), image.height(), image.pixelWidth(), image.pixelHeight(), ByteBuffer.wrap(imageBytes));
                } finally {
                    buffer.dispose();
                }
                previousInputFrameIndex = iframe.index();
            }
            bgRenderer.render(imageProjection);

            Matrix44F projectionMatrix = cameraParameters.projection(0.01f, 500.f, viewport_aspect_ratio, screenRotation, true, false);
            if (iframe.trackingStatus() != MotionTrackingStatus.NotTracking) {
                Matrix44F transform = iframe.cameraTransform();
                Matrix44F transformInverse = inverseMatrix(transform);
                if (boxRenderer != null) {
                    boxRenderer.render(projectionMatrix, transformInverse, new Vec3F(0.2f, 0.2f, 0.2f));
                }
                calWorldCoordinates(transformInverse);
                //Log.e("Debug", String.valueOf(worldCoordinates[0]) + " " + String.valueOf(worldCoordinates[1]) + " " + String.valueOf(worldCoordinates[2]));
            }
        } finally {
            iframe.dispose();
            oframe.dispose();
            if(cameraParameters != null) {
                cameraParameters.dispose();
            }
            image.dispose();
        }
    }
}
