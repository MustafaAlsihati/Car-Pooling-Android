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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Register extends AppCompatActivity {

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
                    Pattern.CASE_INSENSITIVE);

    EditText username, email, password, confPass, fname, lname, phoneNum;
    Button submitBtn;
    private FirebaseAuth firebaseAuth;
    Context context;

    @Override
    protected void onStart() {
        super.onStart();
        if(firebaseAuth.getCurrentUser() != null){
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        context = this;

        if(!NetworkCheck.IsNetworkConnected(context)){
            Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth = FirebaseAuth.getInstance();

        username = (EditText) findViewById(R.id.userNameRegField);
        fname = (EditText) findViewById(R.id.fNameRegField);
        lname = (EditText) findViewById(R.id.lNameRegField);
        email = (EditText) findViewById(R.id.emailRegField);
        password = (EditText) findViewById(R.id.passwordRegField);
        confPass = (EditText) findViewById(R.id.confPassRegField);
        phoneNum = (EditText) findViewById(R.id.phoneRegField);
        submitBtn = (Button) findViewById(R.id.registerConfirmBtn);

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String CurrentUserName = username.getText().toString().trim();
                final String CurrentFname = fname.getText().toString().trim();
                final String CurrentLname = lname.getText().toString().trim();
                final String CurrentEmail = email.getText().toString().trim();
                final String CurrentPhoneNum = phoneNum.getText().toString().trim();
                final String CurrentPassword = password.getText().toString();
                final String CurrentConfPass = confPass.getText().toString();

                if(CurrentUserName.isEmpty()){
                    Toast.makeText(Register.this, "Please enter a username", Toast.LENGTH_SHORT).show();
                    return;
                }if(CurrentFname.isEmpty()){
                    Toast.makeText(Register.this, "Please Enter Your First Name", Toast.LENGTH_SHORT).show();
                    return;
                }if(CurrentLname.isEmpty()){
                    Toast.makeText(Register.this, "Please Enter Your Last Name", Toast.LENGTH_SHORT).show();
                    return;
                }if(CurrentPhoneNum.isEmpty()){
                    Toast.makeText(Register.this, "Please Enter Your Last Name", Toast.LENGTH_SHORT).show();
                    return;
                }if(CurrentUserName.contains(" ")){
                    Toast.makeText(Register.this, "Spaces are not allowed", Toast.LENGTH_SHORT).show();
                    return;
                } if( CurrentPassword.length() < 6 ){
                    Toast.makeText(Register.this, "Password should be at least 6 digits/letters", Toast.LENGTH_SHORT).show();
                    return;
                } if( !validate(CurrentEmail) ){
                    Toast.makeText(Register.this, "Please Enter a valid Email Address", Toast.LENGTH_SHORT).show();
                    return;
                } if(!CurrentConfPass.equals(CurrentPassword)){
                    Toast.makeText(Register.this, "Confirmation Password is not matched", Toast.LENGTH_SHORT).show();
                    return;
                }

                final String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                final DatabaseReference Users = FirebaseDatabase.getInstance().getReference().child("Users");

                firebaseAuth.createUserWithEmailAndPassword(CurrentEmail, CurrentPassword)
                        .addOnCompleteListener(Register.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()){
                                    User user = new User(CurrentUserName, CurrentEmail, CurrentFname, CurrentLname, date, CurrentPhoneNum);
                                    Users.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(user)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()){
                                                        Toast.makeText(Register.this, "Register Completed", Toast.LENGTH_SHORT).show();
                                                        setResult(RESULT_OK, null);
                                                        finish();
                                                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                                    }
                                                }
                                            });
                                } else {
                                    Toast.makeText(Register.this, "Registration didn't complete, email address may be exist", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }

    public static boolean validate(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(emailStr);
        return matcher.find();
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
