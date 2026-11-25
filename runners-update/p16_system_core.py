import os, sys, time, shutil, subprocess, random, json, math, datetime
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import PIL.ImageCms as cms
from tqdm import tqdm
import cv2, numpy as np

C1 = '\033[95m'
C2 = '\033[91m'
C3 = '\033[93m'
C4 = '\033[92m'
C5 = '\033[96m'
C6 = '\033[94m'
ENDC = '\033[0m'
BOLD = '\033[1m'

QUOTES = [
    "Create your own sunshine.",
    "Small steps every day.",
    "Keep going. Keep growing.",
    "Aesthetic is not just a look, it's a vibe.",
    "Coding with a touch of elegance.",
    "Transform reality with code."
]

def display_header():
    os.system('clear')
    quote = random.choice(QUOTES)
    
    print(f" {C1}✿{C2} ❀{C3} ❁{C5} ════════════════════════════════════════{C3} ❁{C2} ❀{C1} ✿ {ENDC}")
    print(f"{C1}{BOLD}  ██████╗  █████╗ ████████╗███████╗██████╗  █████╗ ███████╗███████╗{ENDC}")
    print(f"{C2}{BOLD}  ██╔══██╗██╔══██╗╚══██╔══╝██╔════╝██╔══██╗██╔══██╗██╔════╝██╔════╝{ENDC}")
    print(f"{C3}{BOLD}  ██║  ██║███████║   ██║   █████╗  ██████╔╝███████║███████╗█████╗  {ENDC}")
    print(f"{C4}{BOLD}  ██║  ██║██╔══██║   ██║   ██╔══╝  ██╔══██╗██╔══██║╚════██║██╔══╝  {ENDC}")
    print(f"{C5}{BOLD}  ██████╔╝██║  ██║   ██║   ███████╗██████╔╝██║  ██║███████║███████╗{ENDC}")
    print(f"{C6}{BOLD}  ╚═════╝ ╚═╝  ╚═╝   ╚═╝   ╚══════╝╚═════╝ ╚═╝  ╚═╝╚══════╝╚══════╝{ENDC}")
    
    print(f" {C5}          W e l c o m e  t o  t h e  S y s t e m{ENDC}")
    
    print(f" {C6}╔═════════════════════════════════════════════════════╗ {ENDC}")
    print(f" {C6}║{ENDC} {C3}✦ {quote:<43}{C6}║{ENDC}")
    print(f" {C6}║{ENDC} {C3}✦ Owner   : {ENDC}{C5}DATABASE OFFICIAL & Digital Services  {C6}║{ENDC}")
    print(f" {C6}║{ENDC} {C3}✦ Support : {ENDC}{C5}+44 7404 032136                       {C6}║{ENDC}")
    print(f" {C6}╚═════════════════════════════════════════════════════╝ {ENDC}\n")

def animated_spinner(text, duration=0.8):
    frames = ['•', 'o', 'O', 'o']
    for i in range(int(duration * 10)):
        sys.stdout.write(f"\r {C1}✿{ENDC} {C5}{frames[i % len(frames)]}{ENDC} {text}...")
        sys.stdout.flush()
        time.sleep(0)
    sys.stdout.write(f"\r {C4}✔{ENDC} {text} Completed!       \n")

def safe_scan(path):
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

cv2.setNumThreads(0)
P_NUM = sys.argv[2] if len(sys.argv) > 2 else "16"
B = os.environ.get("MOCKUP_BASE") or os.path.dirname(os.path.abspath(__file__))
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")
T_F = "/storage/emulated/0/.mockup_time_tracker.txt"
M_P, S_P = os.path.join(B, "mockup.png"), os.path.join(B, "shape.png")


# =====================================================================
#  PAPER-CUT LOCKSCREEN  (reference: cream paper top + torn mountain edge + widgets)
#  Wallpaper (1179x2556) -> top ~44% cream paper-cut  ->  "Ready wallpaper"
#  Phir uspar time/date/widgets (wallpaper ke attractive rang me) -> flat phone mockup
# =====================================================================
SW, SH = 1179, 2556                    # iPhone native screen
CORNER_R = 150                         # screen corner radius (SW space)
CANVAS_W, CANVAS_H = 2160, 2700        # mockup size
PHONE_H = 2363                         # mockup me phone ki unchai
CREAM = (255, 241, 211)                # RGB — paper ka rang
PAPER_TEXTURE = 0.55                   # 0 = bilkul solid, 1 = reference jesa fibres/specks
CUT_MEAN, CUT_AMP = 0.435, 0.052       # cut ki ausat jagah (screen height ka hissa) aur uthaan
FIXED_TIME = None                      # "9:06" likho to har baar yehi time
FIXED_DATE = None                      # "Thursday, August 21" likho to har baar yehi date
CARRIER_TEXT = ""                      # status bar ke left me likhna ho to (default khali)
FONT_UI = os.path.join(B, "Inter.ttf")
BG_DIR = os.path.join(B, "backgrounds")
BG_LAST = os.path.join(B, ".bg_last.txt")

_fc = {}
def ifont(size, wght=650, opsz=None):
    k = (int(size), wght)
    if k not in _fc:
        f = ImageFont.truetype(FONT_UI, int(size))
        try: f.set_variation_by_axes([min(32, max(14, opsz if opsz else size / 4.0)), wght])
        except Exception: pass
        _fc[k] = f
    return _fc[k]

def fit_font(text, target_w=None, target_h=None, wght=650, glyph="0"):
    lo, hi = 10, 600
    for _ in range(14):
        mid = (lo + hi) // 2
        f = ifont(mid, wght)
        if target_w:
            v = f.getlength(text)
        else:
            l, t, r, b = f.getbbox(glyph, anchor="ls"); v = b - t
        if v < (target_w or target_h): lo = mid
        else: hi = mid
    return ifont(hi, wght)

def aa_mask(fn, w, h, ss=4):
    im = Image.new("L", (int(w * ss), int(h * ss)), 0)
    fn(ImageDraw.Draw(im), ss)
    return im.resize((int(w), int(h)), Image.LANCZOS)

def paste_m(canvas, m, cx, cy):
    canvas.paste(255, (int(round(cx - m.width / 2)), int(round(cy - m.height / 2))), m)

# ---------------- wallpaper se rang ----------------
def kmeans_clusters(bgr, K=6):
    px = cv2.resize(bgr, (64, 64), interpolation=cv2.INTER_AREA).reshape(-1, 3).astype(np.float32)
    crit = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 20, 1.0)
    _, lab, cen = cv2.kmeans(px, K, None, crit, 3, cv2.KMEANS_PP_CENTERS); lab = lab.ravel()
    out = []
    for k in range(K):
        b, g, r = cen[k] / 255.0; mx, mn = max(r, g, b), min(r, g, b)
        out.append(dict(bgr=cen[k], frac=float((lab == k).mean()), sat=0 if mx == 0 else (mx - mn) / mx, val=mx))
    return out

def hsv_set(bgr, dh=0, s=None, v=None):
    c = cv2.cvtColor(np.uint8([[np.clip(bgr, 0, 255)]]), cv2.COLOR_BGR2HSV)[0, 0].astype(np.float32)
    c[0] = (c[0] + dh / 2.0) % 180
    if s is not None: c[1] = np.clip(s, 0, 1) * 255
    if v is not None: c[2] = np.clip(v, 0, 1) * 255
    return tuple(int(x) for x in cv2.cvtColor(np.uint8([[c]]), cv2.COLOR_HSV2BGR)[0, 0])

def pick_colors(art_bgr):
    cl = kmeans_clusters(art_bgr)
    tone = lambda c: 1.0 if 0.14 < c["val"] < 0.97 else 0.35
    main = max(cl, key=lambda c: (c["frac"] ** 0.6) * (0.15 + c["sat"]) * tone(c))
    if main["sat"] < 0.18:                      # lagbhag grey wallpaper
        ui = hsv_set(main["bgr"], s=0.25, v=0.22)
    else:
        ui = hsv_set(main["bgr"], s=max(0.55, min(0.88, main["sat"] * 1.05)), v=0.37)
    cands = [c for c in cl if c["frac"] > 0.02] or cl
    def warm(c):
        h = cv2.cvtColor(np.uint8([[np.clip(c['bgr'], 0, 255)]]), cv2.COLOR_BGR2HSV)[0, 0][0] * 2
        return 1.6 if (h < 70 or h > 335) else 1.0
    hi = max(cands, key=lambda c: c["sat"] * c["val"] * warm(c))
    if warm(hi) > 1.0:                           # garam rang: reference jesa peela -> narangi
        e1 = hsv_set(hi["bgr"], s=0.95, v=1.0, dh=0); e2 = hsv_set(hi["bgr"], s=0.95, v=1.0, dh=0)
        def _h(bgr, hue):
            c = cv2.cvtColor(np.uint8([[np.clip(bgr, 0, 255)]]), cv2.COLOR_BGR2HSV)[0, 0].astype(np.float32); c[0] = hue / 2.0; c[1] = 242; c[2] = 255
            return tuple(int(x) for x in cv2.cvtColor(np.uint8([[c]]), cv2.COLOR_HSV2BGR)[0, 0])
        e1 = _h(hi["bgr"], 48); e2 = _h(hi["bgr"], 16)
    else:
        e1 = hsv_set(hi["bgr"], dh=6, s=max(0.7, hi["sat"]), v=0.99)
        e2 = hsv_set(hi["bgr"], dh=-26, s=max(0.75, hi["sat"]), v=0.97)
    return ui, e1, e2

# ---------------- cut + cream ----------------
def _octave(rng, step, W):
    n = int(W / step) + 3; v = rng.uniform(-1, 1, n)
    xs = np.arange(n) * step - step
    return lambda x: np.interp(x, xs, v)

def cut_points(rng, W, H):
    k = W / 1179.0
    o1, o2, o3 = _octave(rng, 230 * k, W), _octave(rng, 92 * k, W), _octave(rng, 34 * k, W)
    base, amp = CUT_MEAN * H, CUT_AMP * H
    trend = rng.uniform(-0.45, 0.45)
    xs = np.arange(0, W + 12, 12 * k, dtype=np.float32)
    ys = base + amp * (0.62 * o1(xs) + 0.30 * o2(xs) + 0.13 * o3(xs)) + trend * amp * (xs / W - 0.5) * 2
    ys += rng.uniform(-3.5, 3.5, len(xs)) * k                # chhote chhote daant
    return [(float(x), float(y)) for x, y in zip(xs, ys)]

def paper_texture(rng, W, H):
    k0 = W / 1179.0
    base = np.zeros((H, W, 3), np.float32); base[:] = np.array(CREAM, np.float32)
    if PAPER_TEXTURE <= 0: return base
    n = cv2.resize(rng.rand(H // 40 + 2, W // 40 + 2).astype(np.float32), (W, H), interpolation=cv2.INTER_CUBIC)
    base *= (1 - 0.035 * PAPER_TEXTURE + 0.07 * PAPER_TEXTURE * n)[..., None]
    lay = Image.new("RGBA", (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(lay)
    k = PAPER_TEXTURE; area = (W * H) / (1179.0 * 2556.0)
    for _ in range(int(360 * k * area)):
        x, y = rng.uniform(0, W), rng.uniform(0, H * 0.6); L = rng.uniform(16, 52) * k0; a = rng.uniform(0, math.pi)
        pts = [(x, y), (x + L * 0.5 * math.cos(a) + rng.uniform(-4, 4), y + L * 0.5 * math.sin(a)), (x + L * math.cos(a), y + L * math.sin(a))]
        d.line(pts, fill=(190, 140, 80, int(rng.uniform(40, 95))), width=max(1, int(rng.choice([2, 2, 3]) * k0)))
    for _ in range(int(900 * k * area)):
        x, y = rng.uniform(0, W), rng.uniform(0, H * 0.6); r = rng.choice([1.5, 2, 2.5, 3.5], p=[.4, .3, .2, .1]) * k0
        d.ellipse([x - r, y - r, x + r, y + r], fill=(176, 112, 52, int(rng.uniform(70, 170))))
    a = np.array(lay).astype(np.float32); al = a[..., 3:4] / 255.0
    return base * (1 - al) + a[..., :3] * al

def make_papercut(art_bgr, rng, e1, e2):
    """art_bgr (H,W,3) -> paper-cut wallpaper (BGR uint8), usi size me."""
    H, W = art_bgr.shape[:2]; k = W / 1179.0
    pts = cut_points(rng, W, H)
    poly = np.array([(-5, -5), (W + 5, -5)] + [(W + 5, pts[-1][1])] + pts[::-1] + [(-5, pts[0][1])], np.float32)
    mask = np.zeros((H, W), np.uint8)
    cv2.fillPoly(mask, [np.round(poly * 16).astype(np.int32)], 255, lineType=cv2.LINE_AA, shift=4)
    m = mask.astype(np.float32) / 255.0
    art = art_bgr.astype(np.float32)
    sh = cv2.GaussianBlur(cv2.warpAffine(m, np.float32([[1, 0, 0], [0, 1, 9 * k]]), (W, H)), (0, 0), 8 * k) * 0.30
    art = art * (1 - sh[..., None])
    cream = paper_texture(rng, W, H)[..., ::-1]
    out = art * (1 - m[..., None]) + cream * m[..., None]
    line = np.zeros((H, W), np.uint8)
    cv2.polylines(line, [np.round(np.array(pts) * 16).astype(np.int32)], False, 255, thickness=max(2, int(round(8 * k))), lineType=cv2.LINE_AA, shift=4)
    lm = cv2.GaussianBlur(line, (0, 0), 0.9 * k).astype(np.float32) / 255.0
    t = np.linspace(0, 1, W, dtype=np.float32)[None, :, None]
    grad = np.array(e1, np.float32) * (1 - t) + np.array(e2, np.float32) * t
    grad = np.broadcast_to(grad, (H, W, 3))
    out = out * (1 - lm[..., None]) + grad * lm[..., None]
    return np.clip(out, 0, 255).astype(np.uint8)

# ---------------- UI icons ----------------
def draw_ui(img_bgr, ui_bgr, time_str, date_str, temp):
    img = Image.fromarray(cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB))
    mask = Image.new("L", (SW, SH), 0); d = ImageDraw.Draw(mask)
    def T(text, f, cx=None, cy=None, left=None, right=None, anchor_v="m"):
        w = f.getlength(text)
        x = cx - w / 2 if cx is not None else (left if left is not None else right - w)
        d.text((x, cy), text, font=f, fill=255, anchor="lm" if anchor_v == "m" else "ls")
    s = SW / 1050.0
    # status bar (right): signal, 5G, battery
    sy = SH * 0.0804
    def bars(dd, ss):
        for i, hgt in enumerate([13, 19, 26, 32]):
            x0 = (i * 14) * s * ss
            dd.rounded_rectangle([x0, (32 - hgt) * s * ss, x0 + 9.5 * s * ss, 32 * s * ss], radius=4.5 * s * ss, fill=255)
    bm = aa_mask(bars, 56 * s / 1.123 + 4, 32 * s + 2); paste_m(mask, bm, SW * 0.7400 - bm.width / 2, sy)
    T("5G" if True else "", ifont(41 * s, 700), right=SW * 0.8114, cy=sy)
    bw, bh = 58 * s, 28 * s
    def batt(dd, ss):
        dd.rounded_rectangle([0, 0, bw * ss, bh * ss], radius=9 * s * ss, fill=255)
        dd.rounded_rectangle([3.2 * s * ss, 3.2 * s * ss, (bw - 3.2 * s) * ss, (bh - 3.2 * s) * ss], radius=6.5 * s * ss, fill=0)
        dd.rounded_rectangle([5.8 * s * ss, 5.8 * s * ss, (bw - 5.8 * s) * ss, (bh - 5.8 * s) * ss], radius=4.5 * s * ss, fill=255)
    batm = aa_mask(batt, bw + 2, bh + 2); paste_m(mask, batm, SW * 0.8867 - batm.width / 2, sy)
    if CARRIER_TEXT: T(CARRIER_TEXT, ifont(40 * s, 700), left=SW * 0.1238, cy=sy)
    # date + time
    fdate = fit_font(date_str, target_w=622 * (len(date_str) / 19.0) ** 0.0 if date_str == "Thursday, August 21" else None, wght=640) if False else ifont(52 * s * 1.0, 640)
    wdate = fdate.getlength(date_str)
    # reference ke "Thursday, August 21" ki chorai (622px) ko hadd maan kar size
    ref_w = ifont(52 * s, 640).getlength("Thursday, August 21"); scl = (555 * s) / ref_w
    fdate = ifont(52 * s * scl, 640)
    T(date_str, fdate, cx=SW * 0.502, cy=SH * 0.1351)
    ftime = fit_font("0", target_h=203 * s, wght=720)
    tb = ftime.getbbox(time_str, anchor="ls"); wt = tb[2] - tb[0]
    gb = ftime.getbbox("0", anchor="ls"); cap_mid = (gb[1] + gb[3]) / 2
    d.text((SW * 0.4981 - wt / 2 - tb[0], SH * 0.2098 - cap_mid), time_str, font=ftime, fill=255, anchor="ls")
    # weather widget
    lx = SW * 0.1238
    def cloud(dd, ss):
        W_, H_ = 66 * s, 40 * s
        dd.rounded_rectangle([0, 17 * s * ss, W_ * ss, 40 * s * ss], radius=11.5 * s * ss, fill=255)
        dd.ellipse([8 * s * ss, 7 * s * ss, 34 * s * ss, 33 * s * ss], fill=255)
        dd.ellipse([22 * s * ss, 0, 50 * s * ss, 28 * s * ss], fill=255)
    cm = aa_mask(cloud, 68 * s, 42 * s); paste_m(mask, cm, lx + cm.width / 2, SH * 0.3022)
    ft = fit_font("0", target_h=47 * 1.0 * (SW / 1179.0) * 1.0 * 1.0 + 0, wght=700) if False else ifont(62 * s / 1.123, 700)
    d.text((SW * 0.2002, SH * 0.3022), f"{temp}°", font=ifont(62 * s / 1.123 * 1.0, 700), fill=255, anchor="lm")
    d.text((lx, SH * 0.3298), "Cloudy", font=ifont(56 * s / 1.123, 640), fill=255, anchor="lm")
    d.text((lx, SH * 0.3538), f"H:{temp + 5}° L:{temp - 3}°", font=ifont(56 * s / 1.123, 640), fill=255, anchor="lm")
    # spotify
    cx, cy, r = SW * 0.6305, SH * 0.3253, 93 * s / 1.123 * 1.0
    def spot(dd, ss):
        R_ = r * ss; c = (r + 2) * ss
        dd.ellipse([c - R_, c - R_, c + R_, c + R_], fill=255)
        for k, (dy, half, wd) in enumerate([(-0.43, 0.74, 0.165), (-0.09, 0.64, 0.150), (0.22, 0.50, 0.135)]):
            pts = []
            for u in np.linspace(-1, 1, 30):
                x = c + u * half * R_; y = c + dy * R_ - (1 - u * u) * 0.17 * R_ + 0 * u
                pts.append((x, y))
            dd.line(pts, fill=0, width=int(wd * R_), joint="curve")
            for (x, y) in (pts[0], pts[-1]): dd.ellipse([x - wd * R_ / 2, y - wd * R_ / 2, x + wd * R_ / 2, y + wd * R_ / 2], fill=0)
    sm = aa_mask(spot, 2 * r + 4, 2 * r + 4); paste_m(mask, sm, cx, cy)
    # gauge
    gx, gy, gr = SW * 0.8457, SH * 0.3240, 71 * s
    def gauge(dd, ss):
        c = (gr + 10) * ss; st = int(12 * s * ss)
        dd.arc([c - gr * ss, c - gr * ss, c + gr * ss, c + gr * ss], 150, 390, fill=255, width=st)
        for ang in (150, 30):
            x = c + math.cos(math.radians(ang)) * (gr * ss - st / 2); y = c + math.sin(math.radians(ang)) * (gr * ss - st / 2)
            dd.ellipse([x - st / 2, y - st / 2, x + st / 2, y + st / 2], fill=255)
    gm = aa_mask(gauge, 2 * gr + 20, 2 * gr + 20); paste_m(mask, gm, gx, gy + 6 * s)
    d.text((gx, gy + 4 * s), str(temp), font=ifont(60 * s / 1.123, 640), fill=255, anchor="mm")
    d.text((gx, SH * 0.3543), f"{temp - 3} {temp + 5}", font=ifont(38 * s / 1.123, 500), fill=255, anchor="mm")
    col = Image.new("RGB", (SW, SH), (ui_bgr[2], ui_bgr[1], ui_bgr[0]))
    img.paste(col, (0, 0), mask)
    return cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)

# ---------------- mockup (flat phone on paper background) ----------------
def default_paper_bg(W, H, rng):
    base = np.array([230, 224, 214], np.float32)[None, None, :] * np.ones((H, W, 1), np.float32)
    n = cv2.resize(rng.rand(H // 90 + 2, W // 90 + 2).astype(np.float32), (W, H), interpolation=cv2.INTER_CUBIC)
    base *= (0.988 + 0.024 * n)[..., None]
    lay = Image.new("RGBA", (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(lay)
    for _ in range(2600):
        x, y = rng.uniform(0, W), rng.uniform(0, H); L = rng.uniform(14, 46); a = rng.uniform(0, math.pi)
        d.line([(x, y), (x + L * math.cos(a), y + L * math.sin(a))], fill=(170, 160, 146, int(rng.uniform(30, 70))), width=2)
    for _ in range(2200):
        x, y = rng.uniform(0, W), rng.uniform(0, H); r = rng.choice([1.5, 2.5, 3.5], p=[.6, .3, .1])
        d.ellipse([x - r, y - r, x + r, y + r], fill=(120, 116, 108, int(rng.uniform(60, 150))))
    a = np.array(lay).astype(np.float32); al = a[..., 3:4] / 255.0
    return (base * (1 - al) + a[..., :3] * al)[..., ::-1]

BG_MODE = "dynamic"      # "dynamic" = wallpaper ke best rangon ka gradient | "folder" = backgrounds/ folder ki images

def _hue(bgr):
    return float(cv2.cvtColor(np.uint8([[np.clip(bgr, 0, 255)]]), cv2.COLOR_BGR2HSV)[0, 0][0]) * 2.0

def gradient_bg(art_bgr, W, H):
    """Wallpaper ke 2 behtareen (prominent + vivid) rangon se nafees gradient."""
    cl = kmeans_clusters(art_bgr, 7)
    score = lambda c: (c["frac"] ** 0.5) * (0.15 + c["sat"]) * (1.0 if 0.14 < c["val"] < 0.97 else 0.35)
    a = max(cl, key=score)
    if a["sat"] < 0.15:                                           # lagbhag grey wallpaper
        cA, cB = (232, 230, 226), (176, 172, 166)
    else:
        far = [c for c in cl if c is not a and c["sat"] > 0.25 and min(abs(_hue(c["bgr"]) - _hue(a["bgr"])), 360 - abs(_hue(c["bgr"]) - _hue(a["bgr"]))) >= 30]
        b = max(far, key=score) if far else None
        cA = hsv_set(a["bgr"], s=min(0.85, max(0.40, a["sat"])), v=0.90)
        cB = hsv_set(b["bgr"], s=min(0.85, max(0.40, b["sat"])), v=0.58) if b else hsv_set(a["bgr"], dh=34, s=min(0.85, max(0.40, a["sat"])), v=0.58)
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    t = np.clip(0.70 * (y / H) + 0.30 * (x / W), 0, 1)[..., None]
    img = np.array(cA, np.float32) * (1 - t) + np.array(cB, np.float32) * t
    glow = np.clip(1 - np.sqrt(((x - W / 2) / (W * 0.7)) ** 2 + ((y - H * 0.45) / (H * 0.55)) ** 2), 0, 1) ** 1.4
    img = img + glow[..., None] * 34
    d = np.sqrt(((x - W / 2) / (W * 0.8)) ** 2 + ((y - H / 2) / (H * 0.8)) ** 2)
    img = img * (1 - 0.06 * np.clip(d, 0, 1.2) ** 2)[..., None]
    return np.clip(img, 0, 255).astype(np.float32)

def pick_bg(W, H, rng):
    files = sorted(f for f in os.listdir(BG_DIR) if f.lower().endswith(('.jpg', '.jpeg', '.png', '.webp')) and not f.startswith('.')) if os.path.isdir(BG_DIR) else []
    if not files: return default_paper_bg(W, H, rng)
    last = ""
    try: last = open(BG_LAST).read().strip()
    except Exception: pass
    pool = [f for f in files if f != last] or files; f = random.choice(pool)
    try: open(BG_LAST, "w").write(f)
    except Exception: pass
    img = cv2.imread(os.path.join(BG_DIR, f))
    if img is None: return default_paper_bg(W, H, rng)
    h, w = img.shape[:2]; sc = max(W / w, H / h)
    r = cv2.resize(img, (int(math.ceil(w * sc)), int(math.ceil(h * sc))), interpolation=cv2.INTER_AREA if sc < 1 else cv2.INTER_CUBIC)
    return r[(r.shape[0] - H) // 2:(r.shape[0] - H) // 2 + H, (r.shape[1] - W) // 2:(r.shape[1] - W) // 2 + W].astype(np.float32)

def rr_alpha(w, h, r, ss=3):
    m = Image.new("L", (w * ss, h * ss), 0); ImageDraw.Draw(m).rounded_rectangle([0, 0, w * ss - 1, h * ss - 1], radius=r * ss, fill=255)
    return np.array(m.resize((w, h), Image.LANCZOS)).astype(np.float32) / 255.0

def make_mockup(screen_bgr, rng, art_bgr=None):
    bg = (gradient_bg(art_bgr, CANVAS_W, CANVAS_H) if (BG_MODE == "dynamic" and art_bgr is not None) else pick_bg(CANVAS_W, CANVAS_H, rng)).astype(np.float32)
    ph = PHONE_H; pw = int(round(ph * SW / SH)); x0 = (CANVAS_W - pw) // 2; y0 = int(CANVAS_H * 0.0617)
    rad = int(round(CORNER_R * ph / SH))
    a = rr_alpha(pw, ph, rad)
    full = np.zeros((CANVAS_H, CANVAS_W), np.float32); full[y0:y0 + ph, x0:x0 + pw] = a
    def sh(dx, dy, blur, al):
        return cv2.GaussianBlur(cv2.warpAffine(full, np.float32([[1, 0, dx], [0, 1, dy]]), (CANVAS_W, CANVAS_H)), (0, 0), blur) * al
    s = np.clip(sh(0, 30, 42, 0.30) + sh(0, 7, 8, 0.16), 0, 0.6)
    bg = bg * (1 - s[..., None])
    scr = cv2.resize(screen_bgr, (pw, ph), interpolation=cv2.INTER_AREA).astype(np.float32)
    reg = bg[y0:y0 + ph, x0:x0 + pw]
    bg[y0:y0 + ph, x0:x0 + pw] = scr * a[..., None] + reg * (1 - a[..., None])
    return np.clip(bg, 0, 255).astype(np.uint8)

def fit_screen(w_bgr):
    return resize_exact(w_bgr, SW, SH)


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
    if len(img.shape)==2: img=cv2.cvtColor(img,cv2.COLOR_GRAY2BGRA)
    elif img.shape[2]==3: img=np.concatenate([img, np.full((img.shape[0],img.shape[1],1),255,dtype=np.uint8)],axis=2)
    return img

if len(sys.argv) < 2: sys.exit()
action = sys.argv[1]

if action == 'process':
    display_header()
    os.makedirs(O, exist_ok=True); os.makedirs(R, exist_ok=True)
    
    imgs = [f for f in os.listdir(I) if f.lower().endswith(('.jpg','.jpeg','.png','.webp','.avif','.heic')) and not f.startswith('.') and "_temp" not in f]
    if len(imgs) == 0:
        print(f" {C2}✖ Images not found.{ENDC}")
        sys.exit()

    print(f" {C5}✿ {BOLD}S e l e c t  M o d e{ENDC}")
    print(f" {C1}[1]{ENDC} Normal (Fast KB Pack) - {C4}[DEFAULT]{ENDC}")
    print(f" {C1}[2]{ENDC} HD Mode (AI Enhance)")
    
    choice = input(f"\n {C4}➜ {ENDC}").strip()
    is_hd = (choice == '2')

    print(f" {C5}✿ {BOLD}S e l e c t  S i z e{ENDC}")
    print(f" {C1}[1]{ENDC} Original Size - {C4}[DEFAULT]{ENDC}")
    print(f" {C1}[2]{ENDC} Crop 9:16 (1080x1920)")
    crop_916 = (input(f"\n {C4}➜ {ENDC}").strip() == '2')
    
        
    s_idx = get_max_seq([O, R, W_H]) + 1
    cur_t = time.time()
    if os.path.exists(T_F):
        try:
            with open(T_F,"r") as f: 
                lt=float(f.read().strip())
                if lt>cur_t: cur_t=lt
        except: pass

    print("") 
    pbar_format = f" {C1}{{desc}}{ENDC} {C5}|{ENDC}{{bar:20}}{C5}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"
    desc_text = "✨ HD Mode " if is_hd else "⚙️ Normal "
    desc_text += "| Crop 9:16 " if crop_916 else "| Original "

    processed_count = 0
    for f in tqdm(imgs, desc=desc_text, bar_format=pbar_format):
        try:
            raw_p = os.path.join(I, f)
            w = cv2.imread(raw_p)
            if w is None: continue
            seed = random.randrange(1 << 30)
            w_scr = resize_exact(w, SW, SH)
            if is_hd: w_scr = apply_hd_enhance(w_scr)
            ui_col, e1, e2 = pick_colors(w_scr[int(SH * 0.48):])
            cut_mock = make_papercut(w_scr, np.random.RandomState(seed), e1, e2)      # phone ki screen ke liye
            now = datetime.datetime.now()
            date_str = FIXED_DATE or (now.strftime("%A, %B ") + str(now.day))
            time_str = FIXED_TIME or f"{random.randint(1, 12)}:{random.randint(0, 59):02d}"
            temp = random.randint(58, 96)
            screen = draw_ui(cut_mock, ui_col, time_str, date_str, temp)
            mock = make_mockup(screen, np.random.RandomState(seed + 1), w_scr[int(SH * 0.40):])
            # 3 cheezein: (1) mockup  (2) aapka wallpaper fix size (1080x1920)  (3) paper-cut wallpaper (wohi fix size)
            plain = resize_exact(w) if crop_916 else w
            if is_hd: plain = apply_hd_enhance(plain)
            cut = make_papercut(plain, np.random.RandomState(seed), e1, e2)
            mock_path = os.path.join(O, f"{s_idx}.jpg")
            plain_path = os.path.join(R, f"{s_idx+1}.jpg")
            cut_path = os.path.join(R, f"{s_idx+2}.jpg")
            mt, pt, ct = cur_t + 60, cur_t + 120, cur_t + 180
            save_optimized(mock, mock_path); os.utime(mock_path, (mt, mt)); safe_scan(mock_path)
            save_optimized(plain, plain_path); os.utime(plain_path, (pt, pt)); safe_scan(plain_path)
            save_optimized(cut, cut_path); os.utime(cut_path, (ct, ct)); safe_scan(cut_path)
            gallery_copy_outputs([(mock_path, mt), (plain_path, pt), (cut_path, ct)])
            os.remove(raw_p)
            s_idx += 3
            cur_t = ct
            processed_count += 1
        except Exception as e:
            if os.environ.get("MOCKUP_DEBUG"):
                import traceback; traceback.print_exc()

    if processed_count > 0:
        try:
            with open(T_F,"w") as f: f.write(str(cur_t))
        except: pass
        print(f"\n {C4}✔ {processed_count} Processed successfully!{ENDC}\n")
    else:
        print(f"\n {C5}✔ Already processed.{ENDC}\n")

elif action == 'batch':
    display_header()
    os.makedirs(W_H, exist_ok=True)
    mks = [f for f in os.listdir(O) if os.path.splitext(f)[0].isdigit()] if os.path.exists(O) else []
    if mks:
        bn = len([d for d in os.listdir(W_H) if os.path.isdir(os.path.join(W_H, d))]) + 1
        cb = os.path.join(W_H, f"P{P_NUM}_Batch_{bn}")
        os.makedirs(cb, exist_ok=True)
        
        for f in os.listdir(O): 
            if os.path.splitext(f)[0].isdigit(): 
                src = os.path.join(O,f); dst = os.path.join(cb,f); shutil.move(src,cb); safe_scan([src,dst])
        for f in os.listdir(R):
            if os.path.splitext(f)[0].isdigit(): 
                src = os.path.join(R,f); dst = os.path.join(cb,f); shutil.move(src,cb); safe_scan([src,dst])
                
        animated_spinner("Packing Batch", 0.6)
        print(f" {C4}📦 {os.path.basename(cb)} Ready for upload!{ENDC}\n")
    else:
        print(f"\n {C2}✖ No data for batch.{ENDC}\n")

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
    
    def perm_del_images(folder):
        c = 0
        if os.path.exists(folder):
            for f in os.listdir(folder):
                if not f.startswith('.') and f.lower().endswith(img_exts):
                    try: 
                        p = os.path.join(folder, f); os.remove(p); safe_scan(p); c += 1
                    except: pass
        return c

    if choice == '1': print(f"\n {C4}✔ {perm_del_images(I)} Input Images Deleted.{ENDC}")
    elif choice == '2': print(f"\n {C4}✔ {perm_del_images(O) + perm_del_images(R)} Tayaar Images Deleted.{ENDC}")
    elif choice == '3':
        if not os.path.exists(W_H): print(f"\n {C2}✖ Warehouse empty!{ENDC}"); sys.exit()
        batches = sorted([d for d in os.listdir(W_H) if "Batch_" in d])
        for i, b in enumerate(batches): print(f" {C5}[{i+1}]{ENDC} Delete {b}")
        sub_choice = input(f"\n {C4}Choose Batch or 'A' (All): {ENDC}")
        if sub_choice.lower() == 'a':
            for r,d,fs in os.walk(W_H):
                for f in fs: safe_scan(os.path.join(r,f))
            shutil.rmtree(W_H); os.makedirs(W_H); print(f"\n {C4}✔ Warehouse cleaned.{ENDC}")
        elif sub_choice.isdigit() and 1 <= int(sub_choice) <= len(batches):
            target = os.path.join(W_H, batches[int(sub_choice)-1])
            for r,d,fs in os.walk(target):
                for f in fs: safe_scan(os.path.join(r,f))
            shutil.rmtree(target); print(f"\n {C4}✔ Deleted.{ENDC}")
