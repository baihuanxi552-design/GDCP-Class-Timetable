package cn.gdcp.timetable;

import org.json.*;
import java.util.*;

/** Compare normalized occurrences: row order and week order are irrelevant. */
public final class JwCourseDiff {
 public final int added,removed;
 private JwCourseDiff(int a,int r){added=a;removed=r;}
 static Set<String> occurrences(JSONObject data)throws JSONException {
  Set<String> result=new TreeSet<>();JSONArray rows=data.getJSONArray("courses");
  for(int i=0;i<rows.length();i++){
   JSONObject c=rows.getJSONObject(i);JSONArray s=c.getJSONArray("sections"),w=c.getJSONArray("weeks");
   for(int j=0;j<w.length();j++)result.add(new JSONArray().put(c.getString("name")).put(c.getString("teacher"))
     .put(c.getString("position")).put(c.getInt("day")).put(s).put(w.getInt(j)).toString());
  }
  return result;
 }
 public static JwCourseDiff between(JSONObject old,JSONObject next)throws JSONException{
  Set<String> before=occurrences(old),after=occurrences(next),a=new HashSet<>(after),r=new HashSet<>(before);
  a.removeAll(before);r.removeAll(after);return new JwCourseDiff(a.size(),r.size());
 }
 public boolean changed(){return added>0||removed>0;}
 public boolean needsConfirmation(JSONObject old)throws JSONException{int count=occurrences(old).size();return removed>0&&(removed==count||(removed>=3&&removed*10>count*3));}
 public String summary(){return "新增 "+added+" 个、移除或调整 "+removed+" 个课程时段";}
 public static String describe(JSONObject old,JSONObject next)throws JSONException {
  Set<String> before=occurrences(old),after=occurrences(next),added=new TreeSet<>(after),removed=new TreeSet<>(before);
  added.removeAll(before);removed.removeAll(after);StringBuilder out=new StringBuilder();
  describeSet(out,"新增",added);describeSet(out,"移除或调整前",removed);return out.toString();
 }
 private static void describeSet(StringBuilder out,String title,Set<String> values)throws JSONException{
  if(values.isEmpty())return;out.append(title).append("：\n");int count=0;
  for(String value:values){if(count++>=12){out.append("其余 ").append(values.size()-12).append(" 个时段…\n");break;}
   JSONArray c=new JSONArray(value);out.append(c.getString(0)).append(" · 周").append(c.getInt(3)).append(" · 第").append(c.getInt(5)).append("周 · ").append(c.getJSONArray(4).toString()).append("节 · ").append(c.getString(2)).append("\n");
  }
 }
 public static boolean pendingFresh(long savedAt,long now){return savedAt>0&&now>=savedAt&&now-savedAt<=86400000L;}
 public static boolean canRefresh(boolean enabled,boolean active,boolean hasSession,boolean needsLogin){
  return enabled&&active&&hasSession&&!needsLogin;
 }
}
