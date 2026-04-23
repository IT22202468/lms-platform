#!/bin/bash
set -e

# Check required env vars
for var in JWT_SECRET AUTH_MONGODB_URI COURSE_MONGODB_URI; do
  if [ -z "${!var}" ]; then
    echo "ERROR: $var is not set"
    exit 1
  fi
done

echo "==> Applying namespace and resource quota..."
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/resource-quota.yaml

echo "==> Creating secrets..."
kubectl -n lms create secret generic lms-secrets \
  --from-literal=JWT_SECRET="$JWT_SECRET" \
  --from-literal=AUTH_MONGODB_URI="$AUTH_MONGODB_URI" \
  --from-literal=COURSE_MONGODB_URI="$COURSE_MONGODB_URI" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "==> Deploying MongoDB..."
kubectl apply -f k8s/mongodb/

echo "==> Deploying auth-service..."
kubectl apply -f k8s/auth-service/
kubectl -n lms rollout status deployment/auth-service --timeout=180s

echo "==> Deploying course-service..."
kubectl apply -f k8s/course-service/
kubectl -n lms rollout status deployment/course-service --timeout=180s

echo "==> Deploying api-gateway..."
kubectl apply -f k8s/api-gateway/
kubectl -n lms rollout status deployment/api-gateway --timeout=180s

echo ""
echo "==> All services deployed!"
kubectl -n lms get pods,svc
echo ""
echo "API Gateway available at: http://localhost:30085"
