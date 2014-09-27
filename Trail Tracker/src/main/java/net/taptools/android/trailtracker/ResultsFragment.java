package net.taptools.android.trailtracker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import static net.taptools.android.trailtracker.TTSQLiteOpenHelper.*;


public class ResultsFragment extends Fragment implements RenameDialogFragment.RenameListener {
    private MultiPickerFragment pickerFragment;
    private ArrayList<Map> activeMaps;

    public static ResultsFragment newInstance() {
        ResultsFragment fragment = new ResultsFragment();
        return fragment;
    }

    public ResultsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.results,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_delete:
                if(activeMaps.size()>0) {
                    new ConfirmDeleteDialogFragment().show(getFragmentManager(),"deleteMapFrag");
                }else {
                    Toast.makeText(getActivity(), "Select at least one map", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_rename:
                if(activeMaps.size()==1) {
                    RenameDialogFragment.newInstance(activeMaps.get(0).getName(), this)
                            .show(getFragmentManager(), "renameDialog");
                }else{
                    Toast.makeText(getActivity(),"Select one map",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_share:
                if(activeMaps.size()==1){

                }
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_results, container, false);

        pickerFragment = MultiPickerFragment.newInstance(this);
        getFragmentManager().beginTransaction()
                .replace(R.id.resultsFrame,pickerFragment)
                .commit();
        return root;
    }

    public ArrayList<Map> getActiveMaps(){
        return activeMaps;
    }

    public void setActiveMaps(ArrayList<Map> maps){
        activeMaps = maps;
    }

    @Override
    public void onRename(String newName) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, newName);
        SQLiteDatabase db = ((MyApplication)getActivity().getApplication()).getDatabaseHelper()
                .getWritableDatabase();
        db.update(TABLE_MAPS, values, COLUMN_ID + " = " + activeMaps.get(0).getId(), null);
        pickerFragment.updateList();
    }

    @SuppressLint("ValidFragment")
    private class ConfirmDeleteDialogFragment extends DialogFragment {
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
                            }
                            pickerFragment.updateList();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            return builder.create();
        }
    }
}
