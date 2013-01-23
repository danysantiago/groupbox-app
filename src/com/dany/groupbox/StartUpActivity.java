package com.dany.groupbox;

import java.io.IOException;

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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Account;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

public class StartUpActivity extends Activity {

	private DropboxAPI<AndroidAuthSession> mDBApi;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AppKeyPair appKeys = new AppKeyPair(Dropbox.APP_KEY, Dropbox.APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys, Dropbox.ACCESS_TYPE);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);

		SharedPreferences prefs = getSharedPreferences("DBAuth", Context.MODE_PRIVATE);
		if(prefs.getString("key", null) != null && prefs.getString("secret", null) != null){
			AccessTokenPair access = getStoredKeys(prefs);
			mDBApi.getSession().setAccessTokenPair(access);

			startMainActivity();
		} else {
			setContentView(R.layout.activity_intro);
			
			Gallery gallery = (Gallery) findViewById(R.id.intro_gallery);
			ImageAdapter adapter = new ImageAdapter(this);
			gallery.setAdapter(adapter);
			gallery.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> adapter, View v, int pos, long id) {
					if(pos == adapter.getCount() - 1){
						Log.d("DEBUG", "Clicked last image!");
						
						if(isOnline()){
							mDBApi.getSession().startAuthentication(StartUpActivity.this);
						} else {
							Toast.makeText(getApplicationContext(), "An internet connection is needed for first time set-up,  please make sure you have a working internet connection.", Toast.LENGTH_LONG).show();
						}
					}
					
				}
			});
		}


	}
	
	public boolean isOnline() {
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnected()) {
	        return true;
	    }
	    return false;
	}

	//Start Main Activity thinking in Compability
	private void startMainActivity() {

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			Intent i = new Intent(StartUpActivity.this, MainActivityOld.class);
			startActivity(i);

			finish();
		} else {
			Intent i = new Intent(StartUpActivity.this, MainActivity.class);
			startActivity(i);

			finish();
		}
	}

	private AccessTokenPair getStoredKeys(SharedPreferences prefs) {
		AccessTokenPair atp = new AccessTokenPair(prefs.getString("key", null), prefs.getString("secret", null));
		return atp;
	}

	protected void onResume() {
		super.onResume();

		if (mDBApi.getSession().authenticationSuccessful()) {

			try {
				// MANDATORY call to complete auth.
				// Sets the access token on the session
				mDBApi.getSession().finishAuthentication();

				AccessTokenPair tokens = mDBApi.getSession().getAccessTokenPair();

				storeKeys(tokens.key, tokens.secret);

				RegisterTask task = new RegisterTask(this);
				task.execute();

			} catch (IllegalStateException e) {
				Log.i("DbAuthLog", "Error authenticating", e);
			}
		} else {
			//TODO: Handle other resume stuff
		}

	}

	private void storeKeys(String key, String secret) {
		Log.d("DEBUG", "Got Db Key: " + key);
		Log.d("DEBUG", "Got Db Secret: " + secret);

		SharedPreferences.Editor prefs = getSharedPreferences("DBAuth", Context.MODE_PRIVATE).edit();
		prefs.putString("key", key);
		prefs.putString("secret", secret);
		prefs.commit();

	}

	private void storeAccountInfo(long uid, String displayName) {
		Log.d("DEBUG", "Got Db UID: " + uid);
		Log.d("DEBUG", "Got Db DisplayName: " + displayName);

		SharedPreferences.Editor prefs = getSharedPreferences("DBAuth", Context.MODE_PRIVATE).edit();
		prefs.putLong("uid", uid);
		prefs.putString("displayName", displayName);
		prefs.commit();
	}

	private class RegisterTask extends AsyncTask<Void, Void, Void>{

		private Context context;
		
		private DefaultHttpClient client;

		private ProgressDialog pd;
		
		private boolean success;

		public RegisterTask(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			
			pd = new ProgressDialog(context);
			pd.setMessage("Registering...");
			pd.setCancelable(false);
			pd.show();
			
			int timeout = 10000;
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeout);
			HttpConnectionParams.setSoTimeout(httpParameters, timeout);

			client = new DefaultHttpClient(httpParameters);
		}

		@Override
		protected Void doInBackground(Void... params) {
			
			try {
				//Get Account Info
				Account mDAccount = mDBApi.accountInfo();
				storeAccountInfo(mDAccount.uid, mDAccount.displayName);
				
				SharedPreferences prefs = getSharedPreferences("DBAuth", Context.MODE_PRIVATE);
				
				//Create JSON String
				JSONObject registerJson = new JSONObject();		
				registerJson.put("uid", prefs.getLong("uid", -1));
				registerJson.put("key", prefs.getString("key", null));
				registerJson.put("secret", prefs.getString("secret", null));
				registerJson.put("displayName", prefs.getString("displayName", null));

				//HTTP Post
				HttpPost post = new HttpPost(Server.BASE_URL + Server.REGISTER_ROUTE);
				StringEntity postParam = new StringEntity(registerJson.toString());
				post.addHeader("content-type", "application/json");
				post.setEntity(postParam);

				HttpResponse response = client.execute(post);
				int statusCode = response.getStatusLine().getStatusCode();

				if(statusCode == HttpStatus.SC_OK){
					String fullRes = EntityUtils.toString(response.getEntity());
					if(fullRes.equals("0")){
						success = true;
						Log.d("DEBUG", "Registration cool!");
					} else {
						success = false;
					}
				} else {
					Log.d("BackendRegisterLog","HttpError performing registration, code: " + statusCode);
					success = false;
				}

			} catch (JSONException e) {
				Log.d("JSONLog", "Error creating JSON object", e);
				success = false;
			} catch (ClientProtocolException e) {
				Log.d("BackendRegisterLog", "Error doing register POST", e);
				success = false;
			} catch (IOException e) {
				Log.d("BackendRegisterLog", "Error doing register POST", e);
				success = false;
			} catch (DropboxException e) {
				Log.d("DbAuthLog", "Error getting account info", e);
				success = false;
			}
						
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			pd.dismiss();

			if(success){
				startMainActivity();
			} else {
				SharedPreferences.Editor prefs = getSharedPreferences("DBAuth", Context.MODE_PRIVATE).edit();
				prefs.remove("key");
				prefs.remove("secret");
				prefs.commit();
				
				Toast.makeText(context, "Something went wrong with the registration, please try again later.", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private class ImageAdapter extends BaseAdapter {
		
		private Context context;
		
		private int[] imageIDs = {
				R.drawable.intro_image_1, 
				R.drawable.intro_image_2,
				R.drawable.intro_image_3,
				R.drawable.intro_image_4,
				R.drawable.intro_image_5};
		
		public ImageAdapter(Context context){
			super();
			this.context = context;
		}

		@Override
		public int getCount() {
			return imageIDs.length;
		}

		@Override
		public Object getItem(int position) {
			return imageIDs[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView = new ImageView(context);
			imageView.setImageResource(imageIDs[position]);
			imageView.setScaleType(ImageView.ScaleType.FIT_XY);
			
			return imageView;
		}
		
	}
}
