package com.fatih.hermesbots;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** OpenAI-compatible Hermes transport. No automatic retries: a turn may run tools. */
public final class HermesApi {
 public static String base(String value) {
  String s=value.trim().replaceAll("/+$", "");
  URI u;
  try { u=URI.create(s); } catch(Exception e) { throw new IllegalArgumentException("Geçerli bir sunucu adresi girin."); }
  if(u.getHost()==null || u.getUserInfo()!=null || u.getQuery()!=null || u.getFragment()!=null || !("https".equals(u.getScheme()) || "http".equals(u.getScheme()))) throw new IllegalArgumentException("Adres http:// veya https:// ile başlamalı; anahtar içermemeli.");
  return s.endsWith("/v1") ? s : s+"/v1";
 }
 public static boolean localHttp(String value) {
  URI u=URI.create(base(value)); String h=u.getHost();
  return "http".equals(u.getScheme()) && (h.equals("localhost") || h.equals("127.0.0.1") || h.equals("10.0.2.2") || h.startsWith("192.168.") || h.startsWith("10.") || h.matches("172\\.(1[6-9]|2[0-9]|3[01])\\..+"));
 }
 public static void validate(String value, String key, boolean lanAllowed) {
  String b=base(value);
  if(b.startsWith("http:") && !(lanAllowed && localHttp(value))) throw new IllegalArgumentException("HTTPS kullanın. Yerel ağ HTTP bağlantısı için izin kutusunu açın.");
  if(key.trim().isEmpty()) throw new IllegalArgumentException("Hermes API anahtarını girin.");
  if(key.contains("\n") || key.contains("\r")) throw new IllegalArgumentException("API anahtarı tek satır olmalı.");
 }
 public static JSONObject request(String url,String key,String method,JSONObject body) throws Exception {
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
  c.setInstanceFollowRedirects(false); c.setConnectTimeout(15000); c.setReadTimeout(180000);
  c.setRequestMethod(method); c.setRequestProperty("Authorization","Bearer "+key); c.setRequestProperty("Accept","application/json");
  try {
   if(body!=null) { c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json; charset=utf-8"); try(OutputStream out=c.getOutputStream()){ out.write(body.toString().getBytes(StandardCharsets.UTF_8)); } }
   int status=c.getResponseCode();
   if(status<200 || status>=300) throw new IOException(status==401 || status==403 ? "Erişim reddedildi. API anahtarını kontrol edin ("+status+")." : "Sunucu HTTP "+status+" döndürdü. İstek otomatik tekrarlanmadı.");
   try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
    byte[] buf=new byte[8192]; int n; while((n=in.read(buf))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("Sunucu yanıtı çok büyük.");out.write(buf,0,n);}
    return new JSONObject(out.toString("UTF-8"));
   }
  } finally { c.disconnect(); }
 }
 public static JSONObject payload(JSONObject bot) throws JSONException {
  JSONArray messages=new JSONArray(); messages.put(new JSONObject().put("role","system").put("content",bot.getString("prompt")));
  JSONArray h=bot.getJSONArray("messages");
  for(int i=0;i<h.length();i++){JSONObject m=h.getJSONObject(i);if(!m.optBoolean("error") && !m.optBoolean("demo")) messages.put(new JSONObject().put("role",m.getString("role")).put("content",m.getString("content")));}
  JSONObject body=new JSONObject().put("model",bot.optString("model","hermes-agent")).put("messages",messages).put("stream",false);
  if(!bot.optString("provider").trim().isEmpty()) body.put("provider",bot.getString("provider"));
  return body;
 }
 public static String chat(JSONObject bot) throws Exception {
  JSONObject result=request(base(bot.getString("url"))+"/chat/completions",bot.getString("key"),"POST",payload(bot));
  String answer=result.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","");
  if(answer.trim().isEmpty())throw new IOException("Sunucu boş yanıt döndürdü. Hermes günlüğünü kontrol edin.");
  return answer;
 }
}
