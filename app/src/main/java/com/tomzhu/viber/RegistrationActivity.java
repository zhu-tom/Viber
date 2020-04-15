package com.tomzhu.viber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hbb20.CountryCodePicker;
import com.tomzhu.viber.models.User;

import org.w3c.dom.Text;

import java.util.concurrent.TimeUnit;

public class RegistrationActivity extends AppCompatActivity {

    private CountryCodePicker cpp;
    private EditText phoneNumberInput;
    private Button continueButton;
    private ConstraintLayout phoneLayout;
    private Button backButton;

    private EditText nameInput;
    private EditText usernameInput;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private FirebaseAuth auth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private AlertDialog mDialog;

    private EditText codeInput;
    private ConstraintLayout codeLayout;

    private boolean codeSent = false;

    public static final String TAG = "RegistrationActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            sendUserToMainActivity();
        }
        setContentView(R.layout.activity_registration);
        cpp = findViewById(R.id.ccp);

        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        continueButton = findViewById(R.id.continue_button);
        backButton = findViewById(R.id.back_button);
        phoneLayout = findViewById(R.id.phoneLayout);
        codeInput = findViewById(R.id.codeInput);
        codeLayout = findViewById(R.id.codeLayout);

        nameInput = findViewById(R.id.name_input);
        usernameInput = findViewById(R.id.username_input);

        cpp.registerCarrierNumberEditText(phoneNumberInput);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (codeSent) {
                    String verificationCode = codeInput.getText().toString();

                    if (verificationCode.equals("")) {
                        Toast.makeText(RegistrationActivity.this,"Please enter a verification code.", Toast.LENGTH_SHORT).show();
                    } else {
                        showLoading("Please wait while we verify your code...");

                        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, verificationCode);
                        signInWithPhoneAuthCredential(credential);
                    }
                } else {
                    if (!(phoneNumberInput.getText().toString().equals("") || usernameInput.getText().toString().equals("") || nameInput.getText().toString().equals(""))) {
                        String phoneNumber = cpp.getFullNumberWithPlus();
                        Log.i(TAG, phoneNumber);

                        showLoading("Please wait while we verify your phone number...");

                        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                                phoneNumber,        // Phone number to verify
                                60,                 // Timeout duration
                                TimeUnit.SECONDS,   // Unit of timeout
                                RegistrationActivity.this,               // Activity (for callback binding)
                                callbacks);        // OnVerificationStateChangedCallbacks

                    } else {
                        Toast.makeText(RegistrationActivity.this, "Please ensure fields are non-empty.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (codeSent) {

                    phoneLayout.setVisibility(View.VISIBLE);
                    continueButton.setText(R.string.continue_button_label);
                    codeLayout.setVisibility(View.GONE);
                    backButton.setEnabled(false);
                    codeSent = false;

                }
            }
        });

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                mDialog.dismiss();
                Log.i(TAG, e.getMessage());
                Toast.makeText(RegistrationActivity.this, "Invalid phone number.", Toast.LENGTH_SHORT).show();
                phoneLayout.setVisibility(View.VISIBLE);

                continueButton.setText(R.string.continue_button_label);
                codeLayout.setVisibility(View.GONE);
                backButton.setEnabled(false);
                codeSent = false;

            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);

                mDialog.dismiss();

                verificationId = s;
                resendingToken = forceResendingToken;

                phoneLayout.setVisibility(View.GONE);
                codeLayout.setVisibility(View.VISIBLE);

                backButton.setEnabled(true);

                codeSent = true;
                continueButton.setText(R.string.submit_button_label);
            }
        };
    }

    private void showLoading(String titleText) {
        LinearLayout linearLayout = new LinearLayout(RegistrationActivity.this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayout.setLayoutParams(layoutParams);

        ProgressBar loadingBar = new ProgressBar(RegistrationActivity.this, null, R.attr.progressBarStyle);
        loadingBar.setLayoutParams(layoutParams);

        ViewGroup.LayoutParams vgLayoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        TextView title = new TextView(RegistrationActivity.this);
        title.setText(titleText);
        title.setTextSize(12);
        title.setLayoutParams(vgLayoutParams);

        linearLayout.addView(title);
        linearLayout.addView(loadingBar);

        AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity.this);
        builder.setCancelable(false);
        builder.setView(linearLayout);

        mDialog = builder.create();
        mDialog.show();
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information

                        mDialog.dismiss();
                        Toast.makeText(RegistrationActivity.this, "Nice! You are logged in.", Toast.LENGTH_SHORT).show();


                        String currUID = auth.getCurrentUser().getUid();
                        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("Users").child(currUID);
                        User user = new User(nameInput.getText().toString(), currUID, usernameInput.getText().toString());
                        db.child("username").setValue(user.getUsername());
                        db.child("name").setValue(user.getName());
                        db.child("uid").setValue(currUID);
                        db.child("photoUrl").setValue(user.getPhotoUrl());


                        sendUserToMainActivity();

                        // ...
                    } else {
                        // Sign in failed, display a message and update the UI
                        String e = task.getException().toString();
                        mDialog.dismiss();
                        Toast.makeText(RegistrationActivity.this, "Error: " + e, Toast.LENGTH_SHORT).show();
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                            Toast.makeText(RegistrationActivity.this, "Invalid verification code.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
    }

    private void sendUserToMainActivity() {
        Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
