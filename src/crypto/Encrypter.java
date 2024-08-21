package crypto;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import model.Keys;
import model.RSAKeys;

public class Encrypter {

	public static String vernamEncrypt(String privateKey, String message) {
		StringBuilder vernamMessage = new StringBuilder();
		
		for(int i = 0; i < message.length(); i++) {
			char caracter = message.charAt(i);
			char caracKey = privateKey.charAt(i % privateKey.length()); // vai pegando o proximo caractere com modulo p chave ir sendo repetida
			char encryptedCaractere = (char)(caracter ^ caracKey); // xor de caracMessage com caracKey de um em um
			vernamMessage.append(encryptedCaractere); // junta tudo
		}
		
		return vernamMessage.toString();
	}
	
	public static String vernamDecrypt(String privateKey, String message) {
		return vernamEncrypt(privateKey, message); // pq vernam é simetrico
	}
	
	public static String aesEncrypt(SecretKey aesKey, String message) {
		
		byte[] bytesEncryptedMessage;
		Cipher cipher;
		String encryptedMessage = null;
		
		try {
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, aesKey);
			
			bytesEncryptedMessage = cipher.doFinal(message.getBytes()); // aq ja terminou a cifragem, poderia passar pra String
			
			encryptedMessage = Base64.getEncoder().encodeToString(bytesEncryptedMessage); // so q eu boto logo na base 64 pra acelerar
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		
		return encryptedMessage;
	}
	
	public static String aesDecrypt(SecretKey aesKey, String encryptedMessage) {
		
		byte[] bytesEncryptedMessage = Base64.getDecoder().decode(encryptedMessage); // tirar da base 64 e passar pra array de bytes
		Cipher decipher;
		String decryptedMessage = null;
		
		try {
			decipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			decipher.init(Cipher.DECRYPT_MODE, aesKey); 
			
			byte[] bytesDecryptedMessage = decipher.doFinal(bytesEncryptedMessage);
			decryptedMessage = new String(bytesDecryptedMessage);
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		
		return decryptedMessage;
	}
	
	public static String fullEncrypt(Keys keys, String message) {
		
		String encrypted = Encrypter.vernamEncrypt(keys.getVernamKey(), message);
		encrypted = Encrypter.aesEncrypt(keys.getAesKey(), encrypted);
		
		return encrypted;
	}
	
	public static String fullDecrypt(Keys keys, String encryptedMessage) {
		
		String decrypted = Encrypter.aesDecrypt(keys.getAesKey(), encryptedMessage); // retorna nulo se a chave n for a mesma da criptografia
		decrypted = Encrypter.vernamDecrypt(keys.getVernamKey(), decrypted);
		
		return decrypted;
	}
	
	public static boolean verifySignature(RSAKeys keys, String realHmac, String signature) {
		
		ArrayList<BigInteger> bigInts = new ArrayList<BigInteger>();
		String[] parts = signature.split("#");
		String decoded = "";
		
		int i = 0;
		for(String part : parts) { 
			if(!part.equals("")) {
				byte[] decodedPart = Base64.getDecoder().decode(part);
				
				BigInteger next = new BigInteger(1, decodedPart);
				
				BigInteger decodedNext = next.modPow(keys.getPublicKey(), keys.getnMod());
				
				if(!decodedNext.equals(new BigInteger("0"))) { // p não add a primeira parte, que é 0
					bigInts.add(decodedNext);
				}
				
				char v = (char) decodedNext.intValue();
				decoded = decoded + v;
				
			}
		} 

		if(realHmac.equals(decoded)) {
			return true;
		} else {
			return false;
		}

	}
	
	public static String signMessage(Keys keys, String HMAC) {
		
		String base64Next = "";
		
		for(int i = 0; i < HMAC.length(); i++) {
			char nextChar = HMAC.charAt(i);
			int number = (int) nextChar;
			
			BigInteger next = new BigInteger(Integer.toString(number));
			
			BigInteger codedNext = next.modPow(keys.getRsaKeys().getPrivateKey(), keys.getRsaKeys().getnMod());
			
			base64Next = base64Next + "#" + Base64.getEncoder().encodeToString(codedNext.toByteArray());
		}
		
		return base64Next;
	}
	
	public static String signMessage(RSAKeys keys, String HMAC) {
		
		String base64Next = "";
		
		for(int i = 0; i < HMAC.length(); i++) {
			char nextChar = HMAC.charAt(i);
			int number = (int) nextChar;
			
			BigInteger next = new BigInteger(Integer.toString(number));
			
			BigInteger codedNext = next.modPow(keys.getPrivateKey(), keys.getnMod());
			
			base64Next = base64Next + "#" + Base64.getEncoder().encodeToString(codedNext.toByteArray());
		}
		
		return base64Next;
	}
}
