# WMS System Context for System Testing

Tài liệu này mô tả siêu chi tiết logic nghiệp vụ của các luồng chính trong hệ thống Quản lý Kho (WMS). Mục đích của tài liệu là cung cấp Context (bối cảnh) đầy đủ để Agent có thể hiểu rõ luồng đi của dữ liệu, sự chuyển đổi trạng thái của các chứng từ, và cách các Entity tương tác với nhau trong cơ sở dữ liệu để sinh ra các kịch bản kiểm thử (System Testing) bao quát nhất.

---

## 1. Module Master Data (Dữ liệu nền)
Thực hiện các thao tác CRUD cơ bản để tạo ra nền tảng dữ liệu cho toàn hệ thống:
- **User**: Quản lý tài khoản người dùng, phân quyền và gắn liền người dùng với 1 `Warehouse` cố định.
- **Warehouse**: Quản lý thông tin kho bãi.
- **Product**: Quản lý thông tin sản phẩm (Mã vạch, SKU, Tên, Trọng lượng đơn vị, Danh mục, Base UoM).
- **UoM (Unit of Measure)**: Quản lý quy đổi đơn vị đo lường thông qua `ProductUoMConversion`. Tỷ lệ quy đổi luôn được nhân với số lượng để đưa về `Base UoM` khi tính toán tồn kho.
- **Bin**: Quản lý vị trí lưu trữ trong kho (Vị trí, Sức chứa tối đa/Max Weight). Tất cả logic nhập/xuất/chuyển kho đều kiểm tra sức chứa khả dụng của Bin (tổng trọng lượng hàng hiện có + hàng đang dự kiến nhập).
- **Partner**: Quản lý thông tin Nhà cung cấp (Supplier) và Khách hàng (Customer).

---

## 2. Luồng Inbound (Mua hàng): PurchaseOrder -> GoodsReceiptNote

Quy trình nhập hàng từ Nhà cung cấp.

1. **Tạo Purchase Order (PO):**
   - User tạo PO trạng thái `Draft` chọn Warehouse và Supplier.
   - Khi chọn "Submit for Approval", hệ thống validate PO phải có ít nhất 1 dòng chi tiết hợp lệ. PO chuyển sang trạng thái `Pending Approval`.
2. **Duyệt PO:**
   - Manager duyệt PO. Trạng thái PO đổi thành `Approved`.
   - **Tự động sinh GRN:** Hệ thống tự động tạo 1 phiếu Nhập kho (Goods Receipt Note - GRN) trạng thái `Draft`.
   - Ứng với mỗi dòng trong PO, hệ thống tạo dòng GRN Detail:
     - Tính toán trọng lượng hàng nhập = Qty * ConversionFactor * UnitWeight.
     - Tự động quét và cấp phát `Bin` theo thuật toán: Tìm Bin đang active trong kho có sức chứa còn lại >= Trọng lượng hàng nhập. (Sức chứa còn lại tính theo tồn kho vật lý + trọng lượng hàng trên các GRN Draft đang chờ duyệt).
     - Sinh `BatchNumber` duy nhất: `BATCH-{yyyyMMdd}-PO{poNumber}-P{productId}`.
3. **Thực thi Nhập kho (Post GRN):**
   - Storekeeper thực nhận hàng hóa cập nhật `receivedQty`.
   - Nếu số lượng thực nhận > số lượng thiếu (Ordered - Received): Báo lỗi.
   - Nếu nhập thành công:
     - Tạo mới hoặc cộng dồn vào `StockBatch` theo `BatchNumber`, tăng `qtyAvailable`.
     - Lưu lịch sử `StockMovement` (MovementType: `Receipt`, StockType: `Physical`).
     - Trạng thái GRN chuyển sang `Posted`.
   - **Hoàn thành PO:**
     - Nếu nhận **đủ** số lượng của toàn bộ các line: PO chuyển sang `Completed`.
     - Nếu nhận **một phần** (nhập thiếu): PO chuyển sang `Incomplete`. Hệ thống **tự động sinh ra 1 phiếu GRN Back-order (Draft)** cho số lượng hàng còn thiếu, tiếp tục chờ Storekeeper nhập kho đợt sau. Hệ thống cho phép nối tiếp quy trình Back-order cho đến khi nhập đầy đủ số lượng trên PO.

---

## 3. Luồng Outbound (Bán hàng): SalesOrder -> PurchaseRequest -> GoodsIssueNote

Luồng bán hàng cho Customer. Đặc biệt chú ý kịch bản thiếu hàng (ATP - Available To Promise).

### 3.1. Kịch bản Đủ Hàng (Happy Path)
1. **Tạo SO:** User tạo SO (`Draft`), Submit.
2. **ATP Check:** Hệ thống tính `Total_ATP = Σ(qtyAvailable - qtyReserved)` cho Product này trong Warehouse. Vì ATP >= Qty yêu cầu, SO chuyển sang `Pending Approval`.
3. **Approve SO:** 
   - Manager duyệt SO. Hệ thống thực hiện Reservation Stock theo FIFO:
     - Quét các lô hàng (`StockBatch`) theo thứ tự ưu tiên nhập trước (ArrivalDateTime ASC).
     - Tăng `qtyReserved` cho từng Batch cho đến khi đủ số lượng.
     - Log `StockMovement` loại `Reserve` (Stocktype: `Reserved`).
   - Tự động sinh phiếu Xuất Kho (Goods Issue Note - GIN) trạng thái `Draft` với số lượng cấp phát từ chính các Batch đã đặt trước.
   - SO chuyển trạng thái `Approved`.
4. **Post GIN:**
   - Storekeeper báo cáo số lượng thực xuất (`issuedQty`).
   - Hệ thống trừ tồn kho vật lý: giảm `qtyAvailable` và giảm `qtyReserved` của lô Batch đó.
   - Log StockMovement loại `Issue / Physical` & `Issue / Reserved`.
   - GIN -> `Posted`. Nếu xuất đủ, SO -> `Completed`. Nếu xuất thiếu, SO -> `Incomplete` và tự động sinh GIN Back-order.

### 3.2. Kịch bản Thiếu Hàng (Loopback Workflow: SO -> PR -> PO -> GRN -> GIN)
Đây là workflow phân nhánh tự động khi khách đặt hàng nhưng tồn kho vật lý hiện tại (hoặc Available to Promise) không đủ.

1. **ATP Check (Phát hiện thiếu hàng):**
   - Khi User Submit SO, nếu `Total_ATP < Qty_Needed`, hệ thống bật cờ `hasShortage=true`.
   - Nếu User xác nhận tạo Yêu cầu mua hàng (Purchase Request - PR), hệ thống:
     - Đặt SO ở trạng thái `Pending Approval`.
     - **Tự động sinh PR (`Pending`)** chứa số lượng hàng bị thiếu chính xác (`MissingQty` = `BaseQtyNeeded` - `Total_ATP`). Linked PR này gắn cứng với SO thông qua `RelatedSalesOrder`.
2. **Approve SO (Tạm ngưng luồng Outbound):**
   - Manager duyệt SO. Hệ thống check xem PR liên kết đã hoàn thành chưa.
   - Do PR đang `Pending`, hệ thống chuyển PR sang `Approved`, đồng thời ngưng xử lý SO và chuyển SO sang **`Waiting for Stock`** (không tạo GIN).
3. **Quy trình Thu mua (Procurement - Convert PR to PO):**
   - Nhân viên thu mua quét các PR trạng thái `Approved`.
   - Gom nhóm (Aggregate) các sản phẩm giống nhau từ nhiều PR để tạo thành 1 PO duy nhất.
   - Trạng thái PR chuyển thành `Converted`. PO được sinh ra mang trạng thái `Pending Approval`.
4. **Nhập hàng (PO -> GRN):**
   - PO được duyệt -> Approved -> Sinh GRN Draft.
   - Storekeeper thực hiện nhận hàng (Post GRN). 
   - Tồn kho vật lý trong kho tăng lên (`qtyAvailable`). 
   - PO chuyển thành `Completed` (Kích hoạt quá trình Loopback Listner).
5. **Loopback Event (Tự động kích hoạt lại SO):**
   - Logic `handlePOCompletionLoopback` (trong `GoodsReceiptNoteServiceImpl`) tự động được gọi.
   - Hệ thống truy ngược từ PO hoàn tất -> Tách ra các PR liên đới -> Chuyển PR thành `Completed`.
   - Kiểm tra xem đối với từng `soId` (Sales Order), tất cả các PR thuộc về nó đã `Completed` hết chưa.
   - Nếu điều kiện thỏa mãn, hệ thống **tự động gọi lại hàm `approveSO`** cho SO đang nằm chờ ở trạng thái `Waiting for Stock`.
   - Lúc này, vì hàng đã về kho và ATP đã đủ (Vòng lặp chạy qua Branch B):
     - Hệ thống thực hiện FIFO reserve kho (Giữ chỗ cho lô hàng vừa nhập).
     - **Tự động sinh GIN Draft**.
     - Đổi SO thành `Approved`.
6. **Hoàn tất Outbound:**
   - Storekeeper vào màn hình xuất kho thấy GIN Draft, thực hiện thao tác Post GIN để trừ số dư vật lý đi và gửi hàng cho khách.
   - GIN -> `Posted`, SO -> `Completed` (Toàn bộ chuỗi cung ứng đóng lại).

---

## 4. Luồng Transfer Note (Điều Chuyển Nội Bộ Trong Cùng 1 Kho)

Đây là thao tác luân chuyển hàng hóa thực tế từ vị trí lưu trữ (Bin) này sang Bin khác trong cùng một Warehouse.

1. **Tạo Transfer Note:** 
   - Manager tạo và chọn chính xác: Product, Số lượng, Batch Number, `fromBin` và `toBin`.
   - Validation quan trọng: `Sức chứa khả dụng của toBin >= Trọng lượng hàng nhận vào`. 
   - Nếu hợp lệ, lưu thông tin. Việc tạo lưu TN mặc định mang trạng thái `Approved` do nó chỉ là nghiệp vụ sắp xếp lại kho.
2. **Complete Transfer Note:**
   - Storekeeper thực hiện luân chuyển hàng dưới kho vật lý và nhấn Complete.
   - Hệ thống quét `StockBatch` cũ ở `fromBin`.
     - Validation: Kiểm tra tồn kho khả dụng `qtyAvailable - qtyReserved >= baseQty`.
     - Giảm `qtyAvailable` ở lô nguồn. Log `Transfer-Out / Physical`.
   - Tạo mới (hoặc cộng dồn) vào `StockBatch` đích ở `toBin` với cùng `BatchNumber`.
     - Tăng `qtyAvailable` ở lô đích. Log `Transfer-In / Physical`.
   - Cập nhật trạng thái TN thành `Completed`.

---

## 5. Luồng Transfer Order (Điều Chuyển Giữa Hai Kho Khác Nhau)

Luồng TO liên quan tới quá trình lưu chuyển đi qua trạng thái `In-Transit` (Đang đi đường). Cấu trúc: TO -> GIN (Kho đi) -> In Transit -> GRN (Kho đến).

1. **Yêu cầu (Từ phía Dest Warehouse):** 
   - Nhân viên kho đích (Destination) tạo Transfer Order (`Draft`) xin hàng từ kho nguồn (Source).
   - Đệ trình duyệt -> Trạng thái TO chuyển thành `Pending`.
   - Validation ATP: Số lượng xin không được vượt quá số tồn khả dụng `(qtyAvailable - qtyReserved)` thực tế tại Kho nguồn.
2. **Duyệt (Từ phía Source Warehouse):**
   - Manager kho nguồn Approve TO. Status TO chuyển thành `Approved`.
   - Hệ thống tự động Reserve tồn kho (logic FIFO y hệt Sales Order) và **tự động sinh phiếu GIN (`Draft`)** cho kho Nguồn để thực hiện xuất kho.
3. **Kho Nguồn Xuất Hàng (Post GIN):**
   - Storekeeper kho nguồn báo cáo xuất hàng thực tế.
   - GIN -> `Posted`.
   - Hàng được trừ ngay lập tức khỏi `StockBatch` nguồn (Vừa trừ Physical vừa trừ Reserved).
   - **Tự động tạo StockBatch `qtyInTransit` tại Kho đích**, tự động allocate một Bin thích hợp bên phía Dest Warehouse cho đống hàng đang đi trên đường.
   - **Tự động sinh phiếu GRN (`Draft`)** cho kho đích. Dữ liệu (BatchNumber, số lượng, Bin dự kiến) lấy chính xác từ lô hàng mà kho nguồn vừa xuất đi.
   - Trạng thái TO cập nhật thành `In-Transit`.
4. **Kho Đích Nhận Hàng (Post GRN):**
   - Khi xe hàng tới nơi, Storekeeper kho đích nhận hàng lên hệ thống (Post GRN).
   - Hệ thống tìm batch có chứa hàng transit (`qtyInTransit`), trừ đi `qtyInTransit` và đổ vào `qtyAvailable` của kho đích.
   - TO chuyển thành `Completed` (Nếu nhận đủ) hoặc tạo GRN Back-order (Nếu còn hàng đang thất lạc chưa nhận hết).

---

## 6. Module Report (Báo cáo)

Các nghiệp vụ báo cáo phục vụ việc truy xuất dữ liệu từ hai core engine: `StockMovementDao` và `StockBatchDao` (trong interface `ReportServiceImpl`):

- **Inbound / Outbound History Report**:
  - Truy xuất lịch sử thay đổi tồn kho (StockMovement) được map trực tiếp sang DTO hiển thị. Chứa chi tiết về ngày tháng, kho, BatchNumber, số lượng, Movement Type và số dư cuối tương ứng của lô.
- **Inventory Balance Report (Báo cáo xuất nhập tồn toàn diện)**:
  - Dùng logic 2 câu truy vấn tối ưu chạy song song:
    1. Lấy dữ liệu **Tồn Đầu Kỳ (`Opening Stock`)**: Tính tổng của (Receipt + Transfer-In) - (Issue + Transfer-Out) xảy ra TRƯỚC thời điểm `startDate`.
    2. Lấy dữ liệu **Phát sinh Trong Kỳ (`Period Summary`)**: Tính tổng lượng Nhập / Xuất diễn ra TRONG phạm vi `[startDate, endDate]`.
  - Từ dữ liệu trên tính ra được **Tồn Cuối Kỳ (`Closing Stock`)**: `Closing = Opening + Inbound - Outbound`.
- **Current Stock Report**:
  - Trả về tình trạng tồn kho theo thời gian thực tại các Bin và các Batch khác nhau. Lấy trực tiếp từ bảng `StockBatch` nhóm (Group by) theo Kho và Sản phẩm. 
  - Đính kèm theo cờ (flag) lọc `Low Stock` (Kho đang cạn dưới ngưỡng an toàn) hỗ trợ cho hệ thống Dashboards/Cảnh báo tồn kho.
