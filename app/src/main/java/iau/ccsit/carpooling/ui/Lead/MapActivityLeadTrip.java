package iau.ccsit.carpooling.ui.Lead;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import iau.ccsit.carpooling.CustomInfoWindowAdapter;
import iau.ccsit.carpooling.MainActivity;
import iau.ccsit.carpooling.PlaceAutocompleteAdapterNew;
import iau.ccsit.carpooling.R;
import iau.ccsit.carpooling.model.PlaceInfo;
import iau.ccsit.carpooling.ui.Locations.LocationFragment;

public class MapActivityLeadTrip extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener,
        PermissionsListener, OnCameraTrackingChangedListener, OnLocationClickListener {

    private static final String TAG = "MapActivityLeadTrip";

    private static final float DEFAULT_ZOOM = 15f;
    private static final LatLngBounds LAT_LNG_BOUNDS  =  LatLngBounds.from (71,136,-40,-168);

    private AutocompleteSessionToken autocompleteSessionToken;
    private PlacesClient placesClient;
    private PlaceAutocompleteAdapterNew autoCompleteAdpater;

    private Marker marker,markerClick;
    private com.mapbox.mapboxsdk.geometry.LatLng mylocationLatlng, latLng;

    private PermissionsManager permissionsManager;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private LocationComponent locationComponent;

    private boolean isInTrackingMode;


    private AutoCompleteTextView mSearchText;
    private ImageButton myLocation,mFilter;
    private Button mButton;
    private String addressPoint;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        Log.d(TAG, "onCreate: is called!!!");
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_key));
        Mapbox.setAccessToken(getString(R.string.mapbox_key));
        setContentView(R.layout.activity_map);

        mSearchText = (AutoCompleteTextView) findViewById(R.id.input_search);
        myLocation = (ImageButton) findViewById(R.id.myLocation);
        mFilter = (ImageButton) findViewById(R.id.filterMap);
        mButton = (Button) findViewById(R.id.request);
        mapView = (MapView) findViewById(R.id.mapView);

        mapView.onCreate(savedInstanceState);
        Intent intentCome = getIntent();
        Log.d(TAG, "onCreate: is number "+intentCome.getIntExtra("Num",0));
        int numType =0;

        try {
            numType = intentCome.getIntExtra("Num",0);
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),"Error is "+e.getMessage(),Toast.LENGTH_LONG).show();
        }

        if(numType == 1) {
            mButton.setText("Set your start point");
        }else{
            mButton.setText("Set your destination point");
        }

        mapView.getMapAsync(this);
//        getLocationPermission();
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
            }
        });

        mFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked place info icon");
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapActivityLeadTrip.this);
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
        });

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(markerClick != null) {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Log.d(TAG, "onClick: check " + userId);

                    latLng = new LatLng(markerClick.getPosition().getLatitude(), markerClick.getPosition().getLongitude());

                    Geocoder geocoder;
                    List<Address> addresses;
                    geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                    String address="";
                    String city="";
                    String country="";

                    try {
                        addresses = geocoder.getFromLocation(latLng.getLatitude(), latLng.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                        address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                        city = addresses.get(0).getLocality();
                        country = addresses.get(0).getCountryName();

                    }catch (IOException e){
                        Toast.makeText(getApplicationContext(),"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();
                    }

                    addressPoint = address+", "+country;


                    //pickuplocation = new LatLng(markerClick.getPosition().latitude, markerClick.getPosition().longitude);
                    //map.clear();
                    //map.addMarker(new MarkerOptions().position(pickuplocation).title("Pickup here"));
                    markerClick.setTitle("Your pick point");
                    mButton.setText("Done");
                    mButton.setEnabled(false);

                    Intent intentCome = getIntent();
                    int numType = intentCome.getIntExtra("Num",0);

                    if(numType == 1) {
                        Toast.makeText(getApplicationContext(), "Set your pick point done", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent();
                        intent.putExtra("address", addressPoint);
                        intent.putExtra("lat", latLng.getLatitude());
                        intent.putExtra("lng", latLng.getLongitude());
                        setResult(2, intent);
                        finish();
                    }else{
                        Toast.makeText(getApplicationContext(), "Set your pick point done", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent();
                        intent.putExtra("address", addressPoint);
                        intent.putExtra("lat", latLng.getLatitude());
                        intent.putExtra("lng", latLng.getLongitude());
                        setResult(3, intent);
                        finish();
                    }
                }
                else{
                    //myLocationPickPoint();
                }
            }
        });


        hideSoftKeyboard();

    }

    private void myLocationPickPoint(){

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapActivityLeadTrip.this);
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

//                pickuplocation = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
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


    private void geoLocate(){
        Log.d(TAG, "geoLocate: geolocating");
        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapActivityLeadTrip.this);
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

    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.getLatitude() + ", lng: " + latLng.getLongitude() );
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

//        if(!title.equals("My Location")){
//            map.clear();
//            MarkerOptions options = new MarkerOptions()
//                    .position(latLng)
//                    .icon(bitmapDescriptorFromVector(getApplicationContext(),R.drawable.ic_fiber_manual_record_black_24dp))
//                    .title(title);
//            marker = map.addMarker(options);
//        }

        hideSoftKeyboard();
    }

    private void moveCameraSearch(LatLng latLng, float zoom){
        Log.d(TAG, "moveCameraSearch: moving the camera to: lat: " + latLng.getLatitude() + ", lng: " + latLng.getLongitude() );
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        hideSoftKeyboard();
    }


//    private Bitmap bitmapFromVector(Context context, int vectorResId){
//        Drawable vectorDrawable = ContextCompat.getDrawable(context,vectorResId);
//        vectorDrawable.setBounds(0,0,vectorDrawable.getIntrinsicWidth(),vectorDrawable.getIntrinsicHeight());
//        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),vectorDrawable.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
//
//        return bitmap;
//    }


    private void initMap(@NonNull Style loadedMapStyle){
        Log.d(TAG,"initMap: initializing map");
        setLocationComponent(loadedMapStyle);

        init();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "OnRequestPermissionsResult: called.");
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        Log.d(TAG, "hideSoftKeyboard: calling");
    }


    //----------------- google place API autocomplete suggestions ------------------

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: here map!");
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: is called!!!");
        super.onBackPressed();

    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG,"OnMapReady: map is ready");

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_API_key));
        }

        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                mapboxMap.getUiSettings().setZoomGesturesEnabled(true);
                mapboxMap.getUiSettings().setQuickZoomGesturesEnabled(true);
                mapboxMap.getUiSettings().setCompassEnabled(true);

                //instead of getLocationPermission()
                enableLocationComponent(style);

            }
        });

        mapboxMap.addOnMapClickListener(this);

        autocompleteSessionToken = AutocompleteSessionToken.newInstance();
        placesClient = Places.createClient(this);
        init();

    }

    @Override
    public boolean onMapClick(@NonNull LatLng latLng) {

        Log.d(TAG, "onMapClick: test");

        String pick_message = "Your pick point";

        if(markerClick != null){

            if(!markerClick.getTitle().equals(pick_message)) {
                markerClick.remove();
                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title("Your spot");
                markerClick = mapboxMap.addMarker(options);
            }

        }else{
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title("Your spot");
            markerClick = mapboxMap.addMarker(options);
        }

        return false;
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
    public void onCameraTrackingDismissed() {
        isInTrackingMode = false;
    }

    @Override
    public void onCameraTrackingChanged(int currentMode) {

    }

    @Override
    public void onLocationComponentClick() {

    }
}
