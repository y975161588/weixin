package chen.testchat.manager;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import chen.testchat.CustomApplication;
import chen.testchat.base.Constant;
import chen.testchat.bean.BaseMessage;
import chen.testchat.bean.ChatMessage;
import chen.testchat.bean.CustomInstallation;
import chen.testchat.bean.GroupChatMessage;
import chen.testchat.bean.GroupTableMessage;
import chen.testchat.bean.HappyBean;
import chen.testchat.bean.HappyContentBean;
import chen.testchat.bean.ImageItem;
import chen.testchat.bean.PictureBean;
import chen.testchat.bean.RecentMsg;
import chen.testchat.bean.SharedMessage;
import chen.testchat.bean.User;
import chen.testchat.bean.WinXinBean;
import chen.testchat.db.ChatDB;
import chen.testchat.listener.AddFriendCallBackListener;
import chen.testchat.listener.AddShareMessageCallBack;
import chen.testchat.listener.DealCommentMsgCallBack;
import chen.testchat.listener.DealMessageCallBack;
import chen.testchat.listener.DealUserInfoCallBack;
import chen.testchat.listener.LoadShareMessageCallBack;
import chen.testchat.listener.OnCreateSharedMessageListener;
import chen.testchat.listener.OnReceiveListener;
import chen.testchat.listener.OnSendMessageListener;
import chen.testchat.listener.OnSendPushMessageListener;
import chen.testchat.listener.OnSendTagMessageListener;
import chen.testchat.listener.SendFileListener;
import chen.testchat.util.CommonUtils;
import chen.testchat.util.JsonUtil;
import chen.testchat.util.LogUtil;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobPushManager;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BmobDate;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.DeleteBatchListener;
import cn.bmob.v3.listener.DeleteListener;
import cn.bmob.v3.listener.FindCallback;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.PushListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadBatchListener;
import cn.bmob.v3.listener.UploadFileListener;

/**
 * 项目名称:    TestChat
 * 创建人:        陈锦军
 * 创建时间:    2016/10/9      12:16
 * QQ:             1981367757
 */
public class MsgManager {

        private static MsgManager instance;
        private BmobPushManager<CustomInstallation> mPushManager;
        /**
         * 用于单例模式的双重锁定
         */
        private static final Object LOCK = new Object();

        private MsgManager() {
                mPushManager = new BmobPushManager<>(CustomApplication.getInstance());
        }

        public static MsgManager getInstance() {
                if (instance == null) {
                        synchronized (LOCK) {
                                if (instance == null) {
                                        instance = new MsgManager();
                                }
                        }
                }
                return instance;
        }

        /**
         * 发送标签消息
         * 不保存在本地数据库    ,发送完后上传消息到服务器上面
         *
         * @param targetId 对方的ID
         * @param tag      消息的类型
         * @param listener 回调
         */
        public void sendTagMessage(final String targetId, final String tag, final OnSendTagMessageListener listener) {
                getUserById(targetId, new FindListener<User>() {
                                @Override
                                public void onSuccess(List<User> list) {
                                        if (list != null && list.size() > 0) {
                                                LogUtil.e("在服务器上查询好友成功");
                                                final ChatMessage msg = createTagMessage(list.get(0).getObjectId(), tag);
//                                  在这里发送完同意请求后，把消息转为对方发送的消息
                                                if (tag.equals(Constant.TAG_AGREE)) {
                                                        RecentMsg recentMsg = new RecentMsg();
                                                        recentMsg.setNick(list.get(0).getNick());
                                                        recentMsg.setAvatar(list.get(0).getAvatar());
                                                        recentMsg.setMsgType(Constant.TAG_MSG_TYPE_TEXT);
                                                        recentMsg.setLastMsgContent(msg.getContent());
                                                        recentMsg.setBelongId(list.get(0).getObjectId());
                                                        recentMsg.setName(list.get(0).getUsername());
                                                        recentMsg.setTime(msg.getCreateTime());
                                                        LogUtil.e(recentMsg);
                                                        LogUtil.e("保存同意消息到最近会话列表中");
                                                        ChatDB.create().saveRecentMessage(recentMsg);
                                                        LogUtil.e("保存同意消息到聊天消息表中");
                                                        ChatDB.create().saveChatMessage(msg);
                                                }
                                                sendJsonMessage(list.get(0).getInstallId(), createJsonMessage(msg),
                                                        new OnSendPushMessageListener() {
                                                                @Override
                                                                public void onSuccess() {
                                                                        LogUtil.e("发送json推送消息成功");
                                                                        msg.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                                                                        LogUtil.e("上传同意消息到服务器上");
                                                                        saveMessageToService(msg);
                                                                        listener.onSuccess(msg);
                                                                }

                                                                @Override
                                                                public void onFailed(BmobException e) {
//                                                                推送失败后也要把消息保存到服务器上，方便在接收方那边从服务器上拉去数据
                                                                        LogUtil.e("推送失败保存消息到服务器上面");
                                                                        msg.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                                                                        saveMessageToService(msg);
                                                                        listener.onSuccess(msg);
                                                                }
                                                        }
                                                );
                                        } else {
                                                LogUtil.e("未查到该用户" + targetId);
                                                listener.onFailed(new BmobException("服务器上没有该用户"));
                                        }
                                }

                                @Override
                                public void onError(int i, String s) {
                                        LogUtil.e("在服务器上查询用户失败" + s + i);
                                        listener.onFailed(new BmobException("在服务器上查询用户失败"));
                                }
                        }
                );


        }

        /**
         * 上传消息到服务器中
         *
         * @param msg 消息
         */
        private void saveMessageToService(ChatMessage msg) {
                msg.save(CustomApplication.getInstance(), new SaveListener() {
                                @Override
                                public void onSuccess() {
                                        LogUtil.e("保存消息到服务器上成功");
                                }

                                @Override
                                public void onFailure(int i, String s) {
                                        LogUtil.e("保存消息到服务器上失败:" + s + i);
                                }
                        }
                );
        }


        /**
         * 推送消息
         *
         * @param installId 对方用户设备ID
         * @param message   可推送的消息  JSONObject
         * @param listener  回调
         */
        private void sendJsonMessage(String installId, final JSONObject message, final OnSendPushMessageListener listener) {
                sendJsonMessage(installId, message, new PushListener() {
                                @Override
                                public void onSuccess() {
                                        LogUtil.e("推送消息成功");
                                        listener.onSuccess();
                                }

                                @Override
                                public void onFailure(int i, String s) {
                                        LogUtil.e("推送消息失败" + s + i);
                                        listener.onFailed(new BmobException(s));
                                }
                        }
                );
        }

        /**
         * 创建json
         *
         * @param message 消息
         * @return 放回json
         */
        private JSONObject createJsonMessage(ChatMessage message) {
                try {
                        JSONObject result = new JSONObject();
//                        添加为好友标签
                        switch (message.getTag()) {
                                case Constant.TAG_ADD_FRIEND:
                                        result.put(Constant.MESSAGE_TAG, message.getTag());
                                        result.put(Constant.TAG_BELONG_AVATAR, message.getBelongAvatar());
                                        result.put(Constant.TAG_BELONG_NICK, message.getBelongNick());
                                        result.put(Constant.TAG_BELONG_NAME, message.getBelongUserName());
                                        result.put(Constant.TAG_CREATE_TIME, message.getCreateTime());
                                        result.put(Constant.TAG_MESSAGE_READ_STATUS, message.getReadStatus());
//                                已读回执标签
                                        break;
                                case Constant.TAG_ASK_READ:
                                        result.put(Constant.TAG_CONVERSATION, message.getConversationId());
                                        result.put(Constant.TAG_CREATE_TIME, message.getCreateTime());
                                        result.put(Constant.MESSAGE_TAG, message.getTag());
                                        result.put(Constant.TAG_MESSAGE_READ_STATUS, message.getReadStatus());
                                        break;
                                case Constant.TAG_AGREE:
//                                        alert = message.getBelongUserName() + "已同意添加你为好友";
                                        result.put(Constant.MESSAGE_TAG, message.getTag());
                                        result.put(Constant.TAG_BELONG_AVATAR, message.getBelongAvatar());
                                        result.put(Constant.TAG_BELONG_NICK, message.getBelongNick());
                                        result.put(Constant.TAG_BELONG_NAME, message.getBelongUserName());
                                        result.put(Constant.TAG_CREATE_TIME, message.getCreateTime());
                                        result.put(Constant.TAG_CONTENT, message.getContent());
                                        result.put(Constant.TAG_CONVERSATION, message.getConversationId());
                                        result.put(Constant.TAG_MESSAGE_READ_STATUS, message.getReadStatus());
                                        result.put(Constant.TAG_MESSAGE_TYPE, message.getMsgType());
                                        break;
                                default:
                                        break;
                        }
                        result.put(Constant.TAG_BELONG_ID, message.getBelongId());
                        result.put(Constant.TAG_TO_ID, message.getToId());
                        LogUtil.e("组装后的json:" + result.toString());
                        return result;
                } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                }
        }

        /**
         * 创建标签实体消息类  目前有的标签类型消息：1、邀请消息2、同意消息3、回执消息
         *
         * @param targetId 对方ID
         * @param tag      标签
         * @return 标签实体类
         */
        private ChatMessage createTagMessage(String targetId, String tag) {
                User user = UserManager.getInstance().getCurrentUser();
                ChatMessage message = new ChatMessage();
                if (tag.equals(Constant.TAG_AGREE)) {
                        message.setContent("你们已经成为好友可以聊天啦啦啦");
                        message.setConversationId(user.getObjectId() + "&" + targetId);
                }
                message.setMsgType(Constant.TAG_MSG_TYPE_TEXT);
                message.setToId(targetId);
                message.setTag(tag);
                message.setBelongAvatar(user.getAvatar());
                message.setBelongNick(user.getNick());
                message.setBelongUserName(user.getUsername());
                message.setBelongId(user.getObjectId());
                message.setCreateTime(String.valueOf(System.currentTimeMillis()));
                message.setReadStatus(Constant.READ_STATUS_UNREAD);
                return message;
        }

        /**
         * 根据用户id到后台服务器去查找用户
         *
         * @param targetId     对方用户ID
         * @param findListener 回调
         */
        private void getUserById(String targetId, FindListener<User> findListener) {
                BmobQuery<User> query = new BmobQuery<>();
                query.addWhereEqualTo("objectId", targetId);
                query.findObjects(CustomApplication.getInstance(), findListener);
        }

        /**
         * 从json数据中解析和组成接受的聊天消息(不是标签消息)   并保存和上传到服务器上和发回执消息
         *
         * @param json     json数据
         * @param listener 回调
         */
        public void createReceiveMsg(String json, final OnReceiveListener listener) {
                try {
                        JSONObject jsonObject = new JSONObject(json);
                        final ChatMessage message = new ChatMessage();
                        message.setTag(JsonUtil.getString(jsonObject, Constant.MESSAGE_TAG));
                        message.setToId(JsonUtil.getString(jsonObject, Constant.TAG_TO_ID));
                        message.setBelongId(JsonUtil.getString(jsonObject, Constant.TAG_BELONG_ID));
                        message.setBelongUserName(JsonUtil.getString(jsonObject, Constant.TAG_BELONG_NAME));
                        message.setCreateTime(JsonUtil.getString(jsonObject, Constant.TAG_CREATE_TIME));
                        message.setMsgType(JsonUtil.getInt(jsonObject, Constant.TAG_MESSAGE_TYPE));
                        message.setBelongAvatar(JsonUtil.getString(jsonObject, Constant.TAG_BELONG_AVATAR));
                        message.setBelongNick(JsonUtil.getString(jsonObject, Constant.TAG_BELONG_NICK));
                        message.setConversationId(JsonUtil.getString(jsonObject, Constant.TAG_CONVERSATION));
                        message.setReadStatus(Constant.RECEIVE_UNREAD);
                        message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                        message.setContent(JsonUtil.getString(jsonObject, Constant.TAG_CONTENT));
                        String tag = message.getTag();
                        if (tag != null && !tag.equals("")) {
                                switch (tag) {
                                        case Constant.TAG_AGREE:
                                                LogUtil.e("接收到同意消息");
                                                if (ChatDB.create().isExistChatMessage(message.getConversationId(), message.getCreateTime())) {
                                                        LogUtil.e("接受推送过来的同意消息，数据库中已经存在，不需要再接受");
                                                        return;
                                                }

                                                UserManager.getInstance().addNewFriend(message.getBelongId(), message.getToId(), new AddFriendCallBackListener() {
                                                        @Override
                                                        public void onSuccess(User user) {
                                                                if (saveAndUploadReceiverMessage(true, message)) {
                                                                        if (UserManager.getInstance().getCurrentUser() != null && UserManager.getInstance().getCurrentUserObjectId().equals(message.getToId())) {
                                                                                UserCacheManager.getInstance().addContact(user);
                                                                        }
                                                                        listener.onSuccess(message);
                                                                } else {
                                                                        listener.onFailed(new BmobException("保存同意消息到最近会话列表或聊天消息表中失败"));
                                                                }
                                                        }

                                                        @Override
                                                        public void onFailed(BmobException e) {
                                                                listener.onFailed(e);
                                                        }
                                                });
                                                break;
                                        case Constant.TAG_ADD_FRIEND:
                                                if (ChatDB.create().hasInvitation(message.getBelongId(), message.getCreateTime())) {
                                                        LogUtil.e("已经有请求消息拉，这里就不许要再次保存");
                                                        return;
                                                }
                                                if (saveAndUpdateInvitationMsg(message) > 0) {
                                                        listener.onSuccess(message);
                                                } else {
                                                        listener.onFailed(new BmobException("保存邀请消息到数据库中失败!!"));
                                                }
                                                break;
                                        case Constant.TAG_ASK_READ:
                                                updateReadTagMsgReaded(message.getConversationId(), message.getCreateTime());
                                                if (uploadAndUpdateChatMessageReadStatus(message, true) > 0) {
                                                        listener.onSuccess(message);
                                                } else {
                                                        listener.onFailed(new BmobException("更新聊天消息已读状态失败"));
                                                }
                                                break;
                                        default:
                                                break;
                                }
                        } else {
//                                聊天消息
//                                接收到的消息有种情况，1、推送接收到的消息，已经在检测得到了，所以推送的就不要了
                                if (ChatDB.create().isExistChatMessage(message.getConversationId(), message.getCreateTime())) {
                                        LogUtil.e("接受推送过来的聊天" +
                                                "消息，数据库中已经存在，不需要再接受");
                                        return;
                                }
                                if (saveAndUploadReceiverMessage(true, message)) {
                                        listener.onSuccess(message);
                                } else {
                                        listener.onFailed(new BmobException("保存聊天消息到最近会话列表或聊天消息表中失败"));
                                }
                        }
                } catch (JSONException e) {
                        e.printStackTrace();
                }
        }

        public GroupChatMessage createReceiveGroupChatMsg(JSONObject jsonObject) {
                GroupChatMessage message = new GroupChatMessage();
                message.setGroupId(JsonUtil.getString(jsonObject, Constant.GROUP_ID));
                message.setContent(JsonUtil.getString(jsonObject, Constant.TAG_CONTENT));
                message.setReadStatus(Constant.RECEIVE_UNREAD);
                message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
//                message.setReadStatus(JsonUtil.getInt(jsonObject, Constant.TAG_MESSAGE_READ_STATUS));
//                message.setSendStatus(JsonUtil.getInt(jsonObject, Constant.TAG_MESSAGE_SEND_STATUS));
                message.setCreateTime(JsonUtil.getString(jsonObject, Constant.TAG_CREATE_TIME));
                message.setConversationType(JsonUtil.getString(jsonObject, Constant.TYPE_CONVERSATION));
                message.setBelongAvatar(JsonUtil.getString(jsonObject, Constant.TAG_BELONG_AVATAR));
                message.setBelongId(JsonUtil.getString(jsonObject, Constant.TAG_BELONG_ID));
                message.setBelongNick(JsonUtil.getString(jsonObject, Constant.TAG_BELONG_NICK));
                message.setBelongUserName(JsonUtil.getString(jsonObject, Constant.TAG_BELONG_NAME));
                message.setMsgType(JsonUtil.getInt(jsonObject, Constant.TAG_MESSAGE_TYPE));
                message.setObjectId(JsonUtil.getString(jsonObject, Constant.ID));
                return message;
        }

        /**
         * 1、发送回执
         * 2、在服务器上更新该消息的读取状态为已读，防止被定时服务再次拉取到
         * 3、保存聊天消息到数据库中
         * 4、保存最近会话消息到数据库中
         * <p>
         * <p>
         * <p>
         * 保存接收到的聊天消息(包括同意欢迎消息)到数据库中和发送回执
         *
         * @param isAskRead   是否要求进行回执发送
         * @param baseMessage 消息
         * @return 返回保存到数据库中是否成功的结果
         */
        public boolean saveAndUploadReceiverMessage(boolean isAskRead, BaseMessage baseMessage) {
                RecentMsg recentMsg = new RecentMsg();
                if (baseMessage instanceof ChatMessage) {
                        ChatMessage message = (ChatMessage) baseMessage;
                        String toId = message.getToId();
//                        发送回执
                        if (isAskRead) {
                                sendAskReadMsg(message.getConversationId(), message.getCreateTime());
                        }
//                不管发送回执消息成功与否都要在服务器上更新消息的已读状态
                        if (UserManager.getInstance().getCurrentUser() != null && UserManager.getInstance().getCurrentUserObjectId().equals(toId)) {
                                updateMsgReaded(false, message.getConversationId(), message.getCreateTime());
                        }
//                保存聊天消息
                        long chatResult = ChatDB.create(toId).saveChatMessage(message);
                        //                这里保存最近会话消息
                        recentMsg = createChatRecentMsg(baseMessage);
                        long recentResult = ChatDB.create(toId).saveRecentMessage(recentMsg);
                        return chatResult > 0 && recentResult > 0;
                } else {
                        LogUtil.e("未知类型的baseMessage");
                        return false;
                }
        }


        public RecentMsg createChatRecentMsg(BaseMessage baseMessage) {
                RecentMsg recentMsg = new RecentMsg();
                recentMsg.setBelongId(baseMessage.getBelongId());
                recentMsg.setMsgType(baseMessage.getMsgType());
                recentMsg.setTime(baseMessage.getCreateTime());
                recentMsg.setAvatar(baseMessage.getBelongAvatar());
                recentMsg.setNick(baseMessage.getBelongNick());
                recentMsg.setName(baseMessage.getBelongUserName());
                recentMsg.setLastMsgContent(baseMessage.getContent());
                return recentMsg;
        }


        /**
         * 根据会话ID和消息的创建时间来发送一个回执已读消息
         *
         * @param conversationId 会话id
         * @param createTime     消息创建时间
         */
        private void sendAskReadMsg(final String conversationId, final String createTime) {
                getUserById(conversationId.split("&")[0],
                        new FindListener<User>() {
                                @Override
                                public void onSuccess(List<User> list) {
                                        if (list != null && list.size() > 0) {
                                                final ChatMessage chatMessage = createAskReadMessage(conversationId, createTime);
                                                sendJsonMessage(list.get(0).getInstallId(), createJsonMessage(chatMessage), new OnSendPushMessageListener() {
                                                        @Override
                                                        public void onSuccess() {
                                                                LogUtil.e("发送回执已读json消息成功");
//                                                        发送已读回执消息也要上传到服务器上面
                                                                chatMessage.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                                                                saveMessageToService(chatMessage);
                                                        }

                                                        @Override
                                                        public void onFailed(BmobException e) {
                                                                LogUtil.e("发送回执json消息成功失败" + e.getMessage() + e.getErrorCode());
                                                                LogUtil.e("发送失败也要保存回执已读消息到服务器上面啊");
                                                                chatMessage.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                                                                saveMessageToService(chatMessage);

                                                        }
                                                });
                                        }
                                }

                                @Override
                                public void onError(int i, String s) {
                                        LogUtil.e("查找用户失败" + s + i);

                                }
                        }
                );
        }

        /**
         * 根据会话ID和创建时间来创建回执消息
         *
         * @param conversationId 会话ID
         * @param createTime     该聊天的创建时间
         * @return 聊天消息实体
         */

        private ChatMessage createAskReadMessage(String conversationId, String createTime) {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setToId(conversationId.split("&")[0]);
                chatMessage.setBelongId(conversationId.split("&")[1]);
                chatMessage.setConversationId(conversationId);
                chatMessage.setCreateTime(createTime);
                chatMessage.setReadStatus(Constant.READ_STATUS_UNREAD);
                chatMessage.setTag(Constant.TAG_ASK_READ);
                return chatMessage;
        }

        /**
         * 在服务器上面更新聊天消息的已读状态(首先根据用户ID在服务器上查询获取该消息,然后再更新该消息)
         *
         * @param isBelongId 查询的标志
         * @param id         会话ID或者是belongID
         * @param createTime 创建时间
         */
        public void updateMsgReaded(boolean isBelongId, String id, String createTime) {
                queryMsg(isBelongId, id, createTime, new FindListener<ChatMessage>() {
                                @Override
                                public void onSuccess(List<ChatMessage> list) {
                                        if (list != null && list.size() > 0) {
                                                final ChatMessage chatMessage = list.get(0);
                                                chatMessage.setReadStatus(Constant.READ_STATUS_READED);
                                                chatMessage.update(CustomApplication.getInstance(), new UpdateListener() {
                                                                @Override
                                                                public void onSuccess() {
                                                                        LogUtil.e("更新服务器上的消息已读状态成功11111");
                                                                }

                                                                @Override
                                                                public void onFailure(int i, String s) {
                                                                        LogUtil.e("更新服务器上的消息已读消息失败" + s + i);
                                                                }
                                                        }
                                                );
                                        }
                                }

                                @Override
                                public void onError(int i, String s) {
                                        LogUtil.e("查找聊天消息失败" + s + i);

                                }
                        }
                );
        }

        /**
         * 根据是否是标签和ID值(conversation 或  belongId)和创建时间来查询消息
         *
         * @param isBelongId   是否是标签消息
         * @param id           会话id或者是用户ID
         * @param createTime   创建时间
         * @param findListener 找到消息的回调
         */
        private void queryMsg(boolean isBelongId, String id, String createTime, FindListener<ChatMessage> findListener) {
                BmobQuery<ChatMessage> query = new BmobQuery<>();
                if (isBelongId) {
                        query.addWhereEqualTo("belongId", id);
                } else {
                        query.addWhereEqualTo("conversationId", id);
                        query.addWhereNotEqualTo("tag", Constant.TAG_ASK_READ);
                }
                query.addWhereEqualTo("createTime", createTime);
                query.findObjects(CustomApplication.getInstance(), findListener);
        }


        /**
         * 更新已读状态到服务器上,然后保存邀请消息到数据库只中
         *
         * @param message 消息
         * @return 结果
         */
        public long saveAndUpdateInvitationMsg(ChatMessage message) {
//                当当前用户是该消息接受者的时候，更新服务器上该消息为已读状态,否则就不更新，以便以后登录其他设备的时候可以通过在服务器上搜索未读取状态在检测到该数据
                updateMsgReaded(true, message.getBelongId(), message.getCreateTime());
                return ChatDB.create(message.getToId()).saveInvitationMsg(message);
        }


        /**
         * 重新发送text消息
         *
         * @param baseMessage 消息实体
         * @param listener    回调
         */
        public void resendTextChatMessage(BaseMessage baseMessage, final OnSendMessageListener listener) {
                sendTextMessage(true, baseMessage, listener);
        }


        public void sendTextMessage(boolean isResend, BaseMessage message, OnSendMessageListener listener) {
                message.setSendStatus(Constant.SEND_STATUS_SENDING);
                listener.onSending();
                if (!CommonUtils.isNetWorkAvailable(CustomApplication.getInstance())) {
                        saveAndUploadSendMessage(false, message);
                        LogUtil.e("没有网络请检查下网络配置");
                        listener.onFailed(new BmobException("没有网络请检查下网络配置"));
                        return;
                }
                if (message.getConversationType().equals(Constant.TYPE_CONVERSATION_PERSON) && ChatDB.create().isBlackUser(((ChatMessage) message).getToId())) {
                        message.setSendStatus(Constant.SEND_STATUS_FAILED);
                        if (!isResend) {
                                ChatDB.create().saveChatMessage((ChatMessage) message);
                                LogUtil.e("该用户为黑名单......不能发送消息");
                        }
                        listener.onFailed(new BmobException("黑名单用户不能发消息"));
                        return;
                }
                realSendTextMessage(isResend, message, listener);
        }


        /**
         * 真正发送文本消息的方法
         *
         * @param isResend 是否是重发
         * @param message  消息实体
         * @param listener 回调
         */
        private void realSendTextMessage(final boolean isResend, final BaseMessage message, final OnSendMessageListener listener) {
                if (message.getConversationType().equals(Constant.TYPE_CONVERSATION_PERSON)) {
                        String uid = ((ChatMessage) message).getToId();
                        User user = UserCacheManager.getInstance().getUser(uid);
                        final JSONObject jsonObject = createSendJsonTextMessage(message);
                        if (user == null) {
                                getUserById(uid, new FindListener<User>() {
                                                @Override
                                                public void onSuccess(List<User> list) {
                                                        if (list != null && list.size() > 0) {
                                                                LogUtil.e("在服务器上查询用户成功");
                                                                sendJsonMessage(list.get(0).getInstallId(), jsonObject,
                                                                        new OnSendPushMessageListener() {
                                                                                @Override
                                                                                public void onSuccess() {
                                                                                        //      服务器上保存的图片消息内容是服务器那边的URL
                                                                                        message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                                                                                        saveAndUploadSendMessage(true, message);
                                                                                        listener.onSuccess();
                                                                                }

                                                                                @Override
                                                                                public void onFailed(BmobException e) {
                                                                                        message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                                                                                        saveAndUploadSendMessage(true, message);
                                                                                        listener.onSuccess();
                                                                                }
                                                                        });
                                                        } else {
                                                                message.setSendStatus(Constant.SEND_STATUS_FAILED);
                                                                if (!isResend) {
                                                                        saveAndUploadSendMessage(false, message);
                                                                }
                                                                listener.onFailed(new BmobException("服务器上查询不到该用户"));
                                                        }
                                                }

                                                @Override
                                                public void onError(int i, String s) {
                                                        message.setSendStatus(Constant.SEND_STATUS_FAILED);
                                                        if (!isResend) {
                                                                saveAndUploadSendMessage(false, message);
                                                        }
                                                        listener.onFailed(new BmobException(s));

                                                }
                                        }
                                );
                        } else {
                                sendJsonMessage(user.getInstallId(), jsonObject, new OnSendPushMessageListener() {
                                        @Override
                                        public void onSuccess() {
                                                message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                                                saveAndUploadSendMessage(true, message);
                                                listener.onSuccess();
                                        }

                                        @Override
                                        public void onFailed(BmobException e) {
                                                message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                                                saveAndUploadSendMessage(true, message);
                                                listener.onSuccess();
                                        }
                                });
                        }
                } else {
//                        群消息发送
                        message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                        saveAndUploadGroupChatMessage(((GroupChatMessage) message), new SaveListener() {
                                @Override
                                public void onSuccess() {
                                        saveRecentAndChatGroupMessage(((GroupChatMessage) message));
                                        LogUtil.e("保存群消息到服务器上成功");
                                        listener.onSuccess();
                                }

                                @Override
                                public void onFailure(int i, String s) {
                                        LogUtil.e("保存群消息到服务器上失败" + s + i);
                                        listener.onFailed(new BmobException(i, s));
                                }
                        });
                }
        }

        /**
         * 保存群消息到我们实时监听的群消息表中，这里每当我们在上面保存一个群消息的时候，每个成员都能实时获取到该群消息
         *
         * @param message
         * @param listener
         */
        private void saveAndUploadGroupChatMessage(GroupChatMessage message, SaveListener listener) {
                message.setTableName("g" + message.getGroupId());
                message.save(CustomApplication.getInstance(), listener);
        }


        public void queryGroupChatMessage(String groupId, FindListener<GroupChatMessage> listener) {
                if (groupId == null) {
                        return;
                }
                List<String> list = new ArrayList<>();
                list.add(groupId);
                queryGroupChatMessage(list, listener);
        }


        public void queryGroupChatMessage(List<String> groupIdList, final FindListener<GroupChatMessage> listener) {
                if (groupIdList!=null&&groupIdList.size() > 0) {
                        LogUtil.e("12群id列表如下");
                        for (int i = 0; i < groupIdList.size(); i++) {
                                String groupId = groupIdList.get(i);
                                LogUtil.e(groupId);
                                BmobQuery<GroupChatMessage> query = new BmobQuery<>("g" + groupId);
                                final RecentMsg recentMsg = ChatDB.create().getRecentMsg(groupId);
                                long lastTime;
                                if (recentMsg == null) {
                                        lastTime = 0;
                                } else {
                                        lastTime = Long.valueOf(ChatDB.create().getRecentMsg(groupId).getTime());
                                }
                                query.addWhereGreaterThan("updatedAt", new BmobDate(new Date(lastTime)));
//                                query.addWhereEqualTo("groupId", groupId);
                                query.findObjects(CustomApplication.getInstance(), new FindCallback() {
                                        @Override
                                        public void onSuccess(JSONArray jsonArray) {
                                                LogUtil.e("群消息解析");
                                                LogUtil.e("jsonArray：" + jsonArray.toString());
                                                List<GroupChatMessage> list = new ArrayList<>();
                                                for (int i = 0; i < jsonArray.length(); i++) {
                                                        try {
                                                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                                                GroupChatMessage groupChatMessage = MsgManager.getInstance().createReceiveGroupChatMsg(jsonObject);
                                                                groupChatMessage.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                                                                groupChatMessage.setReadStatus(Constant.RECEIVE_UNREAD);
                                                                list.add(groupChatMessage);
                                                        } catch (JSONException e) {
                                                                e.printStackTrace();
                                                        }
                                                }
                                                listener.onSuccess(list);
                                        }

                                        @Override
                                        public void onFailure(int i, String s) {
                                                LogUtil.e("查询断网期间的群消息失败：" + s + i);
                                                listener.onError(i, s);
                                        }
                                });
                        }
                }
        }


        /**
         * 创建聊天消息json
         *
         * @param baseMessage 聊天消息实体
         * @return JSONObject
         */
        private JSONObject createSendJsonTextMessage(BaseMessage baseMessage) {
                try {
                        JSONObject result = new JSONObject();
                        result.put(Constant.TAG_BELONG_NAME, baseMessage.getBelongUserName());
                        result.put(Constant.TAG_BELONG_NICK, baseMessage.getBelongNick());
                        result.put(Constant.TAG_BELONG_AVATAR, baseMessage.getBelongAvatar());
                        result.put(Constant.TAG_BELONG_ID, baseMessage.getBelongId());
                        result.put(Constant.TAG_MESSAGE_TYPE, baseMessage.getMsgType());
                        result.put(Constant.TAG_CREATE_TIME, baseMessage.getCreateTime());
                        result.put(Constant.TAG_MESSAGE_SEND_STATUS, Constant.SEND_STATUS_SUCCESS);
                        result.put(Constant.TAG_MESSAGE_READ_STATUS, baseMessage.getReadStatus());
                        result.put(Constant.TAG_CONTENT, baseMessage.getContent());
                        result.put(Constant.TYPE_CONVERSATION, baseMessage.getConversationType());
                        if (baseMessage instanceof ChatMessage) {
                                ChatMessage message = (ChatMessage) baseMessage;
                                result.put(Constant.TAG_CONVERSATION, message.getConversationId());
                                result.put(Constant.TAG_TO_ID, message.getToId());
                                result.put(Constant.MESSAGE_TAG, message.getTag());
                        } else if (baseMessage instanceof GroupChatMessage) {
                                GroupChatMessage message = (GroupChatMessage) baseMessage;
                                result.put(Constant.GROUP_ID, message.getGroupId());
                        }
                        return result;
                } catch (JSONException e) {
                        e.printStackTrace();
                        return null;
                }
        }

        /**
         * 保存消息到数据库和上传到服务器上
         *
         * @param isSuccess   是否成功发送了
         * @param baseMessage 消息实体
         */
        private void saveAndUploadSendMessage(boolean isSuccess, BaseMessage baseMessage) {
                if (!isSuccess) {
                        baseMessage.setSendStatus(Constant.SEND_STATUS_FAILED);
                } else {
                        baseMessage.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                        if (baseMessage instanceof ChatMessage) {
                                uploadSendMessage((ChatMessage) baseMessage);
                        } else if (baseMessage instanceof GroupChatMessage) {
                                uploadSendMessage(((GroupChatMessage) baseMessage));
                        }
                }
//                这里的消息是发送的消息，如果要保存到最近会话列表中，必须要弄到对方用户的资料,所以......
//                1、判断是否是好友
                if (baseMessage instanceof ChatMessage) {
                        final ChatMessage message = (ChatMessage) baseMessage;
                        ChatDB.create().saveChatMessage(message);
                        User user = null;
                        if (UserCacheManager.getInstance().getUser(message.getToId()) != null) {
                                user = UserCacheManager.getInstance().getUser(message.getToId());
                        } else {
                                getUserById(message.getToId(), new FindListener<User>() {
                                                @Override
                                                public void onSuccess(List<User> list) {
                                                        if (list != null && list.size() > 0) {
                                                                RecentMsg recentMsg = new RecentMsg();
                                                                User user1 = list.get(0);
                                                                recentMsg.setName(user1.getUsername());
                                                                recentMsg.setBelongId(user1.getObjectId());
                                                                recentMsg.setAvatar(user1.getAvatar());
                                                                recentMsg.setTime(message.getCreateTime());
                                                                recentMsg.setNick(user1.getNick());
                                                                recentMsg.setLastMsgContent(message.getContent());
                                                                recentMsg.setMsgType(message.getMsgType());
                                                                ChatDB.create().saveRecentMessage(recentMsg);
                                                        }
                                                }

                                                @Override
                                                public void onError(int i, String s) {
                                                        LogUtil.e("在服务器上查询用户失败" + s + i);
                                                }
                                        }
                                );
                        }
                        if (user != null) {
                                RecentMsg recentMsg = new RecentMsg();
                                recentMsg.setMsgType(message.getMsgType());
                                recentMsg.setBelongId(user.getObjectId());
                                recentMsg.setAvatar(user.getAvatar());
                                recentMsg.setNick(user.getNick());
                                recentMsg.setLastMsgContent(message.getContent());
                                recentMsg.setName(user.getUsername());
                                recentMsg.setTime(message.getCreateTime());
                                LogUtil.e("保存最近消息到数据库中成功");
                                ChatDB.create().saveRecentMessage(recentMsg);
                        }
                } else if (baseMessage instanceof GroupChatMessage) {
                        LogUtil.e("保存到最近群会话和群消息数据库中");
                        saveRecentAndChatGroupMessage(((GroupChatMessage) baseMessage));
                }
        }

        /**
         * 更具群消息来保存群消息和最近群消息
         *
         * @param groupChatMessage 群消息实体
         * @return 结果
         */
        public boolean saveRecentAndChatGroupMessage(final GroupChatMessage groupChatMessage) {
                long chatResult;
                long recentResult;
                LogUtil.e("群消息");
                chatResult = ChatDB.create().saveGroupChatMessage(groupChatMessage);
//                        明天从这里开始,首先要在内存中保存群组消息，然后判断是否有该群配置消息，若无，则在服务器上获取
                GroupTableMessage message = MessageCacheManager.getInstance().getGroupTableMessage(groupChatMessage.getGroupId());
                if (message != null) {
                        LogUtil.e(message);
                        RecentMsg recentMsg = createGroupRecentMsg(groupChatMessage, message);
                        recentResult = ChatDB.create(message.getToId()).saveRecentMessage(recentMsg);
                } else {
                        recentResult = 1;
                        LogUtil.e("111111如果内存中没有该群结构消息，则从服务器上获取,并存入内存中");
//                        getGroupTableMessageById(groupChatMessage.getGroupId(), new FindListener<GroupTableMessage>() {
//                                        @Override
//                                        public void onSuccess(List<GroupTableMessage> list) {
//                                                if (list != null && list.size() > 0) {
//                                                        LogUtil.e("群结构大小应该为" + list.size());
//                                                        MessageCacheManager.getInstance().addGroupTableMessage(list.get(0));
//                                                        GroupTableMessage message = list.get(0);
//                                                        RecentMsg recentMsg = new RecentMsg();
//                                                        recentMsg.setMsgType(groupChatMessage.getMsgType());
//                                                        recentMsg.setBelongId(groupChatMessage.getGroupId());
//                                                        recentMsg.setTime(groupChatMessage.getCreateTime());
//                                                        recentMsg.setLastMsgContent(groupChatMessage.getContent());
//                                                        recentMsg.setName(message.getGroupName());
//                                                        recentMsg.setAvatar(message.getGroupAvatar());
////                                                到时候再设置群昵称
//                                                        recentMsg.setNick(message.getGroupNick());
//                                                        ChatDB.create(message.getToId()).saveRecentMessage(recentMsg);
//                                                } else {
//                                                        LogUtil.e("服务器上没有查询得到该用户所拥有的群");
//                                                }
//                                        }
//
//                                        @Override
//                                        public void onError(int i, String s) {
//                                                LogUtil.e("在服务器上查找群结构表失败" + s + i);
//                                        }
//                                }
//                        );
                }
                return chatResult > 0 && recentResult > 0;
        }

        public RecentMsg createGroupRecentMsg(GroupChatMessage groupChatMessage, GroupTableMessage message) {
                RecentMsg recentMsg = new RecentMsg();
                recentMsg.setMsgType(groupChatMessage.getMsgType());
                recentMsg.setBelongId(groupChatMessage.getGroupId());
                recentMsg.setTime(groupChatMessage.getCreateTime());
                recentMsg.setName(message.getGroupName());
                recentMsg.setAvatar(message.getGroupAvatar());
                recentMsg.setLastMsgContent(groupChatMessage.getContent());
//                                                到时候再设置群昵称
//                recentMsg.setNick(message.getGroupName());
                return recentMsg;
        }


        private void getGroupTableMessageById(String groupId, FindListener<GroupTableMessage> findListener) {
                BmobQuery<GroupTableMessage> query = new BmobQuery<>();
                query.addWhereEqualTo(Constant.GROUP_ID, groupId);
                query.addWhereEqualTo(Constant.TAG_TO_ID, UserManager.getInstance().getCurrentUserObjectId());
                query.findObjects(CustomApplication.getInstance(), findListener);
        }

        private void uploadSendMessage(GroupChatMessage groupChatMessage) {
                groupChatMessage.save(CustomApplication.getInstance(), new SaveListener() {
                                @Override
                                public void onSuccess() {
                                        LogUtil.e("上传群消息到服务器中成功");
                                }

                                @Override
                                public void onFailure(int i, String s) {
                                        LogUtil.e("上传群消息到服务器上失败" + s + i);
                                }
                        }
                );
        }

        /**
         * @param message 上传和保存消息到服务器上
         */
        private void uploadSendMessage(ChatMessage message) {
                message.save(CustomApplication.getInstance(), new SaveListener() {
                                @Override
                                public void onSuccess() {
                                        LogUtil.e("上传个人消息到服务器中成功");
                                }

                                @Override
                                public void onFailure(int i, String s) {
                                        LogUtil.e("上传个人消息到服务器上失败" + s + i);
                                }
                        }
                );
        }

        /**
         * 根据内容和目标ID创建消息实体
         *
         * @param content 内容
         * @param uid     用户ID
         * @return 消息实体
         */
        public ChatMessage createChatMessage(String content, String uid, int msgType) {
                User currentUser = UserManager.getInstance().getCurrentUser();
                ChatMessage message = new ChatMessage();
                message.setConversationType(Constant.TYPE_CONVERSATION_PERSON);
                message.setBelongId(currentUser.getObjectId());
                message.setBelongUserName(currentUser.getUsername());
                message.setBelongNick(currentUser.getNick());
                message.setBelongAvatar(currentUser.getAvatar());
                message.setToId(uid);
                message.setConversationId(currentUser.getObjectId() + "&" + uid);
//                 默认设置消息发送成功
                message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                message.setReadStatus(Constant.READ_STATUS_UNREAD);
                message.setContent(content);
                message.setCreateTime(String.valueOf(System.currentTimeMillis()));
                message.setMsgType(msgType);
                message.setTag("");
                return message;
        }

        /**
         * 发送文件消息
         *
         * @param isPerson         是否是单聊
         * @param uid              目标ID
         * @param localFilePath    图片的本地路径  或者是语音的本地路径&长度  或者是地址内容：格式为:  显示图片的本地路径&纬度&经度&地址
         * @param sendFileListener 回调
         */
        private void sendFileMessage(boolean isPerson, final String uid, final String localFilePath, Integer messageType, final SendFileListener sendFileListener) {
//                明天这里开始
                BaseMessage message = createFileMessage(isPerson, uid, localFilePath, messageType);
                String localPath = null;
                if (messageType.equals(Constant.TAG_MSG_TYPE_IMAGE)) {
                        localPath = localFilePath;
                } else if (messageType.equals(Constant.TAG_MSG_TYPE_VOICE)) {
                        localPath = localFilePath.split("&")[0];
                } else if (messageType.equals(Constant.TAG_MSG_TYPE_LOCATION)) {
                        localPath = localFilePath.split(",")[0];
                }
                message.setSendStatus(Constant.SEND_STATUS_START);
                sendFileListener.onStart(message);
                if (!CommonUtils.isNetWorkAvailable(CustomApplication.getInstance())) {
//                        如果没有网络
                        message.setSendStatus(Constant.SEND_STATUS_FAILED);
                        saveAndUploadSendMessage(false, message);
                        sendFileListener.onFailed(new BmobException("没有网络，请检查网络配置"));
                        return;
                }
                realSendFileMessage(false, message, localPath, sendFileListener);
        }

        private BaseMessage createFileMessage(boolean isPerson, String uid, String localFilePath, Integer messageType) {
                if (isPerson) {
                        return createChatMessage(localFilePath, uid, messageType);
                } else {
                        return createGroupChatMessage(localFilePath, uid, messageType);
                }
        }

        /**
         * 真正发送图片消息
         *
         * @param isResend         是否是重发
         * @param message          图片消息
         * @param localPath        未见的本地路径
         * @param sendFileListener 回调
         */
        private void realSendFileMessage(final boolean isResend, final BaseMessage message, final String localPath, final SendFileListener sendFileListener) {
                File imageFile = new File(localPath);
//                MB单位
                final BmobFile bmobFile = new BmobFile(imageFile);
                bmobFile.uploadblock(CustomApplication.getInstance(), new UploadFileListener() {
                        @Override
                        public void onStart() {
                                super.onStart();
                                LogUtil.e("bmobFile.uploadblock：onStart");
                                message.setSendStatus(Constant.SEND_STATUS_SENDING);
                        }

                        @Override
                        public void onProgress(Integer value) {
                                super.onProgress(value);
                                sendFileListener.onProgress(value);
                        }

                        @Override
                        public void onFinish() {
                                super.onFinish();
                                LogUtil.e("bmobFile.uploadblock：onFinish");
                        }

                        @Override
                        public void onSuccess() {
                                LogUtil.e("上传文件成功");
                                String fileUrl = bmobFile.getFileUrl(CustomApplication.getInstance());
                                if (message.getMsgType().equals(Constant.TAG_MSG_TYPE_IMAGE)) {
                                        message.setContent(fileUrl);
                                } else if (message.getMsgType().equals(Constant.TAG_MSG_TYPE_VOICE)) {
//                                                                                     如果是语音则是   服务器上的短连接URL&语音时长length
                                        String length = message.getContent().split("&")[1];
                                        message.setContent(fileUrl + "&" + length);
                                } else {
//                                                                                     地址消息
                                        String[] content = message.getContent().split(",");
                                        message.setContent(fileUrl + "," + content[1] + "," + content[2] + "," + content[3]);
                                }
                                sendTextMessage(isResend, message,
                                        new OnSendMessageListener() {

                                                @Override
                                                public void onSending() {
                                                        LogUtil.e("地址文本消息发送中.........");
                                                }

                                                @Override
                                                public void onSuccess() {
                                                        message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                                                        LogUtil.e("发送成功");
//                                                                                        重新再更改数据库中的消息中的内容
//                                                                                        内容格式为:   本地地址&网络地址

                                                        if (message.getMsgType().equals(Constant.TAG_MSG_TYPE_IMAGE)) {
//                                                                                             message.setContent(localPath + "&" + message.getContent());
                                                                message.setContent(localPath + "&" + message.getContent());
                                                        } else if (message.getMsgType().equals(Constant.TAG_MSG_TYPE_VOICE)) {
//                                                                                                             保存到数据库中的格式:  本地地址&服务器URL&length
                                                                message.setContent(localPath + "&" + message.getContent());
                                                        } else {
//                                                                                                             地址消息 发送成功后，保存的定位地址为(localPath&shortUrl&latitude&longitude&address)
                                                                message.setContent(localPath + "," + message.getContent());
                                                        }
//                                                                                        本地数据库中保存的内容格式为：本地地址&网络地址
                                                        if (message instanceof ChatMessage) {
                                                                ChatDB.create().saveChatMessage((ChatMessage) message);
                                                        } else {
                                                                ChatDB.create().saveGroupChatMessage((GroupChatMessage) message);
                                                        }
                                                        sendFileListener.onSuccess();
                                                }

                                                @Override
                                                public void onFailed(BmobException e) {
                                                        message.setSendStatus(Constant.SEND_STATUS_FAILED);
                                                        LogUtil.e("发送失败");
//                                                                                                     发送失败后，要还原之前的内容格式，防止重发的时候发生冲突
                                                        if (message.getMsgType().equals(Constant.TAG_MSG_TYPE_VOICE)) {
                                                                message.setContent(localPath + "&" + message.getContent().split("&")[1]);
                                                        } else if (message.getMsgType().equals(Constant.TAG_MSG_TYPE_IMAGE)) {
//                                                                                                             还原图片消息
                                                                message.setContent(localPath);
                                                        } else {
//                                                                                                             还原地址消息
                                                                String[] content = message.getContent().split(",");
                                                                message.setContent(localPath + "," + content[1] + "," + content[2] + "," + content[3]);
                                                        }
                                                        sendFileListener.onFailed(e);
                                                }
                                        });
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                LogUtil.e("怎么回事?");
                                LogUtil.e("上传文件失败" + s + i);
                                message.setSendStatus(Constant.SEND_STATUS_FAILED);
                                if (!isResend) {
                                        saveAndUploadSendMessage(false, message);
                                }
                                sendFileListener.onFailed(new BmobException(s));

                        }
                });
        }

        /**
         * 重发图片消息
         * content为   当地图片的存储路径
         *
         * @param baseMessage      图片消息
         * @param sendFileListener 回调
         */

        public void resendImageChatMessage(BaseMessage baseMessage, SendFileListener sendFileListener) {
                baseMessage.setSendStatus(Constant.SEND_STATUS_START);
                sendFileListener.onStart(baseMessage);
                if (!CommonUtils.isNetWorkAvailable(CustomApplication.getInstance())) {
                        LogUtil.e("当前网络未连接，请检查网络配置");
                        baseMessage.setSendStatus(Constant.SEND_STATUS_FAILED);
                        sendFileListener.onFailed(new BmobException("当前网络未连接，请检查网络配置"));
                        return;
                }
                String path;
                if (baseMessage.getContent().contains("&")) {
                        path = baseMessage.getContent().split("&")[0];
                } else {
                        path = baseMessage.getContent();
                }
                realSendFileMessage(true, baseMessage, path, sendFileListener);
        }

        /**
         * 更新聊天消息的读取状态
         *
         * @param chatMessage 聊天消息实体
         * @param isReaded    是否已阅读
         * @return 结果
         */
        public long uploadAndUpdateChatMessageReadStatus(ChatMessage chatMessage, boolean isReaded) {
                updateMsgReaded(false, chatMessage.getConversationId(), chatMessage.getCreateTime());
                return ChatDB.create(chatMessage.getToId()).updateChatMessageReadStatus(chatMessage.getConversationId(), chatMessage.getCreateTime(), isReaded);
        }

        /**
         * 发送语音消息
         *
         * @param isPerson         是否是单聊
         * @param uid              目标ID
         * @param localPath        语音路径
         * @param recordTime       语音长度
         * @param sendFileListener 回调
         */
        public void sendVoiceMessage(boolean isPerson, String uid, String localPath, int recordTime, SendFileListener sendFileListener) {
                sendFileMessage(isPerson, uid, localPath + "&" + recordTime, Constant.TAG_MSG_TYPE_VOICE, sendFileListener);
        }

        /**
         * 发送图片消息
         *
         * @param isPerson         是否是单聊
         * @param uid              目标ID
         * @param localImagePath   图片路径
         * @param sendFileListener 回调
         */
        public void sendImageMessage(boolean isPerson, String uid, String localImagePath, SendFileListener sendFileListener) {
                sendFileMessage(isPerson, uid, localImagePath, Constant.TAG_MSG_TYPE_IMAGE, sendFileListener);
        }

        /**
         * 重新发送语音消息
         * content 格式为   当地的语音存储路径&长度
         *
         * @param baseMessage      消息实体
         * @param sendFileListener 回调
         */
        public void resendVoiceChatMessage(BaseMessage baseMessage, SendFileListener sendFileListener) {
                baseMessage.setSendStatus(Constant.SEND_STATUS_START);
                baseMessage.setReadStatus(Constant.READ_STATUS_UNREAD);
                sendFileListener.onStart(baseMessage);
                if (!CommonUtils.isNetWorkAvailable(CustomApplication.getInstance())) {
                        LogUtil.e("当前网络未连接，请检查网络配置");
                        baseMessage.setSendStatus(Constant.SEND_STATUS_FAILED);
                        sendFileListener.onFailed(new BmobException("当前网络未连接，请检查网络配置"));
                        return;
                }
                String localPath = baseMessage.getContent().split("&")[0];
                realSendFileMessage(true, baseMessage, localPath, sendFileListener);
        }

        /**
         * @param isPerson 是否是单聊
         * @param uid      用户ID
         * @param content  消息内容
         * @param listener 回调
         */
        public void sendLocationMessage(boolean isPerson, String uid, String content, SendFileListener listener) {
                sendFileMessage(isPerson, uid, content, Constant.TAG_MSG_TYPE_LOCATION, listener);
        }


        /**
         * 重新发送地址消息
         *
         * @param baseMessage      消息实体
         * @param sendFileListener 回调
         */
        public void resendLocationChatMessage(BaseMessage baseMessage, SendFileListener sendFileListener) {
                baseMessage.setSendStatus(Constant.SEND_STATUS_START);
                baseMessage.setReadStatus(Constant.READ_STATUS_UNREAD);
                sendFileListener.onStart(baseMessage);
                if (!CommonUtils.isNetWorkAvailable(CustomApplication.getInstance())) {
                        LogUtil.e("当前网络未连接，请检查网络配置");
                        baseMessage.setSendStatus(Constant.SEND_STATUS_FAILED);
                        sendFileListener.onFailed(new BmobException("当前网络未连接，请检查网络配置"));
                        return;
                }
                realSendFileMessage(true, baseMessage, baseMessage.getContent().split(",")[0], sendFileListener);
        }

        /**
         * 给其他设备发送下线通知(除了本设备)
         *
         * @param customInstallation 设备列表
         * @param listener           推送监听
         */
        void sendOfflineNotificationMsg(CustomInstallation customInstallation, PushListener listener) {
                JSONObject offlineJsonObject = createOfflineJsonObject();
                sendJsonMessage(customInstallation.getInstallationId(), offlineJsonObject, listener);
        }

        /**
         * 创建下载通知的jsonObject
         *
         * @return 下载通知的jsonObject
         */
        private JSONObject createOfflineJsonObject() {
                try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(Constant.MESSAGE_TAG, Constant.TAG_OFFLINE);
                        jsonObject.put(Constant.TAG_BELONG_ID, UserManager.getInstance().getCurrentUserObjectId());
                        return jsonObject;
                } catch (JSONException e) {
                        e.printStackTrace();
                        return null;
                }
        }

        /**
         * 推送消息
         *
         * @param installationId 推送的设备ID
         * @param jsonObject     推送的jsonObject
         */
        private void sendJsonMessage(String installationId, JSONObject jsonObject, PushListener listener) {
                BmobQuery<CustomInstallation> query = new BmobQuery<>();
                query.addWhereEqualTo("installationId", installationId);
                mPushManager.setQuery(query);
                mPushManager.pushMessage(jsonObject, listener);
        }

        /**
         * 发送建群消息
         * <p>
         * 主要流程：
         * 1、构建群结构消息，保存到服务器上
         * 2、从服务器上获取该消息，并把该消息的id设置为群id,即groupId
         * 3、推送群结构消息到每个 成员中
         * 4、为每个成员在服务器上保存群结构消息，是为了防止推送失败时，对方不能获取到服务器上的群结构消息，也就不能定时拉取到群数据
         * 5、群主发送群欢迎消息给每个成员
         *
         * @param groupName        群组姓名
         * @param groupDescription 群组描述
         * @param contacts         群组成员(还没包括群主)
         */
        public void sendCreateGroupMessage(String groupName, final String groupDescription, final List<String> contacts, final SaveListener listener) {
                LogUtil.e("2221111111发送建群消息中...............");
//                这里要把群主消息加进来
                contacts.add(0, UserManager.getInstance().getCurrentUser().getObjectId());
                GroupTableMessage message = createGroupTableMessage(groupName, groupDescription, contacts);
                message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                message.setReadStatus(Constant.READ_STATUS_READED);
                final String time = String.valueOf(System.currentTimeMillis());
                message.setCreatedTime(time);
                LogUtil.e("toId:" + message.getToId());
                LogUtil.e("time:" + message.getCreatedTime());
                message.save(CustomApplication.getInstance(), new SaveListener() {
                                @Override
                                public void onSuccess() {
                                        LogUtil.e("11111222333保存群主所建的群结构消息到服务器上成功");
                                        LogUtil.e("1toId:" + UserManager.getInstance().getCurrentUserObjectId());
                                        LogUtil.e("1time:" + time);
                                        BmobQuery<GroupTableMessage> bmobQuery = new BmobQuery<>();
                                        bmobQuery.addWhereEqualTo("toId", UserManager.getInstance().getCurrentUserObjectId());
//                                        不知道干嘛，设置createdTime,竟然查询不到，应该是个bug
//                                        bmobQuery.addWhereEqualTo("createdTime", time);
                                        bmobQuery.addWhereEqualTo("groupDescription", groupDescription);
                                        bmobQuery.findObjects(CustomApplication.getInstance(), new FindListener<GroupTableMessage>() {
                                                @Override
                                                public void onSuccess(List<GroupTableMessage> list) {

                                                        if (list != null && list.size() > 0) {
                                                                final GroupTableMessage groupTableMessage = list.get(0);
                                                                groupTableMessage.setGroupId(groupTableMessage.getObjectId());
//                                                                设置未读取状态，方便定时拉取到
                                                                groupTableMessage.setReadStatus(Constant.READ_STATUS_UNREAD);
                                                                groupTableMessage.update(CustomApplication.getInstance(), new UpdateListener() {
                                                                        @Override
                                                                        public void onSuccess() {
                                                                                ChatDB.create().saveGroupTableMessage(groupTableMessage);
                                                                                MessageCacheManager.getInstance().addGroupTableMessage(groupTableMessage);
//                                                                                这里先上传再推送，因为对方可能接收到推送信息的时候无法即时获取到服务器上的群结构消息,也就查询信息失败
                                                                                uploadChatTableMessage(groupTableMessage, new SaveListener() {
                                                                                        @Override
                                                                                        public void onSuccess() {
                                                                                                LogUtil.e("批量保存群结构消息成功");
                                                                                                sendGroupChatMessage("大家好", groupTableMessage.getGroupId(), Constant.TAG_MSG_TYPE_TEXT, new OnSendMessageListener() {
                                                                                                        @Override
                                                                                                        public void onSending() {

                                                                                                        }

                                                                                                        @Override
                                                                                                        public void onSuccess() {
                                                                                                                LogUtil.e("发送群欢迎消息成功");
                                                                                                                listener.onSuccess();
                                                                                                        }

                                                                                                        @Override
                                                                                                        public void onFailed(BmobException e) {
                                                                                                                LogUtil.e("发送群欢迎消息失败");
                                                                                                                listener.onFailure(e.getErrorCode(), e.getMessage());

                                                                                                        }
                                                                                                });
                                                                                        }

                                                                                        @Override
                                                                                        public void onFailure(int i, String s) {
                                                                                                LogUtil.e("批量保存群结构消息失败" + s + i);
                                                                                                listener.onFailure(i, s);

                                                                                        }
                                                                                });
                                                                        }

                                                                        @Override
                                                                        public void onFailure(int i, String s) {
                                                                                LogUtil.e("更新群主的群结构消息失败" + s + i);
                                                                                listener.onFailure(i, s);
                                                                        }
                                                                });
                                                        } else {
                                                                LogUtil.e("查询不到群结构消息，创建群结构表失败");
                                                                listener.onFailure(0, "查询不到群结构消息，创建群结构表失败");
                                                        }
                                                }

                                                @Override
                                                public void onError(int i, String s) {
                                                        LogUtil.e("在服务器上保存群结构消息失败,创建群结构表失败" + s + i);
                                                        listener.onFailure(i, s);
                                                }
                                        });
                                }

                                @Override
                                public void onFailure(int i, String s) {
                                        LogUtil.e("保存群主所建的群结构消息到服务器上失败" + s + i);
                                        listener.onFailure(i, s);
                                }
                        }
                );
        }

        private void getUserInstallationId(String toId, final FindListener<CustomInstallation> findListener) {
                BmobQuery<CustomInstallation> query = new BmobQuery<>();
                query.addWhereEqualTo("uid", toId);
                query.findObjects(CustomApplication.getInstance(), findListener);
        }


        private void uploadChatTableMessage(final GroupTableMessage groupTableMessage, SaveListener listener) {
                List<String> groupNumber = groupTableMessage.getGroupNumber();
                GroupTableMessage message;
                List<String> copy = new ArrayList<>(groupNumber);
                if (copy.contains(UserManager.getInstance().getCurrentUserObjectId())) {
                        copy.remove(UserManager.getInstance().getCurrentUserObjectId());
                }
                List<BmobObject> groupTableMessageList = new ArrayList<>();
                for (int i = 0; i < copy.size(); i++) {
                        message = new GroupTableMessage();
                        message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                        message.setReadStatus(Constant.READ_STATUS_UNREAD);
                        message.setGroupDescription(groupTableMessage.getGroupDescription());
                        message.setGroupId(groupTableMessage.getGroupId());
                        message.setCreatedTime(groupTableMessage.getCreatedTime());
                        message.setToId(copy.get(i));
                        message.setGroupNumber(groupNumber);
                        message.setGroupAvatar(groupTableMessage.getGroupAvatar());
                        message.setGroupName(groupTableMessage.getGroupName());
                        message.setGroupNick(groupTableMessage.getGroupNick());
                        message.setNotification(groupTableMessage.getNotification());
                        message.setCreatorId(groupTableMessage.getCreatorId());
                        groupTableMessageList.add(message);
                }
                new BmobObject().insertBatch(CustomApplication.getInstance(), groupTableMessageList, listener);
        }

        private GroupTableMessage createGroupTableMessage(String groupName, String groupDescription, List<String> contacts) {
                User currentUser = UserManager.getInstance().getCurrentUser();
                GroupTableMessage message = new GroupTableMessage();
                message.setGroupName(groupName);
                message.setGroupDescription(groupDescription);
                message.setGroupAvatar(currentUser.getAvatar() == null ? "" : currentUser.getAvatar());
                message.setCreatorId(UserManager.getInstance().getCurrentUserObjectId());
                message.setGroupNumber(contacts);
                message.setReadStatus(Constant.READ_STATUS_UNREAD);
                message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                message.setToId(UserManager.getInstance().getCurrentUserObjectId());
                message.setGroupNick("");
                message.setNotification("");
                return message;
        }

        private void sendGroupChatMessage(String content, String groupId, int msgType, final OnSendMessageListener listener) {
//                这里先不通过组播推送消息，因为在对方用户还没有接受建群的消息的时候(也就意味着没订阅该群消息)，发送欢迎消息会接收不到的
                sendTextMessage(false, createGroupChatMessage(content, groupId, msgType), new OnSendMessageListener() {
                        @Override
                        public void onSending() {
                                LogUtil.e("发送建群欢迎消息中.............");

                        }

                        @Override
                        public void onSuccess() {
                                LogUtil.e("发送建群欢迎消息成功:这里是推送和上传成功后的回调");
                                listener.onSuccess();
                        }

                        @Override
                        public void onFailed(BmobException e) {
                                LogUtil.e("发送建群欢迎消息失败" + e.getMessage() + e.getErrorCode());
                                listener.onFailed(e);
                        }
                });
        }

        public void queryGroupTableMessage(String uid, FindListener<GroupTableMessage> findListener) {
                BmobQuery<GroupTableMessage> query = new BmobQuery<>();
                query.addWhereEqualTo(Constant.TAG_TO_ID, uid);
                query.findObjects(CustomApplication.getInstance(), findListener);
        }

        public GroupChatMessage createGroupChatMessage(String content, String groupId, int msgType) {
                User user = UserManager.getInstance().getCurrentUser();
                GroupChatMessage message = new GroupChatMessage();
                message.setGroupId(groupId);
                message.setContent(content);
                message.setReadStatus(Constant.READ_STATUS_UNREAD);
                message.setMsgType(msgType);
                message.setBelongAvatar(user.getAvatar() == null ? "" : user.getAvatar());
                message.setBelongId(user.getObjectId());
                GroupTableMessage groupTableMessage = MessageCacheManager.getInstance().getGroupTableMessage(groupId);
                message.setBelongNick(groupTableMessage.getGroupNick() == null || groupTableMessage.getGroupNick().equals("") ? user.getNick() : groupTableMessage.getGroupNick());
                message.setBelongUserName(user.getUsername());
                message.setConversationType(Constant.TYPE_CONVERSATION_GROUP);
                message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                message.setCreateTime(String.valueOf(System.currentTimeMillis()));
                return message;
        }


        public void updateGroupTableMessage(GroupTableMessage groupTableMessage) {
                LogUtil.e("这里开始同步更新群结构消息");
                groupTableMessage.update(CustomApplication.getInstance(), new UpdateListener() {
                        @Override
                        public void onSuccess() {
                                LogUtil.e("同步更新服务器群结构消息成功");
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                LogUtil.e("同步更新服务器群结构消息失败" + s + i);

                        }
                });
        }



        public void updateGroupTableMessageReaded(GroupTableMessage message, UpdateListener listener) {
                message.update(CustomApplication.getInstance(), listener);
        }


        public GroupTableMessage createReceiveGroupTableMsg(String json) {
//                GroupTableMessage message = new GroupTableMessage();
                Gson mGson=new Gson();
                GroupTableMessage message=mGson.fromJson(json,GroupTableMessage.class);
                LogUtil.e("实时监听到的群结构消息如下1");
                if (message != null) {
                        LogUtil.e(message);
                }
                return message;
//                mGson.fromJson(jsonObject.toString())
//                message.setReadStatus(JsonUtil.getInt(jsonObject, Constant.TAG_MESSAGE_READ_STATUS));
//                message.setSendStatus(JsonUtil.getInt(jsonObject, Constant.TAG_MESSAGE_SEND_STATUS));
//                message.setGroupName(JsonUtil.getString(jsonObject, Constant.GROUP_NAME));
//                message.setGroupNick(JsonUtil.getString(jsonObject, Constant.GROUP_NICK));
//                message.setGroupAvatar(JsonUtil.getString(jsonObject, Constant.GROUP_AVATAR));
//                message.setCreatedTime(JsonUtil.getString(jsonObject, Constant.GROUP_TIME));
//                message.setCreatorId(JsonUtil.getString(jsonObject, Constant.GROUP_CREATOR_ID));
//                message.setGroupDescription(JsonUtil.getString(jsonObject, Constant.GROUP_DESCRIPTION));
//                message.setGroupId(JsonUtil.getString(jsonObject, Constant.GROUP_ID));
//                message.setNotification(JsonUtil.getString(jsonObject, Constant.GROUP_NOTIFICATION));
//                message.setObjectId(JsonUtil.getString(jsonObject, Constant.ID));
//                message.setToId(JsonUtil.getString(jsonObject, Constant.TAG_TO_ID));
//                message.setGroupNumber(CommonUtils.string2list(JsonUtil.getString(jsonObject, Constant.GROUP_NUMBER)));
        }

        public void sendShareMessage(final SharedMessage shareMessage, final AddShareMessageCallBack listener) {
                final String time = shareMessage.getCreateTime();
                shareMessage.save(CustomApplication.getInstance(), new SaveListener() {
                        @Override
                        public void onSuccess() {
                                BmobQuery<SharedMessage> query = new BmobQuery<>();
                                query.addWhereEqualTo("createTime", time);
                                query.addWhereEqualTo("belongId", UserManager.getInstance().getCurrentUserObjectId());
                                query.findObjects(CustomApplication.getInstance(), new FindListener<SharedMessage>() {
                                        @Override
                                        public void onSuccess(List<SharedMessage> list) {
                                                if (list != null && list.size() > 0) {
                                                        ChatDB.create().saveSharedMessage(list.get(0));
                                                        listener.onSuccess(list.get(0));
                                                } else {
                                                        LogUtil.e("查询不到刚刚保存的说说消息");
                                                        listener.onFailed(0, "查询不到刚刚保存的说说消息");
                                                }
                                        }

                                        @Override
                                        public void onError(int i, String s) {
                                                LogUtil.e("查询刚刚保存的说说消息失败" + s + i);
                                                listener.onFailed(i, s);

                                        }
                                });

                        }

                        @Override
                        public void onFailure(int i, String s) {
                                LogUtil.e("保存说说消息到服务器上失败" + s + i);
                                listener.onFailed(i, s);
                        }
                });
        }


        public void addLiker(final String id, final DealMessageCallBack dealMessageCallBack) {
                final SharedMessage sharedMessage = ChatDB.create().getSharedMessage(id);
                sharedMessage.getLikerList().add(UserManager.getInstance().getCurrentUserObjectId());
                sharedMessage.update(CustomApplication.getInstance(), new UpdateListener() {
                        @Override
                        public void onSuccess() {
                                ChatDB.create().saveSharedMessage(sharedMessage);
                                dealMessageCallBack.onSuccess(sharedMessage.getObjectId());
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                dealMessageCallBack.onFailed(sharedMessage.getObjectId(), i, s);
                        }
                });
        }


        public void deleteShareMessage(final String id, final DealMessageCallBack dealMessageCallBack) {
                final SharedMessage sharedMessage = ChatDB.create().getSharedMessage(id);
                LogUtil.e("删除前的说说消息格式");
                LogUtil.e(sharedMessage);
                if (sharedMessage.getMsgType().equals(Constant.MSG_TYPE_SHARE_MESSAGE_IMAGE) || sharedMessage.getMsgType().equals(Constant.MSG_TYPE_SHARE_MESSAGE_VIDEO)) {
                        BmobFile.deleteBatch(CustomApplication.getInstance(), sharedMessage.getImageList().toArray(new String[]{}), new DeleteBatchListener() {
                                @Override
                                public void done(String[] strings, BmobException e) {
                                        LogUtil.e("删除的所有文件");
                                        if (e != null) {
                                                LogUtil.e("所有删除失败文件的URL");
                                                if (strings != null) {
                                                        for (String string : strings) {
                                                                LogUtil.e(string);
                                                        }
                                                }
                                                LogUtil.e("文件删除失败" + e.getMessage() + e.getErrorCode());
                                                dealMessageCallBack.onFailed(id, e.getErrorCode(), e.getMessage());
                                        } else {
                                                LogUtil.e("全部文件删除成功");
                                                sharedMessage.delete(CustomApplication.getInstance(), new DeleteListener() {
                                                        @Override
                                                        public void onSuccess() {
                                                                LogUtil.e("在服务器上删除说说消息成功");
                                                                ChatDB.create().deleteSharedMessage(id);
                                                                dealMessageCallBack.onSuccess(id);
                                                        }

                                                        @Override
                                                        public void onFailure(int i, String s) {
                                                                LogUtil.e("在服务器上删除说说消息失败" + s + i);
                                                                dealMessageCallBack.onFailed(id, i, s);
                                                        }
                                                });
                                        }
                                }
                        });
                } else {
                        sharedMessage.delete(CustomApplication.getInstance(), new DeleteListener() {
                                @Override
                                public void onSuccess() {
                                        LogUtil.e("在服务器上删除说说消息成功1");
                                        ChatDB.create().deleteSharedMessage(id);
                                        dealMessageCallBack.onSuccess(id);
                                }

                                @Override
                                public void onFailure(int i, String s) {
                                        LogUtil.e("在服务器上删除说说消息失败" + s + i);
                                        dealMessageCallBack.onFailed(id, i, s);
                                }
                        });
                }
        }


        public void deleteLiker(final String id, final DealMessageCallBack dealMessageCallBack) {
                final SharedMessage sharedMessage = ChatDB.create().getSharedMessage(id);
                if (sharedMessage.getLikerList().contains(UserManager.getInstance().getCurrentUserObjectId())) {
                        sharedMessage.getLikerList().remove(UserManager.getInstance().getCurrentUserObjectId());
                        LogUtil.e("删除点赞消息成功");
                }
                sharedMessage.update(CustomApplication.getInstance(), new UpdateListener() {
                        @Override
                        public void onSuccess() {
                                LogUtil.e("更新服务器上删除点赞消息成功");
                                ChatDB.create().saveSharedMessage(sharedMessage);
                                dealMessageCallBack.onSuccess(sharedMessage.getObjectId());
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                LogUtil.e("更新服务器上删除点赞消息失败");
                                dealMessageCallBack.onFailed(sharedMessage.getObjectId(), i, s);
                        }
                });
        }

        public void addComment(final String id, final String content, final DealCommentMsgCallBack dealCommentMsgCallBack) {
                final SharedMessage sharedMessage = ChatDB.create().getSharedMessage(id);
                LogUtil.e("11之前的评论内容为:");
                for (int i = 0; i < sharedMessage.getCommentMsgList().size(); i++) {
                        LogUtil.e(sharedMessage.getCommentMsgList().get(i));
                }
                LogUtil.e("评论的内容:" + content);
                sharedMessage.getCommentMsgList().add(content);
                LogUtil.e("现在的评论内容为:");
                for (int i = 0; i < sharedMessage.getCommentMsgList().size(); i++) {
                        LogUtil.e(sharedMessage.getCommentMsgList().get(i));
                }
                sharedMessage.update(CustomApplication.getInstance(), new UpdateListener() {
                        @Override
                        public void onSuccess() {
                                LogUtil.e("1在服务器上更新添加评论消息成功");
                                ChatDB.create().saveSharedMessage(sharedMessage);
                                dealCommentMsgCallBack.onSuccess(id, content, sharedMessage.getCommentMsgList().size() - 1);
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                LogUtil.e("在服务器上更新添加评论消息失败" + s + i);
                                dealCommentMsgCallBack.onFailed(sharedMessage.getObjectId(), content, sharedMessage.getCommentMsgList().size() - 1, i, s);
                        }
                });
        }


        public void deleteComment(final String id, final int position, final DealCommentMsgCallBack dealCommentMsgCallBack) {
//                这里先进行说说主题的解绑操作
                final SharedMessage sharedMessage = ChatDB.create().getSharedMessage(id);
                if (sharedMessage.getCommentMsgList().size() > position) {
                        final String commentMsg = sharedMessage.getCommentMsgList().remove(position);
                        LogUtil.e("11将要删除的评论消息:" + commentMsg);
                        sharedMessage.update(CustomApplication.getInstance(), new UpdateListener() {
                                @Override
                                public void onSuccess() {
                                        LogUtil.e("在服务器上更新删除评论成功");
                                        ChatDB.create().saveSharedMessage(sharedMessage);
                                        dealCommentMsgCallBack.onSuccess(sharedMessage.getObjectId(), commentMsg, position);
                                }

                                @Override
                                public void onFailure(int i, String s) {
                                        LogUtil.e("在服务器上更新删除评论失败" + s + i);
                                        dealCommentMsgCallBack.onFailed(sharedMessage.getObjectId(), commentMsg, position, i, s);
                                }
                        });
                }
        }


        public void loadAllShareMessages(boolean isPullRefresh, String time, final LoadShareMessageCallBack loadShareMessageCallBack) {
                loadShareMessages(true, null, isPullRefresh, time, loadShareMessageCallBack);
        }

        public void loadShareMessages(boolean isAll, String uid, boolean isPullRefresh, String time, final LoadShareMessageCallBack loadShareMessageCallBack) {
                try {
                        BmobQuery<SharedMessage> query = new BmobQuery<>();
                        if (isAll) {
                                if (UserCacheManager.getInstance().getContacts() != null && UserCacheManager.getInstance().getContacts().size() > 0) {
                                        List<String> list = new ArrayList<>(UserCacheManager.getInstance().getContacts().keySet());
                                        list.add(UserManager.getInstance().getCurrentUserObjectId());
                                        list.add(UserManager.getInstance().getCurrentUser().getObjectId());
                                        query.addWhereContainedIn("belongId", list);
                                } else {
                                        query.addWhereEqualTo("belongId", UserManager.getInstance().getCurrentUser().getObjectId());
                                }
                        } else {
                                query.addWhereEqualTo("belongId", uid);
                        }
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        long currentTime = simpleDateFormat.parse(time).getTime();
                        LogUtil.e("现在的时间:" + currentTime);
                        BmobDate bmobDate;
                        if (isPullRefresh) {
                                currentTime += 1000;
                                LogUtil.e("加一秒后的时间" + currentTime);
                                bmobDate = new BmobDate(new Date(currentTime));
                                query.addWhereGreaterThan("createdAt", bmobDate);
                        } else {
                                currentTime -= 1000;
                                LogUtil.e("减一秒后的时间" + currentTime);
                                bmobDate = new BmobDate(new Date(currentTime));
                                query.addWhereLessThan("createdAt", bmobDate);
                        }
                        query.order("-createdAt");
                        query.setLimit(10);
                        query.setCachePolicy(BmobQuery.CachePolicy.NETWORK_ONLY);
                        query.findObjects(CustomApplication.getInstance(), new FindListener<SharedMessage>() {
                                @Override
                                public void onSuccess(List<SharedMessage> list) {
                                        LogUtil.e("11从服务器上加载得到的最新的不多于10条的说说消息为：");
                                        if (list != null && list.size() > 0) {
                                                List<SharedMessage> result = new ArrayList<>(list);
                                                for (SharedMessage message :
                                                        list) {
                                                        LogUtil.e(message);
                                                        if (message.getBelongId().equals(UserManager.getInstance().getCurrentUserObjectId())) {
                                                                continue;
                                                        }
                                                        if (message.getVisibleType().equals(Constant.SHARE_MESSAGE_VISIBLE_TYPE_PRIVATE)) {
                                                                result.remove(message);
                                                        } else {
                                                                if (message.getInVisibleUserList().contains(UserManager.getInstance().getCurrentUserObjectId())) {
                                                                        result.remove(message);
                                                                }
                                                        }
                                                }
                                                LogUtil.e("筛选可见后的说说列表");
                                                for (SharedMessage message :
                                                        result) {
                                                        LogUtil.e(message);
                                                }
                                                if (result.size() > 0) {
                                                        loadShareMessageCallBack.onSuccess(result);
                                                } else {
                                                        loadShareMessageCallBack.onSuccess(null);
                                                }
                                        } else {
                                                loadShareMessageCallBack.onSuccess(null);
                                        }
                                }

                                @Override
                                public void onError(int i, String s) {
                                        LogUtil.e("在服务器上查找说说消息失败"+s+i);
                                        loadShareMessageCallBack.onFailed(s, i);
                                }
                        });
                } catch (ParseException e) {
                        e.printStackTrace();
                        LogUtil.e("刷新查询说说消息时解析时间失败" + e.getMessage());
                }
        }


//        public void createAndUploadGroupTableMessage(GroupTableMessage message, OnReceiveGroupTableListener listener) {
//                updateGroupTableMessageReaded(message.getGroupId(), UserCacheManager.getInstance().getUser().getObjectId(), listener);
//        }


        public void updateUserInstallationId(UpdateListener listener) {
                User currentUser = UserManager.getInstance().getCurrentUser();
                currentUser.setInstallId(CustomInstallation.getInstallationId(CustomApplication.getInstance()));
                currentUser.update(CustomApplication.getInstance(), listener);
        }

        public User createUserFromJsonObject(JSONObject jsonObject) {
                User user;
                user = new Gson().fromJson(jsonObject.toString(), User.class);
                LogUtil.e("实时监听到的user");
                LogUtil.e(user);
//                user.setObjectId(JsonUtil.getString(jsonObject, "objectId"));
//                user.setAvatar(JsonUtil.getString(jsonObject, "avatar"));
//                user.setNick(JsonUtil.getString(jsonObject, "nick"));
//                user.setUsername(JsonUtil.getString(jsonObject, "username"));
//                user.setSortedKey(JsonUtil.getString(jsonObject, "sortedKey"));
//                user.setInstallId(JsonUtil.getString(jsonObject, "installId"));
//                user.setSex(JsonUtil.getBoolean(jsonObject, "sex", false));
//                user.setSignature(JsonUtil.getBoolean());
                LogUtil.e("groupId：" + JsonUtil.getString(jsonObject, Constant.GROUP_ID) + "\n");
                LogUtil.e("fromId：" + JsonUtil.getString(jsonObject, Constant.TAG_BELONG_ID + "\n"));
                LogUtil.e("fromAvatar：" + JsonUtil.getString(jsonObject, Constant.TAG_BELONG_AVATAR) + "\n");
                LogUtil.e("fromName：" + JsonUtil.getString(jsonObject, Constant.TAG_BELONG_NAME) + "\n");
                LogUtil.e("fromNick" + JsonUtil.getString(jsonObject, Constant.TAG_BELONG_NICK) + "\n");
                LogUtil.e("sendStatus" + JsonUtil.getInt(jsonObject, Constant.TAG_MESSAGE_SEND_STATUS) + "\n");
                LogUtil.e("readStatus" + JsonUtil.getInt(jsonObject, Constant.TAG_MESSAGE_READ_STATUS) + "\n");
                LogUtil.e("msgType" + JsonUtil.getInt(jsonObject, Constant.MESSAGE_TAG) + "\n");
                LogUtil.e("createTime" + JsonUtil.getString(jsonObject, Constant.TAG_CREATE_TIME) + "\n");
                return user;
        }

        public void updateReadTagMsgReaded(String conversationId, String createTime) {
                findReadTag(conversationId, createTime, new FindListener<ChatMessage>() {
                        @Override
                        public void onSuccess(List<ChatMessage> list) {
                                if (list != null && list.size() > 0) {
                                        ChatMessage message = list.get(0);
                                        message.setReadStatus(Constant.READ_STATUS_READED);
                                        message.update(CustomApplication.getInstance(), new UpdateListener() {
                                                @Override
                                                public void onSuccess() {
                                                        LogUtil.e("在服务器上更新回执已读消息为已读成功");
                                                }

                                                @Override
                                                public void onFailure(int i, String s) {
                                                        LogUtil.e("在服务器上更新回执已读消息为已读失败" + s + i);
                                                }
                                        });
                                } else {
                                        LogUtil.e("在服务器上没有查询到回执读取消息");
                                }
                        }

                        @Override
                        public void onError(int i, String s) {
                                LogUtil.e("在服务器上查询回执已读消息失败" + s + i);
                        }
                });
        }


        private void findReadTag(String conversationId, String createTime, FindListener<ChatMessage> findListener) {
                BmobQuery<ChatMessage> query = new BmobQuery<>();
                query.addWhereEqualTo("conversationId", conversationId);
                query.addWhereEqualTo("createTime", createTime);
                query.addWhereEqualTo("tag", Constant.TAG_ASK_READ);
                query.findObjects(CustomApplication.getInstance(), findListener);
        }

        public void createSharedMessage(HappyBean happyBean, HappyContentBean happyContentBean,
                                        WinXinBean winXinBean, PictureBean pictureBean, String location, String videoPath, String displayPath, String content, final List<ImageItem> imageList, List<String> selectedInVisibleUsers, int visibleType, final OnCreateSharedMessageListener listener) {
                final SharedMessage sharedMessage = new SharedMessage();
                if (!location.equals("不显示")) {
                        sharedMessage.setAddress(location);
                }
                if (visibleType == Constant.SHARE_MESSAGE_VISIBLE_TYPE_PUBLIC) {
                        sharedMessage.setInVisibleUserList(selectedInVisibleUsers);
                }
                sharedMessage.setVisibleType(visibleType);
                sharedMessage.setBelongId(UserManager.getInstance().getCurrentUser().getObjectId());
                sharedMessage.setCreateTime(String.valueOf(System.currentTimeMillis()));
                sharedMessage.setContent(content);
                if (happyBean != null || happyContentBean != null || winXinBean != null||pictureBean!=null) {
                        sharedMessage.setMsgType(Constant.MSG_TYPE_SHARE_MESSAGE_LINK);
                        if (happyBean != null) {
                                List<String> urlList = new ArrayList<>();
                                urlList.add(happyBean.getUrl());
                                sharedMessage.setUrlList(urlList);
                                sharedMessage.setUrlTitle(happyBean.getContent());
                        } else if (happyContentBean != null) {
                                sharedMessage.setUrlTitle(happyContentBean.getContent());
                        } else if (pictureBean != null) {
                                List<String> urlList=new ArrayList<>();
                                urlList.add(pictureBean.getUrl());
                                sharedMessage.setUrlList(urlList);
                        }else {
                                List<String> list = new ArrayList<>();
                                list.add(winXinBean.getPicUrl());
                                list.add(winXinBean.getUrl());
                                sharedMessage.setUrlTitle(winXinBean.getTitle());
                                sharedMessage.setUrlList(list);
                        }
                        listener.onSuccess(sharedMessage);
                        return;
                }
//                上面是分享的链接部分
                sharedMessage.setContent(content);
                final List<String> photoUrls = new ArrayList<>();
                if (imageList != null && imageList.size() > 0) {
                        sharedMessage.setMsgType(Constant.MSG_TYPE_SHARE_MESSAGE_IMAGE);
                        LogUtil.e("发送的全部path为:");
                        for (ImageItem imageItem :
                                imageList) {
                                photoUrls.add(imageItem.getPath());
                                LogUtil.e(imageItem.getPath());
                        }
                        BmobFile.uploadBatch(CustomApplication.getInstance(), photoUrls.toArray(new String[]{}), new UploadBatchListener() {
                                @Override
                                public void onSuccess(List<BmobFile> list2, List<String> list1) {
                                        if (imageList.size() == list1.size()) {
                                                LogUtil.e("11全部上传图片成功");
                                                sharedMessage.setImageList(list1);
                                                LogUtil.e("上传得到的全部URL");
                                                for (String url :
                                                        sharedMessage.getImageList()) {
                                                        LogUtil.e(url);
                                                }
                                                listener.onSuccess(sharedMessage);
                                        } else {
                                                LogUtil.e("目前得到的URL集合为:");
                                                for (String url :
                                                        list1) {
                                                        LogUtil.e(url);
                                                }
                                        }
                                }


                                @Override
                                public void onProgress(int i, int i1, int i2, int i3) {

                                }

                                @Override
                                public void onError(int i, String s) {
                                        listener.onFailed(0, "上传图片失败" + s + i);
                                }
                        });
                } else if (videoPath != null && displayPath != null) {
                        sharedMessage.setMsgType(Constant.MSG_TYPE_SHARE_MESSAGE_VIDEO);
                        LogUtil.e("视频说说消息");
                        final List<String> urlList = new ArrayList<>();
                        urlList.add(displayPath);
                        urlList.add(videoPath);
                        BmobFile.uploadBatch(CustomApplication.getInstance(), urlList.toArray(new String[]{}), new UploadBatchListener() {
                                @Override
                                public void onSuccess(List<BmobFile> list, List<String> list1) {
                                        if (urlList.size() == list1.size()) {
                                                LogUtil.e("视频全部上传成功");
                                                sharedMessage.setImageList(list1);
                                                LogUtil.e("上传得到的全部URL");
                                                for (String url :
                                                        sharedMessage.getImageList()) {
                                                        LogUtil.e(url);
                                                }
                                                listener.onSuccess(sharedMessage);
                                        }
                                }

                                @Override
                                public void onProgress(int i, int i1, int i2, int i3) {

                                }

                                @Override
                                public void onError(int i, String s) {
                                        LogUtil.e("上传视频失败" + s + i);
                                        listener.onFailed(i, s);
                                }
                        });
                } else {
                        LogUtil.e("文本说说消息");
                        sharedMessage.setMsgType(Constant.MSG_TYPE_SHARE_MESSAGE_TEXT);
                        listener.onSuccess(sharedMessage);
                }
        }

        public SharedMessage createSharedMessageFromJson(JSONObject object) {
                try {
                        SharedMessage sharedMessage = new SharedMessage();
                        sharedMessage.setBelongId(JsonUtil.getString(object, "belongId"));
                        sharedMessage.setContent(JsonUtil.getString(object, "content"));
                        sharedMessage.setCreateTime(JsonUtil.getString(object, "createTime"));
                        sharedMessage.setSeverCreateTime(JsonUtil.getString(object, "createdAt"));
                        sharedMessage.setMsgType(JsonUtil.getInt(object, "msgType"));
                        sharedMessage.setObjectId(JsonUtil.getString(object, "objectId"));
                        sharedMessage.setVisibleType(JsonUtil.getInt(object, "visibleType"));
                        sharedMessage.setAddress(JsonUtil.getString(object, "address"));
                        if (object.has("likerList")) {
                                JSONArray jsonArray = object.getJSONArray("likerList");
                                if (jsonArray.length() > 0) {
                                        List<String> list = new ArrayList<>();
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                                String uid = jsonArray.getString(i);
                                                LogUtil.e("点赞的用户ID：" + uid);
                                                list.add(uid);
                                        }
                                        sharedMessage.setLikerList(list);
                                }
                        }
                        if (object.has("inVisibleUserList")) {
                                JSONArray jsonArray = object.getJSONArray("inVisibleUserList");
                                if (jsonArray.length() > 0) {
                                        List<String> list = new ArrayList<>();
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                                String uid = jsonArray.getString(i);
                                                LogUtil.e("不可见的用户ID：" + uid);
                                                list.add(uid);
                                        }
                                        sharedMessage.setInVisibleUserList(list);
                                }
                        }
                        if (object.has("commentMsgList")) {
                                JSONArray jsonArray = object.getJSONArray("commentMsgList");
                                if (jsonArray.length() > 0) {
                                        List<String> list = new ArrayList<>();
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                                String content = jsonArray.getString(i);
                                                LogUtil.e("评论的内容列表：" + content);
                                                list.add(content);
                                        }
                                        sharedMessage.setCommentMsgList(list);
                                }
                        }
                        if (object.has("imageList")) {
                                JSONArray jsonArray = object.getJSONArray("imageList");
                                if (jsonArray.length() > 0) {
                                        List<String> list = new ArrayList<>();
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                                String url = jsonArray.getString(i);
                                                LogUtil.e("图片URL：" + url);
                                                list.add(url);
                                        }
                                        sharedMessage.setImageList(list);
                                }
                        }
                        return sharedMessage;
                } catch (JSONException e) {
                        e.printStackTrace();
                        LogUtil.e("解析说说json失败");
                        return null;
                }
        }

        public void updateUserAvatar(final String uid, final String avatar, final DealUserInfoCallBack dealUserInfoCallBack) {
                User user = new User();
                user.setObjectId(uid);
                user.setAvatar(avatar);
                user.update(CustomApplication.getInstance(), new UpdateListener() {
                        @Override
                        public void onSuccess() {
                                LogUtil.e("在服务器上更新用户头像成功");
                                dealUserInfoCallBack.updateAvatarSuccess(uid, avatar);
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                LogUtil.e("在服务器上更新用户头像失败");
                                dealUserInfoCallBack.onFailed(uid, i, s);
                        }
                });

        }


        public void updateUserSignature(final String uid, final String signature, final DealUserInfoCallBack dealUserInfoCallBack) {
                User user = new User();
                user.setSignature(signature);
                user.setObjectId(uid);
                user.update(CustomApplication.getInstance(), new UpdateListener() {
                        @Override
                        public void onSuccess() {
                                LogUtil.e("在服务器上更新用户签名成功");
                                dealUserInfoCallBack.updateSignatureSuccess(uid, signature);
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                LogUtil.e("在服务器上更新用户签名失败");
                                dealUserInfoCallBack.onFailed(uid, i, s);
                        }
                });
        }


        public void updateUserNick(final String uid, final String nick, final DealUserInfoCallBack dealUserInfoCallBack) {
                User user = new User();
                user.setNick(nick);
                user.setObjectId(uid);
                user.update(CustomApplication.getInstance(), new UpdateListener() {
                        @Override
                        public void onSuccess() {
                                LogUtil.e("在服务器上更新用户昵称成功");
                                dealUserInfoCallBack.updateNickSuccess(uid, nick);
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                LogUtil.e("在服务器上更新用户昵称失败");
                                dealUserInfoCallBack.onFailed(uid, i, s);
                        }
                });
        }

        public void updateUserinfo(final User user, final DealUserInfoCallBack dealUserInfoCallBack) {
                user.update(CustomApplication.getInstance(), new UpdateListener() {
                        @Override
                        public void onSuccess() {
                                LogUtil.e("在服务器上更新用户信息成功");
                                dealUserInfoCallBack.updateUserInfoSuccess(user);
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                dealUserInfoCallBack.onFailed(user.getObjectId(), i, s);
                        }
                });
        }


        public void getAllDefaultAvatarFromServer(final FindListener<String> findListener) {
                BmobQuery bmobQuery = new BmobQuery("sys_data");
                bmobQuery.setCachePolicy(BmobQuery.CachePolicy.CACHE_ELSE_NETWORK);
                bmobQuery.findObjects(CustomApplication.getInstance(), new FindCallback() {
                        @Override
                        public void onSuccess(JSONArray jsonArray) {
                                List<String> avatarList = null;
                                if (jsonArray != null && jsonArray.length() > 0) {
                                        LogUtil.e(jsonArray.toString());
                                        avatarList = new ArrayList<>();
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                                try {
                                                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                                                        JSONObject avatarJson = jsonObject.getJSONObject("avatar");
                                                        String avatar = avatarJson.getString("url");
                                                        if (avatar != null) {
                                                                avatarList.add(avatar);
                                                        }
                                                } catch (JSONException e) {
                                                        e.printStackTrace();
                                                }
                                        }
                                }
                                if (avatarList == null) {
                                        LogUtil.e("服务器上面没有头像数据");
                                }
                                findListener.onSuccess(avatarList);
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                findListener.onError(i, s);
                        }
                });
        }


        public void queryAllGroupNumber(String groupId, FindListener<User> listener) {
                BmobQuery<User> query = new BmobQuery<>();
                query.addWhereContainedIn("objectId", MessageCacheManager.getInstance().getGroupTableMessage(groupId).getGroupNumber());
                query.findObjects(CustomApplication.getInstance(), listener);
        }


        private void realUpdateGroupMessage(final String groupId, String name, final String content, final UpdateListener listener) {
                final GroupTableMessage groupTableMessage = MessageCacheManager.getInstance().getGroupTableMessage(groupId);
                ;
                GroupTableMessage copy = new GroupTableMessage();
                LogUtil.e("更新群结构信息时群结构表信息如下");
                LogUtil.e(groupTableMessage);
                copy.setObjectId(groupTableMessage.getObjectId());
                if (name.equals("groupNick")) {
                        LogUtil.e("改变昵称时的群结构消息");
                        copy.setGroupNick(content);
                        copy.update(CustomApplication.getInstance(), new UpdateListener() {
                                @Override
                                public void onSuccess() {
                                        LogUtil.e("更新该用户的群结构中昵称信息成功");
                                        updateGroupChatMessageNick(groupId, content, listener);
                                }

                                @Override
                                public void onFailure(int i, String s) {
                                        LogUtil.e("更新该用户的群结构昵称失败" + s + i);
                                        listener.onFailure(i, s);
                                }
                        });
                } else {
                        switch (name) {
                                case "groupAvatar":
                                        copy.setGroupAvatar(content);
                                        break;
                                case "groupName":
                                        copy.setGroupName(content);
                                        break;
                                case "groupDescription":
                                        copy.setGroupDescription(content);
                                        break;
                                case "groupNotification":
                                        copy.setNotification(content);
                                        break;
                                case "deleteNumber":
                                        List<String> list = MessageCacheManager.getInstance().getGroupTableMessage(groupId).getGroupNumber();
                                        List<String> numberCopy = new ArrayList<>(list);
                                        if (numberCopy.contains(content)) {
                                                numberCopy.remove(content);
                                        }
                                        copy.setGroupNumber(numberCopy);
                                        break;
                        }
                        copy.update(CustomApplication.getInstance(), new UpdateListener() {
                                @Override
                                public void onSuccess() {
                                        listener.onSuccess();
                                }

                                @Override
                                public void onFailure(int i, String s) {
                                        listener.onFailure(i, s);
                                }
                        });
                }
        }


        public void updateGroupMessage(final String groupId, final String name, final String content, final UpdateListener listener) {
                realUpdateGroupMessage(groupId, name, content, new UpdateListener() {
                        @Override
                        public void onSuccess() {
                                GroupTableMessage groupTableMessage = MessageCacheManager.getInstance().getGroupTableMessage(groupId);
                                switch (name) {
                                        case "groupNick":
                                                groupTableMessage.setGroupNick(content);
                                                break;
                                        case "groupAvatar":
                                                groupTableMessage.setGroupAvatar(content);
                                                break;
                                        case "groupName":
                                                groupTableMessage.setGroupName(content);
                                                break;
                                        case "groupDescription":
                                                groupTableMessage.setGroupDescription(content);
                                                break;
                                        case "groupNotification":
                                                groupTableMessage.setNotification(content);
                                                break;
                                        case "deleteNumber":
                                                List<String> numberCopy = groupTableMessage.getGroupNumber();
                                                if (numberCopy.contains(content)) {
                                                        numberCopy.remove(content);
                                                }
                                                break;
                                }
                                ChatDB.create().saveGroupTableMessage(groupTableMessage);
                                listener.onSuccess();
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                listener.onFailure(i, s);
                        }
                });
        }

        private void updateGroupChatMessageNick(final String groupId, final String nick, final UpdateListener listener) {
                BmobQuery<GroupChatMessage> query = new BmobQuery<>("g" + groupId);
                query.addWhereEqualTo("belongId",UserManager.getInstance().getCurrentUserObjectId());
                query.findObjects(CustomApplication.getInstance(), new FindCallback() {
                        @Override
                        public void onSuccess(JSONArray jsonArray) {
                                LogUtil.e("12群消息解析");
                                LogUtil.e("jsonArray：" + jsonArray.toString());
                                LogUtil.e("群消息修改如下12356");
                                List<BmobObject> updateList = new ArrayList<>();
                                for (int i = 0; i < jsonArray.length(); i++) {
                                        try {
                                                JSONObject jsonObject = jsonArray.getJSONObject(i);
//                                                GroupChatMessage groupChatMessage = MsgManager.getInstance().createReceiveGroupChatMsg(jsonObject);
//                                                groupChatMessage.setSendStatus(Constant.SEND_STATUS_SUCCESS);
//                                                groupChatMessage.setReadStatus(Constant.RECEIVE_UNREAD);
                                                GroupChatMessage message=new GroupChatMessage();
                                                message.setTableName("g"+groupId);
                                                message.setObjectId(JsonUtil.getString(jsonObject,Constant.ID));
                                                message.setBelongNick(nick);
                                                updateList.add(message);
                                                LogUtil.e("objectId:"+message.getObjectId()+"nick:"+message.getBelongNick());

                                        } catch (JSONException e) {
                                                e.printStackTrace();
                                        }
                                }
                                if (updateList.size() > 0) {
                                        new BmobObject().updateBatch(CustomApplication.getInstance(), updateList, new UpdateListener() {
                                                @Override
                                                public void onSuccess() {
                                                        LogUtil.e("更新群消息上的昵称成功");
                                                        LogUtil.e("这里更新数据库中的昵称群消息");
                                                        ChatDB.create().updateGroupChatMessageNick(groupId, nick);
                                                        listener.onSuccess();
                                                }

                                                @Override
                                                public void onFailure(int i, String s) {
                                                        LogUtil.e("更新群消息上的昵称消息失败" + s + i);
                                                        listener.onFailure(i, s);

                                                }
                                        });
                                } else {
                                        listener.onSuccess();
                                }
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                LogUtil.e("查询群消息失败：" + s + i);
                                listener.onFailure(i, s);
                        }
                });


//
//                query.findObjects(CustomApplication.getInstance(), new FindListener<GroupChatMessage>() {
//                        @Override
//                        public void onSuccess(List<GroupChatMessage> list) {
//                                if (list != null && list.size() > 0) {
//                                        for (GroupChatMessage message
//                                                : list
//                                                ) {
//                                                message.setBelongId(nick);
//                                        }
//                                        List<BmobObject> updateList = new ArrayList<>();
//                                        updateList.addAll(list);
//                                        new BmobObject().updateBatch(CustomApplication.getInstance(), updateList, new UpdateListener() {
//                                                @Override
//                                                public void onSuccess() {
//                                                        LogUtil.e("更新群消息上的昵称成功");
//                                                        LogUtil.e("这里更新数据库中的昵称群消息");
//                                                        ChatDB.create().updateGroupChatMessageNick(groupId, nick);
//                                                        listener.onSuccess();
//                                                }
//
//                                                @Override
//                                                public void onFailure(int i, String s) {
//                                                        LogUtil.e("更新群消息上的昵称消息失败" + s + i);
//                                                        listener.onFailure(i, s);
//
//                                                }
//                                        });
//                                } else {
//                                        listener.onSuccess();
//                                }
//                        }
//
//                        @Override
//                        public void onError(int i, String s) {
//                                LogUtil.e("查询该群上的属于该用户的群消息失败" + s + i);
//                                listener.onFailure(i, s);
//                        }
//                });
        }

        public void queryAllGroupTableMessage(String groupId, FindListener<GroupTableMessage> findListener) {
                BmobQuery<GroupTableMessage> query = new BmobQuery<>();
                query.addWhereEqualTo("groupId", groupId);
                query.findObjects(CustomApplication.getInstance(), findListener);
        }

        public void clearAllChatMessage() {
                ChatDB.create().clearAllMessage();
        }

        public void getAllDefaultWallPaperFromServer(final FindListener<String> findListener) {
                BmobQuery bmobQuery = new BmobQuery("sys_data");
                bmobQuery.setCachePolicy(BmobQuery.CachePolicy.CACHE_ELSE_NETWORK);
                bmobQuery.findObjects(CustomApplication.getInstance(), new FindCallback() {
                        @Override
                        public void onSuccess(JSONArray jsonArray) {
                                List<String> avatarList = null;
                                if (jsonArray != null && jsonArray.length() > 0) {
                                        LogUtil.e(jsonArray.toString());
                                        avatarList = new ArrayList<>();
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                                try {
                                                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                                                        JSONObject avatarJson = jsonObject.getJSONObject("wallpaper");
                                                        String avatar = avatarJson.getString("url");
                                                        if (avatar != null) {
                                                                avatarList.add(avatar);
                                                        }
                                                } catch (JSONException e) {
                                                        e.printStackTrace();
                                                }
                                        }
                                }
                                if (avatarList == null) {
                                        LogUtil.e("服务器上面没有背景数据");
                                }
                                findListener.onSuccess(avatarList);
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                findListener.onError(i, s);
                        }
                });
        }

        public void getAllDefaultTitleWallPaperFromServer(final FindListener<String> findListener) {
                BmobQuery bmobQuery = new BmobQuery("sys_data");
                bmobQuery.setCachePolicy(BmobQuery.CachePolicy.CACHE_ELSE_NETWORK);
                bmobQuery.findObjects(CustomApplication.getInstance(), new FindCallback() {
                        @Override
                        public void onSuccess(JSONArray jsonArray) {
                                List<String> avatarList = null;
                                if (jsonArray != null && jsonArray.length() > 0) {
                                        LogUtil.e(jsonArray.toString());
                                        avatarList = new ArrayList<>();
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                                try {




                                                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                                                        JSONObject avatarJson = jsonObject.getJSONObject("twallpaper");
                                                        String avatar = avatarJson.getString("url");
                                                        if (avatar != null) {
                                                                avatarList.add(avatar);
                                                        }
                                                } catch (JSONException e) {
                                                        e.printStackTrace();
                                                }
                                        }
                                }
                                if (avatarList == null) {
                                        LogUtil.e("服务器上面没有背景数据");
                                }
                                findListener.onSuccess(avatarList);
                        }

                        @Override
                        public void onFailure(int i, String s) {
                                findListener.onError(i, s);
                        }
                });
        }

//        public void loadAllMyShareMessages(boolean isPullRefresh, String time, final LoadShareMessageCallBack loadShareMessageCallBack) {
//                if (CommonUtils.isNetWorkAvailable()) {
//                        realLoadAllMyShareMessages(isPullRefresh, time, new LoadShareMessageCallBack() {
//                                @Override
//                                public void onSuccess(List<SharedMessage> data) {
//                                        LogUtil.e("下拉刷新从服务器上面加载数据成功");
//                                        ChatDB.create().saveAllSharedMessage(data);
//                                        loadShareMessageCallBack.onSuccess(data);
//                                }
//
//                                @Override
//                                public void onFailed(String errorMsg, int errorId) {
//                                        LogUtil.e("下拉刷新从服务器上面加载数据失败" + errorMsg + errorId);
//                                        loadShareMessageCallBack.onFailed(errorMsg, errorId);
//                                }
//                        });
//                } else {
//                        List<SharedMessage> list;
//                        list = ChatDB.create().getMyAllSharedMessage(isPullRefresh, time, 10);
//                        if (list != null) {
//                                LogUtil.e("无网络时从数据库中加载数据成功");
//                                loadShareMessageCallBack.onSuccess(list);
//                        } else {
//                                LogUtil.e("无网络时从数据库中加载数据失败");
//                                loadShareMessageCallBack.onFailed("数据库中加载说说消息失败", 0);
//                        }
//                }
//        }

        private void realLoadAllMyShareMessages(boolean isPullRefresh, String time, final LoadShareMessageCallBack loadShareMessageCallBack) {
                try {
                        BmobQuery<SharedMessage> query = new BmobQuery<>();
                        query.addWhereEqualTo("belongId",UserManager.getInstance().getCurrentUser().getObjectId());
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        long currentTime = simpleDateFormat.parse(time).getTime();
                        LogUtil.e("现在的时间:" + currentTime);
                        BmobDate bmobDate;
                        if (isPullRefresh) {
                                currentTime += 1000;
                                LogUtil.e("加一秒后的时间" + currentTime);
                                bmobDate = new BmobDate(new Date(currentTime));
                                query.addWhereGreaterThan("createdAt", bmobDate);
                        } else {
                                currentTime -= 1000;
                                LogUtil.e("减一秒后的时间" + currentTime);
                                bmobDate = new BmobDate(new Date(currentTime));
                                query.addWhereLessThan("createdAt", bmobDate);
                        }
                        query.order("-createdAt");
                        query.setLimit(10);
                        query.setCachePolicy(BmobQuery.CachePolicy.NETWORK_ONLY);
                        query.findObjects(CustomApplication.getInstance(), new FindListener<SharedMessage>() {
                                @Override
                                public void onSuccess(List<SharedMessage> list) {
                                        LogUtil.e("从服务器上加载得到的最新的不多于10条的说说消息为：");
                                        if (list != null) {
                                                for (SharedMessage sharedMessage :
                                                        list) {
                                                        LogUtil.e(sharedMessage);
                                                }
                                        } else {
                                                LogUtil.e("说说消息为空！！！！！");
                                        }
                                        loadShareMessageCallBack.onSuccess(list);
                                }

                                @Override
                                public void onError(int i, String s) {
                                        LogUtil.e("在服务器上查找不到说说消息");
                                        loadShareMessageCallBack.onFailed(s, i);
                                }
                        });
                } catch (ParseException e) {
                        e.printStackTrace();
                        LogUtil.e("刷新查询说说消息时解析时间失败" + e.getMessage());
                }

        }


        public void deleteGroupTableMessage(String objectId, DeleteListener deleteListener) {
                LogUtil.e("删除的群结构id"+objectId);
                GroupTableMessage groupTableMessage=new GroupTableMessage();
                groupTableMessage.setObjectId(objectId);
                groupTableMessage.delete(CustomApplication.getInstance(),deleteListener);
        }
}
