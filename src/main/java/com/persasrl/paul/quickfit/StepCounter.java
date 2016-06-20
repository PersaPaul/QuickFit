package com.persasrl.paul.quickfit;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.SupportErrorDialogFragment;

import java.text.DateFormat;
import java.util.Date;

public class StepCounter extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    //keys



    GoogleApiClient mGoogleApiClient;
    TextView mDistanceText;
    TextView mTimeText;
    Button pause;
    Location mLastLocation;
    Location mCurrentLocation;
    UserLocalStore userLocalStore;
    float[] d = new float[1];
    float totald=0;
    int type;
    boolean paused;
    long time;


    private MCountDownTimer countDownTimer;
    private long timeElapsed;

    private final long startTime = 9223372036854775807L;
    private final long interval = 1;

    LocationRequest mLocationRequest;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_counter);
        userLocalStore = new UserLocalStore(this);
        mDistanceText = (TextView) findViewById(R.id.dist);
        mTimeText = (TextView) findViewById(R.id.time);
        pause = (Button) findViewById(R.id.ps);
        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
        Intent startIntent = getIntent();
        Bundle bundle = startIntent.getExtras();
        if(bundle!=null)
        type= bundle.getInt("sport");
        isGpsConnected();
        createLocationRequest();
        buildGoogleApiClient();

    }

    public void isGpsConnected()
    {
        // Get Location Manager and check for GPS & Network location services
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Build the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location Services Not Active");
            builder.setMessage("Please enable Location Services and GPS");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!mResolvingError)
            mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void stopApp(View view)
    {
        User user = userLocalStore.getLoggedInUser();
        Workout workout = new Workout (totald,type,user.username,timeElapsed);
        registerWorkout(workout);
    }

    private void registerWorkout(Workout workout) {
        ServerRequests serverRequest = new ServerRequests(this);
        serverRequest.storeWorkoutDataInBackground(workout, new GetWorkoutCallback() {
            @Override
            public void done(Workout returnedWorkout) {
                Intent Intent = new Intent(StepCounter.this, MainActivity.class);
                startActivity(Intent);
            }
        });
    }

    public void onConnected(Bundle connectionHint) {
        countDownTimer = new MCountDownTimer(startTime, interval);
        countDownTimer.start();
        startLocationUpdates();
    }

    //pana aici merge de aici vine partea cu update

    protected void startLocationUpdates() {

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(500);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onLocationChanged(Location location) {
        if(mCurrentLocation!=null)
            mLastLocation = mCurrentLocation;
        mCurrentLocation = location;
        if(mLastLocation!=null && mCurrentLocation!=null)
            updateDistance();
    }



    public void updateDistance()
    {
        Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), d);
        if(!(d[0]<0.001))
        totald+=d[0];
        mDistanceText.setText(String.valueOf(String.format("%.3f", totald / 1000) + "km"));
    }

    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }
    public void pauseApp(View view)
    {
        if(!paused) {
            paused = true;
            pause.setText("RESUME");
            countDownTimer.cancel();
            onPause();
        }

        else {
            paused = false;
            pause.setText("PAUSE");
            countDownTimer = new MCountDownTimer(startTime,interval);
            countDownTimer.start();
            mLastLocation=null;
            onResume();
        }
    }



    // De aici partea cu rezolvatu problemei


    @Override
    public void onConnectionSuspended(int i) {
        //todo nust...
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }


    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((StepCounter) getActivity()).onDialogDismissed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
    }
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    //timer
    public class MCountDownTimer extends CountDownTimer
    {

        public MCountDownTimer(long startTime, long interval)
        {
            super(startTime, interval);
        }

        @Override
        public void onFinish()
        {

        }

        @Override
        public void onTick(long millisUntilFinished)
        {

                long hours = 0;
                long minutes = 0;
                long seconds = 0;
                timeElapsed = (startTime - millisUntilFinished) / 1000;
                seconds = timeElapsed % 60;
                minutes = timeElapsed / 60 % 60;
                hours = timeElapsed / 3600;
                mTimeText.setText(String.valueOf(hours) + ":" + String.valueOf(minutes) + ":" + String.valueOf(seconds));
                time=millisUntilFinished;
        }


    }


}


