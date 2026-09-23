package com.mgwprod.collab.model;

// Estados posibles de una Collaboration: arranca en PENDING y el productor la pasa a
// ACCEPTED o REJECTED. Una vez decidida no cambia más (lo controla CollaborationController).
public enum CollaborationStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
