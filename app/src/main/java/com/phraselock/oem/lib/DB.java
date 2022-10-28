package com.phraselock.oem.lib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

@SuppressLint("DefaultLocale")
public class DB extends SQLiteOpenHelper {
	
	public static String  	  DBGLEVEL             = "PLNK_DEBUG";
	final static public String ATOMIC_COUNTER       = "ATOMIC_COUNTER";
	final static public String GLOBAL_AUTHNDATA     = "GLOBAL_AUTHNDATA";
	final static public String CORE_DATA            = "CORE_DATA_";

	private static final String DB_NAME             = "phraselock_db"; // the name of our database
	private static final int DB_VERSION             = 1; // the version of the sqlite database
	private Context context;
	
	/*
	Possible USB-Modes
	 */
	public interface USBMODE
	{
		int RETURN_USB_MODE 	= 0x00;		// NO USB - Not yet implemented
		int HID_USB 			= 0x01;		// HID Keyboard
		int FDO_USB 			= 0x02;		// FIDO Support
		int CPD_USB 			= 0x03;		// Composit Device Support (KBD_USB | KBD_USB)
	};
	
	public DB(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

	public void initOnStartDB(Context context) {
		// Block-Data
		checkBlockDataExists(false);
		
		// Resident Credentials
		checkResidentCredDataExists(false);
	}
	
	public String dumpTableJSON(String tablename){
		StringBuilder sb = new StringBuilder();
		try {
			SQLiteDatabase sql = getReadableDatabase();
			String cmd = String.format("SELECT *from '%s';", tablename);
			Cursor c = sql.rawQuery(cmd, null);
			dumpSQLResultJSON(c,sb,tablename);
			c.close();
			sql.close();
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception in dumpTableJSON: " + e.getMessage()+" / "+e.toString());
		}
		return sb.toString();
	}
	
	void dumpSQLResultJSON(Cursor c, StringBuilder sb, String tableName)
	{
		sb.append("{\"");
		sb.append(tableName);
		sb.append("\":[");
		if (c!=null){
			if (c!=null){
				boolean bCursor = c.moveToFirst();
				while(bCursor) {
					String[] cn = c.getColumnNames();
					sb.append("{");
					for(int i=0; i< cn.length; i++){
						String n = cn[i];
						String v = c.getString(i);
						if(n!=null && v!=null){
							sb.append("\"");
							sb.append(n);
							sb.append("\"");
							sb.append(":");
							sb.append("\"");
							sb.append(v);
							sb.append("\"");
							if (i < cn.length - 1) {
								sb.append(",");
							}
						}
					}
					sb.append("}");
					bCursor = c.moveToNext();
					if(bCursor) {
						sb.append(",");
					}
				}
			}
		}
		sb.append("]}");
	}
	
	public void checkBlockDataExists(boolean reset){
		try {
			SQLiteDatabase sql = getReadableDatabase();
			Cursor c = sql.query("sqlite_master",
					new String[]{"name"}, "type='table' AND name='blockdata'",
					null, null, null, null);
			if (c!=null ){
				int ccount = c.getCount();
				if(ccount==0 || reset){
					dbBlockDataReset();
					Log.d(DB.DBGLEVEL, "Table created: " + "blockdata");
				}
			}
			c.close();
			sql.close();
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception: checkBlockDataExists: " + e.getMessage()+" / "+e.toString());
		}
	}

	public void dbBlockDataReset(){
		try {
			SQLiteDatabase sql = getWritableDatabase();
			sql.execSQL("DROP TABLE IF EXISTS blockdata;");
			sql.execSQL("CREATE TABLE IF NOT EXISTS blockdata (idx TEXT NOT NULL PRIMARY KEY, data TEXT);");
			sql.close();
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception: dbBlockDataReset: " + e.getMessage()+" / "+e.toString());
		}
	}
	
	public void checkResidentCredDataExists(boolean reset){
		try {
			SQLiteDatabase sql = getReadableDatabase();
			Cursor c = sql.query("sqlite_master",
					new String[]{"name"}, "type='table' AND name='residentCredData'",
					null, null, null, null);
			if (c!=null){
				int ccount = c.getCount();
				if(ccount==0 || reset){
					dbResidentCredDataReset();
					Log.d(DB.DBGLEVEL, "Table created: " + "category");
				}
			}
			c.close();
			sql.close();
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception on checkResidentCredDataExists: " + e.getMessage()+" / "+e.toString());
		}
	}
	
	public void dbResidentCredDataReset(){
		try {
			SQLiteDatabase sql = getWritableDatabase();
			sql.execSQL("DROP TABLE IF EXISTS residentCredData;");
			sql.execSQL("DROP INDEX IF EXISTS residentCredData_idx01;");
			sql.execSQL("DROP INDEX IF EXISTS residentCredData_idx02;");
			sql.execSQL("DROP INDEX IF EXISTS residentCredData_idx03;");
			
			sql.execSQL("CREATE TABLE IF NOT EXISTS residentCredData ( " +
					"idx            INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
					"userid		    TEXT DEFAULT '-', " +
					"uname          TEXT DEFAULT '-', " +
					"dname          TEXT DEFAULT '-', " +
					"rpidhash       TEXT DEFAULT '-', " +
					"cridhash       TEXT DEFAULT '-', " +
					"residentkey    TEXT DEFAULT '-', " +
					"privkey        TEXT DEFAULT '-' );"
			);
			
			sql.execSQL("CREATE UNIQUE INDEX residentCredData_idx01 ON residentCredData (cridhash,rpidhash);");
			sql.execSQL("CREATE INDEX residentCredData_idx02 ON residentCredData (cridhash);");
			sql.execSQL("CREATE UNIQUE INDEX residentCredData_idx03 ON residentCredData (userid);");
			
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception on dbResidentCredDataReset: " + e.getMessage()+" / "+e.toString());
		}
	}
	
	public void delete_All_residentCredData(){
		try {
			SQLiteDatabase sql = getWritableDatabase();
			String cmd = String.format("DELETE from residentCredData;");
			sql.execSQL(cmd);
			sql.close();
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception on delete_All_residentCredData " + e.getMessage()+" / "+e.toString());
		}
	}
	
	public boolean setBlockdata(String key, String data){
		try {
			SQLiteDatabase sql = getWritableDatabase();
			String cmd = String.format("INSERT OR REPLACE INTO blockdata (idx, data) VALUES ('%s','%s')", key, data);
			sql.execSQL(cmd);
			sql.close();
			return true;
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception on setBlockdata " + e.getMessage()+" / "+e.toString());
		}
		return false;
	}

	public boolean setBlockdata(String key, byte[] data){
		try {
			String s = DB.byteArrayToHexString(data);
			return setBlockdata(key,s);
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception on setBlockdata " + e.getMessage()+" / "+e.toString());
		}
		return false;
	}

	public boolean setBlockdata(String key, long dataUint32_t){
		try {
			String data = Long.toString(dataUint32_t);
			return setBlockdata(key,data);
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception on setBlockdata " + e.getMessage()+" / "+e.toString());
		}
		return false;
	}
	
	public String getBlockdata(String key, String deflt){
		try {
			SQLiteDatabase sql = getReadableDatabase();
			String cmd = String.format("SELECT * from blockdata where idx='%s';", key);
			Cursor c = sql.rawQuery(cmd, null);
			if (c!=null){
				if(c.moveToFirst()) {
					String[] cn = c.getColumnNames();
					String res = c.getString(1);
					if (res != null) {
						c.close();
						sql.close();
						return res;
					}
				}
			}
			c.close();
			sql.close();
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception on getBlockdata: " + e.getMessage()+" / "+e.toString());
		}
		if(deflt!=null){
			setBlockdata(key,deflt);
			return deflt;
		}
		return null;
	}
	
	public String getBlockdataAsString(String key, String deflt){
		try {
			SQLiteDatabase sql = getReadableDatabase();
			String cmd = String.format("SELECT * from blockdata where idx='%s';", key);
			Cursor c = sql.rawQuery(cmd, null);
			if (c!=null){
				if(c.moveToFirst()) {
					String[] cn = c.getColumnNames();
					String res = c.getString(1);
					if (res != null) {
						c.close();
						sql.close();
						return res;
					}
				}
			}
			c.close();
			sql.close();
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception on getBlockdataAsString: " + e.getMessage()+" / "+e.toString());
		}
		if(deflt!=null){
			setBlockdata(key,deflt);
			return deflt;
		}
		return null;
	}
	
	public byte[] getBlockdata(String key, byte[] data){
		try {
			SQLiteDatabase sql = getReadableDatabase();
			String cmd = String.format("SELECT * from blockdata where idx='%s';", key);
			Cursor c = sql.rawQuery(cmd, null);
			if (c!=null){
				if(c.moveToFirst()) {
					String[] cn = c.getColumnNames();
					String res = c.getString(1);
					if (res != null) {
						c.close();
						sql.close();
						return DB.hexStringToByteArray(res);
					}
				}
			}
			c.close();
			sql.close();
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception on getBlockdata: " + e.getMessage()+" / "+e.toString());
		}
		if(data!=null){
			String s = DB.byteArrayToHexString(data);
			setBlockdata(key,s);
			return data;
		}
		return null;
	}

	public long getBlockdata(String key, long dataUint32_t){
		try {
			SQLiteDatabase sql = getReadableDatabase();
			String cmd = String.format("SELECT * from blockdata where idx='%s';", key);
			Cursor c = sql.rawQuery(cmd, null);
			if (c!=null){
				if(c.moveToFirst()) {
					String[] cn = c.getColumnNames();
					String res = c.getString(1);
					if (res != null) {
						c.close();
						sql.close();
						return Long.parseLong(res);
					}
				}
			}
			c.close();
			sql.close();
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception on getBlockdata: " + e.getMessage()+" / "+e.toString());
		}
		String s = Long.toString(dataUint32_t);
		setBlockdata(key,s);
		return dataUint32_t;
	}

	public void deleteBlockdata(String key){
		try {
			SQLiteDatabase sql = getWritableDatabase();
			String cmd = String.format("DELETE from blockdata where idx='%s';", key );
			sql.execSQL(cmd);
			sql.close();
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception on deleteBlockdata " + e.getMessage()+" / "+e.toString());
		}
	}

	public void storeResidentKeyRecord(String uname,
	                                   String userid,
	                                   String dname,
	                                   String rpidhash,
	                                   String cridhash,
	                                   String residentkey,
	                                   String privkey)
	{
		try{
			String query = String.format("INSERT OR REPLACE INTO residentCredData " +
							" (uname, userid, dname, rpidhash, cridhash, residentkey, privkey) " +
							" VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s');",
					uname, userid, dname, rpidhash, cridhash, residentkey, privkey);
			SQLiteDatabase sql = getWritableDatabase();
			sql.execSQL(query);
			sql.close();
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception storeResidentKeyRecord: " + e.getMessage()+" / "+e.toString());
		}
	}
	
	public String readResidentKeys(String rpidHash)
	{
		StringBuilder sb = new StringBuilder();
		try{
			String cmd = String.format("SELECT residentkey, privkey from residentCredData where rpidhash='%s';",rpidHash);
			SQLiteDatabase sql = getReadableDatabase();
			Cursor c = sql.rawQuery(cmd, null);
			dumpSQLResultJSON(c,sb,"rklist");
			c.close();
			sql.close();
		}catch (Exception ignored){
		}
		return sb.toString();
	}
	
	public String readResidentKeys(String cridHash, String rpidHash)
	{
		StringBuilder sb = new StringBuilder();
		try{
			String cmd = String.format("SELECT residentkey, privkey from residentCredData where cridhash='%s' and rpidhash='%s';",cridHash,rpidHash);
			SQLiteDatabase sql = getReadableDatabase();
			Cursor c = sql.rawQuery(cmd, null);
			dumpSQLResultJSON(c,sb,"rklist");
			c.close();
			sql.close();
		}catch (Exception ignored){
		}
		return sb.toString();
	}
	
	public void setCoreDataSet(String ltc, byte[] cd)
	{
		String idx = CORE_DATA +ltc;
		String cds = DB.byteArrayToHexString(cd);
		deleteBlockdata(idx);
		setBlockdata(idx,cds);
	}

	public byte[] getCoreDataSet(String ltc)
	{
		String idx = CORE_DATA +ltc;
		SQLiteDatabase sql = getReadableDatabase();
		String cds = getBlockdataAsString(idx.toUpperCase(),null);
		if(cds!=null) {
			return DB.hexStringToByteArray(cds);
		}
		return null;
	}

	public ArrayList getAllBlockDataWithIndicesLike(String idxStart){
		ArrayList ars = null;
		try {
			String cmd = String.format("SELECT * from blockdata where idx like '%s%%';",idxStart);
			SQLiteDatabase sql = getReadableDatabase();
			Cursor c = sql.rawQuery(cmd, null);
			int idx = 0;
			if (c!=null){
				ars = new ArrayList();
				boolean bCursor = c.moveToFirst();
				while(bCursor) {
					
					String n = c.getString(0);
					String v = c.getString(1);
					if(v!=null){
						ars.add(DB.hexStringToByteArray(v));
					}
					bCursor = c.moveToNext();
				}
			}
			c.close();
			sql.close();
		}catch (Exception e){
			Log.d(DB.DBGLEVEL, "DB-Exception in getAllBlockDataWithIndicesLike: " + e.getMessage()+" / "+e.toString());
		}
		return ars;
	}
	
	public ArrayList getAllCoreDataSets()
	{
		ArrayList coreDataArray = getAllBlockDataWithIndicesLike(CORE_DATA);
		return coreDataArray;
	}
	
	public static byte[] hexStringToByteArray(String s) {
		s = s.toUpperCase();
		int len = s.length();
		byte[] data = new byte[len/2];
		for(int i = 0; i < len; i+=2){
			data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
	
	final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	public static String byteArrayToHexString(byte[] bytes) {
		char[] hexChars = new char[bytes.length*2];
		int v;
		for(int j=0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j*2] = hexArray[v>>>4];
			hexChars[j*2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	public static String byteArrayToHexString(byte[] bytes, int len) {
		int v;
		if(len>bytes.length) len = bytes.length;
		char[] hexChars = new char[len*2];
		for(int j=0; j < len; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j*2] = hexArray[v>>>4];
			hexChars[j*2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	public static String charArrayToHexString(char[] bytes) {
		char[] hexChars = new char[bytes.length*2];
		int v;
		for(int j=0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j*2] = hexArray[v>>>4];
			hexChars[j*2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	public static byte[] revertByteArray(byte[] data) {
		byte[] tmp = new byte[data.length];
		for (int i = 0; i < data.length; i++) {
			tmp[i] = data[data.length-1 -i];
		}
		return tmp;
	}
	
	public static char[] revertCharArray(char[] data) {
		char[] tmp = new char[data.length];
		for (int i = 0; i < data.length; i++) {
			tmp[i] = data[data.length-1 -i];
		}
		return tmp;
	}
	
	public static char[] byteArray2CharArray(byte[] data) {
		char[] tmp = new char[data.length];
		for (int i = 0; i < data.length; i++) {
			tmp[i] = (char)(data[i]);
		}
		return tmp;
	}
	
	public static byte[] charArray2byteArray(char[] data) {
		byte[] tmp = new byte[data.length];
		for (int i = 0; i < data.length; i++) {
			tmp[i] = (byte)(data[i]);
		}
		return tmp;
	}
	
	public static String getGuidFromByteArray(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		long high = bb.getLong();
		long low = bb.getLong();
		UUID uuid = new UUID(high, low);
		return uuid.toString();
	}

}
