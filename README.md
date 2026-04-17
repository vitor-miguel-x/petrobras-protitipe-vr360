# Projeto Krytos 🛡️👁️

## 📝 Sobre o Projeto
O **Projeto Krytos** é um software de inteligência e segurança de última geração, desenvolvido para integrar a **autenticação biométrica de íris** diretamente ao fluxo de trabalho dos óculos de Realidade Aumentada (RA).

Nossa solução vai além de um login convencional. Estabelecemos um ecossistema seguro e confiável que acessa sistemas de IA corporativos com:
* **Autenticação por Íris:** Monitoramento contínuo de quem está operando o dispositivo.
* **Autorização em Tempo Real:** O assistente de IA só responde se reconhecer o colaborador autorizado.
* **Segurança Avançada:** Criptografia de ponta a ponta e prevenção ativa de vazamento de dados.
* **Governança:** Garantia de que a pessoa certa acesse o dado certo, no momento exato, unindo acessibilidade e produtividade à conformidade corporativa.

---

## 🏁 Grand Prix SENAI 2026
Este projeto foi idealizado e desenvolvido como resposta aos desafios de inovação propostos no **Grand Prix SENAI de Inovação 2026**. O Krytos reflete os pilares da competição: criatividade, colaboração e empreendedorismo aplicado à cibersegurança e acessibilidade.

### 👥 Escuderia Responsável
O desenvolvimento e a ideação foram realizados pela nossa equipe de alunos dedicados:

* **Maria Eduarda Reis da Silva** - Porta-voz e Estrategista de Negócios (Pitch & Lean Canvas), Designer de Experiência e UI/UX (Prototipação), Pesquisadora e Documentação (Design Thinking & Contexto).
* **Vitor Miguel Rodrigues Cezario** - Desenvolvedor Lead (Android Studio & Integração), Designer de Experiência e UI/UX (Prototipação).
* **Matheus da Silva Conceição** - Designer de Experiência e UI/UX (Prototipação).
* **Kauã Santos da Silva Cruz** - Especialista em Cibersegurança e IA (Lógica de Autenticação e Criptografia).
* **Matheus Gabriel Perez dos Santos** - Pesquisador e Documentação (Design Thinking & Contexto).

---

## 🚀 Guia de Instalação (Passo a Passo Metódico)

Este guia foi feito para que qualquer pessoa, mesmo sem experiência em programação, consiga instalar o aplicativo Krytos em um dispositivo Android usando o **Android Studio**.

### Passo 1: Preparando o seu Computador
Antes de começar, você precisa baixar e instalar as ferramentas básicas:

1.  **Android Studio:** É o programa onde o aplicativo foi construído.
    * [Baixar Android Studio](https://developer.android.com/studio)
2.  **Git:** Ferramenta para baixar o código do projeto para o seu PC.
    * [Baixar Git para Windows/Mac](https://git-scm.com/downloads)

### Passo 2: Baixando o Código do Projeto
Agora vamos trazer os arquivos do projeto para o seu computador.

1.  Crie uma pasta no seu computador (ex: `C:\\Projetos`).
2.  Dentro dessa pasta, clique com o botão direito e selecione **"Git Bash Here"** (ou abra o terminal/prompt de comando).
3.  Digite o seguinte comando e aperte Enter:
    ```bash
    git clone [https://github.com/usuario/projeto-krytos.git](https://github.com/usuario/projeto-krytos.git)
    ```
    *(Nota: Substitua o link acima pelo link real do seu repositório Git).*

### Passo 3: Abrindo no Android Studio
1.  Abra o **Android Studio**.
2.  Clique em **"Open"** (Abrir).
3.  Navegue até a pasta onde você baixou o projeto no Passo 2 e selecione-a.
4.  Aguarde o Android Studio carregar. No canto inferior direito, você verá uma barra de progresso chamada "Gradle Build". **Espere ela terminar totalmente** antes de fazer qualquer coisa. Isso pode levar alguns minutos.

---

### Passo 4: Preparando o Celular ou Óculos (Modo Debug)
Para o computador enviar o app para o dispositivo, precisamos ativar uma "porta dos fundos" chamada **Depuração USB**.

#### Como ativar o Modo Desenvolvedor:
1.  No seu dispositivo Android, vá em **Configurações**.
2.  Vá em **Sobre o Telefone** ou **Informações do Software**.
3.  Procure por **"Número da Versão"** ou **"Número da Compilação"**.
4.  Toque **7 VEZES seguidas** nesse número até aparecer a mensagem: *"Você agora é um desenvolvedor!"*.
5.  Volte ao menu principal das Configurações e procure por **"Opções do Desenvolvedor"**.
6.  Lá dentro, ative a chave **"Depuração USB"**.

#### Particularidades por Marca:
Cada fabricante esconde ou adiciona travas extras. Veja a sua:

* **Samsung:** Geralmente padrão. Se usar o Samsung Knox, pode haver restrições de segurança que impedem a instalação de apps fora da loja.
* **Xiaomi (MIUI):** Além de "Depuração USB", você DEVE ativar a opção **"Depuração USB (Configurações de Segurança)"** e **"Instalar via USB"**, caso contrário, o Android Studio dará erro de instalação. Requer conta Mi logada.
* **Motorola e Google Pixel:** Seguem o padrão acima sem passos extras.
* **Huawei:** Pode ser necessário baixar o [HiSuite](https://consumer.huawei.com/en/support/hisuite/) para que os drivers funcionem. Nas opções de desenvolvedor, ative "Permitir que o ADB depure apenas no modo de carregamento".
* **LG:** Verifique se o modo de conexão USB está definido como "Transferência de Arquivos (MTP)".

#### Drivers Necessários:
Se o computador não reconhecer o celular ao conectar o cabo:
* [Driver Geral do Google](https://developer.android.com/studio/run/win-usb)
* [Drivers da Samsung](https://developer.samsung.com/android-usb-driver)
* [Drivers da Motorola](https://motorola-global-portal-pt.custhelp.com/app/answers/detail/a_id/87130)

---

### Passo 5: Instalando o Aplicativo
1.  Conecte o dispositivo ao computador via cabo USB de boa qualidade.
2.  No celular, aparecerá uma pergunta: *"Permitir depuração USB?"*. Marque "Sempre permitir" e dê **OK**.
3.  No topo do **Android Studio**, você verá um menu suspenso com o nome do seu dispositivo. Selecione-o.
4.  Clique no botão **Play (triângulo verde)** 🟢 localizado na barra superior.
5.  O computador vai compilar o código e, em instantes, o app abrirá automaticamente no seu dispositivo.

---

## 🛠️ Tecnologias Utilizadas
* **Linguagem:** Kotlin/Java
* **IDE:** Android Studio
* **Segurança:** Biometria Facial/Íris via Camera API
* **IA:** Integração com APIs corporativas de Inteligência Artificial

---

## 📞 Suporte
Se encontrar dificuldades no processo de "Build" ou erros de conexão USB, verifique se o seu cabo é de dados (e não apenas de carga) ou tente trocar a porta USB do computador.
"""
