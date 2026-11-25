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
        if enable: os.system("termux-wake-lock >/dev/null 2>&1")
        else: os.system("termux-wake-unlock >/dev/null 2>&1")
    except: pass

# --- SCAN (Project 1 jesa) ---
# Har file save hote hi foran, ONE BY ONE aur usi order me scan hoti hai (pehle mockup, phir wallpapers 1..6).
# Isse gallery me sequence bilkul sahi rehti hai.
def safe_scan(path, timeout_sec=5):
    try:
        subprocess.run(["termux-media-scan", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)
    except subprocess.TimeoutExpired:
        pass
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

# ====================== PROJECT 9 : REALISTIC iPHONE MOCKUP ======================
P_NUM = sys.argv[2] if len(sys.argv) > 2 else "9"
B = os.environ.get("MOCKUP_BASE", os.path.dirname(os.path.abspath(__file__)))
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")
BG_DIR = os.path.join(B, "backgrounds")
FONT_DIR = os.path.join(B, "fonts")
BG_RECENT = os.path.join(B, ".bg_recent.txt")
CASE_RECENT = os.path.join(B, ".case_recent.txt")
T_F = "/storage/emulated/0/.mockup_time_tracker.txt"

# --- FONTS (sab fonts zip ke "fonts" folder me hain; apna font chahiye to bas file badal do) ---
FONT_TIME   = os.path.join(FONT_DIR, "TimeSerif-Bold.ttf")    # bada serif time (lock screen)
FONT_DATE   = os.path.join(FONT_DIR, "Inter-Medium.otf")      # date (SF Pro jesa)
FONT_STATUS = os.path.join(FONT_DIR, "Inter-SemiBold.otf")    # status bar ka chota time
FONT_NUM    = os.path.join(FONT_DIR, "Inter-Bold.otf")        # battery ke andar % number

# --- CANVAS / PHONE ---
CANVAS_W, CANVAS_H = 1080, 1920
PHONE_W_F  = 0.66        # phone ki chaudai canvas ka hissa
TILT_MODE  = "natural"   # "natural" = halka sa haath me pakde jesa tilt (har mockup me thora alag) | "flat" = seedha
TILT_DEG   = 4.0         # natural mode: zyada se zyada kitne degree ghoomna
TILT_PERSP = 0.045       # natural mode: perspective (upar se thora patla)
CASE_MODE  = "smart"     # "smart" = wallpaper + background ke hisab se case ka rang | ya naam: pink, black, lavender, sky, sage, ivory, midnight, natural, red

# --- LOCK SCREEN ---
TIME_MODE  = "real"      # "real" = asli (abhi ka) waqt | "fixed" = hamesha FIXED_TIME
FIXED_TIME = "9:41"
STATUS_TIME_LEFT = True  # upar left status bar me bhi chota time (real iPhone jesa) | False = khali
CARRIER_TEXT     = ""    # upar left me time ke bajaye carrier name (jese "Jazz") chahiye to yahan likho
BATTERY_MODE  = "random" # "random" = har mockup me alag | "fixed"
BATTERY_RANGE = (55, 100)
BATTERY_FIXED = 80
TEXT_STYLE   = "tinted"  # "tinted" = time/date wallpaper ke rang jesa (iPhone widget) | "white" = safed/kala
BRIGHT_SPLIT = 0.78
LUM_SPLIT    = 0.55
TINT_MIN_CONTRAST = 4.0
STATUS_DARK_LUM   = 0.70 # status bar / home bar: wallpaper is se zyada roshan ho to kale icons
GLOSS = True             # screen par halka shisha-reflection
GRAIN = 1.0              # poori image par halka grain (CG look kam)

# ---------------------------------------------------------------------------------
# GEOMETRY  (iPhone 15 Pro: 393 x 852 pt)
SW = 700
PT = SW / 393.0
SH = int(round(852 * PT))
BEZEL = 6 * PT
RIM = 12 * PT
PAD = 12 * PT
OFF = BEZEL + RIM + PAD
LW = int(round(SW + 2 * OFF))
LH = int(round(SH + 2 * OFF))

CASES = {
    "pink": (243, 178, 198), "lavender": (186, 168, 226), "sky": (146, 194, 232),
    "sage": (164, 194, 164), "ivory": (238, 230, 214), "black": (30, 30, 34),
    "midnight": (38, 52, 88), "natural": (186, 176, 162), "red": (196, 52, 56),
}

# ---------------- small drawing helpers ----------------
def _ss_mask(w, h, fn, ss=2):
    im = Image.new('L', (max(1, int(w)) * ss, max(1, int(h)) * ss), 0)
    fn(ImageDraw.Draw(im), ss)
    return np.asarray(im.resize((max(1, int(w)), max(1, int(h))), Image.BOX), np.float32) / 255.0

def rr_mask(w, h, rect, radius, ss=2):
    x0, y0, x1, y1 = rect
    return _ss_mask(w, h, lambda d, s: d.rounded_rectangle((x0 * s, y0 * s, x1 * s - 1, y1 * s - 1), radius=radius * s, fill=255), ss)

def put(canvas, mask, x, y, color, alpha=1.0):
    h, w = mask.shape[:2]; H, W = canvas.shape[:2]
    x, y = int(round(x)), int(round(y))
    x0, y0, x1, y1 = max(0, x), max(0, y), min(W, x + w), min(H, y + h)
    if x1 <= x0 or y1 <= y0: return
    m = mask[y0 - y:y1 - y, x0 - x:x1 - x][..., None] * alpha
    reg = canvas[y0:y1, x0:x1]
    reg += (np.array(color, np.float32) - reg) * m

def text_tile(text, font, stroke=0):
    bb = font.getbbox(text, anchor='ls', stroke_width=stroke)
    pad = 8
    w, h = bb[2] - bb[0] + 2 * pad, bb[3] - bb[1] + 2 * pad
    im = Image.new('L', (w, h), 0)
    ox, oy = pad - bb[0], pad - bb[1]
    ImageDraw.Draw(im).text((ox, oy), text, font=font, fill=255, anchor='ls', stroke_width=stroke, stroke_fill=255)
    return np.asarray(im, np.float32) / 255.0, ox, oy, bb

def draw_text(scr, text, font, cx, base_y, color, alpha=1.0, stroke=0, shadow=0.0):
    """cx = ink ka horizontal centre, base_y = baseline."""
    m, ox, oy, bb = text_tile(text, font, stroke)
    x = cx - (bb[0] + bb[2]) / 2.0 - ox
    y = base_y - oy
    if shadow > 0:
        sm = cv2.GaussianBlur(m, (0, 0), 3.0 * PT / 1.8)
        put(scr, sm, x, y + 1.6 * PT, (0, 0, 0), shadow)
    put(scr, m, x, y, color, alpha)

def digit_h(font):
    return -font.getbbox("0", anchor='ls')[1]

def fit_font(path, text, target_w):
    f = ImageFont.truetype(path, 200)
    bb = f.getbbox(text, anchor='ls')
    return ImageFont.truetype(path, max(8, int(round(200 * target_w / float(bb[2] - bb[0])))))

# ---------------- icons ----------------
def signal_mask():
    bw, gap, H = 3.0 * PT, 1.7 * PT, 11.2 * PT
    w, h = int(math.ceil(4 * bw + 3 * gap)) + 1, int(math.ceil(H)) + 1
    ratios = [0.36, 0.56, 0.78, 1.0]
    def fn(d, s):
        for i, r in enumerate(ratios):
            x0 = i * (bw + gap); bh = H * r
            d.rounded_rectangle((x0 * s, (H - bh) * s, (x0 + bw) * s - 1, H * s - 1), radius=0.9 * PT * s, fill=255)
    return _ss_mask(w, h, fn, 4)

def wifi_mask():
    Ro = 11.6 * PT
    w, h = int(math.ceil(2 * Ro * math.sin(math.radians(46)))) + 2, int(math.ceil(Ro + 0.8 * PT))
    cx, cy = w / 2.0, Ro + 0.3 * PT
    bands = [(11.6, 8.9), (7.5, 5.0), (3.5, 0)]
    def fn(d, s):
        for ro, ri in bands:
            ro, ri = ro * PT, ri * PT
            d.pieslice(((cx - ro) * s, (cy - ro) * s, (cx + ro) * s, (cy + ro) * s), 224, 316, fill=255)
            if ri > 0:
                d.pieslice(((cx - ri) * s, (cy - ri) * s, (cx + ri) * s, (cy + ri) * s), 224, 316, fill=0)
    return _ss_mask(w, h, fn, 4)

def battery_masks(level):
    bw, bh, cw, ch, gp = 25.5 * PT, 12.6 * PT, 1.7 * PT, 4.6 * PT, 0.9 * PT
    w, h = int(math.ceil(bw + gp + cw)) + 1, int(math.ceil(bh)) + 1
    body = _ss_mask(w, h, lambda d, s: d.rounded_rectangle((0, 0, bw * s - 1, bh * s - 1), radius=3.8 * PT * s, fill=255), 4)
    cap = _ss_mask(w, h, lambda d, s: d.rounded_rectangle(((bw + gp) * s, (bh - ch) / 2 * s, (bw + gp + cw) * s - 1, (bh + ch) / 2 * s - 1), radius=0.8 * PT * s, fill=255), 4)
    fill = body.copy()
    xcut = bw * max(0, min(100, level)) / 100.0
    cols = np.clip((xcut - np.arange(w)), 0, 1).astype(np.float32)
    fill *= cols[None, :]
    return body, cap, fill, w, h, bw, bh

def flashlight_mask(size):
    k0 = size / 24.0
    def fn(d, s):
        k = k0 * s
        d.polygon([(6.4 * k, 2.0 * k), (17.6 * k, 2.0 * k), (15.4 * k, 8.2 * k), (8.6 * k, 8.2 * k)], fill=255)
        d.rounded_rectangle((5.6 * k, 0.9 * k, 18.4 * k, 3.6 * k), radius=1.1 * k, fill=255)
        d.rounded_rectangle((8.4 * k, 7.8 * k, 15.6 * k, 22.8 * k), radius=2.6 * k, fill=255)
        d.rounded_rectangle((10.9 * k, 11.8 * k, 13.1 * k, 15.8 * k), radius=1.1 * k, fill=0)
    return _ss_mask(size + 1, size + 1, fn, 4)

def camera_mask(size):
    k0 = size / 24.0
    def fn(d, s):
        k = k0 * s
        d.rounded_rectangle((1.6 * k, 6.4 * k, 22.4 * k, 20.8 * k), radius=3.6 * k, fill=255)
        d.rounded_rectangle((8.0 * k, 3.6 * k, 16.0 * k, 7.6 * k), radius=1.5 * k, fill=255)
        cy = 13.6
        d.ellipse(((12 - 5.0) * k, (cy - 5.0) * k, (12 + 5.0) * k, (cy + 5.0) * k), fill=0)
        d.ellipse(((12 - 3.4) * k, (cy - 3.4) * k, (12 + 3.4) * k, (cy + 3.4) * k), fill=255)
        d.ellipse(((19.2 - 0.9) * k, (9.0 - 0.9) * k, (19.2 + 0.9) * k, (9.0 + 0.9) * k), fill=0)
    return _ss_mask(size + 1, size + 1, fn, 4)

def glass_button(scr, cx, cy, r):
    x0, y0, x1, y1 = int(cx - r - 4), int(cy - r - 4), int(cx + r + 5), int(cy + r + 5)
    reg = scr[y0:y1, x0:x1].copy()
    blur = cv2.GaussianBlur(reg, (0, 0), max(2.0, r * 0.32))
    glass = blur * 0.60 + np.array([24, 24, 28], np.float32) * 0.40 + 5
    w, h = x1 - x0, y1 - y0
    lx, ly = cx - x0, cy - y0
    m = _ss_mask(w, h, lambda d, s: d.ellipse(((lx - r) * s, (ly - r) * s, (lx + r) * s - 1, (ly + r) * s - 1), fill=255), 3)
    m_in = _ss_mask(w, h, lambda d, s: d.ellipse(((lx - r + 1.3) * s, (ly - r + 1.3) * s, (lx + r - 1.3) * s - 1, (ly + r - 1.3) * s - 1), fill=255), 3)
    scr[y0:y1, x0:x1] += (glass - reg) * m[..., None]
    put(scr, np.clip(m - m_in, 0, 1), x0, y0, (255, 255, 255), 0.10)

def _mean_lum(bgr):
    f = bgr.astype(np.float32) / 255.0
    return float((0.2126 * f[..., 2] + 0.7152 * f[..., 1] + 0.0722 * f[..., 0]).mean())

# ---------------- lock screen UI ----------------
def draw_lock_ui(scr, scr_bgr, now_dt, level):
    H, W = scr.shape[:2]
    p = lambda v: v * PT
    if TIME_MODE == "fixed": time_str = FIXED_TIME
    else: time_str = f"{now_dt.hour % 12 or 12}:{now_dt.strftime('%M')}"
    date_str = now_dt.strftime("%a ") + str(now_dt.day) + now_dt.strftime(" %b")

    zx0, zx1, zy0, zy1 = int(0.18 * W), int(0.82 * W), int(0.07 * H), int(0.235 * H)
    tint = zone_text_rgb(scr_bgr, zx0, zy0, zx1 - zx0, zy1 - zy0) if TEXT_STYLE == "tinted" else zone_plain_rgb(scr_bgr, zx0, zy0, zx1 - zx0, zy1 - zy0)

    top_dark = _mean_lum(scr_bgr[:int(0.06 * H)]) > STATUS_DARK_LUM
    bot_dark = _mean_lum(scr_bgr[int(0.97 * H):]) > STATUS_DARK_LUM
    ic = (20, 20, 22) if top_dark else (255, 255, 255)
    inv = (255, 255, 255) if top_dark else (18, 18, 20)
    ic_b = (20, 20, 22) if bot_dark else (255, 255, 255)

    # bottom glass buttons first (they blur the wallpaper)
    by, br = 0.905 * H, p(25)
    glass_button(scr, p(70), by, br); glass_button(scr, W - p(70), by, br)
    fl = flashlight_mask(int(round(p(25)))); put(scr, fl, p(70) - fl.shape[1] / 2, by - fl.shape[0] / 2, (255, 255, 255), 0.97)
    cm = camera_mask(int(round(p(25)))); put(scr, cm, W - p(70) - cm.shape[1] / 2, by - cm.shape[0] / 2, (255, 255, 255), 0.97)

    # date + big serif time
    f_date = fit_font(FONT_DATE, "Tue 23 Jun", 0.30 * W)
    f_time = fit_font(FONT_TIME, "7:01", 0.44 * W)
    stroke = max(1, int(round(f_time.size * 0.012)))
    draw_text(scr, date_str, f_date, W / 2.0, 0.100 * H + digit_h(f_date) / 2.0, tint, 0.93, shadow=0.16)
    draw_text(scr, time_str, f_time, W / 2.0, 0.178 * H + digit_h(f_time) / 2.0, tint, 1.0, stroke=stroke, shadow=0.20)

    # dynamic island
    iw, ih = p(125), p(37)
    ix, iy = (W - iw) / 2.0, p(11)
    isl = _ss_mask(int(iw) + 2, int(ih) + 2, lambda d, s: d.rounded_rectangle((0, 0, iw * s - 1, ih * s - 1), radius=ih / 2 * s, fill=255), 4)
    put(scr, isl, ix, iy, (0, 0, 0), 1.0)
    lr = p(5.6); lcx, lcy = ix + iw - p(21), iy + ih / 2
    lens = _ss_mask(int(2 * lr) + 4, int(2 * lr) + 4, lambda d, s: d.ellipse((2 * s, 2 * s, (2 + 2 * lr) * s, (2 + 2 * lr) * s), fill=255), 4)
    put(scr, lens, lcx - lens.shape[1] / 2, lcy - lens.shape[0] / 2, (13, 15, 30), 1.0)
    hl = _ss_mask(int(lr) + 2, int(lr) + 2, lambda d, s: d.ellipse((0, 0, lr * 0.55 * s, lr * 0.55 * s), fill=255), 4)
    put(scr, hl, lcx - lr * 0.55, lcy - lr * 0.62, (70, 84, 130), 0.55)

    # status bar (right)
    yc = p(29.6)
    bm, cap, fill, bw_i, bh_i, bw, bh = battery_masks(level)
    bx, by0 = W - p(21) - bw_i, yc - bh_i / 2.0
    put(scr, bm, bx, by0, ic, 0.34); put(scr, cap, bx, by0, ic, 0.40); put(scr, fill, bx, by0, ic, 1.0)
    f_num = ImageFont.truetype(FONT_NUM, max(8, int(round(p(10.4)))))
    draw_text(scr, str(int(level)), f_num, bx + bw / 2.0, by0 + bh / 2.0 + digit_h(f_num) / 2.0, inv, 0.92)
    wm = wifi_mask(); wx = bx - p(6.6) - wm.shape[1]; put(scr, wm, wx, yc - wm.shape[0] / 2.0 + p(0.4), ic, 1.0)
    sm = signal_mask(); sx = wx - p(6.4) - sm.shape[1]; put(scr, sm, sx, yc - sm.shape[0] / 2.0, ic, 1.0)
    # status bar (left)
    f_st = ImageFont.truetype(FONT_STATUS, max(8, int(round(p(16.4)))))
    if STATUS_TIME_LEFT:
        draw_text(scr, time_str, f_st, p(62), yc + digit_h(f_st) / 2.0, ic, 1.0)
    elif CARRIER_TEXT:
        draw_text(scr, CARRIER_TEXT, f_st, p(62), yc + digit_h(f_st) / 2.0, ic, 0.95)

    # home indicator
    hw, hh = p(134), p(5)
    hm = _ss_mask(int(hw) + 2, int(hh) + 2, lambda d, s: d.rounded_rectangle((0, 0, hw * s - 1, hh * s - 1), radius=hh / 2 * s, fill=255), 4)
    put(scr, hm, (W - hw) / 2.0, H - p(8) - hh, ic_b, 0.96)
    np.clip(scr, 0, 255, out=scr)

# ---------------- phone body ----------------
def build_phone(scr_rgb, case_rgb):
    ys, xs = np.mgrid[0:LH, 0:LW].astype(np.float32)
    u = (xs / LW + ys / LH) / 2.0
    case = np.array(case_rgb, np.float32)
    R_s = 55 * PT; R_bez = R_s + BEZEL; R_out = R_bez + RIM
    body = rr_mask(LW, LH, (PAD, PAD, LW - PAD, LH - PAD), R_out)
    bez = rr_mask(LW, LH, (PAD + RIM, PAD + RIM, LW - PAD - RIM, LH - PAD - RIM), R_bez)
    scr_m = rr_mask(LW, LH, (OFF, OFF, OFF + SW, OFF + SH), R_s)

    # side buttons (case ke saath ek hi piece)
    btn = np.zeros((LH, LW), np.float32)
    def sidebtn(x0, y0pt, y1pt, x1):
        btn[:] = np.maximum(btn, rr_mask(LW, LH, (x0, OFF + y0pt * PT, x1, OFF + y1pt * PT), 1.5 * PT))
    for (a, b) in [(112, 142), (170, 218), (232, 280)]: sidebtn(PAD - 2.6 * PT, a, b, PAD + 6 * PT)
    sidebtn(LW - PAD - 6 * PT, 178, 250, LW - PAD + 2.6 * PT)
    cm = np.maximum(body, btn)

    d_in = cv2.distanceTransform((cm > 0.5).astype(np.uint8), cv2.DIST_L2, 5)
    shade = 1.12 - 0.26 * u
    col = case[None, None, :] * shade[..., None]
    hi = np.clip(1.25 - 1.6 * u, 0.10, 1.0)
    edge = np.exp(-(d_in / (1.5 * PT)) ** 2)
    col = col + (255 - col) * (edge * hi * 0.55)[..., None]
    t = np.clip(d_in / RIM, 0, 1)
    col = col * (1 + 0.07 * np.sin(np.pi * t))[..., None]
    d_out = cv2.distanceTransform((bez < 0.5).astype(np.uint8), cv2.DIST_L2, 5)
    lip_dark = np.exp(-((d_out - 1.3 * PT) / (1.0 * PT)) ** 2)
    lip_hi = np.exp(-((d_out - 3.4 * PT) / (1.5 * PT)) ** 2)
    col = col * (1 - 0.40 * lip_dark)[..., None]
    col = col + (255 - col) * (0.16 * lip_hi * hi)[..., None]

    L = col * cm[..., None]
    A = np.maximum(cm, bez)
    L = L * (1 - bez[..., None]) + np.array([8, 8, 10], np.float32) * bez[..., None]
    full = np.zeros((LH, LW, 3), np.float32); full[int(round(OFF)):int(round(OFF)) + SH, int(round(OFF)):int(round(OFF)) + SW] = scr_rgb
    L = L * (1 - scr_m[..., None]) + full * scr_m[..., None]
    if GLOSS:
        s = (xs - OFF) / SW * 0.62 + (ys - OFF) / SH
        g = 0.085 * np.clip(1 - s / 0.75, 0, 1) ** 1.6 + 0.032 * np.exp(-((s - 1.0) / 0.045) ** 2)
        L = L + (255 - L) * (g * scr_m)[..., None]
    return np.dstack([L, A * 255.0]).astype(np.float32)

def warp_layer(layer, cx, cy, scale, ang, persp):
    lh, lw = layer.shape[:2]
    src = np.float32([[0, 0], [lw, 0], [lw, lh], [0, lh]])
    th = math.radians(ang); c, s = math.cos(th), math.sin(th)
    dst = []
    for (x, y) in src:
        px, py = x - lw / 2.0, y - lh / 2.0
        px2 = px * (1 + persp * (py / lh) * 2)
        dst.append([cx + (px2 * c - py * s) * scale, cy + (px2 * s + py * c) * scale])
    M = cv2.getPerspectiveTransform(src, np.float32(dst))
    out = cv2.warpPerspective(layer, M, (CANVAS_W, CANVAS_H), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0))
    return np.clip(out, 0, 255)

def _shadow(a, sigma, dx, dy, op):
    b = cv2.GaussianBlur(a, (0, 0), sigma)
    b = cv2.warpAffine(b, np.float32([[1, 0, dx], [0, 1, dy]]), (a.shape[1], a.shape[0]))
    return b * op

def render_mockup(w_ready_bgr, bg_bgr, case_rgb, now_dt, level):
    scr_bgr = resize_exact(w_ready_bgr, SW, SH)
    scr = cv2.cvtColor(scr_bgr, cv2.COLOR_BGR2RGB).astype(np.float32)
    draw_lock_ui(scr, scr_bgr, now_dt, level)
    layer = build_phone(scr, case_rgb)
    if TILT_MODE == "natural":
        ang = random.uniform(-TILT_DEG, TILT_DEG); persp = random.uniform(0.0, TILT_PERSP)
        ox, oy = random.uniform(-14, 14), random.uniform(-14, 14)
    else:
        ang, persp, ox, oy = 0.0, 0.0, 0.0, 0.0
    scale = (PHONE_W_F * CANVAS_W) / (LW - 2 * PAD)
    warped = warp_layer(layer, CANVAS_W / 2.0 + ox, CANVAS_H / 2.0 + oy, scale, ang, persp)
    a = warped[..., 3] / 255.0
    bg = cv2.cvtColor(resize_exact(bg_bgr, CANVAS_W, CANVAS_H), cv2.COLOR_BGR2RGB).astype(np.float32)
    s1, s2, s3 = _shadow(a, 36, 26, 46, 0.36), _shadow(a, 14, 9, 17, 0.30), _shadow(a, 4, 2, 4, 0.30)
    sh = 1 - (1 - s1) * (1 - s2) * (1 - s3)
    out = bg * (1 - sh[..., None] * 0.80)
    out = out * (1 - a[..., None]) + warped[..., :3]
    if GRAIN > 0:
        out += np.random.normal(0, GRAIN, out.shape[:2]).astype(np.float32)[..., None]
    return cv2.cvtColor(np.clip(out, 0, 255).astype(np.uint8), cv2.COLOR_RGB2BGR)

# ---------------- smart choices ----------------
def bg_features(designs):
    out = {}
    for name in designs:
        img = cv2.imread(os.path.join(BG_DIR, name))
        if img is None: continue
        sm = cv2.resize(img, (108, 192), interpolation=cv2.INTER_AREA)
        m = np.ones((192, 108), bool); m[12:180, 12:96] = False
        out[name] = color_features(sm[m].reshape(-1, 1, 3))
    return out

def _read_list(path):
    try:
        with open(path, "r") as fh: return [l.strip() for l in fh if l.strip()]
    except Exception:
        return []

def _write_list(path, lst):
    try:
        with open(path, "w") as fh: fh.write("\n".join(lst[-4:]))
    except Exception:
        pass

def pick_case(wf, bf, recent):
    if CASE_MODE != "smart" and CASE_MODE in CASES: return CASE_MODE
    best, best_s = "pink", -1e9
    for name, rgb in CASES.items():
        r, g, b = [c / 255.0 for c in rgb]
        hue, light, sat = colorsys.rgb_to_hls(r, g, b)
        lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
        sc = 0.5 * min(abs(lum - bf["lum"]) / 0.30, 1.0)
        if wf["hue"] is not None and wf["sat"] > 0.15 and sat > 0.2:
            dh = abs(wf["hue"] - hue); dh = min(dh, 1 - dh)
            sc += 0.5 * (1 - min(dh / 0.22, 1.0))
        else:
            sc += 0.22
        if name in recent: sc -= 0.35
        sc += random.uniform(0, 0.10)
        if sc > best_s: best, best_s = name, sc
    return best

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



def list_backgrounds():
    if not os.path.isdir(BG_DIR): return []
    return sorted([f for f in os.listdir(BG_DIR) if f.lower().endswith(IMG_EXTS) and not f.startswith('.')])

def process_one(f, idx, t_now, is_hd, bg_name, case_name):
    raw_p = os.path.join(I, f)
    raw = safe_imread(raw_p)
    if raw is None:
        return 'unreadable', idx, t_now
    w_ready = resize_exact(raw) if crop_916 else raw  # Size: Crop 9:16 ya Original (baghair text ke)
    if is_hd: w_ready = apply_hd_enhance(w_ready)
    bg = cv2.imread(os.path.join(BG_DIR, bg_name))
    if bg is None: raise IOError("background load nahi hua: " + bg_name)
    level = BATTERY_FIXED if BATTERY_MODE == "fixed" else random.randint(BATTERY_RANGE[0], BATTERY_RANGE[1])
    fin = render_mockup(w_ready, bg, CASES.get(case_name, CASES["pink"]), datetime.now(), level)

    mock_path = os.path.join(O, f"{idx}.jpg")
    wall_path = os.path.join(R, f"{idx + 1}.jpg")
    mt, wt = t_now + 60, t_now + 120
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

    # Project 1 jesa: har file foran, ek ek karke, usi order me scan (pehle mockup, phir wallpaper)
    safe_scan(mock_path)
    safe_scan(wall_path)
    gallery_copy_outputs([(mock_path, mt), (wall_path, wt)])
    try: os.remove(raw_p)
    except Exception: pass
    del raw, w_ready, fin
    return 'ok', idx + 2, wt

if len(sys.argv) < 2:
    action = 'process'
else:
    action = sys.argv[1]

if action == 'process':
    display_header()
    os.makedirs(O, exist_ok=True); os.makedirs(R, exist_ok=True)

    if not os.path.isdir(I):
        print(f" {C2}✖ Input folder nahi mila: {P_NUM}_wallpapers{ENDC}")
        sys.exit()
    imgs = list_inputs()
    imgs.sort(key=lambda f: os.path.getctime(os.path.join(I, f)))   # folder me aane ka order
    if len(imgs) == 0:
        print(f" {C2}✖ {P_NUM}_wallpapers me images nahi mili.{ENDC}")
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

    for _ff in (FONT_TIME, FONT_DATE, FONT_STATUS, FONT_NUM):
        if not os.path.exists(_ff):
            print(f" {C2}✖ Font file missing: fonts/{os.path.basename(_ff)}{ENDC}")
            sys.exit()
    designs = list_backgrounds()
    if not designs:
        print(f" {C2}✖ backgrounds folder khali hai — pehle background file(s) rakho.{ENDC}")
        sys.exit()
    print(f" {C4}✔ iPhone mockup engine ready! ({len(designs)} background design(s)){ENDC}")

    # --- SMART: wallpaper ka rang/roshni nikalo (background + case ka rang isi se chunte hain) ---
    feats, unreadable = {}, []
    for f in tqdm(imgs, desc=" \U0001f50e Analyzing ", bar_format=f" {C1}{{desc}}{ENDC} {C5}|{ENDC}{{bar:20}}{C5}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"):
        t = thumb_bgr(os.path.join(I, f))
        if t is None: unreadable.append(f); continue
        feats[f] = color_features(t)
    imgs = [f for f in imgs if f in feats]
    bgf = bg_features(designs)
    recent_bg, recent_case = read_recent(), _read_list(CASE_RECENT)
    bg_rr = 0

    s_idx = get_max_seq([O, R, W_H]) + 1
    cur_t = time.time()
    if os.path.exists(T_F):
        try:
            with open(T_F, "r") as fh:
                lt = float(fh.read().strip())
                if lt > cur_t: cur_t = lt
        except: pass

    print("")
    pbar_format = f" {C1}{{desc}}{ENDC} {C5}|{ENDC}{{bar:20}}{C5}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"
    desc_text = "✨ HD Mode " if is_hd else "⚙️ Normal "

    processed_count, corrupt_count, skip_log = 0, 0, []
    set_wakelock(True)
    for f in tqdm(imgs, desc=desc_text, bar_format=pbar_format):
        wf = feats[f]
        if bgf:
            bg_name = pick_background(designs, bgf, [wf], recent_bg) or designs[bg_rr % len(designs)]
        else:
            bg_name = designs[bg_rr % len(designs)]
        bg_rr += 1
        case_name = pick_case(wf, bgf.get(bg_name, wf), recent_case)
        success, last_err = False, None
        for attempt in range(3):
            try:
                status, new_idx, new_t = process_one(f, s_idx, cur_t, is_hd, bg_name, case_name)
                if status == 'ok':
                    s_idx, cur_t = new_idx, new_t
                    processed_count += 1; success = True
                    recent_bg.append(bg_name); recent_bg = recent_bg[-3:]; write_recent(recent_bg)
                    recent_case.append(case_name); recent_case = recent_case[-3:]; _write_list(CASE_RECENT, recent_case)
                    break
                else:
                    last_err = "padhi nahi gayi: " + READ_ERR.get(os.path.join(I, f), "?")
                    break
            except Exception as e:
                last_err = f"{type(e).__name__}: {str(e)[:50]}"
                for p in (os.path.join(O, f"{s_idx}.jpg"), os.path.join(R, f"{s_idx + 1}.jpg")):
                    if os.path.exists(p):
                        try: os.remove(p)
                        except: pass
                gc.collect()
                time.sleep(0.3)
        if not success:
            corrupt_count += 1
            skip_log.append(f"{f} ({last_err})")
        gc.collect()

    set_wakelock(False)

    if processed_count > 0:
        try:
            with open(T_F, "w") as fh: fh.write(str(cur_t))
        except: pass
        print(f"\n {C4}✔ {processed_count} Mockup + Wallpaper ready (real iPhone look)!{ENDC}")
    else:
        print(f"\n {C5}✔ Sab already set hai.{ENDC}")

    for u in unreadable:
        skip_log.append(f"{u} ({READ_ERR.get(os.path.join(I, u), '?')})")
    if skip_log:
        print(f" {C2}⚠ {len(skip_log)} file(s) skip ki gayin (dobara retry hongi agle run me):{ENDC}")
        for s in skip_log[:8]:
            print(f"   {C2}- {s}{ENDC}")
        if len(skip_log) > 8:
            print(f"   {C2}...aur {len(skip_log) - 8} more{ENDC}")
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
            dst = shutil.move(os.path.join(O, f), cb)
            safe_scan(dst)
        for f in r_files:
            dst = shutil.move(os.path.join(R, f), cb)
            safe_scan(dst)
        set_wakelock(False)

        animated_spinner("Packing Batch", 0.6)
        print(f" {C4}\U0001f4e6 {os.path.basename(cb)} Warehouse mein chala gaya!{ENDC}\n")
    else:
        print(f"\n {C2}\u2716 Batch banane ke liye data nahi hai.{ENDC}\n")

elif action == 'del':
    display_header()

    img_exts = IMG_EXTS
    del_pbar_format = "{l_bar}%s{bar}%s{r_bar}" % (HR, ENDC)

    def count_imgs(folder):
        if not os.path.exists(folder): return 0
        return len([f for f in os.listdir(folder) if not f.startswith('.') and f.lower().endswith(img_exts)])

    print(f"{HG} \U0001f50d Scanning files...{ENDC}")
    i_count = count_imgs(I)
    o_count = count_imgs(O)
    r_count = count_imgs(R)
    wh_count = len([d for d in os.listdir(W_H) if "Batch_" in d]) if os.path.exists(W_H) else 0
    time.sleep(0.3)

    print(f"\n {HG2}\u2554{HD * 54}\u2557{ENDC}")
    print(f" {HG2}\u2551{ENDC}  {HR}{BOLD}\U0001f5d1\ufe0f   D E L E T E   M E N U{ENDC}{' ' * 23}{HG2}\u2551{ENDC}")
    print(f" {HG2}\u255a{HD * 54}\u255d{ENDC}")
    print(f" {HC}[1]{ENDC} \U0001f5bc\ufe0f  Tayaar Maal (Mock+Wall) {HY}\u2192 {o_count + r_count} file(s){ENDC} {HG}[DEFAULT]{ENDC}")
    print(f" {HC}[2]{ENDC} \U0001f4e5 Input Images          {HY}\u2192 {i_count} file(s){ENDC}")
    print(f" {HC}[3]{ENDC} \U0001f4e6 Warehouse (Batches)    {HY}\u2192 {wh_count} batch(es){ENDC}")
    print(f" {HC}[0]{ENDC} Cancel")
    print(f" {HG2}\u26a0 Ye action wapas nahi ho sakta.{ENDC}")
    choice = input(f"\n {HG}\u279c {ENDC}").strip()
    if choice == '': choice = '1'

    def perm_del_images(folder, label):
        files = []
        if os.path.exists(folder):
            files = [f for f in os.listdir(folder) if not f.startswith('.') and f.lower().endswith(img_exts)]
        if not files:
            print(f" {HG2}\u2716 {label}: koi file nahi mili.{ENDC}")
            return 0
        c = 0
        for f in tqdm(files, desc=f" \U0001f5d1\ufe0f  {label}", bar_format=del_pbar_format):
            try:
                os.remove(os.path.join(folder, f))
                c += 1
            except: pass
        safe_scan_dir(folder)
        return c

    def delete_folder_with_progress(path, label):
        all_files = []
        for r, d, fs in os.walk(path):
            for f in fs: all_files.append(os.path.join(r, f))
        for f in tqdm(all_files, desc=f" \U0001f5d1\ufe0f  {label}", bar_format=del_pbar_format):
            try: os.remove(f)
            except: pass
        safe_scan_dir(path)
        shutil.rmtree(path, ignore_errors=True)

    set_wakelock(True)
    if choice == '1':
        d1 = perm_del_images(O, "Mockups")
        d2 = perm_del_images(R, "Wallpapers")
        print(f"\n {HG}\u2714 {d1 + d2}/{o_count + r_count} Tayaar Images Deleted.{ENDC}")
    elif choice == '2':
        d = perm_del_images(I, "Input")
        print(f"\n {HG}\u2714 {d}/{i_count} Input Images Deleted.{ENDC}")
    elif choice == '3':
        if not os.path.exists(W_H) or wh_count == 0:
            print(f"\n {HG2}\u2716 Warehouse khali hai!{ENDC}"); set_wakelock(False); sys.exit()
        batches = sorted([d for d in os.listdir(W_H) if "Batch_" in d])
        for i, b in enumerate(batches):
            bcount = len([f for f in os.listdir(os.path.join(W_H, b)) if f.lower().endswith(img_exts)])
            print(f" {HC}[{i + 1}]{ENDC} {b}  {HY}\u2192 {bcount} file(s){ENDC}")
        sub_choice = input(f"\n {HG}Choose Batch or 'A' (All): {ENDC}")
        if sub_choice.lower() == 'a':
            delete_folder_with_progress(W_H, "Warehouse (All)")
            os.makedirs(W_H)
            print(f"\n {HG}\u2714 Warehouse saaf ho gaya.{ENDC}")
        elif sub_choice.isdigit() and 1 <= int(sub_choice) <= len(batches):
            target_name = batches[int(sub_choice) - 1]
            target = os.path.join(W_H, target_name)
            delete_folder_with_progress(target, target_name)
            print(f"\n {HG}\u2714 Deleted.{ENDC}")
    set_wakelock(False)

