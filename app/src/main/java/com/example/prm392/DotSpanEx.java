package com.example.prm392;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

/** Vẽ 1 chấm tròn dưới số ngày */
public class DotSpanEx implements LineBackgroundSpan {

    private final float radiusDp;
    private final int color;

    public DotSpanEx(float radiusDp, int color) {
        this.radiusDp = radiusDp;
        this.color = color;
    }

    @Override
    public void drawBackground(
            Canvas canvas, Paint paint,
            int left, int right, int top, int baseline, int bottom,
            CharSequence text, int start, int end, int lineNum
    ) {
        int oldColor = paint.getColor();
        float oldStroke = paint.getStrokeWidth();
        Paint.Style oldStyle = paint.getStyle();

        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        // chuyển dp -> px đơn giản (density mặc định 3 nếu không có view context)
        float density = canvas.getDensity() > 0 ? canvas.getDensity() / 160f : 3f;
        float r = radiusDp * density;

        float cx = (left + right) / 2f;
        float cy = bottom + r * 1.6f; // vị trí dot dưới chữ ngày
        canvas.drawCircle(cx, cy, r, paint);

        paint.setColor(oldColor);
        paint.setStrokeWidth(oldStroke);
        paint.setStyle(oldStyle);
    }
}
