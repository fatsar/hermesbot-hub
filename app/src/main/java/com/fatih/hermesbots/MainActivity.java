package com.fatih.hermesbots;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private static final int BG=0xff101113,CARD=0xff1c1d20,INK=0xfff3f3f4,MUTED=0xff96989e,MINT=0xffd8dbdf;
 private static JSONArray bots;
 private static final Set<String> busy=new HashSet<>();
 private static final ExecutorService pool=Executors.newFixedThreadPool(4);
 private static final Handler ui=new Handler(Looper.getMainLooper());
 private static java.lang.ref.WeakReference<MainActivity> active=new java.lang.ref.WeakReference<>(null);
 private static boolean storageFailed;
 private BotStore store; private LinearLayout root,body; private String selected;
 private EditText composer;
 private int tab=0; private String filter=""; private boolean pinnedOnly=false;
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(BG);store=new BotStore(this);
  if(bots==null){try{bots=store.load();}catch(Exception e){storageFailed=true;bots=new JSONArray();}}
  if(state!=null)selected=state.getString("selected");
  if(storageFailed){new AlertDialog.Builder(this).setTitle("Kayıtlar açılamadı").setMessage("Şifreli veriler okunamadı. Eski dosyayı korumak için bu oturumda kayıt kapatıldı.").setPositiveButton("Tamam",null).show();}
  render();
 }
 @Override protected void onResume(){super.onResume();active=new java.lang.ref.WeakReference<>(this);render();}
 @Override protected void onPause(){if(active.get()==this)active.clear();save();super.onPause();}
 @Override protected void onSaveInstanceState(Bundle b){b.putString("selected",selected);super.onSaveInstanceState(b);}
 @Override public void onBackPressed(){if(selected!=null){selected=null;render();}else super.onBackPressed();}
 int dp(int n){return (int)(getResources().getDisplayMetrics().density*n);}
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(18));return d;}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(6),0,dp(6));return t;}
 void title(LinearLayout l,String s){TextView t=text(s,28,INK);t.setTypeface(null,Typeface.BOLD);l.addView(t);}
 Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);boolean primary=s.equals("İlk botunu oluştur");b.setTextColor(primary?BG:INK);b.setBackgroundTintList(null);GradientDrawable bg=shape(primary?MINT:CARD);if(!primary)bg.setStroke(dp(1),0xff33353a);b.setBackground(bg);b.setMinHeight(dp(48));b.setPadding(dp(14),dp(4),dp(14),dp(4));b.setOnClickListener(v->r.run());return b;}
 void gap(LinearLayout l,int n){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(n)));}
 JSONObject find(String id){if(id==null)return null;for(int i=0;i<bots.length();i++){JSONObject b=bots.optJSONObject(i);if(id.equals(b.optString("id")))return b;}return null;}
 void put(JSONObject b,String k,Object v){try{b.put(k,v);}catch(JSONException e){throw new IllegalStateException(e);}}
 boolean save(){if(storageFailed)return false;try{store.save(bots);return true;}catch(Exception e){toast("Kayıt başarısız. Depolama alanını kontrol edin.");return false;}}
 void toast(String m){Toast.makeText(this,m,Toast.LENGTH_LONG).show();}
 void render(){
  root=column();root.setBackgroundColor(BG);root.setPadding(dp(20),dp(8),dp(20),dp(12));root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(20)+insets.getSystemWindowInsetLeft(),dp(8)+insets.getSystemWindowInsetTop(),dp(20)+insets.getSystemWindowInsetRight(),dp(12)+insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});setContentView(root);root.requestApplyInsets();
  JSONObject b=find(selected);if(b==null){selected=null;home();}else chat(b);
 }

 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 void line(LinearLayout l){View v=new View(this);v.setBackgroundColor(0xff292a2e);l.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
 TextView avatar(JSONObject b,int size){String n=b.optString("name","B");TextView a=text(n.isEmpty()?"B":n.substring(0,1).toUpperCase(new Locale("tr")),size/3,INK);a.setGravity(Gravity.CENTER);int[] colors={0xff3d414a,0xff42374a,0xff344643,0xff4a4034};a.setBackground(shape(colors[Math.abs(b.optString("id").hashCode()%colors.length)]));a.setPadding(0,0,0,0);a.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));a.setContentDescription(n);return a;}
 View icon(String type,String label,Runnable action){IconView v=new IconView(this,type);v.setContentDescription(label);v.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));v.setOnClickListener(x->action.run());v.setFocusable(true);return v;}
 void home(){
  LinearLayout header=row();TextView brand=text("botluk",25,INK);brand.setTypeface(null,Typeface.BOLD);header.addView(brand,new LinearLayout.LayoutParams(0,dp(56),1));header.addView(icon("plus","Yeni bot",()->edit(null)));root.addView(header);gap(root,16);
  if(tab==2){settings();navigation();return;}
  title(root,tab==0?"Sohbetler":"Botların");
  if(tab==0){
   EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(15);search.setTextColor(INK);search.setHintTextColor(MUTED);search.setHint("Sohbetlerde ara");search.setBackground(shape(CARD));search.setPadding(dp(16),0,dp(16),0);search.setText(filter);root.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));gap(root,12);
   LinearLayout filters=row();Button all=button("Tümü",()->{pinnedOnly=false;render();});Button pinned=button("Sabitlenenler",()->{pinnedOnly=true;render();});all.setTextColor(!pinnedOnly?INK:MUTED);pinned.setTextColor(pinnedOnly?INK:MUTED);filters.addView(all);Space spacer=new Space(this);filters.addView(spacer,new LinearLayout.LayoutParams(dp(8),1));filters.addView(pinned);root.addView(filters);
   search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence c,int a,int b,int d){}public void afterTextChanged(android.text.Editable e){}public void onTextChanged(CharSequence c,int a,int b,int d){filter=c.toString();inbox();}});
  }else root.addView(text("Her sohbetin arkasında kendi botun.",14,MUTED));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body=column();scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));inbox();navigation();
 }
 void inbox(){
  if(body==null)return;body.removeAllViews();gap(body,14);int count=0;
  java.util.ArrayList<JSONObject> ordered=new java.util.ArrayList<>();for(int i=0;i<bots.length();i++)ordered.add(bots.optJSONObject(i));
  ordered.sort((a,b)->{int p=Boolean.compare(b.optBoolean("pinned"),a.optBoolean("pinned"));return p!=0?p:Long.compare(b.optLong("updated"),a.optLong("updated"));});
  for(JSONObject b:ordered){
   JSONArray h=b.optJSONArray("messages");String preview=h.length()==0?"Sohbeti başlat":h.optJSONObject(h.length()-1).optString("content").replace("\n"," ");
   String searchable=b.optString("name")+" "+h.toString();
   if(tab==0 && ((!searchable.toLowerCase(new Locale("tr")).contains(filter.toLowerCase(new Locale("tr")))) || (pinnedOnly&&!b.optBoolean("pinned"))))continue;count++;
   LinearLayout item=row();item.setPadding(0,dp(14),0,dp(14));item.setMinimumHeight(dp(86));item.addView(avatar(b,48));LinearLayout info=column();LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-2,1);ip.setMargins(dp(14),0,dp(8),0);item.addView(info,ip);
   LinearLayout heading=row();TextView name=text(b.optString("name"),17,INK);name.setTypeface(null,Typeface.BOLD);name.setMaxLines(1);heading.addView(name,new LinearLayout.LayoutParams(0,-2,1));TextView time=text(b.optBoolean("pinned")?"●":b.optLong("updated")==0?"":new java.text.SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(b.optLong("updated"))),11,MUTED);heading.addView(time);info.addView(heading);
   TextView last=text(tab==1?b.optString("prompt"):busy.contains(b.optString("id"))?"Yanıt yazıyor…":preview,14,MUTED);last.setMaxLines(2);last.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(last);
   TextView badge=text(b.optBoolean("demo")?"DEMO":"HERMES",10,MUTED);badge.setLetterSpacing(.1f);info.addView(badge);
   item.setOnClickListener(v->{if(tab==1)edit(b);else openBot(b);});item.setOnLongClickListener(v->{botMenu(b);return true;});body.addView(item);line(body);
  }
  if(count==0){gap(body,38);TextView mark=text("◌",52,MUTED);mark.setGravity(Gravity.CENTER);body.addView(mark);TextView t=text(bots.length()==0?"İlk sohbetin burada başlıyor":"Sohbet bulunamadı",22,INK);t.setGravity(Gravity.CENTER);body.addView(t);TextView desc=text(bots.length()==0?"Bir bot oluştur, ona bir rol ver\nve konuşmaya başla.":"Aramanı veya seçili filtreyi değiştir.",15,MUTED);desc.setGravity(Gravity.CENTER);body.addView(desc);gap(body,20);if(bots.length()==0){body.addView(button("İlk botunu oluştur",()->edit(null)));gap(body,12);body.addView(button("Demo sohbetlerini dene",()->{addDemo("Asistan","Günlük işlerde yardımcı ol. Türkçe yanıt ver.");addDemo("Araştırmacı","Araştırma yap ve kaynakları belirt.");addDemo("Yazılımcı","Kod yaz ve açıklamalarını Türkçe yap.");save();render();}));}}
 }
 void botMenu(JSONObject b){new AlertDialog.Builder(this).setTitle(b.optString("name")).setItems(new String[]{b.optBoolean("pinned")?"Sabitlemeyi kaldır":"Sohbeti sabitle","Botu düzenle"},(d,i)->{if(i==0){put(b,"pinned",!b.optBoolean("pinned"));save();render();}else edit(b);}).show();}
 void navigation(){gap(root,8);line(root);LinearLayout nav=row();String[] labels={"Sohbetler","Botlar","Ayarlar"};String[] symbols={"chat","bots","settings"};for(int i=0;i<3;i++){final int n=i;LinearLayout cell=column();cell.setGravity(Gravity.CENTER);IconView iv=new IconView(this,symbols[i]);iv.setAlpha(tab==i?1f:.45f);cell.addView(iv,new LinearLayout.LayoutParams(dp(38),dp(30)));TextView tx=text(labels[i],11,tab==i?INK:MUTED);tx.setGravity(Gravity.CENTER);cell.addView(tx);cell.setMinimumHeight(dp(64));cell.setContentDescription(labels[i]);cell.setOnClickListener(v->{tab=n;filter="";render();});nav.addView(cell,new LinearLayout.LayoutParams(0,-2,1));}root.addView(nav);}
 void settings(){title(root,"Ayarlar");root.addView(text("BOTLUK  /  0.2.0",12,MUTED));gap(root,24);root.addView(button("Hermes bağlantı rehberi",this::help));gap(root,12);root.addView(button("Bot bağlantılarını düzenle",()->{tab=1;render();}));gap(root,24);root.addView(text("Sohbetler bu cihazda şifreli saklanır.\nModel ve araçlar bağlandığın Hermes sunucusunda çalışır.",15,MUTED));root.addView(new Space(this),new LinearLayout.LayoutParams(1,0,1));}
 void textHeader(String s){root.addView(text(s,11,MINT));}
 void openBot(JSONObject b){selected=b.optString("id");render();}
 void addDemo(String name,String prompt){JSONObject b=new JSONObject();put(b,"id",UUID.randomUUID().toString());put(b,"name",name);put(b,"prompt",prompt);put(b,"demo",true);put(b,"url","");put(b,"key","");put(b,"model","hermes-agent");put(b,"provider","");put(b,"messages",new JSONArray());bots.put(b);}
 EditText field(LinearLayout l,String label,String value,boolean secret){l.addView(text(label,13,MUTED));EditText e=new EditText(this);e.setTextColor(INK);e.setTextSize(16);e.setText(value);e.setSingleLine(!label.equals("Bot talimatı"));e.setInputType(secret?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT| (label.equals("Bot talimatı")?InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));l.addView(e);return e;}
 void edit(JSONObject existing){
  if(existing!=null && busy.contains(existing.optString("id"))){toast("Bot yanıt verirken bağlantı ve talimat değiştirilemez.");return;}
  JSONObject b=existing==null?new JSONObject():existing;LinearLayout form=column();form.setPadding(dp(20),dp(8),dp(20),dp(16));ScrollView sc=new ScrollView(this);sc.addView(form);
  EditText name=field(form,"Bot adı",b.optString("name"),false),prompt=field(form,"Bot talimatı",b.optString("prompt","Türkçe yanıt veren yardımcı bir asistansın."),false);
  Switch demo=new Switch(this);demo.setText("Demo modu (örnek yanıtlar)");demo.setTextColor(INK);demo.setChecked(b.optBoolean("demo",existing==null));form.addView(demo);
  EditText url=field(form,"Sunucu adresi",b.optString("url"),false);url.setHint("https://hermes.ornek.com/v1");
  EditText key=field(form,"Hermes API anahtarı",b.optString("key"),true),model=field(form,"Model / rota",b.optString("model","hermes-agent"),false),provider=field(form,"Sağlayıcı (isteğe bağlı)",b.optString("provider"),false);
  CheckBox lan=new CheckBox(this);lan.setText("Yerel ağda şifresiz HTTP’ye izin ver");lan.setTextColor(MUTED);lan.setChecked(b.optBoolean("lan"));form.addView(lan);
  form.addView(text("VPS için HTTPS kullanın. HTTP seçilirse API anahtarı ve mesajlar yerel ağda şifrelenmez. Her bot ayrı sohbet tutar; sunucunun dosya ve araç izinlerini paylaşır.",12,MUTED));
  TextView status=text("",13,MINT);form.addView(status);
  form.addView(button("Bağlantıyı test et",()->{final String u=url.getText().toString().trim(),k=key.getText().toString().trim();try{HermesApi.validate(u,k,lan.isChecked());}catch(Exception e){status.setText(e.getMessage());return;}status.setText("Bağlantı kontrol ediliyor…");pool.execute(()->{String result;try{JSONObject r=HermesApi.request(HermesApi.base(u)+"/models",k,"GET",null);r.getJSONArray("data");result="Bağlantı ve kimlik doğrulama başarılı.";}catch(Exception e){result="Bağlantı başarısız: "+safeError(e);}final String s=result;ui.post(()->status.setText(s));});}));
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"Yeni bot":"Botu düzenle").setView(sc).setPositiveButton("Kaydet",null).setNegativeButton("Vazgeç",null).create();dialog.show();
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String n=name.getText().toString().trim(),p=prompt.getText().toString().trim(),u=url.getText().toString().trim(),k=key.getText().toString().trim();
   if(n.isEmpty()||p.isEmpty()){status.setText("Bot adı ve talimat gerekli.");return;}if(!demo.isChecked()){try{HermesApi.validate(u,k,lan.isChecked());}catch(Exception e){status.setText(e.getMessage());return;}}
   if(model.getText().toString().trim().isEmpty()){status.setText("Model / rota gerekli.");return;}
   if(existing==null){put(b,"id",UUID.randomUUID().toString());put(b,"messages",new JSONArray());bots.put(b);}
   boolean changed=existing!=null && (!u.equals(b.optString("url")) || demo.isChecked()!=b.optBoolean("demo"));
   if(changed)put(b,"messages",new JSONArray());
   put(b,"name",n);put(b,"prompt",p);put(b,"url",u);put(b,"key",k);put(b,"demo",demo.isChecked());put(b,"model",model.getText().toString().trim());put(b,"provider",provider.getText().toString().trim());put(b,"lan",lan.isChecked());
   save();dialog.dismiss();openBot(b);
  });
  form.addView(text("Sunucu veya demo modu değişirse bu botun sohbeti temizlenir.",12,MUTED));
 }

 void chat(JSONObject b){
  LinearLayout header=row();header.addView(icon("back","Sohbetlere dön",()->{selected=null;render();}));header.addView(avatar(b,36));LinearLayout details=column();LinearLayout.LayoutParams di=new LinearLayout.LayoutParams(0,-2,1);di.setMargins(dp(10),0,0,0);header.addView(details,di);
  TextView name=text(b.optString("name"),17,INK);name.setTypeface(null,Typeface.BOLD);name.setPadding(0,0,0,dp(2));details.addView(name);TextView status=text(b.optBoolean("demo")?"Demo bot · örnek yanıtlar":busy.contains(b.optString("id"))?"Yanıt yazıyor…":"Hermes",11,MUTED);status.setPadding(0,0,0,0);details.addView(status);header.addView(icon("more","Sohbet seçenekleri",()->options(b)));root.addView(header);gap(root,12);line(root);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);LinearLayout messages=column();scroll.addView(messages);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  JSONArray history=b.optJSONArray("messages");if(history.length()==0){messages.setGravity(Gravity.CENTER);TextView av=avatar(b,72);messages.addView(av);gap(messages,16);TextView hello=text(b.optString("name")+" ile sohbet",23,INK);hello.setGravity(Gravity.CENTER);messages.addView(hello);TextView hint=text("Bir soru sor ya da aklındaki işi anlat.",15,MUTED);hint.setGravity(Gravity.CENTER);messages.addView(hint);gap(messages,24);messages.addView(button("Neler yapabilirsin?",()->{composer.setText("Neler yapabilirsin?");composer.requestFocus();}));gap(messages,10);messages.addView(button("Birlikte bir plan yapalım",()->{composer.setText("Birlikte bir plan yapalım.");composer.requestFocus();}));}
  else{TextView day=text("SOHBET",10,MUTED);day.setGravity(Gravity.CENTER);messages.addView(day);for(int i=0;i<history.length();i++){JSONObject m=history.optJSONObject(i);boolean user=m.optString("role").equals("user");LinearLayout container=column();container.setGravity(user?Gravity.RIGHT:Gravity.LEFT);gap(messages,14);messages.addView(container,new LinearLayout.LayoutParams(-1,-2));LinearLayout bubble=column();bubble.setPadding(dp(14),dp(10),dp(14),dp(10));bubble.setBackground(shape(user?0xff303237:CARD));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,-2);bp.setMargins(user?dp(36):0,0,user?0:dp(24),0);container.addView(bubble,bp);if(!user){TextView author=text(m.optBoolean("error")?"Bağlantı bilgisi":b.optString("name"),12,MUTED);bubble.addView(author);}TextView t=text(m.optString("content"),16,INK);t.setLineSpacing(dp(3),1);t.setTextIsSelectable(true);bubble.addView(t);if(m.optLong("time")>0){TextView time=text(new java.text.SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(m.optLong("time"))),10,MUTED);time.setGravity(Gravity.RIGHT);bubble.addView(time);}}}
  boolean running=busy.contains(b.optString("id"));if(running)messages.addView(text("•••  "+b.optString("name")+" yanıt yazıyor",13,MUTED));gap(messages,18);gap(root,10);
  LinearLayout compose=row();compose.setPadding(dp(6),dp(4),dp(4),dp(4));GradientDrawable border=shape(CARD);border.setStroke(dp(1),0xff37393e);compose.setBackground(border);
  composer=new EditText(this);composer.setTextColor(INK);composer.setHintTextColor(MUTED);composer.setTextSize(16);composer.setHint("Mesaj yaz…");composer.setBackgroundColor(Color.TRANSPARENT);composer.setPadding(dp(12),dp(10),dp(8),dp(10));composer.setMaxLines(5);composer.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);composer.setText(b.optString("draft"));composer.setContentDescription("Mesaj yaz");
  composer.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int before,int count){put(b,"draft",s.toString());}public void afterTextChanged(android.text.Editable e){}});
  compose.addView(composer,new LinearLayout.LayoutParams(0,-2,1));View sendIcon=icon("send","Mesaj gönder",()->send(b));sendIcon.setEnabled(!running);sendIcon.setAlpha(running?.35f:1f);compose.addView(sendIcon);root.addView(compose);if(b.optBoolean("demo")){TextView demo=text("Demo modu · Mesajlar sunucuya gönderilmez",10,MUTED);demo.setGravity(Gravity.CENTER);root.addView(demo);}scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
 }
 void options(JSONObject b){new AlertDialog.Builder(this).setItems(new String[]{"Botu düzenle","Sohbeti paylaş","Sohbeti temizle","Botu sil"},(d,which)->{if(which==0){edit(b);return;}if(which==1){StringBuilder out=new StringBuilder(b.optString("name")+"\n\n");JSONArray h=b.optJSONArray("messages");for(int i=0;i<h.length();i++){JSONObject m=h.optJSONObject(i);out.append(m.optString("role")).append(": ").append(m.optString("content")).append("\n\n");}Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,out.toString());startActivity(Intent.createChooser(share,"Sohbeti paylaş"));return;}if(busy.contains(b.optString("id"))){toast("Önce botun yanıt vermesini bekleyin.");return;}new AlertDialog.Builder(this).setTitle(which==2?"Sohbet temizlensin mi?":"Bot silinsin mi?").setMessage("Telefondaki bu kayıt geri alınamaz.").setNegativeButton("Vazgeç",null).setPositiveButton("Sil",(x,y)->{if(which==2)put(b,"messages",new JSONArray());else{for(int i=0;i<bots.length();i++)if(bots.optJSONObject(i)==b){bots.remove(i);break;}selected=null;}save();render();}).show();}).show();}
 void send(JSONObject b){String input=composer.getText().toString().trim();String id=b.optString("id");if(input.isEmpty()||busy.contains(id))return;
  if(!b.optBoolean("demo")){try{HermesApi.validate(b.optString("url"),b.optString("key"),b.optBoolean("lan"));}catch(Exception e){toast(e.getMessage());return;}}
  JSONObject m=new JSONObject();put(m,"role","user");put(m,"content",input);put(m,"time",System.currentTimeMillis());put(b,"updated",System.currentTimeMillis());put(m,"demo",b.optBoolean("demo"));b.optJSONArray("messages").put(m);put(b,"draft","");if(!save()){b.optJSONArray("messages").remove(b.optJSONArray("messages").length()-1);put(b,"draft",input);return;}
  busy.add(id);render();final JSONObject snapshot;try{snapshot=new JSONObject(b.toString());}catch(Exception e){busy.remove(id);return;}
  final Context context=getApplicationContext();pool.execute(()->{String answer;boolean error=false;try{if(snapshot.optBoolean("demo")){answer="[Demo sohbeti]\n\n"+snapshot.optString("name")+" rolü için mesajını aldım: “"+input+"”\n\nGerçek sonuçlar için Sohbet seçenekleri → Botu düzenle ekranında demo modunu kapatıp bilgisayarındaki Hermes adresini ve API anahtarını gir.";}else answer=HermesApi.chat(snapshot);}catch(Exception e){error=true;answer=safeError(e)+"\n\nİstek otomatik tekrarlanmadı. Sunucuda işlem başlamış olabilir; tekrar göndermeden önce Hermes’i kontrol edin.";}
   final String a=answer;final boolean err=error;ui.post(()->{MainActivity screen=active.get();JSONObject target=null;for(int i=0;i<bots.length();i++){JSONObject x=bots.optJSONObject(i);if(id.equals(x.optString("id")))target=x;}busy.remove(id);if(target!=null){JSONObject r=new JSONObject();try{put(target,"updated",System.currentTimeMillis());r.put("time",System.currentTimeMillis()).put("role","assistant").put("content",a).put("error",err).put("demo",snapshot.optBoolean("demo"));target.getJSONArray("messages").put(r);new BotStore(context).save(bots);}catch(Exception e){if(screen!=null)screen.toast("Yanıt alındı ancak kaydedilemedi.");}}if(screen!=null)screen.render();});
  });
 }
 static String safeError(Exception e){if(e instanceof java.net.SocketTimeoutException)return "Bağlantı zaman aşımına uğradı.";if(e instanceof java.net.ConnectException || e instanceof java.net.UnknownHostException)return "Sunucuya ulaşılamadı. Adresi, ağı ve Hermes API servisinin açık olduğunu kontrol edin.";if(e instanceof javax.net.ssl.SSLException)return "HTTPS sertifikası doğrulanamadı.";if(e instanceof org.json.JSONException)return "Sunucu beklenen Hermes / OpenAI biçiminde yanıt vermedi.";if(e instanceof java.io.IOException)return e.getMessage()==null?"Ağ bağlantısı kesildi.":e.getMessage();return "İşlem tamamlanamadı.";}
 void help(){new AlertDialog.Builder(this).setTitle("Hermes’e bağlan").setMessage("1. Bilgisayardaki Hermes API sunucusunu etkinleştir: API_SERVER_ENABLED=true ve güçlü bir API_SERVER_KEY ayarla; hermes gateway çalıştır.\n\n2. Telefonun bilgisayara erişebilmesi gerekir. Aynı Wi-Fi ağında bilgisayarın yerel IP adresini kullan; localhost telefonun kendisidir. Varsayılan API portu 8642. LAN erişimi için sunucu dinleme adresi ve güvenlik duvarı ayrıca ayarlanmalıdır.\n\n3. Bot oluştur: adres, API anahtarı ve talimat gir. Önce bağlantıyı test et.\n\n4. Hostinger’a geçtiğinde HTTPS adresini kaydet. Anahtarı uygulamaya gir; APK içinde hiçbir anahtar bulunmaz.\n\nBotlar ayrı sohbet ve talimatlardır; bağımsız sunucu süreçleri veya güvenlik alanları değildir. Birden fazla botun isteği eşzamanlı yürüyebilir. Bu sürüm zamanlanmış 7/24 görev içermez. Telefon uygulaması kapanırsa devam eden yanıtın alınması garanti edilmez.\n\nModel seçimi Hermes sunucusundaki sağlayıcı / rota ayarlarına bağlıdır.").setPositiveButton("Anladım",null).show();}
}
