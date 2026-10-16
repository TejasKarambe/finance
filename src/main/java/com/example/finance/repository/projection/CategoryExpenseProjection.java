package com.example.finance.repository.projection;

import java.math.BigDecimal;

public interface CategoryExpenseProjection {
    // Projection is used to retrieve specific columns from the database

    String getCategory();

    BigDecimal getTotalAmount();
}