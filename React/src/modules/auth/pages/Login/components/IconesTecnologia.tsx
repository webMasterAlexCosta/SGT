import React from "react";
import { Coffee, Database } from "lucide-react";
import { EnvolucroDica } from "../../../../../core/ui/EnvolucroDica";
import estilos from "../index.module.css";

export const IconesTecnologia = React.memo(() => (
  <div className={estilos.listaIconesTech}>
    <EnvolucroDica tooltip="React.js (Frontend)" position="top">
      <svg
        viewBox="-10.5 -9.45 21 18.9"
        className={`${estilos.iconeTech} ${estilos.iconeReact}`}
      >
        <circle cx="0" cy="0" r="2" fill="currentColor" />
        <g stroke="currentColor" strokeWidth="1" fill="none">
          <ellipse rx="10" ry="4.5" />
          <ellipse rx="10" ry="4.5" transform="rotate(60)" />
          <ellipse rx="10" ry="4.5" transform="rotate(120)" />
        </g>
      </svg>
    </EnvolucroDica>
    <EnvolucroDica tooltip="Java 25" position="top">
      <Coffee
        className={`${estilos.iconeTech} ${estilos.iconeJava}`}
        strokeWidth={2.5}
      />
    </EnvolucroDica>
    <EnvolucroDica tooltip="Spring Boot 4.1 (API)" position="top">
      <svg viewBox="0 0 32 32" className={estilos.iconeTech}>
        <path
          fill="#6DB33F"
          d="M30.7 12.6c.1.9.1 1.9 0 2.8-1.5 5.3-7.6 10-14.7 10s-13.2-4.7-14.7-10c-.1-.9-.1-1.9 0-2.8 1.5-5.3 7.6-10 14.7-10s13.2 4.7 14.7 10z"
        />
        <path
          fill="#FFF"
          d="M16 23.5c-4.9 0-9.6-2.6-12.4-6.8.6-2.1 2.3-3.8 4.4-4.4 2.1-.6 4.3-.2 6.2 1 1.9 1.2 3.2 3.1 3.5 5.3.3-2.2 1.6-4.1 3.5-5.3 1.9-1.2 4.1-1.6 6.2-1 2.1.6 3.8 2.3 4.4 4.4-2.8 4.2-7.5 6.8-12.4 6.8-1.2 0-2.3-.1-3.4-.3-1.1.2-2.2.3-3.4.3z"
        />
        <path
          fill="#6DB33F"
          d="M16 21c-3.6 0-7.1-1.9-9.1-5 1-2.9 3.5-5 6.6-5s5.6 2.1 6.6 5c-2 3.1-5.5 5-9.1 5z"
        />
      </svg>
    </EnvolucroDica>
    <EnvolucroDica tooltip="MySQL (Database)" position="top">
      <Database
        className={`${estilos.iconeTech} ${estilos.iconeDb}`}
        strokeWidth={2.5}
      />
    </EnvolucroDica>
  </div>
));
IconesTecnologia.displayName = "IconesTecnologia";
