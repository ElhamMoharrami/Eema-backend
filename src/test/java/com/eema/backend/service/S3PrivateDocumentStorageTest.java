package com.eema.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.*;
import software.amazon.awssdk.core.sync.RequestBody;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3PrivateDocumentStorageTest {
 final S3Client client=mock(S3Client.class);
 final S3PrivateDocumentStorage storage=new S3PrivateDocumentStorage(client,"qa-private-documents","private-finance");
 @Test void privateBucketOpaqueKeyConditionalWriteAndExactBytes(){
  byte[] bytes={37,80,68,70};storage.store("receipts/qa.pdf",bytes);
  var request=org.mockito.ArgumentCaptor.forClass(PutObjectRequest.class);var body=org.mockito.ArgumentCaptor.forClass(RequestBody.class);verify(client).putObject(request.capture(),body.capture());assertEquals("qa-private-documents",request.getValue().bucket());assertEquals("private-finance/receipts/qa.pdf",request.getValue().key());assertEquals("application/pdf",request.getValue().contentType());assertEquals("*",request.getValue().ifNoneMatch());assertTrue(request.getValue().acl()==null||request.getValue().acl()==ObjectCannedACL.UNKNOWN_TO_SDK_VERSION);
  when(client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(),bytes));assertArrayEquals(bytes,storage.load("receipts/qa.pdf"));verify(client).getObjectAsBytes(argThat((GetObjectRequest r)->r.bucket().equals("qa-private-documents")&&r.key().equals("private-finance/receipts/qa.pdf")));
 }
 @ParameterizedTest @ValueSource(strings={"../outside.pdf","/absolute.pdf","receipts/../../outside.pdf","receipts/not-a-pdf.txt"})
 void invalidKeysNeverCallProvider(String ref){assertThrows(IllegalArgumentException.class,()->storage.store(ref,new byte[]{1}));assertThrows(IllegalArgumentException.class,()->storage.load(ref));verifyNoInteractions(client);doCallRealMethod().when(client).deleteObject(any(java.util.function.Consumer.class));assertThrows(IllegalArgumentException.class,()->storage.delete(ref));verify(client,never()).deleteObject(any(DeleteObjectRequest.class));}
 @Test void failedConditionalStorePropagatesWithoutFallbackOverwrite(){when(client.putObject(any(PutObjectRequest.class),any(RequestBody.class))).thenThrow(S3Exception.builder().statusCode(412).message("QA exists").build());assertThrows(S3Exception.class,()->storage.store("receipts/qa.pdf",new byte[]{1}));verify(client,times(1)).putObject(any(PutObjectRequest.class),any(RequestBody.class));verifyNoMoreInteractions(client);}
}
