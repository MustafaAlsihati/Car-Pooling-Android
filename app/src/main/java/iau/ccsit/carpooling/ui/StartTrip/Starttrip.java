package iau.ccsit.carpooling.ui.StartTrip;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
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
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.List;

import iau.ccsit.carpooling.MainActivity;
import iau.ccsit.carpooling.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Starttrip extends AppCompatActivity implements OnMapReadyCallback, OnLocationClickListener,
        PermissionsListener, OnCameraTrackingChangedListener, MapboxMap.OnMapClickListener{

    private static final String TAG = "StartTrip";

    private MapView mapView;
    private MapboxMap mapboxMap;
    private Button startButton;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location originLocation;
    private Point originPosition;
    private Point destinationPosition;
    private Marker destinationMarker;
    private NavigationMapRoute navigationMapRoute;
    private LocationComponent locationComponent;
    private DirectionsRoute route;
    private MapboxNavigation navigation;
    private MapboxNavigationOptions options;
    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_key));
        Mapbox.setAccessToken(getString(R.string.mapbox_key));
        setContentView(R.layout.activity_starttrip);
        accessToken = Mapbox.getAccessToken();

        options = MapboxNavigationOptions.builder()
                .isDebugLoggingEnabled(true)
                .build();

        navigation = new MapboxNavigation(this,accessToken,options);

        mapView = (MapView) findViewById(R.id.mapView);
        startButton = (Button) findViewById(R.id.startTrip);

        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i(TAG, "onClick: is clicked!!!");

                NavigationRoute.builder(v.getContext())
                        .accessToken(accessToken)
                        .origin(originPosition)
                        .addWaypoint(Point.fromLngLat( 49.6764, 25.4159))
                        .destination(destinationPosition)
                        .build()
                        .getRoute(new Callback<DirectionsResponse>() {
                            @Override
                            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                                if(response.body() == null){
                                    Log.e(TAG, "onResponse: no route found, check right user");
                                    return;
                                } else if (response.body().routes().size() == 0){
                                    Log.e(TAG, "onResponse: no route found");
                                    return;
                                }

                                Log.i(TAG, "onResponse: route found!");
                                route = response.body().routes().get(0);

                            }

                            @Override
                            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                                Log.e(TAG, "Error: "+ t.getMessage());
                            }
                        });

                if(route != null) {
                    Log.i(TAG, "onClick: is okay!");
                    NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                            .directionsRoute(route)
                            .shouldSimulateRoute(true)
                            .build();

                    NavigationLauncher.startNavigation(Starttrip.this, options);
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap1) {
        this.mapboxMap = mapboxMap1;
//        Style.MAPBOX_STREETS
        mapboxMap1.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                mapboxMap.getUiSettings().setZoomGesturesEnabled(true);
                mapboxMap.getUiSettings().setQuickZoomGesturesEnabled(true);
                mapboxMap.getUiSettings().setCompassEnabled(false);

                //instead of getLocationPermission()
                enableLocationComponent(style);

            }
        });

        mapboxMap1.addOnMapClickListener(this);


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

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        Log.d(TAG, "onMapClick: test in point " + point.getLatitude() +", " + point.getLongitude());

        String pick_message = "Your pick point";

        destinationPosition = Point.fromLngLat(point.getLongitude(),point.getLatitude());

        if (destinationMarker != null) {
                destinationMarker.remove();

            destinationMarker = mapboxMap.addMarker(new MarkerOptions().position(point));

            originPosition = com.mapbox.geojson.Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),locationComponent.getLastKnownLocation().getLatitude());
            getRoute(originPosition,destinationPosition);

            startButton.setEnabled(true);
            startButton.setClickable(true);

            startButton.setBackgroundResource(R.color.mapbox_blue);
        } else{
            destinationMarker = mapboxMap.addMarker(new MarkerOptions().position(point));

            originPosition = com.mapbox.geojson.Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),locationComponent.getLastKnownLocation().getLatitude());
            getRoute(originPosition,destinationPosition);

            startButton.setEnabled(true);
            startButton.setClickable(true);

            startButton.setBackgroundResource(R.color.mapbox_blue);
        }




        return false;

    }

    private void getRoute(Point originPosition, Point destinationPosition){
        NavigationRoute.builder(this)
                .accessToken(accessToken)
                .origin(originPosition)
                .addWaypoint(Point.fromLngLat(49.6764, 25.4159))
                .destination(destinationPosition)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if(response.body() == null){
                            Log.e(TAG, "onResponse: no route found, check right user");
                            return;
                        } else if (response.body().routes().size() == 0){
                            Log.e(TAG, "onResponse: no route found");
                            return;
                        }

                        DirectionsRoute currentRoute = response.body().routes().get(0);

                        if(navigationMapRoute != null){
                            navigationMapRoute.removeRoute();
                        } else {

                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap);
                        }
                        navigationMapRoute.addRoute(currentRoute);

                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e(TAG, "Error: "+ t.getMessage());
                    }
                });
    }


    private void initMap(@NonNull Style loadedMapStyle){
        Log.i(TAG, "initMap: is called!!!");

        setLocationComponent(loadedMapStyle);

        if(locationComponent == null){
            Log.i(TAG, "initMap: LC is null !!!");
        }else{
            Log.i(TAG, "initMap: LC is not null !!!");
        }


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
//        isInTrackingMode = false;
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
        navigation.onDestroy();
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

}
