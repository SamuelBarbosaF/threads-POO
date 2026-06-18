package br.edu.datacenter.controller;

import br.edu.datacenter.model.entities.Servidor;
import br.edu.datacenter.model.entities.StatusTarefa;
import br.edu.datacenter.model.entities.Tarefa;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {
    //Ligação com o FXML
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private VBox serversContainer;
    @FXML private ListView<String> readyQueueList;
    @FXML private ListView<String> waitingQueueList;
    @FXML private LineChart<Number,Number> waitTimeChart;
    @FXML private BarChart<String,Number> utilizationChart;
    @FXML private TextArea summaryArea;
    @FXML private TextArea logArea;

    private int tickCount = 0;
    private Timeline uiUpdateTimeline; //timer p atualização
    private final SimuladorController simuladorController = new SimuladorController();
    private final Map<String, HBox> serverCards = new HashMap<>();
    private final XYChart.Series<Number, Number> waitSeries = new XYChart.Series<>(); //p gráfico do tempo de espera

    @FXML
    public void initialize() {
        // Configurações iniciais dos componentes, start/stop
        waitTimeChart.getData().add(waitSeries);

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
            utilizationChart.getData().clear();
            summaryArea.clear();
        });
    }

    //A cada 500ms chama refreshDashboard(), que atualiza todos os componentes visuais com os dados mais recentes da simulação.
    private void startUiRefresh() {
        if (uiUpdateTimeline != null) {
            uiUpdateTimeline.stop();
        }
        uiUpdateTimeline = new Timeline(new KeyFrame(Duration.millis(500), event -> refreshDashboard()));
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
        // Atualiza métricas no summaryArea
        // Atualiza gráfico de utilização
        if (simuladorController.getCluster() == null || simuladorController.getMotor() == null) {
            return;
        }

        List<String> readyTasks = new ArrayList<>();
        List<String> waitingTasks = new ArrayList<>();

        simuladorController.getMotor().getFilaGlobalDeProntos().forEach(tarefa ->
                readyTasks.add(formatTask(tarefa)));

        simuladorController.getMotor().getTodasAsTarefasDoSistema().stream()
                .filter(tarefa -> tarefa.getStatus() == StatusTarefa.BLOQUEADA)
                .forEach(tarefa -> waitingTasks.add(formatTask(tarefa)));

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

        String summaryText = String.join("\n",
                "── Tempo ──────────────────────",
                "Espera média:         " + String.format("%.0f ms", mediaEspera),
                "Processamento médio:  " + String.format("%.0f ms", tempoMedioProcessamento),
                "",
                "── Throughput ─────────────────",
                "Vazão:                " + String.format("%.1f tarefas/min", vazao),
                "Concluídas:           " + concluido,
                "Total no sistema:     " + totalTarefas,
                "",
                "── Filas ──────────────────────",
                "Prontas:              " + readyTasks.size(),
                "Bloqueadas:           " + waitingTasks.size(),
                "",
                "── Recursos ───────────────────",
                "Utilização média:     " + String.format("%.1f%%", utilizacaoMedia * 100));

        Platform.runLater(() -> summaryArea.setText(summaryText));

        updateUtilizationChart(servidores);
    }

    private void updateUtilizationChart(List<Servidor> servidores) {
        utilizationChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Servidor servidor : servidores) {
            double utilization = Math.min(1.0, servidor.getCargaAtual() / (double) servidor.getCapacidadeMaxima());
            series.getData().add(new XYChart.Data<>("S" + servidor.getId(), utilization * 100));
        }
        utilizationChart.getData().add(series);
    }

    private String formatTask(Tarefa tarefa) {
        return "T" + tarefa.getId()
                + " [p=" + tarefa.getPrioridade() + "]"
                + " status=" + tarefa.getStatus()
                + " deps=" + tarefa.getIdsDependencias();
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
    public void updateReadyQueue(java.util.List<String> tasks) {
        Platform.runLater(() -> {
            readyQueueList.getItems().setAll(tasks);
        });
    }

    // Atualiza a lista visual de tarefas bloqueadas na tela.
    public void updateWaitingQueue(java.util.List<String> tasks) {
        Platform.runLater(() -> {
            waitingQueueList.getItems().setAll(tasks);
        });
    }

    public void addWaitTimeSample(Number x, Number y) {
        Platform.runLater(() -> {
            waitSeries.getData().add(new XYChart.Data<>(x, y));
            if (waitSeries.getData().size() > 60) {
                waitSeries.getData().remove(0);
            }
        });
    }

}
