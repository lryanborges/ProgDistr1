package model;

public class IntermediaryCar extends Car {

	private static int amount;
	
	public IntermediaryCar(String renavam, String name, int category, String manufactureYear, double price) {
		super(renavam, name, category, manufactureYear, price);
	}
	
	public static int getAmount() {
		return amount;
	}

	public static void setAmount(int amount) {
		IntermediaryCar.amount = amount;
	}
	
}
