package com.example.finance.service;

import com.example.finance.dto.request.TransactionFilterRequest;
import com.example.finance.dto.request.TransactionRequest;
import com.example.finance.dto.request.TransactionUpdateRequest;
import com.example.finance.dto.response.TransactionResponse;
import com.example.finance.entity.Account;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import com.example.finance.exception.AccountNotFoundException;
import com.example.finance.exception.InsufficientBalanceException;
import com.example.finance.exception.TransactionNotFoundException;
import com.example.finance.repository.AccountRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.specification.TransactionSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.finance.dto.response.CsvImportResponse;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, Long userId) {
        Account account;
        if (userId != null) {
            account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                    .orElseThrow(() -> new AccountNotFoundException("Account Not Found or access denied"));
        } else {
            account = accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new AccountNotFoundException("Account Not Found"));
        }

        if (request.type() == TransactionType.EXPENSE
                && account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient account balance");
        }

        if (request.type() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(request.amount()));
        } else if (request.type() == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().subtract(request.amount()));
        }

        String paymentMethod = request.paymentMethod() != null && !request.paymentMethod().isBlank()
                ? request.paymentMethod()
                : "OTHER";

        Transaction transaction = new Transaction(
                request.amount(),
                request.type(),
                request.category(),
                request.description(),
                paymentMethod,
                request.transactionDate(),
                LocalDateTime.now(),
                account,
                account.getUser()
        );

        Transaction savedTransaction = transactionRepository.save(transaction);
        accountRepository.save(account);

        return mapToResponse(savedTransaction);
    }

    public TransactionResponse createTransaction(TransactionRequest request) {
        return createTransaction(request, null);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(TransactionFilterRequest filter, Pageable pageable, Long userId) {
        Specification<Transaction> spec = (root, query, cb) -> cb.conjunction();

        if (userId != null) {
            spec = spec.and(TransactionSpecification.hasUserId(userId));
        }

        if (filter != null) {
            if (filter.search() != null && !filter.search().isBlank()) {
                spec = spec.and(TransactionSpecification.descriptionContains(filter.search()));
            }
            if (filter.type() != null) {
                spec = spec.and(TransactionSpecification.hasType(filter.type()));
            }
            if (filter.category() != null && !filter.category().isBlank()) {
                spec = spec.and(TransactionSpecification.hasCategory(filter.category()));
            }
            if (filter.accountId() != null) {
                spec = spec.and(TransactionSpecification.hasAccountId(filter.accountId()));
            }
            if (filter.minAmount() != null) {
                spec = spec.and(TransactionSpecification.amountGreaterThanOrEqualTo(filter.minAmount()));
            }
            if (filter.maxAmount() != null) {
                spec = spec.and(TransactionSpecification.amountLessThanOrEqualTo(filter.maxAmount()));
            }
            if (filter.startDate() != null) {
                spec = spec.and(TransactionSpecification.dateGreaterThanOrEqualTo(filter.startDate()));
            }
            if (filter.endDate() != null) {
                spec = spec.and(TransactionSpecification.dateLessThanOrEqualTo(filter.endDate()));
            }
        }

        return transactionRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    public Page<TransactionResponse> getTransactions(Pageable pageable) {
        return getTransactions(null, pageable, null);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id, Long userId) {
        Transaction transaction;
        if (userId != null) {
            transaction = transactionRepository.findByIdAndAccountUserId(id, userId)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction Not Found"));
        } else {
            transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction Not Found"));
        }
        return mapToResponse(transaction);
    }

    public TransactionResponse getTransactionById(Long id) {
        return getTransactionById(id, null);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request, Long userId) {
        Transaction transaction;
        if (userId != null) {
            transaction = transactionRepository.findByIdAndAccountUserId(id, userId)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction Not Found"));
        } else {
            transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction Not Found"));
        }

        Account account = transaction.getAccount();

        // 1. Reverse the OLD transaction effect
        if (transaction.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        } else if (transaction.getType() == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        }

        // 2. Validate the NEW transaction
        if (request.type() == TransactionType.EXPENSE && account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient account balance");
        }

        // 3. Apply the NEW transaction effect
        if (request.type() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(request.amount()));
        } else if (request.type() == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().subtract(request.amount()));
        }

        // 4. Update fields
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setCategory(request.category());
        transaction.setDescription(request.description());
        if (request.paymentMethod() != null) {
            transaction.setPaymentMethod(request.paymentMethod());
        }
        transaction.setTransactionDate(request.transactionDate());

        Transaction updatedTransaction = transactionRepository.save(transaction);
        accountRepository.save(account);

        return mapToResponse(updatedTransaction);
    }

    public TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request) {
        return updateTransaction(id, request, null);
    }

    @Transactional
    public void deleteTransaction(Long id, Long userId) {
        Transaction transaction;
        if (userId != null) {
            transaction = transactionRepository.findByIdAndAccountUserId(id, userId)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction Not Found"));
        } else {
            transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction Not Found"));
        }

        Account account = transaction.getAccount();

        // Reverse balance impact
        if (transaction.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        } else if (transaction.getType() == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        }

        transactionRepository.delete(transaction);
        accountRepository.save(account);
    }

    public void deleteTransaction(Long id) {
        deleteTransaction(id, null);
    }

    public List<TransactionResponse> getTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionsByType(TransactionType type) {
        return transactionRepository.findByType(type)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionsByCategory(String category) {
        return transactionRepository.findByCategory(category)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByAccountId(accountId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionsByAccountAndType(Long accountId, TransactionType type) {
        return transactionRepository.findByAccountIdAndType(accountId, type)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Page<TransactionResponse> searchTransactions(TransactionFilterRequest filter, Pageable pageable) {
        return getTransactions(filter, pageable, null);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        String accountName = transaction.getAccount() != null ? transaction.getAccount().getName() : "";
        Long userId = transaction.getUser() != null
                ? transaction.getUser().getId()
                : (transaction.getAccount() != null && transaction.getAccount().getUser() != null
                        ? transaction.getAccount().getUser().getId()
                        : null);

        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCategory(),
                transaction.getDescription(),
                transaction.getPaymentMethod(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt(),
                transaction.getAccount() != null ? transaction.getAccount().getId() : null,
                accountName,
                userId
        );
    }

    @Transactional
    public CsvImportResponse importCsv(Long userId, InputStream inputStream) {
        List<String> errors = new ArrayList<>();
        int totalRows = 0;
        int importedCount = 0;
        int skippedCount = 0;

        List<Account> userAccounts = accountRepository.findByUserId(userId);
        if (userAccounts.isEmpty()) {
            return new CsvImportResponse(0, 0, 0, List.of("User has no accounts. Create an account before importing transactions."), "Failed: No accounts found.");
        }

        Map<String, Account> accountMap = new HashMap<>();
        for (Account a : userAccounts) {
            accountMap.put(a.getName().trim().toLowerCase(), a);
        }
        Account defaultAccount = userAccounts.get(0);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new CsvImportResponse(0, 0, 0, List.of("CSV file is empty"), "Empty CSV");
            }

            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            String[] headers = parseCsvLine(headerLine);
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colIndex.put(headers[i].trim().toLowerCase().replace(" ", "").replace("_", ""), i);
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;
                totalRows++;

                try {
                    String[] tokens = parseCsvLine(line);

                    LocalDate txDate = LocalDate.now();
                    Integer dateIdx = getCol(colIndex, "date", "transactiondate");
                    if (dateIdx != null && dateIdx < tokens.length && !tokens[dateIdx].isBlank()) {
                        txDate = parseFlexibleDate(tokens[dateIdx].trim());
                    }

                    Integer amountIdx = getCol(colIndex, "amount");
                    if (amountIdx == null || amountIdx >= tokens.length || tokens[amountIdx].isBlank()) {
                        errors.add("Row " + lineNum + ": Missing amount");
                        skippedCount++;
                        continue;
                    }
                    String amtStr = tokens[amountIdx].trim().replace("$", "").replace(",", "");
                    BigDecimal amount = new BigDecimal(amtStr).abs();
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Row " + lineNum + ": Amount must be positive");
                        skippedCount++;
                        continue;
                    }

                    TransactionType type = TransactionType.EXPENSE;
                    Integer typeIdx = getCol(colIndex, "type", "transactiontype");
                    if (typeIdx != null && typeIdx < tokens.length && !tokens[typeIdx].isBlank()) {
                        String rawType = tokens[typeIdx].trim().toUpperCase();
                        if (rawType.contains("INC") || rawType.contains("CREDIT") || rawType.contains("DEPOSIT")) {
                            type = TransactionType.INCOME;
                        } else {
                            type = TransactionType.EXPENSE;
                        }
                    }

                    String category = "General";
                    Integer catIdx = getCol(colIndex, "category");
                    if (catIdx != null && catIdx < tokens.length && !tokens[catIdx].isBlank()) {
                        category = tokens[catIdx].trim();
                    }

                    Account account = defaultAccount;
                    Integer accIdx = getCol(colIndex, "account", "accountname");
                    if (accIdx != null && accIdx < tokens.length && !tokens[accIdx].isBlank()) {
                        String accName = tokens[accIdx].trim().toLowerCase();
                        if (accountMap.containsKey(accName)) {
                            account = accountMap.get(accName);
                        }
                    }

                    String paymentMethod = "OTHER";
                    Integer pmIdx = getCol(colIndex, "paymentmethod", "payment");
                    if (pmIdx != null && pmIdx < tokens.length && !tokens[pmIdx].isBlank()) {
                        paymentMethod = tokens[pmIdx].trim().toUpperCase();
                    }

                    String description = "Imported transaction";
                    Integer descIdx = getCol(colIndex, "description", "memo", "notes", "payee");
                    if (descIdx != null && descIdx < tokens.length && !tokens[descIdx].isBlank()) {
                        description = tokens[descIdx].trim();
                    }

                    TransactionRequest req = new TransactionRequest(
                            amount,
                            type,
                            category,
                            description,
                            paymentMethod,
                            txDate,
                            account.getId()
                    );

                    createTransaction(req, userId);
                    importedCount++;
                } catch (Exception ex) {
                    errors.add("Row " + lineNum + ": " + ex.getMessage());
                    skippedCount++;
                }
            }
        } catch (Exception e) {
            errors.add("General CSV parse failure: " + e.getMessage());
            return new CsvImportResponse(totalRows, importedCount, skippedCount, errors, "Import failed: " + e.getMessage());
        }

        String msg = String.format("Successfully imported %d of %d transactions (%d skipped)", importedCount, totalRows, skippedCount);
        return new CsvImportResponse(totalRows, importedCount, skippedCount, errors, msg);
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    private Integer getCol(Map<String, Integer> colIndex, String... possibleNames) {
        for (String name : possibleNames) {
            if (colIndex.containsKey(name)) {
                return colIndex.get(name);
            }
        }
        return null;
    }

    private LocalDate parseFlexibleDate(String dateStr) {
        String clean = dateStr.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd")
        );
        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDate.parse(clean, fmt);
            } catch (Exception ignored) {
            }
        }
        return LocalDate.now();
    }

    @Transactional(readOnly = true)
    public void exportFilteredCsv(Long userId, TransactionFilterRequest filter, Writer writer) throws IOException {
        Specification<Transaction> spec = (root, query, cb) -> cb.conjunction();
        if (userId != null) {
            spec = spec.and(TransactionSpecification.hasUserId(userId));
        }
        if (filter != null) {
            if (filter.search() != null && !filter.search().isBlank()) {
                spec = spec.and(TransactionSpecification.descriptionContains(filter.search()));
            }
            if (filter.type() != null) {
                spec = spec.and(TransactionSpecification.hasType(filter.type()));
            }
            if (filter.category() != null && !filter.category().isBlank()) {
                spec = spec.and(TransactionSpecification.hasCategory(filter.category()));
            }
            if (filter.accountId() != null) {
                spec = spec.and(TransactionSpecification.hasAccountId(filter.accountId()));
            }
            if (filter.startDate() != null) {
                spec = spec.and(TransactionSpecification.dateGreaterThanOrEqualTo(filter.startDate()));
            }
            if (filter.endDate() != null) {
                spec = spec.and(TransactionSpecification.dateLessThanOrEqualTo(filter.endDate()));
            }
        }

        List<Transaction> transactions = transactionRepository.findAll(spec);
        transactions.sort((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()));

        writer.write("Transaction ID,Date,Type,Category,Amount,Currency,Account,Payment Method,Description\n");
        for (Transaction tx : transactions) {
            String currency = tx.getAccount() != null && tx.getAccount().getCurrency() != null ? tx.getAccount().getCurrency() : "USD";
            String accountName = tx.getAccount() != null ? tx.getAccount().getName() : "N/A";
            String description = tx.getDescription() != null ? tx.getDescription().replace("\"", "\"\"") : "";
            String category = tx.getCategory() != null ? tx.getCategory().replace("\"", "\"\"") : "General";

            writer.write(String.format("%d,%s,%s,\"%s\",%.2f,%s,\"%s\",%s,\"%s\"\n",
                    tx.getId(),
                    tx.getTransactionDate(),
                    tx.getType(),
                    category,
                    tx.getAmount(),
                    currency,
                    accountName.replace("\"", "\"\""),
                    tx.getPaymentMethod() != null ? tx.getPaymentMethod() : "OTHER",
                    description
            ));
        }
        writer.flush();
    }
}