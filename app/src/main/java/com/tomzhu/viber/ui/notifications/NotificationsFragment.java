package com.tomzhu.viber.ui.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tomzhu.viber.ChildEventListener;
import com.tomzhu.viber.NotificationsViewAdapter;
import com.tomzhu.viber.R;
import com.tomzhu.viber.models.FriendRequest;
import com.tomzhu.viber.models.NotificationItem;

import java.util.ArrayList;

public class NotificationsFragment extends Fragment {
    private static final String TAG = "NotificationsFragment";
    private ArrayList<NotificationItem> items;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        items = new ArrayList<>();

        RecyclerView recyclerView = root.findViewById(R.id.notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(false);

        NotificationsViewAdapter viewAdapter = new NotificationsViewAdapter(items, db, auth.getUid());
        recyclerView.setAdapter(viewAdapter);

        db.getReference("/Users/" + auth.getUid() + "/friendRequests").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    items = new ArrayList<>();
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        Object time = child.child("datetime").getValue();
                        Object uid = child.child("uid").getValue();
                        if (uid != null) {
                            db.getReference("/Users/" + uid.toString()).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        Object username = dataSnapshot.child("username").getValue();
                                        Object photoUrl = dataSnapshot.child("photoUrl").getValue();
                                        if (username != null && time != null) {
                                            items.add(new FriendRequest(uid.toString(), photoUrl == null ? null : photoUrl.toString(),
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return root;
    }
}