package br.edu.datacenter.model.entities.strategies;

import br.edu.datacenter.model.entities.StatusTarefa;
import br.edu.datacenter.model.entities.Tarefa;
import java.util.List;

/**
 * ## EscalonamentoPrioridadeAging
 *
 * Escolhe tarefas por prioridade, mas aplica aging.
 *
 * Aging aumenta a chance de tarefas antigas serem executadas, evitando que uma
 * tarefa fique esperando para sempre.
 */
public class EscalonamentoPrioridadeAging implements EstrategiaEscalonamento {

    // A cada 5 segundos esperando, a tarefa ganha bonus de prioridade.
    private static final long AGING_MS = 5000;

    /**
     * Seleciona a tarefa pronta com melhor prioridade efetiva.
     */
    @Override
    public Tarefa selecionarProximaTarefa(List<Tarefa> tarefas) {
        // Melhor tarefa encontrada ate agora.
        Tarefa melhor = null;

        // Horario atual usado para calcular quanto tempo cada tarefa esperou.
        long agora = System.currentTimeMillis();

        // Percorre todas as tarefas recebidas.
        for (Tarefa tarefa : tarefas) {
            // Ignora tarefas que ainda nao estao prontas.
            if (tarefa.getStatus() != StatusTarefa.PRONTA) {
                continue;
            }

            // A primeira tarefa pronta vira a melhor inicial.
            if (melhor == null) {
                melhor = tarefa;
                continue;
            }

            // Calcula a prioridade considerando aging da tarefa atual.
            int prioridadeAtual = calcularPrioridadeEfetiva(tarefa, agora);

            // Calcula a prioridade considerando aging da melhor tarefa atual.
            int prioridadeMelhor = calcularPrioridadeEfetiva(melhor, agora);

            // Numero menor representa maior prioridade.
            if (prioridadeAtual < prioridadeMelhor) {
                melhor = tarefa;
            } else if (prioridadeAtual == prioridadeMelhor
                    && tarefa.getTempoChegada() < melhor.getTempoChegada()) {
                // Em caso de empate, escolhe quem chegou primeiro.
                melhor = tarefa;
            }
        }

        // Retorna a melhor tarefa pronta encontrada.
        return melhor;
    }

    /**
     * Calcula a prioridade apos aplicar o bonus de aging.
     */
    private int calcularPrioridadeEfetiva(Tarefa tarefa, long agora) {
        // Tempo que a tarefa ja esperou na simulacao.
        long tempoEspera = agora - tarefa.getTempoChegada();

        // Bonus cresce conforme o tempo de espera.
        int bonus = (int) (tempoEspera / AGING_MS);

        // A prioridade nunca fica menor que 1.
        return Math.max(1, tarefa.getPrioridade() - bonus);
    }

    /**
     * Nome exibido para esta estrategia.
     */
    @Override
    public String getNome() {
        return "Prioridade com Aging";
    }
}
