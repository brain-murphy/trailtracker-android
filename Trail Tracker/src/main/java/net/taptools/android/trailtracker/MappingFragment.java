package net.taptools.android.trailtracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.Spinner;

import com.google.android.gms.maps.MapFragment;

import java.util.ArrayList;

/**
 * A Fragment that hold the view and logic for allowing the user to view several
 * maps in a full screen manner.
 * In a hierarchy of Fragments on something like a backstack.
 *
 */
public class MappingFragment extends ResultsSubFragment {

    //Layouts, views//
    private LinearLayout keyLayout;
    private MapFragment mapFragment;

    /**
     * required public instantiation method
     * @param maps maps to display
     * @return {@link MappingFragment} ready to display
     */
    public static MappingFragment newInstance(ArrayList<Map> maps){
        MappingFragment fragment = new MappingFragment();
        fragment.activeMaps = maps;
        return fragment;
    }

    public MappingFragment() {
        //required empty default constructor
    }

    /**
     * Creates an anonymous inner subclass of MapFragment that calls
     * {@link MappingFragment#onMapReady()} when the {@link com.google.android.gms.maps.GoogleMap}
     * is available.
     * Adds the MapFragment to the Layout, sets the callback of the info button to show a new
     * {@link MappingFragment.InfoChoiceDialogFragment}.
     *
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mapping, container, false);
        keyLayout = (LinearLayout)root.findViewById(R.id.mapKeyLayout);
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
                .add(R.id.mapFrameMappingFrag, mapFragment)
                .commit();
        root.findViewById(R.id.mapInfoButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InfoChoiceDialogFragment.newInstance(activeMaps).show(getFragmentManager(),
                        "mapChooserDialogFrag");
            }
        });

        Spinner spinner = (Spinner) root.findViewById(R.id.chartSpinner);
        ArrayAdapter<String> aa = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,
                new String[]{"Chart Speed", "Chart Altitiude"});
        spinner.setAdapter(aa);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean justInstantiated = true;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (justInstantiated) {
                    justInstantiated = !justInstantiated;
                    return;
                }
                Log.d("MappingFragment.AdapterView#onItemSelected", "called");
                long[][] timeArrays = new long[activeMaps.size()][];
                float[][] valueArrays = new float[activeMaps.size()][];
                String chartTitle = null;
                if (position == 0) {
                    //Chart Speed//
                    chartTitle = "Speeds";
                    for (int mapIndex = 0; mapIndex < activeMaps.size(); mapIndex++) {
                        TTLocation[] locs = activeMaps.get(mapIndex).getLocations();
                        long[] times = new long[locs.length];
                        float[] speeds = new float[locs.length];
                        for (int locIndex = 0; locIndex < locs.length; locIndex++) {
                            times[locIndex] = locs[locIndex].getTime();
                            speeds[locIndex] = locs[locIndex].getSpeed();
                        }
                        timeArrays[mapIndex] = times;
                        valueArrays[mapIndex] = speeds;
                    }
                } else if (position == 1) {
                    //Chart Altitude//
                    chartTitle = "Altitudes";
                    for (int mapIndex = 0; mapIndex < activeMaps.size(); mapIndex++) {
                        TTLocation[] locs = activeMaps.get(mapIndex).getLocations();
                        long[] times = new long[locs.length];
                        float[] alts = new float[locs.length];
                        for (int locIndex = 0; locIndex < locs.length; locIndex++) {
                            times[locIndex] = locs[locIndex].getTime();
                            alts[locIndex] = locs[locIndex].getElevation();
                        }
                        timeArrays[mapIndex] = times;
                        valueArrays[mapIndex] = alts;
                    }
                }
                ((ResultsActivity) getActivity()).showChartFragment(chartTitle, timeArrays,
                        valueArrays, activeMaps);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.i("MappingFragment#Spinner", "nothing selected");
            }
        });
        return root;
    }

    private void onMapReady(){
        for (Map mapDatum : activeMaps) {
            mapFragment.getMap().addPolyline(mapDatum.getNewPolyline());
            for (Stop stop : mapDatum.getStops()) {
                mapFragment.getMap().addMarker(stop.getMarker());
            }
            for (Waypoint wp : mapDatum.getWaypoints()) {
                mapFragment.getMap().addMarker(wp.getMarker());
            }
        }
    }

    /**
     * Dialog fragment that shows a list of the currently displayed maps, and starts
     * {@link MapInfoFragment} when one is chosen.
     */
    public static class InfoChoiceDialogFragment extends DialogFragment
    {
        private ArrayList<Map> maps;
        public static InfoChoiceDialogFragment newInstance(ArrayList<Map> mapData){
            InfoChoiceDialogFragment fragment = new InfoChoiceDialogFragment();
            fragment.maps = mapData;
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String[] mapNames = new String[maps.size()];
            for(int mapIndex = 0; mapIndex<maps.size();mapIndex++){
                mapNames[mapIndex] = maps.get(mapIndex).getName();
            }
            ListAdapter adapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.select_dialog_item,mapNames);
            return new AlertDialog.Builder(getActivity())
                    .setAdapter(adapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i("InfoChoiceDialog", "onClick");
                            ArrayList<Map> map_s = new ArrayList<Map>(1);
                            map_s.add(maps.get(which));
                            ((ResultsActivity)getActivity()).showInfoFragment(map_s);
                        }
                    })
                    .setTitle("Choose a Map")
                    .create();
        }
    }
}
