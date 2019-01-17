package org.pointstone.cugappplat.net;

/**
 * 项目名称:    TestChat
 * 创建人:        陈锦军
 * 创建时间:    2016/10/29      19:54
 * QQ:             1981367757
 */

public interface OnDownLoadFileListener {

        void onStart();

        void onProgress(int value);

        void onSuccess(String localPath);

        void onFailed(Exception e);
}
