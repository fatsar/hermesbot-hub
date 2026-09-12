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
 private static final int BG=0xff0b1118,CARD=0xff17222e,INK=0xffeff6fc,MUTED=0xffa9bacb,MINT=0xff67e8c1;
 private static JSONArray bots;
 private static final Set<String> busy=new HashSet<>();
 private static final ExecutorService pool=Executors.newFixedThreadPool(4);
 private static final Handler ui=new Handler(Looper.getMainLooper());
 private static java.lang.ref.WeakReference<MainActivity> active=new java.lang.ref.WeakReference<>(null);
 private static boolean storageFailed;
 private BotStore store; private LinearLayout root,body; private String selected;
 private EditText composer;
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
 Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);boolean primary=s.startsWith("＋")||s.startsWith("Gönder");b.setTextColor(primary?BG:INK);b.setBackgroundTintList(null);GradientDrawable bg=shape(primary?MINT:CARD);if(!primary)bg.setStroke(dp(1),0xff344657);b.setBackground(bg);b.setMinHeight(dp(48));b.setPadding(dp(14),dp(4),dp(14),dp(4));b.setOnClickListener(v->r.run());return b;}
 void gap(LinearLayout l,int n){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(n)));}
 JSONObject find(String id){if(id==null)return null;for(int i=0;i<bots.length();i++){JSONObject b=bots.optJSONObject(i);if(id.equals(b.optString("id")))return b;}return null;}
 void put(JSONObject b,String k,Object v){try{b.put(k,v);}catch(JSONException e){throw new IllegalStateException(e);}}
 boolean save(){if(storageFailed)return false;try{store.save(bots);return true;}catch(Exception e){toast("Kayıt başarısız. Depolama alanını kontrol edin.");return false;}}
 void toast(String m){Toast.makeText(this,m,Toast.LENGTH_LONG).show();}
 void render(){
  root=column();root.setBackgroundColor(BG);root.setPadding(dp(20),dp(8),dp(20),dp(12));root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(20)+insets.getSystemWindowInsetLeft(),dp(8)+insets.getSystemWindowInsetTop(),dp(20)+insets.getSystemWindowInsetRight(),dp(12)+insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});setContentView(root);root.requestApplyInsets();
  JSONObject b=find(selected);if(b==null){selected=null;home();}else chat(b);
 }
 void home(){
  textHeader("BOTLUK / MOBİL KONTROL");title(root,"Botların, tek yerde.");
  root.addView(text("Bilgisayarından VPS’e. Her bot için ayrı görev ve sohbet.",15,MUTED));
  LinearLayout row=new LinearLayout(this);row.addView(button("＋ Bot oluştur",()->edit(null)),new LinearLayout.LayoutParams(0,-2,1));row.addView(button("Kurulum",this::help));root.addView(row);
  ScrollView scroll=new ScrollView(this);body=column();scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  if(bots.length()==0){gap(body,26);title(body,"İlk ekibini kur");body.addView(text("Araştırmacı, yazılımcı veya kişisel asistan. Bir rol seç, talimatını yaz ve Hermes bağlantısını ekle.",17,MUTED));gap(body,12);body.addView(button("Demo botlarıyla dene",()->{addDemo("Araştırmacı","Araştırma yap, kaynaklarını belirt ve Türkçe yanıt ver.");addDemo("Yazılım asistanı","Yazılım geliştirmede yardımcı ol. Değişiklikleri açıkla ve Türkçe yanıt ver.");addDemo("Günlük asistan","Planlama ve günlük görevlerde kısa, uygulanabilir Türkçe yanıtlar ver.");save();render();}));}
  for(int i=0;i<bots.length();i++){JSONObject b=bots.optJSONObject(i);LinearLayout card=column();card.setPadding(dp(16),dp(12),dp(16),dp(12));card.setBackground(shape(CARD));gap(body,12);body.addView(card);
   card.addView(text(b.optBoolean("demo")?"● DEMO · Gerçek ajan çalıştırmaz":busy.contains(b.optString("id"))?"● YANIT BEKLENİYOR":"◉ HERMES BAĞLANTISI",11,MINT));
   TextView n=text(b.optString("name"),22,INK);n.setTypeface(null,Typeface.BOLD);card.addView(n);
   TextView p=text(b.optString("prompt"),14,MUTED);p.setMaxLines(2);card.addView(p);
   LinearLayout actions=new LinearLayout(this);actions.addView(button("Sohbeti aç  →",()->{selected=b.optString("id");render();}),new LinearLayout.LayoutParams(0,-2,1));actions.addView(button("Düzenle",()->edit(b)));card.addView(actions);
  }
  root.addView(text(bots.length()+" bot  ·  "+busy.size()+" işlem  ·  v0.1",12,MUTED));
 }
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
   save();dialog.dismiss();render();
  });
  form.addView(text("Sunucu veya demo modu değişirse bu botun sohbeti temizlenir.",12,MUTED));
 }
 void chat(JSONObject b){
  LinearLayout top=new LinearLayout(this);top.addView(button("‹ Botlar",()->{selected=null;render();}),new LinearLayout.LayoutParams(0,-2,1));top.addView(button("Seçenekler",()->options(b)));root.addView(top);
  title(root,b.optString("name"));textHeader(b.optBoolean("demo")?"DEMO · ÖRNEK YANITLAR":"HERMES · "+b.optString("model"));
  ScrollView scroll=new ScrollView(this);LinearLayout messages=column();scroll.addView(messages);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  JSONArray history=b.optJSONArray("messages");if(history.length()==0){gap(messages,24);messages.addView(text("Ne üzerinde çalışalım?",23,INK));messages.addView(text(b.optString("prompt"),16,MUTED));}
  for(int i=0;i<history.length();i++){JSONObject m=history.optJSONObject(i);LinearLayout bubble=column();bubble.setPadding(dp(14),dp(10),dp(14),dp(10));bubble.setBackground(shape(m.optString("role").equals("user")?0xff173b36:CARD));gap(messages,10);messages.addView(bubble);bubble.addView(text(m.optBoolean("error")?"BAĞLANTI BİLGİSİ":m.optString("role").equals("user")?"SEN":b.optString("name"),11,MINT));TextView t=text(m.optString("content"),16,INK);t.setTextIsSelectable(true);bubble.addView(t);}
  boolean running=busy.contains(b.optString("id"));if(running){messages.addView(text("● Bot çalışıyor… Diğer botlara geçebilirsin.",14,MINT));}
  composer=new EditText(this);composer.setTextColor(INK);composer.setHintTextColor(MUTED);composer.setHint("Bir mesaj yaz…");composer.setMaxLines(4);composer.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);composer.setText(b.optString("draft"));
  composer.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int before,int count){put(b,"draft",s.toString());}public void afterTextChanged(android.text.Editable e){}});
  root.addView(composer);Button send=button(running?"Yanıt bekleniyor…":"Gönder  ↑",()->send(b));send.setEnabled(!running);root.addView(send);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
 }
 void options(JSONObject b){new AlertDialog.Builder(this).setItems(new String[]{"Botu düzenle","Sohbeti paylaş","Sohbeti temizle","Botu sil"},(d,which)->{if(which==0){edit(b);return;}if(which==1){StringBuilder out=new StringBuilder(b.optString("name")+"\n\n");JSONArray h=b.optJSONArray("messages");for(int i=0;i<h.length();i++){JSONObject m=h.optJSONObject(i);out.append(m.optString("role")).append(": ").append(m.optString("content")).append("\n\n");}Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,out.toString());startActivity(Intent.createChooser(share,"Sohbeti paylaş"));return;}if(busy.contains(b.optString("id"))){toast("Önce botun yanıt vermesini bekleyin.");return;}new AlertDialog.Builder(this).setTitle(which==2?"Sohbet temizlensin mi?":"Bot silinsin mi?").setMessage("Telefondaki bu kayıt geri alınamaz.").setNegativeButton("Vazgeç",null).setPositiveButton("Sil",(x,y)->{if(which==2)put(b,"messages",new JSONArray());else{for(int i=0;i<bots.length();i++)if(bots.optJSONObject(i)==b){bots.remove(i);break;}selected=null;}save();render();}).show();}).show();}
 void send(JSONObject b){String input=composer.getText().toString().trim();String id=b.optString("id");if(input.isEmpty()||busy.contains(id))return;
  if(!b.optBoolean("demo")){try{HermesApi.validate(b.optString("url"),b.optString("key"),b.optBoolean("lan"));}catch(Exception e){toast(e.getMessage());return;}}
  JSONObject m=new JSONObject();put(m,"role","user");put(m,"content",input);put(m,"demo",b.optBoolean("demo"));b.optJSONArray("messages").put(m);put(b,"draft","");if(!save()){b.optJSONArray("messages").remove(b.optJSONArray("messages").length()-1);put(b,"draft",input);return;}
  busy.add(id);render();final JSONObject snapshot;try{snapshot=new JSONObject(b.toString());}catch(Exception e){busy.remove(id);return;}
  final Context context=getApplicationContext();pool.execute(()->{String answer;boolean error=false;try{if(snapshot.optBoolean("demo")){answer="[Demo yanıtı — Hermes’e gönderilmedi]\n\n"+snapshot.optString("name")+" rolü için mesajını aldım: “"+input+"”\n\nGerçek sonuçlar için Botu düzenle ekranında demo modunu kapatıp bilgisayarındaki Hermes adresini ve API anahtarını gir.";}else answer=HermesApi.chat(snapshot);}catch(Exception e){error=true;answer=safeError(e)+"\n\nİstek otomatik tekrarlanmadı. Sunucuda işlem başlamış olabilir; tekrar göndermeden önce Hermes’i kontrol edin.";}
   final String a=answer;final boolean err=error;ui.post(()->{MainActivity screen=active.get();JSONObject target=null;for(int i=0;i<bots.length();i++){JSONObject x=bots.optJSONObject(i);if(id.equals(x.optString("id")))target=x;}busy.remove(id);if(target!=null){JSONObject r=new JSONObject();try{r.put("role","assistant").put("content",a).put("error",err).put("demo",snapshot.optBoolean("demo"));target.getJSONArray("messages").put(r);new BotStore(context).save(bots);}catch(Exception e){if(screen!=null)screen.toast("Yanıt alındı ancak kaydedilemedi.");}}if(screen!=null)screen.render();});
  });
 }
 static String safeError(Exception e){if(e instanceof java.net.SocketTimeoutException)return "Bağlantı zaman aşımına uğradı.";if(e instanceof java.net.ConnectException || e instanceof java.net.UnknownHostException)return "Sunucuya ulaşılamadı. Adresi, ağı ve Hermes API servisinin açık olduğunu kontrol edin.";if(e instanceof javax.net.ssl.SSLException)return "HTTPS sertifikası doğrulanamadı.";if(e instanceof org.json.JSONException)return "Sunucu beklenen Hermes / OpenAI biçiminde yanıt vermedi.";if(e instanceof java.io.IOException)return e.getMessage()==null?"Ağ bağlantısı kesildi.":e.getMessage();return "İşlem tamamlanamadı.";}
 void help(){new AlertDialog.Builder(this).setTitle("Hermes’e bağlan").setMessage("1. Bilgisayardaki Hermes API sunucusunu etkinleştir: API_SERVER_ENABLED=true ve güçlü bir API_SERVER_KEY ayarla; hermes gateway çalıştır.\n\n2. Telefonun bilgisayara erişebilmesi gerekir. Aynı Wi-Fi ağında bilgisayarın yerel IP adresini kullan; localhost telefonun kendisidir. Varsayılan API portu 8642. LAN erişimi için sunucu dinleme adresi ve güvenlik duvarı ayrıca ayarlanmalıdır.\n\n3. Bot oluştur: adres, API anahtarı ve talimat gir. Önce bağlantıyı test et.\n\n4. Hostinger’a geçtiğinde HTTPS adresini kaydet. Anahtarı uygulamaya gir; APK içinde hiçbir anahtar bulunmaz.\n\nBotlar ayrı sohbet ve talimatlardır; bağımsız sunucu süreçleri veya güvenlik alanları değildir. Birden fazla botun isteği eşzamanlı yürüyebilir. Bu sürüm zamanlanmış 7/24 görev içermez. Telefon uygulaması kapanırsa devam eden yanıtın alınması garanti edilmez.\n\nModel seçimi Hermes sunucusundaki sağlayıcı / rota ayarlarına bağlıdır.").setPositiveButton("Anladım",null).show();}
}
