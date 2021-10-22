/*
 * Copyright (c) PROG's Team (Mustafa AlSihati Team).
 * This Project is currently an academic project for educational purposes.
 * This Project May be used for benefits for the working team.
 * Fully owned by the application developers.
 */

package iau.ccsit.carpooling;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Login extends AppCompatActivity {

    int REQUEST_EXIT;
    EditText email, password;
    Button loginBtn, goToRegisterBtn, resetPassword;
    private FirebaseAuth firebaseAuth;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        context = this;

        email = (EditText) findViewById(R.id.emailField);
        password = (EditText) findViewById(R.id.passwordField);
        loginBtn = (Button) findViewById(R.id.loginBtn);
        goToRegisterBtn = (Button) findViewById(R.id.registerBtn);
        resetPassword = (Button) findViewById(R.id.resetPassBtn);

        //for test
        firebaseAuth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkCheck.IsNetworkConnected(context)) {
                    String CurrentEmail = email.getText().toString().trim();
                    String CurrentPassword = password.getText().toString().trim();

                    if (TextUtils.isEmpty(CurrentEmail)) {
                        Toast.makeText(Login.this, "Please enter a email address", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(CurrentPassword)) {
                        Toast.makeText(Login.this, "Please enter a password address", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (CurrentPassword.length() < 6) {
                        Toast.makeText(Login.this, "Password should be at least 6 digits/letters.", Toast.LENGTH_LONG).show();
                    }

                    firebaseAuth.signInWithEmailAndPassword(CurrentEmail, CurrentPassword)
                            .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                        if (firebaseAuth.getCurrentUser() != null) {
                                            System.out.println(firebaseAuth.getCurrentUser().getEmail());
                                        }
                                    } else {
                                        Toast.makeText(Login.this, "Login failed, user may not be available", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                } else {
                    Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        goToRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkCheck.IsNetworkConnected(context)) {
                    startActivityForResult(new Intent(Login.this, Register.class), REQUEST_EXIT);
                } else {
                    Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Login.this, ForgotPassword.class), REQUEST_EXIT);
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = firebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EXIT) {
            if (resultCode == RESULT_OK) {
                this.finish();
            }
        }
    }
}