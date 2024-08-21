package server.authentication;

import java.rmi.Remote;
import java.rmi.RemoteException;

import model.User;

public interface AuthInterface extends Remote {

	//void startServer() throws RemoteException;
	User loginUser(String cpf, String password) throws RemoteException;
	void registerUser(User newUser) throws RemoteException;
	
}
