package com.example.syre.friendbump;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
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
    private ListView listView;
    public Resources res;
    private static LayoutInflater inflater = null;

    public FriendListAdapter(Activity a, ArrayList l, Resources r, ListView lv)
    {
        activity = a;
        list = l;
        res = r;
        listView = lv;
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
            holder.toolbar.setTag(position);
            holder.phoneButton = (ImageView) view.findViewById(R.id.phone_button);
            holder.phoneButton.setOnClickListener(this);
            holder.phoneButton.setTag(position);

            holder.chatButton = (ImageView) view.findViewById(R.id.chat_button);
            holder.chatButton.setOnClickListener(this);
            holder.chatButton.setTag(position);

            holder.nudgeButton = (ImageView) view.findViewById(R.id.nudge_button);
            holder.nudgeButton.setOnClickListener(this);
            holder.nudgeButton.setTag(position);

            view.setTag(holder);
        }
        else
            holder = (ViewHolder) view.getTag();

        if (list.size() > 0)
        {
            Friend f = (Friend) list.get(position);
            holder.name.setText(f.getName());
            holder.distance.setText(String.format("%.2f",f.getDistance())+"m");

            holder.profileImage.setImageResource(res.getIdentifier("com.example.syre.friendbump:drawable/person_placeholder",null,null));
        }
        return view;
    }
    @Override
    public void onClick(View view)
    {
        int position =(int)view.getTag();
        switch(view.getId())
        {
        case R.id.chat_button:
            try {
                String number = getNumber(list.get(position).toString());  // The number to send SMS to
                if(!number.equals("NULL")) {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", number, "")));
                }
                else
                    Toast.makeText(activity.getApplicationContext(), "Number not found. Can't send a sms!", Toast.LENGTH_SHORT).show();
            }
            catch (Error error){
                Toast.makeText(activity.getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
            break;

        case R.id.nudge_button:
            ((MainActivity)activity).sendNudgeMessage((Friend) list.get(position));//mainActivity.
            break;
        case R.id.phone_button:
            String number = getNumber(list.get(position).toString());
            if(!number.equals("NULL")) {
                try {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + getNumber(list.get(position).toString())));
                    activity.startActivity(callIntent);
                } catch (ActivityNotFoundException activityException) {
                    Toast.makeText(activity.getApplicationContext(), "Error!", Toast.LENGTH_SHORT).show();
                    Log.e("Calling a Phone Number", "Call failed", activityException);
                }
            }
            else
                Toast.makeText(activity.getApplicationContext(), "Number not found. Can't make the call!", Toast.LENGTH_SHORT).show();
            break;
        default:
            break;
        }

    }
    public String getNumber(String qName) {
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER};

        Cursor people = activity.getContentResolver().query(uri, projection, null, null, null);

        int indexName = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int indexNumber = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        String resultNumber = "NULL";
        people.moveToFirst();
        do {
            String name = people.getString(indexName);
            String number = people.getString(indexNumber);

            if(qName.equals(name)) {
                resultNumber = number;
                break;
            }
        } while (people.moveToNext());
        return resultNumber;
    }


    public void hideAllOtherToolbars(int position)
    {
        final int childCount = listView.getChildCount();
        for (int i = 0; i < childCount; i++)
        {
            if (i == position)
                continue;

            View child = listView.getChildAt(i);
            TableRow toolbar = (TableRow)child.findViewById(R.id.toolbar_row);
            toolbar.setVisibility(View.GONE);
        }
    }
    public class ToggleToolbarListener implements View.OnClickListener
    {
       @Override
        public void onClick(View v) {
           View parent = (View)v.getParent();
            final TableRow toolbar = (TableRow)parent.findViewById(R.id.toolbar_row);
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
                int position = (int)toolbar.getTag();
                hideAllOtherToolbars(position);
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
        public ImageView phoneButton;
        public ImageView chatButton;
        public ImageView nudgeButton;
    }
}
