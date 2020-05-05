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
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class WalkersMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    private Button walkerLogoutButton;
    private Button settingsButton;
    private Button callHelperButton;
    private String walkerId;
    private LatLng walkerPickupLocation;
    private int radius = 1;
    private Boolean helperFound = false, requestType = false;
    private String helperFoundId;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference walkerDatabaseRef;
    private DatabaseReference helperAvailableRef;
    private DatabaseReference helperRef;
    private DatabaseReference helperLocationRef;
    Marker helperMarker, pickupMarker;
    GeoQuery geoQuery;
    private ValueEventListener helperLocationRefListener;
    private TextView txtName, txtPhone;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walkers_map);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        walkerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        walkerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Walkers Requests");
        helperAvailableRef = FirebaseDatabase.getInstance().getReference().child("Helpers Available");
        helperLocationRef = FirebaseDatabase.getInstance().getReference().child("Helpers Working");

        walkerLogoutButton = (Button) findViewById(R.id.walker_logout_btn);
        settingsButton = (Button) findViewById(R.id.walker_settings_btn);
        callHelperButton = (Button) findViewById(R.id.walkers_call_btn);

        txtName = findViewById(R.id.name_helper);
        txtPhone = findViewById(R.id.phone_helper);
        profilePic = findViewById(R.id.profile_image_helper);
        relativeLayout = findViewById(R.id.rel1);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WalkersMapActivity.this, SettingsActivity.class);
                intent.putExtra("type","Walkers");
                startActivity(intent);
            }
        });

        walkerLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                logoutWalker();
            }
        });

        callHelperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestType) {
                    requestType = false;
                    geoQuery.removeAllListeners();
                    helperLocationRef.removeEventListener(helperLocationRefListener);

                    if(helperFound != null) {
                        helperRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Helpers").child(helperFoundId).child("WalkerWalkId");
                        helperRef.removeValue();
                        helperFoundId = null;
                    }

                    helperFound = false;
                    radius = 1;

                    String walkerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    GeoFire geoFire = new GeoFire(walkerDatabaseRef);
                    geoFire.removeLocation(walkerId);

                    if(pickupMarker != null) {
                        pickupMarker.remove();
                    }

                    if(helperMarker != null) {
                        helperMarker.remove();
                    }

                    callHelperButton.setText("Call Helper");
                    relativeLayout.setVisibility(View.GONE);
                } else {
                    requestType = true;

                    String walkerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    GeoFire geoFire = new GeoFire(walkerDatabaseRef);
                    geoFire.setLocation(walkerId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                    walkerPickupLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(walkerPickupLocation).title("My location").icon(BitmapDescriptorFactory.fromResource(R.drawable.pickup)));

                    callHelperButton.setText("Getting Helper...");
                    getClosestHelper();
                }
            }
        });
    }

    private void getClosestHelper() {
        GeoFire geoFire = new GeoFire(helperAvailableRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(walkerPickupLocation.latitude, walkerPickupLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!helperFound && requestType) {
                    helperFound = true;
                    helperFoundId = key;

                    helperRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Helpers").child(helperFoundId);
                    HashMap helperMap = new HashMap();
                    helperMap.put("WalkerWalkId", walkerId);
                    helperRef.updateChildren(helperMap);

                    getHelperLocation();
                    callHelperButton.setText("Looking for Helper Location...");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!helperFound) {
                    radius = radius + 1;
                    getClosestHelper();
                    callHelperButton.setText("Looking for Helper location...");
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getHelperLocation() {
        helperLocationRefListener = helperLocationRef.child(helperFoundId).child("l").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && requestType) {
                    List<Object> helperLocationMap = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    callHelperButton.setText("Helper Found");

                    relativeLayout.setVisibility(View.VISIBLE);
                    getAssignedHelperInformation();

                    if(helperLocationMap.get(0) != null) {
                        locationLat = Double.parseDouble(helperLocationMap.get(0).toString());
                    }
                    if(helperLocationMap.get(1) != null) {
                        locationLng = Double.parseDouble(helperLocationMap.get(1).toString());
                    }

                    LatLng helperLatLng = new LatLng(locationLat, locationLng);
                    if(helperMarker != null) {
                        helperMarker.remove();
                    }

                    Location location1 = new Location("");
                    location1.setLatitude(walkerPickupLocation.latitude);
                    location1.setLongitude(walkerPickupLocation.longitude);

                    Location location2 = new Location("");
                    location2.setLatitude(helperLatLng.latitude);
                    location2.setLongitude(helperLatLng.longitude);

                    float distance = location1.distanceTo(location2);
                    if (distance < 90) {
                        callHelperButton.setText("Helper's Arrived");
                    } else {
                        callHelperButton.setText("Helper found: " + String.valueOf(distance));
                    }

                    helperMarker = mMap.addMarker(new MarkerOptions().position(helperLatLng).title("Helper is here").icon(BitmapDescriptorFactory.fromResource(R.drawable.helper)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedHelperInformation() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child("Helpers").child(helperFoundId);
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

    private void logoutWalker() {
        Intent welcomeIntent = new Intent(WalkersMapActivity.this, WelcomeActivity.class);
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

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
