# Contrats d'événements Kafka

| Topic | Producteur | Consommateurs | Événements |
|---|---|---|---|
| cse.besoins.events | ms-besoins | ms-achats, ms-suivi, ms-notification, ms-audit, ms-reporting | BesoinSoumis, BesoinValide, BesoinRejete, BesoinAffecteDirect, BesoinEnAchat |
| cse.achats.events | ms-achats | ms-fournisseurs, ms-suivi, ms-notification, ms-audit, ms-reporting | AppelOffrePublie, OffreDeposee, AppelOffreCloture, MarcheAttribue, ContratSigne, EngagementDemande, EngagementValide, LivraisonEffectuee, FactureRecue, PaiementMandate, AlerteRetardLivraison, AlerteBudget |
| cse.fournisseurs.events | ms-fournisseurs | ms-achats, ms-notification, ms-audit, ms-reporting | FournisseurInscrit, FournisseurValide, FournisseurSuspendu, FournisseurBlackliste, ScoreRecalcule, DocumentExpire, LitigeOuvert, LitigeResolu |
| cse.suivi.events | ms-suivi | ms-besoins, ms-achats, ms-fournisseurs, ms-notification, ms-audit, ms-reporting | MaterielEnregistre, MaterielAffecte, ReceptionConfirmee, PanneSignalee, MaintenanceDeclaree, MaintenanceCloturee, MaterielReforme, AlerteMaintenance, FinAffectationProchaine, MaterielIndisponible |
| cse.audit.events | tous | ms-audit | AuditEvent |

Tous les événements utilisent `EventEnvelope<T>` avec version 1.0.
