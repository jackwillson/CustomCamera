package com.customcamera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * For api level lower to KITKAT
 */
public class CustomCameraNewActivity extends AppCompatActivity implements OnClickListener {
    private static final int GALLERY_PICK_CODE = 105;
    private static final int REQUEST_PERMISSION_CODE = 456;
    private static final int CAM_CAPTURE_CODE = 123;

    private Camera camera = null;
    private CameraInfo cameraInfo;

    private int cameraId;
    private int rotation;

    private RequestManager glideRequestManager = null;
    private CameraPreview maPreview = null;
    private FrameLayout frmSurfaceContainer = null;

    private boolean canTake = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aaa);
        glideRequestManager = Glide.with(this);

        // camera surface view created
        cameraId = CameraInfo.CAMERA_FACING_BACK;

        Button btnFlip = (Button) findViewById(R.id.btnFlip);
        Button btnCapture = (Button) findViewById(R.id.btnCapture);
        Button btnGallery = (Button) findViewById(R.id.btnGallery);

        btnFlip.setOnClickListener(this);
        btnCapture.setOnClickListener(this);
        btnGallery.setOnClickListener(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Camera.getNumberOfCameras() > 1) {
            btnFlip.setVisibility(View.VISIBLE);
        }
        frmSurfaceContainer = (FrameLayout) findViewById(R.id.frmSurfaceContainer);
    }

    private void reopenCamera() {
        try {
            if (!openCamera(cameraId)) {
                alertCameraDialog();
            }
            setCameraParams();

            if (maPreview != null) {
                frmSurfaceContainer.removeView(maPreview);
            }
            maPreview = new CameraPreview(this, camera);
            frmSurfaceContainer.addView(maPreview);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (canTake) {
                    canTake = false;
                    takeImage();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            canTake = true;
                        }
                    }, 1500);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasPermissions()) {
            reopenCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        releaseCamera();
    }

    private boolean openCamera(int id) {
        boolean result = false;
        cameraId = id;
        releaseCamera();
        try {
            camera = Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (camera != null) {
            try {
                setUpCamera(camera);
                camera.setErrorCallback(new ErrorCallback() {
                    @Override
                    public void onError(int error, Camera camera) {
                    }
                });
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
                result = false;
                releaseCamera();
            }
        }
        return result;
    }

    private void setUpCamera(Camera camera) {
        try {
            cameraInfo = new CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);
            rotation = getWindowManager().getDefaultDisplay().getRotation();
            int degree = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degree = 0;
                    break;
                case Surface.ROTATION_90:
                    degree = 90;
                    break;
                case Surface.ROTATION_180:
                    degree = 180;
                    break;
                case Surface.ROTATION_270:
                    degree = 270;
                    break;
                default:
                    break;
            }

            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                // frontFacing
                rotation = (cameraInfo.orientation + degree) % 330;
                rotation = (360 - rotation) % 360;
            } else {
                // Back-facing
                rotation = (cameraInfo.orientation - degree + 360) % 360;
            }

            camera.setDisplayOrientation(rotation);
            Parameters params = camera.getParameters();

            List<String> focusModes = params.getSupportedFlashModes();
            if (focusModes != null) {
                if (focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    params.setFlashMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
            }
            params.setRotation(rotation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.setErrorCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("error", e.toString());
            camera = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnFlip:
                flipCamera();
                setCameraParams();

                frmSurfaceContainer.removeView(maPreview);
                maPreview = new CameraPreview(this, camera);
                frmSurfaceContainer.addView(maPreview);

                break;
            case R.id.btnCapture:
                takeImage();
                break;
            case R.id.btnGallery:
                if (hasPermissions()) {
                    Intent galaryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    PackageManager manager = getPackageManager();
                    List<ResolveInfo> infos = manager.queryIntentActivities(galaryIntent, 0);
                    if (infos.size() > 0) {
                        //Then there is an Application(s) can handle your intent
                        startActivityForResult(galaryIntent, GALLERY_PICK_CODE);
                    } else {
                        //No Application can handle your intent
                    }
                }
                break;
            default:
                break;
        }
    }

    private int setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        int result;
        if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            //Front is flipped
            result = (this.rotation + 180 + 2 * rotation + 720) % 360;
        } else {
            result = this.rotation;
        }
        Log.d("Captured rotation", "=" + result);
        return result;
    }

    private void takeImage() {
        try {
            camera.takePicture(null, null, new PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, final Camera camera) {
                    try {
                        glideRequestManager.load(data).asBitmap().into(new SimpleTarget<Bitmap>(getDisplayWidth(), getDisplayHeight()) {
                            @Override
                            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                try {
                                    Matrix rotateMatrix = new Matrix();
                                    // Perform matrix rotations/mirrors depending on camera that took the photo
                                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                                        float[] mirrorY = { -1, 0, 0, 0, 1, 0, 0, 0, 1};
                                        Matrix matrixMirrorY = new Matrix();
                                        matrixMirrorY.setValues(mirrorY);

                                        rotateMatrix.postConcat(matrixMirrorY);
                                    }
                                    rotateMatrix.postRotate(90);

                                    /*Matrix rotateMatrix = new Matrix();
                                    rotateMatrix.postRotate(setCameraDisplayOrientation(CustomCameraNewActivity.this, cameraId, camera));*/

                                    Bitmap rotatedBitmap = Bitmap.createBitmap(resource, 0, 0, resource.getWidth(), resource.getHeight(), rotateMatrix, false);

                                    File file = new File(getDirectoryPath(CustomCameraNewActivity.this), getTimeStamp() + "CAMERA.jpg");
                                    file.createNewFile();
                                    FileOutputStream fos = new FileOutputStream(file);
                                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos);

                                    fos.close();
                                    rotatedBitmap.recycle();
                                    Log.d("Captured path",""+file.getPath());
                                    releaseCamera();
                                    finish();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onLoadStarted(Drawable placeholder) {
                                super.onLoadStarted(placeholder);
                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                super.onLoadFailed(e, errorDrawable);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void flipCamera() {
        int id = (cameraId == CameraInfo.CAMERA_FACING_BACK ? CameraInfo.CAMERA_FACING_FRONT : CameraInfo.CAMERA_FACING_BACK);
        if (!openCamera(id)) {
            alertCameraDialog();
        }
    }

    private void alertCameraDialog() {
        Builder dialog = createAlert(CustomCameraNewActivity.this, getString(R.string.app_name), "Error to open camera");
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private Builder createAlert(Context context, String title, String message) {
        Builder dialog = new Builder(new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog));
        dialog.setIcon(R.mipmap.ic_launcher);
        if (title != null)
            dialog.setTitle(title);
        else
            dialog.setTitle(R.string.app_name);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        return dialog;
    }

    public int getDisplayWidth() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    public int getDisplayHeight() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    private void setCameraParams() {
        try {
            if (camera != null) {
                Parameters param = camera.getParameters();

                if (cameraId == CameraInfo.CAMERA_FACING_BACK) {
                    param.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    param.setFlashMode(Parameters.FLASH_MODE_AUTO);
                }

                List<Camera.Size> pictureSizes = param.getSupportedPictureSizes();
                List<Camera.Size> previewSizes = param.getSupportedPreviewSizes();

                int previewHeight = getDisplayHeight();
                if (previewSizes.size() > 1) {
                    Camera.Size optSize = getOptimalSize(previewSizes, getDisplayWidth(), previewHeight);
                    Log.d("PreviewSize w:h", "=" + optSize.width + ":" + optSize.height);

                    param.setPreviewSize(optSize.width, optSize.height);// get can be value 0 - 5
                }
                if (pictureSizes.size() > 1) {
                    Camera.Size optSize = getOptimalSize(pictureSizes, getDisplayWidth(), previewHeight);
                    Log.d("PictureSize w:h", "=" + optSize.width + ":" + optSize.height);
                    param.setPictureSize(optSize.width, optSize.height);// get can be value 0 - 5
                }
                camera.setParameters(param);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Camera.Size getOptimalSize(List<Camera.Size> sizes, int requireWidth, int requireHeight) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) requireHeight / requireWidth;

        if (sizes == null)
            return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            Log.d("1 PreviewSize w:h", "=" + size.width + ":" + size.height);

            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - requireHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - requireHeight);
                Log.d("optimalSize ASSIGNED", ">>1");
            }
        }

        if (optimalSize == null) {
            Log.d("optimalSize null", ">>2");

            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - requireHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - requireHeight);
                }
            }
        }
        return optimalSize;
    }

    private boolean hasPermissions() {
        ArrayList<String> perArray = getCameraFunctionsPermissions();
        if (perArray.size() > 0) {
            String sPer[] = perArray.toArray(new String[perArray.size()]);
            ActivityCompat.requestPermissions(this, sPer, REQUEST_PERMISSION_CODE);
            return false;
        }
        return true;
    }

    public boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == 0;
    }

    @TargetApi(16)
    public ArrayList<String> getCameraFunctionsPermissions() {
        ArrayList<String> perArray = new ArrayList();
        if (Build.VERSION.SDK_INT > 22) {
            if (!checkPermission("android.permission.CAMERA")) {
                perArray.add("android.permission.CAMERA");
            }
            if (!checkPermission("android.permission.WRITE_EXTERNAL_STORAGE")) {
                perArray.add("android.permission.WRITE_EXTERNAL_STORAGE");
            }
            if (!checkPermission("android.permission.READ_EXTERNAL_STORAGE")) {
                perArray.add("android.permission.READ_EXTERNAL_STORAGE");
            }
        }
        return perArray;
    }

    public String getTimeStamp() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    public String getTimeStampFull() {
        return String.valueOf(System.currentTimeMillis());
    }

    public static String getDirectoryPath(Context context) {
        try {
            File fileDir = new File(Environment.getExternalStorageDirectory().toString() + "/" + context.getPackageName());
            fileDir.mkdirs();
            return fileDir.getPath();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void createFileFromBitmap(Bitmap bitmap, String imageDirPath, String imageName) {
        try {
            new File(imageDirPath).mkdirs();
            OutputStream fOut2 = new FileOutputStream(new File(imageDirPath, imageName));
            try {
                if (bitmap != null) {
                    try {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut2);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                fOut2.close();
            }
        } catch (Exception e5) {
            e5.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case CAM_CAPTURE_CODE:
                    //Need to set profile on previous screen
                    setResult(Activity.RESULT_OK, data);
                    releaseCamera();
                    finish();
                    break;
                case GALLERY_PICK_CODE:
                    if (data != null) {
                        String fileName = getTimeStamp() + ".jpg";
                        String dirPath = getDirectoryPath(CustomCameraNewActivity.this);
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(data.getData());
                            Bitmap newBitMap = BitmapScaler.scaleToFill(BitmapFactory.decodeStream(inputStream), getDisplayWidth(), getDisplayHeight());

                            createFileFromBitmap(newBitMap, dirPath, fileName);
                            if (inputStream != null) {
                                inputStream.close();
                            }
                            Log.d("Captured path",""+dirPath + "/" + fileName);
                            releaseCamera();
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        } else {
            releaseCamera();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            releaseCamera();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}