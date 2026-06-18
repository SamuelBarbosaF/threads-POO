package br.edu.datacenter.controller;

import br.edu.datacenter.model.entities.Servidor;
import br.edu.datacenter.model.entities.StatusTarefa;
import br.edu.datacenter.model.entities.Tarefa;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {
    //Ligação com o FXML
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private VBox serversContainer;
    @FXML private ListView<Tarefa> readyQueueList;
    @FXML private ListView<Tarefa> waitingQueueList;
    @FXML private AreaChart<Number,Number> waitTimeChart;
    @FXML private BarChart<String,Number> utilizationChart;
    @FXML private TextArea logArea;
    @FXML private Label metricConcluidas;
    @FXML private Label metricVazao;
    @FXML private Label metricEspera;
    @FXML private Label metricProcessamento;
    @FXML private Label metricUtilizacao;
    @FXML private Label metricBloqueadas;
    @FXML private Label readyCountLabel;
    @FXML private Label waitingCountLabel;
    @FXML private Label metricTotal;

    private int quantidadeTarefasTotal = 0;
    private int tickCount = 0;
    private Timeline uiUpdateTimeline; //timer p atualização
    private final SimuladorController simuladorController = new SimuladorController();
    private final Map<String, HBox> serverCards = new HashMap<>();
    private final XYChart.Series<Number, Number> waitSeries = new XYChart.Series<>(); //p gráfico do tempo de espera
    private final XYChart.Series<String, Number> utilizationSeries = new XYChart.Series<>();

    @FXML
    public void initialize() {
        // Configurações iniciais dos componentes, start/stop
        waitTimeChart.getData().add(waitSeries);
        waitTimeChart.setAnimated(false);
        utilizationChart.setAnimated(false);
        utilizationChart.getData().add(utilizationSeries);
        utilizationChart.setLegendVisible(false);
        readyQueueList.setCellFactory(_ -> criarCelulaTarefa(false));
        waitingQueueList.setCellFactory(_ -> criarCelulaTarefa(true));

        // desabilita stop ao iniciar
        btnStop.setDisable(true);
    }

    //Controle do estado dos botões
    @FXML
    private void onStart() {
        btnStart.setDisable(true);
        btnStop.setDisable(false);
        System.out.println("Simulação iniciada");

        int quantidadeServidores = 4;
        int capacidadeServidor = 1; // ajuste conforme o teste
        int quantidadeTarefas = 50;
        quantidadeTarefasTotal = 50;

        simuladorController.iniciarSimulacao(
                quantidadeServidores,
                capacidadeServidor,
                quantidadeTarefas);
        appendLog("Simulação iniciada com " + quantidadeServidores + " servidores e " + quantidadeTarefas + " tarefas.");
        startUiRefresh();
    }

    @FXML
    private void onStop() {
        btnStart.setDisable(false);
        btnStop.setDisable(true);
        tickCount = 0;
        System.out.println("Simulação parada");
        simuladorController.pararSimulacao();
        appendLog("Simulação parada pelo usuário.");
        stopUiRefresh();
        clearServerCards();
    }

    private void clearServerCards() {
        Platform.runLater(() -> {
            serverCards.clear();
            serversContainer.getChildren().clear();
            readyQueueList.getItems().clear();
            waitingQueueList.getItems().clear();
            waitSeries.getData().clear();
            utilizationSeries.getData().clear();

        });
    }

    //A cada 500ms chama refreshDashboard(), que atualiza todos os componentes visuais com os dados mais recentes da simulação.
    private void startUiRefresh() {
        if (uiUpdateTimeline != null) {
            uiUpdateTimeline.stop();
        }
        uiUpdateTimeline = new Timeline(new KeyFrame(Duration.millis(500), _ -> refreshDashboard()));
        uiUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        uiUpdateTimeline.play();
    }

    private void stopUiRefresh() {
        if (uiUpdateTimeline != null) {
            uiUpdateTimeline.stop();
            uiUpdateTimeline = null;
        }
    }

    private void refreshDashboard() {
        // Coleta tarefas prontas e bloqueadas
        // Atualiza filas visuais
        // Atualiza cards dos servidores
        // Atualiza gráficos de utilização
        if (simuladorController.getCluster() == null || simuladorController.getMotor() == null) {
            return;
        }
        //mostra tudo que não está bloqueado (pronta + executando + concluída)
        List<Tarefa> readyTasks = simuladorController.getMotor()
                .getTodasAsTarefasDoSistema().stream()
                .filter(t -> t.getStatus() != StatusTarefa.BLOQUEADA)
                .sorted(Comparator.comparing(t -> t.getStatus().name()))
                .collect(Collectors.toList());

        List<Tarefa> waitingTasks = simuladorController.getMotor()
                .getTodasAsTarefasDoSistema().stream()
                .filter(t -> t.getStatus() == StatusTarefa.BLOQUEADA)
                .collect(java.util.stream.Collectors.toList());

        updateReadyQueue(readyTasks);
        updateWaitingQueue(waitingTasks);

        List<Servidor> servidores = simuladorController.getCluster().getServidores();
        for (Servidor servidor : servidores) {
            double utilization = Math.min(1.0, servidor.getCargaAtual() / (double) servidor.getCapacidadeMaxima());
            String status = servidor.isOcupado() ? "EXECUTANDO" : (servidor.getCargaAtual() == 0 ? "OCIOSO" : "EM FILA");
            addOrUpdateServerCard(
                    "Servidor " + servidor.getId(),
                    utilization,
                    status + " (" + servidor.getCargaAtual() + "/" + servidor.getCapacidadeMaxima() + ")",
                    servidor.getTarefasProcessadas());
        }

        double mediaEspera = simuladorController.getTempoMedioEspera();
        double utilizacaoMedia = simuladorController.getUtilizacaoMedia();
        int concluido = simuladorController.getQuantidadeConcluidas();
        int totalTarefas = simuladorController.getMotor().getTodasAsTarefasDoSistema().size();

        // Alimenta o gráfico de linha a cada tick
        tickCount++;
        addWaitTimeSample(tickCount, mediaEspera);
        double vazao = simuladorController.getVazao();
        double tempoMedioProcessamento = simuladorController.getTempoMedioProcessamento();

        Platform.runLater(() -> {
            metricConcluidas.setText(String.valueOf(concluido));
            metricVazao.setText(String.format("%.1f /min", vazao));
            metricTotal.setText(String.valueOf(totalTarefas));
            metricEspera.setText(String.format("%.0f ms", mediaEspera));
            metricProcessamento.setText(String.format("%.0f ms", tempoMedioProcessamento));
            metricUtilizacao.setText(String.format("%.1f%%", utilizacaoMedia));
            metricBloqueadas.setText(String.valueOf(waitingTasks.size()));
        });

        updateUtilizationChart(servidores);

        // Auto-stop quando todas as tarefas forem concluídas
        if (quantidadeTarefasTotal > 0
                && totalTarefas >= quantidadeTarefasTotal
                && concluido == totalTarefas) {
            appendLog("✔ Todas as " + concluido + " tarefas concluídas. Simulação encerrada.");
            onStop();
        }
    }

    private void updateUtilizationChart(List<Servidor> servidores) {
        // Cria os itens na primeira vez
        if (utilizationSeries.getData().isEmpty()) {
            for (Servidor servidor : servidores) {
                utilizationSeries.getData().add(
                        new XYChart.Data<>("S" + servidor.getId(), 0));
            }
        }

        // Atualiza valores e cores semanticamente
        for (int i = 0; i < servidores.size() && i < utilizationSeries.getData().size(); i++) {
            double percent = Math.min(1.0, servidores.get(i).getCargaAtual()
                    / (double) servidores.get(i).getCapacidadeMaxima()) * 100;

            XYChart.Data<String, Number> data = utilizationSeries.getData().get(i);
            data.setYValue(percent);

            if (data.getNode() != null) {
                String cor = percent >= 80 ? "#A32D2D"   // vermelho — sobrecarregado
                        : percent >= 50 ? "#BA7517"   // laranja  — ocupado
                        :                 "#1D9E75";  // verde    — ok
                data.getNode().setStyle("-fx-bar-fill: " + cor + ";");
            }
        }

    }

    //Adiciona mensagens no TextArea de log e limita a 100 linhas.
    private void appendLog(String message) {
        if (logArea == null) {
            return;
        }
        logArea.appendText(message + "\n");
        if (logArea.getText().split("\n").length > 150) {
            String[] lines = logArea.getText().split("\n");
            StringBuilder trimmed = new StringBuilder();
            for (int i = Math.max(0, lines.length - 100); i < lines.length; i++) {
                trimmed.append(lines[i]).append("\n");
            }
            logArea.setText(trimmed.toString());
        }
    }

    //Para cada servidor, cria um card visual (se ainda não existe) ou atualiza o existente.
    public void addOrUpdateServerCard(String serverId, double utilization, String status, int tarefasProcessadas) {
        Platform.runLater(() -> {
            HBox card = serverCards.get(serverId);
            if (card == null) {
                card = createServerCard(serverId, utilization, status, tarefasProcessadas);
                serverCards.put(serverId, card);
                serversContainer.getChildren().add(card);
            } else {
                updateServerCard(card, utilization, status, tarefasProcessadas);
            }
        });
    }

    private HBox createServerCard(String serverId, double utilization, String status, int tarefasProcessadas) {
        HBox box = new HBox(8);
        Label id = new Label(serverId);
        ProgressBar pb = new ProgressBar(utilization);
        pb.setPrefWidth(180);
        Label st = new Label(status);
        Label tp = new Label("✔ " + tarefasProcessadas);
        tp.setStyle("-fx-text-fill: #1D9E75; -fx-font-size: 11px;");
        HBox.setHgrow(pb, Priority.ALWAYS);
        box.getChildren().addAll(id, pb, st, tp);
        box.setUserData(pb); // store progressbar for updates
        return box;
    }

    private void updateServerCard(HBox card, double utilization, String status, int tarefasProcessadas) {

        ProgressBar pb = (ProgressBar) card.getChildren().get(1);
        Label st = (Label) card.getChildren().get(2);
        Label tp = (Label) card.getChildren().get(3);

        pb.setProgress(utilization);
        st.setText(status);
        tp.setText("✔ " + tarefasProcessadas);
    }

    // Atualiza a lista visual de tarefas prontas na tela.
    public void updateReadyQueue(List<Tarefa> tasks) {
        Platform.runLater(() -> {
            readyQueueList.getItems().setAll(tasks);
            readyCountLabel.setText("(" + tasks.size() + ")");
        });
    }

    // Atualiza a lista visual de tarefas bloqueadas na tela.
    public void updateWaitingQueue(List<Tarefa> tasks) {
        Platform.runLater(() -> {
            waitingQueueList.getItems().setAll(tasks);
            waitingCountLabel.setText("(" + tasks.size() + ")");
        });
    }

    public void addWaitTimeSample(Number x, Number y) {
        Platform.runLater(() -> {
            waitSeries.getData().add(new XYChart.Data<>(x, y));
            if (waitSeries.getData().size() > 60) {
                waitSeries.getData().removeFirst();
            }
            // Eixo X acompanha a janela dos últimos 60 ticks
            NumberAxis xAxis = (NumberAxis) waitTimeChart.getXAxis();
            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(Math.max(0, tickCount - 60));
            xAxis.setUpperBound(Math.max(60, tickCount));
            xAxis.setTickUnit(10);
        });
    }

    private ListCell<Tarefa> criarCelulaTarefa(boolean mostrarDetalheBloqueio) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Tarefa tarefa, boolean empty) {
                super.updateItem(tarefa, empty);
                if (empty || tarefa == null) {
                    setGraphic(null);
                    return;
                }

                HBox row = new HBox(6);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new javafx.geometry.Insets(2, 4, 2, 4));

                // ID
                Label idLabel = new Label("T" + tarefa.getId());
                idLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 32px;");

                // Badge de prioridade
                int p = tarefa.getPrioridade();
                String bg, fg, texto;
                if (p <= 2)      { bg = "#FCEBEB"; fg = "#A32D2D"; texto = "alta"; }
                else if (p == 3) { bg = "#FAEEDA"; fg = "#BA7517"; texto = "med";  }
                else             { bg = "#EAF3DE"; fg = "#3B6D11"; texto = "baixa";}

                Label prioLabel = new Label(texto);
                prioLabel.setStyle(
                        "-fx-background-color: " + bg + ";" +
                                "-fx-text-fill: " + fg + ";" +
                                "-fx-font-size: 10px;" +
                                "-fx-padding: 1 6 1 6;" +
                                "-fx-background-radius: 4;"
                );

                row.getChildren().addAll(idLabel, prioLabel);

                if (mostrarDetalheBloqueio) {
                    // Dependências pendentes
                    Label depsLabel = new Label("dep: " + tarefa.getIdsDependencias());
                    depsLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;");

                    // Tempo aguardando
                    long esperaMs = System.currentTimeMillis() - tarefa.getTempoChegada();
                    String tempoStr = esperaMs < 60000
                            ? String.format("%.0fs", esperaMs / 1000.0)
                            : String.format("%.1fmin", esperaMs / 60000.0);
                    Label tempoLabel = new Label("⏳ " + tempoStr);
                    tempoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #A32D2D;");

                    row.getChildren().addAll(depsLabel, tempoLabel);
                } else {
                    // Badge de status para a fila de prontos
                    String statusTxt, statusBg, statusFg;
                    switch (tarefa.getStatus()) {
                        case PRONTA     -> { statusTxt = "pronta";     statusBg = "#E1F5EE"; statusFg = "#0F6E56"; }
                        case EXECUTANDO -> { statusTxt = "executando"; statusBg = "#E6F1FB"; statusFg = "#185FA5"; }
                        case CONCLUIDA  -> { statusTxt = "concluída";  statusBg = "#F1EFE8"; statusFg = "#5F5E5A"; }
                        default         -> { statusTxt = "?";          statusBg = "#F1EFE8"; statusFg = "#5F5E5A"; }
                    }

                    Label statusLabel = new Label(statusTxt);
                    statusLabel.setStyle(
                            "-fx-background-color: " + statusBg + ";" +
                                    "-fx-text-fill: " + statusFg + ";" +
                                    "-fx-font-size: 10px;" +
                                    "-fx-padding: 1 6 1 6;" +
                                    "-fx-background-radius: 4;"
                    );
                    row.getChildren().add(statusLabel);
                }

                setGraphic(row);
            }
        };
    }

}
