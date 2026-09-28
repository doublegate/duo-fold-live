package org.duofold.live;
import org.junit.Test;
import java.io.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;
public class RotationRecoveryProcessTest {
 @Test public void ownerDisconnectEndsWait()throws Exception {
  PipedInputStream input=new PipedInputStream();
  PipedOutputStream owner=new PipedOutputStream(input);
  ExecutorService executor=Executors.newSingleThreadExecutor();
  try {
   Future<?> waiting=executor.submit(()->{try{RotationRecoveryProcess.awaitOwnerExit(input);}catch(IOException e){throw new RuntimeException(e);}});
   owner.write(1);owner.flush();
   try{waiting.get(50,TimeUnit.MILLISECONDS);fail("A live owner must not trigger restoration");}catch(TimeoutException expected){}
   owner.close();
   waiting.get(2,TimeUnit.SECONDS);
  }finally{owner.close();input.close();executor.shutdownNow();}
 }
 @Test public void alreadyDeadOwnerDoesNotRequireUninstallBroadcast()throws Exception {
  RotationRecoveryProcess.awaitOwnerExit(new ByteArrayInputStream(new byte[0]));
 }
}
