package com.jack.priceservice.schedule;

import com.jack.common.constants.ApplicationConstants;
import com.jack.priceservice.entity.BTCPriceHistory;
import com.jack.priceservice.repository.BTCPriceHistoryRepository;
import com.jack.priceservice.service.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScheduledTasksTest {

    @Mock
    private PriceService priceService;

    @Mock
    private BTCPriceHistoryRepository btcPriceHistoryRepository;

    @InjectMocks
    private ScheduledTasks scheduledTasks;

    @Captor
    private ArgumentCaptor<BTCPriceHistory> btcPriceHistoryCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Initialize the ScheduledTasks with initial price from ApplicationConstants
        ReflectionTestUtils.setField(scheduledTasks, "currentPrice", ApplicationConstants.INITIAL_PRICE);
        ReflectionTestUtils.setField(scheduledTasks, "isIncreasing", true);
    }

    @Test
    void testSaveInitialPrice() {
        // Arrange
        BTCPriceHistory initialHistory = new BTCPriceHistory();
        initialHistory.setId(1L);
        initialHistory.setPrice(BigDecimal.valueOf(ApplicationConstants.INITIAL_PRICE));
        initialHistory.setTimestamp(LocalDateTime.now());

        when(btcPriceHistoryRepository.save(any(BTCPriceHistory.class))).thenReturn(initialHistory);

        // Act
        scheduledTasks.saveInitialPrice();

        // Assert
        verify(btcPriceHistoryRepository, times(1)).save(btcPriceHistoryCaptor.capture());
        BTCPriceHistory capturedHistory = btcPriceHistoryCaptor.getValue();

        assert capturedHistory.getPrice().equals(BigDecimal.valueOf(ApplicationConstants.INITIAL_PRICE));
        // Timestamp is set to now; additional assertions can be added if necessary

        verify(priceService, times(1)).setPriceWithId(initialHistory.getId(), initialHistory.getPrice());
    }

    @Test
    void testUpdateCurrentPrice_Increasing() {
        // Arrange
        ReflectionTestUtils.setField(scheduledTasks, "isIncreasing", true);
        ReflectionTestUtils.setField(scheduledTasks, "currentPrice", 100.0);

        BTCPriceHistory savedHistory = new BTCPriceHistory();
        savedHistory.setId(2L);
        savedHistory.setPrice(BigDecimal.valueOf(110.0));
        savedHistory.setTimestamp(LocalDateTime.now());

        when(btcPriceHistoryRepository.save(any(BTCPriceHistory.class))).thenReturn(savedHistory);

        // Act
        scheduledTasks.updateCurrentPrice();

        // Assert
        double expectedPrice = 110.0;
        double actualPrice = (double) ReflectionTestUtils.getField(scheduledTasks, "currentPrice");
        boolean isIncreasing = (boolean) ReflectionTestUtils.getField(scheduledTasks, "isIncreasing");

        assert expectedPrice == actualPrice : "Price should be incremented by PRICE_INCREMENT";
        assert isIncreasing : "isIncreasing should remain true as currentPrice < MAX_PRICE";

        verify(btcPriceHistoryRepository, times(1)).save(btcPriceHistoryCaptor.capture());
        BTCPriceHistory capturedHistory = btcPriceHistoryCaptor.getValue();
        assert capturedHistory.getPrice().equals(BigDecimal.valueOf(110.0));

        verify(priceService, times(1)).setPriceWithId(savedHistory.getId(), savedHistory.getPrice());
    }

    @Test
    void testUpdateCurrentPrice_Decreasing() {
        // Arrange
        ReflectionTestUtils.setField(scheduledTasks, "isIncreasing", false);
        ReflectionTestUtils.setField(scheduledTasks, "currentPrice", 460.0);

        BTCPriceHistory savedHistory = new BTCPriceHistory();
        savedHistory.setId(3L);
        savedHistory.setPrice(BigDecimal.valueOf(450.0));
        savedHistory.setTimestamp(LocalDateTime.now());

        when(btcPriceHistoryRepository.save(any(BTCPriceHistory.class))).thenReturn(savedHistory);

        // Act
        scheduledTasks.updateCurrentPrice();

        // Assert
        double expectedPrice = 450.0;
        double actualPrice = (double) ReflectionTestUtils.getField(scheduledTasks, "currentPrice");
        boolean isIncreasing = (boolean) ReflectionTestUtils.getField(scheduledTasks, "isIncreasing");

        assert expectedPrice == actualPrice : "Price should be decremented by PRICE_INCREMENT";
        assert !isIncreasing : "isIncreasing should remain false as currentPrice > MIN_PRICE";

        verify(btcPriceHistoryRepository, times(1)).save(btcPriceHistoryCaptor.capture());
        BTCPriceHistory capturedHistory = btcPriceHistoryCaptor.getValue();
        assert capturedHistory.getPrice().equals(BigDecimal.valueOf(450.0));

        verify(priceService, times(1)).setPriceWithId(savedHistory.getId(), savedHistory.getPrice());
    }

    @Test
    void testUpdateCurrentPrice_IncreaseToMax() {
        // Arrange
        ReflectionTestUtils.setField(scheduledTasks, "isIncreasing", true);
        ReflectionTestUtils.setField(scheduledTasks, "currentPrice", 450.0); // MAX_PRICE is 460

        BTCPriceHistory savedHistory = new BTCPriceHistory();
        savedHistory.setId(4L);
        savedHistory.setPrice(BigDecimal.valueOf(460.0));
        savedHistory.setTimestamp(LocalDateTime.now());

        when(btcPriceHistoryRepository.save(any(BTCPriceHistory.class))).thenReturn(savedHistory);

        // Act
        scheduledTasks.updateCurrentPrice();

        // Assert
        double expectedPrice = 460.0;
        double actualPrice = (double) ReflectionTestUtils.getField(scheduledTasks, "currentPrice");
        boolean isIncreasing = (boolean) ReflectionTestUtils.getField(scheduledTasks, "isIncreasing");

        assert expectedPrice == actualPrice : "Price should be incremented to MAX_PRICE";
        assert !isIncreasing : "isIncreasing should be set to false as currentPrice >= MAX_PRICE";

        verify(btcPriceHistoryRepository, times(1)).save(btcPriceHistoryCaptor.capture());
        BTCPriceHistory capturedHistory = btcPriceHistoryCaptor.getValue();
        assert capturedHistory.getPrice().equals(BigDecimal.valueOf(460.0));

        verify(priceService, times(1)).setPriceWithId(savedHistory.getId(), savedHistory.getPrice());
    }

    @Test
    void testUpdateCurrentPrice_DecreaseToMin() {
        // Arrange
        ReflectionTestUtils.setField(scheduledTasks, "isIncreasing", false);
        ReflectionTestUtils.setField(scheduledTasks, "currentPrice", 110.0); // MIN_PRICE is 100

        BTCPriceHistory savedHistory = new BTCPriceHistory();
        savedHistory.setId(5L);
        savedHistory.setPrice(BigDecimal.valueOf(100.0));
        savedHistory.setTimestamp(LocalDateTime.now());

        when(btcPriceHistoryRepository.save(any(BTCPriceHistory.class))).thenReturn(savedHistory);

        // Act
        scheduledTasks.updateCurrentPrice();

        // Assert
        double expectedPrice = 100.0;
        double actualPrice = (double) ReflectionTestUtils.getField(scheduledTasks, "currentPrice");
        boolean isIncreasing = (boolean) ReflectionTestUtils.getField(scheduledTasks, "isIncreasing");

        assert expectedPrice == actualPrice : "Price should be decremented to MIN_PRICE";
        assert isIncreasing : "isIncreasing should be set to true as currentPrice <= MIN_PRICE";

        verify(btcPriceHistoryRepository, times(1)).save(btcPriceHistoryCaptor.capture());
        BTCPriceHistory capturedHistory = btcPriceHistoryCaptor.getValue();
        assert capturedHistory.getPrice().equals(BigDecimal.valueOf(100.0));

        verify(priceService, times(1)).setPriceWithId(savedHistory.getId(), savedHistory.getPrice());
    }
}
