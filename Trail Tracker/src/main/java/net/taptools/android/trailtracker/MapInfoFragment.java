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

import java.util.Calendar;


/**
 * A simple {@link Fragment} subclass.
 *
 */
public class MapInfoFragment extends Fragment {

    private Map mapData;
    private static final String KEY_VALS = "valskey";
    private static final String KEY_TIMES = "teimeskey";


    public static MapInfoFragment newInstance(Map mapData){
        MapInfoFragment fragment = new MapInfoFragment();
        fragment.mapData = mapData;
        return fragment;
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
//                Intent intent = new Intent(getActivity(), ChartActivity.class);
//                TTLocation[] locations = mapData.getLocations();
//                long[] times = new long[locations.length];
//                float[] speeds = new float[locations.length];
//
//                for(int locationIndex = 0; locationIndex<locations.length; locationIndex++){
//                    times[locationIndex]= locations[locationIndex].getTime();
//                    speeds[locationIndex]= locations[locationIndex].getSpeed();
//                }
//
//                //Redundant in this case but negligibly inefficient//
//                //necessary because ChartActivity can show multiple plots//
//                intent.putExtra(KEY_TIMES, times);
//                intent.putExtra(KEY_VALS, speeds);
//
//                intent.putExtra(ChartActivity.KEY_TIMES_KEYS, new String[] {KEY_TIMES});
//                intent.putExtra(ChartActivity.KEY_VALS_KEYS, new String[] {KEY_VALS});
//                getActivity().startActivity(intent);
            }
        });

        root.findViewById(R.id.altitudeGraphButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(getActivity(), ChartActivity.class);
//                TTLocation[] locations = mapData.getLocations();
//                long[] times = new long[locations.length];
//                float[] altitudes = new float[locations.length];
//
//                for(int locationIndex = 0; locationIndex<locations.length;locationIndex++){
//                    times[locationIndex] = locations[locationIndex].getTime();
//                    altitudes[locationIndex] = locations[locationIndex].getElevation();
//                }
//
//                intent.putExtra(KEY_TIMES, times);
//                intent.putExtra(KEY_VALS, altitudes);
//                intent.putExtra(ChartActivity.KEY_VALS_KEYS, new String []{KEY_VALS});
//                intent.putExtra(ChartActivity.KEY_TIMES_KEYS, new String[]{KEY_TIMES});
//                getActivity().startActivity(intent);
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
