package net.taptools.android.trailtracker.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.model.PolylineOptions;

import net.taptools.android.trailtracker.TTSQLiteOpenHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;

import static net.taptools.android.trailtracker.TTSQLiteOpenHelper.*;

/**
 * Created by Brian Murphy on 5/6/2014.
 */
public class Map {
    private int id;
    private String name;
    private int startTime;
    private int endTime;
    private float startAltitude;
    private float endAltitude;
    private float averageSpeed;
    private float totalDistance;
    private float linearDistance;
    private float maximumSpeed;
    private float maximumAltitude;
    private float minimumAltitude;
    private String notes;

    private TTLocation[] locations;
    private Waypoint[] waypoints;
    private Stop[] stops;

    private TTLocation[] checkpoints;

    private Map() {
    }

    public static final Map instanceOf(TTSQLiteOpenHelper sqLiteOpenHelper, int mapId) {
        Map map = new Map();
        SQLiteDatabase database = sqLiteOpenHelper.getReadableDatabase();
        Cursor cursor = database.query(TABLE_MAPS, ALL_MAP_COLUMNS, COLUMN_ID + " = " + mapId,
                null, null, null, null);
        cursor.moveToFirst();
        map.id = mapId;
        map.name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
        map.startTime = cursor.getInt(cursor.getColumnIndex(COLUMN_START_TIME));
        map.endTime = cursor.getInt(cursor.getColumnIndex(COLUMN_END_TIME));
        map.averageSpeed = cursor.getFloat(cursor.getColumnIndex(COLUMN_AVERAGE_SPEED));
        map.totalDistance = cursor.getFloat(cursor.getColumnIndex(COLUMN_TOTAL_DISTANCE));
        map.linearDistance = cursor.getFloat(cursor.getColumnIndex(COLUMN_LINEAR_DISTANCE));
        map.maximumSpeed = cursor.getFloat(cursor.getColumnIndex(COLUMN_MAXIMUM_SPEED));
        map.maximumAltitude = cursor.getFloat(cursor.getColumnIndex(COLUMN_MAX_ALTITUDE));
        map.minimumAltitude = cursor.getFloat(cursor.getColumnIndex(COLUMN_MIN_ALTITUDE));
        map.startAltitude = cursor.getFloat(cursor.getColumnIndex(COLUMN_START_ALTITUDE));
        map.endAltitude = cursor.getFloat(cursor.getColumnIndex(COLUMN_END_ALTITUDE));
        map.notes = cursor.getString(cursor.getColumnIndex(COLUMN_NOTES));
        cursor.close();

        map.locations = TTLocation.getAll(sqLiteOpenHelper, mapId);
        map.waypoints = Waypoint.getAll(sqLiteOpenHelper, mapId);
        map.stops = Stop.getAll(sqLiteOpenHelper, mapId);

        return map;
    }

    public void delete(TTSQLiteOpenHelper openHelper) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.delete(TABLE_STOPS,COLUMN_MAP_ID + " = " + id, null);
        db.delete(TABLE_WAYPOINTS, COLUMN_MAP_ID + " = " + id, null);
        db.delete(TABLE_LOCATIONS, COLUMN_MAP_ID + " = " + id, null);
        db.delete(TABLE_MAPS,COLUMN_ID + " = " + id, null);
    }

    public static PolylineOptions toNewPolyline(TTLocation[] locations) {
        PolylineOptions options = new PolylineOptions();
        for(int pointIndex = 0; pointIndex < locations.length; pointIndex++){
            options.add(locations[pointIndex].toLatLng());
        }
        return options;
    }

    public String getFormattedTime() {
        long millis = endTime-startTime;
        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    public Element getKMLElement(Document doc) {
        Element root = doc.createElement("kml");
        root.appendChild(doc.createElement("start").appendChild(doc.createTextNode("" + startTime)));
        Element trailPoints = doc.createElement("trail-points");
        StringBuilder ptsSB = new StringBuilder("");
        for (TTLocation loc : locations) {
            appendLocationString(ptsSB,loc);
        }
        trailPoints.appendChild(doc.createTextNode(ptsSB.toString()));
        root.appendChild(trailPoints);
        Element stops = doc.createElement("stops");
        StringBuilder stopsSB = new StringBuilder("");
        for (Stop stop : getStops()) {
            appendLocationString(stopsSB, stop.getStartLocation());
        }
        stops.appendChild(doc.createTextNode(stopsSB.toString()));
        root.appendChild(stops);
        Element waypointsEle = doc.createElement("waypoints");
        for (Waypoint wp : waypoints) {
            Element wpEle = doc.createElement("waypoint");
            wpEle.appendChild(doc.createElement("name").appendChild(doc.createTextNode(wp.getName())));
            StringBuilder wpLocSB = new StringBuilder("");
            appendLocationString(wpLocSB, wp.getLocation());
            wpEle.appendChild(doc.createElement("location").appendChild(doc.createTextNode(wpLocSB.toString())));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            wp.getImage().compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            Log.d("Map getKMLELement()", "jpg output:" + outputStream.toString());
            wpEle.appendChild(doc.createElement("image").appendChild(doc.createTextNode(outputStream.toString())));
            waypointsEle.appendChild(wpEle);
        }
        root.appendChild(waypointsEle);
        //TODO checkpoints
        root.appendChild(doc.createElement("map-title").appendChild(doc.createTextNode(name)));
        root.appendChild(doc.createElement("path-distance").appendChild(doc.createTextNode(
                Float.toString(totalDistance))));
        root.appendChild(doc.createElement("linear-distance").appendChild(doc.createTextNode(
                Float.toString(linearDistance))));
        root.appendChild(doc.createElement("average-speed").appendChild(doc.createTextNode(
                Float.toString(averageSpeed))));
        root.appendChild(doc.createElement("maximum-speed").appendChild(doc.createTextNode(
                Float.toString(maximumSpeed))));
        root.appendChild(doc.createElement("start-elevation").appendChild(doc.createTextNode(
                Float.toString(startAltitude))));
        root.appendChild(doc.createElement("end-elevation").appendChild(doc.createTextNode(
                Float.toString(endAltitude))));
        root.appendChild(doc.createElement("trip-time").appendChild(doc.createTextNode(
                Integer.toString((endTime - startTime) / 1000))));
        root.appendChild(doc.createElement("notes").appendChild(doc.createTextNode(notes)));
        return root;
    }

    /**
     * Convenience method for adding appending a location's information to a StringBuilder
     * for use in a KML export
     * @param stopsSB StringBuilder to which to append the location info
     * @param loc TTLocation from which to get info
     */
    private void appendLocationString(StringBuilder stopsSB, TTLocation loc) {
        stopsSB.append(loc.getLongitude()).append(",")
                .append(loc.getLatitude()).append(",")
                .append(loc.getAccuracy()).append(",")
                .append(loc.getAltitude()).append(",")
                .append(loc.getAccuracy()).append(",")
                .append(loc.getSpeed()).append(",")
                .append(loc.getTime()).append(",")
                .append(loc.getTime() / 1000).append(",")
                .append(loc.getDistance()).append(";").append("\n");
    }

    @Override
    public boolean equals(Object other) {
        return (other != null) && (other instanceof Map) && ((Map) other).getId() == getId();
    }

    @Override
    public int hashCode() {
        return getId();
    }


    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public float getAverageSpeed() {
        return averageSpeed;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public float getLinearDistance() {
        return linearDistance;
    }

    public float getMaximumSpeed() {
        return maximumSpeed;
    }

    public float getMaximumAltitude(){return maximumAltitude;}

    public float getMinimumAltitude(){return minimumAltitude;}

    public String getNotes() {
        return notes;
    }

    public TTLocation[] getLocations() {
        return locations;
    }

    /**
     * filters out nonessential points.
     * A point is nonessential if it affects the path taken by less than 5 meters
     * @return The checkpoints of the trail in this map
     */
    public TTLocation[] getCheckpoints() {
        //starting point always relevant//
        ArrayList<TTLocation> checkpointList = new ArrayList<TTLocation>();
        checkpointList.add(locations[0]);

        int relevantPointIndex = 0;
        for (int locationIndex = 2; locationIndex < locations.length; locationIndex++) {
            TTLocation ptInQuestion = locations[locationIndex - 1];
            TTLocation relevantPt = locations[relevantPointIndex];

            //distance from the last relevant to the point in question//
            float distanceTo = relevantPt
                    .distanceTo(ptInQuestion.getLongitude(), ptInQuestion.getLatitude());

            //if the point is less than 5m away, it can't affect the trail by more than 5m//
            if (distanceTo < 5) {
                continue;
            }

            //difference between the bearing from the relevant point to the point in question
            //and the bearing from the relevant point to the point after the point in question
            float bearingDifference = Math.abs(relevantPt.bearingTo(ptInQuestion.getLongitude(),
                    ptInQuestion.getLatitude()) - relevantPt.bearingTo(locations[locationIndex].getLongitude(),
                    locations[locationIndex].getLatitude()));

            //distance away from the point in question that the new path will take//
            double distanceOff = Math.sin(bearingDifference) * distanceTo;

            if (distanceOff > 5) {
                checkpointList.add(ptInQuestion);
                relevantPointIndex = locationIndex - 1;
            }
        }

        //the last point is always relevant and, as a result of the loop conditions,
        //never added by the loop//
        checkpointList.add(locations[locations.length - 1]);

        TTLocation[] array = new TTLocation[checkpointList.size()];
        return checkpointList.toArray(array);
    }

    public Waypoint[] getWaypoints() {
        return waypoints;
    }

    public Stop[] getStops() {
        return stops;
    }

    public float getStartAltitude() {
        return startAltitude;
    }

    public float getEndAltitude() {
        return endAltitude;
    }
}
