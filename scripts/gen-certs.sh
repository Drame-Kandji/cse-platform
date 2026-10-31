#!/usr/bin/env bash
# Génère une PKI de DÉVELOPPEMENT dans infra/certs/ (répertoire ignoré par Git) :
#   ca.crt / ca.key           autorité de certification auto-signée « CSE Dev CA » (10 ans)
#   localhost.crt / .key      certificat serveur du frontal Nginx (SAN : localhost, 127.0.0.1, web-interne)
#   services.crt / .key       certificat serveur commun aux microservices (SAN : ms-*, localhost), profil tls
#   truststore.p12            magasin de confiance PKCS12 contenant la CA (pour les JVM clientes), si keytool présent
# Usage : scripts/gen-certs.sh [--force]
# En production, ces fichiers sont remplacés par des certificats émis par la PKI de l'entreprise.
set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="${CSE_CERTS_DIR:-$RACINE/infra/certs}"
JOURS_CA="${CSE_CERTS_CA_JOURS:-3650}"
JOURS_SERVEUR="${CSE_CERTS_JOURS:-825}"
# Mot de passe du truststore : ne protège que l'intégrité d'un fichier PUBLIC (la CA), valeur de dev.
TRUSTSTORE_MDP="${CSE_TRUSTSTORE_PASSWORD:-changeit}"
SERVICES="ms-gateway ms-auth ms-besoins ms-achats ms-fournisseurs ms-suivi ms-reporting ms-chatbot ms-notification ms-audit"

command -v openssl >/dev/null || { echo "openssl introuvable"; exit 1; }
mkdir -p "$DEST"

if [[ -f "$DEST/ca.crt" && "${1:-}" != "--force" ]]; then
  echo "Certificats déjà présents dans $DEST (relancer avec --force pour les régénérer)."
  exit 0
fi

echo "Autorité de certification de développement..."
openssl req -x509 -new -nodes -newkey rsa:3072 -sha256 -days "$JOURS_CA" \
  -subj "/C=SN/O=CSE/OU=Developpement/CN=CSE Dev CA" \
  -addext "basicConstraints=critical,CA:TRUE,pathlen:0" \
  -addext "keyUsage=critical,keyCertSign,cRLSign" \
  -keyout "$DEST/ca.key" -out "$DEST/ca.crt" 2>/dev/null

# emettre <nom> <CN> <liste SAN>
emettre() {
  local nom="$1" cn="$2" san="$3" ext
  ext="$(mktemp)"
  cat >"$ext" <<EOF
basicConstraints=critical,CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth,clientAuth
subjectAltName=$san
EOF
  openssl req -new -nodes -newkey rsa:2048 -sha256 -subj "/C=SN/O=CSE/CN=$cn" \
    -keyout "$DEST/$nom.key" -out "$DEST/$nom.csr" 2>/dev/null
  openssl x509 -req -in "$DEST/$nom.csr" -CA "$DEST/ca.crt" -CAkey "$DEST/ca.key" -CAcreateserial \
    -days "$JOURS_SERVEUR" -sha256 -extfile "$ext" -out "$DEST/$nom.crt" 2>/dev/null
  rm -f "$DEST/$nom.csr" "$ext"
  echo "  $nom.crt émis (SAN : $san)"
}

echo "Certificats serveurs..."
emettre localhost localhost "DNS:localhost,DNS:web-interne,IP:127.0.0.1,IP:::1"
san_services="DNS:localhost,IP:127.0.0.1"
for s in $SERVICES; do san_services="$san_services,DNS:$s"; done
emettre services cse-services "$san_services"

if command -v keytool >/dev/null; then
  rm -f "$DEST/truststore.p12"
  keytool -importcert -noprompt -alias cse-dev-ca -file "$DEST/ca.crt" \
    -keystore "$DEST/truststore.p12" -storetype PKCS12 -storepass "$TRUSTSTORE_MDP" >/dev/null
  echo "  truststore.p12 créé"
else
  echo "  keytool absent : truststore.p12 non généré (nécessaire uniquement pour le profil tls)"
fi

# La clé de la CA reste privée. Les clés serveurs sont lues par des conteneurs non-root (UID 101 pour
# nginx-unprivileged, utilisateur « cse » des microservices) : lecture ouverte, acceptable pour une PKI
# de développement seulement : en production, utiliser un secret Docker/Kubernetes avec le bon propriétaire.
chmod 600 "$DEST/ca.key"
chmod 644 "$DEST"/*.crt "$DEST/localhost.key" "$DEST/services.key"
rm -f "$DEST/ca.srl"
echo "PKI de développement prête dans $DEST. Pour éviter l'alerte navigateur, importer $DEST/ca.crt comme autorité de confiance."
