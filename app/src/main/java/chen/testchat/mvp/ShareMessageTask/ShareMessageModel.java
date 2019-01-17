package chen.testchat.mvp.ShareMessageTask;

import java.util.List;

import chen.testchat.bean.SharedMessage;
import chen.testchat.db.ChatDB;
import chen.testchat.listener.AddShareMessageCallBack;
import chen.testchat.listener.DealCommentMsgCallBack;
import chen.testchat.listener.DealMessageCallBack;
import chen.testchat.listener.LoadShareMessageCallBack;
import chen.testchat.manager.MsgManager;
import chen.testchat.manager.UserManager;
import chen.testchat.util.LogUtil;

/**
 * 项目名称:    TestChat
 * 创建人:        陈锦军
 * 创建时间:    2016/12/2      21:00
 * QQ:             1981367757
 */

public class ShareMessageModel implements ShareMessageContacts.Model {


        @Override
        public void addShareMessage(SharedMessage shareMessage, final AddShareMessageCallBack listener) {
                if (!ChatDB.create().hasSharedMessage(shareMessage.getObjectId())) {
                        MsgManager.getInstance().sendShareMessage(shareMessage, new AddShareMessageCallBack() {
                                @Override
                                public void onSuccess(SharedMessage shareMessage) {
                                        listener.onSuccess(shareMessage);
                                }

                                @Override
                                public void onFailed(int errorId, String errorMsg) {
                                        listener.onFailed(errorId, errorMsg);
                                }
                        });
                } else {
                        LogUtil.e("数据库中已经保存该说说消息了");
                        listener.onSuccess(shareMessage);
                }
        }

        @Override
        public void deleteShareMessage(String id, final DealMessageCallBack dealMessageCallBack) {
                MsgManager.getInstance().deleteShareMessage(id, new DealMessageCallBack() {
                        @Override
                        public void onSuccess(String id) {
//                                这里可能会删除失败，因为在监听该说说的服务会先于其发送通知消息到服务那里，所以在服务那里已经删除了
                                ChatDB.create().deleteSharedMessage(id);
                                dealMessageCallBack.onSuccess(id);
                        }

                        @Override
                        public void onFailed(String id, int errorId, String errorMsg) {
                                dealMessageCallBack.onFailed(id, errorId, errorMsg);
                        }
                });
        }

        @Override
        public void addLiker(String id, final DealMessageCallBack dealMessageCallBack) {
                MsgManager.getInstance().addLiker(id, new DealMessageCallBack() {
                        @Override
                        public void onSuccess(String id) {
                                dealMessageCallBack.onSuccess(id);
                        }

                        @Override
                        public void onFailed(String id, int errorId, String errorMsg) {
                                dealMessageCallBack.onFailed(id, errorId, errorMsg);

                        }
                });
        }

        @Override
        public void deleteLiker(String id, final DealMessageCallBack dealMessageCallBack) {
                MsgManager.getInstance().deleteLiker(id, new DealMessageCallBack() {
                        @Override
                        public void onSuccess(String id) {
                                dealMessageCallBack.onSuccess(id);
                        }

                        @Override
                        public void onFailed(String id, int errorId, String errorMsg) {
                                dealMessageCallBack.onFailed(id, errorId, errorMsg);
                        }
                });
        }

        @Override
        public void addComment(String id, String content, final DealCommentMsgCallBack dealCommentMsgCallBack) {
                MsgManager.getInstance().addComment(id, content, new DealCommentMsgCallBack() {
                        @Override
                        public void onSuccess(String shareMessageId, String content, int position) {
                                dealCommentMsgCallBack.onSuccess(shareMessageId, content, position);
                        }

                        @Override
                        public void onFailed(String shareMessageId, String content, int position, int errorId, String errorMsg) {
                                dealCommentMsgCallBack.onFailed(shareMessageId, content, position, errorId, errorMsg);
                        }
                });
        }

        @Override
        public void deleteComment(String id, int position, final DealCommentMsgCallBack dealCommentMsgCallBack) {
                MsgManager.getInstance().deleteComment(id, position, new DealCommentMsgCallBack() {
                        @Override
                        public void onSuccess(String shareMessageId, String content, int position) {
//                                这里返回的评论消息，唯一不同点就是是否设置了删除标记，如果设置了，就表明是推送不成功的情况
                                dealCommentMsgCallBack.onSuccess(shareMessageId, content, position);
                        }

                        @Override
                        public void onFailed(String shareMessageId, String content, int position, int errorId, String errorMsg) {
                                dealCommentMsgCallBack.onFailed(shareMessageId, content, position, errorId, errorMsg);
                        }
                });

        }

        @Override
        public void loadAllShareMessages(final boolean isPullRefresh, final String time, final LoadShareMessageCallBack loadShareMessageCallBack) {
                MsgManager.getInstance().loadAllShareMessages(isPullRefresh, time, new LoadShareMessageCallBack() {
                        @Override
                        public void onSuccess(List<SharedMessage> data) {
                                LogUtil.e("保存到数据库之前的说说消息");
                                if (data != null) {
                                        for (SharedMessage sharedMessage :
                                                data) {
                                                LogUtil.e(sharedMessage);
                                        }
                                }
                                ChatDB.create().saveAllSharedMessage(data);
                                loadShareMessageCallBack.onSuccess(data);
                        }

                        @Override
                        public void onFailed(String errorMsg, int errorId) {
                                LogUtil.e("从服务器上获取说说数据失败，这里从数据库中获取");
//                                List<SharedMessage> list;
//                                list = ChatDB.create().getAllSharedMessage(true,isPullRefresh, time, 10);
//                                if (list != null) {
//                                        LogUtil.e("无网络时从数据库中加载数据成功");
//                                        loadShareMessageCallBack.onSuccess(list);
//                                } else {
//                                        LogUtil.e("无网络时从数据库中加载数据失败");
//                                        loadShareMessageCallBack.onFailed("数据库中加载说说消息失败", 0);
//                                }
                                loadShareMessageCallBack.onFailed(errorMsg, errorId);
                        }
                });
        }

        @Override
        public void loadShareMessages(final String uid, final boolean isPullRefresh, final String time, final LoadShareMessageCallBack loadShareMessageCallBack) {
                MsgManager.getInstance().loadShareMessages(false, uid, isPullRefresh, time, new LoadShareMessageCallBack() {
                        @Override
                        public void onSuccess(List<SharedMessage> data) {
                                if (uid.equals(UserManager.getInstance().getCurrentUserObjectId())) {
                                        LogUtil.e("本地用户，保存到数据库中");
                                        LogUtil.e("保存本用户的说说消息如下");
                                        if (data != null) {
                                                for (SharedMessage sharedMessage :
                                                        data) {
                                                        LogUtil.e(sharedMessage);
                                                }
                                        }

                                        ChatDB.create().saveAllSharedMessage(data);
                                }
                                loadShareMessageCallBack.onSuccess(data);
                        }

                        @Override
                        public void onFailed(String errorMsg, int errorId) {
                                if (uid.equals(UserManager.getInstance().getCurrentUserObjectId())) {
                                        List<SharedMessage> list;
                                        list = ChatDB.create().getAllSharedMessage(false,isPullRefresh, time, 10);
                                        if (list != null) {
                                                LogUtil.e("无网络时从数据库中加载数据成功");
                                                loadShareMessageCallBack.onSuccess(list);
                                        } else {
                                                LogUtil.e("无网络时从数据库中加载数据失败");
                                                loadShareMessageCallBack.onFailed("数据库中加载说说消息失败", 0);
                                        }
                                }
                                LogUtil.e("查询说说失败"+errorMsg+errorId);
                                loadShareMessageCallBack.onFailed(errorMsg,errorId);
                        }
                });
        }


}
