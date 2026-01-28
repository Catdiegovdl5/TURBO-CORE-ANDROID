@echo off
echo --- INSTALANDO DEPENDENCIAS ---
python -m pip install -r requirements.txt

echo.
echo --- CONSTRUINDO TURBO CORE V108 (STANDALONE MASTER) ---
echo Isso pode levar alguns minutos. Nao feche a janela...

:: O segredo esta no "python -m" para evitar erros de comando nao encontrado
:: E no "--clean" para garantir que nao haja restos de versoes com erro
python -m PyInstaller --noconfirm --onefile --windowed --clean --icon "icon.ico" --name "TurboCore_V108_Final" --add-data "fundo_chip.jpg;." --add-data "icon.ico;." --add-data "bin;bin" --collect-all customtkinter main.py

echo.
echo --- COMPILACAO CONCLUIDA! ---
echo O executavel unico (standalone) esta na pasta 'dist'.
echo Ele ja contem o ADB e as imagens dentro dele.
pause