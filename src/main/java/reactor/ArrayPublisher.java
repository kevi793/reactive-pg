package reactor;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ArrayPublisher<T> implements Publisher<T> {

    private final T[] array;

    public ArrayPublisher(T[] array) {
        this.array = array;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new Subscription() {

            private int index = 0;
            private boolean completed = false;

            @Override
            public void request(long l) {
                if (!completed) {
                    while (index < ArrayPublisher.this.array.length && l > 0) {

                        T item = ArrayPublisher.this.array[index];

                        if (item == null) {
                            completed = true;
                            subscriber.onError(new NullPointerException());
                            return;
                        }

                        index++;
                        l--;
                        subscriber.onNext(item);
                    }

                    if (index == ArrayPublisher.this.array.length) {
                        subscriber.onComplete();
                        completed = true;
                    }
                }
            }

            @Override
            public void cancel() {

            }
        });
    }
}
