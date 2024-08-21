package model;

public class Employee extends User {

	public Employee(String newCpf, String newPassword, String newName) {
		super.cpf = newCpf;
		super.password = newPassword;
		super.name = newName;
		super.isEmployee = true;
	}
	
	public Car addCar(Car newCar) {
		return newCar;
	}
	
	public Car editCar(Car car) {
		return car;
	}
	
	public boolean deleteCar(Car carToDelete) {
		return false;
	}
	
	public boolean deleteCar(long renavamToDelete) {
		return false;
	}
}
