package com.dany.groupbox;

import java.util.Calendar;

public class EventHolder {

	private String name;
	private long start;
	private long end;
	private String folder;
	private int socials;
	private String message;
	private int eventID;
	private String serverID;

	public EventHolder(String name, long start, long end, String folder, int socials, String message) {
		super();
		this.name = name;
		this.start = start;
		this.end = end;
		this.folder = folder;
		this.socials = socials;
		this.message = message;
	}
	public String getName() {
		return name;
	}
	public long getStart() {
		return start;
	}
	public long getEnd() {
		return end;
	}
	public String getFolder() {
		return folder;
	}
	public int getSocials() {
		return socials;
	}
	public String getMessage() {
		return message;
	}
	public void setEventID(int eventID) {
		this.eventID= eventID;
	}
	
	public int getEventID(){
		return eventID;
	}
	
	public void setServerID(String eventID) {
		this.serverID= eventID;
	}
	
	public String getServerID(){
		return serverID;
	}

	public Calendar getStartCalendar(){
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(start*1000);
		return c;
	}
	
	public Calendar getEndCalendar(){
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(end*1000);
		return c;
	}


}
