@echo off
echo --- INSTALLING DEPENDENCIES ---
pip install -r requirements.txt

echo.
echo --- BUILDING TURBO CORE V107 ---
echo This may take a few minutes...

pyinstaller --noconfirm --onedir --windowed --icon "icon.png" --name "TurboCore_V107_FF" --add-data "fundo_chip.jpg;." --add-data "bin;bin" --collect-all customtkinter main.py

echo.
echo --- BUILD COMPLETE ---
echo The executable is located in the 'dist/TurboCore_V107_FF' folder.
pause
