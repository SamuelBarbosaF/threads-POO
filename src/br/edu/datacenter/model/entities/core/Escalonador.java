package br.edu.datacenter.model.entities.core;

import br.edu.datacenter.model.entities.Servidor;
import br.edu.datacenter.model.entities.Tarefa;
import br.edu.datacenter.model.entities.balancer.BalanceadorCarga;
import br.edu.datacenter.model.entities.strategies.EstrategiaEscalonamento;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * ## Escalonador
 *
 * Responsavel por mover tarefas da fila global para os servidores.
 *
 * Ele usa duas regras separadas:
 * - uma estrategia escolhe qual tarefa executar;
 * - um balanceador escolhe qual servidor deve receber essa tarefa.
 */
public class Escalonador implements Runnable {

    // Fila com tarefas prontas para execucao.
    private final BlockingQueue<Tarefa> filaGlobal;

    // Lista de servidores disponiveis no data center.
    private final List<Servidor> servidores;

    // Estrategia usada para escolher a proxima tarefa.
    private EstrategiaEscalonamento estrategia;

    // Balanceador usado para escolher o servidor.
    private BalanceadorCarga balanceador;

    // Controla o loop principal da thread.
    private volatile boolean ativo;

    /**
     * Cria um escalonador com fila, servidores, estrategia e balanceador.
     */
    public Escalonador(
            BlockingQueue<Tarefa> filaGlobal,
            List<Servidor> servidores,
            EstrategiaEscalonamento estrategia,
            BalanceadorCarga balanceador) {

        // Guarda a fila global recebida.
        this.filaGlobal = filaGlobal;

        // Guarda a lista de servidores.
        this.servidores = servidores;

        // Guarda a estrategia inicial.
        this.estrategia = estrategia;

        // Guarda o balanceador inicial.
        this.balanceador = balanceador;

        // O escalonador nasce ativo.
        this.ativo = true;
    }

    /**
     * Troca a estrategia de escalonamento.
     */
    public void setEstrategia(EstrategiaEscalonamento estrategia) {
        this.estrategia = estrategia;
    }

    /**
     * Troca o balanceador de carga.
     */
    public void setBalanceador(BalanceadorCarga balanceador) {
        this.balanceador = balanceador;
    }

    /**
     * Solicita o encerramento do escalonador.
     */
    public void desligar() {
        this.ativo = false;
    }

    /**
     * Metodo executado pela thread do escalonador.
     */
    @Override
    public void run() {
        System.out.println("[Escalonador] Iniciado utilizando estrategia: " + estrategia.getNome());

        // Loop principal: roda enquanto estiver ativo.
        while (ativo) {
            try {
                // Se nao ha tarefas prontas, espera um pouco e tenta novamente.
                if (filaGlobal.isEmpty()) {
                    Thread.sleep(100);
                    continue;
                }

                // Copia a fila para a estrategia analisar sem alterar a fila original.
                List<Tarefa> tarefasDisponiveis =
                        new ArrayList<>(filaGlobal);

                // Pede para a estrategia escolher a tarefa.
                Tarefa tarefaEscolhida =
                        estrategia.selecionarProximaTarefa(tarefasDisponiveis);

                // Se nenhuma tarefa foi escolhida, espera e tenta de novo.
                if (tarefaEscolhida == null) {
                    Thread.sleep(100);
                    continue;
                }

                // Pede para o balanceador escolher o servidor.
                Servidor servidorEscolhido =
                        balanceador.selecionarServidor(servidores);

                // Se nao houver servidor disponivel, espera e tenta novamente.
                if (servidorEscolhido == null) {
                    Thread.sleep(100);
                    continue;
                }

                // Tenta colocar a tarefa na fila local do servidor escolhido.
                boolean enviada =
                        servidorEscolhido.adicionarTarefa(tarefaEscolhida);

                if (enviada) {
                    // Remove da fila global apenas se entrou na fila do servidor.
                    filaGlobal.remove(tarefaEscolhida);

                    System.out.println(
                            "[Escalonador] Tarefa "
                            + tarefaEscolhida.getId()
                            + " enviada para Servidor "
                            + servidorEscolhido.getId());
                } else {
                    // Se a fila local estava cheia, espera antes de tentar de novo.
                    System.out.println(
                            "[Escalonador] Servidor "
                            + servidorEscolhido.getId()
                            + " esta com fila cheia.");

                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                // Interrupcao geralmente acontece no desligamento.
                System.out.println("[Escalonador] Interrompido.");
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[Escalonador] Finalizado.");
    }
}
