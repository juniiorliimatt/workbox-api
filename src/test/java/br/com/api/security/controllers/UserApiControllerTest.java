package br.com.api.security.controllers;

import br.com.api.config.AppConfigTest;
import br.com.api.security.dto.UserApiDTO;
import br.com.api.security.entities.UserApi;
import br.com.api.security.repositories.UserApiRepository;
import br.com.api.security.services.UserApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@Import(AppConfigTest.class)
@WebMvcTest(UserApiController.class)
@MockBean(JpaMetamodelMappingContext.class)
class UserApiControllerTest {

    private static final String USER_TEST = "user_test";
    private static final String USERNAME = "username";

    private UUID id;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private UserApiRepository userApiRepository;

    @MockBean
    private UserApiService userApiService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        this.id = UUID.randomUUID();
    }

    @Test
    void testGetUserApiById() throws Exception {
        final var user = new UserApi(this.id, USERNAME, USERNAME, true, LocalDateTime.now(), LocalDateTime.now(), USER_TEST, USER_TEST);
        final var userApiDto = new UserApiDTO(user.getId(), user.getUsername(), user.getEnabled());
        when(userApiService.findById(this.id)).thenReturn(userApiDto);

        mockMvc.perform(get("/user/"+this.id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(USERNAME));
    }
}
