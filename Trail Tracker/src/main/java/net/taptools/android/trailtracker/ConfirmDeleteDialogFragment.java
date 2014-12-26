package net.taptools.android.trailtracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import net.taptools.android.trailtracker.Models.Map;
import net.taptools.android.trailtracker.Results.ResultsFragment;

import java.util.ArrayList;

/**
 * Created by Brian on 12/26/2014.
 */
public class ConfirmDeleteDialogFragment extends DialogFragment {

    private ArrayList<Map> activeMaps;
    private OnMapDeletedListener listener;

    public static ConfirmDeleteDialogFragment newInstance(ArrayList<Map> activeMaps,
                                                          OnMapDeletedListener listener) {
        ConfirmDeleteDialogFragment frag = new ConfirmDeleteDialogFragment();
        frag.activeMaps = activeMaps;
        frag.listener = listener;
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Are you sure you want to delete map(s)")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        for (Map map : activeMaps) {
                            map.delete(((MyApplication) getActivity().getApplication())
                                    .getDatabaseHelper());
                            listener.onMapDeleted();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }

    public interface OnMapDeletedListener {
        public void onMapDeleted();
    }
}
