# FilePack API

![CI](https://github.com/hamdenvogel/filepack-api/actions/workflows/ci.yml/badge.svg)

API Spring Boot para empacotamento/descompactação de arquivos nos formatos **ZIP** e **7z**, com ou sem senha.

Repositório: [hamdenvogel/filepack-api](https://github.com/hamdenvogel/filepack-api)

Demo: https://filepack-api-649100031966.us-central1.run.app

## 📋 Descrição

FilePack API é uma aplicação que recebe múltiplos arquivos via upload e gera um arquivo ZIP criptografado, protegido com a senha fornecida pelo usuário. Ideal para cenários onde é necessário:

- Agrupar múltiplos arquivos em um único pacote
- Proteger arquivos sensíveis com criptografia
- Facilitar o download de múltiplos documentos simultaneamente
- Garantir segurança no transporte de dados

## 🚀 Como Executar a Aplicação

### Pré-requisitos
- Java 25 ou superior
- Maven 3.6+
- GitBash

### Compilar e Executar

```bash
# Compilar o projeto
./mvnw clean package

# Executar a aplicação
./mvnw spring-boot:run
```

A aplicação estará disponível em: `$SERVICE_URL`

### UI no navegador

Abra `$SERVICE_URL/` para a página de upload (empacotar / descompactar) sem usar `curl`.

## 🔧 Endpoints da API

### `POST /api/filepack` — empacotar

**Parâmetros (multipart/form-data):**
- `files`: arquivos para empacotar (múltiplos)
- `password`: senha (obrigatória se `encrypt=true`)
- `encrypt`: `true` (padrão) ou `false` — com/sem senha
- `format`: `zip` (padrão) ou `7z`

**Formatos suportados:** ZIP (Zip4j/AES) e 7z (Commons Compress/AES-256).

### `POST /api/filepack/unpack` — descompactar

**Parâmetros:**
- `file`: arquivo `.zip` ou `.7z`
- `password`: senha (se o arquivo estiver protegido)

**Resposta:** ZIP **sem senha** contendo os arquivos extraídos.

### `GET /api/filepack/sobre` — metadados da API

Retorna JSON com autor (HV Softwares), versão, data/hora da alteração, formatos e endpoints.

## 📝 Exemplos de Uso com cURL

> **Nota:** Os arquivos de exemplo estão localizados na pasta `data/` na raiz do projeto.

### Configurar URL do Serviço

Antes de executar os testes, defina a variável `SERVICE_URL` de acordo com seu ambiente:

**Executando localmente:**
```bash
export SERVICE_URL="http://localhost:8080"
```

**Executando na AWS (substitua pelo endereço do seu ALB):**
```bash
export SERVICE_URL="http://seu-alb-123456.us-east-1.elb.amazonaws.com"
```

---

```bash
# ZIP com senha (padrão)
curl -X POST $SERVICE_URL/api/filepack \
  -F "files=@data/infrastructure_config.xml" \
  -F "password=teste123" \
  --output validation_pack.zip

# ZIP sem senha
curl -X POST $SERVICE_URL/api/filepack \
  -F "files=@data/infrastructure_config.xml" \
  -F "encrypt=false" \
  --output plain_pack.zip

# 7z com senha
curl -X POST $SERVICE_URL/api/filepack \
  -F "files=@data/infrastructure_config.xml" \
  -F "password=teste123" \
  -F "format=7z" \
  --output validation_pack.7z

# Descompactar
curl -X POST $SERVICE_URL/api/filepack/unpack \
  -F "file=@validation_pack.zip" \
  -F "password=teste123" \
  --output unpacked.zip

# Sobre
curl $SERVICE_URL/api/filepack/sobre
```

### Exemplo 0: Validação Básica (1 arquivo)

**Quando usar:** Validação inicial, verificar se a API está funcionando, testes de conectividade

**Tamanho do upload:** ~170 KB

```bash
curl -X POST $SERVICE_URL/api/filepack \
  -F "files=@data/infrastructure_config.xml" \
  -F "password=teste123" \
  --output validation_pack.zip
```

## 🔓 Como Descompactar o ZIP Resultante

## No Windows com GitBash e 7zip

```bash
"C:\Program Files\7-Zip\7z.exe" x validation_pack.zip -pteste123 -oextracted/
```

### No Linux/Mac

```bash
# Usando unzip
unzip -P teste123 validation_pack.zip

# Ou extrair para um diretório específico
unzip -P teste123 validation_pack.zip -d extracted/
```

**Nota:** A senha usada para descompactar deve ser a mesma fornecida no parâmetro `password` durante a criação do ZIP.

## 🔒 Segurança

- O ZIP gerado utiliza criptografia AES
- As senhas não são armazenadas no servidor
- Cada requisição gera um novo arquivo ZIP
- Recomenda-se usar senhas fortes (mínimo 8 caracteres, com letras, números e símbolos)

## 📄 Licença

Este projeto foi desenvolvido para fins educacionais como parte do blog HVogel.
