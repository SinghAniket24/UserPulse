package com.aniket.userpulse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ActivityAnalyticsController.class,
        properties = {
                "spring.kafka.listener.auto-startup=false",
                "spring.kafka.admin.auto-create=false"
        }
)
class ActivityAnalyticsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityAnalyticsService analyticsService;

    @Test
    void returnsCountsMapAsJsonWhenServiceHasData() throws Exception {
        Map<String, Long> counts = Map.of(
                "LOGIN_SUCCESS", 5L,
                "USER_SIGNED_UP", 2L
        );
        when(analyticsService.getCounts()).thenReturn(counts);

        mockMvc.perform(get("/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                            "LOGIN_SUCCESS": 5,
                            "USER_SIGNED_UP": 2
                        }
                        """));

        verify(analyticsService).getCounts();
    }

    @Test
    void returnsEmptyJsonObjectWhenServiceReturnsEmptyMap() throws Exception {
        when(analyticsService.getCounts()).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{}", true));

        verify(analyticsService).getCounts();
    }
}
