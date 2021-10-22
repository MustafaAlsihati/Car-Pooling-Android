package iau.ccsit.carpooling.ui.Profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import iau.ccsit.carpooling.EditProfile;
import iau.ccsit.carpooling.NetworkCheck;
import iau.ccsit.carpooling.R;
import iau.ccsit.carpooling.User;

public class ProfileFragment extends Fragment {

    private View view;
    Button editProfile;
    Context context;
    TextView userName, email;
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);
        context = getContext();
        editProfile = (Button) view.findViewById(R.id.editProfile_from_profile);
        userName = (TextView) view.findViewById(R.id.profile_userName);
        email = (TextView) view.findViewById(R.id.profile_email);
        progressBar = (ProgressBar) view.findViewById(R.id.profile_prgBar);

        if (NetworkCheck.IsNetworkConnected(context)) {
            firebaseAuth = FirebaseAuth.getInstance();
            user = firebaseAuth.getCurrentUser();
            if(firebaseAuth!=null) {
                try {
                    progressBar.setVisibility(View.VISIBLE);
                    getUserName();
                } catch (Exception e) {
                    //
                }
            }
        } else {
            Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show();
        }

        editProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), EditProfile.class);
                startActivity(intent);
            }
        });

        return view;
    }

    public void getUserName(){
        Query UserRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(user.getUid());
        UserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    User userInfo = dataSnapshot.getValue(User.class);
                    userName.setText(userInfo.username);
                    email.setText(user.getEmail());
                    progressBar.setVisibility(View.GONE);
                } catch (Exception e){
                    //exception
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}