package model;

import java.io.Serializable;
import java.math.BigInteger;

public class RSAKeys implements Serializable {

	private BigInteger publicKey;
	private BigInteger privateKey;
	private BigInteger nMod;
	
	public RSAKeys(BigInteger publicKey, BigInteger privateKey, BigInteger nMod) {
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.nMod = nMod;
	}
	
	// construtor p compartilhamento
	public RSAKeys(BigInteger publicKey, BigInteger nMod) {
		this.publicKey = publicKey;
		this.nMod = nMod;
	}
	
	public BigInteger getPublicKey() {
		return publicKey;
	}
	public void setPublicKey(BigInteger publicKey) {
		this.publicKey = publicKey;
	}
	public BigInteger getPrivateKey() {
		return privateKey;
	}
	public void setPrivateKey(BigInteger privateKey) {
		this.privateKey = privateKey;
	}
	public BigInteger getnMod() {
		return nMod;
	}

	public void setnMod(BigInteger nMod) {
		this.nMod = nMod;
	}
	
}
