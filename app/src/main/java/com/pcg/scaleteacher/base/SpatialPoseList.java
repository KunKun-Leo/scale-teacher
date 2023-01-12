package com.pcg.scaleteacher.base;

import android.util.Log;

import java.util.LinkedList;

/* 该类封装实现了空间位姿信息的定容量队列 */
public class SpatialPoseList {
    private int size;
    private int capacity;
    private LinkedList<SpatialPose> poses;

    public SpatialPoseList(int sizeLimit) {
        capacity = sizeLimit;
        size = 0;
        poses = new LinkedList<SpatialPose>();
    }

    public int addPose(SpatialPose pose) {
        if (size >= capacity) {
            poses.poll();
            size--;
        }
        poses.add(pose);
        size++;

        return size;
    }

    //基于存储的spatialpose，获取其平均pose
    public SpatialPose getAveragePose() {
        if (size <= 0)
            return null;

        //translation部分取缓存的位姿的平均值
        //rotation部分取最后一个缓存（因为不确定旋转矩阵是否有连续性或周期性，不方便求平均值）
        float[] averageTransform = poses.getLast().getTransform();

        float totalSway = 0f;
        float totalHeave = 0f;
        float totalSurge = 0f;
        for (SpatialPose pose: poses) {
            float[] transform = pose.getTransform();
            totalSway += transform[3];
            totalHeave += transform[7];
            totalSurge += transform[11];
        }
        averageTransform[3] = totalSway / size;
        averageTransform[7] = totalHeave / size;
        averageTransform[11] = totalSurge / size;
        return new SpatialPose(averageTransform);
    }

    public void clear() {
        poses.clear();
        size = 0;
    }
}
