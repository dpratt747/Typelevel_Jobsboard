# Typelevel Jobs Board

---
## Useful Commands:

**To run the application:**
```bash
sbt "runMain com.github.dpratt747.jobsboard.Application"
```

**To run the playground**
```bash
sbt "runMain com.github.dpratt747.playground.JobsPlayground"
```

**To run a specific test:**
```bash
sbt 'testOnly *JobRoutesSpec -- -z "<test name>"'
```
---

## Endpoints:

**Health endpoint:**
```bash
curl --location 'http://localhost:8080/private/health'
```