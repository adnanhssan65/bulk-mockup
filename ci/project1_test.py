#!/usr/bin/env python3
"""
Bulk Mockup — Project 1 real-world test driver (Override H).

termux-docker (aarch64) container ke andar chalta hai. Driver khud container
ke stock python par chalta hai; test honay wale scripts (--sut-python) us
prefix ke python se chalte hain jis ka test ho raha hai:
  --label stock   -> /data/data/com.termux/files/usr/bin/python
                     (Bulk Mockup ka package bhi com.termux hi hai)

Har check ek line print hota hai:  CHECK <name> PASS|FAIL <detail>
Aakhir me manifest (output hashes) JSON likha jata hai parity compare ke liye.
"""
import argparse
import hashlib
import json
import os
import resource
import shutil
import subprocess
import sys
import time

from PIL import Image

STORAGE = "/storage/emulated/0"
PROJECT = os.path.join(STORAGE, "mockup_project_1")
TRACKER = os.path.join(STORAGE, ".mockup_time_tracker.txt")
HEAL = os.path.join(STORAGE, ".mockup_gallery_heal_1.done")
BACKUP_DIR = os.path.join(STORAGE, ".mockup_backup_1")

RESULTS = []


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond), detail))
    print(f"CHECK {name} {'PASS' if cond else 'FAIL'} {detail}", flush=True)
    return cond


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


# ---------------------------------------------------------------- inputs

def big_photo_image():
    """6000x8000 photo-like image (smooth gradients + texture + halki noise).
    Pure random noise jaan bujh kar nahi — wo JPEG ke liye pathological
    worst case hai, asal wallpaper photo jesa kabhi nahi hota."""
    import numpy as np
    rng = np.random.default_rng(11)
    small = rng.integers(0, 255, (500, 375, 3), dtype=np.uint8)
    tex = Image.fromarray(small, "RGB").resize((6000, 8000), Image.LANCZOS)
    arr = np.asarray(tex).astype(np.int16)
    yy = np.linspace(0, 120, 8000, dtype=np.int16)[:, None, None]
    arr = np.clip(arr // 2 + 64 + yy, 0, 255).astype(np.uint8)
    return Image.fromarray(arr, "RGB")


def gen_inputs(dest):
    """9 test inputs, creation order hi process order hai (ctime sort)."""
    import numpy as np
    os.makedirs(dest, exist_ok=True)
    made = []

    def save(img, name, **kw):
        p = os.path.join(dest, name)
        img.save(p, **kw)
        made.append(name)
        time.sleep(0.06)

    def gradient(w, h, c1, c2):
        arr = np.zeros((h, w, 3), dtype=np.uint8)
        for x in range(3):
            arr[:, :, x] = np.linspace(c1[x], c2[x], w, dtype=np.uint8)[None, :, ] \
                if False else np.tile(np.linspace(c1[x], c2[x], w).astype(np.uint8), (h, 1))
        return Image.fromarray(arr, "RGB")

    save(gradient(900, 1600, (200, 60, 60), (40, 40, 180)), "01_portrait.jpg", quality=90)
    save(gradient(1920, 1080, (30, 160, 90), (240, 220, 120)), "02_landscape.png")
    save(gradient(1000, 1000, (120, 40, 160), (250, 180, 60)), "03_square.webp", quality=88)
    rgba = gradient(800, 1400, (80, 80, 80), (230, 230, 230)).convert("RGBA")
    px = rgba.load()
    for y in range(0, 700):
        for x in range(0, 400):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 0)
    save(rgba, "04_alpha.png")
    # corrupt file — jaan boojh kar kharab, beech me rakha hai
    with open(os.path.join(dest, "05_corrupt.jpg"), "wb") as f:
        f.write(b"this is not a jpeg at all \x00\x01\x02" * 40)
    made.append("05_corrupt.jpg")
    time.sleep(0.06)
    save(gradient(90, 160, (255, 120, 0), (0, 120, 255)), "06_tiny.jpg", quality=85)
    save(gradient(720, 1280, (60, 200, 160), (200, 60, 120)), "07_portrait2.jpg", quality=90)
    save(gradient(1600, 900, (90, 90, 200), (240, 160, 40)), "08_land2.jpg", quality=90)
    save(big_photo_image(), "09_big.jpg", quality=88)
    return made


def gen_big(dest):
    os.makedirs(dest, exist_ok=True)
    big_photo_image().save(os.path.join(dest, "01_big.jpg"), quality=88)


def gen_many(dest, n=20):
    os.makedirs(dest, exist_ok=True)
    names = []
    for i in range(n):
        img = Image.new("RGB", (600, 1000), (30 + i * 9 % 200, 80, 160))
        name = f"{i + 1:02d}_img.jpg"
        img.save(os.path.join(dest, name), quality=88)
        names.append(name)
        time.sleep(0.04)
    return names


# ---------------------------------------------------------------- project reset

class Env:
    def __init__(self, args):
        self.label = args.label
        self.sut_python = args.sut_python
        self.sut_bin = os.path.dirname(args.sut_python)
        self.fixtures = args.fixtures
        self.work = args.work

    def run_env(self):
        env = dict(os.environ)
        env["PATH"] = self.sut_bin + ":" + env.get("PATH", "")
        return env

    def run_script(self, script, action, stdin_text, timeout=900):
        cmd = [self.sut_python, os.path.join(PROJECT, script), action, "1"]
        t0 = time.time()
        proc = subprocess.Popen(cmd, cwd=PROJECT, env=self.run_env(),
                                stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, text=True)
        try:
            out, _ = proc.communicate(input=stdin_text, timeout=timeout)
            timed_out = False
        except subprocess.TimeoutExpired:
            proc.kill()
            out, _ = proc.communicate()
            timed_out = True
        secs = time.time() - t0
        peak_kb = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
        tail = "\n".join((out or "").splitlines()[-6:])
        try:
            logdir = os.path.join(self.work, "logs")
            os.makedirs(logdir, exist_ok=True)
            stamp = f"{script.replace('.py', '')}_{action}_{int(t0 * 1000) % 100000000}.log"
            with open(os.path.join(logdir, stamp), "w") as lf:
                lf.write(out or "")
        except Exception:
            pass
        return proc.returncode, out, secs, peak_kb, timed_out, tail


def reset_project(fixtures, input_names, inputs_src):
    shutil.rmtree(PROJECT, ignore_errors=True)
    shutil.rmtree(BACKUP_DIR, ignore_errors=True)
    for f in (TRACKER, HEAL):
        try:
            os.remove(f)
        except FileNotFoundError:
            pass
    shutil.copytree(fixtures, PROJECT)
    # inputs exact order me (ctime = copy order) — fixture ke protected
    # files (hidden + mp4) pehle se 1_wallpapers me hain
    inp_dir = os.path.join(PROJECT, "1_wallpapers")
    os.makedirs(inp_dir, exist_ok=True)
    for name in input_names:
        shutil.copy2(os.path.join(inputs_src, name), os.path.join(inp_dir, name))
        time.sleep(0.06)


def listing(folder):
    if not os.path.isdir(folder):
        return []
    return sorted(os.listdir(folder))


def numbered(folder):
    out = []
    for f in listing(folder):
        stem, ext = os.path.splitext(f)
        if stem.isdigit() and ext.lower() == ".jpg":
            out.append(int(stem))
    return sorted(out)


def protected_hashes():
    inp = os.path.join(PROJECT, "1_wallpapers")
    res = {}
    for f in os.listdir(inp):
        if f.startswith(".") or f.lower().endswith(".mp4"):
            res[f] = sha256(os.path.join(inp, f))
    return res


# ---------------------------------------------------------------- case: process

def verify_process_run(env, mode_label, expect_inputs, manifest_out):
    O = os.path.join(PROJECT, "1_Mockups")
    R = os.path.join(PROJECT, "1_Ready_Wallpapers")
    mks, rds = numbered(O), numbered(R)
    ok_count = len(expect_inputs)
    check(f"{mode_label}.pairs", len(mks) == ok_count and len(rds) == ok_count,
          f"mockups={mks} ready={rds}")
    # numbering: mock k, ready k+1, step 2
    seq_ok = True
    if mks and rds:
        pairs = list(zip(mks, rds))
        seq_ok = all(r == m + 1 for m, r in pairs) and \
            all(mks[i + 1] - mks[i] == 2 for i in range(len(mks) - 1))
    check(f"{mode_label}.numbering_step2", seq_ok, f"pairs={list(zip(mks, rds))}")
    # sizes + JPEG + ICC + mtime ladder
    with Image.open(os.path.join(PROJECT, "mockup.png")) as im:
        mock_size = im.size
    size_ok, icc_ok, open_ok = True, True, True
    mtimes = {}
    for folder, tag in ((O, "m"), (R, "r")):
        for n in numbered(folder):
            p = os.path.join(folder, f"{n}.jpg")
            mtimes[n] = os.path.getmtime(p)
            try:
                with Image.open(p) as im:
                    im.verify()
                with Image.open(p) as im:
                    want = mock_size if tag == "m" else (1080, 1920)
                    if im.size != want:
                        size_ok = False
                    if not im.info.get("icc_profile"):
                        icc_ok = False
            except Exception:
                open_ok = False
    check(f"{mode_label}.sizes", size_ok, f"mockup={mock_size} ready=1080x1920")
    check(f"{mode_label}.jpeg_open", open_ok)
    check(f"{mode_label}.icc_embedded", icc_ok)
    ordered = [mtimes[n] for n in sorted(mtimes)]
    check(f"{mode_label}.mtime_strictly_increasing",
          all(ordered[i] < ordered[i + 1] for i in range(len(ordered) - 1)),
          f"count={len(ordered)}")
    # manifest for parity
    for folder in (O, R):
        for n in numbered(folder):
            p = os.path.join(folder, f"{n}.jpg")
            manifest_out[os.path.relpath(p, PROJECT)] = sha256(p)
    return mks, rds


def case_process(env, inputs_src, input_names, mode, mode_label):
    valid = [n for n in input_names if n != "05_corrupt.jpg"]
    reset_project(env.fixtures, input_names, inputs_src)
    before = protected_hashes()
    manifest = {}
    rc, out, secs, peak, timed_out, tail = env.run_script("adnan_core.py", "process", mode + "\n2\n")  # v127: dusra jawab = Size (2 = Crop 9:16, taake ready 1080x1920 hi rahe)
    check(f"{mode_label}.exit0", rc == 0 and not timed_out, f"rc={rc} secs={secs:.1f}\n{tail}")
    verify_process_run(env, mode_label, valid, manifest)
    inp = os.path.join(PROJECT, "1_wallpapers")
    remaining = set(os.listdir(inp))
    check(f"{mode_label}.inputs_removed",
          all(v not in remaining for v in valid), f"remaining={sorted(remaining)}")
    check(f"{mode_label}.corrupt_skipped_not_deleted", "05_corrupt.jpg" in remaining)
    after = protected_hashes()
    check(f"{mode_label}.hidden_and_mp4_untouched", before == after,
          f"protected={list(before)}")
    print(f"INFO {mode_label} time={secs:.1f}s processed={len(valid)} "
          f"per_image={secs / max(1, len(valid)):.2f}s peak_rss_mb={peak / 1024:.0f}")
    return manifest


def case_batch(env):
    O = os.path.join(PROJECT, "1_Mockups")
    R = os.path.join(PROJECT, "1_Ready_Wallpapers")
    W = os.path.join(PROJECT, "Ready_Upload_Batches")
    before_mtimes = {}
    for folder in (O, R):
        for n in numbered(folder):
            before_mtimes[f"{n}.jpg"] = os.path.getmtime(os.path.join(folder, f"{n}.jpg"))
    rc, out, secs, peak, timed_out, tail = env.run_script("adnan_core.py", "batch", "")
    check("batch.exit0", rc == 0 and not timed_out, f"rc={rc}\n{tail}")
    batches = [d for d in listing(W) if d.startswith("P1_Batch_")]
    check("batch.folder", batches == ["P1_Batch_1"], f"batches={batches}")
    if batches:
        cb = os.path.join(W, batches[0])
        files = {f: os.path.getmtime(os.path.join(cb, f)) for f in listing(cb)}
        check("batch.all_files_moved", set(files) == set(before_mtimes),
              f"batch={len(files)} expected={len(before_mtimes)}")
        check("batch.mtimes_preserved",
              all(abs(files[k] - before_mtimes[k]) < 0.01 for k in files))
    check("batch.sources_empty", numbered(O) == [] and numbered(R) == [])


def case_del_adnan(env, inputs_src):
    # corrupt file apna skip-test kar chuki — del checks se pehle use hata do
    try:
        os.remove(os.path.join(PROJECT, "1_wallpapers", "05_corrupt.jpg"))
    except FileNotFoundError:
        pass
    O = os.path.join(PROJECT, "1_Mockups")
    R = os.path.join(PROJECT, "1_Ready_Wallpapers")
    W = os.path.join(PROJECT, "Ready_Upload_Batches")

    def run_del(stdin):
        return env.run_script("adnan_core.py", "del", stdin)

    # warehouse batch delete + restore
    rc, *_rest = run_del("4\n1\n")
    batch_gone = not os.path.isdir(os.path.join(W, "P1_Batch_1"))
    bk_files = []
    if os.path.isdir(BACKUP_DIR):
        for r, d, fs in os.walk(BACKUP_DIR):
            bk_files += [f for f in fs if f != ".nomedia"]
    check("del4.batch_to_backup", batch_gone and len(bk_files) > 0,
          f"backup_files={len(bk_files)}")
    rc, *_ = run_del("5\n")
    check("del5.restore_batch",
          os.path.isdir(os.path.join(W, "P1_Batch_1"))
          and len(listing(os.path.join(W, "P1_Batch_1"))) == len(bk_files))

    # fresh outputs: process 2 images
    two = gen_many(os.path.join(env.work, "inputs_two"), 2)
    reset_project_keep(env, inputs_src)
    # reset_project_keep ke bajaye: mojooda project me sirf inputs daalo
    inp = os.path.join(PROJECT, "1_wallpapers")
    for n in two:
        shutil.copy2(os.path.join(env.work, "inputs_two", n), os.path.join(inp, n))
        time.sleep(0.05)
    rc, out, *_ = env.run_script("adnan_core.py", "process", "1\n")
    check("delsetup.process2", numbered(O) != [] and numbered(R) != [],
          f"O={numbered(O)} R={numbered(R)}")
    n_out = len(numbered(O))

    rc, *_ = run_del("2\n")
    check("del2.mockups_only", numbered(O) == [] and len(numbered(R)) == n_out,
          f"O={numbered(O)} R={numbered(R)}")
    rc, *_ = run_del("5\n")
    check("del5.restore_mockups", len(numbered(O)) == n_out)
    rc, *_ = run_del("1\n")
    check("del1.both", numbered(O) == [] and numbered(R) == [])
    rc, *_ = run_del("5\n")
    check("del5.restore_both", len(numbered(O)) == n_out and len(numbered(R)) == n_out)
    # input delete
    for n in two:
        shutil.copy2(os.path.join(env.work, "inputs_two", n),
                     os.path.join(inp, n))
    rc, *_ = run_del("3\n")
    left_inputs = [f for f in listing(inp) if not f.startswith(".")
                   and not f.lower().endswith(".mp4")]
    check("del3.inputs", left_inputs == [], f"left={left_inputs}")
    # cancel
    before_state = (numbered(O), numbered(R))
    rc, *_ = run_del("0\n")
    check("del0.cancel_noop", (numbered(O), numbered(R)) == before_state)


def reset_project_keep(env, inputs_src):
    """Batch/restore ke baad project ko saaf suthra karo lekin warehouse rehne do —
    yahan sirf O/R/inputs khali karne hain, is liye alag halka reset."""
    for folder in ("1_Mockups", "1_Ready_Wallpapers"):
        d = os.path.join(PROJECT, folder)
        shutil.rmtree(d, ignore_errors=True)
        os.makedirs(d, exist_ok=True)


def case_system_core(env, inputs_src, input_names):
    four = [n for n in input_names if n != "05_corrupt.jpg"][:4]
    # system_core ke inputs: chhoti set (uski apni listing order hai — ctime nahi)
    reset_project(env.fixtures, four, inputs_src)
    rc, out, secs, peak, timed_out, tail = env.run_script("system_core.py", "process", "1\n")
    O = os.path.join(PROJECT, "1_Mockups")
    R = os.path.join(PROJECT, "1_Ready_Wallpapers")
    check("sys.process", rc == 0 and len(numbered(O)) == 4 and len(numbered(R)) == 4,
          f"rc={rc} O={numbered(O)} R={numbered(R)}\n{tail}")
    rc, *_ = env.run_script("system_core.py", "del", "1\n")
    check("sys.del1_ready", numbered(O) == [] and numbered(R) == [])
    # dobara process + batch + warehouse delete
    reset_project(env.fixtures, four, inputs_src)
    rc, *_ = env.run_script("system_core.py", "process", "2\n")
    rc, *_ = env.run_script("system_core.py", "batch", "")
    W = os.path.join(PROJECT, "Ready_Upload_Batches")
    batches = [d for d in listing(W) if "Batch_" in d]
    check("sys.batch", len(batches) == 1 and
          len(listing(os.path.join(W, batches[0]))) == 8 if batches else False,
          f"batches={batches}")
    rc, *_ = env.run_script("system_core.py", "del", "3\nA\n")
    left = [d for d in listing(W) if "Batch_" in d]
    check("sys.del3_warehouse_all", left == [], f"left={left}")
    rc, *_ = env.run_script("system_core.py", "del", "0\n")
    check("sys.del0_cancel", rc == 0)


def case_perf_big(env):
    big_src = os.path.join(env.work, "inputs_big")
    reset_project(env.fixtures, ["01_big.jpg"], big_src)
    t0 = time.time()
    rc, out, secs, peak, timed_out, tail = env.run_script(
        "adnan_core.py", "process", "1\n", timeout=1800)
    dt = time.time() - t0
    O = os.path.join(PROJECT, "1_Mockups")
    ok = rc == 0 and numbered(O) != []
    check("perf.big_6000x8000", ok, f"rc={rc} time={dt:.1f}s peak_rss_mb={peak / 1024:.0f}")
    print(f"PERF big_image time_s={dt:.1f} peak_rss_mb={peak / 1024:.0f} "
          f"phone_budget_mb=1536 close={'YES' if peak / 1024 > 1200 else 'NO'}")


def case_stress20(env):
    src = os.path.join(env.work, "inputs_20")
    names = gen_many(src, 20)
    reset_project(env.fixtures, names, src)
    rc, out, secs, peak, timed_out, tail = env.run_script(
        "adnan_core.py", "process", "1\n", timeout=1200)
    O = os.path.join(PROJECT, "1_Mockups")
    check("stress.20_images", rc == 0 and len(numbered(O)) == 20,
          f"rc={rc} outputs={len(numbered(O))} time={secs:.1f}s peak_rss_mb={peak / 1024:.0f}")
    print(f"PERF stress20 time_s={secs:.1f} peak_rss_mb={peak / 1024:.0f}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--label", required=True)
    ap.add_argument("--sut-python", required=True)
    ap.add_argument("--fixtures", required=True)
    ap.add_argument("--work", required=True)
    args = ap.parse_args()
    env = Env(args)
    os.makedirs(args.work, exist_ok=True)
    os.makedirs(STORAGE, exist_ok=True)

    inputs_src = os.path.join(args.work, "inputs_main")
    input_names = gen_inputs(inputs_src)
    gen_big(os.path.join(args.work, "inputs_big"))

    man_hd = case_process(env, inputs_src, input_names, "1", "adnan_hd")
    case_batch(env)
    case_del_adnan(env, inputs_src)
    man_norm = case_process(env, inputs_src, input_names, "2", "adnan_normal")
    case_system_core(env, inputs_src, input_names)
    case_perf_big(env)
    case_stress20(env)

    with open(os.path.join(args.work, f"manifest_{args.label}.json"), "w") as f:
        json.dump({"hd": man_hd, "normal": man_norm}, f, indent=1, sort_keys=True)

    failed = [r for r in RESULTS if not r[1]]
    print(f"SUMMARY {args.label}: {len(RESULTS) - len(failed)}/{len(RESULTS)} passed")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
