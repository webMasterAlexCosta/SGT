package com.example.siggtrat.servico;

import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.ProvaMotivoDesligamentoHistoricoRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
import static com.example.siggtrat.servico.SuporteProvaHistoricaServico.*;

@Service
@Transactional(readOnly = true)
public class MotivoDesligamentoHistoricoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(MotivoDesligamentoHistoricoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final EntityManager em;
    private final ProvaMotivoDesligamentoHistoricoRepositorio provas;
    private final SuporteProvaHistoricaServico suporte;
    private final AcessoOrganizacionalServico acesso;
    private final GestaoContratoTemporalServico gestao;
    private final MinimizadorDadosPessoaisServico minimizador;
    private final InvalidacaoCacheServico cache;
    private final InvalidacaoEmissaoRelatorioServico relatorios;
    public MotivoDesligamentoHistoricoServico(EntityManager em, ProvaMotivoDesligamentoHistoricoRepositorio provas,
            SuporteProvaHistoricaServico suporte, AcessoOrganizacionalServico acesso, GestaoContratoTemporalServico gestao,
            MinimizadorDadosPessoaisServico minimizador, InvalidacaoCacheServico cache, InvalidacaoEmissaoRelatorioServico relatorios) {
        this.em=em; this.provas=provas; this.suporte=suporte; this.acesso=acesso; this.gestao=gestao;
        this.minimizador=minimizador; this.cache=cache; this.relatorios=relatorios;
    }
    @Transactional
    public PreviaProvaHistoricaDTO previa(Long id,RegistroProvaMotivoHistoricoRequisicao r) {
        var u=suporte.executor(); Contrato c=suporte.contrato(r==null?null:r.contratoId(),false);
        suporte.exigirCustodia(u,c); MotivoDesligamento m=motivo(id); validar(r);
        var d=suporte.fonte(c,r.documentoComprobatorioId(),r.referenciaSei(),r.finalidadeAcessoDocumento(),false);
        return preparar(m,c,r,u,d);
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String,Object> registrar(Long id,RegistroProvaMotivoHistoricoRequisicao r) {
        var u=suporte.executor(); Contrato c=suporte.contrato(r==null?null:r.contratoId(),true);
        suporte.exigirCustodia(u,c); MotivoDesligamento m=motivo(id); em.refresh(m,LockModeType.PESSIMISTIC_WRITE); validar(r);
        String intencao=intencao(id,r,u);
        var existente=provas.findByContratoIdAndResponsavelIdAndChaveIdempotencia(c.getId(),u.getId(),r.chaveIdempotencia().trim()).orElse(null);
        if(existente!=null) {
            if(!Objects.equals(intencao,existente.getHashIntencao()))throw erro(HttpStatus.CONFLICT,"Chave já usada para outra intenção.");
            return dto(existente,u,true);
        }
        var d=suporte.fonte(c,r.documentoComprobatorioId(),r.referenciaSei(),r.finalidadeAcessoDocumento(),true);
        var previa=preparar(m,c,r,u,d);
        if(!Boolean.TRUE.equals(r.confirmarImpacto()) || !Objects.equals(r.tokenPrevia(),previa.tokenPrevia()))
            throw erro(HttpStatus.CONFLICT,"Prévia não confirmada ou obsoleta. Refaça a análise.");
        var p=new ProvaMotivoDesligamentoHistorico();
        suporte.preencher(p,c,r.dataInicioEfetiva(),r.dataFimEfetiva(),d,r.referenciaSei(),r.motivo(),r.autorizacao(),r.chaveIdempotencia(),intencao,u);
        p.setMotivoDesligamento(m); p.setDescricaoComprovada(r.descricaoComprovada().trim()); p.setCategoriaComprovada(r.categoriaComprovada());
        p.setProvaAnterior(anterior(m,c,r.provaAnteriorId())); provas.saveAndFlush(p);
        suporte.registrarUso(u,c,"PROVA_MOTIVO_HISTORICO",p.getId(),r.motivo());
        em.lock(c,LockModeType.PESSIMISTIC_FORCE_INCREMENT); em.lock(m,LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        relatorios.invalidarPorContrato(c,r.dataInicioEfetiva(),r.dataFimEfetiva(),"Incorporação de contexto histórico de motivo",u);
        cache.invalidarCachesDeNegocioAposCommit();
        return dto(p,u,false);
    }
    public ContextoMotivoHistoricoDTO consultar(Long id,Long contratoId,LocalDate data,Instant k) {
        Contrato c=suporte.contrato(contratoId,false); acesso.exigirAcessoContrato(acesso.usuarioAtual(),c);
        return resolver(motivo(id),c,data,k);
    }
    public List<Map<String,Object>> listar(Long id,Long contratoId) {
        Contrato c=suporte.contrato(contratoId,false); var u=acesso.usuarioAtual(); acesso.exigirAcessoContrato(u,c);
        motivo(id);
        return provas.findByContratoIdAndMotivoDesligamentoIdOrderByRegistradoEmAscIdAsc(contratoId,id).stream().map(p->dto(p,u,false)).toList();
    }
    public ContextoMotivoHistoricoDTO resolver(MotivoDesligamento m,Contrato c,LocalDate data,Instant conhecidoAte) {
        if(m==null || c==null || data==null)return ContextoMotivoHistoricoDTO.ausente();
        Instant k=conhecidoAte==null?Instant.now():conhecidoAte;
        List<ProvaMotivoDesligamentoHistorico> candidatas=trilha(m,c).stream().filter(p->!p.getRegistradoEm().isAfter(k))
                .filter(p->IdentidadeHistoricaServico.contem(p.getDataInicioEfetiva(),p.getDataFimEfetiva(),data)).toList();
        var vigentes=candidatas.stream().filter(p->candidatas.stream().noneMatch(n->descendeDe(n,p.getId()))).toList();
        if(vigentes.isEmpty())return ContextoMotivoHistoricoDTO.ausente();
        var p=vigentes.get(0);
        if(vigentes.stream().anyMatch(n->!Objects.equals(n.getDescricaoComprovada(),p.getDescricaoComprovada()) || n.getCategoriaComprovada()!=p.getCategoriaComprovada()))
            throw erro(HttpStatus.CONFLICT,"Fontes de contexto conflitantes exigem retificação explícita.");
        return new ContextoMotivoHistoricoDTO(p.getDescricaoComprovada(),p.getCategoriaComprovada(),true,"PROVA_MOTIVO_HISTORICO",p.getId(),p.getRegistradoEm(),p.getDataInicioEfetiva(),p.getDataFimEfetiva());
    }
    /** Somente novo ato contemporâneo pode selecionar o catálogo corrente. */
    public ContextoMotivoHistoricoDTO capturarNovoFato(MotivoDesligamento m,Contrato c,LocalDate data,Instant k) {
        if(data.isBefore(RelogioSistema.hoje())) return resolver(m,c,data,k);
        return new ContextoMotivoHistoricoDTO(m.getDescricao(),m.getCategoria(),true,"CADASTRO_CONTEMPORANEO",m.getId(),k,data,data);
    }
    private MotivoDesligamento motivo(Long id) {
        var m=id==null?null:em.find(MotivoDesligamento.class,id);
        if(m==null)throw erro(HttpStatus.NOT_FOUND,"Motivo não encontrado.");
        return m;
    }
    private void validar(RegistroProvaMotivoHistoricoRequisicao r) {
        suporte.validar(r.dataInicioEfetiva(),r.dataFimEfetiva(),r.motivo(),r.autorizacao(),r.chaveIdempotencia());
        texto(r.descricaoComprovada(),160,"Descrição comprovada é obrigatória.");
        if(r.categoriaComprovada()==null || r.versaoMotivoEsperada()==null || r.versaoContratoEsperada()==null || r.versaoMotivoEsperada()<0 || r.versaoContratoEsperada()<0)
            throw erro(HttpStatus.BAD_REQUEST,"Categoria comprovada e versões esperadas são obrigatórias.");
    }
    private List<ProvaMotivoDesligamentoHistorico> trilha(MotivoDesligamento m,Contrato c) {
        return provas.findByContratoIdAndMotivoDesligamentoIdOrderByRegistradoEmAscIdAsc(c.getId(),m.getId());
    }
    private ProvaMotivoDesligamentoHistorico anterior(MotivoDesligamento m,Contrato c,Long id) {
        if(id==null)return null;
        var p=provas.findById(id).orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Prova anterior não encontrada."));
        if(!Objects.equals(p.getContrato().getId(),c.getId()) || !Objects.equals(p.getMotivoDesligamento().getId(),m.getId()))
            throw erro(HttpStatus.NOT_FOUND,"Prova anterior não encontrada no escopo.");
        return p;
    }
    private static boolean descendeDe(ProvaMotivoDesligamentoHistorico p,Long id) {
        Set<Long> vistos=new HashSet<>();
        for(var a=p.getProvaAnterior();a!=null && vistos.add(a.getId());a=a.getProvaAnterior())if(Objects.equals(a.getId(),id))return true;
        return false;
    }
    private PreviaProvaHistoricaDTO preparar(MotivoDesligamento m,Contrato c,RegistroProvaMotivoHistoricoRequisicao r,UsuarioAplicacao u,DocumentoComprobatorioDTO d) {
        if(m.getVersion()!=r.versaoMotivoEsperada() || c.getVersion()!=r.versaoContratoEsperada())throw erro(HttpStatus.CONFLICT,"Motivo ou Contrato mudou. Refaça a prévia.");
        // Um período aberto precisa continuar no mesmo escopo também após transferências programadas.
        LocalDate fim=r.dataFimEfetiva()==null?LocalDate.of(9999,12,31):r.dataFimEfetiva();
        for(LocalDate data:gestao.datasDeMudancaNoIntervalo(c,r.dataInicioEfetiva(),fim,Instant.now())) {
            GestaoLocal g=gestao.gestaoEm(c,data,Instant.now());
            if(g==null || m.getGestaoLocal()==null || !Objects.equals(g.getId(),m.getGestaoLocal().getId()))
                throw erro(HttpStatus.CONFLICT,"O motivo não pertence à Gestão comprovada em todo o intervalo.");
        }
        var anterior=anterior(m,c,r.provaAnteriorId()); var trilha=trilha(m,c);
        if(anterior!=null && trilha.stream().anyMatch(n->descendeDe(n,anterior.getId())))throw erro(HttpStatus.CONFLICT,"A origem já foi retificada. Indique a prova vigente.");
        for(var p:trilha) {
            if(!ProvaNomePessoaHistoricoServico.intersecta(p.getDataInicioEfetiva(),p.getDataFimEfetiva(),r.dataInicioEfetiva(),r.dataFimEfetiva()))continue;
            LocalDate ini=p.getDataInicioEfetiva().isAfter(r.dataInicioEfetiva())?p.getDataInicioEfetiva():r.dataInicioEfetiva();
            LocalDate limite=ProvaNomePessoaHistoricoServico.min(p.getDataFimEfetiva(),r.dataFimEfetiva());
            if(trilha.stream().anyMatch(n->descendeDe(n,p.getId()) && !n.getDataInicioEfetiva().isAfter(ini)
                    && (n.getDataFimEfetiva()==null || limite!=null && !n.getDataFimEfetiva().isBefore(limite))))continue;
            boolean relacionada=anterior!=null && (Objects.equals(anterior.getId(),p.getId()) || descendeDe(anterior,p.getId()));
            if((!r.descricaoComprovada().trim().equals(p.getDescricaoComprovada()) || r.categoriaComprovada()!=p.getCategoriaComprovada()) && !relacionada)
                throw erro(HttpStatus.CONFLICT,"Contextos conflitantes exigem provaAnteriorId explícito.");
        }
        String fontes=trilha.stream().map(p->p.getId()+":"+p.getHashConteudo()).toList().toString();
        return new PreviaProvaHistoricaDTO(hash("sgt.previa.prova-motivo.v1", intencao(m.getId(),r,u),c.getVersion(),m.getVersion(),fontes,d==null?null:d.id(),d==null?null:d.version(),d==null?null:d.estado()),
                List.of("A prova não altera disponibilidade, cadastro mestre ou fotografias anteriores do motivo."));
    }
    private String intencao(Long id,RegistroProvaMotivoHistoricoRequisicao r,UsuarioAplicacao u) {
        return hash("sgt.prova-motivo.intencao.v1",id,r.contratoId(),u.getId(),r.descricaoComprovada().trim(),r.categoriaComprovada(),r.dataInicioEfetiva(),r.dataFimEfetiva(),normalizar(r.referenciaSei()),r.documentoComprobatorioId(),normalizar(r.finalidadeAcessoDocumento()),r.motivo().trim(),r.autorizacao().trim(),r.provaAnteriorId());
    }
    private Map<String,Object> dto(ProvaMotivoDesligamentoHistorico p,UsuarioAplicacao u,boolean reenvio) {
        boolean completo=minimizador.completo(minimizador.nivelPadrao(u)); Map<String,Object>d=new LinkedHashMap<>();
        d.put("id",p.getId()); d.put("motivoDesligamentoId",p.getMotivoDesligamento().getId()); d.put("contratoId",p.getContrato().getId());
        d.put("descricaoComprovada",p.getDescricaoComprovada()); d.put("categoriaComprovada",p.getCategoriaComprovada());
        d.put("dataInicioEfetiva",p.getDataInicioEfetiva()); d.put("dataFimEfetiva",p.getDataFimEfetiva()); d.put("registradoEm",p.getRegistradoEm());
        d.put("responsavelId",p.getResponsavelId()); d.put("responsavelUsername",completo?p.getResponsavelUsername():null);
        d.put("provaAnteriorId",p.getProvaAnterior()==null?null:p.getProvaAnterior().getId()); d.put("relacao",p.getProvaAnterior()==null?"ORIGEM":"RETIFICA");
        d.put("motivo",completo?p.getMotivo():null); d.put("autorizacao",completo?p.getAutorizacao():null); d.put("referenciaSei",completo?p.getReferenciaSei():null);
        d.put("documentoComprobatorioId",completo&&p.getDocumentoComprobatorio()!=null?p.getDocumentoComprobatorio().getId():null); d.put("reenvio",reenvio);return d;
    }
}
