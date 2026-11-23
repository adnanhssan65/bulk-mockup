package com.termux.app;

import android.app.Activity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Projects");
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildUi(); // counts taaza
    }

    private void buildUi() {
        BulkTheme.applyWindow(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));
        root.addView(BulkTheme.subHeader(this, "Projects", "Tap a project for Run, Batch & Delete"));

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 8), BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));

        List<BulkProjects.ProjectInfo> projects = loadProjects();
        if (projects.isEmpty()) {
            body.addView(emptyState());
        } else {
            if (sExpandedNumber == Integer.MIN_VALUE && projects.get(0).number > 0) {
                sExpandedNumber = projects.get(0).number; // design: pehla project khula hua
            }
            for (BulkProjects.ProjectInfo p : projects) {
                LinearLayout panel = projectPanel(p);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, BulkTheme.dp(this, 12));
                panel.setLayoutParams(lp);
                body.addView(panel);
            }
        }
        scroll.addView(body);
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
        plusText.setText("Add New Project (ZIP)");
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
    }

    private LinearLayout emptyState() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setBackground(BulkTheme.glassPanel(this));
        panel.setPadding(BulkTheme.dp(this, 24), BulkTheme.dp(this, 24), BulkTheme.dp(this, 24), BulkTheme.dp(this, 24));
        TextView title = new TextView(this);
        title.setText("No Projects Installed");
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(BulkTheme.textMain(this));
        title.setGravity(Gravity.CENTER);
        panel.addView(title);
        TextView sub = new TextView(this);
        sub.setText("Tap \"Add New Project (ZIP)\" below to install a template.");
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
            buildUi();
        });

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

            TextView run = BulkTheme.primaryButton(this, "Run", v -> runInTerminal(p.workCmd));
            run.setTextSize(12);
            setEnabledStyle(run, p.workCmd != null);
            actions.addView(run, actionLp(0));

            TextView batch = BulkTheme.secondaryButton(this, "Batch", v -> runInTerminal(p.batchCmd));
            batch.setTextSize(12);
            setEnabledStyle(batch, p.batchCmd != null);
            actions.addView(batch, actionLp(BulkTheme.dp(this, 8)));

            TextView delete = new TextView(this);
            delete.setText("Delete");
            delete.setTextSize(12);
            delete.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            delete.setTextColor(BulkTheme.danger(this));
            delete.setGravity(Gravity.CENTER);
            delete.setBackground(BulkTheme.dangerBg(this));
            delete.setPadding(BulkTheme.dp(this, 8), BulkTheme.dp(this, 8), BulkTheme.dp(this, 8), BulkTheme.dp(this, 8));
            delete.setClickable(true);
            delete.setOnClickListener(v -> runInTerminal(p.deleteCmd));
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
