package net.taptools.android.trailtracker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import static net.taptools.android.trailtracker.TTSQLiteOpenHelper.*;


import java.io.ByteArrayOutputStream;

public class WaypointActivity extends Activity implements View.OnClickListener {

    static final int REQUEST_IMAGE_CAPTURE =9078;
    static final String KEY_MAP_ID = "mapIdkey";
    static final String KEY_LOCATION_ID = "locationID";
    static final String KEY_WP_ID = "waypointId";

    private ImageButton takePicButton;
    private ImageButton removeImageButton;
    private ImageView wpImage;
    private Bitmap bmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waypoint);
        takePicButton = (ImageButton)findViewById(R.id.addPhotoImageButton);
        takePicButton.setOnClickListener(this);
        removeImageButton = (ImageButton)findViewById(R.id.removePhotoImageButton_wp);
        removeImageButton.setVisibility(View.INVISIBLE);
        removeImageButton.setOnClickListener(this);
        wpImage = (ImageView) findViewById(R.id.wpImage_imageView);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.waypoint, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_saveWaypoint) {
            SQLiteDatabase db = ((MyApplication)getApplication()).getDatabaseHelper()
                    .getWritableDatabase();
            ContentValues vals = new ContentValues();
            if(wpImage.getVisibility()==View.VISIBLE){
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG,9,outStream);
                vals.put(COLUMN_IMAGE,outStream.toByteArray());
            }
            vals.put(COLUMN_NAME,((EditText)findViewById(R.id.titleTFwp)).getText().toString());
            vals.put(COLUMN_NOTES,((EditText)findViewById(R.id.notesTFwp)).getText().toString());
            vals.put(COLUMN_MAP_ID, getIntent().getExtras().getLong(KEY_MAP_ID));
            vals.put(COLUMN_LOCATION_ID, getIntent().getExtras().getLong(KEY_LOCATION_ID));
            long wpId = db.insert(TABLE_WAYPOINTS,null,vals);
            Intent data = new Intent();
            data.putExtra(KEY_WP_ID, wpId);
            Log.d("onServiceConnected()", "result about to be set");
            setResult(RESULT_OK, data);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.addPhotoImageButton) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }else if(v.getId()==R.id.removePhotoImageButton_wp){
            wpImage.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode ==REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            wpImage.setVisibility(View.VISIBLE);
            bmp=(Bitmap)data.getExtras().get("data");
            wpImage.setImageBitmap(bmp);
        }
    }
}
