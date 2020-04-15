package com.tomzhu.viber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.tomzhu.viber.models.User;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText usernameInput;
    private ImageButton avatarChange;
    private Button signOutBtn;
    private FirebaseAuth mAuth;
    private FirebaseDatabase db;
    private FirebaseStorage mStorage;
    private Button saveButton;
    private Uri newAvatar;
    private String prevName;
    private String prevUser;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        nameInput = findViewById(R.id.name_change);
        usernameInput = findViewById(R.id.username_change);
        avatarChange = findViewById(R.id.avatarChange);
        signOutBtn = findViewById(R.id.signOut);
        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();
        db = FirebaseDatabase.getInstance();
        saveButton = findViewById(R.id.saveChangesButton);
        cancelButton = findViewById(R.id.cancelButton);

        FirebaseUser currUser = mAuth.getCurrentUser();

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, Object> update = new HashMap<>();

                if (newAvatar != null) {
                    update.put("photoUrl", newAvatar.toString());
                }
                String newName = nameInput.getText().toString();
                if (!newName.equals(prevName)) {
                    update.put("name", nameInput.getText().toString());
                }
                String newUser = usernameInput.getText().toString();
                if (!newUser.equals(prevUser)) {
                    update.put("username", newUser);
                }

                db.getReference("Users").child(mAuth.getCurrentUser().getUid()).updateChildren(update).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(SettingsActivity.this, task.isSuccessful() ? "Changed Saved!":"Save Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        db.getReference("Users").child(currUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                prevName = dataSnapshot.child("name").getValue().toString();
                nameInput.setText(prevName);
                prevUser = dataSnapshot.child("username").getValue().toString();
                usernameInput.setText(prevUser);
                Object url = dataSnapshot.child("photoUrl").getValue();
                if (url == null) {
                    Picasso.get().load(R.drawable.avatar_placeholder).into(avatarChange);
                } else {
                    Uri uri = Uri.parse(url.toString());
                    Picasso.get().load(uri).placeholder(R.drawable.avatar_placeholder).into(avatarChange);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        avatarChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 0);
            }
        });

        signOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(SettingsActivity.this, RegistrationActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED && requestCode == 0) {
            final Uri image = data.getData();
            final StorageReference storageRef = mStorage.getReference("ProfileImages/" + image.getPath());
            storageRef.putFile(image).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(SettingsActivity.this, "Upload successful!", Toast.LENGTH_SHORT).show();

                    storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            newAvatar = uri;
                            avatarChange.setImageURI(image);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(SettingsActivity.this, "Upload failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(SettingsActivity.this, "Upload failed.", Toast.LENGTH_SHORT).show();
                }
            });

        }

    }
}
