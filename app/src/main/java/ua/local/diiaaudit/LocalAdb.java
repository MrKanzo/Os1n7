package ua.local.diiaaudit;

import android.content.Context;
import android.os.Build;
import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbStream;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/** Pocket Audit 2.8 local-only ADB client. Never exposes arbitrary shell commands. */
final class LocalAdb extends AbsAdbConnectionManager {
 private final PrivateKey key;
 private final Certificate cert;
 LocalAdb(Context context) throws Exception {
  setApi(Build.VERSION.SDK_INT);setTimeout(12,TimeUnit.SECONDS);setThrowOnUnauthorised(true);
  File file=new File(context.getNoBackupFilesDir(),"local-adb-identity.bin");
  if(file.exists()) {
   try(DataInputStream in=new DataInputStream(new FileInputStream(file))){
    int n=in.readInt();if(n<1||n>16384)throw new IOException("Invalid key length");byte[] b=new byte[n];in.readFully(b);
    key=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(b));
    cert=CertificateFactory.getInstance("X.509").generateCertificate(in);
   }
  } else {
   KeyPairGenerator gen=KeyPairGenerator.getInstance("RSA");gen.initialize(2048);KeyPair kp=gen.generateKeyPair();key=kp.getPrivate();
   X500Name name=new X500Name("CN=OS1N7 Pocket Audit Local ADB");long now=System.currentTimeMillis();
   cert=new JcaX509CertificateConverter().getCertificate(new JcaX509v3CertificateBuilder(name,new BigInteger(128,new SecureRandom()).add(BigInteger.ONE),new Date(now-86400000L),new Date(now+3650L*86400000L),name,kp.getPublic()).build(new JcaContentSignerBuilder("SHA256withRSA").build(key)));
   android.util.AtomicFile atomic=new android.util.AtomicFile(file);FileOutputStream raw=atomic.startWrite();try{DataOutputStream out=new DataOutputStream(raw);byte[] b=key.getEncoded();out.writeInt(b.length);out.write(b);out.write(cert.getEncoded());out.flush();atomic.finishWrite(raw);}catch(Exception e){atomic.failWrite(raw);throw e;}
  }
 }
 @Override protected PrivateKey getPrivateKey(){return key;}
 @Override protected Certificate getCertificate(){return cert;}
 @Override protected String getDeviceName(){return "OS1N7-PocketAudit-local";}
 static String localHost(String text) throws Exception {
  String host=text.trim();if(host.isEmpty()||host.equals("127.0.0.1"))return "127.0.0.1";
  if(!host.matches("[0-9.]{7,15}"))throw new IOException("Введи IPv4 адресу цього телефона");
  InetAddress addr=InetAddress.getByName(host);if(!(addr instanceof Inet4Address)||NetworkInterface.getByInetAddress(addr)==null)throw new IOException("Дозволена тільки адреса цього телефона");return addr.getHostAddress();
 }
 static String pkg(String p){if(!p.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"))throw new IllegalArgumentException("Invalid package");return p;}
 private String run(String command) throws Exception {
  String marker="__POCKET_EXIT__";String result;
  try(AdbStream stream=openStream("shell:"+command+"; echo "+marker+"$?"); InputStream in=stream.openInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1){if(out.size()+n>16384)throw new IOException("Завелика відповідь");out.write(b,0,n);String s=out.toString("UTF-8");if(s.matches("(?s).*"+marker+"[0-9]+\\s*"))break;}result=out.toString("UTF-8");
  }
  int at=result.lastIndexOf(marker);if(at<0||!result.substring(at+marker.length()).trim().equals("0"))throw new IOException("Android відхилив команду: "+result.trim());return result.substring(0,at).trim();
 }
 boolean chain() throws Exception {String s=run("cmd connectivity get-chain3-enabled");if(s.equals("chain:enabled"))return true;if(s.equals("chain:disabled"))return false;throw new IOException("Невідомий статус: "+s);}
 boolean denied(String p) throws Exception {p=pkg(p);String s=run("cmd connectivity get-package-networking-enabled "+p);if(s.equals(p+":deny"))return true;if(s.equals(p+":allow"))return false;throw new IOException("Невідома відповідь: "+s);}
 void enableChain() throws Exception {run("cmd connectivity set-chain3-enabled true");if(!chain())throw new IOException("Ланцюг не активовано");}
 void setDenied(String p,boolean block) throws Exception {p=pkg(p);run("cmd connectivity set-package-networking-enabled "+(!block)+" "+p);if(denied(p)!=block)throw new IOException("Правило не підтверджено: "+p);}
}
