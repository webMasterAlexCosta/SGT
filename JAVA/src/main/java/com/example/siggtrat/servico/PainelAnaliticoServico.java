package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.PainelAnaliticoDTO;
import com.example.siggtrat.dto.PainelAnaliticoDTO.*;
import com.example.siggtrat.dto.PainelGestaoDTO;
import com.example.siggtrat.repositorio.PainelAnaliticoRepositorio;
import com.example.siggtrat.repositorio.PainelAnaliticoRepositorio.Filtro;
import com.example.siggtrat.repositorio.PainelEfetivoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
public class PainelAnaliticoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PainelAnaliticoServico.class);
    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG; }
    private final PainelAnaliticoRepositorio repo;
    private final PainelEfetivoRepositorio efetivo;
    private final PainelServico painel;
    private static final int TAMANHO_PAGINA=20;
    public PainelAnaliticoServico(PainelAnaliticoRepositorio repo, PainelEfetivoRepositorio efetivo, PainelServico painel) {
        this.repo=repo;this.efetivo=efetivo;this.painel=painel;
    }

    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ, timeout=90)
    public PainelAnaliticoDTO obter(String granularidade,LocalDate referencia,Long contratoId,String tipoUnidade,Long unidadeId,boolean comparar,int pagina) {
        return obter(granularidade,referencia,contratoId,tipoUnidade,unidadeId,comparar,pagina,null);
    }
    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ, timeout=90)
    public PainelAnaliticoDTO obter(String granularidade,LocalDate referencia,Long contratoId,String tipoUnidade,Long unidadeId,boolean comparar,int pagina, Long prestadorId) {
        var contexto=painel.contextoAtual(); // Autorização atual em toda leitura, antes de qualquer consulta.
        LocalDate hoje=LocalDate.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO));
        LocalDate ref=referencia==null?hoje.minusDays(1):referencia;
        if(ref.isAfter(hoje)||ref.isBefore(hoje.minusYears(2))) throw erro(HttpStatus.BAD_REQUEST,"A referência deve estar nos últimos dois anos, sem data futura.");
        if(pagina<0||pagina>100000||(contratoId!=null&&contratoId<=0)||(unidadeId!=null&&unidadeId<=0)||(prestadorId!=null&&prestadorId<=0)) throw erro(HttpStatus.BAD_REQUEST,"Filtro ou página inválidos.");
        ResolvedorPeriodoPainel.Periodo periodo;
        try { periodo=ResolvedorPeriodoPainel.resolver(granularidade,ref); }
        catch(IllegalArgumentException e) { throw erro(HttpStatus.BAD_REQUEST,"Granularidade inválida. Use DIARIO, SEMANAL, MENSAL, TRIMESTRAL ou ANUAL."); }
        var faixas=ResolvedorPeriodoPainel.evolucao(granularidade,ref).stream().map(this::faixa).toList();
        var consulta=resolverConsulta(contexto,faixas.get(0).inicio(),ref,contratoId,tipoUnidade,unidadeId);
        var prestadores=repo.prestadores(consulta.contratos().stream().map(Opcao::id).toList());
        if (prestadorId!=null && prestadores.stream().noneMatch(p->p.id().equals(prestadorId))) throw erro(HttpStatus.FORBIDDEN,"Prestador fora do escopo autorizado.");
        var original=consulta.filtro();
        var filtro=new Filtro(original.gestao(),original.superintendencia(),original.contrato(),original.tipo(),original.unidade(),prestadorId);
        TipoUnidade tipo=filtro.tipo();
        List<Opcao> unidades=consulta.unidades(),contratos=consulta.contratos();
        Long gestao=filtro.gestao(),se=filtro.superintendencia(),lotacao=consulta.lotacao();
        Totais totais=repo.totais(periodo.inicio(),ref,filtro);
        Totais anterior=comparar?repo.totais(periodo.anteriorInicio(),periodo.anteriorFim(),filtro):null;
        List<Totais> serie=repo.serie(faixas,filtro);
        List<Faixa> faixasAnteriores=faixas.stream().map(f->{
            var p=ResolvedorPeriodoPainel.resolver(granularidade,f.fim());
            return new Faixa(p.anteriorInicio(),p.anteriorFim(),f.rotulo(),f.parcial());
        }).toList();
        List<Totais> serieAnterior=comparar?repo.serie(faixasAnteriores,filtro):List.of();
        List<Ponto> pontos=new ArrayList<>();
        for(int i=0;i<faixas.size();i++) pontos.add(new Ponto(faixas.get(i),comparar?faixasAnteriores.get(i):null,valor(serie,i),comparar?valor(serieAnterior,i):null));
        var colunas=ResolvedorPeriodoPainel.heatmap(granularidade,ref).stream().map(this::faixa).toList();
        LocalDate inicioBase = faixas.get(0).inicio();
        if (comparar && faixasAnteriores.get(0).inicio().isBefore(inicioBase)) inicioBase = faixasAnteriores.get(0).inicio();
        if (colunas.get(0).inicio().isBefore(inicioBase)) inicioBase = colunas.get(0).inicio();
        var bases = efetivo.obterCoberturaAnalitica(inicioBase, ref, contratoId, lotacao, gestao, se, tipo, prestadorId);
        Map<Long,String> nomes = new HashMap<>(); unidades.forEach(u -> nomes.put(u.id(),u.nome()));
        var totaisUnidades = repo.paginaUnidades(colunas.get(0).inicio().isBefore(periodo.inicio()) ? colunas.get(0).inicio() : periodo.inicio(), ref, filtro, 0, 100000);
        Set<Long> ids = new TreeSet<>(); totaisUnidades.forEach(u -> ids.add(u.id()));
        bases.stream().filter(x -> !x.data().isBefore(colunas.get(0).inicio()))
                .filter(x -> unidadeId == null || unidadeId.equals(x.unidadeId())).forEach(x -> ids.add(x.unidadeId()));
        long totalUnidades = ids.size();
        List<Long> paginaIds = ids.stream().skip((long)pagina*TAMANHO_PAGINA).limit(TAMANHO_PAGINA).toList();
        var mapaPeriodo = repo.heatmap(List.of(new Faixa(periodo.inicio(),ref,"Período",periodo.parcial())),paginaIds,filtro);
        List<Unidade> tabela = paginaIds.stream().map(id -> new Unidade(id, nomes.getOrDefault(id,"Unidade histórica #"+id), valor(mapaPeriodo.getOrDefault(id,List.of()),0))).toList();
        var mapa = repo.heatmap(colunas,paginaIds,filtro);
        List<LinhaHeatmap> calor = tabela.stream().map(u -> new LinhaHeatmap(u.id(),u.nome(),mapa.getOrDefault(u.id(),Collections.nCopies(colunas.size(),Totais.vazio())))).toList();
        BaseCobertura cobertura = base(bases, ref, ref, unidadeId);
        List<PontoPendencias> pendencias = new ArrayList<>();
        for (int i=0;i<faixas.size();i++) pendencias.add(new PontoPendencias(faixas.get(i), base(bases,faixas.get(i).inicio(),faixas.get(i).fim(),unidadeId),
                comparar ? base(bases,faixasAnteriores.get(i).inicio(),faixasAnteriores.get(i).fim(),unidadeId) : null));
        List<LinhaPendencias> calorPendencias = tabela.stream().map(u -> new LinhaPendencias(u.id(),u.nome(),
                colunas.stream().map(c -> base(bases,c.inicio(),c.fim(),u.id())).toList())).toList();
        var detalhes=efetivo.obterDetalhamentoContratos(ref,contratoId,lotacao,null,null,gestao,se,prestadorId);
        long previsto=0,alocado=0,lancados=0,pendentes=0;
        for(var d:detalhes) { previsto+=d.efetivoPrevisto();alocado+=d.efetivoAlocado();lancados+=d.registrosLancados();pendentes+=d.pendentesLancamento(); }
        var retrato=new Retrato(previsto,alocado,lancados,pendentes,taxa(lancados,alocado),taxa(alocado,previsto),detalhes.size());
        var metas=List.of(new Meta("COBERTURA","Cobertura de frequência",cobertura.semProva()>0?null:taxa(cobertura.lancados(),cobertura.esperados()),100D,
                    cobertura.lancados()+" / "+cobertura.esperados()+" esperados. Meta proposta após prazo; "+cobertura.semProva()+" sem prova de escala."),
                new Meta("OCUPACAO","Ocupação do efetivo",retrato.ocupacao(),100D,alocado+" vinculados / "+previsto+" previstos. Meta proposta; déficit e excesso são distintos."),
                new Meta("QUALIDADE_CADASTRAL","Qualidade cadastral",taxa(cobertura.comAdmissao(),cobertura.vinculados()),100D,
                    cobertura.comAdmissao()+" / "+cobertura.vinculados()+" vínculos com admissão preenchida no cadastro consultado. Meta proposta; não mede toda a qualidade cadastral."));
        var tempestividade=repo.tempestividade(periodo.inicio(),ref,filtro);
        List<DiagnosticoContrato> diagnosticoContratos = new ArrayList<>();
        for (var d : detalhes) {
            var baseContrato = contratoId != null ? cobertura : base(efetivo.obterCoberturaAnalitica(ref,ref,d.id(),lotacao,gestao,se,tipo),ref,ref,unidadeId);
            diagnosticoContratos.add(new DiagnosticoContrato(d.id(),d.nome(),repo.prestador(d.id()),d.efetivoPrevisto(),d.efetivoAlocado(),baseContrato.esperados(),baseContrato.lancados(),baseContrato.pendentes(),efetivo.obterFimVigenciaCanonica(d.id(),ref)));
        }
        List<PainelGestaoDTO.AlertaGestao> alertas=new ArrayList<>();
        long vencendo=diagnosticoContratos.stream().filter(c->c.vigenciaFim()!=null && !c.vigenciaFim().isBefore(ref) && !c.vigenciaFim().isAfter(ref.plusDays(30))).count();
        if(vencendo>0) alertas.add(new PainelGestaoDTO.AlertaGestao("CONTRATOS_VENCENDO_30_DIAS","AVISO","Vencimento contratual em até 30 dias",vencendo+" contrato(s) vigente(s) no filtro com término entre "+ref+" e "+ref.plusDays(30)+", pela vigência canônica.",null));
        if(cobertura.pendentes()>0) alertas.add(new PainelGestaoDTO.AlertaGestao("ALOCADOS_SEM_REGISTRO","AVISO","Frequências pendentes na referência",cobertura.pendentes()+" esperado(s) pela escala sem lançamento válido; não significam faltas.",contexto.podeOperarFrequencia()?"/frequencia":null));
        if(totais.ausencias()>0) alertas.add(new PainelGestaoDTO.AlertaGestao("AUSENCIAS_REGISTRADAS","INFO","Ausências registradas no período",totais.ausencias()+" lançamento(s) de falta, atestado ou suspensão.",contexto.podeOperarFrequencia()?"/frequencia":null));
        if (alocado < previsto) alertas.add(new PainelGestaoDTO.AlertaGestao("DEFICIT_EFETIVO","AVISO","Déficit de efetivo na referência",(previsto-alocado)+" vínculo(s) abaixo do previsto.","/painel"));
        if (alocado > previsto) alertas.add(new PainelGestaoDTO.AlertaGestao("EXCESSO_EFETIVO","AVISO","Efetivo acima do previsto",(alocado-previsto)+" vínculo(s) acima da previsão.","/painel"));
        if (cobertura.comAdmissao()<cobertura.vinculados()) alertas.add(new PainelGestaoDTO.AlertaGestao("CADASTRO_ADMISSAO","AVISO","Admissão sem preenchimento",(cobertura.vinculados()-cobertura.comAdmissao())+" vínculo(s) sem admissão comprovada no cadastro consultado.",null));
        if (tempestividade.emRevisao()>0) alertas.add(new PainelGestaoDTO.AlertaGestao("REVISAO_SOLICITADA","AVISO","Solicitações de revisão pendentes",tempestividade.emRevisao()+" frequência(s) aguardando o Gestor Local.",contexto.podeOperarFrequencia()?"/frequencia":null));
        return new PainelAnaliticoDTO(ref,Instant.now(),"analitico-2-proposta",new PainelGestaoDTO.Escopo(contexto.nivel(),contexto.nome(),contexto.superintendenciaId(),contexto.gestaoLocalId(),contexto.permiteDetalhamentoOperacional()),
                new Intervalo(granularidade,periodo.inicio(),ref,periodo.parcial(),comparar?periodo.anteriorInicio():null,comparar?periodo.anteriorFim():null),
                new Selecao(contratoId,tipo,unidadeId,comparar,prestadorId),new Filtros(contratos,unidades,tipo,prestadores),totais,anterior,
                anterior==null?null:new Variacoes(comparacao(totais.registros(),anterior.registros()),comparacao(totais.presencas(),anterior.presencas()),comparacao(totais.ausencias(),anterior.ausencias())),
                retrato,pontos,new Heatmap(colunas,calor),tabela,pagina,totalUnidades,TAMANHO_PAGINA,metas,alertas,
                cobertura.semProva()>0 ? List.of(cobertura.semProva()+" vínculo(s) sem prova de escala: cobertura/pendências podem estar incompletas e a taxa não é publicada.") : List.of(),
                List.of("Registros operacionais válidos: PRESENCA, PRESENCA_PARCIAL, ATRASO, TR, FALTA, ATESTADO e SUSPENSAO. ATRASO é chegada do trabalhador, não lançamento tardio.",
                        "Tempestividade usa o primeiro lançamento e seu prazo capturado. Acervo sem prazo capturado é indeterminado; dentro + fora + indeterminado = total. Revisão é solicitação pendente independente e pode sobrepor essas classes.",
                        "Comparação: parcial usa o mesmo número de dias decorridos, limitado ao fim do calendário anterior; períodos completos usam calendários completos. Volume maior não determina melhora.",
                        "Evolução é contexto histórico separado do período dos KPIs. Heatmap diário é contexto dos últimos sete dias. Taxa de pendências = soma pendentes / soma esperados; sem prova de escala ou sem esperados não publica taxa.",
                        "Fotografia de referência: vinculados são vínculos com alocação registrada vigente no motor contratual. Esperados usam grade fotografada, folgas aprovadas e calendário aplicável à data, na verdade conhecida na consulta. Turno noturno pertence à data de início.",
                        "Qualidade cadastral mede apenas admissão preenchida, com dados do cadastro consultado, no universo dos vínculos vigentes. Não representa fotografia histórica imutável do cadastro. Metas 100% são propostas para avaliação da chefia.",
                        "Diagnóstico/heatmap: página de até 20 unidades. Totais globais e evolução incluem todo o filtro. Prestador é a identificação cadastrada no contrato consultado."),
                new Indicadores(tempestividade,cobertura,pendencias,calorPendencias,diagnosticoContratos));
    }
    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ, timeout=90)
    public PaginaRegistros registros(LocalDate referencia,LocalDate inicio,LocalDate fim,Long contratoId,String tipoUnidade,Long unidadeId,String metrica,int pagina) {
        return registros(referencia,inicio,fim,contratoId,tipoUnidade,unidadeId,metrica,pagina,null);
    }
    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ, timeout=90)
    public PaginaRegistros registros(LocalDate referencia,LocalDate inicio,LocalDate fim,Long contratoId,String tipoUnidade,Long unidadeId,String metrica,int pagina,Long prestadorId) {
        var contexto=painel.contextoAtual();
        if(!contexto.permiteDetalhamentoOperacional()) throw erro(HttpStatus.FORBIDDEN,"Este perfil acessa somente agregados do painel.");
        LocalDate hoje=LocalDate.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO));
        if(referencia==null||referencia.isAfter(hoje)||referencia.isBefore(hoje.minusYears(2))) throw erro(HttpStatus.BAD_REQUEST,"Referência inválida.");
        if(inicio==null||fim==null||inicio.isAfter(fim)||fim.isAfter(referencia)
                ||inicio.isBefore(referencia.withDayOfYear(1).minusYears(4))||java.time.temporal.ChronoUnit.DAYS.between(inicio,fim)>365
                ||pagina<0||pagina>100000||metrica==null||!List.of("registros","presencas","ausencias","dentro","fora","revisao","indeterminado").contains(metrica))
            throw erro(HttpStatus.BAD_REQUEST,"Intervalo, métrica ou página inválidos para o detalhamento.");
        var consulta=resolverConsulta(contexto,referencia.withDayOfYear(1).minusYears(4),referencia,contratoId,tipoUnidade,unidadeId);
        var f=consulta.filtro();
        if (prestadorId!=null && repo.prestadores(consulta.contratos().stream().map(Opcao::id).toList()).stream().noneMatch(p->p.id().equals(prestadorId)))
            throw erro(HttpStatus.FORBIDDEN,"Prestador fora do escopo autorizado.");
        return repo.registros(inicio,fim,new Filtro(f.gestao(),f.superintendencia(),f.contrato(),f.tipo(),f.unidade(),prestadorId),metrica,pagina);
    }

    private record Consulta(Filtro filtro,Long lotacao,List<Opcao> unidades,List<Opcao> contratos) {}
    private Consulta resolverConsulta(PainelServico.PainelCalculoContexto contexto,LocalDate inicioHistorico,LocalDate ref,
                                       Long contratoId,String tipoUnidade,Long unidadeId) {
        if((contratoId!=null&&contratoId<=0)||(unidadeId!=null&&unidadeId<=0)) throw erro(HttpStatus.BAD_REQUEST,"Identificador de filtro inválido.");
        TipoUnidade tipo=contexto.gestaoLocalId()!=null?TipoUnidade.LOTACAO:contexto.superintendenciaId()!=null?TipoUnidade.GESTAO_LOCAL:TipoUnidade.SUPERINTENDENCIA;
        if(tipoUnidade!=null&&!tipo.name().equals(tipoUnidade)) throw erro(HttpStatus.FORBIDDEN,"O tipo de unidade não pertence ao nível autorizado do painel.");
        List<Opcao> unidades=repo.unidades(tipo,contexto.gestaoLocalId(),contexto.superintendenciaId());
        if(unidadeId!=null&&unidades.stream().noneMatch(u->u.id().equals(unidadeId))) throw erro(HttpStatus.FORBIDDEN,"Unidade fora do escopo autorizado.");
        Long gestao=contexto.gestaoLocalId(),se=contexto.superintendenciaId(),lotacao=null;
        if(unidadeId!=null) switch(tipo) { case SUPERINTENDENCIA -> se=unidadeId; case GESTAO_LOCAL -> gestao=unidadeId; case LOTACAO -> lotacao=unidadeId; }
        var contratos=efetivo.listarContratos(gestao,se,lotacao,null,null,inicioHistorico,ref).stream().map(c->new Opcao(c.id(),c.nome())).toList();
        if(contratoId!=null&&contratos.stream().noneMatch(c->c.id().equals(contratoId))) throw erro(HttpStatus.FORBIDDEN,"Contrato fora do escopo ou do histórico consultado.");
        return new Consulta(new Filtro(gestao,se,contratoId,tipo,unidadeId),lotacao,unidades,contratos);
    }

    private BaseCobertura base(List<PainelEfetivoRepositorio.BaseCobertura> fontes, LocalDate inicio, LocalDate fim, Long unidade) {
        long vinculados=0,esperados=0,lancados=0,pendentes=0,semProva=0,comAdmissao=0;
        for (var b : fontes) if (!b.data().isBefore(inicio) && !b.data().isAfter(fim) && (unidade==null || unidade.equals(b.unidadeId()))) {
            vinculados+=b.vinculados(); esperados+=b.esperados(); lancados+=b.lancados(); pendentes+=b.pendentes(); semProva+=b.semGrade(); comAdmissao+=b.comAdmissao();
        }
        return new BaseCobertura(vinculados,esperados,lancados,pendentes,semProva,comAdmissao);
    }

    private Faixa faixa(ResolvedorPeriodoPainel.Faixa f) { return new Faixa(f.inicio(),f.fim(),f.rotulo(),f.parcial()); }
    private Totais valor(List<Totais> valores,int i) { return i<valores.size()?valores.get(i):Totais.vazio(); }
    private Double taxa(long n,long d) { return d==0?null:Math.round(n*10000D/d)/100D; }
    private Comparacao comparacao(long atual,long anterior) { return new Comparacao(atual-anterior,anterior==0?null:Math.round((atual-anterior)*10000D/anterior)/100D); }
    private ResponseStatusException erro(HttpStatus status,String mensagem) { return new ResponseStatusException(status,mensagem); }
}
