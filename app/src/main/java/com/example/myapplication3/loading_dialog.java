package com.example.myapplication3;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;

public class loading_dialog  {
     static Activity activity;
private static AlertDialog dialog;
    loading_dialog(Activity myActivity){
        activity=myActivity;

    }

    public static void startloading() {    AlertDialog.Builder builder =new AlertDialog.Builder(activity);

        LayoutInflater inflater =activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.custom_dialog,null);
        builder.setView(view);
        builder.setCancelable(true);
        dialog= builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();;
    }

     static void dismissloading()  {
     dialog.dismiss();}
}