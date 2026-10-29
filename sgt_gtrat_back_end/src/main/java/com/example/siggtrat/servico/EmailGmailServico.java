package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesEmailGmail;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailGmailServico {

    public static final String CID_LOGO_CORREIOS = "logoCorreios";

    private static final Logger logger = LoggerFactory.getLogger(EmailGmailServico.class);

    private final JavaMailSender mailSender;
    private final PropriedadesEmailGmail propriedades;

    public EmailGmailServico(JavaMailSender mailSender, PropriedadesEmailGmail propriedades) {
        this.mailSender = mailSender;
        this.propriedades = propriedades;
    }

    public boolean enviarHtml(String destinatario, String assunto, String html) {
        try {
            propriedades.validarConfiguracao();

            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mensagem,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(propriedades.remetenteEfetivo().trim(), propriedades.nomeRemetente().trim());
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(html, true);

            ClassPathResource logo = new ClassPathResource(propriedades.logoClasspath().trim());
            if (!logo.exists()) {
                throw new IllegalStateException(
                        "Logo dos Correios não encontrado no classpath: " + propriedades.logoClasspath()
                );
            }
            helper.addInline(CID_LOGO_CORREIOS, logo, "image/png");

            mailSender.send(mensagem);
            return true;
        } catch (Exception e) {
            logger.error(
                    "Falha ao enviar e-mail transacional pelo Gmail para o destinatário informado.",
                    e
            );
            return false;
        }
    }
}
