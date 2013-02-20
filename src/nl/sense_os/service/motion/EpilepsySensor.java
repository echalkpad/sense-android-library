package nl.sense_os.service.motion;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.SystemClock;
import android.util.Log;

public class EpilepsySensor implements MotionSensorInterface {

    private static final String TAG = "EpilepsySensor";
    private static final long LOCAL_BUFFER_TIME = 15 * 1000;

    private long[] lastLocalSampleTimes = new long[50];
    private long firstTimeSend = 0;
    private JSONArray[] dataBuffer = new JSONArray[10];
    private Context context;

    public EpilepsySensor(Context context) {
        this.context = context;
    }

    @Override
    public boolean isSampleComplete() {
        // never unregister
        return false;
    }

    @Override
    public void onNewData(SensorEvent event) {
        
        // check if this is useful data
        Sensor sensor = event.sensor;
        if (sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }
        Log.v(TAG, "New data from " + MotionSensorUtils.getSensorName(sensor));

        JSONObject json = MotionSensorUtils.createJsonValue(event);

        if (dataBuffer[sensor.getType()] == null) {
            dataBuffer[sensor.getType()] = new JSONArray();
        }
        dataBuffer[sensor.getType()].put(json);
        if (lastLocalSampleTimes[sensor.getType()] == 0) {
            lastLocalSampleTimes[sensor.getType()] = SystemClock.elapsedRealtime();
        }

        if (SystemClock.elapsedRealtime() > lastLocalSampleTimes[sensor.getType()]
                + LOCAL_BUFFER_TIME) {
            // send the stuff
            sendData(sensor);

            // reset data buffer
            dataBuffer[sensor.getType()] = new JSONArray();
            lastLocalSampleTimes[sensor.getType()] = SystemClock.elapsedRealtime();
            if (firstTimeSend == 0) {
                firstTimeSend = SystemClock.elapsedRealtime();
            }
        }
    }

    private void sendData(Sensor sensor) {

        String value = "{\"interval\":"
                + Math.round(LOCAL_BUFFER_TIME / dataBuffer[sensor.getType()].length())
                + ",\"data\":" + dataBuffer[sensor.getType()].toString() + "}";

        // pass message to the MsgHandler
        Intent i = new Intent(context.getString(R.string.action_sense_new_data));
        i.putExtra(DataPoint.SENSOR_NAME, SensorNames.ACCELEROMETER_EPI);
        i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
        i.putExtra(DataPoint.VALUE, value);
        i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON_TIME_SERIES);
        i.putExtra(DataPoint.TIMESTAMP, lastLocalSampleTimes[sensor.getType()]);
        context.startService(i);
    }

    @Override
    public void startNewSample() {
        // not used
    }

}
