package org.async.async4j.samples.http;

class HeaderField {
	private final String name;
	private final String value;
	public HeaderField(String name, String value) {
		super();
		this.name = name;
		this.value = value;
	}
	public String getName() {
		return name;
	}
	public String getValue() {
		return value;
	}
	@Override
	public String toString() {
		return "HeaderField [name=" + name + ", value=" + value + "]";
	}
}