package market.rest;

import market.domain.Cart;
import market.domain.Distillery;
import market.domain.Order;
import market.dto.CartDTO;
import market.dto.CartItemDTO;
import market.dto.OrderDTO;
import market.dto.OrderedProductDTO;
import market.dto.assembler.CartDtoAssembler;
import market.dto.assembler.OrderDtoAssembler;
import market.exception.UnknownEntityException;
import market.properties.MarketProperties;
import market.service.OrderService;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

import javax.validation.Valid;

import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/**
 * Customer orders history.
 */
@RestController
@RequestMapping(value = "customer/orders")
@ExposesResourceFor(OrderDTO.class)
@Secured({"ROLE_USER"})
public class OrdersRestController {
	private final OrderService orderService;
	private final OrderDtoAssembler orderDtoAssembler;
	
	public OrdersRestController(OrderService orderService, MarketProperties marketProperties) {
		this.orderService = orderService;
		
		// add for test
		orderDtoAssembler = new OrderDtoAssembler();
	}

	/**
	 * View orders.
	 *
	 * @return orders list of the specified customer
	 */
	@GetMapping
	public List<OrderDTO> getOrders(Principal principal) {
		return orderService.getUserOrders(principal.getName()).stream()
			.map(orderDtoAssembler::toModel)
			.collect(toList());
	}

	/**
	 * View a single order.
	 *
	 * @return order of the specified customer
	 * @throws UnknownEntityException if the order with the specified id doesn't exist
	 */
	@GetMapping(value = "/{orderId}")
	public OrderDTO getOrder(Principal principal, @PathVariable long orderId) {
		String login = principal.getName();
		return orderService.getUserOrder(login, orderId)
			.map(orderDtoAssembler::toModel)
			.orElseThrow(() -> new UnknownEntityException(OrderDTO.class, orderId));
	}
	
	/*
	 *  New method for test: endpoint (POST) to add a new order in the database
	 */
	
	/**
	 * New method for testing
	 * Adding a new order.
	 */
	@PostMapping
	public ResponseEntity<OrderDTO> createNewOrder(Principal principal, @RequestBody @Valid OrderDTO orderdto) {
		String login = principal.getName();
		Order order = orderDtoAssembler.toDomain(orderdto);
		Order order = orderService.createNewOrder(login, order);
		OrderDTO dto = orderDtoAssembler.toModel(order);

		HttpHeaders headers = new HttpHeaders();
		return new ResponseEntity<>(dto, headers, HttpStatus.CREATED);
	}
	
	/**
	 * New method for testing
	 * Convert a order to a OrderDTO
	 */
	private OrderDTO toDto(Order order) {
		OrderDTO dto = orderDtoAssembler.toModel(order);
		for (OrderedProductDTO orderedProductDto : dto.getOrderedProducts())
			orderedProductDto.add(linkTo(ProductsRestController.class).slash(orderedProductDto.getProductId()).withRel("View product"));
		return dto;
	}

}
