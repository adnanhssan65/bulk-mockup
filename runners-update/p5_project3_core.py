import os, sys, time, shutil, random, gc, subprocess
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
    print(f" {C5}           V i c k y   D a t a B a s e{ENDC}")
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
    try:
        # Fire-and-forget: app ke session-less process me ye command atak sakti hai - intezar nahi karna
        subprocess.Popen(["termux-wake-lock" if enable else "termux-wake-unlock"],
                         stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except: pass

def scan_media(path, timeout_sec=5):
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
            scan_media(d)
    except Exception:
        pass

if len(sys.argv) < 3:
    base_file = os.path.basename(sys.argv[0])
    guess_num = ''.join(filter(str.isdigit, base_file))
    P_NUM = guess_num if guess_num else "3"
else: P_NUM = sys.argv[2]

cv2.setNumThreads(0)
B = os.environ.get("MOCKUP_BASE", os.path.dirname(os.path.abspath(__file__)))
I = os.path.join(B, f"{P_NUM}_wallpapers")
O = os.path.join(B, f"{P_NUM}_Mockups")
R = os.path.join(B, f"{P_NUM}_Ready_Wallpapers")
W_H = os.path.join(B, "Ready_Upload_Batches")
T_F = "/storage/emulated/0/.mockup_time_tracker.txt"

M_W_P = os.path.join(B, "mockup_white.png")
M_B_P = os.path.join(B, "mockup_black.png")
S_P = os.path.join(B, "shape.png")

def get_max_seq(dirs):
    m = 0
    for d in dirs:
        if os.path.exists(d):
            for r, sd, fs in os.walk(d):
                for f in fs:
                    n, _ = os.path.splitext(f)
                    if n.isdigit(): m = max(m, int(n))
    return m

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

    print(f" {C5}✿ {BOLD}S e l e c t  S i z e{ENDC}")
    print(f" {C1}[1]{ENDC} Original Size - {C4}[DEFAULT]{ENDC}")
    print(f" {C1}[2]{ENDC} Crop 9:16")

    size_choice = input(f"\n {C4}➜ {ENDC}").strip()
    crop_916 = (size_choice == '2')
    
    if not os.path.exists(M_W_P) or not os.path.exists(M_B_P) or not os.path.exists(S_P): 
        print(f" {C2}✖ mockup_white.png, mockup_black.png ya shape.png file missing hai!{ENDC}\n"); sys.exit()
        
    mock_w, mock_b = load_rgba(M_W_P), load_rgba(M_B_P)
    H_M, W_M = mock_w.shape[:2]
    
    shape_bgr = cv2.imread(S_P)
    shape_bgr = cv2.resize(shape_bgr, (W_M, H_M))
    shape_hsv = cv2.cvtColor(shape_bgr, cv2.COLOR_BGR2HSV)
    m1 = cv2.inRange(shape_hsv, np.array([0, 120, 70]), np.array([10, 255, 255]))
    m2 = cv2.inRange(shape_hsv, np.array([170, 120, 70]), np.array([180, 255, 255]))
    mask = m1 | m2
    
    # 6 Slots find karne ki logic
    cnts, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    cnts = [c for c in cnts if cv2.contourArea(c) > 1000] # Choti noise ignore karega
    
    if len(cnts) == 0: 
        print(f" {C2}✖ Error: shape.png mein koi proper red box (shape) nahi mila!{ENDC}\n"); sys.exit()
        
    # Shapes ko properly left-to-right aur top-to-bottom arrange karna
    bboxes = [cv2.boundingRect(c) for c in cnts]
    bboxes.sort(key=lambda b: b[1])
    rows, curr_row = [], [bboxes[0]]
    for b in bboxes[1:]:
        if abs(b[1] - curr_row[0][1]) < 150: curr_row.append(b)
        else:
            rows.append(sorted(curr_row, key=lambda x: x[0]))
            curr_row = [b]
    rows.append(sorted(curr_row, key=lambda x: x[0]))
    sorted_bboxes = [b for row in rows for b in row]
    
    slots_count = len(sorted_bboxes)
    print(f" {C4}✔ MOCKUP LAYOUT DETECTED: {slots_count} Phone Slots per Mockup.{ENDC}\n")
    
    m_w_bgr = mock_w[:,:,:3].astype(np.float32); m_w_a = (mock_w[:,:,3].astype(np.float32)/255.0).reshape(H_M, W_M, 1)
    m_b_bgr = mock_b[:,:,:3].astype(np.float32); m_b_a = (mock_b[:,:,3].astype(np.float32)/255.0).reshape(H_M, W_M, 1)

    s_idx = get_max_seq([O, R, W_H]) + 1

    # --- SHARED TIME SEQUENCE SYNC (family pattern: sab projects ek hi history) ---
    cur_t = time.time()
    if os.path.exists(T_F):
        try:
            with open(T_F, "r") as fh:
                lt = float(fh.read().strip())
                if lt > cur_t: cur_t = lt
        except: pass

    # Chunking logic: 6 (ya jitne bhi slots hain) ka set banaye ga
    chunks = [imgs[i:i + slots_count] for i in range(0, len(imgs), slots_count)]

    pbar_format = f" {C5}{{desc}}{ENDC} {C1}|{ENDC}{{bar:20}}{C1}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}} Images{ENDC}"
    desc_text = "✨ Collage Engine " if is_hd else "⚡ Collage Engine "
    processed_count = 0
    
    set_wakelock(True)
    try:
        pbar = tqdm(total=len(imgs), desc=desc_text, bar_format=pbar_format)
        for chunk in chunks:
            if len(chunk) == 0: continue
            
            canvas = np.zeros((H_M, W_M, 3), dtype=np.float32)
            ready_walls_data = []
            total_brightness = 0
            
            for i, f in enumerate(chunk):
                if i >= slots_count: break
                raw_p = os.path.join(I, f)
                w = cv2.imread(raw_p)
                if w is None: continue
                
                w_processed = apply_hd_enhance(w) if is_hd else w
                gray_w = cv2.cvtColor(w_processed, cv2.COLOR_BGR2GRAY)
                total_brightness += np.mean(gray_w)
                
                h_w, w_w = w_processed.shape[:2]
                x, y, w_box, h_box = sorted_bboxes[i]
                
                # Image ko sirf apne specific slot ke hisab se crop aur scale karna
                scale = max(w_box / w_w, h_box / h_w)
                new_w, new_h = int(np.ceil(w_w * scale)), int(np.ceil(h_w * scale))
                w_scaled = cv2.resize(w_processed, (new_w, new_h), interpolation=cv2.INTER_LANCZOS4)
                
                start_y = (new_h - h_box) // 2
                start_x = (new_w - w_box) // 2
                prepped_wall = w_scaled[start_y:start_y+h_box, start_x:start_x+w_box, :]
                
                if prepped_wall.shape[:2] != (h_box, w_box):
                    prepped_wall = cv2.resize(prepped_wall, (w_box, h_box))
                    
                canvas[y:y+h_box, x:x+w_box] = prepped_wall.astype(np.float32)
                
                # Ready Wallpaper HD Size (1080x1920)
                if (w_w / h_w) > (1080/1920):
                    nw = int(h_w * (1080/1920))
                    w_crop_r = w_processed[:, (w_w-nw)//2:(w_w+nw)//2, :]
                else:
                    nh = int(w_w / (1080/1920))
                    w_crop_r = w_processed[(h_w-nh)//2:(h_w+nh)//2, :, :]
                w_ready_final = cv2.resize(w_crop_r, (1080,1920), interpolation=cv2.INTER_LANCZOS4) if crop_916 else w_processed
                
                ready_walls_data.append((raw_p, w_ready_final))

            # Auto brightness (Dark vs Light Layout)
            avg_bright = total_brightness / len(chunk) if len(chunk) > 0 else 255
            m_bgr, m_a = (m_w_bgr, m_w_a) if avg_bright < 127 else (m_b_bgr, m_b_a)

            # Master masking se corner rounding bilkul perfect apply hogi
            shape_mask_norm = (mask / 255.0).reshape(H_M, W_M, 1)
            canvas = canvas * shape_mask_norm
            fin = canvas * (1.0 - m_a) + m_bgr * m_a
            
            # Save Master Collage Mockup (1 picture for 6 walls)
            mock_path = os.path.join(O, f"{s_idx}.jpg")
            save_optimized(np.clip(fin, 0, 255).astype(np.uint8), mock_path)
            cur_t += 60
            os.utime(mock_path, (cur_t, cur_t))
            scan_media(mock_path)
            gal_items = [(mock_path, cur_t)]
            s_idx += 1
            
            # Save All Ready Wallpapers in sequence aur unki original images ko del karna
            for raw_p, w_ready_final in ready_walls_data:
                wall_path = os.path.join(R, f"{s_idx}.jpg")
                save_optimized(w_ready_final, wall_path)
                cur_t += 60
                os.utime(wall_path, (cur_t, cur_t))
                scan_media(wall_path)
                gal_items.append((wall_path, cur_t))
                s_idx += 1
                try:
                    tmp_raw = raw_p + ".del"
                    os.rename(raw_p, tmp_raw)
                    scan_media(raw_p)
                    os.remove(tmp_raw)
                except: pass

            gallery_copy_outputs(gal_items)

            processed_count += len(chunk)
            pbar.update(len(chunk))
            gc.collect()
            
        pbar.close()    
        if processed_count > 0:
            try:
                with open(T_F, "w") as fh: fh.write(str(cur_t))
            except: pass
            print(f" \n {C4}✔ {processed_count} Images processed dynamically!{ENDC}\n")
        else: print(f"\n {C5}✔ Processed 0 images.{ENDC}\n")
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
    
    def perm_del_progress(folders, desc_text):
        if isinstance(folders, str): folders = [folders]
        targets = []
        for folder in folders:
            if os.path.exists(folder):
                for r, d, fs in os.walk(folder):
                    for f in fs:
                        if not f.startswith('.') and f.lower().endswith(img_exts):
                            targets.append(os.path.join(r, f))
        
        total_files = len(targets)
        if total_files == 0:
            return 0
            
        pbar_format = f" {C5}{{desc}}{ENDC} {C1}|{ENDC}{{bar:20}}{C1}|{ENDC} {C4}{{n_fmt}}/{{total_fmt}}{ENDC}"
        
        for p in tqdm(targets, desc=desc_text, bar_format=pbar_format):
            tmp_p = p + ".del"
            try:
                os.rename(p, tmp_p)
                scan_media(p)
                os.remove(tmp_p)
            except: pass
            
        return total_files
    
    set_wakelock(True)
    try:
        if choice == '1': 
            print("")
            del_count = perm_del_progress([O, R], "🗑️ Deleting Ready")
            print(f"\n {C4}✔ {del_count} Ready Wallpaper & Pic Permanently Deleted.{ENDC}")
        elif choice == '2': 
            print("")
            del_count = perm_del_progress([I], "🗑️ Deleting Input")
            print(f"\n {C4}✔ {del_count} Input Images Permanently Deleted.{ENDC}")
        elif choice == '3':
            if not os.path.exists(W_H): print(f"\n {C2}✖ Warehouse empty!{ENDC}"); set_wakelock(False); sys.exit()
            batches = sorted([d for d in os.listdir(W_H) if "Batch_" in d])
            for i, b in enumerate(batches): print(f" {C5}[{i+1}]{ENDC} Delete {b}")
            sub_choice = input(f"\n {C4}Choose Batch or 'A' (All): {ENDC}")
            print("")
            if sub_choice.lower() == 'a':
                del_count = perm_del_progress([W_H], "🗑️ Cleaning Warehouse")
                try: shutil.rmtree(W_H); os.makedirs(W_H)
                except: pass
                print(f"\n {C4}✔ Warehouse cleaned. ({del_count} images deleted){ENDC}")
            elif sub_choice.isdigit() and 1 <= int(sub_choice) <= len(batches):
                target = os.path.join(W_H, batches[int(sub_choice)-1])
                del_count = perm_del_progress([target], "🗑️ Destroying Batch")
                try: shutil.rmtree(target)
                except: pass
                print(f"\n {C4}✔ Deleted. ({del_count} images removed){ENDC}")
    finally: set_wakelock(False)
