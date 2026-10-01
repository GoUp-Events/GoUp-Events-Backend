# GoUp — Backend

API REST do **GoUp**, uma plataforma de **descoberta e divulgação de eventos e lazer em Blumenau e região**.

Na plataforma, as pessoas encontram o que fazer, publicam eventos, favoritam o que querem ir e recebem sugestões de uma IA que só recomenda eventos reais cadastrados.

> O GoUp **não** vende ingressos, **não** é rede social, **não** é planejador de viagem completo e **não** é um assistente de IA genérico.

---

## Sumário

- [Stack](#stack)
- [Funcionalidades](#funcionalidades)
- [Como rodar](#como-rodar)
- [Configuração](#configuração)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Autenticação](#autenticação)
- [Endpoints](#endpoints)
- [Exemplos de uso](#exemplos-de-uso)
- [Regras de negócio](#regras-de-negócio)
- [Erros](#erros)
- [Integrações externas](#integrações-externas)
- [Próximos passos](#próximos-passos)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 (Spring MVC) |
| Persistência | Spring Data JPA / Hibernate |
| Banco de dados | MySQL 8 |
| Segurança | Spring Security + JWT (jjwt), senhas com BCrypt |
| Validação | Bean Validation (Jakarta) |
| Integrações | Google Places API (New) e Gemini API |
| Utilitários | Lombok |
| Build | Maven (com wrapper `mvnw`) |

---

## Funcionalidades

- **Contas**: cadastro e login com JWT. Existem dois papéis, `USER` e `ADMIN`.
- **Eventos**: qualquer usuário logado cria eventos. Cada um edita e exclui só os próprios; o ADMIN pode editar e excluir qualquer evento.
- **Locais via Google Places**: o local do evento vem do autocomplete do Google e é reaproveitado entre eventos.
- **Categorias**: lista fixa, criada automaticamente.
- **Favoritos**: o usuário salva os eventos que quer ir. É a versão simplificada do "roteiro".
- **Descoberta Mágica**: o usuário descreve o que quer fazer e a IA (Gemini) sugere eventos reais.
- **Locais próximos**: na tela do evento, mostra os 3 lugares mais próximos (restaurantes, bares, parques etc.).

---

## Como rodar

### Pré-requisitos

- **JDK 21**
- **MySQL 8** rodando em `localhost:3306`
- Chaves de API do **Google Places** e do **Gemini** (veja [Integrações externas](#integrações-externas)). Sem elas, a API sobe normalmente, mas criar locais, locais próximos e a Descoberta Mágica retornam erro 502.

### Subindo o MySQL com Docker (opcional)

```bash
docker run -d --name goup-mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:8.4
```

O banco `goup_events` é criado automaticamente na primeira execução.

### Rodando a aplicação

```bash
export GOOGLE_PLACES_API_KEY=sua-chave-do-google
export GEMINI_API_KEY=sua-chave-do-gemini

./mvnw spring-boot:run
```

A API sobe em **http://localhost:8080**.

Para gerar o `.jar`:

```bash
./mvnw clean package
java -jar target/goup-0.0.1-SNAPSHOT.jar
```

> Se aparecer `Permissão negada` ao rodar `./mvnw`, execute `chmod +x mvnw`.

---

## Configuração

As configurações ficam em `src/main/resources/application.properties`.

| Propriedade | Padrão | Descrição |
|---|---|---|
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/goup_events...` | Conexão com o MySQL |
| `spring.datasource.username` / `password` | `root` / `root` | Credenciais do banco |
| `goup.jwt.secret` | — | Segredo de assinatura do JWT |
| `goup.jwt.expiration-ms` | — | Validade do token em milissegundos |
| `goup.google.places.api-key` | env `GOOGLE_PLACES_API_KEY` | Chave do Google Places |
| `goup.google.places.nearby-radius-meters` | `1500` | Raio da busca de locais próximos |
| `goup.google.places.nearby-types` | `restaurant,cafe,bar,bakery,tourist_attraction,park` | Tipos de lugar buscados como "próximos" |
| `goup.gemini.api-key` | env `GEMINI_API_KEY` | Chave do Gemini |
| `goup.gemini.model` | `gemini-2.5-flash` | Modelo do Gemini usado |
| `goup.discovery.candidate-limit` | `20` | Máximo de eventos enviados ao Gemini por busca |

O schema do banco é gerado pelo Hibernate (`ddl-auto=update`). As categorias são inseridas automaticamente ao iniciar.

---

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

| Entidade | Campos principais |
|---|---|
| **User** | `name`, `email` (único), `password` (BCrypt), `role` (`USER`/`ADMIN`), `createdAt` |
| **Event** | `title`, `description`, `eventDate`, `startTime`, `endTime`, `price`, `status`, `ageRating`, `user`, `location`, `category` |
| **Location** | `placeId` (único), `name`, `formattedAddress`, `address`, `number`, `neighborhood`, `city`, `state`, `zip`, `latitude`, `longitude`, `rating`, `priceLevel` |
| **Category** | `name` (único), `description` |
| **Favorite** | `user`, `event`, `createdAt` |

**Enums:**
- `EventStatus`: `DRAFT`, `PUBLISHED`, `CANCELLED`, `FINISHED`
- `AgeRating`: `FREE`, `TEN`, `TWELVE`, `FOURTEEN`, `SIXTEEN`, `EIGHTEEN`
- `PriceLevel`: `FREE`, `INEXPENSIVE`, `MODERATE`, `EXPENSIVE`, `VERY_EXPENSIVE`

---

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

---

## Endpoints

🌐 = público · 🔒 = exige login

### Auth

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/auth/register` | 🌐 | Cria conta e retorna token |
| `POST` | `/auth/login` | 🌐 | Faz login e retorna token |

### Usuário

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/users/me` | 🔒 | Dados do usuário logado |
| `GET` | `/users/me/events` | 🔒 | Eventos criados pelo usuário (inclui rascunhos) |
| `GET` | `/users/me/favorites` | 🔒 | Favoritos, na ordem em que foram adicionados |

### Eventos

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/events` | 🌐 | Lista eventos (rascunhos não aparecem) |
| `GET` | `/events/{id}` | 🌐 | Detalhe do evento |
| `GET` | `/events/{id}/nearby` | 🌐 | Os 3 lugares mais próximos do evento |
| `POST` | `/events` | 🔒 | Cria evento |
| `PUT` | `/events/{id}` | 🔒 | Edita evento (dono ou ADMIN) |
| `DELETE` | `/events/{id}` | 🔒 | Exclui evento (dono ou ADMIN) |
| `POST` | `/events/{id}/favorite` | 🔒 | Favorita o evento |
| `DELETE` | `/events/{id}/favorite` | 🔒 | Remove dos favoritos |

### Locais

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/locations` | 🌐 | Lista locais |
| `GET` | `/locations/{id}` | 🌐 | Detalhe do local |
| `POST` | `/locations` | 🔒 | Resolve um `placeId` do Google (salva ou reaproveita) |

### Categorias

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/categories` | 🌐 | Lista categorias |
| `GET` | `/categories/{id}` | 🌐 | Detalhe da categoria |

### Descoberta Mágica

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/discovery` | 🔒 | Sugestão de eventos pela IA |

---

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
  "expiresIn": 900000,
  "user": { "id": 1, "name": "Ana", "email": "ana@exemplo.com", "role": "USER", "createdAt": "2026-09-30T22:15:23" }
}
```

> `expiresIn` está em segundos.

### Criar evento

O `placeId` vem do autocomplete do Google Places no frontend.

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

| Campo | Obrigatório | Observação |
|---|---|---|
| `title` | ✅ | |
| `description` | | |
| `eventDate` | ✅ | `yyyy-MM-dd`, não pode estar no passado |
| `startTime` | ✅ | `HH:mm` |
| `endTime` | | Precisa ser depois de `startTime` |
| `price` | ✅ | `0` para gratuito |
| `ageRating` | ✅ | Ver enum `AgeRating` |
| `status` | | Padrão `DRAFT` |
| `placeId` | ✅ | `placeId` do Google Places |
| `categoryId` | | Id de uma categoria existente |

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
  "ageRating": "FREE",
  "user": { "id": 1, "name": "Ana" },
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
  "category": { "id": 12, "name": "Festas Típicas", "description": "Festas tradicionais e típicas da região" }
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
    "googleMapsUri": "https://maps.google.com/?cid=..."
  }
]
```

### Descoberta Mágica

```http
POST /discovery
Authorization: Bearer <token>
Content-Type: application/json

{ "message": "quero algo com cerveja e música no fim de semana", "city": "Blumenau" }
```

```json
{
  "summary": "Separei a Oktoberfest, que tem cerveja artesanal e shows ao vivo durante todo o fim de semana.",
  "events": [ { "id": 1, "title": "Oktoberfest Blumenau", "...": "..." } ]
}
```

| Campo | Obrigatório | Observação |
|---|---|---|
| `message` | ✅ | Até 500 caracteres |
| `city` | | Filtra os candidatos pela cidade do local |

### Favoritos

```http
POST   /events/1/favorite      → 204 No Content
DELETE /events/1/favorite      → 204 No Content
GET    /users/me/favorites     → [ { "favoritedAt": "...", "event": { ... } } ]
```

---

## Regras de negócio

### Usuários e permissões
- Existe um único tipo de conta, com papel `USER` ou `ADMIN`. Não há perfil de organizador separado.
- Qualquer usuário logado pode criar eventos.
- O `USER` edita e exclui só os **próprios** eventos. O `ADMIN` edita e exclui qualquer evento.
  - Evento de outro usuário → **403**.
  - Evento inexistente → **404**.
- O visitante (sem login) pode listar e ver eventos, ver categorias, ver locais e ver locais próximos.
- Exigem login: criar, editar e excluir eventos, favoritar, `/users/me/**`, resolver locais e a Descoberta Mágica.

### Eventos
- Status padrão: `DRAFT` (rascunho).
- Rascunhos não aparecem na listagem pública. Só o dono e o ADMIN conseguem abri-los; para os demais, a resposta é 404.
- A data não pode estar no passado, e o horário de término precisa ser depois do de início.

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
1. O backend busca os candidatos: eventos `PUBLISHED`, com data de hoje em diante, filtrados pela cidade (se informada), até 20.
2. Só esses eventos são enviados ao Gemini.
3. O Gemini responde `{ eventIds, summary }` em JSON.
4. Qualquer id fora da lista de candidatos é descartado. A IA nunca devolve evento inventado.
5. Se não houver candidatos, o Gemini não é chamado.

### Locais próximos
- Retorna no máximo **3** lugares, de propósito, para economizar a cota gratuita do Google Places.
- O próprio local do evento é excluído do resultado.
- O resultado é guardado em cache por local enquanto a aplicação está rodando.

---

## Erros

Os erros tratados pela API seguem o formato abaixo. As exceções são o `401` de rota protegida sem token, que volta sem corpo, e o `405`, que usa o formato padrão do Spring.

```json
{
  "timestamp": "2026-09-30T22:15:39.69",
  "status": 400,
  "message": "Dados inválidos",
  "errors": [ { "field": "message", "message": "A mensagem é obrigatória" } ]
}
```

| Status | Quando |
|---|---|
| `400` | Dados inválidos ou regra violada (ex.: término antes do início) |
| `401` | Sem login, token inválido ou credenciais erradas |
| `403` | Logado, mas sem permissão (ex.: editar evento de outro usuário) |
| `404` | Recurso não encontrado |
| `405` | Método HTTP não suportado na rota |
| `409` | Conflito (ex.: e-mail já cadastrado) |
| `502` | Falha no Google Places ou no Gemini, ou chave não configurada |

---

## Integrações externas

### Google Places API (New)

Usada para:
- buscar os dados do local a partir do `placeId` (**Place Details**);
- buscar os locais próximos (**Nearby Search**).

Para configurar:
1. No [Google Cloud Console](https://console.cloud.google.com/), crie um projeto e ative a **Places API (New)**.
2. Crie uma chave de API e defina a variável `GOOGLE_PLACES_API_KEY`.
3. No frontend, use o autocomplete do Google Places, que pode usar outra chave restrita ao domínio, e mande o `placeId` escolhido.

> As chamadas usam *field mask* e pedem só os campos necessários, para reduzir o custo.

### Gemini API

Usada na Descoberta Mágica.

1. Gere uma chave no [Google AI Studio](https://aistudio.google.com/).
2. Defina a variável `GEMINI_API_KEY`.
3. O modelo pode ser trocado em `goup.gemini.model`.

---

## Próximos passos

- [ ] Configurar CORS para o frontend
- [ ] Escrever testes automatizados (services e controllers)
- [ ] Documentar a API com OpenAPI/Swagger
- [ ] Mover o segredo do JWT e as credenciais do banco para variáveis de ambiente
- [ ] Usar migrations com Flyway no lugar de `ddl-auto=update`
- [ ] Paginação e filtros (cidade, data, categoria) em `GET /events`
- [ ] Dockerfile e `docker-compose` para subir a API junto com o MySQL
