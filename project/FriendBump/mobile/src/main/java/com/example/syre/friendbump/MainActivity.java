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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

//For notification
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;

public class MainActivity extends Activity implements OnMapReadyCallback,
                                                      LocationListener,
                                                      GoogleApiClient.ConnectionCallbacks,
                                                      GoogleApiClient.OnConnectionFailedListener,
                                                      MqttCallback {
    List<Friend> listItems = new ArrayList<Friend>();
    List<Marker> markers = new ArrayList<Marker>();
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
    Location last_location;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        friendmapview = (MapView)findViewById(R.id.friendMapView);
        toggleBroadcastingButton = (ImageButton)findViewById(R.id.toggleBroadcastingButton);
        friendmapview.onCreate(savedInstanceState);
        listItems.add(new Friend("Anders Rahbek",0.0,0.0,"handiiandii@gmail.com"));
        listItems.add(new Friend("Søren Howe Gersager",0.0,0.0,"syrelyre@gmail.com"));
        friendListView = (ListView)findViewById(R.id.friendListView);
        friendListAdapter = new ArrayAdapter<Friend>(this, android.R.layout.simple_list_item_1, listItems);
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
    }
    @Override
    public void onPause() {
        super.onPause();
        friendmapview.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        friendmapview.onResume();
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
        Criteria criteria = new Criteria();
        String provider = locationmanager.getBestProvider(criteria, false);
        last_location = locationmanager.getLastKnownLocation(provider);

        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(last_location.getLatitude(), last_location.getLongitude()));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
        // enables location marker
        mMap = map;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.moveCamera(center);
        mMap.animateCamera(zoom);


        Location myLocation = mMap.getMyLocation();
        if(myLocation != null) {
            LatLng myLatLng = new LatLng(myLocation.getLatitude(),
                    myLocation.getLongitude());

            CameraPosition myPosition = new CameraPosition.Builder()
                    .target(myLatLng).zoom(17).build();
            mMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(myPosition));
        }


        for(int i = 0; i<listItems.size();i++)
        {
            Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(listItems.get(i).getLat(), listItems.get(i).getLng()))
                .title(listItems.get(i).getName()));
            markers.add(marker);
        }
    }

    private double roundtoThreeDecimals(double value)
    {
        DecimalFormat df2 = new DecimalFormat("###.###");
        return Double.valueOf(df2.format(value));
    }

    @Override
    public void onLocationChanged(Location loc)
    {
        Log.d("MainActivity","Location changed to: lat: "+loc.getLatitude()+", lng: "+loc.getLongitude());
        if (broadcastingEnabled)
        {   // if location when converted to accuracy of 110m (3 decimal places) has changed
            if (roundtoThreeDecimals(loc.getLatitude()) != roundtoThreeDecimals(last_location.getLatitude()) &&
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
                for (Friend f: listItems)
                {
                    String topic_string = f.getEmail()+"."+roundtoThreeDecimals(loc.getLatitude())+"."+roundtoThreeDecimals(loc.getLongitude());
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
                Log.d("MainActivity", "MQTT: could not send location message: "+except.getMessage());
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

    }

    private int findFriendIndexByEmail(String email)
    {
        for (Friend f: listItems)
        {
            if (f.getEmail().equals(email))
            {
                return listItems.indexOf(f);
            }

        }
        return -1;
    }

    private void parseCommand(JSONObject json_obj) {
        try {
            final String command = json_obj.getString("command");
            final String email = json_obj.getString("email");
            if (command.equals("loc_update"))
            {
                final Double lat = json_obj.getDouble("lat");
                final Double lng = json_obj.getDouble("lng");
                if (findFriendIndexByEmail(email) == -1)
                {
                    listItems.add(new Friend(email,lat, lng, email));
                    notification();
                }
                else
                {
                    Friend friend = listItems.get(findFriendIndexByEmail(email));
                    friend.setLat(lat);
                    friend.setLng(lng);
                }
                // update list view on UI thread
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        friendListAdapter.notifyDataSetChanged();
                        updateMarker();
                    }

                });
            }
            else if (command.equals("loc_removal"))
            {
                int index = findFriendIndexByEmail(email);
                if (index != 1)
                {
                    listItems.remove(index);

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

        for (Friend f: listItems)
        {
            boolean flag = false;
            for (Marker m : markers) {
                if (m.getTitle().equals(f.getEmail()))
                {
                    LatLng pos = new LatLng(f.getLat(), f.getLng());
                    m.setPosition(pos);
                    flag = true;
                }

            }
            if(!flag) //Marker for friend f, doesn't exist in Marker
            {
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(f.getLat(), f.getLng()))
                        .title(f.getName()));
                Log.e("Markers", "Adding the marker to markers");
                markers.add(marker);
            }
        }

        for (Marker m: markers)
        {
            boolean flag = false;
            for (Friend f : listItems) {
                if (m.getTitle().equals(f.getName()))
                {
                    flag = true;
                }

            }
            if(!flag) //Friend for Marker m, doesn't exist in Marker
            {
              markers.remove(m);
            }
        }

    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
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
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    public void notification()
    {
        Log.e("Notification", "Run notification() ");
        int mId = 5;
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.broadcast_disabled)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");
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
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mBuilder.build());
    }
}
