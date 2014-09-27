package net.taptools.android.trailtracker;

import android.app.Application;

/**
 * Created by Brian on 7/22/2014.
 */
public class MyApplication extends Application {

    private TTSQLiteOpenHelper ttsqLiteOpenHelper;

    public TTSQLiteOpenHelper getDatabaseHelper(){
        if(ttsqLiteOpenHelper == null){
            ttsqLiteOpenHelper = new TTSQLiteOpenHelper(this);
        }
        return ttsqLiteOpenHelper;
    }
}
