package main.project;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import main.project.constants.Credentials;
import main.project.constants.GeneralConstants;

public class TwitterProducer {

	Logger logger = LoggerFactory.getLogger(TwitterProducer.class);
	
	public TwitterProducer() {

	}

	public static void main(String[] args) {
		new TwitterProducer().run();
	}

	public void run() {
		BlockingQueue<String> msgQueue = new LinkedBlockingDeque<String>(1000);
		Client hosebirdClient = getTwitterClient(msgQueue);
		hosebirdClient.connect();
		
		KafkaProducer<String, String> kafkaProducer = createKafkaProducer();
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("Stopping app and shutting down twitter client");
			hosebirdClient.stop();
			logger.info("Stopping producer");
			kafkaProducer.close();
		}));
		
		try {
			while(!hosebirdClient.isDone()) {
				String msg = msgQueue.poll(5, TimeUnit.SECONDS);
				if(msg != null) {
					kafkaProducer.send(new ProducerRecord<String, String>(GeneralConstants.kafkaTopicName, msg), new Callback() {
						
						public void onCompletion(RecordMetadata metadata, Exception exception) {
							if(exception != null) {
								logger.error("Something bad happened", exception);
							}
						}
					});
				}
			}	
		} catch(Exception e) {
			e.printStackTrace();
			hosebirdClient.stop();
		}
	}

	public KafkaProducer<String, String> createKafkaProducer() {
		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		
		KafkaProducer<String , String> producer = new KafkaProducer<String, String>(properties);
		return producer;
	}
	
	public Client getTwitterClient(BlockingQueue<String> msgQueue) {
		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
		// Optional: set up some followings and track terms
		List<String> terms = Lists.newArrayList("kafka");
		hosebirdEndpoint.trackTerms(terms);

		Authentication hosebirdAuth = new OAuth1(Credentials.TWITTER_API_KEY, Credentials.TWITTER_API_KEY_SECRET, Credentials.TWITTER_ACCESS_TOKEN, Credentials.TWITTER_ACCESS_TOKEN_SECRET);

		ClientBuilder builder = new ClientBuilder()
				.name("Hosebird-Client-01")                        
				.hosts(hosebirdHosts)
				.authentication(hosebirdAuth)
				.endpoint(hosebirdEndpoint)
				.processor(new StringDelimitedProcessor(msgQueue));

		Client hosebirdClient = builder.build();
		return hosebirdClient;
	}
}
