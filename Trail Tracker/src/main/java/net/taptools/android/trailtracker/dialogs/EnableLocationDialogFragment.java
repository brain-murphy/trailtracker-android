package net.taptools.android.trailtracker.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import net.taptools.android.trailtracker.R;
import net.taptools.android.trailtracker.TrackTrailFragment;

/**
 * Created by Brian on 7/29/2014.
 */
public class EnableLocationDialogFragment extends DialogFragment{

    public interface OnGPSCancelListener{
        public void onGPSCancel();
    }

    private OnGPSCancelListener listener;

    public static EnableLocationDialogFragment newInstance(OnGPSCancelListener listener){
        EnableLocationDialogFragment fragment = new EnableLocationDialogFragment();
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle("GPS service required");
        alertDialog.setMessage("Please Enable GPS");
        alertDialog.setMessage("Please Enable GPS");
        alertDialog.setPositiveButton( "Settings",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent gpsSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        getActivity().startActivityForResult(gpsSettingsIntent, TrackTrailFragment.GPS_REQUEST_CODE);
                    }
                }
        );
        alertDialog.setNeutralButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onGPSCancel();
                    }
                }
        );
        alertDialog.setIcon(R.drawable.ic_action_location_found);
        return alertDialog.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        listener.onGPSCancel();
    }
}
