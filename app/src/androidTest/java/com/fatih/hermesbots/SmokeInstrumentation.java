package com.fatih.hermesbots;
import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.EditText;
import org.json.*;

public class SmokeInstrumentation extends Instrumentation {
 private Bundle args;
 @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
 private void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 private EditText input(View v){if(v instanceof EditText)return (EditText)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){EditText e=input(g.getChildAt(i));if(e!=null)return e;}}return null;}
 @Override public void onStart(){Bundle out=new Bundle();try{
  MainActivity a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  final JSONObject[] bot={null};
  runOnMainSync(()->{a.addDemo("Test botu","Türkçe yanıt ver.");a.save();a.render();});
  JSONArray data=new BotStore(getTargetContext()).load();bot[0]=data.getJSONObject(data.length()-1);String id=bot[0].getString("id");
  check(bot[0].getString("name").equals("Test botu"),"Encrypted save/load failed");
  runOnMainSync(()->{JSONObject b=a.find(id);a.openBot(b);input(a.getWindow().getDecorView()).setText("Merhaba Hermes");a.send(b);});
  for(int n=0;n<50;n++){Thread.sleep(100);data=new BotStore(getTargetContext()).load();JSONObject b=data.getJSONObject(data.length()-1);if(b.getJSONArray("messages").length()==2)break;}
  JSONArray messages=data.getJSONObject(data.length()-1).getJSONArray("messages");check(messages.length()==2,"Demo request/response not persisted");check(messages.getJSONObject(1).getBoolean("demo"),"Demo not labelled");
  byte[] raw=new java.io.File(getTargetContext().getFilesDir(),"bots.enc").exists()?new android.util.AtomicFile(new java.io.File(getTargetContext().getFilesDir(),"bots.enc")).readFully():new byte[0];check(!new String(raw,java.nio.charset.StandardCharsets.UTF_8).contains("Merhaba Hermes"),"Plaintext leaked to disk");
  if(args!=null && args.containsKey("api_key")){
   runOnMainSync(()->{JSONObject b=a.find(id);a.put(b,"demo",false);a.put(b,"url","http://10.0.2.2:18642");a.put(b,"lan",true);a.put(b,"key",args.getString("api_key"));a.put(b,"prompt","Do not use any tools. This is a connectivity test.");a.put(b,"messages",new JSONArray());a.openBot(b);input(a.getWindow().getDecorView()).setText("Reply with exactly HERMES_BOTS_ANDROID_OK. Do not use tools or perform any actions.");a.send(b);});
   for(int n=0;n<1800;n++){Thread.sleep(100);data=new BotStore(getTargetContext()).load();if(data.getJSONObject(data.length()-1).getJSONArray("messages").length()==2)break;}
   JSONObject reply=data.getJSONObject(data.length()-1).getJSONArray("messages").getJSONObject(1);check(!reply.optBoolean("error") && reply.getString("content").contains("HERMES_BOTS_ANDROID_OK"),"Real Hermes Android reply failed");
   runOnMainSync(()->{JSONObject b=a.find(id);a.put(b,"key","");a.put(b,"url","");a.put(b,"demo",true);a.put(b,"messages",new JSONArray());a.save();});
  }
  runOnMainSync(()->{a.render();a.onBackPressed();});waitForIdleSync();
  out.putString("stream","PASS: launch, create bot, encrypted persistence, demo send/receive, response persistence, navigation.\n");finish(Activity.RESULT_OK,out);
 }catch(Throwable e){out.putString("stream","FAIL: "+e);finish(Activity.RESULT_CANCELED,out);}}
}
