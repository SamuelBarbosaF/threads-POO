
package br.edu.datacenter.controller;


import br.edu.datacenter.model.entities.balancer.BalanceadorCarga;
import br.edu.datacenter.model.entities.balancer.BalanceadorMenorFila;
import br.edu.datacenter.model.entities.core.ClusterServidores;
import br.edu.datacenter.model.entities.core.Escalonador;
import br.edu.datacenter.model.entities.core.GeradorDeCarga;
import br.edu.datacenter.model.entities.core.MotorSimulacao;
import br.edu.datacenter.model.entities.metrics.ColetorDeMetricas;
import br.edu.datacenter.model.entities.strategies.EscalonamentoPrioridadeAging;
import br.edu.datacenter.model.entities.strategies.EstrategiaEscalonamento;

/**
 * ## SimuladorController
 *
 * Esta classe funciona como o "controle" da simulacao.
 *
 * Ela nao executa tarefas diretamente. O papel dela e montar os objetos
 * principais, iniciar as threads, parar a simulacao e entregar metricas para
 * quem estiver usando o sistema.
 */
public class SimuladorController {

    // Motor central: guarda a fila global, a lista de tarefas e o gerenciador de dependencias.
    private MotorSimulacao motor;

    // Cluster: conjunto de servidores que processam tarefas.
    private ClusterServidores cluster;

    // Escalonador: escolhe tarefas prontas e envia para servidores.
    private Escalonador escalonador;

    // Gerador: cria tarefas automaticamente durante a simulacao.
    private GeradorDeCarga gerador;

    // Coletor: calcula tempo medio, vazao, utilizacao e outras metricas.
    private ColetorDeMetricas coletor;

    // Thread que executa o escalonador em paralelo.
    private Thread threadEscalonador;

    // Thread que executa o gerador de carga em paralelo.
    private Thread threadGerador;

    // Guarda o horario em que a simulacao comecou.
    private long inicioSimulacao;


    public void iniciarSimulacao(
            int quantidadeServidores,
            int capacidadeServidor,
            int quantidadeTarefas) {

        // Chama a versao completa do metodo usando Prioridade com Aging e Menor Fila como padrao.
        iniciarSimulacao(
                quantidadeServidores,
                capacidadeServidor,
                quantidadeTarefas,
                new EscalonamentoPrioridadeAging(),
                new BalanceadorMenorFila());
    }

    /**
     * Inicia a simulacao com estrategia e balanceador escolhidos.
     *
     * @param quantidadeServidores quantos servidores serao criados
     * @param capacidadeServidor tamanho maximo da fila de cada servidor
     * @param quantidadeTarefas quantas tarefas o gerador deve criar
     * @param estrategia regra usada para escolher a proxima tarefa
     * @param balanceador regra usada para escolher o servidor
     */
    public void iniciarSimulacao(
            int quantidadeServidores,
            int capacidadeServidor,
            int quantidadeTarefas,
            EstrategiaEscalonamento estrategia,
            BalanceadorCarga balanceador) {

        // Cria um novo motor central para esta simulacao.
        motor = new MotorSimulacao();

        // Cria o cluster de servidores usando a quantidade e capacidade informadas.
        cluster =
                new ClusterServidores(
                        quantidadeServidores,
                        capacidadeServidor,
                        motor.getGerenciadorDependencias());

        // Liga os servidores, ou seja, inicia as threads deles.
        cluster.ligarDataCenter();

        // Cria o escalonador que vai consumir a fila global do motor.
        escalonador =
                new Escalonador(
                        motor.getFilaGlobalDeProntos(),
                        cluster.getServidores(),
                        estrategia,
                        balanceador);

        // Cria a thread responsavel por executar o escalonador.
        threadEscalonador = new Thread(escalonador);

        // Nomeia a thread para facilitar identificacao durante debug.
        threadEscalonador.setName("Thread-Escalonador");

        // Inicia a execucao paralela do escalonador.
        threadEscalonador.start();

        // Cria o gerador de tarefas usando o motor e a quantidade desejada.
        gerador =
                new GeradorDeCarga(
                        motor,
                        quantidadeTarefas);

        // Cria a thread responsavel por executar o gerador.
        threadGerador = new Thread(gerador);

        // Nomeia a thread para facilitar identificacao durante debug.
        threadGerador.setName("Thread-Gerador");

        // Inicia a execucao paralela do gerador de carga.
        threadGerador.start();

        // Cria o coletor usando todas as tarefas do motor e os servidores do cluster.
        coletor =
                new ColetorDeMetricas(
                        motor.getTodasAsTarefasDoSistema(),
                        cluster.getServidores());

        // Salva o horario atual para calcular depois o tempo total da simulacao.
        inicioSimulacao = System.currentTimeMillis();

        // Mostra no console que a simulacao foi iniciada.
        System.out.println("[Controller] Simulacao iniciada.");
    }

    /**
     * Para a simulacao.
     *
     * Este metodo tenta encerrar todos os componentes principais:
     * gerador, escalonador, servidores e threads.
     */
    public void pararSimulacao() {
        // Se o gerador ja foi criado, pede para ele parar o loop.
        if (gerador != null) {
            gerador.desligar();
        }

        // Se o escalonador ja foi criado, pede para ele parar o loop.
        if (escalonador != null) {
            escalonador.desligar();
        }

        // Se o cluster ja foi criado, desliga todos os servidores.
        if (cluster != null) {
            cluster.desligarDataCenter();
        }

        // Se a thread do gerador existir, interrompe caso esteja em sleep.
        if (threadGerador != null) {
            threadGerador.interrupt();
        }

        // Se a thread do escalonador existir, interrompe caso esteja em sleep.
        if (threadEscalonador != null) {
            threadEscalonador.interrupt();
        }

        // Mostra no console que a simulacao foi encerrada.
        System.out.println("[Controller] Simulacao encerrada.");
    }

    /**
     * Retorna o tempo medio que as tarefas esperaram antes de executar.
     *
     * @return tempo medio de espera em milissegundos
     */
    public double getTempoMedioEspera() {
        // Se o coletor ainda nao existe, nao ha metrica para calcular.
        if (coletor == null) {
            return 0;
        }

        // Delega o calculo para o coletor de metricas.
        return coletor.calcularTempoMedioEspera();
    }

    /**
     * Retorna o tempo medio de processamento das tarefas concluidas.
     *
     * @return tempo medio de processamento em milissegundos
     */
    public double getTempoMedioProcessamento() {
        // Se o coletor ainda nao existe, nao ha metrica para calcular.
        if (coletor == null) {
            return 0;
        }

        // Delega o calculo para o coletor de metricas.
        return coletor.calcularTempoMedioProcessamento();
    }

    /**
     * Retorna a vazao da simulacao.
     *
     * Vazao significa quantas tarefas sao concluidas por minuto.
     *
     * @return tarefas concluidas por minuto
     */
    public double getVazao() {
        // Se o coletor ainda nao existe, nao ha metrica para calcular.
        if (coletor == null) {
            return 0;
        }

        // Delega o calculo para o coletor de metricas.
        return coletor.calcularVazao();
    }

    /**
     * Retorna a quantidade de tarefas concluidas.
     *
     * @return total de tarefas com status CONCLUIDA
     */
    public int getQuantidadeConcluidas() {
        // Se o coletor ainda nao existe, nao ha metrica para calcular.
        if (coletor == null) {
            return 0;
        }

        // Delega a contagem para o coletor de metricas.
        return coletor.getQuantidadeConcluidas();
    }

    /**
     * Retorna a utilizacao media dos servidores.
     *
     * @return percentual medio de uso dos servidores
     */
    public double getUtilizacaoMedia() {
        // Se o coletor ainda nao existe, nao ha metrica para calcular.
        if (coletor == null) {
            return 0;
        }

        // Calcula ha quanto tempo a simulacao esta rodando.
        long tempoTotalSimulacao =
                System.currentTimeMillis()
                - inicioSimulacao;

        // Delega o calculo para o coletor de metricas.
        return coletor.calcularUtilizacaoMedia(tempoTotalSimulacao);
    }

    /**
     * Retorna o motor da simulacao.
     *
     * @return objeto MotorSimulacao usado atualmente
     */
    public MotorSimulacao getMotor() {
        return motor;
    }

    /**
     * Retorna o cluster de servidores.
     *
     * @return objeto ClusterServidores usado atualmente
     */
    public ClusterServidores getCluster() {
        return cluster;
    }

    /**
     * Retorna o coletor de metricas.
     *
     * @return objeto ColetorDeMetricas usado atualmente
     */
    public ColetorDeMetricas getColetor() {
        return coletor;
    }
}
