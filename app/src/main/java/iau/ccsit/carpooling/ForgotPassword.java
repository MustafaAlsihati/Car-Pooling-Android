/*
 * Copyright (c) PROG's Team (Mustafa AlSihati Team).
 * This Project is currently an academic project for educational purposes.
 * This Project May be used for benefits for the working team.
 * Fully owned by the application developers.
 */

package iau.ccsit.carpooling;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {

    Context context;
    EditText emailAddress;
    Button send;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password);
        setTitle("Reset Password");
        context = this;
        firebaseAuth = FirebaseAuth.getInstance();

        emailAddress = (EditText) findViewById(R.id.email_reset_password);
        send = (Button) findViewById(R.id.send_passReset_Btn);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(NetworkCheck.IsNetworkConnected(context)) {
                    String email = emailAddress.getText().toString().trim();
                    // In case of Empty Value:
                    if(email.isEmpty()){
                        Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Send Email:
                    firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(context, "Email sent", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(context, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
