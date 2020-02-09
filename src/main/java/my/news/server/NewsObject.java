package my.news.server;

import io.vertx.core.json.JsonObject;

public class NewsObject {
	private static final String TITLE = "title";
	private static final String DESCRIPTION = "description";
	private static final String LINK = "link";
	private static final String MEDIA = "media";
	private static final String TYPE = "type";
	
	private String title;
	private String description;
	private String link;
	private String media;
	private String type;
	private JsonObject jsonObject;
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getsetDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getLink() {
		return link;
	}
	
	public void setLink(String link) {
		this.link = link;
	}
	
	public String getMedia() {
		return media;
	}
	
	public void setMedia(String media) {
		this.media = media;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void createJsonObject() {
		jsonObject = new JsonObject()
				.put(TITLE, title)
				.put(DESCRIPTION, description)
				.put(LINK, link)
				.put(MEDIA, media)
				.put(TYPE, type);
	}
	
	public JsonObject getJsonObject() {
		return jsonObject;
	}
}
