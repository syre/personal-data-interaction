package com.example.syre.friendbump;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
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


public class MainActivity extends Activity implements OnMapReadyCallback,
                                                      LocationListener,
                                                      GoogleApiClient.ConnectionCallbacks,
                                                      GoogleApiClient.OnConnectionFailedListener,
                                                      MqttCallback {

    HashMap<String, Friend> friendHashMap = new HashMap<>();
    HashMap<String, Marker>  markers = new HashMap<>();
    ListView friendListView;
    FriendListAdapter friendListAdapter;
    MapView friendMapView;
    GoogleApiClient mGoogleApiClient;
    Boolean broadcastingEnabled;
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
        friendHashMap.put("handiiandii@gmail.com", new Friend("Anders Rahbek", 0.0, 0.0, "handiiandii@gmail.com"));
        friendHashMap.put("syrelyre@gmail.com", new Friend("Søren Howe Gersager", 0.0, 0.0, "syrelyre@gmail.com"));

        friendListView = (ListView)findViewById(R.id.friendListView);
        ArrayList<Friend> valuesList = new ArrayList<>(friendHashMap.values());
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
        Log.d("onResume", "onResume executed!");
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

    private double roundtoThreeDecimals(double value)
    {
        DecimalFormat df2 = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
        df2.applyPattern("###.###");
        return Double.valueOf(df2.format(value));
    }

    private void subscribeToFriends(Location loc)
    {
        Iterator<String> friendListIterator = friendHashMap.keySet().iterator();
        while (friendListIterator.hasNext())
        {
            String key = friendListIterator.next();
            String topic_string = friendHashMap.get(key).getEmail()+"."+roundtoThreeDecimals(loc.getLatitude())+"."+roundtoThreeDecimals(loc.getLongitude());
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
        Iterator<String> friendListIterator = friendHashMap.keySet().iterator();
        while (friendListIterator.hasNext())
        {
            String key = friendListIterator.next();
            String topic_string = friendHashMap.get(key).getEmail()+"."+roundtoThreeDecimals(loc.getLatitude())+"."+roundtoThreeDecimals(loc.getLongitude());
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
        String command = "loc_removal";
        String json_string = "{email:"+ clientEmail +
                ",command:"+command+"}";
        String topic =  clientEmail +"."+roundtoThreeDecimals(loc.getLatitude())+"."+roundtoThreeDecimals(loc.getLongitude());
        MqttMessage msg = new MqttMessage(json_string.getBytes());
        msg.setQos(0);
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
        String topic = clientEmail +"."+roundtoThreeDecimals(loc.getLatitude())+"."+roundtoThreeDecimals(loc.getLongitude());
        Log.d("MainActivity", "topic is: "+topic);
        MqttMessage msg = new MqttMessage(json_string.getBytes());
        msg.setQos(0);
        try {
            mqttClient.publish(topic,msg);
        }
        catch(MqttException except)
        {
            Log.d("MainActivity", "MQTT: could not send location message: "+except.getMessage() +" (" +except.getReasonCode() + ")");
        }
    }

    @Override
    public void onLocationChanged(Location loc)
    {
        LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
        mMap.animateCamera(zoom);
        subscribeToFriends(loc);
        Log.d("MainActivity","Location changed to: lat: "+loc.getLatitude()+", lng: "+loc.getLongitude());
        if (broadcastingEnabled)
        {   // if location when converted to accuracy of 110m (3 decimal places) has changed
            if (lastLocation == null || roundtoThreeDecimals(loc.getLatitude()) != roundtoThreeDecimals(lastLocation.getLatitude()) &&
                    roundtoThreeDecimals(loc.getLongitude()) != roundtoThreeDecimals(lastLocation.getLongitude()))
            {
                sendNewAreaUpdate(loc);
                if (lastLocation != null)
                    unsubscribeToFriends(lastLocation);
                subscribeToFriends(loc);

                lastLocation = loc;
            }
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
            if (command.equals("loc_update"))
            {
                final Double lat = json_obj.getDouble("lat");
                final Double lng = json_obj.getDouble("lng");
                if (friendHashMap.get(email) == null)
                {
                    friendHashMap.put(email, new Friend(email, lat, lng, email));
                    //sendNotification();
                }
                else
                {
                    Friend friend = friendHashMap.get(email);
                    friend.setLat(lat);
                    friend.setLng(lng);
                }

                // update list view on UI thread
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        friendListAdapter.notifyDataSetChanged();
                        updateMarkers();
                        sendNotification();
                    }

                });
            }
            else if (command.equals("loc_remove"))
            {
                if (friendHashMap.get(email) != null)
                {
                    friendHashMap.remove(email);

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            friendListAdapter.notifyDataSetChanged();
                            updateMarkers();
                        }

                    });
                }
            }


        }
        catch(Exception except) {
            Log.d("MainActivity", "MessageArrived exception: " + except.getMessage());
        }
    }

    private void updateMarkers()
    {
        Iterator<String> friendListIterator = friendHashMap.keySet().iterator();
        while(friendListIterator.hasNext())
        {
            String key = friendListIterator.next();
            if(markers.get(key) == null) //if there is no marker for the friend
            {
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(friendHashMap.get(key).getLat(), friendHashMap.get(key).getLng()))
                        .title(friendHashMap.get(key).getName()));

                markers.put(key, marker);
                //if(!notificationList.contains(key))
                    notificationList.add(key);
            }
            else
            {
                LatLng old_loc = markers.get(key).getPosition();
                LatLng new_loc = new LatLng(friendHashMap.get(key).getLat(), friendHashMap.get(key).getLng());
                markers.get(key).setPosition(new_loc);
                float[] result = new float[1];
                Location.distanceBetween(old_loc.latitude, old_loc.longitude, new_loc.latitude, new_loc.longitude, result);
                if(result[0] >100) {
                    //if(!notificationList.contains(key))
                        notificationList.add(key);
                }
            }
        }
        Iterator<String> markerIterator = markers.keySet().iterator();
        while (markerIterator.hasNext())
        {
            String key = markerIterator.next();
            if(friendHashMap.get(key) == null) //Friend for Marker m, doesn't exist in Marker
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

    public void sendNotification()
    {
        Log.d("Notification", "Run notification() ");
        if(!isInForeground && notificationList.size()>0)
        {
            String title;
            String contentText = "";
            Log.d("Notification", "notiList.size() = "+notificationList.size());
            /*
            if(notificationList.size()==1)
            {
                title = "There is 1 friend near you!";
                contentText = friendHashMap.get(notificationList.getName() + " is near you!";
            }
            else
            {
            */
                int i = 0;
                title = "There is several friends near you!";
                for(Object name : notificationList)
                {
                    if(i==3)
                        break;
                    contentText += friendHashMap.get(name).getName() + ", ";
                    i++;
                }
                contentText = contentText.substring(0, contentText.length()-2);
                if(i == 1)
                    title = "There is 1 friend near you!";
                else
                    title = "There is several friends near you!";

           // }

            notificationList.clear();
            Log.d("Notification", "Notification send!");
            int mId = 5;
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.broadcast_disabled)
                            .setContentTitle(title)
                            .setContentText(contentText);
// Creates an explicit intent for an Activity in your app
            final Intent resultIntent = new Intent(getApplicationContext(),
                    MainActivity.class);
            //resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            resultIntent.setAction("android.intent.action.MAIN");
            resultIntent.addCategory("android.intent.category.LAUNCHER");



// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
// Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            mBuilder.setAutoCancel(true);
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mBuilder.setSound(alarmSound);
            mBuilder.setVibrate(new long[] { 0, 100, 100, 100, 100 });
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
            mNotificationManager.notify(mId, mBuilder.build());
        }
        else
        {
            Log.d("Notification", "Activity is in foreground. No notification send");
            notificationList.clear();
        }
        notificationList.clear();
    }
}
