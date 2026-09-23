package com.mgwprod.billing.service;

import com.mgwprod.billing.gateway.PaymentGateway;
import com.mgwprod.billing.gateway.PaymentResult;
import com.mgwprod.billing.model.Subscription;
import com.mgwprod.billing.model.SubscriptionPlan;
import com.mgwprod.billing.repository.SubscriptionRepository;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

// Lógica de negocio de las suscripciones: crear/leer, subir o bajar de plan, y el
// límite de producciones del plan free — usado desde catalog y collab antes de
// permitir crear un beat o un topline nuevo.
@Service
public class SubscriptionService {

    private static final BigDecimal PREMIUM_PRICE_USD = new BigDecimal("15.00");
    private static final int FREE_PLAN_LIMIT = 50;

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentGateway paymentGateway;
    private final UserRepository userRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                                PaymentGateway paymentGateway,
                                UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentGateway = paymentGateway;
        this.userRepository = userRepository;
    }

    // El controller las usa para devolver 404/403 antes de tocar la suscripción.
    @Transactional(readOnly = true)
    public boolean userExists(Long userId) {
        return userRepository.findById(userId).isPresent();
    }

    // true si el usuario existe y tiene rol ARTIST (solo los artistas tienen plan).
    @Transactional(readOnly = true)
    public boolean isArtist(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRole() == Role.ARTIST;
    }

    // Si el usuario nunca tuvo suscripción, le crea una en FREE automáticamente — así
    // el resto del código nunca tiene que preguntarse "¿y si no tiene suscripción todavía?".
    @Transactional
    public Subscription getOrCreate(Long userId) {
        Subscription existing = subscriptionRepository.findByUserId(userId).orElse(null);
        if (existing != null) {
            return existing;
        }
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setPlan(SubscriptionPlan.FREE);
        return subscriptionRepository.save(subscription);
    }

    // Si ya es premium no hace nada (evita cobrar dos veces). Si el pago no se
    // aprueba, la suscripción se guarda igual pero se queda en FREE — no rompe nada,
    // simplemente el upgrade no se efectiviza.
    @Transactional
    public Subscription upgrade(Long userId) {
        Subscription subscription = getOrCreate(userId);
        if (subscription.getPlan() == SubscriptionPlan.PREMIUM) {
            return subscription;
        }
        PaymentResult result = paymentGateway.charge(userId, PREMIUM_PRICE_USD);
        if (result.approved()) {
            subscription.setPlan(SubscriptionPlan.PREMIUM);
        }
        return subscriptionRepository.save(subscription);
    }

    // Pasa el plan a FREE. No devuelve plata: es una simulación.
    @Transactional
    public Subscription downgrade(Long userId) {
        Subscription subscription = getOrCreate(userId);
        subscription.setPlan(SubscriptionPlan.FREE);
        return subscriptionRepository.save(subscription);
    }

    // Los servicios de catalog/collab la llaman antes de crear un beat/topline nuevo,
    // para poder devolver 403 sin necesidad de una excepción cruzando módulos.
    // OJO: no puede ser readOnly, aunque a simple vista solo "lee" el límite — por
    // dentro llama a getOrCreate, que puede terminar haciendo un INSERT si el usuario
    // todavía no tenía suscripción (ej. la primera vez que un artista nuevo publica
    // algo). Con readOnly=true, MySQL rechaza ese INSERT con un error de conexión de
    // solo lectura.
    @Transactional
    public boolean isAtProductionLimit(Long userId) {
        Subscription subscription = getOrCreate(userId);
        return subscription.getPlan() == SubscriptionPlan.FREE && subscription.getProductionsCount() >= FREE_PLAN_LIMIT;
    }

    // Suma 1 al contador de producciones — la llama BeatService/ToplineService cada
    // vez que se crea un beat o un topline nuevo.
    @Transactional
    public void recordProduction(Long userId) {
        Subscription subscription = getOrCreate(userId);
        subscription.setProductionsCount(subscription.getProductionsCount() + 1);
        subscriptionRepository.save(subscription);
    }
}
