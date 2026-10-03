package cn.gdcp.timetable;
import android.app.*;
import android.os.Bundle;
import android.webkit.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
public final class JwImportActivity extends androidx.activity.ComponentActivity {
 private WebView web;private JwImportUi ui;private boolean busy=false;
 private boolean allowed(String url){try{Uri u=Uri.parse(url);return "https".equals(u.getScheme())&&"jw.gdcp.edu.cn".equals(u.getHost());}catch(Exception e){return false;}}
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);ui=new JwImportUi();web=new WebView(this);ui.mount(this,web,()->extract());WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSaveFormData(false);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !allowed(r.getUrl().toString());}@Override public void onReceivedSslError(WebView v,android.webkit.SslErrorHandler h,android.net.http.SslError e){h.cancel();ui.updateMessage("教务系统证书验证失败，请稍后重试。");}@Override public void onPageFinished(WebView v,String url){ui.updateMessage("首页个人课表加载后点“解析个人课表”。个人课表以实际选课为准。");}});
  web.loadUrl("https://jw.gdcp.edu.cn/");
 }
 private void extract(){if(busy||!allowed(web.getUrl()))return;busy=true;ui.updateBusy(true);try{java.io.InputStream input=getAssets().open("jw-parser.js");java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);input.close();String parser=new String(output.toByteArray(),StandardCharsets.UTF_8);web.evaluateJavascript("(function(){"+parser+";window.__gdcpImportResult=null;GDCPParser.collect(document).then(data=>{window.__gdcpImportResult=JSON.stringify({ok:true,data:data})}).catch(e=>{window.__gdcpImportResult=JSON.stringify({ok:false,error:e.message})});return true})()",ignored->poll(0));}catch(Exception e){busy=false;ui.updateBusy(false);ui.updateMessage("解析器读取失败。");}}
 private void poll(int attempt){if(isFinishing()||isDestroyed())return;web.evaluateJavascript("window.__gdcpImportResult||null",raw->{if(raw==null||raw.equals("null")){if(attempt>=100){busy=false;ui.updateBusy(false);ui.updateMessage("读取超时，请确认已登录且首页课表加载完成，再重试。");}else new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->poll(attempt+1),150);return;}busy=false;ui.updateBusy(false);try{String text=new JSONArray("["+raw+"]").getString(0);JSONObject result=new JSONObject(text);if(!result.optBoolean("ok"))throw new JSONException(result.optString("error","解析失败"));JSONObject clean=PersonalImport.validate(result.getJSONObject("data").toString());JSONArray rows=clean.getJSONArray("courses");int slots=0;for(int i=0;i<rows.length();i++)slots+=rows.getJSONObject(i).getJSONArray("weeks").length();ui.showPreview(rows.length()+" 条排课规则 · "+slots+" 个课程时段",()->{try{PersonalImport.save(this,clean.toString());finish();}catch(Exception e){ui.updateMessage("保存失败，请重新解析。");}});}catch(Exception e){ui.updateMessage("未导入："+e.getMessage());}});}
 @Override public void onBackPressed(){if(ui.dismissPreview())return;if(web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override protected void onDestroy(){if(web!=null){web.stopLoading();web.clearCache(true);web.clearHistory();web.destroy();android.webkit.WebStorage.getInstance().deleteAllData();}CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();super.onDestroy();}
}
