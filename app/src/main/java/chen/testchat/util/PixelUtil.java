package chen.testchat.util;

import android.util.TypedValue;

import chen.testchat.CustomApplication;


/**
 * 项目名称:    HappyChat
 * 创建人:        陈锦军
 * 创建时间:    2016/9/13      9:26
 * QQ:             1981367757
 */
public class PixelUtil {

        /**
         * dp转化为px的工具
         * @param i
         * @return
         */
        public static int dp2px(int i) {
                return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX,i,CustomApplication.getInstance().getResources().getDisplayMetrics());
        }

        public static int todp(int i) {
                return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,i,CustomApplication.getInstance().getResources().getDisplayMetrics());
        }
}
