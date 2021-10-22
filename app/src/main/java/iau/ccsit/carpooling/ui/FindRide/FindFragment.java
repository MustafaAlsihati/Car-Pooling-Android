package iau.ccsit.carpooling.ui.FindRide;

import android.Manifest;
import android.content.Context;
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
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Map;
import iau.ccsit.carpooling.MainActivity;
import iau.ccsit.carpooling.R;

import static android.content.Context.LOCATION_SERVICE;

public class FindFragment extends Fragment {

    private static final String TAG = "FindFragment";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private Boolean mLocationPermissionGranted = false;
    private  LocationManager  locationManager;


    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LatLng currentLatLng;

    private View view;
    private Spinner nearbySpinner;
    private Button createCustomRideBtn;
    Context context;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_findride, container, false);
        context = getContext();
        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        createCustomRideBtn = (Button) view.findViewById(R.id.createCustomRideBtn);
        nearbySpinner = (Spinner) view.findViewById(R.id.nearbySpinner);
        createCustomRideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //here...
            }
        });

        nearbySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position != 0){
                    Log.d(TAG, "onItemSelected: it is work!");
                    BottomList bottomList = new BottomList();
                    Bundle args = new Bundle();
                    args.putInt("range", Range(position));
                    bottomList.setArguments(args);
                    bottomList.show(((AppCompatActivity) context).getSupportFragmentManager(), "BottomList");

                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // if nothing selected here...
            }
        });



        return view;
    }


    public int Range(int position){
        int range=0;
        switch (position) {
            case 1:
                range = 1;
                break;
            case 2:
                range = 3;
                break;
            case 3:
                range = 5;
                break;
            case 4:
                range = 10;
                break;
        }
        return range;
    }
}