package com.example.irving.smartfridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import com.example.irving.smartfridge.util.Buzzer;
import com.example.irving.smartfridge.util.EasyDLClassify;
import com.example.irving.smartfridge.util.ItemChangeService;
import com.example.irving.smartfridge.util.LightSwitch;
import com.example.irving.smartfridge.util.MyCamera;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

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
public class PutActivity extends Activity {
    private final static String TAG = "PutActivity";
    private int userId;


    private ImageView imageView;

    private MyCamera mCamera;
    private Handler mCameraHandler;     // for running camera in background
    private HandlerThread mCameraThread;

    private Buzzer buzzer;
    private Gpio mGpioLightSwitch;

    private static final int IDEN_RETURN = 0;
    private Handler identifyHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case IDEN_RETURN:
                    Bundle bundle = msg.getData();
                    String item_name = bundle.getString("item_name");
                    if(item_name.equals("")){
                        Log.e(TAG, "onHandle: upload image error");
                    }else if(item_name.equals("other")){
                        Log.d(TAG, "onHandle: somthing cannot identify");
                    }
                    else{
                        Log.d(TAG, "onHandle: identify success");
                        buzzer.buzz();      // give user a hint that identify process success
                        onItemIdentified(item_name);
                        takePhoto();
                    }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_put);
        Intent intent = getIntent();
        userId = intent.getIntExtra("userId", -1);
        imageView = findViewById(R.id.put_imageView);

        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        mCamera = MyCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener, 1);   // use camera id = 0, which is outside the fridge
        try {
            initGpio();
        }catch (IOException e){
            Log.e(TAG, "onCreate: error when initialize gpio device");
        }
        buzzer.buzz();  // good to begin putting food
        // go into a loop which endless take a photo and identify it
        takePhoto();
    }


    @Override
    protected void onStop() {
        super.onStop();
        // free camera
        if(mCamera != null)
            mCamera.shutDown();

        // free Gpio resource
        if (mGpioLightSwitch!=null) try {
            mGpioLightSwitch.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void initGpio() throws IOException{
        PeripheralManager service = PeripheralManager.getInstance();
        buzzer = new Buzzer();
        mGpioLightSwitch = new LightSwitch().getGpio();  // open with close mode, which take light off as an event
        mGpioLightSwitch.registerGpioCallback(new GpioCallback() {
            @Override
            public boolean onGpioEdge(Gpio gpio) {
                Log.d(TAG, "Button pushed");
                // switch to PutActivity
                buzzer.buzz();      // give user a signal
                finish();   // return to MainActivity
                return true;
            }

            @Override
            public void onGpioError(Gpio gpio, int error) {
                Log.w(TAG, mGpioLightSwitch + ": Error event " + error );
            }
        });
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
            identify(file);


        }
    }

    private void onItemIdentified(final String item_name){
        //TODO
        new Thread(new Runnable() {
            @Override
            public void run() {
                MyApplication application = (MyApplication) getApplicationContext();
                String fridge_id = application.getFridge_id();
                String result = ItemChangeService.putRequest(item_name, fridge_id);
                Log.d(TAG, "onItemIdentified: " + result);
            }
        }).start();
    }

    private void identify(final File file){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EasyDLClassify classifier = new EasyDLClassify();
                    JSONObject jsonObject = classifier.easydlClassifyByFilePath(file.getPath(), 1);
                    JSONArray array =(JSONArray) jsonObject.get("results");
                    JSONObject result = (JSONObject)array.get(0);
                    String result_name = (String) result.get("name");

                    // Handler
                    Message message = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putString("item_name", result_name);
                    message.setData(bundle);
                    identifyHandler.sendMessage(message);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private File convertByteToFile(byte[] imageByte) {
        File f = null;
        try {
            // create a file to write bitmap data
            f = new File(PutActivity.this.getCacheDir(), "photo");
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


}
