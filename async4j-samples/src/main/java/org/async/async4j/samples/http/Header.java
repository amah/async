package org.async.async4j.samples.http;

import java.util.List;

public class Header{
	private final StatusLine statusLine;
	private final List<HeaderField> headerFields;
	public Header(StatusLine statusLine, List<HeaderField> headerFields) {
		this.statusLine = statusLine;
		this.headerFields = headerFields;
	}
	public StatusLine getStatusLine() {
		return statusLine;
	}
	public List<HeaderField> getHeaderFields() {
		return headerFields;
	}

	public String getFieldValue(String name){
		for (HeaderField field : headerFields) {
			if(field.getName().equals(name)){
				return field.getValue();
			}
		}
		
		return null;
	}
	

	public Long getFieldValueAsLong(String name){
		String value = getFieldValue(name);
		if(value == null || value.length() == 0){
			return null;
		}
		return new Long(value);
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s %s %s\r\n", statusLine.getHttpVersion(), statusLine.getStatusCode(), statusLine.getReasonPhrase()));
		for (HeaderField field : headerFields) {
			sb.append(String.format("%s: %s\r\n", field.getName(), field.getValue()));
		}
		return sb.toString();
	}

	
}