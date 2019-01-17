package chen.testchat.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amap.api.services.weather.LocalDayWeatherForecast;
import com.amap.api.services.weather.LocalWeatherForecast;
import com.amap.api.services.weather.LocalWeatherForecastResult;
import com.amap.api.services.weather.LocalWeatherLive;
import com.amap.api.services.weather.LocalWeatherLiveResult;
import com.amap.api.services.weather.WeatherSearch;
import com.amap.api.services.weather.WeatherSearchQuery;

import org.pointstone.cugappplat.base.cusotomview.ToolBarOption;

import java.util.List;

import chen.testchat.R;
import chen.testchat.bean.WeatherInfoBean;
import chen.testchat.manager.LocationManager;
import chen.testchat.manager.UserManager;
import chen.testchat.util.CommonUtils;
import chen.testchat.util.LogUtil;

/**
 * 项目名称:    TestChat
 * 创建人:        陈锦军
 * 创建时间:    2017/1/10      23:15
 * QQ:             1981367757
 */

public class WeatherInfoActivity extends SlideBaseActivity implements WeatherSearch.OnWeatherSearchListener {
        private TextView city;
        private TextView realTime;
        private TextView forecastTime;
        private TextView forecastInfo;
        private TextView wind;
        private TextView humidity;
        private TextView temperature;
        private TextView weatherStatus;
        private TextView emptyView;
        private LinearLayout container;
        private WeatherInfoBean mWeatherInfoBean;
        private List<LocalDayWeatherForecast> forecastInfoList;



        @Override
        protected boolean isNeedHeadLayout() {
                return true;
        }

        @Override
        protected boolean isNeedEmptyLayout() {
                return false;
        }

        @Override
        protected int getContentLayout() {
                return R.layout.activity_weather_info;
        }


        @Override
        public void initView() {
                city = (TextView) findViewById(R.id.tv_weather_city);
                realTime = (TextView) findViewById(R.id.tv_weather_real_time);
                weatherStatus = (TextView) findViewById(R.id.tv_weather_weather_info);
                forecastInfo = (TextView) findViewById(R.id.tv_weather_forecast_info);
                wind = (TextView) findViewById(R.id.tv_weather_wind);
                humidity = (TextView) findViewById(R.id.tv_weather_humidity);
                temperature = (TextView) findViewById(R.id.tv_weather_temperature);
                forecastTime = (TextView) findViewById(R.id.tv_weather_forecast_time);
                container = (LinearLayout) findViewById(R.id.ll_weather_container);
                emptyView = (TextView) findViewById(R.id.tv_weather_empty);
        }




        @Override
        public void initData() {
                WeatherInfoBean weatherInfoBean = (WeatherInfoBean) getIntent().getSerializableExtra("WeatherInfo");
                if (weatherInfoBean != null) {
                        mWeatherInfoBean = weatherInfoBean;
                        city.setText(mWeatherInfoBean.getCity());
                        realTime.setText(mWeatherInfoBean.getRealTime());
                        weatherStatus.setText(mWeatherInfoBean.getWeatherStatus());
                        temperature.setText(mWeatherInfoBean.getTemperature());
                        wind.setText(mWeatherInfoBean.getWind());
                        humidity.setText(mWeatherInfoBean.getHumidity());
                        if (weatherInfoBean.getForecastTime() == null) {
                                LogUtil.e("没有预测值");
                                startSearchForecastWeather();
                        } else {
                                city.setText(weatherInfoBean.getCity());
                                realTime.setText(weatherInfoBean.getRealTime());
                                weatherStatus.setText(weatherInfoBean.getWeatherStatus());
                                temperature.setText(weatherInfoBean.getTemperature());
                                wind.setText(weatherInfoBean.getWind());
                                humidity.setText(weatherInfoBean.getHumidity());
                                forecastTime.setText(weatherInfoBean.getForecastTime());
                                forecastInfo.setText(weatherInfoBean.getForecastInfo());
                        }
                } else {
                        if (CommonUtils.isNetWorkAvailable()) {
                                showLoadDialog("正在加载天气数据........请稍候......");
                                emptyView.setVisibility(View.VISIBLE);
                                container.setVisibility(View.GONE);
                                LogUtil.e("网络连接失败，请重新检查网络配置");
                                getWeatherInfo();
                        } else {
                                LogUtil.e("网络连接失败，请重新检查网络配置");
                                emptyView.setVisibility(View.VISIBLE);
                                container.setVisibility(View.GONE);
                        }
                }
                initActionBar();
        }

        private void initActionBar() {
                ToolBarOption toolBarOption = new ToolBarOption();
                toolBarOption.setAvatar(UserManager.getInstance().getCurrentUser().getAvatar());
                toolBarOption.setTitle("天气情况");
                toolBarOption.setNeedNavigation(true);
                setToolBar(toolBarOption);
        }

        private void getWeatherInfo() {
                mWeatherInfoBean = new WeatherInfoBean();
                List<String> addressList = LocationManager.getInstance().getLocationList();
                String cityName = addressList.get(4).substring(addressList.get(4).indexOf("省") + 1);
                mWeatherInfoBean.setCity(cityName);
                startSearchLiveWeather();
                startSearchForecastWeather();
        }


        private void startSearchForecastWeather() {
                WeatherSearchQuery query = new WeatherSearchQuery(mWeatherInfoBean.getCity(), WeatherSearchQuery.WEATHER_TYPE_FORECAST);
                WeatherSearch weatherSearch = new WeatherSearch(this);
                weatherSearch.setQuery(query);
                weatherSearch.setOnWeatherSearchListener(this);
                weatherSearch.searchWeatherAsyn();
        }

        private void startSearchLiveWeather() {
                WeatherSearchQuery query = new WeatherSearchQuery(mWeatherInfoBean.getCity(), WeatherSearchQuery.WEATHER_TYPE_LIVE);
                WeatherSearch weatherSearch = new WeatherSearch(this);
                weatherSearch.setQuery(query);
                weatherSearch.setOnWeatherSearchListener(this);
                weatherSearch.searchWeatherAsyn();
        }

        @Override
        public void onWeatherLiveSearched(LocalWeatherLiveResult localWeatherLiveResult, int i) {
                dismissBaseDialog();
                if (i == 1000) {
                        if (localWeatherLiveResult != null && localWeatherLiveResult.getLiveResult() != null) {
                                LocalWeatherLive localWeatherLive = localWeatherLiveResult.getLiveResult();
                                mWeatherInfoBean.setRealTime(localWeatherLive.getReportTime());
                                realTime.setText(mWeatherInfoBean.getRealTime() + "发布");
                                mWeatherInfoBean.setWeatherStatus(localWeatherLive.getWeather());
                                weatherStatus.setText(mWeatherInfoBean.getWeatherStatus());
                                mWeatherInfoBean.setTemperature(localWeatherLive.getTemperature() + "°");
                                temperature.setText(mWeatherInfoBean.getTemperature());
                                mWeatherInfoBean.setWind(localWeatherLive.getWindDirection() + "风       " + localWeatherLive
                                        .getWindPower() + "级");
                                wind.setText(mWeatherInfoBean.getWind());
                                mWeatherInfoBean.setHumidity("湿度       " + localWeatherLive.getHumidity() + "%");
                                humidity.setText(mWeatherInfoBean.getHumidity());
                        } else {
                                LogUtil.e("获取到的天气实时信息为空");
                        }
                } else {
                        LogUtil.e("获取到的天气实时信息失败" + i);
                }
        }

        @Override
        public void onWeatherForecastSearched(LocalWeatherForecastResult localWeatherForecastResult, int i) {
                dismissLoadDialog();
                if (i == 1000) {
                        if (localWeatherForecastResult != null && localWeatherForecastResult.getForecastResult() != null
                                && localWeatherForecastResult.getForecastResult().getWeatherForecast() != null
                                && localWeatherForecastResult.getForecastResult().getWeatherForecast().size() > 0) {
                                LocalWeatherForecast localWeatherForecast = localWeatherForecastResult.getForecastResult();
                                mWeatherInfoBean.setForecastTime(localWeatherForecast.getReportTime());
                                forecastTime.setText(mWeatherInfoBean.getForecastTime() + "发布");
                                forecastInfoList = localWeatherForecast.getWeatherForecast();
                                fillForeCast();
                        } else {
                                LogUtil.e("查询不到天气预报的结果");
                        }
                } else {
                        LogUtil.e("查询天气预报的结果失败" + i);
                }
        }

        private void fillForeCast() {
                String forecast = "";
                for (int i = 0; i < forecastInfoList.size(); i++) {
                        LocalDayWeatherForecast localdayweatherforecast = forecastInfoList.get(i);
                        String week = null;
                        switch (Integer.valueOf(localdayweatherforecast.getWeek())) {
                                case 1:
                                        week = "周一";
                                        break;
                                case 2:
                                        week = "周二";
                                        break;
                                case 3:
                                        week = "周三";
                                        break;
                                case 4:
                                        week = "周四";
                                        break;
                                case 5:
                                        week = "周五";
                                        break;
                                case 6:
                                        week = "周六";
                                        break;
                                case 7:
                                        week = "周日";
                                        break;
                                default:
                                        break;
                        }
                        String temp = String.format("%-3s/%3s",
                                localdayweatherforecast.getDayTemp() + "°",
                                localdayweatherforecast.getNightTemp() + "°");
                        String date = localdayweatherforecast.getDate();
                        forecast += date + "  " + week + "                       " + temp + "\n\n";
                }
                mWeatherInfoBean.setForecastInfo(forecast);
                forecastInfo.setText(mWeatherInfoBean.getForecastInfo());
        }

        @Override
        public void finish() {
                super.finish();
                if (mWeatherInfoBean != null) {
                        Intent intent = new Intent();
                        intent.putExtra("WeatherInfo", mWeatherInfoBean);
                        setResult(Activity.RESULT_OK, intent);
                } else {
                        setResult(Activity.RESULT_CANCELED);
                }
        }
}
