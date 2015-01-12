package net.taptools.android.trailtracker;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by Brian Murphy on 5/27/2014.
 */
public class CheckableExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;

    private volatile ArrayList<String> categoryNames;
    private volatile LinkedHashMap<String, ArrayList<String>> childrenGroups;
    private volatile ArrayList<Integer> ids;

    private ArrayList<Integer> groupSizes;
    private ArrayList<String> allChildren;
    private boolean[] enabledChildren;

    public CheckableExpandableListAdapter(Context context, ArrayList<String> listDataHeader,
                                          LinkedHashMap<String, ArrayList<String>> listChildData,
                                          ArrayList<Integer> ids) {
        this.context = context;
        childrenGroups = listChildData;
        Log.d("Checkable", "childrenGroups size: " + childrenGroups.size());
        Log.d("Checkable", "childrenGroups sizeOfFirst: " + childrenGroups.get(childrenGroups.get(0)));
        categoryNames = listDataHeader;
        this.ids = ids;

        groupSizes = new ArrayList<Integer>();

        //Initialize and fill allChildren ArrayList//
        allChildren = new ArrayList<String>();
        for (int groupIndex = 0; groupIndex < categoryNames.size(); groupIndex++) {
            ArrayList<String> group = childrenGroups.get(categoryNames.get(groupIndex));
            for (int childIndex = 0; childIndex < group.size(); childIndex++) {
                allChildren.add((String) getChild(groupIndex, childIndex));
                groupSizes.add(groupIndex, childIndex + 1);
            }
        }
        enabledChildren = new boolean[allChildren.size()];
        Log.d("CheckableExpandableListAdapter", "ctor called, enabledChildrenset");
    }

    @Override
    public void notifyDataSetChanged() {
        //Initialize and fill allChildren ArrayList//
        Log.d("Checkable#notfyDataSetChanged()", "enabledChildred size: " + enabledChildren.length);
        allChildren = new ArrayList<String>();
        groupSizes = new ArrayList<Integer>();
        for (int groupIndex = 0; groupIndex < categoryNames.size(); groupIndex++) {
            ArrayList<String> group = childrenGroups.get(categoryNames.get(groupIndex));
            for (int childIndex = 0; childIndex < group.size(); childIndex++) {
                allChildren.add((String) getChild(groupIndex, childIndex));
                groupSizes.add(groupIndex, childIndex + 1);
            }
        }
        enabledChildren = new boolean[allChildren.size()];

        //now that things are re-set up//
        super.notifyDataSetChanged();
    }

    /**
     * allows the containing Fragment to set which data points should
     * be displayed based on ListView input.
     * @param id id of map entry to be displayed as checked
     * @param enabled true to enable; false to disable
     */
    public void setChildChecked(int id, boolean enabled) {
        Log.d("Checkable#setChildChecked", "id: " + id + " enabled: " + enabled);
        for (int idIndex = 0; idIndex < ids.size(); idIndex++) {
            if (ids.get(idIndex) == id) {
                enabledChildren[idIndex] = enabled;
                break;
            }
        }
    }

    /**
     *
     * @param groupPosition the category index in which the child resides
     * @param childPosition the index within the group of the child
     * @return child object (String representing data point)
     */
    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return  childrenGroups.get(categoryNames.get(groupPosition)).get(childPosition);
    }

    public int getChildIndex(int groupPosition, int childPosition) {
        int index = 0;
        for (int groupIndex = 0; groupIndex<groupPosition; groupIndex++) {
            index += groupSizes.get(groupIndex);
        }
        return index + childPosition;
    }

    /**
     *
     * @param groupPosition the category index in which the child resides
     * @param childPosition the index within the group of the child
     * @return the id of the child element as designated by the ids array
     */
    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return ids.get(getChildIndex(groupPosition, childPosition));
    }

    /**
     * used by ArrayList to display itself
     * @param groupPosition the category index in which the child resides
     * @param childPosition the index within the group of the child
     * @param isLastChild not used by local code
     * @param convertView view that is used to display data point in ListView
     * @param parent not used by local code
     * @return View that will be displayed in ArrayList
     */
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        final String childText = (String) getChild(groupPosition, childPosition);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.checkable_expandable_list_view_child, null);
        }
        CheckedTextView textView = (CheckedTextView) convertView.findViewById(android.R.id.text1);
        textView.setText(childText);
        textView.setChecked(enabledChildren[getChildIndex(groupPosition, childPosition)]);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return childrenGroups.get(categoryNames.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return categoryNames.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return categoryNames.size();
    }

     @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    /**
     * used by ListView to display itself
     * @param groupPosition index of group
     * @param isExpanded  not used by local code
     * @param convertView View used to display category heading
     * @param parent not used by local code
     * @return View that the ListView uses to display itself
     */
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(android.R.layout.simple_expandable_list_item_2, null);
        }
        TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
        textView.setText(headerTitle);
        ((TextView) convertView.findViewById(android.R.id.text2))
                .setText(getChildrenCount(groupPosition) + " Maps");
        return convertView;
    }

    /**
     *
     * @return true, because we designate ids based on position in the ArrayList
     * allChildren
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     *
     * @return true, because the views must be clicked to disable or
     * enable data points
     */
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
