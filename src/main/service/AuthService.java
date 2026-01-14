package service;

import java.util.HashMap;
import java.util.Map;
import model.User;

/**
 * Сервис для управления авторизацией пользователей
 */
public class AuthService {
    private Map<String, User> users;
    private User currentUser;

    public AuthService() {
        this.users = new HashMap<>();
        this.currentUser = null;
    }

    /**
     * Регистрация нового пользователя
     */
    public boolean register(String login, String password) {
        if (login == null || login.trim().isEmpty()) {
            return false;
        }
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        if (users.containsKey(login)) {
            return false; 
        }
        users.put(login, new User(login, password));
        return true;
    }

    /**
     * Авторизация пользователя
     */
    public boolean login(String login, String password) {
        if (login == null || password == null) {
            return false;
        }
        User user = users.get(login);
        if (user != null && user.getPassword().equals(password)) {
            currentUser = user;
            return true;
        }
        return false;
    }

    /**
     * Выход из системы
     */
    public void logout() {
        currentUser = null;
    }

    /**
     * Получить текущего авторизованного пользователя
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Проверить, авторизован ли пользователь
     */
    public boolean isAuthenticated() {
        return currentUser != null;
    }

    /**
     * Получить всех пользователей (для загрузки из файла)
     */
    public Map<String, User> getUsers() {
        return users;
    }

    /**
     * Установить пользователей (для загрузки из файла)
     */
    public void setUsers(Map<String, User> users) {
        this.users = users;
    }

    /**
     * Получить пользователя по логину
     */
    public User getUserByLogin(String login) {
        return users.get(login);
    }
}
