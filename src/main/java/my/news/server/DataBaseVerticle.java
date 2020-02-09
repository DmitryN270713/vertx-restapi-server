package my.news.server;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.BulkOperation;

public class DataBaseVerticle extends AbstractVerticle {

	private MongoClient mongoDB;
	private static final String EVENTBUSADDRDB = "eventBusURIDB";
	private static final String CURTABLENAME = "currency_col";
	private static final String NEWSTABLENAME = "bbc_news_col";
	private static final String MSGHEADER = "action";
	private static final String NEEDRESPONSEHEADER = "need_response";
	private static final String SENDERHEADER = "sender_address";
	private static final String TABLENAMEHEADER = "table_name";
	private static final String INSERTED = "inserted";
	private static final String INSERT = "insert";
	private static final String READ = "read";
	private static final String UPDATE = "update";
	private static final String UPDATED = "updated";
	private static final String UPDATEKEYHEADERFILT = "filter_key";
	private static final String UPDATEVALUEHEADERFILT = "filter_value";
	private static final String ISREGEXFILTER = "is_regex";
	
	private void initializeMongo(Future<Void> mainFuture) {
		
		JsonObject config = Vertx.currentContext().config();
		String uri = config.getString("connection_string");
		String dbName = config.getString("db_name");
		JsonObject mongoConfig = new JsonObject()
				.put("connection_string", uri)
				.put("db_name", dbName);
		
		mongoDB = MongoClient.createShared(vertx, mongoConfig);
		
		// Count news entities and drop collection if not zero
		Future<Long> newsCount = Future.future();
		
		mongoDB.count(NEWSTABLENAME, new JsonObject(), newsCount.completer());
		
		newsCount.compose(count -> {
			Future<Void> dropFuture = Future.future();
			if (count > 0) {
				mongoDB.dropCollection(NEWSTABLENAME, dropFuture.completer());
			} else {
				dropFuture.complete();
			}
			
			return dropFuture;
		}).compose(fut -> {
			Future <Long> currencyFuture = Future.future();
			mongoDB.count(CURTABLENAME, new JsonObject(), currencyFuture.completer());
			
			return currencyFuture;
		}).compose(count -> {
			Future <Void> dropFuture = Future.future();
			if (count > 0) {
				mongoDB.dropCollection(CURTABLENAME, dropFuture.completer());
			} else {
				dropFuture.complete();
			}
			
			return dropFuture;
		}).setHandler(res -> {
			if (res.succeeded()) {
				mainFuture.complete();
			} else {
				mainFuture.fail(res.cause());
			}
		});
	}
	
	private void insertBulkData(final String collectionName, final JsonArray data, final Boolean needResp, final String sender) {
		List<BulkOperation> lsBulk = new ArrayList<>();
		for (Object news : data) {
			BulkOperation operation = BulkOperation.createInsert((JsonObject) news);
			lsBulk.add(operation);
		}
		
		mongoDB.bulkWrite(collectionName, lsBulk, res -> {
			if (res.succeeded()) {
				if (needResp) {
					sendMsg(null, sender, INSERTED);
				}
			} else {
				System.out.println("Failed from DB: " + res.cause());
			}
		});
	}
	
	private void updateBulkData(final String collectionName, final JsonArray data, 
			final Boolean needResp, final JsonObject filter, final String sender) {
		List<BulkOperation> lsBulk = new ArrayList<>();
		
		for (Object news : data) {
			BulkOperation operation = BulkOperation.createReplace(filter, (JsonObject) news);
			lsBulk.add(operation);
		}
		
		mongoDB.bulkWrite(collectionName, lsBulk, res -> {
			if (res.succeeded()) {
				if (needResp) {
					sendMsg(data, sender, UPDATED);
				}
			} else {
				System.out.println("Failed from DB: " + res.cause());
			}
		});
	}
	
	private void readFromDB(final String collectionName, final String sender, final JsonObject query) {
		JsonArray lsNews = new JsonArray();
		
		mongoDB.find(collectionName, query, res -> {
			if (res.succeeded()) {
				for (JsonObject news : res.result()) {
					lsNews.add(news);
				}
				//Send response to client
				sendMsg(lsNews, sender, READ);
				
			} else {
				System.out.println(res.cause());
			}
		});
	}
	
	private <T> void receiveMsg(final String addr) {
		
		EventBus eventBus = vertx.eventBus();
		MessageConsumer<T> consumer = eventBus.consumer(addr);
		consumer.handler(msg -> {
			switch (msg.headers().get(MSGHEADER)) {
				case INSERT:
					insertBulkData(msg.headers().get(TABLENAMEHEADER),
								   (JsonArray) msg.body(), 
								   Boolean.valueOf(msg.headers().get(NEEDRESPONSEHEADER)), 
								   msg.headers().get(SENDERHEADER));
					break;
				case READ:
					readFromDB(msg.headers().get(TABLENAMEHEADER),
							   msg.headers().get(SENDERHEADER), 
							   (JsonObject) msg.body());
					break;
				case UPDATE:
					JsonObject filter = null;
					if (!Boolean.valueOf(msg.headers().get(ISREGEXFILTER))) {
						filter = new JsonObject().put(msg.headers().get(UPDATEKEYHEADERFILT),
																 msg.headers().get(UPDATEVALUEHEADERFILT));
					} else {
						filter = new JsonObject().put(msg.headers().get(UPDATEKEYHEADERFILT),
								new JsonObject().put("$regex", msg.headers().get(UPDATEVALUEHEADERFILT)));
					}
					updateBulkData(msg.headers().get(TABLENAMEHEADER),
								   (JsonArray) msg.body(), Boolean.valueOf(msg.headers().get(NEEDRESPONSEHEADER)), 
								   filter, 
								   msg.headers().get(SENDERHEADER));
					break;
				default:
					System.out.println(msg.headers().get(MSGHEADER));
					break;
			}
		});
	}
	
	private <T> void sendMsg(final T msg, final String eventBusAddr, final String headerMsg) {
		EventBus eventBus = vertx.eventBus();
		DeliveryOptions options = new DeliveryOptions().addHeader(MSGHEADER, headerMsg);
		
		eventBus.send(eventBusAddr, msg, options);
	}
	
	@Override
	public void start(Future<Void> future) throws Exception {
		String eventBusAddr = config().getString(EVENTBUSADDRDB);

		initializeMongo(future);
		//Call receiver
		receiveMsg(eventBusAddr);
	}
	
	@Override
	public void stop() {
		mongoDB.close();
	}
}
