package com.flashsale.ordersystem.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.ordersystem.order.adapter.rest.controller.PurchaseController;
import com.flashsale.ordersystem.order.adapter.rest.dto.PurchaseRequest;
import com.flashsale.ordersystem.order.adapter.rest.dto.PurchaseResponse;
import com.flashsale.ordersystem.order.service.OrderService;
import com.flashsale.ordersystem.shared.exception.BusinessException;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(PurchaseController.class)
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private OrderService orderService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldPurchaseSuccessfully() throws Exception {

        PurchaseRequest request =
                new PurchaseRequest(100L);

        PurchaseResponse response =
                new PurchaseResponse(
                        "event-1",
                        "PENDING",
                        "order is being processed"
                );

        when(orderService.purchase(
                anyString(),
                eq(1L),
                eq(100L)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/sales/1/purchase")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(csrf())
                                .with(jwt().jwt(jwt -> jwt.subject("user-1")))
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status")
                        .value("PENDING"));

        verify(orderService)
                .purchase(anyString(), eq(1L), eq(100L));
    }

    @Test
    void shouldReturnInsufficientStock() throws Exception {
        PurchaseRequest request =
                new PurchaseRequest(100L);

        when(orderService.purchase(
                anyString(),
                anyLong(),
                anyLong()
        )).thenThrow(
                new BusinessException(
                        ErrorCode.INSUFFICIENT_STOCK
                )
        );

        mockMvc.perform(
                        post("/api/v1/sales/1/purchase")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(csrf())
                                .with(jwt().jwt(jwt -> jwt.subject("user-1")))
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldFailValidationWhenProductIdMissing() throws Exception {

        String invalidRequest = """
                {
                }
                """;

        mockMvc.perform(
                        post("/api/v1/sales/1/purchase")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(csrf())
                                .with(jwt().jwt(jwt -> jwt.subject("user-1")))
                                .content(invalidRequest)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }
}