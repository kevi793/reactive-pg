package reactor;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ArrayPublisher<T> implements Publisher<T> {
    private final T[] array;
    ExecutorService executorService;

    public ArrayPublisher(T[] array) {
        this.array = array;
        this.executorService = Executors.newFixedThreadPool(1);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new Subscription() {
            final AtomicInteger index = new AtomicInteger(0);
            final AtomicLong requested = new AtomicLong(0);
            private final AtomicBoolean completed = new AtomicBoolean(false);
            private final AtomicBoolean cancelled = new AtomicBoolean(false);
            private Throwable error = null;

            private void processRequest() {
                while (index.get() < ArrayPublisher.this.array.length && requested.get() > 0) {

                    if (this.cancelled.get()) {
                        break;
                    }

                    T item = ArrayPublisher.this.array[index.getAndIncrement()];

                    if (item == null) {
                        subscriber.onError(new NullPointerException());
                        return;
                    }

                    subscriber.onNext(item);
                    requested.decrementAndGet();
                }

                if (this.error != null) {
                    subscriber.onError(error);
                    return;
                }

                if (ArrayPublisher.this.array.length == this.index.get()) {
                    completed.set(true);
                    subscriber.onComplete();
                }
            }

            @Override
            public void request(long l) {
                if (completed.get() || cancelled.get()) {
                    return;
                }

                if (l <= 0) {
                    this.error = new IllegalArgumentException("Positive request amount required but it was " + l);
                    cancel();
                    return;
                }

                if (requested.getAndAdd(l) == 0) {
                    executorService.execute(this::processRequest);
                }
            }

            @Override
            public void cancel() {
                this.cancelled.set(true);
            }
        });
    }
}
