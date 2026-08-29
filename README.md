# workbox-api

Backend REST do [monorepo `workbox`](../README.md) — API de autenticação/usuários,
servida junto com o build do [`workbox-app`](../workbox-app/README.md) num único JAR.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / runtime | Java 26 (toolchain Gradle) |
| Framework | Spring Boot 3.5.16 |
| Build | Gradle 9.7.1 |
| Persistência | Spring Data JPA + Hibernate, Liquibase (migrations) |
| Banco | PostgreSQL (dev/prod), H2 em memória (test) |
| Segurança | Spring Security 6 + JWT (`io.jsonwebtoken`), OAuth2 resource server/client |
| Hypermedia | Spring HATEOAS |
| Documentação de API | springdoc-openapi (Swagger UI + contrato versionado) |
| Cobertura | JaCoCo |
| Análise estática | SonarQube/SonarCloud |

## Estrutura de pacotes

```
br.com.workbox
├── config/            Configuração geral (OpenAPI, JPA auditing, servir o frontend)
├── core/               Anotações/utilitários compartilhados
├── exceptions/         Exceções de domínio + handler global (RestExceptionHandler)
└── security/
    ├── config/         Spring Security, CORS, JWT filter
    ├── controllers/     AuthController, UserApiController
    ├── dto/             DTOs de entrada/saída
    ├── entities/         UserApi, Role
    ├── repositories/     Spring Data JPA
    └── services/         JwtService, UserApiService
```

## Rodando localmente

Profiles disponíveis (`spring.profiles.active`):

| Profile | Banco | Uso |
|---|---|---|
| `test` | H2 em memória, schema criado via Hibernate (`ddl-auto=create-drop`) | Testes automatizados, geração do contrato OpenAPI — não precisa de Postgres |
| `dev` (default) | PostgreSQL local via `DATABASE_URL` (default `jdbc:postgresql://localhost:5432/workbox`) | Desenvolvimento — schema via Liquibase |
| `prod` | PostgreSQL via `DATABASE_URL` (obrigatório) | Deploy |

```bash
./gradlew bootRun                                    # profile dev, exige Postgres local
./gradlew bootRun --args='--spring.profiles.active=test'  # sem dependência externa
```

Variáveis de ambiente relevantes: `PORT` (default 8080), `JWT_SECRET`, `DATABASE_URL`,
`POSTGRES_USER`, `POSTGRES_PASSWORD`, `SCHEMA` (default `api`), `admin.password`.

O Liquibase (`db/changelog/`) já semeia dois usuários (`admin`, `USER`/`ADMIN` roles) e
`user` para desenvolvimento local.

## Autenticação

JWT via `POST /api/auth/login` (retorna `access_token` + `refresh_token`) e
`POST /api/auth/refresh`. Endpoints protegidos exigem `Authorization: Bearer <token>`.

## Contrato de API (OpenAPI)

`openapi/openapi.yaml` é o contrato REST versionado desta API — fonte da verdade para
qualquer client (frontend `workbox-app`, agentes de IA) que a consuma. Não assuma
comportamento de endpoint que não esteja descrito nesse arquivo.

**Regra**: qualquer mudança de contrato (novo endpoint, novo campo, mudança de schema)
exige regenerar e commitar o arquivo junto com a mudança de código. O job
`contract-drift-check` do CI falha o pipeline se o arquivo commitado divergir do gerado
a partir do código.

Para regenerar localmente:

```bash
./gradlew generateOpenApiDocs
git diff openapi/openapi.yaml
```

A task sobe a aplicação com o profile `test` (H2 em memória, sem dependência de
Postgres), baixa `/v3/api-docs.yaml` e grava em `openapi/openapi.yaml`. Com o app
rodando, o Swagger UI fica em `/swagger-ui/index.html`.

Ver também: [AGENTS.md](../AGENTS.md), que descreve como este contrato alinha o
desenvolvimento entre o agente de backend (Claude Code) e o de frontend (Antigravity).

## Testes

```bash
./gradlew check          # testes + JaCoCo
./gradlew jacocoTestReport   # relatório em build/jacocoHtml
```

JUnit 5 + Spring Boot Test + MockMvc. `ApiControllerTestConfig` sobrescreve
intencionalmente os beans de segurança (`spring.main.allow-bean-definition-overriding=true`
no profile `test`) para isolar os testes de controller do fluxo real de
autenticação/DB.

## CI/CD

`.gitlab-ci.yml`: `test` (build + testes) → `contract-drift-check` (contrato em dia) →
`build` (empacota o JAR). `sonarcloud-check` roda análise estática em MRs e nas branches
`main`/`develop`.

## Deploy

Single-jar: o build do `workbox-app` (`npm run build`) escreve direto em
`src/main/resources/static/`, e `FrontendController` serve o `index.html` na raiz —
um único artefato sobe API + SPA. `Procfile`/`system.properties` configuram deploy
estilo Heroku (JDK 26).

## Referências

* [Spring Boot Gradle Plugin](https://docs.spring.io/spring-boot/gradle-plugin) ·
  [Spring Data JPA](https://spring.io/guides/gs/accessing-data-jpa/) ·
  [Spring Security](https://spring.io/guides/gs/securing-web/) ·
  [Spring HATEOAS](https://spring.io/guides/gs/rest-hateoas/) ·
  [Liquibase](https://docs.spring.io/spring-boot/reference/howto/data-initialization.html#howto.data-initialization.migration-tool.liquibase) ·
  [springdoc-openapi](https://springdoc.org/)
