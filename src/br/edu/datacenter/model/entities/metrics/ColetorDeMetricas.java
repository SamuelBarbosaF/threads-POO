package br.edu.datacenter.model.entities.metrics;

import br.edu.datacenter.model.entities.Servidor;
import br.edu.datacenter.model.entities.StatusTarefa;
import br.edu.datacenter.model.entities.Tarefa;
import java.util.List;

/**
 * ## ColetorDeMetricas
 *
 * Calcula estatisticas da simulacao usando as tarefas e servidores.
 */
public class ColetorDeMetricas {

    // Lista de tarefas analisadas.
    private final List<Tarefa> tarefas;

    // Lista de servidores analisados.
    private final List<Servidor> servidores;

    /**
     * Recebe as listas usadas para calcular as metricas.
     */
    public ColetorDeMetricas(
            List<Tarefa> tarefas,
            List<Servidor> servidores) {

        // Guarda a lista de tarefas.
        this.tarefas = tarefas;

        // Guarda a lista de servidores.
        this.servidores = servidores;
    }

    /**
     * Calcula o tempo medio entre chegada e inicio da execucao.
     */
    public double calcularTempoMedioEspera() {
        // Soma dos tempos de espera.
        long soma = 0;

        // Quantidade de tarefas consideradas.
        int quantidade = 0;

        // Percorre todas as tarefas.
        for (Tarefa tarefa : tarefas) {
            // Considera apenas tarefas que ja iniciaram.
            if (tarefa.getTempoInicioExecucao() > 0) {
                soma += tarefa.getTempoEspera();
                quantidade++;
            }
        }

        // Evita divisao por zero.
        return quantidade == 0
                ? 0
                : (double) soma / quantidade;
    }

    /**
     * Calcula o tempo medio entre inicio e fim da execucao.
     */
    public double calcularTempoMedioProcessamento() {
        long soma = 0;
        int quantidade = 0;

        for (Tarefa tarefa : tarefas) {
            // Considera apenas tarefas concluidas.
            if (tarefa.getTempoFimExecucao() > 0) {
                soma += tarefa.getTempoProcessamento();
                quantidade++;
            }
        }

        return quantidade == 0
                ? 0
                : (double) soma / quantidade;
    }

    /**
     * Calcula o tempo medio total dentro do sistema.
     */
    public double calcularTempoMedioNoSistema() {
        long soma = 0;
        int quantidade = 0;

        for (Tarefa tarefa : tarefas) {
            // Considera apenas tarefas que ja terminaram.
            if (tarefa.getTempoFimExecucao() > 0) {
                soma += tarefa.getTempoTotalNoSistema();
                quantidade++;
            }
        }

        return quantidade == 0
                ? 0
                : (double) soma / quantidade;
    }

    /**
     * Conta quantas tarefas estao concluidas.
     */
    public int getQuantidadeConcluidas() {
        int total = 0;

        for (Tarefa tarefa : tarefas) {
            if (tarefa.getStatus() == StatusTarefa.CONCLUIDA) {
                total++;
            }
        }

        return total;
    }

    /**
     * Calcula vazao: tarefas concluidas por minuto.
     */
    public double calcularVazao() {
        // Primeiro horario de chegada entre tarefas concluidas.
        long primeiro = Long.MAX_VALUE;

        // Ultimo horario de fim entre tarefas concluidas.
        long ultimo = Long.MIN_VALUE;

        // Quantidade de tarefas concluidas.
        int concluidas = 0;

        for (Tarefa tarefa : tarefas) {
            if (tarefa.getTempoFimExecucao() > 0) {
                concluidas++;
                primeiro = Math.min(primeiro, tarefa.getTempoChegada());
                ultimo = Math.max(ultimo, tarefa.getTempoFimExecucao());
            }
        }

        if (concluidas == 0) {
            return 0;
        }

        // Converte milissegundos para minutos.
        double minutos = (ultimo - primeiro) / 60000.0;

        if (minutos <= 0) {
            return concluidas;
        }

        return concluidas / minutos;
    }

    /**
     * Calcula o percentual de uso de um servidor.
     */
    public double calcularUtilizacaoServidor(
            Servidor servidor,
            long tempoTotalSimulacao) {

        if (tempoTotalSimulacao <= 0) {
            return 0;
        }

        // Uso = tempo trabalhando / tempo total da simulacao.
        double utilizacao =
                (servidor.getTempoTotalProcessamento() * 100.0)
                / tempoTotalSimulacao;

        // Limita a utilizacao em 100%.
        return Math.min(utilizacao, 100);
    }

    /**
     * Calcula o percentual de ociosidade de um servidor.
     */
    public double calcularOciosidadeServidor(
            Servidor servidor,
            long tempoTotalSimulacao) {

        return 100.0 - calcularUtilizacaoServidor(servidor, tempoTotalSimulacao);
    }

    /**
     * Calcula a media de utilizacao dos servidores.
     */
    public double calcularUtilizacaoMedia(long tempoTotalSimulacao) {
        if (servidores.isEmpty()) {
            return 0;
        }

        double soma = 0;

        for (Servidor servidor : servidores) {
            soma += calcularUtilizacaoServidor(servidor, tempoTotalSimulacao);
        }

        return soma / servidores.size();
    }

    /**
     * Verifica se a fila do servidor esta com 80% ou mais de ocupacao.
     */
    public boolean servidorSobrecarregado(Servidor servidor) {
        return servidor.getFilaLocal().size()
                >= servidor.getCapacidadeMaxima() * 0.8;
    }

    /**
     * Verifica se o servidor nao tem tarefas esperando na fila.
     */
    public boolean servidorOcioso(Servidor servidor) {
        return servidor.getFilaLocal().isEmpty();
    }

    /**
     * Conta quantos servidores estao sobrecarregados.
     */
    public int getQuantidadeServidoresSobrecarregados() {
        int total = 0;

        for (Servidor servidor : servidores) {
            if (servidorSobrecarregado(servidor)) {
                total++;
            }
        }

        return total;
    }

    /**
     * Conta quantos servidores estao ociosos.
     */
    public int getQuantidadeServidoresOciosos() {
        int total = 0;

        for (Servidor servidor : servidores) {
            if (servidorOcioso(servidor)) {
                total++;
            }
        }

        return total;
    }

    /**
     * Monta um texto com as principais metricas.
     */
    public String gerarResumo(long tempoTotalSimulacao) {
        return String.format(
                """
                ===== METRICAS DO DATA CENTER =====
                Tarefas Concluidas: %d
                Tempo Medio de Espera: %.2f ms
                Tempo Medio de Processamento: %.2f ms
                Tempo Medio no Sistema: %.2f ms
                Vazao: %.2f tarefas/min
                Utilizacao Media: %.2f %%
                Servidores Ociosos: %d
                Servidores Sobrecarregados: %d
                ==================================
                """,
                getQuantidadeConcluidas(),
                calcularTempoMedioEspera(),
                calcularTempoMedioProcessamento(),
                calcularTempoMedioNoSistema(),
                calcularVazao(),
                calcularUtilizacaoMedia(tempoTotalSimulacao),
                getQuantidadeServidoresOciosos(),
                getQuantidadeServidoresSobrecarregados());
    }
}
