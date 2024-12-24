package com.example.myapplication3;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Objects;

public class ocrimage extends AppCompatActivity {
    TextRecognizer recognizer;
    TextView textshow,first;
    ImageView okayimage;
    Button click;
    int flag;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocrimage);
        click = findViewById(R.id.Camera);
        textshow = findViewById(R.id.text);
        first = findViewById(R.id.textView2);
        okayimage = findViewById(R.id.myphoto);
        flag = 0;
        okayimage.setVisibility(View.INVISIBLE);
        int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if(currentMode==Configuration.UI_MODE_NIGHT_YES){
            textshow.setTextColor(Color.WHITE);
            first.setTextColor(Color.WHITE);
        }
        else{
            textshow.setTextColor(Color.BLACK);
            first.setTextColor(Color.BLACK);
        }
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag=1;
                if(checkSelfPermission(android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.CAMERA},2);//agr camera ki permission nahi hai to mango
            }
            else{
                Intent get = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(get,1);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode ==1&&data!=null){
            Bitmap map = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
            okayimage.setImageBitmap(map);
            okayimage.setVisibility(View.VISIBLE);
            first.setVisibility(View.INVISIBLE);
            InputImage image = InputImage.fromBitmap(map,0);
            recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
                @Override
                public void onSuccess(Text text) {
                    String gottext = text.getText();
                    textshow.setText(gottext);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ocrimage.this, "TEXT RECOGNIZATION FAILED", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}