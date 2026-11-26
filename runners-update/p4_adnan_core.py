import os, sys, time, shutil, subprocess, random, gc
from PIL import Image, ImageFile
ImageFile.LOAD_TRUNCATED_IMAGES = True  # Halki truncated files ko galat "corrupt" na samjhe
import PIL.ImageCms as cms
from tqdm import tqdm
import cv2, numpy as np

# --- PREMIUM NEON COLORS ---
C1 = '\033[38;5;213m' 
C2 = '\033[91m'       
C3 = '\033[93m'       
C4 = '\033[92m'       
C5 = '\033[96m'       
C6 = '\033[94m'       
ENDC = '\033[0m'
BOLD = '\033[1m'

# --- DYNAMIC QUOTES ---
QUOTES = [
    "Create your own sunshine.",
    "Small steps every day.",
    "Keep going. Keep growing.",
    "Aesthetic is not just a look, it's a vibe.",
    "Transform reality with code.",
    "Success is a series of small wins."
]

def display_header():
    os.system('clear')
    quote = random.choice(QUOTES)
    
    print(f" {C1}✿{C5} ❀{C3} ❁{C6} ════════════════════════════════════════{C3} ❁{C5} ❀{C1} ✿ {ENDC}")
    
    ascii_art = [
        r"   █████╗ ██████╗ ███╗   ██╗█████╗ ███╗   ██╗",
        r"  ██╔══██╗██╔══██╗████╗  ██║██╔══██╗████╗  ██║",
        r"  ███████║██║  ██║██╔██╗ ██║███████║██╔██╗ ██║",
        r"  ██╔══██║██║  ██║██║╚██╗██║██╔══██║██║╚██╗██║",
        r"  ██║  ██║██████╔╝██║ ╚████║██║  ██║██║ ╚████║",
        r"  ╚═╝  ╚═╝╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═══╝╚═╝  ╚═══╝"
    ]
    grad_colors = [
        '\033[38;5;51m', '\033[38;5;45m', '\033[38;5;39m', 
        '\033[38;5;33m', '\033[38;5;129m', '\033[38;5;135m'
    ]
    
    for i, line in enumerate(ascii_art):
        print(f"{grad_colors[i]}{BOLD}{line}{ENDC}")
    
    print(f" {C5}           W e l c o m e  t o  t h e  S y s t e m{ENDC}")
    
    print(f" {C6}╔═════════════════════════════════════════════════════╗ {ENDC}")
    print(f" {C6}║{ENDC} {C3}✦ {quote:<43}{C6}║{ENDC}")
    print(f" {C6}║{ENDC} {C3}✦ Developer : {ENDC}{C5}Adnan Hassan                          {C6}║{ENDC}")
    print(f" {C6}║{ENDC} {C3}✦ Layout    : {ENDC}{C5}Mehar Adnan                           {C6}║{ENDC}")
    print(f" {C6}║{ENDC} {C3}✦ Support   : {ENDC}{C5}+92 343 6873 471                      {C6}║{ENDC}")
    print(f" {C6}╚═════════════════════════════════════════════════════╝ {ENDC}\n")

def animated_spinner(text, duration=0.8):
    frames = ['[■□□□□]', '[□■□□□]', '[□□■□□]', '[□□□■□]', '[□□□□■]', '[□□□■□]', '[□□■□□]', '[□■□□□]']
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

def fast_bulk_scan(paths):
    if not paths: return
    if isinstance(paths, str): paths = [paths]
    for i in range(0, len(paths), 50):
        try:
            subprocess.Popen(["termux-media-scan"] + paths[i:i+50], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
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

def ghost_buster(paths):
    if isinstance(paths, str): paths = [paths]
    for p in paths:
        if os.path.exists(p):
            try:
                with open(p, 'w') as f: f.write("TRASH")
                os.remove(p)
            except: pass
    fast_bulk_scan(paths)

def safe_imread(path):
    # PIL se pehle load/verify karo (corrupt/unsupported file par crash nahi, sirf None milega)
    try:
        with Image.open(path) as im:
            im.load()
            rgb = im.convert('RGB')
        return np.array(rgb)[:, :, ::-1].copy()  # RGB -> BGR (cv2 format)
    except Exception:
        return None

def verify_saved(path):
    try:
        if not os.path.exists(path) or os.path.getsize(path) < 100: return False
        with Image.open(path) as im: im.verify()
        return True
    except Exception:
        return False

# Limited threads for zero lag (Prevents CPU from bottlenecking)
cv2.setNumThreads(4)

# --- PROJECT 4 STRICT SETTINGS ---
P_NUM = "4"
B = os.environ.get("MOCKUP_BASE", os.path.dirname(os.path.abspath(__file__)))
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")

# 🔥 GLOBAL SHARED TIME TRACKER 
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

action = sys.argv[1].lower() if len(sys.argv) > 1 else 'w4'

if action in ['process', 'w4']:
    display_header()
    if not os.path.exists(B):
        print(f" {C2}✖ Error: Folder not found -> {B}{ENDC}\n")
        sys.exit()

    os.makedirs(O, exist_ok=True); os.makedirs(R, exist_ok=True)
    
    imgs = [f for f in os.listdir(I) if f.lower().endswith(('.jpg','.jpeg','.png','.webp','.avif','.heic')) and not f.startswith('.') and "_temp" not in f]
    if len(imgs) == 0:
        print(f" {C2}✖ Images not found in {P_NUM}_wallpapers.{ENDC}\n")
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
    
    if not os.path.exists(M_P) or not os.path.exists(S_P):
        print(f" {C2}✖ mockup.png ya shape.png missing in {P_NUM} folder!{ENDC}\n")
        sys.exit()
        
    mockup, shape = load_rgba(M_P), load_rgba(S_P)
    H_M, W_M = mockup.shape[:2]
    shape = cv2.resize(shape,(W_M,H_M))
    mask = (shape[:,:,3]>0).astype(np.uint8)*255
    cnts, _ = cv2.findContours(mask,cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE)
    
    if not cnts:
        print(f" {C2}✖ Error: shape.png me mask area nahi mila!{ENDC}\n")
        sys.exit()
        
    dst = order_points(cv2.boxPoints(cv2.minAreaRect(max(cnts,key=cv2.contourArea))).astype(np.float32))
    M = cv2.getPerspectiveTransform(np.float32([[0,0],[W_M,0],[W_M,H_M],[0,H_M]]),dst)
    m_bgr, m_a = mockup[:,:,:3].astype(np.float32), mockup[:,:,3].astype(np.float32)/255.0

    s_idx = get_max_seq([O, R, W_H]) + 1
    
    # --- GLOBAL TIME SEQUENCE SYNC ---
    cur_t = time.time()
    if os.path.exists(T_F):
        try:
            with open(T_F,"r") as f: 
                lt = float(f.read().strip())
                if lt > cur_t: 
                    cur_t = lt
        except: pass

    print("") 
    pbar_format = f" {C5}{{desc}}{ENDC} {C1}|{ENDC}{{bar:20}}{C1}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"
    desc_text = "✨ HD Engine " if is_hd else "⚡ Fast Engine "

    processed_count = 0
    corrupt_count = 0
    skip_log = []

    def process_one(f, idx, t_now):
        raw_p = os.path.join(I, f)
        w = safe_imread(raw_p)
        if w is None:
            return 'unreadable', idx, t_now, []

        w_exact = resize_exact(w) if crop_916 else w
        w_processed = apply_hd_enhance(w_exact) if is_hd else w_exact
        w_resized_mock = cv2.resize(w_processed, (W_M,H_M))
        wrp = cv2.bitwise_and(cv2.warpPerspective(w_resized_mock,M,(W_M,H_M)),cv2.warpPerspective(w_resized_mock,M,(W_M,H_M)),mask=mask).astype(np.float32)
        fin = np.zeros_like(m_bgr)
        for c in range(3): fin[:,:,c] = ((1-m_a)*wrp[:,:,c]+m_a*m_bgr[:,:,c])

        mock_path = os.path.join(O, f"{idx}.jpg")
        wall_path = os.path.join(R, f"{idx+1}.jpg")
        mt, wt = t_now+60, t_now+120

        save_optimized(np.clip(fin, 0, 255).astype(np.uint8), mock_path)
        os.utime(mock_path, (mt, mt))

        save_optimized(w_processed, wall_path)
        os.utime(wall_path, (wt, wt))

        if not (verify_saved(mock_path) and verify_saved(wall_path)):
            raise IOError("save verify failed")

        # Original tarteeb: har image ke baad foran scan, pehle mock phir wall, phir agli image
        safe_scan(mock_path)
        safe_scan(wall_path)
        gallery_copy_outputs([(mock_path, mt), (wall_path, wt)])

        raw_p_trash = raw_p
        try:
            with open(raw_p_trash, 'w') as trash_file: trash_file.write("TRASH")
            os.remove(raw_p_trash)
        except: pass

        del w, w_exact, w_processed, w_resized_mock, wrp, fin
        return 'ok', idx+2, wt, [mock_path, wall_path]

    set_wakelock(True)
    try:
        for i, f in enumerate(tqdm(imgs, desc=desc_text, bar_format=pbar_format)):
            success = False
            last_err = None
            for attempt in range(3):
                try:
                    status, new_idx, new_t, saved_paths = process_one(f, s_idx, cur_t)
                    if status == 'ok':
                        s_idx, cur_t = new_idx, new_t
                        processed_count += 1
                        success = True
                        break
                    else:
                        last_err = "padhi nahi gayi"
                        break  # unreadable file - retry se faida nahi
                except Exception as e:
                    last_err = f"{type(e).__name__}: {str(e)[:50]}"
                    # Retry se pehle is attempt ki adhoori files saaf karo
                    for p in (os.path.join(O, f"{s_idx}.jpg"), os.path.join(R, f"{s_idx+1}.jpg")):
                        if os.path.exists(p):
                            try: os.remove(p)
                            except: pass
                    time.sleep(0.3)

            if not success:
                corrupt_count += 1
                skip_log.append(f"{f} ({last_err})")

            if (i + 1) % 5 == 0: gc.collect()

        if processed_count > 0:
            try:
                with open(T_F,"w") as f: f.write(str(cur_t))
            except: pass
            animated_spinner("Syncing Gallery", 0.5)
            print(f" {C4}✔ {processed_count} Processed seamlessly!{ENDC}\n")
        else:
            print(f"\n {C5}✔ Already processed.{ENDC}\n")

        if corrupt_count > 0:
            print(f" {C2}⚠ {corrupt_count} file(s) skip ki gayin (dobara retry hongi agle run me):{ENDC}")
            for s in skip_log[:8]:
                print(f"   {C2}- {s}{ENDC}")
            if len(skip_log) > 8:
                print(f"   {C2}...aur {len(skip_log)-8} more{ENDC}")
            print("")
    finally:
        set_wakelock(False)

elif action in ['batch', 'b4']:
    display_header()
    os.makedirs(W_H, exist_ok=True)
    mks = [f for f in os.listdir(O) if os.path.splitext(f)[0].isdigit()] if os.path.exists(O) else []
    if mks:
        bn = len([d for d in os.listdir(W_H) if os.path.isdir(os.path.join(W_H, d))]) + 1
        cb = os.path.join(W_H, f"P{P_NUM}_Batch_{bn}")
        os.makedirs(cb, exist_ok=True)
        
        set_wakelock(True)
        try:
            animated_spinner("Packing Batch", 0.6)
            scans_to_run = []
            for d_path in [O, R]:
                if os.path.exists(d_path):
                    for f in os.listdir(d_path):
                        if os.path.splitext(f)[0].isdigit():
                            src = os.path.join(d_path, f)
                            dst = os.path.join(cb, f)
                            shutil.copy2(src, dst)
                            scans_to_run.append(dst)
                            scans_to_run.append(src)
                            try:
                                with open(src, 'w') as trash_file: trash_file.write("TRASH")
                                os.remove(src)
                            except: pass
            fast_bulk_scan(scans_to_run)
            print(f" {C4}📦 {os.path.basename(cb)} Ready for upload!{ENDC}\n")
        finally:
            set_wakelock(False)
    else:
        print(f"\n {C2}✖ No data for batch.{ENDC}\n")

elif action in ['del', 'd4']:
    display_header()
    print(f" {C1}✿ ❀ ❁ ═════════════════════════════════ ❁ ❀ ✿{ENDC}")
    print(f"              {BOLD}{C2}🗑️  D E L E T E  M E N U{ENDC}   ")
    print(f" {C1}✿ ❀ ❁ ═════════════════════════════════ ❁ ❀ ✿{ENDC}")
    print(f" {C5}[1]{ENDC} Tayaar Wallpaper (Images Only) - {C4}[DEFAULT]{ENDC}")
    print(f" {C5}[2]{ENDC} Input (Images Only)")
    print(f" {C5}[3]{ENDC} Warehouse (Ready_Upload_Batches)")
    print(f" {C5}[0]{ENDC} Cancel")
    
    choice = input(f"\n {C4}➜ {ENDC}").strip()
    
    if choice == '':
        choice = '1'
        
    img_exts = ('.jpg', '.jpeg', '.png', '.webp', '.heic', '.avif')
    
    def perm_del_images(folder):
        dels = []
        if os.path.exists(folder):
            for f in os.listdir(folder):
                if not f.startswith('.') and f.lower().endswith(img_exts):
                    dels.append(os.path.join(folder, f))
        ghost_buster(dels)
        return len(dels)

    set_wakelock(True)
    try:
        if choice == '1': 
            animated_spinner("Deleting Tayaar Wallpaper", 1.0)
            print(f"\n {C4}✔ {perm_del_images(O) + perm_del_images(R)} Tayaar Images Deleted.{ENDC}")
        elif choice == '2': 
            animated_spinner("Deleting Input Images", 1.0)
            print(f"\n {C4}✔ {perm_del_images(I)} Input Images Deleted.{ENDC}")
        elif choice == '3':
            if not os.path.exists(W_H): 
                print(f"\n {C2}✖ Warehouse empty!{ENDC}")
                set_wakelock(False)
                sys.exit()
            batches = sorted([d for d in os.listdir(W_H) if "Batch_" in d])
            for i, b in enumerate(batches): print(f" {C5}[{i+1}]{ENDC} Delete {b}")
            sub_choice = input(f"\n {C4}Choose Batch or 'A' (All): {ENDC}")
            
            if sub_choice.lower() == 'a':
                animated_spinner("Cleaning Warehouse", 1.5)
                dels = []
                for r,d,fs in os.walk(W_H):
                    for f in fs:
                        if not f.startswith('.'): dels.append(os.path.join(r,f))
                ghost_buster(dels)
                shutil.rmtree(W_H)
                os.makedirs(W_H)
                print(f"\n {C4}✔ Warehouse cleaned.{ENDC}")
            elif sub_choice.isdigit() and 1 <= int(sub_choice) <= len(batches):
                target = os.path.join(W_H, batches[int(sub_choice)-1])
                animated_spinner("Destroying Batch", 1.0)
                dels = []
                for r,d,fs in os.walk(target):
                    for f in fs:
                        if not f.startswith('.'): dels.append(os.path.join(r,f))
                ghost_buster(dels)
                shutil.rmtree(target)
                print(f"\n {C4}✔ Deleted.{ENDC}")
    finally:
        set_wakelock(False)
