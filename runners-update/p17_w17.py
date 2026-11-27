import os, sys, time, shutil, subprocess, random, threading, gc, queue, colorsys
from datetime import datetime
from PIL import Image, ImageDraw, ImageFont, ImageFile, ImageFilter
ImageFile.LOAD_TRUNCATED_IMAGES = True  # Halki truncated files ko galat "corrupt" na samjhe
import PIL.ImageCms as cms
from tqdm import tqdm
import cv2, numpy as np

# --- MULTI-COLORS (Neon/Aesthetic Gradient) ---
C1 = '\033[95m'  # Magenta
C2 = '\033[91m'  # Red
C3 = '\033[93m'  # Yellow
C4 = '\033[92m'  # Green
C5 = '\033[96m'  # Cyan
C6 = '\033[94m'  # Blue
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

#  HACKER TERMINAL PREMIUM HEADER
HL = '\u2500'
HD = '\u2550'
HG  = '\033[92m'   # Hacker Green (bright)
HG2 = '\033[32m'   # Hacker Green (dim)
HC  = '\033[96m'   # Cyan accent
HR  = '\033[91m'   # Red alert accent
HY  = '\033[93m'   # Yellow accent

def display_header():
    os.system('clear')
    quote = random.choice(QUOTES)

    print(f"{HG2}{HL * 56}{ENDC}")
    boot_lines = [
        "Initializing secure shell........",
        "Loading encryption modules.......",
        "Mounting mockup engine...........",
        "Verifying integrity checksum.....",
    ]
    for line in boot_lines:
        print(f"{HG} [BOOT]{ENDC} {line} {HC}[OK]{ENDC}")
        time.sleep(0)
    print(f"{HG2}{HL * 56}{ENDC}\n")

    ascii_art = [
        "   \u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2557   \u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2557   \u2588\u2588\u2557",
        "  \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2551",
        "  \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2551\u2588\u2588\u2551  \u2588\u2588\u2551\u2588\u2588\u2554\u2588\u2588\u2557 \u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2551\u2588\u2588\u2554\u2588\u2588\u2557 \u2588\u2588\u2551",
        "  \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2551\u2588\u2588\u2551  \u2588\u2588\u2551\u255a\u2588\u2588\u2557\u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2551\u2588\u2588\u2551\u255a\u2588\u2588\u2557\u2588\u2588\u2551",
        "  \u2588\u2588\u2551  \u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255d\u2588\u2588\u2551 \u255a\u2588\u2588\u2588\u2588\u2551\u2588\u2588\u2551  \u2588\u2588\u2551\u2588\u2588\u2551 \u255a\u2588\u2588\u2588\u2588\u2551",
        "  \u255a\u2550\u255d  \u255a\u2550\u255d\u255a\u2550\u2550\u2550\u2550\u2550\u255d \u255a\u2550\u255d  \u255a\u2550\u2550\u2550\u255d\u255a\u2550\u255d  \u255a\u2550\u255d\u255a\u2550\u255d  \u255a\u2550\u2550\u2550\u255d"
    ]
    for line in ascii_art:
        print(f"{HG}{BOLD}{line}{ENDC}")

    print(f"\n{HC}      \u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510")
    print(f"      \u2502   \U0001f4bb  R O O T   M O D E    \u2502")
    print(f"      \u2502   \U0001f513  A C C E S S   O K    \u2502")
    print(f"      \u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518{ENDC}\n")

    print(f" {HG}\U0001f338 \u26a1 \U0001f338   W e l c o m e   t o   t h e   S y s t e m   \U0001f338 \u26a1 \U0001f338{ENDC}\n")

    project_line = f"Project : {P_NUM}"
    owner_line   = "Owner   : Mehar Adnan"
    contact_line = "Contact : +44 7404 032136"
    pad = 46
    prefix = " \u26a1 "
    box_w = len(prefix) + pad

    print(f" {HG2}\u2554{HD * box_w}\u2557{ENDC}")
    print(f" {HG2}\u2551{HY}{prefix}{project_line:<{pad}}{HG2}\u2551{ENDC}")
    print(f" {HG2}\u2551{HY}{prefix}{quote:<{pad}}{HG2}\u2551{ENDC}")
    print(f" {HG2}\u2551{HY}{prefix}{owner_line:<{pad}}{HG2}\u2551{ENDC}")
    print(f" {HG2}\u2551{HY}{prefix}{contact_line:<{pad}}{HG2}\u2551{ENDC}")
    print(f" {HG2}\u255a{HD * box_w}\u255d{ENDC}")

    print(f"\n {HR}\U0001f6e1\ufe0f  [ ACCESS GRANTED ]{ENDC} {HG}\u2014 Session Encrypted \u2705{ENDC}")
    print(f" {C1}\u26a1{C3}\u2726{C5}\u26a1{C3}\u2726{C1}\u26a1  {BOLD}{HG}P R E M I U M   E D I T I O N{ENDC}  {C1}\u26a1{C3}\u2726{C5}\u26a1{C3}\u2726{C1}\u26a1{ENDC}\n")

def animated_spinner(text, duration=0.8):
    frames = ['\u2022', 'o', 'O', 'o']
    for i in range(int(duration * 10)):
        sys.stdout.write(f"\r {C1}\u273f{ENDC} {C5}{frames[i % len(frames)]}{ENDC} {text}...")
        sys.stdout.flush()
        time.sleep(0)
    sys.stdout.write(f"\r {C4}\u2714{ENDC} {text} Completed!       \n")

def set_wakelock(enable=True):
    try:
        # Fire-and-forget: app ke session-less process me ye command atak sakti hai - intezar nahi karna
        subprocess.Popen(["termux-wake-lock" if enable else "termux-wake-unlock"],
                         stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except: pass

# --- SCAN (Project 1 jesa) ---
# Har file save hote hi foran, ONE BY ONE aur usi order me scan hoti hai (pehle mockup, phir wallpapers 1..6).
# Isse gallery me sequence bilkul sahi rehti hai.
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


# --- Gallery copies (TermuxMod) — set ki tartib me ---
# Run ke sath hi har set ki files TermuxMod/Project N ke EK hi "Pairs"
# folder me laga do, bilkul usi production order me jis order me save hui
# theen (mockup pehle, phir usi ka wallpaper); har copy par usi file ka
# tracker time aur copy ke foran baad scan. Is se gallery me images set ki
# tartib me ati hain (block me nahi). Project folder mojood na ho to
# khamoshi se skip — folder banana app ka kaam hai.
TM_ROOT = "/storage/emulated/0/TermuxMod"
def gallery_copy_outputs(items):
    try:
        proj = os.path.join(TM_ROOT, f"Project {P_NUM}")
        if not os.path.isdir(proj):
            return
        dp = os.path.join(proj, "Pairs")
        if not os.path.isdir(dp):
            os.makedirs(dp, exist_ok=True)
        import shutil
        for src, t in items:
            d = os.path.join(dp, os.path.basename(src))
            shutil.copyfile(src, d)
            os.utime(d, (t, t))
            safe_scan(d)
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

# Har tarah ki image extension (naam ke bajaye andar ka asal format PIL khud pehchanta hai)
IMG_EXTS = ('.jpg', '.jpeg', '.jpe', '.jfif', '.png', '.webp', '.avif', '.heic', '.heif',
            '.bmp', '.tif', '.tiff', '.gif', '.jp2', '.ico')

READ_ERR = {}  # file path -> asli wajah (skip message me dikhane ke liye)

def sniff_type(path):
    # File ke andar asal me kya hai (naam .jpg ho tab bhi)
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
    # 1) PIL pehle (har type ki image isi se khulti hai) — transparent/palette/16-bit sab RGB me
    try:
        with Image.open(path) as im:
            im.load()
            if im.mode in ("RGBA", "LA") or (im.mode == "P" and "transparency" in im.info):
                im = im.convert("RGBA")
                bgc = Image.new("RGBA", im.size, (0, 0, 0, 255))
                im = Image.alpha_composite(bgc, im)
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
    # 4) ffmpeg (agar Termux me installed ho) - aakhri koshish
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


cv2.setNumThreads(0)
import math

cv2.setNumThreads(0)
import math

# ====================== PROJECT 17 : PHONE ON STAND (white wall + marble) ======================
P_NUM = sys.argv[2] if len(sys.argv) > 2 else "17"
B = os.environ.get("MOCKUP_BASE", os.path.dirname(os.path.abspath(__file__)))
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")
FONT_DIR = os.path.join(B, "fonts")
ASSET_DIR = os.path.join(B, "assets")
TEMPLATE = os.path.join(ASSET_DIR, "template.jpg")        # stand par khada iPhone (white wall + marble) ki photo
# Time tracker SHARED hai (w14/w15 jesa): ek project ke baad dosra chalao to wahi se aage ka waqt shuru hota hai
T_F = "/storage/emulated/0/.mockup_time_tracker.txt"


def _time_mode():
    # v155: Time Mode marker (app ki More Settings se likha jata
    # hai): /storage/emulated/0/TermuxMod/.time_mode ka content
    # past / current / future. File na ho ya value ghalat ho to
    # PAST — v154 wala default.
    try:
        with open("/storage/emulated/0/TermuxMod/.time_mode", "r") as _mf:
            _mv = _mf.read().strip().lower()
        if _mv in ("past", "current", "future"):
            return _mv
    except Exception:
        pass
    return "past"

# --- FONTS (zip ke "fonts" folder me; apna font chahiye to bas file badal do) ---
FONT_TIME   = os.path.join(FONT_DIR, "InterDisplay-Bold.otf")    # bada time
FONT_DATE   = os.path.join(FONT_DIR, "Inter-SemiBold.otf")       # date
FONT_STATUS = os.path.join(FONT_DIR, "Inter-SemiBold.otf")       # carrier
FONT_NUM    = os.path.join(FONT_DIR, "Inter-Bold.otf")           # battery number

# --- TEXT / MODES ---
CARRIER_TEXT = "Jio"
TIME_MODE  = "real"        # "real" = asli waqt | "fixed" = hamesha FIXED_TIME
FIXED_TIME = "9:41"
BATTERY_RANGE = (35, 100)
TEXT_STYLE = "tinted"      # "tinted" = wallpaper ke rang ka date/time | "white" = reference jesa safed
LUM_SPLIT = 0.55; BRIGHT_SPLIT = 0.78; TINT_MIN_CONTRAST = 4.0
STATUS_DARK_LUM = 0.70
GRAIN = 1.0

# --- GEOMETRY (reference photo se naapa hua, photo-pixels) ---
SCR_X, SCR_Y, SCR_W, SCR_H = 246, 296, 597, 1303      # screen ka rect
RAD = 84                                               # screen corner radius
SW = 700
K = SW / float(SCR_W)                                  # photo-px -> flat-px
SH = int(round(SCR_H * K))
PT = (SW / 393.0) * 1.3                                # status icons ka size

def _ss_mask(w, h, fn, ss=2):
    im = Image.new('L', (max(1, int(w)) * ss, max(1, int(h)) * ss), 0)
    fn(ImageDraw.Draw(im), ss)
    return np.asarray(im.resize((max(1, int(w)), max(1, int(h))), Image.BOX), np.float32) / 255.0

def put(canvas, mask, x, y, color, alpha=1.0):
    h, w = mask.shape[:2]; H, W = canvas.shape[:2]
    x, y = int(round(x)), int(round(y))
    x0, y0, x1, y1 = max(0, x), max(0, y), min(W, x + w), min(H, y + h)
    if x1 <= x0 or y1 <= y0: return
    m = mask[y0 - y:y1 - y, x0 - x:x1 - x][..., None] * alpha
    reg = canvas[y0:y1, x0:x1]
    reg += (np.array(color, np.float32) - reg) * m

def text_tile(text, font):
    bb = font.getbbox(text, anchor='ls')
    pad = 8
    w, h = bb[2] - bb[0] + 2 * pad, bb[3] - bb[1] + 2 * pad
    im = Image.new('L', (w, h), 0)
    ox, oy = pad - bb[0], pad - bb[1]
    ImageDraw.Draw(im).text((ox, oy), text, font=font, fill=255, anchor='ls')
    return np.asarray(im, np.float32) / 255.0, ox, oy, bb

def draw_text(cv, text, font, cx, base_y, color, alpha=1.0, align="c"):
    m, ox, oy, bb = text_tile(text, font)
    if align == "c": x = cx - (bb[0] + bb[2]) / 2.0 - ox
    elif align == "l": x = cx - bb[0] - ox
    else: x = cx - bb[2] - ox
    put(cv, m, x, base_y - oy, color, alpha)

def digit_h(font):
    return -font.getbbox("0", anchor='ls')[1]

def fit_font(path, text, target_w):
    f = ImageFont.truetype(path, 200)
    bb = f.getbbox(text, anchor='ls')
    return ImageFont.truetype(path, max(8, int(round(200 * target_w / float(bb[2] - bb[0])))))

def signal_mask():
    bw, gap, H = 3.0 * PT, 1.7 * PT, 11.2 * PT
    w, h = int(math.ceil(4 * bw + 3 * gap)) + 1, int(math.ceil(H)) + 1
    def fn(d, s):
        for i, r in enumerate([0.36, 0.56, 0.78, 1.0]):
            x0 = i * (bw + gap); bh = H * r
            d.rounded_rectangle((x0 * s, (H - bh) * s, (x0 + bw) * s - 1, H * s - 1), radius=0.9 * PT * s, fill=255)
    return _ss_mask(w, h, fn, 4)

def wifi_mask():
    Ro = 11.6 * PT
    w, h = int(math.ceil(2 * Ro * math.sin(math.radians(46)))) + 2, int(math.ceil(Ro + 0.8 * PT))
    cx, cy = w / 2.0, Ro + 0.3 * PT
    def fn(d, s):
        for ro, ri in [(11.6, 8.9), (7.5, 5.0), (3.5, 0)]:
            ro, ri = ro * PT, ri * PT
            d.pieslice(((cx - ro) * s, (cy - ro) * s, (cx + ro) * s, (cy + ro) * s), 224, 316, fill=255)
            if ri > 0: d.pieslice(((cx - ri) * s, (cy - ri) * s, (cx + ri) * s, (cy + ri) * s), 224, 316, fill=0)
    return _ss_mask(w, h, fn, 4)

def camera_mask(size):
    k0 = size / 24.0
    def fn(d, s):
        k = k0 * s
        d.rounded_rectangle((1.6 * k, 6.4 * k, 22.4 * k, 20.8 * k), radius=3.6 * k, fill=255)
        d.rounded_rectangle((8.0 * k, 3.6 * k, 16.0 * k, 7.6 * k), radius=1.5 * k, fill=255)
        d.ellipse(((12 - 5.0) * k, (13.6 - 5.0) * k, (12 + 5.0) * k, (13.6 + 5.0) * k), fill=0)
        d.ellipse(((12 - 3.4) * k, (13.6 - 3.4) * k, (12 + 3.4) * k, (13.6 + 3.4) * k), fill=255)
    return _ss_mask(size + 1, size + 1, fn, 4)

def _mean_lum(bgr):
    f = bgr.astype(np.float32) / 255.0
    return float((0.2126 * f[..., 2] + 0.7152 * f[..., 1] + 0.0722 * f[..., 0]).mean())


def flashlight_mask(size):
    k0 = size / 24.0
    def fn(d, s):
        k = k0 * s
        d.polygon([(6.4 * k, 2.0 * k), (17.6 * k, 2.0 * k), (15.4 * k, 8.2 * k), (8.6 * k, 8.2 * k)], fill=255)
        d.rounded_rectangle((5.6 * k, 0.9 * k, 18.4 * k, 3.6 * k), radius=1.1 * k, fill=255)
        d.rounded_rectangle((8.4 * k, 7.8 * k, 15.6 * k, 22.8 * k), radius=2.6 * k, fill=255)
        d.rounded_rectangle((10.9 * k, 11.8 * k, 13.1 * k, 15.8 * k), radius=1.1 * k, fill=0)
    return _ss_mask(size + 1, size + 1, fn, 4)

def glass_circle(scr, cx, cy, r):
    x0, y0, x1, y1 = int(cx - r - 4), int(cy - r - 4), int(cx + r + 5), int(cy + r + 5)
    reg = scr[y0:y1, x0:x1].copy()
    blur = cv2.GaussianBlur(reg, (0, 0), max(2.0, r * 0.3))
    glass = blur * 0.36 + np.array([22, 20, 22], np.float32) * 0.64
    w, h = x1 - x0, y1 - y0; lx, ly = cx - x0, cy - y0
    m = _ss_mask(w, h, lambda d, s: d.ellipse(((lx - r) * s, (ly - r) * s, (lx + r) * s - 1, (ly + r) * s - 1), fill=255), 3)
    scr[y0:y1, x0:x1] += (glass - reg) * m[..., None]

def _round_rect(w, h, rad, ss=4):
    return _ss_mask(int(w) + 1, int(h) + 1, lambda d, s: d.rounded_rectangle((0, 0, w * s - 1, h * s - 1), radius=rad * s, fill=255), ss)

def render_flat(w_bgr, now_dt, level):
    scr_bgr = resize_exact(w_bgr, SW, SH)
    scr = cv2.cvtColor(scr_bgr, cv2.COLOR_BGR2RGB).astype(np.float32)
    H, W = SH, SW
    k = lambda v: v * K
    if TIME_MODE == "fixed": time_str = FIXED_TIME
    else: time_str = f"{now_dt.hour % 12 or 12}:{now_dt.strftime('%M')}"
    date_str = now_dt.strftime("%A, ") + str(now_dt.day) + now_dt.strftime(" %B")
    zx0, zx1, zy0, zy1 = int(0.20 * W), int(0.80 * W), int(k(150)), int(k(262))
    if TEXT_STYLE == "tinted": tint = zone_text_rgb(scr_bgr, zx0, zy0, zx1 - zx0, zy1 - zy0)
    else: tint = zone_plain_rgb(scr_bgr, zx0, zy0, zx1 - zx0, zy1 - zy0)
    top_dark = _mean_lum(scr_bgr[:int(k(70))]) > STATUS_DARK_LUM
    bot_dark = _mean_lum(scr_bgr[int(0.97 * H):]) > STATUS_DARK_LUM
    ic = (20, 20, 22) if top_dark else (255, 255, 255)
    ic_b = (20, 20, 22) if bot_dark else (255, 255, 255)

    # dynamic island + camera lens
    iw, ih = k(178), k(49); ix, iy = k(208), k(19)
    put(scr, _round_rect(iw, ih, ih / 2), ix, iy, (0, 0, 0), 1.0)
    lr = k(8.5); lcx, lcy = k(360), iy + ih / 2
    lens = _ss_mask(int(2 * lr) + 4, int(2 * lr) + 4, lambda d, s: d.ellipse((2 * s, 2 * s, (2 + 2 * lr) * s, (2 + 2 * lr) * s), fill=255), 4)
    put(scr, lens, lcx - lens.shape[1] / 2, lcy - lens.shape[0] / 2, (14, 18, 38), 1.0)

    # status bar: carrier (left), signal/wifi/battery(+number) (right), thin line
    yc = k(41)
    f_c = fit_font(FONT_STATUS, "Jio", k(37))
    draw_text(scr, CARRIER_TEXT, f_c, k(84), k(45) + digit_h(f_c) / 2.0, ic, 0.98, align="l")
    pw, ph = k(44), k(24)
    px0, py0 = k(505), yc - ph / 2.0
    pill = _round_rect(pw, ph, k(7.5))
    put(scr, pill, px0, py0, ic, 0.36)
    colm = np.clip(pw * max(0, min(100, level)) / 100.0 - np.arange(pill.shape[1]), 0, 1).astype(np.float32)[None, :]
    put(scr, pill * colm, px0, py0, ic, 0.97)
    cap = _ss_mask(int(k(5)) + 2, int(k(10)) + 2, lambda d, s: d.rounded_rectangle((0, 0, k(2.6) * s, k(8) * s), radius=k(1.3) * s, fill=255), 4)
    put(scr, cap, px0 + pw + k(1.5), yc - k(4), ic, 0.5)
    f_n = ImageFont.truetype(FONT_NUM, max(8, int(round(k(19)))))
    draw_text(scr, str(int(level)), f_n, px0 + pw / 2.0, yc + digit_h(f_n) / 2.0, (18, 18, 20) if ic[0] > 128 else (240, 240, 240), 0.95)
    wm = wifi_mask(); put(scr, wm, k(462), yc - wm.shape[0] / 2.0 + k(0.5), ic, 1.0)
    sm = signal_mask(); put(scr, sm, k(416), yc + k(12) - sm.shape[0], ic, 1.0)
    ln = _round_rect(k(87), k(4), k(2))
    put(scr, ln, k(436), k(63), ic, 0.42)

    # date + time
    f_date = fit_font(FONT_DATE, "Friday, 17 October", k(277))
    draw_text(scr, date_str, f_date, W / 2.0, k(122.5) + digit_h(f_date) / 2.0, tint, 0.98)
    f100 = ImageFont.truetype(FONT_TIME, 100)
    f_time = ImageFont.truetype(FONT_TIME, int(round(100 * k(105) / digit_h(f100))))
    draw_text(scr, time_str, f_time, W / 2.0, k(208.5) + digit_h(f_time) / 2.0, tint, 0.99)

    # bottom: glass buttons + home indicator
    by, br = k(1186.5), k(48.5)
    glass_circle(scr, k(92), by, br); glass_circle(scr, W - k(92), by, br)
    fl = flashlight_mask(int(round(k(40)))); put(scr, fl, k(92) - fl.shape[1] / 2.0, by - fl.shape[0] / 2.0, (255, 255, 255), 0.98)
    cm = camera_mask(int(round(k(46)))); put(scr, cm, W - k(92) - cm.shape[1] / 2.0, by - cm.shape[0] / 2.0 + k(1), (255, 255, 255), 0.98)
    hw, hh = k(234), k(7)
    put(scr, _round_rect(hw, hh, hh / 2), (W - hw) / 2.0, k(1283.5), ic_b, 0.95)
    np.clip(scr, 0, 255, out=scr)
    return scr

# ---------------- photo compositing ----------------
_TPL = {}
def _load_template():
    if "T" in _TPL: return _TPL["T"]
    t = cv2.imread(TEMPLATE)
    if t is None: raise IOError("assets/template.jpg nahi mila")
    T = cv2.cvtColor(t, cv2.COLOR_BGR2RGB).astype(np.float32)
    h, w = T.shape[:2]
    ss = 4
    im = Image.new('L', (SCR_W * ss, SCR_H * ss), 0)
    ImageDraw.Draw(im).rounded_rectangle((0, 0, SCR_W * ss - 1, SCR_H * ss - 1), radius=RAD * ss, fill=255)
    m = np.asarray(im.resize((SCR_W, SCR_H), Image.BOX), np.float32) / 255.0
    M = np.zeros((h, w), np.float32); M[SCR_Y:SCR_Y + SCR_H, SCR_X:SCR_X + SCR_W] = m
    _TPL["T"] = (T, M, w, h)
    return _TPL["T"]

def render_mockup(w_bgr, now_dt, level):
    T, M, w, h = _load_template()
    flat = render_flat(w_bgr, now_dt, level)
    small = cv2.resize(flat, (SCR_W, SCR_H), interpolation=cv2.INTER_AREA)
    S = np.zeros((h, w, 3), np.float32); S[SCR_Y:SCR_Y + SCR_H, SCR_X:SCR_X + SCR_W] = small
    S = S * 0.985 + 1.5                                    # screen glass ka halka asar
    Mh = np.clip(cv2.dilate(M, np.ones((3, 3), np.uint8), iterations=1), 0, 1)
    out = T * (1 - Mh[..., None]) + np.array([4, 4, 5], np.float32) * Mh[..., None]
    out = out * (1 - M[..., None]) + S * M[..., None]
    if GRAIN > 0:
        g = np.random.normal(0, GRAIN, (h, w)).astype(np.float32)
        out += (g * M)[..., None]
    return cv2.cvtColor(np.clip(out, 0, 255).astype(np.uint8), cv2.COLOR_RGB2BGR)
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
    r = w / h
    target_ratio = target_w / target_h
    if r > target_ratio:
        nw = int(h * target_ratio)
        img = img[:, (w - nw) // 2: (w - nw) // 2 + nw]
    else:
        nh = int(w / target_ratio)
        img = img[(h - nh) // 2: (h - nh) // 2 + nh, :]
    return cv2.resize(img, (target_w, target_h), interpolation=cv2.INTER_LANCZOS4)

def apply_hd_enhance(img):
    lab = cv2.cvtColor(img, cv2.COLOR_BGR2LAB)
    l, a, b = cv2.split(lab)
    l_denoised = cv2.bilateralFilter(l, d=5, sigmaColor=30, sigmaSpace=30)
    a_denoised = cv2.GaussianBlur(a, (0, 0), 0.8)
    b_denoised = cv2.GaussianBlur(b, (0, 0), 0.8)
    blur1 = cv2.GaussianBlur(l_denoised, (0, 0), 1.0)
    sharp1 = cv2.addWeighted(l_denoised, 1.35, blur1, -0.35, 0)
    blur2 = cv2.GaussianBlur(sharp1, (0, 0), 2.5)
    sharp2 = cv2.addWeighted(sharp1, 1.15, blur2, -0.15, 0)
    l_final = np.clip(sharp2, 0, 255).astype(np.uint8)
    return cv2.cvtColor(cv2.merge([l_final, a_denoised, b_denoised]), cv2.COLOR_LAB2BGR)

_SRGB_BYTES = None
def save_optimized(cv2_img, output_path):
    global _SRGB_BYTES
    rgb = cv2.cvtColor(cv2_img, cv2.COLOR_BGR2RGB)
    pil_img = Image.fromarray(rgb)
    try:
        if _SRGB_BYTES is None:
            _SRGB_BYTES = cms.ImageCmsProfile(cms.createProfile('sRGB')).tobytes()
        pil_img.save(output_path, format='JPEG', quality=87, subsampling=0, icc_profile=_SRGB_BYTES, optimize=True)
    except Exception:
        pil_img.save(output_path, format='JPEG', quality=87, subsampling=0, optimize=True)

def draw_ink(draw, text, font, cx, cy):
    """Text ko is tarah rakho ke us ka INK box (font metrics nahi) centre (cx, cy) pe aaye."""
    l, t, r, btm = font.getbbox(text)
    draw.text((cx - (l + r) / 2, cy - (t + btm) / 2), text, font=font, fill=255)


def _contrast(l1, l2):
    hi, lo = max(l1, l2), min(l1, l2)
    return (hi + 0.05) / (lo + 0.05)

def _lin(c):
    c = np.asarray(c, dtype=np.float32)
    return np.where(c <= 0.03928, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)

def _rel_lum_rgb01(r, g, b):
    return float(0.2126 * _lin(r) + 0.7152 * _lin(g) + 0.0722 * _lin(b))

def zone_text_rgb(canvas_bgr, x, y, w, h):
    """iPhone lock-screen widget style: text ka rang us screen ke wallpaper ke apne rang se banta hai.
    Zone ka asal (numaya) rang nikalo -> dark hisse pe halka pastel shade, roshan hisse pe gehra shade.
    Phir contrast check: agar text saaf na dikhe to shade tab tak adjust hota hai jab tak TINT_MIN_CONTRAST na mile.
    Wallpaper me koi rang hi na ho (grey) to safed/kala."""
    zone = canvas_bgr[y:y + h, x:x + w].reshape(-1, 3).astype(np.float32) / 255.0
    b, g, r = zone[:, 0], zone[:, 1], zone[:, 2]
    mx = zone.max(axis=1); mn = zone.min(axis=1)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
    mean_lum = float(lum.mean())
    bg_rl = _rel_lum_rgb01(float(r.mean()), float(g.mean()), float(b.mean()))
    dark_bg = mean_lum < LUM_SPLIT
    wgt = sat * (0.35 + 0.65 * np.clip(1 - np.abs(lum - 0.5) * 2, 0, 1))
    wsum = float(wgt.sum())
    if wsum < 1e-3 or float((sat * wgt).sum() / wsum) < 0.10:
        v = 255 if dark_bg else 17
        return (v, v, v)
    # Hue ko circular average se nikalo (red wrap-around ka masla na ho)
    hh = np.array([colorsys.rgb_to_hls(float(rr), float(gg), float(bb))[0] for rr, gg, bb in zip(r[::7], g[::7], b[::7])], dtype=np.float32)
    ww = wgt[::7]
    hue = (np.arctan2((np.sin(hh * 2 * np.pi) * ww).sum(), (np.cos(hh * 2 * np.pi) * ww).sum()) / (2 * np.pi)) % 1.0
    sat_t = float(np.clip((sat * wgt).sum() / wsum * 1.0 + 0.25, 0.45, 0.85))
    best = None
    steps = [0.86, 0.90, 0.94, 0.97] if dark_bg else [0.26, 0.20, 0.15, 0.10]
    for L in steps:
        rr, gg, bb = colorsys.hls_to_rgb(float(hue), L, sat_t)
        c = _contrast(_rel_lum_rgb01(rr, gg, bb), bg_rl)
        best = (rr, gg, bb)
        if c >= TINT_MIN_CONTRAST: break
    else:
        # phir bhi saaf nahi: safed/kala me badal do
        best = (1, 1, 1) if dark_bg else (0.07, 0.07, 0.07)
    return (int(best[0] * 255), int(best[1] * 255), int(best[2] * 255))

def zone_plain_rgb(canvas_bgr, x, y, w, h):
    """White mode: hamesha safed text; sirf bohat roshan wallpaper pe kala (warna nazar na aaye)."""
    zone = canvas_bgr[y:y + h, x:x + w].astype(np.float32) / 255.0
    lum = float((0.2126 * zone[..., 2] + 0.7152 * zone[..., 1] + 0.0722 * zone[..., 0]).mean())
    return (17, 17, 17) if lum > BRIGHT_SPLIT else (255, 255, 255)

# ---------------- SMART: wallpaper features / grouping / background ----------------
def thumb_bgr(path, size=96):
    """Choti thumbnail (fast) — rang/roshni nikalne ke liye. Na khule to None."""
    try:
        with Image.open(path) as im:
            try: im.draft('RGB', (size * 2, size * 2))
            except Exception: pass
            im.load()
            if im.mode in ("RGBA", "LA") or (im.mode == "P" and "transparency" in im.info):
                im = Image.alpha_composite(Image.new("RGBA", im.size, (0, 0, 0, 255)), im.convert("RGBA"))
            im = im.convert('RGB'); im.thumbnail((size, size))
            return np.array(im)[:, :, ::-1].copy()
    except Exception:
        t = safe_imread(path)
        if t is None: return None
        hh, ww = t.shape[:2]; sc = size / max(hh, ww)
        return cv2.resize(t, (max(1, int(ww * sc)), max(1, int(hh * sc))), interpolation=cv2.INTER_AREA)

def color_features(bgr):
    """lum (0..1), sat (0..1), hue (0..1 ya None), lab-ish vector, top/bottom lum."""
    f = bgr.astype(np.float32) / 255.0
    b, g, r = f[..., 0], f[..., 1], f[..., 2]
    mx = f.max(axis=2); mn = f.min(axis=2)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
    wgt = sat * (0.3 + 0.7 * np.clip(1 - np.abs(lum - 0.5) * 2, 0, 1)) + 1e-6
    hs = np.array([colorsys.rgb_to_hls(float(a), float(c), float(d))[0] for a, c, d in zip(r.ravel()[::5], g.ravel()[::5], b.ravel()[::5])], dtype=np.float32)
    ws = wgt.ravel()[::5]
    sx, cx = float((np.sin(hs * 2 * np.pi) * ws).sum()), float((np.cos(hs * 2 * np.pi) * ws).sum())
    mean_sat = float(sat.mean())
    hue = (np.arctan2(sx, cx) / (2 * np.pi)) % 1.0 if mean_sat > 0.08 else None
    lab = cv2.cvtColor(bgr, cv2.COLOR_BGR2LAB).reshape(-1, 3).astype(np.float32).mean(axis=0)
    return {"lum": float(lum.mean()), "sat": mean_sat, "hue": hue, "lab": lab}


def pick_background(designs, bgf, grp_feats, recent):
    """Wallpapers ki roshni + rang ke hisab se sab se munasib background.
    Score = (screens aur background me roshni ka farq = screens ubhar kar dikhein) + (rang ka mail) - (haal hi me use hua)."""
    g_lum = float(np.mean([f["lum"] for f in grp_feats]))
    g_sat = float(np.mean([f["sat"] for f in grp_feats]))
    hues = [f["hue"] for f in grp_feats if f["hue"] is not None]
    g_hue = None
    if hues:
        sx = sum(np.sin(h * 2 * np.pi) for h in hues); cx = sum(np.cos(h * 2 * np.pi) for h in hues)
        g_hue = (np.arctan2(sx, cx) / (2 * np.pi)) % 1.0
    best, best_s = None, -1e9
    for name in designs:
        if name not in bgf: continue
        bf = bgf[name]
        contrast = min(abs(bf["lum"] - g_lum) / 0.35, 1.0)
        if g_hue is not None and bf["hue"] is not None and g_sat > 0.12:
            dh = abs(g_hue - bf["hue"]); dh = min(dh, 1 - dh)
            harmony = 1.0 - min(dh / 0.30, 1.0) if dh < 0.30 else max(0.0, 1.0 - abs(dh - 0.5) / 0.12) * 0.8  # analogous ya complementary
        else:
            harmony = 0.6   # grey/neutral: sab theek
        sc = 0.55 * contrast + 0.45 * harmony
        if name in recent:
            sc -= 0.35 * (1.0 - recent.index(name) / max(1, len(recent)) * 0.5)
        if sc > best_s: best, best_s = name, sc
    return best

def read_recent():
    try:
        with open(BG_RECENT, "r") as fh: return [l.strip() for l in fh if l.strip()]
    except Exception:
        return []

def write_recent(lst):
    try:
        with open(BG_RECENT, "w") as fh: fh.write("\n".join(lst[-4:]))
    except Exception:
        pass


def list_inputs():
    """Har tarah ki image: extension ya file ke andar ka asal format (hidden .naam wali files bhi). Video/temp files skip."""
    skip_names = {'.nomedia', '.bg_rotation.txt', '.bg_recent.txt', '.ff_tmp_read.png'}
    out = []
    for f in os.listdir(I):
        p = os.path.join(I, f)
        if not os.path.isfile(p) or f in skip_names or "_temp" in f or f.endswith(('.del', '.tmp', '.part', '.crdownload')): continue
        if f.lower().endswith(IMG_EXTS):
            out.append(f); continue
        if os.path.splitext(f)[1].lower() in ('.mp4', '.mov', '.mkv', '.avi', '.webm', '.3gp', '.txt', '.json', '.py', '.zip'): continue
        t = sniff_type(p)   # naam ajeeb ho (bina extension) magar andar image ho
        if t in ("jpeg", "png", "webp") or t.startswith("heif/avif"): out.append(f)
    return out



def process_one(f, idx, t_now, is_hd, level, crop_916=False):
    raw_p = os.path.join(I, f)
    raw = safe_imread(raw_p)
    if raw is None:
        return 'unreadable', idx, t_now
    w_ready = resize_exact(raw) if crop_916 else raw   # Crop: 1080x1920 ready; Original: asal size ready (baghair text ke)
    if is_hd: w_ready = apply_hd_enhance(w_ready)
    fin = render_mockup(w_ready, datetime.now(), level)
    mock_path = os.path.join(O, f"{idx}.jpg")
    wall_path = os.path.join(R, f"{idx + 1}.jpg")
    mt, wt = t_now + t_step, t_now + 2*t_step
    save_optimized(fin, mock_path); os.utime(mock_path, (mt, mt))
    save_optimized(w_ready, wall_path); os.utime(wall_path, (wt, wt))
    def ok(path):
        try:
            if not os.path.exists(path) or os.path.getsize(path) < 100: return False
            with Image.open(path) as im: im.verify()
            return True
        except Exception:
            return False
    if not (ok(mock_path) and ok(wall_path)): raise IOError("save verify failed")
    safe_scan(mock_path); safe_scan(wall_path)
    gallery_copy_outputs([(mock_path, mt), (wall_path, wt)])
    try: os.remove(raw_p)
    except Exception: pass
    return 'ok', idx + 2, wt

action = sys.argv[1] if len(sys.argv) >= 2 else 'process'

if action == 'process':
    display_header()
    os.makedirs(O, exist_ok=True); os.makedirs(R, exist_ok=True)
    if not os.path.isdir(I):
        print(f" {C2}✖ Input folder nahi mila: {P_NUM}_wallpapers{ENDC}"); sys.exit()
    imgs = list_inputs()
    imgs.sort(key=lambda f: os.path.getctime(os.path.join(I, f)))
    if len(imgs) == 0:
        print(f" {C2}✖ {P_NUM}_wallpapers me images nahi mili.{ENDC}"); sys.exit()
    print(f" {C5}✿ {BOLD}S e l e c t  M o d e{ENDC}")
    print(f" {C1}[1]{ENDC} HD Mode (Galaxy AI Enhance) - {C4}[DEFAULT]{ENDC}")
    print(f" {C1}[2]{ENDC} Normal (Fast KB Pack)")
    is_hd = (input(f"\n {C4}➜ {ENDC}").strip() != '2')
    print(f" {C5}✿ {BOLD}S e l e c t  S i z e{ENDC}")
    print(f" {C1}[1]{ENDC} Original Size - {C4}[DEFAULT]{ENDC}")
    print(f" {C1}[2]{ENDC} Crop 9:16 (1080x1920)")
    crop_916 = (input(f"\n {C4}➜ {ENDC}").strip() == '2')
    for _ff in (FONT_TIME, FONT_DATE, FONT_STATUS, FONT_NUM, TEMPLATE):
        if not os.path.exists(_ff):
            print(f" {C2}✖ File missing: {os.path.relpath(_ff, B)}{ENDC}"); sys.exit()
    print(f" {C4}✔ Stand iPhone mockup engine ready!{ENDC}")

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
    _slots = max(1, 2 * len(imgs))
    _tmode = _time_mode()
    if _tmode == "future":
        # v155 Time Mode FUTURE — purana (pre-v154) tareeqa,
        # do behtariyon ke sath: step +10s/file (pehle 60s tha)
        # or tracker ki bhaag-daud par bandish: tracker abhi
        # se 600s se zyada aage ho to use nazar-andaz karke
        # anchor abhi par (self-healing pull-back); tracker me
        # aakhri waqt neeche wapas likha jata hai.
        _base = _now0
        if os.path.exists(T_F):
            try:
                with open(T_F,"r") as f:
                    _lt = float(f.read().strip())
                    if _lt > _base and _lt <= _now0 + 600: _base = _lt
            except: pass
        t_step = 10.0
        cur_t = _base
    elif _tmode == "current":
        # v155 Time Mode CURRENT — tracker na parho na likho:
        # files abhi tak barhti hui, 1s step, aakhri file <= abhi.
        t_step = 1.0
        cur_t = _now0 - t_step * _slots
    else:
        # PAST (default) — v154 wala proven tareeqa, bilkul waisa:
        # ladder hamesha past me rehti hai; nayi mtime ka
        # nishana abhi se ~60s pehle; tracker me PAST value ho
        # to nayi ladder us ke baad shuru hoti hai.
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
        # v155 Past SELF-SIZING: ye run jitni outputs banata
        # hai (slots — triples x3 sameet) utni hi time-jagah
        # leta hai: har file 1.0s. Block pichle block ke end
        # (tracker) ke BAAD se shuru ho kar end (abhi-60) par
        # khatam hota hai. Sirf gin-ti + hisaab hai, ek dafa
        # per run — koi scan ya hash nahi, fauri ho jata hai.
        # Pathological back-to-back surat me step 0.5s se
        # neeche jaye to start deeper past me (end - slots
        # x 0.5) — future me kabhi nahi barhna. Tracker me
        # aakhir me is run ka aakhri waqt hi likha jata hai
        # (purani convention) — agla run wahi se aage.
        _start = max(_end_soft - _slots * 1.0, _floor)
        t_step = (_end_soft - _start) / _slots
        if t_step < 0.5:
            _start = _end_soft - _slots * 0.5
            t_step = 0.5
        cur_t = _start
    print("")
    desc_text = "✨ HD Mode " if is_hd else "⚙️ Normal "
    desc_text += "| Crop 9:16 " if crop_916 else "| Original "
    processed_count, skip_log = 0, []
    set_wakelock(True)
    for f in tqdm(imgs, desc=desc_text, bar_format=f" {C1}{{desc}}{ENDC} {C5}|{ENDC}{{bar:20}}{C5}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"):
        level = random.randint(*BATTERY_RANGE)
        success, last_err = False, None
        for attempt in range(3):
            try:
                status, new_idx, new_t = process_one(f, s_idx, cur_t, is_hd, level, crop_916)
                if status == 'ok':
                    s_idx, cur_t = new_idx, new_t; processed_count += 1; success = True; break
                last_err = "padhi nahi gayi: " + READ_ERR.get(os.path.join(I, f), "?"); break
            except Exception as e:
                last_err = f"{type(e).__name__}: {str(e)[:50]}"
                for q in (os.path.join(O, f"{s_idx}.jpg"), os.path.join(R, f"{s_idx + 1}.jpg")):
                    if os.path.exists(q):
                        try: os.remove(q)
                        except: pass
                gc.collect(); time.sleep(0.3)
        if not success: skip_log.append(f"{f} ({last_err})")
        gc.collect()
    set_wakelock(False)
    if processed_count > 0:
        try:
            if _tmode != "current":
                with open(T_F, "w") as fh: fh.write(str(cur_t))
        except: pass
        print(f"\n {C4}✔ {processed_count} Mockup + {processed_count} Wallpapers ready!{ENDC}")
    else:
        print(f"\n {C5}✔ Sab already set hai.{ENDC}")
    if skip_log:
        print(f" {C2}⚠ {len(skip_log)} skip (agle run me retry):{ENDC}")
        for s in skip_log[:8]: print(f"   {C2}- {s}{ENDC}")
    print("")

if action == 'del':
    import shutil
    BACKUP = f"/storage/emulated/0/.mockup_backup_{P_NUM}"
    def _files(d):
        if not os.path.isdir(d): return []
        return [f for f in os.listdir(d) if os.path.isfile(os.path.join(d, f))]
    def _backup_and_clear(d, tag):
        files = _files(d)
        if not files:
            print(f" {C5}ℹ {tag}: pehle se khaali hai.{ENDC}"); return 0
        bd = os.path.join(BACKUP, tag)
        if os.path.isdir(bd): shutil.rmtree(bd, ignore_errors=True)
        os.makedirs(bd, exist_ok=True)
        n = 0
        for f in files:
            try:
                shutil.copy2(os.path.join(d, f), os.path.join(bd, f))
                os.remove(os.path.join(d, f)); n += 1
            except Exception: pass
        print(f" {C4}✔ {tag}: {n} files delete (backup ban gaya).{ENDC}")
        return n
    def tm_pairs_cleanup(parity):
        # TermuxMod "Pairs" copies ki safai — $HOME delete ke sath wahan ki
        # copies bhi jayen: 'odd' = sirf mockups, 'even' = sirf wallpapers,
        # 'all' = dono. _backup_and_clear sirf poora folder leta hai, single
        # files nahi — is liye Pairs files seedha remove + folder scan.
        # Project/Pairs folder na ho to khamoshi se skip.
        try:
            dp = os.path.join(TM_ROOT, f"Project {P_NUM}", "Pairs")
            if not os.path.isdir(dp): return
            _exts = ('.jpg', '.jpeg', '.png', '.webp', '.heic', '.avif')
            for f in os.listdir(dp):
                if f.startswith('.') or not f.lower().endswith(_exts): continue
                stem = os.path.splitext(f)[0]
                if not stem.isdigit(): continue
                n = int(stem)
                if parity == 'odd' and n % 2 == 0: continue
                if parity == 'even' and n % 2 == 1: continue
                try: os.remove(os.path.join(dp, f))
                except Exception: pass
            safe_scan_dir(dp)
        except Exception:
            pass
    display_header()
    print(f" {C5}✿ {BOLD}D e l e t e  M e n u{ENDC}")
    print(f" {C1}[1]{ENDC} Tayaar Maal (Mockups + Ready Wallpapers)")
    print(f" {C1}[2]{ENDC} Sirf Mockups")
    print(f" {C1}[3]{ENDC} Input Wallpapers")
    print(f" {C1}[4]{ENDC} Warehouse (Ready_Upload_Batches)")
    print(f" {C1}[5]{ENDC} Backup se Wapas lao (Restore)")
    print(f" {C1}[0]{ENDC} Cancel")
    ch = input(f"\n {C4}➜ {ENDC}").strip()
    if ch == '1':
        _backup_and_clear(O, "Mockups"); _backup_and_clear(R, "Ready")
        tm_pairs_cleanup('all')
    elif ch == '2':
        _backup_and_clear(O, "Mockups")
        tm_pairs_cleanup('odd')
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
                print(f" {C4}✔ Warehouse saaf (backup ban gaya).{ENDC}")
            except Exception as e:
                print(f" {C2}✖ Warehouse masla: {e}{ENDC}")
        else:
            print(f" {C5}ℹ Warehouse pehle se khaali hai.{ENDC}")
    elif ch == '5':
        if not os.path.isdir(BACKUP):
            print(f" {C2}✖ Koi backup nahi mila.{ENDC}")
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
            if total: print(f" {C4}✔ Restore ho gaya: {total} items wapas.{ENDC}")
            else: print(f" {C5}ℹ Wapas lane ko kuch nahi (ya sab pehle se mojood hai).{ENDC}")
    else:
        print(f" {C5}ℹ Cancel.{ENDC}")
    try: safe_scan_dir(B)
    except Exception: pass
    print("")

if action == 'batch':
    import shutil
    display_header()
    files = []
    for d in (O, R):
        if os.path.isdir(d):
            files += [(os.path.join(d, f), f) for f in os.listdir(d) if os.path.isfile(os.path.join(d, f))]
    if not files:
        print(f" {C2}✖ Batch ke liye Mockups/Ready me kuch nahi hai.{ENDC}")
    else:
        os.makedirs(W_H, exist_ok=True)
        bname = _batch_pick_name("Batch_" + datetime.now().strftime("%d-%b_%I-%M%p"))
        bd = os.path.join(W_H, bname)
        os.makedirs(bd, exist_ok=True)
        n = 0
        for src, f in files:
            try: shutil.copy2(src, os.path.join(bd, f)); n += 1
            except Exception: pass
        print(f" {C4}✔ {bname}: {n} files copy ho gayi (Ready_Upload_Batches).{ENDC}")
        try: safe_scan_dir(B)
        except Exception: pass
    print("")
