package model;

public class EconomicCar extends Car {
	
	private static int amount;
	
	public EconomicCar(String renavam, String name, int category, String manufactureYear, double price) {
		super(renavam, name, category, manufactureYear, price);
	}

	public static int getAmount() {
		return amount;
	}

	public static void setAmount(int amount) {
		EconomicCar.amount = amount;
	}
	
}
