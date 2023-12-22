package com.toshiba.dotandbox;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity {
    private int rows = 5;
    private int cols = 5;
    String spinner_value = "5x5";
    private boolean withAndroid = true;
    NumberPicker numberPickerRows;
    NumberPicker numberPickerCols;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button singlePlayerButton = findViewById(R.id.button_singlePlayer);
        Button twoPlayerButton = findViewById(R.id.button_TwoPlayer);
        singlePlayerButton.setSelected(true);
        twoPlayerButton.setSelected(false);

        singlePlayerButton.setOnClickListener(v -> {
            // Toggle the selected state
            if(!singlePlayerButton.isSelected()) // if PlayWithAndroid button is clicked, set the other button deselected
            {
                Log.d("singleplayer", "singlePlayerButton.setOnClickListener: ");
                singlePlayerButton.setSelected(true);
                twoPlayerButton.setSelected(false);
                withAndroid = true;
            }
        });

        twoPlayerButton.setOnClickListener(v -> {
            // Toggle the selected state
            if(!twoPlayerButton.isSelected()) // if PlayWithFriend button is clicked, set the other button deselected
            {
                Log.d("singleplayer", "twoPlayerButton.setOnClickListener: ");
                twoPlayerButton.setSelected(true);
                singlePlayerButton.setSelected(false);
                withAndroid = false;
            }
        });

        Spinner spinner = findViewById(R.id.dropdownSpinner);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Get the selected item
                spinner_value = parentView.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Handle when nothing is selected (optional)
            }
        });
        spinner.setSelection(0);

        final Button predefinedGridButton = findViewById(R.id.button_predefinedGrids);
        final Button customGridsButton = findViewById(R.id.button_customGrids);
        numberPickerRows = findViewById(R.id.numberPicker_rows);
        numberPickerRows.setMinValue(2);
        numberPickerRows.setMaxValue(8);

        numberPickerCols = findViewById(R.id.numberPicker_cols);
        numberPickerCols.setMinValue(2);
        numberPickerCols.setMaxValue(8);

        predefinedGridButton.setSelected(true);
        // Set selected state when the button is clicked
        predefinedGridButton.setOnClickListener(v -> {
            // Toggle the selected state
            if(!predefinedGridButton.isSelected())
            {
                predefinedGridButton.setSelected(true);
                customGridsButton.setSelected(false);
            }
            /*String[] dimen = spinner_value.split("x");  // set the value selected in dropdown of grids when PredefinedGrids button is selected
            rows = Integer.parseInt(dimen[0]);
            cols = Integer.parseInt(dimen[1]);
            Log.d("singleplayer", "predefinedGridButton.setOnClickListener: rows="+rows+" cols=" +cols);
            */

        });

        // Set selected state when the button is clicked
        customGridsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle the selected state
                if(!customGridsButton.isSelected())
                {
                    customGridsButton.setSelected(true);
                    predefinedGridButton.setSelected(false);
                    // Set a listener to handle value changes (optional)
                    /*numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                        @Override
                        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                            // Handle value changes if needed
                        }
                    });*/
                    // Get the selected value when needed
                   /* rows = numberPickerRows.getValue();  // set the value selected in numberPicker of grids when customGridButton button is selected
                    cols = numberPickerCols.getValue();*/
                }
            }
        });
        Button playButton = findViewById(R.id.button_play);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(predefinedGridButton.isSelected())
                {
                    String[] dimen = spinner_value.split("x");  // set the value selected in dropdown of grids when PredefinedGrids button is selected
                    rows = Integer.parseInt(dimen[0]);
                    cols = Integer.parseInt(dimen[1]);
                    Log.d("singleplayer", "playButton.setOnClickListener: rows="+rows+" cols=" +cols);
                }
                else if(customGridsButton.isSelected()) // set the value selected in numberPicker of grids when customGridButton button is selected
                {
                    rows = numberPickerRows.getValue();
                    cols = numberPickerCols.getValue();
                    Log.d("singleplayer", "playButton.setOnClickListener: rows="+rows+" cols=" +cols);
                }
                // Create an Intent
                Intent intent = new Intent(MainActivity.this, GameActivity.class);

                // Put the values into the Intent
                intent.putExtra("ROWS_VALUE", rows);
                intent.putExtra("COLS_VALUE", cols);
                intent.putExtra("MODE_VALUE", withAndroid);

                // Start the new activity
                startActivity(intent);   //start game activity on pressing Play button
            }
        });
    }
}

