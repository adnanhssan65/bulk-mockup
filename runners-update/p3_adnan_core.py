import os, sys, time, shutil, subprocess, random
from PIL import Image
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

QUOTES = [
    "Create your own sunshine.",
    "Small steps every day.",
    "Keep going. Keep growing.",
    "Aesthetic is not just a look, it's a vibe.",
    "Transform reality with code."
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
        r"  ╚═╝  ╚═╝╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═╝╚═╝  ╚═══╝"
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
    print(f" {C6}║{ENDC} {C3}✦ Owner   : {ENDC}{C5}Mehar Adnan                           {C6}║{ENDC}")
    print(f" {C6}║{ENDC} {C3}✦ Support : {ENDC}{C5}+92 343 6873 471                      {C6}║{ENDC}")
    print(f" {C6}╚═════════════════════════════════════════════════════╝ {ENDC}\n")

def animated_spinner(text, duration=0.8):
    frames = ['[■□□□□]', '[□■□□□]', '[□□■□□]', '[□□□■□]', '[□□□□■]', '[□□□■□]', '[□□■□□]', '[□■□□□]']
    for i in range(int(duration * 10)):
        sys.stdout.write(f"\r {C1}✿{ENDC} {C5}{frames[i % len(frames)]}{ENDC} {text}...")
        sys.stdout.flush()
        time.sleep(0)
    sys.stdout.write(f"\r {C4}✔{ENDC} {text} Completed!       \n")

def set_wakelock(enable=True):
    # Fire-and-forget: app ke session-less process me ye command atak sakti hai - intezar nahi karna
    try:
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

def safe_scan(path, timeout_sec=5):
    try:
        subprocess.run(["termux-media-scan", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)
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

def ghost_buster(paths):
    if isinstance(paths, str): paths = [paths]
    for p in paths:
        if os.path.exists(p):
            try:
                with open(p, 'w') as f: f.write("TRASH")
                os.remove(p)
            except: pass
    fast_bulk_scan(paths)

cv2.setNumThreads(0)

P_NUM = sys.argv[2] if len(sys.argv) > 2 else "3"

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

if len(sys.argv) < 2: sys.exit()
action = sys.argv[1]

if action == 'process':
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
        print(f" {C2}✖ Mockup files missing!{ENDC}")
        sys.exit()
        
    mockup, shape = load_rgba(M_P), load_rgba(S_P)
    H_M, W_M = mockup.shape[:2]
    shape = cv2.resize(shape,(W_M,H_M))
    mask = (shape[:,:,3]>0).astype(np.uint8)*255
    cnts, _ = cv2.findContours(mask,cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE)
    if not cnts:
        sys.exit()
        
    dst = order_points(cv2.boxPoints(cv2.minAreaRect(max(cnts,key=cv2.contourArea))).astype(np.float32))
    M = cv2.getPerspectiveTransform(np.float32([[0,0],[W_M,0],[W_M,H_M],[0,H_M]]),dst)
    m_bgr, m_a = mockup[:,:,:3].astype(np.float32), mockup[:,:,3].astype(np.float32)/255.0

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
        # v155 Time Mode FUTURE — purana (pre-v154) tareeqa:
        # base = max(abhi, tracker), har file +60s; tracker me
        # aakhri waqt neeche wapas likha jata hai.
        _base = _now0
        if os.path.exists(T_F):
            try:
                with open(T_F,"r") as f:
                    _lt = float(f.read().strip())
                    if _lt > _base: _base = _lt
            except: pass
        t_step = 60.0
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
        _raw_step = (_end_soft - _floor) / _slots
        if _raw_step >= 2:
            t_step = min(_raw_step, 60.0)
            cur_t = _end_soft - t_step * _slots
        else:
            # Branch B: window tang hai — tracker ke baad min step se chalo
            t_step = 2.0
            cur_t = _floor

    print("") 
    
    pbar_format = f" {C5}{{desc}}{ENDC} {C1}|{ENDC}{{bar:20}}{C1}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"
    desc_text = "✨ HD Engine " if is_hd else "⚡ Fast Engine "

    processed_count = 0
    scans_to_run = []
    
    set_wakelock(True)
    try:
        for f in tqdm(imgs, desc=desc_text, bar_format=pbar_format):
            try:
                raw_p = os.path.join(I, f)
                w = cv2.imread(raw_p)
                if w is None: continue
                
                w_exact = resize_exact(w) if crop_916 else w
                w_processed = apply_hd_enhance(w_exact) if is_hd else w_exact
                
                mock_path = os.path.join(O, f"{s_idx}.jpg")
                wall_path = os.path.join(R, f"{s_idx+1}.jpg")
                mt, wt = cur_t + t_step, cur_t + 2*t_step
                
                w_resized_mock = cv2.resize(w_processed, (W_M,H_M))
                wrp = cv2.bitwise_and(cv2.warpPerspective(w_resized_mock,M,(W_M,H_M)),cv2.warpPerspective(w_resized_mock,M,(W_M,H_M)),mask=mask).astype(np.float32)
                fin = np.zeros_like(m_bgr)
                for c in range(3): fin[:,:,c] = ((1-m_a)*wrp[:,:,c]+m_a*m_bgr[:,:,c])
                
                save_optimized(np.clip(fin, 0, 255).astype(np.uint8), mock_path) 
                os.utime(mock_path, (mt, mt))
                safe_scan(mock_path)
                
                save_optimized(w_processed, wall_path)
                os.utime(wall_path, (wt, wt))
                safe_scan(wall_path)
                gallery_copy_outputs([(mock_path, mt), (wall_path, wt)])
                
                scans_to_run.append(raw_p)
                try:
                    with open(raw_p, 'w') as trash_file: trash_file.write("TRASH")
                    os.remove(raw_p)
                except: pass
                
                s_idx += 2
                cur_t = wt
                processed_count += 1
                
            except Exception as e: pass

        if processed_count > 0:
            try:
                if _tmode != "current":
                    with open(T_F, "w") as f: f.write(str(cur_t))
            except: pass
            animated_spinner("Syncing Gallery", 0.5)
            fast_bulk_scan(scans_to_run)
            print(f" {C4}✔ {processed_count} Joray aesthetic style mein ready!{ENDC}\n")
        else:
            print(f"\n {C5}✔ Sab already set hai.{ENDC}\n")
    finally:
        set_wakelock(False)

elif action == 'batch':
    display_header()
    os.makedirs(W_H, exist_ok=True)
    mks = [f for f in os.listdir(O) if os.path.splitext(f)[0].isdigit()] if os.path.exists(O) else []
    if mks:
        bn = len([d for d in os.listdir(W_H) if os.path.isdir(os.path.join(W_H, d))]) + 1
        cb = os.path.join(W_H, _batch_pick_name(f"P{P_NUM}_Batch_{bn}"))
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
            print(f" {C4}📦 {os.path.basename(cb)} Warehouse mein chala gaya!{ENDC}\n")
        finally:
            set_wakelock(False)
    else:
        print(f"\n {C2}✖ Batch banane ke liye data nahi hai.{ENDC}\n")

elif action == 'del':
    display_header()
    print(f" {C1}✿ ❀ ❁ ═════════════════════════════════ ❁ ❀ ✿{ENDC}")
    print(f"              {BOLD}{C2}🗑️  D E L E T E  M E N U{ENDC}   ")
    print(f" {C1}✿ ❀ ❁ ═════════════════════════════════ ❁ ❀ ✿{ENDC}")
    print(f" {C5}[1]{ENDC} Ready Wallpaper & Pic - {C4}[DEFAULT]{ENDC}")
    print(f" {C5}[2]{ENDC} Input (Images Only)")
    print(f" {C5}[3]{ENDC} Warehouse (Ready_Upload_Batches)")
    print(f" {C5}[0]{ENDC} Cancel")
    
    choice = input(f"\n {C4}➜ (Enter for Ready Wallpaper & Pic): {ENDC}").strip()
    
    if choice == '':
        choice = '1'
        
    img_exts = ('.jpg', '.jpeg', '.png', '.webp', '.heic', '.avif')
    
    def tm_pairs_cleanup(parity):
        # TermuxMod "Pairs" copies ki safai — $HOME delete ke sath wahan ki
        # copies bhi jayen: 'odd' = sirf mockups, 'even' = sirf wallpapers,
        # 'all' = dono. Project/Pairs folder na ho to khamoshi se skip.
        try:
            dp = os.path.join(TM_ROOT, f"Project {P_NUM}", "Pairs")
            if not os.path.isdir(dp): return
            dels = []
            for f in os.listdir(dp):
                if f.startswith('.') or not f.lower().endswith(img_exts): continue
                stem = os.path.splitext(f)[0]
                if not stem.isdigit(): continue
                n = int(stem)
                if parity == 'odd' and n % 2 == 0: continue
                if parity == 'even' and n % 2 == 1: continue
                dels.append(os.path.join(dp, f))
            if dels: ghost_buster(dels)
        except Exception:
            pass

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
            tm_pairs_cleanup('all')
        elif choice == '2': 
            animated_spinner("Deleting Input Images", 1.0)
            print(f"\n {C4}✔ {perm_del_images(I)} Input Images Deleted.{ENDC}")
        elif choice == '3':
            if not os.path.exists(W_H): 
                print(f"\n {C2}✖ Warehouse khali hai!{ENDC}")
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
                print(f"\n {C4}✔ Warehouse saaf ho gaya.{ENDC}")
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
