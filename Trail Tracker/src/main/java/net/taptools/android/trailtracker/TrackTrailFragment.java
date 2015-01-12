package net.taptools.android.trailtracker;


import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import net.taptools.android.trailtracker.models.Stop;
import net.taptools.android.trailtracker.models.TTLocation;
import net.taptools.android.trailtracker.models.Waypoint;
import net.taptools.android.trailtracker.dialogs.EnableLocationDialogFragment;
import net.taptools.android.trailtracker.dialogs.MapDetailsDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import static net.taptools.android.trailtracker.TTSQLiteOpenHelper.*;

/**
 * Created by Brian Murphy on 5/4/2014.
 */
public class TrackTrailFragment extends Fragment implements
        MapDetailsDialogFragment.MapDetailsChangeListener,

        EnableLocationDialogFragment.OnGPSCancelListener {

    public static final int GPS_REQUEST_CODE = 16512;
    static final int WAYPOINT_REQUEST_CODE = 36585;

    private MapFragment mapFragment;
    private DashboardFragment dashboardFragment;

    private long lastTime;
    private float totalDistance;
    private Polyline polyline;
    private long lastLocationId = -1;
    private ArrayList<Waypoint> waypoints;
    private ArrayList<Marker> waypointMarkers;
    private ArrayList<Marker> stopMarkers;

    private TTSQLiteOpenHelper sqLiteHelper;
    private SQLiteDatabase writableDatabase;

    private ProgressDialog findingLocProgressDialog;

    private Menu actionMenu;

    public static TrackTrailFragment newInstance() {
        return new TrackTrailFragment();
    }

    public TrackTrailFragment(){
        //default empty constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(getString(R.string.key_show_stops))) {
                    drawStops();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("TrackTrailFrag onCreateView()", "called");
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fragment_track_trail,
                container, false);
        mapFragment = MapFragment.newInstance();
        getActivity().getFragmentManager().beginTransaction()
            .add(R.id.mapFragmentWindow, mapFragment)
            .commit();
        dashboardFragment = new DashboardFragment();

        if (PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getBoolean(getString(R.string.key_show_stats_tracking), true)) {
            getActivity().getFragmentManager().beginTransaction()
                    .add(R.id.dashboardFragmentWindow, dashboardFragment)
                    .commit();
        }

        if (TrailTrackingService.getStarted()) {
            MainActivity activity = (MainActivity) getActivity();
            if (!activity.isBoundToLocationService()) {
                Log.d("TrackTrailFrag onCreateView()","binding to tracking Service");
                activity.bindToLocationService();
            }

            if (sqLiteHelper == null) {
                sqLiteHelper = ((MyApplication) getActivity().getApplication()).getDatabaseHelper();
            } else {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                if (prefs.getBoolean(getString(R.string.key_show_stops), true)) {
                    drawStops();
                }

                drawWaypoints();
            }
        }
        return layout;
    }

    private void drawWaypoints() {
        waypoints = new ArrayList<Waypoint>(Arrays.asList(
                Waypoint.getAll(sqLiteHelper, ((MainActivity) getActivity()).binder.getMapId())
        ));
        GoogleMap map = mapFragment.getMap();
        if (waypointMarkers == null) {
            waypointMarkers = new ArrayList<Marker>();
        }
        for (Marker marker : waypointMarkers) {
            marker.remove();
        }
        for (Waypoint wp : waypoints) {
            Log.d("TrackTrailFrag onCreateView()", "wp added");
            waypointMarkers.add(map.addMarker(wp.getMarker()));
        }
    }

    private void drawStops() {
        Stop[] stops = Stop.getAll(sqLiteHelper,
                ((MainActivity) getActivity()).binder.getMapId());
        GoogleMap map = mapFragment.getMap();
        if (stopMarkers == null) {
            stopMarkers = new ArrayList<Marker>();
        }
        for (Marker marker : stopMarkers) {
            marker.remove();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (prefs.getBoolean(getString(R.string.key_show_stops), true)) {
            for (Stop stp : stops) {
                stopMarkers.add(map.addMarker(stp.getMarker()));
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.track_trail, menu);
        actionMenu = menu;
        if (TrailTrackingService.getStarted()) {
            setActionBarTracking();
        } else {
            setActionBarNotTracking();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void setActionBarTracking() {
        Log.d("TrackTrailFrag setActionBarTracking()", "called");
        actionMenu.findItem(R.id.action_start_tracking).setVisible(false);
        actionMenu.findItem(R.id.action_stop_tracking).setVisible(true);
        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            actionMenu.findItem(R.id.action_add_landmark).setVisible(true);
        }
    }

    private void setActionBarNotTracking() {
        Log.d("TrackTrailFrag setActionBarNotTracking()", "called");
        actionMenu.findItem(R.id.action_start_tracking).setVisible(true);
        actionMenu.findItem(R.id.action_stop_tracking).setVisible(false);
        actionMenu.findItem(R.id.action_add_landmark).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("TrackTrailFragment onOptionsItemSelected()", "called");
        final MainActivity mainActivity = (MainActivity) getActivity();
        switch (item.getItemId()) {
            case R.id.action_start_tracking :
                waypoints = new ArrayList<Waypoint>();
                waypointMarkers = new ArrayList<Marker>();
                stopMarkers = new ArrayList<Marker>();
                setActionBarTracking();
                //Bind location service//
                Log.d("TrackTrailFragment onOptionsItemSelected()", "binding to Tracking service");
                mainActivity.bindToLocationService();

                Log.d("TrackTrailFragment onOptionsItemSelected()", "binding to dbService");
                if (sqLiteHelper == null) {
                    sqLiteHelper = ((MyApplication) getActivity().getApplication()).getDatabaseHelper();
                }
                break;
            case R.id.action_stop_tracking :
                mainActivity.binder.pauseTracking();
                dashboardFragment.pauseTimer();
                MapDetailsDialogFragment detailsDialogFragment = MapDetailsDialogFragment.instanceOf(
                        MapDetailsDialogFragment.MODE_FINISH_TRACKING, mainActivity.binder.getMapId(),
                        this);
                detailsDialogFragment.show(getFragmentManager(), "editingMapDetails");
                //cleanup done on callbacks
                break;
            case R.id.action_add_landmark :
                Intent wpIntent = new Intent(mainActivity, WaypointActivity.class);
                wpIntent.putExtra(WaypointActivity.KEY_MAP_ID, mainActivity.binder.getMapId());
                wpIntent.putExtra(WaypointActivity.KEY_LOCATION_ID, lastLocationId);
                Log.d("onOptionsItemSelected()", "starting wp activity for result");
                mainActivity.startActivityForResult(wpIntent, WAYPOINT_REQUEST_CODE);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onLocationServiceBound() {
        if (((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE))
                .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (!TrailTrackingService.getStarted()) {
                Log.d("startTrackingServiceConnection onServiceConnected", "map Initializing");
                initializeMap();
            }
        } else {
            EnableLocationDialogFragment.newInstance(this).show(getFragmentManager(), "gpsEnableDialog");
        }
        drawWaypoints();
        drawStops();
    }

    private void initializeMap() {
        Log.d("TrackTrailFrag initializeMap()", "called");

        final MainActivity mainActivity = (MainActivity) getActivity();
        findingLocProgressDialog = new ProgressDialog(mainActivity);
        findingLocProgressDialog.setTitle("Finding Location");
        findingLocProgressDialog.setMessage("If this takes more than ten seconds, you should make sure " +
                "there is no interference (buildings or other large structures).");
        findingLocProgressDialog.setCanceledOnTouchOutside(false);
        findingLocProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (lastLocationId < 0) {
                    setActionBarNotTracking();
                    mainActivity.binder.cancelTracking();
                    mainActivity.unbindFromLocationService();
                    Toast.makeText(getActivity(), "Tracking Canceled", Toast.LENGTH_SHORT).show();
                }
            }
        });
        findingLocProgressDialog.show();

        writableDatabase = sqLiteHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, "temp");
        values.put(COLUMN_START_TIME, Calendar.getInstance().getTimeInMillis());
        values.put(COLUMN_END_TIME, 0);
        values.put(COLUMN_AVERAGE_SPEED, 0);
        values.put(COLUMN_TOTAL_DISTANCE, -1);
        values.put(COLUMN_MAXIMUM_SPEED, 0);
        values.put(COLUMN_LINEAR_DISTANCE, 0);
        values.put(COLUMN_NOTES, "...");
        values.put(COLUMN_MAX_ALTITUDE, 0);
        values.put(COLUMN_MIN_ALTITUDE, 0);
        values.put(COLUMN_START_ALTITUDE, 0);
        values.put(COLUMN_END_ALTITUDE, 0);
        long mapId = writableDatabase.insert(TABLE_MAPS, null, values);
        Intent trackingIntent = new Intent(getActivity(), TrailTrackingService.class);
        trackingIntent.putExtra(TrailTrackingService.KEY_MAP_ID, mapId);
        Log.d("TrackTrailFrag initializeMap()", "STARTING LOCATION SERVICE");
        getActivity().startService(trackingIntent);
    }

    @Override
    public void onGPSCancel() {
        Log.d("TrackTrailFrag gpsCancelled()", "called");
        MainActivity mainActivity = (MainActivity) getActivity();
        Toast.makeText(mainActivity, "Please enable GPS for tracking", Toast.LENGTH_SHORT).show();
        setActionBarNotTracking();
        Log.d("TrackTrailFrag gpsCancelled()", "unbind location service");
        mainActivity.unbindFromLocationService();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("TrackTrailFrag onActivityResult()", "called");
        switch (requestCode) {
            case GPS_REQUEST_CODE :
                if (((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE))
                        .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    initializeMap();
                } else {
                    onGPSCancel();
                }
                break;
            case WAYPOINT_REQUEST_CODE :
                if (resultCode == Activity.RESULT_OK) {
                    Log.d("onActivityResult()", "adding wp");
                    if (waypointMarkers == null) {
                        waypointMarkers = new ArrayList<Marker>();
                    }
                    Waypoint wp = Waypoint.instanceOf(sqLiteHelper,
                            data.getLongExtra(WaypointActivity.KEY_WP_ID, -1));
                    waypointMarkers.add(mapFragment.getMap().addMarker(wp.getMarker()));
                    waypoints.add(wp);
                }
        }
    }

    @Override
    public void onDestroyView() {
        Log.d("TrackTrailFrag onDestroyView()", "called");
        super.onDestroyView();
        MainActivity activity = (MainActivity) getActivity();
        if (activity.isBoundToLocationService()) {
            Log.d("TrackTrailFrag onDestroyView()", "unbindLocationService");
            activity.unbindFromLocationService();
        }
        try {
            getFragmentManager().beginTransaction()
                    .remove(mapFragment)
                    .commitAllowingStateLoss();
            getFragmentManager().beginTransaction()
                    .remove(dashboardFragment)
                    .commitAllowingStateLoss();
        }
        catch (IllegalStateException e) {
            Log.e("destroying Fragment", "activity already destroyed");
        }
    }

    public void onLocationChanged(Location location, ArrayList<LatLng> coordinatesList,
                                  long lastLocID) {
        lastLocationId = lastLocID;
        if (findingLocProgressDialog.isShowing()) {
            findingLocProgressDialog.dismiss();
            Toast.makeText(getActivity(), "Location Found!", Toast.LENGTH_SHORT).show();
        }
        if (coordinatesList == null) {
            return;
        }
        float[] distanceFromLast = {0f};
        if (coordinatesList.size() != 0) {
            LatLng last = coordinatesList.get(coordinatesList.size() - 1);
            Location.distanceBetween(last.latitude, last.longitude,
                    location.getLatitude(), location.getLongitude(), distanceFromLast);
        }
        totalDistance += distanceFromLast[0];
        dashboardFragment.setStats(totalDistance, location.getSpeed(),
                location.getAltitude());
        lastTime = location.getTime();
        if (polyline == null) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(coordinatesList);
            polyline = mapFragment.getMap().addPolyline(polylineOptions);
        } else {
            polyline.setPoints(coordinatesList);
        }
        mapFragment.getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(
                coordinatesList.get(coordinatesList.size() - 1), 30f));//TODO attribute to settings
    }

    public void onStop(Location loc) {
        MarkerOptions stopOptions = new MarkerOptions();
        stopOptions.title("Stop")
                .position(new LatLng(loc.getLatitude(), loc.getLongitude()));
        stopMarkers.add(mapFragment.getMap().addMarker(stopOptions));
    }

    public void onResumeMoving(long stopId) {
        Marker lastMarker = stopMarkers.get(stopMarkers.size() - 1);
        Stop stp = Stop.instanceOf(sqLiteHelper, stopId);
        int startTime = (int) stp.getStartLocation().getTime();
        int endTime = (int) stp.getEndLocation().getTime();
        int span = endTime - startTime;
        StringBuilder snippet = new StringBuilder("Stopped for ");
        byte mins = (byte) (span / 60);
        snippet.append(mins == 0 ? "00" : mins);
        snippet.append(":");
        String secs = "" + (span % 60);
        snippet.append(secs.length() == 1 ? "0" + secs : secs);
        lastMarker.setSnippet(snippet.toString());
    }

    @Override
    public void onCancel() {
        ((MainActivity) getActivity()).binder.resumeTracking();
        dashboardFragment.resumeTimer();
    }

    @Override
    public void onQuitWithoutSaving() {
        MainActivity mainActivity = (MainActivity) getActivity();
        lastLocationId = -1;
        mainActivity.binder.stopTracking();
        mainActivity.unbindFromLocationService();
        setActionBarNotTracking();
        mainActivity.refreshFragment();
    }

    @Override
    public void onSaveNewDetails() {
        MainActivity mainActivity = (MainActivity) getActivity();
        lastLocationId = -1;
        mainActivity.binder.stopTracking();
        mainActivity.unbindFromLocationService();
        setActionBarNotTracking();
        mainActivity.refreshFragment();
    }
}
