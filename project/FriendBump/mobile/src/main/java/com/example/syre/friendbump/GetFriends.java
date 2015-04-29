package com.example.syre.friendbump;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Anders on 28-04-2015.
 */
public class GetFriends extends AsyncTask<String, Void, String> {
    protected String doInBackground(String... clientEmail) {
        Log.d("getFriends", "doInBackground!");
        Log.d("getFriends", "clientEmail = " + clientEmail[0]);
        String httpString = "http://syrelyre.dk:81/?email=" + clientEmail[0];
        Log.d("getFriends", "clientEmail = " + clientEmail[0]);

        String jSon = "";
        while (jSon.equals("")) {
            try {
                URL friensURL = new URL(httpString);
                URLConnection friendsCon = friensURL.openConnection();
                BufferedReader friends = new BufferedReader(
                        new InputStreamReader(
                                friendsCon.getInputStream()));
                String inputLine;

                while ((inputLine = friends.readLine()) != null)
                    jSon += inputLine;

                friends.close();
                Log.d("getFriends", "jSon: " + jSon);

            } catch (Exception exception) {
                Log.d("getFriends", "try-exception: " + exception.getMessage());
                Log.d("getFriends", "try-exception: " + exception.toString());
            }
        }
        return jSon;
    }
}
