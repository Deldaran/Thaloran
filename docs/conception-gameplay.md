# Thaloran — conception du gameplay

Document de référence pour le gameplay. Il part de ce qui existe déjà :

- le monde simulé sur 5000 ans : Foule, Villes, Etats, Economie, Routes, Livres ;
- les villes rejouées (Villages) ;
- la vie des villageois (Habitants, Villageois) ;
- les relations façon Nemesis (Relations) ;
- l'interface de scènes (Joueur, Interface).

Principe directeur, posé dès le début du projet : **tout ce que le joueur peut faire, un PNJ doit pouvoir le faire**. Le joueur n'est pas au centre du monde, c'est une personne de plus dans la Foule. Le monde continue de vivre sans lui.

### Décisions prises (2026-11-14)

- **Les quatre peuples sont jouables** : humain, nain, elfe, ork.
- **Les parents sont de vrais PNJ** de la Foule, vivants ou morts selon l'histoire de la ville. Le joueur peut aussi avoir **des frères et sœurs**, eux aussi de vrais PNJ.
- **Le travail se fait en temps réel**, avec la possibilité de **passer le temps de travail** si on ne veut pas le faire à la main.
- **Les batailles de bandes sont d'abord résolues automatiquement**. Les **batailles rangées jouées** viendront plus tard (phase 12).
- **Méthode** : Claude code et teste dans la copie m25, copie dans le projet, puis explique en détail ce qu'il a fait pour que l'utilisateur puisse y toucher plus tard.

---

## 1. Création du personnage

Le joueur crée une **nouvelle personne**, il n'incarne plus un habitant existant. Elle est insérée dans la Foule comme un vrai habitant, avec un identifiant, des parents, une maison et une ville. Les PNJ peuvent donc la connaître, en parler et s'en souvenir.

La création se fait **à la Bannerlord** : une suite de scènes racontées. Chaque choix est un moment de vie qui donne des compétences, des traits, de l'argent, des objets et des relations. À la fin, un récit résume la vie du personnage jusqu'au jour où le jeu commence.

### 1.1 Les étapes

| # | Étape | Ce qu'on choisit | Ce que ça donne |
|---|-------|------------------|-----------------|
| 1 | Peuple | humain, nain, elfe, ork | espérance de vie, âge adulte, bonus de départ, culture, noms, langues |
| 2 | Lieu de naissance | royaume → comté → ville (scènes existantes) | marché local, métiers disponibles, histoire vécue |
| 3 | Identité | sexe, nom (proposé par Noms, modifiable), apparence (calques de l'Assembleur) | — |
| 4 | Famille | métier et rang des parents, frères et sœurs, parents vivants ou morts | compétences de base, argent, maison, relations |
| 5 | Petite enfance (0–6 ans) | comment on t'a élevé | 1 attribut |
| 6 | Enfance (7–12 ans) | ce que tu faisais de tes journées | compétences, instruction, trait |
| 7 | Adolescence (13–16 ans) | apprentissage, maître, événement marquant | compétences, relation (maître, ami, rival) |
| 8 | Jeunesse (17–20 ans) | la voie prise | compétences, équipement, renom de départ |
| 9 | Âge de départ | 20, 30, 40 ou 50 ans | plus de compétences et d'argent, mais moins d'années à vivre |
| 10 | Aspiration | le grand but de sa vie (section 3) | suivi d'étapes, bonus liés |
| 11 | Récit | lecture du résumé, puis entrée dans le monde | — |

### 1.2 Les parents (étape 4)

On ne propose que les métiers **qui existent vraiment dans la ville choisie** (Metiers.besoins du profil de la ville). Une ville sans forge n'a pas de fils de forgeron.

| Origine | Rang (caste Foule) | Apporte |
|---------|---------------------|---------|
| Paysans (labour, troupeau) | 0 | Labeur, Survie, endurance, peu d'argent |
| Bûcherons ou mineurs | 0 | Labeur, Deux mains |
| Pêcheurs (si eau) | 0 | Survie, Athlétisme, Navigation plus tard |
| Artisans : forgeron, menuisier, tisserand, potier, tanneur, boulanger, boucher, meunier | 1 | la compétence d'atelier correspondante, outils, un peu d'argent |
| Aubergistes ou taverniers | 1 | Éloquence, Commerce, connaissance des rumeurs |
| Marchands (entrepôt, étal) | 1–2 | Commerce, Lettres, argent, contacts dans d'autres villes |
| Soldats ou gardes (caserne) | 1 | Une main, Hast, Athlétisme, une arme |
| Clergé ou lettrés (école, temple) | 2 | Lettres, Médecine, instruction (lire les livres, voir Savoir) |
| Petite noblesse vassale (si château ou mairie) | 3 | Équitation, Commandement, Intendance, nom de Maison, cheval, beaucoup de renom local, mais des devoirs et des ennemis |
| Hors-la-loi ou vagabonds | 0 | Discrétion, Survie, rancunes avec la garde |
| Orphelin | — | Discrétion ou Éloquence au choix, aucune attache, une rancune possible |

Les parents sont **de vrais PNJ** : ce sont les champs `pere` et `mere` de la Foule. S'ils sont vivants, ils habitent une maison de la ville. On peut les visiter, ils ont un Lien avec le joueur dès le départ et peuvent aider, demander de l'aide, mourir ou être menacés.

**Frères et sœurs.** Ce sont les autres enfants de la Foule qui ont les mêmes parents, plus ceux qu'on ajoute selon le choix « famille nombreuse, fils unique ». Chacun a :

- un âge, aîné ou cadet : l'aîné hérite de la maison et de l'atelier, le cadet doit partir faire sa vie, ce qui est une raison forte d'ambition ;
- son métier et sa ville (il peut être parti ailleurs) ;
- un Lien avec le joueur choisi pendant l'enfance : complice, rival, protecteur ou protégé.

Ils peuvent devenir des compagnons, des héritiers, des otages ou des rivaux.

La famille est tirée de l'histoire réelle de la ville (Villes.raconter). Si la ville a été saccagée quand le personnage avait 10 ans, la scène le propose : « Tes parents sont morts lors du sac de l'an 4993 », avec la maison en ruine qu'on voit en jeu.

### 1.3 Petite enfance (étape 5)

Un attribut, au choix :

- Tu étais un enfant robuste, qui ne tombait jamais malade : **Vigueur**.
- Tu grimpais partout et attrapais tout ce qu'on te lançait : **Adresse**.
- Tu courais du matin au soir sans te fatiguer : **Endurance**.
- Tu démontais et réparais les objets de la maison : **Habileté**.
- Tu posais sans cesse des questions : **Esprit**.
- Les autres enfants te suivaient partout : **Présence**.
- Tu savais toujours où étaient cachés les gâteaux : **Ruse**.

### 1.4 Enfance (étape 6)

Les choix dépendent de l'origine :

- Tu aidais à l'atelier ou aux champs de tes parents : compétence du métier familial, +1 relation avec eux.
- Tu gardais les bêtes dans les collines : Survie, Arc.
- Tu suivais le prêtre ou le lettré qui t'apprenait à lire : Lettres et instruction. Sans ce choix, ou un autre qui apprend à lire, le personnage ne lit pas les livres.
- Tu te battais avec les gamins du quartier : Une main, Athlétisme, et un ami ou un rival d'enfance, qui est un PNJ réel du même âge.
- Tu chapardais au marché : Discrétion, petite rancune d'un marchand, trait Fourbe.
- Tu traînais à la taverne pour écouter les voyageurs : Éloquence, connaissance des rumeurs et de régions lointaines.
- Tu soignais les animaux blessés : Médecine, trait Clément.

### 1.5 Adolescence (étape 7)

**Apprentissage**, chez un maître réel de la ville : un artisan, le capitaine de la garde, un marchand, un prêtre, un chasseur. On en tire la compétence principale et un **maître**, un PNJ avec un Lien fort, qui pourra embaucher le joueur plus tard.

**L'événement marquant** est tiré de la chronique de la ville et du royaume pendant ces années : sac, guerre, famine, couronnement, nouveau maître, épidémie. On choisit sa réaction :

| Réaction | Gain | Trait |
|----------|------|-------|
| Tu as pris les armes pour défendre la ville | combat | Brave |
| Tu as caché ta famille | Discrétion | Protecteur |
| Tu as fui | Survie | Prudent |
| Tu as pillé les maisons abandonnées | argent | Cupide |
| Tu as soigné les blessés | Médecine | Clément |

Si l'ennemi d'alors existe encore (un royaume, une Maison noble), le joueur en garde une **rancune**. C'est la graine d'une aspiration Vengeance.

**Premier amour ou amitié** : un PNJ réel, avec un Lien fort. C'est un futur compagnon possible.

### 1.6 Jeunesse (étape 8)

| Voie | Compétences | Départ |
|------|-------------|--------|
| Milice ou garde de la ville | Hast, Une main, Commandement | arme, armure légère, contact capitaine |
| Mercenaire dans une compagnie | deux armes, Tactique | équipement, petit renom, un ancien camarade |
| Caravanier | Commerce, Équitation, Survie | mule, contacts dans 2 à 3 villes |
| Compagnon artisan | compétence d'atelier | outils, droit d'ouvrir un atelier plus tard |
| Études (école, temple) | Lettres, Médecine, Intendance | livres, accès aux bibliothèques |
| Brigand dans les bois | Discrétion, Arc, Survie | réputation de bandit, contacts illégaux |
| Marin ou pêcheur (si eau) | Athlétisme, Survie | barque plus tard |
| Resté travailler la terre | Labeur | un lopin, des bêtes |
| Vagabond | Survie, Éloquence | connaît plusieurs régions |

### 1.7 Âge de départ (étape 9)

| Âge | Points de compétences en plus | Argent | Contrepartie |
|-----|-------------------------------|--------|--------------|
| 20 | 0 | 0 | longue vie devant soi |
| 30 | + | + | conjoint ou enfant possible au départ |
| 40 | ++ | ++ | moins d'années, enfants grands, qui sont des héritiers possibles |
| 50 | +++ | +++ | il faut préparer l'héritier tôt |

Les durées sont adaptées au peuple : un elfe de 50 ans est encore jeune.

### 1.8 Ce que la création écrit

- Une **fiche** : attributs, compétences, focus, traits, instruction, argent, inventaire, aspiration.
- Une **personne dans la Foule** : id, parents, maison, ville, rang, axes de caractère loi, bien, ambition, courage et foi, déduits des choix.
- Des **Liens** de départ : parents, maître, ami ou rival d'enfance, premier amour, ennemi historique.
- Un **récit** que le joueur peut relire dans son carnet, et que les PNJ peuvent connaître en partie selon le Savoir. La vieille voisine sait que « tu es le fils du forgeron ».

---

## 2. La fiche du personnage

### 2.1 Attributs et compétences

Il y a 7 attributs, chacun avec 3 compétences. Une compétence progresse **par l'usage**, comme dans Skyrim ou Kenshi. Le **focus**, comme dans Bannerlord, fixe la vitesse d'apprentissage et le plafond.

| Attribut | Compétences | Exemples d'usage |
|----------|-------------|------------------|
| Vigueur | Une main, Deux mains, Hast | combat au corps à corps |
| Adresse | Arc, Arbalète, Jet | chasse, combat à distance |
| Endurance | Athlétisme, Équitation, Labeur | courir, porter, voyager, couper du bois, miner, labourer |
| Habileté | Forge, Bois, Métiers fins | armes et outils ; meubles, charpente, chariots ; tissage, cuir, poterie, cuisine |
| Esprit | Lettres, Médecine, Intendance | lire, écrire, contrats ; soigner ; gérer un atelier, un domaine, des stocks |
| Présence | Éloquence, Commandement, Commerce | convaincre, recruter ; taille et moral de la bande ; prix d'achat et de vente |
| Ruse | Discrétion, Survie, Tactique | vol, filature, infiltration ; chasse, pistage, éclaireur ; batailles, embuscades |

On pourra ajouter plus tard Navigation (mers, corsaires) et Élevage.

### 2.2 Traits de caractère

On reprend les axes de la Foule, pour que les PNJ et le joueur soient comparables :

| Axe Foule | Pôles côté joueur | Ce qui le fait bouger |
|-----------|-------------------|-----------------------|
| loi | Loyal ↔ Rebelle | tenir ou trahir sa parole, respecter ou défier les seigneurs |
| bien | Bon ↔ Cruel | épargner ou exécuter, aider ou exploiter |
| ambition | Ambitieux ↔ Humble | chercher le pouvoir ou s'en détourner |
| courage | Brave ↔ Prudent | charger ou fuir |
| foi | Pieux ↔ Impie | prier, donner au temple, profaner |

On ajoute deux traits propres au joueur : **Généreux ↔ Avare** et **Honnête ↔ Fourbe**.

Les PNJ jugent le joueur selon leurs propres axes. Un garde loyal apprécie un chef loyal. Un brigand cruel méprise la clémence.

### 2.3 État du corps

On reprend faim, fatigue et social des villageois, et on y ajoute santé, blessures localisées et maladies. Le personnage vieillit, parce que le temps du jeu avance. Il meurt de vieillesse, de blessure ou de maladie. Le jeu continue alors avec **l'héritier**, enfant ou proche choisi, comme prévu dans la vision du jeu.

### 2.4 Argent et inventaire

- Monnaie en pièces (cuivre, argent, or). Chaque royaume pourra plus tard avoir son nom de pièce.
- Poids porté, limité par l'Endurance, puis mule, chariot, entrepôt.
- Objets : outils, armes, armures, vêtements (Tenue existe déjà), nourriture, marchandises, livres, lettres et contrats.

---

## 3. Aspirations (le grand but, comme dans les Sims)

L'aspiration est choisie à la fin de la création. On peut en changer à certains moments de vie : mariage, mort d'un proche, grande victoire, ruine. Chacune donne :

- une **suite d'étapes** visibles dans un panneau « Destin », avec des objectifs concrets et mesurables ;
- une **récompense par étape** : point de focus, trait, renom, objet ou titre ;
- des **envies** du jour, courtes et tirées du contexte (« boire avec Garin ce soir », « acheter une meilleure hache »). Les accomplir donne du moral.

### 3.1 Pouvoir

| Aspiration | Étapes (exemple) |
|------------|------------------|
| **Devenir roi ou reine** | se faire un nom (renom 2) → bande de 20 → servir un seigneur ou prendre un fort → obtenir un fief → rallier 3 vassaux → réclamer le trône (sang, conquête ou élection selon le peuple) → être couronné |
| Devenir noble | renom 2 → service d'un seigneur → adoubement, achat de titre ou mariage noble → premier fief |
| Diriger sa ville (maire, bourgmestre) | réputation 50 dans la ville → être membre d'une guilde → financer un bâtiment public → être élu ou nommé |
| Fonder une ville | rassembler 10 colons → trouver un site → camp → 20 maisons → ville reconnue (elle entre dans Villes et l'histoire) |
| Unifier son peuple | roi → contrôler toutes les capitales du peuple |

### 3.2 Guerre

| Aspiration | Étapes |
|------------|--------|
| Chef de guerre légendaire | 1, 5, puis 20 batailles gagnées → vaincre une armée deux fois plus grande → chanson écrite sur toi (Livres) |
| Capitaine mercenaire | compagnie de 30 → 3 contrats avec des seigneurs → compagnie de 100 → être connu dans 3 royaumes |
| Chevalier | écuyer d'un noble → adoubement → tournoi gagné → défendre un fief |
| **Vengeance** | identifier le coupable (souvent tiré de la création : la Maison qui a saccagé ta ville) → le retrouver → l'affaiblir → le vaincre ou le tuer |
| Roi des voleurs | guilde des voleurs dans une ville → réseau dans 3 villes → corrompre un seigneur |

### 3.3 Richesse

| Aspiration | Étapes |
|------------|--------|
| **Fonder une entreprise** | 1er atelier → 5 employés → 3 ateliers → une chaîne complète (ex. bûcherons → menuiserie → vente) → ateliers dans 3 villes |
| Grand marchand | 1 000 pièces → caravane → comptoirs dans 3 villes → route commerciale entre royaumes |
| Maître artisan | compagnon → chef-d'œuvre → maître de guilde → arme ou objet légendaire nommé |
| Banquier ou usurier | prêter à 10 personnes → prêter à un seigneur → financer une guerre |

### 3.4 Terre

| Aspiration | Étapes |
|------------|--------|
| **Travailler la terre** (paysan prospère) | un lopin → une récolte → des bêtes → une ferme avec ouvriers → nourrir une ville en famine |
| Seigneur foncier | un domaine de 10 champs → un moulin → un village de fermiers |
| Éleveur de chevaux | des chevaux → un haras → fournir la cavalerie d'un seigneur |

### 3.5 Savoir et foi

| Aspiration | Étapes |
|------------|--------|
| Érudit | apprendre à lire → lire 20 livres → découvrir une vérité cachée de la chronique (Savoir) → **écrire sa propre chronique**, qui entre dans Livres et que les PNJ liront |
| Guérisseur | soigner 10 blessés → ouvrir un dispensaire → arrêter une épidémie |
| Prêtre ou prophète | servir au temple → convertir → bâtir un temple → fonder un culte |
| Explorateur | visiter 10 régions → retrouver une ville en ruine ancienne → cartographier un royaume |

### 3.6 Famille et cœur

| Aspiration | Étapes |
|------------|--------|
| Fonder une dynastie | se marier → 3 enfants → un enfant noble ou maître → 3 générations |
| Grand amour | rencontrer → courtiser → épouser → vieillir ensemble |
| Protéger les siens | parents logés et nourris → maison en pierre → famille à l'abri de toute guerre |
| Sauver quelqu'un | lié à un événement : prisonnier, malade, enlevé |

### 3.7 Liberté et renom

| Aspiration | Étapes |
|------------|--------|
| Aventurier | 5 régions → ruine ancienne explorée → bête légendaire tuée |
| Hors-la-loi légendaire | prime sur ta tête → bande de brigands → vol d'un seigneur → chanson sur toi |
| Barde | apprendre 10 chansons → jouer dans 5 tavernes → composer une chanson (Livres) |
| Champion | gagner un tournoi → gagner dans 3 royaumes |
| Corsaire ou pirate (plus tard, avec les mers) | barque → navire → équipage → lettre de marque |

---

## 4. Renom, réputation et relations

### 4.1 Renom (la célébrité)

Le renom a **6 paliers**, comme les rangs de clan dans Bannerlord :

| Palier | Seuil | Effets |
|--------|-------|--------|
| 0 · Inconnu | 0 | personne ne te suit |
| 1 · Connu du quartier | 10 | des amis peuvent te suivre |
| 2 · Connu en ville | 50 | bande de 10 à 20, missions de la mairie |
| 3 · Connu dans le comté | 150 | les seigneurs te reçoivent |
| 4 · Célèbre dans le royaume | 400 | anoblissement possible, armée |
| 5 · Légendaire | 1000 | chansons, réclamer un trône |

**Le renom est local et voyage** : on reprend l'esprit Skyrim et les rumeurs de Relations. Un exploit est connu tout de suite dans la ville. Il se répand ensuite le long des Routes, porté par les marchands, les voyageurs et les bardes, et perd en détail ou se déforme en route. On peut être un héros à Caltamont et un inconnu à Garneroc.

On le stocke comme un renom par ville, diffusé chaque jour vers les villes voisines selon le trafic des routes.

### 4.2 Réputation

Il y a une valeur par groupe : ville, guilde, Maison noble, royaume, peuple, brigands. Elle est complétée par des **étiquettes** tirées des actes : honnête ou fourbe, généreux ou avare, cruel ou clément, brave ou lâche.

### 4.3 Relations individuelles

Les **Liens existants** sont estime, peur, rancune et souvenirs. On y ajoute :

- la **dette** : il te doit, ou tu lui dois ;
- les **promesses** faites et tenues ou brisées (section 8) ;
- l'**intérêt** amoureux.

### 4.4 Surnoms

Comme pour les rivaux Nemesis, le joueur reçoit des surnoms selon ses actes : « le Boucher de Garneroc », « la Généreuse », « Main-de-Fer ». Les PNJ les utilisent.

---

## 5. Économie

### 5.1 Deux niveaux

1. **Macro (régions)** : c'est l'Economie existante, avec production, besoin, stock, prix et échanges de nourriture, bois et minerai entre régions. On y ajoute de nouveaux biens.
2. **Micro (villes)** : chaque ville a un **marché** avec stock et prix par bien, ses ateliers avec leurs propriétaires, employés et recettes, et des **foyers** qui gagnent et dépensent.
   - Les villageois visibles (Habitants) sont l'incarnation de ce registre. La ville entière est simulée de façon abstraite, même les foyers qu'on ne voit pas.

### 5.2 Les biens et les chaînes

| Matière | Source | Transformation | Atelier | Produit |
|---------|--------|----------------|---------|---------|
| Céréales | champs | mouture | Moulin | Farine → (Boulangerie) Pain |
| Bétail | prés | abattage | Boucherie | Viande, Peaux, Salaisons |
| Poisson | mer, rivière | — | Pêcherie | Poisson, Poisson salé |
| Bois | forêt (bûcherons) | sciage | Menuiserie | Planches → meubles, chariots, charpente, arcs, manches |
| Pierre | carrière | taille | (Tailleur, à ajouter) | Blocs → maisons en pierre, murailles |
| Minerai de fer | mine | fonte (charbon) | Forge | Fer → outils, armes, armures, clous, fers à cheval |
| Charbon de bois | bois | meule | (Charbonnier, à ajouter) | combustible de la forge |
| Laine, lin | moutons, champs | filage, tissage | Tisserand | Tissu → vêtements, voiles, tentes |
| Peaux | Boucherie, chasse | tannage | Tannerie | Cuir → bottes, selles, armures de cuir |
| Argile | berges | cuisson | Potier | Poteries, briques, tuiles |
| Orge, raisin, miel | champs | brassage | Taverne | Bière, vin, hydromel |
| Herbes | cueillette | préparation | (Herboriste, à ajouter) | Remèdes |
| Sel | côte, mines | — | — | Conservation (salaisons) |
| Luxe | commerce lointain | — | Orfèvre (à ajouter) | Bijoux, épices, livres, chevaux |

### 5.3 Les prix

- Le prix local dépend du stock et du besoin, avec la même formule que `Economie.coursDe`.
- Les événements font bouger les prix : siège, guerre, famine, sac, routes coupées par les brigands, arrivée d'une caravane.
- Ces écarts entre villes créent les **occasions** du marchand.

### 5.4 Les ateliers

Chaque bâtiment de Metiers a :

- un **propriétaire** : PNJ, joueur, guilde ou seigneur ;
- des **employés**, qui sont des villageois avec une compétence et un salaire ;
- des **recettes** (entrées → sorties, temps, compétence) ;
- un **stock** et une **caisse**.

Il achète ses matières au marché et y vend ses produits. Il peut faire faillite, être racheté, brûler (Incendies) ou être reconstruit.

### 5.5 Foyers, impôts et loyers

- Les **foyers** touchent un salaire et achètent de la nourriture, du bois et des vêtements. La pauvreté pousse au vol, à l'émigration et à la révolte, et ces effets remontent dans la simulation d'histoire.
- **Impôts** : le seigneur prélève sur le commerce et la propriété. **Loyers** : les maisons appartiennent à quelqu'un.

---

## 6. Missions données par la ville

Les missions naissent **des besoins réels** de la ville, jamais d'une liste au hasard :

| Besoin réel | Mission générée |
|-------------|-----------------|
| Stock du marché sous le besoin (bois, grain, fer…) | Livraison : « La ville manque de bois : 40 stères au grenier, 3 pièces le stère » |
| Bâtiment manquant (phase 6 des villes : les villageois construisent ce qui manque) | Chantier : « Il faut 60 planches et 30 pierres pour le nouveau moulin », ou travail sur le chantier |
| Ruines après un sac (phase 4) | Reconstruction : déblayer, fournir, bâtir |
| Routes peu sûres (brigands sur les Routes) | Escorte de caravane, chasse aux brigands |
| Bêtes sauvages près des champs | Chasse aux loups |
| Crime (témoins de Relations) | Enquête : retrouver le voleur, l'assassin |
| Querelle entre deux PNJ (Liens) | Médiation |
| Guerre du royaume (Etats.ennemi) | Recrutement de la milice, livrer des armes, espionner |
| Dette impayée | Recouvrement |
| Lettre à porter | Messagerie vers une autre ville |
| Savoir perdu (Livres) | Retrouver un livre ancien dans une ruine |
| Malades | Apporter des remèdes, soigner |

**Qui donne les missions**

- Le maire, sur un tableau d'annonces de la place ou par le crieur.
- Les maîtres d'atelier, l'aubergiste, le capitaine de la garde, le prêtre.
- Les nobles, les marchands.
- Les **particuliers**, selon leur Lien avec le joueur : des demandes personnelles comme dans les Sims.

**Règles**

- Récompenses : argent, renom, réputation, relation, accès (licence, guilde, audience), objets, terres.
- Délai et échec : la ville souffre (pénurie, prix en hausse), la relation baisse.
- **Les PNJ prennent aussi les missions** : si le joueur ne livre pas le bois, un bûcheron ou une bande rivale le fera, et en tirera le renom.
- Les missions illégales (contrebande, sabotage, vol) passent par d'autres donneurs : tavernier louche, guilde des voleurs.

---

## 7. Travail et embauche

### 7.1 Être employé

- **Trouver du travail** : parler à un maître d'atelier, à un fermier, au contremaître du port ou au capitaine. Il embauche selon la compétence, la réputation, son Lien avec le joueur et ses besoins réels en main-d'œuvre.
- **Journalier** : pas de contrat, payé à la journée (moisson, porter des sacs, chantier).
- **Contrat** : salaire, horaires (la routine horaire des villageois existe déjà), repas, logement, part des bénéfices.
- **Travailler** :
  - en **temps réel** : le joueur fait les gestes du métier (couper un arbre, frapper le fer, porter un sac, servir à la taverne), avec gain de compétence, salaire, fatigue et petits événements (accident, client difficile, vol dans la caisse) ;
  - avec à tout moment la possibilité de **passer le temps de travail** (« finir la journée ») : le temps avance, et le résultat est calculé d'après les compétences comme pour un PNJ, avec un rendement un peu moindre que fait à la main.
- **Carrière** : apprenti → compagnon → maître (chef-d'œuvre, guilde) → son propre atelier. On peut être renvoyé (absences, vol, bagarre) ou promu.

### 7.2 Être employeur

- **Acheter** un atelier (à un PNJ, aux enchères après une faillite) ou en **bâtir** un sur un lot libre ou une ruine (Villages).
- **Embaucher** des villageois :
  - chacun a un salaire demandé, des compétences (talent de la Foule), une fiabilité et une loyauté ;
  - il peut refuser si le joueur a mauvaise réputation, ou partir s'il est mal payé ;
  - il peut se mettre en grève si les conditions sont injustes ;
  - il peut voler si le joueur ne contrôle jamais rien.
- **Gérer** : recette produite, quantités, prix de vente, achats, stocks. Il est possible de déléguer à un **intendant**, qui s'appuie sur sa compétence Intendance.
- **Concurrence** : ateliers PNJ, guildes qui protègent leur marché (licence obligatoire), seigneur qui taxe.

### 7.3 La terre

- Louer ou acheter des champs autour de la ville. ✅ Phase 11 : des lopins à la ferme du village, gérés en scènes. Les champs ne sont pas encore dessinés dans Villages.
- Saisons : labour, semis, récolte. ✅ Phase 11 : un calendrier de 4 saisons de 21 jours.
- Bêtes, grange, ouvriers agricoles. ✅ Phase 11 : les bêtes au pré, la grange, les journaliers.
- Récolte vendue au marché ou au grenier de la ville. En famine, c'est un pouvoir énorme.

---

## 8. Recrutement, bande et armée

### 8.1 Pourquoi te suivre ?

Personne ne suit le joueur au hasard. Chaque PNJ a un **profil de motivations**, calculé à partir de ses axes Foule, de sa situation et de son Lien avec le joueur :

| Motivation | Qui y est sensible | Ce qu'il faut offrir | Risque |
|------------|--------------------|----------------------|--------|
| **Argent** | pauvres, mercenaires, avides | une solde régulière | part chez le plus offrant |
| **Promesse d'un avenir meilleur** | ambitieux, cadets sans héritage, paysans sans terre | une terre, un titre, une part du butin, un métier | **la promesse est enregistrée** : la briser fait déserter ou trahir. Elle n'est crue que si ton renom et tes actes la rendent crédible |
| **Loyauté personnelle** | amis d'enfance, famille, gens que tu as sauvés, ton ancien maître | ton estime (Lien) | très fidèles, mais ils souffrent si tu changes |
| **Une cause** | idéalistes (loi, bien, foi) | une cause partagée : vengeance contre un tyran, libérer la ville, défendre le peuple ou la foi | partent si tu trahis la cause |
| **Gloire et aventure** | jeunes, courageux | des combats, de la renommée | s'ennuient sans action |
| **Besoin** | réfugiés après un sac, chômeurs, endettés, fugitifs | un toit, à manger, une protection | nombreux après une catastrophe (lien avec l'histoire) |
| **Peur ou contrainte** | faibles, prisonniers | une menace | peu fiables, fuient à la première défaite |

**Le dialogue de recrutement**

1. Le joueur demande : « Rejoins-moi ».
2. Il choisit un argument : solde, promesse, cause, amitié ou menace.
3. Le jeu compare cet argument aux motivations du PNJ, avec Éloquence ou Commandement, le renom local et les étiquettes de réputation.
4. Le PNJ peut **poser ses conditions** : « 3 pièces par semaine », « une terre quand tu seras seigneur », « tu protèges ma sœur ».

### 8.2 Taille maximale

La taille de la bande dépend du palier de renom et du Commandement : 3 au palier 1, 20 au palier 2, 60 au palier 3, puis des centaines avec des officiers.

### 8.3 Faire vivre la bande

- **Solde** : chaque semaine.
- **Nourriture** : prise sur les marchés réels, ce qui fait monter les prix locaux.
- **Moral**, qui dépend :
  - de la paie et des repas ;
  - des victoires ;
  - des promesses tenues ;
  - de l'accord entre les actes du joueur et les valeurs de chacun. Un garde loyal et bon quitte un chef qui massacre des villageois.
- **Équipement** : acheté ou fabriqué dans tes ateliers. C'est là que l'entreprise et la guerre se rejoignent.
- **Entraînement** : progression des compétences des membres.
- **Blessures, morts et désertions.**
- **Compagnons nommés** : des personnages uniques, comme les rivaux Nemesis.
  - Ils ont une histoire, des avis et des ambitions.
  - Un lieutenant ambitieux peut contester le chef.
  - Un compagnon peut avoir sa propre aspiration et demander de l'aide pour elle.

### 8.4 De la bande à l'armée

| Stade | Taille | Exige |
|-------|--------|-------|
| Bande | 3–20 | renom 1–2 |
| Compagnie | 20–60 | un camp ou une base, un lieutenant, une source de revenus (contrats, atelier, pillage) |
| Troupe | 60–200 | renom 3, des officiers, une logistique (chariots, réserves) |
| Armée | 200+ | un titre (noble), un fief ou une cause reconnue, des vassaux ou capitaines |

**Ce que fait une bande**

- Les contrats de ville : escorte, chantier, chasse aux brigands.
- ✔ Le mercenariat pour un seigneur, avec un contrat et une solde (phase 10).
- Le brigandage.
- La prise d'un fort ou d'un village.
- La défense d'une ville assiégée.

### 8.5 Le combat

- **Duels et rixes** : le système existant (frapper, à terre, mort, témoins).
- **Batailles de bandes** : d'abord une **résolution automatique**, comme la simulation de Bannerlord. Elle compare effectifs, équipement, compétences, Tactique, terrain et moral. Les **batailles rangées jouées** en temps réel viendront plus tard (phase 12).
- **Conséquences** : butin, prisonniers (rançon, recrutement forcé, exécution, qui touchent les traits), renom, et rancunes chez les familles des morts. Ces rancunes sont la source de nouveaux rivaux Nemesis.

---

## 9. Noblesse et politique

### 9.1 Devenir noble

1. **Adoubement** : servir un seigneur, se faire un renom de palier 3, accomplir un exploit pour lui. ✅ Phase 10 (renom 50, faveur 60, un exploit).
2. **Achat d'un titre** à un roi endetté, pour un riche marchand. ✅ Phase 10.
3. **Mariage** dans une Maison noble (Maisons existe déjà). ✅ Phase 10.
4. **Conquête** d'un fief, puis reconnaissance par un roi, ou fondation de son propre royaume. ✅ Phase 10 : une ville prise devient un fief ; le trône se réclame.
5. **Récompense de guerre.**
6. **Élection**, selon les peuples (cela se règle dans Peuple).

### 9.2 Être noble

- Un **fief** (village, ville, comté) : impôts, justice, garnison, construction, loi locale.
- Des **devoirs** envers son suzerain : lever des troupes, payer, conseiller.
- La **diplomatie et l'intrigue** : alliances, mariages, complots, trahisons.
- Le joueur entre dans **Etats** : son fief et son royaume vivent dans la simulation d'histoire comme ceux des PNJ. Ses guerres sont écrites dans la chronique, et les livres parleront de lui.

### 9.3 La royauté

- Les **prétentions** : sang (Maison), conquête, élection, soutien populaire.
- Rébellion, guerre civile, usurpation. Le roi existant est un PNJ avec sa Maison, ses vassaux et ses rivaux.

---

## 10. Temps et échelle

- Jour et nuit, comme déjà en place, et des **saisons** à ajouter.
- Temps accéléré pendant le travail, le sommeil et les voyages.
- **L'histoire continue pendant le jeu** : la simulation avance d'un an pendant qu'on joue. Guerres, famines et sacs peuvent toucher la ville du joueur.
- Vieillesse, mort, **héritier**.

---

## 11. Où ça s'accroche dans le code

| Système | Classes existantes | À créer |
|---------|--------------------|---------|
| Création | Joueur (scènes royaume → comté → ville), Interface, Assembleur, Noms | `Creation` (scènes de vie), `Fiche` (attributs, compétences, traits, argent) ; insertion dans la Foule (`Foule.ajouterJoueur`) |
| Fiche et inventaire | Personnage, Tenue | `Fiche`, `Inventaire`, `Objets` (catalogue) |
| Aspirations | — | `Destin` (aspiration, étapes, envies), panneau dans Interface |
| Renom et réputation | Relations (Lien, rumeurs), Routes | `Renom` (par ville, diffusé par les routes), étiquettes |
| Économie de ville | Economie (régions), Metiers, Villages | `Marche` (par ville), `Atelier` (recettes, employés, caisse), `Biens` |
| Missions | Habitants, Relations | `Missions` (générées depuis Marche, chantiers, Routes, Relations) |
| Travail | Habitants (routine, travail), Villageois.travail | `Emploi` (contrats, salaires, carrière) |
| Bande | Habitants (combat), Relations | `Bande` (membres, motivations, promesses, moral, solde) |
| Batailles | — | `Bataille` (résolution automatique) |
| Noblesse | Etats, Maisons | prise en compte du joueur comme chef ou vassal |
| Sauvegarde | Joueur.enregistrer, Relations | un format de sauvegarde du joueur étendu |

---

## 12. Ordre de construction

Chaque phase est jouable et testée avant la suivante.

1. **Fiche et création** : création complète à la Bannerlord, aspiration choisie, insertion dans la Foule avec de vrais parents, récit.
2. **Inventaire, argent, objets** : catalogue des biens et objets ; acheter et vendre à un étal au prix du marché local.
3. **Économie de ville** : marché par ville, ateliers avec recettes, stocks et caisses, foyers. La ville tourne seule, même sans le joueur.
4. **Travail et embauche** : se faire embaucher, travailler (temps accéléré, compétences, salaire), carrière.
5. **Missions de ville** : générées par les besoins réels, avec tableau, donneurs, délais et récompenses.
6. **Renom, réputation, Destin** : renom local qui voyage, étiquettes, panneau des aspirations et envies.
7. **Entreprise** : acheter ou bâtir un atelier, embaucher, gérer, intendant.
8. **Recrutement et bande** : motivations, promesses, moral, solde, compagnons nommés.
9. **Combat de groupe** : résolution automatique, brigands, mercenariat, butin et prisonniers.
10. **Noblesse et armée** : adoubement, fiefs, armée, entrée dans Etats, royauté. ✅ Faite le 2026-11-15 (lots 10a et 10b).
11. **Terre et famille** : champs, saisons, élevage, mariage, enfants, héritier. ✅ Faite le 2026-11-15 (lots 11a et 11b).
12. **Batailles rangées jouées** : remplacent la résolution automatique quand le joueur est présent. Il faut des formations, des ordres, le terrain réel de la carte et le moral par unité.

---

## 13. État d'avancement

### Phase 1 — faite le 2026-11-14

Ordre réel des scènes, légèrement différent du tableau 1.1 : l'âge de départ est choisi tout de suite après le sexe, parce qu'il fixe l'année de naissance, donc les événements vécus.

peuple → royaume → comté → ville → sexe → âge → nom → métier des parents → sort des parents (ou orphelin) → frères et sœurs → lien avec le frère ou la sœur le plus proche → petite enfance → enfance (→ ami ou rival si bagarre) → apprentissage → l'année où tout a changé → premier amour ou ami → jeunesse → une scène par décennie en plus (mariage après la première, enfants ensuite) → aspiration → récit → entrée dans le monde.

| Fichier | Rôle |
|---------|------|
| `histoire/Fiche.java` | la fiche : 7 attributs, 21 compétences, focus, expérience, 7 axes de caractère, argent en cuivre, objets, récit, proches, aspiration ; `apprendre()` est déjà prêt pour le travail (phase 4) |
| `histoire/Aspirations.java` | le catalogue des 27 aspirations en 7 familles, avec leurs étapes |
| `histoire/Creation.java` | toutes les scènes de la création, le retour en arrière, les données des origines et des voies, et l'insertion dans la Foule |
| `histoire/Joueur.java` | lance la création, garde la fiche, nouveaux menus (fiche, famille, destin, vie, apparence), sauvegarde FORMAT 2 |
| `histoire/Foule.java` | `inserer`, `placeDe`, `couples`, `gens`, `enfantsDe`, `unir`, `idReserve` |
| `Interface.java` | champ de saisie du nom, liste de choix qui défile |
| `FirstScreen.java`, `CameraController.java` | touches du jeu coupées pendant la saisie ; apparence et sexe du personnage créé |
| `Assembleur.java` | `tirer(graine, espece, sexe)` impose le sexe et l'âge adulte |

Reste à faire plus tard : ✔ le métier des parents et des proches est écrit (lot G4) ; ✔ on croise désormais les proches en ville (lot F) ; les envies du jour arriveront avec le Destin (phase 6).

### Phase 2 — faite le 2026-11-14

Le joueur a un **sac** (des piles de biens, certaines portées sur lui), de l'argent, et peut **acheter et vendre** dans chaque boutique de sa ville. Les prix suivent le cours régional calculé par l'Économie.

| Fichier | Rôle |
|---------|------|
| `histoire/Biens.java` | le catalogue : 72 biens en 10 catégories (vivres, matières, outils, armes, armures, vêtements, montures, bétail, livres, luxe). Pour chacun : prix de base en cuivre, poids, matière de l'Économie et part de cette matière dans le prix, boutiques qui le vendent, place où il se porte, pièce de la Tenue et couleur, compétence, valeur (force ou protection), charge en plus (sac à dos, mule). Contient aussi `reconnaitre()` (« trois livres » donne le livre ×3) et `habiller()` (met sur le sprite ce que le joueur porte) |
| `histoire/Inventaire.java` | les piles : ajouter, retirer, poids, porter (une seule chose par place), `porterLeMieux()`, sauvegarde |
| `histoire/Marche.java` | les boutiques d'une ville (le marché, plus chaque atelier présent selon `Metiers.besoins`), le stock du jour, la caisse du marchand, les prix d'achat et de vente, les transactions |
| `histoire/Fiche.java` | `sac`, `recevoir(objet)`, `capacite()`, `allure()` ; sauvegarde du sac |
| `histoire/Joueur.java` | scènes « Mon sac », « Aller au marché », boutique, combien acheter, vendre ; sauvegarde FORMAT 3 (les fiches FORMAT 2 sont converties) |
| `histoire/Creation.java` | les objets de la création passent par `recevoir` ; à l'entrée, le personnage porte le meilleur de chaque place |
| `FirstScreen.java` | touches **M** (marché) et **U** (sac) ; vitesse selon la charge ; sprite habillé selon l'équipement porté |

Les règles, toutes réglables en haut de `Marche.java` :
- **Prix local** = prix de base × (1 + part × (cours régional ÷ cours moyen − 1)). Le cours est borné entre 0,6 et 2. Il est × 1,25 si aucun atelier de la ville ne fabrique ce bien.
- **Le joueur paie** : prix local × 1,15, puis la marge baisse jusqu'à 1 avec la compétence Commerce (au maximum à 150). Il paie × 1,1 s'il est étranger, c'est-à-dire d'un autre peuple que la ville.
- **Le marchand rachète** : prix local × 0,55, jusqu'à 0,75 avec le Commerce. C'est × 0,8 si ce n'est pas sa spécialité. Il est limité par sa caisse du jour (100 + 2 × habitants cuivre, × 2 pour la forge, × 3 pour l'entrepôt).
- **Stock du jour** : tiré au hasard selon la taille de la ville et le prix du bien. Il est le même pour tout le jour, puis renouvelé chaque jour de jeu. Ce que le joueur achète ou vend ce jour-là est retenu.
- **Charge** : 20 kg + 3 × Vigueur + 2 × Endurance + Athlétisme ÷ 20 + le sac à dos et la monture. Ce que l'on porte sur soi compte pour moitié. Au-delà de la charge, on ralentit jusqu'à 35 % et on ne peut plus courir.
- Chaque achat ou vente fait gagner un peu de Commerce.

Reste à faire plus tard : le stock tiré au hasard sera remplacé par le vrai marché de la ville en phase 3 (producteurs, réserves, caisses qui durent). On commerce depuis le menu : marcher jusqu'à l'étal viendra avec la phase 3. ✔ Manger les vivres : fait au lot A (la faim). ✔ Les livres achetés sont reliés aux vrais livres du monde (lot G4). La force des armes et la protection des armures serviront au combat (phase 9).

### Phase 3 — faite le 2026-11-14 (première version)

Le stock tiré au hasard de la phase 2 est remplacé par une **vraie économie de ville**, simulée jour par jour. Chaque lieu de commerce est un **agent** avec ses ouvriers, sa caisse, son stock, son coût de revient et son prix. On ne commerce plus de partout : **il faut être devant** l'atelier, l'étal ou la ferme.

**La chaîne.** Les fermes produisent grain, légumes, fromage, laine, porcs, bêtes. Les bûcherons font les billes et les fagots, la mine le minerai. Ensuite, chaque artisan transforme :
- le moulin fait la farine avec le grain ;
- le boulanger fait le pain avec la farine ;
- le boucher découpe le porc en viande, jambon et peau ;
- le tanneur fait le cuir, puis les bottes, sacs et cuirasses ;
- le tisserand fait le drap avec la laine, puis les capes et gambisons ;
- le menuisier fait les planches avec les billes, puis les arcs et lances ;
- la forge fond le fer (minerai et charbon), puis fait les outils et les armes.

Le **marché** (les étals) rachète en gros aux producteurs et revend avec sa marge. Les **colporteurs** importent ce que la ville ne produit pas, au prix de la région × 1,35. Les surplus partent à l'export, au prix de la région × 0,7.

**Pourquoi le moulin est moins cher que le marché.** Chaque intermédiaire ajoute sa marge : farine au moulin 11,5, au grenier 12,7, à l'étal 15,4. Mais le moulin est au bord de l'eau, loin de la place. Les habitants choisissent où acheter selon `accès ÷ (prix relatif)³` : place 1, boulangerie et boucherie 0,7, rue 0,5, rive 0,2, bord (moulin, grenier) 0,15, fermes 0,06. La plupart achètent donc sur la place. Le joueur peut faire le chemin et payer moins.

| Fichier | Rôle |
|---------|------|
| `histoire/Recettes.java` | qui fabrique quoi avec quoi (≈ 70 recettes) et la consommation par habitant et par jour (`CONSO`, plus `CONSO_GARNISON` s'il y a une caserne). Le temps de travail se déduit des prix du catalogue : jours = (valeur produite − valeur des matières) ÷ `VA_JOUR` (12). Salaire : `SALAIRE` = 8 cuivre par jour |
| `histoire/EconomieVille.java` | la simulation d'une ville : agents, `unJour()` (cibles → produire → s'approvisionner → foyers → export, prix, embauche), sauvegarde |
| `histoire/Marche.java` | garde une `EconomieVille` par ville visitée (60 jours de rodage la première fois, puis rattrapage du temps écoulé, 60 jours au plus) ; prix pour le joueur (Commerce : jusqu'à −10 % ; étranger : +10 %), achat et vente |
| `histoire/Biens.java` | 4 biens intermédiaires ajoutés (minerai, bille de bois, porc, peau brute) et les lieux `CHAMPS`, `FORET`, `MINE`, `DEHORS` |
| `histoire/Joueur.java` | scène Commerce (« ici » ou « à 140 m »), boutique, vente ; horloge de jeu sauvegardée (`heuresVecues`, un jour = 24 h de la météo) ; FORMAT 4 qui sauvegarde aussi les économies des villes |
| `FirstScreen.java` | `reperer()` trois fois par seconde : village le plus proche, distance à chaque atelier (par son `plan.metier`), aux étals et à la place, et à la sortie de la ville ; bandeau « Devant toi : le moulin (M pour commercer) » ; bouton de débogage « Commercer de partout » |

Les règles, en haut d'`EconomieVille` :
- **Prix d'un agent** = coût de revient × (1 + marge) × pression du stock, où la pression vaut (cible ÷ stock)^0,35, bornée entre 0,6 et 2,5. Le prix est lissé de 25 % par jour. Il ne descend pas sous le coût, sauf si l'agent croule sous les stocks, et ne monte pas au-dessus de l'import.
- **Marges** : fermes 10 %, artisans 15 %, grenier 12 %, marché 35 %, entrepôt 30 %, auberge et taverne 40 %.
- **Les achats entre agents** se font en gros, au prix du vendeur × 0,9 (`GROS`). La commande est signalée au producteur local même s'il n'a plus rien : c'est ce qui le fait produire et embaucher.
- **Stocks visés** : 4 jours de demande pour les produits (`JOURS_STOCK`), 5 jours pour les matières (`JOURS_MATIERE`). Sans commande, un atelier ne prépare un objet pour la vitrine que s'il en a les moyens.
- **Embauche** : au-delà de 85 % d'occupation, +3 % d'ouvriers ; sous 40 %, −3 %. Le plafond est de 15 ouvriers par bâtiment.
- **Foyers** : ils reçoivent les salaires, les bénéfices des ateliers et un revenu hors marché (0,6 cuivre par habitant et par jour). Ils dépensent au plus 15 % de leur épargne par jour.
- **Un atelier à sec** est renfloué par l'épargne de la ville.

Le joueur rachète au prix de l'agent. Il vend à 80 % (jusqu'à 90 % avec le Commerce) de ce que la marchandise vaut pour l'agent : son prix d'achat en gros s'il en a besoin, son coût s'il la fabrique, le prix d'export pour l'entrepôt et le marché. C'est moitié moins s'il en a déjà trop. La vente est limitée par la caisse de l'agent.

Reste à faire plus tard :
- les autres villes ne tournent que quand on y passe : leur temps est rattrapé à l'arrivée (choix assumé, lot G4) ;
- ✔ commandes : fait au lot D ;
- ✔ les foyers restent agrégés (choix assumé) ; le métier de chacun est tiré de l'économie de sa ville (lot G4) ;
- ✔ impôts du seigneur (lot D) et loyers (lot A) ;
- ✔ caravanes entre villes : fait au lot D (les colporteurs restent pour les villes lointaines).

### Phase 4 — faite le 2026-11-14 (première version)

Le joueur peut **se faire embaucher et travailler**. Touche **L** (ou « Chercher du travail » / « Mon travail » dans le menu du personnage). Comme pour le commerce, on demande du travail **sur place**, devant l'atelier, la ferme ou l'étal.

**Deux façons de travailler :**
- **à la journée** (journalier) aux champs, au bois, à la mine, au moulin, au grenier, à l'entrepôt, à la pêcherie et au marché : sans contrat, payé le soir 6 cuivre + compétence ÷ 25 par journée complète ;
- **sous contrat** chez un artisan (tout atelier qui produit) : **apprenti** (3 cuivre, nourri) → **compagnon** (8 + compétence ÷ 15) → **maître** (14 + compétence ÷ 10).

**L'embauche.** Le patron décide d'après : s'il a besoin de bras (l'occupation de l'atelier dans l'économie), ton savoir-faire dans la compétence du métier, ta Présence, ton Éloquence, ton renom, ce qu'il pense de toi (son **Lien** : le maître est le vrai villageois qui travaille dans le bâtiment) et si tu es d'ici. Un refus vaut pour la journée.

**Travailler.** « Travailler jusqu'au soir » ou « deux heures » : le temps passe vraiment (la météo avance), dans les heures d'ouverture (6 h à 19 h). Le résultat dépend du **rendement** = 0,35 + compétence ÷ 120 (× 0,9 quand on fait passer le temps), réduit par la fatigue. Les journées faites **produisent dans l'atelier** (`EconomieVille.travailJoueur`) et le salaire **sort de sa caisse** ; si elle est vide, on te doit des arriérés, payés à la séance suivante. On apprend la compétence du métier (× 1,5 pour un apprenti) et l'Athlétisme dans les travaux de force. La **fatigue** monte en travaillant (moins avec l'Endurance) et baisse au repos ; « Dormir jusqu'au matin » fait passer la nuit.

**Petits événements** (par journée complète) : accident dans les travaux de force (l'Adresse l'évite), client difficile dans les commerces (Éloquence et Présence), **caisse restée ouverte** (prendre ou non : Honnêteté ; être pris, c'est le renvoi), éloge du maître.

**Carrière.**
- Le maître a une **estime** de toi (0 à 100). Elle monte si tu travailles mieux que ce qu'il attend de ton rang, et baisse pour chaque jour manqué au-delà d'un jour de repos. Sous 15, il te renvoie.
- Apprenti → compagnon : le maître le décide de lui-même quand tu as 40 dans la compétence, 20 jours d'apprentissage et un maître content.
- Compagnon → maître (métiers de guilde seulement) : 100 dans la compétence, 30 jours comme compagnon, maître content. Il faut aussi présenter un **chef-d'œuvre**, qui coûte 2 jours et 150 cuivre de droit. On le réussit si compétence + 10 × Habileté + un jet de 0 à 60 atteint 200 ; sinon on peut réessayer 10 jours plus tard. On peut aussi demander une **augmentation** quand on a progressé, ou démissionner.
- La compétence plafonne selon l'attribut et le focus (`Fiche.plafond`). Sans don, on ne devient pas maître, et l'écran le dit.

| Fichier | Rôle |
|---------|------|
| `histoire/Travail.java` | contrat, scènes (travail, chercher, offre, embauche, séance, caisse, chef-d'œuvre, augmentation, démission), fatigue, absences, carrière, sauvegarde. Tous les réglages sont en haut |
| `histoire/EconomieVille.java` | `fabriquer()` (production commune), `travailJoueur()` : les heures du joueur produisent dans l'atelier, sans salaire versé aux foyers |
| `histoire/Joueur.java` | FORMAT 5 (sauvegarde le travail) ; `passer(heures)` demande à l'écran de faire avancer le temps ; `heureLocale`, `maitres` (Lien du villageois de chaque atelier), `allerTravailler()` |
| `FirstScreen.java` | touche L ; applique les heures passées à la météo ; `reperer()` trouve le villageois qui travaille dans chaque atelier proche ; bandeau « M pour commercer, L pour travailler » |

Banc d'essai m25 : `CREATION=1 TRAVAIL=1` (journée aux champs, embauche, 45 jours de forge, caisse ouverte, absences, rechargement). Résultat : apprenti à Forge 0, compagnon au 36ᵉ jour (Forge 40), salaire passé de 3 à 11 cuivre.

Reste à faire plus tard :
- ✔ le travail **à la main en temps réel** (faire les gestes du métier) : fait (passe « en action ») ;
- ✔ les ouvriers des ateliers sont de vrais habitants de la Foule (lot G4) ;
- ✔ logement et repas de l'apprenti, horaires par métier (le boulanger avant l'aube), congés : faits (lot G1) ;
- ✔ grève, licences et enchères d'ateliers en faillite : faits (phase 7 et lot D) ;
- ✔ le chantier comme travail à la journée : fait (lot G1).

### Phase 5 — faite le 2026-11-14 (première version)

Les **missions de ville** naissent des besoins réels de l'économie (phase 3). Touche **Q** (ou « Missions » dans le menu du personnage). Les annonces sont au **tableau de la place** : il faut être sur la place pour le lire et accepter. On rend une mission là où elle a été demandée.

**Les trois sortes de missions (v1) :**
- **Livraison.** Un atelier manque d'une matière (stock sous 40 % de ce qu'il vise) ou doit la faire venir des colporteurs (nouvelle mesure `importe` dans `EconomieVille`). Il paie le prix d'import × 1,05 la pièce. Les étals, le grenier et l'entrepôt ne demandent que ce que la ville ne fabrique pas (sel, vin, poisson séché…). Une livraison vaut au moins 60 cuivre, au plus 2 500, et pèse au plus 60 kg. On peut livrer en plusieurs fois ; l'atelier paie de sa caisse et la ville avance le reste.
- **Lettre.** Le bourgmestre envoie du courrier vers une ville voisine reliée par une route ouverte (`Routes`). Paie 15 + 1,2 cuivre par km, avec un délai calculé sur 25 km de marche par jour. On la remet sur la place de la ville de destination.
- **Armes.** Quand le royaume est en guerre (`Etats.ennemi`), le capitaine de la garde demande des lances, javelots, flèches, boucliers, casques ou gambisons pour la milice, à la valeur × 1,3, avec 2 de renom.

**Règles :**
- au plus 6 annonces par ville et 5 missions acceptées ; le tableau se met à jour une fois par jour et par ville ;
- le donneur est le vrai villageois de l'atelier quand on le connaît (« Syion, le tavernier ») : réussir améliore son Lien, échouer ou abandonner le dégrade ;
- une annonce que personne ne prend est faite par un autre (colporteur, messager…) à l'échéance ; une mission acceptée et non tenue est « manquée » : le Lien baisse, ainsi que le renom si la mission en donnait ;
- récompenses : argent, renom (1 pour une grosse livraison ou une longue lettre, 2 pour les armes), un peu de Commerce.

| Fichier | Rôle |
|---------|------|
| `histoire/Missions.java` | naissance des missions (`actualiser`), échéances, scènes (missions en cours, tableau, détail, accepter, livrer, abandonner), sauvegarde. Réglages en haut |
| `histoire/EconomieVille.java` | `importe[]` : ce que chaque agent fait venir des colporteurs, lissé jour après jour |
| `histoire/Joueur.java` | FORMAT 6 (sauvegarde les missions), `allerAuxMissions()`, entrée « Missions » au menu |
| `FirstScreen.java` | touche Q ; le bandeau devant le marché rappelle « Q pour le tableau des missions » |

Banc d'essai m25 : `CREATION=1 MISSIONS=1`. Résultat (graine de création 5) : tableau avec livraison de poisson séché et lettre ; les deux sont livrées (115 et 90 cuivre, +1 renom) ; une mission acceptée et laissée échoue à l'échéance ; rechargement identique. Les missions d'armes n'ont pas pu être testées : la ville d'essai n'était pas en guerre.

Reste à faire plus tard :
- ✔ chantiers : relever une maison en ruine en fournissant les planches (lot E) ;
- ✔ chasse aux loups, escorte d'un marchand (lot E) ; chasse aux brigands (phase 9) ;
- ✔ enquêtes sur un vol (témoins), médiation entre deux voisins, recouvrement de dettes (lot E) ;
- ✔ demandes des villageois qui t'estiment (lot E), et des proches de la famille (lot G2) ;
- ✔ missions illégales (contrebande, vol) par le tavernier louche (lot E) ;
- ✔ les PNJ qui prennent les missions en gagnent le renom : le serviable de chaque ville (lot G1).

### Phase 6 — faite le 2026-11-14 (première version)

**Renom local qui voyage** (`histoire/Renommee.java`) :
- Le renom est tenu **ville par ville**, avec les 6 paliers de la section 4.1 : inconnu, connu du quartier (10), connu en ville (50), connu dans le comté (150), célèbre dans le royaume (400), légendaire (1000).
- Un acte rapporte du renom là où il a lieu :
  - mission réussie : 1,5 à 8 ;
  - lettre : la moitié de son renom aussi dans la ville qui la reçoit ;
  - passage compagnon : 3 ;
  - maîtrise : 15 ;
  - étape du destin : 5.
- Chaque jour, le renom **voyage le long des routes ouvertes** : chaque ville voisine reçoit 1,2 % de l'écart × le niveau de la route (sentier 1 … voie pavée 4) ÷ (1 + km ÷ 40). Il s'efface de 0,2 % par jour.
- La **réputation** (−100 à 100) suit les actes : mission tenue +3, manquée −4, abandonnée −2, vol découvert −20, renvoi −5, jour de travail +0,3. Elle voyage deux fois moins vite.
- Les **étiquettes** comptent les actes marquants et donnent un **surnom** : « Main-Leste » (pris à voler), « Maître forgeron », « le Messager » (3 lettres), « Rend-Service » (8 missions), « Promet-Tout » (3 manquées), « l'Infatigable » (40 jours de travail).
- `Fiche.renom` reste le renom total accumulé.

**Les villageois réagissent, en action** (`Habitants.accueillir`) : un passant qui ne te connaît pas encore personnellement peut te saluer par ton surnom (« Tiens, Rend-Service ! On parle de toi, ici. »), avec une chance de 25 % par palier de renom dans ce village. Si ta réputation y est mauvaise, il crache, te menace et se méfie (« Voilà Main-Leste… Gardez vos bourses. »). Sa première estime de toi suit ta réputation.

**Destin** (`histoire/Destin.java`, « Mon destin » dans le menu du personnage) :
- **Étapes vérifiées d'après le jeu** : renom 50, estime de la ville (réputation 50), 1 or, devenir compagnon, chef-d'œuvre, avoir un lopin, des bêtes, des chevaux, savoir lire, lire 20 livres, visiter 10 ou 5 régions, être connu dans 3 royaumes. Les autres sont marquées « pas encore possible dans le jeu ».
- Une étape franchie donne **un point de focus** dans la compétence de l'aspiration, du renom, une ligne dans le récit et une annonce.
- **Envies du jour** (2 par jour), tirées du contexte :
  - une bonne journée de travail (6 h) ;
  - boire une bière à la taverne ;
  - t'offrir un objet vendu en ville ;
  - rendre service (une mission) ;
  - lire un livre ;
  - aller voir un atelier, ce qui se fait en marchant devant, sans menu.
- Une envie accomplie donne +8 de moral, une envie manquée −3. Le **moral** joue sur le rendement au travail (× 0,85 à × 1,15).
- La page « Ce qu'on dit de moi » montre le surnom, les villes où l'on te connaît, et la réputation.

**Bandeau du jeu** : les annonces (envie accomplie, renom gagné, nouveau palier, étape du destin) passent dans le bandeau en bas de l'écran, sans ouvrir de fenêtre.

| Fichier | Rôle |
|---------|------|
| `histoire/Renommee.java` | renom et réputation par ville, diffusion par les routes, étiquettes, surnom, sauvegarde |
| `histoire/Destin.java` | étapes mesurées, envies, moral, scènes « Mon destin » et « Ce qu'on dit de moi » |
| `histoire/Joueur.java` | FORMAT 7 ; `entrer(ville)`, `devant(lieu)` appelés par l'écran ; file d'annonces ; achats et lectures signalés au Destin |
| `histoire/Missions.java`, `histoire/Travail.java` | gains de renom, de réputation et d'étiquettes ; moral dans le rendement |
| `Habitants.java` | réactions des passants selon le renom, la réputation et le surnom |
| `FirstScreen.java` | `reperer()` : entrée dans un village, arrivée devant un lieu, renom du village transmis aux Habitants, annonces vers le bandeau |

Banc d'essai m25 : `CREATION=1 DESTIN=1`. Résultat (graine de création 5) :
- après 10 jours de missions : renom 44 à Ulanewen, réputation 75, surnom « Rend-Service » ;
- l'étape « Gagner l'estime de la ville » est franchie (focus en Éloquence) ;
- le renom a voyagé vers 5 villes voisines (1 à 3) ;
- des passants ont dit « Regarde, c'est Rend-Service. » ;
- l'envie « aller voir le menuisier » a été accomplie en marchant devant l'atelier ;
- la partie recharge à l'identique.

Reste à faire plus tard :
- ✔ réputation par groupe : guilde, royaume, peuple, brigands (lot F) ; les Maisons nobles (lot G2) ;
- ✔ surnoms et étiquettes propres à chaque ville (lot F) ;
- ✔ changer d'aspiration aux grands moments (lot F) ;
- ✔ envies tournées vers les proches (lot F) ;
- ✔ dettes, promesses, amour et noces (lot F, tenus par `Social`) ;
- ✔ renom porté par les bardes, les caravanes et les marchands escortés (lot F) ; les chansons sont sur l'étagère « Livres et chansons » et voyagent (lot G2).

### Passe « en action » — première partie faite le 2026-11-14

But : jouer le commerce, le travail et les missions dans le monde, pas seulement dans les fenêtres. Le texte reste pour les choix et les détails.

**Travail à la main.** Dans « Mon travail » (L), ou en se louant comme journalier, le choix « Travailler à la main » ferme la fenêtre :
- le personnage enchaîne les gestes du métier (faucher, traire, piocher, fendre une bûche, crier sa marchandise…) et s'éponge le front quand il est fatigué ;
- le temps défile vite : une heure de jeu en 4 secondes ;
- chaque quart d'heure de jeu compte comme une petite séance : salaire, apprentissage, fatigue, estime du maître, événements ;
- le bandeau montre les heures, la compétence, la fatigue et le gain ;
- bouger ou appuyer sur L arrête. La fatigue, la fermeture ou un événement (la caisse ouverte) arrêtent aussi. Un bilan s'affiche ensuite.
- Le rendement à la main (1) est meilleur que « faire passer le temps » (0,9).

**Commerce en personne.** M devant un marchand ouvre directement sa boutique ; ailleurs, la liste des marchands de la ville. Le marchand se tourne vers le joueur et montre sa marchandise ou salue. Le joueur donne en achetant, reçoit en vendant, lit en consultant le tableau, et tend l'objet en livrant une mission.

**Repère de mission.** Sous le bandeau, la première mission en cours indique où aller : « Mission : 51 poissons séchés pour un marchand de la place, vers la place à 15 m à l'ouest ». Dans une autre ville : « dans une autre ville ». Sur place : « tu y es (Q pour la rendre) ».

| Fichier | Rôle |
|---------|------|
| `histoire/Travail.java` | `Seance` et `seance()` communs ; `travailler()` (faire passer le temps) ; `commencerAMain`, `heureAMain`, `finirAMain`, `mainActif`, `mainArret`, `mainTexte`, `A_LA_MAIN`, `TRANCHE` |
| `histoire/Joueur.java` | `Scene.fermer` ; `gesteJoueur`, `reactionLieu`, `cibleVille`/`cibleLieu`/`cibleTexte` lus par l'écran ; `agentDevant()` pour M |
| `histoire/Missions.java` | `majCible()` met le repère à jour (accepter, livrer, échouer, ouvrir) ; gestes lire et donner |
| `Habitants.java` | `gestesJoueur(lieu)` : gestes de chaque métier ; `reagir(...)` : le travailleur d'un bâtiment se tourne vers le joueur |
| `FirstScreen.java` | `ACCELERATION_TRAVAIL` ; `travaillerAMain`, `arreterTravail`, `gestesJoueur` ; repère dans `reperer()` et `bandeau()` |

Banc d'essai m25 : `CREATION=1 MAIN=1`. Résultat : 2 h 15 aux champs en 10 secondes avec les gestes moissonner et traire. Fatigue 20 %, 1 cuivre. M devant l'auberge ouvre l'auberge. Le repère indique la place à 15 m à l'ouest.

### Passe « en action » — deuxième partie faite le 2026-11-14

**Dormir (touche Z).** Une petite fenêtre propose où dormir. Le choix « Dormir… » de « Mon travail » y mène aussi.
- **À l'auberge, une chambre** (`PRIX_CHAMBRE` = 8 cuivre, payés dans la caisse de l'auberge) : le personnage disparaît dans l'auberge. Au réveil, la fatigue tombe à 0 et le moral gagne `MORAL_CHAMBRE` (3).
- **À la belle étoile** : le personnage s'allonge (animation « dormir », bulle zzz). Il se repose moins bien : `FATIGUE_DEHORS` (1,5) par heure est rendue au réveil. Après 4 h ou plus, le moral perd `MORAL_DEHORS` (1).
- **Une sieste** de 2 h, dehors.
- Le temps file (`ACCELERATION_SOMMEIL` : une heure de jeu par seconde) jusqu'à 6 h du matin. Le bandeau dit « Tu dors à l'auberge · 3 h (bouge pour te lever) ».
- Bouger réveille avant l'heure : dans ce cas, pas de bonus de la chambre.

**Le tableau des annonces.** Chaque village a maintenant un tableau sur la place : deux poteaux, un panneau, un petit toit et des affiches. Il est posé du côté opposé au puits, face à la caméra. Il a son propre tirage au sort, donc le reste du village ne change pas. Devant lui, le bandeau dit « Devant toi : le tableau des annonces (Q pour le lire) », et Q ouvre directement les annonces.

| Fichier | Rôle |
|---------|------|
| `Batiments.java` | style `TABLEAU` (14) et son dessin dans `objet()` |
| `Villages.java` | `tableau(...)` le pose sur la place ; `Village.tableau` = son index |
| `histoire/Joueur.java` | `dormir()`, `coucher()`, `reveiller()`, `reveilPasse()`, `sommeil`, `couche`, `reveil`, `devantTableau` ; Q devant le tableau |
| `histoire/Missions.java` | le tableau se lit aussi devant le panneau, même sans étal |
| `FirstScreen.java` | `dormirAction()`, `ACCELERATION_SOMMEIL`, `TABLEAU_PROCHE`, ligne du bandeau |

Banc d'essai m25 : `CREATION=1 DORMIR=1`. Résultats :
- le tableau est posé (village 821, 12 m de la halle) ; Q devant lui ouvre « Le tableau de la place » ;
- une nuit à l'auberge de 21 h à 6 h passe en 9 secondes, avec la fatigue de 70 à 0 et le moral +3 ;
- une sieste dehors fait passer la fatigue de 50 à 43, avec le personnage allongé.

### Passe « en action » — troisième partie faite le 2026-11-14

**Le marchand vient servir.** Quand tu ouvres une boutique (M), un travailleur de l'atelier vient vers toi :
- celui qui est le plus près, à moins de `SERVIR_PORTEE` (40 m) ;
- s'il était à l'intérieur, il sort par la porte ;
- il marche jusqu'à `SERVIR_DISTANCE` (1,3 m) de toi, se tourne vers toi et montre sa marchandise ou te salue ;
- il reste un moment, puis reprend sa journée ;
- s'il dort, personne ne vient.

**Le destinataire t'attend.** Quand tu arrives à moins de `MISSION_ACCUEIL` (12 m) du lieu d'une mission :
- le destinataire sort et vient à ta rencontre ;
- le bandeau annonce « Syion t'attendait : Q pour lui remettre ta livraison » ;
- à la remise, il fait le geste de recevoir pendant que tu tends l'objet.

Le repère dit « tu y es » dès que tu es à portée du mur (`Joueur.PROCHE`, 4 m), et non plus du centre du bâtiment.

| Fichier | Rôle |
|---------|------|
| `Habitants.java` | `servir(village, bâtiment, joueur, geste)`, `nom(villageois)` ; dans `vivre()`, le geste fait à l'arrivée |
| `Villageois.java` | champ `servir` : geste à faire en arrivant près du joueur |
| `histoire/Joueur.java`, `histoire/Missions.java` | `reactionGeste` (« recevoir_objet » à la livraison) |
| `FirstScreen.java` | `gestesJoueur()` appelle `servir` ; `MISSION_ACCUEIL`, `missionAccueillie` ; repère au mur |

Banc d'essai m25 : `CREATION=1 SERVIR=1`. Résultats :
- livraison de 8 bouteilles de vin au tavernier : Syion sort, marche de 9 m à 1,5 m en 7 secondes et te salue ;
- à la remise, il fait « recevoir_objet » ;
- à l'auberge, Thandil était déjà à 0,7 m : il salue sur place.

Reste à faire plus tard :
- ✔ sa propre maison et son lit : faits (lot A) ;
- ✔ le destinataire d'une lettre, qui attend sur la place (lot G3) ;
- ✔ les marchands des étals de la place (lot G3).

### Phase 7 — faite le 2026-11-15 (première version)

**Racheter un atelier.** Devant un atelier, M ouvre la boutique, puis « Proposer de racheter l'atelier… ».
- Le maître donne ses ventes et son bénéfice par jour, puis son prix.
- Le prix comprend les murs (`BATIMENT` 1 500 cuivre, ×1,2 ou ×1,5 selon le métier), `JOURS_BENEFICE` (90) jours de bénéfice, et sa part du stock et de la caisse.
- La guilde demande un droit (`LICENCE`, 600 cuivre) à qui n'est pas maître du métier.
- S'il tient plusieurs ateliers, il en cède un, avec sa part d'ouvriers, de stock et de caisse. S'il n'en a qu'un, on reprend tout.
- Il refuse si son estime est trop basse (`ESTIME_MIN`) ou si ta réputation est mauvaise (`REPUTATION_MIN`).
- Si tu étais employé dans cet atelier, tu quittes ton poste.

**L'atelier du joueur dans l'économie.** C'est un agent d'`EconomieVille` avec `joueur = true`.
- Il achète ses matières, fabrique et vend aux foyers et aux autres ateliers, en concurrence avec ceux du pays.
- Différences avec un atelier de la ville :
  - il paie tous ses ouvriers chaque jour, même sans ouvrage ;
  - la ville ne le renfloue pas ;
  - il n'embauche que sur ordre, ou par l'intendant.
- Sans argent pour payer, un quart des ouvriers part chaque jour.
- Mal payés, ils partent un à un (`DEPART_MAL_PAYES`).
- Si le patron n'est pas passé depuis 7 jours (`PRESENCE_JOURS`) et qu'il n'y a pas d'intendant, l'ouvrage ralentit (`ABSENT_RENDEMENT` ×0,8) et la caisse fond (`VOL_ABSENT`, 1 % par jour).

**Gérer** (M devant son atelier, ou « Mes ateliers » dans le menu) :
- la caisse (mettre ou prendre de l'argent) ;
- embaucher ou renvoyer un ouvrier ;
- le salaire : bas ×0,8, comme ailleurs, ou généreux ×1,25 (rendement ×0,9 / 1 / 1,1) ;
- les prix : bas ×0,6, comme ailleurs, ou hauts ×1,5 sur la marge ;
- les recettes à arrêter ;
- l'intendant (`INTENDANT_SALAIRE` 12 cuivre par jour). Il surveille la caisse, garde de quoi payer `JOURS_PRUDENCE` (10) jours de salaires, renvoie sous 70 % d'ouvrage (`OUVRAGE_MIN`) et embauche au-dessus de 92 % (`OUVRAGE_MAX`). Plus son Intendance est haute, plus il rend l'atelier efficace.
- revendre à la ville (`REVENTE` 70 % de la valeur).

La fenêtre montre :
- les comptes depuis ta dernière visite : ventes, matières, salaires, argent disparu, bénéfice ;
- la réserve et ce qui manque ;
- un conseil quand il y a trop ou trop peu d'ouvriers.

Le bandeau dit « Devant toi : ta boulangerie (M pour le gérer) ».

**Aspiration « Fonder une entreprise ».** Ses étapes sont maintenant mesurées : un atelier, cinq employés, trois ateliers, une chaîne (un de tes ateliers fabrique la matière d'un autre), des ateliers dans trois villes.

| Fichier | Rôle |
|---------|------|
| `histoire/Entreprise.java` (nouveau) | les ateliers du joueur, rachat, gestion, intendant, revente, sauvegarde |
| `histoire/EconomieVille.java` | `joueur` et réglages sur l'Agent ; `produireJoueur`, `gererJoueur`, `racheter`, `rendre`, `valeur`, `ventesJour`, `beneficeJour` ; comptes ; sauvegarde (lieu négatif) |
| `histoire/Joueur.java` | FORMAT 8 ; menu « Mes ateliers » ; « Proposer de racheter » dans la boutique ; M devant son atelier ; `batimentsIci` |
| `histoire/Destin.java` | étapes 13 à 17 (entreprise) |
| `histoire/Missions.java`, `histoire/Travail.java` | pas de livraison ni d'embauche chez soi |
| `FirstScreen.java` | `batimentsIci`, `devantMien`, bandeau |

Ce que dit l'économie d'un village de 259 habitants (Ulanewen) :
- la boulangerie vend environ 183 cuivre par jour, pour 42 de bénéfice ;
- la taverne, le moulin et l'auberge rapportent aussi ;
- la forge, la tannerie et le potier ne vendent presque rien.

Il faut donc choisir son atelier selon ce que la ville achète.

Banc d'essai m25 : `CREATION=1 ENTREPRISE=1` (`DIAG=1` : ventes et bénéfice de chaque atelier). Résultat :
- boulangerie rachetée 91 argent 77 cuivre ;
- 14 ouvriers à 60 % d'ouvrage : petite perte, et le conseil s'affiche ;
- 20 jours d'absence sans intendant : 8 argent disparus ;
- avec l'intendante (Intendance 70), 11 ouvriers à 77 % et un bénéfice ;
- la partie recharge à l'identique, et la revente rend 87 argent.

Reste à faire plus tard :
- ✔ bâtir un atelier sur une ruine (lot D) ; ✔ sur un terrain libre (lot G5) ;
- ✔ des employés nommés avec fiabilité, loyauté, grève et vol (lot D) ; ce sont de vrais habitants de la Foule (lot G4) ;
- ✔ travailler soi-même dans son atelier (lot D) ;
- ✔ enchères après faillite (lot D) ;
- ✔ impôts du seigneur (lot D) ;
- ✔ caravanes entre ses ateliers de plusieurs villes (lot D).

### Phase 8 — faite le 2026-11-15 (première version) : recrutement et bande

On recrute de vrais villageois en leur parlant, dans le monde.

**Parler (touche X).** Devant un villageois (à 2,5 m au plus), X l'arrête : il se tourne vers toi et la fenêtre s'ouvre. Elle donne son nom, son atelier et son humeur envers toi (il t'aime bien, il te regarde de travers, tu lui fais peur…). Le Lien de Relations décide de cette humeur. On peut lui demander « Rejoins-moi », ouvrir « Ma bande », ou partir.

**Les motivations.** Chaque villageois a sept motivations, de 0 à 1 :
- l'argent ;
- une promesse ;
- l'amitié (son estime pour toi) ;
- une cause ;
- la gloire ;
- le besoin ;
- la peur (sa peur de toi).

Elles viennent de sa graine, donc la même personne a toujours les mêmes. Sa situation compte aussi : sans travail, il est plus sensible à l'argent et au besoin ; un village en ruines pousse au besoin.

**Convaincre.** Le joueur choisit un argument, un par motivation. Le score se calcule ainsi :
- 100 × la motivation touchée ;
- \+ la compétence / 3 : Commerce pour l'argent et le besoin, Commandement pour la gloire et la peur, Éloquence pour le reste ;
- \+ le palier de renom dans la ville × 7 (× 14 pour la gloire ; une promesse n'est crue qu'avec du renom) ;
- \+ l'estime × 30 ;
- − 20 s'il est marié, − 10 s'il a un poste ;
- ± 10 de hasard du jour.

Au-dessus de 60 (`SEUIL`), il accepte. Sinon il refuse. Si le refus est proche, il donne un indice : « Peut-être… mais pas comme ça. Elle semble sensible à la gloire. » On a deux essais par personne et par jour (`ESSAIS`). Une menace ratée lui laisse de la rancune.

**Les conditions.** Il demande une solde par semaine. Elle dépend de son goût pour l'argent et de sa valeur au combat ; l'amitié ou une cause la divisent par trois, et le besoin ou la peur l'annulent. Une promesse se choisit parmi : une part du butin, une terre, une place dans tes ateliers. Elle est enregistrée. On accepte (« Marché conclu ») ou non.

**Il te suit.** Le villageois quitte son village : `Habitants.retirer` le sort de la vie du village, et `partis` l'empêche d'être recréé. Son propre corps marche derrière toi, en rangs de trois. Il court s'il est à plus de 5 m. Il rejoint d'un coup s'il est à plus de 40 m ou coincé depuis 2 s. Au chargement, la bande est recréée à partir des graines, avec la même apparence.

**La vie de la bande** (`Bande.avancer`, toutes les 2 s de jeu) :
- **Chaque jour**, 2 pains par personne. On les prend d'abord dans le sac (pain, galettes), sinon on les achète au marché de la ville où l'on est, au vrai prix et dans le vrai stock. S'il en manque plus de la moitié, la bande a faim : moral − 8.
- **Chaque semaine**, la solde : moral + 2 si elle est payée, − 15 sinon.
- **Le moral** monte de 0,6 par jour bien nourri. Son plafond dépend du motif : l'amitié monte jusqu'à 100, une cause jusqu'à 91, le reste jusqu'à 79.
- **La désertion.** Sous 20 (35 pour ceux qui ont suivi par peur), il y a 25 % de chances par jour qu'il déserte. Le bandeau l'annonce et le carnet le note.

**Taille maximale** selon le palier de renom (`TAILLES`) :

| Palier | Taille |
|---|---|
| inconnu | 1 |
| quartier | 3 |
| ville | 20 |
| comté | 60 |
| royaume | 200 |
| légende | 500 |

On ajoute 1 par 25 points de Commandement.

**« Ma bande ».** Le menu principal y mène, et X devant un compagnon aussi. Chaque compagnon a une fiche : d'où il vient, pourquoi il t'a suivi, son moral, sa solde, sa valeur au combat (novice, solide, redoutable ; elle servira en phase 9), la promesse et les semaines dues. On peut lui donner de l'argent (moral + 8) ou le renvoyer.

| Fichier | Rôle |
|---|---|
| `histoire/Bande.java` (nouveau) | motivations, arguments, conditions, compagnons, pain, solde, moral, désertion, sauvegarde |
| `histoire/Joueur.java` | FORMAT 9 ; `bande()`, `bandeVue` (pour l'écran), `avancerBande()`, menu « Ma bande » |
| `Habitants.java` | `aQuiParler`, `retenir`, `retirer`, `partis` |
| `FirstScreen.java` | touche X (`parler`, `rencontre`), `suivreBande`, `Suiveur`, `ecart` |

Banc d'essai m25 : `CREATION=1 BANDE=1` (étapes 100 à 106). Résultat :
- trois villageoises et villageois recrutés à Ulanewen, par la gloire, l'amitié et une cause ;
- 4 s après, ils se tiennent à 1,2–1,7 m derrière le joueur ;
- un pas de côté de 8 m contre une maison : coincés, ils rejoignent après 2 s ;
- un saut de 120 m : ils rejoignent aussitôt ;
- 14 jours : 84 pains achetés au marché et 60 cuivre de solde, tous « dévoués » ;
- la partie recharge avec ses 3 compagnons ;
- sans argent ni pain, les trois désertent en 8 jours.

Reste à faire plus tard :
- ✔ un chemin calculé autour des maisons (lot G3) ;
- ✔ tenir les promesses, et la colère quand on ne les tient pas (lot B) ;
- ✔ des compagnons nommés qui parlent par l'IA locale (lot B) ;
- ✔ les déserteurs qui retournent vivre dans leur village (lot B) ;
- ✔ la bande au combat (phase 9).

### Phase 9 — faite le 2026-11-15 (première version) : combat de groupe et brigands

**Les repaires.** Autour d'une ville que l'on visite, une bande de brigands peut s'installer. La chance est de 12 % par jour en paix, 25 % en guerre et 50 % sous contrat. Après une bande détruite, il faut attendre 20 jours (4 sous contrat). La bande :
- campe dans une **clairière** (les arbres s'écartent : `Villages.clairieres`), 110 à 200 m au-delà du bord de la ville ;
- compte de 3 brigands à 3 + 4 + 3 × le palier de renom du joueur ;
- reçoit un renfort tous les 10 jours ;
- a un chef (le plus fort), des armes (bâton, dague, lance, hache, arc, masse, rarement une épée) et parfois un gambison ou une cuirasse de cuir ;
- garde une caisse : le butin.

À la création du repaire, la garde affiche une **mission de chasse** au tableau : 90 cuivre par brigand. Le bandeau guide jusqu'au campement (« leur campement à 201 m au sud »), puis, une fois la bande vaincue, jusqu'à la place pour toucher la prime.

**La rencontre, en action.** À 160 m, le campement se peuple : les brigands se chauffent, aiguisent leurs lames ou s'assoient, chacun armé et habillé selon sa fiche. À 16 m, ils se lèvent, défient le joueur (« Halte ! ») et la fenêtre s'ouvre :
- **attaquer** ;
- **les sommer de se rendre** : il faut une bande ou du renom. Le score vient du rapport des forces, du Commandement et du renom ; s'il manque, ils chargent ;
- **payer le passage** : 25 cuivre par brigand ;
- **passer son chemin**. S'ils sont bien plus forts, c'est « La bourse ou la vie ! » et il faut **tenter de fuir** (Athlétisme, Discrétion) ou se battre.

Une jauge dit le rapport des forces : « Vous avez l'avantage », « Ils sont bien trop forts pour vous ».

**La bataille** (`Combat`) est résolue automatiquement, comme dans Bannerlord, puis **rejouée à l'écran**.

*Les forces en présence :*
- **Le joueur :** 15 + compétence de son arme / 4 + 2 × Vigueur + 4 × force de l'arme.
- **Un compagnon :** sa valeur au combat + 4 × force de l'arme qu'on lui a donnée.
- **Un brigand :** sa force + son arme.
- **Le chef :** sa Tactique s'ajoute à son camp.

*Chaque tour :*
- **Les coups :** chacun frappe un adversaire. La chance de toucher vaut 0,5 + écart ÷ 150 (entre 15 et 85 %). Les dégâts dépendent de l'écart et diminuent de 5 % par point d'armure.
- **Les chutes :** à zéro de santé, on tombe à terre (75 % pour la bande, 60 % pour les brigands), sinon on meurt. Le joueur n'est qu'assommé.
- **La fuite :** chaque chute coûte du moral à son camp ; sous 22, les survivants fuient.

*À l'écran :* cri de guerre, puis chacun court vers son adversaire, sauf les archers qui tirent de loin. Ensuite viennent attaques, parades, esquives, coups reçus, chutes (étoiles d'assommé), morts et fuyards en débandade. Si les brigands se rendent, ils lèvent les mains.

**Après la bataille.**
- **Victoire :**
  - la caisse des brigands ;
  - une arme sur deux et une armure sur trois des vaincus vont au sac ;
  - renom (8 + 2 par brigand) et réputation +3 ;
  - l'étiquette qui donne le surnom « Fléau-des-Brigands » après 3 victoires ;
  - de l'expérience dans l'arme portée et en Tactique ;
  - pour les compagnons : moral +10 et valeur au combat +1 à +4 (entraînement).
- **Blessés et morts :** un compagnon à terre est blessé (santé 0,2). Il guérit de 0,15 par jour et ne combat pas sous 0,35. Un mort reste étendu sur place, le carnet le note et le moral des autres baisse.
- **Défaite :** assommé, le joueur perd la moitié de sa bourse et reprend conscience trois heures plus tard. Si la bande a fui, elle ne perd rien d'autre.
- **Les prisonniers** (les brigands à terre) :
  - **les ligoter pour la garde :** ils restent à genoux, ligotés. La garde les ramène et paie 150 cuivre par tête à la prochaine entrée en ville. Penche vers Loyal.
  - **les enrôler de force :** ils se relèvent et suivent la bande, par peur, sans solde, avec un moral bas. Penche vers Cruel.
  - **les relâcher :** ils s'enfuient. Penche vers Bon, et un peu de réputation.
  - **les exécuter :** renom, réputation −3, penche nettement vers Cruel. Les compagnons venus par amitié ou pour une cause le prennent mal.

**Équiper sa bande.** Dans la fiche d'un compagnon : « Lui donner une arme ou une armure… » prend dans le sac (sans toucher à ce que l'on porte) et rend l'ancienne pièce. Le compagnon porte alors visiblement son arme et son armure.

**Le contrat de mercenaire.** Avec une bande de deux personnes au moins, connu du quartier et pas mal vu, la garde propose au tableau un contrat de 20 jours :
- elle paie 10 cuivre par jour et par tête, versés chaque semaine ;
- les brigands viennent plus souvent pendant le contrat ;
- à la fin, s'il ne reste aucun repaire autour de la ville, la garde verse une prime et du renom ; sinon, perte de réputation.

| Fichier | Rôle |
|---|---|
| `histoire/Combat.java` (nouveau) | résolution, forces, équipement, liste des coups |
| `histoire/Brigands.java` (nouveau) | repaires, missions, rencontre, sommation, passage, fuite, après-bataille, prisonniers, contrat, sauvegarde |
| `histoire/Bande.java` | arme, armure, santé, victoires des compagnons ; équiper ; combattants ; après-bataille ; enrôlés |
| `histoire/Missions.java` | types CHASSE et MERCENAIRE, repère du campement |
| `histoire/Renommee.java` | étiquette BRIGANDS, surnom « Fléau-des-Brigands » |
| `histoire/Joueur.java` | FORMAT 10 ; `brigands()`, `bataille`, `sortPrisonniers`, `repairesVue`, `cibleCamp`, `santeJoueur` |
| `FirstScreen.java` | campements (`camps`), bataille rejouée (`lutte`), prisonniers, cadavres, repère, compagnons armés |
| `Villages.java`, `Vegetation.java` | clairières sans arbres |

Banc d'essai m25 : `CREATION=1 BRIGANDS=1` (étapes 110 à 117), `SEUL=1` pour la défaite. Résultat :
- quatre compagnons armés de lances contre 4 brigands : victoire en 49 coups, 1 brigand mort, 3 à terre, un compagnon grièvement blessé ;
- 3 prisonniers enrôlés, qui se relèvent et suivent : la bande passe à 7 ;
- la partie recharge à l'identique ;
- seul contre une bande forte : assommé en 6 coups, la moitié de la bourse volée, la mission reste ouverte.

Reste à faire plus tard :
- ✔ un feu de camp et des tentes au campement (lot C) ;
- ✔ des brigands qui attaquent sur les routes, loin des villes (lot C) ;
- ✔ le mercenariat pour un seigneur en guerre (phase 10, lot 10b) ;
- ✔ les familles des morts qui gardent rancune (nouveaux rivaux) (lot C), et le chef en fuite qui devient ennemi juré.

### Ce qui manquait — lot A, fait le 2026-11-15 : le corps et le logement (`histoire/Corps.java`)

**La faim.**
- Elle monte de 3,5 points par heure éveillé et de 1,2 en dormant.
- À 60, le bandeau affiche « Faim ».
- À 85, il affiche « Affame (U pour manger) », la marche ralentit (allure × 0,75) et le moral baisse.

**Manger.**
- **Ce qu'on a dans le sac :** dans la fiche d'un objet, « En manger » ou « En boire ». Chaque vivre a sa valeur : le pain retire 25 points.
- **Un repas chaud :** à l'auberge ou à la taverne, « Prendre un repas chaud » coûte 6 cuivre et remet la faim à zéro.

**Se loger.** Dans le menu, « Chercher un logement » propose la maison la plus proche (à moins de 6 m de sa porte) :
- **la louer :** 20 cuivre par semaine, plus 0,05 par habitant de la ville, payés chaque semaine. Après deux semaines impayées, on est expulsé.
- **l'acheter :** 1 500 cuivre, plus 4 par habitant.

Ses habitants déménagent et personne d'autre n'y habite plus.

Devant sa porte, le bandeau dit « ta maison (Z pour dormir, M pour ton coffre) » :
- **Z, dormir chez soi :** comme une chambre d'auberge, plus 5 de moral au réveil.
- **M, le coffre :** ranger et reprendre ses affaires.

| Fichier | Rôle |
|---|---|
| `histoire/Corps.java` (nouveau) | faim, repas, logement, loyer, coffre, sauvegarde |
| `histoire/Joueur.java` | FORMAT 11 ; manger depuis le sac, repas, « Ma maison », dormir chez soi |
| `Habitants.java` | les maisons du joueur se vident (`vider`, `maisonsJoueur`) |
| `FirstScreen.java` | maison la plus proche, devant sa porte, alertes de faim |

Essai m25 `CORPS=1` (étapes 130 à 135) :
- la faim passe de 10 à 94 en un jour (« affamé », allure 0,75) ;
- un pain la fait baisser de 25, un repas chaud la remet à 0 ;
- une maison louée 33 cuivre par semaine, son habitant parti ;
- une nuit chez soi : moral +5 ;
- le coffre et la partie rechargent à l'identique ;
- expulsion après deux semaines sans payer.

### Ce qui manquait — lot B, fait le 2026-11-15 : la bande

**Les promesses.**
- Une promesse faite au recrutement (part du butin, terre, place dans un atelier) se rappelle tous les 30 jours.
- **Part du butin :** elle est versée à chaque victoire (10 % du butin pour chacun, `Bande.partager`).
- **Place dans un atelier :** se tient depuis la fiche du compagnon, « Tenir ta promesse : lui donner sa place dans ton atelier » (+1 ouvrier, moral +6, réputation d'Honnête).
- **Renvoyer quelqu'un avant d'avoir tenu parole :** penche vers Fourbe, et il garde rancune (son Lien perd de l'estime).

**Ils parlent.** Toutes les 35 à 80 secondes, un compagnon dit une phrase qui dépend de sa situation :
- en route ;
- affamé ;
- pas payé ;
- content ;
- blessé ;
- promesse due.

Avec l'IA locale, la phrase est écrite par Qwen selon une consigne ; sinon, une phrase de secours est tirée au hasard. Le compagnon joue l'animation « parler ».

**Les déserteurs rentrent chez eux.** Un compagnon qui part (renvoyé, déserteur) marche vers son village pendant 15 secondes, puis disparaît. Il ne s'évapore plus sur place.

**Les obstacles.** Bloqué contre un mur, un compagnon tente un pas de côté, toujours du même côté pour lui.

| Fichier | Rôle |
|---|---|
| `histoire/Bande.java` | promesses (rappel, tenue, colère), partage du butin, paroles (`parole`) |
| `histoire/Brigands.java` | le butin passe par `Bande.partager` |
| `Habitants.java` | `direBande` : la phrase par l'IA, ou la phrase de secours |
| `FirstScreen.java` | `Partant` (déserteurs qui s'en vont), remarques de la bande, pas de côté |

### Ce qui manquait — lot C, fait le 2026-11-15 : les brigands

**Le campement.** Autour du feu de camp (`Batiments.FEU`), à 6,8 m, se dressent de 1 à 4 tentes de toile (`Batiments.TENTE`), une de plus tous les trois brigands. L'entrée de chaque tente est tournée vers le feu.

Ces objets sont posés après les bâtiments des villages (`Villages.poserObjets`). Ils sont donc dessinés, triés en profondeur et bloquent le passage comme eux. Les flammes alternent trois formes toutes les 0,17 s.

**Les embuscades sur les routes.** Loin des villes (aucun village à moins de 400 m), le jeu tire au sort tous les 500 m parcourus :
- 8 % par tirage en paix, 15 % en guerre ;
- 10 % de plus quand l'ennemi juré rôde.

La bande (2 à 4 brigands, plus selon le renom) surgit à 13 m devant le joueur, sur la terre ferme : « Embuscade ! ». S'ils sont au moins aussi forts, c'est « La bourse ou la vie ! ». S'ils sont nettement plus faibles, ils hésitent et s'écartent.

Payée, fuie, gagnée ou perdue, l'embuscade se disperse : les brigands s'en vont en courant. Elle ne survit pas à la sauvegarde.

**L'ennemi juré.** Quand le chef d'une bande défaite s'enfuit, il devient l'ennemi juré : « Sydor s'enfuit en jurant de se venger. On l'appellera désormais Sydor le Revenant. » Il garde son corps, son arme et sa force.

- **Son retour :** 6 à 14 jours plus tard, il prend la tête de la prochaine bande (60 % des chances, 35 % pour une embuscade). Il a plus de force par niveau et autant de brigands en plus que son niveau.
- **La rencontre :** il te reconnaît (« Je t'attendais. Cette fois, c'est toi qui vas fuir. »), refuse l'argent et se laisse moins facilement sommer.
- **S'il s'échappe encore :** il monte de niveau, et devient « l'Increvable » au niveau 3.
- **Le tuer, le livrer, l'exécuter ou l'enrôler :** cela met fin à la rivalité et rapporte 12 de renom.
- **Le relâcher :** il reviendra.

**Les familles des morts.**
- 40 % des brigands tués au combat ont de la famille dans la ville ; c'est 60 % pour les exécutés.
- En ville, un habitant, toujours le même pour un mort donné, se souvient : « a tué un de mes proches ».
- Pour un exécuté, et une fois sur deux sinon, il jure vengeance et devient un rival du système Nemesis (« le Vengeur », « la Vengeresse ») qui viendra t'affronter.

| Fichier | Rôle |
|---|---|
| `histoire/Brigands.java` | embuscades (`embuscade`, `disperser`), ennemi juré (`fuiteChef`, `finRival`, `rivalComplet`), deuils, sauvegarde |
| `histoire/Joueur.java` | FORMAT 12 ; `deuilsVue` |
| `Batiments.java` | styles `TENTE` et `FEU` |
| `Villages.java` | `poserObjets` : objets hors des villes, à la suite des bâtiments |
| `FirstScreen.java` | tirage des embuscades (`route`), placement devant le joueur, mobilier, fuite des dispersés, deuils en ville |

Essai m25 `CAMP=1` (étapes 150 à 156) :
- le campement : un feu et deux tentes ;
- Sydor s'enfuit et devient « Sydor le Revenant » ;
- 15 jours plus tard, il revient à la tête d'une bande de 5, avec le même corps, et refuse de se laisser payer ;
- une embuscade de 3 brigands surgit devant le joueur ; payés, ils s'enfuient ;
- le deuil fait naître « Ulamir le Vengeur » ;
- la partie recharge avec l'ennemi juré et sans l'embuscade.

### La palette agrandie — faite le 2026-11-15

`palettes/sol.png` passe de 16 à 24 rangées de 8 teintes (du plus sombre au plus clair). Les 16 premières ne changent pas. Les 8 nouvelles sont pensées pour le dark fantasy : désaturées, ombres froides, lumières chaudes.

| Rangée | Constante (`Batiments`) | Usage |
|---|---|---|
| 16 | `R_FEU` | feu, braises, torches (la seule rangée vive : une source de lumière) |
| 17 | `R_SANG` | sang, vin, bannières, toiles teintes |
| 18 | `R_OR` | or terni, laiton, cierges |
| 19 | `R_POURPRE` | noblesse, magie sombre |
| 20 | `R_INDIGO` | teintures, nuit |
| 21 | `R_SPECTRE` | lueurs spectrales, magie froide |
| 22 | `R_BILE` | poison, marais, maladie |
| 23 | `R_OS` | os, parchemin, cire |

Ce qui a changé dans le code :
- `Batiments.RANGEES` (24) remplace le 16 écrit en dur dans `Batiments` et `Peintre` ;
- les shaders `arbre`, `relief` et `tuile` lisent la texture sur 24 rangées ;
- le feu de camp prend la rangée du feu, et les tentes peuvent être rouge sang ou indigo.

### Ce qui manquait — lot D, fait le 2026-11-15 : l'économie

**Les commandes** (`histoire/Commandes.java`). Quand un atelier n'a plus en réserve un objet qu'il fabrique (une épée dans une petite ville), la boutique propose « Commander : épée ».
- Le prix est celui de l'atelier, plus 15 %. On paie 30 % d'acompte tout de suite.
- Le délai va de 2 à 12 jours : plus long pour un objet cher, plus court quand l'atelier a des bras.
- La commande compte dans la demande de l'atelier : il achète ses matières et se met à l'ouvrage.
- Le jour venu, le bandeau l'annonce. On revient, « Retirer ta commande », et on paie le reste. On peut aussi annuler, mais l'acompte est perdu.

**Les ouvriers ont un nom** (`histoire/Affaires.java`). Chaque ouvrier d'un atelier du joueur a :
- une **fiabilité** (de 0,7 à 1,3), qui multiplie l'ouvrage ;
- une **loyauté** (de 0 à 100) ;
- une **honnêteté** (de 0 à 1), cachée.

La loyauté tend vers 30, 55 ou 75 selon le salaire (bas, comme ailleurs, généreux). Elle baisse quand on ne paie pas et quand le patron ne passe pas.
- **Le vol.** Un ouvrier peu honnête puise dans la caisse. Il vole deux fois plus s'il est mécontent, deux fois moins s'il y a un intendant. Une fois sur deux, on sait qui : « Tu soupçonnes Thawen ».
- **La grève.** Quand la loyauté moyenne passe sous 25 (avec au moins 3 ouvriers), ils posent les outils. Rien ne se fait, personne n'est payé, et de temps en temps un gréviste s'en va. Au-dessus de 45, ils reprennent.
- **Ce que peut faire le patron.** Dans « Tes ouvriers… », il donne une prime (20 cuivre, loyauté +15) ou renvoie quelqu'un. Renvoyer le voleur se comprend ; renvoyer sans raison inquiète les autres.
- **Face aux grévistes.** Il peut céder (salaire généreux et une prime), leur parler (Éloquence), les renvoyer tous (on le dira « briseur de grève ») ou tenir bon.

**Les impôts du seigneur.** Chaque semaine, chaque atelier paie la **taille** (10 cuivre) et la **dîme** (8 % de ses ventes de la semaine). En guerre, c'est une fois et demie plus. La somme apparaît dans « Gérer l'atelier ».

**Travailler soi-même.** Devant son atelier (touche M ou L), « Travailler toi-même à l'atelier » lance le travail à la main au rang **patron** (`Travail.PATRON`). Le patron ne touche pas de salaire. En échange, l'ouvrage avance, il apprend le métier, et il compte comme présent.

**Les enchères.** Un atelier peut être vendu aux enchères dans la ville où l'on est, au plus une fois tous les 60 jours. Deux raisons :
- il est en faillite (sa caisse est sous son crédit) ;
- son maître est mort sans héritier (une chance sur 120 par jour).

Comment se passe la vente :
- Le crieur l'annonce, et la vente s'affiche au **tableau de la place**.
- La mise à prix est de 45 % de la valeur de l'atelier. Un marchand rival surenchérit jusqu'à un plafond secret, entre 60 et 95 % de cette valeur.
- Au bout de 6 jours, l'atelier est adjugé à qui mène. Ses dettes ne suivent pas. Si l'on n'est pas maître du métier, il faut aussi payer la licence.
- Gagner l'enchère sans pouvoir payer coûte de la réputation.

**Bâtir sur une ruine.** Devant une ruine du village, le bandeau dit « M pour y bâtir un atelier ».
- On choisit un métier qu'on n'a pas déjà en ville. Le prix est de 80 % des murs, plus la licence.
- Le chantier dure 12 jours.
- L'atelier neuf arrive sans ouvriers et la caisse vide. À l'écran, il prend la place de la ruine, avec la même taille.

**Les caravanes entre ses ateliers.**
- Quand un de ses ateliers fabrique une matière qu'un autre achète, dans une autre ville à moins de 600 km, « Gérer l'atelier » propose « Caravanes… ».
- Chaque semaine, la caravane part avec ce dont l'autre atelier a besoin.
- Le transport (0,4 % du prix par kilomètre et par pièce) se paie sur la caisse de l'atelier qui reçoit.
- 4 % des caravanes sont pillées, 8 % en guerre. Les deux ateliers en reçoivent la nouvelle.

**Les caravanes entre les villes.**
- Chaque semaine, un atelier qui a plus que sa réserve envoie le surplus à une ville proche (moins de 250 km) qui fait venir ce bien de loin, plus cher.
- En entrant dans la ville, on l'apprend : « Une caravane d'Elavenor est arrivée avec 8 bâtons. »
- Seules les villes déjà visitées y prennent part : ce sont les seules dont l'économie tourne.

| Fichier | Rôle |
|---|---|
| `histoire/Commandes.java` (nouveau) | commandes : proposer, payer l'acompte, annoncer, retirer, sauvegarde |
| `histoire/Affaires.java` (nouveau) | ouvriers nommés, vols, grève, impôts, enchères, chantiers sur ruine, caravanes (les siennes et celles du pays), sauvegarde |
| `histoire/Entreprise.java` | l'atelier garde ses ouvriers, sa grève, ses nouvelles ; `prendre` (atelier gagné ou bâti) ; ce que le lot D ajoute à « Gérer l'atelier » |
| `histoire/EconomieVille.java` | `fiabilite` et `greve` de l'atelier du joueur |
| `histoire/Travail.java` | rang `PATRON` |
| `histoire/Joueur.java` | FORMAT 13 ; `agentIci` trouve son propre atelier ; « Commander », « Travailler toi-même », la ruine sous M ; `ruineProche`, `batisVue` |
| `histoire/Missions.java` | les enchères au tableau |
| `histoire/Marche.java`, `histoire/Population.java` | `economies()`, `km()` |
| `Villages.java` | `rafraichir()` |
| `FirstScreen.java` | la ruine la plus proche, les ateliers bâtis dessinés à la place des ruines, le bandeau |

Essai m25 `AFFAIRES=1` (étapes 170 à 174) :
- **commande :** un jambon commandé (32 cuivre, acompte 10), annoncé au bout de 2 jours, retiré en payant les 22 cuivre restants ;
- **ouvriers :** la boulangerie rachetée passe à 15 ouvriers payés bas, puis reste 40 jours sans patron. Résultat : loyauté 26, un vol (« Tu soupçonnes Thawen ») et 7 argent d'impôts. La grève éclate ensuite ; elle prend fin quand le patron cède (loyauté 58) ;
- **travailler soi-même :** 4 h de travail en patron ;
- **enchère :** une boucherie dont le maître est mort sans héritier, gagnée à 45 argent après trois mises contre le marchand Saendil ;
- **ruine :** un moulin bâti en 12 jours, qui remplace la ruine à l'écran ;
- **caravanes :** une caravane de peaux, de la boucherie vers une tannerie d'Elavenor (19 km), est pillée. Puis cinq convois du pays arrivent d'Elavenor ;
- **sauvegarde :** la partie recharge avec ses 4 ateliers, leurs ouvriers, la caravane et l'atelier bâti.

### Ce qui manquait — lot E, fait le 2026-11-15 : les missions

Neuf nouvelles sortes de missions (`histoire/Quetes.java`). Elles se jouent dans le monde : on va voir les gens et on leur parle (touche X), on sort des murs la nuit, on force une caisse. Le repère de l'écran montre où aller. Quand il faut parler à quelqu'un, il désigne la personne : « Laelas à 34 m au nord-est », puis « Laelas est là (X pour lui parler) ».

**Au tableau de la place**, une fois par jour, selon ce qui se passe en ville :
- **Escorte** (35 % des jours, s'il y a une route ouverte). Un marchand et sa mule partent pour une ville voisine. Prime : 40 cuivre plus 2,2 par km. Une fois la mission acceptée, le marchand suit le joueur comme un membre de la bande. Sur la route, les embuscades sont plus fréquentes (+25 % à chaque tirage). La mission se rend sur la place de la ville d'arrivée.
- **Loups** (20 %, s'il y a des fermes). Une meute de 2 à 5 loups attaque les troupeaux. Il faut sortir des murs la nuit (de 20 h à 5 h). Le bandeau annonce alors : « les loups sont là ! (Q pour les affronter) ». La fenêtre des missions propose « Affronter les loups ! » en premier. Le combat est le même que contre les brigands, sans rejouer la bataille à l'écran. Un loup a une force de 10 à 19 (un brigand : 15 à 40) et fuit vite. Après la victoire, on garde les peaux et on va l'annoncer sur la place. Depuis le lot G3, on voit les loups rôder autour de soi.
- **Enquête** (20 %). On a volé dans un atelier, et le coupable est un habitant. On interroge les gens avec X, « L'interroger ». En moyenne une personne sur trois a vu quelque chose, et la sixième parle forcément. Le premier témoin dit « un homme » ou « une femme ». Le second donne le nom. On va alors accuser le coupable. La réussite dépend de l'Éloquence, de la Présence et du nombre de témoins. En cas de succès, l'atelier retrouve son argent, le coupable garde de la rancune envers le joueur, et la réputation monte. En cas d'échec, il nie tout et il faut un témoin de plus.
- **Médiation** (15 %). Deux voisins se querellent. On écoute la version de chacun, puis on propose un arrangement. On peut demander que chacun reconnaisse ses torts (jet d'Éloquence), donner raison à l'un (l'autre t'en veut) ou payer soi-même 30 cuivre de dédommagement.
- **Dette** (20 %). Un artisan n'est pas payé, et on va réclamer la somme au débiteur.
  - « Exiger » est un jet de Présence, de Commandement et de renom. En cas de refus, il faut revenir le lendemain.
  - « Menacer » fait toujours payer, mais le débiteur prend peur et garde de la rancune, et la réputation baisse.
  - On rapporte ensuite l'argent à l'atelier, qui laisse sa part au joueur (un quart).
- **Chantier** (30 %, s'il y a des ruines en ville). Le bourgmestre veut relever une maison en ruine. On lui apporte 15 à 30 planches sur la place. La maison se relève alors à l'écran, à la place de la ruine.

**Les demandes des villageois.** Un villageois qui t'estime (Lien d'estime de 0,3 ou plus) peut répondre à « « Tu as besoin de quelque chose ? » ». C'est le cas un jour sur trois environ, et toujours le même jour pour la même personne. Il demande un objet : bière, fromage, cape, couteau, soins… On le lui apporte (X, « Lui donner »). Il paie une fois et demie le prix, plus 10 cuivre, et son estime monte. On ne peut avoir que deux demandes en cours.

**Le tavernier louche.** À la taverne, « Parler à voix basse au tavernier… ». Il refuse si ta réputation est bonne (au-dessus de 20) et ta Discrétion faible (sous 25). Sinon, il propose deux affaires :
- **Contrebande.** Il te confie du sel ou des épices à passer jusqu'à la taverne d'une ville voisine. À la porte de la ville, la garde peut fouiller ton sac : 35 % de risque, moins la Discrétion et la Ruse. Si elle trouve la marchandise, elle la saisit, tu paies 1 argent d'amende et on te dit voleur.
- **Vol.** Il désigne l'atelier le plus riche de la ville. La nuit, la boutique propose « Forcer la caisse » (jet de Discrétion, de Ruse et d'Adresse). En cas de succès, tu prends un quart de la caisse (au plus 3 argent) et tu rapportes la moitié au tavernier. En cas d'échec, tu paies l'amende, la réputation baisse de 8 et la mission est manquée.

| Fichier | Rôle |
|---|---|
| `histoire/Quetes.java` (nouveau) | les neuf missions : naissance au tableau, textes, repères, loups, scènes avec les villageois (X), tavernier, maisons relevées, sauvegarde |
| `histoire/Missions.java` | types 5 à 13 ; une mission garde ses personnes (graine, nom), son avancement et les gens déjà interrogés ; elle passe la main à Quetes |
| `histoire/Joueur.java` | FORMAT 14 ; `cibleGraine`, les gens et les ruines du village publiés par l'écran, `horsLesMurs()` ; le marchand escorté suit avec la bande |
| `histoire/Bande.java` | parler à un villageois propose les choix des missions |
| `histoire/Brigands.java` | plus d'embuscades pendant une escorte |
| `histoire/Affaires.java` | les maisons relevées s'ajoutent aux ateliers bâtis |
| `FirstScreen.java` | repère vers une personne (-3) ou hors des murs (-4) ; publie les adultes et les ruines du village ; une maison relevée est redessinée en maison |

Essai m25 `QUETES=1` (étapes 190 à 192), à Ulanewen :
- **escorte :** Aeia, la marchande, suit le joueur à l'écran, puis paie 3 argent 46 ;
- **enquête :** 7 personnes interrogées, 2 témoins, Arawyn accusée avec succès ;
- **médiation :** Miia et Aralas apaisés contre 30 cuivre ;
- **dette :** Laelas menacé, l'argent rendu au meunier, 30 cuivre de part ;
- **demande :** Araion voulait une cape ; son estime passe de 0,5 à 0,65 ;
- **chantier :** 17 planches livrées, et la ruine devient une maison à l'écran ;
- **contrebande :** la garde n'a pas fouillé ;
- **vol :** au premier essai, la caisse forcée a raté (amende) ; avec plus de Discrétion, 3 argent volés et la moitié rendue ;
- **loups :** au premier essai, trop forts (ils ont été affaiblis depuis) ; ensuite, un loup tué et les autres en fuite ;
- **sauvegarde :** la partie recharge ses missions, avec leurs personnes et leurs témoins.

### Ce qui manquait — lot F, fait le 2026-11-15 : la vie sociale

Le lot est dans `histoire/Social.java` (nouveau) et `histoire/Renommee.java`. Tout se joue en parlant aux gens (touche X).

**La famille se croise en ville.** Chaque proche vivant de la création (parents, frères et sœurs, ami, maître…) qui habite la ville est incarné par un habitant du village, toujours le même et du bon sexe. Cet habitant porte le nom du proche et l'estime qu'il te porte.
- En lui parlant, on lit « Oririel, ta mère ».
- Choix : « « Comment vas-tu ? » » (une fois par jour : moral +3, estime +2) et « Lui donner un peu d'argent » (20 cuivre, estime +5).
- Le conjoint épousé en jeu devient « ta femme » ou « ton mari ».

**Les envies tournées vers les proches.** Le Destin peut tirer « Boire un coup avec Garin » ou « Prendre des nouvelles de ta mère, Oririel ». On l'accomplit en parlant à la personne.
- « « Viens boire un coup avec moi. » » coûte une tournée (6 cuivre) et 1 heure.
- Estime +0,08 et moral +4. Cela compte aussi pour l'envie « boire une bière ».

**Les dettes et les promesses.**
- **Emprunter.** On peut emprunter à qui t'estime (0,4 ou plus) ou à un proche. Il prête 20 cuivre plus 120 × son estime ; un proche prête le double. C'est à rendre dans 10 jours : c'est une promesse.
- **Prêter.** Un villageois qui t'apprécie est parfois à court (« Tu as l'air soucieux »). Si on lui prête, son estime monte. Sept sur dix rendent à temps ; les autres, il faut aller les réclamer (jet de Présence et d'Éloquence).
- **Les promesses** (un emprunt, une demande acceptée au lot E). Tenue, une promesse rapproche. Rompue, elle coûte −0,25 d'estime, de la rancune et −3 de réputation, et compte parmi les promesses manquées (« Promet-Tout »).

**L'amour.** Avec un habitant qui t'estime (0,15 ou plus), qui n'est ni marié ni de ta famille :
1. **Faire la cour**, une fois par jour : jet d'Éloquence, de Présence et d'estime.
2. **Offrir un présent** tiré du sac : plus il est cher, plus il touche.
3. **Déclarer sa flamme** quand l'amour atteint 0,7.
4. **Demander la main** après 5 jours d'amour, quand l'amour atteint 0,85.
5. **Célébrer les noces** : une fête de 2 argent sur la place, du renom, et le moral au plus haut.

On ne peut aimer qu'une personne à la fois. L'amour s'use quand on ne se voit plus pendant 10 jours. Les étapes « Rencontrer quelqu'un », « Lui faire la cour », « Se marier » et « L'épouser » se franchissent désormais.

**Changer d'aspiration aux grands moments.** Un grand moment ouvre le choix « Changer d'aspiration » dans « Mon destin ». C'est le cas pour un nouveau palier de renom à partir de « connu en ville », la maîtrise d'un métier, les noces ou une aspiration accomplie. L'ancienne aspiration est notée dans le récit.

**La réputation par groupe** (de −100 à 100, elle s'efface lentement) :
- **Le royaume** de la ville prend 30 % de chaque gain ou perte de réputation, **le peuple** 20 %. Un vol coûte −3 auprès du royaume ; vaincre des brigands rapporte +2.
- Un habitant qui ne te connaît pas encore juge sur la réputation de sa ville, et un peu sur celles du royaume et du peuple.
- Un royaume qui te tient à l'œil fouille plus volontiers à la porte (contrebande).
- **La guilde** d'un métier : +0,5 par jour de travail, +15 à la maîtrise. Elle fait un prix sur la licence, jusqu'à moitié prix.
- **Les brigands** : +3 par vol, −6 par victoire contre eux. Respecté (30 et plus), on est attaqué deux fois moins sur les routes ; haï (−30 et moins), 30 % de plus.
- Tout s'affiche dans « Ce qu'on dit de moi ».

**Un surnom par ville.** Les actes marquants sont comptés aussi ville par ville. Une ville te donne le surnom de ce que tu y as fait, et il en faut deux fois moins qu'ailleurs : 4 missions suffisent pour « Rend-Service ». Sinon, elle reprend ton surnom général, mais seulement si ton renom y est arrivé (connu en ville). Ainsi, on peut être « Main-Leste » ici et « Rend-Service » là-bas.

**Les bardes et les voyageurs.**
- À la taverne ou à l'auberge, « Écouter le barde ». Si l'on est connu ici, il chante tes exploits, tirés de ton carnet (missions, loups, brigands, maîtrise, noces). Sinon, il chante une vieille complainte.
- Pour 20 cuivre, il porte ta chanson sur les routes pendant 30 jours. Ton renom part alors de cette ville deux fois plus vite, et la chanson est notée dans le carnet.
- Les voyageurs portent aussi ton renom : une caravane du pays fait arriver 10 % de l'écart de renom, et le marchand que tu as escorté 20 %.

| Fichier | Rôle |
|---|---|
| `histoire/Social.java` (nouveau) | famille en ville, envies vers les proches, dettes, promesses, amour et noces, bardes, grands moments, sauvegarde |
| `histoire/Renommee.java` | réputation par groupe, surnom par ville, licence de guilde, facteur des brigands, bardes, renom porté par les voyageurs |
| `histoire/Destin.java` | envie `PROCHE`, étapes d'amour mesurables, grand moment quand l'aspiration est accomplie, « Changer d'aspiration », groupes et surnom d'ici dans « Ce qu'on dit de moi » |
| `histoire/Joueur.java` | FORMAT 15 ; `social()` ; lignes d'amours, de dettes et de promesses dans « Ma famille et mes proches » ; le barde dans les boutiques |
| `histoire/Bande.java` | « X, ta mère » ; les choix sociaux quand on parle |
| `histoire/Quetes.java` | une demande acceptée est une promesse ; l'escorte porte ton renom ; la garde selon le royaume |
| `histoire/Travail.java`, `Entreprise.java`, `Affaires.java`, `Brigands.java` | la guilde, les licences, les caravanes qui portent le renom, les embuscades selon les brigands |
| `Habitants.java`, `FirstScreen.java` | noms des proches incarnés, leur estime, réputation et surnom d'ici pour les passants |

Essai m25 `SOCIAL=1` (étapes 200 à 202), à Ulanewen :
- **famille :** 13 proches croisés en ville (« Oririel, ta mère », « Caeion, ton père »…) ;
- **emprunt :** 80 cuivre empruntés à Elamir et rendus en retard ; la promesse est rompue, son estime tombe de 0,5 à 0,25 ;
- **prêt :** 57 cuivre prêtés à Oriion, rendus à temps ;
- **amour :** cour à Vaewyn (avec deux capes en présent), flamme déclarée le 4ᵉ jour, mariage le 10ᵉ. Le grand moment ouvre le changement d'aspiration : « Diriger sa ville » devient « Devenir noble » ;
- **guilde :** licence de boulangerie à 480 au lieu de 600 ;
- **barde :** il chante les noces, puis porte la chanson sur les routes ;
- **sauvegarde :** la partie recharge le mariage, les surnoms et les groupes.

### Phase 10 — faite le 2026-11-15 (première version) : la noblesse et l'armée

Dans le monde, seuls les royaumes ont un chef : le roi ou la reine, de sa Maison. Chaque comté est une région du royaume. Le seigneur que l'on sert est donc la couronne. Son château est dans la capitale, et l'on y entre avec la touche M devant le château.

#### Lot 10a : la noblesse (`histoire/Noblesse.java`)

**La cour.** Elle affiche le souverain et la Maison qui règne (« la dame Vaewyn, de la lignée de Tyndil »), puis la **faveur** dont on y jouit, de −100 à 100.

**Le service.**
- **Entrer au service.** Il faut être connu en ville (renom 50) pour offrir ses services. On ne sert qu'une couronne à la fois ; en changer coûte de la faveur.
- **Les tâches.** Le souverain confie une tâche, à faire en 20 jours :
  - **porter une lettre** au bailli d'une ville du royaume : faveur +8 ;
  - **lever l'impôt** d'une ville, puis le rapporter : faveur +12. On peut aussi le garder pour soi, mais alors la faveur tombe de 40, la réputation auprès du royaume baisse et l'on devient plus fourbe ;
  - **chasser les brigands** des routes du royaume : faveur +15. C'est un exploit.
- **L'échec.** Une tâche ratée coûte 10 de faveur.

**Devenir noble.** Il y a trois voies :
- **l'adoubement** : avec 60 de faveur, un renom de 50 et un exploit. On reçoit une épée et le titre de chevalier ;
- **l'achat d'un titre** : 300 argent, ou moitié prix quand le trésor du royaume est presque vide ;
- **le mariage dans une Maison noble** : avec 40 de faveur et une dot de 50 argent. La cour propose un parti libre d'une Maison du royaume. S'il n'y en a pas, elle propose un cousin ou une cousine du souverain, qui entre alors dans le monde.

Chaque voie donne une chevalière, du renom et un grand moment.

**Le tournoi.** Une fois par saison, pour 50 cuivre, on affronte trois chevaliers de plus en plus forts. Le vainqueur reçoit 6 argent, du renom et de la faveur.

**Le fief.** Noble, on demande un fief au souverain (50 de faveur, puis 20 de plus pour chaque nouveau fief). Il donne le plus petit village libre du royaume. On le gouverne à la mairie du village (touche M) ou depuis « Mes fiefs ».
- **L'impôt.** Il est bas, juste ou lourd : 2, 4 ou 7 cuivre par habitant et par saison. Un impôt bas fait monter la réputation, un impôt lourd la fait baisser.
- **L'aide.** Chaque saison, 20 % de l'impôt va au suzerain.
- **La justice.** Chaque saison, une affaire attend la sentence du seigneur : un voleur, un champ disputé, un meunier tricheur, une demande de remise d'impôt. Chaque sentence touche la réputation, l'argent et les traits. Une affaire laissée sans réponse fait murmurer le village.
- **Les gardes.** Jusqu'à 12, payés 3 cuivre par jour. Ils font baisser les embuscades autour du fief.
- **Les travaux.**
  - Une palissade : deux fois moins de brigands.
  - Un grenier seigneurial : +20 % d'impôt.
  - Une chapelle : de la réputation chaque saison.
- **La défense.** Vaincre des brigands près de son fief compte comme « Défendre un fief ».

**Le Destin.** Les étapes suivantes se mesurent maintenant : entrer au service, être adoubé (ou acheter un titre, ou épouser une Maison), recevoir un fief, gagner un tournoi, défendre un fief, remplir trois contrats pour des seigneurs, qu'un enfant devienne noble.

#### Lot 10b : l'armée, la guerre et la couronne (`histoire/Armee.java`)

**Les troupes.** En plus des compagnons nommés de la bande, on a des soldats.

| Soldat | Recrue | Solde par jour | Force |
|---|---|---|---|
| Piquier | 30 cuivre | 3 cuivre | 24 |
| Archer | 40 cuivre | 4 cuivre | 25 |
| Cavalier | 120 cuivre | 8 cuivre | 34 |

- **Recruter.** À la caserne (touche M devant la caserne), une ville fournit un homme pour 10 habitants chaque saison.
- **La levée.** Dans son fief, on lève des piquiers sans payer de recrue (un homme pour 15 habitants), mais la réputation du village baisse.
- **La désertion.** Sans solde pendant trois jours, 15 % des soldats désertent chaque jour.

**Les stades.**

| Stade | Hommes au plus | Ce qu'il faut |
|---|---|---|
| Bande | 20 | — |
| Compagnie | 60 | une base (une maison ou un fief) et un lieutenant (un compagnon) |
| Troupe | 200 | un renom de 150 et deux officiers (deux compagnons) |
| Armée | 1 000 | être noble et tenir un fief |

**La bataille.** Elle se résout comme le combat de groupe de la phase 9. Au-delà de 50 combattants par camp, chaque combattant vaut plusieurs hommes (« chaque combattant en vaut 4 »).
- La Tactique et le Commandement aident.
- Les pertes se comptent par type de soldat.
- Les compagnons nommés sont blessés ou tués comme en phase 9.
- On peut tomber à la bataille et y mourir ; l'héritier reprend alors.

**La guerre.** Quand un royaume est en guerre :
- **l'ost** : au service de sa couronne, on rejoint l'ost contre l'ennemi. Une victoire donne +10 de faveur, du renom et du butin ;
- **le mercenariat** : à la cour d'un autre royaume en guerre, on loue sa compagnie pour 30 jours, à 4 cuivre par homme et par jour. Un contrat qui finit avec au moins une victoire compte pour l'étape « contrats pour des seigneurs » ;
- **les sièges** : dans « Mon armée », on marche sur les trois plus petites villes de l'ennemi ou sur sa capitale. Une ville prise est pillée. Si l'on est noble, on la garde pour fief.

**La couronne.**
- **Rallier des vassaux.** Il faut être noble et avoir un fief. On rallie des nobles à sa cause pour 20 argent chacun ; la réussite dépend de l'Éloquence et du renom.
- **Réclamer le trône.** Avec trois vassaux et 100 hommes, on réclame le trône et l'on affronte l'armée royale. Elle est plus faible :
  - si l'on a épousé la Maison régnante (par le sang) ;
  - si l'on est célèbre dans la capitale (par le peuple) ;
  - et pour chaque vassal.
- **L'issue.**
  - **La victoire.** Le joueur devient le chef du royaume dans le monde. La réplique « Ici, c'est moi qui gouverne » s'affiche, et le joueur est appelé « roi de Naevesae ».
  - **La défaite.** On perd ses fiefs et presque toute sa faveur.
- **Régner.** Le roi lève l'impôt royal chaque saison (un cuivre par habitant du royaume). Il déclare la guerre aux royaumes de son peuple et fait la paix.
- **Unifier son peuple.** Prendre la capitale d'un ennemi de son peuple, quand on est déjà roi, en prend aussi la couronne.
- **Ce qui dure.** La couronne passe à l'héritier et revient à chaque chargement.

**Le Destin.** Ces étapes se mesurent maintenant :
- réunir 20, 30 et 100 hommes ;
- gagner 1, 5 et 20 batailles ;
- vaincre une armée deux fois plus grande ;
- rallier trois vassaux, réclamer le trône, être couronné ;
- prendre une capitale de son peuple, puis les tenir toutes.

| Fichier | Rôle |
|---|---|
| `histoire/Noblesse.java` (nouveau) | la cour, la faveur, le service et ses tâches, l'adoubement, l'achat de titre, le mariage noble, le tournoi, les fiefs (impôt, aide, justice, gardes, travaux), sauvegarde |
| `histoire/Armee.java` (nouveau) | soldats, stades, caserne et levée, solde et désertion, bataille, ost, mercenariat, sièges, vassaux, réclamation du trône, couronnes, guerre et paix, sauvegarde |
| `histoire/Joueur.java` | FORMAT 18 puis 19 ; `noblesse()`, `armee()` ; M devant le château, la mairie de son fief, la caserne ; « Mon service », « Mes fiefs », « Mon armée » ; le titre dans la présentation ; la couronne passe à l'héritier et revient au chargement ; « accompli » quand l'aspiration est finie |
| `histoire/Brigands.java` | la défense du fief et la tâche « brigands » ; moins d'embuscades autour d'un fief gardé |
| `histoire/Destin.java` | étapes 27 à 46 |
| `histoire/Foule.java`, `Recit.java` | `taille()` ; le rang et la Maison du souverain ; « depuis peu » pour le joueur roi |

Essais m25 :
- **`NOBLESSE=1`** (étapes 230 à 239) :
  - service de la dame Vaewyn, quatre tâches (brigands et lettres) ;
  - adoubement, tournoi gagné, fief d'Elavenor ;
  - impôt lourd, gardes, palissade (embuscades × 0,44), justice rendue, défense du fief ;
  - mariage avec une cousine de la souveraine ;
  - la partie recharge le tout.
- **`ARMEE=1`** (étapes 240 à 249) :
  - recrutement à la caserne et levée dans le fief, puis l'ost contre Luvenor : une défaite à 163 contre 155 ;
  - siège d'Elaliamar, gardée pour fief ;
  - trois vassaux ralliés, puis le trône réclamé « par l'épée » : 152 contre 337. Couronné, le joueur entend « Ici, c'est moi qui gouverne, depuis peu » ;
  - l'impôt royal rentre (64 argent), et la couronne revient au chargement.

**Reste à faire pour la phase 10.**
- Les comtes et barons n'existent pas encore entre le roi et le joueur.
- Les vassaux du joueur ne sont pas des personnes du monde.
- Le monde ne simule pas la guerre pendant la partie : une ville prise reste au royaume vaincu sur la carte.
- Il n'y a ni élection ni guerre civile menée par des PNJ.
- Les batailles rangées jouées viendront en phase 12.

### Phase 11 — faite le 2026-11-15 (première version) : la terre et la famille

#### Lot 11a : la terre (`histoire/Terre.java`)

**Le calendrier.** Une saison dure 21 jours de jeu, une année 84 jours ; on commence au printemps de l'an 5000.
- La date s'affiche en tête de « Mon personnage » : « Nous sommes au 3e jour du printemps, an 5000 ».
- Chaque saison s'annonce : « Voici l'automne de l'an 5000 : il faut rentrer les récoltes avant le gel ».

**Les champs.** On les gère à la ferme du village : touche M devant les champs hors des murs, ou « Mes terres » dans « Mon personnage ».
- Un lopin s'achète 400 cuivre ou se loue 50 cuivre par saison. On en tient 12 au plus par ferme.
- Un lopin passe par ces états : en friche → labouré → semé → mûr → récolté.
- **Les cultures.** Le blé rend 5 grains pour 1 semé et mûrit en 14 jours. Les légumes rendent 5 pour 1 en 8 jours. On ne sème qu'au printemps et en été ; l'hiver, le gel tue ce qui reste aux champs.
- **Le travail à la main.**
  - Le temps de chaque geste : labourer 4 h, semer 1 h, désherber 1 h, récolter 3 h.
  - Le personnage joue l'animation du villageois : labourer, semer, désherber, moissonner.
  - Plus on soigne le champ, meilleure est la récolte.
- **Les journaliers.** On embauche un journalier par lopin pour 5 cuivre par jour. Il fait tout seul : il laboure, sème avec le grain de la réserve, désherbe et récolte.

**La réserve et la grange.** La récolte va dans la réserve de la ferme : 30 places sans grange, 300 avec.
- La grange se bâtit avec 250 cuivre et 20 planches.
- On dépose et on reprend avec son sac.

**Les bêtes.** On met au pré les poules, chèvres, moutons et bœufs du sac : 4 bêtes sans grange, plus avec.
- **Les produits.**
  - Les poules pondent des œufs tous les 4 jours, tous les 8 l'hiver.
  - Les chèvres donnent du fromage.
  - Les moutons donnent de la laine au printemps.
- **Les petits.** Avec une grange, les bêtes font des petits au printemps.
- **L'hiver.** Les bêtes mangent le grain de la réserve chaque semaine. Sans grain, une bête meurt.
- Deux nouveaux biens : la douzaine d'œufs et le mouton.

**Le Destin.** L'aspiration « Travailler la terre » se suit pas à pas :
- « Faire une première récolte » ;
- « Avoir des bêtes » (les bêtes au pré comptent) ;
- « Tenir une ferme avec des ouvriers » (2 journaliers) ;
- « Tenir un domaine de dix champs ».

| Fichier | Rôle |
|---|---|
| `histoire/Terre.java` (nouveau) | calendrier, saisons, fermes, lopins, cultures, journaliers, réserve, grange, bêtes, scènes, sauvegarde |
| `histoire/Biens.java` | `OEUFS`, `MOUTON` |
| `histoire/Joueur.java` | FORMAT 16 ; `terre()` ; la date dans « Mon personnage » ; « Mes terres » ; la ferme dans les boutiques (M devant les champs) |
| `histoire/Destin.java` | étapes 21 à 23 mesurables ; les bêtes au pré comptent |

Essai m25 `TERRE=1` (étapes 210 à 219) :
- une année entière ;
- 6 récoltes, dont une faite à la main ;
- 2 journaliers, une grange, des œufs, du fromage et de la laine ;
- l'aspiration « Travailler la terre » arrive à l'étape 4 ;
- la partie recharge à l'identique : réserve 73/73, 8 bêtes, la grange.

#### Lot 11b : la famille, la vieillesse et l'héritier (`histoire/Famille.java`)

**Vivre ensemble.** Avec son conjoint, épousé au lot F, on a de nouveaux choix quand on lui parle (touche X) :
- « « Viens vivre chez moi. » », si l'on a une maison ;
- « Passer la soirée ensemble », une fois par jour : 3 heures, l'amour et le moral montent.

**Les enfants.**
- **La naissance.** Un couple marié qui s'aime (amour 0,6 ou plus) attend un enfant :
  - chaque jour, 3 chances sur 100 si l'on vit ensemble, moins de 1 sur 100 sinon ;
  - la grossesse dure trois saisons, et l'enfant naît à la date annoncée ;
  - 6 enfants au plus ;
  - il faut être en âge d'en avoir : de 16 à 45 ans pour une femme, jusqu'à 60 ans pour un homme, en âge humain.
- **L'adoption.** Un couple du même sexe peut adopter un orphelin de 5 ans, qui arrive au bout de 7 jours.
- **Dans le monde.** L'enfant entre dans la population du monde, comme les proches créés à la création. Il apparaît dans « Ma famille et mes proches » et grandit avec le calendrier.
- **Le foyer.** Chez soi, le coffre propose « Ta famille… ». On y passe du temps avec ses enfants (2 h par jour) : leur éducation monte, plus vite si l'on sait lire, et l'héritier en gardera quelque chose.

**La vieillesse.** À chaque nouvel an :
- à partir de 55 ans humains, on perd un point de Vigueur ;
- à partir de 40 ans, le risque de mourir suit la table de mortalité du monde.

Les âges suivent maintenant le calendrier : on vieillit d'un an toutes les 84 journées. Un elfe compte toujours ses années en années d'elfe.

**La mort.**
- **De vieillesse**, au nouvel an, selon le risque ci-dessus.
- **Au combat.** Si l'on finit à terre et presque sans vie (santé 0,2 ou moins) dans une défaite contre des brigands ou des loups, on a une chance sur deux de ne pas s'en relever. Sinon, on est laissé pour mort mais on respire encore.

**La succession.** Quand le personnage meurt, « Mon personnage » ouvre la succession.
- **Les héritiers possibles.** Ce sont les enfants vivants, puis les frères et sœurs vivants.
- **La régence.** Un héritier mineur attend sa majorité : les années passent d'un coup. La majorité est à 16 ans humains, soit 192 ans pour un elfe et 11 ans pour un ork. La ferme, la bande et la faim ne rattrapent pas ces années.
- **Ce que reçoit l'héritier.**
  - Le seigneur prend 10 % de l'argent en droits ; l'héritier reçoit le reste, le sac, les objets, la maison, les ateliers, les terres et la bande.
  - Il garde la moitié du renom et de la réputation des groupes. Les surnoms, eux, restent à gagner.
  - Il garde la même aspiration, reprise à la première étape.
  - **De son parent**, il tient la moitié de ses dons au-dessus de la moyenne, de 25 à 50 % de ses compétences (selon l'éducation reçue), et la moitié de ses focus et de ses traits.
  - Il garde ses dettes.
  - Son travail, ses missions, son Destin du jour et ses amours recommencent.
- **La nouvelle famille.** Le défunt devient son père ou sa mère, mort, avec le nom qu'on lui avait choisi ; les autres enfants deviennent ses frères et sœurs. Le personnage a un nouveau visage, et le carnet note la passation.
- **Sans héritier**, la lignée s'éteint et l'on crée un nouveau personnage.
- **Passer la main.** Dans « Mon destin », dès qu'un héritier est majeur, « Passer la main à un héritier… » fait la même chose de son vivant.

**Le Destin.** Ces étapes se mesurent maintenant :
- « Avoir trois enfants » ;
- « Voir naître la troisième génération » : un héritier qui a lui-même un enfant ;
- « Vieillir ensemble » : marié, à 60 ans humains.

| Fichier | Rôle |
|---|---|
| `histoire/Famille.java` (nouveau) | vie commune, naissances et adoptions, foyer et éducation, vieillesse, mort, succession et régence, noms des ancêtres, sauvegarde |
| `histoire/Joueur.java` | FORMAT 17 ; `lignee()`, `nouvelId()`, `inserer()`, `succeder()` ; la succession s'ouvre à la mort ; âges au calendrier |
| `histoire/Social.java` | `conjoint()` |
| `histoire/Renommee.java` | `heriter()` : moitié du renom et des groupes, surnoms effacés |
| `histoire/Corps.java`, `Terre.java`, `Bande.java` | « Ta famille… » au coffre ; `sauter()` pour ne pas rattraper la régence |
| `histoire/Destin.java`, `Bande.java` | « Passer la main » ; les choix du couple quand on parle |
| `histoire/Brigands.java`, `Quetes.java` | la blessure mortelle (brigands, loups) |

Essai m25 `FAMILLE=1` (étapes 220 à 229), avec un elfe de 480 ans à Ulanewen :
- **la famille :** mariage avec Vaewyn et une soirée ensemble ; deux fils naissent, à l'hiver de l'an 5000 et à l'automne de 5001 ;
- **la mort au combat :** le père tombe sous les coups des brigands. La succession propose les deux fils (avec 191 et 192 ans de régence) et ses frères et sœurs ;
- **la régence :** l'aîné reprend en l'an 5192, à 192 ans, avec 42 argent après 10 % de droits. Il voit « Saendil — père (mort) » et son frère ;
- **la sauvegarde :** la partie recharge l'héritier ;
- **la vieillesse :** vieilli à 90 ans humains, l'héritier perd un point de Vigueur au nouvel an et meurt dans son lit. Son frère, devenu majeur, est proposé pour la suite.

**Reste à faire pour la phase 11.**
- Les champs ne sont pas encore dessinés dans le monde ; la ferme se gère en scènes.
- Le conjoint et les enfants n'ont pas encore de sprite qui vit dans la maison.
- ✔ « Qu'un enfant devienne noble ou maître » (Fonder une dynastie) : mesuré depuis la phase 10.
- ✔ Les oncles, tantes et grands-parents passent à l'héritier (lot G2).

### Lot G1 — fait le 2026-11-15 : le quotidien du travail (`histoire/Quotidien.java`)

Premier lot des petits restes.

**Les horaires par métier.** Chaque lieu de travail a ses heures. Le travail, le « Reviens entre… » et l'heure du réveil les suivent.

| Lieu | Heures |
|---|---|
| Boulangerie | 3 h – 13 h |
| Boucherie | 5 h – 17 h |
| Moulin | 5 h – 19 h |
| Fermes (champs) | 5 h – 20 h |
| Auberge, taverne | 10 h – 23 h |
| Marché | 7 h – 18 h |
| Forêt, mine | 6 h – 18 h |
| École | 8 h – 16 h |
| Autres ateliers | 6 h – 19 h |

**L'apprenti nourri et logé.**
- Le premier jour où il travaille, son maître lui sert une écuelle de soupe : la faim baisse de 35.
- Le soir, dans sa ville, il peut « dormir dans le grenier de son maître » sans payer.

**Les congés.** Dans « Travail », on demande 3 ou 7 jours de congé, une fois par saison.
- Il faut que le maître soit content (estime 40).
- Pendant le congé, aucune absence n'est comptée.
- Le congé coûte un peu d'estime (3 pour 3 jours, 7 pour 7 jours).

**Le chantier à la journée.**
- **Devant une ruine de la ville**, on la déblaie à la journée (8 heures, geste « pelleter »). Le bailli paie 6 cuivre, plus 1 par tranche de 25 en Labeur, sur la bourse de la ville. On y gagne un peu de renom et du Labeur.
- **Devant son propre chantier**, on met la main à l'ouvrage (geste « poser_poutre ») : le chantier avance d'un jour.
- Une seule journée de chantier par jour, et pas quand on est déjà trop fatigué.

**Les serviables.** Dans chaque ville, un habitant (toujours le même) fait une fois sur deux les annonces que personne n'a prises. Chacune lui vaut 5 de renom. Le tableau des missions en parle : « Sur la place, on parle de Faendil, qui a déjà rendu 4 services : il a 20 de renom ici, toi 9. » Quand on le dépasse (après au moins trois de ses services), la ville le dit et l'on gagne 5 de renom.

| Fichier | Rôle |
|---|---|
| `histoire/Quotidien.java` (nouveau) | horaires, soupe de l'apprenti, congés, chantier à la journée, serviables, sauvegarde |
| `histoire/Joueur.java` | FORMAT 20 ; `quotidien()` ; l'heure du réveil suit l'ouverture de son lieu ; le grenier du maître |
| `histoire/Travail.java` | les horaires de chaque lieu ; « Demander des jours de congé » ; la soupe à chaque séance |
| `histoire/Affaires.java` | la journée sur son chantier et sur une ruine de la ville |
| `histoire/Missions.java` | les annonces non prises vont au serviable ; il est cité au tableau |

Essai m25 `QUOTIDIEN=1` (étapes 250 à 259) :
- les horaires de la boulangerie (3 h – 13 h), de la taverne, de la forge, des fermes et du marché ;
- un congé de 7 jours : l'estime passe de 60 à 53, et rien n'est compté pendant le congé. Quatre jours d'absence ensuite la font tomber à 29 ;
- le grenier du maître est proposé au coucher ;
- une ruine déblayée : 6 cuivre, 51 % de fatigue ;
- Faendil, serviable d'Ulanewen, a 4 services et 20 de renom. La partie recharge le tout.

### Lot G2 — fait le 2026-11-15 : les chansons, la famille et les Maisons (`histoire/Chansons.java`)

**Les chansons sur toi.** Elles naissent de deux façons :
- un barde payé chante tes exploits ; pendant 30 jours, la chanson voyage deux fois plus vite ;
- une ville où tu es connu dans le comté (renom 150) fait d'elle-même « La geste de … » dès que tu as deux exploits.

**Les chansons de toi.** À la taverne ou à l'auberge :
- **Chanter pour la salle** (1 h, une fois par jour et par ville). Il faut connaître au moins une chanson. Le chapeau rapporte 2 cuivre, plus 1 par tranche de 8 en Éloquence et 1 par tranche de 400 habitants (60 au plus). On y gagne aussi de l'Éloquence, un peu de renom et du moral.
- **Composer une chanson** (une soirée, une fois par saison). Il faut 40 en Éloquence et 5 chansons connues. Quatre thèmes existent : la ballade, les remparts, la complainte et la chanson à boire, chacun avec le nom de la ville. Une belle chanson part aussitôt vers une ville voisine.

**Elles voyagent.** Chaque jour, une chanson a 8 % de chances de gagner une ville voisine par une route ouverte (24 villes au plus). On la trouve sur l'étagère « Livres et chansons » des villes où elle est chantée, à côté des œuvres du monde.

**On les apprend.** Écouter une chanson du monde sur l'étagère, ou une des tiennes, l'ajoute aux chansons connues.

**Le Destin.** Ces étapes se mesurent maintenant :
- apprendre dix chansons ;
- jouer dans cinq tavernes ;
- composer une chanson qui te survivra, c'est-à-dire chantée dans 3 villes ;
- qu'une chanson soit écrite sur toi (pour le Chef de guerre légendaire et le Hors-la-loi légendaire).

**Les demandes des proches.** Un parent, un frère ou une sœur qui t'estime (20 ou plus) peut aussi te demander un service, avec des demandes de famille :
- les parents, oncles et grands-parents : des soins pour leurs vieux os, des planches pour le toit, des chandelles, de la laine, de la farine ;
- les frères et sœurs : des bottes pour le petit, un couteau, du vin pour des noces, une cape, du miel.

La famille ne paie que la moitié de ce que ça coûte, mais l'estime du proche monte de 10 et le moral de 5. Refuser coûte 2 d'estime.

**Les Maisons nobles.** La Maison qui règne sur un royaume retient ce que l'on fait pour sa couronne.
- **Ce qui la fait changer.**
  - Chaque gain ou perte de faveur à la cour change d'autant la moitié de la réputation auprès de la Maison.
  - Épouser quelqu'un de la Maison donne +25.
  - Réclamer le trône donne −50.
  - Prendre une ville du royaume donne −10.
- **Ses effets.**
  - Une Maison qui t'aime fait monter la faveur plus vite (jusqu'à × 1,5).
  - À −40, elle te tient pour ennemi : la cour ne t'offre plus ni service ni tâche.
- **L'affichage.** On la voit dans « Ce qu'on dit de moi » (« La lignée de Tyndil : … »).

**Oncles, tantes et grands-parents.** Quand un enfant hérite, les frères et sœurs du mort deviennent ses oncles et tantes, et les parents du mort ses grands-parents. Leur estime vaut la moitié de l'ancienne, plus 30.

| Fichier | Rôle |
|---|---|
| `histoire/Chansons.java` (nouveau) | chansons sur toi et de toi, voyage de ville en ville, étagère, chanter, composer, chansons apprises, sauvegarde |
| `histoire/Joueur.java` | FORMAT 21 ; `chansons()` ; les chansons sur l'étagère ; écouter une chanson du monde l'apprend ; chanter et composer à la taverne ; l'héritier repart sans chansons apprises |
| `histoire/Social.java` | le barde payé crée la chanson ; « la geste d'Orindil » ; `procheDe()`, `roleDe()` |
| `histoire/Quetes.java` | les demandes de la famille |
| `histoire/Renommee.java` | le groupe MAISON et son affichage |
| `histoire/Noblesse.java`, `Armee.java` | la faveur touche la Maison ; noces, trône réclamé, conquête ; la cour hostile |
| `histoire/Destin.java` | étapes 47 à 50 |
| `histoire/Fiche.java`, `Famille.java` | les rôles « oncle / tante » et « grand-père / grand-mère » ; ils passent à l'héritier |

Essai m25 `CHANSONS=1` (étapes 260 à 269) :
- Faelune, la sœur, demande une bouteille de vin pour ses noces d'étain. Elle la reçoit, et son estime passe de 50 à 60 ;
- la faveur +12 donne +6 auprès de la lignée de Tyndil. Une trahison (−100) la fait tomber à −44, et la cour refuse alors le service ;
- dix chansons du monde sont apprises ;
- chanter à la taverne rapporte 3 cuivre ;
- « La complainte d'Ulanewen » est composée et part aussitôt vers une ville voisine ;
- le barde payé crée « La chanson d'Orindil » ;
- après 60 jours, les deux chansons sont chantées dans 7 et 9 villes, et l'on voit les deux sur l'étagère d'Ulanewen ;
- le Destin franchit « Apprendre dix chansons » (Barde) et « Qu'une chanson soit écrite sur toi » (Hors-la-loi) ;
- la partie recharge le tout.

### Lot G3 — fait le 2026-11-15 : le monde visible

**Les marchands des étals.**
- Chaque étal de la place est tenu par un habitant sans emploi du village (`Villageois.etal`).
- De 7 h à 18 h, il se tient derrière son étal, à 1,1 m, côté extérieur de la place, tourné vers les chalands. Il crie sa marchandise, la montre et compte ses pièces.
- Avec M sur la place, le marchand le plus proche vient servir, comme l'artisan qui sort de son atelier (`Habitants.servirEtal`).

**Le destinataire sur la place.** Une mission à rendre sur la place (une lettre) a maintenant quelqu'un qui vient : le bailli de la mairie, sinon l'adulte éveillé le plus proche, à moins de 60 m (`Habitants.accueillir`). Le bandeau dit « Caeion t'attendait sur la place : Q pour lui remettre la lettre. »

**La bande contourne les maisons.** Dans un village, quand une maison est entre un compagnon et sa place derrière le joueur, il suit un chemin calculé autour d'elle. C'est le même A* que les villageois (`Chemins`), recalculé toutes les 1,2 s. Hors des villages, il marche droit comme avant.

**Les moulins tournent.**
- La roue à aubes (quatre rais, huit pales) et les ailes du moulin à vent ne sont plus peintes dans la texture du bâtiment.
- Ce sont des objets à part (`Batiments.ROUE_EAU`, `AILES`), peints en 4 positions de rotation. L'écran les pose à chaque image, sur tous les moulins des villages chargés (`FirstScreen.animerObjets`).
- La roue change de position toutes les 0,2 s, les ailes toutes les 0,28 s.

**Les loups se voient.**
- Quand la mission des loups les fait sortir (la nuit, hors des murs), chaque loup restant rôde en cercle autour du joueur, à 10 m (± 2), chacun à son allure (`Joueur.loupsDehors`).
- Le loup est peint avec les volumes du peintre de bâtiments (`Batiments.LOUP`) : corps, poitrail, tête et museau, oreilles, yeux qui luisent, queue basse, pattes qui avancent tour à tour.
- Il existe en 16 orientations, 4 pas et 2 pelages (gris ou brun).
- Ils disparaissent quand le combat est livré (Q) ou au lever du jour.

| Fichier | Rôle |
|---|---|
| `Villageois.java` | `etal` |
| `Habitants.java` | un marchand par étal, la place derrière l'étal et ses gestes ; `servirEtal()`, `accueillir()`, `amener()` |
| `FirstScreen.java` | M sur la place fait venir un marchand ; le destinataire sur la place ; le chemin de la bande ; `animerObjets()` (mobilier des camps, moulins, loups) |
| `Batiments.java` | `ROUE_EAU`, `AILES`, `LOUP`, `rais()` ; le moulin n'a plus que l'axe de sa roue |
| `histoire/Joueur.java`, `Quetes.java` | `loupsDehors` |
| `histoire/Bande.java` | `ajouterPourEssai()` |

Essai m25 `MONDE=1` (étapes 270 à 289), avec captures :
- 4 étals tenus à Ulanewen, chaque marchand à 1,1 m de son étal. Tyriel vient servir, de 4,1 m à 1,5 m ;
- le destinataire de la lettre vient de 8,5 m à 1,5 m ;
- la bande, de l'autre côté d'une maison, suit un chemin de 3 à 5 points. Elle revient à 2 m en 7 secondes sans se téléporter ;
- la roue du moulin tourne d'une capture à l'autre ;
- trois loups, gris et bruns, rôdent autour du joueur.

**Reste à faire pour le lot G3.**
- Les ailes du moulin à vent n'ont pas été vues en capture, car le village d'essai n'a qu'un moulin à eau.
- Les loups ne se battent pas encore en action : le combat reste une scène.

### Lot G4 — fait le 2026-11-16 : l'économie

**Les ouvriers sont de vrais habitants.** Quand un atelier du joueur embauche, l'ouvrier est tiré parmi les adultes de la ville dans la Foule (`Affaires.nouveau`). Son nom et son sexe sont les siens, et son id de la Foule est gardé (`Employe.graine`). On essaie jusqu'à 12 habitants pour ne pas prendre le joueur ni quelqu'un qui travaille déjà pour lui (`EMBAUCHE_ESSAIS`). Sa fiabilité, son honnêteté et sa loyauté restent tirées comme avant.

**Chacun a un métier.**
- La Foule ne garde pas de métier. On le tire de l'id de l'habitant et de l'économie de sa ville (`histoire/Emplois.java`) : plus un lieu a d'ouvriers (champs, forêt, mine, colporteurs, place, ateliers), plus il a de chances d'être le sien.
- Au moins 35 % des adultes travaillent la terre (`CHAMPS_MIN`). Ceux qui vivent hors des villes aussi.
- Le même habitant garde le même métier tant que le lieu existe.
- Les phrases : « travaille aux champs », « garde les bêtes », « coupe du bois », « travaille à la mine », « tient un étal sur la place », « travaille chez le forgeron », « travaille dans ton atelier (la forge) ».
- Le père et la mère ont le métier de l'origine choisie à la création (« menuisier », « menuisière » : `Creation.METIERS_PARENTS`). Un maître qui a déjà son métier dans sa note (« guérisseur ») n'en reçoit pas d'autre.
- Le métier s'affiche dans « Ma famille et mes proches » : « Tyia — amie, travaille chez le tisserand (470 ans) ».

**Les livres achetés sont de vrais livres.**
- Chaque livre du sac (`Biens.OUVRAGE`) porte une oeuvre de `Livres` (`Joueur.bibliotheque`). C'est une oeuvre écrite dont la ville a une copie, sinon une oeuvre du peuple du joueur, jamais deux fois la même.
- À l'achat, la boutique dit le titre : « C'est « Annales de Luenor ». »
- Dans le sac, la fiche du livre propose « Lire « titre » » pour chacun. Il faut savoir lire (instruction), sinon « Tu ne sais pas lire ». Lire note le titre au carnet et compte pour le Destin, comme à l'étagère.
- Un livre vendu ou jeté enlève le dernier de la liste. La liste est sauvegardée (format 22 du joueur).

**Choix assumés (on ne les change pas).**
- Les foyers restent agrégés dans l'économie : la ville n'a pas un salaire par habitant, mais des lieux avec un nombre d'ouvriers. Le métier de chacun en est tiré.
- Les autres villes ne tournent que quand on y passe : leur temps est rattrapé à l'arrivée.

| Fichier | Rôle |
|---|---|
| `histoire/Emplois.java` (nouveau) | `metier()`, `phrase()`, `parent()` |
| `histoire/Affaires.java` | ouvriers tirés de la Foule ; `embauche()`, `equipePourEssai()` |
| `histoire/Creation.java` | `METIERS_PARENTS` |
| `histoire/Joueur.java` | `bibliotheque`, `rangerLivres()`, `livreAuHasard()`, `lireLivre()`, le titre à l'achat, « Lire » dans le sac, le métier dans la famille ; format 22 ; `livresPourEssai()` |

Essai m25 `ECONOMIE4=1` (étapes 290 à 299) :
- quatre ouvriers embauchés à la forge sont des habitants de la ville (Naedor, Naelas, Oridor, Laeael) ;
- trois livres achetés : « Annales de Luenor », « Les Guerres de Nyriion », « Annales de Nyriion », chacun avec son « Lire » ;
- sans instruction : « Tu ne sais pas lire ». Avec : le texte des Annales s'ouvre ;
- la famille : « Vaelas — père, menuisier », « Eolas — frère, travaille chez le tavernier »… ;
- après sauvegarde et rechargement, les trois mêmes titres.

**Reste à faire pour le lot G4.**
- ✔ Bâtir un atelier sur un terrain libre : fait au lot G5.

### Lot G5 — fait le 2026-11-16 : bâtir sur un terrain libre

C'est le premier pas vers « bâtir une ville » : le joueur pose un bâtiment là où il veut, et le village le garde.

**En jeu.**
- Dans un village, le joueur s'arrête face à un terrain nu. Au bout de 1,5 s (`TERRAIN_ARRET`), l'écran essaie, pour chaque métier, le plan de l'atelier posé devant lui, la porte à 1,5 m (`Villages.PORTE_RECUL`), la façade tournée vers lui.
- Le terrain doit suivre les mêmes règles que les lots du village (`Villages.terrainLibre`) :
  - rien de bâti dessous ;
  - pas de rue sous les murs (une rue peut longer le bâtiment) ;
  - au plus 3,5 m de dénivelé.
- Le bandeau dit « Devant toi : un terrain libre (M pour y bâtir) ».
- M ouvre « Un terrain libre » (`Affaires.terrain`). On y trouve les ateliers qui tiennent sur ce terrain, qui ont des recettes, que le joueur n'a pas déjà dans cette ville et qu'il peut payer.
- Le prix est celui du bâtiment × `TERRAIN` (1,0, contre 0,8 sur une ruine), plus la licence.
- Le chantier dure 12 jours. Il se voit tout de suite : des murs à demi montés (`Plan.chantier`, peint comme une ruine fraîche).
- Au bout des 12 jours, l'atelier est debout. Il entre dans l'économie de la ville comme un atelier bâti sur une ruine, et le bandeau annonce « Le chantier est fini : ta forge d'Ulanewen est debout ».
- M devant l'atelier ouvre « Ta forge ». Ses murs arrêtent le joueur et les villageois (la grille est marquée `BATIMENT`). Les plantes du village qui étaient dessous sont arrachées.

**Comment le village le garde.**
- L'histoire retient chaque construction (`Affaires.Construction`) :
  - la ville ;
  - le point où se tenait le joueur, en mètres locaux du village ;
  - la direction où il regardait ;
  - le métier, la fin du chantier, et si c'est fini.
- La liste est sauvegardée (format 23 du joueur). Elle est publiée pour l'écran (`Joueur.constructionsVue`, `constructionsVersion`).
- Le village se génère comme avant. Les bâtiments du joueur s'ajoutent après tout le reste (`Villages.ajouts`, `planAjout`) : aucun index du village ne bouge, et les maisons, ruines et étals déjà repérés restent les mêmes.
- Le plan dépend seulement de la ville, du point, de la direction et du métier (`graineAjout`) : le même bâtiment revient à chaque chargement.
- Quand la liste change, l'écran oublie le village concerné (`Villages.oublier`, `Habitants.oublier`). Il se refait à l'image suivante (environ 140 ms), et ses habitants reviennent.

**Corrigé au passage.** Les ruines d'un village sont rangées après les maisons, avec les objets. Or l'écran ne les cherchait que parmi les maisons. Il les cherche maintenant partout :
- « Devant toi : une ruine » s'affiche enfin en jeu ;
- un atelier rebâti sur une ruine se dessine et s'ouvre avec M ;
- les chantiers de maisons du lot E voient les ruines.

Les ateliers sont aussi reconnus où qu'ils soient rangés, et les roues et ailes des moulins tournent partout.

| Fichier | Rôle |
|---|---|
| `Villages.java` | `Ajout`, `ajouts`, `graineAjout()`, `planAjout()`, `terrainLibre()`, `oublier()` ; les bâtiments du joueur ajoutés à la fin du village, la grille marquée, les plantes arrachées |
| `Batiments.java` | `Plan.chantier` |
| `Habitants.java` | `oublier()` |
| `FirstScreen.java` | le terrain libre devant le joueur arrêté (`terrainsLibres()`, `regard()`), le bandeau, le village refait quand les constructions changent ; ruines, ateliers et moulins cherchés partout |
| `histoire/Affaires.java` | `Construction`, `TERRAIN`, `terrain()`, `batirTerrain()`, la fin du chantier, la publication, la sauvegarde ; `terrainPourEssai()`, `finirConstructionsPourEssai()` |
| `histoire/Joueur.java` | `terrainLibre`, `constructionsVue`, `constructionsVersion` ; M devant un terrain libre ; format 23 |

Essai m25 `LOTLIBRE=1` (étapes 300 à 319), avec captures `g5_terrain`, `g5_chantier`, `g5_atelier` :
- à Ulanewen, 3 ruines, toutes rangées après les maisons (d'où la correction) ;
- un terrain libre à 24 m du centre : 11 ateliers y tiennent. On bâtit la forge pour 28 argent 50 cuivre ;
- le chantier apparaît (murs à demi montés), puis la forge debout ;
- M devant la forge ouvre « Ta forge ». La grille est marquée au centre. En marchant 4 s droit vers le centre, le joueur reste à 2,8 m (demi-profondeur 2,5 m) : il bute sur le mur ;
- après sauvegarde et rechargement, la construction est là, finie.

**Reste à faire, vers « bâtir une ville ».**
- Bâtir une maison (pour soi ou à louer), des rues et une place. On pourrait aussi fonder un village hors de tout village : il faudrait une grille à soi, là où le joueur se trouve.
- Les villageois ne travaillent pas dans l'atelier du joueur : ses ouvriers restent ceux de la Foule, sans corps.
- Si une ville grandit au point de changer de nombre de maisons, son plan se régénère. Un bâtiment du joueur pourrait alors chevaucher une maison nouvelle (c'était déjà le cas des ruines rebâties).

### Plus tard : un tutoriel en jeu

L'utilisateur veut un tutoriel joué dans le monde : des bulles ou des conseils au bon moment (première fois devant un marchand, premier travail, premier soir…). Il devra présenter les touches M, L, Q, U, Z, X, la marche et l'arrêt.

## 14. Le monde vivant (décidé le 2026-11-16)

But : **aucune différence entre le joueur et un PNJ géré par l'IA.** Les villageois existent vraiment, la famille vit, les fermes se voient, les batailles se jouent en tacticien. Machine de référence : RTX 5070, 24 Go de RAM, i7-8700. Le processeur (6 cœurs) est la limite : on compte les cerveaux, pas les corps.

### 14.1 Les PNJ égaux au joueur

**Un PNJ, c'est son identifiant.**
- Toute sa fiche se déduit de son id dans la Foule et de ce que la Foule garde déjà de lui :
  - le sexe, la naissance, la ville ;
  - le caractère (loi, bien, ambition, courage, foi) ;
  - le rang, le talent, le conjoint, les parents.
- On en tire :
  - les attributs, les compétences (selon son métier et son âge), la générosité et l'honnêteté ;
  - l'argent, le sac (les outils de son métier, ses habits, des vivres).
- Le même id redonne toujours la même personne.
- Seuls les **gens importants** gardent une fiche qui évolue, et l'on n'en sauvegarde que les points qui ont changé (argent, objets, progrès, estime). Les gens importants sont :
  - la famille du joueur et ses proches ;
  - ses ouvriers ;
  - les rivaux ;
  - ceux avec qui la relation est forte.

**Les rivaux (notables).**
- Chaque ville a un nombre fixe de personnages importants, selon sa taille (par exemple 2 dans un hameau, 12 dans une grande ville).
- S'il en meurt un, un autre le remplace ou apparaît. Ce nombre borne le calcul.
- Ils ont des ambitions et agissent comme le joueur : racheter un atelier, gagner du renom, monter une bande, entrer au service d'un seigneur, devenir noble, prendre la tête de la ville.

**Les niveaux de détail.**
- **Près du joueur** (environ 150 m, une centaine de corps au plus) : corps, gestes, chemins. Les objets bougent pour de vrai : le forgeron consomme le minerai, le boulanger vend son pain.
- **Dans la ville chargée** : pas de corps, une journée résumée (« 8 h à la forge : 3 fers, 2 argent ; repas ; nuit »).
- **Ailleurs** : seulement le fonctionnement d'ensemble (économie, Foule). Quand le joueur arrive, chacun reprend son état détaillé à partir de là.

**Les décisions.** Des besoins à la Sims (faim, fatigue, argent, liens, ambition) choisissent l'activité. L'IA locale ne sert qu'aux paroles.

**Lots.**
- **H1 — la fiche de chaque habitant** (`histoire/Gens.java`) : la fiche tirée de l'id, les gens importants qui gardent la leur ; on la voit en parlant (X), dans la famille et chez ses ouvriers.
- **H2 — les villageois visibles sont de vrais habitants** : les foyers de la Foule répartis dans les maisons du village. Un villageois est une personne (id, nom, âge, métier, lieu de travail réel). Les corps se chargent selon la distance.
- **H3 — les rivaux de chaque ville** : leur nombre, leur remplacement, leurs ambitions jouées jour après jour.
- **H4 — la journée de chacun** : besoins, achats avec son argent, repas, sommeil ; en action près du joueur, résumée loin de lui.
- **H5 — tes ouvriers au travail** : visibles dans ton atelier, ils font les gestes, la matière baisse, leur compétence monte, tu les paies.

### 14.2 La famille vivante

- Le conjoint et les enfants ont un corps et vivent dans ta maison. Ils travaillent, grandissent, entrent en apprentissage, se marient et partent.
- Ce sont des PNJ comme les autres (14.1), mais toujours importants.
- À ta mort, tu continues avec ton héritier, qui a vraiment vécu. On n'incarne que son héritier.

### 14.3 Les fermes persistantes

- Les champs, prés et vergers se placent autour de chaque village selon sa taille, son terrain et son histoire. Ils s'étendent quand la ville grandit, et retournent à la friche quand elle décline.
- Les cultures poussent avec les saisons du calendrier (phase 11) : labour, semis, pousse, épis, moisson, chaume. Les bêtes sont aux prés.
- Ce sont les PNJ (et la famille) qui y travaillent, en action près du joueur.
- Le joueur choisit son champ sur le terrain, comme le terrain libre (lot G5).

### 14.4 Le combat en action (petits combats)

- Jusqu'à environ 15 à 20 combattants, on se bat soi-même, librement, à la manière d'un RPG.
- On peut passer à tout moment du **temps réel** au **tour par tour**.
- Au-delà de ce nombre, c'est une bataille rangée (14.5).
- Les loups, les brigands et les duels passent par là ; la fenêtre de combat disparaît.

### 14.5 Les batailles rangées, à la Total War

- **Des groupes qui pensent.** Une unité est une seule entité qui réfléchit : 1000 soldats par camp en 8 groupes, c'est 8 cerveaux. Les soldats ne font que suivre leur groupe. La taille n'a pas de limite : on adapte les groupes.
- **La formation.** Le joueur choisit la formation de son armée avant la bataille.
- **Les ordres** : on sélectionne des groupes et on les envoie où l'on veut. Les ordres sont limités, pour une vraie tactique : portée de la voix et des cors, messagers qui prennent du temps, groupes qui obéissent selon leur moral et le Commandement de leur chef.
- **Le joueur est un groupe parmi les autres.** Il se bat avec son groupe. S'il tombe, son second prend la main ; sinon les troupes se désorganisent.
- **La caméra** reste en 3/4. On peut mettre en pause, ralentir ou accélérer. Un menu de réglages viendra plus tard.

### 14.6 La noblesse complète

- Quatre rangs : roi, comte (une région), baron (un village ou quelques-uns), chevalier (sans terre, ou avec un domaine).
- Ce sont de vraies personnes, et les vassaux du joueur aussi.
- La guerre est simulée pendant la partie : les sièges se font, et les villes changent de royaume sur la carte.
- Il y a des élections et des guerres civiles menées par les PNJ.

### 14.7 Bâtir une ville

- La suite du terrain libre (lot G5) : des maisons, des rues, une place.
- On pourra fonder un village hors de toute ville.

### 14.8 L'ordre

PNJ égaux (H1 à H5) → famille vivante → fermes → combat en action → batailles rangées → noblesse → bâtir une ville. Le tutoriel reste suspendu.

### Lot H1 — fait le 2026-11-16 : la fiche de chaque habitant (`histoire/Gens.java`)

**Un habitant, c'est son id.**
- `Gens.tirer()` déduit sa fiche de son id et de ce que la Foule garde de lui :
  - le sexe, la naissance, la ville, la Maison ;
  - le caractère (loi, bien, ambition, courage, foi) et le talent.
- Le même id redonne la même fiche, vérifié deux fois de suite sur 8 habitants.
- La fiche contient :
  - **les attributs** : 2 partout, plus `POINTS_BASE` (3), plus un point par 34 de talent (`TALENT_POINT`), plus un pour un adulte. La moitié des points va dans l'attribut du métier. Au plus 6 (`ATTRIBUT_PNJ_MAX`) ;
  - **le métier**, donné par le lieu de travail (`Emplois.lieu`) et la compétence qu'il demande (`Emplois.competence`) : 15 + 4 par année de métier depuis 14 ans (au plus 45 ans de métier) + talent ÷ 3, dans la limite du plafond de la fiche. Les années d'expérience donnent aussi du focus ;
  - **ce que tout adulte sait** : labeur, athlétisme, un peu de commerce et d'éloquence. Les hommes courageux ont de quoi tenir une arme (la milice) ;
  - **les lettres** : les nobles, les maîtres d'école et un habitant sur 7 environ (`LETTRE`) savent lire. Les enfants apprennent avec l'âge ;
  - **la générosité et l'honnêteté**, tirées de l'id ;
  - **l'argent** : selon le métier (champs 150, ateliers 250, commerce 600 cuivre), l'âge et le rang (×12 pour un noble) ;
  - **le sac** : du pain, l'outil de son métier (faucille, hache, pic, marteau, trousse…), une cape s'il est aisé.

**Les gens importants gardent leur fiche** (`importants`, sauvegardés au format 24 du joueur). Pour l'instant :
- les proches de la famille, dès qu'on ouvre « Ma famille et mes proches » ;
- les ouvriers du joueur, dès qu'ils sont embauchés.

On garde aussi des **lieux fixés** (`lieux`) : celui des parents vient de l'origine choisie à la création (`Creation.lieuOrigine`). Un père menuisier a donc Bois 215, pas Labeur.

**Où ça se voit.**
- « Qui es-tu ? » (X, ou parler à un proche) ajoute ce qu'on remarque : « (Tyia travaille aux champs. Elle excelle en labeur (185), ne sait pas lire et vit chichement.) » (`Gens.portrait`).
- La famille montre la meilleure compétence de chacun : « Caeion — père, menuisier [Bois 215] (729 ans) ».
- **On embauche le plus habile.** Parmi les 12 habitants vus, l'atelier garde le meilleur pour son métier. Sa fiabilité vient de sa compétence (0,7 pour un débutant, 1,3 à 150 : `Affaires.fiabilite`), son honnêteté de sa fiche.
- **Les ouvriers apprennent.** Chaque journée à l'atelier (hors grève) est un effort de 4 (`APPRENTISSAGE`) dans la compétence du métier, et leur fiabilité suit. La fiche d'un ouvrier montre ce qu'il sait.

| Fichier | Rôle |
|---|---|
| `histoire/Gens.java` (nouveau) | `tirer()`, `de()`, `retenir()`, `fixerLieu()`, `portrait()`, `meilleure()`, sauvegarde ; `essai()`, `importantsPourEssai()` |
| `histoire/Emplois.java` | `lieu()`, `competence()`, `employeur()` ; le lieu fixé passe en premier |
| `histoire/Creation.java` | `lieuOrigine()` |
| `histoire/Affaires.java` | embauche du plus habile, fiche de l'ouvrier, apprentissage ; `postePourEssai()` |
| `histoire/Joueur.java` | `gens()`, format 24, le portrait dans « Qui es-tu ? », la compétence dans la famille ; `portraitPourEssai()` |

Essai m25 `GENS=1` (étape 320) :
- 8 habitants d'Ulanewen tirés deux fois, même fiche à chaque fois (Syriel, tisserande, Métiers fins 154 ; Saeriel, aux champs, Labeur 61…) ;
- trois portraits ;
- trois ouvriers de forge : Miwyn (Forge 106) a été choisie pour son métier. Après 120 jours, les deux débutants passent de 0 à 17, et Miwyn de 106 à 109 ;
- après sauvegarde et rechargement, les 3 fiches sont là, avec leurs progrès ;
- la famille : les parents menuisiers ont Bois 215 et 170.

**Reste à faire après H1.** Les villageois visibles ne sont pas encore ces habitants-là (lot H2), les fiches importantes n'évoluent qu'à l'atelier (lot H4), et il n'y a pas encore de rivaux (lot H3).

### Lot H2 — les villageois visibles sont les vrais habitants (fait le 2026-11-16)

Avant H2, un villageois était tiré de la graine de sa maison : il n'existait que sur l'écran, sans lien avec la Foule. Désormais **chaque villageois visible est un habitant de la Foule** : sa graine est son id, il porte son vrai nom, et sa fiche (H1) est la sienne.

**La répartition (`Gens.foyers`, sur le fil de l'histoire).** Pour une ville, on prend tous les vivants de la Foule (sauf le joueur) :
- **la maison** : un couple partage la même, un enfant vit chez ses parents (clé du foyer → maison logeable tirée de la clé) ;
- **le travail** : le lieu d'`Emplois.lieu` donne un bâtiment du village de ce métier. Les ouvriers du joueur vont dans son bâtiment (code `JOUEUR + métier`). Ceux de la place tiennent les étals, puis des adultes sans emploi. Les bâtiments vides (moulin, mairie…) prennent un sans-emploi, trois pour la caserne et le château ;
- **pas de travail** pour les enfants (moins de 14 ans) ni pour les vieux (plus de 65 ans, `AGE_RETRAITE`) ;
- **important** : un homme important (H1), un proche du joueur ou un de ses ouvriers.

L'écran demande cette répartition à l'histoire (`FirstScreen.foyersDe`) et la redemande toutes les 30 s (`FOYERS_DUREE`), ou quand le village est refait.

**Les corps selon la distance (`Habitants.incarner`, chaque seconde).**
- Un habitant prend corps quand sa maison ou son travail est à moins de **120 m** (`VIE`) du centre de la vue. Les gens importants et celui qu'une mission fait chercher (`prioritaires`) passent avant tout le monde.
- Il y a au plus **80 corps par village** (`CORPS_VILLAGE`) et 120 en tout. On prend les plus proches.
- Un corps s'en va au-delà de **150 m** (`OUBLI_CORPS`), ou s'il faut de la place. Jamais quand il est occupé : en discussion, avec le joueur, au comptoir, en fuite…
- Il paraît déjà à sa place : chez lui la nuit, au travail le jour.
- Les loin ne sont pas simulés : ils existent seulement dans la Foule et la répartition.

**Ce que ça change.**
- **La famille est là, en vrai.** Le père et la mère du joueur sont les villageois de leur maison, avec leurs noms. `Social` les reconnaît par leur id et n'« emprunte » plus le corps d'un autre villageois.
- Un ouvrier embauché est le même habitant qu'on croise ensuite à son poste.
- Les bâtiments du joueur sont marqués (`Plan.joueur`).

| Fichier | Rôle |
|---|---|
| `histoire/Gens.java` | `Foyers`, `foyers()`, `foyer()` ; `AGE_RETRAITE`, `LIEU_ETAL`, `JOUEUR` |
| `histoire/Social.java` | le proche est retrouvé par son id |
| `Habitants.java` | `foyers` (fournisseur), `prioritaires`, `incarner()`, `etatFoyers()` ; `VIE`, `OUBLI_CORPS`, `CORPS_VILLAGE`, `INCARNATION` ; l'ancien `peupler()` reste en secours |
| `Villageois.java` | `nomReel` |
| `Relations.java` | le lien prend le vrai nom |
| `Batiments.java`, `Villages.java` | `Plan.joueur` |
| `FirstScreen.java` | `foyersDe()` (lieux et maisons du village, demande à l'histoire, cache) ; la cible de mission en priorité |

Essai m25 `HABITANTS2=1` (étapes 330 à 339), à Ulanewen :
- 265 habitants, 80 corps, tous nommés (25 au travail, 14 enfants, 42 en couple) ;
- 12 des 14 proches du joueur sont visibles avec leur nom (famille dans la maison 39). Les 2 autres ne vivent pas ici ;
- à 160 m puis de retour à 23 h : toujours 80 corps, 66 sont les mêmes gens.

**Reste à faire après H2.** Les rivaux (H3), la vie quotidienne par besoins (H4), et les ouvriers qui font les gestes du métier avec leur sac (H5).

### Lot H3 — les notables, rivaux de chaque ville (fait le 2026-11-17)

**Combien.** Chaque ville suivie a un nombre fixe de notables, selon sa taille : `2 + √habitants / 8`, entre 2 et 12 (`Notables.nombre`). Ulanewen (259 habitants) en a 4, une capitale de 6 000 habitants en a 12. Ce nombre borne le calcul.

**Quelles villes.** On suit la ville où vit le joueur et celles où il passe, au plus 16 (`VILLES_MAX`). Les plus anciennes sont oubliées, sauf la sienne.

**Qui.** Les meilleurs habitants de la Foule de la ville, de 20 à 67 ans. On les classe selon l'ambition, le talent, la moitié du courage, le rang, et un bonus de 60 pour les nobles. Un notable devient important : sa fiche (H1) est gardée et évolue, et il a un corps en priorité (H2).

**Son but.** Comme le joueur, chacun poursuit une **aspiration** de la même liste (`Aspirations`). Elle est choisie selon son caractère (l'axe de l'aspiration), ce qu'il sait faire et un peu de hasard. La couronne, l'unification et la fondation d'une ville sont réservées aux nobles. Quand il atteint son but, il en prend un autre.

**Chaque jour** (`Notables.journee`) :
- il gagne sa vie : sa meilleure compétence / 12, moins ses dépenses ;
- il avance vers sa prochaine étape : (0,4 + compétence de l'aspiration / 150 + ambition / 150) × un hasard de 0,5 à 1,5 ;
- une étape coûte 120 + 80 × son rang. Un but de 4 étapes prend donc plusieurs années de jeu ;
- chaque étape franchie donne 15 de renom et 5 points dans la compétence. Les aspirations de richesse rapportent aussi de l'argent ;
- celui qui accomplit « Diriger sa ville » **prend la tête de la ville** (`chefs`).

**Remplacement.** Un notable mort, parti ailleurs ou trop vieux (72 ans) est remplacé par le meilleur des autres. La nouvelle se répand : « À Ulanewen, Isimir prend la place de Naedor parmi les notables. »

**Le rival.** Un notable de ta ville qui poursuit le même genre d'aspiration que toi (pouvoir, richesse…) est ton rival (`rival()`).

**Ce que le joueur voit** :
- dans le bandeau et le carnet, les nouvelles de sa ville et de celle où il est : « On dit à Ulanewen que Vaewyn a réussi : « Monter une caravane ». », « Ta rivale Orithien avance : « Entrer dans une guilde ». » ;
- « Qui es-tu ? » ajoute : « C'est une notable d'Ulanewen : elle veut diriger sa ville (étape 1 sur 4). C'est ta rivale. » ;
- la page **« Les notables d'ici »** (accueil) : chacun, son métier, son but, l'étape où il en est, sa fortune, son renom, et qui est ton rival.

| Fichier | Rôle |
|---|---|
| `histoire/Notables.java` (nouveau) | `nombre()`, `suivre()`, `completer()`, `aspiration()`, `journee()`, `vivre()`, `rival()`, `role()`, `lignes()`, `but()`, `troisieme()`, sauvegarde ; essais `joursPourEssai()`, `retirerPourEssai()` |
| `histoire/Joueur.java` | `notables()`, format 25, avancée chaque jour, la page « Les notables d'ici », le rôle dans « Qui es-tu ? » ; `notablesPourEssai()` |

Essai m25 `NOTABLES=1` (étapes 340 à 349), à Ulanewen :
- 4 notables (deux qui veulent protéger les leurs, une érudite, une marchande) ;
- une notable reçoit le même but que le joueur (diriger sa ville) : elle devient sa rivale ;
- en 800 jours (environ 9 ans), 13 nouvelles. Deux buts sont atteints, chacun repart sur un autre but, et la rivale en est à « Financer un bâtiment public » ;
- au départ de Naedor, Isimir prend sa place ;
- après sauvegarde et rechargement, tout est identique. 2 notables ont un corps à l'écran.

**Limites.** Les étapes des notables sont **résumées** : « se marier » ou « ouvrir un atelier » ne changent encore ni la Foule ni l'économie. Elles le feront avec la vie quotidienne (H4), la famille vivante et la noblesse. Le rival n'agit pas encore contre toi.

### Lot H4 — la journée de chacun, par ses besoins (fait le 2026-11-17)

**Plus d'horaire écrit.** Avant, un villageois suivait une grille fixe : dormir avant 6 h, manger à midi, travailler jusqu'à 18 h. Maintenant, ce sont **ses besoins** qui choisissent, à la Sims (`Besoins.java`). Chaque besoin va de 0 (comblé) à 1 (pressant) :
- **la faim** monte en 8 h, trois fois moins vite en dormant. Un repas la comble ;
- **le sommeil** monte en 16 h d'éveil. Une nuit chez soi le comble en 7 h ;
- **l'envie** (de se distraire) monte en 8 h de travail, en 24 h sinon. Les loisirs, les jeux et les discussions la comblent ;
- **la fatigue** et **le besoin de compagnie** restent ce qu'ils étaient ;
- **le besoin d'argent** : sa bourse comparée à dix journées de salaire.

**Le choix.** Chaque activité reçoit un score ; la plus forte l'emporte, avec un petit avantage (0,15, `GARDER`) pour celle qu'on fait déjà.
- **Dormir** : très fort la nuit (sommeil × 1,2 + 1). Le jour, seulement si l'on tombe de sommeil.
- **Manger** : faim × 1,3, au-dessus de la moitié de la faim.
- **Travailler** :
  - pendant ses heures (0,75, plus 0,3 si l'on est pauvre) ;
  - hors de ses heures, seulement si l'on est très pauvre.
- **Loisirs** : l'envie, la compagnie, un bonus le soir. Les vieux s'y attardent.
- **Pause** : quand on est épuisé au travail.
- Les enfants jouent, les vieux (plus de 65 ans) ne travaillent plus.

**Les heures de chacun** viennent de son métier (`Quotidien.ouverture` et `fermeture`). On se lève une heure avant d'ouvrir (entre 3 h et 8 h) et l'on veille 16 h. Personne n'a d'horaire de coucher écrit : le boulanger, qui commence à 3 h, se couche vers 19 h parce que son sommeil monte plus tôt.

**L'argent.** Chaque villageois a une **bourse** et un **salaire** (0,6 cuivre de l'heure, plus sa compétence / 200, tirés de sa fiche).
- Il gagne son salaire en travaillant. Les ouvriers du joueur ne gagnent rien à l'écran : c'est le joueur qui les paie.
- Il paie ses repas : 1 cuivre à la boulangerie, 3 à la taverne. Chez lui, il mange ses vivres.
- Il paie aussi la bière : 2 cuivre à la taverne.
- Le riche qui a envie de compagnie va dîner à la taverne. Celui qui n'a pas un sou mange chez lui et travaille plus tard.
- Pour les **gens importants**, chaque gain et chaque dépense est reporté sur leur fiche (`Habitants.bourses` → `Gens.depenser`, sur le fil de l'histoire).

**Loin des yeux** (`Gens.avancer`). Chaque jour, les gens importants qui n'ont pas de corps vivent une **journée résumée** : 8 h de salaire, moins 2 cuivre de repas. Les notables ont la leur (H3). Les ouvriers du joueur reçoivent ici leur salaire.

**Ce qu'on en voit.** « Qui es-tu ? » dit comment vit la personne : « Ses journées : debout vers 4 h, elle travaille aux champs de 5 h à 20 h, achète son pain à la boulangerie et passe ses soirées chez elle. » (`Gens.journee`). La fiche de débogage d'un villageois montre ses besoins et sa bourse.

| Fichier | Rôle |
|---|---|
| `Besoins.java` (nouveau) | `choisir()`, `user()`, `initialiser()`, `horaires()`, `pauvrete()` ; les constantes des besoins et des prix |
| `Villageois.java` | `sommeil`, `envie`, `gain`, `bourse`, `salaire`, `debut`, `fin`, `lever`, `coucher`, `vieux`, `important` |
| `Habitants.java` | le choix par les besoins, le lieu du repas et des loisirs selon la bourse, `payer()`, `bourses` ; l'habitant qui prend corps reçoit sa bourse, ses heures et les besoins de l'heure |
| `histoire/Gens.java` | `Foyers` : `argent`, `salaire`, `debut`, `fin`, `vieux` ; `depenser()`, `avancer()`, `journee()` |
| `histoire/Joueur.java` | la journée résumée chaque jour, la phrase « Ses journées » dans « Qui es-tu ? » |
| `FirstScreen.java` | reporte les bourses des gens importants sur leur fiche |

Essai m25 `BESOINS=1` (étapes 350 à 359) :
- **48 h de besoins seuls**, sans la marche :
  - un paysan travaille de 4 h à 20 h avec deux pauses, dort de 20 h à 4 h, mange 3 à 4 fois par jour ;
  - le boulanger travaille de 3 h à 13 h et dort de 19 h à 3 h ;
  - le vieux ne travaille pas ;
  - l'enfant joue ;
  - le même paysan sans un sou travaille 26 h au lieu de 23 et mange moins.
- **À l'écran, 80 villageois** : à 7 h 30, 55 au travail ; à 12 h 36, 31 à table ; à 20 h, 33 couchés et 16 en loisirs ; à 23 h 30, 61 couchés.
- **Les bourses** des 4 notables (les seuls gens importants de cette partie) montent de 5 à 13 cuivre par jour sur 10 jours. La journée résumée des proches et des ouvriers n'a pas été vérifiée par cet essai.

**Limites.**
- Les achats des villageois ne passent pas encore par l'économie de la ville : le pain acheté ne baisse pas le stock du boulanger.
- La journée résumée ne joue que l'argent.

### Lot H5 — tes ouvriers au travail, sous tes yeux (fait le 2026-11-17)

**Où ils sont.** Tes ouvriers sont de vrais habitants (H2) et des gens importants : ils ont un corps en priorité. Ils travaillent **dans le bâtiment de ton atelier** (`Atelier.batiment`), aux heures de son métier (H4).
- Avant, seuls les ateliers bâtis sur un terrain libre les recevaient. Maintenant c'est aussi le cas d'un atelier racheté.
- Ils font les gestes de leur métier devant la porte. Ils ne gagnent rien à l'écran : c'est toi qui les paies, chaque jour, par la caisse de l'atelier.

**Leur ouvrage se fait sous tes yeux.**
- Chaque heure de travail d'un de tes ouvriers à l'écran (`Villageois.ouvrage`) fabrique tout de suite **1/8 de journée** dans l'économie de la ville (`Affaires.ouvrage` → `EconomieVille.ouvrageVu`) : la matière baisse et le produit monte.
- Cet ouvrage est retiré de la journée de l'atelier (`Agent.faitVu`) : rien n'est compté deux fois, ni la production ni les salaires.
- Loin de toi, l'atelier tourne comme avant, en une fois par jour.

**Ce que tu vois.** À moins de 15 m (`PROCHE_OUVRAGE`), le bandeau dit ce qui vient de se faire : « Miwyn : +0,2 marteau de forge ; −0,2 lingot de fer, −0,1 sac de charbon. » S'il n'y a rien à faire, il le dit aussi : « … il manque de la matière, ou la vitrine est pleine. »

**Leur compétence** monte chaque jour à l'atelier (H1), et leur fiabilité suit.

**Correction de H2.** Les corps « en sursis » (ni voulus, ni encore assez loin pour être oubliés) prenaient les places libres avant les gens voulus. Dans un village plein, un nouvel important n'avait donc jamais de corps. Maintenant, les voulus passent d'abord.

| Fichier | Rôle |
|---|---|
| `histoire/EconomieVille.java` | `Agent.faitVu`, `ouvrageVu()` ; la journée de l'atelier du joueur en tient compte |
| `histoire/Affaires.java` | `ouvrage()`, `HEURES_OUVRAGE` ; essais `atelierIciPourEssai()`, `stockPourEssai()` |
| `histoire/Gens.java` | `Foyers.ouvrier` ; l'ouvrier va au bâtiment de son atelier |
| `Villageois.java`, `Besoins.java` | `ouvrier`, `ouvrage` (les heures faites sous les yeux) |
| `Habitants.java` | `ouvrages`, `PROCHE_OUVRAGE` ; les voulus avant les corps en sursis ; `etatFoyers()` compte les importants et les ouvriers |
| `FirstScreen.java` | reporte l'ouvrage à l'histoire |

Essai m25 `OUVRIERS=1` (étapes 360 à 369) :
- une forge rachetée dans le bâtiment 4 d'Ulanewen, avec 3 ouvriers (Naedor, Miwyn, Naelas). Après la répartition, ils ont un corps, leur poste est le bâtiment 4, ils travaillent de 6 h à 19 h ;
- le joueur, devant la forge, voit 8 heures d'ouvrage en 150 s (marteaux de forge, puis haches). Le fer et le charbon baissent, et 0,84 journée est « faite sous les yeux ».

**Limites.**
- Pas encore de geste de paie à l'écran : le salaire part de la caisse chaque jour.
- Les ouvriers restent devant la porte : on ne voit pas l'intérieur des bâtiments.

### Lot I — la famille vivante (fait le 2026-11-17)

**I1 — sous ton toit.**
- Ton conjoint (si vous vivez ensemble) et tes enfants qui ne sont pas mariés **habitent ta maison** (`Gens.famille`, `Foyers.famille`). Ils y ont un corps, en priorité, et `Habitants` ne les exclut plus de la maison du joueur.
- Un conjoint venu d'une autre ville vit chez toi.
- Ce sont des habitants comme les autres :
  - les enfants jouent, puis travaillent à 14 ans ;
  - le conjoint travaille ;
  - tous mangent et dorment selon leurs besoins (H4).
- **Les gens vieillissent enfin pendant la partie.** `Gens.annee()` est l'année du monde, plus les jours passés en jeu. Avant, les âges des habitants restaient figés à l'année de départ.

**I2 — grandir.**
- **L'apprentissage.** Chez toi, dans « Ta famille… », on peut placer un enfant de 12 ans ou plus chez un maître :
  - on choisit parmi les ateliers de la ville, les champs, la forêt et la mine ;
  - le maître demande 30 cuivre (`FRAIS_APPRENTI`) ;
  - le lieu devient celui de sa fiche (`Gens.placer`, sauvegardé avec les lieux fixés) ;
  - chaque jour, l'apprenti fait un effort de 4 dans la compétence du métier (`APPRENTISSAGE`). Sa fiche est gardée ;
  - il travaille chez son maître dès 14 ans.
- **L'instruction.** Le temps passé avec ses enfants (« Passer du temps avec tes enfants ») devient leur Lettres : jusqu'à 120 selon l'éducation reçue.
- **Le mariage.** Chaque nouvel an, un enfant majeur et seul se marie une fois sur trois (`MARIAGE_AN`) :
  - le conjoint est un habitant seul de sa ville, d'un âge proche (`ECART_MARIAGE`, 8 ans humains) ;
  - le couple s'installe dans sa propre maison, et l'enfant quitte la tienne ;
  - le mariage est gardé par l'ajout de l'enfant, donc sauvegardé sans changer de format ;
  - le carnet et le bandeau l'annoncent.
- **Ce qu'on en voit.** « Ta famille… » montre de chacun son âge, s'il est majeur, s'il est instruit, son apprentissage (avec sa compétence) et s'il est marié.
- **Un corps qui change de maison ou de travail** (un mariage, un apprentissage) y va sans disparaître (`Habitants.incarner`).

**I3 — l'héritier a vécu.** À la succession, l'héritier garde **ce qu'il a appris de sa vie à lui** (sa fiche) :
- ses compétences, ses attributs et son focus, s'ils dépassent ce qu'il tient de son parent ;
- son caractère, mêlé à celui du parent ;
- son propre argent.

La scène le dit : « Ce que tu sais de ta vie à toi : Bois 140. »

| Fichier | Rôle |
|---|---|
| `histoire/Gens.java` | `annee()`, `placer()`, `famille()`, `Foyers.famille` ; les âges en années de jeu |
| `histoire/Famille.java` | `apprendre()`, `apprentissage()`, `marie()`, `marier()`, `choisirMaitre()`, `placer()` ; la succession tient compte de la fiche de l'héritier ; essais `enfantPourEssai()`, `placerPourEssai()`, `marierPourEssai()`, `joursPourEssai()` |
| `histoire/Notables.java` | les âges en années de jeu |
| `histoire/Joueur.java` | `ajout(id)` |
| `Habitants.java`, `Villageois.java` | la famille dans la maison du joueur ; la maison et le travail qui changent |

Essai m25 `FAMILLE2=1` (étapes 370 à 379), à Ulanewen :
- une maison, une conjointe qui vit avec toi, trois enfants de 8, 13 et 19 ans (humains). Tous les quatre ont un corps dans ta maison : à 10 h, la conjointe et l'aîné travaillent et les petits jouent ; le soir, la petite dort ;
- la fille de 13 ans est placée chez le forgeron : Forge 0 → 16 en 120 jours, Lettres 50 ;
- le fils de 19 ans épouse Saewen. Son corps part dans la maison 41 ;
- le fils reprend le flambeau : la scène dit « Ce que tu sais de ta vie à toi : Bois 140 », et sa fiche a Bois 140.

**Limites.**
- Les enfants restent des enfants à l'écran jusqu'à ce que leur corps soit refait (en s'éloignant puis en revenant).
- On ne choisit pas encore le conjoint de ses enfants. Il n'y a pas encore de dot, d'alliance ou de mariage arrangé (cela viendra avec la noblesse).
- Les petits-enfants ne naissent pas encore.

## Les fermes visibles (14.3)

Le chantier se fait en quatre lots :
- **J1 — les champs visibles** : les champs, prés, vergers et friches autour des villages, peints au sol selon la saison ;
- **J2 — les bêtes aux prés** : moutons, vaches et chevaux qui paissent ;
- **J3 — le travail aux champs** : les PNJ (et ta famille) labourent, sèment, fauchent et moissonnent sous tes yeux ;
- **J4 — ton champ** : tes lopins (phase 11) sont de vrais champs sur le terrain, choisis sur place comme le terrain libre (G5).

### Lot J1 — les champs visibles

**Le placement** (`Villages.champs`). Les champs se posent en anneaux autour du village bâti :
- chaque anneau fait 34 m de profondeur (`BANDE`), séparé du suivant par un chemin de 4 m (`ALLEE`). Il est coupé en blocs d'environ 46 m (`BLOC`) ;
- la surface voulue suit le **plus fort** de la ville : 110 m² par habitant (`SURFACE_PAR_HABITANT`). Les anneaux se remplissent l'un après l'autre, depuis un angle au hasard. Un anneau à moitié rempli fait donc un arc d'un seul tenant ;
- un bloc est refusé s'il touche l'eau ou la mer, une route qui part du village (`COULOIR` de 6 m) ou un autre village. Il est aussi refusé si la pente dépasse 0,32 (`PENTE_PRE`). Entre 0,16 (`PENTE_CHAMP`) et 0,32, ce n'est qu'un pré ;
- un bloc cultivé se coupe en **lanières** de 7 à 13 m de large, séparées par une bande d'herbe de 0,7 m (`BALK`). Leurs sillons vont le long de la lanière ;
- quelques blocs entiers sont des **prés** (14 %) ou des **vergers** (8 %, sur les deux premiers anneaux). Les arbres d'un verger sont plantés tous les 6,5 m (`PAS_VERGER`) ;
- dans le premier anneau, une lanière sur trois environ est un **potager** ;
- **le déclin** : si la ville a perdu des habitants depuis son plus fort, une part des lanières retourne à la **friche**. La probabilité vaut (1 − habitants/pic) × (0,6 + 0,5 × anneau) : les plus éloignées partent les premières ;
- **la croissance** : le village se refait quand son nombre de maisons change, et ses champs avec lui.

Le hasard des champs est à part : le village lui-même ne change pas.

**Les arbres sauvages** ne poussent ni dans les champs ni à moins de 3 m de leur bord (`DEGAGE`) : les chemins entre les blocs restent nus (`Villages.dansChamp`). La friche, elle, laisse revenir les arbres.

**La texture** (`Rues`). Les champs sont peints dans une deuxième texture de 1024 × 1024, posée comme celle des rues. Chaque texel garde trois choses :
- la sorte et une variante ;
- le sens des sillons ;
- la distance au bord, en décimètres.

Les sillons se calculent par rapport à une **ancre** fixe (`decalageX`, `decalageY`). Ils ne sautent donc pas quand la texture se recentre. L'ancre ne bouge qu'au-delà de 600 m.

**Le rendu** (`tuile.frag`). La saison vient du calendrier de la phase 11 (`u_annee` : 0 au premier jour du printemps, 1 au dernier de l'hiver). Chaque champ a son propre décalage de quelques jours.

Le **blé** passe par toutes les étapes de l'année :

| Moment | Étape |
|---|---|
| 0 – 0,06 | labour : sillons bruns, crête claire, creux sombre |
| 0,06 – 0,12 | semis : quelques pousses sur les crêtes |
| 0,12 – 0,25 | pousse : les rangs verdissent et s'élargissent |
| 0,25 – 0,36 | blé vert : des tiges en traits verticaux, une onde de vent qui passe |
| 0,36 – 0,47 | épis : le champ jaunit, puis il est mûr (or) |
| 0,47 – 0,56 | moisson : les faucheurs avancent le long des rangs, trois rangs à la fois |
| 0,47 – 0,68 | gerbes posées sur le champ coupé |
| 0,56 – 0,84 | chaume : des rangs de tiges coupées, la terre entre eux |
| 0,84 – 1 | hiver : la terre nue (la neige la couvre quand il neige) |

Les autres sortes :
- **le potager** : la terre labourée au printemps, puis des touffes en rangs qui grossissent. On les récolte peu à peu à l'automne ; l'hiver, la terre est nue ;
- **le pré** : de l'herbe, fauchée au début de l'été (les brins sont couchés). Le foin sèche en andains dorés, puis l'herbe repousse ;
- **le verger** : de l'herbe, avec ses arbres ;
- **la friche** : de grandes herbes sèches, où l'on devine encore les anciens sillons.

De loin (au-delà de 0,32 m par pixel), le détail des sillons s'efface : il ne reste que la couleur de l'étape.

| Fichier | Rôle |
|---|---|
| `Villages.java` | `Village.champs`, `Village.rayonChamps`, `champs()`, `altitudeChamp()`, `autreVillage()`, `dansChamp()` ; les constantes `CHAMP`, `BLE`, `LEGUMES`, `PRE`, `VERGER`, `FRICHE` |
| `Rues.java` | la texture des champs (`champs()`, `dessinerChamps()`, `remplirChamp()`), l'ancre des sillons |
| `RenduTuiles.java` | `annee` ; l'unité de texture 6 et les uniformes `u_champs`, `u_champDecalage`, `u_annee` |
| `FirstScreen.java` | le moment de l'année depuis le calendrier du joueur (`anneeChamps` pour les essais) |
| `Vegetation.java` | pas d'arbres sauvages dans les champs |
| `assets/shaders/tuile.frag` | `labour()`, `tiges()`, `cereale()`, `legumes()`, le pré, le verger et la friche |

Essai m25 `CHAMPS=1` (étapes 380 à 389), village de 44 maisons (259 habitants, 276 au plus fort) :
- 68 lanières de blé (20 300 m²), 4 potagers, 4 prés, 1 verger et 7 friches, jusqu'à 220 m du centre ;
- les captures montrent chaque étape du blé, le potager, le pré fauché avec ses andains, et l'anneau des champs vu de loin.

**Limites.**
- Les champs ne s'affichent que de près (moins de 1,5 m par pixel, comme les rues).
- Il n'y a pas encore de bêtes aux prés (J2), ni personne aux champs (J3).
- Les lopins du joueur ne sont pas encore des champs du terrain (J4).
- Les champs ne suivent pas le relief fin : ils sont peints sur le sol tel qu'il est, sans être aplanis.

## La refonte de l'interface (avant la suite des fermes)

Le chantier se fait en cinq lots :
- **K1 — la base** : le menu d'accueil, les mondes multiples, les paramètres, le plein écran, le menu Échap, la touche ² pour le debug, les musiques ;
- **K2 — la création du monde en spectacle** : la Phase 1 (le globe se forme) et la Phase 2 (5000 ans d'histoire) montrées en grand ;
- **K3 — la création du personnage** : ton personnage en grand, que l'on tourne et habille ; les choix ; une fiche qui se remplit ;
- **K4 — les vignettes** : un tableau par étape de ton histoire, avec les sprites du jeu et la vue 3/4 de ta ville natale ; tes parents et ta fratrie avec leur vrai visage ;
- **K5 — les sons** : les bruitages (clics, pages, pas…), fabriqués pour le jeu.

### Lot K1 — la base

**Le démarrage** (`Thaloran`). Le jeu s'ouvre sur le menu d'accueil (`MenuScreen`), en plein écran si les paramètres le veulent. Les essais (variable `SORTIE`) vont tout droit au monde 42, sauf avec `MENU=1`.

**Le menu d'accueil** (`MenuScreen`) :
- à gauche : le titre, une phrase qui change toutes les 9 s, puis le menu ;
- le menu propose **Continuer** (le dernier monde joué, avec son état), **Créer un monde**, **Charger un monde**, **Paramètres** et **Quitter le jeu** ;
- à droite : la page ouverte, qui apparaît en fondu ;
- entrer dans un monde fait un fondu au noir, et la musique s'éteint en fondu.

**Le fond animé** (`FondMenu`, refait le 2026-11-17 dans le style d'une image de référence donnée par l'utilisateur). C'est une scène de pixel art de 480 × 270 pixels, peinte une fois au lancement puis agrandie sans lissage. Elle n'utilise aucune image toute faite. Toutes les couleurs viennent de `palettes/sol.png`.

Le tracé :
- en aplats, avec une trame (Bayer 4×4) seulement au milieu des transitions ;
- des montagnes à facettes droites, coupées en arrêtant le déplacement du point milieu, avec un col où le soleil se couche.

La scène, du fond vers l'avant :
- un ciel de crépuscule, du bleu nuit au violet, au cramoisi puis à la braise, avec des bandes de nuages ;
- deux chaînes de montagnes éclairées du côté du soleil, et un château sur un piton rocheux ;
- des collines violettes, des tombes et des colonnes brisées ;
- un chemin dallé en perspective ;
- une arche gothique en ruine. Elle est à contre-jour, avec un liseré de braise du côté du soleil, de la mousse et du lierre ;
- deux grands arbres aux feuilles cramoisies et des buissons fleuris.

Ce qui bouge :
- les fenêtres du château vacillent ;
- deux brumes passent ;
- des rayons de lumière pulsent ;
- 28 lucioles volent et 70 pétales tombent.

**Créer un monde** :
- le nom (tiré au hasard, par exemple « Le Val des Rois morts ») ;
- la graine (au hasard ; la même graine donne toujours le même monde) ;
- la durée de l'histoire : 1000, 2000, 3500 ou 5000 ans.

**Les mondes** (`Mondes`). Chaque monde a son dossier `Thaloran/mondes/<id>/` :
- sa fiche `monde.properties` : nom, graine, durée, création, dernière visite ;
- ses sauvegardes : monde, histoire, joueur, relations.

L'ancien monde 42, rangé à la racine de `Thaloran/mondes`, reste jouable sous le nom « Le premier monde ». **Charger un monde** montre une carte par monde, avec son état : pas encore formé, histoire pas encore écrite, prêt, ou « ton personnage t'y attend ». On peut le jouer ou l'effacer. Effacer demande de cliquer deux fois.

**Les paramètres** (`Parametres`, `Panneaux.parametres`) sont gardés d'une partie à l'autre et s'appliquent tout de suite :
- plein écran ou fenêtre ;
- le volume de la musique et celui des bruitages.

**En jeu** :
- **²** montre ou cache la fenêtre de debug. Elle est cachée par défaut ; cachée, elle ne prend plus la souris ;
- **Échap** ferme d'abord une fenêtre ouverte. Sinon, il ouvre le menu du jeu (`MenuPause`) : Reprendre, Paramètres, Menu principal, Quitter le jeu ;
- tant que ce menu est ouvert, l'écran s'assombrit, le temps du jeu s'arrête et les touches ne répondent plus ;
- quitter arrête l'histoire et enregistre le joueur.

**La musique** (`Sons`). Un dossier de `assets/musics` est une musique, et ses morceaux s'enchaînent dans un ordre mêlé. On passe de l'une à l'autre en fondu de 2 s :

| Moment | Dossier |
|---|---|
| le menu | `main_menu` |
| le monde se forme, puis son histoire s'écrit | `creation_monde` |
| la création du personnage | `select_personnage_history` |
| en jeu | silence (pour l'instant) |

`Sons.effet(nom)` joue `assets/sons/<nom>.wav` s'il existe. Les boutons appellent déjà `clic`, `survol` et `page` (les fichiers viendront avec K5).

**Le style commun** (`Theme`) :
- les polices Georgia ;
- des cadres ornés à double filet d'or avec un losange à chaque coin ;
- des boutons avec un losange et une lueur au survol ;
- des curseurs à bouton en losange et des options exclusives.

| Fichier | Rôle |
|---|---|
| `Thaloran.java` | le menu au démarrage, le déménagement de `~/Politics` vers `~/Thaloran`, `lancer(fiche)`, `menu()`, les fondus de musique |
| `MenuScreen.java`, `FondMenu.java` | le menu d'accueil et son fond animé |
| `MenuPause.java` | le menu Échap |
| `Panneaux.java` | la page des paramètres, commune aux deux menus |
| `Theme.java` | le style des menus |
| `Mondes.java` | les mondes du joueur et leurs dossiers |
| `Parametres.java` | les réglages gardés |
| `Sons.java` | la musique et les bruitages |
| `FirstScreen.java` | le monde joué (`fiche`), la durée de l'histoire, ², Échap, la musique du moment |
| `FenetreDebug.java`, `Interface.java` | `afficher()` / `affichee()` ; `prendreEchap()` |

Essais m25 :
- `MENU=1` : les quatre pages du menu ;
- `CHAMPS=1` : il se termine maintenant par le jeu sans debug et par le menu Échap ;
- `SON=1` : la musique est jouée pendant un essai.

**Limites.**
- La Phase 1 et la Phase 2 se montrent encore comme avant (K2).
- La création du personnage a son écran depuis K3. Les vignettes et la famille visible viendront avec K4.
- Pas encore de bruitages (K5).

### Lot K2 (début) — la barre de la création du monde (fait le 2026-11-17)

Pendant la création, une barre en bas de l'écran (`BarreCreation`) dit ce qui se passe et quand. Le bouton du haut est caché, et l'écran de chargement attend l'entrée dans le monde.

Le panneau contient :
- les trois étapes, I · La terre, II · Les peuples et III · L'histoire. L'étape en cours est en or vif, celles qui sont finies en or sombre ;
- un titre qui change selon l'avancée ;
- le moment ;
- une barre d'or avec son pourcentage ;
- le dernier fait de la chronique ;
- le choix de la vitesse.

| Étape | Titres | Moment | Barre |
|---|---|---|---|
| La terre (tectonique) | La croûte se fend en plaques, Les continents dérivent et se heurtent, Les montagnes se lèvent, Les océans s'ouvrent | « Il y a 2 465 millions d'années » | 0 à 94 % |
| La terre (fleuves) | Les pluies tombent, les fleuves naissent, Les fleuves creusent leur lit, Les glaciers creusent les vallées | « Il y a 7,3 millions d'années » | 94 à 100 % |
| Les peuples | Les peuples s'éveillent | les premiers berceaux | une lueur qui va et vient |
| L'histoire | Les premières villes sont fondées, Les royaumes naissent et se font la guerre, Les dynasties se succèdent, Le monde se prépare à ta naissance | « An 542 sur 1 000 », avec le dernier fait de la chronique toutes les 2,5 s | l'année sur la durée |

La vitesse de la formation de la terre se règle sur Calme, Rapide ou Très rapide, soit 10, 60 ou 250 pas par seconde. Le calcul limite de toute façon la vitesse : un monde met environ 90 s à se former.

**Les mêmes tons du début à la fin.** Avant, le globe en formation prenait les couleurs vives du debug (bleu franc, verts saturés). À la fin, le rendu des tuiles prend ses tons dans `palettes/sol.png`. Maintenant, `Palette.ton()` prend pour chaque cellule la même case de `sol.png` que `materiau()` dans `tuile.frag` :
- la mer : rangée 13, plus sombre avec la profondeur (colonnes 0 à 3) ;
- la terre : selon le biome (table `TONS_BIOMES`), avec un grain fixe par cellule qui décale d'une colonne ;
- les lacs et les fleuves : rangée 13, colonne 5.

Pendant la tectonique, `monde.biome` n'est recalculé qu'avec le climat, tous les 100 Ma. Une terre qui vient d'émerger y serait donc encore « océan » et passerait en bleu derrière les plaques. `tonCellule` choisit donc le biome à chaque rafraîchissement (`Biome.choisir`), d'après l'altitude du moment.

Le globe vu de loin (`preparerFond`) prend les mêmes tons. Tant que les tuiles se chargent pendant la création, ce globe reste dessous, ce qui évite un trou noir au passage.

**Quelle carte on regarde** (`EtiquetteCarte`). Une petite plaque en haut à droite affiche « Carte : Biomes » et « Tab : changer de carte ». Le nom brille un instant quand on change de carte. Les noms vont de Plaques à Routes, dans l'ordre de `NOMS_MODES`. La plaque se montre dans trois cas :
- pendant la création ;
- sur le globe vu de loin ;
- dès qu'une autre carte que les biomes est posée sur le monde.

Deux autres corrections :
- la chronique a maintenant ses accents (à, trône, héritier, région, sécession) ;
- `Meteo.dispose()` attend la fin de son fil avant de libérer son image. Sans cela, le jeu plantait dans `gdx64.dll` en quittant un monde pendant que la météo écrivait.

Pour tester dans m25 : `MENU=1` et `BARRE=1`.

### Lot K3 (première version) — l'écran de création du personnage (fait le 2026-11-17)

Tant que le personnage se crée, ses étapes ne passent plus par la fenêtre de texte. Elles s'affichent dans un écran à elles (`EcranCreation`), sur le fond du menu voilé (`VOILE` 0,5). L'écran a trois colonnes.

**1. Ton personnage.**
- Le personnage est peint par le peintre du jeu (`Peintre`), en grand, à la plus grande échelle entière qui tient.
- Il fait un tour sur lui-même : il change de direction toutes les 2,2 s (`TOUR_AUTO`), dans l'ordre face, trois-quarts, profil, dos. Les flèches « » le tournent à la main ; le tour automatique s'arrête alors 8 s (`PAUSE_TOUR`).
- Il apparaît dès qu'on connaît son peuple, sa ville et son sexe (`Creation.apercu()`). Avant, un texte dit qu'il se dessinera.
- En dessous, les réglages de l'apparence, chacun en « valeur » :

  | Réglage | Valeurs |
  |---|---|
  | Visage | une autre graine : visage, peau, cheveux |
  | Coiffure | Rasée, Courte, Mi-longue, Longue, Queue, Tresse, Chignon, Crête |
  | Barbe | pour les hommes seulement |
  | Habit | Tunique, Chemise, Robe |
  | Couleur, Braies | une pastille de couleur |
  | Cape | Aucune, Cape, Capuche |
  | Couleur de la cape | une pastille de couleur |

- Les couleurs sont des rangées de `palettes/sol.png` (`Aspect.TISSUS`, 16 tissus).

**2. Ta fiche** (`Creation.resume()`). Elle se remplit au fil des choix : peuple, lieu de naissance, sexe, âge, nom, famille, père, mère (avec † s'ils sont morts), fratrie, conjoint.

**3. L'étape.** Elle montre le titre, le texte, puis les choix dans une zone qui défile. Chaque choix s'allume au survol. Les touches 1 à 9 choisissent directement. Pour le nom, il y a une saisie, que l'on valide avec Entrée. Échap ferme l'écran, et la création reprendra où on l'a laissée.

**Le lieu de naissance se choisit sur une carte** (`EcranCreation.CarteMonde`), en deux étapes (`Creation.royaumes()` puis `Creation.villes(etat)`). Une étape porte sa carte dans `Scene.carte` (`CARTE_ROYAUMES`, `CARTE_VILLES`) et ce que montre chaque choix dans `Scene.lieux`.

1. **Le royaume.**
   - La carte est le globe étalé à plat (512 x 256, un pixel par case), donné par `FirstScreen.carteAPlat()` avec les tons du jeu et un relief ombré.
   - Elle est éclaircie de 20 % ; les grands fonds sont ramenés à 60 % vers le bleu des eaux claires (`Palette.tonEau`), sinon ils seraient presque noirs.
   - Les royaumes où vit ton peuple sont teintés (alpha 0,48, ou 0,7 au survol) et bordés d'or. Les autres terres sont un peu ombrées (0,3).
   - La capitale de chaque royaume possible est un losange d'or ; on peut l'attraper au survol même si le royaume ne fait que quelques pixels.
   - Un clic choisit. Le survol allume aussi la ligne de la liste.
2. **La ville.**
   - La carte se resserre autour du royaume : sa boîte et celle de ses villes, marge 1,6, au moins 10 cases de large, toujours en 2:1.
   - Cette zone est repeinte en détail depuis le terrain (`FirstScreen.carteDetail`), en 260 x 130 points (`DETAIL_L`, `DETAIL_H`), dans un fil à part (`atelierCarte`). Chaque point prend le relief fin du terrain (niveau 6), le biome, les fleuves et les lacs, plus un relief ombré.
   - Hors du royaume, la terre s'assombrit ; la frontière est un filet d'or.
   - Tant que le détail n'est pas prêt (moins d'une seconde), on voit la carte du monde agrandie.
   - Les villes sont des carrés, la capitale un losange. On choisit en cliquant sur la plus proche (14 px au plus).

**L'apparence choisie** (`histoire/Aspect.java`) :
- elle est rangée dans la fiche (`Fiche.aspect`). Elle est partagée, et non copiée, entre les étapes, pour que « Revenir » ne défasse pas les retouches ;
- elle est enregistrée avec le joueur (format 26 de `Joueur`) ;
- chaque réglage vaut -1 tant qu'on n'y a pas touché : on garde alors ce que donne la graine ;
- la graine du personnage est `Aspect.graine(id, visage)`. Avant, elle dépendait aussi du bouton « D'autres noms » ;
- dans le monde, `habiller()` part de `aspect.tenue(...)`, et les armes et armures du sac s'y ajoutent ; la coiffure vient de `aspect.traits(...)`. Dans l'aperçu, le personnage n'a rien en main.

### Lot K4 (première version) — la vignette de chaque étape (fait le 2026-11-18)

Quand une étape n'a pas de carte, une vignette (`Vignette.java`) prend sa place en haut de la colonne de l'étape. Ta famille s'y tient debout devant les environs de ta ville natale.

**Qui est là** (`Creation.vignette()`, qui rend un `Creation.Tableau` de `Creation.Figure`) :
- dans l'ordre : le père, la mère, toi, ton époux ou ton épouse, puis la fratrie de la plus âgée à la plus jeune. 8 personnes au plus (`FIGURES_MAX`) ;
- l'âge montré dépend de l'étape (`ageEtape()`) :

  | Étape | Âge (en années humaines) |
  |---|---|
  | Petite enfance | 4 |
  | Enfance | 9 |
  | Adolescence | 14 |
  | Jeunesse | 18 |
  | les autres étapes | l'âge d'entrée dans le monde |

- on calcule l'année de l'étape. Un parent mort avant cette année n'est pas là, ni un frère ou une sœur pas encore né ;
- quelqu'un de moins de 13 ans (`AGE_ENFANT`) est peint en enfant, toi compris ;
- l'époux ou l'épouse n'apparaît qu'à partir de 17 ans.

**Comment chacun est peint :**
- chaque proche est peint avec la graine qu'il a dans le monde (son id) : `Tenue.tirer(id, …)` et `Traits.tirer(id, …)`, comme dans `Personnage`. On reconnaîtra donc ses parents en les croisant ;
- toi, tu portes les réglages du portrait (`Aspect`). Enfant, tu gardes tes couleurs et ta coiffure, sans barbe ;
- personne ne porte d'armes ni d'armure (`Vignette.desarmer`) ;
- ceux qui sont à ta gauche se tournent vers toi (bas-droite), ceux de droite aussi (bas-gauche). Toi, tu regardes en face ;
- l'échelle dépend du nombre de personnes : 2 jusqu'à 4, 1,5 jusqu'à 6, 1,25 au-delà.

**Le décor :**
- ce sont les environs de la ville, sur 8 km de large (`FOND_KM`), peints en 240 x 110 points par `FirstScreen.carteDetail`. Le niveau de détail du terrain est 9 (`NIVEAU_FOND`), contre 6 pour la carte de la ville ;
- le relief ombré suit la vraie pente, quelle que soit la taille d'un point (`OMBRAGE_DETAIL`) ;
- le grain de couleur suit la grille des points, sinon on voyait de gros carreaux de près ;
- le décor ne se repeint que si la ville change ;
- derrière la famille se tiennent **les maisons de la ville**, 5 au plus. Ce sont les premières que le monde y bâtit, avec les mêmes graines que `Villages.placerMaison` : `Batiments.plan(melanger(ville * 1000003 + n))`. On les reconnaîtra en arrivant ;
- elles sont plus loin, donc réduites (`ECHELLE_MAISON` 0,33), et voilées selon leur éloignement. Leurs textures réduites sont lissées : la vignette a pour cela son propre `Batiments`, pour ne pas toucher au rendu du monde ;
- ce qui dépasse du cadre est coupé (`clipBegin`).

**Le survol** d'une personne donne son nom, son lien et son âge. Sinon, la vignette affiche la ville et l'année.

**Correction au passage :** la clé du cache du `Peintre` tient maintenant compte du sexe et de l'âge (enfant ou adulte). Avant, une même graine peinte adulte puis enfant resservait l'image adulte. C'était aussi le cas dans le portrait si l'on revenait changer de sexe.

Pas encore fait : la vraie vue 3/4 de la ville, rendue par le moteur du monde avec ses rues et ses arbres. Le décor reste le terrain vu de dessus, avec des maisons posées devant.

Pour tester dans m25 :
- `PERSO=1`, avec `EssaiPerso`, étapes 390 à 399. L'essai joue la création sur le monde d'essai, retouche l'apparence, entre dans le monde et vérifie le personnage. Il prend 2 à 3 minutes, sans recréer de monde ;
- `MENU=1` et `BARRE=1` : la terre ne dure plus que 500 Ma dans l'essai, pour environ 35 s en tout.
