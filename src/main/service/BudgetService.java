package service;

import java.util.HashMap;
import java.util.Map;
import model.User;
import model.Transaction;
import model.Budget;

/**
 * Сервис для управления бюджетами по категориям
 */
public class BudgetService {
    private AuthService authService;
    private Map<String, Map<String, Budget>> userBudgets; // login -> (category -> budget)

    public BudgetService(AuthService authService) {
        this.authService = authService;
        this.userBudgets = new HashMap<>();
    }

    /**
     * Установить бюджет на категорию расходов
     */
    public boolean setBudget(String category, double limit) {
        if (!authService.isAuthenticated()) {
            return false;
        }
        if (category == null || category.trim().isEmpty()) {
            return false;
        }
        if (limit < 0) {
            return false;
        }
        String login = authService.getCurrentUser().getLogin();
        userBudgets.putIfAbsent(login, new HashMap<>());
        userBudgets.get(login).put(category, new Budget(category, limit));
        return true;
    }

    /**
     * Получить бюджет для категории
     */
    public Budget getBudget(String category) {
        if (!authService.isAuthenticated()) {
            return null;
        }
        String login = authService.getCurrentUser().getLogin();
        Map<String, Budget> budgets = userBudgets.get(login);
        if (budgets == null) {
            return null;
        }
        return budgets.get(category);
    }

    /**
     * Получить все бюджеты текущего пользователя
     */
    public Map<String, Budget> getAllBudgets() {
        if (!authService.isAuthenticated()) {
            return new HashMap<>();
        }
        String login = authService.getCurrentUser().getLogin();
        Map<String, Budget> budgets = userBudgets.get(login);
        return budgets != null ? budgets : new HashMap<>();
    }

    /**
     * Вычислить потраченную сумму по категории расходов
     */
    public double getSpentAmount(String category) {
        if (!authService.isAuthenticated()) {
            return 0.0;
        }
        User user = authService.getCurrentUser();
        double spent = 0.0;
        for (Transaction transaction : user.getWallet().getTransactions()) {
            if (transaction.getType() == Transaction.Type.EXPENSE 
                && transaction.getCategory().equals(category)) {
                spent += transaction.getAmount();
            }
        }
        return spent;
    }

    /**
     * Вычислить оставшийся бюджет для категории
     */
    public double getRemainingBudget(String category) {
        Budget budget = getBudget(category);
        if (budget == null) {
            return 0.0;
        }
        double spent = getSpentAmount(category);
        return budget.getLimit() - spent;
    }

    /**
     * Проверить, превышен ли бюджет для категории
     */
    public boolean isBudgetExceeded(String category) {
        return getRemainingBudget(category) < 0;
    }

    /**
     * Установить бюджеты пользователя (для загрузки из файла)
     */
    public void setUserBudgets(String login, Map<String, Budget> budgets) {
        userBudgets.put(login, budgets);
    }

    /**
     * Получить все бюджеты всех пользователей (для сохранения в файл)
     */
    public Map<String, Map<String, Budget>> getAllUserBudgets() {
        return userBudgets;
    }

    /**
     * Установить все бюджеты (для загрузки из файла)
     */
    public void setAllUserBudgets(Map<String, Map<String, Budget>> budgets) {
        this.userBudgets = budgets;
    }
}
