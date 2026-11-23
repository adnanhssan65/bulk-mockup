package com.termux.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * Termux Mod — v125 design ki SVG sajawat (waves + ambient rays) native
 * canvas par. Prototype ke SVG paths wohi hain (viewBox 380x40 hero,
 * 380x44 footer), sirf Path + gradient shaders se bane hain.
 * Wave rang design ki tarah hardcoded neon hain — dono themes me wohi.
 */
public class BulkWaveView extends View {

    public static final int MODE_RAYS = 0;
    public static final int MODE_HERO_WAVE = 1;
    public static final int MODE_FOOTER_WAVES = 2;

    private final int mMode;

    public BulkWaveView(Context context, int mode) {
        super(context);
        mMode = mode;
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        switch (mMode) {
            case MODE_RAYS:
                drawRays(canvas, w, h);
                break;
            case MODE_HERO_WAVE:
                drawHeroWave(canvas, w, h);
                break;
            case MODE_FOOTER_WAVES:
                drawFooterWaves(canvas, w, h);
                break;
        }
    }

    // ------------------------------------------------------------ ambient rays

    private void drawRays(Canvas canvas, int w, int h) {
        boolean light = BulkTheme.isLight(getContext());
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float maxR = Math.max(w, h) * 0.62f;

        int ray1 = light ? 0x2955D600 : 0x1C76FF03; // dark .11 / light .16 alpha
        int ray2 = light ? 0x1A2E9E00 : 0x142E9E00; // dark .08 / light .10 alpha
        int clear = 0x00000000;

        p.setShader(new RadialGradient(w * 0.82f, h * 0.14f, maxR,
            new int[]{ray1, clear}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, p);

        p.setShader(new RadialGradient(w * 0.18f, h * 0.82f, maxR,
            new int[]{ray2, clear}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, p);
        p.setShader(null);
    }

    // ------------------------------------------------------------ hero wave (380x40)

    private void drawHeroWave(Canvas canvas, int w, int h) {
        float sx = w / 380f;
        float sy = h / 40f;
        canvas.save();
        canvas.scale(sx, sy);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setStyle(Paint.Style.FILL);
        fill.setShader(new LinearGradient(0, 0, 380, 0,
            new int[]{withAlpha(BulkTheme.NEON, 0.02f), withAlpha(BulkTheme.NEON, 0.18f), withAlpha(BulkTheme.NEON, 0.38f)},
            new float[]{0f, 0.65f, 1f}, Shader.TileMode.CLAMP));
        Path fillPath = new Path();
        fillPath.moveTo(0, 26);
        fillPath.cubicTo(120, 35, 245, 36, 380, 5);
        fillPath.lineTo(380, 28);
        fillPath.cubicTo(250, 42, 125, 38, 0, 30);
        fillPath.close();
        canvas.drawPath(fillPath, fill);

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setShader(new LinearGradient(0, 0, 380, 0,
            new int[]{withAlpha(BulkTheme.NEON, 0.18f), withAlpha(BulkTheme.NEON, 0.85f), BulkTheme.NEON_LIGHT},
            new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        Path strokePath = new Path();
        strokePath.moveTo(0, 27);
        strokePath.cubicTo(120, 35, 245, 36, 380, 5);
        canvas.drawPath(strokePath, stroke);

        canvas.restore();
    }

    // ------------------------------------------------------------ footer waves (380x44)

    private void drawFooterWaves(Canvas canvas, int w, int h) {
        float sx = w / 380f;
        float sy = h / 44f;
        canvas.save();
        canvas.scale(sx, sy);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setStyle(Paint.Style.FILL);
        fill.setShader(new LinearGradient(0, 0, 380, 0,
            new int[]{withAlpha(BulkTheme.NEON, 0.38f), withAlpha(BulkTheme.NEON, 0.14f), withAlpha(BulkTheme.NEON, 0.44f)},
            new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        Path fillPath = new Path();
        fillPath.moveTo(0, 18);
        fillPath.cubicTo(95, 38, 255, 38, 380, 10);
        fillPath.lineTo(380, 44);
        fillPath.lineTo(0, 44);
        fillPath.close();
        canvas.drawPath(fillPath, fill);

        Paint s1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        s1.setStyle(Paint.Style.STROKE);
        s1.setStrokeWidth(1.8f);
        s1.setStrokeCap(Paint.Cap.ROUND);
        s1.setColor(withAlpha(BulkTheme.NEON, 0.88f));
        Path p1 = new Path();
        p1.moveTo(0, 18);
        p1.cubicTo(95, 38, 255, 38, 380, 10);
        canvas.drawPath(p1, s1);

        Paint s2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        s2.setStyle(Paint.Style.STROKE);
        s2.setStrokeWidth(1.4f);
        s2.setStrokeCap(Paint.Cap.ROUND);
        s2.setColor(withAlpha(BulkTheme.NEON_LIGHT, 0.72f));
        Path p2 = new Path();
        p2.moveTo(0, 33);
        p2.cubicTo(125, 12, 255, 42, 380, 24);
        canvas.drawPath(p2, s2);

        canvas.restore();
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(255f * alpha);
        if (a < 0) a = 0;
        if (a > 255) a = 255;
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
