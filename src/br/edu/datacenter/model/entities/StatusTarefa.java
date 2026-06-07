package br.edu.datacenter.model.entities;

public enum StatusTarefa {
    BLOQUEADA,  // Esperando tarefas pré-requisito terminarem
    PRONTA,     // Pronta para ser escalonada para um servidor
    EXECUTANDO, // Sendo processada por um servidor
    CONCLUIDA   // Processamento finalizado
}