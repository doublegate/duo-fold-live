package org.duofold.live;

import android.os.Looper;
import android.os.SystemClock;
import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.*;

/** Shell-owned safety net: EOF survives removal of the app and death of its reader. */
public final class RotationRecoveryProcess implements AutoCloseable {
 private final Process process;
 private RotationRecoveryProcess(Process process){this.process=process;}
 static RotationRecoveryProcess start(String apk,int user)throws Exception {
  if(apk==null||!apk.startsWith("/data/app/")||!apk.endsWith("/base.apk"))
   throw new IOException("Recovery APK path unavailable; rotation hold not applied");
  File copy=File.createTempFile("duofold-rotation-recovery-",".apk",new File("/data/local/tmp"));
  Process child=null;
  ExecutorService reader=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"rotation-recovery-ready");t.setDaemon(true);return t;});
  try {
   // An installed APK disappears during uninstall. Keep a private, read-only copy
   // until the recovery process has finished; no app data or signing keys are copied.
   android.system.Os.chmod(copy.getPath(),0600);
   Files.copy(new File(apk).toPath(),copy.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
   android.system.Os.chmod(copy.getPath(),0400);
   ProcessBuilder builder=new ProcessBuilder("/system/bin/app_process","/system/bin",
    "--nice-name=duo_rotation_recovery",RotationRecoveryProcess.class.getName(),Integer.toString(user),copy.getPath());
   builder.environment().put("CLASSPATH",copy.getPath());
   builder.redirectError(new File("/dev/null"));
   child=builder.start();
   final Process running=child;
   String ready=reader.submit(()->new BufferedReader(new InputStreamReader(running.getInputStream())).readLine()).get(10,TimeUnit.SECONDS);
   if(!"READY".equals(ready)||!child.isAlive())throw new IOException("Rotation recovery helper did not become ready");
   return new RotationRecoveryProcess(child);
  }catch(Exception error){if(child!=null)child.destroyForcibly();copy.delete();throw error;}
  finally{reader.shutdownNow();}
 }
 boolean alive(){return process.isAlive();}
 public void close(){try{process.getOutputStream().close();}catch(IOException ignored){}}
 static void awaitOwnerExit(InputStream input)throws IOException{while(input.read()!=-1){}}
 public static void main(String[] args)throws Exception {
  if(args.length!=2)throw new IllegalArgumentException("Expected user and recovery APK");
  int user=Integer.parseInt(args[0]);
  File copy=new File(args[1]);
  if(user<0||!copy.getCanonicalPath().startsWith("/data/local/tmp/duofold-rotation-recovery-"))throw new IllegalArgumentException("Invalid recovery arguments");
  try {
   Looper.prepareMainLooper();
   // Resolve the restoration APIs before permitting the owner to change settings.
   FoldRotationHold.checkRecoveryBackend(user);
   System.out.println("READY");System.out.flush();
   awaitOwnerExit(System.in); // Pipe closure is independent of uninstall broadcasts.
   // Never steal a live owner's lease. Retry transient display remapping / system
   // service failures, then retain the journal for the existing next-start recovery.
   for(int attempt=0;attempt<300;attempt++){
    try{if(FoldRotationHold.recoverAfterOwnerExit(user))return;}catch(Exception ignored){}
    SystemClock.sleep(2000);
   }
  }finally{copy.delete();}
 }
}
