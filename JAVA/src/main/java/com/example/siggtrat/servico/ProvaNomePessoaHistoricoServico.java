package com.example.siggtrat.servico;

import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
import static com.example.siggtrat.servico.SuporteProvaHistoricaServico.*;

@Service
@Transactional(readOnly = true)
public class ProvaNomePessoaHistoricoServico {
    private final EntityManager em;
    private final ProvaNomePessoaHistoricoRepositorio provas;
    private final SuporteProvaHistoricaServico suporte;
    private final IdentidadeHistoricaServico identidade;
    private final AcessoOrganizacionalServico acesso;
    private final MinimizadorDadosPessoaisServico minimizador;
    private final InvalidacaoCacheServico cache;
    private final InvalidacaoEmissaoRelatorioServico relatorios;
    private final EscopoLeituraPessoaVinculoServico escopo;
    public ProvaNomePessoaHistoricoServico(EntityManager em, ProvaNomePessoaHistoricoRepositorio provas,
            SuporteProvaHistoricaServico suporte, IdentidadeHistoricaServico identidade,
            AcessoOrganizacionalServico acesso, MinimizadorDadosPessoaisServico minimizador,
            InvalidacaoCacheServico cache, InvalidacaoEmissaoRelatorioServico relatorios,
            EscopoLeituraPessoaVinculoServico escopo) {
        this.em=em; this.provas=provas; this.suporte=suporte; this.identidade=identidade;
        this.acesso=acesso; this.minimizador=minimizador; this.cache=cache; this.relatorios=relatorios; this.escopo=escopo;
    }
    // A prévia pode registrar acesso a documento restrito, portanto não é transação readOnly.
    @Transactional
    public PreviaProvaHistoricaDTO previa(Long pessoaId, RegistroProvaNomeHistoricoRequisicao r) {
        UsuarioAplicacao u = suporte.executor();
        VinculoContratual v = alvo(pessoaId, r == null ? null : r.vinculoId());
        suporte.exigirCustodia(u,v.getContrato()); validar(r);
        var doc = suporte.fonte(v.getContrato(),r.documentoComprobatorioId(),r.referenciaSei(),r.finalidadeAcessoDocumento(),false);
        return preparar(v,r,u,doc);
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String,Object> registrar(Long pessoaId, RegistroProvaNomeHistoricoRequisicao r) {
        UsuarioAplicacao u = suporte.executor();
        VinculoContratual v = alvo(pessoaId, r == null ? null : r.vinculoId());
        Contrato c = suporte.contrato(v.getContrato().getId(),true);
        em.refresh(v.getPessoa(),LockModeType.PESSIMISTIC_WRITE);
        em.refresh(v,LockModeType.PESSIMISTIC_WRITE);
        suporte.exigirCustodia(u,c); validar(r);
        String intencao = intencao(pessoaId,r,u);
        var existente = provas.findByContratoIdAndResponsavelIdAndChaveIdempotencia(c.getId(),u.getId(),r.chaveIdempotencia().trim()).orElse(null);
        if (existente != null) {
            if (!Objects.equals(existente.getHashIntencao(),intencao)) throw erro(HttpStatus.CONFLICT,"Chave já usada para outra intenção.");
            return dto(existente,u,true);
        }
        var doc = suporte.fonte(c,r.documentoComprobatorioId(),r.referenciaSei(),r.finalidadeAcessoDocumento(),true);
        var previa = preparar(v,r,u,doc);
        if (!Boolean.TRUE.equals(r.confirmarImpacto()) || !Objects.equals(r.tokenPrevia(),previa.tokenPrevia()))
            throw erro(HttpStatus.CONFLICT,"Prévia não confirmada ou obsoleta. Refaça a análise.");
        ProvaNomePessoaHistorico p = new ProvaNomePessoaHistorico();
        suporte.preencher(p,c,r.dataInicioEfetiva(),r.dataFimEfetiva(),doc,r.referenciaSei(),r.motivo(),r.autorizacao(),r.chaveIdempotencia(),intencao,u);
        p.setPessoa(v.getPessoa()); p.setVinculo(v); p.setNomeComprovado(nome(r));
        p.setProvaAnterior(anterior(v,r.provaAnteriorId()));
        p.setEventoOrigemRetificado(evento(v,r.eventoOrigemRetificadoId()));
        provas.saveAndFlush(p);
        suporte.registrarUso(u,c,"PROVA_NOME_HISTORICO",p.getId(),r.motivo());
        em.lock(v.getPessoa(),LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        em.lock(v,LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        relatorios.invalidarPorVinculo(v,r.dataInicioEfetiva(),r.dataFimEfetiva(),"Incorporação de prova nominal histórica",u);
        cache.invalidarCachesDeNegocioAposCommit();
        return dto(p,u,false);
    }
    public IdentidadeHistoricaResolvidaDTO consultar(Long pessoaId, Long vinculoId, LocalDate data, Instant k) {
        VinculoContratual v = alvo(pessoaId,vinculoId); UsuarioAplicacao u=acesso.usuarioAtual();
        exigirLeitura(u,v,data,k);
        var r = identidade.resolver(v.getPessoa(),v,data,k);
        return new IdentidadeHistoricaResolvidaDTO(minimizador.nome(r.nome(),minimizador.nivelPadrao(u)),r.comprovado(),
                r.tipoFonte(),r.fonteId(),r.fonteRegistradaEm(),r.inicioComprovado(),r.fimComprovado());
    }
    public List<Map<String,Object>> listar(Long pessoaId, Long vinculoId) {
        UsuarioAplicacao u=acesso.usuarioAtual();
        List<VinculoContratual> vs = vinculoId == null ? em.createQuery("select v from VinculoContratual v where v.pessoa.id=:p order by v.id",VinculoContratual.class)
                .setParameter("p",pessoaId).getResultList() : List.of(alvo(pessoaId,vinculoId));
        List<Map<String,Object>> out = new ArrayList<>(); boolean permitido=false;
        for (var v:vs) {
            if (!acesso.podeAcessarContrato(u,v.getContrato()) || acesso.ehOperacional(u) && !escopo.permiteVinculo(u,v,null,null)) continue;
            permitido=true;
            for (var p:trilha(v)) {
                if (escopo.permiteIntervalo(u,v,p.getDataInicioEfetiva(),p.getDataFimEfetiva(),null))
                    out.add(dto(p,u,false));
            }
        }
        if (!permitido) throw erro(HttpStatus.NOT_FOUND,"Pessoa não encontrada no seu escopo.");
        return out;
    }
    private void exigirLeitura(UsuarioAplicacao u,VinculoContratual v,LocalDate data,Instant k) {
        if (acesso.ehOperacional(u)) escopo.exigirVinculo(u,v,data,k); else acesso.exigirAcessoContrato(u,v.getContrato());
    }
    private VinculoContratual alvo(Long pessoaId,Long vinculoId) {
        if (vinculoId == null) throw erro(HttpStatus.BAD_REQUEST,"Vínculo é obrigatório.");
        VinculoContratual v=em.find(VinculoContratual.class,vinculoId);
        if (v==null || v.getPessoa()==null || !Objects.equals(pessoaId,v.getPessoa().getId()))
            throw erro(HttpStatus.NOT_FOUND,"Pessoa/Vínculo não encontrado.");
        return v;
    }
    private List<ProvaNomePessoaHistorico> trilha(VinculoContratual v) {
        return provas.findByPessoaIdAndVinculoIdOrderByRegistradoEmAscIdAsc(v.getPessoa().getId(),v.getId());
    }
    private void validar(RegistroProvaNomeHistoricoRequisicao r) {
        if (r==null) throw erro(HttpStatus.BAD_REQUEST,"Requisição é obrigatória.");
        suporte.validar(r.dataInicioEfetiva(),r.dataFimEfetiva(),r.motivo(),r.autorizacao(),r.chaveIdempotencia()); nome(r);
        if (r.versaoPessoaEsperada()==null || r.versaoVinculoEsperada()==null || r.versaoPessoaEsperada()<0 || r.versaoVinculoEsperada()<0)
            throw erro(HttpStatus.BAD_REQUEST,"Versões esperadas são obrigatórias.");
    }
    private String nome(RegistroProvaNomeHistoricoRequisicao r) { return texto(r.nomeComprovado(),180,"Nome comprovado é obrigatório.").replaceAll("\\s+"," "); }
    private ProvaNomePessoaHistorico anterior(VinculoContratual v,Long id) {
        if(id==null)return null;
        var p=provas.findById(id).orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Prova anterior não encontrada."));
        if(!Objects.equals(p.getVinculo().getId(),v.getId()) || !Objects.equals(p.getPessoa().getId(),v.getPessoa().getId()))
            throw erro(HttpStatus.NOT_FOUND,"Prova anterior não encontrada no vínculo.");
        return p;
    }
    private EventoVinculoContratual evento(VinculoContratual v,Long id) {
        if(id==null)return null;
        var e=em.find(EventoVinculoContratual.class,id);
        if(e==null || !Objects.equals(e.getVinculo().getId(),v.getId())) throw erro(HttpStatus.NOT_FOUND,"Evento de origem não encontrado no vínculo.");
        return e;
    }
    private PreviaProvaHistoricaDTO preparar(VinculoContratual v,RegistroProvaNomeHistoricoRequisicao r,UsuarioAplicacao u,DocumentoComprobatorioDTO doc) {
        if (v.getPessoa().getVersion()!=r.versaoPessoaEsperada() || v.getVersion()!=r.versaoVinculoEsperada())
            throw erro(HttpStatus.CONFLICT,"Pessoa ou Vínculo mudou. Refaça a prévia.");
        var anterior=anterior(v,r.provaAnteriorId()); var origem=evento(v,r.eventoOrigemRetificadoId());
        var trilha=trilha(v);
        if(anterior!=null && trilha.stream().anyMatch(p->IdentidadeHistoricaServico.descendeDe(p,anterior.getId())))
            throw erro(HttpStatus.CONFLICT,"A prova anterior já foi retificada. Indique a prova vigente.");
        for(var p:trilha) {
            if(!sobrepoe(p,r))continue;
            boolean substituida=trilha.stream().anyMatch(n->IdentidadeHistoricaServico.descendeDe(n,p.getId()) && cobreIntersecao(n,p,r));
            if(substituida)continue;
            boolean relacionada=anterior!=null && (Objects.equals(anterior.getId(),p.getId()) || IdentidadeHistoricaServico.descendeDe(anterior,p.getId()));
            if(!nome(r).equals(p.getNomeComprovado()) && !relacionada)
                throw erro(HttpStatus.CONFLICT,"Provas nominais conflitantes exigem provaAnteriorId explícito.");
        }
        // Verificar todas as fronteiras das fontes, inclusive fotos pontuais dentro do intervalo.
        var fontes=em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:v",EventoVinculoContratual.class).setParameter("v",v.getId()).getResultList();
        for(var e:fontes) {
            LocalDate ini=e.getTipoEvento()==com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual.CRIACAO && e.getInicioNovo()!=null ? e.getInicioNovo():e.getDataEfetiva();
            LocalDate fim=e.getTipoEvento()==com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual.CRIACAO?e.getFimNovo():e.getDataEfetiva();
            if(e.getPessoaNomeFotografia()==null || e.getPessoaNomeFotografia().isBlank() || !intersecta(ini,fim,r.dataInicioEfetiva(),r.dataFimEfetiva()) || nome(r).equals(e.getPessoaNomeFotografia()))continue;
            boolean explicita=origem!=null && (Objects.equals(origem.getId(),e.getId())
                    || IdentidadeHistoricaServico.eventoDescendeDe(e,origem.getId()) && Objects.equals(origem.getPessoaNomeFotografia(),e.getPessoaNomeFotografia()));
            if (!explicita && anterior != null) explicita=IdentidadeHistoricaServico.retificaEvento(anterior,e);
            if(!explicita)throw erro(HttpStatus.CONFLICT,"Foto nominal conflitante exige eventoOrigemRetificadoId explícito.");
        }
        String dependencias=trilha.stream().map(p->p.getId()+":"+p.getHashConteudo()).toList().toString();
        String eventos=fontes.stream().map(e->e.getId()+":"+e.getHashEvento()).toList().toString();
        return new PreviaProvaHistoricaDTO(hash(intencao(v.getPessoa().getId(),r,u),v.getPessoa().getVersion(),v.getVersion(),v.getContrato().getVersion(),dependencias,eventos,
                doc==null?null:doc.id(),doc==null?null:doc.version(),doc==null?null:doc.estado()),
                List.of("Enriquece a identidade no intervalo documentado; preserva cadastro, fotografias e conhecimento anteriores.","Emissões atingidas serão marcadas para reemissão."));
    }
    private boolean cobreIntersecao(ProvaNomePessoaHistorico n,ProvaNomePessoaHistorico p,RegistroProvaNomeHistoricoRequisicao r) {
        LocalDate ini=p.getDataInicioEfetiva().isAfter(r.dataInicioEfetiva())?p.getDataInicioEfetiva():r.dataInicioEfetiva();
        LocalDate fim=min(p.getDataFimEfetiva(),r.dataFimEfetiva());
        return !n.getDataInicioEfetiva().isAfter(ini) && (n.getDataFimEfetiva()==null || fim!=null && !n.getDataFimEfetiva().isBefore(fim));
    }
    static LocalDate min(LocalDate a,LocalDate b) { return a==null?b:b==null?a:a.isBefore(b)?a:b; }
    static boolean intersecta(LocalDate a,LocalDate b,LocalDate c,LocalDate d) { return (b==null || !c.isAfter(b)) && (d==null || !a.isAfter(d)); }
    private boolean sobrepoe(ProvaNomePessoaHistorico p,RegistroProvaNomeHistoricoRequisicao r) { return intersecta(p.getDataInicioEfetiva(),p.getDataFimEfetiva(),r.dataInicioEfetiva(),r.dataFimEfetiva()); }
    private String intencao(Long pessoaId,RegistroProvaNomeHistoricoRequisicao r,UsuarioAplicacao u) {
        return hash("nome-v1",pessoaId,r.vinculoId(),u.getId(),nome(r),r.dataInicioEfetiva(),r.dataFimEfetiva(),normalizar(r.referenciaSei()),r.documentoComprobatorioId(),normalizar(r.finalidadeAcessoDocumento()),r.motivo().trim(),r.autorizacao().trim(),r.provaAnteriorId(),r.eventoOrigemRetificadoId());
    }
    private Map<String,Object> dto(ProvaNomePessoaHistorico p,UsuarioAplicacao u,boolean reenvio) {
        var nivel=minimizador.nivelPadrao(u); boolean completo=minimizador.completo(nivel);
        Map<String,Object> d=new LinkedHashMap<>();
        d.put("id",p.getId()); d.put("pessoaId",p.getPessoa().getId()); d.put("vinculoId",p.getVinculo().getId()); d.put("contratoId",p.getContrato().getId());
        d.put("nomeComprovado",minimizador.nome(p.getNomeComprovado(),nivel)); d.put("dataInicioEfetiva",p.getDataInicioEfetiva()); d.put("dataFimEfetiva",p.getDataFimEfetiva());
        d.put("registradoEm",p.getRegistradoEm()); d.put("responsavelId",p.getResponsavelId()); d.put("responsavelUsername",completo?p.getResponsavelUsername():null);
        d.put("provaAnteriorId",p.getProvaAnterior()==null?null:p.getProvaAnterior().getId()); d.put("eventoOrigemRetificadoId",p.getEventoOrigemRetificado()==null?null:p.getEventoOrigemRetificado().getId());
        d.put("relacao",p.getProvaAnterior()!=null || p.getEventoOrigemRetificado()!=null?"RETIFICA":"ORIGEM");
        d.put("motivo",completo?p.getMotivo():null); d.put("autorizacao",completo?p.getAutorizacao():null); d.put("referenciaSei",completo?p.getReferenciaSei():null);
        d.put("documentoComprobatorioId",completo && p.getDocumentoComprobatorio()!=null?p.getDocumentoComprobatorio().getId():null); d.put("reenvio",reenvio);
        return d;
    }
}
