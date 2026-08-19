package com.printnow.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
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
    private static final String CONTACT_DESTINATAIRE = "contact@printnow.be";
    private static final String URL_CATALOGUE = "http://localhost:5173/imprimeries";
    private static final String URL_DASHBOARD_PARTENAIRE = "http://localhost:5173/dashboard-imprimeur";
    private static final String URL_DASHBOARD_CLIENT = "http://localhost:5173/dashboard";
    private static final String URL_REINITIALISATION = "http://localhost:5173/reinitialiser-mot-de-passe";

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

    /**
     * @param factureInscriptionPdf peut être null (échec de génération) : le mail
     *                              part quand même, sans pièce jointe — la facture
     *                              reste de toute façon téléchargeable depuis
     *                              l'espace professionnel du partenaire.
     */
    @Async
    public void envoyerBienvenuePartenaire(String destinataire, String nomImprimerie, byte[] factureInscriptionPdf) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // multipart=true nécessaire pour pouvoir joindre la facture PDF.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinataire);
            helper.setFrom(EXPEDITEUR);
            helper.setSubject("Bienvenue dans le réseau partenaire PrintNow !");
            helper.setText(corpsBienvenuePartenaire(nomImprimerie), true);
            if (factureInscriptionPdf != null) {
                helper.addAttachment("facture-inscription-printnow.pdf", new ByteArrayResource(factureInscriptionPdf));
            }
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

    /**
     * Prévient le client que sa commande l'attend en magasin.
     *
     * Envoyé quand l'imprimeur la marque comme prête : sans cela, le client
     * n'aurait aucun moyen de l'apprendre sans retourner de lui-même sur le site.
     */
    @Async
    public void envoyerCommandePrete(String destinataire, String prenom, String numeroCommande,
                                     String nomImprimerie, String adresseImprimerie, String telephoneImprimerie) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(destinataire);
            helper.setFrom(EXPEDITEUR);
            helper.setSubject("Votre commande " + numeroCommande + " est prête !");
            helper.setText(corpsCommandePrete(prenom, numeroCommande, nomImprimerie,
                    adresseImprimerie, telephoneImprimerie), true);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Échec de l'envoi du mail « commande prête » à {}", destinataire, e);
        }
    }

    /**
     * Prévient l'imprimerie qu'une nouvelle commande payée vient d'arriver.
     *
     * Sans cette alerte, l'imprimerie ne découvre une commande — express ou
     * à retirer rapidement — qu'en pensant à rafraîchir son dashboard.
     */
    @Async
    public void envoyerNouvelleCommande(String destinataire, String numeroCommande, String modeRetrait,
                                        boolean express2h, String totalTTC) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(destinataire);
            helper.setFrom(EXPEDITEUR);
            helper.setSubject((express2h ? "⚡ Nouvelle commande express " : "Nouvelle commande ") + numeroCommande);
            helper.setText(corpsNouvelleCommande(numeroCommande, modeRetrait, express2h, totalTTC), true);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Échec de l'envoi du mail « nouvelle commande » à {}", destinataire, e);
        }
    }

    /**
     * Envoie le lien permettant de choisir un nouveau mot de passe.
     *
     * Le jeton ne figure que dans cet email : c'est lui qui fait office de
     * preuve que le demandeur possède bien la boîte mail du compte. Il n'est
     * donc jamais journalisé, même en cas d'échec.
     */
    @Async
    public void envoyerReinitialisationMotDePasse(String destinataire, String prenom, String jeton, long minutesDeValidite) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(destinataire);
            helper.setFrom(EXPEDITEUR);
            helper.setSubject("Réinitialisation de votre mot de passe PrintNow");
            helper.setText(corpsReinitialisation(prenom, jeton, minutesDeValidite), true);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Échec de l'envoi du mail de réinitialisation à {}", destinataire, e);
        }
    }

    /**
     * Envoi du formulaire de contact public. Contrairement aux autres emails
     * transactionnels, l'envoi est ici l'action principale demandée par
     * l'utilisateur : on ne l'exécute donc pas en asynchrone et on laisse
     * l'exception remonter pour que le contrôleur puisse signaler l'échec.
     */
    public void envoyerMessageContact(String nom, String emailExpediteur, String sujet, String message) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        helper.setTo(CONTACT_DESTINATAIRE);
        helper.setFrom(EXPEDITEUR);
        helper.setReplyTo(emailExpediteur);
        helper.setSubject("[Contact] " + sujet);
        helper.setText(corpsMessageContact(nom, emailExpediteur, sujet, message), true);
        mailSender.send(mimeMessage);
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

    private String corpsCommandePrete(String prenom, String numeroCommande, String nomImprimerie,
                                      String adresseImprimerie, String telephoneImprimerie) {
        String salutation = (prenom == null || prenom.isBlank())
                ? "Bonjour," : "Bonjour <strong>%s</strong>,".formatted(echapperHtml(prenom));

        // Adresse et téléphone ne sont pas obligatoires sur une imprimerie :
        // une ligne vide laisserait un blanc inexplicable dans l'encadré.
        StringBuilder coordonnees = new StringBuilder();
        if (adresseImprimerie != null && !adresseImprimerie.isBlank()) {
            coordonnees.append("<p class=\"lieu-ligne\">%s</p>".formatted(echapperHtml(adresseImprimerie)));
        }
        if (telephoneImprimerie != null && !telephoneImprimerie.isBlank()) {
            coordonnees.append("<p class=\"lieu-ligne\">Tél. %s</p>".formatted(echapperHtml(telephoneImprimerie)));
        }

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
                        .commande { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px 20px; text-align: center; margin-bottom: 25px; }
                        .commande p { margin: 0; font-size: 14px; color: #94a3b8; }
                        .commande strong { display: block; font-size: 18px; color: #1e293b; letter-spacing: 0.5px; margin-top: 4px; }
                        .lieu { background-color: #fffbeb; border-left: 4px solid #f59e0b; padding: 15px 20px; border-radius: 0 8px 8px 0; margin-bottom: 25px; }
                        .lieu-titre { margin: 0 0 6px 0; font-size: 15px; font-weight: bold; color: #1e293b; }
                        .lieu-ligne { margin: 0; font-size: 15px; color: #475569; line-height: 1.5; }
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
                                <h1>Votre commande est prête ! 📦</h1>
                                <p>%s</p>
                                <p>Bonne nouvelle : votre impression est terminée et vous attend en magasin.</p>
                                <div class="commande">
                                    <p>Numéro de commande</p>
                                    <strong>%s</strong>
                                </div>
                                <div class="lieu">
                                    <p class="lieu-titre">À retirer chez %s</p>
                                    %s
                                </div>
                                <p>Pensez à vous munir de votre numéro de commande. Les horaires d'ouverture
                                sont indiqués sur la fiche de l'imprimerie.</p>
                                <div class="cta-container">
                                    <a href="%s" class="cta-button">Voir ma commande</a>
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
                """.formatted(salutation, echapperHtml(numeroCommande), echapperHtml(nomImprimerie),
                        coordonnees.toString(), URL_DASHBOARD_CLIENT);
    }

    private String corpsNouvelleCommande(String numeroCommande, String modeRetrait, boolean express2h, String totalTTC) {
        String ligneMode = express2h ? "À préparer en express, sous 2 heures ⚡" : modeRetrait;

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
                        .commande { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px 20px; text-align: center; margin-bottom: 25px; }
                        .commande p { margin: 0; font-size: 14px; color: #94a3b8; }
                        .commande strong { display: block; font-size: 18px; color: #1e293b; letter-spacing: 0.5px; margin-top: 4px; }
                        .detail-ligne { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #e2e8f0; font-size: 15px; }
                        .detail-ligne:last-child { border-bottom: none; }
                        .detail-label { color: #94a3b8; }
                        .detail-valeur { color: #1e293b; font-weight: 600; }
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
                                <h1>Nouvelle commande reçue ! 🎉</h1>
                                <p>Bonjour,</p>
                                <p>Vous venez de recevoir une nouvelle commande.</p>
                                <div class="commande">
                                    <p>Numéro de commande</p>
                                    <strong>%s</strong>
                                </div>
                                <div class="detail-ligne">
                                    <span class="detail-label">Mode</span>
                                    <span class="detail-valeur">%s</span>
                                </div>
                                <div class="detail-ligne">
                                    <span class="detail-label">Montant total</span>
                                    <span class="detail-valeur">%s</span>
                                </div>
                                <div class="cta-container">
                                    <a href="%s" class="cta-button">Voir la commande</a>
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
                """.formatted(echapperHtml(numeroCommande), echapperHtml(ligneMode), echapperHtml(totalTTC), URL_DASHBOARD_PARTENAIRE);
    }

    private String corpsReinitialisation(String prenom, String jeton, long minutesDeValidite) {
        // Le jeton est produit en Base64 « URL » : aucun caractère à échapper.
        String lien = URL_REINITIALISATION + "?jeton=" + jeton;
        String salutation = (prenom == null || prenom.isBlank()) ? "Bonjour," : "Bonjour <strong>%s</strong>,".formatted(echapperHtml(prenom));

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
                        .lien-brut { font-size: 13px; color: #94a3b8; word-break: break-all; text-align: center; }
                        .alert-box { background-color: #f8fafc; border-left: 4px solid #94a3b8; padding: 15px 20px; margin-top: 30px; border-radius: 0 8px 8px 0; }
                        .alert-box p { margin: 0; font-size: 15px; }
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
                                <h1>Réinitialisation de votre mot de passe 🔑</h1>
                                <p>%s</p>
                                <p>Vous avez demandé à réinitialiser le mot de passe de votre compte PrintNow. Cliquez sur le bouton ci-dessous pour en choisir un nouveau.</p>
                                <div class="cta-container">
                                    <a href="%s" class="cta-button">Choisir un nouveau mot de passe</a>
                                </div>
                                <p class="lien-brut">Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>%s</p>
                                <div class="alert-box">
                                    <p>Ce lien est valable <strong>%d minutes</strong> et ne peut servir qu'une seule fois.<br>
                                    Si vous n'êtes pas à l'origine de cette demande, ignorez simplement cet email : votre mot de passe actuel reste inchangé.</p>
                                </div>
                                <p class="signature">
                                    À bientôt,<br>
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
                """.formatted(salutation, lien, lien, minutesDeValidite);
    }

    private String corpsMessageContact(String nom, String emailExpediteur, String sujet, String message) {
        String messageHtml = echapperHtml(message).replace("\n", "<br>");
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
                        h1 { color: #1e293b; font-size: 22px; margin-top: 0; font-weight: 700; margin-bottom: 25px;}
                        p { font-size: 16px; line-height: 1.6; margin-bottom: 12px; color: #475569; }
                        .info-box { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px 20px; margin-bottom: 25px; }
                        .info-box p { margin: 4px 0; }
                        .message-box { background-color: #fffbeb; border-left: 4px solid #f59e0b; padding: 15px 20px; border-radius: 0 8px 8px 0; }
                        .message-box p { margin: 0; color: #334155; }
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
                                <h1>Nouveau message de contact</h1>
                                <div class="info-box">
                                    <p><strong>Nom :</strong> %s</p>
                                    <p><strong>Email :</strong> %s</p>
                                    <p><strong>Sujet :</strong> %s</p>
                                </div>
                                <div class="message-box">
                                    <p>%s</p>
                                </div>
                            </div>
                            <div class="email-footer">
                                <p>Répondez directement à cet email pour contacter %s.</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(echapperHtml(nom), echapperHtml(emailExpediteur), echapperHtml(sujet), messageHtml, echapperHtml(nom));
    }
}
