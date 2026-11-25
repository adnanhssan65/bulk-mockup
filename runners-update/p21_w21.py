# ===================== PROJECT 21 : VIVO LIVE PHOTO MAKER — TEST BUILD v4 =====================
# Input: VIDEO (kisi bhi lambai ki) -> Output: Vivo-format Live Photo JORA (NAME.jpg + NAME.mp4)
#          + 1 Mockup (live photo ke darmiyan wale frame par lockscreen design)
# TEST v4: har video par jora (A) ke sath ek SINGLE-FILE Live Photo (B: <even>_live.jpg —
#          JPG ke andar hi MP4, sirf standard Google XMP + MP4 ke aakhir me Vivo uuid box)
#          bhi banta hai, aur dono formats ki test copies DCIM/Camera me IMG_ naam se
#          bhi jati hain — taake ek hi run me pata chal jaye gallery kis ko Live manti hai.
# Vivo format (asal Vivo camera file se reverse-engineer shuda): photo aur video ALAG files,
#          ek hi naam — dono ke andar ek hi "livephoto" ID chhupa hota hai (JPG ke aakhir me
#          591-byte trailer, MP4 ke aakhir me uuid box "vivoMediaExtInfo"); gallery isi ID se
#          dono ko jor kar Live Photo chalati hai. Clip = darmiyan wale 3 second, CRF 0 LOSSLESS.
# Koi sawal nahi poochta — process bilkul automatic hai. Sirf ffmpeg lazmi hai.
# PATCH (v4 + drop-fetch): process se pehle TermuxMod drop folder ki videos khud input
#          me MOVE ho jati hain (app sirf images fetch karta hai, videos nahi).
import os, sys, time, shutil, subprocess, gc, re, struct
from datetime import datetime, timedelta
from PIL import Image, ImageFile
ImageFile.LOAD_TRUNCATED_IMAGES = True
from tqdm import tqdm

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

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
# Har file save hote hi foran, ONE BY ONE aur usi order me scan (pehle mockup, phir JPG).
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
# Pehli file (mockup) Mockups me, baqi files (Live Photo JPG + MP4 ka jora) Ready Wallpapers me —
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

# --- Vivo Live Photo builder (JPG trailer + MP4 uuid box, shared livephoto ID) ---
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

def probe_fps(path):
    """Source ka frame rate (imageTime = beech wale frame ka number nikalne ke liye). Default 30."""
    _, _, err = _ffrun([ffmpeg_exe(), "-hide_banner", "-i", path])
    m = re.search(r"([\d.]+)\s*fps", err)
    if m:
        try:
            v = float(m.group(1))
            if 1 <= v <= 240:
                return v
        except Exception:
            pass
    return 30.0

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

# --- Vivo pehchan ke bytes (asal Vivo camera file se liye gaye, bilkul wese ke wese) ---
VIVO_MAGIC = bytes([0x1b, 0x2a, 0x39, 0x48, 0x57, 0x66, 0x75, 0x84, 0x93, 0xa2, 0xb3])
# JPG trailer ka DEGS stream-info hissa — asal camera ke stream sizes; sirf maloomati hai,
# jora ID se banta hai, is liye ye bytes har file me bilkul yahi rehte hain.
VIVO_DEGS = (b'\x00\x02\xfe\x01\xff\xff\xfe\x02\xff\xff\xfe\x03\x00\x01\x00\x00\xfe\x04\x00\x00\x00\x00'
             b'\xfe\x05\x00\x00\x00\x00\xfe\x06\x00\x00\x00\x01\xfe\x07\x00\x00\x0f\xf0\xfe\x08\x00\x00\x08\xf8'
             b'\xfen\x00\x00\x01Y\xfe\x0b\x00\x00\x00Z\xfe\x0c\x00\x00\x00\x00')

def gen_live_id():
    """28 hex chars: millis timestamp (13) + random hex (7) + 00000000 — bilkul Vivo camera jesa."""
    return str(int(time.time() * 1000)) + os.urandom(4).hex()[:7] + "00000000"

def vivo_mp4_json(live_id, image_time):
    """MP4 uuid box ke andar wala JSON — key order bilkul asal file jesa."""
    return ('{"com.android.camera.takenmodel":"vivo X200 Pro","com.android.camera.camerafacing":"0",'
            f'"com.android.camera.imageTime":{image_time},"com.android.camera.moduleid":"photo",'
            f'"com.android.camera.livephoto":"{live_id}","version":2200,'
            '"com.android.camera.faceInfo":{}}').encode("utf-8")

def vivo_jpg_json(live_id):
    """JPG trailer ke andar wala JSON — key order bilkul asal file jesa."""
    return ('{"com.android.camera.joint.fullview.orientation":0,"com.android.camera.fisheye":-1,'
            '"com.android.camera.takenmodel":"vivo X200 Pro","com.android.camera.watermarkVersion":null,'
            '"com.android.camera.camerafacing":"0","com.android.camera.moduleid":"photo",'
            f'"com.android.camera.livephoto":"{live_id}","version":2200,'
            '"com.android.camera.project":"PD2405F_EX","com.android.camera.joint.fullview":false}').encode("utf-8")

def _cameralbum_tail(live_id, len_field):
    return (struct.pack(">I", len_field) + b"cameralbum!" + b"\x00\x00\x00" + b"/"
            + live_id.encode("ascii") + b"\xff\xff\xff\xff" + VIVO_MAGIC)

def build_vivo_mp4(clip_mp4, live_id, image_time, out_path):
    """Clip ke aakhir me Vivo uuid box (usertype 'vivoMediaExtInfo') jor do.
    Length field = len(JSON) - 2 (asal file par verify shuda: 269 -> 267 = 0x10b)."""
    jb = vivo_mp4_json(live_id, image_time)
    payload = b"vivo" + jb + _cameralbum_tail(live_id, len(jb) - 2)
    box = struct.pack(">I", 8 + 16 + len(payload)) + b"uuid" + b"vivoMediaExtInfo" + payload
    with open(clip_mp4, "rb") as fh:
        data = fh.read()
    with open(out_path, "wb") as fh:
        fh.write(data)
        fh.write(box)

def build_vivo_jpg(still_jpg, live_id, out_path):
    """Still JPEG ke EOI ke baad 591-byte Vivo trailer jor do (asal file: trailer kul 591 bytes).
    Length field = len(JSON) (asal file par verify shuda: 411 = 0x19b)."""
    jb = vivo_jpg_json(live_id)
    trailer = (b"streamdata" + b"DEGS" + VIVO_DEGS + b"DEGE" + b"streaminfo"
               + b"\x00\x06\x00\x01\x06\x00\x00\x00H" + b"streamcount" + b"\x00\x01"
               + b"vivo" + jb + _cameralbum_tail(live_id, len(jb)))
    with open(still_jpg, "rb") as fh:
        jpeg = fh.read()
    if jpeg[:2] != b"\xff\xd8":
        raise RuntimeError("still JPEG nahi hai")
    with open(out_path, "wb") as fh:
        fh.write(jpeg)
        fh.write(trailer)

# --- TEST v4 (B): SINGLE-FILE Live Photo — JPG ke andar MP4 ---
# JPG me SIRF standard Google XMP (Camera:MotionPhoto) — koi VCamera XMP nahi, koi JPG
# trailer nahi. EOI ke baad wahi lossless clip (aakhir me Vivo uuid box, apni alag fresh ID).
XMP_HEADER = b"http://ns.adobe.com/xap/1.0/\x00"

def build_motion_xmp(mp4_len, ts_us):
    """Standard Google Motion Photo XMP packet — sirf lazmi set, kuch extra nahi."""
    return ('<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">\n'
            ' <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">\n'
            '  <rdf:Description rdf:about=""\n'
            '    xmlns:Camera="http://ns.google.com/photos/1.0/camera/"\n'
            '    xmlns:GContainer="http://ns.google.com/photos/1.0/container/"\n'
            '    xmlns:Item="http://ns.google.com/photos/1.0/container/item/"\n'
            '    Camera:MotionPhoto="1"\n'
            '    Camera:MotionPhotoVersion="1"\n'
            f'    Camera:MotionPhotoPresentationTimestampUs="{ts_us}">\n'
            '   <GContainer:Directory>\n'
            '    <rdf:Seq>\n'
            '     <rdf:li rdf:parseType="Resource">\n'
            '      <Item:Mime>image/jpeg</Item:Mime>\n'
            '      <Item:Semantic>Primary</Item:Semantic>\n'
            '      <Item:Length>0</Item:Length>\n'
            '      <Item:Padding>0</Item:Padding>\n'
            '     </rdf:li>\n'
            '     <rdf:li rdf:parseType="Resource">\n'
            '      <Item:Mime>video/mp4</Item:Mime>\n'
            '      <Item:Semantic>MotionPhoto</Item:Semantic>\n'
            f'      <Item:Length>{mp4_len}</Item:Length>\n'
            '      <Item:Padding>0</Item:Padding>\n'
            '     </rdf:li>\n'
            '    </rdf:Seq>\n'
            '   </GContainer:Directory>\n'
            '  </rdf:Description>\n'
            ' </rdf:RDF>\n'
            '</x:xmpmeta>').encode("utf-8")

def build_single_file(still_jpg, vivo_mp4, ts_us, out_path):
    """Still JPEG (SOI ke foran baad XMP APP1) + EOI ke baad poora Vivo MP4.
    Item:Length = appended MP4 ki kul lambai (uuid box samet)."""
    mp4_len = os.path.getsize(vivo_mp4)
    packet = build_motion_xmp(mp4_len, ts_us)
    seg_len = 2 + len(XMP_HEADER) + len(packet)
    if seg_len > 65533:
        raise RuntimeError("XMP segment bohat bara hai")
    app1 = b"\xff\xe1" + struct.pack(">H", seg_len) + XMP_HEADER + packet
    with open(still_jpg, "rb") as fh:
        jpeg = fh.read()
    if jpeg[:2] != b"\xff\xd8":
        raise RuntimeError("still JPEG nahi hai")
    with open(out_path, "wb") as out:
        out.write(jpeg[:2])       # SOI
        out.write(app1)           # XMP APP1
        out.write(jpeg[2:])       # baqi JPEG (EOI samet)
        with open(vivo_mp4, "rb") as fh:
            shutil.copyfileobj(fh, out)

# --- TEST v4: DCIM/Camera test copies (IMG_ naam, camera style) ---
DCIM_DIR = "/storage/emulated/0/DCIM/Camera"
def dcim_test_drop(pair_jpg, pair_mp4, live_jpg):
    """(A) jora aur (B) single-file — dono ki copies DCIM/Camera me IMG_<time> naam se,
    har copy ke foran baad scan. Koi bhi masla ho to sirf ek note, run rukta nahi."""
    try:
        os.makedirs(DCIM_DIR, exist_ok=True)
        now = datetime.now()
        stem_a = "IMG_" + now.strftime("%Y%m%d_%H%M%S")
        stem_b = "IMG_" + (now + timedelta(seconds=1)).strftime("%Y%m%d_%H%M%S")
        for src, name in ((pair_jpg, stem_a + ".jpg"), (pair_mp4, stem_a + ".mp4"),
                          (live_jpg, stem_b + ".jpg")):
            d = os.path.join(DCIM_DIR, name)
            shutil.copyfile(src, d)
            safe_scan(d)
            print(f" {C5}\u2139 (TEST) DCIM copy: {name}{ENDC}")
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
    try:
        clip_mp4 = os.path.join(stage, "clip.mp4")
        still_jpg = os.path.join(stage, "still.jpg")
        make_clip(raw_p, start, dur, clip_mp4)
        extract_still_jpeg(raw_p, still_abs, still_jpg)
        mock_path = os.path.join(O, f"{idx}.jpg")
        ready_jpg = os.path.join(R, f"{idx + 1}.jpg")
        ready_mp4 = os.path.join(R, f"{idx + 1}.mp4")
        build_mockup(still_jpg, mock_path)
        # Vivo jora: ek hi livephoto ID photo aur video dono ke andar
        live_id = gen_live_id()
        clip_dur = dur if dur else total
        image_time = max(1, int(round((clip_dur / 2) * probe_fps(raw_p))))
        build_vivo_jpg(still_jpg, live_id, ready_jpg)
        build_vivo_mp4(clip_mp4, live_id, image_time, ready_mp4)
        # TEST (B) single-file: wahi clip, apni ALAG fresh ID ke Vivo uuid box ke sath,
        # still JPEG ke andar (sirf standard Google XMP, koi trailer nahi)
        live_path = os.path.join(R, f"{idx + 1}_live.jpg")
        live_mp4_stage = os.path.join(stage, "live.mp4")
        build_vivo_mp4(clip_mp4, gen_live_id(), image_time, live_mp4_stage)
        ts_us = int(round((clip_dur / 2) * 1000000))
        build_single_file(still_jpg, live_mp4_stage, ts_us, live_path)
    finally:
        shutil.rmtree(stage, ignore_errors=True)
    mt, wt = t_now + 60, t_now + 120
    os.utime(mock_path, (mt, mt))
    os.utime(ready_jpg, (wt, wt))
    os.utime(ready_mp4, (wt, wt))
    os.utime(live_path, (wt, wt))
    for p in (mock_path, ready_jpg, ready_mp4, live_path):   # save verify
        if not os.path.exists(p) or os.path.getsize(p) < 100:
            raise IOError("save verify failed: " + p)
    safe_scan(mock_path); safe_scan(ready_jpg); safe_scan(ready_mp4); safe_scan(live_path)
    gallery_copy_outputs([(mock_path, mt), (ready_jpg, wt), (ready_mp4, wt), (live_path, wt)])
    dcim_test_drop(ready_jpg, ready_mp4, live_path)
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
        print(f"\n {C4}\u2714 {done} Vivo Live Photo + Mockup ready! (TEST v4 \u2014 har video: 1 mockup + (A) Live Photo jora JPG + MP4 + (B) single-file _live.jpg; dono ki test copies DCIM/Camera me bhi gayi){ENDC}")
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
