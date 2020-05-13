package com.tomzhu.viber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tomzhu.viber.models.ChatMessage;

import java.security.Key;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okio.Timeout;

public class ChatActivity extends AppCompatActivity {
    public static final int ANONYMOUS = 0;
    public static final int CONTACT = 1;
    private static final String TAG = "ChatActivity";

    private boolean isAnon;

    private FirebaseDatabase db;
    private FirebaseAuth auth;
    private DatabaseReference chatRef;
    private DatabaseReference peopleRef;

    private String currUsername;
    private String currUid;
    private String currName;
    private String chatId;
    private String otherUid;
    private boolean canSendTyping;
    private String otherName;

    private RecyclerView.Adapter adapter;
    private EditText toSend;
    private Button sendBtn;
    private TextView isUserTyping;

    private ArrayList<ChatMessage> messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        currUid = auth.getUid();
        canSendTyping = true;

        db.getReference("Users").child(currUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    currUsername = dataSnapshot.child("username").getValue().toString();
                    currName = dataSnapshot.child("name").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        messages = new ArrayList<>();

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        adapter = new ChatViewAdapter(messages, isAnon);
        RecyclerView recyclerView = findViewById(R.id.messages);
        recyclerView.setHasFixedSize(false);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);

        isUserTyping = findViewById(R.id.isUserTyping);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            chatId = extras.getString("chatId");
            otherUid = extras.getString("otherUid");
            isAnon = extras.getInt("type") == ANONYMOUS;
        }

        DatabaseReference reference = db.getReference("Chats").child(chatId);
        peopleRef = reference.child("people");
        chatRef = reference.child("messages");

        db.getReference("/Users/" + otherUid + "/name").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    otherName = dataSnapshot.getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        toSend = findViewById(R.id.toSend);
        toSend.setOnKeyListener((v, keyCode, event) -> {
            Log.i(TAG, "onKey " + keyCode + ": " + event.getAction());
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                if (toSend.getText().toString().trim().length() != 0) {
                    sendMessage(toSend.getText().toString(), ChatMessage.Type.TEXT);
                    toSend.setText("");
                    peopleRef.child(currUid).setValue(false);
                }
                return true;
            }
            return false;
        });

        toSend.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (canSendTyping) {
                    canSendTyping = false;
                    peopleRef.child(currUid).setValue(count >= before);

                    new Handler().postDelayed(() -> canSendTyping = true, 200);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        sendBtn = findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(v -> {
            sendMessage(toSend.getText().toString(), ChatMessage.Type.TEXT);
            toSend.setText("");
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (isAnon) {
                getSupportActionBar().setTitle("Random Match");
            }
        }


        peopleRef.child(otherUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Object val = dataSnapshot.getValue();
                    if (val != null) handleTyping(Boolean.parseBoolean(val.toString()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        chatRef.orderByKey().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (dataSnapshot.exists()) {
                    Object mUid = dataSnapshot.child("uid").getValue();
                    Object messageText = dataSnapshot.child("message").getValue();
                    Object name = dataSnapshot.child("name").getValue();
                    Object username = dataSnapshot.child("username").getValue();
                    Object type = dataSnapshot.child("type").getValue();
                    Object status = dataSnapshot.child("status").getValue();
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("status", ChatMessage.Status.READ);
                    dataSnapshot.getRef().updateChildren(map);
                    ChatMessage message;
                    if (messageText != null && name != null && username != null && mUid != null && type != null && status != null) {
                        ChatMessage.Type chatType = ChatMessage.Type.valueOf(type.toString().toUpperCase());
                        if (chatType == ChatMessage.Type.TEXT || (chatType == ChatMessage.Type.LEAVE && isAnon)) {
                            message = new ChatMessage(messageText.toString(), username.toString(), mUid.toString(), name.toString(), chatType, ChatMessage.Status.valueOf(status.toString().toUpperCase()));
                            messages.add(message);
                            adapter.notifyDataSetChanged();
                            recyclerView.scrollToPosition(messages.size()-1);
                        }
                    }
                }
            }
        });
    }

    private void handleTyping(boolean b) {
        if (otherName != null) isUserTyping.setText(b ? otherName + " is typing..." : "");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (isAnon) {
            LinearLayout layout = new LinearLayout(this);
            layout.setGravity(Gravity.CENTER);
            layout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layout.setLayoutParams(params);
            TextView label = new TextView(this);
            label.setText(R.string.leave_conf);
            label.setLayoutParams(params);
            layout.addView(label);

            LinearLayout btnLayout = new LinearLayout(this);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);
            btnLayout.setGravity(Gravity.CENTER);
            btnLayout.setLayoutParams(params);
            btnLayout.setDividerPadding(12);

            Button yesButton = new Button(this);
            yesButton.setLayoutParams(params);
            yesButton.setText(R.string.yes);
            yesButton.setOnClickListener(v -> peopleRef.child(currUid).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("type", "text");
                    map.put("otherUid", otherUid);
                    map.put("chatId", chatId);
                    map.put("datetime", new Date().getTime());
                    DatabaseReference userRef = db.getReference("/Users/"+currUid);
                    userRef.child("recent").push().setValue(map);
                    userRef.child("anonChats").removeValue();
                    sendMessage(currUsername + " has left the chat", ChatMessage.Type.LEAVE);
                    onBackPressed();
                }
            }));

            Button noButton = new Button(this);
            noButton.setLayoutParams(params);
            noButton.setText(R.string.no);

            btnLayout.addView(yesButton);
            btnLayout.addView(noButton);

            layout.addView(btnLayout);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(layout);
            AlertDialog dialog = builder.create();
            dialog.show();

            noButton.setOnClickListener(v -> dialog.dismiss());
        } else {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendMessage(String message, ChatMessage.Type type) {
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("message", message);
        hashMap.put("username", currUsername);
        hashMap.put("uid", currUid);
        hashMap.put("name", currName);
        hashMap.put("type", type);
        hashMap.put("status", ChatMessage.Status.SENT);
        chatRef.push().updateChildren(hashMap);
    }
}
