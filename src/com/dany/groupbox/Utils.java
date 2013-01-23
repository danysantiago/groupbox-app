package com.dany.groupbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

public class Utils {

	public static String getDateString(Calendar c) {
		StringBuilder sb = new StringBuilder();
		sb.append(c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));
		sb.append(", ");
		sb.append(c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()));
		sb.append(" ");
		sb.append(c.get(Calendar.DAY_OF_MONTH));
		sb.append(", ");
		sb.append(c.get(Calendar.YEAR));

		return sb.toString();
	}

	public static String getTimeString(Calendar c) {
		StringBuilder sb = new StringBuilder();
		int hour = c.get(Calendar.HOUR);
		sb.append(hour == 0 ? "12" : hour);
		sb.append(":");
		int min = c.get(Calendar.MINUTE);
		sb.append(min < 10 ? "0"+min : min);
		sb.append(c.getDisplayName(Calendar.AM_PM, Calendar.SHORT, Locale.getDefault()));

		return sb.toString();
	}
	
	public static String getDateTimeString(Calendar c){
		return getDateString(c) + " - " + getTimeString(c);
	}

	public static class Socials {
		
		private static String[] SOCIALS_LIST = {
			"twitter",
			"facebook"
		};

		public static int activateSocial(String socialName, int currSocialInt){
			
			int pos = Arrays.asList(SOCIALS_LIST).indexOf(socialName);
						
			return currSocialInt | (1 << pos);
		}
		
		public static boolean isSocialActive(String socialName, int currSocialInt){
			int pos = Arrays.asList(SOCIALS_LIST).indexOf(socialName);
			
			return ( currSocialInt & (1 << pos) ) != 0;
		}

		public static String[] intToActiveSocialList(int currSocialInt) {
			ArrayList<String> socials = new ArrayList<String>();
			
			for(int i = 0; i < SOCIALS_LIST.length; i++){
				if(isSocialActive(SOCIALS_LIST[i], currSocialInt))
					socials.add(SOCIALS_LIST[i]);
			}
			
			String[] arr = new String[socials.size()];
			arr = socials.toArray(arr);
			
			return arr;
		}
		
		public static int activeSocialListToInt(ArrayList<String> socialList){
			int socialInt = 0;
			
			for(int i = 0; i < socialList.size(); i++){
				socialInt = activateSocial(socialList.get(i), socialInt);
			}
			
			return socialInt;
		}
		

	}

}
