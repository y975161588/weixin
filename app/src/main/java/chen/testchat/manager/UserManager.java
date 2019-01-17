package chen.testchat.manager;

/**
 * 项目名称:    HappyChat
 * 创建人:        陈锦军
 * 创建时间:    2016/9/11      17:31
 * QQ:             1981367757
 */


import android.support.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import chen.testchat.CustomApplication;
import chen.testchat.base.Constant;
import chen.testchat.bean.CustomInstallation;
import chen.testchat.bean.User;
import chen.testchat.db.ChatDB;
import chen.testchat.listener.AddBlackCallBackListener;
import chen.testchat.listener.AddFriendCallBackListener;
import chen.testchat.listener.CancelBlackCallBlackListener;
import chen.testchat.util.CommonUtils;
import chen.testchat.util.LogUtil;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobDate;
import cn.bmob.v3.datatype.BmobGeoPoint;
import cn.bmob.v3.datatype.BmobPointer;
import cn.bmob.v3.datatype.BmobRelation;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.PushListener;
import cn.bmob.v3.listener.UpdateListener;

/**
 * 用户管理类
 * 与用户操作有关的操作类：登录、退出、获取好友列表、获取当前的登录用户,删除好友，添加好友等
 */
public class UserManager {
        private static final String COLUMN_NAME_CONTACTS = "contacts";
        private static final String COLUMN_NAME_BLACKLIST = "black";
        //        用于同步
        private static final Object INSTANCE_LOCK = new Object();
        private static UserManager INSTANCE;

        public static UserManager getInstance() {
                if (INSTANCE == null) {
                        synchronized (INSTANCE_LOCK) {
                                if (INSTANCE == null) {
                                        INSTANCE = new UserManager();
                                }
                        }
                }
                return INSTANCE;
        }


        /**
         * 如果没有自定义用户实体类extend BmobUser
         * 默认返回BmobUser类型的用户实体
         * 获取当前用户
         *
         * @return 当前用户
         */
        public User getCurrentUser() {
                return BmobUser.getCurrentUser(CustomApplication.getInstance(), User.class);
        }


        /**
         * 获取当前的好友列表并回调在callback
         *
         * @param callback 回调
         */
        public void queryAndSaveCurrentContactsList(final FindListener<User> callback) {
                BmobQuery<User> query = new BmobQuery<>();
                query.order("-updatedAt");
                query.setLimit(Constant.LIMIT_CONTACTS);
                query.addWhereRelatedTo(COLUMN_NAME_CONTACTS, new BmobPointer(getCurrentUser()));
                query.findObjects(CustomApplication.getInstance(), new FindListener<User>() {
                                @Override
                                public void onSuccess(final List<User> friend) {
                                        if (friend != null && friend.size() > 0) {
                                                if (!ChatDB.create().addOrUpdateContacts(friend)) {
                                                        LogUtil.e("在数据库中更新好友失败");
                                                        callback.onError(0, "在数据库中更新好友失败");
                                                        return;
                                                }
                                                queryBlackList(new FindListener<User>() {
                                                                       @Override
                                                                       public void onSuccess(List<User> list) {
                                                                               if (list != null && list.size() > 0) {
                                                                                       if (ChatDB.create().updateFriendsBlackStatus(list)) {
                                                                                               LogUtil.e("批量更新黑名单成功");
                                                                                               List<User> friends = ChatDB.create().getContactsWithoutBlack();
                                                                                               callback.onSuccess(friends);
                                                                                       } else {
                                                                                               LogUtil.e("批量更新黑名单失败");
                                                                                               callback.onError(0, "在数据库中批量更新黑名单失败");
                                                                                       }
                                                                               } else {
                                                                                       callback.onSuccess(friend);
                                                                               }
                                                                       }

                                                                       @Override
                                                                       public void onError(int i, String s) {
                                                                               LogUtil.e("在服务器上查询黑名单失败" + s + i);
                                                                               callback.onError(0, "在服务器上查询黑名单失败");

                                                                       }
                                                               }
                                                );
                                        } else {
                                                callback.onSuccess(null);
                                        }
                                }

                                @Override
                                public void onError(int i, String s) {
                                        LogUtil.e("在服务器上查询好友失败" + s + i);
                                        callback.onError(i, s);
                                }
                        }

                );
        }

        /**
         * 查询黑名单用户
         *
         * @param callback 回调
         */
        private void queryBlackList(final FindListener<User> callback) {
                BmobQuery<User> query = new BmobQuery<>();
                query.order("updateAt");
                query.addWhereRelatedTo(COLUMN_NAME_BLACKLIST, new BmobPointer(getCurrentUser()));
                query.findObjects(CustomApplication.getInstance(), new FindListener<User>() {
                                @Override
                                public void onSuccess(List<User> list) {
                                        callback.onSuccess(list);
                                }

                                @Override
                                public void onError(int i, String s) {
                                        callback.onError(i, s);
                                }
                        }
                );
        }

        /**
         * 获取本用户ID
         *
         * @return 用户ID
         */
        public String getCurrentUserObjectId() {
                return getCurrentUser().getObjectId();
        }


        /**
         * 根据用户名查询用户
         *
         * @param name     根据用户名在服务器上查询用户
         * @param listener 回调
         */
        public void queryUsers(String name, FindListener<User> listener) {
                BmobQuery<User> query = new BmobQuery<>();
                query.addWhereEqualTo("username", name);
                query.order("createdAt");
                query.findObjects(CustomApplication.getInstance(), listener);
        }


        /**
         * 下线操作
         */
        public void logout() {
                LogUtil.e("下线啦啦啦112233");
                UserCacheManager.getInstance().setLogin(false);
                MessageCacheManager.getInstance().setLogin(false);
                UserCacheManager.getInstance().clear();
                MessageCacheManager.getInstance().clear();
                User.logOut(CustomApplication.getInstance());
                CustomApplication.getInstance().getSharedPreferencesUtil().setLogin(false);
        }

        /**
         * 添加用户为好友(1、在服务器上添加关联，添加如数据库)
         *
         * @param targetId                  对方ID
         * @param currentId                 本用户ID
         * @param addFriendCallBackListener 回调
         */
        public void addNewFriend(final String targetId, final String currentId, @NonNull final AddFriendCallBackListener addFriendCallBackListener) {
                findUserById(targetId, new FindListener<User>() {
                                @Override
                                public void onSuccess(final List<User> list) {
                                        if (list != null && list.size() > 0) {
                                                LogUtil.e("在服务器上查询好友成功");
                                                saveNewFriendToServer(list.get(0), currentId, new UpdateListener() {
                                                                @Override
                                                                public void onSuccess() {
                                                                        LogUtil.e("在服务器上关联好友成功");
                                                                        if (ChatDB.create(currentId).addOrUpdateContacts(list.get(0)) > 0) {
//                                                                        保存好好友后发送同意类型的消息
                                                                                addFriendCallBackListener.onSuccess(list.get(0));
                                                                        } else {
                                                                                addFriendCallBackListener.onFailed(new BmobException("数据库保存好友失败"));
                                                                        }
                                                                }

                                                                @Override
                                                                public void onFailure(int i, String s) {
                                                                        LogUtil.e("在服务器上关联好友失败");
                                                                        addFriendCallBackListener.onFailed(new BmobException(s));

                                                                }
                                                        }
                                                );
                                        } else {
                                                addFriendCallBackListener.onFailed(new BmobException("在服务器上灭有查到该用户"));
                                        }
                                }

                                @Override
                                public void onError(int i, String s) {
                                        LogUtil.e("在服务器上查询用户失败" + s + i);
                                        addFriendCallBackListener.onFailed(new BmobException(s));

                                }
                        }
                );
        }

        /**
         * 在服务器上关联该好友
         *
         * @param user           用户实体
         * @param currentId      现在的登录用户ID
         * @param updateListener 跟新回调
         */
        private void saveNewFriendToServer(User user, String currentId, UpdateListener updateListener) {
                if (getCurrentUser() != null && currentId.equals(getCurrentUserObjectId())) {
                        User currentUser = new User();
                        currentUser.setObjectId(currentId);
                        BmobRelation relation = new BmobRelation();
                        relation.add(user);
                        currentUser.setContacts(relation);
                        currentUser.update(CustomApplication.getInstance(), updateListener
                        );
                } else {
                        LogUtil.e("toId：" + currentId);
                        if (getCurrentUser() != null) {
                                LogUtil.e("现在的UID：" + getCurrentUserObjectId());
                        }
//                        不是当前用户的情况下
                        LogUtil.e("不是当前的用户，不在服务器上关联该好友");
                        updateListener.onFailure(0, "不是当前的用户，不在服务器上关联该好友");
                }
        }

        /**
         * 根据用户ID获取用户信息
         *
         * @param uid          用户ID
         * @param findListener 回调
         */
        private void findUserById(String uid, FindListener<User> findListener) {
                BmobQuery<User> query = new BmobQuery<>();
                query.addWhereEqualTo("objectId", uid);
                query.findObjects(CustomApplication.getInstance(), findListener);
        }

        /**
         * 更新好友的黑名单状态
         *
         * @param username 好友用户名
         * @param isBlack  是否是黑名单
         */
        private long updateFriendBlackStatus(String username, boolean isBlack) {
                return ChatDB.create().updateFriendBlackStatus(username, isBlack);
        }

        /**
         * 添加为黑名单
         *
         * @param user                     用户实体
         * @param addBlackCallBackListener 回调
         */
        public void addToBlack(final User user, final AddBlackCallBackListener addBlackCallBackListener) {
                addBlackRelation(user, new UpdateListener() {
                                @Override
                                public void onSuccess() {
                                        long result1 = updateFriendBlackStatus(user.getUsername(), true);
                                        long result2 = ChatDB.create().deleteRecentMsg(user.getObjectId());
                                        if (result1 > 0 && result2 > 0) {
                                                addBlackCallBackListener.onSuccess(user);
                                        } else {
                                                addBlackCallBackListener.onFailed(new BmobException("在数据库更新用户黑名单状态失败或者是删除最近会话失败"));
                                        }
//                                        LogUtil.e("在服务器上关联该用户为黑名单失败");
                                }

                                @Override
                                public void onFailure(int i, String s) {
                                        LogUtil.e("在服务器上关联该用户为黑名单失败");
                                        addBlackCallBackListener.onFailed(new BmobException(s));
                                }
                        }
                );


        }

        private void addBlackRelation(User user, UpdateListener listener) {
                User currentUser = new User();
                currentUser.setObjectId(getCurrentUserObjectId());
                BmobRelation relation = new BmobRelation();
                relation.add(user);
                currentUser.setBlack(relation);
                currentUser.update(CustomApplication.getInstance(), listener);
        }

        /**
         * 取消黑名单回调
         *
         * @param user     用户实体
         * @param listener 回调
         */
        public void cancelBlack(final User user, final CancelBlackCallBlackListener listener) {
                deleteBlackRelation(user, new UpdateListener() {
                                @Override
                                public void onSuccess() {
                                        long result = updateFriendBlackStatus(user.getUsername(), false);
                                        if (result > 0) {
                                                listener.onSuccess(user);
                                        } else {
                                                listener.onFailed(new BmobException("在数据库中更新黑名单状态失败"));
                                        }
                                }

                                @Override
                                public void onFailure(int i, String s) {
                                        listener.onFailed(new BmobException(s));
                                }
                        }
                );
        }

        private void deleteBlackRelation(User user, UpdateListener listener) {
                User currentUser = new User();
                BmobRelation relation = new BmobRelation();
                relation.remove(user);
                currentUser.setBlack(relation);
                currentUser.setObjectId(getCurrentUserObjectId());
                currentUser.update(CustomApplication.getInstance(), listener);
        }

        private void bindInstallation(final UpdateListener listener) {
                BmobQuery<CustomInstallation> query = new BmobQuery<>();
                query.addWhereEqualTo("installationId", CustomInstallation.getInstallationId(CustomApplication.getInstance()));
                query.findObjects(CustomApplication.getInstance(), new FindListener<CustomInstallation>() {
                        @Override
                        public void onSuccess(List<CustomInstallation> list) {
                                if (list != null && list.size() > 0) {
                                        CustomInstallation installation = list.get(0);
                                        installation.setUid(UserManager.getInstance().getCurrentUserObjectId());
                                        installation.update(CustomApplication.getInstance(), new UpdateListener() {
                                                @Override
                                                public void onSuccess() {
                                                        LogUtil.e("绑定设备表中UID成功");
                                                        listener.onSuccess();
                                                }

                                                @Override
                                                public void onFailure(int i, String s) {
                                                        LogUtil.e("绑定设备表中UID设备失败");
                                                        listener.onFailure(i, s);

                                                }
                                        });
                                }
                        }

                        @Override
                        public void onError(int i, String s) {
                                LogUtil.e("在服务器上查询设备失败" + s + i);
                                listener.onFailure(i, s);
                        }
                });
        }

        /**
         * 检查本设备表中的uid   ，如果有，就发送下线通知，操作成功后，再把uid更新到本地的设备表中
         */
        public void checkInstallation(final UpdateListener listener) {
                BmobQuery<CustomInstallation> query = new BmobQuery<>();
                LogUtil.e("checkInstallation UID：" + getCurrentUserObjectId());
                query.addWhereEqualTo("uid", getCurrentUserObjectId());
                query.findObjects(CustomApplication.getInstance(), new FindListener<CustomInstallation>() {
                                @Override
                                public void onSuccess(List<CustomInstallation> list) {
                                        if (list != null && list.size() > 0) {
                                                CustomInstallation customInstallation = list.get(0);
                                                if (customInstallation.getInstallationId().equals(CustomInstallation.getInstallationId(CustomApplication.getInstance()))) {
                                                        LogUtil.e("由于绑定的是本设备表，不做操作所以");
                                                        listener.onSuccess();
                                                } else {
//                                                        不管推送成功与否，都要更新设备表的UID
                                                        MsgManager.getInstance().sendOfflineNotificationMsg(customInstallation, new PushListener() {
                                                                        @Override
                                                                        public void onSuccess() {
                                                                                LogUtil.e("推送下线通知消息成功");
                                                                                bindInstallation(listener);
                                                                        }

                                                                        @Override
                                                                        public void onFailure(int i, String s) {
                                                                                LogUtil.e("推送下线通知消息失败" + s + i);
                                                                                bindInstallation(listener);
                                                                        }
                                                                }
                                                        );
                                                }
                                        } else {
                                                LogUtil.e("查询不到本用户所对应的设备ID,这里新建一个设备表");
                                                bindInstallation(listener);
                                        }
                                }

                                @Override
                                public void onError(int i, String s) {
                                        LogUtil.e("查询本用户对应的设备表出错" + s + i);
                                        listener.onFailure(i, s);
                                }
                        }
                );
        }

        public void updateUserInfo(final String name, final String content, final UpdateListener listener) {
                 User user=new User();
                user.setObjectId(UserManager.getInstance().getCurrentUserObjectId());
                switch (name) {
                        case "phone":
                                user.setMobilePhoneNumber(content);
                                break;
                        case "email":
                                user.setEmail(content);
                                break;
                        case "nick":
                                user.setNick(content);
                                user.setSortedKey(CommonUtils.getSortedKey(content));
                                break;
                        case "avatar":
                                user.setAvatar(content);
                                break;
                        case "sex":
                                if (content.equals("男")) {
                                        user.setSex(true);
                                } else {
                                        user.setSex(false);
                                }
                                break;
                        case "signature":
                                user.setSignature(content);
                                break;
                        case "birth":
                                user.setBirthDay(content);
                                break;
                        case "address":
                                user.setAddress(content);
                                break;
                        case "location":
                                LogUtil.e("定位location" + content);
                                String result[] = content.split("&");
                                user.setLocation(new BmobGeoPoint(Double.parseDouble(result[0]), Double.parseDouble(result[1])));
                                break;
                        case "titleWallPaper":
                                user.setTitleWallPaper(content);
                                break;
                        case "wallPaper":
                                user.setWallPaper(content);
                                break;
                }
                user.update(CustomApplication.getInstance(), new UpdateListener() {
                        @Override
                        public void onSuccess() {
                                User currentUser = UserManager.getInstance().getCurrentUser();
                                switch (name) {
                                        case "phone":
                                                currentUser.setMobilePhoneNumber(content);
                                                break;
                                        case "email":
                                                currentUser.setEmail(content);
                                                break;
                                        case "nick":
                                                currentUser.setNick(content);
                                                currentUser.setSortedKey(CommonUtils.getSortedKey(content));
                                                break;
                                        case "avatar":
                                                currentUser.setAvatar(content);
                                                break;
                                        case "sex":
                                                if (content.equals("男")) {
                                                        currentUser.setSex(true);
                                                } else {
                                                        currentUser.setSex(false);
                                                }
                                                break;
                                        case "signature":
                                                currentUser.setSignature(content);
                                                break;
                                        case "birth":
                                                currentUser.setBirthDay(content);
                                                break;
                                        case "address":
                                                currentUser.setAddress(content);
                                                break;
                                        case "location":
                                                LogUtil.e("定位location" + content);
                                                String result[] = content.split("&");
                                                currentUser.setLocation(new BmobGeoPoint(Double.parseDouble(result[0]), Double.parseDouble(result[1])));
                                                break;
                                        case "titleWallPaper":
                                                currentUser.setTitleWallPaper(content);
                                                break;
                                        case "wallPaper":
                                                currentUser.setWallPaper(content);
                                                break;
                                }
                                listener.onSuccess();
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                listener.onFailure(i,s);

                        }
                });
        }

        public void queryNearbyPeople(double longitude, double latitude, boolean isAll,boolean isSex, FindListener<User> findListener) {
                BmobQuery<User> query = new BmobQuery<>();
                if (!isAll) {
                        query.addWhereEqualTo("sex", isSex);
                }
                query.addWhereNear("location", new BmobGeoPoint(longitude, latitude));
                query.addWhereNotEqualTo("objectId", UserManager.getInstance().getCurrentUser().getObjectId());
                query.findObjects(CustomApplication.getInstance(), findListener);
        }


        public void refreshUserInfo() {
                List<String> userList = UserCacheManager.getInstance().getAllUserId();
                if (userList != null && userList.size() > 0) {
                        for (final String uid :
                                userList) {
                                BmobQuery<User> query = new BmobQuery<>();
                                String lastTime = MessageCacheManager.getInstance().getUserDataLastUpdateTime(uid);
//                                        第一次断网查询用户数据
                                try {
                                        query.addWhereGreaterThan("updatedAt", new BmobDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(lastTime)));
                                        query.addWhereEqualTo("objectId", uid);
                                } catch (ParseException e) {
                                        e.printStackTrace();
                                        LogUtil.e("解析时间错误");
                                }
                                query.findObjects(CustomApplication.getInstance(), new FindListener<User>() {
                                        @Override
                                        public void onSuccess(List<User> list) {
                                                if (list != null && list.size() > 0) {
                                                        User user = list.get(0);
                                                        MessageCacheManager.getInstance().setUserDataLastUpdateTime(uid, user.getUpdatedAt());
                                                        ChatDB.create().addOrUpdateContacts(user);
                                                        UserCacheManager.getInstance().addContact(user);
                                                }
                                        }

                                        @Override
                                        public void onError(int i, String s) {
                                                LogUtil.e("断网期间内查询用户失败" + s + i);
                                        }
                                });
                        }
                }
        }
}
