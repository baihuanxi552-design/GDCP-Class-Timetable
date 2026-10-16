package cn.gdcp.timetable;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.content.res.Configuration;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout;
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
 @Override public void onDeleted(Context c,int[] ids){android.content.SharedPreferences.Editor e=c.getSharedPreferences("widget_dates",Context.MODE_PRIVATE).edit();for(int id:ids)e.remove("mode_"+id).remove("date_"+id);e.apply();}
 static LocalDate selectedDate(ZonedDateTime now,int mode,String fixed){return mode==2?LocalDate.parse(fixed):ScheduleData.widgetDate(now.toLocalDate(),now.toLocalTime(),mode);}
 static boolean saveDate(Context c,int id,int mode,String fixed){return c.getSharedPreferences("widget_dates",Context.MODE_PRIVATE).edit().putInt("mode_"+id,mode).putString("date_"+id,fixed).commit();}
 static void update(Context c,AppWidgetManager m,int id){
  SchoolData.reload(c);if(providerFor(c,id)==null)return;
  Bundle opts=m.getAppWidgetOptions(id);String style=autoStyle();
  ZonedDateTime now=ZonedDateTime.now(ScheduleData.ZONE);android.content.SharedPreferences prefs=c.getSharedPreferences("widget_dates",Context.MODE_PRIVATE);int mode=prefs.getInt("mode_"+id,0);String fixed=prefs.getString("date_"+id,now.toLocalDate().toString());LocalDate date=selectedDate(now,mode,fixed);int offset=mode==2?2:date.isAfter(now.toLocalDate())?1:0;
  // Use one complete RemoteViews per update. Some launchers retain stale child
  // visibility when swapping between a responsive RemoteViews container's layouts.
  Class<?> cls=providerFor(c,id);
  boolean portrait=c.getResources().getConfiguration().orientation!=Configuration.ORIENTATION_LANDSCAPE;
  String widthKey=portrait?AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH:AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH;
  String heightKey=portrait?AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT:AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT;
  int w=opts.getInt(widthKey,cls==NextWidgetProvider.class?110:300);
  int h=opts.getInt(heightKey,cls==OverviewWidgetProvider.class?300:150);
  if(w<=0)w=cls==NextWidgetProvider.class?110:300;
  if(h<=0)h=cls==OverviewWidgetProvider.class?300:150;
  m.updateAppWidget(id,views(c,id,style,date,now,w,h,offset));
 }
 static RemoteViews views(Context c,int id,String style,LocalDate date,ZonedDateTime now,int w,int h,int offset){
  float fontScale=Math.max(1f,c.getResources().getConfiguration().fontScale);
  int layout=R.layout.widget_large;
  RemoteViews v=new RemoteViews(c.getPackageName(),layout);v.setInt(android.R.id.background,"setBackgroundResource",background(style));
  List<ScheduleData.Course> list=ScheduleData.courses(date);int week=ScheduleData.week(date);String dayName=offset==0?"今日":offset==1?"明日":"指定日期";
  v.setTextViewText(R.id.widget_header,date.getMonthValue()+"月"+date.getDayOfMonth()+"日 · "+dayName+"课表");
  Intent open=new Intent(c,MainActivity.class).setAction("cn.gdcp.timetable.OPEN_WIDGET").putExtra("widget_date",date.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  v.setOnClickPendingIntent(R.id.widget_content,PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  Intent edit=new Intent(c,WidgetSettingsActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
  v.setOnClickPendingIntent(R.id.widget_settings,PendingIntent.getActivity(c,id+100000,edit,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  Intent refresh=new Intent(c,WidgetRefreshReceiver.class).setAction(ACTION_REFRESH);
  v.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(c,0,refresh,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  String footer=week<1||week>20?"本学期范围外":"第"+week+"周 · 点按查看";
  v.setTextViewText(R.id.widget_footer,SchoolData.title()+" · "+footer);
  v.setViewVisibility(R.id.widget_content,View.VISIBLE);
  v.setViewVisibility(R.id.widget_header,View.VISIBLE);
  v.setViewVisibility(R.id.widget_status,View.GONE);
  v.setViewVisibility(R.id.widget_bottom,View.VISIBLE);
  v.setViewVisibility(R.id.widget_rows,View.VISIBLE);
  v.setViewVisibility(R.id.widget_title,list.isEmpty()?View.VISIBLE:View.GONE);
  v.setTextViewText(R.id.widget_title,week<1||week>20?"学期范围外":dayName+"没有课程");
  v.setViewVisibility(R.id.widget_time,View.GONE);v.setViewVisibility(R.id.widget_room,View.GONE);
  List<ScheduleData.Course> shown=new ArrayList<>(list);
  if(offset==0){List<ScheduleData.Course> future=new ArrayList<>();for(ScheduleData.Course course:list)if(now.toLocalTime().isBefore(LocalTime.parse(course.endTime())))future.add(course);if(!future.isEmpty())shown=future;}
  int[] rowIds={R.id.row0,R.id.row1,R.id.row2,R.id.row3,R.id.row4,R.id.row5};
  int[] nameIds={R.id.row_name0,R.id.row_name1,R.id.row_name2,R.id.row_name3,R.id.row_name4,R.id.row_name5};
  int[] infoIds={R.id.row_info0,R.id.row_info1,R.id.row_info2,R.id.row_info3,R.id.row_info4,R.id.row_info5};
  int[] roomIds={R.id.row_room0,R.id.row_room1,R.id.row_room2,R.id.row_room3,R.id.row_room4,R.id.row_room5};
  int count=Math.min(6,shown.size());
  // Fill complete cards first; measure the actual Android view tree instead of
  // estimating rows from arbitrary width/height thresholds.
  for(int i=0;i<6;i++){
   v.setViewVisibility(rowIds[i],i<count?View.VISIBLE:View.GONE);
   if(i<count){ScheduleData.Course course=shown.get(i);
    v.setViewVisibility(nameIds[i],View.VISIBLE);v.setViewVisibility(infoIds[i],View.VISIBLE);
    v.setInt(rowIds[i],"setBackgroundResource",CoursePalette.background(course.name));
    v.setInt(nameIds[i],"setMaxLines",100);v.setTextViewText(nameIds[i],course.displayName());
    v.setInt(infoIds[i],"setMaxLines",100);v.setTextViewText(infoIds[i],course.time()+(course.teacher.isEmpty()?"":"\n"+course.teacher));
    int roomLines=Math.max(1,WidgetRoomPages.split(course.room,t->roomPaint(c).measureText(t),Math.max(1,w-32)*c.getResources().getDisplayMetrics().density).size());
    setRoom(c,v,roomIds[i],course.room,w-32,roomLines);
    v.setViewVisibility(roomIds[i],course.room.isEmpty()?View.GONE:View.VISIBLE);
    Intent courseOpen=new Intent(c,MainActivity.class).setAction("cn.gdcp.timetable.COURSE_"+i).putExtra("widget_date",date.toString()).putExtra("widget_course",indexOf(course)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
    v.setOnClickPendingIntent(rowIds[i],PendingIntent.getActivity(c,id*10+i+200000,courseOpen,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
   }
  }
  if(measuredHeight(c,v,w)>h){ // Teacher is the first optional field removed, globally.
   for(int i=0;i<count;i++)v.setTextViewText(infoIds[i],shown.get(i).time());
  }
  if(measuredHeight(c,v,w)>h){ // Long rooms rotate, with time always separate.
   for(int i=0;i<count;i++)setRoom(c,v,roomIds[i],shown.get(i).room,w-32,1);
  }
  while(count>1&&measuredHeight(c,v,w)>h){count--;v.setViewVisibility(rowIds[count],View.GONE);}
  if(count==1&&measuredHeight(c,v,w)>h){
   // Extreme sizes retain a colored card and cycle all its essential fields.
   ScheduleData.Course course=shown.get(0);
   v.setViewVisibility(nameIds[0],View.GONE);v.setViewVisibility(infoIds[0],View.GONE);
   v.setViewVisibility(R.id.widget_header,View.GONE);v.setViewVisibility(R.id.widget_bottom,View.GONE);
   int lines=Math.max(1,(int)((h-44)/(16*fontScale)));
   String details=course.displayName()+"\n"+course.time()+"\n"+course.room;
   setRoom(c,v,roomIds[0],details,w-32,lines);
   while(lines>1&&measuredHeight(c,v,w)>h)setRoom(c,v,roomIds[0],details,w-32,--lines);
  }
  if(shown.size()>count)v.setTextViewText(R.id.widget_footer,"还有"+(shown.size()-count)+"节 · 点按查看全部");
  v.setContentDescription(R.id.widget_content,dayName+"课表，"+date+"，共"+list.size()+"个课程时段。点按查看。");
  return v;
 }
 static TextPaint roomPaint(Context c){TextPaint paint=new TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG);paint.setTextSize(12*c.getResources().getDisplayMetrics().density*Math.max(1f,c.getResources().getConfiguration().fontScale));return paint;}
 static float measuredHeight(Context c,RemoteViews remote,int widthDp){
  View tree=remote.apply(c,null);float density=c.getResources().getDisplayMetrics().density;
  tree.measure(View.MeasureSpec.makeMeasureSpec(Math.max(1,(int)(widthDp*density)),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
  return tree.getMeasuredHeight()/density;
 }
 static void setRoom(Context c,RemoteViews parent,int id,String room,int widthDp){setRoom(c,parent,id,room,widthDp,1);}
 static void setRoom(Context c,RemoteViews parent,int id,String room,int widthDp,int lines){
  parent.removeAllViews(id);TextPaint paint=new TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG);paint.setTextSize(12*c.getResources().getDisplayMetrics().density*Math.max(1f,c.getResources().getConfiguration().fontScale));float width=Math.max(20,widthDp-8)*c.getResources().getDisplayMetrics().density;
  List<String> parts=WidgetRoomPages.split(room,text->paint.measureText(text),width);List<String> pages=new ArrayList<>();for(int i=0;i<parts.size();i+=lines)pages.add(String.join("\n",parts.subList(i,Math.min(parts.size(),i+lines))));
  for(String page:pages){RemoteViews child=new RemoteViews(c.getPackageName(),R.layout.widget_room_page);child.setTextViewText(R.id.widget_room_text,page);child.setContentDescription(R.id.widget_room_text,room);parent.addView(id,child);}parent.setDisplayedChild(id,0);
 }
 static String shortRoom(String room){return room.replaceAll("[（(]\\s*\\d+\\s*[#＃]\\s*[）)]","").replaceAll("\\s+","").replace("（清远）","").replace("(清远)","");}
 static float textHeight(Context c,String text,int sp,int widthDp,int maxLines){float density=c.getResources().getDisplayMetrics().density;TextPaint paint=new TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG);paint.setTextSize(sp*density*Math.max(1f,c.getResources().getConfiguration().fontScale));StaticLayout layout=StaticLayout.Builder.obtain(text,0,text.length(),paint,Math.max(1,(int)(widthDp*density))).setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setMaxLines(maxLines).build();return layout.getHeight()/density+4;}
 static int indexOf(ScheduleData.Course c){for(int i=0;i<ScheduleData.COURSES.length;i++)if(c==ScheduleData.COURSES[i])return i;return -1;}
 static void schedule(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);boolean any=false;for(Class<?> cls:PROVIDERS)if(m.getAppWidgetIds(new ComponentName(c,cls)).length>0)any=true;Intent intent=new Intent(c,WidgetRefreshReceiver.class).setAction(ACTION_REFRESH);PendingIntent pending=PendingIntent.getBroadcast(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(alarms==null)return;if(!any){alarms.cancel(pending);return;}long next=ScheduleData.nextRefresh(ZonedDateTime.now(ScheduleData.ZONE)).toInstant().toEpochMilli();alarms.setWindow(AlarmManager.RTC,next,15*60*1000,pending);}
}
