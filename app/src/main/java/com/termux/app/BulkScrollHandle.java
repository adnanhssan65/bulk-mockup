package com.termux.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ScrollView;

/**
 * Termux Mod (v134) — file-manager style SCROLL HANDLE: right edge
 * par chhoti si themed pill jo list ki scroll fraction ke sath
 * KHUD uper/neeche chalti hai (top par list = handle uper, bottom
 * par = handle neeche). Ungli se ghaseetna = fast-scroll (scrub);
 * up-chevron tap = bilkul top, down-chevron tap = bilkul bottom.
 * Sirf pill ki apni jagah touchable hai — neeche wali grid/list ke
 * taps bilkul wese hi chalte hain. Idle par ~2.5s me halki (fade)
 * ho jati hai; scroll/drag par wapas jaag jati hai.
 *
 * v136 (owner ke final rules):
 *   - Handle SIRF Add Wallpapers ki gallery grids me hota hai
 *     (All Images + folder grids — BulkGalleryActivity). Baqi har
 *     surface se hata diya gaya hai; forScrollView factory ab
 *     koi istemal nahi karta (sirf tareekhi wajah se mehfooz).
 *   - Shakal system gallery wali pill jesi: patli (36dp), kul
 *     ~64dp unchi, right edge se CHIPKI hui (flush — margin 0),
 *     halki semi-transparent surface, naram shadow, chhote
 *     chevrons or darmiyan hairline divider.
 *
 * Visibility ka faisla har update par: BulkScrollSettings (Show/
 * Hide toggle, gallery gear popup) + surface ka gate + content
 * asal me scrollable hai ya nahi.
 */
public final class BulkScrollHandle extends View {

    private static final int WIDTH_DP = 36;
    private static final int HEIGHT_DP = 64;
    private static final int EDGE_MARGIN_DP = 0;
    private static final int TRACK_INSET_DP = 10;

    private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mChevron = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private float mFraction = 0f;
    private boolean mScrollable = false;
    private boolean mGateOpen = true;
    private boolean mDragging = false;
    private final Runnable mFade = () -> {
        if (getVisibility() == VISIBLE && !mDragging) setAlpha(0.45f);
    };
    private float mDownRawY;
    private float mStartFraction;
    private int mTrackPx = 0;

    // Binding targets (do me se ek hi set hota hai)
    private AbsListView mList;
    private ScrollView mScrollView;

    private BulkScrollHandle(Context c) {
        super(c);
        // v136: halki semi-transparent surface + naram shadow
        // (shadow layer ke liye software layer lazmi hai)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mFill.setStyle(Paint.Style.FILL);
        mFill.setColor(BulkTheme.glassBg(c));
        mFill.setAlpha(150);
        mFill.setShadowLayer(BulkTheme.dp(c, 4), BulkTheme.dp(c, -1),
            BulkTheme.dp(c, 1), 0x40000000);
        mStroke.setStyle(Paint.Style.STROKE);
        mStroke.setColor(BulkTheme.accentBorder(c));
        mStroke.setStrokeWidth(BulkTheme.dp(c, 0.75f));
        mChevron.setStyle(Paint.Style.STROKE);
        mChevron.setColor(BulkTheme.accentStrong(c));
        mChevron.setStrokeWidth(BulkTheme.dp(c, 2));
        mChevron.setStrokeCap(Paint.Cap.ROUND);
        mChevron.setStrokeJoin(Paint.Join.ROUND);
        setContentDescription("Scroll handle");
        setAlpha(0.95f);
        setVisibility(GONE);
    }

    // ------------------------------------------------------------ pure math (tests)

    /** List ki scroll fraction: pehla visible item / kul scrollable range. */
    public static float fractionForList(int firstVisible, int visibleCount, int totalCount) {
        if (totalCount <= 0 || visibleCount <= 0 || totalCount <= visibleCount) return 0f;
        float f = (float) firstVisible / (float) (totalCount - visibleCount);
        return Math.max(0f, Math.min(1f, f));
    }

    /** ScrollView ki fraction: scrollY / maxScroll (clamp shuda). */
    public static float fractionForScroll(int scrollY, int maxScroll) {
        if (maxScroll <= 0) return 0f;
        float f = (float) scrollY / (float) maxScroll;
        return Math.max(0f, Math.min(1f, f));
    }

    /** Drag scrub: shuru ki fraction + ungli ki harkat (track ke hisab se). */
    public static float scrubFraction(float startFraction, float dyPx, float trackPx) {
        if (trackPx <= 0) return startFraction;
        float f = startFraction + dyPx / trackPx;
        return Math.max(0f, Math.min(1f, f));
    }

    /** Scrub fraction se list ki target position (0..total-1). */
    public static int scrubPosition(float fraction, int totalCount) {
        if (totalCount <= 0) return 0;
        int pos = Math.round(fraction * (totalCount - 1));
        return Math.max(0, Math.min(totalCount - 1, pos));
    }

    // ------------------------------------------------------------ factories

    /** Gallery wala GridView (AbsListView) — wrap FrameLayout ke andar. */
    public static BulkScrollHandle forAbsList(Context c, FrameLayout wrap, AbsListView list) {
        BulkScrollHandle h = new BulkScrollHandle(c);
        h.mList = list;
        h.addToWrap(wrap);
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int state) {
                h.wake();
            }

            @Override
            public void onScroll(AbsListView view, int first, int visible, int total) {
                h.mScrollable = total > visible && total > 0;
                h.setFraction(fractionForList(first, visible, total), false);
            }
        });
        wrap.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> h.reposition());
        h.post(h::refresh);
        return h;
    }

    /** Page-scroll wale surfaces (ScrollView ke andar grid/list). */
    public static BulkScrollHandle forScrollView(Context c, FrameLayout wrap, ScrollView sv) {
        BulkScrollHandle h = new BulkScrollHandle(c);
        h.mScrollView = sv;
        h.addToWrap(wrap);
        sv.setOnScrollChangeListener((v, sx, sy, osx, osy) -> h.syncFromScrollView());
        sv.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> h.syncFromScrollView());
        wrap.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> h.reposition());
        h.post(h::refresh);
        return h;
    }

    private void addToWrap(FrameLayout wrap) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            BulkTheme.dp(getContext(), WIDTH_DP), BulkTheme.dp(getContext(), HEIGHT_DP),
            Gravity.END | Gravity.TOP);
        lp.rightMargin = BulkTheme.dp(getContext(), EDGE_MARGIN_DP);
        lp.topMargin = BulkTheme.dp(getContext(), TRACK_INSET_DP);
        setLayoutParams(lp);
        wrap.addView(this);
    }

    // ------------------------------------------------------------ state

    /** Surface ka apna gate (masalan Drop Box: sirf expanded halat me). */
    public void setGateOpen(boolean open) {
        mGateOpen = open;
        updateVisibility();
    }

    /** Setting badalne ke baad (gear popup se) dobara faisla karo. */
    public void refreshGate() {
        updateVisibility();
        wake();
    }

    /** Content/fraction dobara parho (surface ke rebuild ke baad). */
    public void refresh() {
        if (mScrollView != null) syncFromScrollView();
        reposition();
        updateVisibility();
    }

    private void syncFromScrollView() {
        if (mScrollView == null) return;
        int max = maxScroll();
        mScrollable = max > 0;
        setFraction(fractionForScroll(mScrollView.getScrollY(), max), false);
    }

    private int maxScroll() {
        if (mScrollView == null || mScrollView.getChildCount() == 0) return 0;
        return Math.max(0, mScrollView.getChildAt(0).getHeight() - mScrollView.getHeight());
    }

    private void setFraction(float f, boolean fromDrag) {
        mFraction = Math.max(0f, Math.min(1f, f));
        reposition();
        updateVisibility();
        if (!fromDrag) wake();
    }

    private void reposition() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) return;
        int inset = BulkTheme.dp(getContext(), TRACK_INSET_DP);
        mTrackPx = Math.max(0, parent.getHeight() - getHeight() - 2 * inset);
        setTranslationY(mFraction * mTrackPx);
    }

    private void updateVisibility() {
        boolean show = BulkScrollSettings.isShown(getContext()) && mGateOpen && mScrollable;
        setVisibility(show ? VISIBLE : GONE);
    }

    private void wake() {
        animate().cancel();
        if (getVisibility() == VISIBLE) setAlpha(0.95f);
        mHandler.removeCallbacks(mFade);
        mHandler.postDelayed(mFade, 2500);
    }

    // ------------------------------------------------------------ jumps + scrub

    private void jump(boolean toTop) {
        if (mList != null) {
            int count = mList.getCount();
            if (count <= 0) return;
            mList.smoothScrollToPosition(BulkGalleryState.scrollJumpTarget(toTop, count));
        } else if (mScrollView != null) {
            mScrollView.smoothScrollTo(0, toTop ? 0 : maxScroll());
        }
        wake();
    }

    private void scrubTo(float fraction) {
        if (mList != null) {
            int count = mList.getCount();
            if (count <= 0) return;
            mList.setSelection(scrubPosition(fraction, count));
        } else if (mScrollView != null) {
            mScrollView.scrollTo(0, Math.round(fraction * maxScroll()));
        }
    }

    // ------------------------------------------------------------ drawing (v134 pill wali hi shakal)

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float r = w / 2f;
        // v136: right edge se CHIPKI hui shakal — body dayein taraf
        // view se bahar tak jata hai, is liye dayein konay gol nahi
        // bante (flush), bayein konay gol rehte hain (system pill).
        RectF body = new RectF(1, 1, w + r, h - 1);
        canvas.drawRoundRect(body, r, r, mFill);
        canvas.drawRoundRect(body, r, r, mStroke);
        // Hairline divider (˄ / ˅ ke darmiyan)
        mStroke.setAlpha(90);
        canvas.drawLine(w * 0.20f, h / 2f, w * 0.72f, h / 2f, mStroke);
        mStroke.setAlpha(255);
        float cx = w * 0.46f;
        float arm = w * 0.20f;
        float lift = h * 0.08f;
        float cyTop = h * 0.25f;
        Path up = new Path();
        up.moveTo(cx - arm, cyTop + lift);
        up.lineTo(cx, cyTop - lift);
        up.lineTo(cx + arm, cyTop + lift);
        canvas.drawPath(up, mChevron);
        float cyBot = h * 0.75f;
        Path down = new Path();
        down.moveTo(cx - arm, cyBot - lift);
        down.lineTo(cx, cyBot + lift);
        down.lineTo(cx + arm, cyBot - lift);
        canvas.drawPath(down, mChevron);
    }

    // ------------------------------------------------------------ touch: drag = scrub, tap = jump

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mDragging = false;
            mDownRawY = e.getRawY();
            mStartFraction = mFraction;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            wake();
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            float dy = e.getRawY() - mDownRawY;
            int slop = BulkTheme.dp(getContext(), 6);
            if (!mDragging && Math.abs(dy) > slop) mDragging = true;
            if (mDragging) {
                float f = scrubFraction(mStartFraction, dy, mTrackPx);
                mFraction = f;
                reposition();
                scrubTo(f);
                wake();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            if (!mDragging) {
                jump(e.getY() < getHeight() / 2f);
            }
            mDragging = false;
            performClick();
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            mDragging = false;
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
