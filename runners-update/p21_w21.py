# ===================== PROJECT 21 : MOTION PHOTO MAKER =====================
# Input: VIDEO (kisi bhi lambai ki) -> Output: Android Motion Photo (EK hi JPG — video usi ke andar)
#          + 1 Mockup (motion photo ke darmiyan wale frame par lockscreen design)
# Motion Photo = Google format: JPG me XMP (GCamera:MotionPhoto + Container directory)
#          aur file ke aakhir me video ke darmiyan wale 3 second ki clip — CRF 0 LOSSLESS encode,
#          quality me zara sa bhi farq nahi. Android gallery ise Live Photo samajhti hai.
# Koi sawal nahi poochta — process bilkul automatic hai. Sirf ffmpeg lazmi hai.
import os, sys, time, shutil, subprocess, gc, re, struct
from datetime import datetime
from PIL import Image, ImageFile
ImageFile.LOAD_TRUNCATED_IMAGES = True
from tqdm import tqdm

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# --- MULTI-COLORS ---
C1 = '\033[95m'; C2 = '\033[91m'; C3 = '\033[93m'; C4 = '\033[92m'
C5 = '\033[96m'; C6 = '\033[94m'; ENDC = '\033[0m'; BOLD = '\033[1m'
HG = '\033[92m'; HG2 = '\033[32m'; HC = '\033[96m'; HR = '\033[91m'; HY = '\033[93m'
HL = '\u2500'; HD = '\u2550'

P_NUM = sys.argv[2] if len(sys.argv) > 2 else "21"
B = os.environ.get("MOCKUP_BASE", os.path.dirname(os.path.abspath(__file__)))
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")
T_F = "/storage/emulated/0/.mockup_time_tracker.txt"
TEMPLATE = os.path.join(B, "template.png")
SCREEN_MASK = os.path.join(B, "screen_mask.png")
SCREEN = (90, 114, 990, 1806)   # template ke andar screen ka rect (make_template se match)
VID_EXTS = ('.mp4', '.mov', '.m4v')
LIVE_SECONDS = 3.0              # Motion Photo ki clip lambai (video ke darmiyan wale 3 second)

def display_header():
    print(f"{HG2}{HL * 56}{ENDC}")
    for line in ("Initializing secure shell........", "Loading live photo engine........",
                 "Mounting mockup engine...........", "Verifying integrity checksum....."):
        print(f"{HG} [BOOT]{ENDC} {line} {HC}[OK]{ENDC}")
    print(f"{HG2}{HL * 56}{ENDC}")
    print(f" {HG2}\u2554{HD * 49}\u2557{ENDC}")
    print(f" {HG2}\u2551{HY} \u26a1 Project : {P_NUM} (Live Photo){ENDC}{' ' * max(0, 27 - len(P_NUM))}{HG2}\u2551{ENDC}")
    print(f" {HG2}\u2551{HY} \u26a1 Owner   : Mehar Adnan{ENDC}{' ' * 24}{HG2}\u2551{ENDC}")
    print(f" {HG2}\u2551{HY} \u26a1 Contact : +44 7404 032136{ENDC}{' ' * 20}{HG2}\u2551{ENDC}")
    print(f" {HG2}\u255a{HD * 49}\u255d{ENDC}")
    print(f" {HR}[ ACCESS GRANTED ]{ENDC} {HG}\u2014 Live Photo Edition \u2705{ENDC}\n")

def set_wakelock(enable=True):
    try:
        if enable: os.system("termux-wake-lock >/dev/null 2>&1")
        else: os.system("termux-wake-unlock >/dev/null 2>&1")
    except Exception:
        pass

# --- SCAN (family standard) ---
# Har file save hote hi foran, ONE BY ONE aur usi order me scan (pehle mockup, phir JPG).
def safe_scan(path, timeout_sec=5):
    try:
        subprocess.run(["termux-media-scan", path], stdout=subprocess.DEVNULL,
                       stderr=subprocess.DEVNULL, timeout=timeout_sec)
    except Exception:
        pass

def safe_scan_dir(path, timeout_sec=20):
    try:
        subprocess.run(["termux-media-scan", "-r", path], stdout=subprocess.DEVNULL,
                       stderr=subprocess.DEVNULL, timeout=timeout_sec)
    except Exception:
        pass

# --- Gallery copies (TermuxMod) — set ki tartib me ---
# Pehli file (mockup) Mockups me, dusri file (Motion Photo JPG) Ready Wallpapers me —
# har copy par usi file ka tracker time + foran scan.
# Folder mojood na ho to khamoshi se skip — folder banana app ka kaam hai.
TM_ROOT = "/storage/emulated/0/TermuxMod"
def gallery_copy_outputs(items):
    try:
        proj = os.path.join(TM_ROOT, f"Project {P_NUM}")
        dm = os.path.join(proj, "Mockups")
        dr = os.path.join(proj, "Ready Wallpapers")
        if not (os.path.isdir(dm) and os.path.isdir(dr)):
            return
        for i, (src, t) in enumerate(items):
            d = os.path.join(dm if i == 0 else dr, os.path.basename(src))
            shutil.copyfile(src, d)
            os.utime(d, (t, t))
            safe_scan(d)
    except Exception:
        pass

# --- ffmpeg check (sab se pehle, saaf bilingual paigham — traceback nahi) ---
def ffmpeg_available():
    if shutil.which("ffmpeg"):
        return True
    try:
        import imageio_ffmpeg  # noqa
        return True
    except Exception:
        return False

FFMPEG_MSG = (f" {C2}\u2716 ffmpeg nahi mila! Motion Photo banane ke liye ffmpeg lazmi hai.\n"
              f"   Pehle Termux me ye command chalao:  pkg install -y ffmpeg\n"
              f"   (ffmpeg not found \u2014 install it first, then run again.){ENDC}")

# --- Motion Photo builder (Google format: XMP + JPG ke aakhir me MP4) ---
def ffmpeg_exe():
    e = shutil.which("ffmpeg")
    if e:
        return e
    import imageio_ffmpeg
    return imageio_ffmpeg.get_ffmpeg_exe()

def _ffrun(cmd):
    p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return p.returncode, p.stdout, p.stderr.decode("utf-8", "replace")

def probe_duration(path):
    """Video ki lambai (seconds) — `ffmpeg -i` ke output se."""
    _, _, err = _ffrun([ffmpeg_exe(), "-hide_banner", "-i", path])
    m = re.search(r"Duration:\s*(\d+):(\d+):([\d.]+)", err)
    if not m:
        return 0.0
    return int(m.group(1)) * 3600 + int(m.group(2)) * 60 + float(m.group(3))

_PIX_FMTS = ("yuvj420p", "yuv420p", "yuvj422p", "yuv422p", "yuvj444p", "yuv444p",
             "yuv420p10le", "yuv422p10le", "yuv444p10le")

def probe_pix_fmt(path):
    """Source ka pixel format (lossless encode usi format me hoga). Default yuv420p."""
    _, _, err = _ffrun([ffmpeg_exe(), "-hide_banner", "-i", path])
    for line in err.splitlines():
        if " Video: " in line:
            for pf in _PIX_FMTS:
                if pf in line:
                    return pf
    return "yuv420p"

def make_clip(src, start, dur, out_mp4):
    """Darmiyan wali ~3s clip — libx264 CRF 0 (mathematically LOSSLESS, quality bilkul asal jesi).
    Preset sirf size/speed ke liye hai; CRF 0 par quality hamesha lossless rehti hai."""
    cmd = [ffmpeg_exe(), "-y", "-v", "error"]
    if start:
        cmd += ["-ss", f"{start:.4f}"]
    cmd += ["-i", src]
    if dur:
        cmd += ["-t", f"{dur:.4f}"]
    cmd += ["-map", "0:v:0", "-an", "-c:v", "libx264", "-crf", "0",
            "-preset", "fast", "-pix_fmt", probe_pix_fmt(src),
            "-f", "mp4", out_mp4]
    rc, _, err = _ffrun(cmd)
    if rc != 0 or not os.path.exists(out_mp4):
        raise RuntimeError("clip encode failed: " + err[-400:])

def extract_still_jpeg(src, t, out_jpg):
    """Time t wala frame — full resolution JPEG (q100, 4:4:4), bilkul pehle wali key photo jesa."""
    vf = "scale=in_color_matrix=auto:in_range=auto:out_range=full,format=rgb24"
    rc, png, err = _ffrun([ffmpeg_exe(), "-v", "error", "-ss", f"{t:.4f}", "-i", src,
                           "-frames:v", "1", "-vf", vf,
                           "-f", "image2pipe", "-vcodec", "png", "-"])
    if rc != 0 or not png:
        raise RuntimeError("frame extract failed: " + err[-400:])
    import io
    with Image.open(io.BytesIO(png)) as im:
        im.convert("RGB").save(out_jpg, "JPEG", quality=100, subsampling=0, optimize=True)

def motion_xmp(video_len):
    """Google Motion Photo XMP: GCamera tags + Container directory (byte-exact lengths ke sath).
    MicroVideoOffset = file ke aakhir se video ki shuruaat tak ke bytes = video ki lambai
    (video file ke bilkul aakhir me hai, uske baad kuch nahi)."""
    xmp = (
        '<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="MotionPhoto">\n'
        ' <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">\n'
        '  <rdf:Description rdf:about=""'
        ' xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"'
        ' xmlns:Container="http://ns.google.com/photos/1.0/container/"'
        ' xmlns:Item="http://ns.google.com/photos/1.0/container/item/"'
        ' GCamera:MotionPhoto="1"'
        ' GCamera:MotionPhotoVersion="1"'
        f' GCamera:MicroVideoOffset="{video_len}">\n'
        '   <Container:Directory>\n'
        '    <rdf:Seq>\n'
        '     <rdf:li rdf:parseType="Resource">\n'
        '      <Container:Item Item:Mime="image/jpeg" Item:Semantic="Primary" Item:Length="0" Item:Padding="0"/>\n'
        '     </rdf:li>\n'
        '     <rdf:li rdf:parseType="Resource">\n'
        f'      <Container:Item Item:Mime="video/mp4" Item:Semantic="MotionPhoto" Item:Length="{video_len}" Item:Padding="0"/>\n'
        '     </rdf:li>\n'
        '    </rdf:Seq>\n'
        '   </Container:Directory>\n'
        '  </rdf:Description>\n'
        ' </rdf:RDF>\n'
        '</x:xmpmeta>'
    )
    return xmp.encode("utf-8")

XMP_HEADER = b"http://ns.adobe.com/xap/1.0/\x00"  # 29 bytes — standard XMP APP1 pehchan

def assemble_motion_photo(still_jpg, clip_mp4, out_path):
    """Still JPEG me XMP (APP1) ghusao aur aakhir me MP4 chipka do = ek Motion Photo JPG."""
    with open(clip_mp4, "rb") as fh:
        video = fh.read()
    with open(still_jpg, "rb") as fh:
        jpeg = fh.read()
    if jpeg[:2] != b"\xff\xd8":
        raise RuntimeError("still JPEG nahi hai")
    xmp = motion_xmp(len(video))
    payload = XMP_HEADER + xmp
    app1 = b"\xff\xe1" + struct.pack(">H", len(payload) + 2) + payload
    with open(out_path, "wb") as fh:
        fh.write(jpeg[:2])   # SOI
        fh.write(app1)       # XMP segment
        fh.write(jpeg[2:])   # asal JPEG (EOI tak)
        fh.write(video)      # embedded MP4 — file ke bilkul aakhir me

def get_max_seq(dirs):
    mx = 0
    for d in dirs:
        if os.path.isdir(d):
            for f in os.listdir(d):
                head = f.split('.')[0]
                if head.isdigit():
                    mx = max(mx, int(head))
    return mx

def list_inputs():
    if not os.path.isdir(I):
        return []
    out = [f for f in os.listdir(I)
           if os.path.isfile(os.path.join(I, f)) and f.lower().endswith(VID_EXTS)
           and "_temp" not in f]
    out.sort(key=lambda f: os.path.getctime(os.path.join(I, f)))
    return out

def build_mockup(key_jpg, out_path):
    """Live Photo ki key photo (darmiyan wala frame) ko lockscreen template me lagao."""
    tpl = Image.open(TEMPLATE).convert("RGBA")
    mask_full = Image.open(SCREEN_MASK).convert("L")
    mask = mask_full.crop(SCREEN)
    sw, sh = SCREEN[2] - SCREEN[0], SCREEN[3] - SCREEN[1]
    with Image.open(key_jpg) as im:
        frame = im.convert("RGB")
    # cover-fit: screen ko poora bharo, darmiyan se crop
    scale = max(sw / frame.width, sh / frame.height)
    nw, nh = int(frame.width * scale + 0.5), int(frame.height * scale + 0.5)
    frame = frame.resize((nw, nh), Image.LANCZOS)
    x = (nw - sw) // 2; y = (nh - sh) // 2
    frame = frame.crop((x, y, x + sw, y + sh))
    canvas = Image.new("RGB", (1080, 1920), (0, 0, 0))
    canvas.paste(frame, (SCREEN[0], SCREEN[1]), mask)
    canvas.paste(tpl, (0, 0), tpl)
    canvas.save(out_path, quality=95)

def process_one(f, idx, t_now):
    raw_p = os.path.join(I, f)
    total = probe_duration(raw_p)
    if total <= 0:
        raise RuntimeError("video ki lambai nahi parhi gayi")
    # Clip = video ke darmiyan wale 3 second (lossless CRF 0); key photo bilkul beech wala frame (1.5s par)
    if total > LIVE_SECONDS + 0.05:
        start = max(0.0, total / 2 - LIVE_SECONDS / 2)
        dur, still_abs = LIVE_SECONDS, start + LIVE_SECONDS / 2
    else:
        start, dur, still_abs = 0.0, None, total / 2  # chhoti video poori, key photo uske beech me
    stage = os.path.join(B, f".lp_stage_{idx}")
    if os.path.isdir(stage):
        shutil.rmtree(stage, ignore_errors=True)
    os.makedirs(stage, exist_ok=True)
    try:
        clip_mp4 = os.path.join(stage, "clip.mp4")
        still_jpg = os.path.join(stage, "still.jpg")
        make_clip(raw_p, start, dur, clip_mp4)
        extract_still_jpeg(raw_p, still_abs, still_jpg)
        mock_path = os.path.join(O, f"{idx}.jpg")
        ready_jpg = os.path.join(R, f"{idx + 1}.JPG")
        build_mockup(still_jpg, mock_path)
        assemble_motion_photo(still_jpg, clip_mp4, ready_jpg)
    finally:
        shutil.rmtree(stage, ignore_errors=True)
    mt, wt = t_now + 60, t_now + 120
    os.utime(mock_path, (mt, mt))
    os.utime(ready_jpg, (wt, wt))
    for p in (mock_path, ready_jpg):   # save verify
        if not os.path.exists(p) or os.path.getsize(p) < 100:
            raise IOError("save verify failed: " + p)
    safe_scan(mock_path); safe_scan(ready_jpg)
    gallery_copy_outputs([(mock_path, mt), (ready_jpg, wt)])
    try: os.remove(raw_p)
    except Exception: pass
    return idx + 2, wt

action = sys.argv[1] if len(sys.argv) >= 2 else 'process'

if action == 'process':
    display_header()
    if not ffmpeg_available():
        print(FFMPEG_MSG)
        sys.exit()
    if not (os.path.exists(TEMPLATE) and os.path.exists(SCREEN_MASK)):
        print(f" {C2}\u2716 Template file missing: template.png / screen_mask.png{ENDC}"); sys.exit()
    os.makedirs(O, exist_ok=True); os.makedirs(R, exist_ok=True)
    if not os.path.isdir(I):
        print(f" {C2}\u2716 Input folder nahi mila: {P_NUM}_wallpapers{ENDC}"); sys.exit()
    vids = list_inputs()
    if not vids:
        print(f" {C2}\u2716 {P_NUM}_wallpapers me koi video (.mp4/.mov/.m4v) nahi mili.{ENDC}"); sys.exit()
    print(f" {C4}\u2714 Motion Photo engine ready! (ffmpeg mil gaya){ENDC}")
    s_idx = get_max_seq([O, R, W_H]) + 1
    cur_t = time.time()
    if os.path.exists(T_F):
        try:
            with open(T_F, "r") as fh:
                lt = float(fh.read().strip())
                if lt > cur_t: cur_t = lt
        except Exception:
            pass
    done, skip_log = 0, []
    set_wakelock(True)
    for f in tqdm(vids, desc="\U0001f3ac Live Photo ", bar_format=f" {C1}{{desc}}{{bar:20}}{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"):
        try:
            s_idx, cur_t = process_one(f, s_idx, cur_t)
            done += 1
        except Exception as e:
            skip_log.append(f"{f} ({type(e).__name__}: {str(e)[:60]})")
        gc.collect()
    set_wakelock(False)
    if done > 0:
        try:
            with open(T_F, "w") as fh: fh.write(str(cur_t))
        except Exception:
            pass
        print(f"\n {C4}\u2714 {done} Motion Photo + Mockup ready! (har video: 1 mockup + 1 Motion Photo JPG){ENDC}")
    else:
        print(f"\n {C2}\u2716 Koi Motion Photo nahi bani.{ENDC}")
    if skip_log:
        print(f" {C2}\u26a0 {len(skip_log)} skip (video reh gayi, agle run me retry):{ENDC}")
        for s in skip_log[:8]:
            print(f"   {C2}- {s}{ENDC}")
    print("")

if action == 'del':
    BACKUP = f"/storage/emulated/0/.mockup_backup_{P_NUM}"
    def _files(d):
        if not os.path.isdir(d): return []
        return [f for f in os.listdir(d) if os.path.isfile(os.path.join(d, f))]
    def _backup_and_clear(d, tag):
        files = _files(d)
        if not files:
            print(f" {C5}\u2139 {tag}: pehle se khaali hai.{ENDC}"); return 0
        bd = os.path.join(BACKUP, tag)
        if os.path.isdir(bd): shutil.rmtree(bd, ignore_errors=True)
        os.makedirs(bd, exist_ok=True)
        n = 0
        for f in files:
            try:
                shutil.copy2(os.path.join(d, f), os.path.join(bd, f))
                os.remove(os.path.join(d, f)); n += 1
            except Exception: pass
        print(f" {C4}\u2714 {tag}: {n} files delete (backup ban gaya).{ENDC}")
        return n
    display_header()
    print(f" {C5}\u273f {BOLD}D e l e t e  M e n u{ENDC}")
    print(f" {C1}[1]{ENDC} Tayaar Maal (Mockups + Ready Wallpapers)")
    print(f" {C1}[2]{ENDC} Sirf Mockups")
    print(f" {C1}[3]{ENDC} Input Wallpapers")
    print(f" {C1}[4]{ENDC} Warehouse (Ready_Upload_Batches)")
    print(f" {C1}[5]{ENDC} Backup se Wapas lao (Restore)")
    print(f" {C1}[0]{ENDC} Cancel")
    ch = input(f"\n {C4}\u279c {ENDC}").strip()
    if ch == '1':
        _backup_and_clear(O, "Mockups"); _backup_and_clear(R, "Ready")
    elif ch == '2':
        _backup_and_clear(O, "Mockups")
    elif ch == '3':
        _backup_and_clear(I, "Input")
    elif ch == '4':
        if os.path.isdir(W_H) and os.listdir(W_H):
            bd = os.path.join(BACKUP, "Batches")
            if os.path.isdir(bd): shutil.rmtree(bd, ignore_errors=True)
            try:
                shutil.copytree(W_H, bd)
                for name in os.listdir(W_H):
                    p = os.path.join(W_H, name)
                    if os.path.isdir(p): shutil.rmtree(p, ignore_errors=True)
                    else: os.remove(p)
                print(f" {C4}\u2714 Warehouse saaf (backup ban gaya).{ENDC}")
            except Exception as e:
                print(f" {C2}\u2716 Warehouse masla: {e}{ENDC}")
        else:
            print(f" {C5}\u2139 Warehouse pehle se khaali hai.{ENDC}")
    elif ch == '5':
        if not os.path.isdir(BACKUP):
            print(f" {C2}\u2716 Koi backup nahi mila.{ENDC}")
        else:
            total = 0
            for tag, d in (("Mockups", O), ("Ready", R), ("Input", I)):
                bd = os.path.join(BACKUP, tag)
                if not os.path.isdir(bd): continue
                os.makedirs(d, exist_ok=True)
                for f in os.listdir(bd):
                    src = os.path.join(bd, f); dst = os.path.join(d, f)
                    if os.path.isfile(src) and not os.path.exists(dst):
                        try: shutil.move(src, dst); total += 1
                        except Exception: pass
            bd = os.path.join(BACKUP, "Batches")
            if os.path.isdir(bd):
                os.makedirs(W_H, exist_ok=True)
                for name in os.listdir(bd):
                    src = os.path.join(bd, name); dst = os.path.join(W_H, name)
                    if not os.path.exists(dst):
                        try: shutil.move(src, dst); total += 1
                        except Exception: pass
            if total: print(f" {C4}\u2714 Restore ho gaya: {total} items wapas.{ENDC}")
            else: print(f" {C5}\u2139 Wapas lane ko kuch nahi (ya sab pehle se mojood hai).{ENDC}")
    else:
        print(f" {C5}\u2139 Cancel.{ENDC}")
    try: safe_scan_dir(B)
    except Exception: pass
    print("")

if action == 'batch':
    display_header()
    files = []
    for d in (O, R):
        if os.path.isdir(d):
            files += [(os.path.join(d, f), f) for f in os.listdir(d) if os.path.isfile(os.path.join(d, f))]
    if not files:
        print(f" {C2}\u2716 Batch ke liye Mockups/Ready me kuch nahi hai.{ENDC}")
    else:
        os.makedirs(W_H, exist_ok=True)
        bname = "Batch_" + datetime.now().strftime("%d-%b_%I-%M%p")
        bd = os.path.join(W_H, bname)
        os.makedirs(bd, exist_ok=True)
        n = 0
        for src, f in files:
            try: shutil.copy2(src, os.path.join(bd, f)); n += 1
            except Exception: pass
        print(f" {C4}\u2714 {bname}: {n} files copy ho gayi (Ready_Upload_Batches).{ENDC}")
        try: safe_scan_dir(B)
        except Exception: pass
    print("")
