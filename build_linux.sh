#!/bin/bash
echo "🚀 Iniciando Build do Turbo Core V111 Linux..."

# 1. Limpeza de builds antigos
rm -rf build dist TURBO_CORE_V111_LINUX

# 2. Compilação
python3 -m PyInstaller --noconfirm --onefile --windowed \
--add-data "fundo_chip.jpg:." \
--add-data "icon.png:." \
--hidden-import "PIL._tkinter_finder" \
--name "TURBO_CORE_V111_LINUX" \
"TURBO CORE.py"

# 3. Organização
mv dist/TURBO_CORE_V111_LINUX .
rm -rf build dist *.spec
chmod +x TURBO_CORE_V111_LINUX

echo "✅ Build concluído com sucesso! O arquivo está na pasta do projeto."
