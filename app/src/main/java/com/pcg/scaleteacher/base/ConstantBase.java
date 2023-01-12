package com.pcg.scaleteacher.base;

import androidx.appcompat.app.AppCompatActivity;

/* 作为所有Activity的基类，该类会定义一些常量，方便引用 */
public class ConstantBase extends AppCompatActivity {
    //基本功能模块分类（不使用enum，是方便intent传递数据和设置默认值）
    public static final String basicModeTag = "SCALE TEACHER BASIC MODE";
    public static class BasicMode {
        public static final int FORMAL_STYLE = 0;
        public static final int FREE_STYLE = 1;
        public static final int TEST = 2;
    };

    //测量或学习内容分类
    public static final String studyContentTag = "SCALE TEACHER STUDY CONTENT";
    public static class StudyContent {
        public static final int STUDY_SIZE = 0;
        public static final int STUDY_ANGLE = 1;
    }

    //（长度）测量方式分类
    public static final String sizeMeasureMethodTag = "SCALE TEACHER SIZE MEASURE METHOD";
    public static class SizeMeasureMethod {
        public static final int UNKNOWN = 0;
        public static final int SINGLE_FINGER = 1;
        public static final int TWO_FINGERS = 2;
        public static final int ONE_HAND = 3;
        public static final int TWO_HANDS = 4;
        public static final int BODY = 5;
        public static final int MIXED = 6;      //可能是ONE_HAND、 TWO_HANDS、 BODY、 ANGLE中的任何一种
    }
    public static class AngleMeasureMethod {
        public static final int ANGLE = 7;      //角度其实只有一种测量方法，这里只是为了与SizeMeasureMethod并列，便于使用
    }

    //学习目标（仅限正式教学模式）
    public static final String studyGoalTag = "SCALE TEACHER STUDY GOAL";
}
