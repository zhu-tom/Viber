package com.tomzhu.viber;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.se.omapi.Session;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
    private MediaConstraints callConstraints;
    private String otherUid;
    private Gson gson = new Gson();
    private SdpObserverPlaceholder sdpObserverPlaceholder = new SdpObserverPlaceholder();

    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();

    private static final String TAG = "VideoActivity";

    private final class SocketListener extends WebSocketListener {

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            Log.i(TAG, text);

            JSONObject result = new JSONObject();
            JsonObject gsonRes = gson.fromJson(text, JsonObject.class);

            try {
                result = new JSONObject(text);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String type = null;
            try {
                 type = (String) result.get("type");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            switch (type) {
                case "connected":
                    try {
                        if ((boolean) result.get("success")) {
                            Toast.makeText(VideoActivity.this, "Connection Successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(VideoActivity.this, "Connection Failed", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                case "offer":
                    try {
                        handleOffer((String) result.get("offer"), (String) result.get("uid"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case "answer":
                    JsonObject sdpObject = gsonRes.getAsJsonObject("answer");
                    localPeer.setRemoteDescription(sdpObserverPlaceholder, gson.fromJson(sdpObject, SessionDescription.class));
                    break;
                case "candidate":
                    JsonObject iceObject = gsonRes.getAsJsonObject("candidate");
                    localPeer.addIceCandidate(gson.fromJson(iceObject, IceCandidate.class));
                    break;
                case "leave":
                    break;
                default:
                    break;
            }
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

    private void handleOffer(String offer, String uid) {
        localPeer.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                localPeer.createAnswer(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        localPeer.setLocalDescription(sdpObserverPlaceholder, sessionDescription);

                        Map<String, Object> data = new HashMap<>();
                        data.put("type", "answer");
                        data.put("answer", sessionDescription);
                        data.put("uid", uid);

                        webSocket.send(gson.toJson(data));
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
                }, callConstraints);
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
        }, new SessionDescription(SessionDescription.Type.OFFER, offer));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        callConstraints = new MediaConstraints();
        callConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        callConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

        auth = FirebaseAuth.getInstance();

        otherUid = getIntent().getExtras().getString("otherUid");

        initializeWS();

        iceServers.add(PeerConnection.IceServer.builder("stun.l.google.com:19302")
                .createIceServer());

        observer = new PeerConnectionObserver() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Map<String, Object> map = new HashMap<>();
                map.put("type", "candidate");
                map.put("uid", auth.getUid());
                map.put("candidate", iceCandidate);

                webSocket.send(gson.toJson(map));
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                mediaStream.videoTracks.get(0).addSink(remoteRenderer);
                mediaStream.audioTracks.get(0); // TODO: ADD AUDIO
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
        Map<String, String> data = new HashMap<>();
        data.put("type", "join");
        data.put("uid", auth.getUid());
        webSocket.send(gson.toJson(data));
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
        remoteRenderer.setMirror(true);
        remoteRenderer.setEnableHardwareScaler(true);
        remoteRenderer.init(rootBase.getEglBaseContext(), null);
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

        localPeer = peerConnectionFactory.createPeerConnection(iceServers, observer);
        localPeer.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

                // send offer to
                Map<String, Object> map = new HashMap<>();
                map.put("type", "offer");
                map.put("uid", otherUid); // TODO: INSERT UID OF OTHER USER
                map.put("offer", sessionDescription);
                webSocket.send(gson.toJson(map));

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
        }, callConstraints);


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