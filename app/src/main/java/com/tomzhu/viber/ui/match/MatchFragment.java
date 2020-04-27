package com.tomzhu.viber.ui.match;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tomzhu.viber.ChatActivity;
import com.tomzhu.viber.R;
import com.tomzhu.viber.VideoActivity;

import java.util.HashMap;
import java.util.Iterator;

public class MatchFragment extends Fragment {
    private final static String TAG = "MatchFragment";

    private MatchViewModel mViewModel;
    private Button matchBtn;
    private Button matchVidBtn;
    private FirebaseDatabase db;
    private FirebaseAuth auth;
    private AlertDialog dialog;
    private DatabaseReference currUser;
    private boolean firstTime = true;

    private interface Callback {
        void goToActivity(String key, int type);
    }

    public static MatchFragment newInstance() {
        return new MatchFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_match, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(MatchViewModel.class);
        // TODO: Use the ViewModel
        db = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        currUser = db.getReference("/Users/" + auth.getUid());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        matchBtn = view.findViewById(R.id.match_text_button);

        matchVidBtn = view.findViewById(R.id.match_video_button);
        matchVidBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findMatch(db.getReference("VideoQueue"), db.getReference("VideoChats"), "anonVideos",
                        ((key, type) -> MatchFragment.this.goToActivity(VideoActivity.class, key, type)));
            }
        });

        matchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findMatch(db.getReference("ChatQueue"), db.getReference("Chats"), "anonChats",
                        (key, type) -> MatchFragment.this.goToActivity(ChatActivity.class, key, type));
            }
        });
    }

    private void findMatch(DatabaseReference queue, DatabaseReference dbRef, String userChats, Callback callback) {
        showDialog(getString(R.string.finding_match_label));

        final DatabaseReference pushPos = queue.push();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                pushPos.removeValue();
            }
        });


        final ValueEventListener queueMatchListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (firstTime) {
                    firstTime = false;
                } else {
                    if (dataSnapshot.exists()) {
                        Log.i(TAG, "Someone else matched");
                        currUser.child(userChats).removeEventListener(this);
                        dialog.dismiss();
                        pushPos.removeValue();
                        Iterator<DataSnapshot> itr = dataSnapshot.getChildren().iterator();
                        DataSnapshot currItem = itr.next();
                        while (itr.hasNext()) {
                            currItem = itr.next();
                        }
                        callback.goToActivity(currItem.getKey(), ChatActivity.ANONYMOUS);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        final Query query = queue.orderByKey().limitToFirst(1);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.i(TAG, "Found Existing Queue Item");
                    query.removeEventListener(this);

                    DataSnapshot firstChild = dataSnapshot.getChildren().iterator().next();
                    final String itemUid = firstChild.child("uid").getValue().toString();
                    firstChild.getRef().removeValue();
                    pushPos.removeValue();

                    HashMap<String, Object> people = new HashMap<>();
                    people.put(auth.getUid(), false);
                    people.put(itemUid, false);

                    final DatabaseReference chatRef = dbRef.push();

                    chatRef.child("people").updateChildren(people).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            dialog.dismiss();
                            String chatKey = chatRef.getKey();
                            db.getReference("/Users/" + itemUid).child(userChats).child(chatKey).setValue(true);
                            currUser.child(userChats).child(chatKey).setValue(true);
                            callback.goToActivity(chatKey, ChatActivity.ANONYMOUS);
                        }
                    });
                } else {
                    query.removeEventListener(this);
                    addToQueue(pushPos, queueMatchListener, userChats);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addToQueue(DatabaseReference pushPos, final ValueEventListener queueMatchListener, String userChats) {
        Log.i(TAG, "Added to Queue");
        pushPos.child("uid").setValue(auth.getCurrentUser().getUid()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (!task.isSuccessful()) {
                    Toast.makeText(getContext(), "Error adding to queue.", Toast.LENGTH_SHORT).show();
                } else {
                    currUser.child(userChats).addValueEventListener(queueMatchListener);
                }
            }
        });
    }

    private void showDialog(String text) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(params);

        TextView message = new TextView(getContext());
        message.setText(text);
        message.setLayoutParams(params);
        layout.addView(message);

        ProgressBar bar = new ProgressBar(getContext(), null, R.attr.indeterminateProgressStyle);
        bar.setLayoutParams(params);
        bar.setIndeterminate(true);
        bar.setVisibility(View.VISIBLE);
        layout.addView(bar);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(layout);
        dialog = builder.create();
        dialog.show();
    }

    private void goToActivity(Class<?> activity, String chatKey, int anonymous) {
        showDialog("Connecting...");
        Bundle bundle = new Bundle();
        bundle.putString("chatId", chatKey);
        bundle.putInt("type", anonymous);
        Intent intent = new Intent(getContext(), activity);
        intent.putExtras(bundle);
        startActivity(intent);
        dialog.dismiss();
    }
}
