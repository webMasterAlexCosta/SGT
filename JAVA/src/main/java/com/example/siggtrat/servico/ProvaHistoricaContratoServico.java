package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.*;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.documento.EstadoDocumentoComprobatorio;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.*;
import com.fasterxml.jackson.databind.*;
import jakarta.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Confere uma fonte declarada, preserva o acervo e publica conhecimento novo de um fato antigo. */
@Service
@Transactional
public class ProvaHistoricaContratoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(ProvaHistoricaContratoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private static final TipoPermissaoExcepcional PERMISSAO=TipoPermissaoExcepcional.INCORPORACAO_PROVA_HISTORICA;
    private static final Set<String> CAMPOS=Set.of("dataEfetiva","numeroContrato","anoContrato","descricao",
            "tipoContratacaoId","tipoContratacaoDescricao","fornecedorId","cnpjEmpresa","razaoSocialEmpresa",
            "nomeFantasiaEmpresa","dataInicio","dataFim","efetivoPrevisto","gestaoLocalId","gestaoLocalNome",
            "matriculaVinculoObrigatoria","matriculaTipoContratacaoObrigatoria","matriculaVinculoObrigatoriaEfetiva",
            "diaSemanaReduzido","horasDiaReduzido","habilitacaoAdministrativa");
    private final ContratoRepositorio contratos;
    private final VersaoContratoNegocialRepositorio versoes;
    private final ProvaHistoricaContratoRepositorio provas;
    private final ProvaHistoricaComposicaoRepositorio itens;
    private final ProvaHistoricaAditamentoComposicaoRepositorio itensAditamento;
    private final PermissaoExcepcionalRepositorio concessoes;
    private final AcessoOrganizacionalServico acesso;
    private final GestaoContratoTemporalServico gestoes;
    private final PermissaoExcepcionalServico permissoes;
    private final MotorTemporalContratoServico motor;
    private final AditamentoContratualServico aditamentos;
    private final ComposicaoContratualTemporalServico composicaoTemporal;
    private final ConsultaProvaHistoricaContratoServico consulta;
    private final DocumentoComprobatorioServico documentos;
    private final DisponibilidadeCatalogoTemporalServico disponibilidade;
    private final RegistroAuditoriaServico auditoria;
    private final RevalidacaoFrequenciaTemporalServico revalidacao;
    private final ObjectMapper mapper;
    @PersistenceContext private EntityManager em;

    public ProvaHistoricaContratoServico(ContratoRepositorio contratos,VersaoContratoNegocialRepositorio versoes,
            ProvaHistoricaContratoRepositorio provas,ProvaHistoricaComposicaoRepositorio itens,
            ProvaHistoricaAditamentoComposicaoRepositorio itensAditamento,
            PermissaoExcepcionalRepositorio concessoes,AcessoOrganizacionalServico acesso,
            GestaoContratoTemporalServico gestoes,PermissaoExcepcionalServico permissoes,
            MotorTemporalContratoServico motor,AditamentoContratualServico aditamentos,
            ComposicaoContratualTemporalServico composicaoTemporal,
            ConsultaProvaHistoricaContratoServico consulta,DocumentoComprobatorioServico documentos,
            DisponibilidadeCatalogoTemporalServico disponibilidade,RegistroAuditoriaServico auditoria,
            RevalidacaoFrequenciaTemporalServico revalidacao,ObjectMapper mapper) {
        this.contratos=contratos;this.versoes=versoes;this.provas=provas;this.itens=itens;this.itensAditamento=itensAditamento;
        this.concessoes=concessoes;this.acesso=acesso;this.gestoes=gestoes;this.permissoes=permissoes;
        this.motor=motor;this.aditamentos=aditamentos;this.consulta=consulta;this.documentos=documentos;
        this.composicaoTemporal=composicaoTemporal;
        this.disponibilidade=disponibilidade;this.auditoria=auditoria;this.revalidacao=revalidacao;
        this.mapper=mapper.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);
    }
    private record Contexto(Contrato contrato,UsuarioAplicacao usuario,PermissaoExcepcional permissao,
                            GestaoLocal gestaoAtual) { }
    private record Conferencia(EventoCicloVidaContrato evento,VersaoContratoNegocial anterior,
            ProvaHistoricaContrato provaAnterior,VersaoContratoNegocialDTO foto,Set<String> campos,
            DocumentoComprobatorioDTO documento,List<ProvaHistoricaContratoRequisicao.Item> itens) { }

    public PreviaProvaHistoricaContratoDTO previa(Long id,ProvaHistoricaContratoRequisicao r) {
        Contexto c=contexto(id,true);
        Conferencia f=conferir(c,r);
        return new PreviaProvaHistoricaContratoDTO(token(c,r,f),id,c.contrato().getVersion(),f.foto().dataEfetiva(),
                f.evento()==null,f.itens().size(),
                "Incorporação imutável: reconstitui a origem documentada, reaplica atos posteriores e reavalia frequências e emissões afetadas.");
    }
    public ProvaHistoricaContratoDTO incorporar(Long id,ProvaHistoricaContratoRequisicao r) {
        Contexto c=contexto(id,true);
        if(r==null)throw ruim("Informe a prova documental.");
        String chave=texto(r.chaveIdempotencia(),100,"Informe chave de idempotência.");
        String intencao=hashIntencao(c.usuario().getId(),r);
        var existente=provas.findByContratoIdAndChaveIdempotencia(id,chave).orElse(null);
        if(existente!=null) {
            if(!Objects.equals(existente.getHashConteudo(),intencao))throw conflito("A chave já identifica outra intenção documental.");
            // Reenvio não cria prova/versão/uso novo, mas depende de custódia e concessão ainda vigentes.
            return dto(existente);
        }
        Conferencia f=conferir(c,r);
        if(!Boolean.TRUE.equals(r.confirmarImpacto())||r.tokenPrevia()==null||!Objects.equals(r.tokenPrevia(),token(c,r,f)))
            throw conflito("Confirme uma prévia atual da mesma fonte e intenção documental.");
        var permissao=permissoes.exigirERegistrarUso(c.usuario(),PERMISSAO,c.contrato(),
                c.gestaoAtual(),"Incorporar fonte histórica: "+r.motivo(),"Contrato",id);
        EventoCicloVidaContrato evento=f.evento();
        if(evento==null) evento=motor.registrarEvento(c.contrato(),TipoEventoCicloVidaContrato.PUBLICACAO,
                new AcaoCicloVidaContratoDTO(f.foto().dataEfetiva(),r.dataDocumento(),r.motivo(),r.referenciaSei()),
                c.usuario(),null,"ORIGEM_DOCUMENTAL_INCORPORADA");
        Instant agora=Instant.now().truncatedTo(ChronoUnit.MICROS);
        var versao=motor.registrarFotografiaProjetada(c.contrato(),evento,evento.getDataEfetiva(),f.foto(),agora);
        var prova=new ProvaHistoricaContrato(c.contrato(),evento,f.provaAnterior(),f.anterior(),versao,
                f.documento()==null?null:em.getReference(DocumentoComprobatorio.class,f.documento().id()),permissao,
                f.provaAnterior()==null?1:f.provaAnterior().getNumeroRevisao()+1,agora,r.dataDocumento(),
                c.usuario().getId(),limpar(r.referenciaSei()),texto(r.motivo(),1500,"Informe o motivo."),
                texto(r.autorizacaoAdministrativa(),180,"Informe o ato de conferência."),chave,intencao,
                f.documento()==null?null:f.documento().sha256Arquivo(),f.documento()==null?null:f.documento().version(),
                String.join(",",new TreeSet<>(f.campos())),Boolean.TRUE.equals(r.configuracaoCompletaComprovada()));
        provas.saveAndFlush(prova);
        for(var item:f.itens()) {
            if(item.aditamentoComposicaoId()!=null) {
                itensAditamento.save(new ProvaHistoricaAditamentoComposicao(prova,
                        em.getReference(AditamentoComposicao.class,item.aditamentoComposicaoId()),
                        limpar(item.lotacaoNome()),limpar(item.jornadaDescricao()),limpar(item.cargoNome()),limpar(item.lotacaoMunicipio()),
                        Boolean.TRUE.equals(item.municipioComprovado()),limpar(item.jornadaSemanal()),Boolean.TRUE.equals(item.jornadaComprovada())));
            } else itens.save(new ProvaHistoricaComposicao(prova,
                item.composicaoId()==null?null:em.getReference(ComposicaoContratual.class,item.composicaoId()),
                item.mapeamentoId()==null?null:em.getReference(MapeamentoTransferenciaGestao.class,item.mapeamentoId()),
                limpar(item.lotacaoNome()),limpar(item.jornadaDescricao()),limpar(item.cargoNome()),limpar(item.lotacaoMunicipio()),
                Boolean.TRUE.equals(item.municipioComprovado()),limpar(item.jornadaSemanal()),Boolean.TRUE.equals(item.jornadaComprovada())));
        }
        itens.flush();
        itensAditamento.flush();
        em.lock(c.contrato(),LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        aditamentos.sincronizarProjecaoAtual(c.contrato());
        revalidacao.revalidarPorContrato(id,evento.getDataEfetiva(),"Fonte histórica incorporada; preservar emissões anteriores e reavaliar contexto");
        auditoria.registrarAcao("INCORPORAR_PROVA_HISTORICA_CONTRATO","Contrato",id,"FONTE_DOCUMENTAL",
                Map.of("provaId",prova.getId(),"eventoOrigemId",evento.getId(),"versaoGeradaId",versao.getId(),
                        "hashConteudo",intencao,"permissaoId",permissao.getId()));
        return dto(prova);
    }
    @Transactional(readOnly=true)
    public List<ProvaHistoricaContratoDTO> listar(Long id,Instant conhecidoAte) {
        Contexto c=contexto(id,false);
        Instant conhecimento=conhecidoAte==null?Instant.now():conhecidoAte;
        var conhecidas=provas.findByContratoIdAndRegistradoEmLessThanEqualOrderByNumeroRevisaoAsc(c.contrato().getId(),conhecimento);
        conhecidas.stream().map(p->p.getEventoOrigem().getId()).distinct().forEach(e->consulta.vigente(e,conhecimento));
        return conhecidas.stream().map(this::dto).toList();
    }
    private Contexto contexto(Long id,boolean escrever) {
        UsuarioAplicacao u=acesso.usuarioAtual();
        if(escrever&&u.getPerfil()!=PerfilAcesso.GESTOR_LOCAL)throw proibido("Somente o Gestor Local expressamente autorizado pode incorporar provas.");
        if(!escrever)acesso.exigirConsultaContrato(u);
        Contrato ct=(escrever?contratos.buscarPorIdParaAtualizacao(id):contratos.findById(id))
                .orElseThrow(()->ausente("Contrato não encontrado."));
        if (escrever) {
            acesso.revalidarAposEspera(u);
            if (u.getPerfil()!=PerfilAcesso.GESTOR_LOCAL || u.getGestaoLocal()==null
                    || !Boolean.TRUE.equals(u.getGestaoLocal().getAtivo()))
                throw proibido("Somente o Gestor Local ativo e expressamente autorizado pode incorporar provas.");
        }
        GestaoLocal gestao=gestoes.gestaoAtual(ct);
        PermissaoExcepcional p=null;
        if(u.getPerfil()==PerfilAcesso.GESTOR_LOCAL) p=concessoes.buscarAtivas(u.getId(),PERMISSAO,Instant.now()).stream()
                .filter(x->x.getContrato()!=null&&Objects.equals(x.getContrato().getId(),id)).findFirst().orElse(null);
        boolean designado=gestao==null&&p!=null&&p.getGestaoLocal()!=null&&u.getGestaoLocal()!=null
                &&Objects.equals(p.getGestaoLocal().getId(),u.getGestaoLocal().getId());
        if(!designado)acesso.exigirAcessoContrato(u,ct);
        if(escrever&&p==null)throw proibido("A incorporação exige concessão administrativa específica e vigente.");
        return new Contexto(ct,u,p,gestao);
    }
    private Conferencia conferir(Contexto c,ProvaHistoricaContratoRequisicao r) {
        if(r==null)throw ruim("Informe a prova documental.");
        texto(r.motivo(),1500,"Informe o motivo da incorporação.");
        texto(r.autorizacaoAdministrativa(),180,"Informe a autorização da conferência.");
        texto(r.chaveIdempotencia(),100,"Informe chave de idempotência.");
        if(r.versaoContratoEsperada()==null||r.versaoContratoEsperada()!=c.contrato().getVersion())
            throw conflito("A versão contratual mudou; recarregue a prévia.");
        if(r.dataDocumento()==null||r.dataDocumento().isAfter(RelogioSistema.hoje()))throw ruim("Informe a data válida do documento original.");
        if(limpar(r.referenciaSei())==null&&r.documentoComprobatorioId()==null)throw ruim("Documento ou referência SEI é obrigatório; sem fonte a lacuna permanece.");
        if(r.referenciaSei()!=null&&r.referenciaSei().length()>200)throw ruim("Referência SEI excede 200 caracteres.");
        DocumentoComprobatorioDTO doc=null;
        if(r.documentoComprobatorioId()!=null) {
            doc=documentos.conferirFonteHistorica(c.contrato().getId(),r.documentoComprobatorioId(),r.finalidadeAcessoDocumento());
            if(doc.estado()!=EstadoDocumentoComprobatorio.PUBLICADO)throw conflito("Somente uma versão documental publicada e válida pode fundamentar a prova.");
        }
        EventoCicloVidaContrato evento=r.eventoOrigemId()==null?null:em.find(EventoCicloVidaContrato.class,r.eventoOrigemId());
        boolean recuperar=Boolean.TRUE.equals(r.recuperarPublicacaoOriginal());
        if(evento==null) {
            if(r.eventoOrigemId()!=null)throw ausente("Origem não encontrada no contrato.");
            if(!recuperar||!Boolean.TRUE.equals(r.configuracaoCompletaComprovada()))throw ruim("Publicação ausente exige recuperação expressa e fonte integral.");
            if(c.contrato().getEstadoNegocial()==EstadoNegocialContrato.RASCUNHO&&c.contrato().getPublicadoEm()==null)
                throw conflito("Um rascunho nunca publicado deve seguir o fluxo de publicação.");
            Long publicacoes=em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:c and e.tipoEvento=:t",Long.class)
                    .setParameter("c",c.contrato().getId()).setParameter("t",TipoEventoCicloVidaContrato.PUBLICACAO).getSingleResult();
            if(publicacoes>0)throw conflito("A publicação já existe; referencie a origem preservada.");
        } else {
            if(!Objects.equals(evento.getContrato().getId(),c.contrato().getId()))throw ausente("Origem não encontrada no contrato.");
            if(recuperar)throw ruim("Recuperação de publicação não pode substituir um evento existente.");
            if(evento.getTipoEvento()!=TipoEventoCicloVidaContrato.PUBLICACAO
                    &&evento.getTipoEvento()!=TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL
                    &&evento.getTipoEvento()!=TipoEventoCicloVidaContrato.ADITAMENTO)
                throw ruim("Esta incorporação complementa publicação, transferência ou configuração de origem em aditamento.");
        }
        boolean aditamento=evento!=null&&evento.getTipoEvento()==TipoEventoCicloVidaContrato.ADITAMENTO;
        if(aditamento) {
            if(!Boolean.FALSE.equals(r.configuracaoCompletaComprovada())
                    ||r.camposComprovados()!=null&&!r.camposComprovados().isEmpty()
                    ||r.composicoes()==null||r.composicoes().isEmpty())
                throw ruim("A fonte do aditamento comprova somente configurações de seus itens; não certifica campos escalares nem fotografia integral.");
            var instrumento=em.createQuery("select a from AditamentoContratual a where a.evento.id=:id",AditamentoContratual.class)
                    .setParameter("id",evento.getId()).getResultStream().findFirst()
                    .orElseThrow(()->conflito("Identidade canônica do aditamento ausente."));
            if(!Objects.equals(instrumento.getContrato().getId(),c.contrato().getId()))
                throw conflito("O instrumento do aditamento não corresponde ao contrato da origem.");
            if(doc!=null&&(doc.tipoFato()!=com.example.siggtrat.dominio.documento.TipoFatoDocumental.ADITAMENTO
                    ||!Objects.equals(doc.fatoId(),instrumento.getId())))
                throw conflito("A fonte documental deve identificar o aditamento de origem da configuração.");
        }
        ProvaHistoricaContrato anterior=evento==null?null:consulta.vigente(evento.getId(),Instant.now()).orElse(null);
        if(!Objects.equals(r.provaAnteriorId(),anterior==null?null:anterior.getId()))
            throw conflito("Uma revisão documental exige relação explícita com a prova vigente.");
        Set<Long> geradas=provas.findByContratoIdAndRegistradoEmLessThanEqualOrderByNumeroRevisaoAsc(c.contrato().getId(),Instant.now())
                .stream().map(p->p.getVersaoGerada().getId()).collect(java.util.stream.Collectors.toSet());
        VersaoContratoNegocial base=evento==null?null:versoes.findByContratoIdOrderByNumeroVersaoAsc(c.contrato().getId()).stream()
                .filter(v->!geradas.contains(v.getId()))
                .filter(v->v.getEventoOrigem()!=null&&Objects.equals(v.getEventoOrigem().getId(),r.eventoOrigemId())).findFirst().orElse(null);
        VersaoContratoNegocial ultima=anterior==null?base:anterior.getVersaoGerada();
        if(aditamento&&base==null)throw conflito("A fotografia original do aditamento deve estar preservada.");
        if(!Objects.equals(r.versaoAnteriorId(),ultima==null?null:ultima.getId()))throw conflito("A fotografia anterior declarada não corresponde à origem vigente.");
        boolean completa=Boolean.TRUE.equals(r.configuracaoCompletaComprovada());
        Set<String> campos=completa?CAMPOS:r.camposComprovados()==null?Set.of():new TreeSet<>(r.camposComprovados());
        if(!CAMPOS.containsAll(campos))throw ruim("Campos comprovados contêm propriedades não históricas.");
        if(r.fotografia()==null||(!completa&&campos.isEmpty()&&(r.composicoes()==null||r.composicoes().isEmpty())))
            throw ruim("Declare a fotografia e os campos ou configurações efetivamente comprovados.");
        Map<String,Object> proposta=mapa(r.fotografia());
        Map<String,Object> original=base==null?Map.of():mapa(motor.paraVersaoDTO(base));
        Map<String,Object> merged=ultima==null?new TreeMap<>():new TreeMap<>(mapa(motor.paraVersaoDTO(ultima)));
        boolean transferencia=evento!=null&&evento.getTipoEvento()==TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL;
        TransferenciaGestaoContrato transf=transferencia?em.createQuery("select t from TransferenciaGestaoContrato t where t.evento.id=:id",TransferenciaGestaoContrato.class)
                .setParameter("id",evento.getId()).getResultStream().findFirst().orElseThrow(()->conflito("Identidade canônica da transferência ausente.")):null;
        Set<String> semOrigemTipo=transf!=null&&transf.getTipoContratacaoDestino()==null
                ?Set.of("tipoContratacaoId","tipoContratacaoDescricao","matriculaTipoContratacaoObrigatoria"):Set.of();
        if(!semOrigemTipo.isEmpty()&&anterior==null) {
            // A foto legada podia ter herdado o Tipo da origem. Campos não
            // declarados pela fonte nova continuam desconhecidos no destino.
            for(String campo:semOrigemTipo)merged.put(campo,null);
        }
        for(String campo:campos) {
            Object valor=proposta.get(campo), conhecido=original.get(campo);
            if(conhecido instanceof String texto&&texto.isBlank())conhecido=null;
            boolean ausenciaOriginalComprovada=conhecido==null&&original.get("matriculaVinculoObrigatoriaEfetiva")!=null
                    &&Set.of("matriculaVinculoObrigatoria","diaSemanaReduzido","horasDiaReduzido").contains(campo);
            if(ausenciaOriginalComprovada&&valor!=null)throw conflito("A fonte contradiz a ausência originalmente comprovada de "+campo+".");
            if(conhecido!=null&&!semOrigemTipo.contains(campo)&&!Objects.equals(conhecido,valor))
                throw conflito("A fonte complementar contradiz o campo original já comprovado: "+campo+". Use o ato negocial próprio.");
            merged.put(campo,valor);
        }
        // Metadados da nova fotografia são produzidos pelo servidor, nunca copiados do cliente.
        for(String campo:List.of("id","numeroVersao","eventoOrigemId","registradoEm","tipoContratacaoComprovado"))merged.remove(campo);
        VersaoContratoNegocialDTO foto=mapper.convertValue(merged,VersaoContratoNegocialDTO.class);
        validarFotografia(c,evento,transf,foto,completa);
        var configuracoes=r.composicoes()==null?List.<ProvaHistoricaContratoRequisicao.Item>of():List.copyOf(r.composicoes());
        validarItens(c,evento,transf,configuracoes,completa,anterior);
        return new Conferencia(evento,ultima,anterior,foto,campos,doc,configuracoes);
    }
    private void validarFotografia(Contexto c,EventoCicloVidaContrato evento,TransferenciaGestaoContrato transferencia,
                                    VersaoContratoNegocialDTO f,boolean completa) {
        if(f.dataEfetiva()==null)throw ruim("A data efetiva documentada é obrigatória.");
        if(evento!=null&&!Objects.equals(evento.getDataEfetiva(),f.dataEfetiva()))
            throw conflito("Uma prova não altera a data efetiva do ato; use sua retificação negocial.");
        if(completa) {
            texto(f.numeroContrato(),80,"Informe o número contratual original.");
            texto(f.descricao(),500,"Informe a descrição documentada.");
            texto(f.tipoContratacaoDescricao(),160,"Informe a descrição documentada do Tipo.");
            texto(f.gestaoLocalNome(),180,"Informe a denominação documentada da Gestão.");
            texto(f.cnpjEmpresa(),14,"A fonte integral exige o CNPJ original comprovado.");
            texto(f.razaoSocialEmpresa(),180,"A fonte integral exige a razão social original comprovada.");
            texto(f.nomeFantasiaEmpresa(),180,"A fonte integral exige o nome fantasia original comprovado.");
            if(f.anoContrato()==null||f.anoContrato()<1900||f.anoContrato()>9999||f.tipoContratacaoId()==null
                    ||f.gestaoLocalId()==null||f.dataInicio()==null||f.dataFim()==null||f.efetivoPrevisto()==null
                    ||f.matriculaTipoContratacaoObrigatoria()==null||f.matriculaVinculoObrigatoriaEfetiva()==null
                    ||f.habilitacaoAdministrativa()==null)throw ruim("A fonte declarada integral não comprova toda a configuração obrigatória.");
        }
        validarComprimento(f.numeroContrato(),80);validarComprimento(f.descricao(),500);
        validarComprimento(f.tipoContratacaoDescricao(),160);validarComprimento(f.cnpjEmpresa(),14);
        validarComprimento(f.razaoSocialEmpresa(),180);validarComprimento(f.nomeFantasiaEmpresa(),180);
        validarComprimento(f.gestaoLocalNome(),180);
        if(f.cnpjEmpresa()!=null&&!f.cnpjEmpresa().matches("[0-9]{14}"))
            throw ruim("O CNPJ documentado deve conter os 14 dígitos exigidos pelo cadastro contratual.");
        if(f.dataInicio()!=null&&f.dataFim()!=null&&f.dataFim().isBefore(f.dataInicio()))throw ruim("Vigência documental invertida.");
        if(f.efetivoPrevisto()!=null&&f.efetivoPrevisto()<0)throw ruim("Efetivo documentado inválido.");
        if((evento==null||evento.getTipoEvento()==TipoEventoCicloVidaContrato.PUBLICACAO)&&f.efetivoPrevisto()!=null) {
            Long soma=em.createQuery("select sum(c.efetivoPrevisto) from ComposicaoContratual c where c.contrato.id=:c",Long.class)
                    .setParameter("c",c.contrato().getId()).getSingleResult();
            if(completa&&soma==null)
                throw conflito("A fonte integral exige composições originais preservadas com quantitativos comprovados.");
            if(soma!=null&&soma.longValue()!=f.efetivoPrevisto().longValue())
                throw conflito("O total documentado contradiz as quantidades originais preservadas nas composições.");
        }
        if((f.diaSemanaReduzido()==null)!=(f.horasDiaReduzido()==null)
                ||f.horasDiaReduzido()!=null&&(f.horasDiaReduzido()<1||f.horasDiaReduzido()>24))
            throw ruim("A configuração de dia reduzido está incompleta.");
        if(f.matriculaVinculoObrigatoriaEfetiva()!=null&&f.matriculaTipoContratacaoObrigatoria()!=null) {
            boolean esperado=f.matriculaVinculoObrigatoria()!=null?f.matriculaVinculoObrigatoria():f.matriculaTipoContratacaoObrigatoria();
            if(f.matriculaVinculoObrigatoriaEfetiva()!=esperado)throw ruim("A política efetiva de matrícula contradiz a configuração documentada.");
        }
        if(f.gestaoLocalId()!=null&&em.find(GestaoLocal.class,f.gestaoLocalId())==null)throw ruim("Identidade canônica da Gestão não encontrada.");
        if(c.gestaoAtual()==null&&c.permissao()!=null&&c.permissao().getGestaoLocal()!=null
                &&!Objects.equals(c.permissao().getGestaoLocal().getId(),f.gestaoLocalId()))
            throw proibido("A origem documentada não corresponde à designação administrativa de conferência.");
        if(transferencia!=null&&!Objects.equals(transferencia.getGestaoDestino().getId(),f.gestaoLocalId()))
            throw conflito("A prova deve preservar a Gestão de destino da transferência identificada.");
        if(f.tipoContratacaoId()!=null) {
            TipoContratacao tipo=em.find(TipoContratacao.class,f.tipoContratacaoId());
            if(tipo==null||tipo.getGestaoLocal()==null||!Objects.equals(tipo.getGestaoLocal().getId(),f.gestaoLocalId()))
                throw ruim("O Tipo canônico não pertence à Gestão documentada.");
            if(!disponibilidade.ativoEm(TipoCatalogoTemporal.TIPO_CONTRATACAO,tipo.getId(),f.dataEfetiva(),tipo.getAtivo()))
                throw conflito("O Tipo não estava disponível na data documentada; concilie a fonte de disponibilidade.");
        }
        if(f.fornecedorId()!=null&&em.find(Fornecedor.class,f.fornecedorId())==null)throw ruim("Identidade canônica do fornecedor ausente.");
    }
    private void validarItens(Contexto c,EventoCicloVidaContrato evento,TransferenciaGestaoContrato transferencia,
            List<ProvaHistoricaContratoRequisicao.Item> configuracoes,boolean completa,ProvaHistoricaContrato anterior) {
        Set<Long> referenciados=new HashSet<>();
        for(var item:configuracoes) {
            if(item==null||(item.composicaoId()==null?0:1)+(item.mapeamentoId()==null?0:1)
                    +(item.aditamentoComposicaoId()==null?0:1)!=1)
                throw ruim("Cada configuração exige uma única origem canônica.");
            if(completa) {
                texto(item.lotacaoNome(),150,"Informe a Lotação documentada.");
                texto(item.jornadaDescricao(),150,"Informe a Jornada documentada.");
                texto(item.cargoNome(),150,"Informe o Cargo documentado.");
                if(!Boolean.TRUE.equals(item.municipioComprovado())||!Boolean.TRUE.equals(item.jornadaComprovada()))
                    throw ruim("Uma configuração integral exige declaração explícita de município (inclusive ausência) e semana.");
            }
            validarComprimento(item.lotacaoNome(),150);validarComprimento(item.jornadaDescricao(),150);
            validarComprimento(item.cargoNome(),150);validarComprimento(item.lotacaoMunicipio(),180);
            if(Boolean.TRUE.equals(item.jornadaComprovada()))validarSemana(item.jornadaSemanal());
            else if(limpar(item.jornadaSemanal())!=null)throw ruim("Grade informada sem declaração de comprovação.");
            if(!Boolean.TRUE.equals(item.municipioComprovado())&&limpar(item.lotacaoMunicipio())!=null)
                throw ruim("Município informado sem declaração de comprovação.");
            ConsultaProvaHistoricaContratoServico.Configuracao original;
            if(evento!=null&&evento.getTipoEvento()==TipoEventoCicloVidaContrato.ADITAMENTO) {
                if(item.aditamentoComposicaoId()==null)throw ruim("O aditamento referencia seus itens de composição preservados.");
                if(limpar(item.lotacaoNome())==null&&limpar(item.jornadaDescricao())==null&&limpar(item.cargoNome())==null
                        &&!Boolean.TRUE.equals(item.municipioComprovado())&&!Boolean.TRUE.equals(item.jornadaComprovada()))
                    throw ruim("O fragmento deve declarar ao menos uma propriedade de configuração comprovada.");
                var comp=em.find(AditamentoComposicao.class,item.aditamentoComposicaoId());
                if(comp==null||comp.getAditamento().getEvento()==null
                        ||!Objects.equals(comp.getAditamento().getEvento().getId(),evento.getId())
                        ||!Objects.equals(comp.getAditamento().getContrato().getId(),c.contrato().getId()))
                    throw ausente("Item não encontrado no aditamento de origem deste contrato.");
                validarOrigemAditamento(c.contrato(),evento,comp);
                original=new ConsultaProvaHistoricaContratoServico.Configuracao(limpar(comp.getLotacaoNomeFotografia()),
                        limpar(comp.getJornadaDescricaoFotografia()),limpar(comp.getCargoNomeFotografia()),
                        comp.getLotacaoMunicipioFotografia(),comp.getJornadaSemanalFotografia());
                if(!referenciados.add(comp.getId()))throw ruim("Item de aditamento duplicado na fonte.");
            } else if(transferencia==null) {
                if(item.composicaoId()==null)throw ruim("A publicação referencia composições originais.");
                var comp=em.find(ComposicaoContratual.class,item.composicaoId());
                if(comp==null||!Objects.equals(comp.getContrato().getId(),c.contrato().getId()))throw ausente("Composição não encontrada no contrato.");
                original=new ConsultaProvaHistoricaContratoServico.Configuracao(limpar(comp.getLotacaoNomeFotografia()),
                        limpar(comp.getJornadaDescricaoFotografia()),limpar(comp.getCargoNomeFotografia()),
                        comp.getLotacaoMunicipioFotografia(),comp.getJornadaSemanalFotografia());
                if(!referenciados.add(comp.getId()))throw ruim("Composição duplicada na fonte.");
            } else {
                if(item.mapeamentoId()==null)throw ruim("A transferência referencia seus mapeamentos preservados.");
                var mapa=em.find(MapeamentoTransferenciaGestao.class,item.mapeamentoId());
                if(mapa==null||!Objects.equals(mapa.getTransferencia().getId(),transferencia.getId()))throw ausente("Mapeamento não encontrado nesta transferência.");
                original=new ConsultaProvaHistoricaContratoServico.Configuracao(limpar(mapa.getLotacaoDestinoNomeFotografia()),
                        limpar(mapa.getJornadaDestinoDescricaoFotografia()),limpar(mapa.getCargoDestinoNomeFotografia()),
                        mapa.getLotacaoDestinoMunicipioFotografia(),mapa.getJornadaDestinoSemanalFotografia());
                if(!referenciados.add(mapa.getId()))throw ruim("Mapeamento duplicado na fonte.");
            }
            protegerConhecido(original.lotacaoNome(),limpar(item.lotacaoNome()),"Lotação");
            protegerConhecido(original.jornadaDescricao(),limpar(item.jornadaDescricao()),"Jornada");
            protegerConhecido(original.cargoNome(),limpar(item.cargoNome()),"Cargo");
            if(Boolean.TRUE.equals(item.jornadaComprovada()))protegerConhecido(original.jornadaSemanal(),limpar(item.jornadaSemanal()),"grade semanal");
            if(Boolean.TRUE.equals(item.municipioComprovado())&&(original.lotacaoMunicipio()!=null||original.jornadaSemanal()!=null)
                    &&!Objects.equals(original.lotacaoMunicipio(),limpar(item.lotacaoMunicipio())))
                throw conflito("A fonte contradiz o município originalmente comprovado.");
        }
        if(completa) {
            long esperados=transferencia!=null?transferencia.getMapeamentos().size():em.createQuery(
                    "select count(c) from ComposicaoContratual c where c.contrato.id=:c",Long.class)
                    .setParameter("c",c.contrato().getId()).getSingleResult();
            if(esperados==0)throw conflito("A fonte integral exige ao menos uma composição ou mapeamento de origem preservado.");
            if(referenciados.size()!=esperados)throw ruim("A fonte integral deve identificar todas as composições ou mapeamentos da origem.");
        }
    }
    private void validarOrigemAditamento(Contrato contrato,EventoCicloVidaContrato evento,AditamentoComposicao item) {
        Instant conhecimento=Instant.now();
        var eventos=motor.eventosValidosOrdenados(contrato.getId(),evento.getDataEfetiva(),conhecimento);
        int indice=-1;
        for(int i=0;i<eventos.size();i++)if(Objects.equals(eventos.get(i).getId(),evento.getId()))indice=i;
        if(indice<0)throw conflito("O aditamento informado não integra a linha temporal efetiva conhecida.");
        Set<Long> posteriores=new HashSet<>();
        for(int i=indice+1;i<eventos.size();i++)posteriores.add(eventos.get(i).getId());
        boolean origem=composicaoTemporal.projetarEntidades(contrato,evento.getDataEfetiva(),conhecimento,posteriores)
                .stream().anyMatch(p->Objects.equals(p.aditamentoComposicaoOrigemId(),item.getId()));
        if(!origem)throw conflito("O item não introduz a configuração na linha temporal; deltas quantitativos herdam a origem anterior.");
    }
    private void validarSemana(String grade) {
        texto(grade,1000,"A semana comprovada é obrigatória.");
        Set<DayOfWeek> dias=EnumSet.noneOf(DayOfWeek.class);
        try {
            for(String item:grade.split(";",-1)) {
                String[] valores=item.split("\\|",-1);
                if(valores.length!=6||!Set.of("true","false").contains(valores[1])||!dias.add(DayOfWeek.valueOf(valores[0])))
                    throw ruim("Grade semanal inválida ou com dias repetidos.");
                long primeiro=UtilitarioHorario.calcularIntervaloMinutos(valores[2],valores[3],"entrada1","saida1");
                long segundo=UtilitarioHorario.calcularIntervaloMinutos(valores[4],valores[5],"entrada2","saida2");
                if(Boolean.parseBoolean(valores[1])?primeiro+segundo<=0:primeiro+segundo!=0)
                    throw ruim("Horários não correspondem ao dia de trabalho documentado.");
            }
        } catch(IllegalArgumentException erro) {throw ruim("Grade semanal inválida: "+erro.getMessage());}
        if(dias.size()!=7)throw ruim("A fonte semanal deve declarar os sete dias, inclusive os não trabalhados.");
    }
    private String token(Contexto c,ProvaHistoricaContratoRequisicao r,Conferencia f) {
        Map<String,Object> estado=new TreeMap<>();
        estado.put("intencao",hashIntencao(c.usuario().getId(),r));estado.put("contratoVersao",c.contrato().getVersion());
        estado.put("permissaoId",c.permissao().getId());estado.put("permissaoVersao",c.permissao().getVersion());
        estado.put("provaAnterior",f.provaAnterior()==null?null:f.provaAnterior().getId());
        estado.put("documentoVersao",f.documento()==null?null:f.documento().version());
        estado.put("documentoHash",f.documento()==null?null:f.documento().sha256Arquivo());
        estado.put("itensAditamento",f.itens().stream().filter(i->i.aditamentoComposicaoId()!=null).map(i->{
            var item=em.find(AditamentoComposicao.class,i.aditamentoComposicaoId());
            return Arrays.asList(item.getId(),item.getAditamento().getId(),item.getAditamento().getVersion(),
                    item.getAditamento().getEvento().getId(),item.getAditamento().getDataEfetiva(),
                    item.getComposicaoBase()==null?null:item.getComposicaoBase().getId(),
                    item.getLotacao().getId(),item.getJornada().getId(),item.getCargo().getId(),
                    item.getQuantidadeAnterior(),item.getVariacao(),item.getQuantidadeResultante(),
                    item.getLotacaoNomeFotografia(),item.getJornadaDescricaoFotografia(),item.getCargoNomeFotografia(),
                    item.getLotacaoMunicipioFotografia(),item.getJornadaSemanalFotografia());
        }).toList());
        estado.put("versoes",versoes.findByContratoIdOrderByNumeroVersaoAsc(c.contrato().getId()).stream()
                .map(v->mapa(motor.paraVersaoDTO(v))).toList());
        estado.put("eventos",motor.eventosValidosOrdenados(c.contrato().getId(),LocalDate.MAX,Instant.now()).stream()
                .map(e->Arrays.asList(e.getId(),e.getTipoEvento().name(),e.getDataEfetiva().toString(),e.getFatoLogicoId())).toList());
        estado.put("relacoes",motor.relacoesConhecidas(c.contrato().getId(),Instant.now()).stream()
                .map(e->List.of(e.getEventoFonte().getId(),e.getEventoAlvo().getId(),e.getTipoRelacao().name())).toList());
        return hash(estado);
    }
    private String hashIntencao(Long ator,ProvaHistoricaContratoRequisicao r) {
        Map<String,Object> intencao=new TreeMap<>(mapa(r));
        intencao.remove("tokenPrevia");intencao.remove("confirmarImpacto");
        intencao.put("autorId",ator);
        return hash(intencao);
    }
    @SuppressWarnings("unchecked") private Map<String,Object> mapa(Object valor) {return mapper.convertValue(valor,TreeMap.class);}
    private String hash(Object valor) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(valor)));}
        catch(Exception ex){throw new IllegalStateException("Não foi possível calcular a identidade da prova.",ex);}
    }
    private ProvaHistoricaContratoDTO dto(ProvaHistoricaContrato p) {
        return new ProvaHistoricaContratoDTO(p.getId(),p.getContrato().getId(),p.getEventoOrigem().getId(),
                p.getProvaAnterior()==null?null:p.getProvaAnterior().getId(),p.getNumeroRevisao(),
                p.getVersaoAnterior()==null?null:p.getVersaoAnterior().getId(),p.getVersaoGerada().getId(),
                p.getEventoOrigem().getDataEfetiva(),p.getRegistradoEm(),p.getHashConteudo(),p.getConfiguracaoCompletaComprovada(),
                consulta.camposDeclarados(p),consulta.leituraFotografia(p.getVersaoGerada()),consulta.fragmentos(p),
                Map.of("referenciaSei",ProvasCamposHistoricos.restrito(),"motivo",ProvasCamposHistoricos.restrito(),
                        "autorizacaoAdministrativa",ProvasCamposHistoricos.restrito(),
                        "documentoComprobatorioId",ProvasCamposHistoricos.restrito(),"hashFonteArquivo",ProvasCamposHistoricos.restrito()));
    }
    private static void protegerConhecido(String original,String proposto,String campo) {
        if(original!=null&&proposto!=null&&!Objects.equals(original,proposto))throw conflito("A fonte contradiz o valor original de "+campo+".");
    }
    private static void validarComprimento(String v,int max){if(v!=null&&v.length()>max)throw ruim("Campo textual excede o limite de "+max+" caracteres.");}
    private static String limpar(String v){return v==null||v.isBlank()?null:v.trim();}
    private static String texto(String v,int max,String mensagem){if(limpar(v)==null)throw ruim(mensagem);validarComprimento(v,max);return v.trim();}
    private static ResponseStatusException ruim(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
    private static ResponseStatusException conflito(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
    private static ResponseStatusException ausente(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
    private static ResponseStatusException proibido(String m){return new ResponseStatusException(HttpStatus.FORBIDDEN,m);}
}
