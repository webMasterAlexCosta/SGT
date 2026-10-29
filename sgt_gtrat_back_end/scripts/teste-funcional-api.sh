#!/usr/bin/env bash
set -euo pipefail

# Teste funcional do contrato HTTP realmente consumido pelo React.
# Pré-requisitos: backend já iniciado, curl e jq.
# Variáveis opcionais:
#   BASE_URL, ADMIN_MATRICULA, GESTOR_LOCAL_MATRICULA, SENHA_INICIAL,
#   CADASTRO_MATRICULA e RESULTADO_TESTE_API.

BASE_URL="${BASE_URL:-http://127.0.0.1:5000}"
ADMIN_MATRICULA="${ADMIN_MATRICULA:-90000001}"
GESTOR_LOCAL_MATRICULA="${GESTOR_LOCAL_MATRICULA:-92000019}"
SENHA_INICIAL="${SENHA_INICIAL:-123456}"
CADASTRO_MATRICULA="${CADASTRO_MATRICULA:-88990011}"
RESULTADO_TESTE_API="${RESULTADO_TESTE_API:-target/relatorios/resultado-teste-funcional-api.tsv}"

for comando in curl jq; do
  command -v "$comando" >/dev/null 2>&1 || {
    echo "Comando obrigatório não encontrado: $comando" >&2
    exit 2
  }
done

temporario="$(mktemp -d)"
limpar() {
  case "$temporario" in
    /tmp/*) rm -rf -- "$temporario" ;;
  esac
}
trap limpar EXIT

mkdir -p "$(dirname "$RESULTADO_TESTE_API")"
: > "$RESULTADO_TESTE_API"
falhas=0
cinco_xx=0
LAST_BODY=""

registrar() {
  local caso="$1" codigo="$2" esperados="$3"
  local resultado="PASSOU"
  if [[ "$codigo" =~ ^5 ]]; then
    resultado="FALHOU_5XX"
    cinco_xx=$((cinco_xx + 1))
    falhas=$((falhas + 1))
  elif ! [[ "$codigo" =~ ^(${esperados})$ ]]; then
    resultado="FALHOU_STATUS"
    falhas=$((falhas + 1))
  fi
  printf '%s\t%s\t%s\t%s\n' "$caso" "$codigo" "$esperados" "$resultado" >> "$RESULTADO_TESTE_API"
  if [ "$resultado" != "PASSOU" ] && [ -s "$LAST_BODY" ]; then
    printf 'Falha em %s: HTTP %s — ' "$caso" "$codigo" >&2
    jq -c 'del(.data.token,.token,.accessToken,.refreshToken)' "$LAST_BODY" 2>/dev/null >&2 \
      || head -c 800 "$LAST_BODY" >&2
    printf '\n' >&2
  fi
}

iniciar_csrf() {
  local papel="$1"
  local cookie="$temporario/cookies-$papel.txt"
  local resposta="$temporario/csrf-$papel.json"
  local codigo
  codigo=$(curl -sS -o "$resposta" -w '%{http_code}' -c "$cookie" \
    "$BASE_URL/api/autenticacao/csrf")
  LAST_BODY="$resposta"
  registrar "$papel.csrf" "$codigo" "200"
  jq -r '.data.headerName // .headerName' "$resposta" > "$temporario/header-$papel.txt"
  jq -r '.data.token // .token' "$resposta" > "$temporario/token-$papel.txt"
}

autenticar() {
  local papel="$1" matricula="$2"
  iniciar_csrf "$papel"
  local cookie="$temporario/cookies-$papel.txt"
  local resposta="$temporario/login-$papel.json"
  local header token codigo
  header=$(<"$temporario/header-$papel.txt")
  token=$(<"$temporario/token-$papel.txt")
  codigo=$(curl -sS -o "$resposta" -w '%{http_code}' -b "$cookie" -c "$cookie" \
    -H "$header: $token" -H 'Content-Type: application/json' \
    --data "{\"matricula\":\"$matricula\",\"password\":\"$SENHA_INICIAL\",\"totp\":\"\"}" \
    "$BASE_URL/api/autenticacao/autenticar")
  LAST_BODY="$resposta"
  registrar "$papel.login" "$codigo" "200"
}

obter() {
  local caso="$1" papel="$2" caminho="$3" esperados="${4:-200}"
  local resposta="$temporario/$(printf '%s' "$caso" | tr '. /?' '_____').json"
  local codigo
  codigo=$(curl -sS -o "$resposta" -w '%{http_code}' \
    -b "$temporario/cookies-$papel.txt" "$BASE_URL$caminho")
  LAST_BODY="$resposta"
  registrar "$caso" "$codigo" "$esperados"
}

mutar() {
  local caso="$1" papel="$2" metodo="$3" caminho="$4" payload="$5" esperados="$6"
  local resposta="$temporario/$(printf '%s' "$caso" | tr '. /?' '_____').json"
  local resposta_csrf="$temporario/csrf-atual-$papel.json"
  local header token codigo
  curl -sS -o "$resposta_csrf" -b "$temporario/cookies-$papel.txt" \
    -c "$temporario/cookies-$papel.txt" "$BASE_URL/api/autenticacao/csrf"
  header=$(jq -r '.data.headerName // .headerName' "$resposta_csrf")
  token=$(jq -r '.data.token // .token' "$resposta_csrf")
  codigo=$(curl -sS -o "$resposta" -w '%{http_code}' -b "$temporario/cookies-$papel.txt" \
    -c "$temporario/cookies-$papel.txt" -X "$metodo" \
    -H "$header: $token" -H "X-User-Password: $SENHA_INICIAL" \
    -H 'Content-Type: application/json' --data "$payload" "$BASE_URL$caminho")
  LAST_BODY="$resposta"
  registrar "$caso" "$codigo" "$esperados"
}

# Cadastro público: listas dependentes e solicitação de acesso.
iniciar_csrf public
obter public.superintendencias public /api/autenticacao/superintendencias
rj_id=$(jq -r '(.data // .)[] | select(.uf == "RJ") | .id' "$LAST_BODY" | head -1)
obter public.gestoes-locais public "/api/autenticacao/gestoes-locais?superintendenciaId=$rj_id"
gtrat_id=$(jq -r '(.data // .)[] | select((.codigo // .nome // "") | test("GTRAT")) | .id' "$LAST_BODY" | head -1)
obter public.configuracao-runtime public /config.json
cadastro=$(jq -nc --argjson se "$rj_id" --argjson gl "$gtrat_id" --arg matricula "$CADASTRO_MATRICULA" '{
  username:"usuario.teste.homologacao", email:"usuario.teste.homologacao@correios.com.br",
  matricula:$matricula, superintendenciaId:$se, gestaoLocalId:$gl
}')
mutar public.cadastro public POST /api/autenticacao/cadastrar "$cadastro" "200"

# Login e leituras centrais dos dois perfis usados pelo React.
autenticar admin "$ADMIN_MATRICULA"
autenticar local "$GESTOR_LOCAL_MATRICULA"

while IFS='|' read -r caso caminho esperados; do
  obter "admin.$caso" admin "$caminho" "$esperados"
done <<'EOF'
perfil|/api/usuarios/meu-perfil|200
sessao|/api/autenticacao/sessao|200
usuarios|/api/usuarios|200
superintendencias|/api/superintendencias|200
gestoes-locais|/api/gestoes-locais|200
bancos|/api/bancos|200
cargos|/api/cargos|200
cache|/api/cache/status|200
contratos|/api/contratos|200
painel|/api/painel/estatisticas|200|202
relatorios|/api/relatorios/filtros|200
auditoria|/api/registros-auditoria|200
agencias|/api/agencias|200
tipos-conta|/api/tipos-conta|200
status-empregados|/api/status-empregados|200
configuracoes-sistema|/api/sistema/configuracoes|200
contas-bloqueadas-por-politica|/api/contas|403
enderecos-bloqueados-por-politica|/api/enderecos|403
telefones-bloqueados-por-politica|/api/telefones|403
EOF

while IFS='|' read -r caso caminho esperados; do
  obter "local.$caso" local "$caminho" "$esperados"
done <<'EOF'
perfil|/api/usuarios/meu-perfil|200
sessao|/api/autenticacao/sessao|200
usuarios|/api/usuarios|200
contratos|/api/contratos|200
tipos-contratacao|/api/tipos-contratacao|200
lotacoes|/api/lotacoes|200
jornadas|/api/jornadas|200
cargos|/api/cargos|200
empregados|/api/empregados/resumo|200
frequencias|/api/frequencias?data=2026-09-13|200
painel|/api/painel/gestao|200|202
pendencias|/api/pendencias/resumo|200
calendario|/api/calendario-eventos?inicio=2026-01-01&fim=2026-12-31|200
frequencias-adicionais|/api/frequencias-adicionais?data=2026-09-13|200
frequencias-excecoes|/api/frequencias-excecoes?data=2026-09-13|200
governanca|/api/governanca-terceiros/resumo|200
motivos-desligamento|/api/motivos-desligamento|200
ocorrencias|/api/ocorrencias?data=2026-09-13|200
perfil-2fa|/api/perfil/2fa/status|200
permissoes-excepcionais|/api/permissoes-excepcionais|200
pessoas|/api/pessoas|200
vinculos-contratuais|/api/vinculos-contratuais|200
relatorios-administrativos|/api/relatorios/administrativos/emissoes|200
painel-filtros|/api/painel/efetivo/filtros|200
historico-empregado-inexistente|/api/historicos-empregados/empregado/999999999|404
calculo-painel-inexistente|/api/painel/calculos/999999999|404
EOF

# Ciclo contratual válido: catálogo, rascunho, edição, composição e publicação.
obter local.perfil-para-contrato local /api/usuarios/meu-perfil
gestao_local_usuario_id=$(jq -r '(.data // .).gestaoLocal.id // (.data // .).gestaoLocalId // empty' "$LAST_BODY")
if [ -z "$gestao_local_usuario_id" ]; then
  echo "O Gestor Local autenticado não informou gestaoLocalId no perfil." >&2
  exit 3
fi

mutar local.tipo-contratacao.criar local POST /api/tipos-contratacao \
  '{"descricao":"SERVIÇO TESTE FUNCIONAL 2026","ativo":true,"matriculaVinculoObrigatoria":false}' "201"
tipo_id=$(jq -r '(.data // .).id // empty' "$LAST_BODY")
obter local.tipo-contratacao.listar-apos-criacao local /api/tipos-contratacao

mutar local.cargo.criar local POST /api/cargos \
  '{"nome":"AUXILIAR TESTE FUNCIONAL 2026","cbo":"TESTE-001","descricao":"Cargo descartável do teste funcional","ativo":true}' "200|201"

obter local.lotacoes-para-contrato local /api/lotacoes
lotacao_id=$(jq -r '(.data // .) as $v | if ($v | type) == "array" then $v[0].id else $v.content[0].id end // empty' "$LAST_BODY")
obter local.jornadas-para-contrato local /api/jornadas
jornada_id=$(jq -r '(.data // .) as $v | if ($v | type) == "array" then $v[0].id else $v.content[0].id end // empty' "$LAST_BODY")
obter local.cargos-para-contrato local /api/cargos
cargo_id=$(jq -r '(.data // .) as $v | if ($v | type) == "array" then $v[0].id else $v.content[0].id end // empty' "$LAST_BODY")

contrato=$(jq -nc --argjson gl "$gestao_local_usuario_id" --argjson tipo "$tipo_id" '{
  numeroContrato:"FUNC-001", anoContrato:2026, descricao:"Contrato criado pelo teste funcional",
  tipoContratacaoId:$tipo, cnpjEmpresa:"12345678000199",
  razaoSocialEmpresa:"Empresa Fictícia de Teste Ltda", nomeFantasiaEmpresa:"Empresa Teste",
  dataInicio:"2026-01-01", dataFim:"2026-12-31", efetivoPrevisto:2,
  gestaoLocalId:$gl, matriculaVinculoObrigatoria:false, ativo:true
}')
mutar local.contrato.criar local POST /api/contratos "$contrato" "201"
contrato_id=$(jq -r '(.data // .).id // empty' "$LAST_BODY")

obter local.contrato.obter local "/api/contratos/$contrato_id"
contrato_atualizado=$(printf '%s' "$contrato" | jq '.descricao="Contrato atualizado pelo teste funcional"')
mutar local.contrato.atualizar local PUT "/api/contratos/$contrato_id" "$contrato_atualizado" "200"

composicao=$(jq -nc --argjson lot "$lotacao_id" --argjson jornada "$jornada_id" --argjson cargo "$cargo_id" '{
  lotacaoId:$lot, jornadaId:$jornada, cargoId:$cargo, efetivoPrevisto:2,
  dataInicioVigencia:"2026-01-01", dataFimVigencia:"2026-12-31", permiteNovaAlocacao:true
}')
mutar local.contrato.composicao-criar local POST "/api/contratos/$contrato_id/composicoes" "$composicao" "201"
mutar local.contrato.publicar local POST "/api/contratos/$contrato_id/publicar" \
  '{"dataPublicacao":"2026-09-13","referenciaSei":"SEI-TESTE-2026-001","justificativa":"Teste funcional automatizado"}' "200"

obter local.contrato.eventos local "/api/contratos/$contrato_id/eventos"
obter local.contrato.precedencias local "/api/contratos/$contrato_id/eventos/precedencias"
obter local.contrato.versoes local "/api/contratos/$contrato_id/versoes"
obter local.contrato.transferencias local "/api/contratos/$contrato_id/transferencias-gestao"
obter local.contrato.posicao-temporal local "/api/contratos/$contrato_id/posicao-temporal?data=2026-09-13"
obter local.contrato.composicoes local "/api/contratos/$contrato_id/composicoes"
obter local.contrato.composicoes-excedentes local "/api/contratos/$contrato_id/composicoes/excedentes?data=2026-09-13"
obter local.contrato.aditamentos local "/api/contratos/$contrato_id/aditamentos"
obter local.contrato.remanejamentos local "/api/contratos/$contrato_id/remanejamentos"
obter local.contrato.documentos local "/api/contratos/$contrato_id/documentos"
obter local.contrato.correcoes-identificador local "/api/contratos/$contrato_id/correcoes-identificador"
obter local.jornada.regras-adicionais local "/api/jornadas/$jornada_id/regras-adicionais"
obter local.fechamento-atual local "/api/frequencias/fechamentos/atual?data=2026-09-13&lotacaoId=$lotacao_id&jornadaId=$jornada_id&contratoIds=$contrato_id" "200|204"
obter local.vinculo.opcoes-alocacao local "/api/vinculos-contratuais/contratos/$contrato_id/opcoes-alocacao?data=2026-09-13" "200"

total=$(wc -l < "$RESULTADO_TESTE_API")
passou=$(awk -F '\t' '$4 == "PASSOU" {n++} END {print n + 0}' "$RESULTADO_TESTE_API")
printf 'Testes funcionais: %s/%s passaram; 5xx=%s; demais falhas=%s\n' \
  "$passou" "$total" "$cinco_xx" "$falhas"
printf 'Resultado detalhado: %s\n' "$RESULTADO_TESTE_API"

[ "$falhas" -eq 0 ]
