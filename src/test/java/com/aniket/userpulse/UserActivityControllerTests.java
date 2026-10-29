package com.aniket.userpulse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    void validActivity() throws Exception {
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

        verify(producer).sendEvent(new UserActivityEvent("user-1", "LOGIN_SUCCESS"));
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
}
