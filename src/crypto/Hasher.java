package crypto;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Hasher {
	
	public static final String HMACTYPE = "HmacSHA256";

	public static String hMac(String secretKey, String message) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
		
		Mac hmac = Mac.getInstance(HMACTYPE); // especifico que usa o hmac de SHA256
		SecretKeySpec secretKeyObjectified = new SecretKeySpec(secretKey.getBytes("UTF-8"), HMACTYPE);
		
		hmac.init(secretKeyObjectified);
		
		byte[] bytesHMAC = hmac.doFinal(message.getBytes("UTF-8"));
		String hmacMessage = byte2Hex(bytesHMAC);
				
		return hmacMessage;
	}
	
	public static String byte2Hex(byte[] bytes) {
		StringBuilder stringBuilder = new StringBuilder();
		
		for(byte b: bytes) {
			stringBuilder.append(String.format("%02x", b));	
		}
		
		return stringBuilder.toString();
	}
	
}
