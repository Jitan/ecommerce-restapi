package se.groupone.ecommerce.test;

import se.groupone.ecommerce.model.*;
import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

public class CustomerTest
{
	Customer cu = new Customer("Kira", "elf", "erik.welander@hotmail.com", "Erik", "Welander", "Tersv", "073");

	@Test
	public void get()
	{
		assertEquals(cu.getUsername(), "Kira");
		assertEquals(cu.getPassword(), "elf");
		assertEquals(cu.getEmail(), "erik.welander@hotmail.com");
		assertEquals(cu.getFirstName(), "Erik");
		assertEquals(cu.getLastName(), "Welander");
		assertEquals(cu.getAddress(), "Tersv");
		assertEquals(cu.getMobileNumber(), "073");
		assertEquals(cu.toString(), "Erik Welander");
		assertNull(cu.getOrder(0));
		assertEquals(cu.getOrders(), new ArrayList<Order>());
	}

	@Test
	public void set()
	{
		cu.setPassword("Keff");
		cu.setEmail("steffekeff@n");
		cu.setFirstName("Stefan");
		cu.setLastName("De Geer");
		cu.setAddress("SommarBo 228");
		cu.setMobileNumber("0768646474");

		assertEquals(cu.getPassword(), "Keff");
		assertEquals(cu.getEmail(), "steffekeff@n");
		assertEquals(cu.getFirstName(), "Stefan");
		assertEquals(cu.getLastName(), "De Geer");
		assertEquals(cu.getAddress(), "SommarBo 228");
		assertEquals(cu.getMobileNumber(), "0768646474");
	}

	@Test
	public void shopping()
	{
		cu.addOrder();
		assertNull(cu.getOrder(0));
		assertEquals(cu.getOrders(), new ArrayList<String>());

		Product p1 = new Product("Klassisk pannkaka", "Pannkakor", "Stefan", "Vår klassiska och mycket utsökta pannkaka", "klassiskPannkaka.png", 10.90, 10);
		cu.addProduct(p1.getTitle());

		ArrayList<String> pa = cu.getShoppingCart();
		assertEquals(pa.get(0), p1.getTitle());
		assertEquals(pa.size(), 1);

		Product p2 = new Product("Amerikansk pannkaka", "Pannkakor", "Erik", "En lite tjockare men mycket god pannkaka som passar till sirap", "amerikanskPannkaka.png",
				13.90, 10);
		cu.addProduct(p2.getTitle());

		assertEquals(pa.get(1), p2.getTitle());
		assertEquals(pa.size(), 2);

		cu.removeProduct(p2.getTitle());
		assertEquals(pa.size(), 1);

		cu.addOrder();

		assertNotNull(cu.getOrder(0));

	}
}
