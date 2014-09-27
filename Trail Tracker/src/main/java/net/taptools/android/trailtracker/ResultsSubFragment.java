package net.taptools.android.trailtracker;



import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Abstract Fragment used by Fragments embedded inside {@link ResultsActivity}.
 * standardizes the reception and reporting of the {@link Map}s that are displayed.
 */
public abstract class ResultsSubFragment extends Fragment {

    private static final String KEY_MAP_IDS = "mapids";

    protected ArrayList<Map> activeMaps;

    public ArrayList<Map> getActiveMaps() {
        return activeMaps;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (savedInstanceState.containsKey(KEY_MAP_IDS)) {
            TTSQLiteOpenHelper helper =((MyApplication)getActivity().getApplication())
                    .getDatabaseHelper();
            int[] ids = savedInstanceState.getIntArray(KEY_MAP_IDS);
            activeMaps = new ArrayList<Map>(ids.length);
            for (int id : ids) {
                activeMaps.add(Map.instanceOf(helper, id));
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int[] ids = new int[activeMaps.size()];
        for (int mapIndex = 0; mapIndex < activeMaps.size(); mapIndex++) {
            ids[mapIndex] = activeMaps.get(mapIndex).getId();
        }

        outState.putIntArray(KEY_MAP_IDS, ids);
    }
}
