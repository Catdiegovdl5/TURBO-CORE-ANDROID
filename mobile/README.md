# Turbo Core Mobile (Android Port)

Versão mobile do Turbo Core V111, portada para Kivy/Python-for-Android.

## 📱 Funcionalidades
- **Modo Capa (Sensi):** Ajusta DPI e sensibilidade para Free Fire.
- **Modo Liso (Performance):** Reduz resolução e desativa animações.
- **Limpar Otimizações:** Restaura o dispositivo para o padrão de fábrica (safe).

## 🛠️ Como Compilar (Buildozer)

Este projeto usa **Buildozer** para gerar o APK.

1. Instale as dependências no Linux (Ubuntu/WSL):
   ```bash
   sudo apt update
   sudo apt install -y git zip unzip openjdk-17-jdk python3-pip autoconf libtool pkg-config zlib1g-dev libncurses5-dev libncursesw5-dev libtinfo5 cmake libffi-dev libssl-dev
   pip3 install --user --upgrade buildozer cython
   ```

2. Inicialize o buildozer (se ainda não tiver o `buildozer.spec`):
   ```bash
   cd mobile
   buildozer init
   ```
   *Nota: Configure o `buildozer.spec` para incluir as permissões necessárias.*

3. Compile o APK:
   ```bash
   buildozer android debug
   ```

## ⚠️ Permissões Importantes

Como este aplicativo roda comandos de sistema (`wm`, `settings`, `cmd power`), ele precisa de permissões especiais que o Android não dá por padrão para apps normais.

Para que os botões funcionem, você deve conceder permissões via ADB (pelo PC) após instalar o APK, ou rodar o app em um dispositivo com Root (integrado futuramente).

**Permissões via ADB PC:**
```bash
adb shell pm grant org.test.turbocoremobile android.permission.WRITE_SECURE_SETTINGS
adb shell pm grant org.test.turbocoremobile android.permission.DUMP
```
*(Substitua `org.test.turbocoremobile` pelo package name definido no seu buildozer.spec)*

## 🎨 Design
Interface Cyberpunk (Preto, Vermelho, Azul) com execução assíncrona para não travar a UI.
