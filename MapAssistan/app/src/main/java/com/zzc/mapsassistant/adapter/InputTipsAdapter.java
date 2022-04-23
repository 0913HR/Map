package com.zzc.mapsassistant.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.amap.api.services.help.Tip;
import com.zzc.mapsassistant.R;

import java.util.List;

/**
 * 输入提示adapter，展示item名称和地址
 */
public class InputTipsAdapter extends BaseAdapter {

    private Context mContext;
    private List<Tip> mListTips;

    public InputTipsAdapter(Context context, List<Tip> tipList) {
        mContext = context;
        mListTips = tipList;
    }

    @Override
    public int getCount() {
        if (mListTips != null) {
            return mListTips.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        if (mListTips != null) {
            return mListTips.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        Holder holder;
        if (view == null) {
            holder = new Holder();
            view = LayoutInflater.from(mContext).inflate(R.layout.adapter_inputtips, null);
            holder.mName = view.findViewById(R.id.name);
            holder.mAddress = view.findViewById(R.id.adress);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }
        if (mListTips == null) {
            return view;
        }

        holder.mName.setText(mListTips.get(position).getName());
        String address = mListTips.get(position).getAddress();
        if (address == null || address.equals("")) {
            holder.mAddress.setVisibility(View.GONE);
        } else {
            holder.mAddress.setVisibility(View.VISIBLE);
            holder.mAddress.setText(address);
        }
        return view;
    }

    class Holder {
        TextView mName;
        TextView mAddress;
    }
}
