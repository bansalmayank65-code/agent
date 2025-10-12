# 🚀 Render Deployment Guide - Docker Approach

This guide shows how to deploy your Amazon Agentic Workstation on Render using Docker for better reliability and performance.

## 🐳 **Why Docker on Render?**

✅ **Better Java Support** - Java isn't first-class on Render, Docker gives full control  
✅ **Consistent Builds** - Same environment locally and in production  
✅ **Optimized Images** - Multi-stage builds for smaller, faster deployments  
✅ **Better Scaling** - Container-native scaling and health checks  

## 📋 **Deployment Options**

### **Option 1: Docker Deployment (Recommended)**
- **Pros:** Most reliable, consistent, production-ready
- **Cons:** Slightly longer build times
- **Best for:** Production deployments

### **Option 2: Native Build**  
- **Pros:** Faster builds, simpler setup
- **Cons:** Less reliable on Render's Java environment
- **Best for:** Quick testing

---

## 🐳 **Docker Deployment Steps**

### **Step 1: Render Service Configuration**

**Service Type:** Web Service  
**Environment:** Docker  

**Repository:** `https://github.com/bansalmayank65-code/agent`  
**Branch:** `main`  
**Dockerfile Path:** `./Dockerfile`  

### **Step 2: Environment Variables**

```bash
# Essential Configuration
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=$DATABASE_URL

# Container Optimization
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC

# Optional
JPA_SHOW_SQL=false
```

### **Step 3: Database Setup**

1. **Add PostgreSQL Database** in Render dashboard
2. **Database Name:** `agentic-workstation-db`
3. **Plan:** Free or Starter
4. **Link to Service:** The `DATABASE_URL` will be auto-provided

### **Step 4: Deploy!**

1. **Create Web Service** in Render
2. **Connect GitHub Repository**
3. **Select Docker environment**
4. **Add environment variables**
5. **Deploy!**

---

## 🔧 **Docker Configuration Details**

### **Multi-Stage Build Process:**

1. **Flutter Stage** - Builds web app using official Flutter container
2. **Java Stage** - Compiles Spring Boot with Maven
3. **Production Stage** - Minimal Alpine runtime with security hardening

### **Key Features:**

- **Security:** Non-root user, minimal attack surface
- **Performance:** G1GC, container memory awareness
- **Monitoring:** Built-in health checks
- **Optimization:** Layer caching, dependency pre-fetch

---

## 🏠 **Local Development**

### **Using Docker Compose:**
```bash
# Start everything (app + database)
docker-compose up

# Background mode
docker-compose up -d

# View logs
docker-compose logs -f

# Stop everything
docker-compose down
```

### **Access Points:**
- **Frontend:** http://localhost:8080/
- **API:** http://localhost:8080/api/
- **Health:** http://localhost:8080/actuator/health
- **Database:** localhost:5432

### **Manual Docker Build:**
```bash
# Build image
docker build -t agentic-workstation .

# Run container
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev agentic-workstation

# With database connection
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host:5432/db \
  agentic-workstation
```

---

## 📊 **Render Service URLs**

After deployment, your app will be available at:

- **Main App:** `https://agentic-workstation.onrender.com/`
- **API Endpoints:** `https://agentic-workstation.onrender.com/api/`
- **Health Check:** `https://agentic-workstation.onrender.com/actuator/health`

---

## 🔍 **Troubleshooting**

### **Build Issues:**
```bash
# Check build logs in Render dashboard
# Common issues:
# - Out of memory: Increase JAVA_OPTS heap size
# - Flutter build fails: Check Flutter version compatibility
# - Maven dependencies: Clear cache and rebuild
```

### **Runtime Issues:**
```bash
# Check application logs
# Common issues:
# - Database connection: Verify DATABASE_URL
# - Port binding: Ensure PORT env var is set
# - Memory limits: Adjust MaxRAMPercentage
```

### **Performance Optimization:**
```bash
# JVM tuning for containers
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Enable JVM metrics
MANAGEMENT_METRICS_EXPORT_JMX_ENABLED=true
```

---

## 🚀 **Deployment Checklist**

- [ ] **Repository connected** to Render
- [ ] **Dockerfile** exists in root directory  
- [ ] **PostgreSQL database** created and linked
- [ ] **Environment variables** configured
- [ ] **Health check** endpoint working (`/actuator/health`)
- [ ] **Frontend** accessible at root path (`/`)
- [ ] **API endpoints** working (`/api/`)

---

## 💡 **Pro Tips**

1. **Monitor Resources:** Use Render metrics to track memory/CPU usage
2. **Database Connections:** Set appropriate connection pool sizes
3. **Caching:** Enable HTTP caching for static assets
4. **Scaling:** Consider upgrading to paid plans for auto-scaling
5. **Security:** Regularly update base images and dependencies

---

Your Amazon Agentic Workstation is now ready for production deployment on Render! 🎉