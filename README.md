# 📦 WMS -- Warehouse Management System

**Group 7 | SWP391 -- FPT University**

A full-stack **Warehouse Management System (WMS)** designed to
streamline inventory operations, including inbound, outbound, transfer,
and reporting processes.
This project is developed as part of the **SWP391 course** at FPT
University.

------------------------------------------------------------------------

## Team Members

  Name                  Role          GitHub
  --------------------- ------------- ----------------------------------------
  **Đỗ Minh Hải**       Team Leader   https://github.com/minhhai13
  **Nguyễn Thị Thắm**   Member        https://github.com/thamnguyen6508-max
  **Đặng Quang Vinh**   Member        https://github.com/vinhdk611
  **An Linh**           Member        https://github.com/anhlinhvybg-netizen

------------------------------------------------------------------------

## Key Features

### Master Data Management

-   Manage core entities: **Users, Warehouses, Bins, Products,Partners**
-   Ensure data consistency and scalability

### Inbound Process

-   Purchase Request (PR) management
-   Purchase Order (PO) processing
-   Goods Receipt Note (GRN) handling

### Outbound Process

-   Sales Order management
-   Stock issuing and outbound tracking

### Inventory Transfer

-   Support **1-step** and **2-step transfer workflows**
-   Transfer Order management

### Reporting & Analytics

-   Inbound / Outbound reports
-   Inventory balance tracking
-   Stock-availability reports

------------------------------------------------------------------------

## Tech Stack

### Backend

-   **Framework:** Spring MVC 6.2.x
-   **Persistence:** JDBC Template
-   **Security:** BCryptPasswordEncoder

### Database

-   **SQL Server**
-   Connection Pool: **HikariCP**

### Frontend

-   **Thymeleaf 3.1.x**

### Data Processing

-   **Jackson 2.18.x**

### Development Tools

-   **Java 21**
-   **Maven**
-   **Lombok**

------------------------------------------------------------------------

## ⚙️ Setup & Installation

### 1. Clone Repository

git clone https://github.com/minhhai13/Group7-WMS-OfficeSupplier.git

### 2. Configure Database

-   Open `JDBCConfig.java`
-   Update database credentials

### 3. Build Project

mvn clean install

### 4. Run Application

Deploy to **Apache Tomcat 10.x+**

Access: http://localhost:8080/

------------------------------------------------------------------------

## 📄 License

This project is developed for **educational purposes** under FPT
University.
