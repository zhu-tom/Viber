package com.tomzhu.viber.ui.account;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.tomzhu.viber.R;

public class AccountMainFragment extends Fragment {
    private TextView usernameLabel;
    private TextView nameLabel;
    private TextView phoneNumberLabel;
    private ImageView avatar;
    private FirebaseAuth mAuth;
    private FirebaseUser currUser;
    private FirebaseDatabase db;
    private ImageButton settingsButton;
    private static final String TAG = "AccountMainFragment";

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        currUser = mAuth.getCurrentUser();
        db = FirebaseDatabase.getInstance();
        return inflater.inflate(R.layout.fragment_account_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        usernameLabel = getView().findViewById(R.id.account_username_label);
        nameLabel = getView().findViewById(R.id.account_name_label);
        phoneNumberLabel = getView().findViewById(R.id.account_phone_label);
        avatar = getView().findViewById(R.id.userAv);
        settingsButton = getView().findViewById(R.id.settingsButton);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController controller = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                controller.navigate(R.id.action_navigation_account_to_navigation_settings);
            }
        });

        db.getReference("Users").child(currUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String username = dataSnapshot.child("username").getValue().toString();
                String name = dataSnapshot.child("name").getValue().toString();
                Object url = dataSnapshot.child("photoUrl").getValue();
                if (url == null) {
                    Picasso.get().load(R.drawable.avatar_placeholder).into(avatar);
                } else {
                    Uri uri = Uri.parse(url.toString());
                    Picasso.get().load(uri).placeholder(R.drawable.avatar_placeholder).into(avatar);
                }
                phoneNumberLabel.setText(currUser.getPhoneNumber());
                nameLabel.setText(name);
                usernameLabel.setText(username);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
