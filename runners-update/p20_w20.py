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
    # Fire-and-forget: app ke session-less process me ye command atak
    # sakti hai — run ko is ke jawab ka intezar kabhi nahi karna.
    try:
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
import math, urllib.request

# ====================== PROJECT 20 : DEPTH TIME (time subject ke PEECHE) - sab kuch is .py me ======================
P_NUM = sys.argv[2] if len(sys.argv) > 2 else "20"
B = os.environ.get("MOCKUP_BASE", os.path.dirname(os.path.abspath(__file__)))
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")
FONT_DIR = os.path.join(B, "fonts")
MODEL_DIR = os.path.join(B, "models")
MODEL_PATH = os.path.join(MODEL_DIR, "u2netp.onnx")
MODEL_URL = "https://github.com/danielgatis/rembg/releases/download/v0.0.0/u2netp.onnx"
T_F = "/storage/emulated/0/.mockup_time_tracker.txt"

FONT_TIME   = next((os.path.join(FONT_DIR, n) for n in ("SF-Pro-Display-Bold.otf", "SF-Pro-Rounded-Bold.otf", "SF-Pro-Display-Bold.ttf", "SF-Pro-Rounded-Bold.ttf") if os.path.exists(os.path.join(FONT_DIR, n))),
                   os.path.join(FONT_DIR, "InterDisplay-Bold.otf"))   # asli SF Pro fonts/ me rakho to wahi use hoga, warna Inter Display
FONT_DATE   = os.path.join(FONT_DIR, "Inter-Medium.otf")
FONT_STATUS = os.path.join(FONT_DIR, "Inter-SemiBold.otf")
FONT_NUM    = FONT_STATUS

TIME_WGHT = 700; TIME_SLANT = 0; TIME_WR_STACK = 0.34; TIME_ROUND = 0.006
TIME_STEP_MIN = 1          # har mockup pe pichle se 1 minute aage
TIME_ALPHA_DARK, TIME_ALPHA_LIGHT = 0.78, 0.72
BG_BLUR     = 38
BATTERY_RANGE = (40, 100)
LUM_SPLIT = 0.55; BRIGHT_SPLIT = 0.78; TINT_MIN_CONTRAST = 3.6
STATUS_DARK_LUM = 0.70

SC = 1.5
CW, CH = int(round(1080 * SC)), int(round(1440 * SC))
PX0, PY0, PW, PH_, PR = 293, 97, 494, 1098, 46
SWF, SHF = int(round(PW * SC)), int(round(PH_ * SC))
K = lambda v: v * SC
PT = 1.8

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

def tint15(full_bgr, zone_bgr):
    """Poori wallpaper ka numaya (accent) rang -> time/date/icons ka rang (iPhone widget jesa).
    Wallpaper me rang na ho to safed/kala. Contrast zone (time ke piche) ke hisab se check hota hai."""
    f = cv2.resize(full_bgr, (72, 128), interpolation=cv2.INTER_AREA).astype(np.float32) / 255.0
    b, g, r = f[..., 0], f[..., 1], f[..., 2]
    mx = f.max(axis=2); mn = f.min(axis=2)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
    wgt = sat * (0.3 + 0.7 * np.clip(1 - np.abs(lum - 0.5) * 2, 0, 1)) * (sat > 0.18)
    zl = zone_bgr.astype(np.float32) / 255.0
    zr, zg, zb = float(zl[..., 2].mean()), float(zl[..., 1].mean()), float(zl[..., 0].mean())
    zlum = 0.2126 * zr + 0.7152 * zg + 0.0722 * zb
    dark_bg = zlum < 0.52
    bg_rl = _rel_lum_rgb01(zr, zg, zb)
    ws = float(wgt.sum())
    if ws < 6.0:
        return (255, 255, 255) if dark_bg else (17, 17, 17)
    hh = np.array([colorsys.rgb_to_hls(float(a), float(c), float(d))[0] for a, c, d in zip(r.ravel()[::3], g.ravel()[::3], b.ravel()[::3])], np.float32)
    w3 = wgt.ravel()[::3]
    hue = (np.arctan2((np.sin(hh * 2 * np.pi) * w3).sum(), (np.cos(hh * 2 * np.pi) * w3).sum()) / (2 * np.pi)) % 1.0
    sat_t = float(np.clip((sat * wgt).sum() / ws + 0.25, 0.6, 0.92))
    best = None
    for L in ([0.70, 0.76, 0.82, 0.90] if dark_bg else [0.36, 0.30, 0.24, 0.16]):
        rr, gg, bb = colorsys.hls_to_rgb(float(hue), L, sat_t)
        best = (rr, gg, bb)
        if _contrast(_rel_lum_rgb01(rr, gg, bb), bg_rl) >= TINT_MIN_CONTRAST: break
    else:
        best = (1, 1, 1) if dark_bg else (0.07, 0.07, 0.07)
    return (int(best[0] * 255), int(best[1] * 255), int(best[2] * 255))


# ---------------- SEGMENTATION (subject mask) ----------------
_SEG = {"mode": None, "sess": None, "name": None}

def _try_import_ort():
    try:
        import importlib; importlib.invalidate_caches()
        import onnxruntime as ort
        return ort
    except Exception:
        return None

def _download_model():
    os.makedirs(MODEL_DIR, exist_ok=True)
    tmp = MODEL_PATH + ".part"
    for attempt in range(3):
        try:
            print(f" {C5}⬇ Model download ho raha hai (u2netp ~4.5MB) try {attempt + 1}/3 ...{ENDC}")
            req = urllib.request.Request(MODEL_URL, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=60) as r, open(tmp, "wb") as fo:
                shutil.copyfileobj(r, fo)
            if os.path.getsize(tmp) > 3_000_000:
                os.replace(tmp, MODEL_PATH); return True
        except Exception as e:
            print(f" {C2}  download fail: {type(e).__name__}{ENDC}")
        time.sleep(1.5)
    try: os.remove(tmp)
    except Exception: pass
    return False

def ensure_seg():
    """AI mask (onnxruntime + u2netp) ho to wahi; model na ho to pehle download; warna GrabCut (no model)."""
    ort = _try_import_ort()
    if ort is None:
        print(f" {C5}⬇ onnxruntime nahi mila — Termux me install try kar raha hun...{ENDC}")
        for cmd in ("pkg install -y tur-repo python-onnxruntime", "pkg install -y python-onnxruntime", "pip install onnxruntime"):
            try:
                subprocess.run(cmd, shell=True, timeout=900, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            except Exception:
                pass
            ort = _try_import_ort()
            if ort is not None: break
    if ort is None:
        print(f" {C5}ℹ onnxruntime install nahi hua → GrabCut mode (bina model).{ENDC}")
        _SEG.update(mode="grabcut", name="GrabCut"); return
    if not (os.path.exists(MODEL_PATH) and os.path.getsize(MODEL_PATH) > 3_000_000):
        if not _download_model():
            print(f" {C5}ℹ Model download nahi hua → GrabCut mode.{ENDC}")
            _SEG.update(mode="grabcut", name="GrabCut"); return
    try:
        so = ort.SessionOptions(); so.intra_op_num_threads = 2
        _SEG["sess"] = ort.InferenceSession(MODEL_PATH, so, providers=["CPUExecutionProvider"])
        _SEG.update(mode="onnx", name="AI (u2netp)")
    except Exception as e:
        print(f" {C5}ℹ Model load nahi hua ({type(e).__name__}) → GrabCut mode.{ENDC}")
        _SEG.update(mode="grabcut", name="GrabCut")

def onnx_mask(bgr):
    sess = _SEG["sess"]; h, w = bgr.shape[:2]
    x = cv2.resize(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB), (320, 320), interpolation=cv2.INTER_AREA).astype(np.float32) / 255.0
    x = (x - np.array([0.485, 0.456, 0.406], np.float32)) / np.array([0.229, 0.224, 0.225], np.float32)
    x = np.ascontiguousarray(x.transpose(2, 0, 1)[None].astype(np.float32))
    out = np.asarray(sess.run(None, {sess.get_inputs()[0].name: x})[0]).reshape(320, 320)
    mn, mx = float(out.min()), float(out.max())
    out = (out - mn) / (mx - mn + 1e-8)
    out = cv2.resize(out, (w, h), interpolation=cv2.INTER_CUBIC)
    return np.clip((out - 0.30) / 0.40, 0, 1).astype(np.float32)

def grabcut_mask(bgr):
    h, w = bgr.shape[:2]; sc = 320.0 / w
    sm = cv2.resize(bgr, (320, int(round(h * sc))), interpolation=cv2.INTER_AREA)
    H, W = sm.shape[:2]
    lab = cv2.cvtColor(sm, cv2.COLOR_BGR2LAB).astype(np.float32)
    bw, bh = max(4, int(W * 0.04)), max(6, int(H * 0.05))
    border = np.concatenate([lab[:bh].reshape(-1, 3), lab[:, :bw].reshape(-1, 3), lab[:, -bw:].reshape(-1, 3)])
    crit = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 12, 1.0)
    _, _, cen = cv2.kmeans(border, 3, None, crit, 2, cv2.KMEANS_PP_CENTERS)
    dist = np.min([np.linalg.norm(lab - c, axis=2) for c in cen], axis=0)
    d = cv2.GaussianBlur(dist, (0, 0), 2.0)
    mxd = float(d.max())
    if mxd < 18: return None
    d8 = np.clip(d * 255.0 / mxd, 0, 255).astype(np.uint8)
    t, _ = cv2.threshold(d8, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    thr = max(float(t) * mxd / 255.0, 14.0)
    fg0 = (d > thr).astype(np.uint8)
    fg0 = cv2.morphologyEx(fg0, cv2.MORPH_OPEN, np.ones((5, 5), np.uint8))
    frac = float(fg0.mean())
    if frac < 0.015 or frac > 0.80: return None
    gm = np.full((H, W), cv2.GC_PR_BGD, np.uint8)
    gm[fg0 > 0] = cv2.GC_PR_FGD
    gm[cv2.erode(fg0, np.ones((5, 5), np.uint8), iterations=2) > 0] = cv2.GC_FGD
    bgm = cv2.erode((d < thr * 0.45).astype(np.uint8), np.ones((3, 3), np.uint8)) > 0
    gm[bgm] = cv2.GC_BGD
    try:
        cv2.grabCut(sm, gm, None, np.zeros((1, 65), np.float64), np.zeros((1, 65), np.float64), 3, cv2.GC_INIT_WITH_MASK)
        m = ((gm == cv2.GC_FGD) | (gm == cv2.GC_PR_FGD)).astype(np.uint8)
    except Exception:
        m = fg0
    n, lbl, st, _ = cv2.connectedComponentsWithStats(m, connectivity=8)
    keep = np.zeros_like(m)
    for i in range(1, n):
        if st[i, cv2.CC_STAT_AREA] >= 0.004 * H * W: keep[lbl == i] = 1
    keep = cv2.morphologyEx(keep, cv2.MORPH_CLOSE, np.ones((7, 7), np.uint8))
    fr = float(keep.mean())
    if fr < 0.015 or fr > 0.85: return None
    mf = cv2.GaussianBlur(keep.astype(np.float32), (0, 0), 1.3)
    return cv2.resize(mf, (w, h), interpolation=cv2.INTER_CUBIC).clip(0, 1)

def subject_mask(scr_bgr):
    """screen-size (SHF,SWF) float mask 0..1 | None (subject na mila -> time aage rahega)"""
    h, w = scr_bgr.shape[:2]
    small = cv2.resize(scr_bgr, (w // 2, h // 2), interpolation=cv2.INTER_AREA)
    m = None
    if _SEG["mode"] == "onnx":
        try: m = onnx_mask(small)
        except Exception: m = None
    if m is None and _SEG["mode"] in ("onnx", "grabcut"):
        try: m = grabcut_mask(small)
        except Exception: m = None
    if m is None: return None
    m = cv2.resize(m, (w, h), interpolation=cv2.INTER_CUBIC).clip(0, 1)
    fr = float(m.mean())
    if fr < 0.01 or fr > 0.88: return None
    return m

# ---------------- condensed big time ----------------
_TFONT = {}
def _tfont(cap):
    key = int(round(cap))
    if key not in _TFONT:
        f100 = ImageFont.truetype(FONT_TIME, 100)
        f = ImageFont.truetype(FONT_TIME, int(round(100 * cap / digit_h(f100))))
        try: f.set_variation_by_axes([TIME_WGHT])
        except Exception: pass
        _TFONT[key] = f
    return _TFONT[key]

_GL = {}
def _glyph(ch, cap, wr):
    key = (ch, int(round(cap)), round(wr, 3))
    if key in _GL: return _GL[key]
    C2x = cap * 2.0
    f = _tfont(C2x); pad = int(C2x * 0.3); base = int(C2x * 1.25)
    def ink(c):
        im = Image.new('L', (int(C2x * 1.4) + 2 * pad, int(C2x * 1.7)), 0)
        ImageDraw.Draw(im).text((pad, base), c, font=f, fill=255, anchor='ls')
        a = np.array(im); ys, xs = np.where(a > 40)
        return a, xs.min(), xs.max(), ys.min(), ys.max()
    a0, x0, x1, _, _ = ink("0")
    fx = wr * cap / float(x1 - x0 + 1)
    a, x0, x1, y0, y1 = ink(ch)
    a = a[y0:y1 + 1, x0:x1 + 1]
    if ch == "4" and TIME_SLANT:
        hh_, ww_ = a.shape; sh_ = int(round(hh_ * math.tan(math.radians(TIME_SLANT))))
        pad_ = np.zeros((hh_, ww_ + sh_), np.uint8)
        for r_ in range(hh_):
            off = int(round(sh_ * (1 - r_ / float(hh_ - 1)))); pad_[r_, off:off + ww_] = a[r_]
        a = pad_
    nw = max(2, int(round(a.shape[1] * fx))); nh = max(2, int(round(a.shape[0] / 2.0)))
    a = cv2.resize(a, (nw, nh), interpolation=cv2.INTER_AREA)
    pd = int(cap * 0.05) + 2
    a = cv2.copyMakeBorder(a, pd, pd, pd, pd, cv2.BORDER_CONSTANT, value=0)
    af = cv2.GaussianBlur(a.astype(np.float32) / 255.0, (0, 0), max(0.7, cap * TIME_ROUND))
    a = np.clip((af - 0.5) * 4.0 + 0.5, 0, 1)                      # corners halke gol (reference jesa)
    out = (a.astype(np.float32), (y0 - base) / 2.0 - pd, pd)
    _GL[key] = out
    return out

def _iw(it): return it[1].shape[1] - 2 * it[4]

def _row_items(text, cap, wr):
    items = []; gap = 0.05 * cap
    for i, ch in enumerate(text):
        if ch == ":":
            dd = max(6, int(round(0.085 * cap)))
            m = _ss_mask(dd + 2, dd + 2, lambda d, s: d.ellipse((1 * s, 1 * s, (1 + dd) * s, (1 + dd) * s), fill=255), 4)
            items.append(("c", m, 0, 0.09 * cap, 0))
        else:
            m, yo, pd_ = _glyph(ch, cap, wr)
            items.append(("d", m, yo, gap, pd_))
    return items

def _row_width(items):
    return sum(_iw(it) for it in items) + sum((items[i][3] if items[i][0] == "c" or (i + 1 < len(items) and items[i + 1][0] == "c") else items[i][3]) for i in range(len(items) - 1))

def _gap_after(items, i):
    if i >= len(items) - 1: return 0.0
    a, b = items[i], items[i + 1]
    return 0.09 * (a[1].shape[0]) if False else (b[3] if b[0] == "c" else a[3])

def _fit_wr(text, cap, target_w):
    wr = 0.22
    for _ in range(3):
        items = _row_items(text, cap, wr)
        w = sum(_iw(it) for it in items) + sum(_gap_after(items, i) for i in range(len(items)))
        wr *= target_w / float(max(w, 1))
    return float(np.clip(wr, 0.12, 0.5))

def draw_row(tm, text, cap, wr, x_anchor, top_y, align):
    items = _row_items(text, cap, wr)
    total = sum(_iw(it) for it in items) + sum(_gap_after(items, i) for i in range(len(items)))
    x = {"c": x_anchor - total / 2.0, "l": x_anchor, "r": x_anchor - total}[align]
    for i, it in enumerate(items):
        m = it[1]
        if it[0] == "c":
            for off in (0.30, 0.70):
                put(tm, m, x, top_y + off * cap - m.shape[0] / 2.0, (1, 1, 1), 1.0)
        else:
            put(tm, m, x - it[4], top_y + cap + it[2], (1, 1, 1), 1.0)
        x += _iw(it) + _gap_after(items, i)
    return total

# geometry in "ref px" (relative to the phone screen)
WIDE_BOX    = (52, 148, 442, 528)      # x0,y0,x1,y1
STACK_L     = (57, 148, 238, 743)
STACK_R     = (256, 148, 437, 743)

def pick_layout(m, scr_bgr):
    def cov(box):
        x0, y0, x1, y1 = [int(round(K(v))) for v in box]
        return float(m[y0:y1, x0:x1].mean()) if m is not None else 0.0
    def busy(box):
        x0, y0, x1, y1 = [int(round(K(v))) for v in box]
        g = cv2.cvtColor(scr_bgr[y0:y1, x0:x1], cv2.COLOR_BGR2GRAY)
        return float(cv2.Laplacian(cv2.resize(g, (max(8, g.shape[1] // 4), max(8, g.shape[0] // 4))), cv2.CV_32F).var())
    if m is None:
        bl, br = busy(STACK_L), busy(STACK_R); bw = busy(WIDE_BOX)
        if min(bl, br) < 0.6 * bw: return ("stackL" if bl <= br else "stackR")
        return "wide"
    cl, cr, cw = cov(STACK_L), cov(STACK_R), cov(WIDE_BOX)
    if min(cl, cr) < 0.03 and max(cl, cr) > 0.10: return "stackL" if cl < cr else "stackR"
    return "wide"

def build_time_mask(layout, hh, mm):
    tm = np.zeros((SHF, SWF, 3), np.float32)
    if layout == "wide":
        cap = K(250); x0, x1, y0 = WIDE_BOX[0], WIDE_BOX[2], WIDE_BOX[1]
        txt = f"{hh:02d}:{mm:02d}"
        wr = float(np.clip(_fit_wr(txt, cap, K(x1 - x0 - 10)), 0.30, 0.52))
        draw_row(tm, txt, cap, wr, K((x0 + x1) / 2.0), K(y0), "c")
    else:
        cap = K(262); wr = TIME_WR_STACK
        if layout == "stackL": ax, al = K(57), "l"
        else: ax, al = K(437), "r"
        draw_row(tm, f"{hh:02d}", cap, wr, ax, K(148), al)
        draw_row(tm, f"{mm:02d}", cap, wr, ax, K(148 + 262 + 22), al)
    return tm[..., 0]

def glass_time(scr, tm, subj):
    """iOS Liquid Glass time: refraction + chromatic edge, bevel specular, sheen, inner shadow, soft shadow."""
    H, W = tm.shape
    occ = 1.0 - (subj if subj is not None else 0.0)
    mask = np.clip(tm, 0, 1)
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    inside = (mask > 0.5).astype(np.uint8)
    # sub-pixel distance: soft mask se (staircase nahi)
    dist = cv2.distanceTransform(inside, cv2.DIST_L2, 5)
    dist = cv2.GaussianBlur(dist, (0, 0), K(1.4)) + (mask - inside.astype(np.float32)) * 0.5
    lum = float((0.2126 * scr[..., 0] + 0.7152 * scr[..., 1] + 0.0722 * scr[..., 2]).mean() / 255.0)
    bright = lum > 0.62
    # bevel height-map -> normals (gol kinare, andar se flat)
    bevel = K(9.0)
    hmap = np.sqrt(np.clip(dist / bevel, 0, 1))
    hmap = cv2.GaussianBlur(hmap, (0, 0), K(2.2))
    hy, hx = np.gradient(hmap)
    sc_n = K(9.0) * 0.55
    nxv, nyv = -hx * sc_n, -hy * sc_n
    nz = np.ones_like(nxv); inv = 1.0 / np.sqrt(nxv ** 2 + nyv ** 2 + nz ** 2)
    Nx, Ny, Nz = nxv * inv, nyv * inv, nz * inv
    # saaya (bahar, halka)
    sh = cv2.GaussianBlur(mask, (0, 0), K(8)); sh = cv2.warpAffine(sh, np.float32([[1, 0, K(1)], [0, 1, K(6)]]), (W, H))
    scr -= scr * ((sh * (1 - mask) * (0.24 if not bright else 0.18)) * occ)[..., None]
    # refraction + chromatic aberration (kinare par)
    bg = cv2.GaussianBlur(scr, (0, 0), K(3.2))
    k = np.clip(1.0 - dist / K(15), 0, 1) ** 1.5
    gxs = -hx * K(150); gys = -hy * K(150)                           # normal ki disha me displacement
    chans = []
    for ci, amt in enumerate((1.00, 1.14, 1.28)):
        mx = (xs + gxs * amt * k * 0.35).astype(np.float32); my = (ys + gys * amt * k * 0.35).astype(np.float32)
        chans.append(cv2.remap(bg[..., ci], mx, my, cv2.INTER_LINEAR, borderMode=cv2.BORDER_REFLECT))
    refr = np.dstack(chans)
    gray = refr.mean(axis=2, keepdims=True)
    refr = gray + (refr - gray) * 1.30
    yy = (ys / float(H))[..., None]
    if bright: glass = refr * 0.74 + 255.0 * 0.03
    else: glass = refr * 0.58 + 255.0 * (0.30 + 0.12 * np.clip(1.0 - yy * 2.2, 0, 1))   # upar zyada sheen
    # inner shadow (bottom-right andar) + thickness
    L = np.array([-0.50, -0.62, 0.60], np.float32); L /= np.linalg.norm(L)
    ndl = Nx * L[0] + Ny * L[1] + Nz * L[2]
    flat = np.clip(1.0 - k * 1.2, 0, 1)
    glass = glass * (1.0 - (np.clip(-(Nx * L[0] + Ny * L[1]), 0, 1) * k * 0.20)[..., None])
    # specular: broad soft + sharp rim
    Hh = L + np.array([0, 0, 1.0], np.float32); Hh /= np.linalg.norm(Hh)
    ndh = np.clip(Nx * Hh[0] + Ny * Hh[1] + Nz * Hh[2], 0, 1)
    spec_soft = (ndh ** 18) * k * 0.9
    L2 = np.array([0.55, 0.65, 0.55], np.float32); L2 /= np.linalg.norm(L2)
    H2 = L2 + np.array([0, 0, 1.0], np.float32); H2 /= np.linalg.norm(H2)
    spec2 = (np.clip(Nx * H2[0] + Ny * H2[1] + Nz * H2[2], 0, 1) ** 16) * k * 0.55
    rim = np.exp(-(dist / K(1.3)) ** 2) * inside
    ang = np.clip(-(Nx * L[0] + Ny * L[1]), -1, 1)
    rim_l = rim * (0.28 + 0.72 * np.clip(-ang * -1.0, 0, 1))
    a = (mask * (0.92 if not bright else 0.90) * occ)[..., None]
    out = scr + (glass - scr) * a
    hi = np.clip(spec_soft + spec2 + rim_l * 0.85, 0, 1.0) * mask * occ
    out += (255.0 - out) * hi[..., None] * (0.95 if not bright else 0.90)
    scr[...] = out

# ---------------- UI pieces ----------------
def padlock_mask(n):
    def fn(d, s):
        k = n * s / 24.0
        d.rounded_rectangle((3 * k, 10.5 * k, 21 * k, 22.5 * k), radius=3 * k, fill=255)
        d.arc((6.5 * k, 1.5 * k, 17.5 * k, 16 * k), 180, 360, fill=255, width=int(2.4 * k))
        d.line((7.7 * k, 8.5 * k, 7.7 * k, 11 * k), fill=255, width=int(2.4 * k)); d.line((16.3 * k, 8.5 * k, 16.3 * k, 11 * k), fill=255, width=int(2.4 * k))
    return _ss_mask(n + 1, n + 1, fn, 4)

def handset_mask(n):
    def fn(d, s):
        k = n * s / 48.0
        P0, P1, P2 = (12, 11), (11, 34), (36, 37)
        pts = []
        for i in range(41):
            t = i / 40.0
            pts.append((((1 - t) ** 2 * P0[0] + 2 * (1 - t) * t * P1[0] + t * t * P2[0]) * k, ((1 - t) ** 2 * P0[1] + 2 * (1 - t) * t * P1[1] + t * t * P2[1]) * k))
        d.line(pts, fill=255, width=int(8.4 * k), joint='curve')
        for (x, y) in (P0, P2): d.ellipse(((x - 4.2) * k, (y - 4.2) * k, (x + 4.2) * k, (y + 4.2) * k), fill=255)
        d.rounded_rectangle((9 * k, 6 * k, 21 * k, 15 * k), radius=3.2 * k, fill=255)
        d.rounded_rectangle((31 * k, 28 * k, 40 * k, 40 * k), radius=3.2 * k, fill=255)
    return _ss_mask(n + 1, n + 1, fn, 4)

def _rr(w, h, x0, y0, x1, y1, r, ss=3):
    return _ss_mask(w, h, lambda d, s: d.rounded_rectangle((x0 * s, y0 * s, x1 * s - 1, y1 * s - 1), radius=r * s, fill=255), ss)

PU = 1.123 * SC      # iPhone point -> pixel

def draw_island(scr):
    iw, ih = 125 * PU, 37 * PU; ix, iy = (SWF - iw) / 2.0, 11 * PU
    put(scr, _rr(int(iw) + 2, int(ih) + 2, 0, 0, iw, ih, ih / 2, 4), ix, iy, (0, 0, 0), 1.0)
    lr = 5.6 * PU; lcx, lcy = ix + iw - 17 * PU, iy + ih / 2
    lens = _ss_mask(int(2 * lr) + 4, int(2 * lr) + 4, lambda d, s_: d.ellipse((2 * s_, 2 * s_, (2 + 2 * lr) * s_, (2 + 2 * lr) * s_), fill=255), 4)
    put(scr, lens, lcx - lens.shape[1] / 2, lcy - lens.shape[0] / 2, (14, 18, 38), 1.0)
    inner = _ss_mask(int(2 * lr * 0.5) + 4, int(2 * lr * 0.5) + 4, lambda d, s_: d.ellipse((2 * s_, 2 * s_, (2 + lr) * s_, (2 + lr) * s_), fill=255), 4)
    put(scr, inner, lcx - inner.shape[1] / 2, lcy - inner.shape[0] / 2, (30, 40, 90), 0.8)

def draw_status(scr, ic, level, kbps=None):
    yc = 29.5 * PU
    bw, bh = 27 * PU, 13 * PU; bx, by0 = SWF - 34 * PU - bw, yc - bh / 2.0
    body = _rr(int(bw) + 2, int(bh) + 2, 0, 0, bw, bh, 4.2 * PU, 4)
    put(scr, body, bx, by0, ic, 0.36)
    inset = 2.0 * PU
    fw = (bw - 2 * inset) * max(4, min(100, level)) / 100.0
    fill = _rr(int(bw) + 2, int(bh) + 2, inset, inset, inset + fw, bh - inset, 2.6 * PU, 4)
    put(scr, fill, bx, by0, ic, 0.97)
    cap = _rr(int(2 * PU) + 3, int(5 * PU) + 3, 0, 0, 1.6 * PU, 4.6 * PU, 0.8 * PU, 4); put(scr, cap, bx + bw + 1.0 * PU, yc - 2.3 * PU, ic, 0.45)
    wm = wifi_mask(); wx = bx - 7 * PU - wm.shape[1]; put(scr, wm, wx, yc - wm.shape[0] / 2.0 + 0.6 * PU, ic, 1.0)
    sm = signal_mask(); put(scr, sm, wx - 6 * PU - sm.shape[1], yc + sm.shape[0] / 2.0 - sm.shape[0], ic, 1.0)

def glass_circle(scr, cx, cy, r):
    x0, y0, x1, y1 = int(cx - r - 4), int(cy - r - 4), int(cx + r + 5), int(cy + r + 5)
    reg = scr[y0:y1, x0:x1].copy()
    blur = cv2.GaussianBlur(reg, (0, 0), max(3.0, r * 0.35))
    lum = _lum_rgb(reg)
    glass = blur * 0.70 + np.array([40, 34, 34], np.float32) * 0.30 if lum > 0.3 else blur * 0.78 + np.array([120, 120, 130], np.float32) * 0.22
    w, h = x1 - x0, y1 - y0; lx, ly = cx - x0, cy - y0
    m = _ss_mask(w, h, lambda d, s_: d.ellipse(((lx - r) * s_, (ly - r) * s_, (lx + r) * s_ - 1, (ly + r) * s_ - 1), fill=255), 3)
    scr[y0:y1, x0:x1] += (glass - reg) * m[..., None]
    rim = np.clip(m - cv2.erode(m, np.ones((3, 3), np.uint8)), 0, 1)
    yy = np.linspace(0, 1, h)[:, None]
    scr[y0:y1, x0:x1] += (255.0 - scr[y0:y1, x0:x1]) * (rim * (0.40 * (1 - yy) + 0.12))[..., None]

def _lum_rgb(a):
    return float((0.2126 * a[..., 0] + 0.7152 * a[..., 1] + 0.0722 * a[..., 2]).mean()) / 255.0

def fmt_date(dt):
    return dt.strftime("%A ") + str(dt.day) + dt.strftime(" %B")

def render_screen(w_bgr, dt, level, kbps):
    scr_bgr = cv2.resize(w_bgr, (SWF, SHF), interpolation=cv2.INTER_AREA) if False else cover_resize(w_bgr, SWF, SHF)
    scr = cv2.cvtColor(scr_bgr, cv2.COLOR_BGR2RGB).astype(np.float32)
    m = subject_mask(scr_bgr)
    layout = pick_layout(m, scr_bgr)
    tm = build_time_mask(layout, dt.hour, dt.minute)
    # time ka rang: wallpaper ka rang (halka/gehra) — time wale area se
    ys, xs = np.where(tm > 0.5)
    reg = scr[ys, xs] if len(ys) else scr.reshape(-1, 3)
    mean = reg.mean(axis=0); lum = float((0.2126 * mean[0] + 0.7152 * mean[1] + 0.0722 * mean[2]) / 255.0)
    if lum < 0.72:
        col = mean + (255.0 - mean) * (0.55 if lum > 0.3 else 0.45); alpha = TIME_ALPHA_DARK if lum > 0.2 else 0.55
        gmean = col.mean(); col = gmean + (col - gmean) * 1.25
    else:
        col = mean * 0.40; alpha = TIME_ALPHA_LIGHT
    col = np.clip(col, 0, 255)
    glass_time(scr, tm, m)
    # date + lock
    ic = (20, 20, 22) if _mean_lum(scr_bgr[:int(K(60))]) > STATUS_DARK_LUM else (255, 255, 255)
    dcol = np.array([255.0, 255.0, 255.0]) if lum < 0.72 else np.array([30.0, 30.0, 34.0])
    f100 = ImageFont.truetype(FONT_DATE, 100)
    fd = ImageFont.truetype(FONT_DATE, int(round(100 * K(15.5) / digit_h(f100))))
    ds = fmt_date(dt)
    if layout == "stackL": draw_text(scr, ds, fd, K(60), K(120) + digit_h(fd) / 2.0, dcol, 0.96, align="l")
    elif layout == "stackR": draw_text(scr, ds, fd, K(434), K(120) + digit_h(fd) / 2.0, dcol, 0.96, align="r")
    else: draw_text(scr, ds, fd, SWF / 2.0, K(120) + digit_h(fd) / 2.0, dcol, 0.96)
    pl = padlock_mask(int(round(K(24)))); put(scr, pl, SWF / 2.0 - pl.shape[1] / 2.0, K(79) - pl.shape[0] / 2.0, (255, 255, 255) if ic[0] > 100 else (20, 20, 22), 0.95)
    draw_island(scr); draw_status(scr, ic, level)
    icb = (20, 20, 22) if _mean_lum(scr_bgr[int(SHF - 130 * PU):int(SHF - 10 * PU)]) > STATUS_DARK_LUM else (255, 255, 255)
    by, br = SHF - 86 * PU, 25 * PU
    glass_circle(scr, 71 * PU, by, br); glass_circle(scr, SWF - 71 * PU, by, br)
    fl = flashlight_mask(int(round(21 * PU))); put(scr, fl, 71 * PU - fl.shape[1] / 2.0, by - fl.shape[0] / 2.0, (255, 255, 255), 0.98)
    cm = camera_mask(int(round(23 * PU))); put(scr, cm, SWF - 71 * PU - cm.shape[1] / 2.0, by - cm.shape[0] / 2.0, (255, 255, 255), 0.98)
    hw, hh = 138 * PU, 5 * PU
    put(scr, _rr(int(hw) + 2, int(hh) + 2, 0, 0, hw, hh, hh / 2, 4), (SWF - hw) / 2.0, SHF - 13 * PU - hh / 2, icb, 0.95)
    np.clip(scr, 0, 255, out=scr)
    return scr, layout, m is not None

def cover_resize(img, W, H):
    h, w = img.shape[:2]
    s = max(W / float(w), H / float(h))
    r = cv2.resize(img, (int(math.ceil(w * s)), int(math.ceil(h * s))), interpolation=cv2.INTER_AREA if s < 1 else cv2.INTER_CUBIC)
    y0 = (r.shape[0] - H) // 2; x0 = (r.shape[1] - W) // 2
    return np.ascontiguousarray(r[y0:y0 + H, x0:x0 + W])

def make_bg(w_bgr):
    img = cover_resize(w_bgr, CW, CH)
    small = cv2.resize(img, (CW // 4, CH // 4), interpolation=cv2.INTER_AREA).astype(np.float32)
    small = cv2.GaussianBlur(small, (0, 0), BG_BLUR * SC / 4.0)
    rgb = cv2.cvtColor(cv2.resize(small, (CW, CH), interpolation=cv2.INTER_CUBIC), cv2.COLOR_BGR2RGB)
    gray = rgb.mean(axis=2, keepdims=True); rgb = gray + (rgb - gray) * 1.12
    ys, xs = np.mgrid[0:CH, 0:CW].astype(np.float32)
    r2 = ((xs - CW / 2.0) / (CW / 2.0)) ** 2 + ((ys - CH / 2.0) / (CH / 2.0)) ** 2
    rgb *= (1 - 0.12 * np.clip(r2 / 2.0, 0, 1))[..., None] * 0.97
    return rgb.astype(np.float32)

def render_mockup(w_bgr, dt, level, kbps):
    out = make_bg(w_bgr)
    scr, layout, had = render_screen(w_bgr, dt, level, kbps)
    x0, y0, w, h = int(round(K(PX0))), int(round(K(PY0))), SWF, SHF
    M = _rr(w, h, 0, 0, w, h, K(PR), 3)
    # saaya (right/bottom)
    sh = np.zeros((CH, CW), np.float32); sh[y0:y0 + h, x0:x0 + w] = M
    sh = cv2.GaussianBlur(sh, (0, 0), K(16)); sh = cv2.warpAffine(sh, np.float32([[1, 0, K(14)], [0, 1, K(16)]]), (CW, CH))
    out *= (1 - 0.50 * sh)[..., None]
    reg = out[y0:y0 + h, x0:x0 + w]
    reg += (scr - reg) * M[..., None]
    return cv2.cvtColor(np.clip(out, 0, 255).astype(np.uint8), cv2.COLOR_RGB2BGR), layout, had
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



def process_one(f, idx, t_now, is_hd):
    raw_p = os.path.join(I, f)
    raw = safe_imread(raw_p)
    if raw is None:
        return 'unreadable', idx, t_now, ''
    w_ready = resize_exact(raw)                      # 1080x1920 ready wallpaper (bagair time ke)
    if is_hd: w_ready = apply_hd_enhance(w_ready)
    mt, wt = t_now + t_step * TIME_STEP_MIN, t_now + 2 * t_step * TIME_STEP_MIN
    disp = datetime.fromtimestamp(mt)                # mockup ka time = pichle se 1 minute aage
    level = random.randint(BATTERY_RANGE[0], BATTERY_RANGE[1])
    fin, layout, had = render_mockup(w_ready, disp, level, random.choice([37, 94, 128, 385, 802, 1.2]) if False else random.randint(12, 899))
    mock_path = os.path.join(O, f"{idx}.jpg")
    wall_path = os.path.join(R, f"{idx + 1}.jpg")
    save_optimized(fin, mock_path); os.utime(mock_path, (mt, mt))
    save_optimized(w_ready, wall_path); os.utime(wall_path, (wt, wt))
    def verify_saved(path):
        try:
            if not os.path.exists(path) or os.path.getsize(path) < 100: return False
            with Image.open(path) as im: im.verify()
            return True
        except Exception:
            return False
    if not (verify_saved(mock_path) and verify_saved(wall_path)):
        raise IOError("save verify failed")
    safe_scan(mock_path); safe_scan(wall_path)
    gallery_copy_outputs([(mock_path, mt), (wall_path, wt)])
    try: os.remove(raw_p)
    except Exception: pass
    return 'ok', idx + 2, wt, f"{layout}{'+depth' if had else ''}"

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
    for _ff in (FONT_TIME, FONT_DATE, FONT_STATUS):
        if not os.path.exists(_ff):
            print(f" {C2}✖ Font file missing: fonts/{os.path.basename(_ff)}{ENDC}"); sys.exit()
    ensure_seg()
    print(f" {C4}✔ Depth engine ready! Subject mask: {_SEG['name']}{ENDC}")

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
    desc_text = "✨ HD Mode " if is_hd else "⚙️ Normal "
    processed_count, skip_log, lay_log = 0, [], []
    set_wakelock(True)
    for f in tqdm(imgs, desc=desc_text, bar_format=f" {C1}{{desc}}{ENDC} {C5}|{ENDC}{{bar:20}}{C5}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"):
        success, last_err = False, None
        for attempt in range(3):
            try:
                status, new_idx, new_t, info = process_one(f, s_idx, cur_t, is_hd)
                if status == 'ok':
                    s_idx, cur_t = new_idx, new_t; processed_count += 1; success = True; lay_log.append(info); break
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
            with open(T_F, "w") as fh: fh.write(str(cur_t))
        except: pass
        nd = sum(1 for x in lay_log if "depth" in x)
        print(f"\n {C4}✔ {processed_count} Mockup + Wallpaper ready! (depth time: {nd}/{processed_count}){ENDC}")
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
