import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Bot, Send, User, Sparkle, RotateCcw, Printer } from "lucide-react";
import { chatbotService } from "@/services/chatbot.service";
import type { MessageChat } from "@/models/chatbot.model";

type Message = { id: string; role: "bot" | "user"; text: string };

const FAQ = () => {
  const { t } = useTranslation("faq");

  /**
   * Questions proposées au visiteur. Ce ne sont que des raccourcis de saisie :
   * les réponses viennent toujours de l'assistant, jamais d'un texte figé ici.
   */
  const questionsFrequentes = t("questions", { returnObjects: true }) as string[];
  const suggestions = questionsFrequentes.slice(0, 5);

  const [messages, setMessages] = useState<Message[]>([
    { id: "welcome", role: "bot", text: t("chat.welcomeMessage") },
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
      // Le message d'accueil n'a pas été écrit par l'assistant : l'envoyer
      // reviendrait à lui faire croire qu'il l'a dit.
      const historique: MessageChat[] = conversation
        .filter((m) => m.id !== "welcome")
        .map((m) => ({ role: m.role === "bot" ? "assistant" : "user", content: m.text }));

      const reponse = await chatbotService.demander(historique);
      setMessages((prev) => [...prev, { id: `b-${Date.now()}`, role: "bot", text: reponse }]);
    } catch (e) {
      // Le service formule déjà un message compréhensible par le visiteur.
      const texte = e instanceof Error ? e.message : t("chat.errorFallback");
      setMessages((prev) => [...prev, { id: `b-${Date.now()}`, role: "bot", text: texte }]);
    } finally {
      setTyping(false);
      inputRef.current?.focus();
    }
  };

  const reset = () => {
    setMessages([{ id: "welcome", role: "bot", text: t("chat.welcomeMessage") }]);
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
              {t("hero.badge")}
            </div>
            <h1 className="font-display text-3xl md:text-5xl font-bold text-primary-foreground mb-4">
              {t("hero.title")}
            </h1>
            <p className="text-lg text-primary-foreground/80">
              {t("hero.subtitle")}
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
                      <p className="font-display font-semibold leading-tight">{t("chat.assistantName")}</p>
                      <p className="text-xs text-secondary">{t("chat.online")}</p>
                    </div>
                  </div>
                  <Button variant="ghost" size="sm" onClick={reset}>
                    <RotateCcw className="h-4 w-4" />
                    <span className="hidden sm:inline">{t("chat.reset")}</span>
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
                      placeholder={t("chat.inputPlaceholder")}
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
                      {t("sidebar.questionsTitle")}
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
                    <h3 className="font-display font-semibold mb-2">{t("sidebar.noAnswerTitle")}</h3>
                    <p className="text-sm text-muted-foreground mb-4">
                      {t("sidebar.noAnswerText")}
                    </p>
                    <Button variant="outline" className="w-full" asChild>
                      <Link to="/contact">{t("sidebar.contactButton")}</Link>
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
