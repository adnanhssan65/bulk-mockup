package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Termux Mod — Permissions Terminal (v126, fallback).
 *
 * Permissions screen ke "Terminal" option se khulti hai. Agar permission
 * popups kisi wajah se kaam na karen to yahan har permission/setup
 * command ek row me hai — Run dabane par command asal terminal
 * (TermuxActivity) me chalti hai, output wahi dikhta hai; wapas aa kar
 * agli command chalao. Tartib BulkPermissionCommands ki hai (pehle
 * storage). Command bhejne ka tareeqa wohi hai jo Projects screen ke
 * Run/Batch/Delete ka hai (EXTRA_RUN_COMMAND hand-off).
 */
public class BulkPermissionsTerminalActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(BulkStrings.t(this, BulkStrings.PERM_CMD_TITLE));
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildUi(); // language/theme taaza
    }

    private void buildUi() {
        BulkTheme.applyWindow(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));
        root.addView(BulkTheme.subHeader(this,
            BulkStrings.t(this, BulkStrings.PERM_CMD_TITLE),
            BulkStrings.t(this, BulkStrings.PERM_CMD_SUBTITLE)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 8),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));

        int n = 1;
        for (BulkPermissionCommands.Step step : BulkPermissionCommands.steps()) {
            LinearLayout panel = stepPanel(n, step);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (n > 1) lp.topMargin = BulkTheme.dp(this, 12);
            panel.setLayoutParams(lp);
            body.addView(panel);
            n++;
        }
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private LinearLayout stepPanel(int number, final BulkPermissionCommands.Step step) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(BulkTheme.glassPanel(this));
        panel.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 14),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 14));

        panel.addView(BulkTheme.stepLabel(this,
            BulkStrings.format(this, BulkStrings.STEP_N, number)));

        TextView title = new TextView(this);
        title.setText(BulkStrings.t(this, step.titleKey));
        title.setTextSize(14.5f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(BulkTheme.textMain(this));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.topMargin = BulkTheme.dp(this, 6);
        title.setLayoutParams(tLp);
        panel.addView(title);

        TextView hint = new TextView(this);
        hint.setText(BulkStrings.t(this, step.hintKey));
        hint.setTextSize(12);
        hint.setTextColor(BulkTheme.textSub(this));
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.topMargin = BulkTheme.dp(this, 3);
        hint.setLayoutParams(hLp);
        panel.addView(hint);

        // Command — monospace box (terminal jesi shakal)
        TextView cmd = new TextView(this);
        cmd.setText("$ " + step.command);
        cmd.setTextSize(13);
        cmd.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        cmd.setTextColor(BulkTheme.accent(this));
        cmd.setBackground(BulkTheme.roundedBg(this, BulkTheme.appBg(this), 10,
            BulkTheme.accentBorder(this), 1));
        cmd.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 10),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 10));
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.topMargin = BulkTheme.dp(this, 10);
        cmd.setLayoutParams(cLp);
        panel.addView(cmd);

        TextView run = BulkTheme.primaryButton(this,
            BulkStrings.t(this, BulkStrings.RUN), v -> runInTerminal(step.command));
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.topMargin = BulkTheme.dp(this, 10);
        run.setLayoutParams(rLp);
        panel.addView(run);
        return panel;
    }

    /** Command ko asal terminal me chalao — Projects screen wala hand-off. */
    private void runInTerminal(String command) {
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
