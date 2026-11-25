# ===================== PROJECT 21 : LIVE PHOTO MAKER — FINAL v7 (SAMSUNG) =====================
# Input: VIDEO (kisi bhi lambai ki) -> Output: 1 Mockup + 1 Live Photo (Samsung Motion Photo)
# FINAL v7: phone par sabit ho gaya (2026-11-25) ke Vivo gallery aur TikTok sirf
#          SAMSUNG Galaxy S23+ wali Motion Photo saakht ko hi Live Photo mante
#          hain — v1-v5 ke saare tareeqay (ios / vivo / clone / google / vivobox /
#          oppo) FAIL hue the, un sab me Samsung ki asal SEF saakht ghaib thi.
#          v6 TEST me Samsung construction pass hui; final me har video se sirf
#          DO outputs bante hain:
#   1) Mockup     : <n-1>.jpg (Mockups) — Live Photo ki key photo (bilkul beech
#                   wala frame) par lockscreen design.
#   2) Live Photo : <n>.jpg (Ready Wallpapers) — asal Samsung mpv3 file: JPEG ke
#                   andar minimal EXIF (Make 'samsung', Model 'SM-S918B') +
#                   Google XMP (MotionPhoto), EOI ke baad [MotionPhoto_Data
#                   block][MP4][MotionPhoto_Version block 'mpv3'][SEFH footer]
#                   — Samsung ki SEF tartib.
# Samsung format ki spec: reviventura/LivePhotoTools docs/FORMATS.md;
# construction aviv926/MotionPhoto2 (SamsungTags.py) se port ki gayi hai — MIT
# license, poora text LICENSE.txt me hai.
# Clip EK hi dafa encode hoti hai (darmiyan wale 3 second, CRF 0 LOSSLESS).
# HD mode Project 1 ki tarah hai, lekin setting se badal sakta hai: key photo
#          par family wala apply_hd_enhance, aur clip ki encode par halka
#          denoise + sharpen (ffmpeg hqdn3d + unsharp) — phir CRF 0, yani HD
#          result bilkul lossless. Mode project folder ki .p21_mode file me
#          save rehta hai (HD / NORMAL); file na ho ya parhi na jaye to
#          default HD. Badalna ho to: python w21.py setting 21
# Mockup ki ghari asal waqt dikhati hai: run ke pehle mockup par usi waqt ka
#          time, har agle mockup par +1 minute; date bhi usi din ki asal.
# Koi sawal nahi poochta — process bilkul automatic hai. Sirf ffmpeg lazmi hai.
# Drop-fetch: process se pehle TermuxMod drop folder ki videos khud input me MOVE
#          ho jati hain (app sirf images fetch karta hai, videos nahi).
import os, sys, time, shutil, subprocess, gc, re, struct
from datetime import datetime, timedelta
import numpy as np
import cv2
from PIL import Image, ImageFile, ImageDraw, ImageFont, ImageFilter
ImageFile.LOAD_TRUNCATED_IMAGES = True
from tqdm import tqdm

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
LIVE_SECONDS = 3.0              # Live Photo ki clip lambai (video ke darmiyan wale 3 second)
FONT_DATE = os.path.join(B, "fonts", "NotoSans-Light.ttf")
FONT_CLOCK = os.path.join(B, "fonts", "NotoSansDisplay-Light.ttf")
HD_VFILTER = "hqdn3d=4:4:3:3,unsharp=5:5:1.0:5:5:0.0"   # clip ki HD safai + tezgi (encode se pehle)
MODE_FILE = os.path.join(B, ".p21_mode")   # save shuda mode: "HD" ya "NORMAL"
MODE = ["HD"]        # is run ka active mode (process ke start par file se parha jata hai)
RUN_START = [None]   # process run ke start ka waqt — mockup ki ghari yahin se chalti hai
MOCK_SEQ = [0]       # is run me ab tak bane mockups (har agla mockup +1 minute)

def read_mode():
    """Save shuda mode parho — file na ho, parhi na jaye ya andar ghalat likha
    ho to default HD. Sirf saaf 'NORMAL' hi Normal deta hai."""
    try:
        with open(MODE_FILE, "r") as fh:
            v = fh.read().strip().upper()
        if v in ("HD", "NORMAL"):
            return v
    except Exception:
        pass
    return "HD"

def save_mode(v):
    with open(MODE_FILE, "w") as fh:
        fh.write(v + "\n")

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
# Har file save hote hi foran, ONE BY ONE aur usi order me scan (pehle mockup, phir files).
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
# Pehli file (mockup) Mockups me, baqi files (Live Photo JPGs) Ready Wallpapers me —
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

# --- Drop folder auto-fetch (TermuxMod) ---
# App drop folder se sirf IMAGES fetch karta hai, videos kabhi nahi — is liye process
# shuru hone se pehle drop folder (TermuxMod/Project N) ki videos khud input me MOVE
# kar lo, taake folder wala tareeqa bina terminal command ke chal jaye.
# Sirf seedhi files (subfolders ko haath nahi); naam takra jaye to _1, _2 suffix.
# Koi bhi masla ho to khamoshi se aage barho — run kabhi nahi rukta.
def fetch_drop_videos():
    try:
        src_dir = os.path.join(TM_ROOT, f"Project {P_NUM}")
        if not os.path.isdir(src_dir):
            return
        names = [f for f in os.listdir(src_dir)
                 if os.path.isfile(os.path.join(src_dir, f)) and f.lower().endswith(VID_EXTS)]
        moved = 0
        for f in sorted(names):
            dst = os.path.join(I, f)
            if os.path.exists(dst):
                stem, ext = os.path.splitext(f)
                n = 1
                while os.path.exists(os.path.join(I, f"{stem}_{n}{ext}")):
                    n += 1
                dst = os.path.join(I, f"{stem}_{n}{ext}")
            shutil.move(os.path.join(src_dir, f), dst)
            moved += 1
        if moved:
            print(f" {C4}\U0001f4e5 Drop folder se {moved} video(s) mil gayin{ENDC}")
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

FFMPEG_MSG = (f" {C2}\u2716 ffmpeg nahi mila! Live Photo banane ke liye ffmpeg lazmi hai.\n"
              f"   Pehle Termux me ye command chalao:  pkg install -y ffmpeg\n"
              f"   (ffmpeg not found \u2014 install it first, then run again.){ENDC}")

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

def probe_has_audio(path):
    _, _, err = _ffrun([ffmpeg_exe(), "-hide_banner", "-i", path])
    return " Audio: " in err

def make_clip(src, start, dur, out_mp4):
    """Darmiyan wali ~3s clip — pehle HD filter (halka denoise + sharpen), phir
    libx264 CRF 0 (mathematically LOSSLESS — HD result bilkul waisa hi rehta hai).
    Preset sirf size/speed ke liye hai; CRF 0 par quality hamesha lossless rehti hai.
    Source me awaz ho to wo bhi bilkul asal (-c:a copy) sath jati hai."""
    def _enc(with_audio):
        cmd = [ffmpeg_exe(), "-y", "-v", "error"]
        if start:
            cmd += ["-ss", f"{start:.4f}"]
        cmd += ["-i", src]
        if dur:
            cmd += ["-t", f"{dur:.4f}"]
        cmd += ["-map", "0:v:0"]
        if with_audio:
            cmd += ["-map", "0:a:0?"]
        if MODE[0] == "HD":
            cmd += ["-vf", HD_VFILTER]
        cmd += ["-c:v", "libx264", "-crf", "0",
                "-preset", "fast", "-pix_fmt", probe_pix_fmt(src)]
        if with_audio:
            cmd += ["-c:a", "copy"]
        else:
            cmd += ["-an"]
        cmd += ["-f", "mp4", out_mp4]
        return _ffrun(cmd)
    want_audio = probe_has_audio(src)
    rc, _, err = _enc(want_audio)
    if (rc != 0 or not os.path.exists(out_mp4)) and want_audio:
        rc, _, err = _enc(False)   # awaz copy me masla ho to baghair awaz dobara
    if rc != 0 or not os.path.exists(out_mp4):
        raise RuntimeError("clip encode failed: " + err[-400:])

# --- HD enhance (Project 1 ke apply_hd_enhance ki hubahu copy — family standard) ---
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

def extract_still_jpeg(src, t, out_jpg):
    """Time t wala frame — full resolution par nikalo; HD mode ho to family HD
    enhance lagao, Normal mode me bilkul asal frame. Phir JPEG (q100, 4:4:4).
    Yahi photo Live Photo me bhi jati hai aur mockup me bhi."""
    vf = "scale=in_color_matrix=auto:in_range=auto:out_range=full,format=rgb24"
    rc, png, err = _ffrun([ffmpeg_exe(), "-v", "error", "-ss", f"{t:.4f}", "-i", src,
                           "-frames:v", "1", "-vf", vf,
                           "-f", "image2pipe", "-vcodec", "png", "-"])
    if rc != 0 or not png:
        raise RuntimeError("frame extract failed: " + err[-400:])
    import io
    with Image.open(io.BytesIO(png)) as im:
        rgb_im = im.convert("RGB")
    if MODE[0] != "HD":
        rgb_im.save(out_jpg, "JPEG", quality=100, subsampling=0, optimize=True)
        return
    rgb = np.array(rgb_im)
    bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
    hd = apply_hd_enhance(bgr)
    Image.fromarray(cv2.cvtColor(hd, cv2.COLOR_BGR2RGB)).save(
        out_jpg, "JPEG", quality=100, subsampling=0, optimize=True)

# --- Samsung Motion Photo builder (SEF) — FINAL v7 ---
# Saakht (JPEG ke liye):
#   [ JPEG: SOI + EXIF APP1 + XMP APP1 + baqi image ... EOI ]
#   [ MotionPhoto_Data block : id(4) | nameLength(4, LE) | "MotionPhoto_Data" | MP4 bytes ]
#   [ MotionPhoto_Version block : id(4) | nameLength(4, LE) | "MotionPhoto_Version" | "mpv3" ]
#   [ SEFH footer : "SEFH" | version 107 (LE) | count (LE) |
#                   ( id | offsetBack | length ) x count | indexLength (LE) | "SEFT" ]
# offsetBack = SEFH ke start se peeche gin kar us block ke start tak ka faasla.
# Google XMP: Primary ka Item:Padding = MotionPhoto_Data block ke header ki lambai (24),
# MotionPhoto ka Item:Length = MP4 ke start se file ke aakhir tak (video + trailer dono).
# Spec: reviventura/LivePhotoTools docs/FORMATS.md; construction aviv926/MotionPhoto2
# ke SamsungTags.py se port (MIT — LICENSE.txt dekho).
SEF_VERSION = 107
SEF_TAGS = (("MotionPhoto_Data", b"\x00\x00\x30\x0a"),
            ("MotionPhoto_Version", b"\x00\x00\x31\x0a"))
SEF_DATA_HEADER = 4 + 4 + len("MotionPhoto_Data")   # 24 — XMP Primary Padding yahi hai
XMP_HEADER = b"http://ns.adobe.com/xap/1.0/\x00"

def _app1_segment(payload):
    ln = 2 + len(payload)
    if ln > 65533:
        raise RuntimeError("APP1 segment bohat bara hai")
    return b"\xff\xe1" + struct.pack(">H", ln) + payload

def build_exif_app1(now):
    """Minimal EXIF APP1 segment: Make 'samsung', Model 'SM-S918B', DateTime = abhi ka waqt.
    TIFF little-endian; IFD0 ke 3 entries (tag tartib me), 4 byte se bare values
    IFD ke baad wali data jagah me rehte hain."""
    dt = now.strftime("%Y:%m:%d %H:%M:%S").encode("ascii") + b"\x00"   # 20 bytes
    vals = [(0x010F, b"samsung\x00"), (0x0110, b"SM-S918B\x00"), (0x0132, dt)]
    data_base = 8 + 2 + 12 * len(vals) + 4
    body, extra = b"", b""
    for tag, raw in vals:
        if len(raw) <= 4:
            field = raw.ljust(4, b"\x00")
        else:
            field = struct.pack("<I", data_base + len(extra))
            extra += raw
            if len(extra) % 2:
                extra += b"\x00"
        body += struct.pack("<HHI", tag, 2, len(raw)) + field
    tiff = (b"II" + struct.pack("<H", 42) + struct.pack("<I", 8)
            + struct.pack("<H", len(vals)) + body + struct.pack("<I", 0) + extra)
    return _app1_segment(b"Exif\x00\x00" + tiff)

def build_samsung_xmp(item_length, ts_us):
    """Samsung/Google Motion Photo XMP packet (GCamera set + Container directory)."""
    return ('<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">\n'
            ' <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">\n'
            '  <rdf:Description rdf:about=""\n'
            '    xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"\n'
            '    xmlns:Container="http://ns.google.com/photos/1.0/container/"\n'
            '    xmlns:Item="http://ns.google.com/photos/1.0/container/item/"\n'
            '    GCamera:MotionPhoto="1"\n'
            '    GCamera:MotionPhotoVersion="1"\n'
            f'    GCamera:MotionPhotoPresentationTimestampUs="{ts_us}">\n'
            '   <Container:Directory>\n'
            '    <rdf:Seq>\n'
            '     <rdf:li rdf:parseType="Resource">\n'
            '      <Container:Item Item:Mime="image/jpeg" Item:Semantic="Primary" '
            f'Item:Length="0" Item:Padding="{SEF_DATA_HEADER}"/>\n'
            '     </rdf:li>\n'
            '     <rdf:li rdf:parseType="Resource">\n'
            '      <Container:Item Item:Mime="video/mp4" Item:Semantic="MotionPhoto" '
            f'Item:Length="{item_length}" Item:Padding="0"/>\n'
            '     </rdf:li>\n'
            '    </rdf:Seq>\n'
            '   </Container:Directory>\n'
            '  </rdf:Description>\n'
            ' </rdf:RDF>\n'
            '</x:xmpmeta>').encode("utf-8")

def build_sef_tail(clip_bytes):
    """JPEG ke EOI ke baad lagne wala Samsung hissa: dono SEF blocks + SEFH footer.
    (MotionPhoto2 ke SamsungTags.video_footer ki JPEG wali shakal, byte-for-byte.)"""
    blocks = []
    for name, tid in SEF_TAGS:
        payload = clip_bytes if name == "MotionPhoto_Data" else b"mpv3"
        blocks.append(tid + struct.pack("<i", len(name)) + name.encode("ascii") + payload)
    tail = b"".join(blocks)
    sefh = b"SEFH" + struct.pack("<i", SEF_VERSION) + struct.pack("<i", len(blocks))
    offsets, run = [], 0
    for blk in reversed(blocks):
        run += len(blk)
        offsets.append(run)
    offsets.reverse()
    for (name, tid), blk, off in zip(SEF_TAGS, blocks, offsets):
        sefh += tid + struct.pack("<i", off) + struct.pack("<i", len(blk))
    sefh += struct.pack("<i", len(sefh)) + b"SEFT"
    return tail + sefh

def build_samsung_file(still_jpg, clip_mp4, ts_us, out_path):
    """Key photo (SOI ke foran baad EXIF APP1 + XMP APP1) + EOI ke baad Samsung SEF tail.
    Item:Length = tail ki lambai − 24 (yani MP4 ke start se file ke aakhir tak)."""
    with open(clip_mp4, "rb") as fh:
        clip = fh.read()
    tail = build_sef_tail(clip)
    item_length = len(tail) - SEF_DATA_HEADER
    xmp = _app1_segment(XMP_HEADER + build_samsung_xmp(item_length, ts_us))
    exif = build_exif_app1(datetime.now())
    with open(still_jpg, "rb") as fh:
        jpeg = fh.read()
    if jpeg[:2] != b"\xff\xd8":
        raise RuntimeError("still JPEG nahi hai")
    with open(out_path, "wb") as fh:
        fh.write(jpeg[:2])    # SOI
        fh.write(exif)        # EXIF APP1
        fh.write(xmp)         # XMP APP1
        fh.write(jpeg[2:])    # baqi JPEG (EOI samet)
        fh.write(tail)        # Samsung SEF tail

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

def mockup_texts(show_dt):
    """Mockup par chhapne wali date + ghari — reference wale andaaz me:
    date 'Tue 25 Nov' (din ka chhota naam + tareekh + maheene ka chhota naam),
    ghari '4:40' (ghanta baghair agle zero ke, minute do hindse)."""
    date_str = f"{show_dt:%a} {show_dt.day} {show_dt:%b}"
    clock_str = f"{show_dt.hour % 12 or 12}:{show_dt.minute:02d}"
    return date_str, clock_str

def draw_clock_layer(size, show_dt):
    """Date + ghari ki transparent layer — make_template wali hi geometry, fonts
    aur size (2x supersample par bana kar neeche scale, taake kinare naram rahen).
    Template ke andar ab ye text baked NAHI hai; har mockup par asal waqt banta hai."""
    SS = 2
    W, H = size
    layer = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    date_str, clock_str = mockup_texts(show_dt)
    f_date = ImageFont.truetype(FONT_DATE, 47 * SS)
    tw = d.textlength(date_str, font=f_date)
    d.text((540 * SS - tw / 2, 256 * SS), date_str, font=f_date, fill=(255, 255, 255, 255))
    size_1x = 200
    while True:
        f_clock = ImageFont.truetype(FONT_CLOCK, size_1x * SS)
        if d.textlength(clock_str, font=f_clock) >= 600 * SS:
            break
        size_1x += 4
    tw = d.textlength(clock_str, font=f_clock)
    d.text((540 * SS - tw / 2, 322 * SS), clock_str, font=f_clock, fill=(255, 255, 255, 255))
    return layer.resize((W, H), Image.LANCZOS)

def build_mockup(key_jpg, out_path, show_dt):
    """Live Photo ki key photo (enhanced still) ko lockscreen template me lagao,
    upar asal date + ghari (show_dt) chhapo."""
    tpl = Image.open(TEMPLATE).convert("RGBA")
    mask_full = Image.open(SCREEN_MASK).convert("L")
    mask = mask_full.crop(SCREEN)
    sw, sh = SCREEN[2] - SCREEN[0], SCREEN[3] - SCREEN[1]
    with Image.open(key_jpg) as im:
        frame = im.convert("RGB")
    # cover-fit: screen ko poora bharo, darmiyan se crop (behtareen resampling)
    scale = max(sw / frame.width, sh / frame.height)
    nw, nh = int(frame.width * scale + 0.5), int(frame.height * scale + 0.5)
    frame = frame.resize((nw, nh), Image.LANCZOS)
    x = (nw - sw) // 2; y = (nh - sh) // 2
    frame = frame.crop((x, y, x + sw, y + sh))
    if scale > 1.0:
        # Frame screen se chhota tha (upscale) — andar wali tasveer par halki
        # si tezgi, taake bari screen par dhundli na lage (sirf mockup par).
        frame = frame.filter(ImageFilter.UnsharpMask(radius=2, percent=60, threshold=2))
    canvas = Image.new("RGB", tpl.size, (0, 0, 0))
    canvas.paste(frame, (SCREEN[0], SCREEN[1]), mask)
    canvas.paste(tpl, (0, 0), tpl)
    layer = draw_clock_layer(tpl.size, show_dt)
    canvas.paste(layer, (0, 0), layer)
    canvas.save(out_path, quality=100, subsampling=0)

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
    n = idx + 1
    mock_path = os.path.join(O, f"{idx}.jpg")
    live_path = os.path.join(R, f"{n}.jpg")
    try:
        clip_mp4 = os.path.join(stage, "clip.mp4")
        still_jpg = os.path.join(stage, "still.jpg")
        make_clip(raw_p, start, dur, clip_mp4)          # EK hi encode (HD filter + CRF 0 lossless)
        extract_still_jpeg(raw_p, still_abs, still_jpg)
        base = RUN_START[0] or datetime.now()
        show_dt = base + timedelta(minutes=MOCK_SEQ[0])
        MOCK_SEQ[0] += 1
        build_mockup(still_jpg, mock_path, show_dt)
        d_str, c_str = mockup_texts(show_dt)
        print(f" {C5}\U0001f550 Mockup time: {c_str} ({d_str}){ENDC}")
        clip_dur = dur if dur else total
        ts_us = int(round((clip_dur / 2) * 1000000))
        build_samsung_file(still_jpg, clip_mp4, ts_us, live_path)
    finally:
        shutil.rmtree(stage, ignore_errors=True)
    mt, wt = t_now + 60, t_now + 120
    os.utime(mock_path, (mt, mt))
    os.utime(live_path, (wt, wt))
    for p in [mock_path, live_path]:   # save verify
        if not os.path.exists(p) or os.path.getsize(p) < 100:
            raise IOError("save verify failed: " + p)
    safe_scan(mock_path)
    safe_scan(live_path)
    gallery_copy_outputs([(mock_path, mt), (live_path, wt)])
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
    fetch_drop_videos()
    vids = list_inputs()
    if not vids:
        print(f" {C2}\u2716 {P_NUM}_wallpapers me koi video (.mp4/.mov/.m4v) nahi mili.{ENDC}"); sys.exit()
    print(f" {C4}\u2714 Live Photo engine ready! (ffmpeg mil gaya){ENDC}")
    MODE[0] = read_mode()
    print(f" {C5}\u2699\ufe0f Mode: {'HD' if MODE[0] == 'HD' else 'Normal'}{ENDC}")
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
    RUN_START[0] = datetime.now()   # mockup ki ghari is run ke start se chalegi
    MOCK_SEQ[0] = 0
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
        print(f"\n {C4}\u2714 {done} video(s) tayyar! (har video: 1 mockup + 1 Live Photo — Samsung format){ENDC}")
    else:
        print(f"\n {C2}\u2716 Koi Live Photo nahi bani.{ENDC}")
    if skip_log:
        print(f" {C2}\u26a0 {len(skip_log)} skip (video reh gayi, agle run me retry):{ENDC}")
        for s in skip_log[:8]:
            print(f"   {C2}- {s}{ENDC}")
    print("")

if action == 'setting':
    display_header()
    cur = read_mode()
    print(f" {C5}\u273f {BOLD}S e t t i n g{ENDC}")
    print(f" {C5}\u2139 Abhi wala mode: {'HD' if cur == 'HD' else 'Normal'}{ENDC}")
    print(f" {C1}[1]{ENDC} HD Mode")
    print(f" {C1}[2]{ENDC} Normal Mode")
    print(f" {C1}[0]{ENDC} Cancel")
    ch = input(f"\n {C4}\u279c {ENDC}").strip()
    if ch == '1':
        save_mode("HD")
        print(f" {C4}\u2714 Mode save ho gaya: HD{ENDC}")
    elif ch == '2':
        save_mode("NORMAL")
        print(f" {C4}\u2714 Mode save ho gaya: Normal{ENDC}")
    else:
        print(f" {C5}\u2139 Cancel.{ENDC}")
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
