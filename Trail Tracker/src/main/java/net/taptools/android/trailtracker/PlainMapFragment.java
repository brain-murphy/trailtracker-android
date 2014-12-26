package net.taptools.android.trailtracker;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.maps.MapFragment;

import net.taptools.android.trailtracker.Models.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class PlainMapFragment extends Fragment {

    private MapFragment mapFragment;

    public PlainMapFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FrameLayout layout = new FrameLayout(getActivity());
        layout.setId(R.id.container);

        mapFragment = MapFragment.newInstance();
        getFragmentManager().beginTransaction()
                .add(R.id.container, mapFragment)
                .commit();
        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapFragment != null && mapFragment.isAdded()) {
            getFragmentManager().beginTransaction()
                    .remove(mapFragment)
                    .commit();
        }
    }
}
