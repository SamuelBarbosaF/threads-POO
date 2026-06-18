package br.edu.datacenter.model.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * ## Tarefa
 *
 * Representa uma unidade de trabalho dentro da simulacao.
 *
 * Cada objeto desta classe guarda:
 * - identificacao da tarefa;
 * - prioridade;
 * - dependencias;
 * - tempos usados para metricas;
 * - status atual da execucao.
 */
public class Tarefa {

    // Identificador unico da tarefa.
    private final int id;

    // Prioridade da tarefa. Neste projeto, numeros menores representam maior prioridade.
    private final int prioridade;

    // Lista com os IDs das tarefas que precisam terminar antes desta.
    private final List<Integer> idsDependencias;

    // Momento em que a tarefa foi criada ou entrou no sistema.
    private final long tempoChegada;

    // Momento em que a tarefa comecou a executar pela primeira vez.
    private long tempoInicioExecucao;

    // Momento em que a tarefa terminou sua execucao.
    private long tempoFimExecucao;

    // Tempo que ainda falta para a tarefa terminar, em milissegundos.
    private long tempoExecucaoRestante;

    // Estado atual da tarefa: bloqueada, pronta, executando ou concluida.
    private StatusTarefa status;

    /**
     * Cria uma nova tarefa.
     *
     * @param id identificador da tarefa
     * @param tempoExecucao tempo total de execucao em milissegundos
     * @param prioridade prioridade da tarefa
     * @param idsDependencias tarefas que precisam concluir antes desta
     */
    public Tarefa(
            int id,
            long tempoExecucao,
            int prioridade,
            List<Integer> idsDependencias) {

        // Guarda o ID recebido no atributo da classe.
        this.id = id;

        // No inicio, todo o tempo de execucao ainda esta restante.
        this.tempoExecucaoRestante = tempoExecucao;

        // Guarda a prioridade recebida.
        this.prioridade = prioridade;

        // Se nao houver dependencias, a tarefa ja pode ir para a fila global.
        // Se houver dependencias, ela fica bloqueada ate elas terminarem.
        this.status = idsDependencias.isEmpty() ? StatusTarefa.PRONTA : StatusTarefa.BLOQUEADA;

        // Cria uma copia da lista para proteger o atributo interno da classe.
        this.idsDependencias = new ArrayList<>(idsDependencias);

        // Registra o horario de chegada para calcular espera e tempo total.
        this.tempoChegada = System.currentTimeMillis();
    }

    /**
     * Retorna o ID da tarefa.
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna a prioridade da tarefa.
     */
    public int getPrioridade() {
        return prioridade;
    }

    /**
     * Retorna uma copia das dependencias.
     *
     * A copia impede que outra classe altere diretamente a lista interna.
     */
    public List<Integer> getIdsDependencias() {
        return new ArrayList<>(idsDependencias);
    }

    /**
     * Retorna o status atual.
     *
     * O synchronized evita leitura inconsistente quando varias threads acessam
     * a mesma tarefa.
     */
    public synchronized StatusTarefa getStatus() {
        return status;
    }

    /**
     * Altera o status da tarefa de forma sincronizada.
     */
    public synchronized void setStatus(StatusTarefa status) {
        this.status = status;
    }

    /**
     * Retorna o tempo de execucao restante.
     */
    public synchronized long getTempoExecucaoRestante() {
        return tempoExecucaoRestante;
    }

    /**
     * Desconta do tempo restante o tempo que ja foi executado.
     */
    public synchronized void atualizarTempoExecucao(long tempoDecorrido) {
        // Subtrai o tempo processado do tempo restante.
        this.tempoExecucaoRestante -= tempoDecorrido;

        // Garante que o tempo restante nunca fique negativo.
        if (this.tempoExecucaoRestante < 0) {
            this.tempoExecucaoRestante = 0;
        }
    }

    /**
     * Remove uma dependencia quando uma tarefa anterior termina.
     */
    public synchronized void removerDependencia(int idConcluido) {
        idsDependencias.remove(Integer.valueOf(idConcluido));
    }

    /**
     * Retorna o horario em que a tarefa chegou ao sistema.
     */
    public long getTempoChegada() {
        return tempoChegada;
    }

    /**
     * Retorna o horario em que a tarefa iniciou a execucao.
     */
    public synchronized long getTempoInicioExecucao() {
        return tempoInicioExecucao;
    }

    /**
     * Define o horario em que a tarefa iniciou a execucao.
     */
    public synchronized void setTempoInicioExecucao(long tempoInicioExecucao) {
        this.tempoInicioExecucao = tempoInicioExecucao;
    }

    /**
     * Retorna o horario em que a tarefa terminou a execucao.
     */
    public synchronized long getTempoFimExecucao() {
        return tempoFimExecucao;
    }

    /**
     * Define o horario em que a tarefa terminou a execucao.
     */
    public synchronized void setTempoFimExecucao(long tempoFimExecucao) {
        this.tempoFimExecucao = tempoFimExecucao;
    }

    /**
     * Calcula quanto tempo a tarefa esperou ate comecar a executar.
     */
    public synchronized long getTempoEspera() {
        // Se a tarefa ainda nao iniciou, nao existe tempo de espera completo.
        if (tempoInicioExecucao == 0) {
            return 0;
        }

        // Espera = inicio da execucao - chegada ao sistema.
        return tempoInicioExecucao - tempoChegada;
    }

    /**
     * Calcula quanto tempo a tarefa ficou em processamento.
     */
    public synchronized long getTempoProcessamento() {
        // Se nao iniciou ou nao terminou, ainda nao ha processamento completo.
        if (tempoInicioExecucao == 0 || tempoFimExecucao == 0) {
            return 0;
        }

        // Processamento = fim - inicio.
        return tempoFimExecucao - tempoInicioExecucao;
    }

    /**
     * Calcula o tempo total da tarefa dentro do sistema.
     */
    public synchronized long getTempoTotalNoSistema() {
        // Se ainda nao terminou, o tempo total fechado ainda nao existe.
        if (tempoFimExecucao == 0) {
            return 0;
        }

        // Tempo total = conclusao - chegada.
        return tempoFimExecucao - tempoChegada;
    }
}
