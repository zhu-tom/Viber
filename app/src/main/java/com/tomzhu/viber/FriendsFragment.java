package com.tomzhu.viber;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tomzhu.viber.models.FriendItem;

import java.util.ArrayList;
import java.util.HashMap;

public class FriendsFragment extends Fragment {

    private RecyclerView friendsView;
    private FriendsViewAdapter adapter;
    private ArrayList<FriendItem> items;
    private FirebaseDatabase db;
    private FirebaseAuth auth;

    private void goToActivity(Class<?> activity, String chatId, String otherUid) {
        Intent intent = new Intent(getContext(), activity);
        Bundle extras = new Bundle();
        extras.putString("chatId", chatId);
        extras.putString("otherUid", otherUid);
        extras.putInt("type", ChatActivity.CONTACT);
        extras.putBoolean("isCaller", true);
        intent.putExtras(extras);
        startActivity(intent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_friends, container, false);

        items = new ArrayList<>();
        db = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        friendsView = root.findViewById(R.id.friendsView);
        friendsView.setHasFixedSize(false);
        friendsView.setNestedScrollingEnabled(false);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        friendsView.setLayoutManager(layoutManager);
        FriendsViewAdapter.FriendsViewHolder.OnEnterListener listener = position -> {
            FriendItem currItem = items.get(position);

            DatabaseReference peopleRef = db.getReference("/Chats/" + currItem.getChatId() + "/people");
            peopleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        goToActivity(ChatActivity.class, currItem.getChatId(), currItem.getUid());
                    } else {
                        HashMap<String, Boolean> map = new HashMap<>();
                        map.put(auth.getUid(), false);
                        map.put(currItem.getUid(), false);
                        peopleRef.setValue(map).addOnSuccessListener(aVoid -> {
                            goToActivity(ChatActivity.class, currItem.getChatId(), currItem.getUid());
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        };
        adapter = new FriendsViewAdapter(items, listener);
        friendsView.setAdapter(adapter);

        db.getReference("/Users/" + auth.getUid() + "/friends").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                super.onChildAdded(dataSnapshot, s);
                if (dataSnapshot.exists()) {
                    String uid = dataSnapshot.getKey();
                    Object chatId = dataSnapshot.child("chatId").getValue();
                    if (chatId != null) {
                        items.add(new FriendItem(uid, chatId.toString()));
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                super.onChildRemoved(dataSnapshot);
                if (dataSnapshot.exists()) {
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).getUid().equals(dataSnapshot.getKey())) {
                            items.remove(i);
                            adapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }
            }
        });

        return root;
    }
}
