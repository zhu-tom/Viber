package com.tomzhu.viber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.ActivityNavigator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.CandidatePairChangeEvent;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class VideoActivity extends AppCompatActivity {
    private static final int REQUEST_VID_AUD_CODE = 0;
    private FirebaseAuth auth;
    private FirebaseDatabase db;
    private SurfaceViewRenderer localRenderer;
    private SurfaceViewRenderer remoteRenderer;
    private PeerConnection localPeer;
    private AudioTrack localAudioTrack;
    private VideoTrack localVideoTrack;
    private PeerConnectionFactory peerConnectionFactory;
    private EglBase rootBase = EglBase.create();
    private WebSocketClient webSocket;
    private String otherUid;
    private String chatKey;
    private Gson gson = new Gson();
    private boolean offered = false;
    private boolean hasAnswer;
    private SdpObserverPlaceholder sdpObserverPlaceholder = new SdpObserverPlaceholder();
    private VideoCapturer videoCapturer;
    private Button joinCall;
    private boolean isCaller;
    private List<IceCandidate> iceCandidates;

    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();

    private static final String TAG = "VideoActivity";

    private static class ProxyVideoSink implements VideoSink {
        private VideoSink target;
        @Override synchronized public void onFrame(VideoFrame frame) {
            if (target == null) {
                Logging.d(TAG, "Dropping frame in proxy because target is null.");
                return;
            }
            target.onFrame(frame);
        }
        synchronized void setTarget(VideoSink target) { this.target = target; }
    }

    private void handleOffer(SessionDescription offer, String uid) {
        createPeerConnection();

        localPeer.setRemoteDescription(new SdpObserverPlaceholder() {
            @Override
            public void onSetSuccess() {
                super.onSetSuccess();
                Log.i(TAG, "set remote sdp");
            }

            @Override
            public void onSetFailure(String s) {
                super.onSetFailure(s);
                Log.i(TAG, s);
            }
        }, offer);

        localPeer.createAnswer(new SdpObserverPlaceholder() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);

                localPeer.setLocalDescription(sdpObserverPlaceholder, sessionDescription);

                Map<String, Object> data = new HashMap<>();
                data.put("type", "answer");
                data.put("answer", sessionDescription);
                data.put("uid", uid);
                webSocket.send(gson.toJson(data));

                Log.i(TAG, "sent answer");
            }

            @Override
            public void onCreateFailure(String s) {
                super.onCreateFailure(s);
                Log.i(TAG, s);
            }
        }, new MediaConstraints());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        Log.i(TAG, "onCreate");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();

        iceCandidates = new ArrayList<>();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            otherUid = extras.getString("otherUid");
            chatKey = extras.getString("chatId");
            isCaller = extras.getBoolean("isCaller");
            hasAnswer = !isCaller;
        }

        initializeViews();
        initializePCF();
        initializeAV();
        initializeWS();

        joinCall = findViewById(R.id.join_call);
        joinCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
            }
        });

    }

    private void addStreamsToPeer() {
        MediaStream localStream = peerConnectionFactory.createLocalMediaStream("Stream");
        localStream.addTrack(localVideoTrack);
        localStream.addTrack(localAudioTrack);

        localPeer.addStream(localStream);
    }

    private void initializeWS()  {
        try {
            webSocket = new WebSocketClient(new URI("ws://10.0.2.2:9090")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.i(TAG, "Connected to " + getURI());
                    Map<String, String> data = new HashMap<>();
                    data.put("type", "join");
                    data.put("uid", auth.getUid());
                    send(gson.toJson(data));
                }

                @Override
                public void onMessage(String message) {
                    JsonObject gsonRes = gson.fromJson(message, JsonObject.class);

                    String type = gsonRes.get("type").getAsString();
                    switch (type) {
                        case "connected":
                            if (gsonRes.get("success").getAsBoolean()) {
                                Log.i(TAG, "connection successful");
                                if (!offered && otherUid != null && isCaller) {
                                    call();
                                }
                            } else {
                                Log.i(TAG, "connection failed");
                            }
                            break;
                        case "offer":
                            offered = true;
                            Log.i(TAG, "received offer");
                            JsonObject offer = gsonRes.getAsJsonObject("offer");
                            handleOffer(gson.fromJson(offer, SessionDescription.class), gsonRes.get("name").getAsString());
                            break;
                        case "answer":
                            Log.i(TAG, "received answer");
                            SessionDescription remoteSDP = gson.fromJson(gsonRes.getAsJsonObject("answer"), SessionDescription.class);
                            answer(remoteSDP);
                            break;
                        case "candidate":
                            IceCandidate candidate = gson.fromJson(gsonRes.getAsJsonObject("candidate"), IceCandidate.class);
                            Log.i(TAG, "received " + candidate.toString());
                            localPeer.addIceCandidate(candidate);
                            break;
                        case "leave":
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {

                }

                @Override
                public void onError(Exception ex) {
                    throw new RuntimeException(ex);
                }
            };

            webSocket.connect();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void answer(SessionDescription remoteSDP) {
        hasAnswer = true;
        localPeer.setRemoteDescription(new SdpObserverPlaceholder() {
            @Override
            public void onSetSuccess() {
                super.onSetSuccess();
                Log.i(TAG, "set remote sdp");
                for (IceCandidate candidate : iceCandidates) {
                    sendCandidate(candidate);
                }
            }

            @Override
            public void onSetFailure(String s) {
                super.onSetFailure(s);
                Log.i(TAG, s);
            }
        }, remoteSDP);
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

    private void createPeerConnection() {
        iceServers.add(PeerConnection.IceServer.builder("turn:numb.viagenie.ca")
                .setUsername("webrtc@live.com")
                .setPassword("muazkh").createIceServer());

        PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);
        configuration.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        configuration.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        configuration.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        configuration.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        configuration.keyType = PeerConnection.KeyType.ECDSA;

        PeerConnectionObserver observer = new PeerConnectionObserver() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);

                if (!hasAnswer) {
                    iceCandidates.add(iceCandidate);
                } else {
                    sendCandidate(iceCandidate);
                }
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                super.onIceConnectionChange(iceConnectionState);
                Log.i(TAG, "ice connection state: " + iceConnectionState.toString());
            }

            @Override
            public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                Log.i(TAG, "connection state: " + newState.toString());
            }

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                super.onSignalingChange(signalingState);
                Log.i(TAG, "signaling state: " + signalingState.toString());
            }

            @Override
            public void onStandardizedIceConnectionChange(PeerConnection.IceConnectionState newState) {
                Log.i(TAG, "standardized state: " + newState.toString());
            }

            @Override
            public void onSelectedCandidatePairChanged(CandidatePairChangeEvent event) {
                Log.i(TAG, "candidate pair changed: " + event.reason);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                super.onIceGatheringChange(iceGatheringState);
                Log.i(TAG, "gathering state: " + iceGatheringState.toString());
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                Log.i(TAG, "got stream");
                remoteRenderer.setVisibility(View.VISIBLE);
                mediaStream.videoTracks.get(0).addSink(remoteRenderer);
                //mediaStream.audioTracks.get(0); // TODO: ADD AUDIO
            }
        };

        localPeer = peerConnectionFactory.createPeerConnection(configuration, observer);

        addStreamsToPeer();
    }

    private void sendCandidate(IceCandidate iceCandidate) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "candidate");
        map.put("uid", otherUid);
        map.put("candidate", iceCandidate);
        webSocket.send(gson.toJson(map));
        Log.i(TAG, "sent " + iceCandidate.toString());
    }

    private void initializeAV() {
        videoCapturer = getVideoCapturer(new CameraEventsHandler());

        MediaConstraints constraints = new MediaConstraints();

        VideoSource localVideoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());

        SurfaceTextureHelper helper = SurfaceTextureHelper.create(Thread.currentThread().getName(), rootBase.getEglBaseContext());
        videoCapturer.initialize(helper, getApplicationContext(), localVideoSource.getCapturerObserver());
        videoCapturer.startCapture(1000, 1000, 30);

        localVideoTrack = peerConnectionFactory.createVideoTrack("VidTrack", localVideoSource);
        localVideoTrack.addSink(localRenderer);

        AudioSource localAudioSource = peerConnectionFactory.createAudioSource(constraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("AudTrack", localAudioSource);
    }

    private void call() {

        offered = true;

        createPeerConnection();

        MediaConstraints callConstraints = new MediaConstraints();
        callConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        callConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        localPeer.createOffer(new SdpObserverPlaceholder() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);

                localPeer.setLocalDescription(new SdpObserverPlaceholder(), sessionDescription);
                // send offer to
                Map<String, Object> map = new HashMap<>();
                map.put("type", "offer");
                map.put("uid", otherUid);
                map.put("offer", sessionDescription);
                webSocket.send(gson.toJson(map));

                Log.i(TAG, "calling " + otherUid);
            }
        }, callConstraints);


    }

    private VideoCapturer getVideoCapturer(CameraVideoCapturer.CameraEventsHandler eventsHandler) {
        VideoCapturer videoCapturer;
        if (ContextCompat.checkSelfPermission(VideoActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(VideoActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(VideoActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQUEST_VID_AUD_CODE);
        } else {
            CameraEnumerator enumerator = new Camera2Enumerator(this);
            String[] deviceNames = enumerator.getDeviceNames();

            for (String name: deviceNames) {
                if (enumerator.isFrontFacing(name)) {
                    videoCapturer = enumerator.createCapturer(name, eventsHandler);
                    if (videoCapturer != null) {
                        return videoCapturer;
                    }
                }
            }

            for (String name: deviceNames) {
                if (!enumerator.isFrontFacing(name)) {
                    videoCapturer = enumerator.createCapturer(name, eventsHandler);
                    if (videoCapturer != null) return videoCapturer;
                }
            }
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_VID_AUD_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    videoCapturer = getVideoCapturer(new CameraEventsHandler());
                    if (videoCapturer != null) initializeAV();
                } else {
                    Log.i(TAG, "Permission denied");
                }
                return;
            default:
                break;
        }
    }
}