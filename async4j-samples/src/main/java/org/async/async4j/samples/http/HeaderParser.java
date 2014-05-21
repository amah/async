package org.async.async4j.samples.http;

import java.util.ArrayList;
import java.util.List;

class HeaderParser {
	private static final byte[] SP = new byte[]{0x20};
	private static final byte[] CRLF = new byte[]{0x0D, 0x0A};
	private static final byte[] COLON = new byte[]{0x3A};
	
	public Header parse(byte[] data){
		return parse(data, 0, data.length);
	}
	public Header parse(byte[] data, int offset, int length){
		
		int end = offset + length; 
		int index = indexOf(data, offset, end, SP);
		assertFound(index, " httpVersion ");
		String httpVersion = new String(data, offset, index - offset);
		
		offset = index + SP.length;
		index = indexOf(data, offset, end, SP);
		assertFound(index, " statusCode ");
		String status = new String(data, offset, index - offset);
		
		offset = index + SP.length;
		index = indexOf(data, offset, end, CRLF);
		assertFound(index, " reasonPhrase ");
		String reasonPhrase = new String(data, offset, index - offset);
		
		StatusLine statusLine = new StatusLine(httpVersion, new Integer(status), reasonPhrase);
		
		List<HeaderField> headerFields = new ArrayList<>();
		
		offset = index + CRLF.length;
		
		while(offset < end){
			index = indexOf(data, offset, end, COLON);
			if(index == -1){
				if(indexOf(data, offset, end, CRLF) == offset){
					break;
				}else{
					throw new HttpProtocolException("Header field separator missed");
				}
			}
			String name = new String(data, offset, index - offset);
			offset = index + COLON.length;
			if(offset < end){
				index = indexOf(data, offset, end, CRLF);
				if(index == -1){
					throw new HttpProtocolException("End of header field line missed");
				}
				String value = new String(data, offset, index - offset);
				value = value.trim();
				
				headerFields.add(new HeaderField(name, value));
				
				offset = index + CRLF.length;
			}
		}

		return new Header(statusLine, headerFields);
	}
	
	private void assertFound(int index, String string) {
		if(index == -1){
			throw new HttpProtocolException("required field "+string+" not found");
		}
	}

	public int indexOf(byte[] data, byte[] sep){
		return indexOf(data, 0, data.length, sep);
	}		
	public int indexOf(byte[] data, int start, int end, byte[] sep){
		if(sep.length == 0) return 0;
		int matched = 0;
		int index = start;
		for (; matched < sep.length && index < end; index++) {
			if(data[index] == sep[matched]){
				matched++;
			}
			else{
				matched = 0;
			}
			if(matched == sep.length){
				return index - matched + 1;
			}
		}
		
		return -1;
	}
}