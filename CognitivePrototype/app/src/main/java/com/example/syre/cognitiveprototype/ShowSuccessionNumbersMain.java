package com.example.syre.cognitiveprototype;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;


public class ShowSuccessionNumbersMain extends ActionBarActivity {
    private ArrayList<Integer> succession_numbers_list = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_succession_numbers_main);

    }

    @Override protected void onStart()
    {
        super.onStart();
        final TextView succession_number = (TextView) findViewById(R.id.succession_number);
        Random generator = new Random();
        Integer numbers_size = 4;

        // populate array with random integers
        for (int i = 0; i < numbers_size; i++) {
            succession_numbers_list.add(generator.nextInt(9));
        }

        // show numbers 1 second in between each other
        for (int i = 0; i < numbers_size; i++) {
            final Integer current = succession_numbers_list.get(i);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    succession_number.setText(current.toString());
                }
            }, 1000 * i);
        }

        // change activity after numbers have been shown
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(ShowSuccessionNumbersMain.this, ShowSuccessionNumbersInput.class);
                Bundle bundle = new Bundle();

                String numbers_list_string = "";
                for (Integer number : succession_numbers_list)
                {
                    numbers_list_string += number.toString();
                }
                bundle.putString("succession_numbers", numbers_list_string);
                Bundle old_bundle = getIntent().getExtras();
                if (old_bundle != null && old_bundle.containsKey("successful_tries"))
                {
                    bundle.putInt("successful_tries",old_bundle.getInt("successful_tries"));
                }
                intent.putExtras(bundle);
                startActivity(intent);
            }
        }, 1000 * (numbers_size+1));
    }
    @Override
    protected void onStop()
    {
        super.onStop();
        succession_numbers_list.clear();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_succession_numbers_main, menu);
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
}
