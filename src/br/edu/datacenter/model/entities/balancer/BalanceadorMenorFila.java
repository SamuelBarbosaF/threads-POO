package br.edu.datacenter.model.entities.balancer;

import br.edu.datacenter.model.entities.Servidor;
import java.util.List;

/**
 * ## BalanceadorMenorFila
 *
 * Escolhe o servidor com menor carga atual.
 */
public class BalanceadorMenorFila implements BalanceadorCarga {

    /**
     * Percorre todos os servidores e retorna o menos carregado.
     */
    @Override
    public Servidor selecionarServidor(List<Servidor> servidores) {
        // Melhor servidor encontrado ate agora.
        Servidor melhor = null;

        // Percorre a lista de servidores.
        for (Servidor servidor : servidores) {
            // Se ainda nao ha melhor, ou o servidor atual tem carga menor, troca.
            if (melhor == null
                    || servidor.getCargaAtual() < melhor.getCargaAtual()) {
                melhor = servidor;
            }
        }

        // Retorna o servidor com menor fila/carga.
        return melhor;
    }

    /**
     * Nome exibido para esta politica de balanceamento.
     */
    @Override
    public String getNome() {
        return "Menor Fila";
    }
}
