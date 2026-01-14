package service;

import model.User;
import model.Wallet;
import model.Transaction;

/**
 * Сервис для управления кошельком и операциями
 */
public class WalletService {
    private AuthService authService;

    public WalletService(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Добавить доход
     */
    public boolean addIncome(String category, double amount) {
        if (!authService.isAuthenticated()) {
            return false;
        }
        if (category == null || category.trim().isEmpty()) {
            return false;
        }
        if (amount <= 0) {
            return false;
        }
        User user = authService.getCurrentUser();
        Transaction transaction = new Transaction(category, amount, Transaction.Type.INCOME);
        user.getWallet().addTransaction(transaction);
        return true;
    }

    /**
     * Добавить расход
     */
    public boolean addExpense(String category, double amount) {
        if (!authService.isAuthenticated()) {
            return false;
        }
        if (category == null || category.trim().isEmpty()) {
            return false;
        }
        if (amount <= 0) {
            return false;
        }
        User user = authService.getCurrentUser();
        Transaction transaction = new Transaction(category, amount, Transaction.Type.EXPENSE);
        user.getWallet().addTransaction(transaction);
        return true;
    }

    /**
     * Получить кошелек текущего пользователя
     */
    public Wallet getCurrentWallet() {
        if (!authService.isAuthenticated()) {
            return null;
        }
        return authService.getCurrentUser().getWallet();
    }
}
