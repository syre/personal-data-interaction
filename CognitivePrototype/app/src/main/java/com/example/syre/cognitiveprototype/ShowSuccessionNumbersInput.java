package com.example.syre.cognitiveprototype;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;


public class ShowSuccessionNumbersInput extends ActionBarActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_succession_numbers_input);

        EditText succession_numbers_input = (EditText)findViewById(R.id.succession_numbers_input);
        final Bundle bundle = getIntent().getExtras();
        if (bundle == null)
            getIntent().putExtras(new Bundle());
        boolean has_successful_tries = bundle.containsKey("successful_tries");
        if (!has_successful_tries)
        {
            getIntent().getExtras().putInt("successful_tries",1);
        }
        succession_numbers_input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled;
                String user_input = v.getText().toString();
                Integer successful_tries = getIntent().getExtras().getInt("successful_tries");
                ArrayList<Integer> succession_numbers_list = getIntent().getExtras().getIntegerArrayList("succession_numbers_list");

                String correct_numbers = "";
                for (Integer number : succession_numbers_list)
                {
                    correct_numbers += number.toString();
                }

                    if (user_input.equals(correct_numbers))
                    {
                        Intent intent = new Intent(v.getContext(), ShowSuccessionNumbersMain.class);
                        intent.putExtra("successful_tries",++successful_tries);
                        intent.putExtra("succession_numbers_list", succession_numbers_list);
                        startActivity(intent);
                        handled = true;
                    }
                    else
                    {
                        Intent intent = new Intent(v.getContext(), ShowSuccessionNumbersResult.class);
                        intent.putExtra("successful_tries", successful_tries);
                        startActivity(intent);
                        handled = true;
                    }

                    return handled;
                }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_succession_numbers_input, menu);
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
