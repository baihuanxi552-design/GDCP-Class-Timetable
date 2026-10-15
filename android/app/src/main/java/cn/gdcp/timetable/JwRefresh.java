package cn.gdcp.timetable;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Foreground-only refresh; never opens a login form or changes data on a failed request. */
public final class JwRefresh {
 private final Activity activity;private final Handler handler=new Handler(Looper.getMainLooper());
 private WebView web;private boolean running,parsing,finished;private long epoch,sessionWaitStarted;private String baseline;
 private final Runnable timeout=()->fail("刷新超时，请检查网络后重试。原课表已保留。",false);
 public JwRefresh(Activity a){activity=a;}
 public static boolean allowed(String url){try{Uri u=Uri.parse(url);return "https".equals(u.getScheme())&&"jw.gdcp.edu.cn".equals(u.getHost())&&(u.getPort()==-1||u.getPort()==443);}catch(Exception e){return false;}}
 public static void configure(WebView w){
  WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);
  s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSaveFormData(false);s.setCacheMode(WebSettings.LOAD_NO_CACHE);
  CookieManager.getInstance().setAcceptThirdPartyCookies(w,false);
 }
 public void start(boolean manual){
  if(activity.isFinishing()||activity.isDestroyed()||running)return;
  if(JwSession.busy()){
   if(manual)JwSession.message(activity,"正在处理教务页面，请稍候。");
   else {
    long now=android.os.SystemClock.elapsedRealtime();
    if(sessionWaitStarted==0)sessionWaitStarted=now;
    if(now-sessionWaitStarted<5000)handler.postDelayed(()->start(false),50);
    else {sessionWaitStarted=0;JwSession.message(activity,"教务页面仍在关闭，请稍后点立即刷新。");}
   }
   return;
  }
  sessionWaitStarted=0;
  android.content.SharedPreferences p=JwSession.prefs(activity);
  if(!JwCourseDiff.canRefresh(JwSession.enabled(activity),PersonalImport.active(activity),JwSession.hasSession(activity),p.getBoolean("jwSyncNeedsLogin",false))){
   if(manual)JwSession.message(activity,"请先开启自动更新，并登录教务系统确认导入个人课表。");return;
  }
  if(p.contains("jwSyncPending")&&!JwCourseDiff.pendingFresh(p.getLong("jwSyncPendingAt",0),System.currentTimeMillis()))
   p.edit().remove("jwSyncPending").remove("jwSyncPendingBase").remove("jwSyncPendingAt").commit();
  if(!JwSession.acquire())return;
  running=true;finished=false;parsing=false;epoch=JwSession.generation(activity);baseline=p.getString("jwPersonal","");
  p.edit().putLong("jwSyncAttempt",System.currentTimeMillis()).commit();JwSession.message(activity,"正在检查全部教学周…");
  try{
   web=new WebView(activity);configure(web);web.setAlpha(0f);web.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
   ((android.view.ViewGroup)activity.getWindow().getDecorView()).addView(web,new android.view.ViewGroup.LayoutParams(1,1));
   web.setWebViewClient(new WebViewClient(){
    @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if(!allowed(r.getUrl().toString())){handler.post(()->fail("登录状态已失效，请重新登录。原课表已保留。",true));return true;}return false;}
    @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){return allowed(r.getUrl().toString())?null:new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
    @Override public void onReceivedSslError(WebView v,SslErrorHandler h,android.net.http.SslError e){h.cancel();fail("教务系统证书验证失败，原课表已保留。",false);}
    @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())fail("网络连接失败，原课表已保留。",false);}
    @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse response){if(r.isForMainFrame())fail("教务系统暂时不可用，原课表已保留。",false);}
   });
   handler.postDelayed(timeout,45000);
   JwSession.prepare(activity,()->running&&!finished,()->{
    if(!running||finished)return;
    if(JwSession.prefs(activity).getBoolean("jwSyncNeedsLogin",false)){fail("",true);return;}
    web.loadUrl(JwSession.ORIGIN+"/jsxsd/");handler.postDelayed(()->inspect(0),600);
   });
  }catch(Exception e){fail("无法启动课表刷新，原课表已保留。",false);}
 }
 private boolean current(){return running&&!finished&&!activity.isFinishing()&&!activity.isDestroyed();}
 private void inspect(int count){
  if(!current()||parsing)return;
  if(!allowed(web.getUrl())){if(count<80){handler.postDelayed(()->inspect(count+1),400);return;}fail("未能读取教务页面，原课表已保留。",false);return;}
  String state="(function check(d){try{if(d.readyState!=='complete')return 'waiting';if(d.querySelector('input[type=password]'))return 'login';if(d.querySelector('#week')&&d.querySelector('[name=kbjcmsid].layui-this'))return 'ready';if(d.querySelector('#timetable .item-box .tch-name,#timetable .kbcontent font'))return 'ready';for(const f of d.querySelectorAll('iframe,frame')){try{const s=f.contentDocument&&check(f.contentDocument);if(s&&s!=='waiting')return s}catch(e){}}}catch(e){}return 'waiting';})(document)";
  web.evaluateJavascript(state,value->{
   if(!current())return;
   if("\"login\"".equals(value)){fail("",true);return;}
   if("\"ready\"".equals(value)){parsing=true;extract();return;}
   if(count>=80)fail("未找到完整个人课表，请重新登录后检查。原课表已保留。",false);
   else handler.postDelayed(()->inspect(count+1),400);
  });
 }
 static String parser(Context c)throws IOException{
  try(InputStream in=c.getAssets().open("jw-parser.js");ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);
  }
 }
 private void extract(){
  try{web.evaluateJavascript("(function(){"+parser(activity)+";window.__gdcpRefresh=null;GDCPParser.collect(document).then(data=>{window.__gdcpRefresh=JSON.stringify({ok:true,data})}).catch(()=>{window.__gdcpRefresh=JSON.stringify({ok:false})});return true})()",ignored->poll(0));}
  catch(Exception e){fail("课表解析器不可用，原课表已保留。",false);}
 }
 private void poll(int count){
  if(!current())return;
  web.evaluateJavascript("window.__gdcpRefresh||null",raw->{
   if(!current())return;
   if(raw==null||"null".equals(raw)){if(count>=100)fail("课表读取超时，原课表已保留。",false);else handler.postDelayed(()->poll(count+1),150);return;}
   try{
    JSONObject result=new JSONObject(new JSONArray("["+raw+"]").getString(0));if(!result.optBoolean("ok"))throw new JSONException("parse");
    JSONObject clean=PersonalImport.validate(result.getJSONObject("data").toString());
    android.content.SharedPreferences p=JwSession.prefs(activity);
    if(!JwSession.enabled(activity)||epoch!=JwSession.generation(activity)||!PersonalImport.active(activity)||!Objects.equals(baseline,p.getString("jwPersonal",""))){fail("本机课表或设置已变化，本次刷新已取消。",false);return;}
    JwCourseDiff diff=JwCourseDiff.between(PersonalImport.validate(baseline),clean);
    if(diff.needsConfirmation(PersonalImport.validate(baseline))){
     if(!p.edit().putString("jwSyncPending",clean.toString()).putString("jwSyncPendingBase",baseline).putLong("jwSyncPendingAt",System.currentTimeMillis()).commit())throw new IOException();
     JwSession.message(activity,"检测到较多删课或调整："+diff.summary()+"，请在设置中确认。");
    }else if(diff.changed()){
     PersonalImport.save(activity,clean.toString());JwSession.message(activity,"课表已更新："+diff.summary()+"。自建课程已保留。");
    }else {p.edit().remove("jwSyncPending").remove("jwSyncPendingBase").remove("jwSyncPendingAt").commit();JwSession.message(activity,"检查完成，课表没有变化。");}
    p.edit().putLong("jwSyncSuccess",System.currentTimeMillis()).commit();
    finish(true);
   }catch(Exception e){fail("未取得有效的完整课表，原课表已保留。可重新登录后重试。",false);}
  });
 }
 public void cancel(){handler.removeCallbacksAndMessages(null);sessionWaitStarted=0;if(running){fail("刷新已暂停，下次打开应用可继续。原课表已保留。",false);}}
 private void fail(String text,boolean login){
  if(!running||finished)return;
  if(login)JwSession.loginRequired(activity);else JwSession.message(activity,text);
  finish(false);
 }
 private void finish(boolean keep){
  if(finished)return;finished=true;running=false;handler.removeCallbacksAndMessages(null);
  WebView old=web;web=null;if(old!=null&&old.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)old.getParent()).removeView(old);
  JwSession.close(activity,old,keep);
 }
 public void confirmPending(){
  if(running||JwSession.busy())return;
  android.content.SharedPreferences p=JwSession.prefs(activity);
  try{
   String next=p.getString("jwSyncPending",null),previous=p.getString("jwSyncPendingBase",null);
   if(next==null||!JwSession.enabled(activity)||!PersonalImport.active(activity)||!Objects.equals(previous,p.getString("jwPersonal",""))||!JwCourseDiff.pendingFresh(p.getLong("jwSyncPendingAt",0),System.currentTimeMillis()))throw new JSONException("stale");
   PersonalImport.save(activity,next);p.edit().putLong("jwSyncSuccess",System.currentTimeMillis()).commit();JwSession.message(activity,"已确认更新课表，可恢复上一份课表。");
  }catch(Exception e){dismissPending();JwSession.message(activity,"待确认结果已失效，请重新刷新。原课表已保留。");}
 }
 public void dismissPending(){JwSession.prefs(activity).edit().remove("jwSyncPending").remove("jwSyncPendingBase").remove("jwSyncPendingAt").commit();JwSession.message(activity,"已取消本次更新，原课表已保留。");}
 public void restorePrevious(){
  if(running||JwSession.busy())return;
  try{String old=JwSession.prefs(activity).getString("jwPreviousPersonal",null);if(old==null)return;PersonalImport.save(activity,old);JwSession.setEnabled(activity,false);JwSession.message(activity,"已恢复上一份课表，并关闭自动更新。");}
  catch(Exception e){JwSession.message(activity,"恢复失败，当前课表已保留。");}
 }
}
