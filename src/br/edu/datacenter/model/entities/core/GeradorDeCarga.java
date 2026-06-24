package br.edu.datacenter.model.entities.core;

import br.edu.datacenter.model.entities.Tarefa;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ## GeradorDeCarga
 *
 * Cria tarefas automaticamente para alimentar a simulacao.
 */
public class GeradorDeCarga implements Runnable {

    // Motor que recebe as tarefas criadas.
    private final MotorSimulacao motorSimulacao;

    // Gerador de numeros aleatorios para tempos, prioridades e dependencias.
    private final Random random;

    // Quantidade maxima de tarefas a gerar. Zero significa infinito.
    private final int maxTarefas;

    // Menor intervalo entre uma tarefa e outra.
    private final int intervaloMinimoMs;

    // Maior intervalo entre uma tarefa e outra.
    private final int intervaloMaximoMs;

    // Proximo ID que sera usado em uma tarefa.
    private int contadorId;

    // Controla o loop principal do gerador.
    private volatile boolean ativo;

    /**
     * Cria um gerador ligado a um motor e com limite de tarefas.
     */
    public GeradorDeCarga(MotorSimulacao motorSimulacao, int maxTarefas) {
        // Guarda o motor que recebera as tarefas.
        this.motorSimulacao = motorSimulacao;

        // Guarda o limite de tarefas.
        this.maxTarefas = maxTarefas;

        // O gerador nasce ativo.
        this.ativo = true;

        // Cria o gerador de aleatoriedade.
        this.random = new Random();

        // A primeira tarefa tera ID 1.
        this.contadorId = 1;

        // Define intervalo minimo de 0,5 segundo.
        this.intervaloMinimoMs = 100;

        // Define intervalo maximo de 2 segundos.
        this.intervaloMaximoMs = 500;
    }

    /**
     * Solicita que o gerador pare de criar tarefas.
     */
    public void desligar() {
        this.ativo = false;
    }

    /**
     * Metodo executado pela thread do gerador.
     */
    @Override
    public void run() {
        System.out.println("[Gerador] Iniciando injecao de tarefas (Trafego Dinamico)...");

        // Continua enquanto estiver ativo e ainda nao tiver atingido o limite.
        while (ativo && (maxTarefas == 0 || contadorId <= maxTarefas)) {
            try {
                // Sorteia o tempo de espera ate criar a proxima tarefa.
                int tempoEspera =
                        intervaloMinimoMs
                        + random.nextInt(intervaloMaximoMs - intervaloMinimoMs);

                // Simula o intervalo entre chegadas de tarefas.
                Thread.sleep(tempoEspera);

                // Define o ID e avanca o contador para a proxima tarefa.
                int id = contadorId++;

                // Sorteia tempo de execucao entre 1 e 6 segundos.
                long tempoExecucao = 1000 + random.nextInt(5000);

                // Sorteia prioridade entre 1 e 5.
                int prioridade = 1 + random.nextInt(5);

                // Comeca sem dependencias.
                List<Integer> dependencias = new ArrayList<>();

                // Em 30% dos casos, cria dependencia da tarefa anterior.
                if (id > 1 && random.nextInt(100) < 30) {
                    dependencias.add(id - 1);

                    System.out.println(
                            "[Gerador] Nova tarefa "
                            + id
                            + " criada. DEPENDENCIA: Aguarda a tarefa "
                            + (id - 1));
                }

                // Cria a tarefa com os dados sorteados.
                Tarefa novaTarefa =
                        new Tarefa(
                                id,
                                tempoExecucao,
                                prioridade,
                                dependencias);

                // Entrega a tarefa para o motor decidir o destino.
                motorSimulacao.registrarNovaTarefa(novaTarefa);
            } catch (InterruptedException e) {
                // Interrupcao geralmente acontece quando a simulacao e parada.
                System.out.println("[Gerador] Foi interrompido antes de finalizar a geracao.");
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[Gerador] Finalizou a geracao de tarefas. Total gerado: " + (contadorId - 1));
    }
}
