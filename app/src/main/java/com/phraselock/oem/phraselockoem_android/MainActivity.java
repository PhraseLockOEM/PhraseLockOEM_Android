package com.phraselock.oem.phraselockoem_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import java.io.InputStream;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.phraselock.oem.lib.DB;
import com.ipoxo.PhraseLock;
import com.ipoxo.phraselock.PLUserDataCB;
import com.phraselock.oem.phraselockoem_android.ui.service.ServiceFragment;
import com.phraselock.oem.phraselockoem_android.ui.start.StartFragment;

public class MainActivity extends AppCompatActivity implements PhraseLock.IPL, PLUserDataCB
{
  
  /*
  PhraseLock Community API-Key. Required to run specific functionality on the USB-Key.
  */
  public static final char[] COMMUNITY_API_KEY_T4 = {
    0xC6, 0x6D, 0x84, 0x6B, 0x3D, 0x34, 0xD7, 0x10, 0xB5, 0x87, 0xB6, 0xE1, 0x9B, 0x4E, 0x52, 0xEE,
    0x85, 0xB0, 0x96, 0xAB, 0xB1, 0x6F, 0x1F, 0x00, 0x07, 0x4B, 0xFF, 0x19, 0x57, 0x06, 0x8D, 0xC2,
    0xF2, 0x26, 0x9B, 0x72, 0xF2, 0xD4, 0xC7, 0x2A, 0x21, 0x45, 0xBE, 0x69, 0xAE, 0x49, 0xD5, 0x10,
    0x1B, 0x17, 0x73, 0x2B, 0x1C, 0xC3, 0x05, 0x3E, 0x55, 0x48, 0xE1, 0x03, 0x66, 0xF0, 0x95, 0x9A,
  };
  
  public static final char[] API_KEY = COMMUNITY_API_KEY_T4;
  
  public static String aagUUID = "F0DB552BB9B74AE6BCAF4E87F9C563D4";       /*  Identifying your authenticator model */
  public static String certPath = "keystore/my_fido2_cert.p12";               /* Certificate for FIDO2 */
  public static String certPassword = "my_secret_cert_pa&&word";
  
  public static String scanCodes_DE_DE = "de_de.xml";
  
  public PhraseLock ploem;
  public DB db;
  
  public MainActivity self = this;
  private BottomNavigationView navView = null;
  private final FragmentManager fragmentManager = getSupportFragmentManager();
  private final StartFragment startfragment = new StartFragment();
  private final ServiceFragment setupFragment = new ServiceFragment();
  // Must not be null!!!
  private Fragment activeFragment = startfragment;
  
  private boolean isForground = false;
  private int activityReferences = 0;
  private boolean isActivityChangingConfigurations = false;
  Application.ActivityLifecycleCallbacks alc;
  
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    navView = findViewById(R.id.nav_view);
    
    fragmentManager.beginTransaction()
      .add(R.id.container, startfragment).hide(startfragment)
      .add(R.id.container, setupFragment).hide(setupFragment)
      .commit();
    
    navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener()
    {
      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)
      {
        if (menuItem.getItemId() == R.id.navigation_start) {
          fragmentManager.beginTransaction().hide(activeFragment).show(startfragment).commit();
          activeFragment = startfragment;
          return true;
          
        }
        else if (menuItem.getItemId() == R.id.navigation_service) {
          fragmentManager.beginTransaction().hide(activeFragment).show(setupFragment).commit();
          activeFragment = setupFragment;
          return true;
        }
        
        return false;
      }
    });
    
    navView.setSelectedItemId(R.id.navigation_start);
    
    /* ***************** PhraseLock Initialisation START  ***************** */
        /*
         Run this ONLY once! Execute it somewhere in an object, service
         or activity that retains as long the app is running.
        */
    
    db = new DB(this);
    db.initOnStartDB(this);
    
    String certID = null;
    String rp1 = "8CC24602-03BA-4F24-B80C-8F16EB93AA1E";
    String rp2 = "14678";
    
    byte[] p12PrivCert = readByteArrayFile(this, certPath);
    byte[] apiKey = DB.charArray2byteArray(MainActivity.API_KEY);
    String certPWD = getCertPWD(certID);
    
    ploem = new PhraseLock();
    ploem.initPhraseLock(this, this, apiKey, 0xF0FF); // Logging filter
    
    ploem.enableUserVerification(false);
    
    boolean bInt = ploem.loadTokenID(rp1, rp2, false, null, p12PrivCert, certPWD);
    
    if (!bInt) {
      bInt = ploem.loadTokenID(rp1, rp2, false, "12345678910", p12PrivCert, certPWD);
    }
    
    long counter = ploem.incrementCounter(0);
    
    /**
     * loadTokenID() is used to aply a different token
     *
     * ploem.loadTokenID("Your-Specific-Token", [the certificate in pfx format], "the certificate-passwort");
     */
    
    /**
     Logging explaination:
     
     #define FORCE_DEBUG		    0x00000001		Forcing some output in debug-mode only
     
     #define LOG_PL	       		0x00000002		Logging PhraseLock.m
     #define LOG_FDO_RW			0x00000004		Logging of CTAP1/CTAP2 Messages
     #define LOG_KEYS			0x00000008		Logging of keys exchanged! Do not set in release-versions!!!
     
     #define LOG_CTAP			0x00000010		Deep logging of CTAP1/CTAP2 communication
     #define LOG_CBOR			0x00000020		Deep logging of CBOR
     #define LOG_DMP			    0x00000040		Deep logging of dumps
     
     #define LOG_INIT            0x00000080		Logging of BLE init-prozess
     #define LOG_BLE_RW          0x00000100   	Extended logging of BLE communication
     #define LOG_BLE_WRITE_ACK   0x00000200   	write data ack received
     #define LOG_DGB_BLE         0x00000400   	Logging of full BLE communication
     #define LOG_DGB_BLE_FINE    0x00000800	 	Deep logging of BLE communication
     */
    
    /* ***************** PhraseLock Initialisation END  ***************** */
    
    alc = new Application.ActivityLifecycleCallbacks()
    {
      
      @Override
      public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState)
      {
      }
      
      @Override
      public void onActivityStarted(@NonNull Activity activity)
      {
        isForground = true;
      }
      
      @Override
      public void onActivityResumed(@NonNull Activity activity)
      {
      }
      
      @Override
      public void onActivityPaused(@NonNull Activity activity)
      {
      }
      
      @Override
      public void onActivityStopped(@NonNull Activity activity)
      {
        isForground = false;
      }
      
      @Override
      public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState)
      {
      }
      
      @Override
      public void onActivityDestroyed(@NonNull Activity activity)
      {
      }
    };
    
    getApplication().registerActivityLifecycleCallbacks(alc);
  }
  
  public static byte[] readByteArrayFile(Context context, String fname)
  {
    try {
      Resources resources = context.getResources();
      InputStream iS = resources.getAssets().open(fname);
      int size = iS.available();
      byte[] buffer = new byte[size]; //declare the size of the byte array with size of the file
      iS.read(buffer); //read file
      iS.close(); //close file
      return buffer;
    } catch (Exception e) {
      return null;
    }
  }
  
  
  @Override
  public PhraseLock getPL()
  {
    return ploem;
  }
  
  @Override
  public String getAAGUID()
  {
    return aagUUID;
  }
  
  @Override
  public byte[] getCoreDataForServiceUUID(ParcelUuid serviceUUID)
  {
    byte[] cd = db.getCoreDataSet(serviceUUID.getUuid().toString());
    return cd;
  }
  
  public String getCertPWD(String s)
  {
    String certPWD = new String(certPassword);
    return certPWD;
  }
  
  @Override
  public void userMessage(String extra)
  {
    self.runOnUiThread(new Runnable()
    {
      public void run()
      {
        AlertDialog.Builder builder = new AlertDialog.Builder(self);
        builder.setTitle("Phrase-Lock CTAP Message");
        builder.setMessage(extra);
        
        LayoutInflater li = LayoutInflater.from(self);
        View customView = li.inflate(R.layout.pl_alert_content, null);
        
        builder.setView(customView);
        
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
          }
        });
        builder.create().show();
      }
    });
  }
  
  @Override
  public int userAction(String extra)
  {
    if (!isForground) {
      return 1;
    }
    else {
      String syncToken = "0";
      final int[] returnResult = {-1};
      self.runOnUiThread(new Runnable()
      {
        public void run()
        {
          AlertDialog.Builder builder = new AlertDialog.Builder(self);
          builder.setTitle("Phrase-Lock User Presence");
          builder.setMessage(extra/*"Allow Login?"*/);
          
          LayoutInflater li = LayoutInflater.from(self);
          View customView = li.inflate(R.layout.pl_alert_content, null);
          
          builder.setView(customView);
          
          builder.setPositiveButton("YES", new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
              synchronized (syncToken) {
                returnResult[0] = 1;
                syncToken.notifyAll();
              }
            }
          });
          
          builder.setNegativeButton("NO", new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
              synchronized (syncToken) {
                returnResult[0] = 0;
                syncToken.notifyAll();
              }
            }
          });
          AlertDialog alertDlg = builder.create();
          alertDlg.show();
          
          /**
           This is for development purposes only. It is just
           more convenient not to hit the button every time.
           */
          Thread r1 = new Thread()
          {
            int i = 0;
            
            @Override
            public void run()
            {
              while (true) {
                i++;
                if (i == 50) {
                  alertDlg.cancel();
                  synchronized (syncToken) {
                    syncToken.notifyAll();
                  }
                  break;
                }
                try {
                  Thread.sleep(10);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              }
            }
          };
          r1.start();
        }
      });
      
      while (true) {
        synchronized (syncToken) {
          try {
            syncToken.wait();
            break;
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
      return returnResult[0];
    }
  }
  
  @Override
  public void storeAuthnStateConfig(String authConfig, String rp1, String rp2)
  {
    if (rp1 != null && rp1.length() > 0) {
      StringBuffer idx = new StringBuffer();
      idx.append(DB.GLOBAL_AUTHNDATA);
      idx.append("_");
      idx.append(rp1);
      idx.append("_");
      idx.append(rp2);
      db.setBlockdata(idx.toString(), authConfig);
    }
    else {
      db.setBlockdata(DB.GLOBAL_AUTHNDATA, authConfig);
    }
  }
  
  @Override
  public String readAuthnStateConfig(String rp1, String rp2)
  {
    String sas;
    if (rp1 != null && rp1.length() > 0) {
      StringBuffer idx = new StringBuffer();
      idx.append(DB.GLOBAL_AUTHNDATA);
      idx.append("_");
      idx.append(rp1);
      idx.append("_");
      idx.append(rp2);
      sas = db.getBlockdata(idx.toString(), "");
    }
    else {
      sas = db.getBlockdata(DB.GLOBAL_AUTHNDATA, "");
    }
    return sas;
  }
  
  @Override
  public void storeResidentKeyRecord(String rp1,
                                     String rp2,
                                     String credDomain,
                                     String credName,
                                     String uname,
                                     String userid,
                                     String dname,
                                     String rpidhash,
                                     String cridhash,
                                     String residentkey,
                                     String privkey)
  {
    db.storeResidentKeyRecord(rp1,
      credDomain,
      credName,
      uname,
      userid,
      dname,
      rpidhash,
      cridhash,
      residentkey,
      privkey);
  }
  
  public String readResidentKeys(String rp1,
                                 String rp2,
                                 String rpidHash)
  {
    return db.readResidentKeys(rp1,rpidHash);
  }
  
  public String readResidentKeys(String rp1,
                                 String rp2,
                                 String cridHash,
                                 String rpidHash)
  {
    return db.readResidentKeys(rp1, cridHash, rpidHash);
  }
  
}