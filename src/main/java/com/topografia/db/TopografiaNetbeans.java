package com.topografia;

import com.topografia.db.EquipoDAO;
import io.javalin.Javalin;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class TopografiaNetbeans {

    private static final int PORT = System.getenv("PORT") != null ? Integer.parseInt(System.getenv("PORT")) : 8086;
    private static EquipoDAO dao;

    public static void main(String[] args) {
        try {
            dao = new EquipoDAO();
        } catch (Throwable t) {
            t.printStackTrace();
            return;
        }

        try {
            String localIp = getLocalIpAddress();
            Javalin app = Javalin.create().start(PORT);

            // ================= PWA =================
            app.get("/manifest.json", ctx -> {
                ctx.contentType("application/manifest+json");
                ctx.result("{\n" +
                    "  \"name\": \"TopoTracker PRO\",\n" +
                    "  \"short_name\": \"TopoTracker\",\n" +
                    "  \"start_url\": \"/\",\n" +
                    "  \"display\": \"standalone\",\n" +
                    "  \"background_color\": \"#0d1b2a\",\n" +
                    "  \"theme_color\": \"#f59e0b\",\n" +
                    "  \"orientation\": \"portrait-primary\",\n" +
                    "  \"icons\": [\n" +
                    "    {\n" +
                    "      \"src\": \"https://cdn-icons-png.flaticon.com/512/3254/3254641.png\",\n" +
                    "      \"sizes\": \"512x512\",\n" +
                    "      \"type\": \"image/png\",\n" +
                    "      \"purpose\": \"any maskable\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}");
            });

            app.get("/sw.js", ctx -> {
                ctx.contentType("application/javascript");
                ctx.result("self.addEventListener('fetch', function(e) {});");
            });

            // ================= IMÁGENES =================
            app.get("/img/{nombre}", ctx -> {
                String nombre = ctx.pathParam("nombre");
                String[] posibles = {
                    "src/main/resources/img/" + nombre,
                    "src/main/resources/img/" + nombre + ".png",
                    "src/main/resources/img/" + nombre.replace(".png", "") + ".png.png"
                };

                for (String ruta : posibles) {
                    File f = new File(ruta);
                    if (f.exists() && !f.isDirectory()) {
                        ctx.contentType("image/png");
                        ctx.result(java.nio.file.Files.readAllBytes(f.toPath()));
                        return;
                    }
                }

                try (InputStream is = TopografiaNetbeans.class.getResourceAsStream("/img/" + nombre)) {
                    if (is != null) {
                        ctx.contentType("image/png");
                        ctx.result(is.readAllBytes());
                        return;
                    }
                } catch (Exception ignored) {}

                ctx.status(404).result("Imagen no encontrada");
            });

            // ================= VISTAS =================
            app.get("/", ctx -> {
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderLoginHtml(localIp));
            });

            app.get("/dashboard", ctx -> {
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderDashboardHtml(localIp));
            });

            app.get("/frentes", ctx -> {
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderFrentesHtml(localIp));
            });

            // ================= APIS =================
            app.post("/api/auth/login", ctx -> {
                try {
                    String body = ctx.body();
                    String username = extractJsonVal(body, "username");
                    String password = extractJsonVal(body, "password");

                    Map<String, Object> usuario = dao.autenticar(username, password);
                    if (usuario != null) {
                        ctx.contentType("application/json; charset=UTF-8");
                        ctx.result("{\"success\":true,\"usuario\":" + mapToJson(usuario) + "}");
                    } else {
                        ctx.status(401).contentType("application/json; charset=UTF-8");
                        ctx.result("{\"success\":false,\"error\":\"Usuario o contraseña incorrectos\"}");
                    }
                } catch (Exception e) {
                    ctx.status(400).result("{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
                }
            });

            app.post("/api/auth/register", ctx -> {
                try {
                    String body = ctx.body();
                    String username = extractJsonVal(body, "username");
                    String password = extractJsonVal(body, "password");
                    String nombre = extractJsonVal(body, "nombre_completo");
                    String empresa = extractJsonVal(body, "empresa");

                    int newId = dao.registrarUsuario(username, password, nombre, empresa);
                    if (newId > 0) {
                        ctx.contentType("application/json; charset=UTF-8");
                        ctx.result("{\"success\":true,\"id\":" + newId + ",\"message\":\"Usuario registrado exitosamente\"}");
                    } else {
                        ctx.status(400).result("{\"success\":false,\"error\":\"El usuario ya existe\"}");
                    }
                } catch (Exception e) {
                    ctx.status(400).result("{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
                }
            });

            app.delete("/api/auth/usuario/{id}", ctx -> {
                try {
                    int usuarioId = Integer.parseInt(ctx.pathParam("id"));
                    boolean ok = dao.eliminarUsuario(usuarioId);
                    ctx.result("{\"success\":" + ok + "}");
                } catch (Exception e) {
                    ctx.status(400).result("{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
                }
            });

            app.get("/api/equipos", ctx -> {
                int usuarioId = ctx.queryParamAsClass("usuario_id", Integer.class).getOrDefault(1);
                List<Map<String, Object>> equipos = dao.getEquiposPorUsuario(usuarioId);
                ctx.contentType("application/json; charset=UTF-8");
                ctx.result(listMapToJson(equipos));
            });

            app.post("/api/equipos", ctx -> {
                try {
                    String body = ctx.body();
                    int usuarioId = Integer.parseInt(extractJsonVal(body, "usuario_id", "1"));
                    String serie = extractJsonVal(body, "serie");
                    String marca = extractJsonVal(body, "marca");
                    String modelo = extractJsonVal(body, "modelo");
                    String tipo = extractJsonVal(body, "tipo");
                    String estado = extractJsonVal(body, "estado", "DISPONIBLE");
                    String calib = extractJsonVal(body, "fecha_vencimiento_calibracion", null);

                    int id = dao.crearEquipo(usuarioId, serie, marca, modelo, tipo, estado, calib);
                    ctx.result("{\"success\":true,\"id\":" + id + "}");
                } catch (Exception e) {
                    ctx.status(400).result("{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
                }
            });

            app.get("/api/frentes", ctx -> {
                int usuarioId = ctx.queryParamAsClass("usuario_id", Integer.class).getOrDefault(1);
                List<Map<String, Object>> frentes = dao.listarFrentesPorUsuario(usuarioId);
                ctx.contentType("application/json; charset=UTF-8");
                ctx.result(listMapToJson(frentes));
            });

            app.post("/api/frentes", ctx -> {
                try {
                    String body = ctx.body();
                    int usuarioId = Integer.parseInt(extractJsonVal(body, "usuario_id", "1"));
                    String nombre = extractJsonVal(body, "nombre");
                    String ubicacion = extractJsonVal(body, "ubicacion");

                    int id = dao.crearFrente(usuarioId, nombre, ubicacion);
                    ctx.result("{\"success\":true,\"id\":" + id + "}");
                } catch (Exception e) {
                    ctx.status(400).result("{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
                }
            });

            app.post("/api/traslados", ctx -> {
                try {
                    String body = ctx.body();
                    int usuarioId = Integer.parseInt(extractJsonVal(body, "usuario_id", "1"));
                    int equipoId = Integer.parseInt(extractJsonVal(body, "equipo_id"));
                    int frenteDestinoId = Integer.parseInt(extractJsonVal(body, "frente_destino_id"));
                    String motivo = extractJsonVal(body, "motivo", "Reasignación de frente");

                    boolean ok = dao.reasignarFrente(usuarioId, equipoId, frenteDestinoId, motivo);
                    ctx.result("{\"success\":" + ok + "}");
                } catch (Exception e) {
                    ctx.status(400).result("{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
                }
            });

            app.get("/api/traslados/historial", ctx -> {
                int usuarioId = ctx.queryParamAsClass("usuario_id", Integer.class).getOrDefault(1);
                List<Map<String, Object>> traslados = dao.getHistorialTraslados(usuarioId);
                ctx.contentType("application/json; charset=UTF-8");
                ctx.result(listMapToJson(traslados));
            });

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static String extractJsonVal(String json, String key) { return extractJsonVal(json, key, ""); }
    private static String extractJsonVal(String json, String key, String def) {
        if (json == null) return def;
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return def;
        int colon = json.indexOf(":", idx);
        if (colon == -1) return def;
        String rest = json.substring(colon + 1).trim();
        if (rest.startsWith("\"")) {
            int end = rest.indexOf("\"", 1);
            return end != -1 ? rest.substring(1, end) : def;
        } else {
            int end = rest.indexOf(",");
            if (end == -1) end = rest.indexOf("}");
            return end != -1 ? rest.substring(0, end).trim() : def;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    private static String mapToJson(Map<String, Object> map) {
        if (map == null) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else sb.append("\"").append(escape(v.toString())).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String listMapToJson(List<Map<String, Object>> list) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(mapToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().contains(".") && !addr.getHostAddress().startsWith("127.")) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private static String renderHead(String title) {
        return "<head>\n" +
            "  <meta charset='UTF-8'>\n" +
            "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
            "  <title>" + title + "</title>\n" +
            "  <link rel='manifest' href='/manifest.json'>\n" +
            "  <meta name='theme-color' content='#f59e0b'>\n" +
            "  <meta name='apple-mobile-web-app-capable' content='yes'>\n" +
            "  <style>\n" +
            "    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }\n" +
            "    body { color: #f8fafc; min-height: 100vh; display: flex; flex-direction: column; background-size: cover; background-position: center; background-attachment: fixed; }\n" +
            "    .navbar { display: flex; align-items: center; justify-content: space-between; padding: 12px 24px; background: rgba(13, 27, 42, 0.9); backdrop-filter: blur(12px); border-bottom: 1px solid rgba(255,255,255,0.12); flex-wrap: wrap; gap: 10px; }\n" +
            "    .logo { font-size: 1.25rem; font-weight: 800; color: #f59e0b; text-decoration: none; }\n" +
            "    .nav-links { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }\n" +
            "    .nav-link { color: #e2e8f0; text-decoration: none; font-weight: 600; padding: 6px 12px; border-radius: 6px; font-size: 0.9rem; }\n" +
            "    .nav-link:hover, .nav-link.active { background: rgba(245, 158, 11, 0.2); color: #f59e0b; }\n" +
            "    .btn-action { background: #3b82f6; color: white; border: none; padding: 7px 14px; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 0.85rem; }\n" +
            "    .btn-action:hover { background: #2563eb; }\n" +
            "    .btn-danger { background: #ef4444; color: white; border: none; padding: 7px 12px; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 0.85rem; }\n" +
            "    .content-container { flex: 1; padding: 24px; max-width: 1400px; margin: 0 auto; width: 100%; }\n" +
            "    .card { background: rgba(15, 23, 42, 0.88); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 20px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); margin-bottom: 24px; }\n" +
            "    .modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.75); backdrop-filter: blur(5px); z-index: 1000; justify-content: center; align-items: center; padding: 15px; }\n" +
            "    .modal-content { background: #0f172a; border: 1px solid rgba(255,255,255,0.2); border-radius: 12px; padding: 24px; width: 100%; max-width: 500px; color: white; }\n" +
            "    .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; font-size: 1.2rem; font-weight: 700; color: #f59e0b; }\n" +
            "    .modal-close { background: transparent; border: none; color: #94a3b8; font-size: 1.5rem; cursor: pointer; }\n" +
            "    .form-group { margin-bottom: 14px; text-align: left; }\n" +
            "    .form-group label { display: block; font-size: 0.85rem; color: #cbd5e1; margin-bottom: 6px; font-weight: 600; }\n" +
            "    .form-control { width: 100%; padding: 10px 12px; border-radius: 6px; border: 1px solid #334155; background: #1e293b; color: white; font-size: 0.95rem; }\n" +
            "    .grid-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(290px, 1fr)); gap: 18px; }\n" +
            "    .badge { padding: 4px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: 700; }\n" +
            "    .badge-ok { background: rgba(34, 197, 94, 0.2); color: #4ade80; border: 1px solid #22c55e; }\n" +
            "  </style>\n" +
            "</head>";
    }

    private static String renderLoginHtml(String localIp) {
        return "<!DOCTYPE html><html lang='es'>\n" +
            renderHead("TopoTracker PRO - Iniciar Sesión") +
            "<body style='background-image: linear-gradient(rgba(10,25,47,0.8), rgba(10,25,47,0.88)), url(\"/img/login-bg.png\"); justify-content: center; align-items: center;'>\n" +
            "  <div class='card' style='width: 100%; max-width: 420px; text-align: center; border: 1px solid rgba(245,158,11,0.3);'>\n" +
            "    <div style='font-size: 2rem; margin-bottom: 4px;'>🛰️</div>\n" +
            "    <h1 style='color: #f59e0b; font-size: 1.6rem; font-weight: 800; margin-bottom: 4px;'>TopoTracker PRO</h1>\n" +
            "    <p style='color: #94a3b8; font-size: 0.85rem; margin-bottom: 20px;'>Control de Equipos y Topografía en Tiempo Real</p>\n" +
            "    <form id='loginForm' onsubmit='handleLogin(event)'>\n" +
            "      <div class='form-group'><label>Usuario</label><input type='text' id='logUser' class='form-control' required></div>\n" +
            "      <div class='form-group'><label>Contraseña</label><input type='password' id='logPass' class='form-control' required></div>\n" +
            "      <button type='submit' class='btn-action' style='width: 100%; padding: 12px; font-size: 1rem; background: #f59e0b; color: #000; font-weight: bold;'>Ingresar al Sistema</button>\n" +
            "    </form>\n" +
            "    <div style='margin-top: 18px; font-size: 0.88rem; color: #94a3b8;'>\n" +
            "      ¿No tienes cuenta? <a href='javascript:void(0)' onclick='abrirModalRegistro()' style='color: #38bdf8; font-weight: bold;'>Regístrate aquí</a>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "  <div id='modalRegistro' class='modal'>\n" +
            "    <div class='modal-content'>\n" +
            "      <div class='modal-header'><span>📝 Registrar Nueva Cuenta</span><button class='modal-close' onclick='cerrarModalRegistro()'>&times;</button></div>\n" +
            "      <form onsubmit='handleRegistro(event)'>\n" +
            "        <div class='form-group'><label>Nombre Completo</label><input type='text' id='regNombre' class='form-control' required></div>\n" +
            "        <div class='form-group'><label>Empresa u Obra</label><input type='text' id='regEmpresa' class='form-control' required></div>\n" +
            "        <div class='form-group'><label>Usuario</label><input type='text' id='regUsername' class='form-control' required></div>\n" +
            "        <div class='form-group'><label>Contraseña</label><input type='password' id='regPassword' class='form-control' required></div>\n" +
            "        <button type='submit' class='btn-action' style='width: 100%; padding: 10px; background: #10b981; font-weight: bold;'>Crear Cuenta</button>\n" +
            "      </form>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "<script>\n" +
            "  function abrirModalRegistro() { document.getElementById('modalRegistro').style.display = 'flex'; }\n" +
            "  function cerrarModalRegistro() { document.getElementById('modalRegistro').style.display = 'none'; }\n" +
            "  async function handleLogin(e) {\n" +
            "    e.preventDefault();\n" +
            "    const u = document.getElementById('logUser').value.trim();\n" +
            "    const p = document.getElementById('logPass').value.trim();\n" +
            "    const r = await fetch('/api/auth/login', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({username:u, password:p}) });\n" +
            "    const d = await r.json();\n" +
            "    if (d.success) {\n" +
            "      localStorage.setItem('topo_user', JSON.stringify(d.usuario));\n" +
            "      window.location.href = '/dashboard';\n" +
            "    } else { alert('❌ ' + d.error); }\n" +
            "  }\n" +
            "  async function handleRegistro(e) {\n" +
            "    e.preventDefault();\n" +
            "    const r = await fetch('/api/auth/register', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({nombre_completo:document.getElementById('regNombre').value, empresa:document.getElementById('regEmpresa').value, username:document.getElementById('regUsername').value, password:document.getElementById('regPassword').value}) });\n" +
            "    const d = await r.json();\n" +
            "    if (d.success) { alert('✅ ¡Cuenta creada!'); cerrarModalRegistro(); }\n" +
            "    else { alert('❌ ' + d.error); }\n" +
            "  }\n" +
            "</script></body></html>";
    }

    private static String renderDashboardHtml(String localIp) {
        return "<!DOCTYPE html><html lang='es'>\n" +
            renderHead("TopoTracker PRO - Equipos y RTK") +
            "<body style='background-image: linear-gradient(rgba(15,23,42,0.88), rgba(15,23,42,0.92)), url(\"/img/equipos-bg.png\");'>\n" +
            "  <div class='navbar'>\n" +
            "    <a href='/dashboard' class='logo'>🛰️ TopoTracker PRO</a>\n" +
            "    <div class='nav-links'>\n" +
            "      <a href='/dashboard' class='nav-link active'>📦 Equipos</a>\n" +
            "      <a href='/frentes' class='nav-link'>📍 Frentes & Bitácora</a>\n" +
            "      <button class='btn-action' style='background:#10b981;' onclick='abrirModalNuevoEquipo()'>+ Nuevo Equipo</button>\n" +
            "      <span style='font-size:0.85rem; color:#94a3b8; margin-left:8px;'>👤 <span id='userNombre' style='color:#f59e0b; font-weight:bold;'></span></span>\n" +
            "      <button class='btn-danger' onclick='eliminarMiCuenta()'>🗑️ Eliminar</button>\n" +
            "      <button class='btn-action' style='background:#64748b;' onclick='cerrarSesion()'>Salir</button>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "  <div class='content-container'>\n" +
            "    <div class='card' style='display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;'>\n" +
            "      <div>\n" +
            "        <h2 style='color:#f8fafc; font-size:1.4rem;'>Parque de Equipos Topográficos</h2>\n" +
            "        <p style='color:#94a3b8; font-size:0.85rem;'>Control de Calibraciones y Asignaciones</p>\n" +
            "      </div>\n" +
            "      <div style='display:flex; gap:16px; font-size:0.9rem;'>\n" +
            "        <div>Total: <b id='cntTotal' style='color:#f59e0b; font-size:1.1rem;'>0</b></div>\n" +
            "        <div>Operativos: <b id='cntOp' style='color:#4ade80; font-size:1.1rem;'>0</b></div>\n" +
            "      </div>\n" +
            "    </div>\n" +
            "    <div id='equiposGrid' class='grid-cards'></div>\n" +
            "  </div>\n" +
            "  <div id='modalEquipo' class='modal'>\n" +
            "    <div class='modal-content'>\n" +
            "      <div class='modal-header'><span>+ Registrar Nuevo Equipo</span><button class='modal-close' onclick='cerrarModalNuevoEquipo()'>&times;</button></div>\n" +
            "      <form onsubmit='crearEquipo(event)'>\n" +
            "        <div class='form-group'><label>Tipo</label><select id='eqTipo' class='form-control'><option>ESTACION_TOTAL</option><option>GPS_DIFERENCIAL</option><option>NIVEL_DIGITAL</option><option>NIVEL_OPTICO</option><option>DRON</option></select></div>\n" +
            "        <div class='form-group'><label>Marca</label><input type='text' id='eqMarca' class='form-control' required></div>\n" +
            "        <div class='form-group'><label>Modelo</label><input type='text' id='eqModelo' class='form-control' required></div>\n" +
            "        <div class='form-group'><label>Serie</label><input type='text' id='eqSerie' class='form-control' required></div>\n" +
            "        <div class='form-group'><label>Vencimiento Calibración</label><input type='date' id='eqCalib' class='form-control'></div>\n" +
            "        <button type='submit' class='btn-action' style='width:100%; padding:10px; background:#f59e0b; color:#000; font-weight:bold;'>Guardar</button>\n" +
            "      </form>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "<script>\n" +
            "  let currentUser = JSON.parse(localStorage.getItem('topo_user') || '{}');\n" +
            "  window.onload = () => {\n" +
            "    if (!currentUser.id) { window.location.href = '/'; return; }\n" +
            "    document.getElementById('userNombre').innerText = currentUser.nombre_completo || currentUser.username;\n" +
            "    cargarEquipos();\n" +
            "  };\n" +
            "  function cerrarSesion() { localStorage.removeItem('topo_user'); window.location.href = '/'; }\n" +
            "  function abrirModalNuevoEquipo() { document.getElementById('modalEquipo').style.display = 'flex'; }\n" +
            "  function cerrarModalNuevoEquipo() { document.getElementById('modalEquipo').style.display = 'none'; }\n" +
            "  async function cargarEquipos() {\n" +
            "    const r = await fetch('/api/equipos?usuario_id=' + currentUser.id);\n" +
            "    const data = await r.json();\n" +
            "    const grid = document.getElementById('equiposGrid');\n" +
            "    grid.innerHTML = '';\n" +
            "    document.getElementById('cntTotal').innerText = data.length;\n" +
            "    document.getElementById('cntOp').innerText = data.length;\n" +
            "    if (data.length === 0) {\n" +
            "      grid.innerHTML = '<div style=\"grid-column: 1/-1; text-align:center; color:#94a3b8; padding:30px;\">No hay equipos registrados. Presiona <b>+ Nuevo Equipo</b>.</div>';\n" +
            "      return;\n" +
            "    }\n" +
            "    data.forEach(e => {\n" +
            "      grid.innerHTML += `<div class='card' style='margin-bottom:0;'>\n" +
            "        <div style='display:flex; justify-content:space-between; margin-bottom:8px;'><span style='font-size:0.75rem; color:#94a3b8;'>${e.tipo}</span><span class='badge badge-ok'>${e.estado}</span></div>\n" +
            "        <div style='font-size:1.15rem; font-weight:700; color:#f8fafc;'>${e.marca} ${e.modelo}</div>\n" +
            "        <div style='font-size:0.85rem; color:#94a3b8; margin-bottom:12px;'>S/N: ${e.serie}</div>\n" +
            "        <div style='font-size:0.8rem; background:rgba(0,0,0,0.3); padding:8px; border-radius:6px;'>\n" +
            "          <div>📍 Frente: <b style='color:#38bdf8;'>${e.frente_nombre || 'Asignado'}</b></div>\n" +
            "          <div>📅 Calibración: <b style='color:#f59e0b;'>${e.fecha_vencimiento_calibracion || 'Al día'}</b></div>\n" +
            "        </div></div>`;\n" +
            "    });\n" +
            "  }\n" +
            "  async function crearEquipo(e) {\n" +
            "    e.preventDefault();\n" +
            "    const body = { usuario_id: currentUser.id, tipo: document.getElementById('eqTipo').value, marca: document.getElementById('eqMarca').value, modelo: document.getElementById('eqModelo').value, serie: document.getElementById('eqSerie').value, fecha_vencimiento_calibracion: document.getElementById('eqCalib').value };\n" +
            "    const r = await fetch('/api/equipos', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body) });\n" +
            "    if ((await r.json()).success) { cerrarModalNuevoEquipo(); cargarEquipos(); }\n" +
            "  }\n" +
            "  async function eliminarMiCuenta() {\n" +
            "    if (!confirm('¿Eliminar tu cuenta?')) return;\n" +
            "    await fetch('/api/auth/usuario/' + currentUser.id, { method:'DELETE' });\n" +
            "    cerrarSesion();\n" +
            "  }\n" +
            "</script></body></html>";
    }

    private static String renderFrentesHtml(String localIp) {
        return "<!DOCTYPE html><html lang='es'>\n" +
            renderHead("TopoTracker PRO - Frentes & Bitácora") +
            "<body style='background-image: linear-gradient(rgba(15,23,42,0.88), rgba(15,23,42,0.92)), url(\"/img/mapa-bg.png\");'>\n" +
            "  <div class='navbar'>\n" +
            "    <a href='/dashboard' class='logo'>🛰️ TopoTracker PRO</a>\n" +
            "    <div class='nav-links'>\n" +
            "      <a href='/dashboard' class='nav-link'>📦 Equipos</a>\n" +
            "      <a href='/frentes' class='nav-link active'>📍 Frentes & Bitácora</a>\n" +
            "      <button class='btn-action' style='background:#64748b;' onclick='location.href=\"/dashboard\"'>Volver</button>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "  <div class='content-container'><div class='card'><h2 style='color:#f8fafc; font-size:1.3rem; margin-bottom:12px;'>📍 Frentes de Obra</h2><div id='frentesGrid' class='grid-cards'></div></div></div>\n" +
            "<script>\n" +
            "  let currentUser = JSON.parse(localStorage.getItem('topo_user') || '{}');\n" +
            "  window.onload = async () => {\n" +
            "    const r = await fetch('/api/frentes?usuario_id=' + currentUser.id);\n" +
            "    const data = await r.json();\n" +
            "    const grid = document.getElementById('frentesGrid');\n" +
            "    grid.innerHTML = '';\n" +
            "    data.forEach(f => {\n" +
            "      grid.innerHTML += `<div class='card' style='margin-bottom:0;'><div style='font-size:1.1rem; font-weight:bold; color:#38bdf8;'>📍 ${f.nombre}</div><div style='font-size:0.85rem; color:#94a3b8; margin-top:4px;'>${f.ubicacion||''}</div></div>`;\n" +
            "    });\n" +
            "  };\n" +
            "</script></body></html>";
    }
}
