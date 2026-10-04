# language: pt
Funcionalidade: Acesso a módulos por role
  Como administrador
  Quero vincular roles a módulos do sistema
  Para que cada usuário só acesse os módulos liberados pelas suas roles

  Cenário: Admin vincula uma role a um módulo e o usuário passa a ter o módulo
    Dado o módulo "FINANCAS" chamado "Finanças"
    E um usuário habilitado "adm10" com senha "S3nh@Forte!" e a role "ADMIN"
    E um usuário habilitado "contador1" com senha "S3nh@Forte!" e a role "CONTADOR"
    E eu tento autenticar com usuário "adm10" e senha "S3nh@Forte!"
    Quando eu vinculo a role "CONTADOR" ao módulo "FINANCAS"
    Então a resposta é "OK"
    Quando eu tento autenticar com usuário "contador1" e senha "S3nh@Forte!"
    E eu consulto o meu perfil
    Então os módulos do usuário são "FINANCAS"

  Cenário: Usuário recém-criado só com USER não tem nenhum módulo
    Dado o módulo "FORZA" chamado "Forza"
    E um usuário habilitado "novato2" com senha "S3nh@Forte!" e a role "USER"
    Quando eu tento autenticar com usuário "novato2" e senha "S3nh@Forte!"
    E eu consulto o meu perfil
    Então o usuário não tem módulos

  Cenário: Admin acessa todos os módulos
    Dado o módulo "FINANCAS" chamado "Finanças"
    E o módulo "FORZA" chamado "Forza"
    E um usuário habilitado "adm11" com senha "S3nh@Forte!" e a role "ADMIN"
    Quando eu tento autenticar com usuário "adm11" e senha "S3nh@Forte!"
    E eu consulto o meu perfil
    Então os módulos do usuário incluem "FINANCAS" e "FORZA"

  Cenário: Desvincular a role do módulo remove o acesso
    Dado o módulo "FINANCAS" chamado "Finanças"
    E um usuário habilitado "adm12" com senha "S3nh@Forte!" e a role "ADMIN"
    E um usuário habilitado "piloto3" com senha "S3nh@Forte!" e a role "PILOTO"
    E eu tento autenticar com usuário "adm12" e senha "S3nh@Forte!"
    E eu vinculo a role "PILOTO" ao módulo "FINANCAS"
    Quando eu desvinculo a role "PILOTO" de qualquer módulo
    Então a resposta é "OK"
    Quando eu tento autenticar com usuário "piloto3" e senha "S3nh@Forte!"
    E eu consulto o meu perfil
    Então o usuário não tem módulos

  Cenário: Admin lista os módulos disponíveis
    Dado o módulo "FINANCAS" chamado "Finanças"
    E um usuário habilitado "adm13" com senha "S3nh@Forte!" e a role "ADMIN"
    E eu tento autenticar com usuário "adm13" e senha "S3nh@Forte!"
    Quando eu listo os módulos
    Então a resposta é "OK"
    E o módulo "FINANCAS" aparece na listagem de módulos

  Cenário: Usuário comum não pode vincular role a módulo
    Dado o módulo "FINANCAS" chamado "Finanças"
    E um usuário habilitado "comum4" com senha "S3nh@Forte!" e a role "USER"
    E eu tento autenticar com usuário "comum4" e senha "S3nh@Forte!"
    Quando eu vinculo a role "USER" ao módulo "FINANCAS"
    Então a resposta é "FORBIDDEN"
    E o corpo do erro é um ProblemDetail válido

  Cenário: Usuário comum não pode listar módulos
    Dado um usuário habilitado "comum5" com senha "S3nh@Forte!" e a role "USER"
    E eu tento autenticar com usuário "comum5" e senha "S3nh@Forte!"
    Quando eu listo os módulos
    Então a resposta é "FORBIDDEN"

  Cenário: Vincular a um módulo inexistente falha
    Dado um usuário habilitado "adm14" com senha "S3nh@Forte!" e a role "ADMIN"
    E um usuário habilitado "alvo6" com senha "S3nh@Forte!" e a role "AUDITOR"
    E eu tento autenticar com usuário "adm14" e senha "S3nh@Forte!"
    Quando eu vinculo a role "AUDITOR" ao módulo "NAO_EXISTE"
    Então a resposta é "NOT_FOUND"
