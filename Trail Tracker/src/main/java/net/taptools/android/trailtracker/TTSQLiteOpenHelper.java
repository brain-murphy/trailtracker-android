package net.taptools.android.trailtracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Brian Murphy on 5/5/2014.
 */
public class TTSQLiteOpenHelper extends SQLiteOpenHelper{
    public static final String DATABASE_NAME = "trailtracker.db";
    private static final int DATABASE_VERSION = 4;

    public static final String TABLE_MAPS ="maps";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_START_TIME = "start_time";
    public static final String COLUMN_END_TIME = "end_time";
    public static final String COLUMN_AVERAGE_SPEED = "average_speed";
    public static final String COLUMN_TOTAL_DISTANCE = "total_distance";
    public static final String COLUMN_MAXIMUM_SPEED = "max_speed";
    public static final String COLUMN_LINEAR_DISTANCE = "linear_distance";
    public static final String COLUMN_NOTES = "notes";
    public static final String COLUMN_MAX_ALTITUDE = "maxalt";
    public static final String COLUMN_MIN_ALTITUDE = "minalt";
    public static final String COLUMN_START_ALTITUDE = "startalt";
    public static final String COLUMN_END_ALTITUDE = "endalt";


    public static final String TABLE_WAYPOINTS = "waypoints";
    //some column strings already declared
    public static final String COLUMN_LOCATION_ID = "location_id";
    public static final String COLUMN_MAP_ID = "map_id";
    public static final String COLUMN_IMAGE = "image";

    public static final String TABLE_LOCATIONS = "locations";
    //some column strings already declared
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_SPEED = "speed";
    public static final String COLUMN_ELEVATION = "elevation";
    public static final String COLUMN_ACCURACY = "accuracy";
    public static final String COLUMN_BEARING = "bearing";
    public static final String COLUMN_DISTANCE = "distance";
    public static final String COLUMN_TIME = "time";

    public static final String TABLE_STOPS = "stops";
    public static final String COLUMN_START_LOCATION_ID = "startloc";
    public static final String COLUMN_END_LOCATION_ID = "endloc";
    //column strings already declared

    public static final String[] ALL_MAP_COLUMNS = {COLUMN_ID,COLUMN_NAME,COLUMN_START_TIME,
            COLUMN_END_TIME,COLUMN_AVERAGE_SPEED,COLUMN_TOTAL_DISTANCE,COLUMN_MAXIMUM_SPEED,
            COLUMN_LINEAR_DISTANCE,COLUMN_NOTES,COLUMN_MAX_ALTITUDE,COLUMN_MIN_ALTITUDE,
            COLUMN_START_ALTITUDE, COLUMN_END_ALTITUDE};
    public static final String[] ALL_WAYPOINT_COLUMNS = {COLUMN_ID,COLUMN_NAME, COLUMN_NOTES,
            COLUMN_LOCATION_ID,COLUMN_IMAGE, COLUMN_MAP_ID};
    public static final String[] ALL_LOCATION_COLUMNS = {COLUMN_ID,COLUMN_MAP_ID,COLUMN_LONGITUDE,
            COLUMN_LATITUDE,COLUMN_SPEED,COLUMN_ELEVATION,COLUMN_ACCURACY,COLUMN_BEARING,
            COLUMN_DISTANCE,COLUMN_TIME};
    public static final String[] ALL_STOP_COLUMNS = {COLUMN_ID,COLUMN_START_LOCATION_ID,
            COLUMN_END_LOCATION_ID, COLUMN_MAP_ID};

    private static final String CREATE_MAPS_TABLE_SQL = "CREATE TABLE "+TABLE_MAPS+"( "+
            COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
            COLUMN_NAME+" TEXT NOT NULL, "+
            COLUMN_START_TIME+" INTEGER NOT NULL, "+
            COLUMN_END_TIME+" INTEGER NOT NULL, "+
            COLUMN_AVERAGE_SPEED+" REAL NOT NULL, "+
            COLUMN_TOTAL_DISTANCE+" REAL NOT NULL, "+
            COLUMN_LINEAR_DISTANCE+" REAL NOT NULL, "+
            COLUMN_MAXIMUM_SPEED+" REAL NOT NULL, "+
            COLUMN_MAX_ALTITUDE+" REAL NOT NULL, "+
            COLUMN_MIN_ALTITUDE+" REAL NOT NULL, "+
            COLUMN_START_ALTITUDE+" REAL NOT NULL, "+
            COLUMN_END_ALTITUDE+" REAL NOT NULL, "+
            COLUMN_NOTES+" TEXT);";

    private static final String CREATE_LOCATIONS_TABLE_SQL = "CREATE TABLE "+TABLE_LOCATIONS+"( "+
            COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
            COLUMN_LONGITUDE+" REAL NOT NULL, "+
            COLUMN_LATITUDE+" REAL NOT NULL, "+
            COLUMN_SPEED+" REAL NOT NULL, "+
            COLUMN_ELEVATION+" REAL NOT NULL, "+
            COLUMN_ACCURACY+" REAL NOT NULL, "+
            COLUMN_BEARING+" REAL NOT NULL, "+
            COLUMN_DISTANCE+" REAL NOT NULL, "+
            COLUMN_TIME+" INTEGER NOT NULL, "+
            COLUMN_MAP_ID+" INTEGER NOT NULL, "+
            "FOREIGN KEY("+COLUMN_MAP_ID+") REFERENCES "+TABLE_MAPS+"("+COLUMN_ID+") );";

    private static final String CREATE_WAYPOINTS_TABLE_SQL = "CREATE TABLE "+TABLE_WAYPOINTS+"( "+
            COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
            COLUMN_NAME+" TEXT NOT NULL, "+
            COLUMN_IMAGE+" BLOB, "+
            COLUMN_NOTES+" TEXT, "+
            COLUMN_LOCATION_ID+" INTEGER NOT NULL, "+
            COLUMN_MAP_ID+" INTEGER NOT NULL, "+
            "FOREIGN KEY("+COLUMN_LOCATION_ID+") REFERENCES "+TABLE_LOCATIONS+"("+COLUMN_ID+") " +
            "FOREIGN KEY("+COLUMN_MAP_ID+") REFERENCES "+ TABLE_MAPS+"("+COLUMN_ID+") );";

    private static final String CREATE_STOPS_TABLE_SQL = "CREATE TABLE "+TABLE_STOPS+"( "+
            COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
            COLUMN_START_LOCATION_ID+" INTEGER NOT NULL, "+
            COLUMN_END_LOCATION_ID+" INTEGER NOT NULL, "+
            COLUMN_MAP_ID+" INTEGER NOT NULL, "+
            "FOREIGN KEY("+COLUMN_START_LOCATION_ID+") REFERENCES "+TABLE_LOCATIONS+"("+COLUMN_ID+") " +
            "FOREIGN KEY("+COLUMN_END_LOCATION_ID+") REFERENCES "+TABLE_LOCATIONS+"("+COLUMN_ID+") " +
            "FOREIGN KEY("+COLUMN_MAP_ID+") REFERENCES "+ TABLE_MAPS+"("+COLUMN_ID+") );";
    public TTSQLiteOpenHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MAPS_TABLE_SQL);
        db.execSQL(CREATE_LOCATIONS_TABLE_SQL);
        db.execSQL(CREATE_WAYPOINTS_TABLE_SQL);
        db.execSQL(CREATE_STOPS_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
