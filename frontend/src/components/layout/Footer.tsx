import { Link } from "react-router-dom";
import { Mail, Phone } from "lucide-react";

// lucide-react n'inclut plus les icônes de marque (Facebook, Twitter, Instagram,
// Linkedin) : on les redessine ici en SVG, dans le même style (trait, sans remplissage).
const iconProps = {
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 2,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
};

const Facebook = ({ className }: { className?: string }) => (
  <svg {...iconProps} className={className}>
    <path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z" />
  </svg>
);

const Twitter = ({ className }: { className?: string }) => (
  <svg {...iconProps} className={className}>
    <path d="M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z" />
  </svg>
);

const Instagram = ({ className }: { className?: string }) => (
  <svg {...iconProps} className={className}>
    <rect width="20" height="20" x="2" y="2" rx="5" ry="5" />
    <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z" />
    <line x1="17.5" x2="17.51" y1="6.5" y2="6.5" />
  </svg>
);

const Linkedin = ({ className }: { className?: string }) => (
  <svg {...iconProps} className={className}>
    <path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z" />
    <rect width="4" height="12" x="2" y="9" />
    <circle cx="4" cy="4" r="2" />
  </svg>
);

const Footer = () => {
  return (
    <footer className="bg-foreground text-background">
      <div className="container mx-auto px-4 py-14">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-10">
          {/* Brand */}
          <div className="lg:col-span-1">
            <Link to="/" className="flex items-center mb-4">
              <img src="/logo-white.png" alt="PrintNow" className="h-9 w-auto object-contain" />
            </Link>
            <p className="text-background/70 text-sm mb-6 leading-relaxed">
              La plateforme qui connecte vos besoins d'impression aux meilleurs professionnels près de chez vous.
            </p>
            <div className="flex gap-3">
              <a href="#" className="w-9 h-9 rounded-lg bg-background/10 flex items-center justify-center hover:bg-background/20 transition-colors">
                <Facebook className="h-4 w-4" />
              </a>
              <a href="#" className="w-9 h-9 rounded-lg bg-background/10 flex items-center justify-center hover:bg-background/20 transition-colors">
                <Twitter className="h-4 w-4" />
              </a>
              <a href="#" className="w-9 h-9 rounded-lg bg-background/10 flex items-center justify-center hover:bg-background/20 transition-colors">
                <Instagram className="h-4 w-4" />
              </a>
              <a href="#" className="w-9 h-9 rounded-lg bg-background/10 flex items-center justify-center hover:bg-background/20 transition-colors">
                <Linkedin className="h-4 w-4" />
              </a>
            </div>
          </div>

          {/* Services */}
          <div>
            <h4 className="font-display font-semibold text-lg mb-4">Services</h4>
            <ul className="space-y-3 text-sm">
              <li>
                <Link to="/services/documents" className="text-background/70 hover:text-background transition-colors">
                  Documents & CV
                </Link>
              </li>
              <li>
                <Link to="/services/flyers" className="text-background/70 hover:text-background transition-colors">
                  Flyers & Affiches
                </Link>
              </li>
              <li>
                <Link to="/services/cartes" className="text-background/70 hover:text-background transition-colors">
                  Cartes de visite
                </Link>
              </li>
              <li>
                <Link to="/services/photos" className="text-background/70 hover:text-background transition-colors">
                  Impression photo
                </Link>
              </li>
              <li>
                <Link to="/services/grand-format" className="text-background/70 hover:text-background transition-colors">
                  Grand format
                </Link>
              </li>
            </ul>
          </div>

          {/* Company */}
          <div>
            <h4 className="font-display font-semibold text-lg mb-4">Entreprise</h4>
            <ul className="space-y-3 text-sm">
              <li>
                <Link to="/a-propos" className="text-background/70 hover:text-background transition-colors">
                  À propos
                </Link>
              </li>
              <li>
                <Link to="/devenir-partenaire" className="text-background/70 hover:text-background transition-colors">
                  Devenir partenaire
                </Link>
              </li>
              <li>
                <Link to="/faq" className="text-background/70 hover:text-background transition-colors">
                  FAQ
                </Link>
              </li>
              <li>
                <Link to="/blog" className="text-background/70 hover:text-background transition-colors">
                  Blog
                </Link>
              </li>
              <li>
                <Link to="/contact" className="text-background/70 hover:text-background transition-colors">
                  Contact
                </Link>
              </li>
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h4 className="font-display font-semibold text-lg mb-4">Contact</h4>
            <ul className="space-y-3 text-sm">
              <li className="flex items-center gap-2 text-background/70">
                <Mail className="h-4 w-4" />
                contact@printnow.be
              </li>
              <li className="flex items-center gap-2 text-background/70">
                <Phone className="h-4 w-4" />
                +32 2 123 45 67
              </li>
            </ul>
            <div className="mt-6">
              <h5 className="font-medium text-sm mb-2">Langues</h5>
              <div className="flex gap-2">
                <button className="px-3 py-1 text-xs rounded-md bg-background/20 hover:bg-background/30 transition-colors">FR</button>
                <button className="px-3 py-1 text-xs rounded-md bg-background/10 hover:bg-background/20 transition-colors">EN</button>
                <button className="px-3 py-1 text-xs rounded-md bg-background/10 hover:bg-background/20 transition-colors">NL</button>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="mt-12 pt-8 border-t border-background/10 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="text-sm text-background/50">
            © 2024 PrintNow. Tous droits réservés.
          </p>
          <div className="flex gap-6 text-sm">
            <Link to="/mentions-legales" className="text-background/50 hover:text-background/80 transition-colors">
              Mentions légales
            </Link>
            <Link to="/cgv" className="text-background/50 hover:text-background/80 transition-colors">
              CGV
            </Link>
            <Link to="/confidentialite" className="text-background/50 hover:text-background/80 transition-colors">
              Confidentialité
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
