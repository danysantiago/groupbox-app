package com.dany.groupbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class FolderPicker extends Activity {

	private DropboxAPI<AndroidAuthSession> mDBApi;

	private ListView folderListView;

	private ProgressBar pb;

	private Context context;

	private Stack<String> pathStack;
	private Stack<ArrayList<Folder>> folderStack;

	ArrayList<Folder> folderList;

	private FolderLoadTask task;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_folder_picker);

		context = this;

		AppKeyPair appKeys = new AppKeyPair(Dropbox.APP_KEY, Dropbox.APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys, Dropbox.ACCESS_TYPE);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);

		SharedPreferences prefs = getSharedPreferences("DBAuth", Context.MODE_PRIVATE);
		AccessTokenPair access = getStoredKeys(prefs);
		mDBApi.getSession().setAccessTokenPair(access);

		pathStack = new Stack<String>();
		folderStack = new Stack<ArrayList<Folder>>();

		folderListView = (ListView) findViewById(R.id.folders_listView);

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){

		} else {

		}

		pb = (ProgressBar) findViewById(R.id.folders_progressBar);

		task = new FolderLoadTask();
		task.execute(getPathString());

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_file_picker, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.menu_new_folder:
			newFolder();
			break;

		}
		return true;
	}

	public void finishWithResult(String s){
		pathStack.push(s);
		Intent data = new Intent();
		data.putExtra("folderPath", getPathString());
		setResult(Activity.RESULT_OK, data);
		finish();
	}

	@Override
	public void onBackPressed() {

		if(!task.isDone())
			task.cancel(true);

		if(pathStack.isEmpty()){
			finish();
		} else {
			pathStack.pop();
			
			if(!task.isCancelled())
				folderStack.pop();

			folderList = folderStack.peek();

			MyFolderListViewAdapter adapter = new MyFolderListViewAdapter(context, R.id.folders_listView, folderList);
			folderListView.setAdapter(adapter);

			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
				( (Activity) context ).setTitle(getPathString());
			} else {
				( (Activity) context ).getActionBar().setTitle(getPathString());
			}
			
			pb.setVisibility(View.GONE);
			folderListView.setVisibility(View.VISIBLE);
		}
	}

	private void newFolder() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Create Folder");

		final EditText input = new EditText(this);
		builder.setView(input);

		builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String folderName = input.getText().toString();

				task = new FolderLoadTask();
				task.execute(getPathString(), folderName);
				return;
			}
		});

		builder.setNegativeButton("Cancel", null);

		builder.create().show();
	}

	private String getPathString() {
		StringBuilder sb = new StringBuilder();

		if(!pathStack.isEmpty()){
			for(int i = 0; i < pathStack.size(); i++){
				sb.append("/");
				sb.append(pathStack.get(i));
			}
		} else {
			sb.append("/");
		}
		return sb.toString();
	}

	private AccessTokenPair getStoredKeys(SharedPreferences prefs) {
		AccessTokenPair atp = new AccessTokenPair(prefs.getString("key", null), prefs.getString("secret", null));
		return atp;
	}

	private class FolderLoadTask extends AsyncTask<String, Void, Void>{

		private boolean success;
		private boolean cancelled;
		private boolean done;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pb.setVisibility(View.VISIBLE);
			folderListView.setVisibility(View.GONE);

			folderList = new ArrayList<Folder>();
		}

		public boolean isDone() {
			return done;
		}

		@Override
		protected Void doInBackground(String... params) {
			String path = params[0];
			Log.d("DEBUG", "Querying path: " + path);

			try{

				if(params.length == 2 && params[1] != null){
					String newFolderName = params[1];
					mDBApi.createFolder(path + "/" + newFolderName);
					Log.d("DEBUG", "New Folder Created: " + newFolderName);
				}

				Entry entries = mDBApi.metadata(path, 100, null, true, null);
				if(isCancelled()){
					cancelled = true;
					return null;
				}

				for (Entry e : entries.contents) {
					if (!e.isDeleted && e.isDir){
						String name = e.fileName();
						boolean  hasSubFolders = false;

						//TODO: Clean this, cheap attempt to not display arrow on folders without sub folders
						//						for (Entry e2 : e.contents) {
						//							if(!e2.isDeleted && e2.isDir){
						//								hasSubFolders = true;
						//								break;
						//							}
						//						}

						hasSubFolders = true;

						folderList.add(new Folder(name, hasSubFolders));
					}
					Log.d("DEBUG", "Is Folder: " + String.valueOf(e.isDir));
					Log.d("DEBUG", "Item Name: " + e.fileName());
				}
				success = true;

			} catch (DropboxException e){
				success = false;
				Log.e("DBFolder", "Error getting metadata", e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			if(!cancelled){
				if(success){

					if(!folderList.isEmpty()){

						folderStack.push(folderList);

						MyFolderListViewAdapter adapter = new MyFolderListViewAdapter(context, R.id.folders_listView, folderList);
						folderListView.setAdapter(adapter);

					} else {
						pathStack.pop();
						Toast.makeText(context, "No Sub-Folder", Toast.LENGTH_SHORT).show();
					}

					if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
						( (Activity) context ).setTitle(getPathString());
					} else {
						( (Activity) context ).getActionBar().setTitle(getPathString());
					}

				} else {
					if(!pathStack.isEmpty()){
						pathStack.pop();
					}

					Toast.makeText(context, "Couldn't load folder. Got Internet? Please, try again.", Toast.LENGTH_SHORT).show();
				}
			}

			pb.setVisibility(View.GONE);
			folderListView.setVisibility(View.VISIBLE);
			
			done = true;
		}


	}

	private class MyFolderListViewAdapter extends ArrayAdapter<Folder>{

		public MyFolderListViewAdapter(Context context, int textViewResourceId, List<Folder> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;

			Folder folder = getItem(position);

			LayoutInflater inflater = ( (Activity) context).getLayoutInflater();
			row = inflater.inflate(R.layout.folder_list_item, parent, false);
			row.setTag(folder.name);
			row.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					String s = (String) v.getTag();

					finishWithResult(s);
				}
			});


			TextView tv = (TextView) row.findViewById(R.id.row_item_textView);
			tv.setText(folder.name);

			Button btn = (Button) row.findViewById(R.id.row_item_button);
			if(folder.hasSubFolders){
				btn.setTag(folder.name);
				btn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						String s = (String) v.getTag();

						pathStack.push(s);

						task = new FolderLoadTask();
						task.execute(getPathString());
					}
				});
			} else {
				btn.setVisibility(View.GONE);
			}

			return row;
		}


	}

	private class Folder{
		String name;
		boolean hasSubFolders;

		public Folder(String name, boolean hasSubFolders) {
			super();
			this.name = name;
			this.hasSubFolders = hasSubFolders;
		}


	}
}
