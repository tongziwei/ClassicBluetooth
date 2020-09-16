package example.com.classicbluetooth.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import example.com.classicbluetooth.R;

/**
 * Created by clara.tong on 2020/9/14
 */
public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.MsgViewHolder> {
    private List<Msg> mMsgList;

    static class MsgViewHolder extends RecyclerView.ViewHolder{
        LinearLayout mLlMsgLeft;
        LinearLayout mLlMsgRight;
        TextView mTvMsgLeft;
        TextView mTvMsgRight;

        public MsgViewHolder(View view) {
            super(view);
            mLlMsgLeft = (LinearLayout)view.findViewById(R.id.ll_msg_left);
            mLlMsgRight = (LinearLayout)view.findViewById(R.id.ll_msg_right);
            mTvMsgLeft = (TextView)view.findViewById(R.id.tv_msg_left);
            mTvMsgRight = (TextView)view.findViewById(R.id.tv_msg_right);
        }
    }

    public MsgAdapter(List<Msg> mMsgList) {
        this.mMsgList = mMsgList;
    }

    @Override
    public void onBindViewHolder(MsgViewHolder holder, int position) {
        Msg msg = mMsgList.get(position);
        if(msg.getType() == Msg.TYPE_RECEIVE){
            holder.mLlMsgLeft.setVisibility(View.VISIBLE);
            holder.mLlMsgRight.setVisibility(View.GONE);
            holder.mTvMsgLeft.setText(msg.getContent());
        }else if(msg.getType() == Msg.TYPE_SEND ){
            holder.mLlMsgLeft.setVisibility(View.GONE);
            holder.mLlMsgRight.setVisibility(View.VISIBLE);
            holder.mTvMsgRight.setText(msg.getContent());
        }
    }


    @Override
    public MsgViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_item,parent,false);
        return new MsgViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mMsgList.size();
    }
}
