# ===================== PROJECT 21 : LIVE PHOTO MAKER — TEST BUILD v6 (SAMSUNG) =====================
# Input: VIDEO (kisi bhi lambai ki) -> Output: 1 Mockup + 2 Samsung Motion Photo tareeqay (TEST)
# TEST v6: phone par sirf SAMSUNG Galaxy S23+ ki Motion Photos gallery ke Live tab me
#          ati hain (wo bhi Vivo MediaKit ki convert ki hui) — v1-v5 ke saare tareeqay
#          (ios / vivo / clone / google / vivobox / oppo) FAIL hue. Un sab me jo cheez
#          GHAIB thi wo hai Samsung ki asal SEF saakht. Is liye ab har video par sirf
#          DO tareeqay bante hain, dono PAKKI Samsung construction me:
#   1) samsung     : <n>_samsung.jpg — asal Samsung mpv3 file: JPEG ke andar minimal
#                    EXIF (Make 'samsung', Model 'SM-S918B') + Google XMP (MotionPhoto),
#                    EOI ke baad [MotionPhoto_Data block][MP4][MotionPhoto_Version
#                    block 'mpv3'][SEFH footer] — Samsung ki SEF tartib.
#   2) samsungvivo : <n>_samsungvivo.jpg — bilkul wahi Samsung file, sirf XMP me
#                    Vivo MediaKit ke converted-state nishan bhi: VCamera:
#                    VMotionPhotoVersion="1" + VMediaKitVersion="1.0.0.7"
#                    (phone par jo files Live mani gayin, un me yahi do nishan the).
# Samsung format ki spec: reviventura/LivePhotoTools docs/FORMATS.md;
# construction aviv926/MotionPhoto2 (SamsungTags.py) se port ki gayi hai — MIT
# license, poora text LICENSE.txt me hai.
# Clip EK hi dafa encode hoti hai (darmiyan wale 3 second, CRF 0 LOSSLESS) aur usi ke
#          bytes dono tareeqon me istemal hote hain — phone par run tez rehta hai.
# Har tareeqay ki test copy DCIM/Camera me IMG_ naam se jati hai aur run ke aakhir me
#          TEST_MAP.txt (Ready Wallpapers me) bata deta hai konsa IMG naam kis
#          tareeqay ka hai — gallery jis ko Live mane, wahi tareeqa jeetega.
# Koi sawal nahi poochta — process bilkul automatic hai. Sirf ffmpeg lazmi hai.
# PATCH (v4 + drop-fetch): process se pehle TermuxMod drop folder ki videos khud input
#          me MOVE ho jati hain (app sirf images fetch karta hai, videos nahi).
import os, sys, time, shutil, subprocess, gc, re, struct
from datetime import datetime, timedelta
from PIL import Image, ImageFile
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
DCIM_DIR = "/storage/emulated/0/DCIM/Camera"

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
    """Darmiyan wali ~3s clip — libx264 CRF 0 (mathematically LOSSLESS, quality bilkul asal jesi).
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

def extract_still_jpeg(src, t, out_jpg):
    """Time t wala frame — full resolution JPEG (q100, 4:4:4), bilkul pehle wali key photo jesa."""
    vf = "scale=in_color_matrix=auto:in_range=auto:out_range=full,format=rgb24"
    rc, png, err = _ffrun([ffmpeg_exe(), "-v", "error", "-ss", f"{t:.4f}", "-i", src,
                           "-frames:v", "1", "-vf", vf,
                           "-f", "image2pipe", "-vcodec", "png", "-"])
    if rc != 0 or not png:
        raise RuntimeError("frame extract failed: " + err[-400:])
    import io
    with Image.open(io.BytesIO(png)) as im:
        im.convert("RGB").save(out_jpg, "JPEG", quality=100, subsampling=0, optimize=True)

# --- Samsung Motion Photo builder (SEF) — TEST v6 ---
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

def build_samsung_xmp(item_length, ts_us, vivo=False):
    """Samsung/Google Motion Photo XMP packet (GCamera set + Container directory).
    vivo=True ho to usi rdf:Description me Vivo MediaKit ke converted-state attrs
    bhi shamil hote hain — bilkul wese jese phone ki mani gayi files me mile."""
    vc_ns = ('\n    xmlns:VCamera="http://ns.vivo.com/photos/1.0/camera/"') if vivo else ''
    vc_attrs = ('\n    VCamera:VMotionPhotoVersion="1"'
                '\n    VCamera:VMediaKitVersion="1.0.0.7"') if vivo else ''
    return ('<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">\n'
            ' <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">\n'
            '  <rdf:Description rdf:about=""\n'
            '    xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"\n'
            '    xmlns:Container="http://ns.google.com/photos/1.0/container/"\n'
            '    xmlns:Item="http://ns.google.com/photos/1.0/container/item/"'
            + vc_ns + '\n'
            '    GCamera:MotionPhoto="1"\n'
            '    GCamera:MotionPhotoVersion="1"\n'
            f'    GCamera:MotionPhotoPresentationTimestampUs="{ts_us}"'
            + vc_attrs + '>\n'
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

def build_samsung_file(still_jpg, clip_mp4, ts_us, vivo, out_path):
    """Key photo (SOI ke foran baad EXIF APP1 + XMP APP1) + EOI ke baad Samsung SEF tail.
    Item:Length = tail ki lambai − 24 (yani MP4 ke start se file ke aakhir tak)."""
    with open(clip_mp4, "rb") as fh:
        clip = fh.read()
    tail = build_sef_tail(clip)
    item_length = len(tail) - SEF_DATA_HEADER
    xmp = _app1_segment(XMP_HEADER + build_samsung_xmp(item_length, ts_us, vivo))
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

# --- TEST: DCIM/Camera test copies + TEST_MAP ---
# Har tareeqay ki copy DCIM/Camera me IMG_<time> naam se (har tareeqa +1 second) —
# run ke aakhir me mapping TEST_MAP.txt me.
TEST_MAP = []   # (method, ready names, dcim names)
_DCIM_NEXT = [None]   # agle group ke liye sab se pehle wala khaali second (takrao rokne ko)
def dcim_test_drop(entries):
    """entries: [(method, [ready file paths])] — tartib wahi jo tareeqon ki hai.
    Har group ke IMG stems pichle groups se takrate nahi (second aage barha dete hain)."""
    try:
        os.makedirs(DCIM_DIR, exist_ok=True)
        base = datetime.now()
        if _DCIM_NEXT[0] is not None and base < _DCIM_NEXT[0]:
            base = _DCIM_NEXT[0]
        def _stem_free(dt):
            s = "IMG_" + dt.strftime("%Y%m%d_%H%M%S")
            return not any(os.path.exists(os.path.join(DCIM_DIR, s + e))
                           for e in (".jpg", ".mp4", ".mov"))
        while not all(_stem_free(base + timedelta(seconds=k)) for k in range(len(entries))):
            base += timedelta(seconds=1)
        _DCIM_NEXT[0] = base + timedelta(seconds=len(entries))
        for k, (method, srcs) in enumerate(entries):
            stem = "IMG_" + (base + timedelta(seconds=k)).strftime("%Y%m%d_%H%M%S")
            names = []
            for src in srcs:
                name = stem + os.path.splitext(src)[1].lower()
                d = os.path.join(DCIM_DIR, name)
                shutil.copyfile(src, d)
                safe_scan(d)
                names.append(name)
                print(f" {C5}\u2139 (TEST) DCIM copy: {name}{ENDC}")
            TEST_MAP.append((method, ", ".join(os.path.basename(s) for s in srcs),
                             " + ".join(names)))
    except Exception as e:
        print(f" {C2}\u26a0 (TEST) DCIM copy nahi ho saki: {str(e)[:60]}{ENDC}")

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

def build_mockup(key_jpg, out_path):
    """Live Photo ki key photo (darmiyan wala frame) ko lockscreen template me lagao."""
    tpl = Image.open(TEMPLATE).convert("RGBA")
    mask_full = Image.open(SCREEN_MASK).convert("L")
    mask = mask_full.crop(SCREEN)
    sw, sh = SCREEN[2] - SCREEN[0], SCREEN[3] - SCREEN[1]
    with Image.open(key_jpg) as im:
        frame = im.convert("RGB")
    # cover-fit: screen ko poora bharo, darmiyan se crop
    scale = max(sw / frame.width, sh / frame.height)
    nw, nh = int(frame.width * scale + 0.5), int(frame.height * scale + 0.5)
    frame = frame.resize((nw, nh), Image.LANCZOS)
    x = (nw - sw) // 2; y = (nh - sh) // 2
    frame = frame.crop((x, y, x + sw, y + sh))
    canvas = Image.new("RGB", (1080, 1920), (0, 0, 0))
    canvas.paste(frame, (SCREEN[0], SCREEN[1]), mask)
    canvas.paste(tpl, (0, 0), tpl)
    canvas.save(out_path, quality=95)

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
    variants = []   # [(method, [paths])] — tartib: samsung, samsungvivo
    try:
        clip_mp4 = os.path.join(stage, "clip.mp4")
        still_jpg = os.path.join(stage, "still.jpg")
        make_clip(raw_p, start, dur, clip_mp4)          # EK hi encode — dono tareeqay yahi bytes
        extract_still_jpeg(raw_p, still_abs, still_jpg)
        build_mockup(still_jpg, mock_path)
        clip_dur = dur if dur else total
        ts_us = int(round((clip_dur / 2) * 1000000))

        def _try(method, fn):
            try:
                got = fn()
                if got:
                    variants.append((method, got))
            except Exception as e:
                print(f" {C2}\u26a0 (TEST) {method} nahi bana: {str(e)[:70]}{ENDC}")

        def _samsung():
            p = os.path.join(R, f"{n}_samsung.jpg")
            build_samsung_file(still_jpg, clip_mp4, ts_us, False, p)
            return [p]

        def _samsungvivo():
            p = os.path.join(R, f"{n}_samsungvivo.jpg")
            build_samsung_file(still_jpg, clip_mp4, ts_us, True, p)
            return [p]

        _try("samsung", _samsung)
        _try("samsungvivo", _samsungvivo)
    finally:
        shutil.rmtree(stage, ignore_errors=True)
    if not variants:
        raise RuntimeError("koi bhi Live Photo tareeqa nahi bana")
    mt, wt = t_now + 60, t_now + 120
    flat = [p for _, ps in variants for p in ps]
    os.utime(mock_path, (mt, mt))
    for p in flat:
        os.utime(p, (wt, wt))
    for p in [mock_path] + flat:   # save verify
        if not os.path.exists(p) or os.path.getsize(p) < 100:
            raise IOError("save verify failed: " + p)
    safe_scan(mock_path)
    for p in flat:
        safe_scan(p)
    gallery_copy_outputs([(mock_path, mt)] + [(p, wt) for p in flat])
    dcim_test_drop(variants)
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
        print(f"\n {C4}\u2714 {done} video(s) tayyar! (TEST v6 \u2014 har video: 1 mockup + 2 tareeqay: samsung / samsungvivo; test copies DCIM/Camera me bhi gayi){ENDC}")
        if TEST_MAP:
            lines = [f"Project 21 — TEST v6 method map ({datetime.now().strftime('%Y-%m-%d %H:%M:%S')})",
                     "Gallery me jo IMG naam Live Photo bana, wahi tareeqa jeeta:", ""]
            lines += [f"{m}: {r}  ->  DCIM: {d}" for (m, r, d) in TEST_MAP]
            try:
                with open(os.path.join(R, "TEST_MAP.txt"), "w") as fh:
                    fh.write("\n".join(lines) + "\n")
            except Exception:
                pass
            print(f" {C5}\u2139 TEST MAP (konsa IMG naam = konsa tareeqa):{ENDC}")
            for ln in lines[3:]:
                print(f"   {C5}{ln}{ENDC}")
    else:
        print(f"\n {C2}\u2716 Koi Live Photo nahi bani.{ENDC}")
    if skip_log:
        print(f" {C2}\u26a0 {len(skip_log)} skip (video reh gayi, agle run me retry):{ENDC}")
        for s in skip_log[:8]:
            print(f"   {C2}- {s}{ENDC}")
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
