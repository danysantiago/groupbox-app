package com.dany.groupbox;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

public class SocialsPicker {

	private AlertDialog dialog;

	private ArrayList<String> selectedSocials;
	private boolean[] selected;
	
	private int amountOfActives;
	
	public SocialsPicker (Context context, DialogInterface.OnClickListener clickListener){
		SharedPreferences prefs = context.getSharedPreferences("SocialAuths", Activity.MODE_PRIVATE);
		int activeSocials = prefs.getInt("activeSocialsInt", 0);

		final String[] socialsList = Utils.Socials.intToActiveSocialList(activeSocials);
		selected = new boolean[socialsList.length];
		amountOfActives = socialsList.length;
		
		selectedSocials = new ArrayList<String>();
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Select Social Networks");
		builder.setMultiChoiceItems(socialsList, selected, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				if (isChecked) {
					selectedSocials.add(socialsList[which]);
					selected[which] = true;
				} else if (selectedSocials.contains(socialsList[which])) {
					selectedSocials.remove(socialsList[which]);
					selected[which] = false;
				}
			}
		});
		builder.setPositiveButton("Ok", clickListener);
		builder.setNegativeButton("Cancel", null);

		dialog = builder.create();
	}
	
	public boolean show(){
		if(amountOfActives > 0){
			dialog.show();
			return true;
		} else {
			return false;
		}
			
	}
	
	public void dismiss(){
		dialog.dismiss();
	}
	
	public ArrayList<String> getSelectedSocials(){
		return selectedSocials;
	}

}
