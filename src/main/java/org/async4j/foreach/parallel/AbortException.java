package org.async4j.foreach.parallel;

public class AbortException extends Throwable {
	private static final long serialVersionUID = 1L;
	public static final AbortException INSTANCE = new AbortException();
	
}
