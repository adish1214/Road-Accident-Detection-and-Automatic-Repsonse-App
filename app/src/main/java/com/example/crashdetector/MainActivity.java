package com.example.crashdetector;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private ValueAnimator breathingAnimator;
    private String emergencyNumber1 = "";
    private String emergencyNumber2 = "";
    private String emergencyNumber3 = "";
    private static final int PERMISSION_REQUEST_CODE = 100;
    // Last known coordinates used in the emergency SMS.
    private double latitude = 0.0;
    private double longitude = 0.0;
    private boolean locationAvailable = false;
    // Sensitivity settings are loaded from SharedPreferences.
    private int sensitivityLevel = 1;
    private float sensitivityFactor = 1.0f;
    // UI
    TextView timerText;
    Button startBtn, stopBtn;
    ImageButton settingsBtn;
    ImageButton statisticsBtn;

    RotatingHeartbeat rotatingHeartbeat;
    ImageView warningImg;
    ConstraintLayout rootLayout;

    // Location
    FusedLocationProviderClient locationClient;

    // Sensors
    SensorManager sensorManager;
    Sensor accelerometer;
    Sensor gyroscope;

    // Crash detection state
    float lastAcceleration = SensorManager.GRAVITY_EARTH;
    long lastTimestamp = 0;
    boolean crashDetected = false;
    // Gyroscope
    float rotationMagnitude = 0f;
    boolean phoneRotated = false;

    // Threshold (rad/s)
    private static final float ROTATION_THRESHOLD = 4.5f;

    // Settings
    int delaySeconds = 5;
    CountDownTimer countDownTimer;

    //Gyroscope Values
    float gx = 0f;
    float gy = 0f;
    float gz = 0f;

    // Crash severity
    String crashSeverity = "Unknown";

    // Thresholds
    private static final float MINOR_IMPACT = 12f;
    private static final float MODERATE_IMPACT = 18f;
    private static final float SEVERE_IMPACT = 25f;

    // Classifies the detected event using impact and rotation thresholds.
    private String calculateSeverity(float impact, float rotation) {
        if (impact >= SEVERE_IMPACT * sensitivityFactor
                || rotation >= 8.0f * sensitivityFactor) {
            return "SEVERE";
        }

        if (impact >= MODERATE_IMPACT * sensitivityFactor
                || rotation >= 6.0f * sensitivityFactor) {
            return "MODERATE";
        }

        return "MINOR";
    }
    // Retrieves the device's last known location for the emergency message.
    private void updateLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(location -> {

            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                locationAvailable = true;
            }

        });

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind UI
        timerText = findViewById(R.id.timerText);
        startBtn = findViewById(R.id.startBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        stopBtn = findViewById(R.id.stopBtn);
        rootLayout = findViewById(R.id.rootLayout);
        warningImg = findViewById(R.id.warning);
        rotatingHeartbeat = findViewById(R.id.rotatingHeartbeat);
        statisticsBtn = findViewById(R.id.statisticsBtn);

        loadSettings();

        // Location
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        // Sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||

                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.SEND_SMS
                    },
                    PERMISSION_REQUEST_CODE
            );
        }

        // Buttons
        settingsBtn.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            SettingsActivity.class);

            startActivity(intent);

        });
        startBtn.setOnClickListener(v -> simulateCrash());
        stopBtn.setOnClickListener(v -> cancelAlert());

        timerText.setText("SCANNING");
        startBreathing();

        statisticsBtn.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    SensorGraphActivity.class
            );

            startActivity(intent);

        });

    }

    private void loadSettings() {

        SharedPreferences prefs = getSharedPreferences("CrashSettings", MODE_PRIVATE);

        delaySeconds = prefs.getInt("delay", 30);

        emergencyNumber1 = prefs.getString("phone1", "");
        emergencyNumber2 = prefs.getString("phone2", "");
        emergencyNumber3 = prefs.getString("phone3", "");

        sensitivityLevel = prefs.getInt("sensitivity", 1);

        switch (sensitivityLevel) {
            case 0:
                sensitivityFactor = 1.2f;
                break;

            case 1:
                sensitivityFactor = 1.6f;
                break;

            case 2:
                sensitivityFactor = 2.1f;
                break;
        }

    }

    private boolean isLocationEnabled() {

        LocationManager locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            try {
                return locationManager.isProviderEnabled(
                        LocationManager.GPS_PROVIDER
                ) || locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                );
            } catch (Exception e) {
                return false;
            }
        }
    }

    private void checkLocationSettings() {

        if (isLocationEnabled()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Location Required")
                .setMessage(
                        "Location services are turned off.\n\n"
                                + "This app needs your location to send "
                                + "your current position to emergency "
                                + "contacts in case of an accident.\n\n"
                                + "Please enable location to continue."
                )
                .setCancelable(false)
                .setPositiveButton("Enable Location", (dialog, which) -> {
                    Intent intent = new Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    );
                    startActivity(intent);
                })
                .setNegativeButton("Exit App", (dialog, which) -> {
                    finish();
                })
                .show();
    }

    // ================= SENSOR LIFECYCLE AND CRASH DETECTION =================

    @Override
    protected void onResume() {
        super.onResume();

        loadSettings();

        sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
        );

        sensorManager.registerListener(
                this,
                gyroscope,
                SensorManager.SENSOR_DELAY_NORMAL
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            float gx = event.values[0];
            float gy = event.values[1];
            float gz = event.values[2];

            rotationMagnitude =
                    (float) Math.sqrt(gx * gx + gy * gy + gz * gz);

            phoneRotated =
                    rotationMagnitude > ROTATION_THRESHOLD * sensitivityFactor;

            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float currentAcceleration =
                (float) Math.sqrt(x * x + y * y + z * z);

        long currentTime = System.currentTimeMillis();

        if (lastTimestamp != 0) {

            float deltaAcceleration = lastAcceleration - currentAcceleration;
            long deltaTime = currentTime - lastTimestamp;

            if (deltaAcceleration >= MINOR_IMPACT * sensitivityFactor
                    && deltaTime < 250
                    && phoneRotated
                    && !crashDetected)  {

                // Lock the crash detection state
                crashDetected = true;

                // Calculate severity ONCE, at the moment of detection
                crashSeverity = calculateSeverity(
                        deltaAcceleration,
                        rotationMagnitude
                );

                runOnUiThread(() -> {

                    Toast.makeText(
                            MainActivity.this,
                            "Crash Detected!\nSeverity: " + crashSeverity,
                            Toast.LENGTH_LONG
                    ).show();

                    simulateCrash();
                });
            }
        }

        lastAcceleration = currentAcceleration;
        lastTimestamp = currentTime;
    }
    private void startBreathing() {

        breathingAnimator = ValueAnimator.ofFloat(0.4f, 1f);
        breathingAnimator.setDuration(1500);
        breathingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathingAnimator.setRepeatMode(ValueAnimator.REVERSE);

        breathingAnimator.addUpdateListener(animation -> {

            float alpha = (float) animation.getAnimatedValue();

            int green = ContextCompat.getColor(this, R.color.scanner_green_bright);

            int r = (green >> 16) & 0xFF;
            int g = (green >> 8) & 0xFF;
            int b = green & 0xFF;

            timerText.setTextColor(Color.argb((int)(alpha * 255), r, g, b));
        });

        breathingAnimator.start();
    }
    private void stopBreathing() {
        if (breathingAnimator != null) {
            breathingAnimator.cancel();
            timerText.setTextColor(
                    ContextCompat.getColor(this, R.color.scanner_green_faded)
            );
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    void simulateCrash() {

        if ("Unknown".equals(crashSeverity)) {
            crashSeverity = calculateSeverity(
                    0f,
                    rotationMagnitude
            );
        }

        setAlertUI(true);

        stopBreathing();
        timerText.setText("CRASH DETECTED!");

        updateLocation();
        startCountdown();
    }

    void startCountdown() {

        setAlertUI(true);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(delaySeconds * 1000L, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText(
                        "Sending alert in: " + (millisUntilFinished / 1000) + "s"
                );
                stopBreathing();
                timerText.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
                timerText.setTextColor(getResources().getColor(R.color.scanner_green_bright));
            }

            @Override
            public void onFinish() {
                timerText.setText("Alert sent!");

                Toast.makeText(MainActivity.this, "Building message...", Toast.LENGTH_SHORT).show();

                String message =
                        "🚨 CRASH DETECTED!\n\n" +
                                "Severity: " + crashSeverity + "\n\n" +
                                "Location:\n" +
                                "https://maps.google.com/?q=" +
                                latitude + "," + longitude;

                Toast.makeText(MainActivity.this, "Sending Message", Toast.LENGTH_SHORT).show();

                sendSMS(message);
                resetState();
            }
        }.start();
    }

    void cancelAlert() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        Toast.makeText(this, "Alert cancelled", Toast.LENGTH_SHORT).show();
        resetState();
    }

    void resetState() {
        crashDetected = false;
        setAlertUI(false);
        phoneRotated = false;
        rotationMagnitude = 4.5f;
    }

    // ================= UI =================

    void setAlertUI(boolean active) {
        if (active) {
            switch (crashSeverity) {

                case "MINOR":
                    rootLayout.setBackgroundColor(Color.YELLOW);
                    break;

                case "MODERATE":
                    rootLayout.setBackgroundColor(Color.rgb(255, 140, 0));
                    break;

                case "SEVERE":
                    rootLayout.setBackgroundColor(Color.RED);
                    break;

                default:
                    rootLayout.setBackgroundColor(Color.RED);
            }
            stopBtn.setVisibility(Button.VISIBLE);
            startBtn.setVisibility(Button.GONE);
            stopBreathing();
            timerText.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
            timerText.setTextColor(getResources().getColor(R.color.scanner_green_bright));
            rotatingHeartbeat.setVisibility(rotatingHeartbeat.GONE);
            warningImg.setVisibility(ImageView.VISIBLE);

        } else {
            rootLayout.setBackgroundColor(Color.TRANSPARENT);
            stopBtn.setVisibility(Button.GONE);
            startBtn.setVisibility(Button.VISIBLE);
            timerText.setText("SCANNING");
            rotatingHeartbeat.setVisibility(rotatingHeartbeat.VISIBLE);
            warningImg.setVisibility(ImageView.GONE);
            startBreathing();

        }
    }

    // ================= EMERGENCY SMS =================
    private void sendToNumber(String phone, String message) {

        if (phone.isEmpty()) {
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();

        ArrayList<String> parts = smsManager.divideMessage(message);

        smsManager.sendMultipartTextMessage(
                phone,
                null,
                parts,
                null,
                null
        );
    }
    private void sendSMS(String message) {

        SmsManager smsManager = SmsManager.getDefault();

        String[] contacts = {
                emergencyNumber1,
                emergencyNumber2,
                emergencyNumber3
        };

        for (String phone : contacts) {

            if (phone == null || phone.trim().isEmpty()) {
                continue;
            }

            try {
                ArrayList<String> parts =
                        smsManager.divideMessage(message);

                smsManager.sendMultipartTextMessage(
                        phone,
                        null,
                        parts,
                        null,
                        null
                );

            } catch (Exception e) {

                Toast.makeText(
                        MainActivity.this,
                        "Failed to send SMS",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    // ================= LEGACY SETTINGS DIALOG =================

    void showSettingsDialog() {

        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(delaySeconds));

        new AlertDialog.Builder(this)
                .setTitle("Set Delay (seconds)")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        delaySeconds = Integer.parseInt(input.getText().toString());
                        Toast.makeText(
                                this,
                                "Delay set to " + delaySeconds + " seconds",
                                Toast.LENGTH_SHORT
                        ).show();
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Cancel", null)
                .show();

    }

}
