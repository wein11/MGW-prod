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
