# DispatcherService

Microsservico responsavel por consumir eventos de alerta via RabbitMQ, enviar notificacoes por e-mail (SMTP) e atualizar o status do alerta e das regras associadas no banco de dados MySQL.

## Tecnologias

- Java 25
- Spring Boot 3.5
- Spring AMQP (RabbitMQ)
- Spring Mail (SMTP)
- Spring Data JPA / Hibernate
- MySQL 8.4
- Lombok
- jqwik (property-based testing)
- Docker Compose

## Arquitetura

O projeto segue Clean Architecture com as seguintes camadas:

```
domain          - Entidades (Alert, Rule), eventos (AlertTriggeredEvent), ports (interfaces)
application     - Use case (ProcessAlertNotificationUseCase), construtor de conteudo de e-mail (EmailContentBuilder)
adapters        - Listener RabbitMQ (AlertNotificationListener), gateway SMTP (SmtpEmailGateway)
infrastructure  - Configuracoes Spring (RabbitMQ, Retry), adaptadores JPA
```

### Fluxo principal

```
RabbitMQ (invest.alerts.notification.queue)
    -> AlertNotificationListener
        -> ProcessAlertNotificationUseCaseImpl
            -> EmailContentBuilder (monta subject e HTML do e-mail)
            -> SmtpEmailGateway (envia via SMTP)
            -> AlertRepository (marca alerta como SENT)
            -> RuleRepository (desativa regras associadas)
```

### Dead Letter Queue

Mensagens que falham apos as tentativas de retry sao encaminhadas para a DLQ:

- Exchange principal: `invest.alerts.exchange` (Direct)
- Routing key: `alert.triggered`
- Fila principal: `invest.alerts.notification.queue`
- DLX: `invest.alerts.dlx.exchange` (Fanout)
- DLQ: `invest.alerts.notification.dlq`

### Canais de notificacao

O servico suporta o campo `notificationChannel` no evento. Atualmente apenas `EMAIL` e processado; outros canais sao descartados com nack sem requeue.

## Pre-requisitos

- Java 25+
- Docker e Docker Compose
- Maven 3.9+ (ou use o wrapper `./mvnw`)
- Conta SMTP valida para envio de e-mails

## Como rodar (standalone)

Este servico pode ser executado de forma independente, sem depender dos demais servicos do projeto.

### 1. Subir as dependencias

```bash
docker compose up -d
```

Isso inicia:
- **MySQL 8.4** na porta `3307`, com schema e dados de seed carregados automaticamente via `docker/mysql/init/`
- **RabbitMQ** nas portas `5672` (AMQP) e `15672` (Management UI: http://localhost:15672)

### 2. Configurar variaveis SMTP

Crie um arquivo `.env` na raiz do servico ou exporte as variaveis antes de iniciar:

```bash
export SMTP_HOST=smtp.gmail.com
export SMTP_PORT=587
export SMTP_USERNAME=seu-email@gmail.com
export SMTP_PASSWORD=sua-senha-de-app
```

### 3. Iniciar a aplicacao

```bash
./mvnw spring-boot:run
```

A aplicacao conecta ao MySQL em `localhost:3307` e ao RabbitMQ em `localhost:5672` por padrao.

## Banco de dados

Os scripts de inicializacao em `docker/mysql/init/` sao executados automaticamente na primeira vez que o container MySQL e criado:

| Arquivo                    | Descricao                               |
|----------------------------|-----------------------------------------|
| `00-schema.sql`            | DDL completo (tabelas, indices, FKs)    |
| `01-seed-demo-user.sql`    | Usuario demo para desenvolvimento local |
| `02-seed-assets.sql`       | Ativos FII para testes                  |

> Para recriar o banco do zero, remova o volume: `docker compose down -v && docker compose up -d`

## Variaveis de ambiente

| Variavel                              | Padrao                  | Descricao                              |
|---------------------------------------|-------------------------|----------------------------------------|
| `SPRING_RABBITMQ_HOST`                | `localhost`             | Host do RabbitMQ                       |
| `SPRING_RABBITMQ_PORT`                | `5672`                  | Porta AMQP do RabbitMQ                 |
| `SPRING_RABBITMQ_USERNAME`            | `guest`                 | Usuario do RabbitMQ                    |
| `SPRING_RABBITMQ_PASSWORD`            | `guest`                 | Senha do RabbitMQ                      |
| `RABBITMQ_LISTENER_PREFETCH`          | `10`                    | Prefetch count do listener             |
| `RABBITMQ_LISTENER_CONCURRENCY`       | `1`                     | Concorrencia minima do listener        |
| `RABBITMQ_LISTENER_MAX_CONCURRENCY`   | `3`                     | Concorrencia maxima do listener        |
| `MYSQL_HOST`                          | `localhost`             | Host do MySQL                          |
| `MYSQL_PORT`                          | `3307`                  | Porta do MySQL                         |
| `MYSQL_DATABASE`                      | `investalert`           | Nome do banco de dados                 |
| `MYSQL_USERNAME`                      | `root`                  | Usuario do banco                       |
| `MYSQL_PASSWORD`                      | `root`                  | Senha do banco                         |
| `SMTP_HOST`                           | `smtp.example.com`      | Host do servidor SMTP                  |
| `SMTP_PORT`                           | `587`                   | Porta SMTP (STARTTLS)                  |
| `SMTP_USERNAME`                       | `user@example.com`      | Usuario de autenticacao SMTP           |
| `SMTP_PASSWORD`                       | `changeme`              | Senha de autenticacao SMTP             |
| `TZ`                                  | `America/Sao_Paulo`     | Timezone da aplicacao                  |

## Formato do evento

O servico espera mensagens no seguinte formato JSON na fila `invest.alerts.notification.queue`:

```json
{
  "eventType": "ALERT_TRIGGERED",
  "correlationId": "uuid-v4",
  "timestamp": "2025-01-01T10:00:00",
  "notificationChannel": "EMAIL",
  "data": {
    "alertId": 1,
    "userId": 1,
    "email": "usuario@exemplo.com",
    "assetName": "Maxi Renda",
    "ticker": "MXRF11",
    "currentPrice": 9.50,
    "dividendYield": 12.50,
    "pVp": 1.05,
    "groupName": "Meu Grupo",
    "conditions": [
      {
        "field": "DIVIDEND_YIELD",
        "operator": "GREATER_THAN",
        "targetValue": 12.00
      }
    ],
    "evaluatedAt": "2025-01-01T10:00:00"
  }
}
```

Mensagens com `notificationChannel` diferente de `EMAIL` sao descartadas (nack sem requeue).

## Testes

```bash
./mvnw test
```

A suite inclui:

- Testes unitarios para logica de dominio e use cases
- Property-based tests com jqwik
- H2 em memoria para testes de unidade que envolvem persistencia

## Build da imagem Docker

```bash
docker build -t dispatcher-service .
```

O `Dockerfile` usa multi-stage build: compila com `eclipse-temurin:25-jdk` e gera a imagem final com `eclipse-temurin:25-jre`.
