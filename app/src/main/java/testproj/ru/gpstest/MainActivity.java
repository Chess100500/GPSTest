package testproj.ru.gpstest;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener,
        AddGeofenceDialog.AddGeofenceDialogListener, ResultCallback<Status> {
    protected GoogleApiClient mGoogleApiClient;
    private TextView latText;
    private TextView longText;
    private TextView timeText;
    private TextView currentGeofence;
    private Button maddGeofence;
    private Button mremoveGeofence;
    private String currentGeofenceName;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private String mLastUpdateTime;
    protected Boolean mRequestingLocationUpdates;
    private PendingIntent mGeofencePendingIntent;
    protected List<Geofence> mGeofenceList;
    private SharedPreferences mSharedPreferences;
    private boolean mGeofenceAdded;
    protected static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latText = (TextView) findViewById(R.id.latText);
        longText = (TextView) findViewById(R.id.longText);
        timeText = (TextView) findViewById(R.id.timeText);
        maddGeofence = (Button) findViewById(R.id.addGeofence);
        mremoveGeofence = (Button) findViewById(R.id.removeGeofence);
        currentGeofence = (TextView) findViewById(R.id.currentGeofence);

        mGeofenceList = new ArrayList<>();
        mGeofencePendingIntent = null;
        mRequestingLocationUpdates = false;

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);

        mGeofenceAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);

        setButtonsEnabledState();
        buildGoogleApiClient();
        createLocationRequest();
        updateValuesFromBundle(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("send-geofence-name"));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentGeofenceName = intent.getStringExtra("geofenceName");
        }
    };

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Toast.makeText(this, "Connectedto GoogleApiClient", Toast.LENGTH_SHORT).show();

        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            updateUI();

            if (mRequestingLocationUpdates) {
                startLocationUpdates();
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Toast.makeText(this, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection suspended", Toast.LENGTH_SHORT).show();
        mGoogleApiClient.connect();
    }

    public void setFakeLocation(View view) {
        stopLocationUpdates();
        mCurrentLocation.setLatitude(0);
        mCurrentLocation.setLongitude(0);
        updateUI();
    }

    public void setRealLocation(View view) {
        startLocationUpdates();
        updateUI();
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = java.text.DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }

    private void updateUI() {
        latText.setText(String.valueOf(mCurrentLocation.getLatitude()));
        longText.setText(String.valueOf(mCurrentLocation.getLongitude()));
        timeText.setText(mLastUpdateTime);
        currentGeofence.setText(currentGeofenceName==null ? "Не определена" : currentGeofenceName);
    }

    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void getLocation(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(Constants.REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(Constants.LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(Constants.LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(Constants.REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        Constants.REQUESTING_LOCATION_UPDATES_KEY);
            }

            if (savedInstanceState.keySet().contains(Constants.LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(Constants.LOCATION_KEY);
            }

            if (savedInstanceState.keySet().contains(Constants.LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        Constants.LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }

    private GeofencingRequest getGeofenceRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void openAddGeofenceDialog(View view) {
        DialogFragment dialog = new AddGeofenceDialog();
        dialog.show(getFragmentManager(), "AddGeofenceDialog");
    }

    private void addGeofence(String geoFenceName) {
        mGeofenceList.add(new Geofence.Builder()
                .setRequestId(geoFenceName)
                .setCircularRegion(mCurrentLocation.getLongitude(),
                        mCurrentLocation.getLongitude(),
                        Constants.GEOFENCE_RADIUS_IN_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(5000)
                .build());

        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofenceRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        }catch (SecurityException securityException) {
            logSecurityException(securityException);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        EditText geoFenceNameField = (EditText) dialog.getDialog().findViewById(R.id.geoFenceName);
        String geoFenceName = geoFenceNameField.getText().toString();
        addGeofence(geoFenceName);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

        dialog.getDialog().cancel();
    }

    public void removeGeofenceButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        try{
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        }catch (SecurityException securityException) {
            logSecurityException(securityException);
        }
    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            mGeofenceAdded = !mGeofenceAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofenceAdded);
            editor.apply();

            setButtonsEnabledState();

            Toast.makeText(MainActivity.this,
                    "Точка успешно " + (mGeofenceAdded ? "добавлена" : "удалена"),
                    Toast.LENGTH_SHORT).show();
        }else{
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

    private void setButtonsEnabledState() {
        if (mGeofenceAdded) {
            maddGeofence.setEnabled(false);
            mremoveGeofence.setEnabled(true);
        } else {
            maddGeofence.setEnabled(true);
            mremoveGeofence.setEnabled(false);
        }
    }

}
