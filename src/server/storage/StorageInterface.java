package server.storage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import datagrams.Message;
import datagrams.Permission;
import model.Car;
import model.Keys;
import model.RSAKeys;

public interface StorageInterface extends Remote {

	public void addCar(Car newCar) throws RemoteException;
	public void editCar(String renavam, Car editedCar) throws RemoteException;
	public void deleteCar(String renavam) throws RemoteException;
	public void deleteCars(String name) throws RemoteException;
	public List<Car> listCars() throws RemoteException;
	public List<Car> listCars(int category) throws RemoteException;
	public Car searchCar(String renavam) throws RemoteException;
	public List<Car> searchCars(String category) throws RemoteException;
	public Car buyCar(String renavam) throws RemoteException;
	public int getAmount(int category) throws RemoteException;
	public ServerRole getRole() throws RemoteException;
	public void setRole(ServerRole ro) throws RemoteException;
	public int getId() throws RemoteException;
	public void setId(int id) throws RemoteException;
	public StorageInterface startElections() throws RemoteException;
	public void setFollowers() throws RemoteException;
	
	public void addNewClientKeys(Keys newClientKeys) throws RemoteException;
	public RSAKeys getRSAKeys() throws RemoteException;
	public Message<String> receiveMessage(Message<String> msg) throws RemoteException;	
	
	public void setPermission(Permission permission) throws RemoteException;
}
