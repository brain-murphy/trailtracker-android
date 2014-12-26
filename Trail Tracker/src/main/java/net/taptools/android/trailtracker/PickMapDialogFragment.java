package net.taptools.android.trailtracker;


import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.taptools.android.trailtracker.Models.Map;

import java.util.ArrayDeque;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class PickMapDialogFragment extends DialogFragment {

    public PickMapDialogFragment() {
        // Required empty public constructor
    }

    private static final String KEY_MAP_TITLES = "maptitlesstring";
    private static final String KEY_MAP_IDS = "mapidsstring";

    private MapPickListener listener;

    public static PickMapDialogFragment newInstance(String[] mapTitles, int[] mapIds,
                                                    MapPickListener listener) {
        PickMapDialogFragment frag = new PickMapDialogFragment();
        frag.listener = listener;
        Bundle args = new Bundle();
        args.putStringArray(KEY_MAP_TITLES, mapTitles);
        args.putIntArray(KEY_MAP_IDS, mapIds);
        frag.setArguments(args);
        return frag;
    }

    public static PickMapDialogFragment newInstance(ArrayList<Map> maps, MapPickListener listener) {
        String[] mapTitles = new String[maps.size()];
        int[] mapIds = new int[maps.size()];
        for (int i = 0; i < maps.size(); i++) {
            mapIds[i] = maps.get(i).getId();
            mapTitles[i] = maps.get(i).getName();
        }

        return newInstance(mapTitles, mapIds, listener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ListView listView = new ListView(getActivity());
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, getArguments().getStringArray(KEY_MAP_TITLES));
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listener.onMapChosen(getArguments().getIntArray(KEY_MAP_IDS)[position]);
            }
        });
        return listView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog frag = super.onCreateDialog(savedInstanceState);
        frag.setTitle("Choose a Map");
        return frag;
    }

    public interface MapPickListener {
        public void onMapChosen(int id);
    }
}
