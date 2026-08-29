# language: pt
Funcionalidade: Recuperação de senha por e-mail
  Como usuário que esqueceu a senha
  Quero pedir um link de redefinição por e-mail
  Para recuperar acesso à minha conta sem depender de um admin

  Cenário: Pedido de recuperação não revela se o e-mail existe
    Quando eu peço recuperação de senha para o e-mail "ninguem@example.com"
    Então a resposta é "NO_CONTENT"

  Cenário: Fluxo completo — pedir, receber o token por e-mail e redefinir
    Dado um usuário habilitado "ivan04" com senha "S3nh@Forte!" e e-mail "ivan04@example.com"
    Quando eu peço recuperação de senha para o e-mail "ivan04@example.com"
    Então a resposta é "NO_CONTENT"
    E um e-mail de redefinição foi enviado para "ivan04@example.com"
    Quando eu redefino a senha com o token recebido para "NovaSenh@789"
    Então a resposta é "NO_CONTENT"
    E eu consigo logar com usuário "ivan04" e senha "NovaSenh@789"

  Cenário: Token de reset já usado não pode ser reutilizado
    Dado um usuário habilitado "judy05" com senha "S3nh@Forte!" e e-mail "judy05@example.com"
    Quando eu peço recuperação de senha para o e-mail "judy05@example.com"
    E eu redefino a senha com o token recebido para "NovaSenh@789"
    E eu redefino a senha com o token recebido para "OutraSenha@000"
    Então a resposta é "BAD_REQUEST"

  Cenário: Token inválido é rejeitado
    Quando eu redefino a senha com o token "token-que-nunca-existiu" para "NovaSenh@789"
    Então a resposta é "BAD_REQUEST"
