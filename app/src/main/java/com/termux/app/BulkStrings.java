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
    public static final String BACKGROUND = "background";
    public static final String BACK = "back";
    public static final String SWITCH_THEME = "switch_theme";

    public static final String PROJECTS_SUBTITLE = "projects_subtitle";
    public static final String NO_PROJECTS = "no_projects";
    public static final String NO_PROJECTS_SUB = "no_projects_sub";
    public static final String ADD_NEW_PROJECT_ZIP = "add_new_project_zip";
    public static final String RUN = "run";
    public static final String BATCH = "batch";
    public static final String DELETE = "delete";
    // v138 — Remove Project (Projects screen ke top wali row)
    public static final String REMOVE_PROJECT = "remove_project";
    // v154 — More Settings (Auto Scan / Wait Time), batch naam, recycle bin
    public static final String AUTO_SCAN = "auto_scan";
    public static final String AUTO_SCAN_SUB = "auto_scan_sub";
    public static final String GALLERY_SCAN = "gallery_scan";
    public static final String GALLERY_SCAN_SUB = "gallery_scan_sub";
    public static final String GALLERY_SCAN_RUNNING = "gallery_scan_running";
    public static final String GALLERY_SCAN_DONE_FMT = "gallery_scan_done_fmt";
    public static final String WAIT_TIME = "wait_time";
    public static final String WAIT_TIME_SUB_FMT = "wait_time_sub_fmt";
    public static final String WAIT_TIME_HINT = "wait_time_hint";
    public static final String RESET_DEFAULT = "reset_default";
    public static final String BATCH_NAME = "batch_name";
    public static final String REMOVE_SELECTED = "remove_selected";
    public static final String REMOVED_TO_BIN_FMT = "removed_to_bin_fmt";
    public static final String RECYCLE_BIN = "recycle_bin";
    public static final String BIN_EMPTY = "bin_empty";
    public static final String RESTORE = "restore";
    public static final String DELETE_FOREVER = "delete_forever";
    public static final String REMOVE_PROJECT_CONFIRM_TITLE = "remove_project_confirm_title";
    public static final String REMOVE_PROJECT_CONFIRM_MSG = "remove_project_confirm_msg";
    public static final String REMOVE_BTN = "remove_btn";
    public static final String REMOVED_TOAST = "removed_toast";
    public static final String REMOVE_FAILED = "remove_failed";
    // v155 — Projects screen ki Status strip + popup
    public static final String STATUS_LABEL = "status_label";
    public static final String STATUS_ALL_EMPTY = "status_all_empty";
    public static final String STATUS_READY_COUNT = "status_ready_count";
    // v155 — Time Mode (More Settings) + Time Reset
    public static final String TIME_MODE = "time_mode";
    public static final String TIME_MODE_SUB_FMT = "time_mode_sub_fmt";
    public static final String TIME_MODE_PAST = "time_mode_past";
    public static final String TIME_MODE_FUTURE = "time_mode_future";
    public static final String TIME_RESET = "time_reset";
    public static final String TIME_RESET_SUB = "time_reset_sub";
    public static final String TIME_RESET_DONE = "time_reset_done";

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
    public static final String ZIP_NO_FOLDER = "zip_no_folder";
    public static final String ZIP_NO_SCRIPT = "zip_no_script";
    public static final String PROJECT_EXISTS = "project_exists";

    public static final String PERM_SUB = "perm_sub";
    public static final String ALLOW_ALL = "allow_all";
    public static final String ALLOW = "allow";
    public static final String OPEN = "open";
    public static final String PERM_API = "perm_api";
    public static final String PERM_API_INSTALL = "perm_api_install";
    public static final String PERM_API_DONE = "perm_api_done";
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
    public static final String RUN_VIEW = "run_view";
    public static final String RUN_VIEW_SUB = "run_view_sub";
    public static final String RUNNING_IN_TERMINAL = "running_in_terminal";
    public static final String SHOWPIECE_EDIT = "showpiece_edit";
    public static final String SHOWPIECE_EDIT_SUB = "showpiece_edit_sub";
    public static final String SHOWPIECE_NAME_LABEL = "showpiece_name_label";
    public static final String SHOWPIECE_NUMBER_LABEL = "showpiece_number_label";
    public static final String SHOWPIECE_LOADING_LABEL = "showpiece_loading_label";
    public static final String SHOWPIECE_DONE_LABEL = "showpiece_done_label";
    public static final String SEQ_FIX = "seq_fix";
    public static final String SEQ_FIX_SUB = "seq_fix_sub";
    public static final String ALAG_SECOND = "alag_second";
    public static final String ALAG_SECOND_SUB = "alag_second_sub";
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
    public static final String RUN_BACKGROUND = "run_background";
    public static final String RUN_CANCELLED = "run_cancelled";
    public static final String RUN_ALREADY_ACTIVE = "run_already_active";
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

    // v133 — Themes popup, System Picker subtitle, Drop Box compact tile,
    // gallery gear sections, folder-shift (migration) summary
    public static final String THEMES = "themes";
    public static final String SYSTEM_PICKER_SUB = "system_picker_sub";
    public static final String VIEW_ALL = "view_all";
    public static final String TAP_TO_VIEW = "tap_to_view";
    public static final String SORT_SECTION = "sort_section";
    public static final String GRID_SECTION = "grid_section";
    public static final String SCROLL_BUTTON = "scroll_button";
    public static final String SHOW = "show";
    public static final String HIDE = "hide";
    public static final String MIGRATED_TITLE = "migrated_title";
    public static final String MIGRATED_MSG = "migrated_msg";
    public static final String STAGED_DROP_SUB = "staged_drop_sub";

    // v134 — move-back summary + Recreate auto/manual settings
    public static final String MOVEDBACK_TITLE = "movedback_title";
    public static final String MOVEDBACK_MSG = "movedback_msg";
    public static final String RECREATE_SETTINGS = "recreate_settings";
    public static final String RECREATE_FOLDERS_AUTO = "recreate_folders_auto";
    public static final String RECREATE_VIDEOS_AUTO = "recreate_videos_auto";
    public static final String RECREATE_AUTO_OFF_NOTE = "recreate_auto_off_note";
    public static final String VIEW_LESS = "view_less";
    public static final String LOG_MOVEBACK_STAY_ROOT = "log_moveback_stay_root";
    public static final String LOG_MOVEBACK_STAY_RUNNER = "log_moveback_stay_runner";
    public static final String LOG_MOVEBACK_MOVED = "log_moveback_moved";
    public static final String LOG_MIRROR_MOCKUPS = "log_mirror_mockups";
    public static final String LOG_MIRROR_READY = "log_mirror_ready";

    // v136 — engine $HOME shift + drop folders pehli setup
    public static final String HOMEMIG_TITLE = "homemig_title";
    public static final String HOMEMIG_MSG = "homemig_msg";
    public static final String LOG_HOMEMIG = "log_homemig";

    // v135 — Control Panel
    public static final String CONTROL_PANEL = "control_panel";
    public static final String CP_ENTER_PASSWORD = "cp_enter_password";
    public static final String CP_PASSWORD = "cp_password";
    public static final String CP_WRONG_PASSWORD = "cp_wrong_password";
    public static final String CP_UNLOCK = "cp_unlock";
    public static final String CP_APP_LOCK = "cp_app_lock";
    public static final String CP_APP_LOCK_HINT = "cp_app_lock_hint";
    public static final String CP_CHANGE_PASSWORD = "cp_change_password";
    public static final String CP_CURRENT_PASSWORD = "cp_current_password";
    public static final String CP_NEW_PASSWORD = "cp_new_password";
    public static final String CP_PASSWORD_CHANGED = "cp_password_changed";
    public static final String CP_PASSWORD_MISMATCH = "cp_password_mismatch";
    public static final String CP_APP_SECTION = "cp_app_section";
    public static final String CP_HOME_SECTION = "cp_home_section";
    public static final String CP_APPEARANCE_SECTION = "cp_appearance_section";
    public static final String CP_RUN_SECTION = "cp_run_section";
    public static final String CP_MORE_SECTION = "cp_more_section";
    public static final String CP_APP_LOGO = "cp_app_logo";
    public static final String CP_PICK_IMAGE = "cp_pick_image";
    public static final String CP_LOGO_ZOOM = "cp_logo_zoom";
    public static final String CP_APP_NAME = "cp_app_name";
    public static final String CP_HOME_SLOGAN = "cp_home_slogan";
    public static final String CP_HOME_ROWS = "cp_home_rows";
    public static final String CP_FONT_SIZE = "cp_font_size";
    public static final String CP_SIZE_SMALL = "cp_size_small";
    public static final String CP_SIZE_NORMAL = "cp_size_normal";
    public static final String CP_SIZE_LARGE = "cp_size_large";
    public static final String CP_SIZE_XLARGE = "cp_size_xlarge";
    public static final String CP_LAUNCHER_PRESET = "cp_launcher_preset";
    public static final String CP_ICON_TILE = "cp_icon_tile";
    public static final String CP_ICON_GLYPH = "cp_icon_glyph";
    public static final String CP_ICON_LIGHT = "cp_icon_light";
    public static final String CP_ICON_PRO = "cp_icon_pro";
    public static final String CP_ICON_CHANGED = "cp_icon_changed";
    public static final String CP_ACCENT_COLOR = "cp_accent_color";
    public static final String CP_ACCENT_THEME = "cp_accent_theme";
    public static final String CP_CONTACT_NUMBER = "cp_contact_number";
    public static final String CP_SETTINGS_BACKUP = "cp_settings_backup";
    public static final String CP_EXPORT = "cp_export";
    public static final String CP_IMPORT = "cp_import";
    public static final String CP_EXPORT_DONE = "cp_export_done";
    public static final String CP_IMPORT_CONFIRM_TITLE = "cp_import_confirm_title";
    public static final String CP_IMPORT_CONFIRM_MSG = "cp_import_confirm_msg";
    public static final String CP_IMPORT_DONE = "cp_import_done";
    public static final String CP_IMPORT_NOT_FOUND = "cp_import_not_found";
    public static final String CP_IMPORT_FAILED = "cp_import_failed";
    // v144 — sidebar Backup (Settings Save / Wapas Lao)
    public static final String BACKUP = "backup";
    public static final String BACKUP_SAVE = "backup_save";
    public static final String BACKUP_LOAD = "backup_load";
    public static final String BACKUP_SAVED = "backup_saved";
    public static final String BACKUP_LOADED = "backup_loaded";
    public static final String BACKUP_SAVE_FAILED = "backup_save_failed";

    // v145 — New Project Check (ZIP-add par runner auto-fix ki report)
    public static final String CHECK_TITLE = "check_title";
    public static final String CHECK_BASE_PATH = "check_base_path";
    public static final String CHECK_HISTORY = "check_history";
    public static final String CHECK_DELAYS = "check_delays";
    public static final String CHECK_WAKELOCK = "check_wakelock";
    public static final String CHECK_SCAN = "check_scan";
    public static final String CHECK_GALLERY = "check_gallery";
    public static final String CHECK_FIXED = "check_fixed";
    public static final String CHECK_OK = "check_ok";
    public static final String CHECK_NA = "check_na";
    public static final String CHECK_FAILED = "check_failed";
    public static final String CHECK_GALLERY_CLAUDE = "check_gallery_claude";

    // v145 — More Settings (sidebar ki teen rows ab is apni screen par)
    public static final String MORE_SETTINGS = "more_settings";
    public static final String MORE_SETTINGS_SUB = "more_settings_sub";
    public static final String ORDER_FIX = "order_fix";
    public static final String ORDER_FIX_SUB = "order_fix_sub";
    public static final String SCAN_WAQFA = "scan_waqfa";
    public static final String SCAN_WAQFA_SUB = "scan_waqfa_sub";
    public static final String CP_RESET = "cp_reset";
    public static final String CP_RESET_MSG = "cp_reset_msg";
    public static final String CP_RESET_DONE = "cp_reset_done";
    public static final String CP_PREVIEW_VIDEO = "cp_preview_video";
    public static final String CP_SAVED = "cp_saved";
    public static final String SAVE = "save_btn";

    // v129 — Drop Wallpapers (Ready Assets ke andar), Home live logs, permission popup
    public static final String DROP_WALLPAPERS = "drop_wallpapers";
    public static final String DROP_WALLPAPERS_SUB = "drop_wallpapers_sub";
    public static final String LOG_IDLE = "log_idle";
    public static final String LOG_RUN_START = "log_run_start";
    public static final String LOG_RUN_DONE = "log_run_done";
    public static final String LOG_RUN_FAIL = "log_run_fail";
    public static final String LOG_RUN_CANCEL = "log_run_cancel";
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

    // v132 — manual-move drop folders + Recreate Folders
    public static final String RECREATE_FOLDERS = "recreate_folders";
    public static final String RECREATE_FOLDERS_HINT = "recreate_folders_hint";
    public static final String RECREATE = "recreate";
    public static final String RECREATE_RESULT = "recreate_result";

    // v132 — gallery Add ka Copy/Move popup + persisted setting
    public static final String ADD_N_WALLPAPERS = "add_n_wallpapers";
    public static final String COPY_OR_MOVE_Q = "copy_or_move_q";
    public static final String ADD_MODE = "add_mode";
    public static final String ADD_CHOICE_MANUAL = "add_choice_manual";
    public static final String ADD_CHOICE_AUTO_COPY = "add_choice_auto_copy";
    public static final String ADD_CHOICE_AUTO_MOVE = "add_choice_auto_move";

    // v132 — Preview Ready Assets: Sequence tab + select/delete
    public static final String SEQUENCE_TAB = "sequence_tab";
    public static final String TAP_SELECT_HINT = "tap_select_hint";
    public static final String DELETE_ASSETS_TITLE = "delete_assets_title";
    public static final String DELETE_ASSETS_MSG = "delete_assets_msg";
    public static final String DONE_BTN = "done_btn";

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
        put(BACKGROUND, "Background", "Background");
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
        put(REMOVE_PROJECT, "Remove Project", "Project Remove Karo");
        put(AUTO_SCAN, "Auto Scan", "Auto Scan");
        put(AUTO_SCAN_SUB, "Runs ke baad gallery registration khud ho jati hai",
            "Runs ke baad gallery registration khud ho jati hai");
        put(GALLERY_SCAN, "Gallery Scan", "Gallery Scan");
        put(GALLERY_SCAN_SUB, "Ek dafa abhi scan karo", "Ek dafa abhi scan karo");
        put(GALLERY_SCAN_RUNNING, "Gallery scan is running…", "Gallery scan chal raha hai…");
        put(GALLERY_SCAN_DONE_FMT, "Gallery scan done — %d files registered",
            "Gallery scan ho gaya — %d files register huin");
        put(WAIT_TIME, "Wait Time", "Wait Time");
        put(WAIT_TIME_SUB_FMT, "%d seconds", "%d seconds");
        put(WAIT_TIME_HINT, "Seconds (0 = no wait)", "Seconds likho (0 = koi intezar nahi)");
        put(RESET_DEFAULT, "Reset to Default", "Default par Wapas");
        put(BATCH_NAME, "Batch Name", "Batch ka Naam");
        put(REMOVE_SELECTED, "Remove Selected", "Select Wale Hatao");
        put(REMOVED_TO_BIN_FMT, "%d projects moved to Recycle Bin",
            "%d projects Recycle Bin me chale gaye");
        put(RECYCLE_BIN, "Recycle Bin", "Recycle Bin");
        put(BIN_EMPTY, "Recycle Bin is empty", "Recycle Bin khaali hai");
        put(RESTORE, "Restore", "Wapas Lao");
        put(DELETE_FOREVER, "Delete Forever", "Hamesha ke liye Delete");
        put(REMOVE_PROJECT_CONFIRM_TITLE, "Remove %s?", "%s Remove Karun?");
        put(REMOVE_PROJECT_CONFIRM_MSG,
            "This removes the project's engine folder, its entry in the projects list, and its w%1$d / d%1$d / b%1$d terminal shortcuts.\n\nNOT removed: your TermuxMod/Project %1$d folder and its images — they stay safe.\n\nIf the project came with the app, a fresh copy is installed next time the app opens. If you added it from a ZIP, it stays removed until you add the ZIP again.",
            "Is se project ka engine folder, projects list wali entry aur w%1$d / d%1$d / b%1$d terminal shortcuts remove ho jayenge.\n\nYe remove NAHI hoga: tumhara TermuxMod/Project %1$d folder aur us ki images — wo mehfooz rahengi.\n\nAgar project app ke sath aya tha to agli dafa app khulne par fresh copy lag jayegi. Agar ZIP se add kiya tha to ZIP dobara add karne tak remove rahega.");
        put(REMOVE_BTN, "Remove", "Remove Karo");
        put(REMOVED_TOAST, "%s removed", "%s remove ho gaya");
        put(REMOVE_FAILED, "Project could not be removed.", "Project remove nahi ho saka.");
        put(STATUS_LABEL, "Status", "Status");
        put(STATUS_ALL_EMPTY, "All projects are empty", "Sab projects khali hain");
        put(STATUS_READY_COUNT, "Ready count", "Ready count");
        put(TIME_MODE, "Time Mode", "Time Mode");
        put(TIME_MODE_SUB_FMT, "File times: %s", "Files ki time: %s");
        put(TIME_MODE_PAST, "Past", "Past");
        put(TIME_MODE_FUTURE, "Future", "Future");
        put(TIME_RESET, "Time Reset", "Time Reset");
        put(TIME_RESET_SUB, "Shared time tracker saaf — agli ladder fresh shuru hogi",
            "Shared time tracker saaf — agli ladder fresh shuru hogi");
        put(TIME_RESET_DONE, "Time tracker reset ho gaya", "Time tracker reset ho gaya");

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
        put(ZIP_NO_FOLDER, "No project found in the ZIP: it needs a mockup_project_<N> folder inside, or the ZIP file itself named mockup_project_<N>.zip. Nothing was added.",
            "ZIP me project nahi mila: andar mockup_project_<N> folder hona chahiye, ya ZIP file ka naam mockup_project_<N>.zip ho. Kuch add nahi hua.");
        put(ZIP_NO_SCRIPT, "The project's main runner script could not be detected. Nothing was added.",
            "Project ki main runner script nahi mil saki. Kuch add nahi hua.");
        put(PROJECT_EXISTS, "Project %d already exists. Nothing was changed.",
            "Project %d pehle se hai. Kuch change nahi hua.");

        // Permissions screen
        put(PERM_SUB, "All of these are needed for the app to work properly.",
            "Ye sab cheezein app ke sahi kaam ke liye zaroori hain.");
        put(ALLOW_ALL, "✔  ALLOW ALL", "✔  SAB EK SATH ALLOW KARO");
        put(ALLOW, "Allow", "Allow Karo");
        put(OPEN, "Open", "Kholo");
        // v150: scan built-in hai (BulkApiServer + wrapper files) —
        // Termux:API app ki zaroorat nahi; purana install wala
        // dawa ghalat tha. PERM_API_INSTALL ab sirf repair ki
        // hidayat hai (kharab files app khud theek karti hai).
        put(PERM_API, "Media Scan (Built-in)", "Media Scan (App ke andar)");
        put(PERM_API_INSTALL, "Built into the app — reopen Termux Mod and the scan files repair themselves",
            "Ye app ke andar hi hai — Termux Mod dobara kholo, scan files khud theek ho jayengi");
        put(PERM_API_DONE, "Done", "Done");
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
        put(MOCKUPS_TAB, "Mockups", "Mockups");
        put(WALLPAPERS_TAB, "Wallpapers", "Wallpapers");
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
        put(RUN_VIEW, "Show After Run: Popup / Terminal", "Run ke baad dikhao: Popup / Terminal");
        put(RUN_VIEW_SUB, "When on, the terminal opens — when off, the popup shows; the terminal does the work either way",
            "On ho to terminal khulta hai — off ho to popup dikhta hai; kaam dono haal me terminal hi karta hai");
        put(RUNNING_IN_TERMINAL, "Running in the terminal", "Run terminal me ho raha hai");
        put(SHOWPIECE_EDIT, "Showpiece Edit", "Showpiece Edit");
        put(SHOWPIECE_EDIT_SUB, "Change the name and lines shown on the popup showpiece",
            "Popup showpiece par dikhne wala naam or lines badlo");
        put(SHOWPIECE_NAME_LABEL, "Name on the showpiece", "Showpiece par naam");
        put(SHOWPIECE_NUMBER_LABEL, "Number on the showpiece", "Showpiece par number");
        put(SHOWPIECE_LOADING_LABEL, "Loading line", "Loading wali line");
        put(SHOWPIECE_DONE_LABEL, "Done line", "Done wali line");
        put(SEQ_FIX, "Sequence Fix", "Sequence Fix");
        put(SEQ_FIX_SUB, "Write all three dates in production order and self-check — fixes Gallery/TikTok order without deleting any row",
            "Teeno dates production tartib me likho or khud check karo — koi row delete kiye baghair Gallery/TikTok tartib durust");
        put(ALAG_SECOND, "Har File Alag Second", "Har File Alag Second");
        put(ALAG_SECOND_SUB, "Space registrations 1 second apart so no two files share a second — order can never tie",
            "Har registration 1 second ke waqfe se — do files ek second me kabhi nahi, tartib kabhi tie nahi hoti");
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
        put(RUN_BACKGROUND, "Run in Background", "Background me chalao");
        put(RUN_CANCELLED, "Cancelled", "Cancel Ho Gaya");
        put(RUN_ALREADY_ACTIVE, "A run is already going — let it finish first",
            "Ek run pehle se chal raha hai — pehle use khatam hone do");
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

        // v133
        put(THEMES, "Themes", "Themes");
        put(SYSTEM_PICKER_SUB, "Pick images from Gallery / File Manager",
            "Gallery / File Manager se images chuno");
        put(VIEW_ALL, "View all", "Sab dekho");
        put(TAP_TO_VIEW, "tap to view", "tap karo");
        put(SORT_SECTION, "Sort", "Sort");
        put(GRID_SECTION, "Grid", "Grid");
        put(SCROLL_BUTTON, "Scroll Button", "Scroll Button");
        put(SHOW, "Show", "Dikhao");
        put(HIDE, "Hide", "Chhupao");
        put(MIGRATED_TITLE, "Project Folders Updated", "Project Folders Update Ho Gaye");
        put(MIGRATED_MSG,
            "%d project(s) moved into the TermuxMod folder, so storage now shows only one folder. %d project(s) stayed in the old place — they keep working exactly the same.",
            "%d project(s) TermuxMod folder ke andar shift ho gaye — ab storage me sirf ek hi folder dikhega. %d project(s) purani jagah par hi rahe — wo bilkul pehle ki tarah chalte rahenge.");
        // v134
        put(MOVEDBACK_TITLE, "Project Folders Moved Back", "Project Folders Wapas Aa Gaye");
        put(MOVEDBACK_MSG,
            "%d project(s) moved back to the main storage. %d project(s) stayed inside TermuxMod (already present in the main storage, or an old runner) — they keep working exactly the same. Nothing was deleted.",
            "%d project(s) wapas main storage par shift ho gaye. %d project(s) TermuxMod ke andar hi rahe (main storage par pehle se mojood, ya purana runner) — wo bilkul pehle ki tarah chalte rahenge. Kuch bhi delete nahi hua.");
        put(RECREATE_SETTINGS, "Recreate Settings", "Recreate ki Settings");
        put(RECREATE_FOLDERS_AUTO, "Folders — Auto Recreate", "Folders — Auto Recreate");
        put(RECREATE_VIDEOS_AUTO, "Preview Videos — Auto Recreate", "Preview Videos — Auto Recreate");
        put(RECREATE_AUTO_OFF_NOTE, "OFF = only from Recreate Folders", "OFF = sirf Recreate Folders se banenge");
        put(VIEW_LESS, "View less", "Kam dekho");
        put(LOG_MOVEBACK_STAY_ROOT,
            "Project %d stays in TermuxMod (a folder already exists in the main storage — folders are never merged)",
            "Project %d TermuxMod me hi rahega (root par pehle se folder hai — merge nahi hota)");
        put(LOG_MOVEBACK_STAY_RUNNER,
            "Project %d stays in TermuxMod (old runner — it keeps working exactly as before)",
            "Project %d TermuxMod me hi rahega (runner purana hai — bilkul pehle ki tarah chalega)");
        put(LOG_MOVEBACK_MOVED,
            "Project %d moved back to the main storage (root)",
            "Project %d wapas main storage (root) par shift ho gaya");
        // v136
        put(HOMEMIG_TITLE, "Folders Updated", "Folders Update Ho Gaye");
        put(HOMEMIG_MSG,
            "%d project(s) moved inside the app — storage now shows only the TermuxMod folder with Project folders. %d file(s) kept safe. Nothing was lost.",
            "%d project(s) app ke andar shift ho gaye — ab storage me sirf TermuxMod folder dikhega, us ke andar Project folders. %d file(s) mehfooz rahi. Kuch bhi zaya nahi hua.");
        put(LOG_HOMEMIG,
            "Project %d moved inside the app (hidden engine)",
            "Project %d app ke andar shift ho gaya (chhupa engine)");
        put(LOG_MIRROR_MOCKUPS,
            "Project %d -> %d mockups (Project folder)",
            "Project %d -> %d mockups (Project folder me)");
        put(LOG_MIRROR_READY,
            "Project %d -> %d ready wallpapers (Project folder)",
            "Project %d -> %d ready wallpapers (Project folder me)");
        put(STAGED_DROP_SUB, "Staged images — not added to a project yet",
            "Staged images — abhi kisi project me add nahi hue");

        // v129
        put(DROP_WALLPAPERS, "Drop Wallpapers", "Drop Wallpapers");
        put(DROP_WALLPAPERS_SUB, "Wallpapers dropped into each project",
            "Har project me drop kiye hue wallpapers");
        put(LOG_IDLE, "Ready — logs will appear here live", "Ready — logs yahan live ayenge");
        put(LOG_RUN_START, "▶ %s — run started", "▶ %s — run shuru");
        put(LOG_RUN_DONE, "✔ %s — run complete", "✔ %s — run mukammal");
        put(LOG_RUN_FAIL, "✖ %s — run failed", "✖ %s — run fail");
        put(LOG_RUN_CANCEL, "✖ %s — run cancelled", "✖ %s — run cancel ho gaya");
        put(PERM_POPUP_TITLE, "Permission Needed", "Permission Chahiye");
        put(PERM_POPUP_MSG,
            "Without storage permission, wallpapers cannot be added or run. Please allow it from the Permissions screen.",
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

        // v132 — drop folders + Recreate Folders
        put(RECREATE_FOLDERS, "Recreate Folders", "Folders Dobara Banao");
        put(RECREATE_FOLDERS_HINT,
            "Rebuilds the TermuxMod drop folders and any missing project subfolders. Nothing is ever deleted.",
            "TermuxMod drop folders aur project ke ghayab subfolders dobara banata hai. Kuch bhi delete nahi hota.");
        put(RECREATE, "Recreate", "Dobara Banao");
        put(RECREATE_RESULT, "%1$d new folders created · %2$d already present",
            "%1$d naye folders bane · %2$d pehle se mojood thay");
        put(ADD_N_WALLPAPERS, "Add %1$d Wallpapers", "%1$d Wallpapers Add Karo");
        put(COPY_OR_MOVE_Q, "Copy or Move?", "Copy ya Move?");
        put(ADD_MODE, "Add Mode", "Add Mode");
        put(ADD_CHOICE_MANUAL, "Manual — ask every time", "Manual — har dafa poochho");
        put(ADD_CHOICE_AUTO_COPY, "Auto — Copy", "Auto — Copy");
        put(ADD_CHOICE_AUTO_MOVE, "Auto — Move", "Auto — Move");
        put(SEQUENCE_TAB, "Sequence", "Sequence");
        put(TAP_SELECT_HINT, "Tap a photo to preview · Use Select to pick files for delete",
            "Preview ke liye photo par tap · Delete ke liye Select dabao");
        put(DELETE_ASSETS_TITLE, "Delete Files", "Files Delete Karo");
        put(DELETE_ASSETS_MSG,
            "Delete %d selected file(s)? The files will be removed from the project folder. This cannot be undone.",
            "Select kiye hue %d file delete kar dun? Files project folder se hat jayengi. Ye wapas nahi hoga.");
        put(DONE_BTN, "Done", "Ho Gaya");

        // v135 — Control Panel
        put(CONTROL_PANEL, "Control Panel", "Control Panel");
        put(CP_ENTER_PASSWORD, "Enter Control Panel password", "Control Panel ka password likho");
        put(CP_PASSWORD, "Password", "Password");
        put(CP_WRONG_PASSWORD, "Wrong password", "Ghalat password");
        put(CP_UNLOCK, "Unlock", "Kholo");
        put(CP_APP_LOCK, "App Lock", "App Lock");
        put(CP_APP_LOCK_HINT, "Ask for the password when the app opens", "App khulne par password poochho");
        put(CP_CHANGE_PASSWORD, "Change Password", "Password Badlo");
        put(CP_CURRENT_PASSWORD, "Current Password", "Purana Password");
        put(CP_NEW_PASSWORD, "New Password", "Naya Password");
        put(CP_PASSWORD_CHANGED, "Password changed", "Password badal gaya");
        put(CP_PASSWORD_MISMATCH, "Current password is wrong", "Purana password ghalat hai");
        put(CP_APP_SECTION, "App", "App");
        put(CP_HOME_SECTION, "Home Screen", "Home Screen");
        put(CP_APPEARANCE_SECTION, "Appearance", "Appearance");
        put(CP_RUN_SECTION, "Run & Folders", "Run & Folders");
        put(CP_MORE_SECTION, "More", "More");
        put(CP_APP_LOGO, "App Logo", "App Logo");
        put(CP_PICK_IMAGE, "Pick Image", "Image Chuno");
        put(CP_LOGO_ZOOM, "Logo Size", "Logo Size");
        put(CP_APP_NAME, "App Name", "App Ka Naam");
        put(CP_HOME_SLOGAN, "Home Slogan", "Home Slogan");
        put(CP_HOME_ROWS, "Home Rows — Order & Show/Hide", "Home Rows — Tartib & Show/Hide");
        put(CP_FONT_SIZE, "Font Size", "Font Size");
        put(CP_SIZE_SMALL, "Small", "Chhota");
        put(CP_SIZE_NORMAL, "Normal", "Normal");
        put(CP_SIZE_LARGE, "Large", "Bara");
        put(CP_SIZE_XLARGE, "Extra Large", "Sab Se Bara");
        put(CP_LAUNCHER_PRESET, "Launcher Name & Icon", "Launcher Name & Icon");
        put(CP_ICON_TILE, "Termux Mod", "Termux Mod");
        put(CP_ICON_GLYPH, "Bulk Mockup", "Bulk Mockup");
        put(CP_ICON_LIGHT, "Mockup Studio", "Mockup Studio");
        put(CP_ICON_PRO, "Termux Pro", "Termux Pro");
        put(CP_ICON_CHANGED, "Launcher name & icon changed", "Launcher name & icon badal gaya");
        put(CP_ACCENT_COLOR, "Accent Color", "Accent Color");
        put(CP_ACCENT_THEME, "Theme Default", "Theme Default");
        put(CP_CONTACT_NUMBER, "Contact Admin Number", "Contact Admin Number");
        put(CP_SETTINGS_BACKUP, "Settings Backup", "Settings Backup");
        put(CP_EXPORT, "Export", "Export");
        put(CP_IMPORT, "Import", "Import");
        put(CP_EXPORT_DONE, "Settings saved to %s", "Settings %s par save ho gayin");
        put(CP_IMPORT_CONFIRM_TITLE, "Import Settings", "Settings Import Karo");
        put(CP_IMPORT_CONFIRM_MSG,
            "This will replace all current settings with the backup file. Continue?",
            "Backup file se tamam mojooda settings badal jayengi. Aage barho?");
        put(CP_IMPORT_DONE, "Settings imported", "Settings import ho gayin");
        put(CP_IMPORT_NOT_FOUND, "Backup file not found", "Backup file nahi mili");
        put(CP_IMPORT_FAILED, "Import failed — file is not valid", "Import fail — file durust nahi hai");
        put(BACKUP, "Backup", "Backup");
        put(BACKUP_SAVE, "Settings Save", "Settings Save Karo");
        put(BACKUP_LOAD, "Load Settings", "Settings Wapas Lao (Load)");
        put(BACKUP_SAVED, "Settings saved ✓", "Settings save ho gayin ✓");
        put(BACKUP_LOADED, "Settings loaded ✓", "Settings wapas ah gayin ✓");
        put(BACKUP_SAVE_FAILED, "Save failed", "Save nahi ho saki");

        // v145 — New Project Check report (ZIP-add result)
        put(CHECK_TITLE, "Project Check", "Project Check");
        put(CHECK_BASE_PATH, "Base path", "Base path (rasta)");
        put(CHECK_HISTORY, "Shared history", "Shared history");
        put(CHECK_DELAYS, "Loading delays", "Loading delays");
        put(CHECK_WAKELOCK, "Wakelock stall", "Wakelock stall");
        put(CHECK_SCAN, "Scan (one-by-one)", "Scan (ek ek karke)");
        put(CHECK_GALLERY, "Gallery sequence", "Gallery sequence");
        put(CHECK_FIXED, "Fixed", "Fix kar diya");
        put(CHECK_OK, "Already OK", "Pehle se theek");
        put(CHECK_NA, "Not applied", "Nahi laga");
        put(CHECK_FAILED, "Project check could not run — project installed as-is.",
            "Project check nahi ho saka — project wese hi lag gaya.");
        put(CHECK_GALLERY_CLAUDE, "Gallery sequence not applied — send this project to Muse, he will add it.",
            "Gallery sequence nahi laga — ye project Muse ko bhejo, wo laga de ga");
        put(MORE_SETTINGS, "More Settings", "More Settings");
        put(ORDER_FIX, "Order Fix", "Order Fix");
        put(ORDER_FIX_SUB, "When off, the app will not fix the gallery order of new files",
            "Band karne par app nayi files ka gallery order fix nahi kare ga");
        put(SCAN_WAQFA, "Scan Waqfa", "Scan Waqfa");
        put(SCAN_WAQFA_SUB, "When off, the terminal runs at its old fast speed — the app fixes the order",
            "OFF karne par terminal purani tez speed par chalta hai, order fix app kare ga");
        put(MORE_SETTINGS_SUB, "Backup, Export Project and Output Folder",
            "Backup, Export Project aur Output Folder");
        put(CP_RESET, "Reset to Defaults", "Defaults Par Wapas");
        put(CP_RESET_MSG,
            "All settings will return to factory defaults. The Control Panel password will also reset to the default. Continue?",
            "Tamam settings factory defaults par wapas chali jayengi. Control Panel ka password bhi default ho jayega. Aage barho?");
        put(CP_RESET_DONE, "Settings reset to defaults", "Settings defaults par wapas ho gayin");
        put(CP_PREVIEW_VIDEO, "Preview Video", "Preview Video");
        put(CP_SAVED, "Saved", "Save ho gaya");
        put(SAVE, "Save", "Save Karo");
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
