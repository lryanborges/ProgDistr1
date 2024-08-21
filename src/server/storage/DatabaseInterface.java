package server.storage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import model.Car;

public interface DatabaseInterface extends Remote {
    void addCar(Car newCar) throws RemoteException;
    void editCar(String renavam, Car editedCar) throws RemoteException;
    void deleteCar(String renavam) throws RemoteException;
    void deleteCars(String name) throws RemoteException;
    List<Car> listCars() throws RemoteException;
    List<Car> listCars(int category) throws RemoteException;
    Car searchCar(String renavam) throws RemoteException;
    List<Car> searchCars(String name) throws RemoteException;
    Car buyCar(String renavam) throws RemoteException;
    int getAmount(int category) throws RemoteException;
    void attServer() throws RemoteException;
}
