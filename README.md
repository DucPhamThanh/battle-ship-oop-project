# Battleship OOP Project

## 📁 Cấu trúc thư mục (Folder Structure)

Dự án được tổ chức theo cấu trúc tiêu chuẩn của một dự án Maven dành cho ứng dụng JavaFX:

```text
battle-ship-oop-project/
├── src/
│   ├── main/
│   │   ├── java/com/battleship/
│   │   │   ├── logic/              # Xử lý logic cốt lõi của trò chơi
│   │   │   ├── model/              # Các lớp mô hình dữ liệu (Ship, Board, Cell,...)
│   │   │   ├── view/               # Các thành phần giao diện mở rộng
│   │   │   ├── App.java            # File khởi chạy ứng dụng (Main class)
│   │   │   ├── GameController.java # Controller điều khiển màn hình chơi game
│   │   │   └── MenuController.java # Controller điều khiển màn hình menu chính
│   │   └── resources/com/battleship/
│   │       ├── assets/             # Hình ảnh, tài nguyên của game (tàu, đại dương,...)
│   │       ├── instructions-view.fxml # Giao diện hướng dẫn chơi
│   │       ├── main-view.fxml      # Giao diện màn hình chính (bàn cờ)
│   │       ├── menu-view.fxml      # Giao diện menu chính
│   │       └── style.css           # File định dạng CSS cho các giao diện JavaFX
│   └── test/                       # Thư mục chứa mã nguồn kiểm thử (Unit tests)
├── pom.xml                         # Cấu hình Maven (Dependencies & Build Plugins)
└── README.md                       # File tài liệu hướng dẫn (chính là file này)
```

## ⚙️ Yêu cầu môi trường (Prerequisites)

Để có thể tải về và chạy dự án thành công, máy tính của bạn cần cài đặt sẵn các công cụ sau:

1. **Java Development Kit (JDK)**: Phiên bản **17** trở lên. 
   - *Kiểm tra bằng lệnh:* `java -version`
2. **Apache Maven**: Dùng để quản lý thư viện và build dự án. 
   - *Kiểm tra bằng lệnh:* `mvn -version`
3. **IDE (Môi trường phát triển tích hợp)**: Khuyến nghị sử dụng **IntelliJ IDEA**, Eclipse, hoặc VS Code có cài đặt Java Extension Pack.
4. **Git**: Để clone mã nguồn về máy.

## 🚀 Hướng dẫn tải về và chạy dự án (Installation & Running)

### Bước 1: Clone mã nguồn từ GitHub về máy

Mở Terminal (Mac/Linux) hoặc Command Prompt/Git Bash (Windows), chọn thư mục bạn muốn lưu dự án và chạy lệnh:

```bash
git clone https://github.com/DucPhamThanh/battle-ship-oop-project.git
```

### Bước 2: Di chuyển vào thư mục gốc và chuyển nhánh
```bash
cd battle-ship-oop-project
git checkout feature
```
*Lưu ý: Các tính năng mới nhất hiện đang nằm ở nhánh `feature`.*

### Bước 3: Build và Chạy ứng dụng

Bạn có thể chạy ứng dụng theo 1 trong 2 cách dưới đây:

#### Cách 1: Sử dụng Terminal bằng lệnh Maven (Khuyên dùng)
Bạn chỉ cần đứng ở thư mục gốc (nơi chứa file `pom.xml`) và chạy lệnh sau:

```bash
mvn clean javafx:run
```
*Lệnh này sẽ tự động dọn dẹp bản build cũ, tải các thư viện JavaFX cần thiết và mở cửa sổ game lên.*

#### Cách 2: Sử dụng IDE (Ví dụ: IntelliJ IDEA)
1. Mở IntelliJ IDEA, chọn **Open**.
2. Tìm đến thư mục `battle-ship-oop-project` vừa tải về, chọn file `pom.xml` và bấm **Open as Project**.
3. Chờ một lúc để IDE tự động nhận diện cấu hình Maven và tải các dependencies (góc dưới cùng bên phải màn hình hiển thị tiến trình sync).
4. Mở cấu trúc thư mục bên trái, tìm đến file chạy chính theo đường dẫn: `src/main/java/com/battleship/App.java`.
5. Click chuột phải vào file `App.java` và chọn **Run 'App.main()'** (Hoặc ấn nút tam giác xanh bên cạnh hàm `main`).

---

## 🎮 Tính năng nổi bật (Features)

Dự án hiện đã tích hợp hệ thống AI thông minh với 3 cấp độ thử thách:

1.  **Chế độ Dễ (Easy):** Bot bắn ngẫu nhiên hoàn toàn, phù hợp để làm quen game.
2.  **Chế độ Trung bình (Medium):** Bot sử dụng thuật toán **Ô bàn cờ (Parity)** và tự động truy đuổi theo trục của tàu ngay khi phát hiện mục tiêu.
3.  **Chế độ Khó (Hard):** Kế thừa trí thông minh của chế độ Trung bình nhưng Bot được trang bị thêm **"Quyền lợi 2 lượt bắn"** (Chỉ mất lượt sau khi trượt 2 phát liên tiếp), cực kỳ thử thách.

---
*Chúc bạn có những giờ phút giải trí vui vẻ với Battleship!*
