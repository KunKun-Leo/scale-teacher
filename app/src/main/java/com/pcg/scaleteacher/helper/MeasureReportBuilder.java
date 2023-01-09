package com.pcg.scaleteacher.helper;

import com.pcg.scaleteacher.base.ConstantBase.SizeMeasureMethod;
import com.pcg.scaleteacher.base.CompletedFunctionBase;
import com.pcg.scaleteacher.base.FormalStudyBase;

//本类用于生成汇报结果，包括相关的建议话术
public class MeasureReportBuilder {
    //尺寸正式学习模式的报告生成
    public static String buildSizeFormalReport(float result, int goal, int method) {
        String report = "本次测得";
        float diff = result - goal;

        float toleranceA = 0f;
        float toleranceB = 0f;
        switch (method) {
            case SizeMeasureMethod.SINGLE_FINGER:
            case SizeMeasureMethod.TWO_FINGERS:
                toleranceA = CompletedFunctionBase.fingerToleranceA;
                toleranceB = CompletedFunctionBase.fingerToleranceB;
                report = report + String.format("%.1f", result) + "厘米。";
                break;
            case SizeMeasureMethod.ONE_HAND:
            case SizeMeasureMethod.TWO_HANDS:
            case SizeMeasureMethod.BODY:
                toleranceA = FormalStudyBase.getSpatialToleranceA(goal);
                toleranceB = FormalStudyBase.getSpatialToleranceB(goal);
                report = report + (int) result + "厘米。";
                break;
            default:
                return "";
        }
        if (Math.abs(diff) <= toleranceA)
            report = report + "误差很小，你完成得很棒！";
        else if (Math.abs(diff) <= toleranceB) {
            if (diff > 0)
                report = report + "建议下次缩短一点距离哦。";
            else
                report = report + "建议下次增加一点距离哦。";
        }
        else
            report = report + "误差好像有点大，下次加油!";

        return report;
    }

    public static String buildAngleFormalReport(int result, int goal) {
        String report = "本次测得";
        float diff = result - goal;

        if (Math.abs(diff) <= CompletedFunctionBase.angleToleranceA)
            report = report + "误差很小，你完成得很棒！";
        else if (Math.abs(diff) <= CompletedFunctionBase.angleToleranceB) {
            if (diff > 0)
                report = report + "建议下次少转一点哦。";
            else
                report = report + "建议下次多转一点哦。";
        }
        else
            report = report + "误差好像有点大，下次加油!";

        return report;
    }
}
