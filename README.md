<div align="center">

<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/goup_logo.gif" width="200" alt="Logo animado do GoUp Events">

<h2>GoUp &#9670; Backend</h2>

<h3>Mais momentos. Mais você.</h3>

<p>API REST do GoUp: eventos e lazer em Blumenau e região, com favoritos, locais próximos e uma IA que só recomenda eventos reais.</p>

<a href="https://goup-events.lovable.app/"><img src="https://img.shields.io/badge/Site-F03D82?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Site do GoUp"></a>&nbsp;&nbsp;&nbsp;
<a href="https://www.youtube.com/@GoUp.Events"><img src="https://img.shields.io/badge/YouTube-FFC931?style=for-the-badge&logo=youtube&logoColor=black" alt="Canal do GoUp no YouTube"></a>&nbsp;&nbsp;&nbsp;
<a href="https://github.com/GoUp-Events/goup-events"><img src="https://img.shields.io/badge/Frontend-F03D82?style=for-the-badge&logo=github&logoColor=white" alt="Repositório do frontend"></a>&nbsp;&nbsp;&nbsp;
<a href="https://github.com/GoUp-Events"><img src="https://img.shields.io/badge/Organiza%C3%A7%C3%A3o-FFC931?style=for-the-badge&logo=github&logoColor=black" alt="Organização GoUp-Events"></a>

<br><br>

<img src="https://img.shields.io/badge/Java_21-F03D82?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21">
<img src="https://img.shields.io/badge/Spring_Boot_4.1-FFC931?style=flat-square&logo=springboot&logoColor=black" alt="Spring Boot 4.1">
<img src="https://img.shields.io/badge/Spring_Security-F03D82?style=flat-square&logo=springsecurity&logoColor=white" alt="Spring Security">
<img src="https://img.shields.io/badge/JWT-FFC931?style=flat-square&logo=jsonwebtokens&logoColor=black" alt="JWT">
<img src="https://img.shields.io/badge/MySQL_8-F03D82?style=flat-square&logo=mysql&logoColor=white" alt="MySQL 8">
<img src="https://img.shields.io/badge/Maven-FFC931?style=flat-square&logo=apachemaven&logoColor=black" alt="Maven">
<img src="https://img.shields.io/badge/Gemini-F03D82?style=flat-square&logo=googlegemini&logoColor=white" alt="Gemini">
<img src="https://img.shields.io/badge/Google_Places-FFC931?style=flat-square&logo=googlemaps&logoColor=black" alt="Google Places">

<br><br>
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">

</div>

<br>

<h2 align="center">&#9679; Sobre o backend</h2>

<p align="center">
Aqui ficam as regras de negócio do <b>GoUp</b>: contas, eventos, locais, favoritos,<br>
planos Free e Premium e a integração com <b>Google Places</b> e <b>Gemini</b>.<br>
O frontend consome esta API, e a verificação de permissões e de plano acontece sempre aqui.
</p>

<p align="center">
<sub>O GoUp <b>não</b> vende ingressos, <b>não</b> é rede social e <b>não</b> é um assistente de IA genérico.</sub>
</p>

<p align="center"><sub>Projeto final do curso de Desenvolvimento Java &mdash; SENAC Entra21</sub></p>

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

<br>

## Sumário

- [Funcionalidades](#funcionalidades)
- [Stack](#stack)
- [Como rodar](#como-rodar)
- [Configuração](#configuração)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Autenticação](#autenticação)
- [Endpoints](#endpoints)
- [Exemplos de uso](#exemplos-de-uso)
- [Regras de negócio](#regras-de-negócio)
- [Erros](#erros)
- [Integrações externas](#integrações-externas)
- [Equipe](#equipe)

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

## Funcionalidades

| &#9670; | O que faz |
|---|---|
| **Contas** | Cadastro e login com JWT. Dois papéis: `USER` e `ADMIN`. |
| **Eventos** | Qualquer usuário logado cria eventos e edita ou exclui só os próprios. O `ADMIN` edita e exclui qualquer um. |
| **Locais via Google Places** | O local vem do autocomplete do Google e é reaproveitado entre eventos. Nunca é digitado à mão. |
| **Categorias** | Lista fixa, criada automaticamente na primeira execução. |
| **Favoritos** | O usuário salva os eventos que quer ir. |
| **Pesquisa e filtros** | Texto, cidade, categorias, período, preço e classificação etária em `GET /events`. |
| **Eventos encerrados** | Calculados no fuso `America/Sao_Paulo`, saem das listagens e vêm marcados com `ended: true` nos detalhes. |
| **Locais próximos** | Restaurantes, bares, parques e mais perto do evento, com distância, preço e avaliação. |
| **Descoberta Mágica** (Premium) | O usuário conta o que quer fazer e o Gemini sugere eventos reais cadastrados. |
| **Recomendações** (Premium) | Eventos sugeridos a partir dos favoritos do usuário. |
| **Planos Free e Premium** | Definidos pelo atributo `premium` do usuário. Veja [Planos](#planos-free-e-premium). |

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

## Stack

| Camada         | Tecnologia                                      |
|----------------|-------------------------------------------------|
| Linguagem      | Java 21                                         |
| Framework      | Spring Boot 4.1 (Spring MVC)                    |
| Persistência   | Spring Data JPA / Hibernate                     |
| Banco de dados | MySQL 8                                         |
| Segurança      | Spring Security + JWT (jjwt 0.12), senhas com BCrypt |
| Validação      | Bean Validation (Jakarta)                       |
| Integrações    | Google Places API (New) e Gemini API            |
| Utilitários    | Lombok                                          |
| Build          | Maven (com wrapper `mvnw`)                      |

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

## Como rodar

### Pré-requisitos

- **JDK 21**
- **MySQL 8** rodando em `localhost:3306`
- Chaves de API do **Google Places** e do **Gemini** (veja [Integrações externas](#integrações-externas)). Sem elas, a API sobe normalmente, mas autocomplete de locais, criação de locais, locais próximos e Descoberta Mágica retornam erro `502`.

### Subindo o MySQL com Docker (opcional)

```bash
docker run -d --name goup-mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:8.4
```

O banco `goup_events` é criado automaticamente na primeira execução.

### Rodando a aplicação

```bash
export GOOGLE_PLACES_API_KEY=sua-chave-do-google
export GEMINI_API_KEY=sua-chave-do-gemini
export GOUP_JWT_SECRET=um-segredo-longo-e-aleatorio

./mvnw spring-boot:run
```

A API sobe em **http://localhost:8080**.

Se o seu MySQL não usa os valores padrão do `application.properties`, defina também `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER` e `MYSQLPASSWORD`.

Para gerar o `.jar`:

```bash
./mvnw clean package
java -jar target/goup-0.0.1-SNAPSHOT.jar
```

> Se aparecer `Permissão negada` ao rodar `./mvnw`, execute `chmod +x mvnw`.

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

## Configuração

As configurações ficam em `src/main/resources/application.properties`. Segredos e chaves vêm sempre de variáveis de ambiente, nunca do código.

| Propriedade                               | Padrão                                               | Descrição                                           |
|-------------------------------------------|------------------------------------------------------|-----------------------------------------------------|
| `spring.datasource.url`                   | env `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`        | Conexão com o MySQL                                 |
| `spring.datasource.username` / `password` | env `MYSQLUSER` / `MYSQLPASSWORD`                    | Credenciais do banco                                |
| `goup.jwt.secret`                         | env `GOUP_JWT_SECRET`                                | Segredo de assinatura do JWT                        |
| `goup.jwt.expiration-ms`                  | env `GOUP_JWT_EXPIRATION_MS` (padrão `7200000`)      | Validade do token em milissegundos (2 horas)        |
| `goup.cors.allowed-origins`               | env `GOUP_CORS_ORIGINS` (padrão: site publicado e localhost nas portas 5173 e 3000) | Origens CORS permitidas |
| `goup.google.places.api-key`              | env `GOOGLE_PLACES_API_KEY`                          | Chave do Google Places                              |
| `goup.google.places.nearby-radius-meters` | `1500`                                               | Raio da busca de locais próximos                    |
| `goup.google.places.nearby-types`         | `restaurant,cafe,bar,bakery,tourist_attraction,park` | Tipos de lugar buscados como "próximos"             |
| `goup.gemini.api-key`                     | env `GEMINI_API_KEY`                                 | Chave do Gemini                                     |
| `goup.gemini.model`                       | env `GEMINI_MODEL` (padrão: `gemini-3.6-flash`)      | Modelo do Gemini usado                              |
| `goup.discovery.candidate-limit`          | `20`                                                 | Máximo de eventos enviados ao Gemini por busca      |
| `goup.plans.free.nearby-limit`            | `3`                                                  | Locais próximos exibidos para o Free (e visitantes) |
| `goup.plans.premium.nearby-limit`         | `10`                                                 | Locais próximos exibidos para o Premium (máx. 19)   |

O schema do banco é gerado pelo Hibernate (`ddl-auto=update`). As categorias são inseridas automaticamente ao iniciar.

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

## Estrutura do projeto

```
src/main/java/com/events/goup
├── client/        Integrações HTTP externas (Google Places, Gemini)
├── config/        Seed de categorias e configuração de cache
├── controller/    Endpoints REST
├── dto/           Objetos de entrada e saída da API (records)
├── entity/        Entidades JPA e enums
├── exception/     Exceções e tratamento global de erros
├── mapper/        Conversão de entidade para DTO
├── repository/    Repositórios Spring Data JPA
├── security/      JWT, filtro de autenticação e regras de acesso
└── service/       Regras de negócio
```

### Modelo de dados

```
User 1───* Event *───1 Location
             │
             *───1 Category (opcional)

User 1───* Favorite *───1 Event      (par user/event único)
```

| Entidade     | Campos principais                                                                                                                                           |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **User**     | `name`, `email` (único), `password` (BCrypt), `role` (`USER`/`ADMIN`), `premium` (padrão `false`), `createdAt`                                              |
| **Event**    | `title`, `description`, `eventDate`, `startTime`, `endTime`, `price`, `status`, `ageRating`, `user`, `location`, `category`                                 |
| **Location** | `placeId` (único), `name`, `formattedAddress`, `address`, `number`, `neighborhood`, `city`, `state`, `zip`, `latitude`, `longitude`, `rating`, `priceLevel` |
| **Category** | `name` (único), `description`                                                                                                                               |
| **Favorite** | `user`, `event`, `createdAt`                                                                                                                                |

**Enums:**

- `EventStatus`: `DRAFT`, `PUBLISHED`, `CANCELLED`, `FINISHED`
- `AgeRating`: `FREE`, `TEN`, `TWELVE`, `FOURTEEN`, `SIXTEEN`, `EIGHTEEN`
- `PriceLevel`: `FREE`, `INEXPENSIVE`, `MODERATE`, `EXPENSIVE`, `VERY_EXPENSIVE`

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

## Autenticação

A API é stateless e usa **JWT**.

1. Faça `POST /auth/register` ou `POST /auth/login`.
2. Pegue o `accessToken` da resposta.
3. Envie o token nas rotas protegidas:

```
Authorization: Bearer <accessToken>
```

Todo usuário cadastrado começa como `USER`. Para tornar um usuário `ADMIN`, altere direto no banco:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@exemplo.com';
```

Todo usuário também começa como **Free** (`premium = false`). Para tornar um usuário **Premium**, altere direto no banco (não existe endpoint para isso):

```sql
UPDATE users SET premium = true WHERE email = 'usuariopremium@gmail.com';
```

A mudança vale já na próxima requisição, sem precisar fazer login de novo: o backend lê `premium` do banco a cada chamada, a partir do e-mail do token. O valor nunca vem do frontend.

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

## Endpoints

**Legenda de acesso:** &#9679; Público &nbsp;&middot;&nbsp; &#9670; Exige login &nbsp;&middot;&nbsp; &#9733; Exige login com `premium = true` (Free recebe `403`)

### Auth

| Método | Rota             | Acesso      | Descrição                  |
|--------|------------------|-------------|----------------------------|
| `POST` | `/auth/register` | &#9679;     | Cria conta e retorna token |
| `POST` | `/auth/login`    | &#9679;     | Faz login e retorna token  |

### Usuário

| Método | Rota                        | Acesso  | Descrição                                       |
|--------|-----------------------------|---------|-------------------------------------------------|
| `GET`  | `/users/me`                 | &#9670; | Dados do usuário logado (inclui `premium`)      |
| `GET`  | `/users/me/events`          | &#9670; | Eventos criados pelo usuário (inclui rascunhos) |
| `GET`  | `/users/me/favorites`       | &#9670; | Favoritos, na ordem em que foram adicionados    |
| `GET`  | `/users/me/recommendations` | &#9733; | Recomendações personalizadas                    |

### Eventos

| Método   | Rota                    | Acesso  | Descrição                                                                  |
|----------|-------------------------|---------|----------------------------------------------------------------------------|
| `GET`    | `/events`               | &#9679; | Lista e pesquisa eventos (rascunhos não aparecem). Filtros abaixo          |
| `GET`    | `/events/{id}`          | &#9679; | Detalhe do evento                                                          |
| `GET`    | `/events/{id}/nearby`   | &#9679; | Lugares próximos: 3 no Free, até 10 no Premium. O filtro `?type=` é &#9733; |
| `POST`   | `/events`               | &#9670; | Cria evento                                                                |
| `PUT`    | `/events/{id}`          | &#9670; | Edita evento (dono ou ADMIN)                                               |
| `DELETE` | `/events/{id}`          | &#9670; | Exclui evento (dono ou ADMIN)                                              |
| `POST`   | `/events/{id}/favorite` | &#9670; | Favorita o evento                                                          |
| `DELETE` | `/events/{id}/favorite` | &#9670; | Remove dos favoritos                                                       |

### Locais

| Método | Rota                            | Acesso  | Descrição                                             |
|--------|---------------------------------|---------|-------------------------------------------------------|
| `GET`  | `/locations`                    | &#9679; | Lista locais                                          |
| `GET`  | `/locations/{id}`               | &#9679; | Detalhe do local                                      |
| `GET`  | `/locations/autocomplete?q=...` | &#9670; | Busca até 5 sugestões de locais pelo nome             |
| `POST` | `/locations`                    | &#9670; | Resolve um `placeId` do Google (salva ou reaproveita) |

### Categorias

| Método | Rota               | Acesso  | Descrição            |
|--------|--------------------|---------|----------------------|
| `GET`  | `/categories`      | &#9679; | Lista categorias     |
| `GET`  | `/categories/{id}` | &#9679; | Detalhe da categoria |

### Planos

| Método | Rota     | Acesso  | Descrição                                                      |
|--------|----------|---------|----------------------------------------------------------------|
| `GET`  | `/plans` | &#9679; | Free e Premium com benefícios e limites (tela "Evoluir plano") |

### Descoberta Mágica

| Método | Rota         | Acesso  | Descrição                             |
|--------|--------------|---------|---------------------------------------|
| `POST` | `/discovery` | &#9733; | Sugestão de eventos pela IA (chatbot) |

### Filtros de `GET /events`

Todos são opcionais e podem ser combinados.

| Parâmetro             | Exemplo                                      | Descrição                                                                 |
|-----------------------|----------------------------------------------|---------------------------------------------------------------------------|
| `q`                   | `festa`                                      | Texto no título ou na descrição                                           |
| `city`                | `Blumenau`                                   | Cidade do local (sem diferenciar maiúsculas)                              |
| `categoryId`          | `12`                                         | Uma categoria                                                             |
| `categoryIds`         | `12,13` ou `categoryIds=12&categoryIds=13`   | Várias categorias; combinável com `categoryId`                            |
| `dateFrom` / `dateTo` | `2026-10-01`                                 | Período (`yyyy-MM-dd`)                                                    |
| `maxPrice`            | `0`                                          | Preço máximo (`0` = só gratuitos)                                         |
| `freeOnly`            | `true`                                       | Apenas eventos com preço zero (`false` não filtra)                        |
| `ageRating`           | `FREE`                                       | Classificação exata: `FREE`, `TEN`, `TWELVE`, `FOURTEEN`, `SIXTEEN`, `EIGHTEEN` |
| `sort`                | `popular`                                    | `date` (padrão) ou `popular` (mais favoritados, &#9733;)                  |

Sem parâmetros, `GET /events` lista apenas eventos ainda não encerrados, em ordem de data. Os filtros não incluem eventos encerrados, mesmo com `dateFrom` no passado. Datas invertidas, IDs e preços negativos e valores inválidos de `ageRating`, `sort` ou `freeOnly` retornam `400`.

```http
GET /events?q=festival&city=Blumenau&categoryIds=1,2&dateFrom=2026-10-01&dateTo=2026-10-31&freeOnly=true&ageRating=FREE&sort=date
```

```json
[
  {
    "id": 1,
    "title": "Festival gratuito",
    "description": "Música ao vivo",
    "eventDate": "2026-10-20",
    "startTime": "18:00:00",
    "endTime": "22:00:00",
    "price": 0,
    "status": "PUBLISHED",
    "ended": false,
    "ageRating": "FREE",
    "user": { "id": 1, "name": "Ana" },
    "location": { "id": 1, "placeId": "ChIJ...", "name": "Parque Vila Germânica", "formattedAddress": "Blumenau - SC", "address": null, "number": null, "neighborhood": null, "city": "Blumenau", "state": "SC", "zip": null, "latitude": -26.9194, "longitude": -49.0661, "rating": null, "priceLevel": null },
    "category": { "id": 1, "name": "Música", "description": null }
  }
]
```

### Autocomplete de locais

Envie o JWT e pelo menos 3 caracteres em `q`. Textos menores retornam `[]` sem consultar o Google. O frontend exibe `name` e `address` e envia o `placeId` escolhido em `POST /locations` ou `POST /events`.

```http
GET /locations/autocomplete?q=Vila%20Germ%C3%A2nica
Authorization: Bearer <token>
```

```json
[
  { "placeId": "ChIJ...", "name": "Parque Vila Germânica", "address": "Velha, Blumenau - SC, Brasil" }
]
```

A busca usa Places API (New), português do Brasil e viés de 50 km em Blumenau. O viés não exclui locais fora da área. Erros do Google ou falta de chave retornam `502`. Ao mostrar sugestões sem mapa, o frontend deve exibir a atribuição exigida pelo Google Places.

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

## Exemplos de uso

### Cadastro

```http
POST /auth/register
Content-Type: application/json

{ "name": "Ana", "email": "ana@exemplo.com", "password": "123456" }
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "user": {
    "id": 1,
    "name": "Ana",
    "email": "ana@exemplo.com",
    "role": "USER",
    "createdAt": "2026-09-30T22:15:23"
  }
}
```

> `expiresIn` está em segundos.

### Criar evento

O `placeId` vem de `GET /locations/autocomplete`. A chave do Google fica somente no backend.

```http
POST /events
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Oktoberfest Blumenau",
  "description": "A maior festa alemã das Américas.",
  "eventDate": "2026-10-08",
  "startTime": "18:00",
  "endTime": "23:59",
  "price": 0,
  "ageRating": "FREE",
  "status": "PUBLISHED",
  "placeId": "ChIJ...",
  "categoryId": 12
}
```

| Campo         | Obrigatório | Observação                              |
|---------------|:-----------:|-----------------------------------------|
| `title`       | &#10003;    |                                         |
| `description` |             |                                         |
| `eventDate`   | &#10003;    | `yyyy-MM-dd`, não pode estar no passado |
| `startTime`   | &#10003;    | `HH:mm`                                 |
| `endTime`     |             | Precisa ser depois de `startTime`       |
| `price`       | &#10003;    | `0` para gratuito                       |
| `ageRating`   | &#10003;    | Ver enum `AgeRating`                    |
| `status`      |             | Padrão `DRAFT`                          |
| `placeId`     | &#10003;    | `placeId` do Google Places              |
| `categoryId`  |             | Id de uma categoria existente           |

Resposta (`201 Created`):

```json
{
  "id": 1,
  "title": "Oktoberfest Blumenau",
  "description": "A maior festa alemã das Américas.",
  "eventDate": "2026-10-08",
  "startTime": "18:00:00",
  "endTime": "23:59:00",
  "price": 0,
  "status": "PUBLISHED",
  "ended": false,
  "ageRating": "FREE",
  "user": {
    "id": 1,
    "name": "Ana"
  },
  "location": {
    "id": 1,
    "placeId": "ChIJ...",
    "name": "Parque Vila Germânica",
    "formattedAddress": "R. Alberto Stein, 199 - Velha, Blumenau - SC, 89036-200",
    "address": "Rua Alberto Stein",
    "number": "199",
    "neighborhood": "Velha",
    "city": "Blumenau",
    "state": "SC",
    "zip": "89036-200",
    "latitude": -26.9156,
    "longitude": -49.0858,
    "rating": 4.7,
    "priceLevel": null
  },
  "category": {
    "id": 12,
    "name": "Festas Típicas",
    "description": "Festas tradicionais e típicas da região"
  }
}
```

### Locais próximos

```http
GET /events/1/nearby
```

```json
[
  {
    "placeId": "ChIJ...",
    "name": "Café Exemplo",
    "address": "R. Exemplo, 100 - Blumenau - SC",
    "type": "Cafeteria",
    "latitude": -26.9160,
    "longitude": -49.0861,
    "distanceMeters": 52,
    "rating": 4.6,
    "userRatingCount": 312,
    "priceLevel": "INEXPENSIVE",
    "googleMapsUri": "https://maps.google.com/?cid=..."
  }
]
```

Premium filtrando por tipo (`restaurant`, `cafe`, `bar`, `bakery`, `tourist_attraction` ou `park`, conforme `goup.google.places.nearby-types`):

```http
GET /events/1/nearby?type=bar
Authorization: Bearer <token>
```

### Recomendações (Premium)

```http
GET /users/me/recommendations
Authorization: Bearer <token>
```

```json
[
  {
    "reason": "Porque você favoritou eventos de Festas Típicas",
    "event": {
      "id": 1,
      "title": "Oktoberfest Blumenau",
      "...": "..."
    }
  }
]
```

### Descoberta Mágica (Premium)

```http
POST /discovery
Authorization: Bearer <token>
Content-Type: application/json

{ "message": "quero algo com cerveja e música no fim de semana", "city": "Blumenau" }
```

```json
{
  "summary": "Separei a Oktoberfest, que tem cerveja artesanal e shows ao vivo durante todo o fim de semana.",
  "events": [
    {
      "id": 1,
      "title": "Oktoberfest Blumenau",
      "...": "..."
    }
  ]
}
```

| Campo     | Obrigatório | Observação                                |
|-----------|:-----------:|-------------------------------------------|
| `message` | &#10003;    | Até 500 caracteres                        |
| `city`    |             | Filtra os candidatos pela cidade do local |

### Favoritos

```http
POST   /events/1/favorite      → 204 No Content
DELETE /events/1/favorite      → 204 No Content
GET    /users/me/favorites     → [ { "favoritedAt": "...", "event": { ... } } ]
```

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

## Regras de negócio

### Usuários e permissões

- Existe um único tipo de conta, com papel `USER` ou `ADMIN`. Não há perfil de organizador separado.
- Qualquer usuário logado pode criar eventos.
- O `USER` edita e exclui só os **próprios** eventos. O `ADMIN` edita e exclui qualquer evento.
    - Evento de outro usuário: **403**.
    - Evento inexistente: **404**.
- O visitante (sem login) pode listar e ver eventos, categorias, locais e locais próximos.
- Exigem login: criar, editar e excluir eventos, favoritar, `/users/me/**`, autocomplete e resolução de locais e a Descoberta Mágica.

### Planos (Free e Premium)

- O plano é só o atributo booleano `premium` do usuário. Não há tabela nem entidade de plano.
- Novos usuários são Free. Premium é definido direto no banco: não existe endpoint de compra nem de troca de plano. A tela de pagamento do frontend é apenas demonstrativa.
- Free e Premium usam **as mesmas rotas e telas**. O backend identifica o usuário pelo token, lê `premium` no banco e aplica a regra:

| Recurso                                     | Free / visitante | Premium |
|---------------------------------------------|:----------------:|:-------:|
| Pesquisa e filtros de eventos               | &#10003;         | &#10003; |
| Locais próximos (`/events/{id}/nearby`)     | até 3            | até 10  |
| Distância, preço e avaliação dos locais     | &#10003;         | &#10003; |
| Filtro por tipo de lugar (`?type=`)         | 403              | &#10003; |
| Eventos em alta (`?sort=popular`)           | 403              | &#10003; |
| Descoberta Mágica (`POST /discovery`)       | 403              | &#10003; |
| Recomendações (`/users/me/recommendations`) | 403              | &#10003; |

- Recurso exclusivo acessado por Free responde **403** com a mensagem `"Recurso exclusivo do GoUp Premium"`. Esconder o botão no frontend não é a proteção: a verificação é feita no backend.
- A regra é sempre `premium = true`, nunca um e-mail específico.

### Eventos

- Status padrão: `DRAFT` (rascunho).
- Rascunhos não aparecem na listagem pública. Só o dono e o ADMIN conseguem abri-los; para os demais, a resposta é `404`.
- A data não pode estar no passado, e o horário de término precisa ser depois do de início.
- Um evento terminou se o status é `FINISHED` ou se, no fuso `America/Sao_Paulo`, a data já passou. No dia do evento, ele termina quando a hora atual alcança `endTime`; sem `endTime`, termina no dia seguinte.
- `GET /events`, a Descoberta Mágica e as recomendações excluem eventos terminados.
- `GET /events/{id}`, `GET /users/me/events` e `GET /users/me/favorites` continuam mostrando os eventos acessíveis, com `ended: true` no `EventResponse` quando terminados.

### Locais

- Vêm sempre do **Google Places**; nunca são digitados.
- O frontend manda o `placeId`. O backend busca o Place Details e salva, ou reaproveita se o `placeId` já existir.
- O `placeId` é único e um local pode ser usado por vários eventos.
- Não há edição nem exclusão manual.

### Categorias

- Lista fixa, criada pelo seed em `config/CategorySeeder.java`.

### Favoritos

- O par (usuário, evento) é único. Favoritar de novo não dá erro.
- A lista é ordenada pela data em que o evento foi favoritado.
- Excluir um evento remove os favoritos dele.

### Descoberta Mágica

1. O backend busca os candidatos: eventos `PUBLISHED` ainda não terminados, filtrados pela cidade (se informada), até 20.
2. Só esses eventos são enviados ao Gemini.
3. O Gemini responde `{ eventIds, summary }` em JSON.
4. Qualquer id fora da lista de candidatos é descartado. A IA nunca devolve evento inventado.
5. Se não houver candidatos, o Gemini não é chamado.
6. Exclusiva do Premium: a verificação acontece antes de qualquer chamada ao Gemini.

### Recomendações personalizadas

- Exclusivas do Premium.
- Consideram eventos `PUBLISHED` de hoje em diante, sem os já favoritados e sem os criados pelo próprio usuário.
- Pontuação: categorias que o usuário mais favorita pesam mais, depois as cidades. Empates seguem a data. Sem favoritos, viram os próximos eventos da agenda.
- Até 10 eventos, cada um com o motivo da recomendação.

### Locais próximos

- Free (e visitante) recebe no máximo **3** lugares; Premium recebe até **10**. Os limites são configuráveis.
- O backend faz uma única busca no Google com o limite do Premium e corta conforme o plano, então os dois planos compartilham o mesmo cache e a mesma chamada.
- Cada lugar vem com distância, avaliação, número de avaliações e faixa de preço (quando o Google informa).
- O próprio local do evento é excluído do resultado.
- O resultado fica em cache por local (e tipo, quando filtrado) enquanto a aplicação está rodando.

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

## Erros

Os erros tratados pela API seguem o formato abaixo. As exceções são o `401` de rota protegida sem token, que volta sem corpo, e o `405`, que usa o formato padrão do Spring.

```json
{
  "timestamp": "2026-09-30T22:15:39.69",
  "status": 400,
  "message": "Dados inválidos",
  "errors": [
    {
      "field": "message",
      "message": "A mensagem é obrigatória"
    }
  ]
}
```

| Status | Quando                                                                                             |
|--------|----------------------------------------------------------------------------------------------------|
| `400`  | Dados inválidos ou regra violada (ex.: término antes do início)                                    |
| `401`  | Sem login, token inválido ou credenciais erradas                                                   |
| `403`  | Logado, mas sem permissão (ex.: editar evento de outro usuário ou usar recurso Premium sendo Free) |
| `404`  | Recurso não encontrado                                                                             |
| `405`  | Método HTTP não suportado na rota                                                                  |
| `409`  | Conflito (ex.: e-mail já cadastrado)                                                               |
| `502`  | Falha no Google Places ou no Gemini, ou chave não configurada                                      |

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

## Integrações externas

### Google Places API (New)

Usada para:

- buscar sugestões de locais (**Autocomplete**);
- buscar os dados do local a partir do `placeId` (**Place Details**);
- buscar os locais próximos (**Nearby Search**).

Para configurar:

1. No [Google Cloud Console](https://console.cloud.google.com/), crie um projeto e ative a **Places API (New)**.
2. Crie uma chave de API e defina a variável `GOOGLE_PLACES_API_KEY`.
3. No frontend, chame `GET /locations/autocomplete` com JWT e envie o `placeId` escolhido. A chave permanece no backend.

> As chamadas usam *field mask* e pedem só os campos necessários, para reduzir o custo.

### Gemini API

Usada na Descoberta Mágica.

1. Gere uma chave no [Google AI Studio](https://aistudio.google.com/).
2. Defina a variável `GEMINI_API_KEY`.
3. O modelo pode ser trocado em `goup.gemini.model`.

<div align="center">
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
</div>

<h2 align="center">&#9679; Equipe</h2>

<table align="center">
  <tr>
    <td align="center" width="25%">
      <a href="https://github.com/LucasMaxHartmann">
        <img src="https://github.com/LucasMaxHartmann.png?size=200" width="100" style="border-radius:50%" alt="Foto de Lucas Max Hartmann"><br>
        <b>Lucas Max Hartmann</b>
      </a><br>
      <sub>Líder</sub>
    </td>
    <td align="center" width="25%">
      <a href="https://github.com/JenniferReetz">
        <img src="https://github.com/JenniferReetz.png?size=200" width="100" style="border-radius:50%" alt="Foto de Jennifer Reetz"><br>
        <b>Jennifer Reetz</b>
      </a><br>
      <sub>Logo, backend e<br>regras de negócio</sub>
    </td>
    <td align="center" width="25%">
      <a href="https://github.com/StefanieReetz">
        <img src="https://github.com/StefanieReetz.png?size=200" width="100" style="border-radius:50%" alt="Foto de Stefanie Reetz"><br>
        <b>Stefanie Reetz</b>
      </a><br>
      <sub>Backend e<br>integrações externas</sub>
    </td>
    <td align="center" width="25%">
      <a href="https://github.com/fehhy-droid">
        <img src="https://github.com/fehhy-droid.png?size=200" width="100" style="border-radius:50%" alt="Foto de Fernanda"><br>
        <b>Fernanda</b>
      </a><br>
      <sub>Frontend e<br>idealização do GoUp</sub>
    </td>
  </tr>
</table>

<p align="center">
  <a href="https://github.com/GoUp-Events/GoUp-Events-Backend/graphs/contributors"><img src="https://contrib.rocks/image?repo=GoUp-Events/GoUp-Events-Backend" alt="Contribuidores do backend"></a>
</p>

<div align="center">
<br>
<img src="https://raw.githubusercontent.com/GoUp-Events/.github/main/profile/assets/divisor.svg" width="100%" height="6" alt="">
<br><br>

<b>GoUp</b> &nbsp;&#9679;&nbsp; Blumenau - SC &nbsp;&#9679;&nbsp; &#169; 2026<br>
<sub>Projeto final &mdash; SENAC Entra21</sub><br><br>

<a href="https://goup-events.lovable.app/">Site</a> &nbsp;&#9670;&nbsp;
<a href="https://www.youtube.com/@GoUp.Events">YouTube</a> &nbsp;&#9670;&nbsp;
<a href="https://github.com/GoUp-Events/goup-events">Frontend</a> &nbsp;&#9670;&nbsp;
<a href="https://github.com/GoUp-Events">Organização</a>

</div>
