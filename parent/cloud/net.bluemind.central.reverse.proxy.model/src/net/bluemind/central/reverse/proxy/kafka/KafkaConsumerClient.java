package net.bluemind.central.reverse.proxy.kafka;

import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import net.bluemind.central.reverse.proxy.kafka.impl.KafkaConsumerClientImpl;

public interface KafkaConsumerClient<K, V> {

	public static <K, V> KafkaConsumerClient<K, V> create(Vertx vertx, Properties props) {
		return new KafkaConsumerClientImpl<>(vertx, props);
	}

	KafkaConsumerClient<K, V> handler(Handler<ConsumerRecord<K, V>> handler);

	KafkaConsumerClient<K, V> batchHandler(Handler<ConsumerRecords<K, V>> batchHandler);

	KafkaConsumerClient<K, V> subscribe(List<String> topics);
}
