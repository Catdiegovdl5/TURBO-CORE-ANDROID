import threading
import subprocess
import os
import sys

from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
from kivy.uix.textinput import TextInput
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.utils import get_color_from_hex
from kivy.graphics import Color, Line, Rectangle

# --- CONFIGURAÇÃO CYBERPUNK ---
COLOR_BG = "#0A0A0A"
COLOR_RED = "#FF003C"
COLOR_BLUE = "#00E5FF"
COLOR_TEXT = "#FAFAFA"

# Comandos extraídos da versão Desktop
CMD_MODO_CAPA = (
    "wm density 90 && "
    "settings put system pointer_speed 7 && "
    "settings put secure long_press_timeout 100"
)

CMD_MODO_LISO = (
    "wm size 540x960 && "
    "wm density 160 && "
    "cmd power set-mode 1 && "
    "settings put global window_animation_scale 0 && "
    "settings put global transition_animation_scale 0 && "
    "am kill-all"
)

CMD_LIMPAR = (
    "wm size reset && "
    "wm density reset && "
    "settings put system user_rotation 0 && "
    "settings put system accelerometer_rotation 1 && "
    "settings put global low_power 0 && "
    "settings put system screen_brightness 100 && "
    "settings put global window_animation_scale 1 && "
    "settings put global transition_animation_scale 1 && "
    "settings put global animator_duration_scale 1 && "
    "settings put system pointer_speed 1"
)

class AsyncShell:
    """
    Ponte de execução para rodar comandos shell de forma assíncrona.
    Prepara o terreno para futura integração com Shizuku ou Root.
    """
    @staticmethod
    def run(cmd, callback):
        def _target():
            try:
                # Executa no shell local do Android
                # Nota: Algumas permissões (como WRITE_SECURE_SETTINGS) devem ser concedidas via ADB PC antes.
                res = subprocess.run(cmd, shell=True, capture_output=True, text=True)
                output = f"$ {cmd}\nSTDOUT: {res.stdout}\nSTDERR: {res.stderr}\nCode: {res.returncode}"
            except Exception as e:
                output = f"$ {cmd}\nERROR: {str(e)}"

            Clock.schedule_once(lambda dt: callback(output), 0)

        threading.Thread(target=_target).start()

class CyberButton(Button):
    def __init__(self, **kwargs):
        self.accent_color = kwargs.pop('accent_color', COLOR_BLUE)
        super().__init__(**kwargs)
        self.background_color = (0, 0, 0, 0)  # Transparente
        self.color = get_color_from_hex(COLOR_TEXT)
        self.font_size = '18sp'
        self.bold = True
        self.bind(pos=self.update_canvas, size=self.update_canvas, state=self.update_canvas)

    def update_canvas(self, *args):
        self.canvas.before.clear()
        with self.canvas.before:
            # Fundo
            if self.state == 'down':
                Color(*get_color_from_hex(self.accent_color)[:3], 0.3)
            else:
                Color(0.1, 0.1, 0.1, 1)
            Rectangle(pos=self.pos, size=self.size)

            # Borda Neon
            Color(*get_color_from_hex(self.accent_color))
            Line(rectangle=(self.x, self.y, self.width, self.height), width=1.5)

class TurboCoreMobile(App):
    def build(self):
        Window.clearcolor = get_color_from_hex(COLOR_BG)

        # Layout Principal
        root = BoxLayout(orientation='vertical', padding=20, spacing=20)

        # Header
        header = Label(
            text="[b]TURBO CORE[/b] MOBILE",
            markup=True,
            font_size='28sp',
            color=get_color_from_hex(COLOR_BLUE),
            size_hint_y=None,
            height=60
        )
        root.add_widget(header)

        # Botões Principais
        btn_layout = BoxLayout(orientation='vertical', spacing=15, size_hint_y=None, height=240)

        btn_capa = CyberButton(text="🔥 MODO CAPA (Sensi)", accent_color=COLOR_RED)
        btn_capa.bind(on_release=lambda x: self.exec_cmd(CMD_MODO_CAPA))

        btn_liso = CyberButton(text="❄️ MODO LISO (FPS)", accent_color=COLOR_BLUE)
        btn_liso.bind(on_release=lambda x: self.exec_cmd(CMD_MODO_LISO))

        btn_reset = CyberButton(text="⚡ LIMPAR OTIMIZAÇÕES", accent_color="#FFFFFF")
        btn_reset.bind(on_release=lambda x: self.exec_cmd(CMD_LIMPAR))

        btn_layout.add_widget(btn_capa)
        btn_layout.add_widget(btn_liso)
        btn_layout.add_widget(btn_reset)

        root.add_widget(btn_layout)

        # Log de Saída (Terminal View)
        self.log_output = TextInput(
            text="> Sistema Pronto. Aguardando comandos...\n> Nota: Certifique-se de ter permissões via 'pm grant' ou Root.",
            readonly=True,
            foreground_color=get_color_from_hex("#00FF00"),
            background_color=get_color_from_hex("#000000"),
            font_name="RobotoMono-Regular" if "RobotoMono-Regular" in Label().font_name else "DroidSans", # Fallback font
            font_size='12sp',
            size_hint_y=1
        )
        root.add_widget(self.log_output)

        return root

    def exec_cmd(self, cmd):
        self.log_output.text += f"\n> Executando..."
        AsyncShell.run(cmd, self.update_log)

    def update_log(self, result):
        self.log_output.text += f"\n{result}\n"
        # Auto-scroll (simple approximation via cursor setting)
        self.log_output.cursor = (0, len(self.log_output.text))

if __name__ == '__main__':
    TurboCoreMobile().run()
