import type { PessoaDTO, EnderecoDTO } from '../../contracts/java';
const vazio = (valor: unknown) => valor == null || String(valor).trim() === "";

export const ESTADOS_VINCULO = Object.freeze([
  { value: "REGISTRADO", label: "Registrado" },
  { value: "CANCELADO", label: "Cancelado" },
  { value: "ANULADO", label: "Anulado" },
]);

export const CATEGORIAS_TERMINO = Object.freeze<Array<{ value: import("../../contracts/java").CategoriaTerminoVinculo; label: string }>>([
  { value: "DESLIGAMENTO_INDIVIDUAL", label: "Desligamento individual" },
  { value: "FIM_VIGENCIA_CONTRATO", label: "Fim da vigência do contrato" },
  { value: "ENCERRAMENTO_ANTECIPADO_CONTRATO", label: "Encerramento antecipado do contrato" },
  { value: "ANULACAO_CONTRATO", label: "Anulação do contrato" },
  { value: "OUTRO", label: "Outro" },
]);

export const limparParametrosPessoaVinculo = (parametros = {}) => Object.fromEntries(
  Object.entries(parametros).filter(([, valor]) => valor !== undefined && valor !== null && valor !== ""),
);

export const formatarCpfProtegido = (valor: (string | null)) => {
  const texto = String(valor ?? "").trim();
  if (!texto) return "—";
  if (texto.includes("*")) return texto;
  const digitos = texto.replace(/\D/g, "");
  if (digitos.length !== 11) return texto;
  return `***.${digitos.slice(3, 6)}.${digitos.slice(6, 9)}-**`;
};

export const formatarCpfCompleto = (valor: (string | null)) => {
  const texto = String(valor ?? "").trim();
  if (!texto || texto.includes("*")) return texto || "—";
  const digitos = texto.replace(/\D/g, "");
  if (digitos.length !== 11) return texto;
  return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6, 9)}-${digitos.slice(9)}`;
};

export const formatarDataPessoaVinculo = (valor: (`${number}-${number}-${number}` | null) | (`${number}-${number}-${number}` | null | undefined), padrao = "—") => {
  if (!valor) return padrao;
  const data = new Date(`${String(valor).slice(0, 10)}T12:00:00`);
  return Number.isNaN(data.getTime()) ? padrao : new Intl.DateTimeFormat("pt-BR").format(data);
};

export const formatarInstantePessoaVinculo = (valor: (import("../../contracts/shared").Instant | null), padrao = "—") => {
  if (!valor) return padrao;
  const data = new Date(valor);
  return Number.isNaN(data.getTime())
    ? padrao
    : new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "medium" }).format(data);
};

export const rotularSituacaoVinculo = (valor: (import("../../contracts/java").SituacaoTemporalVinculo | null)) => ({
  PROGRAMADO: "Programado",
  VIGENTE: "Vigente",
  ENCERRADO: "Encerrado",
  CANCELADO: "Cancelado",
  ANULADO: "Anulado",
}[String(valor || "").toUpperCase()] || "Não informada");

export const rotularEstadoVinculo = (valor: (import("../../contracts/java").EstadoNegocialVinculo | null)) => ({
  REGISTRADO: "Registrado",
  CANCELADO: "Cancelado",
  ANULADO: "Anulado",
}[String(valor || "").toUpperCase()] || "Não informado");

export const rotularTipoEventoVinculo = (valor: (import("../../contracts/java").TipoEventoVinculoContratual | null)) => ({
  CRIACAO: "Criação",
  RETIFICACAO_INICIO: "Retificação do início",
  ALTERACAO_MATRICULA: "Alteração de matrícula",
  CANCELAMENTO_PROGRAMACAO: "Cancelamento da programação",
  DESLIGAMENTO: "Desligamento",
  RETIFICACAO_DESLIGAMENTO: "Retificação do desligamento",
  CANCELAMENTO_DESLIGAMENTO_PROGRAMADO: "Cancelamento do desligamento programado",
  FIM_NATURAL_CONTRATO: "Fim natural do contrato",
  ENCERRAMENTO_ANTECIPADO_CONTRATO: "Encerramento antecipado do contrato",
  ANULACAO_CONTRATO: "Anulação do contrato",
}[String(valor || "").toUpperCase()] || String(valor || "Evento").replaceAll("_", " "));

const numeroId = (valor: unknown) => {
  const numero = Number(valor);
  return Number.isSafeInteger(numero) && numero > 0 ? numero : null;
};

const textoOuNulo = (valor: unknown) => {
  const texto = String(valor ?? "").trim();
  return texto || null;
};

export const normalizarEnderecoPessoa = (dados: { logradouro?: unknown; numero?: unknown; bairro?: unknown; cidade?: unknown; uf?: unknown; cep?: unknown } = {}) => {
  if ([dados.logradouro, dados.numero, dados.bairro, dados.cidade, dados.uf, dados.cep].every(vazio)) return null;
  return {
    logradouro: textoOuNulo(dados.logradouro),
    numero: textoOuNulo(dados.numero),
    bairro: textoOuNulo(dados.bairro),
    cidade: textoOuNulo(dados.cidade),
    uf: textoOuNulo(dados.uf)?.toUpperCase() || null,
    cep: textoOuNulo(dados.cep),
  };
};

export const prepararCadastroPessoa = (dados: { nome?: unknown; cpf?: unknown; rg?: unknown; ctpsSerie?: unknown; nitPisPasep?: unknown; dataNascimento?: unknown; email?: unknown; endereco?: unknown } = {}) => ({
  nome: textoOuNulo(dados.nome),
  cpf: String(dados.cpf ?? "").replace(/\D/g, "") || null,
  rg: textoOuNulo(dados.rg),
  ctpsSerie: textoOuNulo(dados.ctpsSerie),
  nitPisPasep: textoOuNulo(dados.nitPisPasep),
  dataNascimento: textoOuNulo(dados.dataNascimento),
  email: textoOuNulo(dados.email),
  endereco: normalizarEnderecoPessoa(dados.endereco || {}),
});

export const prepararCadastroVinculo = (dados: { modoPessoa?: unknown; pessoaId?: unknown; pessoa?: Omit<Partial<PessoaDTO>, "endereco" | "dataNascimento"> & { dataNascimento?: string; endereco?: Partial<EnderecoDTO> }; contratoId?: unknown; dataInicio?: unknown; dataFim?: unknown; matricula?: unknown; composicaoId?: unknown; confirmarInclusaoExcedente?: unknown; justificativaExcedente?: unknown; evidenciaExcedente?: unknown; autorizacaoExcedente?: unknown } = {}) => {
  const pessoaExistente = dados.modoPessoa === "EXISTENTE";
  return {
    pessoaId: pessoaExistente ? numeroId(dados.pessoaId) : null,
    pessoa: pessoaExistente ? null : prepararCadastroPessoa(dados.pessoa),
    contratoId: numeroId(dados.contratoId),
    dataInicio: textoOuNulo(dados.dataInicio),
    dataFim: textoOuNulo(dados.dataFim),
    matricula: textoOuNulo(dados.matricula),
    composicaoId: numeroId(dados.composicaoId),
    confirmarInclusaoExcedente: Boolean(dados.confirmarInclusaoExcedente),
    justificativaExcedente: textoOuNulo(dados.justificativaExcedente),
    evidenciaExcedente: textoOuNulo(dados.evidenciaExcedente),
    autorizacaoExcedente: textoOuNulo(dados.autorizacaoExcedente),
  };
};

const cpfEstruturalmenteValido = (valor: (string | null | undefined)) => {
  const cpf = String(valor ?? "").replace(/\D/g, "");
  if (!/^\d{11}$/.test(cpf) || /^(\d)\1{10}$/.test(cpf)) return false;
  for (let digito = 9; digito < 11; digito += 1) {
    let soma = 0;
    for (let indice = 0; indice < digito; indice += 1) soma += Number(cpf[indice]) * (digito + 1 - indice);
    const esperado = ((soma * 10) % 11) % 10;
    if (esperado !== Number(cpf[digito])) return false;
  }
  return true;
};

export const validarCadastroVinculo = (dados: { contratoId?: unknown; dataInicio?: unknown; composicaoId?: unknown; dataFim?: unknown; modoPessoa?: unknown; pessoaId?: unknown; pessoa?: Omit<Partial<PessoaDTO>, "endereco" | "dataNascimento"> & { dataNascimento?: string; endereco?: Partial<EnderecoDTO> }; confirmarInclusaoExcedente?: unknown; justificativaExcedente?: unknown; evidenciaExcedente?: unknown; autorizacaoExcedente?: unknown } = {}, opcaoAlocacao: { exigeJustificativaExcedente?: boolean | null } | null = null) => {
  if (!dados.contratoId || !dados.dataInicio || !dados.composicaoId) return "Contrato, início efetivo e alocação inicial são obrigatórios.";
  if (dados.dataFim && dados.dataFim < dados.dataInicio) return "O fim do vínculo não pode anteceder o início.";
  if (dados.modoPessoa === "EXISTENTE" && !dados.pessoaId) return "Selecione uma pessoa existente.";
  if (dados.modoPessoa !== "EXISTENTE") {
    if (vazio(dados.pessoa?.nome) || vazio(dados.pessoa?.cpf)) return "Nome e CPF são obrigatórios para uma nova pessoa.";
    if (!cpfEstruturalmenteValido(dados.pessoa?.cpf)) return "Informe um CPF válido com 11 dígitos.";
    if (normalizarEnderecoPessoa(dados.pessoa?.endereco || {}) && vazio(dados.pessoa?.endereco?.logradouro)) {
      return "Ao informar endereço, o logradouro é obrigatório.";
    }
  }
  if (opcaoAlocacao?.exigeJustificativaExcedente) {
    if (!dados.confirmarInclusaoExcedente || [dados.justificativaExcedente, dados.evidenciaExcedente, dados.autorizacaoExcedente].some(vazio)) {
      return "A inclusão excedente exige confirmação, justificativa, evidência e autorização.";
    }
  }
  return null;
};

export const prepararAtualizacaoPessoa = (dados: { rg?: unknown; limparRg?: unknown; ctpsSerie?: unknown; limparCtpsSerie?: unknown; nitPisPasep?: unknown; limparNitPisPasep?: unknown; dataNascimento?: unknown; limparDataNascimento?: unknown; email?: unknown; limparEmail?: unknown; endereco?: unknown; limparEndereco?: unknown } = {}) => ({
  rg: textoOuNulo(dados.rg), limparRg: Boolean(dados.limparRg),
  ctpsSerie: textoOuNulo(dados.ctpsSerie), limparCtpsSerie: Boolean(dados.limparCtpsSerie),
  nitPisPasep: textoOuNulo(dados.nitPisPasep), limparNitPisPasep: Boolean(dados.limparNitPisPasep),
  dataNascimento: textoOuNulo(dados.dataNascimento), limparDataNascimento: Boolean(dados.limparDataNascimento),
  email: textoOuNulo(dados.email), limparEmail: Boolean(dados.limparEmail),
  endereco: normalizarEnderecoPessoa(dados.endereco || {}), limparEndereco: Boolean(dados.limparEndereco),
});

export const prepararCorrecaoIdentidade = (dados: { tipo?: import("../../contracts/java").TipoCorrecaoPessoa | null; novoValor?: unknown; motivo?: unknown; evidencia?: unknown; autorizacao?: unknown; impactosEncontrados?: unknown } = {}) => ({
  tipo: dados.tipo || null,
  novoValor: dados.tipo === "CPF" ? String(dados.novoValor ?? "").replace(/\D/g, "") : textoOuNulo(dados.novoValor),
  motivo: textoOuNulo(dados.motivo),
  evidencia: textoOuNulo(dados.evidencia),
  autorizacao: textoOuNulo(dados.autorizacao),
  impactosEncontrados: textoOuNulo(dados.impactosEncontrados),
});

export const prepararAcaoAuditavel = (dados: { justificativa?: unknown; evidencia?: unknown; autorizacao?: unknown; confirmarImpacto?: unknown; tokenPrevia?: unknown } = {}) => ({
  justificativa: textoOuNulo(dados.justificativa),
  evidencia: textoOuNulo(dados.evidencia),
  autorizacao: textoOuNulo(dados.autorizacao),
  confirmarImpacto: Boolean(dados.confirmarImpacto),
  tokenPrevia: textoOuNulo(dados.tokenPrevia),
});

export const prepararRetificacaoInicio = (dados: { novoInicio?: unknown; confirmarInclusaoExcedente?: unknown } & Parameters<typeof prepararAcaoAuditavel>[0] = {}) => ({
  novoInicio: textoOuNulo(dados.novoInicio),
  ...prepararAcaoAuditavel(dados),
  confirmarInclusaoExcedente: Boolean(dados.confirmarInclusaoExcedente),
});

export const prepararAlteracaoMatricula = (dados: { dataEfetiva?: unknown; novaMatricula?: unknown; justificativa?: unknown; evidencia?: unknown; autorizacao?: unknown } = {}) => ({
  dataEfetiva: textoOuNulo(dados.dataEfetiva),
  novaMatricula: textoOuNulo(dados.novaMatricula),
  justificativa: textoOuNulo(dados.justificativa),
  evidencia: textoOuNulo(dados.evidencia),
  autorizacao: textoOuNulo(dados.autorizacao),
});

export const prepararDesligamento = (dados: { dataFim?: unknown; motivoDesligamentoId?: unknown; confirmarPrecedenciaUltimoDia?: unknown; confirmarInclusaoExcedente?: unknown } & Parameters<typeof prepararAcaoAuditavel>[0] = {}) => ({
  dataFim: textoOuNulo(dados.dataFim),
  motivoDesligamentoId: numeroId(dados.motivoDesligamentoId),
  ...prepararAcaoAuditavel(dados),
  confirmarPrecedenciaUltimoDia: Boolean(dados.confirmarPrecedenciaUltimoDia),
  confirmarInclusaoExcedente: Boolean(dados.confirmarInclusaoExcedente),
});

export const descreverOpcaoAlocacao = (opcao: { ocupacaoAposInclusao?: unknown; efetivoPrevisto?: unknown; exigeJustificativaExcedente?: unknown; excedenteAposInclusao?: unknown; lotacaoNome?: unknown; jornadaDescricao?: unknown; cargoNome?: unknown } = {}) => {
  const capacidade = `${Number(opcao.ocupacaoAposInclusao || 0)}/${Number(opcao.efetivoPrevisto || 0)}`;
  const excedente = opcao.exigeJustificativaExcedente ? ` · excedente +${Number(opcao.excedenteAposInclusao || 0)}` : "";
  return `${opcao.lotacaoNome || "Lotação"} · ${opcao.jornadaDescricao || "Turno"} · ${opcao.cargoNome || "Cargo"} · ocupação ${capacidade}${excedente}`;
};
