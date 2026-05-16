package com.flashsale.ordersystem.order.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.ordersystem.order.adapter.rest.controller.PurchaseController;
import com.flashsale.ordersystem.order.adapter.rest.dto.PurchaseRequest;
import com.flashsale.ordersystem.order.adapter.rest.dto.PurchaseResponse;
import com.flashsale.ordersystem.order.service.OrderService;
import com.flashsale.ordersystem.shared.exception.BusinessException;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import com.flashsale.ordersystem.shared.service.MetricsService;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PurchaseController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.oauth2.resource.servlet
                        .OAuth2ResourceServerAutoConfiguration.class
        }
)
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private MetricsService metricsService;

    @MockBean
    @SuppressWarnings("rawtypes")
    private ProxyManager proxyManager;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void stubRateLimiter() {
        // RateLimitFilter calls:
        //   proxyManager.builder().build(key, supplier).tryConsumeAndReturnRemaining(1)
        // build() returns BucketProxy (not Bucket), so we must mock BucketProxy.
        RemoteBucketBuilder bucketBuilder = mock(RemoteBucketBuilder.class);
        BucketProxy bucketProxy = mock(BucketProxy.class);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);

        when(proxyManager.builder()).thenReturn(bucketBuilder);
        doReturn(bucketProxy).when(bucketBuilder).build(any(), any(Supplier.class));
        when(bucketProxy.tryConsumeAndReturnRemaining(anyLong())).thenReturn(probe);
        when(probe.isConsumed()).thenReturn(true);
    }

    @Test
    void shouldPurchaseSuccessfully() throws Exception {
        PurchaseRequest request = new PurchaseRequest(100L);

        PurchaseResponse response = new PurchaseResponse(
                "event-1",
                "PENDING",
                "order is being processed"
        );

        when(orderService.purchase(anyString(), eq(1L), eq(100L)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/sales/1/purchase")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(csrf())
                                .with(jwt().jwt(jwt -> jwt.subject("user-1")))
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService).purchase(anyString(), eq(1L), eq(100L));
    }

    @Test
    void shouldReturnInsufficientStock() throws Exception {
        PurchaseRequest request = new PurchaseRequest(100L);

        when(orderService.purchase(anyString(), anyLong(), anyLong()))
                .thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_STOCK));

        mockMvc.perform(
                        post("/api/v1/sales/1/purchase")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(csrf())
                                .with(jwt().jwt(jwt -> jwt.subject("user-1")))
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldFailValidationWhenProductIdMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/sales/1/purchase")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(csrf())
                                .with(jwt().jwt(jwt -> jwt.subject("user-1")))
                                .content("{}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }
}