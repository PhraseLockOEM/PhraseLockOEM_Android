package com.phraselock.oem.phraselockoem_android.ui.start;

import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ipoxo.PhraseLockStatusDelegate;
import com.phraselock.oem.lib.DB;
import com.phraselock.oem.lib.PLAlertDialog;
import com.phraselock.oem.lib.PLOEMFragment;
import com.phraselock.oem.phraselockoem_android.R;
import com.phraselock.oem.phraselockoem_android.MainActivity;

// Imports from OEM-Library
import com.ipoxo.PhraseLock;
import com.ipoxo.hid.PLHID;
import com.ipoxo.hid.PLHID.HIDDelegate;

public class StartFragment extends PLOEMFragment implements PhraseLockStatusDelegate, HIDDelegate
{
  private static final int MY_PERMISSIONS_REQUEST = 0x1006;
  private StartFragment self = this;
  private AlertDialog alertDlg = null;
  private PhraseLock ploem;
  private PLHID plhid;
  private TextView lockState = null;
  TextView bleState = null;
  TextView appState = null;
  TextView usbModeState = null;
  ProgressBar connectionIndicator = null;
  SwitchMaterial bleSwitch = null;
  EditText hidString = null;
  Button sendHIDDataBtn = null;
  Button setUSBModeBtn = null;
  int usbCurrentMode;
  DB db;
  
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View root = inflater.inflate(R.layout.fragment_start, container, false);
    
    logTextView = root.findViewById(R.id.logTextView);
    logTextView.setMovementMethod(new ScrollingMovementMethod());
    lockState = root.findViewById(R.id.lockState);
    bleState = root.findViewById(R.id.bleState);
    connectionIndicator = root.findViewById(R.id.progressBar);
    bleSwitch = root.findViewById(R.id.bleSwitch);
    hidString = root.findViewById(R.id.hidString);
    sendHIDDataBtn = root.findViewById(R.id.sendHIDData);
    setUSBModeBtn = root.findViewById(R.id.setUSBMode);
    appState = root.findViewById(R.id.appState);
    usbModeState = root.findViewById(R.id.usbModeState);
    
    bleState.setText("No Connection");
    connectionIndicator.setVisibility(View.INVISIBLE);
    bleSwitch.setChecked(false);
    
    bleSwitch.setOnCheckedChangeListener((button, state) -> {
      if (state) {
        onOffPhraseLock(true);
      }
      else {
        onOffPhraseLock(false);
      }
    });
    
    sendHIDDataBtn.setOnClickListener(v -> sendHIDData(String.format("%s\r", hidString.getText().toString())));
    
    setUSBModeBtn.setOnClickListener(v -> setUSBMode());
    
    // Jetzt die Datenbank starten
    db = ((MainActivity) this.getActivity()).db;
    ploem = ((PhraseLock.IPL) getActivity()).getPL();
		/*
		You need to have a unique delegate-ID for each instance that whats to receive
		notifications based on PhraseLock.PhraseLockDelegate and PhraseLock.HIDDelegate
		 */
    ploem.addPLDelegate(this, "MY_UNIQUE_MESSAGE_ID");
    
    plhid = new PLHID();
    plhid.setDelegate(this);
    
    /* Return the version of the library */
    long oemLibVers = ploem.oemLibVersion();
    StringBuffer sb = new StringBuffer();
    sb.append("PhraseLockOEM.aar OEM-Lib Version : ");
    sb.append(oemLibVers);
    sb.append("\n");
    logTextView.setText(sb.toString());
    
    return root;
  }
  
  @Override
  public void onResume()
  {
    super.onResume();
  }
  
  @Override
  public void onDestroyView()
  {
    super.onDestroyView();
  }
  
  @Override
  public void onHiddenChanged(boolean hidden)
  {
    super.onHiddenChanged(hidden);
    
    if (!hidden) {
      ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
      actionBar.hide();
      actionBar.setBackgroundDrawable(new ColorDrawable(0xFFFF9500));
      
      Window window = ((AppCompatActivity) getActivity()).getWindow();
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      final int lFlags = getActivity().getWindow().getDecorView().getSystemUiVisibility();
      getActivity().getWindow().getDecorView().setSystemUiVisibility((lFlags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
    }
  }
  
  /* Called while the connecting process to inform the user */
  @Override
  public void delegatePhraseLockCurrentConnectionState(int state, String message)
  {
    log(String.format("CONNECT STATE: %d: %s", state, message));
  }
  
  /* Called when a connection to the USB-Key is initiated */
  @Override
  public void delegatePhraseLockDidConnect(String versionString, int version, int usbMode)
  {
    showConnectionState(false, "Connection OK");
    
    appState.setText(versionString);
    usbCurrentMode = usbMode;
    switch (usbCurrentMode) {
      case DB.USBMODE.RETURN_USB_MODE:
        self.usbModeState.setText("NO USB");
        break;
      case DB.USBMODE.HID_USB:
        self.usbModeState.setText("HID");
        break;
      case DB.USBMODE.FDO_USB:
        self.usbModeState.setText("CTAP2");
        break;
      case DB.USBMODE.CPD_USB:
        self.usbModeState.setText("CTAP2 | HID");
        break;
    }
  }
  
  /* Called when a connection to the USB-Key got lost */
  @Override
  public void delegatePhraseLockDidDisconnect()
  {
    bleSwitch.setChecked(false);
    showConnectionState(false, "No Connection");
  }
  
  /* Called if unexpected error occured */
  @Override
  public void delegatePhraseLockConnectError()
  {
  
  }
  
  /* Called when a connection to the USB-Key could not established in time */
  @Override
  public void delegatePhraseLockConnectTimeOut()
  {
    bleSwitch.setChecked(false);
    showConnectionState(false, "Connection time out");
  }
  
  /* Called when a masterkey is read or written form or to the usb-key */
  @Override
  public void delegateReceiveMasterKey(byte error, byte[] iv, byte[] aes256Key)
  {
    /**
     This callback is called when your main-key + iv-vector is transmittet from the Phrase-Lock USB-Key
     to your application. You may use this key to e.g. encrypt/decrypt login-credentials. You are free to
     change this key at any time.
     */
  }
  
  /* Called when USB-Mode has changed between CTAP2 / HID / Combined */
  @Override
  public void delegateReceiveUSBMode(byte usbMode)
  {
    usbCurrentMode = usbMode;
    switch (usbCurrentMode) {
      case DB.USBMODE.RETURN_USB_MODE:
        self.usbModeState.setText("NO USB");
        break;
      case DB.USBMODE.HID_USB:
        self.usbModeState.setText("HID");
        break;
      case DB.USBMODE.FDO_USB:
        self.usbModeState.setText("CTAP2");
        break;
      case DB.USBMODE.CPD_USB:
        self.usbModeState.setText("CTAP2 | HID");
        break;
    }
  }
  
  /*	Output of PhraseLock-Framework logging */
  @Override
  public void delegateLogging(long filter, String logStr)
  {
    Log.i("PLNK_DEBUG", logStr);
  }
  
  /* Indicates key-status of NUM-, SCROLL- and CAPS-lock*/
  @Override
  public void delegateKeyStatus(boolean numLock, boolean scrollLock, boolean capsLock)
  {
    if (capsLock) {
      lockState.setText("Caps-Lock set!");
    }
    else {
      lockState.setText("");
    }
  }
  
  /* Keyboard output done */
  @Override
  public void delegateKeyboardStreamDone(byte error)
  {
    log("HID Data Transfer done with error: " + String.valueOf(error));
  }
  
  private void sendHIDData(String asciiString)
  {
    String map = readTXTFile(getContext(), MainActivity.scanCodes_DE_DE);
    plhid.setKBDLayout(map, PLHID.PL_OS_TYPE.OS_WINDOWS_LINUX);
    plhid.setStreamParam((short)6,(short)2);
    plhid.setHIDStream(asciiString);
    plhid.setPLProtocol(ploem, true);
    String unknown = plhid.verifyKBDStream();
    
    if (unknown == null) {
      plhid.startdKBDStream();
    }
    else {
      if (alertDlg != null && alertDlg.isShowing()) {
        alertDlg.dismiss();
      }
      alertDlg = new PLAlertDialog.PLBuilder(this.getContext())
        .setTitle("STOP")
        .setNegativeButton("Abort", null)
        .setPositiveButton("Continue", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            plhid.startdKBDStream();
          }
        }).setMessage("Unknown chars in data stream:\r" + unknown).create();
      alertDlg.show();
    }
  }
  
  private void setUSBMode()
  {
    if (usbCurrentMode == DB.USBMODE.CPD_USB) {
      ploem.setUSBMode(DB.USBMODE.FDO_USB);
    }
    else {
      ploem.setUSBMode(DB.USBMODE.CPD_USB);
    }
  }
  
  private void showConnectionState(boolean waitForConnection, String stateText)
  {
    if (waitForConnection) {
      connectionIndicator.setVisibility(View.VISIBLE);
    }
    else {
      connectionIndicator.setVisibility(View.INVISIBLE);
    }
    
    if (stateText != null) {
      bleState.setText(stateText);
      bleState.setVisibility(View.VISIBLE);
    }
    else {
      bleState.setText("");
      bleState.setVisibility(View.INVISIBLE);
    }
    bleSwitch.setVisibility(View.VISIBLE);
  }
  
  private void onOffPhraseLock(boolean turnOn)
  {
    if (turnOn) {
      if (verifyHardwareAndPermissions()) {
        showConnectionState(true, "...wait...");
        List<byte[]> coreDataArray = new ArrayList<byte[]>();
        ArrayList coreDataSets = ((MainActivity) requireActivity()).db.getAllCoreDataSets();
        for (int i = 0; i < coreDataSets.size(); i++) {
          byte[] s = (byte[]) coreDataSets.get(i);
          byte[] serviceUUID = ploem.getServiceUUIDFromCoreData(s);
          coreDataArray.add(serviceUUID);
        }
        if (coreDataArray.size() > 0) {
          new Handler().post(() ->
            ploem.startPhraseLockConnection(this.getContext(), coreDataArray, false));
        }
        else {
          Toast.makeText(getActivity(), "NO USB-Key found! Pls. scann QR-Code!", Toast.LENGTH_LONG).show();
          new Handler().post(() -> delegatePhraseLockDidDisconnect());
          
        }
      }
      else {
        new Handler().post(() -> ploem.stopPhraseLockConnection());
        delegatePhraseLockDidDisconnect();
      }
    }
    else {
      new Handler().post(() -> ploem.stopPhraseLockConnection());
      delegatePhraseLockDidDisconnect();
    }
  }
  
  public static String readTXTFile(Context context, String fname)
  {
    try {
      Resources resources = context.getResources();
      InputStream iS = resources.getAssets().open(fname);
      StringBuilder inputStringBuilder = new StringBuilder();
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(iS, "UTF-8"));
      String line = bufferedReader.readLine();
      while (line != null) {
        inputStringBuilder.append(line);
        inputStringBuilder.append('\n');
        line = bufferedReader.readLine();
      }
      return inputStringBuilder.toString();
    } catch (Exception e) {
      return null;
    }
  }
  
  public boolean verifyHardwareAndPermissions()
  {
    if (verifyBluetoothHardware()) {
      if (verifyLocationHardware()) {
        if (verifyPermissions()) {
          return true;
        }
      }
    }
    ;
    new Handler().post(() -> bleSwitch.setChecked(false));
    return false;
  }
  
  public boolean verifyBluetoothHardware()
  {
    /* Ab Android 12 */
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      int perm_blscan = this.requireActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN);
      int perm_blconnect = this.requireActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT);
      
      if (perm_blscan == PackageManager.PERMISSION_DENIED ||
        perm_blconnect == PackageManager.PERMISSION_DENIED
      ) {
        String[] permissions = {
          Manifest.permission.BLUETOOTH_SCAN,
          Manifest.permission.BLUETOOTH_CONNECT
        };
        ActivityCompat.requestPermissions(this.requireActivity(), permissions, 225);
        return false;
      }
      return true;
    }
    else {
      try {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean bx = mBluetoothAdapter.isEnabled();
        if (!bx) {
          //mBluetoothAdapter.enable(); Keine automatiches enablen!
          if (alertDlg != null && alertDlg.isShowing()) {
            alertDlg.dismiss();
          }
          alertDlg = new PLAlertDialog.PLBuilder(this.getContext())
            .setTitle("Attention!")
            .setNegativeButton("Abort", null)
            .setPositiveButton("Activate", new DialogInterface.OnClickListener()
            {
              @Override
              public void onClick(DialogInterface dialog, int which)
              {
                startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
              }
            }).setMessage("BLE is off").create();
          alertDlg.show();
        }
        else {
          return true;
        }
      } catch (Exception e) {
        Log.d("TAG", e.getMessage());
      }
    }
    return false;
  }
  
  private boolean verifyLocationHardware()
  {
    LocationManager lm = (LocationManager) this.requireActivity().getSystemService(Context.LOCATION_SERVICE);
    boolean gps_enabled = false;
    boolean network_enabled = false;
    
    try {
      gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
      network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    } catch (Exception ignore) {
    }
    
    if (!gps_enabled && !network_enabled) {
      if (alertDlg != null && alertDlg.isShowing()) {
        alertDlg.dismiss();
      }
      alertDlg = new PLAlertDialog.PLBuilder(this.getContext())
        .setTitle("Attention")
        .setNegativeButton("Abort", null)
        .setPositiveButton("Activate", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
          }
        }).setMessage("Location services are off!").create();
      alertDlg.show();
    }
    else {
      return true;
    }
    return false;
  }
  
  private boolean verifyPermissions()
  {
    int perm_camera = this.requireActivity().checkSelfPermission(Manifest.permission.CAMERA);
    int perm_location = this.requireActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
    if (
      perm_camera == PackageManager.PERMISSION_DENIED ||
        perm_location == PackageManager.PERMISSION_DENIED
    ) {
      if (alertDlg != null && alertDlg.isShowing()) {
        alertDlg.dismiss();
      }
      alertDlg = new PLAlertDialog.PLBuilder(this.getContext())
        .setTitle("Attention")
        .setNegativeButton("Abort", null)
        .setPositiveButton("Activate", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            List<String> listPermissionsNeeded = new ArrayList<>();
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            
            self.requireActivity().requestPermissions(listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
              MY_PERMISSIONS_REQUEST);
          }
        }).setMessage("Persmissions required!").create();
      alertDlg.show();
    }
    else {
      return true;
    }
    return false;
  }
  
}