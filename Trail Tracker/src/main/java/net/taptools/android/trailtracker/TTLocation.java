package net.taptools.android.trailtracker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import static net.taptools.android.trailtracker.TTSQLiteOpenHelper.*;

/**
 * Created by Brian Murphy on 5/6/2014.
 */
public class TTLocation {

    private long id;
    private float longitude;
    private float latitude;
    private float speed;
    private float elevation;
    private float accuracy;
    private float bearing;
    private float distance;
    private long time;
    private long mapId;

    // Cache the inputs and outputs of computeDistanceAndBearing
    // so calls to distanceTo() and bearingTo() can share work
    private double mLat1 = 0.0;
    private double mLon1 = 0.0;
    private float mDistance = 0.0f;
    private float mInitialBearing = 0.0f;
    // Scratchpad
    private final float[] mResults = new float[2];


    public static final TTLocation instanceOf(TTSQLiteOpenHelper sqLiteOpenHelper, long locId){
        TTLocation loc = new TTLocation();
        SQLiteDatabase database = sqLiteOpenHelper.getReadableDatabase();
        Cursor crsr = database.query(TABLE_LOCATIONS,ALL_LOCATION_COLUMNS,COLUMN_ID+" = "+locId,
                null,null,null,null);
        crsr.moveToFirst();
        loc.id = locId;
        loc.longitude = crsr.getFloat(crsr.getColumnIndex(COLUMN_LONGITUDE));
        loc.latitude = crsr.getFloat(crsr.getColumnIndex(COLUMN_LATITUDE));
        loc.speed = crsr.getFloat(crsr.getColumnIndex(COLUMN_SPEED));
        loc.elevation = crsr.getFloat(crsr.getColumnIndex(COLUMN_ELEVATION));
        loc.accuracy = crsr.getFloat(crsr.getColumnIndex(COLUMN_ACCURACY));
        loc.bearing = crsr.getFloat(crsr.getColumnIndex(COLUMN_BEARING));
        loc.distance = crsr.getFloat(crsr.getColumnIndex(COLUMN_DISTANCE));
        loc.time = crsr.getLong(crsr.getColumnIndex(COLUMN_TIME));
        loc.mapId = crsr.getLong(crsr.getColumnIndex(COLUMN_MAP_ID));
        crsr.close();
        return loc;
    }

    public static final TTLocation[] getAll(TTSQLiteOpenHelper sqLiteOpenHelper, long mapId){
        SQLiteDatabase database = sqLiteOpenHelper.getReadableDatabase();
        Cursor crsr = database.query(TABLE_LOCATIONS,ALL_LOCATION_COLUMNS,COLUMN_MAP_ID+" = "+mapId,
                null,null,null,COLUMN_TIME+" ASC");
        crsr.moveToFirst();
        TTLocation[] locs = new TTLocation[crsr.getCount()];
        for(int locIndex = 0; !crsr.isAfterLast();locIndex++){
            TTLocation loc = new TTLocation();
            loc.id = crsr.getLong(crsr.getColumnIndex(COLUMN_ID));
            loc.longitude = crsr.getFloat(crsr.getColumnIndex(COLUMN_LONGITUDE));
            loc.latitude = crsr.getFloat(crsr.getColumnIndex(COLUMN_LATITUDE));
            loc.speed = crsr.getFloat(crsr.getColumnIndex(COLUMN_SPEED));
            loc.elevation = crsr.getFloat(crsr.getColumnIndex(COLUMN_ELEVATION));
            loc.accuracy = crsr.getFloat(crsr.getColumnIndex(COLUMN_ACCURACY));
            loc.bearing = crsr.getFloat(crsr.getColumnIndex(COLUMN_BEARING));
            loc.distance = crsr.getFloat(crsr.getColumnIndex(COLUMN_DISTANCE));
            loc.time = crsr.getLong(crsr.getColumnIndex(COLUMN_TIME));
            loc.mapId = crsr.getLong(crsr.getColumnIndex(COLUMN_MAP_ID));
            locs[locIndex]=loc;
            crsr.moveToNext();
        }

        crsr.close();
        return locs;
    }

    public LatLng toLatLng(){
        return new LatLng(latitude,longitude);
    }

    public long getId() {
        return id;
    }

    public float getLongitude() {
        return longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getSpeed() {
        return speed;
    }

    public float getElevation() {
        return elevation;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public float getBearing() {
        return bearing;
    }

    public float getDistance() {
        return distance;
    }

    public long getTime() {
        return time;
    }

    public long getMapId() {
        return mapId;
    }

    public float bearingHere(double lon, double lat) {
        synchronized (mResults) {
            // See if we already have the result
            if (lat != mLat1 || lon != mLon1 ) {
                computeDistanceAndBearing(lat, lon,
                        latitude, longitude, mResults);
                mLat1 = lat;
                mLon1 = lon;
                mDistance = mResults[0];
                mInitialBearing = mResults[1];
            }
            return mInitialBearing;
        }
    }

    public float distanceTo(double lon, double lat) {
        // See if we already have the result
        synchronized (mResults) {
            if (lat != mLat1 || lon != mLon1) {
                computeDistanceAndBearing(lat, lon,
                        latitude, longitude, mResults);
                mLat1 = lat;
                mLon1 = lon;
                mDistance = mResults[0];
                mInitialBearing = mResults[1];
            }
            return mDistance;
        }
    }

    private static void computeDistanceAndBearing(double lat1, double lon1,
                                                  double lat2, double lon2, float[] results) {
        // Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
        // using the "Inverse Formula" (section 4)

        int MAXITERS = 20;
        // Convert lat/long to radians
        lat1 *= Math.PI / 180.0;
        lat2 *= Math.PI / 180.0;
        lon1 *= Math.PI / 180.0;
        lon2 *= Math.PI / 180.0;

        double a = 6378137.0; // WGS84 major axis
        double b = 6356752.3142; // WGS84 semi-major axis
        double f = (a - b) / a;
        double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

        double L = lon2 - lon1;
        double A = 0.0;
        double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
        double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

        double cosU1 = Math.cos(U1);
        double cosU2 = Math.cos(U2);
        double sinU1 = Math.sin(U1);
        double sinU2 = Math.sin(U2);
        double cosU1cosU2 = cosU1 * cosU2;
        double sinU1sinU2 = sinU1 * sinU2;

        double sigma = 0.0;
        double deltaSigma = 0.0;
        double cosSqAlpha = 0.0;
        double cos2SM = 0.0;
        double cosSigma = 0.0;
        double sinSigma = 0.0;
        double cosLambda = 0.0;
        double sinLambda = 0.0;

        double lambda = L; // initial guess
        for (int iter = 0; iter < MAXITERS; iter++) {
            double lambdaOrig = lambda;
            cosLambda = Math.cos(lambda);
            sinLambda = Math.sin(lambda);
            double t1 = cosU2 * sinLambda;
            double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
            double sinSqSigma = t1 * t1 + t2 * t2; // (14)
            sinSigma = Math.sqrt(sinSqSigma);
            cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
            sigma = Math.atan2(sinSigma, cosSigma); // (16)
            double sinAlpha = (sinSigma == 0) ? 0.0 :
                    cosU1cosU2 * sinLambda / sinSigma; // (17)
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
            cos2SM = (cosSqAlpha == 0) ? 0.0 :
                    cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

            double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
            A = 1 + (uSquared / 16384.0) * // (3)
                    (4096.0 + uSquared *
                            (-768 + uSquared * (320.0 - 175.0 * uSquared)));
            double B = (uSquared / 1024.0) * // (4)
                    (256.0 + uSquared *
                            (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
            double C = (f / 16.0) *
                    cosSqAlpha *
                    (4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
            double cos2SMSq = cos2SM * cos2SM;
            deltaSigma = B * sinSigma * // (6)
                    (cos2SM + (B / 4.0) *
                            (cosSigma * (-1.0 + 2.0 * cos2SMSq) -
                                    (B / 6.0) * cos2SM *
                                            (-3.0 + 4.0 * sinSigma * sinSigma) *
                                            (-3.0 + 4.0 * cos2SMSq)));

            lambda = L +
                    (1.0 - C) * f * sinAlpha *
                            (sigma + C * sinSigma *
                                    (cos2SM + C * cosSigma *
                                            (-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

            double delta = (lambda - lambdaOrig) / lambda;
            if (Math.abs(delta) < 1.0e-12) {
                break;
            }
        }

        float distance = (float) (b * A * (sigma - deltaSigma));
        results[0] = distance;
        if (results.length > 1) {
            float initialBearing = (float) Math.atan2(cosU2 * sinLambda,
                    cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
            initialBearing *= 180.0 / Math.PI;
            results[1] = initialBearing;
            if (results.length > 2) {
                float finalBearing = (float) Math.atan2(cosU1 * sinLambda,
                        -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
                finalBearing *= 180.0 / Math.PI;
                results[2] = finalBearing;
            }
        }
    }
}
