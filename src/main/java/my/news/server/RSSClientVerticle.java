package my.news.server;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RSSClientVerticle extends AbstractVerticle {
	
	private static final String RSS_FEED = "rss_feed";
	private static final String DEFAULT_URI = "default_uri";
	private static final String CURRENCY_URI_DEFAULT = "currency_uri_default";
	private static final String CURRENCY_URI = "currency_uri";
	private static final String EVENTBUSADDRDB = "eventBusURIDB";
	private static final String EVENTBUSADDRRSS = "eventBusURIRSS";
	private static final String EVENTBUSADDRSERVER = "eventBusURIServer";
	private static final String CURTABLENAME = "currency_col";
	private static final String NEWSTABLENAME = "bbc_news_col";
	private static final String MSGHEADER = "action";
	private static final String NEEDRESPONSEHEADER = "need_response";
	private static final String SENDERHEADER = "sender_address";
	private static final String TYPEHEADER = "type";
	private static final String NAMEHEADER = "name";
	private static final String LINKHEADER = "link";
	private static final String TABLENAMEHEADER = "table_name";
	private static final String UPDATEKEYHEADERFILT = "filter_key";
	private static final String UPDATEVALUEHEADERFILT = "filter_value";
	private static final String ISREGEXFILTER = "is_regex";
	private static final String REGEXHEADER = "regex";
	private static final String INSERTED = "inserted";
	private static final String INSERT = "insert";
	private static final String READ = "read";
	private static final String UPDATED = "updated";
	private static final String UPDATE = "update";
	
	private XmlParser parser = new XmlParser();
	
	private JsonArray parseNewsBuffer(Buffer buffer, final String type) {
		return parser.parseRSS(buffer, type);
	}

	private Future<Void> makeRequestToNewsServer(HttpClient client, String link, 
			final String eventBusAddr, final String action, final Boolean needResp) {
		
		Future<Void> reqFuture = Future.future();
		String segments[] = link.split("/"); 
		
		client.request(HttpMethod.GET, link, response -> {
			System.out.println(response.statusCode()); 
			
			response.bodyHandler(buffer -> {
				vertx.executeBlocking(future -> {
					JsonArray lsNews = parseNewsBuffer(buffer, segments[2]);
					future.complete(lsNews);
				}, result -> {
					Map<String, String> headers = new HashMap<String, String>();
					headers.put(MSGHEADER, action);
					headers.put(NEEDRESPONSEHEADER, needResp.toString());
					headers.put(SENDERHEADER, config().getString(EVENTBUSADDRRSS));
					headers.put(TABLENAMEHEADER, NEWSTABLENAME);
					if (action.equals(UPDATE)) {
						headers.put(UPDATEKEYHEADERFILT, TYPEHEADER);
						headers.put(ISREGEXFILTER, "false");
						headers.put(UPDATEVALUEHEADERFILT, segments[2]);
					}
					
					eventBusCommunicationSender(eventBusAddr, result.result(), headers);
					
					reqFuture.complete();
				});
			});
		}).exceptionHandler(e -> {
			reqFuture.fail(e.getCause());
			System.out.println("Received exception: " + e.getMessage());
			e.printStackTrace();
		}).putHeader("Accept", "text/xml").end();
		
		return reqFuture;
	}
	
	private Future<Void> makeRequestToCurrencyServer(HttpClient client, final String eventBusAddr, 
			final String action, final Boolean needResp, final String req) {
		String link = config().getString(CURRENCY_URI);
		Future<Void> reqFuture = Future.future();
		
		client.request(HttpMethod.GET, link, response -> {
			System.out.println(response.statusCode()); 
			
			response.bodyHandler(buffer -> {
				vertx.executeBlocking(future -> {
					JsonArray lsCur = parser.parseCurrency(buffer);
					future.complete(lsCur); 
				}, result -> {
					Map<String, String> headers = new HashMap<String, String>();
					headers.put(MSGHEADER, action);
					headers.put(NEEDRESPONSEHEADER, needResp.toString());
					headers.put(SENDERHEADER, config().getString(EVENTBUSADDRRSS));
					headers.put(TABLENAMEHEADER, CURTABLENAME); 
					if (action.equals(UPDATE)) {
						headers.put(UPDATEKEYHEADERFILT, NAMEHEADER);
						headers.put(ISREGEXFILTER, "true");
						headers.put(UPDATEVALUEHEADERFILT, req);
					}
					
					eventBusCommunicationSender(eventBusAddr, result.result(), headers);
					
					reqFuture.complete();
				});
			});
			
		}).exceptionHandler(e -> {
			reqFuture.fail(e.getCause());
			System.out.println("Received exception: " + e.getMessage());
			e.printStackTrace();
		}).putHeader("Accept", "text/xml").end();
		
		return reqFuture;
	}
	
	private <T> void eventBusCommunicationReceiver(final String addr) {
		EventBus eventBus = vertx.eventBus();
		//Messages consumer
		MessageConsumer<T> consumer = eventBus.consumer(addr);
		
		//Register handler for DB verticle request
		consumer.handler(msg -> {
			switch (msg.headers().get(MSGHEADER)) {
				case INSERTED:
				/*	Map<String, String> headers = new HashMap<String, String>();
					headers.put(MSGHEADER, UPDATE);
					headers.put(NEEDRESPONSEHEADER, "false");
					headers.put(SENDERHEADER, addr);
					headers.put(TYPEHEADER, "rss.xml");
					eventBusCommunicationSender(config().getString(EVENTBUSADDRDB), null, headers);*/
					break;
				case UPDATED:
						Map<String, String> headers = new HashMap<String, String>();
						headers.put(MSGHEADER, UPDATED);
						eventBusCommunicationSender(config().getString(EVENTBUSADDRSERVER), msg.body(), headers);
					break;
				case READ: {
					HttpClientOptions options = null;
					HttpClient client = null;

					if (msg.headers().get(TABLENAMEHEADER).equals(NEWSTABLENAME)) {
						options = new HttpClientOptions().setDefaultHost(config().getString(DEFAULT_URI));
						client = vertx.createHttpClient(options);
						
						makeRequestToNewsServer(client, msg.headers().get(LINKHEADER), config().getString(EVENTBUSADDRDB),
												UPDATE, true);
					} else {
						options = new HttpClientOptions().setDefaultHost(config().getString(CURRENCY_URI_DEFAULT));
						client = vertx.createHttpClient(options);
						
						makeRequestToCurrencyServer(client, config().getString(EVENTBUSADDRDB), 
								UPDATE, true, msg.headers().get(REGEXHEADER));
					}
				}
					break;
				default:
					System.out.println(msg.headers().get(MSGHEADER));
					break;
			}
		});		
	}
	
	private <T> void eventBusCommunicationSender(final String addr, T msg, final Map<String, String> headers) {
		EventBus eventBus = vertx.eventBus();
		DeliveryOptions options = new DeliveryOptions();
		
		for (Map.Entry<String, String> header : headers.entrySet()) {
			options.addHeader(header.getKey(), header.getValue());
		}
		
		eventBus.send(addr, msg, options);	
	}
	
	@Override
	public void start(Future<Void> fut) {
		JsonArray links = config().getJsonArray(RSS_FEED);
		@SuppressWarnings("unchecked")
		List<String> lsLinks = links.getList();
		String eventBusAddrDB = config().getString(EVENTBUSADDRDB);
		String eventBusAddrRSS = config().getString(EVENTBUSADDRRSS);
		
		HttpClientOptions options = new HttpClientOptions().setDefaultHost(config().getString(DEFAULT_URI));
		HttpClient client = vertx.createHttpClient(options);
		@SuppressWarnings("rawtypes")
		List<Future> lsFuture = new ArrayList<>();
		
		for (String link : lsLinks) {
			Future<Void> future = makeRequestToNewsServer(client, link, eventBusAddrDB, INSERT, false);
			lsFuture.add(future);
		}
		
		options = new HttpClientOptions().setDefaultHost(config().getString(CURRENCY_URI_DEFAULT));
		client = vertx.createHttpClient(options);
		
		lsFuture.add(makeRequestToCurrencyServer(client, eventBusAddrDB, INSERT, false, null));
		
		CompositeFuture.all(lsFuture).setHandler(result -> {
			if (result.succeeded()) {
				fut.complete();
			} else {
				fut.fail(result.cause());
			}
		});
		
		eventBusCommunicationReceiver(eventBusAddrRSS);		
	}
}
