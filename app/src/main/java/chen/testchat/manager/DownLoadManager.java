package chen.testchat.manager;

import android.os.Environment;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;

import java.io.File;

import chen.testchat.bean.BaseMessage;
import chen.testchat.bean.ChatMessage;
import chen.testchat.bean.GroupChatMessage;
import chen.testchat.listener.OnDownLoadFileListener;
import chen.testchat.util.LogUtil;
import cn.bmob.v3.exception.BmobException;
import okhttp3.Call;
import okhttp3.Request;

/**
 * 项目名称:    TestChat
 * 创建人:        陈锦军
 * 创建时间:    2016/10/29      19:12
 * QQ:             1981367757
 */

/**
 * 下载管理器
 * 内部采用异步线程AsyncTask下载
 */

public class DownLoadManager {
        private static final Object LOCK = new Object();
        private static DownLoadManager instance = null;

        public static DownLoadManager getInstance() {
                if (instance == null) {
                        synchronized (LOCK) {
                                if (instance == null) {
                                        instance = new DownLoadManager();
                                }
                        }
                }
                return instance;
        }


        /**
         * 下载文件
         *
         * @param message                消息实体
         * @param onDownLoadFileListener 下载回调
         */
        public void downFile(final BaseMessage message, final OnDownLoadFileListener onDownLoadFileListener) {
                LogUtil.e("语音消息的数据格式:");
                if (message instanceof ChatMessage) {
                        LogUtil.e(((ChatMessage) message));
                } else {
                        LogUtil.e(((GroupChatMessage) message));
                }
                LogUtil.e("语音地址11：" + message.getContent().split("&")[0]);
                OkHttpUtils.get().url(message.getContent().split("&")[0]).build().execute(new FileCallBack(Environment.getExternalStorageDirectory().getAbsolutePath(), message.getCreateTime() + message.getBelongId() + ".amr") {
                        @Override
                        public void onBefore(Request request, int id) {
                                onDownLoadFileListener.onStart();
                        }

                        @Override
                        public void inProgress(float progress, long total, int id) {
                                onDownLoadFileListener.onProgress((int) progress);
                        }

                        @Override
                        public void onError(Call call, Exception e, int id) {
                                LogUtil.e("接受语音消息失败");
                                onDownLoadFileListener.onFailed(new BmobException(e));
                        }

                        @Override
                        public void onResponse(File response, int id) {
                                onDownLoadFileListener.onSuccess(response.getAbsolutePath());
                                LogUtil.e("接受到文件啦啦啦，文件路径为：" + response.getAbsolutePath());
                        }
                });
        }

        public void download(String videoUrl, String id, final OnDownLoadFileListener listener) {
                OkHttpUtils.get().url(videoUrl).build().execute(new FileCallBack(Environment.getExternalStorageDirectory().getAbsolutePath(), id + System.currentTimeMillis() + ".mp4") {
                        @Override
                        public void onBefore(Request request, int id) {
                                listener.onStart();
                        }

                        @Override
                        public void inProgress(float progress, long total, int id) {
                                listener.onProgress((int) progress);
                        }

                        @Override
                        public void onError(Call call, Exception e, int id) {
                                LogUtil.e("接受视频消息失败");
                                listener.onFailed(new BmobException(e));
                        }

                        @Override
                        public void onResponse(File response, int id) {
                                listener.onSuccess(response.getAbsolutePath());
                                LogUtil.e("接受到文件啦啦啦，文件路径为：" + response.getAbsolutePath());
                        }
                });
        }
}
