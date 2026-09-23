package com.mgwprod.collab.model;

// Estados posibles de una Collaboration: arranca en PENDING y el productor la pasa a
// ACCEPTED o REJECTED. Ojo: hoy el endpoint no impide volver a mandar PENDING.
public enum CollaborationStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
