package com.example.siggtrat.configuracao.demonstracao;

import com.example.siggtrat.configuracao.InicializadorAdministradorPadrao;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia;
import com.example.siggtrat.dominio.frequencia.TipoEventoFrequencia;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.*;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;

/** Corpus fictício dos indicadores. Nunca executa automaticamente no banco normal. */
@Service
public class PopuladorDadosRj implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final Logger LOG=LoggerFactory.getLogger(PopuladorDadosRj.class);
    @Override public Logger loggerDiagnostico() { return LOG; }
    public static final String MARCADOR="DEMO_RJ_DADOS_V1";
    private static final ZoneId ZONA=ZoneId.of("America/Sao_Paulo");
    private static final String NATUREZA="DEMONSTRAÇÃO FICTÍCIA SE/RJ; sem valor administrativo";
    private final EntityManager em;
    private final TransactionTemplate transacao;
    private final CalculoFrequenciaServico calculo;
    private final AuditoriaFrequenciaServico auditoria;
    private final PoliticaTemporalFrequenciaServico politica;
    private final PasswordEncoder encoder;
    public PopuladorDadosRj(EntityManager em,PlatformTransactionManager tm,CalculoFrequenciaServico calculo,
                            AuditoriaFrequenciaServico auditoria,PoliticaTemporalFrequenciaServico politica,PasswordEncoder encoder) {
        this.em=em;this.transacao=new TransactionTemplate(tm);this.calculo=calculo;this.auditoria=auditoria;this.politica=politica;this.encoder=encoder;
    }
    public record Resultado(LocalDate referencia,long contratos,long empregados,long frequencias,boolean criada) {}
    private record Unidade(GestaoLocal gestao,Lotacao lotacao,Jornada jornada,Cargo cargo,Contrato contrato,
                           ComposicaoContratual composicao,UsuarioAplicacao gestor,int indice) {}
    private record Trabalhador(Unidade unidade,Pessoa pessoa,VinculoContratual vinculo,AlocacaoVinculo alocacao,Empregado empregado,int indice) {}

    public Resultado popular(LocalDate referencia,String senhaInicial) {
        if(referencia==null || referencia.isAfter(LocalDate.now(ZONA)))throw new IllegalArgumentException("SGT_DEMO_RJ_REFERENCIA_INVALIDA");
        return transacao.execute(tx->{
            em.createNativeQuery("select 1 from pg_advisory_xact_lock(73129670670003)").getSingleResult();
            ConfiguracaoSistema marcador=em.find(ConfiguracaoSistema.class,MARCADOR);
            if(marcador!=null) {
                Resultado existente=resultado(LocalDate.parse(marcador.obterValor()),false);
                LOG.info("SGT_DEMO_RJ_EXISTENTE empregados={} frequencias={} dadosPreservados=true",existente.empregados(),existente.frequencias());
                return existente;
            }
            for(String entidade:List.of("Pessoa","Empregado","VinculoContratual","Contrato","Frequencia"))
                if(em.createQuery("select count(e) from "+entidade+" e",Long.class).getSingleResult()!=0)
                    throw new IllegalStateException("SGT_DEMO_RJ_DESTINO_NAO_VAZIO: use um banco exclusivo de demonstração");
            InicializadorAdministradorPadrao.validarSenha(senhaInicial,"APP_DEMO_RJ_INITIAL_PASSWORD");
            LocalDate inicio=LocalDate.of(referencia.getYear()-1,1,1);
            var se=se();var status=status();String hashSenha=encoder.encode(senhaInicial);
            usuario("demo_rj_estadual","99000701",PerfilAcesso.GESTOR_ESTADUAL,se,null,hashSenha);
            List<Trabalhador> trabalhadores=new ArrayList<>();
            String[] siglas={"GTRAT","GEDIS","GETRA"};
            for(int g=0;g<siglas.length;g++) {
                GestaoLocal gestao=gestao(se,siglas[g]);
                var gestor=usuario("demo_rj_"+siglas[g].toLowerCase(Locale.ROOT),"9900070"+(g+2),PerfilAcesso.GESTOR_LOCAL,null,gestao,hashSenha);
                for(int n=0;n<2;n++) {
                    Unidade unidade=unidade(gestao,gestor,g*2+n,inicio,referencia);
                    for(int e=0;e<9;e++)trabalhadores.add(trabalhador(unidade,e,inicio,status));
                }
            }
            em.flush();int registros=0;
            for(LocalDate data=inicio;!data.isAfter(referencia);data=data.plusDays(1)) {
                boolean recente=!data.isBefore(referencia.minusDays(90));
                if(!recente && data.getDayOfWeek()!=DayOfWeek.MONDAY && data.getDayOfWeek()!=DayOfWeek.THURSDAY)continue;
                for(Trabalhador t:trabalhadores) {
                    var dia=dia(t.unidade().jornada(),data);
                    if(!Boolean.TRUE.equals(dia.getDiaTrabalho()))continue;
                    // Pendência de lançamento continua sendo ausência de registro, nunca FALTA.
                    if(t.indice()==8 && !data.isBefore(referencia.minusDays(1)))continue;
                    frequencia(t,data,dia,referencia);
                    if(++registros%25==0){em.flush();em.clear();}
                }
            }
            em.persist(new ConfiguracaoSistema(MARCADOR,referencia.toString(),NATUREZA));em.flush();
            Resultado resultado=resultado(referencia,true);
            LOG.info("SGT_DEMO_RJ_CRIADA contratos={} empregados={} frequencias={} referencia={} natureza=FICTICIA",
                    resultado.contratos(),resultado.empregados(),resultado.frequencias(),referencia);
            return resultado;
        });
    }
    private Resultado resultado(LocalDate ref,boolean criada) {
        long contratos=em.createQuery("select count(c) from Contrato c where c.numeroContrato like 'DEMO-RJ-%'",Long.class).getSingleResult();
        long empregados=em.createQuery("select count(e) from Empregado e where e.matricula like 'DEMO-RJ-%'",Long.class).getSingleResult();
        long frequencias=em.createQuery("select count(f) from Frequencia f where f.empregado.matricula like 'DEMO-RJ-%'",Long.class).getSingleResult();
        return new Resultado(ref,contratos,empregados,frequencias,criada);
    }
    private SuperintendenciaEstadual se() {
        return em.createQuery("select s from SuperintendenciaEstadual s where s.uf='RJ'",SuperintendenciaEstadual.class)
                .getResultStream().findFirst().orElseGet(()->{var s=new SuperintendenciaEstadual("SE/RJ","RJ");em.persist(s);return s;});
    }
    private StatusEmpregado status() {
        return em.createQuery("select s from StatusEmpregado s where s.descricao='ATIVO'",StatusEmpregado.class)
                .getResultStream().findFirst().orElseGet(()->{var s=new StatusEmpregado("ATIVO");em.persist(s);return s;});
    }
    private GestaoLocal gestao(SuperintendenciaEstadual se,String sigla) {
        return em.createQuery("select g from GestaoLocal g where g.superintendencia=:se and g.codigo=:codigo",GestaoLocal.class)
                .setParameter("se",se).setParameter("codigo",sigla+"-RJ").getResultStream().findFirst().orElseGet(()->{
                    var g=new GestaoLocal();g.setSuperintendencia(se);g.setNome(sigla+"/RJ");g.setCodigo(sigla+"-RJ");em.persist(g);return g;
                });
    }
    private UsuarioAplicacao usuario(String nome,String matricula,PerfilAcesso perfil,SuperintendenciaEstadual se,GestaoLocal gestao,String hash) {
        if(em.createQuery("select count(u) from UsuarioAplicacao u where u.username=:nome or u.matricula=:matricula",Long.class)
                .setParameter("nome",nome).setParameter("matricula",matricula).getSingleResult()!=0)
            throw new IllegalStateException("SGT_DEMO_RJ_USUARIO_OCUPADO: nenhuma conta foi modificada");
        var u=new UsuarioAplicacao();u.setUsername(nome);u.setMatricula(matricula);u.setEmail(nome+"@example.test");u.setPerfil(perfil);
        u.setSuperintendencia(se);u.setGestaoLocal(gestao);u.setPassword(hash);u.setActive(true);u.setForceChangePassword(true);u.setTwoFactorEnabled(false);em.persist(u);return u;
    }
    private Unidade unidade(GestaoLocal gestao,UsuarioAplicacao gestor,int i,LocalDate inicio,LocalDate referencia) {
        String[] cidades={"Rio de Janeiro","Duque de Caxias","Niterói","São Gonçalo","Nova Iguaçu","Petrópolis"};
        var l=new Lotacao("DEMO-RJ · Unidade "+(i+1));l.setGestaoLocal(gestao);l.setMunicipio(cidades[i]);em.persist(l);
        var j=new Jornada();j.setDescricao("DEMO-RJ · Turno "+(i+1));j.setGestaoLocal(gestao);em.persist(j);
        // Escalas 5x2 deslocadas: há unidades trabalhando em cada dia civil.
        for(DayOfWeek semana:DayOfWeek.values()) {
            var d=new JornadaDiaSemana();d.setJornada(j);d.setDiaSemana(semana);
            boolean trabalho=Math.floorMod(semana.getValue()-1-i/2,7)<5;d.setDiaTrabalho(trabalho);
            if(trabalho){d.setEntrada1("08:00");d.setSaida1("12:00");d.setEntrada2("13:00");d.setSaida2("17:00");}
            j.getDias().add(d);em.persist(d);
        }
        l.getTurnos().add(j);
        var cargo=new Cargo("DEMO-RJ · Apoio operacional "+(i+1),"411005");cargo.setGestaoLocal(gestao);em.persist(cargo);
        var tipo=new TipoContratacao();tipo.setGestaoLocal(gestao);tipo.setDescricao("DEMO-RJ · Apoio "+(i+1));tipo.setMatriculaVinculoObrigatoria(true);em.persist(tipo);
        var fornecedor=new Fornecedor();fornecedor.setRazaoSocial("DEMO-RJ · Prestador fictício "+(i+1));
        fornecedor.setCnpj(cnpj(i+1));fornecedor.setEmail("prestador"+(i+1)+"@example.test");em.persist(fornecedor);
        var c=new Contrato();c.setNumeroContrato("DEMO-RJ-"+(i+1));c.setAnoContrato(inicio.getYear());c.setDescricao(NATUREZA);
        c.setGestaoLocal(gestao);c.setFornecedor(fornecedor);c.setTipoContratacao(tipo);c.setCnpjEmpresa(fornecedor.getCnpj());
        c.setRazaoSocialEmpresa(fornecedor.getRazaoSocial());c.setNomeFantasiaEmpresa(fornecedor.getRazaoSocial());
        c.setDataInicio(inicio);c.setDataFim(referencia.plusDays(i<2?20:365));c.setEfetivoPrevisto(10);c.setEfetivoOriginalPublicado(10);
        c.setMatriculaVinculoObrigatoria(true);c.setPublicadoEm(inicio.atStartOfDay(ZONA).toInstant());c.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        c.setPublicadoPorId(gestor.getId());c.setPublicadoPorUsername(gestor.getUsername());c.setReferenciaSeiPublicacao("DEMO-RJ-FICTICIO-"+(i+1));em.persist(c);
        var v=new VersaoContratoNegocial();v.setContrato(c);v.setNumeroVersao(1);v.setDataEfetiva(inicio);
        v.setNumeroContrato(c.getNumeroContrato());v.setAnoContrato(c.getAnoContrato());v.setDescricao(NATUREZA);
        v.setDataInicio(inicio);v.setDataFim(c.getDataFim());v.setGestaoLocalId(gestao.getId());v.setGestaoLocalNome(gestao.getNome());
        v.setEfetivoPrevisto(10);v.setFornecedorId(fornecedor.getId());v.setCnpjEmpresa(c.getCnpjEmpresa());v.setRazaoSocialEmpresa(c.getRazaoSocialEmpresa());
        v.setTipoContratacaoId(tipo.getId());v.setTipoContratacaoDescricao(tipo.getDescricao());v.setMatriculaVinculoObrigatoria(true);
        v.setMatriculaTipoContratacaoObrigatoria(true);v.setMatriculaVinculoObrigatoriaEfetiva(true);v.setHabilitacaoAdministrativa(HabilitacaoAdministrativaContrato.ATIVO);em.persist(v);
        var comp=new ComposicaoContratual();comp.setContrato(c);comp.setLotacao(l);comp.setJornada(j);comp.setCargo(cargo);
        comp.setEfetivoPrevisto(10);comp.setDataInicioVigencia(inicio);comp.setDataFimVigencia(c.getDataFim());em.persist(comp);
        return new Unidade(gestao,l,j,cargo,c,comp,gestor,i);
    }
    private Trabalhador trabalhador(Unidade u,int i,LocalDate inicio,StatusEmpregado status) {
        int numero=u.indice()*9+i+1;String matricula=String.format(Locale.ROOT,"DEMO-RJ-%03d",numero);
        var endereco=new Endereco();endereco.setLogradouro("Rua fictícia de demonstração");endereco.setNumero(String.valueOf(numero));
        endereco.setBairro("Bairro fictício");endereco.setCidade(u.lotacao().getMunicipio());endereco.setUf("RJ");endereco.setCep("20000000");em.persist(endereco);
        var p=new Pessoa();p.setNome("DEMO-RJ · Pessoa fictícia "+String.format(Locale.ROOT,"%03d",numero));p.setCpf(cpf(numero));
        p.setEmail("pessoa"+numero+"@example.test");p.setDataNascimento(inicio.minusYears(25).plusDays(numero));p.setEndereco(endereco);em.persist(p);
        var v=new VinculoContratual();v.setPessoa(p);v.setContrato(u.contrato());v.setDataInicio(inicio);v.setMatricula(matricula);
        v.setCriadoPorId(u.gestor().getId());v.setCriadoPorUsername(u.gestor().getUsername());em.persist(v);
        var a=new AlocacaoVinculo();a.setVinculo(v);a.setComposicao(u.composicao());a.setLotacao(u.lotacao());a.setJornada(u.jornada());a.setCargo(u.cargo());
        a.setDataInicio(inicio);a.setDataFim(u.contrato().getDataFim());a.setCriadoPorId(u.gestor().getId());a.setCriadoPorUsername(u.gestor().getUsername());em.persist(a);
        var e=new Empregado();e.setNome(p.getNome());e.setCpf(p.getCpf());e.setMatricula(matricula);e.setPessoaCanonica(p);e.setEmail(p.getEmail());
        e.setDataNascimento(p.getDataNascimento());e.setDataAdmissao(inicio);e.setGestaoLocal(u.gestao());e.setLotacao(u.lotacao());e.setJornada(u.jornada());
        e.setCargo(u.cargo());e.setStatus(status);e.setContratos(new ArrayList<>(List.of(u.contrato())));em.persist(e);
        return new Trabalhador(u,p,v,a,e,i);
    }
    private static JornadaDiaSemana dia(Jornada j,LocalDate data) {
        return j.getDias().stream().filter(d->d.getDiaSemana()==data.getDayOfWeek()).findFirst().orElseThrow();
    }
    private void frequencia(Trabalhador t,LocalDate data,JornadaDiaSemana dia,LocalDate ref) {
        Unidade u=t.unidade();String[] status={"PRESENCA","PRESENCA","PRESENCA","PRESENCA","FALTA","ATESTADO","ATRASO","PRESENCA_PARCIAL","PRESENCA"};
        String situacao=status[Math.floorMod((int)(data.toEpochDay()%9)+t.indice()+u.indice(),9)];
        var f=new Frequencia();f.setEmpregado(t.empregado());f.setPessoa(t.pessoa());f.setVinculoContratual(t.vinculo());f.setAlocacaoVinculo(t.alocacao());
        f.setDataReferencia(data);f.setStatus(situacao);f.setObservacao(NATUREZA);
        f.setGestaoLocalSnapshotId(u.gestao().getId());f.setSuperintendenciaSnapshotId(u.gestao().getSuperintendencia().getId());
        f.setContratoSnapshotId(u.contrato().getId());f.setContratoSnapshotNumero(u.contrato().getNumeroContrato());
        f.setLotacaoSnapshotId(u.lotacao().getId());f.setLotacaoSnapshotNome(u.lotacao().getNome());f.setLotacaoSnapshotMunicipio(u.lotacao().getMunicipio());
        f.setJornadaSnapshotId(u.jornada().getId());f.setJornadaSnapshotDescricao(u.jornada().getDescricao());f.setJornadaSnapshotDiaTrabalho(true);
        f.setJornadaSnapshotEntrada1(dia.getEntrada1());f.setJornadaSnapshotSaida1(dia.getSaida1());f.setJornadaSnapshotEntrada2(dia.getEntrada2());f.setJornadaSnapshotSaida2(dia.getSaida2());
        f.setCargoSnapshotId(u.cargo().getId());f.setCargoSnapshotNome(u.cargo().getNome());f.setEmpregadoSnapshotNome(t.pessoa().getNome());
        if(!Set.of("FALTA","ATESTADO").contains(situacao)) {
            f.setEntrada1("ATRASO".equals(situacao)?"08:30":"08:00");f.setSaida1("12:00");
            if(!"PRESENCA_PARCIAL".equals(situacao)){f.setEntrada2("13:00");f.setSaida2("17:00");}
        }
        calculo.processarCalculos(f);
        boolean tardio=data.isBefore(ref.minusDays(10)) && (data.toEpochDay()+t.indice())%13==0;
        Instant primeiro=(tardio?data.plusDays(8):data).atTime(17,5).atZone(ZONA).toInstant();
        if(primeiro.isAfter(Instant.now()))primeiro=Instant.now();
        politica.capturarPrazos(f,primeiro);f.setAutorOriginalId(u.gestor().getId());f.setAutorOriginalUsername(u.gestor().getUsername());
        if(politica.edicaoEncerrada(f,Instant.now())){f.setEstadoNegocial(EstadoNegocialFrequencia.FECHADA);f.setBloqueadaEdicaoOperacional(true);}
        em.persist(f);
        auditoria.registrar(f,TipoEventoFrequencia.REGISTRO,null,NATUREZA,u.gestor(),"DEMO-RJ:"+t.empregado().getMatricula()+":"+data,null);
    }
    private static String cpf(int numero) {
        String base=String.format(Locale.ROOT,"900%06d",numero);StringBuilder result=new StringBuilder(base);
        for(int tamanho=9;tamanho<=10;tamanho++) {
            int soma=0;for(int i=0;i<tamanho;i++)soma+=(result.charAt(i)-'0')*(tamanho+1-i);
            int digito=11-soma%11;result.append(digito>=10?0:digito);
        }return result.toString();
    }
    private static String cnpj(int numero) {
        StringBuilder result=new StringBuilder(String.format(Locale.ROOT,"99000000%04d",numero));
        for(int pesoInicial:new int[]{5,6}) {
            int soma=0,peso=pesoInicial;for(int i=0;i<result.length();i++){soma+=(result.charAt(i)-'0')*peso;if(--peso==1)peso=9;}
            int digito=11-soma%11;result.append(digito>=10?0:digito);
        }return result.toString();
    }
}
