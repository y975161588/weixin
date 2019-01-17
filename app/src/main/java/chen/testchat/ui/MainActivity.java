package chen.testchat.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amap.api.services.weather.LocalWeatherForecastResult;
import com.amap.api.services.weather.LocalWeatherLive;
import com.amap.api.services.weather.LocalWeatherLiveResult;
import com.amap.api.services.weather.WeatherSearch;
import com.amap.api.services.weather.WeatherSearchQuery;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.nineoldandroids.view.ViewHelper;

import org.pointstone.cugappplat.base.cusotomview.RoundAngleImageView;
import org.pointstone.cugappplat.base.cusotomview.ToolBarOption;
import org.pointstone.cugappplat.baseadapter.BaseWrappedViewHolder;
import org.pointstone.cugappplat.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import chen.testchat.R;
import chen.testchat.adapter.MenuDisplayAdapter;
import chen.testchat.base.Constant;
import chen.testchat.bean.ChatMessage;
import chen.testchat.bean.GroupChatMessage;
import chen.testchat.bean.GroupTableMessage;
import chen.testchat.bean.SharedMessage;
import chen.testchat.bean.User;
import chen.testchat.bean.WeatherInfoBean;
import chen.testchat.db.ChatDB;
import chen.testchat.listener.AddFriendCallBackListener;
import chen.testchat.listener.LocationChangedListener;
import chen.testchat.listener.OnBaseItemClickListener;
import chen.testchat.listener.OnDragDeltaChangeListener;
import chen.testchat.listener.OnMessageReceiveListener;
import chen.testchat.listener.OnNetWorkChangedListener;
import chen.testchat.manager.ChatNotificationManager;
import chen.testchat.manager.LocationManager;
import chen.testchat.manager.MessageCacheManager;
import chen.testchat.manager.MsgManager;
import chen.testchat.manager.UserCacheManager;
import chen.testchat.manager.UserManager;
import chen.testchat.receiver.NetWorkChangedReceiver;
import chen.testchat.receiver.PushMessageReceiver;
import chen.testchat.service.PollService;
import chen.testchat.ui.fragment.ContactsFragment;
import chen.testchat.ui.fragment.InvitationFragment;
import chen.testchat.ui.fragment.RecentFragment;
import chen.testchat.ui.fragment.ShareMessageFragment;
import chen.testchat.util.LogUtil;
import chen.testchat.view.DragLayout;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;

import static android.view.View.GONE;

public class MainActivity extends org.pointstone.cugappplat.base.basemvp.BaseActivity implements OnDragDeltaChangeListener, OnMessageReceiveListener, View.OnClickListener, OnNetWorkChangedListener, LocationChangedListener {
    private Fragment[] mFragments = new Fragment[4];
    private int currentPosition;
    private DragLayout container;
    private RecyclerView menuDisplay;
    private List<String> data = new ArrayList<>();
    private MenuDisplayAdapter menuAdapter;
    private String from;
    private ImageView bg;
    private RoundAngleImageView avatar;
    private TextView nick;
    private TextView signature;
    private RelativeLayout headLayout;
    private User user;
    private TextView net;
    private NetWorkChangedReceiver netWorkReceiver;
    private TextView weatherCity;
    private TextView weatherTemperature;
    private WeatherInfoBean mWeatherInfoBean;
    private LinearLayout bottomLayout;
    private NewMessageReceiver mReceiver;
    private ShareMessageReceiver shareReceiver;


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogUtil.e("onNewIntent");
        if (intent.getSerializableExtra("url_share_message") != null) {
            SharedMessage sharedMessage = (SharedMessage) intent.getSerializableExtra("url_share_message");
            notifyUrlSharedMessageAdd(sharedMessage);
            return;
        }
        from = intent.getStringExtra(Constant.NOTIFICATION_TAG);
        if (from != null && from.equals(Constant.NOTIFICATION_TAG_ADD)) {
            addOrReplaceFragment(mFragments[2], R.id.fl_content_container);
        } else {
            addOrReplaceFragment(mFragments[0], R.id.fl_content_container);
            currentPosition = 0;
        }
    }


    @Override
    public void initView() {
        LogUtil.e("initMain123");
        RecentFragment recentFragment = new RecentFragment();
        ContactsFragment contactsFragment = new ContactsFragment();
        InvitationFragment invitationFragment = new InvitationFragment();
        ShareMessageFragment shareMessageFragment = new ShareMessageFragment();
        mFragments[0] = recentFragment;
        mFragments[1] = contactsFragment;
        mFragments[2] = invitationFragment;
        mFragments[3] = shareMessageFragment;
        container = (DragLayout) findViewById(R.id.drag_container);
        menuDisplay = (RecyclerView) findViewById(R.id.rev_menu_display);
        nick = (TextView) findViewById(R.id.tv_menu_nick);
        signature = (TextView) findViewById(R.id.tv_menu_signature);
        avatar = (RoundAngleImageView) findViewById(R.id.riv_menu_avatar);
        headLayout = (RelativeLayout) findViewById(R.id.rl_menu_head_layout);
        bg = (ImageView) findViewById(R.id.iv_main_bg);
        net = (TextView) findViewById(R.id.tv_main_net);
        weatherCity = (TextView) findViewById(R.id.tv_menu_weather_city);
        weatherTemperature = (TextView) findViewById(R.id.tv_menu_weather_temperature);
        bottomLayout = (LinearLayout) findViewById(R.id.ll_menu_bottom_container);
        bg.setAlpha((float) 0.0);
        menuDisplay.setLayoutManager(new LinearLayoutManager(this));
        menuDisplay.setHasFixedSize(true);
        menuDisplay.setItemAnimator(new DefaultItemAnimator());
        container.setListener(this);
        headLayout.setOnClickListener(this);
        net.setOnClickListener(this);
        bottomLayout.setOnClickListener(this);
        initActionBarView();
    }


    /**
     * 这里重新构建一个头部布局，因为封装的基类中的头部布局与滑动发生冲突
     */
    private RoundAngleImageView icon_1;
    private TextView right_1;
    private TextView title_1;
    private ImageView rightImage_1;
    protected ImageView back_1;

    private void initActionBarView() {
        icon_1 = (RoundAngleImageView) findViewById(R.id.riv_header_layout_icon);
        right_1 = (TextView) findViewById(R.id.tv_header_layout_right);
        title_1 = (TextView) findViewById(R.id.tv_header_layout_title);
        rightImage_1 = (ImageView) findViewById(R.id.iv_header_layout_right);
        back_1 = (ImageView) findViewById(R.id.iv_header_layout_back);
        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);
    }


    @Override
    public void initData() {
        if (from == null || from.equals("") || from.equals(Constant.NOTIFICATION_TAG_MESSAGE) || from.equals(Constant.NOTIFICATION_TAG_AGREE)
                || from.equals(Constant.NOTIFICATION_TAG_GROUP_MESSAGE)) {
            addOrReplaceFragment(mFragments[0], R.id.fl_content_container);
        } else if (from.equals(Constant.NOTIFICATION_TAG_ADD)) {
            addOrReplaceFragment(mFragments[2], R.id.fl_content_container);
        }
        data.add("聊天");
        data.add("好友");
        data.add("邀请");
        data.add("动态");
        menuAdapter = new MenuDisplayAdapter(data, R.layout.menu_item);
        menuDisplay.addOnItemTouchListener(new OnBaseItemClickListener() {
            @Override
            protected void onItemClick(BaseWrappedViewHolder baseWrappedViewHolder, int id, View view, int position) {
                if (currentPosition != position) {
                    addOrReplaceFragment(mFragments[position]);
                    currentPosition = position;
                }
                closeMenu();
            }
        });
        menuDisplay.setAdapter(menuAdapter);
//                注册消息接受器
        initReceiver();
//                bindPollService(5);
        getWeatherInfo();
        initUserInfo();
        if (getIntent().getBooleanExtra("isFirstLogin", false)) {
            ToastUtils.showShortToast("首次登录，设置你的个人资料吧^_^");
            Intent intent = new Intent(this, EditUserInfoActivity.class);
            startActivityForResult(intent, Constant.REQUEST_CODE_EDIT_USER_INFO);
        }
        if (from != null && from.equals(Constant.NOTIFICATION_TAG_GROUP_MESSAGE)) {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("id", getIntent().getStringExtra("groupId"));
            startActivity(intent);
        }
    }

    private void initReceiver() {
        IntentFilter intentFilter = new IntentFilter(Constant.NEW_MESSAGE_ACTION);
//                这里设置优先级要比ChatActivity低,聊天界面一旦接受到消息，这里就不需要再接受
        intentFilter.setPriority(10);
//                聊天消息的监听
        registerReceiver(mReceiver = new NewMessageReceiver(), new IntentFilter(Constant.NEW_MESSAGE_ACTION));
        IntentFilter filter = new IntentFilter();
//                说说消息接受
        filter.addAction(Constant.NEW_SHARE_MESSAGE_ACTION);
//                说说消息改变实时监听
        filter.addAction(Constant.NOTIFY_CHANGE_ACTION);
        registerReceiver(shareReceiver = new ShareMessageReceiver(), filter);
//                网络状态改变监听
        registerReceiver(netWorkReceiver = new NetWorkChangedReceiver(), new IntentFilter(Constant.NETWORK_CONNECTION_CHANGE));
        netWorkReceiver.registerListener(this);
//                单聊和系统推送消息监听
        PushMessageReceiver.registerListener(this);
    }

    private void initUserInfo() {
        user = UserManager.getInstance().getCurrentUser();
        if (user != null) {
            Glide.with(this).load(user.getAvatar()).centerCrop()
                    .into(avatar);
            nick.setText(user.getNick());
            updateMenuBg();
            if (user.getSignature() == null) {
                signature.setText("^_^1设置属于你的个性签名吧^_^");
            } else {
                signature.setText(user.getSignature());
            }
        }
    }


    public void updateMenuBg() {
        Glide.with(this).load(user.getWallPaper()).into(new SimpleTarget<GlideDrawable>() {
            @Override
            public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                container.setBackground(resource);
            }
        });
    }

    private void getWeatherInfo() {
        mWeatherInfoBean = new WeatherInfoBean();
        List<String> addressList = LocationManager.getInstance().getLocationList();
        if (addressList != null) {
            String cityName;
            if (addressList.size() == 5) {
                cityName = addressList.get(4);
            } else {
                cityName = addressList.get(4).substring(addressList.get(4).indexOf("省") + 1);
            }
            LogUtil.e("cityName" + cityName);
            mWeatherInfoBean.setCity(cityName);
            startSearchLiveWeather();
        } else {
            LocationManager.getInstance().registerLocationListener(this);
        }
    }


    private void bindPollService(int second) {
        LogUtil.e("启动定时拉取消息服务");
        Intent intent = new Intent(this, PollService.class);
        intent.putExtra("time", second);
        startService(intent);
//                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), second * 1000, PendingIntent.getService(getApplicationContext(), 0, new Intent(getApplicationContext(), PollService.class), PendingIntent.FLAG_UPDATE_CURRENT));
    }


    /**
     * 通知说说item的实时监听绑定
     *
     * @param objectId 说说id
     * @param isAdd    是否实时监听该说说
     */
    public void notifySharedMessageChanged(String objectId, boolean isAdd) {
        ((RecentFragment) mFragments[0]).notifySharedMessageChanged(objectId, isAdd);
    }


    public void initActionBar(String title) {
        ToolBarOption toolBarOption = new ToolBarOption();
        toolBarOption.setTitle(title);
        toolBarOption.setNeedNavigation(false);
        if (user != null) {
            toolBarOption.setAvatar(user.getAvatar());
        }
        setToolBar(toolBarOption);
    }


    @Override
    public void setToolBar(ToolBarOption option) {
        if (option.getAvatar() != null) {
            icon_1.setVisibility(View.VISIBLE);
            Glide.with(this).load(option.getAvatar()).into(icon_1);
        } else {
            icon_1.setVisibility(GONE);
        }

        if (option.getRightResId() != 0) {
            right_1.setVisibility(GONE);
            rightImage_1.setVisibility(View.VISIBLE);
            rightImage_1.setImageResource(option.getRightResId());
            rightImage_1.setOnClickListener(option.getRightListener());
        } else if (option.getRightText() != null) {
            right_1.setVisibility(View.VISIBLE);
            rightImage_1.setVisibility(GONE);
            right_1.setText(option.getRightText());
            right_1.setOnClickListener(option.getRightListener());
        } else {
            right_1.setVisibility(GONE);
            rightImage_1.setVisibility(GONE);
        }
        if (option.getTitle() != null) {
            title_1.setVisibility(View.VISIBLE);
            title_1.setText(option.getTitle());
        } else {
            title_1.setVisibility(GONE);
        }
        if (option.isNeedNavigation()) {
            back_1.setVisibility(View.VISIBLE);
            back_1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        } else {
            back_1.setVisibility(GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu_layout, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_item_search:
                Intent intent = new Intent(this, SearchActivity.class);
                startActivity(intent);
                break;
            case R.id.main_sub_item_add:
                ToastUtils.showShortToast("点击了添加好友");
                SearchFriendActivity.start(this);
                break;
            case R.id.main_sub_item_create:
                ToastUtils.showShortToast("点击了创建群");
                Intent selectIntent = new Intent(this, SelectedFriendsActivity.class);
                selectIntent.putExtra("from", "createGroup");
                startActivity(selectIntent);
                break;
            case R.id.main_sub_item_settings:
                ToastUtils.showShortToast("点击了设置");
                SettingsActivity.start(this, Constant.REQUEST_CODE_EDIT_USER_INFO);
                break;
            case R.id.main_sub_item_happy:
                HappyActivity.startActivity(this);
//                                Toast.makeText(this, "启动demo", Toast.LENGTH_SHORT).show();
//                                Intent demo = new Intent(this, DemoActivity.class);
//                                startActivity(demo);
                break;
            case R.id.main_sub_item_bg:
                ToastUtils.showShortToast("点击了背景");
                Intent wallPaperIntent = new Intent(this, WallPaperActivity.class);
                wallPaperIntent.putExtra("from", "wallpaper");
                startActivityForResult(wallPaperIntent, Constant.REQUEST_CODE_SELECT_WALLPAPER);
        }
        return true;
    }


    @Override
    public void OnNetWorkChanged(boolean isConnected, int type) {
        if (isConnected) {
//                        这里判断网络的连接类型
            if (type == ConnectivityManager.TYPE_WIFI) {
                LogUtil.e("wife类型的1");
                bindPollService(5);
            } else {
                LogUtil.e("非wifi类型");
                bindPollService(6);
            }
            net.setVisibility(GONE);
            if (mWeatherInfoBean == null) {
                getWeatherInfo();
            }


        } else {
            net.setVisibility(View.VISIBLE);
        }
    }


    private void startSearchLiveWeather() {
        WeatherSearchQuery query = new WeatherSearchQuery(mWeatherInfoBean.getCity(), WeatherSearchQuery.WEATHER_TYPE_LIVE);
        WeatherSearch weatherSearch = new WeatherSearch(this);
        weatherSearch.setQuery(query);
        weatherSearch.setOnWeatherSearchListener(new WeatherSearch.OnWeatherSearchListener() {
            @Override
            public void onWeatherLiveSearched(LocalWeatherLiveResult localWeatherLiveResult, int i) {
                LogUtil.e("查询得到的天气结果" + i);
                if (i == 1000) {
                    if (localWeatherLiveResult != null && localWeatherLiveResult.getLiveResult() != null) {
                        LocalWeatherLive localWeatherLive = localWeatherLiveResult.getLiveResult();
                        mWeatherInfoBean.setRealTime(localWeatherLive.getReportTime());
                        mWeatherInfoBean.setWeatherStatus(localWeatherLive.getWeather());
                        mWeatherInfoBean.setTemperature(localWeatherLive.getTemperature() + "°");
                        mWeatherInfoBean.setWind(localWeatherLive.getWindDirection() + "风       " + localWeatherLive
                                .getWindPower() + "级");
                        mWeatherInfoBean.setHumidity("湿度       " + localWeatherLive.getHumidity() + "%");
                        weatherCity.setText(mWeatherInfoBean.getCity());
                        weatherTemperature.setText(mWeatherInfoBean.getTemperature());
                    } else {
                        LogUtil.e("获取到的天气信息为空");
                    }
                } else {
                    LogUtil.e("获取到的天气信息失败" + i);
                }
            }

            @Override
            public void onWeatherForecastSearched(LocalWeatherForecastResult localWeatherForecastResult, int i) {
            }
        });
        weatherSearch.searchWeatherAsyn();
    }

    @Override
    public void onLocationChanged(List<String> addressList, double latitude, double longitude) {
        LogUtil.e("Main定位到啦啦啦");
        LocationManager.getInstance().unregisterLocationListener(this);
        String cityName = addressList.get(4).substring(addressList.get(4).indexOf("省") + 1);
        mWeatherInfoBean = new WeatherInfoBean();
        mWeatherInfoBean.setCity(cityName);
        startSearchLiveWeather();
    }

    @Override
    public void onLocationFailed(int errorId, String errorMsg) {
        LogUtil.e("Main定位失败" + errorMsg + errorId);
        List<String> addressList = LocationManager.getInstance().getLocationList();
        if (addressList != null) {
            LocationManager.getInstance().unregisterLocationListener(this);
            String cityName;
            if (addressList.size() == 5) {
                cityName = addressList.get(4);
            } else {
                cityName = addressList.get(4).substring(addressList.get(4).indexOf("省") + 1);
            }
            mWeatherInfoBean.setCity(cityName);
            startSearchLiveWeather();
        }
    }

    public void notifyContactAndRecent(ChatMessage chatMessage) {
        LogUtil.e("11222notifyContactAndRecent");
        ((RecentFragment) mFragments[0]).updateRecentData(chatMessage.getToId());
        notifyMenuUpdate();
        updateContactsData(chatMessage.getToId());
    }

    public void notifyMenuUpdate() {
        menuAdapter.notifyDataSetChanged();
    }


    private class NewMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constant.NEW_MESSAGE_ACTION)) {
                if (intent.getBooleanExtra("isLogout", false)) {
                    LogUtil.e("主界面正在关闭");
                    finish();
                    return;
                }
                LogUtil.e("1接受到广播发来的消息");
                if (intent.getStringExtra("from").equals("person")) {
                    List<ChatMessage> list;
                    list = (List<ChatMessage>) intent.getSerializableExtra(Constant.NEW_MESSAGE);
                    onProcessNewMessages(list);
                } else if (intent.getStringExtra("from").equals("group")) {
                    LogUtil.e("这里实时广播接收到实时群消息拉");
                    GroupChatMessage message = (GroupChatMessage) intent.getSerializableExtra(Constant.NEW_MESSAGE);
                    if (MsgManager.getInstance().saveRecentAndChatGroupMessage(message)) {
                        onNewGroupChatMessageCome(message);
                    }
                } else if (intent.getStringExtra("from").equals("groupTable")) {
                    List<GroupTableMessage> list;
                    list = (List<GroupTableMessage>) intent.getSerializableExtra(Constant.NEW_MESSAGE);
                    for (GroupTableMessage message :
                            list) {
                        LogUtil.e(message);
                        message.setReadStatus(Constant.READ_STATUS_READED);
                        message.setSendStatus(Constant.SEND_STATUS_SUCCESS);
                        MessageCacheManager.getInstance().addGroupTableMessage(message);
                        MsgManager.getInstance().updateGroupTableMessageReaded(message, null);
                        ChatDB.create().saveGroupTableMessage(message);
                        onGroupTableMessageCome(message);
                        MsgManager.getInstance().queryGroupChatMessage(message.getGroupId(), new FindListener<GroupChatMessage>() {
                            @Override
                            public void onSuccess(List<GroupChatMessage> list) {
                                LogUtil.e("拉去得到的最近的群聊消息如下");
                                for (GroupChatMessage groupChatMessage
                                        : list
                                        ) {
                                    LogUtil.e(groupChatMessage);
                                    groupChatMessage.setReadStatus(Constant.RECEIVE_UNREAD);
                                    if (MsgManager.getInstance().saveRecentAndChatGroupMessage(groupChatMessage)) {
                                        onNewGroupChatMessageCome(groupChatMessage);
                                    }
                                }
                            }

                            @Override
                            public void onError(int i, String s) {
                                LogUtil.e("拉取最近的群消息失败" + s + i);
                            }
                        });
                    }
                } else if (intent.getStringExtra("from").equals("table")) {
                    LogUtil.e("实时检测得到的群主的群结构更改消息");
                    GroupTableMessage message = (GroupTableMessage) intent.getSerializableExtra(Constant.NEW_MESSAGE);
                    ((RecentFragment) mFragments[0]).updateRecentData(message.getGroupId());
                    LogUtil.e(message);
                }
                abortBroadcast();
            }
        }
    }

    private void onProcessNewMessages(List<ChatMessage> list) {
        String tag;
        for (final ChatMessage chatMessage :
                list) {
            tag = chatMessage.getTag();
            LogUtil.e("tag：" + tag);
            chatMessage.setSendStatus(Constant.SEND_STATUS_SUCCESS);
            chatMessage.setReadStatus(Constant.RECEIVE_UNREAD);
            ChatNotificationManager.getInstance(this).sendChatMessageNotification(chatMessage, this);
            switch (tag) {
                case Constant.TAG_ADD_FRIEND:
                    if (MsgManager.getInstance().saveAndUpdateInvitationMsg(chatMessage) > 0) {
                        LogUtil.e("在服务器上成功检测到未读的邀请消息");
                        onAddFriendMessageCome(chatMessage);
                    } else {
                        LogUtil.e("保存从服务器上检测得来的邀请消息失败");
                    }
                    break;
                case Constant.TAG_AGREE:
                    UserManager.getInstance().addNewFriend(chatMessage.getBelongId(), UserManager.getInstance().getCurrentUserObjectId(), new AddFriendCallBackListener() {
                        @Override
                        public void onSuccess(User user) {
                            LogUtil.e("在服务器上成功检测到未读的同意消息并保存到数据库中成功");
                            UserCacheManager.getInstance().addContact(user);
                            MsgManager.getInstance().saveAndUploadReceiverMessage(true, chatMessage);
                            onAgreeMessageCome(chatMessage);
                        }

                        @Override
                        public void onFailed(BmobException e) {
                            LogUtil.e("保存从服务器上检测得来的同意消息失败");
                        }
                    });

                    break;
                case Constant.TAG_ASK_READ:
                    LogUtil.e("接收到的回执已读标签消息");
                    LogUtil.e(chatMessage);
                    MsgManager.getInstance().updateReadTagMsgReaded(chatMessage.getConversationId(), chatMessage.getCreateTime());
                    if (ChatDB.create().hasFriend(chatMessage.getBelongId()) && ChatDB.create().isBlackUser(chatMessage.getBelongId())) {
                        LogUtil.e("由于是黑名单，直接更新服务器上的已读回执消息为已读状态");
                        MsgManager.getInstance().updateMsgReaded(false, chatMessage.getConversationId(), chatMessage.getCreateTime());
                    } else {
                        if (MsgManager.getInstance().uploadAndUpdateChatMessageReadStatus(chatMessage, true) > 0) {
                            LogUtil.e("在服务器上成功检测到未读的已读回执消息");
                            onAskReadMessageCome(chatMessage);
                        } else {
                            LogUtil.e("保存从服务器上检测得来的回执已读消息失败");
                        }
                    }
                    break;
                default:
//                                                                        聊天消息这里要判断是否是黑名单用户
                    if (ChatDB.create().hasFriend(chatMessage.getBelongId()) && ChatDB.create().isBlackUser(chatMessage.getBelongId())) {
//                                                                                黑名单用户
                        LogUtil.e("黑名单用户，不接受消息");
                        MsgManager.getInstance().updateMsgReaded(false, chatMessage.getConversationId(), chatMessage.getCreateTime());
                        LogUtil.e("更新服务器上黑名单发来的消息为已读");
                    } else {
                        if (MsgManager.getInstance().saveAndUploadReceiverMessage(true, chatMessage)) {
                            LogUtil.e("在服务器上成功检测到未读的聊天消息");
                            onNewChatMessageCome(chatMessage);
                        } else {
                            LogUtil.e("在服务器上检测未读的聊天消息失败");
                        }
                    }
                    break;
            }
        }
    }


    @Override
    protected void onResume() {
        LogUtil.e("111222MainActivity：onResume");
        super.onResume();
//                PushMessageReceiver.registerListener(this);
    }


//        这里说明下，由于头基类封装的头部布局与侧滑发生冲突，这里我们直接在主布局上写入头部布局,所以这里弃用基类的头部布局

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
        return R.layout.activity_main;
    }


    @Override
    protected void onPause() {
        LogUtil.e("MainActivity：onPause");
        super.onPause();
//                PushMessageReceiver.unRegisterListener(this);
    }


    @Override
    public void onDrag(View view, float delta) {
        ViewHelper.setAlpha(bg, delta);
        ViewHelper.setAlpha(icon_1, (1 - delta));
    }


    @Override
    public void onCloseMenu() {
//                当侧滑完全关闭的时候调用
        if (ViewHelper.getAlpha(bg) != 0) {
            ViewHelper.setAlpha(bg, 0);
        }
        if (ViewHelper.getAlpha(icon_1) != 1) {
            ViewHelper.setAlpha(icon_1, 1);
        }
    }

    @Override
    public void onOpenMenu() {
        if (ViewHelper.getAlpha(bg) != 1) {
            ViewHelper.setAlpha(bg, 1);
        }
        if (ViewHelper.getAlpha(icon_1) != 0) {
            ViewHelper.setAlpha(icon_1, 0);
        }
        //                当侧滑完全打开的时候调用
    }

    public void closeMenu() {
        if (container.getCurrentState() == DragLayout.DRAG_STATE_OPEN) {
            container.closeMenu();
        }
    }

    @Override
    public void onNewChatMessageCome(ChatMessage message) {
        LogUtil.e("211新消息啦啦啦");
        LogUtil.e(message);
        if (mFragments[0].isAdded()) {

            ((RecentFragment) mFragments[0]).updateRecentData(message.getBelongId());
            if (currentFragment instanceof RecentFragment) {
            } else {
                notifyMenuUpdate();
            }
        } else {
            addOrReplaceFragment(mFragments[0], R.id.fl_content_container);
        }
    }

    @Override
    public void onNewGroupChatMessageCome(GroupChatMessage message) {
        ChatNotificationManager.getInstance(this).sendGroupMessageNotification(message, this);
        if (mFragments[0].isAdded()) {
            ((RecentFragment) mFragments[0]).updateRecentData(message.getGroupId());
            if (currentFragment instanceof RecentFragment) {
            } else {
                notifyMenuUpdate();
            }
        } else {
            addOrReplaceFragment(mFragments[0], R.id.fl_content_container);
        }

//                LogUtil.e("群消息到主界面了");
//                if (currentFragment instanceof RecentFragment) {
//                        ((RecentFragment) currentFragment).updateRecentData(message);
//                } else {
//                        menuAdapter.notifyDataSetChanged();
//                }
    }

    @Override
    public void onAskReadMessageCome(ChatMessage chatMessage) {
        LogUtil.e("onAskReadMessageCome");
        notifyMenuUpdate();
//                不处理
    }

    @Override
    public void onNetWorkChanged(boolean isConnected) {
    }

    @Override
    public void onAddFriendMessageCome(ChatMessage chatMessage) {
        LogUtil.e("onAddFriendMessageCome");
        if (mFragments[2].isAdded()) {
            ((InvitationFragment) mFragments[2]).updateInvitationData(chatMessage);
            if (currentFragment instanceof InvitationFragment) {

            } else {
                notifyMenuUpdate();
            }
        } else {
            addOrReplaceFragment(mFragments[2]);
        }
    }

    @Override
    public void onAgreeMessageCome(ChatMessage chatMessage) {
        LogUtil.e("onAgreeMessageCome");
        LogUtil.e("同意消息格式");
        LogUtil.e(chatMessage);
        onNewChatMessageCome(chatMessage);
        updateContactsData(chatMessage.getBelongId());
    }

    private void updateContactsData(String id) {
        if (mFragments[1].isAdded()) {
            ((ContactsFragment) mFragments[1]).updateContactsData(id);
            if (currentFragment instanceof ContactsFragment) {

            } else {
                notifyMenuUpdate();
            }
        } else {
            currentPosition = 1;
            addOrReplaceFragment(mFragments[1]);
        }
        ((RecentFragment) mFragments[0]).notifyUserAdd(id);

    }

    @Override
    public void onOffline() {
//                下线通知
        ToastUtils.showShortToast("你的帐号在其他地方登录，被迫下线");
        UserManager.getInstance().logout();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onGroupTableMessageCome(GroupTableMessage message) {
        LogUtil.e("onGroupTableMessageCome");
        LogUtil.e("群结构消息到主界面拉");
//                这里得到的群结构消息仅仅是自己的群结构消息
        if (mFragments.length > 0) {
            ((RecentFragment) mFragments[0]).notifyGroupTableMsgCome(message.getGroupId());
        }
    }

    public void openMenu() {
        if (container.getCurrentState() == DragLayout.DRAG_STATE_CLOSE) {
            container.openMenu();
        }
    }

    /**
     * 点击左上角的图标的回调
     *
     * @param view view
     */
    @Override
    public void onClick(View view) {
        LogUtil.e("侧滑关闭");
        switch (view.getId()) {
            case R.id.drag_container:
                openMenu();
                break;
            case R.id.rl_menu_head_layout:
//                                点击进入个人信息界面之前，先实时监听个人信息界面中的说说
                Intent intent = new Intent(this, UserDetailActivity.class);
                intent.putExtra("from", "me");
                intent.putExtra("uid", UserManager.getInstance().getCurrentUserObjectId());
                startActivityForResult(intent, Constant.REQUEST_CODE_EDIT_USER_INFO);
                break;
            case R.id.tv_main_net:
                Intent settingIntent = new Intent();
                settingIntent.setAction(Settings.ACTION_WIFI_SETTINGS);
                startActivity(settingIntent);
                break;
            case R.id.ll_menu_bottom_container:
                Intent weatherIntent = new Intent(this, WeatherInfoActivity.class);
                if (mWeatherInfoBean != null) {
                    weatherIntent.putExtra("WeatherInfo", mWeatherInfoBean);
                }
                startActivityForResult(weatherIntent, Constant.REQUEST_CODE_WEATHER_INFO);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case Constant.REQUEST_CODE_EDIT_USER_INFO:
                    user = (User) data.getSerializableExtra("user");
                    nick.setText(user.getNick());
                    signature.setText(user.getSignature());
                    Glide.with(this).load(user.getAvatar()).diskCacheStrategy(DiskCacheStrategy.ALL).into(icon_1);
                    ToastUtils.showShortToast("保存个人信息成功");
                    Glide.with(this).load(user.getAvatar()).diskCacheStrategy(DiskCacheStrategy.ALL).into(avatar);
                    break;
                case Constant.REQUEST_CODE_WEATHER_INFO:
                    LogUtil.e("返回天气数据");
                    WeatherInfoBean weatherInfoBean = (WeatherInfoBean) data.getSerializableExtra("WeatherInfo");
                    if (weatherInfoBean != null) {
                        mWeatherInfoBean = weatherInfoBean;
                        weatherCity.setText(mWeatherInfoBean.getCity());
                        weatherTemperature.setText(mWeatherInfoBean.getTemperature());
                    }
                    break;
                case Constant.REQUEST_CODE_SELECT_WALLPAPER:
                    updateMenuBg();
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        LogUtil.e("定时服务取消");
        PushMessageReceiver.unRegisterListener(this);
        Intent intent = new Intent(this, PollService.class);
        stopService(intent);
//                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//                alarmManager.cancel(PendingIntent.getService(getApplicationContext(), 0, new Intent(getApplicationContext(), PollService.class), PendingIntent.FLAG_UPDATE_CURRENT));
        LogUtil.e("用户数据取消拉拉");

//                把定时检测的服务给取消
        PushMessageReceiver.unRegisterListener(this);
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        if (shareReceiver != null) {
            unregisterReceiver(shareReceiver);
            shareReceiver = null;
        }

        if (netWorkReceiver != null) {
            unregisterReceiver(netWorkReceiver);
            netWorkReceiver.unregisterListener(this);
            netWorkReceiver = null;
        }
        LogUtil.e("用户数据取消拉拉，这次应该行了吧");
        super.onDestroy();
    }

    private class ShareMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constant.NEW_SHARE_MESSAGE_ACTION)) {
                String from = intent.getStringExtra("from");
                if (from != null && from.equals("share")) {
                    LogUtil.e("11拉取得到的说说消息来了");
                    List<SharedMessage> list = (List<SharedMessage>) intent.getSerializableExtra(Constant.NEW_MESSAGE);
                    for (SharedMessage shareMessage :
                            list) {
                        ChatDB.create().saveSharedMessage(shareMessage);
                    }


                    if (mFragments[3].isAdded()) {
                        ((ShareMessageFragment) mFragments[3]).updateAllShareMessages(list, true);
                        if (currentFragment instanceof ShareMessageFragment) {
                        } else {
                            notifyMenuUpdate();
                        }
                    }

                }
            } else if (intent.getAction().equals(Constant.NOTIFY_CHANGE_ACTION)) {
                List<String> list = intent.getStringArrayListExtra("id");
                boolean isAdd = intent.getBooleanExtra("isAdd", false);
                if (list != null && list.size() > 0) {
                    for (String id :
                            list) {
                        notifySharedMessageChanged(id, isAdd);
                    }
                }
            }
        }
    }

    private void notifyUrlSharedMessageAdd(SharedMessage sharedMessage) {
        if (currentFragment instanceof ShareMessageFragment) {
            ((ShareMessageFragment) currentFragment).notifyUrlShareMessageCome(sharedMessage);
        } else {
            if (!mFragments[3].isAdded()) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("url_share_message", sharedMessage);
                mFragments[3].setArguments(bundle);
            }
            addOrReplaceFragment(mFragments[3]);
        }
    }


    private long mExitTime = 0;

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - mExitTime > 2000) {
            ToastUtils.showShortToast("再按一次退出程序");
            mExitTime = System.currentTimeMillis();
        } else {
            super.onBackPressed();
        }
    }
}


