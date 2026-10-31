#!/usr/bin/env bash
# Test de fumée de la plateforme via la passerelle : pour chaque rôle, appelle les lectures principales
# et affiche le statut HTTP et le volume renvoyé. Usage : scripts/smoke.sh
set -uo pipefail
GW=${GW:-http://localhost:8080}
RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
appel() { # role url
  local code corps; corps=$(curl -s -w '\n%{http_code}' -H "Authorization: Bearer $TOKEN" "$GW$2")
  code=$(tail -1 <<<"$corps"); corps=$(sed '$d' <<<"$corps")
  local info; info=$(python3 -c 'import sys,json
try:
  d=json.loads(sys.stdin.read())
  if isinstance(d,dict) and "totalElements" in d: print("total="+str(d["totalElements"]))
  elif isinstance(d,list): print("n="+str(len(d)))
  elif isinstance(d,dict) and "code" in d and "status" in d: print("ERREUR "+d["code"]+" : "+str(d.get("detail"))[:80])
  else: print("clés="+",".join(list(d)[:6]))
except Exception: print("(non JSON)")' <<<"$corps")
  printf '  %-4s %-3s %-58s %s\n' "$1" "$code" "$2" "$info"
}
declare -A COMPTES=([CHEF]=chef.chantier@cse.sn [RM]=resp.materiel@cse.sn [RA]=resp.achats@cse.sn [DAF]=daf@cse.sn [DG]=dg@cse.sn [ADMIN]=admin@cse.sn)
for r in CHEF RM RA DAF DG ADMIN; do
  TOKEN=$("$RACINE/scripts/token.sh" "${COMPTES[$r]}") || { echo "  $r : jeton impossible"; continue; }
  echo "== $r"
  appel $r /api/me
  case $r in
    CHEF) for u in /api/chantiers /api/besoins /api/besoins/statistiques /api/materiels /api/affectations /api/maintenances /api/pannes /api/notifications "/api/reporting/kpis" /api/chatbot/suggestions /api/appels-offres; do appel $r "$u"; done ;;
    RM) for u in /api/besoins /api/besoins/consolidation /api/materiels /api/materiels/carte "/api/affectations/planning?du=2026-08-01&au=2026-12-31" /api/maintenances /api/referentiels/types-engins /api/fournisseurs /api/besoins-a-acheter /api/reporting/kpis /api/notifications/non-lues/count; do appel $r "$u"; done ;;
    RA) for u in /api/appels-offres /api/contrats /api/fournisseurs /api/fournisseurs/classement /api/fournisseurs/statistiques /api/litiges /api/factures /api/besoins-a-acheter /api/reporting/kpis; do appel $r "$u"; done ;;
    DAF) for u in "/api/engagements?statut=EN_ATTENTE" /api/factures /api/finance/synthese /api/contrats /api/reporting/kpis; do appel $r "$u"; done ;;
    DG) for u in "/api/reporting/kpis" "/api/reporting/series?indicateur=MONTANT_ENGAGE" /api/appels-offres /api/materiels /api/fournisseurs /api/finance/synthese /api/audit; do appel $r "$u"; done ;;
    ADMIN) for u in /api/admin/utilisateurs /api/admin/roles /api/audit /api/audit/statistiques /api/plateforme/etat /api/contrats; do appel $r "$u"; done ;;
  esac
done
