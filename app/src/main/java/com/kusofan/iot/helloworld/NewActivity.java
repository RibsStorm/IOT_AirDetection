package com.kusofan.iot.helloworld;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Date;

/**
 * 创建人 : heming9174
 * 时间 : 2020/12/25 19:53
 */
public class NewActivity extends Activity {

    private UartDeivceUtils mUartDeivce;

    private TextView mTvTemperature;
    private TextView mTvHumidity;
    private TextView mTvCO2;
    private TextView mTvHCHO;
    private TextView mTvTVOC;
    private TextView mTvPM2_5;
    private TextView mTvPM10;
    private TextView mTvLastTime;
    private FrameLayout mFlLoading;
    //缓存一份数据,当传感器返回数据差异不大时,不更新,节省开销.
    private float mLastTemperature;
    private float mLastHumidity;
    private float mLastCO2;
    private float mLastHCHO;
    private float mLastTVOC;
    private float mLastPM2_5;
    private float mLastPM10;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new);
        initView();

        mUartDeivce = UartDeivceUtils.getInstance();
        mUartDeivce.setCallBack(mUartCallback);
        mUartDeivce.serialInfo();
    }

    private UartDeivceUtils.UpsCallBack mUartCallback = new UartDeivceUtils.UpsCallBack() {
        @Override
        public void callBack(final float[] data) {
            try {
                NewActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mFlLoading.getVisibility() == View.VISIBLE) {
                            mFlLoading.setVisibility(View.GONE);
                        }

                        if (data[0] != 0 && Math.abs(mLastTemperature - data[0]) > 0.1) {
                            mLastTemperature = data[0];
                            mTvTemperature.setText(getString(R.string.now_temperature, dataFormat(data[0])));
                        }
                        if (data[1] != 0  && Math.abs(mLastHumidity - data[1]) > 0.1) {
                            mLastHumidity = data[1];
                            mTvHumidity.setText(getString(R.string.now_humidity, dataFormat(data[1])));
                        }
                        if (data[2] != 0  && Math.abs(mLastCO2 - data[2]) > 1) {
                            mLastCO2 = data[2];
                            mTvCO2.setText(getString(R.string.now_co2, ((int) data[2]) + ""));
                        }
                        if (data[3] != 0  && Math.abs(mLastHCHO - data[3]) > 1) {
                            mLastHCHO = data[3];
                            mTvHCHO.setText(getString(R.string.now_ch20, ((int) data[3]) + ""));
                        }
                        if (data[4] != 0  && Math.abs(mLastTVOC - data[4]) > 1) {
                            mLastTVOC = data[4];
                            mTvTVOC.setText(getString(R.string.now_tvoc, ((int) data[4]) + ""));
                        }
                        if (data[5] != 0  && Math.abs(mLastPM2_5 - data[5]) > 1) {
                            mLastPM2_5 = data[5];
                            mTvPM2_5.setText(getString(R.string.now_pm2_5, ((int) data[5]) + ""));
                        }
                        if (data[6] != 0  && Math.abs(mLastPM10 - data[6]) > 1) {
                            mLastPM10 = data[6];
                            mTvPM10.setText(getString(R.string.now_pm10, ((int) data[6]) + ""));
                        }
                        mTvLastTime.setText(getString(R.string.last_time, DateUtil.getDateTime(new Date())));
                    }
                });
                Log.d("AAA", "传感器返回的数据是:" + Arrays.toString(data));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUartDeivce.close();
    }

    private void initView() {
        mTvTemperature = findViewById(R.id.tv_temperature);
        mTvHumidity = findViewById(R.id.tv_humidity);
        mTvCO2 = findViewById(R.id.tv_co2);
        mTvHCHO = findViewById(R.id.tv_ch2o);
        mTvTVOC = findViewById(R.id.tv_tvoc);
        mTvPM2_5 = findViewById(R.id.tv_pm2_5);
        mTvPM10 = findViewById(R.id.tv_pm10);
        mTvLastTime = findViewById(R.id.tv_last_time);
        mFlLoading = findViewById(R.id.fl_load);
    }

    private String dataFormat(float data) {
        return String.format("%.2f", data);
    }

}
