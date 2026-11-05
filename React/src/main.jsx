import ReactDOM from "react-dom/client";
import Aplicacao from "./Aplicacao";
import "./styles/tokens.css";
import "./index.css";
import { BrowserRouter } from "react-router-dom";
import { StrictMode } from "react";
ReactDOM.createRoot(document.getElementById("root")).render(
  <StrictMode>
    <BrowserRouter>
      <Aplicacao />
    </BrowserRouter>
  </StrictMode>,
);
