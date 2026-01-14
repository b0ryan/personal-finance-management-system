package service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import model.User;
import model.Transaction;
import model.Budget;

/**
 * Сервис для подсчета и вывода статистики
 */
public class StatisticsService {
    private AuthService authService;
    private BudgetService budgetService;
    private DecimalFormat df;

    public StatisticsService(AuthService authService, BudgetService budgetService) {
        this.authService = authService;
        this.budgetService = budgetService;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        this.df = new DecimalFormat("#,##0.00", symbols);
    }

    /**
     * Подсчитать общий доход
     */
    public double getTotalIncome() {
        if (!authService.isAuthenticated()) {
            return 0.0;
        }
        User user = authService.getCurrentUser();
        double total = 0.0;
        for (Transaction transaction : user.getWallet().getTransactions()) {
            if (transaction.getType() == Transaction.Type.INCOME) {
                total += transaction.getAmount();
            }
        }
        return total;
    }

    /**
     * Подсчитать общий расход
     */
    public double getTotalExpense() {
        if (!authService.isAuthenticated()) {
            return 0.0;
        }
        User user = authService.getCurrentUser();
        double total = 0.0;
        for (Transaction transaction : user.getWallet().getTransactions()) {
            if (transaction.getType() == Transaction.Type.EXPENSE) {
                total += transaction.getAmount();
            }
        }
        return total;
    }

    /**
     * Подсчитать доходы по категориям
     */
    public Map<String, Double> getIncomeByCategories() {
        if (!authService.isAuthenticated()) {
            return new HashMap<>();
        }
        User user = authService.getCurrentUser();
        Map<String, Double> incomeByCategory = new HashMap<>();
        for (Transaction transaction : user.getWallet().getTransactions()) {
            if (transaction.getType() == Transaction.Type.INCOME) {
                String category = transaction.getCategory();
                incomeByCategory.put(category, 
                    incomeByCategory.getOrDefault(category, 0.0) + transaction.getAmount());
            }
        }
        return incomeByCategory;
    }

    /**
     * Подсчитать расходы по категориям
     */
    public Map<String, Double> getExpenseByCategories() {
        if (!authService.isAuthenticated()) {
            return new HashMap<>();
        }
        User user = authService.getCurrentUser();
        Map<String, Double> expenseByCategory = new HashMap<>();
        for (Transaction transaction : user.getWallet().getTransactions()) {
            if (transaction.getType() == Transaction.Type.EXPENSE) {
                String category = transaction.getCategory();
                expenseByCategory.put(category, 
                    expenseByCategory.getOrDefault(category, 0.0) + transaction.getAmount());
            }
        }
        return expenseByCategory;
    }

    /**
     * Подсчитать сумму по нескольким категориям
     */
    public double getAmountByCategories(List<String> categories, Transaction.Type type) {
        if (!authService.isAuthenticated()) {
            return 0.0;
        }
        User user = authService.getCurrentUser();
        double total = 0.0;
        Set<String> categorySet = new HashSet<>(categories);
        for (Transaction transaction : user.getWallet().getTransactions()) {
            if (transaction.getType() == type && categorySet.contains(transaction.getCategory())) {
                total += transaction.getAmount();
            }
        }
        return total;
    }

    /**
     * Вывести статистику в терминал
     */
    public void printStatistics() {
        if (!authService.isAuthenticated()) {
            System.out.println("Пользователь не авторизован");
            return;
        }

        double totalIncome = getTotalIncome();
        double totalExpense = getTotalExpense();

        System.out.println("Общий доход: " + df.format(totalIncome));
        System.out.println();
        System.out.println("Доходы по категориям:");
        Map<String, Double> incomeByCategory = getIncomeByCategories();
        if (incomeByCategory.isEmpty()) {
            System.out.println("  (нет данных)");
        } else {
            for (Map.Entry<String, Double> entry : incomeByCategory.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + df.format(entry.getValue()));
            }
        }

        System.out.println();
        System.out.println("Общие расходы: " + df.format(totalExpense));
        System.out.println();
        System.out.println("Бюджет по категориям:");
        Map<String, Budget> budgets = budgetService.getAllBudgets();
        if (budgets.isEmpty()) {
            System.out.println("  (нет данных)");
        } else {
            List<Map.Entry<String, Budget>> sortedBudgets = new ArrayList<>(budgets.entrySet());
            sortedBudgets.sort(Comparator.comparing(e -> e.getValue().getCategory()));
            
            for (Map.Entry<String, Budget> entry : sortedBudgets) {
                Budget budget = entry.getValue();
                double remaining = budgetService.getRemainingBudget(budget.getCategory());
                System.out.println(String.format("  %s: %s, Оставшийся бюджет: %s",
                    budget.getCategory(),
                    df.format(budget.getLimit()),
                    df.format(remaining)));
            }
        }

        if (totalExpense > totalIncome) {
            System.out.println();
            System.out.println("⚠ ВНИМАНИЕ: Расходы превысили доходы!");
        }

        boolean hasBudgetWarnings = false;
        for (Budget budget : budgets.values()) {
            if (budgetService.isBudgetExceeded(budget.getCategory())) {
                if (!hasBudgetWarnings) {
                    System.out.println();
                    hasBudgetWarnings = true;
                }
                System.out.println(String.format("⚠ ВНИМАНИЕ: Превышен лимит бюджета для категории '%s'!",
                    budget.getCategory()));
            }
        }
    }

    /**
     * Сохранить статистику в файл
     */
    public boolean saveStatisticsToFile(String filename) {
        if (!authService.isAuthenticated()) {
            return false;
        }
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(filename), StandardCharsets.UTF_8))) {
            double totalIncome = getTotalIncome();
            double totalExpense = getTotalExpense();

            writer.println("Общий доход: " + df.format(totalIncome));
            writer.println("Доходы по категориям:");
            Map<String, Double> incomeByCategory = getIncomeByCategories();
            for (Map.Entry<String, Double> entry : incomeByCategory.entrySet()) {
                writer.println(entry.getKey() + ": " + df.format(entry.getValue()));
            }

            writer.println("Общие расходы: " + df.format(totalExpense));
            writer.println("Бюджет по категориям:");
            Map<String, Budget> budgets = budgetService.getAllBudgets();
            List<Map.Entry<String, Budget>> sortedBudgets = new ArrayList<>(budgets.entrySet());
            sortedBudgets.sort(Comparator.comparing(e -> e.getValue().getCategory()));
            
            for (Map.Entry<String, Budget> entry : sortedBudgets) {
                Budget budget = entry.getValue();
                double remaining = budgetService.getRemainingBudget(budget.getCategory());
                writer.println(String.format("%s: %s, Оставшийся бюджет: %s",
                    budget.getCategory(),
                    df.format(budget.getLimit()),
                    df.format(remaining)));
            }

            if (totalExpense > totalIncome) {
                writer.println("\n⚠ ВНИМАНИЕ: Расходы превысили доходы!");
            }

            for (Budget budget : budgets.values()) {
                if (budgetService.isBudgetExceeded(budget.getCategory())) {
                    writer.println(String.format("⚠ ВНИМАНИЕ: Превышен лимит бюджета для категории '%s'!",
                        budget.getCategory()));
                }
            }

            return true;
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении статистики в файл: " + e.getMessage());
            return false;
        }
    }
}
