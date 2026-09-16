| Service              | Owns                        | Calls             | Called By               | Communication |
| -------------------- | --------------------------- | ----------------- | ----------------------- | ------------- |
| Identity Service     | Users, authentication, RBAC | —                 | Gateway                 | REST          |
| Policy Service       | Applications, Policies      | Identity, Payment | Gateway, Payment        | REST + event  |
| Claims Service       | Claims, settlement          | Policy, Payment   | Gateway                 | REST + event  |
| Payment Service      | Payments                    | —                 | Gateway, Policy, Claims | REST + event  |
| Notification Service | Notifications               | —                 | Policy, Claims, Payment | Event         |



