import customtkinter as ctk
from tkinter import messagebox, filedialog
import subprocess
import threading
import os
import sys
import time
import datetime
import socket
import concurrent.futures
import re
try:
    import keyboard
except ImportError:
    keyboard = None
from PIL import Image

# --- CONFIGURAÇÃO GERAL ---
ctk.set_appearance_mode("Dark")

# Cores Estáticas
COLOR_BG = "#0A0A0A"
COLOR_SURFACE = "#171717"
COLOR_BORDER = "#333333"
COLOR_TEXT_MAIN = "#FAFAFA"
COLOR_TEXT_DIM = "#A1A1AA"
COLOR_SUCCESS = "#10B981"
COLOR_ERROR = "#EF4444"
COLOR_HOVER = "#262626"

# Temas
THEMES = {
    "Studio Blue": "#00E5FF",
    "ROG Red": "#b91c1c",
    "Razer Green": "#22c55e",
    "Cyberpunk Purple": "#d946ef"
}

# Fontes
FONT_MAIN = ("Roboto Medium", 13)
FONT_BOLD = ("Roboto", 13, "bold")
FONT_TITLE = ("Roboto", 20, "bold")
FONT_MONO = ("Consolas", 11)

# --- TRADUÇÕES ---
TRANSLATIONS = {
    "PT": {
        "app_title": "TURBO CORE V106 - STABILITY POLISH",
        "sidebar_dash": "🖥️ DASHBOARD",
        "sidebar_game": "🎮 COMPETITIVO",
        "sidebar_apps": "📦 APPS",
        "sidebar_term": "💻 TERMINAL",
        "status_searching": "Buscando...",
        "status_disconnected": "❌ DESCONECTADO",
        "sys_ready": "SISTEMA PRONTO",
        "launch_comp": "🚀 IR PARA MODOS COMPETITIVOS",
        "card_pc": "EXPERIÊNCIA PC",
        "card_perf": "CONTROLE DE PERFORMANCE",
        "card_bat": "GERENCIADOR DE BATERIA",
        "card_utils": "FERRAMENTAS DO SISTEMA",
        "opt_video": "Gravar Vídeo",
        "opt_audio": "Gravar Áudio",
        "opt_ghost": "Modo Fantasma (Tela Off)",
        "btn_install": "Instalar APK 📥",
        "btn_send": "Enviar Arquivo 📤",
        "btn_reset": "Restaurar Original 🔄",
        "btn_kill": "⚡ LIMPAR RAM",
        "select_default": "Selecionar...",
        "hero_ff": "ATIVAR MODO FREE FIRE MAX 🎯",
        "sw_keymap": "Ativar Keymapping (WASD)",
        "coming_soon": "EM BREVE (Roadmap)",
        "term_manual": "TERMINAL MANUAL",
        "btn_exec": "EXECUTAR",
        "conn_wizard": "Assistente de Conexão",
        "tools": "FERRAMENTAS",
        "pair_step": "1. PAREAMENTO (Wireless)",
        "conn_step": "2. CONEXÃO (ADB Connect)",
        "btn_pair": "PAREAR DISPOSITIVO",
        "btn_connect": "CONECTAR",
        "btn_scan": "ESCANEAR REDE 🔎",
        "guide_title": "COMO CONECTAR?",
        "guide_text": "1. Ative Opções do Desenvolvedor\n2. Ative Depuração USB\n3. Ative Depuração Sem Fio\n\nNo Wi-Fi:\nUse 'Parear com Código'.\nCopie IP, Porta e Código.",
        "msg_success": "SUCESSO",
        "msg_error": "ERRO",
        "msg_conn_error": "ERRO DE CONEXÃO",
        "msg_no_device": "Nenhum dispositivo conectado!\n\n1. Conecte o cabo USB ou Wi-Fi.\n2. Verifique se o status está verde no topo.",
        "msg_cmd_success": "Modo Aplicado com Sucesso!",
        "msg_restored": "Dispositivo restaurado!",
        "msg_installed": "APK Instalado!",
        "msg_sent": "Arquivo enviado!",
        "msg_ff_active": "MODO FREE FIRE ATIVADO!",
        "msg_kill": "Processos de fundo encerrados!",
        "theme_label": "Theme / Tema",
        "lang_label": "Lang / Idioma",
        "apps_search": "Buscar pacote...",
        "apps_refresh": "🔄 Recarregar",
        "apps_loading": "Carregando apps...",
        "action_open": "Abrir",
        "action_kill": "Parar",
        "action_del": "Del",
        "tab_manual": "MANUAL",
        "tab_logcat": "LOGCAT (MATRIX)",
        "btn_start_log": "▶ INICIAR LEITURA",
        "btn_stop_log": "⏹ PARAR",
        # HELP TEXTS
        "help_pc_lite": "MODO ECONÔMICO (SAFE)\n\n• Resolução: 540p (Baixa)\n• FPS: 30 Travado\n• Risco: NENHUM.\n\nIdeal para leitura e tarefas simples. Economiza bateria e mantém o celular frio.",
        "help_pc_sob": "MODO SOBERANO (PADRÃO)\n\n• Resolução: 720p (HD)\n• Densidade: 160 DPI\n• Risco: BAIXO.\n\nTransforma o celular em um monitor secundário funcional. Melhor equilíbrio entre qualidade e performance.",
        "help_pc_gamer": "MODO GAMER (ULTRA)\n\n• Resolução: 720p (Destravada)\n• FPS: Ilimitado (Máximo)\n• Latência: Mínima (Buffer Reduzido)\n\n⚠️ ALERTA DE AQUECIMENTO: O processador vai rodar no máximo. Monitoramento de temperatura recomendado.",
        "help_perf_gos": "GAMER ULTIMATE (MOBILE)\n\n• Ação: Desabilita GOS (Samsung Game Optimizing Service)\n• Risco: MÉDIO/ALTO.\n\n⚠️ Remove limitadores de software da fabricante. Pode causar aquecimento excessivo em sessões longas.",
        "help_perf_ult": "ULTIMATE DESEMPENHO (BRUTO)\n\n• Ação: DESLIGA SENSORES TÉRMICOS e mata processos.\n• Risco: CRÍTICO ☠️\n\n⚠️ PERIGO: O celular NÃO vai desligar se superaquecer. Risco real de dano físico ao processador/bateria. Use cooler externo obrigatoriamente.",
        "help_perf_std": "USUAL TURBO\n\n• Ação: Acelera animações do Android (0.5x).\n• Risco: NENHUM.\n\nDeixa a navegação mais rápida visualmente sem forçar o hardware.",
        "help_bat_eco": "ECONOMIA NORMAL\n\n• Ação: Ativa modo Low Power nativo.\n• Risco: NENHUM.\n\nGerenciamento padrão do Android.",
        "help_bat_sup": "SUPER ECONOMIA\n\n• Ação: Desliga Bluetooth, Sync e reduz resolução.\n• Risco: BAIXO.\n\nVocê deixará de receber notificações de alguns apps.",
        "help_bat_ult": "ULTIMATE ECONOMIA (DEEP)\n\n• Ação: Brilho Zero, Mata Apps, Limita CPU.\n• Risco: USABILIDADE.\n\n⚠️ O celular vira um 'tijolo' para sobreviver. A tela ficará quase apagada. Só use em emergências."
    },
    "EN": {
        "app_title": "TURBO CORE V106 - STABILITY POLISH",
        "sidebar_dash": "🖥️ DASHBOARD",
        "sidebar_game": "🎮 COMPETITIVE",
        "sidebar_apps": "📦 APPS",
        "sidebar_term": "💻 TERMINAL",
        "status_searching": "Searching...",
        "status_disconnected": "❌ DISCONNECTED",
        "sys_ready": "SYSTEM READY",
        "launch_comp": "🚀 LAUNCH COMPETITIVE MODE",
        "card_pc": "PC EXPERIENCE",
        "card_perf": "PERFORMANCE CONTROL",
        "card_bat": "BATTERY MANAGER",
        "card_utils": "SYSTEM TOOLS",
        "opt_video": "Record Video",
        "opt_audio": "Record Audio",
        "opt_ghost": "Ghost Mode (Screen Off)",
        "btn_install": "Install APK 📥",
        "btn_send": "Send File 📤",
        "btn_reset": "Factory Reset 🔄",
        "btn_kill": "⚡ KILL ALL",
        "select_default": "Select...",
        "hero_ff": "ACTIVATE FREE FIRE MODE 🎯",
        "sw_keymap": "Enable Keymapping (WASD)",
        "coming_soon": "COMING SOON (Roadmap)",
        "term_manual": "MANUAL TERMINAL",
        "btn_exec": "EXECUTE",
        "conn_wizard": "Connection Wizard",
        "tools": "TOOLS",
        "pair_step": "1. PAIRING (Wireless)",
        "conn_step": "2. CONNECTION (ADB Connect)",
        "btn_pair": "PAIR DEVICE",
        "btn_connect": "CONNECT",
        "btn_scan": "SCAN NETWORK 🔎",
        "guide_title": "QUICK GUIDE",
        "guide_text": "1. Enable Developer Options\n2. Enable USB Debugging\n3. Enable Wireless Debugging\n\nFor Wi-Fi:\nUse 'Pair with Code'.\nCopy IP, Port & Code.",
        "msg_success": "SUCCESS",
        "msg_error": "ERROR",
        "msg_conn_error": "CONNECTION ERROR",
        "msg_no_device": "No device connected!\n\n1. Check USB/Wi-Fi.\n2. Check status header.",
        "msg_cmd_success": "Mode Applied Successfully!",
        "msg_restored": "Device restored!",
        "msg_installed": "APK Installed!",
        "msg_sent": "File sent!",
        "msg_ff_active": "FREE FIRE MODE ACTIVE!",
        "msg_kill": "Background processes killed!",
        "theme_label": "Theme",
        "lang_label": "Language",
        "apps_search": "Search package...",
        "apps_refresh": "🔄 Refresh",
        "apps_loading": "Loading apps...",
        "action_open": "Open",
        "action_kill": "Kill",
        "action_del": "Del",
        "tab_manual": "MANUAL",
        "tab_logcat": "LOGCAT (MATRIX)",
        "btn_start_log": "▶ START LOGCAT",
        "btn_stop_log": "⏹ STOP",
        # HELP TEXTS EN
        "help_pc_lite": "ECONOMY MODE (SAFE)\n\n• Res: 540p\n• FPS: 30 Locked\n• Risk: NONE.\n\nSaves battery, keeps device cool.",
        "help_pc_sob": "SOVEREIGN MODE (STD)\n\n• Res: 720p\n• Density: 160 DPI\n• Risk: LOW.\n\nTurns phone into a functional secondary monitor.",
        "help_pc_gamer": "GAMER MODE (ULTRA)\n\n• Res: 720p Unlocked\n• FPS: Max\n• Latency: Min\n\n⚠️ HEATING ALERT: Processor runs at max. Monitor temps.",
        "help_perf_gos": "GAMER ULTIMATE\n\n• Action: Disables Samsung GOS\n• Risk: MED/HIGH.\n\n⚠️ Removes software limits. May cause heating.",
        "help_perf_ult": "ULTIMATE PERFORMANCE\n\n• Action: DISABLES THERMAL SENSORS.\n• Risk: CRITICAL ☠️\n\n⚠️ DANGER: Phone will NOT shutdown on overheat. Real hardware risk.",
        "help_perf_std": "USUAL TURBO\n\n• Action: Faster animations (0.5x).\n• Risk: NONE.",
        "help_bat_eco": "NORMAL SAVER\n\n• Action: Native Low Power Mode.\n• Risk: NONE.",
        "help_bat_sup": "SUPER SAVER\n\n• Action: No Bluetooth/Sync.\n• Risk: LOW.",
        "help_bat_ult": "ULTIMATE SAVER\n\n• Action: Zero Brightness, Kill Apps.\n• Risk: USABILITY.\n\n⚠️ Phone becomes barely usable to survive."
    },
    "ES": {
        "app_title": "TURBO CORE V106 - STABILITY POLISH",
        "sidebar_dash": "🖥️ PANEL",
        "sidebar_game": "🎮 COMPETITIVO",
        "sidebar_apps": "📦 APPS",
        "sidebar_term": "💻 TERMINAL",
        "status_searching": "Buscando...",
        "status_disconnected": "❌ DESCONECTADO",
        "sys_ready": "SISTEMA LISTO",
        "launch_comp": "🚀 IR A MODO COMPETITIVO",
        "card_pc": "EXPERIENCIA PC",
        "card_perf": "CONTROL DE RENDIMIENTO",
        "card_bat": "GESTOR DE BATERÍA",
        "card_utils": "HERRAMIENTAS",
        "opt_video": "Grabar Video",
        "opt_audio": "Grabar Audio",
        "opt_ghost": "Modo Fantasma (Pantalla Off)",
        "btn_install": "Instalar APK 📥",
        "btn_send": "Enviar Archivo 📤",
        "btn_reset": "Restaurar Original 🔄",
        "btn_kill": "⚡ LIMPIAR RAM",
        "select_default": "Seleccionar...",
        "hero_ff": "ACTIVAR MODO FREE FIRE 🎯",
        "sw_keymap": "Activar Keymapping (WASD)",
        "coming_soon": "PRÓXIMAMENTE (Roadmap)",
        "term_manual": "TERMINAL MANUAL",
        "btn_exec": "EJECUTAR",
        "conn_wizard": "Asistente de Conexión",
        "tools": "HERRAMIENTAS",
        "pair_step": "1. VINCULACIÓN (Inalámbrica)",
        "conn_step": "2. CONEXIÓN (ADB Connect)",
        "btn_pair": "VINCULAR DISPOSITIVO",
        "btn_connect": "CONECTAR",
        "btn_scan": "ESCANEAR RED 🔎",
        "guide_title": "GUÍA RÁPIDA",
        "guide_text": "1. Activar Opciones Desarrollador\n2. Activar Depuración USB\n3. Activar Depuración Inalámbrica\n\nPara Wi-Fi:\nUsar 'Vincular con Código'.\nCopiar IP, Puerto y Código.",
        "msg_success": "ÉXITO",
        "msg_error": "ERROR",
        "msg_conn_error": "ERROR DE CONEXIÓN",
        "msg_no_device": "¡No hay dispositivo!\n\n1. Verifique USB/Wi-Fi.\n2. Verifique estado.",
        "msg_cmd_success": "¡Modo Aplicado con Éxito!",
        "msg_restored": "¡Dispositivo restaurado!",
        "msg_installed": "¡APK Instalado!",
        "msg_sent": "¡Archivo enviado!",
        "msg_ff_active": "¡MODO FREE FIRE ACTIVO!",
        "msg_kill": "¡Procesos cerrados!",
        "theme_label": "Tema",
        "lang_label": "Idioma",
        "apps_search": "Buscar paquete...",
        "apps_refresh": "🔄 Recargar",
        "apps_loading": "Cargando apps...",
        "action_open": "Abrir",
        "action_kill": "Parar",
        "action_del": "Del",
        "tab_manual": "MANUAL",
        "tab_logcat": "LOGCAT (MATRIX)",
        "btn_start_log": "▶ INICIAR",
        "btn_stop_log": "⏹ DETENER",
        # HELP TEXTS ES
        "help_pc_lite": "MODO ECONÓMICO (SAFE)\n\n• Res: 540p\n• FPS: 30 Fijo\n• Riesgo: NINGUNO.\n\nAhorra batería, mantiene frío.",
        "help_pc_sob": "MODO SOBERANO (STD)\n\n• Res: 720p\n• Densidad: 160 DPI\n• Riesgo: BAJO.\n\nMonitor secundario funcional.",
        "help_pc_gamer": "MODO GAMER (ULTRA)\n\n• Res: 720p Desbloq.\n• FPS: Max\n• Latencia: Min\n\n⚠️ ALERTA CALOR: CPU al máximo. Monitorear temp.",
        "help_perf_gos": "GAMER ULTIMATE\n\n• Acción: Desactiva Samsung GOS\n• Riesgo: MEDIO/ALTO.\n\n⚠️ Quita límites de software. Puede calentar.",
        "help_perf_ult": "ULTIMATE RENDIMIENTO\n\n• Acción: DESACTIVA SENSORES TÉRMICOS.\n• Riesgo: CRÍTICO ☠️\n\n⚠️ PELIGRO: No se apagará por calor. Riesgo físico.",
        "help_perf_std": "USUAL TURBO\n\n• Acción: Animaciones rápidas (0.5x).\n• Riesgo: NINGUNO.",
        "help_bat_eco": "AHORRO NORMAL\n\n• Acción: Low Power Mode nativo.\n• Riesgo: NINGUNO.",
        "help_bat_sup": "SUPER AHORRO\n\n• Acción: Sin Bluetooth/Sync.\n• Riesgo: BAJO.",
        "help_bat_ult": "ULTIMATE AHORRO\n\n• Acción: Brillo Cero, Mata Apps.\n• Riesgo: USABILIDAD.\n\n⚠️ Casi inutilizable para sobrevivir."
    }
}

MODOS_PC = {
    "PC Lite (Econômico)": {"size": "540x960", "density": "120", "scrcpy": ["--video-bit-rate=4M", "--max-fps=30", "--fullscreen", "--window-borderless", "--always-on-top", "--window-title=TURBO_LITE"], "help_key": "help_pc_lite"},
    "PC Soberano (Padrão)": {"size": "720x1280", "density": "160", "scrcpy": ["--video-bit-rate=8M", "--max-fps=60", "--fullscreen", "--window-borderless", "--always-on-top", "--window-title=TURBO_SOBERANO"], "help_key": "help_pc_sob"},
    "PC Gamer (Ultra)": {"size": "720x1280", "density": "160", "scrcpy": ["--video-bit-rate=16M", "--max-fps=0", "--fullscreen", "--window-borderless", "--audio-buffer=20", "--window-title=TURBO_GAMER"], "help_key": "help_pc_gamer"}
}

MODOS_PERF = {
    "Gamer Ultimate (Mobile)": {"cmd": "wm size 432x960; wm density 160; pm disable-user --user 0 com.samsung.android.game.gos", "help_key": "help_perf_gos"},
    "Ultimate Desempenho (Bruto)": {"cmd": "settings put global power_manager_constants disable_thermal_control=true; wm size 360x800; wm density 120; am kill-all", "help_key": "help_perf_ult"},
    "Usual Turbo (Dia a Dia)": {"cmd": "wm size reset; wm density reset; settings put global window_animation_scale 0.5; settings put global transition_animation_scale 0.5", "help_key": "help_perf_std"}
}

MODOS_BAT = {
    "Economia Normal": {"cmd": "settings put global low_power 1; settings put global adaptive_battery_management_enabled 1", "help_key": "help_bat_eco"},
    "Super Economia": {"cmd": "wm size 576x1280; wm density 240; svc bluetooth disable; settings put global master_sync_enabled 0", "help_key": "help_bat_sup"},
    "Ultimate Economia (Deep)": {"cmd": "wm size 360x800; wm density 120; settings put system screen_brightness 0; am kill-all; settings put global low_power 1", "help_key": "help_bat_ult"}
}

class TurboCoreApp(ctk.CTk):
    def __init__(self):
        super().__init__()
        self.current_lang = "PT"
        self.current_theme = "Studio Blue"
        self.accent_color = THEMES[self.current_theme]
        self.all_apps_cache = []
        self.logcat_process = None
        self.stop_logcat_flag = False
        self.last_ip = "" # V104 Smart Reconnect

        self.debug_log(f"--- INICIANDO TURBO CORE V106 STABILITY POLISH [{self.current_lang}] ---")
        self.title(self.T("app_title"))
        self.geometry("900x750")
        self.resizable(False, True)
        self.configure(fg_color=COLOR_BG)
        self.target_device = ""
        self.device_model = "Desconhecido"

        if getattr(sys, 'frozen', False):
            self.app_dir = os.path.dirname(sys.executable)
        else:
            self.app_dir = os.path.dirname(os.path.abspath(__file__))
        self.bin_dir = os.path.join(self.app_dir, "bin")
        self.caps_dir = os.path.join(self.app_dir, "Capturas")

        self.check_binaries()

        if not os.path.exists(self.caps_dir):
            os.makedirs(self.caps_dir)

        try:
            img_path = os.path.join(self.app_dir, "fundo_chip.jpg")
            self.img_bg = ctk.CTkImage(Image.open(img_path), size=(900, 750))
        except Exception as e:
            self.debug_log(f"Erro imagem fundo: {e}")
            self.img_bg = None

        self.setup_ui()
        self.start_monitor()
        self.start_battery_monitor()
        self.setup_hotkeys()
        self.keymapping_active = False

    def T(self, key):
        return TRANSLATIONS[self.current_lang].get(key, key)

    def check_binaries(self):
        self.debug_log("Verificando binários...")
        if not os.path.exists(self.bin_dir):
            messagebox.showwarning("ERRO CRITICO", "Pasta 'bin' não encontrada!")
            return

        adb_path = os.path.join(self.bin_dir, "adb.exe")
        scrcpy_path = os.path.join(self.bin_dir, "scrcpy.exe")

        if not os.path.exists(adb_path) or not os.path.exists(scrcpy_path):
            messagebox.showwarning("ARQUIVOS FALTANDO", "adb.exe ou scrcpy.exe faltando em bin/")

    def setup_hotkeys(self):
        if keyboard:
            try:
                keyboard.add_hotkey('f1', lambda: self.iniciar_pc("PC Soberano (Padrão)"))
                keyboard.add_hotkey('f2', lambda: self.ativar_free_fire())
                self.hook_space = keyboard.on_press_key("space", self.key_tap_handler, suppress=False)
                for k in ["w", "a", "s", "d"]:
                    keyboard.on_press_key(k, lambda e, key=k: self.key_swipe_handler(key), suppress=False)
            except Exception as e: self.debug_log(f"Key error: {e}")

    def key_tap_handler(self, event):
        if self.keymapping_active and self.target_device:
            threading.Thread(target=lambda: self.run_adb_generic("shell input tap 640 360")).start()

    def key_swipe_handler(self, key):
        if self.keymapping_active and self.target_device:
            cx, cy = 200, 500
            dist, dur = 100, 200
            x1, y1, x2, y2 = cx, cy, cx, cy
            if key == "w": y2 -= dist
            elif key == "s": y2 += dist
            elif key == "a": x2 -= dist
            elif key == "d": x2 += dist
            threading.Thread(target=lambda: self.run_adb_generic(f"shell input swipe {x1} {y1} {x2} {y2} {dur}")).start()

    def toggle_keymapping(self):
        self.keymapping_active = not self.keymapping_active
        self.log(f"Keymapping: {self.keymapping_active}")

    def change_language(self, lang):
        self.current_lang = lang
        self.refresh_ui()

    def change_theme(self, theme):
        self.current_theme = theme
        self.accent_color = THEMES[theme]
        self.refresh_ui()

    def refresh_ui(self):
        self.debug_log("Refreshing UI...")
        for widget in self.winfo_children():
            if isinstance(widget, ctk.CTkToplevel): continue
            widget.destroy()
        self.setup_ui()

    def setup_ui(self):
        self.sidebar = ctk.CTkFrame(self, width=200, corner_radius=0, fg_color=COLOR_SURFACE, border_width=0)
        self.sidebar.pack(side="left", fill="y", expand=False)
        self.sidebar.pack_propagate(False)

        ctk.CTkLabel(self.sidebar, text="TURBO\nCORE", font=("Montserrat", 24, "bold"), text_color=self.accent_color).pack(pady=(40, 5))
        ctk.CTkLabel(self.sidebar, text="V106 POLISH", font=("Roboto", 10), text_color=COLOR_TEXT_DIM).pack(pady=(0, 20))

        self.btn_dash = self.create_sidebar_btn(self.T("sidebar_dash"), "dash")
        self.btn_special = self.create_sidebar_btn(self.T("sidebar_game"), "special")
        self.btn_apps = self.create_sidebar_btn(self.T("sidebar_apps"), "apps")
        self.btn_term = self.create_sidebar_btn(self.T("sidebar_term"), "term")

        self.sidebar_footer = ctk.CTkFrame(self.sidebar, fg_color="transparent")
        self.sidebar_footer.pack(side="bottom", fill="x", padx=10, pady=(20, 60))

        ctk.CTkLabel(self.sidebar_footer, text=self.T("theme_label"), font=("Roboto", 9), text_color=COLOR_TEXT_DIM).pack(pady=(0, 2))
        theme_menu = ctk.CTkOptionMenu(self.sidebar_footer, values=list(THEMES.keys()), width=160,
                                       fg_color=COLOR_BG, button_color=COLOR_BORDER,
                                       command=self.change_theme)
        theme_menu.set(self.current_theme)
        theme_menu.pack(pady=(0, 15))

        ctk.CTkLabel(self.sidebar_footer, text=self.T("lang_label"), font=("Roboto", 9), text_color=COLOR_TEXT_DIM).pack(pady=(0, 2))
        lang_menu = ctk.CTkOptionMenu(self.sidebar_footer, values=["PT", "EN", "ES"], width=160,
                                      fg_color=COLOR_BG, button_color=COLOR_BORDER,
                                      command=self.change_language)
        lang_menu.set(self.current_lang)
        lang_menu.pack(pady=(0, 5))

        self.right_panel = ctk.CTkFrame(self, fg_color="transparent")
        self.right_panel.pack(side="right", fill="both", expand=True)

        self.header = ctk.CTkFrame(self.right_panel, height=70, corner_radius=0, fg_color="transparent")
        self.header.pack(fill="x", side="top", padx=30, pady=(20, 0))

        self.frame_dev_info = ctk.CTkFrame(self.header, fg_color="transparent")
        self.frame_dev_info.pack(side="left")

        self.btn_device_status = ctk.CTkButton(self.frame_dev_info, text=self.T("status_searching"), font=FONT_BOLD,
                                               fg_color=COLOR_SURFACE, text_color=COLOR_TEXT_DIM,
                                               width=180, height=36, corner_radius=18,
                                               border_width=1, border_color=COLOR_BORDER,
                                               hover_color=COLOR_HOVER,
                                               command=self.abrir_gerenciador_conexao)
        self.btn_device_status.pack(side="left")

        self.lbl_stats = ctk.CTkLabel(self.frame_dev_info, text="", font=FONT_MONO, text_color=COLOR_TEXT_DIM)
        self.lbl_stats.pack(side="left", padx=15)

        self.btn_refresh = ctk.CTkButton(self.frame_dev_info, text="↺", width=36, height=36,
                                         fg_color=COLOR_SURFACE, text_color=COLOR_TEXT_MAIN,
                                         hover_color=COLOR_HOVER, corner_radius=18,
                                         border_width=1, border_color=COLOR_BORDER,
                                         command=self.force_refresh)
        self.btn_refresh.pack(side="left", padx=5)

        self.btn_kill = ctk.CTkButton(self.header, text="🚀", width=40, height=36,
                                      fg_color="#EF4444", text_color="white", hover_color="#991B1B",
                                      corner_radius=10, font=FONT_BOLD, command=self.kill_all_processes)
        self.btn_kill.pack(side="right", padx=10)

        self.main_area = ctk.CTkFrame(self.right_panel, fg_color="transparent")
        self.main_area.pack(fill="both", expand=True, padx=30, pady=20)
        
        self.frames = {
            "dash": ctk.CTkFrame(self.main_area, fg_color="transparent"),
            "special": ctk.CTkFrame(self.main_area, fg_color="transparent"),
            "apps": ctk.CTkFrame(self.main_area, fg_color="transparent"),
            "term": ctk.CTkFrame(self.main_area, fg_color="transparent")
        }
        
        if self.img_bg:
            for f in self.frames.values():
                ctk.CTkLabel(f, text="", image=self.img_bg).place(x=0, y=0, relwidth=1, relheight=1)

        self.build_dashboard(self.frames["dash"])
        self.build_special(self.frames["special"])
        self.build_apps(self.frames["apps"])
        self.build_terminal(self.frames["term"])
        
        self.current_frame = None
        self.switch_tab("dash")
        
        self.lbl_system_status = ctk.CTkLabel(self.right_panel, text=self.T("sys_ready"), font=("Roboto", 9), text_color="#333")
        self.lbl_system_status.pack(fill="x", side="bottom", pady=5)

    def create_sidebar_btn(self, text, mode):
        btn = ctk.CTkButton(self.sidebar, text=text, fg_color="transparent", font=FONT_BOLD, anchor="w",
                            height=45, corner_radius=8, hover_color=COLOR_HOVER,
                            command=lambda: self.switch_tab(mode))
        btn.pack(fill="x", padx=15, pady=5)
        return btn

    def switch_tab(self, mode):
        if self.current_frame: self.current_frame.pack_forget()
        self.current_frame = self.frames[mode]
        self.current_frame.pack(fill="both", expand=True)

        btns = {"dash": self.btn_dash, "special": self.btn_special, "apps": self.btn_apps, "term": self.btn_term}
        for k, b in btns.items():
            if k == mode:
                b.configure(fg_color=COLOR_HOVER, text_color=self.accent_color, border_width=1, border_color=COLOR_BORDER)
            else:
                b.configure(fg_color="transparent", text_color=COLOR_TEXT_DIM, border_width=0)

    def abrir_gerenciador_conexao(self):
        toplevel = ctk.CTkToplevel(self)
        toplevel.title(self.T("conn_wizard"))
        toplevel.geometry("650x450")
        toplevel.configure(fg_color=COLOR_BG)
        toplevel.attributes("-topmost", True)

        frame_tools = ctk.CTkFrame(toplevel, fg_color=COLOR_BG)
        frame_tools.pack(side="left", fill="both", expand=True, padx=20, pady=20)

        frame_tutorial = ctk.CTkFrame(toplevel, fg_color=COLOR_SURFACE, corner_radius=12)
        frame_tutorial.pack(side="right", fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame_tools, text=self.T("tools"), font=FONT_BOLD, text_color=COLOR_TEXT_MAIN).pack(pady=10, anchor="w")

        ctk.CTkLabel(frame_tools, text=self.T("pair_step"), font=FONT_MAIN, text_color=self.accent_color).pack(pady=(10,5), anchor="w")
        ip_pair_entry = ctk.CTkEntry(frame_tools, placeholder_text="IP:PORT", fg_color=COLOR_SURFACE, border_color=COLOR_BORDER)
        ip_pair_entry.pack(fill="x", pady=5)
        code_entry = ctk.CTkEntry(frame_tools, placeholder_text="CODE", fg_color=COLOR_SURFACE, border_color=COLOR_BORDER)
        code_entry.pack(fill="x", pady=5)

        def do_pair():
            addr = ip_pair_entry.get(); code = code_entry.get()
            if addr and code: threading.Thread(target=lambda: self.run_adb_generic(f"pair {addr} {code}")).start()

        ctk.CTkButton(frame_tools, text=self.T("btn_pair"), fg_color="transparent", border_width=1, border_color=self.accent_color,
                      text_color=self.accent_color, hover_color=COLOR_HOVER, command=do_pair).pack(fill="x", pady=5)

        ctk.CTkLabel(frame_tools, text=self.T("conn_step"), font=FONT_MAIN, text_color=self.accent_color).pack(pady=(20,5), anchor="w")
        ip_conn_entry = ctk.CTkEntry(frame_tools, placeholder_text="IP:PORT", fg_color=COLOR_SURFACE, border_color=COLOR_BORDER)
        ip_conn_entry.pack(fill="x", pady=5)

        def do_connect():
            addr = ip_conn_entry.get()
            if addr: threading.Thread(target=lambda: self.run_adb_generic(f"connect {addr}")).start()

        ctk.CTkButton(frame_tools, text=self.T("btn_connect"), fg_color=self.accent_color, text_color=COLOR_BG, hover_color=COLOR_TEXT_MAIN,
                      command=do_connect).pack(fill="x", pady=5)

        # RESTORED V106: Network Scan
        ctk.CTkLabel(frame_tools, text="3. AUTO-SCAN", font=FONT_MAIN, text_color=self.accent_color).pack(pady=(20,5), anchor="w")
        ctk.CTkButton(frame_tools, text=self.T("btn_scan"), fg_color="transparent", border_width=1, border_color=COLOR_TEXT_DIM,
                      text_color=COLOR_TEXT_DIM, hover_color=COLOR_HOVER, command=self.scan_network).pack(fill="x", pady=5)

        ctk.CTkLabel(frame_tutorial, text=self.T("guide_title"), font=FONT_BOLD, text_color=COLOR_TEXT_MAIN).pack(pady=10)
        ctk.CTkLabel(frame_tutorial, text=self.T("guide_text"), justify="left", font=FONT_MAIN, text_color=COLOR_TEXT_DIM).pack(padx=15, pady=10)

    # V106 RESTORED METHOD
    def scan_network(self):
        self.log("Scanning local network for ADB devices (Port 5555)...")

        def run_scan():
            try:
                local_ip = socket.gethostbyname(socket.gethostname())
                subnet = '.'.join(local_ip.split('.')[:-1]) + '.'
                found = []

                with concurrent.futures.ThreadPoolExecutor(max_workers=50) as executor:
                    futures = {executor.submit(self._check_ip, f"{subnet}{i}"): f"{subnet}{i}" for i in range(1, 255)}
                    for future in concurrent.futures.as_completed(futures):
                        ip = futures[future]
                        if future.result():
                            found.append(ip)

                if found:
                    self.log(f"Found: {', '.join(found)}")
                    # Assuming ip_conn_entry exists in the toplevel scope? No, it's local.
                    # We can't easily update the toplevel entry from here without refactoring.
                    # Fallback: Just log it. User can copy.
                    messagebox.showinfo("Scan Result", f"Devices Found:\n{', '.join(found)}\n\n(Copy IP to Connect field)")
                else:
                    self.log("No devices found.")
                    messagebox.showinfo("Scan Result", "No devices found on port 5555.")
            except Exception as e:
                self.debug_log(f"Scan Error: {e}")

        threading.Thread(target=run_scan).start()

    def _check_ip(self, ip):
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(0.2) # Fast timeout
            result = sock.connect_ex((ip, 5555))
            sock.close()
            return result == 0
        except: return False

    def build_dashboard(self, p):
        btn_comp = ctk.CTkButton(p, text=self.T("launch_comp"), font=FONT_BOLD,
                      fg_color=COLOR_SURFACE, border_width=1, border_color=self.accent_color, text_color=self.accent_color,
                      hover_color=COLOR_HOVER, height=50, corner_radius=12,
                      command=lambda: self.switch_tab("special"))
        btn_comp.pack(fill="x", pady=(0, 15))

        card_pc = self.create_card(p, self.T("card_pc"))
        self.menu_PC = self.create_menu(card_pc, list(MODOS_PC.keys()), self.iniciar_pc)

        frame_opts = ctk.CTkFrame(card_pc, fg_color="transparent")
        frame_opts.pack(fill="x", pady=(10, 0))

        self.chk_video = ctk.CTkCheckBox(frame_opts, text=self.T("opt_video"), font=FONT_MAIN,
                                         text_color=COLOR_TEXT_DIM, fg_color=self.accent_color, hover_color=COLOR_HOVER, corner_radius=6)
        self.chk_video.pack(side="left", padx=(0, 10))

        self.chk_audio = ctk.CTkCheckBox(frame_opts, text=self.T("opt_audio"), font=FONT_MAIN,
                                         text_color=COLOR_TEXT_DIM, fg_color=self.accent_color, hover_color=COLOR_HOVER, corner_radius=6)
        self.chk_audio.pack(side="left", padx=10)
        self.chk_audio.select()

        self.chk_ghost = ctk.CTkCheckBox(frame_opts, text=self.T("opt_ghost"), font=FONT_MAIN,
                                         text_color=COLOR_TEXT_DIM, fg_color=self.accent_color, hover_color=COLOR_HOVER, corner_radius=6)
        self.chk_ghost.pack(side="left", padx=10)

        card_perf = self.create_card(p, self.T("card_perf"))
        self.menu_PERF = self.create_menu(card_perf, list(MODOS_PERF.keys()), self.aplicar_perf)

        card_bat = self.create_card(p, self.T("card_bat"))
        self.menu_BAT = self.create_menu(card_bat, list(MODOS_BAT.keys()), self.aplicar_bat)

        card_utils = self.create_card(p, self.T("card_utils"))
        grid = ctk.CTkFrame(card_utils, fg_color="transparent")
        grid.pack(fill="x")

        ctk.CTkButton(grid, text=self.T("btn_install"), fg_color=COLOR_SURFACE, border_width=1, border_color=COLOR_BORDER,
                      text_color=COLOR_TEXT_DIM, hover_color=COLOR_HOVER, width=120, height=40, corner_radius=10,
                      command=self.install_apk).pack(side="left", padx=(0, 5), expand=True, fill="x")

        ctk.CTkButton(grid, text=self.T("btn_send"), fg_color=COLOR_SURFACE, border_width=1, border_color=COLOR_BORDER,
                      text_color=COLOR_TEXT_DIM, hover_color=COLOR_HOVER, width=120, height=40, corner_radius=10,
                      command=self.send_file).pack(side="left", padx=5, expand=True, fill="x")

        ctk.CTkButton(grid, text=self.T("btn_reset"), fg_color=COLOR_SURFACE, border_width=1, border_color=COLOR_BORDER,
                      text_color=COLOR_TEXT_DIM, hover_color=COLOR_HOVER, width=120, height=40, corner_radius=10,
                      command=self.restaurar_padrao).pack(side="left", padx=(5, 0), expand=True, fill="x")
        
        self.txt_log = ctk.CTkTextbox(p, height=80, fg_color=COLOR_SURFACE, text_color=self.accent_color,
                                      font=FONT_MONO, corner_radius=12, border_width=1, border_color=COLOR_BORDER)
        self.txt_log.pack(fill="x", pady=20)

    def create_card(self, parent, title):
        frame = ctk.CTkFrame(parent, fg_color=COLOR_SURFACE, corner_radius=12, border_width=1, border_color=COLOR_BORDER)
        frame.pack(fill="x", pady=5, ipadx=15, ipady=15)
        ctk.CTkLabel(frame, text=title, font=("Roboto", 11, "bold"), text_color=COLOR_TEXT_DIM).pack(anchor="w", pady=(0, 10))
        return frame

    def create_menu(self, parent, values, cmd):
        f = ctk.CTkFrame(parent, fg_color="transparent")
        f.pack(fill="x")
        m = ctk.CTkOptionMenu(f, values=[self.T("select_default")] + values,
                              fg_color=COLOR_BG, button_color=COLOR_BORDER, button_hover_color=COLOR_HOVER,
                              text_color=COLOR_TEXT_MAIN, dropdown_fg_color=COLOR_SURFACE, dropdown_hover_color=COLOR_HOVER,
                              corner_radius=8, width=300,
                              command=lambda v: self.on_menu_select(v, cmd, m))
        m.pack(side="left", fill="x", expand=True, padx=(0, 5))
        ctk.CTkButton(f, text="?", width=30, fg_color="transparent", border_width=1, border_color=COLOR_BORDER,
                      text_color=COLOR_TEXT_DIM, hover_color=COLOR_HOVER, corner_radius=8,
                      command=lambda: self.show_info(m.get())).pack(side="right")
        return m

    def on_menu_select(self, value, command_func, menu_widget):
        if value != self.T("select_default"):
            menu_widget.set(value)
            command_func(value)

    def show_info(self, mode_name):
        if mode_name == self.T("select_default"): return
        help_key = None
        if mode_name in MODOS_PC: help_key = MODOS_PC[mode_name]["help_key"]
        elif mode_name in MODOS_PERF: help_key = MODOS_PERF[mode_name]["help_key"]
        elif mode_name in MODOS_BAT: help_key = MODOS_BAT[mode_name]["help_key"]
        info = self.T(help_key) if help_key else "No info."
        messagebox.showinfo(f"Info: {mode_name}", info)

    def build_special(self, p):
        c = self.create_card(p, self.T("sidebar_game"))
        btn_ff = ctk.CTkButton(c, text=self.T("hero_ff"), font=FONT_TITLE,
                               fg_color=self.accent_color, text_color=COLOR_BG, hover_color=COLOR_TEXT_MAIN,
                               height=80, corner_radius=12,
                               command=self.ativar_free_fire)
        btn_ff.pack(fill="x", pady=10)
        self.sw_keymap = ctk.CTkSwitch(c, text=self.T("sw_keymap"), command=self.toggle_keymapping,
                                       font=FONT_BOLD, text_color=COLOR_TEXT_MAIN,
                                       progress_color=self.accent_color, button_color=COLOR_TEXT_MAIN, button_hover_color=COLOR_TEXT_MAIN)
        self.sw_keymap.pack(pady=10)
        c2 = self.create_card(p, self.T("coming_soon"))
        future_games = ["COD Mobile", "PUBG New State", "Genshin Impact", "Wild Rift"]
        for game in future_games:
            ctk.CTkLabel(c2, text=f"• {game}", font=FONT_MAIN, text_color=COLOR_TEXT_DIM).pack(anchor="w", pady=2)

    def build_apps(self, p):
        head = ctk.CTkFrame(p, fg_color="transparent")
        head.pack(fill="x", pady=10)

        self.ent_search = ctk.CTkEntry(head, placeholder_text=self.T("apps_search"), width=300, fg_color=COLOR_SURFACE, border_color=COLOR_BORDER)
        self.ent_search.pack(side="left", padx=(20, 10))
        self.ent_search.bind("<KeyRelease>", self.filter_apps_ui)

        ctk.CTkButton(head, text=self.T("apps_refresh"), width=100, fg_color=COLOR_SURFACE, border_color=COLOR_BORDER, border_width=1,
                      command=self.refresh_apps_list).pack(side="left")

        self.lbl_loading = ctk.CTkLabel(p, text=self.T("apps_loading"), font=FONT_MAIN, text_color=COLOR_TEXT_DIM)

        self.scroll_apps = ctk.CTkScrollableFrame(p, fg_color=COLOR_SURFACE, corner_radius=12)
        self.scroll_apps.pack(fill="both", expand=True, padx=20, pady=10)
        self.refresh_apps_list()

    def refresh_apps_list(self):
        for w in self.scroll_apps.winfo_children(): w.destroy()
        if not self.target_device:
            ctk.CTkLabel(self.scroll_apps, text="No Device").pack(pady=20)
            return

        self.lbl_loading.place(relx=0.5, rely=0.5, anchor="center")

        def load():
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            exe = os.path.join(self.bin_dir, "adb.exe")
            res = subprocess.run([exe, "-s", self.target_device, "shell", "pm", "list", "packages", "-3"], capture_output=True, text=True, startupinfo=si)

            apps = []
            for line in res.stdout.splitlines():
                pkg = line.replace("package:", "").strip()
                if pkg: apps.append(pkg)

            self.all_apps_cache = apps
            self.after(0, lambda: self.populate_apps_ui(apps))

        threading.Thread(target=load, daemon=True).start()

    def filter_apps_ui(self, event=None):
        search = self.ent_search.get().lower()
        filtered = [pkg for pkg in self.all_apps_cache if search in pkg.lower()]
        self.populate_apps_ui(filtered)

    def populate_apps_ui(self, apps):
        self.lbl_loading.place_forget()
        for w in self.scroll_apps.winfo_children(): w.destroy()
        for pkg in apps:
            self.create_app_row(pkg)

    def create_app_row(self, pkg):
        f = ctk.CTkFrame(self.scroll_apps, fg_color="transparent", height=40)
        f.pack(fill="x", pady=2)

        ctk.CTkLabel(f, text=pkg, font=FONT_MONO, text_color=COLOR_TEXT_MAIN, width=350, anchor="w").pack(side="left", padx=10)

        ctk.CTkButton(f, text=self.T("action_open"), width=60, fg_color=COLOR_SUCCESS, text_color="white", height=25,
                      command=lambda: self.run_adb_generic(f"shell monkey -p {pkg} -c android.intent.category.LAUNCHER 1")).pack(side="right", padx=2)

        ctk.CTkButton(f, text=self.T("action_kill"), width=60, fg_color="orange", text_color="white", height=25,
                      command=lambda: self.run_adb_generic(f"shell am force-stop {pkg}")).pack(side="right", padx=2)

        ctk.CTkButton(f, text=self.T("action_del"), width=60, fg_color=COLOR_ERROR, text_color="white", height=25,
                      command=lambda: self.uninstall_app(pkg)).pack(side="right", padx=2)

    def uninstall_app(self, pkg):
        if messagebox.askyesno("Uninstall", f"Uninstall {pkg}?"):
            self.run_adb_cmd_string(f"pm uninstall {pkg}")
            self.after(1000, self.refresh_apps_list)

    def kill_all_processes(self):
        if not self.target_device: return
        self.run_adb_cmd_string("am kill-all")
        messagebox.showinfo(self.T("msg_success"), self.T("msg_kill"))

    def ativar_free_fire(self):
        if not self.target_device:
            messagebox.showerror(self.T("msg_conn_error"), self.T("msg_no_device"))
            return
        self.log(self.T("msg_ff_active"))
        cmds = MODOS_PERF["Gamer Ultimate (Mobile)"]["cmd"]
        self.run_adb_cmd_string(cmds)

    def run_adb_cmd_string(self, cmd_string):
        if not self.target_device:
            messagebox.showerror(self.T("msg_conn_error"), self.T("msg_no_device"))
            return

        self.debug_log(f"CMD: {cmd_string}")

        def t():
            exe = os.path.join(self.bin_dir, "adb.exe")
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            cmds = cmd_string.split(";")

            for attempt in range(2):
                success = True
                for c in cmds:
                    if not c.strip(): continue
                    if not self._ping_device():
                        self.log("Device offline. Healing...")
                        self.heal_adb_connection()
                        success = False
                        break

                    res = subprocess.run([exe, "-s", self.target_device, "shell", c.strip()], startupinfo=si, capture_output=True, text=True)
                    if res.returncode != 0:
                        success = False
                        self.log(f"CMD Fail: {c.strip()}")
                        break

                    if "wm size" in c: time.sleep(4.0)
                    elif "wm density" in c: time.sleep(1.5)
                    else: time.sleep(0.5)

                if success:
                    self.log("Commands applied.")
                    self.after(0, lambda: messagebox.showinfo(self.T("msg_success"), self.T("msg_cmd_success")))
                    return
                else:
                    if attempt == 0:
                        self.log("Retry attempt...")
                        self.heal_adb_connection()
                    else:
                        self.after(0, lambda: messagebox.showerror(self.T("msg_error"), "Failed after retry."))

        threading.Thread(target=t).start()

    def iniciar_pc(self, choice):
        if not self.target_device:
            messagebox.showerror(self.T("msg_conn_error"), self.T("msg_no_device"))
            self.menu_PC.set(self.T("select_default"))
            return

        cfg = MODOS_PC[choice]
        exe_adb = os.path.join(self.bin_dir, "adb.exe")
        exe_scrcpy = os.path.join(self.bin_dir, "scrcpy.exe")
        opt_video = self.chk_video.get()
        opt_audio = self.chk_audio.get()
        opt_ghost = self.chk_ghost.get()

        def thread_pc():
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            self.log(f"Starting {choice}...")

            try:
                if not self._ping_device(): self.heal_adb_connection()
                subprocess.run([exe_adb, "-s", self.target_device, "shell", "wm size reset"], startupinfo=si)
                subprocess.run([exe_adb, "-s", self.target_device, "shell", f"wm size {cfg['size']}"], startupinfo=si)
                time.sleep(4.0)

                if not self._ping_device(): self.heal_adb_connection()
                subprocess.run([exe_adb, "-s", self.target_device, "shell", "wm density reset"], startupinfo=si)
                subprocess.run([exe_adb, "-s", self.target_device, "shell", f"wm density {cfg['density']}"], startupinfo=si)
                time.sleep(1.0)

                subprocess.run([exe_adb, "-s", self.target_device, "shell", "settings put system user_rotation 1"], startupinfo=si)
                subprocess.run([exe_adb, "-s", self.target_device, "shell", "settings put system accelerometer_rotation 0"], startupinfo=si)
                time.sleep(1.0)

            except Exception as e:
                self.debug_log(f"Setup warning: {e}")

            scrcpy_args = list(cfg['scrcpy'])
            if opt_video:
                timestamp = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
                filename = f"REC_{timestamp}.mp4"
                filepath = os.path.join(self.caps_dir, filename)
                scrcpy_args += ["--record", filepath, "--record-format=mp4"]
                self.log(f"Recording: {filename}")
            if not opt_audio: scrcpy_args += ["--no-audio"]
            if opt_ghost: scrcpy_args += ["--turn-screen-off"]

            for attempt in range(2):
                if not self._ping_device(): self.heal_adb_connection()
                try:
                    proc = subprocess.run([exe_scrcpy, "-s", self.target_device] + scrcpy_args, cwd=self.bin_dir, startupinfo=si, capture_output=True, text=True)
                    if proc.returncode == 0: break

                    err = proc.stderr
                    if attempt == 0:
                        self.log("Scrcpy Error. Self-healing...")
                        self.heal_adb_connection()
                    else:
                        self.after(0, lambda: messagebox.showerror("SCRCPY ERROR", f"Scrcpy Failed:\n{err}"))
                        break
                except Exception as e:
                    if attempt == 0: self.heal_adb_connection()

            try:
                cmds_reset = ["wm size reset", "wm density reset", "settings put system user_rotation 0", "settings put system accelerometer_rotation 1"]
                for c in cmds_reset:
                    subprocess.run([exe_adb, "-s", self.target_device, "shell", c], startupinfo=si)
                    time.sleep(0.5)
            except: pass

        threading.Thread(target=thread_pc, daemon=True).start()

    def _ping_device(self):
        try:
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            exe = os.path.join(self.bin_dir, "adb.exe")
            res = subprocess.run([exe, "-s", self.target_device, "get-state"], capture_output=True, text=True, startupinfo=si)
            return "device" in res.stdout
        except: return False

    def heal_adb_connection(self):
        self.debug_log("HEALING ADB CONNECTION (SMART)...")
        si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
        exe = os.path.join(self.bin_dir, "adb.exe")

        subprocess.run([exe, "disconnect"], startupinfo=si)
        time.sleep(1)

        if self.last_ip:
            self.debug_log(f"Reconnecting to {self.last_ip}...")
            subprocess.run([exe, "connect", self.last_ip], startupinfo=si)
            time.sleep(3)
        else:
            self.debug_log("USB Mode: Waiting for auto-reconnect...")
            time.sleep(2)

        self.debug_log("Healing Complete.")

    def aplicar_perf(self, choice):
        if not self.target_device:
            self.menu_PERF.set(self.T("select_default"))
            return
        self.run_adb_cmd_string(MODOS_PERF[choice]["cmd"])

    def aplicar_bat(self, choice):
        if not self.target_device:
            self.menu_BAT.set(self.T("select_default"))
            return
        self.run_adb_cmd_string(MODOS_BAT[choice]["cmd"])

    def restaurar_padrao(self):
        if not self.target_device: return
        self.log(self.T("msg_restored"))
        cmds = "wm size reset; wm density reset; settings put system user_rotation 0; settings put system accelerometer_rotation 1; settings put global low_power 0; settings put system screen_brightness 100; settings put global window_animation_scale 1; settings put global transition_animation_scale 1; settings put global animator_duration_scale 1"
        self.run_adb_cmd_string(cmds)
        default_txt = self.T("select_default")
        self.menu_PC.set(default_txt)
        self.menu_PERF.set(default_txt)
        self.menu_BAT.set(default_txt)
        messagebox.showinfo(self.T("msg_success"), self.T("msg_restored"))

    def install_apk(self):
        if not self.target_device: return messagebox.showerror(self.T("msg_error"), "No Device")
        file_path = filedialog.askopenfilename(filetypes=[("Android Package", "*.apk")])
        if not file_path: return
        self.log(f"Installing...")
        def run():
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            exe = os.path.join(self.bin_dir, "adb.exe")
            res = subprocess.run([exe, "-s", self.target_device, "install", "-r", file_path], capture_output=True, text=True, startupinfo=si)
            if "Success" in res.stdout:
                self.log(self.T("msg_installed"))
                self.after(0, lambda: messagebox.showinfo(self.T("msg_success"), self.T("msg_installed")))
            else:
                self.after(0, lambda: messagebox.showerror(self.T("msg_error"), f"Failed:\n{res.stderr}"))
        threading.Thread(target=run).start()

    def send_file(self):
        if not self.target_device: return messagebox.showerror(self.T("msg_error"), "No Device")
        file_path = filedialog.askopenfilename()
        if not file_path: return
        self.log(f"Sending file...")
        def run():
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            exe = os.path.join(self.bin_dir, "adb.exe")
            res = subprocess.run([exe, "-s", self.target_device, "push", file_path, "/sdcard/Download/"], capture_output=True, text=True, startupinfo=si)
            if res.returncode == 0:
                self.log("File Sent!")
                self.after(0, lambda: messagebox.showinfo(self.T("msg_success"), self.T("msg_sent")))
            else:
                self.after(0, lambda: messagebox.showerror(self.T("msg_error"), f"Failed:\n{res.stderr}"))
        threading.Thread(target=run).start()

    def build_terminal(self, p):
        self.tab_term = ctk.CTkTabview(p, fg_color="transparent")
        self.tab_term.pack(fill="both", expand=True)
        self.tab_term.add(self.T("tab_manual"))
        self.tab_term.add(self.T("tab_logcat"))

        f_manual = self.tab_term.tab(self.T("tab_manual"))
        self.term_input = ctk.CTkTextbox(f_manual, height=200, fg_color=COLOR_SURFACE, text_color=COLOR_TEXT_MAIN, font=FONT_MONO)
        self.term_input.pack(fill="x", padx=20, pady=20)
        ctk.CTkButton(f_manual, text=self.T("btn_exec"), fg_color=self.accent_color, text_color=COLOR_BG, hover_color=COLOR_TEXT_MAIN,
                      command=self.run_manual).pack(padx=20)

        f_logcat = self.tab_term.tab(self.T("tab_logcat"))
        f_btns = ctk.CTkFrame(f_logcat, fg_color="transparent")
        f_btns.pack(fill="x", pady=5)
        ctk.CTkButton(f_btns, text=self.T("btn_start_log"), fg_color=COLOR_SUCCESS, width=120, command=self.start_logcat).pack(side="left", padx=10)
        ctk.CTkButton(f_btns, text=self.T("btn_stop_log"), fg_color=COLOR_ERROR, width=80, command=self.stop_logcat).pack(side="left", padx=10)

        self.txt_logcat = ctk.CTkTextbox(f_logcat, fg_color="black", text_color=self.accent_color, font=FONT_MONO)
        self.txt_logcat.pack(fill="both", expand=True, padx=10, pady=5)

    def start_logcat(self):
        if self.logcat_process: return
        self.stop_logcat_flag = False
        self.txt_logcat.delete("1.0", "end")

        def run():
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            exe = os.path.join(self.bin_dir, "adb.exe")
            try:
                self.logcat_process = subprocess.Popen([exe, "-s", self.target_device, "logcat", "-v", "time"], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, startupinfo=si)
                while not self.stop_logcat_flag and self.logcat_process.poll() is None:
                    line = self.logcat_process.stdout.readline()
                    if line: self.after(0, lambda l=line: self._safe_logcat_insert(l))
            except: pass
            self.logcat_process = None

        threading.Thread(target=run, daemon=True).start()

    def stop_logcat(self):
        self.stop_logcat_flag = True
        if self.logcat_process:
            self.logcat_process.terminate()
            self.logcat_process = None

    def _safe_logcat_insert(self, line):
        self.txt_logcat.insert("end", line)
        self.txt_logcat.see("end")
        # V106 Optimized Buffer
        lines = int(self.txt_logcat.index('end-1c').split('.')[0])
        if lines > 600:
            self.txt_logcat.delete("1.0", "100.0")

    # --- HELPERS ---
    def get_device_name(self):
        try:
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            exe = os.path.join(self.bin_dir, "adb.exe")
            res = subprocess.run([exe, "-s", self.target_device, "shell", "getprop ro.product.model"], capture_output=True, text=True, startupinfo=si, timeout=2)
            return res.stdout.strip() if res.stdout.strip() else self.target_device
        except: return self.target_device

    def update_status_ui(self, connected):
        if connected:
            self.btn_device_status.configure(text=f"📱 {self.device_model}", fg_color=COLOR_SURFACE, border_color=COLOR_SUCCESS)
            self.btn_refresh.configure(text_color=COLOR_SUCCESS)
        else:
            self.btn_device_status.configure(text=self.T("status_disconnected"), fg_color=COLOR_SURFACE, border_color=COLOR_ERROR)
            self.btn_refresh.configure(text_color=COLOR_ERROR)
            self.lbl_stats.configure(text="")

    def update_stats_ui(self, level, temp, free_space, ram_info=""):
        if not self.target_device: return
        color = COLOR_ERROR if temp > 40.0 else COLOR_TEXT_DIM
        self.lbl_stats.configure(text=f"BAT: {level}% | TEMP: {temp}°C | {ram_info} | FREE: {free_space}", text_color=color)

    def force_refresh(self):
        self.btn_device_status.configure(text=self.T("status_searching"), border_color=self.accent_color)
        threading.Thread(target=self.heal_adb_connection).start()

    def start_monitor(self):
        def loop():
            adb = os.path.join(self.bin_dir, "adb.exe")
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            while True:
                try:
                    if os.path.exists(adb):
                        res = subprocess.run([adb, "devices"], capture_output=True, text=True, startupinfo=si)
                        lines = [l for l in res.stdout.split('\n') if 'device' in l and 'List' not in l]
                        if lines:
                            new_id = lines[0].split()[0]
                            # V104 Smart Reconnect Memory
                            if ":" in new_id and "." in new_id:
                                self.last_ip = new_id

                            if self.target_device != new_id:
                                self.target_device = new_id
                                self.device_model = self.get_device_name()
                                self.after(0, lambda: self.update_status_ui(True))
                        else:
                            if self.target_device:
                                self.target_device = ""
                                self.after(0, lambda: self.update_status_ui(False))
                except: pass
                time.sleep(3)
        threading.Thread(target=loop, daemon=True).start()

    def start_battery_monitor(self):
        def loop():
            adb = os.path.join(self.bin_dir, "adb.exe")
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            while True:
                if self.target_device:
                    try:
                        # Battery
                        res = subprocess.run([adb, "-s", self.target_device, "shell", "dumpsys", "battery"], capture_output=True, text=True, startupinfo=si)
                        level = re.search(r'level: (\d+)', res.stdout)
                        temp = re.search(r'temperature: (\d+)', res.stdout)

                        # Storage (df -h /data)
                        res_st = subprocess.run([adb, "-s", self.target_device, "shell", "df", "-h", "/data"], capture_output=True, text=True, startupinfo=si)
                        avail = "N/A"
                        if res_st.stdout:
                            lines = res_st.stdout.split('\n')
                            if len(lines) > 1:
                                parts = lines[1].split()
                                if len(parts) >= 4: avail = parts[3]

                        # RAM (/proc/meminfo)
                        res_mem = subprocess.run([adb, "-s", self.target_device, "shell", "cat", "/proc/meminfo"], capture_output=True, text=True, startupinfo=si)
                        ram_str = "RAM: N/A"
                        if res_mem.stdout:
                            mt = re.search(r'MemTotal:\s+(\d+)', res_mem.stdout)
                            ma = re.search(r'MemAvailable:\s+(\d+)', res_mem.stdout)
                            if mt and ma:
                                t_kb = int(mt.group(1))
                                a_kb = int(ma.group(1))
                                u_gb = (t_kb - a_kb) / 1024 / 1024
                                t_gb = t_kb / 1024 / 1024
                                ram_str = f"RAM: {u_gb:.1f}/{t_gb:.1f}GB"

                        if level and temp:
                            self.after(0, lambda: self.update_stats_ui(int(level.group(1)), int(temp.group(1))/10.0, avail, ram_str))
                    except: pass
                time.sleep(5)
        threading.Thread(target=loop, daemon=True).start()

    def run_manual(self):
        cmd = self.term_input.get("0.0", "end").strip()
        if cmd: threading.Thread(target=lambda: subprocess.Popen(cmd, cwd=self.bin_dir, shell=True, startupinfo=subprocess.STARTUPINFO())).start()

    def run_adb_generic(self, cmd):
        si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
        exe = os.path.join(self.bin_dir, "adb.exe")
        subprocess.run([exe] + cmd.split(), startupinfo=si)

    def debug_log(self, msg):
        try:
            sys.__stdout__.write(f"[DEBUG] {msg}\n")
            sys.__stdout__.flush()
        except: pass

    def log(self, msg):
        ts = datetime.datetime.now().strftime("%H:%M")
        try:
            self.after(0, lambda: self._safe_log_insert(ts, msg))
        except: pass

    def _safe_log_insert(self, ts, msg):
        self.txt_log.configure(state="normal")
        self.txt_log.insert("end", f"[{ts}] {msg}\n")
        self.txt_log.see("end")
        self.txt_log.configure(state="disabled")

    def validate_installation(self):
        if not os.path.exists(self.bin_dir): messagebox.showwarning("ATENÇÃO", "Pasta 'bin' não encontrada!")

if __name__ == "__main__":
    app = TurboCoreApp()
    app.mainloop()
