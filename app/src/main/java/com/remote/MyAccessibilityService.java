package com.remote;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MyAccessibilityService extends AccessibilityService {

    private LocalServerSocket serverSocket;
    private volatile boolean isRunning = true;

    private int width;
    private int height;
    private Integer everyActionTime = 80;

    private final Map<Double, Map<String, Object>> description = new ConcurrentHashMap<>();

    // 获取屏幕分辨率（单位：像素）
    public int[] getScreenResolution() {
        // 通过系统服务获取 WindowManager
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // 创建 DisplayMetrics 对象
        DisplayMetrics displayMetrics = new DisplayMetrics();

        // 获取默认显示器的指标
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        // 提取宽高
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        this.width = width;
        this.height = height;

        return new int[]{width, height};
    }

    /**
     * 无障碍服务的生命周期，表明服务已经连接成功
     */
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.e("zh", "onServiceConnected: 成功");
        Log.e("display", Arrays.toString(getScreenResolution()));
        new Thread(() -> {
            try {
                serverSocket = new LocalServerSocket("my_click_socket");
                while (isRunning) {
                    LocalSocket client = serverSocket.accept();
                    handleClient(client);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(LocalSocket client) {
        try (DataInputStream input = new DataInputStream(client.getInputStream())) {
            int type = input.readInt();
            double id = input.readDouble();
            double x = input.readDouble();
            double y = input.readDouble();
            Log.e("ACTION", "x=" + (x * width) + ",y=" + (y * height));

            if (type == 3) {
                handleEndDrag(x, y, id);
                return;
            }

            if (description.containsKey(id) && type == 4) {
                double x1 = input.readDouble();
                double y1 = input.readDouble();
                handleDrag(x, y, x1, y1, id, description.get(id));
            } else {
                if (type == 1) {
                    handleClick(x, y, id);
                } else if (type == 2) {
                    double x1 = input.readDouble();
                    double y1 = input.readDouble();
                    handleDrag(x, y, x1, y1);
                } else if (type == 5) {
                    handleNotContinueClick(x, y);
                } else if (type == 6) {
                    handleBack();
                } else if (type == 7) {
                    handleHome();
                } else if (type == 8) {
                    handleRecent();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleBack() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    private void handleHome() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    public void handleRecent() {
        performGlobalAction(GLOBAL_ACTION_RECENTS);
    }

    private void handleEndDrag(double x, double y, double id) {
        Path path = new Path();
        path.moveTo((float) (x * width), (float) (y * height)); // 目标坐标

        Map<String, Object> map = description.get(id);

        Integer startTime = (Integer) map.get("time");

        GestureDescription.StrokeDescription description1 = (GestureDescription.StrokeDescription) map.get("description");

        GestureDescription.StrokeDescription strokeDescription = description1.continueStroke(
                path,
                description1.getDuration(),     // 开始时间（ms）
                100,     // 持续时间（ms，建议≥50ms）
                false
        );

        // ...
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(strokeDescription)
                .build();
        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
            }
        }, null); // 执行手势
        description.remove(id);

    }

    private void handleClick(double x, double y, double id) {
        Path path = new Path();
        path.moveTo((float) (x * width), (float) (y * height)); // 目标坐标

        Integer startTime;

        Integer everyActionTime = 50;

        if (description.containsKey(id)) {
            startTime = (Integer) description.get(id).get("time");
        } else {
            startTime = 0;
        }

        GestureDescription.StrokeDescription strokeDescription = new GestureDescription.StrokeDescription(
                path,
                startTime,     // 开始时间（ms）
                everyActionTime,     // 持续时间（ms，建议≥50ms）
                true
        );

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(strokeDescription)
                .build();

        dispatchGesture(gesture, null, null); // 执行手势

        HashMap<String, Object> stringObjectHashMap = new HashMap<>();
        stringObjectHashMap.put("description", strokeDescription);
        stringObjectHashMap.put("time", startTime + everyActionTime);

        description.put(id, stringObjectHashMap);
    }

    private void handleNotContinueClick(double x, double y) {
        Path path = new Path();
        path.moveTo((float) (x * width), (float) (y * height)); // 目标坐标

        GestureDescription.StrokeDescription strokeDescription = new GestureDescription.StrokeDescription(
                path,
                0,     // 开始时间（ms）
                everyActionTime
        );

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(strokeDescription)
                .build();

        dispatchGesture(gesture, null, null); // 执行手势
    }

    // 拖动
    private void handleDrag(double x, double y, double x1, double y1) {
        Path path = new Path();
        path.moveTo((float) (x * width), (float) (y * height)); // 目标坐标
        path.lineTo((float) (x1 * width), (float) (y1 * height)); // 目标坐标
        // ...
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(
                        path,
                        0,     // 开始时间（ms）
                        200     // 持续时间（ms，建议≥50ms）
                ))
                .build();
        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
            }
        }, null); // 执行手势
    }

    private void handleDrag(double x, double y, double x1, double y1, double id, Map<String, Object> map) {
        Path path = new Path();
        path.moveTo((float) (x * width), (float) (y * height)); // 目标坐标
        path.lineTo((float) (x1 * width), (float) (y1 * height)); // 目标坐标

        Integer startTime = (Integer) map.get("time");

        GestureDescription.StrokeDescription description1 = (GestureDescription.StrokeDescription) map.get("description");

        GestureDescription.StrokeDescription strokeDescription = description1.continueStroke(
                path,
                description1.getDuration(),     // 开始时间（ms）
                everyActionTime,     // 持续时间（ms，建议≥50ms）
                true
        );

        // ...
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(strokeDescription)
                .build();
        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
            }
        }, null); // 执行手势
        HashMap<String, Object> stringObjectHashMap = new HashMap<>();
        stringObjectHashMap.put("description", strokeDescription);
        stringObjectHashMap.put("time", startTime + everyActionTime);

        description.put(id, stringObjectHashMap);
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
//        Log.e("zh", "无障碍服务 onAccessibilityEvent:${event}");
//        switch (event.getEventType()) {
//            case AccessibilityEvent.TYPE_ANNOUNCEMENT:
//                Log.e("zh", "应用程序发布公告的事件");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
//                Log.e("zh", "View的焦点");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED:
//                Log.e("zh", "View的焦点清除");
//                break;
//            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
//                Log.e("zh", "通知栏状态更新");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
//                Log.e("zh", "View的鼠标悬停选中");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
//                Log.e("zh", "View的鼠标悬停离开");
//                break;
//            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
//                Log.e("zh", "开始触摸探索手势的事件");
//                break;
//            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
//                Log.e("zh", "结束触摸探索手势的事件");
//                break;
//            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
//                Log.e("zh", "窗口内容更新");
//                break;
//            case AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT:
//                Log.e("zh", "表示助手当前正在读取用户屏幕上下文的事件。");
//                break;
//            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_END:
//                Log.e("zh", "表示用户结束触摸屏幕的事件");
//                break;
//            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START:
//                Log.e("zh", "表示用户开始触摸屏幕的事件");
//                break;
//            case AccessibilityEvent.TYPE_GESTURE_DETECTION_END:
//                Log.e("zh", "结束手势检测");
//                break;
//            case AccessibilityEvent.TYPE_GESTURE_DETECTION_START:
//                Log.e("zh", "开始手势检测");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED:
//                Log.e("zh", "表示在android.view.View上的上下文单击事件");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
//                Log.e("zh", "长按点击事件");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_CLICKED:
//                Log.e("zh", "点击事件");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY:
//                Log.e("zh", "表示以给定的移动粒度遍历视图文本的事件");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
//                Log.e("zh", "EditText视图内容改变");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
//                Log.e("zh", "EditText视图选中内容改变");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_SELECTED:
//                Log.e("zh", "表示通常在android.widget.AdapterView的上下文中选择项的事件");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
//                Log.e("zh", "滚动类View");
//                break;
//            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
//                break;
//            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
//                break;
//            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
//                break;
//        }
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