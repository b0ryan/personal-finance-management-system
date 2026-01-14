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
     * Перевод между пользователями
     */
    public boolean transfer(String recipientLogin, double amount) {
        if (!authService.isAuthenticated()) {
            return false;
        }
        if (amount <= 0) {
            return false;
        }
        User sender = authService.getCurrentUser();
        User recipient = authService.getUserByLogin(recipientLogin);
        
        if (recipient == null) {
            return false; // Получатель не найден
        }
        if (sender.equals(recipient)) {
            return false; // Нельзя переводить самому себе
        }
        
        // Добавляем расход отправителю
        Transaction senderTransaction = new Transaction(
            "Перевод пользователю " + recipientLogin, 
            amount, 
            Transaction.Type.EXPENSE,
            "Перевод пользователю " + recipientLogin
        );
        sender.getWallet().addTransaction(senderTransaction);
        
        // Добавляем доход получателю
        Transaction recipientTransaction = new Transaction(
            "Перевод от пользователя " + sender.getLogin(), 
            amount, 
            Transaction.Type.INCOME,
            "Перевод от пользователя " + sender.getLogin()
        );
        recipient.getWallet().addTransaction(recipientTransaction);
        
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
