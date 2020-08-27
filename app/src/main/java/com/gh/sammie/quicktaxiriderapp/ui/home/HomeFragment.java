package com.gh.sammie.quicktaxiriderapp.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.gh.sammie.quicktaxiriderapp.Common;
import com.gh.sammie.quicktaxiriderapp.R;
import com.gh.sammie.quicktaxiriderapp.callbacks.IFirebaseDriversInfoListener;
import com.gh.sammie.quicktaxiriderapp.callbacks.IFirebaseFailedListener;
import com.gh.sammie.quicktaxiriderapp.model.DriverGeoModel;
import com.gh.sammie.quicktaxiriderapp.model.DriverInfoModel;
import com.gh.sammie.quicktaxiriderapp.model.GeoQueryModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class HomeFragment extends Fragment implements OnMapReadyCallback, IFirebaseFailedListener, IFirebaseDriversInfoListener {

    private HomeViewModel homeViewModel;
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    //location
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private boolean firstTime = true;

    // Load driver
    private double distance = 1.0; //default
    private static final double LIMIT_RANGE = 10.0; // km
    private Location previousLocation, currentLocation; //Use to calculate distance

    //Listeners
    IFirebaseDriversInfoListener iFirebaseDriversInfoListener;
    IFirebaseFailedListener iFirebaseFailedListener;
    private String cityName;


    @Override
    public void onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);


        init();
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        return root;


    }

    private void init() {
        iFirebaseDriversInfoListener = this;
        iFirebaseFailedListener = this;

        locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(10f);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));

                //if user has change location, calculate and load driver again
                if (firstTime) {
                    previousLocation = currentLocation = locationResult.getLastLocation();
                    firstTime = false;


                } else {
                    previousLocation = currentLocation;
                    currentLocation = locationResult.getLastLocation();

                }

                if (previousLocation.distanceTo(currentLocation) / 1000 <= LIMIT_RANGE) // not over range
                {
                    loadAvailableDrivers();
                } else {// do nothing
                }


            }
        };

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        loadAvailableDrivers();

    }

    private void loadAvailableDrivers() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Snackbar.make(getView(), "Location permission required", Snackbar.LENGTH_SHORT).show();
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                //load all drivers around rider
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                List<Address> addressList;
                try {
                    addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    cityName = addressList.get(0).getLocality();

                    //query
                    DatabaseReference driver_loacation_ref = FirebaseDatabase.getInstance()
                            .getReference(Common.DRIVER_LOCATION_REFERENCES)
                            .child(cityName);

                    GeoFire gf = new GeoFire(driver_loacation_ref);
                    GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(location.getLatitude(),
                            location.getLongitude()), distance);
                    geoQuery.removeAllListeners();
                    geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                        @Override
                        public void onKeyEntered(String key, GeoLocation location) {
                            Common.driversFound.add(new DriverGeoModel(key, location));


                        }

                        @Override
                        public void onKeyExited(String key) {

                        }

                        @Override
                        public void onKeyMoved(String key, GeoLocation location) {

                        }

                        @Override
                        public void onGeoQueryReady() {
                            if (distance <= LIMIT_RANGE) {
                                distance++;
                                loadAvailableDrivers(); // continue search in  new disatnce
                            } else {
                                distance = 1.0; // reset it
                                addDriverMarker();

                            }

                        }

                        @Override
                        public void onGeoQueryError(DatabaseError error) {
                            Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_SHORT).show();

                        }
                    });

                    //linstener  for new  drivers in city and range
                    driver_loacation_ref.addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                            //have new driver
                            GeoQueryModel geoQueryModel = snapshot.getValue(GeoQueryModel.class);
                            GeoLocation geoLocation = new GeoLocation(geoQueryModel.getL().get(0),
                                    geoQueryModel.getL().get(1));

                            DriverGeoModel driverGeoModel = new DriverGeoModel(snapshot.getKey(), geoLocation);
                            Location newDriverLocation = new Location("");
                            newDriverLocation.setLatitude(geoLocation.latitude);
                            newDriverLocation.setLongitude(geoLocation.longitude);
                            float newDistance = location.distanceTo(newDriverLocation) / 1000; //in KM
                            if (newDistance <= LIMIT_RANGE)
                                findDriverBykey(driverGeoModel); //if driver in range

                        }

                        @Override
                        public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        }

                        @Override
                        public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                        }

                        @Override
                        public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                } catch (IOException e) {
                    e.printStackTrace();
                    Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addDriverMarker() {
        if (Common.driversFound.size() > 0) {
            Observable.fromIterable(Common.driversFound)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(driverGeoModel -> {
                        //on next
                        findDriverBykey(driverGeoModel);
                    }, throwable -> {
                        Snackbar.make(getView(), throwable.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }, () -> {

                    });


        } else {
            Snackbar.make(getView(), getString(R.string.drivers_not_found), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void findDriverBykey(DriverGeoModel driverGeoModel) {
        FirebaseDatabase.getInstance()
                .getReference(Common.DRIVER_INFO_REF)
                .child(driverGeoModel.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.hasChildren()) {
                            driverGeoModel.setDriverInfoModel(snapshot.getValue(DriverInfoModel.class));
                            iFirebaseDriversInfoListener.onDriverInfoLoadSuccess(driverGeoModel);
                        } else
                            iFirebaseFailedListener.OnFirebaseLoadFailed(getString(R.string.not_found_key) + driverGeoModel.getKey());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        iFirebaseFailedListener.OnFirebaseLoadFailed(error.getMessage());

                    }
                });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Check Permission
        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }

                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        mMap.setOnMyLocationButtonClickListener(() -> {
                            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                                return false;
                            }
                            fusedLocationProviderClient.getLastLocation().addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                                }
                            }).addOnSuccessListener(new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());  //fix null pointer error in the future
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f));

                                }
                            });
                            return true;
                        });

                        // Set layout button
                        View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1"))
                                .getParent())
                                .findViewById(Integer.parseInt("2"));

                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

                        //Right Bottom
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        params.setMargins(0, 0, 0, 250); // move view to see zoom controller


                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Snackbar.make(getView(), permissionDeniedResponse.getPermissionName() + "needs to be enabled",
                                Snackbar.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();

        mMap.getUiSettings().setZoomControlsEnabled(true);


        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.uber_maps_style));
            if (!success)
                Log.e("SAMMIE.GH_ERROR", "style parsing error");
//            Toast.makeText(getContext(), "Load map style failed", Toast.LENGTH_SHORT).show();

        } catch (Resources.NotFoundException e) {
            Log.e("SAMMIE.GH_ERROR", Objects.requireNonNull(e.getMessage()));
            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void OnFirebaseLoadFailed(String message) {
        Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();

    }

    @Override
    public void onDriverInfoLoadSuccess(DriverGeoModel driverGeoModel) {
        //if already have maker with this key dontr set again

        if (!Common.markerList.containsKey(driverGeoModel.getKey()))
            Common.markerList.put(driverGeoModel.getKey(),
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(driverGeoModel.getGeoLocation().latitude,
                                    driverGeoModel.getGeoLocation().longitude))
                            .flat(true)
                            .title(Common.buildName(driverGeoModel.getDriverInfoModel().getFirstName(),
                                    driverGeoModel.getDriverInfoModel().getLastName()))
                            .snippet(driverGeoModel.getDriverInfoModel().getPhoneNumber())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))));

        if (!TextUtils.isEmpty(cityName)) {
            DatabaseReference driverLocation = FirebaseDatabase.getInstance()
                    .getReference(Common.DRIVER_LOCATION_REFERENCES)
                    .child(cityName)
                    .child(driverGeoModel.getKey());

            driverLocation.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.hasChildren()) {
                        if (Common.markerList.get(driverGeoModel.getKey()) != null)
                            Common.markerList.get(driverGeoModel.getKey()).remove(); // remove maker

                        Common.markerList.remove(driverGeoModel.getKey()); // remove marker infor from hashmap
                        driverLocation.removeEventListener(this); // Remove event listener


                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_SHORT).show();

                }
            });
        }

    }
}

