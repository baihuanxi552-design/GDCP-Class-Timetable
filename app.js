let COURSES=[];
const TIMER={
  "totalWeek": 20,
  "startSemester": "1788105600000",
  "startWithSunday": false,
  "showWeekend": true,
  "forenoon": 4,
  "afternoon": 4,
  "night": 4,
  "sections": [
    {
      "section": 1,
      "startTime": "08:30",
      "endTime": "09:10"
    },
    {
      "section": 2,
      "startTime": "09:15",
      "endTime": "09:55"
    },
    {
      "section": 3,
      "startTime": "10:10",
      "endTime": "10:50"
    },
    {
      "section": 4,
      "startTime": "10:55",
      "endTime": "11:35"
    },
    {
      "section": 5,
      "startTime": "14:15",
      "endTime": "14:55"
    },
    {
      "section": 6,
      "startTime": "15:00",
      "endTime": "15:40"
    },
    {
      "section": 7,
      "startTime": "15:55",
      "endTime": "16:35"
    },
    {
      "section": 8,
      "startTime": "16:35",
      "endTime": "17:15"
    },
    {
      "section": 9,
      "startTime": "18:05",
      "endTime": "18:45"
    },
    {
      "section": 10,
      "startTime": "18:45",
      "endTime": "19:25"
    },
    {
      "section": 11,
      "startTime": "19:40",
      "endTime": "20:20"
    },
    {
      "section": 12,
      "startTime": "20:20",
      "endTime": "21:00"
    }
  ]
};
const START=Date.UTC(2026,7,31), DAY=86400000, TOTAL=20;
const $=id=>document.getElementById(id);
const dayNames=['一','二','三','四','五','六','日'];
const palette=[['#eaf0ff','#253f79','#4b6ff4'],['#fff0e6','#77421f','#ed975c'],['#e9f5ee','#265b42','#58a17b'],['#f2ebff','#5c3d86','#9d73db'],['#ffebf0','#803a51','#e87899'],['#e5f4f8','#295c6b','#51a3bb'],['#fff6db','#705d23','#dfb94d'],['#eaf3ff','#315681','#619fdb'],['#f0f2e7','#586633','#9aa864'],['#fcece4','#864832','#d89073'],['#efedff','#514c8a','#867cdf'],['#e9f6f3','#35675c','#70b8a5'],['#f7eaf4','#7b446e','#c882b5']];
let names=[...new Set(COURSES.map(c=>c.name))];
const campusNames=['清远校区','天河校区','花都校区','国际校区'];
const CATALOG_KEY='timetable.catalog.v1';
let catalog=window.SCHOOL_DATA;
try{const saved=localStorage.getItem(CATALOG_KEY);if(saved)catalog=JSON.parse(saved)}catch{}
let selectedClass=0,selectedCampus=0,setupCompleted=false,hasLegacyChoice=false,storageAvailable=true;
const PREFS_KEY='timetable.preferences.v1';
try{
 const raw=localStorage.getItem(PREFS_KEY);
 if(raw){const prefs=JSON.parse(raw);const index=catalog.classes.findIndex(c=>c.name===prefs.className);if(prefs.version===1&&index>=0&&Number.isInteger(prefs.campus)&&prefs.campus>=0&&prefs.campus<4){selectedClass=index;selectedCampus=prefs.campus;setupCompleted=true}}
 if(!setupCompleted){const a=localStorage.getItem('selectedClass'),b=localStorage.getItem('selectedCampus');const c=Number(a),p=Number(b);if(a!==null&&a!==undefined&&b!==null&&b!==undefined&&Number.isInteger(c)&&c>=0&&c<catalog.classes.length&&Number.isInteger(p)&&p>=0&&p<4){selectedClass=c;selectedCampus=p;hasLegacyChoice=true}}
}catch(e){storageAvailable=false}
let draftClass=-1,draftCampus=-1;
function decodeClass(index){const words=catalog.strings;return catalog.classes[index].courses.map(r=>({name:words[r[0]],teacher:words[r[1]],position:words[r[2]],day:r[3],sections:[r[4],r[4]+1],weeks:Array.from({length:20},(_,i)=>i+1).filter(w=>r[5]&(1<<(w-1)))}))}
if(catalog){COURSES=decodeClass(selectedClass);names=[...new Set(COURSES.map(c=>c.name))]}
const personal=()=>!catalog||catalog.classes[selectedClass].personal;
function delayed(room){if(selectedCampus===0)return /融新|融创|12#|13#|操场|运动场|体育馆/.test(room);if(selectedCampus===1)return /2号楼|2-|运动场|操场/.test(room);if(selectedCampus===2)return /10号楼|15号楼|16号楼|10-|15-|16-|运动场|操场/.test(room);return !/教学楼|运动场|操场/.test(room)}
function clock(c,end=false){const sec=end?c.sections[c.sections.length-1]:c.sections[0];if(delayed(c.position)){if(sec===3)return end?'11:10':'10:30';if(sec===4)return end?'11:55':'11:15'}return TIMER.sections[sec-1][end?'endTime':'startTime']}
const initial=new Date(new Date().toLocaleString('en-US',{timeZone:'Asia/Shanghai'}));
let todayStamp=Date.UTC(initial.getFullYear(),initial.getMonth(),initial.getDate());
let currentWeek=Math.floor((todayStamp-START)/(7*DAY))+1, selectedWeek=Math.max(1,Math.min(TOTAL,currentWeek)),selectedDay=(initial.getDay()+6)%7+1;
const fmt=d=>`${d.getUTCMonth()+1}月${d.getUTCDate()}日`;
const dateFor=(w,d)=>new Date(START+((w-1)*7+d-1)*DAY);
const forWeek=w=>COURSES.filter(c=>c.weeks.includes(w));
const timeFor=c=>`${clock(c)}–${clock(c,true)}`;
function weekRanges(weeks){let groups=[],a=weeks[0],b=a;for(let i=1;i<=weeks.length;i++){if(weeks[i]===b+1)b=weeks[i];else{groups.push(a===b?`${a}`:`${a}–${b}`);a=b=weeks[i]}}return `第 ${groups.join('、')} 周`}
function colorIndex(name){let hash=0;for(let i=0;i<name.length;i++)hash=((hash<<5)-hash+name.charCodeAt(i))|0;return (hash>>>1)%palette.length}
function createCourse(c,compact=false){const b=document.createElement('button');b.type='button';b.className=compact?'today-card':'course';const colors=palette[colorIndex(c.name)];b.style.setProperty('--bg',colors[0]);b.style.setProperty('--fg',colors[1]);b.style.setProperty('--accent',colors[2]);const a=colors[2].slice(1);const rgb=[0,2,4].map(i=>Math.round(parseInt(a.slice(i,i+2),16)*.18+28*.82));b.style.setProperty('--dark-bg',`rgb(${rgb.join(',')})`);let name=document.createElement('strong');name.textContent=c.name;b.append(name);if(!compact){let t=document.createElement('span');t.className='teacher';t.textContent=c.teacher;b.append(t);let p=document.createElement('span');p.textContent=c.position;b.append(p)}let tm=document.createElement('span');tm.className='course-time';tm.textContent=compact?`${timeFor(c)} · ${c.position}`:timeFor(c);b.append(tm);b.addEventListener('click',()=>showDetail(c));return b}
function showDetail(c){$('detailName').textContent=c.name;$('detailBody').replaceChildren();for(const [k,v] of [['教师',c.teacher],['教室',c.position],['时间',`星期${dayNames[c.day-1]} · ${timeFor(c)}`],['节次',`第 ${c.sections[0]}–${c.sections[c.sections.length-1]} 节`],['周次',weekRanges(c.weeks)]]){let dt=document.createElement('dt'),dd=document.createElement('dd');dt.textContent=k;dd.textContent=v;$('detailBody').append(dt,dd)}$('sportsNote').hidden=!c.name.includes('大学体育');$('sportsNote').textContent='分组选课以个人课表为准。';$('detail').showModal()}
function renderToday(){const courses=currentWeek>=1&&currentWeek<=TOTAL?forWeek(currentWeek).filter(c=>c.day===((initial.getDay()+6)%7+1)):[];$('todayTitle').textContent=`${fmt(new Date(todayStamp))} · 星期${dayNames[(initial.getDay()+6)%7]}`;$('todayCourses').replaceChildren();if(!courses.length){let p=document.createElement('p');p.className='today-empty';p.textContent=currentWeek<1||currentWeek>TOTAL?'当前日期不在本学期内。':personal()&&[3,4].includes(currentWeek)?'今天安排军训（含入学教育），具体时间以学校通知为准。':'今天没有排课。';$('todayCourses').append(p)}else courses.forEach(c=>$('todayCourses').append(createCourse(c,true)))}
function render(){$('trainingNotice').hidden=!personal()||![3,4].includes(selectedWeek);if($("weekTrigger"))$("weekTrigger").textContent=`第 ${selectedWeek} 周 ▾`;const courses=forWeek(selectedWeek);$('week').value=selectedWeek;$('weekTitle').textContent=`第 ${String(selectedWeek).padStart(2,'0')} 周`;$('range').textContent=`${fmt(dateFor(selectedWeek,1))} — ${fmt(dateFor(selectedWeek,7))} · ${courses.length} 个课程时段`;$('prev').disabled=selectedWeek===1;$('next').disabled=selectedWeek===TOTAL;$('semesterNotice').hidden=currentWeek>=1&&currentWeek<=TOTAL;$('semesterNotice').textContent='当前日期不在本学期内，可以切换教学周查看课程。';$('empty').hidden=courses.length>0;$('grid').replaceChildren();const corner=document.createElement('div');corner.className='day-head';corner.textContent='节次 / 时间';$('grid').append(corner);$('days').replaceChildren();for(let d=1;d<=7;d++){const dt=dateFor(selectedWeek,d),head=document.createElement('div');head.className='day-head'+(+dt===todayStamp?' is-today':'');head.append(`星期${dayNames[d-1]}`);let small=document.createElement('small');small.textContent=fmt(dt);head.append(small);$('grid').append(head);let btn=document.createElement('button');btn.type='button';btn.className=d===selectedDay?'active':'';btn.setAttribute('aria-pressed',d===selectedDay);btn.append(dayNames[d-1]);let date=document.createElement('small');date.textContent=dt.getUTCDate();btn.append(date);btn.onclick=()=>{selectedDay=d;render()};$('days').append(btn)}for(let s=1;s<=12;s+=2){const time=document.createElement('div');time.className='time';time.append(`${s}–${s+1}节`);let small=document.createElement('small');small.textContent=s===3?'10:10–11:35 / 10:30–11:55':TIMER.sections[s-1].startTime+'–'+TIMER.sections[s].endTime;time.append(small);$('grid').append(time);for(let d=1;d<=7;d++){let cell=document.createElement('div');cell.className='cell';courses.filter(c=>c.day===d&&c.sections[0]===s).forEach(c=>cell.append(createCourse(c)));$('grid').append(cell)}}$('daily').replaceChildren();const daily=courses.filter(c=>c.day===selectedDay);for(const c of daily){let row=document.createElement('div');row.className='daily-row';let label=document.createElement('div');label.className='time';label.append(`${c.sections[0]}–${c.sections[c.sections.length-1]}节`);let sm=document.createElement('small');sm.textContent=clock(c);label.append(sm);row.append(label,createCourse(c));$('daily').append(row)}if(!daily.length){let p=document.createElement('p');p.className='daily-empty';p.textContent='这一天没有排课。';$('daily').append(p)}}
for(let w=1;w<=TOTAL;w++){let op=document.createElement('option');op.value=w;op.textContent=`第 ${w} 周`;$('week').append(op)}$('week').onchange=e=>{selectedWeek=Number(e.target.value);render()};$('prev').onclick=()=>{selectedWeek--;render()};$('next').onclick=()=>{selectedWeek++;render()};$('current').onclick=()=>{selectedWeek=Math.max(1,Math.min(TOTAL,currentWeek));selectedDay=(initial.getDay()+6)%7+1;render()};$('close').onclick=()=>$('detail').close();$('detail').addEventListener('click',e=>{if(e.target===$('detail')){const r=e.target.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)e.target.close()}});renderToday();render();

window.refreshToday=function(){const now=new Date(new Date().toLocaleString("en-US",{timeZone:"Asia/Shanghai"}));const stamp=Date.UTC(now.getFullYear(),now.getMonth(),now.getDate());if(stamp===todayStamp)return;initial.setTime(now.getTime());todayStamp=stamp;currentWeek=Math.floor((todayStamp-START)/(7*DAY))+1;renderToday();render()};



$('weekTrigger').onclick=()=>{const options=$('weekOptions');options.replaceChildren();for(let w=1;w<=20;w++){const b=document.createElement('button');b.type='button';b.textContent=String(w);b.setAttribute('aria-label',`第 ${w} 周`);b.setAttribute('aria-pressed',w===selectedWeek);b.className=w===selectedWeek?'active':'';b.onclick=()=>{selectedWeek=w;render();$('weekPicker').close()};options.append(b)}$('weekPicker').showModal()};$('weekClose').onclick=()=>$('weekPicker').close();$('weekPicker').addEventListener('click',e=>{if(e.target===$('weekPicker')){const r=e.target.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)e.target.close()}});document.addEventListener('visibilitychange',()=>{if(!document.hidden)window.refreshToday()});if(typeof setInterval==='function')setInterval(()=>window.refreshToday(),60000);

function settings(){
 $('classTitle').textContent=catalog.classes[selectedClass].name;$('campusTitle').textContent='广东交通职业技术学院 · '+campusNames[selectedCampus];$('selectedClassText').textContent=catalog.classes[selectedClass].name;$('selectedCampusText').textContent=campusNames[selectedCampus];
 const notes=$('scheduleNotes');notes.replaceChildren();const h=document.createElement('h3');h.textContent='课表说明';notes.append(h);
 const lines=['本版本不包含内置课表，请从设置导入备份或自行添加课程。'];
 for(const line of lines){const p=document.createElement('p');p.textContent=line;notes.append(p)}

}
function updateDraft(){
 $('draftClassText').textContent=draftClass>=0?'已选课表：'+catalog.classes[draftClass].name:'请选择课表';$('setupConfirm').disabled=draftClass<0||draftCampus<0;
 for(const radio of document.querySelectorAll('input[name="setupCampus"]'))radio.checked=Number(radio.value)===draftCampus;
}
function classOptions(){const query=$('classSearch').value.trim().toLowerCase(),list=$('classOptions');list.replaceChildren();let count=0;catalog.classes.forEach((c,i)=>{if(!c.name.toLowerCase().includes(query))return;count++;const label=document.createElement('label');label.className='class-option';const radio=document.createElement('input');radio.type='radio';radio.name='setupClass';radio.value=i;radio.checked=i===draftClass;radio.onchange=()=>{draftClass=i;updateDraft()};const span=document.createElement('span');span.textContent=c.name;label.append(radio,span);list.append(label)});$('classCount').textContent=count?`${count} 个选项`:'没有找到课表，试试专业名称或年级。'}
function openSetup(){
 const existing=setupCompleted||hasLegacyChoice;draftClass=existing?selectedClass:-1;draftCampus=existing?selectedCampus:-1;$('classSearch').value='';$('classPickerTitle').textContent=setupCompleted?'更改课表':'设置你的课表';$('setupIntro').textContent=setupCompleted?'更改课表和校区，保存后更新课表及本地设置。':'只需设置一次，之后打开会自动读取。你可以随时更改。';$('setupConfirm').textContent=setupCompleted?'保存更改':'保存并查看课表';$('classClose').hidden=!setupCompleted;$('setupCancel').hidden=!setupCompleted;$('storageNotice').hidden=storageAvailable;$('storageNotice').textContent='浏览器未允许本地存储，本次设置只在当前页面有效。';classOptions();updateDraft();$('classPicker').showModal();$('classPickerTitle').focus();
}
function cancelSetup(){if(setupCompleted)$('classPicker').close()}
function confirmSetup(){
 if(draftClass<0||draftCampus<0)return;
 selectedClass=draftClass;selectedCampus=draftCampus;COURSES=decodeClass(selectedClass);names=[...new Set(COURSES.map(c=>c.name))];
 try{localStorage.setItem(PREFS_KEY,JSON.stringify({version:1,className:catalog.classes[selectedClass].name,campus:selectedCampus}));storageAvailable=true}catch(e){storageAvailable=false}
 setupCompleted=true;settings();renderToday();render();$('classPicker').close();
}
$('classTrigger').onclick=openSetup;$('classSearch').oninput=classOptions;$('classClose').onclick=cancelSetup;$('setupCancel').onclick=cancelSetup;$('setupConfirm').onclick=confirmSetup;
$('classPicker').addEventListener('cancel',e=>{if(!setupCompleted)e.preventDefault()});
for(const radio of document.querySelectorAll('input[name="setupCampus"]'))radio.onchange=()=>{draftCampus=Number(radio.value);updateDraft()};
const viewButtons=document.querySelectorAll('button[data-view]');for(const b of viewButtons)b.onclick=()=>{document.querySelector('main').dataset.view=b.dataset.view;for(const v of viewButtons)v.setAttribute('aria-pressed',v===b)};settings();
