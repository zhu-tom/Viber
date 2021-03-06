package com.tomzhu.viber;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.provider.ContactsContract;
import android.service.autofill.AutofillService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
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
    private static final String TAG = "RecentViewAdapter";
    private ArrayList<RecentItem> list;
    private FirebaseDatabase db;
    private String currUid;

    public RecentViewAdapter(ArrayList<RecentItem> items, FirebaseDatabase db, String currUid) {
        list = items;
        this.db = db;
        this.currUid = currUid;
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
        holder.setType(currItem.getType());
        holder.setRemoveRecent(v -> {
            db.getReference("/Users/" + currUid + "/recent").orderByChild("chatId").equalTo(currItem.getChatId()).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    super.onChildAdded(dataSnapshot, s);
                    if (dataSnapshot.exists()) {
                        dataSnapshot.getRef().removeValue();
                    }
                }
            });
            if (currItem.getType() == RecentItem.ChatType.TEXT) {
                db.getReference("/Chats/" + currItem.getChatId()).removeValue();
            }
        });

        DatabaseReference friendReqRef = db.getReference("/Users/" + currItem.getUid() + "/friendRequests");
        friendReqRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Object requestUid = snapshot.child("uid").getValue();
                        if (requestUid != null && requestUid.toString().equals(currUid)) {
                            setRemoveListener(holder, snapshot.getRef());
                            return;
                        }
                    }
                }
                setAddListener(holder, currItem);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        db.getReference("/Users/" + currItem.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    boolean hasAvatar = false;
                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        if (item != null && item.getValue() != null) {
                            switch (item.getKey()) {
                                case "name":
                                    holder.setName(item.getValue().toString());
                                    break;
                                case "username":
                                    holder.setUsername(item.getValue().toString());
                                    break;
                                case "photoUrl":
                                    hasAvatar = true;
                                    holder.setAvatar(item.getValue().toString());
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    if (!hasAvatar) {
                        holder.setAvatar(R.drawable.avatar_placeholder);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setRemoveListener(RecentViewHolder holder, DatabaseReference toRemove) {
        holder.setButtonImg(R.drawable.baseline_hourglass_empty_white_18dp);
        holder.setOnAddListener(v -> {
            toRemove.removeValue();
        });
    }

    private void setAddListener(RecentViewHolder holder, RecentItem currItem) {
        holder.setButtonImg(R.drawable.baseline_person_add_white_18dp);
        holder.setOnAddListener(v -> {
            db.getReference("/Users/" + currUid + "/friendRequests").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Object uid = snapshot.child("uid").getValue();
                            snapshot.getRef().removeValue();
                            if (uid != null) {
                                if (uid.toString().equals(currItem.getUid())) { // other user already sent request
                                    HashMap<String, String> map = new HashMap<>();
                                    map.put("chatId", currItem.getChatId());
                                    db.getReference("/Users/" + currUid + "/friends/" + currItem.getUid()).setValue(map);
                                    db.getReference("/Users/" + currItem.getUid() + "/friends/" + currUid).setValue(map);
                                    return;
                                }
                            }
                        }
                        // add request
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("uid", currUid);
                        map.put("datetime", new Date().getTime());
                        map.put("chatId", currItem.getChatId());
                        DatabaseReference pushPos = db.getReference("/Users/" + currItem.getUid() + "/friendRequests").push();
                        pushPos.setValue(map);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
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
        private ImageButton addFriend;
        private TextView type;
        private ImageButton removeRecent;

        public RecentViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            username = itemView.findViewById(R.id.username);
            datetime = itemView.findViewById(R.id.datetime);
            avatar = itemView.findViewById(R.id.avatar);
            type = itemView.findViewById(R.id.chatType);
            addFriend = itemView.findViewById(R.id.add_friend);

            removeRecent = itemView.findViewById(R.id.removeRecent);
        }

        public void setType(RecentItem.ChatType type) {
            this.type.setText(type == RecentItem.ChatType.TEXT ? "Text":"Video");
        }

        public void setRemoveRecent(View.OnClickListener listener) {
            removeRecent.setOnClickListener(listener);
        }

        public void setButtonImg(int drawable) {
            switch (drawable) {
                case R.drawable.baseline_person_add_white_18dp:
                    addFriend.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ff669900")));
                    break;
                case R.drawable.baseline_hourglass_empty_white_18dp:
                    addFriend.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#aaaaaa")));
                    break;
                default:
                    break;
            }
            addFriend.setImageResource(drawable);
        }

        public void setOnAddListener(View.OnClickListener listener) {
            addFriend.setOnClickListener(listener);
        }

        public void setAvatar(String url) {
            Picasso.get().load(Uri.parse(url)).placeholder(R.drawable.avatar_placeholder).into(this.avatar);
        }

        public void setAvatar(int avatar) {
            Picasso.get().load(avatar).placeholder(R.drawable.avatar_placeholder).into(this.avatar);
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
