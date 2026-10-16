package cn.gdcp.timetable
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ViewFlipper
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.*
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class WidgetLayoutTest {
 private fun setup(){
  ScheduleData.COURSES=arrayOf(
   ScheduleData.Course("示例课程甲教学实践","测试教师","示例楼（14#）-404",4,1,2,intArrayOf(6)),
   ScheduleData.Course("示例课程","测试教师","示例馆（15#）-207",4,3,4,intArrayOf(6)),
   ScheduleData.Course("示例课程乙综合测试教学","测试教师","示例楼（17#）-209",4,9,10,intArrayOf(6)))
 }
 private fun tree(c:Context,w:Int,h:Int):View {
  val date=LocalDate.of(2026,10,8)
  return BaseWidgetProvider.views(c,1,"xiaomi",date,date.minusDays(1).atTime(22,33).atZone(ScheduleData.ZONE),w,h,1).apply(c,null)
 }
 private fun rows(v:View)=listOf(R.id.row0,R.id.row1,R.id.row2,R.id.row3,R.id.row4,R.id.row5).count{v.findViewById<View>(it).visibility==View.VISIBLE}
 @Test fun reportedProportions(){setup();try {
  val c=RuntimeEnvironment.getApplication()
  assertEquals(1,rows(tree(c,340,150)))
  assertEquals(3,rows(tree(c,340,350)))
  assertEquals(3,rows(tree(c,160,350)))
  val t=tree(c,340,150)
  val card=t.findViewById<ViewGroup>(R.id.row0)
  assertSame(card,t.findViewById<View>(R.id.row_info0).parent)
  assertSame(card,t.findViewById<View>(R.id.row_room0).parent)
 }finally{ScheduleData.COURSES=emptyArray()}}
 @Test fun resizingRestoresVisibleFields(){setup();try{
  val c=RuntimeEnvironment.getApplication();val date=LocalDate.of(2026,10,8);val now=date.minusDays(1).atTime(22,33).atZone(ScheduleData.ZONE)
  val v=tree(c,110,110)
  BaseWidgetProvider.views(c,1,"xiaomi",date,now,340,350,1).reapply(c,v)
  assertEquals(3,rows(v))
  assertEquals(View.VISIBLE,v.findViewById<View>(R.id.row_name0).visibility)
  assertEquals(View.VISIBLE,v.findViewById<View>(R.id.row_info0).visibility)
 }finally{ScheduleData.COURSES=emptyArray()}}
 @Test fun longRoomsRemainComplete(){setup();try{
  val room="示例楼（14#）-404 / 示例校区综合测试中心第二教学区域"
  val old=ScheduleData.COURSES[0]
  ScheduleData.COURSES[0]=ScheduleData.Course(old.name,old.teacher,room,old.day,old.first,old.last,old.weeks)
  val c=RuntimeEnvironment.getApplication()
  for((w,h) in listOf(110 to 110,340 to 150,160 to 350,340 to 350)){
   val v=tree(c,w,h);val f=v.findViewById<ViewFlipper>(R.id.row_room0)
   assertTrue((0 until f.childCount).joinToString(""){(f.getChildAt(it) as TextView).text.toString().replace("\n","")}.contains(room))
   assertTrue(BaseWidgetProvider.measuredHeight(c,BaseWidgetProvider.views(c,1,"xiaomi",LocalDate.of(2026,10,8),LocalDate.of(2026,10,7).atTime(22,33).atZone(ScheduleData.ZONE),w,h,1),w)<=h+1)
  }
 }finally{ScheduleData.COURSES=emptyArray()}}
 @Test fun renderRepresentativeSizes(){setup();try{
  val c=RuntimeEnvironment.getApplication();val d=c.resources.displayMetrics.density
  val out=java.io.File("build/widget-previews");out.mkdirs()
  for((w,h) in listOf(340 to 150,340 to 250,340 to 350,160 to 350,110 to 110)){
   val v=tree(c,w,h);val pw=(w*d).toInt();val ph=(h*d).toInt()
   v.measure(View.MeasureSpec.makeMeasureSpec(pw,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(ph,View.MeasureSpec.EXACTLY));v.layout(0,0,pw,ph)
   val bitmap=android.graphics.Bitmap.createBitmap(pw,ph,android.graphics.Bitmap.Config.ARGB_8888)
   v.draw(android.graphics.Canvas(bitmap))
   java.io.FileOutputStream(java.io.File(out,"$w-$h.png")).use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
  }
 }finally{ScheduleData.COURSES=emptyArray()}}
 @Test fun sizeAndFontMatrix(){setup();try{
  val base=RuntimeEnvironment.getApplication()
  for(scale in listOf(1f,1.3f,1.6f)){
   val config=android.content.res.Configuration(base.resources.configuration);config.fontScale=scale
   val c=base.createConfigurationContext(config)
   for(w in listOf(110,140,160,190,220,260,300,340,420))for(h in listOf(110,150,200,250,300,350,420,500)){
    val t=tree(c,w,h);val density=c.resources.displayMetrics.density
    t.measure(View.MeasureSpec.makeMeasureSpec((w*density).toInt(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
    assertTrue("Overflow $w x $h font $scale height ${t.measuredHeight/density}",t.measuredHeight/density<=h+1)
    assertTrue(rows(t)>=1)
    val room=t.findViewById<ViewFlipper>(R.id.row_room0)
    assertTrue(room.visibility==View.VISIBLE&&room.childCount>0)
    val full=(0 until room.childCount).joinToString(""){(room.getChildAt(it) as TextView).text.toString().replace("\n","")}
    assertTrue("Room lost $w x $h",full.contains("示例楼（14#）-404"))
   }
  }
 }finally{ScheduleData.COURSES=emptyArray()}}
}
