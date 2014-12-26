package net.taptools.android.trailtracker.Models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import net.taptools.android.trailtracker.TTSQLiteOpenHelper;

import static net.taptools.android.trailtracker.TTSQLiteOpenHelper.*;

/**
 * Created by Brian Murphy on 5/6/2014.
 */
public class Stop {

    private long id;
    private TTLocation startLocation;
    private TTLocation endLocation;
    private long mapId;

    private Stop(){}

    public static final Stop instanceOf(TTSQLiteOpenHelper sqLiteOpenHelper, long stpId){
        Stop stp = new Stop();
        SQLiteDatabase database = sqLiteOpenHelper.getReadableDatabase();
        Cursor crsr = database.query(TABLE_STOPS,ALL_STOP_COLUMNS,COLUMN_ID+" = "+stpId,
                null,null,null,null);
        crsr.moveToFirst();
        stp.id = stpId;
        stp.startLocation = TTLocation.instanceOf(sqLiteOpenHelper,crsr.getLong(
                crsr.getColumnIndex(COLUMN_START_LOCATION_ID)));
        stp.endLocation = TTLocation.instanceOf(sqLiteOpenHelper,crsr.getLong(
                crsr.getColumnIndex(COLUMN_END_LOCATION_ID)));
        stp.mapId = crsr.getLong(crsr.getColumnIndex(COLUMN_MAP_ID));
        crsr.close();
        return stp;
    }

    public static final Stop[] getAll(TTSQLiteOpenHelper sqLiteOpenHelper, long mapId){
        SQLiteDatabase database = sqLiteOpenHelper.getReadableDatabase();
        Cursor crsr = database.query(TABLE_STOPS,ALL_STOP_COLUMNS,COLUMN_MAP_ID+" = "+mapId,
                null,null,null,COLUMN_ID+" ASC");
        crsr.moveToFirst();
        Stop[] stops = new Stop[crsr.getCount()];
        for(int stopIndex = 0; !crsr.isAfterLast();stopIndex++){
            Stop stp = new Stop();
            stp.id = crsr.getInt(crsr.getColumnIndex(COLUMN_ID));
            stp.startLocation = TTLocation.instanceOf(sqLiteOpenHelper, crsr.getLong(
                    crsr.getColumnIndex(COLUMN_START_LOCATION_ID)));
            stp.endLocation = TTLocation.instanceOf(sqLiteOpenHelper, crsr.getLong(
                    crsr.getColumnIndex(COLUMN_END_LOCATION_ID)));
            stp.mapId = mapId;
            stops[stopIndex] = stp;
            crsr.moveToNext();
        }
        crsr.close();
        return stops;
    }

    public MarkerOptions getMarker(){
        int startTime = (int)getStartLocation().getTime();
        int endTime = (int)getEndLocation().getTime();
        int span = endTime-startTime;
        StringBuilder snippet = new StringBuilder("Stopped for ");
        byte mins = (byte)(span/60);
        snippet.append(mins ==0 ? "00" : mins);
        snippet.append(":");
        String secs = ""+(span%60);
        snippet.append(secs.length() == 1 ? "0" + secs : secs);
        MarkerOptions options = new MarkerOptions();
        options.title("Start")
                .snippet(snippet.toString())
                .position(new LatLng(getStartLocation().getLatitude(),
                        getStartLocation().getLongitude()));
        return options;
    }

    public long getId() {
        return id;
    }

    public TTLocation getStartLocation() {
        return startLocation;
    }

    public TTLocation getEndLocation() {
        return endLocation;
    }

    public long getMapId() {
        return mapId;
    }
}
