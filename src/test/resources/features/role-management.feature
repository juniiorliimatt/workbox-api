# language: pt
Funcionalidade: Gestão de roles
  Como administrador
  Quero criar, listar, atualizar e remover roles
  Para gerenciar as permissões dos usuários

  Cenário: Admin cria, lista, atualiza e remove uma role
    Dado um usuário habilitado "karen06" com senha "S3nh@Forte!" e a role "ADMIN"
    E eu tento autenticar com usuário "karen06" e senha "S3nh@Forte!"
    Quando eu crio a role "MANAGER"
    Então a resposta é "CREATED"
    E a role "MANAGER" aparece na listagem
    Quando eu atualizo a role "MANAGER" para "SUPERVISOR"
    Então a resposta é "OK"
    Quando eu removo a role "SUPERVISOR"
    Então a resposta é "NO_CONTENT"
    E a role "SUPERVISOR" não aparece mais na listagem

  Cenário: Usuário comum não pode criar role
    Dado um usuário habilitado "leo07" com senha "S3nh@Forte!" e a role "USER"
    E eu tento autenticar com usuário "leo07" e senha "S3nh@Forte!"
    Quando eu crio a role "MANAGER"
    Então a resposta é "FORBIDDEN"

  Cenário: Usuário comum pode listar roles
    Dado um usuário habilitado "mia08" com senha "S3nh@Forte!" e a role "USER"
    E eu tento autenticar com usuário "mia08" e senha "S3nh@Forte!"
    Quando eu listo as roles
    Então a resposta é "OK"
