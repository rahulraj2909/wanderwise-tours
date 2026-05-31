package com.neovarsity.toursattractions.service;

import com.neovarsity.toursattractions.client.CatalogClient;
import com.neovarsity.toursattractions.dto.request.CreateBookingRequest;
import com.neovarsity.toursattractions.dto.request.PaxSelectionRequest;
import com.neovarsity.toursattractions.entity.User;
import com.neovarsity.toursattractions.entity.enums.UserRole;
import com.neovarsity.toursattractions.repository.BookingRepository;
import com.neovarsity.toursattractions.repository.PaymentRepository;
import com.neovarsity.wanderwise.common.dto.CatalogAttractionDto;
import com.neovarsity.wanderwise.common.dto.CatalogPaxTypeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserService userService;
    @Mock
    private CatalogClient catalogClient;
    @Mock
    private StripePaymentService stripePaymentService;

    @InjectMocks
    private BookingService bookingService;

    private User customer;
    private CatalogAttractionDto attraction;

    @BeforeEach
    void setUp() {
        customer = User.builder().id(1L).name("Demo").email("c@demo.com").role(UserRole.CUSTOMER).build();
        attraction = new CatalogAttractionDto(
                5L, "Balloon Flight", new BigDecimal("8999"), "INR", 12, true);
    }

    @Test
    void createBookingWithPaxSelectionsTotalsPerType() throws Exception {
        when(userService.getById(1L)).thenReturn(customer);
        when(catalogClient.getAttraction(5L)).thenReturn(attraction);
        when(catalogClient.findPaxByCode(5L, "ADULT")).thenReturn(
                new CatalogPaxTypeDto(1L, "ADULT", "Adult", new BigDecimal("8999"), "INR"));
        when(catalogClient.findPaxByCode(5L, "CHILD")).thenReturn(
                new CatalogPaxTypeDto(2L, "CHILD", "Child", new BigDecimal("6749"), "INR"));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stripePaymentService.createCheckoutSession(any(), any())).thenReturn("http://pay");

        CreateBookingRequest request = new CreateBookingRequest();
        request.setAttractionId(5L);
        request.setVisitDate(LocalDate.now().plusDays(3));
        request.setContactEmail("c@demo.com");
        PaxSelectionRequest adult = new PaxSelectionRequest();
        adult.setPaxTypeCode("ADULT");
        adult.setQuantity(2);
        PaxSelectionRequest child = new PaxSelectionRequest();
        child.setPaxTypeCode("CHILD");
        child.setQuantity(1);
        request.setPaxSelections(List.of(adult, child));

        var response = bookingService.createBooking(request, 1L);

        assertThat(response.getGuests()).isEqualTo(3);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("24747"));
        assertThat(response.getPaxLines()).hasSize(2);
        verify(catalogClient).findPaxByCode(eq(5L), eq("ADULT"));
    }
}
