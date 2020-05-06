package com.groupfeb.safe;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.legacy.app.ActivityCompat;

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
import com.google.firebase.database.annotations.Nullable;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class WalkersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location LastLocation;
    LocationRequest locationRequest;

    private Button Logout;
    private Button SettingsButton;
    private Button CallHelperButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference WalkerDatabaseRef;
    private LatLng WalkerPickUpLocation;

    private DatabaseReference HelperAvailableRef, HelperLocationRef;
    private DatabaseReference HelpersRef;
    private int radius = 1;

    private Boolean helperFound = false, requestType = false;
    private String helperFoundID;
    private String walkerID;
    Marker HelperMarker, PickUpMarker;
    GeoQuery geoQuery;

    private ValueEventListener HelperLocationRefListner;


    private TextView txtName, txtPhone;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walkers_map);



        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        walkerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        WalkerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Walker Requests");
        HelperAvailableRef = FirebaseDatabase.getInstance().getReference().child("Helpers Available");
        HelperLocationRef = FirebaseDatabase.getInstance().getReference().child("Helpers Working");



        Logout = (Button) findViewById(R.id.logout_walker_btn);
        SettingsButton = (Button) findViewById(R.id.settings_walker_btn);
        CallHelperButton =  (Button) findViewById(R.id.call_helper_button);

        txtName = findViewById(R.id.name_helper);
        txtPhone = findViewById(R.id.phone_helper);
        profilePic = findViewById(R.id.profile_image_helper);
        relativeLayout = findViewById(R.id.rel1);



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        SettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(WalkersMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Walkers");
                startActivity(intent);
            }
        });

        Logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                mAuth.signOut();

                LogOutUser();
            }
        });



        CallHelperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (requestType)
                {
                    requestType = false;
                    geoQuery.removeAllListeners();
                    HelperLocationRef.removeEventListener(HelperLocationRefListner);

                    if (helperFound != null)
                    {
                        HelpersRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Helpers").child(helperFoundID).child("WalkerRideID");

                        HelpersRef.removeValue();

                        helperFoundID = null;
                    }

                    helperFound = false;
                    radius = 1;

                    String walkerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    GeoFire geoFire = new GeoFire(WalkerDatabaseRef);
                    geoFire.removeLocation(walkerId);

                    if (PickUpMarker != null)
                    {
                        PickUpMarker.remove();
                    }
                    if (HelperMarker != null)
                    {
                        HelperMarker.remove();
                    }

                    CallHelperButton.setText("Call a Cab");
                    relativeLayout.setVisibility(View.GONE);
                }
                else
                {
                    requestType = true;

                    String walkerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    GeoFire geoFire = new GeoFire(WalkerDatabaseRef);
                    geoFire.setLocation(walkerId, new GeoLocation(LastLocation.getLatitude(), LastLocation.getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {

                        }
                    });

                    WalkerPickUpLocation = new LatLng(LastLocation.getLatitude(), LastLocation.getLongitude());
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(WalkerPickUpLocation).title("My Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.pickup)));

                    CallHelperButton.setText("Getting your Helper...");
                    getClosetHelperCab();
                }
            }
        });
    }




    private void getClosetHelperCab()
    {
        GeoFire geoFire = new GeoFire(HelperAvailableRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(WalkerPickUpLocation.latitude, WalkerPickUpLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location)
            {
                //anytime the helper is called this method will be called
                //key=helperID and the location
                if(!helperFound && requestType)
                {
                    helperFound = true;
                    helperFoundID = key;


                    //we tell helper which walker he is going to have

                    HelpersRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Helpers").child(helperFoundID);
                    HashMap helpersMap = new HashMap();
                    helpersMap.put("WalkerRideID", walkerID);
                    HelpersRef.updateChildren(helpersMap);

                    //Show helper location on walkerMapActivity
                    GettingHelperLocation();
                    CallHelperButton.setText("Looking for Helper Location...");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady()
            {
                if(!helperFound)
                {
                    radius = radius + 1;
                    getClosetHelperCab();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }





    //and then we get to the helper location - to tell walker where is the helper
    private void GettingHelperLocation()
    {
        HelperLocationRefListner = HelperLocationRef.child(helperFoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists()  &&  requestType)
                        {
                            List<Object> helperLocationMap = (List<Object>) dataSnapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;
                            CallHelperButton.setText("Helper Found");


                            relativeLayout.setVisibility(View.VISIBLE);
                            getAssignedHelperInformation();


                            if(helperLocationMap.get(0) != null)
                            {
                                LocationLat = Double.parseDouble(helperLocationMap.get(0).toString());
                            }
                            if(helperLocationMap.get(1) != null)
                            {
                                LocationLng = Double.parseDouble(helperLocationMap.get(1).toString());
                            }

                            //adding marker - to pointing where helper is - using this lat lng
                            LatLng HelperLatLng = new LatLng(LocationLat, LocationLng);
                            if(HelperMarker != null)
                            {
                                HelperMarker.remove();
                            }


                            Location location1 = new Location("");
                            location1.setLatitude(WalkerPickUpLocation.latitude);
                            location1.setLongitude(WalkerPickUpLocation.longitude);

                            Location location2 = new Location("");
                            location2.setLatitude(HelperLatLng.latitude);
                            location2.setLongitude(HelperLatLng.longitude);

                            float Distance = location1.distanceTo(location2);

                            if (Distance < 90)
                            {
                                CallHelperButton.setText("Helper's Reached");
                            }
                            else
                            {
                                CallHelperButton.setText("Helper Found: " + String.valueOf(Distance));
                            }

                            HelperMarker = mMap.addMarker(new MarkerOptions().position(HelperLatLng).title("your helper is here").icon(BitmapDescriptorFactory.fromResource(R.drawable.helper)));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }




    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        // now let set user location enable
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
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

    @Override
    public void onLocationChanged(Location location)
    {
        //getting the updated location
        LastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }


    //create this method -- for useing apis
    protected synchronized void buildGoogleApiClient()
    {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }


    @Override
    protected void onStop()
    {
        super.onStop();
    }


    public void LogOutUser()
    {
        Intent startPageIntent = new Intent(WalkersMapActivity.this, WelcomeActivity.class);
        startPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startPageIntent);
        finish();
    }



    private void getAssignedHelperInformation()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Helpers").child(helperFoundID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();
                    String car = dataSnapshot.child("car").getValue().toString();

                    txtName.setText(name);
                    txtPhone.setText(phone);

                    if (dataSnapshot.hasChild("image"))
                    {
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