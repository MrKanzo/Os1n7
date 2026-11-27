package ua.local.diiaaudit;

import android.app.Activity;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.*;
import android.provider.Settings;
import android.view.WindowInsets;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

/** Local read-only APK audit and system network counters. No external requests. */
public final class PocketAuditActivity extends Activity {
 private final List<ApplicationInfo> apps=new ArrayList<>();
 private Spinner spinner; private TextView result,traffic; private Button scan;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private long startTime=-1,wallTime,rx,tx,rp,tp; private int uid;
 private boolean measuring=false; private long offRx=0,offTx=0,offStartRx=-1,offStartTx=-1; private int screenChanges=0;
 private final BroadcastReceiver screen=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
  if(!measuring)return;
  if(Intent.ACTION_SCREEN_OFF.equals(i.getAction())){offStartRx=TrafficStats.getTotalRxBytes();offStartTx=TrafficStats.getTotalTxBytes();screenChanges++;}
  if(Intent.ACTION_SCREEN_ON.equals(i.getAction())){closeOff();screenChanges++;}
 }};
 private final Runnable tick=new Runnable(){public void run(){if(!measuring)return;long elapsed=SystemClock.elapsedRealtime()-startTime;if(elapsed>=300000){finishMeasure();return;}traffic.setText("Замір: "+elapsed/1000+" / 300 с\nНадіслано всім телефоном: "+delta(TrafficStats.getTotalTxBytes(),tx)+" Б\nОтримано: "+delta(TrafficStats.getTotalRxBytes(),rx)+" Б");handler.postDelayed(this,1000);}};
 @Override public void onCreate(Bundle b){super.onCreate(b);
  ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setBackgroundColor(0xff0f1720);body.setPadding(24,24,24,24);scroll.addView(body);setContentView(scroll);
  if(Build.VERSION.SDK_INT>=30)body.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets a=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(24,24+a.top,24,24+a.bottom);return i;});
  button(body,"← Security Center",()->finish());text(body,"POCKET AUDIT • локальна лабораторія",22);
  text(body,"Аналіз APK не змінює встановлені програми. Збіги в коді не підтверджують передачу даних.",14);
  PackageManager pm=getPackageManager();apps.addAll(pm.getInstalledApplications(0));apps.sort(Comparator.comparing(a->a.loadLabel(pm).toString().toLowerCase(Locale.ROOT)));
  List<String> names=new ArrayList<>();int diia=0;for(int i=0;i<apps.size();i++){ApplicationInfo a=apps.get(i);names.add(a.loadLabel(pm)+" • "+a.packageName);if(a.packageName.equals("ua.gov.diia.app"))diia=i;}
  spinner=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);body.addView(spinner);if(!apps.isEmpty())spinner.setSelection(diia);
  scan=button(body,"Перевірити вибрану програму",this::audit);
  result=text(body,"Обери програму й натисни перевірку.",14);
  button(body,"Копіювати звіт",()->{ClipboardManager c=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);c.setPrimaryClip(ClipData.newPlainText("Pocket Audit",result.getText()+"\n"+traffic.getText()));});
  text(body,"Системний фаєрвол • без VPN",19);
  text(body,"Потрібне явне ADB-спарювання (Android 11+). Блокування лише вибраної несистемної програми; спершу перевір на некритичній програмі. Розблокуй перед видаленням OS1N7.",14);
  button(body,"Заблокувати вибрану",()->openFirewall(true));button(body,"Розблокувати вибрану",()->openFirewall(false));
  text(body,"Pocket Traffic • 5 хвилин",19);
  button(body,"Дозволити статистику застосунків",()->startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
  button(body,"Почати замір",this::startMeasure);button(body,"Завершити замір",this::finishMeasure);
  traffic=text(body,"Поки немає заміру. Без VPN: системні лічильники, а не адреси пакетів.",14);
  IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_SCREEN_ON);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(screen,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screen,f);
 }
 private ApplicationInfo selected(){int p=spinner.getSelectedItemPosition();return p>=0&&p<apps.size()?apps.get(p):null;}
 private Button button(LinearLayout p,String s,Runnable r){Button v=new Button(this);v.setText(s);v.setOnClickListener(w->r.run());p.addView(v);return v;}
 private TextView text(LinearLayout p,String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Color.WHITE);v.setTextIsSelectable(true);v.setPadding(0,12,0,12);p.addView(v);return v;}
 private void openFirewall(boolean block){ApplicationInfo a=selected();if(a==null)return;String[] shared=getPackageManager().getPackagesForUid(a.uid);
  if(a.packageName.equals(getPackageName())||(a.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0||a.uid<10000||a.uid>=100000||shared==null||shared.length!=1){Toast.makeText(this,"Системні програми, OS1N7 та спільні UID не блокуються.",Toast.LENGTH_LONG).show();return;}
  Intent i=new Intent(this,LocalFirewallActivity.class);i.putExtra("packages",a.packageName);i.putExtra("block",block);startActivity(i);
 }
 private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
 private static final Pattern URL=Pattern.compile("https?://([A-Za-z0-9.-]{3,253})(?=[:/ ?#\\x00]|$)",Pattern.CASE_INSENSITIVE);
 private static final String[] MARKERS={"com/google/firebase/analytics","com/google/android/gms/measurement","com/google/firebase/crashlytics","com/carto/","com/facebook/appevents","com/appsflyer/","com/adjust/sdk/","com/amplitude/","com/mixpanel/"};
 private void audit(){ApplicationInfo a=selected();if(a==null)return;scan.setEnabled(false);result.setText("Аналізуємо "+a.packageName+"…");new Thread(()->{String output;
  try {PackageManager pm=getPackageManager();int flags=PackageManager.GET_PERMISSIONS|(Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES);PackageInfo pi=pm.getPackageInfo(a.packageName,flags);
   StringBuilder s=new StringBuilder(a.loadLabel(pm)+"\n"+a.packageName+"\nВерсія: "+pi.versionName+"\n\nДОЗВОЛИ\n");String[] perms=pi.requestedPermissions;if(perms!=null)for(int n=0;n<perms.length;n++){boolean granted=pi.requestedPermissionsFlags!=null&&n<pi.requestedPermissionsFlags.length&&(pi.requestedPermissionsFlags[n]&PackageInfo.REQUESTED_PERMISSION_GRANTED)!=0;s.append(granted?"Надано: ":"Не надано: ").append(perms[n]).append(" — ").append(permissionHint(perms[n])).append('\n');}
   s.append("\nСЕРТИФІКАТИ (за даними Android)\n");Signature[] sigs=Build.VERSION.SDK_INT>=28&&pi.signingInfo!=null?pi.signingInfo.getApkContentsSigners():pi.signatures;if(sigs!=null)for(Signature sig:sigs)s.append(hex(MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()))).append('\n');
   List<String> paths=new ArrayList<>();paths.add(a.sourceDir);if(a.splitSourceDirs!=null)Collections.addAll(paths,a.splitSourceDirs);
   Set<String> domains=new TreeSet<>(),markers=new TreeSet<>();boolean limit=false;
   for(String path:paths){File file=new File(path);MessageDigest sha=MessageDigest.getInstance("SHA-256");byte[] data=new byte[65536];
    try(InputStream in=new FileInputStream(file)){int n;while((n=in.read(data))!=-1)sha.update(data,0,n);}s.append("\n").append(file.getName()).append(" SHA-256: ").append(hex(sha.digest())).append('\n');
    try(ZipFile z=new ZipFile(file)){Enumeration<? extends ZipEntry> it=z.entries();while(it.hasMoreElements()){ZipEntry e=it.nextElement();if(e.isDirectory()||!(e.getName().endsWith(".dex")||e.getName().endsWith(".so")||e.getName().endsWith(".xml")||e.getName().endsWith(".json")||e.getName().endsWith(".arsc")))continue;
      try(InputStream in=z.getInputStream(e)){String tail="";long read=0;int n;while((n=in.read(data))!=-1){read+=n;if(read>256L*1024*1024){limit=true;break;}String chunk=tail+new String(data,0,n,StandardCharsets.ISO_8859_1);for(String m:MARKERS)if(chunk.contains(m))markers.add(m);Matcher match=URL.matcher(chunk);while(match.find()){String d=match.group(1).toLowerCase(Locale.ROOT);if(d.contains(".")&&!d.endsWith(".")){if(domains.size()<500)domains.add(d);else if(!domains.contains(d))limit=true;}}tail=chunk.substring(Math.max(0,chunk.length()-512));}}}}
   }
   s.append("\nОЗНАКИ БІБЛІОТЕК (не доказ використання)\n").append(markers.isEmpty()?"Збігів не виявлено":String.join("\n",markers));s.append("\n\nURL-ДОМЕНИ В КОДІ (не фактичні з'єднання)\n");for(String d:domains)s.append(d).append(" — ").append(domainHint(d)).append('\n');if(limit)s.append("\nДосягнуто лімітів пошуку, звіт неповний.");output=s.toString();
  }catch(Exception e){output="Не вдалося завершити аудит: "+e.getMessage();}
  final String out=output;runOnUiThread(()->{if(!isDestroyed()){result.setText(out);scan.setEnabled(true);}});}).start();}
 private String permissionHint(String p){if(p.contains("LOCATION"))return "геолокація";if(p.endsWith("CAMERA"))return "камера";if(p.endsWith("RECORD_AUDIO"))return "мікрофон";if(p.endsWith("AD_ID"))return "рекламний ідентифікатор";if(p.endsWith("INTERNET"))return "мережевий доступ";if(p.endsWith("READ_CONTACTS"))return "контакти";if(p.endsWith("READ_PHONE_STATE"))return "стан телефона";return "системний дозвіл";}
 private String domainHint(String d){if(d.equals("diia.gov.ua")||d.endsWith(".diia.gov.ua"))return "домен Дії";if(d.endsWith(".gov.ua"))return "державний домен";if(d.contains("crashlytics"))return "можлива діагностика збоїв";if(d.equals("app-measurement.com")||d.endsWith(".app-measurement.com"))return "можлива аналітика";if(d.contains("firebase"))return "можлива інфраструктура Firebase";if(d.contains("googlesyndication")||d.contains("googleadservices"))return "можлива рекламна інфраструктура";return "призначення не встановлено";}
 private static long delta(long a,long b){return a<0||b<0||a<b?-1:a-b;}
 private void startMeasure(){if(measuring){traffic.setText("Замір вже триває.");return;}ApplicationInfo a=selected();if(a==null)return;uid=a.uid;startTime=SystemClock.elapsedRealtime();wallTime=System.currentTimeMillis();rx=TrafficStats.getTotalRxBytes();tx=TrafficStats.getTotalTxBytes();rp=TrafficStats.getTotalRxPackets();tp=TrafficStats.getTotalTxPackets();offRx=0;offTx=0;offStartRx=-1;offStartTx=-1;screenChanges=0;measuring=true;handler.post(tick);}
 private void closeOff(){if(offStartRx<0)return;long r=delta(TrafficStats.getTotalRxBytes(),offStartRx),t=delta(TrafficStats.getTotalTxBytes(),offStartTx);if(r>=0&&t>=0){offRx+=r;offTx+=t;}offStartRx=-1;offStartTx=-1;}
 private void finishMeasure(){if(!measuring)return;closeOff();measuring=false;handler.removeCallbacks(tick);long end=System.currentTimeMillis();final long start=wallTime;final int selectedUid=uid;
  final String summary="Тривалість: "+(SystemClock.elapsedRealtime()-startTime)/1000+" с\nВесь телефон: ↑ "+delta(TrafficStats.getTotalTxBytes(),tx)+" Б / "+delta(TrafficStats.getTotalTxPackets(),tp)+" пакетів; ↓ "+delta(TrafficStats.getTotalRxBytes(),rx)+" Б / "+delta(TrafficStats.getTotalRxPackets(),rp)+" пакетів.\nВимкнений екран (лише отримані події, весь телефон): ↑ "+offTx+" Б; ↓ "+offRx+" Б. Подій: "+screenChanges+".\n-1 означає недоступний лічильник. Можливі пропущені інтервали.";
  traffic.setText(summary+"\nСтатистика UID…");new Thread(()->{String row;try{NetworkStatsManager m=(NetworkStatsManager)getSystemService(NETWORK_STATS_SERVICE);long r=0,t=0;for(int type:new int[]{ConnectivityManager.TYPE_WIFI,ConnectivityManager.TYPE_MOBILE}){NetworkStats stats=m.querySummary(type,null,start,end);try{NetworkStats.Bucket bucket=new NetworkStats.Bucket();while(stats.hasNextBucket()&&stats.getNextBucket(bucket)){if(bucket.getUid()==selectedUid){r+=bucket.getRxBytes();t+=bucket.getTxBytes();}}}finally{stats.close();}}row="\nUID "+selectedUid+" (приблизна статистика Android, може запізнюватися): ↑ "+t+" Б; ↓ "+r+" Б.";}catch(Exception e){row="\nСтатистика UID недоступна: перевір системний дозвіл.";}final String out=summary+row;runOnUiThread(()->{if(!isDestroyed()&&!measuring)traffic.setText(out);});}).start();}
 @Override public void onDestroy(){handler.removeCallbacks(tick);measuring=false;unregisterReceiver(screen);super.onDestroy();}
}
