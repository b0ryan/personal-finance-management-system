package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Класс для представления транзакции (доход или расход)
 */
public class Transaction {
    public enum Type {
        INCOME,  // Доход
        EXPENSE  // Расход
    }

    private String category;
    private double amount;
    private Type type;
    private LocalDateTime dateTime;
    private String description;

    public Transaction(String category, double amount, Type type) {
        this.category = category;
        this.amount = amount;
        this.type = type;
        this.dateTime = LocalDateTime.now();
        this.description = "";
    }

    public Transaction(String category, double amount, Type type, String description) {
        this.category = category;
        this.amount = amount;
        this.type = type;
        this.dateTime = LocalDateTime.now();
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("%s: %s - %.2f (%s)", 
            type == Type.INCOME ? "Доход" : "Расход",
            category, 
            amount,
            dateTime.format(formatter));
    }
}
