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
    Location lastLocation;
    LocationRequest locationRequest;
    private Button logoutHelperButton;
    private Button settingsHelperButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Boolean currentLogoutHelperStatus = false;
    private DatabaseReference assignedWalkerRef, assignedWalkerPickupRef;
    private String helperId, walkerId = "";
    Marker pickupMarker;
    private ValueEventListener assignedWalkerPickupRefListener;

    private TextView txtName, txtPhone;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helpers_map);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        helperId = mAuth.getCurrentUser().getUid();

        logoutHelperButton = (Button) findViewById(R.id.helper_logout_btn);
        settingsHelperButton = (Button) findViewById(R.id.helper_settings_btn);

        txtName = findViewById(R.id.name_walker);
        txtPhone = findViewById(R.id.phone_walker);
        profilePic = findViewById(R.id.profile_image_walker);
        relativeLayout = findViewById(R.id.rel2);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        settingsHelperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HelpersMapActivity.this, SettingsActivity.class);
                intent.putExtra("type","Helpers");
                startActivity(intent);
            }
        });

        logoutHelperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLogoutHelperStatus = true;
                disconnectHelper();
                mAuth.signOut();
                logoutHelper();
            }
        });

        getAssignedWalkerRequest();
    }

    private void getAssignedWalkerRequest() {
        assignedWalkerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Helpers").child(helperId).child("WalkerWalkId");
        assignedWalkerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    walkerId = dataSnapshot.getValue().toString();
                    getAssignedWalkerPickupLocation();

                    relativeLayout.setVisibility(View.VISIBLE);
                    getAssignedWalkerInformation();

                } else {
                    walkerId = "";

                    if(pickupMarker != null) {
                        pickupMarker.remove();
                    }

                    if (assignedWalkerPickupRefListener != null) {
                        assignedWalkerPickupRef.removeEventListener(assignedWalkerPickupRefListener);
                    }
                    relativeLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedWalkerInformation() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child("Walkers").child(walkerId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void getAssignedWalkerPickupLocation() {
        assignedWalkerPickupRef = FirebaseDatabase.getInstance().getReference().child("Walker Requests").child(walkerId).child("l");
        assignedWalkerPickupRefListener = assignedWalkerPickupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    List<Object> walkerLocationMap = (List<Object>) dataSnapshot.getValue();

                    double locationLat = 0;
                    double locationLng = 0;

                    if(walkerLocationMap.get(0) != null) {
                        locationLat = Double.parseDouble(walkerLocationMap.get(0).toString());
                    }
                    if(walkerLocationMap.get(1) != null) {
                        locationLng = Double.parseDouble(walkerLocationMap.get(1).toString());
                    }

                    LatLng helperLatLng = new LatLng(locationLat, locationLng);
                    mMap.addMarker(new MarkerOptions().position(helperLatLng).title("Pickup Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.pickup)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void logoutHelper() {
        Intent welcomeIntent = new Intent(HelpersMapActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if(getApplicationContext() != null) {
            lastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference helperAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("HelpersAvailable");
            GeoFire geoFireAvailability = new GeoFire(helperAvailabilityRef);

            DatabaseReference helperWorkingRef = FirebaseDatabase.getInstance().getReference().child("Helpers Working");
            GeoFire geoFireWorking = new GeoFire(helperWorkingRef);

            switch (walkerId) {
                case "":
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailability.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvailability.removeLocation(userId);
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(!currentLogoutHelperStatus) {
            disconnectHelper();
        }
    }

    private void disconnectHelper() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference helperAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("HelpersAvailable");

        GeoFire geoFire = new GeoFire(helperAvailabilityRef);
        geoFire.removeLocation(userId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);
    }
}
