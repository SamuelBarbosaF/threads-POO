package br.edu.datacenter.model.entities.core;

import br.edu.datacenter.model.entities.Tarefa;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * ## MotorSimulacao
 *
 * Classe central da simulacao.
 *
 * Ela concentra:
 * - a fila global de tarefas prontas;
 * - a lista com todas as tarefas criadas;
 * - o gerenciador de dependencias.
 */
public class MotorSimulacao {

    // Fila compartilhada onde ficam tarefas prontas para o escalonador.
    private final BlockingQueue<Tarefa> filaGlobalDeProntos;

    // Lista com todas as tarefas do sistema, inclusive bloqueadas e concluidas.
    private final List<Tarefa> todasAsTarefasDoSistema;

    // Objeto responsavel por controlar tarefas bloqueadas por dependencias.
    private final GerenciadorDeDependencias gerenciadorDependencias;

    /**
     * Inicializa as estruturas principais da simulacao.
     */
    public MotorSimulacao() {
        // LinkedBlockingQueue permite acesso seguro por multiplas threads.
        this.filaGlobalDeProntos = new LinkedBlockingQueue<>();

        // CopyOnWriteArrayList e segura para leituras enquanto outras threads escrevem.
        this.todasAsTarefasDoSistema = new CopyOnWriteArrayList<>();

        // O gerenciador recebe a fila global para liberar tarefas quando ficarem prontas.
        this.gerenciadorDependencias = new GerenciadorDeDependencias(filaGlobalDeProntos);
    }

    /**
     * Retorna a fila global de tarefas prontas.
     */
    public BlockingQueue<Tarefa> getFilaGlobalDeProntos() {
        return filaGlobalDeProntos;
    }

    /**
     * Retorna a lista com todas as tarefas conhecidas pelo sistema.
     */
    public List<Tarefa> getTodasAsTarefasDoSistema() {
        return todasAsTarefasDoSistema;
    }

    /**
     * Retorna o gerenciador de dependencias.
     */
    public GerenciadorDeDependencias getGerenciadorDependencias() {
        return gerenciadorDependencias;
    }

    /**
     * Registra uma nova tarefa no sistema.
     *
     * Se a tarefa estiver pronta, ela entra na fila global.
     * Se estiver bloqueada, ela vai para o gerenciador de dependencias.
     */
    public void registrarNovaTarefa(Tarefa novaTarefa) {
        // Toda tarefa fica registrada para metricas e visualizacao.
        todasAsTarefasDoSistema.add(novaTarefa);

        // Decide o destino inicial da tarefa com base no status.
        switch (novaTarefa.getStatus()) {
            case BLOQUEADA:
                // Tarefas bloqueadas aguardam suas dependencias serem concluidas.
                gerenciadorDependencias.adicionarTarefaBloqueada(novaTarefa);
                System.out.println("[Motor] Tarefa " + novaTarefa.getId() + " retida (BLOQUEADA).");
                break;
            case PRONTA:
                // Tarefas prontas ja podem ser escolhidas pelo escalonador.
                filaGlobalDeProntos.offer(novaTarefa);
                System.out.println("[Motor] Tarefa " + novaTarefa.getId() + " enviada para a Fila Global (PRONTA).");
                break;
            default:
                // Outros estados nao devem acontecer no registro inicial.
                break;
        }
    }
}
