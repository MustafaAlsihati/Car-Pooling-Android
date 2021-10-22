package iau.ccsit.carpooling.ui.Lead;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import iau.ccsit.carpooling.MainActivity;
import iau.ccsit.carpooling.R;

public class Captain_2 extends AppCompatActivity {

    private static final String TAG = "Captain_2";

    double latFrom, lngFrom, latTo, lngTo;
    int no_of_seats;
    String addressFrom,addressTo;
    Context context;
    TextView FromTextView, ToTextView;
    Spinner gender;
    FirebaseAuth auth;
    NumberPicker Car_seats;
    String UID;
    String selectedGender;
    String selectedDate;
    Button confirmAddTrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captain_2);
        context = this;
        auth = FirebaseAuth.getInstance();
        UID = auth.getCurrentUser().getUid();

        FromTextView = (TextView) findViewById(R.id.FromTextView);
        ToTextView = (TextView) findViewById(R.id.ToTextView);
        gender = (Spinner) findViewById(R.id.genderSpinner);
        confirmAddTrip = (Button) findViewById(R.id.confirmAddTrip);
        Car_seats = (NumberPicker) findViewById(R.id.carSize_captain);
        Car_seats.setMinValue(1);
        Car_seats.setMaxValue(6);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            latFrom = extras.getDouble("latFrom");
            lngFrom = extras.getDouble("lngFrom");
            latTo = extras.getDouble("latTo");
            lngTo = extras.getDouble("lngTo");
            addressFrom = extras.getString("addressFrom");
            addressTo = extras.getString("addressTo");
            selectedDate = extras.getString("selectedDate");
        } else {
            Toast.makeText(context, "Error Encountered", Toast.LENGTH_SHORT).show();
            return;
        }

        FromTextView.setText(addressFrom);
        ToTextView.setText(addressTo);

        selectedGender = gender.getSelectedItem().toString();

        confirmAddTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                no_of_seats = Car_seats.getValue();
                final DatabaseReference Trip = FirebaseDatabase.getInstance()
                        .getReference("Trips");
                Map<String, Object> addTrip = new HashMap<String,Object>();
                addTrip.put("latFrom", latFrom);
                addTrip.put("lngFrom", lngFrom);
                addTrip.put("addressFrom", addressFrom);
                addTrip.put("latTo", latTo);
                addTrip.put("lngTo", lngTo);
                addTrip.put("addressTo", addressTo);
                addTrip.put("no_of_seats", no_of_seats);
                addTrip.put("captainid", UID);
                addTrip.put("gender", selectedGender);
                addTrip.put("date", selectedDate);
                addTrip.put("availability", true);
                addTrip.put("status", "NotStarted");
                final String key = Trip.push().getKey();
                System.out.println("TID: " + key);
                Trip.child(key).setValue(addTrip)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            DatabaseReference addNearBy = FirebaseDatabase.getInstance().getReference("NearBy");
                            GeoFire geoFire = new GeoFire(addNearBy);
                            geoFire.setLocation(key, new GeoLocation(latFrom, lngFrom), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    Toast.makeText(context, "Trip Added", Toast.LENGTH_SHORT).show();
                                    setResult(1);
                                    finish();
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, "Error Encountered", Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        });

    }
}
