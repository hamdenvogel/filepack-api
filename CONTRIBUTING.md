# Contributing

## Branch protection (main)

A branch `main` está protegida:

1. alterações entram por **Pull Request**;
2. o check **Build & test** precisa ficar verde;
3. force-push e delete da `main` ficam bloqueados.

Fluxo:

```text
branch → Pull Request → CI (Build & test) → merge
```

Sugestão de nomes de branch: `feature/...`, `fix/...`, `chore/...`, `docs/...`.

> Dono do repositório pode fazer bypass em emergência (`enforce_admins` desligado). O fluxo normal continua sendo PR + CI.
