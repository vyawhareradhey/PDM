package com.pdm.core;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private Role role;
    private String fullName;
    private String email;
    private String employeeId;

    public User(int id, String username, String passwordHash, Role role, String fullName, String email, String employeeId) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.employeeId = employeeId;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getEmployeeId() { return employeeId; }
    
    @Override
    public String toString() {
        return username + " (" + role.getName() + ")"; 
    }
}
