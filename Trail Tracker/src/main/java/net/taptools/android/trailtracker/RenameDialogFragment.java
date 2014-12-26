package net.taptools.android.trailtracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link RenameDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class RenameDialogFragment extends DialogFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_TITLE = "title";

    public interface RenameListener {
        public void onRename(String newName);
    }

    private RenameListener listener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param previousTitle previous title of map
     * @return A new instance of fragment RenameDialogFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RenameDialogFragment newInstance(String previousTitle, RenameListener renameListener) {
        RenameDialogFragment fragment = new RenameDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, previousTitle);
        fragment.listener = renameListener;
        fragment.setArguments(args);
        return fragment;
    }
    public RenameDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final EditText textView = new EditText(getActivity());
        textView.setText(getArguments().getString(ARG_TITLE));
        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_action_edit)
                .setTitle("Rename")
                .setView(textView)
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onRename(textView.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setMessage("Choose a new name")
                .create();
    }
}
