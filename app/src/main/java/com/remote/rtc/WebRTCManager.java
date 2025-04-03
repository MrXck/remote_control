package com.remote.rtc;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import com.remote.MainActivity;
import com.remote.pojo.Message;
import com.remote.utils.LogUtils;
import com.remote.utils.RotationUtils;
import org.webrtc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebRTCManager {

    public EglBase eglBase;
    public PeerConnectionFactory peerConnectionFactory;
    public VideoTrack localVideoTrack;
    public Context CONTEXT;
    public PeerConnection localPeerConnection;
    public MediaProjection mediaProjection;
    private VideoCapturer screenCapturer;
    private VideoSource videoSource;

    // UI Components
    public SurfaceViewRenderer localView;
    public SurfaceViewRenderer remoteView;

    public void createOffer(PeerConnection peerConnection) {
        MediaConstraints constraints = new MediaConstraints();
        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {

                    }

                    @Override
                    public void onSetSuccess() {
                        Message message = new Message();
                        message.setType("offer");


                        Map<Object, Object> map = new HashMap<>();
                        map.put("type", sessionDescription.type.toString());
                        map.put("sdp", sessionDescription.description);

                        message.setSdp(map);
                        MainActivity.webSocketManager.sendMessage(message);
                    }

                    @Override
                    public void onCreateFailure(String s) {

                    }

                    @Override
                    public void onSetFailure(String s) {

                    }
                }, sessionDescription);
            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, constraints);
    }

    private void initializeViews() {
//        localView.init(eglBase.getEglBaseContext(), null);
//        localView.setMirror(false); // 屏幕共享无需镜像
//        localView.setZOrderMediaOverlay(true); // 允许覆盖其他视图
        remoteView.init(eglBase.getEglBaseContext(), null);
    }

    private void startLocalVideoCapture() {
        // Create video capturer
        CameraEnumerator enumerator = new Camera2Enumerator(CONTEXT);
        VideoCapturer videoCapturer = enumerator.createCapturer(enumerator.getDeviceNames()[0], null);

        // Create video source
        SurfaceTextureHelper surfaceHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceHelper, CONTEXT, videoSource.getCapturerObserver());
        videoCapturer.startCapture(1280, 720, 30);

        // Create video track
        localVideoTrack = peerConnectionFactory.createVideoTrack("local_video", videoSource);
        localVideoTrack.addSink(localView);
        MediaStream localStream = peerConnectionFactory.createLocalMediaStream("local_stream");
        localStream.addTrack(localVideoTrack);
        localStream.addTrack(peerConnectionFactory.createAudioTrack("local_audio", peerConnectionFactory.createAudioSource(new MediaConstraints())));
        localPeerConnection.addStream(localStream);
    }

    public void startScreenCapture(Intent mediaProjectionIntent) {
        stopScreenCapture();

        screenCapturer = new ScreenCapturerAndroid(
                mediaProjectionIntent,
                new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        Log.d("ScreenCapture", "用户主动停止屏幕共享");
                        stopScreenCapture();
                    }
                }
        );

        videoSource = peerConnectionFactory.createVideoSource(screenCapturer.isScreencast());
        SurfaceTextureHelper surfaceHelper = SurfaceTextureHelper.create(
                "ScreenCaptureThread",
                eglBase.getEglBaseContext()
        );
        screenCapturer.initialize(surfaceHelper, CONTEXT, new AdaptedCapturerObserver(videoSource.getCapturerObserver(), CONTEXT));

        // 5. 设置捕获参数（建议使用屏幕实际分辨率）
        DisplayMetrics metrics = CONTEXT.getResources().getDisplayMetrics();
        screenCapturer.startCapture(metrics.widthPixels, metrics.heightPixels, 30);

        createVideoTrack();
    }

    public void createVideoTrack() {
        // 6. 创建新轨道并替换
        try {
            localVideoTrack.dispose();
        } catch (Exception ignored) {}
        localVideoTrack = peerConnectionFactory.createVideoTrack("screen_video", videoSource);
//        localVideoTrack.addSink(localView);
        localPeerConnection.addTrack(localVideoTrack);
    }

    /**
     * 停止屏幕捕获
     */
    public void stopScreenCapture() {
        try {
            if (screenCapturer != null) {
                screenCapturer.stopCapture();
                screenCapturer.dispose();
                screenCapturer = null;
            }
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
        } catch (Exception e) {
            Log.e("ScreenCapture", "停止捕获失败", e);
        }
    }

    public void initialize(Context context) {
        // 初始化 EGL 上下文
        eglBase = EglBase.create();

        CONTEXT = context;

        // 初始化 PeerConnectionFactory
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(CONTEXT)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        // 创建 PeerConnectionFactory 实例
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(
                        eglBase.getEglBaseContext(),
                        true,  // 启用硬件编码
                        true   // 启用 H.264
                ))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();
        initLocalPeerConnection();
        initializeViews();
//        startLocalVideoCapture();
    }

    public void initLocalPeerConnection() {
        try {
            localPeerConnection.dispose();
        } catch (Exception ignored) {}
        localPeerConnection = createPeerConnection();
    }

    private PeerConnection createPeerConnection() {
        PeerConnection peerConnection;
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:8.217.77.26:3478").createIceServer());
//        iceServers.add(PeerConnection.IceServer.builder("turn:8.217.77.26:3478?transport=udp")
//                .setUsername("demo").setPassword("123456").createIceServer());
        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(iceServers);
        peerConnection = peerConnectionFactory.createPeerConnection(
                config,
                new PeerConnection.Observer() {
                    @Override
                    public void onSignalingChange(PeerConnection.SignalingState signalingState) {

                    }

                    @Override
                    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                        Log.d("WebRTConIceConnectionChangeLOCAL", "ICE 状态: " + iceConnectionState);
                    }

                    @Override
                    public void onIceConnectionReceivingChange(boolean b) {

                    }

                    @Override
                    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

                    }

                    @Override
                    public void onIceCandidate(IceCandidate iceCandidate) {
                        Message message = new Message();
                        message.setType("candidate");
                        Map map = new HashMap<>();

                        map.put("candidate", iceCandidate.sdp);
                        map.put("sdpMid", iceCandidate.sdpMid);
                        map.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                        message.setCandidate(map);
                        MainActivity.webSocketManager.sendMessage(message);
                    }

                    @Override
                    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

                    }

                    @Override
                    public void onAddStream(MediaStream mediaStream) {
                    }

                    @Override
                    public void onRemoveStream(MediaStream mediaStream) {

                    }

                    @Override
                    public void onDataChannel(DataChannel dataChannel) {

                    }

                    @Override
                    public void onRenegotiationNeeded() {

                    }

                    @Override
                    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                    }
                }
        );
        return peerConnection;
    }

    public PeerConnection handlerOffer(Map map) {
        Log.e("offer", map.toString());
        PeerConnection peerConnection;
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:8.217.77.26:3478").createIceServer());
//        iceServers.add(PeerConnection.IceServer.builder("turn:8.217.77.26:3478?transport=udp")
//                .setUsername("demo").setPassword("123456").createIceServer());
        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(iceServers);
        peerConnection = peerConnectionFactory.createPeerConnection(
                config,
                new PeerConnection.Observer() {
                    @Override
                    public void onSignalingChange(PeerConnection.SignalingState signalingState) {

                    }

                    @Override
                    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                        Log.d("WebRTConIceConnectionChange", "ICE 状态: " + iceConnectionState);
                    }

                    @Override
                    public void onIceConnectionReceivingChange(boolean b) {

                    }

                    @Override
                    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

                    }

                    @Override
                    public void onIceCandidate(IceCandidate iceCandidate) {
                        Message message = new Message();
                        message.setType("candidate");
                        Map map = new HashMap<>();

                        map.put("candidate", iceCandidate.sdp);
                        map.put("sdpMid", iceCandidate.sdpMid);
                        map.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                        message.setCandidate(map);
                        MainActivity.webSocketManager.sendMessage(message);
                    }

                    @Override
                    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

                    }

                    @Override
                    public void onAddStream(MediaStream mediaStream) {
                        LogUtils.log(() -> {
                            // 收到远程流，渲染到界面
                            VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                            remoteVideoTrack.addSink(remoteView);
                        });
                    }

                    @Override
                    public void onRemoveStream(MediaStream mediaStream) {

                    }

                    @Override
                    public void onDataChannel(DataChannel dataChannel) {

                    }

                    @Override
                    public void onRenegotiationNeeded() {

                    }

                    @Override
                    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                        MediaStreamTrack track = rtpReceiver.track();
                        if (track == null) return;

                        if (track.kind().equals("video")) {
                            VideoTrack remoteVideoTrack = (VideoTrack) track;
                            new Handler(Looper.getMainLooper()).post(() -> {
                                remoteVideoTrack.addSink(remoteView); // ✅ 主线程操作
                            });
                        } else if (track.kind().equals("audio")) {
                            AudioTrack remoteAudioTrack = (AudioTrack) track;
                            remoteAudioTrack.setEnabled(true);
                        }
                    }
                }
        );
        if (peerConnection != null) {
            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    // 设置远程描述成功
                }

                @Override
                public void onSetSuccess() {
                    peerConnection.createAnswer(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                            peerConnection.setLocalDescription(new SdpObserver() {
                                @Override
                                public void onCreateSuccess(SessionDescription sessionDescription) {

                                }

                                @Override
                                public void onSetSuccess() {
                                    Message message = new Message();
                                    message.setType("answer");

                                    Map map = new HashMap<>();
                                    map.put("sdp", sessionDescription.description);
                                    map.put("type", sessionDescription.type);
                                    message.setSdp(map);
                                    MainActivity.webSocketManager.sendMessage(message);
                                }

                                @Override
                                public void onCreateFailure(String s) {

                                }

                                @Override
                                public void onSetFailure(String s) {

                                }
                            }, sessionDescription);
                        }

                        @Override
                        public void onSetSuccess() {

                        }

                        @Override
                        public void onCreateFailure(String s) {

                        }

                        @Override
                        public void onSetFailure(String s) {

                        }
                    }, new MediaConstraints());
                }

                @Override
                public void onCreateFailure(String s) {

                }

                @Override
                public void onSetFailure(String s) {

                }


            }, new SessionDescription(SessionDescription.Type.OFFER, (String) map.get("sdp")));


        }

        return peerConnection;
    }

    public void handlerAnswer(Map map, PeerConnection peerConnection) {
        Log.e("answer", map.toString());
        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {
                Log.e("answer", "setSuccess");
            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, new SessionDescription(SessionDescription.Type.ANSWER, (String) map.get("sdp")));

    }

    public void handlerCandidate(Map map, PeerConnection peerConnection) {
        Log.e("candidate", map.toString());
        peerConnection.addIceCandidate(new IceCandidate((String) map.get("sdpMid"), (int) ((double) map.get("sdpMLineIndex")), (String) map.get("candidate")));
    }

    private static class AdaptedCapturerObserver implements CapturerObserver {
        private final CapturerObserver originalObserver;
        public Context context;

        public AdaptedCapturerObserver(CapturerObserver originalObserver, Context context) {
            this.originalObserver = originalObserver;
            this.context = context;
        }

        @Override
        public void onFrameCaptured(VideoFrame frame) {
            // 调整旋转参数
            VideoFrame adaptedFrame = new VideoFrame(
                    frame.getBuffer(),
                    RotationUtils.getDeviceRotationDegrees(context),
                    frame.getTimestampNs()
            );
            originalObserver.onFrameCaptured(adaptedFrame);
        }

        // 其他方法直接代理
        @Override
        public void onCapturerStarted(boolean success) {
            originalObserver.onCapturerStarted(success);
        }

        @Override
        public void onCapturerStopped() {
            originalObserver.onCapturerStopped();
        }
    }

    private static class RotationAwareObserver implements CapturerObserver {
        private final Context context;
        private final CapturerObserver originalObserver;

        public RotationAwareObserver(Context context, CapturerObserver originalObserver) {
            this.context = context;
            this.originalObserver = originalObserver;
        }

        @Override
        public void onCapturerStarted(boolean success) {
            originalObserver.onCapturerStarted(success);
        }

        @Override
        public void onCapturerStopped() {
            originalObserver.onCapturerStopped();
        }

        @Override
        public void onFrameCaptured(VideoFrame frame) {
            int currentRotation = RotationUtils.getDeviceRotationDegrees(context);
            VideoFrame rotatedFrame = new VideoFrame(
                    frame.getBuffer(),
                    currentRotation, // 动态应用当前设备方向
                    frame.getTimestampNs()
            );
            originalObserver.onFrameCaptured(rotatedFrame);
        }

        // 其他方法代理...
    }
}
