package chen.testchat.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.pointstone.cugappplat.base.cusotomview.RoundAngleImageView;
import org.pointstone.cugappplat.base.cusotomview.ToolBarOption;
import org.pointstone.cugappplat.util.ToastUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import chen.testchat.CustomApplication;
import chen.testchat.R;
import chen.testchat.base.CommonImageLoader;
import chen.testchat.base.Constant;
import chen.testchat.bean.User;
import chen.testchat.db.ChatDB;
import chen.testchat.manager.UserManager;
import chen.testchat.util.LogUtil;
import chen.testchat.util.PhotoUtil;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadFileListener;

/**
 * 项目名称:    TestChat
 * 创建人:        陈锦军
 * 创建时间:    2016/12/26      16:49
 * QQ:             1981367757
 */

public class EditUserInfoActivity extends SlideBaseActivity implements View.OnClickListener {
        private RelativeLayout avatarLayout, nickLayout, sexLayout, birthLayout, phoneLayout,
                emailLayout, signatureLayout, addressLayout;
        private RoundAngleImageView avatar;
        private TextView nick, sex, birth, phone, email, signature, address;
        private User mUser;



        @Override
        protected boolean isNeedHeadLayout()
        {
                return true;
        }

        @Override
        protected boolean isNeedEmptyLayout() {
                return false;
        }

        @Override
        protected int getContentLayout() {
                return R.layout.activity_edit_info;
        }


        @Override
        public void initView() {
                avatarLayout = (RelativeLayout) findViewById(R.id.rl_edit_user_info_avatar);
                nickLayout = (RelativeLayout) findViewById(R.id.rl_edit_user_info_nick);
                sexLayout = (RelativeLayout) findViewById(R.id.rl_edit_user_info_sex);
                birthLayout = (RelativeLayout) findViewById(R.id.rl_edit_user_info_birth);
                phoneLayout = (RelativeLayout) findViewById(R.id.rl_edit_user_info_phone);
                emailLayout = (RelativeLayout) findViewById(R.id.rl_edit_user_info_email);
                signatureLayout = (RelativeLayout) findViewById(R.id.rl_edit_user_info_signature);
                addressLayout = (RelativeLayout) findViewById(R.id.rl_edit_user_info_address);
                avatar = (RoundAngleImageView) findViewById(R.id.riv_edit_user_info_avatar);
                nick = (TextView) findViewById(R.id.tv_edit_user_info_nick);
                sex = (TextView) findViewById(R.id.tv_edit_user_info_sex);
                birth = (TextView) findViewById(R.id.tv_edit_user_info_birth);
                phone = (TextView) findViewById(R.id.tv_edit_user_info_phone);
                email = (TextView) findViewById(R.id.tv_edit_user_info_email);
                signature = (TextView) findViewById(R.id.tv_edit_user_info_signature);
                address = (TextView) findViewById(R.id.tv_edit_user_info_address);
                avatarLayout.setOnClickListener(this);
                nickLayout.setOnClickListener(this);
                sexLayout.setOnClickListener(this);
                birthLayout.setOnClickListener(this);
                phoneLayout.setOnClickListener(this);
                emailLayout.setOnClickListener(this);
                signatureLayout.setOnClickListener(this);
                addressLayout.setOnClickListener(this);
        }




        @Override
        public void initData() {
                mUser = UserManager.getInstance().getCurrentUser();
                nick.setText(mUser.getNick());
                birth.setText(mUser.getBirthDay());
                phone.setText(mUser.getMobilePhoneNumber());
                sex.setText(mUser.isSex() ? "男" : "女");
                email.setText(mUser.getEmail());
                signature.setText(mUser.getSignature());
                address.setText(mUser.getAddress());
                Glide.with(this).load(mUser.getAvatar()).into(avatar);
                ToolBarOption toolBarOption = new ToolBarOption();
                toolBarOption.setAvatar(null);
                toolBarOption.setRightResId(R.drawable.ic_file_upload_blue_grey_900_24dp);
                toolBarOption.setTitle("编辑个人资料");
                toolBarOption.setRightListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                                if (mUser.getAvatar() == null) {
                                        ToastUtils.showShortToast("请设置个人头像拉^_^");
                                        return;
                                }
                                Intent intent = new Intent();
                                intent.putExtra("user", mUser);
                                setResult(Activity.RESULT_OK, intent);
                                finish();
                        }
                });
                getBack().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                                if (mUser.getAvatar() == null) {
                                        ToastUtils.showShortToast("请设置个人头像拉^_^");
                                        return;
                                }
                                finish();
                        }
                });
                toolBarOption.setNeedNavigation(true);
                setToolBar(toolBarOption);
        }

        @Override

        public void onClick(View v) {
                Intent intent = new Intent();
                switch (v.getId()) {
                        case R.id.rl_edit_user_info_avatar:
                                CommonImageLoader.getInstance().initStanderConfig(1);
                                intent.setClass(this, SelectedPictureActivity.class);
                                startActivityForResult(intent, Constant.REQUEST_CODE_SELECT_PICTURE);
                                break;
                        case R.id.rl_edit_user_info_nick:
                                intent.putExtra("from", "nick");
                                intent.putExtra("message", mUser.getNick());
                                intent.setClass(this, EditUserInfoDetailActivity.class);
                                startActivityForResult(intent, Constant.REQUEST_CODE_NICK);
                                break;
                        case R.id.rl_edit_user_info_sex:
                                intent.putExtra("from", "gender");
                                intent.putExtra("message", mUser.isSex() ? "男" : "女");
                                intent.setClass(this, EditUserInfoDetailActivity.class);
                                startActivityForResult(intent, Constant.REQUEST_CODE_SEX);
                                break;
                        case R.id.rl_edit_user_info_birth:
                                intent.putExtra("from", "birth");
                                intent.putExtra("message", mUser.getBirthDay());
                                intent.setClass(this, EditUserInfoDetailActivity.class);
                                startActivityForResult(intent, Constant.REQUEST_CODE_BIRTH);
                                break;
                        case R.id.rl_edit_user_info_phone:
                                intent.putExtra("from", "phone");
                                intent.putExtra("message", mUser.getMobilePhoneNumber());
                                intent.setClass(this, EditUserInfoDetailActivity.class);
                                startActivityForResult(intent, Constant.REQUEST_CODE_PHONE);
                                break;
                        case R.id.rl_edit_user_info_email:
                                intent.putExtra("from", "email");
                                intent.putExtra("message", mUser.getEmail());
                                intent.setClass(this, EditUserInfoDetailActivity.class);
                                startActivityForResult(intent, Constant.REQUEST_CODE_EMAIL);
                                break;
                        case R.id.rl_edit_user_info_signature:
                                intent.putExtra("from", "signature");
                                intent.putExtra("message", mUser.getSignature());
                                intent.setClass(this, EditUserInfoDetailActivity.class);
                                startActivityForResult(intent, Constant.REQUEST_CODE_SIGNATURE);
                                break;
                        case R.id.rl_edit_user_info_address:
                                intent.putExtra("from", "address");
                                intent.putExtra("message", mUser.getAddress());
                                intent.setClass(this, EditUserInfoDetailActivity.class);
                                startActivityForResult(intent, Constant.REQUEST_CODE_ADDRESS);
                }
        }


        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
                super.onActivityResult(requestCode, resultCode, data);
                if (resultCode == Activity.RESULT_OK) {
                        String message = null;
                        if (data != null) {
                                message = data.getStringExtra("message");
                        }
                        switch (requestCode) {
                                case Constant.REQUEST_CODE_SELECT_PICTURE:
//                                        这里进入编辑图片界面
                                        if (CommonImageLoader.getInstance().getSelectedImages().get(0) != null) {
                                                String path = CommonImageLoader.getInstance().getSelectedImages().get(0).getPath();
                                                cropPhoto(path);
                                        }
                                        break;
                                case Constant.REQUEST_CODE_CROP:
                                        LogUtil.e("裁剪完成");
                                        try {
                                                showLoadDialog("正在上传头像，请稍候........");
                                                final BmobFile bmobFile = new BmobFile(new File(new URI(PhotoUtil.buildUri(this).toString())));
                                                bmobFile.uploadblock(CustomApplication.getInstance(), new UploadFileListener() {
                                                        @Override
                                                        public void onSuccess() {
//                                                                这里更新用户信息头像
                                                                UserManager.getInstance().updateUserInfo("avatar", bmobFile.getFileUrl(CustomApplication.getInstance()), new UpdateListener() {
                                                                        @Override
                                                                        public void onSuccess() {
                                                                                dismissLoadDialog();
                                                                                LogUtil.e("更新用户头像成功");
                                                                                Glide.with(EditUserInfoActivity.this).load(bmobFile.getFileUrl(CustomApplication.getInstance())).diskCacheStrategy(DiskCacheStrategy.ALL).into(avatar);
                                                                                mUser.setAvatar(bmobFile.getFileUrl(EditUserInfoActivity.this));
//                                                                                更新数据库中消息的头像
                                                                                ChatDB.create().updateMessageAvatar(UserManager.getInstance().getCurrentUserObjectId(),bmobFile.getFileUrl(EditUserInfoActivity.this));
                                                                        }

                                                                        @Override
                                                                        public void onFailure(int i, String s) {
                                                                                dismissLoadDialog();
                                                                                LogUtil.e("更新用户头像失败" + s + i);
                                                                        }
                                                                });
                                                        }

                                                        @Override
                                                        public void onFailure(int i, String s) {
                                                                dismissLoadDialog();
                                                                LogUtil.e("加载失败");
                                                        }
                                                });
                                        } catch (URISyntaxException e) {
                                                e.printStackTrace();
                                        }
                                        break;
                                case Constant.REQUEST_CODE_SEX:
                                        sex.setText(message);
                                        if (message != null) {
                                                mUser.setSex(message.equals("男"));
                                        }
                                        break;
                                case Constant.REQUEST_CODE_BIRTH:
                                        birth.setText(message);
                                        mUser.setBirthDay(message);
                                        break;
                                case Constant.REQUEST_CODE_SIGNATURE:
                                        signature.setText(message);
                                        mUser.setSignature(message);
                                        break;
                                case Constant.REQUEST_CODE_EMAIL:
                                        email.setText(message);
                                        mUser.setEmail(message);
                                        break;
                                case Constant.REQUEST_CODE_NICK:
                                        nick.setText(message);
                                        mUser.setNick(message);
                                        break;
                                case Constant.REQUEST_CODE_ADDRESS:
                                        address.setText(message);
                                        mUser.setAddress(message);
                                case Constant.REQUEST_CODE_PHONE:
                                        phone.setText(message);
                                        mUser.setMobilePhoneNumber(message);
                                default:
                                        break;
                        }
                }
        }

        private void cropPhoto(String path) {
                Uri uri = Uri.fromFile(new File(path));
                Intent cropIntent = new Intent("com.android.camera.action.CROP");
                cropIntent.setDataAndType(uri, "image/*");
                cropIntent.putExtra("crop", "true");
                cropIntent.putExtra("aspectX", 1);
                cropIntent.putExtra("aspectY", 1);
                cropIntent.putExtra("outputX", 200);
                cropIntent.putExtra("outputY", 200);
                cropIntent.putExtra("return-data", false);
                cropIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
                Uri cropUri = PhotoUtil.buildUri(this);
                cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, cropUri);

                if (cropIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(cropIntent, Constant.REQUEST_CODE_CROP);
                }
        }

        @Override
        protected void onDestroy() {
                super.onDestroy();
                CommonImageLoader.getInstance().clearAllData();
        }


        @Override
        public void finish() {
                LogUtil.e("editUserInfo_finish");
                Intent intent = new Intent();
                intent.putExtra("user", mUser);
                setResult(Activity.RESULT_OK, intent);
                super.finish();
        }
}
