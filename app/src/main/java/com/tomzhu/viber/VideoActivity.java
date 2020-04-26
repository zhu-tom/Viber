package com.tomzhu.viber;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.se.omapi.Session;
import android.util.JsonReader;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class VideoActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private SurfaceViewRenderer localRenderer;
    private SurfaceViewRenderer remoteRenderer;
    static private PeerConnection localPeer;
    private PeerConnection remotePeer;
    private PeerConnectionFactory peerConnectionFactory;
    private EglBase rootBase = EglBase.create();
    private OkHttpClient client;
    private WebSocket webSocket;
    private PeerConnectionObserver observer;

    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();

    private static final String TAG = "VideoActivity";

    private static final class SocketListener extends WebSocketListener {

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            Log.i(TAG, text);

            JSONObject result = null;

            try {
                result = new JSONObject(text);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            SessionDescription remoteSdp = null;
            try {
                 remoteSdp = (SessionDescription) result.get("");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            localPeer.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {

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
            }, remoteSdp);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            super.onMessage(webSocket, bytes);
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        auth = FirebaseAuth.getInstance();

        initializeWS();

        iceServers.add(PeerConnection.IceServer.builder("stun.l.google.com:19302")
                .createIceServer());

        observer = new PeerConnectionObserver() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Map<String, String> map = new HashMap<>();
                map.put("type", "candidate");
                map.put("uid", auth.getUid());
                map.put("candidate", iceCandidate.toString());
                webSocket.send(new JSONObject(map).toString());
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
            }
        };

        initializeViews();
        initializePCF();
        initializeAV();
    }

    private void initializeWS() {
        client = new OkHttpClient();
        SocketListener listener = new SocketListener();
        Request request = new Request.Builder().url("http://localhost:9090").build();
        webSocket = client.newWebSocket(request, listener);
        webSocket.send("{'type':'join','uid':'" + auth.getUid() + "'}");
    }

    private void initializePCF() {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableEncryption = true;
        options.disableNetworkMonitor = true;

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(rootBase.getEglBaseContext()))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(rootBase.getEglBaseContext(), true, true))
                .setOptions(options)
                .createPeerConnectionFactory();
    }

    private void initializeViews() {
        localRenderer = findViewById(R.id.localSurfaceView);
        localRenderer.setMirror(true);
        localRenderer.setEnableHardwareScaler(true);
        localRenderer.init(rootBase.getEglBaseContext(), null);

        remoteRenderer = findViewById(R.id.remoteSurfaceView);
    }

    private void initializeAV() {
        MediaConstraints constraints = new MediaConstraints();

        VideoCapturer videoCapturer = getVideoCapturer(new CameraEventsHandler());

        VideoSource localVideoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());

        SurfaceTextureHelper helper = SurfaceTextureHelper.create(Thread.currentThread().getName(), rootBase.getEglBaseContext());
        videoCapturer.initialize(helper, getApplicationContext(), localVideoSource.getCapturerObserver());
        videoCapturer.startCapture(1000, 1000, 30);

        VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack("VidTrack", localVideoSource);
        localVideoTrack.addSink(localRenderer);

        AudioSource localAudioSource = peerConnectionFactory.createAudioSource(constraints);
        AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack("AudTrack", localAudioSource);

        MediaStream localStream = peerConnectionFactory.createLocalMediaStream("Stream");
        localStream.addTrack(localVideoTrack);
        localStream.addTrack(localAudioTrack);

        localPeer.addStream(localStream);
    }

    private void call() {

        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

        localPeer = peerConnectionFactory.createPeerConnection(iceServers, observer);
        localPeer.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

                // send offer to
                Map<String, String> map = new HashMap<>();
                map.put("type", "offer");
                map.put("uid", null); // TODO: INSERT UID OF OTHER USER
                map.put("offer", sessionDescription.toString());
                webSocket.send(new JSONObject(map).toString());

                localPeer.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {

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

    private VideoCapturer getVideoCapturer(CameraVideoCapturer.CameraEventsHandler eventsHandler) {
        VideoCapturer videoCapturer = null;
        CameraEnumerator enumerator = new Camera2Enumerator(this);
        String[] deviceNames = enumerator.getDeviceNames();

        for (String name: deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                videoCapturer = enumerator.createCapturer(name, eventsHandler);
                if (videoCapturer != null) return videoCapturer;
            }
        }

        for (String name: deviceNames) {
            if (!enumerator.isFrontFacing(name)) {
                videoCapturer = enumerator.createCapturer(name, eventsHandler);
                if (videoCapturer != null) return videoCapturer;
            }
        }

        throw new RuntimeException("Failed to Find Camera");
    }
}