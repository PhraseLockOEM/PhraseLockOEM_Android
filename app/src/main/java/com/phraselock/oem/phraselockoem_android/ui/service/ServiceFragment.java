package com.phraselock.oem.phraselockoem_android.ui.service;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import com.google.zxing.client.android.Intents;
import com.ipoxo.PhraseLock;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.phraselock.oem.lib.DB;
import com.phraselock.oem.lib.FC;
import com.phraselock.oem.lib.PLOEMFragment;
import com.phraselock.oem.phraselockoem_android.MainActivity;
import com.phraselock.oem.phraselockoem_android.R;

public class ServiceFragment extends PLOEMFragment {
	
	private PhraseLock ploem;
	DB db;
	
	private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
			result -> {
				if(result.getContents() == null) {
					Intent originalIntent = result.getOriginalIntent();
				} else {
					Log.d("PLNK_DEBUG", "Scanned:");
					String scanData = result.getContents();
					storeUSBCoreData(scanData);
				}
			});
	
	Button scanBtn = null;
	Button deleteBtn = null;
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
	    View root = inflater.inflate(R.layout.fragment_service, container, false);
	    logTextView             = root.findViewById(R.id.logService);
	    logTextView.setMovementMethod(new ScrollingMovementMethod());
	    scanBtn                 = root.findViewById(R.id.scanBtnStop);
		deleteBtn               = root.findViewById(R.id.deleteRKBtn);
		
	    // Jetzt die Datenbank starten
	    db = ((MainActivity)this.getActivity()).db;
	    ploem = ((PhraseLock.IPL) getActivity()).getPL();
	
	    scanBtn.setOnClickListener(v -> scanCoreData());
	    deleteBtn.setOnClickListener(v -> deleteAllRK());
	
	    return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
	
	public void scanCoreData(){
		
		ScanOptions options = new ScanOptions();
		options.setCaptureActivity(QRScanner.class);
		options.addExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE);
		barcodeLauncher.launch(options);
	}
	
	public void storeUSBCoreData(String coreData){
		log(coreData);
		byte[] nsCoreData = ploem.prepareScannedCoreData(coreData);
		String dmp = FC.byteArrayToHexString(nsCoreData);
		log(dmp);
		
		byte[] bServiceUUID = ploem.getServiceUUIDFromCoreData(nsCoreData);
		String sServiceUUID = DB.getGuidFromByteArray(bServiceUUID);
		
		((MainActivity) requireActivity()).db.setCoreDataSet(sServiceUUID.toUpperCase(), nsCoreData);
		
	}
	
	public void deleteAllRK(){
		db.delete_All_residentCredData();
	}
	
}