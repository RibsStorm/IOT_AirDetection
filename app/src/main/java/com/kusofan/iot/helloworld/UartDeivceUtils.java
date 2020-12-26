package com.kusofan.iot.helloworld;

import android.util.Log;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;


public class UartDeivceUtils {

    private UpsCallBack callBack;
    PeripheralManager manager;
    private UartDevice mDevice;
    //"USB1-1.2:1.0";//"UART0";//"MINIUART";
    public static String UART_DEVICE_NAME = "MINIUART";
    public static UartDeivceUtils uartDeivceUtils = new UartDeivceUtils();
    private long mLastTime;

    private UartDeivceUtils() {
    }

    public synchronized static UartDeivceUtils getInstance() {
        return uartDeivceUtils;
    }

    public interface UpsCallBack {
        void callBack(float[] data);
    }

    public void setCallBack(UpsCallBack callBack) {
        this.callBack = callBack;
    }

    public void serialInfo() {
        if (callBack == null) {
            return;
        }

        manager = PeripheralManager.getInstance();
        List<String> deviceList = manager.getUartDeviceList();
        if (deviceList.isEmpty()) {
            Log.i(TAG, "No UART port available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + deviceList);
        }

        try {
            // 尝试访问UART设备
            mDevice = manager.openUartDevice(UART_DEVICE_NAME);
            mDevice.registerUartDeviceCallback(mUartCallback);

            configureUartFrame(mDevice);
            setFlowControlEnabled(mDevice, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 配置UART端口
     */
    public void configureUartFrame(UartDevice uart) throws IOException {
        if (uart != null) {
            uart.setBaudrate(9600);
            uart.setDataSize(8);
            uart.setParity(UartDevice.PARITY_NONE);
            uart.setStopBits(1);
        }
    }

    public void setFlowControlEnabled(UartDevice uart, boolean enable) throws IOException {
        if (uart != null) {
            if (enable) {
                // 启用硬件流控制
                uart.setHardwareFlowControl(UartDevice.HW_FLOW_CONTROL_AUTO_RTSCTS);
            } else {
                // 禁用流量控制
                uart.setHardwareFlowControl(UartDevice.HW_FLOW_CONTROL_NONE);
            }
        }
    }

    /**
     * 发送指令返回内容
     */
    private UartDeviceCallback mUartCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(final UartDevice uart) {
            // 从UART设备读取可用数据
            if (System.currentTimeMillis() - mLastTime > 4000) {
                mLastTime = System.currentTimeMillis();
                try {
                    readUartBuffer(uart);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            Log.w(TAG, uart + ": Error event " + error);
        }
    };

    /**
     * 通过UART获取传输过来的数据
     */
    public void readUartBuffer(UartDevice uart){
        byte[] buffer = new byte[33];
        int index = -1;
        try {
            uart.read(buffer, buffer.length);
            //遍历一下数据,找一下 帧头.
            for (int i = 0; i < buffer.length; i++) {
                if (buffer[i] == 60 && i + 1 < 33 && buffer[i + 1] == 2) {
                    //数据不对,越界了,丢弃~
                    if (i + 16 > 33) {
                        return;
                    }
                    index = i;
                }
            }
            if (index == -1) {
                return;
            }
            //sleep一下,因为从uart读取数据到byte[]里面需要时间,避免太快导致 拿到的都是0000000
            Thread.sleep(400);
            float[] compress = compress(buffer, index);
            if (checkData(compress)) {
                callBack.callBack(compress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 对应数据的起点 index
     */
    private float[] compress(byte[] data, int index) {
        float[] result = new float[7];
        try {
            //温度
            result[0] = data[index + 12] + data[index + 13] / 100f;
            //湿度
            result[1] = data[index + 14] + data[index + 15] / 100f;
            //二氧化碳
            result[2] = (0xFF & data[index + 2]) * 256 + (0xFF & data[index + 3]);
            //甲醛
            result[3] = (0xFF & data[index + 4]) * 256 + (0xFF & data[index + 5]);
            //TVOC
            result[4] = (0xFF & data[index + 6]) * 256 + (0xFF & data[index + 7]);
            //PM2.5
            result[5] = (0xFF & data[index + 8]) * 256 + (0xFF & data[index + 9]);
            //PM10
            result[6] = (0xFF & data[index + 10]) * 256 + (0xFF & data[index + 11]);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return result;
        }
    }

    /**
     * 检查数据,当出现 数据都是0的时候,丢掉,不要.
     * TODO...校验位校验
     */
    private boolean checkData(float[] data) {
        byte exceptionCount = 0;
        for (float datum : data) {
            if (datum == 0) {
                exceptionCount++;
            }
            if (datum < 0) {
                Log.w(TAG, "数据检查失败,居然还有负数的.告辞~具体是数据是" + Arrays.toString(data));
                return false;
            }
            if (exceptionCount >= 3) {
                Log.w(TAG, "数据检查失败,超过3个参数为0,具体是数据是" + Arrays.toString(data));
                return false;
            }
        }
        Log.w(TAG, "数据校验成功");
        return true;
    }

    public void close() {
        if (mDevice != null) {
            try {
                mDevice.unregisterUartDeviceCallback(mUartCallback);
                mDevice.close();
                mDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close UART device", e);
            }
        }
    }
}
