import type * as DTO from './java';
export type HistoricalVersions = Partial<Record<'versaoContratoEsperada' | 'versaoEsperada' | 'versaoPessoaEsperada' | 'versaoVinculoEsperada' | 'versaoMotivoEsperada', number>>;
export type HistoricalKind = 'contrato' | 'catalogo' | 'nome' | 'motivo';
export interface HistoricalTarget {
 id?: string | number | null; data?: string | null; conhecidoAte?: string | null;
 tipoCatalogo?: DTO.TipoCatalogoTemporal | string; vinculoId?: string | number | null; contratoId?: string | number | null;
}
export interface HistoricalWeekDay { dia: string; trabalha: string | boolean | null; entrada1: string; saida1: string; entrada2: string; saida2: string }
export interface HistoricalCompositionForm {
 origem: string; composicaoId: string | number | null; mapeamentoId: string | number | null; aditamentoComposicaoId: string | number | null;
 lotacaoNome: string; jornadaDescricao: string; cargoNome: string; lotacaoMunicipio: string; municipioComprovado: string | boolean | null;
 municipioEstado: string; jornadaComprovada: string | boolean | null; semana: HistoricalWeekDay[];
}
export interface HistoricalSourceInput {
 referenciaSei?: string | null; documentoComprobatorioId?: string | number | null; finalidadeAcessoDocumento?: string | null;
 motivo?: string | null; autorizacao?: string | null; autorizacaoAdministrativa?: string | null; provaAnteriorId?: string | number | null;
}
export interface HistoricalProofInput extends HistoricalSourceInput {
 dataInicioEfetiva?: string | null; dataFimEfetiva?: string | null; vinculoId?: string | number | null; contratoId?: string | number | null;
 nomeComprovado?: string | null; eventoOrigemRetificadoId?: string | number | null; versaoPessoaEsperada?: string | number | null;
 versaoVinculoEsperada?: string | number | null; descricaoComprovada?: string | null; categoriaComprovada?: string | null;
 versaoMotivoEsperada?: string | number | null; versaoContratoEsperada?: string | number | null;
 configuracaoCompletaComprovada?: string | boolean | null; camposComprovados?: string[];
 fotografia?: Record<string, string | number | boolean | null | undefined>; composicoes?: HistoricalCompositionForm[];
 dataDocumento?: string | null; eventoOrigemId?: string | number | null; versaoAnteriorId?: string | number | null;
 recuperarPublicacaoOriginal?: string | boolean | null; dataEfetiva?: string | null; ativo?: string | boolean | null;
 versaoEsperada?: string | number | null; evidencia?: string | null;
}
export type HistoricalPreview = DTO.PreviaProvaHistoricaContratoDTO | DTO.PreviaProvaHistoricaDTO | DTO.PreviaTransicaoCatalogoDTO;
export type HistoricalCommand = import('./java-requests').RequestProvaHistoricaContratoRequisicao | import('./java-requests').RequestRegistroProvaNomeHistoricoRequisicao | import('./java-requests').RequestRegistroProvaMotivoHistoricoRequisicao | import('./java-requests').RequestTransicaoDisponibilidadeCatalogoRequisicao;
/** Map<String,Object> emitted by ProvaNomePessoaHistoricoServico.dto / RegistroProvaMotivoHistoricoServico.dto. */
export interface PersonalProofReceiptBase {
 id: number | null; contratoId: number | null; dataInicioEfetiva: string | null; dataFimEfetiva: string | null;
 registradoEm: string | null; responsavelId: number | null; responsavelUsername: string | null; provaAnteriorId: number | null;
 relacao: 'ORIGEM' | 'RETIFICA'; motivo: string | null; autorizacao: string | null; referenciaSei: string | null;
 documentoComprobatorioId: number | null; reenvio: boolean;
}
export interface NameProofReceipt extends PersonalProofReceiptBase { pessoaId: number | null; vinculoId: number | null; nomeComprovado: string | null; eventoOrigemRetificadoId: number | null }
export interface ReasonProofReceipt extends PersonalProofReceiptBase { motivoDesligamentoId: number | null; descricaoComprovada: string | null; categoriaComprovada: DTO.CategoriaTerminoVinculo | null }
export type HistoricalReceipt = DTO.ProvaHistoricaContratoDTO | DTO.TransicaoCatalogoDTO | NameProofReceipt | ReasonProofReceipt;
