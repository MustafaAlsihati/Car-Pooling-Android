package iau.ccsit.carpooling.ui.FindRide;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import iau.ccsit.carpooling.MainActivity;
import iau.ccsit.carpooling.Passenger_2;
import iau.ccsit.carpooling.R;

import static android.content.Context.LOCATION_SERVICE;

public class BottomList extends BottomSheetDialogFragment {

    private static final String TAG = "BottomList";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private Boolean mLocationPermissionGranted = false;
    private  LocationManager locationManager;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LatLng currentLatLng;

    View view;
    ListView nearByList;
    int range;

    //for search in all trips
    final ArrayList<String> TripIDs = new ArrayList<>();
    //for near  trips
    final ArrayList<String> TripNearIDs = new ArrayList<>();
    int REQUEST_CODE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.botton_list, container, false);
        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);

        nearByList = (ListView) view.findViewById(R.id.nearByList);

        range = getArguments().getInt("range");
        System.out.println(range);
        getLocationPermission();
        getNearbyTrip(range);

        return view;
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting  sdxz location permissions");
        String [] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getContext(),FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getContext(),COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionGranted = true;
                //check the gps is working or not
                if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                    buildAlertMessageNoGps();
                } else {
                    getDeviceLocation();
                }
            } else {
                ActivityCompat.requestPermissions(this.getActivity(),permissions,LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else{
            ActivityCompat.requestPermissions(this.getActivity(),permissions,LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    private void buildAlertMessageNoGps() {
        Log.d(TAG, "buildAlertMessageNoGps: check is work");
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        startActivity(new Intent(getActivity(), MainActivity.class));
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void getNearbyTrip(final int range){
        Log.d(TAG, "getNearbyTrip: is called!");
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("NearBy");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Log.d(TAG, "onDataChange1: is work!");
                    collectAllTrip((Map<String,Object>) dataSnapshot.getValue(),range);
                } else {
                    Log.d(TAG, "onDataChange1: there is no trip! ");

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void collectAllTrip(Map<String,Object> trips, final int range) {

        //Add the nearby trip key
        for (Map.Entry <String, Object> entry : trips.entrySet()){
            TripIDs.add( entry.getKey() );
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "collectAllTrip: the current user ID is "+currentUserId);
        DatabaseReference getTripRef = FirebaseDatabase.getInstance().getReference("Trips");
        getTripRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try{

                    //check if it is null to refresh
                    if(TripNearIDs != null){
                        TripNearIDs.clear();
                    }

                    for (int i = 0 ; i < TripIDs.size() ; i++) {
                        //get data from Firebase
                        double latFrom = Double.parseDouble(dataSnapshot.child(TripIDs.get(i)).child("latFrom").getValue().toString());
                        double lngFrom = Double.parseDouble(dataSnapshot.child(TripIDs.get(i)).child("lngFrom").getValue().toString());
                        String captainId = dataSnapshot.child(TripIDs.get(i)).child("captainid").getValue().toString();
                        Boolean availability = Boolean.parseBoolean(dataSnapshot.child(TripIDs.get(i)).child("availability").getValue().toString());
                        String dateTrip = dataSnapshot.child(TripIDs.get(i)).child("date").getValue().toString();
                        String statusTrip = dataSnapshot.child(TripIDs.get(i)).child("status").getValue().toString();

                        //check the distance if it is less than or equal range
                        Log.d(TAG, "onDataChange: " +TripIDs.get(i)+" distance is " + distance(currentLatLng.latitude, currentLatLng.longitude, latFrom, lngFrom) + " " + currentLatLng.toString()+ " "+captainId);
                        if (distance(currentLatLng.latitude, currentLatLng.longitude, latFrom, lngFrom) <= range && (!currentUserId.equals(captainId))) {
                            //Add to the near trips
                            if(availability && validateDate(dateTrip) && statusTrip.equals("NotStarted")) {
                                TripNearIDs.add(TripIDs.get(i));
                                Log.d(TAG, "onDataChange: the id is " + TripIDs.get(i));
                            }
                        }
                    }

                            //Add values to the list:
                            final ArrayAdapter<String> arrayAdapter1 = new ArrayAdapter<String>
                                    (view.getContext(), android.R.layout.simple_list_item_1, TripNearIDs);
                            nearByList.setAdapter(arrayAdapter1);
                            nearByList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                                    Intent intent = new Intent(getContext(), Passenger_2.class);
//                                    intent.putExtra("ProjectID", TripIDs.get(position));
//                                    startActivityForResult(intent, REQUEST_CODE);
                                }
                            });


                    //check if there is no near trip
                    if (TripNearIDs.size() == 0){
                        Toast.makeText(getContext(),"Sorry, there is no trip within your selected range",Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e){
                    Log.e(TAG, "onDataChange: error! " + e.getMessage());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private double distance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371; // in miles, change to 6371 for kilometer output
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;
        return dist; // output distance, in kilometer
    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the device current location");
        mFusedLocationProviderClient  = LocationServices.getFusedLocationProviderClient(getActivity());
        try{
            if(mLocationPermissionGranted){
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            try {
                                Location currentLocation = (Location) task.getResult();
                                LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                                currentLatLng = latLng;
                                Log.d(TAG, "onComplete: found location! " + currentLatLng.latitude + " "+currentLatLng.longitude);

                            } catch (Exception e){
                                Toast.makeText(getContext(),"Your location is not available",Toast.LENGTH_SHORT).show();
                            }
                        } else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(getContext(),"unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    private boolean validateDate(String selectedDate){
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        boolean b = true;
        try {
            String [] currentDateArr = currentDate.split("-");
            String [] selectedDateArr = selectedDate.split("-");
            int currentYear,currentMth,currentDay,selectedDay,selectedYear,selectedMth;

            currentYear = Integer.parseInt(currentDateArr[2]);
            currentMth = Integer.parseInt(currentDateArr[1]);
            currentDay = Integer.parseInt(currentDateArr[0]);

            selectedYear = Integer.parseInt(selectedDateArr[2]);
            selectedMth = Integer.parseInt(selectedDateArr[1]);
            selectedDay = Integer.parseInt(selectedDateArr[0]);

            if(currentYear <= selectedYear){
                if(currentYear == selectedYear){
                    if(currentMth<= selectedMth){
                        if(currentMth== selectedMth){
                            if(currentDay<= selectedDay){
                                b = true;
                            }
                            else
                                b = false;
                        }
                        else
                            b = true;
                    }
                    else
                        b = false;
                }
                else
                    b = true;
            }
            else{
                b = false;
            }
        }catch (Exception e){
            Toast.makeText(getActivity().getApplicationContext(),"Error" + e.getMessage(),Toast.LENGTH_SHORT).show();
        }
        return b;
    }

}
