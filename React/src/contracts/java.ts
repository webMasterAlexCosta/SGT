/** Contracts mirrored from Java DTO declarations. Source map: contracts-java.json. */
import type { CivilDate, Instant, LocalDateTime, LocalTime, JsonValue } from './shared';

export interface AcaoCicloVidaContratoDTO {
  dataEfetiva: CivilDate | null;
  dataAto: CivilDate | null;
  motivo: string | null;
  referenciaSei: string | null;
  eventoPrecedenteId: number | null;
  relacaoPrecedencia: TipoRelacaoPrecedenciaNegocial | null;
  ordemDocumental: number | null;
  tokenPrevia: string | null;
  confirmarImpacto: boolean | null;
}

export interface AcaoDocumentoComprobatorioRequisicao {
  motivo: string | null;
  autorizacaoAdministrativa: string | null;
}

export interface AcaoVinculoContratualRequisicao {
  justificativa: string | null;
  evidencia: string | null;
  autorizacao: string | null;
  confirmarImpacto: boolean | null;
  tokenPrevia: string | null;
  confirmarInclusaoExcedente: boolean | null;
}

export interface AcessoDocumentoRestritoDTO {
  id: number | null;
  documentoId: number | null;
  tipoAcesso: TipoAcessoDocumento | null;
  finalidade: string | null;
  usuarioId: number | null;
  usuarioUsername: string | null;
  perfisFotografia: string | null;
  permissaoId: number | null;
  acessadoEm: Instant | null;
}

export interface AditamentoComposicaoRequisicao {
  composicaoId: number | null;
  lotacaoId: number | null;
  jornadaId: number | null;
  cargoId: number | null;
  quantidadeResultante: number | null;
}

export interface AditamentoContratualDTO {
  id: number | null;
  contratoId: number | null;
  eventoId: number | null;
  identificacao: string | null;
  versaoInstrumento: number | null;
  dataDocumento: CivilDate | null;
  dataAssinatura: CivilDate | null;
  dataEfetiva: CivilDate | null;
  justificativa: string | null;
  referenciaSei: string | null;
  registradoEm: Instant | null;
  usuarioResponsavelId: number | null;
  usuarioResponsavelUsername: string | null;
  situacao: SituacaoFatoNegocial | null;
  novaDescricao: string | null;
  novoTipoContratacaoId: number | null;
  novoTipoContratacaoDescricao: string | null;
  novoFornecedorId: number | null;
  novoCnpjEmpresa: string | null;
  novaRazaoSocialEmpresa: string | null;
  novoNomeFantasiaEmpresa: string | null;
  novaDataInicio: CivilDate | null;
  novaDataFim: CivilDate | null;
  novoEfetivoTotal: number | null;
  reducaoExcepcional: boolean | null;
  fundamentoExcecao: string | null;
  impactoConfirmado: boolean | null;
  alteracoesComposicao: Array<ImpactoComposicaoContratoDTO> | null;
  novaMatriculaVinculoObrigatoria: boolean | null;
  novoDiaSemanaReduzido: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY" | null;
  novasHorasDiaReduzido: number | null;
  restaurarPoliticaMatriculaDoTipo: boolean | null;
  removerConfiguracaoDiaReduzido: boolean | null;
}

export interface AditamentoContratualRequisicao {
  identificacao: string | null;
  dataDocumento: CivilDate | null;
  dataAssinatura: CivilDate | null;
  dataEfetiva: CivilDate | null;
  justificativa: string | null;
  referenciaSei: string | null;
  novaDescricao: string | null;
  novoTipoContratacaoId: number | null;
  novoFornecedorId: number | null;
  novoCnpjEmpresa: string | null;
  novaRazaoSocialEmpresa: string | null;
  novoNomeFantasiaEmpresa: string | null;
  novaDataInicio: CivilDate | null;
  novaDataFim: CivilDate | null;
  novoEfetivoTotal: number | null;
  reducaoExcepcional: boolean | null;
  fundamentoExcecao: string | null;
  confirmarImpacto: boolean | null;
  alteracoesComposicao: Array<AditamentoComposicaoRequisicao> | null;
  eventoPrecedenteId: number | null;
  relacaoPrecedencia: TipoRelacaoPrecedenciaNegocial | null;
  ordemDocumental: number | null;
  tokenPrevia: string | null;
  novaMatriculaVinculoObrigatoria: boolean | null;
  novoDiaSemanaReduzido: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY" | null;
  novasHorasDiaReduzido: number | null;
  restaurarPoliticaMatriculaDoTipo: boolean | null;
  removerConfiguracaoDiaReduzido: boolean | null;
}

export interface AgenciaReferenciaDTO {
  id: number | null;
  numero: string | null;
  banco: BancoReferenciaDTO | null;
  version: number;
}

export interface AlocacaoVinculoDTO {
  id: number | null;
  vinculoId: number | null;
  composicaoBaseId: number | null;
  lotacaoId: number | null;
  lotacaoNome: string | null;
  jornadaId: number | null;
  jornadaDescricao: string | null;
  cargoId: number | null;
  cargoNome: string | null;
  dataInicio: CivilDate | null;
  dataFim: CivilDate | null;
  estadoNegocial: EstadoNegocialAlocacao | null;
  criadoEm: Instant | null;
  criadoPorId: number | null;
  criadoPorUsername: string | null;
  version: number;
}

export interface AlteracaoAlocacaoVinculoRequisicao {
  dataEfetiva: CivilDate | null;
  composicaoId: number | null;
  lotacaoId: number | null;
  jornadaId: number | null;
  cargoId: number | null;
  justificativa: string | null;
  evidencia: string | null;
  autorizacao: string | null;
  confirmarImpacto: boolean | null;
  tokenPrevia: string | null;
  confirmarPrecedenciaUltimoDia: boolean | null;
  confirmarInclusaoExcedente: boolean | null;
}

export interface AlteracaoMatriculaVinculoRequisicao {
  dataEfetiva: CivilDate | null;
  novaMatricula: string | null;
  justificativa: string | null;
  evidencia: string | null;
  autorizacao: string | null;
}

export interface AlteracaoSenhaInicialAutenticacaoDTO {
  newPassword: string | null;
}

export interface AlteracaoSenhaPerfilDTO {
  currentPassword: string;
  newPassword: string;
  token: string | null;
}

export interface AtivacaoUsuarioAplicacaoDTO {
  message: string | null;
  username: string | null;
  temporaryPassword: string | null;
  forceChangeOnNextLogin: boolean;
}

export interface AtualizacaoPessoaOpcionalRequisicao {
  rg: string | null;
  limparRg: boolean | null;
  ctpsSerie: string | null;
  limparCtpsSerie: boolean | null;
  nitPisPasep: string | null;
  limparNitPisPasep: boolean | null;
  dataNascimento: CivilDate | null;
  limparDataNascimento: boolean | null;
  email: string | null;
  limparEmail: boolean | null;
  endereco: EnderecoDTO | null;
  limparEndereco: boolean | null;
}

export interface AtualizacaoUsuarioAplicacaoDTO {
  email: string | null;
  active: boolean | null;
  matricula: string | null;
  perfil: PerfilAcesso | null;
  password: string | null;
  lotacaoId: number | null;
  jornadaId: number | null;
  vinculosGestao: Array<AtualizacaoUsuarioAplicacaoDTOVinculoGestaoDTO> | null;
  superintendenciaId: number | null;
  gestaoLocalId: number | null;
}

export interface AtualizacaoUsuarioAplicacaoDTOVinculoGestaoDTO {
  lotacaoId: number | null;
  jornadaId: number | null;
}

export interface AutenticacaoGestaoLocalDTO {
  id: number | null;
  nome: string | null;
  codigo: string | null;
  superintendenciaId: number | null;
  uf: string | null;
}

export interface AutenticacaoSuperintendenciaDTO {
  id: number | null;
  nome: string | null;
  uf: string | null;
}

export interface AvisoRevisaoRelatorioDTO {
  sujeitoARevisao: boolean;
  quantidadePendencias: number;
  mensagem: string | null;
}

export interface BaixaAnexoDTO {
  resource: Blob | null;
  contentType: string | null;
  disposition: string | null;
  filename: string | null;
}

export interface BaixaDocumentoComprobatorioDTO {
  caminho: string | null;
  nomeOriginal: string | null;
  tipoMime: string | null;
  tamanhoBytes: number;
}

export interface BancoReferenciaDTO {
  id: number | null;
  nome: string | null;
  codigo: string | null;
  version: number;
}

export interface CacheDiagnosticoDTO {
  cacheManager: string | null;
  redisConfigurado: boolean;
  caches: Array<string> | null;
  conectado: boolean;
  ping: string | null;
  erro: string | null;
}

export interface CadastroAutenticacaoDTO {
  username: string;
  email: string;
  matricula: string;
  superintendenciaId: number;
  gestaoLocalId: number;
}

export interface CadastroDocumentoComprobatorioRequisicao {
  tipoFato: TipoFatoDocumental | null;
  fatoId: number | null;
  fatoChave: string | null;
  titulo: string | null;
  descricao: string | null;
  referenciaSei: string | null;
  classificacaoAcesso: ClassificacaoAcessoDocumento | null;
  dataEfetiva: CivilDate | null;
  chaveIdempotencia: string | null;
}

export interface CadastroPessoaRequisicao {
  nome: string | null;
  cpf: string | null;
  rg: string | null;
  ctpsSerie: string | null;
  nitPisPasep: string | null;
  dataNascimento: CivilDate | null;
  email: string | null;
  endereco: EnderecoDTO | null;
}

export interface CadastroVinculoContratualRequisicao {
  pessoaId: number | null;
  pessoa: CadastroPessoaRequisicao | null;
  contratoId: number | null;
  dataInicio: CivilDate | null;
  dataFim: CivilDate | null;
  matricula: string | null;
  composicaoId: number | null;
  confirmarInclusaoExcedente: boolean | null;
  justificativaExcedente: string | null;
  evidenciaExcedente: string | null;
  autorizacaoExcedente: string | null;
  lotacaoId: number | null;
  jornadaId: number | null;
  cargoId: number | null;
}

export interface CalendarioEventoDTO {
  id: number | null;
  data: CivilDate | null;
  tipo: string | null;
  descricao: string | null;
  abrangencia: string | null;
  uf: string | null;
  municipio: string | null;
  superintendencia: SuperintendenciaDTO | null;
  gestaoLocal: GestaoLocalDTO | null;
  lotacaoId: number | null;
  ativo: boolean | null;
  version: number;
  cancelado: boolean;
}

export interface CalendarioEventoRequisicao {
  data: CivilDate;
  tipo: string;
  descricao: string | null;
  abrangencia: string | null;
  uf: string | null;
  municipio: string | null;
  lotacaoId: number | null;
  superintendenciaId: number | null;
  gestaoLocalId: number | null;
  ativo: boolean | null;
  justificativa: string | null;
}

export interface ConferenciaDocumentalAtoDTO {
  versaoDeclaracao: string;
  declaracao: string;
  versaoIntencao: string;
  intencaoCanonica: string;
  hashIntencao: string;
  autorizacaoAdministrativaAto: string;
  usoPermissaoExcepcionalId: number;
  permissaoExcepcionalId: number;
}

export interface CancelamentoTransferenciaGestaoRequisicao {
  dataEfetiva: CivilDate | null;
  dataAto: CivilDate | null;
  motivo: string | null;
  referenciaSei: string | null;
  autorizacaoAdministrativa: string | null;
  conferenciaDocumentalConfirmada: boolean | null;
  versaoConferenciaDocumental: string | null;
}

export interface CargoDTO {
  id: number | null;
  nome: string;
  cbo: string | null;
  descricao: string | null;
  ativo: boolean | null;
  gestaoLocalId: number | null;
  gestaoLocalNome: string | null;
  superintendenciaId: number | null;
  superintendenciaNome: string | null;
  uf: string | null;
  version: number | null;
}

export interface ComparacaoEmissaoRelatorioDTO {
  emissaoHistorica: MetadadosEmissaoRelatorioDTO | null;
  revisaoVigente: AvisoRevisaoRelatorioDTO | null;
  hashHistorico: string | null;
  hashVigente: string | null;
  alterado: boolean;
  conteudoHistorico: JsonValue | null;
  conteudoVigente: JsonValue | null;
}

export interface ComposicaoContratualDTO {
  id: number | null;
  contratoId: number | null;
  lotacaoId: number | null;
  lotacaoNome: string | null;
  jornadaId: number | null;
  jornadaDescricao: string | null;
  cargoId: number | null;
  cargoNome: string | null;
  efetivoPrevisto: number | null;
  dataInicioVigencia: CivilDate | null;
  dataFimVigencia: CivilDate | null;
  permiteNovaAlocacao: boolean;
  version: number | null;
}

export interface ComposicaoContratualLeituraDTO extends ComposicaoContratualDTO {
  lotacaoMunicipio: string | null;
  jornadaSemanal: string | null;
  dataReferencia: CivilDate | null;
  conhecidoAte: Instant | null;
  provaCampos: Record<string, ProvaCampoHistoricoDTO> | null;
}

export interface ConcessaoAcessoDocumentoRequisicao {
  usuarioId: number | null;
  finalidade: string | null;
  justificativa: string | null;
  autorizacaoAdministrativa: string | null;
  validoAte: Instant | null;
}

export interface ConcessaoPermissaoExcepcionalRequisicao {
  tipo: TipoPermissaoExcepcional;
  usuarioBeneficiarioId: number;
  contratoId: number | null;
  gestaoLocalId: number | null;
  finalidade: string;
  justificativa: string;
  autorizacaoAdministrativa: string;
  validoAte: Instant;
  ocorrenciaId: number | null;
}

export interface ConfiguracaoPublicaSistemaDTO {
  EM_MANUTENCAO: boolean;
  PREVISAO_RETORNO: string | null;
  CLIENT_IP: string | null;
  HORAS_DIA_TRABALHO: number;
  HORA_INICIO_NOTURNO: string | null;
  MINUTOS_HORA_NOTURNA: number;
}

export interface ConfiguracaoSegundoFatorDTO {
  secret: string | null;
  otpauthUri: string | null;
  qrCodeDataUrl: string | null;
  periodSeconds: number;
  message: string | null;
}

export interface ConfiguracaoSistemaDTO {
  chave: string;
  valor: string;
  descricao: string | null;
}

export interface ConfirmacaoRecuperacaoSenhaDTO {
  token: string;
}

export interface ContextoComplementoFrequenciaDTO {
  dataReferencia: CivilDate | null;
  contratoId: number | null;
  vinculoId: number | null;
  alocacaoId: number | null;
  gestaoLocalId: number | null;
  superintendenciaId: number | null;
  lotacaoId: number | null;
  jornadaId: number | null;
}

export interface ContextoMotivoHistoricoDTO {
  descricao: string | null;
  categoria: CategoriaTerminoVinculo | null;
  comprovado: boolean;
  tipoFonte: string | null;
  fonteId: number | null;
  fonteRegistradaEm: Instant | null;
  inicioComprovado: CivilDate | null;
  fimComprovado: CivilDate | null;
}

export interface ContratoContagemDTO {
  contratoId: number | null;
  quantidadeColaboradores: number | null;
}

export interface ContratoDTO {
  id: number | null;
  numeroContrato: string | null;
  descricao: string | null;
  tipo: string | null;
  dataInicio: CivilDate | null;
  dataFim: CivilDate | null;
  ativo: boolean | null;
  diaSemanaReduzido: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY" | null;
  horasDiaReduzido: number | null;
  gestaoLocalId: number | null;
  gestaoLocalNome: string | null;
  superintendenciaId: number | null;
  superintendenciaNome: string | null;
  uf: string | null;
  efetivoPrevisto: number | null;
  efetivoAtual: number | null;
  saldoEfetivo: number | null;
  percentualOcupacao: number | null;
  anoContrato: number | null;
  tipoContratacaoId: number | null;
  tipoContratacaoDescricao: string | null;
  fornecedorId: number | null;
  cnpjEmpresa: string | null;
  razaoSocialEmpresa: string | null;
  nomeFantasiaEmpresa: string | null;
  estadoNegocial: EstadoNegocialContrato | null;
  situacaoTemporal: SituacaoTemporalContrato | null;
  habilitacaoAdministrativa: HabilitacaoAdministrativaContrato | null;
  efetivoOriginalPublicado: number | null;
  matriculaVinculoObrigatoria: boolean | null;
  publicadoEm: Instant | null;
  publicadoPorId: number | null;
  publicadoPorUsername: string | null;
  referenciaSeiPublicacao: string | null;
  version: number;
}

export interface ContratoFornecedorDTO {
  contratoId: number | null;
  fornecedor: FornecedorDTO | null;
}

export interface ContratoOpcaoDTO {
  id: number | null;
  numeroContrato: string | null;
  descricao: string | null;
  ativo: boolean | null;
  quantidadeColaboradores: number | null;
}

export interface CorrecaoCnpjContratoDTO {
  id: number | null;
  contratoId: number | null;
  eventoId: number | null;
  cnpjAnterior: string | null;
  cnpjCorrigido: string | null;
  dataEfetiva: CivilDate | null;
  dataAto: CivilDate | null;
  motivo: string | null;
  referenciaSei: string | null;
  chaveIdempotencia: string | null;
  responsavelId: number | null;
  responsavelUsername: string | null;
  registradoEm: Instant | null;
}

export interface CorrecaoCnpjContratoRequisicao {
  cnpjCorrigido: string;
  dataEfetiva: CivilDate;
  dataAto: CivilDate;
  motivo: string;
  referenciaSei: string;
  chaveIdempotencia: string;
  eventoPrecedenteId: number | null;
  relacaoPrecedencia: TipoRelacaoPrecedenciaNegocial | null;
  ordemDocumental: number | null;
}

export interface CorrecaoIdentidadePessoaRequisicao {
  tipo: TipoCorrecaoPessoa | null;
  novoValor: string | null;
  motivo: string | null;
  evidencia: string | null;
  autorizacao: string | null;
  impactosEncontrados: string | null;
}

export interface CorrecaoIdentificadorContratoDTO {
  id: number | null;
  contratoId: number | null;
  eventoId: number | null;
  permissaoId: number | null;
  numeroAnterior: string | null;
  anoAnterior: number | null;
  numeroNovo: string | null;
  anoNovo: number | null;
  dataEfetiva: CivilDate | null;
  dataAto: CivilDate | null;
  motivo: string | null;
  referenciaSei: string | null;
  autorizacaoAdministrativa: string | null;
  chaveIdempotencia: string | null;
  registradoEm: Instant | null;
  responsavelId: number | null;
  responsavel: string | null;
}

export interface CorrecaoIdentificadorContratoRequisicao {
  novoNumero: string;
  novoAno: number;
  dataEfetiva: CivilDate;
  dataAto: CivilDate;
  motivo: string;
  referenciaSei: string;
  autorizacaoAdministrativa: string;
  chaveIdempotencia: string;
  eventoPrecedenteId: number | null;
  relacaoPrecedencia: TipoRelacaoPrecedenciaNegocial | null;
  ordemDocumental: number | null;
}

export interface CorrecaoPessoaDTO {
  id: number | null;
  pessoaId: number | null;
  tipo: TipoCorrecaoPessoa | null;
  valorAnterior: string | null;
  valorNovo: string | null;
  motivo: string | null;
  evidencia: string | null;
  autorizacao: string | null;
  impactosEncontrados: string | null;
  responsavelId: number | null;
  responsavelUsername: string | null;
  registradoEm: Instant | null;
}

export interface CredenciaisLoginDTO {
  matricula: string | null;
  password: string | null;
  totp: string | null;
}

export interface CriacaoOcorrenciaDTO {
  empregadoId: number | null;
  motivo: string | null;
  dataOcorrencia: CivilDate | null;
  observacao: string | null;
  tipoRo: string | null;
  numeroSei: string | null;
  dias: number | null;
  classificacoesAnexos: Array<string> | null;
  chaveIdempotencia: string | null;
}

export interface CriacaoUsuarioAplicacaoDTO {
  username: string;
  email: string;
  matricula: string;
  password: string;
  perfil: PerfilAcesso;
  superintendenciaId: number | null;
  gestaoLocalId: number | null;
  lotacaoId: number | null;
  jornadaId: number | null;
  vinculosGestao: Array<AtualizacaoUsuarioAplicacaoDTOVinculoGestaoDTO> | null;
  active: boolean | null;
}

export interface DesativacaoSegundoFatorDTO {
  currentPassword: string | null;
  token: string | null;
}

export interface DesligamentoVinculoRequisicao {
  dataFim: CivilDate | null;
  motivoDesligamentoId: number | null;
  justificativa: string | null;
  evidencia: string | null;
  autorizacao: string | null;
  confirmarImpacto: boolean | null;
  tokenPrevia: string | null;
  confirmarPrecedenciaUltimoDia: boolean | null;
  confirmarInclusaoExcedente: boolean | null;
}

export interface DisponibilidadeCatalogoDTO {
  tipoCatalogo: TipoCatalogoTemporal | null;
  catalogoId: number | null;
  dataReferencia: CivilDate | null;
  conhecidoAte: Instant | null;
  ativo: boolean | null;
  disponibilidadeComprovada: boolean;
  eventoFonteId: number | null;
  dataEfetivaFonte: CivilDate | null;
  registradoEmFonte: Instant | null;
  ativoAnterior: boolean | null;
}

export interface DocumentoComprobatorioDTO {
  id: number | null;
  contratoId: number | null;
  tipoFato: TipoFatoDocumental | null;
  fatoId: number | null;
  fatoChave: string | null;
  grupoLogicoId: string | null;
  versaoDocumento: number | null;
  documentoAnteriorId: number | null;
  estado: EstadoDocumentoComprobatorio | null;
  classificacaoAcesso: ClassificacaoAcessoDocumento | null;
  tipoFonte: TipoFonteComprobatoria | null;
  titulo: string | null;
  descricao: string | null;
  referenciaSei: string | null;
  nomeOriginalArquivo: string | null;
  tipoMime: string | null;
  tamanhoBytes: number | null;
  sha256Arquivo: string | null;
  dataEfetiva: CivilDate | null;
  registradoEm: Instant | null;
  registradoPorId: number | null;
  registradoPorUsername: string | null;
  publicadoEm: Instant | null;
  publicadoPorId: number | null;
  publicadoPorUsername: string | null;
  versaoAtual: boolean;
  downloadDisponivel: boolean;
  version: number;
}

export interface EmissaoRelatorioRequisicao {
  tipo: TipoRelatorioAdministrativo;
  formato: FormatoEmissaoRelatorio;
  inicio: CivilDate;
  fim: CivilDate;
  contratoIds: Array<number> | null;
  lotacaoId: number | null;
  jornadaId: number | null;
  chaveIdempotencia: string;
}

export interface EmpregadoContratoDTO {
  id: number | null;
  numeroContrato: string | null;
  descricao: string | null;
  ativo: boolean | null;
}

export interface EmpregadoContratoListaDTO {
  empregadoId: number | null;
  contratoId: number | null;
  numeroContrato: string | null;
  descricao: string | null;
  ativo: boolean | null;
}

export interface EmpregadoDTO {
  id: number | null;
  nome: string | null;
  matricula: string | null;
  cpf: string | null;
  rg: string | null;
  ctpsSerie: string | null;
  nitPisPasep: string | null;
  email: string | null;
  dataNascimento: CivilDate | null;
  dataAdmissao: CivilDate | null;
  statusId: number | null;
  statusDescricao: string | null;
  lotacaoId: number | null;
  lotacaoNome: string | null;
  cargoId: number | null;
  cargoNome: string | null;
  jornadaId: number | null;
  jornadaDescricao: string | null;
  endereco: EnderecoDTO | null;
  contratos: Array<EmpregadoContratoDTO> | null;
  gestaoLocalId: number | null;
  gestaoLocalNome: string | null;
  superintendenciaId: number | null;
  superintendenciaNome: string | null;
  uf: string | null;
}

export interface EmpregadoFrequenciaDTO {
  id: number | null;
  nome: string | null;
  matricula: string | null;
  cpf: string | null;
  lotacaoNome: string | null;
  lotacaoId: number | null;
  turnoDescricao: string | null;
  turnoId: number | null;
  cargoNome: string | null;
}

export interface EmpregadoListaBaseDTO {
  id: number | null;
  nome: string | null;
  matricula: string | null;
  cpf: string | null;
  statusId: number | null;
  statusDescricao: string | null;
  lotacaoId: number | null;
  lotacaoNome: string | null;
  cargoId: number | null;
  cargoNome: string | null;
  jornadaId: number | null;
  jornadaDescricao: string | null;
}

export interface EmpregadoListaDTO {
  id: number | null;
  nome: string | null;
  matricula: string | null;
  cpf: string | null;
  statusId: number | null;
  statusDescricao: string | null;
  lotacaoId: number | null;
  lotacaoNome: string | null;
  cargoId: number | null;
  cargoNome: string | null;
  jornadaId: number | null;
  jornadaDescricao: string | null;
  contratos: Array<EmpregadoContratoDTO> | null;
}

export interface EmpregadoReferenciaDTO {
  id: number | null;
}

export interface EnderecoDTO {
  id: number | null;
  logradouro: string;
  numero: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
  cep: string | null;
  version: number;
}

export interface EventoAlocacaoVinculoDTO {
  id: number | null;
  vinculoId: number | null;
  alocacaoNovaId: number | null;
  tipoEvento: TipoEventoAlocacaoVinculo | null;
  relacaoPrecedencia: TipoRelacaoEventoAlocacao | null;
  eventoPrecedenteId: number | null;
  dataEfetiva: CivilDate | null;
  registradoEm: Instant | null;
  responsavelId: number | null;
  responsavelUsername: string | null;
  justificativa: string | null;
  evidencia: string | null;
  autorizacao: string | null;
  alocacaoAnteriorId: number | null;
  inicioAnterior: CivilDate | null;
  fimAnterior: CivilDate | null;
  estadoAnterior: EstadoNegocialAlocacao | null;
  composicaoAnteriorId: number | null;
  lotacaoAnteriorId: number | null;
  lotacaoAnteriorNome: string | null;
  jornadaAnteriorId: number | null;
  jornadaAnteriorDescricao: string | null;
  cargoAnteriorId: number | null;
  cargoAnteriorNome: string | null;
  inicioNovo: CivilDate | null;
  fimNovo: CivilDate | null;
  estadoNovo: EstadoNegocialAlocacao | null;
  composicaoNovaId: number | null;
  lotacaoNovaId: number | null;
  lotacaoNovaNome: string | null;
  jornadaNovaId: number | null;
  jornadaNovaDescricao: string | null;
  cargoNovoId: number | null;
  cargoNovoNome: string | null;
  transferenciaGestaoId: number | null;
  gestaoOrigemIdFotografia: number | null;
  gestaoOrigemNomeFotografia: string | null;
  gestaoDestinoIdFotografia: number | null;
  gestaoDestinoNomeFotografia: string | null;
  hashAnterior: string | null;
  hashEvento: string | null;
}

export interface EventoCicloVidaContratoDTO {
  id: number | null;
  tipoEvento: TipoEventoCicloVidaContrato | null;
  fatoLogicoId: string | null;
  chaveNegocial: string | null;
  versaoNegocial: number | null;
  ordemDocumental: number | null;
  situacaoFato: SituacaoFatoNegocial | null;
  dataEfetiva: CivilDate | null;
  dataAto: CivilDate | null;
  registradoEm: Instant | null;
  usuarioResponsavelId: number | null;
  usuarioResponsavelUsername: string | null;
  motivo: string | null;
  referenciaSei: string | null;
  valorAnterior: string | null;
  valorNovo: string | null;
  conferenciaDocumental: ConferenciaDocumentalAtoDTO | null;
}

export interface EventoDocumentoComprobatorioDTO {
  id: number | null;
  documentoId: number | null;
  grupoLogicoId: string | null;
  tipoEvento: TipoEventoDocumento | null;
  dataEfetiva: CivilDate | null;
  registradoEm: Instant | null;
  responsavelId: number | null;
  responsavelUsername: string | null;
  motivo: string | null;
  estadoAnterior: EstadoDocumentoComprobatorio | null;
  estadoNovo: EstadoDocumentoComprobatorio | null;
  referenciaSeiAnterior: string | null;
  referenciaSeiNova: string | null;
  sha256Anterior: string | null;
  sha256Novo: string | null;
  hashAnterior: string | null;
  hashEvento: string | null;
}

export interface EventoFrequenciaDTO {
  id: number | null;
  frequenciaId: number | null;
  numeroVersao: number | null;
  tipoEvento: TipoEventoFrequencia | null;
  dataReferencia: CivilDate | null;
  snapshotAnterior: string | null;
  snapshotNovo: string | null;
  motivo: string | null;
  usuarioId: number | null;
  usuarioUsername: string | null;
  registradoEm: Instant | null;
  hashAnterior: string | null;
  hashEvento: string | null;
  conteudoMinimizado: boolean;
}

export interface EventoVinculoContratualDTO {
  id: number | null;
  vinculoId: number | null;
  tipo: TipoEventoVinculoContratual | null;
  dataEfetiva: CivilDate | null;
  registradoEm: Instant | null;
  responsavelId: number | null;
  responsavelUsername: string | null;
  justificativa: string | null;
  evidencia: string | null;
  autorizacao: string | null;
  eventoPrecedenteId: number | null;
  relacaoPrecedencia: TipoRelacaoEventoVinculo | null;
  inicioAnterior: CivilDate | null;
  inicioNovo: CivilDate | null;
  fimAnterior: CivilDate | null;
  fimNovo: CivilDate | null;
  matriculaAnterior: string | null;
  matriculaNova: string | null;
  motivoDesligamentoIdFotografia: number | null;
  motivoDesligamentoDescricaoFotografia: string | null;
  motivoDesligamentoCategoriaFotografia: CategoriaTerminoVinculo | null;
  inclusaoExcedente: boolean;
  composicaoExcedenteId: number | null;
  lotacaoExcedenteId: number | null;
  jornadaExcedenteId: number | null;
  cargoExcedenteId: number | null;
  efetivoPrevistoExcedente: number | null;
  ocupacaoAposInclusaoExcedente: number | null;
  pessoaNomeFotografia: string | null;
  pessoaCpfFotografia: string | null;
  contratoNumeroFotografia: string | null;
  contratoAnoFotografia: number | null;
  gestaoLocalIdFotografia: number | null;
  gestaoLocalNomeFotografia: string | null;
  hashAnterior: string | null;
  hashEvento: string | null;
}

export interface EvidenciasIntegridadeDocumentoDTO {
  documentoId: number | null;
  grupoLogicoId: string | null;
  estado: string | null;
  cadeiaIntegra: boolean;
  eventos: Array<EvidenciasIntegridadeDocumentoDTOEvento> | null;
}

export interface EvidenciasIntegridadeDocumentoDTOEvento {
  eventoOriginal: EventoDocumentoComprobatorioDTO | null;
  hashArmazenado: string | null;
  hashCalculado: string | null;
  hashConfere: boolean;
  encadeamentoConfere: boolean;
}

export interface EvidenciasIntegridadeFrequenciaDTO {
  frequenciaId: number | null;
  estado: string | null;
  cadeiaIntegra: boolean;
  eventos: Array<EvidenciasIntegridadeFrequenciaDTOEvento> | null;
}

export interface EvidenciasIntegridadeFrequenciaDTOEvento {
  eventoOriginal: EventoFrequenciaDTO | null;
  hashArmazenado: string | null;
  hashCalculado: string | null;
  hashConfere: boolean;
  encadeamentoConfere: boolean;
}

export interface ExcedenteComposicaoDTO {
  composicaoId: number | null;
  contratoId: number | null;
  lotacaoId: number | null;
  lotacaoNome: string | null;
  jornadaId: number | null;
  jornadaDescricao: string | null;
  cargoId: number | null;
  cargoNome: string | null;
  efetivoPrevisto: number | null;
  quantidadeVinculadaAtual: number;
  quantidadeExcedente: number;
  desdeQuandoExcedente: CivilDate | null;
  excedente: boolean;
  justificativas: Array<JustificativaExcedenteDTO> | null;
}

export interface ExportacaoRelatorioStatusDTO {
  id: string | null;
  relatorio: string | null;
  formato: string | null;
  nomeArquivo: string | null;
  status: string | null;
  etapa: string | null;
  mensagem: string | null;
  processados: number;
  totalEstimado: number;
  percentual: number;
  segundosDecorridos: number;
  segundosRestantesEstimados: number | null;
  tamanhoBytes: number;
  concluido: boolean;
  falhou: boolean;
  podeBaixar: boolean;
  iniciadoEm: Instant | null;
  atualizadoEm: Instant | null;
}

export interface FechamentoFrequenciaDTO {
  id: number | null;
  status: string | null;
  dataReferencia: CivilDate | null;
  lotacaoId: number | null;
  lotacaoNome: string | null;
  jornadaId: number | null;
  jornadaDescricao: string | null;
  contratoIds: Array<number> | null;
  totalPrevisto: number;
  totalProcessado: number;
  totalInserido: number;
  totalAtualizado: number;
  totalPreservado: number;
  totalAlteracoes: number;
  somenteAlteracoes: boolean;
  percentual: number;
  tentativa: number;
  cancelamentoSolicitado: boolean;
  mensagem: string | null;
  criadoEm: LocalDateTime | null;
  iniciadoEm: LocalDateTime | null;
  atualizadoEm: LocalDateTime | null;
  concluidoEm: LocalDateTime | null;
}

export interface FornecedorDTO {
  id: number | null;
  razaoSocial: string | null;
  cnpj: string | null;
  email: string | null;
  ativo: boolean | null;
}

export interface FornecedorEntradaDTO {
  razaoSocial: string | null;
  cnpj: string | null;
  email: string | null;
  ativo: boolean | null;
}

export interface FragmentoProvaHistoricaComposicaoDTO {
  composicaoId: number | null;
  mapeamentoId: number | null;
  aditamentoComposicaoId: number | null;
  lotacaoNome: string | null;
  jornadaDescricao: string | null;
  cargoNome: string | null;
  lotacaoMunicipio: string | null;
  municipioComprovado: boolean;
  jornadaSemanal: string | null;
  jornadaComprovada: boolean;
  provaCampos: Record<string, ProvaCampoHistoricoDTO> | null;
}

export interface FrequenciaAdicionalDTO {
  id: number | null;
  empregadoId: number | null;
  empregadoNome: string | null;
  dataReferencia: CivilDate | null;
  tipo: string | null;
  minutos: number | null;
  quantidade: number | null;
  status: string | null;
  justificativa: string | null;
  frequenciaId: number | null;
  origem: string | null;
}

export interface FrequenciaExcecaoDTO {
  id: number | null;
  empregadoId: number | null;
  empregadoNome: string | null;
  dataInicio: CivilDate | null;
  dataFim: CivilDate | null;
  tipo: string | null;
  status: string | null;
  observacao: string | null;
  justificativa: string | null;
}

export interface GestaoLocalDTO {
  id: number | null;
  nome: string | null;
  codigo: string | null;
  ativo: boolean | null;
  superintendenciaId: number | null;
  superintendenciaNome: string | null;
  uf: string | null;
}

export interface HistoricoEmpregadoDTO {
  id: number | null;
  empregado: EmpregadoReferenciaDTO | null;
  dataEvento: LocalDateTime | null;
  descricao: string | null;
  usuario: UsuarioReferenciaDTO | null;
  hashIntegridade: string | null;
  version: number;
  dataEfetiva: CivilDate | null;
  versaoAssinatura: number | null;
  ocorrenciaId: number | null;
}

export interface HistoricoVinculoContratoDTO {
  id: number | null;
  empregadoId: number | null;
  empregadoNome: string | null;
  contratoId: number | null;
  numeroContrato: string | null;
  nomeContrato: string | null;
  descricaoContrato: string | null;
  tipoContrato: string | null;
  contratoDataInicio: CivilDate | null;
  contratoDataFim: CivilDate | null;
  contratoAtivo: boolean | null;
  efetivoPrevisto: number | null;
  efetivoAtual: number | null;
  saldoEfetivo: number | null;
  percentualOcupacao: number | null;
  diaSemanaReduzido: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY" | null;
  horasDiaReduzido: number | null;
  gestaoLocalId: number | null;
  gestaoLocalNome: string | null;
  superintendenciaId: number | null;
  superintendenciaNome: string | null;
  uf: string | null;
  dataInicio: Instant | null;
  dataFim: Instant | null;
  observacao: string | null;
  ativo: boolean;
  identidadeHistorica: IdentidadeHistoricaResolvidaDTO | null;
  vinculoCanonicoId: number | null;
  dataReferenciaIdentidade: CivilDate | null;
  conhecidoAteIdentidade: Instant | null;
}

export interface IdentidadeHistoricaResolvidaDTO {
  nome: string | null;
  comprovado: boolean;
  tipoFonte: string | null;
  fonteId: number | null;
  fonteRegistradaEm: Instant | null;
  inicioComprovado: CivilDate | null;
  fimComprovado: CivilDate | null;
}

export interface ImpactoComposicaoContratoDTO {
  composicaoId: number | null;
  lotacaoId: number | null;
  lotacaoNome: string | null;
  jornadaId: number | null;
  jornadaDescricao: string | null;
  cargoId: number | null;
  cargoNome: string | null;
  quantidadeAnterior: number | null;
  variacao: number | null;
  quantidadeResultante: number | null;
  colaboradoresAtuais: number | null;
  excedenteResultante: number | null;
  exigeTratamento: boolean;
}

export interface JornadaDTO {
  id: number | null;
  descricao: string | null;
  dias: Array<JornadaDiaSemanaDTO> | null;
  regrasAdicionais: Array<RegraAdicionalDTO> | null;
  gestaoLocalId: number | null;
  gestaoLocalNome: string | null;
  superintendenciaId: number | null;
  superintendenciaNome: string | null;
  uf: string | null;
  ativo: boolean | null;
  version: number | null;
}

export interface JornadaDiaSemanaDTO {
  id: number | null;
  diaSemana: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY" | null;
  diaTrabalho: boolean | null;
  entrada1: string | null;
  saida1: string | null;
  entrada2: string | null;
  saida2: string | null;
}

export interface JornadaResumoDTO {
  id: number | null;
  descricao: string | null;
}

export interface JustificativaExcedenteDTO {
  eventoId: number | null;
  vinculoId: number | null;
  dataEfetiva: CivilDate | null;
  registradoEm: Instant | null;
  responsavelUsername: string | null;
  justificativa: string | null;
  evidencia: string | null;
  autorizacao: string | null;
  efetivoPrevisto: number;
  ocupacaoAposInclusao: number;
  origemEvento: string | null;
}

export interface LotacaoDTO {
  id: number | null;
  nome: string | null;
  turnos: Array<JornadaResumoDTO> | null;
  gestaoLocalId: number | null;
  gestaoLocalNome: string | null;
  superintendenciaId: number | null;
  superintendenciaNome: string | null;
  uf: string | null;
  municipio: string | null;
  ativo: boolean | null;
  version: number | null;
}

export interface LotacaoOpcaoDTO {
  id: number | null;
  nome: string | null;
  turnos: Array<JornadaResumoDTO> | null;
}

export interface MapeamentoTransferenciaGestaoDTO {
  id: number | null;
  composicaoOrigemId: number | null;
  lotacaoOrigemId: number | null;
  lotacaoOrigemNome: string | null;
  jornadaOrigemId: number | null;
  jornadaOrigemDescricao: string | null;
  cargoOrigemId: number | null;
  cargoOrigemNome: string | null;
  lotacaoDestinoId: number | null;
  lotacaoDestinoNome: string | null;
  jornadaDestinoId: number | null;
  jornadaDestinoDescricao: string | null;
  cargoDestinoId: number | null;
  cargoDestinoNome: string | null;
}

export interface MapeamentoTransferenciaGestaoRequisicao {
  composicaoOrigemId: number | null;
  lotacaoOrigemId: number | null;
  jornadaOrigemId: number | null;
  cargoOrigemId: number | null;
  lotacaoDestinoId: number | null;
  jornadaDestinoId: number | null;
  cargoDestinoId: number | null;
}

export interface MensagemApiDTO {
  message: string | null;
  error: string | null;
}

export interface MetadadosEmissaoRelatorioDTO {
  id: number | null;
  tipo: TipoRelatorioAdministrativo | null;
  formato: FormatoEmissaoRelatorio | null;
  versao: number;
  natureza: NaturezaEmissaoRelatorio | null;
  estado: EstadoEmissaoRelatorio | null;
  situacao: string | null;
  inicio: CivilDate | null;
  fim: CivilDate | null;
  geradoEm: Instant | null;
  usuarioId: number | null;
  usuario: string | null;
  perfil: string | null;
  sujeitoARevisao: boolean;
  quantidadePendencias: number;
  aviso: string | null;
  hashConteudo: string | null;
  tamanhoConteudoBytes: number;
  emissaoAnteriorId: number | null;
  superadaEm: Instant | null;
  causaSuperacao: CausaSuperacaoRelatorio | null;
  motivoSuperacao: string | null;
  fonteConhecidaAte: Instant | null;
}

export interface MeuPerfilDTO {
  id: number | null;
  username: string | null;
  email: string | null;
  perfil: PerfilAcesso | null;
  autoridade: string | null;
  matricula: string | null;
  turno: string | null;
  jornadaId: number | null;
  jornada: ResumoUsuarioAplicacaoDTOReferencia | null;
  lotacao: ResumoUsuarioAplicacaoDTOReferencia | null;
  lotacaoNome: string | null;
  lotacaoId: number | null;
  vinculosGestao: Array<ResumoUsuarioAplicacaoDTOVinculoGestaoReferencia> | null;
  superintendencia: ResumoUsuarioAplicacaoDTOOrganizacaoReferencia | null;
  gestaoLocal: ResumoUsuarioAplicacaoDTOOrganizacaoReferencia | null;
  forceChangePassword: boolean;
  isRetroactiveUnlocked: boolean;
  retroactiveExpireAt: LocalDateTime | null;
  fotoUrl: string | null;
  createdAt: LocalDateTime | null;
  twoFactorEnabled: boolean;
}

export interface MotivoDesligamentoDTO {
  id: number | null;
  descricao: string;
  categoria: CategoriaTerminoVinculo;
  ativo: boolean | null;
  gestaoLocalId: number | null;
  gestaoLocalNome: string | null;
  superintendenciaId: number | null;
  superintendenciaNome: string | null;
  uf: string | null;
  criadoEm: Instant | null;
  criadoPorId: number | null;
  criadoPorUsername: string | null;
  atualizadoEm: Instant | null;
  version: number;
}

export interface OcorrenciaHistoricoDTO {
  ocorrencia: OcorrenciaResumoDTO | null;
  cancelada: boolean;
  canceladoEm: Instant | null;
  canceladoPorId: number | null;
  canceladoPorUsername: string | null;
  motivoCancelamento: string | null;
}

export interface OcorrenciaResumoDTO {
  id: number | null;
  empregado: OcorrenciaResumoDTOEmpregadoResumo | null;
  dataOcorrencia: CivilDate | null;
  motivo: string | null;
  tipoRo: string | null;
  numeroSei: string | null;
  observacao: string | null;
  quantidadeDias: number | null;
  diasSuspensao: number | null;
  diasAfastamento: number | null;
  anexos: Array<OcorrenciaResumoDTOAnexoResumo> | null;
}

export interface OcorrenciaResumoDTOEmpregadoResumo {
  id: number | null;
  nome: string | null;
}

export interface OcorrenciaResumoDTOAnexoResumo {
  id: number | null;
  nomeOriginal: string | null;
  tipoArquivo: string | null;
  classificacaoAcesso: string | null;
  sha256: string | null;
}

export interface OpcaoAlocacaoTemporalDTO {
  composicaoBaseId: number | null;
  lotacaoId: number | null;
  lotacaoNome: string | null;
  jornadaId: number | null;
  jornadaDescricao: string | null;
  cargoId: number | null;
  cargoNome: string | null;
  efetivoPrevisto: number;
  ocupacaoAtual: number;
  ocupacaoAposInclusao: number;
  excedenteAposInclusao: number;
  exigeJustificativaExcedente: boolean;
  disponivelParaNovaAlocacao: boolean;
}

export interface OrigemProvaCampoDTO {
  tipo: Tipo | null;
  provaId: number | null;
  eventoId: number | null;
  versaoId: number | null;
  composicaoId: number | null;
  aditamentoComposicaoId: number | null;
  mapeamentoId: number | null;
  dataEfetiva: CivilDate | null;
  registradoEm: Instant | null;
}

export interface PaginaCacheDTO<T> {
  content: Array<T> | null;
  totalElements: number;
}

export interface PaginaPendenciasDTO {
  registros: Array<PendenciaDTO> | null;
  totalElementos: number;
  totalPaginas: number;
  pagina: number;
  tamanho: number;
  primeira: boolean;
  ultima: boolean;
}

export interface PainelCalculoDTO {
  id: number | null;
  status: string | null;
  progresso: number;
  concluido: boolean;
  desatualizado: boolean;
  mensagem: string | null;
  geradoEm: LocalDateTime | null;
  dados: PainelGestaoDTO | null;
}

export interface PainelEfetivoDTO {
  inicio: CivilDate | null;
  fim: CivilDate | null;
  periodo: string | null;
  contratoId: number | null;
  lotacaoId: number | null;
  jornadaId: number | null;
  cargoId: number | null;
  resumoAtual: PainelEfetivoDTOResumo | null;
  resumoAnoAnterior: PainelEfetivoDTOResumo | null;
  serieAtual: Array<PainelEfetivoDTOPontoDiario> | null;
  serieAnoAnterior: Array<PainelEfetivoDTOPontoDiario> | null;
  detalhamentoContratos: Array<PainelEfetivoDTODetalheContrato> | null;
  detalhamentoLotacoes: Array<PainelEfetivoDTODetalheDimensao> | null;
  detalhamentoTurnos: Array<PainelEfetivoDTODetalheDimensao> | null;
  detalhamentoCargos: Array<PainelEfetivoDTODetalheDimensao> | null;
  quadroDimensionado: Array<PainelEfetivoDTOLinhaDimensionada> | null;
}

export interface PainelEfetivoDTOPontoDiario {
  data: CivilDate | null;
  efetivoPrevisto: number;
  efetivoAlocado: number;
  registrosLancados: number;
  presentes: number;
  ausentes: number;
  outrosRegistros: number;
  pendentesLancamento: number;
  deficitContratual: number;
  excedenteContratual: number;
  percentualCobertura: number;
  percentualAtendimento: number;
}

export interface PainelEfetivoDTOResumo {
  efetivoPrevisto: number;
  efetivoAlocado: number;
  registrosLancados: number;
  presentes: number;
  ausentes: number;
  outrosRegistros: number;
  pendentesLancamento: number;
  deficitContratual: number;
  excedenteContratual: number;
  percentualCobertura: number;
  percentualAtendimento: number;
}

export interface PainelEfetivoDTODetalheContrato {
  id: number | null;
  nome: string | null;
  efetivoPrevisto: number;
  efetivoAlocado: number;
  registrosLancados: number;
  presentes: number;
  ausentes: number;
  pendentesLancamento: number;
  deficitContratual: number;
  excedenteContratual: number;
  percentualCobertura: number;
  percentualAtendimento: number;
}

export interface PainelEfetivoDTODetalheDimensao {
  id: number | null;
  nome: string | null;
  efetivoAlocado: number;
  registrosLancados: number;
  presentes: number;
  ausentes: number;
  pendentesLancamento: number;
  percentualCobertura: number;
  percentualAtendimento: number;
  efetivoPrevisto: number;
  deficitContratual: number;
  excedenteContratual: number;
}

export interface PainelEfetivoDTOLinhaDimensionada {
  contratoId: number | null;
  contrato: string | null;
  lotacaoId: number | null;
  lotacao: string | null;
  jornadaId: number | null;
  turno: string | null;
  cargoId: number | null;
  cargo: string | null;
  efetivoAlocado: number;
  registrosLancados: number;
  presentes: number;
  ausentes: number;
  pendentesLancamento: number;
  percentualCobertura: number;
  percentualAtendimento: number;
  efetivoPrevisto: number;
  deficitContratual: number;
  excedenteContratual: number;
}

export interface PainelEfetivoDTOOpcao {
  id: number | null;
  nome: string | null;
}

export interface PainelEfetivoDTOFiltros {
  contratos: Array<PainelEfetivoDTOOpcao> | null;
  lotacoes: Array<PainelEfetivoDTOOpcao> | null;
  turnos: Array<PainelEfetivoDTOOpcao> | null;
  cargos: Array<PainelEfetivoDTOOpcao> | null;
  permiteDetalhamentoOperacional: boolean;
}

export interface PainelGestaoDTO {
  referencia: CivilDate | null;
  geradoEm: LocalDateTime | null;
  escopo: PainelGestaoDTOEscopo | null;
  efetivo: PainelGestaoDTOResumoEfetivo | null;
  quadro: PainelGestaoDTOQuadroPessoal | null;
  analiseCadastro: PainelGestaoDTOAnaliseCadastro | null;
  admissoesMensais: Array<PainelGestaoDTOAdmissaoMensal> | null;
  contratos: PainelGestaoDTOSaudeContratual | null;
  frequencia: PainelGestaoDTOCoberturaFrequencia | null;
  periodo: PainelGestaoDTOAnalisePeriodo | null;
  comparativoPeriodo: PainelGestaoDTOComparativoPeriodo | null;
  pressaoOperacional: PainelGestaoDTOPressaoOperacional | null;
  integridade: PainelGestaoDTOIntegridadeDados | null;
  horizonteContratual: PainelGestaoDTOHorizonteContratual | null;
  distribuicaoStatus: Array<PainelGestaoDTODistribuicaoStatus> | null;
  situacaoQuadro: Array<PainelGestaoDTOSituacaoQuadro> | null;
  padraoSemanal: Array<PainelGestaoDTOPadraoSemanal> | null;
  unidades: Array<PainelGestaoDTOUnidadeGestao> | null;
  evolucao: Array<PainelGestaoDTOSerieTemporal> | null;
  alertas: Array<PainelGestaoDTOAlertaGestao> | null;
  notasMetodologicas: Array<string> | null;
}

export interface PainelGestaoDTOEscopo {
  nivel: string | null;
  nome: string | null;
  superintendenciaId: number | null;
  gestaoLocalId: number | null;
  permiteDetalhamentoOperacional: boolean;
}

export interface PainelGestaoDTOResumoEfetivo {
  efetivoPrevisto: number;
  efetivoVinculado: number;
  empregadosAtivosCadastrados: number;
  saldoContratual: number;
  percentualOcupacao: number;
  ativosSemContrato: number;
  contratosVigentes: number;
}

export interface PainelGestaoDTOQuadroPessoal {
  empregadosCadastrados: number;
  ativos: number;
  inativosOuAfastados: number;
  lotacoes: number;
  cargos: number;
  jornadas: number;
  percentualAtivos: number;
}

export interface PainelGestaoDTOAnaliseCadastro {
  admitidosNoPeriodo: number;
  admitidosNoMesReferencia: number;
  admitidosUltimos12Meses: number;
  comDataAdmissao: number;
  semDataAdmissao: number;
  percentualComDataAdmissao: number;
}

export interface PainelGestaoDTOAdmissaoMensal {
  mes: CivilDate | null;
  quantidade: number;
}

export interface PainelGestaoDTOSaudeContratual {
  total: number;
  vigentes: number;
  vencidosMarcadosAtivos: number;
  vencendoEm30Dias: number;
  semDataFim: number;
  semEfetivoPrevisto: number;
  acimaDoEfetivoPrevisto: number;
  abaixoDoEfetivoPrevisto: number;
}

export interface PainelGestaoDTOCoberturaFrequencia {
  data: CivilDate | null;
  empregadosAtivos: number;
  empregadosEsperados: number;
  registrosEsperados: number;
  pendentes: number;
  percentualCobertura: number;
  presencas: number;
  ausencias: number;
  ausenciasJustificadas: number;
  faltasNaoJustificadas: number;
  repousosTrabalhados: number;
  trabalhosForaEscalaSemRt: number;
}

export interface PainelGestaoDTOAnalisePeriodo {
  inicio: CivilDate | null;
  fim: CivilDate | null;
  dias: number;
  registrosOperacionais: number;
  presencas: number;
  presencasParciais: number;
  atrasos: number;
  faltasNaoJustificadas: number;
  atestados: number;
  suspensoes: number;
  dsrPerdidos: number;
  repousosTrabalhados: number;
  empregadosComAusencia: number;
  empregadosComAusenciaRecorrente: number;
  taxaPresenca: number;
  taxaAbsenteismo: number;
  ultimaAtualizacao: LocalDateTime | null;
}

export interface PainelGestaoDTOComparativoPeriodo {
  inicioAnterior: CivilDate | null;
  fimAnterior: CivilDate | null;
  registrosOperacionaisAnterior: number;
  ausenciasAnterior: number;
  repousosTrabalhadosAnterior: number;
  taxaPresencaAnterior: number;
  taxaAbsenteismoAnterior: number;
  variacaoRegistrosOperacionais: number;
  variacaoAusencias: number;
  variacaoRepousosTrabalhados: number;
  variacaoTaxaPresencaPp: number;
  variacaoTaxaAbsenteismoPp: number;
}

export interface PainelGestaoDTOPressaoOperacional {
  repousosTrabalhados: number;
  empregadosComRt: number;
  ausenciasNoPeriodo: number;
  rtPorCemAtivos: number;
  relacaoRtAusencias: number;
}

export interface PainelGestaoDTOIntegridadeDados {
  totalInconsistencias: number;
  empregadosAtivosSemContrato: number;
  empregadosComMultiplosContratosVigentes: number;
  empregadoContratoGestaoLocalDivergente: number;
  empregadoLotacaoGestaoLocalDivergente: number;
  empregadoJornadaGestaoLocalDivergente: number;
  jornadasComCalendarioIncompleto: number;
  empregadosAtivosSemLotacao: number;
  empregadosAtivosSemJornada: number;
  adicionaisRtSemFrequencia: number;
  empregadosSemGestaoLocal: number;
  contratosSemGestaoLocal: number;
}

export interface PainelGestaoDTOHorizonteContratual {
  vencidos: number;
  ate30Dias: number;
  de31A90Dias: number;
  de91A180Dias: number;
  acimaDe180Dias: number;
  semDataFim: number;
}

export interface PainelGestaoDTODistribuicaoStatus {
  status: string | null;
  quantidade: number;
  percentual: number;
}

export interface PainelGestaoDTOSituacaoQuadro {
  status: string | null;
  quantidade: number;
  percentual: number;
}

export interface PainelGestaoDTOPadraoSemanal {
  ordem: number;
  dia: string | null;
  registrosOperacionais: number;
  ausencias: number;
  taxaAbsenteismo: number;
}

export interface PainelGestaoDTOUnidadeGestao {
  id: number | null;
  tipo: string | null;
  nome: string | null;
  uf: string | null;
  efetivoPrevisto: number | null;
  efetivoAtual: number;
  saldo: number | null;
  percentualOcupacao: number | null;
  contratosVigentes: number;
  alertasContratuais: number;
  ausenciasNaReferencia: number;
  empregadosEsperados: number;
  registrosEsperados: number;
  pendentesFrequencia: number;
  percentualCoberturaEscala: number;
}

export interface PainelGestaoDTOSerieTemporal {
  data: CivilDate | null;
  registros: number;
  presencas: number;
  ausencias: number;
  repousosTrabalhados: number;
}

export interface PainelGestaoDTOAlertaGestao {
  codigo: string | null;
  severidade: string | null;
  titulo: string | null;
  mensagem: string | null;
  rota: string | null;
}

export interface PainelRankingDTO {
  lotacaoId: number | null;
  nome: string | null;
  turnoId: number | null;
  turno: string | null;
  escalados: number;
  presencas: number;
  faltas: number;
  taxa: number;
}

export interface PendenciaDTO {
  id: number | null;
  tipo: TipoPendencia | null;
  estado: EstadoPendencia | null;
  motivo: string | null;
  surgidaEm: Instant | null;
  dataEfetiva: CivilDate | null;
  operacaoOrigem: string | null;
  perfilResponsavel: PerfilAcesso | null;
  escopoBloqueio: EscopoBloqueioPendencia | null;
  contratoId: number | null;
  contrato: string | null;
  pessoaId: number | null;
  pessoa: string | null;
  vinculoId: number | null;
  frequenciaId: number | null;
  gestaoLocalId: number | null;
  superintendenciaId: number | null;
  origemEntidade: string | null;
  origemId: number | null;
  iniciadaEm: Instant | null;
  iniciadaPor: string | null;
  resolvidaEm: Instant | null;
  resolvidaPor: string | null;
  observacaoTratamento: string | null;
  acaoSugerida: string | null;
}

export interface PermissaoDocumentoRestritoDTO {
  id: number | null;
  documentoReferenciaId: number | null;
  grupoLogicoId: string | null;
  usuarioBeneficiarioId: number | null;
  usuarioBeneficiarioUsername: string | null;
  finalidade: string | null;
  justificativa: string | null;
  autorizacaoAdministrativa: string | null;
  concedidoEm: Instant | null;
  concedidoPorId: number | null;
  concedidoPorUsername: string | null;
  validoAte: Instant | null;
  ativo: boolean | null;
  revogadoEm: Instant | null;
  revogadoPorId: number | null;
  revogadoPorUsername: string | null;
  motivoRevogacao: string | null;
  version: number;
}

export interface PermissaoExcepcionalDTO {
  id: number | null;
  tipo: TipoPermissaoExcepcional | null;
  usuarioBeneficiarioId: number | null;
  usuarioBeneficiario: string | null;
  perfilBeneficiario: PerfilAcesso | null;
  contratoId: number | null;
  contrato: string | null;
  gestaoLocalId: number | null;
  gestaoLocal: string | null;
  finalidade: string | null;
  justificativa: string | null;
  autorizacaoAdministrativa: string | null;
  concedidoEm: Instant | null;
  concedidoPorId: number | null;
  concedidoPor: string | null;
  validoAte: Instant | null;
  ativa: boolean;
  revogadoEm: Instant | null;
  revogadoPorId: number | null;
  revogadoPor: string | null;
  motivoRevogacao: string | null;
  version: number;
  ocorrenciaId: number | null;
}

export interface PessoaDTO {
  id: number | null;
  nome: string | null;
  cpf: string | null;
  rg: string | null;
  ctpsSerie: string | null;
  nitPisPasep: string | null;
  dataNascimento: CivilDate | null;
  email: string | null;
  endereco: EnderecoDTO | null;
  criadoEm: Instant | null;
  atualizadoEm: Instant | null;
  version: number;
}

export interface PessoaResumoDTO {
  id: number | null;
  nome: string | null;
  cpf: string | null;
  criadoEm: Instant | null;
  atualizadoEm: Instant | null;
  version: number;
}

export interface PosicaoAlocacaoTemporalDTO {
  vinculoId: number | null;
  dataReferencia: CivilDate | null;
  conhecidoAte: Instant | null;
  situacaoTemporal: SituacaoTemporalAlocacao | null;
  alocacaoId: number | null;
  composicaoBaseId: number | null;
  lotacaoId: number | null;
  lotacaoNome: string | null;
  jornadaId: number | null;
  jornadaDescricao: string | null;
  cargoId: number | null;
  cargoNome: string | null;
  dataInicio: CivilDate | null;
  dataFim: CivilDate | null;
  eventoConhecidoMaisRecenteId: number | null;
}

export interface PosicaoPessoaDTO {
  pessoaId: number | null;
  nomeAtual: string | null;
  cpfAtual: string | null;
  dataReferencia: CivilDate | null;
  conhecidoAte: Instant | null;
  vinculos: Array<PosicaoVinculoTemporalDTO> | null;
  nomeFotografia: string | null;
  eventoIdentidadeFonteId: number | null;
  fotografiaIdentidadeComprovada: boolean;
  identidadeHistorica: IdentidadeHistoricaResolvidaDTO | null;
}

export interface PosicaoTemporalContratoDTO {
  contratoId: number | null;
  dataReferencia: CivilDate | null;
  conhecidoAte: Instant | null;
  estadoNegocial: EstadoNegocialContrato | null;
  situacaoTemporal: SituacaoTemporalContrato | null;
  habilitacaoAdministrativa: HabilitacaoAdministrativaContrato | null;
  vigenciaInicio: CivilDate | null;
  vigenciaFim: CivilDate | null;
  versaoContratual: VersaoContratoNegocialDTO | null;
  eventosAplicados: Array<EventoCicloVidaContratoDTO> | null;
  relacoesConsideradas: Array<RelacaoPrecedenciaEventoContratoDTO> | null;
}

export interface PosicaoTemporalContratoLeituraDTO {
  contratoId: number | null;
  dataReferencia: CivilDate | null;
  conhecidoAte: Instant | null;
  estadoNegocial: EstadoNegocialContrato | null;
  situacaoTemporal: SituacaoTemporalContrato | null;
  habilitacaoAdministrativa: HabilitacaoAdministrativaContrato | null;
  vigenciaInicio: CivilDate | null;
  vigenciaFim: CivilDate | null;
  versaoContratual: VersaoContratoLeituraDTO | null;
  eventosAplicados: Array<EventoCicloVidaContratoDTO> | null;
  relacoesConsideradas: Array<RelacaoPrecedenciaEventoContratoDTO> | null;
  provaCampos: Record<string, ProvaCampoHistoricoDTO> | null;
}

export interface PosicaoVinculoTemporalDTO {
  vinculoId: number | null;
  pessoaId: number | null;
  contratoId: number | null;
  dataReferencia: CivilDate | null;
  conhecidoAte: Instant | null;
  dataInicio: CivilDate | null;
  dataFim: CivilDate | null;
  matricula: string | null;
  estadoNegocial: EstadoNegocialVinculo | null;
  situacaoTemporal: SituacaoTemporalVinculo | null;
  eventoConhecidoMaisRecenteId: number | null;
}

export interface PreviaAditamentoContratualDTO {
  contratoId: number | null;
  dataEfetiva: CivilDate | null;
  efetivoAnterior: number | null;
  efetivoResultante: number | null;
  efetivoOriginalPublicado: number | null;
  pisoNormalReducao: number | null;
  reducao: boolean;
  excedeLimiteNormal25: boolean;
  exigeAutorizacaoExcepcional: boolean;
  possuiImpactoOcupacao: boolean;
  impactosComposicao: Array<ImpactoComposicaoContratoDTO> | null;
  alertas: Array<string> | null;
  tokenPrevia: string | null;
}

export interface PreviaCicloVidaContratoDTO {
  contratoId: number | null;
  operacao: string | null;
  dataEfetiva: CivilDate | null;
  dataAto: CivilDate | null;
  motivo: string | null;
  usuarioResponsavelId: number | null;
  usuarioResponsavelUsername: string | null;
  autorizacaoNecessaria: string | null;
  quantidadeVinculosAfetados: number;
  vinculosAfetados: Array<PreviaCicloVidaContratoDTOVinculoAfetado> | null;
  programacoesFuturasAfetadas: Array<PreviaCicloVidaContratoDTOProgramacaoAfetada> | null;
  quantidadeFrequenciasAfetadas: number;
  datasComFrequenciasAfetadas: Array<CivilDate> | null;
  impactosConhecidos: Array<string> | null;
  exigeConfirmacao: boolean;
  tokenPrevia: string | null;
  limiteDetalhes: number;
  vinculosTruncados: boolean;
  quantidadeProgramacoesFuturasAfetadas: number;
  programacoesTruncadas: boolean;
  quantidadeDatasComFrequenciasAfetadas: number;
  datasComFrequenciasTruncadas: boolean;
}

export interface PreviaCicloVidaContratoDTOVinculoAfetado {
  vinculoId: number | null;
  pessoaId: number | null;
  pessoaNome: string | null;
  matricula: string | null;
  dataInicio: CivilDate | null;
  dataFim: CivilDate | null;
  programado: boolean;
}

export interface PreviaCicloVidaContratoDTOProgramacaoAfetada {
  tipo: string | null;
  id: number | null;
  vinculoId: number | null;
  dataEfetiva: CivilDate | null;
  descricao: string | null;
}

export interface PreviaImpactoAlocacaoDTO {
  token: string | null;
  vinculoId: number | null;
  alocacaoAfetadaId: number | null;
  dataEfetiva: CivilDate | null;
  fimFaixaAtual: CivilDate | null;
  impactos: Array<string> | null;
  incompatibilidades: Array<string> | null;
}

export interface PreviaImpactoVinculoDTO {
  token: string | null;
  vinculoId: number | null;
  valorAtual: CivilDate | null;
  valorProposto: CivilDate | null;
  impactos: Array<string> | null;
  vinculosConflitantes: Array<number> | null;
}

export interface PreviaProvaHistoricaContratoDTO {
  tokenPrevia: string | null;
  contratoId: number | null;
  versaoContrato: number | null;
  dataEfetiva: CivilDate | null;
  recuperaPublicacao: boolean;
  composicoesComprovadas: number;
  efeito: string | null;
}

export interface PreviaProvaHistoricaDTO {
  tokenPrevia: string | null;
  impactos: Array<string> | null;
}

export interface PreviaRemanejamentoContratualDTO {
  contratoId: number | null;
  dataEfetiva: CivilDate | null;
  efetivoTotalAntes: number | null;
  efetivoTotalDepois: number | null;
  somaVariacoes: number | null;
  totalPreservado: boolean;
  possuiImpactoOcupacao: boolean;
  impactosComposicao: Array<ImpactoComposicaoContratoDTO> | null;
  alertas: Array<string> | null;
  tokenPrevia: string | null;
}

export interface PreviaTransicaoCatalogoDTO {
  tipoCatalogo: TipoCatalogoTemporal | null;
  catalogoId: number | null;
  dataEfetiva: CivilDate | null;
  ativoAnterior: boolean | null;
  ativo: boolean;
  ativoAtualAposRegistro: boolean;
  versaoAtual: number;
  tokenPrevia: string | null;
  impactos: Array<string> | null;
}

export interface ProvaCampoHistoricoDTO {
  estado: Estado | null;
  visibilidade: Visibilidade | null;
  origens: Array<OrigemProvaCampoDTO> | null;
}

export interface ProvaHistoricaContratoDTO {
  id: number | null;
  contratoId: number | null;
  eventoOrigemId: number | null;
  provaAnteriorId: number | null;
  numeroRevisao: number | null;
  versaoAnteriorId: number | null;
  versaoGeradaId: number | null;
  dataEfetiva: CivilDate | null;
  registradoEm: Instant | null;
  hashConteudo: string | null;
  configuracaoCompletaComprovada: boolean;
  camposComprovados: Array<string> | null;
  fotografia: VersaoContratoLeituraDTO | null;
  composicoes: Array<FragmentoProvaHistoricaComposicaoDTO> | null;
  provaCampos: Record<string, ProvaCampoHistoricoDTO> | null;
}

export interface ProvaHistoricaContratoRequisicao {
  eventoOrigemId: number | null;
  versaoAnteriorId: number | null;
  provaAnteriorId: number | null;
  recuperarPublicacaoOriginal: boolean | null;
  dataDocumento: CivilDate | null;
  referenciaSei: string | null;
  documentoComprobatorioId: number | null;
  finalidadeAcessoDocumento: string | null;
  motivo: string | null;
  autorizacaoAdministrativa: string | null;
  versaoContratoEsperada: number | null;
  chaveIdempotencia: string | null;
  configuracaoCompletaComprovada: boolean | null;
  camposComprovados: Array<string> | null;
  fotografia: VersaoContratoNegocialDTO | null;
  composicoes: Array<ProvaHistoricaContratoRequisicaoItem> | null;
  tokenPrevia: string | null;
  confirmarImpacto: boolean | null;
}

export interface ProvaHistoricaContratoRequisicaoItem {
  composicaoId: number | null;
  mapeamentoId: number | null;
  aditamentoComposicaoId: number | null;
  lotacaoNome: string | null;
  jornadaDescricao: string | null;
  cargoNome: string | null;
  lotacaoMunicipio: string | null;
  municipioComprovado: boolean | null;
  jornadaSemanal: string | null;
  jornadaComprovada: boolean | null;
}

export interface PublicacaoContratoDTO {
  dataPublicacao: CivilDate | null;
  referenciaSei: string | null;
  justificativa: string | null;
  tokenPrevia: string | null;
  confirmarImpacto: boolean | null;
}

export interface RedefinicaoSenhaUsuarioAplicacaoDTO {
  newPassword: string;
}

export interface RegistroAuditoriaDTO {
  id: number | null;
  createdAt: Instant | null;
  requestId: string | null;
  userId: number | null;
  username: string | null;
  action: string | null;
  entityType: string | null;
  entityId: number | null;
  entityLabel: string | null;
  ip: string | null;
  userAgent: string | null;
  httpMethod: string | null;
  path: string | null;
  queryString: string | null;
  payloadJson: string | null;
  payloadTruncated: boolean | null;
  detailsJson: string | null;
}

export interface RegistroProvaMotivoHistoricoRequisicao {
  contratoId: number | null;
  descricaoComprovada: string | null;
  categoriaComprovada: CategoriaTerminoVinculo | null;
  versaoMotivoEsperada: number | null;
  versaoContratoEsperada: number | null;
  dataInicioEfetiva: CivilDate | null;
  dataFimEfetiva: CivilDate | null;
  referenciaSei: string | null;
  documentoComprobatorioId: number | null;
  finalidadeAcessoDocumento: string | null;
  motivo: string | null;
  autorizacao: string | null;
  provaAnteriorId: number | null;
  chaveIdempotencia: string | null;
  confirmarImpacto: boolean | null;
  tokenPrevia: string | null;
}

export interface RegistroProvaNomeHistoricoRequisicao {
  vinculoId: number | null;
  nomeComprovado: string | null;
  eventoOrigemRetificadoId: number | null;
  versaoPessoaEsperada: number | null;
  versaoVinculoEsperada: number | null;
  dataInicioEfetiva: CivilDate | null;
  dataFimEfetiva: CivilDate | null;
  referenciaSei: string | null;
  documentoComprobatorioId: number | null;
  finalidadeAcessoDocumento: string | null;
  motivo: string | null;
  autorizacao: string | null;
  provaAnteriorId: number | null;
  chaveIdempotencia: string | null;
  confirmarImpacto: boolean | null;
  tokenPrevia: string | null;
}

export interface RegraAdicionalDTO {
  id: number | null;
  tipo: string | null;
  inicio: string | null;
  fim: string | null;
  percentual: number | null;
  ativo: boolean | null;
  ordem: number | null;
}

export interface RelacaoPrecedenciaEventoContratoDTO {
  id: number | null;
  eventoFonteId: number | null;
  eventoAlvoId: number | null;
  tipoRelacao: TipoRelacaoPrecedenciaNegocial | null;
  registradoEm: Instant | null;
  usuarioResponsavelId: number | null;
  usuarioResponsavelUsername: string | null;
  motivo: string | null;
}

export interface RelatorioAbsenteismoDTO {
  empregadoId: number | null;
  nome: string | null;
  cargo: string | null;
  lotacao: string | null;
  turno: string | null;
  faltas: number;
  atestados: number;
  dsrPerdido: number;
  spells: number;
  taxaIndividual: number;
  disrupcao: number;
  diasTrabalhaveis: number;
  minutosPrevistos: number;
}

export interface RelatorioAbsenteismoPaginaDTO {
  registros: Array<RelatorioAbsenteismoDTO> | null;
  totalColaboradores: number;
  pagina: number;
  tamanho: number;
  totalPaginas: number;
  primeira: boolean;
  ultima: boolean;
  revisao: AvisoRevisaoRelatorioDTO | null;
}

export interface RelatorioAbsenteismoResumoDTO {
  faltas: number;
  atestados: number;
  dsrPerdido: number;
  totalColaboradores: number;
  diasTrabalhaveis: number;
  alertasDisrupcao: number;
  taxaGeral: number;
  maioresTaxas: Array<RelatorioAbsenteismoDTO> | null;
  revisao: AvisoRevisaoRelatorioDTO | null;
}

export interface RelatorioAdministrativoDTO {
  metadados: MetadadosEmissaoRelatorioDTO | null;
  conteudo: JsonValue | null;
}

export interface RelatorioFiltrosDTO {
  contratos: Array<ContratoOpcaoDTO> | null;
  lotacoes: Array<LotacaoOpcaoDTO> | null;
  turnos: Array<JornadaResumoDTO> | null;
}

export interface RelatorioFrequenciaConsolidadoDTO {
  completo: boolean;
  calculoVersao: string | null;
  inicio: CivilDate | null;
  fim: CivilDate | null;
  contratos: Array<number> | null;
  registros: Array<RelatorioFrequenciaConsolidadoDTOLinha> | null;
  totais: RelatorioFrequenciaConsolidadoDTOTotais | null;
  revisao: AvisoRevisaoRelatorioDTO | null;
}

export interface RelatorioFrequenciaConsolidadoDTOLinha {
  chaveAlocacao: string | null;
  empregadoId: number | null;
  nome: string | null;
  contratoId: number | null;
  contrato: string | null;
  lotacaoId: number | null;
  lotacao: string | null;
  jornadaId: number | null;
  turno: string | null;
  cargoId: number | null;
  cargo: string | null;
  diario: Record<string, RelatorioFrequenciaConsolidadoDTODia> | null;
  totais: RelatorioFrequenciaConsolidadoDTOTotais | null;
}

export interface RelatorioFrequenciaConsolidadoDTODia {
  data: CivilDate | null;
  diaTrabalho: boolean;
  minutosPrevistos: number;
  lancado: boolean;
  status: string | null;
  entrada1: string | null;
  saida1: string | null;
  entrada2: string | null;
  saida2: string | null;
  minutosFisicos: number;
  minutosNoturnosReais: number;
  minutosNoturnosReduzidos: number;
  minutosAdicionais: number;
  minutosPagaveis: number;
  calculoVersao: string | null;
  ocorrenciaSobreposta: boolean;
}

export interface RelatorioFrequenciaConsolidadoDTOTotais {
  diasTrabalhaveis: number;
  diasLancados: number;
  presencas: number;
  presencasParciais: number;
  faltas: number;
  atestados: number;
  descansos: number;
  semLancamento: number;
  minutosPrevistos: number;
  minutosFisicos: number;
  minutosNoturnosReais: number;
  minutosNoturnosReduzidos: number;
  minutosAdicionais: number;
  minutosPagaveis: number;
}

export interface RelatorioFrequenciaPaginaDTO {
  completo: boolean;
  calculoVersao: string | null;
  inicio: CivilDate | null;
  fim: CivilDate | null;
  contratos: Array<number> | null;
  registros: Array<RelatorioFrequenciaConsolidadoDTOLinha> | null;
  totalColaboradores: number;
  pagina: number;
  tamanho: number;
  totalPaginas: number;
  primeira: boolean;
  ultima: boolean;
  revisao: AvisoRevisaoRelatorioDTO | null;
}

export interface RelatorioHorasDTO {
  empregadoId: number | null;
  nome: string | null;
  cargo: string | null;
  lotacao: string | null;
  turno: string | null;
  totalPresencas: number;
  totalPresencasParciais: number;
  totalHorasTrabalhadas: string | null;
  totalHorasNoturnas: number | null;
}

export interface RelatorioTerceirizadosDTO {
  referencia: CivilDate | null;
  geradoEm: LocalDateTime | null;
  escopo: string | null;
  indicadores: RelatorioTerceirizadosDTOIndicadores | null;
  distribuicaoStatus: Array<RelatorioTerceirizadosDTODistribuicao> | null;
  distribuicaoContratos: Array<RelatorioTerceirizadosDTODistribuicaoContrato> | null;
  statusDisponiveis: Array<RelatorioTerceirizadosDTOOpcaoTexto> | null;
  lotacoes: Array<RelatorioTerceirizadosDTOOpcao> | null;
  contratos: Array<RelatorioTerceirizadosDTOOpcao> | null;
  empregados: RelatorioTerceirizadosDTOPaginaEmpregados | null;
}

export interface RelatorioTerceirizadosDTOIndicadores {
  empregados: number;
  ativos: number;
  semContratoVigente: number;
  admitidosNoMes: number;
  contratosVigentes: number;
  efetivoPrevisto: number;
  efetivoVinculado: number;
  saldoContratual: number;
}

export interface RelatorioTerceirizadosDTODistribuicao {
  nome: string | null;
  quantidade: number;
}

export interface RelatorioTerceirizadosDTODistribuicaoContrato {
  id: number | null;
  nome: string | null;
  quantidade: number;
  semContrato: boolean;
}

export interface RelatorioTerceirizadosDTOOpcao {
  id: number | null;
  nome: string | null;
}

export interface RelatorioTerceirizadosDTOOpcaoTexto {
  valor: string | null;
  nome: string | null;
}

export interface RelatorioTerceirizadosDTOContratoResumo {
  id: number | null;
  numeroContrato: string | null;
  descricao: string | null;
  vigente: boolean;
}

export interface RelatorioTerceirizadosDTOEmpregadoResumo {
  id: number | null;
  nome: string | null;
  matricula: string | null;
  cpf: string | null;
  statusDescricao: string | null;
  lotacaoId: number | null;
  lotacaoNome: string | null;
  cargoNome: string | null;
  jornadaDescricao: string | null;
  dataAdmissao: CivilDate | null;
  contratos: Array<RelatorioTerceirizadosDTOContratoResumo> | null;
}

export interface RelatorioTerceirizadosDTOPaginaEmpregados {
  content: Array<RelatorioTerceirizadosDTOEmpregadoResumo> | null;
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface RemanejamentoComposicaoRequisicao {
  composicaoId: number | null;
  lotacaoId: number | null;
  jornadaId: number | null;
  cargoId: number | null;
  variacao: number | null;
}

export interface RemanejamentoContratualDTO {
  id: number | null;
  contratoId: number | null;
  eventoId: number | null;
  identificacao: string | null;
  versaoInstrumento: number | null;
  dataEfetiva: CivilDate | null;
  justificativa: string | null;
  referenciaSei: string | null;
  registradoEm: Instant | null;
  usuarioResponsavelId: number | null;
  usuarioResponsavelUsername: string | null;
  situacao: SituacaoFatoNegocial | null;
  somaVariacoes: number | null;
  itens: Array<ImpactoComposicaoContratoDTO> | null;
}

export interface RemanejamentoContratualRequisicao {
  identificacao: string | null;
  dataEfetiva: CivilDate | null;
  justificativa: string | null;
  referenciaSei: string | null;
  confirmarImpacto: boolean | null;
  itens: Array<RemanejamentoComposicaoRequisicao> | null;
  eventoPrecedenteId: number | null;
  relacaoPrecedencia: TipoRelacaoPrecedenciaNegocial | null;
  ordemDocumental: number | null;
  tokenPrevia: string | null;
}

export interface RenovacaoSessaoDTO {
  password: string | null;
}

export interface RequisicaoCriacaoFechamentoFrequencia {
  dataReferencia: CivilDate;
  lotacaoId: number;
  jornadaId: number;
  contratoIds: Array<number> | null;
  alteracoes: Array<FrequenciaDTO> | null;
  justificativaRetificacao: string | null;
  chaveIdempotencia: string | null;
}

export interface RespostaCacheConsultaHttp {
  status: number;
  contentType: string | null;
  body: number[] | null;
}

export interface ResultadoAlteracaoSenhaDTO {
  message: string | null;
  reauthenticationRequired: boolean;
}

export interface ResultadoEnvioAvatarDTO {
  fotoUrl: string | null;
  message: string | null;
}

export interface ResultadoRedefinicaoSenhaDTO {
  message: string | null;
  forceChangeOnNextLogin: boolean;
}

export interface ResultadoSegundoFatorDTO {
  enabled: boolean;
  message: string | null;
}

export interface ResultadoValidacaoSenhaDTO {
  valid: boolean;
  message: string | null;
}

export interface ResumoPendenciasDTO {
  abertas: number;
  emTratamento: number;
  resolvidas: number;
  retificacoesFrequencia: number;
  finsNaturaisVigencia: number;
}

export interface ResumoUsuarioAplicacaoDTO {
  id: number | null;
  username: string | null;
  email: string | null;
  matricula: string | null;
  perfil: PerfilAcesso | null;
  autoridade: string | null;
  active: boolean | null;
  forceChangePassword: boolean | null;
  locked: boolean | null;
  lockoutEnd: string | null;
  lotacao: ResumoUsuarioAplicacaoDTOReferencia | null;
  jornada: ResumoUsuarioAplicacaoDTOReferencia | null;
  vinculosGestao: Array<ResumoUsuarioAplicacaoDTOVinculoGestaoReferencia> | null;
  superintendencia: ResumoUsuarioAplicacaoDTOOrganizacaoReferencia | null;
  gestaoLocal: ResumoUsuarioAplicacaoDTOOrganizacaoReferencia | null;
  retroactiveUnlocked: boolean | null;
  retroactiveUnlockExpireAt: string | null;
  fotoUrl: string | null;
  createdAt: string | null;
}

export interface ResumoUsuarioAplicacaoDTOReferencia {
  id: number | null;
  label: string | null;
}

export interface ResumoUsuarioAplicacaoDTOOrganizacaoReferencia {
  id: number | null;
  label: string | null;
  codigo: string | null;
  uf: string | null;
}

export interface ResumoUsuarioAplicacaoDTOVinculoGestaoReferencia {
  id: number | null;
  lotacao: ResumoUsuarioAplicacaoDTOReferencia | null;
  jornada: ResumoUsuarioAplicacaoDTOReferencia | null;
}

export interface RetificacaoInicioVinculoRequisicao {
  novoInicio: CivilDate | null;
  justificativa: string | null;
  evidencia: string | null;
  autorizacao: string | null;
  confirmarImpacto: boolean | null;
  tokenPrevia: string | null;
  confirmarInclusaoExcedente: boolean | null;
}

export interface RetificacaoSeiDocumentoRequisicao {
  novaReferenciaSei: string | null;
  motivo: string | null;
  chaveIdempotencia: string | null;
}

export interface RevogacaoPermissaoExcepcionalRequisicao {
  motivo: string;
}

export interface SenhaAtualDTO {
  currentPassword: string | null;
}

export interface SolicitacaoRecuperacaoSenhaDTO {
  email: string;
}

export interface StatusEmpregadoDTO {
  id: number | null;
  descricao: string;
  version: number;
}

export interface StatusSegundoFatorDTO {
  enabled: boolean;
  setupPending: boolean;
}

export interface SubstituicaoDocumentoComprobatorioRequisicao {
  titulo: string | null;
  descricao: string | null;
  referenciaSei: string | null;
  motivo: string | null;
  chaveIdempotencia: string | null;
}

export interface SuperintendenciaDTO {
  id: number | null;
  nome: string | null;
  uf: string | null;
  ativo: boolean | null;
}

export interface TipoContratacaoDTO {
  id: number | null;
  descricao: string | null;
  ativo: boolean | null;
  matriculaVinculoObrigatoria: boolean | null;
  gestaoLocalId: number | null;
  gestaoLocalNome: string | null;
  superintendenciaId: number | null;
  superintendenciaNome: string | null;
  uf: string | null;
  criadoEm: Instant | null;
  criadoPorId: number | null;
  criadoPorUsername: string | null;
  atualizadoEm: Instant | null;
  version: number;
}

export interface TokenCsrfDTO {
  headerName: string | null;
  parameterName: string | null;
  token: string | null;
}

export interface TokenTotpDTO {
  token: string | null;
}

export interface TransferenciaGestaoContratoDTO {
  id: number | null;
  contratoId: number | null;
  eventoContratoId: number | null;
  gestaoOrigemId: number | null;
  gestaoOrigemNome: string | null;
  gestaoDestinoId: number | null;
  gestaoDestinoNome: string | null;
  dataEfetiva: CivilDate | null;
  motivo: string | null;
  referenciaSei: string | null;
  autorizacaoAdministrativa: string | null;
  registradoEm: Instant | null;
  responsavelId: number | null;
  responsavelUsername: string | null;
  mapeamentos: Array<MapeamentoTransferenciaGestaoDTO> | null;
  tipoContratacaoDestinoId: number | null;
  tipoContratacaoDestinoDescricao: string | null;
  conferenciaDocumental: ConferenciaDocumentalAtoDTO | null;
}

export interface TransferenciaGestaoContratoRequisicao {
  dataEfetiva: CivilDate | null;
  dataAto: CivilDate | null;
  gestaoLocalDestinoId: number | null;
  motivo: string | null;
  referenciaSei: string | null;
  autorizacaoAdministrativa: string | null;
  mapeamentos: Array<MapeamentoTransferenciaGestaoRequisicao> | null;
  eventoPrecedenteId: number | null;
  relacaoPrecedencia: TipoRelacaoPrecedenciaNegocial | null;
  ordemDocumental: number | null;
  tipoContratacaoDestinoId: number | null;
  tokenPrevia: string | null;
  confirmarImpacto: boolean | null;
  conferenciaDocumentalConfirmada: boolean | null;
  versaoConferenciaDocumental: string | null;
}

export interface TransicaoCatalogoDTO {
  eventoId: number | null;
  tipoCatalogo: TipoCatalogoTemporal | null;
  catalogoId: number | null;
  dataEfetiva: CivilDate | null;
  ativo: boolean;
  registradoEm: Instant | null;
  ativoAtualAposRegistro: boolean | null;
  versaoAtual: number;
  reenvio: boolean;
}

export interface TransicaoDisponibilidadeCatalogoRequisicao {
  dataEfetiva: CivilDate | null;
  ativo: boolean | null;
  motivo: string | null;
  evidencia: string | null;
  autorizacao: string | null;
  versaoEsperada: number | null;
  chaveIdempotencia: string | null;
  confirmarImpacto: boolean | null;
  tokenPrevia: string | null;
}

export interface TratamentoPendenciaRequisicao {
  observacao: string | null;
}

export interface UsoPermissaoExcepcionalDTO {
  id: number | null;
  permissaoId: number | null;
  tipo: TipoPermissaoExcepcional | null;
  usuarioId: number | null;
  usuario: string | null;
  tipoRecurso: string | null;
  recursoId: number | null;
  finalidade: string | null;
  usadoEm: Instant | null;
}

export interface UsuarioReferenciaDTO {
  id: number | null;
  username: string | null;
}

export interface ValidacaoSenhaDTO {
  password: string | null;
}

export interface ValidacaoTokenRecuperacaoSenhaDTO {
  valid: boolean;
  message: string | null;
}

export interface VersaoCalendarioEventoDTO {
  id: number | null;
  calendarioEventoId: number | null;
  numeroVersao: number;
  tipoOperacao: string | null;
  dataEfetiva: CivilDate | null;
  registradoEm: Instant | null;
  usuarioId: number | null;
  usuarioUsername: string | null;
  justificativa: string | null;
  motivoTecnico: string | null;
  versaoAnteriorId: number | null;
  fotografia: CalendarioEventoDTO | null;
  hashAnterior: string | null;
  hashVersao: string | null;
}

export interface VersaoComplementoFrequenciaDTO {
  id: number | null;
  tipoComplemento: string | null;
  complementoId: number | null;
  numeroVersao: number | null;
  tipoOperacao: string | null;
  dataEfetiva: CivilDate | null;
  registradoEm: Instant | null;
  usuarioId: number | null;
  usuarioUsername: string | null;
  justificativa: string | null;
  motivoTecnico: string | null;
  versaoAnteriorId: number | null;
  fotografia: JsonValue | null;
  contextos: Array<ContextoComplementoFrequenciaDTO> | null;
  hashAnterior: string | null;
  hashVersao: string | null;
}

export interface VersaoContratoLeituraDTO extends VersaoContratoNegocialDTO {
  provaCampos: Record<string, ProvaCampoHistoricoDTO> | null;
}

export interface VersaoContratoNegocialDTO {
  id: number | null;
  numeroVersao: number | null;
  eventoOrigemId: number | null;
  dataEfetiva: CivilDate | null;
  registradoEm: Instant | null;
  numeroContrato: string | null;
  anoContrato: number | null;
  descricao: string | null;
  tipoContratacaoId: number | null;
  tipoContratacaoDescricao: string | null;
  fornecedorId: number | null;
  cnpjEmpresa: string | null;
  razaoSocialEmpresa: string | null;
  nomeFantasiaEmpresa: string | null;
  dataInicio: CivilDate | null;
  dataFim: CivilDate | null;
  efetivoPrevisto: number | null;
  gestaoLocalId: number | null;
  gestaoLocalNome: string | null;
  matriculaVinculoObrigatoria: boolean | null;
  matriculaTipoContratacaoObrigatoria: boolean | null;
  matriculaVinculoObrigatoriaEfetiva: boolean | null;
  diaSemanaReduzido: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY" | null;
  horasDiaReduzido: number | null;
  habilitacaoAdministrativa: HabilitacaoAdministrativaContrato | null;
  tipoContratacaoComprovado: boolean | null;
}

/** Selection for frequency in the server-authorized D/K and allocation scope. Not the historical registry DTO. */
export interface VinculoElegivelFrequenciaDTO {
  id: number | null;
  pessoaId: number | null;
  pessoaNome: string | null;
  pessoaCpf: string | null;
  contratoId: number | null;
  contratoNumero: string | null;
  contratoAno: number | null;
  matricula: string | null;
  dataReferencia: CivilDate | null;
  conhecidoAte: Instant | null;
  gestaoLocalId: number | null;
  alocacaoId: number | null;
  lotacaoId: number | null;
  jornadaId: number | null;
  cargoId: number | null;
}

export interface VinculoContratualDTO {
  id: number | null;
  pessoaId: number | null;
  pessoaNome: string | null;
  pessoaCpf: string | null;
  contratoId: number | null;
  contratoNumero: string | null;
  contratoAno: number | null;
  gestaoLocalId: number | null;
  gestaoLocalNome: string | null;
  dataInicio: CivilDate | null;
  dataFim: CivilDate | null;
  matricula: string | null;
  estadoNegocial: EstadoNegocialVinculo | null;
  situacaoTemporal: SituacaoTemporalVinculo | null;
  motivoDesligamentoId: number | null;
  motivoDesligamentoDescricao: string | null;
  desligamentoRegistradoEm: Instant | null;
  composicaoInicialId: number | null;
  lotacaoInicialId: number | null;
  jornadaInicialId: number | null;
  cargoInicialId: number | null;
  version: number;
}

export interface AtividadePainelDTO {
  id: number | null;
  dataEvento: LocalDateTime | null;
  descricao: string | null;
  empregadoId: number | null;
  empregadoNome: string | null;
  usuarioId: number | null;
  usuarioUsername: string | null;
}

export interface EstatisticasPainelDTO {
  empregados: number;
  ativos: number;
  inativos: number;
  cargos: number;
  lotacoes: number;
}

export interface FrequenciaDTO {
  id: number | null;
  empregado: FrequenciaDTOEmpregadoResumoDTO | null;
  dataReferencia: CivilDate | null;
  mes: number | null;
  ano: number | null;
  status: string | null;
  entrada1: string | null;
  saida1: string | null;
  entrada2: string | null;
  saida2: string | null;
  horasTrabalhadas: string | null;
  horarioSaida: string | null;
  horasNoturnas: number | null;
  observacao: string | null;
  prazoEdicao: LocalDateTime | null;
  updatedAt: LocalDateTime | null;
  pessoaId: number | null;
  vinculoContratualId: number | null;
  alocacaoVinculoId: number | null;
  contratoId: number | null;
  lotacaoId: number | null;
  jornadaId: number | null;
  cargoId: number | null;
  gestaoLocalId: number | null;
  escopoHistoricoEfetivo: ContextoComplementoFrequenciaDTO | null;
  estadoNegocial: EstadoNegocialFrequencia | null;
  validadeOperacional: ValidadeOperacionalFrequencia | null;
  estadoAuditoria: EstadoAuditoriaFrequencia | null;
  bloqueadaEdicaoOperacional: boolean | null;
  versaoNegocial: number | null;
  justificativaRetificacao: string | null;
  chaveIdempotencia: string | null;
}

export interface FrequenciaDTOEmpregadoResumoDTO {
  id: number | null;
  nome: string | null;
}

export type CategoriaTerminoVinculo = "DESLIGAMENTO_INDIVIDUAL" | "FIM_VIGENCIA_CONTRATO" | "ENCERRAMENTO_ANTECIPADO_CONTRATO" | "ANULACAO_CONTRATO" | "OUTRO";
export type CausaSuperacaoRelatorio = "NOVA_EMISSAO" | "RETIFICACAO_FONTE";
export type ClassificacaoAcessoDocumento = "NEGOCIAL" | "PESSOAL_RESTRITO";
export type EscopoBloqueioPendencia = "FREQUENCIA_PERIODO" | "OPERACAO_VINCULO" | "CONTRATO_TEMPORAL";
export type Estado = "VALOR_COMPROVADO" | "AUSENCIA_COMPROVADA" | "NAO_COMPROVADO";
export type EstadoAuditoriaFrequencia = "ORIGINAL" | "ALTERADA_OPERACIONAL" | "RETIFICADA_GESTOR" | "SUJEITA_REVISAO";
export type EstadoDocumentoComprobatorio = "RASCUNHO" | "PUBLICADO" | "SUBSTITUIDO" | "INVALIDO";
export type EstadoEmissaoRelatorio = "VIGENTE" | "SUPERADA";
export type EstadoNegocialAlocacao = "REGISTRADA" | "SUBSTITUIDA" | "CANCELADA";
export type EstadoNegocialContrato = "RASCUNHO" | "PUBLICADO" | "CANCELADO" | "ANULADO";
export type EstadoNegocialFrequencia = "ABERTA" | "FECHADA";
export type EstadoNegocialVinculo = "REGISTRADO" | "CANCELADO" | "ANULADO";
export type EstadoPendencia = "ABERTA" | "EM_TRATAMENTO" | "RESOLVIDA";
export type FormatoEmissaoRelatorio = "VISUALIZACAO" | "XLSX" | "PDF";
export type HabilitacaoAdministrativaContrato = "ATIVO" | "INATIVO";
export type NaturezaEmissaoRelatorio = "ORIGINAL" | "RETIFICADA";
export type PerfilAcesso = "ADMINISTRADOR" | "GESTOR_NACIONAL" | "GESTOR_ESTADUAL" | "GESTOR_LOCAL" | "PRESIDENTE" | "OPERACIONAL";
export type SituacaoFatoNegocial = "VIGENTE" | "RETIFICADO" | "CANCELADO" | "SUBSTITUIDO";
export type SituacaoTemporalAlocacao = "PROGRAMADA" | "VIGENTE" | "ENCERRADA" | "CANCELADA" | "SEM_ALOCACAO";
export type SituacaoTemporalContrato = "RASCUNHO" | "FUTURO" | "VIGENTE" | "ENCERRADO" | "CANCELADO" | "ANULADO";
export type SituacaoTemporalVinculo = "PROGRAMADO" | "VIGENTE" | "ENCERRADO" | "CANCELADO" | "ANULADO";
export type Tipo = "FOTOGRAFIA_CONTRATUAL" | "PROVA_HISTORICA" | "EVENTO_CONTRATUAL" | "COMPOSICAO_PUBLICADA" | "ITEM_ADITAMENTO" | "MAPEAMENTO_TRANSFERENCIA";
export type TipoAcessoDocumento = "CONSULTA_METADADOS" | "DOWNLOAD" | "CONSULTA_HISTORICO" | "CONSULTA_PERMISSOES";
export type TipoCatalogoTemporal = "LOTACAO" | "JORNADA" | "CARGO" | "TIPO_CONTRATACAO" | "MOTIVO_DESLIGAMENTO";
export type TipoCorrecaoPessoa = "CPF" | "NOME";
export type TipoEventoAlocacaoVinculo = "CRIACAO" | "ALTERACAO" | "RETIFICACAO" | "CANCELAMENTO_PROGRAMACAO" | "AJUSTE_LIMITE_VINCULO" | "TRANSFERENCIA_GESTAO" | "REPROCESSAMENTO_TRANSFERENCIA";
export type TipoEventoCicloVidaContrato = "PUBLICACAO" | "INATIVACAO_ADMINISTRATIVA" | "REATIVACAO_ADMINISTRATIVA" | "ENCERRAMENTO_ANTECIPADO" | "CANCELAMENTO_CONTRATO_FUTURO" | "ANULACAO" | "CORRECAO_ANULACAO" | "CORRECAO_IDENTIFICADOR" | "CORRECAO_CNPJ" | "TRANSFERENCIA_GESTAO_LOCAL" | "CANCELAMENTO_TRANSFERENCIA_GESTAO" | "ADITAMENTO" | "CANCELAMENTO_ADITAMENTO" | "REMANEJAMENTO" | "CANCELAMENTO_REMANEJAMENTO";
export type TipoEventoDocumento = "CRIACAO_RASCUNHO" | "PUBLICACAO" | "ANEXO_ACRESCENTADO" | "RETIFICACAO_SEI" | "SUBSTITUICAO" | "INVALIDACAO" | "CONCESSAO_ACESSO" | "REVOGACAO_ACESSO";
export type TipoEventoFrequencia = "REVISAO_SOLICITADA" | "REVISAO_DECIDIDA" | "REGISTRO" | "ALTERACAO_OPERACIONAL" | "RETIFICACAO_GESTOR" | "FECHAMENTO_AUTOMATICO" | "IMPACTO_TEMPORAL" | "REVALIDACAO_GERENCIAL";
export type TipoEventoVinculoContratual = "CRIACAO" | "RETIFICACAO_INICIO" | "ALTERACAO_MATRICULA" | "CANCELAMENTO_PROGRAMACAO" | "DESLIGAMENTO" | "RETIFICACAO_DESLIGAMENTO" | "CANCELAMENTO_DESLIGAMENTO_PROGRAMADO" | "FIM_NATURAL_CONTRATO" | "ENCERRAMENTO_ANTECIPADO_CONTRATO" | "ANULACAO_CONTRATO";
export type TipoFatoDocumental = "CONTRATO_ORIGINAL" | "ADITAMENTO" | "REMANEJAMENTO" | "TRANSFERENCIA_GESTAO" | "CORRECAO_IDENTIFICADOR" | "CORRECAO_CNPJ" | "ENCERRAMENTO_ANTECIPADO" | "ANULACAO" | "VINCULO" | "ALOCACAO" | "OUTRO";
export type TipoFonteComprobatoria = "ANEXO" | "SEI" | "ANEXO_E_SEI";
export type TipoPendencia = "RETIFICACAO_FREQUENCIA" | "FIM_NATURAL_VIGENCIA" | "CONFLITO_REPLAY_TEMPORAL";
export type TipoPermissaoExcepcional = "CORRECAO_IDENTIFICADOR_CONTRATO" | "REDUCAO_CONTRATUAL_EXCEPCIONAL" | "TRANSFERENCIA_GESTAO_LOCAL" | "INCORPORACAO_PROVA_HISTORICA" | "ACESSO_DADOS_PESSOAIS_RESTRITOS" | "INTERVENCAO_NEGOCIAL_TECNICA";
export type TipoRelacaoEventoAlocacao = "ORIGEM" | "SUBSTITUI" | "RETIFICA" | "CANCELA" | "AJUSTA";
export type TipoRelacaoEventoVinculo = "ORIGEM" | "RETIFICA" | "CANCELA" | "SUBSTITUI";
export type TipoRelacaoPrecedenciaNegocial = "SUCEDE" | "RETIFICA" | "CANCELA" | "SUBSTITUI" | "MANTEM_COEXISTE";
export type TipoRelatorioAdministrativo = "FREQUENCIA_CONSOLIDADA" | "ABSENTEISMO";
export type ValidadeOperacionalFrequencia = "VALIDA" | "SUJEITA_REVISAO" | "INVALIDA_COBERTURA";
export type Visibilidade = "DISPONIVEL" | "RESTRITA";
