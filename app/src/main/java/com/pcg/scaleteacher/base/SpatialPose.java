package com.pcg.scaleteacher.base;

public class SpatialPose {

    private float x, y, z;
    private int yaw, pitch, roll;

    public float calDistanceSquare(SpatialPose another) {
        //由于有些场景不一定需要计算出开平方后的距离，所以该方法只提供平方结果
        return (this.x - another.x) * (this.x - another.x)
                + (this.y - another.y) * (this.y - another.y)
                + (this.z - another.z) * (this.z - another.z);
    }

}
