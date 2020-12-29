package reactor;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.function.Function;

public class MapPublisher<IN, OUT> implements Publisher<OUT> {

    private final Function<IN, OUT> mapper;
    private final Publisher<IN> parent;

    public MapPublisher(Publisher<IN> parent, Function<IN, OUT> mapper) {
        this.mapper = mapper;
        this.parent = parent;
    }

    @Override
    public void subscribe(Subscriber<? super OUT> subscriber) {
        parent.subscribe(new MapSubscriber<>(mapper, subscriber));
    }

    private static class MapSubscriber<IN, OUT> implements Subscriber<IN>, Subscription {

        private final Function<IN, OUT> mapper;
        private final Subscriber<? super OUT> actual;
        private Subscription subscription;
        private boolean terminated;

        public MapSubscriber(Function<IN, OUT> mapper, Subscriber<? super OUT> actual) {
            this.mapper = mapper;
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            actual.onSubscribe(this);
        }

        @Override
        public void onNext(IN inputValue) {

            if (terminated) {
                return;
            }

            try {
                OUT resultValue = this.mapper.apply(inputValue);
                actual.onNext(resultValue);
            } catch (Exception ex) {
                this.onError(ex);
                this.cancel();
                this.terminated = true;
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (!terminated) {
                actual.onError(throwable);
            }
        }

        @Override
        public void onComplete() {
            if (!terminated) {
                actual.onComplete();
            }
        }

        @Override
        public void request(long l) {
            this.subscription.request(l);
        }

        @Override
        public void cancel() {
            this.subscription.cancel();
        }
    }
}
