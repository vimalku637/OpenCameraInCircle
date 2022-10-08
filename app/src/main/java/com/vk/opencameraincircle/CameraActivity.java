package com.vk.opencameraincircle;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends BaseActivity implements ViewTreeObserver.OnGlobalLayoutListener, CameraListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "CameraActivity";
    private static final int ACTION_REQUEST_PERMISSIONS = 1;
    private CameraHelper cameraHelper;
    private RoundTextureView textureView;
    private RoundBorderView roundBorderView;
    private Button btnCapture,btnNext;
    String base64String="";
    private final int PREFERED_IMAGE_WIDTH_SIZE = 1200;
    //用于调整圆角大小
    private SeekBar radiusSeekBar;
    //默认打开的CAMERA
    //private static final int CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_BACK;
    private static final int CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_FRONT;
    //需要的权限
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        initView();
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraHelper.takeImage(CameraActivity.this);
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tempImageFilePath = CameraHelper.imagesFilesPaths.get(CameraHelper.imagesFilesPaths.size()-1);
                Uri tempImageURI = Uri.fromFile(new File( tempImageFilePath ));
                resizeThanLoadImage(tempImageFilePath, tempImageURI);
            }
        });
    }
    private void resizeThanLoadImage(String tempImageFilePath, Uri tempImageURI){
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), tempImageURI);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(bitmap == null)return;

        int sizeDivisor = 1;

        double imageSize = bitmap.getWidth();
        if(bitmap.getHeight() > bitmap.getWidth())imageSize = bitmap.getHeight();
        sizeDivisor = round(imageSize / PREFERED_IMAGE_WIDTH_SIZE);
        if(sizeDivisor == 0)sizeDivisor = 1;
        Bitmap bitmapScaled = Bitmap.createScaledBitmap(bitmap, (bitmap.getWidth()/sizeDivisor), (bitmap.getHeight()/sizeDivisor), true);
        storeBitmapInFile(bitmapScaled, tempImageFilePath);
        onImageLoaded(tempImageURI);
    }
    private int round(double d){
        double dAbs = Math.abs(d);
        int i = (int) dAbs;
        double result = dAbs - (double) i;
        if(result<0.5){
            return d<0 ? -i : i;
        }else{
            return d<0 ? -(i+1) : i+1;
        }
    }
    private void storeBitmapInFile(Bitmap image, String filePath) {
        File pictureFile = new File(filePath);
        String TAG = "TakePhotosFragment>>>> ";
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }
    private void onImageLoaded(Uri imageUri) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            base64String = ImageUtil.convert(bitmap);
            Log.e("TAG>>>", "onImageLoaded: "+base64String);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        textureView = findViewById(R.id.texture_preview);
        btnCapture = findViewById(R.id.btnCapture);
        btnNext = findViewById(R.id.btnNext);
        textureView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    void initCamera() {
        cameraHelper = new CameraHelper.Builder()
                .cameraListener(this)
                .specificCameraId(CAMERA_ID)
                .previewOn(textureView)
                .previewViewSize(new Point(textureView.getLayoutParams().width, textureView.getLayoutParams().height))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();
        cameraHelper.start();
    }

    @Override
    protected void onRequestPermissionResult(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                initCamera();
            } else {
                Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onGlobalLayout() {
        textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
        int sideLength = Math.min(textureView.getWidth(), textureView.getHeight()) * 3 / 4;
        layoutParams.width = sideLength;
        layoutParams.height = sideLength;
        textureView.setLayoutParams(layoutParams);
        textureView.turnRound();
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
            initCamera();
        }
    }

    @Override
    protected void onPause() {
        if (cameraHelper != null) {
            cameraHelper.stop();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraHelper != null) {
            cameraHelper.start();
        }
    }


    private Camera.Size previewSize;
    private int squarePreviewSize;

    @Override
    public void onCameraOpened(Camera camera, int cameraId, final int displayOrientation, boolean isMirror) {
        previewSize = camera.getParameters().getPreviewSize();
        Log.i(TAG, "onCameraOpened:  previewSize = " + previewSize.width + "x" + previewSize.height);
        squarePreviewSize = Math.min(previewSize.width, previewSize.height);
        //在相机打开时，添加右上角的view用于显示原始数据和预览数据
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //将预览控件和预览尺寸比例保持一致，避免拉伸
                {
                    ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
                    //横屏
                    if (displayOrientation % 180 == 0) {
                        layoutParams.height = layoutParams.width * previewSize.height / previewSize.width;
                    }
                    //竖屏
                    else {
                        layoutParams.height = layoutParams.width * previewSize.width / previewSize.height;
                    }
                    textureView.setLayoutParams(layoutParams);
                }
                if (radiusSeekBar != null) {
                    return;
                }
                roundBorderView = new RoundBorderView(CameraActivity.this);
                ((FrameLayout) textureView.getParent()).addView(roundBorderView, textureView.getLayoutParams());

                radiusSeekBar = new SeekBar(CameraActivity.this);
                radiusSeekBar.setOnSeekBarChangeListener(CameraActivity.this);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                FrameLayout.LayoutParams radiusSeekBarLayoutParams = new FrameLayout.LayoutParams(
                        displayMetrics.widthPixels, displayMetrics.heightPixels / 4
                );

                radiusSeekBarLayoutParams.gravity = Gravity.BOTTOM;
                radiusSeekBar.setLayoutParams(radiusSeekBarLayoutParams);
                ((FrameLayout) textureView.getParent()).addView(radiusSeekBar);
                radiusSeekBar.post(new Runnable() {
                    @Override
                    public void run() {
                        radiusSeekBar.setProgress(radiusSeekBar.getMax());
                    }
                });
            }
        });
    }

    @Override
    public void onPreview(final byte[] nv21, Camera camera) {

    }

    @Override
    public void onCameraClosed() {
        Log.i(TAG, "onCameraClosed: ");
    }

    @Override
    public void onCameraError(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {

    }

    @Override
    protected void onDestroy() {
        if (cameraHelper != null) {
            cameraHelper.release();
        }
        super.onDestroy();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        textureView.setRadius(progress * Math.min(textureView.getWidth(), textureView.getHeight()) / 2 / seekBar.getMax());
        textureView.turnRound();

        roundBorderView.setRadius(progress * Math.min(roundBorderView.getWidth(), roundBorderView.getHeight()) / 2 / seekBar.getMax());
        roundBorderView.turnRound();

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}