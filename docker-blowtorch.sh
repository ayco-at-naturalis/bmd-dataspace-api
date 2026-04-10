#!/bin/bash
set -euo pipefail

# docker-blowtorch.sh
# Destroys Docker artifacts: containers, images, volumes, networks, build cache.
#
# WARNING:
# - This will delete data. In particular, removing volumes will permanently delete
#   persistent state (databases, logs, etc.).
# - "scorch" and "inferno" modes will also wipe Docker's state directories, effectively
#   a full reset of Docker on the host.
# NOTE:
# - Data volumes nont managed by Docker will not be touched.

PROG="$(basename "$0")"

log()  { printf '%s\n' "$*"; }
warn() { printf 'WARNING: %s\n' "$*" >&2; }
die()  { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

usage() {
  cat <<EOF
Usage:
  $PROG [options] [mode]

Modes (default: all):
  all         Remove containers, images, volumes, networks, build cache (via docker commands)
  soft        Like all, but preserves volumes
  scorch      all + stop docker + wipe /var/lib/docker (full reset; requires root)
  inferno     scorch + also wipe common containerd state dirs (very aggressive; requires root)

Options:
  -n, --dry-run       Print actions without executing
  -y, --yes           Do not prompt for confirmation
  -q, --quiet         Less output
  -h, --help          Shows this help text
  --keep-volumes      Preserve volumes (same as mode soft)
  --keep-images       Preserve images
  --keep-networks     Preserve user-defined networks
  --keep-containers   Preserve containers (rarely useful)
  --keep-build-cache  Preserve build cache
  --prune-only        Use prune commands only (no brute-force rm loops)

Examples:
  $PROG                 # interactive "all"
  $PROG --yes soft      # non-interactive; delete everything except volumes
  $PROG -n all          # show what would be done
  sudo $PROG scorch     # full docker reset (wipes /var/lib/docker)
EOF
}

# Defaults
MODE="all"
DRY_RUN=0
ASSUME_YES=0
QUIET=0
PRUNE_ONLY=0

KEEP_VOLUMES=0
KEEP_IMAGES=0
KEEP_NETWORKS=0
KEEP_CONTAINERS=0
KEEP_BUILD_CACHE=0

run() {
  if [[ $DRY_RUN -eq 1 ]]; then
    log "[dry-run] $*"
  else
    [[ $QUIET -eq 0 ]] && log "+ $*"
    # shellcheck disable=SC2086
    eval "$@"
  fi
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

is_root() {
  [[ ${EUID:-$(id -u)} -eq 0 ]]
}

confirm_or_die() {
  [[ $ASSUME_YES -eq 1 ]] && return 0

  cat >&2 <<EOF

This operation is DESTRUCTIVE.

Mode: $MODE
Will attempt to remove:
  - containers:  $((1-KEEP_CONTAINERS))
  - images:      $((1-KEEP_IMAGES))
  - volumes:     $((1-KEEP_VOLUMES))
  - networks:    $((1-KEEP_NETWORKS))
  - build cache: $((1-KEEP_BUILD_CACHE))

Type EXACTLY: blowtorch
to proceed:
EOF
  read -r reply
  [[ "$reply" == "blowtorch" ]] || die "Aborted by user."
}

docker_access_check() {
  # We avoid "docker info" in case daemon is down; still try, but message nicely.
  if ! docker info >/dev/null 2>&1; then
    warn "Docker daemon not reachable via docker CLI."
    warn "If you're in scorch/inferno mode, this may be expected after stopping docker."
    warn "Otherwise: check Docker service, or your permissions to /var/run/docker.sock."
  fi
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -n|--dry-run) DRY_RUN=1; shift ;;
      -y|--yes) ASSUME_YES=1; shift ;;
      -q|--quiet) QUIET=1; shift ;;
      --keep-volumes) KEEP_VOLUMES=1; shift ;;
      --keep-images) KEEP_IMAGES=1; shift ;;
      --keep-networks) KEEP_NETWORKS=1; shift ;;
      --keep-containers) KEEP_CONTAINERS=1; shift ;;
      --keep-build-cache) KEEP_BUILD_CACHE=1; shift ;;
      --prune-only) PRUNE_ONLY=1; shift ;;
      -h|--help) usage; exit 0 ;;
      all|soft|scorch|inferno) MODE="$1"; shift ;;
      *) die "Unknown argument: $1 (use --help)" ;;
    esac
  done

  if [[ "$MODE" == "soft" ]]; then
    KEEP_VOLUMES=1
  fi
}

# Cleanup using only the Docker API
polite_phase() {
  docker_access_check

  if [[ $KEEP_CONTAINERS -eq 0 ]]; then
    run "docker container prune -f"
  fi

  if [[ $KEEP_IMAGES -eq 0 ]]; then
    run "docker image prune -a -f"
  fi

  if [[ $KEEP_NETWORKS -eq 0 ]]; then
    run "docker network prune -f"
  fi

  if [[ $KEEP_BUILD_CACHE -eq 0 ]]; then
    # Modern docker uses buildx; `docker builder prune` is still valid.
    run "docker builder prune -a -f"
    # Also attempt buildx prune (if buildx is present)
    if docker buildx version >/dev/null 2>&1; then
      run "docker buildx prune -a -f"
    fi
  fi

  if [[ $KEEP_VOLUMES -eq 0 ]]; then
    run "docker volume prune -f"
  fi

  # Finally: system prune as a catch-all (respect volume preference)
  if [[ $KEEP_VOLUMES -eq 0 ]]; then
    run "docker system prune -a -f --volumes"
  else
    run "docker system prune -a -f"
  fi
}

# Cleanup using hard-boiled methods
bruteforce_reset() {
  [[ $PRUNE_ONLY -eq 1 ]] && return 0

  docker_access_check

  # Stop & remove containers (including running)
  if [[ $KEEP_CONTAINERS -eq 0 ]]; then
    run 'ids=$(docker ps -aq 2>/dev/null || true); if [[ -n "$ids" ]]; then docker rm -f $ids; fi'
  fi

  # Remove images
  if [[ $KEEP_IMAGES -eq 0 ]]; then
    run 'ids=$(docker images -aq 2>/dev/null || true); if [[ -n "$ids" ]]; then docker rmi -f $ids || true; fi'
  fi

  # Remove volumes
  if [[ $KEEP_VOLUMES -eq 0 ]]; then
    run 'ids=$(docker volume ls -q 2>/dev/null || true); if [[ -n "$ids" ]]; then docker volume rm -f $ids || true; fi'
  fi

  # Remove networks (skip default)
  if [[ $KEEP_NETWORKS -eq 0 ]]; then
    run 'ids=$(docker network ls -q 2>/dev/null || true); if [[ -n "$ids" ]]; then docker network rm $ids 2>/dev/null || true; fi'
  fi

  # Build cache again (belt-and-suspenders)
  if [[ $KEEP_BUILD_CACHE -eq 0 ]]; then
    run "docker builder prune -a -f || true"
    if docker buildx version >/dev/null 2>&1; then
      run "docker buildx prune -a -f || true"
    fi
  fi
}

stop_services() {
  # We try systemd first; fallback to service command.
  if command -v systemctl >/dev/null 2>&1; then
    run "systemctl stop docker || true"
    run "systemctl stop containerd || true"
  else
    run "service docker stop || true"
    run "service containerd stop || true"
  fi
}

wipe_disk_state_scorch() {
  # This is the "real reset". Requires root.
  is_root || die "Mode '$MODE' requires root. Re-run with sudo."

  # The canonical docker root dir is /var/lib/docker; we also clean /var/lib/docker/overlay2 etc.
  # We DO NOT touch /etc/docker or /var/run unless you want an even deeper reset.
  run "rm -rf /var/lib/docker"
  run "rm -rf /var/lib/docker.tmp 2>/dev/null || true"
}

wipe_disk_state_inferno() {
  # Wipes common containerd state dirs too (varies by distro).
  is_root || die "Mode '$MODE' requires root. Re-run with sudo."

  # containerd state locations can vary. These are common on Ubuntu/Debian with Docker + containerd.
  run "rm -rf /var/lib/containerd"
  run "rm -rf /run/containerd"
  run "rm -rf /var/lib/docker/containerd 2>/dev/null || true"
}

start_services() {
  if command -v systemctl >/dev/null 2>&1; then
    run "systemctl start containerd || true"
    run "systemctl start docker || true"
  else
    run "service containerd start || true"
    run "service docker start || true"
  fi
}

main() {
  parse_args "$@"

  need_cmd docker

  # Sanity: warn if scorch/inferno and not root
  if [[ "$MODE" == "scorch" || "$MODE" == "inferno" ]]; then
    warn "You selected '$MODE' mode: this is a full on-disk reset."
    warn "It will remove /var/lib/docker (and possibly containerd state)."
  fi

  confirm_or_die

  # Normal cleanup via docker commands
  if [[ "$MODE" == "all" || "$MODE" == "soft" || "$MODE" == "scorch" || "$MODE" == "inferno" ]]; then
    polite_phase
    bruteforce_reset
  fi

  # Full reset modes
  if [[ "$MODE" == "scorch" || "$MODE" == "inferno" ]]; then
    stop_services
    wipe_disk_state_scorch
    if [[ "$MODE" == "inferno" ]]; then
      wipe_disk_state_inferno
    fi
    start_services
  fi

  log ""
  log "Done."
  if [[ $DRY_RUN -eq 1 ]]; then
    log "(dry-run; no changes were made)"
  fi
}

main "$@"
