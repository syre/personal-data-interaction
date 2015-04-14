package com.example.syre.friendbump;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
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
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

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
    String clientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        friendmapview = (MapView)findViewById(R.id.friendMapView);
        toggleBroadcastingButton = (ImageButton)findViewById(R.id.toggleBroadcastingButton);
        friendmapview.onCreate(savedInstanceState);

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
        clientId = "client" + ((int) (10000*Math.random()));
        try {
            mqttClient = new MqttClient("tcp://syrelyre.dk:1883",clientId,persistence);
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setKeepAliveInterval(60);
            mqttClient.setCallback(this);
            mqttClient.connect();
            mqttClient.subscribe("friendbump");
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

        //noinspection SimplifiableIfStatement
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
        Location last_known = locationmanager.getLastKnownLocation(provider);

        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(last_known.getLatitude(), last_known.getLongitude()));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
        // enables location marker
        mMap = map;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.moveCamera(center);
        mMap.animateCamera(zoom);
        for(int i = 0; i<listItems.size();i++)
        {
            Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(listItems.get(i).getLat(), listItems.get(i).getLng()))
                .title(listItems.get(i).getName()));
            markers.add(marker);
        }
        /*
        Toast.makeText(getApplicationContext(), "Creating marker", Toast.LENGTH_SHORT).show();
        //Marker marker = new MarkerOptions().position(new LatLng(55.83045, 12.42811)).title("hej");
        //Marker m1 = new MarkerOptions().position(new LatLng(55.83045, 12.42811)).title("hej");;
        mMap.addMarker(new MarkerOptions().position(new LatLng(55.83045, 12.42811)).title("hej"));
        Toast.makeText(getApplicationContext(), "Adding marker", Toast.LENGTH_SHORT).show();
        //markers.add(m);
        Toast.makeText(getApplicationContext(), "Finish Adding marker", Toast.LENGTH_SHORT).show();
        */
    }
    @Override
    public void onLocationChanged(Location loc)
    {
        Log.d("MainActivity","Location changed");
        if (broadcastingEnabled)
        {
            Double latitude = loc.getLatitude();
            Double longitude = loc.getLongitude();
            String command = "loc_update";
            String json_string = "{id:"+clientId+
                                  ",command:"+command+
                                  ",lat:"+latitude+
                                  ",lng:"+longitude+"}";
            MqttTopic topic = mqttClient.getTopic("friendbump");
            MqttMessage msg = new MqttMessage(json_string.getBytes());
            msg.setQos(1);
            try {
                MqttToken token = topic.publish(msg);
                token.waitForCompletion();
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
            toggleBroadcastingButton.setBackgroundResource(R.mipmap.enable_broadcasting);
        }
        else
        {
            toggleBroadcastingButton.setBackgroundResource(R.mipmap.disable_broadcasting);
        }
        broadcastingEnabled = !broadcastingEnabled;
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    private int findFriendIndexById(String id)
    {
        for (Friend f: listItems)
        {
            if (f.getName().equals(id))
            {
                return listItems.indexOf(f);
            }

        }
        return -1;
    }

    private void parseCommand(JSONObject json_obj) {
        String command = "";
        //final String id = "";
        try {
            command = json_obj.getString("command");
            final String id = json_obj.getString("id");
            if (command.equals("loc_update")) {
                final Double lat = json_obj.getDouble("lat");
                final Double lng = json_obj.getDouble("lng");
                if (findFriendIndexById(id) == -1)
                {
                    //Looper.prepare();

                    listItems.add(new Friend(id, lat, lng));
                    Log.w("Markers", "Creating a marker");
                    /*
                    Toast.makeText(getApplicationContext(), "hej hej! - Creating marker", Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(lat, lng))
                                    .title(id));
                            Log.w("Markers", "Adding the marker to markers");
                            markers.add(marker);
                        }

                    });

                    */


                }
                else
                {
                    listItems.get(findFriendIndexById(id)).setLat(lat);
                    listItems.get(findFriendIndexById(id)).setLng(lng);
                    /*
                    if(updateMarker(id, lat, lng))
                    {
                        Toast.makeText(getApplicationContext(), "Update marker success", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Update marker failed", Toast.LENGTH_LONG).show();
                    }
                    */
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
                if (m.getTitle().equals(f.getName()))
                {
                    LatLng pos = new LatLng(f.getLat(), f.getLng());
                    m.setPosition(pos);
                    flag = true;
                    //return listItems.indexOf(f);
                }

            }
            if(!flag) //Marker for friend f, doesn't exist in Marker
            {
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(f.getLat(), f.getLng()))
                        .title(f.getName()));
                Log.w("Markers", "Adding the marker to markers");
                markers.add(marker);
            }
        }
        //return false;
    }
    /*
    private void drawMarkers()
    {
        for (Marker m: markers)
        {
            if (m.getTitle().equals(id))
            {
                LatLng pos = new LatLng(lat, lng);
                m.setPosition(pos);
                return true;
                //return listItems.indexOf(f);
            }

        }
    }
    */
    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        String msg = new java.lang.String(mqttMessage.getPayload());
        Log.d("MainActivity", "message arrived:" + msg);
        JSONObject json_obj = null;
        try {
            json_obj = new JSONObject(msg);
            if (!json_obj.getString("id").equals(clientId))
                parseCommand(json_obj);

        }
        catch(Exception except) {
            Log.d("MainActivity", "MessageArrived exception: " + except.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
