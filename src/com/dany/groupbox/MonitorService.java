package com.dany.groupbox;

import java.io.File;
import java.util.Calendar;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

public class MonitorService extends IntentService {
	
	public MonitorService() {
		super("MonitorService");
	}
	
	private static final int EVENT_NOTIFICATION_ID = 1;

	private PhotoObserver photoObserver;
	
	private NotificationManager mNotifyManager;
	private Builder mBuilder;

	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.d("DEBUG", "MonitorService onCreate");
				
		mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setContentTitle("GroupBox");
		mBuilder.setSmallIcon(R.drawable.groupbox_logo);
		//mBuilder.setOngoing(true);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String eventName = intent.getStringExtra("eventName");
		String folderPath = intent.getStringExtra("folderPath");
		long endTime = intent.getLongExtra("endTime", -1);
		
		mBuilder.setContentText("Monitoring Pictures for \'" + eventName + "\' event.");
		//mNotifyManager.notify(EVENT_NOTIFICATION_ID, mBuilder.build());
		
		//Set Photo Content Observer
		photoObserver = new PhotoObserver(this, folderPath);
		this.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, photoObserver);
		Log.d("DEBUG", "registered content observer");
		
		startForeground(EVENT_NOTIFICATION_ID, mBuilder.build());
		
		//Old waiting method, minute polling to check if done
//		while(true){
//			Calendar c = Calendar.getInstance();
//			if(c.getTimeInMillis() > endTime)
//				break;
//			
//			try {
//				Thread.sleep(60*1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
		Calendar c = Calendar.getInstance();
		try {
			Thread.sleep(endTime - c.getTimeInMillis());
		} catch (InterruptedException e) {
			Log.d("DEBUG", "Monitor Service interrupted!", e);
		} finally {
			mNotifyManager.cancel(EVENT_NOTIFICATION_ID);
		}
				
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		
		Log.d("DEBUG", "Monitor Service onLowMemory!");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("DEBUG", "Monitor Service OnDestroy");
		
		mNotifyManager.cancel(EVENT_NOTIFICATION_ID);
		
		this.getContentResolver().unregisterContentObserver(photoObserver);
		Log.d("DEBUG", "unregistered content observer");
	}
	
	private class PhotoObserver extends ContentObserver {

		private Context context;
		
		private String folderPath;
			
		public PhotoObserver(Context context, String DBFolderPath) {
			super(null);

			this.context = context;
			this.folderPath = DBFolderPath;
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			PhotoHolder media = readFromMediaStore(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

			SharedPreferences obsPrefs = context.getSharedPreferences("observer", Context.MODE_PRIVATE);
			long lastDateAdded = obsPrefs.getLong("lastDateAdded", -1);
			if(media.getDateAdded() > lastDateAdded){
				saveNewDateAdded(obsPrefs, media.getDateAdded());
				Log.d("DEBUG", "New Picture: " + media.getFile().getName());
				sendPhotoToUploaded(media);
			}
		}


		private PhotoHolder readFromMediaStore(Context context, Uri uri) {
			Cursor cursor = context.getContentResolver().query(uri, null, null, null, "date_added DESC");
			PhotoHolder media = null;
			if (cursor.moveToNext()) {
				String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaColumns.DATA));
				Long dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED));
				media = new PhotoHolder(new File(filePath), dateAdded);			
			}
			cursor.close();
			return media;
		}

		private void saveNewDateAdded(SharedPreferences obsPrefs, Long dateAdded) {
			SharedPreferences.Editor prefsEditor = obsPrefs.edit();
			prefsEditor.putLong("lastDateAdded", dateAdded);
			prefsEditor.commit();
		}
		
		public void sendPhotoToUploaded(PhotoHolder media) {
			Intent intent = new Intent(context, UploadService.class);
			intent.putExtra("photoPath", media.getFile().getAbsolutePath());
			intent.putExtra("folderPath", folderPath);
			context.startService(intent);

		}
		
	}

	
	

}
