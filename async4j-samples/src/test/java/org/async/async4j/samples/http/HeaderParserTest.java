package org.async.async4j.samples.http;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class HeaderParserTest {

	@Test
	public void indexOfTest() {
		HeaderParser parser = new HeaderParser();
		// Assert.assertEquals(0, parser.indexOf("".getBytes(), "".getBytes()));
		// Assert.assertEquals(0, parser.indexOf("abcd".getBytes(),
		// "".getBytes()));
		//
		// Assert.assertEquals(-1, parser.indexOf("abcd".getBytes(),
		// " ".getBytes()));
		Assert.assertEquals(0, parser.indexOf(" abcd".getBytes(), " ".getBytes()));
		Assert.assertEquals(2, parser.indexOf("ab cd".getBytes(), " ".getBytes()));
		Assert.assertEquals(4, parser.indexOf("abcd ".getBytes(), " ".getBytes()));

		Assert.assertEquals(-1, parser.indexOf(" abcd".getBytes(), "  ".getBytes()));
		Assert.assertEquals(-1, parser.indexOf("ab cd".getBytes(), "  ".getBytes()));
		Assert.assertEquals(-1, parser.indexOf("abcd ".getBytes(), "  ".getBytes()));

		Assert.assertEquals(0, parser.indexOf("  abcd".getBytes(), " ".getBytes()));
		Assert.assertEquals(2, parser.indexOf("ab  cd".getBytes(), " ".getBytes()));
		Assert.assertEquals(4, parser.indexOf("abcd  ".getBytes(), " ".getBytes()));

		Assert.assertEquals(0, parser.indexOf("  abcd".getBytes(), "  ".getBytes()));
		Assert.assertEquals(2, parser.indexOf("ab  cd".getBytes(), "  ".getBytes()));
		Assert.assertEquals(4, parser.indexOf("abcd  ".getBytes(), "  ".getBytes()));

	}

	@Test
	public void statusCodeTest() throws IOException {
		byte[] bytes = FileUtils.readFileToByteArray(new File("src/test/data/http-header-01.txt"));
		HeaderParser parser = new HeaderParser();
		Header header = parser.parse(bytes);

		Assert.assertEquals("HTTP/1.1", header.getStatusLine().getHttpVersion());
		Assert.assertEquals(200, header.getStatusLine().getStatusCode());
		Assert.assertEquals("OK", header.getStatusLine().getReasonPhrase());

		Map<String, String> fields = new HashMap<String, String>();
		fields.put("User-Agent", "curl/7.16.3 libcurl/7.16.3 OpenSSL/0.9.7l zlib/1.2.3");
		fields.put("Host", "www.example.com");
		fields.put("Accept-Language", "en, mi");
		fields.put("Date", "Mon, 27 Jul 2009 12:28:53 GMT");
		fields.put("Server", "Apache");
		fields.put("Last-Modified", "Wed, 22 Jul 2009 19:15:56 GMT");
		fields.put("ETag", "\"34aa387-d-1568eb00\"");
		fields.put("Accept-Ranges", "bytes");
		fields.put("Content-Length", "0");
		fields.put("Vary", "Accept-Encoding");
		fields.put("Content-Type", "text/plain");

		Assert.assertEquals(fields.size(), header.getHeaderFields().size());
		
		for (Map.Entry<String, String> entry : fields.entrySet()) {
			Assert.assertEquals(entry.getValue(), header.getFieldValue(entry.getKey()));
		}
	}
}
