import os, sys, time, shutil, subprocess, random
from datetime import datetime
import cv2, numpy as np
from PIL import Image, ImageDraw, ImageFont
import PIL.ImageCms as cms
from tqdm import tqdm

# --- MULTI-COLORS ---
C1='\033[95m';C2='\033[91m';C3='\033[93m';C4='\033[92m';C5='\033[96m';C6='\033[94m';ENDC='\033[0m';BOLD='\033[1m'
QUOTES = [
    "Create your own sunshine.","Small steps every day.","Keep going. Keep growing.",
    "Aesthetic is not just a look, it's a vibe.","Coding with a touch of elegance.",
    "Transform reality with code."
]

def display_header():
    os.system('clear')
    quote = random.choice(QUOTES)
    print(f" {C1}✿{C2} ❀{C3} ❁{C5} ════════════════════════════════════{C3} ❁{C2} ❀{C1} ✿ {ENDC}")
    print(f"{C1}{BOLD}    █████╗ ██████╗ ███╗   ██╗█████╗ ███╗   ██╗{ENDC}")
    print(f"{C2}{BOLD}   ██╔══██╗██╔══██╗████╗  ██║██╔══██╗████╗  ██║{ENDC}")
    print(f"{C3}{BOLD}   ███████║██║  ██║██╔██╗ ██║███████║██╔██╗ ██║{ENDC}")
    print(f"{C4}{BOLD}   ██╔══██║██║  ██║██║╚██╗██║██╔══██║██║╚██╗██║{ENDC}")
    print(f"{C5}{BOLD}   ██║  ██║██████╔╝██║ ╚████║██║  ██║██║ ╚████║{ENDC}")
    print(f"{C6}{BOLD}   ╚═╝  ╚═╝╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═╝╚═╝  ╚═══╝{ENDC}")
    print(f" {C5}          W e l c o m e  t o  t h e  S y s t e m{ENDC}")
    print(f" {C6}╔══════════════════════════════════════════════╗ {ENDC}")
    print(f" {C6}║{ENDC} {C3}✦ {quote:<38}{C6}║{ENDC}")
    print(f" {C6}║{ENDC} {C3}✦ Owner   : {ENDC}{C5}Mehar Adnan                     {C6}║{ENDC}")
    print(f" {C6}║{ENDC} {C3}✦ Support : {ENDC}{C5}+92 343 6873 471               {C6}║{ENDC}")
    print(f" {C6}╚══════════════════════════════════════════════╝ {ENDC}\n")

def animated_spinner(text, duration=0.6):
    frames = ['•', 'o', 'O', 'o']
    for i in range(int(duration * 10)):
        sys.stdout.write(f"\r {C1}✿{ENDC} {C5}{frames[i % len(frames)]}{ENDC} {text}...")
        sys.stdout.flush()
        time.sleep(0)
    sys.stdout.write(f"\r {C4}✔{ENDC} {text} Completed!       \n")

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

def safe_scan(path):
    try:
        subprocess.run(["termux-media-scan", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=5)
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

cv2.setNumThreads(0)
P_NUM = sys.argv[2] if len(sys.argv) > 2 else "2"
B = os.environ.get("MOCKUP_BASE", os.path.dirname(os.path.abspath(__file__)))
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")
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
FONT_DIR = os.path.join(B, "fonts")
ALT_FONT_DIR = "/storage/emulated/0/fonts"

USE_24H = False


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

# --- EXACT CUSTOM FONT LOADER (SF PRO SOFT & STENCIL) ---
def get_font_by_names(font_filenames, size):
    for fname in font_filenames:
        for folder in [FONT_DIR, ALT_FONT_DIR]:
            path = os.path.join(folder, fname)
            if os.path.exists(path):
                try:
                    return ImageFont.truetype(path, size)
                except:
                    pass
    fallback_sys = "/system/fonts/Roboto-Bold.ttf"
    if os.path.exists(fallback_sys):
        try:
            return ImageFont.truetype(fallback_sys, size)
        except:
            pass
    return ImageFont.load_default()

def get_font(bold_size=None, semibold_size=None):
    if bold_size is not None:
        return get_font_by_names(["SF Pro - Semibold Soft.otf", "SF-Pro-Display-Bold.otf", "SF Pro - Bold.otf"], bold_size)
    if semibold_size is not None:
        return get_font_by_names(["SF Pro - Semibold Soft.otf", "SF-Pro-Display-Semibold.otf", "SF Pro - Semibold.otf"], semibold_size)

def get_max_seq(dirs):
    m = 0
    for d in dirs:
        if os.path.exists(d):
            for r, sd, fs in os.walk(d):
                for f in fs:
                    n, _ = os.path.splitext(f)
                    if n.isdigit():
                        m = max(m, int(n))
    return m

def resize_exact(img, target_w=1080, target_h=1920):
    h, w = img.shape[:2]
    r = w / h
    target_ratio = target_w / target_h
    if r > target_ratio:
        nw = int(h * target_ratio)
        img = img[:, (w - nw) // 2 : (w - nw) // 2 + nw]
    else:
        nh = int(w / target_ratio)
        img = img[(h - nh) // 2 : (h - nh) // 2 + nh, :]
    return cv2.resize(img, (target_w, target_h), interpolation=cv2.INTER_LANCZOS4)

def apply_hd_enhance(img):
    smoothed = cv2.bilateralFilter(img, d=5, sigmaColor=50, sigmaSpace=50)
    lab = cv2.cvtColor(smoothed, cv2.COLOR_BGR2LAB)
    l, a, b = cv2.split(lab)
    blur1 = cv2.GaussianBlur(l, (0, 0), 1.0)
    sharp1 = cv2.addWeighted(l, 1.3, blur1, -0.3, 0)
    blur2 = cv2.GaussianBlur(sharp1, (0, 0), 2.5)
    sharp2 = cv2.addWeighted(sharp1, 1.15, blur2, -0.15, 0)
    l_final = np.clip(sharp2, 0, 255).astype(np.uint8)
    return cv2.cvtColor(cv2.merge([l_final, a, b]), cv2.COLOR_LAB2BGR)

def save_optimized(cv2_img, output_path):
    rgb = cv2.cvtColor(cv2_img, cv2.COLOR_BGR2RGB)
    pil_img = Image.fromarray(rgb)
    try:
        srgb_profile = cms.createProfile('sRGB')
        srgb_bytes = cms.ImageCmsProfile(srgb_profile).tobytes()
        pil_img.save(output_path, format='JPEG', quality=90, subsampling=0, icc_profile=srgb_bytes, optimize=True)
    except:
        pil_img.save(output_path, format='JPEG', quality=90, subsampling=0, optimize=True)

# ============== CANVAS ==============
CW, CH = 1080, 1919

def cover_fit(img, box_w, box_h):
    h, w = img.shape[:2]
    scale = max(box_w / w, box_h / h)
    nw, nh = int(round(w * scale)), int(round(h * scale))
    resized = cv2.resize(img, (nw, nh), interpolation=cv2.INTER_LANCZOS4)
    x0 = (nw - box_w) // 2
    y0 = (nh - box_h) // 2
    return resized[y0 : y0 + box_h, x0 : x0 + box_w]

def rounded_mask(w, h, radius, ss=4):
    w, h = max(int(w), 1), max(int(h), 1)
    W, Hh, Rr = w * ss, h * ss, int(radius * ss)
    m = Image.new("L", (W, Hh), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, W - 1, Hh - 1], radius=Rr, fill=255)
    return np.array(m.resize((w, h), Image.LANCZOS))

def avg_brightness(bgr_crop):
    if bgr_crop.size == 0:
        return 128
    return cv2.cvtColor(bgr_crop, cv2.COLOR_BGR2GRAY).mean()

def frost_patch(canvas_bgr, rx, ry, rw, rh, blur_src, radius_frac=0.28, alpha_white=0.28, alpha_black=0.18):
    H, W = canvas_bgr.shape[:2]
    rx2, ry2 = min(rx + rw, W), min(ry + rh, H)
    rx, ry = max(rx, 0), max(ry, 0)
    if rx2 <= rx or ry2 <= ry:
        return
    crop = blur_src[ry:ry2, rx:rx2]
    orig = canvas_bgr[ry:ry2, rx:rx2].copy()
    if crop.size == 0:
        return
    bright = avg_brightness(crop)
    radius = int(radius_frac * min(rx2 - rx, ry2 - ry))
    m = rounded_mask(rx2 - rx, ry2 - ry, radius).astype(np.float32) / 255.0
    m3 = np.dstack([m] * 3)
    if bright < 128:
        overlay = np.full_like(crop, 255, dtype=np.uint8)
        a = alpha_white
    else:
        overlay = np.full_like(crop, 0, dtype=np.uint8)
        a = alpha_black
    blended = crop.astype(np.float32) * (1 - a) + overlay.astype(np.float32) * a
    patch = orig.astype(np.float32) * (1 - m3) + blended * m3
    canvas_bgr[ry:ry2, rx:rx2] = patch.astype(np.uint8)

def draw_lock_panel(wall_bgr, lw, lh):
    lock_crop = cover_fit(wall_bgr, lw, lh)
    top_strip = lock_crop[0 : int(lh * 0.22), :]
    dark_bg = avg_brightness(top_strip) < 128
    text_color = (255, 255, 255) if dark_bg else (28, 28, 30)
    scrim_color = (0, 0, 0) if dark_bg else (255, 255, 255)

    pil = Image.fromarray(cv2.cvtColor(lock_crop, cv2.COLOR_BGR2RGB)).convert("RGBA")
    grad_h = max(int(lh * 0.28), 1)
    grad = Image.new("L", (1, grad_h), 0)
    for yy in range(grad_h):
        grad.putpixel((0, yy), int(55 * (1 - yy / grad_h)))
    grad = grad.resize((lw, grad_h))
    scrim = Image.new("RGBA", (lw, grad_h), scrim_color + (0,))
    scrim.putalpha(grad)
    pil.alpha_composite(scrim, (0, 0))

    draw = ImageDraw.Draw(pil)
    now = datetime.now()
    time_str = now.strftime("%H:%M") if USE_24H else now.strftime("%I:%M").lstrip("0")

    # Reference-style date and time sizing/placement.
    date_str = f"{now.strftime('%A')} {now.day}' {now.strftime('%B')}"

    f_time = get_font_by_names(
        ["SF Pro - Semibold Stencil.otf", "SF-Pro-Semibold-Stencil.otf"],
        int(lw * 0.258)
    )
    f_date = get_font_by_names(
        ["SF Pro - Semibold Soft.otf", "SF-Pro-Semibold-Soft.otf", "SF Pro - Semibold Soft"],
        int(lw * 0.048)
    )

    date_y = int(lh * 0.104)
    bbox = draw.textbbox((0, 0), date_str, font=f_date)
    draw.text(
        ((lw - (bbox[2] - bbox[0])) // 2, date_y),
        date_str,
        font=f_date,
        fill=text_color + (255,)
    )

    TIME_BOLD_STROKE = 2  # matches previous dilate=2 boldness, but crisp/clean
    time_y = int(lh * 0.148)

    bbox = draw.textbbox((0, 0), time_str, font=f_time, stroke_width=TIME_BOLD_STROKE)
    tw = bbox[2] - bbox[0]
    gx = (lw - tw) // 2 - bbox[0]
    draw.text(
        (gx, time_y),
        time_str,
        font=f_time,
        fill=text_color + (255,),
        stroke_width=TIME_BOLD_STROKE,
        stroke_fill=text_color + (255,)
    )

    panel = cv2.cvtColor(np.array(pil.convert("RGB")), cv2.COLOR_RGB2BGR)
    mask = rounded_mask(lw, lh, int(lw * 0.085)).astype(np.float32) / 255.0
    return (panel.astype(np.float32) * np.dstack([mask] * 3)).astype(np.uint8)

def draw_home_panel(wall_bgr, hw, hh):
    home_crop = cover_fit(wall_bgr, hw, hh)
    canvas = cv2.GaussianBlur(home_crop, (0, 0), sigmaX=26, sigmaY=26)
    blur_src = cv2.GaussianBlur(home_crop, (0, 0), sigmaX=10, sigmaY=10)

    margin      = 0.073 * hw
    small_size  = 0.159 * hw
    small_gap   = 0.084 * hw
    big_size    = 0.415 * hw
    block_gap   = 0.055 * hw
    pair_gap    = 0.084 * hw
    top_pad     = 0.072 * hh

    def draw_smallgrid(bx, by):
        for r in range(2):
            for c in range(2):
                ix = int(bx + c * (small_size + small_gap))
                iy = int(by + r * (small_size + small_gap))
                frost_patch(canvas, ix, iy, int(small_size), int(small_size), blur_src, radius_frac=0.30, alpha_white=0.28, alpha_black=0.18)

    def draw_big(bx, by):
        frost_patch(canvas, int(bx), int(by), int(big_size), int(big_size), blur_src, radius_frac=0.19, alpha_white=0.28, alpha_black=0.18)

    y = top_pad
    n_pairs = 3
    for p in range(n_pairs):
        left_x = margin
        right_x = margin + (small_size * 2 + small_gap) + block_gap
        if p % 2 == 0:
            draw_smallgrid(left_x, y)
            draw_big(right_x, y)
        else:
            draw_big(left_x, y)
            draw_smallgrid(right_x, y)
        y += big_size + pair_gap

    # --- EXACT IMAGE 2 BOTTOM DOCK BOXES (HIGH VISIBILITY) ---
    dock_h = int(0.128 * hh)
    dock_top = int(0.845 * hh)
    dock_side_pad = int(0.040 * hw)
    dock_w = int(hw - 2 * dock_side_pad)
    
    # Dock Background Layer
    frost_patch(canvas, dock_side_pad, dock_top, dock_w, dock_h, blur_src,
                radius_frac=0.34, alpha_white=0.18, alpha_black=0.12)

    # 4 Inner Dock Icons with Crisp Contrast Layer
    dock_icon_sz = int(0.152 * hw)
    dock_icon_y = int(dock_top + (dock_h - dock_icon_sz) / 2)
    spacing = int((dock_w - (4 * dock_icon_sz)) / 5)
    
    for c in range(4):
        ix = dock_side_pad + spacing + c * (dock_icon_sz + spacing)
        frost_patch(canvas, ix, dock_icon_y, dock_icon_sz, dock_icon_sz, blur_src,
                    radius_frac=0.30, alpha_white=0.35, alpha_black=0.25)

    mask = rounded_mask(hw, hh, int(hw * 0.085)).astype(np.float32) / 255.0
    return (canvas.astype(np.float32) * np.dstack([mask] * 3)).astype(np.uint8)

def build_mockup(wall_bgr):
    canvas = np.zeros((CH, CW, 3), dtype=np.uint8)
    lx, ly, lw, lh = 4, 395, 524, 1131
    hx, hy, hw, hh = 552, 395, 523, 1130

    pil = Image.fromarray(canvas).convert("RGB")
    draw = ImageDraw.Draw(pil)
    
    # --- EXACT LARGE & CLEAR SIZES USING SF PRO SEMIBOLD SOFT ---
    f_title = get_font_by_names(["SF Pro - Semibold Soft.otf", "SF-Pro-Semibold-Soft.otf", "SF Pro - Semibold.otf"], 46)
    f_cap = get_font_by_names(["SF Pro - Semibold Soft.otf", "SF-Pro-Semibold-Soft.otf", "SF Pro - Semibold.otf"], 36)
    
    title = "Customise Wallpaper"
    bbox = draw.textbbox((0, 0), title, font=f_title)
    draw.text(((CW - (bbox[2] - bbox[0])) // 2, 255), title, font=f_title, fill=(255, 255, 255))
    
    for txt, px, pw in [("Lock Screen", lx, lw), ("Home Screen", hx, hw)]:
        bbox = draw.textbbox((0, 0), txt, font=f_cap)
        cx = px + (pw - (bbox[2] - bbox[0])) // 2
        draw.text((cx, 1565), txt, font=f_cap, fill=(255, 255, 255))
        
    canvas = cv2.cvtColor(np.array(pil), cv2.COLOR_RGB2BGR)

    canvas[ly : ly + lh, lx : lx + lw] = draw_lock_panel(wall_bgr, lw, lh)
    canvas[hy : hy + hh, hx : hx + hw] = draw_home_panel(wall_bgr, hw, hh)
    return canvas

# ============================ CLI ============================
if len(sys.argv) < 2:
    sys.exit()
action = sys.argv[1]

if action == 'process':
    display_header()
    os.makedirs(O, exist_ok=True)
    os.makedirs(R, exist_ok=True)
    imgs = [f for f in os.listdir(I) if f.lower().endswith(('.jpg','.jpeg','.png','.webp','.avif','.heic')) and not f.startswith('.') and "_temp" not in f]
    if not imgs:
        print(f" {C2}✖ Koi tasveer nahi mili.{ENDC}")
        sys.exit()

    print(f" {C5}✿ {BOLD}S e l e c t  M o d e{ENDC}")
    print(f" {C1}[1]{ENDC} HD Mode (AI Enhance) - {C4}[DEFAULT]{ENDC}")
    print(f" {C1}[2]{ENDC} Normal (Fast KB Pack)")
    choice = input(f"\n {C4}➜ {ENDC}").strip()
    is_hd = (choice != '2')

    print(f" {C5}✿ {BOLD}S e l e c t  S i z e{ENDC}")
    print(f" {C1}[1]{ENDC} Original Size - {C4}[DEFAULT]{ENDC}")
    print(f" {C1}[2]{ENDC} Crop 9:16")

    size_choice = input(f"\n {C4}➜ {ENDC}").strip()
    crop_916 = (size_choice == '2')

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
        # v155 behtari (squeeze khatam): window kam az kam
        # slots x 0.5s ki lazmi ho — warna floor ko PEECHE
        # (deeper past) le jao taake har file ko >= 0.5s ki
        # spacing mile. Future me kabhi nahi barhna:
        # end_soft hamesha abhi-60 hi rehta hai, or tracker
        # me aakhir me is run ka aakhri waqt hi likha jata
        # hai (purani convention) — agla run is block ke
        # baad se hi shuru hota hai.
        if _end_soft - _floor < _slots * 0.5:
            _floor = _end_soft - _slots * 0.5
        _raw_step = (_end_soft - _floor) / _slots
        t_step = min(_raw_step, 60.0)
        cur_t = _end_soft - t_step * _slots

    print("")
    pbar_format = f" {C1}{{desc}}{ENDC} {C5}|{ENDC}{{bar:20}}{C5}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"
    desc_text = "✨ HD Mode " if is_hd else "⚙️ Normal "

    processed_count = 0
    for f in tqdm(imgs, desc=desc_text, bar_format=pbar_format):
        try:
            raw_p = os.path.join(I, f)
            w = cv2.imread(raw_p)
            if w is None:
                continue

            w_exact = resize_exact(w) if crop_916 else w
            w_processed = apply_hd_enhance(w_exact) if is_hd else w_exact

            mock_path = os.path.join(O, f"{s_idx}.jpg")
            wall_path = os.path.join(R, f"{s_idx+1}.jpg")
            mt, wt = cur_t + t_step, cur_t + 2*t_step

            final = build_mockup(w_processed)
            save_optimized(final, mock_path)
            os.utime(mock_path, (mt, mt))
            safe_scan(mock_path)

            save_optimized(w_processed, wall_path)
            os.utime(wall_path, (wt, wt))
            safe_scan(wall_path)
            gallery_copy_outputs([(mock_path, mt), (wall_path, wt)])

            os.remove(raw_p)
            s_idx += 2
            cur_t = wt
            processed_count += 1
        except Exception as e:
            print(f" {C2}✖ {f}: {e}{ENDC}")

    if processed_count > 0:
        try:
            if _tmode != "current":
                with open(T_F, "w") as f:
                    f.write(str(cur_t))
        except:
            pass
        print(f"\n {C4}✔ {processed_count} Joray aesthetic style mein ready!{ENDC}\n")
    else:
        print(f"\n {C5}✔ Sab already set hai.{ENDC}\n")

elif action == 'batch':
    display_header()
    os.makedirs(W_H, exist_ok=True)
    mks = [f for f in os.listdir(O) if os.path.splitext(f)[0].isdigit()] if os.path.exists(O) else []
    if mks:
        bn = len([d for d in os.listdir(W_H) if os.path.isdir(os.path.join(W_H, d))]) + 1
        cb = os.path.join(W_H, _batch_pick_name(f"P{P_NUM}_Batch_{bn}"))
        os.makedirs(cb, exist_ok=True)
        for f in os.listdir(O):
            if os.path.splitext(f)[0].isdigit():
                src = os.path.join(O, f)
                shutil.move(src, cb)
                safe_scan(os.path.join(cb, f))
        for f in os.listdir(R):
            if os.path.splitext(f)[0].isdigit():
                src = os.path.join(R, f)
                shutil.move(src, cb)
                safe_scan(os.path.join(cb, f))
        animated_spinner("Packing Batch", 0.6)
        print(f" {C4}📦 {os.path.basename(cb)} Warehouse mein chala gaya!{ENDC}\n")
    else:
        print(f"\n {C2}✖ Batch banane ke liye data nahi hai.{ENDC}\n")

elif action == 'del':
    display_header()
    print(f" {C1}✿ ❀ ❁ ═════════════════════════════════ ❁ ❀ ✿{ENDC}")
    print(f"              {BOLD}{C2}🗑️  D E L E T E  M E N U{ENDC}   ")
    print(f" {C1}✿ ❀ ❁ ═════════════════════════════════ ❁ ❀ ✿{ENDC}")
    print(f" {C5}[1]{ENDC} Input (Images Only)")
    print(f" {C5}[2]{ENDC} Tayaar Maal (Images Only)")
    print(f" {C5}[3]{ENDC} Warehouse (Ready_Upload_Batches)")
    print(f" {C5}[0]{ENDC} Cancel")
    choice = input(f"\n {C4}➜ {ENDC}")
    img_exts = ('.jpg', '.jpeg', '.png', '.webp', '.heic', '.avif')

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
                try: os.remove(os.path.join(dp, f))
                except: pass
            safe_scan(dp)
        except Exception:
            pass

    def perm_del_images(folder):
        c = 0
        if os.path.exists(folder):
            for f in os.listdir(folder):
                if not f.startswith('.') and f.lower().endswith(img_exts):
                    try:
                        p = os.path.join(folder, f)
                        os.remove(p)
                        safe_scan(p)
                        c += 1
                    except:
                        pass
        return c

    if choice == '1':
        print(f"\n {C4}✔ {perm_del_images(I)} Input Images Deleted.{ENDC}")
    elif choice == '2':
        print(f"\n {C4}✔ {perm_del_images(O) + perm_del_images(R)} Tayaar Images Deleted.{ENDC}")
        tm_pairs_cleanup('all')
    elif choice == '3':
        if not os.path.exists(W_H):
            print(f"\n {C2}✖ Warehouse khali hai!{ENDC}")
            sys.exit()
        batches = sorted([d for d in ir(W_H) if "Batch_" in d])
        for i, b in enumerate(batches):
            print(f" {C5}[{i+1}]{ENDC} Delete {b}")
        sub_choice = input(f"\n {C4}Choose Batch or 'A' (All): {ENDC}")
        if sub_choice.lower() == 'a':
            shutil.rmtree(W_H)
            os.makedirs(W_H)
            print(f"\n {C4}✔ Warehouse saaf ho gaya.{ENDC}")
        elif sub_choice.isdigit() and 1 <= int(sub_choice) <= len(batches):
            shutil.rmtree(os.path.join(W_H, batches[int(sub_choice) - 1]))
            print(f"\n {C4}✔ Deleted.{ENDC}")
