package com.groupfeb.safe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class HelpersMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location LastLocation;
    LocationRequest locationRequest;

    private Button LogoutHelperBtn;
    private Button SettingsHelperButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Boolean currentLogOutUserStatus = false;

    //getting request walker's id
    private String walkerID = "";
    private String helperID;
    private DatabaseReference AssignedWalkerRef;
    private DatabaseReference AssignedWalkerPickUpRef;
    Marker PickUpMarker;

    private ValueEventListener AssignedWalkerPickUpRefListner;

    private TextView txtName, txtPhone;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //notice
        setContentView(R.layout.activity_helpers_map);


        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        helperID = mAuth.getCurrentUser().getUid();


        LogoutHelperBtn = (Button) findViewById(R.id.logout_helper_btn);
        SettingsHelperButton = (Button) findViewById(R.id.settings_helper_btn);

        txtName = findViewById(R.id.name_walker);
        txtPhone = findViewById(R.id.phone_walker);
        profilePic = findViewById(R.id.profile_image_walker);
        relativeLayout = findViewById(R.id.rel2);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(HelpersMapActivity.this);


        SettingsHelperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HelpersMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Helpers");
                startActivity(intent);
            }
        });

        LogoutHelperBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLogOutUserStatus = true;
                DisconnectHelper();

                mAuth.signOut();

                LogOutUser();
            }
        });


        getAssignedWalkersRequest();
    }


    private void getAssignedWalkersRequest() {
        AssignedWalkerRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Helpers").child(helperID).child("WalkerWalkID");

        AssignedWalkerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    walkerID = dataSnapshot.getValue().toString();
                    //getting assigned walker location
                    GetAssignedWalkerPickupLocation();

                    relativeLayout.setVisibility(View.VISIBLE);
                    getAssignedWalkerInformation();
                } else {
                    walkerID = "";

                    if (PickUpMarker != null) {
                        PickUpMarker.remove();
                    }

                    if (AssignedWalkerPickUpRefListner != null) {
                        AssignedWalkerPickUpRef.removeEventListener(AssignedWalkerPickUpRefListner);
                    }

                    relativeLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void GetAssignedWalkerPickupLocation() {
        AssignedWalkerPickUpRef = FirebaseDatabase.getInstance().getReference().child("Walker Requests")
                .child(walkerID).child("l");

        AssignedWalkerPickUpRefListner = AssignedWalkerPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<Object> walkerLocationMap = (List<Object>) dataSnapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;

                    if (walkerLocationMap.get(0) != null) {
                        LocationLat = Double.parseDouble(walkerLocationMap.get(0).toString());
                    }
                    if (walkerLocationMap.get(1) != null) {
                        LocationLng = Double.parseDouble(walkerLocationMap.get(1).toString());
                    }

                    LatLng HelperLatLng = new LatLng(LocationLat, LocationLng);
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(HelperLatLng).title("Walker PickUp Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.idefault_user)));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // now let set user location enable
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);


    }

    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null) {
            //getting the updated location
            LastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));


            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference HelpersAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Helpers Available");
            GeoFire geoFireAvailability = new GeoFire(HelpersAvailabilityRef);

            DatabaseReference HelpersWorkingRef = FirebaseDatabase.getInstance().getReference().child("Helpers Working");
            GeoFire geoFireWorking = new GeoFire(HelpersWorkingRef);

            switch (walkerID) {
                case "":
                    geoFireWorking.removeLocation(userID);
                    geoFireAvailability.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;

                default:
                    geoFireAvailability.removeLocation(userID);
                    geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //

            return;
        }
        //it will handle the refreshment of the location
        //if we dont call it we will get location only once
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    //create this method -- for useing apis
    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }


    @Override
    protected void onStop() {
        super.onStop();

        if (!currentLogOutUserStatus) {
            DisconnectHelper();
        }
    }


    private void DisconnectHelper() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference HelpersAvailabiltyRef = FirebaseDatabase.getInstance().getReference().child("Helpers Available");

        GeoFire geoFire = new GeoFire(HelpersAvailabiltyRef);
        geoFire.removeLocation(userID);
    }


    public void LogOutUser() {
        Intent startPageIntent = new Intent(HelpersMapActivity.this, WelcomeActivity.class);
        startPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startPageIntent);
        finish();
    }


    private void getAssignedWalkerInformation() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Walkers").child(walkerID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();

                    txtName.setText(name);
                    txtPhone.setText(phone);

                    if (dataSnapshot.hasChild("image")) {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profilePic);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}