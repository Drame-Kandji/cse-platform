# Opérations

## Démarrage
```bash
make up
make build
```

## Sauvegarde
pg_dump quotidien, rétention 90 jours.

## Supervision
Actuator /health, /metrics, /prometheus

## RTO/RPO
RTO < 4h documenté, tests de restauration requis.
