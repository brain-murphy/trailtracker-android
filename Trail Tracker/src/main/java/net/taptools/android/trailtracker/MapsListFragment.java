package net.taptools.android.trailtracker;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;


public class MapsListFragment extends Fragment {
    /**
     * string keys for passing args to fragment
     */
    private static final String ARG_GROUP_TITLES = "grouptitles";
    private static final String ARG_CHILDREN = "children";
    private static final String ARG_IDS = "Mapids";
    private static final String ARG_INITIALLY_SELECTED = "initselected";

    /**
     * expanableListView is a ListView that displayes data points
     *                  based on category
     * listAdapter is a custom ListAdapter that manages grouping and
     *                  enabling/disabling of data points
     */
    private ExpandableListView expandableListView;
    private CheckableExpandableListAdapter listAdapter;


    private MultiPickerFragment multiPickerFragment;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param groupTitles List of all the category titles
     * @param children HashMap that groups a list of child products to the String key of the
     *                 category in which the children reside. One could get the list of
     *                 products from a particular category by passing an element of the
     *                 groupTitles ArrayList.
     * @return A new instance of fragment MapsListFragment.
     */
    public static MapsListFragment newInstance(ArrayList<String> groupTitles, HashMap<String,
            ArrayList<String>> children, int[] ids, MultiPickerFragment parent){
        MapsListFragment fragment = new MapsListFragment();
        fragment.multiPickerFragment = parent;
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_GROUP_TITLES, groupTitles);
        args.putSerializable(ARG_CHILDREN, children);
        args.putIntArray(ARG_IDS, ids);
        fragment.setArguments(args);
        return fragment;
    }

    public MapsListFragment() {
        // Required empty public constructor
    }

    public void selectMap(int mapId){
        int[] ids = getArguments().getIntArray(ARG_IDS);
        for(int idIndex = 0; idIndex<ids.length;idIndex++ ){
            if(ids[idIndex]==mapId){
                listAdapter.setChildEnabled(idIndex,true);
                listAdapter.notifyDataSetChanged();
            }
            return;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //typical creation of views
        View root = inflater.inflate(R.layout.fragment_expandable_list, container, false);
        expandableListView = (ExpandableListView) root.findViewById(R.id.expandableListView);
        //pass data point info and which are already enabled/disabled
        listAdapter = new CheckableExpandableListAdapter(getActivity(),
                getArguments().getStringArrayList(ARG_GROUP_TITLES),
                (LinkedHashMap<String, ArrayList<String>>)
                getArguments().getSerializable(ARG_CHILDREN));
        expandableListView.setAdapter(listAdapter);
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                //control checking/unchecking and store enabling/disabling
                CheckedTextView ctv = (CheckedTextView)v;
                ctv.toggle();
                listAdapter.setChildEnabled((int)id,ctv.isChecked());
                int[] mapIds = getArguments().getIntArray(ARG_IDS);
                if(ctv.isChecked()){
                    multiPickerFragment.addToMap(mapIds[(int)id]);
                }else{
                    multiPickerFragment.removeFromMap(mapIds[(int)id]);
                }
                return true;
            }
        });
        return root;
    }
}
