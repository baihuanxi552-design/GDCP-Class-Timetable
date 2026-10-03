package cn.gdcp.timetable

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import android.os.Build
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZonedDateTime
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.*
import top.yukonga.miuix.kmp.utils.PressFeedbackType

class MainActivity : ComponentActivity() {
    private var currentDate by mutableStateOf(LocalDate.now(ScheduleData.ZONE))
    private var target by mutableStateOf<Triple<String, Int, Long>?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) readTarget(intent)
        setContent {
            val dark = isSystemInDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window,window.decorView).apply {
                    isAppearanceLightNavigationBars=!dark
                    isAppearanceLightStatusBars=!dark
                }
                @Suppress("DEPRECATION")
                if(Build.VERSION.SDK_INT<35) {window.statusBarColor=android.graphics.Color.TRANSPARENT;window.navigationBarColor=android.graphics.Color.TRANSPARENT}
                if(Build.VERSION.SDK_INT>=29) window.isNavigationBarContrastEnforced=false
            }
            MiuixTheme(colors = if (dark) darkColorScheme() else lightColorScheme()) {
                LaunchedEffect(Unit) { while (true) { currentDate = LocalDate.now(ScheduleData.ZONE); delay(30_000) } }
                Timetable(currentDate, target, dark)
            }
        }
    }
    override fun onResume() { super.onResume();currentDate = LocalDate.now(ScheduleData.ZONE);BaseWidgetProvider.updateAll(this) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent);setIntent(intent);readTarget(intent) }
    private fun readTarget(intent: Intent) { intent.getStringExtra("widget_date")?.let { target = Triple(it, intent.getIntExtra("widget_course", -1), System.nanoTime()) } }
}

private val days = listOf("一", "二", "三", "四", "五", "六", "日")
private val accents = CoursePalette.ACCENTS.map { it.toLong() and 0xFFFFFFFFL }
private fun colorIndex(name:String)=CoursePalette.index(name)
private fun dateFor(week: Int, day: Int): LocalDate = ScheduleData.START.plusDays(((week-1)*7+day-1).toLong())
private fun dateText(date: LocalDate) = "${date.monthValue}月${date.dayOfMonth}日"
private fun weekCourses(week: Int) = (1..7).flatMap { ScheduleData.courses(dateFor(week,it)) }
private fun weekRanges(weeks: IntArray): String { val ranges= mutableListOf<String>();var start=weeks.first();var end=start;for(w in weeks.drop(1)){if(w==end+1)end=w else{ranges += if(start==end) "$start" else "$start–$end";start=w;end=w}};ranges += if(start==end) "$start" else "$start–$end";return "第 ${ranges.joinToString("、")} 周" }

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun Timetable(today: LocalDate, target: Triple<String,Int,Long>?, dark: Boolean) {
    val context=LocalContext.current
    var selectedClass by rememberSaveable { mutableIntStateOf(SchoolData.selected) }
    var selectedCampus by rememberSaveable { mutableIntStateOf(SchoolData.campus) }
    var configured by remember { mutableStateOf(SchoolData.configured(context) || SchoolData.NAMES.size==1 && ScheduleData.COURSES.isEmpty()) }
    val settingsShown=remember { mutableStateOf(false) }
    if(!configured) {
        Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background).imePadding()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                MotionText("设置你的课表",fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=24.dp))
                MotionText("选择班级和校区，只需设置一次。之后打开会自动读取，你也可以随时更改。",fontSize=14.sp,color=MiuixTheme.colorScheme.onSurfaceVariantSummary)
                SetupChoices(if(SchoolData.hadCachedChoice) selectedClass else -1,if(SchoolData.hadCachedChoice) selectedCampus else -1,{ index,campus,groups ->
                    SchoolData.confirmGroups(context,index,campus,groups);selectedClass=index;selectedCampus=campus;configured=true;BaseWidgetProvider.updateAll(context)
                },null)
            }
        }
        return
    }
    var customShown by rememberSaveable { mutableStateOf(false) }
    var courseRevision by remember { mutableIntStateOf(0) }
    var settingsPage by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val courseScroll=rememberLazyListState()
    val aboutScroll=rememberLazyListState()
    val expansion=remember { Animatable(if(customShown) 1f else 0f) }
    var customMounted by remember { mutableStateOf(customShown) }
    var addBounds by remember { mutableStateOf(Rect.Zero) }
    var expansionStart by remember { mutableStateOf(Rect.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    val keyboard=LocalSoftwareKeyboardController.current
    val focus=LocalFocusManager.current
    val scope=rememberCoroutineScope()
    val density=androidx.compose.ui.platform.LocalDensity.current
    val ime=WindowInsets.ime
    var closing by remember { mutableStateOf(false) }
    fun closeCustom() {
        if(closing) return
        closing=true
        focus.clearFocus();keyboard?.hide()
        scope.launch {
            withTimeoutOrNull(450) { snapshotFlow {ime.getBottom(density)}.first {it==0} }
            if(addBounds!=Rect.Zero) expansionStart=Rect(addBounds.left-rootOrigin.x,addBounds.top-rootOrigin.y,addBounds.right-rootOrigin.x,addBounds.bottom-rootOrigin.y)
            customShown=false
            closing=false
        }
    }
    LaunchedEffect(customShown) {
        if(customShown) customMounted=true
        expansion.animateTo(if(customShown) 1f else 0f,tween(if(customShown) 440 else 480,easing=FastOutSlowInEasing))
        if(!customShown) customMounted=false
    }
    BackHandler(enabled=settingsShown.value || customMounted || settingsPage || page!=0) {
        when {
            customMounted -> closeCustom()
            settingsShown.value -> {focus.clearFocus();keyboard?.hide();settingsShown.value=false}
            settingsPage -> settingsPage=false
            else -> page=0
        }
    }
    fun closeSettings(edit:Boolean) {
        focus.clearFocus();keyboard?.hide()
        scope.launch {
            withTimeoutOrNull(450) {snapshotFlow {ime.getBottom(density)}.first {it==0}}
            if(edit) settingsShown.value=false else settingsPage=false
        }
    }
    val currentWeek=ScheduleData.week(today)
    var week by rememberSaveable { mutableIntStateOf(currentWeek.coerceIn(1,20)) }
    var day by rememberSaveable { mutableIntStateOf(today.dayOfWeek.value) }
    var detailIndex by rememberSaveable { mutableIntStateOf(-1) }
    val weekPicker=remember { mutableStateOf(false) }
    val detailShown=remember { mutableStateOf(false) }
    LaunchedEffect(target) { target?.let { (date,index) -> runCatching { LocalDate.parse(date) }.getOrNull()?.let { week=ScheduleData.week(it).coerceIn(1,20);day=it.dayOfWeek.value;detailIndex=index;detailShown.value=index in ScheduleData.COURSES.indices } } }
    LaunchedEffect(detailIndex) { detailShown.value=detailIndex in ScheduleData.COURSES.indices }
    fun show(course: ScheduleData.Course) { detailIndex=ScheduleData.COURSES.indexOf(course);detailShown.value=true }
    val colors=MiuixTheme.colorScheme
    val settingsRoute=if(settingsShown.value) 2 else if(settingsPage) 1 else 0
    val settingsTransition=updateTransition(settingsRoute,label="settings-routes")
    val mainTransition=updateTransition(page,label="main-pages")
    val settingsMounted=settingsTransition.currentState!=0 || settingsRoute!=0
    val settingsShade by settingsTransition.animateFloat(transitionSpec={tween(500,easing=FastOutSlowInEasing)},label="settings-shade") {if(it==0) 0f else .20f}
    val dockHidden by settingsTransition.animateFloat(transitionSpec={if(initialState!=0 && targetState==0) tween(300,delayMillis=220,easing=FastOutSlowInEasing) else tween(480,easing=FastOutSlowInEasing)},label="dock-slide") {if(it==0) 0f else 1f}
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background).onGloballyPositioned {rootOrigin=it.positionInRoot()}) {
        val screenSafe=WindowInsets.safeDrawing.asPaddingValues()
        val autoWeekly=maxWidth>=600.dp || maxWidth>=maxHeight*1.25f
        var viewPreference by rememberSaveable { mutableIntStateOf(0) }
        val wide=if(viewPreference==0) autoWeekly else viewPreference==2
        val backdrop=rememberGraphicsLayer()
        var backdropOrigin by remember { mutableStateOf(Offset.Zero) }
        val textOverlays=remember {TextOverlayRegistry()}
        val textMidpoint=with(density){(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()+128.dp).toPx()}
        val backgroundDepth=maxOf(expansion.value,settingsShade/.20f)
        Box(Modifier.fillMaxSize().then(depthSurface(backgroundDepth)).then(if(customMounted || settingsMounted) Modifier.clearAndSetSemantics {}.blockMotionInput() else Modifier).onGloballyPositioned { backdropOrigin=it.positionInRoot() }.drawWithContent {
            backdrop.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop)
        }) {
        mainTransition.AnimatedContent(modifier=Modifier.fillMaxSize(),transitionSpec={
            val direction=if(targetState>initialState) 1 else -1
            slideInHorizontally(tween(420,easing=FastOutSlowInEasing)) {direction*it} togetherWith
                slideOutHorizontally(tween(420,easing=FastOutSlowInEasing)) {-direction*it}
        }) { visiblePage ->
        val textDepth = transition.animateFloat(transitionSpec={tween(420,easing=FastOutSlowInEasing)},label="header-text-depth") {state -> if(state==EnterExitState.Visible) 0f else 1f}
        val pageCoordinates=remember {PageCoordinates()}
        val textBlurCache=remember(density) {TextBlurCache(with(density){8.dp.toPx()})}
        val textMotion=remember(textDepth,textMidpoint,mainTransition.isRunning,pageCoordinates,textOverlays,textBlurCache) {TextMotion(textDepth,textMidpoint,mainTransition.isRunning,pageCoordinates,textOverlays,textBlurCache)}
        CompositionLocalProvider(LocalTextMotion provides textMotion) {
        Box(Modifier.fillMaxSize().onGloballyPositioned {pageCoordinates.value=it}.then(pageSwipe(visiblePage==page && !customMounted && !settingsMounted) {delta -> page=(page+delta).coerceIn(0,1)}).background(colors.background).then(if(visiblePage!=page || customMounted || settingsMounted) Modifier.clearAndSetSemantics {}.blockMotionInput() else Modifier)) {
        if(visiblePage==1) {
            val aboutLayer=remember {HazeState()}
            val edgeTarget=if(aboutScroll.firstVisibleItemIndex>0) 1f else (aboutScroll.firstVisibleItemScrollOffset/with(density){24.dp.toPx()}).coerceIn(0f,1f)
            val edgeStrength by animateFloatAsState(edgeTarget,tween(210),label="about-top-edge")
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().hazeSource(aboutLayer)) {
                    AboutPage(onSettings={settingsPage=true},scroll=aboutScroll)
                }
                TopScrollEdge(aboutLayer,edgeStrength,dark,title="关于",titleReveal=titleReveal(aboutScroll))
            }
        } else key(selectedClass,selectedCampus,courseRevision) {
        val courseLayer=remember {HazeState()}
        val edgeTarget=if(courseScroll.firstVisibleItemIndex>0) 1f else (courseScroll.firstVisibleItemScrollOffset/with(density){24.dp.toPx()}).coerceIn(0f,1f)
        val edgeStrength by animateFloatAsState(edgeTarget,tween(210),label="top-edge")
        Box(Modifier.fillMaxSize().clipToBounds()) {
        LazyColumn(Modifier.fillMaxSize().hazeSource(courseLayer),state=courseScroll, contentPadding=PaddingValues(start=18.dp+screenSafe.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),end=18.dp+screenSafe.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),top=18.dp+screenSafe.calculateTopPadding(),bottom=132.dp+screenSafe.calculateBottomPadding()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item { Column(Modifier.padding(horizontal=6.dp,vertical=12.dp)) {
                Box(Modifier.height(20.dp)) { MotionText("广东交通职业技术学院 · ${SchoolData.CAMPUSES[selectedCampus]}",fontSize=12.sp,color=colors.onSurfaceVariantSummary) }
                MotionText("班级课表",headerDepth=titleCollapse(courseScroll),fontSize=36.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=10.dp))
                MotionText("${SchoolData.title()}\n2026—2027 · 第一学期",fontSize=13.sp,color=colors.onSurfaceVariantSummary)
            } }
            item { Card(insideMargin=PaddingValues(16.dp),colors=CardDefaults.defaultColors(color=colors.secondaryContainer)) {
                MotionText("桌面小部件",fontWeight=FontWeight.SemiBold,fontSize=14.sp)
                MotionText("长按手机桌面空白处，在“小部件 / 窗口小工具”中找到“班级课表”；小米请进入“安卓小部件”。",fontSize=12.sp,modifier=Modifier.padding(top=6.dp))
            } }
            item { Card(insideMargin=PaddingValues(20.dp)) {
                MotionText("今天",fontSize=12.sp,color=colors.onSurfaceVariantSummary)
                MotionText("${dateText(today)} · 星期${days[today.dayOfWeek.value-1]}",fontSize=23.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp,bottom=16.dp))
                val list=ScheduleData.courses(today)
                if(list.isEmpty()) MotionText(if(selectedClass==0 && currentWeek in 3..4) "今天安排军训（含入学教育），具体时间以学校通知为准。" else if(currentWeek in 1..20) "今天没有排课。" else "当前日期不在本学期内。",fontSize=15.sp,color=colors.onSurfaceVariantSummary)
                list.forEach { CourseCard(it,dark,true) { show(it) };Spacer(Modifier.height(8.dp)) }
            } }
            item { Card(insideMargin=PaddingValues(18.dp)) {
                MotionText("教学安排",fontSize=12.sp,color=colors.onSurfaceVariantSummary)
                MotionText("第 ${week.toString().padStart(2,'0')} 周",fontSize=27.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=8.dp))
                MotionText("${dateText(dateFor(week,1))} — ${dateText(dateFor(week,7))} · ${weekCourses(week).size} 个课程时段",fontSize=13.sp,color=colors.onSurfaceVariantSummary)
                FlowRow(Modifier.fillMaxWidth().padding(top=16.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    TextButton("‹",{week--},enabled=week>1,minWidth=44.dp,insideMargin=PaddingValues(12.dp),modifier=Modifier.semantics { contentDescription="上一周" })
                    TextButton("第 $week 周",{weekPicker.value=true},modifier=Modifier.semantics { contentDescription="选择教学周" })
                    TextButton("›",{week++},enabled=week<20,minWidth=44.dp,insideMargin=PaddingValues(12.dp),modifier=Modifier.semantics { contentDescription="下一周" })
                    TextButton("返回本周",{week=currentWeek.coerceIn(1,20);day=today.dayOfWeek.value},colors=ButtonDefaults.textButtonColorsPrimary())
                }
                if(currentWeek !in 1..20) MotionText("当前日期不在本学期内，可以浏览全部教学周。",fontSize=13.sp,modifier=Modifier.padding(top=14.dp))
                Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton("日课表",{viewPreference=1},Modifier.weight(1f).semantics { selected=!wide },colors=if(!wide) ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors())
                    TextButton("周课表",{viewPreference=2},Modifier.weight(1f).semantics { selected=wide },colors=if(wide) ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors())
                    TextButton("自动",{viewPreference=0},Modifier.semantics { selected=viewPreference==0 },colors=if(viewPreference==0) ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors())
                }
                if(selectedClass==0 && week in 3..4) MotionText("本周安排军训（含入学教育），具体时间以学校通知为准。",fontSize=13.sp,modifier=Modifier.padding(top=12.dp))
                if(!wide) Row(Modifier.fillMaxWidth().padding(top=18.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    (1..7).forEach { d -> val selected=d==day
                        Button(onClick={day=d},modifier=Modifier.weight(1f).semantics { contentDescription="星期${days[d-1]}，${dateText(dateFor(week,d))}";this.selected=selected },minWidth=0.dp,insideMargin=PaddingValues(vertical=12.dp),colors=if(selected) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors()) {
                            Column(horizontalAlignment=Alignment.CenterHorizontally) { MotionText(days[d-1],fontSize=12.sp,color=if(selected) colors.onPrimary else colors.onSurface);MotionText("${dateFor(week,d).dayOfMonth}",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=if(selected) colors.onPrimary else colors.onSurface) }
                        }
                    }
                }
            } }
            item {
                val scheduleTransition=updateTransition(Triple(week,day,wide),label="schedule-content")
                var resizeOrder by remember {mutableIntStateOf(0)}
                scheduleTransition.AnimatedContent(modifier=Modifier.fillMaxWidth(),transitionSpec={
                    val forward=targetState.first>initialState.first || (targetState.first==initialState.first && (targetState.second>initialState.second || (!initialState.third && targetState.third)))
                    layeredTransition(forward,fadeContent=true,resizeOrder=resizeOrder,onSizeDirection={resizeOrder=it})
                }) { (shownWeek,shownDay,shownWide) ->
                    val active=shownWeek==week && shownDay==day && shownWide==wide
                    val from=scheduleTransition.currentState;val to=scheduleTransition.targetState
                    val forward=to.first>from.first || (to.first==from.first && (to.second>from.second || (!from.third && to.third)))
                    val contentDepth by transition.animateFloat(transitionSpec={tween(500,delayMillis=if(resizeOrder>0) 180 else 0,easing=FastOutSlowInEasing)},label="schedule-depth") {state ->
                        if((forward && state==EnterExitState.PostExit) || (!forward && state==EnterExitState.PreEnter)) 1f else 0f
                    }
                    Column(Modifier.fillMaxWidth().padding(3.dp).then(depthSurface(contentDepth,deviceCorners=false,castShadow=false)).background(colors.background).then(if(!active) Modifier.clearAndSetSemantics {}.blockMotionInput() else Modifier),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        if(shownWide) WeeklyGrid(shownWeek,dark,::show)
                        else {
                            val courses=ScheduleData.courses(dateFor(shownWeek,shownDay))
                            if(courses.isEmpty()) Card(Modifier.fillMaxWidth(),insideMargin=PaddingValues(28.dp)) {MotionText("这一天没有排课。",color=colors.onSurfaceVariantSummary)}
                            courses.forEach {course ->
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                    Column(Modifier.width(48.dp).padding(top=18.dp),horizontalAlignment=Alignment.CenterHorizontally) {MotionText("${course.first}–${course.last}节",fontSize=12.sp,color=colors.onSurfaceVariantSummary);MotionText(course.startTime(),fontSize=11.sp,color=colors.onSurfaceVariantSummary,modifier=Modifier.padding(top=6.dp))}
                                    Box(Modifier.weight(1f)) {CourseCard(course,dark,false) {show(course)}}
                                }
                            }
                        }
                    }
                }
            }
            item { Column(Modifier.padding(6.dp)) { MotionText("课表说明",fontWeight=FontWeight.SemiBold,fontSize=14.sp);MotionText(if(selectedClass==0) "体育课按所选分组或教师显示，请以个人教务课表为准。\n军训（含入学教育）：第3—4周，未提供具体节次。\n尚未排定时间：机电装备专业群导论、军事理论。\n\n核对日期 2026.10.02 · 最新调整以教务系统为准" else "本数据来自2026—2027第一学期班级课表查询，包含体育等分组选课。同一时段的不同分组不代表全部需要上课，具体以个人课表为准。\n校区作息按已提供的学校时间表配置，3—4节在部分教学区域延后20分钟。\n核对日期 2026.10.02 · 最新调整以教务系统为准",fontSize=12.sp,color=colors.onSurfaceVariantSummary,modifier=Modifier.padding(top=8.dp)) } }
        }
        TopScrollEdge(courseLayer,edgeStrength,dark,Modifier.align(Alignment.TopCenter),title="班级课表",titleReveal=titleReveal(courseScroll))
        }
        }
        }
        }
        }
        Canvas(Modifier.fillMaxSize()) {
            textOverlays.entries.forEach {glyph ->
                if(glyph.moving && glyph.upper) {
                    glyph.layer.renderEffect=glyph.blur
                    glyph.layer.alpha=(1f-glyph.depth).coerceIn(0f,1f)*glyph.baseAlpha
                    val factor=1f-.06f*glyph.depth
                    translate(glyph.position.x,glyph.position.y) {
                        withTransform({scale(factor,factor,pivot=Offset(glyph.layer.size.width/2f,glyph.layer.size.height/2f))}) {drawLayer(glyph.layer)}
                    }
                }
            }
        }
        }
        FloatingNavigation(page,{if(!customMounted && !settingsMounted) page=it},{if(!customMounted && !settingsMounted) {expansionStart=Rect(addBounds.left-rootOrigin.x,addBounds.top-rootOrigin.y,addBounds.right-rootOrigin.x,addBounds.bottom-rootOrigin.y);customShown=true}},backdrop,backdropOrigin,dark,{addBounds=it},!customMounted && !settingsMounted,Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)).only(WindowInsetsSides.Horizontal+WindowInsetsSides.Bottom)).padding(horizontal=20.dp,vertical=16.dp).graphicsLayer {translationY=dockHidden*with(density){160.dp.toPx()};alpha=(1f-expansion.value)*(1f-dockHidden)})
        if(settingsMounted) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=settingsShade)))
        }
        settingsTransition.AnimatedContent(transitionSpec={
            // Keep the full-window viewport fixed; only the foreground page translates on return.
            layeredTransition(targetState>initialState) using null
        },modifier=Modifier.fillMaxSize()) { route ->
            // The empty return destination must have the same measured size as the outgoing page.
            if(route==0) Box(Modifier.fillMaxSize())
            else {
                val active=route==settingsRoute
                val forward=settingsTransition.targetState>settingsTransition.currentState
                val routeDepth by transition.animateFloat(transitionSpec={tween(500,easing=FastOutSlowInEasing)},label="route-depth") {state ->
                    if((forward && state==EnterExitState.PostExit) || (!forward && state==EnterExitState.PreEnter)) 1f else 0f
                }
                Box(Modifier.fillMaxSize().then(depthSurface(routeDepth)).background(colors.background).then(if(!active) Modifier.clearAndSetSemantics {}.blockMotionInput() else Modifier)) {
                    if(route==1) SettingsPage(onClose={closeSettings(false)},onEdit={settingsShown.value=true},onRestored={selectedClass=SchoolData.selected;selectedCampus=SchoolData.campus;configured=SchoolData.configured(context);courseRevision++;BaseWidgetProvider.updateAll(context)},embedded=true,active=active)
                    else {
                        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                            MotionText("更改课表",fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=16.dp))
                            MotionText("保存后更新课表和桌面小部件，取消则保留原设置。",fontSize=14.sp,color=colors.onSurfaceVariantSummary)
                            SetupChoices(selectedClass,selectedCampus,{index,campus,groups -> SchoolData.confirmGroups(context,index,campus,groups);selectedClass=index;selectedCampus=campus;closeSettings(true);BaseWidgetProvider.updateAll(context)},{closeSettings(true)})
                        }
                    }
                }
            }
        }
        if(customMounted) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.22f*expansion.value)))
            val t=expansion.value
            val fullWidth=constraints.maxWidth.toFloat();val fullHeight=constraints.maxHeight.toFloat()
            val pageWidth=maxWidth;val pageHeight=maxHeight
            val start=if(expansionStart==Rect.Zero) with(density) {Rect(fullWidth-84.dp.toPx(),fullHeight-80.dp.toPx(),fullWidth-20.dp.toPx(),fullHeight-16.dp.toPx())} else expansionStart
            val x=start.left*(1f-t);val y=start.top*(1f-t)
            val width=start.width+(fullWidth-start.width)*t;val height=start.height+(fullHeight-start.height)*t
            val shape=devicePageShape(t)
            Box(Modifier.offset {IntOffset(x.roundToInt(),y.roundToInt())}.size(with(density){width.toDp()},with(density){height.toDp()}).shadow(44.dp*(1f-t*.6f),shape,ambientColor=Color(0xFF353D49).copy(alpha=.75f),spotColor=Color(0xFF353D49).copy(alpha=.95f)).clip(shape).background(lerp(Color(0xFF3482FF),colors.background,t))) {
                Box(Modifier.wrapContentSize(Alignment.TopStart,unbounded=true).requiredSize(pageWidth,pageHeight).graphicsLayer {alpha=((t-.5f)/.5f).coerceIn(0f,1f)}.then(if(t<.999f || closing) Modifier.clearAndSetSemantics {}.blockMotionInput() else Modifier)) {
                    CustomCoursePage(onClose={closeCustom()},onChanged={courseRevision++;BaseWidgetProvider.updateAll(context)},embedded=true)
                }
            }
        }
    }
    SuperDialog(show=weekPicker,title="选择教学周",onDismissRequest={weekPicker.value=false}) {
        Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState())) {
            (0..4).forEach { row -> Row(Modifier.fillMaxWidth().padding(bottom=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) { (1..4).forEach { col -> val w=row*4+col;TextButton("$w",{week=w;weekPicker.value=false},Modifier.weight(1f).semantics { contentDescription="第 $w 周" },colors=if(w==week) ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors(),minWidth=0.dp,insideMargin=PaddingValues(12.dp)) } } }
        }
        TextButton("取消",{weekPicker.value=false},Modifier.fillMaxWidth())
    }
    if(detailIndex in ScheduleData.COURSES.indices) {
        val course=ScheduleData.COURSES[detailIndex]
        SuperDialog(show=detailShown,title=course.displayName(),onDismissRequest={detailShown.value=false;detailIndex=-1}) {
            Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                DetailRow("教师",course.teacher);DetailRow("教室",course.room);DetailRow("时间","星期${days[course.day-1]} · ${course.time()}");DetailRow("节次","第 ${course.first}–${course.last} 节");DetailRow("周次",weekRanges(course.weeks))
                if(course.name.contains("大学体育")) MotionText(if(selectedClass==0) "体育课按已选分组或教师显示，请以个人教务课表为准。" else "本班级课表包含体育分组，请按自己的选课安排查看。",fontSize=13.sp)
                TextButton("关闭",{detailShown.value=false;detailIndex=-1},Modifier.fillMaxWidth(),colors=ButtonDefaults.textButtonColorsPrimary())
            }
        }
    }
}
// Haze keeps the source and the foreground separate; radius fades with height.
@Composable private fun TopScrollEdge(state:HazeState,strength:Float,dark:Boolean,modifier:Modifier=Modifier,title:String="",onBack:(()->Unit)?=null,titleReveal:Float=strength) {
    val density=androidx.compose.ui.platform.LocalDensity.current

    val colors=MiuixTheme.colorScheme

    val status=WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val edgeHeight=(status+144.dp)*2f/3f

    val plateau=with(density){(status+12.dp).toPx()}

    val end=with(density){edgeHeight.toPx()}

    if(strength>.001f) Box(modifier.fillMaxWidth().height(edgeHeight)) {

        Box(Modifier.matchParentSize().graphicsLayer {compositingStrategy=CompositingStrategy.Offscreen}.drawWithContent {
            drawContent()
            // Only feather the last 12dp; the rest of the original effect stays intact.
            drawRect(Brush.verticalGradient(listOf(Color.Black,Color.Transparent),startY=(size.height-12.dp.toPx()).coerceAtLeast(0f),endY=size.height),blendMode=BlendMode.DstIn)
        }) {
        Box(Modifier.matchParentSize().hazeEffect(state) {

            alpha=strength

            blurEnabled=Build.VERSION.SDK_INT>=31

            backgroundColor=colors.background

            blurRadius=72.dp

            noiseFactor=0f

            tints=listOf(HazeTint(Color.Transparent))

            fallbackTint=HazeTint(colors.background.copy(alpha=.12f))

            progressive=HazeProgressive.verticalGradient(startY=plateau,endY=end,startIntensity=1f,endIntensity=0f,preferPerformance=false)

        })

        Canvas(Modifier.matchParentSize().graphicsLayer {alpha=strength}) {

            drawRect(Brush.verticalGradient(0f to (if(dark) Color(0xFF090A0C) else Color(0xFF797B80)).copy(alpha=if(dark) .24f else .20f),.42f to (if(dark) Color.Black else Color(0xFFB7B9BE)).copy(alpha=.08f),1f to Color.Transparent))

        }

        }
        // Foreground is drawn after the effect and is never part of its source.
        Row(Modifier.fillMaxWidth().padding(top=status,start=18.dp,end=18.dp).height(56.dp).graphicsLayer {alpha=strength},verticalAlignment=Alignment.CenterVertically) {
            if(onBack!=null) Box(Modifier.size(48.dp).clip(RoundedCornerShape(100.dp)).background(if(dark) Color.White.copy(alpha=.08f) else Color.White.copy(alpha=.6f)).border(1.dp,Color.White.copy(alpha=.4f),RoundedCornerShape(100.dp)).clickable(role=Role.Button,onClick=onBack).semantics {contentDescription="返回"},contentAlignment=Alignment.Center) {MotionText("‹",fontSize=32.sp)}
            else Spacer(Modifier.width(48.dp))
            val pageDepth=LocalTextMotion.current?.depth?.value ?: 0f
            val titleAlpha by animateFloatAsState(titleReveal*((1f-pageDepth-.25f)/.75f).coerceIn(0f,1f),tween(360,easing=LinearOutSlowInEasing),label="compact-title-appearance")
            Box(Modifier.weight(1f).graphicsLayer {alpha=titleAlpha;translationY=(1f-titleAlpha)*with(density){6.dp.toPx()};scaleX=.92f+.08f*titleAlpha;scaleY=scaleX},contentAlignment=Alignment.Center) {MotionText(title,fontSize=20.sp,fontWeight=FontWeight.SemiBold,overlayAlpha=strength*titleAlpha,revealBlur=1f-titleAlpha)}
            Spacer(Modifier.width(48.dp))
        }
    }
}

// Public window APIs expose each corner in pixels, including rotation/window changes.
@Composable private fun devicePageShape(progress:Float=1f):RoundedCornerShape {
    val view=androidx.compose.ui.platform.LocalView.current
    val density=androidx.compose.ui.platform.LocalDensity.current
    var radii by remember(view) {mutableStateOf(List(4){0})}
    DisposableEffect(view) {
        fun update() {
            if(Build.VERSION.SDK_INT>=31) {
                val insets=view.rootWindowInsets
                radii=listOf(android.view.RoundedCorner.POSITION_TOP_LEFT,android.view.RoundedCorner.POSITION_TOP_RIGHT,android.view.RoundedCorner.POSITION_BOTTOM_RIGHT,android.view.RoundedCorner.POSITION_BOTTOM_LEFT).map {insets?.getRoundedCorner(it)?.radius ?: 0}
            }
        }
        val listener=android.view.View.OnLayoutChangeListener {_,_,_,_,_,_,_,_,_ -> update()}
        view.addOnLayoutChangeListener(listener)
        view.post {update()}
        onDispose {view.removeOnLayoutChangeListener(listener)}
    }
    fun corner(index:Int)=with(density) {
        val actual=if(radii[index]>0) radii[index].toDp() else 40.dp
        28.dp+(actual-28.dp)*progress.coerceIn(0f,1f)
    }
    return RoundedCornerShape(topStart=corner(0),topEnd=corner(1),bottomEnd=corner(2),bottomStart=corner(3))
}

// Forward: next page slides over a shrinking, blurred predecessor. Return reverses the stack.
private fun <S> AnimatedContentTransitionScope<S>.layeredTransition(forward:Boolean,fadeContent:Boolean=false,resizeOrder:Int=0,onSizeDirection:(Int)->Unit={}):ContentTransform {
    if(fadeContent) {
        // Actual measured heights determine the order, independent of mode or course count.
        val delay=if(resizeOrder>0) 180 else 0
        val motion=tween<IntOffset>(500,delayMillis=delay,easing=FastOutSlowInEasing)
        val opacity=tween<Float>(500,delayMillis=delay,easing=FastOutSlowInEasing)
        val enter=if(forward) slideInHorizontally(motion) {it}+fadeIn(opacity) else fadeIn(opacity)
        val exit=if(forward) fadeOut(opacity) else slideOutHorizontally(motion) {it}+fadeOut(opacity)
        return (enter togetherWith exit).apply {targetContentZIndex=if(forward) 1f else -1f} using SizeTransform(clip=false,sizeAnimationSpec={before,after ->
            val order=after.height.compareTo(before.height)
            onSizeDirection(order)
            if(order==0) snap() else tween(180,delayMillis=if(order<0) 500 else 0,easing=FastOutSlowInEasing)
        })
    }
    val enter=if(forward) slideInHorizontally(tween(500,easing=FastOutSlowInEasing)) {it}+(if(fadeContent) fadeIn(tween(500,easing=FastOutSlowInEasing)) else EnterTransition.None) else fadeIn(tween(if(fadeContent) 500 else 240),initialAlpha=if(fadeContent) 0f else .75f)
    val exit=if(forward) fadeOut(tween(500),targetAlpha=if(fadeContent) 0f else .75f) else slideOutHorizontally(tween(500,easing=FastOutSlowInEasing)) {it}+(if(fadeContent) fadeOut(tween(500,easing=FastOutSlowInEasing)) else ExitTransition.None)
    return (enter togetherWith exit).apply {targetContentZIndex=if(forward) 1f else -1f} using SizeTransform(clip=false)
}

@Composable private fun depthSurface(depth:Float,deviceCorners:Boolean=true,castShadow:Boolean=true):Modifier {
    val density=androidx.compose.ui.platform.LocalDensity.current
    val pageShape=if(deviceCorners) devicePageShape(1f-depth) else RoundedCornerShape(CardDefaults.CornerRadius)
    val amount=(depth.coerceIn(0f,1f)*20).roundToInt()/20f
    val effect=remember(amount,density) {
        if(Build.VERSION.SDK_INT>=31 && amount>0f) RenderEffect.createBlurEffect(with(density){(14.dp*amount).toPx()},with(density){(14.dp*amount).toPx()},Shader.TileMode.CLAMP).asComposeRenderEffect() else null
    }
    return Modifier.graphicsLayer {
        scaleX=1f-.06f*depth;scaleY=scaleX
        shape=pageShape;clip=true
        shadowElevation=if(castShadow) with(density){(24.dp+20.dp*depth).toPx()} else 0f
        ambientShadowColor=Color(0xFF353D49).copy(alpha=.75f)
        spotShadowColor=Color(0xFF353D49).copy(alpha=.95f)
        renderEffect=effect
    }
}

// Consume in Initial pass, before descendants can activate a hidden control.
private fun Modifier.blockMotionInput()=pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed=false,pass=androidx.compose.ui.input.pointer.PointerEventPass.Initial).consume()
        do {
            val event=awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
            event.changes.forEach {it.consume()}
        } while(event.changes.any {it.pressed})
    }
}
@Composable private fun DetailRow(label: String,value: String) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)) { MotionText(label,Modifier.width(40.dp),fontSize=14.sp,color=MiuixTheme.colorScheme.onSurfaceVariantSummary);MotionText(value,Modifier.weight(1f),fontSize=15.sp) } }
@Composable private fun CourseCard(course: ScheduleData.Course,dark: Boolean,compact: Boolean,onClick:()->Unit) {
    val accent=Color(accents[colorIndex(course.name)])
    val background=androidx.compose.ui.graphics.lerp(if(dark) Color(0xFF202022) else Color.White,accent,if(dark) .17f else .12f)
    Card(Modifier.fillMaxWidth().semantics { role=Role.Button },insideMargin=PaddingValues(18.dp),colors=CardDefaults.defaultColors(color=background,contentColor=if(dark) Color(0xFFF0F0F0) else Color(0xFF202027)),pressFeedbackType=PressFeedbackType.Sink,onClick=onClick) {
        Box(Modifier.width(24.dp).height(3.dp).background(accent))
        MotionText(course.displayName(),fontWeight=FontWeight.SemiBold,fontSize=if(compact) 15.sp else 16.sp,modifier=Modifier.padding(top=10.dp,bottom=8.dp))
        if(!compact) MotionText(course.teacher,fontSize=13.sp,modifier=Modifier.padding(bottom=4.dp))
        MotionText(course.room,fontSize=13.sp,modifier=Modifier.padding(bottom=4.dp))
        MotionText(course.time(),fontSize=13.sp)
    }
}
@Composable private fun WeeklyGrid(week: Int,dark: Boolean,onCourse:(ScheduleData.Course)->Unit) {
    val colors=MiuixTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gridWidth=if(maxWidth>=560.dp) maxWidth else 840.dp
        val columnWidth=(gridWidth-84.dp-32.dp-6.dp*7)/7
        val small=columnWidth<120.dp
        Column(Modifier.horizontalScroll(rememberScrollState()).width(gridWidth).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                MotionText("节次",Modifier.width(84.dp).padding(start=6.dp,top=12.dp,end=6.dp),fontSize=11.sp)
                (1..7).forEach { d -> Column(Modifier.width(columnWidth).padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    MotionText("周${days[d-1]}",fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                    MotionText(dateText(dateFor(week,d)),fontSize=10.sp,color=colors.onSurfaceVariantSummary)
                } }
            }
            (1..12 step 2).forEach { section -> Row(Modifier.height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Column(Modifier.width(84.dp).padding(start=6.dp,top=12.dp,end=6.dp)) {
                    MotionText("$section–${section+1}节",fontSize=11.sp)
                    MotionText(if(section==3) "10:10 / 10:30\n11:35 / 11:55" else ScheduleData.STARTS[section-1]+"\n"+ScheduleData.ENDS[section],fontSize=10.sp,color=colors.onSurfaceVariantSummary)
                }
                (1..7).forEach { d -> Column(Modifier.width(columnWidth).heightIn(min=96.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    val list=ScheduleData.courses(dateFor(week,d)).filter { it.first in section..section+1 }
                    if(list.isEmpty()) Card(Modifier.fillMaxWidth().height(96.dp)) {}
                    list.forEach { c ->
                        val accent=Color(accents[colorIndex(c.name)])
                        val bg=androidx.compose.ui.graphics.lerp(if(dark) Color(0xFF202022) else Color.White,accent,if(dark) .17f else .12f)
                        Card(Modifier.fillMaxWidth().semantics { role=Role.Button;contentDescription="${c.name}，${c.room}，${c.time()}" },insideMargin=PaddingValues(if(small) 10.dp else 14.dp),colors=CardDefaults.defaultColors(color=bg),pressFeedbackType=PressFeedbackType.Sink,onClick={onCourse(c)}) {
                            MotionText(c.displayName(),fontSize=if(small) 11.sp else 14.sp,fontWeight=FontWeight.SemiBold)
                            MotionText(c.room,fontSize=if(small) 10.sp else 12.sp,modifier=Modifier.padding(top=6.dp))
                            if(!small) MotionText(c.teacher,fontSize=12.sp,modifier=Modifier.padding(top=4.dp))
                            MotionText(c.time(),fontSize=if(small) 9.sp else 11.sp,modifier=Modifier.padding(top=5.dp))
                        }
                    }
                } }
            } }
        }
    }
}



@OptIn(ExperimentalLayoutApi::class)
@Composable private fun SetupChoices(initialClass:Int,initialCampus:Int,onConfirm:(Int,Int,String)->Unit,onCancel:(()->Unit)?) {
    var draftClass by rememberSaveable { mutableIntStateOf(initialClass) }
    var draftCampus by rememberSaveable { mutableIntStateOf(initialCampus) }
    var search by rememberSaveable { mutableStateOf("") }
    val context=LocalContext.current
    val groupOptions=remember(draftClass) { if(draftClass>=0) SchoolData.groups(draftClass) else emptyMap<String,List<String>>() }
    var draftGroups by rememberSaveable(draftClass) { mutableStateOf(if(draftClass>=0) SchoolData.groupChoices(context,draftClass) else "{}") }
    val colors=MiuixTheme.colorScheme
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        MotionText("1 · 选择班级",fontWeight=FontWeight.SemiBold,fontSize=16.sp)
        MotionText("搜索年级、专业或班级名称",fontSize=12.sp,color=colors.onSurfaceVariantSummary)
        TextField(value=search,onValueChange={search=it},modifier=Modifier.fillMaxWidth())
        LazyColumn(Modifier.fillMaxWidth().height(220.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            val matches=SchoolData.NAMES.withIndex().filter { it.value.contains(search.trim(),ignoreCase=true) }
            if(matches.isEmpty()) item { MotionText("没有找到班级，试试专业名称或年级。",modifier=Modifier.padding(12.dp)) }
            matches.forEach { entry -> item(key=entry.index) {
                val chosen=entry.index==draftClass
                Card(Modifier.fillMaxWidth().semantics { role=Role.RadioButton;selected=chosen },insideMargin=PaddingValues(14.dp),colors=CardDefaults.defaultColors(color=if(chosen) colors.secondaryContainer else colors.surface),onClick={draftClass=entry.index}) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        MotionText(if(chosen) "●" else "○",fontSize=18.sp,color=if(chosen) colors.primary else colors.onSurfaceVariantSummary)
                        MotionText(SchoolData.displayName(entry.index),Modifier.weight(1f),fontSize=14.sp,fontWeight=if(chosen) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            } }
        }
        MotionText(if(draftClass>=0) "已选班级：${SchoolData.displayName(draftClass)}" else "请选择班级",fontSize=13.sp,color=colors.onSurfaceVariantSummary)
        MotionText("2 · 选择所在校区",fontWeight=FontWeight.SemiBold,fontSize=16.sp,modifier=Modifier.padding(top=8.dp))
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            (0..1).forEach { row -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                (0..1).forEach { col -> val index=row*2+col;val chosen=index==draftCampus
                    Button(onClick={draftCampus=index},modifier=Modifier.weight(1f).semantics { role=Role.RadioButton;selected=chosen },minWidth=0.dp,insideMargin=PaddingValues(vertical=12.dp,horizontal=8.dp),colors=if(chosen) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors()) {
                        MotionText((if(chosen) "✓ " else "")+SchoolData.CAMPUSES[index],fontSize=14.sp,color=if(chosen) colors.onPrimary else colors.onSurface)
                    }
                }
            } }
        }
        MotionText("作息按校区和教学区域计算，分组选课以个人教务课表为准。",fontSize=12.sp,color=colors.onSurfaceVariantSummary)
        if(groupOptions.isNotEmpty()) {
            MotionText("3 · 分组选课",fontWeight=FontWeight.SemiBold,fontSize=16.sp)
            MotionText("未标注的项目按其他班级中同一教师的明确项目补充，注明来源；组号只采用本班已标注的记录。",fontSize=12.sp,color=colors.onSurfaceVariantSummary)
            groupOptions.forEach { (category,options) ->
                MotionText(category,fontWeight=FontWeight.SemiBold,fontSize=14.sp)
                val chosen=JSONObject(draftGroups).optString(category,"")
                (listOf("*" to "保留全部（待确认）","none" to "不显示此类课程")+options.map { it to SchoolData.groupLabel(it) }).forEach { (value,label) ->
                    Card(Modifier.fillMaxWidth().semantics { role=Role.RadioButton;selected=chosen==value },insideMargin=PaddingValues(12.dp),colors=CardDefaults.defaultColors(color=if(chosen==value) colors.secondaryContainer else colors.surface),onClick={draftGroups=JSONObject(draftGroups).put(category,value).toString()}) { MotionText((if(chosen==value) "● " else "○ ")+label,fontSize=14.sp) }
                }
            }
        }
        Button(onClick={onConfirm(draftClass,draftCampus,draftGroups)},enabled=draftClass>=0&&draftCampus>=0&&groupOptions.keys.all { JSONObject(draftGroups).has(it) },modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColorsPrimary()) { MotionText(if(onCancel==null) "保存并查看课表" else "保存更改",color=colors.onPrimary) }
        if(onCancel!=null) TextButton("取消",onCancel,Modifier.fillMaxWidth())
    }
}


// Capture only page content: controls stay sharp and never feed back into their backdrop.
@Composable private fun FrostedSurface(backdrop:GraphicsLayer,origin:Offset,dark:Boolean,modifier:Modifier=Modifier,content:@Composable BoxScope.()->Unit) {
    val shape=RoundedCornerShape(100.dp)
    var position by remember { mutableStateOf(Offset.Zero) }
    val density=androidx.compose.ui.platform.LocalDensity.current
    val effect=remember(density) { if(Build.VERSION.SDK_INT>=31) RenderEffect.createBlurEffect(with(density){22.dp.toPx()},with(density){22.dp.toPx()},Shader.TileMode.CLAMP).asComposeRenderEffect() else null }
    Box(modifier.shadow(24.dp,shape,ambientColor=Color(0xFF727985).copy(alpha=.14f),spotColor=Color(0xFF727985).copy(alpha=.22f)).onGloballyPositioned {position=it.positionInRoot()}.clip(shape)) {
        if(effect!=null) Canvas(Modifier.matchParentSize().graphicsLayer {renderEffect=effect}) {
            translate(origin.x-position.x,origin.y-position.y) {drawLayer(backdrop)}
        }
        Box(Modifier.matchParentSize().background(if(dark) Color(0xFF23252B).copy(alpha=if(effect==null) .82f else .42f) else Color.White.copy(alpha=if(effect==null) .82f else .32f)).border(1.dp,Brush.linearGradient(listOf(Color.White.copy(alpha=if(dark) .38f else .85f),Color.White.copy(alpha=.12f),Color.White.copy(alpha=if(dark) .25f else .5f))),shape))
        content()
    }
}

@Composable private fun FloatingNavigation(page:Int,onPage:(Int)->Unit,onAdd:()->Unit,backdrop:GraphicsLayer,origin:Offset,dark:Boolean,onAddBounds:(Rect)->Unit,enabled:Boolean,modifier:Modifier=Modifier) {
    val colors=MiuixTheme.colorScheme
    var dragging by remember {mutableStateOf(false)}
    var touching by remember {mutableStateOf(false)}
    var dragFraction by remember {mutableFloatStateOf(page.toFloat())}
    val settled by animateFloatAsState(if(dragging) dragFraction else page.toFloat(),if(dragging) snap() else spring(dampingRatio=.92f,stiffness=300f),label="selected-capsule")
    val selection=if(dragging) dragFraction else settled
    val capsuleScale by animateFloatAsState(if(touching) .90f else 1f,tween(120),label="drag-scale")
    val haptic=LocalHapticFeedback.current
    val currentPage by rememberUpdatedState(page)
    val currentOnPage by rememberUpdatedState(onPage)
    val currentEnabled by rememberUpdatedState(enabled)
    LaunchedEffect(enabled) {if(!enabled) {dragging=false;touching=false}}
    Row(modifier.then(if(!enabled) Modifier.clearAndSetSemantics {} else Modifier).widthIn(max=256.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
        FrostedSurface(backdrop,origin,dark,Modifier.weight(1f).height(56.dp)) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(4.dp)) {
                val capsuleWidth=maxWidth/2
                val cellWidth=with(androidx.compose.ui.platform.LocalDensity.current) {capsuleWidth.toPx()}


                // One recognizer handles tap, immediate drag and hold; no competing click handler.
                val dragInput=Modifier.pointerInput(cellWidth,enabled) {
                    awaitEachGesture {
                        val pass=androidx.compose.ui.input.pointer.PointerEventPass.Initial
                        val down=awaitFirstDown(requireUnconsumed=false,pass=pass)
                        down.consume()
                        if(currentEnabled) {
                            touching=true
                            var point=down.position
                            var released=false
                            var cancelled=false
                            var moved=false
                            try {
                                val early=withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                    while(true) {
                                        val event=awaitPointerEvent(pass)
                                        val change=event.changes.firstOrNull {it.id==down.id}
                                        if(change==null || event.changes.count {it.pressed}>1) {cancelled=true;break}
                                        point=change.position;change.consume()
                                        if(!change.pressed) {released=true;break}
                                        if((point-down.position).getDistance()>viewConfiguration.touchSlop) {moved=true;break}
                                    }
                                    true
                                }
                                if(!cancelled && !released && (moved || early==null)) {
                                    val startPage=currentPage.toFloat()
                                    dragging=true
                                    if(early==null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dragFraction=(startPage+(point.x-down.position.x)/cellWidth).coerceIn(0f,1f)
                                    while(true) {
                                        val event=awaitPointerEvent(pass)
                                        val change=event.changes.firstOrNull {it.id==down.id}
                                        if(change==null || event.changes.count {it.pressed}>1) {cancelled=true;break}
                                        point=change.position;change.consume()
                                        dragFraction=(startPage+(point.x-down.position.x)/cellWidth).coerceIn(0f,1f)
                                        if(!change.pressed) {released=true;break}
                                    }
                                }
                                if(released && !cancelled && currentEnabled && point.x>=-cellWidth*.5f && point.x<=cellWidth*2.5f && point.y>=-size.height*.5f && point.y<=size.height*1.5f) currentOnPage(if(point.x>=cellWidth) 1 else 0)
                            } finally {dragging=false;touching=false}
                        }
                    }
                }
                Box(Modifier.offset {IntOffset((cellWidth*selection).roundToInt(),0)}.width(capsuleWidth).fillMaxHeight().graphicsLayer {scaleX=capsuleScale;scaleY=capsuleScale}.shadow(5.dp,RoundedCornerShape(100.dp),ambientColor=Color(0xFFB8BBC2).copy(alpha=.10f),spotColor=Color(0xFFB8BBC2).copy(alpha=.16f)).background(if(dark) Color(0xFF63666E).copy(alpha=.34f) else Color(0xFFD2D5DB).copy(alpha=.38f),RoundedCornerShape(100.dp)))
            Row(Modifier.fillMaxSize().then(dragInput)) {
                (0..1).forEach { index ->
                    val ink by animateColorAsState(if(page==index) colors.onSurface else colors.onSurfaceVariantSummary,tween(320),label="tab-ink")
                    Column(Modifier.weight(1f).fillMaxHeight().semantics {contentDescription=if(index==0) "课表" else "关于";selected=page==index;role=Role.Tab;if(enabled) onClick {onPage(index);true} else disabled()},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                Canvas(Modifier.size(24.dp)) {
                    val stroke=Stroke(width=2.4.dp.toPx())
                    if(index==0) {
                        drawRoundRect(ink,Offset(size.width*.08f,size.height*.1f),Size(size.width*.84f,size.height*.8f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),style=stroke)
                        drawLine(ink,Offset(size.width*.43f,size.height*.1f),Offset(size.width*.43f,size.height*.9f),stroke.width)
                        (0..2).forEach { drawLine(ink,Offset(size.width*.19f,size.height*(.28f+it*.18f)),Offset(size.width*.32f,size.height*(.28f+it*.18f)),stroke.width) }
                    } else {
                        drawCircle(ink,radius=size.width*.43f,style=stroke)
                        drawCircle(ink,radius=1.6.dp.toPx(),center=Offset(size.width*.5f,size.height*.3f))
                        drawLine(ink,Offset(size.width*.5f,size.height*.44f),Offset(size.width*.5f,size.height*.72f),stroke.width)
                    }
                }
                        Spacer(Modifier.height(3.dp))
                        MotionText(if(index==0) "课表" else "关于",fontSize=11.sp,color=ink,fontWeight=if(page==index) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
        }
        FrostedSurface(backdrop,origin,dark,Modifier.size(56.dp).onGloballyPositioned {onAddBounds(it.boundsInRoot())}) {
            Box(Modifier.fillMaxSize().background(Color(0xFF3482FF).copy(alpha=.86f)).clickable(interactionSource=remember {MutableInteractionSource()},indication=null,role=Role.Button,enabled=enabled,onClick=onAdd).semantics {contentDescription="自助添加课程"},contentAlignment=Alignment.Center) {
                Canvas(Modifier.size(26.dp)) {val w=2.5.dp.toPx();drawLine(Color.White,Offset(center.x,3.dp.toPx()),Offset(center.x,size.height-3.dp.toPx()),w);drawLine(Color.White,Offset(3.dp.toPx(),center.y),Offset(size.width-3.dp.toPx(),center.y),w)}
            }
        }
    }
}

@Composable private fun AboutPage(onSettings:()->Unit,scroll:androidx.compose.foundation.lazy.LazyListState) {
    val colors=MiuixTheme.colorScheme
    val context=LocalContext.current
    val screenSafe=WindowInsets.safeDrawing.asPaddingValues()
    fun open(url:String) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) } }
    LazyColumn(Modifier.fillMaxSize(),state=scroll,contentPadding=PaddingValues(start=18.dp+screenSafe.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),end=18.dp+screenSafe.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),top=18.dp+screenSafe.calculateTopPadding(),bottom=140.dp+screenSafe.calculateBottomPadding()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item { Box(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal=6.dp,vertical=12.dp)) {
                Spacer(Modifier.height(20.dp))
                MotionText("关于",headerDepth=titleCollapse(scroll),fontSize=36.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=10.dp))
            }
            Box(Modifier.align(Alignment.TopEnd).padding(top=12.dp).size(48.dp).clickable(role=Role.Button,onClick=onSettings).semantics {contentDescription="设置"},contentAlignment=Alignment.Center) {
                    Canvas(Modifier.size(26.dp)) { val path=Path();for(i in 0..5){val a=Math.PI/3*i-Math.PI/2;val x=center.x+(size.width*.43f*kotlin.math.cos(a)).toFloat();val y=center.y+(size.height*.43f*kotlin.math.sin(a)).toFloat();if(i==0)path.moveTo(x,y) else path.lineTo(x,y)};path.close();drawPath(path,colors.onSurface,style=Stroke(2.3.dp.toPx()));drawCircle(colors.onSurface,radius=size.width*.16f,style=Stroke(2.3.dp.toPx())) }
            }
        } }
        item { Column(Modifier.fillMaxWidth().padding(vertical=20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.ic_launcher),contentDescription="应用图标",modifier=Modifier.size(96.dp))
            MotionText("班级课表",fontSize=32.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=22.dp))
            MotionText("每一周，都有清晰的安排",fontSize=15.sp,color=colors.onSurfaceVariantSummary,modifier=Modifier.padding(top=10.dp))
            MotionText("版本 3.2.0 · Compose Miuix",fontSize=14.sp,color=colors.onSurfaceVariantSummary,modifier=Modifier.padding(top=18.dp))
        } }
        item { MotionText("项目",fontSize=14.sp,color=colors.onSurfaceVariantSummary);Spacer(Modifier.height(10.dp));Card(insideMargin=PaddingValues(20.dp)) {
            MotionText("广交班级课表",fontSize=20.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(14.dp));MotionText("全校 547 个班级\n2026—2027 · 第一学期",fontSize=14.sp,lineHeight=23.sp);Spacer(Modifier.height(22.dp))
            TextButton("GitHub 项目 ↗",{open("https://github.com/baihuanxi552-design/qingyuan-class-timetable")},Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp));Box(Modifier.fillMaxWidth().height(1.dp).background(colors.onSurface.copy(alpha=.08f)));Spacer(Modifier.height(16.dp))
            TextButton("网页版课表 ↗",{open("https://baihuanxi552-design.github.io/qingyuan-class-timetable/")},Modifier.fillMaxWidth())
        } }
        item { MotionText("适配与数据",fontSize=14.sp,color=colors.onSurfaceVariantSummary);Spacer(Modifier.height(10.dp));Card(insideMargin=PaddingValues(20.dp)) {
            MotionText("安卓桌面小部件",fontSize=18.sp,fontWeight=FontWeight.SemiBold)
            MotionText("提供小米、vivo、OPPO、华为和荣耀风格。通过系统安卓小部件入口添加；具体显示与权限由手机桌面决定。",fontSize=14.sp,modifier=Modifier.padding(top=10.dp))
            MotionText("离线内置课表，班级与校区保存在本机。核对日期：2026.10.02。最新调整以教务系统为准，分组选课以个人课表为准。",fontSize=13.sp,color=colors.onSurfaceVariantSummary,modifier=Modifier.padding(top=18.dp))
        } }
    }
}

@Composable private fun CustomCoursePage(onClose:()->Unit,onChanged:()->Unit,embedded:Boolean=false) {
    val context=LocalContext.current;val colors=MiuixTheme.colorScheme
    var name by rememberSaveable { mutableStateOf("") };var teacher by rememberSaveable { mutableStateOf("") };var room by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("1") };var first by rememberSaveable { mutableStateOf("1") };var last by rememberSaveable { mutableStateOf("2") };var weeks by rememberSaveable { mutableStateOf("1-20") }
    var notice by remember { mutableStateOf("") };var revision by remember { mutableIntStateOf(0) }
    BackHandler {onClose()}
    Column(Modifier.fillMaxSize().then(if(embedded) Modifier else Modifier.background(colors.background)).imePadding().verticalScroll(rememberScrollState()).safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        MotionText("自助添加课程",fontSize=30.sp,fontWeight=FontWeight.Bold)
        MotionText("仅保存到本机当前班级，不修改学校课表。保存后显示在课表及小部件中。",fontSize=13.sp,color=colors.onSurfaceVariantSummary)
        MotionText("课程名称（必填）");TextField(name,{name=it},Modifier.fillMaxWidth())
        MotionText("教师（选填）");TextField(teacher,{teacher=it},Modifier.fillMaxWidth())
        MotionText("教室（选填）");TextField(room,{room=it},Modifier.fillMaxWidth())
        MotionText("星期（1为周一，7为周日）");TextField(day,{day=it},Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {MotionText("开始节次（1–12）");TextField(first,{first=it},Modifier.fillMaxWidth())}
            Column(Modifier.weight(1f)) {MotionText("结束节次（1–12）");TextField(last,{last=it},Modifier.fillMaxWidth())}
        }
        MotionText("周次（如 1-16 或 1,3,5-8）");TextField(weeks,{weeks=it},Modifier.fillMaxWidth())
        if(notice.isNotEmpty()) MotionText(notice,fontSize=13.sp,color=colors.primary,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
        Button(onClick={
            try {
                val d=day.toIntOrNull()?:throw IllegalArgumentException("请填写有效星期");val f=first.toIntOrNull()?:throw IllegalArgumentException("请填写开始节次");val l=last.toIntOrNull()?:throw IllegalArgumentException("请填写结束节次")
                CourseInput.validate(name,d,f,l);val ws=CourseInput.weeks(weeks)
                val conflict=ScheduleData.COURSES.any { it.day==d && it.first<=l && it.last>=f && it.weeks.any { w->ws.contains(w) } }
                SchoolData.addCustom(context,name,teacher,room,d,f,l,weeks);revision++;onChanged();name="";notice=if(conflict) "已添加；与已有课程时间重叠，请核对。" else "已添加，课表和小部件已更新。"
            } catch(e:IllegalArgumentException) {notice=e.message?:"请检查输入"}
        },modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColorsPrimary()) {MotionText("保存课程",color=colors.onPrimary)}
        TextButton("返回课表",onClose,Modifier.fillMaxWidth())
        MotionText("已添加的课程",fontSize=18.sp,fontWeight=FontWeight.SemiBold)
        val saved=remember(revision) { SchoolData.custom(context) }
        if(saved.length()==0) MotionText("还没有自建课程。",fontSize=14.sp,color=colors.onSurfaceVariantSummary)
        for(i in 0 until saved.length()) {val row=saved.getJSONObject(i)
            Card(insideMargin=PaddingValues(16.dp)) {MotionText(row.getString("name"),fontWeight=FontWeight.SemiBold);MotionText("星期${row.getInt("day")} · ${row.getInt("first")}–${row.getInt("last")}节 · ${row.getString("room")}",fontSize=13.sp);MotionText("周次 ${row.getJSONArray("weeks")}",fontSize=12.sp);TextButton("删除此课程",{SchoolData.deleteCustom(context,i);revision++;onChanged();notice="课程已删除"},Modifier.fillMaxWidth())}
        }
    }
}

@Composable private fun SettingsPage(onClose:()->Unit,onEdit:()->Unit,onRestored:()->Unit,embedded:Boolean=false,active:Boolean=true) {
    val context=LocalContext.current;val colors=MiuixTheme.colorScheme
    var notice by rememberSaveable { mutableStateOf("") }
    var pending by remember { mutableStateOf<String?>(null) }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if(uri!=null) try {val text=BackupData.exportData(context);context.contentResolver.openOutputStream(uri)?.use {it.write(text.toByteArray(Charsets.UTF_8))}?:error("无法写入文件");notice="备份已保存"}catch(e:Exception){notice="保存失败：${e.message}"}
    }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) try {val bytes=context.contentResolver.openInputStream(uri)?.use {it.readBytesLimited()}?:error("无法读取文件");val text=bytes.toString(Charsets.UTF_8);BackupData.validate(text);pending=text;notice="备份有效，确认后恢复并替换当前本地设置和自建课程。"}catch(e:Exception){notice="导入失败：${e.message}"}
    }
    val jwImporter=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {onRestored()}
    BackHandler(enabled=active) {onClose()}
    val scroll=rememberScrollState()
    val haze=remember {HazeState()}
    val edge by animateFloatAsState((scroll.value/with(androidx.compose.ui.platform.LocalDensity.current){24.dp.toPx()}).coerceIn(0f,1f),tween(210),label="settings-edge")
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(colors.background).hazeSource(haze).imePadding().verticalScroll(scroll).safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        MotionText("设置",fontSize=32.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=16.dp))
        MotionText("当前课表",fontSize=14.sp,color=colors.onSurfaceVariantSummary)
        Card(insideMargin=PaddingValues(20.dp),onClick=onEdit,modifier=Modifier.semantics { role=Role.Button;contentDescription="更改班级、校区和分组" }) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Column(Modifier.weight(1f)) {MotionText("更改课表",fontSize=19.sp,fontWeight=FontWeight.SemiBold);MotionText(SchoolData.title()+" · "+SchoolData.CAMPUSES[SchoolData.campus],fontSize=13.sp,color=colors.onSurfaceVariantSummary,modifier=Modifier.padding(top=8.dp))};MotionText("›",fontSize=28.sp,color=colors.onSurfaceVariantSummary)}
        }
        MotionText(if(PersonalImport.active(context)) "正在使用个人教务课表" else "教务系统",fontSize=14.sp,color=colors.onSurfaceVariantSummary)
        Button(onClick={jwImporter.launch(Intent(context,JwImportActivity::class.java))},modifier=Modifier.fillMaxWidth()) {MotionText("登录教务系统并解析个人课表")}
        if(PersonalImport.active(context)) TextButton("切回班级课表",{PersonalImport.useClass(context);onRestored()},Modifier.fillMaxWidth())
        MotionText("备份",fontSize=14.sp,color=colors.onSurfaceVariantSummary,modifier=Modifier.padding(top=16.dp))
        Card(insideMargin=PaddingValues(0.dp)) {
            BackupRow("保存到本地",false) {export.launch("广交课表备份-${LocalDate.now(ScheduleData.ZONE)}.json")}
            Box(Modifier.fillMaxWidth().padding(horizontal=20.dp).height(1.dp).background(colors.onSurface.copy(alpha=.06f)))
            BackupRow("从本地导入",true) {importer.launch(arrayOf("application/json","text/plain","application/octet-stream"))}
        }
        MotionText("备份包含班级、校区、各班分组选择和自建课程。通过系统文件选择器保存或读取，无需存储权限。",fontSize=13.sp,color=colors.onSurfaceVariantSummary)
        if(notice.isNotEmpty()) MotionText(notice,fontSize=14.sp,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})
        if(pending!=null) {Button(onClick={try {BackupData.restore(context,pending!!);pending=null;onRestored();notice="恢复成功，课表和小部件已更新"}catch(e:Exception){notice="恢复失败：${e.message}"}},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColorsPrimary()) {MotionText("确认恢复并替换",color=colors.onPrimary)};TextButton("取消导入",{pending=null;notice="已取消，原数据保持不变"},Modifier.fillMaxWidth())}
        TextButton("返回关于",onClose,Modifier.fillMaxWidth())
    }
    TopScrollEdge(haze,edge,isSystemInDarkTheme(),title="设置",onBack=onClose)
    }
}
private fun java.io.InputStream.readBytesLimited():ByteArray {val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val count=read(buffer);if(count<0)break;if(out.size()+count>4*1024*1024)throw IllegalArgumentException("备份文件不能超过4MB");out.write(buffer,0,count)};return out.toByteArray()}
@Composable private fun BackupRow(label:String,importing:Boolean,onClick:()->Unit) {
    val colors=MiuixTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable(role=Role.Button,onClick=onClick).padding(horizontal=24.dp,vertical=28.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
        Canvas(Modifier.size(24.dp)) {val ink=colors.onSurface;val w=2.2.dp.toPx();if(!importing){drawLine(ink,Offset(size.width*.5f,0f),Offset(size.width*.5f,size.height*.7f),w);drawLine(ink,Offset(size.width*.25f,size.height*.45f),Offset(size.width*.5f,size.height*.7f),w);drawLine(ink,Offset(size.width*.75f,size.height*.45f),Offset(size.width*.5f,size.height*.7f),w);val p=Path();p.moveTo(size.width*.1f,size.height*.25f);p.lineTo(size.width*.1f,size.height*.95f);p.lineTo(size.width*.9f,size.height*.95f);p.lineTo(size.width*.9f,size.height*.25f);drawPath(p,ink,style=Stroke(w))}else{val p=Path();p.moveTo(size.width*.15f,size.height*.35f);p.lineTo(size.width*.6f,size.height*.35f);p.cubicTo(size.width*1.05f,size.height*.35f,size.width*1.05f,size.height*.95f,size.width*.6f,size.height*.95f);p.lineTo(size.width*.2f,size.height*.95f);drawPath(p,ink,style=Stroke(w));drawLine(ink,Offset(size.width*.15f,size.height*.35f),Offset(size.width*.4f,size.height*.1f),w);drawLine(ink,Offset(size.width*.15f,size.height*.35f),Offset(size.width*.4f,size.height*.6f),w)}}
        MotionText(label,Modifier.weight(1f),fontSize=20.sp);MotionText("›",fontSize=28.sp,color=colors.onSurfaceVariantSummary)
    }
}

private class TextOverlayRegistry {val entries=mutableStateListOf<TextOverlay>()}
private class TextOverlay(val layer:GraphicsLayer) {
    var position by mutableStateOf(Offset.Zero)
    var upper by mutableStateOf(false)
    var moving by mutableStateOf(false)
    var depth by mutableFloatStateOf(0f)
    var blur by mutableStateOf<androidx.compose.ui.graphics.RenderEffect?>(null)
    var anchored=false
    var baseAlpha by mutableFloatStateOf(1f)
}
private class PageCoordinates { var value:androidx.compose.ui.layout.LayoutCoordinates?=null }
private class TextBlurCache(private val maximumRadius:Float) {
    private val effects=arrayOfNulls<androidx.compose.ui.graphics.RenderEffect>(33)
    fun effect(depth:Float):androidx.compose.ui.graphics.RenderEffect? {
        val step=(depth.coerceIn(0f,1f)*32).roundToInt()
        if(Build.VERSION.SDK_INT<31 || step==0)return null
        return effects[step] ?: RenderEffect.createBlurEffect(maximumRadius*step/32f,maximumRadius*step/32f,Shader.TileMode.DECAL).asComposeRenderEffect().also {effects[step]=it}
    }
}
private data class TextMotion(val depth:State<Float>,val halfway:Float,val moving:Boolean,val coordinates:PageCoordinates,val overlays:TextOverlayRegistry,val blurCache:TextBlurCache)
private val LocalTextMotion=compositionLocalOf<TextMotion?> {null}

@Composable private fun MotionText(text:String,modifier:Modifier=Modifier,color:Color=Color.Unspecified,fontSize:androidx.compose.ui.unit.TextUnit=androidx.compose.ui.unit.TextUnit.Unspecified,fontWeight:FontWeight?=null,lineHeight:androidx.compose.ui.unit.TextUnit=androidx.compose.ui.unit.TextUnit.Unspecified,headerDepth:Float=0f,overlayAlpha:Float=1f,revealBlur:Float=0f) {
    val motion=LocalTextMotion.current
    val density=androidx.compose.ui.platform.LocalDensity.current
    var upper by remember {mutableStateOf(false)}
    val motionDepth=if(upper) motion?.depth?.value ?: 0f else 0f
    val depth=maxOf(headerDepth,motionDepth)
    // Scrolling text is blurred by the same Haze region as the backdrop.
    // A separate whole-glyph blur would spill below that region's feathered edge.
    // Retain leaf-only blur for page switching, independently of scroll collapse.
    val blurDepth=maxOf(motionDepth,revealBlur).coerceIn(0f,1f)
    val localBlurCache=remember(density) {TextBlurCache(with(density){8.dp.toPx()})}
    val blur=(motion?.blurCache ?: localBlurCache).effect(blurDepth)
    val recorded=rememberGraphicsLayer()
    val glyph=remember(recorded) {TextOverlay(recorded)}
    val registry=motion?.overlays
    DisposableEffect(registry,glyph) {
        registry?.entries?.add(glyph)
        onDispose {registry?.entries?.remove(glyph)}
    }
    SideEffect {
        glyph.upper=upper;glyph.moving=motion?.moving==true;glyph.depth=motionDepth;glyph.blur=blur;glyph.baseAlpha=overlayAlpha*(1f-.85f*headerDepth)
    }
    val measured=if(motion==null && headerDepth==0f) modifier else modifier.onGloballyPositioned {coords ->
        val location=coords.positionInRoot()
        if(motion?.moving!=true || !glyph.anchored) {
            upper=motion!=null && location.y+coords.size.height*.5f<motion.halfway && location.y+coords.size.height>0f
            val liveShift=motion?.coordinates?.value?.takeIf {it.isAttached}?.positionInRoot()?.x ?: 0f
            glyph.position=Offset(location.x-liveShift,location.y)
            glyph.anchored=true
        }
    }
    val leaf=if(motion?.moving==true && upper) measured.drawWithContent {
        recorded.record {this@drawWithContent.drawContent()}
        // The root overlay draws this text at its stationary screen position.
    } else measured.graphicsLayer {scaleX=1f-.06f*depth;scaleY=scaleX;renderEffect=blur;clip=false;alpha=1f-.85f*headerDepth}

    top.yukonga.miuix.kmp.basic.Text(text,modifier=leaf,color=color,fontSize=fontSize,fontWeight=fontWeight,lineHeight=lineHeight)
}

@Composable private fun titleCollapse(scroll:androidx.compose.foundation.lazy.LazyListState):Float {
    val density=androidx.compose.ui.platform.LocalDensity.current
    val target=if(scroll.firstVisibleItemIndex>0) 1f else (scroll.firstVisibleItemScrollOffset/with(density){56.dp.toPx()}).coerceIn(0f,1f)
    val value by animateFloatAsState(target,tween(160),label="large-title-collapse")
    return value
}
@Composable private fun titleReveal(scroll:androidx.compose.foundation.lazy.LazyListState):Float {
    val density=androidx.compose.ui.platform.LocalDensity.current
    val target=if(scroll.firstVisibleItemIndex>0) 1f else (scroll.firstVisibleItemScrollOffset/with(density){40.dp.toPx()}).coerceIn(0f,1f)
    val value by animateFloatAsState(target,tween(160),label="earlier-title-reveal")
    return value
}

// Observe the Main pass: a child horizontal timetable scroller keeps its gesture.
@Composable private fun pageSwipe(enabled:Boolean,onSwipe:(Int)->Unit):Modifier {
    val latest by rememberUpdatedState(onSwipe)
    val threshold=with(androidx.compose.ui.platform.LocalDensity.current){56.dp.toPx()}
    return Modifier.pointerInput(enabled,threshold) {
        if(!enabled) return@pointerInput
        awaitEachGesture {
            val down=awaitFirstDown(requireUnconsumed=false)
            var total=Offset.Zero
            var horizontal=false
            while(true) {
                val event=awaitPointerEvent()
                val change=event.changes.firstOrNull {it.id==down.id} ?: break
                if(event.changes.count {it.pressed}>1 || change.isConsumed) break
                total=change.position-down.position
                if(!horizontal && total.getDistance()>viewConfiguration.touchSlop) {
                    if(kotlin.math.abs(total.x)<=kotlin.math.abs(total.y)*1.35f) break
                    horizontal=true
                }
                if(horizontal) change.consume()
                if(!change.pressed) {
                    if(horizontal && kotlin.math.abs(total.x)>=threshold) latest(if(total.x<0f) 1 else -1)
                    break
                }
            }
        }
    }
}
