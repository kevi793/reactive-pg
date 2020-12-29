package reactor;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
            final AtomicLong requested = new AtomicLong(0);
            private Integer index = 0;
            private boolean completed = false;
            private volatile boolean cancelled = false;
            private Throwable error = null;

            private void processRequest() {
                while (index < ArrayPublisher.this.array.length && requested.get() > 0) {

                    if (this.cancelled) {
                        break;
                    }

                    T item = ArrayPublisher.this.array[index++];

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

                if (ArrayPublisher.this.array.length == this.index) {
                    completed = true;
                    subscriber.onComplete();
                }
            }

            @Override
            public void request(long l) {
                if (completed || cancelled) {
                    return;
                }

                if (l <= 0) {
                    l = 0;
                    this.error = new IllegalArgumentException("Positive request amount required but it was " + l);
                    cancel();
                }

                if (requested.getAndAdd(l) == 0) {
                    executorService.execute(this::processRequest);
                }
            }

            @Override
            public void cancel() {
                this.cancelled = true;
            }
        });
    }
}
