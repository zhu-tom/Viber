package com.tomzhu.viber;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.measurement.module.Analytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tomzhu.viber.models.ChatMessage;

import java.util.ArrayList;
import java.util.HashMap;

public class ChatViewAdapter extends RecyclerView.Adapter<ChatViewAdapter.ChatViewHolder> {
    private ArrayList<ChatMessage> messages;
    private String currUid;
    private HashMap<String, String> avatars = new HashMap<>();
    private boolean isAnon;

    private static final int SENDER_TOP = 0;
    private static final int RECEIVER_TOP = 1;
    private static final int ANNOUNCEMENT = 2;
    private static final int SENDER_CONT = 3;
    private static final int SENDER_END = 4;
    private static final int RECEIVER_CONT = 5;
    private static final int RECEIVER_END = 6;

    public ChatViewAdapter(ArrayList<ChatMessage> m, boolean a) {
        currUid = FirebaseAuth.getInstance().getUid();
        messages = m;
        isAnon = a;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layout = new LinearLayout(parent.getContext());

        switch (viewType) {
            case SENDER_CONT:
                layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_sender_cont, parent, false);
                break;
            case RECEIVER_CONT:
                layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_receiver_cont, parent, false);
                break;
            case SENDER_TOP:
                layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_sender, parent, false);
                break;
            case SENDER_END:
                layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_sender_end, parent, false);
                break;
            case RECEIVER_END:
                layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_receiver_end, parent, false);
                break;
            case RECEIVER_TOP:
                layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_receiver, parent, false);
                break;
            case ANNOUNCEMENT:
                layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_announcement, parent, false);
                break;
            default:
                break;
        }
        return new ChatViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull final ChatViewHolder holder, int position) {
        ChatMessage currMessage = messages.get(position);
        int type = getItemViewType(position);
        holder.setStatus(null);

        if (getItemViewType(position) == ANNOUNCEMENT) {
            holder.setAnnouncement(currMessage.getMessage());
        } else {
            holder.setMessage(currMessage.getMessage());
            switch (type) {
                case SENDER_TOP:
                    holder.setUsername("Me");
                    if (position == getItemCount()-1) {
                        holder.setStatus(currMessage.getStatus() == ChatMessage.Status.READ ? "Read":"Sent");
                    }
                    break;
                case RECEIVER_TOP:
                    holder.setUsername(currMessage.getSenderName());
                    break;
                case RECEIVER_END:
                case SENDER_END:
                    holder.setStatus(currMessage.getStatus() == ChatMessage.Status.READ ? "Read":"Sent");
                    break;
            }

            // Check saved avatars
            if (!isAnon && (type == RECEIVER_TOP || type == SENDER_TOP)) {
                final String sender = currMessage.getSenderUid();
                if (avatars.containsKey(sender)) {
                    String url = avatars.get(sender);
                    if (url != null) {
                        holder.setUserAv(Uri.parse(url));
                    }
                } else {
                    FirebaseDatabase.getInstance().getReference("Users").child(sender).child("photoUrl").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                                avatars.put(sender, dataSnapshot.getValue().toString());
                                holder.setUserAv(Uri.parse(avatars.get(sender)));
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
    public int getItemViewType(int position) {
        ChatMessage currMess = messages.get(position);
        if (currMess.getType() == ChatMessage.Type.TEXT) {
            if (currMess.getSenderUid().equals(currUid)) {
                if (position == 0 || (position > 0 && !messages.get(position-1).getSenderUid().equals(currMess.getSenderUid()))) {
                    return SENDER_TOP;
                }
                else if (position == getItemCount()-1) {
                    return SENDER_END;
                }
                return SENDER_CONT;
            } else {
                if (position == getItemCount()) {
                    return RECEIVER_END;
                }
                else if (position == 0 || (position > 0 && !messages.get(position-1).getSenderUid().equals(currMess.getSenderUid()))) {
                    return RECEIVER_TOP;

                }
                return RECEIVER_CONT;
            }
        }
        else if (currMess.getType() == ChatMessage.Type.LEAVE) {
            return ANNOUNCEMENT;
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static public class ChatViewHolder extends RecyclerView.ViewHolder {
        private TextView username;
        private TextView message;
        private TextView status;
        private ImageView userAv;
        private TextView announcement;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            userAv = itemView.findViewById(R.id.userAv);
            message = itemView.findViewById(R.id.message_text);
            username = itemView.findViewById(R.id.sender_username);
            announcement = itemView.findViewById(R.id.announcement_text);
            status = itemView.findViewById(R.id.status);
        }

        public void setMessage(String message) {
            this.message.setText(message);
        }

        public void setUserAv(Uri userAv) {
            if (this.userAv != null) this.userAv.setImageURI(userAv);
        }

        public void setUsername(String username) {
            if (this.username != null) this.username.setText(username);
        }

        public void setAnnouncement(String announcement) {
            if (this.announcement != null) this.announcement.setText(announcement);
        }

        public void setStatus(String status) {
            if (this.status != null) {
                if (status == null) {
                    this.status.setVisibility(View.GONE);
                    return;
                }
                this.status.setText(status);
                this.status.setVisibility(View.VISIBLE);
            }
        }
    }
}
