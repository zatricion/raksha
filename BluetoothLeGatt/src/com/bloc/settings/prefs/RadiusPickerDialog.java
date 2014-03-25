package com.bloc.settings.prefs;

import com.bloc.R;
import com.bloc.bluetooth.le.DeviceControlActivity;
import com.bloc.settings.contacts.AsyncGetContacts;
import com.bloc.settings.contacts.ContactListActivity;
import com.bloc.settings.contacts.ContactListAdapter;
import com.google.gson.Gson;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class RadiusPickerDialog extends DialogFragment {
	private ListView contactListView;
	private AsyncGetContacts asyncGetContacts;
	private Button doneButton;
	private SeekBar sb;
	
	
	private int radius;
	
	public RadiusPickerDialog() {
		// No-arg constructor required for DialogFragment
	}
	
	@Override
	public void onResume()
	{
	    super.onResume();
	    setWindowSize();
	}  

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.radius, container);
        final TextView text = (TextView) view.findViewById(R.id.radius_progress);

        
		doneButton = (Button) view.findViewById(R.id.done);
		sb = (SeekBar) view.findViewById(R.id.pick_radius);
		sb.setMax(50); // in tens of meters
		
		if (DeviceControlActivity.mRadius != -1) {
			radius = DeviceControlActivity.mRadius;
			sb.setProgress(radius / 10);
			text.setText(Integer.toString(radius) + " meters");
		}
		
	    sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
	        @Override
	        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	            radius = 10 * progress;
	            text.setText(Integer.toString(radius) + " meters");
	        }

	        @Override
	        public void onStartTrackingTouch(SeekBar seekBar) {}

	        @Override
	        public void onStopTrackingTouch(SeekBar seekBar) {}

	    });
		
		doneButton.setOnClickListener(new View.OnClickListener() {		
			@Override
			public void onClick(View v) {
	        	SharedPreferences.Editor ed = getActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE).edit();
            	ed.putInt(DeviceControlActivity.KEY_RADIUS, radius);
            	DeviceControlActivity.mRadius = radius;
                ed.commit();
		        Toast.makeText(v.getContext(), "Radius has been set", Toast.LENGTH_SHORT).show();
				dismiss();
			}
		});

        return view;
    }
    
    private void setWindowSize() {
    	Display display = getActivity().getWindowManager().getDefaultDisplay();
    	Point size = new Point();
    	display.getSize(size);
    	int width = (int) Math.round(size.x * 0.75);
        Window window = getDialog().getWindow();
        window.setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT);
    }
}

