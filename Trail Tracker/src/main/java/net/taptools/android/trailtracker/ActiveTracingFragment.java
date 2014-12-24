package net.taptools.android.trailtracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ActiveTracingFragment extends Fragment implements SensorEventListener {

    private Map mapData;
    private ImageView compassImageView;
    private MapFragment mapFragment;
    SensorManager manager;
    private Sensor magnetometer;
    private Sensor accelerometer;

    private float[] geomagnetic;
    private float[] gravity;

    private ArrayList<TTLocation> pointsToHit;
    private TTLocation ptToHit;


    public static ActiveTracingFragment newInstance(Map data) {
        ActiveTracingFragment fragment = new ActiveTracingFragment();
        fragment.mapData =data;
        return fragment;
    }

    public ActiveTracingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d("ActiveTracingFragment#onCreateView()","called");
        View root = inflater.inflate(R.layout.fragment_trace_trail,container,false);
        compassImageView = (ImageView)root.findViewById(R.id.compassNeedleImage);
        mapFragment = new MapFragment() {
            @Override
            public void onActivityCreated(Bundle savedInstanceState) {
                super.onActivityCreated(savedInstanceState);
                if ((mapFragment.getMap()) != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onMapReady();
                        }
                    });
                }
            }
        };
        getFragmentManager().beginTransaction()
                .replace(R.id.mapFragmentWindow_tracing,mapFragment)
                .commit();
        ((MainActivity)getActivity()).bindToLocationService();

        return root;
    }

    private void onMapReady(){
        GoogleMap map = mapFragment.getMap();
        map.clear();

        map.addPolyline(Map.toNewPolyline(mapData.getCheckpoints()));
        for(int waypointIndex = 0; waypointIndex<mapData.getWaypoints().length;waypointIndex++){
            map.addMarker(mapData.getWaypoints()[waypointIndex].getMarker());
        }
        for(int stopIndex = 0; stopIndex<mapData.getStops().length;stopIndex++){
            map.addMarker(mapData.getStops()[stopIndex].getMarker());
        }

        map.setMyLocationEnabled(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ActiveTracingFragment#onCreate()","called");
        manager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
        magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        pointsToHit = new ArrayList<TTLocation>(mapData.getLocations().length);
        for(TTLocation loc: mapData.getLocations()){
            pointsToHit.add(loc);
        }
        ptToHit = pointsToHit.get(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        manager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        manager.unregisterListener(this);
        manager.unregisterListener(this);
    }

    public void onLocationChanged(Location loc){
        if(gravity==null||geomagnetic==null){
            return;
        }
        float[] r = new float[9];
        float[] i = new float[9];
        SensorManager.getRotationMatrix(r,i,gravity,geomagnetic);

        float[] values = new float[3];
        values = SensorManager.getOrientation(r,values);

        GeomagneticField geomagneticField = new GeomagneticField(
                Double.valueOf(loc.getLatitude()).floatValue(),
                Double.valueOf(loc.getLongitude()).floatValue(),
                Double.valueOf(loc.getAltitude()).floatValue(),
                System.currentTimeMillis());

        float bearingNorth = (values[0]*57.2957795f +geomagneticField.getDeclination()+ 360) %360;
        float bearingToPt = 0f;
            float distanceToCurrent = ptToHit.distanceTo(loc.getLongitude(), loc.getLatitude());
            if (distanceToCurrent > loc.getAccuracy()) {
                bearingToPt = ptToHit.bearingHere(loc.getLongitude(), loc.getLatitude());
            } else {
                if(pointsToHit.size()==0){
                    TraceCompleteDialogFragment.newInstance(this).show(getFragmentManager(),"traceCompleteDialog");
                }
                for (int locIndex = 0; locIndex < pointsToHit.size(); locIndex++) {
                    TTLocation thisPt = pointsToHit.get(locIndex);
                    bearingToPt = thisPt.bearingHere(loc.getLongitude(), loc.getLatitude());
                    float distanceToPt = thisPt.distanceTo(loc.getLongitude(), loc.getLatitude());
                    if (distanceToPt > 45 || Math.abs(bearingToPt) > 30) {
                        ptToHit = thisPt;
                        break;
                    }
                    pointsToHit.remove(thisPt);
                }

            }

        float needleAngle = bearingToPt- bearingNorth;


        compassImageView.animate()
                .rotation(needleAngle)///TODO animate angle better
                .setDuration(900)
                .start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geomagnetic = event.values;
        else{
            gravity = event.values;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onTraceComplete() {
        Log.d("ActiveTracingFragment onGPSCancel()", "called");
        ((MainActivity)getActivity()).unbindFromLocationService();
        getFragmentManager().popBackStackImmediate();
    }

    public static class TraceCompleteDialogFragment extends DialogFragment {

        private ActiveTracingFragment listener;

        public static TraceCompleteDialogFragment newInstance(
                ActiveTracingFragment listener){
            TraceCompleteDialogFragment fragment = new TraceCompleteDialogFragment();
            fragment.listener = listener;
            return fragment;
        }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Trace Complete")
                    .setMessage("You've reached the end of the trail!")
                    .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onTraceComplete();
                        }
                    });
            return builder.create();
        }
    }
}
