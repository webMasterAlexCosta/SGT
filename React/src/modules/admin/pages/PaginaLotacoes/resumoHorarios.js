export const resumirHorariosDoTurno = (turno = {}) => {
  if (Array.isArray(turno.dias) && turno.dias.length > 0) {
    const diasTrabalho = turno.dias.filter((dia) => dia?.diaTrabalho);
    if (diasTrabalho.length === 0) return "Sem dias úteis definidos";

    const grupos = new Map();
    for (const dia of diasTrabalho) {
      const primeiro = dia.entrada1 ? `${dia.entrada1} às ${dia.saida1}` : "";
      const segundo = dia.entrada2 ? `${dia.entrada2} às ${dia.saida2}` : "";
      const horario = primeiro && segundo
        ? `${primeiro} e ${segundo}`
        : primeiro || "Horário flexível";
      grupos.set(horario, (grupos.get(horario) || 0) + 1);
    }

    return [...grupos.entries()]
      .map(([horario, quantidade]) =>
        `${quantidade} ${quantidade === 1 ? "dia" : "dias"}: ${horario}`,
      )
      .join(" • ");
  }

  const primeiro = turno.entrada1 ? `${turno.entrada1} às ${turno.saida1}` : "";
  const segundo = turno.entrada2 ? `${turno.entrada2} às ${turno.saida2}` : "";
  if (primeiro && segundo) return `${primeiro} e ${segundo}`;
  return primeiro || "Sem horários definidos";
};
