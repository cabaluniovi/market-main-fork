package market.service.impl;

import market.dao.OrderDAO;
import market.domain.Bill;
import market.domain.Cart;
import market.domain.CartItem;
import market.domain.Order;
import market.domain.OrderedProduct;
import market.domain.UserAccount;
import market.dto.UserOrderTotalDTO;
import market.exception.EmptyCartException;
import market.service.CartService;
import market.service.OrderService;
import market.service.UserAccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
public class OrderServiceImpl implements OrderService {

	private final OrderDAO orderDAO;
	private final UserAccountService userAccountService;
	private final CartService cartService;

	public OrderServiceImpl(OrderDAO orderDAO, UserAccountService userAccountService, CartService cartService) {
		this.orderDAO = orderDAO;
		this.userAccountService = userAccountService;
		this.cartService = cartService;
	}

	@Transactional(readOnly = true)
	@Override
	public List<Order> getUserOrders(String userLogin) {
		UserAccount account = userAccountService.findByEmail(userLogin);
		return orderDAO.findByUserAccountOrderByDateCreatedDesc(account);
	}

	@Transactional(readOnly = true)
	@Override
	public Optional<Order> getUserOrder(String userLogin, long orderId) {
		// todo: add user check
		return orderDAO.findById(orderId);
	}

	@Transactional(readOnly = true)
	@Override
	public Page<Order> fetchFiltered(String executed, String orderAgeInDays, PageRequest request) {
		Date startTime = new Date();
		if (!"all".equals(orderAgeInDays)) {
			int days = Integer.parseInt(orderAgeInDays);
			Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			c.add(Calendar.HOUR_OF_DAY, -(days * 24));
			startTime = c.getTime();
		}
		if (!"all".equals(executed) && !"all".equals(orderAgeInDays)) {
			boolean executedState = Boolean.parseBoolean(executed);
			return orderDAO.findByExecutedAndDateCreatedGreaterThan(executedState, startTime, request);
		} else if (!"all".equals(executed)) {
			boolean executedState = Boolean.parseBoolean(executed);
			return orderDAO.findByExecuted(executedState, request);
		} else if (!"all".equals(orderAgeInDays)) {
			return orderDAO.findByDateCreatedGreaterThan(startTime, request);
		} else {
			return orderDAO.findAll(request);
		}
	}

	@Transactional
	@Override
	public Order createUserOrder(String userLogin, int deliveryCost, String cardNumber) {
		Cart cart = cartService.getCartOrCreate(userLogin);
		if (cart.isEmpty())
			throw new EmptyCartException();

		Order order = createNewOrder(userLogin, cart, deliveryCost);
		Bill bill = createBill(order, cardNumber);
		order.setBill(bill);
		orderDAO.saveAndFlush(order);

		fillOrderItems(cart, order);
		orderDAO.save(order);
		cartService.clearCart(userLogin);

		return order;
	}

	@Override
	public void updateStatus(long orderId, boolean executed) {
		Order order = orderDAO.findById(orderId).orElse(null);
		if (order != null) {
			order.setExecuted(executed);
			orderDAO.save(order);
		}
	}

	private Order createNewOrder(String userLogin, Cart cart, int deliveryCost) {
		return new Order.Builder()
			.setDeliveryIncluded(cart.isDeliveryIncluded())
			.setDeliveryCost(cart.isDeliveryIncluded() ? deliveryCost : 0)
			.setUserAccount(userAccountService.findByEmail(userLogin))
			.setProductsCost(cart.getItemsCost())
			.setDateCreated(new Date())
			.setExecuted(false)
			.build();
	}
	
	private Bill createBill(Order order, String cardNumber) {
		return new Bill.Builder()
			.setOrder(order)
			.setNumber(new Random().nextInt(999999999))
			.setTotalCost(order.getProductsCost() + order.getDeliveryCost())
			.setPayed(true)
			.setDateCreated(new Date())
			.setCcNumber(cardNumber)
			.build();
	}

	private void fillOrderItems(Cart cart, Order order) {
		Set<OrderedProduct> ordered = cart.getCartItems().stream()
			.map(item -> createOrderedProduct(order, item))
			.collect(toSet());
		order.setOrderedProducts(ordered);
	}

	private OrderedProduct createOrderedProduct(Order order, CartItem item) {
		return new OrderedProduct.Builder()
			.setProduct(item.getProduct())
			.setOrder(order)
			.setQuantity(item.getQuantity())
			.build();
	}

	// Add for test
	@Override
	public Order createOrder(String userLogin, Order o,String ccNumber) {
		Order order = new Order.Builder()
				.setDeliveryIncluded(o.isDeliveryIncluded())
				.setDeliveryCost(o.isDeliveryIncluded() ? o.getDeliveryCost() : 0)
				.setProductsCost(0)
				.setDateCreated(o.getDateCreated())
				.setExecuted(o.isExecuted())
				.setUserAccount(userAccountService.findByEmail(userLogin))
				.build();
		
		Bill bill = createBill(order, ccNumber);
		order.setBill(bill);
		orderDAO.saveAndFlush(order);
		
		return order;
	}
	//add for test
	@Transactional(readOnly = true)
	@Override
	public Order getOrder(long orderId) {
		// todo: add user check
		return orderDAO.findById(orderId).orElse(null);
	}

	//add for test
	@Override
	public Order updateOrder(Order order, OrderedProduct op) {
		double increment = op.getQuantity()*op.getProduct().getPrice();
		
		Set<OrderedProduct> ordered = order.getOrderedProducts();
		ordered.add(createOrderedProduct(order, op));
		order.setOrderedProducts(ordered);
		order.setProductsCost(order.getProductsCost()+ increment);
		order.setBill(updateBill(order));
		
		orderDAO.saveAndFlush(order);
		
		return order;
	}
	
	//add for test
	private Bill updateBill(Order order) {
		Bill b = order.getBill();
		b.setTotalCost(order.getProductsCost() + order.getDeliveryCost());
		return b;
	}
	
	//add for test
	private OrderedProduct createOrderedProduct(Order order, OrderedProduct item) {
		return new OrderedProduct.Builder()
			.setProduct(item.getProduct())
			.setOrder(order)
			.setQuantity(item.getQuantity())
			.build();
	}
	
	//testing: add to get the total costs of an user's order
	public List<UserOrderTotalDTO> getTotalAmountByUser() {
        return orderDAO.findTotalAmountByUser();
    }
}
