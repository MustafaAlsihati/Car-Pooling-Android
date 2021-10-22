package iau.ccsit.carpooling.ui.Lead;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.mapbox.mapboxsdk.geometry.LatLng;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import iau.ccsit.carpooling.MainActivity;
import iau.ccsit.carpooling.R;

public class LeadFragment extends Fragment {
    private static final String TAG = "LeadFragment";


    //Widget Variables
    private View view;
    private Button button;
    private ImageView imageViewFrom, imageViewTo;
    private TextView textViewFrom, textViewTo;
    private DatePicker datePicker;
    private LatLng latLngFrom,latLngTo;
    private String addressFrom,addressTo;
    double latFrom, lngFrom, latTo, lngTo;

    //FireBase Variables
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: is called!!!");
        
        view = inflater.inflate(R.layout.fragment_lead, container, false);

        button = (Button) view.findViewById(R.id.btnConfirm);
        imageViewFrom = (ImageView) view.findViewById(R.id.imgVFrom);
        imageViewTo = (ImageView) view.findViewById(R.id.imgVTo);
        textViewFrom = (TextView) view.findViewById(R.id.textVFrom);
        textViewTo = (TextView) view.findViewById(R.id.textVTo);
        datePicker = (DatePicker) view.findViewById(R.id.startDateTrip);

        imageViewFrom.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MapActivityLeadTrip.class);
                intent.putExtra("Num",1);
                startActivityForResult(intent,2);
            }
        });

        imageViewTo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MapActivityLeadTrip.class);
                intent.putExtra("Num",2);
                startActivityForResult(intent,3);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String date = datePicker.getDayOfMonth() + "-" +
                        (datePicker.getMonth()+1) + "-" +
                        datePicker.getYear();

                if(validateDate(date)) {
                    setData(date);
                } else {
                    Toast.makeText(getActivity().getApplicationContext(),"Error : Invalid date",Toast.LENGTH_LONG).show();
                }
            }
        });
        return view;
    }

    private void setData(String selectedDate){
        Intent intent = new Intent(getContext(), Captain_2.class);
        intent.putExtra("latFrom", latFrom);
        intent.putExtra("lngFrom", lngFrom);
        intent.putExtra("latTo", latTo);
        intent.putExtra("lngTo", lngTo);
        intent.putExtra("addressFrom", addressFrom);
        intent.putExtra("addressTo", addressTo);
        intent.putExtra("selectedDate", selectedDate);
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1){
            Intent intent = new Intent(getContext(), MainActivity.class);
            getActivity().finish();
            startActivity(intent);
        }
        else if(requestCode == 2){
            latFrom = data.getDoubleExtra("lat",0.0);
            lngFrom = data.getDoubleExtra("lng",0.0);
            addressFrom = data.getStringExtra("address");
            textViewFrom.setText(addressFrom);
            latLngFrom = new LatLng(latFrom,lngFrom);
            System.out.println("From Lat: " + latFrom);
            System.out.println("From Lng: " + lngFrom);
        } else if (requestCode == 3) {
            latTo = data.getDoubleExtra("lat",0.0);
            lngTo = data.getDoubleExtra("lng",0.0);
            addressTo = data.getStringExtra("address");
            textViewTo.setText(addressTo);
            latLngTo = new LatLng(latTo,lngTo);
            System.out.println("To: " + latLngTo);
        } else {
            Log.d(TAG, "onActivityResult: backed!!!");
            Toast.makeText(getActivity().getApplicationContext(),"Your request is not prepared fine",Toast.LENGTH_LONG).show();
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