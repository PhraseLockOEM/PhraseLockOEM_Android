package com.phraselock.oem.lib;

import android.util.Log;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class PLOEMFragment extends Fragment {

    public TextView logTextView = null;

    public void clearLog(){
        logTextView.setText("");
        logTextView.scrollTo(0,0);
    }

    public void log(String message){
		
        int len = logTextView.length();
        if(len>4096){
            clearLog();
        }
        logTextView.append(message);
        logTextView.append("\r\n");
        logTextView.scrollTo(0, 0);
		
	    Log.i("PLNK_DEBUG",message);
    }
}