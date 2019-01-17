package chen.testchat.ui;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

import chen.testchat.CustomApplication;
import chen.testchat.R;
import chen.testchat.bean.GroupTableMessage;
import chen.testchat.bean.User;
import chen.testchat.db.ChatDB;
import chen.testchat.manager.MessageCacheManager;
import chen.testchat.manager.MsgManager;
import chen.testchat.manager.UserCacheManager;
import chen.testchat.manager.UserManager;
import chen.testchat.util.BmobUtils;
import chen.testchat.util.LogUtil;
import cn.bmob.v3.listener.FindListener;

import static cn.bmob.v3.c.darkness.log;

/**
 * 项目名称:    HappyChat
 * 创建人:        陈锦军
 * 创建时间:    2016/9/11      16:52
 * QQ:             1981367757
 */
public class SplashActivity extends org.pointstone.cugappplat.base.basemvp.BaseActivity {
        private Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        switch (msg.what) {
                                case 0:
                                        LogUtil.e("该用户有缓存数据，直接跳转到主界面");
                                        updateUserInfo();
                                        break;
                                case 1:
                                        LogUtil.e("该用户无缓存数据，直接跳转到登录界面");
                                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                        break;
                                default:
                                        break;
                        }
                }
        };
        private List<GroupTableMessage> newData;

        private void updateUserInfo() {
                if (newData != null) {
                        newData.clear();
                }
                UserManager.getInstance().queryAndSaveCurrentContactsList(new FindListener<User>() {
                                                                                  @Override
                                                                                  public void onSuccess(final List<User> contacts) {
                                                                                          // 返回的是去除了黑名单的好友列表
//                                       否则保存到内存中，方便取用
                                                                                          LogUtil.e("查询好友成功1");
                                                                                          LogUtil.e("把服务器上查询得到的好友消息存到内存中");
                                                                                          LogUtil.e("用户USER");
//                                                                                          User user = UserManager.getInstance().getCurrentUser();
//                                                                                          if (user != null) {
//                                                                                                  CustomApplication.getInstance().setUser(user);
//                                                                                          } else {
//                                                                                                  LogUtil.e("用户USER为空");
//                                                                                          }
                                                                                          String uid = UserManager.getInstance().getCurrentUserObjectId();
                                                                                          MsgManager.getInstance().queryGroupTableMessage(uid, new FindListener<GroupTableMessage>() {
                                                                                                          @Override
                                                                                                          public void onSuccess(final List<GroupTableMessage> list) {
                                                                                                                  if (list != null && list.size() > 0) {
                                                                                                                          LogUtil.e("在服务器上查询到该用户所有的群,数目:" + list.size());
                                                                                                                          newData=new ArrayList<>();
                                                                                                                          for (GroupTableMessage message :
                                                                                                                                  list) {
//                                                                                                                                  这里进行判断出现是因为有可能建群失败的时候，未能把groupId上传上去
                                                                                                                                  if (message.getGroupId()!=null) {
                                                                                                                                          newData.add(message);
                                                                                                                                  }
                                                                                                                          }
                                                                                                                          if (ChatDB.create().saveGroupTableMessage(newData)) {
                                                                                                                                  LogUtil.e("保存用户所拥有的群结构消息到数据库中成功");
                                                                                                                          } else {
                                                                                                                                  LogUtil.e("保存用户所拥有的群结构消息到数据库中失败");
                                                                                                                          }
                                                                                                                  } else {
                                                                                                                          LogUtil.e("服务器上没有查到该用户所拥有的群");
                                                                                                                  }
                                                                                                                  runOnUiThread(new Runnable() {
                                                                                                                          @Override
                                                                                                                          public void run() {
                                                                                                                                  LogUtil.e("1执行到这里开始跳转到MainActivity");
                                                                                                                                  UserCacheManager.getInstance().setLogin(true);
                                                                                                                                  MessageCacheManager.getInstance().setLogin(true);
                                                                                                                                  if (contacts != null && contacts.size() > 0) {
                                                                                                                                          UserCacheManager.getInstance().setContactsList(BmobUtils.list2map(contacts));
                                                                                                                                  }
                                                                                                                                  MessageCacheManager.getInstance().addGroupTableMessage(newData);
                                                                                                                                  Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                                                                                                                                  startActivity(intent);
                                                                                                                                  finish();
                                                                                                                          }
                                                                                                                  });
                                                                                                          }

                                                                                                          @Override
                                                                                                          public void onError(int i, String s) {
                                                                                                                  LogUtil.e("在服务器上查询所有群结构消息失败" + s + i);

                                                                                                                  if (i == 101) {
                                                                                                                          runOnUiThread(new Runnable() {
                                                                                                                                  @Override
                                                                                                                                  public void run() {
                                                                                                                                          LogUtil.e("1执行到这里开始跳转到MainActivity");
                                                                                                                                          UserCacheManager.getInstance().setLogin(true);
                                                                                                                                          MessageCacheManager.getInstance().setLogin(true);
                                                                                                                                          if (contacts != null && contacts.size() > 0) {
                                                                                                                                                  UserCacheManager.getInstance().setContactsList(BmobUtils.list2map(contacts));
                                                                                                                                          }
                                                                                                                                          Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                                                                                                                                          startActivity(intent);
                                                                                                                                          finish();
                                                                                                                                  }
                                                                                                                          });
                                                                                                                  }
                                                                                                                  Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                                                                                                                  startActivity(intent);
                                                                                                                  finish();

                                                                                                          }
                                                                                                  }

                                                                                          );
                                                                                  }

                                                                                  @Override
                                                                                  public void onError(int i, String s) {
                                                                                          UserCacheManager.getInstance().setLogin(true);
                                                                                          MessageCacheManager.getInstance().setLogin(true);
                                                                                          log("查询好友失败! " + s + i);
                                                                                          Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                                                                                          startActivity(intent);
                                                                                          finish();
                                                                                  }
                                                                          }

                );
        }

        @Override
        protected void onResume() {
                super.onResume();
                boolean isLogin = CustomApplication.getInstance().getSharedPreferencesUtil().isLogin();
                if (isLogin) {
                        handler.sendEmptyMessageDelayed(0, 1000);
                } else {
                        handler.sendEmptyMessageDelayed(1, 1000);
                }
        }

        @Override
        protected boolean isNeedHeadLayout() {
                return false;
        }

        @Override
        protected boolean isNeedEmptyLayout() {
                return false;
        }

        @Override
        protected int getContentLayout() {
                return R.layout.activity_splash;
        }

        @Override
        protected void initView() {

        }

        @Override
        protected void initData() {

        }


        @Override
        protected void onDestroy() {
                super.onDestroy();
                handler = null;
        }
}
