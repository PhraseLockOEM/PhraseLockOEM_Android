package com.phraselock.oem.lib;

import android.util.Base64;

import com.ipoxo.lib.ns.NSDictionary;
import com.ipoxo.lib.ns.NSObject;
import com.ipoxo.lib.ns.NSString;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.CharArrayWriter;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.UUID;

//import sun.rmi.runtime.Log;

public class FC
{
  
  private static class Range
  {
    final static public int NotFound = 0xFFFFFFFF;
    int location;
    int length;
    
    public Range()
    {
      location = NotFound;
      length = NotFound;
    }
  };
  
  public static byte[] hexStringToByteArray(String s)
  {
    s = s.toUpperCase();
    int len = s.length();
    byte[] data = new byte[len / 2];
    
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    
    return data;
  }
  
  final protected static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
  
  public static String byteArrayToHexString(byte[] bytes)
  {
    char[] hexChars = new char[bytes.length * 2];
    int v;
    
    for (int j = 0; j < bytes.length; j++) {
      v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    
    return new String(hexChars);
  }
  
  public static String byteArrayToHexString(byte[] bytes, int len)
  {
    int v;
    if (len > bytes.length) {
      len = bytes.length;
    }
    char[] hexChars = new char[len * 2];
    for (int j = 0; j < len; j++) {
      v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    
    return new String(hexChars);
  }
  
  public static String charArrayToHexString(char[] bytes)
  {
    char[] hexChars = new char[bytes.length * 2];
    int v;
    
    for (int j = 0; j < bytes.length; j++) {
      v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    
    return new String(hexChars);
  }
  
  public static byte[] revertByteArray(byte[] data)
  {
    byte[] tmp = new byte[data.length];
    for (int i = 0; i < data.length; i++) {
      tmp[i] = data[data.length - 1 - i];
    }
    return tmp;
  }
  
  public static char[] revertCharArray(char[] data)
  {
    char[] tmp = new char[data.length];
    for (int i = 0; i < data.length; i++) {
      tmp[i] = data[data.length - 1 - i];
    }
    return tmp;
  }
  
  public static char[] byteArray2CharArray(byte[] data)
  {
    char[] tmp = new char[data.length];
    for (int i = 0; i < data.length; i++) {
      tmp[i] = (char) (data[i]);
    }
    return tmp;
  }
  
  public static byte[] charArray2byteArray(char[] data)
  {
    byte[] tmp = new byte[data.length];
    for (int i = 0; i < data.length; i++) {
      tmp[i] = (byte) (data[i]);
    }
    return tmp;
  }
  
  private static Range derDecodeSequence(char[] bytes, int length, int index)
  {
    Range result = new Range();
    // Make sure we are long enough and have a sequence
    if (length - index > 2 && bytes[index] == 0x30) {
      // Make sure the input buffer is large enough
      int sequenceLength = bytes[index + 1];
      if (index + 2 + sequenceLength <= length) {
        result.location = index + 2;
        result.length = sequenceLength;
      }
    }
    return result;
  }
  
  private static Range derDecodeInteger(char[] bytes, int length, int index)
  {
    Range result = new Range();
    // Make sure we are long enough and have an integer
    if (length - index > 3 && bytes[index] == 0x02) {
      
      // Make sure the input buffer is large enough
      int integerLength = bytes[index + 1];
      if (index + 2 + integerLength <= length) {
        
        // Strip any leading zero, used to preserve sign
        if (bytes[index + 2] == 0x00) {
          result.location = index + 3;
          result.length = integerLength - 1;
          
        } else {
          result.location = index + 2;
          result.length = integerLength;
        }
      }
    }
    return result;
  }
  
  public static byte[][] derDecodeSignature(byte[] der, int keySize)
  {
    
    int length = der.length;
    char[] data = FC.byteArray2CharArray(der);
    
    // Make sure we have a sequence
    Range sequence = derDecodeSequence(data, length, 0);
    if (sequence == null) {
      return null;
    }
    
    // Extract the r value (first item)
    Range rValue = derDecodeInteger(data, length, sequence.location);
    if (rValue.location == Range.NotFound || rValue.length > keySize) {
      return null;
    }
    
    // Extract the s value (second item)
    int sStart = (int) rValue.location + (int) rValue.length;
    Range sValue = derDecodeInteger(data, length, sStart);
    if (sValue.location == Range.NotFound || sValue.length > keySize) {
      return null;
    }
    
    byte[][] RS = new byte[2][rValue.length];
    try {
      RS[0] = Arrays.copyOfRange(der, rValue.location, rValue.location + rValue.length);
      RS[1] = Arrays.copyOfRange(der, sValue.location, sValue.location + sValue.length);
    } catch (Exception e) {
      return null;
    }
    
    return RS;
  }
  
  public static byte[][] derDecodeSignatureRev(byte[] der, int keySize)
  {
    
    int length = der.length;
    char[] data = FC.byteArray2CharArray(der);
    
    // Make sure we have a sequence
    Range sequence = derDecodeSequence(data, length, 0);
    if (sequence == null) {
      return null;
    }
    
    // Extract the r value (first item)
    Range rValue = derDecodeInteger(data, length, sequence.location);
    if (rValue.location == Range.NotFound || rValue.length > keySize) {
      return null;
    }
    
    // Extract the s value (second item)
    int sStart = (int) rValue.location + (int) rValue.length;
    Range sValue = derDecodeInteger(data, length, sStart);
    if (sValue.location == Range.NotFound || sValue.length > keySize) {
      return null;
    }
    
    byte[][] RS = new byte[2][rValue.length];
    try {
      RS[0] = FC.revertByteArray(Arrays.copyOfRange(der, rValue.location, rValue.location + rValue.length));
      RS[1] = FC.revertByteArray(Arrays.copyOfRange(der, sValue.location, sValue.location + sValue.length));
    } catch (Exception e) {
      return null;
    }
    
    return RS;
  }
  
  private static char[] derEncodeInteger(char[] value)
  {
    int length = value.length;
    char[] data = new char[length];
    System.arraycopy(value, 0, data, 0, length);
    data = FC.revertCharArray(data);
    
    int outputIndex = 0;
    char[] output = new char[length + 3];
    
    output[outputIndex++] = 0x02;
    
    // Find the first non-zero entry in value
    int start = 0;
    
    while (start < length && data[start] == 0) {
      start++;
    }
    
    // Add the length and zero padding to preserve sign
    if (start == length || data[start] >= 0x80) {
      output[outputIndex++] = (char) (length - start + 1);
      output[outputIndex++] = 0x00;
    } else {
      output[outputIndex++] = (char) (length - start);
    }
    
    System.arraycopy(data, start, output, outputIndex, length - start);
    outputIndex += length - start;
    //return output;
    return Arrays.copyOfRange(output, 0, outputIndex);
  }
  
  public static byte[] derEncodeSignature(byte[] signature)
  {
    int length = signature.length;
    if (length % 2 != 0) {
      return null;
    }
    char[] rValue = derEncodeInteger(FC.byteArray2CharArray(Arrays.copyOfRange(signature, 0, length / 2)));
    char[] sValue = derEncodeInteger(FC.byteArray2CharArray(Arrays.copyOfRange(signature, length / 2, signature.length)));
    
    // Begin with the sequence tag and sequence length
    char header[] = {0, 0};
    header[0] = 0x30;
    header[1] = (char) (rValue.length + sValue.length);
    
    // This requires a long definite octet stream (signatures aren't this long)
    if (header[1] >= 0x80) {
      return null;
    }
    
    CharArrayWriter encoded = new CharArrayWriter();
    encoded.append(CharBuffer.wrap(header));
    encoded.append(CharBuffer.wrap(rValue));
    encoded.append(CharBuffer.wrap(sValue));
    
    return FC.charArray2byteArray(encoded.toCharArray());
  }
  
  public static byte[] derEncodeSignatureRev(byte[] signature)
  {
    int length = signature.length;
    if (length % 2 != 0) {
      return null;
    }
    char[] rValue = derEncodeInteger(FC.byteArray2CharArray(FC.revertByteArray(Arrays.copyOfRange(signature, 0, length / 2))));
    char[] sValue = derEncodeInteger(FC.byteArray2CharArray(FC.revertByteArray(Arrays.copyOfRange(signature, length / 2, signature.length))));
    
    // Begin with the sequence tag and sequence length
    char header[] = {0, 0};
    header[0] = 0x30;
    header[1] = (char) (rValue.length + sValue.length);
    
    // This requires a long definite octet stream (signatures aren't this long)
    if (header[1] >= 0x80) {
      return null;
    }
    
    CharArrayWriter encoded = new CharArrayWriter();
    encoded.append(CharBuffer.wrap(header));
    encoded.append(CharBuffer.wrap(rValue));
    encoded.append(CharBuffer.wrap(sValue));
    
    return FC.charArray2byteArray(encoded.toCharArray());
  }
  
  public static String dict2XML(String nodeName, NSDictionary dict)
  {
    try {
      if (dict != null) {
        StringBuilder xmlDataSB = new StringBuilder();
        FC.dumpDictToXMLString(nodeName, xmlDataSB, dict);
        String res = xmlDataSB.toString();
        return res;
      }
    } catch (Exception e) {
    }
    return null;
  }
  
  public static byte[] b64decode(String input, int flags)
  {
    try {
      if (input != null) {
        return Base64.decode(input, flags);
      }
    } catch (Exception e) {
    }
    return null;
  }
  
  public static String b64decode2String(String input, int flags)
  {
    try {
      if (input != null) {
        byte[] bx = Base64.decode(input, flags);
        if (bx != null) {
          return new String(bx);
        }
      }
    } catch (Exception e) {
    }
    return null;
  }
  
  public static String b64decode2StringNotNull(String input, int flags)
  {
    try {
      if (input != null) {
        byte[] bx = Base64.decode(input, flags);
        if (bx != null && bx.length > 0) {
          return new String(bx);
        }
      }
    } catch (Exception e) {
    }
    return "";
  }
  
  public static byte[] b64decode(byte[] input, int flags)
  {
    try {
      if (input != null) {
        return Base64.decode(input, flags);
      }
    } catch (Exception e) {
    }
    return null;
  }
  
  public static byte[] b64decode(byte[] input, int offset, int len, int flags)
  {
    try {
      if (input != null) {
        return Base64.decode(input, offset, len, flags);
      }
    } catch (Exception e) {
    }
    return null;
  }
  
  public static String b64encode2String(byte[] input, int flags)
  {
    try {
      if (input != null) {
        return Base64.encodeToString(input, flags);
      }
    } catch (Exception e) {
    }
    return null;
  }
  
  public static String b64encode2String(byte[] input, int offset, int len, int flags)
  {
    try {
      if (input != null) {
        return Base64.encodeToString(input, offset, len, flags);
      }
    } catch (Exception e) {
    }
    return null;
  }
  
  public static byte[] b64encode(byte[] input, int flags)
  {
    try {
      if (input != null) {
        return Base64.encode(input, flags);
      }
    } catch (Exception e) {
    }
    return null;
  }
  
  public static byte[] b64encode(byte[] input, int offset, int len, int flags)
  {
    try {
      if (input != null) {
        return Base64.encode(input, offset, len, flags);
      }
    } catch (Exception e) {
    }
    return null;
  }
  
  public static String makeB64Data_URL_Safe(String b64Data)
  {
    if (b64Data != null) {
      b64Data = b64Data.replace('/', '_');
      b64Data = b64Data.replace('+', '-');
    }
    return b64Data;
  }
  
  public static String makeB64Data_PKCS_Compatibel(String b64Data)
  {
    if (b64Data != null) {
      b64Data = b64Data.replace('_', '/');
      b64Data = b64Data.replace('-', '+');
    }
    return b64Data;
  }
  
  public static byte[] UUID_128_ToByteArray(UUID uuid)
  {
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }
  
  public static String getDictStringValue(NSDictionary dict, String key)
  {
    if (dict != null) {
      NSObject v = dict.objectForKey(key);
      if (v != null) {
        if (v instanceof NSString) {
          return v.toString();
        }
      }
    }
    return null;
  }
  
  public static int getDictIntValue(NSDictionary dict, String key)
  {
    if (dict != null) {
      NSObject v = dict.objectForKey(key);
      if (v != null) {
        if (v instanceof NSString) {
          return Integer.valueOf(v.toString());
        }
      }
    }
    return -1;
  }
  
  public static boolean getDictBoolValue(NSDictionary dict, String key)
  {
    if (dict != null) {
      NSObject v = dict.objectForKey(key);
      if (v != null) {
        if (v instanceof NSString) {
          int b = Integer.valueOf(v.toString());
          if (b > 0) {
            return true;
          }
        }
      }
    }
    return false;
  }
  
  
  public static void makeComplexDict(NSDictionary node, String nodeName, XmlPullParser xpp)
  {
    try {
      int eventType = xpp.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_DOCUMENT) {
          //
        } else if (eventType == XmlPullParser.START_TAG) {
          String newNodeName = xpp.getName();
          NSDictionary newNode = new NSDictionary();
          node.put(newNodeName, newNode);
          int ac = xpp.getAttributeCount();
          if (ac > 0 && newNodeName != null) {
            for (int i = 0; i < ac; i++) {
              String an = xpp.getAttributeName(i);
              String av = xpp.getAttributeValue(i);
              if (an != null && av != null) {
                av = av.trim();
                if (av != null) {
                  newNode.put(an, av);
                }
              }
            }
          }
          xpp.next();
          makeComplexDict(newNode, newNodeName, xpp);
        } else if (eventType == XmlPullParser.END_TAG) {
          return;
        } else if (eventType == XmlPullParser.TEXT) {
          if (!xpp.isWhitespace() && node != null) {
            String value = xpp.getText();
            if (value != null && nodeName != null) {
              value = value.trim();
              if (value != null && value.length() > 0) {
                node.put("_value_", value);
              }
            }
          }
        }
        eventType = xpp.next();
      }
    } catch (Exception e) {
      //if (DB.DBGEN) Log.d(DB.DBGLEVEL, "makeComplexDict::EXCEPTION: " + e.getMessage() + " " + e.toString());
    }
  }
  
  public static void dumpDictToXMLString(String nodeName, StringBuilder xmlData, NSObject obj)
  {
    
    if (nodeName != null) {
      if (nodeName.startsWith("item_")) {
        nodeName = "item";
      } else if (nodeName.startsWith("cat_")) {
        nodeName = "cat";
      } else if (nodeName.startsWith("link_")) {
        nodeName = "link";
      } else if (nodeName.startsWith("PHRASE_")) {
        nodeName = "PHRASE";
      }
    }
    
    boolean startTagClosed = false;
    String nodeValue = null;
    if (obj instanceof NSDictionary) {
      
      if (nodeName != null) {
        xmlData.append("<" + nodeName);
      }
      
      NSDictionary dict = (NSDictionary) obj;
      int cx = dict.count();
      String[] keys = dict.allKeys();
      
      // 1. Schleife nur für die Attribute
      for (int idx = 0; idx < cx; idx++) {
        String key = keys[idx];
        NSObject item = dict.objectForKey(key);
        if (item instanceof NSDictionary) {
          /*
          if(nodeName!=null && !startTagClosed ){
            xmlData.append(">");
            startTagClosed=true;
          }
          dumpDictToXMLString(key, xmlData, item);
          */
        } else if (item instanceof NSString) {
          NSString val = (NSString) item;
          if (key.equals("_value_")) {
            nodeValue = val.toString();
          } else if (val != null) {
            String content = val.toString();
            xmlData.append(" " + key + "=\"" + content + "\"");
          }
        }
      }
      
      // 2. Schleife nur eingebettete Elemete
      for (int idx = 0; idx < cx; idx++) {
        String key = keys[idx];
        NSObject item = dict.objectForKey(key);
        if (item instanceof NSDictionary) {
          if (nodeName != null && !startTagClosed) {
            xmlData.append(">");
            startTagClosed = true;
          }
          dumpDictToXMLString(key, xmlData, item);
        } else if (item instanceof NSString) {
          /*
          NSString val = (NSString)item;
          if(key.equals("_value_")){
            nodeValue = val.toString();
          }else if(val!=null){
            String content = val.toString();
            xmlData.append(" "+key+"=\""+content+"\"");
          }
          */
        }
      }
      
      if (nodeName != null) {
        if (!startTagClosed) {
          xmlData.append(">");
          startTagClosed = true;
        }
        if (nodeValue != null) {
          xmlData.append(nodeValue);
        }
        xmlData.append("</" + nodeName + ">");
      }
    }
  }
  
  public static NSDictionary makeComplexDict(String xmlData)
  {
    try {
      
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(false);
      XmlPullParser xpp = factory.newPullParser();
      
      String xmlUTF8 = new String(xmlData.getBytes(/* Charset.forName("UTF-8")*/));
      xmlUTF8 = xmlUTF8.replaceAll("&", "&amp;");
      xpp.setInput(new StringReader(xmlUTF8));
      
      NSDictionary dict = new NSDictionary();
      FC.makeComplexDict(dict, null, xpp);
      
      return dict;
      
      
    } catch (Exception e) {
      //if (DB.DBGEN) Log.d(DB.DBGLEVEL, "EXCEPTION: " + e.getMessage());
    }
    return null;
  }
  
}   // end class Base64
