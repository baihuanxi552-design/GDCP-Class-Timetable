package cn.gdcp.timetable;
import android.content.Context;
import org.json.*;
import java.util.*;
/** Local-only course data. Never stores login forms, HTML or cookies. */
public final class PersonalImport {
 public static JSONObject validate(String text) throws JSONException {
  if(text.length()>1024*1024)throw new JSONException("数据过大");JSONObject root=new JSONObject(text);
  if(!"gdcp-personal-timetable".equals(root.optString("format"))||root.optInt("version")!=1||!"2026-2027-1".equals(root.optString("semester")))throw new JSONException("格式或学期不匹配");
  JSONArray rows=root.getJSONArray("courses"),clean=new JSONArray();if(rows.length()==0||rows.length()>2000)throw new JSONException("课程数量无效");
  for(int i=0;i<rows.length();i++){JSONObject c=rows.getJSONObject(i);String name=c.getString("name"),teacher=c.getString("teacher"),room=c.getString("position");int day=c.getInt("day");JSONArray sections=c.getJSONArray("sections"),weeks=c.getJSONArray("weeks");if(name.trim().isEmpty()||name.length()>200||teacher.length()>200||room.length()>300||day<1||day>7||sections.length()<1||sections.length()>12||weeks.length()<1||weeks.length()>20)throw new JSONException("课程字段无效");for(int n=0;n<sections.length();n++){int s=sections.getInt(n);if(s<1||s>12||(n>0&&s!=sections.getInt(n-1)+1))throw new JSONException("节次无效");}for(int n=0;n<weeks.length();n++)if(weeks.getInt(n)<1||weeks.getInt(n)>20)throw new JSONException("周次无效");JSONObject item=new JSONObject();item.put("name",name.trim());item.put("teacher",teacher);item.put("position",room);item.put("day",day);item.put("sections",sections);item.put("weeks",weeks);clean.put(item);}
  JSONObject result=new JSONObject();result.put("format","gdcp-personal-timetable");result.put("version",1);result.put("semester","2026-2027-1");result.put("courses",clean);return result;
 }
 public static boolean active(Context context){return context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).getBoolean("jwActive",false);}
 public static void save(Context context,String text)throws JSONException{String clean=validate(text).toString();context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).edit().putString("jwPersonal",clean).putBoolean("jwActive",true).commit();SchoolData.confirm(context,SchoolData.selected,SchoolData.campus);SchoolData.reload(context);BaseWidgetProvider.updateAll(context);}
 public static void useClass(Context context){context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).edit().putBoolean("jwActive",false).commit();SchoolData.reload(context);BaseWidgetProvider.updateAll(context);}
 public static ScheduleData.Course[] apply(Context context,ScheduleData.Course[] original){
  if(!active(context))return original;
  try {JSONArray rows=validate(context.getSharedPreferences("school",Context.MODE_MULTI_PROCESS).getString("jwPersonal","")).getJSONArray("courses");List<ScheduleData.Course> result=new ArrayList<>();for(int i=0;i<rows.length();i++){JSONObject c=rows.getJSONObject(i);JSONArray s=c.getJSONArray("sections"),w=c.getJSONArray("weeks");int[] ws=new int[w.length()];for(int j=0;j<ws.length;j++)ws[j]=w.getInt(j);result.add(new ScheduleData.Course(c.getString("name"),c.getString("teacher"),c.getString("position"),c.getInt("day"),s.getInt(0),s.getInt(s.length()-1),ws));}return result.toArray(new ScheduleData.Course[0]);}catch(Exception e){return original;}
 }
}
