package com.example.syre.friendbump;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by syre on 4/18/15.
 */
public class FriendListAdapter extends BaseAdapter implements View.OnClickListener
{
    private Activity activity;
    private ArrayList list;
    public Resources res;
    private static LayoutInflater inflater = null;

    public FriendListAdapter(Activity a, ArrayList l, Resources r)
    {
        activity = a;
        list = l;
        res = r;
        inflater = (LayoutInflater )activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;
        if (convertView == null)
        {
            view = inflater.inflate(R.layout.friendlist_item, null);
            TableRow listItemRow = (TableRow) view.findViewById(R.id.list_item_row);
            listItemRow.setOnClickListener(new ToggleToolbarListener());
            holder = new ViewHolder();
            holder.name = (TextView) view.findViewById(R.id.friend_name);
            holder.profileImage = (ImageView) view.findViewById(R.id.friend_image);
            holder.distance = (TextView) view.findViewById(R.id.friend_distance);
            holder.socialNetworkImage = (ImageView) view.findViewById(R.id.friend_social_network_image);
            holder.toolbar = (TableRow) view.findViewById(R.id.toolbar_row);
            view.setTag(holder);
        }
        else
            holder = (ViewHolder) view.getTag();

        if (list.size() > 0)
        {
            Friend f = (Friend) list.get(position);
            holder.name.setText(f.getName());
            holder.distance.setText(f.getEmail());

            holder.profileImage.setImageResource(res.getIdentifier("com.example.syre.friendbump:drawable/person_placeholder",null,null));
        }
        view.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId())
        {
            case R.id.friend_image:
                break;

            case R.id.friend_name:
                Toast.makeText(activity.getApplicationContext(), "name clicked", Toast.LENGTH_SHORT).show();
            default:
                break;
        }
    }
    private class ToggleToolbarListener implements View.OnClickListener
    {

        @Override
        public void onClick(View v)
        {
            TableRow toolbar = (TableRow) v.findViewById(R.id.toolbar_row);
            if (toolbar.isShown())
            {
                Animation out = AnimationUtils.makeOutAnimation(activity.getApplicationContext(), true);
                toolbar.startAnimation(out);
                toolbar.setVisibility(View.GONE);
            }
            else
            {
                Animation in = AnimationUtils.makeInAnimation(activity.getApplicationContext(), true);
                toolbar.startAnimation(in);
                toolbar.setVisibility(View.VISIBLE);
            }

        }
    }

    public static class ViewHolder
    {
        public TextView name;
        public ImageView profileImage;
        public ImageView socialNetworkImage;
        public TextView distance;
        public TableRow toolbar;
    }
}
