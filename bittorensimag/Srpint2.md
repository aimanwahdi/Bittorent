# Sprint 2

## Présentation

### Illustration

#### Traitement des messages
Switch => Machine à états

#### Stratégie de requêtage
Triviale et non optimisée

## Démonstration

### Performances

|  Expérience (en local ou distribué) | débit |
| :--------------- |:---------------:|
| monoclient (Leecher)  |    18 MB/s       |  
| monoclient (Seeder)  |        65 MB/s      |
| multiclient (Leecher/n Seeders)  |     45 Mb/s (réparti entre seeders)      |

### Tableau d'avancement

| Etape  | Avancement          |
| :--------------- |:---------------:|
| monoclient (Leech, Seed)  |   OK        |
| multiclient (Leech/Seed) avec java.nio ?  | OK             |
| message HAVE, CANCEL, KEEPALIVE  | KO          |
| stratégie de "requêtage"  | KO          |
| tracker contacté à intervalle régulier  | KO          |
| Tests automatisés  | OK          |
| Clients supportés (Vuze, aria2c, ...)  | OK          |
| Taille de pièce (32K et +)  | OK          |
| ServerSocket  | OK          |
| Vérification des pièces  | OK          |
