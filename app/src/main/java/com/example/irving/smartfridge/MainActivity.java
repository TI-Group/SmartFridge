package com.example.irving.smartfridge;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity{
    // some contant
    private static final String UPLOAD_IMAGE_URL = "http://120.78.218.52:8080/fridge/fridgeAction/openFridge";
    private static final String FRIDGE_ID = "";
    private ImageView imageView;
    private MyCamera mCamera;
    private Handler mCameraHandler;     // for running camera in background
    private HandlerThread mCameraThread;

    private Gpio mGpio;
    private static  final String GPIO_NAME = "BCM21";   // PIN_40

    private static String TAG = "myLog";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.photo);
        // check permission
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            Log.e(TAG, "permission problem");
            finish();
        }
        try{
            initGPIO();
        }catch (IOException e){
            Log.w(TAG, "gpio went wrong when initializing");
        }
        
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        mCamera = MyCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

        // take picture
        takePhoto();
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    // get image bytes
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    onPictureTaken(imageBytes);
                }
            };

    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {
            Log.d(TAG, "Picture taken");
            runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    imageView.setImageBitmap(BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length));
                    File file = convertByteToFile(imageBytes);
                    uploadImg(file);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // free camera
        if(mCamera != null)
            mCamera.shutDown();

        // free Gpio resource
        if (mGpio!=null) try {
            mGpio.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
    
    private void takePhoto(){
        while (true){
            if(mCamera.isReady()){
                Log.d(TAG, "camera is ready.");
                mCamera.takePicture();
                Log.d(TAG, "take photo success");
                break;
            }
            else {
                Log.d(TAG, "camera is not ready.");
                try {
                    Thread.sleep(100);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
    

    private void initGPIO() throws IOException{

        PeripheralManager service = PeripheralManager.getInstance();
        mGpio = service.openGpio(GPIO_NAME);

        // set Direction
        mGpio.setDirection(Gpio.DIRECTION_IN);
        mGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
        mGpio.registerGpioCallback(new GpioCallback() {
            @Override
            public boolean onGpioEdge(Gpio gpio) {
                Log.d(TAG, "Button pushed");
                takePhoto();
                return true;
            }

            @Override
            public void onGpioError(Gpio gpio, int error) {
                Log.w(TAG, mGpio + ": Error event " + error );
            }
        });
    }

    private File convertByteToFile(byte[] imageByte) {
        File f = null;
        try {
            // create a file to write bitmap data
            f = new File(MainActivity.this.getCacheDir(), "photo");
            if(f.exists())
                f.delete();
            f.createNewFile();

            // write the bytes in file
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(imageByte);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "error when create file");
        }
        return f;
    }

    private void uploadImg(File file){
        if(file == null){
            Log.w(TAG, "uploadImg: File is null");
            return;
        }
        OkHttpClient client = new OkHttpClient();
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fridgeId", FRIDGE_ID)
                .addFormDataPart("uploadImage", "photo.jpg"
                    , RequestBody.create(MediaType.parse("image/jpg"), file));
        RequestBody requestBody = builder.build();
        Request request = new Request.Builder()
                .url(UPLOAD_IMAGE_URL)
                .post(requestBody)
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "uploadImg onFailure: "+e );
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "成功"+response);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "成功", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
