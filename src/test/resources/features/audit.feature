# language: pt
Funcionalidade: Auditoria
  Como administrador
  Quero consultar o histórico de login e de alterações de usuários
  Para investigar problemas de acesso e mudanças indevidas

  Cenário: Admin consulta o histórico de login de um usuário
    Dado um usuário habilitado "renata13" com senha "S3nh@Forte!" e a role "ADMIN"
    E eu tento autenticar com usuário "renata13" e senha "S3nh@Forte!"
    E eu tento autenticar com usuário "renata13" e senha "senha-errada"
    Quando eu consulto o histórico de login do email "renata13@example.com"
    Então a resposta é "OK"
    E o histórico de login tem pelo menos 2 entradas

  Cenário: Usuário comum não pode consultar o histórico de login
    Dado um usuário habilitado "silas14" com senha "S3nh@Forte!" e a role "USER"
    E eu tento autenticar com usuário "silas14" e senha "S3nh@Forte!"
    Quando eu consulto o histórico de login do email "silas14@example.com"
    Então a resposta é "FORBIDDEN"

  Cenário: Admin consulta o histórico de alterações de um usuário
    Dado um usuário habilitado "quintino12" com senha "S3nh@Forte!" e a role "ADMIN"
    E eu tento autenticar com usuário "quintino12" e senha "S3nh@Forte!"
    Quando eu consulto o histórico de alterações do usuário "quintino12"
    Então a resposta é "OK"
    E o histórico de alterações tem pelo menos 1 revisão do tipo "ADD"
