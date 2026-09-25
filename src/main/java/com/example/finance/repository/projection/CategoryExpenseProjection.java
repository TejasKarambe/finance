package com.example.finance.repository.projection;

import java.math.BigDecimal;

public interface CategoryExpenseProjection {

    String getCategory();

    BigDecimal getTotalAmount();
}