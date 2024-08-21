package model;

import java.io.Serializable;

import javax.crypto.SecretKey;

public class Keys implements Serializable {

	private SecretKey aesKey;
	private String HMACKey;
	private String vernamKey;
	private RSAKeys rsaKeys;
	
	public SecretKey getAesKey() {
		return aesKey;
	}
	public void setAesKey(SecretKey aesKey) {
		this.aesKey = aesKey;
	}
	public String getHMACKey() {
		return HMACKey;
	}
	public void setHMACKey(String hMACKey) {
		HMACKey = hMACKey;
	}
	public String getVernamKey() {
		return vernamKey;
	}
	public void setVernamKey(String vernamKey) {
		this.vernamKey = vernamKey;
	}
	public RSAKeys getRsaKeys() {
		return rsaKeys;
	}
	public void setRsaKeys(RSAKeys rsaKeys) {
		this.rsaKeys = rsaKeys;
	}
	
}
