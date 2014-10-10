package net.taptools.android.trailtracker;



import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.MapFragment;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;


/**
 * A simple {@link Fragment} subclass.
 *
 */
public class MapInfoFragment extends ResultsSubFragment {

    private static final String KEY_VALS = "valskey";
    private static final String KEY_TIMES = "teimeskey";

    private Map mapData;

    /**
     * creates and instatiates new {@link MapInfoFragment} as per
     * the Android design recommendations
     * @param maps {@link ArrayList} containing a single map to display.
     * @return A new Map InfoFragment that is ready for display.
     */
    public static MapInfoFragment newInstance(ArrayList<Map> maps){
        MapInfoFragment fragment = new MapInfoFragment();
        fragment.activeMaps = maps;
        fragment.mapData = maps.get(0);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mapData = activeMaps.get(0);
        }
    }

    public MapInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_map_info, container, false);
        root.findViewById(R.id.speedGraphButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**using two dimensional array because ChartFragment is set
                 * up to display multiple plots*/
                TTLocation[] locs = mapData.getLocations();
                long[][] timeArrays = new long[1][locs.length];
                float[][] speedArrays = new float[1][locs.length];
                for (int locIndex = 0; locIndex < locs.length; locIndex++) {
                    timeArrays[0][locIndex] = locs[locIndex].getTime();
                    speedArrays[0][locIndex] = locs[locIndex].getSpeed();
                }
                ((ResultsActivity)getActivity()).showChartFragment("Speed" , timeArrays,
                        speedArrays, activeMaps);
            }
        });

        root.findViewById(R.id.altitudeGraphButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TTLocation[] locs = mapData.getLocations();
                long[][] timeArrays = new long[1][locs.length];
                float[][] altArrays = new float[1][locs.length];
                for (int locIndex = 0; locIndex < locs.length; locIndex++) {
                    timeArrays[0][locIndex] = locs[locIndex].getTime();
                    altArrays[0][locIndex] = locs[locIndex].getElevation();
                }
                ((ResultsActivity)getActivity()).showChartFragment("Altitude", timeArrays,
                        altArrays, activeMaps);
            }
        });

        ((TextView)root.findViewById(R.id.mapNameTextView)).setText(mapData.getName());
        String notes = mapData.getNotes();
        if(notes==null ||notes.trim().equals("")){
            notes = "none";
        }
        ((TextView)root.findViewById(R.id.notesTextView)).setText(notes);
        Calendar mapCal = Calendar.getInstance();
        mapCal.setTimeInMillis(mapData.getStartTime());
        String dateString = String.format("%02d/%02d/% %02d:%02d%",mapCal.get(Calendar.MONTH),
                mapCal.get(Calendar.DATE),mapCal.get(Calendar.YEAR),mapCal.get(Calendar.HOUR),
                mapCal.get(Calendar.MINUTE),mapCal.get(Calendar.AM_PM));
        ((TextView)root.findViewById(R.id.whenRecordedTextView)).setText(dateString);
        ((TextView)root.findViewById(R.id.timeTextView_info)).setText(mapData.getFormattedTime());
        ((TextView)root.findViewById(R.id.totalDistanceTextView)).setText(
                Float.toString(mapData.getTotalDistance()));
        ((TextView)root.findViewById(R.id.linearDistanceTextView)).setText(
                Float.toString(mapData.getLinearDistance()));
        ((TextView)root.findViewById(R.id.avgSpeedTextView)).setText(
                Float.toString(mapData.getAverageSpeed()));
        ((TextView)root.findViewById(R.id.maxSpeedTextView)).setText(
                Float.toString(mapData.getMaximumSpeed()));
        ((TextView)root.findViewById(R.id.minAltitudeTextView)).setText(
                Float.toString(mapData.getMinimumAltitude()));
        ((TextView)root.findViewById(R.id.maxAltitudeTextView)).setText(
                Float.toString(mapData.getMaximumAltitude()));
        ((TextView)root.findViewById(R.id.startingAltitudeTextView)).setText(
                Float.toString(mapData.getStartAltitude()));
        ((TextView)root.findViewById(R.id.endingAltitudeTextView)).setText(
                Float.toString(mapData.getEndAltitude()));
        return root;
    }
}
