package my.news.server;

import io.vertx.core.json.JsonObject;

public class Currency {

	private static final String NAME = "name";
	private static final String VALUE = "cur_value";
	
	private String curName;
	private String coefToEuro;
	JsonObject curObj = null;
	
	public String getCurName() {
		return curName;
	}
	
	public void setCurName(String curName) {
		this.curName = curName;
	}
	
	public String getCoefToEuro() {
		return coefToEuro;
	}
	
	public void setCoefToEuro(String coefToEuro) {
		this.coefToEuro = coefToEuro;
	}
	
	public JsonObject getJsonObject() {
		return curObj;
	}
	
	public void createJsonObject () {
		curObj = new JsonObject()
				 .put(NAME, curName)
				 .put(VALUE, coefToEuro);
	}
}