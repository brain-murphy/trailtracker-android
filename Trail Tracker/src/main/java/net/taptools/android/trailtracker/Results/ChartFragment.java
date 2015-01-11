package net.taptools.android.trailtracker.results;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.OnChartGestureListener;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Legend;
import com.github.mikephil.charting.utils.LimitLine;
import com.github.mikephil.charting.utils.MarkerView;
import com.github.mikephil.charting.utils.Utils;

import net.taptools.android.trailtracker.models.Map;
import net.taptools.android.trailtracker.R;

import java.util.ArrayList;
import java.util.Stack;

public class ChartFragment extends ResultsSubFragment implements OnChartGestureListener, OnChartValueSelectedListener {

    private static final String KEY_TIME_ARRAYS = "timearraykey";
    private static final String KEY_VALUE_ARRAYS = "valueArrayskey";
    private static final String KEY_TITLE = "titlekey";
    private static final String KEY_UNITS = "unitkey";

    private long[][] timeArrays;
    private float[][] valueArrays;
    private String title;
    private String units;

    //for formatting time//
    private int secondOffset;

    private LineChart chart;


    public static ChartFragment newInstance(String title, long[][] timeArrays,
            float[][] valueArrays, ArrayList<Map> mapsToShare, String units) {
        ChartFragment fragment = new ChartFragment();
        fragment.title = title;
        fragment.timeArrays = timeArrays;
        fragment.valueArrays = valueArrays;
        fragment.activeMaps = mapsToShare;
        fragment.units = units;
        return fragment;
    }

    public ChartFragment() {
        //required empty default constuctor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            timeArrays = (long[][]) savedInstanceState.getSerializable(KEY_TIME_ARRAYS);
            valueArrays = (float[][]) savedInstanceState.getSerializable(KEY_VALUE_ARRAYS);
            title = savedInstanceState.getString(KEY_TITLE);
            units = savedInstanceState.getString(KEY_UNITS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fragment_chart, container,
                false);

        chart = (LineChart) layout.findViewById(R.id.chart);
        chart.setOnChartGestureListener(this);
        chart.setOnChartValueSelectedListener(this);
        chart.setUnit(" " + units);
        chart.setDrawUnitsInChart(true);
        chart.setStartAtZero(false);
        chart.setDrawYValues(false);
        chart.setDrawBorder(true);
        chart.setBorderPositions(new BarLineChartBase.BorderPosition[]{
                BarLineChartBase.BorderPosition.BOTTOM
        });
        chart.setNoDataTextDescription("You need to provide data for the chart.");
        chart.setHighlightEnabled(true);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDescription("");
        MyMarkerView mv = new MyMarkerView(getActivity(), R.layout.custom_marker_view);
        mv.setOffsets(-mv.getMeasuredWidth() / 2, -mv.getMeasuredHeight());
        chart.setMarkerView(mv);
        // enable/disable highlight indicators (the lines that indicate the
        // highlighted Entry)
        chart.setHighlightIndicatorEnabled(false);

        // add data
        setData();

        chart.animateX(2500);

//        // restrain the maximum scale-out factor
//        mChart.setScaleMinima(3f, 3f);
//
//        // center the view to a specific position inside the chart
//        mChart.centerViewPort(10, 50);

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();

        // modify the legend ...
        // l.setPosition(LegendPosition.LEFT_OF_CHART);
        l.setForm(Legend.LegendForm.LINE);

        getActivity().setTitle(title);
        return layout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_TITLE, title);
        outState.putSerializable(KEY_TIME_ARRAYS, timeArrays);
        outState.putSerializable(KEY_VALUE_ARRAYS, valueArrays);
        outState.putString(KEY_UNITS, units);
    }

    private void setData() {

        //time offset//
        int min = Integer.MAX_VALUE;
        for (long[] times : timeArrays) {
            int time = (int) (times[0] / 1000);
            if (time != 0) {
                min = Math.min(min, time);
            }
        }
        secondOffset = min;

        //parse to stacks//

        ArrayList<Stack<Long>> timesStacks = new ArrayList<Stack<Long>>(timeArrays.length);
        ArrayList<Stack<Float>> valuesStacks = new ArrayList<Stack<Float>>(valueArrays.length);

        for (int mapIndex = 0; mapIndex < timeArrays.length; mapIndex++) {
            Stack<Long> timesStack = new Stack<Long>();
            Stack<Float> valueStack = new Stack<Float>();
            long[] times = timeArrays[mapIndex];
            float[] values = valueArrays[mapIndex];
            for (int i = times.length - 1; i >= 0; i--) {
                timesStack.push(times[i]);
                valueStack.push(values[i]);
            }

            timesStacks.add(timesStack);
            valuesStacks.add(valueStack);
        }


        //place in chart data structures in order//

        ArrayList<String> xVals = new ArrayList<String>();

        //initialize ArrayLists of y values//
        ArrayList<Entry>[] entryArrays = new ArrayList[valuesStacks.size()];
        for (int i = 0; i < entryArrays.length; i++) {
            entryArrays[i] = new ArrayList<Entry>(valuesStacks.get(i).size());
        }

        //store min and max for boundary lines//
        float minOnGraph = Float.MAX_VALUE;
        float maxOnGraph = Float.MIN_VALUE;

        while (!timesStacks.isEmpty()) {

            //find stack with lowest time//
            long minTime = Long.MAX_VALUE;
            int minTimeIndex = -1;
            for (int stackIndex = 0; stackIndex < timesStacks.size(); stackIndex++) {
                if (timesStacks.get(stackIndex).peek() < minTime) {
                    minTime = timesStacks.get(stackIndex).peek();
                    minTimeIndex = stackIndex;
                }
            }

            //Add lowest x val and pop//
            //add y val to correct Entry list with correct x index//
            xVals.add(formatTime(timesStacks.get(minTimeIndex).pop()));
            Entry entry = new Entry(valuesStacks.get(minTimeIndex).pop(), xVals.size() - 1);
            entryArrays[minTimeIndex].add(entry);

            //get rid of empty stacks//
            if (timesStacks.get(minTimeIndex).isEmpty()) {
                timesStacks.remove(minTimeIndex);
                valuesStacks.remove(minTimeIndex);
            }

            //min max for boundary lines//
            maxOnGraph = Math.max(maxOnGraph, entry.getVal());
            minOnGraph = Math.min(minOnGraph, entry.getVal());
        }

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>(entryArrays.length);

        for (int i = 0; i < entryArrays.length; i++) {
            dataSets.add(new LineDataSet(entryArrays[i], title));
        }

        LineData data = new LineData(xVals, dataSets);

        LimitLine ll1 = new LimitLine(maxOnGraph);
        ll1.setLineWidth(4f);
        ll1.enableDashedLine(10f, 10f, 0f);
        ll1.setDrawValue(true);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT);

        LimitLine ll2 = new LimitLine(minOnGraph);
        ll2.setLineWidth(4f);
        ll2.enableDashedLine(10f, 10f, 0f);
        ll2.setDrawValue(true);
        ll2.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT);

        data.addLimitLine(ll1);
        data.addLimitLine(ll2);

        // set data
        chart.setData(data);
    }

    private String formatTime(long millis) {
        int seconds = (int) (millis / 1000) - secondOffset;
        Log.d("time", "secondOffset:" + secondOffset + " seconds:" + seconds);
        int hour = (seconds / 3600);
        int min = (seconds / 60) % 60;
        int sec = seconds % 60;

        StringBuilder builder = new StringBuilder();
        if (hour > 0) {
            builder.append(String.format("%02d", hour))
                   .append(":");
        }
        builder.append(String.format("%02d", min))
                .append(":")
                .append(String.format("%02d", sec));

        return builder.toString();
    }

    @Override
    public void onChartLongPressed(MotionEvent motionEvent) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent motionEvent) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent motionEvent) {

    }

    @Override
    public void onChartFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {

    }

    @Override
    public void onValueSelected(Entry entry, int i) {

    }

    @Override
    public void onNothingSelected() {

    }

    private class MyMarkerView extends MarkerView {

        private TextView tvContent;

        public MyMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);

            tvContent = (TextView) findViewById(R.id.tvContent);
        }

        // callbacks everytime the MarkerView is redrawn, can be used to update the
        // content
        @Override
        public void refreshContent(Entry e, int dataSetIndex) {

            if (e instanceof CandleEntry) {

                CandleEntry ce = (CandleEntry) e;

                tvContent.setText("" + Utils.formatNumber(ce.getHigh(), 0, true));
            } else {

                tvContent.setText("" + Utils.formatNumber(e.getVal(), 0, true));
            }
        }
    }
}
