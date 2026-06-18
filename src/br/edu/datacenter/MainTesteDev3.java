package br.edu.datacenter;

import br.edu.datacenter.model.entities.Servidor;
import br.edu.datacenter.model.entities.Tarefa;
import br.edu.datacenter.model.entities.balancer.BalanceadorMenorFila;
import br.edu.datacenter.model.entities.core.Escalonador;
import br.edu.datacenter.model.entities.core.GerenciadorDeDependencias;
import br.edu.datacenter.model.entities.metrics.ColetorDeMetricas;
import br.edu.datacenter.model.entities.strategies.EscalonamentoPrioridadeAging;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * ## MainTesteDev3
 *
 * Classe de teste mais completa.
 *
 * Ela monta:
 * - fila global;
 * - gerenciador de dependencias;
 * - tres servidores;
 * - escalonador real;
 * - tarefas com e sem dependencias;
 * - impressao de metricas ao final.
 */
public class MainTesteDev3 {

    /**
     * Ponto de entrada do teste.
     */
    public static void main(String[] args)
            throws InterruptedException {

        // Cabecalho visual do teste.
        System.out.println("=================================");
        System.out.println("TESTE DEV 3");
        System.out.println("=================================");

        // Fila global onde ficam tarefas prontas.
        BlockingQueue<Tarefa> filaGlobal =
                new LinkedBlockingQueue<>();

        // Gerenciador que libera tarefas bloqueadas quando dependencias terminam.
        GerenciadorDeDependencias gerenciador =
                new GerenciadorDeDependencias(filaGlobal);

        // Lista onde os servidores criados serao guardados.
        List<Servidor> servidores =
                new ArrayList<>();

        // Cria tres servidores.
        for (int i = 1; i <= 3; i++) {
            // Cada servidor tem capacidade local para 10 tarefas.
            Servidor servidor =
                    new Servidor(
                            i,
                            10,
                            gerenciador);

            // Guarda o servidor na lista.
            servidores.add(servidor);

            // Cria uma thread para o servidor.
            Thread threadServidor =
                    new Thread(servidor);

            // Nomeia e inicia a thread.
            threadServidor.setName("Servidor-" + i);
            threadServidor.start();
        }

        // Cria o escalonador com estrategia de prioridade com aging.
        Escalonador escalonador =
                new Escalonador(
                        filaGlobal,
                        servidores,
                        new EscalonamentoPrioridadeAging(),
                        new BalanceadorMenorFila());

        // Coloca o escalonador em uma thread.
        Thread threadEscalonador =
                new Thread(escalonador);

        // Inicia o escalonador.
        threadEscalonador.start();

        // Lista geral usada depois pelo coletor de metricas.
        List<Tarefa> todasTarefas =
                new ArrayList<>();

        // Tarefa pronta, sem dependencias.
        Tarefa tarefa1 =
                new Tarefa(
                        1,
                        5000,
                        3,
                        List.of());

        // Tarefa pronta, mais curta.
        Tarefa tarefa2 =
                new Tarefa(
                        2,
                        2000,
                        2,
                        List.of());

        // Tarefa pronta, prioridade mais alta.
        Tarefa tarefa3 =
                new Tarefa(
                        3,
                        3000,
                        1,
                        List.of());

        // Tarefa bloqueada, depende da tarefa 1.
        Tarefa tarefa4 =
                new Tarefa(
                        4,
                        1000,
                        2,
                        List.of(1));

        // Tarefa bloqueada, depende da tarefa 3.
        Tarefa tarefa5 =
                new Tarefa(
                        5,
                        4000,
                        4,
                        List.of(3));

        // Guarda todas as tarefas para calculo de metricas.
        todasTarefas.add(tarefa1);
        todasTarefas.add(tarefa2);
        todasTarefas.add(tarefa3);
        todasTarefas.add(tarefa4);
        todasTarefas.add(tarefa5);

        // Insere tarefas prontas diretamente na fila global.
        filaGlobal.add(tarefa1);
        filaGlobal.add(tarefa2);
        filaGlobal.add(tarefa3);

        // Insere tarefas com dependencias no gerenciador.
        gerenciador.adicionarTarefaBloqueada(tarefa4);
        gerenciador.adicionarTarefaBloqueada(tarefa5);

        System.out.println("Tarefas inseridas.");

        // Aguarda 20 segundos para as threads processarem as tarefas.
        Thread.sleep(20000);

        // Cria o coletor de metricas.
        ColetorDeMetricas coletor =
                new ColetorDeMetricas(
                        todasTarefas,
                        servidores);

        // Imprime metricas gerais.
        System.out.println("\n========== METRICAS ==========");

        System.out.println(
                "Concluidas: "
                + coletor.getQuantidadeConcluidas());

        System.out.println(
                "Tempo Medio Espera: "
                + coletor.calcularTempoMedioEspera()
                + " ms");

        System.out.println(
                "Tempo Medio Processamento: "
                + coletor.calcularTempoMedioProcessamento()
                + " ms");

        System.out.println(
                "Vazao: "
                + coletor.calcularVazao()
                + " tarefas/min");

        // Tempo total fixo usado neste teste.
        long tempoSimulacao = 20000;

        System.out.println(
                "\nUtilizacao Media: "
                + coletor.calcularUtilizacaoMedia(tempoSimulacao)
                + "%");

        // Imprime dados individuais de cada servidor.
        System.out.println("\n===== SERVIDORES =====");

        for (Servidor servidor : servidores) {
            System.out.println("\nServidor " + servidor.getId());

            System.out.println(
                    "Tarefas processadas: "
                    + servidor.getTarefasProcessadas());

            System.out.println(
                    "Tempo processamento: "
                    + servidor.getTempoTotalProcessamento()
                    + " ms");

            System.out.println(
                    "Fila atual: "
                    + servidor.getFilaLocal().size());

            System.out.println(
                    "Historico: "
                    + servidor.getHistoricoUso());
        }

        // Solicita encerramento do escalonador.
        escalonador.desligar();

        // Solicita encerramento dos servidores.
        for (Servidor servidor : servidores) {
            servidor.desligar();
        }

        // Interrompe o escalonador caso ele esteja dormindo.
        threadEscalonador.interrupt();

        System.out.println("\nTeste finalizado.");
    }
}
