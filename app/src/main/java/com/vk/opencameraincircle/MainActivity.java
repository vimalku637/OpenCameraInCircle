package com.vk.opencameraincircle;

import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends BaseActivity {
    private static final int REQUEST_CODE_USING_CAMERA = 1;
    private static final int REQUEST_CODE_USING_CAMERA2 = 2;
    private static final int REQUEST_CODE_USING_CAMERA_AND_OPENGL = 3;
    private static final String[] CAMERA_PERMISSION = new String[]{
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    public void usingCamera(View view) {
        if (checkPermissions(CAMERA_PERMISSION)) {
            startActivity(new Intent(this, CameraActivity.class));
        } else {
            ActivityCompat.requestPermissions(this, CAMERA_PERMISSION, REQUEST_CODE_USING_CAMERA);
        }
    }

    @Override
    protected void onRequestPermissionResult(int requestCode, boolean isAllGranted) {
        super.onRequestPermissionResult(requestCode, isAllGranted);
        if (isAllGranted) {
            switch (requestCode) {
                case REQUEST_CODE_USING_CAMERA:
                    usingCamera(null);
                    break;
                default:
                    break;
            }
        }else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
        }
    }
}