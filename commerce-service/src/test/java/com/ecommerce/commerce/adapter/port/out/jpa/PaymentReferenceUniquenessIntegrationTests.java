package com.ecommerce.commerce.adapter.port.out.jpa;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.commerce.adapter.port.out.jpa.entity.OrderEntity;
import com.ecommerce.commerce.adapter.port.out.jpa.repository.OrderJpaRepository;
import com.ecommerce.commerce.application.port.in.ConfirmOrderPaymentUseCase;
import com.ecommerce.commerce.domain.exception.PaymentReferenceConflictException;
import com.ecommerce.commerce.domain.model.Order;
import com.ecommerce.commerce.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentReferenceUniquenessIntegrationTests {

    @Autowired
    private ConfirmOrderPaymentUseCase confirmOrderPaymentUseCase;

    @Autowired
    private OrderJpaRepository orderRepository;

    private final List<UUID> orderIds = new ArrayList<>();

    @AfterEach
    void deleteTestOrders() {
        orderRepository.deleteAllById(orderIds);
    }

    @Test
    void concurrentOrdersCannotReuseTheSameProviderReference() throws Exception {
        OrderEntity firstOrder = savePendingOrder();
        OrderEntity secondOrder = savePendingOrder();
        String providerReference = "concurrent-" + UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Object> firstAttempt = executor.submit(() -> confirmAfterStart(
                    firstOrder,
                    providerReference,
                    ready,
                    start
            ));
            Future<Object> secondAttempt = executor.submit(() -> confirmAfterStart(
                    secondOrder,
                    providerReference,
                    ready,
                    start
            ));

            assertThat(ready.await(5, SECONDS)).isTrue();
            start.countDown();

            List<Object> outcomes = List.of(
                    firstAttempt.get(10, SECONDS),
                    secondAttempt.get(10, SECONDS)
            );

            assertThat(outcomes.stream().filter(Order.class::isInstance)).hasSize(1);
            PaymentReferenceConflictException conflict = outcomes.stream()
                    .filter(PaymentReferenceConflictException.class::isInstance)
                    .map(PaymentReferenceConflictException.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertThat(conflict.getMessage())
                    .isEqualTo("Payment reference is already assigned to another order");
            assertThat(conflict.details()).containsEntry("paymentMethod", "CARD")
                    .containsEntry("providerReference", providerReference);

            assertThat(orderRepository.findAllById(orderIds))
                    .filteredOn(order -> order.getStatus() == OrderStatus.CONFIRMED)
                    .singleElement()
                    .satisfies(order -> {
                        assertThat(order.getPaymentMethod()).isEqualTo("CARD");
                        assertThat(order.getPaymentReference()).isEqualTo(providerReference);
                    });
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void retryingTheSameOrderWithTheSameReferenceIsIdempotent() {
        OrderEntity entity = savePendingOrder();
        String providerReference = "retry-" + UUID.randomUUID();
        ConfirmOrderPaymentUseCase.Command command =
                new ConfirmOrderPaymentUseCase.Command("CARD", providerReference);

        Order firstConfirmation =
                confirmOrderPaymentUseCase.confirmPayment(entity.getUserId(), entity.getId(), command);
        Order retry =
                confirmOrderPaymentUseCase.confirmPayment(entity.getUserId(), entity.getId(), command);

        assertThat(retry).isEqualTo(firstConfirmation);
        assertThat(retry.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(retry.paymentMethod()).isEqualTo("CARD");
        assertThat(retry.paymentReference()).isEqualTo(providerReference);
    }

    private Object confirmAfterStart(
            OrderEntity order,
            String providerReference,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, SECONDS)) {
            throw new IllegalStateException("Payment attempts did not start");
        }

        try {
            return confirmOrderPaymentUseCase.confirmPayment(
                    order.getUserId(),
                    order.getId(),
                    new ConfirmOrderPaymentUseCase.Command("CARD", providerReference)
            );
        } catch (PaymentReferenceConflictException exception) {
            return exception;
        }
    }

    private OrderEntity savePendingOrder() {
        OrderEntity entity = new OrderEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(UUID.randomUUID());
        entity.setStatus(OrderStatus.PENDING_PAYMENT);
        entity.setCurrency("USD");
        entity.setTotal(new BigDecimal("10.00"));
        entity.setCreatedAt(Instant.now());
        orderIds.add(entity.getId());
        return orderRepository.saveAndFlush(entity);
    }
}
