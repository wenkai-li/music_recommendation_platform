package com.neu;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

/**
 * Created by hadoop on 2018/9/30.
 */
public class CurrentTimestampExtractor implements TimestampExtractor {
    @Override
    public long extract(ConsumerRecord<Object, Object> record, long previousTimestamp) {
        long timestamp = record.timestamp();
        if (timestamp < 0) {
            return System.currentTimeMillis();
        } else {
            return timestamp;
        }
    }
}