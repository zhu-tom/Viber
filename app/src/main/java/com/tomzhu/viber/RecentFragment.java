package com.tomzhu.viber;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.tomzhu.viber.models.RecentItem;

import java.util.ArrayList;

public class RecentFragment extends Fragment {
    private RecentViewAdapter recentViewAdapter;

    private FirebaseDatabase db;
    private FirebaseAuth auth;

    private final static String TAG = "RecentFragment";

    private ArrayList<RecentItem> recentItems;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_recent, container, false);

        db = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        recentItems = new ArrayList<>();
        Log.i(TAG, "reset list");

        recentViewAdapter = new RecentViewAdapter(recentItems, db, auth.getUid());
        RecyclerView recyclerView = root.findViewById(R.id.recent_items);
        recyclerView.setAdapter(recentViewAdapter);
        recyclerView.setHasFixedSize(false);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db.getReference("/Users/" + auth.getUid() + "/recent").orderByKey().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                super.onChildAdded(dataSnapshot, s);
                if (dataSnapshot.exists()) {
                    Object type = dataSnapshot.child("type").getValue();
                    Object chatId = dataSnapshot.child("chatId").getValue();
                    Object otherUid = dataSnapshot.child("otherUid").getValue();
                    Object datetime = dataSnapshot.child("datetime").getValue();

                    if (type != null && chatId != null && otherUid != null && datetime != null) {
                        recentItems.add(new RecentItem(RecentItem.ChatType.valueOf(type.toString().toUpperCase()),
                                otherUid.toString(), chatId.toString(), Long.parseLong(datetime.toString())));
                        recentViewAdapter.notifyDataSetChanged();
                        Log.i(TAG, "child added");
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                super.onChildRemoved(dataSnapshot);
                if (dataSnapshot.exists()) {
                    Object chatId = dataSnapshot.child("chatId").getValue();
                    if (chatId != null) {
                        for (int i = 0; i < recentItems.size(); i++) {
                            if (recentItems.get(i).getChatId().equals(chatId)) {
                                recentItems.remove(i);
                                recentViewAdapter.notifyDataSetChanged();
                                break;
                            }
                        }
                    }
                }
            }
        });

        return root;
    }
}
