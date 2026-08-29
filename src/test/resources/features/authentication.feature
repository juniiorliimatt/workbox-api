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
