import os, sys, time, shutil, subprocess, random, threading, gc
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
    
    #  Fake Boot Sequence 
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
    
    #  GRADIENT ADNAN (Matrix Green) 
    ascii_art = [
        "   \u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2557   \u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2557   \u2588\u2588\u2557",
        "  \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2551",
        "  \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2551\u2588\u2588\u2551  \u2588\u2588\u2551\u2588\u2588\u2554\u2588\u2588\u2557 \u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2551\u2588\u2588\u2554\u2588\u2588\u2557 \u2588\u2588\u2551",
        "  \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2551\u2588\u2588\u2551  \u2588\u2588\u2551\u2588\u2588\u2551\u255a\u2588\u2588\u2557\u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2551\u2588\u2588\u2551\u255a\u2588\u2588\u2557\u2588\u2588\u2551",
        "  \u2588\u2588\u2551  \u2588\u2588\u2551\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255d\u2588\u2588\u2551 \u255a\u2588\u2588\u2588\u2588\u2551\u2588\u2588\u2551  \u2588\u2588\u2551\u2588\u2588\u2551 \u255a\u2588\u2588\u2588\u2588\u2551",
        "  \u255a\u2550\u255d  \u255a\u2550\u255d\u255a\u2550\u2550\u2550\u2550\u2550\u255d \u255a\u2550\u255d  \u255a\u2550\u2550\u2550\u255d\u255a\u2550\u255d  \u255a\u2550\u255d\u255a\u2550\u255d  \u255a\u2550\u2550\u2550\u255d"
    ]
    for line in ascii_art:
        print(f"{HG}{BOLD}{line}{ENDC}")
    
    #  Small Lock/Computer Icon 
    print(f"\n{HC}      \u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510")
    print(f"      \u2502   \U0001f4bb  R O O T   M O D E    \u2502")
    print(f"      \u2502   \U0001f513  A C C E S S   O K    \u2502")
    print(f"      \u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518{ENDC}\n")
    
    #  Flower + Welcome Mix 
    print(f" {HG}\U0001f338 \u26a1 \U0001f338   W e l c o m e   t o   t h e   S y s t e m   \U0001f338 \u26a1 \U0001f338{ENDC}\n")
    
    #  Secure Info Box (auto-sized, no manual alignment errors) 
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
    
    #  Access Granted Footer 
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

def safe_scan(path, timeout_sec=1):
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

def safe_imread(path):
    try:
        with Image.open(path) as im:
            im.load()
            rgb = im.convert('RGB')
        return np.array(rgb)[:, :, ::-1].copy()  # RGB -> BGR (cv2 format)
    except Exception:
        return None

cv2.setNumThreads(0)

# --- PROJECT 7 CONFIGURATION ---
P_NUM = sys.argv[2] if len(sys.argv) > 2 else "7"
B = os.environ.get("MOCKUP_BASE", os.path.dirname(os.path.abspath(__file__)))
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")
T_F = "/storage/emulated/0/.mockup_time_tracker.txt"
M_P = os.path.join(B, "mockup.png")
S_P = os.path.join(B, "shape.jpg")
FONT_FILE = os.path.join(B, "Losta Masta Light.ttf")

FONT_SMALL_FILE = os.path.join(B, "New York - Semibold.otf")

# --- TEXT LAYOUT (measured from the reference image, ink-exact) ---
BIGTIME_SIZE = 123   # Losta - big clock
DATE_SIZE    = 44    # Losta - date line
SMALL_SIZE   = 34    # New York Semibold - small top time
# Per phone (left, right): offsets from each screen-hole's top-left corner.
DATE_C   = [(401, 219), (403, 212)]      # ink centre of date line
BIG_C    = [(390.5, 333.5), (401.5, 328.5)]  # ink centre of big clock
SMALL_R  = [(108, 64), (107, 63)]        # (ink right edge, ink vertical centre) of small time

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

def save_optimized(cv2_img, output_path):
    rgb = cv2.cvtColor(cv2_img, cv2.COLOR_BGR2RGB)
    pil_img = Image.fromarray(rgb)
    try:
        srgb_profile = cms.createProfile('sRGB')
        srgb_bytes = cms.ImageCmsProfile(srgb_profile).tobytes()
        pil_img.save(output_path, format='JPEG', quality=87, subsampling=0, icc_profile=srgb_bytes, optimize=True)
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
    if len(img.shape) == 2: img = cv2.cvtColor(img, cv2.COLOR_GRAY2BGRA)
    elif img.shape[2] == 3: img = np.concatenate([img, np.full((img.shape[0], img.shape[1], 1), 255, dtype=np.uint8)], axis=2)
    return img

def draw_ink(draw, text, font, cx, cy, align="center"):
    """Place text so its INK box (not font metrics) is centred at (cx,cy),
    or right-aligned at cx with vertical centre cy."""
    l, t, r, btm = font.getbbox(text)
    y = cy - (t + btm) / 2
    x = (cx - r) if align == "right" else (cx - (l + r) / 2)
    draw.text((x, y), text, font=font, fill=255)

def _setup_mockup():
    """Loads mockup.png + shape.jpg, finds the 2 red screen regions in shape.jpg
    (rounded-corner masks preserved exactly, since we read them straight from the
    red pixels rather than approximating a rectangle), sorts them left-to-right,
    and builds a perspective transform per region - same technique as Project 1,
    just done twice (once per phone)."""
    mk = load_rgba(M_P)
    if mk is None: return None
    hm, wm = mk.shape[:2]

    shape_img = cv2.imread(S_P)
    if shape_img is None: return None
    shape_img = cv2.resize(shape_img, (wm, hm))
    b, g, r = cv2.split(shape_img)
    red_mask = ((r.astype(int) - g.astype(int) > 40) & (r.astype(int) - b.astype(int) > 40)).astype(np.uint8) * 255

    n, labels, stats, cent = cv2.connectedComponentsWithStats(red_mask, connectivity=8)
    candidates = [lbl for lbl in range(1, n) if stats[lbl, cv2.CC_STAT_AREA] > 5000]
    if len(candidates) < 2:
        return None
    candidates.sort(key=lambda l: -stats[l, cv2.CC_STAT_AREA])
    candidates = candidates[:2]
    candidates.sort(key=lambda l: cent[l][0])  # left phone first, right phone second

    slots = []
    for lbl in candidates:
        rmask = (labels == lbl).astype(np.uint8) * 255
        cnts, _ = cv2.findContours(rmask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        c = max(cnts, key=cv2.contourArea)
        d = order_points(cv2.boxPoints(cv2.minAreaRect(c)).astype(np.float32))
        x, y, w, h = cv2.boundingRect(c)
        pm = cv2.getPerspectiveTransform(np.float32([[0, 0], [w, 0], [w, h], [0, h]]), d)
        slots.append({"mask": rmask, "M": pm, "rect": (x, y, w, h), "size": (w, h)})

    mb, ma = mk[:, :, :3].astype(np.float32), mk[:, :, 3].astype(np.float32) / 255.0
    return hm, wm, slots, mb, ma

def process_one_pair(f1, f2, idx, t_now, slots, W_M, H_M, m_bgr, m_a, is_hd, font_big, font_date, font_small):
    raw1, raw2 = os.path.join(I, f1), os.path.join(I, f2)
    w1, w2 = safe_imread(raw1), safe_imread(raw2)
    if w1 is None or w2 is None:
        return 'unreadable', idx, t_now

    w1e, w2e = (resize_exact(w1) if crop_916 else w1), (resize_exact(w2) if crop_916 else w2)
    w1p = apply_hd_enhance(w1e) if is_hd else w1e
    w2p = apply_hd_enhance(w2e) if is_hd else w2e

    mock_path = os.path.join(O, f"{idx}.jpg")
    wall1_path = os.path.join(R, f"{idx + 1}.jpg")
    wall2_path = os.path.join(R, f"{idx + 2}.jpg")
    mt, w1t, w2t = t_now + 60, t_now + 120, t_now + 180

    combined = np.zeros_like(m_bgr)
    for orig, slot in zip((w1, w2), slots):
        # original ratio kept: scale up to COVER the frame, centre-crop the overflow (no stretching)
        sw, sh = slot["size"]
        fit = resize_exact(orig, sw, sh)
        if is_hd: fit = apply_hd_enhance(fit)
        warped = cv2.warpPerspective(fit, slot["M"], (W_M, H_M), flags=cv2.INTER_LANCZOS4)
        keep = slot["mask"][:, :, None] > 0
        combined = np.where(keep, warped, combined)

    fin = np.empty_like(m_bgr)
    for c in range(3):
        fin[:, :, c] = (1 - m_a) * combined[:, :, c] + m_a * m_bgr[:, :, c]
    fin = np.clip(fin, 0, 255).astype(np.uint8)

    # --- Time / date overlay - always the real current time, per mockup ---
    fin_pil = Image.fromarray(cv2.cvtColor(fin, cv2.COLOR_BGR2RGB))
    tmask = Image.new("L", fin_pil.size, 0)
    draw = ImageDraw.Draw(tmask)
    now_dt = datetime.now()
    big_str = now_dt.strftime("%H:%M")
    date_str = now_dt.strftime("%A %b ") + str(now_dt.day)
    small_hour = now_dt.hour % 12 or 12
    small_str = f"{small_hour}:{now_dt.strftime('%M')}"

    for k, slot in enumerate(slots):
        x, y, w, h = slot["rect"]
        draw_ink(draw, date_str, font_date, x + DATE_C[k][0], y + DATE_C[k][1])
        draw_ink(draw, big_str, font_big, x + BIG_C[k][0], y + BIG_C[k][1])
        draw_ink(draw, small_str, font_small, x + SMALL_R[k][0], y + SMALL_R[k][1], align="right")

    shadow = tmask.filter(ImageFilter.GaussianBlur(4)).point(lambda v: int(v * 0.4))
    fin_pil.paste((0, 0, 0), (0, 3), shadow)
    fin_pil.paste((255, 255, 255), (0, 0), tmask)

    fin_final = cv2.cvtColor(np.array(fin_pil.convert("RGB")), cv2.COLOR_RGB2BGR)

    def verify_saved(path):
        try:
            if not os.path.exists(path) or os.path.getsize(path) < 100: return False
            with Image.open(path) as im: im.verify()
            return True
        except Exception:
            return False

    save_optimized(fin_final, mock_path)
    os.utime(mock_path, (mt, mt))
    save_optimized(w1p, wall1_path)
    os.utime(wall1_path, (w1t, w1t))
    save_optimized(w2p, wall2_path)
    os.utime(wall2_path, (w2t, w2t))

    if not (verify_saved(mock_path) and verify_saved(wall1_path) and verify_saved(wall2_path)):
        raise IOError("save verify failed")

    safe_scan(mock_path); safe_scan(wall1_path); safe_scan(wall2_path)
    gallery_copy_outputs([(mock_path, mt), (wall1_path, w1t), (wall2_path, w2t)])
    os.remove(raw1); os.remove(raw2)

    del w1, w2, w1e, w2e, w1p, w2p, combined, fin, fin_pil
    return 'ok', idx + 3, w2t

if len(sys.argv) < 2:
    action = 'process'   # bare `w7` still just runs process, like before
else:
    action = sys.argv[1]

if action == 'process':
    display_header()
    os.makedirs(O, exist_ok=True); os.makedirs(R, exist_ok=True)

    imgs = [f for f in os.listdir(I) if f.lower().endswith(('.jpg', '.jpeg', '.png', '.webp', '.avif', '.heic')) and not f.startswith('.') and "_temp" not in f]
    imgs.sort(key=lambda f: os.path.getctime(os.path.join(I, f)))  # order files arrived in folder = selection order

    total_pairs = len(imgs) // 2
    leftover = len(imgs) % 2
    if total_pairs == 0:
        print(f" {C2}\u2716 Pair banane ke liye kam az kam 2 wallpapers chahiye.{ENDC}")
        sys.exit()
    pair_imgs = imgs[:total_pairs * 2]

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

    if not os.path.exists(M_P) or not os.path.exists(S_P):
        print(f" {C2}\u2716 mockup.png ya shape.jpg missing!{ENDC}")
        sys.exit()
    for _ff in (FONT_FILE, FONT_SMALL_FILE):
        if not os.path.exists(_ff):
            print(f" {C2}\u2716 Font file missing: {_ff}{ENDC}")
            sys.exit()

    _result = {}
    _t = threading.Thread(target=lambda: _result.update(data=_setup_mockup()))
    _t.start()
    _frames = ['\u2022', 'o', 'O', 'o']
    _i = 0
    while _t.is_alive():
        sys.stdout.write(f"\r {C1}\u273f{ENDC} {C5}{_frames[_i % 4]}{ENDC} Loading mockup engine...")
        sys.stdout.flush()
        time.sleep(0.08)
        _i += 1
    _t.join()

    if _result.get('data') is None:
        sys.stdout.write(f"\r {C2}\u2716{ENDC} shape.jpg mein 2 red regions nahi mile - design check karo.\n")
        sys.exit()
    sys.stdout.write(f"\r {C4}\u2714{ENDC} Mockup engine ready!            \n")
    H_M, W_M, slots, m_bgr, m_a = _result['data']

    font_big = ImageFont.truetype(FONT_FILE, BIGTIME_SIZE)
    font_date = ImageFont.truetype(FONT_FILE, DATE_SIZE)
    font_small = ImageFont.truetype(FONT_SMALL_FILE, SMALL_SIZE)

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
    for i in tqdm(range(total_pairs), desc=desc_text, bar_format=pbar_format):
        f1, f2 = pair_imgs[i * 2], pair_imgs[i * 2 + 1]
        success = False
        last_err = None
        for attempt in range(3):
            try:
                status, new_idx, new_t = process_one_pair(f1, f2, s_idx, cur_t, slots, W_M, H_M, m_bgr, m_a, is_hd, font_big, font_date, font_small)
                if status == 'ok':
                    s_idx, cur_t = new_idx, new_t
                    processed_count += 1
                    success = True
                    break
                else:
                    last_err = "padhi nahi gayi"
                    break
            except Exception as e:
                last_err = f"{type(e).__name__}: {str(e)[:50]}"
                for p in (os.path.join(O, f"{s_idx}.jpg"), os.path.join(R, f"{s_idx + 1}.jpg"), os.path.join(R, f"{s_idx + 2}.jpg")):
                    if os.path.exists(p):
                        try: os.remove(p)
                        except: pass
                gc.collect()
                time.sleep(0.3)

        if not success:
            corrupt_count += 1
            skip_log.append(f"{f1} + {f2} ({last_err})")

        if (i + 1) % 5 == 0: gc.collect()

    set_wakelock(False)

    if processed_count > 0:
        try:
            with open(T_F, "w") as fh: fh.write(str(cur_t))
        except: pass
        print(f"\n {C4}\u2714 {processed_count} Joray (pairs) aesthetic style mein ready!{ENDC}")
    else:
        print(f"\n {C5}\u2714 Sab already set hai.{ENDC}")

    if leftover:
        print(f" {C3}\u279c 1 wallpaper akela reh gaya - agli baar pair poora hote hi process hoga.{ENDC}")

    if corrupt_count > 0:
        print(f" {C2}\u26a0 {corrupt_count} pair(s) skip kiye gaye (dobara retry honge agle run me):{ENDC}")
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

    img_exts = ('.jpg', '.jpeg', '.png', '.webp', '.heic', '.avif')
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
