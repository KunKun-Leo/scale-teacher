package com.pcg.scaleteacher.base;

import java.util.LinkedList;

/* 该类封装实现了空间位姿信息的定容量队列 */
public class SpatialPoseList {
    private int size;
    private int capacity;
    private LinkedList<SpatialPose> poses;

    private float totalX;
    private float totalY;
    private float totalZ;
    private int totalYaw;
    private int totalPitch;
    private int totalRoll;

    public SpatialPoseList(int sizeLimit) {
        capacity = sizeLimit;
        size = 0;
        totalX = 0f;
        totalY = 0f;
        totalZ = 0f;
        totalYaw = 0;
        totalPitch = 0;
        totalRoll = 0;
        poses = new LinkedList<SpatialPose>();
    }

    public int addPose(SpatialPose pose) {
        if (size >= capacity) {
            SpatialPose firstPose = poses.poll();
            totalX -= firstPose.getX();
            totalY -= firstPose.getY();
            totalZ -= firstPose.getZ();
            totalYaw -= firstPose.getYaw();
            totalPitch -= firstPose.getPitch();
            totalRoll -= firstPose.getRoll();
            size--;
        }
        poses.add(pose);
        totalX += pose.getX();
        totalY += pose.getY();
        totalZ += pose.getZ();
        totalYaw += pose.getYaw();
        totalPitch += pose.getPitch();
        totalRoll += pose.getRoll();
        size--;

        return size;
    }

    //基于存储的spatialpose，获取其平均pose
    public SpatialPose getAveragePose() {
        if (size < 1)
            return new SpatialPose();
        float[] location = new float[] {totalX / size, totalY / size, totalZ / size};
        int[] rotation = new int[] {(int)(totalYaw / size), (int)(totalPitch / size), (int)(totalRoll / size)};
        SpatialPose averagePose = new SpatialPose(location, rotation);
        averagePose.isLocationValid = poses.peek().isLocationValid;
        averagePose.isRotationValid = poses.peek().isRotationValid;
        return averagePose;
    }

    public void clear() {
        poses.clear();
        size = totalYaw = totalPitch = totalRoll = 0;
        totalX = totalY = totalZ = 0f;
    }
}
