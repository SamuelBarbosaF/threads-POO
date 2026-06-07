package br.edu.datacenter.model.entities.core;

import br.edu.datacenter.model.entities.Tarefa;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class MotorSimulacao {
    
    // 1. FILA THREAD-SAFE: Onde as tarefas prontas aguardam o Escalonador (Dev 3)
    private final BlockingQueue<Tarefa> filaGlobalDeProntos;
    
    // 2. LISTA THREAD-SAFE: Para a Interface Gráfica (Dev 1) ler sem dar erro de concorrência
    private final List<Tarefa> todasAsTarefasDoSistema;
    
    private final GerenciadorDeDependencias gerenciadorDependencias;

    public MotorSimulacao() {
        // Inicializa a Fila Global de forma segura (sem limite de tamanho)
        this.filaGlobalDeProntos = new LinkedBlockingQueue<>();
        
        // Inicializa a lista do FrontEnd com CopyOnWriteArrayList (perfeita para leituras frequentes)
        this.todasAsTarefasDoSistema = new CopyOnWriteArrayList<>();
        
        // Passa a fila global para o gerenciador, pois quando ele liberar uma tarefa, ela cai lá
        this.gerenciadorDependencias = new GerenciadorDeDependencias(this.filaGlobalDeProntos);
    }

    public BlockingQueue<Tarefa> getFilaGlobalDeProntos() {
        return filaGlobalDeProntos;
    }

    public List<Tarefa> getTodasAsTarefasDoSistema() {
        return todasAsTarefasDoSistema;
    }

    public GerenciadorDeDependencias getGerenciadorDependencias() {
        return gerenciadorDependencias;
    }

    // Método central seguro para injetar uma nova tarefa no Data Center
    public void registrarNovaTarefa(Tarefa novaTarefa) {
        // Adiciona na lista visual para o Dev 1
        todasAsTarefasDoSistema.add(novaTarefa);
        
        // Regras de roteamento baseadas no status
        switch (novaTarefa.getStatus()) {
            case BLOQUEADA:
                gerenciadorDependencias.adicionarTarefaBloqueada(novaTarefa);
                System.out.println("[Motor] Tarefa " + novaTarefa.getId() + " retida (BLOQUEADA).");
                break;
            case PRONTA:
                filaGlobalDeProntos.offer(novaTarefa);
                System.out.println("[Motor] Tarefa " + novaTarefa.getId() + " enviada para a Fila Global (PRONTA).");
                break;
            default:
                break;
        }
    }
}