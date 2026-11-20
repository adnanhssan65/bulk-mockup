package com.termux.app;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.shared.logger.Logger;
import com.termux.terminal.TerminalSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Termux Mod — Projects launcher.
 *
 * Drawer ke PROJECTS button se khulta hai. List ~/projects.conf se har dafa
 * fresh parhi jati hai (owner nano se ya Edit button se badal sakta hai).
 * Project ke naam par tap = Start (work command) terminal session me type +
 * Enter — bilkul jese user khud type kare, or project ka apna menu khul
 * jata hai. Delete / Batch ek alag option ke andar hain (owner ki hidayat
 * 2026-11-14: naam par click = sirf Start, baqi alag).
 *
 * projects.conf format (ek project per line):
 *   Name | work_cmd | batch_cmd | delete_cmd
 *   Project 8 | w8 | b8 | d8
 */
public final class ProjectsLauncher {

    private static final String LOG_TAG = "ProjectsLauncher";

    public static final String DEFAULT_CONF = ProjectsConf.defaultContent();

    private ProjectsLauncher() {}

    public static File projectsConfFile() {
        return new File(BulkMockupSetup.homeDir(), "projects.conf");
    }

    public static void seedProjectsConfIfMissing(Context context) throws Exception {
        File dest = projectsConfFile();
        if (dest.exists()) return;
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileOutputStream out = new FileOutputStream(dest)) {
            out.write(DEFAULT_CONF.getBytes(StandardCharsets.UTF_8));
        }
        Logger.logInfo(LOG_TAG, "Seeded default projects.conf");
    }

    /** Dashboard / chooser ke liye — projects.conf ki taaza list. */
    public static List<ProjectsConf.Project> readProjectsList() {
        return readProjects();
    }

    private static List<ProjectsConf.Project> readProjects() {
        try {
            File f = projectsConfFile();
            if (!f.exists()) return new ArrayList<>();
            return ProjectsConf.parse(readConfText());
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "projects.conf parhne me masla: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static String readConfText() {
        try {
            File f = projectsConfFile();
            if (!f.exists()) return DEFAULT_CONF;
            byte[] buf = new byte[(int) f.length()];
            FileInputStream in = new FileInputStream(f);
            int off = 0;
            while (off < buf.length) {
                int r = in.read(buf, off, buf.length - off);
                if (r < 0) break;
                off += r;
            }
            in.close();
            return new String(buf, 0, off, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return DEFAULT_CONF;
        }
    }

    // ------------------------------------------------------------------ UI
    public static void show(final TermuxActivity activity) {
        try {
            seedProjectsConfIfMissing(activity);
        } catch (Exception ignored) {}

        List<ProjectsConf.Project> projects = readProjects();

        int dp = (int) (activity.getResources().getDisplayMetrics().density + 0.5f);
        LinearLayout col = new LinearLayout(activity);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(Color.rgb(5, 8, 5));
        col.setPadding(14 * dp, 10 * dp, 14 * dp, 10 * dp);

        TextView header = new TextView(activity);
        header.setText("PROJECTS");
        header.setTextColor(Color.rgb(0, 255, 65));
        header.setTextSize(17);
        header.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        header.setPadding(0, 0, 0, 8 * dp);
        col.addView(header);

        if (projects.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("List khali hai — Edit list se projects add karo.");
            empty.setTextColor(Color.rgb(255, 215, 64));
            empty.setTextSize(14);
            col.addView(empty);
        }

        final AlertDialog[] holder = new AlertDialog[1];
        for (final ProjectsConf.Project p : projects) {
            // Ek project = ek bara button. Tap = Start (work command) — bas.
            // Neeche wali line me taaza counts (v124): wallpapers / mockups / ready.
            Button start = new Button(activity);
            String label = "▶  " + p.name;
            int projectNumber = BulkProjects.parseProjectNumber(p.name, p.workCmd);
            if (projectNumber > 0) {
                label += "\n" + BulkProjects.countsLineFor(projectNumber);
            }
            start.setText(label);
            start.setTextColor(Color.rgb(0, 255, 65));
            start.setTextSize(16);
            start.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            start.setGravity(Gravity.CENTER_VERTICAL);
            start.setPadding(10 * dp, 12 * dp, 10 * dp, 12 * dp);
            start.setOnClickListener(v -> {
                if (holder[0] != null) holder[0].dismiss();
                runCommand(activity, p.workCmd);
            });
            col.addView(start, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            View line = new View(activity);
            line.setBackgroundColor(Color.rgb(13, 40, 18));
            col.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }

        // Delete / Batch — alag option (owner ki hidayat 2026-11-14)
        if (!projects.isEmpty()) {
            Button manage = new Button(activity);
            manage.setText("Delete / Batch — alag options");
            manage.setTextSize(14);
            manage.setPadding(10 * dp, 10 * dp, 10 * dp, 10 * dp);
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mlp.setMargins(0, 10 * dp, 0, 0);
            manage.setLayoutParams(mlp);
            manage.setOnClickListener(v -> {
                if (holder[0] != null) holder[0].dismiss();
                showManageDialog(activity, projects);
            });
            col.addView(manage);
        }

        TextView hint = new TextView(activity);
        hint.setText("Project par tap = Start — us project ka menu terminal me khul jayega.");
        hint.setTextColor(Color.rgb(139, 196, 147));
        hint.setTextSize(11);
        hint.setPadding(0, 8 * dp, 0, 0);
        col.addView(hint);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(col);

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setView(scroll)
            .setNeutralButton("Edit list", (d, w) -> showEditor(activity))
            .setNegativeButton("Close", null)
            .create();
        holder[0] = dialog;
        dialog.show();
    }

    /** Delete / Batch wali alag screen — har project ke B (batch) or D (delete) buttons. */
    private static void showManageDialog(final TermuxActivity activity, List<ProjectsConf.Project> projects) {
        int dp = (int) (activity.getResources().getDisplayMetrics().density + 0.5f);
        LinearLayout col = new LinearLayout(activity);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(Color.rgb(5, 8, 5));
        col.setPadding(14 * dp, 10 * dp, 14 * dp, 10 * dp);

        TextView header = new TextView(activity);
        header.setText("DELETE / BATCH");
        header.setTextColor(Color.rgb(0, 255, 65));
        header.setTextSize(17);
        header.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        header.setPadding(0, 0, 0, 8 * dp);
        col.addView(header);

        final AlertDialog[] holder = new AlertDialog[1];
        for (final ProjectsConf.Project p : projects) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 5 * dp, 0, 5 * dp);

            TextView name = new TextView(activity);
            name.setText(p.name);
            name.setTextColor(Color.rgb(0, 255, 65));
            name.setTextSize(15);
            name.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            row.addView(cmdButton(activity, "B", dp, v -> {
                if (holder[0] != null) holder[0].dismiss();
                runCommand(activity, p.batchCmd);
            }));
            row.addView(cmdButton(activity, "D", dp, v -> {
                if (holder[0] != null) holder[0].dismiss();
                runCommand(activity, p.deleteCmd);
            }));
            col.addView(row);

            View line = new View(activity);
            line.setBackgroundColor(Color.rgb(13, 40, 18));
            col.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }

        TextView hint = new TextView(activity);
        hint.setText("B = batch · D = delete");
        hint.setTextColor(Color.rgb(139, 196, 147));
        hint.setTextSize(11);
        hint.setPadding(0, 8 * dp, 0, 0);
        col.addView(hint);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(col);

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setView(scroll)
            .setNegativeButton("Close", null)
            .create();
        holder[0] = dialog;
        dialog.show();
    }

    private static Button cmdButton(Context ctx, String label, int dp, View.OnClickListener listener) {
        Button b = new Button(ctx);
        b.setText(label);
        b.setTextSize(15);
        b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        b.setTextColor(Color.rgb(0, 255, 65));
        b.setMinWidth(58 * dp);
        b.setPadding(6 * dp, 10 * dp, 6 * dp, 10 * dp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(3 * dp, 0, 0, 0);
        b.setLayoutParams(lp);
        b.setOnClickListener(listener);
        return b;
    }

    private static void showEditor(final TermuxActivity activity) {
        final EditText edit = new EditText(activity);
        edit.setText(readConfText());
        edit.setTextColor(Color.rgb(0, 255, 65));
        edit.setBackgroundColor(Color.rgb(5, 8, 5));
        edit.setTypeface(Typeface.MONOSPACE);
        edit.setTextSize(13);
        edit.setGravity(Gravity.TOP | Gravity.START);
        edit.setMinLines(12);
        int dp = (int) (activity.getResources().getDisplayMetrics().density + 0.5f);
        edit.setPadding(12 * dp, 10 * dp, 12 * dp, 10 * dp);

        new AlertDialog.Builder(activity)
            .setTitle("Edit projects list")
            .setView(edit)
            .setPositiveButton("Save", (d, w) -> {
                try {
                    File f = projectsConfFile();
                    FileOutputStream out = new FileOutputStream(f);
                    out.write(edit.getText().toString().getBytes(StandardCharsets.UTF_8));
                    out.close();
                    Toast.makeText(activity, "Projects list save ho gayi", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(activity, "Save fail: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private static long sLastSendMs = 0;

    /** Shell busy hai? (shell ke neeche koi process chal raha ho to busy) */
    private static boolean isSessionBusy(TerminalSession session) {
        try {
            if (session == null || !session.isRunning()) return false;
            int pid = session.getPid();
            if (pid <= 0) return false;
            File taskDir = new File("/proc/" + pid + "/task");
            File[] tids = taskDir.listFiles();
            if (tids == null) return false;
            byte[] buf = new byte[256];
            for (File tid : tids) {
                File children = new File(tid, "children");
                if (!children.exists()) continue;
                FileInputStream in = new FileInputStream(children);
                int n = in.read(buf);
                in.close();
                if (n > 0 && new String(buf, 0, n, StandardCharsets.UTF_8).trim().length() > 0) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /** Command ko current session me type + Enter — bilkul jese user khud likhe. */
    static void runCommand(TermuxActivity activity, String command) {
        // Double-tap protection: 800ms ke andar dusri command ignore
        long now = System.currentTimeMillis();
        if (now - sLastSendMs < 800) return;
        sLastSendMs = now;
        try {
            activity.getDrawer().closeDrawers();
        } catch (Exception ignored) {}

        TerminalSession session = activity.getCurrentSession();
        if (session == null) {
            activity.mTermuxTerminalSessionActivityClient.addNewSession(false, null);
            session = activity.getCurrentSession();
            if (session == null) {
                session = activity.mTermuxTerminalSessionActivityClient.getCurrentStoredSessionOrLast();
            }
        }
        if (session == null) {
            Toast.makeText(activity, "Terminal session nahi mila — ek dafa screen pe tap karo", Toast.LENGTH_LONG).show();
            return;
        }
        if (isSessionBusy(session)) {
            Toast.makeText(activity, "Pehla kaam chal raha hai — use khatam hone do. (A task is already running.)", Toast.LENGTH_LONG).show();
            return;
        }
        byte[] bytes = (command + "\n").getBytes(StandardCharsets.UTF_8);
        session.write(bytes, 0, bytes.length);
        try {
            activity.getTerminalView().requestFocus();
        } catch (Exception ignored) {}
    }
}
