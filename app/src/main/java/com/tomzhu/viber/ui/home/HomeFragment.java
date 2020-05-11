package com.tomzhu.viber.ui.home;

import android.os.Bundle;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.tomzhu.viber.ChildEventListener;
import com.tomzhu.viber.R;
import com.tomzhu.viber.RecentViewAdapter;
import com.tomzhu.viber.models.RecentItem;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private RecentViewAdapter recentViewAdapter;

    private FirebaseDatabase db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;

    private ArrayList<RecentItem> recentItems;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        recentItems = new ArrayList<>();

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
                    }
                }
            }
        });

        return root;
    }
}