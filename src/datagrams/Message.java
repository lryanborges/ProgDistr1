package datagrams;

import java.io.Serializable;
import java.util.List;

public class Message<T> implements Serializable {
	
	private int clientSendingMsg;
	private int operation;
	private T content;
	private String messageSignature;
	private List<T> listContent; 
	
	public Message(int op, T acc) {
		this.operation = op;
		this.content = acc;
	}
	
	public Message(int op, T acc, String hmac) {
		this.operation = op;
		this.content = acc;
		this.messageSignature = hmac;
	}
	
	public Message(int op, List<T> listAcc, String hmac) {
		this.operation = op;
		this.listContent = listAcc;
		this.messageSignature = hmac;
	}
	
	public Message(int op, T acc, int clientNumber) {
		this.operation = op;
		this.content = acc;
		this.clientSendingMsg = clientNumber;
	}
	
	public Message(int op, T acc, String hmac, int clientNumber) {
		this.operation = op;
		this.content = acc;
		this.messageSignature = hmac;
		this.clientSendingMsg = clientNumber;
	}
	
	public int getOperation() {
		return operation;
	}
	public void setOperation(int operation) {
		this.operation = operation;
	}
	public T getContent() {
		return content;
	}
	public void setContent(T content) {
		this.content = content;
	}
	public String getMessageSignature() {
		return messageSignature;
	}
	public void setMessageSignature(String messageHmac) {
		this.messageSignature = messageHmac;
	}
	public int getClientSendingMsg() {
		return clientSendingMsg;
	}
	public void setClientSendingMsg(int processSendingMsg) {
		this.clientSendingMsg = processSendingMsg;
	}
	public List<T> getListContent() {
		return listContent;
	}
	public void setListContent(List<T> listContent) {
		this.listContent = listContent;
	}
		
}
