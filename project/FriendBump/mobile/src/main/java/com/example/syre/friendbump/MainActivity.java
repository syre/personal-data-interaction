package com.example.syre.friendbump;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
//For notification
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

public class MainActivity extends Activity implements OnMapReadyCallback,
                                                      LocationListener,
                                                      GoogleApiClient.ConnectionCallbacks,
                                                      GoogleApiClient.OnConnectionFailedListener,
                                                      MqttCallback {

    HashMap<String, Friend> FriendHashMap = new HashMap<>();
    HashMap<String, Friend> areaFriendHashMap = new HashMap<>();
    HashMap<String, Marker>  markers = new HashMap<>();
    ListView friendListView;
    FriendListAdapter friendListAdapter;
    MapView friendMapView;
    GoogleApiClient mGoogleApiClient;
    Boolean broadcastingEnabled;
    ArrayList<Friend> valuesList;
    ImageButton toggleBroadcastingButton;
    MqttClient mqttClient;
    GoogleMap mMap;
    MemoryPersistence persistence;
    String clientEmail;
    Location lastLocation = null;
    private static boolean isInForeground;
    Set notificationList = new HashSet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("onCreate", "onCreate executed!");
        setContentView(R.layout.activity_main);
        friendMapView = (MapView)findViewById(R.id.friendMapView);
        toggleBroadcastingButton = (ImageButton)findViewById(R.id.toggleBroadcastingButton);
        friendMapView.onCreate(savedInstanceState);
        FriendHashMap.put("handiiandii@gmail.com", new Friend("Anders Rahbek", 0.0, 0.0, "handiiandii@gmail.com"));
        FriendHashMap.put("syrelyre@gmail.com", new Friend("Søren Howe Gersager", 0.0, 0.0, "syrelyre@gmail.com"));

        friendListView = (ListView)findViewById(R.id.friendListView);
        valuesList = new ArrayList<>(areaFriendHashMap.values());
        friendListAdapter = new FriendListAdapter(this, valuesList, getResources(), friendListView);
        friendListView.setAdapter(friendListAdapter);
        MapsInitializer.initialize(this);
        friendMapView.getMapAsync(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .addApi(LocationServices.API)
                            .build();
        broadcastingEnabled = true;
        persistence = new MemoryPersistence();
        // find email of first account to use as id
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(getApplicationContext()).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                clientEmail = account.name;
                break;
            }
        }

        try {
            mqttClient = new MqttClient("tcp://syrelyre.dk:1883", clientEmail,persistence);
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setKeepAliveInterval(60);
            mqttClient.setCallback(this);
            mqttClient.connect();
            }
        catch(MqttException except)
        {
            Log.d("MainActivity mqtt:", except.getMessage());
            Toast.makeText(getApplicationContext(), "MQTT Connection failed: "+except.getMessage(), Toast.LENGTH_SHORT).show();
        }


    }
    @Override
    public void onStart()
    {
        super.onStart();
        mGoogleApiClient.connect();
        isInForeground = true;
        Log.d("MainActivity", "onStart executed!");
    }
    @Override
    public void onPause() {
        super.onPause();
        friendMapView.onPause();
        isInForeground = false;
        Log.d("MainActivity", "onPause executed!");
    }

    @Override
    public void onStop() {
        super.onStop();
        isInForeground = false;
        Log.d("MainActivity", "onStop executed!");
    }
    @Override
    public void onResume() {
        super.onResume();
        friendMapView.onResume();
        isInForeground = true;
        notificationList.clear();
        Log.d("MainActivity", "onResume executed!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        friendMapView.onDestroy();
        try{
            mqttClient.disconnect();
        }
        catch(MqttException except)
        {
            Log.d("MainActivity","MQTT: could not disconnect from broker");
        }
        isInForeground = false;
        Log.d("MainActivity", "onDestroy executed!");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        friendMapView.onLowMemory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return id == R.id.action_settings || super.onOptionsItemSelected(item);

    }

    @Override
    public void onMapReady(GoogleMap map) {
        // enables location marker and disables all gestures
        mMap = map;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(false);

        updateMarkers();

    }

    private double roundtoDecimals(int decimals, double value)
    {
        String pattern = "###.";
        for (int i = 0; i < decimals; i++)
            pattern += "#";
        DecimalFormat df2 = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
        df2.applyPattern(pattern);
        return Double.valueOf(df2.format(value));
    }

    private void subscribeToFriends(Location loc)
    {
        Iterator<String> friendListIterator = FriendHashMap.keySet().iterator();
        while (friendListIterator.hasNext())
        {
            String key = friendListIterator.next();
            String topic_string = FriendHashMap.get(key).getEmail()+"."+roundtoDecimals(3, loc.getLatitude())+"."+roundtoDecimals(3, loc.getLongitude());
            try {
                mqttClient.subscribe(topic_string);
            }
            catch(MqttException except)
            {
                Log.d("MainActivity", "MQTT: could not subscribe message: "+except.getMessage());
            }
        }

    }

    private void unsubscribeToFriends(Location loc)
    {
        Iterator<String> friendListIterator = FriendHashMap.keySet().iterator();
        while (friendListIterator.hasNext())
        {
            String key = friendListIterator.next();
            String topic_string = FriendHashMap.get(key).getEmail()+"."+roundtoDecimals(3, loc.getLatitude())+"."+roundtoDecimals(3, loc.getLongitude());
            try {
                mqttClient.unsubscribe(topic_string);
            }
            catch(MqttException except)
            {
                Log.d("MainActivity", "MQTT: could not subscribe message: "+except.getMessage());
            }
        }

    }
    // Sent when leaving an "area"
    private void sendNewAreaUpdate(Location loc)
    {
        String command = "loc_remove";
        String json_string = "{email:"+ clientEmail +
                ",command:"+command+"}";
        String topic =  clientEmail +"."+roundtoDecimals(3, loc.getLatitude())+"."+roundtoDecimals(3, loc.getLongitude());
        MqttMessage msg = new MqttMessage(json_string.getBytes());
        msg.setQos(1);
        try {
            mqttClient.publish(topic, msg);
        }
        catch(MqttException except)
        {
            Log.d("MainActivity", "MQTT: could not publish loc_removal message: "+except.getMessage());
        }
    }

    private void sendLocationChangeUpdate(Location loc)
    {
        Double latitude = loc.getLatitude();
        Double longitude = loc.getLongitude();
        String command = "loc_update";
        String json_string = "{email:"+ clientEmail +
                ",command:"+command+
                ",lat:"+latitude+
                ",lng:"+longitude+"}";
        String topic = clientEmail +"."+roundtoDecimals(3, loc.getLatitude())+"."+roundtoDecimals(3, loc.getLongitude());
        Log.d("MainActivity", "topic is: "+topic);
        MqttMessage msg = new MqttMessage(json_string.getBytes());
        msg.setQos(0);
        try {
            mqttClient.publish(topic,msg);
        }
        catch(MqttException except)
        {
            Log.d("MainActivity", "MQTT: could not send location message: "+except.getMessage() +" (" +except.getReasonCode() + ")");
            Toast.makeText(getApplicationContext(), "MQTT: could not send location message: "+except.getMessage() +" (" +except.getReasonCode() + ")", Toast.LENGTH_LONG).show();

        }
    }

    public void sendNudgeMessage(String targetName)
    {
        String command = "nudge";
        Friend friend = getFriendByName(targetName, areaFriendHashMap);
        if (friend != null) {
            String targetEmail = friend.getEmail();
            String json_string = "{email:" + clientEmail +
                    ",command:" + command + ",targetEmail:" + targetEmail + "}";
            String topic = clientEmail + "." + roundtoDecimals(3, lastLocation.getLatitude()) + "." + roundtoDecimals(3, lastLocation.getLongitude());
            MqttMessage msg = new MqttMessage(json_string.getBytes());
            msg.setQos(0);
            try {
                mqttClient.publish(topic, msg);
            } catch (MqttException except) {
                Log.d("MainActivity", "MQTT: could not publish nudge message: " + except.getMessage());
            }
        }
        else
            Log.d("sendNudgeMessage", "friend was null!");
    }
    public Friend getFriendByName(String name, HashMap map)
    {
        for (Object friend : map.values())
        {
            Friend friend1 = (Friend)friend;
            if (friend1.getName().equals(name))
                return friend1;

        }
        return null;
    }
    @Override
    public void onLocationChanged(Location loc)
    {
        LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
        mMap.animateCamera(zoom);

        Log.d("MainActivity","Location changed to: lat: "+loc.getLatitude()+", lng: "+loc.getLongitude());
        if (broadcastingEnabled)
        {   // if location when converted to accuracy of 110m (3 decimal places) has changed
            if (lastLocation == null || roundtoDecimals(3, loc.getLatitude()) != roundtoDecimals(3, lastLocation.getLatitude()) ||
                    roundtoDecimals(3, loc.getLongitude()) != roundtoDecimals(3, lastLocation.getLongitude()))
            {
                if (lastLocation != null)
                {
                    sendNewAreaUpdate(lastLocation);
                    unsubscribeToFriends(lastLocation);

                }
                areaFriendHashMap.clear();

                subscribeToFriends(loc);

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        valuesList.clear();
                        friendListAdapter.notifyDataSetChanged();
                        updateMarkers();
                    }

                });

                Log.d("MainActivity", "Sending new area update (loc_remove)");
                Toast.makeText(getApplicationContext(), "Sending new area update (loc_remove)", Toast.LENGTH_LONG).show();


            }
            lastLocation = loc;
            sendLocationChangeUpdate(loc);
        }

    }
    @Override public void onConnectionSuspended(int id)
    {

    }
    @Override public void onConnected(Bundle bundle)
    {
        Log.d("MainActivity", "device connected!");
        LocationRequest locationrequest = new LocationRequest();
        locationrequest.setInterval(10000);
        locationrequest.setFastestInterval(5000);
        locationrequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,locationrequest, this);
        Toast.makeText(getApplicationContext(), "Connection successful", Toast.LENGTH_LONG).show();
    }

    @Override public void onConnectionFailed(ConnectionResult result)
    {
        Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_LONG).show();
    }
    public void toggleBroadcasting(View v)
    {
        if (broadcastingEnabled)
        {
            toggleBroadcastingButton.setBackgroundResource(R.mipmap.broadcast_disabled);
        }
        else
        {
            toggleBroadcastingButton.setBackgroundResource(R.mipmap.broadcast_enabled);
        }
        broadcastingEnabled = !broadcastingEnabled;
    }

    @Override
    public void connectionLost(Throwable throwable) {
        try {
            mqttClient.connect();
        }
        catch(MqttException except)
        {
            Log.d("MainActivity", "MQTT: Connection Lost, reconnect failed: "+except.getMessage());
        }
    }

    private void parseCommand(JSONObject json_obj) {
        try {
            final String command = json_obj.getString("command");
            final String email = json_obj.getString("email");
            Log.d("parseCommand", "Command = " +command);
            Log.d("parseCommand", "email = " +email);
            if (command.equals("loc_update"))
            {
                Log.d("parseCommand", "log_update");
                final Double lat = json_obj.getDouble("lat");
                final Double lng = json_obj.getDouble("lng");
                if (areaFriendHashMap.get(email) == null)
                {
                    Log.d("parseCommand", "if null");
                    areaFriendHashMap.put(email, new Friend(FriendHashMap.get(email).getName(), lat, lng, email));
                }
                else
                {

                    Friend friend = areaFriendHashMap.get(email);
                    friend.setLat(lat);
                    friend.setLng(lng);
                }

                // update list view on UI thread
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        sendFriendNotification();
                    }

                });
            }
            else if (command.equals("loc_remove"))
            {
                if (areaFriendHashMap.get(email) != null)
                {
                    areaFriendHashMap.remove(email);

                }
            }
            else if (command.equals("nudge"))
            {
                Log.d("parseCommand", "Nudge!!");
                final String targetEmail = json_obj.getString("targetEmail");

                if (targetEmail.equals(clientEmail))
                {
                    Log.d("parseCommand", "Nudge!! - targetEmail == clientEmail!");
                    sendNudgeNotification(areaFriendHashMap.get(email));

                }
            }
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    valuesList.clear();
                    valuesList.addAll(areaFriendHashMap.values());
                    friendListAdapter.notifyDataSetChanged();
                    updateMarkers();
                }

            });
        }
        catch(Exception except) {
            Log.d("MainActivity", "MessageArrived exception: " + except.getMessage());
        }
    }
    private Bitmap drawMarkerBitmap(Context mContext,  int resourceId,  String mText)
    {
        Resources resources = mContext.getResources();
        float scale = resources.getDisplayMetrics().density;
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resourceId, options);
        bitmap = Bitmap.createScaledBitmap(bitmap, 80, 80, true);
        android.graphics.Bitmap.Config bitmapConfig =   bitmap.getConfig();
        // set default bitmap config if none
        if(bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are immutable,
        // so we need to convert it to mutable one
        bitmap = bitmap.copy(bitmapConfig, true);

        Canvas canvas = new Canvas(bitmap);
        // new antialised Paint
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // text color - #3D3D3D
        paint.setColor(Color.rgb(0,0, 0));
        // text size in pixels
        paint.setTextSize((int) (12 * scale));
        // optional - text shadow
        //paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY);

        // draw text to the Canvas center
        Rect bounds = new Rect();
        paint.getTextBounds(mText, 0, mText.length(), bounds);
        int x = (bitmap.getWidth() - bounds.width())/6;
        int y = (bitmap.getHeight() + bounds.height())/6;

        canvas.drawText(mText, x * scale, y * scale, paint);
        return bitmap;
    }
    private String extractInitials(String text)
    {
        String[] splitarray = text.split("\\s+");
        String extracted = "";
        for (String value: splitarray)
        {
            extracted += value.substring(0,1);
        }
        return extracted;
    }
    private void updateMarkers()
    {
        Iterator<String> friendListIterator = areaFriendHashMap.keySet().iterator();
        while(friendListIterator.hasNext())
        {
            String key = friendListIterator.next();
            if(markers.get(key) == null) //if there is no marker for the friend
            {
                String initials = extractInitials(areaFriendHashMap.get(key).getName());
                Bitmap markerBitmap = drawMarkerBitmap(getApplicationContext(),R.drawable.circle,initials);
                BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(markerBitmap);
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(areaFriendHashMap.get(key).getLat(), areaFriendHashMap.get(key).getLng()))
                        .title(areaFriendHashMap.get(key).getName()).icon(descriptor));

                markers.put(key, marker);
                notificationList.add(key);
            }
            else
            {
                LatLng old_loc = markers.get(key).getPosition();
                LatLng new_loc = new LatLng(areaFriendHashMap.get(key).getLat(), areaFriendHashMap.get(key).getLng());
                markers.get(key).setPosition(new_loc);
                float[] result = new float[1];
                Location.distanceBetween(old_loc.latitude, old_loc.longitude, new_loc.latitude, new_loc.longitude, result);
                if(result[0] >100) {
                        notificationList.add(key);
                }
            }
        }
        Iterator<String> markerIterator = markers.keySet().iterator();
        while (markerIterator.hasNext())
        {
            String key = markerIterator.next();
            if(areaFriendHashMap.get(key) == null) //Friend for Marker m, doesn't exist in Marker
            {
                markers.get(key).remove();
                markerIterator.remove();

            }
        }

    }


    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception
    {
        String msg = new java.lang.String(mqttMessage.getPayload());
        Log.d("MainActivity", "message arrived:" + msg);
        try {
            JSONObject json_obj = new JSONObject(msg);
            if (!json_obj.getString("email").equals(clientEmail))
                parseCommand(json_obj);

        }
        catch(Exception except) {
            Log.d("MainActivity", "MessageArrived exception: " + except.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken)
    {

    }

    public void sendNudgeNotification(Friend fromFriend)
    {
        Log.d("sendNudgeNotification", "sendNudgeNotification here!!");
        String title = "You have got nudged!";
        if(fromFriend == null)
            Log.d("sendNudgeNotification", "fromFriend is NULL!");

        String contentText = fromFriend.getName() + " is near you and wants to meet!";
        int mId = 02;
        Log.d("sendNudgeNotification", "contentText = " + contentText);
        Intent resultIntent = new Intent(this,
                MainActivity.class);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.broadcast_disabled)
                        .setContentTitle(title)
                        //.setContentText(contentText)
                        .setContentIntent(viewPendingIntent)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(contentText));
        mBuilder.setAutoCancel(true);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(alarmSound);
        mBuilder.setVibrate(new long[] { 0, 100, 100, 100, 100 });

        NotificationManagerCompat mNotificationManager =
                NotificationManagerCompat.from(this);
// mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mBuilder.build());
    }

    private void sendFriendNotification()
    {
        Log.d("Notification", "Run notification() ");
        if(!isInForeground && notificationList.size()>0)
        {
            if(notificationList.contains(clientEmail))
                notificationList.remove(clientEmail);
            String title;
            String contentText = "";
            Log.d("Notification", "notiList.size() = "+notificationList.size());
                int i = 0;
                for(Object name : notificationList)
                {
                    if(i==3)
                        break;
                    contentText += areaFriendHashMap.get(name).getName() + ", ";
                    i++;
                }
                contentText = contentText.substring(0, contentText.length()-2);
                if(i == 1) {
                    Iterator iter = notificationList.iterator();

                    Object name = iter.next();
                    title = "There is 1 friend near you!";
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    Log.d("Notification", "String name = " + areaFriendHashMap.get((String) name).getName());
                    String number = getNumber(areaFriendHashMap.get((String) name).getName());
                    callIntent.setData(Uri.parse("tel:" + number));
                    PendingIntent callPendingIntent = PendingIntent.getActivity(this, 0, callIntent, 0);

                    Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", number, ""));
                    PendingIntent smsPendingIntent = PendingIntent.getActivity(this, 0, smsIntent, 0);

                    int mId = 01;

                    Intent resultIntent = new Intent(this,
                            MainActivity.class);
                    PendingIntent viewPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(this)
                                    .setSmallIcon(R.mipmap.broadcast_disabled)
                                    .setContentTitle(title)
                                    //.setContentText(contentText)
                                    .setStyle(new NotificationCompat.BigTextStyle()
                                            .bigText(contentText))
                                    .setContentIntent(viewPendingIntent)
                                    .addAction(R.drawable.phone_notification, "Call", callPendingIntent)
                                    .addAction(R.drawable.chat_notification, "SMS", smsPendingIntent);
                    mBuilder.setAutoCancel(true);
                    Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    mBuilder.setSound(alarmSound);
                    mBuilder.setVibrate(new long[] { 0, 100, 100, 100, 100 });

                    NotificationManagerCompat mNotificationManager =
                            NotificationManagerCompat.from(this);
// mId allows you to update the notification later on.
                    mNotificationManager.notify(mId, mBuilder.build());
                }
                else {
                    title = "There is several friends near you!";
                }


            notificationList.clear();
            Log.d("Notification", "Notification send!");






        }
        else
        {
            Log.d("Notification", "Activity is in foreground. No notification send");
            notificationList.clear();
        }
        notificationList.clear();
    }


    public String getNumber(String qName) {
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER};

        Cursor people = getContentResolver().query(uri, projection, null, null, null);

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
}
