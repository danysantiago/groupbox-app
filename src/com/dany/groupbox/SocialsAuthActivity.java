package com.dany.groupbox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class SocialsAuthActivity extends Activity {

	private long uid;
	private String socialName;
	
	private boolean hasDelegated;
	
	private String social;
	private String oauthKey;
	private String oauthSecret;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        if (savedInstanceState != null) {
            hasDelegated = savedInstanceState.getBoolean("hasDelegated");
        }

		SharedPreferences prefs = getSharedPreferences("DBAuth", Context.MODE_PRIVATE);
		uid = prefs.getLong("uid", -1);
		
		socialName = getIntent().getExtras().getString("socialName");
		
        setTheme(android.R.style.Theme_Translucent_NoTitleBar);
	}
	
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("hasDelegated", hasDelegated);
    }

	@Override
	protected void onResume() {
		super.onResume();
		
		if(!hasDelegated){
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(Server.BASE_URL+Server.SOCIAL_AUTH_ROUTE + socialName + "?uid=" + uid));
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			startActivity(intent);
			
			hasDelegated = true;
		} else if(hasDelegated && oauthKey == null){
			finish();
		}
		
	}


	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		Log.d("DEBUG", "onNewIntent");
		
		 Uri uri = intent.getData();
         if (uri != null) {
             String path = uri.getPath();
             if (path.equals("/auth-ok")) {
                 try {
                	 social = uri.getQueryParameter("social");
                	 oauthKey = uri.getQueryParameter("key");
                	 oauthSecret = uri.getQueryParameter("secret");
                 } catch (UnsupportedOperationException e) {
                	 e.printStackTrace();
                 }
                 
                 SharedPreferences prefs = this.getSharedPreferences("SocialAuths", MODE_PRIVATE);
                 int activeSocialsInt = Utils.Socials.activateSocial(social, prefs.getInt("activeSocialsInt", 0));
                 
                 SharedPreferences.Editor prefsEditor = prefs.edit();
                 prefsEditor.putInt("activeSocialsInt", activeSocialsInt);
                 prefsEditor.commit();
             }
         }
         
         finish();
	}

}
