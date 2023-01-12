package com.pcg.scaleteacher.base;

import android.util.Log;

public class SpatialPose {

    private float[] transform;      //相机相对于世界坐标系的变换矩阵的原始信息（行优先）

    //构造或更新模式
    public static final int ONLY_TRANSLATION = 0;
    public static final int ONLY_ROTATION = 1;

    //当手机竖直举起，显示屏朝向自己时
    //private float sway;     //左右位移，左为负，右为正
    //private float heave;    //上下位移，上为正，下为负
    //private float surge;    //前后位移，后为正，前为负
    //private int yaw;        //偏航角（绕heave轴左右旋转），左转减小，右转增大，初始值180
    //private int pitch;      //俯仰角（绕sway轴前后俯仰），前倒减小，后仰增大，初始值180
    //private int roll;       //横滚角（绕surge轴左右翻滚），左滚增大，右滚减小，初始值90

    //public boolean isTranslationValid;     //指示x、heave、z是否有效（SpatialPose失效时不会置为null，以避免意想不到的错误）
    //public boolean isRotationValid;     //指示yaw、pitch、roll是否有效（SpatialPose失效时不会置为null，以避免意想不到的错误）
    public boolean isValid;         //指示transform是否有效（SpatialPose失效时不会置为null，以避免意想不到的错误）

    public SpatialPose() {
        transform = new float[16];
        isValid = false;
    }

    public SpatialPose(float[] transformData) {
        transform = transformData;
        isValid = true;
    }

    public void update(float[] newTransform) {
        transform = newTransform;
        isValid = true;
    }

    public float calDistanceSquare(SpatialPose another) {
        //由于有些场景不一定需要计算出开平方后的距离，所以该方法只提供平方结果
        float sway1 = this.getSway();
        float sway2 = another.getSway();
        float heave1 = this.getHeave();
        float heave2 = another.getHeave();
        float surge1 = this.getSurge();
        float surge2 = another.getSurge();
        return (sway1 - sway2) * (sway1 - sway2)
                + (heave1 - heave2) * (heave1 - heave2)
                + (surge1 - surge2) * (surge1 - surge2);
    }

    public float calAngleChange(SpatialPose original) {
        //计算此角度变化的预设是：
        //1.手机水平放倒，屏幕朝上，计算人的偏航，因此实际计算的角度差为手机的roll的差值
        //2.HelloAR中已经将角度规范到了[0°, 360°)范围
        //int angleDiff = Math.abs(this.roll - original.getRoll());
        //if (angleDiff > 180)
        //    return 360 - angleDiff;
        //else
        //    return angleDiff;

        //计算角度变化的步骤：
        //1.计算向量[1, 1, 1]分别经两个旋转变换（变换矩阵左上角的3×3矩阵）的结果
        //2.求两新向量的夹角

        float[] originalTransform = original.getTransform();

        float[] vector1 = new float[] {
                transform[0] + transform[1] + transform[2],
                transform[4] + transform[5] + transform[6],
                transform[8] + transform[9] + transform[10]
        };
        float[] vector2 = new float[] {
                originalTransform[0] + originalTransform[1] + originalTransform[2],
                originalTransform[4] + originalTransform[5] + originalTransform[6],
                originalTransform[8] + originalTransform[9] + originalTransform[10]
        };

        float dotProduct = vector1[0] * vector2[0] + vector1[1] * vector2[1] + vector1[2] * vector2[2];

        //注意两个向量的模都为根号3
        return (float) Math.toDegrees(Math.acos(dotProduct / 3));
    }

    //左右位移，单位厘米
    public float getSway() {
        return transform[3] * 100;
    }

    //上下位移，单位厘米
    public float getHeave() {
        return transform[7] * 100;
    }

    //前后位移，单位厘米
    public float getSurge() {
        return transform[11] * 100;
    }

    public float[] getTransform() {
        return transform;
    }
}
