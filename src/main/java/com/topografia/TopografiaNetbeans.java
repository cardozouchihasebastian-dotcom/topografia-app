package com.topografia.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.net.URI;

public class DatabaseManager {

    public static Connection getConnection() throws Exception {
        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
            URI uri = new URI(databaseUrl);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String dbName = uri.getPath().substring(1);
            String user = uri.getUserInfo().split(":")[0];
            String pass = uri.getUserInfo().split(":")[1];
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
            return DriverManager.getConnection(jdbcUrl, user, pass);
        } else {
            return DriverManager.getConnection("jdbc:sqlite:database.sqlite");
        }
    }
}
