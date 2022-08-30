package com.pcg.scaleteacher.helper;

import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

public class ScreenReaderCheckHelper {

    public static boolean check(Context context) {
        AccessibilityManager accessibilityManager = (AccessibilityManager)
                context.getSystemService(Context.ACCESSIBILITY_SERVICE);

        boolean touchEnable= accessibilityManager.isTouchExplorationEnabled(); //启用了系统中的触摸检测
        boolean enabled= accessibilityManager.isEnabled(); //判断系统是否是可辅助的
        return (touchEnable && enabled);
    }
}
