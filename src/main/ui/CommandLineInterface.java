package ui;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import service.AuthService;
import service.WalletService;
import service.BudgetService;
import service.StatisticsService;
import service.FileService;
import model.User;
import model.Wallet;
import model.Transaction;
import model.Budget;

/**
 * Класс для взаимодействия с пользователем через командную строку
 */
public class CommandLineInterface {
    private Scanner scanner;
    private AuthService authService;
    private WalletService walletService;
    private BudgetService budgetService;
    private StatisticsService statisticsService;
    private FileService fileService;
    private boolean running;

    public CommandLineInterface() {
        // Настраиваем Scanner для работы с UTF-8
        this.scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        this.authService = new AuthService();
        this.budgetService = new BudgetService(authService);
        this.walletService = new WalletService(authService);
        this.statisticsService = new StatisticsService(authService, budgetService);
        this.fileService = new FileService();
        this.running = true;
    }

    /**
     * Запустить интерфейс командной строки
     */
    public void run() {
        System.out.println("Добро пожаловать в систему управления личными финансами!");

        while (running) {
            if (!authService.isAuthenticated()) {
                showAuthMenu();
            } else {
                showMainMenu();
            }
        }

        // Сохраняем данные текущего пользователя при выходе из приложения
        if (authService.isAuthenticated()) {
            saveCurrentUserData(true);
        }
        System.out.println("До свидания!");
    }

    /**
     * Показать меню авторизации
     */
    private void showAuthMenu() {
        System.out.println("\n");
        System.out.println("========================================");
        System.out.println("===        Меню авторизации        ===");
        System.out.println("========================================");
        System.out.println();
        System.out.println("1. Войти");
        System.out.println("2. Зарегистрироваться");
        System.out.println("0. Выход");
        System.out.println();
        System.out.print("Выберите действие: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                handleLogin();
                break;
            case "2":
                handleRegister();
                break;
            case "0":
                running = false;
                break;
            default:
                System.out.println("\nНеверный выбор. Попробуйте снова.\n");
        }
    }

    /**
     * Показать главное меню
     */
    private void showMainMenu() {
        System.out.println("\n");
        System.out.println("========================================");
        System.out.println("===         Главное меню            ===");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Текущий пользователь: " + authService.getCurrentUser().getLogin());
        System.out.println("Баланс: " + String.format("%.2f", authService.getCurrentUser().getWallet().getBalance()));
        System.out.println();
        System.out.println("1. Добавить доход");
        System.out.println("2. Добавить расход");
        System.out.println("3. Установить бюджет на категорию");
        System.out.println("4. Показать статистику");
        System.out.println("5. Сохранить статистику в файл");
        System.out.println("6. Подсчитать сумму по категориям");
        System.out.println("7. Выйти из аккаунта");
        System.out.println("0. Выход из приложения");
        System.out.println();
        System.out.print("Выберите действие: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                handleAddIncome();
                break;
            case "2":
                handleAddExpense();
                break;
            case "3":
                handleSetBudget();
                break;
            case "4":
                printStatisticsWithPause();
                break;
            case "5":
                handleSaveStatistics();
                break;
            case "6":
                handleCalculateByCategories();
                break;
            case "7":
                // Сохраняем данные перед выходом из аккаунта
                saveCurrentUserData(true);
                authService.logout();
                System.out.println("\nВы вышли из аккаунта.\n");
                break;
            case "0":
                running = false;
                break;
            default:
                System.out.println("\nНеверный выбор. Попробуйте снова.\n");
        }
    }

    /**
     * Обработка входа
     */
    private void handleLogin() {
        System.out.print("Введите логин: ");
        String login = scanner.nextLine().trim();
        System.out.print("Введите пароль: ");
        String password = scanner.nextLine().trim();

        // Сначала проверяем, есть ли файл с таким логином и паролем
        String userFile = fileService.findUserFile(login, password);
        
        if (userFile != null) {
            // Файл найден,авторизуем пользователя и загружаем данные
            if (authService.getUserByLogin(login) == null) {
                authService.register(login, password);
            }
            
            if (authService.login(login, password)) {
                System.out.println("\nУспешный вход!");
                // Загружаем данные из найденного файла
                if (fileService.loadUserDataFromFile(userFile, login, authService, budgetService)) {
                    System.out.println("Данные пользователя загружены из файла " + userFile);
                } else {
                    System.out.println("Предупреждение: не удалось загрузить данные пользователя.");
                }
                System.out.println();
            } else {
                System.out.println("\nОшибка авторизации.\n");
            }
        } else {
            // Файл не найден, проверяем авторизацию через AuthService
            if (authService.login(login, password)) {
                System.out.println("\nУспешный вход!");
                System.out.println("Файл данных не найден. Создан новый кошелек.\n");
            } else {
                System.out.println("\nНеверный логин или пароль.\n");
            }
        }
    }

    /**
     * Обработка регистрации
     */
    private void handleRegister() {
        System.out.print("Введите логин: ");
        String login = scanner.nextLine().trim();
        System.out.print("Введите пароль: ");
        String password = scanner.nextLine().trim();

        if (authService.register(login, password)) {
            System.out.println("\nРегистрация успешна!");
            
            // Автоматически входим после регистрации
            if (authService.login(login, password)) {
                System.out.println("Вы автоматически вошли в систему.");
                
                // Проверяем, есть ли файл данных для этого пользователя
                String userFile = fileService.findUserFile(login, password);
                if (userFile != null) {
                    // Загружаем данные из найденного файла
                    if (fileService.loadUserDataFromFile(userFile, login, authService, budgetService)) {
                        System.out.println("Данные пользователя загружены из файла " + userFile);
                    } else {
                        System.out.println("Предупреждение: не удалось загрузить данные пользователя.");
                    }
                } else {
                    System.out.println("Создан новый кошелек.");
                }
                System.out.println();
            } else {
                System.out.println("Ошибка при автоматическом входе.\n");
            }
        } else {
            System.out.println("\nОшибка регистрации. Возможно, пользователь с таким логином уже существует.\n");
        }
    }

    /**
     * Обработка добавления дохода
     */
    private void handleAddIncome() {
        System.out.print("Введите категорию дохода: ");
        String category = scanner.nextLine().trim();
        System.out.print("Введите сумму: ");
        String amountStr = scanner.nextLine().trim();

        try {
            double amount = Double.parseDouble(amountStr);
            if (!isValidDecimalPlaces(amountStr)) {
                System.out.println("\nОшибка: сумма должна иметь не более 2 знаков после запятой.\n");
                return;
            }
            if (walletService.addIncome(category, amount)) {
                System.out.println("\nДоход успешно добавлен!\n");
                saveCurrentUserData();
                checkAlerts();
            } else {
                System.out.println("\nОшибка при добавлении дохода. Проверьте введенные данные.\n");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nНеверный формат суммы.\n");
        }
    }

    /**
     * Обработка добавления расхода
     */
    private void handleAddExpense() {
        System.out.print("Введите категорию расхода: ");
        String category = scanner.nextLine().trim();
        System.out.print("Введите сумму: ");
        String amountStr = scanner.nextLine().trim();

        try {
            double amount = Double.parseDouble(amountStr);
            if (!isValidDecimalPlaces(amountStr)) {
                System.out.println("\nОшибка: сумма должна иметь не более 2 знаков после запятой.\n");
                return;
            }
            
            // Проверяем, не превысит ли сумма расходов сумму доходов
            double currentIncome = statisticsService.getTotalIncome();
            double currentExpense = statisticsService.getTotalExpense();
            double newTotalExpense = currentExpense + amount;
            double newCurrentMoney = currentIncome - newTotalExpense;
            
            if (newTotalExpense > currentIncome) {
                System.out.println();
                System.out.println("========================================");
                System.out.println("⚠ ОШИБКА: Превышение лимита расходов!");
                System.out.println("========================================");
                System.out.println(String.format("Текущий доход: %.2f", currentIncome));
                System.out.println(String.format("Текущий расход: %.2f", currentExpense));
                System.out.println(String.format("Новый расход: %.2f", newTotalExpense));
                System.out.println(String.format("Итоговый бюджет: %.2f", newCurrentMoney));
                System.out.println();
                System.out.println("Сумма расходов не может превышать сумму доходов!");
                System.out.println("========================================");
                System.out.println();
                return;
            }
            
            if (walletService.addExpense(category, amount)) {
                System.out.println("\nРасход успешно добавлен!\n");
                saveCurrentUserData();
                checkAlerts();
            } else {
                System.out.println("\nОшибка при добавлении расхода. Проверьте введенные данные.\n");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nНеверный формат суммы.\n");
        }
    }

    /**
     * Обработка установки бюджета
     */
    private void handleSetBudget() {
        System.out.print("Введите категорию расходов: ");
        String category = scanner.nextLine().trim();
        System.out.print("Введите лимит бюджета: ");
        String limitStr = scanner.nextLine().trim();

        try {
            double limit = Double.parseDouble(limitStr);
            if (!isValidDecimalPlaces(limitStr)) {
                System.out.println("\nОшибка: сумма должна иметь не более 2 знаков после запятой.\n");
                return;
            }
            if (budgetService.setBudget(category, limit)) {
                System.out.println("\nБюджет успешно установлен!\n");
                saveCurrentUserData();
            } else {
                System.out.println("\nОшибка при установке бюджета. Проверьте введенные данные.\n");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nНеверный формат суммы.\n");
        }
    }

    /**
     * Обработка сохранения статистики в файл
     */
    private void handleSaveStatistics() {
        System.out.print("Введите имя файла (или нажмите Enter для 'statistics.txt'): ");
        String filename = scanner.nextLine().trim();
        if (filename.isEmpty()) {
            filename = "statistics.txt";
        } else {
            // Добавляем расширение .txt, если оно отсутствует
            if (!filename.toLowerCase().endsWith(".txt")) {
                filename = filename + ".txt";
            }
        }

        if (statisticsService.saveStatisticsToFile(filename)) {
            System.out.println("\nСтатистика успешно сохранена в файл " + filename + "\n");
        } else {
            System.out.println("\nОшибка при сохранении статистики.\n");
        }
    }

    /**
     * Обработка подсчета суммы по категориям
     */
    private void handleCalculateByCategories() {
        // Сначала выбираем тип
        System.out.println("Выберите тип:");
        System.out.println("1. Доходы");
        System.out.println("2. Расходы");
        System.out.print("Ваш выбор: ");
        String typeChoice = scanner.nextLine().trim();

        Transaction.Type type;
        if ("1".equals(typeChoice)) {
            type = Transaction.Type.INCOME;
        } else if ("2".equals(typeChoice)) {
            type = Transaction.Type.EXPENSE;
        } else {
            System.out.println("\nНеверный выбор типа.\n");
            return;
        }

        // Затем вводим категории
        System.out.print("Введите категории через запятую: ");
        String categoriesStr = scanner.nextLine().trim();

        List<String> categories = new ArrayList<>(Arrays.asList(categoriesStr.split(",")));
        categories.replaceAll(String::trim);
        categories.removeIf(String::isEmpty);

        if (categories.isEmpty()) {
            System.out.println("\nНе указаны категории.\n");
            return;
        }

        // Проверяем существование категорий
        List<String> existingCategories = getExistingCategories(type);
        List<String> nonExistentCategories = new ArrayList<>();
        
        for (String category : categories) {
            if (!existingCategories.contains(category)) {
                nonExistentCategories.add(category);
            }
        }

        if (!nonExistentCategories.isEmpty()) {
            System.out.println();
            System.out.println("⚠ ВНИМАНИЕ: Следующие категории не найдены:");
            for (String category : nonExistentCategories) {
                System.out.println("  - " + category);
            }
            System.out.println();
            
            // Удаляем несуществующие категории из списка
            categories.removeAll(nonExistentCategories);
            
            if (categories.isEmpty()) {
                System.out.println("Нет существующих категорий для подсчета.\n");
                return;
            }
        }

        double total = statisticsService.getAmountByCategories(categories, type);
        System.out.println();
        System.out.println(String.format("Сумма по выбранным категориям (%s): %.2f",
            type == Transaction.Type.INCOME ? "доходы" : "расходы",
            total));
        System.out.println();
    }

    /**
     * Получить список существующих категорий для указанного типа
     */
    private List<String> getExistingCategories(Transaction.Type type) {
        List<String> categories = new ArrayList<>();
        if (!authService.isAuthenticated()) {
            return categories;
        }
        
        User user = authService.getCurrentUser();
        Set<String> categorySet = new HashSet<>();
        
        for (Transaction transaction : user.getWallet().getTransactions()) {
            if (transaction.getType() == type) {
                categorySet.add(transaction.getCategory());
            }
        }
        
        categories.addAll(categorySet);
        return categories;
    }

    /**
     * Проверить, что число имеет не более 2 знаков после запятой
     */
    private boolean isValidDecimalPlaces(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return false;
        }
        
        // Проверяем наличие точки
        int dotIndex = amountStr.indexOf('.');
        if (dotIndex == -1) {
            // Нет точки - целое число, это допустимо
            return true;
        }
        
        // Проверяем количество знаков после точки
        String decimalPart = amountStr.substring(dotIndex + 1);
        return decimalPart.length() <= 2;
    }

    /**
     * Сохранить данные текущего пользователя
     */
    private void saveCurrentUserData() {
        saveCurrentUserData(false);
    }

    /**
     * Сохранить данные текущего пользователя
     * @param showMessage показывать ли сообщение о сохранении
     */
    private void saveCurrentUserData(boolean showMessage) {
        if (!authService.isAuthenticated()) {
            return;
        }
        User user = authService.getCurrentUser();
        Wallet wallet = user.getWallet();
        Map<String, Budget> budgets = budgetService.getAllBudgets();
        
        if (fileService.saveUserData(user.getLogin(), user.getPassword(), wallet, budgets)) {
            if (showMessage) {
                System.out.println("Данные пользователя сохранены в файл " + user.getLogin() + ".txt");
            }
        } else {
            if (showMessage) {
                System.out.println("Ошибка при сохранении данных пользователя.");
            }
        }
    }

    /**
     * Вывести статистику с форматированием и паузой
     */
    private void printStatisticsWithPause() {
        System.out.println("\n");
        System.out.println("========================================");
        System.out.println("===           СТАТИСТИКА            ===");
        System.out.println("========================================");
        System.out.println();
        
        statisticsService.printStatistics();
        
        System.out.println();
        System.out.println("========================================");
        System.out.println();
        waitForEnter();
    }

    /**
     * Ожидание нажатия Enter для продолжения
     */
    private void waitForEnter() {
        System.out.print("Нажмите Enter для продолжения...");
        scanner.nextLine();
        System.out.println();
    }

    /**
     * Проверить и вывести оповещения
     */
    private void checkAlerts() {
        double totalIncome = statisticsService.getTotalIncome();
        double totalExpense = statisticsService.getTotalExpense();

        boolean hasAlerts = false;

        if (totalExpense > totalIncome) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("⚠ ВНИМАНИЕ: Расходы превысили доходы!");
            System.out.println("========================================");
            hasAlerts = true;
        }

        var budgets = budgetService.getAllBudgets();
        boolean hasBudgetWarnings = false;
        boolean hasBudgetAlerts = false;
        
        for (var budget : budgets.values()) {
            String category = budget.getCategory();
            double limit = budget.getLimit();
            double remaining = budgetService.getRemainingBudget(category);
            double spent = budgetService.getSpentAmount(category);
            
            // Проверка превышения лимита
            if (budgetService.isBudgetExceeded(category)) {
                if (!hasBudgetWarnings) {
                    if (!hasAlerts) {
                        System.out.println();
                    }
                    System.out.println("========================================");
                    System.out.println("⚠ ПРЕДУПРЕЖДЕНИЯ О БЮДЖЕТЕ:");
                    System.out.println("========================================");
                    hasBudgetWarnings = true;
                    hasAlerts = true;
                }
                System.out.println(String.format("⚠ Превышен лимит бюджета для категории '%s'!",
                    category));
            }
            // Проверка: осталось 25% или меньше, но не исчерпан
            else if (remaining > 0 && remaining <= limit * 0.25) {
                if (!hasBudgetAlerts) {
                    if (!hasAlerts) {
                        System.out.println();
                    }
                    System.out.println("========================================");
                    System.out.println("⚠ УВЕДОМЛЕНИЯ О БЮДЖЕТЕ:");
                    System.out.println("========================================");
                    hasBudgetAlerts = true;
                    hasAlerts = true;
                }
                double percentage = (remaining / limit) * 100;
                System.out.println(String.format("⚠ Внимание: осталось %.2f%% лимита для категории '%s' (остаток: %.2f из %.2f)",
                    percentage, category, remaining, limit));
            }
            // Проверка: лимит исчерпан (остаток = 0)
            else if (remaining == 0 && spent > 0) {
                if (!hasBudgetAlerts) {
                    if (!hasAlerts) {
                        System.out.println();
                    }
                    System.out.println("========================================");
                    System.out.println("⚠ УВЕДОМЛЕНИЯ О БЮДЖЕТЕ:");
                    System.out.println("========================================");
                    hasBudgetAlerts = true;
                    hasAlerts = true;
                }
                System.out.println(String.format("⚠ Лимит бюджета для категории '%s' исчерпан (потрачено: %.2f из %.2f)",
                    category, spent, limit));
            }
        }
        
        if (hasAlerts) {
            System.out.println("========================================");
            System.out.println();
            waitForEnter();
        }
    }
}
