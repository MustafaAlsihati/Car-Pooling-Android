package iau.ccsit.carpooling;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;


import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import iau.ccsit.carpooling.ui.About.AboutFragment;
import iau.ccsit.carpooling.ui.FindRide.FindFragment;
import iau.ccsit.carpooling.ui.Lead.LeadFragment;
import iau.ccsit.carpooling.ui.Locations.LocationFragment;
import iau.ccsit.carpooling.ui.Locations.byPassFragment;
import iau.ccsit.carpooling.ui.MyBoard.MyBoardFragment;
import iau.ccsit.carpooling.ui.Profile.ProfileFragment;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private AppBarConfiguration mAppBarConfiguration;
    private DrawerLayout drawer;
    private TextView navUserName, navEmail;
    private FirebaseAuth firebaseAuth;
    Context context;
    String uid;

    private static final String TAG = "Main Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(!NetworkCheck.IsNetworkConnected(context)) {
            Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth = FirebaseAuth.getInstance();
        uid = firebaseAuth.getCurrentUser().getUid();

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_findRide, R.id.nav_lead, R.id.nav_board,
                R.id.nav_locations, R.id.nav_profile, R.id.nav_about)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer,toolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        View headerView = navigationView.getHeaderView(0);
        navUserName = (TextView) headerView.findViewById(R.id.navUserName);
        navEmail = (TextView) headerView.findViewById(R.id.navEmail);

        Query UserRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid);
        UserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    User user = dataSnapshot.getValue(User.class);
                    navUserName.setText(user.username);
                    navEmail.setText(firebaseAuth.getCurrentUser().getEmail());
                } catch (Exception e){
                    //exception
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //when visit at first time
        if(savedInstanceState == null){
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment,new MyBoardFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_board);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.LogOut) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(), Login.class));
            Toast.makeText(MainActivity.this, "Signed Out successfully", Toast.LENGTH_LONG).show();
        }
        if (item.getItemId()==R.id.action_settings){
//            Intent intent = new Intent(context, Settings.class);
        }
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || this.onSupportNavigateUp();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        Log.d(TAG, "onNavigationItemSelected: test is work fine!");

        switch (menuItem.getItemId()){

            case R.id.nav_findRide:
                getSupportFragmentManager().beginTransaction()
                            .replace(R.id.nav_host_fragment,new FindFragment()).commit();
                break;
            case R.id.nav_lead:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment,new LeadFragment()).commit();
                break;
            case R.id.nav_board:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment,new MyBoardFragment()).commit();
                break;
            case R.id.nav_locations:
                Log.d(TAG, "onNavigationItemSelected: done");
//                startActivity(new Intent(getApplicationContext(),MapActivity.class));
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment,new byPassFragment()).commit();
                break;
            case R.id.nav_profile:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment,new ProfileFragment()).commit();
                break;
            case R.id.nav_about:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment,new AboutFragment()).commit();
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)){
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

}
