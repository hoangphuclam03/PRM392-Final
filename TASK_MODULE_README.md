# Task Management Module - Documentation

## 📋 Tổng quan
Module quản lý Task bao gồm 3 chức năng chính:
1. **Create Task & Assign Members** - Tạo task và phân công thành viên
2. **List Your Tasks** - Danh sách task của user
3. **Kanban Board** - Bảng Kanban với drag & drop

---

## 📁 Cấu trúc Files

### Activities (com.example.prm392.activities)
- `CreateTaskActivity.java` - Tạo task mới
- `ListTasksActivity.java` - Danh sách task
- `KanbanBoardActivity.java` - Bảng Kanban

### Adapters (com.example.prm392.adapters)
- `MemberSelectAdapter.java` - Adapter cho chọn members
- `TaskAdapter.java` - Adapter cho danh sách task
- `KanbanAdapter.java` - Adapter cho Kanban board

### Utils (com.example.prm392.utils)
- `FirebaseHelper.java` - Helper class cho Firebase

### Layouts (res/layout)
- `activity_create_task.xml`
- `activity_list_tasks.xml`
- `activity_kanban_board.xml`
- `item_member_select.xml`
- `item_task.xml`
- `item_kanban_card.xml`

### Drawables (res/drawable)
- `circle_avatar.xml`
- `badge_todo.xml`
- `badge_in_progress.xml`
- `badge_in_review.xml`
- `badge_done.xml`

---

## 🔥 Firebase Structure
```
firebase-database/
├── tasks/
│   └── {taskId}/
│       ├── taskId: int
│       ├── projectId: int
│       ├── title: String
│       ├── description: String
│       ├── dueDate: String (dd/MM/yyyy)
│       ├── status: String (TODO, IN_PROGRESS, IN_REVIEW, DONE)
│       └── createdBy: int
│
├── task_assignees/
│   └── {assigneeId}/
│       ├── id: int
│       ├── taskId: int
│       └── userId: int
│
├── projects/
│   └── {projectId}/
│       ├── projectId: int
│       ├── projectName: String
│       ├── description: String
│       ├── createdBy: int
│       └── createdAt: String
│
└── users/
    └── {userId}/
        ├── id: int
        ├── firstName: String
        ├── lastName: String
        ├── email: String
        └── password: String
```

---

## 🚀 Cách sử dụng

### 1. Mở CreateTaskActivity
```java
Intent intent = new Intent(context, CreateTaskActivity.class);
intent.putExtra("PROJECT_ID", projectId); // Optional
startActivity(intent);
```

### 2. Mở ListTasksActivity
```java
Intent intent = new Intent(context, ListTasksActivity.class);
startActivity(intent);
```

### 3. Mở KanbanBoardActivity
```java
Intent intent = new Intent(context, KanbanBoardActivity.class);
intent.putExtra("PROJECT_ID", projectId); // Required
startActivity(intent);
```

---

## 🎨 Features

### CreateTaskActivity
- ✅ Chọn project từ spinner
- ✅ Nhập tiêu đề và mô tả task
- ✅ Chọn ngày hết hạn (DatePicker)
- ✅ Chọn nhiều members với checkbox
- ✅ Chọn trạng thái ban đầu
- ✅ Lưu vào Firebase
- ✅ Validation input

### ListTasksActivity
- ✅ Hiển thị tất cả tasks của user
- ✅ Filter theo status
- ✅ Swipe to refresh
- ✅ Hiển thị project name
- ✅ Hiển thị assignees với avatar
- ✅ Empty state
- ✅ FAB để tạo task mới

### KanbanBoardActivity
- ✅ 4 cột: TODO, IN_PROGRESS, IN_REVIEW, DONE
- ✅ Drag & Drop giữa các cột
- ✅ Real-time update từ Firebase
- ✅ Hiển thị số lượng task mỗi cột
- ✅ Visual feedback khi drag

---

## ⚠️ TODO - Cần bổ sung

### 1. User Authentication
```java
// TODO: Thay thế currentUserId = 1 bằng:
SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
int currentUserId = prefs.getInt("USER_ID", -1);
```

### 2. Load Members theo Project
```java
// TODO: Query members thuộc project được chọn
dbRef.child("project_members")
     .orderByChild("projectId")
     .equalTo(selectedProjectId)
     .addListenerForSingleValueEvent(...);
```

### 3. Task Detail Screen
Cần tạo `TaskDetailActivity.java` để:
- Xem chi tiết task
- Edit task
- Add comments

---

## 🐛 Troubleshooting

### Issue 1: Firebase không kết nối
**Solution:**
- Kiểm tra `google-services.json` có trong thư mục `app/`
- Verify Firebase project đã setup

### Issue 2: Drag & Drop không hoạt động
**Solution:**
- Kiểm tra `android:clipToPadding="false"` trong RecyclerView
- Verify event listener đã setup đúng

### Issue 3: Tasks không hiển thị
**Solution:**
- Check Firebase data structure
- Verify currentUserId đúng
- Check Firebase Rules cho phép read

---

## 📊 Firebase Security Rules
```json
{
  "rules": {
    "tasks": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "task_assignees": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

---

## 📞 Support

Nếu gặp vấn đề:
1. Check logcat: `adb logcat | grep Firebase`
2. Verify Firebase connection
3. Test với dummy data trước
4. Check permissions trong AndroidManifest

**Good luck! 🚀**