package org.pointstone.cugappplat.base.basemvp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.trello.rxlifecycle.LifecycleTransformer;
import com.trello.rxlifecycle.components.support.RxFragment;

import org.common.R;
import org.pointstone.cugappplat.base.cusotomview.ToolBarOption;
import org.pointstone.cugappplat.baseadapter.baseloadview.EmptyLayout;
import org.pointstone.cugappplat.util.LogUtil;
import org.pointstone.cugappplat.util.ToastUtils;

import java.util.List;

import static android.view.View.GONE;

/**
 * 项目名称:    Cugappplat
 * 创建人:        陈锦军
 * 创建时间:    2017/4/3      14:24
 * QQ:             1981367757
 */

public abstract class BaseFragment extends RxFragment implements BaseView {

        /**
         * 采用懒加载
         */
        private View root;
        private EmptyLayout mEmptyLayout;
        private boolean hasInit = false;
        private RelativeLayout headerLayout;

        private ImageView icon;
        private TextView right;
        private TextView title;
        private ImageView rightImage;
        protected ImageView back;


        protected abstract boolean isNeedHeadLayout();

        protected abstract boolean isNeedEmptyLayout();


        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
                LogUtil.e("第一次onCreateView");
                if (root == null) {
                        LogUtil.e("这里初始化root");
                        if (isNeedHeadLayout()) {
                                LinearLayout linearLayout = new LinearLayout(getActivity());
                                linearLayout.setOrientation(LinearLayout.VERTICAL);
                                headerLayout = (RelativeLayout) LayoutInflater.from(getActivity()).inflate(R.layout.header_layout, null);
                                linearLayout.addView(headerLayout);
                                linearLayout.addView(LayoutInflater.from(getActivity()).inflate(getContentLayout(), null));
                                if (isNeedEmptyLayout()) {
                                        FrameLayout frameLayout = new FrameLayout(getActivity());
                                        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                        mEmptyLayout = new EmptyLayout(getActivity());
                                        mEmptyLayout.setVisibility(GONE);
                                        frameLayout.addView(linearLayout);
                                        frameLayout.addView(mEmptyLayout);
                                        root = frameLayout;
                                } else {
                                        root = linearLayout;
                                }
                        } else {
                                if (isNeedEmptyLayout()) {
                                        LogUtil.e("这里没有头部，只有空布局");
                                        FrameLayout frameLayout = new FrameLayout(getActivity());
                                        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                        mEmptyLayout = new EmptyLayout(getActivity());
                                        mEmptyLayout.setVisibility(GONE);
                                        frameLayout.addView(LayoutInflater.from(getActivity()).inflate(getContentLayout(), null));
                                        frameLayout.addView(mEmptyLayout);
                                        root = frameLayout;
                                } else {
                                        root = inflater.inflate(getContentLayout(), container, false);
                                }
                        }
//                        mEmptyLayout = (EmptyLayout) root.findViewById(R.id.fl_empty_layout);
                        initBaseView();
                        initData();
                }
                if (root.getParent() != null) {
                        ((ViewGroup) root.getParent()).removeView(root);
                }
                return root;
        }

        private void initBaseView() {
                if (isNeedHeadLayout()) {
                        LogUtil.e("这里初始化头部布局");
                        icon = (ImageView) headerLayout.findViewById(R.id.riv_header_layout_icon);
                        title = (TextView) headerLayout.findViewById(R.id.tv_header_layout_title);
                        right = (TextView) headerLayout.findViewById(R.id.tv_header_layout_right);
                        back = (ImageView) headerLayout.findViewById(R.id.iv_header_layout_back);
                        rightImage = (ImageView) headerLayout.findViewById(R.id.iv_header_layout_right);
                        rightImage.setVisibility(View.GONE);
                        right.setVisibility(View.VISIBLE);
                }
                initView();
        }


        protected View findViewById(int id) {
                if (root != null) {
                        return root.findViewById(id);
                }
                return null;
        }


        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
                super.onActivityCreated(savedInstanceState);
                if (root != null && getUserVisibleHint() && !hasInit) {
                        hasInit = true;
                        updateView();
                }
        }


        /**
         * 视图真正可见的时候才调用
         */

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
                super.setUserVisibleHint(isVisibleToUser);
                if (root != null && isVisibleToUser && !hasInit) {
                        hasInit = true;
                        updateView();
                }
        }

        protected abstract int getContentLayout();


        protected abstract void initView();

        protected abstract void initData();

        protected abstract void updateView();


        public void setToolBar(ToolBarOption option) {
                if (!isNeedHeadLayout()) {
                        return;
                }
                if (option.getAvatar() != null) {
                        icon.setVisibility(View.VISIBLE);
                        Glide.with(this).load(option.getAvatar()).into(icon);
                } else {
                        icon.setVisibility(GONE);
                }

                if (option.getRightResId() != 0) {
                        right.setVisibility(GONE);
                        rightImage.setVisibility(View.VISIBLE);
                        rightImage.setImageResource(option.getRightResId());
                        rightImage.setOnClickListener(option.getRightListener());
                } else if (option.getRightText() != null) {
                        right.setVisibility(View.VISIBLE);
                        rightImage.setVisibility(GONE);
                        right.setText(option.getRightText());
                        right.setOnClickListener(option.getRightListener());
                } else {
                        right.setVisibility(GONE);
                        rightImage.setVisibility(GONE);
                }
                if (option.getTitle() != null) {
                        title.setVisibility(View.VISIBLE);
                        title.setText(option.getTitle());
                } else {
                        title.setVisibility(GONE);
                }
                if (option.isNeedNavigation()) {
                        back.setVisibility(View.VISIBLE);
                        back.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                        getActivity().finish();
                                }
                        });
                } else {
                        back.setVisibility(GONE);
                }

        }


        @Override
        public void showLoading(String loadingMsg) {
                LogUtil.e("这里没有头部，只有空布局showLoading");
                if (mEmptyLayout != null) {
                        LogUtil.e("showLoading不为空");
                        mEmptyLayout.setCurrentStatus(EmptyLayout.STATUS_LOADING);
                } else {
                        if (!getActivity().isFinishing()) {
                                if (getActivity() instanceof BaseActivity) {
                                        ((BaseActivity) getActivity()).showLoadDialog(loadingMsg);
                                }
                        }
                }
        }

        @Override
        public void hideLoading() {
                if (mEmptyLayout != null) {
                        mEmptyLayout.setCurrentStatus(EmptyLayout.STATUS_HIDE);
                } else {
                        if (!getActivity().isFinishing()) {
                                if (getActivity() instanceof BaseActivity) {
                                        ((BaseActivity) getActivity()).dismissLoadDialog();
                                }
                        }
                }
        }


        protected void hideBaseDialog() {
                if (getActivity() instanceof BaseActivity && !getActivity().isFinishing()) {
                        ((BaseActivity) getActivity()).dismissBaseDialog();
                }
        }


        protected void showChooseDialog(String title, List<String> list, AdapterView.OnItemClickListener listener) {
                if (getActivity() instanceof BaseActivity && !getActivity().isFinishing()) {
                        ((BaseActivity) getActivity()).showChooseDialog(title, list, listener);
                }
        }

        @Override
        public void showError(String errorMsg, EmptyLayout.OnRetryListener listener) {
                if (mEmptyLayout != null) {
                        mEmptyLayout.setCurrentStatus(EmptyLayout.STATUS_NO_NET);
                        if (listener != null) {
                                mEmptyLayout.setOnRetryListener(listener);
                        }
                } else {
                        ToastUtils.showShortToast(errorMsg);
                }
        }

        @Override
        public <T> LifecycleTransformer<T> bindLife() {
                return bindToLifecycle();
        }

        public ImageView getIcon() {
                return icon;
        }
}
