-- 1. TABLA DE OBRAS CIVILES
CREATE TABLE IF NOT EXISTS obras (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(150) NOT NULL,
    codigo_obra VARCHAR(50) UNIQUE NOT NULL,
    ubicacion VARCHAR(200) NOT NULL
);

-- 2. TABLA DE FRENTES DE OBRA
CREATE TABLE IF NOT EXISTS frentes_obra (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    obra_id INTEGER NOT NULL,
    nombre_frente VARCHAR(150) NOT NULL,
    tramo_sector VARCHAR(100),
    FOREIGN KEY (obra_id) REFERENCES obras(id) ON DELETE CASCADE
);

-- 3. TABLA DE OPERADORES / TOPÓGRAFOS
CREATE TABLE IF NOT EXISTS operadores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(120) NOT NULL,
    dni_id VARCHAR(30) UNIQUE NOT NULL,
    telefono VARCHAR(30)
);

-- 4. TABLA DE INVENTARIO DE EQUIPOS DE TOPOGRAFÍA
CREATE TABLE IF NOT EXISTS equipos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_inventario VARCHAR(50) UNIQUE NOT NULL,
    numero_serie VARCHAR(100) UNIQUE NOT NULL,
    tipo_equipo VARCHAR(80) NOT NULL,
    marca VARCHAR(60) NOT NULL,
    modelo VARCHAR(80) NOT NULL,
    estado_operativo VARCHAR(30) DEFAULT 'DISPONIBLE',
    frente_actual_id INTEGER,
    operador_actual_id INTEGER,
    FOREIGN KEY (frente_actual_id) REFERENCES frentes_obra(id),
    FOREIGN KEY (operador_actual_id) REFERENCES operadores(id)
);

-- 5. TABLA DE CERTIFICADOS DE CALIBRACIÓN
CREATE TABLE IF NOT EXISTS calibraciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    equipo_id INTEGER NOT NULL,
    numero_certificado VARCHAR(100) UNIQUE NOT NULL,
    empresa_certificadora VARCHAR(150) NOT NULL,
    fecha_emision DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    estado_calibracion VARCHAR(30) DEFAULT 'VIGENTE',
    FOREIGN KEY (equipo_id) REFERENCES equipos(id) ON DELETE CASCADE
);
