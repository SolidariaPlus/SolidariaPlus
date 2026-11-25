# modules/vpc-base

## Overview

The permanent part of the network of one account: VPC, internet gateway, two public subnets and their
route table, plus a default security group that allows nothing. None of these resources has an hourly
price, so they are created once by `infra/terraform/bootstrap` and survive `session-stop`. The runner
lives in the first public subnet. `modules/network` adds the private subnets, NAT gateways and interface
endpoints in the environment roots.

## Interface

| Variable                  | Type   | Default | Description                                 |
| ------------------------- | ------ | ------- | ------------------------------------------- |
| `name`                    | string |         | Name prefix, for example `solidaria-qa-a1`  |
| `cidr_block`              | string |         | VPC CIDR (`infra/accounts.yml`, `vpc_cidr`) |
| `availability_zone_count` | number | `2`     | Availability Zones                          |

| Output               | Description                  |
| -------------------- | ---------------------------- |
| `vpc_id`             | VPC identifier               |
| `public_subnet_ids`  | Public subnets, one per zone |
| `availability_zones` | Zones used                   |

## Invariants

- The VPC has the tag `Name = <name>`. The environment roots find it by that tag and by the subnet tag
  `tier = public`.
- Public subnets take the `/24` blocks 0 and 1 of the VPC. `modules/network` uses blocks 10 and up, so
  the two modules never overlap.
- Public subnets do not assign public IPs by default. Resources that need one ask for it explicitly
  (the runner, the broker's Elastic IP).

## Side Effects

- None outside the VPC.

## Verification

- `terraform init -backend=false && terraform validate` in this directory (`ci.yml`, job `terraform`).
