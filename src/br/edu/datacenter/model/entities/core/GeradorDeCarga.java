package br.edu.datacenter.model.entities.core;

import br.edu.datacenter.model.entities.Tarefa;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeradorDeCarga implements Runnable {
    
    private final MotorSimulacao motorSimulacao;
    private volatile boolean ativo;
    private final Random random;
    
    // Controles de geração
    private int contadorId = 1;
    private final int maxTarefas; // Limite de tarefas a gerar (0 para infinito)
    private final int intervaloMinimoMs;
    private final int intervaloMaximoMs;

    /**
     * Construtor do Gerador de Carga
     * @param motorSimulacao O motor central onde as tarefas serão injetadas
     * @param maxTarefas Quantidade máxima de tarefas a gerar (use um número alto para simulação infinita)
     */
    public GeradorDeCarga(MotorSimulacao motorSimulacao, int maxTarefas) {
        this.motorSimulacao = motorSimulacao;
        this.maxTarefas = maxTarefas;
        this.ativo = true;
        this.random = new Random();
        
        // Configuração de quão rápido as tarefas chegam (ex: entre 0.5s e 2s)
        this.intervaloMinimoMs = 500;
        this.intervaloMaximoMs = 2000;
    }

    // Método para interromper a geração de tarefas externamente (Botão "Stop" da UI)
    public void desligar() {
        this.ativo = false;
    }

    @Override
    public void run() {
        System.out.println("[Gerador] Iniciando injeção de tarefas (Tráfego Dinâmico)...");
        
        while (ativo && (maxTarefas == 0 || contadorId <= maxTarefas)) {
            try {
                // 1. Simula o tempo de espera até o próximo "cliente" enviar uma tarefa
                int tempoEspera = intervaloMinimoMs + random.nextInt(intervaloMaximoMs - intervaloMinimoMs);
                Thread.sleep(tempoEspera);
                
                // 2. Criação da Tarefa Aleatória
                int id = contadorId++;
                // Tempo de execução entre 1 e 6 segundos
                long tempoExecucao = 1000 + random.nextInt(5000); 
                // Prioridade de 1 a 5 (onde 1 pode ser a mais alta, dependendo do Dev 3)
                int prioridade = 1 + random.nextInt(5); 
                
                // 3. Simulação de Dependências
                List<Integer> dependencias = new ArrayList<>();
                // 30% de chance de a tarefa precisar que a tarefa anterior termine primeiro (Grafo simples)
                if (id > 1 && random.nextInt(100) < 30) {
                    dependencias.add(id - 1); 
                    System.out.println("[Gerador] Nova tarefa " + id + " criada. DEPENDÊNCIA: Aguarda a tarefa " + (id - 1));
                }

                // 4. Instancia e injeta no motor (Thread-Safe)
                Tarefa novaTarefa = new Tarefa(id, tempoExecucao, prioridade, dependencias);
                motorSimulacao.registrarNovaTarefa(novaTarefa);

            } catch (InterruptedException e) {
                System.out.println("[Gerador] Foi interrompido antes de finalizar a geração.");
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("[Gerador] Finalizou a geração de tarefas. Total gerado: " + (contadorId - 1));
    }
}