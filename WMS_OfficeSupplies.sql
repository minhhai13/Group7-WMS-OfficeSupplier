-- =============================================
-- WAREHOUSE MANAGEMENT SYSTEM - FINAL SCHEMA
-- SQL Server Database for Student Project
-- Total Tables: 18
-- =============================================
SELECT * from Users
USE master;
GO

IF DB_ID('WMS_OfficeSupplies') IS NOT NULL
    DROP DATABASE WMS_OfficeSupplies;
GO

CREATE DATABASE WMS_OfficeSupplies;
GO

USE WMS_OfficeSupplies;
GO

-- =============================================
-- 1. USERS TABLE
-- =============================================
CREATE TABLE Users (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    Username NVARCHAR(50) NOT NULL UNIQUE,
    PasswordHash NVARCHAR(255) NOT NULL,
    FullName NVARCHAR(100) NOT NULL,
    
    Role NVARCHAR(30) NOT NULL CHECK (Role IN (
        'System Admin', 'Warehouse Admin', 'Warehouse Manager', 
        'Purchasing Manager', 'Purchasing Staff', 
        'Sales Manager', 'Sales Staff', 'Storekeeper'
    )),
    
    WarehouseID INT NULL,
);

-- =============================================
-- 2. WAREHOUSES TABLE
-- =============================================
CREATE TABLE Warehouses (
    WarehouseID INT IDENTITY(1,1) PRIMARY KEY,
    WarehouseCode NVARCHAR(20) NOT NULL UNIQUE,
    WarehouseName NVARCHAR(100) NOT NULL,
);

ALTER TABLE Users
ADD CONSTRAINT FK_Users_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID);

-- =============================================
-- 3. BINS TABLE
-- =============================================
CREATE TABLE Bins (
    BinID INT IDENTITY(1,1) PRIMARY KEY,
    WarehouseID INT NOT NULL,
    BinLocation NVARCHAR(50) NOT NULL, 
    AssignedProductID INT NULL,  
    MaxCapacity INT,    
    
    CONSTRAINT CHK_BinFormat CHECK (BinLocation LIKE 'W[0-9][0-9]-Z[0-9][0-9]-S[0-9][0-9]-B[0-9][0-9]',
    CONSTRAINT FK_Bins_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
    CONSTRAINT UQ_Bins UNIQUE (WarehouseID, BinLocation)
);

-- =============================================
-- 4. PRODUCTS TABLE
-- =============================================
CREATE TABLE Products (
    ProductID INT IDENTITY(1,1) PRIMARY KEY,
    SKU NVARCHAR(50) NOT NULL UNIQUE,
    ProductName NVARCHAR(200) NOT NULL,
    
    -- Base UoM: Đơn vị nhỏ nhất để lưu stock
    BaseUoM NVARCHAR(10) NOT NULL DEFAULT 'EA',
    
    MinStockLevel INT DEFAULT 0,
);

ALTER TABLE Bins
ADD CONSTRAINT FK_Bins_Products FOREIGN KEY (AssignedProductID) REFERENCES Products(ProductID);

-- =============================================
-- 5. PRODUCT_UOM_CONVERSIONS TABLE (NEW!)
-- =============================================
CREATE TABLE ProductUoMConversions (
    ConversionID INT IDENTITY(1,1) PRIMARY KEY,
    ProductID INT NOT NULL,
    
    -- Conversion: FromUoM → ToUoM
    FromUoM NVARCHAR(10) NOT NULL,
    ToUoM NVARCHAR(10) NOT NULL,
    
    -- 1 FromUoM = ConversionFactor × ToUoM
    ConversionFactor INT NOT NULL,
        
    CONSTRAINT FK_UoMConv_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    CONSTRAINT UQ_UoMConv UNIQUE (ProductID, FromUoM, ToUoM),
    
    -- Validation: ConversionFactor must be > 0
    CONSTRAINT CHK_ConversionFactor CHECK (ConversionFactor > 0)
);

-- =============================================
-- 6. PARTNERS TABLE
-- =============================================
CREATE TABLE Partners (
    PartnerID INT IDENTITY(1,1) PRIMARY KEY,
    PartnerName NVARCHAR(200) NOT NULL,
    PartnerType NVARCHAR(20) NOT NULL CHECK (PartnerType IN ('Supplier', 'Customer')),
    ContactPerson NVARCHAR(100),
    PhoneNumber NVARCHAR(20),
);

-- =============================================
-- BỔ SUNG: PURCHASE_REQUESTS TABLE
-- =============================================
CREATE TABLE PurchaseRequests (
    PRID INT IDENTITY(1,1) PRIMARY KEY,
    PRNumber NVARCHAR(20) NOT NULL UNIQUE,
    WarehouseID INT NOT NULL,
    Status NVARCHAR(20) DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Approved', 'Rejected', 'Converted')),
    POID INT NULL,
    RelatedSOID INT NULL UNIQUE, -- Link tới SO gây ra việc thiếu hàng
    
    CONSTRAINT FK_PR_PurchaseOrders FOREIGN KEY (POID) REFERENCES PurchaseOrders(POID),
    CONSTRAINT FK_PR_SalesOrders FOREIGN KEY (RelatedSOID) REFERENCES SalesOrders(SOID),
    CONSTRAINT FK_PR_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
);

CREATE TABLE PurchaseRequestDetails (
    PRDetailID INT IDENTITY(1,1) PRIMARY KEY,
    PRID INT NOT NULL,
    ProductID INT NOT NULL,
    RequestedQty INT NOT NULL,
    UoM NVARCHAR(10) NOT NULL,

    CONSTRAINT CHK_RequestedQty CHECK (RequestedQty > 0),
    CONSTRAINT FK_PRDetail_PR FOREIGN KEY (PRID) REFERENCES PurchaseRequests(PRID),
    CONSTRAINT FK_PRDetail_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
);

-- =============================================
-- 7. PURCHASE_ORDERS TABLE
-- =============================================
CREATE TABLE PurchaseOrders (
    POID INT IDENTITY(1,1) PRIMARY KEY,
    PONumber NVARCHAR(20) NOT NULL UNIQUE,
    WarehouseID INT NOT NULL,
    SupplierID INT NOT NULL,
    POStatus NVARCHAR(20) DEFAULT 'Pending' CHECK (POStatus IN ('Pending', 'Approved', 'Rejected', 'Incomplete', 'Complete')),

    CONSTRAINT FK_PO_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
    CONSTRAINT FK_PO_Suppliers FOREIGN KEY (SupplierID) REFERENCES Partners(PartnerID)
);

CREATE TABLE PurchaseOrderDetails (
    PODetailID INT IDENTITY(1,1) PRIMARY KEY,
    POID INT NOT NULL,
    PRDetailID INT NULL REFERENCES PurchaseRequestDetails(PRDetailID),
    ProductID INT NOT NULL,
    OrderedQty INT NOT NULL,
    ReceivedQty INT DEFAULT 0,
    UoM NVARCHAR(10) NOT NULL,

    CONSTRAINT CHK_OrderedQty CHECK (OrderedQty > 0),
    CONSTRAINT CHK_ReceivedQty CHECK (ReceivedQty >= 0 AND ReceivedQty <= OrderedQty),
    CONSTRAINT FK_PODetail_PO FOREIGN KEY (POID) REFERENCES PurchaseOrders(POID),
    CONSTRAINT FK_PODetail_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
);

-- =============================================
-- 8. GOODS_RECEIPT_NOTES TABLE
-- =============================================
CREATE TABLE GoodsReceiptNotes (
    GRNID INT IDENTITY(1,1) PRIMARY KEY,
    GRNNumber NVARCHAR(20) NOT NULL UNIQUE,
    POID INT NOT NULL,
    WarehouseID INT NOT NULL,
    
    GRStatus NVARCHAR(20) DEFAULT 'Draft' CHECK (GRStatus IN ('Draft', 'Posted')),
    
    CONSTRAINT FK_GRN_PurchaseOrders FOREIGN KEY (POID) REFERENCES PurchaseOrders(POID),
    CONSTRAINT FK_GRN_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
);

-- =============================================
-- 9. GOODS_RECEIPT_DETAILS TABLE
-- =============================================
CREATE TABLE GoodsReceiptDetails (
    GRDetailID INT IDENTITY(1,1) PRIMARY KEY,
    GRNID INT NOT NULL,
    
    PODetailID INT NOT NULL,
    ProductID INT NOT NULL,
    ReceivedQty INT NOT NULL,
    UoM NVARCHAR(10) NOT NULL,  -- Giữ UoM gốc khi nhận
    
    BatchNumber NVARCHAR(50) NOT NULL,
    BinID INT NOT NULL,
        
    CONSTRAINT FK_GRDetail_GRN FOREIGN KEY (GRNID) REFERENCES GoodsReceiptNotes(GRNID),
    CONSTRAINT FK_GRDetail_PODetail FOREIGN KEY (PODetailID) REFERENCES PurchaseOrderDetails(PODetailID),
    CONSTRAINT FK_GRDetail_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    CONSTRAINT FK_GRDetail_Bins FOREIGN KEY (BinID) REFERENCES Bins(BinID)
);

-- =============================================
-- 10. SALES ORDERS (HEADER & DETAILS)
-- =============================================
CREATE TABLE SalesOrders (
    SOID INT IDENTITY(1,1) PRIMARY KEY,
    SONumber NVARCHAR(20) NOT NULL UNIQUE,
    WarehouseID INT NOT NULL,
    CustomerID INT NOT NULL,
    SOStatus NVARCHAR(30) DEFAULT 'Pending' CHECK (SOStatus IN ('Pending', 'Waiting for stock', 'Waiting for confirm', 'Approved', 'Incomplete', 'Complete', 'Cancelled')),
    CONSTRAINT FK_SO_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
    CONSTRAINT FK_SO_Customers FOREIGN KEY (CustomerID) REFERENCES Partners(PartnerID)
);

CREATE TABLE SalesOrderDetails (
    SODetailID INT IDENTITY(1,1) PRIMARY KEY,
    SOID INT NOT NULL,
    ProductID INT NOT NULL,
    OrderedQty INT NOT NULL,
    IssuedQty INT DEFAULT 0,
    UoM NVARCHAR(10) NOT NULL,
    CONSTRAINT FK_SODetail_SO FOREIGN KEY (SOID) REFERENCES SalesOrders(SOID),
    CONSTRAINT FK_SODetail_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
);


-- =============================================
-- 11. GOODS_ISSUE_NOTES TABLE
-- =============================================
CREATE TABLE GoodsIssueNotes (
    GINID INT IDENTITY(1,1) PRIMARY KEY,
    GINNumber NVARCHAR(20) NOT NULL UNIQUE,
    SOID INT NOT NULL,
    WarehouseID INT NOT NULL,
    GIStatus NVARCHAR(20) DEFAULT 'Draft' CHECK (GIStatus IN ('Draft', 'Posted')),
    
    CONSTRAINT FK_GIN_SalesOrders FOREIGN KEY (SOID) REFERENCES SalesOrders(SOID),
    CONSTRAINT FK_GIN_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
);

-- =============================================
-- 12. GOODS_ISSUE_DETAILS TABLE
-- =============================================
CREATE TABLE GoodsIssueDetails (
    GIDetailID INT IDENTITY(1,1) PRIMARY KEY,
    GINID INT NOT NULL,
    SODetailID INT NOT NULL,

    ProductID INT NOT NULL,
    IssuedQty INT NOT NULL,
    UoM NVARCHAR(10) NOT NULL,  -- Giữ UoM gốc khi xuất
    
    BatchNumber NVARCHAR(50) NOT NULL,
    BinID INT NOT NULL,
    
    CONSTRAINT FK_GIDetail_GIN FOREIGN KEY (GINID) REFERENCES GoodsIssueNotes(GINID),
    CONSTRAINT FK_GIDetail_SODetail FOREIGN KEY (SODetailID) REFERENCES SalesOrderDetails(SODetailID),
    CONSTRAINT FK_GIDetail_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    CONSTRAINT FK_GIDetail_Bins FOREIGN KEY (BinID) REFERENCES Bins(BinID)
);
-- =============================================
-- 13. TRANSFERS (HEADER & DETAILS)
-- =============================================
CREATE TABLE Transfers (
    TransferID INT IDENTITY(1,1) PRIMARY KEY,
    TransferNumber NVARCHAR(20) NOT NULL UNIQUE,
    SourceWarehouseID INT NOT NULL,
    DestinationWarehouseID INT NOT NULL,
    TransferStatus NVARCHAR(30) DEFAULT 'Pending' CHECK (TransferStatus IN ('Pending', 'Approved', 'In-Transit', 'Completed')),
    CONSTRAINT FK_Transfer_SourceWH FOREIGN KEY (SourceWarehouseID) REFERENCES Warehouses(WarehouseID),
    CONSTRAINT FK_Transfer_DestWH FOREIGN KEY (DestinationWarehouseID) REFERENCES Warehouses(WarehouseID)
);

CREATE TABLE TransferDetails (
    TDetailID INT IDENTITY(1,1) PRIMARY KEY,
    TransferID INT NOT NULL,
    ProductID INT NOT NULL,
    Quantity INT NOT NULL,
    UoM NVARCHAR(10) NOT NULL,
    FromBinID INT NULL,
    ToBinID INT NULL,
    BatchNumber NVARCHAR(50) NULL,

    CONSTRAINT FK_TDetail_ToBin FOREIGN KEY (ToBinID) REFERENCES Bins(BinID),
    CONSTRAINT FK_TDetail_FromBin FOREIGN KEY (FromBinID) REFERENCES Bins(BinID),
    CONSTRAINT FK_TDetail_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    CONSTRAINT FK_TDetail_Transfer FOREIGN KEY (TransferID) REFERENCES Transfers(TransferID)
);

-- =============================================
-- 14. STOCK_BATCHES TABLE
-- =============================================
CREATE TABLE StockBatches (
    StockBatchID INT IDENTITY(1,1) PRIMARY KEY,
    WarehouseID INT NOT NULL,
    ProductID INT NOT NULL,
    BinID INT NOT NULL,
    
    BatchNumber NVARCHAR(50) NOT NULL,
    ArrivalDateTime DATETIME2 NOT NULL,
    
    -- CRITICAL: Stock LUÔN lưu theo BaseUoM
    QtyAvailable INT NOT NULL DEFAULT 0,
    QtyReserved INT NOT NULL DEFAULT 0,
    QtyInTransit INT NOT NULL DEFAULT 0,
    
    UoM NVARCHAR(10) NOT NULL,  -- Phải = Products.BaseUoM
        
    CONSTRAINT FK_StockBatch_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
    CONSTRAINT FK_StockBatch_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    CONSTRAINT FK_StockBatch_Bins FOREIGN KEY (BinID) REFERENCES Bins(BinID),
    CONSTRAINT UQ_StockBatch UNIQUE (WarehouseID, ProductID, BinID, BatchNumber)
);

-- =============================================
-- 15. STOCK_MOVEMENTS TABLE
-- =============================================
-- =============================================
-- 15. STOCK_MOVEMENTS TABLE (OPTIMIZED)
-- =============================================
CREATE TABLE StockMovements (
    MovementID INT IDENTITY(1,1) PRIMARY KEY,
    
    -- Kho chính xảy ra biến động
    WarehouseID INT NOT NULL,
    ProductID INT NOT NULL,
    BinID INT NOT NULL,
    BatchNumber NVARCHAR(50) NULL,
    
    -- Phân loại hành động
    MovementType NVARCHAR(30) NOT NULL CHECK (MovementType IN (
        'Receipt',      -- Nhập hàng từ NCC (GRN)
        'Issue',        -- Xuất hàng cho khách (GIN)
        'Transfer-Out', -- Xuất đi kho khác (Step 1)
        'Transfer-In',  -- Nhập vào kho mới (Step 2)
        'Reserve',      -- Giữ hàng cho SO
        'Release'       -- Giải phóng hàng giữ (khi SO bị hủy hoặc đã xuất đi)
    )),
    MovementDate DATETIME2 DEFAULT GETDATE(),
    -- QUAN TRỌNG: Phân loại loại tồn kho bị ảnh hưởng
    -- Giúp báo cáo tách biệt được: Tồn thực tế (Physical) và Tồn ảo (Reserved)
    StockType NVARCHAR(20) NOT NULL CHECK (StockType IN ('Physical', 'Reserved', 'In-Transit')),
    -- Số dư sau biến động (Dùng để kiểm tra tính toàn vẹn dữ liệu)
    BalanceAfter INT NOT NULL,

    -- Số lượng: Luôn lưu số dương. Logic cộng/trừ sẽ dựa vào MovementType
    Quantity INT NOT NULL,
    UoM NVARCHAR(10) NOT NULL, 

    CONSTRAINT FK_Movement_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
    CONSTRAINT FK_Movement_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    CONSTRAINT FK_Movement_Bins FOREIGN KEY (BinID) REFERENCES Bins(BinID),
);

