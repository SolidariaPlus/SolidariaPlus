#!/usr/bin/env python3
import json
import pathlib
import sys

import yaml

ROOT = pathlib.Path(__file__).resolve().parents[2]


def load():
    with open(ROOT / "infra" / "accounts.yml", encoding="utf-8") as handle:
        return yaml.safe_load(handle)


def environment(data, name):
    key = "prod" if name == "production" else name
    if key not in data["environments"]:
        sys.exit(f"Unknown environment '{name}' in infra/accounts.yml")
    return key, data["environments"][key]


def enabled_slots(data, env_key, env):
    slots = []
    for slot in data["apply_order"]:
        settings = env["slots"].get(slot, {})
        if settings.get("enabled"):
            slots.append(
                {
                    "slot": slot,
                    "slot_lower": slot.lower(),
                    "runner": settings["runner"],
                    "region": env["region"],
                    "environment": env_key,
                    "github_environment": env["github_environment"],
                }
            )
    return slots


def main(argv):
    if len(argv) < 2:
        sys.exit(__doc__)
    data = load()
    command = argv[0]
    env_key, env = environment(data, argv[1])

    if command == "slots":
        print(json.dumps(enabled_slots(data, env_key, env)))
    elif command == "slot-of":
        service = argv[2]
        for slot, settings in env["slots"].items():
            if service in settings.get("services", []):
                print(slot)
                return
        sys.exit(f"Service '{service}' is not mapped to a slot in infra/accounts.yml")
    elif command == "region":
        print(env["region"])
    elif command == "github-environment":
        print(env["github_environment"])
    elif command == "account-id":
        print(env["slots"][argv[2]].get("account_id") or "")
    elif command == "services":
        print(json.dumps(env["slots"][argv[2]].get("services", [])))
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
