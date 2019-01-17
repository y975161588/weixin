package chen.testchat.ui;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.pointstone.cugappplat.base.cusotomview.ToolBarOption;
import org.pointstone.cugappplat.baseadapter.BaseWrappedViewHolder;
import org.pointstone.cugappplat.util.ToastUtils;

import java.util.List;

import chen.testchat.R;
import chen.testchat.adapter.SearchFriendAdapter;
import chen.testchat.bean.User;
import chen.testchat.listener.OnBaseItemChildClickListener;
import chen.testchat.manager.UserManager;
import chen.testchat.util.LogUtil;
import chen.testchat.view.ListViewDecoration;
import cn.bmob.v3.listener.FindListener;

/**
 * 项目名称:    HappyChat
 * 创建人:        陈锦军
 * 创建时间:    2016/9/25      12:18
 * QQ:             1981367757
 */
public class SearchFriendActivity extends SlideBaseActivity implements View.OnClickListener {
        private EditText input;
        //        private SearchFriendAdapter adapter;
        private RecyclerView display;
        private Button search;
        private SearchFriendAdapter mAdapter;



        @Override
        protected boolean isNeedHeadLayout() {
                return true;
        }

        @Override
        protected boolean isNeedEmptyLayout() {
                return false;
        }

        @Override
        protected int getContentLayout() {
                return R.layout.activity_search_friend;
        }

        @Override
        public void initView() {
                input = (EditText) findViewById(R.id.et_search_friend_input);
                search = (Button) findViewById(R.id.btn_search_friend);
                display = (RecyclerView) findViewById(R.id.swrc_search_friend_display);
                display.setLayoutManager(new LinearLayoutManager(this));
                display.setItemAnimator(new DefaultItemAnimator());
                display.addItemDecoration(new ListViewDecoration(this));
                display.addOnItemTouchListener(new OnBaseItemChildClickListener() {
                        @Override
                        protected void onItemChildClick(BaseWrappedViewHolder baseWrappedViewHolder, int id, View view, int position) {
                                Intent intent = new Intent(SearchFriendActivity.this, UserInfoActivity.class);
                                User user = mAdapter.getData(position);
                                intent.putExtra("uid", user.getObjectId());
                                intent.putExtra("user", user);
                                startActivity(intent);
                                finish();
                        }
                });
                search.setOnClickListener(this);

        }


        @Override
        public void initData() {
//                display.setAdapter(adapter);
                mAdapter = new SearchFriendAdapter(null, R.layout.search_friend_item);
                display.setAdapter(mAdapter);
                initActionBar();
        }

        private void initActionBar() {
                ToolBarOption toolBarOption = new ToolBarOption();
                toolBarOption.setAvatar(null);
                toolBarOption.setNeedNavigation(true);
                toolBarOption.setTitle("查找好友");
                setToolBar(toolBarOption);
        }

        private void searchUsers() {
                showLoadDialog("正在搜索，请稍候.......");
                if (TextUtils.isEmpty(input.getText().toString().trim())) {
                        dismissLoadDialog();
                        ToastUtils.showShortToast("请输入用户名进行查询!");
                        return;
                }
                UserManager.getInstance().queryUsers(input.getText().toString().trim(), new FindListener<User>() {
                                @Override
                                public void onSuccess(List<User> list) {
                                        dismissLoadDialog();
                                        if (list != null && list.size() > 0) {
                                                LogUtil.e("查询用户成功：个数为" + list.size());
                                                LogUtil.e(list.size() + "大小");
                                                mAdapter.clearData();
                                                mAdapter.addData(list);
//                                                adapter.setData(list);
                                        }
                                }

                                @Override
                                public void onError(int i, String s) {
                                        LogUtil.e("查询出错!" + s + i);
                                }
                        }
                );
        }

        @Override
        public void onClick(View v) {
                switch (v.getId()) {
                        case R.id.btn_search_friend:
                                searchUsers();
                                break;
                }
        }


        public static void start(Activity activity) {
                Intent intent = new Intent(activity, SearchFriendActivity.class);
                activity.startActivity(intent);
        }
}
