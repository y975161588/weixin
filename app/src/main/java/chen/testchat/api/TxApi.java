package chen.testchat.api;

import chen.testchat.base.Constant;
import chen.testchat.bean.TxResponse;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * 项目名称:    TestChat
 * 创建人:        陈锦军
 * 创建时间:    2017/1/6      21:17
 * QQ:             1981367757
 */

public interface TxApi {

        @GET("/wxnew/?key=" + Constant.TIAN_XING_KEY + "&num=20")
        Observable<TxResponse> getWinXinInfo(@Query("page") int page);
}
