package com.fatih.hermesbots;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class HermesApiTest {
 @Test public void normalizesBase(){assertEquals("https://example.com/v1",HermesApi.base(" https://example.com/ "));assertEquals("https://example.com/api/v1",HermesApi.base("https://example.com/api/v1/"));}
 @Test public void blocksUnsafeAddresses(){for(String u:new String[]{"ftp://host","https://user:password@host","https://host?token=x","http://8.8.8.8"}){try{HermesApi.validate(u,"key",true);fail(u);}catch(IllegalArgumentException expected){}}}
 @Test public void requiresExplicitLan(){try{HermesApi.validate("http://192.168.1.4:8642","key",false);fail();}catch(IllegalArgumentException expected){}HermesApi.validate("http://192.168.1.4:8642","key",true);}
 @Test public void excludesDemoAndErrors() throws Exception {JSONObject b=new JSONObject("{\"prompt\":\"Research\",\"model\":\"hermes-agent\",\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"},{\"role\":\"assistant\",\"content\":\"fake\",\"demo\":true},{\"role\":\"assistant\",\"content\":\"timeout\",\"error\":true}]}");JSONArray m=HermesApi.payload(b).getJSONArray("messages");assertEquals(2,m.length());assertEquals("Research",m.getJSONObject(0).getString("content"));}
 static class Mock implements AutoCloseable {
  final ServerSocket socket=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1")); volatile String headers="",body="";final Thread worker;
  Mock(int status,String response)throws Exception{worker=new Thread(()->{try(Socket s=socket.accept()){InputStream in=s.getInputStream();ByteArrayOutputStream h=new ByteArrayOutputStream();int ch;while((ch=in.read())!=-1){h.write(ch);if(h.toString("UTF-8").endsWith("\r\n\r\n"))break;}headers=h.toString("UTF-8");int len=0;for(String l:headers.split("\r\n"))if(l.toLowerCase().startsWith("content-length:"))len=Integer.parseInt(l.split(":",2)[1].trim());byte[] b=new byte[len];int off=0;while(off<len){int n=in.read(b,off,len-off);if(n<0)break;off+=n;}body=new String(b,StandardCharsets.UTF_8);byte[] bytes=response.getBytes(StandardCharsets.UTF_8);s.getOutputStream().write(("HTTP/1.1 "+status+" Test\r\nContent-Type: application/json\r\nContent-Length: "+bytes.length+"\r\nLocation: https://example.com/\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));s.getOutputStream().write(bytes);}catch(Exception ignored){}});worker.start();}
  String url(){return "http://127.0.0.1:"+socket.getLocalPort();} public void close()throws Exception{socket.close();worker.join(2000);}
 }
 @Test public void sendsAuthenticatedChatAndReadsResponse() throws Exception {try(Mock m=new Mock(200,"{\"choices\":[{\"message\":{\"content\":\"Merhaba dünya\"}}]}")){JSONObject b=new JSONObject().put("url",m.url()).put("key","test-key").put("model","hermes-agent").put("prompt","Türkçe yanıt ver").put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content","Selam")));assertEquals("Merhaba dünya",HermesApi.chat(b));assertTrue(m.headers.contains("Bearer test-key"));assertTrue(m.body.contains("Türkçe"));}}
 @Test public void rejectsRedirectInsteadOfForwardingSecret() throws Exception {try(Mock m=new Mock(302,"{}")){try{HermesApi.request(m.url()+"/v1/models","secret","GET",null);fail();}catch(IOException e){assertTrue(e.getMessage().contains("302"));}}}
 @Test public void rejectsBadKey() throws Exception {try(Mock m=new Mock(401,"{}")){try{HermesApi.request(m.url()+"/v1/models","wrong","GET",null);fail();}catch(IOException e){assertTrue(e.getMessage().contains("401"));}}}
}
