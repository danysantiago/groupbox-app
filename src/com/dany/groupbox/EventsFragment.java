package com.dany.groupbox;

import java.io.IOException;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class EventsFragment extends Fragment {

	private Context context;

	private Button eventCreateButton;

	private ListView eventListView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_events, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		context = getActivity();

		eventCreateButton = (Button) view.findViewById(R.id.add_event_button);
		eventCreateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, EventActivity.class);
				startActivity(intent);
			}
		});

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			eventCreateButton.setVisibility(View.GONE);
		}

		eventListView = (ListView) view.findViewById(R.id.events_listview);
		eventListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
				Log.d("DEBUG", "Clicked event with id: " + v.getTag());

			}
		});

		eventListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View v, int position, long id) {

				int event_id = (Integer) v.getTag();
				Log.d("DEBUG", "Long Clicked event with id: " + event_id);

				cancelEventRequest(event_id);

				return true;
			}
		});

	}

	private void cancelEventRequest(final int eventID){
		DBAdapter db = new DBAdapter(context);

		EventHolder event = db.querySingleEvent(eventID);

		Calendar currCalendar = Calendar.getInstance();

		Calendar startCalendar = Calendar.getInstance();
		startCalendar.setTimeInMillis(event.getStart()*1000);

		Calendar endCalendar = Calendar.getInstance();
		endCalendar.setTimeInMillis(event.getEnd()*1000);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(event.getName());
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				cancelEvent(eventID);
			}
		});
		builder.setNegativeButton("No", null);

		if(currCalendar.before(startCalendar) && currCalendar.before(endCalendar)){ //Event hasen't started
			builder.setMessage("Are you sure you wish to cancel this event?");
			builder.create().show();
		} else if (currCalendar.before(endCalendar)){ //Event is started and going
			builder.setMessage("Are you sure you wish to cancel this on-going event?");
			builder.create().show();
		} else { //Event has ended
			cancelEvent(eventID);

		}

	}

	private void cancelEvent(int eventID){
		DBAdapter db = new DBAdapter(context);
		EventHolder event = db.querySingleEvent(eventID);
		db.removeEvent(eventID);

		Intent intent = new Intent(context, MonitorService.class);
		intent.setData(Uri.fromParts("groupbox-event", "//1/event?id="+eventID, null));
		PendingIntent pIntent = PendingIntent.getService(context, 1, intent, 0);

		AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Activity.ALARM_SERVICE);
		alarmManager.cancel(pIntent);

		context.stopService(intent);

		SharedPreferences prefs = context.getSharedPreferences("DBAuth", Context.MODE_PRIVATE);
		long uid = prefs.getLong("uid", -1);

		EventCancelTask task = new EventCancelTask(0);
		task.execute(event.getServerID(), ""+uid); //UID Marron, fix or something... or not

		onResume();
	}

	@Override
	public void onResume() {
		super.onResume();

		DBAdapter db = new DBAdapter(getActivity());
		Cursor c = db.queryAllEvents();
		MyEventListViewAdapter adapter = new MyEventListViewAdapter(getActivity(), c);
		eventListView.setAdapter(adapter);

		if(!adapter.isEmpty()){
			TextView noEventTextView = (TextView) getView().findViewById(R.id.no_events_textView);
			noEventTextView.setVisibility(View.GONE);
		}

	}

	private class MyEventListViewAdapter extends CursorAdapter{

		public MyEventListViewAdapter(Context context, Cursor c) {
			super(context, c, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView nameTextView = (TextView) view.findViewById(R.id.name_textView);
			nameTextView.setText(cursor.getString(cursor.getColumnIndex(DBAdapter.COL_NAME)));

			TextView folderTextView = (TextView) view.findViewById(R.id.folder_textView);
			folderTextView.setText("Folder: " + cursor.getString(cursor.getColumnIndex(DBAdapter.COL_FOLDER)));

			long start = cursor.getLong(cursor.getColumnIndex(DBAdapter.COL_START_TIME));
			long end = cursor.getLong(cursor.getColumnIndex(DBAdapter.COL_END_TIME));

			Calendar startCalendar = Calendar.getInstance();
			startCalendar.setTimeInMillis(start*1000);

			Calendar endCalendar = Calendar.getInstance();
			endCalendar.setTimeInMillis(end*1000);

			String time;

			if(startCalendar.get(Calendar.YEAR) == endCalendar.get(Calendar.YEAR) &&
					startCalendar.get(Calendar.MONTH) == endCalendar.get(Calendar.MONTH) &&
					startCalendar.get(Calendar.DAY_OF_MONTH) == endCalendar.get(Calendar.DAY_OF_MONTH)){
				time = Utils.getDateString(startCalendar) + "   " + Utils.getTimeString(startCalendar) + 
						" - " + Utils.getTimeString(endCalendar);	
			} else {
				time = Utils.getDateString(startCalendar) + " " + Utils.getTimeString(startCalendar) + 
						" - " +  Utils.getDateString(endCalendar) + " " + Utils.getTimeString(endCalendar);			

			}
			TextView timeTextView = (TextView) view.findViewById(R.id.time_textView);
			timeTextView.setText(time);

			Calendar currCalendar = Calendar.getInstance();
			if(currCalendar.after(startCalendar) && currCalendar.before(endCalendar)){
				view.setBackgroundColor(getResources().getColor(R.color.green_event_color));
			} else if(currCalendar.after(endCalendar)){
				view.setBackgroundColor(getResources().getColor(R.color.gray_event_color));
				nameTextView.setPaintFlags(nameTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			}

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(context);
			View v = inflater.inflate(R.layout.event_list_item, parent, false);
			v.setTag(cursor.getInt(cursor.getColumnIndex("_id")));
			bindView(v, context, cursor);
			return v;
		}

	}


	private class EventCancelTask extends AsyncTask<String, Void, String[]>{

		private DefaultHttpClient client;
		private boolean success;

		private int tries;

		public EventCancelTask(int tryNum){
			tries = tryNum;
		}

		@Override
		protected void onPreExecute() {
			int timeout = 10000;
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeout);
			HttpConnectionParams.setSoTimeout(httpParameters, timeout);

			client = new DefaultHttpClient(httpParameters);
		}

		@Override
		protected String[] doInBackground(String... params) {

			try {
				HttpGet get = new HttpGet(Server.BASE_URL + Server.CANCEL_EVENT_ROUTE + "?eventID=" + params[0] + "&uid=" + params[1]);

				HttpResponse response = client.execute(get);
				int statusCode = response.getStatusLine().getStatusCode();

				if(statusCode == HttpStatus.SC_OK){
					String fullRes = EntityUtils.toString(response.getEntity());
					if(fullRes.equals("0")){
						Log.d("DEBUG", "Event cancel cool!");
						success = true;
					} else {
						success = false;
					}
				} else {
					Log.d("BackendEventLog","HttpError performing cancel-event, code: " + statusCode);
					success = false;
				}
			} catch (ClientProtocolException e) {
				Log.d("BackendEventLog", "Error canceling event POST", e);
				success = false;
			} catch (IOException e) {
				Log.d("BackendEventLog", "Error canceling event POST", e);
				success = false;
			}
			return params;
		}

		@Override
		protected void onPostExecute(String[] result) {
			super.onPostExecute(result);

			if(success){

			} else {
				//Try again
				if(tries < 3){
					Log.d("DEBUG", "Trying cancel again, tryNum: " + (tries+1));
					EventCancelTask task = new EventCancelTask(tries+1);
					task.execute(result[0], result[1]);
				}
			}
		}


	}

}
