package com.example.myapplication3;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

public class Menu extends AppCompatActivity {
    private static final int DETECTION_REQUEST_CODE = 1;
    private Button Backbutton;
    private CardView Manual,Color,Person,ocr;
    TextView Welcome,controller,Colorfollow,Personfollow,OCR;
    ConstraintLayout mylayout;
    String myadd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Backbutton=findViewById(R.id.button4);
        Manual=findViewById(R.id.manualcontrol);
        Color=findViewById(R.id.colorfollower);
        Person=findViewById(R.id.humanfollower);
        Welcome=findViewById(R.id.welcome);
        controller=findViewById(R.id.manual);
        Colorfollow=findViewById(R.id.colorFollower);
        Personfollow=findViewById(R.id.personFollow);
        OCR=findViewById(R.id.ocrtext);
        ocr=findViewById(R.id.Ocr);
        mylayout = findViewById(R.id.Mylayout);
        int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if(currentMode==Configuration.UI_MODE_NIGHT_YES){
            Welcome.setTextColor(android.graphics.Color.parseColor("#E6E3E3"));
            controller.setTextColor(android.graphics.Color.parseColor("#E6E3E3"));
            Colorfollow.setTextColor(android.graphics.Color.parseColor("#E6E3E3"));
            Personfollow.setTextColor(android.graphics.Color.parseColor("#E6E3E3"));
            OCR.setTextColor(android.graphics.Color.parseColor("#E6E3E3"));
            Backbutton.setTextColor(android.graphics.Color.parseColor("#E6E3E3"));
            mylayout.setBackgroundColor(android.graphics.Color.BLACK);
        }
        else{
            Welcome.setTextColor(android.graphics.Color.BLACK);
            controller.setTextColor(android.graphics.Color.BLACK);
            Colorfollow.setTextColor(android.graphics.Color.BLACK);
            Personfollow.setTextColor(android.graphics.Color.BLACK);
            OCR.setTextColor(android.graphics.Color.BLACK);
            Backbutton.setTextColor(android.graphics.Color.BLACK);
            mylayout.setBackgroundColor(android.graphics.Color.WHITE);
        }
        myadd = getIntent().getStringExtra("mydeviceadd");
        ocr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Menu.this, ocrimage.class));
            }
        });
        Manual.setOnClickListener(v -> {
            Intent intent = new Intent(Menu.this,Controller.class);
            if(myadd!=null){
                intent.putExtra("mydevice",myadd);
            }
            startActivity(intent);
            finish(); // Temporarily comment out
        });
        Color.setOnClickListener(v -> {

            Intent intent2 = new Intent(Menu.this, MainActivity2.class);
            intent2.putExtra("fromMenu", true); // Add a flag to indicate return from Menu
            startActivity(intent2);
        });
        Backbutton.setOnClickListener(v -> {
            Intent intent11 = new Intent(Menu.this, MainActivity.class);
            intent11.putExtra("fromMenu", true); // Add a flag to indicate return from Menu
            startActivity(intent11);
        });
        Person.setOnClickListener(v -> {
            Intent intent = new Intent(Menu.this, OBJfollowing.class);
            startActivityForResult(intent, DETECTION_REQUEST_CODE);
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DETECTION_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String detectedObjects = data.getStringExtra("detectedObjects");
//            resultTextView.setText(detectedObjects);

        }
    }
}




