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
                .add(R.id.mapFrame_Mapping,mapFragment)
                .commit();
        root.findViewById(R.id.mapInfoButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InfoChoiceDialogFragment.newInstance(activeMaps).show(getFragmentManager(),
                        "mapChooserDialogFrag");
            }
        });
        return root;
    }

    private void onMapReady(){
        for (Map mapDatum : activeMaps) {
            mapFragment.getMap().addPolyline(mapDatum.getPolyline());
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
                            Log.d("InfoChoiceDialog", "onClick");
                        }
                    })
                    .setTitle("Choose a Map")
                    .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            Log.d("InfoChoiceDialog", "onItemSelecteds");
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    })
                    .create();
        }
    }
}
