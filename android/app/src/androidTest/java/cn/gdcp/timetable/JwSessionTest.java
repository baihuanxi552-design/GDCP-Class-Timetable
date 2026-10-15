package cn.gdcp.timetable;

import android.content.Context;
import android.webkit.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class JwSessionTest {
 private Context context;private WebView web;
 private void ui(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
 private void waitReleased()throws Exception{long end=System.currentTimeMillis()+5000;while(JwSession.busy()&&System.currentTimeMillis()<end)Thread.sleep(20);assertFalse(JwSession.busy());}
 @Before public void setup(){
  context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  ui(()->JwSession.setEnabled(context,true));
 }
 @After public void cleanup()throws Exception{
  if(JwSession.busy())ui(()->{JwSession.close(context,web,false);web=null;});
  waitReleased();ui(()->JwSession.setEnabled(context,false));
 }
 private void open()throws Exception{
  CountDownLatch ready=new CountDownLatch(1);
  ui(()->{assertTrue(JwSession.acquire());web=new WebView(context);JwRefresh.configure(web);JwSession.prepare(context,()->true,ready::countDown);});
  assertTrue(ready.await(5,TimeUnit.SECONDS));
 }
 private void saveFakeSession()throws Exception{
  open();CountDownLatch cookie=new CountDownLatch(1);
  ui(()->CookieManager.getInstance().setCookie(JwSession.ORIGIN,"JSESSIONID=fake-test-session; Path=/; Secure; HttpOnly",ok->{assertTrue(ok);cookie.countDown();}));
  assertTrue(cookie.await(5,TimeUnit.SECONDS));
  ui(()->{JwSession.close(context,web,true);web=null;});waitReleased();assertTrue(JwSession.hasSession(context));
 }
 @Test public void encryptedSessionSurvivesWebViewAndRestoresCookie()throws Exception{
  saveFakeSession();File file=new File(context.getNoBackupFilesDir(),"jw-session.enc");
  byte[] bytes;try(FileInputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1)out.write(b,0,n);bytes=out.toByteArray();}
  assertFalse(new String(bytes,StandardCharsets.UTF_8).contains("fake-test-session"));
  open();ui(()->assertTrue(CookieManager.getInstance().getCookie(JwSession.ORIGIN).contains("fake-test-session")));
 }
 @Test public void corruptedSnapshotRequiresLoginAndIsDeleted()throws Exception{
  saveFakeSession();File file=new File(context.getNoBackupFilesDir(),"jw-session.enc");
  try(RandomAccessFile data=new RandomAccessFile(file,"rw")){data.seek(8);int value=data.read();data.seek(8);data.write(value^1);}
  open();assertFalse(JwSession.hasSession(context));assertTrue(JwSession.prefs(context).getBoolean("jwSyncNeedsLogin",false));
 }
 @Test public void disablingDeletesEncryptedSnapshot()throws Exception{
  saveFakeSession();ui(()->JwSession.setEnabled(context,false));
  assertFalse(JwSession.enabled(context));assertFalse(JwSession.hasSession(context));
 }
 @Test public void cookiePathScopesSurviveRestore()throws Exception{
  open();CountDownLatch cookies=new CountDownLatch(2);
  ui(()->{
   CookieManager manager=CookieManager.getInstance();
   manager.setCookie(JwSession.ORIGIN,"JSESSIONID=outer; Path=/; Secure; HttpOnly",ok->cookies.countDown());
   manager.setCookie(JwSession.ORIGIN,"JSESSIONID=inner; Path=/jsxsd/; Secure; HttpOnly",ok->cookies.countDown());
  });
  assertTrue(cookies.await(5,TimeUnit.SECONDS));
  ui(()->{JwSession.close(context,web,true);web=null;});waitReleased();open();
  ui(()->{
   String root=CookieManager.getInstance().getCookie(JwSession.ORIGIN+"/");
   String school=CookieManager.getInstance().getCookie(JwSession.ORIGIN+"/jsxsd/");
   assertTrue(root.contains("outer"));assertFalse(root.contains("inner"));assertTrue(school.startsWith("JSESSIONID=inner"));
  });
 }
 @Test public void schoolOriginIsRestricted(){
  assertTrue(JwRefresh.allowed("https://jw.gdcp.edu.cn/jsxsd/"));
  assertFalse(JwRefresh.allowed("http://jw.gdcp.edu.cn/jsxsd/"));
  assertFalse(JwRefresh.allowed("https://jw.gdcp.edu.cn.evil.example/"));
  assertFalse(JwRefresh.allowed("https://jw.gdcp.edu.cn:444/jsxsd/"));
  assertFalse(JwRefresh.allowed("javascript:alert(1)"));
 }
 @Test public void onlyOneWebViewSessionCanRun()throws Exception{
  open();assertFalse(JwSession.acquire());
 }
}
