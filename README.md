# AI Hackathon - eCommerce MVP

This code was generated as part of **2 days** bootcamp **Distributed System with AI Hackathon**
conducted by **Shah Ali Newaj Topu**. I attended the first offering of this course on 17, 24 January 2026.

This repository contains code from second day, where we created an eCommerce application following clean architecture
using AntiGravity.

**Note:** Although prompts are tailored toward asynchronous programming, but this implementation doesn't utilize Javalin
7.2's asynchronous web server features.

## Running locally

The system is 17 JVM processes (4 per subsystem × 4 subsystems + 1 saga) plus RabbitMQ and MongoDB. They're orchestrated
with [Overmind](https://github.com/DarthSim/overmind) reading the [Procfile](./Procfile).

### Bring everything up

```sh
docker compose up -d         # rabbitmq + mongodb
./gradlew clean build        # build the project
overmind start               # all 17 JVM processes, color-coded aggregated logs
```

### Bring it down

`Ctrl-C` in the Overmind terminal stops all JVM processes. Then:

```sh
docker compose down
```

### Port map

| Subsystem       | Command API | Query API |
|-----------------|-------------|-----------|
| product-catalog | 8080        | 8081      |
| inventory       | 8082        | 8083      |
| cart            | 8084        | 8085      |
| ordering        | 8086        | 8087      |

CommandHandler, EventHandler, and SagaProcess have no HTTP port — they're message-bus consumers.

### Useful Overmind commands

```sh
overmind connect cart-command-api   # attach to a single process's tmux pane
overmind restart inventory-event-handler
overmind echo                       # show stdout of all processes
```

### Tests

```sh
./gradlew test           # all modules
./gradlew :cart:test     # one module
```
