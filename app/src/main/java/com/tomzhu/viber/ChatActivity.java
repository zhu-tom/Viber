package com.tomzhu.viber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tomzhu.viber.models.ChatMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    public static final int ANONYMOUS = 0;
    public static final int CONTACT = 1;

    private boolean isAnon;

    private FirebaseDatabase db;
    private FirebaseAuth auth;
    private DatabaseReference chatRef;
    private DatabaseReference peopleRef;

    private String currUsername;
    private String currUid;
    private String currName;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView.Adapter adapter;
    private EditText toSend;
    private Button sendBtn;

    private ArrayList<ChatMessage> messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        currUid = auth.getCurrentUser().getUid();

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

        layoutManager = new LinearLayoutManager(this);
        adapter = new ChatViewAdapter(messages);
        recyclerView = findViewById(R.id.messages);
        recyclerView.setHasFixedSize(false);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);

        toSend = findViewById(R.id.toSend);
        toSend.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    sendMessage(toSend.getText().toString(), currUid);
                    toSend.setText("");
                    return true;
                }
                return false;
            }
        });

        sendBtn = findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(toSend.getText().toString(), currUid);
                toSend.setText("");
            }
        });

        Bundle extras = getIntent().getExtras();
        String chatId = extras.getString("chatId");
        isAnon = extras.getInt("type") == ANONYMOUS;

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (isAnon) {
            getSupportActionBar().setTitle("Random Match");
        }

        DatabaseReference reference = db.getReference("Chats").child(chatId);

        peopleRef = reference.child("people");
        chatRef = reference.child("messages");

        chatRef.orderByKey().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (dataSnapshot.exists()) {
                    Object mUid = dataSnapshot.child("uid").getValue();
                    ChatMessage message;
                    if (mUid == null) {
                        message = new ChatMessage(dataSnapshot.child("message").getValue().toString(),
                                isAnon ? null:dataSnapshot.child("username").getValue().toString(),
                                    null,
                                        dataSnapshot.child("name").getValue().toString());
                    } else {
                        message = new ChatMessage(dataSnapshot.child("message").getValue().toString(),
                                isAnon ? null:dataSnapshot.child("username").getValue().toString(),
                                    mUid.toString(),
                                        dataSnapshot.child("name").getValue().toString());
                    }
                    messages.add(message);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        LinearLayout layout = new LinearLayout(this);
        layout.setGravity(Gravity.CENTER);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(params);
        TextView label = new TextView(this);
        label.setText("Are you sure you want to leave?");
        label.setLayoutParams(params);
        layout.addView(label);

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER);
        btnLayout.setLayoutParams(params);
        btnLayout.setDividerPadding(12);

        Button yesButton = new Button(this);
        yesButton.setLayoutParams(params);
        yesButton.setText("Yes");
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                peopleRef.child(currUid).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            sendMessage((isAnon ? "Anonymous":currUsername) + " has left the chat", null);
                            onBackPressed();
                        }
                    }
                });
            }
        });

        Button noButton = new Button(this);
        noButton.setLayoutParams(params);
        noButton.setText("No");

        btnLayout.addView(yesButton);
        btnLayout.addView(noButton);

        layout.addView(btnLayout);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        AlertDialog dialog = builder.create();
        dialog.show();
        return super.onOptionsItemSelected(item);
    }

    private void sendMessage(String message, String uid) {
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("message", message);
        hashMap.put("username", isAnon ? null : currUsername);
        hashMap.put("uid", uid);
        hashMap.put("name", currName);
        chatRef.push().updateChildren(hashMap);
    }
}
