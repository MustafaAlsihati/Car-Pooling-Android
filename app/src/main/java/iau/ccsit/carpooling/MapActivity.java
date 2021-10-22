package iau.ccsit.carpooling;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import iau.ccsit.carpooling.model.PlaceInfo;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40,-168),new LatLng(71,136));

    private Boolean mLocationPermissionGranted = false;
    private GoogleMap map;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private AutocompleteSessionToken autocompleteSessionToken;
    private PlacesClient placesClient;
    private PlaceAutocompleteAdapterNew autoCompleteAdpater;
    private Marker marker,markerClick;
    // for test
    private LatLng currentLatlng;


    private AutoCompleteTextView mSearchText;
    private ImageButton myLocation,mFilter;
    private Button mButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_3);
        mSearchText = (AutoCompleteTextView) findViewById(R.id.input_search);
        myLocation = (ImageButton) findViewById(R.id.myLocation);
        mFilter = (ImageButton) findViewById(R.id.filterMap);
        mButton = (Button) findViewById(R.id.request);

        getLocationPermission();

    }



    private void init(){
        Log.d(TAG, "init: initializing");

        autoCompleteAdpater = new PlaceAutocompleteAdapterNew(this,placesClient,autocompleteSessionToken);
        mSearchText.setAdapter(autoCompleteAdpater);
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        event.getAction() == KeyEvent.ACTION_DOWN ||
                        event.getAction() == KeyEvent.KEYCODE_ENTER){
                    geoLocate();
                }
                else{
                    Log.d(TAG, "onEditorAction: code action " + event.getAction()+". ");
                }
                return false;

            }

        });

        myLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });

        mFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked place info icon");
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapActivity.this);
                final View mView = getLayoutInflater().inflate(R.layout.filter,null);
                mBuilder.setView(mView);

                final RadioGroup radioGroup = (RadioGroup) mView.findViewById(R.id.radiogroupid);

                mBuilder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                mBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                final AlertDialog dialog = mBuilder.create();
                dialog.show();



                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int radioBtnId;
                        radioBtnId = radioGroup.getCheckedRadioButtonId();

                        if(radioBtnId == R.id.btnNormal){
                            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                            Toast.makeText(v.getContext(),"Change the map type",Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                        else if (radioBtnId == R.id.btnSatellite){
                            map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                            Toast.makeText(v.getContext(),"Change the map type",Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }else{
                            Toast.makeText(v.getContext(),"You should select any item to change",Toast.LENGTH_SHORT).show();
                        }

                    }
                });

                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

            }
        });

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(markerClick != null) {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Log.d(TAG, "onClick: check " + userId);

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("locations");
                    GeoFire geoFire = new GeoFire(ref);
                    String pickpoint = ref.push().getKey();

                    geoFire.setLocation(pickpoint, new GeoLocation(markerClick.getPosition().latitude, markerClick.getPosition().longitude),
                            new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    markerClick.setTitle("Your pick point");
                                    mButton.setText("Done");
                                    mButton.setEnabled(false);
                                    Toast.makeText(getApplicationContext(),"Location Saved",Toast.LENGTH_SHORT).show();
                                    moveToNewFragment();
                                }
                            });

//                    pickuplocation = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
                    //map.clear();
                    //map.addMarker(new MarkerOptions().position(pickuplocation).title("Pickup here"));
                }
                else{
//                    myLocationPickPoint();
                }
            }
        });


        hideSoftKeyboard();

    }

    private void moveToNewFragment(){

        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?saddr=47.295601,0.010586&daddr=47.295601,1.010586+to:47.295601,2.010586+to:47.295601,3.010586"));
        startActivity(intent);

//        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
//                Uri.parse("http://maps.google.com/maps?saddr="+currentLatlng.latitude+","+currentLatlng.longitude+"&daddr=26.558212, 50.034554&mode=driving"));
//        startActivity(intent);

    }

//    private void myLocationPickPoint(){
//
//        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapActivity.this);
//        View mView = getLayoutInflater().inflate(R.layout.mylocation,null);
//
//        mBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//
//            }
//        });
//
//        mBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//            }
//        });
//
//
//        mBuilder.setView(mView);
//        final AlertDialog dialog = mBuilder.create();
//        dialog.show();
//
//        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                markerClick = map.addMarker(new MarkerOptions().position(marker.getPosition()));
//                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child("normal").child(userId);
//                GeoFire geoFire = new GeoFire(ref);
//                geoFire.setLocation("pickpoint", new GeoLocation(marker.getPosition().latitude, marker.getPosition().longitude));
//
//                pickuplocation = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
//                markerClick.setPosition(marker.getPosition());
//                //map.clear();
//                //map.addMarker(new MarkerOptions().position(pickuplocation).title("Pickup here"));
//                markerClick.setTitle("Your pick point");
//                mButton.setText("Done");
//                mButton.setEnabled(false);
//                Toast.makeText(v.getContext(),"Set your pick point done",Toast.LENGTH_SHORT).show();
//                dialog.dismiss();
//            }
//        });
//
//        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(v.getContext(),"Please choose location of pick point",Toast.LENGTH_SHORT).show();
//                dialog.dismiss();
//            }
//        });
//    }


    private void geoLocate(){
        Log.d(TAG, "geoLocate: geolocating");
        String searchString = mSearchText.getText().toString();
        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
        try{
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage() );
        }

        if(list.size()>0){
            Address address = list.get(0);

            Log.d(TAG, "geoLocate:  found a location " + address.toString());

            moveCameraSearch(new LatLng(address.getLatitude(), address.getLongitude()),DEFAULT_ZOOM);
        }

    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the device current location");

        mFusedLocationProviderClient  = LocationServices.getFusedLocationProviderClient(this);

        try{

            if(mLocationPermissionGranted){

                final Task location = mFusedLocationProviderClient.getLastLocation();

                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "getDeviceLocation: check is not null "+ ((task != null) ? "yes":"no"));
                            Log.d(TAG, "onComplete: found location!");
                            try{
                                Location currentLocation = (Location) task.getResult();
                                Log.d(TAG, "getDeviceLocation: check is not null "+ ((currentLocation != null) ? "yes":"no"));
                                LatLng latLng = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
                                currentLatlng = latLng;
//                            map.clear();
//                            map.addMarker(new MarkerOptions().position(latLng).title("My location"));

                                moveCamera(latLng,DEFAULT_ZOOM, "My location");
                            }
                            catch (Exception e){
                                Toast.makeText(MapActivity.this,"error null pointer", Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapActivity.this,"unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }

    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        hideSoftKeyboard();
    }

    private void moveCameraSearch(LatLng latLng, float zoom){
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId){
        Drawable vectorDrawable = ContextCompat.getDrawable(context,vectorResId);
        vectorDrawable.setBounds(0,0,vectorDrawable.getIntrinsicWidth(),vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),vectorDrawable.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

//    private void moveCamera(LatLng latLng, float zoom, PlaceInfo placeInfo){
//        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
//
//        map.clear();
//
//        map.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapActivity.this));
//
//        if(placeInfo != null){
//            try{
//                String snippet = "Address: " + placeInfo.getAddress() + "\n" +
//                        "Phone Number: " + placeInfo.getPhoneNumber() + "\n" +
//                        "Website: " + placeInfo.getWebsiteUri() + "\n" +
//                        "Price Rating: " + placeInfo.getRating() + "\n";
//
//                MarkerOptions options = new MarkerOptions()
//                        .position(latLng)
//                        .title(placeInfo.getName())
//                        .snippet(snippet);
//                marker = map.addMarker(options);
//
//            }catch (NullPointerException e){
//                Log.e(TAG, "moveCamera: NullPointerException: " + e.getMessage() );
//            }
//        }else{
//            map.addMarker(new MarkerOptions().position(latLng));
//        }
//
//        hideSoftKeyboard();
//    }

    private void initMap(){
        Log.d(TAG,"initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapActivity.this);
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String [] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionGranted = true;

                initMap();

            }else{
                ActivityCompat.requestPermissions(this,permissions,LOCATION_PERMISSION_REQUEST_CODE);
            }

        }else{
            ActivityCompat.requestPermissions(this,permissions,LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "OnRequestPermissionsResult: called.");
        mLocationPermissionGranted = false;
        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for (int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionGranted = false;
                            Log.d(TAG, "OnRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "OnRequestPermissionsResult: permission granted");
                    mLocationPermissionGranted = true;
                    initMap();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG,"OnMapReady: map is ready");

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_API_key));
        }

        map = googleMap;

        if(mLocationPermissionGranted){
            getDeviceLocation();

            if(ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED){
                return;
            }
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            map.setOnMapClickListener(this);
            //enable the UI settings
            map.setMyLocationEnabled(true);
//            map.getUiSettings().setZoomControlsEnabled(true);
            autocompleteSessionToken = AutocompleteSessionToken.newInstance();
            placesClient = Places.createClient(getApplicationContext());
            init();
        }
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        Log.d(TAG, "hideSoftKeyboard: calling");
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick: test");

        String pick_message = "Your pick point";

        if(markerClick != null){

            if(!markerClick.getTitle().equals(pick_message)) {
                markerClick.remove();
                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title("Your spot");
                markerClick = map.addMarker(options);
            }

        }else{
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title("Your spot");
            markerClick = map.addMarker(options);
        }

    }

    //----------------- google place API autocomplete suggestions ------------------

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: here map!");
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class));
    }
}
