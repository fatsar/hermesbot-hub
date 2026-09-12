package com.fatih.hermesbots;

import android.content.Context;
import android.security.keystore.*;
import android.util.AtomicFile;
import java.io.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.*;

/** Entire local bot database is encrypted with an Android Keystore AES key. */
final class BotStore {
 private final AtomicFile file;
 BotStore(Context c){file=new AtomicFile(new File(c.getFilesDir(),"bots.enc"));}
 private javax.crypto.SecretKey key() throws Exception {
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(!ks.containsAlias("hermes-bots")){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("hermes-bots",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}
  return (javax.crypto.SecretKey)ks.getKey("hermes-bots",null);
 }
 JSONArray load() throws Exception {
  if(!file.getBaseFile().exists())return new JSONArray();
  byte[] raw=file.readFully(); if(raw.length<29)throw new IOException("Kayıt dosyası eksik.");
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,raw,0,12));
  return new JSONArray(new String(c.doFinal(raw,12,raw.length-12),java.nio.charset.StandardCharsets.UTF_8));
 }
 void save(JSONArray data) throws Exception {
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
  byte[] encrypted=c.doFinal(data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
  FileOutputStream out=null;try{out=file.startWrite();out.write(c.getIV());out.write(encrypted);file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
 }
}
