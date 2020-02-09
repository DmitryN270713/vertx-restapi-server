package my.news.server;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.util.Iterator;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.XMLEvent;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;

public class XmlParser {
	private static final String TITLE = "title";
	private static final String DESCRIPTION = "description";
	private static final String LINK = "link";
	private static final String MEDIA = "thumbnail";
	private static final String MEDIA_ATTR = "url";
	private static final String SKIP_GUID = "guid";
	private static final String SKIP_PUBDATE = "pubDate";
	private static final String ITEM = "item";
	
	private static final String CUBE = "Cube";
	private static final String CURRENCY = "currency";
	private static final String RATE = "rate";
	
	
	public JsonArray parseCurrency(Buffer buf) {
		
		XMLInputFactory inFactory = XMLInputFactory.newInstance();
		JsonArray lsCur = new JsonArray();
		
		try {
			InputStream is = new ByteArrayInputStream(buf.getBytes());
			XMLEventReader reader = inFactory.createXMLEventReader(is);
			
			Currency cur = null;
			
			while (reader.hasNext()) {
				XMLEvent event = reader.nextEvent();
				
				if (event.isStartElement()) {
					if (event.asStartElement().getName().getLocalPart().equals(CUBE)) {
						cur = new Currency();
						@SuppressWarnings("unchecked")
						Iterator<Attribute> attributes = event.asStartElement().getAttributes();
						while (attributes.hasNext()) {
							Attribute attribute = attributes.next();
							if (attribute.getName().getLocalPart().equals(CURRENCY)) {
								cur.setCurName(attribute.getValue());
								continue;
							}
							
							if (attribute.getName().getLocalPart().equals(RATE)) {
								cur.setCoefToEuro(attribute.getValue());
								continue;
							}
						}	
					}
				}
				
				if (event.isEndElement()) {
					EndElement endElement = event.asEndElement();
					if (endElement.getName().getLocalPart().equals(CUBE)) {
						cur.createJsonObject();
						lsCur.add(cur.getJsonObject());
					}
				}
			}
			
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		
		return lsCur;
	}
	
	public JsonArray parseRSS(Buffer buf, final String type) {		

			XMLInputFactory inFactory = XMLInputFactory.newInstance();
			JsonArray lsNews = new JsonArray();
			
			try { 
				InputStream is = new ByteArrayInputStream(buf.getBytes());
			
				XMLEventReader reader = inFactory.createXMLEventReader(is);

				NewsObject news = null;
				
				while(reader.hasNext()) {
					XMLEvent event = reader.nextEvent();
					
					if (event.isStartElement()) { 
						if (event.asStartElement().getName().getLocalPart().equals(ITEM)) {
							news = new NewsObject();
							news.setType(type);
						}
						
						if (news != null) {
						
							if (event.asStartElement().getName().getLocalPart().equals(TITLE)) {
								event = reader.nextEvent();
								news.setTitle(event.asCharacters().getData());
								continue;
							}
							
							if (event.asStartElement().getName().getLocalPart().equals(DESCRIPTION)) {
								event = reader.nextEvent();
								news.setDescription(event.asCharacters().getData());
								continue;
							}
							
							if (event.asStartElement().getName().getLocalPart().equals(LINK)) {
								event = reader.nextEvent();
								news.setLink(event.asCharacters().getData());
								continue;
							}
							
							if (event.asStartElement().getName().getLocalPart().equals(SKIP_GUID)) {
								continue;
							}
							
							if (event.asStartElement().getName().getLocalPart().equals(SKIP_PUBDATE)) {
								continue;
							}
							
							if (event.asStartElement().getName().getLocalPart().equals(MEDIA)) {
								@SuppressWarnings("unchecked")
								Iterator<Attribute> attributes = event.asStartElement().getAttributes();
								while (attributes.hasNext()) {
									Attribute attribute = attributes.next();
									if (attribute.getName().toString().equals(MEDIA_ATTR)) {
									    news.setMedia(attribute.getValue());
									}
								}
							}
						}
					}
					
					if (event.isEndElement()) {
						EndElement endElement = event.asEndElement();
						if (endElement.getName().getLocalPart().equals(ITEM)) {
							news.createJsonObject();
							lsNews.add(news.getJsonObject());
						}
					}					
				}
				
			} catch (XMLStreamException e) {
				System.out.println(e.getMessage());
			}
			
		return lsNews;
	}
}
