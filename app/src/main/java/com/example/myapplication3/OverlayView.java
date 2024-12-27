package com.example.myapplication3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.List;



public class OverlayView extends View {

    private List<BoundingBox> results;
    private Paint boxPaint;
    private Paint textBackgroundPaint;
    private Paint textPaint;

    private Rect bounds;


    public static float Cwidth = 0;
    public static float Cheight = 0;






    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
        bounds = new Rect();

        int dpWidth = 700;
        int dpHeight = 400;

        // Convert dp to px based on screen density
        float density = context.getResources().getDisplayMetrics().density;
        Cwidth = (int) (dpWidth * density);
        Cheight = (int) (dpHeight * density);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);



        Log.d("OverlayView", "Width: " + Cwidth + ", Height: " + Cheight);
    }

//    public int getWD(){
//        return  getWidth();
//   }
//   public  int getHT(){
//        return  getWidth();
//   }
    public void clear() {
        textPaint.reset();
        textBackgroundPaint.reset();
        boxPaint.reset();
        invalidate();
        initPaints();
    }

    private void initPaints() {
        textBackgroundPaint = new Paint();
        textBackgroundPaint.setColor(Color.BLACK);
        textBackgroundPaint.setStyle(Paint.Style.FILL);
        textBackgroundPaint.setTextSize(50f);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(50f);

        boxPaint = new Paint();
        boxPaint.setColor(ContextCompat.getColor(getContext(), R.color.theme));
        boxPaint.setStrokeWidth(8f);
        boxPaint.setStyle(Paint.Style.STROKE);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (results != null) {
            for (BoundingBox box : results) {
                float left = box.getX1() * getWidth();
                float top = box.getY1() * getHeight();
                float right = box.getX2() * getWidth();
                float bottom = box.getY2() * getHeight();
                Cwidth = getWidth();
                Cheight = getHeight();


                float cx= (box.getX1() * getWidth() + box.getX2() * getWidth())/2;
                float cy=(box.getY1() * getHeight()+box.getY2() * getHeight())/2;
                canvas.drawCircle(Cwidth/2,Cheight/2,10,boxPaint);
                Log.d("camera height and width",getWidth()/2 +"  camera width" +getHeight()/2 + " camera Height");

                canvas.drawRect(left, top, right, bottom, boxPaint);
               canvas.drawCircle(cx,cy, 10,boxPaint);
                String drawableText = box.getClsName();

                textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length(), bounds);
                int textWidth = bounds.width();
                int textHeight = bounds.height();

                canvas.drawRect(
                        left,
                        top,
                        left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                        top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                        textBackgroundPaint
                );
                canvas.drawText(drawableText, left, top + bounds.height(), textPaint);
            }
        }
    }



    public void setResults(List<BoundingBox> boundingBoxes) {
        this.results = boundingBoxes;
        invalidate();
    }
    public void clearBoundingBox() {
        if (results!=null){
            results.clear();
        }
        invalidate();
    }

    float camerawidth = getWidth();
    float camerahieght = getHeight();

    public float getCamerawidth() {
        return camerawidth;
    }

    public float getCamerahieght() {
        return camerahieght;
    }

    private static final int BOUNDING_RECT_TEXT_PADDING = 8;
}
