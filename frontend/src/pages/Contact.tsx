import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "@/hooks/use-toast";
import { contactService } from "@/services/contact.service";
import {
  Mail,
  Phone,
  MessageCircle,
  Send,
  CheckCircle,
  Loader2,
} from "lucide-react";

const infos = [
  { icon: Mail, title: "Email", value: "contact@printnow.be", detail: "Réponse sous 24h ouvrées" },
  { icon: Phone, title: "Téléphone", value: "+32 2 123 45 67", detail: "Lun – Ven, 9h à 18h" },
];

const Contact = () => {
  const [form, setForm] = useState({
    nom: "",
    email: "",
    sujet: "",
    message: "",
  });
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);

  const update = (key: keyof typeof form, value: string) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    const missing: string[] = [];
    if (!form.nom.trim()) missing.push("nom");
    if (!form.email.trim()) missing.push("email");
    if (!form.sujet.trim()) missing.push("sujet");
    if (!form.message.trim()) missing.push("message");

    if (missing.length > 0) {
      toast({
        title: "Champs manquants",
        description: `Merci de compléter : ${missing.join(", ")}.`,
        variant: "destructive",
      });
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      toast({
        title: "Email invalide",
        description: "Merci de saisir une adresse email valide.",
        variant: "destructive",
      });
      return;
    }

    setSending(true);
    try {
      await contactService.envoyerMessage(form);

      setSent(true);
      toast({
        title: "Message envoyé",
        description: "Notre équipe vous répond sous 24h ouvrées.",
      });
    } catch (error) {
      toast({
        title: "Erreur",
        description: (error as Error).message,
        variant: "destructive",
      });
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <main className="flex-1 pt-20">
        {/* Hero */}
        <section className="bg-primary py-16">
          <div className="container mx-auto px-4 max-w-4xl text-center">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary-foreground/10 border border-primary-foreground/20 text-primary-foreground/90 text-sm mb-6">
              <MessageCircle className="h-4 w-4" />
              Contact
            </div>
            <h1 className="font-display text-3xl md:text-5xl font-bold text-primary-foreground mb-4">
              Une question ? Parlons-en
            </h1>
            <p className="text-lg text-primary-foreground/80">
              Notre équipe accompagne clients et imprimeurs partenaires du lundi au vendredi.
            </p>
          </div>
        </section>

        {/* Infos */}
        <section className="py-12">
          <div className="container mx-auto px-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-2xl mx-auto">
              {infos.map((info) => (
                <Card key={info.title} className="border-border">
                  <CardContent className="p-5">
                    <div className="p-2.5 rounded-xl bg-primary/10 w-fit mb-3">
                      <info.icon className="h-5 w-5 text-primary" />
                    </div>
                    <h3 className="font-display font-semibold mb-1">{info.title}</h3>
                    <p className="text-sm text-foreground font-medium">{info.value}</p>
                    <p className="text-xs text-muted-foreground mt-1">{info.detail}</p>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        </section>

        {/* Formulaire */}
        <section className="pb-20">
          <div className="container mx-auto px-4">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 max-w-6xl mx-auto">
              <Card className="lg:col-span-2 border-border">
                <CardContent className="p-6 md:p-8">
                  {sent ? (
                    <div className="text-center py-10">
                      <div className="p-3 rounded-2xl bg-secondary/10 w-fit mx-auto mb-4">
                        <CheckCircle className="h-8 w-8 text-secondary" />
                      </div>
                      <h2 className="font-display text-2xl font-bold mb-2">Message envoyé</h2>
                      <p className="text-muted-foreground mb-6 max-w-md mx-auto">
                        Merci {form.nom.split(" ")[0]}, nous avons bien reçu votre demande
                        « {form.sujet} ». Une réponse sera envoyée à {form.email}.
                      </p>
                      <Button
                        variant="outline"
                        onClick={() => {
                          setSent(false);
                          setForm({ nom: "", email: "", sujet: "", message: "" });
                        }}
                      >
                        Envoyer un autre message
                      </Button>
                    </div>
                  ) : (
                    <form onSubmit={handleSubmit} className="space-y-5">
                      <div>
                        <h2 className="font-display text-2xl font-bold mb-1">Nous écrire</h2>
                        <p className="text-sm text-muted-foreground">
                          Tous les champs marqués d'un * sont obligatoires.
                        </p>
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label htmlFor="nom">Nom complet *</Label>
                          <Input
                            id="nom"
                            maxLength={100}
                            value={form.nom}
                            onChange={(e) => update("nom", e.target.value)}
                            placeholder="Marie Dupont"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="email">Email *</Label>
                          <Input
                            id="email"
                            type="email"
                            maxLength={255}
                            value={form.email}
                            onChange={(e) => update("email", e.target.value)}
                            placeholder="marie@exemple.be"
                          />
                        </div>
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor="sujet">Sujet *</Label>
                        <Input
                          id="sujet"
                          maxLength={120}
                          value={form.sujet}
                          onChange={(e) => update("sujet", e.target.value)}
                          placeholder="Ex. : Problème de livraison, partenariat, question technique…"
                        />
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor="message">Message *</Label>
                        <Textarea
                          id="message"
                          rows={6}
                          maxLength={1000}
                          value={form.message}
                          onChange={(e) => update("message", e.target.value)}
                          placeholder="Décrivez votre demande…"
                        />
                        <p className="text-xs text-muted-foreground text-right">
                          {form.message.length}/1000
                        </p>
                      </div>

                      <Button type="submit" size="lg" className="w-full sm:w-auto" disabled={sending}>
                        {sending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                        Envoyer le message
                      </Button>
                    </form>
                  )}
                </CardContent>
              </Card>

              <div className="space-y-4">
                <Card className="border-border">
                  <CardContent className="p-6">
                    <h3 className="font-display font-semibold text-lg mb-2">
                      Vous êtes imprimeur ?
                    </h3>
                    <p className="text-sm text-muted-foreground mb-4">
                      Rejoignez le catalogue PrintNow : inscription en ligne, services et horaires
                      configurables, activation immédiate après paiement.
                    </p>
                    <Button className="w-full" asChild>
                      <Link to="/devenir-partenaire">Devenir partenaire</Link>
                    </Button>
                  </CardContent>
                </Card>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
};

export default Contact;
