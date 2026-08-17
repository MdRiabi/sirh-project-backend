package com.sirh.common.dto;

// Enveloppe unique pour toute réponse API portant un seul objet — le frontend
// n'a qu'une seule forme à parser (`{ "data": ... }`) quel que soit l'endpoint.
public record SingleObjectDto<T>(T data) {

    public static <T> SingleObjectDto<T> of(T data) {
        return new SingleObjectDto<>(data);
    }
}
