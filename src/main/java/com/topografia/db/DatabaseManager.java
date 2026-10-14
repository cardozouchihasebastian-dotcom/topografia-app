package com.topografia.db;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseManager {

    private static final String SQLITE_URL = "jdbc:sqlite:database.sqlite";
    private static boolean usandoPostgres = false;

    public static Connection getConnection() throws Exception {
        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            usandoPostgres = true;
            return conectarPostgres(databaseUrl);
        } else {
            usandoPostgres = false;
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(SQLITE_URL);
        }
    }

    private static Connection conectarPostgres(String databaseUrl) throws Exception {
        Class.forName("org.postgresql.Driver");

        URI dbUri = new URI(databaseUrl);
        String[] userInfo = dbUri.getUserInfo().split(":");
        String username = userInfo[0];
        String password = userInfo[1];

        String host = dbUri.getHost();
        int port = dbUri.getPort();
        String dbName = dbUri.getPath().replace("/", "");

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName + "?sslmode=require";

        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public static boolean esPostgres() {
        return usandoPostgres;
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            if (usandoPostgres) {
                System.out.println("[OK] Conectado exitosamente a PostgreSQL (Render)!");
            } else {
                stmt.execute("PRAGMA foreign_keys = ON;");
                System.out.println("[OK] Conectado exitosamente a SQLite (local)!");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Error en Base de Datos: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
