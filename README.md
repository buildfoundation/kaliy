## Tech Spec

### Expectations v1

- Multi-layer system
- Metrics
- Logging
- Health checks:
    - Readiness check (ready to handle traffic)
    - Liveness check (alive)
- Warm up (randomized delay + backpressure)
- JSON config
- Multi-build system support: Gradle, Bazel, Buck
- Data consistency check
- Universal stream management system (memory + disk)
- 100% non-blocking IO (epoll + AIO)
- Storage systems: in-memory, disk, AWS S3 and S3 compatible systems, Redis
- Reactive internally

### Expectations v2

- User-modules (ServiceLoader)
- Configurable multi-project support: storage size, auth
- Storage systems: Apache Ignite, Apache Hazelcast
