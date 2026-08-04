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
    private static final String URL_DASHBOARD_PARTENAIRE = "http://localhost:5173/dashboard-imprimeur";
    private static final String URL_DASHBOARD_CLIENT = "http://localhost:5173/dashboard";

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

    @Async
    public void envoyerBienvenuePartenaire(String destinataire, String nomImprimerie) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(destinataire);
            helper.setFrom(EXPEDITEUR);
            helper.setSubject("Bienvenue dans le réseau partenaire PrintNow !");
            helper.setText(corpsBienvenuePartenaire(nomImprimerie), true);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Échec de l'envoi du mail de bienvenue partenaire à {}", destinataire, e);
        }
    }

    @Async
    public void envoyerVerificationAcceptee(String destinataire, String prenom) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(destinataire);
            helper.setFrom(EXPEDITEUR);
            helper.setSubject("Votre statut étudiant est validé !");
            helper.setText(corpsVerificationAcceptee(prenom), true);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Échec de l'envoi du mail de vérification acceptée à {}", destinataire, e);
        }
    }

    @Async
    public void envoyerVerificationRefusee(String destinataire, String prenom, String motifRefus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(destinataire);
            helper.setFrom(EXPEDITEUR);
            helper.setSubject("Votre statut étudiant n'a pas pu être validé");
            helper.setText(corpsVerificationRefusee(prenom, motifRefus), true);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Échec de l'envoi du mail de vérification refusée à {}", destinataire, e);
        }
    }

    private String echapperHtml(String texte) {
        if (texte == null) return "";
        return texte.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
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

    private String corpsBienvenuePartenaire(String nomImprimerie) {
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
                        .cta-container { text-align: center; margin: 35px 0; }
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
                                <h1>Bienvenue dans le réseau partenaire ! 🤝</h1>
                                <p>Bonjour <strong>%s</strong>,</p>
                                <p>Votre compte imprimeur a bien été validé. Nous sommes ravis de vous compter parmi nos partenaires de confiance sur PrintNow !</p>
                                <p>Votre profil est entièrement configuré et vos services sont désormais visibles par les clients de votre région. Grâce à notre plateforme, vous allez pouvoir accroître votre visibilité locale et gérer vos nouvelles commandes de manière totalement centralisée.</p>
                                <p>Vous êtes officiellement prêt à recevoir vos premières demandes. Gardez un œil sur votre boîte mail pour être alerté de l'arrivée d'une nouvelle commande.</p>
                                <div class="cta-container">
                                    <a href="%s" class="cta-button">Accéder à mon espace pro</a>
                                </div>
                                <p class="signature">
                                    L'équipe Partenariats PrintNow est là pour vous accompagner.<br>
                                    <strong>L'équipe PrintNow</strong>
                                </p>
                            </div>
                            <div class="email-footer">
                                <p>© 2026 PrintNow. Tous droits réservés.</p>
                                <p>Besoin d'aide avec votre espace partenaire ? Écrivez-nous à <a href="mailto:partenaires@printnow.be" style="color: #f59e0b; text-decoration: none;">partenaires@printnow.be</a></p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(nomImprimerie, URL_DASHBOARD_PARTENAIRE);
    }

    private String corpsVerificationAcceptee(String prenom) {
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
                        .cta-container { text-align: center; margin: 35px 0; }
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
                                <h1>Bonne nouvelle ! 🎓</h1>
                                <p>Bonjour <strong>%s</strong>,</p>
                                <p>Nous avons le plaisir de vous informer que votre document a été vérifié et que votre <strong>statut étudiant est désormais validé</strong> !</p>
                                <p>Vous bénéficiez à présent automatiquement du <strong>tarif étudiant</strong> sur les imprimeries participantes lors de vos prochaines commandes. C'est le moment idéal pour imprimer vos dossiers, mémoires ou projets au meilleur prix.</p>
                                <div class="cta-container">
                                    <a href="%s" class="cta-button">Profiter de mes réductions</a>
                                </div>
                                <p class="signature">
                                    Bonnes impressions,<br>
                                    <strong>L'équipe PrintNow</strong>
                                </p>
                            </div>
                            <div class="email-footer">
                                <p>© 2026 PrintNow. Tous droits réservés.</p>
                                <p>Une question ? Contactez-nous à <a href="mailto:contact@printnow.be" style="color: #f59e0b; text-decoration: none;">contact@printnow.be</a></p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(prenom, URL_CATALOGUE);
    }

    private String corpsVerificationRefusee(String prenom, String motifRefus) {
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
                        .alert-box { background-color: #fff1f2; border-left: 4px solid #f43f5e; padding: 15px 20px; margin-bottom: 25px; border-radius: 0 8px 8px 0; color: #881337; }
                        .alert-box p { margin: 0; font-size: 15px; color: inherit; }
                        .cta-container { text-align: center; margin: 35px 0; }
                        .cta-button { display: inline-block; background-color: #1e293b; color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 8px; font-weight: bold; font-size: 16px; }
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
                                <h1>Action requise concernant votre statut étudiant ⚠️</h1>
                                <p>Bonjour <strong>%s</strong>,</p>
                                <p>Nous avons bien reçu votre demande pour bénéficier du tarif étudiant, mais malheureusement, <strong>nous n'avons pas pu valider le document fourni.</strong></p>
                                <div class="alert-box">
                                    <p><strong>Motif du refus :</strong><br>%s</p>
                                </div>
                                <p>Pas d'inquiétude, vous pouvez recommencer la procédure en téléversant un nouveau document clair et lisible directement depuis votre espace personnel.</p>
                                <div class="cta-container">
                                    <a href="%s" class="cta-button">Soumettre un nouveau document</a>
                                </div>
                                <p class="signature">
                                    À très vite,<br>
                                    <strong>L'équipe PrintNow</strong>
                                </p>
                            </div>
                            <div class="email-footer">
                                <p>© 2026 PrintNow. Tous droits réservés.</p>
                                <p>Une question ? Contactez-nous à <a href="mailto:contact@printnow.be" style="color: #f59e0b; text-decoration: none;">contact@printnow.be</a></p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(prenom, echapperHtml(motifRefus), URL_DASHBOARD_CLIENT);
    }
}
