package br.edu.datacenter.model.entities.core;

import br.edu.datacenter.model.entities.StatusTarefa;
import br.edu.datacenter.model.entities.Tarefa;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class GerenciadorDeDependencias {
    
    // Lista de tarefas que ainda aguardam outras terminarem
    private final List<Tarefa> tarefasBloqueadas;
    
    // Fila onde as tarefas caem assim que ficam prontas (O Dev 3 vai usar isso)
    private final BlockingQueue<Tarefa> filaGlobalDeProntos;

    public GerenciadorDeDependencias(BlockingQueue<Tarefa> filaGlobalDeProntos) {
        this.tarefasBloqueadas = new ArrayList<>();
        this.filaGlobalDeProntos = filaGlobalDeProntos;
    }

    // Sincronizado para garantir segurança ao adicionar novas tarefas bloqueadas
    public synchronized void adicionarTarefaBloqueada(Tarefa tarefa) {
        tarefasBloqueadas.add(tarefa);
    }

    // O coração do mecanismo: Sincronizado para evitar Condição de Corrida (Race Condition)
    // entre vários Servidores tentando avisar que terminaram.
    public synchronized void notificarConclusao(Tarefa tarefaConcluida) {
        int idConcluido = tarefaConcluida.getId();
        
        // Usamos Iterator porque vamos remover elementos da lista enquanto iteramos nela.
        // Fazer isso com um `for` normal causaria um erro (ConcurrentModificationException).
        Iterator<Tarefa> iterator = tarefasBloqueadas.iterator();
        
        while (iterator.hasNext()) {
            Tarefa tarefaBloqueada = iterator.next();
            
            // Remove a dependência recém concluída
            tarefaBloqueada.removerDependencia(idConcluido);
            
            // Verifica se esvaziou as dependências
            if (tarefaBloqueada.getIdsDependencias().isEmpty()) {
                
                // 1. Muda o status para PRONTA
                tarefaBloqueada.setStatus(StatusTarefa.PRONTA);
                
                // 2. Tira da lista de bloqueadas
                iterator.remove(); 
                
                // 3. Joga na fila para o Escalonador (Dev 3) usar
                filaGlobalDeProntos.offer(tarefaBloqueada);
                
                System.out.println("[Gerenciador] Tarefa " + tarefaBloqueada.getId() + 
                                   " resolveu suas dependências e agora está PRONTA!");
            }
        }
    }
}
