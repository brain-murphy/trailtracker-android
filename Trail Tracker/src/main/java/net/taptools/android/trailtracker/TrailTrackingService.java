package net.taptools.android.trailtracker;

import android.app.Dialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import net.taptools.android.trailtracker.models.TTLocation;

import java.util.ArrayList;
import java.util.Calendar;

import static net.taptools.android.trailtracker.TTSQLiteOpenHelper.*;

public class TrailTrackingService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks, LocationListener {

    public static final String KEY_MAP_ID = "mapidkey";

    private MainActivity listener;

    private TTBinder binder = new TTBinder();

    private ArrayList<LatLng> trail;
    private ArrayList<Marker> stops;
    private LatLng previousStopLoc;

    private static boolean isStarted;
    public static boolean getStarted(){
        return isStarted;
    }
    private boolean isServingLocations;
    private LocationIntermediary locationIntermediary;

    private LocationClient client;
    private LocationRequest request;

    private TTSQLiteOpenHelper databaseHelper;
    private SQLiteDatabase writableDatabase;
    private SQLiteDatabase readableDatabase;
    private long mapId;
    private float totalDistance;

    private long startTime;
    private long lastTime;
    private float maxSpeed;
    private double maxAltitude;
    private double minAltitude;
    private double startAltitude;
    private double endAltitude;
    private double firstLat;
    private double firstLong;
    long lastLocId = -1;


    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
    }

    private void startServingLocations() {
        if (!isStarted)
            locationIntermediary = simpleLocIntermediary;
        isServingLocations = true;
        request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(1000);
        request.setFastestInterval(1000);
        client = new LocationClient(listener, this, listener);
        client.connect();
    }

    private void stopServingLocations() {
        Log.d("TrailTrackingService#stopServingLocations()", "called");
        if (client == null) {
            Log.d("TrailTrackingService onDestroy()", "client null");
        } else {
            if (client.isConnected()) {
                client.removeLocationUpdates(this);
            }
            client.disconnect();
        }
        isServingLocations = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mapId = intent.getLongExtra(KEY_MAP_ID, -1);
        totalDistance = 0;
        maxSpeed = 0;
        maxAltitude = 0;
        minAltitude = 0;
        lastTime = 0;
        endAltitude = 0;
        trail = new ArrayList<LatLng>();
        stops = new ArrayList<Marker>();
        if(!isServingLocations)
            startServingLocations();
        databaseHelper = ((MyApplication) getApplication()).getDatabaseHelper();
        writableDatabase = databaseHelper.getWritableDatabase();
        Log.d("databaseNull", "got writableDatabase:" + (writableDatabase != null));
        locationIntermediary = new StarterIntermediary();
        isStarted = true;
        return flags;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onConnected(Bundle bundle) {
        client.requestLocationUpdates(request, this);
    }

    @Override
    public void onDisconnected() {
        listener = null;
    }

    @Override
    public void onDestroy() {
        Log.d("TrailTrackingService onDestroy()", "called");
        if (client == null){
            Log.d("TrailTrackingService onDestroy()", "client null; exiting");
        } else {
            if (client.isConnected()) {
                client.removeLocationUpdates(this);
            }
            client.disconnect();
        }
        super.onDestroy();
    }


    private interface LocationIntermediary {
        public void onLocationChanged(Location location);
    }

    private class StarterIntermediary implements LocationIntermediary {
        byte numPoints = 0;
        private Location lastLoc;

        public StarterIntermediary() {
        }

        @Override
        public void onLocationChanged(Location loc) {
                if (lastLoc != null && (loc.getAccuracy() < 5 || numPoints > 5)){
                    startTime = Calendar.getInstance().getTimeInMillis();
                    lastTime = startTime;
                    firstLat = loc.getLatitude();
                    firstLong = loc.getLongitude();
                    startAltitude = loc.getAltitude();
                    trail.add(new LatLng(loc.getLatitude(),loc.getLongitude()));
                    addLocToDB(loc);
                    locationIntermediary = new TrackingIntermediary();
                }
            lastLoc=loc;
            numPoints++;
        }
    };

    private class TrackingIntermediary implements LocationIntermediary {
        private int numStationaryPoints = 0;
        private long firstStoppedLocID;
        private long previousLocID;
        private boolean isStopped = false;
        private long stopId;

        public TrackingIntermediary(){
            Log.d("TrackingIntermediary constructor", "init");
        }
        @Override
        public void onLocationChanged(Location location) {
            trail.add(new LatLng(location.getLatitude(), location.getLongitude()));
            if (isStarted) {
                addLocToDB(location);
            }
            endAltitude = location.getAltitude();
            lastTime = Calendar.getInstance().getTimeInMillis();

            if (location.getSpeed() > maxSpeed) {
                maxSpeed = location.getSpeed();
            }
            if (location.getAltitude() > maxAltitude) {
                maxAltitude = location.getAltitude();
            }
            if (location.getAltitude() < minAltitude) {
                minAltitude = location.getAltitude();
            }

            float[] distanceFromLast = {0f};
            LatLng last = trail.get(trail.size() - 2);
            Location.distanceBetween(last.latitude, last.longitude,
                    location.getLatitude(), location.getLongitude(), distanceFromLast);
            totalDistance += distanceFromLast[0];

            if (listener != null) {
                listener.onLocationReceived(location, trail, lastLocId);
            }

            LatLng firstStopPt = trail.get(trail.size() - 2 - numStationaryPoints);
            LatLng mostRecentPt = trail.get(trail.size() - 1);
            float[] results = new float[1];
            Location.distanceBetween(firstStopPt.latitude, firstStopPt.longitude, mostRecentPt.latitude,
                    mostRecentPt.longitude, results);

            if (results[0] < location.getAccuracy()) {
                if (numStationaryPoints == 0) {
                    firstStoppedLocID = lastLocId;
                }
                numStationaryPoints++;
                previousLocID  = lastLocId;
            } else if (isStopped) {
                ContentValues newVals = new ContentValues();
                newVals.put(COLUMN_END_LOCATION_ID, previousLocID);
                writableDatabase.update(TABLE_STOPS, newVals, COLUMN_ID + " = " + stopId, null);
                isStopped = false;
                numStationaryPoints = 0;
                listener.onResumeMoving(stopId);
            }

            if (numStationaryPoints > 3 && !isStopped) {
                isStopped = true;
                ContentValues stopValues = new ContentValues();
                stopValues.put(COLUMN_START_LOCATION_ID, firstStoppedLocID);
                stopValues.put(COLUMN_END_LOCATION_ID, lastLocId);
                stopValues.put(COLUMN_MAP_ID, mapId);
                stopId = writableDatabase.insert(TABLE_STOPS, null, stopValues);
                listener.onStop(location);
            }
        }
    };

    LocationIntermediary simpleLocIntermediary = new LocationIntermediary() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d("simpleLocIntermediary onLocationChanged()", "called");
            listener.onLocationReceived(location, null, -1);
        }
    };

    @Override
    public void onLocationChanged(Location location) {
        locationIntermediary.onLocationChanged(location);
    }

    private void addLocToDB(Location location) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LONGITUDE, location.getLongitude());
        values.put(COLUMN_LATITUDE, location.getLatitude());
        values.put(COLUMN_MAP_ID, mapId);
        values.put(COLUMN_SPEED, location.getSpeed());
        values.put(COLUMN_ELEVATION, location.getAltitude());
        values.put(COLUMN_ACCURACY, location.getAccuracy());
        values.put(COLUMN_BEARING, location.getBearing());
        values.put(COLUMN_DISTANCE, totalDistance);
        values.put(COLUMN_TIME, Calendar.getInstance().getTimeInMillis());
        lastLocId = writableDatabase.insert(TABLE_LOCATIONS, null, values);
    }

    public class TTBinder extends Binder {
        public void setLocationListener(MainActivity activity){
            TrailTrackingService.this.listener = activity;
        }

        public void startServingLocations(MainActivity activity){
            if(activity.isGooglePlayServicesAvailable()) {
                TrailTrackingService.this.startServingLocations();
            }
        }

        public void stopServingLocations(){
            TrailTrackingService.this.stopServingLocations();
        }

        public boolean getTracking(){
            return TrailTrackingService.this.getStarted();
        }
        public long getMapId(){ return TrailTrackingService.this.mapId;}

        public void stopTracking(){
            isStarted = false;
            readableDatabase = databaseHelper.getReadableDatabase();
            Cursor crsr = readableDatabase.query(TABLE_LOCATIONS, ALL_LOCATION_COLUMNS, COLUMN_MAP_ID + " = " + mapId,
                    null, null, null, COLUMN_ID + " DESC");
            crsr.moveToFirst();
            ContentValues values = new ContentValues();
            values.put(COLUMN_AVERAGE_SPEED, totalDistance/(startTime-lastTime));//TODO units
            values.put(COLUMN_END_TIME,lastTime);
            values.put(COLUMN_TOTAL_DISTANCE,totalDistance);
            values.put(COLUMN_MAXIMUM_SPEED,maxSpeed);
            values.put(COLUMN_MAX_ALTITUDE, maxAltitude);
            values.put(COLUMN_MIN_ALTITUDE, minAltitude);
            values.put(COLUMN_START_ALTITUDE, startAltitude);
            values.put(COLUMN_END_ALTITUDE, endAltitude);
            float[] results = {0f};
            Location.distanceBetween(firstLat, firstLong,
                    crsr.getDouble(crsr.getColumnIndex(COLUMN_LATITUDE)),
                    crsr.getDouble(crsr.getColumnIndex((COLUMN_LONGITUDE))), results);
            values.put(COLUMN_LINEAR_DISTANCE, results[0]);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(startTime);
            String nameString = cal.get(Calendar.DAY_OF_MONTH) + "/"
                                + cal.get(Calendar.MONTH) + " "
                                + cal.get(Calendar.HOUR_OF_DAY) + ":"
                                + cal.get(Calendar.MINUTE);
            values.put(COLUMN_NAME, nameString);
            writableDatabase.update(TABLE_MAPS, values, COLUMN_ID + " = " + mapId, null);
            writableDatabase.close();
            writableDatabase = null;
            readableDatabase.close();
            readableDatabase = null;
            stopSelf();
        }

        public void cancelTracking(){
            isStarted = false;
            writableDatabase.delete(TABLE_MAPS, COLUMN_ID + " = " + mapId, null);
            writableDatabase.delete(TABLE_LOCATIONS, COLUMN_ID + " = " + mapId, null);
            writableDatabase.delete(TABLE_STOPS, COLUMN_ID + " = " + mapId, null);
            writableDatabase.delete(TABLE_WAYPOINTS, COLUMN_ID + " = " + mapId, null);
            writableDatabase.close();
            writableDatabase = null;
            stopSelf();
        }
    }
}
