package com.example.siggtrat.dto;

import com.example.siggtrat.controlador.PessoaControlador;
import com.example.siggtrat.controlador.VinculoContratualControlador;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** COL-004:001[1]: estrutura e propriedades Jackson; não substitui teste de persistência ou HTTP. */
class ContratoPessoaSemDadosBancariosTest {
    @Test
    void entradaESaidaDePessoaNaoDeclaramCamposBancarios() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (Class<?> tipo : List.of(CadastroPessoaRequisicao.class, AtualizacaoPessoaOpcionalRequisicao.class,
                CorrecaoIdentidadePessoaRequisicao.class, CadastroVinculoContratualRequisicao.class,
                PessoaDTO.class, PessoaResumoDTO.class, CorrecaoPessoaDTO.class, VinculoContratualDTO.class, EnderecoDTO.class)) {
            assertThat(tipo.isRecord()).as("Contrato concreto inspecionado: %s", tipo.getName()).isTrue();
            Set<String> componentes = Arrays.stream(tipo.getRecordComponents()).map(RecordComponent::getName).collect(Collectors.toSet());
            assertThat(componentes).as("COL004 componentes de entrada/saída: %s", tipo.getName()).isNotEmpty();
            semCamposBancarios(componentes, tipo);
            semCamposBancarios(mapper.getSerializationConfig().introspect(mapper.constructType(tipo)).findProperties()
                    .stream().map(p -> p.getName()).collect(Collectors.toSet()), tipo);
            semCamposBancarios(mapper.getDeserializationConfig().introspect(mapper.constructType(tipo)).findProperties()
                    .stream().map(p -> p.getName()).collect(Collectors.toSet()), tipo);
        }
        assertThat(Arrays.stream(CadastroPessoaRequisicao.class.getRecordComponents()).map(RecordComponent::getName).toList())
                .contains("nome", "cpf", "rg", "ctpsSerie", "nitPisPasep", "dataNascimento", "email", "endereco");
        assertThat(Arrays.stream(PessoaDTO.class.getRecordComponents()).map(RecordComponent::getName).toList())
                .contains("id", "nome", "cpf", "rg", "ctpsSerie", "nitPisPasep", "dataNascimento", "email", "endereco");
        assertThat(Arrays.stream(CadastroVinculoContratualRequisicao.class.getRecordComponents())
                .filter(c -> c.getName().equals("pessoa")).map(RecordComponent::getType).toList())
                .containsExactly(CadastroPessoaRequisicao.class);
        assertThat(VinculoContratualControlador.class.getMethod("cadastrar", CadastroVinculoContratualRequisicao.class)
                .getParameterTypes()).containsExactly(CadastroVinculoContratualRequisicao.class);
        assertThat(PessoaControlador.class.getMethod("buscar", Long.class).getReturnType()).isEqualTo(PessoaDTO.class);
        assertThat(PessoaControlador.class.getMethod("atualizarOpcionais", Long.class, AtualizacaoPessoaOpcionalRequisicao.class)
                .getReturnType()).isEqualTo(PessoaDTO.class);
    }

    private void semCamposBancarios(Set<String> campos, Class<?> tipo) {
        assertThat(campos).as("COL-004:001[1] contrato %s sem banco/conta", tipo.getName()).isNotEmpty()
                .doesNotContain("banco", "conta", "contaBancaria", "dadosBancarios");
    }
}
