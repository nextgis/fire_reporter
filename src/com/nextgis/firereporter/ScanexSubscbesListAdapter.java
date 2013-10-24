package com.nextgis.firereporter;

import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ScanexSubscbesListAdapter extends BaseAdapter {
	private Context mContext;
	private List <ScanexSubscriptionItem> mListSubscibeInfo;

	public ScanexSubscbesListAdapter(Context c, List<ScanexSubscriptionItem> ListSubscibeInfo) {
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
		ScanexSubscriptionItem entry = mListSubscibeInfo.get(position);
		if(entry == null){
			return -1;
		}
		return entry.GetId();
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		// get the selected entry
		ScanexSubscriptionItem entry = mListSubscibeInfo.get(position);

		// inflate new layout if null
		if(convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(mContext);
			convertView = inflater.inflate(R.layout.subsciberowlayout, null);
		}

		// set data to display
		TextView tvText1 = (TextView)convertView.findViewById(R.id.tvText1);

		tvText1.setText(entry.GetTitle());
		if(entry.HasNews()){
			tvText1.setTypeface(null, Typeface.BOLD);
		}
		else{
			tvText1.setTypeface(null, Typeface.NORMAL);
		}

		return convertView;
	}
}
