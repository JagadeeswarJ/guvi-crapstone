the landlord will pay the premium amount, and the tenant dont have to pay the security deposit.
when the tenant either:
1. defaults on rent
2. does damage to property
3. abruptly exits from rented property -> lose of next month rent cause the property is without a tenant.

the landlord will be indemnified.
and the tenant will be subrogated according to the laws and the rental documents 


flow:
[ LANDLORD ] ──(1. Apply & Submit Data)──► [ UNDERWRITING ENGINE ]
                                                   │
                                            (Auto or Manual)
                                                   ▼
                                         [ UNDERWRITER ] (Approves/Sets Premium)
                                                   │
[ LANDLORD ] ◄──(2. Pays Premium)──────────────────┘
     │
     ▼ (Policy Bound / Active)
  ... Lease in progress ...
     │
     ▼ (Tenant Defaults / Damages / Abruptly Leaves)
[ LANDLORD ] ──(3. Submits Claim + Evidence)──► [ CLAIMS OFFICER ]
                                                        │
                                                 (Reviews Proofs)
                                                        ▼
[ LANDLORD ] ◄──(4. Receives Indemnity Payout)──────────┘
                                                        │
                                      (Emits ClaimSettledEvent)
                                                        ▼
                                             [ SUBROGATION ENGINE ]
                                                        │
[ TENANT ] ◄────(5. Legal Demand & Dunning)─────────────┘



changes:
1. there is no underwriting engine, only manual underwriting approves and sets the premium
2. 

input data:
input data for policy:

1. tenant details -> credit score
2. timestamped footage of property -> for proving damage during rental period
3. 



Actors:
1. Landlord
2. Claim
3. Underwriter
