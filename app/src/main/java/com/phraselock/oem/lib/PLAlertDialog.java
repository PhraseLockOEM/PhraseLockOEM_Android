package com.phraselock.oem.lib;

import android.app.AlertDialog;
import android.content.Context;
import android.os.CancellationSignal;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.phraselock.oem.phraselockoem_android.R;

public class PLAlertDialog extends AlertDialog {

	public PLAlertDialog(Context context){
		super(context);
	}

	public static class PLBuilder extends Builder{

		public View customView = null;
		public static CancellationSignal fpCancellationSignal;
		public static int fpTryCounter;

		public PLBuilder(Context context){
			super(context);
			LayoutInflater li = LayoutInflater.from(context);
			customView = li.inflate(R.layout.pl_alert_content, null);

			setView(customView);
			setCancelable(false);
		}

		public PLBuilder(Context context, int resourceID){
			super(context);
			LayoutInflater li = LayoutInflater.from(context);
			customView = li.inflate(resourceID, null);

			setView(customView);
			setCancelable(false);
		}

		@Override
		public AlertDialog create(){
			AlertDialog dlg = super.create();
			dlg.getWindow().setBackgroundDrawableResource(R.drawable.dialog_shape_round_alert);
			return dlg;
		}

		@Override
		public PLBuilder setTitle(CharSequence title) {
			//super.setTitle(title);
			((TextView)customView.findViewById(R.id.dlgTitle)).setText(title);
			return this;
		}

		@Override
		public PLBuilder setMessage(CharSequence title) {
			//super.setMessage(title);
			((TextView)customView.findViewById(R.id.dlgMessageView)).setText(title);
			return this;
		}
	}
}
