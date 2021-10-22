package iau.ccsit.carpooling.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;

import com.mapbox.mapboxsdk.Mapbox;
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
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import iau.ccsit.carpooling.MainActivity;
import iau.ccsit.carpooling.R;
import iau.ccsit.carpooling.ui.Locations.LocationFragment;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

/**
 * Use the places plugin to take advantage of Mapbox's location search ("geocoding") capabilities. The plugin
 * automatically makes geocoding requests, has built-in saved locations, includes location picker functionality,
 * and adds beautiful UI into your Android project.
 */
public class PlacesPluginActivity extends AppCompatActivity implements OnMapReadyCallback, OnLocationClickListener,
        PermissionsListener, OnCameraTrackingChangedListener, MapboxMap.OnMapClickListener, LocationCallback{

    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private static final String TAG = "PlacesPluginActivity";
    final ArrayList<String> LocationsIDs = new ArrayList<>();
    final ArrayList<GeoLocation> geoLocationsList = new ArrayList<>();



    private MapView mapView;
    private MapboxMap mapboxMap;
    private CarmenFeature loc1,loc2;
    private LocationComponent locationComponent;
    private PermissionsManager permissionsManager;
    private @NonNull Marker marker,markerClick;

    private String geojsonSourceLayerId = "geojsonSourceLayerId";
    private String symbolIconId = "symbolIconId";
    private ImageButton myLocation, mFilter;
    private Button mButton;
    private boolean isInTrackingMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.mapbox_key));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.act_map_search);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
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

                addUserLocations();

                init();

                // Add the symbol layer icon to map for future use
                style.addImage(symbolIconId, BitmapFactory.decodeResource(
                        PlacesPluginActivity.this.getResources(), R.drawable.blue_marker_view));

                // Create an empty GeoJSON source using the empty feature collection
                setUpSource(style);

                // Set up a new symbol layer for displaying the searched location's feature coordinates
                setupLayer(style);
            }
        });

        mapboxMap.addOnMapClickListener(this);
    }

    private void init() {

        mFilter = (ImageButton) findViewById(R.id.filterMap);
        myLocation = (ImageButton) findViewById(R.id.myLocation);
        mButton = (Button) findViewById(R.id.request);


        findViewById(R.id.fab_location_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if(geoLocationsList.size() > 1 && loc1 != null && loc2 != null) {

                    Intent intent = new PlaceAutocomplete.IntentBuilder()
                            .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.mapbox_key))
                            .placeOptions(PlaceOptions.builder()
                                    .backgroundColor(Color.parseColor("#EEEEEE"))
                                    .limit(10)
                                    .addInjectedFeature(loc1)
                                    .addInjectedFeature(loc2)
                                    .build(PlaceOptions.MODE_CARDS))
                            .build(PlacesPluginActivity.this);
                    startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);

                }
                else if (geoLocationsList.size() == 1 && loc1 != null ){
                    Intent intent = new PlaceAutocomplete.IntentBuilder()
                            .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.mapbox_key))
                            .placeOptions(PlaceOptions.builder()
                                    .backgroundColor(Color.parseColor("#EEEEEE"))
                                    .limit(10)
                                    .addInjectedFeature(loc1)
                                    .build(PlaceOptions.MODE_CARDS))
                            .build(PlacesPluginActivity.this);
                    startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
                }
                else{
                    Intent intent = new PlaceAutocomplete.IntentBuilder()
                            .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.mapbox_key))
                            .placeOptions(PlaceOptions.builder()
                                    .backgroundColor(Color.parseColor("#EEEEEE"))
                                    .limit(10)
                                    .build(PlaceOptions.MODE_CARDS))
                            .build(PlacesPluginActivity.this);
                    startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
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
                                    Toast.makeText(PlacesPluginActivity.this,"Location Saved",Toast.LENGTH_SHORT).show();
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

    // add the locations of user from firebase
    private void addUserLocations() {
        Log.i(TAG, "addUserLocations: is called!!!");

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("locations");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.i(TAG, "onDataChange: check the data");
                    collectAllLocations((Map<String, Object>) dataSnapshot.getValue());
                }else{
                    Log.d(TAG, "onDataChange1: there is no trip! ");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

//        loc1 = CarmenFeature.builder().text("Mapbox SF Office")
//                .geometry(Point.fromLngLat(-122.3964485, 37.7912561))
//                .placeName("50 Beale St, San Francisco, CA")
//                .id("mapbox-sf")
//                .properties(new JsonObject())
//                .build();
//
//        loc2 = CarmenFeature.builder().text("Mapbox DC Office")
//                .placeName("740 15th Street NW, Washington DC")
//                .geometry(Point.fromLngLat(-77.0338348, 38.899750))
//                .id("mapbox-dc")
//                .properties(new JsonObject())
//                .build();
    }

    private void setLocation(ArrayList<GeoLocation> LocationsList){

        Log.i(TAG, "setLocation: the size is " + LocationsList.size());

        if(LocationsList.size() == 0){
            return;
        }

        ArrayList<String> street = new ArrayList<>();
        ArrayList<String> city = new ArrayList<>();
        ArrayList<String> country = new ArrayList<>();
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        if(LocationsList.size() == 1){


            try{
                addresses = geocoder.getFromLocation(LocationsList.get(0).latitude,LocationsList.get(0).longitude,1);
                street.add(addresses.get(0).getAddressLine(0));
                city.add(addresses.get(0).getLocality());
                country.add(addresses.get(0).getCountryCode());
            }catch (IOException e){
                Toast.makeText(getApplicationContext(),"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();
            }

            loc1 = CarmenFeature.builder().text("Location one")
                    .geometry(Point.fromLngLat(LocationsList.get(0).latitude,LocationsList.get(0).longitude))
                    .placeName(street.get(0)+", "+city.get(0)+", "+country.get(0))
                    .id("loc1")
                    .properties(new JsonObject())
                    .build();

        }else{

            try{
                for(int i = 0; i<2; i++) {

                    addresses = geocoder.getFromLocation(LocationsList.get(i).latitude, LocationsList.get(i).longitude, 1);
                    street.add(addresses.get(0).getAddressLine(0));
                    city.add(addresses.get(0).getLocality());
                    country.add(addresses.get(0).getCountryCode());

                }

            }catch (IOException e){
                Toast.makeText(getApplicationContext(),"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();
            }

            for(int i = 0; i<2; i++) {
                CarmenFeature feature;

                feature = CarmenFeature.builder().text("Location" + ((i == 0) ? " one" : " two"))
                        .geometry(Point.fromLngLat(LocationsList.get(0).latitude,LocationsList.get(0).longitude))
                        .placeName(street.get(i)+", "+city.get(i)+", "+country.get(i))
                        .id("loc"+((i == 0) ? "1" : "2"))
                        .properties(new JsonObject())
                        .build();

                if(i == 0 ){
                    loc1 = feature;
                }else{
                    loc2 = feature;
                }

            }

        }



    }

    private void collectAllLocations(Map<String,Object> locations) {

        Log.i(TAG, "collectAllLocations: is called!!!");

        if(LocationsIDs != null){
            LocationsIDs.clear();
        }

        if(geoLocationsList != null){
            geoLocationsList.clear();
        }

        //Add the nearby trip key
        for (Map.Entry <String, Object> entry : locations.entrySet()){
            LocationsIDs.add( entry.getKey() );
            Log.i(TAG, "collectAllLocations: the key is " + entry.getKey() );
        }

        if (LocationsIDs == null || LocationsIDs.size() == 0){
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("locations");

        GeoFire geoFire = new GeoFire(ref);

                    for (int i = 0 ; i < LocationsIDs.size() ; i++) {
                        Log.i(TAG, "onDataChange2: work!");

                        //get data from Firebase
                        geoFire.getLocation(LocationsIDs.get(i), this);
                        Log.i(TAG, "collectAllLocations: the size is "+geoLocationsList.size());
                    }

                    //check if there is no near trip
                    if (geoLocationsList.size() == 0){
                        Toast.makeText(PlacesPluginActivity.this,"Sorry, there is no trip in list "+geoLocationsList.size(),Toast.LENGTH_LONG).show();
                    }else {
                        setLocation(geoLocationsList);
                    }

    }






    private void setUpSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(geojsonSourceLayerId));
    }

    private void setupLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addLayer(new SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
                iconImage(symbolIconId),
                iconOffset(new Float[] {0f, -8f})
        ));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

            // Retrieve selected location's CarmenFeature
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);

            // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
            // Then retrieve and update the source designated for showing a selected location's symbol layer icon

            if (mapboxMap != null) {
                Style style = mapboxMap.getStyle();
                if (style != null) {
                    GeoJsonSource source = style.getSourceAs(geojsonSourceLayerId);
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(
                                new Feature[] {Feature.fromJson(selectedCarmenFeature.toJson())}));
                    }

                    // Move map camera to the selected location
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                                            ((Point) selectedCarmenFeature.geometry()).longitude()))
                                    .zoom(14)
                                    .build()), 4000);
                }
            }
        }
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
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
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class));
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

    private void initMap(@NonNull Style loadedMapStyle) {

        setLocationComponent(loadedMapStyle);

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


    @Override
    public void onLocationResult(String key, GeoLocation location) {

        geoLocationsList.add(location);

    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        Log.e(TAG, "onCancelled: no locations for user");
    }
}
