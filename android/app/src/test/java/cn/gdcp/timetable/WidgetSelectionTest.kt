package cn.gdcp.timetable
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.view.View
import android.widget.TextView
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.*
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class WidgetSelectionTest {
 @Test fun relativeDatesCrossMidnightAndYear(){
  val now=LocalDate.of(2026,12,31).atTime(23,59).atZone(ScheduleData.ZONE)
  assertEquals(LocalDate.of(2027,1,1),BaseWidgetProvider.selectedDate(now,1,"2026-10-15"))
  assertEquals(LocalDate.of(2027,1,2),BaseWidgetProvider.selectedDate(now.plusMinutes(1),1,"2026-10-15"))
  assertEquals(LocalDate.of(2026,10,15),BaseWidgetProvider.selectedDate(now,2,"2026-10-15"))
  assertEquals(LocalDate.of(2026,10,15),BaseWidgetProvider.selectedDate(now.plusDays(3),2,"2026-10-15"))
 }
 @Test fun savesEachWidgetIndependentlyAndDeletesOnlyItsSettings(){
  val c=RuntimeEnvironment.getApplication();val p=c.getSharedPreferences("widget_dates",0);p.edit().clear().commit()
  assertTrue(BaseWidgetProvider.saveDate(c,11,1,"2026-10-15"))
  assertTrue(BaseWidgetProvider.saveDate(c,12,2,"2026-10-18"))
  assertEquals(1,p.getInt("mode_11",0));assertEquals(2,p.getInt("mode_12",0))
  assertEquals("2026-10-18",p.getString("date_12",null))
  BaseWidgetProvider().onDeleted(c,intArrayOf(11))
  assertFalse(p.contains("mode_11"));assertEquals(2,p.getInt("mode_12",0))
  p.edit().clear().commit()
 }
 @Test fun openingAndCancellingSettingsDoesNotSave(){
  val c=RuntimeEnvironment.getApplication();val p=c.getSharedPreferences("widget_dates",0);p.edit().clear().commit()
  BaseWidgetProvider.saveDate(c,13,2,"2026-10-15")
  val intent=Intent(c,WidgetSettingsActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,13)
  val controller=Robolectric.buildActivity(WidgetSettingsActivity::class.java,intent).setup()
  controller.recreate();controller.get().finish();controller.pause().stop().destroy()
  assertEquals(2,p.getInt("mode_13",0));assertEquals("2026-10-15",p.getString("date_13",null))
  p.edit().clear().commit()
 }
 @Test fun fixedPastAndOutOfSemesterDatesAreLabelledCorrectly(){
  val c=RuntimeEnvironment.getApplication();val now=LocalDate.of(2026,10,10).atTime(22,0).atZone(ScheduleData.ZONE)
  val date=LocalDate.of(2027,2,1)
  val v=BaseWidgetProvider.views(c,15,"xiaomi",date,now,340,150,2).apply(c,null)
  assertTrue(v.findViewById<TextView>(R.id.widget_header).text.contains("指定日期"))
  assertEquals("学期范围外",v.findViewById<TextView>(R.id.widget_title).text.toString())
  assertEquals(View.VISIBLE,v.findViewById<View>(R.id.widget_settings).visibility)
 }
}
