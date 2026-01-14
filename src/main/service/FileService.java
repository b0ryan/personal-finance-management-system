import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Сервис для сохранения и загрузки данных из файла
 */
public class FileService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Получить имя файла для пользователя
     */
    private String getUserFileName(String login) {
        return login + ".txt";
    }

    /**
     * Сохранить данные пользователя в файл
     */
    public boolean saveUserData(String login, String password, Wallet wallet, Map<String, Budget> budgets) {
        String filename = getUserFileName(login);
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(filename), StandardCharsets.UTF_8))) {
            
            // Сохраняем информацию о пользователе
            writer.println("USER:" + login + ":" + password);
            
            // Сохраняем транзакции
            for (Transaction transaction : wallet.getTransactions()) {
                writer.println(String.format("TRANSACTION:%s:%s:%s:%s",
                    transaction.getType().name(),
                    transaction.getCategory(),
                    transaction.getAmount(),
                    transaction.getDateTime().format(DATE_FORMATTER)));
            }

            // Сохраняем бюджеты
            for (Budget budget : budgets.values()) {
                writer.println(String.format("BUDGET:%s:%s",
                    budget.getCategory(),
                    budget.getLimit()));
            }

            return true;
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении данных: " + e.getMessage());
            return false;
        }
    }

    /**
     * Загрузить данные пользователя из файла
     */
    public boolean loadUserData(String login, AuthService authService, BudgetService budgetService) {
        String filename = getUserFileName(login);
        File file = new File(filename);
        if (!file.exists()) {
            return false; // Файл не существует, это нормально при первом запуске
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(filename), StandardCharsets.UTF_8))) {
            
            List<Transaction> transactions = new ArrayList<>();
            Map<String, Budget> budgets = new HashMap<>();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Используем ограниченное разделение, чтобы избежать проблем с двоеточиями в категориях
                int firstColon = line.indexOf(':');
                if (firstColon == -1) {
                    continue;
                }
                String type = line.substring(0, firstColon);
                String rest = line.substring(firstColon + 1);

                switch (type) {
                    case "USER":
                        // Информация о пользователе уже есть в AuthService
                        break;

                    case "TRANSACTION":
                        // Формат: TYPE:CATEGORY:AMOUNT:DATETIME
                        // Разделяем на части, но ограничиваем количество разделений
                        String[] transParts = rest.split(":", 4);
                        if (transParts.length >= 4) {
                            try {
                                Transaction.Type transType = Transaction.Type.valueOf(transParts[0]);
                                String category = transParts[1];
                                double amount = Double.parseDouble(transParts[2]);
                                LocalDateTime dateTime = LocalDateTime.parse(transParts[3], DATE_FORMATTER);
                                
                                Transaction transaction = new Transaction(category, amount, transType);
                                transaction.setDateTime(dateTime);
                                transactions.add(transaction);
                            } catch (Exception e) {
                                System.err.println("Ошибка при парсинге транзакции: " + e.getMessage());
                            }
                        }
                        break;

                    case "BUDGET":
                        // Формат: CATEGORY:LIMIT
                        String[] budgetParts = rest.split(":", 2);
                        if (budgetParts.length >= 2) {
                            try {
                                String category = budgetParts[0];
                                double limit = Double.parseDouble(budgetParts[1]);
                                budgets.put(category, new Budget(category, limit));
                            } catch (Exception e) {
                                System.err.println("Ошибка при парсинге бюджета: " + e.getMessage());
                            }
                        }
                        break;
                }
            }

            // Восстанавливаем данные пользователя
            User user = authService.getUserByLogin(login);
            if (user != null) {
                // Восстанавливаем транзакции в кошелек
                Wallet wallet = new Wallet();
                wallet.setTransactions(transactions);
                wallet.recalculateBalance();
                user.setWallet(wallet);
                
                // Восстанавливаем бюджеты
                budgetService.setUserBudgets(login, budgets);
            }

            return true;
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке данных: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге данных: " + e.getMessage());
            return false;
        }
    }

    /**
     * Найти файл пользователя по логину и паролю
     * Сканирует все .txt файлы в текущей директории
     * @return имя файла, если найден, иначе null
     */
    public String findUserFile(String login, String password) {
        File currentDir = new File(".");
        File[] files = currentDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        
        if (files == null) {
            return null;
        }
        
        for (File file : files) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    // Используем ограниченное разделение для USER
                    int firstColon = line.indexOf(':');
                    if (firstColon != -1 && line.substring(0, firstColon).equals("USER")) {
                        String rest = line.substring(firstColon + 1);
                        String[] parts = rest.split(":", 2);
                        if (parts.length >= 2) {
                            String fileLogin = parts[0];
                            String filePassword = parts[1];
                            if (fileLogin.equals(login) && filePassword.equals(password)) {
                                return file.getName();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // Пропускаем файлы, которые не удалось прочитать
                continue;
            }
        }
        
        return null;
    }

    /**
     * Загрузить данные пользователя из указанного файла
     */
    public boolean loadUserDataFromFile(String filename, String login, AuthService authService, BudgetService budgetService) {
        File file = new File(filename);
        if (!file.exists()) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(filename), StandardCharsets.UTF_8))) {
            
            List<Transaction> transactions = new ArrayList<>();
            Map<String, Budget> budgets = new HashMap<>();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Используем ограниченное разделение, чтобы избежать проблем с двоеточиями в категориях
                int firstColon = line.indexOf(':');
                if (firstColon == -1) {
                    continue;
                }
                String type = line.substring(0, firstColon);
                String rest = line.substring(firstColon + 1);

                switch (type) {
                    case "USER":
                        // Информация о пользователе уже есть в AuthService
                        break;

                    case "TRANSACTION":
                        // Формат: TYPE:CATEGORY:AMOUNT:DATETIME
                        // Разделяем на части, но ограничиваем количество разделений
                        String[] transParts = rest.split(":", 4);
                        if (transParts.length >= 4) {
                            try {
                                Transaction.Type transType = Transaction.Type.valueOf(transParts[0]);
                                String category = transParts[1];
                                double amount = Double.parseDouble(transParts[2]);
                                LocalDateTime dateTime = LocalDateTime.parse(transParts[3], DATE_FORMATTER);
                                
                                Transaction transaction = new Transaction(category, amount, transType);
                                transaction.setDateTime(dateTime);
                                transactions.add(transaction);
                            } catch (Exception e) {
                                System.err.println("Ошибка при парсинге транзакции: " + e.getMessage());
                            }
                        }
                        break;

                    case "BUDGET":
                        // Формат: CATEGORY:LIMIT
                        String[] budgetParts = rest.split(":", 2);
                        if (budgetParts.length >= 2) {
                            try {
                                String category = budgetParts[0];
                                double limit = Double.parseDouble(budgetParts[1]);
                                budgets.put(category, new Budget(category, limit));
                            } catch (Exception e) {
                                System.err.println("Ошибка при парсинге бюджета: " + e.getMessage());
                            }
                        }
                        break;
                }
            }

            // Восстанавливаем данные пользователя
            User user = authService.getUserByLogin(login);
            if (user != null) {
                // Восстанавливаем транзакции в кошелек
                Wallet wallet = new Wallet();
                wallet.setTransactions(transactions);
                wallet.recalculateBalance();
                user.setWallet(wallet);
                
                // Восстанавливаем бюджеты
                budgetService.setUserBudgets(login, budgets);
            }

            return true;
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке данных: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге данных: " + e.getMessage());
            return false;
        }
    }
}
