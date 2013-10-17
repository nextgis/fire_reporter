package com.nextgis.firereporter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SubscbesListAdapter extends BaseAdapter {
	private Context mContext;
	private List <SubscriptionItem> mListSubscibeInfo;

	public SubscbesListAdapter(Context c, List<SubscriptionItem> ListSubscibeInfo) {
		mContext = c;
		mListSubscibeInfo = ListSubscibeInfo;
	}
	
	public int getCount() {
		return mListSubscibeInfo.size();
	}

	public Object getItem(int position) {
		return mListSubscibeInfo.get(position);
	}

	public long getItemId(int position) {
		SubscriptionItem entry = mListSubscibeInfo.get(position);
		if(entry == null){
			return -1;
		}
		return entry.GetId();
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		// get the selected entry
		SubscriptionItem entry = mListSubscibeInfo.get(position);

		// reference to convertView
		View v = convertView;

		// inflate new layout if null
		if(v == null) {
			LayoutInflater inflater = LayoutInflater.from(mContext);
			v = inflater.inflate(R.layout.subsciberowlayout, null);
		}

		// set data to display
		TextView tvText1 = (TextView)v.findViewById(R.id.tvText1);

		tvText1.setText(entry.GetTitle());

		return v;
	}
}
