package com.example.nguide6;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.AgastFeatureDetector;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.features2d.Feature2D;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

public class OpenCVCamera extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private Mat mRGBA, mRGBAT;

    private static final String TAG = "OpenCVCamera";
    private CameraBridgeViewBase cameraBridgeViewBase;

    private int angle = 0;
    private int stepCount = 0;
    private boolean detected = false;
    private boolean consumed = false;


    private double currentX = 0;
    private double currentY = 0;
    private double prevX = 0;
    private double prevY = 0;
    private int prevAngle = 0;


    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    cameraBridgeViewBase.setCameraPermissionGranted();
                    cameraBridgeViewBase.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_opencv_camera);
        cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(OpenCVCamera.this);


        TextView step_view = findViewById(R.id.step);
        TextView angle_view = findViewById(R.id.angle);

        android.hardware.SensorManager sensorManager = (android.hardware.SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensorRotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Sensor sensorAccelometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        SensorEventListener sensorEventListenerRotationVector = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] rotationMatrix = new float[16];
                android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                float[] remappedRotationMatrix = new float[16];
                android.hardware.SensorManager.remapCoordinateSystem(rotationMatrix, android.hardware.SensorManager.AXIS_X, android.hardware.SensorManager.AXIS_Z,
                        remappedRotationMatrix);
                float[] orientations = new float[3];
                android.hardware.SensorManager.getOrientation(remappedRotationMatrix, orientations);
                angle = (int) ((Math.toDegrees(orientations[0]) + 360) % 360);
                angle_view.setText("Direction: "+angle);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        SensorEventListener sensorEventListenerAccelometer = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float upperThreshold = 10.5f;
                float lowerTreshold =  10.0f;

                float x_acceleration = event.values[0];
                float y_acceleration = event.values[1];
                float z_acceleration = event.values[2];

                double magnitude = (float) Math.sqrt(x_acceleration*x_acceleration + y_acceleration*y_acceleration + z_acceleration*z_acceleration);
                if(!detected) {
                    if(magnitude > upperThreshold) {
                        detected = true;
                    }
                } else if(magnitude < lowerTreshold) {
                    detected = false;
                    consumed = false;
                }
                if(detected && !consumed){
                    stepCount++;
                    consumed = true;
                    step_view.setText("Steps: "+stepCount);

                    double step_length = 0.8;

                    currentX =  prevX + (step_length * Math.sin(Math.toRadians(angle)));
                    currentY =  prevY + (step_length * Math.cos(Math.toRadians(angle)));

                    prevX = currentX;
                    prevY = currentY;
                }

                System.out.print(""+stepCount);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        sensorManager.registerListener(sensorEventListenerRotationVector, sensorRotation, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListenerAccelometer, sensorAccelometer, SensorManager.SENSOR_DELAY_NORMAL);

        Log.d(TAG, "******** oncreate");

    }
    @Override
    public void onResume(){
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(height,width,CvType.CV_8UC4);
        Log.d(TAG, "******** started");

    }

    @Override
    public void onCameraViewStopped() {
        Log.d(TAG, "******** stoped");
        mRGBA.release();

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        mRGBA = featureDetector(mRGBA);
        return mRGBA;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraBridgeViewBase!= null){
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!= null){
            cameraBridgeViewBase.disableView();
        }
    }
    public Mat featureDetector(Mat mRGB){

        ORB orb = ORB.create();
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        orb.detect(mRGB, keypoints1);
        Mat descriptors1 = new Mat();
        orb.compute(mRGB, keypoints1, descriptors1);
        Features2d.drawKeypoints(mRGB,keypoints1,mRGB);
        return mRGB;
    }


}
