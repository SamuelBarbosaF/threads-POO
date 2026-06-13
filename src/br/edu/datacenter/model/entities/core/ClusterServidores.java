package br.edu.datacenter.model.entities.core;

import br.edu.datacenter.model.entities.Servidor;
import java.util.ArrayList;
import java.util.List;

/**
 * ## ClusterServidores
 *
 * Agrupa varios servidores e controla as threads deles.
 */
public class ClusterServidores {

    // Lista com os objetos Servidor.
    private final List<Servidor> listaServidores;

    // Lista com as threads que executam os servidores.
    private final List<Thread> listaThreads;

    // Gerenciador compartilhado entre os servidores.
    private final GerenciadorDeDependencias gerenciadorDependencias;

    /**
     * Cria a quantidade desejada de servidores.
     */
    public ClusterServidores(
            int quantidadeServidores,
            int capacidadePorServidor,
            GerenciadorDeDependencias gerenciadorDependencias) {

        // Inicializa a lista de servidores.
        this.listaServidores = new ArrayList<>();

        // Inicializa a lista de threads.
        this.listaThreads = new ArrayList<>();

        // Guarda o gerenciador recebido.
        this.gerenciadorDependencias = gerenciadorDependencias;

        // Cria servidores numerados de 1 ate quantidadeServidores.
        for (int i = 1; i <= quantidadeServidores; i++) {
            // Cria um servidor com ID, capacidade e gerenciador.
            Servidor servidor =
                    new Servidor(
                            i,
                            capacidadePorServidor,
                            gerenciadorDependencias);

            // Cria uma thread para executar esse servidor.
            Thread threadServidor =
                    new Thread(servidor);

            // Nomeia a thread para facilitar debug.
            threadServidor.setName("Thread-Servidor-" + i);

            // Guarda o servidor.
            listaServidores.add(servidor);

            // Guarda a thread do servidor.
            listaThreads.add(threadServidor);
        }
    }

    /**
     * Inicia todas as threads de servidores.
     */
    public void ligarDataCenter() {
        System.out.println("Iniciando Data Center...");

        // start() chama o metodo run() de cada servidor em paralelo.
        for (Thread thread : listaThreads) {
            thread.start();
        }
    }

    /**
     * Desliga todos os servidores.
     */
    public void desligarDataCenter() {
        // Pede para cada servidor parar seu loop.
        for (Servidor servidor : listaServidores) {
            servidor.desligar();
        }

        // Interrompe threads que estejam bloqueadas em take() ou sleep().
        for (Thread thread : listaThreads) {
            thread.interrupt();
        }
    }

    /**
     * Retorna a lista de servidores.
     */
    public List<Servidor> getServidores() {
        return listaServidores;
    }

    /**
     * Retorna o gerenciador de dependencias usado pelo cluster.
     */
    public GerenciadorDeDependencias getGerenciadorDependencias() {
        return gerenciadorDependencias;
    }
}
