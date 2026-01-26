import customtkinter as ctk
from tkinter import messagebox
import subprocess
import threading
import os
import sys
import time
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
        self.title("TURBO CORE V48 - ELITE TRINITY")
        self.geometry("440x820")
        self.resizable(False, False)
        self.configure(fg_color=COR_FUNDO)
        self.target_device = ""
        self.monitor_thread_started = False

        # GESTÃO DE CAMINHOS
        if getattr(sys, 'frozen', False):
            self.app_dir = os.path.dirname(sys.executable)
        else:
            self.app_dir = os.path.dirname(os.path.abspath(__file__))

        # IMAGEM DE FUNDO
        try:
            img_path = os.path.join(self.app_dir, "fundo_chip.jpg")
            self.img_bg = ctk.CTkImage(Image.open(img_path), size=(440, 820))
        except: self.img_bg = None

        # ABAS
        self.tabview = ctk.CTkTabview(self, fg_color="#0a0a0a", segmented_button_selected_color=COR_PRIMARIA, height=770)
        self.tabview.pack(fill="both", expand=True, padx=5, pady=5)
        
        self.tab_dash = self.tabview.add("CENTRAL")
        self.tab_conn = self.tabview.add("CONEXÃO")
        self.tab_pair = self.tabview.add("PAREAR")

        if self.img_bg:
            ctk.CTkLabel(self.tab_dash, text="", image=self.img_bg).place(x=0, y=0, relwidth=1, relheight=1)

        self.setup_dashboard()
        self.setup_connect_tab()
        self.setup_pair_tab()
        
        self.lbl_status = ctk.CTkLabel(self, text="SISTEMA OFFLINE", font=("Consolas", 12, "bold"), text_color="#555555", fg_color="#000000")
        self.lbl_status.pack(fill="x", side="bottom", ipady=5)

        self.start_device_monitoring()

    def create_menu(self, parent, label_text, values, cmd_func, color, bg_color):
        ctk.CTkLabel(parent, text=label_text, font=("Arial", 11, "bold"), text_color=color).pack(anchor="w", pady=(10,0))
        f = ctk.CTkFrame(parent, fg_color="transparent")
        f.pack(fill="x", pady=2)
        m = ctk.CTkOptionMenu(f, values=values, fg_color=bg_color, button_color=color, width=300, command=cmd_func)
        m.pack(side="left", padx=(0,5))
        ctk.CTkButton(f, text="?", width=30, fg_color="#333", command=lambda: self.show_info(m.get())).pack(side="left")
        return m

    def setup_dashboard(self):
        c = ctk.CTkFrame(self.tab_dash, fg_color="transparent")
        c.pack(fill="both", expand=True, padx=15, pady=15)

        # 1. PERFORMANCE
        self.menu_perf = self.create_menu(c, "🔥 1. MODO DESEMPENHO", 
            ["Selecionar...", "Gamer Ultimate (FPS Máximo)", "Ultimate Desempenho (Bruta)", "Balanceado"], 
            self.apply_perf_mode, COR_PRIMARIA, "#330000")

        # 2. BATERIA
        self.menu_bat = self.create_menu(c, "🔋 2. MODO BATERIA", 
            ["Selecionar...", "Economia Normal", "Super Economia", "Ultimate Economia"], 
            self.apply_bat_mode, "#15803d", "#052e16")

        # 3. MODO PC (SEU CÓDIGO NOVO)
        self.menu_pc = self.create_menu(c, "🖥️ 3. MODO PC / ESPELHAMENTO", 
            ["Selecionar...", "PC Lite (Eco/Leve)", "PC Workstation (Soberano)", "PC Gamer (Apex/H265)"], 
            self.apply_pc_selector, "#0284c7", "#0c4a6e")

        # AÇÕES GLOBAIS
        ctk.CTkLabel(c, text="⚠️ AÇÕES DE SISTEMA", font=("Arial", 11, "bold"), text_color="white").pack(anchor="w", pady=(30,0))
        ctk.CTkButton(c, text="TESTAR ARQUIVOS DA PASTA 🛠️", fg_color="#444", command=self.check_files).pack(fill="x", pady=5)
        ctk.CTkButton(c, text="🚨 RESETAR TUDO (PADRÃO)", fg_color="#b91c1c", height=40, font=("Arial", 12, "bold"), command=self.emergency_reset).pack(fill="x", pady=(10,0))

    def setup_connect_tab(self):
        f = ctk.CTkFrame(self.tab_conn, fg_color="transparent"); f.pack(fill="both", expand=True, padx=30, pady=30)
        ctk.CTkLabel(f, text="LINK WIRELESS", font=("Arial", 20, "bold")).pack(pady=20)
        self.entry_ip = ctk.CTkEntry(f, placeholder_text="IP (ex: 192.168.0.5)", height=45)
        self.entry_ip.pack(fill="x", pady=10)
        self.entry_port = ctk.CTkEntry(f, placeholder_text="PORTA (ex: 5555)", height=45)
        self.entry_port.pack(fill="x", pady=10)
        ctk.CTkButton(f, text="CONECTAR 🔗", fg_color=COR_PRIMARIA, height=50, command=self.connect_wifi).pack(fill="x", pady=30)

    def setup_pair_tab(self):
        f = ctk.CTkFrame(self.tab_pair, fg_color="transparent"); f.pack(fill="both", expand=True, padx=20, pady=20)
        ctk.CTkLabel(f, text="PAREAMENTO ANDROID 11+", font=("Arial", 16, "bold"), text_color="#fbbf24").pack(pady=20)
        self.ep_ip = ctk.CTkEntry(f, placeholder_text="IP:Porta Pareamento"); self.ep_ip.pack(fill="x", pady=5)
        self.ep_code = ctk.CTkEntry(f, placeholder_text="Código 6 dígitos"); self.ep_code.pack(fill="x", pady=5)
        ctk.CTkButton(f, text="PAREAR 🔑", fg_color="#fbbf24", text_color="black", command=self.pair_wifi).pack(fill="x", pady=20)

    # --- HELPER: LANÇADOR MESTRE DE MODO PC (SEU CÓDIGO INTEGRADO) ---
    def launch_pc_mode(self, adb_cmds, scrcpy_args, mode_name):
        if not self.target_device: 
            messagebox.showwarning("AVISO", "Conecte o dispositivo primeiro!")
            return
        
        exe_scrcpy = os.path.join(self.app_dir, "scrcpy.exe")
        exe_adb = os.path.join(self.app_dir, "adb.exe")
        if not os.path.exists(exe_adb): exe_adb = "adb"

        if not os.path.exists(exe_scrcpy):
            messagebox.showerror("ERRO", "scrcpy.exe não encontrado!")
            return

        def task():
            self.lbl_status.configure(text=f"⚙️ PREPARANDO: {mode_name}...", text_color="#0284c7")
            
            # 1. Aplica as configurações no Android via ADB
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            for cmd in adb_cmds.split(';'):
                if cmd.strip():
                    subprocess.run([exe_adb, "-s", self.target_device, "shell", cmd.strip()], startupinfo=si)
            
            # Pequena pausa para o Android processar a mudança de resolução
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
                self.lbl_status.configure(text=f"🖥️ ABRINDO TELA: {mode_name}", text_color="#22c55e")
                # Abre o scrcpy sem bloquear o app
                subprocess.Popen(final_cmd, startupinfo=si, cwd=self.app_dir)
            except Exception as e:
                self.lbl_status.configure(text=f"❌ ERRO SCRCPY", text_color="#ef4444")
                
        threading.Thread(target=task).start()

    # --- SELETOR DOS 3 MODOS PC ---
    def apply_pc_selector(self, choice):
        if choice == "PC Lite (Eco/Leve)":
            # NÍVEL 1: PC LITE
            cmds = "wm size 600x1333; wm density 240; settings put global master_sync_enabled 0; settings put global low_power 1; am kill-all"
            args = ["--max-size=1333", "--max-fps=30", "--bit-rate=4M", "--no-audio"]
            self.launch_pc_mode(cmds, args, "LITE_MODE")

        elif choice == "PC Workstation (Soberano)":
            # NÍVEL 2: WORKSTATION / SOBERANO
            cmds = "wm size 720x1600; wm density 180; settings put global activity_manager_constants max_phantom_processes=2147483647; settings put global window_animation_scale 0.5"
            args = ["--max-size=1600", "--max-fps=90", "--bit-rate=16M", "--audio-codec=aac"]
            self.launch_pc_mode(cmds, args, "SOBERANO")

        elif choice == "PC Gamer (Apex/H265)":
            # NÍVEL 3: GAMER APEX
            cmds = "settings put global power_manager_constants disable_thermal_control=true; wm size 480x1066; wm density 140; cmd power set-mode 1; am kill-all"
            # H265 exige PC compatível, mas é superior.
            args = ["--max-size=1066", "--max-fps=90", "--bit-rate=24M", "--audio-buffer=20", "--video-codec=h265"]
            self.launch_pc_mode(cmds, args, "GAMER_APEX")
        
        self.menu_pc.set("Selecionar...")

    # --- MODOS DE PERFORMANCE (ANTIGOS) ---
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

    # --- MODOS DE BATERIA (ANTIGOS) ---
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

    # --- UTILITÁRIOS ---
    def run_adb_cmd(self, cmds):
        def t():
            si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            exe_adb = os.path.join(self.app_dir, "adb.exe")
            if not os.path.exists(exe_adb): exe_adb = "adb"
            subprocess.run([exe_adb, "-s", self.target_device, "shell", cmds], startupinfo=si)
            self.lbl_status.configure(text="MODO APLICADO!", text_color="#22c55e")
            self.after(2000, lambda: self.lbl_status.configure(text=f"ONLINE: {self.target_device}", text_color="#22c55e"))
        threading.Thread(target=t).start()

    def emergency_reset(self): 
        self.run_adb_cmd("wm size reset; wm density reset; settings put global power_manager_constants disable_thermal_control=false; settings put global window_animation_scale 1; settings put global zen_mode 0; settings put global low_power 0; settings put system screen_brightness 100")
    
    def show_info(self, m): messagebox.showinfo("Detalhes", DESCRICOES.get(m, "Selecione um modo.")) if m and m!="Selecionar..." else None
    
    def connect_wifi(self): threading.Thread(target=lambda: subprocess.run([os.path.join(self.app_dir,"adb.exe"), "connect", f"{self.entry_ip.get()}:{self.entry_port.get()}"], startupinfo=subprocess.STARTUPINFO())).start()
    
    def pair_wifi(self): threading.Thread(target=lambda: subprocess.run([os.path.join(self.app_dir,"adb.exe"), "pair", self.ep_ip.get(), self.ep_code.get()], startupinfo=subprocess.STARTUPINFO())).start()
    
    def check_files(self):
        req = ["scrcpy.exe", "adb.exe", "AdbWinApi.dll"]
        missing = [f for f in req if not os.path.exists(os.path.join(self.app_dir, f))]
        messagebox.showerror("FALTA ARQUIVO", f"Não encontrei: {missing}") if missing else messagebox.showinfo("OK", "Todos os arquivos encontrados!")

def _update_device_status(self, connected, device_name):
        if connected:
            self.target_device = device_name
            self.lbl_status.configure(text=f"CONECTADO: {self.target_device}", text_color="#22c55e")
        else:
            self.target_device = ""
            self.lbl_status.configure(text="DISPOSITIVO DESCONECTADO", text_color="#ef4444")

    def _monitor_devices_loop(self):
        # Garante uso do caminho correto
        adb = os.path.join(self.bin_dir, "adb.exe")
        if not os.path.exists(adb): adb = "adb"
        
        si = subprocess.STARTUPINFO(); si.dwFlags |= subprocess.STARTF_USESHOWWINDOW

        while True:
            try:
                res = subprocess.run([adb, "devices"], capture_output=True, text=True, startupinfo=si)
                lines = [l for l in res.stdout.split('\n') if 'device' in l and 'List' not in l]

                if lines:
                    device_name = lines[0].split()[0]
                    # self.after garante que a UI não trave
                    self.after(0, lambda d=device_name: self._update_device_status(True, d))
                else:
                    self.after(0, lambda: self._update_device_status(False, ""))
            except Exception:
                pass
            time.sleep(2)

    def detect_device(self):
        # Inicia a thread de monitoramento apenas uma vez
        if getattr(self, 'monitor_thread_started', False): return
        self.monitor_thread_started = True
        threading.Thread(target=self._monitor_devices_loop, daemon=True).start()

if __name__ == "__main__":
    app = TurboCoreApp()
    app.mainloop()
