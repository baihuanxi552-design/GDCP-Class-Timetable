package cn.gdcp.timetable;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;

public class JwCourseDiffTest {
 private JSONObject data(int... weeks)throws Exception{
  JSONArray w=new JSONArray();for(int week:weeks)w.put(week);
  JSONObject c=new JSONObject().put("name","Example course").put("teacher","Example teacher").put("position","Example room").put("day",1)
    .put("sections",new JSONArray("[1,2]")).put("weeks",w);
  return new JSONObject().put("format","gdcp-personal-timetable").put("version",1).put("semester","2026-2027-1").put("courses",new JSONArray().put(c));
 }
 @Test public void reorderedWeeksAndDuplicateRowsDoNotUpdate()throws Exception{
  JSONObject before=data(1,2,3),after=data(3,1,2,1);after.getJSONArray("courses").put(after.getJSONArray("courses").getJSONObject(0));
  assertFalse(JwCourseDiff.between(before,after).changed());
 }
 @Test public void changedRoomReportsOldAndNewOccurrence()throws Exception{
  JSONObject before=data(1,2),after=data(1,2);after.getJSONArray("courses").getJSONObject(0).put("position","Another room");
  JwCourseDiff diff=JwCourseDiff.between(before,after);assertEquals(2,diff.added);assertEquals(2,diff.removed);
  assertTrue(JwCourseDiff.describe(before,after).contains("Another room"));
 }
 @Test public void removedWeeksRequireConfirmationWhenLarge()throws Exception{
  JSONObject before=data(1,2,3,4,5,6,7,8,9,10);
  assertFalse(JwCourseDiff.between(before,data(1,2,3,4,5,6,7,8,9)).needsConfirmation(before));
  assertTrue(JwCourseDiff.between(before,data(1,2,3)).needsConfirmation(before));
 }
 @Test public void replacingAllOldSlotsEvenInSmallScheduleNeedsConfirmation()throws Exception{
  JSONObject before=data(1),after=data(1);after.getJSONArray("courses").getJSONObject(0).put("name","Another course");
  assertTrue(JwCourseDiff.between(before,after).needsConfirmation(before));
 }
 @Test public void emptyAndWrongSemesterAreNeverValidSnapshots()throws Exception{
  JSONObject empty=data(1);empty.put("courses",new JSONArray());
  try{PersonalImport.validate(empty.toString());fail();}catch(JSONException expected){}
  JSONObject wrong=data(1);wrong.put("semester","2025-2026-2");
  try{PersonalImport.validate(wrong.toString());fail();}catch(JSONException expected){}
 }
 @Test public void refreshingRequiresOptInActiveTimetableAndSession(){
  assertFalse(JwCourseDiff.canRefresh(false,true,true,false));
  assertFalse(JwCourseDiff.canRefresh(true,false,true,false));
  assertFalse(JwCourseDiff.canRefresh(true,true,false,false));
  assertFalse(JwCourseDiff.canRefresh(true,true,true,true));
  assertTrue(JwCourseDiff.canRefresh(true,true,true,false));
 }
 @Test public void pendingConfirmationExpiresAndRejectsClockRollback(){
  assertTrue(JwCourseDiff.pendingFresh(1000,2000));
  assertFalse(JwCourseDiff.pendingFresh(1000,86401001));
  assertFalse(JwCourseDiff.pendingFresh(2000,1000));
  assertFalse(JwCourseDiff.pendingFresh(0,1000));
 }

}
