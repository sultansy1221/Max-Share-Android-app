package com.portablefileserver.server

/**
 * صفحة الويب التي تظهر للأجهزة المتصلة.
 * ملاحظة: لا تستخدم رمز الدولار داخل النص لأن Kotlin يفسّره كمتغير.
 */
object WebUi {
    val HTML = """<!doctype html>
<html lang="ar" dir="rtl"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="color-scheme" content="light dark">
<meta name="theme-color" content="#f4f7f7">
<title>Max Share — مشاركة الملفات</title>
<style>
:root{color-scheme:light dark;--bg:#f4f7f7;--card:#fff;--card2:#edf3f2;--tx:#15272b;--sub:#607276;--pr:#137b77;--prSoft:#e5f3f1;--bd:#dce6e4;--er:#b83d49;--ok:#187b5b;--hv:#f0f6f5;--warn:#a36613;--hero:#e9f3f1}
@media(prefers-color-scheme:dark){:root{--bg:#10191b;--card:#182426;--card2:#202e30;--tx:#eff7f6;--sub:#a5b6b5;--pr:#74c9bd;--prSoft:#233e3b;--bd:#2b3b3d;--er:#ff929a;--ok:#73d0a5;--hv:#202e30;--warn:#e2b45c;--hero:#1c3030}}
*{box-sizing:border-box}
body{margin:0;min-height:100vh;font-family:"Noto Sans Arabic","Noto Sans",system-ui,-apple-system,"Segoe UI",sans-serif;background:var(--bg);color:var(--tx)}
header{background:var(--card);color:var(--tx);padding:13px max(18px,calc((100% - 1040px)/2));display:flex;align-items:center;justify-content:space-between;gap:14px;position:sticky;top:0;z-index:5;border-bottom:1px solid var(--bd)}
.brand{display:flex;align-items:center;gap:12px;min-width:0}
.brand img{width:44px;height:44px;object-fit:cover;border-radius:13px;background:var(--prSoft)}
.brand b{font-size:17px;display:block}
.brand small{font-size:12px;color:var(--sub);display:block;margin-top:2px}
#free{font-size:12px;color:var(--sub);background:var(--card2);border:1px solid var(--bd);padding:8px 12px;border-radius:12px;white-space:nowrap}
main{max-width:1040px;margin:0 auto;padding:22px 18px 50px}
.hero{border:1px solid var(--bd);border-radius:22px;padding:24px 26px;margin-bottom:18px;background:var(--hero)}
.eyebrow{font-size:11px;font-weight:750;letter-spacing:.1em;color:var(--pr)}
.hero h1{font-size:clamp(22px,4vw,32px);line-height:1.25;margin:8px 0}
.hero p{color:var(--sub);margin:0;max-width:680px;line-height:1.8;font-size:14px}
.heroNote{margin-top:16px;color:var(--sub);font-size:12px;display:flex;align-items:center;gap:8px}
.heroNote i{display:inline-block;width:8px;height:8px;border-radius:50%;background:var(--ok)}
.tool{display:flex;gap:9px;flex-wrap:wrap;align-items:center;margin-bottom:11px}
.tool input[type=search]{flex:1;min-width:140px}
input,select{background:var(--card);color:var(--tx);border:1px solid var(--bd);border-radius:12px;padding:11px 13px;font-size:14px;outline:none}
input:focus,select:focus{border-color:var(--pr);box-shadow:0 0 0 3px color-mix(in srgb,var(--pr) 18%,transparent)}
button,.btn{background:var(--pr);color:#fff;border:1px solid transparent;border-radius:12px;padding:10px 14px;font-size:14px;font-weight:650;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;gap:8px;transition:background-color .16s,transform .16s}
button:hover,.btn:hover{filter:brightness(1.06)}
button:active,.btn:active{transform:scale(.97)}
button:focus-visible,.btn:focus-visible{outline:2px solid var(--pr);outline-offset:3px}
button.g,.btn.g{background:var(--card);color:var(--tx);border:1px solid var(--bd)}
button.r{background:transparent;color:var(--er);border:1px solid var(--bd)}
button.i{padding:8px;background:transparent;color:var(--sub);border:1px solid transparent;box-shadow:none}
button.i:hover{color:var(--tx);background:var(--hv);border-color:var(--bd)}
button svg,.btn svg{width:18px;height:18px;flex:none}
[hidden]{display:none!important}
#crumbs{font-size:13px;margin:8px 2px 12px;display:flex;align-items:center;gap:8px;flex-wrap:wrap;color:var(--sub)}
#crumbs a{color:var(--pr);cursor:pointer;text-decoration:none}
#crumbs span{color:var(--sub)}
.list{background:var(--card);border-radius:18px;overflow:hidden;border:1px solid var(--bd)}
.row{display:flex;align-items:center;gap:11px;padding:12px 15px;border-bottom:1px solid var(--bd);transition:background .14s}
.row:last-child{border-bottom:0}
.row:hover{background:var(--hv)}
.row .ic{width:40px;height:40px;display:grid;place-items:center;border-radius:12px;background:var(--card2);color:var(--pr);flex:none}
.row .ic svg{width:22px;height:22px}
.row .ic img{width:24px;height:24px;object-fit:contain}
.row .nm{flex:1;min-width:0;cursor:pointer}
.row .nm div:first-child{word-break:break-word}
.row .mt{font-size:11px;color:var(--sub);margin-top:3px}
.row input[type=checkbox]{width:18px;height:18px}
.filterBar{display:flex;gap:7px;flex-wrap:wrap}
.filterBar button{padding:8px 11px;background:var(--card);color:var(--sub);border:1px solid var(--bd)}
.filterBar button[aria-pressed=true]{background:var(--prSoft);color:var(--pr);border-color:var(--pr)}
.gallery{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(100%,168px),1fr));gap:12px;background:transparent;border:0;overflow:visible}
.gallery-card{position:relative;min-width:0;background:var(--card);border:1px solid var(--bd);border-radius:16px;overflow:hidden}
.gallery-preview{display:grid;width:100%;aspect-ratio:4/3;place-items:center;padding:0;border:0;border-radius:0;background:var(--card2);color:var(--pr);overflow:hidden}
.gallery-preview svg{width:38px;height:38px}
.gallery-preview img{width:100%;height:100%;object-fit:cover}
.gallery-preview img.app-thumb{object-fit:contain;padding:22px}
.gallery-check{position:absolute;inset-inline-start:10px;inset-block-start:10px;width:22px;height:22px;z-index:1}
.gallery-meta{padding:10px 11px 11px;min-width:0}
.gallery-name{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-weight:650;cursor:pointer}
.gallery-actions{display:flex;gap:4px;justify-content:flex-end;margin-top:4px}
.gallery>.empty{grid-column:1/-1}
.empty{padding:48px 20px;text-align:center;color:var(--sub)}
#up{margin-bottom:12px}
#up .u{background:var(--card);border:1px solid var(--bd);border-radius:14px;padding:12px 14px;margin-bottom:8px;font-size:13px}
.bar{height:6px;background:var(--bd);border-radius:8px;margin-top:9px;overflow:hidden}
.bar i{display:block;height:100%;width:0;background:var(--pr);border-radius:8px;transition:width .15s}
.u.done .bar i{background:var(--ok)}.u.fail .bar i{background:var(--er)}
#drop{position:fixed;inset:14px;background:var(--card);color:var(--tx);border:2px dashed var(--pr);border-radius:22px;display:flex;flex-direction:column;gap:14px;align-items:center;justify-content:center;font-size:20px;font-weight:650;z-index:20}
#drop svg{width:48px;height:48px;color:var(--pr)}
#modal{position:fixed;inset:0;background:rgba(0,0,0,.85);z-index:30;display:flex;flex-direction:column}
#modal .top{display:flex;justify-content:space-between;align-items:center;padding:10px 14px;color:#fff;gap:8px}
#modal .top span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;flex:1}
#pv{flex:1;display:flex;align-items:center;justify-content:center;overflow:auto;padding:8px}
#pv img,#pv video,#pv iframe{max-width:100%;max-height:100%;border:0}
#pv iframe{width:100%;height:100%;background:#fff}
#pv pre{background:var(--card);color:var(--tx);padding:14px;border-radius:10px;max-width:100%;overflow:auto;white-space:pre-wrap;word-break:break-word;direction:ltr;text-align:left;margin:0}
#toast{position:fixed;bottom:20px;left:50%;transform:translateX(-50%);background:#15272b;color:#fff;padding:10px 18px;border-radius:12px;font-size:14px;z-index:40;opacity:0;pointer-events:none;transition:opacity .2s}
#toast.show{opacity:1}
@media(max-width:600px){header{padding:11px 14px}.brand img{width:40px;height:40px}.brand b{font-size:15px}.brand small{font-size:11px}#free{white-space:normal;font-size:10px;padding:6px 8px}main{padding:16px 12px 34px}.hero{padding:20px;border-radius:18px}.row{gap:8px;padding:11px 9px}.row .ic{width:36px;height:36px}.tool button,.tool .btn{padding:10px 11px;font-size:13px}.gallery{grid-template-columns:repeat(auto-fill,minmax(min(100%,142px),1fr));gap:9px}}
@media(prefers-reduced-motion:reduce){*,*::before,*::after{scroll-behavior:auto!important;transition-duration:.01ms!important;animation-duration:.01ms!important;animation-iteration-count:1!important}}
</style></head><body>
<header><div class="brand"><img src="/brand.png" alt=""><div><b>Max Share</b><small>مشاركة محلية بين أجهزتك</small></div></div><span id="free"></span></header>
<main>
<section class="hero"><span class="eyebrow">MAX SHARE</span><h1>ملفاتك بين أجهزتك، من المتصفح</h1><p>يتصل جهازك بشبكة المشاركة التي ينشئها هذا الهاتف، ثم يعرض الملفات في صفحة ويب محلية للتصفح والتنزيل، من دون تثبيت التطبيق على الجهاز المستقبِل.</p><div class="heroNote"><i></i> يعمل الخادم محلياً على هاتفك</div></section>
<div class="tool">
   <input type="search" id="q" aria-label="بحث في هذا المجلد" placeholder="بحث في هذا المجلد…">
  <select id="sort"><option value="name">الاسم</option><option value="size">الحجم</option><option value="date">الأحدث</option></select>
</div>
<div class="tool" role="group" aria-label="طريقة عرض الملفات">
  <button class="g" id="btnList" type="button" aria-pressed="true">قائمة الملفات</button>
  <button class="g" id="btnGallery" type="button" aria-pressed="false">معرض الوسائط والتطبيقات</button>
  <div class="filterBar" id="galleryFilters" role="group" aria-label="تصفية المعرض" hidden>
    <button type="button" data-filter="all" aria-pressed="true">الكل</button>
    <button type="button" data-filter="image" aria-pressed="false">الصور</button>
    <button type="button" data-filter="video" aria-pressed="false">الفيديو</button>
    <button type="button" data-filter="audio" aria-pressed="false">الصوت</button>
    <button type="button" data-filter="app" aria-pressed="false">التطبيقات (APK)</button>
  </div>
</div>
<div class="tool">
  <label class="btn" id="bUp"><span data-icon="upload"></span><span>رفع ملفات</span><input type="file" id="fi" multiple hidden></label>
  <button class="g" id="bMk"><span data-icon="folderPlus"></span><span>مجلد جديد</span></button>
  <button class="g" id="bAll"><span data-icon="select"></span><span>تحديد الكل</span></button>
  <button id="bZip" hidden><span data-icon="download"></span><span>تحميل المحدد (ZIP)</span></button>
  <button class="r" id="bDel" hidden><span data-icon="trash"></span><span>حذف المحدد</span></button>
</div>
<div id="crumbs"></div>
<div id="up"></div>
<div class="list" id="list"></div>
</main>
<div id="drop" hidden><span data-icon="upload"></span><span>أفلت الملفات هنا للرفع</span></div>
<div id="modal" hidden>
  <div class="top"><span id="pvn"></span><a class="btn" id="pvd" href="#"><span data-icon="download"></span><span>تحميل</span></a><button class="g" id="pvc" aria-label="إغلاق المعاينة"><span data-icon="close"></span><span>إغلاق</span></button></div>
  <div id="pv"></div>
</div>
<div id="toast"></div>
<script>
(function(){
var cur="",items=[],sel={},info={upload:true,delete:true,free:0,total:0},galleryMode=false,galleryFilter="all";
function g(id){return document.getElementById(id);}
function el(t,c,x){var e=document.createElement(t);if(c)e.className=c;if(x!==undefined)e.textContent=x;return e;}
 var P={
  upload:"<path d='M12 16V4m0 0L7 9m5-5 5 5'/><path d='M5 14v5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-5'/>",
  folderPlus:"<path d='M3 7a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v2'/><path d='M3 9h18l-2 10H5L3 9Z'/><path d='M15 13v4m-2-2h4'/>",
  select:"<path d='M4 7V5a1 1 0 0 1 1-1h2m10 0h2a1 1 0 0 1 1 1v2M4 17v2a1 1 0 0 0 1 1h2m10 0h2a1 1 0 0 0 1-1v-2'/><path d='m8 12 2.5 2.5L16 9'/>",
  download:"<path d='M12 4v12m0 0 5-5m-5 5-5-5'/><path d='M5 18v2h14v-2'/>",
  trash:"<path d='M4 7h16M10 11v6m4-6v6M6 7l1 13h10l1-13M9 7V4h6v3'/>",
  rename:"<path d='m4 16-.7 4 4-.7L19 7.6 16.4 5 4 16Z'/><path d='m14.8 6.6 2.6 2.6'/>",
  close:"<path d='m6 6 12 12M18 6 6 18'/>",
  folder:"<path d='M3 7a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7Z'/>",
  image:"<rect x='3' y='4' width='18' height='16' rx='2'/><circle cx='8.5' cy='9' r='1.5'/><path d='m21 15-5-5L5 20'/>",
  video:"<rect x='3' y='5' width='13' height='14' rx='2'/><path d='m16 10 5-3v10l-5-3z'/>",
  audio:"<path d='M9 18V5l12-2v13'/><circle cx='6' cy='18' r='3'/><circle cx='18' cy='16' r='3'/>",
  document:"<path d='M6 3h8l4 4v14H6z'/><path d='M14 3v5h5M9 13h6m-6 3h6'/>",
  pdf:"<path d='M6 3h8l4 4v14H6z'/><path d='M14 3v5h5M9 15h2a1.5 1.5 0 0 0 0-3H9v6m5-6v6h1a3 3 0 0 0 0-6h-1'/>",
  archive:"<path d='M4 4h16v5H4zM6 9v11h12V9M10 13h4m-2-9v2'/>",
  code:"<path d='m8 8-4 4 4 4m8-8 4 4-4 4m-2-11-4 14'/>",
  table:"<rect x='3' y='4' width='18' height='16' rx='2'/><path d='M3 10h18M9 4v16m6-16v16'/>",
  mail:"<rect x='3' y='5' width='18' height='14' rx='2'/><path d='m4 7 8 6 8-6'/>",
  presentation:"<rect x='3' y='4' width='18' height='13' rx='2'/><path d='m8 21 4-4 4 4M12 17v4m-5-8 3-3 2 2 3-4'/>",
  android:"<path d='M7 8a5 5 0 0 1 10 0M6 8h12v8a2 2 0 0 1-2 2h-1v3h-2v-3h-2v3H9v-3H8a2 2 0 0 1-2-2V8Z'/><path d='m8 3-2-2m10 2 2-2M9 11h.01M15 11h.01'/>",
  file:"<path d='M6 3h8l4 4v14H6z'/><path d='M14 3v5h5'/>"
 };
 function svg(name){return '<svg aria-hidden="true" focusable="false" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">'+(P[name]||P.file)+'</svg>';}
 document.querySelectorAll("[data-icon]").forEach(function(n){n.innerHTML=svg(n.getAttribute("data-icon"));});
 var APP_ICONS={
  "com.whatsapp":"whatsapp","com.facebook.katana":"facebook","com.instagram.android":"instagram",
  "org.telegram.messenger":"telegram","com.snapchat.android":"snapchat","com.google.android.youtube":"youtube",
  "com.zhiliaoapp.musically":"tiktok","com.ss.android.ugc.trill":"tiktok","com.facebook.orca":"messenger",
  "com.discord":"discord","com.twitter.android":"x","us.zoom.videomeetings":"zoom",
  "com.google.android.gm":"gmail","com.android.chrome":"google-chrome","com.google.android.apps.docs":"google-drive",
  "com.google.android.apps.photos":"google-photos","com.google.android.apps.maps":"google-maps",
  "com.android.vending":"google-play","com.google.android.apps.meetings":"google-meet",
  "com.spotify.music":"spotify","com.netflix.mediaclient":"netflix","org.videolan.vlc":"vlc",
  "com.dropbox.android":"dropbox","com.google.android.apps.docs.editors.docs":"google-docs",
  "com.google.android.apps.docs.editors.sheets":"google-sheets","com.google.android.apps.docs.editors.slides":"google-slides",
  "org.mozilla.firefox":"firefox","com.opera.browser":"opera","com.brave.browser":"brave",
  "com.pinterest":"pinterest","com.reddit.frontpage":"reddit","com.viber.voip":"viber",
  "org.thoughtcrime.securesms":"signal","jp.naver.line.android":"line","com.tencent.mm":"wechat",
  "tv.twitch.android.app":"twitch","com.valvesoftware.android.steam.community":"steam","com.github.android":"github"
 };
function enc(p){return p.split("/").map(encodeURIComponent).join("/");}
function join(a,b){return a?a+"/"+b:b;}
function sz(n){if(n<1024)return n+" B";if(n<1048576)return (n/1024).toFixed(1)+" KB";if(n<1073741824)return (n/1048576).toFixed(1)+" MB";return (n/1073741824).toFixed(2)+" GB";}
function ext(n){var i=n.lastIndexOf(".");return i<0?"":n.slice(i+1).toLowerCase();}
var T={img:["jpg","jpeg","png","gif","webp","bmp","svg"],vid:["mp4","webm","mkv","mov","3gp","m4v"],aud:["mp3","wav","ogg","m4a","aac","flac"],txt:["txt","md","log","json","xml","csv","html","js","css","kt","java","py","ini","yml","yaml"],pdf:["pdf"]};
function kind(n){var e=ext(n);for(var k in T){if(T[k].indexOf(e)>=0)return k;}return "";}
function mediaCategory(it){if(it.dir)return "folder";if(ext(it.name)==="apk"||it.applicationId||it.appIconDataUrl)return "app";var k=kind(it.name);if(k==="img")return "image";if(k==="vid")return "video";if(k==="aud")return "audio";return "other";}
function matchesGalleryFilter(it,filter){var category=mediaCategory(it);if(filter==="all")return ["image","video","audio","app"].indexOf(category)>=0;return category===filter;}
 function fileIcon(it){
  if(it.appIconDataUrl)return {src:it.appIconDataUrl,label:it.applicationLabel||"أيقونة التطبيق"};
  var app=APP_ICONS[(it.applicationId||"").toLowerCase()];
  if(app)return {src:"/icons/"+app+".svg",label:it.applicationLabel||"أيقونة التطبيق"};
  if(it.dir)return {path:"folder",label:"مجلد"};
  var e=ext(it.name),k=kind(it.name);
  var byExt={pdf:"pdf",zip:"archive",rar:"archive","7z":"archive",gz:"archive",tar:"archive",apk:"android",
   doc:"document",docx:"document",rtf:"document",odt:"document",xls:"table",xlsx:"table",csv:"table",
   ppt:"presentation",pptx:"presentation",odp:"presentation",eml:"mail",msg:"mail",pst:"mail"};
  var path=byExt[e]||({img:"image",vid:"video",aud:"audio",txt:"code"}[k]||"file");
  return {path:path,label:({pdf:"ملف PDF",archive:"أرشيف",android:"حزمة تطبيق Android",image:"صورة",video:"فيديو",audio:"ملف صوتي",table:"جدول",presentation:"عرض تقديمي",mail:"بريد إلكتروني",document:"مستند",code:"ملف نصي"})[path]||"ملف"};
 }
function toast(m){var t=g("toast");t.textContent=m;t.className="show";clearTimeout(toast.h);toast.h=setTimeout(function(){t.className="";},2600);}
function api(url,data){
  return fetch(url,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:new URLSearchParams(data).toString()})
   .then(function(r){return r.json().then(function(j){if(!r.ok)throw new Error(j.error||("خطأ "+r.status));return j;});});
}
function selKeys(){return Object.keys(sel).filter(function(k){return sel[k];});}
function load(p){
  cur=p;sel={};
  fetch("/api/list?path="+encodeURIComponent(p)).then(function(r){return r.json();})
   .then(function(d){items=d.items||[];render();})
   .catch(function(){toast("تعذّر تحميل القائمة، تحقق من الاتصال بالشبكة");});
}
function loadInfo(){
  fetch("/api/info").then(function(r){return r.json();}).then(function(d){
    info=d;
    g("bUp").hidden=!d.upload;g("bMk").hidden=!d.upload;
    g("free").textContent="المساحة الحرة: "+sz(d.free)+" من "+sz(d.total);
    render();
  });
}
function crumbs(){
  var c=g("crumbs");c.innerHTML="";
  var a=el("a",null,"الرئيسية");a.onclick=function(){load("");};c.appendChild(a);
  var acc="";
  cur.split("/").filter(function(x){return x;}).forEach(function(x){
    acc=join(acc,x);var path=acc;
    c.appendChild(el("span",null,"/"));
    var l=el("a",null,x);l.onclick=function(){load(path);};c.appendChild(l);
  });
}
function view(){
  var q=g("q").value.trim().toLowerCase(),s=g("sort").value;
  var l=items.filter(function(i){return !q||i.name.toLowerCase().indexOf(q)>=0;});
  l.sort(function(a,b){
    if(a.dir!==b.dir)return a.dir?-1:1;
    if(s==="size")return b.size-a.size;
    if(s==="date")return b.mtime-a.mtime;
    return a.name.toLowerCase()<b.name.toLowerCase()?-1:1;
  });
  return l;
}
function visibleItems(){
  var l=view();
  return galleryMode?l.filter(function(it){return matchesGalleryFilter(it,galleryFilter);}):l;
}
function renderGalleryCard(it,box){
  var full=join(cur,it.name),category=mediaCategory(it),card=el("article","gallery-card");
  var cb=el("input","gallery-check");cb.type="checkbox";cb.checked=!!sel[full];
  cb.setAttribute("aria-label","تحديد "+it.name);
  cb.onchange=function(){sel[full]=cb.checked;bulk();};
  card.appendChild(cb);
  var opener=el("button","gallery-preview");opener.type="button";
  opener.setAttribute("aria-label",(category==="app"?"تحميل التطبيق ":"معاينة ")+it.name);
  opener.onclick=function(){open(full,it);};
  var mark=fileIcon(it);
  if(category==="image"||category==="app"&&mark.src){
    var image=el("img",category==="app"?"app-thumb":"media-thumb");
    image.src=category==="image"?"/f/"+enc(full):mark.src;
    image.alt="";image.loading="lazy";
    image.onerror=function(){opener.innerHTML=svg(category==="image"?"image":category==="app"?"android":mark.path);};
    opener.appendChild(image);
  }else{
    opener.innerHTML=svg(category==="video"?"video":category==="audio"?"audio":mark.path);
  }
  card.appendChild(opener);
  var meta=el("div","gallery-meta");
  meta.appendChild(el("div","gallery-name",it.applicationLabel||it.name));
  var labels={image:"صورة",video:"فيديو",audio:"صوت",app:"تطبيق Android"};
  meta.appendChild(el("div","mt",labels[category]+" · "+sz(it.size)));
  var actions=el("div","gallery-actions");
  var dl=el("button","i");dl.innerHTML=svg("download");dl.title="تحميل";dl.setAttribute("aria-label","تحميل "+it.name);
  dl.onclick=function(){download(full);};actions.appendChild(dl);
  if(info["delete"]){
    var rn=el("button","i");rn.innerHTML=svg("rename");rn.title="إعادة تسمية";rn.setAttribute("aria-label","إعادة تسمية "+it.name);
    rn.onclick=function(){rename(full,it.name);};actions.appendChild(rn);
    var rm=el("button","i");rm.innerHTML=svg("trash");rm.title="حذف";rm.setAttribute("aria-label","حذف "+it.name);
    rm.onclick=function(){remove([full]);};actions.appendChild(rm);
  }
  meta.appendChild(actions);card.appendChild(meta);box.appendChild(card);
}
function render(){
  crumbs();
  var box=g("list");box.innerHTML="";
  box.className=galleryMode?"list gallery":"list";
  var l=visibleItems();
  if(!l.length){
    var emptyText=galleryMode?"لا توجد صور أو فيديوهات أو ملفات صوت أو تطبيقات APK في هذا المجلد.":(items.length?"لا نتائج":"المجلد فارغ");
    box.appendChild(el("div","empty",emptyText));
  }
  if(galleryMode){l.forEach(function(it){renderGalleryCard(it,box);});}
  else l.forEach(function(it){
    var full=join(cur,it.name);
    var row=el("div","row");
    var cb=el("input");cb.type="checkbox";cb.checked=!!sel[full];
    cb.setAttribute("aria-label","تحديد "+it.name);
    cb.onchange=function(){sel[full]=cb.checked;bulk();};
    row.appendChild(cb);
    var ic=el("div","ic"),mark=fileIcon(it);
    ic.setAttribute("role","img");ic.setAttribute("aria-label",mark.label);
    if(mark.src){var image=el("img");image.src=mark.src;image.alt="";image.onerror=function(){ic.innerHTML=svg("file");};ic.appendChild(image);}
    else ic.innerHTML=svg(mark.path);
    row.appendChild(ic);
    var nm=el("div","nm");
    nm.appendChild(el("div",null,it.name));
    var sub=it.applicationLabel?"تطبيق: "+it.applicationLabel+" · "+sz(it.size):(it.dir?"مجلد":sz(it.size));
    nm.appendChild(el("div","mt",sub+" · "+new Date(it.mtime).toLocaleString()));
    nm.onclick=function(){it.dir?load(full):open(full,it);};
    row.appendChild(nm);
    var dl=el("button","i");dl.innerHTML=svg("download");dl.title=it.dir?"تحميل مجلد ZIP":"تحميل";dl.setAttribute("aria-label",dl.title);
    dl.onclick=function(){it.dir?zip([full]):download(full);};
    row.appendChild(dl);
    if(info["delete"]){
      var rn=el("button","i");rn.innerHTML=svg("rename");rn.title="إعادة تسمية";rn.setAttribute("aria-label",rn.title);rn.onclick=function(){rename(full,it.name);};
      var rm=el("button","i");rm.innerHTML=svg("trash");rm.title="حذف";rm.setAttribute("aria-label",rm.title);rm.onclick=function(){remove([full]);};
      row.appendChild(rn);row.appendChild(rm);
    }
    box.appendChild(row);
  });
  bulk();
}
function bulk(){
  var n=selKeys().length;
  g("bZip").hidden=!n;g("bZip").lastElementChild.textContent="تحميل المحدد ("+n+") ZIP";
  g("bDel").hidden=!n||!info["delete"];
  g("bAll").lastElementChild.textContent=n?"إلغاء التحديد":"تحديد الكل";
}
function download(p){var a=el("a");a.href="/f/"+enc(p)+"?dl=1";a.download="";document.body.appendChild(a);a.click();a.remove();}
function zip(paths){
  var u="/api/zip?"+paths.map(function(p){return "p="+encodeURIComponent(p);}).join("&");
  var a=el("a");a.href=u;a.download="";document.body.appendChild(a);a.click();a.remove();
}
function remove(paths){
  if(!confirm("حذف "+paths.length+" عنصر؟ لا يمكن التراجع."))return;
  var i=0;
  (function next(){
    if(i>=paths.length){toast("تم الحذف");loadInfo();load(cur);return;}
    api("/api/delete",{path:paths[i++]}).then(next).catch(function(e){toast(e.message);load(cur);});
  })();
}
function rename(p,old){
  var n=prompt("الاسم الجديد",old);
  if(!n||n===old)return;
  api("/api/rename",{path:p,name:n}).then(function(){load(cur);}).catch(function(e){toast(e.message);});
}
function open(p,it){
  var k=kind(it.name),pv=g("pv");pv.innerHTML="";
  g("pvn").textContent=it.name;g("pvd").href="/f/"+enc(p)+"?dl=1";
  var url="/f/"+enc(p);
  if(k==="img"){var im=el("img");im.src=url;pv.appendChild(im);}
  else if(k==="vid"){var v=el("video");v.src=url;v.controls=true;v.autoplay=true;pv.appendChild(v);}
  else if(k==="aud"){var a=el("audio");a.src=url;a.controls=true;a.autoplay=true;pv.appendChild(a);}
  else if(k==="pdf"){var f=el("iframe");f.src=url;pv.appendChild(f);}
  else if(k==="txt"&&it.size<1048576){
    var pre=el("pre",null,"جارٍ التحميل…");pv.appendChild(pre);
    fetch(url).then(function(r){return r.text();}).then(function(t){pre.textContent=t;});
  }else{download(p);return;}
  g("modal").hidden=false;
}
function closePv(){g("modal").hidden=true;g("pv").innerHTML="";}
var queue=[],busy=false;
function enqueue(files){
  if(!info.upload){toast("الرفع معطّل من إعدادات التطبيق");return;}
  for(var i=0;i<files.length;i++)queue.push({f:files[i],dir:cur});
  if(!busy)next();
}
function next(){
  var job=queue.shift();
  if(!job){busy=false;loadInfo();load(cur);return;}
  busy=true;
  var box=el("div","u");var t=el("div",null,job.f.name+" ("+sz(job.f.size)+")");
  var bar=el("div","bar");var fill=el("i");bar.appendChild(fill);
  var st=el("div","mt","");box.appendChild(t);box.appendChild(bar);box.appendChild(st);g("up").appendChild(box);
  var x=new XMLHttpRequest(),t0=Date.now();
   x.open("POST","/api/upload?path="+encodeURIComponent(job.dir)+"&name="+encodeURIComponent(job.f.name));
   x.setRequestHeader("Content-Type","application/octet-stream");
  x.upload.onprogress=function(e){
    if(!e.lengthComputable)return;
    var p=Math.round(e.loaded*100/e.total);fill.style.width=p+"%";
    var s=(Date.now()-t0)/1000;st.textContent=p+"% — "+sz(e.loaded/Math.max(s,0.1))+"/ث";
  };
  x.onload=function(){
     if(x.status===200){box.className="u done";fill.style.width="100%";st.textContent="اكتمل الرفع";}
    else{box.className="u fail";var m="فشل";try{m=JSON.parse(x.responseText).error||m;}catch(e){}st.textContent=m;}
    setTimeout(function(){box.remove();},4000);next();
  };
  x.onerror=function(){box.className="u fail";st.textContent="انقطع الاتصال";setTimeout(function(){box.remove();},5000);next();};
   // إرسال الملف الخام يتجنب multipart المؤقت ونسخ الملف الكبير مرتين على الهاتف.
   x.send(job.f);
}
function syncGalleryControls(){
  g("btnList").setAttribute("aria-pressed",String(!galleryMode));
  g("btnGallery").setAttribute("aria-pressed",String(galleryMode));
  g("galleryFilters").hidden=!galleryMode;
  document.querySelectorAll("[data-filter]").forEach(function(button){
    button.setAttribute("aria-pressed",String(button.getAttribute("data-filter")===galleryFilter));
  });
}
g("btnList").onclick=function(){galleryMode=false;syncGalleryControls();render();};
g("btnGallery").onclick=function(){galleryMode=true;syncGalleryControls();render();};
document.querySelectorAll("[data-filter]").forEach(function(button){
  button.onclick=function(){galleryMode=true;galleryFilter=button.getAttribute("data-filter");syncGalleryControls();render();};
});
g("fi").onchange=function(e){enqueue(e.target.files);e.target.value="";};
g("bMk").onclick=function(){var n=prompt("اسم المجلد الجديد");if(n)api("/api/mkdir",{path:cur,name:n}).then(function(){load(cur);}).catch(function(e){toast(e.message);});};
g("bAll").onclick=function(){var any=selKeys().length>0;sel={};if(!any)visibleItems().forEach(function(i){sel[join(cur,i.name)]=true;});render();};
g("bZip").onclick=function(){zip(selKeys());};
g("bDel").onclick=function(){remove(selKeys());};
g("q").oninput=render;g("sort").onchange=render;
g("pvc").onclick=closePv;
document.addEventListener("keydown",function(e){if(e.key==="Escape")closePv();});
var depth=0;
window.addEventListener("dragenter",function(e){e.preventDefault();depth++;if(info.upload)g("drop").hidden=false;});
window.addEventListener("dragleave",function(e){e.preventDefault();depth=Math.max(0,depth-1);if(!depth)g("drop").hidden=true;});
window.addEventListener("dragover",function(e){e.preventDefault();});
window.addEventListener("drop",function(e){e.preventDefault();depth=0;g("drop").hidden=true;if(e.dataTransfer&&e.dataTransfer.files.length)enqueue(e.dataTransfer.files);});
syncGalleryControls();loadInfo();load("");
})();
</script></body></html>"""
}
