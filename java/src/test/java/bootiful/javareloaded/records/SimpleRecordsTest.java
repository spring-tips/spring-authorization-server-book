package bootiful.javareloaded.records;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class SimpleRecordsTest {

	// <1>
	record Customer(Integer id, String name) {
	}

	record Order(Integer id, double total) {
	}

	record CustomerOrders(Customer customer, List<Order> orders) {
	}

	@Test
	void records() {
		var customer = new Customer(253, "Tammie");
		var order1 = new Order(2232, 74.023);
		var order2 = new Order(9593, 23.44);
		var cos = new CustomerOrders(customer, List.of(order1, order2));
		Assertions.assertEquals(order1.id(), 2232);
		Assertions.assertEquals(order1.total(), 74.023);
		Assertions.assertEquals(customer.name(), "Tammie");
		Assertions.assertEquals(cos.orders().size(), 2);
		System.out.println("order components  " +  order1.id() + ':' + order1.total()); // <2>
	}

}
