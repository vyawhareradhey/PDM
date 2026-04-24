CREATE DATABASE IF NOT EXISTS pdm2;
USE pdm2;

-- Roles Table
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    full_name VARCHAR(100),
    email VARCHAR(100) NOT NULL, -- Email is now required for recovery
    employee_id VARCHAR(50),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Items Table (Master)
CREATE TABLE IF NOT EXISTS items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id VARCHAR(50) NOT NULL UNIQUE, -- The user facing ID (e.g. 000100)
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- Part, Document, Assembly
    description TEXT,
    owner_id INT NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Item Revisions Table
CREATE TABLE IF NOT EXISTS item_revisions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_pk INT NOT NULL,
    revision_id VARCHAR(10) NOT NULL, -- A, B, C...
    status VARCHAR(50) NOT NULL, -- In Work, Released, Obsolete
    checked_out_by INT DEFAULT NULL, -- NULL if checked in, else user_id
    file_name VARCHAR(255), -- Original filename
    storage_path VARCHAR(255), -- Path in vault
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    modified_by INT,
    file_mod_timestamp TIMESTAMP NULL,
    commit_message TEXT,
    FOREIGN KEY (item_pk) REFERENCES items(id),
    FOREIGN KEY (checked_out_by) REFERENCES users(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (modified_by) REFERENCES users(id),
    UNIQUE KEY unique_revision (item_pk, revision_id)
);

-- Product Structure (BOM) Table
-- Links a Parent Revision to a Child Item (Imprecise BOM)
CREATE TABLE IF NOT EXISTS product_structure (
    id INT AUTO_INCREMENT PRIMARY KEY,
    parent_rev_id INT NOT NULL, -- FK to item_revisions
    child_item_id INT NOT NULL, -- FK to items
    quantity INT DEFAULT 1,
    FOREIGN KEY (parent_rev_id) REFERENCES item_revisions(id),
    FOREIGN KEY (child_item_id) REFERENCES items(id)
);

-- Folders Table (Recursive)
CREATE TABLE IF NOT EXISTS folders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id INT DEFAULT NULL,
    owner_id INT NOT NULL,
    FOREIGN KEY (parent_id) REFERENCES folders(id),
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Folder Items (Content Link)
CREATE TABLE IF NOT EXISTS folder_items (
    folder_id INT NOT NULL,
    item_id INT NOT NULL,
    PRIMARY KEY (folder_id, item_id),
    FOREIGN KEY (folder_id) REFERENCES folders(id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);

-- Initial Seed Data
INSERT INTO roles (role_name) VALUES ('Admin'), ('Engineer'), ('Manager') ON DUPLICATE KEY UPDATE role_name=role_name;

-- Default Admin User (password: admin123)
-- Using ON DUPLICATE KEY UPDATE to avoid errors if run multiple times
INSERT INTO users (username, password_hash, role_id, full_name, email) 
SELECT 'admin', 'admin123', id, 'System Administrator', 'admin@example.com' 
FROM roles WHERE role_name = 'Admin' 
ON DUPLICATE KEY UPDATE password_hash='admin123';
