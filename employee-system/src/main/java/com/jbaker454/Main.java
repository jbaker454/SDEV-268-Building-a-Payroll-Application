package com.jbaker454;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main {
    public static void main(String[] args) {

        System.out.println("Employee System Starting...");

        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:company.db");
            System.out.println("Database connected successfully!");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}