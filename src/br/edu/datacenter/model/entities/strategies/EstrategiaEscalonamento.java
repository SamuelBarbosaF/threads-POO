package br.edu.datacenter.model.entities.strategies;

import br.edu.datacenter.model.entities.Tarefa;
import java.util.List;

/**
 * ## EstrategiaEscalonamento
 *
 * Interface que define como uma estrategia de escalonamento deve funcionar.
 *
 * O escalonador usa esta interface sem precisar saber qual estrategia concreta
 * esta sendo usada.
 */
public interface EstrategiaEscalonamento {

    /**
     * Escolhe a proxima tarefa a partir da lista de tarefas disponiveis.
     */
    Tarefa selecionarProximaTarefa(List<Tarefa> tarefas);

    /**
     * Retorna o nome da estrategia.
     */
    String getNome();
}
