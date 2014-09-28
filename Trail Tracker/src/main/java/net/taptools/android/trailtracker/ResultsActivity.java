package net.taptools.android.trailtracker;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class ResultsActivity extends Activity {

    public static final String KEY_MAP_IDS = "mapidskey";

    private static final String KEY_VIEW_STATE = " viewsatae";
    private static final int STATE_MAPPING = 0;
    private static final int STATE_INFO = 1;
    private static final int STATE_CHART = 2;
    private int viewState;

    private static final String TAG_MAPPING_FRAG = "mapfragtag";
    private static final String TAG_INFO_FRAG = "taginfofrag";
    private static final String TAG_CHART_FRAG = "tagchartFrag";
    private static final String[] TAGS_SEQUENCED = {TAG_MAPPING_FRAG, TAG_INFO_FRAG, TAG_CHART_FRAG};

    private FrameLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState.containsKey(KEY_VIEW_STATE)) {
            viewState = savedInstanceState.getInt(KEY_VIEW_STATE, 0);
            FragmentManager manager = getFragmentManager();
            Stack<ResultsSubFragment> fragStack = new Stack<ResultsSubFragment>();
            ResultsSubFragment temp = null;
            switch (viewState) {
                //Fallthrough behavior intentional//
                case STATE_CHART:
                    if((temp = (ResultsSubFragment)manager.findFragmentByTag(TAG_CHART_FRAG)) != null) {
                        fragStack.push(temp);
                    }
                case STATE_INFO:
                    if((temp = (ResultsSubFragment)manager.findFragmentByTag(TAG_INFO_FRAG)) != null) {
                        fragStack.push(temp);
                    }
                case STATE_MAPPING:
                    if((temp = (ResultsSubFragment)manager.findFragmentByTag(TAG_MAPPING_FRAG)) != null) {
                        fragStack.push(temp);
                    }
            }

            for (int fragIndex = 0; fragIndex < fragStack.size(); fragIndex++) {
                manager.beginTransaction()
                        .add(fragStack.pop(), TAGS_SEQUENCED[fragIndex])
                        .addToBackStack(TAGS_SEQUENCED[fragIndex])
                        .commit();
            }
        }

        TTSQLiteOpenHelper helper = ((MyApplication) getApplication()).getDatabaseHelper();
        int[] mapIds = getIntent().getIntArrayExtra(KEY_MAP_IDS);
        if (mapIds == null) {
            mapIds = savedInstanceState.getIntArray(KEY_MAP_IDS);
        }

        ArrayList<Map> maps = new ArrayList<Map>(mapIds.length);
        for (int id : mapIds) {
            maps.add(Map.instanceOf(helper,id));
        }

        layout = new FrameLayout(this);
        setContentView(layout);

        getFragmentManager().beginTransaction()
                .add(MappingFragment.newInstance(maps),"mappingFrag")
                .addToBackStack("mappingFrag")
                .commit();
    }

    public void showInfoFragment(ArrayList<Map> maps) {
        assert maps.size() == 1;

        getFragmentManager().beginTransaction()
                .add(MapInfoFragment.newInstance(maps), "mapInfoFrag")
                .addToBackStack("mapInfoFrag")
                .commit();
        viewState = STATE_INFO;
    }

    public void showChartFragment(String title, long[][] timeArrays, float[][] valueArrays,
                                  ArrayList<Map> maps) {
        getFragmentManager().beginTransaction()
                .add(ChartFragment.newInstance(title, timeArrays, valueArrays,maps), TAG_CHART_FRAG)
                .addToBackStack(TAG_CHART_FRAG)
                .commit();
        viewState = STATE_CHART;
    }

    @Override
    public void onBackPressed() {
        if (viewState > STATE_MAPPING) {
            FragmentManager manager = getFragmentManager();
            String name = manager.getBackStackEntryAt(manager.getBackStackEntryCount() - 1).getName();
            viewState = name.equals(TAG_MAPPING_FRAG) ? 0 : 1;
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_VIEW_STATE, viewState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.action_share) {
            FragmentManager manager = getFragmentManager();
            ResultsSubFragment frag = (ResultsSubFragment) manager.findFragmentByTag(
                    TAGS_SEQUENCED[viewState]);
            ArrayList<Map> mapsToShare = frag.getActiveMaps();

            Document doc = null;
            try {
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }

            for (Map map : mapsToShare) {
                doc.appendChild(map.getKMLElement(doc));
            }

            File emailDir = new File(getFilesDir().getAbsolutePath() + "/emailAttachments");
            emailDir.mkdirs();
            //File Name//
            String name = "";
            for (int i = 0; i < 15; i++) {
                name += ((int)(Math.random()*10)) + "";
            }
            File kmlFile = new File(emailDir.getAbsolutePath() + name + ".kml");

            try {
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(kmlFile));
                DOMSource domSource = new DOMSource(doc);
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.transform(domSource, result);
                out.write(writer.toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return super.onOptionsItemSelected(item);
    }
}
