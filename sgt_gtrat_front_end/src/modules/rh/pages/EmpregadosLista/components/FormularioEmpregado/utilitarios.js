export const extrairApenasDigitos = (valor = "") => String(valor || "").replace(/\D/g, "");

export const verificarCpfValido = (cpfBruto) => {
  const cpfCriptografado = extrairApenasDigitos(cpfBruto);
  if (cpfCriptografado.length !== 11) return false;
  if (/^(\d)\1{10}$/.test(cpfCriptografado)) return false;
  
  const calcularDigitoVerificador = (base, fator) => {
    let totalCaculado = 0;
    for (let indice = 0; indice < base.length; indice += 1) {
      totalCaculado += Number(base[indice]) * (fator - indice);
    }
    const moduloCaculado = (totalCaculado * 10) % 11;
    return moduloCaculado === 10 ? 0 : moduloCaculado;
  };
  
  const primeiroDigito = calcularDigitoVerificador(cpfCriptografado.slice(0, 9), 10);
  const segundoDigito = calcularDigitoVerificador(cpfCriptografado.slice(0, 10), 11);
  
  return primeiroDigito === Number(cpfCriptografado[9]) && segundoDigito === Number(cpfCriptografado[10]);
};

export const converterMoedaParaNumero = (valorBruto) => {
  if (valorBruto == null || valorBruto === "") return null;
  if (typeof valorBruto === "number") return Number.isFinite(valorBruto) ? valorBruto : null;
  const textoLimpo = String(valorBruto).trim().replace(/[R$\s]/g, "");
  
  if (textoLimpo.includes(",")) {
    const textoNormalizado = textoLimpo.replace(/\./g, "").replace(",", ".");
    return Number.isFinite(Number(textoNormalizado)) ? Number(textoNormalizado) : null;
  }
  return Number.isFinite(Number(textoLimpo)) ? Number(textoLimpo) : null;
};

export const formatarMoedaBrasileira = (numeroDecimal) => {
  if (numeroDecimal == null || !Number.isFinite(numeroDecimal)) return "-";
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(numeroDecimal);
};

export const calcularTotalMinutosJornada = (dadosJornada) => {
  if (!dadosJornada) return null;
  if (dadosJornada.duracaoPadraoMin != null) return Number(dadosJornada.duracaoPadraoMin);
  
  const converterHorasParaMinutos = (formatoHoraMinuto) => {
    if (!formatoHoraMinuto) return null;
    const [horas, minutos] = String(formatoHoraMinuto).split(":").map(Number);
    return Number.isFinite(horas) && Number.isFinite(minutos) ? (horas * 60) + minutos : null;
  };
  
  const calcularDiferencaHorarios = (inicioTurno, fimTurno) => {
    const minutosInicio = converterHorasParaMinutos(inicioTurno);
    const minutosFim = converterHorasParaMinutos(fimTurno);
    if (minutosInicio == null || minutosFim == null) return 0;
    return minutosFim >= minutosInicio ? minutosFim - minutosInicio : (24 * 60) - minutosInicio + minutosFim;
  };

  if (dadosJornada.dias && dadosJornada.dias.length > 0) {
    const diasDeTrabalho = dadosJornada.dias.filter(d => d.diaTrabalho);
    if (diasDeTrabalho.length === 0) return null;
    
    // O primeiro dia útil serve de base para a estimativa diária exibida.
    const diaBase = diasDeTrabalho[0];
    const somaTotalMinutosDia = calcularDiferencaHorarios(diaBase.entrada1, diaBase.saida1) + 
                                calcularDiferencaHorarios(diaBase.entrada2, diaBase.saida2);
    return somaTotalMinutosDia > 0 ? somaTotalMinutosDia : null;
  }
  
  return null;
};

export const formatarNumeroTelefone = (numeroBruto) => extrairApenasDigitos(numeroBruto);

export const verificarTelefoneValido = (numeroBruto) => {
  const quantidadeDigitos = formatarNumeroTelefone(numeroBruto);
  return quantidadeDigitos.length === 10 || quantidadeDigitos.length === 11;
};
