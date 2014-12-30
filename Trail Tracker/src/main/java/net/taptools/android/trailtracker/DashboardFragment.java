package net.taptools.android.trailtracker;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class DashboardFragment extends Fragment {

    private TextView timeTextView;
    private TextView distanceTextView;
    private TextView speedTextView;
    private TextView altitudeTextView;

    private long time = 0;
    private Timer timer = new Timer();

    private String unitSystem;

    public DashboardFragment() {
        // Required empty public constructor
    }

    public void pauseTimer() {
        timer.cancel();
        timer.purge();
    }

    public void resumeTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long second = (time) % 60;
                        long minute = (time / 60) % 60;
                        long hour = (time / 3600) % 24;

                        timeTextView.setText(String.format("%02d:%02d:%02d", hour, minute, second));
                        time++;
                    }
                });
            }
        }, 0, 1000);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        unitSystem = prefs.getString(getString(R.string.key_units), getResources().getStringArray(R.array.unit_systems)[0]);
        prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(getString(R.string.key_units))){
                    unitSystem = prefs.getString(getString(R.string.key_units),
                            getResources().getStringArray(R.array.unit_systems)[0]);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        timeTextView = (TextView) root.findViewById(R.id.timeTextView);
        distanceTextView = (TextView) root.findViewById(R.id.distanceTextView);
        speedTextView = (TextView) root.findViewById(R.id.speedTextView);
        altitudeTextView = (TextView) root.findViewById(R.id.altitudeTextView);
        return root;
    }

    public void setStats(float distance, float speed, double altitude){
        if (time == 0) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            long second = (time) % 60;
                            long minute = (time / 60) % 60;
                            long hour = (time / 3600) % 24;

                            timeTextView.setText(String.format("%02d:%02d:%02d", hour, minute, second));
                            time++;
                        }
                    });
                }
            }, 0, 1000);
        }

        if (unitSystem.equals(getResources().getStringArray(R.array.unit_systems)[0])) {
            distanceTextView.setText(String.format("%.2f mi", distance * 0.000621371));
            speedTextView.setText(String.format("%.2f mph", speed * 3600 * 0.000621371));
            altitudeTextView.setText(String.format("%.2f ft", distance * 3.28084));
        } else {
            String.format("%.2f km", distance / 1000);
            speedTextView.setText(String.format("%.2f km/h", speed / 1000 * 3600));
            altitudeTextView.setText(String.format("%.2f m", altitude));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        timer.cancel();
        timer.purge();
    }
}
