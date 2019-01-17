package chen.testchat.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import chen.testchat.CustomApplication;
import cn.bmob.v3.Bmob;
import cn.bmob.v3.listener.GetServerTimeListener;

/**
 * 项目名称:    HappyChat
 * 创建人:        陈锦军
 * 创建时间:    2016/9/13      18:57
 * QQ:             1981367757
 */
public class TimeUtil {
        public static String getTime(long time) {
                SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd HH:mm");
                return format.format(new Date(time));
        }


        public static Date getDateFormalFromString(String message) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = null;
                try {
                        date = simpleDateFormat.parse(message);
                } catch (ParseException e) {
                        e.printStackTrace();
                }
                return date;
        }


        public static String getServerFormatTime() {
                long deltaTime = CustomApplication.getInstance().getSharedPreferencesUtil().getDeltaTime();
                LogUtil.e("这里通过缓存的时间差值来计算出服务器上的时间");
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                long realServerTime = System.currentTimeMillis() - deltaTime;
                return simpleDateFormat.format(new Date(realServerTime));
        }

        public static String getRealTime(String time) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                        long serverTime = simpleDateFormat.parse(time).getTime();
                        LogUtil.e("服务器上的时间:" + serverTime);
                        long deltaTime = CustomApplication.getInstance().getSharedPreferencesUtil().getDeltaTime();
                        long realTime = serverTime + deltaTime;
                        LogUtil.e("客户端的时间:" + realTime);
                        LogUtil.e("现在的客户端的时间:" + System.currentTimeMillis());
                        long currentDletaTime = System.currentTimeMillis() - realTime;
                        LogUtil.e("差值:" + currentDletaTime);
                        return getShareTime(currentDletaTime);
                } catch (ParseException e) {
                        e.printStackTrace();
                        return null;
                }
        }

        private static String getShareTime(long currentDletaTime) {
                String result;
                int time = (int) (currentDletaTime / (1000 * 60));
                LogUtil.e("差值分钟:" + time);
                if (time == 0) {
                        result = "刚刚发表";
                        return result;
                }
                if (time > 0 && time < 60) {
                        result = time + "分钟前";
                } else {
                        time = (int) (currentDletaTime / (1000 * 60 * 60));
                        if (time > 0 && time < 24) {
                                result = time + "小时前";
                        } else {
                                time = (int) (currentDletaTime / (1000 * 60 * 60 * 24));
                                if (time == 1) {
                                        result = "昨天";
                                } else if (time == 2) {
                                        result = "前天";
                                } else {
                                        result = time + "天前";
                                }
                        }
                }
                LogUtil.e("时间拉拉：" + result);
                return result;
        }


        public static String getDateFormalFromString(int currentYear, int currentMonth, int currentDay) {
                GregorianCalendar gregorianCalendar = new GregorianCalendar(currentYear, currentMonth, currentDay);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                return simpleDateFormat.format(gregorianCalendar.getTime());
        }

        public static void getServerTime() {
                Bmob.getServerTime(CustomApplication.getInstance(), new GetServerTimeListener() {
                        @Override
                        public void onSuccess(long l) {
                                long deltaTime = System.currentTimeMillis() - l * 1000L;
                                LogUtil.e("客户端与服务器端的时间差值 :" + deltaTime);
                                CustomApplication.getInstance().getSharedPreferencesUtil().setDeltaTime(deltaTime);
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                LogUtil.e("获取服务器上的时间失败" + s + i);
                        }
                });
        }
}
