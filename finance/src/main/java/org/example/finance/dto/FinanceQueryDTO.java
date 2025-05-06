package org.example.finance.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FinanceQueryDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private String category;
    private Double minAmount;
    private Double maxAmount;
    private String type; // "income" 或 "expense"
}