package com.kgm.service;

import com.kgm.config.AppConfig;

public class AuthService {

    public static boolean login(String username, String password) {
        String expectedUsername = AppConfig.adminUsername();
        String expectedPassword = AppConfig.adminPassword();
        return expectedPassword != null
                && !expectedPassword.isBlank()
                && expectedUsername != null
                && !expectedUsername.isBlank()
                && expectedUsername.equals(username == null ? "" : username.trim())
                && expectedPassword.equals(password == null ? "" : password);
    }

    public static boolean isAdminPassword(String password) {
        return login(AppConfig.adminUsername(), password);
    }
}
