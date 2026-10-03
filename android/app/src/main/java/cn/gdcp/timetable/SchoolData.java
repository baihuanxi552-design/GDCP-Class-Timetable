package cn.gdcp.timetable;
import android.content.Context;
import org.json.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
public final class SchoolData {
 public static final String[] CAMPUSES={"清远校区","天河校区","花都校区","国际校区"};
 public static final List<String> NAMES=new ArrayList<>();
 private static JSONArray entries,strings;
 private static JSONObject catalog;
 private static String loadedCatalog;
 public static JSONObject catalog(){return catalog;}
 public static synchronized void invalidate(){entries=null;strings=null;NAMES.clear();ScheduleData.COURSES=new ScheduleData.Course[0];}
 private static JSONObject teacherProjects=new JSONObject();
 public static int selected=0,campus=0;
 public static boolean hadCachedChoice=false;
 public static synchronized void initialize(Context context){
  if(entries!=null)return;
  try { java.io.InputStream in=context.getAssets().open("school-data.json");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);in.close();String saved=context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).getString("catalog",null);loadedCatalog=saved;JSONObject root=new JSONObject(saved==null?new String(out.toByteArray(),StandardCharsets.UTF_8):saved);catalog=root;teacherProjects=root.optJSONObject("teacherProjects");if(teacherProjects==null)teacherProjects=new JSONObject();entries=root.getJSONArray("classes");strings=root.getJSONArray("strings");for(int i=0;i<entries.length();i++)NAMES.add(entries.getJSONObject(i).getString("name"));android.content.SharedPreferences prefs=context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS);int cachedIndex=prefs.getInt("class",0);String savedName=prefs.getString("className",null);if(savedName!=null)cachedIndex=NAMES.indexOf(savedName);int cachedCampus=prefs.getInt("campus",0);hadCachedChoice=prefs.contains("class")&&prefs.contains("campus")&&cachedIndex>=0&&cachedIndex<NAMES.size()&&cachedCampus>=0&&cachedCampus<4;load(context,hadCachedChoice?cachedIndex:0,hadCachedChoice?cachedCampus:0,false); }
  catch(Exception e){entries=null;NAMES.clear();NAMES.add("我的课表");}
 }
 public static synchronized void select(Context context,int index,int campusIndex){load(context,index,campusIndex,true);}
 private static synchronized void load(Context context,int index,int campusIndex,boolean persist){
  if(entries==null)return;
  try {
   int chosen=Math.max(0,Math.min(entries.length()-1,index));JSONArray rows=entries.getJSONObject(chosen).getJSONArray("courses");ScheduleData.Course[] courses=new ScheduleData.Course[rows.length()];
   for(int i=0;i<rows.length();i++){JSONArray r=rows.getJSONArray(i);int bits=r.getInt(5);List<Integer> weeks=new ArrayList<>();for(int w=1;w<=20;w++)if((bits&(1<<(w-1)))!=0)weeks.add(w);int[] ws=new int[weeks.size()];for(int w=0;w<ws.length;w++)ws[w]=weeks.get(w);int first=r.getInt(4);courses[i]=new ScheduleData.Course(strings.getString(r.getInt(0)),strings.getString(r.getInt(1)),strings.getString(r.getInt(2)),r.getInt(3),first,first+1,ws);}
   selected=chosen;campus=Math.max(0,Math.min(3,campusIndex));ScheduleData.COURSES=personalize(context,chosen,courses);if(persist)context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).edit().putInt("class",selected).putInt("campus",campus).putString("className",NAMES.get(selected)).commit();
  } catch(JSONException e){throw new IllegalStateException("课表数据格式错误",e);}
 }
 public static boolean configured(Context context){
  if(!hadCachedChoice||!context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).getBoolean("setupCompleted",false))return false;
  try{JSONObject saved=new JSONObject(groupChoices(context,selected));for(Map.Entry<String,List<String>> e:groups(selected).entrySet()){String value=saved.optString(e.getKey(),"");if(!value.equals("*")&&!value.equals("none")&&!e.getValue().contains(value))return false;}return true;}catch(Exception e){return false;}
 }
 public static synchronized void reload(Context context){String saved=context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).getString("catalog",null);if(!Objects.equals(saved,loadedCatalog))invalidate();initialize(context);android.content.SharedPreferences prefs=context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS);int index=NAMES.indexOf(prefs.getString("className",title()));if(index>=0)load(context,index,prefs.getInt("campus",campus),false);}
 public static void confirm(Context context,int index,int campusIndex){select(context,index,campusIndex);hadCachedChoice=true;context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).edit().putBoolean("setupCompleted",true).commit();}
 public static String groupKey(String name){
  if(name.contains("体育"))return "体育课";
  if(name.contains("分组")){int at=name.indexOf("分组");return name.substring(0,at).replaceAll("[（(\\s]+$", "");}
  return "";
 }
 public static Map<String,List<String>> groups(int index){
  Map<String,List<String>> result=new TreeMap<>();
  try{JSONArray rows=entries.getJSONObject(index).getJSONArray("courses");for(int i=0;i<rows.length();i++){JSONArray r=rows.getJSONArray(i);String name=strings.getString(r.getInt(0));String option=groupOption(name,strings.getString(r.getInt(1)));String key=groupKey(name);if(!key.isEmpty()){List<String> names=result.computeIfAbsent(key,k->new ArrayList<>());if(!names.contains(option))names.add(option);}}}catch(Exception ignored){}
  return result;
 }
 public static String groupOption(String name,String teacher){return name+"｜教师："+teacher;}
 public static String groupLabel(String option){
  String[] parts=option.split("｜教师：",2);String name=parts[0];String teacher=parts.length==2?" · 教师："+parts[1]:"";
  java.util.regex.Matcher match=java.util.regex.Pattern.compile("[（(]分组(\\d+)[（(](.+?)[）)][）)]").matcher(name);
  if(match.find())return match.group(2)+" · 第"+match.group(1)+"组"+teacher;
  if(name.contains("体育")){String project=parts.length==2?teacherProjects.optString(parts[1],""):"";return project.isEmpty()?"大学体育 · 项目未标注"+teacher:project+teacher+"（据其他班级）";}
  return name+teacher;
 }

 public static String courseLabel(String name,String teacher){if(name.contains("体育")&&!name.contains("分组")){String project=teacherProjects.optString(teacher,"");if(!project.isEmpty())return "大学体育（"+project+"）";}return name;}
 private static String normalized(String name){return name.replace('（','(').replace('）',')');}
 public static String displayName(int index){return index==0?NAMES.get(index).replace("",""):NAMES.get(index);}
 public static String groupChoices(Context context,int index){
  String saved=context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).getString("groups:"+NAMES.get(index),"{}");
  try{JSONObject choices=new JSONObject(saved);for(Map.Entry<String,List<String>> e:groups(index).entrySet()){String old=choices.optString(e.getKey(),"");if(old.isEmpty()||old.equals("*")||old.equals("none")||e.getValue().contains(old))continue;List<String> matches=new ArrayList<>();for(String option:e.getValue())if(normalized(option.split("｜教师：",2)[0]).equals(normalized(old)))matches.add(option);if(matches.size()==1)choices.put(e.getKey(),matches.get(0));}return choices.toString();}catch(Exception e){return "{}";}
 }
 public static void confirmGroups(Context context,int index,int campusIndex,String choices){context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).edit().putString("groups:"+NAMES.get(index),choices).commit();confirm(context,index,campusIndex);}
 private static String customKey(){return "custom:"+NAMES.get(selected);}
 public static JSONArray custom(Context context){try{return new JSONArray(context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).getString(customKey(),"[]"));}catch(Exception e){return new JSONArray();}}
 public static void addCustom(Context context,String name,String teacher,String room,int day,int first,int last,String weeks){
  int[] ws=CourseInput.weeks(weeks);CourseInput.validate(name,day,first,last);
  try{JSONArray list=custom(context);JSONObject row=new JSONObject();row.put("name",name.trim());row.put("teacher",teacher.trim());row.put("room",room.trim());row.put("day",day);row.put("first",first);row.put("last",last);JSONArray w=new JSONArray();for(int value:ws)w.put(value);row.put("weeks",w);list.put(row);context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).edit().putString(customKey(),list.toString()).commit();load(context,selected,campus,false);}catch(JSONException e){throw new IllegalStateException(e);}
 }
 public static void deleteCustom(Context context,int index){JSONArray list=custom(context);list.remove(index);context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).edit().putString(customKey(),list.toString()).commit();load(context,selected,campus,false);}
 private static ScheduleData.Course[] personalize(Context context,int index,ScheduleData.Course[] raw){
  List<ScheduleData.Course> result=new ArrayList<>();JSONObject choices;try{choices=new JSONObject(groupChoices(context,index));}catch(Exception e){choices=new JSONObject();}
  for(ScheduleData.Course c:raw){String key=groupKey(c.name);String choice=choices.optString(key,"*");if(key.isEmpty()||choice.equals("*")||choice.equals(groupOption(c.name,c.teacher)))result.add(c);}
  try{JSONArray extra=custom(context);for(int i=0;i<extra.length();i++){JSONObject c=extra.getJSONObject(i);JSONArray w=c.getJSONArray("weeks");int[] ws=new int[w.length()];for(int n=0;n<ws.length;n++)ws[n]=w.getInt(n);result.add(new ScheduleData.Course(c.getString("name"),c.getString("teacher"),c.getString("room"),c.getInt("day"),c.getInt("first"),c.getInt("last"),ws));}}catch(Exception ignored){}
  return result.toArray(new ScheduleData.Course[0]);
 }
 public static boolean delayed(String room){
  if(campus==0)return room.matches(".*(融新|融创|12#|13#|12＃|13＃|操场|运动场|体育馆).*" );
  if(campus==1)return room.matches(".*(2号楼|2-).*" )||room.contains("运动场")||room.contains("操场");
  if(campus==2)return room.matches(".*(10号楼|15号楼|16号楼|10-|15-|16-).*" )||room.contains("运动场")||room.contains("操场");
  return !(room.contains("教学楼")||room.contains("运动场")||room.contains("操场"));
 }
 public static String title(){return NAMES.isEmpty()?"我的课表":displayName(Math.min(selected,NAMES.size()-1));}
}
