package cn.gdcp.timetable
import org.junit.Test
import org.junit.Assert.*
class CourseInputTest {
    @Test fun mixedWeeksAreSortedAndDeduplicated() {
        assertArrayEquals(intArrayOf(1,3,5,6,7,8),CourseInput.weeks("8,1,3,5-8"))
        assertArrayEquals(intArrayOf(1,2,3),CourseInput.weeks("1—2，3"))
    }
    @Test fun invalidRangesCannotBeSaved() {
        listOf("", "0-5", "1-21", "9-3", "1-2-3", "1,").forEach {
            try { CourseInput.weeks(it);fail("accepted invalid weeks: $it") } catch(_:IllegalArgumentException) {}
        }
        try { CourseInput.validate("",1,1,2);fail() } catch(_:IllegalArgumentException) {}
        try { CourseInput.validate("测试",8,1,2);fail() } catch(_:IllegalArgumentException) {}
        try { CourseInput.validate("测试",1,4,3);fail() } catch(_:IllegalArgumentException) {}
    }
    @Test fun groupingOnlyUsesExplicitMarkers() {
        assertEquals("体育课",SchoolData.groupKey("大学体育(分组4（毽球）)"))
        assertEquals("实验课",SchoolData.groupKey("实验课（分组2）"))
        assertEquals("",SchoolData.groupKey("综合英语(2)"))
    }
}
