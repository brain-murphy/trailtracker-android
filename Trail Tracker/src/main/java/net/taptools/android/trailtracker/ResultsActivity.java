package net.taptools.android.trailtracker;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;

import net.taptools.android.trailtracker.R;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.zip.CheckedOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class ResultsActivity extends Activity {

    private ArrayList<Map> maps;

    public static final String KEY_MAP_IDS = "mapidskey";

    private Fragment activeFragment;

    private FrameLayout layout;

    public interface IResultsFragment {
        public ArrayList<Map> getMapsToShare();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TTSQLiteOpenHelper helper = ((MyApplication) getApplication()).getDatabaseHelper();
        int[] mapIds = getIntent().getIntArrayExtra(KEY_MAP_IDS);
        if (mapIds == null) {
            mapIds = savedInstanceState.getIntArray(KEY_MAP_IDS);
        }

        maps = new ArrayList<Map>(mapIds.length);
        for (int id : mapIds) {
            maps.add(Map.instanceOf(helper,id));
        }

        layout = new FrameLayout(this);
        setContentView(layout);
        activeFragment = MappingFragment.newInstance(maps);

        getFragmentManager().beginTransaction()
                .add(activeFragment,"mappingFrag")
                .addToBackStack("mappingFrag")
                .commit();
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
            ArrayList<Map> mapsToShare = ((IResultsFragment)activeFragment).getMapsToShare();

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
