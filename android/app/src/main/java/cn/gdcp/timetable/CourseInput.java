package cn.gdcp.timetable;
import java.util.*;
public final class CourseInput {
 public static void validate(String name,int day,int first,int last){if(name.trim().isEmpty())throw new IllegalArgumentException("请填写课程名称");if(day<1||day>7)throw new IllegalArgumentException("星期需为1至7");if(first<1||last>12||last<first)throw new IllegalArgumentException("节次需为1至12，结束节次不能早于开始节次");}
 public static int[] weeks(String text){TreeSet<Integer> result=new TreeSet<>();try{for(String part:text.trim().replace('，',',').replace('、',',').replace('—','-').replace('–','-').split(",",-1)){String[] range=part.trim().split("-",-1);if(range.length>2)throw new Exception();int a=Integer.parseInt(range[0].trim()),b=range.length==2?Integer.parseInt(range[1].trim()):a;if(a<1||b>20||a>b)throw new Exception();for(int w=a;w<=b;w++)result.add(w);}if(result.isEmpty())throw new Exception();}catch(Exception e){throw new IllegalArgumentException("周次格式示例：1-16 或 1,3,5-8；范围1至20");}return result.stream().mapToInt(Integer::intValue).toArray();}
}
