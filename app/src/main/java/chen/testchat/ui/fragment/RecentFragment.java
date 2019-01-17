package chen.testchat.ui.fragment;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.view.ViewGroup;

import org.pointstone.cugappplat.base.cusotomview.swipeview.Closeable;
import org.pointstone.cugappplat.base.cusotomview.swipeview.OnSwipeMenuItemClickListener;
import org.pointstone.cugappplat.base.cusotomview.swipeview.SwipeMenu;
import org.pointstone.cugappplat.base.cusotomview.swipeview.SwipeMenuCreator;
import org.pointstone.cugappplat.base.cusotomview.swipeview.SwipeMenuItem;
import org.pointstone.cugappplat.base.cusotomview.swipeview.SwipeMenuRecyclerView;
import org.pointstone.cugappplat.baseadapter.BaseWrappedViewHolder;
import org.pointstone.cugappplat.rxbus.RxBusManager;
import org.pointstone.cugappplat.util.ToastUtils;

import java.util.List;

import chen.testchat.R;
import chen.testchat.adapter.ConversationListAdapter;
import chen.testchat.base.Constant;
import chen.testchat.bean.GroupChatMessage;
import chen.testchat.bean.RecentMsg;
import chen.testchat.bean.User;
import chen.testchat.db.ChatDB;
import chen.testchat.events.GroupInfoEvent;
import chen.testchat.listener.OnBaseItemClickListener;
import chen.testchat.listener.OnNetWorkChangedListener;
import chen.testchat.manager.MessageCacheManager;
import chen.testchat.manager.MsgManager;
import chen.testchat.manager.UserManager;
import chen.testchat.receiver.NetWorkChangedReceiver;
import chen.testchat.service.GroupMessageService;
import chen.testchat.ui.ChatActivity;
import chen.testchat.ui.MainActivity;
import chen.testchat.util.LogUtil;
import chen.testchat.view.ListViewDecoration;
import cn.bmob.v3.listener.FindListener;
import rx.Subscription;
import rx.functions.Action1;

/**
 * 项目名称:    HappyChat
 * 创建人:        陈锦军
 * 创建时间:    2016/9/13      12:18
 * QQ:             1981367757
 */


/**
 * 最近会话列表fragment
 */
public class RecentFragment extends org.pointstone.cugappplat.base.basemvp.BaseFragment implements SwipeRefreshLayout.OnRefreshListener, OnNetWorkChangedListener {
        private ConversationListAdapter mAdapter;
        private SwipeRefreshLayout mSwipeRefreshLayout;
        private SwipeMenuRecyclerView display;
        private NetWorkChangedReceiver netWorkChangedReceiver;
        private LinearLayoutManager mLinearLayoutManager;
        private Intent intent;
        private ServiceConnection connection;
        private GroupMessageService.NotifyBinder binder;


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
                return R.layout.fragment_recent;
        }

        @Override
        public void initView() {
                LogUtil.e("12RecentFragment：initView");
                display = (SwipeMenuRecyclerView) findViewById(R.id.rcv_recent_display);
                mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_recent_container);
                display.setLayoutManager(mLinearLayoutManager = new LinearLayoutManager(getActivity()));
                display.setItemAnimator(new DefaultItemAnimator());
                display.addItemDecoration(new ListViewDecoration(getActivity()));
//                display.setOnItemMoveListener(this);
                display.setSwipeMenuCreator(new SwipeMenuCreator() {
                        @Override
                        public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int viewType) {
                                int width = getActivity().getResources().getDimensionPixelSize(R.dimen.recent_top_height);
                                int height = ViewGroup.LayoutParams.MATCH_PARENT;
//                                 添加左右侧菜单
                                {
                                        SwipeMenuItem topItem = new SwipeMenuItem(getActivity());
                                        topItem.setBackgroundDrawable(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                                                0xCE))).setText("置顶").setTextColor(Color.WHITE).setWidth(width).setHeight(height);
                                        SwipeMenuItem deleteItem = new SwipeMenuItem(getActivity());
                                        deleteItem.setBackgroundDrawable(new ColorDrawable(Color.rgb(0xF9,
                                                0x3F, 0x25))).setText("删除").setTextColor(Color.WHITE).setHeight(height).setWidth(width);
                                        swipeRightMenu.addMenuItem(topItem);
                                        swipeRightMenu.addMenuItem(deleteItem);
                                }
                        }
                });
                display.setSwipeMenuItemClickListener(new MySwipeItemClickListener());
                mSwipeRefreshLayout.setOnRefreshListener(this);
                display.setAdapter(mAdapter = new ConversationListAdapter(ChatDB.create().getAllRecentMsg(), R.layout.fragment_recent_item));
                display.addOnItemTouchListener(new OnBaseItemClickListener() {
                        @Override
                        protected void onItemClick(BaseWrappedViewHolder baseWrappedViewHolder, int id, View view, int position) {
                                if (id == R.id.rl_recent_item_container) {
                                        LogUtil.e("id对了");

                                } else {
                                        LogUtil.e("id错了");
                                }
                                Intent intent = new Intent(getActivity(), ChatActivity.class);
                                RecentMsg msg;
                                msg = mAdapter.getData(position);
                                if (MessageCacheManager.getInstance().getGroupTableMessage(msg.getBelongId()) == null) {
                                        User user = new User();
                                        user.setObjectId(msg.getBelongId());
                                        user.setAvatar(msg.getAvatar());
                                        user.setNick(msg.getNick());
                                        user.setUsername(msg.getName());
                                        intent.putExtra("user", user);
                                        intent.putExtra("from", "person");
                                } else {
                                        intent.putExtra("from", "group");
                                        intent.putExtra("groupId", msg.getBelongId());
                                }
                                startActivity(intent);
                        }
                });
                display.setSwipeMenuItemClickListener(new MySwipeItemClickListener());
        }


        @Override
        public void initData() {
                LogUtil.e("RecentFragment：initData");
                if (mLinearLayoutManager.findViewByPosition(0) != null) {
                        mLinearLayoutManager.findViewByPosition(0).setVisibility(View.GONE);
                }
                getActivity().registerReceiver(netWorkChangedReceiver = new NetWorkChangedReceiver(), new IntentFilter(Constant.NETWORK_CONNECTION_CHANGE));
                netWorkChangedReceiver.registerListener(this);
                registerRxBus();


        }


        private void registerRxBus() {
                Subscription mSubscription = RxBusManager.getInstance().registerEvent(GroupInfoEvent.class, new Action1<GroupInfoEvent>() {
                        @Override
                        public void call(GroupInfoEvent groupInfoEvent) {
//                                刷新过来的，更新下群结构消息
                                if (groupInfoEvent.getType()==GroupInfoEvent.TYPE_GROUP_NUMBER){
                                                String content=groupInfoEvent.getContent();
                                                LogUtil.e("recent接收到被提出群的消息");
                                                RecentMsg recentMsg=new RecentMsg();
                                                recentMsg.setBelongId(content);
                                        if (mAdapter.getAllData().contains(recentMsg)) {
                                                mAdapter.getAllData().remove(recentMsg);
                                                mAdapter.notifyDataSetChanged();
                                        }
                                }
                        }
                }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                                LogUtil.e("rxbus传递出现异常");
                                if (throwable != null) {
                                        LogUtil.e(throwable.getMessage());
                                }
                        }
                });
                RxBusManager.getInstance().addSubscription(this, mSubscription);
        }




        @Override
        protected void updateView() {
        }


        @Override
        public void onResume() {
                super.onResume();
                onHiddenChanged(false);
        }

        @Override
        public void onHiddenChanged(boolean hidden) {
                super.onHiddenChanged(hidden);
                if (!hidden) {
                        ((MainActivity) getActivity()).initActionBar("聊天");
                        mAdapter.clearData();
                        mAdapter.addData(ChatDB.create().getAllRecentMsg());
                }
        }



        @Override
        public void onRefresh() {
//                这里进行更新操作
                LogUtil.e("主界面刷新拉");
                mAdapter.clearData();
                mAdapter.addData(ChatDB.create().getAllRecentMsg());
                if (mSwipeRefreshLayout.isRefreshing()) {
                        mSwipeRefreshLayout.setRefreshing(false);
                }
        }


        @Override
        public void onDestroy() {
                super.onDestroy();
                if (netWorkChangedReceiver != null) {
                        netWorkChangedReceiver.unregisterListener(this);
                        getActivity().unregisterReceiver(netWorkChangedReceiver);
                }
                RxBusManager.getInstance().unSubscrible(this);
                LogUtil.e("这里停止实时检测服务拉");
                if (connection != null) {
                        getActivity().unbindService(connection);
                        connection = null;
                }
        }


        /**
         * 由主activity调用更新数据
         */
        public void updateRecentData(String id) {
                RecentMsg recentMsg;
                recentMsg = ChatDB.create().getRecentMsg(id);
                LogUtil.e("最近消息如下1");
                if (recentMsg != null) {
                        LogUtil.e(recentMsg);
                        mAdapter.addData(recentMsg);
                }
        }

        @Override
        public void OnNetWorkChanged(boolean isConnected, int type) {
                LogUtil.e("1网络状态改变");
                if (type == ConnectivityManager.TYPE_WIFI) {
                        LogUtil.e("当前的状态为" + isConnected + "   111当前的连接类型为wifi");
                } else {
                        LogUtil.e("当前的状态为" + isConnected + "   当前的连接类型移动网络");
                }
                if (isConnected) {

//                        查询好友信息

//                        这里拉取网络中断这段时间内的信息
//                        queryGroupChatMessage();
                        MsgManager.getInstance().queryGroupChatMessage(MessageCacheManager.getInstance().getAllGroupId(), new FindListener<GroupChatMessage>() {
                                @Override
                                public void onSuccess(List<GroupChatMessage> list) {
                                        for (GroupChatMessage groupChatMessage :
                                                list) {
                                                if (MsgManager.getInstance().saveRecentAndChatGroupMessage(groupChatMessage)) {
                                                        RecentMsg recentMsg1 = ChatDB.create().getRecentMsg(groupChatMessage.getGroupId());
                                                        if (recentMsg1 != null) {
                                                                mAdapter.addData(recentMsg1);
                                                        }
                                                }
                                        }
                                }

                                @Override
                                public void onError(int i, String s) {
                                        LogUtil.e(s+i);
                                }
                        });
//                        queryUser();
                        UserManager.getInstance().refreshUserInfo();
                        if (connection == null) {
                                connection = new ServiceConnection() {
                                        @Override
                                        public void onServiceConnected(ComponentName name, IBinder service) {
                                                binder = (GroupMessageService.NotifyBinder) service;
                                        }

                                        @Override
                                        public void onServiceDisconnected(ComponentName name) {
                                                LogUtil.e("服务绑定拉");

                                        }
                                };
                                if (intent == null) {
                                        intent = new Intent();
                                        intent.setClass(getContext(), GroupMessageService.class);
                                }
                                getActivity().bindService(intent, connection, Service.BIND_AUTO_CREATE);
                        } else {
                                LogUtil.e("已经绑定了");
                                if (binder != null) {
                                        LogUtil.e("网络又连接通时，开始实时监听");
                                        binder.startListener();
                                }
                        }
                }
        }

//        private void queryUser() {
//                List<String> userList = UserCacheManager.getInstance().getAllUserId();
//                if (userList != null && userList.size() > 0) {
//                        for (final String uid :
//                                userList) {
//                                BmobQuery<User> query = new BmobQuery<>();
//                                String lastTime = MessageCacheManager.getInstance().getUserDataLastUpdateTime(uid);
////                                        第一次断网查询用户数据
//                                try {
//                                        query.addWhereGreaterThan("updatedAt", new BmobDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(lastTime)));
//                                        query.addWhereEqualTo("objectId", uid);
//                                } catch (ParseException e) {
//                                        e.printStackTrace();
//                                        LogUtil.e("解析时间错误");
//                                }
//                                query.findObjects(CustomApplication.getInstance(), new FindListener<User>() {
//                                        @Override
//                                        public void onSuccess(List<User> list) {
//                                                if (list != null && list.size() > 0) {
//                                                        User user = list.get(0);
//                                                        MessageCacheManager.getInstance().setUserDataLastUpdateTime(uid, user.getUpdatedAt());
//                                                        ChatDB.create().addOrUpdateContacts(user);
//                                                        UserCacheManager.getInstance().addContact(user);
//                                                }
//                                        }
//
//                                        @Override
//                                        public void onError(int i, String s) {
//                                                LogUtil.e("断网期间内查询用户失败" + s + i);
//                                        }
//                                });
//                        }
//                }
//        }
//
//        private void queryGroupChatMessage() {
//                List<String> groupIdList = MessageCacheManager.getInstance().getAllGroupId();
//                if (groupIdList.size() > 0) {
//                        for (String groupId :
//                                groupIdList) {
//                                BmobQuery<GroupChatMessage> query = new BmobQuery<>("g" + groupId);
//                                final RecentMsg recentMsg = ChatDB.create().getRecentMsg(groupId);
//                                long lastTime;
//                                if (recentMsg == null) {
//                                        lastTime = 0;
//                                } else {
//                                        lastTime = Long.valueOf(ChatDB.create().getRecentMsg(groupId).getTime());
//                                }
//                                query.addWhereGreaterThan("updatedAt", new BmobDate(new Date(lastTime)));
////                                query.addWhereEqualTo("groupId", groupId);
//                                query.findObjects(CustomApplication.getInstance(), new FindCallback() {
//                                        @Override
//                                        public void onSuccess(JSONArray jsonArray) {
//                                                LogUtil.e("群消息解析");
//                                                LogUtil.e("jsonArray：" + jsonArray.toString());
//                                                for (int i = 0; i < jsonArray.length(); i++) {
//                                                        try {
//                                                                JSONObject jsonObject = jsonArray.getJSONObject(i);
//                                                                GroupChatMessage groupChatMessage = MsgManager.getInstance().createReceiveGroupChatMsg(jsonObject);
//                                                                groupChatMessage.setSendStatus(Constant.SEND_STATUS_SUCCESS);
//                                                                groupChatMessage.setReadStatus(Constant.RECEIVE_UNREAD);
//                                                                if (MsgManager.getInstance().saveRecentAndChatGroupMessage(groupChatMessage)) {
//                                                                        RecentMsg recentMsg1 = ChatDB.create().getRecentMsg(groupChatMessage.getGroupId());
//                                                                        if (recentMsg1 != null) {
//                                                                                mAdapter.addData(recentMsg1);
//                                                                        }
//                                                                }
//                                                        } catch (JSONException e) {
//                                                                e.printStackTrace();
//                                                        }
//                                                }
//                                        }
//
//                                        @Override
//                                        public void onFailure(int i, String s) {
//                                                LogUtil.e("查询断网期间的群消息失败：" + s + i);
//                                        }
//                                });
//                        }
//                }
//        }


        @Override
        public void onDestroyView() {
                super.onDestroyView();
                if (getActivity() != null) {
                        getActivity().stopService(intent);
                }
        }





        public void notifyGroupTableMsgCome(String groupId) {
                LogUtil.e("这里通知服务监听群消息");
                if (binder != null) {
                        binder.addGroup(groupId);
                } else {
                        LogUtil.e("binder 是空的，监听不了群消息");
                }
        }


        public void notifySharedMessageChanged(String objectId, boolean isAdd) {
                if (binder != null) {
                        if (isAdd) {
                                LogUtil.e("通知实时监听说说消息拉拉");
                                binder.addShareMessage(objectId);
                        } else {
                                LogUtil.e("移除实时监听说说消息拉拉");
                                binder.removeShareMessage(objectId);
                        }
                } else {
                        LogUtil.e("binder为空");
                }
        }

        public void notifyUserAdd(String id) {
                if (binder != null) {
                        LogUtil.e("实时监听新增的用户信息");
                        binder.addUser(id);
                }
        }


        private class MySwipeItemClickListener implements OnSwipeMenuItemClickListener {
                @Override
                public void onItemClick(Closeable closeable, int adapterPosition, int menuPosition, int direction) {
//                        删除操作
                        if (direction == SwipeMenuRecyclerView.RIGHT_DIRECTION) {
                                if (menuPosition == 0) {
                                        ToastUtils.showShortToast("置顶操作");
//                                        置顶操作
                                } else if (menuPosition == 1) {
//                                        删除操作，1、最近会话删除
                                        RecentMsg msg = mAdapter.getData(adapterPosition);
                                        LogUtil.e("将要删除的最近会话的消息：" + msg.getLastMsgContent());
                                        ChatDB.create().deleteRecentMsg(msg.getBelongId(), msg.getTime());
//                                        删除操作，2、聊天消息删除
                                        if (MessageCacheManager.getInstance().getGroupTableMessage(msg.getBelongId()) == null) {
                                                ChatDB.create().deleteAllChatMessage(msg.getBelongId());
                                        } else {
                                                ChatDB.create().deleteAllGroupChatMessage(msg.getBelongId());
                                        }
//                                        数据中删除
                                        mAdapter.removeData(adapterPosition);
                                }
                        }
                }
        }
}
