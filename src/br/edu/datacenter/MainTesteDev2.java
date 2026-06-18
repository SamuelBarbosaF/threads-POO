package br.edu.datacenter;

import br.edu.datacenter.model.entities.Servidor;
import br.edu.datacenter.model.entities.Tarefa;
import br.edu.datacenter.model.entities.core.GeradorDeCarga;
import br.edu.datacenter.model.entities.core.MotorSimulacao;

/**
 * ## MainTesteDev2
 *
 * Classe de teste simples para validar infraestrutura basica:
 * - motor;
 * - servidores;
 * - gerador de carga;
 * - envio manual alternado de tarefas para os servidores.
 */
public class MainTesteDev2 {

    /**
     * Ponto de entrada do teste.
     */
    public static void main(String[] args) {
        System.out.println("=== TESTE DE INFRAESTRUTURA (DEV 2) ===");

        // Cria o motor central da simulacao.
        MotorSimulacao motor = new MotorSimulacao();

        // Cria o primeiro servidor com capacidade para 5 tarefas na fila.
        Servidor servidor1 =
                new Servidor(
                        1,
                        5,
                        motor.getGerenciadorDependencias());

        // Cria o segundo servidor com a mesma capacidade.
        Servidor servidor2 =
                new Servidor(
                        2,
                        5,
                        motor.getGerenciadorDependencias());

        // Inicia o primeiro servidor em uma thread.
        new Thread(servidor1).start();

        // Inicia o segundo servidor em uma thread.
        new Thread(servidor2).start();

        // Cria um gerador que produz 10 tarefas.
        GeradorDeCarga gerador =
                new GeradorDeCarga(
                        motor,
                        10);

        // Inicia o gerador em uma thread.
        new Thread(gerador).start();

        // Cria uma thread anonima para simular um escalonador simples.
        new Thread(() -> {
            // Controla qual servidor recebera a proxima tarefa.
            int vez = 1;

            // Loop infinito para consumir tarefas da fila global.
            while (true) {
                try {
                    // Espera ate existir uma tarefa pronta.
                    Tarefa tarefa = motor.getFilaGlobalDeProntos().take();

                    // Envia alternadamente para servidor 1 e servidor 2.
                    if (vez == 1) {
                        servidor1.adicionarTarefa(tarefa);
                        vez = 2;
                    } else {
                        servidor2.adicionarTarefa(tarefa);
                        vez = 1;
                    }
                } catch (InterruptedException e) {
                    // Restaura o estado de interrupcao e encerra a thread.
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
}
