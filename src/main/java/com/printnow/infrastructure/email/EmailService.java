package com.printnow.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Envoi des emails transactionnels de l'application (Mailtrap en sandbox pour
 * le moment). Toujours asynchrone et sans jamais faire échouer l'action
 * métier qui déclenche l'envoi si le mail lui-même échoue.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String EXPEDITEUR = "no-reply@printnow.be";
    private static final String URL_CATALOGUE = "http://localhost:5173/#imprimeries";

    private final JavaMailSender mailSender;

    @Async
    public void envoyerBienvenue(String destinataire, String prenom) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(destinataire);
            helper.setFrom(EXPEDITEUR);
            helper.setSubject("Bienvenue sur PrintNow !");
            helper.setText(corpsBienvenue(prenom), true);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Échec de l'envoi du mail de bienvenue à {}", destinataire, e);
        }
    }

    private String corpsBienvenue(String prenom) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { margin: 0; padding: 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f4f7f6; color: #334155; }
                        .email-wrapper { width: 100%%; background-color: #f8fafc; padding: 40px 15px; box-sizing: border-box; }
                        .email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05); border: 1px solid #e2e8f0; }
                        .email-header { background-color: #1e293b; padding: 35px 20px; text-align: center; }
                        .logo { font-size: 32px; font-weight: 800; color: #ffffff; margin: 0; letter-spacing: 0.5px; }
                        .logo span { color: #f59e0b; }
                        .email-body { padding: 40px 35px; }
                        h1 { color: #1e293b; font-size: 22px; margin-top: 0; font-weight: 700; text-align: center; margin-bottom: 25px;}
                        p { font-size: 16px; line-height: 1.6; margin-bottom: 20px; color: #475569; }
                        .cta-container { text-align: center; margin: 40px 0; }
                        .cta-button { display: inline-block; background-color: #f59e0b; color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 8px; font-weight: bold; font-size: 16px; }
                        .signature { margin-top: 30px; font-size: 16px; }
                        .email-footer { background-color: #f8fafc; padding: 25px; text-align: center; border-top: 1px solid #e2e8f0; }
                        .email-footer p { font-size: 12px; color: #94a3b8; margin: 5px 0; }
                    </style>
                </head>
                <body>
                    <div class="email-wrapper">
                        <div class="email-container">
                            <div class="email-header">
                                <p class="logo">PRINT<span>NOW</span></p>
                            </div>
                            <div class="email-body">
                                <h1>Bienvenue parmi nous ! 🎉</h1>
                                <p>Bonjour <strong>%s</strong>,</p>
                                <p>Bienvenue sur PrintNow ! Votre compte a été créé avec succès.</p>
                                <p>Vous pouvez dès maintenant parcourir notre catalogue d'imprimeries partenaires, comparer les offres et passer votre première commande en quelques clics.</p>
                                <div class="cta-container">
                                    <a href="%s" class="cta-button">Parcourir le catalogue</a>
                                </div>
                                <p class="signature">
                                    À bientôt,<br>
                                    <strong>L'équipe PrintNow</strong>
                                </p>
                            </div>
                            <div class="email-footer">
                                <p>© 2026 PrintNow. Tous droits réservés.</p>
                                <p>Si vous avez des questions, contactez-nous à <a href="mailto:contact@printnow.be" style="color: #f59e0b; text-decoration: none;">contact@printnow.be</a></p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(prenom, URL_CATALOGUE);
    }
}
