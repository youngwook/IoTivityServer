package com.example.iotivityserver.iotivityserver;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.example.iotivityserver.iotivityserver.resources.BMP280Sensor;
import com.google.android.things.contrib.driver.bmx280.Bmx280;

import org.iotivity.base.ModeType;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.PlatformConfig;
import org.iotivity.base.QualityOfService;
import org.iotivity.base.ServiceType;

import java.io.IOException;

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
public class MainActivity extends Activity {

    private String TAG = "Android IoTivity MainActivity";
    private Bmx280 bmx280;
    BMP280Sensor bmp280Sensor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startIoTivityServer();
    }

    protected void onDestroy(){
        super.onDestroy();

        Log.d(TAG, "unregister resource");
        try {
            bmp280Sensor.unregisterResource();
        } catch (OcException e) {
            Log.d(TAG, "unrigister error :" + e.toString());
        }

        Log.d(TAG, "disconnect with sensor");
        if(bmx280 != null){
            try{
                bmx280.setMode(Bmx280.MODE_SLEEP);
                bmx280.close();
            }catch (IOException e){
                Log.d(TAG, "failed stop sensor");
            }
        }

    }
    public void startIoTivityServer(){
        Log.d(TAG,"start bmp280 sensor");
        try{
            bmx280 = new Bmx280("I2C1");
            bmx280.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
            bmx280.setMode(Bmx280.MODE_NORMAL);
        }catch (IOException e){
            Log.d(TAG, "failed start sensor");
        }

        Log.d(TAG, "initialize server");

        PlatformConfig servercfg = new PlatformConfig(this, ServiceType.IN_PROC,
                ModeType.SERVER,"0.0.0.0",5683, QualityOfService.LOW);
        OcPlatform.Configure(servercfg);
        bmp280Sensor = new BMP280Sensor("/t/temperature", "test", bmx280);
        try {
            bmp280Sensor.registerResource();
        } catch (OcException e) {
            Log.d(TAG, e.toString());
        }
    }
}
