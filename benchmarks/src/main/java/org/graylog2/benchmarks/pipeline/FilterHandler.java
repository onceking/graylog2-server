package org.graylog2.benchmarks.pipeline;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.EventHandler;
import org.graylog2.benchmarks.utils.TimeCalculator;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.graylog2.benchmarks.utils.BusySleeper.consumeCpuFor;

public class FilterHandler implements EventHandler<Event> {

    private final MetricRegistry metricRegistry;
    private final OutputBuffer outputBuffer;
    private final TimeCalculator timeCalculator;
    private final int ordinal;
    private final int numHandler;
    private final Meter processed;
    private final Timer filterTime;

    @AssistedInject
    public FilterHandler(MetricRegistry metricRegistry,
                         @Assisted OutputBuffer outputBuffer,
                         @Assisted TimeCalculator timeCalculator,
                         @Assisted("ordinal") int ordinal,
                         @Assisted("numHandler") int numHandler) {
        this.metricRegistry = metricRegistry;
        this.outputBuffer = outputBuffer;
        this.timeCalculator = timeCalculator;
        this.ordinal = ordinal;
        this.numHandler = numHandler;
        processed = metricRegistry.meter(metricName("processed"));
        filterTime = metricRegistry.timer(metricName("timer"));
    }

    private String metricName(String suffix) {
        return "filter-handler" + ordinal + "." + suffix;
    }

    @Override
    public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
        if ((sequence % numHandler) != ordinal) {
            return;
        }
        final Timer.Context context = filterTime.time();

        consumeCpuFor(timeCalculator.sleepTimeNsForThread(ordinal), NANOSECONDS);

        outputBuffer.publish(event.message);
        processed.mark();

        context.stop();
    }

    public interface Factory {
        FilterHandler create(OutputBuffer outputBuffer, TimeCalculator timeCalculator, @Assisted("ordinal") int ordinal, @Assisted("numHandler") int numHandler);
    }
}