package br.com.workbox.security.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.workbox.security.dto.UserApiDTO;
import br.com.workbox.security.dto.UserApiInsertOrUpdateDTO;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.services.AvatarService;
import br.com.workbox.security.services.RefreshTokenService;
import br.com.workbox.security.services.UserApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
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

    private static final String NAME = "Test User";
    private static final String EMAIL = "user@example.com";
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

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private AvatarService avatarService;

    @BeforeEach
    void setUp() {
        this.id = UUID.randomUUID();
        this.userApi = UserApi.builder()
                .id(this.id)
                .socialName(NAME)
                .email(EMAIL)
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
        final var userApiDto = new UserApiDTO(userApi.getId(), userApi.getSocialName(), userApi.getEmail(), userApi.getIsEnabled(), null);
        when(userApiService.findById(this.id)).thenReturn(userApiDto);

        mockMvc.perform(get("/api/v1/user/" + this.id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/hal+json"))
                .andExpect(jsonPath("$.socialName").value(NAME));
    }

    @Test
    @DisplayName(value = "Get avatar — devolve os bytes com o content-type correto")
    void testGetAvatar() throws Exception {
        final var bytes = new byte[]{1, 2, 3};
        when(avatarService.load(this.id)).thenReturn(new AvatarService.AvatarContent(bytes, "image/png"));

        mockMvc.perform(get("/api/v1/user/" + this.id + "/avatar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @DisplayName(value = "Save user — Location header aponta pro path real do recurso")
    void testSaveUserApi() throws Exception {
        final var userApiDto = new UserApiDTO(userApi.getId(), userApi.getSocialName(), userApi.getEmail(), userApi.getIsEnabled(), null);
        // Role sem o back-reference `users` — só pra não estourar em recursão infinita na
        // serialização JSON do corpo da requisição (Role não tem @JsonIgnore/@JsonBackReference
        // em `users`, e UserApi.roles -> Role.users -> UserApi de novo).
        final var roleForRequest = Role.builder().id(1L).authority("TEST").build();
        final var insertDto = new UserApiInsertOrUpdateDTO(null, NAME, PASSWORD, EMAIL, true, Set.of(roleForRequest));
        when(userApiService.save(any(UserApiInsertOrUpdateDTO.class))).thenReturn(userApiDto);

        mockMvc.perform(post("/api/v1/user/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(insertDto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/user/" + this.id)));
    }
}
