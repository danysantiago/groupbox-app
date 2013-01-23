package com.dany.groupbox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class UploadService extends IntentService{

	//Dropbox API Info
	final static private String APP_KEY = "wh0so1j5zng3xg3";
	final static private String APP_SECRET = "srldhx2vego0a9c";
	final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;
	private static final long MIN_SIZE_FOR_COMPRESSION = 1048576; //1 Megabyte

	private DropboxAPI<AndroidAuthSession> mDBApi;

	private NotificationManager mNotifyManager;
	private Builder mBuilder;
	
	private boolean success;

	public UploadService() {
		super("UploadService");
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences prefs = getSharedPreferences("DBAuth", Context.MODE_PRIVATE);

		AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
		AccessTokenPair access = getStoredKeys(prefs);
		mDBApi.getSession().setAccessTokenPair(access);

		mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setContentTitle("Picture Upload");
		mBuilder.setContentText("Upload in progress...");
		mBuilder.setSmallIcon(android.R.drawable.stat_sys_upload);
	}

	private AccessTokenPair getStoredKeys(SharedPreferences prefs) {
		AccessTokenPair atp = new AccessTokenPair(prefs.getString("key", null), prefs.getString("secret", null));
		return atp;
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Bundle extras = intent.getExtras();
		String photoPath = extras.getString("photoPath");
		String folderPath = extras.getString("folderPath");
		Log.d("DEBUG", "AbsolutePath: " + photoPath);

		File tempFile = null;
		File photoFile = new File(photoPath);
		if(photoFile.length() > MIN_SIZE_FOR_COMPRESSION){

			SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			int scaleFactor = Integer.parseInt(settingsPrefs.getString(SettingsActivity.IMAGE_COMPRESSION_KEY, "2"));

			// Get the dimensions of the bitmap
			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(photoPath, bmOptions);
			int photoW = bmOptions.outWidth;
			int photoH = bmOptions.outHeight;

//			int targetW = (int) (photoW * (1.0-((float) compressRatio)/100.0));
//			int targetH = (int) (photoH * (1.0-((float) compressRatio)/100.0));

			// Determine how much to scale down the image
//			int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
			Log.d("DEBUG", "Scale factor: " + scaleFactor);

			// Decode the image file into a Bitmap sized to fill the View
			bmOptions.inJustDecodeBounds = false;
			bmOptions.inSampleSize = scaleFactor;
			bmOptions.inPurgeable = true;

			Bitmap bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);

			File dirFile = new File(Environment.getExternalStorageDirectory() + "/.groupbox");
			if(!dirFile.exists())
				dirFile.mkdir();

			tempFile = new File(Environment.getExternalStorageDirectory() + "/.groupbox/temp");

			try {
				int imgQlty = scaleFactor == 3 ? 96 : 100;
				bitmap.compress(CompressFormat.JPEG, imgQlty, new FileOutputStream(tempFile));
			} catch (FileNotFoundException e) {
				Log.e("DbUploadPic", "Error compressing photo.", e);
			}
			
			Log.d("DEBUG", "Compressed from " + photoFile.length() +" to " + tempFile.length()); 

		} else {
			tempFile = photoFile;
		}

		// Upload content.
		InputStream inputStream = null;
		try {

			inputStream = getContentResolver().openInputStream(Uri.fromFile(tempFile));
			Log.d("DEBUG", "Uploading: " + folderPath + "/" + photoFile.getName());
			Entry newEntry = mDBApi.putFile(folderPath + "/" + photoFile.getName(), inputStream, tempFile.length(), null, new UploadProgressListener());

			Log.i("DbUploadPic", "The uploaded file's rev is: " + newEntry.rev);
			success = true;
		} catch (DropboxUnlinkedException e) {
			// User has unlinked, ask them to link again here.
			Log.e("DbUploadPic", "User has unlinked.");
			success = false;
		} catch (DropboxException e) {
			Log.e("DbUploadPic", "Something went wrong while uploading.");
			success = false;
		} catch (FileNotFoundException e) {
			Log.e("DbUploadPic", "File not found.");
			success = false;
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {}
			}
		}
		
		if(success){
			mBuilder.setContentText("Upload completed.");
			mBuilder.setSmallIcon(android.R.drawable.stat_sys_upload_done);
		} else {
			mBuilder.setContentText("Upload failed... Sorry, thats one lost picture.");
			mBuilder.setSmallIcon(android.R.drawable.stat_notify_error);

		}
		
		mBuilder.setProgress(0,0,false);
		mNotifyManager.notify(0, mBuilder.build());

	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}


	private class UploadProgressListener extends ProgressListener{

		@Override
		public void onProgress(long bytes, long total) {
			int progress = (int) (((float) bytes/ (float) total)*100);
			mBuilder.setProgress(100, progress, false);
			mNotifyManager.notify(0, mBuilder.build());
			Log.d("DEBUG", "Progress: " + progress);
		}

	}

}
