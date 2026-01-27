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
COR_PRIMARIA = "#b91c1c"
COR_PC = "#0284c7"
COR_BAT = "#15803d"
COR_FUNDO = "#000000"
COR_SUCESSO = "#22c55e"
COR_ERRO = "#ef4444"
COR_ALERTA = "#f59e0b"

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
        self.debug_log("--- INICIANDO TURBO CORE V91 - STATUS INTERATIVO ---")
        self.title("TURBO CORE V91 - STATUS INTERATIVO")
        self.geometry("540x980")
        self.resizable(False, False)
        self.configure(fg_color=COR_FUNDO)
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
            self.img_bg = ctk.CTkImage(Image.open(img_path), size=(540, 980))
        except Exception as e:
            self.debug_log(f"Erro ao carregar imagem de fundo: {e}")
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
        self.debug_log("Iniciando construção da UI...")
        self.header = ctk.CTkFrame(self, height=60, corner_radius=0, fg_color="#080808")
        self.header.pack(fill="x", side="top")

        self.frame_dev_info = ctk.CTkFrame(self.header, fg_color="transparent")
        self.frame_dev_info.pack(side="left", padx=15, pady=10)

        # V91 - Botão de Status Interativo
        self.btn_device_status = ctk.CTkButton(self.frame_dev_info, text="Buscando...", font=("Arial", 12, "bold"),
                                               fg_color="#333", width=200, command=self.abrir_gerenciador_conexao)
        self.btn_device_status.pack(side="left", padx=(0, 10))

        # Monitoramento (BAT / TEMP)
        self.lbl_stats = ctk.CTkLabel(self.frame_dev_info, text="", font=("Consolas", 12, "bold"), text_color="gray")
        self.lbl_stats.pack(side="left", padx=(10, 0))

        self.btn_refresh = ctk.CTkButton(self.frame_dev_info, text="🔄", width=30, height=30, fg_color="#222", command=self.force_refresh)
        self.btn_refresh.pack(side="left", padx=10)

        self.frame_nav = ctk.CTkFrame(self.header, fg_color="transparent")
        self.frame_nav.pack(side="right", padx=10)
        
        # Botões de Navegação
        self.btn_dash = self.create_nav_btn("MODOS", "dash")
        self.btn_special = self.create_nav_btn("ESPECIAL", "special")
        self.btn_conn = self.create_nav_btn("CONEXÃO", "conn")
        self.btn_term = self.create_nav_btn("TERMINAL", "term")

        self.main_area = ctk.CTkFrame(self, fg_color="transparent")
        self.main_area.pack(fill="both", expand=True)
        
        self.frames = {
            "dash": ctk.CTkFrame(self.main_area, fg_color="transparent"),
            "special": ctk.CTkFrame(self.main_area, fg_color="transparent"),
            "conn": ctk.CTkFrame(self.main_area, fg_color="transparent"),
            "term": ctk.CTkFrame(self.main_area, fg_color="transparent")
        }
        
        if self.img_bg:
            for f in self.frames.values():
                ctk.CTkLabel(f, text="", image=self.img_bg).place(x=0, y=0, relwidth=1, relheight=1)

        self.build_dashboard(self.frames["dash"])
        self.build_special(self.frames["special"])
        self.build_connection(self.frames["conn"])
        self.build_terminal(self.frames["term"])
        
        self.current_frame = None
        self.switch_tab("dash")
        
        self.lbl_system_status = ctk.CTkLabel(self, text="SISTEMA PRONTO", font=("Consolas", 11), text_color="#555", fg_color="black")
        self.lbl_system_status.pack(fill="x", side="bottom", ipady=2)
        self.debug_log("UI construída.")

    def abrir_gerenciador_conexao(self):
        # V91 - Assistente de Conexão
        toplevel = ctk.CTkToplevel(self)
        toplevel.title("Assistente de Conexão")
        toplevel.geometry("600x450")
        toplevel.attributes("-topmost", True)

        # Layout 2 Colunas
        frame_tools = ctk.CTkFrame(toplevel)
        frame_tools.pack(side="left", fill="both", expand=True, padx=10, pady=10)

        frame_tutorial = ctk.CTkFrame(toplevel, fg_color="#111")
        frame_tutorial.pack(side="right", fill="both", expand=True, padx=10, pady=10)

        # Coluna Ferramentas
        ctk.CTkLabel(frame_tools, text="FERRAMENTAS", font=("Arial", 14, "bold")).pack(pady=10)

        # Pareamento
        ctk.CTkLabel(frame_tools, text="1. PAREAMENTO (Wireless Debugging)", text_color="#fbbf24").pack(pady=(10,5))
        ip_pair_entry = ctk.CTkEntry(frame_tools, placeholder_text="IP:PORTA (Ex: 192.168.0.5:40000)")
        ip_pair_entry.pack(fill="x", padx=10, pady=5)
        code_entry = ctk.CTkEntry(frame_tools, placeholder_text="CÓDIGO (6 Dígitos)")
        code_entry.pack(fill="x", padx=10, pady=5)

        def do_pair():
            addr = ip_pair_entry.get(); code = code_entry.get()
            if addr and code: threading.Thread(target=lambda: self.run_adb_generic(f"pair {addr} {code}")).start()

        ctk.CTkButton(frame_tools, text="PAREAR", fg_color="#fbbf24", text_color="black", command=do_pair).pack(pady=5)

        # Conexão
        ctk.CTkLabel(frame_tools, text="2. CONEXÃO (ADB Connect)", text_color=COR_PRIMARIA).pack(pady=(20,5))
        ip_conn_entry = ctk.CTkEntry(frame_tools, placeholder_text="IP:PORTA (Ex: 192.168.0.5:5555)")
        ip_conn_entry.pack(fill="x", padx=10, pady=5)

        def do_connect():
            addr = ip_conn_entry.get()
            if addr: threading.Thread(target=lambda: self.run_adb_generic(f"connect {addr}")).start()

        ctk.CTkButton(frame_tools, text="CONECTAR", fg_color=COR_PRIMARIA, command=do_connect).pack(pady=5)

        # Coluna Tutorial
        ctk.CTkLabel(frame_tutorial, text="COMO CONECTAR?", font=("Arial", 14, "bold")).pack(pady=10)
        tut_text = """
1. No Celular, ative as
   'Opções do Desenvolvedor'.

2. Ative a 'Depuração USB'.

3. (Para Wi-Fi) Ative a
   'Depuração por Wi-Fi'.

4. Entre na opção 'Depuração por Wi-Fi'
   e clique em 'Parear com Código'.

5. Copie o IP, Porta e Código
   para os campos ao lado.

OBS: O IP de Pareamento e o IP
de Conexão geralmente têm
PORTAS DIFERENTES!
        """
        lbl_tut = ctk.CTkLabel(frame_tutorial, text=tut_text, justify="left", font=("Consolas", 11), text_color="#ccc")
        lbl_tut.pack(padx=10, pady=10)

    def create_nav_btn(self, text, mode):
        btn = ctk.CTkButton(self.frame_nav, text=text, fg_color="transparent", width=80, font=("Arial", 11, "bold"), 
                            command=lambda: self.switch_tab(mode))
        btn.pack(side="left", padx=2)
        return btn

    def switch_tab(self, mode):
        self.debug_log(f"Trocando aba para: {mode}")
        if self.current_frame: self.current_frame.place_forget()
        self.current_frame = self.frames[mode]
        self.current_frame.place(x=0, y=0, relwidth=1, relheight=1)
        
        # Lógica de Cores da Aba Ativa
        btns = {"dash": self.btn_dash, "special": self.btn_special, "conn": self.btn_conn, "term": self.btn_term}
        
        for k, b in btns.items():
            if k == mode:
                # Cor diferente para cada aba
                if k == "dash": color = COR_PC
                elif k == "special": color = COR_PRIMARIA
                else: color = "#333"
                b.configure(text_color="white", fg_color=color)
            else:
                b.configure(text_color="#888", fg_color="transparent")

    # --- DASHBOARD ---
    def build_dashboard(self, p):
        c = ctk.CTkFrame(p, fg_color="transparent"); c.pack(fill="both", padx=20, pady=20)
        
        # 1. PERFORMANCE
        self.create_menu(c, "🔥 DESEMPENHO MÁXIMO", list(MODOS_PERF.keys()), self.aplicar_perf, COR_PRIMARIA)

        # 2. MODO PC
        self.create_menu(c, "🖥️ MODOS PC (MONITOR)", list(MODOS_PC.keys()), self.iniciar_pc, COR_PC)

        # Opções de Lançamento (NOVO V90)
        frame_opts = ctk.CTkFrame(c, fg_color="#111", border_width=1, border_color="#333")
        frame_opts.pack(fill="x", pady=(5, 15))
        ctk.CTkLabel(frame_opts, text="OPÇÕES DE LANÇAMENTO", font=("Arial", 10, "bold"), text_color="gray").pack(pady=(5,2))

        self.chk_video = ctk.CTkCheckBox(frame_opts, text="Gravar Vídeo", font=("Arial", 11), text_color="#ccc", fg_color=COR_PC)
        self.chk_video.pack(side="left", padx=10, pady=10)

        self.chk_audio = ctk.CTkCheckBox(frame_opts, text="Gravar Áudio", font=("Arial", 11), text_color="#ccc", fg_color=COR_PC)
        self.chk_audio.pack(side="left", padx=10, pady=10)
        self.chk_audio.select() # Padrão: Ativado

        self.chk_ghost = ctk.CTkCheckBox(frame_opts, text="Modo Fantasma (Tela Off)", font=("Arial", 11), text_color="#ccc", fg_color=COR_PC)
        self.chk_ghost.pack(side="left", padx=10, pady=10)

        # 3. BATERIA
        self.create_menu(c, "🔋 ECONOMIA DE BATERIA", list(MODOS_BAT.keys()), self.aplicar_bat, COR_BAT)
            
        # UTILITÁRIOS
        ctk.CTkLabel(c, text="FERRAMENTAS GERAIS", font=("Arial", 12, "bold")).pack(anchor="w", pady=(30,5))

        # Instalador de APK
        ctk.CTkButton(c, text="INSTALAR APK 📥", fg_color="#fbbf24", text_color="black", height=40, font=("Arial", 11, "bold"), command=self.install_apk).pack(fill="x", pady=5)

        ctk.CTkButton(c, text="RESTAURAR ORIGINAL 🔄", fg_color="#333", height=40, font=("Arial", 11, "bold"), command=self.restaurar_padrao).pack(fill="x", pady=5)
        
        self.txt_log = ctk.CTkTextbox(c, height=120, fg_color="#050505", text_color="#0f0", font=("Consolas", 10))
        self.txt_log.pack(fill="x", pady=20)

    # --- MENU INTELIGENTE (ATUALIZA O NOME) ---
    def create_menu(self, parent, title, values, cmd, color):
        ctk.CTkLabel(parent, text=title, font=("Arial", 13, "bold"), text_color=color).pack(anchor="w", pady=(15,2))
        
        f = ctk.CTkFrame(parent, fg_color="transparent")
        f.pack(fill="x")
        
        # Menu Dropdown (Com callback para mudar o texto)
        m = ctk.CTkOptionMenu(f, values=["Selecionar..."] + values, fg_color="#111", button_color=color, width=350,
                              command=lambda v: self.on_menu_select(v, cmd, m))
        m.pack(side="left", fill="x", expand=True, padx=(0, 5))
        
        # Botão de Ajuda
        ctk.CTkButton(f, text="?", width=40, fg_color="#222", hover_color="#444", 
                      command=lambda: self.show_info(m.get())).pack(side="right")
        
        # Salva referência para resets futuros
        setattr(self, f"menu_{title.split()[0]}", m) 

    def on_menu_select(self, value, command_func, menu_widget):
        if value != "Selecionar...":
            self.debug_log(f"Menu selecionado: {value}")
            menu_widget.set(value) # Atualiza o texto do botão para o modo escolhido
            command_func(value)    # Executa a função

    def show_info(self, mode_name):
        if mode_name == "Selecionar...":
            messagebox.showinfo("Ajuda", "Selecione um modo primeiro para ver os detalhes.")
            return
        
        info = HELP_TEXTS.get(mode_name, "Descrição técnica não disponível.")
        messagebox.showinfo(f"Info: {mode_name}", info)

    # --- ABA ESPECIAL ---
    def build_special(self, p):
        c = ctk.CTkFrame(p, fg_color="transparent")
        c.pack(expand=True, fill="both", padx=30, pady=40)

        ctk.CTkLabel(c, text="MODO COMPETITIVO", font=("Impact", 32), text_color="#fbbf24").pack(pady=(0, 20))
        
        btn_ff = ctk.CTkButton(c, text="FREE FIRE MAX 🎯\n(OTIMIZAR AGORA)", font=("Arial", 20, "bold"), 
                               fg_color="#b91c1c", hover_color="#991b1b", height=100, corner_radius=15,
                               command=self.ativar_free_fire)
        btn_ff.pack(fill="x", pady=10)

        # Keymapping Toggle
        self.sw_keymap = ctk.CTkSwitch(c, text="ATIVAR KEYMAPPING (Espaço + WASD)", command=self.toggle_keymapping,
                                       font=("Arial", 12, "bold"), text_color="white", progress_color=COR_SUCESSO)
        self.sw_keymap.pack(pady=15)

        ctk.CTkLabel(c, text="JOGOS ESPECÍFICOS (EM BREVE)", font=("Arial", 12, "bold"), text_color="gray").pack(pady=(30, 10))
        
        grid = ctk.CTkFrame(c, fg_color="transparent")
        grid.pack(fill="x")
        self.create_placeholder_btn(grid, "COD MOBILE 💀", 0)
        self.create_placeholder_btn(grid, "PUBG 🔫", 1)
        self.create_placeholder_btn(grid, "GENSHIN ⚔️", 2)

        ctk.CTkLabel(c, text="Detalhes do Modo Free Fire:", font=("Arial", 14, "bold")).pack(anchor="w", pady=(30,5))
        desc = """
        • Resolução: 540x1170 (Foco em FPS)
        • Densidade: 140 DPI (Mira Precisa)
        • Remove Animações do Sistema
        • Ativa Modo Performance Android
        • Limpa RAM antes de jogar
        """
        ctk.CTkLabel(c, text=desc, font=("Consolas", 12), justify="left", anchor="w").pack(anchor="w")

    def create_placeholder_btn(self, parent, text, col):
        btn = ctk.CTkButton(parent, text=text, fg_color="#222", text_color="#666", state="disabled", width=140, height=50)
        btn.grid(row=0, column=col, padx=5, sticky="ew")
        parent.grid_columnconfigure(col, weight=1)

    def ativar_free_fire(self):
        self.debug_log("Ativando modo Free Fire...")
        if not self.target_device: return messagebox.showerror("ERRO", "Conecte o celular!")
        self.log("ATIVANDO MODO FREE FIRE...")
        cmds = "wm size 540x1170; wm density 140; settings put global window_animation_scale 0; settings put global transition_animation_scale 0; settings put global animator_duration_scale 0; cmd power set-mode 1; am kill-all"
        self.run_adb_cmd_string(cmds)
        messagebox.showinfo("SUCESSO", "MODO FREE FIRE ATIVADO!\nBom jogo!")

    # --- EXECUÇÃO ---
    def run_adb_cmd_string(self, cmd_string):
        self.debug_log(f"Executando comando ADB: {cmd_string}")
        def t():
            exe = os.path.join(self.bin_dir, "adb.exe")
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            for c in cmd_string.split(";"):
                if c.strip(): subprocess.run([exe, "-s", self.target_device, "shell", c.strip()], startupinfo=si)
            self.log("Comandos aplicados.")
            self.debug_log("Comandos finalizados.")
        threading.Thread(target=t).start()

    def iniciar_pc(self, choice):
        if not self.target_device: return
        cfg = MODOS_PC[choice]
        exe_adb = os.path.join(self.bin_dir, "adb.exe")
        exe_scrcpy = os.path.join(self.bin_dir, "scrcpy.exe")
        
        # Lê opções do V90
        opt_video = self.chk_video.get()
        opt_audio = self.chk_audio.get()
        opt_ghost = self.chk_ghost.get()

        def thread_pc():
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            self.log(f"Ativando {choice}...")
            self.debug_log(f"Iniciando thread PC Mode: {choice}")
            
            cmds = [
                "wm size reset", "wm density reset",
                f"wm size {cfg['size']}", f"wm density {cfg['density']}",
                "settings put system user_rotation 1", "settings put system accelerometer_rotation 0"
            ]
            for c in cmds: subprocess.run([exe_adb, "-s", self.target_device, "shell", c], startupinfo=si)
            
            time.sleep(2.5)

            scrcpy_args = list(cfg['scrcpy'])

            # Aplica flags V90
            if opt_video:
                timestamp = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
                filename = f"REC_{timestamp}.mp4"
                filepath = os.path.join(self.caps_dir, filename)
                scrcpy_args += ["--record", filepath, "--record-format=mp4"]
                self.log(f"Gravando: {filename}")
                self.debug_log(f"Configurando gravação: {filepath}")

            if not opt_audio:
                scrcpy_args += ["--no-audio"]
                self.debug_log("Audio desativado.")

            if opt_ghost:
                scrcpy_args += ["--turn-screen-off"]
                self.debug_log("Modo Fantasma (Tela Off) ativado.")

            self.debug_log(f"Executando Scrcpy com args: {scrcpy_args}")
            try:
                subprocess.run([exe_scrcpy, "-s", self.target_device] + scrcpy_args, cwd=self.bin_dir, startupinfo=si)
            except Exception as e:
                self.debug_log(f"ERRO CRITICO AO EXECUTAR SCRCPY: {e}")
            
            cmds_reset = ["wm size reset", "wm density reset", "settings put system user_rotation 0", "settings put system accelerometer_rotation 1"]
            for c in cmds_reset: subprocess.run([exe_adb, "-s", self.target_device, "shell", c], startupinfo=si)
            self.debug_log("Modo PC encerrado.")

        threading.Thread(target=thread_pc, daemon=True).start()

    def aplicar_perf(self, choice):
        self.debug_log(f"Aplicando Perf: {choice}")
        self.run_adb_cmd_string(MODOS_PERF[choice])

    def aplicar_bat(self, choice):
        self.debug_log(f"Aplicando Bateria: {choice}")
        self.run_adb_cmd_string(MODOS_BAT[choice])

    def restaurar_padrao(self):
        if not self.target_device: return
        self.log("Restaurando...")
        cmds = "wm size reset; wm density reset; settings put system user_rotation 0; settings put system accelerometer_rotation 1; settings put global low_power 0; settings put system screen_brightness 100; settings put global window_animation_scale 1; settings put global transition_animation_scale 1; settings put global animator_duration_scale 1"
        self.run_adb_cmd_string(cmds)
        messagebox.showinfo("Sucesso", "Celular restaurado!")

    def install_apk(self):
        if not self.target_device: return messagebox.showerror("Erro", "Conecte o dispositivo primeiro!")

        file_path = filedialog.askopenfilename(filetypes=[("Android Package", "*.apk")])
        if not file_path: return

        self.log(f"Instalando: {os.path.basename(file_path)}...")
        self.debug_log(f"Iniciando instalação de APK: {file_path}")

        def run_install():
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            exe = os.path.join(self.bin_dir, "adb.exe")

            try:
                res = subprocess.run([exe, "-s", self.target_device, "install", "-r", file_path], capture_output=True, text=True, startupinfo=si)
                if "Success" in res.stdout:
                    self.log("Instalação Concluída!")
                    self.after(0, lambda: messagebox.showinfo("Sucesso", "APK Instalado com Sucesso!"))
                else:
                    self.log(f"Erro na instalação: {res.stderr}")
                    self.debug_log(f"Erro install: {res.stderr}")
                    self.after(0, lambda: messagebox.showerror("Erro", f"Falha ao instalar:\n{res.stderr}"))
            except Exception as e:
                self.debug_log(f"Exceção install: {e}")

        threading.Thread(target=run_install).start()

    # --- CONEXÃO ---
    def build_connection(self, p):
        # Passo 1: Pareamento
        frame_pair = ctk.CTkFrame(p, fg_color="#111", border_width=1, border_color="#333")
        frame_pair.pack(fill="x", padx=20, pady=(20, 10))
        ctk.CTkLabel(frame_pair, text="PASSO 1: PAREAMENTO (Wireless Debugging)", font=("Arial", 12, "bold"), text_color="#fbbf24").pack(pady=10)

        self.ent_pair_ip = ctk.CTkEntry(frame_pair, placeholder_text="IP:PORTA (Ex: 192.168.1.5:40000)", width=300)
        self.ent_pair_ip.pack(pady=5)
        self.ent_pair_code = ctk.CTkEntry(frame_pair, placeholder_text="CÓDIGO DE PAREAMENTO", width=300)
        self.ent_pair_code.pack(pady=5)
        ctk.CTkButton(frame_pair, text="PAREAR DISPOSITIVO", fg_color="#fbbf24", text_color="black", command=self.wifi_pair).pack(pady=15)

        # Passo 2: Conexão
        frame_conn = ctk.CTkFrame(p, fg_color="#111", border_width=1, border_color="#333")
        frame_conn.pack(fill="x", padx=20, pady=10)
        ctk.CTkLabel(frame_conn, text="PASSO 2: CONEXÃO (ADB)", font=("Arial", 12, "bold"), text_color=COR_PRIMARIA).pack(pady=10)

        self.ent_ip = ctk.CTkEntry(frame_conn, placeholder_text="IP:PORTA (Ex: 192.168.1.5:5555)", width=300)
        self.ent_ip.pack(pady=5)

        btn_frame = ctk.CTkFrame(frame_conn, fg_color="transparent")
        btn_frame.pack(pady=15)
        ctk.CTkButton(btn_frame, text="ESCANEAR REDE 🔎", fg_color="#333", width=140, command=self.scan_network).pack(side="left", padx=5)
        ctk.CTkButton(btn_frame, text="CONECTAR", fg_color=COR_PRIMARIA, width=140, command=self.wifi_connect).pack(side="left", padx=5)


    def build_terminal(self, p):
        ctk.CTkLabel(p, text="TERMINAL MANUAL", font=("Arial", 14, "bold")).pack(pady=10)
        self.term_input = ctk.CTkTextbox(p, height=200); self.term_input.pack(fill="x", padx=20, pady=10)
        ctk.CTkButton(p, text="EXECUTAR COMANDO", command=self.run_manual).pack(padx=20, pady=10)

    # --- HELPERS ---
    def get_device_name(self):
        try:
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            exe = os.path.join(self.bin_dir, "adb.exe")
            res = subprocess.run([exe, "-s", self.target_device, "shell", "getprop ro.product.model"], capture_output=True, text=True, startupinfo=si, timeout=2)
            name = res.stdout.strip()
            return name if name else self.target_device
        except: return self.target_device

    def update_status_ui(self, connected):
        if connected:
            self.btn_device_status.configure(text=f"📱 {self.device_model}", fg_color=COR_SUCESSO)
            self.btn_refresh.configure(fg_color="#14532d")
        else:
            self.btn_device_status.configure(text="❌ DESCONECTADO (Clique aqui)", fg_color=COR_ERRO)
            self.btn_refresh.configure(fg_color="#222")
            self.lbl_stats.configure(text="")

    def update_stats_ui(self, level, temp):
        if not self.target_device: return

        temp_color = COR_ERRO if temp > 40.0 else "gray"
        text = f" | 🔋 {level}% | 🌡️ {temp}°C"
        self.lbl_stats.configure(text=text, text_color=temp_color)

    def force_refresh(self):
        self.debug_log("Forçando refresh ADB...")
        self.btn_device_status.configure(text="Buscando...", fg_color="orange")
        threading.Thread(target=self.run_adb_generic, args=("kill-server",)).start()
        threading.Thread(target=self.run_adb_generic, args=("start-server",)).start()

    def start_monitor(self):
        self.debug_log("Iniciando monitor de dispositivos...")
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
                                self.log(f"Conectado: {self.device_model} ({new_id})")
                                self.debug_log(f"Dispositivo detectado: {new_id}")
                                self.after(0, lambda: self.update_status_ui(True))
                        else:
                            if self.target_device:
                                self.target_device = ""
                                self.log("Desconectado.")
                                self.debug_log("Dispositivo desconectado.")
                                self.after(0, lambda: self.update_status_ui(False))
                except Exception as e:
                    print(f"Erro no monitor: {e}")
                    pass
                time.sleep(3)
        threading.Thread(target=loop, daemon=True).start()

    def start_battery_monitor(self):
        self.debug_log("Iniciando monitor de bateria/temp...")
        def loop():
            adb = os.path.join(self.bin_dir, "adb.exe")
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            while True:
                if self.target_device:
                    try:
                        res = subprocess.run([adb, "-s", self.target_device, "shell", "dumpsys", "battery"], capture_output=True, text=True, startupinfo=si)
                        output = res.stdout

                        level = re.search(r'level: (\d+)', output)
                        temp = re.search(r'temperature: (\d+)', output)

                        if level and temp:
                            l_val = int(level.group(1))
                            t_val = int(temp.group(1)) / 10.0 # Converte 370 para 37.0
                            self.after(0, lambda: self.update_stats_ui(l_val, t_val))
                    except Exception as e:
                        self.debug_log(f"Erro Battery Monitor: {e}")
                time.sleep(5)
        threading.Thread(target=loop, daemon=True).start()

    def run_manual(self):
        cmd = self.term_input.get("0.0", "end").strip()
        if cmd: threading.Thread(target=lambda: subprocess.Popen(cmd, cwd=self.bin_dir, shell=True, startupinfo=subprocess.STARTUPINFO())).start()

    def run_adb_generic(self, cmd):
        si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
        exe = os.path.join(self.bin_dir, "adb.exe")
        subprocess.run([exe] + cmd.split(), startupinfo=si)

    def wifi_connect(self):
        addr = self.ent_ip.get()
        self.debug_log(f"Tentando conectar Wi-Fi: {addr}")
        if addr: threading.Thread(target=lambda: self.run_adb_generic(f"connect {addr}")).start()

    def wifi_pair(self):
        addr = self.ent_pair_ip.get(); code = self.ent_pair_code.get()
        self.debug_log(f"Tentando parear: {addr} code={code}")
        if addr and code: threading.Thread(target=lambda: self.run_adb_generic(f"pair {addr} {code}")).start()

    def scan_network(self):
        self.log("Escaneando rede por dispositivos (Porta 5555)...")
        self.debug_log("Iniciando scan de rede...")

        def run_scan():
            try:
                local_ip = socket.gethostbyname(socket.gethostname())
                subnet = '.'.join(local_ip.split('.')[:-1]) + '.'
                self.debug_log(f"Subnet detectada: {subnet}0/24")
                found = []

                with concurrent.futures.ThreadPoolExecutor(max_workers=50) as executor:
                    futures = {executor.submit(self._check_ip, f"{subnet}{i}"): f"{subnet}{i}" for i in range(1, 255)}
                    for future in concurrent.futures.as_completed(futures):
                        ip = futures[future]
                        if future.result():
                            found.append(ip)
                            self.debug_log(f"SCAN: Encontrado {ip}")

                if found:
                    self.log(f"Encontrados: {', '.join(found)}")
                    # Preenche o primeiro encontrado
                    self.after(0, lambda: self.ent_ip.delete(0, 'end'))
                    self.after(0, lambda: self.ent_ip.insert(0, f"{found[0]}:5555"))
                else:
                    self.log("Nenhum dispositivo com porta 5555 aberta encontrado.")
            except Exception as e:
                self.debug_log(f"ERRO NO SCANNER: {e}")

        threading.Thread(target=run_scan).start()

    def _check_ip(self, ip):
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(0.5)
            result = sock.connect_ex((ip, 5555))
            sock.close()
            return result == 0
        except:
            return False

    def debug_log(self, msg):
        # Escreve no console real do sistema (CMD) para debug
        try:
            sys.__stdout__.write(f"[DEBUG] {msg}\n")
            sys.__stdout__.flush()
        except: pass

    def log(self, msg):
        ts = datetime.datetime.now().strftime("%H:%M:%S")
        self.debug_log(f"GUI LOG: {msg}") # Espelha logs da GUI no console
        try:
            self.txt_log.configure(state="normal")
            self.txt_log.insert("end", f"[{ts}] {msg}\n")
            self.txt_log.see("end")
            self.txt_log.configure(state="disabled")
        except: pass

    def validate_installation(self):
        if not os.path.exists(self.bin_dir): messagebox.showwarning("ATENÇÃO", "Pasta 'bin' não encontrada!")

if __name__ == "__main__":
    app = TurboCoreApp()
    app.mainloop()
