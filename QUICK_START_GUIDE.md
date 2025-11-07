# ⚡ Quick Start Guide - Task Module

## 🚀 Bắt đầu trong 5 phút

### ✅ Bước 1: Verify Files đã copy
Kiểm tra các files sau đã có trong project:
```
✓ activities/CreateTaskActivity.java
✓ activities/ListTasksActivity.java
✓ activities/KanbanBoardActivity.java
✓ adapters/MemberSelectAdapter.java
✓ adapters/TaskAdapter.java
✓ adapters/KanbanAdapter.java
✓ utils/FirebaseHelper.java
✓ All XML layouts
✓ All drawables
```

### ✅ Bước 2: Update build.gradle
Mở `build.gradle (Module: app)` và thêm dependencies như hướng dẫn trên.

### ✅ Bước 3: Update AndroidManifest.xml
Thêm 3 activities vào AndroidManifest.xml

### ✅ Bước 4: Sync Project
```
File > Sync Project with Gradle Files
```

### ✅ Bước 5: Add Dummy Data vào Firebase

#### Vào Firebase Console:
1. Mở Firebase Console: https://console.firebase.google.com
2. Chọn project của bạn
3. Vào `Realtime Database`
4. Click `+` để add data

#### Add Projects:
```json
{
  "projects": {
    "1": {
      "projectId": 1,
      "projectName": "PRM392 Mobile App",
      "description": "Ứng dụng quản lý task",
      "createdBy": 1,
      "createdAt": "01/01/2024"
    }
  }
}
```

#### Add Users:
```json
{
  "users": {
    "1": {
      "id": 1,
      "firstName": "Nguyen",
      "lastName": "Van A",
      "email": "nguyenvana@email.com",
      "password": "123456"
    },
    "2": {
      "id": 2,
      "firstName": "Tran",
      "lastName": "Thi B",
      "email": "tranthib@email.com",
      "password": "123456"
    }
  }
}
```

#### Add Tasks:
```json
{
  "tasks": {
    "1": {
      "taskId": 1,
      "projectId": 1,
      "title": "Thiết kế UI Login",
      "description": "Tạo màn hình đăng nhập với Material Design",
      "dueDate": "25/12/2024",
      "status": "TODO",
      "createdBy": 1
    },
    "2": {
      "taskId": 2,
      "projectId": 1,
      "title": "Code Firebase Integration",
      "description": "Kết nối Firebase Database",
      "dueDate": "26/12/2024",
      "status": "IN_PROGRESS",
      "createdBy": 1
    }
  }
}
```

#### Add Task Assignees:
```json
{
  "task_assignees": {
    "1": {
      "id": 1,
      "taskId": 1,
      "userId": 1
    },
    "2": {
      "id": 2,
      "taskId": 1,
      "userId": 2
    }
  }
}
```

---

## 🧪 Test ngay

### Test 1: Mở ListTasksActivity
Thêm code này vào MainActivity hoặc button nào đó:
```java
Intent intent = new Intent(this, ListTasksActivity.class);
startActivity(intent);
```

**Kết quả mong đợi:**
- Hiển thị 2 tasks đã thêm
- Filter buttons hoạt động
- Click task hiển thị Toast

### Test 2: Mở CreateTaskActivity
```java
Intent intent = new Intent(this, CreateTaskActivity.class);
startActivity(intent);
```

**Kết quả mong đợi:**
- Project spinner hiển thị "PRM392 Mobile App"
- Members list hiển thị 2 users
- DatePicker hoạt động

### Test 3: Mở KanbanBoardActivity
```java
Intent intent = new Intent(this, KanbanBoardActivity.class);
intent.putExtra("PROJECT_ID", 1);
startActivity(intent);
```

**Kết quả mong đợi:**
- Task 1 ở cột TODO
- Task 2 ở cột IN_PROGRESS
- Có thể drag & drop tasks

---

## 🎯 Checklist hoàn thành

### Setup
- [ ] Dependencies đã thêm vào build.gradle
- [ ] Sync Gradle thành công (không có lỗi)
- [ ] AndroidManifest.xml đã update
- [ ] google-services.json có trong app/

### Firebase
- [ ] Firebase project đã tạo
- [ ] Dummy data đã add
- [ ] Database Rules cho phép read/write

### Test
- [ ] Build project thành công
- [ ] ListTasksActivity hiển thị tasks
- [ ] CreateTaskActivity mở được
- [ ] KanbanBoard hiển thị đúng
- [ ] Drag & drop hoạt động

---

## ❓ Câu hỏi thường gặp

**Q: Build bị lỗi "Duplicate class"**
```
A: Check không có 2 dependencies giống nhau với version khác nhau
```

**Q: Firebase không kết nối**
```
A: 
1. Check google-services.json có trong app/
2. Verify package name trong Firebase match với app
3. Rebuild project: Build > Clean Project > Rebuild Project
```

**Q: Tasks không hiển thị**
```
A:
1. Verify dummy data đã add đúng vào Firebase
2. Check currentUserId = 1 trong code
3. Check Firebase Rules cho phép read
```

**Q: Drag & drop không work**
```
A:
1. Test trên real device (emulator có thể lỗi)
2. Long press task card để bắt đầu drag
3. Check logcat có lỗi gì không
```

---

## 🎉 Hoàn thành!

Bạn đã setup xong Task Management Module!

**Các bước tiếp theo:**
1. ✅ Thay currentUserId = 1 bằng user thật
2. ✅ Implement Task Detail screen
3. ✅ Add more features (edit, delete, comments)
4. ✅ Deploy lên device thật để test

**Happy Coding! 🚀**

---

## 📞 Cần trợ giúp?

Nếu gặp vấn đề:
1. Check TASK_MODULE_README.md để xem hướng dẫn chi tiết
2. Check logcat để xem lỗi: `View > Tool Windows > Logcat`
3. Google error message
4. Hỏi team members

**Chúc may mắn! 💪**
```

### Bước 3: Lưu file
`Ctrl + S`

---

## 🎯 TÓM TẮT NHỮNG GÌ VỪA LÀM

✅ **build.gradle** - Đã thêm tất cả dependencies cần thiết
✅ **TASK_MODULE_README.md** - Documentation đầy đủ chi tiết
✅ **QUICK_START_GUIDE.md** - Hướng dẫn bắt đầu nhanh

## 📍 Vị trí các files
```
PRM392-Final/
├── app/
│   ├── build.gradle  ← ĐÃ SỬA (thêm dependencies)
│   └── src/...
├── TASK_MODULE_README.md  ← MỚI TẠO
└── QUICK_START_GUIDE.md   ← MỚI TẠO