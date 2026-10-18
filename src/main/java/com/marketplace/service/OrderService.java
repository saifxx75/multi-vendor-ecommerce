package com.marketplace.service;

import com.marketplace.dto.CheckoutRequest;
import com.marketplace.dto.OrderResponse;
import com.marketplace.dto.PageResponse;
import com.marketplace.dto.VendorOrderItemResponse;
import com.marketplace.entity.CartItem;
import com.marketplace.entity.ItemStatus;
import com.marketplace.entity.Order;
import com.marketplace.entity.OrderItem;
import com.marketplace.entity.OrderStatus;
import com.marketplace.entity.Product;
import com.marketplace.entity.Vendor;
import com.marketplace.exception.BadRequestException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.CartItemRepository;
import com.marketplace.repository.OrderItemRepository;
import com.marketplace.repository.OrderRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Core business logic: checkout, cancellation and per-vendor fulfilment.
 *
 * Key rules:
 *  - checkout is atomic: either every line is reserved and the order is created, or nothing changes;
 *  - stock rows are locked (SELECT ... FOR UPDATE) in ascending product-id order, which prevents both
 *    overselling and deadlocks between concurrent checkouts;
 *  - price and product name are copied onto the order line (historical accuracy);
 *  - each vendor only sees and updates its own lines; the order status is derived from its lines.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final VendorService vendorService;

    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Order order = new Order();
        order.setCustomer(userRepository.getReferenceById(userId));
        order.setShippingAddress(request.shippingAddress().trim());
        order.setStatus(OrderStatus.PLACED);

        List<CartItem> lockOrder = new ArrayList<>(cartItems);
        lockOrder.sort(Comparator.comparing((CartItem ci) -> ci.getProduct().getId()));

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : lockOrder) {
            Product product = lockProduct(cartItem.getProduct().getId());
            if (!product.isActive()) {
                throw new BadRequestException("'" + product.getName() + "' is no longer available");
            }
            if (product.getStock() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for '" + product.getName()
                        + "'. Available: " + product.getStock());
            }
            product.setStock(product.getStock() - cartItem.getQuantity());

            OrderItem line = new OrderItem();
            line.setProduct(product);
            line.setVendor(product.getVendor());
            line.setProductName(product.getName());
            line.setUnitPrice(product.getPrice());
            line.setQuantity(cartItem.getQuantity());
            line.setStatus(ItemStatus.PLACED);
            order.addItem(line);

            total = total.add(line.getLineTotal());
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);
        return Mapper.toOrder(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> myOrders(Long userId, Pageable pageable) {
        Page<Order> page = orderRepository.findByCustomerId(userId, pageable);
        return PageResponse.from(page.map(Mapper::toOrder));
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        return Mapper.toOrder(findOwnOrder(userId, orderId));
    }

    /** A customer can cancel only while no vendor has started processing any line. Stock is returned. */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = findOwnOrder(userId, orderId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }
        boolean untouched = order.getItems().stream().allMatch(i -> i.getStatus() == ItemStatus.PLACED);
        if (!untouched) {
            throw new BadRequestException("Order can no longer be cancelled: a vendor has already started processing it");
        }

        List<OrderItem> lockOrder = new ArrayList<>(order.getItems());
        lockOrder.sort(Comparator.comparing((OrderItem i) -> i.getProduct().getId()));
        for (OrderItem item : lockOrder) {
            restock(item);
            item.setStatus(ItemStatus.CANCELLED);
        }
        order.setStatus(OrderStatus.CANCELLED);
        return Mapper.toOrder(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<VendorOrderItemResponse> vendorOrderItems(Long userId, ItemStatus status, Pageable pageable) {
        Vendor vendor = vendorService.getVendorForUser(userId);
        Page<OrderItem> page = status == null
                ? orderItemRepository.findByVendorId(vendor.getId(), pageable)
                : orderItemRepository.findByVendorIdAndStatus(vendor.getId(), status, pageable);
        return PageResponse.from(page.map(Mapper::toVendorOrderItem));
    }

    @Transactional
    public VendorOrderItemResponse updateItemStatus(Long userId, Long itemId, ItemStatus newStatus) {
        Vendor vendor = vendorService.getVendorForUser(userId);
        OrderItem item = orderItemRepository.findByIdAndVendorId(itemId, vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

        if (!item.getStatus().canTransitionTo(newStatus)) {
            throw new BadRequestException("Cannot change item status from " + item.getStatus() + " to " + newStatus);
        }
        if (newStatus == ItemStatus.CANCELLED) {
            restock(item);
        }
        item.setStatus(newStatus);
        refreshOrderStatus(item.getOrder());
        return Mapper.toVendorOrderItem(item);
    }

    private Order findOwnOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndCustomerId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private Product lockProduct(Long productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new BadRequestException("A product in your cart no longer exists"));
    }

    private void restock(OrderItem item) {
        Product product = lockProduct(item.getProduct().getId());
        product.setStock(product.getStock() + item.getQuantity());
    }

    /** CANCELLED if every line is cancelled; COMPLETED if every line is delivered or cancelled; otherwise PLACED. */
    private void refreshOrderStatus(Order order) {
        List<OrderItem> items = order.getItems();
        boolean allCancelled = items.stream().allMatch(i -> i.getStatus() == ItemStatus.CANCELLED);
        boolean allFinished = items.stream()
                .allMatch(i -> i.getStatus() == ItemStatus.DELIVERED || i.getStatus() == ItemStatus.CANCELLED);

        if (allCancelled) {
            order.setStatus(OrderStatus.CANCELLED);
        } else if (allFinished) {
            order.setStatus(OrderStatus.COMPLETED);
        } else {
            order.setStatus(OrderStatus.PLACED);
        }
    }
}
