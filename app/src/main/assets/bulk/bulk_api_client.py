# Termux Mod built-in API client (termux-media-scan / termux-wake-lock /
# termux-wake-unlock ka replacement — Termux:API app ki zaroorat nahi).
# Ye file app ke pehle setup par $PREFIX/bin me teen naamon se lagti hai;
# __MODE__ aur __SOCK__ setup ke waqt asli values se badal jate hain.
import socket
import sys
import time

MODE = "__MODE__"
SOCK = "__SOCK__"


def talk(lines):
    s = None
    # App service band ho to 2 second me saaf fail — kabhi hang nahi
    for _ in range(10):
        try:
            s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            s.settimeout(30)
            # Abstract namespace socket: naam se pehle \0 lagta hai
            s.connect("\0" + SOCK)
            break
        except OSError:
            try:
                s.close()
            except Exception:
                pass
            s = None
            time.sleep(0.2)
    if s is None:
        print("Termux Mod: app service is not running. "
              "Open the Termux Mod app first.", file=sys.stderr)
        return 1
    try:
        f = s.makefile("rwb")
        for ln in lines:
            f.write((ln + "\n").encode())
            f.flush()
            reply = f.readline().decode().strip()
            if not reply.startswith("OK"):
                print("Termux Mod: " + reply, file=sys.stderr)
                return 1
        return 0
    except OSError as e:
        print("Termux Mod: connection problem: " + str(e), file=sys.stderr)
        return 1
    finally:
        try:
            s.close()
        except Exception:
            pass


def main():
    if MODE == "scan":
        args = sys.argv[1:]
        recursive = False
        if args and args[0] == "-r":
            recursive = True
            args = args[1:]
        if not args:
            print("Usage: termux-media-scan [-r] <path>...", file=sys.stderr)
            return 1
        cmds = [("SCANR " if recursive else "SCAN ") + a for a in args]
        return talk(cmds)
    if MODE == "wakelock":
        return talk(["WAKELOCK"])
    if MODE == "wakeunlock":
        return talk(["WAKEUNLOCK"])
    print("Termux Mod: unknown mode", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
