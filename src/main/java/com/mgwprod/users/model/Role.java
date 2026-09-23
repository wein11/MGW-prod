package com.mgwprod.users.model;

public enum Role {
    ARTIST,        // productor/artista: publica beats, toplines y submissions de challenges
    DISCOGRAFICA,  // cuenta de sello: puede crear challenges, junto con ADMIN
    ADMIN          // rol interno: verifica artistas y puede cerrar/liquidar challenges
}
