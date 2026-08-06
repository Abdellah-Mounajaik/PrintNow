import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Bot, Send, User, Sparkle, RotateCcw, Printer } from "lucide-react";

type Message = { id: string; role: "bot" | "user"; text: string };

const MESSAGE_ACCUEIL =
  "Bonjour 👋 Je suis l'assistant PrintNow. Posez-moi votre question sur les commandes, les prix, la livraison, les factures ou le partenariat imprimeur.";

/**
 * Questions proposées au visiteur. Ce ne sont que des raccourcis de saisie :
 * les réponses viennent toujours de l'assistant, jamais d'un texte figé ici.
 */
const questionsFrequentes = [
  "Comment passer une commande ?",
  "Comment le prix est-il calculé ?",
  "Quels formats de fichiers acceptez-vous ?",
  "Livraison ou retrait en magasin ?",
  "Y a-t-il un tarif étudiant ?",
  "Comment utiliser un code promo ?",
  "Quels moyens de paiement acceptez-vous ?",
  "Où trouver mes factures ?",
  "Comment suivre ma commande ?",
  "Comment devenir imprimeur partenaire ?",
  "Quelle commission prend PrintNow ?",
  "Mes documents sont-ils confidentiels ?",
];

const suggestions = questionsFrequentes.slice(0, 5);

/** Nombre de messages d'historique envoyés au backend (qui en accepte 20 max). */
const MAX_HISTORIQUE = 10;

const FAQ = () => {
  const [messages, setMessages] = useState<Message[]>([
    { id: "welcome", role: "bot", text: MESSAGE_ACCUEIL },
  ]);
  const [input, setInput] = useState("");
  const [typing, setTyping] = useState(false);
  const endRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, typing]);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const ask = async (question: string) => {
    const value = question.trim().slice(0, 500);
    if (!value || typing) return;

    const userMessage: Message = { id: `u-${Date.now()}`, role: "user", text: value };
    // On construit l'historique à partir de l'état courant : setMessages étant
    // asynchrone, on ne peut pas se fier à `messages` juste après l'appel.
    const conversation = [...messages, userMessage];

    setMessages(conversation);
    setInput("");
    setTyping(true);

    try {
      const response = await fetch("http://localhost:8080/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          messages: conversation
            .filter((m) => m.id !== "welcome")
            .slice(-MAX_HISTORIQUE)
            .map((m) => ({
              role: m.role === "bot" ? "assistant" : "user",
              content: m.text.slice(0, 500),
            })),
        }),
      });

      const data = await response.json().catch(() => null);
      const texte = response.ok
        ? data?.reply
        : data?.message ?? "L'assistant est momentanément indisponible.";

      setMessages((prev) => [...prev, { id: `b-${Date.now()}`, role: "bot", text: texte }]);
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          id: `b-${Date.now()}`,
          role: "bot",
          text: "Je n'arrive pas à joindre le serveur pour le moment. Réessayez dans un instant ou écrivez-nous via la page Contact.",
        },
      ]);
    } finally {
      setTyping(false);
      inputRef.current?.focus();
    }
  };

  const reset = () => {
    setMessages([{ id: "welcome", role: "bot", text: MESSAGE_ACCUEIL }]);
    setInput("");
    inputRef.current?.focus();
  };

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <main className="flex-1 pt-20">
        {/* Hero */}
        <section className="bg-primary py-16">
          <div className="container mx-auto px-4 max-w-4xl text-center">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary-foreground/10 border border-primary-foreground/20 text-primary-foreground/90 text-sm mb-6">
              <Sparkle className="h-4 w-4" />
              Foire aux questions
            </div>
            <h1 className="font-display text-3xl md:text-5xl font-bold text-primary-foreground mb-4">
              Posez votre question, l'assistant répond
            </h1>
            <p className="text-lg text-primary-foreground/80">
              Commandes, prix, délais, livraison, factures ou partenariat : réponse immédiate.
            </p>
          </div>
        </section>

        {/* Chat */}
        <section className="py-12">
          <div className="container mx-auto px-4">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 max-w-6xl mx-auto">
              <Card className="lg:col-span-2 border-border overflow-hidden">
                {/* Chat header */}
                <div className="flex items-center justify-between gap-3 px-5 py-4 border-b border-border bg-muted/40">
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-xl bg-primary/10">
                      <Printer className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-display font-semibold leading-tight">Assistant PrintNow</p>
                      <p className="text-xs text-secondary">En ligne</p>
                    </div>
                  </div>
                  <Button variant="ghost" size="sm" onClick={reset}>
                    <RotateCcw className="h-4 w-4" />
                    <span className="hidden sm:inline">Réinitialiser</span>
                  </Button>
                </div>

                {/* Messages */}
                <div className="h-[420px] overflow-y-auto px-4 py-5 space-y-4">
                  {messages.map((message) => (
                    <div
                      key={message.id}
                      className={`flex gap-3 ${message.role === "user" ? "justify-end" : "justify-start"}`}
                    >
                      {message.role === "bot" && (
                        <div className="p-2 rounded-lg bg-primary/10 h-fit shrink-0">
                          <Bot className="h-4 w-4 text-primary" />
                        </div>
                      )}
                      <div
                        className={`max-w-[80%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed whitespace-pre-wrap ${
                          message.role === "user"
                            ? "bg-primary text-primary-foreground rounded-br-sm"
                            : "bg-muted text-foreground rounded-bl-sm"
                        }`}
                      >
                        {message.text}
                      </div>
                      {message.role === "user" && (
                        <div className="p-2 rounded-lg bg-secondary/10 h-fit shrink-0">
                          <User className="h-4 w-4 text-secondary" />
                        </div>
                      )}
                    </div>
                  ))}

                  {typing && (
                    <div className="flex gap-3">
                      <div className="p-2 rounded-lg bg-primary/10 h-fit">
                        <Bot className="h-4 w-4 text-primary" />
                      </div>
                      <div className="bg-muted rounded-2xl rounded-bl-sm px-4 py-3 flex items-center gap-1">
                        <span className="h-2 w-2 rounded-full bg-muted-foreground/50 animate-pulse" />
                        <span className="h-2 w-2 rounded-full bg-muted-foreground/50 animate-pulse [animation-delay:150ms]" />
                        <span className="h-2 w-2 rounded-full bg-muted-foreground/50 animate-pulse [animation-delay:300ms]" />
                      </div>
                    </div>
                  )}
                  <div ref={endRef} />
                </div>

                {/* Suggestions + input */}
                <div className="border-t border-border px-4 py-4 space-y-3 bg-card">
                  <div className="flex flex-wrap gap-2">
                    {suggestions.map((suggestion) => (
                      <button
                        key={suggestion}
                        type="button"
                        disabled={typing}
                        onClick={() => ask(suggestion)}
                        className="px-3 py-1.5 text-xs rounded-full border border-border text-muted-foreground hover:border-primary hover:text-primary transition-colors disabled:opacity-50"
                      >
                        {suggestion}
                      </button>
                    ))}
                  </div>
                  <form
                    className="flex gap-2"
                    onSubmit={(e) => {
                      e.preventDefault();
                      ask(input);
                    }}
                  >
                    <Input
                      ref={inputRef}
                      value={input}
                      maxLength={500}
                      onChange={(e) => setInput(e.target.value)}
                      placeholder="Écrivez votre question…"
                    />
                    <Button type="submit" size="icon" disabled={typing || !input.trim()}>
                      <Send className="h-4 w-4" />
                    </Button>
                  </form>
                </div>
              </Card>

              {/* Toutes les questions */}
              <div className="space-y-4">
                <Card className="border-border">
                  <CardContent className="p-5">
                    <h2 className="font-display font-semibold text-lg mb-3">
                      Questions fréquentes
                    </h2>
                    <ul className="space-y-1.5">
                      {questionsFrequentes.map((question) => (
                        <li key={question}>
                          <button
                            type="button"
                            disabled={typing}
                            onClick={() => ask(question)}
                            className="text-left text-sm text-muted-foreground hover:text-primary transition-colors w-full disabled:opacity-50"
                          >
                            {question}
                          </button>
                        </li>
                      ))}
                    </ul>
                  </CardContent>
                </Card>

                <Card className="border-border bg-muted/40">
                  <CardContent className="p-5">
                    <h3 className="font-display font-semibold mb-2">Pas de réponse ?</h3>
                    <p className="text-sm text-muted-foreground mb-4">
                      Notre équipe vous répond sous 24h ouvrées par email.
                    </p>
                    <Button variant="outline" className="w-full" asChild>
                      <Link to="/contact">Contacter le support</Link>
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

export default FAQ;
