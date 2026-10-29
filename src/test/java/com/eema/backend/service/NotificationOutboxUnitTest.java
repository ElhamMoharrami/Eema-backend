package com.eema.backend.service;

import com.eema.backend.config.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.sun.net.httpserver.HttpServer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationOutboxUnitTest {
    AppProperties props(){return new AppProperties(new AppProperties.JwtProperties("synthetic-local-test-key-at-least-32-bytes",15,30),null,null,null,
        new AppProperties.ResendProperties(true,"synthetic-local-provider-key","qa@example.test"),null,null,"http://localhost",null);}

    @Test void tokenCanBeReconstructedFromNonceOnlyWithRootKeyAndHashRemainsOneWay(){
        var a=new OneTimeTokenCodec(props());var b=new OneTimeTokenCodec(props());String raw=a.generate(),nonce=a.nonce(raw);
        assertEquals(raw,b.reconstruct(nonce));assertNotEquals(raw,new OneTimeTokenCodec().reconstruct(nonce));
        assertFalse(a.hash(raw).contains(nonce));assertEquals(64,a.hash(raw).length());assertNotEquals(raw,a.generate());
        assertNotEquals(a.hash(nonce),a.hash(raw));assertThrows(IllegalArgumentException.class,()->a.nonce(nonce+".tampered"));
        // Existing opaque tokens remain redeemable through unchanged hash lookup after upgrade.
        assertEquals(64,a.hash("pre-outbox-opaque-token").length());
    }
    @Test void templateCaptureHasNoProviderAndClearsAfterCompositionFailure(){
        var e=new EmailService(props());assertThrows(IllegalStateException.class,()->e.capture(()->{throw new IllegalStateException("fixture");}));
        assertFalse(e.isCapturing());assertEquals(1,e.capture(()->e.send("qa@example.test","s","t","h")).size());
        assertThrows(IllegalStateException.class,()->e.send("qa@example.test","s","t","h"));
    }
    @Test void retryBackoffIsBounded(){
        var p=new NotificationOutboxProperties(true,20,8,300,30,3600);
        assertEquals(30,p.backoffSeconds(1));assertEquals(60,p.backoffSeconds(2));assertEquals(3600,p.backoffSeconds(20));
    }
    @Test void schedulerNeverQueuesAnotherBatchWhileOneIsOutstanding(){
        var dispatcher=mock(NotificationOutboxDispatcher.class);var job=new NotificationOutboxJob(dispatcher);
        job.dispatchDue();job.dispatchDue();job.dispatchDue();var completion=org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(dispatcher).dispatchAsync(completion.capture());completion.getValue().run();job.dispatchDue();verify(dispatcher,times(2)).dispatchAsync(any());
    }
    @ParameterizedTest @CsvSource({"200,unused,false","422,validation_error,true","429,rate_limit_exceeded,false","503,application_error,false","409,invalid_idempotent_request,true","409,concurrent_idempotent_requests,false","408,request_timeout,false"})
    void providerHttpClassificationAndStableIdempotencyKey(int status,String errorName,boolean permanent)throws Exception{
        var calls=new ArrayList<String>();var bodies=new ArrayList<String>();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/emails",exchange->{
            calls.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));bodies.add(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] response=(status==200?"{\"id\":\"qa-provider-id\"}":"{\"name\":\""+errorName+"\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status,response.length);exchange.getResponseBody().write(response);exchange.close();
        });server.start();
        try{
            var e=new EmailService(props(),URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/emails"));var id=UUID.randomUUID();
            var draft=new EmailService.Draft("recipient@example.test","Existing subject","Existing text","<p>Existing text</p>");
            for(int i=0;i<2;i++){
                if(status==200)e.deliver(id,e.sender(),draft);
                else assertEquals(permanent,assertThrows(EmailService.DeliveryFailure.class,()->e.deliver(id,e.sender(),draft)).permanent());
            }
            assertEquals(List.of("eema-notification-"+id,"eema-notification-"+id),calls);assertEquals(bodies.getFirst(),bodies.getLast());
            TransactionSynchronizationManager.setActualTransactionActive(true);
            try{assertThrows(IllegalStateException.class,()->e.deliver(id,e.sender(),draft));}finally{TransactionSynchronizationManager.setActualTransactionActive(false);}
            assertEquals(2,calls.size());
        }finally{server.stop(0);}
    }
}
