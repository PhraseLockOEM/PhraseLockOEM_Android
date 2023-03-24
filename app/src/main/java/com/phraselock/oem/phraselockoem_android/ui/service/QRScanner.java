package com.phraselock.oem.phraselockoem_android.ui.service;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.phraselock.oem.phraselockoem_android.R;

public class QRScanner extends AppCompatActivity
{
  private CaptureManager capture;
  Button scanStopBtn = null;
  private DecoratedBarcodeView barcodeScannerView;
  
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.qr_scanner);
    
    ActionBar actionBar = getSupportActionBar();
    actionBar.hide();
    
    scanStopBtn = findViewById(R.id.scanStopBtn);
    barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
    barcodeScannerView.setStatusText(null);
    capture = new CaptureManager(this, barcodeScannerView);
    capture.initializeFromIntent(getIntent(), savedInstanceState);
    capture.decode();
    
    scanStopBtn = findViewById(R.id.scanStopBtn);
    scanStopBtn.setOnClickListener(v -> stopScanning());
  }
  
  public void stopScanning()
  {
    finish();
  }
  
  @Override
  protected void onResume()
  {
    super.onResume();
    capture.onResume();
  }
  
  @Override
  protected void onPause()
  {
    super.onPause();
    capture.onPause();
  }
  
  @Override
  protected void onDestroy()
  {
    super.onDestroy();
    capture.onDestroy();
  }
  
  @Override
  protected void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);
    capture.onSaveInstanceState(outState);
  }
  
  @Override
  public boolean onSupportNavigateUp()
  {
    onBackPressed();
    return true;
  }
  
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event)
  {
    return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
  }
}
