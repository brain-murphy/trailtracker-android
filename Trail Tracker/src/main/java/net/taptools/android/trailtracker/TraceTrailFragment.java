package net.taptools.android.trailtracker;

import android.app.Fragment;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.taptools.android.trailtracker.Models.Map;
import net.taptools.android.trailtracker.dialogs.EnableLocationDialogFragment;


public class TraceTrailFragment extends Fragment implements MapPickerFragment.MapPickListener, EnableLocationDialogFragment.OnGPSCancelListener {

    static final String KEY_IS_SELECTING = "isSelecting";
    static final String KEY_MAP_ID = "mapid";

    private int mapId = -1;
    private ActiveTracingFragment tracingFragment;
    private MapPickerFragment pickerFragment;

    public static TraceTrailFragment newInstance() {
        Log.d("TraceTrailFragment#newInstance()","called");
        TraceTrailFragment fragment = new TraceTrailFragment();
        return fragment;
    }
    public TraceTrailFragment() {

        Log.d("TraceTrailFragment#constructor","called");
    }

    public void onLocationChanged(Location loc){
        tracingFragment.onLocationChanged(loc);
    }

    public void onLocationServiceBound(){
        if(getActivity()!=null) {
            ((MainActivity) getActivity()).binder.startServingLocations((MainActivity) getActivity());
            if (!((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE))
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                EnableLocationDialogFragment.newInstance(this).show(getFragmentManager(), "gpsEnableDialog");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("TraceTrailFragment#onCreate()", "called");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    View root;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("TraceTrailFragment#onCreateView()", "called");
        if(root!=null){
            Log.d("TraceTrailFragment#onCreateView()", "returned old view");
            if(isTracing()){
                MainActivity activity = (MainActivity)getActivity();
                activity.bindToLocationService();
            }
            return root;
        }
        root = inflater.inflate(R.layout.fragment_tracing_main,container,false);
        if(pickerFragment== null){
            pickerFragment = MapPickerFragment.newInstance(this);
            Log.d("TraceTrailFragmnet#onCreateView()","new pickerFragment");
        }
        if(!pickerFragment.isAdded()) {
            Log.d("TraceTrailFragment#onCreateView()", "picker Fragment added");
            getFragmentManager().beginTransaction()
                    .add(R.id.traceTrailFrame, pickerFragment)
                    .commit();
        }
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MainActivity activity=(MainActivity)getActivity();
        if(activity.binder!=null) {
            activity.binder.stopServingLocations();
            Log.d("ActiveTracingFragment#onStop()", "Locations stopped");
        }
        ((MainActivity)getActivity()).unbindFromLocationService();
    }

    @Override
    public void onMapSelected(int mapId) {
        this.mapId = mapId;
        Map mapData = Map.instanceOf(((MyApplication)getActivity().getApplication())
                .getDatabaseHelper(),mapId);
        tracingFragment = ActiveTracingFragment.newInstance(mapData);
        getFragmentManager().beginTransaction()
                .replace(R.id.traceTrailFrame,tracingFragment)
                .commit();
    }

    public void onBackPressed(){
        pickerFragment = MapPickerFragment.newInstance(this);
        getFragmentManager().beginTransaction()
                .replace(R.id.traceTrailFrame,pickerFragment)
                .commit();
        MainActivity activity = (MainActivity)getActivity();
        if(activity.binder!=null) {
            activity.binder.stopServingLocations();
            activity.unbindFromLocationService();
        }
    }

    public boolean isTracing() {
        boolean added= false;
        if(tracingFragment!= null){
            added = tracingFragment.isAdded();
        }
        return added;
    }

    @Override
    public void onGPSCancel() {
        onBackPressed();
    }
}
