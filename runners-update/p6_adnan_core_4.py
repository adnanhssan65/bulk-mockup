import os, sys, time, shutil, subprocess, random, threading, gc
from PIL import Image, ImageFile
ImageFile.LOAD_TRUNCATED_IMAGES = True
import PIL.ImageCms as cms
from tqdm import tqdm
import cv2, numpy as np

# --- MULTI-COLORS ---
C1 = '\033[95m'
C2 = '\033[91m'
C3 = '\033[93m'
C4 = '\033[92m'
C5 = '\033[96m'
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

HG  = '\033[92m'   
HG2 = '\033[32m'   
HC  = '\033[96m'   
HR  = '\033[91m'   
HY  = '\033[93m'   

def display_header():
    os.system('clear')
    quote = random.choice(QUOTES)
    
    print(f"{HG2}{'─' * 56}{ENDC}")
    boot_lines = [
        "Initializing secure shell........",
        "Loading encryption modules.......",
        "Mounting mockup engine...........",
        "Verifying integrity checksum.....",
    ]
    for line in boot_lines:
        print(f"{HG} [BOOT]{ENDC} {line} {HC}[OK]{ENDC}")
        time.sleep(0)
    print(f"{HG2}{'─' * 56}{ENDC}\n")
    
    ascii_art = [
        r"   █████╗ ██████╗ ███╗   ██╗█████╗ ███╗   ██╗",
        r"  ██╔══██╗██╔══██╗████╗  ██║██╔══██╗████╗  ██║",
        r"  ███████║██║  ██║██╔██╗ ██║███████║██╔██╗ ██║",
        r"  ██╔══██║██║  ██║██║╚██╗██║██╔══██║██║╚██╗██║",
        r"  ██║  ██║██████╔╝██║ ╚████║██║  ██║██║ ╚████║",
        r"  ╚═╝  ╚═╝╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═╝╚═╝  ╚═══╝"
    ]
    for line in ascii_art:
        print(f"{HG}{BOLD}{line}{ENDC}")
    
    print(f"\n{HC}      ┌──────────────────────────┐")
    print(f"      │   💻  R O O T   M O D E    │")
    print(f"      │   🔓  A C C E S S   O K    │")
    print(f"      └──────────────────────────┘{ENDC}\n")
    print(f" {HG}🌸 ⚡ 🌸   W e l c o m e   t o   t h e   S y s t e m   🌸 ⚡ 🌸{ENDC}\n")
    
    project_line = f"Project : {sys.argv[2] if len(sys.argv) > 2 else '6'}"
    owner_line   = "Owner   : Mehar Adnan"
    contact_line = "Contact : +44 7404 032136"
    pad = 46
    prefix = " ⚡ "
    box_w = len(prefix) + pad
    
    print(f" {HG2}╔{'═' * box_w}╗{ENDC}")
    print(f" {HG2}║{HY}{prefix}{project_line:<{pad}}{HG2}║{ENDC}")
    print(f" {HG2}║{HY}{prefix}{quote:<{pad}}{HG2}║{ENDC}")
    print(f" {HG2}║{HY}{prefix}{owner_line:<{pad}}{HG2}║{ENDC}")
    print(f" {HG2}║{HY}{prefix}{contact_line:<{pad}}{HG2}║{ENDC}")
    print(f" {HG2}╚{'═' * box_w}╝{ENDC}")
    
    print(f"\n {HR}🛡️  [ ACCESS GRANTED ]{ENDC} {HG}— Session Encrypted ✅{ENDC}")
    print(f" {C1}⚡{C3}✦{C5}⚡{C3}✦{C1}⚡  {BOLD}{HG}P R E M I U M   E D I T I O N{ENDC}  {C1}⚡{C3}✦{C5}⚡{C3}✦{C1}⚡{ENDC}\n")

def animated_spinner(text, duration=0.8):
    frames = ['•', 'o', 'O', 'o']
    for i in range(int(duration * 10)):
        sys.stdout.write(f"\r {C1}✿{ENDC} {C5}{frames[i % len(frames)]}{ENDC} {text}...")
        sys.stdout.flush()
        time.sleep(0)
    sys.stdout.write(f"\r {C4}✔{ENDC} {text} Completed!       \n")

def set_wakelock(enable=True):
    try:
        # Fire-and-forget: app ke session-less process me ye command atak sakti hai - intezar nahi karna
        subprocess.Popen(["termux-wake-lock" if enable else "termux-wake-unlock"],
                         stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except: pass

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
    except: pass

def safe_imread(path):
    try:
        with Image.open(path) as im:
            im.load()
            rgb = im.convert('RGB')
        return np.array(rgb)[:, :, ::-1].copy()
    except: return None

cv2.setNumThreads(0)
P_NUM = sys.argv[2] if len(sys.argv) > 2 else "6"
B = os.environ.get("MOCKUP_BASE", os.path.dirname(os.path.abspath(__file__)))
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")
T_F = "/storage/emulated/0/.mockup_time_tracker.txt"
M_P, S_P = os.path.join(B, "mockup.png"), os.path.join(B, "shape.png")

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
    if len(img.shape)==2: img=cv2.cvtColor(img,cv2.COLOR_GRAY2BGRA)
    elif img.shape[2]==3: img=np.concatenate([img, np.full((img.shape[0],img.shape[1],1),255,dtype=np.uint8)],axis=2)
    return img

if len(sys.argv) < 2: sys.exit()
action = sys.argv[1]

if action == 'process':
    display_header()
    os.makedirs(O, exist_ok=True); os.makedirs(R, exist_ok=True)
    
    imgs = [f for f in os.listdir(I) if f.lower().endswith(('.jpg','.jpeg','.png','.webp','.avif','.heic')) and not f.startswith('.') and "_temp" not in f]
    imgs.sort(key=lambda f: os.path.getctime(os.path.join(I, f)))
    if len(imgs) == 0:
        print(f" {C2}✖ Koi tasveer nahi mili.{ENDC}")
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
    
    if not os.path.exists(M_P) or not os.path.exists(S_P):
        print(f" {C2}✖ Mockup files missing!{ENDC}")
        sys.exit()
    
    def _setup_mockup():
        mk, sh = load_rgba(M_P), load_rgba(S_P)
        hm, wm = mk.shape[:2]
        sh = cv2.resize(sh, (wm, hm))
        mk_mask = (sh[:,:,3] > 0).astype(np.uint8) * 255
        cnts, _ = cv2.findContours(mk_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        c = max(cnts, key=cv2.contourArea)
        d = order_points(cv2.boxPoints(cv2.minAreaRect(c)).astype(np.float32))
        x, y, sw, sh_box = cv2.boundingRect(c)
        pm = cv2.getPerspectiveTransform(np.float32([[0,0],[sw,0],[sw,sh_box],[0,sh_box]]), d)
        mb, ma = mk[:,:,:3].astype(np.float32), mk[:,:,3].astype(np.float32)/255.0
        return hm, wm, mk_mask, pm, (sw, sh_box), mb, ma
    
    _result = {}
    _t = threading.Thread(target=lambda: _result.update(data=_setup_mockup()))
    _t.start()
    _frames = ['•', 'o', 'O', 'o']
    _i = 0
    while _t.is_alive():
        sys.stdout.write(f"\r {C1}✿{ENDC} {C5}{_frames[_i % 4]}{ENDC} Loading mockup engine...")
        sys.stdout.flush()
        time.sleep(0.08)
        _i += 1
    _t.join()
    sys.stdout.write(f"\r {C4}✔{ENDC} Mockup engine ready!            \n")
    H_M, W_M, mask, M, slot_size, m_bgr, m_a = _result['data']

    s_idx = get_max_seq([O, R, W_H]) + 1
    cur_t = time.time()
    if os.path.exists(T_F):
        try:
            with open(T_F,"r") as f: 
                lt=float(f.read().strip())
                if lt>cur_t: cur_t=lt
        except: pass

    pbar_format = f" {C1}{{desc}}{ENDC} {C5}|{ENDC}{{bar:20}}{C5}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"
    desc_text = "✨ HD Mode " if is_hd else "⚙️ Normal "

    def verify_saved(path):
        try:
            if not os.path.exists(path) or os.path.getsize(path) < 100: return False
            with Image.open(path) as im: im.verify()
            return True
        except: return False

    def process_one(f, idx, t_now):
        raw_p = os.path.join(I, f)
        w = safe_imread(raw_p)
        if w is None: return 'unreadable', idx, t_now
        
        w_exact = resize_exact(w) if crop_916 else w
        w_processed = apply_hd_enhance(w_exact) if is_hd else w_exact
        
        mock_path = os.path.join(O, f"{idx}.jpg")
        wall_path = os.path.join(R, f"{idx+1}.jpg")
        mt, wt = t_now+60, t_now+120
        
        # Shape ke exact aspect ratio ke mutabiq resize karega taake stretch na ho
        fit = resize_exact(w_processed, slot_size[0], slot_size[1])
        warped = cv2.warpPerspective(fit, M, (W_M, H_M), flags=cv2.INTER_LANCZOS4)
        wrp = cv2.bitwise_and(warped, warped, mask=mask).astype(np.float32)
        
        fin = np.zeros_like(m_bgr)
        for c in range(3): 
            fin[:,:,c] = ((1-m_a)*wrp[:,:,c] + m_a*m_bgr[:,:,c])
            
        save_optimized(np.clip(fin, 0, 255).astype(np.uint8), mock_path) 
        os.utime(mock_path, (mt, mt))
        
        save_optimized(w_processed, wall_path)
        os.utime(wall_path, (wt, wt))
        
        if not (verify_saved(mock_path) and verify_saved(wall_path)):
            raise IOError("save verify failed")
        
        safe_scan(mock_path)
        safe_scan(wall_path)
        gallery_copy_outputs([(mock_path, mt), (wall_path, wt)])
        os.remove(raw_p)
        
        del w, w_exact, w_processed, fit, warped, wrp, fin
        return 'ok', idx+2, wt

    processed_count = 0
    corrupt_count = 0
    skip_log = []
    set_wakelock(True)
    for i, f in enumerate(tqdm(imgs, desc=desc_text, bar_format=pbar_format)):
        success = False
        last_err = None
        for attempt in range(3):
            try:
                status, new_idx, new_t = process_one(f, s_idx, cur_t)
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
                for p in (os.path.join(O, f"{s_idx}.jpg"), os.path.join(R, f"{s_idx+1}.jpg")):
                    if os.path.exists(p):
                        try: os.remove(p)
                        except: pass
                gc.collect()
                time.sleep(0.3)
        
        if not success:
            corrupt_count += 1
            skip_log.append(f"{f} ({last_err})")
        
        if (i + 1) % 5 == 0: gc.collect()

    set_wakelock(False)

    if processed_count > 0:
        try:
            with open(T_F,"w") as f: f.write(str(cur_t))
        except: pass
        print(f"\n {C4}✔ {processed_count} Joray aesthetic style mein ready!{ENDC}")
    else: print(f"\n {C5}✔ Sab already set hai.{ENDC}")
    if corrupt_count > 0: print(f" {C2}⚠ {corrupt_count} file(s) skip ki gayin{ENDC}\n")

elif action == 'batch':
    display_header(); os.makedirs(W_H, exist_ok=True)
    mks = [f for f in os.listdir(O) if os.path.splitext(f)[0].isdigit()] if os.path.exists(O) else []
    if mks:
        bn = len([d for d in os.listdir(W_H) if os.path.isdir(os.path.join(W_H, d))]) + 1
        cb = os.path.join(W_H, f"P{P_NUM}_Batch_{bn}")
        os.makedirs(cb, exist_ok=True)
        set_wakelock(True)
        for f in sorted([f for f in os.listdir(O) if os.path.splitext(f)[0].isdigit()], key=lambda x: int(os.path.splitext(x)[0])): shutil.move(os.path.join(O, f), cb)
        for f in sorted([f for f in os.listdir(R) if os.path.splitext(f)[0].isdigit()], key=lambda x: int(os.path.splitext(x)[0])): shutil.move(os.path.join(R, f), cb)
        safe_scan_dir(cb); set_wakelock(False)
        animated_spinner("Packing Batch", 0.6)
        print(f" {C4}📦 {os.path.basename(cb)} Warehouse mein chala gaya!{ENDC}\n")
    else: print(f"\n {C2}✖ Batch banane ke liye data nahi hai.{ENDC}\n")

elif action == 'del':
    display_header()
    img_exts = ('.jpg', '.jpeg', '.png', '.webp', '.heic', '.avif')
    del_pbar_format = "{l_bar}%s{bar}%s{r_bar}" % (HR, ENDC)
    def count_imgs(folder):
        if not os.path.exists(folder): return 0
        return len([f for f in os.listdir(folder) if not f.startswith('.') and f.lower().endswith(img_exts)])
    i_count, o_count, r_count = count_imgs(I), count_imgs(O), count_imgs(R)
    wh_count = len([d for d in os.listdir(W_H) if "Batch_" in d]) if os.path.exists(W_H) else 0
    print(f"\n {HG2}╔{'═'*54}╗{ENDC}\n {HG2}║{ENDC}  {HR}{BOLD}🗑️   D E L E T E   M E N U{ENDC}{' '*23}{HG2}║{ENDC}\n {HG2}╚{'═'*54}╝{ENDC}")
    print(f" {HC}[1]{ENDC} 🖼️  Tayaar Maal (Mock+Wall) {HY}→ {o_count + r_count} file(s){ENDC} {HG}[DEFAULT]{ENDC}")
    print(f" {HC}[2]{ENDC} 📥 Input Images          {HY}→ {i_count} file(s){ENDC}")
    print(f" {HC}[3]{ENDC} 📦 Warehouse (Batches)    {HY}→ {wh_count} batch(es){ENDC}\n {HC}[0]{ENDC} Cancel\n {HG2}⚠ Ye action wapas nahi ho sakta.{ENDC}")
    choice = input(f"\n {HG}➜ {ENDC}").strip()
    if choice == '': choice = '1'
    def perm_del_images(folder, label):
        files = [f for f in os.listdir(folder) if not f.startswith('.') and f.lower().endswith(img_exts)] if os.path.exists(folder) else []
        if not files: print(f" {HG2}✖ {label}: koi file nahi mili.{ENDC}"); return 0
        c = 0
        for f in tqdm(files, desc=f" 🗑️  {label}", bar_format=del_pbar_format):
            try: os.remove(os.path.join(folder, f)); c += 1
            except: pass
        safe_scan_dir(folder); return c
    def delete_folder_with_progress(path, label):
        all_files = [os.path.join(r, f) for r, d, fs in os.walk(path) for f in fs]
        for f in tqdm(all_files, desc=f" 🗑️  {label}", bar_format=del_pbar_format):
            try: os.remove(f)
            except: pass
        safe_scan_dir(path); shutil.rmtree(path, ignore_errors=True)
    set_wakelock(True)
    if choice == '1': print(f"\n {HG}✔ {perm_del_images(O, 'Mockups')+perm_del_images(R, 'Wallpapers')}/{o_count+r_count} Tayaar Images Deleted.{ENDC}")
    elif choice == '2': print(f"\n {HG}✔ {perm_del_images(I, 'Input')}/{i_count} Input Images Deleted.{ENDC}")
    elif choice == '3':
        if not os.path.exists(W_H) or wh_count == 0: print(f"\n {HG2}✖ Warehouse khali hai!{ENDC}"); set_wakelock(False); sys.exit()
        batches = sorted([d for d in os.listdir(W_H) if "Batch_" in d])
        for i, b in enumerate(batches): print(f" {HC}[{i+1}]{ENDC} {b}  {HY}→ {len([f for f in os.listdir(os.path.join(W_H, b)) if f.lower().endswith(img_exts)])} file(s){ENDC}")
        sub_choice = input(f"\n {HG}Choose Batch or 'A' (All): {ENDC}")
        if sub_choice.lower() == 'a': delete_folder_with_progress(W_H, "Warehouse (All)"); os.makedirs(W_H); print(f"\n {HG}✔ Warehouse saaf ho gaya.{ENDC}")
        elif sub_choice.isdigit() and 1 <= int(sub_choice) <= len(batches):
            target_name = batches[int(sub_choice)-1]; delete_folder_with_progress(os.path.join(W_H, target_name), target_name); print(f"\n {HG}✔ Deleted.{ENDC}")
    set_wakelock(False)
