package reactor;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.LongStream;

public class MapPublisherTest extends PublisherVerification<Long> {

    public MapPublisherTest() {
        super(new TestEnvironment());
    }

    private static Long[] generate(long num) {
        return LongStream.range(0, num >= Integer.MAX_VALUE ? 1000000 : num)
                .boxed()
                .toArray(Long[]::new);
    }

    @Test
    public void shouldWorkCorrectlyIncaseOfError() throws RuntimeException, InterruptedException {
        ArrayPublisher<Long> arrayPublisher = new ArrayPublisher<>(generate(10));
        MapPublisher<Long, String> mapPublisher = new MapPublisher<Long, String>(arrayPublisher, (obj) -> {
            throw new RuntimeException("For Test");
        });

        ArrayList<String> collected = new ArrayList<>();

        Subscriber<String> subscriber = new Subscriber<String>() {

            private boolean done = false;

            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(10);
            }

            @Override
            public void onNext(String s) {
                Assert.fail("Should never be called");
            }

            @Override
            public void onError(Throwable throwable) {
                collected.add("came");
                Assertions.assertEquals(RuntimeException.class, throwable.getClass());
                Assertions.assertFalse(done);
                done = true;
            }

            @Override
            public void onComplete() {
                Assert.fail("Complete should never be called");
            }
        };

        mapPublisher.subscribe(subscriber);
        Thread.sleep(1000);
        Assertions.assertEquals(1, collected.size());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return new MapPublisher<>(new MapPublisher<>(new ArrayPublisher<>(generate(elements)),
                Object::toString), Long::valueOf);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }
}
