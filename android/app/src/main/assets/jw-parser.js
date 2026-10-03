(function(root){
 'use strict';
 function weeks(text){
  const spec=text.replace(/[（(]周[）)].*$/,'').replace(/周.*$/,'').replace(/[（()）\s]/g,'').replace(/，/g,',');
  const result=new Set();
  for(const part of spec.split(',')){const m=part.match(/^(\d+)(?:-(\d+))?(单|双)?$/);if(!m)throw Error('无法识别周次：'+text);const a=+m[1],b=+(m[2]||m[1]);if(a<1||b>20||b<a)throw Error('当前版本支持第1—20周');for(let w=a;w<=b;w++)if(!m[3]||(m[3]==='单'?w%2===1:w%2===0))result.add(w)}
  return [...result].sort((a,b)=>a-b);
 }
 function validate(data){
  if(!data||data.format!=='gdcp-personal-timetable'||data.version!==1||data.semester!=='2026-2027-1'||!Array.isArray(data.courses)||!data.courses.length||data.courses.length>2000)throw Error('个人课表格式、学期或课程数量无效');
  const clean={format:data.format,version:1,semester:data.semester,courses:[]};
  for(const c of data.courses){if(typeof c.name!=='string'||!c.name.trim()||c.name.length>200||typeof c.teacher!=='string'||c.teacher.length>200||typeof c.position!=='string'||c.position.length>300||!Number.isInteger(c.day)||c.day<1||c.day>7||!Array.isArray(c.sections)||!c.sections.length||c.sections.length>12||c.sections.some((s,i)=>!Number.isInteger(s)||s<1||s>12||(i&&s!==c.sections[i-1]+1))||!Array.isArray(c.weeks)||!c.weeks.length||c.weeks.length>20||c.weeks.some(w=>!Number.isInteger(w)||w<1||w>20))throw Error('课程数据无效');clean.courses.push({name:c.name.trim(),teacher:c.teacher,position:c.position,day:c.day,sections:[...c.sections],weeks:[...new Set(c.weeks)].sort((a,b)=>a-b)})}
  return clean;
 }
 function parse(doc){
  const table=doc.querySelector('#timetable');if(!table)throw Error('未找到个人课表，请先登录并打开“我的课表”的个人课表页面');
  const semester=doc.querySelector('#xnxq01id');if(semester&&semester.value!=='2026-2027-1')throw Error('请选择2026—2027第一学期');
  if(table.querySelector('tr')?.cells.length>10)throw Error('请使用个人课表，不能导入全校班级查询结果');
  const all=[];let errors=0;
  for(const block of table.querySelectorAll('.kbcontent')){
   const fonts=[...block.querySelectorAll('font')];const weekFonts=fonts.filter(f=>f.title?.includes('周次')&&/\[\d{1,2}-\d{1,2}节\]/.test(f.textContent));
   for(const wf of weekFonts){try{
    const start=fonts.indexOf(wf);const previous=fonts.slice(0,start);const lastWeek=previous.map(f=>f.title?.includes('周次')).lastIndexOf(true);const prefix=previous.slice(lastWeek+1);const named=prefix.filter(f=>!f.title&&!f.getAttribute('name')&&f.textContent.trim());
    const name=named.map(f=>f.textContent.trim()).join('');if(!name)throw Error('缺少课程名称');
    const teacher=[...prefix].reverse().find(f=>f.title==='教师')?.textContent.trim()||'';
    const nextWeek=fonts.slice(start+1).findIndex(f=>f.title?.includes('周次'));const suffix=fonts.slice(start+1,nextWeek<0?undefined:start+1+nextWeek);
    const position=suffix.find(f=>f.title==='教室')?.textContent.trim()||'';
    const id=block.id.match(/-(\d)-[124]$/);if(!id)throw Error('无法确定星期');
    const range=wf.textContent.match(/\[(\d{1,2})-(\d{1,2})节\]/);const a=+range[1],b=+range[2];
    all.push({name,teacher,position,day:+id[1],sections:Array.from({length:b-a+1},(_,i)=>a+i),weeks:weeks(wf.textContent)});
   }catch(e){errors++}}
  }
  if(errors)throw Error('有'+errors+'条课程无法解析，未导入任何数据');
  const merged=new Map();for(const c of all){const key=JSON.stringify([c.name,c.teacher,c.position,c.day,c.sections]);if(merged.has(key))merged.get(key).weeks=[...new Set([...merged.get(key).weeks,...c.weeks])].sort((a,b)=>a-b);else merged.set(key,c)}
  return validate({format:'gdcp-personal-timetable',version:1,semester:'2026-2027-1',courses:[...merged.values()]});
 }
 function find(doc){try{return parse(doc)}catch(original){for(const frame of doc.querySelectorAll('iframe,frame')){try{if(frame.contentDocument)return find(frame.contentDocument)}catch{}}throw original}}
 root.GDCPParser={parse,find,validate,weeks};
})(globalThis);
