package iau.ccsit.carpooling.ui.Locations;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.OnLocationClickListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import iau.ccsit.carpooling.MainActivity;
import iau.ccsit.carpooling.PlaceAutocompleteAdapterNew;
import iau.ccsit.carpooling.R;


import static java.security.AccessController.getContext;

public class LocationFragment extends AppCompatActivity implements OnMapReadyCallback, OnLocationClickListener,
        PermissionsListener, OnCameraTrackingChangedListener, MapboxMap.OnMapClickListener{

    private static final String TAG = "LocationFragment";

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private AutocompleteSessionToken autocompleteSessionToken;
    private PlacesClient placesClient;
    private PlaceAutocompleteAdapterNew autoCompleteAdpater;
    private static final float DEFAULT_ZOOM = 15f;

    private PermissionsManager permissionsManager;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private LocationComponent locationComponent;
    private LatLng pickuplocation;

    private boolean isInTrackingMode;

    private @NonNull Marker marker,markerClick;
    private ImageButton myLocation, mFilter;
    private Button mButton;
    private AutoCompleteTextView mSearchText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_key));
        Mapbox.setAccessToken(getString(R.string.mapbox_key));
        setContentView(R.layout.activity_map);
        mapView = (MapView) findViewById(R.id.mapView);


        mapView.onCreate(savedInstanceState);

        if (!Places.isInitialized()) {
            Places.initialize(this, "AIzaSyDy-qEwVgzSqX0EM0HXTiK-IZoqLh4r40w", Locale.US);
        }

        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                mapboxMap.getUiSettings().setZoomGesturesEnabled(true);
                mapboxMap.getUiSettings().setQuickZoomGesturesEnabled(true);
                mapboxMap.getUiSettings().setCompassEnabled(false);

                //instead of getLocationPermission()
                enableLocationComponent(style);

            }
        });

        mapboxMap.addOnMapClickListener(this);

        autocompleteSessionToken = AutocompleteSessionToken.newInstance();
        placesClient = Places.createClient(this);
        init();
    }

    private void init(){

        Log.d(TAG, "init: initializing");
        mSearchText = (AutoCompleteTextView) findViewById(R.id.input_search);


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

    }

    private void geoLocate(){
        Log.d(TAG, "geoLocate: geolocating");
        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(this);
        List<Address> list = new ArrayList<>();
        try{
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage() );
        }

        if(list.size()>0){
            Address address = list.get(0);

            Log.d(TAG, "geoLocate:  found a location " + address.toString());

            moveCamera(new com.mapbox.mapboxsdk.geometry.LatLng(address.getLatitude(), address.getLongitude()),DEFAULT_ZOOM,address.getAddressLine(0));
        }

    }

    private void moveCamera(com.mapbox.mapboxsdk.geometry.LatLng latLng, float zoom, String title){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.getLatitude() + ", lng: " + latLng.getLongitude() );
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));


        if(!title.equals("My Location")){

            if(marker != null)
                marker.remove();

            Log.d(TAG, "addMarker: is okay!!!");

            IconFactory iconFactory = IconFactory.getInstance(this);
            Icon iconBit = iconFactory.fromBitmap(bitmapFromVector(this,R.drawable.ic_fiber_manual_record_black_24dp));


            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .icon(iconBit)
                    .title(title);
            marker = mapboxMap.addMarker(options);
        }

        hideSoftKeyboard();
    }

    private Bitmap bitmapFromVector(Context context, int vectorResId){
        Drawable vectorDrawable = ContextCompat.getDrawable(context,vectorResId);
        vectorDrawable.setBounds(0,0,vectorDrawable.getIntrinsicWidth(),vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),vectorDrawable.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            initMap(loadedMapStyle);

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    private void initMap(@NonNull Style loadedMapStyle){

        setLocationComponent(loadedMapStyle);

        myLocation = (ImageButton) findViewById(R.id.myLocation);
        mFilter = (ImageButton) findViewById(R.id.filterMap);
        mButton = (Button) findViewById(R.id.request);



        myLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isInTrackingMode) {
                    isInTrackingMode = true;
                    locationComponent.setCameraMode(CameraMode.TRACKING);
                    locationComponent.zoomWhileTracking(16f);
//                        Toast.makeText(MainActivity.this, "the tracking enabled",
//                                Toast.LENGTH_SHORT).show();
                } else {
//                        Toast.makeText(MainActivity.this, "the tracking already enabled",
//                                Toast.LENGTH_SHORT).show();
                }

                if(markerClick != null){
                    markerClick.remove();
                }
                if(marker != null){
                    marker.remove();
                }
            }
        });


        mFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setFilterOption();

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


                    geoFire.setLocation(pickpoint, new GeoLocation(markerClick.getPosition().getLatitude(), markerClick.getPosition().getLongitude()),
                            new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    markerClick.setTitle("Your pick point");
                                    mButton.setText("Done");
                                    mButton.setEnabled(false);
                                    Toast.makeText(LocationFragment.this,"Location Saved",Toast.LENGTH_SHORT).show();
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

    }

    private void moveToNewFragment(){

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

//        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
//                Uri.parse("http://maps.google.com/maps?saddr="+currentLatlng.latitude+","+currentLatlng.longitude+"&daddr=26.558212, 50.034554&mode=driving"));
//        startActivity(intent);

    }

    private void myLocationPickPoint(){

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        View mView = getLayoutInflater().inflate(R.layout.mylocation,null);

        mBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        mBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });


        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markerClick = mapboxMap.addMarker(new MarkerOptions().position(marker.getPosition()));
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child("normal").child(userId);
                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation("pickpoint", new GeoLocation(marker.getPosition().getLatitude(), marker.getPosition().getLongitude()));

                pickuplocation = new LatLng(marker.getPosition().getLatitude(), marker.getPosition().getLongitude());
                markerClick.setPosition(marker.getPosition());
                //map.clear();
                //map.addMarker(new MarkerOptions().position(pickuplocation).title("Pickup here"));
                markerClick.setTitle("Your pick point");
                mButton.setText("Done");
                mButton.setEnabled(false);
                Toast.makeText(v.getContext(),"Set your pick point done",Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(),"Please choose location of pick point",Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }

    private void setFilterOption(){
        Log.d(TAG, "onClick: clicked filter button");
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
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
                    mapboxMap.setStyle(Style.MAPBOX_STREETS);
                    Toast.makeText(v.getContext(),"Change the map type",Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
                else if (radioBtnId == R.id.btnSatellite){
                    mapboxMap.setStyle(Style.SATELLITE_STREETS);
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

    private void setLocationComponent(@NonNull Style loadedMapStyle){

        // Create and customize the LocationComponent's options
        LocationComponentOptions customLocationComponentOptions = LocationComponentOptions.builder(this)
                .elevation(10)
                .accuracyAlpha(.6f)
                .backgroundDrawable(R.drawable.ic_circle_dot)
                .accuracyColor(Color.BLUE)
                .backgroundTintColor(Color.WHITE)
                .foregroundTintColor(Color.BLUE)
                .bearingTintColor(Color.WHITE)
                .build();

        // Get an instance of the component
        locationComponent = mapboxMap.getLocationComponent();

        LocationComponentActivationOptions locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                        .locationComponentOptions(customLocationComponentOptions)
                        .build();

        // Activate with options
        locationComponent.activateLocationComponent(locationComponentActivationOptions);

        // Enable to make component visible
        locationComponent.setLocationComponentEnabled(true);

        // Set the component's camera mode
        locationComponent.setCameraMode(CameraMode.TRACKING);

        // Set the component's render mode
        locationComponent.setRenderMode(RenderMode.COMPASS);

        // Add the location icon click listener
        locationComponent.addOnLocationClickListener(this);

        // Add the camera tracking listener. Fires if the map camera is manually moved.
        locationComponent.addOnCameraTrackingChangedListener(this);

    }

    @SuppressWarnings( {"MissingPermission"})
    @Override
    public void onLocationComponentClick() {
        if (locationComponent.getLastKnownLocation() != null) {
            Toast.makeText(this, "Your location is "+
                    locationComponent.getLastKnownLocation().getLatitude() + " "+
                    locationComponent.getLastKnownLocation().getLongitude(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCameraTrackingDismissed() {
        isInTrackingMode = false;
    }

    @Override
    public void onCameraTrackingChanged(int currentMode) {
// Empty on purpose
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "You have some issue", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(this, "user location permission not granted", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public boolean onMapClick(@NonNull LatLng latLng) {

        Log.d(TAG, "onMapClick: test in point " + latLng.getLatitude() +", " + latLng.getLongitude());

        String pick_message = "Your pick point";

        if (markerClick != null) {

            if(!markerClick.getTitle().equals(pick_message)) {

                if(marker != null ) marker.remove();

                markerClick.remove();
                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title("Your spot");
                markerClick = mapboxMap.addMarker(options);
            }
        } else {

            if(marker != null ) marker.remove();

            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title("Your spot");
            markerClick = mapboxMap.addMarker(options);
        }

        return false;
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        Log.d(TAG, "hideSoftKeyboard: calling");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class));
    }

}
