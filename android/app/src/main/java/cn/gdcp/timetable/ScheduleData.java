package cn.gdcp.timetable;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
public final class ScheduleData {
 public static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
 public static final LocalDate START=LocalDate.of(2026,8,31);
 public static final String[] STARTS={"08:30","09:15","10:10","10:55","14:15","15:00","15:55","16:35","18:05","18:45","19:40","20:20"};
 public static final String[] ENDS={"09:10","09:55","10:50","11:35","14:55","15:40","16:35","17:15","18:45","19:25","20:20","21:00"};
 public static final class Course {
  public final String name,teacher,room;public final int day,first,last;public final int[] weeks;
  Course(String n,String t,String r,int d,int f,int l,int[] w){name=n;teacher=t;room=r;day=d;first=f;last=l;weeks=w;}
  public String displayName(){return SchoolData.courseLabel(name,teacher);}
  public String startTime(){return first==3 && SchoolData.delayed(room)?"10:30":first==4 && SchoolData.delayed(room)?"11:15":STARTS[first-1];}
  public String endTime(){return last==3 && SchoolData.delayed(room)?"11:10":last==4 && SchoolData.delayed(room)?"11:55":ENDS[last-1];}
  public String time(){return startTime()+"–"+endTime();}
  public boolean inWeek(int week){for(int w:weeks)if(w==week)return true;return false;}
 }
 public static Course[] COURSES={};
 public static int week(LocalDate date){return (int)Math.floorDiv(ChronoUnit.DAYS.between(START,date),7)+1;}
 public static List<Course> courses(LocalDate date){List<Course> list=new ArrayList<>();int w=week(date);if(w<1||w>20)return list;for(Course c:COURSES)if(c.day==date.getDayOfWeek().getValue()&&c.inWeek(w))list.add(c);list.sort(Comparator.comparingInt(c->c.first));return list;}
 public static Course next(LocalDate date,LocalTime time){for(Course c:courses(date))if(time.isBefore(LocalTime.parse(c.endTime())))return c;return null;}
 public static String state(Course c,LocalTime time){return time.isBefore(LocalTime.parse(c.startTime()))?"接下来":"正在上课";}
 public static ZonedDateTime nextRefresh(ZonedDateTime now){ZonedDateTime end=now.plusMinutes(30);ZonedDateTime midnight=now.toLocalDate().plusDays(1).atStartOfDay(ZONE);if(midnight.isBefore(end))end=midnight;for(Course c:courses(now.toLocalDate()))for(String clock:new String[]{c.startTime(),c.endTime()}){ZonedDateTime t=now.toLocalDate().atTime(LocalTime.parse(clock)).atZone(ZONE);if(t.isAfter(now)&&t.isBefore(end))end=t;}return end;}
}
