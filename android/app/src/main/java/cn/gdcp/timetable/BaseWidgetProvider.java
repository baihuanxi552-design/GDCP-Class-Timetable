package cn.gdcp.timetable;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;
import java.time.*;
import java.util.*;

public class BaseWidgetProvider extends AppWidgetProvider {
 static final String ACTION_REFRESH="cn.gdcp.timetable.REFRESH_WIDGETS";
 static final Class<?>[] PROVIDERS={NextWidgetProvider.class,TodayWidgetProvider.class,OverviewWidgetProvider.class};
 static String autoStyle(){String m=Build.MANUFACTURER.toLowerCase(Locale.ROOT);if(m.contains("xiaomi")||m.contains("redmi"))return "xiaomi";if(m.contains("vivo")||m.contains("iqoo"))return "vivo";if(m.contains("oppo")||m.contains("oneplus")||m.contains("realme"))return "oppo";if(m.contains("honor"))return "honor";if(m.contains("huawei"))return "huawei";if(m.contains("meizu"))return "flyme";return "classic";}
 static int background(String style){switch(style){case "xiaomi":return R.drawable.widget_bg_xiaomi;case "vivo":return R.drawable.widget_bg_vivo;case "oppo":return R.drawable.widget_bg_oppo;case "honor":return R.drawable.widget_bg_honor;case "huawei":return R.drawable.widget_bg_huawei;case "flyme":return R.drawable.widget_bg_flyme;default:return R.drawable.widget_bg_classic;}}
 static Class<?> providerFor(Context c,int id){AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);if(info==null||!c.getPackageName().equals(info.provider.getPackageName()))return null;for(Class<?> cls:PROVIDERS)if(cls.getName().equals(info.provider.getClassName()))return cls;return null;}
 public static void updateAll(Context c){SchoolData.reload(c);AppWidgetManager m=AppWidgetManager.getInstance(c);for(Class<?> cls:PROVIDERS)for(int id:m.getAppWidgetIds(new ComponentName(c,cls)))update(c,m,id);schedule(c);}
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){if(ids!=null)for(int id:ids)if(providerFor(c,id)!=null)update(c,m,id);schedule(c);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle opts){if(providerFor(c,id)!=null)update(c,m,id);schedule(c);}
 @Override public void onReceive(Context c,Intent intent){String a=intent.getAction();if("miui.appwidget.action.APPWIDGET_UPDATE".equals(a)){updateAll(c);return;}super.onReceive(c,intent);}
 @Override public void onEnabled(Context c){updateAll(c);}
 @Override public void onDisabled(Context c){schedule(c);}
 static void update(Context c,AppWidgetManager m,int id){
  SchoolData.reload(c);if(providerFor(c,id)==null)return;
  Bundle opts=m.getAppWidgetOptions(id);String style=opts.getString("qingyuan_style","auto");if("auto".equals(style))style=autoStyle();
  ZonedDateTime now=ZonedDateTime.now(ScheduleData.ZONE);int offset=opts.getInt("qingyuan_day",0)==1?1:0;LocalDate date=now.toLocalDate().plusDays(offset);
  if(Build.VERSION.SDK_INT>=31){Map<SizeF,RemoteViews> sizes=new LinkedHashMap<>();sizes.put(new SizeF(110,110),views(c,id,style,date,now,110,110,offset));sizes.put(new SizeF(250,150),views(c,id,style,date,now,250,150,offset));sizes.put(new SizeF(250,300),views(c,id,style,date,now,250,300,offset));sizes.put(new SizeF(300,380),views(c,id,style,date,now,300,380,offset));m.updateAppWidget(id,new RemoteViews(sizes));}
  else {Class<?> cls=providerFor(c,id);int w=opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,cls==NextWidgetProvider.class?110:300);int h=opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,cls==OverviewWidgetProvider.class?300:150);m.updateAppWidget(id,views(c,id,style,date,now,w,h,offset));}
 }
 static RemoteViews views(Context c,int id,String style,LocalDate date,ZonedDateTime now,int w,int h,int offset){
  boolean compact=w<230||h<125,large=!compact&&h>=240;int layout=compact?R.layout.widget_compact:(large?R.layout.widget_large:R.layout.widget_wide);
  RemoteViews v=new RemoteViews(c.getPackageName(),layout);v.setInt(android.R.id.background,"setBackgroundResource",background(style));
  List<ScheduleData.Course> list=ScheduleData.courses(date);int week=ScheduleData.week(date);String dayName=offset==0?"今日":"明日";
  v.setTextViewText(R.id.widget_header,date.getMonthValue()+"月"+date.getDayOfMonth()+"日 · "+dayName+"课表");
  Intent open=new Intent(c,MainActivity.class).setAction("cn.gdcp.timetable.OPEN_WIDGET").putExtra("widget_date",date.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  v.setOnClickPendingIntent(R.id.widget_content,PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  Intent edit=new Intent(c,WidgetSettingsActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra("editing",true);
  v.setOnClickPendingIntent(R.id.widget_settings,PendingIntent.getActivity(c,id+100000,edit,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  Intent refresh=new Intent(c,WidgetRefreshReceiver.class).setAction(ACTION_REFRESH);
  v.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(c,0,refresh,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  String footer=week<1||week>20?"本学期范围外":"第"+week+"周 · 点按查看";
  v.setTextViewText(R.id.widget_footer,SchoolData.title()+" · "+footer);
  v.setViewVisibility(R.id.widget_status,View.GONE);
  if(compact){
   ScheduleData.Course next=offset==0?ScheduleData.next(date,now.toLocalTime()):(list.isEmpty()?null:list.get(0));
   float scale=Math.max(1f,c.getResources().getConfiguration().fontScale);v.setViewVisibility(R.id.widget_bottom,h<150&&scale>1.3f?View.GONE:View.VISIBLE);v.setViewVisibility(R.id.widget_time,h<125&&scale>1.6f?View.GONE:View.VISIBLE);v.setInt(R.id.widget_title,"setMaxLines",h>=160*scale?2:1);v.setViewVisibility(R.id.widget_room,h>=150*scale?View.VISIBLE:View.GONE);
   v.setInt(R.id.widget_title,"setBackgroundResource",next==null?0:CoursePalette.background(next.name));
   v.setViewPadding(R.id.widget_title,next==null?0:6,next==null?0:3,next==null?0:6,next==null?0:3);
   if(next!=null){v.setTextViewText(R.id.widget_title,next.displayName());v.setTextViewText(R.id.widget_time,next.time());v.setTextViewText(R.id.widget_room,next.room);v.setTextViewText(R.id.widget_header,offset==0?ScheduleData.state(next,now.toLocalTime())+" · "+date.getMonthValue()+"/"+date.getDayOfMonth():"明日第一节");}
   else {v.setTextViewText(R.id.widget_title,week<1||week>20?"学期范围外":list.isEmpty()?dayName+"没有课程":"今日课程已结束");v.setTextViewText(R.id.widget_time,dayName+" "+list.size()+" 个时段");v.setTextViewText(R.id.widget_room,"点按查看完整课表");}
   v.setViewVisibility(R.id.widget_rows,View.GONE);
  }else {
   float scale=Math.max(1f,c.getResources().getConfiguration().fontScale);int count=large?6:2,fit=Math.max(1,Math.min(count,(h-(int)(60*scale+16))/(int)(38*scale+16)));
   v.setViewVisibility(R.id.widget_title,list.isEmpty()?View.VISIBLE:View.GONE);v.setTextViewText(R.id.widget_title,week<1||week>20?"当前日期不在本学期内":dayName+"没有课程");
   v.setViewVisibility(R.id.widget_time,View.GONE);v.setViewVisibility(R.id.widget_room,View.GONE);
   int[] rowIds={R.id.row0,R.id.row1,R.id.row2,R.id.row3,R.id.row4,R.id.row5};int[] nameIds={R.id.row_name0,R.id.row_name1,R.id.row_name2,R.id.row_name3,R.id.row_name4,R.id.row_name5};int[] infoIds={R.id.row_info0,R.id.row_info1,R.id.row_info2,R.id.row_info3,R.id.row_info4,R.id.row_info5};
   // Wide cards prioritize a class still to come, while overview retains full daily order.
   List<ScheduleData.Course> shown=new ArrayList<>(list);if(!large&&offset==0){List<ScheduleData.Course> future=new ArrayList<>();for(ScheduleData.Course course:list)if(now.toLocalTime().isBefore(LocalTime.parse(course.endTime())))future.add(course);if(!future.isEmpty())shown=future;}
   for(int i=0;i<count;i++){boolean visible=i<shown.size()&&i<fit;v.setViewVisibility(rowIds[i],visible?View.VISIBLE:View.GONE);if(visible){ScheduleData.Course course=shown.get(i);v.setInt(rowIds[i],"setBackgroundResource",CoursePalette.background(course.name));v.setTextViewText(nameIds[i],course.displayName());v.setTextViewText(infoIds[i],course.time()+" · "+course.room);Intent courseOpen=new Intent(c,MainActivity.class).setAction("cn.gdcp.timetable.COURSE_"+i).putExtra("widget_date",date.toString()).putExtra("widget_course",indexOf(course)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);v.setOnClickPendingIntent(rowIds[i],PendingIntent.getActivity(c,id*10+i+200000,courseOpen,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));}}
   if(shown.size()>fit)v.setTextViewText(R.id.widget_footer,"第"+week+"周 · 还有"+(shown.size()-fit)+"节，点按查看");
  }
  v.setContentDescription(R.id.widget_content,dayName+"课表，"+date+"，共"+list.size()+"个课程时段。点按查看。");
  return v;
 }
 static int indexOf(ScheduleData.Course c){for(int i=0;i<ScheduleData.COURSES.length;i++)if(c==ScheduleData.COURSES[i])return i;return -1;}
 static void schedule(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);boolean any=false;for(Class<?> cls:PROVIDERS)if(m.getAppWidgetIds(new ComponentName(c,cls)).length>0)any=true;Intent intent=new Intent(c,WidgetRefreshReceiver.class).setAction(ACTION_REFRESH);PendingIntent pending=PendingIntent.getBroadcast(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(alarms==null)return;if(!any){alarms.cancel(pending);return;}long next=ScheduleData.nextRefresh(ZonedDateTime.now(ScheduleData.ZONE)).toInstant().toEpochMilli();alarms.setWindow(AlarmManager.RTC,next,15*60*1000,pending);}
}
