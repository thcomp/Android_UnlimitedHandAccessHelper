package jp.co.thcomp.unlimitedhand;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import jp.co.thcomp.bluetoothhelper.BluetoothAccessHelper;
import jp.co.thcomp.unlimitedhand.data.AbstractSensorData;
import jp.co.thcomp.unlimitedhand.data.AccelerationData;
import jp.co.thcomp.unlimitedhand.data.AngleData;
import jp.co.thcomp.unlimitedhand.data.CalibrationData;
import jp.co.thcomp.unlimitedhand.data.GyroData;
import jp.co.thcomp.unlimitedhand.data.PhotoReflectorData;
import jp.co.thcomp.unlimitedhand.data.QuaternionData;
import jp.co.thcomp.unlimitedhand.data.SharpnessData;
import jp.co.thcomp.unlimitedhand.data.TemperatureData;
import jp.co.thcomp.unlimitedhand.data.VoltageData;
import jp.co.thcomp.util.LogUtil;
import jp.co.thcomp.util.ThreadUtil;

public class UhAccessHelper {
    public static final String TAG = UhAccessHelper.class.getSimpleName();
    private static final String DEFAULT_UH_NAME_PATTERN = "RNBT-[\\w]{4}";
    private static final int DEFAULT_EMS_VOLTAGE_LEVEL = 8;
    private static final int DEFAULT_EMS_SHARPNESS_LEVEL = 15;
    private static final String LINE_SEPARATOR = "\n";

    public static final int DEFAULT_POLLING_RATE_PER_SECOND = 30;
    public static final int DEFAULT_AMBIENT_LIGHT_POLLING = SensorManager.SENSOR_DELAY_UI;

    public static final int POLLING_PHOTO_REFLECTOR = 1;
    public static final int POLLING_ANGLE = 2;
    public static final int POLLING_TEMPERATURE = 4;
    public static final int POLLING_ACCELERATION = 8;
    public static final int POLLING_GYRO = 16;
    public static final int POLLING_QUATERNION = 32;
    public static final int POLLING_ALL = POLLING_PHOTO_REFLECTOR | POLLING_ANGLE | POLLING_TEMPERATURE | POLLING_ACCELERATION | POLLING_GYRO | POLLING_QUATERNION;
    private static boolean sDebug = false;

    public static void enableDebug(boolean enable) {
        sDebug = enable;
        BluetoothAccessHelper.enableDebug(enable);
    }

    public static boolean isEnableDebug() {
        return sDebug;
    }

    public interface OnSensorPollingListener {
        void onPollSensor(AbstractSensorData[] sensorDataArray);
    }

    public enum ConnectResult {
        ErrNoSupportBT,
        ErrNotPairedUnlimitedHand,
        ErrUnknown,
        PairedWithoutConnection,
        Connected,
    }

    public enum SendCommand {
        Vibrate("b", 0),
        EMS_Pad0("0", 0),
        EMS_Pad1("1", 0),
        EMS_Pad2("2", 0),
        EMS_Pad3("3", 0),
        EMS_Pad4("4", 0),
        EMS_Pad5("5", 0),
        EMS_Pad6("6", 0),
        EMS_Pad7("7", 0),
        PhotoSensor("c", Integer.parseInt("000001", 2)),
        Angle("A", Integer.parseInt("000010", 2)),
        Temperature("A", 0),
        Acceleration("a", Integer.parseInt("000100", 2)),
        Gyro("a", Integer.parseInt("001000", 2)),
        Quaternion("q", Integer.parseInt("010000", 2)),
        UpSharpnessLevel("t", 0),
        DownSharpnessLevel("u", 0),
        UpVoltageLevel("h", 0),
        DownVoltageLevel("l", 0),;

        private String mCode;
        private int mMultiCmdBit = 0;

        SendCommand(String code, int multiCmdBit) {
            mCode = code;
            mMultiCmdBit = multiCmdBit;
        }

        byte[] getLineCode() {
            return getCode();
        }

        byte[] getCode() {
            return mCode.getBytes();
        }

        int getMultiCommandbit() {
            return mMultiCmdBit;
        }
    }

    public enum PhotoReflector {
        PR_0,
        PR_1,
        PR_2,
        PR_3,
        PR_4,
        PR_5,
        PR_6,
        PR_7,
    }

    public enum Axis {
        X,
        Y,
        Z,
    }

    private enum AccessStatus {
        NoSupportBT,
        Init,
        LaunchBTAccessHelper,
        PairedUnlimitedHand,
        ConnectedUnlimitedHand,
    }

    private Context mContext;
    private BluetoothAccessHelper mBTAccessHelper;
    private AccessStatus mAccessStatus = AccessStatus.Init;
    private BluetoothDevice mUnlimitedHand = null;
    private final ThreadUtil.OnetimeSemaphore mConnectSemaphore = new ThreadUtil.OnetimeSemaphore();
    private final ThreadUtil.OnetimeSemaphore mSendSemaphore = new ThreadUtil.OnetimeSemaphore();
    private int mCurrentSharpnessLevel = DEFAULT_EMS_SHARPNESS_LEVEL;
    private int mCurrentVoltageLevel = DEFAULT_EMS_VOLTAGE_LEVEL;
    private long mPollingRatePerSecond = DEFAULT_POLLING_RATE_PER_SECOND;
    private HandlerThread mBtHelperNotifyThread;
    private Handler mBtHelperNotifyHandler;
    private Thread mSensorPollingThread;
    private final HashMap<OnSensorPollingListener, Integer> mPollingListenerMap = new HashMap<OnSensorPollingListener, Integer>();
    private int mPollingTargetFlag = 0;
    //private CalibrationData mCalibrationData;
    private HashMap<CalibrationCondition, CalibrationData> mCalibrationDataMap = new HashMap<CalibrationCondition, CalibrationData>();
    private UhCalibrator mCalibrator;
    private CalibrationStatus mCalibrationStatus = CalibrationStatus.Init;
    private final ArrayList<OnCalibrationStatusChangeListener> mOnCalibrationStatusChangeListenerList = new ArrayList<OnCalibrationStatusChangeListener>();

    public UhAccessHelper(Context context) {
        if (context == null) {
            throw new NullPointerException("context == null");
        }

        mContext = context;
        mBTAccessHelper = new BluetoothAccessHelper(context, UhAccessHelper.class.getName());
        mBTAccessHelper.setOnBluetoothStatusListener(mBTStatusListener);
        mBTAccessHelper.setOnNotifyResultListener(mBTNotifyResultListener);
    }

    public List<BluetoothDevice> getDevices() {
        return getDevices(DEFAULT_UH_NAME_PATTERN, true);
    }

    public List<BluetoothDevice> getDevices(String deviceName, boolean useRegExp) {
        ArrayList<BluetoothDevice> ret = new ArrayList<BluetoothDevice>();
        Set<BluetoothDevice> deviceSet = mBTAccessHelper.getPairedDevices();
        BluetoothDevice[] deviceArray = deviceSet != null ? deviceSet.toArray(new BluetoothDevice[deviceSet.size()]) : new BluetoothDevice[0];
        Method compareMethod = null;

        try {
            if (useRegExp) {
                compareMethod = String.class.getMethod("matches", String.class);
            } else {
                compareMethod = Object.class.getMethod("equals", Object.class);
            }
        } catch (NoSuchMethodException e) {
            LogUtil.exception(TAG, e);
        }

        for (BluetoothDevice device : deviceArray) {
            try {
                if ((boolean) compareMethod.invoke(device.getName(), deviceName)) {
                    ret.add(device);
                    break;
                }
            } catch (IllegalAccessException e) {
                LogUtil.exception(TAG, e);
            } catch (InvocationTargetException e) {
                LogUtil.exception(TAG, e);
            }
        }

        return ret;
    }

    public ConnectResult connect() {
        return connect(DEFAULT_UH_NAME_PATTERN, true);
    }

    public ConnectResult connect(String deviceName) {
        return connect(deviceName, false);
    }

    public void disconnect() {
        mBTAccessHelper.stopBluetoothHelper();
        mAccessStatus = AccessStatus.Init;

        if (mBtHelperNotifyThread != null) {
            mBtHelperNotifyThread.quit();
            mBtHelperNotifyThread = null;
        }
    }

    public void startCalibration(Context context, final CalibrationCondition condition, final OnCalibrationStatusChangeListener listener) {
        if (condition == null) {
            throw new NullPointerException("condition == null");
        }

        synchronized (this) {
            switch (mCalibrationStatus) {
                case Init:
                case CalibrateFail:
                case CalibrateSuccess:
                    // start new caliration
                    mCalibrationStatus = CalibrationStatus.Calibrating;
                    mCalibrator = new UhCalibrator(mContext, this, sDebug);
                    mOnCalibrationStatusChangeListenerList.add(listener);
                    mCalibrator.setOnCalibrationStatusChangeListener(new OnCalibrationStatusChangeListener() {
                        @Override
                        public void onCalibrationStatusChange(CalibrationCondition calibrationCondition, CalibrationStatus status) {
                            mCalibrationStatus = status;

                            synchronized (UhAccessHelper.this) {
                                try {
                                    if (status == CalibrationStatus.CalibrateSuccess) {
                                        CalibrationData calibrationData = new CalibrationData();
                                        mCalibrator.getCalibrationData(calibrationData);
                                        mCalibrationDataMap.put(calibrationCondition, calibrationData);
                                        LogUtil.d(TAG, calibrationData.toString());
                                    }

                                    for (OnCalibrationStatusChangeListener onCalibrationStatusChangeListener : mOnCalibrationStatusChangeListenerList) {
                                        onCalibrationStatusChangeListener.onCalibrationStatusChange(condition, status);
                                    }
                                } finally {
                                    mCalibrator = null;
                                }
                            }
                        }
                    });
                    mCalibrator.startCalibration(condition);
                    break;
                case Calibrating:
                    mOnCalibrationStatusChangeListenerList.add(listener);
                    break;
                default:
                    break;
            }
        }
    }

    public void stopCalibration() {
        synchronized (this) {
            if (mCalibrator != null) {
                mCalibrator.stopCalibration();
                mCalibrator = null;
            }
        }
    }

    public boolean isCalibrated(CalibrationCondition condition) {
        return mCalibrationDataMap.containsKey(condition);
    }

    public void setPollingRatePerSecond(int pollingRatePerSecond) {
        mPollingRatePerSecond = pollingRatePerSecond;
    }

    public void startPollingSensor(OnSensorPollingListener listener, int pollingTargetFlag) {
        synchronized (mPollingListenerMap) {
            mPollingListenerMap.put(listener, pollingTargetFlag);
            mPollingTargetFlag |= pollingTargetFlag;

            if (mSensorPollingThread == null) {
                mSensorPollingThread = new Thread(mPollingSensorRunnable);
                mSensorPollingThread.start();
            }
        }
    }

    public void stopPollingSensor(OnSensorPollingListener listener) {
        synchronized (mPollingListenerMap) {
            mPollingListenerMap.remove(listener);

            if (mPollingListenerMap.size() == 0) {
                mSensorPollingThread = null;
            } else {
                // change polling target
                int pollingTargetFlag = 0;
                for (int tempPollingTargetFlag : mPollingListenerMap.values()) {
                    pollingTargetFlag |= tempPollingTargetFlag;
                }
                mPollingTargetFlag = pollingTargetFlag;
            }
        }
    }

    /**
     * 光学センサー読み取り
     *
     * @param data
     * @return
     */
    public boolean readPhotoReflector(PhotoReflectorData data) {
        boolean ret = false;

        if (data != null) {
            switch (mAccessStatus) {
                case PairedUnlimitedHand:
                case ConnectedUnlimitedHand:
                    mSendSemaphore.initialize();
                    if (mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.PhotoSensor.getLineCode())) {
                        mSendSemaphore.start();
                        ret = data.expandRawData(readData());
                    }
                    break;
            }
        }

        return ret;
    }

    /**
     * 角度読み取り
     * UHへの読み取り命令は温度と一緒で、一緒にデータが返却される
     *
     * @param data
     * @return
     */
    public boolean readAngle(AngleData data) {
        boolean ret = false;

        if (data != null) {
            switch (mAccessStatus) {
                case PairedUnlimitedHand:
                case ConnectedUnlimitedHand:
                    mSendSemaphore.initialize();
                    if (mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.Angle.getLineCode())) {
                        mSendSemaphore.start();
                        ret = data.expandRawData(readData());
                    }
                    break;
            }
        }

        return ret;
    }

    /**
     * 温度読み取り
     * UHへの読み取り命令は角度と一緒で、一緒にデータが返却される
     *
     * @param data
     * @return
     */
    public boolean readTemperature(TemperatureData data) {
        boolean ret = false;

        if (data != null) {
            switch (mAccessStatus) {
                case PairedUnlimitedHand:
                case ConnectedUnlimitedHand:
                    mSendSemaphore.initialize();
                    if (mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.Temperature.getLineCode())) {
                        mSendSemaphore.start();
                        ret = data.expandRawData(readData());
                    }
                    break;
            }
        }

        return ret;
    }

    /**
     * 加速度センサー値読み取り
     * UHへの読み取り命令はジャイロセンサーと一緒で、一緒にデータが返却される
     *
     * @param data
     * @return
     */
    public boolean readAcceleration(AccelerationData data) {
        boolean ret = false;

        if (data != null) {
            switch (mAccessStatus) {
                case PairedUnlimitedHand:
                case ConnectedUnlimitedHand:
                    mSendSemaphore.initialize();
                    if (mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.Acceleration.getLineCode())) {
                        mSendSemaphore.start();
                        ret = data.expandRawData(readData());
                    }
                    break;
            }
        }

        return ret;
    }

    /**
     * ジャイロセンサー値読み取り
     * UHへの読み取り命令は加速度センサーと一緒で、一緒にデータが返却される
     *
     * @param data
     * @return
     */
    public boolean readGyro(GyroData data) {
        boolean ret = false;

        if (data != null) {
            switch (mAccessStatus) {
                case PairedUnlimitedHand:
                case ConnectedUnlimitedHand:
                    mSendSemaphore.initialize();
                    if (mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.Gyro.getLineCode())) {
                        mSendSemaphore.start();
                        ret = data.expandRawData(readData());
                    }
                    break;
            }
        }

        return ret;
    }

    /**
     * クォータニオン値読み取り
     * UHへの読み取り命令は加速度センサーと一緒で、一緒にデータが返却される
     *
     * @param data
     * @return
     */
    public boolean readQuaternion(QuaternionData data) {
        boolean ret = false;

        if (data != null) {
            switch (mAccessStatus) {
                case PairedUnlimitedHand:
                case ConnectedUnlimitedHand:
                    mSendSemaphore.initialize();
                    if (mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.Quaternion.getLineCode())) {
                        mSendSemaphore.start();
                        ret = data.expandRawData(readData());
                    }
                    break;
            }
        }

        return ret;
    }

    /**
     * 複数センサー読み込み
     *
     * @param cmdBitFlag
     * @param photoReflectorData
     * @param angleData
     * @param accelerationData
     * @param gyroData
     * @param quaternionData
     * @return
     */
    protected int readAnySensor(byte cmdBitFlag, PhotoReflectorData photoReflectorData, AngleData angleData, AccelerationData accelerationData, GyroData gyroData, QuaternionData quaternionData) {
        int retFlag = 0;

        if (cmdBitFlag > 0) {
            switch (mAccessStatus) {
                case PairedUnlimitedHand:
                case ConnectedUnlimitedHand:
                    mSendSemaphore.initialize();
                    if (mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, new byte[]{cmdBitFlag})) {
                        mSendSemaphore.start();

                        byte[] rawData = readData();

                        if (rawData != null && rawData.length > 0) {
                            String responseData = new String(rawData);
                            String[] eachCmdResponseDataArray = responseData.split("_");

                            for (String cmdResponseData : eachCmdResponseDataArray) {
                                if (cmdResponseData != null && cmdResponseData.length() > 0) {
                                    String[] keyValueArray = cmdResponseData.split(":");
                                    if (keyValueArray != null && keyValueArray.length >= 2) {
                                        AbstractSensorData sensorData = null;
                                        int multiCmdBit = 0;

                                        switch (keyValueArray[0]) {
                                            case "PR":
                                                sensorData = photoReflectorData;
                                                multiCmdBit = SendCommand.PhotoSensor.getMultiCommandbit();
                                                break;
                                            case "ANGLE":
                                                sensorData = angleData;
                                                multiCmdBit = SendCommand.Angle.getMultiCommandbit();
                                                break;
                                            case "ACCEL":
                                                sensorData = accelerationData;
                                                multiCmdBit = SendCommand.Acceleration.getMultiCommandbit();
                                                break;
                                            case "GYRO":
                                                sensorData = gyroData;
                                                multiCmdBit = SendCommand.Gyro.getMultiCommandbit();
                                                break;
                                            case "QUAT":
                                                sensorData = quaternionData;
                                                multiCmdBit = SendCommand.Quaternion.getMultiCommandbit();
                                                break;
                                        }

                                        if (sensorData != null) {
                                            keyValueArray[1] = keyValueArray[1].replaceAll(",", sensorData.getRawDataSeparator());
                                            if (sensorData.expandRawData(keyValueArray[1].getBytes())) {
                                                retFlag |= multiCmdBit;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
        }

        return retFlag;
    }

    public boolean vibrate() {
        boolean ret = false;

        switch (mAccessStatus) {
            case PairedUnlimitedHand:
            case ConnectedUnlimitedHand:
                ret = mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.Vibrate.getLineCode());
                break;
        }

        return ret;
    }

    public int currentSharpnessLevel() {
        return mCurrentSharpnessLevel;
    }

    public boolean upSharpnessLevel() {
        boolean ret = false;

        switch (mAccessStatus) {
            case PairedUnlimitedHand:
            case ConnectedUnlimitedHand:
                mSendSemaphore.initialize();
                if ((ret = mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.UpSharpnessLevel.getLineCode()))) {
                    mSendSemaphore.start();

                    SharpnessData data = new SharpnessData();
                    data.expandRawData(readData());
                    mCurrentSharpnessLevel = data.getRawValue(0);
                }
                break;
        }

        return ret;
    }

    public boolean downSharpnessLevel() {
        boolean ret = false;

        switch (mAccessStatus) {
            case PairedUnlimitedHand:
            case ConnectedUnlimitedHand:
                mSendSemaphore.initialize();
                if ((ret = mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.DownSharpnessLevel.getLineCode()))) {
                    mSendSemaphore.start();

                    SharpnessData data = new SharpnessData();
                    data.expandRawData(readData());
                    mCurrentSharpnessLevel = data.getRawValue(0);
                }
                break;
        }

        return ret;
    }

    public int currentVoltageLevel() {
        return mCurrentVoltageLevel;
    }

    public boolean upVoltageLevel() {
        boolean ret = false;

        switch (mAccessStatus) {
            case PairedUnlimitedHand:
            case ConnectedUnlimitedHand:
                mSendSemaphore.initialize();

                if ((ret = mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.UpVoltageLevel.getLineCode()))) {
                    mSendSemaphore.start();

                    VoltageData data = new VoltageData();
                    data.expandRawData(readData());
                    mCurrentVoltageLevel = data.getRawValue(0);
                }
                break;
        }

        return ret;
    }

    public boolean downVoltageLevel() {
        boolean ret = false;

        switch (mAccessStatus) {
            case PairedUnlimitedHand:
            case ConnectedUnlimitedHand:
                mSendSemaphore.initialize();

                if ((ret = mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.DownVoltageLevel.getLineCode()))) {
                    mSendSemaphore.start();

                    VoltageData data = new VoltageData();
                    data.expandRawData(readData());
                    mCurrentVoltageLevel = data.getRawValue(0);
                }
                break;
        }

        return ret;
    }

    public boolean electricMuscleStimulation(int padNum) {
        boolean ret = false;

        switch (mAccessStatus) {
            case PairedUnlimitedHand:
            case ConnectedUnlimitedHand: {
                String enumName = "EMS_Pad" + String.valueOf(padNum);
                Field enumField = null;

                try {
                    enumField = SendCommand.class.getDeclaredField(enumName);
                    ret = mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, ((SendCommand) enumField.get(null)).getLineCode());
                } catch (NoSuchFieldException e) {
                    LogUtil.e(TAG, e.getLocalizedMessage());
                } catch (IllegalAccessException e) {
                    LogUtil.e(TAG, e.getLocalizedMessage());
                }
                break;
            }
        }

        return ret;
    }

    HashMap<CalibrationCondition, CalibrationData> getCalibrationDataMap() {
        return mCalibrationDataMap;
    }

    private byte[] readData() {
        if (UhAccessHelper.sDebug) {
            LogUtil.d(TAG, this.getClass().getSimpleName() + ": readData(S)");
        }
        byte[] ret = mBTAccessHelper.readLine(mUnlimitedHand, BluetoothAccessHelper.BT_SERIAL_PORT, "\r\n".getBytes());

        if (UhAccessHelper.sDebug) {
            LogUtil.d(TAG, this.getClass().getSimpleName() + ": readData(E)");
        }
        return ret;
    }

    private synchronized ConnectResult connect(String deviceName, boolean useRegExp) {
        ConnectResult ret = ConnectResult.ErrUnknown;
        boolean continueConnection = true;

        while (continueConnection) {
            switch (mAccessStatus) {
                case Init:
                    if (mBtHelperNotifyThread != null) {
                        mBtHelperNotifyThread.quit();
                    }
                    mBtHelperNotifyThread = new HandlerThread(TAG);
                    mBtHelperNotifyThread.start();
                    mBtHelperNotifyHandler = new Handler(mBtHelperNotifyThread.getLooper());
                    mConnectSemaphore.initialize();
                    mBTAccessHelper.setNotifyHandler(mBtHelperNotifyHandler);
                    mBTAccessHelper.startBluetoothHelper();
                    mConnectSemaphore.start();
                    break;
                case LaunchBTAccessHelper: {
                    Set<BluetoothDevice> deviceSet = mBTAccessHelper.getPairedDevices();
                    BluetoothDevice[] deviceArray = deviceSet != null ? deviceSet.toArray(new BluetoothDevice[deviceSet.size()]) : new BluetoothDevice[0];
                    Method compareMethod = null;

                    try {
                        if (useRegExp) {
                            compareMethod = String.class.getMethod("matches", String.class);
                        } else {
                            compareMethod = Object.class.getMethod("equals", Object.class);
                        }
                    } catch (NoSuchMethodException e) {
                        LogUtil.exception(TAG, e);
                    }

                    for (BluetoothDevice device : deviceArray) {
                        try {
                            if ((boolean) compareMethod.invoke(device.getName(), deviceName)) {
                                mUnlimitedHand = device;
                                break;
                            }
                        } catch (IllegalAccessException e) {
                            LogUtil.exception(TAG, e);
                        } catch (InvocationTargetException e) {
                            LogUtil.exception(TAG, e);
                        }
                    }

                    if (mUnlimitedHand != null) {
                        mAccessStatus = AccessStatus.PairedUnlimitedHand;
                    } else {
                        ret = ConnectResult.ErrNotPairedUnlimitedHand;
                        continueConnection = false;
                    }
                    break;
                }
                case PairedUnlimitedHand:
                    if (mBTAccessHelper.connect(mUnlimitedHand, BluetoothAccessHelper.BT_SERIAL_PORT)) {
                        mAccessStatus = AccessStatus.ConnectedUnlimitedHand;
                        ret = ConnectResult.Connected;
                    } else {
                        ret = ConnectResult.PairedWithoutConnection;
                    }
                    continueConnection = false;
                    break;
                case ConnectedUnlimitedHand:
                    if (mBTAccessHelper.isConnected(mUnlimitedHand, BluetoothAccessHelper.BT_SERIAL_PORT)) {
                        ret = ConnectResult.Connected;
                        continueConnection = false;
                    } else {
                        // connection has already disconnected, try again.
                        mAccessStatus = AccessStatus.PairedUnlimitedHand;
                    }
                    break;
                case NoSupportBT:
                    ret = ConnectResult.ErrNoSupportBT;
                    continueConnection = false;
                    break;
                default:
                    continueConnection = false;
                    break;
            }
        }

        return ret;
    }

    private BluetoothAccessHelper.OnBluetoothStatusListener mBTStatusListener = new BluetoothAccessHelper.OnBluetoothStatusListener() {
        @Override
        public void onStatusChange(int status, int scanMode) {
            switch (status) {
                case BluetoothAccessHelper.StatusNoSupportBluetooth:
                    mAccessStatus = AccessStatus.NoSupportBT;
                    mConnectSemaphore.stop();
                    break;
                case BluetoothAccessHelper.StatusStartBluetooth:
                    mAccessStatus = AccessStatus.Init.LaunchBTAccessHelper;
                    mConnectSemaphore.stop();
                    break;
                case BluetoothAccessHelper.StatusInit:
                case BluetoothAccessHelper.StatusProgress:
                default:
                    // 処理なし
                    break;
            }
        }
    };

    private BluetoothAccessHelper.OnNotifyResultListener mBTNotifyResultListener = new BluetoothAccessHelper.OnNotifyResultListener() {
        @Override
        public void onSendDataResult(int result, BluetoothDevice device, byte[] data, int offset, int length) {
            mSendSemaphore.stop();
        }
    };

    private Runnable mPollingSensorRunnable = new Runnable() {
        @Override
        public void run() {
            OnSensorPollingListener[] toArrayTypeListener = new OnSensorPollingListener[0];
            AbstractSensorData[] toArrayTypeData = new AbstractSensorData[0];
            PhotoReflectorData photoReflectorData = new PhotoReflectorData();
            AngleData angleData = new AngleData();
            TemperatureData temperatureData = new TemperatureData();
            AccelerationData accelerationData = new AccelerationData();
            GyroData gyroData = new GyroData();
            QuaternionData quaternionData = new QuaternionData();
            ArrayList<AbstractSensorData> retList = new ArrayList<>();

            if (isEnableDebug()) {
                LogUtil.d(TAG, "start sensor polling");
            }

            while (mSensorPollingThread != null) {
                long startTimeMS = System.currentTimeMillis();
                long intervalMS = 1000 / mPollingRatePerSecond;
                int pollingTargetFlag = mPollingTargetFlag;
                byte uhCommandBitFlag = 0;
                int multiCmdRet = 0;

                retList.clear();

                if ((pollingTargetFlag & POLLING_TEMPERATURE) == POLLING_TEMPERATURE) {
                    if (readTemperature(temperatureData)) {
                        retList.add(temperatureData);
                    }
                }
                if ((pollingTargetFlag & POLLING_PHOTO_REFLECTOR) == POLLING_PHOTO_REFLECTOR) {
                    uhCommandBitFlag |= SendCommand.PhotoSensor.getMultiCommandbit();
                }
                if ((pollingTargetFlag & POLLING_ANGLE) == POLLING_ANGLE) {
                    uhCommandBitFlag |= SendCommand.Angle.getMultiCommandbit();
                }
                if ((pollingTargetFlag & POLLING_ACCELERATION) == POLLING_ACCELERATION) {
                    uhCommandBitFlag |= SendCommand.Acceleration.getMultiCommandbit();
                }
                if ((pollingTargetFlag & POLLING_GYRO) == POLLING_GYRO) {
                    uhCommandBitFlag |= SendCommand.Gyro.getMultiCommandbit();
                }
                if ((pollingTargetFlag & POLLING_QUATERNION) == POLLING_QUATERNION) {
                    uhCommandBitFlag |= SendCommand.Quaternion.getMultiCommandbit();
                }

                if (uhCommandBitFlag > 0) {
                    multiCmdRet = readAnySensor(uhCommandBitFlag, photoReflectorData, angleData, accelerationData, gyroData, quaternionData);
                    if ((multiCmdRet & SendCommand.PhotoSensor.getMultiCommandbit()) > 0) {
                        retList.add(photoReflectorData);
                    }
                    if ((multiCmdRet & SendCommand.Angle.getMultiCommandbit()) > 0) {
                        retList.add(angleData);
                    }
                    if ((multiCmdRet & SendCommand.Acceleration.getMultiCommandbit()) > 0) {
                        retList.add(accelerationData);
                    }
                    if ((multiCmdRet & SendCommand.Gyro.getMultiCommandbit()) > 0) {
                        retList.add(gyroData);
                    }
                    if ((multiCmdRet & SendCommand.Quaternion.getMultiCommandbit()) > 0) {
                        retList.add(quaternionData);
                    }
                }

                LogUtil.d(TAG, "command bit flag=" + uhCommandBitFlag + ", data size=" + retList.size());
                if (retList.size() > 0) {
                    OnSensorPollingListener[] listenerArray = mPollingListenerMap.keySet().toArray(toArrayTypeListener);
                    AbstractSensorData[] dataArray = retList.toArray(toArrayTypeData);
                    for (OnSensorPollingListener listener : listenerArray) {
                        listener.onPollSensor(dataArray);
                    }
                }

                long sleepTimeMS = intervalMS - (System.currentTimeMillis() - startTimeMS);
                if (sleepTimeMS > 0) {
                    try {
                        Thread.sleep(sleepTimeMS);
                    } catch (InterruptedException e) {
                    }
                }
            }

            if (isEnableDebug()) {
                LogUtil.d(TAG, "end sensor polling");
            }
        }
    };
}
