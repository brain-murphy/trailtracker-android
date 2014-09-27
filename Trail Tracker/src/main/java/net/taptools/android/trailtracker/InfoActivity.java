package net.taptools.android.trailtracker;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import net.taptools.android.trailtracker.R;

public class InfoActivity extends Activity {

    public static final String KEY_MAP_ID = "mapidsdsfa";
    private static final String TAG_INFO_FRAG = "inffofragtag";

    private MapInfoFragment infoFragment;

    @Override
    protected void onStart() {
        super.onStart();
        if (infoFragment == null) {
            infoFragment = MapInfoFragment.newInstance(Map.instanceOf(
                    ((MyApplication)getApplication()).getDatabaseHelper(),
                    getIntent().getIntExtra(KEY_MAP_ID, -1)));
        }
        getFragmentManager().beginTransaction()
                .add(infoFragment, TAG_INFO_FRAG)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getFragmentManager().beginTransaction()
                .remove(infoFragment)
                .commit();
    }
}
