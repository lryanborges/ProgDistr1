package model;

import java.io.Serializable;

public class Car implements Serializable {

	private String renavam;
	private String name;
	private int category;
	private String manufactureYear;
	private double price;
	
	public Car(String renavam, String name, int category, String manufactureYear, double price) {
		this.renavam = renavam;
		this.name = name;
		this.category = category;
		this.manufactureYear = manufactureYear;
		this.price = price;
	}
	
	public String getRenavam() {
		return renavam;
	}
	public void setRenavam(String renavam) {
		this.renavam = renavam;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getCategory() {
		return category;
	}
	public String getStringCategory() {
		switch(category) {
		case 1:
			return "Econômico";
		case 2:
			return "Intermediário";
		case 3:
			return "Executivo";
		default:
			return "Tipo desconhecido";
		}
	}
	public void setCategory(int category) {
		this.category = category;
	}
	public String getManufactureYear() {
		return manufactureYear;
	}
	public void setManufactureYear(String manufactureYear) {
		this.manufactureYear = manufactureYear;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}

	@Override
	public String toString() {
		return renavam + "°" + name + "°" + category + "°"
				+ manufactureYear + "°" + price;
	}
	
}
