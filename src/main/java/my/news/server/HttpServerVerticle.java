package my.news.server;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HttpServerVerticle extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);
	private static final String SERVER_PORT = "server_port";
	private static final String SERVER_ADDR = "server_address";
	private static final String SERVER_CONTENT = "server_content";
	private static final String EVENTBUSADDRDB = "eventBusURIDB";
	private static final String EVENTBUSADDRRSS = "eventBusURIRSS";
	private static final String EVENTBUSADDRSERVER = "eventBusURIServer";
	private static final String NEWSTABLENAME = "bbc_news_col";
	private static final String CURTABLENAME = "currency_col";
	private static final String MSGHEADER = "action";	
	private static final String NEEDRESPONSEHEADER = "need_response";
	private static final String SENDERHEADER = "sender_address";
	private static final String TYPEHEADER = "type";
	private static final String LINKHEADER = "link";
	private static final String NAMEHEADER = "name";
	private static final String REGEXHEADER = "regex";
	private static final String TABLENAMEHEADER = "table_name";
	private static final String READ = "read";
	private static final String UPDATED = "updated";
	
	//There is no need to ping-pong messages. Let's send the answer from the receiver
	private RoutingContext context = null;
	JsonArray listContentServer = null;
	
	private void indexHandler(RoutingContext context) {
		JsonObject obj = new JsonObject();
		
		obj.put("request", listContentServer);
		
		HttpServerResponse response = context.response();
		
		response.putHeader("content-type", "application/json")
				.end(obj.encodePrettily());
	}
	
	private void mainPageNewsList(RoutingContext context) {
		this.context = context;
		JsonObject query = new JsonObject().put(TYPEHEADER, "rss.xml");
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(MSGHEADER, READ);
		headers.put(NEEDRESPONSEHEADER, "true");
		headers.put(SENDERHEADER, config().getString(EVENTBUSADDRSERVER));
		headers.put(TABLENAMEHEADER, NEWSTABLENAME);
		
		eventBusSender(config().getString(EVENTBUSADDRDB), query, headers);
	}
	
	private void worldPageNewsList(RoutingContext context) {
		this.context = context;
		JsonObject query = new JsonObject().put(TYPEHEADER, "world");
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(MSGHEADER, READ);
		headers.put(NEEDRESPONSEHEADER, "true");
		headers.put(SENDERHEADER, config().getString(EVENTBUSADDRSERVER));
		headers.put(TABLENAMEHEADER, NEWSTABLENAME);
		
		eventBusSender(config().getString(EVENTBUSADDRDB), query, headers);
	}
	
	private void sciencePageNewsList(RoutingContext context) {
		this.context = context;
		JsonObject query = new JsonObject().put(TYPEHEADER, "science_and_environment");
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(MSGHEADER, READ);
		headers.put(NEEDRESPONSEHEADER, "true");
		headers.put(SENDERHEADER, config().getString(EVENTBUSADDRSERVER));
		headers.put(TABLENAMEHEADER, NEWSTABLENAME);
		
		eventBusSender(config().getString(EVENTBUSADDRDB), query, headers);
	}
	
	private void currencyPageList(RoutingContext context) {
		this.context = context;
		JsonObject query = new JsonObject().put(NAMEHEADER, new JsonObject().put("$regex", "."));
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(MSGHEADER, READ);
		headers.put(NEEDRESPONSEHEADER, "true");
		headers.put(SENDERHEADER, config().getString(EVENTBUSADDRSERVER));
		headers.put(TABLENAMEHEADER, CURTABLENAME);
		
		eventBusSender(config().getString(EVENTBUSADDRDB), query, headers);
	}
	
	private void latestMainPageNewsList(RoutingContext context) {
		this.context = context;
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(MSGHEADER, READ);
		headers.put(NEEDRESPONSEHEADER, "true");
		headers.put(LINKHEADER, "/news/rss.xml");
		headers.put(TABLENAMEHEADER, NEWSTABLENAME);
		headers.put(SENDERHEADER, config().getString(EVENTBUSADDRSERVER));
		
		eventBusSender(config().getString(EVENTBUSADDRRSS), null, headers);
	}
	
	private void latestWorldPageNewsList(RoutingContext context) {
		this.context = context;
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(MSGHEADER, READ);
		headers.put(NEEDRESPONSEHEADER, "true");
		headers.put(LINKHEADER, "/news/world/rss.xml");
		headers.put(TABLENAMEHEADER, NEWSTABLENAME);
		headers.put(SENDERHEADER, config().getString(EVENTBUSADDRSERVER));
		
		eventBusSender(config().getString(EVENTBUSADDRRSS), null, headers);
	}
	
	private void latestSciencePageNewsList(RoutingContext context) {
		this.context = context;
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(MSGHEADER, READ);
		headers.put(NEEDRESPONSEHEADER, "true");
		headers.put(LINKHEADER, "/news/science_and_environment/rss.xml");
		headers.put(TABLENAMEHEADER, NEWSTABLENAME);
		headers.put(SENDERHEADER, config().getString(EVENTBUSADDRSERVER));
		
		eventBusSender(config().getString(EVENTBUSADDRRSS), null, headers);
	}
	
	private void latestCurrencyPageList(RoutingContext context) {
		this.context = context;
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(MSGHEADER, READ);
		headers.put(NEEDRESPONSEHEADER, "true");
		headers.put(LINKHEADER, "/stats/eurofxref/eurofxref-daily.xml");
		headers.put(TABLENAMEHEADER, CURTABLENAME);
		headers.put(SENDERHEADER, config().getString(EVENTBUSADDRSERVER));
		headers.put(REGEXHEADER, ".");
		
		eventBusSender(config().getString(EVENTBUSADDRRSS), null, headers);
	}
	
	private void receiveMsgDB(final String addr) {
		
		EventBus eventBus = vertx.eventBus();
		MessageConsumer<JsonArray> consumer = eventBus.consumer(addr);
		consumer.handler(msg -> {
			switch (msg.headers().get(MSGHEADER)) {
				case READ: {
					HttpServerResponse response = context.response();
					response.setStatusCode(201)
							.putHeader("content-type", "application/json; charset=utf-8")
							.end(msg.body().encodePrettily());
				}
					break;
				case UPDATED: {
					HttpServerResponse response = context.response();
					response.setStatusCode(202)
							.putHeader("content-type", "application/json; charset=utf-8")
							.end(msg.body().encodePrettily());
				}
					break;
				default:
					System.out.println(msg.headers().get(MSGHEADER));
					break;
			}
		});
	}
	
	private <T> void eventBusSender(final String addr, T msg, final Map<String, String> headers) {
		EventBus eventBus = vertx.eventBus();
		DeliveryOptions options = new DeliveryOptions();
		
		for (Map.Entry<String, String> header : headers.entrySet()) {
			options.addHeader(header.getKey(), header.getValue());
		}
		
		eventBus.send(addr, msg, options);	
	}
	
	@Override
	public void start(Future<Void> future) {
		Integer port = new Integer(config().getString(SERVER_PORT));
		String hostAddr = config().getString(SERVER_ADDR);
		listContentServer = config().getJsonArray(SERVER_CONTENT);
		
		HttpServerOptions options = new HttpServerOptions().setHost(hostAddr);
		HttpServer server = vertx.createHttpServer(options);
		
		Router router = Router.router(vertx);

		router.route("/").handler(this::indexHandler);
		//Main page
		router.route(listContentServer.getString(0)).handler(this::mainPageNewsList);
		//World news page
		router.route(listContentServer.getString(1)).handler(this::worldPageNewsList);
		//Science news page
		router.route(listContentServer.getString(2)).handler(this::sciencePageNewsList);
		//Currency page
		router.route(listContentServer.getString(3)).handler(this::currencyPageList);
		//Latest Main news page
		router.route(listContentServer.getString(4)).handler(this::latestMainPageNewsList);
		//Latest World news page
		router.route(listContentServer.getString(5)).handler(this::latestWorldPageNewsList);
		//Latest Science news page
		router.route(listContentServer.getString(6)).handler(this::latestSciencePageNewsList);
		//Latest currency page
		router.route(listContentServer.getString(7)).handler(this::latestCurrencyPageList);
		
		server.requestHandler(router::accept)
		.listen(port, request -> {
			if (request.succeeded()) {
				LOGGER.info("Listenning on port: " + port);
				receiveMsgDB(config().getString(EVENTBUSADDRSERVER));
				future.complete();
			} else {
				LOGGER.error("Failed to start server");
				future.fail(request.cause());
			}
		});
		
	}
}
