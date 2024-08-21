package server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import datagrams.Message;
import model.Car;
import model.Keys;
import model.RSAKeys;
import model.User;

public interface GatewayInterface extends Remote {

	public void register(User newUser) throws RemoteException;
	public User login(String cpf, String password) throws RemoteException;
	
	/*public void addCar(Car newCar) throws RemoteException;
	public void editCar(String renavam, Car editedCar) throws RemoteException;
	public void deleteCar(String renavam) throws RemoteException;
	public void deleteCars(String name) throws RemoteException;
	public List<Car> listCars() throws RemoteException;
	public List<Car> listCars(int category) throws RemoteException;
	public Car searchCar(String renavam) throws RemoteException;
	public List<Car> searchCars(String category) throws RemoteException;
	public Car buyCar(String renavam) throws RemoteException;
	public int getAmount(int category) throws RemoteException;*/
	public void putToSleep() throws RemoteException;
	
	public void addNewClientKeys(Integer clientNumber, Keys newClientKeys) throws RemoteException;
	public RSAKeys getRSAKeys() throws RemoteException;
	public Message<String> receiveMessage(Message<String> msg) throws RemoteException;
	
}
