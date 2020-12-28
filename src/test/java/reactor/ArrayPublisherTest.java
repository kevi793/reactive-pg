package reactor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

public class ArrayPublisherTest extends PublisherVerification<Long> {

    public static final int LATCH_TIMEOUT = 1000;

    public ArrayPublisherTest() {
        super(new TestEnvironment());
    }

    private static Long[] generate(long num) {
        return LongStream.range(0, num >= Integer.MAX_VALUE ? 1000000 : num)
                .boxed()
                .toArray(Long[]::new);
    }

    @Test
    public void everyMethodInSubscriberShouldBeExecutedInParticularOrder() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<String> observedSignals = new ArrayList<>();
        ArrayPublisher<Long> arrayPublisher = new ArrayPublisher<>(ArrayPublisherTest.generate(5));
        arrayPublisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                observedSignals.add("onSubscribe()");
                subscription.request(10);
            }

            @Override
            public void onNext(Long aLong) {
                observedSignals.add("onNext(" + aLong + ")");
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
                observedSignals.add("onComplete()");
                latch.countDown();
            }
        });

        Assertions.assertTrue(latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertThat(observedSignals, contains("onSubscribe()", "onNext(0)", "onNext(1)", "onNext(2)", "onNext(3)",
                "onNext(4)", "onComplete()"));

    }

    @Test
    public void mustSupportBackpressureControl() throws InterruptedException {
        ArrayList<Long> collected = new ArrayList<>();
        long toRequest = 5;
        Long[] array = generate(toRequest);
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);
        Subscription[] subscription = new Subscription[1];
        CountDownLatch countDownLatch = new CountDownLatch(1);

        publisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscription[0] = s;
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {
                System.out.println("OnComplete");
                countDownLatch.countDown();
            }
        });

        Assertions.assertEquals(0, collected.size());

        subscription[0].request(1);
        assertThat(collected, contains(0L));

        subscription[0].request(1);
        assertThat(collected, contains(0L, 1L));

        subscription[0].request(1);
        assertThat(collected, contains(0L, 1L, 2L));

        subscription[0].request(1);
        assertThat(collected, contains(0L, 1L, 2L, 3L));

        subscription[0].request(20);

        subscription[0].request(20);

        Assertions.assertTrue(countDownLatch.await(1000, TimeUnit.MILLISECONDS));

        assertThat(collected, contains(array));
    }

    @Test
    public void mustSendNPENormally() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Long[] array = new Long[]{null};
        AtomicReference<Throwable> error = new AtomicReference<>();
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);

        publisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(4);
            }

            @Override
            public void onNext(Long aLong) {

            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                latch.countDown();
            }

            @Override
            public void onComplete() {

            }
        });

        Assertions.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        Assertions.assertNotNull(error.get());
    }

    @Test
    public void shouldNotDieInStackOverflow() {
        CountDownLatch latch = new CountDownLatch(1);
        long toRequest = 1;
        Long[] array = generate(toRequest);
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);

        publisher.subscribe(new Subscriber<Long>() {

            private Subscription s;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.s = subscription;
                subscription.request(toRequest);
            }

            @Override
            public void onNext(Long aLong) {
                s.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
    }

    @Test
    public void shouldBePossibleToCancelSubscriptionTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        long toRequest = 1;
        Long[] array = generate(toRequest);
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);

        publisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.cancel();
                subscription.request(10);
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        Assertions.assertFalse(latch.await(1000, TimeUnit.MILLISECONDS));
        Assertions.assertEquals(0, collected.size());
    }

    @Test
    public void t() throws Throwable {
        this.required_createPublisher3MustProduceAStreamOfExactly3Elements();
    }


    @Override
    public Publisher<Long> createPublisher(long l) {
        return new ArrayPublisher<>(generate(l));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }
}
