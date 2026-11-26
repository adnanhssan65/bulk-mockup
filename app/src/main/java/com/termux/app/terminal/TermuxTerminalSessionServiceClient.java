package com.termux.app.terminal;

import android.app.Service;

import androidx.annotation.NonNull;

import com.termux.app.BulkRunHelper;
import com.termux.app.TermuxService;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

/** The {@link TerminalSessionClient} implementation that may require a {@link Service} for its interface methods. */
public class TermuxTerminalSessionServiceClient extends TermuxTerminalSessionClientBase {

    private static final String LOG_TAG = "TermuxTerminalSessionServiceClient";

    private final TermuxService mService;

    public TermuxTerminalSessionServiceClient(TermuxService service) {
        this.mService = service;
    }

    /**
     * Termux Mod v153: base class ki onSessionFinished khaali
     * hai — activity band na ho to khatam hone wali khabar
     * kahin nahi jati. App-launched run (BulkRun- nishan) agar
     * TermuxActivity ke bind hone se PEHLE hi safai se
     * (exit 0) khatam ho jaye, to v152 wala auto-return
     * activity client tak kabhi nahi pohanchta (us waqt
     * session ka client YEHI hota hai) or terminal khula reh
     * jata. Aisi surat me khatam hone ka record service me
     * rakh do — bind hote hi activity client wahi auto-return
     * kar dega. Hath ke sessions is me kabhi nahi ate.
     */
    @Override
    public void onSessionFinished(@NonNull TerminalSession finishedSession) {
        if (BulkRunHelper.isRunSessionName(finishedSession.mSessionName)
            && finishedSession.getExitStatus() == 0) {
            mService.noteBulkRunSessionFinishedBeforeBind(finishedSession);
        }
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
        TermuxSession termuxSession = mService.getTermuxSessionForTerminalSession(terminalSession);
        if (termuxSession != null)
            termuxSession.getExecutionCommand().mPid = pid;
    }

}
