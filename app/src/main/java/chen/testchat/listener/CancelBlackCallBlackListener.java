package chen.testchat.listener;

import chen.testchat.bean.User;
import cn.bmob.v3.exception.BmobException;

/**
 * 项目名称:    TestChat
 * 创建人:        陈锦军
 * 创建时间:    2016/11/1      16:34
 * QQ:             1981367757
 */

public interface CancelBlackCallBlackListener {

        void onSuccess(User user);

        void onFailed(BmobException e);
}
