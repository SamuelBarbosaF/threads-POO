package br.edu.datacenter.model.entities.core;

import br.edu.datacenter.model.entities.StatusTarefa;
import br.edu.datacenter.model.entities.Tarefa;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

/**
 * ## GerenciadorDeDependencias
 * Controla as tarefas que ainda nao podem executar porque dependem de outras.
 */
public class GerenciadorDeDependencias {

    // Lista de tarefas aguardando conclusao de dependencias.
    private final List<Tarefa> tarefasBloqueadas;

    // Fila para onde uma tarefa vai quando todas as dependencias terminam.
    private final BlockingQueue<Tarefa> filaGlobalDeProntos;

    // IDs de tarefas ja concluidas.
    private final Set<Integer> idsConcluidos = new HashSet<>();

    /**
     * Recebe a fila global para conseguir liberar tarefas depois.
     */
    public GerenciadorDeDependencias(BlockingQueue<Tarefa> filaGlobalDeProntos) {
        // Inicia sem tarefas bloqueadas.
        this.tarefasBloqueadas = new ArrayList<>();

        // Guarda a fila global compartilhada.
        this.filaGlobalDeProntos = filaGlobalDeProntos;
    }

    /**
     * Adiciona uma tarefa na lista de bloqueadas.
     * Antes de bloquear de fato, remove qualquer dependencia que ja tenha sido concluida
     * antes deste registro acontecer.
     */
    public synchronized void adicionarTarefaBloqueada(Tarefa tarefa) {
        for (int idJaConcluido : idsConcluidos) {
            tarefa.removerDependencia(idJaConcluido);
        }

        if (tarefa.getIdsDependencias().isEmpty()) {
            tarefa.setStatus(StatusTarefa.PRONTA);
            filaGlobalDeProntos.offer(tarefa);
            System.out.println(
                    "[Gerenciador] Tarefa "
                            + tarefa.getId()
                            + " ja tinha todas as dependencias concluidas no momento do registro - liberada direto.");
            return;
        }

        tarefasBloqueadas.add(tarefa);
    }

    /**
     * Notifica que uma tarefa terminou.
     * Com isso, o gerenciador tenta remover essa dependencia das tarefas
     * bloqueadas. Se alguma ficar sem dependencias, ela vira PRONTA.
     */
    public synchronized void notificarConclusao(Tarefa tarefaConcluida) {
        // Pega o ID da tarefa que acabou.
        int idConcluido = tarefaConcluida.getId();

        // Registra a conclusao para que tarefas registradas DEPOIS deste ponto (mas que
        // dependiam dela) possam ser liberadas direto em adicionarTarefaBloqueada.
        idsConcluidos.add(idConcluido);

        // Iterator permite remover itens durante a iteracao.
        Iterator<Tarefa> iterator = tarefasBloqueadas.iterator();

        // Percorre todas as tarefas bloqueadas.
        while (iterator.hasNext()) {
            // Pega a proxima tarefa bloqueada.
            Tarefa tarefaBloqueada = iterator.next();

            // Remove a dependencia que acabou de ser concluida.
            tarefaBloqueada.removerDependencia(idConcluido);

            // Se nao restam dependencias, a tarefa pode ser liberada.
            if (tarefaBloqueada.getIdsDependencias().isEmpty()) {
                // Atualiza o status da tarefa.
                tarefaBloqueada.setStatus(StatusTarefa.PRONTA);

                // Remove da lista de bloqueadas.
                iterator.remove();

                // Coloca na fila global para o escalonador usar.
                filaGlobalDeProntos.offer(tarefaBloqueada);

                System.out.println(
                        "[Gerenciador] Tarefa "
                                + tarefaBloqueada.getId()
                                + " resolveu suas dependencias e agora esta PRONTA!");
            }
        }
    }
}