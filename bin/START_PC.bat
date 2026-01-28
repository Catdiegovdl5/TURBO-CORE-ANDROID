@echo off
title PROJETO TURBO CORE - PC REI
cls

echo [1] LIMPANDO CONFIGURACOES...
adb shell wm size reset
adb shell wm density reset

echo [2] APLICANDO MODO PANORAMICO (16:9)...
:: Forca o celular a pensar que tem 720x1280 (proporcao de PC)
adb shell wm size 720x1280
adb shell wm density 160
:: Forca o celular a deitar (1 = Paisagem)
adb shell settings put system user_rotation 1

echo [3] INICIANDO SCRCPY EM TELA CHEIA...
:: Sem comandos invalidos, apenas o que funciona no 3.3.4
scrcpy --window-width=1366 --window-height=768 --window-borderless --fullscreen --always-on-top --video-bit-rate=4M --max-fps=30

echo.
echo [4] SCRCPY FECHADO. RESETANDO TUDO...
:: O script so chega aqui depois que voce fecha a janela do Scrcpy
adb shell wm size reset
adb shell wm density reset
adb shell settings put system user_rotation 0
echo Celular normalizado.
pause