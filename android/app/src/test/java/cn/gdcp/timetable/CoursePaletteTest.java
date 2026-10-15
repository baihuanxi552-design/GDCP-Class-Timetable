package cn.gdcp.timetable;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CoursePaletteTest {
 private ScheduleData.Course course(String name,int first) {
  return new ScheduleData.Course(name,"Teacher","Room",1,first,first,new int[]{1});
 }
 @Test public void resolvesHashCollisionsAndRepeatedNames() {
  // Aa and BB deliberately have identical Java string hashes.
  List<ScheduleData.Course> day=Arrays.asList(course("Aa",1),course("BB",2),course("Aa",3));
  assertEquals(CoursePalette.index("Aa"),CoursePalette.index("BB"));
  assertEquals(3,new HashSet<>(CoursePalette.forDay(day).values()).size());
 }
 @Test public void exhaustsPaletteBeforeReusingAndBalancesOverflow() {
  List<ScheduleData.Course> day=new ArrayList<>();
  for(int i=0;i<CoursePalette.ACCENTS.length;i++)day.add(course("Same name",i%12+1));
  assertEquals(CoursePalette.ACCENTS.length,new HashSet<>(CoursePalette.forDay(day).values()).size());
  for(int i=0;i<20;i++)day.add(course("Same name",i%12+1));
  int[] counts=new int[CoursePalette.ACCENTS.length];
  for(int index:CoursePalette.forDay(day).values())counts[index]++;
  assertTrue(Arrays.stream(counts).max().getAsInt()-Arrays.stream(counts).min().getAsInt()<=1);
 }
 @Test public void orderDoesNotChangeAssignmentOrMutateInput() {
  List<ScheduleData.Course> day=new ArrayList<>(Arrays.asList(course("Aa",1),course("BB",1),course("Aa",3)));
  List<ScheduleData.Course> original=new ArrayList<>(day);
  Map<ScheduleData.Course,Integer> colors=CoursePalette.forDay(day);
  assertEquals(original,day);
  Collections.reverse(day);
  Map<ScheduleData.Course,Integer> reversed=CoursePalette.forDay(day);
  for(ScheduleData.Course c:day)assertEquals(colors.get(c),reversed.get(c));
 }
 @Test public void emptyAndSeparateDaysAreIndependent() {
  assertTrue(CoursePalette.forDay(Collections.emptyList()).isEmpty());
  ScheduleData.Course a=course("Aa",1),b=course("BB",1);
  assertEquals(CoursePalette.forDay(Collections.singletonList(a)).get(a),
               CoursePalette.forDay(Collections.singletonList(b)).get(b));
 }
}
