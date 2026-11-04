package com.aniket.userpulse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserActivityController.class,
        properties = {
                "spring.kafka.listener.auto-startup=false",
                "spring.kafka.admin.auto-create=false"
        }
)
class UserActivityControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserActivityProducer producer;

    @Test
    void missingEventIdGeneratesUuid() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": "user-1",
                                    "eventType": "LOGIN_SUCCESS"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Activity accepted for processing"));

        ArgumentCaptor<UserActivityEvent> captor = ArgumentCaptor.forClass(UserActivityEvent.class);
        verify(producer).sendEvent(captor.capture());

        UserActivityEvent capturedEvent = captor.getValue();
        assertEquals("user-1", capturedEvent.userId());
        assertEquals("LOGIN_SUCCESS", capturedEvent.eventType());
        assertNotNull(capturedEvent.eventId());
        assertFalse(capturedEvent.eventId().isBlank());
        assertDoesNotThrow(() -> UUID.fromString(capturedEvent.eventId()));
    }

    @Test
    void blankEventIdGeneratesUuid() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "eventId": "   ",
                                    "userId": "user-1",
                                    "eventType": "LOGIN_SUCCESS"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Activity accepted for processing"));

        ArgumentCaptor<UserActivityEvent> captor = ArgumentCaptor.forClass(UserActivityEvent.class);
        verify(producer).sendEvent(captor.capture());

        UserActivityEvent capturedEvent = captor.getValue();
        assertEquals("user-1", capturedEvent.userId());
        assertEquals("LOGIN_SUCCESS", capturedEvent.eventType());
        assertNotNull(capturedEvent.eventId());
        assertFalse(capturedEvent.eventId().isBlank());
        assertDoesNotThrow(() -> UUID.fromString(capturedEvent.eventId()));
    }

    @Test
    void clientSuppliedEventIdIsPreserved() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "eventId": "custom-event-123",
                                    "userId": "user-1",
                                    "eventType": "LOGIN_SUCCESS"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Activity accepted for processing"));

        verify(producer).sendEvent(new UserActivityEvent("custom-event-123", "user-1", "LOGIN_SUCCESS"));
    }

    @Test
    void existingRequestFormatWithoutEventIdStillWorks() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": "user-101",
                                    "eventType": "LOGIN_SUCCESS"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Activity accepted for processing"));

        ArgumentCaptor<UserActivityEvent> captor = ArgumentCaptor.forClass(UserActivityEvent.class);
        verify(producer).sendEvent(captor.capture());

        UserActivityEvent capturedEvent = captor.getValue();
        assertEquals("user-101", capturedEvent.userId());
        assertEquals("LOGIN_SUCCESS", capturedEvent.eventType());
        assertNotNull(capturedEvent.eventId());
        assertFalse(capturedEvent.eventId().isBlank());
        assertDoesNotThrow(() -> UUID.fromString(capturedEvent.eventId()));
    }

    @Test
    void blankEventType() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": "user-1",
                                    "eventType": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid activity request"));

        verifyNoInteractions(producer);
    }

    @Test
    void nullEventType() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": "user-1",
                                    "eventType": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid activity request"));

        verifyNoInteractions(producer);
    }

    @Test
    void missingUserId() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "eventType": "LOGIN_SUCCESS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid activity request"));

        verifyNoInteractions(producer);
    }

    @Test
    void blankUserId() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": "   ",
                                    "eventType": "LOGIN_SUCCESS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid activity request"));

        verifyNoInteractions(producer);
    }
}
