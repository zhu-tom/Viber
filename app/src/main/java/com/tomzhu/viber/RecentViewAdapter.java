package com.tomzhu.viber;

import android.media.Image;
import android.net.Uri;
import android.service.autofill.AutofillService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.tomzhu.viber.models.RecentItem;

import org.w3c.dom.Text;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;

public class RecentViewAdapter extends RecyclerView.Adapter<RecentViewAdapter.RecentViewHolder> {
    private ArrayList<RecentItem> list;
    private FirebaseDatabase db;

    public RecentViewAdapter(ArrayList<RecentItem> items, FirebaseDatabase db) {
        list = items;
        this.db = db;
    }

    @NonNull
    @Override
    public RecentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecentViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recent_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecentViewHolder holder, int position) {
        RecentItem currItem = list.get(position);

        holder.setDatetime(currItem.getDatetime());
        holder.setOnAddListener(v -> {
            if (db != null && currItem.getUid() != null) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("uid", currItem.getUid());
                map.put("datetime", new Date().getTime());
                db.getReference("/Users/" + currItem.getUid()).child("friendRequests").push().setValue(map);
            }
        });

        db.getReference("/Users/" + currItem.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot currItem : dataSnapshot.getChildren()) {
                        if (currItem != null && currItem.getValue() != null) {
                            switch (currItem.getKey()) {
                                case "name":
                                    holder.setName(currItem.getValue().toString());
                                    break;
                                case "username":
                                    holder.setUsername(currItem.getValue().toString());
                                    break;
                                case "photoUrl":
                                    holder.setAvatar(currItem.getValue().toString());
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
        return list.size();
    }

    public static class RecentViewHolder extends RecyclerView.ViewHolder {

        private TextView name;
        private TextView username;
        private TextView datetime;
        private ImageView avatar;
        private Button addFriend;

        private String uid;
        private FirebaseDatabase db;

        public RecentViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            username = itemView.findViewById(R.id.username);
            datetime = itemView.findViewById(R.id.datetime);
            avatar = itemView.findViewById(R.id.avatar);
            addFriend = itemView.findViewById(R.id.add_friend);
            addFriend.setOnClickListener(v -> {
                if (db != null && uid != null) {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("uid", uid);
                    map.put("datetime", new Date().getTime());
                    db.getReference("/Users/" + uid).child("friendRequests").push().setValue(map);
                }
            });
        }

        public void setOnAddListener(View.OnClickListener listener) {
            addFriend.setOnClickListener(listener);
        }

        public void setAvatar(String url) {
            Picasso.get().load(Uri.parse(url)).placeholder(R.drawable.avatar_placeholder).into(this.avatar);
        }

        public void setDatetime(long datetime) {
            this.datetime.setText(new Date(datetime).toString());
        }

        public void setName(String name) {
            this.name.setText(name);
        }

        public void setUsername(String username) {
            this.username.setText(username);
        }
    }
}
