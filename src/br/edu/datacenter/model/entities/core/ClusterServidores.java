package br.edu.datacenter.model.entities.core;

import br.edu.datacenter.model.entities.Servidor;
import java.util.ArrayList;
import java.util.List;

public class ClusterServidores {
    private final List<Servidor> listaServidores;
    private final List<Thread> listaThreads;

    public ClusterServidores(int quantidadeServidores, int capacidadePorServidor) {
        this.listaServidores = new ArrayList<>();
        this.listaThreads = new ArrayList<>();

        for (int i = 1; i <= quantidadeServidores; i++) {
            // 1. Instancia o objeto Servidor (Runnable)
            Servidor servidor = new Servidor(i, capacidadePorServidor);
            listaServidores.add(servidor);

            // 2. Envolve o Servidor em uma Thread do Java
            Thread threadServidor = new Thread(servidor);
            // Nomear a thread é uma ótima prática para debug em Sistemas Operacionais
            threadServidor.setName("Thread-Servidor-" + i); 
            listaThreads.add(threadServidor);
        }
    }

    public void ligarDataCenter() {
        System.out.println("Iniciando Data Center...");
        for (Thread t : listaThreads) {
            t.start(); // Inicia a execução concorrente do método run() de cada Servidor
        }
    }
    
    public void desligarDataCenter() {
        for (Servidor s : listaServidores) {
            s.desligar();
        }
        for (Thread t : listaThreads) {
            t.interrupt(); // Interrompe caso estejam presas no sleep() ou take()
        }
    }

    public List<Servidor> getServidores() {
        return listaServidores;
    }
}
