package com.pcg.scaleteacher.base;

import com.pcg.scaleteacher.helper.QuaternionHelper;

public class SpatialPose {

    //构造或更新模式
    public static final int ONLY_TRANSLATION = 0;
    public static final int ONLY_QUATERNION = 1;

    private float x, y, z;
    private int yaw, pitch, roll;

    public boolean isLocationValid;     //指示x、y、z是否有效（SpatialPose失效时不会置为null，以避免意想不到的错误）
    public boolean isRotationValid;     //指示yaw、pitch、roll是否有效（SpatialPose失效时不会置为null，以避免意想不到的错误）

    public SpatialPose() {
        x = y = z = yaw = pitch = roll = 0;
        isLocationValid = isRotationValid = false;
    }

    public SpatialPose(float[] rawData, int constructMode ) {
        switch (constructMode) {
            case ONLY_TRANSLATION:
                x = rawData[0];
                y = rawData[1];
                z = rawData[2];
                yaw = pitch = roll = 0;
                isLocationValid = true;
                isRotationValid = false;
                break;
            case ONLY_QUATERNION:
                int[] rotation = QuaternionHelper.ComputeEulerAngle(rawData);
                yaw = rotation[0];
                pitch = rotation[1];
                roll = rotation[2];
                x = y = z = 0;
                isLocationValid = false;
                isRotationValid = true;
                break;
            default:
                x = y = z = 0;
                yaw = pitch = roll = 0;
                isLocationValid = false;
                isRotationValid = false;
                break;
        }
    }

    public SpatialPose(float[] translation, float[] quaternion) {
        x = translation[0];
        y = translation[1];
        z = translation[2];

        int[] rotation = QuaternionHelper.ComputeEulerAngle(quaternion);
        yaw = rotation[0];
        pitch = rotation[1];
        roll = rotation[2];

        isLocationValid = isRotationValid = true;
    }

    public SpatialPose(float[] location, int[] rotation) {
        x = location[0];
        y = location[1];
        z = location[2];
        yaw = rotation[0];
        pitch = rotation[1];
        roll = rotation[2];
        isLocationValid = isRotationValid = false;
    }

    public void updateLocation(float[] translation) {
        x = translation[0];
        y = translation[1];
        z = translation[2];
        isLocationValid = true;
    }

    public void updateRotation(float[] quaternion) {
        int[] rotation = QuaternionHelper.ComputeEulerAngle(quaternion);
        yaw = rotation[0];
        pitch = rotation[1];
        roll = rotation[2];
        isRotationValid = true;
    }

    public float calDistanceSquare(SpatialPose another) {
        //由于有些场景不一定需要计算出开平方后的距离，所以该方法只提供平方结果
        return (this.x - another.x) * (this.x - another.x)
                + (this.y - another.y) * (this.y - another.y)
                + (this.z - another.z) * (this.z - another.z);
    }

    public int[] calAngleChange(SpatialPose original) {
        /************这种计算方式有很大问题！还需要完善************/
        return new int[] {this.yaw - original.yaw, this.pitch - original.pitch, this.roll - original.roll};
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public int getYaw() {
        return yaw;
    }

    public int getPitch() {
        return pitch;
    }

    public int getRoll() {
        return roll;
    }
}
