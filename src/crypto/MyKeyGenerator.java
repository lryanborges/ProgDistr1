package crypto;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import model.RSAKeys;

public class MyKeyGenerator {

	public static SecretKey generateKeyAes() {

		SecretKey generatedKey = null;

		try {
			KeyGenerator generator = KeyGenerator.getInstance("AES");
			generatedKey = generator.generateKey();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return generatedKey;

	}

	public static String generateKeyVernam() {

		int size = 16;
		String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder randomString = new StringBuilder(size);
		Random random = new Random();

		for (int i = 0; i < size; i++) {
			int randomIndex = random.nextInt(characters.length());
			char randomChar = characters.charAt(randomIndex);
			randomString.append(randomChar);
		}
		
		return randomString.toString();
		
	}

	public static String generateKeyHMAC() {

		int size = 16;
		String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder randomString = new StringBuilder(size);
		Random random = new Random();

		for (int i = 0; i < size; i++) {
			int randomIndex = random.nextInt(characters.length());
			char randomChar = characters.charAt(randomIndex);
			randomString.append(randomChar);
		}
		
		return randomString.toString();
		
	}
	
	public static RSAKeys generateKeysRSA() {
		
		SecureRandom rand = new SecureRandom();
		
		BigInteger p = BigInteger.probablePrime(1194, rand);
		BigInteger q = BigInteger.probablePrime(1194, rand);
		
		BigInteger n = p.multiply(q); // n é o valor do modulo
		
		BigInteger phiNEuler = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
		
		BigInteger publicKey = generatePublicKey(phiNEuler);
		BigInteger privateKey = generatePrivateKey(publicKey, phiNEuler); 
		
		return new RSAKeys(publicKey, privateKey, n);
	}

	private static BigInteger generatePublicKey(BigInteger phiN) {
		BigInteger e = null;
		BigInteger mdc = null;
		SecureRandom rand = new SecureRandom();
		
		do {
			e = BigInteger.probablePrime(1194, rand);
			mdc = e.gcd(phiN);
			
			String phiNString = phiN.toString();
			String eString = e.toString();
			
			int comparation = eString.compareTo(phiNString);
			if(mdc.equals(new BigInteger("1")) && comparation < 0) {
				return e;
			}
			
		} while(!mdc.equals(new BigInteger("1")));
		
		return e;
	}
	
	private static BigInteger generatePrivateKey(BigInteger publicKey, BigInteger phiN) {
		BigInteger d = publicKey.modInverse(phiN);

		return d;	
	}
	
}
