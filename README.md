## Tech Spec

### Expectations v1

- [] Multi-layer system
- [] Metrics
- [] Logging
- [] Health checks:
    - [] Readiness check (ready to handle traffic)
    - [] Liveness check (alive)
- [] Warm up (randomized delay + backpressure)
- [x] JSON config
- [] Multi-build system support: Gradle, Bazel, Buck
- [] Data consistency check
- [] Universal stream management system (memory + disk)
- [] 100% non-blocking IO (epoll + AIO)
- [] Storage systems: in-memory, disk, AWS S3 and S3 compatible systems, Redis, Kaliy (recursive)
- [x] Reactive internally
- [] User-modules (ServiceLoader)

### Expectations v2

- Configurable multi-project support: storage size, auth
- Storage systems: Apache Ignite, Apache Hazelcast
- Data deduplication by content checksum (ie different compiler flags can result in same output)
