package cn.gdcp.timetable
import org.junit.Test
import org.junit.Assert.*
class WidgetRoomPagesTest {
 @Test fun preservesAllRoomText(){val room="示例馆（15#）-206 / 第二教学区";val pages=WidgetRoomPages.split(room,{it.length.toDouble()},6.0);assertEquals(room,pages.joinToString(""));assertTrue(pages.all{it.length<=6})}
 @Test fun shortRoomDoesNotScroll(){assertEquals(listOf("操场1"),WidgetRoomPages.split("操场1",{it.length.toDouble()},20.0))}
 @Test fun keepsUnicodeCharactersIntact(){val pages=WidgetRoomPages.split("A😀B",{it.codePointCount(0,it.length).toDouble()},1.0);assertEquals(listOf("A","😀","B"),pages)}
}
