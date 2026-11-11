import ReactDOM from "react-dom/client";
import Aplicacao from "./Aplicacao";
import "./styles/tokens.css";
import "./index.css";
import { BrowserRouter } from "react-router-dom";
import { StrictMode } from "react";
const elementoRaiz = document.getElementById("root");
if (!elementoRaiz) throw new Error("Elemento raiz da aplicação ausente.");
ReactDOM.createRoot(elementoRaiz).render(
  <StrictMode>
    <BrowserRouter>
      <Aplicacao />
    </BrowserRouter>
  </StrictMode>,
);
