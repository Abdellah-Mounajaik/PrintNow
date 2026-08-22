import { Helmet } from "react-helmet-async";

const SITE_NAME = "PrintNow";
const SITE_URL = "https://printnow.be";

interface SeoProps {
  title: string;
  description: string;
  path?: string;
}

/** Titre, meta description et balises Open Graph/Twitter propres à chaque page. */
const Seo = ({ title, description, path = "" }: SeoProps) => {
  const fullTitle = `${title} | ${SITE_NAME}`;
  const url = `${SITE_URL}${path}`;

  return (
    <Helmet>
      <title>{fullTitle}</title>
      <meta name="description" content={description} />
      <link rel="canonical" href={url} />

      <meta property="og:type" content="website" />
      <meta property="og:site_name" content={SITE_NAME} />
      <meta property="og:title" content={fullTitle} />
      <meta property="og:description" content={description} />
      <meta property="og:url" content={url} />

      <meta name="twitter:card" content="summary" />
      <meta name="twitter:title" content={fullTitle} />
      <meta name="twitter:description" content={description} />
    </Helmet>
  );
};

export default Seo;
