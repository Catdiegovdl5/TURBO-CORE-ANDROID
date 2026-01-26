import customtkinter as ctk
from tkinter import messagebox
import subprocess
import threading
import os
import sys
import time
import webbrowser
import datetime
from PIL import Image

# --- CONFIGURAÇÃO VISUAL ---
ctk.set_appearance_mode("Dark")
COR_PRIMARIA = "#b91c1c"  # Vermelho Sangue
COR_FUNDO = "#000000"     # Preto

# --- DICIONÁRIO DE DESCRIÇÕES ---
DESCRICOES = {
    # 1. DESEMPENHO
    "Gamer Ultimate (FPS Máximo)": "🎮 FPS EXTREMO\nResolução 432x960 | DPI 160.\nDesativa Samsung GOS e libera CPU térmica.",
    "Ultimate Desempenho (Bruta)": "☢️ POTÊNCIA TOTAL\nResolução 360x800 | DPI 120.\nMata todos os processos e desativa controle térmico.",
    "Balanceado": "⚖️ USO MISTO\nReduz a carga da GPU em 20% para jogos leves.",

    # 2. BATERIA
    "Economia Normal": "🍃 LEVE\nAtiva bateria adaptativa e economia nativa.",
    "Super Economia": "🔋 MÉDIA\nResolução 576p. Desliga Bluetooth e Sincronização.",
    "Ultimate Economia": "🪫 DEEP SLEEP\nResolução 360p, Brilho 0, Hibernação forçada.",

    # 3. PC / ESPELHAMENTO
    "PC Lite (Eco/Leve)": "💻 MODO LEVE (30 FPS)\nIdeal para leitura ou bateria baixa.\n600p, 4Mbps, Sem áudio.",
    "PC Workstation (Soberano)": "🖥️ MODO SOBERANO (90 FPS)\nO equilíbrio perfeito. 720p (Desktop), 16Mbps.\nIdeal para multitarefa e trabalho.",
    "PC Gamer (Apex/H265)": "🕹️ MODO APEX (BAIXA LATÊNCIA)\nCodec H265, 24Mbps, Buffer de áudio mínimo.\nFeito para jogar competitivo pelo PC."
}

class TurboCoreApp(ctk.CTk):
    def __init__(self):
        super().__init__()
        self.title("TURBO CORE V51 - TERMINAL EDITION")
        self.geometry("500x900") # Mais largo para o terminal
        self.resizable(False, False)
        self.configure(fg_color=COR_FUNDO)
        self.target_device = ""

        # GESTÃO DE CAMINHOS
        if getattr(sys, 'frozen', False):
            self.app_dir = os.path.dirname(sys.executable)
        else:
            self.app_dir = os.path.dirname(os.path.abspath(__file__))

        self.bin_dir = os.path.join(self.app_dir, "bin")

        # IMAGEM DE FUNDO
        try:
            img_path = os.path.join(self.app_dir, "fundo_chip.jpg")
            self.img_bg = ctk.CTkImage(Image.open(img_path), size=(500, 900))
        except: self.img_bg = None

        # --- NOVA INTERFACE (MODERNA) ---
        self.nav_frame = ctk.CTkFrame(self, height=50, corner_radius=0, fg_color="#050505")
        self.nav_frame.pack(fill="x", side="top")

        self.content_container = ctk.CTkFrame(self, corner_radius=0, fg_color="transparent")
        self.content_container.pack(fill="both", expand=True)

        # Botões de Navegação (Agora com TERMINAL)
        self.btn_dash = self.create_nav_btn(self.nav_frame, "CENTRAL", "dash")
        self.btn_conn = self.create_nav_btn(self.nav_frame, "CONEXÃO", "conn")
        self.btn_pair = self.create_nav_btn(self.nav_frame, "PAREAR", "pair")
        self.btn_term = self.create_nav_btn(self.nav_frame, "TERMINAL", "term")
        
        self.btn_dash.pack(side="left", expand=True, fill="both", padx=2, pady=2)
        self.btn_conn.pack(side="left", expand=True, fill="both", padx=2, pady=2)
        self.btn_pair.pack(side="left", expand=True, fill="both", padx=2, pady=2)
        self.btn_term.pack(side="left", expand=True, fill="both", padx=2, pady=2)

        # Frames das Páginas
        self.frame_dash = ctk.CTkFrame(self.content_container, corner_radius=0, fg_color="transparent")
        self.frame_conn = ctk.CTkFrame(self.content_container, corner_radius=0, fg_color="transparent")
        self.frame_pair = ctk.CTkFrame(self.content_container, corner_radius=0, fg_color="transparent")
        self.frame_term = ctk.CTkFrame(self.content_container, corner_radius=0, fg_color="transparent")

        if self.img_bg:
            ctk.CTkLabel(self.frame_dash, text="", image=self.img_bg).place(x=0, y=0, relwidth=1, relheight=1)

        self.setup_dashboard(self.frame_dash)
        self.setup_connect_tab(self.frame_conn)
        self.setup_pair_tab(self.frame_pair)
        self.setup_terminal_tab(self.frame_term)

        self.current_frame = None
        self.switch_tab("dash") # Inicia na central
        
        self.lbl_status = ctk.CTkLabel(self, text="SISTEMA OFFLINE", font=("Consolas", 12, "bold"), text_color="#555555", fg_color="#000000")
        self.lbl_status.pack(fill="x", side="bottom", ipady=5)

        # --- DEBUG CONSOLE ---
        self.log_console = ctk.CTkTextbox(self, height=120, fg_color="#111", text_color="#0f0", font=("Consolas", 10))
        self.log_console.pack(fill="x", side="bottom")
        self.log_console.configure(state="disabled")

        self.start_device_monitor()
        self.after(500, self.validate_installation)

    def log(self, message):
        timestamp = datetime.datetime.now().strftime("%H:%M:%S")
        full_msg = f"[{timestamp}] {message}\n"

        self.log_console.configure(state="normal")
        self.log_console.insert("end", full_msg)
        self.log_console.see("end")
        self.log_console.configure(state="disabled")

    def create_nav_btn(self, parent, text, mode):
        return ctk.CTkButton(parent, text=text, fg_color="transparent", corner_radius=5,
                             font=("Arial", 11, "bold"),
                             hover_color="#222",
                             command=lambda: self.switch_tab(mode))

    def switch_tab(self, mode):
        target_frame = {"dash": self.frame_dash, "conn": self.frame_conn, "pair": self.frame_pair, "term": self.frame_term}[mode]
        target_btn = {"dash": self.btn_dash, "conn": self.btn_conn, "pair": self.btn_pair, "term": self.btn_term}[mode]

        if self.current_frame == target_frame: return

        # Visual dos botões
        for btn in [self.btn_dash, self.btn_conn, self.btn_pair, self.btn_term]:
            btn.configure(fg_color="transparent", text_color="#888")
        target_btn.configure(fg_color="#222", text_color="white")

        # Animação
        if self.current_frame is None:
            target_frame.place(x=0, y=0, relwidth=1, relheight=1)
            self.current_frame = target_frame
        else:
            self.animate_transition(self.current_frame, target_frame)

    def animate_transition(self, old_frame, new_frame):
        width = 500
        new_frame.place(x=width, y=0, relwidth=1, relheight=1)
        new_frame.lift()

        def step(i):
            progress = i / 15 # 15 steps for speed
            offset = width * progress
            old_frame.place(x=0 - offset, y=0, relwidth=1, relheight=1)
            new_frame.place(x=width - offset, y=0, relwidth=1, relheight=1)

            if i < 15:
                self.after(10, lambda: step(i+1))
            else:
                old_frame.place_forget()
                self.current_frame = new_frame
        step(0)

    def create_menu(self, parent, label_text, values, cmd_func, color, bg_color):
        ctk.CTkLabel(parent, text=label_text, font=("Arial", 11, "bold"), text_color=color).pack(anchor="w", pady=(10,0))
        f = ctk.CTkFrame(parent, fg_color="transparent")
        f.pack(fill="x", pady=2)
        m = ctk.CTkOptionMenu(f, values=values, fg_color=bg_color, button_color=color, width=350, command=cmd_func, corner_radius=10)
        m.pack(side="left", padx=(0,5))
        ctk.CTkButton(f, text="?", width=30, fg_color="#333", corner_radius=10, command=lambda: self.show_info(m.get())).pack(side="left")
        return m

    def setup_dashboard(self, parent):
        c = ctk.CTkFrame(parent, fg_color="transparent")
        c.pack(fill="both", expand=True, padx=15, pady=15)

        # 1. PERFORMANCE
        self.menu_perf = self.create_menu(c, "🔥 1. MODO DESEMPENHO", 
            ["Selecionar...", "Gamer Ultimate (FPS Máximo)", "Ultimate Desempenho (Bruta)", "Balanceado"], 
            self.apply_perf_mode, COR_PRIMARIA, "#330000")

        # 2. BATERIA
        self.menu_bat = self.create_menu(c, "🔋 2. MODO BATERIA", 
            ["Selecionar...", "Economia Normal", "Super Economia", "Ultimate Economia"], 
            self.apply_bat_mode, "#15803d", "#052e16")

        # 3. MODO PC (CORRIGIDO)
        self.menu_pc = self.create_menu(c, "🖥️ 3. MODO PC / ESPELHAMENTO", 
            ["Selecionar...", "PC Lite (Eco/Leve)", "PC Workstation (Soberano)", "PC Gamer (Apex/H265)"], 
            self.apply_pc_selector, "#0284c7", "#0c4a6e")

        # AÇÕES GLOBAIS
        ctk.CTkLabel(c, text="⚠️ AÇÕES DE SISTEMA", font=("Arial", 11, "bold"), text_color="white").pack(anchor="w", pady=(30,0))
        ctk.CTkButton(c, text="TESTAR ARQUIVOS DA PASTA 🛠️", fg_color="#444", command=self.check_files).pack(fill="x", pady=5)
        ctk.CTkButton(c, text="🚨 RESETAR TUDO (PADRÃO)", fg_color="#b91c1c", height=40, font=("Arial", 12, "bold"), command=self.emergency_reset).pack(fill="x", pady=(10,0))

    def setup_connect_tab(self, parent):
        f = ctk.CTkFrame(parent, fg_color="transparent"); f.pack(fill="both", expand=True, padx=30, pady=30)
        ctk.CTkLabel(f, text="LINK WIRELESS", font=("Arial", 20, "bold")).pack(pady=20)
        self.entry_ip = ctk.CTkEntry(f, placeholder_text="IP (ex: 192.168.0.5)", height=45)
        self.entry_ip.pack(fill="x", pady=10)
        self.entry_port = ctk.CTkEntry(f, placeholder_text="PORTA (ex: 5555)", height=45)
        self.entry_port.pack(fill="x", pady=10)
        ctk.CTkButton(f, text="CONECTAR 🔗", fg_color=COR_PRIMARIA, height=50, command=self.connect_wifi).pack(fill="x", pady=30)

    def setup_pair_tab(self, parent):
        f = ctk.CTkFrame(parent, fg_color="transparent"); f.pack(fill="both", expand=True, padx=20, pady=20)
        ctk.CTkLabel(f, text="PAREAMENTO ANDROID 11+", font=("Arial", 16, "bold"), text_color="#fbbf24").pack(pady=20)
        self.ep_ip = ctk.CTkEntry(f, placeholder_text="IP:Porta Pareamento"); self.ep_ip.pack(fill="x", pady=5)
        self.ep_code = ctk.CTkEntry(f, placeholder_text="Código 6 dígitos"); self.ep_code.pack(fill="x", pady=5)
        ctk.CTkButton(f, text="PAREAR 🔑", fg_color="#fbbf24", text_color="black", command=self.pair_wifi).pack(fill="x", pady=20)

    def setup_terminal_tab(self, parent):
        f = ctk.CTkFrame(parent, fg_color="transparent")
        f.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(f, text="TERMINAL DE COMANDO MANUAL", font=("Arial", 16, "bold"), text_color="white").pack(pady=(0,10))
        ctk.CTkLabel(f, text="Cole seu comando ADB ou Scrcpy aqui:", font=("Arial", 10)).pack(anchor="w")

        self.txt_terminal = ctk.CTkTextbox(f, height=150, fg_color="#222", text_color="#0f0", font=("Consolas", 11))
        self.txt_terminal.pack(fill="x", pady=10)
        self.txt_terminal.insert("0.0", "scrcpy -s DISPOSITIVO --video-bit-rate=20M --max-fps=90")

        def run_manual():
            cmd_text = self.txt_terminal.get("0.0", "end").strip()
            if not cmd_text: return
            if "DISPOSITIVO" in cmd_text and self.target_device:
                cmd_text = cmd_text.replace("DISPOSITIVO", self.target_device)

            self.log(f"EXECUTANDO MANUAL: {cmd_text}")

            # Tratamento básico para scrcpy vs adb
            try:
                si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW

                # Se for Scrcpy
                if cmd_text.startswith("scrcpy"):
                    # Tenta achar o exe na pasta bin
                    exe = os.path.join(self.bin_dir, "scrcpy.exe")
                    if not os.path.exists(exe): exe = "scrcpy"

                    # Substitui 'scrcpy' pelo caminho completo e quebra em lista
                    parts = [exe] + cmd_text.split()[1:]
                    subprocess.Popen(parts, startupinfo=si, cwd=self.bin_dir)
                    self.log("Processo Scrcpy iniciado (Background)")

                else:
                    # Assume shell/adb
                    exe = os.path.join(self.bin_dir, "adb.exe")
                    if not os.path.exists(exe): exe = "adb"

                    # Se não começar com 'adb', assume que é pra rodar direto
                    if cmd_text.startswith("adb"):
                        parts = [exe] + cmd_text.split()[1:]
                    else:
                        parts = cmd_text.split()

                    res = subprocess.run(parts, capture_output=True, text=True, startupinfo=si, cwd=self.bin_dir)
                    self.log(f"SAÍDA:\n{res.stdout}")
                    if res.stderr: self.log(f"ERRO:\n{res.stderr}")

            except Exception as e:
                self.log(f"ERRO NA EXECUÇÃO: {e}")

        ctk.CTkButton(f, text="EXECUTAR COMANDO 🚀", fg_color="#b91c1c", height=45, command=run_manual).pack(fill="x", pady=10)
        ctk.CTkLabel(f, text="Nota: Use 'DISPOSITIVO' no comando para injetar o ID do celular conectado.", font=("Arial", 10, "italic"), text_color="gray").pack()

    # --- HELPER: LANÇADOR MESTRE DE MODO PC ---
    def launch_pc_mode(self, adb_cmds, scrcpy_args, mode_name):
        if not self.target_device: 
            messagebox.showwarning("AVISO", "Conecte o dispositivo primeiro!")
            return
        
        exe_scrcpy = os.path.join(self.bin_dir, "scrcpy.exe")
        exe_adb = os.path.join(self.bin_dir, "adb.exe")
        if not os.path.exists(exe_adb): exe_adb = "adb"

        if not os.path.exists(exe_scrcpy):
            self.show_missing_files_dialog(["scrcpy.exe"])
            return

        def task():
            self.after(0, lambda: self.log(f"Iniciando Modo PC: {mode_name}"))
            self.lbl_status.configure(text=f"⚙️ PREPARANDO: {mode_name}...", text_color="#0284c7")
            
            # 1. Aplica as configurações no Android via ADB
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            for cmd in adb_cmds.split(';'):
                if cmd.strip():
                    self.after(0, lambda c=cmd: self.log(f"ADB Shell: {c}"))
                    subprocess.run([exe_adb, "-s", self.target_device, "shell", cmd.strip()], startupinfo=si)
            
            time.sleep(1.5)
            
            # 2. Monta o comando do Scrcpy
            base_scrcpy = [
                exe_scrcpy, 
                "-s", self.target_device, 
                "--always-on-top",
                "--stay-awake",
                "--window-title", f"TURBO_CORE_{mode_name}"
            ]
            final_cmd = base_scrcpy + scrcpy_args
            
            try:
                self.after(0, lambda: self.log(f"Executando Scrcpy: {' '.join(final_cmd)}"))
                self.lbl_status.configure(text=f"🖥️ ABRINDO TELA: {mode_name}", text_color="#22c55e")

                # Abre o scrcpy capturando stderr
                proc = subprocess.Popen(final_cmd, startupinfo=si, cwd=self.bin_dir, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)

                def monitor_scrcpy_output(process, is_fallback=False):
                    stdout, stderr = process.communicate()
                    if stdout: self.after(0, lambda: self.log(f"Scrcpy OUT: {stdout}"))
                    if stderr: self.after(0, lambda: self.log(f"Scrcpy ERR: {stderr}"))

                    # Lógica de Fallback para H265
                    if process.returncode != 0 and "--video-codec=h265" in final_cmd and not is_fallback:
                        self.after(0, lambda: self.log("H265 falhou. Tentando fallback para codec padrão..."))
                        fallback_args = [arg for arg in scrcpy_args if "video-codec=h265" not in arg]
                        fallback_cmd = base_scrcpy + fallback_args
                        self.after(0, lambda: self.log(f"Executando Fallback: {' '.join(fallback_cmd)}"))
                        try:
                            proc_fb = subprocess.Popen(fallback_cmd, startupinfo=si, cwd=self.bin_dir, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
                            threading.Thread(target=monitor_scrcpy_output, args=(proc_fb, True)).start()
                        except Exception as e:
                            self.after(0, lambda: self.log(f"Erro no Fallback: {e}"))

                threading.Thread(target=monitor_scrcpy_output, args=(proc, False)).start()

            except Exception as e:
                self.lbl_status.configure(text=f"❌ ERRO SCRCPY", text_color="#ef4444")
                self.after(0, lambda: self.log(f"Exceção ao abrir Scrcpy: {e}"))
                
        threading.Thread(target=task).start()

    # --- SELETOR DOS 3 MODOS PC (CORRIGIDO PARA --video-bit-rate) ---
    def apply_pc_selector(self, choice):
        if choice == "PC Lite (Eco/Leve)":
            cmds = "wm size 600x1333; wm density 240; settings put global master_sync_enabled 0; settings put global zen_mode 1"
            # CORRIGIDO: --video-bit-rate
            args = ["--max-size=1333", "--max-fps=30", "--video-bit-rate=4M", "--no-audio"]
            self.launch_pc_mode(cmds, args, "LITE_MODE")

        elif choice == "PC Workstation (Soberano)":
            cmds = "wm size 720x1600; wm density 180; settings put global activity_manager_constants max_phantom_processes=2147483647"
            # CORRIGIDO: --video-bit-rate
            args = ["--max-size=1600", "--max-fps=90", "--video-bit-rate=16M", "--audio-codec=aac"]
            self.launch_pc_mode(cmds, args, "SOBERANO")

        elif choice == "PC Gamer (Apex/H265)":
            cmds = "settings put global power_manager_constants disable_thermal_control=true; wm size 480x1066; wm density 140; cmd power set-mode 1; am kill-all"
            # CORRIGIDO: --video-bit-rate
            args = ["--max-size=1066", "--max-fps=90", "--video-bit-rate=24M", "--audio-buffer=20", "--video-codec=h265"]
            self.launch_pc_mode(cmds, args, "GAMER_APEX")
        
        self.menu_pc.set("Selecionar...")

    # --- RESTANTE DAS FUNÇÕES (IGUAIS) ---
    def apply_perf_mode(self, choice):
        if not self.target_device: return
        cmd = ""
        if choice == "Ultimate Desempenho (Bruta)":
            cmd = "settings put global power_manager_constants disable_thermal_control=true; wm size 360x800; wm density 120; am kill-all"
        elif choice == "Gamer Ultimate (FPS Máximo)":
            cmd = "wm size 432x960; wm density 160; pm disable-user --user 0 com.samsung.android.game.gos"
        elif choice == "Balanceado":
            cmd = "wm size 576x1280; wm density 240"
        
        if cmd: self.run_adb_cmd(cmd)
        self.menu_perf.set("Selecionar...")

    def apply_bat_mode(self, choice):
        if not self.target_device: return
        cmd = ""
        if choice == "Economia Normal":
            cmd = "settings put global low_power 1; settings put global adaptive_battery_management_enabled 1"
        elif choice == "Super Economia":
            cmd = "wm size 576x1280; wm density 240; settings put global master_sync_enabled 0; svc bluetooth disable"
        elif choice == "Ultimate Economia":
            cmd = "wm size 360x800; wm density 120; settings put system screen_brightness 0; am kill-all; settings put global low_power_sticky 1"
        
        if cmd: self.run_adb_cmd(cmd)
        self.menu_bat.set("Selecionar...")

    def run_adb_cmd(self, cmds):
        def t():
            try:
                self.after(0, lambda: self.log(f"Executando ADB: {cmds}"))
                si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
                exe_adb = os.path.join(self.bin_dir, "adb.exe")
                if not os.path.exists(exe_adb): exe_adb = "adb"
                res = subprocess.run([exe_adb, "-s", self.target_device, "shell", cmds], startupinfo=si, capture_output=True, text=True, timeout=10)
                if res.returncode == 0:
                      self.after(0, lambda: self.log(f"Comando executado com sucesso."))
                      self.after(0, lambda: self.lbl_status.configure(text="MODO APLICADO!", text_color="#22c55e"))
                else:
                      self.after(0, lambda: self.log(f"Erro ADB: {res.stderr}"))
                      self.after(0, lambda: self.lbl_status.configure(text="ERRO ADB!", text_color="#ef4444"))
                self.after(2000, lambda: self.lbl_status.configure(text=f"ONLINE: {self.target_device}", text_color="#22c55e"))
            except Exception as e:
                self.after(0, lambda: self.lbl_status.configure(text="ERRO AO APLICAR!", text_color="#ef4444"))
                self.after(0, lambda: self.log(f"Exceção ao executar comando: {e}"))
        threading.Thread(target=t).start()

    def emergency_reset(self): 
        self.run_adb_cmd("wm size reset; wm density reset; settings put global power_manager_constants disable_thermal_control=false; settings put global window_animation_scale 1; settings put global zen_mode 0; settings put global low_power 0; settings put system screen_brightness 100")
    
    def show_info(self, m): messagebox.showinfo("Detalhes", DESCRICOES.get(m, "Selecione um modo.")) if m and m!="Selecionar..." else None
    
    def connect_wifi(self): threading.Thread(target=lambda: subprocess.run([os.path.join(self.bin_dir,"adb.exe"), "connect", f"{self.entry_ip.get()}:{self.entry_port.get()}"], startupinfo=subprocess.STARTUPINFO())).start()
    
    def pair_wifi(self): threading.Thread(target=lambda: subprocess.run([os.path.join(self.bin_dir,"adb.exe"), "pair", self.ep_ip.get(), self.ep_code.get()], startupinfo=subprocess.STARTUPINFO())).start()
    
    def check_files(self):
        req = ["scrcpy.exe", "adb.exe", "AdbWinApi.dll"]
        missing = [f for f in req if not os.path.exists(os.path.join(self.bin_dir, f))]
        messagebox.showerror("FALTA ARQUIVO", f"Não encontrei: {missing}") if missing else messagebox.showinfo("OK", "Todos os arquivos encontrados!")

    def update_status(self, device, connected):
        if connected:
            self.target_device = device
            self.lbl_status.configure(text=f"CONECTADO: {self.target_device}", text_color="#22c55e")
        else:
            self.target_device = ""
            self.lbl_status.configure(text="DISPOSITIVO DESCONECTADO", text_color="#ef4444")

    def start_device_monitor(self):
        def monitor_loop():
            adb_path = os.path.join(self.bin_dir, "adb.exe")
            if not os.path.exists(adb_path): adb_path = "adb"
            while True:
                try:
                    si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
                    result = subprocess.run([adb_path, "devices"], capture_output=True, text=True, startupinfo=si, timeout=5)
                    lines = [l for l in result.stdout.split('\n') if 'device' in l and 'List' not in l]
                    if lines:
                        new_device = lines[0].split()[0]
                        self.after(0, lambda d=new_device: self.update_status(d, True))
                    else:
                        self.after(0, lambda: self.update_status(None, False))
                except Exception: pass
                time.sleep(2)
        threading.Thread(target=monitor_loop, daemon=True).start()

    def validate_installation(self):
        missing = []
        if not os.path.exists(os.path.join(self.bin_dir, "adb.exe")): missing.append("adb.exe")
        if not os.path.exists(os.path.join(self.bin_dir, "scrcpy.exe")): missing.append("scrcpy.exe")
        if missing: self.show_missing_files_dialog(missing)

    def show_missing_files_dialog(self, missing_files):
        dialog = ctk.CTkToplevel(self)
        dialog.title("Arquivos Faltando")
        dialog.geometry("400x200")
        dialog.attributes("-topmost", True)
        ctk.CTkLabel(dialog, text=f"Arquivos não encontrados:\n{', '.join(missing_files)}", text_color="red", font=("Arial", 14, "bold")).pack(pady=20)
        frame_btns = ctk.CTkFrame(dialog, fg_color="transparent")
        frame_btns.pack(fill="x", padx=20, pady=20)
        ctk.CTkButton(frame_btns, text="Download Automático", command=self.open_download_page).pack(side="left", expand=True, padx=5)
        ctk.CTkButton(frame_btns, text="Abrir Pasta", command=self.open_bin_folder).pack(side="right", expand=True, padx=5)

    def open_download_page(self): webbrowser.open("https://github.com/Genymobile/scrcpy/releases")
    def open_bin_folder(self):
        try: os.startfile(self.bin_dir)
        except: subprocess.Popen(["explorer", self.bin_dir])

if __name__ == "__main__":
    app = TurboCoreApp()
    app.mainloop()
