package br.edu.datacenter.model.entities;

package br.edu.datacenter.model.entities;

import br.edu.datacenter.model.core.GerenciadorDeDependencias;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Servidor implements Runnable {
    private final int id;
    private final int capacidadeMaxima;
    
    // Fila segura para concorrência
    private final BlockingQueue<Tarefa> filaLocal; 
    
    // Histórico de IDs processados para o Coletor de Métricas (Dev 3) usar depois
    private final List<Integer> historicoUso;      
    
    // volatile garante que todas as threads vejam imediatamente se o servidor for desligado
    private volatile boolean rodando;              

    // NOVO: Referência para o Gerenciador de Dependências
    private final GerenciadorDeDependencias gerenciadorDependencias;

    // ATUALIZADO: Construtor agora recebe o GerenciadorDeDependencias
    public Servidor(int id, int capacidadeMaxima, GerenciadorDeDependencias gerenciadorDependencias) {
        this.id = id;
        this.capacidadeMaxima = capacidadeMaxima;
        this.filaLocal = new LinkedBlockingQueue<>(capacidadeMaxima);
        this.historicoUso = new ArrayList<>();
        this.rodando = true; // Inicia ligado
        this.gerenciadorDependencias = gerenciadorDependencias; // Guarda a referência
    }

    // --- GETTERS ---
    public int getId() { return id; }
    public int getCapacidadeMaxima() { return capacidadeMaxima; }
    public BlockingQueue<Tarefa> getFilaLocal() { return filaLocal; }

    // Retorna uma cópia do histórico para não dar erro de concorrência se a tela tentar ler
    public synchronized List<Integer> getHistoricoUso() {
        return new ArrayList<>(historicoUso);
    }

    // --- MÉTODOS DE CONTROLE ---
    
    // Método usado pelo Escalonador (Dev 3) para injetar tarefas neste servidor
    public boolean adicionarTarefa(Tarefa tarefa) {
        // offer() tenta adicionar. Retorna false se a fila estiver cheia (Sobrecarga)
        return filaLocal.offer(tarefa); 
    }

    // Desliga a Thread do Servidor com segurança
    public void desligar() {
        this.rodando = false;
    }

    // --- MOTOR DA THREAD (O TRABALHADOR) ---
    @Override
    public void run() {
        System.out.println("[Servidor " + id + "] Ligado. Monitorando fila local...");

        while (rodando) {
            try {
                // O MÉTODO TAKE(): 
                // Se a fila estiver vazia, a thread "dorme" aqui sem gastar CPU.
                // Assim que o Escalonador adicionar uma tarefa, ela acorda instantaneamente.
                Tarefa tarefa = filaLocal.take();

                System.out.println("[Servidor " + id + "] Iniciou execução da Tarefa " + tarefa.getId());
                tarefa.setStatus(StatusTarefa.EXECUTANDO);

                // 1. SIMULANDO A PASSAGEM DE TEMPO (Processamento)
                long tempoDeExecucao = tarefa.getTempoExecucaoRestante();
                Thread.sleep(tempoDeExecucao); 

                // 2. TAREFA FINALIZADA: Atualizando os dados
                tarefa.atualizarTempoExecucao(tempoDeExecucao); // Zera o tempo restante
                tarefa.setStatus(StatusTarefa.CONCLUIDA);
                
                // 3. REGISTRANDO NO HISTÓRICO (Com synchronized pois listas normais não são thread-safe)
                synchronized (this) {
                    historicoUso.add(tarefa.getId());
                }

                System.out.println("[Servidor " + id + "] Tarefa " + tarefa.getId() + " CONCLUÍDA.");

                // NOVO: Integrando com o Gerenciador de Dependências
                // Avisa o gerenciador que essa tarefa acabou para ele tentar liberar quem estiver esperando
                if (gerenciadorDependencias != null) {
                    gerenciadorDependencias.notificarConclusao(tarefa);
                }

            } catch (InterruptedException e) {
                // Exceção disparada se o programa for fechado à força enquanto a Thread dorme
                System.out.println("[Servidor " + id + "] Interrompido de forma inesperada.");
                Thread.currentThread().interrupt(); // Restaura o estado de interrupção
                break; // Quebra o while e finaliza a Thread
            }
        }
        System.out.println("[Servidor " + id + "] Totalmente Desligado.");
    }
}