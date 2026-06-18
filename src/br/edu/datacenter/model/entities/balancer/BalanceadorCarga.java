package br.edu.datacenter.model.entities.balancer;

import br.edu.datacenter.model.entities.Servidor;
import java.util.List;

/**
 * ## BalanceadorCarga
 *
 * Interface que define o contrato de qualquer balanceador.
 *
 * Um balanceador decide para qual servidor uma tarefa deve ser enviada.
 */
public interface BalanceadorCarga {

    /**
     * Escolhe um servidor dentro da lista recebida.
     */
    Servidor selecionarServidor(List<Servidor> servidores);

    /**
     * Retorna o nome da estrategia de balanceamento.
     */
    String getNome();
}
