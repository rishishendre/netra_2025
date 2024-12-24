package com.example.myapplication3;

import static com.example.myapplication3.Constants.LABELS_PATH;
import static com.example.myapplication3.Constants.MODEL_PATH;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.example.myapplication3.databinding.ActivityObjfollowingBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OBJfollowing extends AppCompatActivity implements Detector.DetectorListener {

    private ActivityObjfollowingBinding binding;
    private boolean isFrontCamera = false;

    private Preview preview;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private Detector detector;
    TextView command;

    private ExecutorService cameraExecutor;
    private Set<String> previouslyDetectedObjects = new HashSet<>();
    private static final String TAG = "YOLOApp";

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA))) {
                    Log.d(TAG, "Camera permission granted.");
                    try {
                        startCamera();
                    } catch (Exception e) {
                        Log.e(TAG, "Exception in startCamera(): ", e);
                    }
                } else {
                    Log.d(TAG, "Camera permission denied.");
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate  (savedInstanceState);
        binding = ActivityObjfollowingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "Activity created and layout set.");

        detector = new Detector(getBaseContext(), MODEL_PATH, LABELS_PATH, this);
        detector.setup();
        Log.d(TAG, "Detector setup completed.");
        command = findViewById(R.id.Command);
        command.setText(BluetoothSingleton.getInstance().getCommand());
        if (allPermissionsGranted()) {
            Log.d(TAG, "All permissions granted. Starting camera...");
            try {
                startCamera();

            } catch (Exception e) {
                Log.e(TAG, "Exception in startCamera(): ", e);
            }
        } else {
            Log.d(TAG, "Requesting permissions...");
            requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        Log.d(TAG, "Starting camera...");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                Log.d(TAG, "CameraProvider obtained.");
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: ", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        Log.d(TAG, "Binding camera use cases...");
        if (cameraProvider == null) {
            Log.e(TAG, "Camera initialization failed.");
            throw new IllegalStateException("Camera initialization failed.");
        }

        int rotation = binding.viewFinder.getDisplay().getRotation();
        Log.d(TAG, "Display rotation: " + rotation);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
              .setTargetRotation(rotation)
                .build();

        imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(rotation)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalyzer.setAnalyzer(cameraExecutor, imageProxy -> {
            Log.d(TAG, "Analyzing image...");
            Bitmap bitmapBuffer = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
            bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());

            Matrix matrix = new Matrix();
            matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
            Log.d(TAG, "Image rotation degrees: " + imageProxy.getImageInfo().getRotationDegrees());

            if (isFrontCamera) {
                matrix.postScale(-1f, 1f, imageProxy.getWidth(), imageProxy.getHeight());
            }

            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.getWidth(), bitmapBuffer.getHeight(), matrix, true);
            detector.detect(rotatedBitmap, detector.cameraWidth, detector.cameraHeight);
            Log.d(TAG, "Detection performed on rotated bitmap.");
            imageProxy.close();
        });

        cameraProvider.unbindAll();

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
            preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
            Log.d(TAG, "Camera bind complete.");
        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : new String[]{Manifest.permission.CAMERA}) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission " + permission + " not granted.");
                return false;
            }
        }
        Log.d(TAG, "All required permissions granted.");
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detector.clear();
        cameraExecutor.shutdown();
        BluetoothSingleton.getInstance().sendData("S*");
        Log.d(TAG, "Activity destroyed, resources cleaned up.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            Log.d(TAG, "Camera unbound on pause.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resuming activity...");
        if (allPermissionsGranted()) {
            try {
                startCamera();
            } catch (Exception e) {
                Log.e(TAG, "Exception in startCamera(): ", e);
            }
        } else {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }
    }

    @Override
    public void onEmptyDetect() {
        Log.d(TAG, "No objects detected.");
        binding.overlay.invalidate();
    }

    @Override
    public void onDetect(@NonNull List<BoundingBox> boundingBoxes, long inferenceTime) {
        Log.d(TAG, "Objects detected. Inference time: " + inferenceTime + "ms");

        runOnUiThread(() -> {
            binding.inferenceTime.setText(inferenceTime + "ms");
            binding.overlay.setResults(boundingBoxes);
            binding.overlay.invalidate();
        });

        Set<String> currentDetectedObjects = new HashSet<>();
        for (BoundingBox box : boundingBoxes) {
            String className = box.getClsName();
            currentDetectedObjects.add(className);
            Log.d(TAG, "Detected Object: " + className);
        }

        StringBuilder detectedObj = new StringBuilder();
        for (String obj : currentDetectedObjects) {
            if (detectedObj.length() > 0) {
                detectedObj.append(", ");
            }
            detectedObj.append(obj);
        }


    }

}
