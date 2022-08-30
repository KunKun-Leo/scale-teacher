package com.pcg.scaleteacher.helper;

public class QuaternionHelper {
    //Use quaternion to compute the euler angle
    public static int [] ComputeEulerAngle (float [] q) {
        final float x = (float) q[0];
        final float y = (float) q[1];
        final float z = (float) q[2];
        final float w = (float) q[3];

        final int yaw = (int) Math.toDegrees(Math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y)));
        final int pitch = (int) Math.toDegrees(Math.asin(2 * (w * y - z * x)));
        final int roll = (int) Math.toDegrees(Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z)));

        return new int[] {yaw, pitch, roll};
    }
}
