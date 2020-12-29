package reactor;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

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
