package iau.ccsit.carpooling.ui.Locations;

import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import iau.ccsit.carpooling.MainActivity;
import iau.ccsit.carpooling.MapActivity;
import iau.ccsit.carpooling.ui.PlacesPluginActivity;
import iau.ccsit.carpooling.ui.StartTrip.Starttrip;

public class byPassFragment extends Fragment {



    @Override
    public void onStart() {
        Log.d("Pass", "onStart: Start!");
        super.onStart();
        startActivity(new Intent(getActivity(), PlacesPluginActivity.class));

//        startActivity(new Intent(getActivity(), LocationFragment.class));

    }

//    @Override
//    public void onResume() {
//        Log.d("Pass", "onStart: back!");
//        super.onResume();
//        startActivity(new Intent(getActivity(), MainActivity.class));
//    }
}
