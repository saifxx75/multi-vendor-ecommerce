package com.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the real security filter chain, controllers, services and JPA (H2 in-memory DB).
 * Every test creates its own uniquely named users/products, so tests don't depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarketplaceFlowIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@marketplace.com";
    private static final String ADMIN_PASSWORD = "Admin@12345";
    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode call(HttpMethod method, String url, String token, Object body, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(method, url);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        String content = mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return (content == null || content.isBlank()) ? null : objectMapper.readTree(content);
    }

    private String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String registerCustomer(String uid) throws Exception {
        JsonNode response = call(HttpMethod.POST, "/api/auth/register", null, Map.of(
                "name", "Customer " + uid, "email", "customer-" + uid + "@example.com",
                "password", PASSWORD, "role", "CUSTOMER"), 201);
        return response.get("token").asText();
    }

    private String registerVendor(String uid) throws Exception {
        JsonNode response = call(HttpMethod.POST, "/api/auth/register", null, Map.of(
                "name", "Vendor " + uid, "email", "vendor-" + uid + "@example.com",
                "password", PASSWORD, "role", "VENDOR", "storeName", "Store " + uid), 201);
        return response.get("token").asText();
    }

    private String adminToken() throws Exception {
        JsonNode response = call(HttpMethod.POST, "/api/auth/login", null,
                Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD), 200);
        return response.get("token").asText();
    }

    private void approveVendor(String vendorToken, String adminToken) throws Exception {
        long vendorId = call(HttpMethod.GET, "/api/vendor/profile", vendorToken, null, 200).get("id").asLong();
        JsonNode approved = call(HttpMethod.PUT, "/api/admin/vendors/" + vendorId + "/approve", adminToken, null, 200);
        assertThat(approved.get("status").asText()).isEqualTo("APPROVED");
    }

    private long createCategory(String adminToken, String uid) throws Exception {
        return call(HttpMethod.POST, "/api/admin/categories", adminToken,
                Map.of("name", "Category " + uid, "description", "Test category"), 201).get("id").asLong();
    }

    private long createProduct(String vendorToken, long categoryId, String uid, String price, int stock) throws Exception {
        return call(HttpMethod.POST, "/api/vendor/products", vendorToken, Map.of(
                "name", "Gadget " + uid, "description", "A fine gadget", "price", new java.math.BigDecimal(price),
                "stock", stock, "categoryId", categoryId), 201).get("id").asLong();
    }

    /** Registers an approved vendor with one product and returns {vendorToken, productId}. */
    private Object[] vendorWithProduct(String uid, String price, int stock) throws Exception {
        String admin = adminToken();
        String vendor = registerVendor(uid);
        approveVendor(vendor, admin);
        long categoryId = createCategory(admin, uid);
        long productId = createProduct(vendor, categoryId, uid, price, stock);
        return new Object[]{vendor, productId};
    }

    private int stockOf(long productId) throws Exception {
        return call(HttpMethod.GET, "/api/products/" + productId, null, null, 200).get("stock").asInt();
    }

    @Test
    @DisplayName("a protected endpoint without a token returns 401")
    void noTokenReturns401() throws Exception {
        JsonNode error = call(HttpMethod.GET, "/api/cart", null, null, 401);
        assertThat(error.get("status").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("an invalid token returns 401")
    void badTokenReturns401() throws Exception {
        call(HttpMethod.GET, "/api/cart", "this.is.not-a-valid-token", null, 401);
    }

    @Test
    @DisplayName("a customer cannot use vendor or admin endpoints (403)")
    void customerCannotAccessVendorOrAdmin() throws Exception {
        String customer = registerCustomer(uid());
        call(HttpMethod.GET, "/api/vendor/products", customer, null, 403);
        call(HttpMethod.GET, "/api/admin/vendors", customer, null, 403);
    }

    @Test
    @DisplayName("a vendor cannot use customer or admin endpoints (403)")
    void vendorCannotAccessCustomerOrAdmin() throws Exception {
        String vendor = registerVendor(uid());
        call(HttpMethod.GET, "/api/cart", vendor, null, 403);
        call(HttpMethod.POST, "/api/admin/categories", vendor, Map.of("name", "Nope"), 403);
    }

    @Test
    @DisplayName("the public catalogue needs no token")
    void catalogueIsPublic() throws Exception {
        JsonNode page = call(HttpMethod.GET, "/api/products", null, null, 200);
        assertThat(page.has("content")).isTrue();
        assertThat(call(HttpMethod.GET, "/api/categories", null, null, 200).isArray()).isTrue();
    }

    @Test
    @DisplayName("registering the same email twice returns 409")
    void duplicateRegistration() throws Exception {
        String uid = uid();
        registerCustomer(uid);
        call(HttpMethod.POST, "/api/auth/register", null, Map.of(
                "name", "Again", "email", "customer-" + uid + "@example.com",
                "password", PASSWORD, "role", "CUSTOMER"), 409);
    }

    @Test
    @DisplayName("invalid registration data returns 400 with field errors")
    void invalidRegistration() throws Exception {
        JsonNode error = call(HttpMethod.POST, "/api/auth/register", null, Map.of(
                "name", "X", "email", "not-an-email", "password", "short", "role", "CUSTOMER"), 400);
        assertThat(error.get("fieldErrors").has("email")).isTrue();
        assertThat(error.get("fieldErrors").has("password")).isTrue();
    }

    @Test
    @DisplayName("nobody can self-register as ADMIN")
    void cannotRegisterAsAdmin() throws Exception {
        call(HttpMethod.POST, "/api/auth/register", null, Map.of(
                "name", "Mallory", "email", "mallory-" + uid() + "@example.com",
                "password", PASSWORD, "role", "ADMIN"), 400);
    }

    @Test
    @DisplayName("login works with the right password and fails with 401 otherwise")
    void login() throws Exception {
        String uid = uid();
        registerCustomer(uid);
        String email = "customer-" + uid + "@example.com";

        JsonNode ok = call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", email, "password", PASSWORD), 200);
        assertThat(ok.get("token").asText()).isNotBlank();
        assertThat(ok.get("role").asText()).isEqualTo("CUSTOMER");

        call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", email, "password", "wrong-password"), 401);
        call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", "ghost@example.com", "password", PASSWORD), 401);
    }

    @Test
    @DisplayName("an unapproved vendor cannot create products (403) until an admin approves it")
    void unapprovedVendorCannotSell() throws Exception {
        String uid = uid();
        String admin = adminToken();
        String vendor = registerVendor(uid);
        long categoryId = createCategory(admin, uid);
        Map<String, Object> product = Map.of("name", "Gadget " + uid, "description", "x",
                "price", new java.math.BigDecimal("10.00"), "stock", 1, "categoryId", categoryId);

        call(HttpMethod.POST, "/api/vendor/products", vendor, product, 403);

        approveVendor(vendor, admin);
        call(HttpMethod.POST, "/api/vendor/products", vendor, product, 201);
    }

    @Test
    @DisplayName("product validation rejects a negative price and unknown category")
    void productValidation() throws Exception {
        String uid = uid();
        String admin = adminToken();
        String vendor = registerVendor(uid);
        approveVendor(vendor, admin);
        long categoryId = createCategory(admin, uid);

        call(HttpMethod.POST, "/api/vendor/products", vendor, Map.of("name", "Bad", "price", -5,
                "stock", 1, "categoryId", categoryId), 400);
        call(HttpMethod.POST, "/api/vendor/products", vendor, Map.of("name", "Bad", "price", 5,
                "stock", 1, "categoryId", 999999), 404);
    }

    @Test
    @DisplayName("a vendor cannot modify another vendor's product (404)")
    void cannotEditForeignProduct() throws Exception {
        String uid = uid();
        Object[] first = vendorWithProduct(uid, "20.00", 5);
        long productId = (long) first[1];

        String otherUid = uid();
        String admin = adminToken();
        String otherVendor = registerVendor(otherUid);
        approveVendor(otherVendor, admin);
        long categoryId = createCategory(admin, otherUid);

        Map<String, Object> update = Map.of("name", "Hijacked", "price", new java.math.BigDecimal("1.00"),
                "stock", 1, "categoryId", categoryId);
        call(HttpMethod.PUT, "/api/vendor/products/" + productId, otherVendor, update, 404);
        call(HttpMethod.DELETE, "/api/vendor/products/" + productId, otherVendor, null, 404);
    }

    @Test
    @DisplayName("catalogue search by keyword works and soft-deleted products disappear")
    void searchAndSoftDelete() throws Exception {
        String uid = uid();
        Object[] data = vendorWithProduct(uid, "49.90", 4);
        String vendor = (String) data[0];
        long productId = (long) data[1];

        JsonNode found = call(HttpMethod.GET, "/api/products?q=" + uid, null, null, 200);
        assertThat(found.get("totalElements").asLong()).isEqualTo(1);

        call(HttpMethod.DELETE, "/api/vendor/products/" + productId, vendor, null, 204);

        assertThat(call(HttpMethod.GET, "/api/products?q=" + uid, null, null, 200).get("totalElements").asLong()).isZero();
        call(HttpMethod.GET, "/api/products/" + productId, null, null, 404);
    }

    @Test
    @DisplayName("full flow: cart -> checkout -> vendor fulfils -> order completes")
    void fullPurchaseFlow() throws Exception {
        String uid = uid();
        Object[] data = vendorWithProduct(uid, "100.00", 5);
        String vendor = (String) data[0];
        long productId = (long) data[1];
        String customer = registerCustomer(uid);

        // cart
        call(HttpMethod.POST, "/api/cart/items", customer, Map.of("productId", productId, "quantity", 2), 200);
        JsonNode cart = call(HttpMethod.GET, "/api/cart", customer, null, 200);
        assertThat(cart.get("totalItems").asInt()).isEqualTo(2);
        assertThat(cart.get("totalAmount").decimalValue()).isEqualByComparingTo("200.00");

        // checkout
        JsonNode order = call(HttpMethod.POST, "/api/orders", customer,
                Map.of("shippingAddress", "12 MG Road, Pune"), 201);
        long orderId = order.get("id").asLong();
        assertThat(order.get("status").asText()).isEqualTo("PLACED");
        assertThat(order.get("totalAmount").decimalValue()).isEqualByComparingTo("200.00");
        assertThat(stockOf(productId)).isEqualTo(3);
        assertThat(call(HttpMethod.GET, "/api/cart", customer, null, 200).get("items").size()).isZero();

        // vendor sees only its own line
        JsonNode lines = call(HttpMethod.GET, "/api/vendor/orders?status=PLACED", vendor, null, 200);
        assertThat(lines.get("totalElements").asLong()).isEqualTo(1);
        long itemId = lines.get("content").get(0).get("itemId").asLong();
        assertThat(lines.get("content").get(0).get("shippingAddress").asText()).isEqualTo("12 MG Road, Pune");

        String statusUrl = "/api/vendor/orders/items/" + itemId + "/status";
        call(HttpMethod.PUT, statusUrl, vendor, Map.of("status", "DELIVERED"), 400); // cannot skip steps
        call(HttpMethod.PUT, statusUrl, vendor, Map.of("status", "CONFIRMED"), 200);

        // once a vendor confirmed, the customer can no longer cancel
        call(HttpMethod.PUT, "/api/orders/" + orderId + "/cancel", customer, null, 400);

        call(HttpMethod.PUT, statusUrl, vendor, Map.of("status", "SHIPPED"), 200);
        call(HttpMethod.PUT, statusUrl, vendor, Map.of("status", "DELIVERED"), 200);

        JsonNode finished = call(HttpMethod.GET, "/api/orders/" + orderId, customer, null, 200);
        assertThat(finished.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(finished.get("items").get(0).get("status").asText()).isEqualTo("DELIVERED");
    }

    @Test
    @DisplayName("cancelling an order returns the stock")
    void cancelRestoresStock() throws Exception {
        String uid = uid();
        long productId = (long) vendorWithProduct(uid, "10.00", 5)[1];
        String customer = registerCustomer(uid);

        call(HttpMethod.POST, "/api/cart/items", customer, Map.of("productId", productId, "quantity", 3), 200);
        long orderId = call(HttpMethod.POST, "/api/orders", customer,
                Map.of("shippingAddress", "Somewhere 1"), 201).get("id").asLong();
        assertThat(stockOf(productId)).isEqualTo(2);

        JsonNode cancelled = call(HttpMethod.PUT, "/api/orders/" + orderId + "/cancel", customer, null, 200);
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(stockOf(productId)).isEqualTo(5);

        call(HttpMethod.PUT, "/api/orders/" + orderId + "/cancel", customer, null, 400); // not twice
    }

    @Test
    @DisplayName("cannot put more units in the cart than are in stock")
    void cannotExceedStock() throws Exception {
        String uid = uid();
        long productId = (long) vendorWithProduct(uid, "10.00", 2)[1];
        String customer = registerCustomer(uid);

        call(HttpMethod.POST, "/api/cart/items", customer, Map.of("productId", productId, "quantity", 3), 400);
        call(HttpMethod.POST, "/api/cart/items", customer, Map.of("productId", productId, "quantity", 2), 200);
        call(HttpMethod.POST, "/api/cart/items", customer, Map.of("productId", productId, "quantity", 1), 400);
    }

    @Test
    @DisplayName("checkout with an empty cart returns 400")
    void emptyCheckout() throws Exception {
        String customer = registerCustomer(uid());
        call(HttpMethod.POST, "/api/orders", customer, Map.of("shippingAddress", "Somewhere 1"), 400);
    }

    @Test
    @DisplayName("checkout fails if stock dropped after the item was added to the cart")
    void checkoutRechecksStock() throws Exception {
        String uid = uid();
        Object[] data = vendorWithProduct(uid, "10.00", 5);
        String vendor = (String) data[0];
        long productId = (long) data[1];
        String customer = registerCustomer(uid);
        call(HttpMethod.POST, "/api/cart/items", customer, Map.of("productId", productId, "quantity", 4), 200);

        // vendor lowers the stock to 1 before the customer checks out
        long categoryId = call(HttpMethod.GET, "/api/products/" + productId, null, null, 200).get("categoryId").asLong();
        call(HttpMethod.PUT, "/api/vendor/products/" + productId, vendor, Map.of("name", "Gadget " + uid,
                "price", new java.math.BigDecimal("10.00"), "stock", 1, "categoryId", categoryId), 200);

        call(HttpMethod.POST, "/api/orders", customer, Map.of("shippingAddress", "Somewhere 1"), 400);
        assertThat(stockOf(productId)).isEqualTo(1);
    }

    @Test
    @DisplayName("a customer cannot read another customer's order (404)")
    void ordersArePrivate() throws Exception {
        String uid = uid();
        long productId = (long) vendorWithProduct(uid, "10.00", 5)[1];
        String owner = registerCustomer(uid);
        String stranger = registerCustomer(uid());

        call(HttpMethod.POST, "/api/cart/items", owner, Map.of("productId", productId, "quantity", 1), 200);
        long orderId = call(HttpMethod.POST, "/api/orders", owner,
                Map.of("shippingAddress", "Somewhere 1"), 201).get("id").asLong();

        call(HttpMethod.GET, "/api/orders/" + orderId, owner, null, 200);
        call(HttpMethod.GET, "/api/orders/" + orderId, stranger, null, 404);
        call(HttpMethod.PUT, "/api/orders/" + orderId + "/cancel", stranger, null, 404);
    }

    @Test
    @DisplayName("one order can contain items from two vendors, each vendor sees only its own line")
    void multiVendorOrder() throws Exception {
        String uidA = uid();
        String uidB = uid();
        Object[] a = vendorWithProduct(uidA, "10.00", 5);
        Object[] b = vendorWithProduct(uidB, "20.00", 5);
        String customer = registerCustomer(uidA);

        call(HttpMethod.POST, "/api/cart/items", customer, Map.of("productId", (long) a[1], "quantity", 1), 200);
        call(HttpMethod.POST, "/api/cart/items", customer, Map.of("productId", (long) b[1], "quantity", 2), 200);

        JsonNode order = call(HttpMethod.POST, "/api/orders", customer, Map.of("shippingAddress", "Somewhere 1"), 201);
        assertThat(order.get("items").size()).isEqualTo(2);
        assertThat(order.get("totalAmount").decimalValue()).isEqualByComparingTo("50.00");

        JsonNode aLines = call(HttpMethod.GET, "/api/vendor/orders", (String) a[0], null, 200);
        JsonNode bLines = call(HttpMethod.GET, "/api/vendor/orders", (String) b[0], null, 200);
        assertThat(aLines.get("totalElements").asLong()).isEqualTo(1);
        assertThat(bLines.get("totalElements").asLong()).isEqualTo(1);
        assertThat(bLines.get("content").get(0).get("quantity").asInt()).isEqualTo(2);

        // Vendor A cannot touch vendor B's line
        long bItemId = bLines.get("content").get(0).get("itemId").asLong();
        call(HttpMethod.PUT, "/api/vendor/orders/items/" + bItemId + "/status", (String) a[0],
                Map.of("status", "CONFIRMED"), 404);
    }
}
