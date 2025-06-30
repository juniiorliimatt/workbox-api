package br.com.workbox.security.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.workbox.security.dto.UserApiDTO;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.services.UserApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@Import(ApiControllerTestConfig.class)
@WebMvcTest(UserApiController.class)
@MockBean(JpaMetamodelMappingContext.class)
class UserApiControllerTest {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private UUID id;
    private UserApi userApi;
    private Role role;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private UserApiService userApiService;

    @BeforeEach
    void setUp() {
        this.id = UUID.randomUUID();
        this.userApi = UserApi.builder()
                .id(this.id)
                .username(USERNAME)
                .password(PASSWORD)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();
        this.role = Role.builder().id(1L).authority("TEST").users(Set.of(userApi)).build();
        userApi.setRoles(Set.of(role));
    }

    @Test
    @DisplayName(value = "Get UserById")
    void testGetUserApiById() throws Exception {
        final var userApiDto = new UserApiDTO(userApi.getId(), userApi.getUsername(), userApi.getIsEnabled());
        when(userApiService.findById(this.id)).thenReturn(userApiDto);

        mockMvc.perform(get("/api/user/" + this.id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/hal+json"))
                .andExpect(jsonPath("$.username").value(USERNAME));
    }
}
