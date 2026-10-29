package com.eema.backend.config;

import com.eema.backend.service.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationExecutorBindingTest {
    @Configuration @EnableAsync
    @Import({AsyncConfiguration.class,NotificationOutboxDispatcher.class})
    static class Wiring {
        @Bean NotificationOutboxStore store(){return mock(NotificationOutboxStore.class);}
        @Bean EmailService email(){var email=mock(EmailService.class);when(email.enabled()).thenReturn(true);return email;}
        @Bean OneTimeTokenCodec tokens(){return new OneTimeTokenCodec();}
        @Bean NotificationOutboxProperties properties(){return new NotificationOutboxProperties(true,1,8,300,30,3600);}
        @Bean SimpleMeterRegistry meters(){return new SimpleMeterRegistry();}
        @Bean ThreadPoolTaskScheduler taskScheduler(){return new ThreadPoolTaskScheduler();}
    }
    private AnnotationConfigApplicationContext context(){
        var c=new AnnotationConfigApplicationContext();
        c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("qa",Map.of(
            "app.async.core-pool-size",1,"app.async.max-pool-size",1,"app.async.queue-capacity",1)));
        c.register(Wiring.class);c.refresh();return c;
    }
    @Test void actualDispatcherUsesBoundedExecutorWithStandardAlias()throws Exception{
        try(var c=context()){
            var called=new CountDownLatch(1);var thread=new AtomicReference<String>();
            when(c.getBean(NotificationOutboxStore.class).claim()).thenAnswer(i->{thread.set(Thread.currentThread().getName());return Optional.empty();});
            c.getBean(NotificationOutboxDispatcher.class).dispatchAsync(called::countDown);
            assertTrue(called.await(10,TimeUnit.SECONDS));assertTrue(thread.get().startsWith("eema-async-"));
            assertSame(c.getBean("notificationExecutor"),c.getBean("taskExecutor"));
        }
    }
    @Test void saturationHasBoundedQueueAndCallerBackpressure()throws Exception{
        var release=new CountDownLatch(1);var producer=Executors.newSingleThreadExecutor(r->new Thread(r,"qa-producer"));
        try(var c=context()){
            var first=new CountDownLatch(1);var caller=new CountDownLatch(1);var done=new CountDownLatch(3);
            when(c.getBean(NotificationOutboxStore.class).claim()).thenAnswer(i->{
                if(Thread.currentThread().getName().equals("qa-producer"))caller.countDown();else first.countDown();
                assertTrue(release.await(10,TimeUnit.SECONDS));return Optional.empty();
            });
            var worker=c.getBean(NotificationOutboxDispatcher.class);var executor=c.getBean("notificationExecutor",ThreadPoolTaskExecutor.class);
            worker.dispatchAsync(done::countDown);assertTrue(first.await(10,TimeUnit.SECONDS));
            worker.dispatchAsync(done::countDown);assertEquals(1,executor.getThreadPoolExecutor().getQueue().size());
            var saturated=producer.submit(()->worker.dispatchAsync(done::countDown));assertTrue(caller.await(10,TimeUnit.SECONDS));
            assertEquals(1,executor.getActiveCount());assertFalse(saturated.isDone());release.countDown();saturated.get(10,TimeUnit.SECONDS);assertTrue(done.await(10,TimeUnit.SECONDS));
        }finally{release.countDown();producer.shutdownNow();}
    }
}
