# Matriz de endpoints — registro real da candidata

Java 0.0.4. Evidência de registro Spring; sem homologação integral.

**259 combinações método/rota, 43 controladores.** Registro: `continuidade_20261102/evidencias/C23_VERIFY_FINAL/auditoria-http/endpoints-runtime.json`, SHA-256 `e827728f004cb46e7e3b5d92b651d34412f810c6b9f4d57d753301d85b474233`. Fontes de produção confrontadas com o manifesto desse lote.

As chamadas abaixo pertencem exclusivamente a `C23_VERIFY_FINAL`: 11330 linhas atribuídas após comparação de prefixo de bytes. São integrações MockMvc com filtros/serviços/persistência reais. Não representam tráfego TCP do JAR. Contagens históricas da matriz antiga foram preservadas apenas na pasta histórica.

Nenhuma rota do prefixo `/api/governanca-terceiros` aparece neste registro. As quatro operações cadastrais canônicas permanecem em `FornecedorControlador`. A auditoria geral continua registrada. A ausência de rota deve ser complementada pelos testes autenticados de remoção; 401/403 isolado não prova retirada.

| Método | Rota registrada | Controlador/método | Chamadas do lote | Status observados |
|---|---|---|---:|---|
| POST | `/api/autenticacao/alterar-senha-inicial` | `AutenticacaoControlador#alterarSenhaInicial` | 11 | {"200": 3, "401": 4, "400": 1, "403": 3} |
| POST | `/api/autenticacao/autenticar` | `AutenticacaoControlador#autenticar` | 11 | {"200": 7, "403": 1, "401": 2, "202": 1} |
| POST | `/api/autenticacao/cadastrar` | `AutenticacaoControlador#registrar` | 3 | {"400": 2, "200": 1} |
| GET | `/api/autenticacao/csrf` | `AutenticacaoControlador#tokenCsrf` | 9 | {"200": 9} |
| POST | `/api/autenticacao/encerrar-sessao` | `AutenticacaoControlador#encerrarSessao` | 1 | {"200": 1} |
| GET | `/api/autenticacao/gestoes-locais` | `AutenticacaoControlador#listarGestoesLocaisParaCadastro` | 1 | {"200": 1} |
| POST | `/api/autenticacao/recuperar-senha/confirmar` | `AutenticacaoControlador#confirmarRecuperacaoSenha` | 3 | {"200": 1, "400": 1, "503": 1} |
| POST | `/api/autenticacao/recuperar-senha/solicitar` | `AutenticacaoControlador#solicitarRecuperacaoSenha` | 2 | {"200": 2} |
| GET | `/api/autenticacao/recuperar-senha/validar` | `AutenticacaoControlador#validarTokenRecuperacaoSenha` | 1 | {"200": 1} |
| POST | `/api/autenticacao/renovar-sessao` | `AutenticacaoControlador#renovarSessao` | 7 | {"401": 4, "200": 1, "403": 2} |
| GET | `/api/autenticacao/sessao` | `AutenticacaoControlador#informacoesSessao` | 7 | {"401": 5, "200": 2} |
| GET | `/api/autenticacao/superintendencias` | `AutenticacaoControlador#listarSuperintendenciasParaCadastro` | 1 | {"200": 1} |
| GET | `/api/cache/status` | `CacheDiagnosticoControlador#status` | 6 | {"401": 4, "200": 1, "403": 1} |
| GET | `/api/calendario-eventos` | `CalendarioEventoControlador#listar` | 16 | {"401": 4, "200": 10, "403": 1, "400": 1} |
| POST | `/api/calendario-eventos` | `CalendarioEventoControlador#criar` | 32 | {"201": 23, "401": 4, "403": 3, "409": 1, "400": 1} |
| DELETE | `/api/calendario-eventos/{id}` | `CalendarioEventoControlador#excluir` | 9 | {"404": 3, "204": 4, "400": 2} |
| PUT | `/api/calendario-eventos/{id}` | `CalendarioEventoControlador#atualizar` | 18 | {"200": 13, "404": 3, "400": 2} |
| GET | `/api/calendario-eventos/{id}/eventos` | `CalendarioEventoControlador#listarEventos` | 19 | {"200": 18, "404": 1} |
| GET | `/api/cargos` | `CargoControlador#listarTodos` | 6 | {"401": 4, "200": 2} |
| POST | `/api/cargos` | `CargoControlador#criar` | 44 | {"200": 36, "401": 4, "409": 1, "403": 2, "400": 1} |
| DELETE | `/api/cargos/{id}` | `CargoControlador#excluir` | 10 | {"204": 7, "409": 3} |
| GET | `/api/cargos/{id}` | `CargoControlador#buscarPorId` | 65 | {"200": 64, "404": 1} |
| PUT | `/api/cargos/{id}` | `CargoControlador#atualizar` | 21 | {"200": 20, "404": 1} |
| GET | `/api/catalogos-temporais/{tipo}/{id}/disponibilidade` | `DisponibilidadeCatalogoControlador#consultar` | 56 | {"200": 55, "404": 1} |
| POST | `/api/catalogos-temporais/{tipo}/{id}/transicoes` | `DisponibilidadeCatalogoControlador#registrar` | 63 | {"200": 39, "409": 13, "404": 1, "403": 5, "400": 5} |
| POST | `/api/catalogos-temporais/{tipo}/{id}/transicoes/previa` | `DisponibilidadeCatalogoControlador#previa` | 53 | {"200": 39, "404": 1, "403": 5, "409": 8} |
| GET | `/api/contratos` | `ContratoControlador#listarTodos` | 20 | {"401": 5, "403": 1, "200": 12, "409": 2} |
| POST | `/api/contratos` | `ContratoControlador#criar` | 610 | {"201": 587, "400": 5, "401": 4, "403": 7, "409": 7} |
| GET | `/api/contratos/historico-identificador` | `CorrecaoIdentificadorContratoControlador#pesquisar` | 9 | {"401": 4, "200": 5} |
| GET | `/api/contratos/opcoes` | `ContratoControlador#obterOpcoes` | 9 | {"401": 4, "200": 5} |
| GET | `/api/contratos/{contratoId}/aditamentos` | `AditamentoContratualControlador#listar` | 55 | {"200": 52, "404": 3} |
| POST | `/api/contratos/{contratoId}/aditamentos` | `AditamentoContratualControlador#registrar` | 237 | {"201": 208, "409": 23, "400": 4, "404": 1, "403": 1} |
| POST | `/api/contratos/{contratoId}/aditamentos/previa` | `AditamentoContratualControlador#previa` | 281 | {"409": 6, "200": 259, "400": 15, "404": 1} |
| POST | `/api/contratos/{contratoId}/aditamentos/{aditamentoId}/cancelar` | `AditamentoContratualControlador#cancelar` | 17 | {"200": 11, "409": 5, "404": 1} |
| POST | `/api/contratos/{contratoId}/aditamentos/{aditamentoId}/retificar` | `AditamentoContratualControlador#retificar` | 21 | {"200": 14, "409": 6, "404": 1} |
| GET | `/api/contratos/{contratoId}/composicoes` | `ComposicaoContratualControlador#listar` | 135 | {"200": 132, "404": 3} |
| POST | `/api/contratos/{contratoId}/composicoes` | `ComposicaoContratualControlador#criar` | 712 | {"201": 706, "400": 4, "404": 1, "409": 1} |
| GET | `/api/contratos/{contratoId}/composicoes/excedentes` | `ComposicaoContratualControlador#listarExcedentes` | 35 | {"200": 31, "404": 4} |
| DELETE | `/api/contratos/{contratoId}/composicoes/{id}` | `ComposicaoContratualControlador#excluir` | 4 | {"204": 2, "404": 1, "409": 1} |
| PUT | `/api/contratos/{contratoId}/composicoes/{id}` | `ComposicaoContratualControlador#atualizar` | 5 | {"200": 4, "404": 1} |
| GET | `/api/contratos/{contratoId}/correcoes-cnpj` | `CorrecaoCnpjContratoControlador#listar` | 10 | {"200": 7, "404": 3} |
| POST | `/api/contratos/{contratoId}/correcoes-cnpj` | `CorrecaoCnpjContratoControlador#corrigir` | 13 | {"404": 2, "201": 9, "409": 2} |
| GET | `/api/contratos/{contratoId}/correcoes-identificador` | `CorrecaoIdentificadorContratoControlador#listar` | 12 | {"200": 8, "404": 4} |
| POST | `/api/contratos/{contratoId}/correcoes-identificador` | `CorrecaoIdentificadorContratoControlador#corrigir` | 22 | {"201": 19, "409": 1, "404": 1, "403": 1} |
| GET | `/api/contratos/{contratoId}/documentos` | `DocumentoComprobatorioControlador#listar` | 9 | {"200": 7, "403": 1, "404": 1} |
| POST | `/api/contratos/{contratoId}/documentos` | `DocumentoComprobatorioControlador#criar` | 47 | {"201": 41, "404": 2, "400": 2, "415": 2} |
| DELETE | `/api/contratos/{contratoId}/documentos/{documentoId}` | `DocumentoComprobatorioControlador#descartarRascunho` | 6 | {"204": 3, "409": 2, "404": 1} |
| GET | `/api/contratos/{contratoId}/documentos/{documentoId}` | `DocumentoComprobatorioControlador#buscar` | 35 | {"200": 18, "403": 12, "404": 4, "400": 1} |
| GET | `/api/contratos/{contratoId}/documentos/{documentoId}/acessos` | `DocumentoComprobatorioControlador#acessos` | 6 | {"403": 3, "200": 3} |
| GET | `/api/contratos/{contratoId}/documentos/{documentoId}/arquivo` | `DocumentoComprobatorioControlador#baixar` | 24 | {"200": 18, "403": 4, "404": 2} |
| GET | `/api/contratos/{contratoId}/documentos/{documentoId}/eventos` | `DocumentoComprobatorioControlador#eventos` | 7 | {"403": 1, "404": 2, "200": 2, "409": 2} |
| GET | `/api/contratos/{contratoId}/documentos/{documentoId}/evidencias-integridade` | `DocumentoComprobatorioControlador#evidenciasIntegridade` | 20 | {"200": 13, "403": 3, "404": 3, "400": 1} |
| POST | `/api/contratos/{contratoId}/documentos/{documentoId}/invalidar` | `DocumentoComprobatorioControlador#invalidar` | 2 | {"404": 1, "200": 1} |
| GET | `/api/contratos/{contratoId}/documentos/{documentoId}/permissoes` | `DocumentoComprobatorioControlador#permissoes` | 4 | {"403": 2, "200": 2} |
| POST | `/api/contratos/{contratoId}/documentos/{documentoId}/permissoes` | `DocumentoComprobatorioControlador#conceder` | 11 | {"200": 11} |
| POST | `/api/contratos/{contratoId}/documentos/{documentoId}/permissoes/{permissaoId}/revogar` | `DocumentoComprobatorioControlador#revogar` | 4 | {"200": 4} |
| POST | `/api/contratos/{contratoId}/documentos/{documentoId}/retificar-sei` | `DocumentoComprobatorioControlador#retificarSei` | 5 | {"200": 4, "404": 1} |
| POST | `/api/contratos/{contratoId}/documentos/{documentoId}/substituir` | `DocumentoComprobatorioControlador#substituir` | 2 | {"404": 1, "200": 1} |
| PUT | `/api/contratos/{contratoId}/fornecedor/{fornecedorId}` | `FornecedorControlador#vincularFornecedor` | 8 | {"400": 1, "200": 4, "409": 2, "404": 1} |
| GET | `/api/contratos/{contratoId}/provas-historicas` | `ProvaHistoricaContratoControlador#listar` | 18 | {"200": 14, "409": 2, "404": 2} |
| POST | `/api/contratos/{contratoId}/provas-historicas` | `ProvaHistoricaContratoControlador#incorporar` | 83 | {"201": 77, "409": 3, "403": 2, "400": 1} |
| POST | `/api/contratos/{contratoId}/provas-historicas/previa` | `ProvaHistoricaContratoControlador#previa` | 89 | {"200": 75, "400": 7, "403": 1, "404": 3, "409": 3} |
| GET | `/api/contratos/{contratoId}/remanejamentos` | `RemanejamentoContratualControlador#listar` | 51 | {"200": 48, "404": 3} |
| POST | `/api/contratos/{contratoId}/remanejamentos` | `RemanejamentoContratualControlador#registrar` | 63 | {"201": 52, "409": 6, "400": 4, "404": 1} |
| POST | `/api/contratos/{contratoId}/remanejamentos/previa` | `RemanejamentoContratualControlador#previa` | 75 | {"200": 69, "400": 4, "404": 1, "409": 1} |
| POST | `/api/contratos/{contratoId}/remanejamentos/{remanejamentoId}/cancelar` | `RemanejamentoContratualControlador#cancelar` | 14 | {"200": 10, "409": 3, "404": 1} |
| POST | `/api/contratos/{contratoId}/remanejamentos/{remanejamentoId}/retificar` | `RemanejamentoContratualControlador#retificar` | 6 | {"200": 4, "409": 1, "404": 1} |
| DELETE | `/api/contratos/{id}` | `ContratoControlador#excluir` | 10 | {"409": 6, "204": 3, "404": 1} |
| GET | `/api/contratos/{id}` | `ContratoControlador#buscarPorId` | 82 | {"200": 70, "404": 12} |
| PUT | `/api/contratos/{id}` | `ContratoControlador#atualizar` | 20 | {"400": 1, "200": 15, "409": 3, "404": 1} |
| POST | `/api/contratos/{id}/anular` | `ContratoControlador#anular` | 20 | {"200": 16, "400": 2, "409": 1, "404": 1} |
| POST | `/api/contratos/{id}/cancelar-futuro` | `ContratoControlador#cancelarFuturo` | 6 | {"409": 1, "200": 3, "404": 1, "400": 1} |
| POST | `/api/contratos/{id}/corrigir-anulacao` | `ContratoControlador#corrigirAnulacao` | 6 | {"200": 5, "404": 1} |
| POST | `/api/contratos/{id}/encerrar-antecipadamente` | `ContratoControlador#encerrarAntecipadamente` | 12 | {"200": 8, "409": 2, "400": 1, "404": 1} |
| GET | `/api/contratos/{id}/eventos` | `ContratoControlador#listarEventos` | 80 | {"200": 77, "404": 3} |
| GET | `/api/contratos/{id}/eventos/precedencias` | `ContratoControlador#listarPrecedencias` | 27 | {"200": 24, "404": 3} |
| POST | `/api/contratos/{id}/inativar` | `ContratoControlador#inativar` | 41 | {"200": 36, "409": 4, "404": 1} |
| GET | `/api/contratos/{id}/posicao-temporal` | `ContratoControlador#consultarPosicaoTemporal` | 257 | {"200": 251, "404": 6} |
| POST | `/api/contratos/{id}/previas/publicar` | `ContratoControlador#previaPublicacao` | 453 | {"200": 452, "404": 1} |
| POST | `/api/contratos/{id}/previas/{operacao}` | `ContratoControlador#previaCicloVida` | 98 | {"200": 92, "404": 6} |
| POST | `/api/contratos/{id}/publicar` | `ContratoControlador#publicar` | 564 | {"200": 537, "400": 21, "404": 1, "409": 5} |
| POST | `/api/contratos/{id}/reativar` | `ContratoControlador#reativar` | 16 | {"200": 14, "404": 1, "409": 1} |
| GET | `/api/contratos/{id}/transferencias-gestao` | `ContratoControlador#listarTransferenciasGestao` | 23 | {"200": 20, "404": 3} |
| POST | `/api/contratos/{id}/transferencias-gestao/previa` | `ContratoControlador#previaTransferenciaGestao` | 24 | {"200": 24} |
| POST | `/api/contratos/{id}/transferencias-gestao/{transferenciaId}/cancelar` | `ContratoControlador#cancelarTransferenciaGestao` | 7 | {"200": 3, "409": 3, "404": 1} |
| POST | `/api/contratos/{id}/transferir-gestao` | `ContratoControlador#transferirGestao` | 169 | {"200": 151, "403": 2, "400": 5, "409": 10, "404": 1} |
| GET | `/api/contratos/{id}/versoes` | `ContratoControlador#listarVersoes` | 37 | {"200": 32, "404": 3, "409": 2} |
| GET | `/api/empregados` | `EmpregadoControlador#listarTodos` | 27 | {"401": 4, "200": 22, "403": 1} |
| GET | `/api/empregados/filtrados-frequencia` | `EmpregadoControlador#obterEmpregadosParaFrequencia` | 23 | {"401": 4, "200": 15, "403": 3, "400": 1} |
| GET | `/api/empregados/resumo` | `EmpregadoControlador#obterResumo` | 19 | {"401": 4, "200": 13, "403": 1, "400": 1} |
| GET | `/api/empregados/{id}` | `EmpregadoControlador#buscarPorId` | 38 | {"200": 29, "404": 9} |
| GET | `/api/empregados/{id}/historico-contratos` | `EmpregadoControlador#obterHistoricoContratos` | 8 | {"200": 8} |
| GET | `/api/fornecedores` | `FornecedorControlador#fornecedores` | 21 | {"200": 8, "401": 6, "403": 7} |
| POST | `/api/fornecedores` | `FornecedorControlador#criarFornecedor` | 35 | {"200": 23, "401": 4, "403": 7, "400": 1} |
| PUT | `/api/fornecedores/{id}` | `FornecedorControlador#atualizarFornecedor` | 6 | {"200": 6} |
| GET | `/api/frequencias` | `FrequenciaControlador#buscarPorData` | 35 | {"200": 30, "400": 1, "401": 4} |
| POST | `/api/frequencias` | `FrequenciaControlador#salvarOuAtualizar` | 372 | {"200": 340, "409": 11, "400": 4, "403": 13, "401": 4} |
| GET | `/api/frequencias-adicionais` | `FrequenciaAdicionalControlador#listar` | 32 | {"200": 26, "401": 4, "400": 2} |
| POST | `/api/frequencias-adicionais` | `FrequenciaAdicionalControlador#criar` | 44 | {"201": 31, "401": 4, "403": 5, "400": 2, "409": 2} |
| DELETE | `/api/frequencias-adicionais/{id}` | `FrequenciaAdicionalControlador#excluir` | 11 | {"403": 3, "204": 5, "404": 1, "400": 2} |
| PUT | `/api/frequencias-adicionais/{id}` | `FrequenciaAdicionalControlador#atualizar` | 28 | {"200": 21, "403": 3, "404": 1, "400": 3} |
| GET | `/api/frequencias-adicionais/{id}/eventos` | `FrequenciaAdicionalControlador#listarEventos` | 54 | {"200": 50, "404": 4} |
| GET | `/api/frequencias-excecoes` | `FrequenciaExcecaoControlador#listarAtivasPorData` | 22 | {"200": 17, "401": 4, "400": 1} |
| POST | `/api/frequencias-excecoes` | `FrequenciaExcecaoControlador#criar` | 40 | {"201": 27, "401": 4, "403": 6, "409": 3} |
| DELETE | `/api/frequencias-excecoes/{id}` | `FrequenciaExcecaoControlador#excluir` | 12 | {"404": 1, "403": 3, "204": 6, "400": 2} |
| PUT | `/api/frequencias-excecoes/{id}` | `FrequenciaExcecaoControlador#atualizar` | 26 | {"200": 19, "403": 4, "400": 3} |
| GET | `/api/frequencias-excecoes/{id}/eventos` | `FrequenciaExcecaoControlador#listarEventos` | 41 | {"200": 37, "404": 4} |
| POST | `/api/frequencias/fechamentos` | `FechamentoFrequenciaControlador#criar` | 15 | {"401": 4, "403": 5, "202": 4, "400": 2} |
| GET | `/api/frequencias/fechamentos/atual` | `FechamentoFrequenciaControlador#buscarAtual` | 6 | {"401": 4, "204": 1, "200": 1} |
| GET | `/api/frequencias/fechamentos/{id}` | `FechamentoFrequenciaControlador#buscar` | 58 | {"200": 54, "404": 4} |
| POST | `/api/frequencias/fechamentos/{id}/cancelamento` | `FechamentoFrequenciaControlador#cancelar` | 4 | {"404": 2, "202": 1, "409": 1} |
| POST | `/api/frequencias/fechamentos/{id}/reprocessamento` | `FechamentoFrequenciaControlador#reprocessar` | 4 | {"404": 2, "202": 1, "409": 1} |
| GET | `/api/frequencias/intervalo` | `FrequenciaControlador#obterIntervalo` | 29 | {"200": 23, "400": 1, "403": 1, "401": 4} |
| POST | `/api/frequencias/lote` | `FrequenciaControlador#salvarOuAtualizarLote` | 15 | {"400": 3, "200": 6, "401": 4, "403": 2} |
| GET | `/api/frequencias/mensal` | `FrequenciaControlador#obterMensal` | 16 | {"200": 11, "400": 1, "401": 4} |
| GET | `/api/frequencias/{id}/eventos` | `FrequenciaControlador#listarEventos` | 106 | {"200": 101, "404": 4, "409": 1} |
| GET | `/api/frequencias/{id}/evidencias-integridade` | `FrequenciaControlador#evidenciasIntegridade` | 17 | {"200": 13, "404": 4} |
| GET | `/api/gestoes-locais` | `GestaoLocalControlador#listar` | 7 | {"401": 4, "200": 3} |
| POST | `/api/gestoes-locais` | `GestaoLocalControlador#criar` | 16 | {"201": 3, "401": 4, "400": 1, "403": 7, "409": 1} |
| DELETE | `/api/gestoes-locais/{id}` | `GestaoLocalControlador#excluir` | 1 | {"409": 1} |
| PUT | `/api/gestoes-locais/{id}` | `GestaoLocalControlador#atualizar` | 5 | {"200": 4, "400": 1} |
| GET | `/api/historicos-empregados` | `HistoricoEmpregadoControlador#listarTodos` | 15 | {"200": 10, "401": 4, "403": 1} |
| GET | `/api/historicos-empregados/empregado/{id}` | `HistoricoEmpregadoControlador#buscarPorEmpregado` | 29 | {"200": 29} |
| GET | `/api/historicos-empregados/empregado/{id}/integridade` | `HistoricoEmpregadoControlador#verificarIntegridade` | 8 | {"200": 7, "403": 1} |
| GET | `/api/historicos-empregados/{id}` | `HistoricoEmpregadoControlador#buscarPorId` | 26 | {"200": 18, "403": 8} |
| GET | `/api/jornadas` | `JornadaControlador#listar` | 6 | {"401": 4, "200": 2} |
| POST | `/api/jornadas` | `JornadaControlador#criar` | 16 | {"401": 4, "201": 9, "403": 2, "400": 1} |
| DELETE | `/api/jornadas/{id}` | `JornadaControlador#excluir` | 7 | {"409": 3, "204": 4} |
| GET | `/api/jornadas/{id}` | `JornadaControlador#obter` | 53 | {"200": 52, "404": 1} |
| PUT | `/api/jornadas/{id}` | `JornadaControlador#atualizar` | 39 | {"200": 38, "404": 1} |
| GET | `/api/jornadas/{jornadaId}/regras-adicionais` | `RegraAdicionalControlador#listar` | 2 | {"200": 1, "404": 1} |
| POST | `/api/jornadas/{jornadaId}/regras-adicionais` | `RegraAdicionalControlador#criar` | 5 | {"201": 3, "404": 1, "400": 1} |
| GET | `/api/lotacoes` | `LotacaoControlador#listarTodos` | 6 | {"401": 4, "200": 2} |
| POST | `/api/lotacoes` | `LotacaoControlador#criar` | 18 | {"401": 4, "201": 11, "403": 2, "400": 1} |
| GET | `/api/lotacoes/opcoes` | `LotacaoControlador#obterOpcoes` | 5 | {"401": 4, "200": 1} |
| DELETE | `/api/lotacoes/{id}` | `LotacaoControlador#excluir` | 7 | {"409": 3, "204": 4} |
| GET | `/api/lotacoes/{id}` | `LotacaoControlador#buscarPorId` | 57 | {"200": 56, "404": 1} |
| PUT | `/api/lotacoes/{id}` | `LotacaoControlador#atualizar` | 43 | {"200": 42, "404": 1} |
| GET | `/api/motivos-desligamento` | `MotivoDesligamentoControlador#listar` | 6 | {"401": 4, "200": 2} |
| POST | `/api/motivos-desligamento` | `MotivoDesligamentoControlador#criar` | 28 | {"201": 21, "401": 4, "403": 2, "400": 1} |
| GET | `/api/motivos-desligamento/{id}` | `MotivoDesligamentoControlador#buscarPorId` | 18 | {"200": 17, "404": 1} |
| PUT | `/api/motivos-desligamento/{id}` | `MotivoDesligamentoControlador#atualizar` | 15 | {"200": 13, "404": 1, "403": 1} |
| GET | `/api/motivos-desligamento/{id}/contexto-historico` | `ProvaMotivoHistoricoControlador#consultar` | 16 | {"200": 16} |
| GET | `/api/motivos-desligamento/{id}/provas-historicas` | `ProvaMotivoHistoricoControlador#listar` | 7 | {"200": 7} |
| POST | `/api/motivos-desligamento/{id}/provas-historicas` | `ProvaMotivoHistoricoControlador#registrar` | 12 | {"201": 9, "409": 3} |
| POST | `/api/motivos-desligamento/{id}/provas-historicas/previa` | `ProvaMotivoHistoricoControlador#previa` | 18 | {"200": 9, "409": 2, "403": 2, "404": 3, "400": 2} |
| GET | `/api/ocorrencias` | `OcorrenciaControlador#listarTodos` | 16 | {"200": 10, "401": 4, "403": 1, "400": 1} |
| POST | `/api/ocorrencias` | `OcorrenciaControlador#criar` | 50 | {"201": 34, "401": 4, "400": 5, "403": 6, "409": 1} |
| GET | `/api/ocorrencias/anexos/{anexoId}` | `OcorrenciaControlador#baixarAnexo` | 43 | {"200": 23, "403": 19, "404": 1} |
| DELETE | `/api/ocorrencias/{id}` | `OcorrenciaControlador#excluir` | 16 | {"204": 11, "403": 4, "404": 1} |
| GET | `/api/ocorrencias/{id}` | `OcorrenciaControlador#consultarHistorico` | 39 | {"200": 28, "403": 10, "404": 1} |
| GET | `/api/painel/atividades` | `PainelControlador#obterAtividadesRecentes` | 16 | {"401": 4, "200": 10, "400": 1, "403": 1} |
| POST | `/api/painel/calculos` | `PainelCalculoControlador#solicitar` | 14 | {"202": 4, "401": 4, "200": 1, "400": 2, "403": 3} |
| GET | `/api/painel/calculos/{id}` | `PainelCalculoControlador#consultar` | 23 | {"200": 14, "403": 8, "404": 1} |
| GET | `/api/painel/efetivo` | `PainelControlador#obterIndicadorEfetivo` | 12 | {"200": 6, "401": 4, "400": 1, "403": 1} |
| GET | `/api/painel/efetivo/filtros` | `PainelControlador#obterFiltrosEfetivo` | 8 | {"401": 4, "200": 2, "400": 1, "403": 1} |
| GET | `/api/painel/estatisticas` | `PainelControlador#obterEstatisticas` | 7 | {"401": 4, "403": 2, "200": 1} |
| GET | `/api/painel/gestao` | `PainelControlador#obterGestao` | 13 | {"200": 7, "401": 4, "400": 1, "403": 1} |
| GET | `/api/painel/ranking-lotacoes` | `PainelControlador#obterRankingLotacoes` | 8 | {"401": 4, "403": 1, "200": 2, "400": 1} |
| GET | `/api/pendencias` | `PendenciaAdministrativaControlador#listar` | 38 | {"200": 33, "400": 1, "401": 4} |
| GET | `/api/pendencias/resumo` | `PendenciaAdministrativaControlador#resumo` | 13 | {"200": 8, "400": 1, "401": 4} |
| POST | `/api/pendencias/{id}/confirmar-resolucao` | `PendenciaAdministrativaControlador#confirmarResolucao` | 24 | {"403": 5, "409": 8, "200": 9, "404": 2} |
| POST | `/api/pendencias/{id}/iniciar-tratamento` | `PendenciaAdministrativaControlador#iniciar` | 35 | {"404": 6, "200": 23, "409": 1, "403": 5} |
| DELETE | `/api/perfil/2fa` | `PerfilControlador#desativarSegundoFator` | 7 | {"401": 4, "200": 1, "403": 2} |
| POST | `/api/perfil/2fa/ativar` | `PerfilControlador#ativarSegundoFator` | 8 | {"401": 4, "400": 1, "200": 1, "403": 2} |
| POST | `/api/perfil/2fa/configurar` | `PerfilControlador#configurarSegundoFator` | 8 | {"401": 4, "400": 1, "200": 1, "403": 2} |
| GET | `/api/perfil/2fa/status` | `PerfilControlador#statusSegundoFator` | 6 | {"401": 4, "200": 2} |
| POST | `/api/perfil/alterar-senha` | `PerfilControlador#alterarSenha` | 7 | {"401": 4, "200": 1, "403": 2} |
| POST | `/api/perfil/avatar` | `PerfilControlador#enviarAvatar` | 8 | {"401": 4, "200": 1, "400": 1, "403": 2} |
| GET | `/api/perfil/avatar/{nomeArquivo}` | `PerfilControlador#obterAvatar` | 2 | {"200": 1, "404": 1} |
| POST | `/api/perfil/validar-senha` | `PerfilControlador#validarSenha` | 8 | {"401": 4, "200": 1, "400": 1, "403": 2} |
| GET | `/api/permissoes-excepcionais` | `PermissaoExcepcionalControlador#listar` | 6 | {"401": 4, "200": 2} |
| POST | `/api/permissoes-excepcionais` | `PermissaoExcepcionalControlador#conceder` | 292 | {"201": 279, "409": 3, "401": 4, "403": 3, "400": 3} |
| POST | `/api/permissoes-excepcionais/{id}/revogar` | `PermissaoExcepcionalControlador#revogar` | 6 | {"200": 5, "409": 1} |
| GET | `/api/permissoes-excepcionais/{id}/usos` | `PermissaoExcepcionalControlador#usos` | 14 | {"200": 12, "404": 1, "403": 1} |
| GET | `/api/pessoas` | `PessoaControlador#listar` | 16 | {"401": 4, "200": 12} |
| GET | `/api/pessoas/{id}` | `PessoaControlador#buscar` | 81 | {"200": 75, "404": 6} |
| GET | `/api/pessoas/{id}/correcoes-identidade` | `PessoaControlador#correcoes` | 22 | {"200": 17, "404": 5} |
| POST | `/api/pessoas/{id}/correcoes-identidade` | `PessoaControlador#corrigir` | 54 | {"200": 47, "400": 4, "409": 2, "404": 1} |
| PUT | `/api/pessoas/{id}/dados-opcionais` | `PessoaControlador#atualizarOpcionais` | 44 | {"200": 37, "404": 5, "400": 2} |
| GET | `/api/pessoas/{id}/dados-restritos` | `PessoaControlador#buscarDadosRestritos` | 6 | {"200": 2, "404": 1, "403": 3} |
| GET | `/api/pessoas/{pessoaId}/nome-historico` | `ProvaNomePessoaHistoricoControlador#consultar` | 37 | {"200": 35, "404": 2} |
| GET | `/api/pessoas/{pessoaId}/provas-nome` | `ProvaNomePessoaHistoricoControlador#listar` | 14 | {"200": 14} |
| POST | `/api/pessoas/{pessoaId}/provas-nome` | `ProvaNomePessoaHistoricoControlador#registrar` | 31 | {"201": 27, "409": 4} |
| POST | `/api/pessoas/{pessoaId}/provas-nome/previa` | `ProvaNomePessoaHistoricoControlador#previa` | 40 | {"200": 32, "403": 1, "404": 2, "400": 4, "409": 1} |
| GET | `/api/registros-auditoria` | `RegistroAuditoriaControlador#pesquisar` | 12 | {"200": 3, "401": 4, "403": 5} |
| GET | `/api/registros-auditoria/{id}` | `RegistroAuditoriaControlador#buscarPorId` | 4 | {"200": 3, "404": 1} |
| DELETE | `/api/regras-adicionais/{id}` | `RegraAdicionalControlador#excluir` | 2 | {"404": 1, "204": 1} |
| PUT | `/api/regras-adicionais/{id}` | `RegraAdicionalControlador#atualizar` | 3 | {"200": 2, "404": 1} |
| GET | `/api/relatorios/absenteismo` | `RelatorioControlador#gerarRelatorioAbsenteismo` | 17 | {"200": 10, "401": 4, "404": 1, "400": 1, "403": 1} |
| GET | `/api/relatorios/absenteismo/exportacao` | `RelatorioControlador#exportarAbsenteismo` | 14 | {"200": 7, "401": 4, "400": 2, "403": 1} |
| POST | `/api/relatorios/absenteismo/exportacao/solicitacao` | `RelatorioControlador#solicitarExportacaoAbsenteismo` | 11 | {"202": 2, "401": 4, "403": 3, "400": 2} |
| GET | `/api/relatorios/absenteismo/paginado` | `RelatorioControlador#gerarRelatorioAbsenteismoPaginado` | 12 | {"401": 4, "200": 5, "404": 1, "400": 1, "403": 1} |
| GET | `/api/relatorios/absenteismo/resumo` | `RelatorioControlador#gerarResumoAbsenteismo` | 12 | {"401": 4, "200": 5, "404": 1, "400": 1, "403": 1} |
| GET | `/api/relatorios/administrativos/emissoes` | `RelatorioAdministrativoControlador#listar` | 11 | {"200": 6, "400": 1, "401": 4} |
| POST | `/api/relatorios/administrativos/emissoes` | `RelatorioAdministrativoControlador#emitir` | 79 | {"201": 71, "403": 3, "409": 1, "401": 4} |
| POST | `/api/relatorios/administrativos/emissoes/conhecimento` | `RelatorioAdministrativoControlador#conhecidaAte` | 10 | {"200": 3, "404": 1, "401": 4, "403": 2} |
| GET | `/api/relatorios/administrativos/emissoes/{id}` | `RelatorioAdministrativoControlador#obter` | 56 | {"200": 53, "404": 3} |
| GET | `/api/relatorios/administrativos/emissoes/{id}/comparacao-vigente` | `RelatorioAdministrativoControlador#comparar` | 28 | {"200": 22, "404": 6} |
| POST | `/api/relatorios/administrativos/previa` | `RelatorioAdministrativoControlador#previa` | 34 | {"200": 26, "403": 3, "400": 1, "401": 4} |
| GET | `/api/relatorios/exportacoes/{id}` | `RelatorioControlador#consultarExportacao` | 39 | {"200": 30, "403": 7, "404": 1, "400": 1} |
| GET | `/api/relatorios/exportacoes/{id}/arquivo` | `RelatorioControlador#baixarExportacao` | 20 | {"200": 9, "409": 5, "404": 1, "403": 5} |
| GET | `/api/relatorios/filtros` | `RelatorioControlador#filtros` | 8 | {"401": 4, "200": 2, "400": 1, "403": 1} |
| GET | `/api/relatorios/frequencia-consolidada` | `RelatorioControlador#frequenciaConsolidada` | 60 | {"200": 45, "409": 8, "401": 4, "404": 1, "400": 1, "403": 1} |
| GET | `/api/relatorios/frequencia-consolidada/exportacao` | `RelatorioControlador#exportarFrequenciaConsolidada` | 19 | {"200": 13, "401": 4, "400": 1, "403": 1} |
| POST | `/api/relatorios/frequencia-consolidada/exportacao/solicitacao` | `RelatorioControlador#solicitarExportacaoFrequencia` | 16 | {"202": 8, "401": 4, "403": 3, "400": 1} |
| GET | `/api/relatorios/frequencia-consolidada/paginada` | `RelatorioControlador#frequenciaConsolidadaPaginada` | 17 | {"401": 4, "200": 10, "404": 1, "400": 1, "403": 1} |
| GET | `/api/relatorios/horas-trabalhadas` | `RelatorioControlador#gerarRelatorioHorasTrabalhadas` | 12 | {"401": 4, "200": 5, "404": 1, "400": 1, "403": 1} |
| GET | `/api/relatorios/terceirizados/consolidado` | `RelatorioControlador#consolidadoTerceirizados` | 12 | {"401": 4, "200": 6, "400": 1, "403": 1} |
| GET | `/api/relatorios/terceirizados/exportacao` | `RelatorioControlador#exportarTerceirizados` | 11 | {"200": 5, "401": 4, "400": 1, "403": 1} |
| POST | `/api/relatorios/terceirizados/exportacao/solicitacao` | `RelatorioControlador#solicitarExportacaoTerceirizados` | 9 | {"401": 4, "403": 3, "400": 1, "202": 1} |
| GET | `/api/sistema/configuracoes` | `ConfiguracaoSistemaControlador#listarConfiguracoes` | 10 | {"401": 4, "200": 1, "403": 5} |
| POST | `/api/sistema/configuracoes` | `ConfiguracaoSistemaControlador#salvarConfiguracao` | 14 | {"401": 4, "200": 2, "400": 1, "403": 7} |
| GET | `/api/status-empregados` | `StatusEmpregadoControlador#listarTodos` | 5 | {"401": 4, "200": 1} |
| POST | `/api/status-empregados` | `StatusEmpregadoControlador#criar` | 9 | {"401": 4, "403": 2, "200": 2, "400": 1} |
| DELETE | `/api/status-empregados/{id}` | `StatusEmpregadoControlador#excluir` | 1 | {"204": 1} |
| GET | `/api/status-empregados/{id}` | `StatusEmpregadoControlador#buscarPorId` | 3 | {"200": 1, "404": 2} |
| PUT | `/api/status-empregados/{id}` | `StatusEmpregadoControlador#atualizar` | 2 | {"200": 2} |
| GET | `/api/superintendencias` | `SuperintendenciaControlador#listar` | 5 | {"401": 4, "200": 1} |
| POST | `/api/superintendencias` | `SuperintendenciaControlador#criar` | 16 | {"401": 4, "201": 4, "409": 1, "403": 7} |
| DELETE | `/api/superintendencias/{id}` | `SuperintendenciaControlador#excluir` | 1 | {"409": 1} |
| PUT | `/api/superintendencias/{id}` | `SuperintendenciaControlador#atualizar` | 2 | {"200": 2} |
| POST | `/api/superintendencias/{id}/ativar` | `SuperintendenciaControlador#ativar` | 2 | {"200": 2} |
| GET | `/api/tipos-contratacao` | `TipoContratacaoControlador#listar` | 6 | {"401": 4, "200": 2} |
| POST | `/api/tipos-contratacao` | `TipoContratacaoControlador#criar` | 15 | {"401": 4, "201": 8, "403": 2, "400": 1} |
| GET | `/api/tipos-contratacao/{id}` | `TipoContratacaoControlador#buscarPorId` | 6 | {"200": 5, "404": 1} |
| PUT | `/api/tipos-contratacao/{id}` | `TipoContratacaoControlador#atualizar` | 15 | {"200": 13, "404": 1, "403": 1} |
| GET | `/api/usuarios` | `UsuarioAplicacaoControlador#listarTodos` | 7 | {"401": 4, "200": 1, "403": 2} |
| POST | `/api/usuarios` | `UsuarioAplicacaoControlador#criar` | 11 | {"401": 4, "201": 2, "403": 4, "400": 1} |
| GET | `/api/usuarios/meu-perfil` | `UsuarioAplicacaoControlador#obterMeuPerfil` | 6 | {"401": 4, "200": 2} |
| DELETE | `/api/usuarios/{id}` | `UsuarioAplicacaoControlador#excluir` | 4 | {"200": 4} |
| PUT | `/api/usuarios/{id}` | `UsuarioAplicacaoControlador#atualizar` | 67 | {"200": 65, "404": 1, "403": 1} |
| POST | `/api/usuarios/{id}/ativar` | `UsuarioAplicacaoControlador#ativar` | 3 | {"200": 3} |
| POST | `/api/usuarios/{id}/conceder-acesso-retroativo` | `UsuarioAplicacaoControlador#concederAcessoRetroativo` | 5 | {"200": 4, "404": 1} |
| POST | `/api/usuarios/{id}/redefinir-senha` | `UsuarioAplicacaoControlador#redefinirSenha` | 2 | {"200": 2} |
| POST | `/api/usuarios/{id}/revogar-acesso-retroativo` | `UsuarioAplicacaoControlador#revogarAcessoRetroativo` | 4 | {"200": 3, "404": 1} |
| GET | `/api/vinculos-contratuais` | `VinculoContratualControlador#listar` | 19 | {"200": 14, "401": 4, "400": 1} |
| POST | `/api/vinculos-contratuais` | `VinculoContratualControlador#cadastrar` | 366 | {"201": 310, "409": 29, "401": 4, "404": 1, "403": 6, "400": 16} |
| GET | `/api/vinculos-contratuais/contratos/{contratoId}/opcoes-alocacao` | `VinculoContratualControlador#opcoesAlocacao` | 16 | {"200": 13, "404": 3} |
| POST | `/api/vinculos-contratuais/contratos/{contratoId}/tratar-fim-natural` | `VinculoContratualControlador#tratarFimNatural` | 9 | {"200": 7, "404": 1, "409": 1} |
| GET | `/api/vinculos-contratuais/pessoas/{pessoaId}/posicao` | `VinculoContratualControlador#posicao` | 58 | {"200": 50, "409": 3, "404": 5} |
| GET | `/api/vinculos-contratuais/{id}` | `VinculoContratualControlador#buscar` | 72 | {"200": 66, "404": 6} |
| GET | `/api/vinculos-contratuais/{id}/alocacoes` | `VinculoContratualControlador#alocacoes` | 24 | {"200": 18, "404": 5, "400": 1} |
| POST | `/api/vinculos-contratuais/{id}/alocacoes` | `VinculoContratualControlador#alterarAlocacao` | 53 | {"200": 49, "409": 3, "404": 1} |
| GET | `/api/vinculos-contratuais/{id}/alocacoes/eventos` | `VinculoContratualControlador#eventosAlocacao` | 61 | {"200": 42, "404": 19} |
| GET | `/api/vinculos-contratuais/{id}/alocacoes/posicao` | `VinculoContratualControlador#posicaoAlocacao` | 104 | {"200": 99, "404": 5} |
| POST | `/api/vinculos-contratuais/{id}/alocacoes/{alocacaoId}/cancelar-programacao` | `VinculoContratualControlador#cancelarAlocacao` | 6 | {"409": 2, "200": 3, "404": 1} |
| POST | `/api/vinculos-contratuais/{id}/alocacoes/{alocacaoId}/retificar` | `VinculoContratualControlador#retificarAlocacao` | 4 | {"200": 3, "404": 1} |
| POST | `/api/vinculos-contratuais/{id}/alterar-matricula` | `VinculoContratualControlador#alterarMatricula` | 18 | {"200": 16, "404": 1, "400": 1} |
| POST | `/api/vinculos-contratuais/{id}/cancelar-desligamento-programado` | `VinculoContratualControlador#cancelarDesligamento` | 8 | {"409": 3, "200": 4, "404": 1} |
| POST | `/api/vinculos-contratuais/{id}/cancelar-programacao` | `VinculoContratualControlador#cancelarProgramacao` | 9 | {"200": 7, "409": 1, "404": 1} |
| POST | `/api/vinculos-contratuais/{id}/desligar` | `VinculoContratualControlador#desligar` | 56 | {"200": 48, "409": 6, "404": 1, "400": 1} |
| GET | `/api/vinculos-contratuais/{id}/eventos` | `VinculoContratualControlador#eventos` | 103 | {"200": 96, "404": 7} |
| POST | `/api/vinculos-contratuais/{id}/previas/alocacoes` | `VinculoContratualControlador#previaAlocacao` | 57 | {"200": 55, "404": 1, "400": 1} |
| POST | `/api/vinculos-contratuais/{id}/previas/retificacao-desligamento` | `VinculoContratualControlador#previaDesligamento` | 42 | {"200": 31, "409": 9, "400": 1, "404": 1} |
| POST | `/api/vinculos-contratuais/{id}/previas/retificacao-inicio` | `VinculoContratualControlador#previaInicio` | 30 | {"200": 28, "404": 1, "400": 1} |
| POST | `/api/vinculos-contratuais/{id}/retificar-desligamento` | `VinculoContratualControlador#retificarDesligamento` | 32 | {"409": 9, "200": 22, "404": 1} |
| POST | `/api/vinculos-contratuais/{id}/retificar-inicio` | `VinculoContratualControlador#retificarInicio` | 30 | {"409": 10, "200": 18, "400": 1, "404": 1} |
| GET | `/config.json` | `ConfiguracaoSistemaControlador#obterConfiguracaoPublica` | 2 | {"200": 2} |
