package com.example.crashdetector;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class SensorGraphActivity extends AppCompatActivity
        implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    private GraphView accelGraph;
    private GraphView gyroGraph;

    private long startTime;

    private LineGraphSeries<DataPoint> accelXSeries = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> accelYSeries = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> accelZSeries = new LineGraphSeries<>();

    private LineGraphSeries<DataPoint> gyroXSeries = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> gyroYSeries = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> gyroZSeries = new LineGraphSeries<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        startTime = System.currentTimeMillis();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_graph);

        accelGraph = findViewById(R.id.accelGraph);
        gyroGraph = findViewById(R.id.gyroGraph);

        accelGraph.addSeries(accelXSeries);
        accelGraph.addSeries(accelYSeries);
        accelGraph.addSeries(accelZSeries);

        gyroGraph.addSeries(gyroXSeries);
        gyroGraph.addSeries(gyroYSeries);
        gyroGraph.addSeries(gyroZSeries);

        accelXSeries.setColor(android.graphics.Color.RED);
        accelYSeries.setColor(android.graphics.Color.GREEN);
        accelZSeries.setColor(android.graphics.Color.BLUE);

        gyroXSeries.setColor(android.graphics.Color.RED);
        gyroYSeries.setColor(android.graphics.Color.GREEN);
        gyroZSeries.setColor(android.graphics.Color.BLUE);

        accelGraph.getViewport().setScalable(true);
        accelGraph.getViewport().setScrollable(true);
        accelGraph.getViewport().setYAxisBoundsManual(true);
        accelGraph.getViewport().setMinY(-15);
        accelGraph.getViewport().setMaxY(15);


        gyroGraph.getViewport().setScalable(true);
        gyroGraph.getViewport().setScrollable(true);
        gyroGraph.getViewport().setYAxisBoundsManual(true);
        gyroGraph.getViewport().setMinY(-10);
        gyroGraph.getViewport().setMaxY(10);



        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        accelerometer =
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        gyroscope =
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }
    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME);

        sensorManager.registerListener(
                this,
                gyroscope,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this);
    }

    private int maxDataPoints=500;
    @Override
    public void onSensorChanged(SensorEvent event) {


        double time = (System.currentTimeMillis() - startTime) / 4000.0;

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                accelXSeries.appendData(
                        new DataPoint(time, x),
                        true,
                        maxDataPoints);

                accelYSeries.appendData(
                        new DataPoint(time, y),
                        true,
                        maxDataPoints);

                accelZSeries.appendData(
                        new DataPoint(time, z),
                        true,
                        maxDataPoints);

            }

            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

                float gx = event.values[0];
                float gy = event.values[1];
                float gz = event.values[2];

                gyroXSeries.appendData(
                        new DataPoint(time, gx),
                        true,
                        maxDataPoints);

                gyroYSeries.appendData(
                        new DataPoint(time, gy),
                        true,
                        maxDataPoints);

                gyroZSeries.appendData(
                        new DataPoint(time, gz),
                        true,
                        maxDataPoints);



        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}