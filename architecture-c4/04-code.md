# 04. Code View

## Vista de codigo

Este nivel muestra las clases principales y como colaboran para convertir un mensaje libre en una accion concreta del dominio.

## Diagrama de clases

```mermaid
classDiagram
    class TelegramAgentPhase2Application {
        +main(String[] args)
    }

    class BotProps {
        -String name
        -String token
    }

    class AiProps {
        -boolean enabled
        -String baseUrl
        -String apiKey
        -String model
        -int timeoutSeconds
    }

    class TelegramAgentBot {
        -BotProps botProps
        -AgentOrchestrator orchestrator
        -TelegramClient telegramClient
        +consume(Update update) void
    }

    class AgentOrchestrator {
        -LlmIntentParser llmIntentParser
        -ProjectWorkspaceService workspaceService
        +handleMessage(String messageText) String
    }

    class IntentParser {
        <<interface>>
        +parse(String messageText) ParsedIntent
    }

    class LlmIntentParser {
        -AiProps aiProps
        -ObjectMapper objectMapper
        -RuleBasedIntentParser fallbackParser
        +parse(String messageText) ParsedIntent
    }

    class RuleBasedIntentParser {
        +parse(String messageText) ParsedIntent
    }

    class ParsedIntent {
        -IntentType intent
        -String assignee
        -String status
        -String title
        -Integer storyPoints
        -String sprintName
        -boolean clarificationNeeded
        -String clarificationQuestion
    }

    class ProjectWorkspaceService {
        <<interface>>
        +findAllTasks() List~TaskItem~
        +findTasksByAssignee(String assignee) List~TaskItem~
        +findTasksByStatus(String status) List~TaskItem~
        +createTask(String title, String assignee, int storyPoints, String sprintName) TaskItem
        +getCurrentSprint() SprintInfo
        +storyPointsByAssignee() Map~String, Integer~
    }

    class InMemoryProjectWorkspaceService {
        -AtomicLong sequence
        -List~TaskItem~ tasks
        -SprintInfo currentSprint
    }

    class TaskItem
    class SprintInfo
    class IntentType {
        <<enum>>
    }

    TelegramAgentBot --> AgentOrchestrator
    TelegramAgentBot --> BotProps
    AgentOrchestrator --> LlmIntentParser
    AgentOrchestrator --> ProjectWorkspaceService
    LlmIntentParser ..|> IntentParser
    RuleBasedIntentParser ..|> IntentParser
    LlmIntentParser --> RuleBasedIntentParser
    LlmIntentParser --> AiProps
    LlmIntentParser --> ParsedIntent
    RuleBasedIntentParser --> ParsedIntent
    ParsedIntent --> IntentType
    InMemoryProjectWorkspaceService ..|> ProjectWorkspaceService
    InMemoryProjectWorkspaceService --> TaskItem
    InMemoryProjectWorkspaceService --> SprintInfo
```

## Diagrama de secuencia: consulta con LLM habilitado

```mermaid
sequenceDiagram
    actor U as Usuario
    participant T as Telegram
    participant B as TelegramAgentBot
    participant O as AgentOrchestrator
    participant P as LlmIntentParser
    participant A as API LLM
    participant W as InMemoryProjectWorkspaceService

    U->>T: que tareas tiene ana
    B->>T: long polling getUpdates
    T-->>B: Update con mensaje
    B->>O: handleMessage(texto)
    O->>P: parse(texto)
    P->>A: POST /chat/completions
    A-->>P: JSON con LIST_TASKS_BY_ASSIGNEE
    P-->>O: ParsedIntent
    O->>W: findTasksByAssignee("Ana")
    W-->>O: List<TaskItem>
    O-->>B: respuesta formateada
    B->>T: SendMessage(chatId, texto)
    T-->>U: tareas de Ana
```

## Diagrama de secuencia: fallback local

```mermaid
sequenceDiagram
    actor U as Usuario
    participant B as TelegramAgentBot
    participant O as AgentOrchestrator
    participant P as LlmIntentParser
    participant R as RuleBasedIntentParser
    participant W as InMemoryProjectWorkspaceService

    U->>B: crea una tarea para revisar login
    B->>O: handleMessage(texto)
    O->>P: parse(texto)
    P->>R: fallbackParser.parse(texto)
    R-->>P: ParsedIntent(CREATE_TASK)
    P-->>O: ParsedIntent
    O->>W: createTask(...)
    W-->>O: TaskItem
    O-->>B: confirmacion
```

## Mapeo codigo -> capas

| Capa | Clases | Comentario |
|---|---|---|
| Bootstrap | `TelegramAgentPhase2Application` | arranque Spring |
| Configuracion | `BotProps`, `AiProps` | credenciales y feature flags |
| Adaptador de mensajeria | `TelegramAgentBot` | integra Telegram |
| Aplicacion | `AgentOrchestrator` | coordina interpretacion y ejecucion |
| NLU | `IntentParser`, `LlmIntentParser`, `RuleBasedIntentParser`, `ParsedIntent`, `IntentType` | transforma lenguaje natural a intencion |
| Dominio | `TaskItem`, `SprintInfo`, `ProjectWorkspaceService` | herramientas y modelos del proyecto |
| Infraestructura de datos | `InMemoryProjectWorkspaceService` | workspace demo en memoria |

## Puntos de extension

### Nuevas intenciones

Se pueden agregar nuevas acciones incorporando:

- nuevo valor en `IntentType`
- reglas nuevas en `RuleBasedIntentParser`
- ampliacion del prompt de `LlmIntentParser`
- nuevo caso en `AgentOrchestrator`
- nuevas herramientas en `ProjectWorkspaceService`

### Sustituir workspace demo

La implementacion en memoria puede reemplazarse por:

- `JpaProjectWorkspaceService`
- `RestProjectWorkspaceService`
- `GraphQlProjectWorkspaceService`

### Robustecer el parser LLM

Se puede mejorar con:

- validacion fuerte del JSON recibido
- uso real del timeout configurado
- esquema JSON o structured outputs
- observabilidad por tipo de intencion
