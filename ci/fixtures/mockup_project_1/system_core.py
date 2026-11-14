import os, sys, time, shutil, random, gc
from PIL import Image
import PIL.ImageCms as cms
from tqdm import tqdm
import cv2, numpy as np

C1 = '\033[38;5;213m'; C2 = '\033[91m'; C3 = '\033[93m'
C4 = '\033[92m'; C5 = '\033[96m'; C6 = '\033[94m'
ENDC = '\033[0m'; BOLD = '\033[1m'

QUOTES = ["Create your own sunshine.", "Small steps every day.", "Keep going. Keep growing.", "Transform reality with code."]

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
    grad_colors = ['\033[38;5;51m', '\033[38;5;45m', '\033[38;5;39m', '\033[38;5;33m', '\033[38;5;129m', '\033[38;5;135m']
    for i, line in enumerate(ascii_art): print(f"{grad_colors[i]}{BOLD}{line}{ENDC}")
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
        time.sleep(0.08)
    sys.stdout.write(f"\r {C4}✔{ENDC} {text} Completed!       \n")

def set_wakelock(enable=True):
    try:
        if enable: os.system("termux-wake-lock >/dev/null 2>&1")
        else: os.system("termux-wake-unlock >/dev/null 2>&1")
    except: pass

def scan_media(path):
    try:
        # Native Android Broadcast (Zero Termux:API Errors, Zero Hangs)
        os.system(f'am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://{path}" >/dev/null 2>&1')
    except: pass

if len(sys.argv) < 3:
    base_file = os.path.basename(sys.argv[0])
    guess_num = ''.join(filter(str.isdigit, base_file))
    P_NUM = guess_num if guess_num else "Unknown"
else: P_NUM = sys.argv[2]

cv2.setNumThreads(0)
B = f"/storage/emulated/0/mockup_project_{P_NUM}"
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
    except: pil_img.save(output_path, format='JPEG', quality=87, subsampling=0, optimize=True)

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
    if not os.path.exists(B): print(f" {C2}✖ Error: Folder not found -> {B}{ENDC}\n"); sys.exit()
    os.makedirs(O, exist_ok=True); os.makedirs(R, exist_ok=True)
    imgs = [f for f in os.listdir(I) if f.lower().endswith(('.jpg','.jpeg','.png','.webp','.avif','.heic')) and not f.startswith('.') and "_temp" not in f]
    if len(imgs) == 0: print(f" {C2}✖ Images not found in {P_NUM}_wallpapers.{ENDC}\n"); sys.exit()

    print(f" {C5}✿ {BOLD}S e l e c t  M o d e{ENDC}")
    print(f" {C1}[1]{ENDC} HD Mode (AI Enhance) - {C4}[DEFAULT]{ENDC}")
    print(f" {C1}[2]{ENDC} Normal (Fast KB Pack)")
    choice = input(f"\n {C4}➜ {ENDC}").strip()
    is_hd = (choice != '2')
    
    if not os.path.exists(M_P) or not os.path.exists(S_P): print(f" {C2}✖ mockup.png ya shape.png missing!{ENDC}\n"); sys.exit()
    mockup, shape = load_rgba(M_P), load_rgba(S_P)
    H_M, W_M = mockup.shape[:2]
    shape = cv2.resize(shape,(W_M,H_M))
    mask = (shape[:,:,3]>0).astype(np.uint8)*255
    cnts, _ = cv2.findContours(mask,cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE)
    if not cnts: print(f" {C2}✖ Error: shape.png mask failed!{ENDC}\n"); sys.exit()
    dst = order_points(cv2.boxPoints(cv2.minAreaRect(max(cnts,key=cv2.contourArea))).astype(np.float32))
    
    if P_NUM == "6": x, y, w_box, h_box = cv2.boundingRect(mask)
    else: M = cv2.getPerspectiveTransform(np.float32([[0,0],[W_M,0],[W_M,H_M],[0,H_M]]),dst)
    m_bgr, m_a = mockup[:,:,:3].astype(np.float32), mockup[:,:,3].astype(np.float32)/255.0

    s_idx = get_max_seq([O, R, W_H]) + 1
    cur_t = time.time()
    if os.path.exists(T_F):
        try:
            with open(T_F,"r") as f: 
                lt=float(f.read().strip())
                if lt > cur_t: cur_t = lt
        except: pass

    print("") 
    pbar_format = f" {C5}{{desc}}{ENDC} {C1}|{ENDC}{{bar:20}}{C1}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"
    desc_text = "✨ HD Engine " if is_hd else "⚡ Safe Engine "
    processed_count = 0
    
    set_wakelock(True)
    try:
        for f in tqdm(imgs, desc=desc_text, bar_format=pbar_format):
            try:
                raw_p = os.path.join(I, f); w = cv2.imread(raw_p)
                if w is None: continue
                if P_NUM == "6":
                    if is_hd: w_processed = apply_hd_enhance(w)
                    else: w_processed = w
                    h_w, w_w = w_processed.shape[:2]
                    s = max(w_box / w_w, h_box / h_w)
                    w_scaled = cv2.resize(w_processed, (int(s * w_w), int(s * h_w)), interpolation=cv2.INTER_LANCZOS4)
                    prepped_wall = w_scaled[0:h_box, 0:w_box, :]
                    canvas = np.zeros((H_M, W_M, 3), dtype=np.float32)
                    canvas[y:y+h_box, x:x+w_box] = prepped_wall.astype(np.float32)
                    shape_mask_norm = (mask / 255.0).reshape(H_M, W_M, 1); canvas = canvas * shape_mask_norm
                    fin = canvas * (1 - m_a) + m_bgr * m_a
                    if (w_w / h_w) > (1080/1920):
                        new_w = int(h_w * (1080/1920)); w_crop_r = w_processed[:, (w_w-new_w)//2:(w_w+new_w)//2, :]
                    else:
                        new_h = int(w_w / (1080/1920)); w_crop_r = w_processed[(h_w-new_h)//2:(h_w+new_h)//2, :, :]
                    w_processed = cv2.resize(w_crop_r, (1080,1920), interpolation=cv2.INTER_LANCZOS4)
                else:
                    w_exact = resize_exact(w)
                    w_processed = apply_hd_enhance(w_exact) if is_hd else w_exact
                    w_resized_mock = cv2.resize(w_processed, (W_M,H_M))
                    wrp = cv2.bitwise_and(cv2.warpPerspective(w_resized_mock,M,(W_M,H_M)),cv2.warpPerspective(w_resized_mock,M,(W_M,H_M)),mask=mask).astype(np.float32)
                    fin = np.zeros_like(m_bgr)
                    for c in range(3): fin[:,:,c] = ((1-m_a)*wrp[:,:,c]+m_a*m_bgr[:,:,c])
                
                mock_path = os.path.join(O, f"{s_idx}.jpg")
                wall_path = os.path.join(R, f"{s_idx+1}.jpg")
                
                mt, wt = cur_t + 60, cur_t + 120
                
                save_optimized(np.clip(fin, 0, 255).astype(np.uint8), mock_path); os.utime(mock_path, (mt, mt))
                scan_media(mock_path)
                
                save_optimized(w_processed, wall_path); os.utime(wall_path, (wt, wt))
                scan_media(wall_path)

                try:
                    tmp_raw = raw_p + ".del"
                    os.rename(raw_p, tmp_raw)
                    scan_media(raw_p)
                    os.remove(tmp_raw)
                except: pass

                s_idx += 2; cur_t = wt; processed_count += 1
                gc.collect()
            except Exception as e: pass
        if processed_count > 0:
            try:
                with open(T_F,"w") as f: f.write(str(cur_t))
            except: pass
            print(f" \n {C4}✔ {processed_count} Processed seamlessly!{ENDC}\n")
        else: print(f"\n {C5}✔ Already processed.{ENDC}\n")
    finally: set_wakelock(False)

elif action == 'batch':
    display_header()
    os.makedirs(W_H, exist_ok=True)
    mks = [f for f in os.listdir(O) if os.path.splitext(f)[0].isdigit()] if os.path.exists(O) else []
    if mks:
        bn = len([d for d in os.listdir(W_H) if os.path.isdir(os.path.join(W_H, d))]) + 1
        cb = os.path.join(W_H, f"P{P_NUM}_Batch_{bn}"); os.makedirs(cb, exist_ok=True)
        set_wakelock(True)
        try:
            animated_spinner("Packing Batch", 0.6)
            for d_path in [O, R]:
                if os.path.exists(d_path):
                    for f in os.listdir(d_path):
                        if os.path.splitext(f)[0].isdigit():
                            src = os.path.join(d_path, f); dst = os.path.join(cb, f)
                            shutil.copy2(src, dst)
                            try:
                                tmp_src = src + ".del"
                                os.rename(src, tmp_src)
                                scan_media(src)
                                os.remove(tmp_src)
                            except: pass
            print(f" {C4}📦 {os.path.basename(cb)} Ready for upload!{ENDC}\n")
        finally: set_wakelock(False)
    else: print(f"\n {C2}✖ No data for batch.{ENDC}\n")

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
    if choice == '': choice = '1'
    
    img_exts = ('.jpg', '.jpeg', '.png', '.webp', '.heic', '.avif')
    
    def perm_del(folder):
        dels = []
        if not os.path.exists(folder): return 0
        for f in os.listdir(folder):
            if not f.startswith('.') and f.lower().endswith(img_exts):
                p = os.path.join(folder, f)
                tmp_p = p + ".del"
                try:
                    os.rename(p, tmp_p)
                    scan_media(p)
                    os.remove(tmp_p)
                    dels.append(p)
                except: pass
        return len(dels)
    
    set_wakelock(True)
    try:
        if choice == '1': 
            animated_spinner("Deleting Ready Wallpaper & Pic", 1.0)
            del_count = perm_del(O) + perm_del(R)
            print(f"\n {C4}✔ {del_count} Ready Wallpaper & Pic Permanently Deleted.{ENDC}")
        elif choice == '2': 
            animated_spinner("Deleting Input Images", 1.0)
            del_count = perm_del(I)
            print(f"\n {C4}✔ {del_count} Input Images Permanently Deleted.{ENDC}")
        elif choice == '3':
            if not os.path.exists(W_H): print(f"\n {C2}✖ Warehouse empty!{ENDC}"); set_wakelock(False); sys.exit()
            batches = sorted([d for d in os.listdir(W_H) if "Batch_" in d])
            for i, b in enumerate(batches): print(f" {C5}[{i+1}]{ENDC} Delete {b}")
            sub_choice = input(f"\n {C4}Choose Batch or 'A' (All): {ENDC}")
            if sub_choice.lower() == 'a':
                animated_spinner("Cleaning Warehouse", 1.5)
                for r,d,fs in os.walk(W_H):
                    for f in fs:
                        if not f.startswith('.'): perm_del(r)
                try: shutil.rmtree(W_H); os.makedirs(W_H)
                except: pass
                print(f"\n {C4}✔ Warehouse cleaned.{ENDC}")
            elif sub_choice.isdigit() and 1 <= int(sub_choice) <= len(batches):
                target = os.path.join(W_H, batches[int(sub_choice)-1]); animated_spinner("Destroying Batch", 1.0)
                for r,d,fs in os.walk(target):
                    for f in fs:
                        if not f.startswith('.'): perm_del(r)
                try: shutil.rmtree(target)
                except: pass
                print(f"\n {C4}✔ Deleted.{ENDC}")
    finally: set_wakelock(False)
