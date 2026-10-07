# workbox-api — instruções do serviço

> Complementa o [`CLAUDE.md` da raiz](../CLAUDE.md) (visão do monorepo, contrato, commits,
> infra) e as regras globais de `~/.claude/CLAUDE.md`. Aqui só o que é específico deste
> serviço. Detalhes de uso/endpoints: [`README.md`](README.md).

## Papel e autoria
- **Único emissor de identidade** do monorepo: login/refresh/MFA/logout, CRUD de usuários
  e roles, auditoria e a **introspecção** (`POST /api/v1/auth/introspect`) que todos os
  resource servers usam.
- Implementação **exclusiva do Claude Code** — o desenvolvedor só pede mudanças.
- O nome `workbox-api` (sufixo `-api`) está fixado; não renomear.

## Stack e execução
- Java 25 LTS, Spring Boot 3.5.16, Gradle 9.7.1 (`./gradlew`, nunca `gradle` global),
  Spring Data JPA + Liquibase, Hibernate Envers, Spring Security 6 (+ OAuth2 resource
  server/client), HATEOAS, springdoc, Redis, JaCoCo, Sonar.
- Porta **7051** (container 8080). Profiles: `dev` (default, Postgres `:7050` + Redis
  `:7056`), `prod`, `test` (H2, sem Postgres/Redis).
- Comandos: `./gradlew bootRun`, `./gradlew check` (testes + JaCoCo),
  `./gradlew generateOpenApiDocs` (sobe com profile `test` e grava `openapi/openapi.yaml`).
- Role Postgres `workbox_service`, schema `workbox` (não é o superusuário).

## Estrutura (`br.com.workbox`)
`config/` (OpenAPI, JPA auditing, mail, `CorrelationIdFilter`, Envers em `audit/`) ·
`core/` · `exceptions/` (+ `handler/RestExceptionHandler`) · `security/{config,
controllers, dto, entities, oauth2, repositories, services}`.
Controllers: `AuthController`, `UserApiController`, `RoleController`, `AuditController`.
Entidades: `UserApi`, `Role`, `LoginAudit`, `PasswordResetToken`, `ApiClient`
(+ `ApiClientGrantType`). Não há entidade de refresh token — a tabela foi dropada em
`260916_0001`; o refresh vive no Redis.

## Regras e armadilhas específicas
- **Login é por e-mail** (`UserApi.getUsername()` devolve o e-mail); `socialName` é só
  exibição, sem unicidade. `register` sempre atribui `USER` no servidor — nunca aceitar
  role do payload.
- **Prefixo `ROLE_` só existe no claim `roles` do JWT/introspecção.** No banco e em
  `/api/v1/role` e `/api/v1/user/**` a authority é pura (`ADMIN`, `USER`). Não confundir
  nem "normalizar" — já causou uma role `ADMIN` virar `ROLE_ADMIN` no banco.
- **Acesso a módulos** (`AppModule`, tabela `modules`, `roles.module_id`): `ADMIN` acessa
  todo o catálogo; qualquer outro usuário só os módulos das roles que tem. `USER` é a role
  inicial de todo cadastro e **não libera módulo nenhum** — um ADMIN concede a role de
  módulo depois (`PUT /api/v1/role/{id}/module`). `ModuleAccessService.codigosDo` calcula
  os códigos; a introspecção devolve `modules` lido do **banco** (vale já, sem esperar o
  access token de 15 min expirar) e os resource servers exigem o módulo deles (403).
  Módulo novo = changeset Liquibase novo (módulo + role vinculada), nunca CRUD. A entidade
  chama-se `AppModule` pra não colidir com `java.lang.Module`.
- **Refresh tokens no Redis** (`RefreshTokenService`, chaves `refresh:{jti}` e
  `family:{familyId}`, TTL nativo). Detecção de reuso + revogação da família roda **atômica
  em Lua** (`consume_refresh_token.lua`, `RedisConfig`) — não reescrever como
  read-then-write em Java. `tokenVersion` **continua no Postgres** de propósito (bump
  na mesma transação da troca de senha/reset).
- **Introspecção**: sempre 200; token inválido/expirado → `{"active": false}` (RFC 7662),
  nunca 401. Clientes ficam na tabela `workbox.api_clients` (não em Java). Liberar um
  serviço novo = **changeset Liquibase novo** em `db/changelog/v0.0.2/create/` com
  `client_id`, hash BCrypt custo 12 do secret e `allowed_grant_types=["CLIENT_SECRET"]`
  (referência: `260906_0000_create_table_api_clients.sql`, `261003_0000_seed_api_clients_forza_telemetry_service.sql`);
  e as env vars `*_INTROSPECTION_CLIENT_ID/SECRET` no `docker-compose.yml`/`.env.example`
  da raiz. Revogar cliente = `active=false`, nunca `DELETE`.
- **Liquibase**: `includeAll` por diretório; arquivo novo `yymmdd_nnnn_<acao>_<alvo>.sql`.
  **Nunca editar changeset já aplicado** — sempre arquivo novo. Migrations
  backward-compatible (expand → migrate → contract).
- **Login**: rate limit 10/min por IP; 5 falhas travam a conta por 15 min. Toda tentativa
  vai pra `login_audit`; mudanças em `UserApi`/`Role` ficam no Envers (`*_aud`, `rev_info`).
- **MFA** TOTP: segredo em texto plano em `users_api.mfa_secret` (simplificação de estudo —
  apontar, não "consertar" sem pedido). Login social Google é opt-in por env var.
- **Avatar**: `multipart`, máx. 2 MB, jpeg/png/webp, validado via `ImageIO` e reencodado
  como PNG; gravado em `AVATAR_STORAGE_PATH` (`uploads/avatars`, fora do versionamento).
- **Erros** em `ProblemDetail` (RFC 7807/9457), com catch-all — nunca vazar stack trace.
  Todo request carrega `X-Request-Id` (MDC); propague aos serviços downstream.
- `/actuator/health` e `/actuator/prometheus` abertos (conveniência de estudo local).
- Segredos de dev nos defaults (`jwt.secret`, `admin.password`) são só de estudo; `prod`
  exige `JWT_SECRET`, `DATABASE_URL`, `REDIS_HOST` por env.
- Contas QA fixas (`qa.admin@workbox.local`, `qa.user@workbox.local`) e como recriá-las:
  [README → Contas de teste](README.md#contas-de-teste-qa). As seed (`admin`/`user`) não
  têm senha estável.

## Convenção Java deste repo
- **`final` obrigatório** em todo parâmetro de método/construtor e toda variável local
  (`src/main` **e** `src/test`), exceto quando há reatribuição real (contador de loop,
  acumulador). Código novo já nasce conforme; na dúvida, `final`. (Retrofit feito via
  OpenRewrite pontual — não é dependência do build.)
- Lombok está nas dependências (uso permitido). Respeitar os recursos do Java 25.
- Comentários/Javadoc e nomes de métodos de negócio da camada de serviço em **português**
  (padrão adotado em `refactor: renomeia métodos de negócio ... para português`).

## Testes (test-first)
- JUnit 5 + AssertJ + Spring Boot Test + MockMvc; **Cucumber** para regra de negócio
  (`src/test/resources/features/*.feature` + `steps/`): o `.feature` nasce **antes** da
  implementação (fluxo outside-in no README). `./gradlew test` já roda os `.feature`
  (`RunCucumberTest`).
- `ApiControllerTestConfig` sobrescreve beans de segurança nos testes de controller
  (`allow-bean-definition-overriding` no profile `test`).
- Testes que exigem **Docker** (Testcontainers): `RealPostgresSchemaIT` (Postgres 18,
  role/schema reais, `ddl-auto=validate` — pega drift de Envers que o H2 não pega),
  `RefreshTokenServiceIT` e `CucumberSpringConfiguration` (Redis descartável, usado pelos
  cenários que emitem refresh token). O resto roda só com H2.
- `junit-bom 5.14.2` e `cucumber-bom 7.34.7` fixados no `build.gradle` — não deixar o BOM
  do Spring rebaixar o `junit-platform`.

## Contrato (OpenAPI)
- `openapi/openapi.yaml` é a fonte da verdade; o front consome só dele. Mudou endpoint/
  DTO/auth → regenerar (`generateOpenApiDocs`) e commitar junto (CI `contract-drift-check`
  quebra se divergir). O contrato é **obrigatório e anterior ao front** (global e raiz): alterar o
  contrato observável inclui regenerar o arquivo na mesma tarefa, sem esperar pedido.
- Mudança de contrato observável → ajustar `workbox-app` na mesma tarefa (ver raiz).
- `springdoc.writer-with-order-by-keys=true` mantém a saída determinística.

## Commits
pt-BR, Conventional Commits (`feat(auth): adiciona ...`), conforme o
[CLAUDE.md da raiz](../CLAUDE.md#convenção-de-mensagens-de-commit). Trabalhar em `develop`;
push só com confirmação.
