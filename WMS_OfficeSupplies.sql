-- =============================================
-- WAREHOUSE MANAGEMENT SYSTEM - FINAL SCHEMA
-- SQL Server Database for Student Project
-- Total Tables: 18
-- =============================================

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
    Email NVARCHAR(100) NOT NULL UNIQUE,
    PhoneNumber NVARCHAR(20),
    
    Role NVARCHAR(30) NOT NULL CHECK (Role IN (
        'System Admin', 'Warehouse Admin', 'Warehouse Manager', 
        'Purchasing Manager', 'Purchasing Staff', 
        'Sales Manager', 'Sales Staff', 'Storekeeper'
    )),
    
    WarehouseID INT NULL,
    
    IsActive BIT DEFAULT 1,
    IsFirstLogin BIT DEFAULT 1,
    CreatedAt DATETIME2 DEFAULT GETDATE(),
    UpdatedAt DATETIME2 DEFAULT GETDATE()
);

-- =============================================
-- 2. WAREHOUSES TABLE
-- =============================================
CREATE TABLE Warehouses (
    WarehouseID INT IDENTITY(1,1) PRIMARY KEY,
    WarehouseCode NVARCHAR(20) NOT NULL UNIQUE,
    WarehouseName NVARCHAR(100) NOT NULL,
    Address NVARCHAR(255),
    City NVARCHAR(50),
    IsActive BIT DEFAULT 1,
    CreatedAt DATETIME2 DEFAULT GETDATE()
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
    MaxCapacity DECIMAL(18, 2),    
    CurrentQuantity DECIMAL(18, 2) DEFAULT 0,
    IsActive BIT DEFAULT 1,
    CreatedAt DATETIME2 DEFAULT GETDATE(),
    
    CONSTRAINT CHK_BinFormat CHECK (BinLocation LIKE '[0-9][0-9]-[A-Z]-[0-9][0-9]-[0-9][0-9]'),
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
    Description NVARCHAR(500),
    Category NVARCHAR(100),
    
    -- Base UoM: Đơn vị nhỏ nhất để lưu stock
    BaseUoM NVARCHAR(10) NOT NULL DEFAULT 'EA',
    
    MinStockLevel INT DEFAULT 0,
    IsActive BIT DEFAULT 1,
    CreatedAt DATETIME2 DEFAULT GETDATE()
);

ALTER TABLE Bins
ADD CONSTRAINT FK_Bins_Products FOREIGN KEY (CurrentProductID) REFERENCES Products(ProductID);

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
    
    IsActive BIT DEFAULT 1,
    CreatedAt DATETIME2 DEFAULT GETDATE(),
    
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
    PartnerCode NVARCHAR(20) NOT NULL UNIQUE,
    PartnerName NVARCHAR(200) NOT NULL,
    PartnerType NVARCHAR(20) NOT NULL CHECK (PartnerType IN ('Supplier', 'Customer', 'Both')),
    ContactPerson NVARCHAR(100),
    Email NVARCHAR(100),
    PhoneNumber NVARCHAR(20),
    IsActive BIT DEFAULT 1,
    CreatedAt DATETIME2 DEFAULT GETDATE()
);

-- =============================================
-- 7. PURCHASE_ORDERS TABLE
-- =============================================
CREATE TABLE PurchaseOrders (
    POID INT IDENTITY(1,1) PRIMARY KEY,
    PONumber NVARCHAR(20) NOT NULL UNIQUE,
    PRID INT NULL,
    WarehouseID INT NOT NULL,
    SupplierID INT NOT NULL,
    POStatus NVARCHAR(20) DEFAULT 'Pending' CHECK (POStatus IN ('Pending', 'Approved', 'Rejected', 'Incomplete', 'Complete')),
    OrderDate DATETIME2 DEFAULT GETDATE(),
    CreatedBy INT NOT NULL,
    ApprovedBy INT NULL,
    ApprovalDate DATETIME2 NULL,
    RelatedSONumber NVARCHAR(20) NULL,
    CreatedAt DATETIME2 DEFAULT GETDATE(),
    UpdatedAt DATETIME2 DEFAULT GETDATE(),
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
    CONSTRAINT FK_PODetail_PO FOREIGN KEY (POID) REFERENCES PurchaseOrders(POID),
    CONSTRAINT FK_PODetail_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
);

-- =============================================
-- BỔ SUNG: PURCHASE_REQUESTS TABLE
-- =============================================
CREATE TABLE PurchaseRequests (
    PRID INT IDENTITY(1,1) PRIMARY KEY,
    PRNumber NVARCHAR(20) NOT NULL UNIQUE,
    WarehouseID INT NOT NULL,
    CreatedBy INT NOT NULL, -- Thường là Sales Staff
    Status NVARCHAR(20) DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Approved', 'Rejected', 'Converted')),
    RelatedSONumber NVARCHAR(20) NULL, -- Link tới SO gây ra việc thiếu hàng
    Notes NVARCHAR(500),
    CreatedAt DATETIME2 DEFAULT GETDATE(),
    UpdatedAt DATETIME2 DEFAULT GETDATE(),
    
    CONSTRAINT FK_PR_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
    CONSTRAINT FK_PR_CreatedBy FOREIGN KEY (CreatedBy) REFERENCES Users(UserID)
);

CREATE TABLE PurchaseRequestDetails (
    PRDetailID INT IDENTITY(1,1) PRIMARY KEY,
    PRID INT NOT NULL,
    ProductID INT NOT NULL,
    RequestedQty INT NOT NULL,
    UoM NVARCHAR(10) NOT NULL,
    CONSTRAINT FK_PRDetail_PR FOREIGN KEY (PRID) REFERENCES PurchaseRequests(PRID),
    CONSTRAINT FK_PRDetail_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
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
    ReceiptDate DATETIME2 DEFAULT GETDATE(),
    ReceivedBy INT NOT NULL,
    PostedAt DATETIME2 NULL,
    
    Notes NVARCHAR(500),
    CreatedAt DATETIME2 DEFAULT GETDATE(),
    UpdatedAt DATETIME2 DEFAULT GETDATE(),
    
    CONSTRAINT FK_GRN_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
    CONSTRAINT FK_GRN_ReceivedBy FOREIGN KEY (ReceivedBy) REFERENCES Users(UserID)
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
    ArrivalDateTime DATETIME2 DEFAULT GETDATE(),
    BinID INT NOT NULL,
    
    Notes NVARCHAR(255),
    
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
    OrderDate DATETIME2 DEFAULT GETDATE(),
    CreatedBy INT NOT NULL,
    ApprovedBy INT NULL,
    ApprovalDate DATETIME2 NULL,
    CreatedAt DATETIME2 DEFAULT GETDATE(),
    UpdatedAt DATETIME2 DEFAULT GETDATE(),
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
    IssueDate DATETIME2 DEFAULT GETDATE(),
    IssuedBy INT NOT NULL,
    PostedAt DATETIME2 NULL,
    
    Notes NVARCHAR(500),
    CreatedAt DATETIME2 DEFAULT GETDATE(),
    UpdatedAt DATETIME2 DEFAULT GETDATE(),
    
    CONSTRAINT FK_GIN_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
    CONSTRAINT FK_GIN_IssuedBy FOREIGN KEY (IssuedBy) REFERENCES Users(UserID)
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
    
    Notes NVARCHAR(255),
    
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
    TransferStatus NVARCHAR(30) DEFAULT 'Pending' CHECK (TransferStatus IN ('Pending', 'Approved', 'In-Transit', 'Completed', 'Cancelled')),
    RequestedBy INT NOT NULL,
    ShippedBy INT NULL,
    ReceivedBy INT NULL,
    ShippedAt DATETIME2 NULL,
    ReceivedAt DATETIME2 NULL,
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
    
    SourceType NVARCHAR(20) CHECK (SourceType IN ('Purchase', 'Transfer')),
    SourceDocNumber NVARCHAR(20),
    
    CreatedAt DATETIME2 DEFAULT GETDATE(),
    UpdatedAt DATETIME2 DEFAULT GETDATE(),
    
    CONSTRAINT FK_StockBatch_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
    CONSTRAINT FK_StockBatch_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    CONSTRAINT FK_StockBatch_Bins FOREIGN KEY (BinID) REFERENCES Bins(BinID),
    CONSTRAINT UQ_StockBatch UNIQUE (WarehouseID, ProductID, BinID, BatchNumber)
);

-- =============================================
-- 15. STOCK_MOVEMENTS TABLE
-- =============================================
CREATE TABLE StockMovements (
    MovementID INT IDENTITY(1,1) PRIMARY KEY,
    
    WarehouseID INT NOT NULL,
    ProductID INT NOT NULL,
    BinID INT NULL,
    BatchNumber NVARCHAR(50) NULL,
    
    MovementType NVARCHAR(30) NOT NULL CHECK (MovementType IN (
        'Receipt', 'Issue', 'Transfer-In', 'Transfer-Out', 'Reservation', 'Release'
    )),
    MovementDate DATETIME2 DEFAULT GETDATE(),
    
    Quantity INT NOT NULL,
    UoM NVARCHAR(10) NOT NULL,  -- Lưu theo BaseUoM
    
    DocumentType NVARCHAR(20),
    DocumentNumber NVARCHAR(20),
    
    PerformedBy INT NOT NULL,
    BalanceAfter INT NOT NULL,
    
    Notes NVARCHAR(500),
    CreatedAt DATETIME2 DEFAULT GETDATE(),
    
    CONSTRAINT FK_Movement_Warehouses FOREIGN KEY (WarehouseID) REFERENCES Warehouses(WarehouseID),
    CONSTRAINT FK_Movement_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    CONSTRAINT FK_Movement_Bins FOREIGN KEY (BinID) REFERENCES Bins(BinID),
    CONSTRAINT FK_Movement_PerformedBy FOREIGN KEY (PerformedBy) REFERENCES Users(UserID)
);

-- =============================================
-- INDEXES FOR PERFORMANCE
-- =============================================

-- Users
CREATE INDEX IX_Users_Username ON Users(Username);
CREATE INDEX IX_Users_Role ON Users(Role);
CREATE INDEX IX_Users_WarehouseID ON Users(WarehouseID);

-- Bins
CREATE INDEX IX_Bins_WarehouseID ON Bins(WarehouseID);
CREATE INDEX IX_Bins_BinLocation ON Bins(BinLocation);
CREATE INDEX IX_Bins_CurrentProductID ON Bins(CurrentProductID);

-- Products
CREATE INDEX IX_Products_SKU ON Products(SKU);
CREATE INDEX IX_Products_Category ON Products(Category);

-- ProductUoMConversions (NEW!)
CREATE INDEX IX_UoMConv_ProductID ON ProductUoMConversions(ProductID);
CREATE INDEX IX_UoMConv_FromUoM ON ProductUoMConversions(FromUoM);
CREATE INDEX IX_UoMConv_ToUoM ON ProductUoMConversions(ToUoM);

-- Partners
CREATE INDEX IX_Partners_PartnerType ON Partners(PartnerType);

-- Purchase Orders
CREATE INDEX IX_PO_PONumber ON PurchaseOrders(PONumber);
CREATE INDEX IX_PO_WarehouseID ON PurchaseOrders(WarehouseID);
CREATE INDEX IX_PO_POStatus ON PurchaseOrders(POStatus);
CREATE INDEX IX_PO_ProductID ON PurchaseOrders(ProductID);

-- Goods Receipt Notes
CREATE INDEX IX_GRN_GRNNumber ON GoodsReceiptNotes(GRNNumber);
CREATE INDEX IX_GRN_PONumber ON GoodsReceiptNotes(PONumber);
CREATE INDEX IX_GRN_Status ON GoodsReceiptNotes(GRStatus);
CREATE INDEX IX_GRN_WarehouseID ON GoodsReceiptNotes(WarehouseID);

-- Goods Receipt Details
CREATE INDEX IX_GRDetail_GRNID ON GoodsReceiptDetails(GRNID);
CREATE INDEX IX_GRDetail_ProductID ON GoodsReceiptDetails(ProductID);
CREATE INDEX IX_GRDetail_BatchNumber ON GoodsReceiptDetails(BatchNumber);
CREATE INDEX IX_GRDetail_ArrivalDateTime ON GoodsReceiptDetails(ArrivalDateTime);
CREATE INDEX IX_GRDetail_BinID ON GoodsReceiptDetails(BinID);

-- Sales Orders
CREATE INDEX IX_SO_SONumber ON SalesOrders(SONumber);
CREATE INDEX IX_SO_WarehouseID ON SalesOrders(WarehouseID);
CREATE INDEX IX_SO_SOStatus ON SalesOrders(SOStatus);
CREATE INDEX IX_SO_ProductID ON SalesOrders(ProductID);
CREATE INDEX IX_SO_ConfirmationDeadline ON SalesOrders(ConfirmationDeadline);

-- Goods Issue Notes
CREATE INDEX IX_GIN_GINNumber ON GoodsIssueNotes(GINNumber);
CREATE INDEX IX_GIN_SONumber ON GoodsIssueNotes(SONumber);
CREATE INDEX IX_GIN_Status ON GoodsIssueNotes(GIStatus);
CREATE INDEX IX_GIN_WarehouseID ON GoodsIssueNotes(WarehouseID);

-- Goods Issue Details
CREATE INDEX IX_GIDetail_GINID ON GoodsIssueDetails(GINID);
CREATE INDEX IX_GIDetail_ProductID ON GoodsIssueDetails(ProductID);
CREATE INDEX IX_GIDetail_BatchNumber ON GoodsIssueDetails(BatchNumber);
CREATE INDEX IX_GIDetail_BinID ON GoodsIssueDetails(BinID);

-- Transfers
CREATE INDEX IX_Transfer_TransferNumber ON Transfers(TransferNumber);
CREATE INDEX IX_Transfer_Type ON Transfers(TransferType);
CREATE INDEX IX_Transfer_Status ON Transfers(TransferStatus);
CREATE INDEX IX_Transfer_SourceWH ON Transfers(SourceWarehouseID);
CREATE INDEX IX_Transfer_DestWH ON Transfers(DestinationWarehouseID);

-- Stock Batches
CREATE INDEX IX_StockBatch_WarehouseID_ProductID ON StockBatches(WarehouseID, ProductID);
CREATE INDEX IX_StockBatch_BinID ON StockBatches(BinID);
CREATE INDEX IX_StockBatch_BatchNumber ON StockBatches(BatchNumber);
CREATE INDEX IX_StockBatch_ArrivalDateTime ON StockBatches(ArrivalDateTime);

-- Stock Movements
CREATE INDEX IX_Movement_WarehouseID_ProductID ON StockMovements(WarehouseID, ProductID);
CREATE INDEX IX_Movement_MovementType ON StockMovements(MovementType);
CREATE INDEX IX_Movement_MovementDate ON StockMovements(MovementDate);
CREATE INDEX IX_Movement_DocumentNumber ON StockMovements(DocumentNumber);

-- =============================================
-- SEED DATA
-- =============================================

-- Sample Warehouses
INSERT INTO Warehouses (WarehouseCode, WarehouseName, Address, City) VALUES
('WH01', 'Main Warehouse - Hanoi', '123 Tran Hung Dao', 'Hanoi'),
('WH02', 'Branch Warehouse - HCMC', '456 Nguyen Hue', 'Ho Chi Minh');

-- Sample Admin Users
INSERT INTO Users (Username, PasswordHash, FullName, Email, Role, WarehouseID, IsFirstLogin) VALUES
('admin', 'hashed_password_here', 'System Administrator', 'admin@wms.com', 'System Admin', NULL, 0),
('whadmin', 'hashed_password_here', 'Warehouse Administrator', 'whadmin@wms.com', 'Warehouse Admin', NULL, 0);

-- Sample Products with BaseUoM
INSERT INTO Products (SKU, ProductName, Description, Category, BaseUoM, MinStockLevel) VALUES
('PEN-BLK-001', 'Black Ballpoint Pen', 'Standard black ink ballpoint pen', 'Pens', 'EA', 100),
('PAPER-A4-001', 'A4 Copy Paper 80gsm', 'White A4 size copy paper', 'Paper', 'SHEET', 5000),
('STAPLER-001', 'Office Stapler', 'Standard office stapler', 'Stationery', 'EA', 20);

-- Sample UoM Conversions
INSERT INTO ProductUoMConversions (ProductID, FromUoM, ToUoM, ConversionFactor) VALUES
-- Pen conversions
(1, 'BOX', 'EA', 12),           -- 1 BOX = 12 pens
(1, 'CARTON', 'EA', 1200),      -- 1 CARTON = 1200 pens (100 boxes)
(1, 'CARTON', 'BOX', 100),      -- 1 CARTON = 100 boxes

-- Paper conversions (A4)
(2, 'PACK', 'SHEET', 100),      -- 1 PACK = 100 sheets
(2, 'REAM', 'SHEET', 500),      -- 1 REAM = 500 sheets
(2, 'CARTON', 'SHEET', 2500),   -- 1 CARTON = 2500 sheets (5 reams)
(2, 'CARTON', 'REAM', 5);       -- 1 CARTON = 5 reams

-- Sample Bins
INSERT INTO Bins (WarehouseID, BinLocation, IsOccupied) VALUES
(1, 'A-01-001', 0),
(1, 'A-01-002', 0),
(1, 'A-02-001', 0),
(2, 'B-01-001', 0),
(2, 'B-01-002', 0);

-- Sample Partners
INSERT INTO Partners (PartnerCode, PartnerName, PartnerType, ContactPerson, Email, PhoneNumber) VALUES
('SUP001', 'ABC Office Supplies Co.', 'Supplier', 'Nguyen Van A', 'contact@abc.com', '0901234567'),
('CUS001', 'XYZ Company Ltd.', 'Customer', 'Tran Thi B', 'info@xyz.com', '0912345678');

GO

-- =============================================
-- VIEWS FOR REPORTING
-- =============================================

-- Physical Inventory View (UC27)
CREATE VIEW vw_PhysicalInventory AS
SELECT 
    w.WarehouseName,
    p.SKU,
    p.ProductName,
    b.BinLocation,
    sb.BatchNumber,
    sb.QtyAvailable,
    sb.QtyReserved,
    sb.QtyInTransit,
    (sb.QtyAvailable + sb.QtyReserved + sb.QtyInTransit) AS TotalStock,
    sb.UoM AS BaseUoM,
    sb.ArrivalDateTime
FROM StockBatches sb
JOIN Warehouses w ON sb.WarehouseID = w.WarehouseID
JOIN Products p ON sb.ProductID = p.ProductID
JOIN Bins b ON sb.BinID = b.BinID
WHERE sb.QtyAvailable + sb.QtyReserved + sb.QtyInTransit > 0;
GO

-- Inbound Report View (UC28)
CREATE VIEW vw_InboundReport AS
SELECT 
    grn.GRNNumber,
    grn.PONumber,
    grn.ReceiptDate,
    grn.GRStatus,
    w.WarehouseName,
    u.FullName AS ReceivedBy,
    p.SKU,
    p.ProductName,
    grd.ReceivedQty,
    grd.UoM AS ReceivedUoM,
    p.BaseUoM,
    grd.BatchNumber,
    b.BinLocation,
    grd.ArrivalDateTime
FROM GoodsReceiptNotes grn
JOIN GoodsReceiptDetails grd ON grn.GRNID = grd.GRNID
JOIN Warehouses w ON grn.WarehouseID = w.WarehouseID
JOIN Users u ON grn.ReceivedBy = u.UserID
JOIN Products p ON grd.ProductID = p.ProductID
JOIN Bins b ON grd.BinID = b.BinID;
GO

-- Outbound Report View (UC29)
CREATE VIEW vw_OutboundReport AS
SELECT 
    gin.GINNumber,
    gin.SONumber,
    gin.IssueDate,
    gin.GIStatus,
    w.WarehouseName,
    u.FullName AS IssuedBy,
    p.SKU,
    p.ProductName,
    gid.IssuedQty,
    gid.UoM AS IssuedUoM,
    p.BaseUoM,
    gid.BatchNumber,
    b.BinLocation
FROM GoodsIssueNotes gin
JOIN GoodsIssueDetails gid ON gin.GINID = gid.GINID
JOIN Warehouses w ON gin.WarehouseID = w.WarehouseID
JOIN Users u ON gin.IssuedBy = u.UserID
JOIN Products p ON gid.ProductID = p.ProductID
JOIN Bins b ON gid.BinID = b.BinID;
GO

-- Inventory Movement Report (UC30)
CREATE VIEW vw_InventoryMovement AS
SELECT 
    w.WarehouseName,
    p.SKU,
    p.ProductName,
    CONVERT(DATE, sm.MovementDate) AS MovementDate,
    SUM(CASE WHEN sm.MovementType IN ('Receipt', 'Transfer-In') THEN sm.Quantity ELSE 0 END) AS TotalInbound,
    SUM(CASE WHEN sm.MovementType IN ('Issue', 'Transfer-Out') THEN ABS(sm.Quantity) ELSE 0 END) AS TotalOutbound,
    MAX(sm.BalanceAfter) AS ClosingBalance,
    p.BaseUoM
FROM StockMovements sm
JOIN Warehouses w ON sm.WarehouseID = w.WarehouseID
JOIN Products p ON sm.ProductID = p.ProductID
GROUP BY w.WarehouseName, p.SKU, p.ProductName, CONVERT(DATE, sm.MovementDate), p.BaseUoM;
GO

PRINT '================================';
PRINT 'DATABASE CREATED SUCCESSFULLY!';
PRINT 'Total Tables: 15';
PRINT 'Total Views: 4';
PRINT '================================';
PRINT 'Tables:';
PRINT '1. Users';
PRINT '2. Warehouses';
PRINT '3. Bins';
PRINT '4. Products';
PRINT '5. ProductUoMConversions (NEW!)';
PRINT '6. Partners';
PRINT '7. PurchaseOrders (merged)';
PRINT '8. GoodsReceiptNotes (header)';
PRINT '9. GoodsReceiptDetails (items)';
PRINT '10. SalesOrders (merged)';
PRINT '11. GoodsIssueNotes (header)';
PRINT '12. GoodsIssueDetails (items)';
PRINT '13. Transfers (merged)';
PRINT '14. StockBatches';
PRINT '15. StockMovements';
PRINT '================================';
PRINT 'UoM Conversion Support:';
PRINT '- Stock always stored in BaseUoM';
PRINT '- Transactions keep original UoM';
PRINT '- Auto-conversion on GRN/GIN post';
PRINT '================================';
GO