package net.taptools.android.trailtracker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import static net.taptools.android.trailtracker.TTSQLiteOpenHelper.*;
/**
 * Created by Brian Murphy on 5/6/2014.
 */
public class Waypoint {

    private long id;
    private String name;
    private String notes;
    private Bitmap image;
    private TTLocation location;

    private Waypoint(){}

    public static final Waypoint instanceOf(TTSQLiteOpenHelper openHelper, long waypointId){
        Waypoint wp = new Waypoint();
        SQLiteDatabase database = openHelper.getReadableDatabase();
        Cursor crsr = database.query(TABLE_WAYPOINTS,ALL_WAYPOINT_COLUMNS,COLUMN_ID+" = "+waypointId,
                null,null,null,null);
        crsr.moveToFirst();
        wp.id = waypointId;
        wp.name = crsr.getString(crsr.getColumnIndex(COLUMN_NAME));
        byte[] bitmapBytes = crsr.getBlob(crsr.getColumnIndex(COLUMN_IMAGE));
        wp.image = BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
        wp.location = TTLocation.instanceOf(openHelper,
                crsr.getInt(crsr.getColumnIndex(COLUMN_LOCATION_ID)));
        wp.notes = crsr.getString(crsr.getColumnIndex(COLUMN_NOTES));
        crsr.close();
        return wp;
    }

    public static final Waypoint[] getAll(TTSQLiteOpenHelper openHelper, long mapId){
        SQLiteDatabase database = openHelper.getReadableDatabase();
        Cursor crsr = database.query(TABLE_WAYPOINTS,ALL_WAYPOINT_COLUMNS,COLUMN_MAP_ID+" = "+mapId,
                null,null,null,COLUMN_ID+" ASC");
        crsr.moveToFirst();
        Waypoint[] waypoints = new Waypoint[crsr.getCount()];
        for(int wpIndex = 0; !crsr.isAfterLast();wpIndex++){
            Waypoint wp = new Waypoint();
            wp.id = crsr.getLong(crsr.getColumnIndex(COLUMN_ID));
            wp.name = crsr.getString(crsr.getColumnIndex(COLUMN_NAME));
            byte[] bitmapBytes = crsr.getBlob(crsr.getColumnIndex(COLUMN_IMAGE));
            wp.image = BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
            wp.location = TTLocation.instanceOf(openHelper,
                    crsr.getInt(crsr.getColumnIndex(COLUMN_LOCATION_ID)));
            wp.notes = crsr.getString(crsr.getColumnIndex(COLUMN_NOTES));
            waypoints[wpIndex] = wp;
            crsr.moveToNext();
        }
        crsr.close();
        return waypoints;
    }

    public MarkerOptions getMarker() {
        MarkerOptions options = new MarkerOptions();
        options.title(name);
        options.icon(BitmapDescriptorFactory.fromBitmap(image));
        if (notes.length() < 20){
            options.snippet(notes+"...");
        }else{
            options.snippet(notes.substring(0, 20) + "...");
        }
        options.position(location.toLatLng());
        return options;
    }


    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Bitmap getImage() {
        return image;
    }

    public TTLocation getLocation() {
        return location;
    }

    public String getNotes() {
        return notes;
    }
}
