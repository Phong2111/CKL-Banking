# 📱 Hướng dẫn kết nối Android App với Firebase Emulator

## ✅ Có thể chạy bình thường!

Khi chạy Docker emulator, Android Studio có thể chạy app **bình thường**, nhưng cần cấu hình để app kết nối với emulator thay vì production Firebase.

---

## 🔧 Đã cấu hình sẵn

Đã tạo `CKLBankingApplication.java` để tự động kết nối với emulator khi:
- `USE_EMULATOR = true` (mặc định)
- Docker emulator đang chạy

---

## 🚀 Cách sử dụng

### 1. Khởi động Docker Emulator:

```bash
docker-compose up -d
```

### 2. Mở Android Studio và chạy app:

- Build và Run app như bình thường
- App sẽ tự động kết nối với emulator

### 3. Kiểm tra Logcat:

Bạn sẽ thấy log:
```
===========================================
🔥 Firebase Emulator Mode ENABLED
Firestore: 10.0.2.2:8080
Functions: 10.0.2.2:5001
===========================================
```

---

## ⚙️ Cấu hình

### File: `CKLBankingApplication.java`

```java
// Bật/tắt emulator mode
private static final boolean USE_EMULATOR = true; // true = emulator, false = production

// Host cho Android Emulator
private static final String EMULATOR_HOST = "10.0.2.2"; // Android Emulator

// Hoặc cho Physical Device
// private static final String EMULATOR_HOST = "192.168.1.100"; // IP máy tính
```

### Các tùy chọn:

#### 1. **Android Emulator (Mặc định):**
```java
private static final String EMULATOR_HOST = "10.0.2.2";
```
- `10.0.2.2` = localhost của máy host từ Android emulator
- Không cần thay đổi gì

#### 2. **Physical Device:**
```java
private static final String EMULATOR_HOST = "192.168.1.100"; // IP máy tính của bạn
```

**Cách lấy IP máy tính:**
- Windows: `ipconfig` → tìm IPv4 Address
- Mac/Linux: `ifconfig` hoặc `ip addr`

**Lưu ý:** Đảm bảo Android device và máy tính cùng mạng WiFi.

#### 3. **Production Firebase:**
```java
private static final boolean USE_EMULATOR = false;
```

---

## 🧪 Test Flow

### 1. Khởi động Emulator:

```bash
docker-compose up -d
```

### 2. Chạy Android App:

- Mở Android Studio
- Run app trên emulator hoặc device
- App sẽ kết nối với Firestore emulator

### 3. Test OTP Email:

1. **Tạo transaction trong app**
2. **App tạo email_request trong Firestore emulator**
3. **Function trigger → Gửi email thật**
4. **User nhận email và nhập OTP**

### 4. Test VNPay:

1. **Tạo payment request trong app**
2. **Function tạo payment URL**
3. **App hiển thị payment URL**

---

## 🔍 Kiểm tra kết nối

### 1. Xem Logcat trong Android Studio:

Tìm log:
```
🔥 Firebase Emulator Mode ENABLED
```

### 2. Kiểm tra Firestore Emulator UI:

- Mở: http://localhost:4000
- Vào Firestore tab
- Xem data được tạo từ app

### 3. Test đơn giản:

1. **Đăng ký user mới trong app**
2. **Check Firestore emulator UI** → Collection `users` → Document mới
3. **Tạo transaction** → Check collection `transactions`

---

## ⚠️ Lưu ý quan trọng

### 1. **Authentication:**

Firebase Auth **KHÔNG** chạy trên emulator (chưa cấu hình). 
- App vẫn có thể đăng nhập/đăng ký
- Nhưng sẽ dùng production Firebase Auth
- Chỉ Firestore và Functions dùng emulator

### 2. **Data Persistence:**

- Emulator data được lưu trong Docker volume
- Khi `docker-compose down`, data vẫn còn (trừ khi dùng `-v`)
- Khi `docker-compose up`, data được import lại

### 3. **Network:**

- **Android Emulator:** Dùng `10.0.2.2` (không cần thay đổi)
- **Physical Device:** Cần IP máy tính, cùng WiFi

### 4. **Production vs Emulator:**

- **Emulator:** Test local, không tốn phí
- **Production:** Deploy lên Firebase (cần Blaze plan)

---

## 🐛 Troubleshooting

### Lỗi: Không kết nối được Firestore

**Kiểm tra:**
1. Docker emulator đang chạy: `docker ps`
2. Port 8080 đang mở: `netstat -ano | findstr :8080`
3. Logcat có log "Emulator Mode ENABLED"

**Giải pháp:**
```bash
# Restart emulator
docker-compose restart

# Hoặc rebuild
docker-compose down
docker-compose up --build
```

### Lỗi: Physical device không kết nối được

**Kiểm tra:**
1. Device và máy tính cùng WiFi
2. IP máy tính đúng
3. Firewall không chặn port 8080

**Giải pháp:**
```java
// Trong CKLBankingApplication.java
private static final String EMULATOR_HOST = "192.168.1.100"; // IP máy tính
```

### Lỗi: Functions không chạy

**Kiểm tra:**
1. Functions emulator đang chạy: http://localhost:5001
2. Check logs: `docker-compose logs -f`

**Giải pháp:**
```bash
# Xem logs
docker-compose logs firebase-emulator

# Restart
docker-compose restart
```

---

## 📊 Workflow hoàn chỉnh

```
1. Docker Emulator
   ↓ docker-compose up
   ↓ Chạy tại localhost:8080 (Firestore)
   ↓ Chạy tại localhost:5001 (Functions)

2. Android Studio
   ↓ Run app
   ↓ App kết nối 10.0.2.2:8080 (Firestore emulator)
   ↓ Tạo data trong Firestore

3. Functions Trigger
   ↓ Firestore document created
   ↓ Function chạy trong emulator
   ↓ Gửi email THẬT (OTP)
   ↓ Hoặc tạo VNPay URL

4. User nhận kết quả
   ↓ Email OTP đến inbox
   ↓ Hoặc payment URL được tạo
```

---

## ✅ Checklist

- [x] Docker emulator đang chạy
- [x] `USE_EMULATOR = true` trong `CKLBankingApplication.java`
- [x] Android app build và run thành công
- [x] Logcat hiển thị "Emulator Mode ENABLED"
- [x] Firestore emulator UI hiển thị data từ app
- [x] Functions trigger và chạy đúng

---

## 🎯 Tóm tắt

**Có thể chạy bình thường!** ✅

- ✅ Android Studio chạy app như bình thường
- ✅ App tự động kết nối với emulator
- ✅ Tất cả tính năng hoạt động (OTP, VNPay, v.v.)
- ✅ Không cần thay đổi code trong Activities
- ✅ Chỉ cần bật/tắt `USE_EMULATOR` flag

**Lưu ý:** Đảm bảo Docker emulator đang chạy trước khi chạy app!

