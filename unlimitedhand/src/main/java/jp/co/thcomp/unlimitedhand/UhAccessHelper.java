package jp.co.thcomp.unlimitedhand;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import jp.co.thcomp.bluetoothhelper.BluetoothAccessHelper;
import jp.co.thcomp.util.LogUtil;
import jp.co.thcomp.util.ThreadUtil;

public class UhAccessHelper {
    private static final String TAG = UhAccessHelper.class.getSimpleName();
    private static final String DEFAULT_UH_NAME_PATTERN = "RNBT-[\\w]{4}";
    private static final int DEFAULT_EMS_VOLTAGE_LEVEL = 8;
    private static final int DEFAULT_EMS_SHARPNESS_LEVEL = 15;
    private static final int MAX_EMS_VOLTAGE_LEVEL = 12;
    private static final int MIN_EMS_VOLTAGE_LEVEL = 0;
    private static final int CHANGE_EMS_VOLTAGE_LEVEL = 1;
    private static final int MAX_EMS_SHARPNESS_LEVEL = 20;
    private static final int MIN_EMS_SHARPNESS_LEVEL = 0;
    private static final int CHANGE_EMS_SHARPNESS_LEVEL = 5;
    private static final String LINE_SEPARATOR = "\n";

    public enum ConnectResult {
        ErrNoSupportBT,
        ErrNotPairedUnlimitedHand,
        ErrUnknown,
        PairedWithoutConnection,
        Connected,
    }

    public enum SendCommand {
        Vibrate("b"),
        EMS_Pad0("0"),
        EMS_Pad1("1"),
        EMS_Pad2("2"),
        EMS_Pad3("3"),
        EMS_Pad4("4"),
        EMS_Pad5("5"),
        EMS_Pad6("6"),
        EMS_Pad7("7"),
        PhotoSensor("c"),
        Angle("A"),
        Temperature("A"),
        Acceleration("a"),
        Gyro("a"),
        Quaternion("q"),
        UpSharpnessLevel("t"),
        DownSharpnessLevel("u"),
        UpVoltageLevel("h"),
        DownVoltageLevel("l"),;

        private String mCode;

        SendCommand(String code) {
            mCode = code;
        }

        byte[] getLineCode() {
            return (mCode + LINE_SEPARATOR).getBytes();
        }

        byte[] getCode() {
            return mCode.getBytes();
        }
    }

    private enum AccessStatus {
        NoSupportBT,
        Init,
        LaunchBTAccessHelper,
        PairedUnlimitedHand,
        ConnectedUnlimitedHand,
    }

    private static UhAccessHelper sInstance;

    public synchronized static UhAccessHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new UhAccessHelper(context);
        }

        return sInstance;
    }

    private Context mContext;
    private BluetoothAccessHelper mBTAccessHelper;
    private AccessStatus mAccessStatus = AccessStatus.Init;
    private BluetoothDevice mUnlimitedHand = null;
    private final ThreadUtil.OnetimeSemaphore mConnectSemaphore = new ThreadUtil.OnetimeSemaphore();
    private final ThreadUtil.OnetimeSemaphore mSendSemaphore = new ThreadUtil.OnetimeSemaphore();
    private int mCurrentSharpnessLevel = DEFAULT_EMS_SHARPNESS_LEVEL;
    private int mCurrentVoltageLevel = DEFAULT_EMS_VOLTAGE_LEVEL;
    private HandlerThread mBtHelperNotifyThread;
    private Handler mBtHelperNotifyHandler;

    private UhAccessHelper(Context context) {
        if (context == null) {
            throw new NullPointerException("context == null");
        }

        mContext = context;
        mBTAccessHelper = new BluetoothAccessHelper(context, UhAccessHelper.class.getName());
        mBTAccessHelper.setOnBluetoothStatusListener(mBTStatusListener);
        mBTAccessHelper.setOnNotifyResultListener(mBTNotifyResultListener);
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

        if(mBtHelperNotifyThread != null) {
            mBtHelperNotifyThread.quit();
            mBtHelperNotifyThread = null;
        }
    }

    /**
     * 光学センサー読み取り
     *
     * @param data
     * @return
     */
    public boolean readPhotoSensor(PhotoSensorData data) {
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
                if (mCurrentSharpnessLevel < MAX_EMS_SHARPNESS_LEVEL) {
                    if ((ret = mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.UpSharpnessLevel.getLineCode()))) {
                        mCurrentSharpnessLevel += CHANGE_EMS_SHARPNESS_LEVEL;
                    }
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
                if (MIN_EMS_SHARPNESS_LEVEL < mCurrentSharpnessLevel) {
                    if ((ret = mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.DownSharpnessLevel.getLineCode()))) {
                        mCurrentSharpnessLevel -= CHANGE_EMS_SHARPNESS_LEVEL;
                    }
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
                if (mCurrentVoltageLevel < MAX_EMS_VOLTAGE_LEVEL) {
                    if ((ret = mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.UpVoltageLevel.getLineCode()))) {
                        mCurrentVoltageLevel += CHANGE_EMS_VOLTAGE_LEVEL;
                    }
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
                if (MIN_EMS_VOLTAGE_LEVEL < mCurrentVoltageLevel) {
                    synchronized (mSendSemaphore) {
                        mSendSemaphore.initialize();

                        if ((ret = mBTAccessHelper.sendData(BluetoothAccessHelper.BT_SERIAL_PORT, mUnlimitedHand, SendCommand.DownVoltageLevel.getLineCode()))) {
                            mSendSemaphore.start();
                            mCurrentVoltageLevel -= CHANGE_EMS_VOLTAGE_LEVEL;
                        }
                    }
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

    private byte[] readData() {
        byte[] readBuffer = new byte[1024];
        int readSize = 0;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        if ((readSize = mBTAccessHelper.readData(mUnlimitedHand, BluetoothAccessHelper.BT_SERIAL_PORT, readBuffer)) > 0) {
            outStream.write(readBuffer, 0, readSize);
        }

        return outStream.toByteArray();
    }

    private synchronized ConnectResult connect(String deviceName, boolean useRegExp) {
        ConnectResult ret = ConnectResult.ErrUnknown;
        boolean continueConnection = true;

        while (continueConnection) {
            switch (mAccessStatus) {
                case Init:
                    if(mBtHelperNotifyThread != null){
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
}
