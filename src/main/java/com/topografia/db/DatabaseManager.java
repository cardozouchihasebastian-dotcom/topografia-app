package com.topografia.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseManager {
    // Base de Datos SQL embebida (sin claves ni permisos requeridos)
    private static final String DB_URL = "jdbc:sqlite:database.sqlite";

    public static Connection getConnection() throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            System.out.println("[OK] Conectado exitosamente a la Base de Datos SQL!");
        } catch (Exception e) {
            System.err.println("[ERROR] Error en Base de Datos: " + e.getMessage());
        }
    }
}