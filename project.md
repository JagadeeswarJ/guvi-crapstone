# LeaseBond Insurance
- why the name: 
Domain: `Property & Casualty (P&C) insurance`

- Landlords take insurance against tenant-related financial losses, primarily rent default and covered property damage.
- Tenants avoid paying a large upfront security deposit, reducing the initial cost of renting.
- The landlord pays the insurance premium, and the insurer indemnifies the landlord for eligible losses under the policy.
- The system can support recovery/subrogation against the tenant after an eligible claim is settled, subject to the policy, lease agreement, and applicable law.


### Limits of subrogation
1. The Anti-Subrogation Rule: An insurance company cannot sue its own policyholder.
2. Made-Whole Doctrine:  It means that after an accident, you must get 100% of your money back before your insurance company is allowed to keep any money they recover from the at-fault party.
3. Waiver of Subrogation: If parties sign a contract waiving subrogation before any loss occurs (very common in commercial leases), the insurer is legally blocked from pursuing the at-fault party.


## Claims:
 | Case                   | Trigger                                                                        | What landlord claims for                                          |
| ---------------------- | ------------------------------------------------------------------------------ | ----------------------------------------------------------------- |
| **1. Rent default**    | Tenant fails to pay rent according to the lease                                | Covered unpaid rent, subject to policy limits/conditions          |
| **2. Property damage** | Tenant causes covered damage to the rented property                            | Repair/restoration costs for covered damage                       |
| **3. Abrupt exit**     | Tenant leaves before the agreed lease period and creates a defined rental loss | Potentially the **next month's lost rent**, subject to the policy |


## Flow
```
LEASEBOND INSURANCE

[ LANDLORD ]
     │
     │ 1. Submit Insurance Application
     │    - Property details
     │    - Tenant details
     │    - Lease details
     │    - Credit/risk information
     │    - Property condition evidence
     ▼
[ UNDERWRITER ]
     │
     │ 2. Reviews application
     │    - Risk assessment
     │    - Approves / Rejects
     │    - Sets premium
     ▼
[ LANDLORD ]
     │
     │ 3. Pays Premium
     ▼
[ POLICY ]
     │
     │ Policy becomes ACTIVE
     ▼
════════════════════════════════════════════════════
                LEASE PERIOD
════════════════════════════════════════════════════
     │
     ├── Tenant defaults on rent
     │
     ├── Tenant causes covered property damage
     │
     └── Tenant leaves early → covered rental loss
     │
     ▼
[ LANDLORD ]
     │
     │ 4. Submit Claim + Evidence
     ▼
[ CLAIMS OFFICER ]
     │
     │ 5. Review claim
     │    - Validate policy coverage
     │    - Review evidence
     │    - Determine claim amount
     │    - Approve / Reject
     ▼
[ LANDLORD ]
     │
     │ 6. Indemnity Payment
     ▼
[ CLAIM SETTLED ]
     │
     │ 7. Recovery / Subrogation process
     │
     ▼
[ TENANT ]
     │
     │ Legal demand / recovery
     │ according to policy + lease
     ▼
[ RECOVERY / SUBROGATION ]

```

## Input Data:
for policy:
1. Tenant Details - cibil score, address, income proof
2. rental agreement
3. property images as a prrof for property damage


## Actors:
1. landlord
2. Underwriter
3. Claims Officer
4. Admin



## Microservices
1. API Gateway
2. eureka
3. authservice
