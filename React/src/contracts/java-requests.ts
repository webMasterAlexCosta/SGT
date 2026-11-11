/** Jackson request bodies. Omitted unvalidated fields use the Java defaults; bean constraints remain required. */
import type * as DTO from './java';

export interface RequestAcaoCicloVidaContratoDTO {
  dataEfetiva?: string | null;
  dataAto?: string | null;
  motivo?: string | null;
  referenciaSei?: string | null;
  eventoPrecedenteId?: number | null;
  relacaoPrecedencia?: DTO.TipoRelacaoPrecedenciaNegocial | null;
  ordemDocumental?: number | null;
  tokenPrevia?: string | null;
  confirmarImpacto?: boolean | null;
}

export interface RequestAcaoDocumentoComprobatorioRequisicao {
  motivo?: string | null;
  autorizacaoAdministrativa?: string | null;
}

export interface RequestAcaoVinculoContratualRequisicao {
  justificativa?: string | null;
  evidencia?: string | null;
  autorizacao?: string | null;
  confirmarImpacto?: boolean | null;
  tokenPrevia?: string | null;
  confirmarInclusaoExcedente?: boolean | null;
}

export interface RequestAcessoDocumentoRestritoDTO {
  id?: number | null;
  documentoId?: number | null;
  tipoAcesso?: DTO.TipoAcessoDocumento | null;
  finalidade?: string | null;
  usuarioId?: number | null;
  usuarioUsername?: string | null;
  perfisFotografia?: string | null;
  permissaoId?: number | null;
  acessadoEm?: string | null;
}

export interface RequestAditamentoComposicaoRequisicao {
  composicaoId?: number | null;
  lotacaoId?: number | null;
  jornadaId?: number | null;
  cargoId?: number | null;
  quantidadeResultante?: number | null;
}

export interface RequestAditamentoContratualDTO {
  id?: number | null;
  contratoId?: number | null;
  eventoId?: number | null;
  identificacao?: string | null;
  versaoInstrumento?: number | null;
  dataDocumento?: string | null;
  dataAssinatura?: string | null;
  dataEfetiva?: string | null;
  justificativa?: string | null;
  referenciaSei?: string | null;
  registradoEm?: string | null;
  usuarioResponsavelId?: number | null;
  usuarioResponsavelUsername?: string | null;
  situacao?: DTO.SituacaoFatoNegocial | null;
  novaDescricao?: string | null;
  novoTipoContratacaoId?: number | null;
  novoTipoContratacaoDescricao?: string | null;
  novoFornecedorId?: number | null;
  novoCnpjEmpresa?: string | null;
  novaRazaoSocialEmpresa?: string | null;
  novoNomeFantasiaEmpresa?: string | null;
  novaDataInicio?: string | null;
  novaDataFim?: string | null;
  novoEfetivoTotal?: number | null;
  reducaoExcepcional?: boolean | null;
  fundamentoExcecao?: string | null;
  impactoConfirmado?: boolean | null;
  alteracoesComposicao?: Array<RequestImpactoComposicaoContratoDTO> | null;
  novaMatriculaVinculoObrigatoria?: boolean | null;
  novoDiaSemanaReduzido?: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY" | null;
  novasHorasDiaReduzido?: number | null;
  restaurarPoliticaMatriculaDoTipo?: boolean | null;
  removerConfiguracaoDiaReduzido?: boolean | null;
}

export interface RequestAditamentoContratualRequisicao {
  identificacao?: string | null;
  dataDocumento?: string | null;
  dataAssinatura?: string | null;
  dataEfetiva?: string | null;
  justificativa?: string | null;
  referenciaSei?: string | null;
  novaDescricao?: string | null;
  novoTipoContratacaoId?: number | null;
  novoFornecedorId?: number | null;
  novoCnpjEmpresa?: string | null;
  novaRazaoSocialEmpresa?: string | null;
  novoNomeFantasiaEmpresa?: string | null;
  novaDataInicio?: string | null;
  novaDataFim?: string | null;
  novoEfetivoTotal?: number | null;
  reducaoExcepcional?: boolean | null;
  fundamentoExcecao?: string | null;
  confirmarImpacto?: boolean | null;
  alteracoesComposicao?: Array<RequestAditamentoComposicaoRequisicao> | null;
  eventoPrecedenteId?: number | null;
  relacaoPrecedencia?: DTO.TipoRelacaoPrecedenciaNegocial | null;
  ordemDocumental?: number | null;
  tokenPrevia?: string | null;
  novaMatriculaVinculoObrigatoria?: boolean | null;
  novoDiaSemanaReduzido?: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY" | null;
  novasHorasDiaReduzido?: number | null;
  restaurarPoliticaMatriculaDoTipo?: boolean | null;
  removerConfiguracaoDiaReduzido?: boolean | null;
}

export interface RequestAgenciaReferenciaDTO {
  id?: number | null;
  numero?: string | null;
  banco?: RequestBancoReferenciaDTO | null;
  version?: number;
}

export interface RequestAlocacaoVinculoDTO {
  id?: number | null;
  vinculoId?: number | null;
  composicaoBaseId?: number | null;
  lotacaoId?: number | null;
  lotacaoNome?: string | null;
  jornadaId?: number | null;
  jornadaDescricao?: string | null;
  cargoId?: number | null;
  cargoNome?: string | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  estadoNegocial?: DTO.EstadoNegocialAlocacao | null;
  criadoEm?: string | null;
  criadoPorId?: number | null;
  criadoPorUsername?: string | null;
  version?: number;
}

export interface RequestAlteracaoAlocacaoVinculoRequisicao {
  dataEfetiva?: string | null;
  composicaoId?: number | null;
  lotacaoId?: number | null;
  jornadaId?: number | null;
  cargoId?: number | null;
  justificativa?: string | null;
  evidencia?: string | null;
  autorizacao?: string | null;
  confirmarImpacto?: boolean | null;
  tokenPrevia?: string | null;
  confirmarPrecedenciaUltimoDia?: boolean | null;
  confirmarInclusaoExcedente?: boolean | null;
}

export interface RequestAlteracaoMatriculaVinculoRequisicao {
  dataEfetiva?: string | null;
  novaMatricula?: string | null;
  justificativa?: string | null;
  evidencia?: string | null;
  autorizacao?: string | null;
}

export interface RequestAlteracaoSenhaInicialAutenticacaoDTO {
  newPassword?: string | null;
}

export interface RequestAlteracaoSenhaPerfilDTO {
  currentPassword: string;
  newPassword: string;
  token?: string | null;
}

export interface RequestAtivacaoUsuarioAplicacaoDTO {
  message?: string | null;
  username?: string | null;
  temporaryPassword?: string | null;
  forceChangeOnNextLogin?: boolean;
}

export interface RequestAtualizacaoPessoaOpcionalRequisicao {
  rg?: string | null;
  limparRg?: boolean | null;
  ctpsSerie?: string | null;
  limparCtpsSerie?: boolean | null;
  nitPisPasep?: string | null;
  limparNitPisPasep?: boolean | null;
  dataNascimento?: string | null;
  limparDataNascimento?: boolean | null;
  email?: string | null;
  limparEmail?: boolean | null;
  endereco?: RequestEnderecoDTO | null;
  limparEndereco?: boolean | null;
}

export interface RequestAtualizacaoUsuarioAplicacaoDTO {
  email?: string | null;
  active?: boolean | null;
  matricula?: string | null;
  perfil?: DTO.PerfilAcesso | null;
  password?: string | null;
  lotacaoId?: number | null;
  jornadaId?: number | null;
  vinculosGestao?: Array<RequestAtualizacaoUsuarioAplicacaoDTOVinculoGestaoDTO> | null;
  superintendenciaId?: number | null;
  gestaoLocalId?: number | null;
}

export interface RequestAtualizacaoUsuarioAplicacaoDTOVinculoGestaoDTO {
  lotacaoId?: number | null;
  jornadaId?: number | null;
}

export interface RequestAutenticacaoGestaoLocalDTO {
  id?: number | null;
  nome?: string | null;
  codigo?: string | null;
  superintendenciaId?: number | null;
  uf?: string | null;
}

export interface RequestAutenticacaoSuperintendenciaDTO {
  id?: number | null;
  nome?: string | null;
  uf?: string | null;
}

export interface RequestAvisoRevisaoRelatorioDTO {
  sujeitoARevisao?: boolean;
  quantidadePendencias?: number;
  mensagem?: string | null;
}

export interface RequestBaixaAnexoDTO {
  resource?: Blob | null;
  contentType?: string | null;
  disposition?: string | null;
  filename?: string | null;
}

export interface RequestBaixaDocumentoComprobatorioDTO {
  caminho?: string | null;
  nomeOriginal?: string | null;
  tipoMime?: string | null;
  tamanhoBytes?: number;
}

export interface RequestBancoReferenciaDTO {
  id?: number | null;
  nome?: string | null;
  codigo?: string | null;
  version?: number;
}

export interface RequestCacheDiagnosticoDTO {
  cacheManager?: string | null;
  redisConfigurado?: boolean;
  caches?: Array<string> | null;
  conectado?: boolean;
  ping?: string | null;
  erro?: string | null;
}

export interface RequestCadastroAutenticacaoDTO {
  username: string;
  email: string;
  matricula: string;
  superintendenciaId: number;
  gestaoLocalId: number;
}

export interface RequestCadastroDocumentoComprobatorioRequisicao {
  tipoFato?: DTO.TipoFatoDocumental | null;
  fatoId?: number | null;
  fatoChave?: string | null;
  titulo?: string | null;
  descricao?: string | null;
  referenciaSei?: string | null;
  classificacaoAcesso?: DTO.ClassificacaoAcessoDocumento | null;
  dataEfetiva?: string | null;
  chaveIdempotencia?: string | null;
}

export interface RequestCadastroPessoaRequisicao {
  nome?: string | null;
  cpf?: string | null;
  rg?: string | null;
  ctpsSerie?: string | null;
  nitPisPasep?: string | null;
  dataNascimento?: string | null;
  email?: string | null;
  endereco?: RequestEnderecoDTO | null;
}

export interface RequestCadastroVinculoContratualRequisicao {
  pessoaId?: number | null;
  pessoa?: RequestCadastroPessoaRequisicao | null;
  contratoId?: number | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  matricula?: string | null;
  composicaoId?: number | null;
  confirmarInclusaoExcedente?: boolean | null;
  justificativaExcedente?: string | null;
  evidenciaExcedente?: string | null;
  autorizacaoExcedente?: string | null;
  lotacaoId?: number | null;
  jornadaId?: number | null;
  cargoId?: number | null;
}

export interface RequestCalendarioEventoDTO {
  id?: number | null;
  data?: string | null;
  tipo?: string | null;
  descricao?: string | null;
  abrangencia?: string | null;
  uf?: string | null;
  municipio?: string | null;
  superintendencia?: RequestSuperintendenciaDTO | null;
  gestaoLocal?: RequestGestaoLocalDTO | null;
  lotacaoId?: number | null;
  ativo?: boolean | null;
  version?: number;
  cancelado?: boolean;
}

export interface RequestCalendarioEventoRequisicao {
  data: string;
  tipo: string;
  descricao?: string | null;
  abrangencia?: string | null;
  uf?: string | null;
  municipio?: string | null;
  lotacaoId?: number | null;
  superintendenciaId?: number | null;
  gestaoLocalId?: number | null;
  ativo?: boolean | null;
  justificativa?: string | null;
}

export interface RequestCancelamentoTransferenciaGestaoRequisicao {
  dataEfetiva?: string | null;
  dataAto?: string | null;
  motivo?: string | null;
  referenciaSei?: string | null;
  autorizacaoAdministrativa?: string | null;
  conferenciaDocumentalConfirmada?: boolean | null;
  versaoConferenciaDocumental?: string | null;
}

export interface RequestCargoDTO {
  id?: number | null;
  nome: string;
  cbo?: string | null;
  descricao?: string | null;
  ativo?: boolean | null;
  gestaoLocalId?: number | null;
  gestaoLocalNome?: string | null;
  superintendenciaId?: number | null;
  superintendenciaNome?: string | null;
  uf?: string | null;
  version?: number | null;
}

export interface RequestComparacaoEmissaoRelatorioDTO {
  emissaoHistorica?: RequestMetadadosEmissaoRelatorioDTO | null;
  revisaoVigente?: RequestAvisoRevisaoRelatorioDTO | null;
  hashHistorico?: string | null;
  hashVigente?: string | null;
  alterado?: boolean;
  conteudoHistorico?: import('./shared').JsonValue | null;
  conteudoVigente?: import('./shared').JsonValue | null;
}

export interface RequestComposicaoContratualDTO {
  id?: number | null;
  contratoId?: number | null;
  lotacaoId?: number | null;
  lotacaoNome?: string | null;
  jornadaId?: number | null;
  jornadaDescricao?: string | null;
  cargoId?: number | null;
  cargoNome?: string | null;
  efetivoPrevisto?: number | null;
  dataInicioVigencia?: string | null;
  dataFimVigencia?: string | null;
  permiteNovaAlocacao?: boolean;
  version?: number | null;
}

export interface RequestComposicaoContratualLeituraDTO extends RequestComposicaoContratualDTO {
  lotacaoMunicipio?: string | null;
  jornadaSemanal?: string | null;
  dataReferencia?: string | null;
  conhecidoAte?: string | null;
  provaCampos?: Record<string, RequestProvaCampoHistoricoDTO> | null;
}

export interface RequestConcessaoAcessoDocumentoRequisicao {
  usuarioId?: number | null;
  finalidade?: string | null;
  justificativa?: string | null;
  autorizacaoAdministrativa?: string | null;
  validoAte?: string | null;
}

export interface RequestConcessaoPermissaoExcepcionalRequisicao {
  tipo: DTO.TipoPermissaoExcepcional;
  usuarioBeneficiarioId: number;
  contratoId?: number | null;
  gestaoLocalId?: number | null;
  finalidade: string;
  justificativa: string;
  autorizacaoAdministrativa: string;
  validoAte: string;
  ocorrenciaId?: number | null;
}

export interface RequestConfiguracaoPublicaSistemaDTO {
  EM_MANUTENCAO?: boolean;
  PREVISAO_RETORNO?: string | null;
  CLIENT_IP?: string | null;
  HORAS_DIA_TRABALHO?: number;
  HORA_INICIO_NOTURNO?: string | null;
  MINUTOS_HORA_NOTURNA?: number;
}

export interface RequestConfiguracaoSegundoFatorDTO {
  secret?: string | null;
  otpauthUri?: string | null;
  qrCodeDataUrl?: string | null;
  periodSeconds?: number;
  message?: string | null;
}

export interface RequestConfiguracaoSistemaDTO {
  chave: string;
  valor: string;
  descricao?: string | null;
}

export interface RequestConfirmacaoRecuperacaoSenhaDTO {
  token: string;
}

export interface RequestContextoComplementoFrequenciaDTO {
  dataReferencia?: string | null;
  contratoId?: number | null;
  vinculoId?: number | null;
  alocacaoId?: number | null;
  gestaoLocalId?: number | null;
  superintendenciaId?: number | null;
  lotacaoId?: number | null;
  jornadaId?: number | null;
}

export interface RequestContextoMotivoHistoricoDTO {
  descricao?: string | null;
  categoria?: DTO.CategoriaTerminoVinculo | null;
  comprovado?: boolean;
  tipoFonte?: string | null;
  fonteId?: number | null;
  fonteRegistradaEm?: string | null;
  inicioComprovado?: string | null;
  fimComprovado?: string | null;
}

export interface RequestContratoContagemDTO {
  contratoId?: number | null;
  quantidadeColaboradores?: number | null;
}

export interface RequestContratoDTO {
  id?: number | null;
  numeroContrato?: string | null;
  descricao?: string | null;
  tipo?: string | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  ativo?: boolean | null;
  diaSemanaReduzido?: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY" | null;
  horasDiaReduzido?: number | null;
  gestaoLocalId?: number | null;
  gestaoLocalNome?: string | null;
  superintendenciaId?: number | null;
  superintendenciaNome?: string | null;
  uf?: string | null;
  efetivoPrevisto?: number | null;
  efetivoAtual?: number | null;
  saldoEfetivo?: number | null;
  percentualOcupacao?: number | null;
  anoContrato?: number | null;
  tipoContratacaoId?: number | null;
  tipoContratacaoDescricao?: string | null;
  fornecedorId?: number | null;
  cnpjEmpresa?: string | null;
  razaoSocialEmpresa?: string | null;
  nomeFantasiaEmpresa?: string | null;
  estadoNegocial?: DTO.EstadoNegocialContrato | null;
  situacaoTemporal?: DTO.SituacaoTemporalContrato | null;
  habilitacaoAdministrativa?: DTO.HabilitacaoAdministrativaContrato | null;
  efetivoOriginalPublicado?: number | null;
  matriculaVinculoObrigatoria?: boolean | null;
  publicadoEm?: string | null;
  publicadoPorId?: number | null;
  publicadoPorUsername?: string | null;
  referenciaSeiPublicacao?: string | null;
  version?: number;
}

export interface RequestContratoFornecedorDTO {
  contratoId?: number | null;
  fornecedor?: RequestFornecedorDTO | null;
}

export interface RequestContratoOpcaoDTO {
  id?: number | null;
  numeroContrato?: string | null;
  descricao?: string | null;
  ativo?: boolean | null;
  quantidadeColaboradores?: number | null;
}

export interface RequestCorrecaoCnpjContratoDTO {
  id?: number | null;
  contratoId?: number | null;
  eventoId?: number | null;
  cnpjAnterior?: string | null;
  cnpjCorrigido?: string | null;
  dataEfetiva?: string | null;
  dataAto?: string | null;
  motivo?: string | null;
  referenciaSei?: string | null;
  chaveIdempotencia?: string | null;
  responsavelId?: number | null;
  responsavelUsername?: string | null;
  registradoEm?: string | null;
}

export interface RequestCorrecaoCnpjContratoRequisicao {
  cnpjCorrigido: string;
  dataEfetiva: string;
  dataAto: string;
  motivo: string;
  referenciaSei: string;
  chaveIdempotencia: string;
  eventoPrecedenteId?: number | null;
  relacaoPrecedencia?: DTO.TipoRelacaoPrecedenciaNegocial | null;
  ordemDocumental?: number | null;
}

export interface RequestCorrecaoIdentidadePessoaRequisicao {
  tipo?: DTO.TipoCorrecaoPessoa | null;
  novoValor?: string | null;
  motivo?: string | null;
  evidencia?: string | null;
  autorizacao?: string | null;
  impactosEncontrados?: string | null;
}

export interface RequestCorrecaoIdentificadorContratoDTO {
  id?: number | null;
  contratoId?: number | null;
  eventoId?: number | null;
  permissaoId?: number | null;
  numeroAnterior?: string | null;
  anoAnterior?: number | null;
  numeroNovo?: string | null;
  anoNovo?: number | null;
  dataEfetiva?: string | null;
  dataAto?: string | null;
  motivo?: string | null;
  referenciaSei?: string | null;
  autorizacaoAdministrativa?: string | null;
  chaveIdempotencia?: string | null;
  registradoEm?: string | null;
  responsavelId?: number | null;
  responsavel?: string | null;
}

export interface RequestCorrecaoIdentificadorContratoRequisicao {
  novoNumero: string;
  novoAno: number;
  dataEfetiva: string;
  dataAto: string;
  motivo: string;
  referenciaSei: string;
  autorizacaoAdministrativa: string;
  chaveIdempotencia: string;
  eventoPrecedenteId?: number | null;
  relacaoPrecedencia?: DTO.TipoRelacaoPrecedenciaNegocial | null;
  ordemDocumental?: number | null;
}

export interface RequestCorrecaoPessoaDTO {
  id?: number | null;
  pessoaId?: number | null;
  tipo?: DTO.TipoCorrecaoPessoa | null;
  valorAnterior?: string | null;
  valorNovo?: string | null;
  motivo?: string | null;
  evidencia?: string | null;
  autorizacao?: string | null;
  impactosEncontrados?: string | null;
  responsavelId?: number | null;
  responsavelUsername?: string | null;
  registradoEm?: string | null;
}

export interface RequestCredenciaisLoginDTO {
  matricula?: string | null;
  password?: string | null;
  totp?: string | null;
}

export interface RequestCriacaoOcorrenciaDTO {
  empregadoId?: number | null;
  motivo?: string | null;
  dataOcorrencia?: string | null;
  observacao?: string | null;
  tipoRo?: string | null;
  numeroSei?: string | null;
  dias?: number | null;
  classificacoesAnexos?: Array<string> | null;
  chaveIdempotencia?: string | null;
}

export interface RequestCriacaoUsuarioAplicacaoDTO {
  username: string;
  email: string;
  matricula: string;
  password: string;
  perfil: DTO.PerfilAcesso;
  superintendenciaId?: number | null;
  gestaoLocalId?: number | null;
  lotacaoId?: number | null;
  jornadaId?: number | null;
  vinculosGestao?: Array<RequestAtualizacaoUsuarioAplicacaoDTOVinculoGestaoDTO> | null;
  active?: boolean | null;
}

export interface RequestDesativacaoSegundoFatorDTO {
  currentPassword?: string | null;
  token?: string | null;
}

export interface RequestDesligamentoVinculoRequisicao {
  dataFim?: string | null;
  motivoDesligamentoId?: number | null;
  justificativa?: string | null;
  evidencia?: string | null;
  autorizacao?: string | null;
  confirmarImpacto?: boolean | null;
  tokenPrevia?: string | null;
  confirmarPrecedenciaUltimoDia?: boolean | null;
  confirmarInclusaoExcedente?: boolean | null;
}

export interface RequestDisponibilidadeCatalogoDTO {
  tipoCatalogo?: DTO.TipoCatalogoTemporal | null;
  catalogoId?: number | null;
  dataReferencia?: string | null;
  conhecidoAte?: string | null;
  ativo?: boolean | null;
  disponibilidadeComprovada?: boolean;
  eventoFonteId?: number | null;
  dataEfetivaFonte?: string | null;
  registradoEmFonte?: string | null;
  ativoAnterior?: boolean | null;
}

export interface RequestDocumentoComprobatorioDTO {
  id?: number | null;
  contratoId?: number | null;
  tipoFato?: DTO.TipoFatoDocumental | null;
  fatoId?: number | null;
  fatoChave?: string | null;
  grupoLogicoId?: string | null;
  versaoDocumento?: number | null;
  documentoAnteriorId?: number | null;
  estado?: DTO.EstadoDocumentoComprobatorio | null;
  classificacaoAcesso?: DTO.ClassificacaoAcessoDocumento | null;
  tipoFonte?: DTO.TipoFonteComprobatoria | null;
  titulo?: string | null;
  descricao?: string | null;
  referenciaSei?: string | null;
  nomeOriginalArquivo?: string | null;
  tipoMime?: string | null;
  tamanhoBytes?: number | null;
  sha256Arquivo?: string | null;
  dataEfetiva?: string | null;
  registradoEm?: string | null;
  registradoPorId?: number | null;
  registradoPorUsername?: string | null;
  publicadoEm?: string | null;
  publicadoPorId?: number | null;
  publicadoPorUsername?: string | null;
  versaoAtual?: boolean;
  downloadDisponivel?: boolean;
  version?: number;
}

export interface RequestEmissaoRelatorioRequisicao {
  tipo: DTO.TipoRelatorioAdministrativo;
  formato: DTO.FormatoEmissaoRelatorio;
  inicio: string;
  fim: string;
  contratoIds?: Array<number> | null;
  lotacaoId?: number | null;
  jornadaId?: number | null;
  chaveIdempotencia: string;
}

export interface RequestEmpregadoContratoDTO {
  id?: number | null;
  numeroContrato?: string | null;
  descricao?: string | null;
  ativo?: boolean | null;
}

export interface RequestEmpregadoContratoListaDTO {
  empregadoId?: number | null;
  contratoId?: number | null;
  numeroContrato?: string | null;
  descricao?: string | null;
  ativo?: boolean | null;
}

export interface RequestEmpregadoDTO {
  id?: number | null;
  nome?: string | null;
  matricula?: string | null;
  cpf?: string | null;
  rg?: string | null;
  ctpsSerie?: string | null;
  nitPisPasep?: string | null;
  email?: string | null;
  dataNascimento?: string | null;
  dataAdmissao?: string | null;
  statusId?: number | null;
  statusDescricao?: string | null;
  lotacaoId?: number | null;
  lotacaoNome?: string | null;
  cargoId?: number | null;
  cargoNome?: string | null;
  jornadaId?: number | null;
  jornadaDescricao?: string | null;
  endereco?: RequestEnderecoDTO | null;
  contratos?: Array<RequestEmpregadoContratoDTO> | null;
  gestaoLocalId?: number | null;
  gestaoLocalNome?: string | null;
  superintendenciaId?: number | null;
  superintendenciaNome?: string | null;
  uf?: string | null;
}

export interface RequestEmpregadoFrequenciaDTO {
  id?: number | null;
  nome?: string | null;
  matricula?: string | null;
  cpf?: string | null;
  lotacaoNome?: string | null;
  lotacaoId?: number | null;
  turnoDescricao?: string | null;
  turnoId?: number | null;
  cargoNome?: string | null;
}

export interface RequestEmpregadoListaBaseDTO {
  id?: number | null;
  nome?: string | null;
  matricula?: string | null;
  cpf?: string | null;
  statusId?: number | null;
  statusDescricao?: string | null;
  lotacaoId?: number | null;
  lotacaoNome?: string | null;
  cargoId?: number | null;
  cargoNome?: string | null;
  jornadaId?: number | null;
  jornadaDescricao?: string | null;
}

export interface RequestEmpregadoListaDTO {
  id?: number | null;
  nome?: string | null;
  matricula?: string | null;
  cpf?: string | null;
  statusId?: number | null;
  statusDescricao?: string | null;
  lotacaoId?: number | null;
  lotacaoNome?: string | null;
  cargoId?: number | null;
  cargoNome?: string | null;
  jornadaId?: number | null;
  jornadaDescricao?: string | null;
  contratos?: Array<RequestEmpregadoContratoDTO> | null;
}

export interface RequestEmpregadoReferenciaDTO {
  id?: number | null;
}

export interface RequestEnderecoDTO {
  id?: number | null;
  logradouro: string;
  numero?: string | null;
  bairro?: string | null;
  cidade?: string | null;
  uf?: string | null;
  cep?: string | null;
  version?: number;
}

export interface RequestEventoAlocacaoVinculoDTO {
  id?: number | null;
  vinculoId?: number | null;
  alocacaoNovaId?: number | null;
  tipoEvento?: DTO.TipoEventoAlocacaoVinculo | null;
  relacaoPrecedencia?: DTO.TipoRelacaoEventoAlocacao | null;
  eventoPrecedenteId?: number | null;
  dataEfetiva?: string | null;
  registradoEm?: string | null;
  responsavelId?: number | null;
  responsavelUsername?: string | null;
  justificativa?: string | null;
  evidencia?: string | null;
  autorizacao?: string | null;
  alocacaoAnteriorId?: number | null;
  inicioAnterior?: string | null;
  fimAnterior?: string | null;
  estadoAnterior?: DTO.EstadoNegocialAlocacao | null;
  composicaoAnteriorId?: number | null;
  lotacaoAnteriorId?: number | null;
  lotacaoAnteriorNome?: string | null;
  jornadaAnteriorId?: number | null;
  jornadaAnteriorDescricao?: string | null;
  cargoAnteriorId?: number | null;
  cargoAnteriorNome?: string | null;
  inicioNovo?: string | null;
  fimNovo?: string | null;
  estadoNovo?: DTO.EstadoNegocialAlocacao | null;
  composicaoNovaId?: number | null;
  lotacaoNovaId?: number | null;
  lotacaoNovaNome?: string | null;
  jornadaNovaId?: number | null;
  jornadaNovaDescricao?: string | null;
  cargoNovoId?: number | null;
  cargoNovoNome?: string | null;
  transferenciaGestaoId?: number | null;
  gestaoOrigemIdFotografia?: number | null;
  gestaoOrigemNomeFotografia?: string | null;
  gestaoDestinoIdFotografia?: number | null;
  gestaoDestinoNomeFotografia?: string | null;
  hashAnterior?: string | null;
  hashEvento?: string | null;
}

export interface RequestEventoCicloVidaContratoDTO {
  id?: number | null;
  tipoEvento?: DTO.TipoEventoCicloVidaContrato | null;
  fatoLogicoId?: string | null;
  chaveNegocial?: string | null;
  versaoNegocial?: number | null;
  ordemDocumental?: number | null;
  situacaoFato?: DTO.SituacaoFatoNegocial | null;
  dataEfetiva?: string | null;
  dataAto?: string | null;
  registradoEm?: string | null;
  usuarioResponsavelId?: number | null;
  usuarioResponsavelUsername?: string | null;
  motivo?: string | null;
  referenciaSei?: string | null;
  valorAnterior?: string | null;
  valorNovo?: string | null;
  conferenciaDocumental?: DTO.ConferenciaDocumentalAtoDTO | null;
}

export interface RequestEventoDocumentoComprobatorioDTO {
  id?: number | null;
  documentoId?: number | null;
  grupoLogicoId?: string | null;
  tipoEvento?: DTO.TipoEventoDocumento | null;
  dataEfetiva?: string | null;
  registradoEm?: string | null;
  responsavelId?: number | null;
  responsavelUsername?: string | null;
  motivo?: string | null;
  estadoAnterior?: DTO.EstadoDocumentoComprobatorio | null;
  estadoNovo?: DTO.EstadoDocumentoComprobatorio | null;
  referenciaSeiAnterior?: string | null;
  referenciaSeiNova?: string | null;
  sha256Anterior?: string | null;
  sha256Novo?: string | null;
  hashAnterior?: string | null;
  hashEvento?: string | null;
}

export interface RequestEventoFrequenciaDTO {
  id?: number | null;
  frequenciaId?: number | null;
  numeroVersao?: number | null;
  tipoEvento?: DTO.TipoEventoFrequencia | null;
  dataReferencia?: string | null;
  snapshotAnterior?: string | null;
  snapshotNovo?: string | null;
  motivo?: string | null;
  usuarioId?: number | null;
  usuarioUsername?: string | null;
  registradoEm?: string | null;
  hashAnterior?: string | null;
  hashEvento?: string | null;
  conteudoMinimizado?: boolean;
}

export interface RequestEventoVinculoContratualDTO {
  id?: number | null;
  vinculoId?: number | null;
  tipo?: DTO.TipoEventoVinculoContratual | null;
  dataEfetiva?: string | null;
  registradoEm?: string | null;
  responsavelId?: number | null;
  responsavelUsername?: string | null;
  justificativa?: string | null;
  evidencia?: string | null;
  autorizacao?: string | null;
  eventoPrecedenteId?: number | null;
  relacaoPrecedencia?: DTO.TipoRelacaoEventoVinculo | null;
  inicioAnterior?: string | null;
  inicioNovo?: string | null;
  fimAnterior?: string | null;
  fimNovo?: string | null;
  matriculaAnterior?: string | null;
  matriculaNova?: string | null;
  motivoDesligamentoIdFotografia?: number | null;
  motivoDesligamentoDescricaoFotografia?: string | null;
  motivoDesligamentoCategoriaFotografia?: DTO.CategoriaTerminoVinculo | null;
  inclusaoExcedente?: boolean;
  composicaoExcedenteId?: number | null;
  lotacaoExcedenteId?: number | null;
  jornadaExcedenteId?: number | null;
  cargoExcedenteId?: number | null;
  efetivoPrevistoExcedente?: number | null;
  ocupacaoAposInclusaoExcedente?: number | null;
  pessoaNomeFotografia?: string | null;
  pessoaCpfFotografia?: string | null;
  contratoNumeroFotografia?: string | null;
  contratoAnoFotografia?: number | null;
  gestaoLocalIdFotografia?: number | null;
  gestaoLocalNomeFotografia?: string | null;
  hashAnterior?: string | null;
  hashEvento?: string | null;
}

export interface RequestEvidenciasIntegridadeDocumentoDTO {
  documentoId?: number | null;
  grupoLogicoId?: string | null;
  estado?: string | null;
  cadeiaIntegra?: boolean;
  eventos?: Array<RequestEvidenciasIntegridadeDocumentoDTOEvento> | null;
}

export interface RequestEvidenciasIntegridadeDocumentoDTOEvento {
  eventoOriginal?: RequestEventoDocumentoComprobatorioDTO | null;
  hashArmazenado?: string | null;
  hashCalculado?: string | null;
  hashConfere?: boolean;
  encadeamentoConfere?: boolean;
}

export interface RequestEvidenciasIntegridadeFrequenciaDTO {
  frequenciaId?: number | null;
  estado?: string | null;
  cadeiaIntegra?: boolean;
  eventos?: Array<RequestEvidenciasIntegridadeFrequenciaDTOEvento> | null;
}

export interface RequestEvidenciasIntegridadeFrequenciaDTOEvento {
  eventoOriginal?: RequestEventoFrequenciaDTO | null;
  hashArmazenado?: string | null;
  hashCalculado?: string | null;
  hashConfere?: boolean;
  encadeamentoConfere?: boolean;
}

export interface RequestExcedenteComposicaoDTO {
  composicaoId?: number | null;
  contratoId?: number | null;
  lotacaoId?: number | null;
  lotacaoNome?: string | null;
  jornadaId?: number | null;
  jornadaDescricao?: string | null;
  cargoId?: number | null;
  cargoNome?: string | null;
  efetivoPrevisto?: number | null;
  quantidadeVinculadaAtual?: number;
  quantidadeExcedente?: number;
  desdeQuandoExcedente?: string | null;
  excedente?: boolean;
  justificativas?: Array<RequestJustificativaExcedenteDTO> | null;
}

export interface RequestExportacaoRelatorioStatusDTO {
  id?: string | null;
  relatorio?: string | null;
  formato?: string | null;
  nomeArquivo?: string | null;
  status?: string | null;
  etapa?: string | null;
  mensagem?: string | null;
  processados?: number;
  totalEstimado?: number;
  percentual?: number;
  segundosDecorridos?: number;
  segundosRestantesEstimados?: number | null;
  tamanhoBytes?: number;
  concluido?: boolean;
  falhou?: boolean;
  podeBaixar?: boolean;
  iniciadoEm?: string | null;
  atualizadoEm?: string | null;
}

export interface RequestFechamentoFrequenciaDTO {
  id?: number | null;
  status?: string | null;
  dataReferencia?: string | null;
  lotacaoId?: number | null;
  lotacaoNome?: string | null;
  jornadaId?: number | null;
  jornadaDescricao?: string | null;
  contratoIds?: Array<number> | null;
  totalPrevisto?: number;
  totalProcessado?: number;
  totalInserido?: number;
  totalAtualizado?: number;
  totalPreservado?: number;
  totalAlteracoes?: number;
  somenteAlteracoes?: boolean;
  percentual?: number;
  tentativa?: number;
  cancelamentoSolicitado?: boolean;
  mensagem?: string | null;
  criadoEm?: string | null;
  iniciadoEm?: string | null;
  atualizadoEm?: string | null;
  concluidoEm?: string | null;
}

export interface RequestFornecedorDTO {
  id?: number | null;
  razaoSocial?: string | null;
  cnpj?: string | null;
  email?: string | null;
  ativo?: boolean | null;
}

export interface RequestFornecedorEntradaDTO {
  razaoSocial?: string | null;
  cnpj?: string | null;
  email?: string | null;
  ativo?: boolean | null;
}

export interface RequestFragmentoProvaHistoricaComposicaoDTO {
  composicaoId?: number | null;
  mapeamentoId?: number | null;
  aditamentoComposicaoId?: number | null;
  lotacaoNome?: string | null;
  jornadaDescricao?: string | null;
  cargoNome?: string | null;
  lotacaoMunicipio?: string | null;
  municipioComprovado?: boolean;
  jornadaSemanal?: string | null;
  jornadaComprovada?: boolean;
  provaCampos?: Record<string, RequestProvaCampoHistoricoDTO> | null;
}

export interface RequestFrequenciaAdicionalDTO {
  id?: number | null;
  empregadoId?: number | null;
  empregadoNome?: string | null;
  dataReferencia?: string | null;
  tipo?: string | null;
  minutos?: number | null;
  quantidade?: number | null;
  status?: string | null;
  justificativa?: string | null;
  frequenciaId?: number | null;
  origem?: string | null;
}

export interface RequestFrequenciaExcecaoDTO {
  id?: number | null;
  empregadoId?: number | null;
  empregadoNome?: string | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  tipo?: string | null;
  status?: string | null;
  observacao?: string | null;
  justificativa?: string | null;
}

export interface RequestGestaoLocalDTO {
  id?: number | null;
  nome?: string | null;
  codigo?: string | null;
  ativo?: boolean | null;
  superintendenciaId?: number | null;
  superintendenciaNome?: string | null;
  uf?: string | null;
}

export interface RequestHistoricoEmpregadoDTO {
  id?: number | null;
  empregado?: RequestEmpregadoReferenciaDTO | null;
  dataEvento?: string | null;
  descricao?: string | null;
  usuario?: RequestUsuarioReferenciaDTO | null;
  hashIntegridade?: string | null;
  version?: number;
  dataEfetiva?: string | null;
  versaoAssinatura?: number | null;
  ocorrenciaId?: number | null;
}

export interface RequestHistoricoVinculoContratoDTO {
  id?: number | null;
  empregadoId?: number | null;
  empregadoNome?: string | null;
  contratoId?: number | null;
  numeroContrato?: string | null;
  nomeContrato?: string | null;
  descricaoContrato?: string | null;
  tipoContrato?: string | null;
  contratoDataInicio?: string | null;
  contratoDataFim?: string | null;
  contratoAtivo?: boolean | null;
  efetivoPrevisto?: number | null;
  efetivoAtual?: number | null;
  saldoEfetivo?: number | null;
  percentualOcupacao?: number | null;
  diaSemanaReduzido?: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY" | null;
  horasDiaReduzido?: number | null;
  gestaoLocalId?: number | null;
  gestaoLocalNome?: string | null;
  superintendenciaId?: number | null;
  superintendenciaNome?: string | null;
  uf?: string | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  observacao?: string | null;
  ativo?: boolean;
  identidadeHistorica?: RequestIdentidadeHistoricaResolvidaDTO | null;
  vinculoCanonicoId?: number | null;
  dataReferenciaIdentidade?: string | null;
  conhecidoAteIdentidade?: string | null;
}

export interface RequestIdentidadeHistoricaResolvidaDTO {
  nome?: string | null;
  comprovado?: boolean;
  tipoFonte?: string | null;
  fonteId?: number | null;
  fonteRegistradaEm?: string | null;
  inicioComprovado?: string | null;
  fimComprovado?: string | null;
}

export interface RequestImpactoComposicaoContratoDTO {
  composicaoId?: number | null;
  lotacaoId?: number | null;
  lotacaoNome?: string | null;
  jornadaId?: number | null;
  jornadaDescricao?: string | null;
  cargoId?: number | null;
  cargoNome?: string | null;
  quantidadeAnterior?: number | null;
  variacao?: number | null;
  quantidadeResultante?: number | null;
  colaboradoresAtuais?: number | null;
  excedenteResultante?: number | null;
  exigeTratamento?: boolean;
}

export interface RequestJornadaDTO {
  id?: number | null;
  descricao?: string | null;
  dias?: Array<RequestJornadaDiaSemanaDTO> | null;
  regrasAdicionais?: Array<RequestRegraAdicionalDTO> | null;
  gestaoLocalId?: number | null;
  gestaoLocalNome?: string | null;
  superintendenciaId?: number | null;
  superintendenciaNome?: string | null;
  uf?: string | null;
  ativo?: boolean | null;
  version?: number | null;
}

export interface RequestJornadaDiaSemanaDTO {
  id?: number | null;
  diaSemana?: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY" | null;
  diaTrabalho?: boolean | null;
  entrada1?: string | null;
  saida1?: string | null;
  entrada2?: string | null;
  saida2?: string | null;
}

export interface RequestJornadaResumoDTO {
  id?: number | null;
  descricao?: string | null;
}

export interface RequestJustificativaExcedenteDTO {
  eventoId?: number | null;
  vinculoId?: number | null;
  dataEfetiva?: string | null;
  registradoEm?: string | null;
  responsavelUsername?: string | null;
  justificativa?: string | null;
  evidencia?: string | null;
  autorizacao?: string | null;
  efetivoPrevisto?: number;
  ocupacaoAposInclusao?: number;
  origemEvento?: string | null;
}

export interface RequestLotacaoDTO {
  id?: number | null;
  nome?: string | null;
  turnos?: Array<RequestJornadaResumoDTO> | null;
  gestaoLocalId?: number | null;
  gestaoLocalNome?: string | null;
  superintendenciaId?: number | null;
  superintendenciaNome?: string | null;
  uf?: string | null;
  municipio?: string | null;
  ativo?: boolean | null;
  version?: number | null;
}

export interface RequestLotacaoOpcaoDTO {
  id?: number | null;
  nome?: string | null;
  turnos?: Array<RequestJornadaResumoDTO> | null;
}

export interface RequestMapeamentoTransferenciaGestaoDTO {
  id?: number | null;
  composicaoOrigemId?: number | null;
  lotacaoOrigemId?: number | null;
  lotacaoOrigemNome?: string | null;
  jornadaOrigemId?: number | null;
  jornadaOrigemDescricao?: string | null;
  cargoOrigemId?: number | null;
  cargoOrigemNome?: string | null;
  lotacaoDestinoId?: number | null;
  lotacaoDestinoNome?: string | null;
  jornadaDestinoId?: number | null;
  jornadaDestinoDescricao?: string | null;
  cargoDestinoId?: number | null;
  cargoDestinoNome?: string | null;
}

export interface RequestMapeamentoTransferenciaGestaoRequisicao {
  composicaoOrigemId?: number | null;
  lotacaoOrigemId?: number | null;
  jornadaOrigemId?: number | null;
  cargoOrigemId?: number | null;
  lotacaoDestinoId?: number | null;
  jornadaDestinoId?: number | null;
  cargoDestinoId?: number | null;
}

export interface RequestMensagemApiDTO {
  message?: string | null;
  error?: string | null;
}

export interface RequestMetadadosEmissaoRelatorioDTO {
  id?: number | null;
  tipo?: DTO.TipoRelatorioAdministrativo | null;
  formato?: DTO.FormatoEmissaoRelatorio | null;
  versao?: number;
  natureza?: DTO.NaturezaEmissaoRelatorio | null;
  estado?: DTO.EstadoEmissaoRelatorio | null;
  situacao?: string | null;
  inicio?: string | null;
  fim?: string | null;
  geradoEm?: string | null;
  usuarioId?: number | null;
  usuario?: string | null;
  perfil?: string | null;
  sujeitoARevisao?: boolean;
  quantidadePendencias?: number;
  aviso?: string | null;
  hashConteudo?: string | null;
  tamanhoConteudoBytes?: number;
  emissaoAnteriorId?: number | null;
  superadaEm?: string | null;
  causaSuperacao?: DTO.CausaSuperacaoRelatorio | null;
  motivoSuperacao?: string | null;
  fonteConhecidaAte?: string | null;
}

export interface RequestMeuPerfilDTO {
  id?: number | null;
  username?: string | null;
  email?: string | null;
  perfil?: DTO.PerfilAcesso | null;
  autoridade?: string | null;
  matricula?: string | null;
  turno?: string | null;
  jornadaId?: number | null;
  jornada?: RequestResumoUsuarioAplicacaoDTOReferencia | null;
  lotacao?: RequestResumoUsuarioAplicacaoDTOReferencia | null;
  lotacaoNome?: string | null;
  lotacaoId?: number | null;
  vinculosGestao?: Array<RequestResumoUsuarioAplicacaoDTOVinculoGestaoReferencia> | null;
  superintendencia?: RequestResumoUsuarioAplicacaoDTOOrganizacaoReferencia | null;
  gestaoLocal?: RequestResumoUsuarioAplicacaoDTOOrganizacaoReferencia | null;
  forceChangePassword?: boolean;
  isRetroactiveUnlocked?: boolean;
  retroactiveExpireAt?: string | null;
  fotoUrl?: string | null;
  createdAt?: string | null;
  twoFactorEnabled?: boolean;
}

export interface RequestMotivoDesligamentoDTO {
  id?: number | null;
  descricao: string;
  categoria: DTO.CategoriaTerminoVinculo;
  ativo?: boolean | null;
  gestaoLocalId?: number | null;
  gestaoLocalNome?: string | null;
  superintendenciaId?: number | null;
  superintendenciaNome?: string | null;
  uf?: string | null;
  criadoEm?: string | null;
  criadoPorId?: number | null;
  criadoPorUsername?: string | null;
  atualizadoEm?: string | null;
  version?: number;
}

export interface RequestOcorrenciaHistoricoDTO {
  ocorrencia?: RequestOcorrenciaResumoDTO | null;
  cancelada?: boolean;
  canceladoEm?: string | null;
  canceladoPorId?: number | null;
  canceladoPorUsername?: string | null;
  motivoCancelamento?: string | null;
}

export interface RequestOcorrenciaResumoDTO {
  id?: number | null;
  empregado?: RequestOcorrenciaResumoDTOEmpregadoResumo | null;
  dataOcorrencia?: string | null;
  motivo?: string | null;
  tipoRo?: string | null;
  numeroSei?: string | null;
  observacao?: string | null;
  quantidadeDias?: number | null;
  diasSuspensao?: number | null;
  diasAfastamento?: number | null;
  anexos?: Array<RequestOcorrenciaResumoDTOAnexoResumo> | null;
}

export interface RequestOcorrenciaResumoDTOEmpregadoResumo {
  id?: number | null;
  nome?: string | null;
}

export interface RequestOcorrenciaResumoDTOAnexoResumo {
  id?: number | null;
  nomeOriginal?: string | null;
  tipoArquivo?: string | null;
  classificacaoAcesso?: string | null;
  sha256?: string | null;
}

export interface RequestOpcaoAlocacaoTemporalDTO {
  composicaoBaseId?: number | null;
  lotacaoId?: number | null;
  lotacaoNome?: string | null;
  jornadaId?: number | null;
  jornadaDescricao?: string | null;
  cargoId?: number | null;
  cargoNome?: string | null;
  efetivoPrevisto?: number;
  ocupacaoAtual?: number;
  ocupacaoAposInclusao?: number;
  excedenteAposInclusao?: number;
  exigeJustificativaExcedente?: boolean;
  disponivelParaNovaAlocacao?: boolean;
}

export interface RequestOrigemProvaCampoDTO {
  tipo?: DTO.Tipo | null;
  provaId?: number | null;
  eventoId?: number | null;
  versaoId?: number | null;
  composicaoId?: number | null;
  aditamentoComposicaoId?: number | null;
  mapeamentoId?: number | null;
  dataEfetiva?: string | null;
  registradoEm?: string | null;
}

export interface RequestPaginaCacheDTO<T> {
  content?: Array<T> | null;
  totalElements?: number;
}

export interface RequestPaginaPendenciasDTO {
  registros?: Array<RequestPendenciaDTO> | null;
  totalElementos?: number;
  totalPaginas?: number;
  pagina?: number;
  tamanho?: number;
  primeira?: boolean;
  ultima?: boolean;
}

export interface RequestPainelCalculoDTO {
  id?: number | null;
  status?: string | null;
  progresso?: number;
  concluido?: boolean;
  desatualizado?: boolean;
  mensagem?: string | null;
  geradoEm?: string | null;
  dados?: RequestPainelGestaoDTO | null;
}

export interface RequestPainelEfetivoDTO {
  inicio?: string | null;
  fim?: string | null;
  periodo?: string | null;
  contratoId?: number | null;
  lotacaoId?: number | null;
  jornadaId?: number | null;
  cargoId?: number | null;
  resumoAtual?: RequestPainelEfetivoDTOResumo | null;
  resumoAnoAnterior?: RequestPainelEfetivoDTOResumo | null;
  serieAtual?: Array<RequestPainelEfetivoDTOPontoDiario> | null;
  serieAnoAnterior?: Array<RequestPainelEfetivoDTOPontoDiario> | null;
  detalhamentoContratos?: Array<RequestPainelEfetivoDTODetalheContrato> | null;
  detalhamentoLotacoes?: Array<RequestPainelEfetivoDTODetalheDimensao> | null;
  detalhamentoTurnos?: Array<RequestPainelEfetivoDTODetalheDimensao> | null;
  detalhamentoCargos?: Array<RequestPainelEfetivoDTODetalheDimensao> | null;
  quadroDimensionado?: Array<RequestPainelEfetivoDTOLinhaDimensionada> | null;
}

export interface RequestPainelEfetivoDTOPontoDiario {
  data?: string | null;
  efetivoPrevisto?: number;
  efetivoAlocado?: number;
  registrosLancados?: number;
  presentes?: number;
  ausentes?: number;
  outrosRegistros?: number;
  pendentesLancamento?: number;
  deficitContratual?: number;
  excedenteContratual?: number;
  percentualCobertura?: number;
  percentualAtendimento?: number;
}

export interface RequestPainelEfetivoDTOResumo {
  efetivoPrevisto?: number;
  efetivoAlocado?: number;
  registrosLancados?: number;
  presentes?: number;
  ausentes?: number;
  outrosRegistros?: number;
  pendentesLancamento?: number;
  deficitContratual?: number;
  excedenteContratual?: number;
  percentualCobertura?: number;
  percentualAtendimento?: number;
}

export interface RequestPainelEfetivoDTODetalheContrato {
  id?: number | null;
  nome?: string | null;
  efetivoPrevisto?: number;
  efetivoAlocado?: number;
  registrosLancados?: number;
  presentes?: number;
  ausentes?: number;
  pendentesLancamento?: number;
  deficitContratual?: number;
  excedenteContratual?: number;
  percentualCobertura?: number;
  percentualAtendimento?: number;
}

export interface RequestPainelEfetivoDTODetalheDimensao {
  id?: number | null;
  nome?: string | null;
  efetivoAlocado?: number;
  registrosLancados?: number;
  presentes?: number;
  ausentes?: number;
  pendentesLancamento?: number;
  percentualCobertura?: number;
  percentualAtendimento?: number;
  efetivoPrevisto?: number;
  deficitContratual?: number;
  excedenteContratual?: number;
}

export interface RequestPainelEfetivoDTOLinhaDimensionada {
  contratoId?: number | null;
  contrato?: string | null;
  lotacaoId?: number | null;
  lotacao?: string | null;
  jornadaId?: number | null;
  turno?: string | null;
  cargoId?: number | null;
  cargo?: string | null;
  efetivoAlocado?: number;
  registrosLancados?: number;
  presentes?: number;
  ausentes?: number;
  pendentesLancamento?: number;
  percentualCobertura?: number;
  percentualAtendimento?: number;
  efetivoPrevisto?: number;
  deficitContratual?: number;
  excedenteContratual?: number;
}

export interface RequestPainelEfetivoDTOOpcao {
  id?: number | null;
  nome?: string | null;
}

export interface RequestPainelEfetivoDTOFiltros {
  contratos?: Array<RequestPainelEfetivoDTOOpcao> | null;
  lotacoes?: Array<RequestPainelEfetivoDTOOpcao> | null;
  turnos?: Array<RequestPainelEfetivoDTOOpcao> | null;
  cargos?: Array<RequestPainelEfetivoDTOOpcao> | null;
  permiteDetalhamentoOperacional?: boolean;
}

export interface RequestPainelGestaoDTO {
  referencia?: string | null;
  geradoEm?: string | null;
  escopo?: RequestPainelGestaoDTOEscopo | null;
  efetivo?: RequestPainelGestaoDTOResumoEfetivo | null;
  quadro?: RequestPainelGestaoDTOQuadroPessoal | null;
  analiseCadastro?: RequestPainelGestaoDTOAnaliseCadastro | null;
  admissoesMensais?: Array<RequestPainelGestaoDTOAdmissaoMensal> | null;
  contratos?: RequestPainelGestaoDTOSaudeContratual | null;
  frequencia?: RequestPainelGestaoDTOCoberturaFrequencia | null;
  periodo?: RequestPainelGestaoDTOAnalisePeriodo | null;
  comparativoPeriodo?: RequestPainelGestaoDTOComparativoPeriodo | null;
  pressaoOperacional?: RequestPainelGestaoDTOPressaoOperacional | null;
  integridade?: RequestPainelGestaoDTOIntegridadeDados | null;
  horizonteContratual?: RequestPainelGestaoDTOHorizonteContratual | null;
  distribuicaoStatus?: Array<RequestPainelGestaoDTODistribuicaoStatus> | null;
  situacaoQuadro?: Array<RequestPainelGestaoDTOSituacaoQuadro> | null;
  padraoSemanal?: Array<RequestPainelGestaoDTOPadraoSemanal> | null;
  unidades?: Array<RequestPainelGestaoDTOUnidadeGestao> | null;
  evolucao?: Array<RequestPainelGestaoDTOSerieTemporal> | null;
  alertas?: Array<RequestPainelGestaoDTOAlertaGestao> | null;
  notasMetodologicas?: Array<string> | null;
}

export interface RequestPainelGestaoDTOEscopo {
  nivel?: string | null;
  nome?: string | null;
  superintendenciaId?: number | null;
  gestaoLocalId?: number | null;
  permiteDetalhamentoOperacional?: boolean;
}

export interface RequestPainelGestaoDTOResumoEfetivo {
  efetivoPrevisto?: number;
  efetivoVinculado?: number;
  empregadosAtivosCadastrados?: number;
  saldoContratual?: number;
  percentualOcupacao?: number;
  ativosSemContrato?: number;
  contratosVigentes?: number;
}

export interface RequestPainelGestaoDTOQuadroPessoal {
  empregadosCadastrados?: number;
  ativos?: number;
  inativosOuAfastados?: number;
  lotacoes?: number;
  cargos?: number;
  jornadas?: number;
  percentualAtivos?: number;
}

export interface RequestPainelGestaoDTOAnaliseCadastro {
  admitidosNoPeriodo?: number;
  admitidosNoMesReferencia?: number;
  admitidosUltimos12Meses?: number;
  comDataAdmissao?: number;
  semDataAdmissao?: number;
  percentualComDataAdmissao?: number;
}

export interface RequestPainelGestaoDTOAdmissaoMensal {
  mes?: string | null;
  quantidade?: number;
}

export interface RequestPainelGestaoDTOSaudeContratual {
  total?: number;
  vigentes?: number;
  vencidosMarcadosAtivos?: number;
  vencendoEm30Dias?: number;
  semDataFim?: number;
  semEfetivoPrevisto?: number;
  acimaDoEfetivoPrevisto?: number;
  abaixoDoEfetivoPrevisto?: number;
}

export interface RequestPainelGestaoDTOCoberturaFrequencia {
  data?: string | null;
  empregadosAtivos?: number;
  empregadosEsperados?: number;
  registrosEsperados?: number;
  pendentes?: number;
  percentualCobertura?: number;
  presencas?: number;
  ausencias?: number;
  ausenciasJustificadas?: number;
  faltasNaoJustificadas?: number;
  repousosTrabalhados?: number;
  trabalhosForaEscalaSemRt?: number;
}

export interface RequestPainelGestaoDTOAnalisePeriodo {
  inicio?: string | null;
  fim?: string | null;
  dias?: number;
  registrosOperacionais?: number;
  presencas?: number;
  presencasParciais?: number;
  atrasos?: number;
  faltasNaoJustificadas?: number;
  atestados?: number;
  suspensoes?: number;
  dsrPerdidos?: number;
  repousosTrabalhados?: number;
  empregadosComAusencia?: number;
  empregadosComAusenciaRecorrente?: number;
  taxaPresenca?: number;
  taxaAbsenteismo?: number;
  ultimaAtualizacao?: string | null;
}

export interface RequestPainelGestaoDTOComparativoPeriodo {
  inicioAnterior?: string | null;
  fimAnterior?: string | null;
  registrosOperacionaisAnterior?: number;
  ausenciasAnterior?: number;
  repousosTrabalhadosAnterior?: number;
  taxaPresencaAnterior?: number;
  taxaAbsenteismoAnterior?: number;
  variacaoRegistrosOperacionais?: number;
  variacaoAusencias?: number;
  variacaoRepousosTrabalhados?: number;
  variacaoTaxaPresencaPp?: number;
  variacaoTaxaAbsenteismoPp?: number;
}

export interface RequestPainelGestaoDTOPressaoOperacional {
  repousosTrabalhados?: number;
  empregadosComRt?: number;
  ausenciasNoPeriodo?: number;
  rtPorCemAtivos?: number;
  relacaoRtAusencias?: number;
}

export interface RequestPainelGestaoDTOIntegridadeDados {
  totalInconsistencias?: number;
  empregadosAtivosSemContrato?: number;
  empregadosComMultiplosContratosVigentes?: number;
  empregadoContratoGestaoLocalDivergente?: number;
  empregadoLotacaoGestaoLocalDivergente?: number;
  empregadoJornadaGestaoLocalDivergente?: number;
  jornadasComCalendarioIncompleto?: number;
  empregadosAtivosSemLotacao?: number;
  empregadosAtivosSemJornada?: number;
  adicionaisRtSemFrequencia?: number;
  empregadosSemGestaoLocal?: number;
  contratosSemGestaoLocal?: number;
}

export interface RequestPainelGestaoDTOHorizonteContratual {
  vencidos?: number;
  ate30Dias?: number;
  de31A90Dias?: number;
  de91A180Dias?: number;
  acimaDe180Dias?: number;
  semDataFim?: number;
}

export interface RequestPainelGestaoDTODistribuicaoStatus {
  status?: string | null;
  quantidade?: number;
  percentual?: number;
}

export interface RequestPainelGestaoDTOSituacaoQuadro {
  status?: string | null;
  quantidade?: number;
  percentual?: number;
}

export interface RequestPainelGestaoDTOPadraoSemanal {
  ordem?: number;
  dia?: string | null;
  registrosOperacionais?: number;
  ausencias?: number;
  taxaAbsenteismo?: number;
}

export interface RequestPainelGestaoDTOUnidadeGestao {
  id?: number | null;
  tipo?: string | null;
  nome?: string | null;
  uf?: string | null;
  efetivoPrevisto?: number | null;
  efetivoAtual?: number;
  saldo?: number | null;
  percentualOcupacao?: number | null;
  contratosVigentes?: number;
  alertasContratuais?: number;
  ausenciasNaReferencia?: number;
  empregadosEsperados?: number;
  registrosEsperados?: number;
  pendentesFrequencia?: number;
  percentualCoberturaEscala?: number;
}

export interface RequestPainelGestaoDTOSerieTemporal {
  data?: string | null;
  registros?: number;
  presencas?: number;
  ausencias?: number;
  repousosTrabalhados?: number;
}

export interface RequestPainelGestaoDTOAlertaGestao {
  codigo?: string | null;
  severidade?: string | null;
  titulo?: string | null;
  mensagem?: string | null;
  rota?: string | null;
}

export interface RequestPainelRankingDTO {
  lotacaoId?: number | null;
  nome?: string | null;
  turnoId?: number | null;
  turno?: string | null;
  escalados?: number;
  presencas?: number;
  faltas?: number;
  taxa?: number;
}

export interface RequestPendenciaDTO {
  id?: number | null;
  tipo?: DTO.TipoPendencia | null;
  estado?: DTO.EstadoPendencia | null;
  motivo?: string | null;
  surgidaEm?: string | null;
  dataEfetiva?: string | null;
  operacaoOrigem?: string | null;
  perfilResponsavel?: DTO.PerfilAcesso | null;
  escopoBloqueio?: DTO.EscopoBloqueioPendencia | null;
  contratoId?: number | null;
  contrato?: string | null;
  pessoaId?: number | null;
  pessoa?: string | null;
  vinculoId?: number | null;
  frequenciaId?: number | null;
  gestaoLocalId?: number | null;
  superintendenciaId?: number | null;
  origemEntidade?: string | null;
  origemId?: number | null;
  iniciadaEm?: string | null;
  iniciadaPor?: string | null;
  resolvidaEm?: string | null;
  resolvidaPor?: string | null;
  observacaoTratamento?: string | null;
  acaoSugerida?: string | null;
}

export interface RequestPermissaoDocumentoRestritoDTO {
  id?: number | null;
  documentoReferenciaId?: number | null;
  grupoLogicoId?: string | null;
  usuarioBeneficiarioId?: number | null;
  usuarioBeneficiarioUsername?: string | null;
  finalidade?: string | null;
  justificativa?: string | null;
  autorizacaoAdministrativa?: string | null;
  concedidoEm?: string | null;
  concedidoPorId?: number | null;
  concedidoPorUsername?: string | null;
  validoAte?: string | null;
  ativo?: boolean | null;
  revogadoEm?: string | null;
  revogadoPorId?: number | null;
  revogadoPorUsername?: string | null;
  motivoRevogacao?: string | null;
  version?: number;
}

export interface RequestPermissaoExcepcionalDTO {
  id?: number | null;
  tipo?: DTO.TipoPermissaoExcepcional | null;
  usuarioBeneficiarioId?: number | null;
  usuarioBeneficiario?: string | null;
  perfilBeneficiario?: DTO.PerfilAcesso | null;
  contratoId?: number | null;
  contrato?: string | null;
  gestaoLocalId?: number | null;
  gestaoLocal?: string | null;
  finalidade?: string | null;
  justificativa?: string | null;
  autorizacaoAdministrativa?: string | null;
  concedidoEm?: string | null;
  concedidoPorId?: number | null;
  concedidoPor?: string | null;
  validoAte?: string | null;
  ativa?: boolean;
  revogadoEm?: string | null;
  revogadoPorId?: number | null;
  revogadoPor?: string | null;
  motivoRevogacao?: string | null;
  version?: number;
  ocorrenciaId?: number | null;
}

export interface RequestPessoaDTO {
  id?: number | null;
  nome?: string | null;
  cpf?: string | null;
  rg?: string | null;
  ctpsSerie?: string | null;
  nitPisPasep?: string | null;
  dataNascimento?: string | null;
  email?: string | null;
  endereco?: RequestEnderecoDTO | null;
  criadoEm?: string | null;
  atualizadoEm?: string | null;
  version?: number;
}

export interface RequestPessoaResumoDTO {
  id?: number | null;
  nome?: string | null;
  cpf?: string | null;
  criadoEm?: string | null;
  atualizadoEm?: string | null;
  version?: number;
}

export interface RequestPosicaoAlocacaoTemporalDTO {
  vinculoId?: number | null;
  dataReferencia?: string | null;
  conhecidoAte?: string | null;
  situacaoTemporal?: DTO.SituacaoTemporalAlocacao | null;
  alocacaoId?: number | null;
  composicaoBaseId?: number | null;
  lotacaoId?: number | null;
  lotacaoNome?: string | null;
  jornadaId?: number | null;
  jornadaDescricao?: string | null;
  cargoId?: number | null;
  cargoNome?: string | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  eventoConhecidoMaisRecenteId?: number | null;
}

export interface RequestPosicaoPessoaDTO {
  pessoaId?: number | null;
  nomeAtual?: string | null;
  cpfAtual?: string | null;
  dataReferencia?: string | null;
  conhecidoAte?: string | null;
  vinculos?: Array<RequestPosicaoVinculoTemporalDTO> | null;
  nomeFotografia?: string | null;
  eventoIdentidadeFonteId?: number | null;
  fotografiaIdentidadeComprovada?: boolean;
  identidadeHistorica?: RequestIdentidadeHistoricaResolvidaDTO | null;
}

export interface RequestPosicaoTemporalContratoDTO {
  contratoId?: number | null;
  dataReferencia?: string | null;
  conhecidoAte?: string | null;
  estadoNegocial?: DTO.EstadoNegocialContrato | null;
  situacaoTemporal?: DTO.SituacaoTemporalContrato | null;
  habilitacaoAdministrativa?: DTO.HabilitacaoAdministrativaContrato | null;
  vigenciaInicio?: string | null;
  vigenciaFim?: string | null;
  versaoContratual?: RequestVersaoContratoNegocialDTO | null;
  eventosAplicados?: Array<RequestEventoCicloVidaContratoDTO> | null;
  relacoesConsideradas?: Array<RequestRelacaoPrecedenciaEventoContratoDTO> | null;
}

export interface RequestPosicaoTemporalContratoLeituraDTO {
  contratoId?: number | null;
  dataReferencia?: string | null;
  conhecidoAte?: string | null;
  estadoNegocial?: DTO.EstadoNegocialContrato | null;
  situacaoTemporal?: DTO.SituacaoTemporalContrato | null;
  habilitacaoAdministrativa?: DTO.HabilitacaoAdministrativaContrato | null;
  vigenciaInicio?: string | null;
  vigenciaFim?: string | null;
  versaoContratual?: RequestVersaoContratoLeituraDTO | null;
  eventosAplicados?: Array<RequestEventoCicloVidaContratoDTO> | null;
  relacoesConsideradas?: Array<RequestRelacaoPrecedenciaEventoContratoDTO> | null;
  provaCampos?: Record<string, RequestProvaCampoHistoricoDTO> | null;
}

export interface RequestPosicaoVinculoTemporalDTO {
  vinculoId?: number | null;
  pessoaId?: number | null;
  contratoId?: number | null;
  dataReferencia?: string | null;
  conhecidoAte?: string | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  matricula?: string | null;
  estadoNegocial?: DTO.EstadoNegocialVinculo | null;
  situacaoTemporal?: DTO.SituacaoTemporalVinculo | null;
  eventoConhecidoMaisRecenteId?: number | null;
}

export interface RequestPreviaAditamentoContratualDTO {
  contratoId?: number | null;
  dataEfetiva?: string | null;
  efetivoAnterior?: number | null;
  efetivoResultante?: number | null;
  efetivoOriginalPublicado?: number | null;
  pisoNormalReducao?: number | null;
  reducao?: boolean;
  excedeLimiteNormal25?: boolean;
  exigeAutorizacaoExcepcional?: boolean;
  possuiImpactoOcupacao?: boolean;
  impactosComposicao?: Array<RequestImpactoComposicaoContratoDTO> | null;
  alertas?: Array<string> | null;
  tokenPrevia?: string | null;
}

export interface RequestPreviaCicloVidaContratoDTO {
  contratoId?: number | null;
  operacao?: string | null;
  dataEfetiva?: string | null;
  dataAto?: string | null;
  motivo?: string | null;
  usuarioResponsavelId?: number | null;
  usuarioResponsavelUsername?: string | null;
  autorizacaoNecessaria?: string | null;
  quantidadeVinculosAfetados?: number;
  vinculosAfetados?: Array<RequestPreviaCicloVidaContratoDTOVinculoAfetado> | null;
  programacoesFuturasAfetadas?: Array<RequestPreviaCicloVidaContratoDTOProgramacaoAfetada> | null;
  quantidadeFrequenciasAfetadas?: number;
  datasComFrequenciasAfetadas?: Array<string> | null;
  impactosConhecidos?: Array<string> | null;
  exigeConfirmacao?: boolean;
  tokenPrevia?: string | null;
  limiteDetalhes?: number;
  vinculosTruncados?: boolean;
  quantidadeProgramacoesFuturasAfetadas?: number;
  programacoesTruncadas?: boolean;
  quantidadeDatasComFrequenciasAfetadas?: number;
  datasComFrequenciasTruncadas?: boolean;
}

export interface RequestPreviaCicloVidaContratoDTOVinculoAfetado {
  vinculoId?: number | null;
  pessoaId?: number | null;
  pessoaNome?: string | null;
  matricula?: string | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  programado?: boolean;
}

export interface RequestPreviaCicloVidaContratoDTOProgramacaoAfetada {
  tipo?: string | null;
  id?: number | null;
  vinculoId?: number | null;
  dataEfetiva?: string | null;
  descricao?: string | null;
}

export interface RequestPreviaImpactoAlocacaoDTO {
  token?: string | null;
  vinculoId?: number | null;
  alocacaoAfetadaId?: number | null;
  dataEfetiva?: string | null;
  fimFaixaAtual?: string | null;
  impactos?: Array<string> | null;
  incompatibilidades?: Array<string> | null;
}

export interface RequestPreviaImpactoVinculoDTO {
  token?: string | null;
  vinculoId?: number | null;
  valorAtual?: string | null;
  valorProposto?: string | null;
  impactos?: Array<string> | null;
  vinculosConflitantes?: Array<number> | null;
}

export interface RequestPreviaProvaHistoricaContratoDTO {
  tokenPrevia?: string | null;
  contratoId?: number | null;
  versaoContrato?: number | null;
  dataEfetiva?: string | null;
  recuperaPublicacao?: boolean;
  composicoesComprovadas?: number;
  efeito?: string | null;
}

export interface RequestPreviaProvaHistoricaDTO {
  tokenPrevia?: string | null;
  impactos?: Array<string> | null;
}

export interface RequestPreviaRemanejamentoContratualDTO {
  contratoId?: number | null;
  dataEfetiva?: string | null;
  efetivoTotalAntes?: number | null;
  efetivoTotalDepois?: number | null;
  somaVariacoes?: number | null;
  totalPreservado?: boolean;
  possuiImpactoOcupacao?: boolean;
  impactosComposicao?: Array<RequestImpactoComposicaoContratoDTO> | null;
  alertas?: Array<string> | null;
  tokenPrevia?: string | null;
}

export interface RequestPreviaTransicaoCatalogoDTO {
  tipoCatalogo?: DTO.TipoCatalogoTemporal | null;
  catalogoId?: number | null;
  dataEfetiva?: string | null;
  ativoAnterior?: boolean | null;
  ativo?: boolean;
  ativoAtualAposRegistro?: boolean;
  versaoAtual?: number;
  tokenPrevia?: string | null;
  impactos?: Array<string> | null;
}

export interface RequestProvaCampoHistoricoDTO {
  estado?: DTO.Estado | null;
  visibilidade?: DTO.Visibilidade | null;
  origens?: Array<RequestOrigemProvaCampoDTO> | null;
}

export interface RequestProvaHistoricaContratoDTO {
  id?: number | null;
  contratoId?: number | null;
  eventoOrigemId?: number | null;
  provaAnteriorId?: number | null;
  numeroRevisao?: number | null;
  versaoAnteriorId?: number | null;
  versaoGeradaId?: number | null;
  dataEfetiva?: string | null;
  registradoEm?: string | null;
  hashConteudo?: string | null;
  configuracaoCompletaComprovada?: boolean;
  camposComprovados?: Array<string> | null;
  fotografia?: RequestVersaoContratoLeituraDTO | null;
  composicoes?: Array<RequestFragmentoProvaHistoricaComposicaoDTO> | null;
  provaCampos?: Record<string, RequestProvaCampoHistoricoDTO> | null;
}

export interface RequestProvaHistoricaContratoRequisicao {
  eventoOrigemId?: number | null;
  versaoAnteriorId?: number | null;
  provaAnteriorId?: number | null;
  recuperarPublicacaoOriginal?: boolean | null;
  dataDocumento?: string | null;
  referenciaSei?: string | null;
  documentoComprobatorioId?: number | null;
  finalidadeAcessoDocumento?: string | null;
  motivo?: string | null;
  autorizacaoAdministrativa?: string | null;
  versaoContratoEsperada?: number | null;
  chaveIdempotencia?: string | null;
  configuracaoCompletaComprovada?: boolean | null;
  camposComprovados?: Array<string> | null;
  fotografia?: RequestVersaoContratoNegocialDTO | null;
  composicoes?: Array<RequestProvaHistoricaContratoRequisicaoItem> | null;
  tokenPrevia?: string | null;
  confirmarImpacto?: boolean | null;
}

export interface RequestProvaHistoricaContratoRequisicaoItem {
  composicaoId?: number | null;
  mapeamentoId?: number | null;
  aditamentoComposicaoId?: number | null;
  lotacaoNome?: string | null;
  jornadaDescricao?: string | null;
  cargoNome?: string | null;
  lotacaoMunicipio?: string | null;
  municipioComprovado?: boolean | null;
  jornadaSemanal?: string | null;
  jornadaComprovada?: boolean | null;
}

export interface RequestPublicacaoContratoDTO {
  dataPublicacao?: string | null;
  referenciaSei?: string | null;
  justificativa?: string | null;
  tokenPrevia?: string | null;
  confirmarImpacto?: boolean | null;
}

export interface RequestRedefinicaoSenhaUsuarioAplicacaoDTO {
  newPassword: string;
}

export interface RequestRegistroAuditoriaDTO {
  id?: number | null;
  createdAt?: string | null;
  requestId?: string | null;
  userId?: number | null;
  username?: string | null;
  action?: string | null;
  entityType?: string | null;
  entityId?: number | null;
  entityLabel?: string | null;
  ip?: string | null;
  userAgent?: string | null;
  httpMethod?: string | null;
  path?: string | null;
  queryString?: string | null;
  payloadJson?: string | null;
  payloadTruncated?: boolean | null;
  detailsJson?: string | null;
}

export interface RequestRegistroProvaMotivoHistoricoRequisicao {
  contratoId?: number | null;
  descricaoComprovada?: string | null;
  categoriaComprovada?: DTO.CategoriaTerminoVinculo | null;
  versaoMotivoEsperada?: number | null;
  versaoContratoEsperada?: number | null;
  dataInicioEfetiva?: string | null;
  dataFimEfetiva?: string | null;
  referenciaSei?: string | null;
  documentoComprobatorioId?: number | null;
  finalidadeAcessoDocumento?: string | null;
  motivo?: string | null;
  autorizacao?: string | null;
  provaAnteriorId?: number | null;
  chaveIdempotencia?: string | null;
  confirmarImpacto?: boolean | null;
  tokenPrevia?: string | null;
}

export interface RequestRegistroProvaNomeHistoricoRequisicao {
  vinculoId?: number | null;
  nomeComprovado?: string | null;
  eventoOrigemRetificadoId?: number | null;
  versaoPessoaEsperada?: number | null;
  versaoVinculoEsperada?: number | null;
  dataInicioEfetiva?: string | null;
  dataFimEfetiva?: string | null;
  referenciaSei?: string | null;
  documentoComprobatorioId?: number | null;
  finalidadeAcessoDocumento?: string | null;
  motivo?: string | null;
  autorizacao?: string | null;
  provaAnteriorId?: number | null;
  chaveIdempotencia?: string | null;
  confirmarImpacto?: boolean | null;
  tokenPrevia?: string | null;
}

export interface RequestRegraAdicionalDTO {
  id?: number | null;
  tipo?: string | null;
  inicio?: string | null;
  fim?: string | null;
  percentual?: number | null;
  ativo?: boolean | null;
  ordem?: number | null;
}

export interface RequestRelacaoPrecedenciaEventoContratoDTO {
  id?: number | null;
  eventoFonteId?: number | null;
  eventoAlvoId?: number | null;
  tipoRelacao?: DTO.TipoRelacaoPrecedenciaNegocial | null;
  registradoEm?: string | null;
  usuarioResponsavelId?: number | null;
  usuarioResponsavelUsername?: string | null;
  motivo?: string | null;
}

export interface RequestRelatorioAbsenteismoDTO {
  empregadoId?: number | null;
  nome?: string | null;
  cargo?: string | null;
  lotacao?: string | null;
  turno?: string | null;
  faltas?: number;
  atestados?: number;
  dsrPerdido?: number;
  spells?: number;
  taxaIndividual?: number;
  disrupcao?: number;
  diasTrabalhaveis?: number;
  minutosPrevistos?: number;
}

export interface RequestRelatorioAbsenteismoPaginaDTO {
  registros?: Array<RequestRelatorioAbsenteismoDTO> | null;
  totalColaboradores?: number;
  pagina?: number;
  tamanho?: number;
  totalPaginas?: number;
  primeira?: boolean;
  ultima?: boolean;
  revisao?: RequestAvisoRevisaoRelatorioDTO | null;
}

export interface RequestRelatorioAbsenteismoResumoDTO {
  faltas?: number;
  atestados?: number;
  dsrPerdido?: number;
  totalColaboradores?: number;
  diasTrabalhaveis?: number;
  alertasDisrupcao?: number;
  taxaGeral?: number;
  maioresTaxas?: Array<RequestRelatorioAbsenteismoDTO> | null;
  revisao?: RequestAvisoRevisaoRelatorioDTO | null;
}

export interface RequestRelatorioAdministrativoDTO {
  metadados?: RequestMetadadosEmissaoRelatorioDTO | null;
  conteudo?: import('./shared').JsonValue | null;
}

export interface RequestRelatorioFiltrosDTO {
  contratos?: Array<RequestContratoOpcaoDTO> | null;
  lotacoes?: Array<RequestLotacaoOpcaoDTO> | null;
  turnos?: Array<RequestJornadaResumoDTO> | null;
}

export interface RequestRelatorioFrequenciaConsolidadoDTO {
  completo?: boolean;
  calculoVersao?: string | null;
  inicio?: string | null;
  fim?: string | null;
  contratos?: Array<number> | null;
  registros?: Array<RequestRelatorioFrequenciaConsolidadoDTOLinha> | null;
  totais?: RequestRelatorioFrequenciaConsolidadoDTOTotais | null;
  revisao?: RequestAvisoRevisaoRelatorioDTO | null;
}

export interface RequestRelatorioFrequenciaConsolidadoDTOLinha {
  chaveAlocacao?: string | null;
  empregadoId?: number | null;
  nome?: string | null;
  contratoId?: number | null;
  contrato?: string | null;
  lotacaoId?: number | null;
  lotacao?: string | null;
  jornadaId?: number | null;
  turno?: string | null;
  cargoId?: number | null;
  cargo?: string | null;
  diario?: Record<string, RequestRelatorioFrequenciaConsolidadoDTODia> | null;
  totais?: RequestRelatorioFrequenciaConsolidadoDTOTotais | null;
}

export interface RequestRelatorioFrequenciaConsolidadoDTODia {
  data?: string | null;
  diaTrabalho?: boolean;
  minutosPrevistos?: number;
  lancado?: boolean;
  status?: string | null;
  entrada1?: string | null;
  saida1?: string | null;
  entrada2?: string | null;
  saida2?: string | null;
  minutosFisicos?: number;
  minutosNoturnosReais?: number;
  minutosNoturnosReduzidos?: number;
  minutosAdicionais?: number;
  minutosPagaveis?: number;
  calculoVersao?: string | null;
  ocorrenciaSobreposta?: boolean;
}

export interface RequestRelatorioFrequenciaConsolidadoDTOTotais {
  diasTrabalhaveis?: number;
  diasLancados?: number;
  presencas?: number;
  presencasParciais?: number;
  faltas?: number;
  atestados?: number;
  descansos?: number;
  semLancamento?: number;
  minutosPrevistos?: number;
  minutosFisicos?: number;
  minutosNoturnosReais?: number;
  minutosNoturnosReduzidos?: number;
  minutosAdicionais?: number;
  minutosPagaveis?: number;
}

export interface RequestRelatorioFrequenciaPaginaDTO {
  completo?: boolean;
  calculoVersao?: string | null;
  inicio?: string | null;
  fim?: string | null;
  contratos?: Array<number> | null;
  registros?: Array<RequestRelatorioFrequenciaConsolidadoDTOLinha> | null;
  totalColaboradores?: number;
  pagina?: number;
  tamanho?: number;
  totalPaginas?: number;
  primeira?: boolean;
  ultima?: boolean;
  revisao?: RequestAvisoRevisaoRelatorioDTO | null;
}

export interface RequestRelatorioHorasDTO {
  empregadoId?: number | null;
  nome?: string | null;
  cargo?: string | null;
  lotacao?: string | null;
  turno?: string | null;
  totalPresencas?: number;
  totalPresencasParciais?: number;
  totalHorasTrabalhadas?: string | null;
  totalHorasNoturnas?: number | null;
}

export interface RequestRelatorioTerceirizadosDTO {
  referencia?: string | null;
  geradoEm?: string | null;
  escopo?: string | null;
  indicadores?: RequestRelatorioTerceirizadosDTOIndicadores | null;
  distribuicaoStatus?: Array<RequestRelatorioTerceirizadosDTODistribuicao> | null;
  distribuicaoContratos?: Array<RequestRelatorioTerceirizadosDTODistribuicaoContrato> | null;
  statusDisponiveis?: Array<RequestRelatorioTerceirizadosDTOOpcaoTexto> | null;
  lotacoes?: Array<RequestRelatorioTerceirizadosDTOOpcao> | null;
  contratos?: Array<RequestRelatorioTerceirizadosDTOOpcao> | null;
  empregados?: RequestRelatorioTerceirizadosDTOPaginaEmpregados | null;
}

export interface RequestRelatorioTerceirizadosDTOIndicadores {
  empregados?: number;
  ativos?: number;
  semContratoVigente?: number;
  admitidosNoMes?: number;
  contratosVigentes?: number;
  efetivoPrevisto?: number;
  efetivoVinculado?: number;
  saldoContratual?: number;
}

export interface RequestRelatorioTerceirizadosDTODistribuicao {
  nome?: string | null;
  quantidade?: number;
}

export interface RequestRelatorioTerceirizadosDTODistribuicaoContrato {
  id?: number | null;
  nome?: string | null;
  quantidade?: number;
  semContrato?: boolean;
}

export interface RequestRelatorioTerceirizadosDTOOpcao {
  id?: number | null;
  nome?: string | null;
}

export interface RequestRelatorioTerceirizadosDTOOpcaoTexto {
  valor?: string | null;
  nome?: string | null;
}

export interface RequestRelatorioTerceirizadosDTOContratoResumo {
  id?: number | null;
  numeroContrato?: string | null;
  descricao?: string | null;
  vigente?: boolean;
}

export interface RequestRelatorioTerceirizadosDTOEmpregadoResumo {
  id?: number | null;
  nome?: string | null;
  matricula?: string | null;
  cpf?: string | null;
  statusDescricao?: string | null;
  lotacaoId?: number | null;
  lotacaoNome?: string | null;
  cargoNome?: string | null;
  jornadaDescricao?: string | null;
  dataAdmissao?: string | null;
  contratos?: Array<RequestRelatorioTerceirizadosDTOContratoResumo> | null;
}

export interface RequestRelatorioTerceirizadosDTOPaginaEmpregados {
  content?: Array<RequestRelatorioTerceirizadosDTOEmpregadoResumo> | null;
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
  first?: boolean;
  last?: boolean;
}

export interface RequestRemanejamentoComposicaoRequisicao {
  composicaoId?: number | null;
  lotacaoId?: number | null;
  jornadaId?: number | null;
  cargoId?: number | null;
  variacao?: number | null;
}

export interface RequestRemanejamentoContratualDTO {
  id?: number | null;
  contratoId?: number | null;
  eventoId?: number | null;
  identificacao?: string | null;
  versaoInstrumento?: number | null;
  dataEfetiva?: string | null;
  justificativa?: string | null;
  referenciaSei?: string | null;
  registradoEm?: string | null;
  usuarioResponsavelId?: number | null;
  usuarioResponsavelUsername?: string | null;
  situacao?: DTO.SituacaoFatoNegocial | null;
  somaVariacoes?: number | null;
  itens?: Array<RequestImpactoComposicaoContratoDTO> | null;
}

export interface RequestRemanejamentoContratualRequisicao {
  identificacao?: string | null;
  dataEfetiva?: string | null;
  justificativa?: string | null;
  referenciaSei?: string | null;
  confirmarImpacto?: boolean | null;
  itens?: Array<RequestRemanejamentoComposicaoRequisicao> | null;
  eventoPrecedenteId?: number | null;
  relacaoPrecedencia?: DTO.TipoRelacaoPrecedenciaNegocial | null;
  ordemDocumental?: number | null;
  tokenPrevia?: string | null;
}

export interface RequestRenovacaoSessaoDTO {
  password?: string | null;
}

export interface RequestRequisicaoCriacaoFechamentoFrequencia {
  dataReferencia: string;
  lotacaoId: number;
  jornadaId: number;
  contratoIds?: Array<number> | null;
  alteracoes?: Array<RequestFrequenciaDTO> | null;
  justificativaRetificacao?: string | null;
  chaveIdempotencia?: string | null;
}

export interface RequestRespostaCacheConsultaHttp {
  status?: number;
  contentType?: string | null;
  body?: number[] | null;
}

export interface RequestResultadoAlteracaoSenhaDTO {
  message?: string | null;
  reauthenticationRequired?: boolean;
}

export interface RequestResultadoEnvioAvatarDTO {
  fotoUrl?: string | null;
  message?: string | null;
}

export interface RequestResultadoRedefinicaoSenhaDTO {
  message?: string | null;
  forceChangeOnNextLogin?: boolean;
}

export interface RequestResultadoSegundoFatorDTO {
  enabled?: boolean;
  message?: string | null;
}

export interface RequestResultadoValidacaoSenhaDTO {
  valid?: boolean;
  message?: string | null;
}

export interface RequestResumoPendenciasDTO {
  abertas?: number;
  emTratamento?: number;
  resolvidas?: number;
  retificacoesFrequencia?: number;
  finsNaturaisVigencia?: number;
}

export interface RequestResumoUsuarioAplicacaoDTO {
  id?: number | null;
  username?: string | null;
  email?: string | null;
  matricula?: string | null;
  perfil?: DTO.PerfilAcesso | null;
  autoridade?: string | null;
  active?: boolean | null;
  forceChangePassword?: boolean | null;
  locked?: boolean | null;
  lockoutEnd?: string | null;
  lotacao?: RequestResumoUsuarioAplicacaoDTOReferencia | null;
  jornada?: RequestResumoUsuarioAplicacaoDTOReferencia | null;
  vinculosGestao?: Array<RequestResumoUsuarioAplicacaoDTOVinculoGestaoReferencia> | null;
  superintendencia?: RequestResumoUsuarioAplicacaoDTOOrganizacaoReferencia | null;
  gestaoLocal?: RequestResumoUsuarioAplicacaoDTOOrganizacaoReferencia | null;
  retroactiveUnlocked?: boolean | null;
  retroactiveUnlockExpireAt?: string | null;
  fotoUrl?: string | null;
  createdAt?: string | null;
}

export interface RequestResumoUsuarioAplicacaoDTOReferencia {
  id?: number | null;
  label?: string | null;
}

export interface RequestResumoUsuarioAplicacaoDTOOrganizacaoReferencia {
  id?: number | null;
  label?: string | null;
  codigo?: string | null;
  uf?: string | null;
}

export interface RequestResumoUsuarioAplicacaoDTOVinculoGestaoReferencia {
  id?: number | null;
  lotacao?: RequestResumoUsuarioAplicacaoDTOReferencia | null;
  jornada?: RequestResumoUsuarioAplicacaoDTOReferencia | null;
}

export interface RequestRetificacaoInicioVinculoRequisicao {
  novoInicio?: string | null;
  justificativa?: string | null;
  evidencia?: string | null;
  autorizacao?: string | null;
  confirmarImpacto?: boolean | null;
  tokenPrevia?: string | null;
  confirmarInclusaoExcedente?: boolean | null;
}

export interface RequestRetificacaoSeiDocumentoRequisicao {
  novaReferenciaSei?: string | null;
  motivo?: string | null;
  chaveIdempotencia?: string | null;
}

export interface RequestRevogacaoPermissaoExcepcionalRequisicao {
  motivo: string;
}

export interface RequestSenhaAtualDTO {
  currentPassword?: string | null;
}

export interface RequestSolicitacaoRecuperacaoSenhaDTO {
  email: string;
}

export interface RequestStatusEmpregadoDTO {
  id?: number | null;
  descricao: string;
  version?: number;
}

export interface RequestStatusSegundoFatorDTO {
  enabled?: boolean;
  setupPending?: boolean;
}

export interface RequestSubstituicaoDocumentoComprobatorioRequisicao {
  titulo?: string | null;
  descricao?: string | null;
  referenciaSei?: string | null;
  motivo?: string | null;
  chaveIdempotencia?: string | null;
}

export interface RequestSuperintendenciaDTO {
  id?: number | null;
  nome?: string | null;
  uf?: string | null;
  ativo?: boolean | null;
}

export interface RequestTipoContratacaoDTO {
  id?: number | null;
  descricao?: string | null;
  ativo?: boolean | null;
  matriculaVinculoObrigatoria?: boolean | null;
  gestaoLocalId?: number | null;
  gestaoLocalNome?: string | null;
  superintendenciaId?: number | null;
  superintendenciaNome?: string | null;
  uf?: string | null;
  criadoEm?: string | null;
  criadoPorId?: number | null;
  criadoPorUsername?: string | null;
  atualizadoEm?: string | null;
  version?: number;
}

export interface RequestTokenCsrfDTO {
  headerName?: string | null;
  parameterName?: string | null;
  token?: string | null;
}

export interface RequestTokenTotpDTO {
  token?: string | null;
}

export interface RequestTransferenciaGestaoContratoDTO {
  id?: number | null;
  contratoId?: number | null;
  eventoContratoId?: number | null;
  gestaoOrigemId?: number | null;
  gestaoOrigemNome?: string | null;
  gestaoDestinoId?: number | null;
  gestaoDestinoNome?: string | null;
  dataEfetiva?: string | null;
  motivo?: string | null;
  referenciaSei?: string | null;
  autorizacaoAdministrativa?: string | null;
  registradoEm?: string | null;
  responsavelId?: number | null;
  responsavelUsername?: string | null;
  mapeamentos?: Array<RequestMapeamentoTransferenciaGestaoDTO> | null;
  tipoContratacaoDestinoId?: number | null;
  tipoContratacaoDestinoDescricao?: string | null;
  conferenciaDocumental?: DTO.ConferenciaDocumentalAtoDTO | null;
}

export interface RequestTransferenciaGestaoContratoRequisicao {
  dataEfetiva?: string | null;
  dataAto?: string | null;
  gestaoLocalDestinoId?: number | null;
  motivo?: string | null;
  referenciaSei?: string | null;
  autorizacaoAdministrativa?: string | null;
  mapeamentos?: Array<RequestMapeamentoTransferenciaGestaoRequisicao> | null;
  eventoPrecedenteId?: number | null;
  relacaoPrecedencia?: DTO.TipoRelacaoPrecedenciaNegocial | null;
  ordemDocumental?: number | null;
  tipoContratacaoDestinoId?: number | null;
  tokenPrevia?: string | null;
  confirmarImpacto?: boolean | null;
  conferenciaDocumentalConfirmada?: boolean | null;
  versaoConferenciaDocumental?: string | null;
}

export interface RequestTransicaoCatalogoDTO {
  eventoId?: number | null;
  tipoCatalogo?: DTO.TipoCatalogoTemporal | null;
  catalogoId?: number | null;
  dataEfetiva?: string | null;
  ativo?: boolean;
  registradoEm?: string | null;
  ativoAtualAposRegistro?: boolean | null;
  versaoAtual?: number;
  reenvio?: boolean;
}

export interface RequestTransicaoDisponibilidadeCatalogoRequisicao {
  dataEfetiva?: string | null;
  ativo?: boolean | null;
  motivo?: string | null;
  evidencia?: string | null;
  autorizacao?: string | null;
  versaoEsperada?: number | null;
  chaveIdempotencia?: string | null;
  confirmarImpacto?: boolean | null;
  tokenPrevia?: string | null;
}

export interface RequestTratamentoPendenciaRequisicao {
  observacao?: string | null;
}

export interface RequestUsoPermissaoExcepcionalDTO {
  id?: number | null;
  permissaoId?: number | null;
  tipo?: DTO.TipoPermissaoExcepcional | null;
  usuarioId?: number | null;
  usuario?: string | null;
  tipoRecurso?: string | null;
  recursoId?: number | null;
  finalidade?: string | null;
  usadoEm?: string | null;
}

export interface RequestUsuarioReferenciaDTO {
  id?: number | null;
  username?: string | null;
}

export interface RequestValidacaoSenhaDTO {
  password?: string | null;
}

export interface RequestValidacaoTokenRecuperacaoSenhaDTO {
  valid?: boolean;
  message?: string | null;
}

export interface RequestVersaoCalendarioEventoDTO {
  id?: number | null;
  calendarioEventoId?: number | null;
  numeroVersao?: number;
  tipoOperacao?: string | null;
  dataEfetiva?: string | null;
  registradoEm?: string | null;
  usuarioId?: number | null;
  usuarioUsername?: string | null;
  justificativa?: string | null;
  motivoTecnico?: string | null;
  versaoAnteriorId?: number | null;
  fotografia?: RequestCalendarioEventoDTO | null;
  hashAnterior?: string | null;
  hashVersao?: string | null;
}

export interface RequestVersaoComplementoFrequenciaDTO {
  id?: number | null;
  tipoComplemento?: string | null;
  complementoId?: number | null;
  numeroVersao?: number | null;
  tipoOperacao?: string | null;
  dataEfetiva?: string | null;
  registradoEm?: string | null;
  usuarioId?: number | null;
  usuarioUsername?: string | null;
  justificativa?: string | null;
  motivoTecnico?: string | null;
  versaoAnteriorId?: number | null;
  fotografia?: import('./shared').JsonValue | null;
  contextos?: Array<RequestContextoComplementoFrequenciaDTO> | null;
  hashAnterior?: string | null;
  hashVersao?: string | null;
}

export interface RequestVersaoContratoLeituraDTO extends RequestVersaoContratoNegocialDTO {
  provaCampos?: Record<string, RequestProvaCampoHistoricoDTO> | null;
}

export interface RequestVersaoContratoNegocialDTO {
  id?: number | null;
  numeroVersao?: number | null;
  eventoOrigemId?: number | null;
  dataEfetiva?: string | null;
  registradoEm?: string | null;
  numeroContrato?: string | null;
  anoContrato?: number | null;
  descricao?: string | null;
  tipoContratacaoId?: number | null;
  tipoContratacaoDescricao?: string | null;
  fornecedorId?: number | null;
  cnpjEmpresa?: string | null;
  razaoSocialEmpresa?: string | null;
  nomeFantasiaEmpresa?: string | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  efetivoPrevisto?: number | null;
  gestaoLocalId?: number | null;
  gestaoLocalNome?: string | null;
  matriculaVinculoObrigatoria?: boolean | null;
  matriculaTipoContratacaoObrigatoria?: boolean | null;
  matriculaVinculoObrigatoriaEfetiva?: boolean | null;
  diaSemanaReduzido?: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY" | null;
  horasDiaReduzido?: number | null;
  habilitacaoAdministrativa?: DTO.HabilitacaoAdministrativaContrato | null;
  tipoContratacaoComprovado?: boolean | null;
}

export interface RequestVinculoContratualDTO {
  id?: number | null;
  pessoaId?: number | null;
  pessoaNome?: string | null;
  pessoaCpf?: string | null;
  contratoId?: number | null;
  contratoNumero?: string | null;
  contratoAno?: number | null;
  gestaoLocalId?: number | null;
  gestaoLocalNome?: string | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  matricula?: string | null;
  estadoNegocial?: DTO.EstadoNegocialVinculo | null;
  situacaoTemporal?: DTO.SituacaoTemporalVinculo | null;
  motivoDesligamentoId?: number | null;
  motivoDesligamentoDescricao?: string | null;
  desligamentoRegistradoEm?: string | null;
  composicaoInicialId?: number | null;
  lotacaoInicialId?: number | null;
  jornadaInicialId?: number | null;
  cargoInicialId?: number | null;
  version?: number;
}

export interface RequestAtividadePainelDTO {
  id?: number | null;
  dataEvento?: string | null;
  descricao?: string | null;
  empregadoId?: number | null;
  empregadoNome?: string | null;
  usuarioId?: number | null;
  usuarioUsername?: string | null;
}

export interface RequestEstatisticasPainelDTO {
  empregados?: number;
  ativos?: number;
  inativos?: number;
  cargos?: number;
  lotacoes?: number;
}

export interface RequestFrequenciaDTO {
  id?: number | null;
  empregado?: RequestFrequenciaDTOEmpregadoResumoDTO | null;
  dataReferencia?: string | null;
  mes?: number | null;
  ano?: number | null;
  status?: string | null;
  entrada1?: string | null;
  saida1?: string | null;
  entrada2?: string | null;
  saida2?: string | null;
  horasTrabalhadas?: string | null;
  horarioSaida?: string | null;
  horasNoturnas?: number | null;
  observacao?: string | null;
  prazoEdicao?: string | null;
  updatedAt?: string | null;
  pessoaId?: number | null;
  vinculoContratualId?: number | null;
  alocacaoVinculoId?: number | null;
  contratoId?: number | null;
  lotacaoId?: number | null;
  jornadaId?: number | null;
  cargoId?: number | null;
  gestaoLocalId?: number | null;
  escopoHistoricoEfetivo?: RequestContextoComplementoFrequenciaDTO | null;
  estadoNegocial?: DTO.EstadoNegocialFrequencia | null;
  validadeOperacional?: DTO.ValidadeOperacionalFrequencia | null;
  estadoAuditoria?: DTO.EstadoAuditoriaFrequencia | null;
  bloqueadaEdicaoOperacional?: boolean | null;
  versaoNegocial?: number | null;
  justificativaRetificacao?: string | null;
  chaveIdempotencia?: string | null;
}

export interface RequestFrequenciaDTOEmpregadoResumoDTO {
  id?: number | null;
  nome?: string | null;
}

