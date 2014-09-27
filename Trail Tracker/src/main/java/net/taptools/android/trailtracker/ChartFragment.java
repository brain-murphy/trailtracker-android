package net.taptools.android.trailtracker;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;

public class ChartFragment extends Fragment implements ResultsActivity.IResultsFragment {

    private long[][] timeArrays;
    private float[][] valueArrays;
    private ArrayList<Map> mapsToShare;

    private RelativeLayout layout;

    private GraphicalView mChart;
    private XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
    private ArrayList<XYSeries> currentSeriesList;
    private XYSeriesRenderer currentRenderer;

    public static ChartFragment newInstance(String title, long[][] timeArrays,
            float[][] valueArrays, ArrayList<Map> mapsToShare) {
        ChartFragment fragment = new ChartFragment();
        fragment.timeArrays = timeArrays;
        fragment.valueArrays = valueArrays;
        fragment.mapsToShare = mapsToShare;
        return fragment;
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

    private void addSampleData(){
        for (int seriesIndex = 0; seriesIndex < currentSeriesList.size(); seriesIndex++) {
            long[] times = timeArrays[seriesIndex];
            float[] vals = valueArrays[seriesIndex];
            for(int timeIndex = 0; timeIndex<times.length; timeIndex++){
                currentSeriesList.get(seriesIndex).add(times[timeIndex],vals[timeIndex]);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layout = (RelativeLayout) inflater.inflate(R.layout.fragment_chart,container,false);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChart == null) {
            initChart();
            addSampleData();
            mChart = ChartFactory.getTimeChartView(getActivity(), dataset, renderer, null );
            layout.addView(mChart);
        } else {
            mChart.repaint();
        }
    }

    @Override
    public ArrayList<Map> getMapsToShare() {
        return mapsToShare;
    }
}
