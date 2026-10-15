package cn.gdcp.timetable;
import android.os.*;
import android.webkit.*;
import android.view.*;
import org.json.*;

public final class JwImportActivity extends androidx.activity.ComponentActivity {
 private WebView web;private JwImportUi ui;private boolean busy,sessionOwned,imported;
 private final Handler handler=new Handler(Looper.getMainLooper());
 @Override public void onCreate(Bundle state){
  super.onCreate(state);getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);
  ui=new JwImportUi();web=new WebView(this);ui.mount(this,web,()->extract());JwRefresh.configure(web);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !JwRefresh.allowed(r.getUrl().toString());}
   @Override public void onReceivedSslError(WebView v,SslErrorHandler h,android.net.http.SslError e){h.cancel();ui.updateMessage("教务系统证书验证失败，请稍后重试。");}
   @Override public void onPageFinished(WebView v,String url){ui.updateMessage("首页个人课表加载后点“解析个人课表”。个人课表以实际选课为准。");}
  });open(0);
 }
 private void open(int retry){
  if(isFinishing()||isDestroyed())return;
  if(!JwSession.acquire()){
   if(retry<30)handler.postDelayed(()->open(retry+1),100);else ui.updateMessage("教务刷新尚未结束，请返回后重试。");
   return;
  }
  sessionOwned=true;
  JwSession.prepare(this,()->!isFinishing()&&!isDestroyed(),()->{if(!isFinishing()&&!isDestroyed())web.loadUrl(JwSession.ORIGIN+"/");});
 }
 private void extract(){
  if(busy||!sessionOwned||!JwRefresh.allowed(web.getUrl()))return;busy=true;ui.updateBusy(true);
  try{web.evaluateJavascript("(function(){"+JwRefresh.parser(this)+";window.__gdcpImportResult=null;GDCPParser.collect(document).then(data=>{window.__gdcpImportResult=JSON.stringify({ok:true,data:data})}).catch(e=>{window.__gdcpImportResult=JSON.stringify({ok:false,error:e.message})});return true})()",ignored->poll(0));}
  catch(Exception e){busy=false;ui.updateBusy(false);ui.updateMessage("解析器读取失败。");}
 }
 private void poll(int attempt){
  if(isFinishing()||isDestroyed())return;
  web.evaluateJavascript("window.__gdcpImportResult||null",raw->{
   if(isFinishing()||isDestroyed())return;
   if(raw==null||raw.equals("null")){
    if(attempt>=100){busy=false;ui.updateBusy(false);ui.updateMessage("读取超时，请确认已登录且首页课表加载完成，再重试。");}
    else handler.postDelayed(()->poll(attempt+1),150);return;
   }
   busy=false;ui.updateBusy(false);
   try{
    JSONObject result=new JSONObject(new JSONArray("["+raw+"]").getString(0));
    if(!result.optBoolean("ok"))throw new JSONException(result.optString("error","解析失败"));
    JSONObject clean=PersonalImport.validate(result.getJSONObject("data").toString());JSONArray rows=clean.getJSONArray("courses");int slots=0;
    for(int i=0;i<rows.length();i++)slots+=rows.getJSONObject(i).getJSONArray("weeks").length();
    ui.showPreview(rows.length()+" 条排课规则 · "+slots+" 个课程时段",()->{
     try{PersonalImport.save(this,clean.toString());imported=true;
      JwSession.prefs(this).edit().putBoolean("jwSyncNeedsLogin",false).putLong("jwSyncSuccess",System.currentTimeMillis()).putLong("jwSyncAttempt",System.currentTimeMillis()).commit();
      JwSession.message(this,JwSession.enabled(this)?"个人课表已导入，打开应用时将自动检查更新。":"个人课表已导入。");finish();
     }catch(Exception e){ui.updateMessage("保存失败，请重新解析。");}
    });
   }catch(Exception e){ui.updateMessage("未导入："+e.getMessage());}
  });
 }
 @Override public void onBackPressed(){if(ui.dismissPreview())return;if(web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override protected void onDestroy(){
  handler.removeCallbacksAndMessages(null);
  if(sessionOwned)JwSession.close(this,web,imported);else if(web!=null)web.destroy();
  web=null;super.onDestroy();
 }
}
