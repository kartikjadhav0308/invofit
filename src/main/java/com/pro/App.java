package com.pro;

import java.sql.*;
import java.util.Scanner;

// ================= DB CONNECTION =================
class DbConnection {
    private Connection con;

    public DbConnection(String url, String username, String password) {
        try {
            con = DriverManager.getConnection(url, username, password);
            System.out.println("Connected to database!\n");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Connection getConnection() {
        return con;
    }
}

// ================= CUSTOMER SYSTEM =================
class CustomerSystem {

    Scanner sc;

    public CustomerSystem(Scanner sc) {
        this.sc = sc;
    }

    private double calculateTotal(Connection con, String[] products, int[] qty) throws SQLException {
        double total = 0;

        String sql = "SELECT cost_price FROM product WHERE product_name = ?";
        PreparedStatement ps = con.prepareStatement(sql);

        for (int i = 0; i < products.length; i++) {
            ps.setString(1, products[i].trim());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                total += rs.getDouble(1) * qty[i];
            }
        }
        return total;
    }

    private void updateInventory(Connection con, String[] products, int[] qty) throws SQLException {
        String sql = "UPDATE product SET quantity = quantity - ? WHERE product_name = ?";
        PreparedStatement ps = con.prepareStatement(sql);

        for (int i = 0; i < products.length; i++) {
            ps.setInt(1, qty[i]);
            ps.setString(2, products[i].trim());
            ps.executeUpdate();
        }
    }

    // ================= ADD CUSTOMER =================
    public void addCustomer(Connection con) {
        try {
            System.out.println("\nEnter customer name:");
            String name = sc.nextLine();

            System.out.println("Enter mobile no:");
            String mobile = sc.nextLine();

            System.out.println("Enter product names (comma separated):");
            String productsInput = sc.nextLine();

            System.out.println("Enter quantities (comma separated):");
            String qtyInput = sc.nextLine();

            System.out.println("Enter amount paid:");
            double userPay = Double.parseDouble(sc.nextLine().trim());

            String[] products = productsInput.split(",");
            String[] qtyStr = qtyInput.split(",");

            int[] qty = new int[qtyStr.length];
            for (int i = 0; i < qtyStr.length; i++) {
                qty[i] = Integer.parseInt(qtyStr[i].trim());
            }

            double actualCost = calculateTotal(con, products, qty);
            double profit = userPay - actualCost;

            String insert = "INSERT INTO customer(customer_name, product_name, quantity, mobile_no, purchaseDate) VALUES (?,?,?,?,NOW())";
            PreparedStatement ps = con.prepareStatement(insert);

            ps.setString(1, name);
            ps.setString(2, productsInput);
            ps.setString(3, qtyInput);
            ps.setString(4, mobile);
            ps.executeUpdate();

            updateInventory(con, products, qty);

            String profitSql = "INSERT INTO profit(Date_, profit) VALUES (NOW(), ?)";
            PreparedStatement pp = con.prepareStatement(profitSql);
            pp.setDouble(1, profit);
            pp.executeUpdate();

            System.out.println("Customer added successfully!");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ================= PAYLATER =================
    public void addPaylater(Connection con) {
        try {
            System.out.println("Enter customer name:");
            String name = sc.nextLine();

            System.out.println("Enter amount paid:");
            double paid = Double.parseDouble(sc.nextLine().trim());

            System.out.println("Enter pending amount:");
            double pending = Double.parseDouble(sc.nextLine().trim());

            String sql = "INSERT INTO paylater(customer_name, amount_paid, pending_amount) VALUES (?,?,?)";
            PreparedStatement pl = con.prepareStatement(sql);

            pl.setString(1, name);
            pl.setDouble(2, paid);
            pl.setDouble(3, pending);

            pl.executeUpdate();
            System.out.println("Paylater added!");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void viewCustomer(Connection con) {
        try {
            String sql = "SELECT * FROM customer";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getString(1) + " | " +
                        rs.getString(2) + " | " +
                        rs.getString(3) + " | " +
                        rs.getString(4) + " | " +
                        rs.getTimestamp(5));
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteCustomer(Connection con) {
        try {
            System.out.println("Enter customer name:");
            String name = sc.nextLine();

            String sql = "DELETE FROM customer WHERE customer_name = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.executeUpdate();

            System.out.println("Deleted successfully");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void viewPaylater(Connection con) {
        try {
            String sql = "SELECT * FROM paylater";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getString(1) + " | " +
                        rs.getDouble(2) + " | " +
                        rs.getDouble(3));
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void updatePaylater(Connection con) {
        try {
            System.out.println("Enter customer name:");
            String name = sc.nextLine();

            System.out.println("Enter paid amount:");
            double paid = Double.parseDouble(sc.nextLine().trim());

            String sql = "UPDATE paylater SET amount_paid = amount_paid + ?, pending_amount = pending_amount - ? WHERE customer_name = ?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setDouble(1, paid);
            ps.setDouble(2, paid);
            ps.setString(3, name);

            ps.executeUpdate();
            System.out.println("Updated!");

            String deleteSql = "DELETE FROM paylater WHERE customer_name = ? AND pending_amount <= 0";
            PreparedStatement dp = con.prepareStatement(deleteSql);
            dp.setString(1, name);
            dp.executeUpdate();

            System.out.println("Updated! (Auto-removed if fully paid)");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void totalProfit(Connection con) {
        try {
            String sql = "SELECT SUM(profit) FROM profit";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("Total Profit = " + rs.getDouble(1));
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

// ================= INVENTORY =================
class Inventory {

    Scanner sc;

    public Inventory(Scanner sc) {
        this.sc = sc;
    }

    public void invo(Connection con) {
        try {
            while (true) {
                System.out.println("\n1.Insert 2.Update 3.Delete 4.View 5.Exit");

                int choice = Integer.parseInt(sc.nextLine().trim());

                switch (choice) {

                    case 1 -> {
                        System.out.println("Enter product name:");
                        String name = sc.nextLine();

                        System.out.println("Enter quantity:");
                        int qty = Integer.parseInt(sc.nextLine());

                        System.out.println("Enter cost:");
                        double cost = Double.parseDouble(sc.nextLine());

                        String sql = "INSERT INTO product(product_name,cost_price,quantity) VALUES (?,?,?)";
                        PreparedStatement ps = con.prepareStatement(sql);
                        ps.setString(1, name);
                        ps.setDouble(2, cost);
                        ps.setInt(3, qty);
                        ps.executeUpdate();

                        System.out.println("Inserted!");
                    }

                    case 2 -> {
                        System.out.println("1.Update cost 2.Update qty");
                        int ch = Integer.parseInt(sc.nextLine());

                        if (ch == 1) {
                            System.out.println("Product name:");
                            String name = sc.nextLine();

                            System.out.println("New cost:");
                            double cost = Double.parseDouble(sc.nextLine());

                            String sql = "UPDATE product SET cost_price=? WHERE product_name=?";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setDouble(1, cost);
                            ps.setString(2, name);
                            ps.executeUpdate();

                            System.out.println("Updated cost!");
                        } else {
                            System.out.println("Product name:");
                            String name = sc.nextLine();

                            System.out.println("Qty to add:");
                            int qty = Integer.parseInt(sc.nextLine());

                            String sql = "UPDATE product SET quantity = quantity + ? WHERE product_name=?";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setInt(1, qty);
                            ps.setString(2, name);
                            ps.executeUpdate();

                            System.out.println("Updated qty!");
                        }
                    }

                    case 3 -> {
                        System.out.println("Enter product name:");
                        String name = sc.nextLine();

                        String sql = "DELETE FROM product WHERE product_name=?";
                        PreparedStatement ps = con.prepareStatement(sql);
                        ps.setString(1, name);
                        ps.executeUpdate();

                        System.out.println("Deleted!");
                    }

                    case 4 -> {
                        String sql = "SELECT * FROM product";
                        PreparedStatement ps = con.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery();

                        while (rs.next()) {
                            System.out.println(rs.getInt(1) + " | " +
                                    rs.getString(2) + " | " +
                                    rs.getDouble(3) + " | " +
                                    rs.getInt(4));
                        }
                    }

                    case 5 -> {
                        System.out.println("Exiting inventory...");
                        return;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

// ================= MAIN =================
public class App {
    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/shopdb";
        String username = "root";
        String password = "password";

        Scanner sc = new Scanner(System.in);

        DbConnection db = new DbConnection(url, username, password);
        CustomerSystem cs = new CustomerSystem(sc);
        Inventory inv = new Inventory(sc);

        while (true) {
            System.out.println("\n1.Product Menu 2.Add Customer 3.View Customer 4.Delete customer 5.Paylater View 6.Update Paylater 7.Profit 8.Add Paylater 9.Exit");

            int choice = Integer.parseInt(sc.nextLine().trim());

            switch (choice) {
                case 1 -> inv.invo(db.getConnection());
                case 2 -> cs.addCustomer(db.getConnection());
                case 3 -> cs.viewCustomer(db.getConnection());
                case 4 -> cs.deleteCustomer(db.getConnection());
                case 5 -> cs.viewPaylater(db.getConnection());
                case 6 -> cs.updatePaylater(db.getConnection());
                case 7 -> cs.totalProfit(db.getConnection());
                case 8 -> cs.addPaylater(db.getConnection());
                case 9 -> {
                    System.out.println("Exited!");
                    System.exit(0);
                }
            }
        }
    }
}