package chen.testchat.view;

import android.content.Context;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import org.pointstone.cugappplat.baseadapter.BaseWrappedViewHolder;
import org.pointstone.cugappplat.util.LogUtil;

import chen.testchat.R;
import chen.testchat.adapter.ImageFoldersAdapter;
import chen.testchat.listener.OnBaseItemClickListener;

/**
 * 项目名称:    TestChat
 * 创建人:        陈锦军
 * 创建时间:    2016/11/30      9:52
 * QQ:             1981367757
 */
public abstract class ImageFolderPopupWindow extends PopupWindow {

        //        private View mask;
//        private View margin;
        private RecyclerView display;
        //        private int marginSize;
        private View view;

//        public void setMarginSize(int marginSize) {
//                this.marginSize = marginSize;
//        }

        public ImageFolderPopupWindow(final Context context, ImageFoldersAdapter adapter) {
                super(context);
                view = LayoutInflater.from(context).inflate(R.layout.image_folder_popup_layout, null);
                display = (RecyclerView) view.findViewById(R.id.rcv_image_folder_item_display);
                display.setLayoutManager(new LinearLayoutManager(context));
                display.setItemAnimator(new DefaultItemAnimator());
                display.addItemDecoration(new ListViewDecoration(context));
                display.addOnItemTouchListener(new OnBaseItemClickListener() {
                        @Override
                        protected void onItemClick(BaseWrappedViewHolder baseWrappedViewHolder, int id, View view, int position) {
                                LogUtil.e("id:" + id + "position:" + position);
                                ImageFolderPopupWindow.this.onItemClick(view, position, id);
                        }
                });
                display.setAdapter(adapter);
                setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
                setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                setContentView(view);
                setAnimationStyle(R.style.ImageFolderPopupWindow);
                view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                int maxHeight = view.getHeight() * 5 / 8;
                                int listHeight = display.getHeight();
                                LinearLayout.LayoutParams layoutParam = (LinearLayout.LayoutParams) display.getLayoutParams();
                                layoutParam.height = Math.min(maxHeight, listHeight);
                                display.setLayoutParams(layoutParam);
                        }
                });
                view.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                                if (isShowing()) {
                                        dismiss();
                                }
                                return true;
                        }
                });
        }

//        private void startEnterAnimation() {
//                ObjectAnimator alpha = ObjectAnimator.ofFloat(mask, "alpha", 0, 1);
//                ObjectAnimator translationY = ObjectAnimator.ofFloat(display, "translationY", display.getHeight(), 0);
//                AnimatorSet animatorSet = new AnimatorSet();
//                animatorSet.playTogether(alpha, translationY);
//                animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
//                animatorSet.start();
//        }


//        private void startExitAnimation() {
//                ObjectAnimator alpha = ObjectAnimator.ofFloat(mask, "alpha", 1, 0);
//                ObjectAnimator translationY = ObjectAnimator.ofFloat(display, "translationY", 0, display.getHeight());
//                AnimatorSet animatorSet = new AnimatorSet();
//                animatorSet.playTogether(alpha, translationY);
//                animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
//                animatorSet.addListener(new Animator.AnimatorListener() {
//                        @Override
//                        public void onAnimationStart(Animator animation) {
//                                view.setVisibility(View.VISIBLE);
//                        }
//
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                                ImageFolderPopupWindow.super.dismiss();
//                        }
//
//                        @Override
//                        public void onAnimationCancel(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animator animation) {
//
//                        }
//                });
//                animatorSet.start();
//
//        }


        public abstract void onItemClick(View view, int position, long id);


        public RecyclerView getRecyclerView() {
                return display;
        }

}
