package br.edu.datacenter.model.entities;

import java.util.ArrayList;
import java.util.List;

public class Tarefa {
    private final int id;
    private final int prioridade;
    private final List<Integer> idsDependencias; // IDs das tarefas que precisam terminar antes desta
    
    // Variáveis que mudam constantemente precisam de atenção à concorrência
    private long tempoExecucaoRestante; 
    private StatusTarefa status;

    public Tarefa(int id, long tempoExecucao, int prioridade, List<Integer> idsDependencias) {
        this.id = id;
        this.tempoExecucaoRestante = tempoExecucao;
        this.prioridade = prioridade;
        // Se não houver dependências, ela já nasce PRONTA, caso contrário, nasce BLOQUEADA
        this.status = idsDependencias.isEmpty() ? StatusTarefa.PRONTA : StatusTarefa.BLOQUEADA;
        this.idsDependencias = new ArrayList<>(idsDependencias); 
    }

    // Getters de variáveis imutáveis não precisam de sincronização
    public int getId() { return id; }
    public int getPrioridade() { return prioridade; }
    
    // retornar uma nova lista para ninguém alterar a lista interna de fora da classe
    public List<Integer> getIdsDependencias() { 
        return new ArrayList<>(idsDependencias); 
    }

    // Getters e Setters Sincronizados garantem consistência entre múltiplas Threads
    public synchronized StatusTarefa getStatus() { 
        return status; 
    }
    
    public synchronized void setStatus(StatusTarefa status) { 
        this.status = status; 
    }

    public synchronized long getTempoExecucaoRestante() { 
        return tempoExecucaoRestante; 
    }

    public synchronized void atualizarTempoExecucao(long tempoDecorrido) {
        this.tempoExecucaoRestante -= tempoDecorrido;
        if (this.tempoExecucaoRestante < 0) {
            this.tempoExecucaoRestante = 0;
        }
    }

    public synchronized void removerDependencia(int idConcluido) {
        idsDependencias.remove(Integer.valueOf(idConcluido));
    }
}