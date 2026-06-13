package br.edu.datacenter.model.entities;

import br.edu.datacenter.model.entities.core.GerenciadorDeDependencias;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * ## Servidor
 *
 * Representa uma maquina do data center.
 *
 * A classe implementa Runnable, entao cada servidor pode ser executado em uma
 * Thread propria. Assim, varios servidores podem processar tarefas ao mesmo
 * tempo.
 */
public class Servidor implements Runnable {

    // Identificador do servidor.
    private final int id;

    // Quantidade maxima de tarefas que a fila local aceita.
    private final int capacidadeMaxima;

    // Fila local do servidor. BlockingQueue e segura para uso com threads.
    private final BlockingQueue<Tarefa> filaLocal;

    // Guarda os IDs das tarefas ja executadas por este servidor.
    private final List<Integer> historicoUso;

    // Gerenciador avisado quando uma tarefa termina.
    private final GerenciadorDeDependencias gerenciadorDependencias;

    // Soma do tempo gasto processando tarefas.
    private long tempoTotalProcessamento;

    // Quantidade de tarefas processadas.
    private int tarefasProcessadas;

    // Controla se o loop principal da thread continua rodando.
    private volatile boolean rodando;

    // Indica se o servidor esta processando uma tarefa neste momento.
    private volatile boolean ocupado;

    /**
     * Cria um servidor com ID, capacidade de fila e gerenciador de dependencias.
     */
    public Servidor(
            int id,
            int capacidadeMaxima,
            GerenciadorDeDependencias gerenciadorDependencias) {

        // Salva o ID recebido.
        this.id = id;

        // Salva o limite da fila local.
        this.capacidadeMaxima = capacidadeMaxima;

        // Cria uma fila bloqueante com capacidade maxima.
        this.filaLocal = new LinkedBlockingQueue<>(capacidadeMaxima);

        // Inicia o historico vazio.
        this.historicoUso = new ArrayList<>();

        // Servidor nasce ligado.
        this.rodando = true;

        // Servidor nasce sem tarefa em execucao.
        this.ocupado = false;

        // Guarda a referencia para avisar conclusoes.
        this.gerenciadorDependencias = gerenciadorDependencias;

        // Inicia as metricas zeradas.
        this.tempoTotalProcessamento = 0;
        this.tarefasProcessadas = 0;
    }

    /**
     * Retorna o ID do servidor.
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna a capacidade maxima da fila local.
     */
    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }

    /**
     * Retorna a fila local do servidor.
     */
    public BlockingQueue<Tarefa> getFilaLocal() {
        return filaLocal;
    }

    /**
     * Informa se o servidor esta ocupado.
     */
    public boolean isOcupado() {
        return ocupado;
    }

    /**
     * Retorna uma copia do historico.
     *
     * A copia evita que outra classe altere diretamente a lista interna.
     */
    public synchronized List<Integer> getHistoricoUso() {
        return new ArrayList<>(historicoUso);
    }

    /**
     * Retorna o tempo total ja gasto processando tarefas.
     */
    public synchronized long getTempoTotalProcessamento() {
        return tempoTotalProcessamento;
    }

    /**
     * Retorna quantas tarefas o servidor concluiu.
     */
    public synchronized int getTarefasProcessadas() {
        return tarefasProcessadas;
    }

    /**
     * Calcula a carga atual do servidor.
     *
     * Carga = tarefas esperando na fila + 1 se o servidor estiver ocupado.
     */
    public synchronized int getCargaAtual() {
        return filaLocal.size() + (ocupado ? 1 : 0);
    }

    /**
     * Tenta adicionar uma tarefa na fila local.
     *
     * @return true se conseguiu adicionar, false se a fila estava cheia
     */
    public boolean adicionarTarefa(Tarefa tarefa) {
        return filaLocal.offer(tarefa);
    }

    /**
     * Solicita o desligamento do servidor.
     */
    public void desligar() {
        this.rodando = false;
    }

    /**
     * Metodo executado pela Thread do servidor.
     *
     * Enquanto o servidor estiver rodando, ele espera uma tarefa na fila local,
     * processa essa tarefa e atualiza as metricas.
     */
    @Override
    public void run() {
        System.out.println("[Servidor " + id + "] Ligado. Monitorando fila local...");

        // Loop principal da thread.
        while (rodando) {
            try {
                // take() bloqueia a thread enquanto a fila estiver vazia.
                Tarefa tarefa = filaLocal.take();

                // Marca o servidor como ocupado.
                ocupado = true;

                // Registra o inicio da execucao.
                System.out.println("[Servidor " + id + "] Iniciou execucao da Tarefa " + tarefa.getId());
                tarefa.setTempoInicioExecucao(System.currentTimeMillis());
                tarefa.setStatus(StatusTarefa.EXECUTANDO);

                // Pega o tempo que a tarefa precisa executar.
                long tempoDeExecucao = tarefa.getTempoExecucaoRestante();

                // Simula o processamento real fazendo a thread dormir.
                Thread.sleep(tempoDeExecucao);

                // Atualiza a tarefa como finalizada.
                tarefa.atualizarTempoExecucao(tempoDeExecucao);
                tarefa.setTempoFimExecucao(System.currentTimeMillis());
                tarefa.setStatus(StatusTarefa.CONCLUIDA);

                // Atualiza historico e metricas com protecao de concorrencia.
                synchronized (this) {
                    historicoUso.add(tarefa.getId());
                    tempoTotalProcessamento += tempoDeExecucao;
                    tarefasProcessadas++;
                }

                System.out.println("[Servidor " + id + "] Tarefa " + tarefa.getId() + " CONCLUIDA.");

                // Avisa que uma tarefa terminou para liberar possiveis dependentes.
                if (gerenciadorDependencias != null) {
                    gerenciadorDependencias.notificarConclusao(tarefa);
                }

                // Servidor volta a ficar livre.
                ocupado = false;
            } catch (InterruptedException e) {
                // Se a thread for interrompida, restaura o estado de interrupcao.
                System.out.println("[Servidor " + id + "] Interrompido de forma inesperada.");
                Thread.currentThread().interrupt();

                // Sai do loop principal.
                break;
            }
        }

        System.out.println("[Servidor " + id + "] Totalmente Desligado.");
    }

    /**
     * Calcula o tempo medio de processamento deste servidor.
     */
    public synchronized double getTempoMedioProcessamento() {
        // Evita divisao por zero.
        if (tarefasProcessadas == 0) {
            return 0;
        }

        // Media = tempo total / quantidade de tarefas.
        return (double) tempoTotalProcessamento / tarefasProcessadas;
    }
}
