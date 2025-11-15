package com.city.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CityManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(CityManagementApplication.class, args);
        System.out.println("\n===========================================");
        System.out.println("Sustainable City Management Backend Started");
        System.out.println("===========================================");
        System.out.println("Server running at: http://localhost:8080");
        System.out.println("API Base URL: http://localhost:8080/api");
        System.out.println("\nTest Users:");
        System.out.println("  Admin: admin / admin123");
        System.out.println("  Manager: manager / manager123");
        System.out.println("  Provider Admin: provider_admin / provider123");
        System.out.println("  Provider User: provider_user / user123");
        System.out.println("===========================================\n");
    }
}
