package cn.gdcp.timetable;
public class ScheduleApplication extends android.app.Application {
 @Override public void onCreate(){super.onCreate();SchoolData.initialize(this);}
}
