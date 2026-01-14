package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для представления кошелька пользователя
 */
public class Wallet {
    private List<Transaction> transactions;
    private double balance;

    public Wallet() {
        this.transactions = new ArrayList<>();
        this.balance = 0.0;
    }

    public Wallet(List<Transaction> transactions, double balance) {
        this.transactions = transactions;
        this.balance = balance;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        recalculateBalance();
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    /**
     * Добавить транзакцию в кошелек
     */
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        if (transaction.getType() == Transaction.Type.INCOME) {
            balance += transaction.getAmount();
        } else {
            balance -= transaction.getAmount();
        }
    }

    /**
     * Пересчитать баланс на основе всех транзакций
     */
    public void recalculateBalance() {
        balance = 0.0;
        for (Transaction transaction : transactions) {
            if (transaction.getType() == Transaction.Type.INCOME) {
                balance += transaction.getAmount();
            } else {
                balance -= transaction.getAmount();
            }
        }
    }
}
