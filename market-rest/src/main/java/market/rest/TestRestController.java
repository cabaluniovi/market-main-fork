package market.rest;

import java.sql.SQLException;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import market.service.TestService;

@RestController
@RequestMapping(value = "test")
public class TestRestController {
	private final TestService testService;
	
	public TestRestController(TestService testService) {
		this.testService = testService;
	}
	/**
	 * Get all the existing data
	 */
	@GetMapping
	public Object getAll() {
		final Object allData = testService.getAll();
		
		return allData;
	}
	
	/**
	 * Delete all the existing data
	 */
	@DeleteMapping
	public void deleteAll() throws SQLException {
		testService.deleteAll();
	}
}
