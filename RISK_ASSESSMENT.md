# Avaliação de Risco (Risk Assessment) - V800 Rank Ξ

Este documento detalha os pontos frágeis identificados na arquitetura V800 e as ferramentas recomendadas para mitigação.

## 1. Riscos Identificados

### 1.1 Conexão Shizuku & Shell
*   **Risco:** Se o usuário nunca conceder permissão, o app ficará preso no status "Permissão Necessária" ou "Conectando...". O método `runCommand` falha se o binder morrer silenciosamente.
*   **Risco Crítico:** O timeout de 3000ms (`ShellEngine.kt`) é seguro para leitura de stats, mas pode ser **insuficiente** para comandos pesados como `cmd package compile -m speed`, que podem levar 10-30s. Isso causaria um `TimeoutCancellationException`, falhando a otimização silenciosamente.
*   **Shizuku Manager:** O loop de retry roda em uma `Thread` crua. Se a Activity for destruída, a thread pode continuar rodando (leak de contexto se não tratado, embora `statusState` seja Compose State).

### 1.2 Persistência e Segurança
*   **EncryptedSharedPreferences:** Se a Keystore do Android for resetada (ex: usuário mudou senha de bloqueio), a chave mestra pode ser invalidada, causando crash ao tentar decriptar as preferências na inicialização (`PreferencesManager.kt`). O bloco `try-catch` atual faz fallback para prefs inseguras, mas os dados antigos seriam perdidos.

### 1.3 Detecção de Apps (AppDetector)
*   **Permissão:** Se o usuário não conceder `PACKAGE_USAGE_STATS` (Acesso a dados de uso) nas configurações do Android, `queryUsageStats` retorna uma lista vazia. O app não tem um fluxo claro na UI para forçar o usuário a ir nessa tela de configurações específica.
*   **Confiabilidade:** Em dispositivos chineses (Xiaomi/Huawei), o sistema mata serviços de background agressivamente, o que pode impedir o `DashboardViewModel` de atualizar as métricas se o app for minimizado.

### 1.4 UI/UX
*   **Responsividade:** O layout do Dashboard é fixo com `Modifier.height(140.dp)` no Grid. Em telas muito pequenas (ou modo janela dividida), o conteúdo pode cortar.
*   **Acessibilidade:** Os botões e cards não possuem `contentDescription` explícitos para TalkBack.

## 2. Ferramentas Recomendadas

Para elevar o nível profissional do app, recomendo integrar:

### 2.1 Monitoramento de Erros
*   **Firebase Crashlytics:** Para capturar exceções não tratadas (ex: falhas de Keystore, SecurityExceptions do Shizuku).
*   **Timber:** Para logs estruturados em vez de usar callbacks manuais de log.

### 2.2 Detecção de Vazamentos
*   **LeakCanary:** Essencial para verificar se o `Shizuku.OnBinderReceivedListener` está realmente sendo limpo e se a `Thread` do `ShizukuManager` não está vazando a Activity.

### 2.3 Qualidade de Código (Linting)
*   **StrictMode:** Ativar em builds de DEBUG para detectar acesso a disco/rede na Main Thread (embora estejamos usando `Dispatchers.IO`, é bom garantir).
*   **Detekt:** Para análise estática de código Kotlin.

## 3. Ações de Mitigação Imediata

1.  **Aumentar Timeout para Ações Pesadas:** Criar uma variante de `runCommand` com timeout maior para JIT compilation.
2.  **Verificar Permissão de Uso:** Adicionar verificação explícita se `PACKAGE_USAGE_STATS` foi concedido e mostrar um Dialog se não.
3.  **StrictMode:** Configurar no `MainActivity` para validar a thread safety.
