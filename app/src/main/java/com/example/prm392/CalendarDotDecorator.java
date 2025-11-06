package com.example.prm392;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;
import java.util.Set;

/** Decorator gắn dấu chấm (dot) dưới những ngày có sự kiện/task */
public class CalendarDotDecorator implements DayViewDecorator {

    private final int color;                    // màu dot
    private final float radiusDp;               // kích thước dot (dp)
    private final Set<CalendarDay> dates = new HashSet<>(); // ngày có dot

    public CalendarDotDecorator(int color) {
        this(color, 6f);
    }

    public CalendarDotDecorator(int color, float radiusDp) {
        this.color = color;
        this.radiusDp = radiusDp;
    }

    /** Cập nhật danh sách ngày có dot và gọi invalidateDecorators() ở Activity */
    public void setDates(Set<CalendarDay> newDates) {
        dates.clear();
        if (newDates != null) dates.addAll(newDates);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new DotSpanEx(radiusDp, color));
    }
}
