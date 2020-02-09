package my.news.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;

public class MainVerticle extends AbstractVerticle {

	@Override
	public void start(Future<Void> mainFut) throws Exception {
		Future<String> dbFuture = Future.future();
		DeploymentOptions options = new DeploymentOptions().setConfig(config());
		
		vertx.deployVerticle(DataBaseVerticle.class.getName(), options, dbFuture.completer()); 
		
		dbFuture.compose(vert -> {
			
			Future<String> httpClientFuture = Future.future();
			vertx.deployVerticle(RSSClientVerticle.class.getName(), options, httpClientFuture.completer());
			
			return httpClientFuture;
		}).compose(vert -> {
			
			Future<String> httpServerFuture = Future.future();
			vertx.deployVerticle(HttpServerVerticle.class.getName(), options, httpServerFuture.completer());
			
			return httpServerFuture;			
		}).setHandler(res -> {
			if (res.succeeded()) {
				mainFut.complete();
			} else {
				mainFut.fail(res.cause());
			}
		});
	}
	
}// End of class declaration
