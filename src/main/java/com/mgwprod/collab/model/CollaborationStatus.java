package com.mgwprod.collab.model;

// Estados posibles de una Collaboration. No hay transición de vuelta a PENDING: una
// vez decidida (ACCEPTED/REJECTED), queda fija.
public enum CollaborationStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
