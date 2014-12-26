package net.taptools.android.trailtracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.model.LatLng;

import net.taptools.android.trailtracker.Results.ResultsFragment;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener{

    private static final String TAG_TRACING_FRAGMENT = "tracing";
    private static final String TAG_RESULTS_FRAGMENT = "results";

    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
     //TODO get Google Play Services checking code from sample code
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private Fragment activeFragment;
    private int lastPosition;

    TrailTrackingService.TTBinder binder;

    private boolean isBoundToLocationService;

    private SQLiteDatabase writableDatabase;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("ServiceConnection onServiceConnected()", "called");
            binder = (TrailTrackingService.TTBinder)service;
            binder.setLocationListener(MainActivity.this);
            if(activeFragment instanceof TrackTrailFragment){
                ((TrackTrailFragment)activeFragment).onLocationServiceBound();
            }
            if(activeFragment instanceof TraceTrailFragment){
                ((TraceTrailFragment)activeFragment).onLocationServiceBound();
            }
            isBoundToLocationService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            System.out.println("service disconnected");
        }
    };

    public void bindToLocationService(){
        Log.d("MainActivity bindToLocationService()", "called");
        bindService(new Intent(this, TrailTrackingService.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbindFromLocationService(){
        Log.d("MainActivity#unbindFromLocationService()", "called");
        if(isBoundToLocationService)
            unbindService(serviceConnection);
        isBoundToLocationService = false;
        binder = null;
    }

    @Override
    public void onBackPressed() {
        if ((activeFragment != null) && (activeFragment instanceof TraceTrailFragment)) {
            if (((TraceTrailFragment) activeFragment).isTracing()) {
                AlertDialog dialog = new AlertDialog.Builder(this).create();
                dialog.setTitle("Closing");
                dialog.setMessage("Are you sure you want to cancel tracing?");
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Stop Tracing",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((TraceTrailFragment) activeFragment).onBackPressed();
                            }
                        }
                );
                dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Continue Tracing",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //do nothing; dialog is dismissed automatically//
                            }
                        }
                );
                dialog.show();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lastPosition = -1;

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        isGooglePlayServicesAvailable();

        if (savedInstanceState == null) {
            activeFragment = TrackTrailFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, activeFragment)
                    .commit();
        }
    }

    public boolean isGooglePlayServicesAvailable(){
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode == ConnectionResult.SUCCESS) {
            return true;
        } else {
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode,this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);
            if (errorDialog!= null) {
                ErrorDialogFragment errorFrag = new ErrorDialogFragment();
                errorFrag.setDialog(errorDialog);
                errorFrag.show(getFragmentManager(),"Location Services");
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("mainActivity onActivityResult()","called");
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST :
            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
                switch (resultCode) {
                    case Activity.RESULT_OK :
                    if(!isGooglePlayServicesAvailable()){
                        //TODO tell user to fix google play services issue.
                    }
                        break;
                }
             break;
            case TrackTrailFragment.GPS_REQUEST_CODE :
                activeFragment.onActivityResult(requestCode,resultCode,data);
                break;
            case TrackTrailFragment.WAYPOINT_REQUEST_CODE :
                Log.d("mainActivity onActivityResult()","passing to fragment");
                activeFragment.onActivityResult(requestCode,resultCode,data);
                break;
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (position == lastPosition) {
            return;
        }
        lastPosition = position;

        FragmentManager fragmentManager = getFragmentManager();
        if (position == 0) {
            activeFragment = TrackTrailFragment.newInstance();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, activeFragment)
                    .commit();
        } else if (position == 1) {
            activeFragment = fragmentManager.findFragmentByTag(TAG_TRACING_FRAGMENT);
            if (activeFragment == null) {
                activeFragment = TraceTrailFragment.newInstance();
            }
            fragmentManager.beginTransaction()
                    .replace(R.id.container, activeFragment, TAG_TRACING_FRAGMENT)
                    .addToBackStack(TAG_TRACING_FRAGMENT)
                    .commit();
        } else if (position == 3) {
            activeFragment = new ResultsFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, activeFragment, TAG_RESULTS_FRAGMENT)
                    .commit();
        }
        else {
            activeFragment = PlaceholderFragment.newInstance(position + 1);
            fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, activeFragment)
                    .commit();
        }
        onSectionAttached(position + 1);
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
            case 4:
                mTitle = getString(R.string.title_section5);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
         /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            //TODO showErrorDialog(connectionResult.getErrorCode());
        }
    }

    public void onLocationReceived(Location loc, ArrayList<LatLng> trail, long lastLocId){
        if (activeFragment instanceof TrackTrailFragment) {
            ((TrackTrailFragment) activeFragment).onLocationChanged(loc, trail, lastLocId);
        }
        if (activeFragment instanceof TraceTrailFragment) {
            ((TraceTrailFragment) activeFragment).onLocationChanged(loc);
        }
    }

    public void onStop(Location loc){
        if (activeFragment instanceof TrackTrailFragment) {
            ((TrackTrailFragment) activeFragment).onStop(loc);
        }
    }
    public void onResumeMoving(long stopId){
        if (activeFragment instanceof TrackTrailFragment) {
            ((TrackTrailFragment) activeFragment).onResumeMoving(stopId);
        }
    }

    public boolean isBoundToLocationService() {
        return isBoundToLocationService;
    }
    /*
     * Handle results returned to the FragmentActivity
     * by Google Play services
     */

    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
