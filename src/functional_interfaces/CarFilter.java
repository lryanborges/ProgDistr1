package functional_interfaces;

import model.Car;

public interface CarFilter {
	boolean execute(Car car);
}
