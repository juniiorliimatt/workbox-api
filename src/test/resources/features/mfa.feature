# language: pt
Funcionalidade: Autenticação multifator (TOTP)
  Como usuário
  Quero proteger minha conta com um segundo fator
  Para reduzir o risco de comprometimento por senha vazada

  Cenário: Usuário habilita MFA e o próximo login exige o código
    Dado um usuário habilitado "nora09" com senha "S3nh@Forte!"
    E eu tento autenticar com usuário "nora09" e senha "S3nh@Forte!"
    Quando eu habilito o MFA
    E eu confirmo o MFA com o código correto
    Então a resposta é "NO_CONTENT"
    Quando eu tento autenticar com usuário "nora09" e senha "S3nh@Forte!"
    Então a resposta é "OK"
    E o login exige um segundo fator
    Quando eu envio o código correto de MFA
    Então a resposta é "OK"
    E um access_token é retornado

  Cenário: Login com código de MFA incorreto é rejeitado
    Dado um usuário habilitado "oscar10" com senha "S3nh@Forte!"
    E eu tento autenticar com usuário "oscar10" e senha "S3nh@Forte!"
    E eu habilito o MFA
    E eu confirmo o MFA com o código correto
    Quando eu tento autenticar com usuário "oscar10" e senha "S3nh@Forte!"
    E o login exige um segundo fator
    E eu envio o código "000000" de MFA
    Então a resposta é "UNAUTHORIZED"

  Cenário: Usuário desabilita o MFA
    Dado um usuário habilitado "paula11" com senha "S3nh@Forte!"
    E eu tento autenticar com usuário "paula11" e senha "S3nh@Forte!"
    E eu habilito o MFA
    E eu confirmo o MFA com o código correto
    Quando eu desabilito o MFA com o código correto
    Então a resposta é "NO_CONTENT"
    Quando eu tento autenticar com usuário "paula11" e senha "S3nh@Forte!"
    Então a resposta é "OK"
    E um access_token é retornado
