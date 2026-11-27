import os, sys, time, shutil, subprocess, random, threading, gc
from PIL import Image, ImageFile
ImageFile.LOAD_TRUNCATED_IMAGES = True  # Halki truncated files ko galat "corrupt" na samjhe
import PIL.ImageCms as cms
from tqdm import tqdm
import cv2, numpy as np

# --- MULTI-COLORS (Neon/Aesthetic Gradient) ---
C1 = '\033[95m' # Magenta
C2 = '\033[91m' # Red
C3 = '\033[93m' # Yellow
C4 = '\033[92m' # Green
C5 = '\033[96m' # Cyan
C6 = '\033[94m' # Blue
ENDC = '\033[0m'
BOLD = '\033[1m'

# --- AESTHETIC QUOTES ---
QUOTES = [
    "Create your own sunshine.",
    "Small steps every day.",
    "Keep going. Keep growing.",
    "Aesthetic is not just a look, it's a vibe.",
    "Coding with a touch of elegance.",
    "Transform reality with code."
]

# 🛡️ HACKER TERMINAL PREMIUM HEADER 🛡️
HG  = '\033[92m'   # Hacker Green (bright)
HG2 = '\033[32m'   # Hacker Green (dim)
HC  = '\033[96m'   # Cyan accent
HR  = '\033[91m'   # Red alert accent
HY  = '\033[93m'   # Yellow accent

def display_header():
    os.system('clear')
    quote = random.choice(QUOTES)

    # ⌨️ Fake Boot Sequence ⌨️
    print(f"{HG2}{'─' * 56}{ENDC}")
    boot_lines = [
        "Initializing secure shell........",
        "Loading encryption modules.......",
        "Mounting mockup engine...........",
        "Verifying integrity checksum.....",
    ]
    for line in boot_lines:
        print(f"{HG} [BOOT]{ENDC} {line} {HC}[OK]{ENDC}")
        time.sleep(0)
    print(f"{HG2}{'─' * 56}{ENDC}\n")

    # 💻 GRADIENT ADNAN (Matrix Green) 💻
    ascii_art = [
        r"   █████╗ ██████╗ ███╗   ██╗█████╗ ███╗   ██╗",
        r"  ██╔══██╗██╔══██╗████╗  ██║██╔══██╗████╗  ██║",
        r"  ███████║██║  ██║██╔██╗ ██║███████║██╔██╗ ██║",
        r"  ██╔══██║██║  ██║██║╚██╗██║██╔══██║██║╚██╗██║",
        r"  ██║  ██║██████╔╝██║ ╚████║██║  ██║██║ ╚████║",
        r"  ╚═╝  ╚═╝╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═╝╚═╝  ╚═══╝"
    ]
    for line in ascii_art:
        print(f"{HG}{BOLD}{line}{ENDC}")

    # 🔓 Small Lock/Computer Icon 🔓
    print(f"\n{HC}      ┌──────────────────────────┐")
    print(f"      │   💻  R O O T   M O D E    │")
    print(f"      │   🔓  A C C E S S   O K    │")
    print(f"      └──────────────────────────┘{ENDC}\n")

    # 🌸 Flower + Welcome Mix 🌸
    print(f" {HG}🌸 ⚡ 🌸   W e l c o m e   t o   t h e   S y s t e m   🌸 ⚡ 🌸{ENDC}\n")

    # 🔐 Secure Info Box (auto-sized, no manual alignment errors) 🔐
    project_line = "Project : 1"
    owner_line   = "Owner   : Mehar Adnan"
    contact_line = "Contact : +44 7404 032136"
    pad = 46
    prefix = " ⚡ "
    box_w = len(prefix) + pad

    print(f" {HG2}╔{'═' * box_w}╗{ENDC}")
    print(f" {HG2}║{HY}{prefix}{project_line:<{pad}}{HG2}║{ENDC}")
    print(f" {HG2}║{HY}{prefix}{quote:<{pad}}{HG2}║{ENDC}")
    print(f" {HG2}║{HY}{prefix}{owner_line:<{pad}}{HG2}║{ENDC}")
    print(f" {HG2}║{HY}{prefix}{contact_line:<{pad}}{HG2}║{ENDC}")
    print(f" {HG2}╚{'═' * box_w}╝{ENDC}")

    # 🛡️ Access Granted Footer 🛡️
    print(f"\n {HR}🛡️  [ ACCESS GRANTED ]{ENDC} {HG}— Session Encrypted ✅{ENDC}")
    print(f" {C1}⚡{C3}✦{C5}⚡{C3}✦{C1}⚡  {BOLD}{HG}P R E M I U M   E D I T I O N{ENDC}  {C1}⚡{C3}✦{C5}⚡{C3}✦{C1}⚡{ENDC}\n")

def animated_spinner(text, duration=0.8):
    frames = ['•', 'o', 'O', 'o']
    for i in range(int(duration * 10)):
        sys.stdout.write(f"\r {C1}✿{ENDC} {C5}{frames[i % len(frames)]}{ENDC} {text}...")
        sys.stdout.flush()
        time.sleep(0)
    sys.stdout.write(f"\r {C4}✔{ENDC} {text} Completed!       \n")

def set_wakelock(enable=True):
    # Fire-and-forget: app ke session-less process me ye command atak
    # sakti hai — run ko is ke jawab ka intezar kabhi nahi karna.
    try:
        subprocess.Popen(["termux-wake-lock" if enable else "termux-wake-unlock"],
                         stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except: pass

_WAQFA = None
def _waqfa_on():
    # Scan waqfa (v149): gallery copy ke scan ke baad chhota sa gap, taake
    # MediaStore ka record sahi tartib me lage. Marker TermuxMod/.scan_waqfa
    # me "off" (ya "0") ho to waqfa band; marker na ho ya kuch aur ho to ON.
    # Marker ek run me sirf ek dafa parha jata hai.
    global _WAQFA
    if _WAQFA is None:
        _WAQFA = True
        try:
            with open("/storage/emulated/0/TermuxMod/.scan_waqfa") as _f:
                if _f.read().strip().lower() in ("off", "0"):
                    _WAQFA = False
        except Exception:
            pass
    return _WAQFA

def safe_scan(path, timeout_sec=5):
    try:
        subprocess.run(["termux-media-scan", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)
    except subprocess.TimeoutExpired:
        pass
    except Exception:
        pass
    if _waqfa_on() and "/TermuxMod/Project " in str(path):
        time.sleep(0.25)

# --- Gallery copies (TermuxMod) — jori ki tartib me ---
# Run ke sath hi har jori ko TermuxMod/Project N ke EK hi "Pairs" folder me
# laga do: pehle mockup (odd number), phir usi ka wallpaper (even number),
# har copy ke foran baad scan. Naam, mtime aur scan — teeno ki tartib ek hi
# rehti hai, is liye gallery/TikTok/Threads har jagah jori sahi lagti hai.
# Project folder mojood na ho to khamoshi se skip — folder banana app ka kaam hai.
TM_ROOT = "/storage/emulated/0/TermuxMod"
def gallery_pair_copy(mock_path, wall_path, mt, wt):
    try:
        proj = os.path.join(TM_ROOT, f"Project {P_NUM}")
        if not os.path.isdir(proj):
            return
        dp = os.path.join(proj, "Pairs")
        if not os.path.isdir(dp):
            os.makedirs(dp, exist_ok=True)
        import shutil
        dm_p = os.path.join(dp, os.path.basename(mock_path))
        shutil.copyfile(mock_path, dm_p)
        os.utime(dm_p, (mt, mt))
        safe_scan(dm_p)
        dr_p = os.path.join(dp, os.path.basename(wall_path))
        shutil.copyfile(wall_path, dr_p)
        os.utime(dr_p, (wt, wt))
        safe_scan(dr_p)
    except Exception:
        pass


# --- Batch ka naam (app popup se) ---
# App Batch dabane par popup me naam poochta hai aur wo naam engine
# folder ki ".batch_name" file me likh kar bhejta hai; file ho to batch
# folder usi naam se banta hai, warna purana auto naam. Batch ka flow
# bilkul purana hi hai - sirf folder ka naam badalta hai.
def _batch_pick_name(auto_name):
    try:
        nf = os.path.join(B, ".batch_name")
        if os.path.isfile(nf):
            with open(nf) as fh:
                nm = fh.read().strip()
            try: os.remove(nf)
            except Exception: pass
            nm = nm.replace("/", "_").replace("\\", "_").strip().strip(".")
            if nm:
                return nm[:60]
    except Exception:
        pass
    return auto_name

def safe_scan_dir(path, timeout_sec=20):
    # Bulk/cleanup ke liye ek dafa poore folder ko scan karta hai (order matter nahi karta yahan)
    try:
        subprocess.run(["termux-media-scan", "-r", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)
    except subprocess.TimeoutExpired:
        pass
    except Exception:
        pass

# Optional plugins: agar installed hon to HEIC/AVIF bhi khul jayengi (na hon to koi masla nahi)
try:
    import pillow_heif
    pillow_heif.register_heif_opener()
except Exception:
    pass
try:
    import pillow_avif  # noqa
except Exception:
    pass

READ_ERR = {}  # file path -> asli wajah (skip message mein dikhane ke liye)

def sniff_type(path):
    # File ke andar asal mein kya hai (naam .jpg ho tab bhi)
    try:
        with open(path, 'rb') as fh:
            h = fh.read(32)
    except Exception as e:
        return f"open fail: {type(e).__name__}"
    if len(h) == 0: return "khali file (0 bytes)"
    if h[:3] == b'\xff\xd8\xff': return "jpeg"
    if h[:8] == b'\x89PNG\r\n\x1a\n': return "png"
    if h[:4] == b'RIFF' and h[8:12] == b'WEBP': return "webp"
    if h[4:8] == b'ftyp': return "heif/avif:" + h[8:12].decode('latin1')
    if h[:5].lower() == b'<html' or h[:9].lower() == b'<!doctype': return "html (download fail)"
    return "unknown:" + h[:8].hex()

def safe_imread(path):
    errs = []
    flags = cv2.IMREAD_COLOR | getattr(cv2, 'IMREAD_IGNORE_ORIENTATION', 0)
    # 1) PIL pehle - bilkul original tareeqa (har type ki image isi se khulti thi)
    try:
        with Image.open(path) as im:
            im.load()
            rgb = im.convert('RGB')
        return np.array(rgb)[:, :, ::-1].copy()  # RGB -> BGR (cv2 format)
    except Exception as e:
        errs.append(f"PIL {type(e).__name__}: {str(e)[:40]}")
    # 2) Backup: cv2 direct
    try:
        img = cv2.imread(path, flags)
        if img is not None and img.size > 0:
            return img
        errs.append("cv2 None")
    except Exception as e:
        errs.append(f"cv2 {type(e).__name__}")
    # 3) Backup: cv2 imdecode
    try:
        img = cv2.imdecode(np.fromfile(path, dtype=np.uint8), flags)
        if img is not None and img.size > 0:
            return img
    except Exception as e:
        errs.append(f"imdecode {type(e).__name__}")
    # 4) ffmpeg (agar Termux mein installed ho) - aakhri koshish
    try:
        if shutil.which('ffmpeg'):
            tmp_png = os.path.join(B, ".ff_tmp_read.png")
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", path, tmp_png],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=30)
            img = cv2.imread(tmp_png, cv2.IMREAD_COLOR)
            try: os.remove(tmp_png)
            except: pass
            if img is not None and img.size > 0:
                return img
            errs.append("ffmpeg fail")
    except Exception as e:
        errs.append(f"ffmpeg {type(e).__name__}")
    READ_ERR[path] = f"{sniff_type(path)} | " + "; ".join(errs)
    return None

# Threads: agar phone garam ho ya koi masla aaye to THREADS = 0 kar do (purana behavior).
THREADS = max(1, min(2, os.cpu_count() or 1))
cv2.setNumThreads(THREADS)

# sRGB profile ek hi dafa banta hai (pehle har save pe dobara banta tha)
try:
    _SRGB_BYTES = cms.ImageCmsProfile(cms.createProfile('sRGB')).tobytes()
except Exception:
    _SRGB_BYTES = None

P_NUM = sys.argv[2] if len(sys.argv) > 2 else "1"
B = os.environ.get("MOCKUP_BASE", os.path.dirname(os.path.abspath(__file__)))
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")
T_F = "/storage/emulated/0/.mockup_time_tracker.txt"
M_P, S_P = os.path.join(B, "mockup.png"), os.path.join(B, "shape.png")

# --- GALLERY REPAIR / BACKUP LOCATION ---
# Backup ab project folder ke BAHAR hai (hidden + .nomedia), taake project folder ke andar kabhi .nomedia na aaye
BK_DIR = f"/storage/emulated/0/.mockup_backup_{P_NUM}"
LEGACY_BK = os.path.join(B, ".last_delete_backup")   # purani jagah (project folder ke andar)
HEAL_MARK = f"/storage/emulated/0/.mockup_gallery_heal_{P_NUM}.done"
_gone_paths = []

def bulk_scan_gone():
    # Move/delete hue purane paths gallery se hatao (delete mein order matter nahi karta)
    global _gone_paths
    paths, _gone_paths = _gone_paths, []
    for i in range(0, len(paths), 50):
        try:
            subprocess.run(["termux-media-scan"] + paths[i:i+50], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=30)
        except Exception:
            pass

def ghost_remove(path, scan=True):
    # Project 3 wali ghost-buster trick: file ko pehle TRASH se overwrite, phir delete, phir scan
    # => gallery mein "File not found" wali ghost entry nahi bachti
    try:
        with open(path, 'wb') as fh: fh.write(b"TRASH")
    except Exception:
        pass
    try:
        os.remove(path)
    except Exception:
        pass
    if scan:
        safe_scan(path)

def move_clean(src, dst_dir):
    # Copy (mtime wahi rehta hai) + original ko ghost_remove. Copy fail ho to original chhua nahi jata.
    dst = os.path.join(dst_dir, os.path.basename(src))
    shutil.copy2(src, dst)
    if os.path.getsize(dst) != os.path.getsize(src):
        raise IOError("copy size mismatch")
    ghost_remove(src, scan=False)
    return dst

def migrate_legacy_backup():
    # Purana backup (project folder ke andar, .nomedia ke saath) bahar nayi jagah le jao
    if not os.path.isdir(LEGACY_BK): return 0
    os.makedirs(BK_DIR, exist_ok=True)
    try:
        with open(os.path.join(BK_DIR, ".nomedia"), "a"): pass
    except Exception:
        pass
    moved = 0; failed = 0
    for r, ds, fs in os.walk(LEGACY_BK):
        for f in fs:
            if f == '.nomedia': continue
            src = os.path.join(r, f)
            dst = os.path.join(BK_DIR, os.path.relpath(src, LEGACY_BK))
            try:
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                if os.path.exists(dst):
                    base, ext = os.path.splitext(dst); k = 1
                    while os.path.exists(f"{base}_{k}{ext}"): k += 1
                    dst = f"{base}_{k}{ext}"
                shutil.move(src, dst); moved += 1
            except Exception:
                failed += 1
    if failed == 0:
        shutil.rmtree(LEGACY_BK, ignore_errors=True)
    return moved

def heal_gallery(force=False):
    # 1) project folders ke andar koi .nomedia ho to hata do  2) ek dafa poora folder gallery ke liye dobara scan
    if not os.path.isdir(B): return
    migrate_legacy_backup()
    check = [B, I, O, R, W_H]
    if os.path.isdir(W_H):
        try: check += [os.path.join(W_H, d) for d in os.listdir(W_H) if os.path.isdir(os.path.join(W_H, d))]
        except Exception: pass
    removed = []
    for d in check:
        pth = os.path.join(d, ".nomedia")
        if os.path.isfile(pth):
            try: os.remove(pth); removed.append(pth)
            except Exception: pass
    first_time = not os.path.exists(HEAL_MARK)
    if removed or first_time or force:
        display_header()
        print(f" {C5}\u273f Gallery repair: project folder dobara scan ho raha hai (thora waqt lagega)...{ENDC}")
        if removed: print(f" {C3}  .nomedia mila aur hata diya: {len(removed)}{ENDC}")
        safe_scan_dir(B, timeout_sec=120)
        try:
            with open(HEAL_MARK, "w") as fh: fh.write("1")
        except Exception: pass
        if force: print(f" {C4}\u2714 Gallery repair mukammal.{ENDC}\n")

def get_max_seq(dirs):
    m = 0
    for d in dirs:
        if os.path.exists(d):
            for r, sd, fs in os.walk(d):
                for f in fs:
                    n, _ = os.path.splitext(f)
                    if n.isdigit(): m = max(m, int(n))
    return m

def resize_exact(img, target_w=1080, target_h=1920):
    h, w = img.shape[:2]
    r = w/h
    target_ratio = target_w / target_h
    if r > target_ratio:
        nw = int(h * target_ratio)
        img = img[:, (w-nw)//2 : (w-nw)//2 + nw]
    else:
        nh = int(w / target_ratio)
        img = img[(h-nh)//2 : (h-nh)//2 + nh, :]
    return cv2.resize(img, (target_w, target_h), interpolation=cv2.INTER_LANCZOS4)

def apply_hd_enhance(img):
    lab = cv2.cvtColor(img, cv2.COLOR_BGR2LAB)
    l, a, b = cv2.split(lab)

    # Luminance noise reduction (detail-preserving, edge-aware)
    l_denoised = cv2.bilateralFilter(l, d=5, sigmaColor=30, sigmaSpace=30)

    # Color/chroma noise reduction — light touch, strong blur here smears
    # color across edges on already-low-quality source images
    a_denoised = cv2.GaussianBlur(a, (0, 0), 0.8)
    b_denoised = cv2.GaussianBlur(b, (0, 0), 0.8)

    # Moderate two-pass sharpening on luminance only — strong sharpening
    # amplifies compression noise on low quality inputs instead of detail
    blur1 = cv2.GaussianBlur(l_denoised, (0, 0), 1.0)
    sharp1 = cv2.addWeighted(l_denoised, 1.35, blur1, -0.35, 0)
    blur2 = cv2.GaussianBlur(sharp1, (0, 0), 2.5)
    sharp2 = cv2.addWeighted(sharp1, 1.15, blur2, -0.15, 0)

    l_final = np.clip(sharp2, 0, 255).astype(np.uint8)
    return cv2.cvtColor(cv2.merge([l_final, a_denoised, b_denoised]), cv2.COLOR_LAB2BGR)

def save_optimized(cv2_img, output_path):
    rgb = cv2.cvtColor(cv2_img, cv2.COLOR_BGR2RGB)
    pil_img = Image.fromarray(rgb)
    try:
        if _SRGB_BYTES is None: raise ValueError("no icc")
        pil_img.save(output_path, format='JPEG', quality=87, subsampling=0, icc_profile=_SRGB_BYTES, optimize=True)
    except:
        pil_img.save(output_path, format='JPEG', quality=87, subsampling=0, optimize=True)

def order_points(pts):
    rect = np.zeros((4, 2), dtype="float32")
    s = pts.sum(axis=1); rect[0] = pts[np.argmin(s)]; rect[2] = pts[np.argmax(s)]
    d = np.diff(pts, axis=1); rect[1] = pts[np.argmin(d)]; rect[3] = pts[np.argmax(d)]
    return rect

def load_rgba(path):
    img = cv2.imread(path, cv2.IMREAD_UNCHANGED)
    if img is None: return None
    if len(img.shape)==2: img=cv2.cvtColor(img,cv2.COLOR_GRAY2BGRA)
    elif img.shape[2]==3: img=np.concatenate([img, np.full((img.shape[0],img.shape[1],1),255,dtype=np.uint8)],axis=2)
    return img

if len(sys.argv) < 2: sys.exit()
action = sys.argv[1]
if action == 'fix':
    heal_gallery(force=True)
    sys.exit()
if action in ('process', 'batch', 'del'):
    heal_gallery()

if action == 'process':
    display_header()
    os.makedirs(O, exist_ok=True); os.makedirs(R, exist_ok=True)

    imgs = [f for f in os.listdir(I) if f.lower().endswith(('.jpg','.jpeg','.png','.webp','.avif','.heic')) and not f.startswith('.') and "_temp" not in f]
    imgs.sort(key=lambda f: os.path.getctime(os.path.join(I, f)))  # order files arrived in folder = selection order
    if len(imgs) == 0:
        print(f" {C2}✖ Koi tasveer nahi mili.{ENDC}")
        sys.exit()

    print(f" {C5}✿ {BOLD}S e l e c t  M o d e{ENDC}")
    print(f" {C1}[1]{ENDC} HD Mode (Galaxy AI Enhance) - {C4}[DEFAULT]{ENDC}")
    print(f" {C1}[2]{ENDC} Normal (Fast KB Pack)")

    choice = input(f"\n {C4}➜ {ENDC}").strip()
    is_hd = (choice != '2')

    print(f" {C5}✿ {BOLD}S e l e c t  S i z e{ENDC}")
    print(f" {C1}[1]{ENDC} Original Size - {C4}[DEFAULT]{ENDC}")
    print(f" {C1}[2]{ENDC} Crop 9:16")

    size_choice = input(f"\n {C4}➜ {ENDC}").strip()
    crop_916 = (size_choice == '2')

    if not os.path.exists(M_P) or not os.path.exists(S_P):
        print(f" {C2}✖ Mockup files missing!{ENDC}")
        sys.exit()

    def _setup_mockup():
        mk, sh = load_rgba(M_P), load_rgba(S_P)
        hm, wm = mk.shape[:2]
        sh = cv2.resize(sh, (wm, hm))
        mk_mask = (sh[:,:,3] > 0).astype(np.uint8) * 255
        cnts, _ = cv2.findContours(mk_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        d = order_points(cv2.boxPoints(cv2.minAreaRect(max(cnts, key=cv2.contourArea))).astype(np.float32))
        pm = cv2.getPerspectiveTransform(np.float32([[0,0],[wm,0],[wm,hm],[0,hm]]), d)
        mb, ma = mk[:,:,:3].astype(np.float32), mk[:,:,3].astype(np.float32)/255.0
        # Pehle se tayaar: har image pe dobara nahi nikalna padega (result bilkul wahi)
        m_pre = mb * ma[:, :, None]          # m_a * mockup
        m_inv = (1.0 - ma)[:, :, None]       # (1 - m_a)
        return hm, wm, mk_mask, pm, m_pre, m_inv

    _result = {}
    _t = threading.Thread(target=lambda: _result.update(data=_setup_mockup()))
    _t.start()
    _frames = ['•', 'o', 'O', 'o']
    _i = 0
    while _t.is_alive():
        sys.stdout.write(f"\r {C1}✿{ENDC} {C5}{_frames[_i % 4]}{ENDC} Loading mockup engine...")
        sys.stdout.flush()
        time.sleep(0.08)
        _i += 1
    _t.join()
    sys.stdout.write(f"\r {C4}✔{ENDC} Mockup engine ready!            \n")
    H_M, W_M, mask, M, m_pre, m_inv = _result['data']

    s_idx = get_max_seq([O, R, W_H]) + 1
    # --- Time ladder PAST me anchored (2026-11-27, phone test ke baad) ---
    # Purana tareeqa cur_t ko abhi/future se shuru karke har file par +60s
    # barhata tha — mtimes ghanton future me chale jate thay. Phone par
    # saabit hua: Gallery/TikTok future dates bhi theek parhte hain, lekin
    # Threads future-dated files ki tartib bigaar deta hai (system future
    # dates ko clamp/tie kar deta hai); past-dated files har jagah perfect
    # order me aati hain. Is liye ladder ab hamesha PAST me rehti hai:
    #   * Run ki sab se nayi mtime ka nishana = abhi se ~60s pehle.
    #   * Tracker me PAST value ho to nayi ladder us ke BAAD shuru hoti
    #     hai (runs ki aapas ki tartib bani rehti hai) — step window ke
    #     hisaab se shrink ho jata hai, min 2s/file.
    #   * Branch B (window min step par bhi tang ho): ladder tracker ke
    #     foran baad se min step (2s) par chalti hai — end abhi ke aas
    #     paas rehta hai, future me nahi jata.
    #   * Tracker me FUTURE (purani) value ho to use nazar-andaz kar do —
    #     floor = ek din pehle. Purani future files nayi files se "nayi"
    #     rahengi jab tak delete na hon — ye qabool shuda hai.
    _now0 = time.time()
    _end_soft = _now0 - 60
    _floor = None
    if os.path.exists(T_F):
        try:
            with open(T_F,"r") as f:
                _lt = float(f.read().strip())
                if _lt < _now0: _floor = _lt
        except: pass
    if _floor is None:
        _floor = _end_soft - 86400
    _slots = max(1, 2 * len(imgs))
    _raw_step = (_end_soft - _floor) / _slots
    if _raw_step >= 2:
        t_step = min(_raw_step, 60.0)
        cur_t = _end_soft - t_step * _slots
    else:
        # Branch B: window tang hai — tracker ke baad min step se chalo
        t_step = 2.0
        cur_t = _floor

    print("")

    # 🔥 FIX: Removed 'bar_chars' to prevent tqdm crash on older versions
    pbar_format = f" {C1}{{desc}}{ENDC} {C5}|{ENDC}{{bar:20}}{C5}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"
    desc_text = "✨ HD Mode " if is_hd else "⚙️ Normal "

    def verify_saved(path):
        try:
            if not os.path.exists(path) or os.path.getsize(path) < 100: return False
            with Image.open(path) as im: im.verify()
            return True
        except Exception:
            return False

    def process_one(f, idx, t_now):
        raw_p = os.path.join(I, f)
        w = safe_imread(raw_p)
        if w is None:
            return 'unreadable', idx, t_now

        w_exact = resize_exact(w) if crop_916 else w
        w = None   # bari original image ki RAM foran free
        w_processed = apply_hd_enhance(w_exact) if is_hd else w_exact

        mock_path = os.path.join(O, f"{idx}.jpg")
        wall_path = os.path.join(R, f"{idx+1}.jpg")
        mt, wt = t_now+t_step, t_now+2*t_step

        w_resized_mock = cv2.resize(w_processed, (W_M,H_M))
        warped = cv2.warpPerspective(w_resized_mock,M,(W_M,H_M))
        wrp = cv2.bitwise_and(warped,warped,mask=mask).astype(np.float32)
        wrp *= m_inv      # same formula, lekin in-place (RAM kam)
        wrp += m_pre
        fin = wrp
        np.clip(fin, 0, 255, out=fin)
        save_optimized(fin.astype(np.uint8), mock_path)
        os.utime(mock_path, (mt, mt))

        save_optimized(w_processed, wall_path)
        os.utime(wall_path, (wt, wt))

        if not (verify_saved(mock_path) and verify_saved(wall_path)):
            raise IOError("save verify failed")

        # Original tarteeb: har image ke baad foran scan, pehle mock phir wall, phir agli image
        safe_scan(mock_path)
        safe_scan(wall_path)
        gallery_pair_copy(mock_path, wall_path, mt, wt)
        ghost_remove(raw_p)   # input ki gallery entry bhi saaf (pehle ghost reh jati thi)

        del w, w_exact, w_processed, w_resized_mock, warped, wrp, fin
        return 'ok', idx+2, wt

    processed_count = 0
    corrupt_count = 0
    skip_log = []
    set_wakelock(True)
    for i, f in enumerate(tqdm(imgs, desc=desc_text, bar_format=pbar_format)):
        success = False
        last_err = None
        for attempt in range(3):
            try:
                status, new_idx, new_t = process_one(f, s_idx, cur_t)
                if status == 'ok':
                    s_idx, cur_t = new_idx, new_t
                    processed_count += 1
                    success = True
                    break
                else:
                    last_err = "padhi nahi gayi: " + READ_ERR.get(os.path.join(I, f), "?")
                    break  # unreadable file - retry se faida nahi
            except Exception as e:
                last_err = f"{type(e).__name__}: {str(e)[:50]}"
                # Retry se pehle is attempt ki adhoori files saaf karo
                for p in (os.path.join(O, f"{s_idx}.jpg"), os.path.join(R, f"{s_idx+1}.jpg")):
                    if os.path.exists(p):
                        try: os.remove(p)
                        except: pass
                gc.collect()
                time.sleep(0.3)

        if not success:
            corrupt_count += 1
            skip_log.append(f"{f} ({last_err})")

        gc.collect()   # har image ke baad RAM saaf (signal 9 se bachne ke liye)

    set_wakelock(False)

    if processed_count > 0:
        try:
            with open(T_F,"w") as f: f.write(str(cur_t))
        except: pass
        print(f"\n {C4}✔ {processed_count} Joray aesthetic style mein ready!{ENDC}")
    else:
        print(f"\n {C5}✔ Sab already set hai.{ENDC}")

    if corrupt_count > 0:
        print(f" {C2}⚠ {corrupt_count} file(s) skip ki gayin (dobara retry hongi agle run me):{ENDC}")
        for s in skip_log[:8]:
            print(f"   {C2}- {s}{ENDC}")
        if len(skip_log) > 8:
            print(f"   {C2}...aur {len(skip_log)-8} more{ENDC}")
        print("")
    else:
        print("")

elif action == 'batch':
    display_header()
    os.makedirs(W_H, exist_ok=True)
    mks = [f for f in os.listdir(O) if os.path.splitext(f)[0].isdigit()] if os.path.exists(O) else []
    if mks:
        bn = len([d for d in os.listdir(W_H) if os.path.isdir(os.path.join(W_H, d))]) + 1
        cb = os.path.join(W_H, _batch_pick_name(f"P{P_NUM}_Batch_{bn}"))
        os.makedirs(cb, exist_ok=True)

        set_wakelock(True)
        o_files = sorted([f for f in os.listdir(O) if os.path.splitext(f)[0].isdigit()], key=lambda x: int(os.path.splitext(x)[0]))
        r_files = sorted([f for f in os.listdir(R) if os.path.splitext(f)[0].isdigit()], key=lambda x: int(os.path.splitext(x)[0]))
        for f in o_files:
            _s = os.path.join(O, f)
            dst = move_clean(_s, cb)
            safe_scan(dst)
            _gone_paths.append(_s)
        for f in r_files:
            _s = os.path.join(R, f)
            dst = move_clean(_s, cb)
            safe_scan(dst)
            _gone_paths.append(_s)
        bulk_scan_gone()   # purane Mockups/Ready folders ki ghost entries saaf
        set_wakelock(False)

        animated_spinner("Packing Batch", 0.6)
        print(f" {C4}📦 {os.path.basename(cb)} Warehouse mein chala gaya!{ENDC}\n")
    else:
        print(f"\n {C2}✖ Batch banane ke liye data nahi hai.{ENDC}\n")

elif action == 'del':
    display_header()

    img_exts = ('.jpg', '.jpeg', '.png', '.webp', '.heic', '.avif')
    del_pbar_format = "{l_bar}%s{bar}%s{r_bar}" % (HR, ENDC)

    def count_imgs(folder):
        if not os.path.exists(folder): return 0
        return len([f for f in os.listdir(folder) if not f.startswith('.') and f.lower().endswith(img_exts)])

    print(f"{HG} 🔍 Scanning files...{ENDC}")
    i_count = count_imgs(I)
    o_count = count_imgs(O)
    r_count = count_imgs(R)
    wh_count = len([d for d in os.listdir(W_H) if "Batch_" in d]) if os.path.exists(W_H) else 0
    BK = BK_DIR
    bk_count = 0
    if os.path.isdir(BK):
        for r, d, fs in os.walk(BK):
            bk_count += len([f for f in fs if f != '.nomedia'])
    time.sleep(0.3)

    print(f"\n {HG2}╔{'═'*54}╗{ENDC}")
    print(f" {HG2}║{ENDC}  {HR}{BOLD}🗑️   D E L E T E   M E N U{ENDC}{' '*23}{HG2}║{ENDC}")
    print(f" {HG2}╚{'═'*54}╝{ENDC}")
    print(f" {HC}[1]{ENDC} 🖼️  Tayaar Maal (Mock+Wall) {HY}→ {o_count + r_count} file(s){ENDC} {HG}[DEFAULT]{ENDC}")
    print(f" {HC}[2]{ENDC} 📱 Sirf Mockups           {HY}→ {o_count} file(s){ENDC}")
    print(f" {HC}[3]{ENDC} 📥 Input Images          {HY}→ {i_count} file(s){ENDC}")
    print(f" {HC}[4]{ENDC} 📦 Warehouse (Batches)    {HY}→ {wh_count} batch(es){ENDC}")
    print(f" {HC}[5]{ENDC} ♻️  Backup Wapas Lao (Last Delete) {HY}→ {bk_count} file(s){ENDC}")
    print(f" {HC}[0]{ENDC} Cancel")
    print(f" {HG2}⚠ Delete ke baad files [5] se wapas aa sakti hain — lekin sirf AAKHRI delete ki.{ENDC}")
    choice = input(f"\n {HG}➜ {ENDC}").strip()
    if choice == '': choice = '1'

    _bk_state = {'ready': False}

    def ensure_backup_reset():
        # Har naye delete action pe purana backup saaf — sirf AAKHRI delete ka backup rehta hai
        if _bk_state['ready']: return
        _bk_state['ready'] = True
        if os.path.isdir(BK): shutil.rmtree(BK, ignore_errors=True)
        os.makedirs(BK, exist_ok=True)
        try:
            with open(os.path.join(BK, ".nomedia"), "w") as fh: fh.write("")
        except: pass

    def move_to_backup(src):
        rel = os.path.relpath(src, B)
        dst = os.path.join(BK, rel)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        if os.path.exists(dst):
            base, ext = os.path.splitext(dst); k = 1
            while os.path.exists(f"{base}_{k}{ext}"): k += 1
            dst = f"{base}_{k}{ext}"
        shutil.copy2(src, dst)
        if os.path.getsize(dst) != os.path.getsize(src):
            raise IOError('backup copy size mismatch')
        ghost_remove(src, scan=False)   # purana path gallery se batch mein hatega (bulk_scan_gone)

    def restore_backup():
        if not os.path.isdir(BK): return 0
        moved = 0
        for r, d, fs in os.walk(BK):
            for f in fs:
                if f == '.nomedia': continue
                src = os.path.join(r, f)
                dst = os.path.join(B, os.path.relpath(src, BK))
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                if os.path.exists(dst):
                    base, ext = os.path.splitext(dst); k = 1
                    while os.path.exists(f"{base}_back{k}{ext}"): k += 1
                    dst = f"{base}_back{k}{ext}"
                try:
                    shutil.move(src, dst)
                    moved += 1
                except: pass
        shutil.rmtree(BK, ignore_errors=True)
        for d in (O, R, I, W_H):
            if os.path.isdir(d): safe_scan_dir(d)
        return moved

    def perm_del_images(folder, label):
        files = []
        if os.path.exists(folder):
            files = [f for f in os.listdir(folder) if not f.startswith('.') and f.lower().endswith(img_exts)]
        if not files:
            print(f" {HG2}✖ {label}: koi file nahi mili.{ENDC}")
            return 0
        ensure_backup_reset()
        c = 0
        for f in tqdm(files, desc=f" 🗑️  {label}", bar_format=del_pbar_format):
            try:
                _src = os.path.join(folder, f)
                move_to_backup(_src)
                _gone_paths.append(_src)
                c += 1
            except: pass
        bulk_scan_gone()
        safe_scan_dir(folder)
        return c

    def tm_pairs_cleanup(parity):
        # TermuxMod "Pairs" copies ki safai — $HOME delete ke sath wahan ki
        # copies bhi jayen: 'odd' = sirf mockups, 'even' = sirf wallpapers,
        # 'all' = dono. Project/Pairs folder na ho to khamoshi se skip.
        try:
            dp = os.path.join(TM_ROOT, f"Project {P_NUM}", "Pairs")
            if not os.path.isdir(dp): return
            for f in os.listdir(dp):
                if f.startswith('.') or not f.lower().endswith(img_exts): continue
                stem = os.path.splitext(f)[0]
                if not stem.isdigit(): continue
                n = int(stem)
                if parity == 'odd' and n % 2 == 0: continue
                if parity == 'even' and n % 2 == 1: continue
                p = os.path.join(dp, f)
                ghost_remove(p, scan=False)
                _gone_paths.append(p)
            bulk_scan_gone()
        except Exception:
            pass

    def delete_folder_with_progress(path, label):
        all_files = []
        for r, d, fs in os.walk(path):
            for f in fs: all_files.append(os.path.join(r, f))
        if all_files: ensure_backup_reset()
        for f in tqdm(all_files, desc=f" 🗑️  {label}", bar_format=del_pbar_format):
            try:
                move_to_backup(f)
                _gone_paths.append(f)
            except: pass
        bulk_scan_gone()
        safe_scan_dir(path)
        shutil.rmtree(path, ignore_errors=True)

    set_wakelock(True)
    if choice == '1':
        d1 = perm_del_images(O, "Mockups")
        d2 = perm_del_images(R, "Wallpapers")
        tm_pairs_cleanup('all')
        print(f"\n {HG}✔ {d1+d2}/{o_count+r_count} Tayaar Images Deleted.{ENDC}")
    elif choice == '2':
        d = perm_del_images(O, "Mockups")
        tm_pairs_cleanup('odd')
        print(f"\n {HG}✔ {d}/{o_count} Sirf Mockups Deleted.{ENDC}")
    elif choice == '3':
        d = perm_del_images(I, "Input")
        print(f"\n {HG}✔ {d}/{i_count} Input Images Deleted.{ENDC}")
    elif choice == '4':
        if not os.path.exists(W_H) or wh_count == 0:
            print(f"\n {HG2}✖ Warehouse khali hai!{ENDC}"); set_wakelock(False); sys.exit()
        batches = sorted([d for d in os.listdir(W_H) if "Batch_" in d])
        for i, b in enumerate(batches):
            bcount = len([f for f in os.listdir(os.path.join(W_H, b)) if f.lower().endswith(img_exts)])
            print(f" {HC}[{i+1}]{ENDC} {b}  {HY}→ {bcount} file(s){ENDC}")
        sub_choice = input(f"\n {HG}Choose Batch or 'A' (All): {ENDC}")
        if sub_choice.lower() == 'a':
            delete_folder_with_progress(W_H, "Warehouse (All)")
            os.makedirs(W_H)
            print(f"\n {HG}✔ Warehouse saaf ho gaya.{ENDC}")
        elif sub_choice.isdigit() and 1 <= int(sub_choice) <= len(batches):
            target_name = batches[int(sub_choice)-1]
            target = os.path.join(W_H, target_name)
            delete_folder_with_progress(target, target_name)
            print(f"\n {HG}✔ Deleted.{ENDC}")
    elif choice == '5':
        d = restore_backup()
        if d: print(f"\n {HG}✔ {d} file(s) backup se wapas aa gayi.{ENDC}")
        else: print(f"\n {HG2}✖ Backup khali hai — wapas lane ke liye kuch nahi.{ENDC}")
    set_wakelock(False)
