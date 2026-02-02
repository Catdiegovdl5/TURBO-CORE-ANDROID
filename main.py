from kivy.lang import Builder
from kivy.clock import Clock
from kivy.properties import StringProperty, ListProperty, NumericProperty, ColorProperty
from kivymd.app import MDApp
from kivymd.uix.tab import MDTabsBase
from kivymd.uix.floatlayout import MDFloatLayout
from kivymd.uix.list import ThreeLineAvatarIconListItem, IconLeftWidget, MDList
from kivymd.uix.dialog import MDDialog
from kivymd.uix.button import MDFlatButton
from modes_db import modes
import subprocess
import os

# --- KV UI STRUCTURE ---
KV = '''
MDBoxLayout:
    orientation: "vertical"

    MDTopAppBar:
        id: toolbar
        title: "Turbo Core V206"
        elevation: 4
        md_bg_color: app.brand_primary_color
        specific_text_color: 1, 1, 1, 1

    MDTabs:
        id: tabs
        background_color: app.brand_secondary_color
        indicator_color: 1, 1, 1, 1

<Tab>:
    ScrollView:
        MDList:
            id: list_container

<ModeItem>:
    text: root.title
    secondary_text: root.risk_level
    tertiary_text: root.brand_logic
    theme_text_color: "Custom"
    text_color: 1, 1, 1, 1
    on_release: app.show_mode_details(root.mode_id)
    IconLeftWidget:
        icon: root.item_icon
        theme_text_color: "Custom"
        text_color: app.brand_primary_color
'''

class Tab(MDFloatLayout, MDTabsBase):
    pass

class ModeItem(ThreeLineAvatarIconListItem):
    title = StringProperty()
    risk_level = StringProperty()
    brand_logic = StringProperty()
    item_icon = StringProperty("flash")
    mode_id = StringProperty()

class TurboCoreApp(MDApp):
    temp = NumericProperty(35.0)
    brand_primary_color = ColorProperty([0, 1, 1, 1]) # Default Cyan
    brand_secondary_color = ColorProperty([0.1, 0.1, 0.1, 1])

    def build(self):
        self.theme_cls.theme_style = "Dark"
        self.detect_hardware()
        return Builder.load_string(KV)

    def detect_hardware(self):
        # Em produção: brand = Build.MANUFACTURER.upper()
        # Aqui simularemos via getprop ou default
        self.manufacturer = "SAMSUNG" # Simulação

        if "SAMSUNG" in self.manufacturer:
            self.brand_primary_color = [33/255, 150/255, 243/255, 1] # Blue
            self.app_title = "TURBO CORE - SAMSUNG EDITION"
        elif "XIAOMI" in self.manufacturer or "POCO" in self.manufacturer:
            self.brand_primary_color = [255/255, 87/255, 34/255, 1] # Orange
            self.app_title = "TURBO CORE - XIAOMI/POCO"
        else:
            self.brand_primary_color = [0, 1, 1, 1] # Cyan
            self.app_title = "TURBO CORE UNIVERSAL"

    def on_start(self):
        self.root.ids.toolbar.title = self.app_title
        self.create_tabs()
        Clock.schedule_interval(self.thermal_guardian, 30)

    def create_tabs(self):
        categories = [
            "CPU & THREADS", "GPU & VISUAL", "SENSITIVITY & INPUT",
            "REDE & CONECTIVIDADE", "DEBLOAT CIRÚRGICO", "PROTOCOLOS HÍBRIDOS"
        ]
        for cat in categories:
            tab = Tab(title=cat)
            self.root.ids.tabs.add_widget(tab)
            self.populate_tab(tab, cat)

    def populate_tab(self, tab, category):
        for m_id, m_data in modes.items():
            # Filtragem básica: Universal + Marca detectada
            if m_data["category"] == category:
                if m_data["brand_logic"] in ["Universal", self.manufacturer.capitalize()]:
                    item = ModeItem(
                        title=m_data["title"],
                        risk_level=f"Risco: {m_data['risk']}",
                        brand_logic=f"Lógica: {m_data['brand_logic']}",
                        mode_id=m_id,
                        item_icon=self.get_icon_by_cat(category)
                    )
                    tab.ids.list_container.add_widget(item)

    def get_icon_by_cat(self, cat):
        if "CPU" in cat: return "cpu-64-bit"
        if "GPU" in cat: return "card-bulleted-settings"
        if "SENSITIVITY" in cat: return "gesture-tap"
        if "REDE" in cat: return "wifi"
        if "DEBLOAT" in cat: return "auto-fix"
        return "fountain-pen-tip"

    def show_mode_details(self, mode_id):
        m = modes[mode_id]
        self.dialog = MDDialog(
            title=m["title"],
            text=f"[b]Explicação Técnica:[/b]\n{m['desc_tech']}\n\n[b]Comando:[/b]\n[i]{m['command']}[/i]\n\n[b]Risco:[/b] {m['risk']}",
            buttons=[
                MDFlatButton(text="FECHAR", on_release=lambda x: self.dialog.dismiss()),
                MDFlatButton(text="APLICAR", theme_text_color="Custom", text_color=self.brand_primary_color, on_release=lambda x: self.execute_mode(mode_id))
            ],
        )
        self.dialog.open()

    def thermal_guardian(self, *args):
        # Simulação
        if self.temp > 39.0:
            return True
        return False

    def execute_mode(self, mode_id):
        m = modes[mode_id]
        if m["category"] in ["CPU & THREADS", "GPU & VISUAL"] and self.thermal_guardian():
            self.dialog.dismiss()
            MDDialog(title="BLOQUEIO TÉRMICO", text="Dispositivo acima de 39°C. Resfriamento necessário.").open()
            return

        try:
            # Tenta executar via shell (requer root ou adb shell se rodando no PC)
            # Em Android real com Kivy, pode precisar de bridge para Shizuku
            process = subprocess.Popen(
                m['command'], shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE
            )
            stdout, stderr = process.communicate()
            result = stdout.decode().strip() or "Sucesso"
            if stderr:
                result = f"Erro: {stderr.decode().strip()}"
        except Exception as e:
            result = f"Erro: {str(e)}"

        self.dialog.dismiss()
        MDDialog(title="RESULTADO", text=result).open()

if __name__ == "__main__":
    TurboCoreApp().run()
