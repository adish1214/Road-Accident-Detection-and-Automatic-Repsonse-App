package com.example.crashdetector;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    EditText phoneInput1;
    EditText phoneInput2;
    EditText phoneInput3;
    private EditText delayInput;
    private Button saveBtn;

    private SeekBar sensitivitySeekBar;
    private TextView sensitivityLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sensitivitySeekBar = findViewById(R.id.sensitivitySeekBar);
        sensitivityLabel = findViewById(R.id.sensitivityLabel);

        SharedPreferences prefs =
                getSharedPreferences("CrashSettings", MODE_PRIVATE);

        int savedSensitivity = prefs.getInt("sensitivity", 1);
        sensitivitySeekBar.setProgress(savedSensitivity);

        updateSensitivityLabel(savedSensitivity);

        sensitivitySeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar, int progress, boolean fromUser) {
                        updateSensitivityLabel(progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

        // Connect XML views
        phoneInput1 = findViewById(R.id.phoneInput1);
        phoneInput2 = findViewById(R.id.phoneInput2);
        phoneInput3 = findViewById(R.id.phoneInput3);
        delayInput = findViewById(R.id.delayInput);
        saveBtn = findViewById(R.id.saveBtn);

        // Open SharedPreferences


        // Load previously saved values
        phoneInput1.setText(prefs.getString("phone1", ""));
        phoneInput2.setText(prefs.getString("phone2", ""));
        phoneInput3.setText(prefs.getString("phone3", ""));
        delayInput.setText(String.valueOf(prefs.getInt("delay", 30)));

        saveBtn.setOnClickListener(v -> {

            String phone1 = phoneInput1.getText().toString().trim();
            String phone2 = phoneInput2.getText().toString().trim();
            String phone3 = phoneInput3.getText().toString().trim();

            String delayText = delayInput.getText().toString().trim();

            if (phone1.isEmpty() && phone2.isEmpty() && phone3.isEmpty()) {
                phoneInput1.setError("Enter at least one emergency number");
                return;
            }

            if (delayText.isEmpty()) {
                delayInput.setError("Enter delay");
                return;
            }

            int delay;

            try {
                delay = Integer.parseInt(delayText);

                if (delay <= 0) {
                    delayInput.setError("Delay must be greater than 0");
                    return;
                }
            } catch (NumberFormatException e) {
                delayInput.setError("Enter a valid number");
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();

            editor.putString("phone1", phone1);
            editor.putString("phone2", phone2);
            editor.putString("phone3", phone3);
            editor.putInt("delay", delay);
            editor.putInt("sensitivity", sensitivitySeekBar.getProgress());

            editor.apply();

            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();

            finish();
        });

    }
    private void updateSensitivityLabel(int progress) {
        String[] levels = {"Low", "Medium", "High"};

        sensitivityLabel.setText(
                "Sensor Sensitivity: " + levels[progress]
        );
    }
}