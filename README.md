# Aspectow Demo Console: Dedicated Management & Monitoring Console for Aspectow Demo

Aspectow Demo Console is a dedicated management and application monitoring (AppMon) console server designed specifically to manage and monitor the [Aspectow Demo Site](https://github.com/aspectran/aspectow-demo) environment. Built on the [Aspectran](https://aspectran.com/) framework and an embedded Undertow web server, it serves as the centralized administrative hub for Aspectow WAS cluster nodes.

## System Architecture

The Console operates in coordination with `aspectow-demo` instances via Redis, which functions as the shared message broker and cluster registry.

```text
┌─────────────────────────────────┐           ┌─────────────────────────────────┐
│     Aspectow Demo Console       │           │       Aspectow Demo Site        │
│    (aspectow-demo-console)      │           │        (aspectow-demo)          │
│                                 │           │                                 │
│  - Remote Node Manager          │           │  - JPetStore, PetClinic, etc.   │
│  - Remote Scheduler Manager     │           │  - AppMon Engine Exporters      │
│  - Remote Command Dispatcher    │           │  - Embedded Undertow WAS        │
│  - AppMon Web Console UI (:8082)│           │                                 │
└────────────────┬────────────────┘           └────────────────┬────────────────┘
                 │                                             │
                 └───────────────► ┌─────────┐ ◄───────────────┘
                                   │  Redis  │ (Node Registry & Pub/Sub Bus)
                                   └─────────┘
```

## Key Capabilities

* **Cluster & Node Management**
  * Real-time monitoring and coordination of distributed cluster nodes via Redis.
  * Heartbeat pulsing, gateway/direct routing modes, and cluster node discovery.

* **Remote Scheduler Management**
  * Centralized monitoring and control of Quartz and Aspectran schedulers across cluster nodes.
  * Inspect running jobs, trigger ad-hoc job executions, and pause/resume schedules remotely.

* **Remote Command Execution**
  * Dispatching and tracking remote commands across cluster nodes.
  * Supports asynchronous file-based polling and command queues (`incoming`, `queued`, `completed`, `failed`).

* **Application Monitoring (AppMon)**
  * Real-time JVM heap memory and Undertow worker thread pool metrics.
  * Live HTTP request activity and active session tracking.
  * Real-time log file tailing and access log analytics with WHOIS IP geolocation lookup.

* **Multi-Database Persistence Support**
  * Flexible MyBatis-based persistence layer supporting H2, MariaDB, MySQL, PostgreSQL, Oracle, and Supabase for historical monitoring data storage.

## Project Structure

```text
aspectow-demo-console/
├── app/
│   ├── bin/             Startup scripts for Shell, Daemon, and Windows Service (Procrun)
│   ├── cmd/             Asynchronous command directories (incoming, queued, sample)
│   ├── config/          Application rules, server, logging, and console configurations
│   ├── lib/             Runtime dependencies and application JARs (generated on build)
│   └── logs/            Application, server, and access log files
├── setup/               Server deployment scripts and service registration tools
├── src/                 Java source code and unit tests
└── pom.xml              Maven build configuration
```

## Configuration & Profiles

Configuration files are located under `app/config/`:

* `aspectran-config.apon`: Core Aspectran configuration, shell commands, daemon settings
* `console/node-config.apon`: Cluster and node configuration
* `console/appmon-config.apon`: AppMon monitoring targets, metrics, and log settings
* `console/redis-dev.properties` / `redis-prod.properties`: Redis connection settings
* `server/undertow/tow-server.xml`: Undertow HTTP listener port and worker thread settings

### Active Profiles

* `dev` (default): Configured for local development.
* `prod`: Configured for production environment.
* `appmon.ext-persistence`: Enables external database persistence for AppMon.

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE.txt](LICENSE.txt) file for details.
