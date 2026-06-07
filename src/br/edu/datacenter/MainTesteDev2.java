package br.edu.datacenter;

import br.edu.datacenter.model.entities.core.MotorSimulacao;
import br.edu.datacenter.model.entities.core.GeradorDeCarga;
import br.edu.datacenter.model.entities.Servidor;
import br.edu.datacenter.model.entities.Tarefa;

public class MainTesteDev2 {
    public static void main(String[] args) {
        
        System.out.println("=== TESTE DE INFRAESTRUTURA (DEV 2) ===");

        // 1. Inicia o Motor Compartilhado (Fila Global + Dependências)
        MotorSimulacao motor = new MotorSimulacao();

        // 2. Inicia 2 Servidores (Eles vão ficar esperando tarefas caírem na fila deles)
        Servidor s1 = new Servidor(1, 5, motor.getGerenciadorDependencias());
        Servidor s2 = new Servidor(2, 5, motor.getGerenciadorDependencias());
        
        new Thread(s1).start();
        new Thread(s2).start();

        // 3. Inicia o Gerador de Carga (Vai gerar 10 tarefas dinamicamente)
        GeradorDeCarga gerador = new GeradorDeCarga(motor, 10);
        new Thread(gerador).start();

        // 4. "Falso Escalonador" só para testar
        // Como o Dev 3 ainda não fez o Escalonador inteligente, vamos fazer uma Thread boba
        // que tira da Fila Global e joga pro Servidor 1 ou 2 de forma alternada (Round-Robin burro) só para ver rodando.
        new Thread(() -> {
            int vez = 1;
            while (true) {
                try {
                    // Pega da fila global (congela se estiver vazia)
                    Tarefa t = motor.getFilaGlobalDeProntos().take(); 
                    
                    if (vez == 1) {
                        s1.adicionarTarefa(t);
                        vez = 2;
                    } else {
                        s2.adicionarTarefa(t);
                        vez = 1;
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
}