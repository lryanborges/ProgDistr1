package datagrams;

import java.io.Serializable;

public class Permission implements Serializable {
	private String sourceIp;
	private String destinationIp;
	private int destinationPort;
	private String name;
	private boolean allow;
	
	public Permission(String sourceIp, String destIp, int destPort, String name, boolean allow) {
		this.sourceIp = sourceIp;
		this.destinationIp = destIp;
		this.destinationPort = destPort;
		this.name = name;
		this.allow = allow;
	}
	
	public String getSourceIp() {
		return sourceIp;
	}
	public void setSourceIp(String sourceIp) {
		this.sourceIp = sourceIp;
	}
	public String getDestinationIp() {
		return destinationIp;
	}
	public void setDestinationIp(String destinationIp) {
		this.destinationIp = destinationIp;
	}
	public int getDestinationPort() {
		return destinationPort;
	}
	public void setDestinationPort(int destinationPort) {
		this.destinationPort = destinationPort;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isAllow() {
		return allow;
	}
	public void setAllow(boolean allow) {
		this.allow = allow;
	}
	
}
