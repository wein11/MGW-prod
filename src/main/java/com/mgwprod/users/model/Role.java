package com.mgwprod.users.model;

// Cada User tiene exactamente un Role. Los services exponen métodos booleanos
// (isArtist, isAdmin, etc.) que leen este campo, y los controllers los consultan
// antes de permitir una acción.
public enum Role {
    ARTIST,        // productor/artista: publica beats, toplines y submissions de challenges
    DISCOGRAFICA,  // cuenta de sello: puede crear challenges, junto con ADMIN
    ADMIN          // rol interno: verifica artistas y puede cerrar/liquidar challenges
}
