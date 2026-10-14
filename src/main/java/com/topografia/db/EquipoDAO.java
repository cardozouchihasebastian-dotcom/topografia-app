package com.topografia.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipoDAO {

    private boolean esPostgres = false;

    public EquipoDAO() {
        esPostgres = System.getenv("DATABASE_URL") != null;
        inicializarBaseDatos();
    }

    private void inicializarBaseDatos() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            if (esPostgres) {
                stmt.execute("CREATE TABLE IF NOT EXISTS usuarios (" +
                        "id SERIAL PRIMARY KEY, " +
                        "username TEXT UNIQUE NOT NULL, " +
                        "password TEXT NOT NULL, " +
                        "nombre_completo TEXT, " +
                        "empresa TEXT);");

                stmt.execute("CREATE TABLE IF NOT EXISTS frentes_obra (" +
                        "id SERIAL PRIMARY KEY, " +
                        "usuario_id INTEGER DEFAULT 1, " +
                        "nombre TEXT NOT NULL, " +
                        "ubicacion TEXT);");

                stmt.execute("CREATE TABLE IF NOT EXISTS equipos (" +
                        "id SERIAL PRIMARY KEY, " +
                        "usuario_id INTEGER DEFAULT 1, " +
                        "serie TEXT NOT NULL, " +
                        "marca TEXT NOT NULL, " +
                        "modelo TEXT NOT NULL, " +
                        "tipo TEXT NOT NULL, " +
                        "estado TEXT DEFAULT 'DISPONIBLE', " +
                        "frente_id INTEGER, " +
                        "fecha_vencimiento_calibracion TEXT);");

                stmt.execute("CREATE TABLE IF NOT EXISTS historial_traslados (" +
                        "id SERIAL PRIMARY KEY, " +
                        "usuario_id INTEGER DEFAULT 1, " +
                        "equipo_id INTEGER, " +
                        "frente_origen_id INTEGER, " +
                        "frente_destino_id INTEGER, " +
                        "fecha_traslado TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "motivo TEXT);");

                // Crear usuario sebastian
                stmt.execute("INSERT INTO usuarios (username, password, nombre_completo, empresa) " +
                        "VALUES ('sebastian', '1234', 'Sebastian C.', 'Topografia General') " +
                        "ON CONFLICT (username) DO NOTHING;");

            } else {
                stmt.execute("CREATE TABLE IF NOT EXISTS usuarios (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "username TEXT UNIQUE NOT NULL, " +
                        "password TEXT NOT NULL, " +
                        "nombre_completo TEXT, " +
                        "empresa TEXT);");

                stmt.execute("CREATE TABLE IF NOT EXISTS frentes_obra (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "usuario_id INTEGER DEFAULT 1, " +
                        "nombre TEXT NOT NULL, " +
                        "ubicacion TEXT);");

                stmt.execute("CREATE TABLE IF NOT EXISTS equipos (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "usuario_id INTEGER DEFAULT 1, " +
                        "serie TEXT NOT NULL, " +
                        "marca TEXT NOT NULL, " +
                        "modelo TEXT NOT NULL, " +
                        "tipo TEXT NOT NULL, " +
                        "estado TEXT DEFAULT 'DISPONIBLE', " +
                        "frente_id INTEGER, " +
                        "fecha_vencimiento_calibracion TEXT);");

                stmt.execute("CREATE TABLE IF NOT EXISTS historial_traslados (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "usuario_id INTEGER DEFAULT 1, " +
                        "equipo_id INTEGER, " +
                        "frente_origen_id INTEGER, " +
                        "frente_destino_id INTEGER, " +
                        "fecha_traslado DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "motivo TEXT);");

                stmt.execute("INSERT OR IGNORE INTO usuarios (id, username, password, nombre_completo, empresa) " +
                        "VALUES (1, 'sebastian', '1234', 'Sebastian C.', 'Topografia General');");
            }

            // Inyectar datos iniciales si no existen
            precargarDatosIniciales(conn);

            System.out.println(">>> BASE DE DATOS Y EQUIPOS INICIALIZADOS CORRECTAMENTE");

        } catch (Exception e) {
            System.err.println(">>> ERROR AL CREAR TABLAS O INICIALIZAR:");
            e.printStackTrace();
        }
    }

    private void precargarDatosIniciales(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM equipos")) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Obtener ID del usuario sebastian
                int uId = 1;
                try (ResultSet rsU = stmt.executeQuery("SELECT id FROM usuarios WHERE username = 'sebastian'")) {
                    if (rsU.next()) uId = rsU.getInt("id");
                }

                // 1. Frentes Iniciales
                stmt.execute("INSERT INTO frentes_obra (usuario_id, nombre, ubicacion) VALUES " +
                        "(" + uId + ", 'Tramo Central Km 12', 'Sector Norte')," +
                        "(" + uId + ", 'Puente Río Claro', 'Sector Sur')," +
                        "(" + uId + ", 'Túnel La Línea Acceso', 'Sector Cordillera')," +
                        "(" + uId + ", 'Plataforma Parque Logístico', 'Zona Industrial')," +
                        "(" + uId + ", 'Campamento Base', 'Almacén Central');");

                // 2. Equipos Reales Topográficos
                stmt.execute("INSERT INTO equipos (usuario_id, serie, marca, modelo, tipo, estado, frente_id, fecha_vencimiento_calibracion) VALUES " +
                        "(" + uId + ", 'TG-98214', 'Topcon', 'GTS-235W', 'ESTACION_TOTAL', 'DISPONIBLE', 1, '2026-12-15')," +
                        "(" + uId + ", 'RD-4401', 'Ruide', 'RL-C32', 'NIVEL_OPTICO', 'DISPONIBLE', 2, '2026-11-20')," +
                        "(" + uId + ", 'RD-4402', 'Ruide', 'RL-C32', 'NIVEL_OPTICO', 'DISPONIBLE', 1, '2026-11-20')," +
                        "(" + uId + ", 'RD-4403', 'Ruide', 'RL-C32', 'NIVEL_OPTICO', 'DISPONIBLE', 3, '2026-11-20')," +
                        "(" + uId + ", 'RD-4404', 'Ruide', 'RL-C32', 'NIVEL_OPTICO', 'DISPONIBLE', 4, '2026-10-30')," +
                        "(" + uId + ", 'RD-4405', 'Ruide', 'RL-C32', 'NIVEL_OPTICO', 'DISPONIBLE', 5, '2026-10-30')," +
                        "(" + uId + ", 'RD-5510', 'Ruide', 'RL-C33', 'NIVEL_OPTICO', 'DISPONIBLE', 2, '2026-12-01')," +
                        "(" + uId + ", 'HT-8820', 'Hi-Target', 'B60', 'GPS_DIFERENCIAL', 'DISPONIBLE', 3, '2027-01-15')," +
                        "(" + uId + ", 'HT-9930', 'Hi-Target', 'B300', 'GPS_DIFERENCIAL', 'DISPONIBLE', 1, '2027-02-10');");

                System.out.println(">>> DATOS REALES CARGADOS EXITOSAMENTE");
            }
        } catch (Exception e) {
            System.err.println(">>> AVISO PRECARGA: " + e.getMessage());
        }
    }

    public Map<String, Object> autenticar(String username, String password) {
        String sql = "SELECT id, username, nombre_completo, empresa FROM usuarios WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> u = new HashMap<>();
                u.put("id", rs.getInt("id"));
                u.put("username", rs.getString("username"));
                u.put("nombre_completo", rs.getString("nombre_completo"));
                u.put("empresa", rs.getString("empresa"));
                return u;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int registrarUsuario(String username, String password, String nombreCompleto, String empresa) {
        String sql = "INSERT INTO usuarios (username, password, nombre_completo, empresa) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, nombreCompleto);
            ps.setString(4, empresa);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println(">>> ERROR AL REGISTRAR USUARIO: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public boolean eliminarUsuario(int usuarioId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM historial_traslados WHERE usuario_id = ?")) {
                    ps.setInt(1, usuarioId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM equipos WHERE usuario_id = ?")) {
                    ps.setInt(1, usuarioId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM frentes_obra WHERE usuario_id = ?")) {
                    ps.setInt(1, usuarioId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM usuarios WHERE id = ?")) {
                    ps.setInt(1, usuarioId); ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> getEquiposPorUsuario(int usuarioId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT e.*, f.nombre as frente_nombre FROM equipos e " +
                     "LEFT JOIN frentes_obra f ON e.frente_id = f.id " +
                     "WHERE e.usuario_id = ? ORDER BY e.id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("serie", rs.getString("serie"));
                row.put("marca", rs.getString("marca"));
                row.put("modelo", rs.getString("modelo"));
                row.put("tipo", rs.getString("tipo"));
                row.put("estado", rs.getString("estado"));
                row.put("frente_nombre", rs.getString("frente_nombre"));
                row.put("fecha_vencimiento_calibracion", rs.getString("fecha_vencimiento_calibracion"));
                list.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public int crearEquipo(int usuarioId, String serie, String marca, String modelo, String tipo, String estado, String calib) {
        String sql = "INSERT INTO equipos (usuario_id, serie, marca, modelo, tipo, estado, fecha_vencimiento_calibracion) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, serie);
            ps.setString(3, marca);
            ps.setString(4, modelo);
            ps.setString(5, tipo);
            ps.setString(6, estado);
            ps.setString(7, calib);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<Map<String, Object>> listarFrentesPorUsuario(int usuarioId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM frentes_obra WHERE usuario_id = ? ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("nombre", rs.getString("nombre"));
                row.put("ubicacion", rs.getString("ubicacion"));
                list.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public int crearFrente(int usuarioId, String nombre, String ubicacion) {
        String sql = "INSERT INTO frentes_obra (usuario_id, nombre, ubicacion) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, nombre);
            ps.setString(3, ubicacion);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean reasignarFrente(int usuarioId, int equipoId, int frenteDestinoId, String motivo) {
        String sqlOrigen = "SELECT frente_id FROM equipos WHERE id = ? AND usuario_id = ?";
        String sqlUpdate = "UPDATE equipos SET frente_id = ?, estado = 'ASIGNADO' WHERE id = ? AND usuario_id = ?";
        String sqlHist = "INSERT INTO historial_traslados (usuario_id, equipo_id, frente_origen_id, frente_destino_id, motivo) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection()) {
            Integer frenteOrigen = null;
            try (PreparedStatement ps = conn.prepareStatement(sqlOrigen)) {
                ps.setInt(1, equipoId);
                ps.setInt(2, usuarioId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) frenteOrigen = (Integer) rs.getObject("frente_id");
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setInt(1, frenteDestinoId);
                ps.setInt(2, equipoId);
                ps.setInt(3, usuarioId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlHist)) {
                ps.setInt(1, usuarioId);
                ps.setInt(2, equipoId);
                if (frenteOrigen != null) ps.setInt(3, frenteOrigen); else ps.setNull(3, Types.INTEGER);
                ps.setInt(4, frenteDestinoId);
                ps.setString(5, motivo);
                ps.executeUpdate();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> getHistorialTraslados(int usuarioId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT h.fecha_traslado, h.motivo, e.marca, e.modelo, e.serie, " +
                     "fo.nombre as frente_origen, fd.nombre as frente_destino " +
                     "FROM historial_traslados h " +
                     "JOIN equipos e ON h.equipo_id = e.id " +
                     "LEFT JOIN frentes_obra fo ON h.frente_origen_id = fo.id " +
                     "LEFT JOIN frentes_obra fd ON h.frente_destino_id = fd.id " +
                     "WHERE h.usuario_id = ? ORDER BY h.id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("fecha_traslado", rs.getString("fecha_traslado"));
                row.put("motivo", rs.getString("motivo"));
                row.put("marca", rs.getString("marca"));
                row.put("modelo", rs.getString("modelo"));
                row.put("serie", rs.getString("serie"));
                row.put("frente_origen", rs.getString("frente_origen"));
                row.put("frente_destino", rs.getString("frente_destino"));
                list.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
