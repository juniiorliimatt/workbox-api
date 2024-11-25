package br.com.workbox.security.controllers;

import br.com.workbox.security.dto.UserApiDTO;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.UserApiRepository;
import br.com.workbox.security.services.UserApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@Import(ApiControllerTestConfig.class)
@WebMvcTest(UserApiController.class)
@MockBean(JpaMetamodelMappingContext.class)
class UserApiControllerTest {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "username";

    private UUID id;

    private UserApi userApi;

    private Role role;

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
        mockMvc.perform(get("/user/" + this.id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/hal+json"))
                .andExpect(jsonPath("$.username").value(USERNAME));
    }
}
