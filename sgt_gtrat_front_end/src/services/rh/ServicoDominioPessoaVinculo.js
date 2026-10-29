const vazio = (valor) => valor == null || String(valor).trim() === "";

export const ESTADOS_VINCULO = Object.freeze([
  { value: "REGISTRADO", label: "Registrado" },
  { value: "CANCELADO", label: "Cancelado" },
  { value: "ANULADO", label: "Anulado" },
]);

export const CATEGORIAS_TERMINO = Object.freeze([
  { value: "DESLIGAMENTO_INDIVIDUAL", label: "Desligamento individual" },
  { value: "FIM_VIGENCIA_CONTRATO", label: "Fim da vigência do contrato" },
  { value: "ENCERRAMENTO_ANTECIPADO_CONTRATO", label: "Encerramento antecipado do contrato" },
  { value: "ANULACAO_CONTRATO", label: "Anulação do contrato" },
  { value: "OUTRO", label: "Outro" },
]);

export const limparParametrosPessoaVinculo = (parametros = {}) => Object.fromEntries(
  Object.entries(parametros).filter(([, valor]) => valor !== undefined && valor !== null && valor !== ""),
);

export const formatarCpfProtegido = (valor) => {
  const texto = String(valor ?? "").trim();
  if (!texto) return "—";
  if (texto.includes("*")) return texto;
  const digitos = texto.replace(/\D/g, "");
  if (digitos.length !== 11) return texto;
  return `***.${digitos.slice(3, 6)}.${digitos.slice(6, 9)}-**`;
};

export const formatarCpfCompleto = (valor) => {
  const texto = String(valor ?? "").trim();
  if (!texto || texto.includes("*")) return texto || "—";
  const digitos = texto.replace(/\D/g, "");
  if (digitos.length !== 11) return texto;
  return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6, 9)}-${digitos.slice(9)}`;
};

export const formatarDataPessoaVinculo = (valor, padrao = "—") => {
  if (!valor) return padrao;
  const data = new Date(`${String(valor).slice(0, 10)}T12:00:00`);
  return Number.isNaN(data.getTime()) ? padrao : new Intl.DateTimeFormat("pt-BR").format(data);
};

export const formatarInstantePessoaVinculo = (valor, padrao = "—") => {
  if (!valor) return padrao;
  const data = new Date(valor);
  return Number.isNaN(data.getTime())
    ? padrao
    : new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "medium" }).format(data);
};

export const rotularSituacaoVinculo = (valor) => ({
  PROGRAMADO: "Programado",
  VIGENTE: "Vigente",
  ENCERRADO: "Encerrado",
  CANCELADO: "Cancelado",
  ANULADO: "Anulado",
}[String(valor || "").toUpperCase()] || "Não informada");

export const rotularEstadoVinculo = (valor) => ({
  REGISTRADO: "Registrado",
  CANCELADO: "Cancelado",
  ANULADO: "Anulado",
}[String(valor || "").toUpperCase()] || "Não informado");

export const rotularTipoEventoVinculo = (valor) => ({
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

const numeroId = (valor) => {
  const numero = Number(valor);
  return Number.isSafeInteger(numero) && numero > 0 ? numero : null;
};

const textoOuNulo = (valor) => {
  const texto = String(valor ?? "").trim();
  return texto || null;
};

export const normalizarEnderecoPessoa = (dados = {}) => {
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

export const prepararCadastroPessoa = (dados = {}) => ({
  nome: textoOuNulo(dados.nome),
  cpf: String(dados.cpf ?? "").replace(/\D/g, "") || null,
  rg: textoOuNulo(dados.rg),
  ctpsSerie: textoOuNulo(dados.ctpsSerie),
  nitPisPasep: textoOuNulo(dados.nitPisPasep),
  dataNascimento: textoOuNulo(dados.dataNascimento),
  email: textoOuNulo(dados.email),
  endereco: normalizarEnderecoPessoa(dados.endereco || {}),
});

export const prepararCadastroVinculo = (dados = {}) => {
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

const cpfEstruturalmenteValido = (valor) => {
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

export const validarCadastroVinculo = (dados = {}, opcaoAlocacao = null) => {
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

export const prepararAtualizacaoPessoa = (dados = {}) => ({
  rg: textoOuNulo(dados.rg), limparRg: Boolean(dados.limparRg),
  ctpsSerie: textoOuNulo(dados.ctpsSerie), limparCtpsSerie: Boolean(dados.limparCtpsSerie),
  nitPisPasep: textoOuNulo(dados.nitPisPasep), limparNitPisPasep: Boolean(dados.limparNitPisPasep),
  dataNascimento: textoOuNulo(dados.dataNascimento), limparDataNascimento: Boolean(dados.limparDataNascimento),
  email: textoOuNulo(dados.email), limparEmail: Boolean(dados.limparEmail),
  endereco: normalizarEnderecoPessoa(dados.endereco || {}), limparEndereco: Boolean(dados.limparEndereco),
});

export const prepararCorrecaoIdentidade = (dados = {}) => ({
  tipo: dados.tipo || null,
  novoValor: dados.tipo === "CPF" ? String(dados.novoValor ?? "").replace(/\D/g, "") : textoOuNulo(dados.novoValor),
  motivo: textoOuNulo(dados.motivo),
  evidencia: textoOuNulo(dados.evidencia),
  autorizacao: textoOuNulo(dados.autorizacao),
  impactosEncontrados: textoOuNulo(dados.impactosEncontrados),
});

export const prepararAcaoAuditavel = (dados = {}) => ({
  justificativa: textoOuNulo(dados.justificativa),
  evidencia: textoOuNulo(dados.evidencia),
  autorizacao: textoOuNulo(dados.autorizacao),
  confirmarImpacto: Boolean(dados.confirmarImpacto),
  tokenPrevia: textoOuNulo(dados.tokenPrevia),
});

export const prepararRetificacaoInicio = (dados = {}) => ({
  novoInicio: textoOuNulo(dados.novoInicio),
  ...prepararAcaoAuditavel(dados),
});

export const prepararAlteracaoMatricula = (dados = {}) => ({
  dataEfetiva: textoOuNulo(dados.dataEfetiva),
  novaMatricula: textoOuNulo(dados.novaMatricula),
  justificativa: textoOuNulo(dados.justificativa),
  evidencia: textoOuNulo(dados.evidencia),
  autorizacao: textoOuNulo(dados.autorizacao),
});

export const prepararDesligamento = (dados = {}) => ({
  dataFim: textoOuNulo(dados.dataFim),
  motivoDesligamentoId: numeroId(dados.motivoDesligamentoId),
  ...prepararAcaoAuditavel(dados),
});

export const descreverOpcaoAlocacao = (opcao = {}) => {
  const capacidade = `${Number(opcao.ocupacaoAposInclusao || 0)}/${Number(opcao.efetivoPrevisto || 0)}`;
  const excedente = opcao.exigeJustificativaExcedente ? ` · excedente +${Number(opcao.excedenteAposInclusao || 0)}` : "";
  return `${opcao.lotacaoNome || "Lotação"} · ${opcao.jornadaDescricao || "Turno"} · ${opcao.cargoNome || "Cargo"} · ocupação ${capacidade}${excedente}`;
};
