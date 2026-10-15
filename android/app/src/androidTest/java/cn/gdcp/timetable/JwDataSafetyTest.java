package cn.gdcp.timetable;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.Arrays;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class JwDataSafetyTest {
 private Context context;
 private interface Action {void run()throws Exception;}
 private void ui(Action a){
  final Throwable[] failure=new Throwable[1];
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{try{a.run();}catch(Throwable t){failure[0]=t;}});
  if(failure[0]!=null)throw new AssertionError(failure[0]);
 }
 private String timetable(String name)throws Exception{
  JSONObject row=new JSONObject().put("name",name).put("teacher","Example teacher").put("position","Example room").put("day",1)
    .put("sections",new JSONArray("[1,2]")).put("weeks",new JSONArray("[1,2]"));
  return new JSONObject().put("format","gdcp-personal-timetable").put("version",1).put("semester","2026-2027-1").put("courses",new JSONArray().put(row)).toString();
 }
 @Before public void setup(){
  context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  ui(()->{JwSession.prefs(context).edit().clear().commit();SchoolData.invalidate();SchoolData.initialize(context);});
 }
 @After public void cleanup(){ui(()->{JwSession.prefs(context).edit().clear().commit();SchoolData.invalidate();SchoolData.initialize(context);});}
 @Test public void updatingPersonalSnapshotKeepsCustomAndPrevious()throws Exception{
  String before=timetable("Before course"),after=timetable("After course");
  ui(()->{
   SchoolData.addCustom(context,"Custom course","Example teacher","Example room",2,3,4,"1-2");
   PersonalImport.save(context,before);PersonalImport.save(context,after);
   assertEquals("Before course",new JSONObject(JwSession.prefs(context).getString("jwPreviousPersonal","")).getJSONArray("courses").getJSONObject(0).getString("name"));
   assertTrue(Arrays.stream(ScheduleData.COURSES).anyMatch(c->c.name.equals("Custom course")));
   assertTrue(Arrays.stream(ScheduleData.COURSES).anyMatch(c->c.name.equals("After course")));
   assertFalse(Arrays.stream(ScheduleData.COURSES).anyMatch(c->c.name.equals("Before course")));
  });
 }
 @Test public void backupRoundTripRestoresPersonalAndCustomWithoutSession()throws Exception{
  String saved=timetable("Saved course"),changed=timetable("Changed course");
  ui(()->{
   SchoolData.addCustom(context,"Custom course","Example teacher","Example room",2,3,4,"1-2");
   PersonalImport.save(context,saved);JwSession.setEnabled(context,true);
   String backup=BackupData.exportData(context);JSONObject data=BackupData.validate(backup);
   assertTrue(data.has("personalImport"));assertFalse(data.has("jwSyncEnabled"));assertFalse(backup.contains("JSESSIONID"));
   PersonalImport.save(context,changed);SchoolData.deleteCustom(context,0);
   BackupData.restore(context,backup);
   assertTrue(PersonalImport.active(context));assertFalse(JwSession.enabled(context));
   assertTrue(Arrays.stream(ScheduleData.COURSES).anyMatch(c->c.name.equals("Saved course")));
   assertTrue(Arrays.stream(ScheduleData.COURSES).anyMatch(c->c.name.equals("Custom course")));
  });
 }
 @Test public void legacyBackupDoesNotErasePersonalSnapshot()throws Exception{
  String current=timetable("Current personal course");
  ui(()->{
   String legacy=BackupData.exportData(context);assertFalse(new JSONObject(legacy).has("personalImport"));
   PersonalImport.save(context,current);BackupData.restore(context,legacy);
   assertEquals("Current personal course",new JSONObject(JwSession.prefs(context).getString("jwPersonal","")).getJSONArray("courses").getJSONObject(0).getString("name"));
  });
 }
}
