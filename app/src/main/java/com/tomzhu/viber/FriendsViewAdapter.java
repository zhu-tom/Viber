package com.tomzhu.viber;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.tomzhu.viber.models.FriendItem;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class FriendsViewAdapter extends RecyclerView.Adapter<FriendsViewAdapter.FriendsViewHolder> {
    private ArrayList<FriendItem> items;
    private FriendsViewHolder.OnEnterListener listener;
    private FirebaseDatabase db;

    public FriendsViewAdapter(ArrayList<FriendItem> list, FriendsViewHolder.OnEnterListener listener) {
        items = list;
        this.listener = listener;
        db = FirebaseDatabase.getInstance();
    }

    @NonNull
    @Override
    public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item, parent, false);
        return new FriendsViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendsViewHolder holder, int position) {
        FriendItem currItem = items.get(position);

        db.getReference("/Users/" + currItem.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (snapshot != null && snapshot.getValue() != null) {
                            switch (snapshot.getKey()) {
                                case "username":
                                    holder.setUsername(snapshot.getValue().toString());
                                    break;
                                case "name":
                                    holder.setName(snapshot.getValue().toString());
                                    break;
                                case "photoUrl":
                                    holder.setAvatar(Uri.parse(snapshot.getValue().toString()));
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private OnEnterListener listener;
        private ImageView avatar;
        private TextView username;
        private TextView name;
        private TextView lastMessage;

        public FriendsViewHolder(@NonNull View itemView, OnEnterListener listener) {
            super(itemView);
            this.listener = listener;
            itemView.setOnClickListener(this);

            avatar = itemView.findViewById(R.id.avatar);
            username = itemView.findViewById(R.id.username);
            name = itemView.findViewById(R.id.name);
            lastMessage = itemView.findViewById(R.id.last_message);
        }

        public void setAvatar(Uri avatar) {
            Picasso.get().load(avatar).placeholder(R.drawable.avatar_placeholder).into(this.avatar);
        }

        public void setUsername(String u) {
            username.setText(u);
        }

        public void setName(String n) {
            name.setText(n);
        }

        public void setLastMessage(String l) {
            lastMessage.setText(l);
        }

        @Override
        public void onClick(View v) {
            listener.onEnter(getAdapterPosition());
        }

        public interface OnEnterListener {
            void onEnter(int position);
        }
    }
}
