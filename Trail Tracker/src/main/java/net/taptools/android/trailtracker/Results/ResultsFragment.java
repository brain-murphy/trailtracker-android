package net.taptools.android.trailtracker.results;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import net.taptools.android.trailtracker.CheckableExpandableListAdapter;
import net.taptools.android.trailtracker.dialogs.ConfirmDeleteDialogFragment;
import net.taptools.android.trailtracker.models.Map;
import net.taptools.android.trailtracker.MyApplication;
import net.taptools.android.trailtracker.R;
import net.taptools.android.trailtracker.dialogs.RenameDialogFragment;
import net.taptools.android.trailtracker.models.Stop;
import net.taptools.android.trailtracker.TTSQLiteOpenHelper;
import net.taptools.android.trailtracker.models.Waypoint;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static net.taptools.android.trailtracker.TTSQLiteOpenHelper.*;


public class ResultsFragment extends Fragment implements RenameDialogFragment.RenameListener,
        ConfirmDeleteDialogFragment.OnMapDeletedListener {

    //TODO test deletion ConcurrentModificationException

    private ArrayList<Map> activeMaps = new ArrayList<Map>();

    private volatile ArrayList<String> groupNames;
    private volatile LinkedHashMap<String, ArrayList<String>> mapGroups;
    private volatile ArrayList<Integer> mapIds;
    private CheckableExpandableListAdapter listAdapter;

    private TTSQLiteOpenHelper databaseHelper;

    private MapFragment mapFragment;
    private HashMap<Map, Polyline> polylines;
    private HashMap<Map, ArrayList<Marker>> markers;

    private static int[] colors = {R.color.blue, R.color.red, R.color.green, R.color.orange,
            R.color.purple, R.color.cyan};
    private static int colorMarker = -1;
    public static int getAColor() {
        colorMarker++;
        return colors[colorMarker % 5];
    }

    public static ResultsFragment newInstance() {
        ResultsFragment fragment = new ResultsFragment();
        return fragment;
    }

    public ResultsFragment() {
        // Required empty public constructor
    }

    /**
     * Allocate data structures to store expandable listData.
     * Allocate data structures to store references to {@link GoogleMap} graphics objects.
     * Not populated until {@link ResultsFragment#onActivityCreated(android.os.Bundle)}onActivityCreated()}.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupNames = new ArrayList<String>();
        mapGroups = new LinkedHashMap<String, ArrayList<String>>();
        mapIds = new ArrayList<Integer>();
        //allocate hash maps to store reference to map data displayed on GoogleMap//
        polylines = new HashMap<Map, Polyline>();
        markers = new HashMap<Map, ArrayList<Marker>>();
    }

    @Override
    public void onStart() {
        super.onStart();
        onMapDeleted();
    }

    /**
     * Creates view by:
     * -inflating layout
     * -instantiating and attaching {@link MapFragment} to upper frame
     * -getting {@link ExpandableListView} instance, setting adapter and onClick callback. Onclick
     *      adds polylines and markers to mapFragment and records that the map was selectedds
     * @return all views in hierarchy for this Fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_results, container, false);
        root.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        FragmentManager fragManager = getFragmentManager();
        mapFragment = new MapFragment();
        fragManager.beginTransaction()
                .add(R.id.mapFragmentWindow_results, mapFragment)
                .commit();

        listAdapter = new CheckableExpandableListAdapter(
                getActivity(), groupNames, mapGroups, mapIds);
        ExpandableListView expandableListView = (ExpandableListView) root.findViewById(R.id.expandableListView);
        expandableListView.setAdapter(listAdapter);

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id) {
                CheckedTextView checkedText = (CheckedTextView) v.findViewById(android.R.id.text1);
                checkedText.setChecked(!checkedText.isChecked());
                //check to see if this map is already being displayed. If so, remove//
                for (int activeMapIndex = 0; activeMapIndex < activeMaps.size(); activeMapIndex++) {
                    Map removeMap = activeMaps.get(activeMapIndex);
                    if (removeMap.getId() == id) {

                        //notify adapter
                        listAdapter.setChildChecked((int) id, false);
                        //remove markers from map//
                        for (Marker mark : markers.get(removeMap)) {
                            mark.remove();
                        }
                        //remove marker arraylist from marker hashmap//
                        markers.remove(removeMap);
                        //remove polyline//
                        polylines.get(removeMap).remove();
                        polylines.remove(removeMap);

                        activeMaps.remove(removeMap);

                        //do not pass go. do not collect $200//
                        return true;
                    }
                }

                //if the above did not remove a map and return//
                //Get full map data in memory, display//
                Map mapData = Map.instanceOf(databaseHelper, (int) id);
                activeMaps.add(mapData);

                GoogleMap map = mapFragment.getMap();
                PolylineOptions line = Map.toNewPolyline(mapData.getCheckpoints());
                //set a (unique) color. (Only five colors)//
                line.color(getAColor());
                polylines.put(mapData, map.addPolyline(line));
                mapFragment.getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(
                        mapData.getLocations()[0].toLatLng(), 30f));

                ArrayList<Marker> markerList = new ArrayList<Marker>();
                for (Waypoint wp : mapData.getWaypoints()) {
                    markerList.add(map.addMarker(wp.getMarker()));
                }
                for (Stop stop : mapData.getStops()) {
                    markerList.add(map.addMarker(stop.getMarker()));
                }
                markers.put(mapData, markerList);

                listAdapter.setChildChecked((int) id, true);
                //listAdapter.notifyDataSetChanged();
                return true;
            }
        });

        //set up View Maps Button//
        root.findViewById(R.id.viewMapsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activeMaps.size() > 0) {
                    Intent intent = new Intent(getActivity(), ResultsActivity.class);

                    int[] mapIds = new int[activeMaps.size()];
                    for (int mapIndex = 0; mapIndex < activeMaps.size(); mapIndex++) {
                        mapIds[mapIndex] = activeMaps.get(mapIndex).getId();
                    }
                    intent.putExtra(ResultsActivity.KEY_MAP_IDS, mapIds);

                    startActivity(intent);
                }
            }
        });

        return root;
    }

    /**
     * Get instance of {@link TTSQLiteOpenHelper} and execute new {@link ReadFromDBTask}.
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        databaseHelper = ((MyApplication) getActivity().getApplication()).getDatabaseHelper();
        new ReadFromDBTask<Void, Void, Void>().execute();
    }

    /**
     * Helper method to format date for drop down grouping
     * @param cal Calendar representing the date that will be stringified
     * @return d/m/yyyy
     */
    private static String formatDate(Calendar cal){
        StringBuilder todaySb = new StringBuilder()
                .append(cal.get(Calendar.DATE))
                .append("/")
                .append(cal.get(Calendar.MONTH))
                .append("/")
                .append(cal.get(Calendar.YEAR));
        return todaySb.toString();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.results,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                if (activeMaps.size() > 0) {
                    ConfirmDeleteDialogFragment.newInstance(activeMaps, this)
                            .show(getFragmentManager(), "deleteMapFrag");
                } else {
                    Toast.makeText(getActivity(), "Select at least one map", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_rename:
                if (activeMaps.size() == 1) {
                    RenameDialogFragment.newInstance(activeMaps.get(0).getName(), this)
                            .show(getFragmentManager(), "renameDialog");
                } else {
                    Toast.makeText(getActivity(), "Select one map", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_share:
                try {
                    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                            .newDocument();
                    for (Map map : activeMaps) {
                        doc.appendChild(map.getKMLElement(doc));
                    }
                    Log.d("xml", doc.toString());
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }
        }
        return true;
    }

    /**
     * Callback from {@link RenameDialogFragment} that updates SqLite entry for map name and then
     * re runs {@link ReadFromDBTask} to assure normalization.
     * @param newName
     */
    @Override
    public void onRename(String newName) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, newName);
        SQLiteDatabase db = ((MyApplication) getActivity().getApplication()).getDatabaseHelper()
                .getWritableDatabase();
        db.update(TABLE_MAPS, values, COLUMN_ID + " = " + activeMaps.get(0).getId(), null);
        new ReadFromDBTask<Void, Void, Void>().execute();
    }

    @Override
    public void onMapDeleted() {
        new ReadFromDBTask<Void, Void, Void>(){
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                //find whether activeMaps contains more maps than are currently in the list (after deletion)//
                for (int i = 0; i < activeMaps.size(); i++) {
                    Map activeMap = activeMaps.get(0);
                    boolean found = false;
                    for (int readId : mapIds) {
                        if (readId == activeMap.getId()) {
                            found = true;
                            break;
                        }
                    }
                    //remove from map and data structures//
                    if (!found) {
                        //remove markers from map//
                        for (Marker mark : markers.get(activeMap)) {
                            mark.remove();
                        }
                        //remove marker arraylist from marker hashmap//
                        markers.remove(activeMap);
                        //remove polyline//
                        polylines.get(activeMap).remove();
                        polylines.remove(activeMap);

                        activeMaps.remove(activeMap);
                    }
                }
            }
        }.execute();

    }

    /**
     * Background task that reads into memory the SqLite data necessary to lay out the
     * {@link ExpandableListView}
     * @param <Params> always Void
     * @param <Progress> always Void
     * @param <Result> always Void
     */
    private class ReadFromDBTask<Params, Progress, Result>
            extends AsyncTask<Params, Progress, Result> {

        @Override
        protected Result doInBackground(Params... params) {
            //get date string for today//
            Calendar todayCal = Calendar.getInstance();
            String todayCalStr = formatDate(todayCal);
            todayCal.roll(Calendar.DATE,false);
            String yesterdayCalStr = formatDate(todayCal);

            //Query SqLite for pertinent info, in descending order by time recorded//
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            String[] colsToQuery = {COLUMN_ID, COLUMN_NAME, COLUMN_START_TIME, COLUMN_NOTES};
            Cursor crsr = db.query(TABLE_MAPS, colsToQuery, null, null, null, null,
                    COLUMN_ID + " DESC");

            //wipe all previous records from data structures//
            groupNames.clear();
            mapGroups.clear();
            mapIds.clear();

            crsr.moveToFirst();
            for (int mapIndex = 0; !crsr.isAfterLast(); mapIndex++) {

                mapIds.add(crsr.getInt(crsr.getColumnIndex(COLUMN_ID)));

                //Get string for the date during which this map was created//
                Calendar mapCal = Calendar.getInstance();
                mapCal.setTimeInMillis(crsr.getLong(crsr.getColumnIndex(COLUMN_START_TIME)));
                String mapDateStr = formatDate(mapCal);

                //Place map name in group titled "Today" if the map was created today//
                if (mapDateStr.equals(todayCalStr)) {
                    ArrayList<String> mapsInGroup = null;
                    String todayStr = "Today";
                    //Create arraylist if it is not already in hash map//
                    if (groupNames.contains(todayStr)) {
                        mapsInGroup = mapGroups.get(todayStr);
                    }else {
                        groupNames.add(todayStr);
                        mapsInGroup = new ArrayList<String>();
                    }
                    mapsInGroup.add(crsr.getString(crsr.getColumnIndex(COLUMN_NAME)));
                    mapGroups.put(todayStr,mapsInGroup);
                    //Place map name in group titled "Yesterday" if it was created yesterday//
                } else if (mapDateStr.equals(yesterdayCalStr)) {
                    ArrayList<String> mapsInGroup = null;
                    String yesterdayStr = "Yesterday";
                    if (groupNames.contains(yesterdayStr)) {
                        mapsInGroup = mapGroups.get(yesterdayStr);
                    }else {
                        groupNames.add(yesterdayStr);
                        mapsInGroup = new ArrayList<String>();
                    }
                    mapsInGroup.add(crsr.getString(crsr.getColumnIndex(COLUMN_NAME)));
                    mapGroups.put(yesterdayStr,mapsInGroup);
                    //if map created three days or more ago, store in group by date//
                } else {
                    ArrayList<String> mapsInGroup = null;
                    if(groupNames.contains(mapDateStr)){
                        mapsInGroup = mapGroups.get(mapDateStr);
                    }else{
                        groupNames.add(mapDateStr);
                        mapsInGroup = new ArrayList<String>();
                    }
                    mapsInGroup.add(crsr.getString(crsr.getColumnIndex(COLUMN_NAME)));
                    mapGroups.put(mapDateStr,mapsInGroup);
                }
                publishProgress();
                crsr.moveToNext();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Progress... values) {
            super.onProgressUpdate(values);
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void onPostExecute(Result aVoid) {
            super.onPostExecute(aVoid);
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }
    }
}