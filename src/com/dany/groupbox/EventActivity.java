package com.dany.groupbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class EventActivity extends FragmentActivity {

	protected static final int FOLDER_PICK_REQUEST_CODE = 1;
	protected static final int SOCIAL_APIS_PICK_REQUEST_CODE = 2;

	Button doneButton;

	EditText eventNameEditText;
	Button fromDateButton;
	Button fromTimeButton;
	Button toDateButton;
	Button toTimeButton;
	Button photoFolderButton;
	Button socialAPIsButton;
	EditText messageEditText;

	int from_year, from_month, from_day, from_hour, from_minute;
	int to_year, to_month, to_day, to_hour, to_minute;

	private SocialsPicker socialPicker;
	private ArrayList<String> selectedSocialsList;

	private Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_event_create);

		context = this;

		initialCalendarSetup();

		doneButton = (Button) findViewById(R.id.done_button);

		eventNameEditText = (EditText) findViewById(R.id.event_name_editText);
		fromDateButton = (Button) findViewById(R.id.from_date_button);
		fromTimeButton = (Button) findViewById(R.id.from_time_button);
		toDateButton = (Button) findViewById(R.id.to_date_button);
		toTimeButton = (Button) findViewById(R.id.to_time_button);
		photoFolderButton = (Button) findViewById(R.id.photo_folder_button);
		socialAPIsButton = (Button) findViewById(R.id.social_apis_button);
		messageEditText = (EditText) findViewById(R.id.message_editText);

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH){

		} else {
			//Disable done button
			doneButton.setVisibility(View.GONE);

			// Inflate a "Done/Discard" custom action bar view.
			LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
			final View customActionBarView = inflater.inflate(R.layout.actionbar_custom_view_done_discard, null);

			customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					createEvent();
				}
			});

			customActionBarView.findViewById(R.id.actionbar_discard).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// "Discard"
					finish();
				}
			});


			// Show the custom action bar view and hide the normal Home icon and title.
			final ActionBar actionBar = getActionBar();
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
			actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

		}

		doneButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				createEvent();
			}

		});

		fromDateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				DialogFragment newFragment = new DatePickerFragment();
				Bundle arg = new Bundle();
				arg.putBoolean("isFrom", true);
				arg.putInt("year", from_year);
				arg.putInt("month", from_month);
				arg.putInt("day", from_day);
				newFragment.setArguments(arg);
				newFragment.show(getSupportFragmentManager(), "datePicker");

			}
		});

		fromTimeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				DialogFragment newFragment = new TimePickerFragment();
				Bundle arg = new Bundle();
				arg.putBoolean("isFrom", true);
				arg.putInt("hour", from_hour);
				arg.putInt("minute", from_minute);
				newFragment.setArguments(arg);
				newFragment.show(getSupportFragmentManager(), "timePicker");
			}
		});

		toDateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				DialogFragment newFragment = new DatePickerFragment();
				Bundle arg = new Bundle();
				arg.putBoolean("isFrom", false);
				arg.putInt("year", to_year);
				arg.putInt("month", to_month);
				arg.putInt("day", to_day);
				newFragment.setArguments(arg);
				newFragment.show(getSupportFragmentManager(), "datePicker");

			}
		});

		toTimeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				DialogFragment newFragment = new TimePickerFragment();
				Bundle arg = new Bundle();
				arg.putBoolean("isFrom", false);
				arg.putInt("hour", to_hour);
				arg.putInt("minute", to_minute);
				newFragment.setArguments(arg);
				newFragment.show(getSupportFragmentManager(), "timePicker");
			}
		});

		photoFolderButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), FolderPicker.class);
				startActivityForResult(intent, FOLDER_PICK_REQUEST_CODE);

			}
		});

		socialAPIsButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(socialPicker == null){
					socialPicker = new SocialsPicker(context, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							selectedSocialsList = socialPicker.getSelectedSocials();

							if(selectedSocialsList.size() > 0){
								StringBuilder sb = new StringBuilder();
								for(int i = 0; i < selectedSocialsList.size(); i++){
									sb.append(selectedSocialsList.get(i));
									if(i < selectedSocialsList.size()-1)
										sb.append(", ");
								}

								socialAPIsButton.setText(sb.toString());
							} else {
								socialAPIsButton.setText("");
							}
						}

					});
				}
				if(!socialPicker.show()){
					Toast.makeText(context, "No social network activated.", Toast.LENGTH_SHORT).show();
				}
			}
		});

	}



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == FOLDER_PICK_REQUEST_CODE){
			if(resultCode == Activity.RESULT_OK){
				photoFolderButton.setText(data.getExtras().getString("folderPath"));
			} else {

			}
		}
	}



	private void createEvent() {

		//Get Event fields
		String name = eventNameEditText.getText().toString();
		if(name.isEmpty()){
			Toast.makeText(this, "Event Name is Empty.", Toast.LENGTH_SHORT).show();
			return;
		}

		Calendar startCalendar = Calendar.getInstance();
		startCalendar.set(from_year, from_month, from_day, from_hour, from_minute, 0);
		long startTime = startCalendar.getTimeInMillis() / 1000L;

		Calendar endCalendar = Calendar.getInstance();
		endCalendar.set(to_year, to_month, to_day, to_hour, to_minute, 0);
		long endTime = endCalendar.getTimeInMillis() / 1000L;

		String folderPath = photoFolderButton.getText().toString();
		if(folderPath.isEmpty()){
			Toast.makeText(this, "Please Select a Dropbox Folder.", Toast.LENGTH_SHORT).show();
			return;
		}

		if(selectedSocialsList == null)
			selectedSocialsList = new ArrayList<String>();
		int socials = Utils.Socials.activeSocialListToInt(selectedSocialsList);

		String message = messageEditText.getText().toString();

		//Create Event Holder
		EventHolder event = new EventHolder(name, startTime, endTime, folderPath, socials, message);

		EventHolder conflictEvent = findConflict(event);

		if(conflictEvent == null){

			//Insert into local DB
			DBAdapter db = new DBAdapter(this);
			int eventID = db.insertEvent(event);
			event.setEventID(eventID);

			//Create Intent and set Alarm
			Intent intent = new Intent(this, MonitorService.class);
			intent.setData(Uri.fromParts("groupbox-event", "//1/event?id="+eventID, null));
			intent.putExtra("eventName", name);
			intent.putExtra("folderPath", folderPath);
			intent.putExtra("endTime", endCalendar.getTimeInMillis());
			PendingIntent pIntent = PendingIntent.getService(this, 1, intent, 0);

			AlarmManager alarmManager = (AlarmManager) this.getSystemService(ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP, startCalendar.getTimeInMillis(), pIntent);

			//Send event to Backend Server
			EventCreateTask task = new EventCreateTask(event);
			task.execute();

			finish();

		} else {
			View root = getLayoutInflater().inflate(R.layout.dialog_event_conflict, null);
			
			TextView tv1 = (TextView) root.findViewById(R.id.conflict_textView1);
			tv1.setText("\'" + event.getName() + "\' conflicts with \'" + conflictEvent.getName() + "\'");

			TextView tv2 = (TextView) root.findViewById(R.id.conflict_textView2);
			tv2.setText("Events cannot overlap, please verify the time and date of your event. "  + event.getName() + " must not overlap:");
			
			TextView tv3 = (TextView) root.findViewById(R.id.conflict_textView3);
			tv3.setText(Utils.getDateTimeString(conflictEvent.getStartCalendar()));
			
			TextView tv4 = (TextView) root.findViewById(R.id.conflict_textView4);
			tv4.setText(Utils.getDateTimeString(conflictEvent.getEndCalendar()));

			AlertDialog.Builder dialogBulder = new AlertDialog.Builder(this);
			dialogBulder.setTitle("Events Conflict");
			dialogBulder.setView(root);
			dialogBulder.setPositiveButton("Ok", null);
			dialogBulder.create().show();
		}

	}


	private EventHolder findConflict(EventHolder event) {
		DBAdapter db = new DBAdapter(this);
		EventHolder conflictEvent = null;

		Cursor c = db.queryAllEvents();

		c.moveToFirst();

		while(!c.isAfterLast()){
			long startTime = c.getLong(c.getColumnIndex(DBAdapter.COL_START_TIME));
			long endTime = c.getLong(c.getColumnIndex(DBAdapter.COL_END_TIME));

			Calendar startCalendar = Calendar.getInstance();
			startCalendar.setTimeInMillis(startTime*1000);

			Calendar endCalendar = Calendar.getInstance();
			endCalendar.setTimeInMillis(endTime*1000);

			if( (event.getStartCalendar().before(startCalendar) && event.getEndCalendar().before(startCalendar))
					|| (event.getStartCalendar().after(endCalendar) && event.getEndCalendar().after(endCalendar)) ){
				//Do nothing, no conflict
			} else {
				conflictEvent = new EventHolder(
						c.getString(c.getColumnIndex(DBAdapter.COL_NAME)),
						startTime,
						endTime,
						c.getString(c.getColumnIndex(DBAdapter.COL_FOLDER)),
						c.getInt(c.getColumnIndex(DBAdapter.COL_SOCIAL_API)), 
						c.getString(c.getColumnIndex(DBAdapter.COL_MESSAGE)));
				break;
			}
			
			c.moveToNext();
		}

		c.close();

		return conflictEvent;
	}



	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if(hasFocus){

			//Handle to time being before from time
			Calendar currCalendar = Calendar.getInstance();
			
			Calendar toCalendar = Calendar.getInstance();
			toCalendar.set(to_year, to_month, to_day, to_hour, to_minute);

			Calendar fromCalendar = Calendar.getInstance();
			fromCalendar.set(from_year, from_month, from_day, from_hour, from_minute);
			
			if(fromCalendar.before(currCalendar)){
				currCalendar.add(Calendar.HOUR_OF_DAY, 1);
				currCalendar.set(Calendar.MINUTE, 0);
				
				from_year = currCalendar.get(Calendar.YEAR);
				from_month = currCalendar.get(Calendar.MONTH);
				from_day = currCalendar.get(Calendar.DAY_OF_MONTH);
				from_hour = currCalendar.get(Calendar.HOUR_OF_DAY);
				from_minute = currCalendar.get(Calendar.MINUTE);
			}

			if(toCalendar.before(fromCalendar)){
				to_year = from_year;
				to_month = from_month;
				to_day = from_day;
				to_hour = from_hour;
				to_minute = from_minute;
			}

			setFromEditTexts();
			setToEditTexts();
		}
	}

	private class EventCreateTask extends AsyncTask<Void, Void, Void>{

		private DefaultHttpClient client;

		private EventHolder event;

		private boolean success;

		public EventCreateTask(EventHolder event){
			this.event = event;
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
		protected Void doInBackground(Void... params) {

			SharedPreferences prefs = getSharedPreferences("DBAuth", Context.MODE_PRIVATE);

			try {

				String[] selectedSocials = Utils.Socials.intToActiveSocialList(event.getSocials());

				//Create JSON String
				JSONObject registerJson = new JSONObject();		
				registerJson.put("uid", prefs.getLong("uid", -1));
				registerJson.put("name", event.getName());
				registerJson.put("start", event.getStart());
				registerJson.put("end", event.getEnd());
				registerJson.put("folder", event.getFolder());
				registerJson.put("message", event.getMessage());
				for(int i = 0; i < selectedSocials.length; i++)
					registerJson.put(selectedSocials[i], true);

				//HTTP Post
				HttpPost post = new HttpPost(Server.BASE_URL + Server.CREATE_EVENT_ROUTE);
				StringEntity postParam = new StringEntity(registerJson.toString());
				post.addHeader("content-type", "application/json");
				post.setEntity(postParam);

				HttpResponse response = client.execute(post);
				int statusCode = response.getStatusLine().getStatusCode();

				if(statusCode == HttpStatus.SC_OK){
					String fullRes = EntityUtils.toString(response.getEntity());
					JSONObject jsonRes = new JSONObject(fullRes);
					String serverEventID = jsonRes.getString("eventID");
					DBAdapter db = new DBAdapter(context);
					db.setServerIDToEvent(event.getEventID(), serverEventID);
					success = true;
					Log.d("DEBUG", "Event creation cool!");
				} else {
					Log.d("BackendEventLog","HttpError performing event-create, code: " + statusCode);
					success = false;
				}

			} catch (JSONException e) {
				Log.d("JSONLog", "Error creating JSON object", e);
			} catch (ClientProtocolException e) {
				success = false;
				Log.d("BackendEventLog", "Error creating event POST", e);
			} catch (IOException e) {
				success = false;
				Log.d("BackendEventLog", "Error creating event POST", e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if(success){

			} else {
				DBAdapter db = new DBAdapter(context);
				db.removeEvent(event.getEventID());

				Intent intent = new Intent(context, MonitorService.class);
				intent.setData(Uri.fromParts("groupbox-event", "//1/event?id="+event.getEventID(), null));
				PendingIntent pIntent = PendingIntent.getService(context, 1, intent, 0);

				AlarmManager alarmManager = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
				alarmManager.cancel(pIntent);

				context.stopService(intent);

				Toast.makeText(context, "Could not register \'" + event.getName() + "\' event with server, please try creating event again.", Toast.LENGTH_LONG).show();
			}
		}

	}

	private void setFromEditTexts() {
		Calendar c = Calendar.getInstance();
		c.set(from_year, from_month, from_day, from_hour, from_minute);

		String date = Utils.getDateString(c);
		String time = Utils.getTimeString(c);

		fromDateButton.setText(date);
		fromTimeButton.setText(time);
	}

	private void setToEditTexts() {
		Calendar c = Calendar.getInstance();
		c.set(to_year, to_month, to_day, to_hour, to_minute);

		String date = Utils.getDateString(c);
		String time = Utils.getTimeString(c);

		toDateButton.setText(date);
		toTimeButton.setText(time);
	}





	private void initialCalendarSetup() {
		Calendar c = Calendar.getInstance();
		//int curr_year = c.get(Calendar.YEAR);
		//int curr_month = c.get(Calendar.MONTH);
		//int curr_day = c.get(Calendar.DAY_OF_MONTH);
		//int curr_hour = c.get(Calendar.HOUR_OF_DAY);
		//int curr_minute = c.get(Calendar.MINUTE);

		c.set(Calendar.MINUTE, 0);
		c.add(Calendar.HOUR, 1);

		from_year = c.get(Calendar.YEAR);
		from_month = c.get(Calendar.MONTH);
		from_day = c.get(Calendar.DAY_OF_MONTH);
		from_hour = c.get(Calendar.HOUR_OF_DAY);
		from_minute = c.get(Calendar.MINUTE);

		c.add(Calendar.HOUR, 1);

		to_year = c.get(Calendar.YEAR);
		to_month = c.get(Calendar.MONTH);
		to_day = c.get(Calendar.DAY_OF_MONTH);
		to_hour = c.get(Calendar.HOUR_OF_DAY);
		to_minute = c.get(Calendar.MINUTE);
	}

	@SuppressLint("ValidFragment")
	public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle args = getArguments();
			int year = args.getInt("year");
			int month = args.getInt("month");
			int day = args.getInt("day");

			return new DatePickerDialog(getActivity(), this, year, month, day);
		}

		public void onDateSet(DatePicker view, int year, int month, int day) {

			boolean isFrom = getArguments().getBoolean("isFrom");
			if(isFrom){
				from_year = year;
				from_month = month;
				from_day = day;
			} else {
				to_year = year;
				to_month = month;
				to_day = day;
			}
		}
	}

	@SuppressLint("ValidFragment")
	public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle args = getArguments();
			int hour = args.getInt("hour");
			int minute = args.getInt("minute");

			return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
		}

		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

			boolean isFrom = getArguments().getBoolean("isFrom");
			if(isFrom){
				from_hour = hourOfDay;
				from_minute = minute;
			} else {
				to_hour = hourOfDay;
				to_minute = minute;
			}
		}
	}

}
