package com.example.prm392;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.CalendarEvents;


public class CalendarEventsActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private RecyclerView rvTasks;
    private TextView tvSelectedDate;

    // Dữ liệu toàn tháng hiện tại (giả lập). Bạn có thể thay bằng Firestore/API thật.
    private final List<CalendarEvents> monthlyEvents = new ArrayList<>();

    // Adapter cho RecyclerView
    private TasksAdapter tasksAdapter;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Decorator vẽ dot dưới các ngày có event
    private CalendarDotDecorator dotDecorator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_events);

        calendarView = findViewById(R.id.calendarView);
        rvTasks = findViewById(R.id.rvTasks);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);

        // RecyclerView basic
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        tasksAdapter = new TasksAdapter();
        rvTasks.setAdapter(tasksAdapter);

        // Decorator dấu chấm (đỏ). Bạn muốn màu nào thì đổi ở đây:
        int dotColor = ContextCompat.getColor(this, android.R.color.holo_red_dark);
        dotDecorator = new CalendarDotDecorator(dotColor, 5f);
        calendarView.addDecorator(dotDecorator);

        // Ngày hôm nay
        LocalDate today = LocalDate.now();
        calendarView.setSelectedDate(today);
        updateSelectedTitle(today);

        // Lần đầu mở: nạp data cho tháng của "today"
        loadMonthAndDecorate(today);

        // Khi đổi tháng (vuốt trái/phải)
        calendarView.setOnMonthChangedListener((widget, date) -> {
            LocalDate firstDayOfNewMonth = date.getDate();
            // Load data theo tháng được chọn
            loadMonthAndDecorate(firstDayOfNewMonth);
            // Nếu ngày đang chọn không thuộc tháng mới, bạn có thể tự động chọn mùng 1
            // Ở đây mình giữ nguyên selectedDate hiện tại
        });

        // Khi chọn ngày
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            LocalDate picked = date.getDate();
            updateSelectedTitle(picked);
            showTasksForDate(picked);
        });

        // Hiển thị task cho ngày hôm nay lần đầu
        showTasksForDate(today);
    }

    /** Cập nhật tiêu đề "Tasks on ..." */
    private void updateSelectedTitle(LocalDate date) {
        String text = "Tasks on " + date.format(DATE_FMT);
        tvSelectedDate.setText(text);
    }

    /** Tải dữ liệu theo tháng và cập nhật dot trên lịch */
    private void loadMonthAndDecorate(LocalDate anyDayOfMonth) {
        // Tính first-last của tháng
        LocalDate first = anyDayOfMonth.withDayOfMonth(1);
        LocalDate last = first.plusMonths(1).minusDays(1);

        // TODO: Thay bằng gọi Firestore/API thật
        // Ở đây mình demo giả lập dữ liệu theo khoảng [first, last]:
        fetchMonthlyEventsMock(first, last);

        // Build set CalendarDay để vẽ dot
        Set<CalendarDay> dots = new HashSet<>();
        for (CalendarEvents ev : monthlyEvents) {
            if (ev.getEventDate() == null) continue;
            try {
                LocalDate d = LocalDate.parse(ev.getEventDate(), DATE_FMT);
                // Chỉ lấy những ngày thuộc range tháng đang xem
                if ((d.isEqual(first) || d.isAfter(first)) && (d.isEqual(last) || d.isBefore(last))) {
                    dots.add(CalendarDay.from(d));
                }
            } catch (Exception ignore) {}
        }
        dotDecorator.setDates(dots);
        calendarView.invalidateDecorators();
    }

    /** Lọc và hiển thị danh sách tasks/event của 1 ngày */
    private void showTasksForDate(LocalDate date) {
        String key = DATE_FMT.format(date);
        List<CalendarEvents> filtered = new ArrayList<>();
        for (CalendarEvents ev : monthlyEvents) {
            if (key.equals(ev.getEventDate())) {
                filtered.add(ev);
            }
        }
        tasksAdapter.submit(filtered);
    }

    // ==========================================================
    // Demo MOCK DATA. Bạn thay bằng Firestore: tasks where dueDate between [first, last]
    // ==========================================================
    private void fetchMonthlyEventsMock(LocalDate first, LocalDate last) {
        monthlyEvents.clear();

        // Giả lập: thêm vài event rải rác trong tháng
        // eventDate phải đúng format "yyyy-MM-dd"
        LocalDate d1 = first.plusDays(2);
        LocalDate d2 = first.plusDays(5);
        LocalDate d3 = first.plusDays(5);   // cùng ngày -> vẫn vẽ 1 dot
        LocalDate d4 = first.plusDays(10);
        LocalDate d5 = first.plusDays(15);

        monthlyEvents.add(new CalendarEvents(1, 101, d1.format(DATE_FMT)));
        monthlyEvents.add(new CalendarEvents(2, 102, d2.format(DATE_FMT)));
        monthlyEvents.add(new CalendarEvents(3, 103, d3.format(DATE_FMT)));
        monthlyEvents.add(new CalendarEvents(4, 104, d4.format(DATE_FMT)));
        monthlyEvents.add(new CalendarEvents(5, 105, d5.format(DATE_FMT)));

        // Bạn có thể thêm nhiều hơn nếu muốn test
    }

    // ==========================================================
    // RecyclerView Adapter hiển thị danh sách task theo ngày
    // ==========================================================
    private static class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.VH> {

        private final List<CalendarEvents> data = new ArrayList<>();

        public void submit(List<CalendarEvents> newData) {
            data.clear();
            if (newData != null) data.addAll(newData);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_event_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CalendarEvents item = data.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTaskId, tvEventDate;

            VH(@NonNull View itemView) {
                super(itemView);
                tvTaskId = itemView.findViewById(R.id.tvTaskId);
                tvEventDate = itemView.findViewById(R.id.tvEventDate);
            }

            void bind(CalendarEvents ev) {
                tvTaskId.setText("Task ID: " + ev.getTaskId());
                tvEventDate.setText("Date: " + ev.getEventDate());
            }
        }
    }
}
