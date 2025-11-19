CREATE DATABASE IF NOT EXISTS techfix CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE techfix;

CREATE TABLE IF NOT EXISTS customers (
    id CHAR(7) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    cpf CHAR(14) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tickets (
    id CHAR(8) PRIMARY KEY,
    customer_id CHAR(7) NOT NULL,
    device_type VARCHAR(60) NOT NULL,
    device_model VARCHAR(60),
    description TEXT,
    status ENUM('TRIAGEM','EM_ANDAMENTO','FINALIZADO') DEFAULT 'TRIAGEM',
    entry_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE IF NOT EXISTS inventory_items (
    id VARCHAR(20) PRIMARY KEY,
    type VARCHAR(80) NOT NULL,
    brand VARCHAR(60) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    price DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_item_type_brand (type, brand)
);

CREATE TABLE IF NOT EXISTS ticket_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id CHAR(8) NOT NULL,
    previous_status ENUM('TRIAGEM','EM_ANDAMENTO','FINALIZADO'),
    new_status ENUM('TRIAGEM','EM_ANDAMENTO','FINALIZADO') NOT NULL,
    note VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id)
);

CREATE TABLE IF NOT EXISTS technicians (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    specialty VARCHAR(80),
    phone VARCHAR(20),
    status ENUM('ATIVO','INATIVO') DEFAULT 'ATIVO'
);

CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id CHAR(8) NOT NULL,
    technician_id BIGINT NOT NULL,
    scheduled_for DATETIME NOT NULL,
    status ENUM('AGENDADO','CONFIRMADO','CONCLUIDO','CANCELADO') DEFAULT 'AGENDADO',
    notes VARCHAR(255),
    CONSTRAINT fk_appointment_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id),
    CONSTRAINT fk_appointment_technician FOREIGN KEY (technician_id) REFERENCES technicians(id)
);


