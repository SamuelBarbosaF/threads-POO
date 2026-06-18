package br.edu.datacenter.model.entities;

/**
 * ## StatusTarefa
 *
 * Enum usado para limitar os estados validos de uma tarefa.
 *
 * Usar enum evita strings soltas como "pronta" ou "executando",
 * reduzindo erros de digitacao e deixando o codigo mais claro.
 */
public enum StatusTarefa {

    // A tarefa ainda depende de outra tarefa para poder executar.
    BLOQUEADA,

    // A tarefa esta liberada para ser escolhida pelo escalonador.
    PRONTA,

    // A tarefa foi entregue a um servidor e esta sendo processada.
    EXECUTANDO,

    // A tarefa terminou seu processamento.
    CONCLUIDA
}
