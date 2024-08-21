package model;

public class Client extends User {

	public Client(String newCpf, String newPassword, String newName) {
		super.cpf = newCpf;
		super.password = newPassword;
		super.name = newName;
		super.isEmployee = false;
	}
	
}
