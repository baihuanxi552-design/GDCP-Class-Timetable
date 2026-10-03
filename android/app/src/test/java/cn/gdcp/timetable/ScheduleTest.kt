package cn.gdcp.timetable
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalTime
class ScheduleTest {
 @Test fun blankSchedule(){ assertEquals(0,ScheduleData.COURSES.size);assertTrue(ScheduleData.courses(ScheduleData.START).isEmpty());assertNull(ScheduleData.next(ScheduleData.START,LocalTime.NOON)) }
 @Test fun semesterBoundaries(){assertEquals(1,ScheduleData.week(ScheduleData.START));assertEquals(0,ScheduleData.week(ScheduleData.START.minusDays(1)));assertEquals(21,ScheduleData.week(ScheduleData.START.plusWeeks(20)))}
}
