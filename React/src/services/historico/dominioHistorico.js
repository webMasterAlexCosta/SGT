import { serializarInstant, serializarLocalDate } from '../../core/http/datasHttp.js';

export const TIPOS_CATALOGO = Object.freeze([
  ['LOTACAO', 'Lotação'], ['JORNADA', 'Jornada'], ['CARGO', 'Cargo'],
  ['TIPO_CONTRATACAO', 'Tipo de contratação'], ['MOTIVO_DESLIGAMENTO', 'Motivo de término'],
]);
export const CATEGORIAS_MOTIVO = Object.freeze([
  ['DESLIGAMENTO_INDIVIDUAL', 'Desligamento individual'], ['FIM_VIGENCIA_CONTRATO', 'Fim de vigência do contrato'],
  ['ENCERRAMENTO_ANTECIPADO_CONTRATO', 'Encerramento antecipado do contrato'],
  ['ANULACAO_CONTRATO', 'Anulação do contrato'], ['OUTRO', 'Outro'],
]);
export const DIAS_SEMANA = Object.freeze([
  ['MONDAY', 'Segunda-feira'], ['TUESDAY', 'Terça-feira'], ['WEDNESDAY', 'Quarta-feira'],
  ['THURSDAY', 'Quinta-feira'], ['FRIDAY', 'Sexta-feira'], ['SATURDAY', 'Sábado'], ['SUNDAY', 'Domingo'],
]);

// Lista fechada de ProvaHistoricaContratoServico.CAMPOS. Metadados da fotografia
// (ID, revisão, registro e evento) são produzidos exclusivamente pelo Java.
export const CAMPOS_FOTOGRAFIA = Object.freeze([
  { nome: 'dataEfetiva', rotulo: 'Data efetiva do ato documentado', tipo: 'date' },
  { nome: 'numeroContrato', rotulo: 'Número original do contrato', tipo: 'text', limite: 80 },
  { nome: 'anoContrato', rotulo: 'Ano original do contrato', tipo: 'integer', min: 1900, max: 9999 },
  { nome: 'descricao', rotulo: 'Descrição documentada do contrato', tipo: 'text', limite: 500 },
  { nome: 'tipoContratacaoId', rotulo: 'ID do tipo de contratação documentado', tipo: 'id' },
  { nome: 'tipoContratacaoDescricao', rotulo: 'Descrição histórica do tipo', tipo: 'text', limite: 160 },
  { nome: 'fornecedorId', rotulo: 'ID canônico do fornecedor', tipo: 'id' },
  { nome: 'cnpjEmpresa', rotulo: 'CNPJ documentado (14 dígitos)', tipo: 'text', limite: 14 },
  { nome: 'razaoSocialEmpresa', rotulo: 'Razão social documentada', tipo: 'text', limite: 180 },
  { nome: 'nomeFantasiaEmpresa', rotulo: 'Nome fantasia documentado', tipo: 'text', limite: 180 },
  { nome: 'dataInicio', rotulo: 'Início de vigência documentado', tipo: 'date' },
  { nome: 'dataFim', rotulo: 'Fim de vigência documentado', tipo: 'date' },
  { nome: 'efetivoPrevisto', rotulo: 'Efetivo original previsto', tipo: 'integer', min: 0 },
  { nome: 'gestaoLocalId', rotulo: 'ID da gestão local documentada', tipo: 'id' },
  { nome: 'gestaoLocalNome', rotulo: 'Nome histórico da gestão local', tipo: 'text', limite: 180 },
  { nome: 'matriculaVinculoObrigatoria', rotulo: 'Matrícula exigida pelo contrato', tipo: 'boolean' },
  { nome: 'matriculaTipoContratacaoObrigatoria', rotulo: 'Matrícula exigida pelo tipo', tipo: 'boolean' },
  { nome: 'matriculaVinculoObrigatoriaEfetiva', rotulo: 'Exigência efetiva de matrícula', tipo: 'boolean' },
  { nome: 'diaSemanaReduzido', rotulo: 'Dia de jornada reduzida', tipo: 'dia' },
  { nome: 'horasDiaReduzido', rotulo: 'Horas no dia reduzido', tipo: 'integer', min: 1, max: 24 },
  { nome: 'habilitacaoAdministrativa', rotulo: 'Habilitação administrativa documentada', tipo: 'habilitacao' },
]);

const vazio = (valor) => valor == null || (typeof valor === 'string' && valor.trim() === '');
const texto = (valor, rotulo, limite = 1000, obrigatorio = false) => {
  const limpo = vazio(valor) ? null : String(valor).trim();
  if (obrigatorio && !limpo) throw new TypeError(`${rotulo} é obrigatório.`);
  if (limpo && limpo.length > limite) throw new TypeError(`${rotulo} excede ${limite} caracteres.`);
  return limpo;
};
const inteiro = (valor, rotulo, min = 0, max = Number.MAX_SAFE_INTEGER, opcional = false) => {
  if (vazio(valor)) {
    if (opcional) return null;
    throw new TypeError(`${rotulo} é obrigatório.`);
  }
  if (!/^\d+$/.test(String(valor).trim())) throw new TypeError(`${rotulo} deve ser um número inteiro válido.`);
  const numero = Number(valor);
  if (!Number.isSafeInteger(numero) || numero < min || numero > max) throw new TypeError(`${rotulo} está fora do intervalo permitido.`);
  return numero;
};
export const idHistorico = (valor, rotulo = 'ID do registro', opcional = false) => inteiro(valor, rotulo, 1, Number.MAX_SAFE_INTEGER, opcional);
const versao = (valor, rotulo) => inteiro(valor, rotulo, 0);
export const booleanoHistorico = (valor) => {
  if (vazio(valor)) return null;
  if (valor === true || valor === 'true') return true;
  if (valor === false || valor === 'false') return false;
  throw new TypeError('Selecione um estado documentado válido.');
};
const data = (valor, rotulo, opcional = false) => {
  if (opcional && vazio(valor)) return null;
  try { return serializarLocalDate(valor); }
  catch { throw new TypeError(`${rotulo} deve ser uma data válida (AAAA-MM-DD).`); }
};
const enumHistorico = (valor, opcoes, rotulo, opcional = false) => {
  if (opcional && vazio(valor)) return null;
  if (!opcoes.includes(valor)) throw new TypeError(`Selecione ${rotulo} válido.`);
  return valor;
};
export const tipoCatalogoHistorico = (valor) => enumHistorico(valor, TIPOS_CATALOGO.map(([chave]) => chave), 'um tipo de catálogo');

export const prepararConsultaHistorica = (formulario = {}) => ({
  id: idHistorico(formulario.id),
  data: data(formulario.data, 'Data de referência'),
  ...(vazio(formulario.conhecidoAte) ? {} : { conhecidoAte: serializarInstant(formulario.conhecidoAte) }),
});

const intervalo = (formulario) => {
  const inicio = data(formulario.dataInicioEfetiva, 'Início do intervalo');
  const fim = data(formulario.dataFimEfetiva, 'Fim do intervalo', true);
  if (inicio < '1000-01-01' || (fim && fim < inicio)) throw new TypeError('Informe um intervalo válido; o fim deve ser igual ou posterior ao início.');
  return { dataInicioEfetiva: inicio, dataFimEfetiva: fim };
};
const fonte = (formulario, contrato = false) => {
  const referenciaSei = texto(formulario.referenciaSei, 'Referência SEI', contrato ? 200 : 1000);
  const documentoComprobatorioId = idHistorico(formulario.documentoComprobatorioId, 'ID do documento comprobatório', true);
  if (!referenciaSei && documentoComprobatorioId == null) throw new TypeError('Informe uma referência SEI ou um documento comprobatório.');
  return {
    referenciaSei, documentoComprobatorioId,
    finalidadeAcessoDocumento: texto(formulario.finalidadeAcessoDocumento, 'Finalidade de acesso ao documento'),
    motivo: texto(formulario.motivo, 'Motivo / justificativa', contrato ? 1500 : 1000, true),
    [contrato ? 'autorizacaoAdministrativa' : 'autorizacao']: texto(
      formulario[contrato ? 'autorizacaoAdministrativa' : 'autorizacao'], 'Autorização', contrato ? 180 : 1000, true),
    provaAnteriorId: idHistorico(formulario.provaAnteriorId, 'ID da prova anterior', true),
  };
};

export const prepararProvaNome = (formulario = {}) => ({
  vinculoId: idHistorico(formulario.vinculoId, 'ID do vínculo'),
  nomeComprovado: texto(formulario.nomeComprovado, 'Nome comprovado', 180, true).replace(/\s+/g, ' '),
  eventoOrigemRetificadoId: idHistorico(formulario.eventoOrigemRetificadoId, 'ID do evento nominal retificado', true),
  versaoPessoaEsperada: versao(formulario.versaoPessoaEsperada, 'Versão esperada da pessoa'),
  versaoVinculoEsperada: versao(formulario.versaoVinculoEsperada, 'Versão esperada do vínculo'),
  ...intervalo(formulario), ...fonte(formulario),
});
export const prepararProvaMotivo = (formulario = {}) => ({
  contratoId: idHistorico(formulario.contratoId, 'ID do contrato'),
  descricaoComprovada: texto(formulario.descricaoComprovada, 'Descrição comprovada', 160, true),
  categoriaComprovada: enumHistorico(formulario.categoriaComprovada, CATEGORIAS_MOTIVO.map(([chave]) => chave), 'uma categoria'),
  versaoMotivoEsperada: versao(formulario.versaoMotivoEsperada, 'Versão esperada do motivo'),
  versaoContratoEsperada: versao(formulario.versaoContratoEsperada, 'Versão esperada do contrato'),
  ...intervalo(formulario), ...fonte(formulario),
});
export const prepararTransicaoCatalogo = (formulario = {}) => {
  const dataEfetiva = data(formulario.dataEfetiva, 'Data efetiva da transição');
  if (dataEfetiva < '1000-01-01' || dataEfetiva > serializarLocalDate(new Date())) throw new TypeError('A data efetiva da transição deve estar entre 1000-01-01 e hoje.');
  const ativo = booleanoHistorico(formulario.ativo);
  if (ativo == null) throw new TypeError('Selecione a disponibilidade documentada.');
  return {
    dataEfetiva, ativo, versaoEsperada: versao(formulario.versaoEsperada, 'Versão esperada do catálogo'),
    motivo: texto(formulario.motivo, 'Motivo / justificativa', 500, true),
    evidencia: texto(formulario.evidencia, 'Evidência da transição', 1000, true),
    autorizacao: texto(formulario.autorizacao, 'Autorização', 1000, true),
  };
};

const horario = (valor) => {
  if (vazio(valor)) return '';
  if (!/^(?:[01]\d|2[0-3]):[0-5]\d$/.test(valor)) throw new TypeError('Os horários da grade devem usar HH:mm.');
  return valor;
};
export const prepararGradeSemanal = (semana) => {
  if (!Array.isArray(semana) || semana.length !== 7 || new Set(semana.map((dia) => dia.dia)).size !== 7) throw new TypeError('A grade comprovada deve declarar os sete dias da semana.');
  return DIAS_SEMANA.map(([chave, rotulo]) => {
    const item = semana.find((dia) => dia.dia === chave);
    if (!item) throw new TypeError('A grade comprovada deve declarar os sete dias da semana.');
    const trabalha = booleanoHistorico(item.trabalha);
    if (trabalha == null) throw new TypeError(`Declare se há trabalho em ${rotulo}.`);
    const horas = ['entrada1', 'saida1', 'entrada2', 'saida2'].map((campo) => horario(item[campo]));
    if (Boolean(horas[0]) !== Boolean(horas[1]) || Boolean(horas[2]) !== Boolean(horas[3])) throw new TypeError(`Informe entrada e saída em conjunto em ${rotulo}.`);
    const haDuracao = (horas[0] && horas[0] !== horas[1]) || (horas[2] && horas[2] !== horas[3]);
    if (trabalha && !haDuracao) throw new TypeError(`Informe horários com duração positiva em ${rotulo}.`);
    if (!trabalha && horas.some(Boolean)) throw new TypeError(`O dia ${rotulo} não trabalhado deve permanecer sem horários.`);
    return [chave, String(trabalha), ...horas].join('|');
  }).join(';');
};
const prepararComposicao = (item) => {
  const ids = {
    composicaoId: idHistorico(item.composicaoId, 'ID da composição', true),
    mapeamentoId: idHistorico(item.mapeamentoId, 'ID do mapeamento', true),
    aditamentoComposicaoId: idHistorico(item.aditamentoComposicaoId, 'ID do item de aditamento', true),
  };
  if (Object.values(ids).filter((id) => id != null).length !== 1) throw new TypeError('Cada configuração exige uma única origem canônica.');
  const municipioComprovado = booleanoHistorico(item.municipioComprovado);
  const jornadaComprovada = booleanoHistorico(item.jornadaComprovada);
  const lotacaoMunicipio = texto(item.lotacaoMunicipio, 'Município documentado', 180);
  if (item.municipioEstado === 'true' && !lotacaoMunicipio) throw new TypeError('Informe o nome do município comprovado ou selecione explicitamente a ausência de município.');
  if (lotacaoMunicipio && municipioComprovado !== true) throw new TypeError('Declare a comprovação do município informado.');
  return {
    ...ids,
    lotacaoNome: texto(item.lotacaoNome, 'Nome histórico da lotação', 150),
    jornadaDescricao: texto(item.jornadaDescricao, 'Descrição histórica da jornada', 150),
    cargoNome: texto(item.cargoNome, 'Nome histórico do cargo', 150),
    lotacaoMunicipio, municipioComprovado,
    jornadaSemanal: jornadaComprovada === true ? prepararGradeSemanal(item.semana) : null,
    jornadaComprovada,
  };
};

export const prepararProvaContrato = (formulario = {}) => {
  const completa = booleanoHistorico(formulario.configuracaoCompletaComprovada) ?? false;
  const campos = completa ? CAMPOS_FOTOGRAFIA.map(({ nome }) => nome) : [...(formulario.camposComprovados || [])];
  if (campos.some((campo) => !CAMPOS_FOTOGRAFIA.some(({ nome }) => nome === campo))) throw new TypeError('A fotografia contém um campo não histórico.');
  const fotografia = {};
  for (const campo of CAMPOS_FOTOGRAFIA) {
    const valor = campos.includes(campo.nome) ? formulario.fotografia?.[campo.nome] : null;
    if (campo.tipo === 'id') fotografia[campo.nome] = idHistorico(valor, campo.rotulo, true);
    else if (campo.tipo === 'integer') fotografia[campo.nome] = inteiro(valor, campo.rotulo, campo.min ?? 0, campo.max ?? Number.MAX_SAFE_INTEGER, true);
    else if (campo.tipo === 'boolean') fotografia[campo.nome] = booleanoHistorico(valor);
    else if (campo.tipo === 'date') fotografia[campo.nome] = data(valor, campo.rotulo, true);
    else if (campo.tipo === 'dia') fotografia[campo.nome] = enumHistorico(valor, DIAS_SEMANA.map(([dia]) => dia), 'um dia da semana', true);
    else if (campo.tipo === 'habilitacao') fotografia[campo.nome] = enumHistorico(valor, ['ATIVO', 'INATIVO'], 'uma habilitação administrativa', true);
    else fotografia[campo.nome] = texto(valor, campo.rotulo, campo.limite);
  }
  const composicoes = (formulario.composicoes || []).map(prepararComposicao);
  if (!campos.length && !composicoes.length) throw new TypeError('Declare ao menos um campo ou uma configuração comprovada.');
  const dataDocumento = data(formulario.dataDocumento, 'Data do documento original');
  if (dataDocumento > serializarLocalDate(new Date())) throw new TypeError('A data do documento original não pode estar no futuro.');
  return {
    eventoOrigemId: idHistorico(formulario.eventoOrigemId, 'ID do evento de origem', true),
    versaoAnteriorId: idHistorico(formulario.versaoAnteriorId, 'ID da fotografia anterior', true),
    recuperarPublicacaoOriginal: booleanoHistorico(formulario.recuperarPublicacaoOriginal) ?? false,
    dataDocumento, ...fonte(formulario, true),
    versaoContratoEsperada: versao(formulario.versaoContratoEsperada, 'Versão esperada do contrato'),
    configuracaoCompletaComprovada: completa, camposComprovados: campos, fotografia, composicoes,
  };
};

export const PREPARADORES_HISTORICOS = Object.freeze({
  contrato: prepararProvaContrato, catalogo: prepararTransicaoCatalogo,
  nome: prepararProvaNome, motivo: prepararProvaMotivo,
});
