package cn.gdcp.timetable;
import android.content.*;
import org.json.*;
import java.util.*;
public final class BackupData {
 public static void validateCatalog(JSONObject data)throws JSONException {
  JSONArray words=data.getJSONArray("strings"),classes=data.getJSONArray("classes");if(classes.length()<1||classes.length()>2000||words.length()>100000)throw new IllegalArgumentException("课表数据无效");
  for(int i=0;i<words.length();i++)if(!(words.get(i) instanceof String)||words.getString(i).length()>500)throw new IllegalArgumentException("课表文字无效");
  Set<String> names=new HashSet<>();int count=0;
  for(int i=0;i<classes.length();i++){JSONObject c=classes.getJSONObject(i);String name=c.getString("name");if(name.isEmpty()||name.length()>500||!names.add(name))throw new IllegalArgumentException("课表无效");JSONArray rows=c.getJSONArray("courses");for(int j=0;j<rows.length();j++){JSONArray r=rows.getJSONArray(j);if(++count>100000||r.length()!=6)throw new IllegalArgumentException("课程数据无效");for(int k=0;k<6;k++)if(!(r.get(k) instanceof Integer))throw new IllegalArgumentException("课程数值无效");for(int k=0;k<3;k++)if(r.getInt(k)<0||r.getInt(k)>=words.length())throw new IllegalArgumentException("课程索引无效");if(r.getInt(3)<1||r.getInt(3)>7||r.getInt(4)<1||r.getInt(4)>11||r.getInt(5)<1||r.getInt(5)>1048575)throw new IllegalArgumentException("课程时间无效");}}
  JSONObject projects=data.optJSONObject("teacherProjects");if(projects!=null)for(Iterator<String> it=projects.keys();it.hasNext();){Object v=projects.get(it.next());if(!(v instanceof String)||((String)v).length()>500)throw new IllegalArgumentException("体育项目无效");}
 }
 public static String exportData(Context c)throws JSONException {
  SharedPreferences p=c.getSharedPreferences("school",Context.MODE_MULTI_PROCESS);
  JSONObject settings=new JSONObject();settings.put("className",SchoolData.NAMES.get(SchoolData.selected));settings.put("campus",SchoolData.campus);
  JSONObject groups=new JSONObject(),custom=new JSONObject();
  for(String name:SchoolData.NAMES){String g=p.getString("groups:"+name,null),x=p.getString("custom:"+name,null);if(g!=null)groups.put(name,new JSONObject(g));if(x!=null)custom.put(name,new JSONArray(x));}
  JSONObject root=new JSONObject();root.put("format","gdcp-timetable-backup");root.put("version",1);root.put("semester","2026-2027-1");root.put("settings",settings);root.put("groups",groups);root.put("custom",custom);root.put("catalog",SchoolData.catalog());return root.toString(2);
 }
 public static JSONObject validate(String text)throws JSONException {
  if(text.length()>4*1024*1024)throw new IllegalArgumentException("备份文件过大");
  JSONObject root=new JSONObject(text);
  if(!"gdcp-timetable-backup".equals(root.getString("format"))||root.getInt("version")!=1||!"2026-2027-1".equals(root.getString("semester")))throw new IllegalArgumentException("备份格式或学期不匹配");
  JSONObject data=root.optJSONObject("catalog");if(data!=null)validateCatalog(data);Set<String> names=new HashSet<>();if(data==null)names.addAll(SchoolData.NAMES);else{JSONArray cs=data.getJSONArray("classes");for(int i=0;i<cs.length();i++)names.add(cs.getJSONObject(i).getString("name"));}JSONObject settings=root.getJSONObject("settings");known(settings.getString("className"),names);int campus=settings.getInt("campus");if(campus<0||campus>3)throw new IllegalArgumentException("备份校区无效");
  JSONObject groups=root.getJSONObject("groups"),custom=root.getJSONObject("custom");
  for(Iterator<String> it=groups.keys();it.hasNext();){String name=it.next();known(name,names);JSONObject choices=groups.getJSONObject(name);for(Iterator<String> keys=choices.keys();keys.hasNext();){String key=keys.next();if(key.length()>200||!(choices.get(key) instanceof String)||choices.getString(key).length()>500)throw new IllegalArgumentException("分组选项无效");}}
  int total=0;
  for(Iterator<String> it=custom.keys();it.hasNext();){String name=it.next();known(name,names);JSONArray courses=custom.getJSONArray(name);total+=courses.length();if(total>10000)throw new IllegalArgumentException("自建课程数量过多");for(int i=0;i<courses.length();i++){JSONObject row=courses.getJSONObject(i);String n=row.getString("name");if(n.length()>500||row.getString("teacher").length()>500||row.getString("room").length()>500)throw new IllegalArgumentException("课程文字过长");CourseInput.validate(n,row.getInt("day"),row.getInt("first"),row.getInt("last"));JSONArray weeks=row.getJSONArray("weeks");if(weeks.length()<1||weeks.length()>20)throw new IllegalArgumentException("周次无效");Set<Integer> seen=new HashSet<>();for(int j=0;j<weeks.length();j++){int w=weeks.getInt(j);if(w<1||w>20||!seen.add(w))throw new IllegalArgumentException("周次无效或重复");}}}
  return root;
 }
 private static void known(String name,Set<String> names){if(!names.contains(name))throw new IllegalArgumentException("备份包含未识别的课表");}
 public static void restore(Context c,String text)throws JSONException {
  JSONObject root=validate(text),settings=root.getJSONObject("settings"),groups=root.getJSONObject("groups"),custom=root.getJSONObject("custom");
  SharedPreferences p=c.getSharedPreferences("school",Context.MODE_MULTI_PROCESS);SharedPreferences.Editor edit=p.edit();
  for(String key:p.getAll().keySet())if(key.startsWith("groups:")||key.startsWith("custom:"))edit.remove(key);
  for(Iterator<String> it=groups.keys();it.hasNext();){String name=it.next();edit.putString("groups:"+name,groups.getJSONObject(name).toString());}
  for(Iterator<String> it=custom.keys();it.hasNext();){String name=it.next();edit.putString("custom:"+name,custom.getJSONArray(name).toString());}
  JSONObject data=root.optJSONObject("catalog");if(data!=null)edit.putString("catalog",data.toString());String name=settings.getString("className");edit.putString("className",name).putInt("class",SchoolData.NAMES.indexOf(name)).putInt("campus",settings.getInt("campus")).putBoolean("setupCompleted",true);
  if(!edit.commit())throw new IllegalStateException("无法保存恢复数据");SchoolData.invalidate();SchoolData.initialize(c);SchoolData.hadCachedChoice=true;SchoolData.reload(c);
 }
}
