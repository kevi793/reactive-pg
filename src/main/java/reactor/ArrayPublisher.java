package reactor;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicLong;

public class ArrayPublisher<T> implements Publisher<T> {

    private final T[] array;

    public ArrayPublisher(T[] array) {
        this.array = array;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new Subscription() {

            final AtomicLong requested = new AtomicLong(0);
            private int index = 0;
            private boolean completed = false;
            private boolean cancelled = false;
            private boolean inFlight = false;

            @Override
            public void request(long l) {

                if (completed || cancelled) {
                    return;
                }

                if (l <= 0) {
                    cancel();
                    subscriber.onError(new IllegalArgumentException("Positive request amount required but it was " + l));
                    return;
                }

                requested.addAndGet(l);

                if (inFlight) {
                    return;
                }

                inFlight = true;

                while (index < ArrayPublisher.this.array.length && requested.get() > 0) {

                    if (this.cancelled) {
                        return;
                    }

                    T item = ArrayPublisher.this.array[index];

                    if (item == null) {
                        completed = true;
                        subscriber.onError(new NullPointerException());
                        return;
                    }

                    index++;
                    requested.decrementAndGet();
                    subscriber.onNext(item);
                }

                if (index == ArrayPublisher.this.array.length) {
                    subscriber.onComplete();
                    completed = true;
                }

                inFlight = false;
            }

            @Override
            public void cancel() {
                this.cancelled = true;
            }
        });
    }
}
