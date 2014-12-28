package net.taptools.android.trailtracker.results;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import net.taptools.android.trailtracker.models.Map;
import net.taptools.android.trailtracker.R;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;

public class ChartFragment extends ResultsSubFragment {

    private static final String KEY_TIME_ARRAYS = "timearraykey";
    private static final String KEY_VALUE_ARRAYS = "valueArrayskey";
    private static final String KEY_TITLE = "titlekey";

    private long[][] timeArrays;
    private float[][] valueArrays;
    private String title;

    private RelativeLayout layout;

    private GraphicalView mChart;
    private XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
    private ArrayList<XYSeries> currentSeriesList;
    private XYSeriesRenderer currentRenderer;

    public static ChartFragment newInstance(String title, long[][] timeArrays,
            float[][] valueArrays, ArrayList<Map> mapsToShare) {
        ChartFragment fragment = new ChartFragment();
        fragment.title = title;
        fragment.timeArrays = timeArrays;
        fragment.valueArrays = valueArrays;
        fragment.activeMaps = mapsToShare;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            timeArrays = (long[][]) savedInstanceState.getSerializable(KEY_TIME_ARRAYS);
            valueArrays = (float[][]) savedInstanceState.getSerializable(KEY_VALUE_ARRAYS);
            title = savedInstanceState.getString(KEY_TITLE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_TITLE, title);
        outState.putSerializable(KEY_TIME_ARRAYS, timeArrays);
        outState.putSerializable(KEY_VALUE_ARRAYS, valueArrays);
    }

    public ChartFragment() {
        //required empty default constuctor
    }

    private void initChart() {
        currentSeriesList = new ArrayList<XYSeries>();
        for (int index = 0; index < timeArrays.length; index++) {
            currentSeriesList.add(new XYSeries(""));
        }
        dataset.addAllSeries(currentSeriesList);
        currentRenderer = new XYSeriesRenderer();
        renderer.addSeriesRenderer(currentRenderer);
    }

    private void addData(){
        for (int seriesIndex = 0; seriesIndex < currentSeriesList.size(); seriesIndex++) {
            long[] times = timeArrays[seriesIndex];
            float[] vals = valueArrays[seriesIndex];
            for (int timeIndex = 0; timeIndex < times.length; timeIndex++) {
                //standardize all lines to start at zero//
                currentSeriesList.get(seriesIndex).add(times[timeIndex] - times[0], vals[timeIndex]);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layout = (RelativeLayout) inflater.inflate(R.layout.fragment_chart,container,false);
        getActivity().setTitle(title);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChart == null) {
            initChart();
            addData();
            mChart = ChartFactory.getTimeChartView(getActivity(), dataset, renderer, null );
            layout.addView(mChart);
        } else {
            mChart.repaint();
        }
    }
}
