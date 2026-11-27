package ua.local.diiaaudit;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public final class LocalFirewallActivity extends Activity {
 private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
 private EditText host,pairPort,code,connectPort;private TextView status;private Button pair,apply,check;private CheckBox consent;private final List<String> selected=new ArrayList<>();private boolean block;private boolean busy;
 interface Task {String run() throws Exception;}
 @Override public void onCreate(Bundle b){super.onCreate(b);setTitle("Pocket Firewall • локальний ADB");
  block=getIntent().getBooleanExtra("block",false);String csv=getIntent().getStringExtra("packages");if(csv!=null&&!csv.isEmpty())selected.addAll(Arrays.asList(csv.split(",")));
  ScrollView sv=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(24,24,24,24);l.setBackgroundColor(0xff0b121a);sv.addView(l);setContentView(sv);
  if(Build.VERSION.SDK_INT>=30)l.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(24,24+bars.top,24,24+bars.bottom);return insets;});
  Button back=button(l,"← Назад");back.setOnClickListener(v->finish());
  label(l,"Без VPN і Shizuku. Потрібен дозвіл Android на бездротове налагодження. Використовуй довірену Wi-Fi мережу. Для спарювання відкрий налаштування у розділеному екрані, щоб код залишався дійсним.");
  Button settings=button(l,"Відкрити параметри розробника");settings.setOnClickListener(v->{try{startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));}catch(Exception e){status.setText(e.toString());}});
  host=field(l,"IP цього телефона (типово 127.0.0.1)",false);host.setText("127.0.0.1");pairPort=field(l,"Порт спарювання",true);code=field(l,"6-значний код",true);code.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);pair=button(l,"1. Спарувати з Android");
  connectPort=field(l,"Порт підключення (інший, ніж порт спарювання)",true);connectPort.setText(getPreferences(0).getString("port",""));
  label(l,"Вибрано: "+(selected.isEmpty()?"жодної програми":String.join(", ",selected)));consent=new CheckBox(this);consent.setTextColor(Color.WHITE);consent.setText("Дозволяю зміну системних правил. Якщо chain 3 вимкнено, його активація може відновити раніше збережені правила інших фаєрволів.");l.addView(consent);
  check=button(l,"2. Перевірити підключення та правила");apply=button(l,block?"3. Заблокувати вибрану":"3. Розблокувати вибрану");status=label(l,"Не підключено. Після роботи вимкни бездротове налагодження. Після перезавантаження правила можуть скидатися.");
  pair.setOnClickListener(v->{String h=host.getText().toString(),c=code.getText().toString(),p=pairPort.getText().toString();code.setText("");work(()->{if(Build.VERSION.SDK_INT<30||!c.matches("[0-9]{6}"))throw new Exception("Потрібен Android 11+ і код із шести цифр");LocalAdb a=new LocalAdb(this);try{if(!a.pair(LocalAdb.localHost(h),port(p),c))throw new Exception("Спарювання відхилено");return "Спаровано. Введи основний порт підключення.";}finally{a.disconnect();}});});
  check.setOnClickListener(v->operation(false));apply.setOnClickListener(v->{if(!consent.isChecked()){status.setText("Підтвердь дозвіл на зміну правил.");return;}operation(true);});refresh();
 }
 private int port(String s){int p=Integer.parseInt(s.trim());if(p<1||p>65535)throw new IllegalArgumentException("Неправильний порт");return p;}
 private void operation(boolean change){String h=host.getText().toString(),p=connectPort.getText().toString();if(change&&selected.isEmpty())return;getPreferences(0).edit().putString("port",p).apply();work(()->{
  if(Build.VERSION.SDK_INT<30)throw new Exception("Потрібен Android 11+");LocalAdb a=new LocalAdb(this);try{if(!a.connect(LocalAdb.localHost(h),port(p)))throw new Exception("ADB не підключено");boolean enabled=a.chain();StringBuilder out=new StringBuilder("Системний ланцюг: "+(enabled?"увімкнено":"вимкнено")+"\n");
  for(String pkg:selected)validate(pkg);
  if(change&&block&&!enabled){a.enableChain();enabled=true;out.append("Ланцюг увімкнено.\n");}
  for(String pkg:selected){try{if(change)a.setDenied(pkg,block);boolean deny=a.denied(pkg);out.append(pkg).append(": ").append(deny?(enabled?"заборонено правилом":"правило неактивне"):"немає заборони цим ланцюгом").append('\n');}catch(Exception e){out.append(pkg).append(": НЕ ПІДТВЕРДЖЕНО — ").append(e.getMessage()).append('\n');}}
  return out.toString();}finally{a.disconnect();}});}
 private void validate(String p)throws Exception{LocalAdb.pkg(p);ApplicationInfo a=getPackageManager().getApplicationInfo(p,0);String[] shared=getPackageManager().getPackagesForUid(a.uid);if(p.equals(getPackageName())||p.equals("com.arslan.shizuwall")||p.equals("moe.shizuku.privileged.api")||(a.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0||a.uid<10000||a.uid>=100000||shared==null||shared.length!=1)throw new Exception("Небезпечний або спільний UID: "+p);}
 private void work(Task t){if(busy)return;busy=true;refresh();status.setText("Виконується…");WORK.execute(()->{String result;try{result=t.run();}catch(Exception e){result="Не підтверджено: "+e.getClass().getSimpleName()+": "+e.getMessage();}final String msg=result;runOnUiThread(()->{if(isDestroyed())return;busy=false;refresh();status.setText(msg);});});}
 private void refresh(){pair.setEnabled(!busy);check.setEnabled(!busy);apply.setEnabled(!busy&&!selected.isEmpty());}
 private TextView label(LinearLayout l,String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(15);t.setPadding(0,10,0,10);l.addView(t);return t;}
 private EditText field(LinearLayout l,String hint,boolean numeric){EditText e=new EditText(this);e.setTextColor(Color.WHITE);e.setHintTextColor(0xffa5bac8);e.setHint(hint);e.setSingleLine(true);e.setInputType(numeric?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_TEXT);l.addView(e);return e;}
 private Button button(LinearLayout l,String s){Button b=new Button(this);b.setText(s);l.addView(b);return b;}
}
