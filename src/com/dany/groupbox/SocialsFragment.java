package com.dany.groupbox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SocialsFragment extends ListFragment {

	private static final String[] SOCIALS_LIST = {
		"Twitter",
		"Facebook"
	};

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);


		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {

				boolean isSocialActive = (Boolean) v.getTag();

				if(isSocialActive){
					
					Toast.makeText(getActivity(), "Social Network is already activated.", Toast.LENGTH_SHORT).show();
					
				} else {

					String socialName = SOCIALS_LIST[position].toLowerCase();

					Intent intent = new Intent(getActivity(), SocialsAuthActivity.class);
					intent.putExtra("socialName", socialName);
					startActivity(intent);

				}

			}
		});
	}
	
	

	@Override
	public void onResume() {
		super.onResume();
		
		setListAdapter(new MySocialListViewAdapter(getActivity()));

	}



	private class MySocialListViewAdapter extends ArrayAdapter<String>{

		private Context context;

		public MySocialListViewAdapter(Context context) {
			super(context, R.layout.social_list_item, SOCIALS_LIST);

			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;

			String name = getItem(position);

			LayoutInflater inflater = ( (Activity) context).getLayoutInflater();
			row = inflater.inflate(R.layout.social_list_item, parent, false);

			SharedPreferences prefs = context.getSharedPreferences("SocialAuths", Context.MODE_PRIVATE);

			TextView nameTextView = (TextView) row.findViewById(R.id.social_item_textView);
			nameTextView.setText(name);

			ImageView imageButton = (ImageView) row.findViewById(R.id.social_item_imageView);
			boolean isSocialActive = Utils.Socials.isSocialActive(name.toLowerCase(), prefs.getInt("activeSocialsInt", 0));
			String logoResName = null;
			if(isSocialActive)
				logoResName = name.toLowerCase()+"_logo";
			else
				logoResName = name.toLowerCase()+"_bw_logo";
			int logoResId = context.getResources().getIdentifier(logoResName, "drawable", context.getPackageName());
			imageButton.setImageResource(logoResId);

			row.setTag(isSocialActive);

			return row;
		}



	}

}
