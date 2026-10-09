package com.events.goup;

import com.events.goup.entity.User;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Login, cadastro e JWT")
class AuthIntegrationTest extends IntegrationTestBase {

    @Value("${goup.jwt.secret}")
    private String jwtSecret;

    private String registerJson(String name, String email, String password) {
        return """
                {"name":"%s","email":"%s","password":"%s"}
                """.formatted(name, email, password);
    }

    private String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    // ---------- cadastro ----------

    @Test
    @DisplayName("cadastro cria usuário Free com role USER, normaliza o e-mail (minúsculas) e devolve token")
    void register_ok() throws Exception {
        perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Ana", "Ana@Email.com", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value("ana@email.com"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.premium").value(false));
    }

    @Test
    @DisplayName("a senha é salva com BCrypt, nunca em texto puro")
    void register_savesPasswordHashed() throws Exception {
        perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Ana", "ana@email.com", PASSWORD)))
                .andExpect(status().isCreated());

        User saved = userRepository.findByEmail("ana@email.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("cadastro com e-mail já existente responde 409")
    void register_duplicateEmail() throws Exception {
        createUser("ana@email.com", false);

        perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Outra Ana", "ANA@email.com", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("cadastro sem e-mail, nome ou senha responde 400 apontando o campo")
    void register_missingFields() throws Exception {
        perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ana","password":"senha123"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));

        perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"  ","email":"ana@email.com","password":"senha123"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));

        perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ana","email":"ana@email.com"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    @DisplayName("cadastro com e-mail em formato inválido responde 400")
    void register_invalidEmail() throws Exception {
        perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Ana", "isso-nao-e-email", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("E-mail inválido"));
    }

    @Test
    @DisplayName("cadastro com senha curta (menos de 8) ou longa demais (mais de 72) responde 400")
    void register_invalidPasswordSize() throws Exception {
        perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Ana", "ana@email.com", "1234567")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));

        perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Ana", "ana@email.com", "a".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    // ---------- login ----------

    @Test
    @DisplayName("login com credenciais corretas devolve token e dados do usuário")
    void login_ok() throws Exception {
        createUser("ana@email.com", true);

        perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("ana@email.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("ana@email.com"))
                .andExpect(jsonPath("$.user.premium").value(true));
    }

    @Test
    @DisplayName("login ignora maiúsculas e espaços no e-mail")
    void login_emailIsNormalized() throws Exception {
        createUser("ana@email.com", false);

        perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("  ANA@Email.com", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("senha errada e e-mail inexistente dão a mesma resposta 401")
    void login_invalidCredentials() throws Exception {
        createUser("ana@email.com", false);

        perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("ana@email.com", "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));

        perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("ninguem@email.com", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
    }

    @Test
    @DisplayName("login sem e-mail ou sem senha responde 400")
    void login_missingFields() throws Exception {
        perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"ana@email.com"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));

        perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"password":"senha123"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    @DisplayName("fluxo completo: cadastrar, logar e usar o token em /users/me")
    void fullFlow_registerLoginAndUseToken() throws Exception {
        perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Ana", "ana@email.com", PASSWORD)))
                .andExpect(status().isCreated());

        String body = perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("ana@email.com", PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@email.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.premium").value(false));
    }

    // ---------- JWT e rotas protegidas ----------

    @Test
    @DisplayName("rota protegida sem token responde 401 (e não 403)")
    void protectedRoute_withoutToken() throws Exception {
        perform(get("/users/me")).andExpect(status().isUnauthorized());
        perform(get("/users/me/favorites")).andExpect(status().isUnauthorized());
        perform(get("/users/me/events")).andExpect(status().isUnauthorized());
        perform(post("/events").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        perform(post("/locations").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token com lixo, de outra chave ou expirado responde 401")
    void protectedRoute_invalidTokens() throws Exception {
        perform(get("/users/me").header("Authorization", "Bearer isto-nao-e-um-jwt"))
                .andExpect(status().isUnauthorized());

        String wrongKey = Jwts.builder()
                .subject("ana@email.com")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("outra-chave-secreta-com-mais-de-32-bytes-ok".getBytes()))
                .compact();
        createUser("ana@email.com", false);
        perform(get("/users/me").header("Authorization", "Bearer " + wrongKey))
                .andExpect(status().isUnauthorized());

        String expired = Jwts.builder()
                .subject("ana@email.com")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
        perform(get("/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token válido de um usuário que não existe mais responde 401 (e não 500)")
    void protectedRoute_tokenOfDeletedUser() throws Exception {
        User user = createUser("fantasma@goup.com", false);
        String token = bearer(user);
        userRepository.delete(user);

        perform(get("/users/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("cabeçalho Authorization sem o prefixo Bearer é ignorado")
    void protectedRoute_withoutBearerPrefix() throws Exception {
        User user = createUser("ana@email.com", false);
        String rawToken = bearer(user).substring("Bearer ".length());

        perform(get("/users/me").header("Authorization", rawToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rotas públicas funcionam sem token")
    void publicRoutes_withoutToken() throws Exception {
        perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)));
        perform(get("/events")).andExpect(status().isOk());
        perform(get("/locations")).andExpect(status().isOk());
        perform(get("/plans")).andExpect(status().isOk());
    }
}
