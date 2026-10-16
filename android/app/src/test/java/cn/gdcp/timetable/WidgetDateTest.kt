package cn.gdcp.timetable
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalTime
class WidgetDateTest {
 @Test fun switchesAtLastCourseEnd(){
  val original=ScheduleData.COURSES
  try {
   val date=ScheduleData.START
   ScheduleData.COURSES=arrayOf(ScheduleData.Course("测试课程","","",1,1,2,intArrayOf(1)))
   assertEquals(date,ScheduleData.widgetDate(date,LocalTime.of(9,54),0))
   assertEquals(date.plusDays(1),ScheduleData.widgetDate(date,LocalTime.of(9,55),0))
   assertEquals(date.plusDays(1),ScheduleData.widgetDate(date,LocalTime.of(8,0),1))
  } finally {ScheduleData.COURSES=original}
 }
 @Test fun noCoursesKeepsToday(){assertEquals(ScheduleData.START.plusWeeks(20),ScheduleData.widgetDate(ScheduleData.START.plusWeeks(20),LocalTime.NOON,0))}
}
