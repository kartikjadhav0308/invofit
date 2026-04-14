package com.pro;

import java.sql.*;
import java.util.Scanner;

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

class CustomerSystem {

    Scanner sc = new Scanner(System.in);

    // ====== CALCULATE TOTAL COST ======
    private double calculateTotal(Connection con, String[] products, int[] qty) throws SQLException {
        double total = 0;

        String sql = "SELECT cost_price FROM product WHERE product_name = ?";

        PreparedStatement ps = con.prepareStatement(sql);

        for (int i = 0; i < products.length; i++) {
            ps.setString(1, products[i]);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                total += rs.getDouble(1) * qty[i];
            }
        }
        return total;
    }

    // ====== REDUCE INVENTORY ======
    private void updateInventory(Connection con, String[] products, int[] qty) throws SQLException {

        String sql = "UPDATE product SET quantity = quantity - ? WHERE product_name = ?";
        PreparedStatement ps = con.prepareStatement(sql);

        for (int i = 0; i < products.length; i++) {
            ps.setInt(1, qty[i]);
            ps.setString(2, products[i]);
            ps.executeUpdate();
        }
    }

    // ====== CUSTOMER INSERT ======
    public void addCustomer(Connection con) {
        try {

            System.out.println("Enter customer name:");
            String name = sc.nextLine();

            System.out.println("Enter mobile no:");
            int mobile = sc.nextInt();
            sc.nextLine();

            System.out.println("Enter product names (comma separated):");
            String productsInput = sc.nextLine();

            System.out.println("Enter quantities (comma separated):");
            String qtyInput = sc.nextLine();

            System.out.println("Enter amount paid by customer:");
            double userPay = sc.nextDouble();

            String[] products = productsInput.split(",");
            String[] qtyStr = qtyInput.split(",");

            int[] qty = new int[qtyStr.length];
            for (int i = 0; i < qtyStr.length; i++) {
                qty[i] = Integer.parseInt(qtyStr[i].trim());
            }

            double actualCost = calculateTotal(con, products, qty);
            double profit = userPay - actualCost;
            double pending = actualCost - userPay;

            // 1. Insert customer
            String insert = "INSERT INTO customer(customer_name, product_name, quantity, mobile_no, purchaseDate) VALUES (?,?,?,?,NOW())";
            PreparedStatement ps = con.prepareStatement(insert);

            ps.setString(1, name);
            ps.setString(2, productsInput);
            ps.setString(3, qtyInput);
            ps.setInt(4, mobile);
            ps.executeUpdate();

            // 2. Update inventory
            updateInventory(con, products, qty);

            // 3. Profit table insert
            String profitSql = "INSERT INTO profit(Date_, profit) VALUES (NOW(), ?)";
            PreparedStatement pp = con.prepareStatement(profitSql);
            pp.setDouble(1, profit);
            pp.executeUpdate();

            // 4. Paylater (if pending exists)
            if (pending > 0) {
                String paylaterSql = "INSERT INTO paylater(customer_name, amount_paid, pending_amount, quantity) VALUES (?,?,?,?)";
                PreparedStatement pl = con.prepareStatement(paylaterSql);

                pl.setString(1, name);
                pl.setDouble(2, userPay);
                pl.setDouble(3, pending);
                pl.setInt(4, qty.length);

                pl.executeUpdate();
            }

            System.out.println("Customer added successfully!");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ====== VIEW CUSTOMER ======
    public void viewCustomer(Connection con) {
        try {
            String sql = "SELECT * FROM customer";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                System.out.println(
                        rs.getString(1) + " | " +
                                rs.getString(2) + " | " +
                                rs.getString(3) + " | " +
                                rs.getInt(4) + " | " +
                                rs.getTimestamp(5)
                );
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ====== DELETE CUSTOMER ======
    public void deleteCustomer(Connection con) {
        try {
            System.out.println("Enter customer name:");
            String name = sc.nextLine();

            String sql = "DELETE FROM customer WHERE customer_name = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.executeUpdate();

            System.out.println("Customer deleted");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ====== PAYLATER VIEW ======
    public void viewPaylater(Connection con) {
        try {
            String sql = "SELECT * FROM paylater";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                System.out.println(
                        rs.getString(1) + " | " +
                                rs.getDouble(2) + " | " +
                                rs.getDouble(3) + " | " +
                                rs.getInt(4)
                );
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ====== UPDATE PAYLATER ======
    public void updatePaylater(Connection con) {
        try {
            System.out.println("Enter customer name:");
            String name = sc.nextLine();

            System.out.println("Enter new paid amount:");
            double paid = sc.nextDouble();

            String sql = "UPDATE paylater SET amount_paid = amount_paid + ?, pending_amount = pending_amount - ? WHERE customer_name = ?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setDouble(1, paid);
            ps.setDouble(2, paid);
            ps.setString(3, name);

            ps.executeUpdate();

            System.out.println("Paylater updated!");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ====== PROFIT TOTAL (AGGREGATE FUNCTION) ======
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

// =============Inventary===========
class Inventory {

    public void invo(Connection con) {

        try {
            Scanner sc = new Scanner(System.in);
            System.out.println("Product Table operation");
            mainloop:
        while(true){
            System.out.println("1.insert 2.update 3.delete 4.view 5.exit");

            byte choice = sc.nextByte();
            sc.nextLine();
            switch (choice){
                case 1->{
                    System.out.println("Enter product name");
                    String name = sc.nextLine();
                    System.out.println("Enter quantity of product");
                    int quantity =sc.nextInt();
                    System.out.println("Enter the price of product");
                    double cost = sc.nextDouble();

                    String query ="insert into product(product_name,cost_price,quantity) values (?,?,?)";
                    PreparedStatement p = con.prepareStatement(query);
                    p.setString(1,name);
                    p.setDouble(2,cost);
                    p.setInt(3,quantity);
                    p.executeUpdate();
                    System.out.println("Item add successfully !");

                }

                case 2->{
                    System.out.println("Enter 1.update cost 2.update quantity");
                    byte ch = sc.nextByte();
                    if(ch == 1){
                        sc.nextLine();
                        System.out.println("Enter the name of product");
                        String name = sc.nextLine();
                        System.out.println("Enter new cost:");
                        double cost = sc.nextDouble();
                        

                        String updateCost = "UPDATE product SET cost_price = ? WHERE product_name = ?";
                        PreparedStatement ps= con.prepareStatement(updateCost);
                        ps.setDouble(1, cost);
                        ps.setString(2, name);
                        ps.executeUpdate();
                        System.out.println("Cost updated successfully!");

                    } else if (ch == 2) {
                        sc.nextLine();
                        System.out.println("Enter the name of product");
                        String name = sc.nextLine();
                        System.out.println("Enter quantity to add:");
                        int qty = sc.nextInt();

                        String updateQty = "UPDATE product SET quantity = quantity + ? WHERE product_name = ?";
                        PreparedStatement ps = con.prepareStatement(updateQty);
                        ps.setInt(1, qty);
                        ps.setString(2, name);

                        ps.executeUpdate();
                        System.out.println("Quantity updated successfully!");
                    }

                }
                case 3->{
                    String query = "DELETE FROM product WHERE product_name = ?;";
                    System.out.println("Enter the name of the product");
                    String name = sc.nextLine();
                    PreparedStatement p = con.prepareStatement(query);
                    p.setString(1,name);
                    p.executeUpdate();
                    System.out.println("Item delete successfully");
                }
                case 4->{
                    String query = "Select * from product";
                    PreparedStatement ps = con.prepareStatement(query);
                    ResultSet rs = ps.executeQuery();
                    while(rs.next()){
                        System.out.println("product_id : "+rs.getInt(1)+" Product_name : "+rs.getString(2)+" Cost : "+rs.getDouble(3)+" quantity : "+rs.getInt(4));
                    }
                }
                case 5->{
                    System.out.println("Exit successfully");

                    sc.close();
                    break mainloop;
                }
                default -> {
                    System.out.println("Enter the invalid choise... Please enter the valid choice ");
                }
            }
        }
            } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        finally {
            try {
                con.close();
                System.out.println("\nconnection close");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

    }
}


public class App 
{
    public static void main( String[] args )
    {
        String url = "jdbc:mysql://localhost:3306/shopdb";
        String username = "root";
        String password = "password";

        Scanner sc = new Scanner(System.in);
        CustomerSystem cs = new CustomerSystem();
        DbConnection db = new DbConnection(url, username, password);
        Inventory inv = new Inventory();

        System.out.println("1. Product Menu\n2. Add Customer\n3. View Customer\n4. Delete Customer\n5. Paylater View\n6. Update Paylater\n7. Total Profit\n8. Exit");

        byte choice = sc.nextByte();

        switch(choice) {

            case 1 -> inv.invo(db.getConnection());
            case 2 -> cs.addCustomer(db.getConnection());
            case 3 -> cs.viewCustomer(db.getConnection());
            case 4 -> cs.deleteCustomer(db.getConnection());
            case 5 -> cs.viewPaylater(db.getConnection());
            case 6 -> cs.updatePaylater(db.getConnection());
            case 7 -> cs.totalProfit(db.getConnection());
            case 8 -> System.exit(0);
        }

    }
}
