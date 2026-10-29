package com.example.siggtrat.servico;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class CodigoQrServico {

    private static final int TAMANHO = 256;

    public String gerarDataUrl(String conteudo) {
        if (conteudo == null || conteudo.isBlank()) {
            throw new IllegalArgumentException("Conteúdo do QR Code não informado.");
        }
        try {
            BitMatrix matriz = new QRCodeWriter().encode(
                    conteudo,
                    BarcodeFormat.QR_CODE,
                    TAMANHO,
                    TAMANHO,
                    Map.of(
                            EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.MARGIN, 2
                    )
            );
            String svg = gerarSvg(matriz);
            String base64 = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
            return "data:image/svg+xml;base64," + base64;
        } catch (WriterException ex) {
            throw new IllegalStateException("Não foi possível gerar o QR Code de configuração.", ex);
        }
    }

    private String gerarSvg(BitMatrix matriz) {
        int largura = matriz.getWidth();
        int altura = matriz.getHeight();
        StringBuilder caminho = new StringBuilder(largura * altura / 2);

        for (int y = 0; y < altura; y++) {
            int x = 0;
            while (x < largura) {
                if (!matriz.get(x, y)) {
                    x++;
                    continue;
                }
                int inicio = x;
                while (x < largura && matriz.get(x, y)) x++;
                caminho.append('M').append(inicio).append(' ').append(y)
                        .append('h').append(x - inicio).append("v1H")
                        .append(inicio).append('z');
            }
        }

        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 "
                + largura + " " + altura
                + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>"
                + "<path d=\"" + caminho + "\" fill=\"#003b73\"/></svg>";
    }
}
