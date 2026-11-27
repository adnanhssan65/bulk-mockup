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

    // v155: Status strip (Remove Project se upar) + us ka taaza data
    private LinearLayout mStatusStrip;
    private int[] mStatusNumbers = new int[0];
    private int[] mStatusReady = new int[0];

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
        // v155: Status strip — Remove Project se bhi upar
        // (owner: "uper status wala laga do, pata chal jaye
        // kis kis me pare hue hain")
        root.addView(buildStatusStrip());
        // v138: Remove Project ki chhoti si row screen ke TOP par
        // (owner ki hidayat: har project ke sath alag button NAHI)
        root.addView(buildRemoveProjectRow());

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
        // v155: Status strip hamesha UNFILTERED poori list se
        // taaza hoti hai (search filter is par lagu nahi)
        refreshStatusStrip(projects);
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
                // v138: enable ka faisla bhi asal (run-time) command par
                setEnabledStyle(run, BulkProjects.effectiveCommand(p, "process") != null);
                actions.addView(run, actionLp(0));
                first = false;
            }

            // v138: Batch bhi snapshot alias par nahi — asal command
            // v151: Batch bhi BulkRunHelper se — pehle drop sync,
            // phir terminal session (Run wala hi rasta/view)
            TextView batch = BulkTheme.secondaryButton(this, BulkStrings.t(this, BulkStrings.BATCH),
                v -> showBatchNamePopup(p, () -> BulkRunHelper.startBatch(this, p)));
            batch.setTextSize(12);
            setEnabledStyle(batch, BulkProjects.effectiveCommand(p, "batch") != null);
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
            setEnabledStyle(delete, BulkProjects.effectiveCommand(p, "del") != null);
            actions.addView(delete, actionLp(BulkTheme.dp(this, 8)));

            panel.addView(actions);

            // v154: Terminal ki chhoti si row — is dafa ke liye
            // Run/Batch/Delete seedha terminal view me. Run View
            // ki saved setting (Popup default) is se nahi badalti.
            LinearLayout termRow = new LinearLayout(this);
            termRow.setOrientation(LinearLayout.HORIZONTAL);
            termRow.setGravity(Gravity.CENTER_VERTICAL);
            termRow.setBackground(BulkTheme.pillBg(this));
            termRow.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 8),
                BulkTheme.dp(this, 12), BulkTheme.dp(this, 8));
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tLp.topMargin = BulkTheme.dp(this, 10);
            termRow.setLayoutParams(tLp);
            termRow.setClickable(true);
            ImageView tIcon = BulkTheme.icon(this, R.drawable.ic_bulk_terminal,
                BulkTheme.textMain(this), 15);
            tIcon.setLayoutParams(new LinearLayout.LayoutParams(
                BulkTheme.dp(this, 15), BulkTheme.dp(this, 15)));
            termRow.addView(tIcon);
            TextView tLabel = new TextView(this);
            tLabel.setText(BulkStrings.t(this, BulkStrings.TERMINAL));
            tLabel.setTextSize(13);
            tLabel.setTextColor(BulkTheme.textMain(this));
            LinearLayout.LayoutParams tlLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            tlLp.leftMargin = BulkTheme.dp(this, 8);
            tLabel.setLayoutParams(tlLp);
            termRow.addView(tLabel);
            ImageView tChev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron,
                BulkTheme.textMuted(this), 13);
            tChev.setLayoutParams(new LinearLayout.LayoutParams(
                BulkTheme.dp(this, 13), BulkTheme.dp(this, 13)));
            termRow.addView(tChev);
            termRow.setOnClickListener(v -> showTerminalPopup(p));
            panel.addView(termRow);
        }
        return panel;
    }

    /** Terminal popup: Run / Batch / Delete — teeno is dafa terminal me. */
    private void showTerminalPopup(final BulkProjects.ProjectInfo p) {
        if (p == null || p.number <= 0) return;
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            p.name + " — " + BulkStrings.t(this, BulkStrings.TERMINAL), dialog);
        panel.addView(terminalActionRow(BulkStrings.t(this, BulkStrings.RUN),
            BulkProjects.effectiveCommand(p, "process") != null, () -> {
                dialog.dismiss();
                BulkRunHelper.startInTerminal(this, p, "process");
            }));
        panel.addView(terminalActionRow(BulkStrings.t(this, BulkStrings.BATCH),
            BulkProjects.effectiveCommand(p, "batch") != null, () -> {
                dialog.dismiss();
                showBatchNamePopup(p, () -> BulkRunHelper.startInTerminal(this, p, "batch"));
            }));
        panel.addView(terminalActionRow(BulkStrings.t(this, BulkStrings.DELETE),
            BulkProjects.effectiveCommand(p, "del") != null, () -> {
                dialog.dismiss();
                runInTerminal(BulkProjects.effectiveCommand(p, "del"));
            }));
        BulkTheme.showPanelDialog(this, panel);
    }

    private LinearLayout terminalActionRow(String label, boolean enabled,
                                           final Runnable onTap) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 10),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = BulkTheme.dp(this, 8);
        row.setLayoutParams(lp);
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(BulkTheme.textMain(this));
        name.setTextSize(13.5f);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(name, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1f : 0.4f);
        row.setClickable(enabled);
        row.setOnClickListener(v -> { if (onTap != null) onTap.run(); });
        return row;
    }

    /**
     * v154 (item 11 — sirf NAAM): Batch dabane par naam wala
     * popup. Field me default (auto) naam pehle se likha hota
     * hai, sath \u00D7 (clear) icon se field saaf karke apna
     * naam likha ja sakta hai, neeche Done. Chuna hua naam
     * engine folder ki ".batch_name" file se runner tak jata
     * hai or batch folder usi naam se banta hai — files apne
     * purane naamon/silsile ke sath EK hi flat folder me jati
     * hain. Batch ka baqi flow 100% original hai.
     */
    private void showBatchNamePopup(final BulkProjects.ProjectInfo p,
                                    final Runnable proceed) {
        if (p == null || p.number <= 0) return;
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            p.name + " — " + BulkStrings.t(this, BulkStrings.BATCH_NAME), dialog);

        LinearLayout fieldRow = new LinearLayout(this);
        fieldRow.setOrientation(LinearLayout.HORIZONTAL);
        fieldRow.setGravity(Gravity.CENTER_VERTICAL);
        final android.widget.EditText field = new android.widget.EditText(this);
        field.setText(BulkProjects.defaultBatchName(
            BulkProjects.warehouseDir(p.number), p.number));
        field.setSingleLine(true);
        field.setTextSize(14);
        field.setTextColor(BulkTheme.textMain(this));
        field.setSelectAllOnFocus(true);
        fieldRow.addView(field, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView clear = new TextView(this);
        clear.setText("\u00D7");
        clear.setTextSize(20);
        clear.setGravity(Gravity.CENTER);
        clear.setTextColor(BulkTheme.textMuted(this));
        clear.setClickable(true);
        clear.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 36), BulkTheme.dp(this, 36)));
        clear.setOnClickListener(v -> {
            field.setText("");
            field.requestFocus();
        });
        fieldRow.addView(clear);
        panel.addView(fieldRow);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = BulkTheme.dp(this, 10);
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
                BulkProjects.writeBatchName(p.number, field.getText().toString());
                dialog.dismiss();
                if (proceed != null) proceed.run();
            });
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        dLp.leftMargin = BulkTheme.dp(this, 5);
        done.setLayoutParams(dLp);
        row.addView(done);
        panel.addView(row);

        BulkTheme.showPanelDialog(this, panel);
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
        // v154: Mockups/Ready ab engine folders se NAHI — TermuxMod
        // ke EK hi "Pairs" folder se, number ki parity par
        // (odd = Mockups, even = Ready)
        int[] pairs = BulkProjects.pairsCounts(BulkProjects.pairsDir(n));
        int m = pairs[0];
        int r = pairs[1];

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
        // v138: asal delete command run ke waqt banti hai (snapshot
        // alias kabhi execute nahi hoti — popup wala rasta
        // non-interactive bash hai, wahan alias chalta hi nahi)
        if (p == null || p.number <= 0
            || BulkProjects.effectiveCommand(p, "del") == null) return;
        final String script = BulkProjects.resolveScript(p.number);
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
                // v138: dono raston (terminal Warehouse + popup pipe)
                // par asal command — snapshot ki bare alias kabhi nahi
                final String delCmd = BulkProjects.effectiveCommand(p, "del");
                if (BulkDeleteMenu.isWarehouse(script, selected[0])) {
                    runInTerminal(delCmd);
                } else {
                    String piped = BulkRunSettings.buildPipedCommand(delCmd,
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

    // ------------------------------------------------------------ v138 Remove Project

    /**
     * v154 — Screen ke TOP par "Remove Project" ki CHHOTI slim
     * row (approved preview v2): pill, chhota trash icon + naam
     * + arrow — project cards se wazeh chhoti. Tap par ek hi
     * popup khulta hai: multi-select removal + Recycle Bin.
     * TermuxMod/Project N + images mehfooz rehte hain.
     */
    private LinearLayout buildRemoveProjectRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 7),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 7));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(BulkTheme.dp(this, 18), BulkTheme.dp(this, 10),
            BulkTheme.dp(this, 18), 0);
        row.setLayoutParams(lp);
        row.setClickable(true);
        row.setOnClickListener(v -> showRemoveProjectPicker());
        ImageView icon = BulkTheme.icon(this, R.drawable.ic_bulk_trash, BulkTheme.danger(this), 15);
        icon.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 15), BulkTheme.dp(this, 15)));
        row.addView(icon);
        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.REMOVE_PROJECT));
        label.setTextSize(14);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(BulkTheme.danger(this));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        llp.leftMargin = BulkTheme.dp(this, 8);
        label.setLayoutParams(llp);
        row.addView(label);
        ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron,
            BulkTheme.textMuted(this), 13);
        chev.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 13), BulkTheme.dp(this, 13)));
        row.addView(chev);
        return row;
    }

    // ------------------------------------------------------------ v155: Status strip + popup

    /**
     * v155 (PURE): status entries — {number, ready} jore, input
     * order me. number <= 0 wale skip; onlyPositive ho to
     * ready <= 0 wale bhi skip. Null / lambai ka farq mehfooz.
     */
    static java.util.List<int[]> statusEntries(int[] numbers, int[] readyCounts,
                                               boolean onlyPositive) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        if (numbers == null || readyCounts == null) return out;
        int len = Math.min(numbers.length, readyCounts.length);
        for (int i = 0; i < len; i++) {
            if (numbers[i] <= 0) continue;
            if (onlyPositive && readyCounts[i] <= 0) continue;
            out.add(new int[]{numbers[i], readyCounts[i]});
        }
        return out;
    }

    /** v155 (PURE): strip ka compact text — "P1 6 · P18 5 · P20 5"; khaali par "". */
    static String statusStripText(java.util.List<int[]> entries) {
        if (entries == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int[] e : entries) {
            if (e == null || e.length < 2) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append('P').append(e[0]).append(' ').append(e[1]);
        }
        return sb.toString();
    }

    /** v155 (PURE): popup ki rows — har row me zyada se zyada 2 entries (do column). */
    static java.util.List<java.util.List<int[]>> statusPairRows(
            java.util.List<int[]> entries) {
        java.util.List<java.util.List<int[]>> rows = new java.util.ArrayList<>();
        if (entries == null) return rows;
        for (int i = 0; i < entries.size(); i += 2) {
            java.util.List<int[]> row = new java.util.ArrayList<>();
            row.add(entries.get(i));
            if (i + 1 < entries.size()) row.add(entries.get(i + 1));
            rows.add(row);
        }
        return rows;
    }

    /**
     * v155: Status strip ka khaali dabba — ek slim si row
     * (Remove Project wali row jesi unchai). Andar ka text
     * refreshStatusStrip bharti hai. Tap par popup khulta
     * hai jahan sab projects theek se dikhte hain.
     */
    private LinearLayout buildStatusStrip() {
        mStatusStrip = new LinearLayout(this);
        mStatusStrip.setOrientation(LinearLayout.HORIZONTAL);
        mStatusStrip.setGravity(Gravity.CENTER_VERTICAL);
        mStatusStrip.setBackground(BulkTheme.pillBg(this));
        mStatusStrip.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 8),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(BulkTheme.dp(this, 18), BulkTheme.dp(this, 10),
            BulkTheme.dp(this, 18), 0);
        mStatusStrip.setLayoutParams(lp);
        mStatusStrip.setClickable(true);
        mStatusStrip.setOnClickListener(v -> showStatusPopup());
        return mStatusStrip;
    }

    /** Strip ka text taaza karo — cards wali hi Pairs parity ginti (Ready). */
    private void refreshStatusStrip(List<BulkProjects.ProjectInfo> projects) {
        if (mStatusStrip == null) return;
        int size = projects != null ? projects.size() : 0;
        int[] numbers = new int[size];
        int[] ready = new int[size];
        for (int i = 0; i < size; i++) {
            BulkProjects.ProjectInfo p = projects.get(i);
            numbers[i] = p != null ? p.number : 0;
            ready[i] = numbers[i] > 0
                ? BulkProjects.pairsCounts(BulkProjects.pairsDir(numbers[i]))[1] : 0;
        }
        mStatusNumbers = numbers;
        mStatusReady = ready;

        mStatusStrip.removeAllViews();
        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.STATUS_LABEL));
        label.setTextSize(13.5f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(BulkTheme.accent(this));
        mStatusStrip.addView(label);

        java.util.List<int[]> entries = statusEntries(numbers, ready, true);
        TextView body = new TextView(this);
        if (entries.isEmpty()) {
            body.setText(BulkStrings.t(this, BulkStrings.STATUS_ALL_EMPTY));
            body.setTextColor(BulkTheme.textMuted(this));
        } else {
            body.setText(statusStripText(entries));
            body.setTextColor(BulkTheme.textMain(this));
        }
        body.setTextSize(13);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        blp.leftMargin = BulkTheme.dp(this, 10);
        body.setLayoutParams(blp);
        mStatusStrip.addView(body);

        TextView caption = new TextView(this);
        caption.setText(BulkStrings.t(this, BulkStrings.STATUS_READY_COUNT));
        caption.setTextSize(10.5f);
        caption.setTextColor(BulkTheme.textMuted(this));
        mStatusStrip.addView(caption);
        BulkFonts.applyToTree(this, mStatusStrip);
    }

    /**
     * v155: Status popup — TAMAM projects owner ki text form
     * me ("Project 1. 6"), ek line me do. Ready hi count hai
     * (Mockups har card par wese hi dikhta hai — owner ka
     * sketch sirf Ready wala tha). Band sirf button se
     * (modalPanel ka X + neeche Close; bahir click band hai).
     */
    private void showStatusPopup() {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            BulkStrings.t(this, BulkStrings.STATUS_LABEL), dialog);
        java.util.List<int[]> all = statusEntries(mStatusNumbers, mStatusReady, false);
        if (all.isEmpty()) {
            TextView none = new TextView(this);
            none.setText(BulkStrings.t(this, BulkStrings.STATUS_ALL_EMPTY));
            none.setTextColor(BulkTheme.textMuted(this));
            none.setTextSize(13);
            panel.addView(none);
        } else {
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            for (java.util.List<int[]> row : statusPairRows(all)) {
                LinearLayout r = new LinearLayout(this);
                r.setOrientation(LinearLayout.HORIZONTAL);
                for (int[] e : row) {
                    TextView cell = new TextView(this);
                    cell.setText("Project " + e[0] + ". " + e[1]);
                    cell.setTextSize(14);
                    cell.setTextColor(BulkTheme.textMain(this));
                    cell.setPadding(0, BulkTheme.dp(this, 5), 0, BulkTheme.dp(this, 5));
                    r.addView(cell, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                }
                if (row.size() == 1) {
                    // Akeli entry wali row me dusra column khaali
                    // rakho — columns hamesha seedh me rahen
                    r.addView(new TextView(this), new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                }
                list.addView(r);
            }
            scroll.addView(list);
            int scrollH = all.size() > 12
                ? (int) (getResources().getDisplayMetrics().heightPixels * 0.45)
                : ViewGroup.LayoutParams.WRAP_CONTENT;
            panel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, scrollH));
        }
        TextView close = BulkTheme.primaryButton(this,
            BulkStrings.t(this, BulkStrings.CLOSE), v -> dialog.dismiss());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = BulkTheme.dp(this, 14);
        close.setLayoutParams(clp);
        panel.addView(close);
        BulkTheme.showPanelDialog(this, panel);
    }

    /**
     * v154 — Remove popup: upar tamam projects ki checkbox
     * list (kitne bhi select karke ek sath Recycle Bin me),
     * neeche USI popup me Recycle Bin ki list — har entry ke
     * sath Restore or Delete Forever. Removal = bin me move
     * (BulkProjectInstaller.removeProjectToBin); permanent
     * delete sirf bin ke andar se hota hai.
     */
    private void showRemoveProjectPicker() {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            BulkStrings.t(this, BulkStrings.REMOVE_PROJECT), dialog);
        final LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        panel.addView(content);

        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            content.removeAllViews();
            final java.util.List<BulkProjects.ProjectInfo> projects =
                new java.util.ArrayList<>();
            for (BulkProjects.ProjectInfo p : loadProjects()) {
                if (p != null && p.number > 0) projects.add(p);
            }
            final java.util.Set<Integer> checked = new java.util.HashSet<>();
            final TextView[] removeBtn = new TextView[1];

            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            for (final BulkProjects.ProjectInfo p : projects) {
                list.addView(removeCheckRow(p, on -> {
                    if (on) checked.add(p.number); else checked.remove(p.number);
                    if (removeBtn[0] != null) {
                        setEnabledStyle(removeBtn[0], !checked.isEmpty());
                    }
                }));
            }
            if (projects.isEmpty()) {
                TextView none = new TextView(this);
                none.setText(BulkStrings.t(this, BulkStrings.NO_PROJECTS));
                none.setTextColor(BulkTheme.textMuted(this));
                none.setTextSize(13);
                list.addView(none);
            }
            scroll.addView(list);
            int scrollH = projects.size() > 5
                ? (int) (getResources().getDisplayMetrics().heightPixels * 0.38)
                : ViewGroup.LayoutParams.WRAP_CONTENT;
            content.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, scrollH));

            TextView remove = new TextView(this);
            remove.setText(BulkStrings.t(this, BulkStrings.REMOVE_SELECTED));
            remove.setTextSize(13);
            remove.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            remove.setTextColor(BulkTheme.danger(this));
            remove.setGravity(Gravity.CENTER);
            remove.setBackground(BulkTheme.dangerBg(this));
            remove.setPadding(BulkTheme.dp(this, 8), BulkTheme.dp(this, 9),
                BulkTheme.dp(this, 8), BulkTheme.dp(this, 9));
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.topMargin = BulkTheme.dp(this, 6);
            remove.setLayoutParams(rLp);
            removeBtn[0] = remove;
            setEnabledStyle(remove, false);
            remove.setOnClickListener(v -> {
                if (checked.isEmpty()) return;
                final java.util.List<BulkProjects.ProjectInfo> sel =
                    new java.util.ArrayList<>();
                StringBuilder names = new StringBuilder();
                for (BulkProjects.ProjectInfo p : projects) {
                    if (checked.contains(p.number)) {
                        sel.add(p);
                        if (names.length() > 0) names.append(", ");
                        names.append(p.name);
                    }
                }
                BulkTheme.showConfirm(this,
                    BulkStrings.t(this, BulkStrings.REMOVE_PROJECT),
                    names + " — Recycle Bin me chale jayenge. TermuxMod ki"
                        + " images mehfooz rahengi.",
                    BulkStrings.t(this, BulkStrings.REMOVE_BTN), true,
                    () -> new Thread(() -> {
                        int moved = 0;
                        for (BulkProjects.ProjectInfo p : sel) {
                            try {
                                BulkProjectInstaller.removeProjectToBin(
                                    this, p.number, p.name);
                                moved++;
                            } catch (Exception e) {
                                Logger.logError(LOG_TAG, "Bin move fail Project "
                                    + p.number + ": " + e.getMessage());
                            }
                        }
                        final int doneCount = moved;
                        runOnUiThread(() -> {
                            Toast.makeText(this, BulkStrings.format(this,
                                BulkStrings.REMOVED_TO_BIN_FMT, doneCount),
                                Toast.LENGTH_SHORT).show();
                            refreshList();
                            rebuild[0].run();
                        });
                    }).start());
            });
            content.addView(remove);

            // ---- Recycle Bin section (usi popup ke andar)
            TextView binTitle = new TextView(this);
            binTitle.setText(BulkStrings.t(this, BulkStrings.RECYCLE_BIN));
            binTitle.setTextSize(13.5f);
            binTitle.setTypeface(Typeface.DEFAULT_BOLD);
            binTitle.setTextColor(BulkTheme.textMain(this));
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bLp.topMargin = BulkTheme.dp(this, 14);
            binTitle.setLayoutParams(bLp);
            content.addView(binTitle);

            java.util.List<Integer> bin = BulkRecycleBin.entries();
            if (bin.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText(BulkStrings.t(this, BulkStrings.BIN_EMPTY));
                empty.setTextColor(BulkTheme.textMuted(this));
                empty.setTextSize(12.5f);
                LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
                eLp.topMargin = BulkTheme.dp(this, 4);
                empty.setLayoutParams(eLp);
                content.addView(empty);
            } else {
                for (final int bn : bin) {
                    content.addView(binEntryRow(bn, rebuild[0]));
                }
            }
        };
        rebuild[0].run();
        BulkTheme.showPanelDialog(this, panel);
    }

    private interface CheckChange { void onChange(boolean on); }

    /** Remove popup ki checkbox wali project row. */
    private LinearLayout removeCheckRow(final BulkProjects.ProjectInfo p,
                                        final CheckChange onChange) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 10), BulkTheme.dp(this, 8),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = BulkTheme.dp(this, 8);
        row.setLayoutParams(lp);
        row.setClickable(true);
        final android.widget.CheckBox cb = new android.widget.CheckBox(this);
        cb.setOnCheckedChangeListener((button, on) -> onChange.onChange(on));
        row.addView(cb);
        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextColor(BulkTheme.textMain(this));
        name.setTextSize(13.5f);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(name, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.setOnClickListener(v -> cb.toggle());
        return row;
    }

    /** Recycle Bin ki ek entry: naam + Restore + Delete Forever. */
    private LinearLayout binEntryRow(final int n, final Runnable rebuild) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 8),
            BulkTheme.dp(this, 8), BulkTheme.dp(this, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = BulkTheme.dp(this, 8);
        row.setLayoutParams(lp);
        TextView name = new TextView(this);
        name.setText(BulkRecycleBin.displayName(n));
        name.setTextColor(BulkTheme.textMain(this));
        name.setTextSize(13.5f);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(name, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        final String entryName = BulkRecycleBin.displayName(n);
        TextView restore = BulkTheme.secondaryButton(this,
            BulkStrings.t(this, BulkStrings.RESTORE), v -> new Thread(() -> {
                boolean ok = BulkRecycleBin.restoreProject(this, n);
                runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(this, entryName + " wapas aa gaya",
                            Toast.LENGTH_SHORT).show();
                    }
                    refreshList();
                    rebuild.run();
                });
            }).start());
        restore.setTextSize(11.5f);
        row.addView(restore);
        TextView del = new TextView(this);
        del.setText(BulkStrings.t(this, BulkStrings.DELETE_FOREVER));
        del.setTextSize(11.5f);
        del.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        del.setTextColor(BulkTheme.danger(this));
        del.setGravity(Gravity.CENTER);
        del.setBackground(BulkTheme.dangerBg(this));
        del.setPadding(BulkTheme.dp(this, 8), BulkTheme.dp(this, 8),
            BulkTheme.dp(this, 8), BulkTheme.dp(this, 8));
        del.setClickable(true);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dLp.leftMargin = BulkTheme.dp(this, 6);
        del.setLayoutParams(dLp);
        del.setOnClickListener(v -> BulkTheme.showConfirm(this,
            BulkStrings.t(this, BulkStrings.DELETE_FOREVER),
            BulkRecycleBin.displayName(n),
            BulkStrings.t(this, BulkStrings.DELETE), true,
            () -> {
                BulkRecycleBin.deleteForever(n);
                rebuild.run();
            }));
        row.addView(del);
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
