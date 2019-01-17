package chen.testchat.adapter;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.pointstone.cugappplat.baseadapter.BaseMultipleWrappedAdapter;

import java.util.ArrayList;
import java.util.List;

import chen.testchat.R;
import chen.testchat.adapter.viewholder.BaseChatHolder;
import chen.testchat.adapter.viewholder.ReceiveImageHolder;
import chen.testchat.adapter.viewholder.ReceiveLocationHolder;
import chen.testchat.adapter.viewholder.ReceiveTextHolder;
import chen.testchat.adapter.viewholder.ReceiveVoiceHolder;
import chen.testchat.adapter.viewholder.SendImageHolder;
import chen.testchat.adapter.viewholder.SendLocationHolder;
import chen.testchat.adapter.viewholder.SendTextHolder;
import chen.testchat.adapter.viewholder.SendVoiceHolder;
import chen.testchat.bean.BaseMessage;
import chen.testchat.util.LogUtil;

/**
 * 项目名称:    TestChat
 * 创建人:        陈锦军
 * 创建时间:    2017/5/24      17:44
 * QQ:             1981367757
 */

public class ChatMessageAdapter extends BaseMultipleWrappedAdapter<BaseMessage, BaseChatHolder> {
        public static final int TYPE_SEND_TEXT = 0;
        public static final int TYPE_RECEIVER_TEXT = 1;
        public static final int TYPE_SEND_IMAGE = 2;
        public static final int TYPE_RECEIVER_IMAGE = 3;
        public static final int TYPE_SEND_VOICE = 4;
        public static final int TYPE_RECEIVER_VOICE = 5;
        public static final int TYPE_SEND_LOCATION = 6;
        public static final int TYPE_RECEIVER_LOCATION = 7;


        //        5秒没回应就显示时间
        private static final long TIME_INTERVAL = 5 * 60 * 1000;

        public ChatMessageAdapter(List<BaseMessage> data, int layoutId) {
                super(data, layoutId);
        }

        public ChatMessageAdapter() {
                this(null, 0);
        }


        public interface OnItemClickListener {
                void onPictureClick(View view, String contentUrl, int position);

                void onAvatarClick(View view, int position, boolean isRight);

                void onMessageClick(View view, int position);

                void onItemResendClick(View view, BaseMessage chatMessage, int position);
        }


        private OnItemClickListener mOnItemClickListener;


        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
                mOnItemClickListener = onItemClickListener;
        }

        @Override
        protected SparseArray<Integer> getLayoutIdMap() {
                SparseArray<Integer> sparseArray = new SparseArray<>();
                sparseArray.put(TYPE_SEND_TEXT, R.layout.chat_send_text_item);
                sparseArray.put(TYPE_SEND_IMAGE, R.layout.chat_send_image_item);
                sparseArray.put(TYPE_SEND_VOICE, R.layout.chat_send_voice_item);
                sparseArray.put(TYPE_SEND_LOCATION, R.layout.chat_send_location_item);
                sparseArray.put(TYPE_RECEIVER_TEXT, R.layout.chat_receive_text_item);
                sparseArray.put(TYPE_RECEIVER_IMAGE, R.layout.chat_receive_image_item);
                sparseArray.put(TYPE_RECEIVER_LOCATION, R.layout.chat_receive_location_item);
                sparseArray.put(TYPE_RECEIVER_VOICE, R.layout.chat_receive_voice_item);
                return sparseArray;
        }


        @Override
        public BaseChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
                if (viewType == TYPE_SEND_TEXT) {
                        return new SendTextHolder(layoutInflater.inflate(getLayoutIds().get(viewType), parent, false));
                } else if (viewType == TYPE_SEND_IMAGE) {
                        return new SendImageHolder(layoutInflater.inflate(getLayoutIds().get(viewType), parent, false));
                } else if (viewType == TYPE_SEND_LOCATION) {
                        return new SendLocationHolder(layoutInflater.inflate(getLayoutIds().get(viewType), parent, false));

                } else if (viewType == TYPE_SEND_VOICE) {
                        return new SendVoiceHolder(layoutInflater.inflate(getLayoutIds().get(viewType), parent, false));
                } else if (viewType == TYPE_RECEIVER_TEXT) {
                        return new ReceiveTextHolder(layoutInflater.inflate(getLayoutIds().get(viewType), parent, false));

                } else if (viewType == TYPE_RECEIVER_IMAGE) {
                        return new ReceiveImageHolder(layoutInflater.inflate(getLayoutIds().get(viewType), parent, false));

                } else if (viewType == TYPE_RECEIVER_LOCATION) {
                        return new ReceiveLocationHolder(layoutInflater.inflate(getLayoutIds().get(viewType), parent, false));


                } else if (viewType == TYPE_RECEIVER_VOICE) {
                        return new ReceiveVoiceHolder(layoutInflater.inflate(getLayoutIds().get(viewType), parent, false));


                }
                return new SendTextHolder(layoutInflater.inflate(getLayoutIds().get(viewType), parent, false));
        }

        @Override
        protected void convert(BaseChatHolder holder, BaseMessage data) {
                int position = holder.getAdapterPosition();
                holder.bindData(data, mOnItemClickListener, shouldShowTime(position));
        }

        private boolean shouldShowTime(int position) {
                return position == 0 || Long.valueOf(data.get(position).getCreateTime()) - Long.valueOf(data.get(position - 1).getCreateTime()) > TIME_INTERVAL;
        }

//        @Override
//        public void addData(BaseMessage newData) {
//                if (data.contains(newData)) {
//                        int index = data.indexOf(newData);
//                        data.set(index, newData);
//                        notifyDataSetChanged();
//                }else {
//                        super.addData(newData);
//                }
//        }


        @Override
        public void addData(int position, BaseMessage newData) {
                if (data.contains(newData)) {
                        int index = data.indexOf(newData);
                        data.set(index, newData);
                        notifyDataSetChanged();
                } else {
                        super.addData(position,newData);
                }
        }

        @Override
        public void addData(int position, List<BaseMessage> newData) {
                LogUtil.e("添加数据12345678910");
                if (newData == null || newData.size() == 0) {
                        return;
                }
                List<BaseMessage> temp=new ArrayList<>();
                List<BaseMessage> copyData=new ArrayList<>(data);
                for (BaseMessage message :
                        newData) {
                        for (BaseMessage item :
                                copyData) {
                                if (message.equals(item)) {
                                        temp.add(message);
                                }
                        }
                }
                if (temp.size() > 0) {
                        LogUtil.e("大于0");
                        int index=0;
                        for (BaseMessage message :
                                temp) {
                                index=data.indexOf(message);
                                data.set(index,message);
                                newData.remove(message);
                        }
                }
                super.addData(position, newData);
        }
}
