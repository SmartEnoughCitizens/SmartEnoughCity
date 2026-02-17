# Infrastructure Scripts

Helper scripts for managing the SmartEnoughCity infrastructure.

## Scripts

### cluster-up.sh
Brings up the entire infrastructure stack.

**What it does:**
1. Applies Terraform configurations (in order)
2. Waits for cluster to be ready
3. Installs all components
4. Verifies everything is running

**Usage:**
```bash
./cluster-up.sh
```

**Time:** ~10-15 minutes

---

### cluster-down.sh
Tears down the cluster to save costs.

**What it does:**
1. Deletes the GKE cluster
2. Preserves Terraform state
3. Optionally creates snapshots

**Usage:**
```bash
./cluster-down.sh
```

**Cost Savings:** 60-70% for dev environments (only run during work hours)

---

### apply-all-terraform.sh
Applies all Terraform configurations in order.

**What it does:**
1. Runs `terraform apply` in each directory
2. Waits for dependencies
3. Verifies outputs

---

### verify-infrastructure.sh
Comprehensive health check of the entire stack.

**What it checks:**
- Cluster connectivity
- All namespaces exist
- Cert-manager working
- Ingress-nginx healthy
- Istio control plane ready
- mTLS enabled
- NetworkPolicies applied
- Website accessible

## Cost Optimization

### Dev Environment
- **24/7 uptime:** $144/month (3 nodes × e2-standard-4)
- **8am-6pm weekdays:** $43/month (save $101/month!)

### How to Automate
1. Use `cluster-up.sh` and `cluster-down.sh`
2. Schedule with cron or Cloud Scheduler
3. Or run manually each day

### Example Cron Schedule
```bash
# Start cluster at 8am weekdays
0 8 * * 1-5 /path/to/cluster-up.sh

# Stop cluster at 6pm weekdays
0 18 * * 1-5 /path/to/cluster-down.sh
```
