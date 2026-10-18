package com.marketplace.service;

import com.marketplace.dto.CheckoutRequest;
import com.marketplace.dto.OrderResponse;
import com.marketplace.dto.VendorOrderItemResponse;
import com.marketplace.entity.CartItem;
import com.marketplace.entity.ItemStatus;
import com.marketplace.entity.Order;
import com.marketplace.entity.OrderItem;
import com.marketplace.entity.OrderStatus;
import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.entity.Vendor;
import com.marketplace.entity.VendorStatus;
import com.marketplace.exception.BadRequestException;
import com.marketplace.repository.CartItemRepository;
import com.marketplace.repository.OrderItemRepository;
import com.marketplace.repository.OrderRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VendorService vendorService;

    @InjectMocks
    private OrderService orderService;

    private Vendor vendor(long id) {
        Vendor vendor = new Vendor();
        vendor.setId(id);
        vendor.setStoreName("Store " + id);
        vendor.setStatus(VendorStatus.APPROVED);
        return vendor;
    }

    private Product product(long id, String price, int stock, Vendor vendor) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setActive(true);
        product.setVendor(vendor);
        return product;
    }

    private CartItem cartItem(Product product, int quantity) {
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }

    private OrderItem orderItem(Product product, int quantity, ItemStatus status) {
        OrderItem item = new OrderItem();
        item.setId(100L);
        item.setProduct(product);
        item.setVendor(product.getVendor());
        item.setProductName(product.getName());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(quantity);
        item.setStatus(status);
        return item;
    }

    private Order order(long id, User customer) {
        Order order = new Order();
        order.setId(id);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PLACED);
        order.setShippingAddress("221B Baker Street");
        order.setTotalAmount(BigDecimal.TEN);
        return order;
    }

    private User customer(long id) {
        User user = new User();
        user.setId(id);
        user.setName("Customer " + id);
        return user;
    }

    @Test
    @DisplayName("checkout creates one order across vendors, totals it, reduces stock and empties the cart")
    void checkoutSuccess() {
        Product p1 = product(10L, "10.00", 5, vendor(1L));
        Product p2 = product(20L, "5.50", 3, vendor(2L));
        List<CartItem> cart = List.of(cartItem(p2, 1), cartItem(p1, 2));

        when(cartItemRepository.findByUserId(1L)).thenReturn(cart);
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(p1));
        when(productRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(p2));
        when(userRepository.getReferenceById(1L)).thenReturn(customer(1L));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.checkout(1L, new CheckoutRequest("221B Baker Street"));

        assertThat(response.totalAmount()).isEqualByComparingTo("25.50");
        assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items()).allMatch(i -> i.status() == ItemStatus.PLACED);
        assertThat(p1.getStock()).isEqualTo(3);
        assertThat(p2.getStock()).isEqualTo(2);
        verify(cartItemRepository).deleteAll(cart);
    }

    @Test
    @DisplayName("checkout snapshots the product name and price onto the order line")
    void checkoutSnapshotsPrice() {
        Product product = product(10L, "10.00", 5, vendor(1L));
        List<CartItem> cart = List.of(cartItem(product, 1));

        when(cartItemRepository.findByUserId(1L)).thenReturn(cart);
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));
        when(userRepository.getReferenceById(1L)).thenReturn(customer(1L));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.checkout(1L, new CheckoutRequest("Somewhere"));
        product.setPrice(new BigDecimal("99.00")); // vendor changes the price later

        assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo("10.00");
        assertThat(response.items().get(0).productName()).isEqualTo("Product 10");
    }

    @Test
    @DisplayName("checkout with an empty cart is rejected")
    void checkoutEmptyCart() {
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout(1L, new CheckoutRequest("Somewhere")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("checkout fails when stock is insufficient and creates no order")
    void checkoutInsufficientStock() {
        Product product = product(10L, "10.00", 1, vendor(1L));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(cartItem(product, 3)));
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));
        when(userRepository.getReferenceById(1L)).thenReturn(customer(1L));

        assertThatThrownBy(() -> orderService.checkout(1L, new CheckoutRequest("Somewhere")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
        assertThat(product.getStock()).isEqualTo(1);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("checkout fails for a product that has been deactivated")
    void checkoutInactiveProduct() {
        Product product = product(10L, "10.00", 5, vendor(1L));
        product.setActive(false);
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(cartItem(product, 1)));
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));
        when(userRepository.getReferenceById(1L)).thenReturn(customer(1L));

        assertThatThrownBy(() -> orderService.checkout(1L, new CheckoutRequest("Somewhere")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer available");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("cancelling an untouched order restocks every item and cancels the order")
    void cancelRestocks() {
        Product product = product(10L, "10.00", 3, vendor(1L));
        Order order = order(7L, customer(1L));
        OrderItem item = orderItem(product, 2, ItemStatus.PLACED);
        order.addItem(item);

        when(orderRepository.findByIdAndCustomerId(7L, 1L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));

        OrderResponse response = orderService.cancelOrder(1L, 7L);

        assertThat(product.getStock()).isEqualTo(5);
        assertThat(item.getStatus()).isEqualTo(ItemStatus.CANCELLED);
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("an order cannot be cancelled once a vendor has started processing it")
    void cancelAfterProcessingStartedIsRejected() {
        Product product = product(10L, "10.00", 3, vendor(1L));
        Order order = order(7L, customer(1L));
        order.addItem(orderItem(product, 2, ItemStatus.CONFIRMED));
        when(orderRepository.findByIdAndCustomerId(7L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer be cancelled");
        assertThat(product.getStock()).isEqualTo(3);
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("an already cancelled order cannot be cancelled twice (no double restock)")
    void cancelTwiceIsRejected() {
        Order order = order(7L, customer(1L));
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findByIdAndCustomerId(7L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already cancelled");
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("a vendor cannot skip steps in the fulfilment flow")
    void invalidTransitionIsRejected() {
        Vendor vendor = vendor(3L);
        Product product = product(10L, "10.00", 3, vendor);
        Order order = order(7L, customer(1L));
        OrderItem item = orderItem(product, 1, ItemStatus.PLACED);
        order.addItem(item);

        when(vendorService.getVendorForUser(2L)).thenReturn(vendor);
        when(orderItemRepository.findByIdAndVendorId(100L, 3L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> orderService.updateItemStatus(2L, 100L, ItemStatus.DELIVERED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PLACED")
                .hasMessageContaining("DELIVERED");
        assertThat(item.getStatus()).isEqualTo(ItemStatus.PLACED);
    }

    @Test
    @DisplayName("delivering the last open item completes the order")
    void deliveringLastItemCompletesOrder() {
        Vendor vendor = vendor(3L);
        Product product = product(10L, "10.00", 3, vendor);
        Order order = order(7L, customer(1L));
        OrderItem item = orderItem(product, 1, ItemStatus.SHIPPED);
        order.addItem(item);

        when(vendorService.getVendorForUser(2L)).thenReturn(vendor);
        when(orderItemRepository.findByIdAndVendorId(100L, 3L)).thenReturn(Optional.of(item));

        VendorOrderItemResponse response = orderService.updateItemStatus(2L, 100L, ItemStatus.DELIVERED);

        assertThat(response.status()).isEqualTo(ItemStatus.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("an order stays PLACED while another vendor's item is still open")
    void orderStaysOpenWhileOtherVendorPending() {
        Vendor vendor = vendor(3L);
        Product mine = product(10L, "10.00", 3, vendor);
        Product theirs = product(20L, "5.00", 3, vendor(4L));
        Order order = order(7L, customer(1L));
        OrderItem myItem = orderItem(mine, 1, ItemStatus.SHIPPED);
        OrderItem otherItem = orderItem(theirs, 1, ItemStatus.PLACED);
        otherItem.setId(101L);
        order.addItem(myItem);
        order.addItem(otherItem);

        when(vendorService.getVendorForUser(2L)).thenReturn(vendor);
        when(orderItemRepository.findByIdAndVendorId(100L, 3L)).thenReturn(Optional.of(myItem));

        orderService.updateItemStatus(2L, 100L, ItemStatus.DELIVERED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    @DisplayName("a vendor cancelling an item returns its stock")
    void vendorCancelRestocks() {
        Vendor vendor = vendor(3L);
        Product product = product(10L, "10.00", 3, vendor);
        Order order = order(7L, customer(1L));
        OrderItem item = orderItem(product, 2, ItemStatus.CONFIRMED);
        order.addItem(item);

        when(vendorService.getVendorForUser(2L)).thenReturn(vendor);
        when(orderItemRepository.findByIdAndVendorId(100L, 3L)).thenReturn(Optional.of(item));
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));

        orderService.updateItemStatus(2L, 100L, ItemStatus.CANCELLED);

        assertThat(product.getStock()).isEqualTo(5);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
