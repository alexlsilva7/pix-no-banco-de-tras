<div align="center">
  <!-- Você pode adicionar um logo ou banner do app aqui no futuro -->
  <h1>🚗 Pix no Banco de Trás</h1>
  <p><strong>Compartilhamento remoto de QR Codes (Pix e Wi-Fi) para motoristas de aplicativo.</strong></p>
</div>

## 📖 Sobre o Projeto

O **Pix no Banco de Trás** é um aplicativo Android desenvolvido especialmente para motoristas de aplicativo (Uber, 99, inDrive, etc.) que utilizam um tablet ou smartphone extra fixado no encosto do banco para os passageiros.

Com ele, o motorista pode compartilhar a sua chave Pix em formato de QR Code, a rede Wi-Fi do carro, ou até mesmo capturar e extrair um QR Code de cobrança diretamente da tela do seu celular principal (Modo Motorista) e enviá-lo instantaneamente para a tela do passageiro (Modo Passageiro) via rede local, tudo através de uma bolha flutuante discreta.

## ✨ Principais Funcionalidades

*   🚘 **Dois Modos de Operação:** Funciona tanto como **Motorista** (Servidor) quanto como **Passageiro** (Cliente/Display).
*   🔍 **Extração Inteligente de QR Code:** Usa o Serviço de Acessibilidade do Android para ler a tela anonimamente, extrair apenas o código Pix/QR Code (via ZXing) e enviar para o tablet de trás, mantendo a privacidade do resto da tela.
*   🫧 **Bolha Flutuante (Overlay):** Um menu flutuante e discreto que fica sobre outros apps (como o Waze/Google Maps), permitindo ao motorista enviar o Pix, compartilhar o Wi-Fi ou apagar a tela com apenas um toque.
*   📡 **Descoberta Automática de Rede:** O tablet do passageiro localiza o celular do motorista automaticamente na rede Wi-Fi do carro (via UDP Broadcast), sem precisar digitar IPs manualmente.
*   🔒 **Controle Remoto de Tela:** O motorista pode enviar um comando para apagar ou até mesmo **bloquear** a tela do tablet do passageiro assim que o pagamento for concluído.
*   ☀️ **Controle de Brilho e Despertar:** O tablet do passageiro acende a tela automaticamente e vai para o brilho máximo quando um novo Pix chega, desligando logo em seguida após um tempo de segurança (timeout configurável).

## 🛠️ Tecnologias Utilizadas

*   **Linguagem:** Kotlin
*   **Interface:** Jetpack Compose (Material Design 3)
*   **Rede:** TCP Sockets (transferência de dados) e UDP Sockets (auto-discovery)
*   **Processamento de Imagem:** ZXing (Zebra Crossing) para decodificação e geração de QR Codes on-the-fly.
*   **Serviços do Android:**
    *   `AccessibilityService` (captura silenciosa da tela no Android 11+).
    *   `SYSTEM_ALERT_WINDOW` (bolha flutuante sobreposta).
    *   `DevicePolicyManager` / `DeviceAdminReceiver` (bloqueio de tela remoto).

## 📱 Capturas de Tela
Em breve, adicionaremos capturas de tela e um vídeo demonstrativo para mostrar o app em ação!

## 🚀 Como Instalar e Rodar Localmente

**Pré-requisitos:** [Android Studio](https://developer.android.com/studio) instalado e dispositivos conectados na mesma rede Wi-Fi.

1. Clone o repositório ou baixe o código fonte.
2. Abra a pasta do projeto no **Android Studio**.
3. Sincronize o projeto com o Gradle.
4. Compile e instale o app em **dois dispositivos** (o celular do motorista e o tablet/celular do passageiro).

### Configuração no Celular do Motorista (Servidor)
1. Abra o app e selecione **Motorista**.
2. Conceda a permissão de **Desenhar Sobre Outros Apps** (necessária para a bolha flutuante).
3. Conceda a permissão do **Serviço de Acessibilidade** ao "Pix no Banco de Trás" (necessária para capturar a tela anonimamente).
4. Inicie o servidor. A bolha flutuante aparecerá na tela.

### Configuração no Tablet do Passageiro (Cliente)
1. Abra o app e selecione **Passageiro**.
2. (Opcional, mas recomendado) Conceda a permissão de **Administrador do Dispositivo** se quiser que o motorista consiga bloquear a tela do tablet remotamente após o pagamento.
3. O app buscará o motorista automaticamente. Quando conectado, a tela ficará preta e aguardará comandos.

## ⚙️ Permissões Sensíveis e Privacidade

Este aplicativo solicita permissões avançadas do Android para funcionar com a menor fricção possível durante a direção:

*   **Serviço de Acessibilidade (`AccessibilityService`):** Usado *exclusivamente* para tirar um screenshot silencioso quando o motorista clica no botão "Capturar" da bolha. O app busca QR Codes na imagem extraída e descarta a imagem. Nenhum dado de tela sai da rede local do carro.
*   **Administrador do Dispositivo (`DeviceAdminReceiver`):** Usado apenas no modo passageiro, caso configurado, para que o comando "Apagar Tela" efetivamente bloqueie o dispositivo, economizando bateria do tablet traseiro.

## 🤝 Contribuição

Fique à vontade para abrir *Issues* ou enviar *Pull Requests* se encontrar bugs ou quiser sugerir melhorias. 

## 📝 Licença

Este projeto é de código aberto. Sinta-se livre para modificá-lo e adaptá-lo para as necessidades do seu dia a dia no trânsito.