package net.taptools.android.trailtracker;

import android.app.Activity;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DashboardFragment extends Fragment {

    private TextView timeTextView;
    private TextView distanceTextView;
    private TextView speedTextView;
    private TextView altitudeTextView;
    private long time =0;

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        timeTextView = (TextView)root.findViewById(R.id.timeTextView);
        distanceTextView =(TextView)root.findViewById(R.id.distanceTextView);
        speedTextView = (TextView)root.findViewById(R.id.speedTextView);
        altitudeTextView = (TextView)root.findViewById(R.id.altitudeTextView);
        return root;
    }

    public void setStats(long timeInterval, float distance, float speed, double altitude){
        time += timeInterval;
        long second = (time / 1000) % 60;
        long minute = (time / (1000 * 60)) % 60;
        long hour = (time / (1000 * 60 * 60)) % 24;
        timeTextView.setText(String.format("%02d:%02d:%02d", hour, minute, second));
        //TODO convert to desired units
        distanceTextView.setText(String.format("%.2f", distance));
        speedTextView.setText(String.format("%.2f",speed));
        altitudeTextView.setText(String.format("%.2f",altitude));
    }
}
