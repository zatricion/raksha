package com.bloc.settings.prefs;

import com.bloc.MainWithMapActivity;
import com.bloc.R;
import com.bloc.bluetooth.le.BackgroundService;
import com.bloc.bluetooth.le.DeviceControlActivity;

import android.support.v4.app.DialogFragment;
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
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class RadiusPickerDialog extends DialogFragment {
	private Button doneButton;
	private SeekBar sb;
	private boolean isPopup = true;
	
	
	private int radius;
	
	public RadiusPickerDialog(boolean isPopup) {
		this.isPopup = isPopup;
	}
	
	@Override
	public void onResume()
	{
	    super.onResume();
	    if (isPopup) {
	    	setWindowSize();
	    }
	}  

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.radius, container, false);
        final TextView text = (TextView) view.findViewById(R.id.radius_progress);

        
		doneButton = (Button) view.findViewById(R.id.done);
		sb = (SeekBar) view.findViewById(R.id.pick_radius);
		sb.setMax(200); // in tens of meters
		
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
            	MainWithMapActivity.mRadius = radius;
            	BackgroundService.mRadius = radius;
                ed.commit();
                
                // Broadcast radius change
                Intent radiusChange = new Intent(DeviceControlActivity.ACTION_RADIUS_CHANGE);
                getActivity().sendBroadcast(radiusChange);
                
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

