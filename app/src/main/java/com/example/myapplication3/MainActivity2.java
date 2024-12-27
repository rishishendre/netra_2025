package com.example.myapplication3;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaSquare;


public class MainActivity2 extends CameraActivity {
    CameraBridgeViewBase camera;
    View lowerbondcolor;
    TextView COMMAND;
    ArrayList<MatOfPoint> detected = new ArrayList<>();
    ArrayList<MatOfPoint> focus = new ArrayList<>();
    MatOfPoint thatframe = new MatOfPoint();
    int defaultcolor = 0xFFFFFF;
    Scalar upperbond,lowerbond;
    double[] colorbooi = new double[3];
    Button colorpicker;
    String cmd;
    Mat input;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        camera = findViewById(R.id.camera);
        lowerbondcolor = findViewById(R.id.lowerbondcolor);
        COMMAND = findViewById(R.id.textView);
        colorpicker = findViewById(R.id.button);
        if(OpenCVLoader.initDebug()){
            camera.enableView();
        }
        else{
            Log.e("OPENCVLOAD", "onCreate: FAILED");
        }
        colorpicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AmbilWarnaDialog colorpick = new AmbilWarnaDialog(MainActivity2.this, defaultcolor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                    }
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        int red = Color.red(color);
                        int green = Color.green(color);
                        int blue = Color.blue(color);
                        Colorconverter(red,green,blue);
                        lowerbondcolor.setBackgroundColor(color);
                        }
                });
                colorpick.getDialog().setOnShowListener(dialog -> {
                    AlertDialog alertDialog = (AlertDialog) dialog;
                    Window window = alertDialog.getWindow();
                    if(window!=null){
                        WindowManager.LayoutParams layout = new WindowManager.LayoutParams();
                        layout.copyFrom(window.getAttributes());
                        layout.width = 1100;
                        layout.height=600;
                        window.setAttributes(layout);
                    }
                    Button okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    Button cancelButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                    if (okButton != null&&cancelButton!=null) {
                        okButton.setTextColor(getResources().getColor(R.color.iconTint)); // Example color
                        cancelButton.setTextColor(getResources().getColor(R.color.iconTint)); // Example color
                    }
                    View dialogView = alertDialog.findViewById(android.R.id.content);
                    if (dialogView != null) {
                        View colorpickerview = dialogView.findViewById(yuku.ambilwarna.R.id.ambilwarna_viewSatBri);
                        View hueBar = dialogView.findViewById(yuku.ambilwarna.R.id.ambilwarna_viewHue);
                        if(colorpickerview!=null){
                            ViewGroup.LayoutParams para = colorpickerview.getLayoutParams();
                            para.height=600;
                            colorpickerview.setLayoutParams(para);
                            colorpickerview.requestLayout();
                            colorpickerview.invalidate();

                        }
                        if(hueBar!=null){
                            ViewGroup.LayoutParams para = hueBar.getLayoutParams();
                            para.height=600;
                            hueBar.setLayoutParams(para);
                        }
                        dialogView.setBackgroundColor(getResources().getColor(R.color.theme)); // Example color
                    }
                });
                colorpick.show();
            }
        });
        camera.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {}
            @Override
            public void onCameraViewStopped() {}
            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                input = inputFrame.rgba();
                Mat hsv = new Mat();
                Mat bit = new Mat();
                Imgproc.cvtColor(input, input, Imgproc.COLOR_RGBA2BGR);
                Mat img = new Mat(input.size(), input.type());
                Imgproc.cvtColor(input, hsv, Imgproc.COLOR_BGR2HSV);
                if(lowerbond!=null&&upperbond!=null) {
                    Core.inRange(hsv, lowerbond, upperbond, bit);
                    Imgproc.cvtColor(bit, bit, Imgproc.COLOR_GRAY2BGR);
                    Core.bitwise_and(input, bit, img);
                    Mat cnt = new Mat();
                    Imgproc.cvtColor(img,cnt,Imgproc.COLOR_BGR2GRAY);
                    Imgproc.findContours(cnt,detected,new Mat(),Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
                  if(!detected.isEmpty()) {
                        double maxarea = Imgproc.contourArea(detected.get(0));
                        for (int i = 0; i < detected.size(); i++) {
                            double area = Imgproc.contourArea(detected.get(i));
                            if (area >= maxarea) {
                                maxarea = area;
                                thatframe = detected.get(i);
                            } else {
                            }
                        }
                        focus.add(thatframe);
                    }
                    for(MatOfPoint m : focus){
                        Rect r=Imgproc.boundingRect(m);
                        Imgproc.rectangle(img,r,new Scalar(255,255,255));
                        double height=inputFrame.rgba().height();
                        double width=inputFrame.rgba().width();
                        double inputarea=height*width;
                        double detected=r.area();
                        double min=inputarea/200;
                        double threshold=inputarea/6;
                        double thresholdwidth=width/6;
                        double inputcenterX = (double) inputFrame.rgba().width()/2;
                        double inputcenterY = (double) inputFrame.rgba().height()/2;
                        double reccenterX = (double) r.x+r.width/2;
                        double reccenterY = (double) r.y+r.height/2;
                        System.out.println(inputarea);
                        Imgproc.circle(img,new Point(reccenterX,reccenterY),10,new Scalar(255,0,0),5);
                        Imgproc.circle(img,new Point(inputcenterX,inputcenterY),10,new Scalar(0,255,255),5);
                        if(detected>=threshold||detected<min){
                            COMMAND.setText("S");
                            cmd="S";
                        }
                        else if(reccenterX>inputcenterX+thresholdwidth){
                            COMMAND.setText("R");
                            cmd="R";
                        } else if(reccenterX<inputcenterX-thresholdwidth){
                            COMMAND.setText("L");
                            cmd="L";
                        } else if(reccenterX>inputcenterX-thresholdwidth && reccenterX<inputcenterX+thresholdwidth){
                            COMMAND.setText("F");
                            cmd="F";
                        }
                        else{
                            COMMAND.setText("S");
                            cmd="S";
                        }

                        BluetoothSingleton.getInstance().sendData(cmd+"*");
                    }
                    focus.clear();
                    detected.clear();

                }
                else{
                    img=inputFrame.rgba();
                }
                return img;
            }

        });
        getpermission();
    }
    private void getpermission() {
        if(checkSelfPermission(Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA},100);
            recreate();
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(camera);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0&&grantResults[0]!=PackageManager.PERMISSION_GRANTED){
            getpermission();
        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    public void Colorconverter(int red,int green,int blue){
        Mat rgb = new Mat(1,1, CvType.CV_8UC3,new Scalar(red,green,blue));
        Mat hsv = new Mat();
        Imgproc.cvtColor(rgb,hsv,Imgproc.COLOR_RGB2HSV);
        colorbooi = hsv.get(0,0);
        lowerbond = new Scalar(colorbooi[0]-50,colorbooi[1]-100,colorbooi[2]-20);
        upperbond = new Scalar(colorbooi[0]+50,colorbooi[1]+100,colorbooi[2]+20);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothSingleton.getInstance().sendData("S*");
    }
}