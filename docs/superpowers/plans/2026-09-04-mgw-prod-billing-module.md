# Módulo `billing` — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir el módulo `billing` (suscripción free/premium + pago simulado) del pedido del profesor. Dueño: Dani.

**Prerequisito:** `feature/roles-migration` (Santiago) ya mergeado a `main`.

```bash
git checkout main
git pull
git checkout -b feature/billing-module
```

**Architecture:** Paquete vertical nuevo `com.mgwprod.billing` (controller/service/repository/model/gateway/exception), calcado del patrón de `catalog`/`collab`/`challenges`. `billing` depende de `users` (FK a `User`, igual que los demás módulos) y de nada más — en particular, **nunca importa nada de `catalog` ni de `collab`**. La dirección de la dependencia va al revés: `catalog` y `collab` van a depender de `billing` (Task 5), como ya dependen de `users`.

**Por qué el conteo del límite vive en `billing` y no se calcula sumando `catalog`+`collab`:** el límite de 50 es "beats + toplines combinados", pero si `billing` tuviera que sumar `COUNT(*)` de las tablas de `catalog` y `collab`, tendría que importar ambos módulos — y si en cambio `catalog`/`collab` se consultaran entre sí para sumar sus propios conteos, se rompe la regla ya establecida de que `collab` depende de `catalog` y nunca al revés. La solución: `Subscription` tiene su propio contador (`productionsCount`), que `catalog` y `collab` incrementan llamando a `billing` (nunca se consultan entre sí). El contador **no decrementa** si se borra un beat/topline — es un total histórico, no el tamaño del catálogo activo (evita gamear el límite publicando y borrando).

**Tech Stack:** Java 21+, Spring Boot 4.1.0, Spring Data JPA, MySQL, Lombok, JUnit 5 + Mockito + MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-04-mgw-prod-professor-corrections-design.md` (sección 3).

## Global Constraints

- Sin DTOs: los controllers reciben/devuelven `Subscription` directo.
- Validar solo con `@Valid @RequestBody` en el controller (no aplica acá: los 3 endpoints de `billing` no reciben body, solo el `userId` de la sesión).
- `ddl-auto=none`, schema manual.
- **Aviso de conflicto esperado:** este plan modifica `BeatService.java` (Task 5) y `ToplineService.java` (Task 5) para sumar la llamada a `recordProduction`. El plan de `feature/crud-completion` (Paolo) también modifica esos mismos dos archivos (para agregar `update`/`delete`). Si ambas branches están abiertas en paralelo, el merge de la segunda que se mergee a `main` va a tener conflicto en esos 2 archivos — es esperable, no es un error: cada rama agrega métodos distintos a la misma clase, se resuelve tomando ambos bloques (mismo tipo de conflicto ya resuelto antes en este proyecto entre `catalog`/`collab`/`challenges`).

---

### Task 1: `Subscription` — entidad, repositorio y schema

**Files:**
- Create: `src/main/java/com/mgwprod/billing/model/SubscriptionPlan.java`
- Create: `src/main/java/com/mgwprod/billing/model/Subscription.java`
- Create: `src/main/java/com/mgwprod/billing/repository/SubscriptionRepository.java`
- Modify: `docs/db/schema.sql`
- Test: `src/test/java/com/mgwprod/billing/repository/SubscriptionRepositoryTest.java`

- [ ] **Step 1: Agregar la tabla al schema**

```sql
CREATE TABLE subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    plan VARCHAR(20) NOT NULL,
    productions_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

Correr contra tu MySQL local (o copiar solo este bloque si el resto ya existe):
```bash
mysql -u root -padmin mgw_prod < docs/db/schema.sql
```

- [ ] **Step 2: Test de repositorio**

```java
package com.mgwprod.billing.repository;

import com.mgwprod.billing.model.Subscription;
import com.mgwprod.billing.model.SubscriptionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class SubscriptionRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    void findByUserIdReturnsTheUsersSubscription() {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.FREE);
        subscriptionRepository.save(subscription);

        Optional<Subscription> result = subscriptionRepository.findByUserId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getPlan()).isEqualTo(SubscriptionPlan.FREE);
    }
}
```

(Nota de paquetes Boot 4.1.0: `@DataJpaTest`/`@AutoConfigureTestDatabase` viven en `org.springframework.boot.data.jpa.test.autoconfigure`/`org.springframework.boot.jdbc.test.autoconfigure` — no en `org.springframework.boot.test.autoconfigure.orm.jpa` como en Boot 3. Si `com.h2database:h2` no está en `pom.xml` con scope `test`, agregarlo antes de este paso.)

- [ ] **Step 3: Correr, verificar que falla**

Run: `./mvnw test -Dtest=SubscriptionRepositoryTest`
Expected: FAIL (no compila).

- [ ] **Step 4: `SubscriptionPlan.java`**

```java
package com.mgwprod.billing.model;

public enum SubscriptionPlan {
    FREE,
    PREMIUM
}
```

- [ ] **Step 5: `Subscription.java`**

```java
package com.mgwprod.billing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    @Column(name = "productions_count", nullable = false)
    private int productionsCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
```

- [ ] **Step 6: `SubscriptionRepository.java`**

```java
package com.mgwprod.billing.repository;

import com.mgwprod.billing.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserId(Long userId);
}
```

- [ ] **Step 7: Correr, verificar que pasa; commit**

Run: `./mvnw test -Dtest=SubscriptionRepositoryTest`

```bash
git add src/main/java/com/mgwprod/billing/model/SubscriptionPlan.java \
        src/main/java/com/mgwprod/billing/model/Subscription.java \
        src/main/java/com/mgwprod/billing/repository/SubscriptionRepository.java \
        src/test/java/com/mgwprod/billing/repository/SubscriptionRepositoryTest.java \
        docs/db/schema.sql
git commit -m "feat(billing): add Subscription entity, repository, and schema"
```

---

### Task 2: `PaymentGateway` simulado

**Files:**
- Create: `src/main/java/com/mgwprod/billing/gateway/PaymentResult.java`
- Create: `src/main/java/com/mgwprod/billing/gateway/PaymentGateway.java`
- Create: `src/main/java/com/mgwprod/billing/gateway/SimulatedPaymentGateway.java`
- Test: `src/test/java/com/mgwprod/billing/gateway/SimulatedPaymentGatewayTest.java`

- [ ] **Step 1: Test**

```java
package com.mgwprod.billing.gateway;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedPaymentGatewayTest {

    @Test
    void chargeAlwaysApproves() {
        SimulatedPaymentGateway gateway = new SimulatedPaymentGateway();

        PaymentResult result = gateway.charge(1L, new BigDecimal("15.00"));

        assertThat(result.approved()).isTrue();
        assertThat(result.reference()).isNotBlank();
    }
}
```

- [ ] **Step 2: Correr, verificar que falla**

Run: `./mvnw test -Dtest=SimulatedPaymentGatewayTest`

- [ ] **Step 3: `PaymentResult.java`**

```java
package com.mgwprod.billing.gateway;

public record PaymentResult(boolean approved, String reference) {
}
```

- [ ] **Step 4: `PaymentGateway.java`**

```java
package com.mgwprod.billing.gateway;

import java.math.BigDecimal;

// El día de mañana, una MercadoPagoGateway (o cualquier otra pasarela real) implementa
// esta misma interfaz y se enchufa sin tocar SubscriptionService ni ningún otro
// consumidor — el service depende de la interfaz, nunca de la implementación concreta.
public interface PaymentGateway {
    PaymentResult charge(Long userId, BigDecimal amount);
}
```

- [ ] **Step 5: `SimulatedPaymentGateway.java`**

```java
package com.mgwprod.billing.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(Long userId, BigDecimal amount) {
        return new PaymentResult(true, "SIMULATED-" + UUID.randomUUID());
    }
}
```

- [ ] **Step 6: Correr, verificar que pasa; commit**

```bash
git add src/main/java/com/mgwprod/billing/gateway src/test/java/com/mgwprod/billing/gateway
git commit -m "feat(billing): add simulated PaymentGateway"
```

---

### Task 3: `SubscriptionService`

**Files:**
- Create: `src/main/java/com/mgwprod/billing/exception/SubscriptionLimitExceededException.java`
- Create: `src/main/java/com/mgwprod/billing/service/SubscriptionService.java`
- Test: `src/test/java/com/mgwprod/billing/service/SubscriptionServiceTest.java`

**Interfaces:**
- Consumes: `Subscription`, `SubscriptionRepository`, `SubscriptionPlan` (Task 1); `PaymentGateway` (Task 2); `com.mgwprod.users.repository.UserRepository`, `com.mgwprod.users.model.{User, Role}` (ya existen en `users`).
- Produces: `SubscriptionService.getOrCreate(Long userId)`, `.upgrade(Long userId)`, `.downgrade(Long userId)`, `.recordProduction(Long userId)`.

- [ ] **Step 1: Excepción**

```java
package com.mgwprod.billing.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SubscriptionLimitExceededException extends ApiException {
    public SubscriptionLimitExceededException() {
        super(HttpStatus.FORBIDDEN, "Llegaste al límite de 50 producciones del plan free — pasate a premium para subir sin límite");
    }
}
```

- [ ] **Step 2: Test de servicio**

```java
package com.mgwprod.billing.service;

import com.mgwprod.billing.exception.SubscriptionLimitExceededException;
import com.mgwprod.billing.gateway.PaymentGateway;
import com.mgwprod.billing.gateway.PaymentResult;
import com.mgwprod.billing.model.Subscription;
import com.mgwprod.billing.model.SubscriptionPlan;
import com.mgwprod.billing.repository.SubscriptionRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void getOrCreateCreatesFreeSubscriptionWhenMissing() {
        User artist = new User();
        artist.setId(1L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Subscription subscription = subscriptionService.getOrCreate(1L);

        assertThat(subscription.getPlan()).isEqualTo(SubscriptionPlan.FREE);
        assertThat(subscription.getUserId()).isEqualTo(1L);
    }

    @Test
    void getOrCreateThrowsForNonArtistRoles() {
        User label = new User();
        label.setId(2L);
        label.setRole(Role.DISCOGRAFICA);
        when(userRepository.findById(2L)).thenReturn(Optional.of(label));

        assertThatThrownBy(() -> subscriptionService.getOrCreate(2L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void upgradeChargesAndSetsPremium() {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.FREE);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(paymentGateway.charge(anyLong(), any(BigDecimal.class)))
                .thenReturn(new PaymentResult(true, "SIMULATED-abc"));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Subscription upgraded = subscriptionService.upgrade(1L);

        assertThat(upgraded.getPlan()).isEqualTo(SubscriptionPlan.PREMIUM);
    }

    @Test
    void downgradeSetsFree() {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.PREMIUM);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Subscription downgraded = subscriptionService.downgrade(1L);

        assertThat(downgraded.getPlan()).isEqualTo(SubscriptionPlan.FREE);
    }

    @Test
    void recordProductionIncrementsCountWhenUnderLimit() {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.FREE);
        subscription.setProductionsCount(10);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.recordProduction(1L);

        assertThat(subscription.getProductionsCount()).isEqualTo(11);
    }

    @Test
    void recordProductionThrowsWhenFreeLimitReached() {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.FREE);
        subscription.setProductionsCount(50);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.recordProduction(1L))
                .isInstanceOf(SubscriptionLimitExceededException.class);
    }

    @Test
    void recordProductionNeverThrowsForPremium() {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.PREMIUM);
        subscription.setProductionsCount(500);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.recordProduction(1L);

        assertThat(subscription.getProductionsCount()).isEqualTo(501);
    }
}
```

Nota: en `getOrCreate...` cuando ya existe la suscripción no hace falta pisar `userRepository` con otro mock — solo el caso "crear por primera vez" necesita validar el rol contra `users`.

- [ ] **Step 3: Correr, verificar que falla**

Run: `./mvnw test -Dtest=SubscriptionServiceTest`

- [ ] **Step 4: Implementar `SubscriptionService`**

```java
package com.mgwprod.billing.service;

import com.mgwprod.billing.exception.SubscriptionLimitExceededException;
import com.mgwprod.billing.gateway.PaymentGateway;
import com.mgwprod.billing.gateway.PaymentResult;
import com.mgwprod.billing.model.Subscription;
import com.mgwprod.billing.model.SubscriptionPlan;
import com.mgwprod.billing.repository.SubscriptionRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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

    @Transactional
    public Subscription getOrCreate(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UserNotFoundException(userId));
                    if (user.getRole() != Role.ARTIST) {
                        throw new ForbiddenOperationException("Solo los artistas tienen suscripción");
                    }
                    Subscription subscription = new Subscription();
                    subscription.setUserId(userId);
                    subscription.setPlan(SubscriptionPlan.FREE);
                    return subscriptionRepository.save(subscription);
                });
    }

    @Transactional
    public Subscription upgrade(Long userId) {
        Subscription subscription = getOrCreate(userId);
        PaymentResult result = paymentGateway.charge(userId, PREMIUM_PRICE_USD);
        if (result.approved()) {
            subscription.setPlan(SubscriptionPlan.PREMIUM);
        }
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription downgrade(Long userId) {
        Subscription subscription = getOrCreate(userId);
        subscription.setPlan(SubscriptionPlan.FREE);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public void recordProduction(Long userId) {
        Subscription subscription = getOrCreate(userId);
        if (subscription.getPlan() == SubscriptionPlan.FREE && subscription.getProductionsCount() >= FREE_PLAN_LIMIT) {
            throw new SubscriptionLimitExceededException();
        }
        subscription.setProductionsCount(subscription.getProductionsCount() + 1);
        subscriptionRepository.save(subscription);
    }
}
```

- [ ] **Step 5: Correr, verificar que pasa; commit**

```bash
git add src/main/java/com/mgwprod/billing/exception/SubscriptionLimitExceededException.java \
        src/main/java/com/mgwprod/billing/service/SubscriptionService.java \
        src/test/java/com/mgwprod/billing/service/SubscriptionServiceTest.java
git commit -m "feat(billing): add SubscriptionService (getOrCreate, upgrade, downgrade, recordProduction)"
```

---

### Task 4: `SubscriptionController`

**Files:**
- Create: `src/main/java/com/mgwprod/billing/controller/SubscriptionController.java`
- Test: `src/test/java/com/mgwprod/billing/controller/SubscriptionControllerTest.java`

- [ ] **Step 1: Test**

```java
package com.mgwprod.billing.controller;

import com.mgwprod.billing.model.Subscription;
import com.mgwprod.billing.model.SubscriptionPlan;
import com.mgwprod.billing.service.SubscriptionService;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void getMeReturns200WhenAuthenticated() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.FREE);
        when(subscriptionService.getOrCreate(1L)).thenReturn(subscription);

        mockMvc.perform(get("/api/subscriptions/me").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("FREE"));
    }

    @Test
    void getMeReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/subscriptions/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upgradeReturns200WithPremiumPlan() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.PREMIUM);
        when(subscriptionService.upgrade(1L)).thenReturn(subscription);

        mockMvc.perform(post("/api/subscriptions/upgrade").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("PREMIUM"));
    }

    @Test
    void downgradeReturns200WithFreePlan() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.FREE);
        when(subscriptionService.downgrade(1L)).thenReturn(subscription);

        mockMvc.perform(put("/api/subscriptions/downgrade").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("FREE"));
    }
}
```

- [ ] **Step 2: Correr, verificar que falla**

Run: `./mvnw test -Dtest=SubscriptionControllerTest`

- [ ] **Step 3: Implementar `SubscriptionController`**

```java
package com.mgwprod.billing.controller;

import com.mgwprod.billing.model.Subscription;
import com.mgwprod.billing.service.SubscriptionService;
import com.mgwprod.users.exception.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/me")
    public Subscription me(@RequestAttribute(name = "userId", required = false) Long userId) {
        requireAuthenticated(userId);
        return subscriptionService.getOrCreate(userId);
    }

    @PostMapping("/upgrade")
    public Subscription upgrade(@RequestAttribute(name = "userId", required = false) Long userId) {
        requireAuthenticated(userId);
        return subscriptionService.upgrade(userId);
    }

    @PutMapping("/downgrade")
    public Subscription downgrade(@RequestAttribute(name = "userId", required = false) Long userId) {
        requireAuthenticated(userId);
        return subscriptionService.downgrade(userId);
    }

    private void requireAuthenticated(Long userId) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para gestionar tu suscripción");
        }
    }
}
```

- [ ] **Step 4: Correr, verificar que pasa; commit**

```bash
git add src/main/java/com/mgwprod/billing/controller/SubscriptionController.java \
        src/test/java/com/mgwprod/billing/controller/SubscriptionControllerTest.java
git commit -m "feat(billing): add GET/POST/PUT subscriptions endpoints"
```

---

### Task 5: Enforcement — `BeatService`/`ToplineService` llaman a `recordProduction`

**Files:**
- Modify: `src/main/java/com/mgwprod/catalog/service/BeatService.java`
- Modify: `src/main/java/com/mgwprod/collab/service/ToplineService.java`
- Modify: `src/test/java/com/mgwprod/catalog/service/BeatServiceTest.java`
- Modify: `src/test/java/com/mgwprod/collab/service/ToplineServiceTest.java`

- [ ] **Step 1: Tests**

Sumar a `BeatServiceTest` (mockeando `SubscriptionService` — inyectado nuevo en el constructor de `BeatService`):

```java
    @Test
    void createCallsRecordProductionBeforeSaving() {
        // producer válido como en el test existente de create()
        beatService.create(1L, beat);
        verify(subscriptionService).recordProduction(1L);
    }

    @Test
    void createPropagatesSubscriptionLimitExceeded() {
        doThrow(new com.mgwprod.billing.exception.SubscriptionLimitExceededException())
                .when(subscriptionService).recordProduction(1L);

        assertThatThrownBy(() -> beatService.create(1L, beat))
                .isInstanceOf(com.mgwprod.billing.exception.SubscriptionLimitExceededException.class);
        verify(beatRepository, never()).save(any());
    }
```

Mismo par de casos en `ToplineServiceTest` con `ToplineService`.

- [ ] **Step 2: Correr, verificar que fallan**

- [ ] **Step 3: Inyectar `SubscriptionService` y llamarlo antes de `save`**

En `BeatService` (constructor + campo `SubscriptionService subscriptionService`):

```java
    @Transactional
    public Beat create(Long producerId, Beat beat) {
        User producer = userRepository.findById(producerId)
                .orElseThrow(() -> new UserNotFoundException(producerId));
        if (producer.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("Solo un artista puede publicar beats");
        }
        subscriptionService.recordProduction(producerId);
        beat.setProducerId(producerId);
        return beatRepository.save(beat);
    }
```

Mismo patrón en `ToplineService.create`, después de validar el rol y antes de `toplineRepository.save(topline)`.

- [ ] **Step 4: Correr, verificar que pasa; commit**

```bash
git add src/main/java/com/mgwprod/catalog/service/BeatService.java \
        src/main/java/com/mgwprod/collab/service/ToplineService.java \
        src/test/java/com/mgwprod/catalog/service/BeatServiceTest.java \
        src/test/java/com/mgwprod/collab/service/ToplineServiceTest.java
git commit -m "feat: enforce free plan production limit on beat/topline creation"
```

(Recordatorio: si `feature/crud-completion` ya está mergeada a `main` cuando arranques esta task, vas a tener que resolver un conflicto chico en estos mismos 2 archivos al mergear — ver Global Constraints.)

---

### Task 6: Suite completa, docs, recreación de la base local

- [ ] **Step 1: Correr toda la suite**

Run: `./mvnw test`

- [ ] **Step 2: Recrear la base local**

```bash
mysql -u root -padmin -e "DROP DATABASE IF EXISTS mgw_prod; CREATE DATABASE mgw_prod;"
mysql -u root -padmin mgw_prod < docs/db/schema.sql
```

- [ ] **Step 3: Smoke test manual**

Registrar un artista, `GET /api/subscriptions/me` (FREE, `productionsCount=0`), publicar un beat (`productionsCount=1`), `POST /api/subscriptions/upgrade` (PREMIUM), publicar más de 50 beats/toplines como otro artista FREE y confirmar el 403 en el intento 51.

- [ ] **Step 4: Escribir `docs/api/billing-API.md`** (mismo formato que `docs/api/users-API.md`)

- [ ] **Step 5: Commit final y push**

```bash
git add docs/api/billing-API.md
git commit -m "docs(billing): add billing-API.md"
git push -u origin feature/billing-module
```

## Al terminar

Abrir PR de `feature/billing-module` contra `main`.
