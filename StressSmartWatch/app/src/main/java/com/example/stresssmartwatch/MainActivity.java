package com.example.stresssmartwatch;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.example.stresssmartwatch.databinding.ActivityMainBinding;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private TextView mTextView;
    private ActivityMainBinding binding;
    private SensorManager mSensorManager;
    private Sensor mHeartSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mTextView = (TextView)findViewById(R.id.healthText);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mHeartSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        Log.i(TAG, " Program Start!");
        mTextView.setText("Program Start");
        startMeasure();

    }

    private void startMeasure() {
        boolean sensorRegistered = mSensorManager.registerListener(mSensorEventListener, mHeartSensor, mSensorManager.SENSOR_DELAY_FASTEST);
        Log.d("Sensor Status:", " Sensor registered: " + (sensorRegistered ? "yes" : "no"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mSensorEventListener, mHeartSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mSensorEventListener);
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {

            if (event.sensor.getType() == Sensor.TYPE_HEART_RATE){
                //if we have a change in the heart rate, display it
                mTextView = (TextView)findViewById(R.id.healthText);

                float mHeartRateFloat = event.values[0];
                int mHeartRate = Math.round(mHeartRateFloat);
                mTextView.setText(Integer.toString(mHeartRate));
            }

        }


    };
}



