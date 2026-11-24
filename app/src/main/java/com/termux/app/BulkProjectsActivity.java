package com.termux.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.R;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.util.List;

/**
 * Termux Mod — Projects screen (v125, approved design).
 *
 * Glass panels ki list: naam + counts (Wallpapers · Mockups · Ready),
 * tap par panel khulta hai — Run / Batch / Delete. Neeche fixed bar me
 * "Add New Project (ZIP)". List hamesha asal projects.conf + disk
 * folders se banti hai (BulkProjects) — koi hardcoded list nahi.
 * Run/Batch/Delete v124 ki tarah terminal me command bhej kar chalte hain.
 */
public class BulkProjectsActivity extends Activity {

    private static final String LOG_TAG = "BulkProjectsScreen";

    /** Theme toggle (recreate) ke baad bhi wahi project khula rahe. */
    private static int sExpandedNumber = Integer.MIN_VALUE;

    // v132: header search ki halat (recreate ke baad bhi mehfooz)
    private static String sQuery = "";
    private static boolean sSearchOpen = false;

    private LinearLayout mBody;
    private LinearLayout mSearchRow;
    private android.widget.EditText mSearchField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Projects");
        buildShell();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList(); // counts taaza
        // v132: manual-move drop folders ka auto-import (peeche);
        // kuch move ho to counts dobara taaza
        new Thread(() -> {
            int moved = BulkDropFolders.syncNow(this);
            if (moved > 0) runOnUiThread(this::refreshList);
        }).start();
    }

    /**
     * v132: shell ek dafa banta hai (header + search row + scroll +
     * bottom bar); list sirf refreshList se badalti hai — search
     * field ka focus/text rebuild par mehfooz rehta hai.
     */
    private void buildShell() {
        BulkTheme.applyWindow(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));
        root.addView(buildHeader());
        root.addView(buildSearchRow());

        ScrollView scroll = new ScrollView(this);
        mBody = new LinearLayout(this);
        mBody.setOrientation(LinearLayout.VERTICAL);
        mBody.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 8), BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));
        scroll.addView(mBody);
        // v136: scroll handle yahan NAHI — sirf Add Wallpapers ki
        // gallery grids me hota hai (owner ka rule)
        root.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // Bottom fixed bar: Add New Project (ZIP)
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setBackgroundColor(BulkTheme.glassBg(this));
        View topLine = new View(this);
        topLine.setBackground(BulkTheme.hLine(this));
        bottom.addView(topLine, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, BulkTheme.dp(this, 0.6f))));
        LinearLayout barInner = new LinearLayout(this);
        barInner.setOrientation(LinearLayout.HORIZONTAL);
        barInner.setGravity(Gravity.CENTER);
        barInner.setPadding(BulkTheme.dp(this, 20), BulkTheme.dp(this, 12), BulkTheme.dp(this, 20), BulkTheme.dp(this, 12));
        // Plus icon + label wala primary button — design me + icon hai
        LinearLayout btnWrap = new LinearLayout(this);
        btnWrap.setOrientation(LinearLayout.HORIZONTAL);
        btnWrap.setGravity(Gravity.CENTER);
        btnWrap.setBackground(BulkTheme.primaryBg(this));
        btnWrap.setPadding(BulkTheme.dp(this, 16), BulkTheme.dp(this, 11), BulkTheme.dp(this, 16), BulkTheme.dp(this, 11));
        btnWrap.setClickable(true);
        btnWrap.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, BulkAddProjectActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Project add wali screen nahi khul saki.", Toast.LENGTH_SHORT).show();
            }
        });
        ImageView plus = BulkTheme.icon(this, R.drawable.ic_bulk_plus, BulkTheme.onAccent(this), 16);
        plus.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 16), BulkTheme.dp(this, 16)));
        btnWrap.addView(plus);
        TextView plusText = new TextView(this);
        plusText.setText(BulkStrings.t(this, BulkStrings.ADD_NEW_PROJECT_ZIP));
        plusText.setTextSize(13.5f);
        plusText.setTypeface(Typeface.DEFAULT_BOLD);
        plusText.setTextColor(BulkTheme.onAccent(this));
        LinearLayout.LayoutParams ptLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ptLp.setMargins(BulkTheme.dp(this, 7), 0, 0, 0);
        plusText.setLayoutParams(ptLp);
        btnWrap.addView(plusText);
        barInner.addView(btnWrap, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bottom.addView(barInner);
        root.addView(bottom);

        setContentView(root);
        refreshList();
        BulkFonts.apply(this);
    }

    /**
     * Header: back (bayein) + title/subtitle + SEARCH icon + theme
     * icon (dayein) — subHeader jesa hi layout, search ke izafe ke
     * sath (owner ki hidayat, v132).
     */
    private LinearLayout buildHeader() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(BulkTheme.dp(this, 20), BulkTheme.dp(this, 14),
            BulkTheme.dp(this, 20), BulkTheme.dp(this, 10));
        bar.addView(BulkTheme.circleButton(this, R.drawable.ic_bulk_back, 34, v -> finish()));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tlp.setMargins(BulkTheme.dp(this, 10), 0, BulkTheme.dp(this, 8), 0);
        titles.setLayoutParams(tlp);
        TextView t = new TextView(this);
        t.setText(BulkStrings.t(this, BulkStrings.PROJECTS));
        t.setTextSize(16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(BulkTheme.textMain(this));
        titles.addView(t);
        TextView s = new TextView(this);
        s.setText(BulkStrings.t(this, BulkStrings.PROJECTS_SUBTITLE));
        s.setTextSize(11);
        s.setTextColor(BulkTheme.textMuted(this));
        titles.addView(s);
        bar.addView(titles);
        bar.addView(BulkTheme.circleButton(this, R.drawable.ic_bulk_search, 34,
            v -> toggleSearch()));
        LinearLayout.LayoutParams thp = new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 34), BulkTheme.dp(this, 34));
        thp.leftMargin = BulkTheme.dp(this, 8);
        View themeBtn = BulkTheme.themeButton(this);
        themeBtn.setLayoutParams(thp);
        bar.addView(themeBtn);
        wrap.addView(bar);
        View line = new View(this);
        line.setBackground(BulkTheme.hLine(this));
        wrap.addView(line, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, BulkTheme.dp(this, 0.6f))));
        return wrap;
    }

    /** Search field wali row — icon se khulti/band hoti hai; likhte hi list live filter hoti hai. */
    private LinearLayout buildSearchRow() {
        mSearchRow = new LinearLayout(this);
        mSearchRow.setOrientation(LinearLayout.HORIZONTAL);
        mSearchRow.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 10),
            BulkTheme.dp(this, 18), 0);
        mSearchRow.setVisibility(sSearchOpen ? View.VISIBLE : View.GONE);
        mSearchField = new android.widget.EditText(this);
        mSearchField.setHint(BulkStrings.t(this, BulkStrings.SEARCH_PROJECT));
        mSearchField.setSingleLine(true);
        mSearchField.setTextSize(13);
        mSearchField.setTextColor(BulkTheme.textMain(this));
        mSearchField.setHintTextColor(BulkTheme.textMuted(this));
        mSearchField.setBackground(BulkTheme.pillBg(this));
        mSearchField.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 9),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 9));
        mSearchRow.addView(mSearchField, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (!sQuery.isEmpty()) mSearchField.setText(sQuery);
        mSearchField.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                sQuery = s == null ? "" : s.toString();
                refreshList();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        return mSearchRow;
    }

    private void toggleSearch() {
        sSearchOpen = !sSearchOpen;
        if (mSearchRow == null) return;
        mSearchRow.setVisibility(sSearchOpen ? View.VISIBLE : View.GONE);
        if (sSearchOpen) {
            if (mSearchField != null) mSearchField.requestFocus();
        } else {
            // Band karne par filter saaf — poori list wapas
            sQuery = "";
            if (mSearchField != null) mSearchField.setText("");
            refreshList();
        }
    }

    /** Sirf list dobara banti hai (counts + search filter fresh). */
    private void refreshList() {
        if (mBody == null) return;
        mBody.removeAllViews();
        List<BulkProjects.ProjectInfo> projects = loadProjects();
        if (projects.isEmpty()) {
            mBody.addView(emptyState());
            BulkFonts.applyToTree(this, mBody);
            return;
        }
        java.util.List<BulkProjects.ProjectInfo> shown = new java.util.ArrayList<>();
        for (BulkProjects.ProjectInfo p : projects) {
            if (BulkPreviewSource.matchesQuery(p.name, p.number, sQuery)) shown.add(p);
        }
        if (shown.isEmpty()) {
            TextView none = new TextView(this);
            none.setText(BulkStrings.t(this, BulkStrings.NO_MATCHING));
            none.setTextColor(BulkTheme.textMuted(this));
            none.setTextSize(12.5f);
            none.setGravity(Gravity.CENTER);
            none.setPadding(0, BulkTheme.dp(this, 18), 0, 0);
            mBody.addView(none);
            BulkFonts.applyToTree(this, mBody);
            return;
        }
        if (sExpandedNumber == Integer.MIN_VALUE && shown.get(0).number > 0) {
            sExpandedNumber = shown.get(0).number; // design: pehla project khula hua
        }
        for (BulkProjects.ProjectInfo p : shown) {
            LinearLayout panel = projectPanel(p);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, BulkTheme.dp(this, 12));
            panel.setLayoutParams(lp);
            mBody.addView(panel);
        }
        BulkFonts.applyToTree(this, mBody);
    }

    private LinearLayout emptyState() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setBackground(BulkTheme.glassPanel(this));
        panel.setPadding(BulkTheme.dp(this, 24), BulkTheme.dp(this, 24), BulkTheme.dp(this, 24), BulkTheme.dp(this, 24));
        TextView title = new TextView(this);
        title.setText(BulkStrings.t(this, BulkStrings.NO_PROJECTS));
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(BulkTheme.textMain(this));
        title.setGravity(Gravity.CENTER);
        panel.addView(title);
        TextView sub = new TextView(this);
        sub.setText(BulkStrings.format(this, BulkStrings.NO_PROJECTS_SUB,
            BulkStrings.t(this, BulkStrings.ADD_NEW_PROJECT_ZIP)));
        sub.setTextSize(12);
        sub.setTextColor(BulkTheme.textMuted(this));
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0, BulkTheme.dp(this, 4), 0, 0);
        sub.setLayoutParams(subLp);
        panel.addView(sub);
        return panel;
    }

    private LinearLayout projectPanel(final BulkProjects.ProjectInfo p) {
        final boolean expanded = p.number > 0 && p.number == sExpandedNumber;

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(expanded ? BulkTheme.glassPanelStrong(this) : BulkTheme.glassPanel(this));
        panel.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 14), BulkTheme.dp(this, 14), BulkTheme.dp(this, 14));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setClickable(true);
        head.setOnClickListener(v -> {
            sExpandedNumber = expanded ? 0 : p.number; // 0 = koi khula nahi
            refreshList();
        });

        // v132: DP — project ke asal frame ki chhoti preview.
        // SIRF DP par tap se bara preview popup khulta hai; baqi
        // row ka tap purana hi hai (expand / actions)
        if (p.number > 0) {
            View dp = BulkPreviewUi.dpView(this, p.number, 44);
            dp.setClickable(true);
            dp.setOnClickListener(v -> BulkPreviewUi.showPopup(this, p, false, null));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                BulkTheme.dp(this, 44), BulkTheme.dp(this, 44));
            dlp.rightMargin = BulkTheme.dp(this, 11);
            dp.setLayoutParams(dlp);
            head.addView(dp);
        }

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextSize(14);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setTextColor(BulkTheme.textMain(this));
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(name);

        if (p.number > 0) {
            TextView counts = new TextView(this);
            counts.setText(countsSpans(p.number));
            counts.setTextSize(11);
            counts.setTypeface(Typeface.MONOSPACE);
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cLp.setMargins(0, BulkTheme.dp(this, 4), 0, 0);
            counts.setLayoutParams(cLp);
            texts.addView(counts);
        }
        head.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout arrow = new LinearLayout(this);
        arrow.setGravity(Gravity.CENTER);
        arrow.setBackground(BulkTheme.arrowCircleBg(this));
        arrow.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 26), BulkTheme.dp(this, 26)));
        ImageView av = BulkTheme.icon(this, R.drawable.ic_bulk_chevron, BulkTheme.accent(this), 14);
        av.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 14), BulkTheme.dp(this, 14)));
        arrow.addView(av);
        arrow.setRotation(expanded ? 90f : 0f);
        head.addView(arrow);
        panel.addView(head);

        if (expanded) {
            View line = new View(this);
            line.setBackground(BulkTheme.hLine(this));
            LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, BulkTheme.dp(this, 0.6f)));
            lineLp.setMargins(0, BulkTheme.dp(this, 12), 0, BulkTheme.dp(this, 10));
            line.setLayoutParams(lineLp);
            panel.addView(line);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);

            // v127 RUN FILTER: 0 wallpapers wale project ka Run action
            // bilkul nahi dikhta (Batch/Delete par koi asar nahi).
            int wallCount = p.number > 0
                ? BulkProjects.countFiles(BulkProjects.wallpapersDir(p.number)) : 0;
            boolean first = true;
            if (BulkProjects.canRun(wallCount)) {
                TextView run = BulkTheme.primaryButton(this, BulkStrings.t(this, BulkStrings.RUN),
                    v -> BulkRunHelper.startRun(this, p));
                run.setTextSize(12);
                setEnabledStyle(run, p.workCmd != null);
                actions.addView(run, actionLp(0));
                first = false;
            }

            TextView batch = BulkTheme.secondaryButton(this, BulkStrings.t(this, BulkStrings.BATCH), v -> runInTerminal(p.batchCmd));
            batch.setTextSize(12);
            setEnabledStyle(batch, p.batchCmd != null);
            actions.addView(batch, actionLp(first ? 0 : BulkTheme.dp(this, 8)));

            TextView delete = new TextView(this);
            delete.setText(BulkStrings.t(this, BulkStrings.DELETE));
            delete.setTextSize(12);
            delete.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            delete.setTextColor(BulkTheme.danger(this));
            delete.setGravity(Gravity.CENTER);
            delete.setBackground(BulkTheme.dangerBg(this));
            delete.setPadding(BulkTheme.dp(this, 8), BulkTheme.dp(this, 8), BulkTheme.dp(this, 8), BulkTheme.dp(this, 8));
            delete.setClickable(true);
            delete.setOnClickListener(v -> showDeletePopup(p));
            setEnabledStyle(delete, p.deleteCmd != null);
            actions.addView(delete, actionLp(BulkTheme.dp(this, 8)));

            panel.addView(actions);
        }
        return panel;
    }

    private LinearLayout.LayoutParams actionLp(int leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(leftMargin, 0, 0, 0);
        return lp;
    }

    private void setEnabledStyle(TextView b, boolean enabled) {
        b.setEnabled(enabled);
        b.setAlpha(enabled ? 1f : 0.4f);
        b.setClickable(enabled);
    }

    /** Counts line design ke spans ke sath: labels sub, numbers bold (Mockups accent). */
    private SpannableStringBuilder countsSpans(int n) {
        File dir = BulkProjects.projectDir(n);
        int w = BulkProjects.countFiles(new File(dir, n + "_wallpapers"));
        int m = BulkProjects.countFiles(new File(dir, n + "_Mockups"));
        int r = BulkProjects.countFiles(new File(dir, n + "_Ready_Wallpapers"));

        SpannableStringBuilder sb = new SpannableStringBuilder();
        java.util.List<int[]> ranges = new java.util.ArrayList<>();
        java.util.List<Integer> colors = new java.util.ArrayList<>();
        appendCount(sb, ranges, colors, "Wallpapers: ", String.valueOf(w), BulkTheme.textMain(this));
        sb.append(" · ");
        appendCount(sb, ranges, colors, "Mockups: ", String.valueOf(m), BulkTheme.accent(this));
        sb.append(" · ");
        appendCount(sb, ranges, colors, "Ready: ", String.valueOf(r), BulkTheme.textMain(this));
        // Pehle poori line ka base rang (labels + dots), phir numbers ke rang us ke upar
        sb.setSpan(new ForegroundColorSpan(BulkTheme.textSub(this)), 0, sb.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        for (int i = 0; i < ranges.size(); i++) {
            int[] rg = ranges.get(i);
            sb.setSpan(new ForegroundColorSpan(colors.get(i)), rg[0], rg[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.BOLD), rg[0], rg[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    private void appendCount(SpannableStringBuilder sb, java.util.List<int[]> ranges,
                             java.util.List<Integer> colors, String label, String value, int valueColor) {
        sb.append(label);
        int vStart = sb.length();
        sb.append(value);
        ranges.add(new int[]{vStart, sb.length()});
        colors.add(valueColor);
    }

    private List<BulkProjects.ProjectInfo> loadProjects() {
        try {
            return BulkProjects.mergeProjects(ProjectsLauncher.readProjectsList(), BulkProjects.scanDiskProjects());
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Projects parhne me masla: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    // ------------------------------------------------------------ v133 Delete popup

    /**
     * Delete popup: runner ke apne delete-menu ke radio options
     * (BulkDeleteMenu — script ke hisab se). Kuch NAHI chalta jab
     * tak Done na dabay; Cancel / X = koi action nahi. Option 1
     * pehle se select. Done par:
     *   - aam choice: del command console popup me, digit printf se
     *     pipe karke;
     *   - Warehouse choice: script batch ka sawal poochta hai, is
     *     liye command terminal par interactive jati hai (bina pipe).
     */
    private void showDeletePopup(final BulkProjects.ProjectInfo p) {
        if (p == null || p.deleteCmd == null || p.number <= 0) return;
        final String script = BulkProjects.detectMainScript(
            BulkProjects.listFileNames(BulkProjects.projectDir(p.number)), p.number);
        final List<BulkDeleteMenu.Option> options = BulkDeleteMenu.optionsFor(script);
        final int[] selected = {options.get(0).digit};

        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            p.name + " — " + BulkStrings.t(this, BulkStrings.DELETE), dialog);

        final LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        panel.addView(group);
        final Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            group.removeAllViews();
            for (final BulkDeleteMenu.Option o : options) {
                group.addView(deleteOptionRow(o, o.digit == selected[0],
                    () -> { selected[0] = o.digit; refresh[0].run(); }));
            }
        };
        refresh[0].run();

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = BulkTheme.dp(this, 6);
        row.setLayoutParams(rowLp);
        TextView cancel = BulkTheme.secondaryButton(this,
            BulkStrings.t(this, BulkStrings.CANCEL), v -> dialog.dismiss());
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cLp.rightMargin = BulkTheme.dp(this, 5);
        cancel.setLayoutParams(cLp);
        row.addView(cancel);
        TextView done = BulkTheme.primaryButton(this,
            BulkStrings.t(this, BulkStrings.DONE_BTN), v -> {
                dialog.dismiss();
                if (BulkDeleteMenu.isWarehouse(script, selected[0])) {
                    runInTerminal(p.deleteCmd);
                } else {
                    String piped = BulkRunSettings.buildPipedCommand(p.deleteCmd,
                        java.util.Collections.singletonList(String.valueOf(selected[0])));
                    BulkRunHelper.runCommandInPopup(this, p, piped);
                }
            });
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        dLp.leftMargin = BulkTheme.dp(this, 5);
        done.setLayoutParams(dLp);
        row.addView(done);
        panel.addView(row);

        BulkTheme.showPanelDialog(this, panel);
    }

    /** Delete popup ki radio row (Run Settings wali hi shakal: ●/○ + label). */
    private LinearLayout deleteOptionRow(final BulkDeleteMenu.Option o, boolean selected,
                                         final Runnable onTap) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(selected ? BulkTheme.secondaryBg(this) : BulkTheme.pillBg(this));
        // v136: compact radio rows (Run Settings wali hi height family)
        row.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 7),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 7));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = BulkTheme.dp(this, 6);
        row.setLayoutParams(lp);

        TextView dot = new TextView(this);
        dot.setText(selected ? "●" : "○");
        dot.setTextColor(selected ? BulkTheme.accent(this) : BulkTheme.textMuted(this));
        dot.setTextSize(13);
        row.addView(dot);

        TextView label = new TextView(this);
        label.setText(o.digit + ".  " + o.label);
        label.setTextColor(BulkTheme.textMain(this));
        label.setTextSize(13.5f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.leftMargin = BulkTheme.dp(this, 9);
        label.setLayoutParams(llp);
        row.addView(label);

        row.setOnClickListener(v -> { if (onTap != null) onTap.run(); });
        return row;
    }

    /** Command ko terminal me chalao — TermuxActivity khol kar wahan type ho jati hai. */
    private void runInTerminal(String command) {
        if (command == null) return;
        try {
            Intent intent = new Intent(this, TermuxActivity.class);
            intent.putExtra(BulkDashboardActivity.EXTRA_RUN_COMMAND, command);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Terminal nahi khul saka.", Toast.LENGTH_SHORT).show();
        }
    }
}
