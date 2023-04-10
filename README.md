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

**To run the integration tests:**
```bash 
sbt "IntegrationTest/test"
```

**To run a specific test:**
```bash
sbt 'testOnly *JobRoutesSpec -- -z "<test name>"'
```

**To continuously compile the integration tests:**
```bash
sbt "~IntegrationTest/compile" 
```

**To run in debug mode:**

run the following command and then connect to the debugger on port 5005
```bash
sbt -jvm-debug 5005 "runMain com.github.dpratt747.jobsboard.Application"
```

---

## Endpoints:

**Health endpoint:**
```bash
curl --location 'http://localhost:8080/private/health'
```