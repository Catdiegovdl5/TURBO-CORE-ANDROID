# Relatório de Inteligência: Samsung Galaxy A07 (Cortex-A55)

## 1. Gargalos de Hardware
A linha A0x (A03, A04, A07) utiliza processadores de entrada como o **MediaTek Helio P35** ou **Exynos 850**.
- **Litografia Antiga**: Construídos em 12nm ou 14nm, esses chips dissipam calor de forma ineficiente.
- **Arquitetura Cortex-A55**: Focada em eficiência, não em performance bruta. Loops de processamento saturam os núcleos rapidamente.

## 2. Conflitos de Software (One UI Core)
Mesmo na versão "Core", a Samsung mantém serviços pesados em segundo plano:
- **Samsung Knox**: A verificação constante de integridade consome ciclos de CPU preciosos em chips fracos.
- **GOS (Game Optimizing Service)**: O GOS entra em um loop de monitoramento térmico. Ele usa CPU para checar a temperatura; o uso da CPU esquenta o chip; o calor faz o GOS trabalhar mais. É um **Feedback Loop Positivo** desastroso.

## 3. O Problema do dex2oat
Após atualizações ou instalações, o Android tenta otimizar os apps via `dex2oat`. Em dispositivos com pouca RAM (4GB ou menos), esse processo consome 100% da CPU e drena a bateria mais rápido do que o USB consegue carregar.

## 4. Solução: Perfil "Samsung Lite"
O Turbo Core agora implementa o perfil Lite, que:
- Cancela compilações automáticas de fundo.
- Congela o GOS e serviços Knox não essenciais.
- Desativa o logger do sistema (`logd`).
