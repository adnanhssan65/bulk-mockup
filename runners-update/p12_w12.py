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
    project_line = f"Project : {P_NUM}"
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
    try:
        # Fire-and-forget: app ke session-less process me ye command atak sakti hai - intezar nahi karna
        subprocess.Popen(["termux-wake-lock" if enable else "termux-wake-unlock"],
                         stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except: pass

def safe_scan(path, timeout_sec=1):
    # Fire-and-forget: scan ko kabhi wait nahi karna — app ke andar run ho to
    # termux-media-scan wapas ane me dair karta hai aur har image par time zaya hota hai.
    # Scan pichay chal kar apna kaam khud pura kar leta hai, tartib launch order se rehti hai.
    try:
        subprocess.Popen(["termux-media-scan", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except Exception:
        pass


# --- Gallery copies (TermuxMod) — set ki tartib me ---
# Run ke sath hi har set ki files TermuxMod/Project N folder me bhi laga do,
# bilkul usi order me jis order me save hui theen: pehli file Mockups me,
# baqi Ready Wallpapers me; har copy par usi file ka tracker time aur copy
# ke foran baad scan. Is se gallery me images set ki tartib me ati hain
# (block me nahi). Folder mojood na ho to khamoshi se skip —
# folder banana app ka kaam hai.
TM_ROOT = "/storage/emulated/0/TermuxMod"
def gallery_copy_outputs(items):
    try:
        proj = os.path.join(TM_ROOT, f"Project {P_NUM}")
        dm = os.path.join(proj, "Mockups")
        dr = os.path.join(proj, "Ready Wallpapers")
        if not (os.path.isdir(dm) and os.path.isdir(dr)):
            return
        import shutil
        for i, (src, t) in enumerate(items):
            d = os.path.join(dm if i == 0 else dr, os.path.basename(src))
            shutil.copyfile(src, d)
            os.utime(d, (t, t))
            safe_scan(d)
    except Exception:
        pass

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

# Project number: argv[2], warna file ke naam se (w12.py -> 12), warna 12
if len(sys.argv) > 2 and sys.argv[2].isdigit():
    P_NUM = sys.argv[2]
else:
    _g = ''.join(ch for ch in os.path.basename(sys.argv[0]) if ch.isdigit())
    P_NUM = _g if _g else "12"
B = os.environ.get("MOCKUP_BASE", os.path.dirname(os.path.abspath(__file__)))     # script project folder ke andar ho to wahi
_SD = os.path.dirname(B)
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")
T_F = os.path.join(_SD, ".mockup_time_tracker.txt")
M_P, S_P = os.path.join(B, "mockup.png"), os.path.join(B, "shape.png")

# --- GALLERY REPAIR / BACKUP LOCATION ---
# Backup ab project folder ke BAHAR hai (hidden + .nomedia), taake project folder ke andar kabhi .nomedia na aaye
BK_DIR = os.path.join(_SD, f".mockup_backup_{P_NUM}")
LEGACY_BK = os.path.join(B, ".last_delete_backup")   # purani jagah (project folder ke andar)
HEAL_MARK = os.path.join(_SD, f".mockup_gallery_heal_{P_NUM}.done")
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

# =====================================================================
#  📱 LOCK SCREEN ENGINE (Project 12)
#  Wallpaper -> iPhone lock screen (time, date, signal, wifi, battery,
#  dynamic island, torch, camera, home bar) -> mockup screen mein fit
# =====================================================================
import datetime
from PIL import ImageDraw, ImageFont, ImageFilter

# ---- Settings (yahan se badlo) ----
TIME_FORMAT   = "12"     # "12" -> 9:41   |   "24" -> 09:41
BATTERY_MIN   = 62       # har screen pe random battery (min..max)
BATTERY_MAX   = 100
SHOW_BATT_PCT = False    # True -> battery ke andar number
CANVAS_W      = 1179     # lock screen render width (iPhone 14 Pro)
SUPERSAMPLE   = 2        # warp quality (2 = sharp, phone pe RAM kam chahiye to 1)
CLOCK_W_FRAC  = 0.56     # time ki chorai (screen width ka hissa)
CLOCK_Y_PT    = 146      # time center (pt, top se)
DATE_Y_PT     = 82       # date center (pt, top se)

DAY_NAMES   = ["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"]
MONTH_NAMES = ["January","February","March","April","May","June","July","August","September","October","November","December"]

_FONT_CACHE = {}
def get_font(name, size):
    key = (name, int(size))
    if key in _FONT_CACHE: return _FONT_CACHE[key]
    here = os.path.dirname(os.path.abspath(__file__))
    cands = [os.path.join(B, "fonts", name), os.path.join(here, "fonts", name),
             os.path.join("/usr/share/fonts/opentype/inter", name)]
    f = None
    for c in cands:
        if os.path.isfile(c):
            try: f = ImageFont.truetype(c, int(size)); break
            except Exception: f = None
    if f is None:
        for fallback in ("DejaVuSans-Bold.ttf", "arialbd.ttf"):
            try: f = ImageFont.truetype(fallback, int(size)); break
            except Exception: pass
    if f is None: f = ImageFont.load_default()
    _FONT_CACHE[key] = f
    return f

def fit_screen_quad(mask):
    """Rounded screen mask -> 4 exact corners (TL,TR,BR,BL): 4 seedhi sides fit karke."""
    cnts, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
    c = max(cnts, key=cv2.contourArea).reshape(-1, 2).astype(np.float32)
    rect = order_points(cv2.boxPoints(cv2.minAreaRect(c)).astype(np.float32))
    lines = []
    for i in range(4):
        p, q = rect[i], rect[(i+1) % 4]
        d = q - p; L = float(np.linalg.norm(d)); u = d / L; nrm = np.array([-u[1], u[0]], dtype=np.float32)
        t = (c - p) @ u; dist = np.abs((c - p) @ nrm)
        sel = c[(t > 0.25*L) & (t < 0.75*L) & (dist < 0.05*L)]
        if len(sel) < 10: sel = c[(t > 0.1*L) & (t < 0.9*L) & (dist < 0.06*L)]
        if len(sel) < 10: return rect
        vx, vy, x0, y0 = cv2.fitLine(sel, cv2.DIST_HUBER, 0, 0.01, 0.01).flatten()
        lines.append((np.array([x0, y0]), np.array([vx, vy])))
    def inter(l1, l2):
        (p1, d1), (p2, d2) = l1, l2
        t = np.linalg.solve(np.array([d1, -d2]).T, p2 - p1)
        return p1 + t[0]*d1
    quad = np.array([inter(lines[3], lines[0]), inter(lines[0], lines[1]),
                     inter(lines[1], lines[2]), inter(lines[2], lines[3])], dtype=np.float32)
    return order_points(quad)

# ---------- chhote helpers ----------
def _ss_mask(w, h, fn, ss=4):
    """Supersampled shape -> smooth L mask (w x h)."""
    big = Image.new("L", (max(1, int(w*ss)), max(1, int(h*ss))), 0)
    fn(ImageDraw.Draw(big), ss)
    return big.resize((max(1, int(w)), max(1, int(h))), Image.LANCZOS)

def _paste(canvas, mask, x, y, rgb, alpha=1.0):
    if alpha < 0.999: mask = mask.point(lambda v: int(v*alpha))
    canvas.paste(tuple(rgb), (int(round(x)), int(round(y))), mask)

def _lum(canvas_np, box):
    x0, y0, x1, y1 = [int(v) for v in box]
    reg = canvas_np[max(0, y0):max(1, y1), max(0, x0):max(1, x1)].astype(np.float32)
    if reg.size == 0: return 0.0
    b, g, r = reg[..., 0], reg[..., 1], reg[..., 2]
    return float(np.mean(0.2126*r + 0.7152*g + 0.0722*b) / 255.0)

LIGHT_INK = (255, 255, 255)
DARK_INK  = (24, 24, 28)
def _ink(lum): return DARK_INK if lum > 0.66 else LIGHT_INK

def cover_resize(img_bgr, tw, th):
    h, w = img_bgr.shape[:2]
    tr = tw / th
    if w / h > tr:
        nw = int(h * tr); x0 = (w - nw)//2; img_bgr = img_bgr[:, x0:x0+nw]
    else:
        nh = int(w / tr); y0 = (h - nh)//2; img_bgr = img_bgr[y0:y0+nh, :]
    return cv2.resize(img_bgr, (tw, th), interpolation=cv2.INTER_CUBIC)

# ---------- icons ----------
def icon_signal(s):
    w, h = int(round(18*s)), int(round(12*s))
    def fn(d, ss):
        bw, gap = 3.0*s*ss, 1.6*s*ss
        hs = [4.0, 6.6, 9.2, 12.0]
        for i, hh in enumerate(hs):
            x0 = i*(bw+gap); y1 = h*ss; y0 = y1 - hh*s*ss
            d.rounded_rectangle([x0, y0, x0+bw, y1], radius=0.9*s*ss, fill=255)
    return _ss_mask(w, h, fn)

def icon_wifi(s):
    w, h = int(round(17.5*s)), int(round(12.5*s))
    def fn(d, ss):
        W, H = w*ss, h*ss
        cx, cy = W/2, H*1.02                       # sector ka apex (neeche)
        rs = [(0.0, 0.26), (0.40, 0.62), (0.76, 1.0)]   # inner/outer (fraction of radius)
        R = H*1.0
        for ri, ro in rs:
            ring = Image.new("L", (int(W), int(H)), 0)
            dd = ImageDraw.Draw(ring)
            dd.pieslice([cx-R*ro, cy-R*ro, cx+R*ro, cy+R*ro], 225, 315, fill=255)
            if ri > 0: dd.pieslice([cx-R*ri, cy-R*ri, cx+R*ri, cy+R*ri], 224, 316, fill=0)
            d._image.paste(255, (0, 0), ring)
        # niche ka dot ek wedge hi hai (ri=0) -> thoda sa chhota round
    return _ss_mask(w, h, fn)

def icon_battery(s, level, show_pct=False):
    bw, bh = 25.0*s, 12.5*s
    tipw = 1.7*s
    w, h = int(round(bw + tipw + 1.5*s)), int(round(bh + 1))
    def outline(d, ss):
        d.rounded_rectangle([0, 0, bw*ss, bh*ss], radius=4.0*s*ss, outline=255, width=int(1.1*s*ss))
        d.rounded_rectangle([(bw+0.9*s)*ss, 4.1*s*ss, (bw+0.9*s+tipw)*ss, (bh-4.1*s)*ss], radius=0.8*s*ss, fill=255)
    def fill(d, ss):
        inset = 1.9*s
        fw = max(2.0*s, (bw - 2*inset) * level/100.0)
        d.rounded_rectangle([inset*ss, inset*ss, (inset+fw)*ss, (bh-inset)*ss], radius=2.3*s*ss, fill=255)
    return _ss_mask(w, h, outline), _ss_mask(w, h, fill)

def icon_torch(size):
    S = size
    def fn(d, ss):
        u = S*ss/100.0
        d.rounded_rectangle([22*u, 2*u, 78*u, 26*u], radius=5*u, fill=255)       # head
        d.polygon([(30*u, 24*u), (70*u, 24*u), (65*u, 44*u), (35*u, 44*u)], fill=255)   # neck
        d.rounded_rectangle([35*u, 36*u, 65*u, 98*u], radius=8*u, fill=255)       # body
        d.rounded_rectangle([45*u, 52*u, 55*u, 70*u], radius=5*u, fill=0)         # button slot
    return _ss_mask(S*0.62, S, fn)

def icon_camera(size):
    S = size
    def fn(d, ss):
        u = S*ss/100.0
        d.rounded_rectangle([0, 22*u, 100*u, 92*u], radius=14*u, fill=255)        # body
        d.rounded_rectangle([30*u, 10*u, 68*u, 30*u], radius=6*u, fill=255)       # top bump
        d.ellipse([50*u-24*u, 57*u-24*u, 50*u+24*u, 57*u+24*u], fill=0)           # lens hole
        d.ellipse([50*u-13*u, 57*u-13*u, 50*u+13*u, 57*u+13*u], fill=255)         # lens centre
        d.ellipse([86*u-3.6*u, 37*u-3.6*u, 86*u+3.6*u, 37*u+3.6*u], fill=0)       # flash dot
    return _ss_mask(S, S*0.92, fn)

def _clock_layout(text, F):
    f = get_font("InterDisplay-SemiBold.otf", F)
    adv, x = [], 0.0
    for ch in text:
        a = 0.40*F if ch == ":" else f.getlength(ch)
        adv.append((ch, x)); x += a - 0.004*F
    return f, adv, x

def render_lock_screen(wall_bgr, when, batt, CW, CH):
    """wall_bgr (kisi bhi size ki) -> iPhone lock screen BGR (CW x CH)."""
    s = CW / 393.0                                        # 1 pt = s px
    base = cover_resize(wall_bgr, CW, CH)
    canvas = Image.fromarray(cv2.cvtColor(base, cv2.COLOR_BGR2RGB))
    npb = base                                            # luminance ke liye (BGR)

    # --- time / date text ---
    if TIME_FORMAT == "24": ttxt = when.strftime("%H:%M")
    else:
        hh = when.hour % 12 or 12; ttxt = f"{hh}:{when.minute:02d}"
    dtxt = f"{DAY_NAMES[when.weekday()]}, {MONTH_NAMES[when.month-1]} {when.day}"

    # Time ka size: "09:41" ki chorai = CLOCK_W_FRAC * width (calibrate)
    f0, _, w0 = _clock_layout("09:41", 100)
    F = 100.0 * (CLOCK_W_FRAC * CW) / w0
    font_t, adv, tw = _clock_layout(ttxt, F)
    dh = -font_t.getbbox("8", anchor="ls")[1]             # digit height
    cy = CLOCK_Y_PT * s
    baseline = cy + dh/2
    x_start = (CW - tw) / 2

    clock = Image.new("L", (CW, CH), 0)
    cd = ImageDraw.Draw(clock)
    for ch, x in adv:
        if ch == ":":
            r = 0.058*F
            cxx = x_start + x + 0.20*F
            for yy in (cy - 0.215*dh, cy + 0.215*dh):
                cd.ellipse([cxx-r, yy-r, cxx+r, yy+r], fill=255)
        else:
            cd.text((x_start + x, baseline), ch, font=font_t, fill=255, anchor="ls")
    # date
    font_d = get_font("Inter-SemiBold.otf", 21.5*s)
    date_mask = Image.new("L", (CW, CH), 0)
    ImageDraw.Draw(date_mask).text((CW/2, DATE_Y_PT*s), dtxt, font=font_d, fill=255, anchor="mm")

    clk_box = (CW*0.12, (DATE_Y_PT-14)*s, CW*0.88, (CLOCK_Y_PT+50)*s)
    ink_c = _ink(_lum(npb, clk_box))
    if ink_c == LIGHT_INK:    # soft shadow, white text ko padhne layak rakhne ke liye
        sh = ImageChops_lighter(clock, date_mask).filter(ImageFilter.GaussianBlur(radius=2.2*s))
        canvas.paste((0, 0, 0), (0, 0), sh.point(lambda v: int(v*0.22)))
    canvas.paste(ink_c, (0, 0), clock)
    canvas.paste(ink_c, (0, 0), date_mask.point(lambda v: int(v*0.93)))

    # --- status bar (top right) ---
    ink_s = _ink(_lum(npb, (CW*0.60, 14*s, CW*0.97, 46*s)))
    sy = 29.5 * s                                          # island ka center
    bm, bmask_fill = icon_battery(s, batt, SHOW_BATT_PCT)
    right = CW - 24.5*s
    bx = right - bm.size[0]; by = sy - bm.size[1]/2
    _paste(canvas, bm, bx, by, ink_s, 0.42)
    _paste(canvas, bmask_fill, bx, by, ink_s, 1.0)
    wf = icon_wifi(s); wx = bx - 5.2*s - wf.size[0]; _paste(canvas, wf, wx, sy - wf.size[1]/2 - 0.3*s, ink_s)
    sg = icon_signal(s); sx = wx - 5.0*s - sg.size[0]; _paste(canvas, sg, sx, sy - sg.size[1]/2, ink_s)

    # --- dynamic island ---
    iw, ih = 124.0*s, 36.0*s
    isl = _ss_mask(iw, ih, lambda d, ss: d.rounded_rectangle([0, 0, iw*ss, ih*ss], radius=ih*ss/2, fill=255))
    canvas.paste((0, 0, 0), (int(round((CW-iw)/2)), int(round(11*s))), isl)

    # --- bottom: torch + camera + home bar ---
    cd_ = 50.0 * s                                         # circle diameter
    cyb = CH - 79.0*s                                      # circle center y
    for cxb, kind in ((70.0*s, "torch"), (CW - 70.0*s, "cam")):
        bx0, by0 = int(cxb - cd_/2), int(cyb - cd_/2)
        bx1, by1 = int(bx0 + cd_), int(by0 + cd_)
        reg = canvas.crop((bx0, by0, bx1, by1))
        glass = reg.filter(ImageFilter.GaussianBlur(radius=8*s)).point(lambda v: int(v*0.55))
        circ = _ss_mask(bx1-bx0, by1-by0, lambda d, ss: d.ellipse([0, 0, (bx1-bx0)*ss-1, (by1-by0)*ss-1], fill=255))
        canvas.paste(glass, (bx0, by0), circ.point(lambda v: int(v*0.88)))
        if kind == "torch":
            m = icon_torch(int(23*s))
        else:
            m = icon_camera(int(24*s))
        canvas.paste((255, 255, 255), (int(cxb - m.size[0]/2), int(cyb - m.size[1]/2)), m)

    ink_h = _ink(_lum(npb, (CW*0.3, CH-30*s, CW*0.7, CH-6*s)))
    hw, hh_ = 134.0*s, 5.2*s
    hm = _ss_mask(hw, hh_, lambda d, ss: d.rounded_rectangle([0, 0, hw*ss, hh_*ss], radius=hh_*ss/2, fill=255))
    _paste(canvas, hm, (CW-hw)/2, CH - 8.5*s - hh_, ink_h, 0.92)

    return cv2.cvtColor(np.array(canvas), cv2.COLOR_RGB2BGR)

def ImageChops_lighter(a, b):
    from PIL import ImageChops
    return ImageChops.lighter(a, b)


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
        hole = (sh[:, :, 3] > 127).astype(np.uint8) * 255
        n, lab, st, _c = cv2.connectedComponentsWithStats(hole)
        if n > 1: hole = (lab == (1 + int(np.argmax(st[1:, 4])))).astype(np.uint8) * 255
        quad = fit_screen_quad(hole)
        # lock screen canvas ka size = asli screen ke aspect ke mutabiq (koi stretch nahi)
        wq = (np.linalg.norm(quad[1]-quad[0]) + np.linalg.norm(quad[2]-quad[3])) / 2
        hq = (np.linalg.norm(quad[3]-quad[0]) + np.linalg.norm(quad[2]-quad[1])) / 2
        cw = CANVAS_W; ch = int(round(cw * hq / wq))
        # sirf screen wala hissa warp hoga (RAM bachat)
        pad = 6
        x0 = max(0, int(np.floor(quad[:, 0].min())) - pad); y0 = max(0, int(np.floor(quad[:, 1].min())) - pad)
        x1 = min(wm, int(np.ceil(quad[:, 0].max())) + pad); y1 = min(hm, int(np.ceil(quad[:, 1].max())) + pad)
        ma = mk[y0:y1, x0:x1, 3].astype(np.float32) / 255.0
        mb = mk[:, :, :3].copy()
        reg_pre = mb[y0:y1, x0:x1].astype(np.float32) * ma[:, :, None]
        reg_inv = (1.0 - ma)[:, :, None]
        ss = max(1, int(SUPERSAMPLE))
        src = np.float32([[0, 0], [cw, 0], [cw, ch], [0, ch]])
        dst = (quad - np.float32([x0, y0])) * ss
        pm = cv2.getPerspectiveTransform(src, dst.astype(np.float32))
        return dict(mb=mb, cw=cw, ch=ch, box=(x0, y0, x1, y1), pre=reg_pre, inv=reg_inv, M=pm, ss=ss)

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
    MK = _result['data']

    s_idx = get_max_seq([O, R, W_H]) + 1
    T0 = datetime.datetime.now().replace(second=0, microsecond=0)   # pehli screen = abhi ka asli time
    cur_t = time.time()
    if os.path.exists(T_F):
        try:
            with open(T_F,"r") as f:
                lt=float(f.read().strip())
                if lt>cur_t: cur_t=lt
        except: pass

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

    def process_one(f, idx, t_now, k):
        raw_p = os.path.join(I, f)
        w = safe_imread(raw_p)
        if w is None:
            return 'unreadable', idx, t_now

        w_exact = resize_exact(w) if crop_916 else w
        w = None   # bari original image ki RAM foran free
        w_processed = apply_hd_enhance(w_exact) if is_hd else w_exact

        mock_path = os.path.join(O, f"{idx}.jpg")
        wall_path = os.path.join(R, f"{idx+1}.jpg")
        mt, wt = t_now+60, t_now+120

        # --- lock screen: pehli image = abhi ka time, har agli +1 minute ---
        when = T0 + datetime.timedelta(minutes=k)
        batt = random.randint(BATTERY_MIN, BATTERY_MAX)
        lock = render_lock_screen(w_processed, when, batt, MK['cw'], MK['ch'])

        ss = MK['ss']; x0, y0, x1, y1 = MK['box']
        big = cv2.warpPerspective(lock, MK['M'], ((x1-x0)*ss, (y1-y0)*ss), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)
        if ss > 1: big = cv2.resize(big, (x1-x0, y1-y0), interpolation=cv2.INTER_AREA)
        reg = big.astype(np.float32) * MK['inv']
        reg += MK['pre']
        np.clip(reg, 0, 255, out=reg)
        fin = MK['mb'].copy()
        fin[y0:y1, x0:x1] = reg.astype(np.uint8)
        save_optimized(fin, mock_path)
        os.utime(mock_path, (mt, mt))

        save_optimized(w_processed, wall_path)
        os.utime(wall_path, (wt, wt))

        if not (verify_saved(mock_path) and verify_saved(wall_path)):
            raise IOError("save verify failed")

        # Original tarteeb: har image ke baad foran scan, pehle mock phir wall, phir agli image
        safe_scan(mock_path)
        safe_scan(wall_path)
        gallery_copy_outputs([(mock_path, mt), (wall_path, wt)])
        ghost_remove(raw_p)   # input ki gallery entry bhi saaf (pehle ghost reh jati thi)

        del w_exact, w_processed, lock, big, reg, fin
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
                status, new_idx, new_t = process_one(f, s_idx, cur_t, processed_count)
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
        cb = os.path.join(W_H, f"P{P_NUM}_Batch_{bn}")
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
        print(f"\n {HG}✔ {d1+d2}/{o_count+r_count} Tayaar Images Deleted.{ENDC}")
    elif choice == '2':
        d = perm_del_images(O, "Mockups")
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
