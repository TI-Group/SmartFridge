package com.example.irving.smartfridge;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.irving.smartfridge.util.Buzzer;
import com.example.irving.smartfridge.util.LightSwitch;
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

import com.example.irving.smartfridge.util.MyCamera;
import com.youtu.Youtu;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private ImageView imageView;

    private MyCamera mCamera;
    private Handler mCameraHandler;     // for running camera in background
    private HandlerThread mCameraThread;

    private Gpio mGpioPut;
    private Gpio mGpioTake;
    private Gpio mGpioLightSwitch;
    private Gpio mGpioBuzzer;
    private Buzzer buzzer;
    private static final String GPIO_PUT = "BCM21";   // PIN_40
    private static final String GPIO_TAKE = "BCM20";    // PIN_38

    private static String TAG = "myLog";

    // user id
    private int userId = -1;  // identify by camera
    private boolean photo_taken = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.photo);
        // check permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
           requestPermissions(new String[] {Manifest.permission.CAMERA}, 1);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.INTERNET}, 1);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
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
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener, 0);   // use camera id = 0, which is outside the fridge

        // take picture
        //takePhoto();
    }

    @Override
    protected void onResume() {
        super.onResume();
        photo_taken = false;
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
                }
            });
            File file = convertByteToFile(imageBytes);
            int person_id = uploadImg(file);
            if(person_id == -1){
                Log.e(TAG, "onPictureTaken: upload image error");
                return;
            }else{
                Log.d(TAG, "onPictureTaken: identify success");
                buzzer.buzz();      // give user a hint that identify process success
                userId = person_id; // replace userId with new identified person id
            }
        }
    }

    @Override
    protected void onDestroy() {   // when switch to another activity
        super.onDestroy();
        // free camera
        if(mCamera != null)
            mCamera.shutDown();

        // free Gpio resource
        if (mGpioPut!=null) try {
            mGpioPut.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        if (mGpioTake!=null) try {
            mGpioTake.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        if (mGpioLightSwitch!=null) try {
            mGpioLightSwitch.close();
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
        mGpioPut = service.openGpio(GPIO_PUT);
        mGpioTake = service.openGpio(GPIO_TAKE);

        // set Direction
        mGpioPut.setDirection(Gpio.DIRECTION_IN);
        mGpioPut.setEdgeTriggerType(Gpio.EDGE_FALLING);
        mGpioPut.registerGpioCallback(new GpioCallback() {
            @Override
            public boolean onGpioEdge(Gpio gpio) {
                Log.d(TAG, "Button pushed");
                // switch to PutActivity
                Intent intent = new Intent(MainActivity.this, PutActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                return true;
            }

            @Override
            public void onGpioError(Gpio gpio, int error) {
                Log.w(TAG, mGpioPut + ": Error event " + error );
            }
        });

        mGpioTake.setDirection(Gpio.DIRECTION_IN);
        mGpioTake.setEdgeTriggerType(Gpio.EDGE_FALLING);
        mGpioTake.registerGpioCallback(new GpioCallback() {
            @Override
            public boolean onGpioEdge(Gpio gpio) {
                Log.d(TAG, "Button pushed");
                // switch to TakeActivity
                Intent intent = new Intent(MainActivity.this, TakeActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                return true;
            }

            @Override
            public void onGpioError(Gpio gpio, int error) {
                Log.w(TAG, mGpioTake + ": Error event " + error );
            }
        });

        // new hardware
        buzzer = new Buzzer();
        mGpioBuzzer = buzzer.getGpio();
        mGpioLightSwitch = new LightSwitch().getGpio();  // open with close mode, which take light on as an event

        mGpioLightSwitch.registerGpioCallback(new GpioCallback() {
            @Override
            public boolean onGpioEdge(Gpio gpio) {
                Log.d(TAG, "light on detected");
                // switch to PutActivity
                if(!photo_taken) {
                    buzzer.buzz();      // give user a signal
                    takePhoto();
                    photo_taken = true;
                }
                return true;
            }

            @Override
            public void onGpioError(Gpio gpio, int error) {
                Log.w(TAG, mGpioLightSwitch + ": Error event " + error );
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

    private int uploadImg(File file){
        String path = file.getPath();
        MyApplication application = (MyApplication) getApplicationContext();
        Youtu faceYoutu = application.getFaceYoutu();
        String fridge_id = application.getFridge_id();
        try {
            JSONObject response = faceYoutu.FaceIdentify(path, fridge_id);
            JSONArray array = (JSONArray) response.get("candidates");
            JSONObject json = (JSONObject) array.get(0);
            String person_id = (String)json.get("person_id");
            return Integer.getInteger(person_id);
        }catch (Exception e){
            Log.e(TAG, "uploadImg: error when upload image");
            e.printStackTrace();
            return -1;
        }
    }
}
