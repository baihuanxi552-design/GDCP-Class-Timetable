package cn.gdcp.timetable;
import java.util.*;
public final class CoursePalette {
 /** Allocate from the whole day, before filtering into time slots. Never mutate the input. */
 public static Map<ScheduleData.Course,Integer> forDay(List<ScheduleData.Course> courses) {
  List<ScheduleData.Course> ordered=new ArrayList<>(courses);
  ordered.sort(Comparator.comparingInt((ScheduleData.Course c)->c.first)
   .thenComparingInt(c->c.last).thenComparing(c->c.name)
   .thenComparing(c->c.teacher).thenComparing(c->c.room));
  Map<ScheduleData.Course,Integer> result=new IdentityHashMap<>();
  int[] uses=new int[ACCENTS.length];
  for(ScheduleData.Course c:ordered) {
   int preferred=index(c.name),chosen=preferred;
   for(int offset=1;offset<uses.length;offset++) {
    int candidate=(preferred+offset)%uses.length;
    if(uses[candidate]<uses[chosen]) chosen=candidate;
   }
   result.put(c,chosen);uses[chosen]++;
  }
  return result;
 }
 public static final int[] ACCENTS={0xFF4B6FF4,0xFFED975C,0xFF58A17B,0xFF9D73DB,0xFFE87899,0xFF51A3BB,0xFFDFB94D,0xFF619FDB,0xFF9AA864,0xFFD89073,0xFF867CDF,0xFF70B8A5,0xFFC882B5};
 public static int index(String name){return (name.hashCode()>>>1)%ACCENTS.length;}
 public static int accent(String name){return ACCENTS[index(name)];}
 public static final int[] BACKGROUNDS={R.drawable.widget_course_0,R.drawable.widget_course_1,R.drawable.widget_course_2,R.drawable.widget_course_3,R.drawable.widget_course_4,R.drawable.widget_course_5,R.drawable.widget_course_6,R.drawable.widget_course_7,R.drawable.widget_course_8,R.drawable.widget_course_9,R.drawable.widget_course_10,R.drawable.widget_course_11,R.drawable.widget_course_12};
 public static int background(String name){return BACKGROUNDS[index(name)];}
}

