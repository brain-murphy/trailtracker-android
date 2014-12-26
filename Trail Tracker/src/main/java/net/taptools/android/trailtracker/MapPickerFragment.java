package net.taptools.android.trailtracker;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

import net.taptools.android.trailtracker.Models.Map;
import net.taptools.android.trailtracker.Models.Stop;
import net.taptools.android.trailtracker.Models.Waypoint;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;

import static net.taptools.android.trailtracker.TTSQLiteOpenHelper.*;


public class MapPickerFragment extends Fragment {

    public interface MapPickListener{
        public void onMapSelected(int mapId);
    }

    private LinearLayout root;

    private MapPickListener listener;

    private int selectedMapId = -1;

    private TTSQLiteOpenHelper databaseHelper;
    private MapFragment mapFrag;
    protected ExpandableListView listView;
    protected Button selectButton;

    public static MapPickerFragment newInstance(MapPickListener listener) {
        Log.d("MapPickerFragment#newInstance()","called");
        MapPickerFragment fragment = new MapPickerFragment();
        fragment.listener = listener;
        return fragment;
    }
    public MapPickerFragment() {
        // Required empty public constructor
    }

    public static String formatDate(Calendar cal){
        StringBuilder todaySb = new StringBuilder()
                .append(cal.get(Calendar.DATE))
                .append("/")
                .append(cal.get(Calendar.MONTH))
                .append("/")
                .append(cal.get(Calendar.YEAR));
        return todaySb.toString();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = (LinearLayout)inflater.inflate(R.layout.fragment_map_picker, container, false);

        LinkedHashMap<String,ArrayList<String>> groupsTable = new LinkedHashMap<String, ArrayList<String>>();
        ArrayList<String> groupNames = new ArrayList<String>();
        final int[] mapIds;
        databaseHelper = ((MyApplication)getActivity().getApplication()).getDatabaseHelper();
        Cursor crsr = databaseHelper.getReadableDatabase().query(TABLE_MAPS, new String[]{COLUMN_ID,
                COLUMN_NAME, COLUMN_START_TIME}, null, null, null, null, COLUMN_START_TIME + " DESC");
        crsr.moveToFirst();
        mapIds = new int[crsr.getCount()];

        Calendar todayCal = Calendar.getInstance();
        String todayCalStr = formatDate(todayCal);
        todayCal.roll(Calendar.DATE,false);
        String yesterdayCalStr = formatDate(todayCal);

        for(int mapIndex = 0; !crsr.isAfterLast();mapIndex++){
            mapIds[mapIndex] = crsr.getInt(crsr.getColumnIndex(COLUMN_ID));
            Calendar mapCal = Calendar.getInstance();
            mapCal.setTimeInMillis(crsr.getLong(crsr.getColumnIndex(COLUMN_START_TIME)));
            String mapDateStr = formatDate(mapCal);
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
        final MapPickerAdapter adapter = new MapPickerAdapter(groupNames,groupsTable,mapIds);
        listView = (ExpandableListView)root.findViewById(R.id.pickerListView);
        listView.setAdapter(adapter);
        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                //((CheckedTextView)v).setChecked(true);

                selectedMapId = (int)id;
                Map mapData = Map.instanceOf(databaseHelper,(int)id);
                GoogleMap map = mapFrag.getMap();
                map.clear();
                map.addPolyline(Map.toNewPolyline(mapData.getCheckpoints()));

                for(Waypoint wp : mapData.getWaypoints()){
                    map.addMarker(wp.getMarker());
                }
                for(Stop stop : mapData.getStops()){
                    map.addMarker(stop.getMarker());
                }
                adapter.notifyDataSetChanged();
                return true;
            }
        });

        selectButton = ((Button)root.findViewById(R.id.pickMapButton));
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedMapId>=0)
                listener.onMapSelected(selectedMapId);
            }
        });
        mapFrag = MapFragment.newInstance();
        getFragmentManager().beginTransaction()
                .add(R.id.pickerMapFrame,mapFrag)
                .commit();
        return root;
    }

    private class MapPickerAdapter extends BaseExpandableListAdapter {

        private ArrayList<String> groupNames;
        private LinkedHashMap<String,ArrayList<String>> groupsTable;
        private int[] ids;

        public MapPickerAdapter(ArrayList<String> groupNames, LinkedHashMap<String,ArrayList<String>> groupsTable,
                                 int[] ids){
            this.groupNames = groupNames;
            this.groupsTable = groupsTable;
            this.ids = ids;
        }

        @Override
        public int getGroupCount() {
            return groupNames.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return groupsTable.get(groupNames.get(groupPosition)).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groupNames.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return groupsTable.get(groupNames.get(groupPosition)).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            int childIndex=0;
            for(int groupIndex = 0; groupIndex<groupPosition; groupIndex++){
                childIndex+=getChildrenCount(groupIndex);
            }
            childIndex+=childPosition;
            return ids[childIndex];
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if(convertView==null){
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_expandable_list_item_2,null);
            }
            ((TextView)convertView.findViewById(android.R.id.text1))
                    .setText(groupNames.get(groupPosition));
            ((TextView)convertView.findViewById(android.R.id.text2))
                    .setText(getChildrenCount(groupPosition)+" Maps");
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if(convertView == null){
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_list_item_single_choice,null);
            }
            CheckedTextView listItem  = (CheckedTextView)convertView.findViewById(android.R.id.text1);
            listItem.setText(groupsTable.get(groupNames.get(groupPosition)).get(childPosition));
            listItem.setChecked(getChildId(groupPosition, childPosition) == selectedMapId);
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
