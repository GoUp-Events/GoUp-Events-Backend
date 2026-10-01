# O que mudou no backend do GoUp

Resumo das mudanças da branch `feat/places-favoritos-descoberta`, para facilitar a revisão do PR.

A ideia foi alinhar o backend às regras de negócio do GoUp: **descoberta e divulgação de eventos em Blumenau e região**. O GoUp não vende ingressos.

---

## Resumo rápido

| Área | Antes | Agora |
|---|---|---|
| **Local** | Endereço digitado manualmente, com CRUD completo | Vem do **Google Places** pelo `placeId`, é reaproveitado entre eventos e não tem edição manual |
| **Categoria** | CRUD aberto (qualquer pessoa criava/apagava) | **Lista fixa** criada por seed; só leitura |
| **Favoritos** | Não existia | Favoritar/desfavoritar e "meus favoritos" (o roteiro simplificado) |
| **Descoberta Mágica** | Não existia | `POST /discovery`: o Gemini escolhe entre eventos reais |
| **Locais próximos** | Não existia | `GET /events/{id}/nearby`: os 3 lugares mais próximos do evento |
| **Rascunhos** | Apareciam para todo mundo | Só o dono ou um ADMIN vê |
| **Segurança** | Categorias e locais aceitavam escrita sem login | Só o GET é público; sem login → 401 |

---

## 1. Local (Location) via Google Places

**Fluxo:**
1. O frontend usa o autocomplete do Google e manda **só o `placeId`**.
2. Se já existe um Location com esse `placeId`, ele é **reaproveitado** sem chamar o Google.
3. Se não existe, o backend busca o **Place Details** e salva o local.

**Campos novos no Location:** `placeId` (único), `formattedAddress`, `latitude`, `longitude`, `rating`, `priceLevel`.
Rua, número, bairro, cidade, estado e CEP passam a ser preenchidos a partir dos dados do Google.

**Endpoints:**
- `GET /locations` e `GET /locations/{id}`: públicos.
- `POST /locations` com `{ "placeId": "..." }`: exige login e devolve o local salvo (novo ou reaproveitado).
- `PUT` e `DELETE` foram **removidos**, porque não há edição manual.

> ⚠️ **Mudança que afeta o front:** o `POST/PUT /events` agora recebe **`placeId`** no lugar de `locationId`.

## 2. Categorias fixas

- `POST`, `PUT` e `DELETE /categories` foram **removidos**.
- As categorias são criadas automaticamente ao iniciar a aplicação (`config/CategorySeeder.java`). O seed só insere as que ainda não existem, então não duplica.
- São 12 categorias iniciais: Shows e Música, Festas e Baladas, Teatro e Dança, Gastronomia, Cerveja, Cultura e Arte, Esportes, Ar Livre e Natureza, Infantil e Família, Feiras e Exposições, Cursos e Workshops, Festas Típicas. A lista é uma sugestão; para mudar, basta editar o seed.

## 3. Favoritos

- `POST /events/{id}/favorite`: favorita. Repetir não dá erro nem duplica.
- `DELETE /events/{id}/favorite`: desfavorita.
- `GET /users/me/favorites`: lista os favoritos na ordem em que foram adicionados (o "roteiro" simplificado).
- O par (usuário, evento) é **único** no banco.
- Excluir um evento também remove os favoritos dele.
- Todos exigem login.

## 4. Descoberta Mágica (IA com Gemini)

Não é uma conversa livre. O fluxo é:

1. O usuário manda `{ "message": "algo pra fazer sábado com cerveja", "city": "Blumenau" }` (`city` é opcional).
2. O backend separa os **candidatos reais**: eventos `PUBLISHED`, com data de hoje em diante, filtrados pela cidade se ela vier, até **20**.
3. Só esses candidatos são enviados ao **Gemini**, que responde um JSON `{ eventIds, summary }`.
4. O backend **descarta qualquer id que não esteja na lista de candidatos**, então a IA não consegue inventar eventos.
5. A resposta tem o resumo e os eventos reais (até 5).

Detalhes:
- Se não houver candidatos, o Gemini nem é chamado e a resposta já diz que não há eventos.
- Exige login, para não gastar a cota do Gemini com visitantes. Liberar é uma linha no `SecurityConfig`.

## 5. Locais próximos

- `GET /events/{id}/nearby` é público e devolve os **3 lugares mais próximos** do local do evento (Google Places Nearby Search).
- O próprio local do evento é ignorado, e cada lugar vem com a **distância em metros**.
- O resultado fica **em cache por local** enquanto a aplicação está rodando, para economizar a cota do Google.
- Os tipos de lugar buscados são configuráveis (padrão: restaurante, café, bar, padaria, atração turística e parque).

## 6. Eventos

- **Rascunhos (`DRAFT`)** não aparecem mais em `GET /events`. Para quem não é dono nem ADMIN, `GET /events/{id}` de um rascunho responde 404.
- Novo `GET /users/me/events`: o usuário vê os próprios eventos, inclusive os rascunhos.
- As consultas de eventos carregam usuário, local e categoria em uma só query. Antes havia o problema de N+1 queries.

## 7. Segurança e erros

| Situação | Status |
|---|---|
| Sem login em rota protegida | **401** (antes era 403) |
| Logado, mas sem permissão (ex.: editar evento de outro usuário) | **403** |
| Recurso inexistente | **404** |
| Falha no Google Places/Gemini ou chave não configurada | **502** |

- Rotas públicas: `/auth/**` e os GET de `/events/**`, `/categories/**` e `/locations/**`.
- Todo o resto exige login.

---

## Arquivos novos

```
client/GooglePlacesClient.java       chamadas ao Google Places (Place Details e Nearby Search)
client/GeminiClient.java             chamada ao Gemini com resposta em JSON
config/CategorySeeder.java           seed das categorias
config/CacheConfig.java              cache dos locais próximos
controller/FavoriteController.java
controller/DiscoveryController.java
service/FavoriteService.java
service/DiscoveryService.java
entity/Favorite.java
entity/enums/PriceLevel.java
repository/FavoriteRepository.java
mapper/EventMapper.java, LocationMapper.java
dto/favorite, dto/discovery, dto/place
exception/ExternalServiceException.java
```

---

## Para testar localmente

1. **Recriem o banco**, porque a tabela `locations` mudou bastante:
   ```sql
   DROP DATABASE goup_events;
   ```
2. Definam as chaves de API como variáveis de ambiente:
   ```bash
   export GOOGLE_PLACES_API_KEY=...   # com a "Places API (New)" ativada no Google Cloud
   export GEMINI_API_KEY=...          # gerada no Google AI Studio
   ```
3. Subam a aplicação com `./mvnw spring-boot:run`.

O passo a passo completo e a lista de endpoints estão no [README](README.md).

## O que foi testado

Testamos com a API rodando contra um MySQL real:
- Permissões: 401, 403 e 404.
- Rascunhos escondidos do público.
- Favoritos sem duplicar, e removidos junto com o evento.
- Seed de categorias sem duplicar ao reiniciar.
- Validações.
- Erro 502 quando falta a chave do Google ou do Gemini.

**Ainda não testado:** as chamadas reais ao Google Places e ao Gemini, porque precisam das chaves. Vale fazer um teste com as chaves antes do merge.

## Próximos passos sugeridos

- Configurar **CORS** para o frontend conseguir consumir a API.
- Escrever **testes automatizados**.
- Tirar o segredo do JWT e a senha do banco do `application.properties` e passar para variáveis de ambiente.
- Revisar `goup.jwt.expiration-ms=900000080` (≈ 10 dias; talvez a intenção fosse `900000`, 15 min).
- Usar migrations com Flyway no lugar de `ddl-auto=update`.
