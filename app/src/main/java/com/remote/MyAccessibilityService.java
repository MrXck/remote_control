package com.remote;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {

    /**
     * 无障碍服务的生命周期，表明服务已经连接成功
     */
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.e("zh", "onServiceConnected: 成功");
    }

    /**
     *当系统想要中断您的服务正在提供的反馈（通常是为了响应将焦点移到其他
     *控件等用户操作）时，就会调用此方法。此方法可能会在您的服务的整个生命
     *周期内被调用多次。
     */
    @Override
    public void onInterrupt() {
        Log.e("zh", "onInterrupt: 成功");
    }

    /**
     * 当用户在触摸屏上执行特定手势时由系统调用。注意:为了接收手势，
     * 辅助服务必须通过设置AccessibilityServiceInfo请求设备处于触摸探索模式FLAG_REQUEST_TOUCH_EXPLORATION_MOD
     */
    @Override
    public boolean onGesture(int gestureId) {
        Log.e("zh", "onGesture: " + gestureId);
        return super.onGesture(gestureId);
    }

    /**
     *当系统检测到与无障碍服务指定的事件过滤参数匹配的 AccessibilityEvent
     *时，就会回调此方法。例如，当用户点击按钮，或者聚焦于某个应用（无障碍
     *服务正在为该应用提供反馈）中的界面控件时。出现这种情况时，系统会调用
     *此方法，并传递关联的 AccessibilityEvent，然后服务会对该类进行解释并
     *使用它来向用户提供反馈。此方法可能会在您的服务的整个生命周期内被调用多次。
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.e("zh", "无障碍服务 onAccessibilityEvent:${event}");
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_ANNOUNCEMENT:
                Log.e("zh", "应用程序发布公告的事件");
                break;
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
                Log.e("zh", "View的焦点");
                break;
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED:
                Log.e("zh", "View的焦点清除");
                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.e("zh", "通知栏状态更新");
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
                Log.e("zh", "View的鼠标悬停选中");
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
                Log.e("zh", "View的鼠标悬停离开");
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
                Log.e("zh", "开始触摸探索手势的事件");
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
                Log.e("zh", "结束触摸探索手势的事件");
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                Log.e("zh", "窗口内容更新");
                break;
            case AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT:
                Log.e("zh", "表示助手当前正在读取用户屏幕上下文的事件。");
                break;
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_END:
                Log.e("zh", "表示用户结束触摸屏幕的事件");
                break;
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START:
                Log.e("zh", "表示用户开始触摸屏幕的事件");
                break;
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_END:
                Log.e("zh", "结束手势检测");
                break;
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_START:
                Log.e("zh", "开始手势检测");
                break;
            case AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED:
                Log.e("zh", "表示在android.view.View上的上下文单击事件");
                break;
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                Log.e("zh", "长按点击事件");
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                Log.e("zh", "点击事件");
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY:
                Log.e("zh", "表示以给定的移动粒度遍历视图文本的事件");
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                Log.e("zh", "EditText视图内容改变");
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                Log.e("zh", "EditText视图选中内容改变");
                break;
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                Log.e("zh", "表示通常在android.widget.AdapterView的上下文中选择项的事件");
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                Log.e("zh", "滚动类View");
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                break;
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                break;
        }
    }

    /**
     * 按键事件
     *
     */
    @Override
    public boolean onKeyEvent(KeyEvent event) {
        Log.e("zh", "onKeyEvent: ${event}");
        return super.onKeyEvent(event);
    }
}