package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.dto.PosicaoAlocacaoTemporalDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.AlocacaoVinculoRepositorio;
import com.example.siggtrat.repositorio.ContratoRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** A consulta operacional acompanha as faixas canônicas autorizadas, inclusive históricas. */
@Service
@Transactional(readOnly=true)
public class EscopoLeituraPessoaVinculoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(EscopoLeituraPessoaVinculoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final AcessoOrganizacionalServico acesso;
    private final EscopoAcessoUsuarioServico escopos;
    private final AlocacaoVinculoRepositorio alocacoes;
    private final MotorTemporalAlocacaoServico motor;
    private ContratoRepositorio contratos;

    public EscopoLeituraPessoaVinculoServico(AcessoOrganizacionalServico acesso, EscopoAcessoUsuarioServico escopos,
            AlocacaoVinculoRepositorio alocacoes, MotorTemporalAlocacaoServico motor) {
        this.acesso=acesso; this.escopos=escopos; this.alocacoes=alocacoes; this.motor=motor;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarContratosTemporais(ContratoRepositorio contratos) { this.contratos=contratos; }

    public boolean operacional(UsuarioAplicacao usuario) { return acesso.ehOperacional(usuario); }

    public boolean permiteVinculo(UsuarioAplicacao usuario,VinculoContratual vinculo,LocalDate data,Instant conhecidoAte) {
        if(!operacional(usuario))return true;
        if(!acesso.podeConsultarEmpregados(usuario))return false;
        if(data!=null)return permitePosicao(usuario,motor.reconstruir(vinculo,data,conhecidoAte));
        return alocacoes.findByVinculoIdOrderByDataInicioAscIdAsc(vinculo.getId()).stream()
                .anyMatch(a->permiteAlocacao(usuario,a));
    }

    public void exigirVinculo(UsuarioAplicacao usuario,VinculoContratual vinculo,LocalDate data,Instant conhecidoAte) {
        if(!permiteVinculo(usuario,vinculo,data,conhecidoAte))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Vínculo não encontrado na Lotação/Turno do seu escopo.");
    }

    /** Uma fonte integral não expõe datas externas ao recorte autorizado. */
    public boolean permiteIntervalo(UsuarioAplicacao usuario, VinculoContratual vinculo,
                                    LocalDate inicio, LocalDate fim, Instant conhecidoAte) {
        if (!operacional(usuario)) return true;
        if (inicio == null || fim != null && fim.isBefore(inicio)) return false;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        SortedSet<LocalDate> fronteiras = new TreeSet<>();
        fronteiras.add(inicio);
        if (fim != null) fronteiras.add(fim);
        for (var faixa : motor.reconstruirFaixas(vinculo, conhecimento)) {
            if (faixa.dataInicio() != null && !faixa.dataInicio().isBefore(inicio)
                    && (fim == null || !faixa.dataInicio().isAfter(fim))) fronteiras.add(faixa.dataInicio());
            if (faixa.dataFim() != null && !faixa.dataFim().equals(LocalDate.MAX)) {
                LocalDate depois = faixa.dataFim().plusDays(1);
                if (!depois.isBefore(inicio) && (fim == null || !depois.isAfter(fim))) fronteiras.add(depois);
            }
        }
        return fronteiras.stream().allMatch(d -> permiteVinculo(usuario, vinculo, d, conhecimento));
    }

    public boolean permiteAlocacao(UsuarioAplicacao usuario,AlocacaoVinculo alocacao) {
        return !operacional(usuario) || alocacao!=null && alocacao.getLotacao()!=null && alocacao.getJornada()!=null
                && escopos.podeGerenciar(usuario,alocacao.getLotacao().getId(),alocacao.getJornada().getId());
    }

    public boolean permiteCombinacao(UsuarioAplicacao usuario,Long lotacaoId,Long jornadaId) {
        return !operacional(usuario) || lotacaoId!=null && jornadaId!=null && escopos.podeGerenciar(usuario,lotacaoId,jornadaId);
    }

    public boolean permitePosicao(UsuarioAplicacao usuario,PosicaoAlocacaoTemporalDTO posicao) {
        return !operacional(usuario) || posicao!=null && permiteCombinacao(usuario,posicao.lotacaoId(),posicao.jornadaId());
    }

    public boolean permiteEvento(UsuarioAplicacao usuario,EventoAlocacaoVinculo evento) {
        return !operacional(usuario) || permiteCombinacao(usuario,evento.getLotacaoNovaId(),evento.getJornadaNovaId())
                || permiteCombinacao(usuario,evento.getLotacaoAnteriorId(),evento.getJornadaAnteriorId());
    }

    public Specification<Pessoa> pessoas(UsuarioAplicacao usuario,Long contratoId,LocalDate ativoEm,String busca,String cpf) {
        List<EscopoAcessoUsuarioServico.Escopo> permitidos=operacional(usuario)?escopos.obterEscopos(usuario):List.of();
        Set<Long> contratosPermitidos=contratosDoEscopoAtual(usuario);
        return (p,q,cb)->{
            Subquery<Long> sq=q.subquery(Long.class); Root<VinculoContratual> v=sq.from(VinculoContratual.class);
            List<Predicate> filtros=new ArrayList<>(); filtros.add(cb.equal(v.get("pessoa"),p));
            if(contratoId!=null)filtros.add(cb.equal(v.get("contrato").get("id"),contratoId));
            periodoAtivo(filtros,cb,v,ativoEm);
            filtros.add(recorteVinculo(usuario,sq,cb,v,permitidos,contratosPermitidos,ativoEm)); sq.select(v.get("id")).where(filtros.toArray(Predicate[]::new));
            List<Predicate> pessoa=new ArrayList<>(); pessoa.add(cb.exists(sq));
            if(busca!=null) {
                List<Predicate> termos=new ArrayList<>(); termos.add(cb.like(cb.lower(p.get("nome")),"%"+busca+"%"));
                if(cpf!=null)termos.add(cb.like(p.get("cpf"),"%"+cpf+"%")); pessoa.add(cb.or(termos.toArray(Predicate[]::new)));
            }
            return cb.and(pessoa.toArray(Predicate[]::new));
        };
    }

    public Specification<VinculoContratual> vinculos(UsuarioAplicacao usuario,Long contratoId,Long pessoaId,
            EstadoNegocialVinculo estado,LocalDate inicioDe,LocalDate inicioAte,LocalDate fimDe,LocalDate fimAte,
            LocalDate ativoEm,String busca,String cpf) {
        List<EscopoAcessoUsuarioServico.Escopo> permitidos=operacional(usuario)?escopos.obterEscopos(usuario):List.of();
        Set<Long> contratosPermitidos=contratosDoEscopoAtual(usuario);
        return (v,q,cb)->{
            List<Predicate> filtros=new ArrayList<>(); filtros.add(recorteVinculo(usuario,q,cb,v,permitidos,contratosPermitidos,ativoEm));
            if(contratoId!=null)filtros.add(cb.equal(v.get("contrato").get("id"),contratoId));
            if(pessoaId!=null)filtros.add(cb.equal(v.get("pessoa").get("id"),pessoaId));
            if(estado!=null)filtros.add(cb.equal(v.get("estadoNegocial"),estado));
            if(inicioDe!=null)filtros.add(cb.greaterThanOrEqualTo(v.get("dataInicio"),inicioDe));
            if(inicioAte!=null)filtros.add(cb.lessThanOrEqualTo(v.get("dataInicio"),inicioAte));
            if(fimDe!=null)filtros.add(cb.greaterThanOrEqualTo(v.get("dataFim"),fimDe));
            if(fimAte!=null)filtros.add(cb.lessThanOrEqualTo(v.get("dataFim"),fimAte));
            periodoAtivo(filtros,cb,v,ativoEm);
            if(busca!=null) {
                List<Predicate> termos=new ArrayList<>(); termos.add(cb.like(cb.lower(v.get("pessoa").get("nome")),"%"+busca+"%"));
                termos.add(cb.like(cb.lower(v.get("matricula")),"%"+busca+"%"));
                if(cpf!=null)termos.add(cb.like(v.get("pessoa").get("cpf"),"%"+cpf+"%")); filtros.add(cb.or(termos.toArray(Predicate[]::new)));
            }
            return cb.and(filtros.toArray(Predicate[]::new));
        };
    }

    /** Resolve responsabilidade corrente antes de paginar, mantendo lista e contagem idênticas. */
    private Set<Long> contratosDoEscopoAtual(UsuarioAplicacao usuario) {
        if(operacional(usuario)||acesso.ehGlobal(usuario)||acesso.ehPresidente(usuario))return Set.of();
        if(contratos==null)return Set.of();
        List<Contrato> candidatos;
        if(acesso.ehGestorEstadual(usuario)&&acesso.superintendenciaEfetiva(usuario)!=null) {
            candidatos=contratos.buscarCandidatosSuperintendenciaTemporal(acesso.superintendenciaEfetiva(usuario).getId(),RelogioSistema.hoje());
        } else if(acesso.ehGestaoLocal(usuario)&&acesso.gestaoLocalEfetiva(usuario)!=null) {
            candidatos=contratos.buscarCandidatosGestaoTemporal(acesso.gestaoLocalEfetiva(usuario).getId(),RelogioSistema.hoje());
        } else return Set.of();
        Set<Long> ids=new LinkedHashSet<>();
        for(Contrato contrato:candidatos)if(acesso.podeAcessarContrato(usuario,contrato))ids.add(contrato.getId());
        return Set.copyOf(ids);
    }

    private Predicate recorteVinculo(UsuarioAplicacao usuario,AbstractQuery<?> query,CriteriaBuilder cb,
            Root<VinculoContratual> vinculo,List<EscopoAcessoUsuarioServico.Escopo> permitidos,
            Set<Long> contratosPermitidos,LocalDate data) {
        if(operacional(usuario))return existeAlocacao(query,cb,vinculo,permitidos,data);
        if(acesso.ehGlobal(usuario)||acesso.ehPresidente(usuario))return cb.conjunction();
        return contratosPermitidos.isEmpty()?cb.disjunction():vinculo.get("contrato").get("id").in(contratosPermitidos);
    }

    private Predicate existeAlocacao(AbstractQuery<?> query,CriteriaBuilder cb,Root<VinculoContratual> vinculo,
            List<EscopoAcessoUsuarioServico.Escopo> permitidos,LocalDate data) {
        Subquery<Long> sq=query.subquery(Long.class); Root<AlocacaoVinculo> a=sq.from(AlocacaoVinculo.class);
        List<Predicate> filtros=new ArrayList<>(); filtros.add(cb.equal(a.get("vinculo"),vinculo));
        if(data!=null) {
            filtros.add(cb.equal(a.get("estadoNegocial"),EstadoNegocialAlocacao.REGISTRADA));
            filtros.add(cb.lessThanOrEqualTo(a.get("dataInicio"),data));
            filtros.add(cb.or(cb.isNull(a.get("dataFim")),cb.greaterThanOrEqualTo(a.get("dataFim"),data)));
        }
        List<Predicate> recortes=new ArrayList<>();
        for(var escopo:permitidos) {
            List<Predicate> partes=new ArrayList<>();
            if(escopo.gestaoLocalId()!=null)partes.add(cb.equal(a.get("lotacao").get("gestaoLocal").get("id"),escopo.gestaoLocalId()));
            if(escopo.lotacaoId()!=null)partes.add(cb.equal(a.get("lotacao").get("id"),escopo.lotacaoId()));
            if(escopo.jornadaId()!=null)partes.add(cb.equal(a.get("jornada").get("id"),escopo.jornadaId()));
            recortes.add(cb.and(partes.toArray(Predicate[]::new)));
        }
        filtros.add(cb.or(recortes.toArray(Predicate[]::new))); sq.select(a.get("id")).where(filtros.toArray(Predicate[]::new));
        return cb.exists(sq);
    }
    private void periodoAtivo(List<Predicate> filtros,CriteriaBuilder cb,Root<VinculoContratual> v,LocalDate data) {
        if(data==null)return;
        filtros.add(cb.equal(v.get("estadoNegocial"),EstadoNegocialVinculo.REGISTRADO));
        filtros.add(cb.lessThanOrEqualTo(v.get("dataInicio"),data));
        filtros.add(cb.or(cb.isNull(v.get("dataFim")),cb.greaterThanOrEqualTo(v.get("dataFim"),data)));
    }
}
