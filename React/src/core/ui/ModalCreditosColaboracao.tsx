import React, { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import {
  X,
  Code2,
  Server,
  Layout,
  Database,
  ShieldCheck,
  CheckCircle2,
  Quote,
  Heart,
} from "lucide-react";
import { CONFIG } from "../config";
import logoCorreios from "../../assets/logo-footer.png";
import estilos from "./ModalCreditosColaboracao.module.css";
import manifestoFrontend from "../../../package.json";

const dependenciasFrontend = {
  ...manifestoFrontend.dependencies,
  ...manifestoFrontend.devDependencies,
};

const PILHA_DE_TECNOLOGIAS = [
  // Frontend e interface
  {
    categoria: "Frontend e interface",
    nome: `React ${dependenciasFrontend.react}`,
    papel: "Interface / componentes",
    icone: <Layout size={20} />,
  },
  {
    categoria: "Frontend e interface",
    nome: `React DOM ${dependenciasFrontend["react-dom"]}`,
    papel: "Renderização da aplicação",
    icone: <Layout size={20} />,
  },
  {
    categoria: "Frontend e interface",
    nome: `React Router DOM ${dependenciasFrontend["react-router-dom"]}`,
    papel: "Rotas e navegação SPA",
    icone: <Layout size={20} />,
  },
  {
    categoria: "Frontend e interface",
    nome: `Axios ${dependenciasFrontend.axios}`,
    papel: "Cliente HTTP / API REST",
    icone: <Code2 size={20} />,
  },
  {
    categoria: "Frontend e interface",
    nome: `React Toastify ${dependenciasFrontend["react-toastify"]}`,
    papel: "Dependência instalada; avisos atuais usam componente próprio",
    icone: <Layout size={20} />,
  },
  {
    categoria: "Frontend e interface",
    nome: `Recharts ${dependenciasFrontend.recharts}`,
    papel: "Gráficos e indicadores",
    icone: <Layout size={20} />,
  },
  {
    categoria: "Frontend e interface",
    nome: `Lucide React ${dependenciasFrontend["lucide-react"]}`,
    papel: "Biblioteca de ícones",
    icone: <Layout size={20} />,
  },
  {
    categoria: "Frontend e interface",
    nome: "CSS Modules",
    papel: "Estilos encapsulados por componente",
    icone: <Layout size={20} />,
  },

  // Build e execução
  {
    categoria: "Build e execução",
    nome: `Vite ${dependenciasFrontend.vite}`,
    papel: "Build e servidor de desenvolvimento",
    icone: <Code2 size={20} />,
  },
  {
    categoria: "Build e execução",
    nome: `@vitejs/plugin-react ${dependenciasFrontend["@vitejs/plugin-react"]}`,
    papel: "Integração React com Vite",
    icone: <Code2 size={20} />,
  },
  {
    categoria: "Build e execução",
    nome: `@vitejs/plugin-basic-ssl ${dependenciasFrontend["@vitejs/plugin-basic-ssl"]}`,
    papel: "Dependência instalada; HTTPS atual usa PFX configurado",
    icone: <ShieldCheck size={20} />,
  },
  {
    categoria: "Build e execução",
    nome: `Node.js ${manifestoFrontend.engines.node}`,
    papel: "Runtime do frontend e scripts",
    icone: <Code2 size={20} />,
  },
  {
    categoria: "Build e execução",
    nome: manifestoFrontend.packageManager.replace("@", " "),
    papel: "Gerenciamento de dependências frontend",
    icone: <Code2 size={20} />,
  },
  {
    categoria: "Build e execução",
    nome: "Maven",
    papel: "Build e dependências do backend",
    icone: <Server size={20} />,
  },
  {
    categoria: "Build e execução",
    nome: "Spring Boot Maven Plugin",
    papel: "Empacotamento e execução da aplicação",
    icone: <Server size={20} />,
  },
  {
    categoria: "Build e execução",
    nome: "Maven Compiler Plugin",
    papel: "Compilação Java 25",
    icone: <Code2 size={20} />,
  },
  {
    categoria: "Build e execução",
    nome: "Maven Surefire",
    papel: "Execução de testes unitários",
    icone: <CheckCircle2 size={20} />,
  },
  {
    categoria: "Build e execução",
    nome: "Maven Failsafe",
    papel: "Execução de testes de integração",
    icone: <CheckCircle2 size={20} />,
  },

  // Backend e arquitetura
  {
    categoria: "Backend e arquitetura",
    nome: "Java 25",
    papel: "Linguagem principal do backend",
    icone: <Server size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Spring Boot 4.1.2",
    papel: "Plataforma principal da API",
    icone: <Server size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Spring Web / MVC",
    papel: "Controllers e API REST",
    icone: <Server size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Spring Data JPA",
    papel: "Persistência e repositórios",
    icone: <Database size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Hibernate ORM",
    papel: "Mapeamento objeto-relacional",
    icone: <Database size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Spring Security",
    papel: "Autenticação e autorização",
    icone: <ShieldCheck size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Spring Validation",
    papel: "Validação de DTOs e regras de entrada",
    icone: <CheckCircle2 size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Jackson Databind",
    papel: "Serialização e desserialização JSON",
    icone: <Code2 size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Jackson JSR-310",
    papel: "Datas e horários Java Time",
    icone: <Code2 size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Lombok 1.18.46",
    papel: "Redução de código repetitivo",
    icone: <Code2 size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Spring Cache",
    papel: "Abstração de cache da aplicação",
    icone: <Server size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Spring Data Redis",
    papel: "Integração do backend com Redis",
    icone: <Database size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Spring AOP / AspectJ",
    papel: "Aspectos e invalidação de cache",
    icone: <Server size={20} />,
  },
  {
    categoria: "Backend e arquitetura",
    nome: "Spring Retry 2.0.13",
    papel: "Resiliência e repetição controlada",
    icone: <Server size={20} />,
  },

  // Dados e segurança
  {
    categoria: "Dados e segurança",
    nome: "MySQL",
    papel: "Banco de dados relacional",
    icone: <Database size={20} />,
  },
  {
    categoria: "Dados e segurança",
    nome: "MySQL Connector/J",
    papel: "Driver JDBC do MySQL",
    icone: <Database size={20} />,
  },
  {
    categoria: "Dados e segurança",
    nome: "Redis",
    papel: "Cache e aceleração de consultas",
    icone: <Database size={20} />,
  },
  {
    categoria: "Dados e segurança",
    nome: "JJWT 0.13.0",
    papel: "Tokens JWT",
    icone: <ShieldCheck size={20} />,
  },
  {
    categoria: "Dados e segurança",
    nome: "Bucket4j 8.10.1",
    papel: "Rate limiting / proteção da API",
    icone: <ShieldCheck size={20} />,
  },
  {
    categoria: "Dados e segurança",
    nome: "Google Authenticator 1.5.0",
    papel: "TOTP / autenticação em dois fatores",
    icone: <ShieldCheck size={20} />,
  },
  {
    categoria: "Dados e segurança",
    nome: "ZXing 3.5.4",
    papel: "Geração de QR Code para 2FA",
    icone: <ShieldCheck size={20} />,
  },

  // Relatórios, documentos e fotografias
  {
    categoria: "Relatórios e fotografias",
    nome: `@vladmandic/face-api ${dependenciasFrontend["@vladmandic/face-api"]}`,
    papel: "Detecção facial para enquadramento de fotografia",
    icone: <ShieldCheck size={20} />,
  },
  {
    categoria: "Relatórios e fotografias",
    nome: `jsPDF ${dependenciasFrontend.jspdf}`,
    papel: "Geração de PDF no frontend",
    icone: <Code2 size={20} />,
  },
  {
    categoria: "Relatórios e fotografias",
    nome: `jsPDF AutoTable ${dependenciasFrontend["jspdf-autotable"]}`,
    papel: "Tabelas formatadas em PDF",
    icone: <Code2 size={20} />,
  },
  {
    categoria: "Relatórios e fotografias",
    nome: "OpenPDF 1.3.30",
    papel: "Geração de documentos PDF no backend",
    icone: <Server size={20} />,
  },
  {
    categoria: "Relatórios e fotografias",
    nome: "Office Open XML / XLSX",
    papel: "Exportação de planilhas Excel",
    icone: <Code2 size={20} />,
  },

  // Qualidade e testes
  {
    categoria: "Qualidade e testes",
    nome: "JUnit 5",
    papel: "Testes unitários e de integração",
    icone: <CheckCircle2 size={20} />,
  },
  {
    categoria: "Qualidade e testes",
    nome: "JUnit Platform Launcher",
    papel: "Execução da plataforma de testes",
    icone: <CheckCircle2 size={20} />,
  },
  {
    categoria: "Qualidade e testes",
    nome: "Mockito",
    papel: "Mocks e isolamento de dependências",
    icone: <CheckCircle2 size={20} />,
  },
  {
    categoria: "Qualidade e testes",
    nome: "Spring Boot Test",
    papel: "Infraestrutura de testes Spring",
    icone: <CheckCircle2 size={20} />,
  },
  {
    categoria: "Qualidade e testes",
    nome: "Spring Security Test",
    papel: "Testes de autenticação e autorização",
    icone: <CheckCircle2 size={20} />,
  },
  {
    categoria: "Qualidade e testes",
    nome: "Testcontainers",
    papel: "Testes com infraestrutura isolada",
    icone: <CheckCircle2 size={20} />,
  },
  {
    categoria: "Qualidade e testes",
    nome: "Testcontainers MySQL",
    papel: "Banco MySQL real em testes",
    icone: <Database size={20} />,
  },
  {
    categoria: "Qualidade e testes",
    nome: "ArchUnit 1.4.2",
    papel: "Testes de arquitetura e camadas",
    icone: <CheckCircle2 size={20} />,
  },
  {
    categoria: "Qualidade e testes",
    nome: "JaCoCo 0.8.15",
    papel: "Cobertura de testes",
    icone: <CheckCircle2 size={20} />,
  },
  {
    categoria: "Qualidade e testes",
    nome: "PIT Mutation Testing 1.25.7",
    papel: "Testes de mutação",
    icone: <CheckCircle2 size={20} />,
  },
  {
    categoria: "Qualidade e testes",
    nome: "PIT JUnit 5 Plugin 1.2.3",
    papel: "Integração PIT com JUnit 5",
    icone: <CheckCircle2 size={20} />,
  },
  {
    categoria: "Qualidade e testes",
    nome: "ASM 9.7.1",
    papel: "Dependência de teste para bytecode",
    icone: <Code2 size={20} />,
  },
];

export const ModalCreditosColaboracao = ({ isOpen: aberto, onClose: aoFechar }: { isOpen: boolean; onClose: () => void }) => {
  const [mostrarComponente, definirMostrarComponente] = useState(false);

  const tecnologiasPorCategoria = useMemo(
    () =>
      PILHA_DE_TECNOLOGIAS.reduce<Record<string, (typeof PILHA_DE_TECNOLOGIAS)[number][]>>((grupos, tecnologia) => {
        const categoria = tecnologia.categoria || "Outras tecnologias";
        if (!grupos[categoria]) grupos[categoria] = [];
        grupos[categoria].push(tecnologia);
        return grupos;
      }, {}),
    [],
  );

  // Animação de entrada e saída
  useEffect(() => {
    let temporizador: number | undefined;

    if (aberto) {
      definirMostrarComponente(true);
      document.body.style.overflow = "hidden";
    } else {
      temporizador = window.setTimeout(
        () => definirMostrarComponente(false),
        300,
      );
      document.body.style.overflow = "";
    }

    return () => {
      if (temporizador) window.clearTimeout(temporizador);
      document.body.style.overflow = "";
    };
  }, [aberto]);

  // Fechar ao apertar ESC
  useEffect(() => {
    const lidarComTecla = (evento: KeyboardEvent) => {
      if (evento.key === "Escape") aoFechar?.();
    };

    window.addEventListener("keydown", lidarComTecla);
    return () => window.removeEventListener("keydown", lidarComTecla);
  }, [aoFechar]);

  if (!aberto && !mostrarComponente) return null;

  return createPortal(
    <div
      className={`${estilos.sobreposicao} ${aberto ? estilos.sobreposicaoAberta : estilos.sobreposicaoFechada}`}
      onMouseDown={(evento) => {
        if (evento.target === evento.currentTarget) aoFechar?.();
      }}
      role="presentation"
    >
      <div
        className={`${estilos.conteinerModal} ${aberto ? estilos.modalAberto : estilos.modalFechado}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-creditos-sistema"
      >
        {/* === LADO ESQUERDO: MANIFESTO E IDENTIDADE VISUAL === */}
        <div className={estilos.painelEsquerdo}>
          <div className={estilos.cabecalhoIdentidade}>
            <div className={estilos.envoltorioLogo}>
              <img
                src={logoCorreios}
                alt="Correios"
                className={estilos.imagemLogo}
              />
            </div>
            <h2 id="titulo-creditos-sistema" className={estilos.tituloApp}>
              {CONFIG.APP_NAME}
            </h2>
            <p className={estilos.subtituloApp}>
              Gestão de Terceiros • Versão {manifestoFrontend.version}
            </p>
          </div>

          <div className={estilos.corpoManifesto}>
            <Quote className={estilos.iconeAspas} size={40} />
            <p className={estilos.textoManifesto}>
              Este sistema nasce não apenas de linhas de código, mas de um
              compromisso profundo com a inovação e a excelência. Ele é um
              tributo a todos os profissionais do Gestor estadual/RJ, cujo
              esforço diário nos inspira a buscar, incansavelmente, a
              eficiência operacional que nossa instituição merece. Neste
              caminho rumo à modernização, a segurança e a ética são pilares
              inegociáveis. Por isso, este projeto foi desenvolvido sob os mais
              rigorosos padrões de proteção, assegurando total conformidade com
              a LGPD.
              <br />
              <br />
              Um agradecimento especial ao Gerente da Gestão Local, {" "}
              <b>{CONFIG.AUTHOR_ELABORADOR_2}</b>,
              <b>{CONFIG.AUTHOR_ELABORADOR_1}</b>, que enxergaram potencial onde
              havia um desafio e me confiaram a missão de construir este
              caminho. A confiança depositada foi o combustível essencial para
              o meu crescimento e aprendizado.
            </p>
          </div>

          <div className={estilos.rodapeManifesto}>
            <Heart size={48} className={estilos.iconeCoracao} />
            <span className={estilos.textoRodape}>
              Feito com dedicação e muito carinho
            </span>
          </div>
        </div>

        {/* === LADO DIREITO: CONTEÚDO TÉCNICO ROLÁVEL === */}
        <div className={estilos.painelDireito}>
          <button
            type="button"
            onClick={aoFechar}
            className={estilos.botaoFecharFixo}
            title="Fechar tela"
            aria-label="Fechar créditos do sistema"
          >
            <X size={20} strokeWidth={3} />
          </button>

          <div className={estilos.conteudoRolavel}>
            {/* Bloco: Autoria */}
            <div className={estilos.grupoInformacao}>
              <h3 className={estilos.tituloGrupo}>
                <Code2 size={18} className={estilos.iconeTitulo} />
                Arquitetura e Desenvolvimento
              </h3>
              <div className={estilos.cartaoAutor}>
                <h4 className={estilos.nomePessoa}>
                  {CONFIG.AUTHOR_NAME || "Alex Costa"}
                </h4>
                <p className={estilos.cargoPessoa}>Desenvolvedor Full Stack</p>
                <span className={estilos.emblemaMatricula}>
                  MATRÍCULA: {CONFIG.AUTHOR_MATRICULA || "89618815"}
                </span>
              </div>
            </div>

            {/* Bloco: Tecnologias */}
            <div className={estilos.grupoInformacao}>
              <h3 className={estilos.tituloGrupo}>
                <Server size={18} className={estilos.iconeTitulo} />
                Tecnologias Utilizadas
              </h3>

              

              <div className={estilos.listaCategoriasTecnologias}>
                {Object.entries(tecnologiasPorCategoria).map(
                  ([categoria, tecnologias]) => (
                    <section
                      key={categoria}
                      className={estilos.blocoCategoriaTecnologia}
                    >
                      <div className={estilos.cabecalhoCategoriaTecnologia}>
                        <h4 className={estilos.tituloCategoriaTecnologia}>
                          {categoria}
                        </h4>
                      
                      </div>

                      <div className={estilos.gradeTecnologias}>
                        {(Array.isArray(tecnologias) ? tecnologias : []).map((tecnologia) => (
                          <div
                            key={`${categoria}-${tecnologia.nome}`}
                            className={estilos.cartaoTecnologia}
                          >
                            <div className={estilos.caixaIconeTech}>
                              {tecnologia.icone}
                            </div>
                            <div className={estilos.textoTecnologia}>
                              <h5 className={estilos.nomeTecnologia}>
                                {tecnologia.nome}
                              </h5>
                              <span className={estilos.papelTecnologia}>
                                {tecnologia.papel}
                              </span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </section>
                  ),
                )}
              </div>
            </div>
          </div>

          {/* Rodapé Fixo do Lado Direito */}
          <div className={estilos.rodapeDireito}>
            <div className={estilos.infoLicenca}>
              <CheckCircle2 size={24} className={estilos.iconeLicenca} />
              <div className={estilos.textosLicenca}>
                <span className={estilos.tituloLicenca}>
                  Licença de Uso Interno
                </span>
                <span className={estilos.empresaLicenca}>
                  {CONFIG.APP_COMPANY}{" "}
                  {CONFIG.APP_CNPJ ? `• CNPJ: ${CONFIG.APP_CNPJ}` : ""}
                </span>
              </div>
            </div>
            <button
              type="button"
              onClick={aoFechar}
              className={estilos.botaoAcaoRodape}
            >
              FECHAR
            </button>
          </div>
        </div>
      </div>
    </div>,
    document.body,
  );
};
