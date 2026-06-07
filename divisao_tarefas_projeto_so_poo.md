# Plano de Divisão de Tarefas: Simulador de Data Center (SO I & POO)

Este documento estabelece o escopo, a arquitetura conceitual e a divisão de tarefas para a equipe de 3 desenvolvedores. O objetivo é construir um **Escalonador de Tarefas para Data Center** utilizando conceitos avançados de **Programação Orientada a Objetos (POO)**, concorrência com **Threads (Java)** e uma interface de alta fidelidade em **JavaFX**.

---

## 📌 Visão Geral do Projeto (Problema 6)

O sistema irá simular o comportamento de um grande data center recebendo tarefas dinamicamente. As tarefas possuem dependências (Grafo de Dependência) e prioridades. Os servidores possuem capacidade limitada. O sistema deve mitigar *starvation*, balancear carga e evitar ociosidade/sobrecarga, gerando métricas de desempenho em tempo real.

---

## 👥 Perfis da Equipe

* **Desenvolvedor 1 (Dev 1 - Frontend):** Responsável pela Interface Gráfica (JavaFX), UX/UI, gráficos e atualização visual da simulação em tempo real.
* **Desenvolvedor 2 (Dev 2 - Core & Concorrência):** Responsável pela estrutura de dados concorrente, ciclo de vida das Threads dos Servidores e resolução do Grafo de Dependências.
* **Desenvolvedor 3 (Dev 3 - Algoritmos & Métricas):** Responsável pelas políticas de escalonamento (Ex: FIFO, Prioridade, SJF), lógica de Balanceamento de Carga e cálculo estatístico das métricas do Data Center.

---

## 🏗️ Proposta de Arquitetura de Classes (POO)

Para garantir que o trabalho dos três desenvolvedores se integre perfeitamente, o projeto seguirá o seguinte modelo inicial de classes:

```
[Tarefa] ──> (Possui dependências de) ──> [Tarefa]
   │
   ▼
[Fila de Entrada / Escalonador] ──> (Aplica Política / Balanceamento)
   │
   ▼
[Servidor (Thread)] ──> (Atualiza) ──> [Gerenciador de Métricas]
   │
   ▼ (Notifica via Listener / Callback)
[Interface JavaFX]
```

---

## 📋 Divisão Detalhada de Tarefas (Backlog por Desenvolvedor)

### 🎨 Desenvolvedor 1: Interface Gráfica & UX (JavaFX)

*Foco: Visualização limpa dos estados dos processos, filas e métricas. Garante o ponto extra.*

- [ ] **Configuração do Ambiente JavaFX:** Configurar o projeto base com suporte a JavaFX e arquitetura MVC (Model-View-Controller).
- [ ] **Painel de Controle de Simulação:** Criar inputs para o usuário configurar parâmetros: quantidade de servidores, política de escalonamento padrão, taxa de chegada de tarefas e botão de Start/Pause/Stop.
- [ ] **Visualização dos Servidores (Monitor de Carga):** Criar componentes visuais (Cards ou Barras de Progresso) que mostrem o estado de cada servidor em tempo real (Ocioso, Processando, Sobregregado).
- [ ] **Visualização de Filas (Fila de Espera e Prontos):** Renderizar de forma dinâmica as listas de tarefas aguardando liberação de dependências e as tarefas prontas para execução.
- [ ] **Dashboard de Métricas Gráficas:** Implementar gráficos de linha/barra dinâmicos (usando `LineChart` ou `BarChart` do JavaFX) para mostrar o Tempo Médio de Espera e o Percentual de Utilização dos Servidores.
- [ ] **Mecanismo de Atualização Segura (Concorrência UI):** Implementar o uso estrito de `Platform.runLater()` para receber atualizações do Core (Threads) e atualizar a UI sem travar a interface do JavaFX.

### ⚙️ Desenvolvedor 2: Core Engine, Threads & Grafo de Dependências

*Foco: Sincronização, concorrência segura, modelagem orientada a objetos das entidades base.*

- [X] **Modelagem das Classes Base (`Tarefa` e `Servidor`):**
  - `Tarefa`: ID, tempo de execução restante, prioridade, status (Bloqueada, Pronta, Executando, Concluída), lista de IDs de dependências.
  - `Servidor`: ID, capacidade máxima de processamento, histórico de uso, fila local de tarefas.
- [X] **Implementação das Threads dos Servidores:** Transformar a classe `Servidor` em uma `Thread` (ou implementar `Runnable`). Cada servidor deve monitorar continuamente sua fila interna e processar as tarefas simulando a passagem de tempo (`Thread.sleep()`).
- [X] **Mecanismo de Resolução de Dependências:** Criar uma classe `DependencyManager` que gerencia o ciclo de vida. Uma tarefa só pode ir para o status "Pronta" quando todas as suas tarefas pré-requisito mudarem para "Concluída".
- [X] **Thread-Safety (Sincronização):** Garantir que a inserção e remoção de tarefas em filas compartilhadas utilizem coleções thread-safe (`ConcurrentLinkedQueue`, `CopyOnWriteArrayList`) ou blocos `synchronized`/`ReentrantLock` para evitar *Race Conditions*.
- [ ] **Gerador de Carga Dinâmica:** Criar uma Thread secundária encarregada de "fabricar" e injetar novas tarefas aleatórias ou pré-definidas no sistema em intervalos regulares (Chegadas Dinâmicas).

### 🧮 Desenvolvedor 3: Algoritmos de Escalonamento, Balanceamento & Métricas

*Foco: Inteligência distributiva do sistema, algoritmos e extração de dados analíticos.*

- [ ] **Módulo de Políticas de Escalonamento:** Criar a interface `SchedulingStrategy` e implementar pelo menos duas políticas diferentes para fins de comparação (Ex: *Shortest Job First (SJF)*, *Escalonamento por Prioridade com Envelhecimento para evitar Starvation*, ou *Round Robin*).
- [ ] **Algoritmo de Balanceamento de Carga (Load Balancer):** Desenvolver o distribuidor inteligente de tarefas. Quando uma tarefa estiver "Pronta", o balanceador deve decidir para qual servidor enviá-la, baseando-se no número de tarefas atual do servidor e sua capacidade (Ex: estratégia *Least Connections* ou *Round Robin entre servidores*).
- [ ] **Mecanismo Antiprivação (Anti-Starvation):** Implementar um mecanismo de "Aging" (Envelhecimento) onde tarefas de baixa prioridade que estão muito tempo na fila de prontos ganham prioridade gradativamente.
- [ ] **Classe `MetricsCollector` (Estatísticas):** Desenvolver o motor matemático para calcular:
  - Tempo médio de espera das tarefas na fila.
  - Vazão do Data Center (Tarefas concluídas por minuto).
  - Taxa de ociosidade e utilização individual de cada servidor (%).
  - Alertas de Sobrecarga e detecção de ociosidade severa.

---

## 🔄 Cronograma de Integração e Marcos (Sprints)

### Fase 1: Alinhamento de Contratos (Dias 1-3)

* **Ação:** Os 3 desenvolvedores se reúnem para fechar os métodos públicos de comunicação.
* **Entregável:** Definição das interfaces Java. Exemplo: Como o `Scheduler` do Dev 3 vai entregar a tarefa para o `Servidor` do Dev 2; e como o Dev 1 (JavaFX) vai se inscrever como observador (Pattern Observer) para escutar as mudanças de estado das tarefas.

### Fase 2: Desenvolvimento Isolado (Dias 4-10)

* Cada desenvolvedor foca no seu backlog em branches separadas no Git (`feature/gui`, `feature/core`, `feature/analytics`).

### Fase 3: A Grande União (Dias 11-13)

* **Integração Backend (Dev 2 + Dev 3):** Conectar os algoritmos de escalonamento e balanceamento no laço de execução das threads dos servidores.
* **Integração Frontend/Backend (Dev 1 + Core):** Vincular as propriedades observáveis (`ObservableList`, `ObjectProperty` do JavaFX) ao motor do simulador.

### Fase 4: Ajustes Finos e Preparação da Apresentação (Dias 14-15)

* Simulações de cenários de teste extremos (ex: injetar 500 tarefas simultâneas para testar robustez).
* Preparação do roteiro de apresentação em dupla/trio conforme os critérios de avaliação (Clareza, Precisão, Resposta a Perguntas).

---

## 📌 Padrões de Projeto (POO) Recomendados para o Grupo

* **Strategy Pattern:** Para alternar dinamicamente as políticas de escalonamento na interface gráfica.
* **Observer Pattern / Listeners:** Para notificar a interface JavaFX toda vez que uma tarefa mudar de estado ou um servidor computar uma métrica, mantendo o desacoplamento.
* **Factory Pattern:** Para o gerador dinâmico de tarefas de forma limpa.

---

*Dica para a Equipe: Façam Commits frequentes e organizados. O professor avaliará o empenho percebido e a qualidade do código implementado! Boa sorte!*
