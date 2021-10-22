package iau.ccsit.carpooling;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
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

public class EditProfile extends AppCompatActivity {

    Context context;
    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
                    Pattern.CASE_INSENSITIVE);

    private FirebaseAuth firebaseAuth;
    FirebaseUser user;
    TextView fname, lname, emailAddress, car, password, phone;
    Button save;
    NumberPicker seats;
    CheckBox smoker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        context = this;

        if (!NetworkCheck.IsNetworkConnected(context)) {
            Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        fname = (TextView) findViewById(R.id.EditFirstName);
        lname = (TextView) findViewById(R.id.EditLastName);
        emailAddress = (TextView) findViewById(R.id.EditEmailField);
        phone = (TextView) findViewById(R.id.EditPhoneField);
        car = (TextView) findViewById(R.id.carModelfield);
        seats = (NumberPicker) findViewById(R.id.carSize);
        save = (Button) findViewById(R.id.editProfile_save);
        password = (TextView) findViewById(R.id.PasswordField_forEdit);
        smoker = (CheckBox) findViewById(R.id.smokerCheckBox);

        seats.setMinValue(1);
        seats.setMaxValue(6);

        if(firebaseAuth!=null) {
            try {
                emailAddress.setText(firebaseAuth.getCurrentUser().getEmail());
                updateFields();
            } catch (Exception e) {
                //
            }
        }

        try {
            save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String email = emailAddress.getText().toString().trim();
                    final String phoneNum = phone.getText().toString().trim();
                    final String fname1 = fname.getText().toString();
                    final String lname1 = lname.getText().toString();
                    final String pass = password.getText().toString().trim();
                    final String carModel = car.getText().toString();
                    final int numOfSeats = seats.getValue();
                    final String Smoker;
                    if(smoker.isChecked()){
                        Smoker = "true";
                    } else {
                        Smoker = "false";
                    }

                    if (email.isEmpty()) {
                        Toast.makeText(context, "Please enter your new email address",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!validate(email)) {
                        Toast.makeText(context, "Invalid New Email Address",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (phoneNum.isEmpty()) {
                        Toast.makeText(context, "Please enter your phone number",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AuthCredential credential = EmailAuthProvider
                            .getCredential(firebaseAuth.getCurrentUser().getEmail(), pass);
                    user.reauthenticate(credential).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            user.updateEmail(email)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            DatabaseReference UserRef = FirebaseDatabase.getInstance()
                                                    .getReference("Users").child(user.getUid());
                                            Map<String, Object> updates = new HashMap<String, Object>();
                                            updates.put("fname", fname1);
                                            updates.put("lname", lname1);
                                            updates.put("car", carModel);
                                            updates.put("no_of_seats", numOfSeats);
                                            updates.put("smoker", Smoker);
                                            UserRef.updateChildren(updates)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Toast.makeText(context, "Profile Updated",
                                                                    Toast.LENGTH_SHORT).show();
                                                            setResult(RESULT_OK, null);
                                                            finish();
                                                        }
                                                    });
                                        }
                                    });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, "Unable to update, please try again",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            //
        }

    }

    public void updateFields(){
        Query UserRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(user.getUid());
        UserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    if(dataSnapshot.exists()) {
                        Map<String, Object> newPost = (Map<String, Object>) dataSnapshot.getValue();
                        fname.setText(newPost.get("fname").toString());
                        phone.setText(newPost.get("phone").toString());
                        lname.setText(newPost.get("lname").toString());
                        car.setText(newPost.get("car").toString());
                        seats.setValue(Integer.parseInt(newPost.get("no_of_seats").toString()));
                        if(newPost.get("smoker").toString().equals("true")) {
                            smoker.setChecked(true);
                        }
                    }
                } catch (Exception e){
                    //exception
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public static boolean validate(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(emailStr);
        return matcher.find();
    }

}
