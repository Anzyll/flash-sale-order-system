package product;

import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.product.presentation.dto.CreateProductRequest;
import com.flashsale.ordersystem.product.presentation.dto.ProductResponse;
import com.flashsale.ordersystem.product.repository.ProductRepository;
import com.flashsale.ordersystem.product.service.ProductService;
import com.flashsale.ordersystem.shared.exception.BusinessException;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProductSuccessfully() {
        CreateProductRequest request =
                new CreateProductRequest(
                        "iPhone",
                        "17 pro",
                        BigDecimal.valueOf(50000)
                );
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone");

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        ProductResponse response =
                productService.createProduct(request);

        assertEquals("iPhone", response.name());

        verify(productRepository)
                .save(any(Product.class));
    }

    @Test
    void shouldThrowWhenProductNotFound() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService.getProductOrThrow(1L)
                );

        assertEquals(
                ErrorCode.PRODUCT_NOT_FOUND,
                exception.getErrorCode()
        );
    }
}
