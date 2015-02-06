package com.example.syre.cognitiveprototype;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.Random;


public class ShowSuccessionNumbersMain extends ActionBarActivity {
    private ArrayList<Integer> succession_numbers_list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_succession_numbers_main);
    }

    @Override protected void onStart()
    {
        super.onStart();
        final TextSwitcher succession_number = (TextSwitcher) findViewById(R.id.succession_textswitcher);
        succession_number.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView textView = new TextView(ShowSuccessionNumbersMain.this);
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(200);
                textView.setTextColor(Color.parseColor("#e5e5e5"));
                return textView;
            }
        });
        succession_number.setInAnimation(this, android.R.anim.slide_in_left);
        succession_number.setOutAnimation(this, android.R.anim.slide_out_right);
        Random generator = new Random();

        final Bundle old_bundle = getIntent().getExtras();
        if (old_bundle != null && old_bundle.containsKey("succession_numbers_list"))
            succession_numbers_list = old_bundle.getIntegerArrayList("succession_numbers_list");
        else
            succession_numbers_list = new ArrayList<>();
        // add random integer to array
        succession_numbers_list.add(generator.nextInt(10));

        // show numbers 1 second in between each other
        for (int i = 0; i < succession_numbers_list.size(); i++) {
            final Integer current = succession_numbers_list.get(i);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    succession_number.setText(current.toString());
                }
            }, 1500 * i);
        }

        // change activity after numbers have been shown
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(ShowSuccessionNumbersMain.this, ShowSuccessionNumbersInput.class);
                Bundle bundle = new Bundle();


                if (old_bundle != null && old_bundle.containsKey("successful_tries"))
                {
                    bundle.putInt("successful_tries",old_bundle.getInt("successful_tries"));
                }
                bundle.putIntegerArrayList("succession_numbers_list", succession_numbers_list);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        }, 1500 * (succession_numbers_list.size()));
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
