# Contributing

## Regra em uma frase

> **PR só valida; `main` integra; tag `v*` publica e faz deploy.**

## Branch protection (`main`)

A branch `main` está protegida:

1. alterações entram por **Pull Request**;
2. o check **Build & test** precisa ficar verde;
3. force-push e delete da `main` ficam bloqueados.

Sugestão de nomes de branch: `feature/...`, `fix/...`, `chore/...`, `docs/...`.

> Dono do repositório pode fazer bypass em emergência (`enforce_admins` desligado). O fluxo normal continua sendo PR + CI.

## Estratégia de gatilhos (CI/CD)

| Gatilho | Objetivo | O que roda | Deploy produção? |
|---------|----------|------------|------------------|
| **Pull Request** | Validar a mudança | CI: Maven, Docker, Gitleaks, Trivy | **Não** |
| **Push na `main`** | Integrar código estável | CI completo (mesmos checks) | **Não** |
| **Tag `v*`** (ex.: `v0.1.0`) | Release oficial | Release (JAR) + imagem no GHCR + deploy Cloud Run + health check | **Sim** |

Fluxo completo:

```text
branch → Pull Request → CI verde → merge na main
                                      │
                     (quando for versão oficial)
                                      ▼
                    git tag -a vX.Y.Z -m "..."
                    git push origin vX.Y.Z
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
              GitHub Release    imagem GHCR     Cloud Run + health
```

### Por que o deploy **não** roda em todo push na `main`

Produção só muda quando você cria uma **tag SemVer** (`v*`). Assim:

- commits de feature/docs/fix entram na `main` sem risco de derrubar a demo;
- cada versão em produção fica rastreável (`v0.1.0`, `v0.2.0`, …);
- rollback é escolher a revisão anterior no Cloud Run (ou redeployar uma tag antiga).

### Como publicar uma versão

1. Garanta que a `main` está verde e com o código desejado.
2. Alinhe a versão no `pom.xml` (ex.: `0.2.0`) se ainda não estiver.
3. Crie e envie a tag anotada:

```bash
git checkout main
git pull origin main
git tag -a v0.2.0 -m "Release v0.2.0"
git push origin v0.2.0
```

Os workflows **Release**, **Publish Docker image** e **Deploy to Cloud Run** disparam nessa ordem (o deploy espera a imagem existir no GHCR).

Redeploy manual de uma tag já publicada: Actions → **Deploy to Cloud Run** → *Run workflow* → informe a tag (ex.: `v0.1.0`).

### Segredos

- Credenciais da aplicação (API keys, JDBC, Redis) ficam no **Cloud Run** / Secret Manager — **não** no repositório.
- O GitHub Actions autentica no GCP via **Workload Identity Federation** (OIDC), sem chave JSON no repo.
- Variáveis públicas de deploy (`GCP_PROJECT_ID`, `GCP_REGION`, etc.) estão em *Settings → Variables*.

### Environments do GitHub (opcional)

Não usamos Environments `development` / `production` neste portfólio: o gate de produção é a **tag `v*`**. Environments com aprovação manual podem ser adicionados depois se o deploy precisar de um “ok humano” extra.
