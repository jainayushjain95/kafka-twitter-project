package kafka;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerDemoWithKeys {

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		
		final Logger logger = LoggerFactory.getLogger(ProducerDemoWithKeys.class);
		
		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		
		KafkaProducer<String , String> producer = new KafkaProducer<String, String>(properties);
		final int[] ps = new int[3];
		for(int i = 0;i < 10; i++) {
			String key = "id_" + i;
			ProducerRecord<String, String> record = new ProducerRecord<String, String>("firstTopic", key, "hello from: " + i);
			
			logger.info("\nKey: " + key + "\n");
			
			producer.send(record, new Callback() {
				
				public void onCompletion(RecordMetadata metadata, Exception exception) {
					if(exception == null) {
						logger.info("Received new metadata" + 
					"\nTopic: " + metadata.topic() + "\nPartition: " + metadata.partition() + "\nOffset: " + metadata.offset() + "\nTimestamp: " + metadata.timestamp());
						ps[metadata.partition()]++;
					} else {
						logger.error("Error while producing", exception);
					}
				}
			}).get();	
		}
		producer.flush();
		System.out.println("\n\n\n\n#################\n");
		for(int x : ps) {
			System.out.println(x);
		}
	}

}
