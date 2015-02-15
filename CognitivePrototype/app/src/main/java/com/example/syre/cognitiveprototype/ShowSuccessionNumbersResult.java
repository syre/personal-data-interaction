package com.example.syre.cognitiveprototype;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ValueFormatter;
import com.github.mikephil.charting.utils.XLabels;
import com.github.mikephil.charting.utils.YLabels;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;


public class ShowSuccessionNumbersResult extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_succession_numbers_result);
        TextView successful_tries_text = (TextView)findViewById(R.id.successful_tries_text);
        BarChart result_chart = (BarChart)findViewById(R.id.result_chart);
        result_chart.setDrawBarShadow(false);
        result_chart.setDrawVerticalGrid(false);
        result_chart.setDescription("");
        ValueFormatter integer_formatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float v) {
                return new DecimalFormat("#").format(v);
            }
        };
        result_chart.setValueFormatter(integer_formatter);
        XLabels xLabels = result_chart.getXLabels();
        xLabels.setPosition(XLabels.XLabelPosition.BOTTOM);
        xLabels.setSpaceBetweenLabels(0);
        Integer successful_tries = getIntent().getExtras().getInt("successful_tries");
        successful_tries_text.setText("You managed " + successful_tries + " consecutive tries");

        ScoresDbHelper dbHelper = new ScoresDbHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
          DatabaseContract.ScoresTable.COLUMN_NAME_COL1,
          DatabaseContract.ScoresTable.COLUMN_NAME_COL3
        };
        Cursor c = db.query(
                DatabaseContract.ScoresTable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                "_id DESC limit 4"
        );

        ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        ArrayList<String> xvals = new ArrayList<String>();
        // get just measured score
        c.moveToNext();
        Integer current_score = c.getInt(0);
        ArrayList<BarEntry> current_yval = new ArrayList<BarEntry>();
        current_yval.add(new BarEntry(current_score, 0));
        xvals.add(c.getString(1).split("\\s")[0]);
        BarDataSet current_set = new BarDataSet(current_yval, "Current");
        current_set.setColors(ColorTemplate.JOYFUL_COLORS);
        dataSets.add(current_set);
        // get next 3 newest scores
        ArrayList<BarEntry> previous_yvals = new ArrayList<>();
        Integer counter = 1;
        while (c.moveToNext()) {
            previous_yvals.add(new BarEntry(c.getInt(0), counter));
            xvals.add(c.getString(1).split("\\s")[0]);

            counter += 1;
        }
        BarDataSet previous_set = new BarDataSet(previous_yvals, "Previous");
        dataSets.add(previous_set);
        BarData data = new BarData(xvals, dataSets);
        result_chart.setData(data);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_succession_numbers_result, menu);
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
