package dk.itu.energyfutures.ble.helpers;

import java.util.List;
import java.util.zip.Inflater;

public class GzipAndJsonParser {

	public static String unzipAndParseByteList(List<Byte> bytes){	
		try {
			byte[] buf = new byte[bytes.size()];
			for(int i =0; i < buf.length; i++){
				buf[i] = bytes.get(i);
			}
			Inflater decompresser = new Inflater();
			decompresser.setInput(buf, 0, buf.length);
			StringBuilder sb = new StringBuilder("");
			byte[] result = new byte[1000];
			int resultLength = 0;
			while((resultLength = decompresser.inflate(result)) > 0){
				sb.append( new String(result, 0, resultLength, "UTF-8"));
			}			
			decompresser.end();
			return sb.toString();
			
		}
		catch (Exception e) {
			throw new RuntimeException("We could not parse the json");
		}		
	}
}
