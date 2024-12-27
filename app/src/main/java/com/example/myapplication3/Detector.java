package com.example.myapplication3;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class Detector {
    private final Context context;
    private final String modelPath;
    private final String labelPath;
    private final DetectorListener detectorListener;

    private Interpreter interpreter = null;
    private final List<String> labels = new ArrayList<>();

    private int tensorWidth = 0;
    private int tensorHeight = 0;
    private int numChannel = 0;
    private int numElements = 0;
//
//    private float recCenterX = 0f;
//    private float recCenterY = 0f;
    float cameraWidth = OverlayView.Cwidth;
    float cameraHeight = OverlayView.Cheight;





    private final ImageProcessor imageProcessor = new ImageProcessor.Builder()
            .add(new NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
            .add(new CastOp(INPUT_IMAGE_TYPE))
            .build();

    public Detector(Context context, String modelPath, String labelPath, DetectorListener detectorListener) {
        this.context = context;
        this.modelPath = modelPath;
        this.labelPath = labelPath;
        this.detectorListener = detectorListener;

    }

    public void setup() {
        try {
            @NonNull MappedByteBuffer model = FileUtil.loadMappedFile(context, modelPath);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            interpreter = new Interpreter(model, options);

            int[] inputShape = interpreter.getInputTensor(0).shape();
            int[] outputShape = interpreter.getOutputTensor(0).shape();

            tensorWidth = inputShape[1];
            tensorHeight = inputShape[2];
            numChannel = outputShape[1];
            numElements = outputShape[2];

            InputStream inputStream = context.getAssets().open(labelPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                labels.add(line);
            }

            reader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }

    public void detect(Bitmap frame, float camerawidth, float cameraheight) {
        if (interpreter == null || tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) {
            return;
        }

        long inferenceTime = SystemClock.uptimeMillis();



        Bitmap resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false);
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(resizedBitmap);
        TensorImage processedImage = imageProcessor.process(tensorImage);
        TensorBuffer imageBuffer = TensorBuffer.createFixedSize(new int[]{1, numChannel, numElements}, OUTPUT_IMAGE_TYPE);

        interpreter.run(processedImage.getBuffer(), imageBuffer.getBuffer());

        List<BoundingBox> rawBoxes =
                bestBox(imageBuffer.getFloatArray());
        List<BoundingBox> finalBoxes = applyNMS(rawBoxes);
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime;

        if (finalBoxes == null || finalBoxes.isEmpty()) {
            BluetoothSingleton.getInstance().sendData("S");
            BluetoothSingleton.getInstance().setCommand("S");
            detectorListener.onNodetect();

            detectorListener.onEmptyDetect();
        } else {
            detectorListener.onDetect(finalBoxes, inferenceTime);

            float screenCenterX = camerawidth/ 2f;
            float screenCenterY = cameraheight / 2f;
            getnsend( finalBoxes,camerawidth,cameraheight);

        }
    }



    private List<BoundingBox> bestBox(float[] array) {
        List<BoundingBox> boundingBoxes = new ArrayList<>();

        for (int c = 0; c < numElements; c++) {
            float maxConf = -1.0f;

            int maxIdx = -1;
            int j = 4;
            int arrayIdx = c + numElements * j;

            while (j < numChannel) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx];
                    maxIdx = j - 4;
                }
                j++;
                arrayIdx += numElements;
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                String clsName = labels.get(maxIdx);
                float cx = array[c];
                float cy = array[c + numElements];
                float w = array[c + numElements * 2];
                float h = array[c + numElements * 3];
                float x1 = cx - (w / 2F);
                float y1 = cy - (h / 2F);
                float x2 = cx + (w / 2F);
                float y2 = cy + (h / 2F);

                if (x1 < 0F || x1 > 1F || y1 < 0F || y1 > 1F || x2 < 0F || x2 > 1F || y2 < 0F) {
                    continue;
                }
                if(maxIdx==0){
                boundingBoxes.add(new BoundingBox(x1, y1, x2, y2, cx, cy, w, h, maxConf, maxIdx, clsName));
            }
            }
        }

        return applyNMS(boundingBoxes);
    }


    public List<BoundingBox> applyNMS(List<BoundingBox> boxes) {
        List<BoundingBox> selectedBoxes = new ArrayList<>();
        List<BoundingBox> boxesCopy = new ArrayList<>(boxes);  // Copy of the original list

        while (!boxesCopy.isEmpty()) {
            // Pick the first box from the list and add it to selectedBoxes
            BoundingBox firstBox = boxesCopy.remove(0);
            selectedBoxes.add(firstBox);

            // Temporary list to collect overlapping boxes
            List<BoundingBox> overlappingBoxes = new ArrayList<>();

            // Check for overlapping boxes
            for (BoundingBox nextBox : boxesCopy) {
                if (calculateIoU(firstBox, nextBox) >= IOU_THRESHOLD) {
                    overlappingBoxes.add(nextBox);
                }
            }

            // Remove all overlapping boxes from boxesCopy
            boxesCopy.removeAll(overlappingBoxes);
        }

        return selectedBoxes;
    }

    private float calculateIoU(BoundingBox box1, BoundingBox box2) {
        float x1 = Math.max(box1.getX1(), box2.getX1());
        float y1 = Math.max(box1.getY1(), box2.getY1());
        float x2 = Math.min(box1.getX2(), box2.getX2());
        float y2 = Math.min(box1.getY2(), box2.getY2());
        float intersectionArea = Math.max(0F, x2 - x1) * Math.max(0F, y2 - y1);
        float box1Area = box1.getW() * box1.getH();
        float box2Area = box2.getW() * box2.getH();
        return intersectionArea / (box1Area + box2Area - intersectionArea);
    }


    public interface DetectorListener {
        void onEmptyDetect();
        float screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        float screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

        void onDetect(List<BoundingBox> boundingBoxes, long inferenceTime);

        void onNodetect();
    }

    private static final float INPUT_MEAN = 0f;
    private static final float INPUT_STANDARD_DEVIATION = 255f;
    private static final DataType INPUT_IMAGE_TYPE = DataType.FLOAT32;
    private static final DataType OUTPUT_IMAGE_TYPE = DataType.FLOAT32;
    private static final float CONFIDENCE_THRESHOLD = 0.75F;
    private static final float IOU_THRESHOLD = 0.35F;





    public static String getnsend(List<BoundingBox> boundingBoxes, float cameraWidth, float cameraHeight) {
        String command = "S"; // Default command
        if (boundingBoxes == null || boundingBoxes.isEmpty()) {
            Log.d("Detector", "ok");
            BluetoothSingleton.getInstance().sendData(command);
            return command;
        }

        BoundingBox largestBox = null;
        double maxArea = 0;

        for (BoundingBox box : boundingBoxes) {
            Log.d("box", "ok " + box);
            if (!"person".equalsIgnoreCase(box.getClsName())) {
                Log.d("class condition", "ok");
                continue;
            }

            float boxWidth = (box.getX2() - box.getX1())*cameraWidth;
            float boxHeight = (box.getY2() - box.getY1())*cameraHeight;
            double area = boxWidth * boxHeight;

            Log.d("area", "ok " + area);
            if (area > maxArea) {
                maxArea = area;
                largestBox = box;
            }
        }

        if (largestBox == null) {
            Log.d("Detector", "No valid 'person' bounding box found. Sending default command.");
            BluetoothSingleton.getInstance().sendData(command);
            return command;
        }

        Log.d("Detector", "Largest Bounding Box: " + largestBox);
        Log.d("Detector", "Largest Box Area: " + maxArea);

        double cameraArea = cameraWidth * cameraHeight;
        double areaDifference = cameraArea - maxArea;

        Log.d("Detector", "Camera Area: " + cameraArea);
        Log.d("Detector", "Area Difference: " + areaDifference);

        float screenCenterX = cameraWidth / 2;
        float threshold = cameraWidth / 6;

        float boxCenterX =(((largestBox.getX1() + largestBox.getX2()) *cameraWidth))/ 2;

        Log.d("Detector", "Box Center X: " + boxCenterX);
        Log.d("Detector", "Screen Center X: " + screenCenterX);
        Log.d("Detector", "Threshold: " + threshold);

        if (maxArea == 0 || areaDifference < 0.3 * cameraArea) {
            command = "S";
            BluetoothSingleton.getInstance().setCommand("S");
        } else if (areaDifference > 0.3*cameraArea) {
            if (boxCenterX > screenCenterX + threshold) {
                command = "R";
                BluetoothSingleton.getInstance().setCommand("R");
            } else if (boxCenterX < screenCenterX - threshold) {
                command = "L";
                BluetoothSingleton.getInstance().setCommand("L");
            } else {
                command = "F";
                BluetoothSingleton.getInstance().setCommand("F");
            }
        }
        else{
            command = "S";
            BluetoothSingleton.getInstance().setCommand("S");
        }


        Log.d("Detector", "Final Command: " + command);
        BluetoothSingleton.getInstance().sendData(command+"*");
        maxArea=0;
        return command;
    }



}
