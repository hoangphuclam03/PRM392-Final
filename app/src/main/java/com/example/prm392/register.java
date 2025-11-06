package com.example.prm392;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import data.local.DBConnect;

public class register extends AppCompatActivity {

    TextView tvGoToLogin;
    EditText edtFirstName, edtLastName, edtEmailRegister, edtPasswordRegister, edtConfirmPassword;
    Button btnCreateAccount;
    CheckBox cbAgree;

    DBConnect dbConnect; // ✅ Kết nối tới SQLite

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Ánh xạ View
        edtFirstName = findViewById(R.id.edtFirstName);
        edtLastName = findViewById(R.id.edtLastName);
        edtEmailRegister = findViewById(R.id.edtEmailRegister);
        edtPasswordRegister = findViewById(R.id.edtPasswordRegister);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        cbAgree = findViewById(R.id.cbAgree);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        dbConnect = new DBConnect(this);

        btnCreateAccount.setOnClickListener(v -> {
            String firstName = edtFirstName.getText().toString().trim();
            String lastName = edtLastName.getText().toString().trim();
            String email = edtEmailRegister.getText().toString().trim();
            String password = edtPasswordRegister.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!cbAgree.isChecked()) {
                Toast.makeText(this, "Vui lòng đồng ý với điều khoản!", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Lưu dữ liệu vào SQLite
            SQLiteDatabase db = dbConnect.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("firstName", firstName);
            values.put("lastName", lastName);
            values.put("email", email);
            values.put("password", password);

            long result = db.insert("users", null, values);

            if (result != -1) {
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                // Sau khi đăng ký → chuyển về màn đăng nhập
                Intent intent = new Intent(register.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Email đã tồn tại hoặc lỗi hệ thống!", Toast.LENGTH_SHORT).show();
            }

            db.close();
        });

        // Quay lại màn Login
        tvGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(register.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
