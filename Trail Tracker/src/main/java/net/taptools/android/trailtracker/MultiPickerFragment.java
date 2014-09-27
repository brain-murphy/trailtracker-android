package net.taptools.android.trailtracker;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import static net.taptools.android.trailtracker.TTSQLiteOpenHelper.*;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;


public class MultiPickerFragment extends Fragment {
    private static final String KEY_MAP_ID = "mapid";
    private static final String KEY_ACTIVE_MAP_IDS = "activemapids";

    private Integer id_mapToDisplay = null;

    private MapFragment mapFragment;
    private MapsListFragment mapsListFragment;
    private ResultsFragment parent;

    private TTSQLiteOpenHelper sqliteOpenHelper;
    private SQLiteDatabase readableDb;

    //private ArrayList<Map> activeMaps;
    private ArrayList<Polyline> polylines;
    private HashMap<Integer, ArrayList<Marker>> activeMarkers;

    private static int[] colors = {R.color.blue,R.color.red,R.color.green, R.color.orange,
                            R.color.purple,R.color.cyan};
    private static int colorMarker=-1;
    public static int getAColor(){
        colorMarker++;
        return colors[colorMarker%5];
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param mapId id of map to display if specific map is requested
     * @return A new instance of fragment
     */
    public static MultiPickerFragment newInstance(int mapId,ResultsFragment parent) {
        MultiPickerFragment fragment = new MultiPickerFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_MAP_ID, mapId);
        fragment.setArguments(args);
        fragment.parent = parent;
        return fragment;
    }

    public static MultiPickerFragment newInstance(ResultsFragment parent){
        MultiPickerFragment fragment = new MultiPickerFragment();
        fragment.parent = parent;
        return fragment;
    }

    public MultiPickerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        if (getArguments() != null&& getArguments().containsKey(KEY_MAP_ID)) {
            id_mapToDisplay = getArguments().getInt(KEY_MAP_ID);
            //TODO display or place emphasis on this map.
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(getActivity());
        root.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        View layout = inflater.inflate(R.layout.fragment_multi_picker, root, true);
        FragmentManager fragManager = getFragmentManager();
        mapFragment = new MapFragment();
        fragManager.beginTransaction()
                .add(R.id.mapFragmentWindow_results, mapFragment)
                .commit();

        ((Button)layout.findViewById(R.id.viewMapsButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(parent.getActiveMaps().size()>0) {
                    //parent.startMappingFragment(parent.getActiveMaps());
                    Intent intent = new Intent(getActivity(), ResultsActivity.class);
                    ArrayList<Map> maps = parent.getActiveMaps();
                    int[] mapIds = new int[maps.size()];
                    for (int mapIndex = 0; mapIndex < maps.size(); mapIndex++) {
                        mapIds[mapIndex] = maps.get(mapIndex).getId();
                    }
                    intent.putExtra(ResultsActivity.KEY_MAP_IDS, mapIds);
                    startActivity(intent);
                }
            }
        });
        if(savedInstanceState != null){
            ArrayList<Integer> ids = savedInstanceState.getIntegerArrayList(KEY_ACTIVE_MAP_IDS);
            for(Integer id : ids){
                addToMap(id);
            }
        }
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        sqliteOpenHelper = ((MyApplication)getActivity().getApplication()).getDatabaseHelper();
        readableDb = sqliteOpenHelper.getReadableDatabase();
        updateList();
        if(MultiPickerFragment.this.getArguments()!=null)
            mapsListFragment.selectMap(MultiPickerFragment.this.getArguments().getInt(KEY_MAP_ID));
    }

    public void updateList(){
        FragmentManager manager = getFragmentManager();
        if(mapsListFragment !=null){
            manager.beginTransaction().remove(mapsListFragment)
                    .commit();
            parent.setActiveMaps(null);
            for(Polyline pl: polylines){
                pl.remove();
            }

            Set<Integer> markerKeys = activeMarkers.keySet();
            for(Integer key : markerKeys){
                ArrayList<Marker> markers = activeMarkers.get(key);
                for(Marker marker : markers){
                    marker.remove();
                }
            }

            polylines = null;
            activeMarkers = null;
        }
        String[] cols = {COLUMN_ID, COLUMN_NAME, COLUMN_START_TIME, COLUMN_NOTES};
        Cursor crsr = readableDb.query(TABLE_MAPS,cols,null,null,null,null,COLUMN_ID+" DESC");

        int[] ids = new int[crsr.getCount()];
        LinkedHashMap<String,ArrayList<String>> groupsTable = new LinkedHashMap<String, ArrayList<String>>();
        ArrayList<String> groupNames = new ArrayList<String>();

        crsr.moveToFirst();
        Calendar todayCal = Calendar.getInstance();
        String todayCalStr = MapPickerFragment.formatDate(todayCal);
        todayCal.roll(Calendar.DATE,false);
        String yesterdayCalStr = MapPickerFragment.formatDate(todayCal);

        for(int mapIndex = 0; !crsr.isAfterLast();mapIndex++){
            ids[mapIndex] = crsr.getInt(crsr.getColumnIndex(COLUMN_ID));
            Calendar mapCal = Calendar.getInstance();
            mapCal.setTimeInMillis(crsr.getLong(crsr.getColumnIndex(COLUMN_START_TIME)));
            String mapDateStr = MapPickerFragment.formatDate(mapCal);
            if(mapDateStr.equals(todayCalStr)){
                ArrayList<String> mapsInGroup= null;
                String todayStr = "Today";
                if(groupNames.contains(todayStr)) {
                    mapsInGroup = groupsTable.get(todayStr);
                }else {
                    groupNames.add(todayStr);
                    mapsInGroup = new ArrayList<String>();
                }
                mapsInGroup.add(crsr.getString(crsr.getColumnIndex(COLUMN_NAME)));
                groupsTable.put(todayStr,mapsInGroup);
            }else if(mapDateStr.equals(yesterdayCalStr)){
                ArrayList<String> mapsInGroup= null;
                String yesterdayStr = "Yesterday";
                if(groupNames.contains(yesterdayStr)) {
                    mapsInGroup = groupsTable.get(yesterdayStr);
                }else {
                    groupNames.add(yesterdayStr);
                    mapsInGroup = new ArrayList<String>();
                }
                mapsInGroup.add(crsr.getString(crsr.getColumnIndex(COLUMN_NAME)));
                groupsTable.put(yesterdayStr,mapsInGroup);
            }else{
                ArrayList<String> mapsInGroup = null;
                if(groupNames.contains(mapDateStr)){
                    mapsInGroup = groupsTable.get(mapDateStr);
                }else{
                    groupNames.add(mapDateStr);
                    mapsInGroup = new ArrayList<String>();
                }
                mapsInGroup.add(crsr.getString(crsr.getColumnIndex(COLUMN_NAME)));
                groupsTable.put(mapDateStr,mapsInGroup);
            }
            crsr.moveToNext();
        }
        mapsListFragment = MapsListFragment.newInstance(groupNames, groupsTable, ids, this);
        manager.beginTransaction()
                .add(R.id.expandableListViewWindow, mapsListFragment)
                .commit();
        return;
    }

    public void addToMap(int mapId){
        Map mapData = Map.instanceOf(sqliteOpenHelper,mapId);
        if(parent.getActiveMaps() == null){
            parent.setActiveMaps(new ArrayList<Map>());
        }
        if(polylines == null){
            polylines = new ArrayList<Polyline>();
        }
        if(activeMarkers == null){
            activeMarkers = new HashMap<Integer, ArrayList<Marker>>();
        }
        parent.getActiveMaps().add(mapData);
        PolylineOptions polylineOptions = new PolylineOptions();
        for(int locationIndex = 0; locationIndex<mapData.getLocations().length; locationIndex++){
            LatLng latLng = new LatLng(mapData.getLocations()[locationIndex].getLatitude(),
                                        mapData.getLocations()[locationIndex].getLongitude());
            polylineOptions.add(latLng);
        }
        polylineOptions.color(getResources().getColor(getAColor()));
        polylines.add(mapFragment.getMap().addPolyline(polylineOptions));

        ArrayList<Marker> markers = new ArrayList<Marker>(mapData.getWaypoints().length+mapData.getStops().length);
        for(int waypointIndex =0; waypointIndex<mapData.getWaypoints().length;waypointIndex++){
            markers.add(mapFragment.getMap().addMarker(mapData.getWaypoints()[waypointIndex].getMarker()));
        }
        for(int stopIndex = 0; stopIndex<mapData.getStops().length; stopIndex++){
            markers.add(mapFragment.getMap().addMarker(mapData.getStops()[stopIndex].getMarker()));
        }
        activeMarkers.put(mapData.getId(), markers);
    }

    public void removeFromMap(int mapId){
        if(parent.getActiveMaps() == null){
            Log.d("MultiPickerFragment removeFromMap()", "activeMaps is null");
        }
        for(int mapIndex=0; mapIndex<parent.getActiveMaps().size();mapIndex++){
            if(parent.getActiveMaps().get(mapIndex).getId()==mapId){
                parent.getActiveMaps().remove(mapIndex);
                polylines.get(mapIndex).remove();
                polylines.remove(mapIndex);
                colorMarker--;

                ArrayList<Marker> markers = activeMarkers.get(mapId);
                for(Marker marker : markers){
                    marker.remove();
                }
                activeMarkers.remove(mapId);
                break;
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        FragmentManager fm = getFragmentManager();
        try {
            if (mapFragment != null) {
                fm.beginTransaction()
                        .remove(mapFragment)
                        .commit();
                mapFragment = null;
            }
            if (mapsListFragment != null) {
                fm.beginTransaction()
                        .remove(mapsListFragment)
                        .commit();
                mapsListFragment = null;
            }
        }catch(IllegalStateException e){
            Log.e("destroying Fragment", "activity already destroyed");
        }
    }

    @SuppressLint("ValidFragment")
    private class ConfirmDeleteDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Are you sure you want to delete map(s)")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            for(Map map : parent.getActiveMaps()){
                                SQLiteDatabase db =sqliteOpenHelper.getWritableDatabase();
                                db.delete(TABLE_STOPS,COLUMN_MAP_ID+" = "+map.getId(),null);
                                db.delete(TABLE_WAYPOINTS, COLUMN_MAP_ID+" = "+map.getId(),null);
                                db.delete(TABLE_LOCATIONS, COLUMN_MAP_ID+" = "+map.getId(),null);
                                db.delete(TABLE_MAPS, COLUMN_ID+" = "+map.getId(), null);
                            }
                            updateList();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            return builder.create();
        }
    }
}
