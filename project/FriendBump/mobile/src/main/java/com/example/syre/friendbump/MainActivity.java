package com.example.syre.friendbump;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
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

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

//For notification
import android.support.v4.app.NotificationCompat;


public class MainActivity extends Activity implements OnMapReadyCallback,
                                                      LocationListener,
                                                      GoogleApiClient.ConnectionCallbacks,
                                                      GoogleApiClient.OnConnectionFailedListener,
                                                      MqttCallback {

    HashMap<String, Friend> friendHashMap = new HashMap<String, Friend>();
    HashMap<String, Marker>  markers = new HashMap<String, Marker>();
    ListView friendListView;
    ArrayAdapter friendListAdapter;
    MapView friendmapview;
    GoogleApiClient mGoogleApiClient;
    Boolean broadcastingEnabled;
    ImageButton toggleBroadcastingButton;
    MqttClient mqttClient;
    GoogleMap mMap;
    MemoryPersistence persistence;
    String client_email;
    Location last_location = null;
    private static boolean isInForeground;
    ArrayList<String> notificationList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        friendmapview = (MapView)findViewById(R.id.friendMapView);
        toggleBroadcastingButton = (ImageButton)findViewById(R.id.toggleBroadcastingButton);
        friendmapview.onCreate(savedInstanceState);
        friendHashMap.put("handiiandii@gmail.com", new Friend("Anders Rahbek", 0.0, 0.0, "handiiandii@gmail.com"));
        friendHashMap.put("syrelyre@gmail.com", new Friend("Søren Howe Gersager", 0.0, 0.0, "syrelyre@gmail.com"));
        Log.d("Søren", "Marker er 0.0");
        friendListView = (ListView)findViewById(R.id.friendListView);
        ArrayList<Friend> valuesList = new ArrayList<Friend>(friendHashMap.values());
        friendListAdapter = new ArrayAdapter<Friend>(this, android.R.layout.simple_list_item_1, valuesList);
        friendListView.setAdapter(friendListAdapter);

        MapsInitializer.initialize(this);
        friendmapview.getMapAsync(this);
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
                client_email = account.name;
                break;
            }
        }

        try {
            mqttClient = new MqttClient("tcp://syrelyre.dk:1883",client_email,persistence);
            
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
    }
    @Override
    public void onPause() {
        super.onPause();
        friendmapview.onPause();
        isInForeground = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        isInForeground = false;
    }
    @Override
    public void onResume() {
        super.onResume();
        friendmapview.onResume();
        isInForeground = true;
        notificationList.clear();
        /*
        if(friendHashMap.isEmpty()==false) {
            Log.d("onResume", "friendHashMap is not empty");
            updateMarker();
        }
        */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        friendmapview.onDestroy();
        try{
            mqttClient.disconnect();
        }
        catch(MqttException except)
        {
            Log.d("MainActivity","MQTT: could not disconnect from broker");
        }
        isInForeground = false;
        Log.d("onDestroy", "Bye bye!");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        friendmapview.onLowMemory();
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

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        // gets last known location

        LocationManager locationmanager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        //LocationHelper loc = new LocationHelper();
        Criteria criteria = new Criteria();
        String provider = locationmanager.getBestProvider(criteria, false);
        Location last_location = locationmanager.getLastKnownLocation(provider);
        /*
        if(last_location!=null)
        {
            onLocationChanged(last_location);
        }

        //locationmanager.requestLocationUpdates(provider, 2000, 0, this);
        */

        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(last_location.getLatitude(), last_location.getLongitude()));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
        // enables location marker
        mMap = map;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.moveCamera(center);
        mMap.animateCamera(zoom);

        Iterator<String> friendListIterator = friendHashMap.keySet().iterator();

        while(friendListIterator.hasNext())
        {
            String key = friendListIterator.next();
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(friendHashMap.get(key).getLat(), friendHashMap.get(key).getLng()))
                    .title(friendHashMap.get(key).getName()));

            markers.put(friendHashMap.get(key).getEmail(), marker);

        }

    }

    private double roundtoThreeDecimals(double value)
    {
        DecimalFormat df2 = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
        df2.applyPattern("###.###");
        return Double.valueOf(df2.format(value));
    }

    @Override
    public void onLocationChanged(Location loc)
    {
        LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        Log.d("MainActivity","Location changed to: lat: "+loc.getLatitude()+", lng: "+loc.getLongitude());
        if (broadcastingEnabled)
        {   // if location when converted to accuracy of 110m (3 decimal places) has changed
            if (last_location == null || roundtoThreeDecimals(loc.getLatitude()) != roundtoThreeDecimals(last_location.getLatitude()) &&
                    roundtoThreeDecimals(loc.getLongitude()) != roundtoThreeDecimals(last_location.getLongitude()))
            {
                String command = "loc_removal";
                String json_string = "{email:"+client_email+
                        ",command:"+command+"}";
                String topic =  client_email +"."+roundtoThreeDecimals(loc.getLatitude())+"."+roundtoThreeDecimals(loc.getLongitude());
                MqttMessage msg = new MqttMessage(json_string.getBytes());
                msg.setQos(1);
                try {
                    mqttClient.publish(topic, msg);
                }
                catch(MqttException except)
                {
                    Log.d("MainActivity", "MQTT: could not publish loc_removal message: "+except.getMessage());
                }
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

                last_location = loc;
            }
            Double latitude = loc.getLatitude();
            Double longitude = loc.getLongitude();
            String command = "loc_update";
            String json_string = "{email:"+client_email+
                                  ",command:"+command+
                                  ",lat:"+latitude+
                                  ",lng:"+longitude+"}";
            String topic = client_email+"."+roundtoThreeDecimals(loc.getLatitude())+"."+roundtoThreeDecimals(loc.getLongitude());
            Log.d("MainActivity", "topic is: "+topic);
            MqttMessage msg = new MqttMessage(json_string.getBytes());
            msg.setQos(1);
            try {
                mqttClient.publish(topic,msg);
            }
            catch(MqttException except)
            {
                Log.d("MainActivity", "MQTT: could not send location message: "+except.getMessage() +" (" +except.getReasonCode() + ")");
            }
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
                        updateMarker();
                        sendNotification();
                    }

                });
            }
            else if (command.equals("loc_removal"))
            {
                //int index = findFriendIndexByEmail(email);
                if (friendHashMap.get(email) != null)
                {
                    friendHashMap.remove(email);

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            friendListAdapter.notifyDataSetChanged();
                            updateMarker();
                        }

                    });
                }
            }


        }
        catch(Exception except) {
            Log.d("MainActivity", "MessageArrived exception: " + except.getMessage());
        }
    }

    private void updateMarker()
    {

        Iterator<String> friendListIterator = friendHashMap.keySet().iterator();
        Iterator<String> markerIterator = markers.keySet().iterator();
        while(friendListIterator.hasNext())
        {
            String key = friendListIterator.next();
            if(markers.get(key) == null) //if there is no marker for the friend
            {
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(friendHashMap.get(key).getLat(), friendHashMap.get(key).getLng()))
                        .title(friendHashMap.get(key).getName()));

                markers.put(key, marker);
                notificationList.add(key);
            }
            else
            {
                LatLng old = markers.get(key).getPosition();
                LatLng New = new LatLng(friendHashMap.get(key).getLat(), friendHashMap.get(key).getLng());
                markers.get(key).setPosition(New);
                float[] result = new float[1];
                Location.distanceBetween(old.latitude, old.longitude, New.latitude, New.longitude, result);
                if(result[0] >100)
                {
                    notificationList.add(key);
                }
                Log.d("updateMarker", "result[0] = "+result[0] + ", name = " + friendHashMap.get(key).getName());
                Log.d("updateMarker", "notificationList.size() = " + notificationList.size());
            }
        }

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
            if (!json_obj.getString("email").equals(client_email))
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
            String title = "";
            String contentText = "";
            if(notificationList.size()==1)
            {
                title = "There is 1 friend near you!";
                contentText = friendHashMap.get(notificationList.get(0)).getName() + " is near you!";
            }
            else
            {
                title = "There is several friends near you!";
                for(int i = 0; i<3; i++)
                {
                    contentText += friendHashMap.get(notificationList.get(0)).getName() + ", ";
                }
                contentText = contentText.substring(0, contentText.length()-2);
            }
            notificationList.clear();
            Log.d("Notification", "Notification send!");
            int mId = 5;
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.broadcast_disabled)
                            .setContentTitle(title)
                            .setContentText(contentText);
// Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(this,
                    MainActivity.class);

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
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
            mNotificationManager.notify(mId, mBuilder.build());
        }
        else
        {
            Log.d("Notification", "Activity is in forground. No notification send");
            Log.d("Notification", "notificationList.size() = " +notificationList.size());
            notificationList.clear();
        }
        notificationList.clear();
    }
}
