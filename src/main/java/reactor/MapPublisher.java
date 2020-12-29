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

    private static class MapSubscriber<IN, OUT> implements Subscriber<IN> {

        private final Function<IN, OUT> mapper;
        private final Subscriber<? super OUT> actual;

        public MapSubscriber(Function<IN, OUT> mapper, Subscriber<? super OUT> actual) {
            this.mapper = mapper;
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            actual.onSubscribe(subscription);
        }

        @Override
        public void onNext(IN inputValue) {
            OUT resultValue = this.mapper.apply(inputValue);
            actual.onNext(resultValue);
        }

        @Override
        public void onError(Throwable throwable) {
            actual.onError(throwable);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }
    }
}
