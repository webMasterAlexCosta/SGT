package com.example.siggtrat.servico;

import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

/** Leitura da cadeia documental e da proveniência das fotografias preservadas. */
@Service
@Transactional(readOnly=true)
public class ConsultaProvaHistoricaContratoServico {
    private final ProvaHistoricaContratoRepositorio provas;
    private final ProvaHistoricaComposicaoRepositorio itens;
    private final ProvaHistoricaAditamentoComposicaoRepositorio itensAditamento;
    private VersaoContratoNegocialRepositorio versoes;
    private TransferenciaGestaoContratoRepositorio transferencias;
    private ObjectProvider<AditamentoContratualServico> projecoes;
    private static final Set<String> CAMPOS_TRANSFERENCIA = Set.of("gestaoLocalId", "gestaoLocalNome",
            "tipoContratacaoId", "tipoContratacaoDescricao", "matriculaTipoContratacaoObrigatoria");
    @org.springframework.beans.factory.annotation.Autowired
    public void configurarLeitura(VersaoContratoNegocialRepositorio versoes,
                                  TransferenciaGestaoContratoRepositorio transferencias) {
        this.versoes=versoes; this.transferencias=transferencias;
    }
    @org.springframework.beans.factory.annotation.Autowired
    public void configurarProjecoes(ObjectProvider<AditamentoContratualServico> projecoes) {
        this.projecoes=projecoes;
    }
    public ConsultaProvaHistoricaContratoServico(ProvaHistoricaContratoRepositorio provas,
                                                 ProvaHistoricaComposicaoRepositorio itens) {
        this(provas,itens,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public ConsultaProvaHistoricaContratoServico(ProvaHistoricaContratoRepositorio provas,
            ProvaHistoricaComposicaoRepositorio itens,ProvaHistoricaAditamentoComposicaoRepositorio itensAditamento) {
        this.provas=provas; this.itens=itens; this.itensAditamento=itensAditamento;
    }
    public record Configuracao(String lotacaoNome, String jornadaDescricao, String cargoNome,
            String lotacaoMunicipio, String jornadaSemanal, boolean municipioComprovado,
            boolean jornadaComprovada, @com.fasterxml.jackson.annotation.JsonIgnore Map<String,ProvaCampoHistoricoDTO> provaCampos) {
        public Configuracao { provaCampos=Map.copyOf(provaCampos); }
        public Configuracao(String lotacaoNome,String jornadaDescricao,String cargoNome,
                            String lotacaoMunicipio,String jornadaSemanal,boolean municipioComprovado) {
            this(lotacaoNome,jornadaDescricao,cargoNome,lotacaoMunicipio,jornadaSemanal,municipioComprovado,
                    jornadaSemanal!=null&&!jornadaSemanal.isBlank(),Map.of());
        }
        /** Fotografias originais preservam município, inclusive ausência, junto da semana completa. */
        public Configuracao(String lotacaoNome,String jornadaDescricao,String cargoNome,
                            String lotacaoMunicipio,String jornadaSemanal) {
            this(lotacaoNome,jornadaDescricao,cargoNome,lotacaoMunicipio,jornadaSemanal,
                    lotacaoMunicipio!=null||jornadaSemanal!=null&&!jornadaSemanal.isBlank());
        }
    }

    public Optional<ProvaHistoricaContrato> vigente(Long evento, Instant conhecido) {
        return vigente(evento, conhecido, null);
    }
    public Optional<ProvaHistoricaContrato> vigente(Long evento, Instant conhecido, LeituraHistoricaContratos entradas) {
        List<ProvaHistoricaContrato> cadeia=provasDoEvento(evento, conhecido, entradas);
        ProvaHistoricaContrato anterior=null;
        for (var prova:cadeia) {
            if (prova.getNumeroRevisao()!=(anterior==null?1:anterior.getNumeroRevisao()+1)
                    || !Objects.equals(prova.getProvaAnterior()==null?null:prova.getProvaAnterior().getId(),
                                        anterior==null?null:anterior.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Cadeia documental histórica incompleta ou divergente.");
            }
            anterior=prova;
        }
        return Optional.ofNullable(anterior);
    }
    public Optional<VersaoContratoNegocial> original(Long contrato,Instant conhecido) {
        return original(contrato, conhecido, null);
    }
    public Optional<VersaoContratoNegocial> original(Long contrato, Instant conhecido, LeituraHistoricaContratos entradas) {
        var origens=(entradas != null && entradas.contemContrato(contrato)
                ? entradas.provasDoContrato(contrato, corte(conhecido))
                : provas.findByContratoIdAndRegistradoEmLessThanEqualOrderByNumeroRevisaoAsc(contrato,corte(conhecido)))
                .stream().filter(p->p.getEventoOrigem().getTipoEvento()==TipoEventoCicloVidaContrato.PUBLICACAO)
                .map(p->p.getEventoOrigem().getId()).distinct().toList();
        if(origens.size()>1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Mais de uma origem documental de publicação exige conciliação explícita.");
        return origens.isEmpty()?Optional.empty():vigente(origens.get(0),conhecido,entradas).map(ProvaHistoricaContrato::getVersaoGerada);
    }
    public Map<Long,VersaoContratoNegocialDTO> selecionarFotografias(List<VersaoContratoNegocialDTO> conhecidas,
                                                                   Instant conhecido) {
        return selecionarFotografias(conhecidas, conhecido, null);
    }
    public Map<Long,VersaoContratoNegocialDTO> selecionarFotografias(List<VersaoContratoNegocialDTO> conhecidas,
                                                                   Instant conhecido, LeituraHistoricaContratos entradas) {
        Map<Long,VersaoContratoNegocialDTO> resultado=new LinkedHashMap<>();
        for(var foto:conhecidas) if(foto.eventoOrigemId()!=null) resultado.putIfAbsent(foto.eventoOrigemId(),foto);
        for(var evento:new ArrayList<>(resultado.keySet())) vigente(evento,conhecido,entradas).ifPresent(p->{
            conhecidas.stream().filter(f->Objects.equals(f.id(),p.getVersaoGerada().getId()))
                    .findFirst().ifPresent(f->resultado.put(evento,f));
        });
        return resultado;
    }
    public boolean versaoSelecionada(VersaoContratoNegocial foto, Instant conhecido) {
        return versaoSelecionada(foto, conhecido, null);
    }
    public boolean versaoSelecionada(VersaoContratoNegocial foto, Instant conhecido, LeituraHistoricaContratos entradas) {
        if(foto.getEventoOrigem()==null) return true;
        return vigente(foto.getEventoOrigem().getId(),conhecido,entradas)
                .map(p->Objects.equals(p.getVersaoGerada().getId(),foto.getId())).orElse(true);
    }
    public boolean campoComprovado(Long evento,String campo,Instant conhecido) {
        return campoComprovado(evento, campo, conhecido, null);
    }
    public boolean campoComprovado(Long evento,String campo,Instant conhecido,LeituraHistoricaContratos entradas) {
        vigente(evento,conhecido,entradas);
        return provasDoEvento(evento,conhecido,entradas)
                .stream().anyMatch(p->Arrays.asList(p.getCamposComprovados().split(",")).contains(campo));
    }
    /** Só fontes persistidas entram aqui; projeções recebem evidência durante o replay. */
    public Map<String,ProvaCampoHistoricoDTO> camposFotografia(VersaoContratoNegocialDTO foto) {
        return camposFotografia(foto,false,null);
    }
    public Map<String,ProvaCampoHistoricoDTO> camposFotografia(VersaoContratoNegocialDTO foto, LeituraHistoricaContratos entradas) {
        return camposFotografia(foto,false,entradas);
    }

    /**
     * Fronteira usada DENTRO do replay de transferência: lê somente os campos
     * próprios do ato e suas máscaras, sem reconstruir a fotografia derivada.
     * Isso impede a recursão fotografia -> replay -> mesma fotografia.
     */
    public Map<String,ProvaCampoHistoricoDTO> camposDiretosTransferencia(VersaoContratoNegocialDTO foto) {
        return camposFotografia(foto,true,null);
    }
    public Map<String,ProvaCampoHistoricoDTO> camposDiretosTransferencia(VersaoContratoNegocialDTO foto,LeituraHistoricaContratos entradas) {
        return camposFotografia(foto,true,entradas);
    }

    private Map<String,ProvaCampoHistoricoDTO> camposFotografia(VersaoContratoNegocialDTO foto,
                                                               boolean somenteTransferencia,LeituraHistoricaContratos entradas) {
        if(foto==null)return Map.of();
        Map<String,Object> valores=ProvasCamposHistoricos.valores(foto);
        Set<String> campos=somenteTransferencia?CAMPOS_TRANSFERENCIA:valores.keySet();
        if(foto.id()==null)return ProvasCamposHistoricos.desconhecidos(campos);
        var prova=provaDaVersao(foto.id(),entradas).orElse(null);
        if(prova!=null) {
            vigente(prova.getEventoOrigem().getId(),prova.getRegistradoEm(),entradas);
            List<ProvaHistoricaContrato> cadeia=new ArrayList<>();
            Set<Long> vistos=new HashSet<>();
            for(var p=prova;p!=null;p=p.getProvaAnterior()) {
                if(!vistos.add(p.getId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Cadeia documental cíclica.");
                cadeia.add(p);
            }
            Collections.reverse(cadeia);
            var original=cadeia.get(0).getVersaoAnterior();
            Map<String,ProvaCampoHistoricoDTO> resultado=original==null
                    ?ProvasCamposHistoricos.desconhecidos(campos)
                    :new LinkedHashMap<>(camposFotografia(ProvasCamposHistoricos.fotografia(original),somenteTransferencia,entradas));
            for(var p:cadeia) {
                var declarados=ProvasCamposHistoricos.valores(ProvasCamposHistoricos.fotografia(p.getVersaoGerada()));
                for(String campo:camposDeclarados(p)) if(campos.contains(campo))
                    resultado.put(campo,ProvasCamposHistoricos.comprovado(declarados.get(campo),
                            ProvasCamposHistoricos.prova(p,null,null,null)));
            }
            return Map.copyOf(resultado);
        }

        var persistida=entradas!=null&&entradas.contemVersao(foto.id())?entradas.versao(foto.id()).orElse(null)
                :versoes==null?null:versoes.findById(foto.id()).orElse(null);
        if(persistida==null)return ProvasCamposHistoricos.desconhecidos(campos);
        if(somenteTransferencia)return camposOriginaisTransferencia(foto,persistida);
        var evento=persistida.getEventoOrigem();
        // O ordinal identifica a linha do acervo, não autentica uma publicação.
        boolean original=evento!=null&&evento.getTipoEvento()==TipoEventoCicloVidaContrato.PUBLICACAO;
        return original?camposOriginais(foto):camposDerivados(foto,persistida,entradas);
    }

    /**
     * HIS-006: o comando PUBLICACAO captura a fotografia integral pelo motor.
     * O marcador legado de configuração só legitima ausências opcionais nesse
     * snapshot original vinculado; valores copiados por outro ato não o herdam.
     */
    private Map<String,ProvaCampoHistoricoDTO> camposOriginais(VersaoContratoNegocialDTO foto) {
        Map<String,Object> valores=ProvasCamposHistoricos.valores(foto);
        Map<String,ProvaCampoHistoricoDTO> resultado=ProvasCamposHistoricos.desconhecidos(valores.keySet());
        var origem=ProvasCamposHistoricos.versao(foto);
        valores.forEach((campo,valor)->{
            boolean ausenciaConfiguracao=foto.matriculaVinculoObrigatoriaEfetiva()!=null
                    &&Set.of("matriculaVinculoObrigatoria","diaSemanaReduzido","horasDiaReduzido").contains(campo);
            if(!ProvasCamposHistoricos.ausente(valor)||ausenciaConfiguracao)
                resultado.put(campo,ProvasCamposHistoricos.comprovado(valor,origem));
        });
        return Map.copyOf(resultado);
    }

    private Map<String,ProvaCampoHistoricoDTO> camposOriginaisTransferencia(VersaoContratoNegocialDTO foto,
                                                                          VersaoContratoNegocial persistida) {
        var resultado=ProvasCamposHistoricos.desconhecidos(CAMPOS_TRANSFERENCIA);
        if(persistida.getEventoOrigem()==null
                ||persistida.getEventoOrigem().getTipoEvento()!=TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL)
            return resultado;
        var valores=ProvasCamposHistoricos.valores(foto);
        var origem=ProvasCamposHistoricos.versao(foto);
        for(String campo:CAMPOS_TRANSFERENCIA) if(!ProvasCamposHistoricos.ausente(valores.get(campo)))
            resultado.put(campo,ProvasCamposHistoricos.comprovado(valores.get(campo),origem));
        // Uma fotografia antiga podia carregar o Tipo da origem; só o mapa explícito
        // da transferência ou uma máscara documental prova seu Tipo de destino.
        var t=transferencias==null?null:transferencias.findByEventoId(foto.eventoOrigemId()).orElse(null);
        if(t==null||t.getTipoContratacaoDestino()==null||t.getTipoContratacaoDestino().getGestaoLocal()==null
                ||!Objects.equals(t.getTipoContratacaoDestino().getGestaoLocal().getId(),foto.gestaoLocalId()))
            for(String campo:List.of("tipoContratacaoId","tipoContratacaoDescricao","matriculaTipoContratacaoObrigatoria"))
                resultado.put(campo,ProvasCamposHistoricos.desconhecido());
        return Map.copyOf(resultado);
    }

    private Map<String,ProvaCampoHistoricoDTO> camposDerivados(VersaoContratoNegocialDTO foto,
                                                              VersaoContratoNegocial persistida,LeituraHistoricaContratos entradas) {
        var valores=ProvasCamposHistoricos.valores(foto);
        var resultado=ProvasCamposHistoricos.desconhecidos(valores.keySet());
        var projetor=projecoes==null?null:projecoes.getIfAvailable();
        if(projetor==null||foto.dataEfetiva()==null||foto.registradoEm()==null||persistida.getEventoOrigem()==null)
            return resultado;
        // A materialização tem seu próprio K. Fontes incorporadas depois dela
        // não transformam retroativamente lacunas em valores/ausências provados.
        var leitura=entradas==null?projetor.projetarLeituraContrato(persistida.getContrato(),foto.dataEfetiva(),foto.registradoEm())
                :projetor.projetarLeituraContrato(persistida.getContrato(),foto.dataEfetiva(),foto.registradoEm(),entradas);
        var reconstruidos=ProvasCamposHistoricos.valores(leitura.dados());
        valores.forEach((campo,valor)->{
            if(Objects.equals(valor,reconstruidos.get(campo))) resultado.put(campo,
                    leitura.provaCampos().getOrDefault(campo,ProvasCamposHistoricos.desconhecido()));
        });
        // A data de referência é um dado próprio da fotografia preservada;
        // não pressupõe a integralidade dos dados de negócio nela materializados.
        resultado.put("dataEfetiva",ProvasCamposHistoricos.comprovado(foto.dataEfetiva(),ProvasCamposHistoricos.versao(foto)));
        return Map.copyOf(resultado);
    }
    public Set<String> camposDeclarados(ProvaHistoricaContrato p) {
        if(p.getCamposComprovados()==null||p.getCamposComprovados().isBlank())return Set.of();
        return Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(p.getCamposComprovados().split(","))));
    }
    public VersaoContratoLeituraDTO leituraFotografia(VersaoContratoNegocial foto) {
        var dados=ProvasCamposHistoricos.fotografia(foto);
        return new VersaoContratoLeituraDTO(dados,camposFotografia(dados));
    }
    public List<FragmentoProvaHistoricaComposicaoDTO> fragmentos(ProvaHistoricaContrato p) {
        List<FragmentoProvaHistoricaComposicaoDTO> resultado=new ArrayList<>();
        for(var i:itens.findByProvaId(p.getId()))resultado.add(fragmento(p,
                i.getComposicao()==null?null:i.getComposicao().getId(),i.getMapeamento()==null?null:i.getMapeamento().getId(),null,
                i.getLotacaoNome(),i.getJornadaDescricao(),i.getCargoNome(),i.getLotacaoMunicipio(),
                i.getMunicipioComprovado(),i.getJornadaSemanal(),i.getJornadaComprovada()));
        if(itensAditamento!=null)for(var i:itensAditamento.findByProvaId(p.getId()))resultado.add(fragmento(p,null,null,
                i.getAditamentoComposicao().getId(),i.getLotacaoNome(),i.getJornadaDescricao(),i.getCargoNome(),
                i.getLotacaoMunicipio(),i.getMunicipioComprovado(),i.getJornadaSemanal(),i.getJornadaComprovada()));
        return List.copyOf(resultado);
    }
    private FragmentoProvaHistoricaComposicaoDTO fragmento(ProvaHistoricaContrato p,Long composicao,Long mapa,Long item,
            String lotacao,String jornada,String cargo,String municipio,boolean municipioComprovado,String semana,boolean jornadaComprovada) {
        var campos=camposConfiguracao(lotacao,jornada,cargo,municipio,municipioComprovado,semana,jornadaComprovada,
                ProvasCamposHistoricos.prova(p,composicao,item,mapa));
        return new FragmentoProvaHistoricaComposicaoDTO(composicao,mapa,item,lotacao,jornada,cargo,municipio,
                municipioComprovado,semana,jornadaComprovada,campos);
    }
    /** Conhecimento é por propriedade; a prova de matrícula não comprova ausência de dia reduzido. */
    public boolean campoConhecidoNaFotografia(VersaoContratoNegocialDTO foto,String campo,boolean conhecidoLegado) {
        return campoConhecidoNaFotografia(foto,campo,conhecidoLegado,null);
    }
    public boolean campoConhecidoNaFotografia(VersaoContratoNegocialDTO foto,String campo,boolean conhecidoLegado,
                                               LeituraHistoricaContratos entradas) {
        if(foto==null||foto.id()==null)return conhecidoLegado;
        var prova=provaDaVersao(foto.id(),entradas).orElse(null);
        if(prova==null)return conhecidoLegado;
        Set<Long> vistos=new HashSet<>();
        ProvaHistoricaContrato raiz=prova;
        for(var p=prova;p!=null&&vistos.add(p.getId());p=p.getProvaAnterior()) {
            if(Arrays.asList(p.getCamposComprovados().split(",")).contains(campo))return true;
            raiz=p;
        }
        var original=raiz.getVersaoAnterior();
        if(original==null)return false;
        return switch(campo) {
            case "diaSemanaReduzido", "horasDiaReduzido" -> original.getMatriculaVinculoObrigatoriaEfetiva()!=null
                    ||original.getDiaSemanaReduzido()!=null||original.getHorasDiaReduzido()!=null;
            case "matriculaVinculoObrigatoria" -> original.getMatriculaVinculoObrigatoriaEfetiva()!=null
                    ||original.getMatriculaVinculoObrigatoria()!=null;
            default -> false;
        };
    }
    public Configuracao configuracao(ComposicaoContratual composicao, Instant conhecido) {
        return configuracao(composicao,conhecido,null);
    }
    public Configuracao configuracao(ComposicaoContratual composicao, Instant conhecido, LeituraHistoricaContratos entradas) {
        Configuracao atual=configuracaoOriginal(limpar(composicao.getLotacaoNomeFotografia()),
                limpar(composicao.getJornadaDescricaoFotografia()),limpar(composicao.getCargoNomeFotografia()),
                composicao.getLotacaoMunicipioFotografia(),composicao.getJornadaSemanalFotografia(),origemComposicao(composicao,conhecido,entradas));
        var foto=original(composicao.getContrato().getId(),conhecido,entradas).orElse(null);
        if(foto==null||foto.getEventoOrigem()==null)return atual;
        return complementar(atual,foto.getEventoOrigem().getId(),conhecido,composicao.getId(),null,entradas);
    }
    public Configuracao configuracao(MapeamentoTransferenciaGestao mapa, Instant conhecido) {
        return configuracao(mapa,conhecido,null);
    }
    public Configuracao configuracao(MapeamentoTransferenciaGestao mapa, Instant conhecido, LeituraHistoricaContratos entradas) {
        Configuracao atual=configuracaoOriginal(limpar(mapa.getLotacaoDestinoNomeFotografia()),
                limpar(mapa.getJornadaDestinoDescricaoFotografia()),limpar(mapa.getCargoDestinoNomeFotografia()),
                mapa.getLotacaoDestinoMunicipioFotografia(),mapa.getJornadaDestinoSemanalFotografia(),ProvasCamposHistoricos.mapa(mapa));
        if(mapa.getTransferencia()==null||mapa.getTransferencia().getEvento()==null)return atual;
        return complementar(atual,mapa.getTransferencia().getEvento().getId(),conhecido,null,mapa.getId(),entradas);
    }
    public Configuracao configuracao(AditamentoComposicao item, Instant conhecido) {
        return configuracao(item,conhecido,null);
    }
    public Configuracao configuracao(AditamentoComposicao item, Instant conhecido, LeituraHistoricaContratos entradas) {
        Configuracao atual=configuracaoOriginal(limpar(item.getLotacaoNomeFotografia()),
                limpar(item.getJornadaDescricaoFotografia()),limpar(item.getCargoNomeFotografia()),
                item.getLotacaoMunicipioFotografia(),item.getJornadaSemanalFotografia(),ProvasCamposHistoricos.item(item));
        for(var fragmento:fragmentosAditamento(item,conhecido,entradas)) {
            var evidencias=incorporarCampos(atual.provaCampos(),fragmento.getLotacaoNome(),fragmento.getJornadaDescricao(),
                    fragmento.getCargoNome(),fragmento.getLotacaoMunicipio(),fragmento.getMunicipioComprovado(),
                    fragmento.getJornadaSemanal(),fragmento.getJornadaComprovada(),
                    ProvasCamposHistoricos.prova(fragmento.getProva(),null,item.getId(),null));
            atual=new Configuracao(
                    fragmento.getLotacaoNome()!=null?fragmento.getLotacaoNome():atual.lotacaoNome(),
                    fragmento.getJornadaDescricao()!=null?fragmento.getJornadaDescricao():atual.jornadaDescricao(),
                    fragmento.getCargoNome()!=null?fragmento.getCargoNome():atual.cargoNome(),
                    fragmento.getMunicipioComprovado()?fragmento.getLotacaoMunicipio():atual.lotacaoMunicipio(),
                    fragmento.getJornadaComprovada()?fragmento.getJornadaSemanal():atual.jornadaSemanal(),
                    fragmento.getMunicipioComprovado()||atual.municipioComprovado(),
                    fragmento.getJornadaComprovada()||atual.jornadaComprovada(),evidencias);
        }
        return atual;
    }
    /** O ID vem da proveniência do replay, nunca de uma busca pela coincidência dos catálogos. */
    public Optional<Configuracao> configuracaoAditamentoComprovada(Long itemId,Instant conhecido) {
        if(itemId==null||itensAditamento==null)return Optional.empty();
        var primeiro=itensAditamento.primeiroConhecido(itemId,corte(conhecido)).orElse(null);
        if(primeiro==null)return Optional.empty();
        var item=primeiro.getAditamentoComposicao();
        // A origem exata e a cadeia documental habilitam apenas as propriedades
        // comprovadas. Grade conhecida não depende de prova municipal; o flag
        // e o valor municipais continuam desconhecidos até sua própria fonte.
        return Optional.of(configuracao(item,conhecido));
    }
    private List<ProvaHistoricaAditamentoComposicao> fragmentosAditamento(AditamentoComposicao item,Instant conhecido,
                                                                         LeituraHistoricaContratos entradas) {
        if(itensAditamento==null||item.getAditamento()==null||item.getAditamento().getEvento()==null)return List.of();
        Long evento=item.getAditamento().getEvento().getId();
        vigente(evento,conhecido,entradas);
        return provasDoEvento(evento,conhecido,entradas)
                .stream().flatMap(p->itensAditamento.findByProvaId(p.getId()).stream())
                .filter(f->Objects.equals(f.getAditamentoComposicao().getId(),item.getId())).toList();
    }
    public boolean mapeamentoPossuiProva(MapeamentoTransferenciaGestao mapa,Instant conhecido) {
        return mapa.getTransferencia()!=null && mapa.getTransferencia().getEvento()!=null
                && itemPossuiFragmentoComprovado(mapa.getTransferencia().getEvento().getId(),null,mapa.getId(),conhecido);
    }
    public boolean composicaoPossuiProva(ComposicaoContratual composicao,Instant conhecido) {
        var origem=original(composicao.getContrato().getId(),conhecido).orElse(null);
        return origem!=null && origem.getEventoOrigem()!=null
                && itemPossuiFragmentoComprovado(origem.getEventoOrigem().getId(),composicao.getId(),null,conhecido);
    }
    private boolean itemPossuiFragmentoComprovado(Long evento,Long composicao,Long mapa,Instant conhecido) {
        vigente(evento,conhecido);
        var fragmentos=provas.findByEventoOrigemIdAndRegistradoEmLessThanEqualOrderByNumeroRevisaoAsc(evento,corte(conhecido))
                .stream().flatMap(p->itens.findByProvaId(p.getId()).stream())
                .filter(i->composicao!=null ? i.getComposicao()!=null&&Objects.equals(i.getComposicao().getId(),composicao)
                                :i.getMapeamento()!=null&&Objects.equals(i.getMapeamento().getId(),mapa)).toList();
        // Cada propriedade é incorporada separadamente por complementar().
        // Uma prova de grade pode completar a fotografia que já preservava
        // município; não exige provar de novo uma propriedade conhecida.
        // A presença do fragmento não transforma campos ausentes em prova.
        return !fragmentos.isEmpty();
    }
    private Configuracao complementar(Configuracao atual,Long evento,Instant conhecido,Long composicao,Long mapa,
                                       LeituraHistoricaContratos entradas) {
        vigente(evento,conhecido,entradas); // Verifica encadeamento antes de usar qualquer fragmento.
        for(var prova:provasDoEvento(evento,conhecido,entradas)) {
            for(var item:itens.findByProvaId(prova.getId())) {
                boolean corresponde=composicao!=null?item.getComposicao()!=null&&Objects.equals(item.getComposicao().getId(),composicao)
                        :item.getMapeamento()!=null&&Objects.equals(item.getMapeamento().getId(),mapa);
                if(corresponde) {
                    var evidencias=incorporarCampos(atual.provaCampos(),item.getLotacaoNome(),item.getJornadaDescricao(),
                            item.getCargoNome(),item.getLotacaoMunicipio(),item.getMunicipioComprovado(),
                            item.getJornadaSemanal(),item.getJornadaComprovada(),
                            ProvasCamposHistoricos.prova(prova,composicao,null,mapa));
                    atual=new Configuracao(
                        item.getLotacaoNome()!=null?item.getLotacaoNome():atual.lotacaoNome(),
                        item.getJornadaDescricao()!=null?item.getJornadaDescricao():atual.jornadaDescricao(),
                        item.getCargoNome()!=null?item.getCargoNome():atual.cargoNome(),
                        item.getMunicipioComprovado()?item.getLotacaoMunicipio():atual.lotacaoMunicipio(),
                        item.getJornadaComprovada()?item.getJornadaSemanal():atual.jornadaSemanal(),
                        item.getMunicipioComprovado()||atual.municipioComprovado(),
                        item.getJornadaComprovada()||atual.jornadaComprovada(),evidencias);
                }
            }
        }
        return atual;
    }
    OrigemProvaCampoDTO origemComposicao(ComposicaoContratual c,Instant conhecido) {
        return origemComposicao(c,conhecido,null);
    }
    OrigemProvaCampoDTO origemComposicao(ComposicaoContratual c,Instant conhecido,LeituraHistoricaContratos entradas) {
        if(c.getId()==null||versoes==null)return null;
        var original=(entradas!=null&&entradas.contemContrato(c.getContrato().getId())
                ?entradas.versoesConhecidas(c.getContrato().getId(),corte(conhecido))
                :versoes.findByContratoIdAndRegistradoEmLessThanEqualOrderByNumeroVersaoAsc(c.getContrato().getId(),corte(conhecido)))
                .stream().filter(v->v.getEventoOrigem()!=null&&v.getEventoOrigem().getTipoEvento()==TipoEventoCicloVidaContrato.PUBLICACAO
                        &&v.getEventoOrigem().getRegistradoEm()!=null&&!v.getEventoOrigem().getRegistradoEm().isAfter(corte(conhecido)))
                .findFirst().orElse(null);
        return original==null?null:new OrigemProvaCampoDTO(OrigemProvaCampoDTO.Tipo.COMPOSICAO_PUBLICADA,null,
                original.getEventoOrigem().getId(),original.getId(),c.getId(),null,null,
                c.getDataInicioVigencia(),original.getRegistradoEm());
    }
    private Configuracao configuracaoOriginal(String lotacao,String jornada,String cargo,String municipio,String semana,OrigemProvaCampoDTO fonte) {
        boolean grade=semana!=null&&!semana.isBlank();
        boolean municipal=municipio!=null||grade;
        return new Configuracao(lotacao,jornada,cargo,municipio,semana,municipal,grade,
                camposConfiguracao(lotacao,jornada,cargo,municipio,municipal,semana,grade,fonte));
    }
    private Map<String,ProvaCampoHistoricoDTO> camposConfiguracao(String lotacao,String jornada,String cargo,String municipio,
            boolean municipal,String semana,boolean grade,OrigemProvaCampoDTO fonte) {
        return incorporarCampos(ProvasCamposHistoricos.desconhecidos(List.of("lotacaoNome","jornadaDescricao","cargoNome",
                "lotacaoMunicipio","jornadaSemanal")),lotacao,jornada,cargo,municipio,municipal,semana,grade,fonte);
    }
    private Map<String,ProvaCampoHistoricoDTO> incorporarCampos(Map<String,ProvaCampoHistoricoDTO> anteriores,String lotacao,
            String jornada,String cargo,String municipio,boolean municipal,String semana,boolean grade,OrigemProvaCampoDTO fonte) {
        Map<String,ProvaCampoHistoricoDTO> campos=new LinkedHashMap<>(anteriores);
        if(limpar(lotacao)!=null)campos.put("lotacaoNome",ProvasCamposHistoricos.comprovado(lotacao,fonte));
        if(limpar(jornada)!=null)campos.put("jornadaDescricao",ProvasCamposHistoricos.comprovado(jornada,fonte));
        if(limpar(cargo)!=null)campos.put("cargoNome",ProvasCamposHistoricos.comprovado(cargo,fonte));
        if(municipal)campos.put("lotacaoMunicipio",ProvasCamposHistoricos.comprovado(municipio,fonte));
        if(grade)campos.put("jornadaSemanal",ProvasCamposHistoricos.comprovado(semana,fonte));
        return Map.copyOf(campos);
    }
    List<ProvaHistoricaContrato> carregarProvas(Collection<Long> contratos) {
        return contratos.isEmpty()?List.of():provas.findByContratoIdInOrderByContratoIdAscNumeroRevisaoAsc(contratos);
    }
    private List<ProvaHistoricaContrato> provasDoEvento(Long evento,Instant conhecido,LeituraHistoricaContratos entradas) {
        return entradas!=null&&entradas.contemEvento(evento)?entradas.provasDoEvento(evento,corte(conhecido))
                :provas.findByEventoOrigemIdAndRegistradoEmLessThanEqualOrderByNumeroRevisaoAsc(evento,corte(conhecido));
    }
    private Optional<ProvaHistoricaContrato> provaDaVersao(Long versao,LeituraHistoricaContratos entradas) {
        return entradas!=null&&entradas.contemVersao(versao)?entradas.provaDaVersao(versao):provas.findByVersaoGeradaId(versao);
    }
    private static Instant corte(Instant valor){return valor==null?Instant.now():valor;}
    private static String limpar(String valor){return valor==null||valor.isBlank()?null:valor;}
}
