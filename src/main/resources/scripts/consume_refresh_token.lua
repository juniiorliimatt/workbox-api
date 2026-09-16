-- KEYS[1] = "refresh:{jti}" do token apresentado em POST /api/auth/refresh.
--
-- Roda como uma única operação atômica no Redis (scripts Lua são single-threaded no
-- servidor) — sem isso, duas chamadas concorrentes de refresh com o mesmo jti poderiam
-- ambas ler "revoked=false" antes de qualquer uma escrever, derrotando a detecção de
-- reuso (rotação vira no-op sob corrida).
--
-- Retorna:
--   {"not_found"}            jti nunca existiu ou já expirou (TTL nativo do Redis)
--   {"reused", familyId}     jti já tinha sido consumido — reuso de token roubado,
--                            família inteira revogada (todos os jti dela apagados)
--   {"ok", familyId}         consumo válido — jti marcado revoked=true

local key = KEYS[1]

if redis.call('EXISTS', key) == 0 then
  return {'not_found'}
end

local revoked = redis.call('HGET', key, 'revoked')
local familyId = redis.call('HGET', key, 'family_id')

if revoked == 'true' then
  local familyKey = 'family:' .. familyId
  local members = redis.call('SMEMBERS', familyKey)
  for _, member in ipairs(members) do
    redis.call('DEL', 'refresh:' .. member)
  end
  redis.call('DEL', familyKey)
  return {'reused', familyId}
end

redis.call('HSET', key, 'revoked', 'true')
return {'ok', familyId}
