# Kubernetes Implementation Guide

This guide explains how to migrate the LMS platform from Docker Compose to Kubernetes (k3s on a VM), what changes are needed on the VM, and how each piece maps to your existing Docker Compose setup.

---

## Overview: What Changes

| Docker Compose Concept | Kubernetes Equivalent |
|---|---|
| `docker-compose.prod.yml` | `k8s/*/deployment.yaml` |
| Container name as DNS (e.g. `auth-service`) | `Service` with `ClusterIP` |
| `.env` file | `Secret` |
| Named volume (`course-uploads`) | `PersistentVolumeClaim` |
| Port mapping (`8085:8085`) | `Service` with `NodePort` or `LoadBalancer` |
| `deploy.resources.limits` | `resources.limits` in Deployment |
| Nginx reverse proxy on VM | `Ingress` (or keep Nginx, point to NodePort) |
| `docker compose up` | `kubectl apply -f k8s/` |

---

## Step 1: VM Preparation

Your current VM has 1GB RAM, which is too small for k3s. For learning purposes, you have two options:

### Option A: Upgrade your existing VM
- **Minimum**: 2GB RAM (tight but functional for learning)
- **Recommended**: 4GB RAM

Most cloud providers let you resize a VM without destroying it (DigitalOcean: Resize Droplet, Hetzner: Upgrade plan).

### Option B: Use a free Kubernetes playground (no VM changes needed)
For pure learning without deployment:
- [killercoda.com](https://killercoda.com) — free browser-based K8s environment
- [Play with Kubernetes](https://labs.play-with-k8s.com) — free 4-hour sessions

---

## Step 2: Install k3s on the VM

SSH into your VM and run:

```bash
# Install k3s (single-node Kubernetes)
curl -sfL https://get.k3s.io | sh -

# Verify it's running
sudo k3s kubectl get nodes

# Set up kubectl for your user (so you don't need sudo)
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER ~/.kube/config

# Verify
kubectl get nodes
# Expected output:
# NAME       STATUS   ROLES                  AGE   VERSION
# your-vm    Ready    control-plane,master   1m    v1.x.x
```

k3s includes:
- Kubernetes control plane
- `kubectl` CLI
- Traefik ingress controller (built-in)
- Local-path storage provisioner (for PVCs)
- Flannel networking (for pod-to-pod communication)

---

## Step 3: Install Metrics Server (Required for HPA)

HPA cannot function without Metrics Server. Install it:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# On k3s you may need this patch for TLS:
kubectl patch deployment metrics-server \
  -n kube-system \
  --type='json' \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'

# Verify (wait ~60 seconds)
kubectl top nodes
```

---

## Step 4: Create the Namespace

All LMS resources are grouped under the `lms` namespace:

```bash
kubectl create namespace lms

# Verify
kubectl get namespaces
```

---

## Step 5: Create the GHCR Image Pull Secret

Your images are on a private GitHub Container Registry. Kubernetes needs credentials to pull them:

```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=<your-github-username> \
  --docker-password=<your-github-personal-access-token> \
  --namespace=lms
```

To create a GitHub PAT: GitHub → Settings → Developer settings → Personal access tokens → New token → select `read:packages`.

---

## Step 6: Create Secrets

Replace the `.env` file with a Kubernetes Secret. Encode your values in base64:

```bash
# Encode your values
echo -n "your-jwt-secret" | base64
echo -n "mongodb+srv://user:pass@cluster.mongodb.net" | base64
echo -n "your-grafana-password" | base64
```

Paste the encoded values into `k8s/secrets.yaml`, then apply:

```bash
kubectl apply -f k8s/secrets.yaml
```

---

## Step 7: Apply All Manifests

```bash
# Apply in order (secrets first, then services, then deployments)
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/course-service/pvc.yaml
kubectl apply -f k8s/auth-service/
kubectl apply -f k8s/course-service/
kubectl apply -f k8s/api-gateway/
kubectl apply -f k8s/monitoring/

# Or apply everything at once (order is handled automatically)
kubectl apply -R -f k8s/
```

---

## Step 8: Verify Everything is Running

```bash
# Watch pods come up (Spring Boot takes ~45s to start)
kubectl get pods -n lms -w

# Expected output after ~2 minutes:
# NAME                              READY   STATUS    RESTARTS   AGE
# auth-service-6d8b9f7c4-xk2lp      1/1     Running   0          2m
# course-service-7f9b8c6d5-mn3qr    1/1     Running   0          2m
# api-gateway-5c7d9e8f6-pq4st       1/1     Running   0          2m
# prometheus-4b6c8d7e5-rs5uv        1/1     Running   0          2m
# grafana-3a5b7c6d4-wx6yz           1/1     Running   0          2m

# Check services
kubectl get services -n lms

# Check HPA (shows current replicas vs desired)
kubectl get hpa -n lms
```

---

## Step 9: Update Nginx on the VM

Your existing Nginx config points to Docker ports (8085, 9090, etc.).
With k3s and NodePort services, the ports shift to the 30000+ range.

Update `/etc/nginx/sites-available/lms` (or wherever your config lives):

```nginx
# Before (Docker Compose):
location /api/ {
    proxy_pass http://localhost:8085;
}

# After (k3s NodePort):
location /api/ {
    proxy_pass http://localhost:30085;   # NodePort defined in api-gateway/service.yaml
}

# Grafana:
# Before: proxy_pass http://localhost:3001;
# After:  proxy_pass http://localhost:30301;
```

Reload Nginx:
```bash
sudo nginx -t && sudo systemctl reload nginx
```

---

## Step 10: Watch Autoscaling in Action

```bash
# In one terminal — watch HPA
kubectl get hpa -n lms -w

# In another terminal — watch pods
kubectl get pods -n lms -w

# To manually trigger scale-up (for testing):
kubectl run -i --tty load-test --rm --image=busybox --restart=Never -- \
  /bin/sh -c "while true; do wget -q -O- http://api-gateway.lms.svc.cluster.local:8085/health; done"
```

---

## Key kubectl Commands Reference

```bash
# Get all resources in the lms namespace
kubectl get all -n lms

# View logs for a service
kubectl logs -n lms deployment/auth-service
kubectl logs -n lms deployment/auth-service -f   # Follow (like docker logs -f)

# Describe a pod (shows events, errors)
kubectl describe pod -n lms <pod-name>

# Restart a deployment (useful after pushing a new image)
kubectl rollout restart deployment/auth-service -n lms

# Scale manually (override HPA temporarily)
kubectl scale deployment/auth-service --replicas=2 -n lms

# Get a shell inside a running pod
kubectl exec -it -n lms deployment/auth-service -- /bin/sh

# Delete everything and start fresh
kubectl delete namespace lms   # WARNING: deletes all resources including PVCs
```

---

## Migrating from Docker Compose: What to Stop

Once K8s is running, stop the Docker Compose stack so both don't fight over ports:

```bash
# On your VM
cd /path/to/lms-platform
docker compose -f docker-compose.prod.yml down

# Optional: stop Docker from auto-starting on boot
sudo systemctl disable docker
```

---

## Architecture After Migration

```
Internet
    │
    ▼
Nginx (VM, port 443/80)
    │
    ▼
NodePort 30085
    │
    ▼
┌─────────────────────────── lms namespace ────────────────────────────┐
│                                                                        │
│  api-gateway (1-3 pods) ──► auth-service (1-3 pods)                  │
│         └───────────────────► course-service (1-5 pods)              │
│                                                                        │
│  prometheus (1 pod) ◄── scrapes all services                         │
│  grafana (1 pod) ◄── reads from prometheus                           │
│                                                                        │
│  Persistent Storage:                                                   │
│    course-uploads-pvc ──► /data/uploads in course-service            │
│    grafana-data-pvc   ──► /var/lib/grafana in grafana                │
│                                                                        │
│  Secrets: lms-secrets (JWT_SECRET, MONGODB_URI, GF_ADMIN_PASSWORD)   │
│                                                                        │
│  MongoDB Atlas (external, unchanged)                                   │
│  Vercel frontend (external, unchanged)                                 │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Common Issues and Fixes

| Problem | Cause | Fix |
|---|---|---|
| Pod stuck in `ImagePullBackOff` | GHCR credentials wrong | Re-create `ghcr-secret` with valid PAT |
| Pod stuck in `CrashLoopBackOff` | App failing to start | `kubectl logs -n lms <pod>` to see error |
| HPA shows `<unknown>/70%` for CPU | Metrics Server not running | See Step 3 |
| PVC stuck in `Pending` | No StorageClass available | k3s: ensure `local-path` StorageClass exists: `kubectl get storageclass` |
| Services can't reach each other | Wrong namespace or service name | Services must be in same namespace, names must match exactly |
| Spring Boot pods slow to start | JVM startup time | Increase `initialDelaySeconds` in liveness/readiness probes |
