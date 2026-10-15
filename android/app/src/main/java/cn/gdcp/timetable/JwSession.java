package cn.gdcp.timetable;

import android.content.*;
import android.security.keystore.*;
import android.util.AtomicFile;
import android.webkit.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Only an encrypted cookie snapshot is retained between WebView sessions. */
public final class JwSession {
 public static final String ORIGIN="https://jw.gdcp.edu.cn";
 private static final String ALIAS="gdcp.jw.session.v1";
 private static boolean owned;
 public static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("school",Context.MODE_PRIVATE);}
 public static boolean enabled(Context c){return prefs(c).getBoolean("jwSyncEnabled",false);}
 public static long generation(Context c){return prefs(c).getLong("jwSyncGeneration",0);}
 public static synchronized boolean acquire(){if(owned)return false;owned=true;return true;}
 public static synchronized boolean busy(){return owned;}
 private static synchronized void release(){owned=false;}
 private static AtomicFile file(Context c){return new AtomicFile(new File(c.getNoBackupFilesDir(),"jw-session.enc"));}
 public static boolean hasSession(Context c){return file(c).getBaseFile().exists();}
 public static void message(Context c,String text){prefs(c).edit().putString("jwSyncMessage",text).apply();}
 public static void setEnabled(Context c,boolean value){
  if(!prefs(c).edit().putBoolean("jwSyncEnabled",value).putLong("jwSyncGeneration",generation(c)+1)
    .putBoolean("jwSyncNeedsLogin",false).remove("jwSyncPending").remove("jwSyncPendingBase").remove("jwSyncPendingAt")
    .putLong("jwSyncAttempt",0).commit())throw new IllegalStateException("无法保存自动更新设置");
  if(!value){forget(c);if(!busy()){CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();}}
  message(c,value?"已开启，请登录教务系统并确认导入，以保存本机登录会话。":"自动更新已关闭，已删除保存的登录会话。");
 }
 public static void forget(Context c){file(c).delete();}
 public static void loginRequired(Context c){
  forget(c);prefs(c).edit().putBoolean("jwSyncNeedsLogin",true).commit();
  message(c,"登录已过期，请重新登录教务系统。原课表已保留。");
 }
 private static javax.crypto.SecretKey key()throws Exception{
  KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
  if(store.containsAlias(ALIAS))return (javax.crypto.SecretKey)store.getKey(ALIAS,null);
  KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());
  return gen.generateKey();
 }
 private static String read(Context c)throws Exception{
  if(!enabled(c)||!hasSession(c))return "";
  try(DataInputStream in=new DataInputStream(file(c).openRead())){
   int n=in.readInt();if(n!=12)throw new IOException();
   byte[] iv=new byte[n];in.readFully(iv);int length=in.readInt();if(length<16||length>65536)throw new IOException();
   byte[] data=new byte[length];in.readFully(data);
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));
   return new String(cipher.doFinal(data),StandardCharsets.UTF_8);
  }
 }
 private static void save(Context c)throws Exception{
  Map<String,String> cookies=new LinkedHashMap<>();
  for(String path:new String[]{"/","/jsxsd/","/jsxsd/framework/"}) {
   String header=CookieManager.getInstance().getCookie(ORIGIN+path);
   if(header!=null)for(String part:header.split(";")){String pair=part.trim();int eq=pair.indexOf('=');if(eq>0&&!pair.contains("\r")&&!pair.contains("\n"))cookies.put(pair.substring(0,eq),pair);}
  }
  if(cookies.isEmpty())throw new IOException("missing session");
  String header=String.join("; ",cookies.values());if(header.length()>32000)throw new IOException();
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
  byte[] data=cipher.doFinal(header.getBytes(StandardCharsets.UTF_8));
  AtomicFile target=file(c);FileOutputStream out=null;
  try{out=target.startWrite();DataOutputStream stream=new DataOutputStream(out);byte[] iv=cipher.getIV();stream.writeInt(iv.length);stream.write(iv);stream.writeInt(data.length);stream.write(data);stream.flush();target.finishWrite(out);}
  catch(Exception e){if(out!=null)target.failWrite(out);throw e;}
 }
 /** Must be called on the main thread after acquire(). Clear any stale native cookies first. */
 public static void prepare(Context c,java.util.function.BooleanSupplier alive,Runnable ready){
  CookieManager manager=CookieManager.getInstance();manager.setAcceptCookie(true);
  manager.removeAllCookies(ignored->{
   if(!alive.getAsBoolean())return;
   String header="";
   try{header=read(c);}catch(Exception e){forget(c);prefs(c).edit().putBoolean("jwSyncNeedsLogin",true).commit();}
   restore(c,manager,header.split(";"),0,alive,ready);
  });
 }
 private static void restore(Context c,CookieManager manager,String[] parts,int index,java.util.function.BooleanSupplier alive,Runnable ready){
  if(!alive.getAsBoolean())return;
  if(index>=parts.length){manager.flush();ready.run();return;}
  String pair=parts[index].trim();
  if(pair.indexOf('=')<=0){restore(c,manager,parts,index+1,alive,ready);return;}
  manager.setCookie(ORIGIN,pair+"; Path=/; Secure; HttpOnly; SameSite=Lax",ok->{
   if(Boolean.TRUE.equals(ok))restore(c,manager,parts,index+1,alive,ready);
   else {forget(c);prefs(c).edit().putBoolean("jwSyncNeedsLogin",true).commit();manager.removeAllCookies(ignored->{if(alive.getAsBoolean())ready.run();});}
  });
 }
 /** Clear WebView's plaintext cookie/cache storage even when keeping the encrypted snapshot. */
 public static void close(Context c,WebView web,boolean keep){
  if(keep&&enabled(c)){
   try{save(c);prefs(c).edit().putBoolean("jwSyncNeedsLogin",false).commit();}
   catch(Exception e){forget(c);prefs(c).edit().putBoolean("jwSyncNeedsLogin",true).commit();message(c,"无法安全保存登录会话，请重新登录。原课表已保留。");}
  }
  if(web!=null){if(web.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)web.getParent()).removeView(web);web.stopLoading();web.clearCache(true);web.clearHistory();web.removeAllViews();web.destroy();}
  WebStorage.getInstance().deleteAllData();
  CookieManager.getInstance().removeAllCookies(ignored->{CookieManager.getInstance().flush();release();prefs(c).edit().putLong("jwSyncUiTick",System.nanoTime()).apply();});
 }
}
