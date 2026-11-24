package com.termux.app;

import android.content.Context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Termux Mod — Language strings table (v126).
 *
 * App ki tamam Bulk screens ke labels ek hi jagah: key → English /
 * Roman Urdu. English DEFAULT hai; Home ke top bar par theme button ke
 * sath wale Language option se Roman Urdu chuni ja sakti hai — choice
 * "bulk_mockup" prefs (key "ui_language") me persist hoti hai or foran
 * lagu ho jati hai.
 *
 * Kuch labels dono zubano me ek hi rehte hain (Projects / Terminal /
 * Permissions / Duplicates — owner khud yehi alfaaz istemal karta hai).
 *
 * Table wala hissa pure Java hai (unit tests ke liye); Context wale
 * helpers sirf prefs parhte/likhte hain.
 */
public final class BulkStrings {

    private static final String PREFS = "bulk_mockup";
    private static final String KEY_LANG = "ui_language";
    private static final String LANG_URDU = "ur";

    // ------------------------------------------------------------ keys

    public static final String LANGUAGE = "language";
    public static final String HOME = "home";
    public static final String SUBTITLE = "subtitle";
    public static final String ADD_WALLPAPERS = "add_wallpapers";
    public static final String PROJECTS = "projects";
    public static final String NEW_PROJECT_ZIP = "new_project_zip";
    public static final String TERMINAL = "terminal";
    public static final String PERMISSIONS = "permissions";
    public static final String CHECK_FOR_UPDATES = "check_for_updates";
    public static final String ABOUT = "about";
    public static final String ABOUT_TITLE = "about_title";
    public static final String ABOUT_BODY = "about_body";
    public static final String CLOSE = "close";
    public static final String CANCEL = "cancel";
    public static final String BACK = "back";
    public static final String SWITCH_THEME = "switch_theme";

    public static final String PROJECTS_SUBTITLE = "projects_subtitle";
    public static final String NO_PROJECTS = "no_projects";
    public static final String NO_PROJECTS_SUB = "no_projects_sub";
    public static final String ADD_NEW_PROJECT_ZIP = "add_new_project_zip";
    public static final String RUN = "run";
    public static final String BATCH = "batch";
    public static final String DELETE = "delete";

    public static final String ADD_WALLPAPERS_SUBTITLE = "add_wallpapers_subtitle";
    public static final String STEP1_SELECT_PROJECT = "step1_select_project";
    public static final String CHOOSE_PROJECT = "choose_project";
    public static final String STEP2_SELECT_IMAGES = "step2_select_images";
    public static final String SELECT_IMAGES = "select_images";
    public static final String STEP3_ACTIONS = "step3_actions";
    public static final String SELECT_PROJECT = "select_project";
    public static final String ADD = "add";
    public static final String RUN_NOW = "run_now";
    public static final String UNLOCKED = "unlocked";
    public static final String REQUIRED = "required";
    public static final String SELECTED_COUNT = "selected_count";
    public static final String WALLPAPERS_COUNT = "wallpapers_count";
    public static final String CLEAR_STAGING_TITLE = "clear_staging_title";
    public static final String CLEAR_STAGING_MSG = "clear_staging_msg";
    public static final String CLEAR = "clear";
    public static final String SORT_TITLE = "sort_title";
    public static final String SORT_NAME_ASC = "sort_name_asc";
    public static final String SORT_NAME_DESC = "sort_name_desc";
    public static final String SORT_DATE_ASC = "sort_date_asc";
    public static final String SORT_DATE_DESC = "sort_date_desc";
    public static final String SORT_SIZE_ASC = "sort_size_asc";
    public static final String SORT_SIZE_DESC = "sort_size_desc";
    public static final String SELECT = "select";
    public static final String SORT = "sort";
    public static final String PREVIEW_ADDED = "preview_added";
    public static final String NO_WALLPAPERS = "no_wallpapers";
    public static final String DUPLICATES = "duplicates";
    public static final String DUP_MOVED_TOAST = "dup_moved_toast";
    public static final String ADDING = "adding";
    public static final String RENDERING = "rendering";
    public static final String ADDED_TOAST = "added_toast";
    public static final String FAIL_SUFFIX = "fail_suffix";

    public static final String NO_DUPLICATES = "no_duplicates";
    public static final String AUTO_DELETE_AFTER = "auto_delete_after";
    public static final String AUTO_DELETE_OFF = "auto_delete_off";
    public static final String SELECT_ALL = "select_all";
    public static final String DUP_COUNT = "dup_count";
    public static final String TTL_HOURS = "ttl_hours";
    public static final String TTL_WEEK = "ttl_week";
    public static final String TTL_NEVER = "ttl_never";

    public static final String ADD_PROJECT_SUBTITLE = "add_project_subtitle";
    public static final String INSTALL_PACKAGE = "install_package";
    public static final String INSTALL_PACKAGE_SUB = "install_package_sub";
    public static final String SELECT_ZIP = "select_zip";
    public static final String INSTALLING = "installing";
    public static final String INSTALLED_TITLE = "installed_title";
    public static final String ADD_ANOTHER = "add_another";
    public static final String OPEN_PROJECTS = "open_projects";
    public static final String EXTRACTED_FROM = "extracted_from";
    public static final String PROJECT_N = "project_n";

    public static final String PERM_SUB = "perm_sub";
    public static final String ALLOW_ALL = "allow_all";
    public static final String ALLOW = "allow";
    public static final String OPEN = "open";
    public static final String PERM_STORAGE_HINT = "perm_storage_hint";
    public static final String PERM_BATTERY = "perm_battery";
    public static final String PERM_BATTERY_HINT = "perm_battery_hint";
    public static final String PERM_NOTIFICATIONS = "perm_notifications";
    public static final String PERM_NOTIFICATIONS_HINT = "perm_notifications_hint";
    public static final String PERM_INSTALL = "perm_install";
    public static final String PERM_INSTALL_HINT = "perm_install_hint";
    public static final String PERM_AUTOSTART = "perm_autostart";
    public static final String PERM_AUTOSTART_HINT = "perm_autostart_hint";
    public static final String PERM_DONE_TOAST = "perm_done_toast";
    public static final String PERM_ALL_DONE_TOAST = "perm_all_done_toast";
    public static final String PERM_TERMINAL_HINT = "perm_terminal_hint";
    public static final String GO_DASHBOARD = "go_dashboard";
    public static final String PERM_CMD_TITLE = "perm_cmd_title";
    public static final String PERM_CMD_SUBTITLE = "perm_cmd_subtitle";
    public static final String STEP_N = "step_n";
    public static final String CMD_STORAGE_TITLE = "cmd_storage_title";
    public static final String CMD_STORAGE_HINT = "cmd_storage_hint";
    public static final String CMD_STORAGE_CHECK_TITLE = "cmd_storage_check_title";
    public static final String CMD_STORAGE_CHECK_HINT = "cmd_storage_check_hint";
    public static final String CMD_WAKELOCK_TITLE = "cmd_wakelock_title";
    public static final String CMD_WAKELOCK_HINT = "cmd_wakelock_hint";
    public static final String CMD_WAKEUNLOCK_TITLE = "cmd_wakeunlock_title";
    public static final String CMD_WAKEUNLOCK_HINT = "cmd_wakeunlock_hint";

    public static final String UPDATE_TITLE = "update_title";
    public static final String UPDATE_CHECKING = "update_checking";
    public static final String DOWNLOAD_INSTALL = "download_install";
    public static final String UPDATE_AVAILABLE = "update_available";
    public static final String UPDATE_NO_APK = "update_no_apk";
    public static final String UPDATE_LATEST = "update_latest";
    public static final String UPDATE_FAIL = "update_fail";
    public static final String UPDATE_NEED_INSTALL_PERM = "update_need_install_perm";
    public static final String UPDATE_DOWNLOADING = "update_downloading";
    public static final String UPDATE_DOWNLOAD_FAIL = "update_download_fail";
    public static final String UPDATE_INSTALL_OPENED = "update_install_opened";
    public static final String UPDATE_INSTALL_FAIL = "update_install_fail";

    // v127 — naya design: Run / Quick Run, hub, ready assets, output folder, font, contact
    public static final String NAV_RUN = "nav_run";
    public static final String QUICK_RUN_TITLE = "quick_run_title";
    public static final String SEARCH_PROJECT = "search_project";
    public static final String READY_COUNT = "ready_count";
    public static final String NO_MATCHING = "no_matching";
    public static final String DUPLICATE_CHECK = "duplicate_check";
    public static final String DROP_BOX = "drop_box";
    public static final String DROP_BOX_EMPTY = "drop_box_empty";
    public static final String UNLOCK_SELECT_PROJECT = "unlock_select_project";
    public static final String UNLOCK_ADD_IMAGES = "unlock_add_images";
    public static final String PREVIEW_READY = "preview_ready";
    public static final String NO_READY_PROJECTS = "no_ready_projects";
    public static final String TAP_TO_INSPECT = "tap_to_inspect";
    public static final String MOCKUPS_TAB = "mockups_tab";
    public static final String WALLPAPERS_TAB = "wallpapers_tab";
    public static final String OUTPUT_FOLDER = "output_folder";
    public static final String STORAGE_LOCATION = "storage_location";
    public static final String MAIN_FOLDER = "main_folder";
    public static final String PROJECT_FOLDER = "project_folder";
    public static final String LIVE_FOLDER_TREE = "live_folder_tree";
    public static final String OK = "ok";
    public static final String FONT = "font";
    public static final String CONTACT_ADMIN = "contact_admin";
    public static final String REPLAY_POPUPS = "replay_popups";
    public static final String REPLAY_POPUPS_HINT = "replay_popups_hint";
    public static final String ZIP_SHORTCUT_DONE = "zip_shortcut_done";
    public static final String RUN_SETTINGS = "run_settings";
    public static final String ANSWER_MODE = "answer_mode";
    public static final String AUTO_OPTION = "auto_option";
    public static final String MANUAL_OPTION = "manual_option";
    public static final String SELECT_MODE = "select_mode";
    public static final String HD_OPTION = "hd_option";
    public static final String NORMAL_OPTION = "normal_option";
    public static final String SELECT_SIZE = "select_size";
    public static final String ORIGINAL_OPTION = "original_option";
    public static final String CROP_OPTION = "crop_option";
    public static final String DEFAULT_TAG = "default_tag";
    public static final String PROCESSING = "processing";
    public static final String RUN_DONE = "run_done";
    public static final String RUN_FAILED = "run_failed";
    public static final String OPEN_FILE_MANAGER = "open_file_manager";
    public static final String EXPORT_PROJECT = "export_project";
    public static final String EXPORTING = "exporting";
    public static final String EXPORT_DONE = "export_done";
    public static final String COPY_PATH = "copy_path";
    public static final String PATH_COPIED = "path_copied";
    public static final String NO_FILE_MANAGER = "no_file_manager";
    public static final String ALL_IMAGES = "all_images";
    public static final String SYSTEM_PICKER = "system_picker";
    public static final String GALLERY_EMPTY = "gallery_empty";

    // v129 — Drop Wallpapers (Ready Assets ke andar), Home live logs, permission popup
    public static final String DROP_WALLPAPERS = "drop_wallpapers";
    public static final String DROP_WALLPAPERS_SUB = "drop_wallpapers_sub";
    public static final String LOG_IDLE = "log_idle";
    public static final String LOG_RUN_START = "log_run_start";
    public static final String LOG_RUN_DONE = "log_run_done";
    public static final String LOG_RUN_FAIL = "log_run_fail";
    public static final String PERM_POPUP_TITLE = "perm_popup_title";
    public static final String PERM_POPUP_MSG = "perm_popup_msg";
    public static final String PERM_MANUAL_TAG = "perm_manual_tag";

    // v130 — gallery toolbar (owner ka HTML design: All Images Gallery Manager)
    public static final String UNSELECT_ALL = "unselect_all";
    public static final String COPY_MODE = "copy_mode";
    public static final String MOVE_MODE = "move_mode";
    public static final String MOVE = "move";
    public static final String ADD_COUNT = "add_count";
    public static final String MOVE_COUNT = "move_count";
    public static final String SORT_NEWEST = "sort_newest";
    public static final String SORT_OLDEST = "sort_oldest";
    public static final String SORT_BY_NAME = "sort_by_name";
    public static final String SWIPE_TIP = "swipe_tip";
    public static final String ITEMS_COUNT = "items_count";
    public static final String MOVED_ORIGINALS_TOAST = "moved_originals_toast";
    public static final String DELETE_DROPPED_TITLE = "delete_dropped_title";
    public static final String DELETE_DROPPED_MSG = "delete_dropped_msg";
    public static final String DELETED_TOAST = "deleted_toast";

    // v132 — frame preview (DP) + gallery Lock/Clear
    public static final String FRAME_PREVIEW = "frame_preview";
    public static final String LOCK = "lock";
    public static final String LOCKED = "locked";

    // ------------------------------------------------------------ table

    private static final Map<String, String> EN = new LinkedHashMap<>();
    private static final Map<String, String> UR = new LinkedHashMap<>();

    private static void put(String key, String en, String ur) {
        EN.put(key, en);
        UR.put(key, ur);
    }

    static {
        // Dashboard / drawer
        put(LANGUAGE, "Language", "Language");
        put(HOME, "Home", "Home");
        put(SUBTITLE, "Powerful Terminal Experience", "Powerful Terminal Experience");
        put(ADD_WALLPAPERS, "Add Wallpapers", "Wallpaper Add Karo");
        put(PROJECTS, "Projects", "Projects");
        put(NEW_PROJECT_ZIP, "New Project (ZIP)", "Naya Project (ZIP)");
        put(TERMINAL, "Terminal", "Terminal");
        put(PERMISSIONS, "Permissions", "Permissions");
        put(CHECK_FOR_UPDATES, "Check for Updates", "Update Check Karo");
        put(ABOUT, "About", "App Ke Bare Me");
        put(ABOUT_TITLE, "About Termux Mod", "Termux Mod Ke Bare Me");
        put(ABOUT_BODY,
            "A streamlined Termux terminal & automated phone mockup generator built for effortless one-tap workflows.",
            "Ek asaan Termux terminal aur automatic phone mockup generator — one-tap workflows ke liye banaya gaya.");
        put(CLOSE, "Close", "Band Karo");
        put(CANCEL, "Cancel", "Cancel Karo");
        put(BACK, "Back", "Wapas");
        put(SWITCH_THEME, "Switch Theme", "Theme Badlo");

        // Projects screen
        put(PROJECTS_SUBTITLE, "Tap a project for Run, Batch & Delete",
            "Project par tap karo — Run, Batch aur Delete ke liye");
        put(NO_PROJECTS, "No Projects Installed", "Koi Project Install Nahi");
        put(NO_PROJECTS_SUB, "Tap \"%s\" below to install a template.",
            "Template lagane ke liye neeche \"%s\" dabao.");
        put(ADD_NEW_PROJECT_ZIP, "Add New Project (ZIP)", "Naya Project Add Karo (ZIP)");
        put(RUN, "Run", "Chalao");
        put(BATCH, "Batch", "Batch");
        put(DELETE, "Delete", "Delete Karo");

        // Add Wallpapers screen
        put(ADD_WALLPAPERS_SUBTITLE, "Select project first, then pick images",
            "Pehle project select karo, phir images chuno");
        put(STEP1_SELECT_PROJECT, "Step 1 • Select Project", "Step 1 • Project Select Karo");
        put(CHOOSE_PROJECT, "-- Choose a Project --", "-- Project Chuno --");
        put(STEP2_SELECT_IMAGES, "Step 2 • Select Images", "Step 2 • Images Chuno");
        put(SELECT_IMAGES, "Select Images", "Images Chuno");
        put(STEP3_ACTIONS, "Step 3 • Actions", "Step 3 • Add aur Run");
        put(SELECT_PROJECT, "Select Project", "Project Select Karo");
        put(ADD, "Add", "Add Karo");
        put(RUN_NOW, "Run Now", "Abhi Chalao");
        put(UNLOCKED, "Unlocked", "Khul Gaya");
        put(REQUIRED, "Required", "Zaroori Hai");
        put(SELECTED_COUNT, "%d selected", "%d select hue");
        put(WALLPAPERS_COUNT, "%d wallpaper(s)", "%d wallpaper");
        put(CLEAR_STAGING_TITLE, "Clear Staging", "Staging Saaf Karo");
        put(CLEAR_STAGING_MSG, "Remove all %d staged wallpapers?",
            "Saare %d staged wallpapers hata dun?");
        put(CLEAR, "Clear", "Saaf Karo");
        put(SORT_TITLE, "Sort Wallpapers", "Wallpapers Sort Karo");
        put(SORT_NAME_ASC, "Name (A to Z)", "Naam (A se Z)");
        put(SORT_NAME_DESC, "Name (Z to A)", "Naam (Z se A)");
        put(SORT_DATE_ASC, "Date Added (Oldest First)", "Date Added (Purane Pehle)");
        put(SORT_DATE_DESC, "Date Added (Newest First)", "Date Added (Naye Pehle)");
        put(SORT_SIZE_ASC, "Size (Small to Large)", "Size (Chhote se Bare)");
        put(SORT_SIZE_DESC, "Size (Large to Small)", "Size (Bare se Chhote)");
        put(SELECT, "Select", "Select Karo");
        put(SORT, "Sort", "Sort Karo");
        put(PREVIEW_ADDED, "Preview Added Images", "Add Kiye Hue Wallpapers");
        put(NO_WALLPAPERS, "No wallpapers", "Koi wallpaper nahi");
        put(DUPLICATES, "Duplicates", "Duplicates");
        put(DUP_MOVED_TOAST, "%d duplicate(s) moved to Duplicates",
            "%d duplicate Duplicates me chale gaye");
        put(ADDING, "Adding wallpapers...", "Wallpapers add ho rahe hain...");
        put(RENDERING, "Rendering phone mockups...", "Phone mockups ban rahe hain...");
        put(ADDED_TOAST, "Added %d wallpaper(s) to %s", "%d wallpaper %s me add ho gaye");
        put(FAIL_SUFFIX, " (%d fail)", " (%d fail)");

        // Duplicates screen
        put(NO_DUPLICATES, "No duplicates", "Koi duplicate nahi");
        put(AUTO_DELETE_AFTER, "Auto-delete after", "Auto-delete itne time baad");
        put(AUTO_DELETE_OFF, "Auto-delete off", "Auto-delete band");
        put(SELECT_ALL, "Select All", "Sab Select Karo");
        put(DUP_COUNT, "%d duplicate(s)", "%d duplicate");
        put(TTL_HOURS, "%d hours", "%d ghante");
        put(TTL_WEEK, "1 week", "1 hafta");
        put(TTL_NEVER, "Never", "Kabhi Nahi");

        // Add Project (ZIP) screen
        put(ADD_PROJECT_SUBTITLE, "Auto-install script & mockup template",
            "Script aur mockup template auto-install");
        put(INSTALL_PACKAGE, "Install Project Package", "Project Package Install Karo");
        put(INSTALL_PACKAGE_SUB, "Select a .ZIP file containing script & template",
            "Wo .ZIP file chuno jis me script aur template ho");
        put(SELECT_ZIP, "Select ZIP File", "ZIP File Chuno");
        put(INSTALLING, "Installing Project Archive...", "Project Archive install ho raha hai...");
        put(INSTALLED_TITLE, "Project Installed!", "Project Install Ho Gaya!");
        put(ADD_ANOTHER, "Add Another", "Ek Aur Add Karo");
        put(OPEN_PROJECTS, "Open Projects", "Projects Kholain");
        put(EXTRACTED_FROM, "Extracted from %s", "%s se nikala gaya");
        put(PROJECT_N, "Project %d", "Project %d");

        // Permissions screen
        put(PERM_SUB, "All of these are needed for the app to work properly.",
            "Ye sab cheezein app ke sahi kaam ke liye zaroori hain.");
        put(ALLOW_ALL, "✔  ALLOW ALL", "✔  SAB EK SATH ALLOW KARO");
        put(ALLOW, "Allow", "Allow Karo");
        put(OPEN, "Open", "Kholo");
        put(PERM_STORAGE_HINT, "To read and save your wallpapers.",
            "Wallpapers parhne/likhne ke liye.");
        put(PERM_BATTERY, "Battery", "Battery");
        put(PERM_BATTERY_HINT, "So the app is not stopped during work.",
            "Kaam ke doran app band na ho.");
        put(PERM_NOTIFICATIONS, "Notifications", "Notifications");
        put(PERM_NOTIFICATIONS_HINT, "To show progress messages.",
            "Kaam mukammal hone ki khabar ke liye.");
        put(PERM_INSTALL, "Install updates", "Update Install Karna");
        put(PERM_INSTALL_HINT, "To install app updates from here.",
            "Nayi update isi app se lagane ke liye.");
        put(PERM_AUTOSTART, "Background / Auto-start", "Background / Auto-start");
        put(PERM_AUTOSTART_HINT,
            "Let the app keep running in the background — open your phone's setting and allow Termux Mod. (The app cannot check this one itself, so no tick is shown — it is enabled only in the phone setting.)",
            "Screen band hone par bhi app chalta rahe — apne phone ki setting kholo aur Termux Mod ko Allow karo. (Ye wali app khud check nahi kar sakti, is liye tick nahi dikhta — ye sirf phone ki setting se on hoti hai.)");
        put(PERM_DONE_TOAST, "Permissions done ✔ — now choose your project",
            "Permissions ho gayi ✔ — ab apna project chuno");
        put(PERM_ALL_DONE_TOAST, "All permissions done ✔", "Sab permissions ho gayi ✔");
        put(PERM_TERMINAL_HINT,
            "If the popups do not work, run the permission commands one by one in the terminal.",
            "Agar popups kaam na karen to permission wali commands terminal me ek ek karke chalao.");
        put(GO_DASHBOARD, "Go to Dashboard", "Dashboard Par Jao");
        put(PERM_CMD_TITLE, "Permission Commands", "Permission Commands");
        put(PERM_CMD_SUBTITLE, "Run each command, then come back for the next",
            "Har command chalao, phir wapas aa kar agli chalao");
        put(STEP_N, "Step %d", "Step %d");
        put(CMD_STORAGE_TITLE, "Storage Permission", "Storage Permission");
        put(CMD_STORAGE_HINT,
            "Asks for storage permission and creates the storage folders — the same step the first-run setup performs.",
            "Storage permission mangta hai aur storage folders banata hai — yehi step pehli setup me chalta hai.");
        put(CMD_STORAGE_CHECK_TITLE, "Storage Check", "Storage Check");
        put(CMD_STORAGE_CHECK_HINT,
            "Lists your phone storage. If the files show up, storage permission is working.",
            "Phone ki storage ki list dikhata hai. Files nazar ayen to storage permission theek hai.");
        put(CMD_WAKELOCK_TITLE, "Wake Lock", "Wake Lock");
        put(CMD_WAKELOCK_HINT,
            "Keeps the phone awake during long work (the app's own command — every project run uses it).",
            "Lambe kaam ke doran phone ko jagaye rakhta hai (app ki apni command — har project run me istemal hoti hai).");
        put(CMD_WAKEUNLOCK_TITLE, "Wake Unlock", "Wake Unlock");
        put(CMD_WAKEUNLOCK_HINT, "Releases the wake lock taken by the previous step.",
            "Pichle step wala wake lock chhor deta hai.");

        // Update screen
        put(UPDATE_TITLE, "CHECK FOR UPDATES", "UPDATE CHECK KARO");
        put(UPDATE_CHECKING, "Checking...", "Dekh raha hai...");
        put(DOWNLOAD_INSTALL, "Download and install", "Download Karke Install Karo");
        put(UPDATE_AVAILABLE, "New update available ✔  (%s)\nYour version: %s",
            "Nayi update hai ✔  (%s)\nTumhara version: %s");
        put(UPDATE_NO_APK, "New update (%s) but BulkMockup.apk was not found in the release.",
            "Nayi update hai (%s) lekin release me BulkMockup.apk nahi mili.");
        put(UPDATE_LATEST, "App is up to date ✔\nVersion: %s",
            "App up to date hai ✔\nVersion: %s");
        put(UPDATE_FAIL, "Could not check.\nCheck your internet and try again.",
            "Check nahi ho saka.\nInternet hai to dobara koshish karo.");
        put(UPDATE_NEED_INSTALL_PERM,
            "First allow 'Install updates' — opening the Permissions screen.",
            "Pehle 'Install updates' wali ijazat do — Permissions screen khul rahi hai.");
        put(UPDATE_DOWNLOADING, "Downloading %s...", "Download ho raha hai... (%s)");
        put(UPDATE_DOWNLOAD_FAIL, "Download failed — check internet and try again.",
            "Download adhoora reh gaya. Internet check karke dobara dabao.");
        put(UPDATE_INSTALL_OPENED,
            "The install screen has opened — tap Install there. Your data, .bashrc and projects list stay safe.",
            "Install wali screen khul gayi hai — wahan Install dabao. Tumhara data, .bashrc aur projects list mehfooz rahegi.");
        put(UPDATE_INSTALL_FAIL, "Install screen could not open: %s",
            "Install screen nahi khul saki: %s");

        // v127 — naya design + additions
        put(NAV_RUN, "Run", "Run");
        put(QUICK_RUN_TITLE, "Select Project to Run", "Run Karne Ke Liye Project Chunein");
        put(SEARCH_PROJECT, "Search project...", "Project search karein...");
        put(READY_COUNT, "%d Ready", "%d Ready");
        put(NO_MATCHING, "No matching projects", "Koi project nahi mila");
        put(DUPLICATE_CHECK, "Duplicate Check", "Duplicate Check");
        put(DROP_BOX, "Drop Box", "Drop Box");
        put(DROP_BOX_EMPTY, "Tap \"Add Wallpapers\" above to stage images",
            "Images lane ke liye upar \"Add Wallpapers\" dabayein");
        put(UNLOCK_SELECT_PROJECT, "Select a project to unlock",
            "Unlock karne ke liye project select karein");
        put(UNLOCK_ADD_IMAGES, "Add wallpapers to unlock",
            "Unlock karne ke liye wallpapers add karein");
        put(PREVIEW_READY, "Preview Ready Assets", "Preview Ready Assets");
        put(NO_READY_PROJECTS, "No completed mockups yet. Run a project first.",
            "Abhi koi mockup tayyar nahi hai. Pehle project run karein.");
        put(TAP_TO_INSPECT, "Tap any item to inspect full preview",
            "Full preview dekhne ke liye kisi bhi image par tap karein");
        put(MOCKUPS_TAB, "Mockups/", "Mockups/");
        put(WALLPAPERS_TAB, "Wallpapers/", "Wallpapers/");
        put(OUTPUT_FOLDER, "Output Folder", "Output Folder");
        put(STORAGE_LOCATION, "Storage Location", "Storage Location");
        put(MAIN_FOLDER, "Main Folder Name", "Main Folder Name");
        put(PROJECT_FOLDER, "Project Folder Name", "Project Folder Name");
        put(LIVE_FOLDER_TREE, "Live Folder Structure", "Live Folder Structure");
        put(OK, "OK", "OK");
        put(FONT, "Font", "Font");
        put(CONTACT_ADMIN, "Contact Admin", "Admin Se Rabta");
        put(REPLAY_POPUPS, "Replay First-Time Popups", "First-Time Popups Dobara Chalayein");
        put(REPLAY_POPUPS_HINT, "Show the first-time permission popups again",
            "Pehli dafa wale permission popups dobara dikhao");
        put(ZIP_SHORTCUT_DONE, "Folders & Home shortcut created",
            "Folders aur Home shortcut ban gaya");
        put(RUN_SETTINGS, "Run Settings", "Run Settings");
        put(ANSWER_MODE, "Answer Mode", "Jawab Ka Tareeqa");
        put(AUTO_OPTION, "Auto — Saved Answers", "Auto — Save Shuda Jawab");
        put(MANUAL_OPTION, "Manual — Ask Every Time", "Manual — Har Bar Poochho");
        put(SELECT_MODE, "Select Mode", "Mode Chunein");
        put(HD_OPTION, "HD Mode", "HD Mode");
        put(NORMAL_OPTION, "Normal (Fast KB Pack)", "Normal (Fast KB Pack)");
        put(SELECT_SIZE, "Select Size", "Size Chunein");
        put(ORIGINAL_OPTION, "Original Size", "Original Size");
        put(CROP_OPTION, "Crop 9:16", "Crop 9:16");
        put(DEFAULT_TAG, "(Default)", "(Default)");
        put(PROCESSING, "Processing…", "Process Ho Raha Hai…");
        put(RUN_DONE, "Finished ✔", "Ho Gaya ✔");
        put(RUN_FAILED, "Failed ✖", "Fail Ho Gaya ✖");
        put(OPEN_FILE_MANAGER, "Open in File Manager", "File Manager Me Kholo");
        put(EXPORT_PROJECT, "Export Project", "Project Export Karo");
        put(EXPORTING, "Exporting…", "Export Ho Raha Hai…");
        put(EXPORT_DONE, "Exported to:\n%s", "Export ho gaya:\n%s");
        put(COPY_PATH, "Copy Path", "Path Copy Karo");
        put(PATH_COPIED, "Path copied", "Path Copy Ho Gaya");
        put(NO_FILE_MANAGER, "No file manager found", "Koi file manager nahi mila");
        put(ALL_IMAGES, "All Images", "Tamam Images");
        put(SYSTEM_PICKER, "System Picker", "System Picker");
        put(GALLERY_EMPTY, "No images found on this device", "Is device par koi image nahi mili");

        // v129
        put(DROP_WALLPAPERS, "Drop Wallpapers", "Drop Wallpapers");
        put(DROP_WALLPAPERS_SUB, "Wallpapers dropped into each project",
            "Har project me drop kiye hue wallpapers");
        put(LOG_IDLE, "Ready — logs yahan live ayenge", "Ready — logs yahan live ayenge");
        put(LOG_RUN_START, "▶ %s — run shuru", "▶ %s — run shuru");
        put(LOG_RUN_DONE, "✔ %s — run mukammal", "✔ %s — run mukammal");
        put(LOG_RUN_FAIL, "✖ %s — run fail", "✖ %s — run fail");
        put(PERM_POPUP_TITLE, "Permission Chahiye", "Permission Chahiye");
        put(PERM_POPUP_MSG,
            "Storage permission ke baghair wallpapers add/run nahi ho sakte. Permissions screen se allow kar dein.",
            "Storage permission ke baghair wallpapers add/run nahi ho sakte. Permissions screen se allow kar dein.");
        put(PERM_MANUAL_TAG, "Manual", "Manual");

        // v130 — gallery toolbar + Move
        put(UNSELECT_ALL, "Unselect All", "Sab Unselect Karo");
        put(COPY_MODE, "Copy", "Copy");
        put(MOVE_MODE, "Move", "Move");
        put(MOVE, "Move", "Move Karo");
        put(ADD_COUNT, "Add (%d)", "Add Karo (%d)");
        put(MOVE_COUNT, "Move (%d)", "Move Karo (%d)");
        put(SORT_NEWEST, "Newest", "Naye Pehle");
        put(SORT_OLDEST, "Oldest", "Purane Pehle");
        put(SORT_BY_NAME, "Name", "Naam");
        put(SWIPE_TIP, "Tip: Swipe/drag horizontally across images to select",
            "Tip: Images select karne ke liye ungli side par ghaseetein");
        put(ITEMS_COUNT, "%d items", "%d items");
        put(MOVED_ORIGINALS_TOAST, "%d original(s) deleted from gallery (moved)",
            "%d original gallery se delete ho gaye (move hue the)");
        put(DELETE_DROPPED_TITLE, "Delete Wallpapers", "Wallpapers Delete Karo");
        put(DELETE_DROPPED_MSG,
            "Delete %d dropped wallpaper(s) from this project? The files will be removed from the project folder. This cannot be undone.",
            "Is project se %d dropped wallpaper delete kar dun? Files project folder se hat jayengi. Ye wapas nahi hoga.");
        put(DELETED_TOAST, "%d wallpaper(s) deleted", "%d wallpaper delete ho gaye");

        // v132 — frame preview + gallery Lock/Clear
        put(FRAME_PREVIEW, "Frame Preview", "Frame Preview");
        put(LOCK, "Lock", "Lock");
        put(LOCKED, "Locked", "Lock Laga Hai");
    }

    private BulkStrings() {}

    // ------------------------------------------------------------ pure table

    /** Tamam keys (EN or UR dono me lazmi mojood). */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(EN.keySet());
    }

    public static boolean hasBothLanguages(String key) {
        String en = EN.get(key);
        String ur = UR.get(key);
        return en != null && !en.isEmpty() && ur != null && !ur.isEmpty();
    }

    /** Key ki value; urdu=true ho to Roman Urdu, warna English. Na mile to key wapas. */
    public static String get(String key, boolean urdu) {
        String v = (urdu ? UR : EN).get(key);
        if (v == null) v = EN.get(key);
        return v != null ? v : key;
    }

    // ------------------------------------------------------------ prefs glue

    public static boolean isUrdu(Context c) {
        try {
            return LANG_URDU.equals(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LANG, "en"));
        } catch (Exception e) {
            return false;
        }
    }

    public static void setUrdu(Context c, boolean urdu) {
        try {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_LANG, urdu ? LANG_URDU : "en").apply();
        } catch (Exception ignored) {}
    }

    /** Mojooda language me label. */
    public static String t(Context c, String key) {
        return get(key, isUrdu(c));
    }

    /** Mojooda language me label, %d/%s args ke sath. */
    public static String format(Context c, String key, Object... args) {
        try {
            return String.format(Locale.US, t(c, key), args);
        } catch (Exception e) {
            return t(c, key);
        }
    }
}
