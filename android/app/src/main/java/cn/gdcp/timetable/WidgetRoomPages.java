package cn.gdcp.timetable;
import java.util.*;
import java.util.function.ToDoubleFunction;
final class WidgetRoomPages {
 static List<String> split(String text,ToDoubleFunction<String> measure,double width){
  List<String> pages=new ArrayList<>();StringBuilder part=new StringBuilder();
  for(int at=0;at<text.length();){int code=text.codePointAt(at);String next=new String(Character.toChars(code));at+=Character.charCount(code);if(code==10){pages.add(part.toString());part.setLength(0);continue;}if(part.length()>0&&measure.applyAsDouble(part.toString()+next)>width){pages.add(part.toString());part.setLength(0);}part.append(next);}
  if(part.length()>0||pages.isEmpty())pages.add(part.toString());return pages;
 }
}
