# Bulk Mockup — Owner Guide (asaan alfaaz, click-by-click)

Ye app asal **Termux** ka fork hai. Do khaas baatein:

1. Tumhare tamam packages (Python, OpenCV, waghera) **APK ke andar pehle se install** hain.
   Naye phone pe sirf app install karni hai — `pkg install` wala setup kabhi nahi karna.
2. Terminal ke drawer (left wala menu) me **PROJECTS**, **PERMISSIONS**,
   **CHECK FOR UPDATES** aur **ABOUT** ke buttons hain.

App ka package naam **com.termux** hi hai (asal Termux wala) — is liye:
- Packages ke tamam official paths bilkul sahi chalte hain, or **`pkg install`
  bhi bilkul asal Termux ki tarah kaam karta hai** (naye packages bhi lag sakte
  hain — waise sab zaroori cheezein pehle se andar hain: python, numpy, opencv,
  pillow, tqdm, nano, curl, wget, zip, unzip, dbus).
- **Lekin:** ek hi phone par **asal Termux or Bulk Mockup dono aik sath install
  NAHI ho sakti** (package naam aik, signature alag). Agar phone me asal Termux
  pehle se hai to Bulk Mockup lagane se pehle use **uninstall** karna hoga.
  Fikr na karo — mockup project folders (`/storage/emulated/0/mockup_project_N`)
  uninstall se delete NAHI hote; sirf asal Termux ki apni andar wali home
  saaf hoti hai, jis ki Bulk Mockup ko zaroorat hi nahi (wo apna sab kuch
  sath lati hai).

---

## STEP 1 — GitHub account or repo (ek dafa)

1. [github.com](https://github.com) par apne account se login karo (account na ho to
   **Sign up** se bana lo — bilkul free hai).
2. Upar right corner me **+** → **New repository**.
3. Repository name: `bulk-mockup` likho. **Public** select karo
   (update check bina login ke chalne ke liye repo public hona zaroori hai;
   repo me sirf app ka code hai — tumhare scripts or koi secret is me NAHI hai).
4. **Create repository** dabao.
5. Is repo ke files wahan upload karne hain — ye step asaan nahi hai (hazaron files
   hain), is liye **Muse se kehna "repo me code bhej do"** — wo tumhare login se
   khud bhej dega. (Ya kisi bhi computer se `git push` kar sakte ho.)

## STEP 2 — Signing key ke secrets (ek dafa) ⭐ SAB SE ZAROORI

App ki har release ek khaas "chaabi" (keystore) se sign hoti hai. **Yahi chaabi
ho to hi nayi APK purani par update ki tarah lagti hai. Chaabi gum = update khatam,**
har phone pe app uninstall karke dobara lagani paregi or sara setup phir se hoga.

1. Muse tumhe keystore ki file or us ke passwords dega.
2. **BACKUP lazmi:** keystore file (.jks) or passwords ko **do jagah** mehfooz rakho —
   masalan apne Google Drive me or apne computer/phone ki ek alag folder me.
   Ye kaam skip mat karna.
3. GitHub repo me jao → **Settings** → left menu **Secrets and variables** → **Actions**
   → **New repository secret**. Ye 4 secrets ek ek karke banao (values Muse dega):
   - `BULK_KEYSTORE_BASE64`
   - `BULK_KEYSTORE_PASS`
   - `BULK_KEY_ALIAS`
   - `BULK_KEY_PASS`

## STEP 3 — Update checker ko apna repo batao (ek dafa)

App ke andar update dekhne ke liye repo ka naam ek jagah likhna hai:

Ye pehle se set hai: `GITHUB_REPO = "adnanhssan65/bulk-mockup"`
(file: `app/src/main/java/com/termux/app/BulkUpdateActivity.java`).
Repo kabhi badalna ho to sirf yehi ek line badalni hai — is ke ilawa kuch nahi.

## STEP 4 — Build chalana (jab bhi nayi APK chahiye)

1. Repo → upar **Actions** tab.
2. Left se **Bulk Mockup Build** select karo → right side **Run workflow** → **Run workflow**.
3. 15–30 minute lagenge (pehle packages bante hain, phir un ke asal tests chalte hain,
   phir APK banti hai). Sab green (✔) ho jaye to:
4. Repo → **Releases** → sab se upar wali release → **BulkMockup.apk** download karo.
   Sirf **EK hi APK** hai — koi dusri app nahi lagani.

## STEP 5 — Naye phone pe install

1. Agar phone me **asal Termux pehle se hai to use uninstall karo** (upar wali
   note parho — project folders mehfooz rahenge).
2. `BulkMockup.apk` phone pe bhejo (WhatsApp/Drive/USB — jese asaan lage) or tap karo.
3. Android 14/15 par ek warning ayegi: *"This app was built for an older version of
   Android"* — **More details → Install anyway** dabana hai. Ye normal hai, ghabrana nahi.
4. Play Protect pooche to **Install anyway** hi dabana hai.
5. App kholo. Pehli dafa **"Setting up Bulk Mockup — please wait..."** ayega
   (1–3 minute, internet ki zaroorat nahi). Beech me app band ho jaye to dobara khol
   lena — setup wahi se dobara theek ho jayega, koi file kharab nahi hoti.
6. Phir **Permissions** wali screen khulegi — upar **ALLOW ALL** ka button dabao or
   har cheez Allow karte jao (beech me phone ki Settings khule to wahan bhi Allow).
7. Terminal khul jayega. Test ke liye ye chalao:

```
python -c 'import cv2, numpy, PIL, tqdm; print("SAB OK")'
```

`SAB OK` ajaye to sab tayyar hai. Drawer (left se swipe) → **PROJECTS** → project ke
**W / P / D** buttons use karo.

## STEP 6 — Projects list or shortcuts badalna

- **Projects list:** Drawer → **PROJECTS** → **Edit list** → line badlo → **Save**.
  Format: `Naam | w-command | batch-command | delete-command`
- **Shortcuts (bashrc):** Termux me `nano ~/.bashrc` — aliases wahan hain.
- Pehli dafa app ke sath **placeholder** shortcuts ate hain (`w1`..`w10`, `p1`..`p10`,
  `d1`..`d10` — script ka naam `w1.py` wali shakal me). Apne asal script naamon
  (masalan `adnan_core.py`) ke hisab se **ek dafa** badal lo — phir ye hamesha
  mehfooz rehte hain. **App update se .bashrc or projects.conf kabhi nahi badalte.**

## STEP 7 — App update karna

1. Drawer → **CHECK FOR UPDATES** dabao (sirf tab hi internet use hota hai).
2. Nayi update ho to notes ke sath **Download and install** ka button ayega — dabao,
   download hoga, phir system ki Install screen ayegi — **Install** dabao.
3. Update purani app ke upar hi lagti hai; data, shortcuts, sab mehfooz.
   "App up to date hai" likha aye to samjho koi nayi update nahi.

## STEP 8 — Asal phone par test checklist (10 lines)

Build milne ke baad kisi asal phone par ye check kar lena:

1. Bulk Mockup install ho or terminal khul jaye (asal Termux pehle uninstall ho chuka ho).
2. `python -c 'import cv2, numpy, PIL, tqdm; print("SAB OK")'` → SAB OK aye.
3. Ek project ka `w1` chalao — mockups banein or gallery me **sahi order** me ayen.
4. Drawer → PERMISSIONS — sab ✔ hon (khas taur par Storage or Battery).
5. Lamba process chalao, screen band karke 10 minute chhor do — kaam rukna nahi chahiye.
6. CHECK FOR UPDATES dabao — "up to date" ya nayi update sahi dikhaye.
7. Apne phone ki company (Vivo/Xiaomi/Oppo/Samsung) ki auto-start setting Allow ho.
8. Phone restart karke app kholo — sab waisa hi mile.
9. `termux-media-scan` wala kaam (gallery refresh) atakta na ho.
10. Update karke dekho: .bashrc or projects.conf bilkul waisi ki waisi rahen.

## Versions ki maloomat

Pin ki hui versions `PINS.txt` file me hain (Termux ka base commit, Docker image).
Packages ki mukammal list har build ke sath `dpkg-list.txt` artifact me ajati hai.
