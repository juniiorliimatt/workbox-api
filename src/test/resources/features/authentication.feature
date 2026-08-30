# language: pt
Funcionalidade: Autenticação de usuário
  Como usuário da API
  Quero autenticar com usuário e senha
  Para obter um par de tokens de acesso válido

  Cenário: Login com credenciais válidas
    Dado um usuário habilitado "alice" com senha "S3nh@Forte!"
    Quando eu tento autenticar com usuário "alice" e senha "S3nh@Forte!"
    Então a resposta é "OK"
    E um access_token é retornado

  Cenário: Login com senha incorreta
    Dado um usuário habilitado "roger" com senha "S3nh@Forte!"
    Quando eu tento autenticar com usuário "roger" e senha "senha-errada"
    Então a resposta é "UNAUTHORIZED"

  Cenário: Login com usuário inexistente não revela a diferença de uma senha incorreta
    Quando eu tento autenticar com usuário "usuario-que-nao-existe" e senha "qualquer"
    Então a resposta é "UNAUTHORIZED"

  Cenário: Login de usuário desabilitado é rejeitado
    Dado um usuário desabilitado "carol" com senha "S3nh@Forte!"
    Quando eu tento autenticar com usuário "carol" e senha "S3nh@Forte!"
    Então a resposta é "UNAUTHORIZED"

  Cenário: Conta é bloqueada automaticamente após 5 tentativas de senha errada
    Dado um usuário habilitado "dave22" com senha "S3nh@Forte!"
    Quando eu tento autenticar sem sucesso 5 vezes com usuário "dave22" e senha errada
    E eu tento autenticar com usuário "dave22" e senha "S3nh@Forte!"
    Então a resposta é "UNAUTHORIZED"

  Cenário: Logout revoga o access_token emitido antes dele
    Dado um usuário habilitado "erin99" com senha "S3nh@Forte!"
    E eu tento autenticar com usuário "erin99" e senha "S3nh@Forte!"
    Quando eu faço logout
    Então minhas requisições autenticadas com o token antigo são rejeitadas

  Cenário: Usuário autenticado consulta o próprio perfil
    Dado um usuário habilitado "frank01" com senha "S3nh@Forte!"
    E eu tento autenticar com usuário "frank01" e senha "S3nh@Forte!"
    Quando eu consulto meus dados
    Então recebo meu perfil com nome "frank01"

  Cenário: Troca de senha exige a senha atual correta
    Dado um usuário habilitado "grace02" com senha "S3nh@Forte!"
    E eu tento autenticar com usuário "grace02" e senha "S3nh@Forte!"
    Quando eu tento trocar minha senha de "senha-errada" para "NovaSenh@456"
    Então a resposta é "BAD_REQUEST"

  Cenário: Troca de senha bem-sucedida revoga o access_token antigo
    Dado um usuário habilitado "heidi03" com senha "S3nh@Forte!"
    E eu tento autenticar com usuário "heidi03" e senha "S3nh@Forte!"
    Quando eu tento trocar minha senha de "S3nh@Forte!" para "NovaSenh@456"
    Então a resposta é "NO_CONTENT"
    E minhas requisições autenticadas com o token antigo são rejeitadas
