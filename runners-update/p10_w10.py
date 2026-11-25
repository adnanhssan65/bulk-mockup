import os, sys, time, shutil, subprocess, random, threading, gc, queue, colorsys
from datetime import datetime
from PIL import Image, ImageDraw, ImageFont, ImageFile, ImageFilter, ImageChops
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

# --- PROJECT CONFIGURATION ---
P_NUM = sys.argv[2] if len(sys.argv) > 2 else "10"
B = os.environ.get("MOCKUP_BASE", os.path.dirname(os.path.abspath(__file__)))
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")
BG_DIR = os.path.join(B, "backgrounds")
BG_STATE = os.path.join(B, ".bg_rotation.txt")
BG_RECENT = os.path.join(B, ".bg_recent.txt")
SLOTS_CACHE = []
T_F = "/storage/emulated/0/.mockup_time_tracker.txt"
S_P = os.path.join(B, "shape.png")
FONT_TIME = os.path.join(B, "SF Pro - Semibold Stencil.otf")
FONT_SOFT = os.path.join(B, "SF Pro - Semibold Soft.otf")   # (optional) SF Soft — agar date/Customized me chahiye to FONT_DATE/FONT_CUST isse badlo
FONT_DATE = os.path.join(B, "Poppins Bold.ttf")             # reference ka date font (Poppins Bold)
FONT_CUST = os.path.join(B, "Customized Serif Bold.ttf")    # reference ka "Customized" bold serif
SLOTS_N = 3   # is design me 3 phones hain (1 row) — group hamesha 3 ka banta hai

# --- TEXT LAYOUT (reference image se pixel-measure kiya gaya; slot ke hisab se fractions) ---
TIME_SIZE_F = 0.198    # time font size = slot width ka hissa (SF Stencil)
DATE_SIZE_F = 0.0514   # date font size (Poppins Bold)
CUST_SIZE_F = 0.0395   # "Customized" font size (bold serif)
DATE_CY_F   = 0.108   # date ink ka centre (slot height ka hissa)
TIME_CY_F   = 0.183    # time ink ka centre
CUST_CY_F   = 0.933    # Customized ink ka centre
BAR_CY_F    = 0.989    # home bar ka centre
BAR_W_F     = 0.355    # home bar ki chaudai (slot width ka hissa)

# --- TIME MODE ---
TIME_MODE  = "varied"  # "varied" = asli waqt, har screen pe ALAG (jese 3:39, 3:50...)  |  "real" = sab pe ek jesa asli waqt  |  "fixed" = hamesha FIXED_TIME
FIXED_TIME = "9:41"
VARY_RANGE = 120       # varied mode: asli waqt ke aas-paas kitne minute tak (+/-) waqt phailana hai
VARY_GAP   = 7         # varied mode: do screens ke waqt me kam az kam itne minute ka farq

# --- TEXT COLOR ---
TEXT_STYLE   = "tinted" # "tinted" = har screen pe wallpaper ke rang ke hisab se (iPhone widget jesa)  |  "white" = solid safed
BRIGHT_SPLIT = 0.78     # white mode me: agar zone is se zyada roshan ho to kala text (saaf nazar aane ke liye)
LUM_SPLIT    = 0.55     # tinted mode: is se kam roshni = halka rangin text, zyada = gehra rangin text
TINT_MIN_CONTRAST = 4.0 # text aur wallpaper ke beech kam az kam contrast (zyada = zyada saaf)

# --- SMART CHOICE ---
SMART_GROUPING = True   # True = milte-julte rang ke 6 wallpapers ek mockup me | False = folder ke order me
SMART_ARRANGE  = True   # True = group ke andar screens ko dark->light / rang ke flow me lagao
SMART_BG       = True   # True = wallpapers ke rang/roshni ke hisab se best background | False = rotate

# --- "Customized" glass pill (reference me peeche halka dark blur box) ---
BG_MODE   = "dynamic"   # "dynamic" = 3 wallpapers ke rangon se gradient bg | "library" = backgrounds folder se
TIME_OPACITY = 0.74    # time ki transparency (1.0 = solid, kam = zyada glass jesa)
PILL_ON   = False   # reference me "Customized" nahi hai
PILL_W_F  = 0.477
PILL_H_F  = 0.045
PILL_DARK = 0.20       # 0 = bilkul clear, 1 = poora kala
PILL_BLUR = 12

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

def setup_slots(canvas_w, canvas_h):
    """shape.png ke RED hisso se 6 screens detect karta hai (row-major order: pehle upar wali
    line left->right, phir neeche wali). Har slot ka exact red mask (rounded corners same)
    aur bounding box return karta hai."""
    shape_img = cv2.imread(S_P)
    if shape_img is None: return None
    if shape_img.shape[1] != canvas_w or shape_img.shape[0] != canvas_h:
        shape_img = cv2.resize(shape_img, (canvas_w, canvas_h))
    b, g, r = cv2.split(shape_img)
    soft_alpha = np.clip((r.astype(np.float32) - np.maximum(g, b).astype(np.float32)) / 255.0, 0, 1)
    red_mask = ((r.astype(int) - g.astype(int) > 40) & (r.astype(int) - b.astype(int) > 40)).astype(np.uint8)

    n, labels, stats, cent = cv2.connectedComponentsWithStats(red_mask, connectivity=8)
    cands = [lbl for lbl in range(1, n) if stats[lbl, cv2.CC_STAT_AREA] > 5000]
    if len(cands) < SLOTS_N:
        return None
    cands.sort(key=lambda l: -stats[l, cv2.CC_STAT_AREA])
    cands = cands[:SLOTS_N]

    # Row-major sort: pehle centroid-y se rows banao, phir har row me centroid-x
    cands.sort(key=lambda l: cent[l][1])
    rows = []
    for lbl in cands:
        if rows and abs(cent[lbl][1] - np.mean([cent[x][1] for x in rows[-1]])) < stats[lbl, cv2.CC_STAT_HEIGHT] * 0.5:
            rows[-1].append(lbl)
        else:
            rows.append([lbl])
    ordered = []
    for row in rows:
        row.sort(key=lambda l: cent[l][0])
        ordered.extend(row)

    slots = []
    for lbl in ordered:
        x = int(stats[lbl, cv2.CC_STAT_LEFT]); y = int(stats[lbl, cv2.CC_STAT_TOP])
        w = int(stats[lbl, cv2.CC_STAT_WIDTH]); h = int(stats[lbl, cv2.CC_STAT_HEIGHT])
        slots.append({"mask": (labels == lbl), "alpha": soft_alpha, "rect": (x, y, w, h)})
    return slots

def list_backgrounds():
    if not os.path.isdir(BG_DIR): return []
    return sorted([f for f in os.listdir(BG_DIR)
                   if f.lower().endswith(IMG_EXTS) and not f.startswith('.')])

def read_bg_state():
    try:
        with open(BG_STATE, "r") as fh: return int(fh.read().strip())
    except Exception:
        return 0

def write_bg_state(v):
    try:
        with open(BG_STATE, "w") as fh: fh.write(str(v))
    except Exception:
        pass

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

def feat_dist(a, b):
    d = float(np.linalg.norm(a["lab"] - b["lab"]))
    return d

def smart_groups(files, feats, n):
    """Folder ke sab se purane wallpaper se shuru karo, phir us ke sab se milte-julte (rang+roshni) n-1 aur uthao.
    Is tarah har mockup me ek jesa mood aata hai, aur purani files pehle process hoti hain."""
    pool = list(files); groups = []
    while len(pool) >= n:
        seed = pool.pop(0); grp = [seed]
        while len(grp) < n:
            cen = np.mean([feats[g]["lab"] for g in grp], axis=0)
            nxt = min(pool, key=lambda p: float(np.linalg.norm(feats[p]["lab"] - cen)))
            pool.remove(nxt); grp.append(nxt)
        groups.append(grp)
    return groups, pool

def smart_arrange(grp, feats):
    """Group ke andar dark -> light flow (upar wali line phir neeche wali), taake mockup ka balance acha lage."""
    return sorted(grp, key=lambda f: feats[f]["lum"])

def bg_features(designs):
    """Har background ka asal rang (screens ke bahar ka hissa) — ek dafa nikalo."""
    out = {}
    for name in designs:
        img = cv2.imread(os.path.join(BG_DIR, name))
        if img is None: continue
        H, W = img.shape[:2]
        m = np.ones((H, W), bool)
        for sl in SLOTS_CACHE:
            x, y, w, h = sl["rect"]; m[max(0, y - 6):y + h + 6, max(0, x - 6):x + w + 6] = False
        px = img[m]
        if len(px) == 0: px = img.reshape(-1, 3)
        small = px[::97].reshape(-1, 1, 3)
        out[name] = color_features(small)
    return out

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

def varied_times(now_dt, n):
    """Asli waqt ke aas-paas n alag-alag waqt (kam az kam VARY_GAP minute ka farq), ek screen pe bilkul asli waqt."""
    offs = [0]
    tries = 0
    while len(offs) < n and tries < 2000:
        tries += 1
        c = random.randint(-VARY_RANGE, VARY_RANGE)
        if all(abs(c - o) >= VARY_GAP for o in offs): offs.append(c)
    while len(offs) < n: offs.append(offs[-1] + VARY_GAP)
    random.shuffle(offs)
    from datetime import timedelta
    out = []
    for o in offs:
        t = now_dt + timedelta(minutes=o)
        out.append(f"{t.hour % 12 or 12}:{t.strftime('%M')}")
    return out

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

def draw_pills(img_pil, slots):
    """Customized ke peeche halka dark glass pill (blur + dark)."""
    SSP = 4
    for slot in slots:
        x, y, w, h = slot["rect"]
        pw, ph = w * PILL_W_F, h * PILL_H_F
        cx, cy = x + w / 2, y + h * CUST_CY_F
        box = (int(round(cx - pw / 2)), int(round(cy - ph / 2)), int(round(cx + pw / 2)), int(round(cy + ph / 2)))
        cw, ch = box[2] - box[0], box[3] - box[1]
        crop = img_pil.crop(box)
        glass = Image.blend(crop.filter(ImageFilter.GaussianBlur(PILL_BLUR)), Image.new("RGB", crop.size, (0, 0, 0)), PILL_DARK)
        m = Image.new("L", (cw * SSP, ch * SSP), 0)
        ImageDraw.Draw(m).rounded_rectangle([0, 0, cw * SSP - 1, ch * SSP - 1], radius=ch * SSP / 2, fill=255)
        img_pil.paste(glass, box[:2], m.resize((cw, ch), Image.LANCZOS))

# ---------------- LOCK SCREEN UI (reference jesa): status icons, torch/camera buttons ----------------
def _icon(fn, bw, bh, SS=6):
    bw, bh = max(2, int(bw)), max(2, int(bh))
    im = Image.new("L", (bw * SS, bh * SS), 0)
    fn(ImageDraw.Draw(im), bw * SS, bh * SS)
    return im.resize((bw, bh), Image.LANCZOS)

def _paste_icon(tmask, im, cx, cy):
    tmask.paste(255, (int(round(cx - im.width / 2)), int(round(cy - im.height / 2))), im)

def _torch(d, W, H):
    d.rounded_rectangle([0, 0, W, H * 0.30], radius=W * 0.18, fill=255)
    d.rounded_rectangle([W * 0.19, H * 0.26, W * 0.81, H], radius=W * 0.2, fill=255)
    d.rectangle([0, H * 0.205, W, H * 0.245], fill=0)
    d.ellipse([W * 0.40, H * 0.52, W * 0.60, H * 0.52 + W * 0.2], fill=0)

def _camera(d, W, H):
    d.rounded_rectangle([W * 0.32, 0, W * 0.68, H * 0.30], radius=H * 0.08, fill=255)
    d.rounded_rectangle([0, H * 0.16, W, H], radius=H * 0.2, fill=255)
    r = H * 0.27; cx, cy = W / 2, H * 0.60
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=0)
    r2 = r * 0.62
    d.ellipse([cx - r2, cy - r2, cx + r2, cy + r2], fill=255)

def _plane(d, W, H):
    pts = [(98,50),(80,57),(58,57),(36,88),(25,88),(40,57),(18,57),(10,67),(2,67),(10,50),(2,33),(10,33),(18,43),(40,43),(25,12),(36,12),(58,43),(80,43)]
    d.polygon([(x * W / 100, y * H / 100) for x, y in pts], fill=255)

def _bell(d, W, H):
    d.ellipse([W * 0.5 - W * 0.08, 0, W * 0.5 + W * 0.08, H * 0.16], fill=255)
    d.pieslice([W * 0.14, H * 0.08, W * 0.86, H * 0.80], 180, 360, fill=255)
    d.polygon([(W * 0.14, H * 0.44), (W * 0.86, H * 0.44), (W * 0.96, H * 0.80), (W * 0.04, H * 0.80)], fill=255)
    d.ellipse([W * 0.5 - W * 0.12, H * 0.80, W * 0.5 + W * 0.12, H], fill=255)
    d.line([(W * 0.06, H * 0.02), (W * 0.94, H * 0.98)], fill=0, width=int(W * 0.26))
    d.line([(W * 0.06, H * 0.02), (W * 0.94, H * 0.98)], fill=255, width=int(W * 0.12))

def _battery(d, W, H, txt="71"):
    bw = W * 0.93
    d.rounded_rectangle([0, 0, bw, H], radius=H * 0.32, fill=255)
    d.rounded_rectangle([W * 0.955, H * 0.32, W, H * 0.68], radius=H * 0.12, fill=255)
    try: f = ImageFont.truetype(FONT_DATE, int(H * 0.74))
    except Exception: f = ImageFont.load_default()
    l, t, r, b = f.getbbox(txt)
    d.text((bw / 2 - (l + r) / 2, H / 2 - (t + b) / 2), txt, font=f, fill=0)

def draw_buttons(img_pil, slots):
    """Torch/camera ke peeche halka dark glass circle (reference jesa)."""
    SSP = 4
    for slot in slots:
        x, y, w, h = slot["rect"]
        r = w * 0.073; cy = y + h * 0.909
        for cx in (x + w * 0.184, x + w * 0.813):
            box = (int(cx - r), int(cy - r), int(cx + r), int(cy + r))
            cw, ch = box[2] - box[0], box[3] - box[1]
            crop = img_pil.crop(box)
            glass = Image.blend(crop.filter(ImageFilter.GaussianBlur(8)), Image.new("RGB", crop.size, (0, 0, 0)), 0.28)
            m = Image.new("L", (cw * SSP, ch * SSP), 0)
            ImageDraw.Draw(m).ellipse([0, 0, cw * SSP - 1, ch * SSP - 1], fill=255)
            img_pil.paste(glass, box[:2], m.resize((cw, ch), Image.LANCZOS))

def draw_lock_ui(tmask, x, y, w, h):
    """Status bar (bell, airplane, battery 71) + torch + camera — text jese hi mask me; rang baad me lagta hai."""
    sy = y + h * 0.035
    _paste_icon(tmask, _icon(_bell, w * 0.036, w * 0.040), x + w * 0.181, sy)
    _paste_icon(tmask, _icon(_plane, w * 0.046, w * 0.046), x + w * 0.768, sy)
    _paste_icon(tmask, _icon(_battery, w * 0.078, w * 0.036), x + w * 0.848, sy)
    by = y + h * 0.909
    _paste_icon(tmask, _icon(_torch, w * 0.040, w * 0.070), x + w * 0.184, by)
    _paste_icon(tmask, _icon(_camera, w * 0.070, w * 0.055), x + w * 0.813, by)

# ---------------- DYNAMIC BACKGROUND: 3 wallpapers ke rangon se gradient ----------------
def _vivid_color(bgr_thumb, K=5):
    """Wallpaper ka sab se achha rang (prominent + saturated) — RGB float."""
    px = cv2.resize(bgr_thumb, (48, 48), interpolation=cv2.INTER_AREA).reshape(-1, 3).astype(np.float32)
    K = min(K, len(px))
    crit = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 15, 1.0)
    _, lab, cen = cv2.kmeans(px, K, None, crit, 3, cv2.KMEANS_PP_CENTERS)
    lab = lab.ravel()
    best, best_sc = cen[0][::-1].copy(), -1
    for k in range(K):
        frac = float((lab == k).mean())
        b, g, r = cen[k] / 255.0
        mx, mn = max(r, g, b), min(r, g, b)
        sat = 0 if mx == 0 else (mx - mn) / mx
        tone = 1.0 if 0.14 < mx < 0.97 else 0.35          # bohat kala/safed nahi
        sc = (frac ** 0.6) * (0.15 + sat) * tone
        if sc > best_sc: best_sc, best = sc, cen[k][::-1].copy()
    return best

def _boost(rgb, sat_mul=1.15, val_lo=0.55, val_hi=0.95):
    r, g, b = [float(c) / 255.0 for c in rgb]
    mx, mn = max(r, g, b), min(r, g, b)
    v = min(val_hi, max(val_lo, mx)); s_ = 0 if mx == 0 else min(1.0, (mx - mn) / mx * sat_mul)
    c = cv2.cvtColor(np.uint8([[[int(b * 255), int(g * 255), int(r * 255)]]]), cv2.COLOR_BGR2HSV)[0, 0].astype(np.float32)
    c[1] = min(255, s_ * 255); c[2] = v * 255
    out = cv2.cvtColor(np.uint8([[c]]), cv2.COLOR_HSV2BGR)[0, 0][::-1]
    return out.astype(np.float32)

def make_dynamic_bg(files, slots, W_C, H_C):
    """3 wallpapers ke rang -> har phone ke peeche usi rang ka glow, upar roshan / neeche gehra."""
    cols = []
    for f in files:
        t = thumb_bgr(os.path.join(I, f))
        cols.append(_boost(_vivid_color(t)))
    mean = np.mean(cols, axis=0)
    yy, xx = np.mgrid[0:H_C, 0:W_C].astype(np.float32)
    img = np.ones((H_C, W_C, 3), np.float32) * (mean * 0.20)
    for sl, c in zip(slots, cols):
        x, y, w, h = sl["rect"]; cx = x + w / 2
        g = np.exp(-(((xx - cx) / (W_C * 0.30)) ** 2 + ((yy - H_C * 0.28) / (H_C * 0.42)) ** 2))
        img += c[None, None, :] * (g[..., None] * 0.78)
    # neeche bhi rang: har mockup me bottom ke rang thode alag (wallpapers ke rangon ka rotation/mix)
    shift = random.choice([0, 1, 2]); mixw = random.uniform(0.25, 0.5)
    for i, sl in enumerate(slots):
        x, y, w, h = sl["rect"]; cx = x + w / 2
        cb = cols[(i + shift) % len(cols)] * (1 - mixw) + mean * mixw
        g2 = np.exp(-(((xx - cx) / (W_C * 0.34)) ** 2 + ((yy - H_C * 0.86) / (H_C * 0.26)) ** 2))
        img += cb[None, None, :] * (g2[..., None] * 0.62)
    img = np.clip(img, 0, 255)
    fall = 1 - 0.42 * np.clip(yy / H_C, 0, 1) ** 1.6
    edge = 1 - 0.28 * np.clip(np.abs(xx - W_C / 2) / (W_C / 2), 0, 1) ** 2
    img *= (fall * edge)[..., None]
    union = np.zeros((H_C, W_C), np.uint8)
    for sl in slots: union |= (sl["mask"].astype(np.uint8) * 255)
    blur = cv2.GaussianBlur(union.astype(np.float32) / 255, (0, 0), 48)
    blur = np.roll(blur, 10, axis=0)
    img *= (1 - 0.45 * np.clip(blur * 1.25, 0, 1))[..., None]
    return cv2.cvtColor(np.clip(img, 0, 255).astype(np.uint8), cv2.COLOR_RGB2BGR), cols

def process_group(files6, bg_path, slots, idx, t_now, is_hd):
    """Ek group (6 wallpapers) process karta hai: 1 mockup + 6 ready wallpapers.
    Success pe ('ok', agli idx, akhri timestamp), warna exception."""
    bg = cv2.imread(bg_path)
    if bg is None: raise IOError("background load nahi hua")
    canvas = bg.copy()
    H_C, W_C = canvas.shape[:2]

    ready_imgs = []
    for f, slot in zip(files6, slots):
        raw = safe_imread(os.path.join(I, f))
        if raw is None: raise IOError(f"input unreadable: {f}")
        # Ready wallpaper (1080x1920, baghair text ke)
        w_ready = resize_exact(raw) if crop_916 else raw
        if is_hd: w_ready = apply_hd_enhance(w_ready)
        ready_imgs.append(w_ready)
        # Slot ke size pe cover-fit, phir exact mask se paste
        x, y, w, h = slot["rect"]
        fit = resize_exact(raw, w, h)
        if is_hd: fit = apply_hd_enhance(fit)
        al = slot["alpha"][y:y + h, x:x + w][..., None]   # soft edges = smooth rounded corners
        region = canvas[y:y + h, x:x + w].astype(np.float32)
        canvas[y:y + h, x:x + w] = np.clip(fit.astype(np.float32) * al + region * (1 - al), 0, 255).astype(np.uint8)
        del raw, fit, region

    # --- Time / date / Customized overlay (har screen pe, asli waqt, adaptive color) ---
    canvas_bgr = canvas
    fin_pil = Image.fromarray(cv2.cvtColor(canvas, cv2.COLOR_BGR2RGB))
    canvas = None
    if PILL_ON: draw_pills(fin_pil, slots)
    draw_buttons(fin_pil, slots)
    tmask = Image.new("L", fin_pil.size, 0)
    color_layer = Image.new("RGB", fin_pil.size, (255, 255, 255))
    draw = ImageDraw.Draw(tmask)
    tmask_time = Image.new("L", fin_pil.size, 0)
    draw_t = ImageDraw.Draw(tmask_time)
    now_dt = datetime.now()
    if TIME_MODE == "fixed": times = [FIXED_TIME] * len(slots)
    elif TIME_MODE == "real": times = [f"{now_dt.hour % 12 or 12}:{now_dt.strftime('%M')}"] * len(slots)
    else: times = varied_times(now_dt, len(slots))
    date_str = now_dt.strftime("%a ") + str(now_dt.day) + now_dt.strftime(" %b")  # e.g. Tue 20 Oct
    # Color zones pill ke BAAD ki asli tasveer se nikalo (Customized pill ke upar hota hai)
    canvas_bgr = cv2.cvtColor(np.array(fin_pil), cv2.COLOR_RGB2BGR)

    for slot_i, slot in enumerate(slots):
        time_str = times[slot_i]
        x, y, w, h = slot["rect"]
        cx = x + w / 2
        # Upar wala text (date/time): us hisse ke wallpaper ke rang se matched color
        _zf = zone_text_rgb if TEXT_STYLE == "tinted" else zone_plain_rgb
        top_rgb = _zf(canvas_bgr, int(x + w * 0.2), y + int(h * 0.08), int(w * 0.6), int(h * 0.17))
        # Neeche wala text (Customized/bar): neeche ke hisse ke rang se matched color
        bot_rgb = _zf(canvas_bgr, int(x + w * 0.10), y + int(h * 0.87), int(w * 0.80), int(h * 0.12))
        f_time = ImageFont.truetype(FONT_TIME, max(10, round(w * TIME_SIZE_F)))
        f_date = ImageFont.truetype(FONT_DATE, max(8, round(w * DATE_SIZE_F)))
        draw_ink(draw, date_str, f_date, cx, y + h * DATE_CY_F)
        draw_ink(draw_t, time_str, f_time, cx, y + h * TIME_CY_F)
        draw_lock_ui(tmask, x, y, w, h)
        bw = w * BAR_W_F; bh = max(3, round(h * 0.0055)); cyb = y + h * BAR_CY_F
        draw.rounded_rectangle([cx - bw / 2, cyb - bh / 2, cx + bw / 2, cyb + bh / 2], radius=bh / 2, fill=255)
        # Is slot ke upar/neeche hisso ke rang color layer me bhar do
        cl = ImageDraw.Draw(color_layer)
        cl.rectangle([x, y, x + w, y + int(h * 0.5)], fill=top_rgb)
        cl.rectangle([x, y + int(h * 0.5), x + w, y + h], fill=bot_rgb)

    shadow = tmask.filter(ImageFilter.GaussianBlur(4)).point(lambda v: int(v * 0.22))
    fin_pil.paste((0, 0, 0), (0, 3), shadow)
    tmask = ImageChops.lighter(tmask, tmask_time.point(lambda v: int(v * TIME_OPACITY)))
    fin_pil.paste(color_layer, (0, 0), tmask)
    fin = cv2.cvtColor(np.array(fin_pil.convert("RGB")), cv2.COLOR_RGB2BGR)
    del fin_pil, tmask, color_layer, canvas_bgr

    def verify_saved(path):
        try:
            if not os.path.exists(path) or os.path.getsize(path) < 100: return False
            with Image.open(path) as im: im.verify()
            return True
        except Exception:
            return False

    mock_path = os.path.join(O, f"{idx}.jpg")
    save_optimized(fin, mock_path)
    os.utime(mock_path, (t_now + 60, t_now + 60))
    del fin

    out_paths = [mock_path]
    t = t_now + 60
    for j, w_ready in enumerate(ready_imgs):
        t += 60
        p = os.path.join(R, f"{idx + 1 + j}.jpg")
        save_optimized(w_ready, p)
        os.utime(p, (t, t))
        out_paths.append(p)
    del ready_imgs

    if not all(verify_saved(p) for p in out_paths):
        raise IOError("save verify failed")

    # Scans bilkul isi order me (sequence safe), phir inputs delete + unke scans
    for p in out_paths: safe_scan(p)   # order: mockup, phir wallpapers
    gallery_copy_outputs([(p, t_now + 60 * (i + 1)) for i, p in enumerate(out_paths)])
    for f in files6:
        rp = os.path.join(I, f)
        try: os.remove(rp)
        except Exception: pass

    return 'ok', idx + 1 + SLOTS_N, t

if len(sys.argv) < 2:
    action = 'process'   # bare `w8` = process
else:
    action = sys.argv[1]

if action == 'process':
    display_header()
    os.makedirs(O, exist_ok=True); os.makedirs(R, exist_ok=True)

    if not os.path.isdir(I):
        print(f" {C2}\u2716 Input folder nahi mila: {P_NUM}_wallpapers{ENDC}")
        sys.exit()
    imgs = list_inputs()
    imgs.sort(key=lambda f: os.path.getctime(os.path.join(I, f)))  # folder me aane ka order = lagne ka order

    total_groups = len(imgs) // SLOTS_N
    leftover = len(imgs) % SLOTS_N
    if total_groups == 0:
        if len(imgs) == 0:
            print(f" {C2}\u2716 {P_NUM}_wallpapers me images nahi mili.{ENDC}")
        else:
            print(f" {C2}\u2716 Sirf {len(imgs)} wallpaper(s) hain — ye design {SLOTS_N} ke group pe chalta hai.{ENDC}")
            print(f" {C3}\u279c {SLOTS_N} pure hote hi process hoga, ye wale folder me hi rahenge.{ENDC}")
        sys.exit()

    print(f" {C5}\u273f {BOLD}S e l e c t  M o d e{ENDC}")
    print(f" {C1}[1]{ENDC} HD Mode (Galaxy AI Enhance) - {C4}[DEFAULT]{ENDC}")
    print(f" {C1}[2]{ENDC} Normal (Fast KB Pack)")
    choice = input(f"\n {C4}\u279c {ENDC}").strip()
    is_hd = (choice != '2')

    print(f" {C5}\u273f {BOLD}S e l e c t  S i z e{ENDC}")
    print(f" {C1}[1]{ENDC} Original Size - {C4}[DEFAULT]{ENDC}")
    print(f" {C1}[2]{ENDC} Crop 9:16")

    size_choice = input(f"\n {C4}\u279c {ENDC}").strip()
    crop_916 = (size_choice == '2')

    if not os.path.exists(S_P):
        print(f" {C2}\u2716 shape.png missing!{ENDC}")
        sys.exit()
    for _ff in (FONT_TIME, FONT_DATE):
        if not os.path.exists(_ff):
            print(f" {C2}\u2716 Font file missing: {os.path.basename(_ff)}{ENDC}")
            sys.exit()
    designs = list_backgrounds()
    if BG_MODE == "dynamic":
        # backgrounds folder ki zaroorat nahi: size shape.png se, rang wallpapers se
        if not designs: designs = ["dynamic"]
        _sh = cv2.imread(S_P)
        if _sh is None:
            print(f" {C2}\u2716 shape.png kharab/missing hai.{ENDC}")
            sys.exit()
        H_C, W_C = _sh.shape[:2]
        del _sh
        bg_idx = read_bg_state()
    else:
        if not designs:
            print(f" {C2}\u2716 backgrounds folder khali hai — pehle design file(s) rakho.{ENDC}")
            sys.exit()
        bg_idx = read_bg_state()
        first_bg = cv2.imread(os.path.join(BG_DIR, designs[bg_idx % len(designs)]))
        if first_bg is None:
            print(f" {C2}\u2716 Background file kharab hai: {designs[bg_idx % len(designs)]}{ENDC}")
            sys.exit()
        H_C, W_C = first_bg.shape[:2]
        del first_bg

    slots = setup_slots(W_C, H_C)
    if slots is None:
        print(f" {C2}\u2716 shape.png me {SLOTS_N} red screens nahi mile — design check karo.{ENDC}")
        sys.exit()
    SLOTS_CACHE[:] = slots
    print(f" {C4}\u2714 Mockup engine ready! ({SLOTS_N} screens, {len(designs)} background design(s)){ENDC}")

    # --- SMART: har wallpaper ka rang/roshni nikalo, milte-julte 6 ka group banao ---
    feats = {}
    unreadable = []
    for f in tqdm(imgs, desc=" \U0001f50e Analyzing ", bar_format=f" {C1}{{desc}}{ENDC} {C5}|{ENDC}{{bar:20}}{C5}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"):
        t = thumb_bgr(os.path.join(I, f))
        if t is None: unreadable.append(f); continue
        feats[f] = color_features(t)
    imgs = [f for f in imgs if f in feats]
    leftover_files = []
    if SMART_GROUPING:
        group_list, leftover_files = smart_groups(imgs, feats, SLOTS_N)
    else:
        group_list = [imgs[i * SLOTS_N:(i + 1) * SLOTS_N] for i in range(len(imgs) // SLOTS_N)]
        leftover_files = imgs[len(group_list) * SLOTS_N:]
    if SMART_ARRANGE:
        group_list = [smart_arrange(g, feats) for g in group_list]
    total_groups = len(group_list)
    leftover = len(leftover_files)
    bgf = bg_features(designs) if (SMART_BG and BG_MODE != "dynamic") else {}
    recent = read_recent()

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
    desc_text = "\u2728 HD Mode " if is_hd else "\u2699\ufe0f Normal "

    processed_count = 0
    corrupt_count = 0
    skip_log = []
    set_wakelock(True)
    for gi in tqdm(range(total_groups), desc=desc_text, bar_format=pbar_format):
        files6 = group_list[gi]
        if SMART_BG and bgf:
            bg_name = pick_background(designs, bgf, [feats[f] for f in files6], recent) or designs[bg_idx % len(designs)]
        else:
            bg_name = designs[bg_idx % len(designs)]
        bg_path = os.path.join(BG_DIR, bg_name)
        if BG_MODE == "dynamic":
            try:
                dyn_img, _cols = make_dynamic_bg(files6, slots, W_C, H_C)
                bg_path = os.path.join(B, ".dynamic_bg.png")
                cv2.imwrite(bg_path, dyn_img); del dyn_img
                bg_name = "dynamic"
            except Exception:
                bg_path = os.path.join(BG_DIR, bg_name)      # fail ho to library bg
        success = False
        last_err = None
        for attempt in range(3):
            try:
                status, new_idx, new_t = process_group(files6, bg_path, slots, s_idx, cur_t, is_hd)
                if status == 'ok':
                    s_idx, cur_t = new_idx, new_t
                    processed_count += 1
                    success = True
                    bg_idx += 1
                    write_bg_state(bg_idx)
                    recent.append(bg_name); recent = recent[-3:]
                    if bg_name != 'dynamic': write_recent(recent)
                    break
                else:
                    last_err = "padhi nahi gayi"
                    break
            except Exception as e:
                last_err = f"{type(e).__name__}: {str(e)[:50]}"
                # Adhoore outputs saaf karo taake numbering kharab na ho
                for p in [os.path.join(O, f"{s_idx}.jpg")] + [os.path.join(R, f"{s_idx + 1 + j}.jpg") for j in range(SLOTS_N)]:
                    if os.path.exists(p):
                        try: os.remove(p)
                        except: pass
                gc.collect()
                time.sleep(0.3)

        if not success:
            corrupt_count += 1
            skip_log.append(f"Group {gi + 1} ({last_err})")

        gc.collect()

    set_wakelock(False)

    if processed_count > 0:
        try:
            with open(T_F, "w") as fh: fh.write(str(cur_t))
        except: pass
        print(f"\n {C4}\u2714 {processed_count} Group(s) aesthetic style mein ready! (har group = 1 Mockup + {SLOTS_N} Wallpapers){ENDC}")
    else:
        print(f"\n {C5}\u2714 Sab already set hai.{ENDC}")

    if unreadable:
        print(f" {C2}\u26a0 {len(unreadable)} file(s) padhi nahi gayin (folder me rahengi):{ENDC}")
        for u in unreadable[:8]: print(f"   {C2}- {u} ({READ_ERR.get(os.path.join(I, u), '?')}){ENDC}")
    if leftover:
        print(f" {C3}\u279c {leftover} wallpaper(s) reh gaye — {SLOTS_N} pure hote hi agli baar process honge.{ENDC}")

    if corrupt_count > 0:
        print(f" {C2}\u26a0 {corrupt_count} group(s) skip kiye gaye (dobara retry honge agle run me):{ENDC}")
        for s in skip_log[:8]:
            print(f"   {C2}- {s}{ENDC}")
        if len(skip_log) > 8:
            print(f"   {C2}...aur {len(skip_log) - 8} more{ENDC}")
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
