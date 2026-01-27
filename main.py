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

# --- CONFIGURAÇÃO GERAL (PREMIUM THEME) ---
ctk.set_appearance_mode("Dark")

# Paleta Minimalista & Neon
COLOR_ACCENT = "#00E5FF"      # Ciano Neon (Destaque Principal)
COLOR_BG = "#0A0A0A"          # Fundo Quase Preto
COLOR_SURFACE = "#171717"     # Superfície de Painéis
COLOR_BORDER = "#333333"      # Bordas Sutis
COLOR_TEXT_MAIN = "#FAFAFA"   # Texto Principal
COLOR_TEXT_DIM = "#A1A1AA"    # Texto Secundário
COLOR_SUCCESS = "#10B981"     # Verde Suave (Status)
COLOR_ERROR = "#EF4444"       # Vermelho Suave (Status/Erro)
COLOR_HOVER = "#262626"       # Hover em botões escuros

# Configuração Padrão de Fonte
FONT_MAIN = ("Roboto Medium", 13)
FONT_BOLD = ("Roboto", 13, "bold")
FONT_TITLE = ("Roboto", 20, "bold")
FONT_MONO = ("Consolas", 11)

# --- HELP TEXTS (SEGURANÇA E RESPONSABILIDADE) ---
HELP_TEXTS = {
    # MODO PC
    "PC Lite (Econômico)": "[RISCO: BAIXO]\nFERRAMENTA: Scrcpy + ADB\n\nO QUE FAZ:\n1. Força resolução física para 960x540 (via ADB).\n2. Trava FPS em 30.\n3. Abre espelhamento sem bordas.\n\nEFEITO:\nReduz drasticamente o uso de CPU/GPU. Seguro para uso prolongado.",
    "PC Soberano (Padrão)": "[RISCO: BAIXO]\nFERRAMENTA: Scrcpy + ADB + Rotation\n\nO QUE FAZ:\n1. Altera resolução para 1280x720 (HD).\n2. Força densidade 160 DPI.\n3. Força rotação Paisagem.\n\nEFEITO:\nTransforma o celular num monitor secundário. Uso padrão recomendado.",
    "PC Gamer (Ultra)": "[RISCO: MÉDIO - AQUECIMENTO]\nFERRAMENTA: Scrcpy (Low Latency) + ADB\n\nO QUE FAZ:\n1. Resolução HD (720p).\n2. Remove limite de FPS.\n3. Reduz buffer de áudio para 20ms.\n\nALERTA:\nO uso contínuo pode causar aquecimento. Use por sua conta e risco.",
    
    # PERFORMANCE
    "Gamer Ultimate (Mobile)": "[RISCO: ALTO - AQUECIMENTO]\nFERRAMENTA: ADB Shell (Package Manager)\n\nO QUE FAZ:\n1. Desabilita o GOS da Samsung.\n2. Altera resolução para 432x960.\n\nPERIGO CRÍTICO:\nRemove proteções térmicas de software. O dispositivo pode superaquecer. Use cooler externo.",
    "Ultimate Desempenho (Bruto)": "[PERIGO CRÍTICO - HARDWARE]\nFERRAMENTA: ADB Shell (Settings Global)\n\nO QUE FAZ:\n1. Desliga proteção térmica (disable_thermal_control).\n2. Mata apps de fundo.\n\nALERTA MÁXIMO:\nO processador rodará no limite térmico. Risco real de danos ao hardware. Use por sua conta e risco.",
    "Usual Turbo (Dia a Dia)": "[RISCO: BAIXO]\nFERRAMENTA: ADB Shell (Window Manager)\n\nO QUE FAZ:\n1. Define animações para 0.5x.\n2. Reseta resolução.\n\nEFEITO:\nMelhora a fluidez visual sem riscos ao hardware.",
    
    # BATERIA
    "Economia Normal": "[RISCO: BAIXO]\nAtiva o 'Low Power Mode' nativo.",
    "Super Economia": "[RISCO: BAIXO]\nDesativa Bluetooth e Sincronização. Reduz resolução.",
    "Ultimate Economia (Deep)": "[RISCO: BAIXO - USABILIDADE]\nReduz brilho a zero e mata processos. O celular ficará difícil de usar."
}

# --- CONFIGS ---
MODOS_PC = {
    "PC Lite (Econômico)": {"size": "540x960", "density": "120", "scrcpy": ["--video-bit-rate=4M", "--max-fps=30", "--fullscreen", "--window-borderless", "--always-on-top", "--window-title=TURBO_LITE"]},
    "PC Soberano (Padrão)": {"size": "720x1280", "density": "160", "scrcpy": ["--video-bit-rate=8M", "--max-fps=60", "--fullscreen", "--window-borderless", "--always-on-top", "--window-title=TURBO_SOBERANO"]},
    "PC Gamer (Ultra)": {"size": "720x1280", "density": "160", "scrcpy": ["--video-bit-rate=16M", "--max-fps=0", "--fullscreen", "--window-borderless", "--audio-buffer=20", "--window-title=TURBO_GAMER"]}
}

MODOS_PERF = {
    "Gamer Ultimate (Mobile)": "wm size 432x960; wm density 160; pm disable-user --user 0 com.samsung.android.game.gos",
    "Ultimate Desempenho (Bruto)": "settings put global power_manager_constants disable_thermal_control=true; wm size 360x800; wm density 120; am kill-all",
    "Usual Turbo (Dia a Dia)": "wm size reset; wm density reset; settings put global window_animation_scale 0.5; settings put global transition_animation_scale 0.5"
}

MODOS_BAT = {
    "Economia Normal": "settings put global low_power 1; settings put global adaptive_battery_management_enabled 1",
    "Super Economia": "wm size 576x1280; wm density 240; svc bluetooth disable; settings put global master_sync_enabled 0",
    "Ultimate Economia (Deep)": "wm size 360x800; wm density 120; settings put system screen_brightness 0; am kill-all; settings put global low_power 1"
}

class TurboCoreApp(ctk.CTk):
    def __init__(self):
        super().__init__()
        self.debug_log("--- INICIANDO TURBO CORE V94.1 - PREMIUM FIX ---")
        self.title("TURBO CORE V94.1 - PREMIUM EDITION")
        self.geometry("850x700") # Aumentado altura para acomodar cards extras
        self.resizable(False, True) # Permitir scroll vertical se necessário futuramente
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

        self.img_bg = None

        self.setup_ui()
        self.start_monitor()
        self.start_battery_monitor()
        self.setup_hotkeys()
        self.keymapping_active = False

    def check_binaries(self):
        self.debug_log("Verificando binários...")
        if not os.path.exists(self.bin_dir):
            self.debug_log("CRITICO: Pasta bin não encontrada!")
            messagebox.showwarning("ERRO CRITICO", "Pasta 'bin' não encontrada!")
            return

        adb_path = os.path.join(self.bin_dir, "adb.exe")
        scrcpy_path = os.path.join(self.bin_dir, "scrcpy.exe")

        missing = []
        if not os.path.exists(adb_path): missing.append("adb.exe")
        if not os.path.exists(scrcpy_path): missing.append("scrcpy.exe")

        if missing:
            msg = f"Arquivos faltando na pasta bin:\n{', '.join(missing)}"
            self.debug_log(f"CRITICO: {msg}")
            messagebox.showwarning("ARQUIVOS FALTANDO", msg)
        else:
            self.debug_log("Binários verificados com sucesso.")

    def setup_hotkeys(self):
        self.debug_log("Configurando Hotkeys...")
        if keyboard:
            try:
                # Hotkeys Globais
                keyboard.add_hotkey('f1', lambda: self.iniciar_pc("PC Soberano (Padrão)"))
                keyboard.add_hotkey('f2', lambda: self.ativar_free_fire())

                # Keymapping Básico
                self.hook_space = keyboard.on_press_key("space", self.key_tap_handler, suppress=False)

                # Keymapping Avançado (WASD - Swipe)
                keyboard.on_press_key("w", lambda e: self.key_swipe_handler("w"), suppress=False)
                keyboard.on_press_key("a", lambda e: self.key_swipe_handler("a"), suppress=False)
                keyboard.on_press_key("s", lambda e: self.key_swipe_handler("s"), suppress=False)
                keyboard.on_press_key("d", lambda e: self.key_swipe_handler("d"), suppress=False)

                self.debug_log("Hotkeys configuradas.")
            except Exception as e:
                self.debug_log(f"ERRO ao configurar hotkeys: {e}")
        else:
            self.debug_log("Biblioteca 'keyboard' não encontrada. Hotkeys desativadas.")

    def key_tap_handler(self, event):
        if self.keymapping_active and self.target_device:
            threading.Thread(target=lambda: self.run_adb_generic("shell input tap 640 360")).start()

    def key_swipe_handler(self, key):
        if self.keymapping_active and self.target_device:
            # Coordenadas do "Joystick Virtual" (Ex: Centro em 200, 500)
            cx, cy = 200, 500
            dist = 100
            duration = 200 # ms

            x1, y1 = cx, cy
            x2, y2 = cx, cy

            if key == "w": y2 -= dist
            elif key == "s": y2 += dist
            elif key == "a": x2 -= dist
            elif key == "d": x2 += dist

            cmd = f"shell input swipe {x1} {y1} {x2} {y2} {duration}"
            threading.Thread(target=lambda: self.run_adb_generic(cmd)).start()

    def toggle_keymapping(self):
        self.keymapping_active = not self.keymapping_active
        state = "ATIVADO" if self.keymapping_active else "DESATIVADO"
        self.log(f"Keymapping (Espaço/WASD): {state}")

    def setup_ui(self):
        self.debug_log("Iniciando construção da UI V94 Premium...")

        # 1. SIDEBAR (Esquerda) - Estilo Dark Matte
        self.sidebar = ctk.CTkFrame(self, width=200, corner_radius=0, fg_color=COLOR_SURFACE, border_width=0)
        self.sidebar.pack(side="left", fill="y", expand=False)
        self.sidebar.pack_propagate(False)

        # Logo / Título
        ctk.CTkLabel(self.sidebar, text="TURBO\nCORE", font=("Montserrat", 24, "bold"), text_color=COLOR_ACCENT).pack(pady=(40, 5))
        ctk.CTkLabel(self.sidebar, text="V94 PREMIUM", font=("Roboto", 10), text_color=COLOR_TEXT_DIM).pack(pady=(0, 40))

        # Botões Sidebar (Flat Style)
        self.btn_dash = self.create_sidebar_btn("DASHBOARD", "dash")
        self.btn_special = self.create_sidebar_btn("GAMING MODE", "special")
        self.btn_term = self.create_sidebar_btn("TERMINAL", "term")

        # 2. PAINEL DIREITO (Header + Conteúdo)
        self.right_panel = ctk.CTkFrame(self, fg_color="transparent")
        self.right_panel.pack(side="right", fill="both", expand=True)

        # 2.1 HEADER (Transparente, integrado)
        self.header = ctk.CTkFrame(self.right_panel, height=70, corner_radius=0, fg_color="transparent")
        self.header.pack(fill="x", side="top", padx=30, pady=(20, 0))

        self.frame_dev_info = ctk.CTkFrame(self.header, fg_color="transparent")
        self.frame_dev_info.pack(side="left")

        # Botão de Status (Estilo Pill)
        self.btn_device_status = ctk.CTkButton(self.frame_dev_info, text="Buscando...", font=FONT_BOLD,
                                               fg_color=COLOR_SURFACE, text_color=COLOR_TEXT_DIM,
                                               width=220, height=36, corner_radius=18,
                                               border_width=1, border_color=COLOR_BORDER,
                                               hover_color=COLOR_HOVER,
                                               command=self.abrir_gerenciador_conexao)
        self.btn_device_status.pack(side="left")

        # Monitoramento (BAT / TEMP)
        self.lbl_stats = ctk.CTkLabel(self.frame_dev_info, text="", font=FONT_MONO, text_color=COLOR_TEXT_DIM)
        self.lbl_stats.pack(side="left", padx=15)

        self.btn_refresh = ctk.CTkButton(self.frame_dev_info, text="↺", width=36, height=36,
                                         fg_color=COLOR_SURFACE, text_color=COLOR_TEXT_MAIN,
                                         hover_color=COLOR_HOVER, corner_radius=18,
                                         border_width=1, border_color=COLOR_BORDER,
                                         command=self.force_refresh)
        self.btn_refresh.pack(side="left", padx=5)

        # 2.2 MAIN AREA
        self.main_area = ctk.CTkFrame(self.right_panel, fg_color="transparent")
        self.main_area.pack(fill="both", expand=True, padx=30, pady=20)
        
        self.frames = {
            "dash": ctk.CTkFrame(self.main_area, fg_color="transparent"),
            "special": ctk.CTkFrame(self.main_area, fg_color="transparent"),
            "term": ctk.CTkFrame(self.main_area, fg_color="transparent")
        }
        
        self.build_dashboard(self.frames["dash"])
        self.build_special(self.frames["special"])
        self.build_terminal(self.frames["term"])
        
        self.current_frame = None
        self.switch_tab("dash")
        
        # Footer Minimalista
        self.lbl_system_status = ctk.CTkLabel(self.right_panel, text="SYSTEM READY", font=("Roboto", 9), text_color="#333")
        self.lbl_system_status.pack(fill="x", side="bottom", pady=5)
        self.debug_log("UI V94 construída.")

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

        # Highlight Sidebar (Indicador de cor no texto ou fundo)
        btns = {"dash": self.btn_dash, "special": self.btn_special, "term": self.btn_term}
        for k, b in btns.items():
            if k == mode:
                b.configure(fg_color=COLOR_HOVER, text_color=COLOR_ACCENT, border_width=1, border_color=COLOR_BORDER)
            else:
                b.configure(fg_color="transparent", text_color=COLOR_TEXT_DIM, border_width=0)

    def abrir_gerenciador_conexao(self):
        toplevel = ctk.CTkToplevel(self)
        toplevel.title("Connection Wizard")
        toplevel.geometry("650x450")
        toplevel.configure(fg_color=COLOR_BG)
        toplevel.attributes("-topmost", True)

        # Estilo interno
        frame_tools = ctk.CTkFrame(toplevel, fg_color=COLOR_BG)
        frame_tools.pack(side="left", fill="both", expand=True, padx=20, pady=20)

        frame_tutorial = ctk.CTkFrame(toplevel, fg_color=COLOR_SURFACE, corner_radius=12)
        frame_tutorial.pack(side="right", fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame_tools, text="Connection Tools", font=FONT_BOLD, text_color=COLOR_TEXT_MAIN).pack(pady=10, anchor="w")

        # Wireless Pairing
        ctk.CTkLabel(frame_tools, text="1. Wireless Pairing", font=FONT_MAIN, text_color=COLOR_ACCENT).pack(pady=(10,5), anchor="w")
        ip_pair_entry = ctk.CTkEntry(frame_tools, placeholder_text="IP:PORT (Ex: 192.168.0.5:40000)",
                                     fg_color=COLOR_SURFACE, border_color=COLOR_BORDER)
        ip_pair_entry.pack(fill="x", pady=5)
        code_entry = ctk.CTkEntry(frame_tools, placeholder_text="Pairing Code (6 Digits)",
                                  fg_color=COLOR_SURFACE, border_color=COLOR_BORDER)
        code_entry.pack(fill="x", pady=5)

        def do_pair():
            addr = ip_pair_entry.get(); code = code_entry.get()
            if addr and code: threading.Thread(target=lambda: self.run_adb_generic(f"pair {addr} {code}")).start()

        ctk.CTkButton(frame_tools, text="Pair Device", fg_color="transparent", border_width=1, border_color=COLOR_ACCENT,
                      text_color=COLOR_ACCENT, hover_color=COLOR_HOVER, command=do_pair).pack(fill="x", pady=5)

        # Connect
        ctk.CTkLabel(frame_tools, text="2. ADB Connect", font=FONT_MAIN, text_color=COLOR_ACCENT).pack(pady=(20,5), anchor="w")
        ip_conn_entry = ctk.CTkEntry(frame_tools, placeholder_text="IP:PORT (Ex: 192.168.0.5:5555)",
                                     fg_color=COLOR_SURFACE, border_color=COLOR_BORDER)
        ip_conn_entry.pack(fill="x", pady=5)

        def do_connect():
            addr = ip_conn_entry.get()
            if addr: threading.Thread(target=lambda: self.run_adb_generic(f"connect {addr}")).start()

        ctk.CTkButton(frame_tools, text="Connect", fg_color=COLOR_ACCENT, text_color=COLOR_BG, hover_color="#00C4D9",
                      command=do_connect).pack(fill="x", pady=5)

        # Tutorial
        ctk.CTkLabel(frame_tutorial, text="Quick Guide", font=FONT_BOLD, text_color=COLOR_TEXT_MAIN).pack(pady=10)
        tut_text = "1. Enable Developer Options\n2. Enable USB Debugging\n3. Enable Wireless Debugging\n\nFor Wi-Fi:\nGo to Wireless Debugging >\nPair with pairing code.\n\nEnter IP, Port & Code."
        ctk.CTkLabel(frame_tutorial, text=tut_text, justify="left", font=FONT_MAIN, text_color=COLOR_TEXT_DIM).pack(padx=15, pady=10)

    # --- DASHBOARD ---
    def build_dashboard(self, p):
        # Atalho Competitivo - Card Style
        btn_comp = ctk.CTkButton(p, text="LAUNCH COMPETITIVE MODE", font=FONT_BOLD,
                      fg_color=COLOR_SURFACE, border_width=1, border_color=COLOR_ACCENT, text_color=COLOR_ACCENT,
                      hover_color=COLOR_HOVER, height=50, corner_radius=12,
                      command=lambda: self.switch_tab("special"))
        btn_comp.pack(fill="x", pady=(0, 15))

        # 1. PC MODES - Card Frame
        card_pc = self.create_card(p, "PC EXPERIENCE")
        self.create_menu(card_pc, list(MODOS_PC.keys()), self.iniciar_pc)

        # Launch Options
        frame_opts = ctk.CTkFrame(card_pc, fg_color="transparent")
        frame_opts.pack(fill="x", pady=(10, 0))

        self.chk_video = ctk.CTkCheckBox(frame_opts, text="Record Video", font=FONT_MAIN,
                                         text_color=COLOR_TEXT_DIM, fg_color=COLOR_ACCENT, hover_color=COLOR_HOVER, corner_radius=6)
        self.chk_video.pack(side="left", padx=(0, 10))

        self.chk_audio = ctk.CTkCheckBox(frame_opts, text="Record Audio", font=FONT_MAIN,
                                         text_color=COLOR_TEXT_DIM, fg_color=COLOR_ACCENT, hover_color=COLOR_HOVER, corner_radius=6)
        self.chk_audio.pack(side="left", padx=10)
        self.chk_audio.select()

        self.chk_ghost = ctk.CTkCheckBox(frame_opts, text="Ghost Mode", font=FONT_MAIN,
                                         text_color=COLOR_TEXT_DIM, fg_color=COLOR_ACCENT, hover_color=COLOR_HOVER, corner_radius=6)
        self.chk_ghost.pack(side="left", padx=10)

        # 2. PERFORMANCE CONTROL (RESTAURADO)
        card_perf = self.create_card(p, "PERFORMANCE CONTROL")
        self.create_menu(card_perf, list(MODOS_PERF.keys()), self.aplicar_perf)

        # 3. BATTERY MANAGER (RESTAURADO)
        card_bat = self.create_card(p, "BATTERY MANAGER")
        self.create_menu(card_bat, list(MODOS_BAT.keys()), self.aplicar_bat)

        # 4. UTILS - Card Frame
        card_utils = self.create_card(p, "SYSTEM TOOLS")

        grid = ctk.CTkFrame(card_utils, fg_color="transparent")
        grid.pack(fill="x")

        ctk.CTkButton(grid, text="Install APK", fg_color=COLOR_SURFACE, border_width=1, border_color=COLOR_BORDER,
                      text_color=COLOR_TEXT_DIM, hover_color=COLOR_HOVER, width=150, height=40, corner_radius=10,
                      command=self.install_apk).pack(side="left", padx=(0, 5), expand=True, fill="x")

        ctk.CTkButton(grid, text="Factory Reset", fg_color=COLOR_SURFACE, border_width=1, border_color=COLOR_BORDER,
                      text_color=COLOR_TEXT_DIM, hover_color=COLOR_HOVER, width=150, height=40, corner_radius=10,
                      command=self.restaurar_padrao).pack(side="left", padx=(5, 0), expand=True, fill="x")
        
        # Log
        self.txt_log = ctk.CTkTextbox(p, height=80, fg_color=COLOR_SURFACE, text_color=COLOR_ACCENT,
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

        m = ctk.CTkOptionMenu(f, values=["Select Mode..."] + values,
                              fg_color=COLOR_BG, button_color=COLOR_BORDER, button_hover_color=COLOR_HOVER,
                              text_color=COLOR_TEXT_MAIN, dropdown_fg_color=COLOR_SURFACE, dropdown_hover_color=COLOR_HOVER,
                              corner_radius=8, width=300,
                              command=lambda v: self.on_menu_select(v, cmd, m))
        m.pack(side="left", fill="x", expand=True, padx=(0, 5))

        ctk.CTkButton(f, text="?", width=30, fg_color="transparent", border_width=1, border_color=COLOR_BORDER,
                      text_color=COLOR_TEXT_DIM, hover_color=COLOR_HOVER, corner_radius=8,
                      command=lambda: self.show_info(m.get())).pack(side="right")
        # Save ref logic simplified for this cleaner version

    def on_menu_select(self, value, command_func, menu_widget):
        if value != "Select Mode...":
            menu_widget.set(value)
            command_func(value)

    def show_info(self, mode_name):
        if mode_name == "Select Mode...": return
        info = HELP_TEXTS.get(mode_name, "No description.")
        messagebox.showinfo(f"Info: {mode_name}", info)

    # --- ABA ESPECIAL ---
    def build_special(self, p):
        # Hero Section
        c = self.create_card(p, "COMPETITIVE OPTIMIZATION")
        
        btn_ff = ctk.CTkButton(c, text="ACTIVATE FREE FIRE MODE", font=FONT_TITLE,
                               fg_color=COLOR_ACCENT, text_color=COLOR_BG, hover_color="#00C4D9",
                               height=80, corner_radius=12,
                               command=self.ativar_free_fire)
        btn_ff.pack(fill="x", pady=10)

        self.sw_keymap = ctk.CTkSwitch(c, text="Enable WASD Keymapping", command=self.toggle_keymapping,
                                       font=FONT_BOLD, text_color=COLOR_TEXT_MAIN,
                                       progress_color=COLOR_ACCENT, button_color=COLOR_TEXT_MAIN, button_hover_color=COLOR_TEXT_MAIN)
        self.sw_keymap.pack(pady=10)

        # Roadmap
        c2 = self.create_card(p, "ROADMAP")
        future_games = ["COD Mobile", "PUBG New State", "Genshin Impact", "Wild Rift"]
        for game in future_games:
            ctk.CTkLabel(c2, text=f"• {game} (Coming Soon)", font=FONT_MAIN, text_color=COLOR_TEXT_DIM).pack(anchor="w", pady=2)

    def ativar_free_fire(self):
        if not self.target_device: return messagebox.showerror("Error", "Connect device first!")
        self.log("ACTIVATING FREE FIRE MODE...")
        cmds = "wm size 540x1170; wm density 140; settings put global window_animation_scale 0; settings put global transition_animation_scale 0; settings put global animator_duration_scale 0; cmd power set-mode 1; am kill-all"
        self.run_adb_cmd_string(cmds)
        messagebox.showinfo("Success", "OPTIMIZATION APPLIED!")

    # --- EXECUÇÃO (Mantida igual, apenas logs limpos) ---
    def run_adb_cmd_string(self, cmd_string):
        self.debug_log(f"CMD: {cmd_string}")
        def t():
            exe = os.path.join(self.bin_dir, "adb.exe")
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            for c in cmd_string.split(";"):
                if c.strip(): subprocess.run([exe, "-s", self.target_device, "shell", c.strip()], startupinfo=si)
            self.log("Commands applied.")
        threading.Thread(target=t).start()

    def iniciar_pc(self, choice):
        if not self.target_device: return
        cfg = MODOS_PC[choice]
        exe_adb = os.path.join(self.bin_dir, "adb.exe")
        exe_scrcpy = os.path.join(self.bin_dir, "scrcpy.exe")
        
        opt_video = self.chk_video.get()
        opt_audio = self.chk_audio.get()
        opt_ghost = self.chk_ghost.get()

        def thread_pc():
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            self.log(f"Starting {choice}...")
            
            cmds = [
                "wm size reset", "wm density reset",
                f"wm size {cfg['size']}", f"wm density {cfg['density']}",
                "settings put system user_rotation 1", "settings put system accelerometer_rotation 0"
            ]
            for c in cmds: subprocess.run([exe_adb, "-s", self.target_device, "shell", c], startupinfo=si)
            
            time.sleep(2.5)

            scrcpy_args = list(cfg['scrcpy'])

            if opt_video:
                timestamp = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
                filename = f"REC_{timestamp}.mp4"
                filepath = os.path.join(self.caps_dir, filename)
                scrcpy_args += ["--record", filepath, "--record-format=mp4"]
                self.log(f"Recording: {filename}")

            if not opt_audio: scrcpy_args += ["--no-audio"]
            if opt_ghost: scrcpy_args += ["--turn-screen-off"]

            try:
                subprocess.run([exe_scrcpy, "-s", self.target_device] + scrcpy_args, cwd=self.bin_dir, startupinfo=si)
            except Exception as e: self.debug_log(f"SCRCPY ERROR: {e}")
            
            cmds_reset = ["wm size reset", "wm density reset", "settings put system user_rotation 0", "settings put system accelerometer_rotation 1"]
            for c in cmds_reset: subprocess.run([exe_adb, "-s", self.target_device, "shell", c], startupinfo=si)

        threading.Thread(target=thread_pc, daemon=True).start()

    def aplicar_perf(self, choice): self.run_adb_cmd_string(MODOS_PERF[choice])
    def aplicar_bat(self, choice): self.run_adb_cmd_string(MODOS_BAT[choice])

    def restaurar_padrao(self):
        if not self.target_device: return
        self.log("Restoring...")
        cmds = "wm size reset; wm density reset; settings put system user_rotation 0; settings put system accelerometer_rotation 1; settings put global low_power 0; settings put system screen_brightness 100; settings put global window_animation_scale 1; settings put global transition_animation_scale 1; settings put global animator_duration_scale 1"
        self.run_adb_cmd_string(cmds)
        messagebox.showinfo("Success", "Restored!")

    def install_apk(self):
        if not self.target_device: return messagebox.showerror("Error", "No Device")
        file_path = filedialog.askopenfilename(filetypes=[("Android Package", "*.apk")])
        if not file_path: return
        self.log(f"Installing: {os.path.basename(file_path)}...")
        def run():
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            exe = os.path.join(self.bin_dir, "adb.exe")
            res = subprocess.run([exe, "-s", self.target_device, "install", "-r", file_path], capture_output=True, text=True, startupinfo=si)
            if "Success" in res.stdout:
                self.log("Install Complete!")
                self.after(0, lambda: messagebox.showinfo("Success", "Installed!"))
            else:
                self.after(0, lambda: messagebox.showerror("Error", f"Failed:\n{res.stderr}"))
        threading.Thread(target=run).start()

    def build_terminal(self, p):
        self.term_input = ctk.CTkTextbox(p, height=200, fg_color=COLOR_SURFACE, text_color=COLOR_TEXT_MAIN, font=FONT_MONO)
        self.term_input.pack(fill="x", padx=20, pady=20)
        ctk.CTkButton(p, text="EXECUTE", fg_color=COLOR_ACCENT, text_color=COLOR_BG, hover_color="#00C4D9",
                      command=self.run_manual).pack(padx=20)

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
            self.btn_device_status.configure(text="❌ DISCONNECTED", fg_color=COLOR_SURFACE, border_color=COLOR_ERROR)
            self.btn_refresh.configure(text_color=COLOR_ERROR)
            self.lbl_stats.configure(text="")

    def update_stats_ui(self, level, temp):
        if not self.target_device: return
        color = COLOR_ERROR if temp > 40.0 else COLOR_TEXT_DIM
        self.lbl_stats.configure(text=f"BAT: {level}% | TEMP: {temp}°C", text_color=color)

    def force_refresh(self):
        self.btn_device_status.configure(text="Scanning...", border_color=COLOR_ACCENT)
        threading.Thread(target=self.run_adb_generic, args=("kill-server",)).start()
        threading.Thread(target=self.run_adb_generic, args=("start-server",)).start()

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
                        res = subprocess.run([adb, "-s", self.target_device, "shell", "dumpsys", "battery"], capture_output=True, text=True, startupinfo=si)
                        level = re.search(r'level: (\d+)', res.stdout)
                        temp = re.search(r'temperature: (\d+)', res.stdout)
                        if level and temp:
                            self.after(0, lambda: self.update_stats_ui(int(level.group(1)), int(temp.group(1))/10.0))
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
            self.txt_log.configure(state="normal")
            self.txt_log.insert("end", f"[{ts}] {msg}\n")
            self.txt_log.see("end")
            self.txt_log.configure(state="disabled")
        except: pass

if __name__ == "__main__":
    app = TurboCoreApp()
    app.mainloop()
