package com.eema.backend.service;

import java.nio.file.*;
import java.util.concurrent.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PrivateDocumentImmutabilityTest {
 @TempDir Path directory;
 @Test void existingReferenceCannotBeOverwritten(){var storage=new LocalPrivateDocumentStorage(directory.toString());storage.store("receipts/qa.pdf",new byte[]{1,2,3});assertThrows(IllegalStateException.class,()->storage.store("receipts/qa.pdf",new byte[]{9,8,7}));assertArrayEquals(new byte[]{1,2,3},storage.load("receipts/qa.pdf"));}
 @Test void simultaneousStoreHasOneWinnerAndLeavesNoTemporaryFile()throws Exception{
  var storage=new LocalPrivateDocumentStorage(directory.toString());var gate=new CyclicBarrier(2);var pool=Executors.newFixedThreadPool(2);
  try{var tasks=new ArrayList<Future<Boolean>>();for(byte value:new byte[]{1,2})tasks.add(pool.submit(()->{gate.await(5,TimeUnit.SECONDS);try{storage.store("receipts/qa.pdf",new byte[]{value});return true;}catch(IllegalStateException e){return false;}}));int success=0;for(var t:tasks)if(t.get(10,TimeUnit.SECONDS))success++;assertEquals(1,success);assertEquals(1,storage.load("receipts/qa.pdf").length);try(var files=Files.list(directory.resolve("receipts"))){assertEquals(1,files.count());}}finally{pool.shutdownNow();}
 }
}
