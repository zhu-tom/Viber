package com.tomzhu.viber.ui.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tomzhu.viber.ChildEventListener;
import com.tomzhu.viber.R;
import com.tomzhu.viber.models.FriendRequest;
import com.tomzhu.viber.models.NotificationItem;

import java.util.ArrayList;

public class NotificationsFragment extends Fragment {
    private static final String TAG = "NotificationsFragment";
    private ArrayList<NotificationItem> items;
    private RecyclerView.Adapter viewAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        items = new ArrayList<>();

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        RecyclerView recyclerView = root.findViewById(R.id.notifications);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(false);

        viewAdapter = new NotificationsViewAdapter(items, db, auth.getUid());
        recyclerView.setAdapter(viewAdapter);

        db.getReference("/Users/" + auth.getUid() + "/friendRequests").addChildEventListener(new ChildEventListener() {
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists()) {
                    Object time = dataSnapshot.child("datetime").getValue();
                    Object uid = dataSnapshot.child("uid").getValue();
                    String key = dataSnapshot.getKey();
                    if (uid != null) {
                        db.getReference("/Users/" + uid.toString()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    Object username = dataSnapshot.child("username").getValue();
                                    Object photoUrl = dataSnapshot.child("photoUrl").getValue();
                                    if (username != null && time != null) {
                                        items.add(new FriendRequest(key, uid.toString(), photoUrl == null ? null : photoUrl.toString(),
                                                username.toString() + " sent you a friend request.",
                                                Long.parseLong(time.toString())));
                                        viewAdapter.notifyDataSetChanged();
                                        Log.i(TAG, items.toString());
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                super.onChildRemoved(dataSnapshot);
                if (dataSnapshot.exists()) {
                    String key = dataSnapshot.getKey();
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).getKey().equals(key)) {
                            items.remove(i);
                            viewAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }
            }
        });

        return root;
    }
}