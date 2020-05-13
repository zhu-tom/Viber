package com.tomzhu.viber;

import android.media.Image;
import android.net.Uri;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.tomzhu.viber.models.FriendRequest;
import com.tomzhu.viber.models.NotificationItem;

import java.sql.ClientInfoStatus;
import java.util.ArrayList;
import java.util.Date;

public class NotificationsViewAdapter extends RecyclerView.Adapter<NotificationsViewAdapter.NotificationsViewHolder> {
    private static final String TAG = "NotificationsViewAdapter";
    private ArrayList<NotificationItem> items;
    private FirebaseDatabase db;
    private String currUid;
    private ChildEventListener childRemover;

    private static final int FRIEND_REQUEST = 0;

    public NotificationsViewAdapter(ArrayList<NotificationItem> items, FirebaseDatabase db, String currUid) {
        this.items = items;
        this.db = db;
        this.currUid = currUid;

        childRemover = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                super.onChildAdded(dataSnapshot, s);
                if (dataSnapshot.exists()) {
                    dataSnapshot.getRef().removeValue();
                }
            }
        };
    }

    @NonNull
    @Override
    public NotificationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layout = new LinearLayout(parent.getContext());
        switch (viewType) {
            case FRIEND_REQUEST:
                layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_request, parent, false);
                break;
            default:
                break;
        }
        return new NotificationsViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationsViewHolder holder, int position) {
        NotificationItem item = items.get(position);

        switch (getItemViewType(position)) {
            case FRIEND_REQUEST:
                FriendRequest friendRequest = (FriendRequest) item;
                holder.setMessage(friendRequest.getMessage());
                holder.setTime(friendRequest.getTime());
                holder.setAvatar(friendRequest.getPhotoUrl());
                holder.setAcceptListener(v -> addFriend(friendRequest));
                holder.setDeclineListener(v -> removeRequest(friendRequest, () -> {}));
                break;
            default:
                break;
        }
    }

    private void removeRequest(FriendRequest item, Runnable callback) {
        db.getReference("/Users/" + currUid + "/friendRequests/" + item.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    dataSnapshot.getRef().removeValue();
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).getKey().equals(item.getKey())) {
                            items.remove(i);
                            notifyDataSetChanged();
                            break;
                        }
                    }
                    callback.run();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addFriend(FriendRequest item) {
        removeRequest(item, () -> {
            db.getReference("/Users/"+currUid+"/friends/"+item.getUid()).setValue(true);
            db.getReference("/Users/"+currUid+"/recent").orderByChild("otherUid").equalTo(item.getUid()).addChildEventListener(childRemover);
            db.getReference("/Users/"+item.getUid()+"/friends/"+currUid).setValue(true);
            db.getReference("/Users/"+item.getUid()+"/recent").orderByChild("otherUid").equalTo(currUid).addChildEventListener(childRemover);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        NotificationItem currItem = items.get(position);
        if (currItem instanceof FriendRequest) {
            return FRIEND_REQUEST;
        }
        return -1;
    }

    public static class NotificationsViewHolder extends RecyclerView.ViewHolder {

        private TextView message;
        private TextView time;
        private ImageButton accept;
        private ImageButton decline;
        private ImageView avatar;

        public NotificationsViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.message);
            time = itemView.findViewById(R.id.time);
            accept = itemView.findViewById(R.id.accept);
            decline = itemView.findViewById(R.id.decline);
            avatar = itemView.findViewById(R.id.avatar);
        }

        public void setMessage(String message) {
            this.message.setText(message);
        }

        public void setTime(long time) {
            this.time.setText(new Date(time).toString());
        }

        public void setAcceptListener(View.OnClickListener listener) {
            accept.setOnClickListener(listener);
        }

        public void setDeclineListener(View.OnClickListener listener) {
            decline.setOnClickListener(listener);
        }

        public void setAvatar(String url) {
            if (url != null) Picasso.get().load(Uri.parse(url)).placeholder(R.drawable.avatar_placeholder).into(avatar);
            else Picasso.get().load(R.drawable.avatar_placeholder).into(avatar);
        }
    }
}
