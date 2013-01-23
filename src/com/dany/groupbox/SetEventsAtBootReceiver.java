package com.dany.groupbox;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class SetEventsAtBootReceiver extends BroadcastReceiver {

	@Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
        	
        	Log.d("DEBUG", "BOOT Broadcast Received");
            
    		SharedPreferences prefs = context.getSharedPreferences("DBAuth", Context.MODE_PRIVATE);
    		if(prefs.getString("key", null) != null && prefs.getString("secret", null) != null){
    			
    			DBAdapter db = new DBAdapter(context);
    			Cursor c = db.queryAllEvents();
    			
    			c.moveToFirst();
    			
    			while(!c.isAfterLast()){
    				long startTime = c.getLong(c.getColumnIndex(DBAdapter.COL_START_TIME));
    				long endTime = c.getLong(c.getColumnIndex(DBAdapter.COL_END_TIME));
    				
    				Calendar currCalendar = Calendar.getInstance();
    				
    				Calendar startCalendar = Calendar.getInstance();
    				startCalendar.setTimeInMillis(startTime*1000);
    				
    				Calendar endCalendar = Calendar.getInstance();
    				endCalendar.setTimeInMillis(endTime*1000);
    				
    				if(currCalendar.before(endCalendar)){
    					int eventID = c.getInt(c.getColumnIndex("_id"));
    					String name = c.getString(c.getColumnIndex(DBAdapter.COL_NAME));
    					String folderPath = c.getString(c.getColumnIndex(DBAdapter.COL_FOLDER));
    					
        				Intent i = new Intent(context, MonitorService.class);
        				i.setData(Uri.fromParts("groupbox-event", "//1/event?id="+eventID, null));
        				i.putExtra("eventName", name);
        				i.putExtra("folderPath", folderPath);
        				i.putExtra("endTime", endCalendar.getTimeInMillis());
        				PendingIntent pIntent = PendingIntent.getService(context, 1, i, 0);
        				
        				AlarmManager alarmManager = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
        				alarmManager.set(AlarmManager.RTC_WAKEUP, startCalendar.getTimeInMillis(), pIntent);
    				}
    				
    				c.moveToNext();
    			}
    			
    			c.close();
    			
    		}
        }
    }

}
