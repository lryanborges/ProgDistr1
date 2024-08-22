package functional_interfaces;

import java.util.List;

import model.Car;

@FunctionalInterface
public interface ListCars {
	List<Car> execute();
}
