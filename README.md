# Réalisation d’un gestionnaire de tas réparti partagé

## Description du projet :
Le projet met en œuvre la gestion des données des segments de mémoire partagée entre plusieurs processus sur différentes machines en développant un gestionnaire de tas partagé distribué (Distributed Shared Heap Manager - DSHM). Les principales fonctions comprennent la création de données, le contrôle d'accès en lecture/écriture et la synchronisation des données pour assurer la cohérence des données entre les processus. En plus, le projet prend en charge l'allocation et la libération dynamiques des données et améliore la tolérance aux pannes du système en utilisant un serveur miroir.
La mise en œuvre de ce projet repose sur l’utilisation de socket pour les communications entre les clients et les serveurs. Un serveur sera chargé de maintenir la correspondance entre les adresses et le nom des données (le symbole), ainsi que le suivi du nombre d’accès en cours pour chaque donnée.
Dans une deuxième phase, nous avons réaliser la tolérance en panne :
 - côté serveur : En créant des serveurs «miroirs» maintenant une copie de la table de
correspondance du serveur, les processus clients pourront basculer automatiquement
sur un serveur disponible
 - côté client : Appliqué la mécanisme de «heartbeat», le serveur peut facilement
détecter l’existence des clients
Lors des expérimentations, des simulations de fautes de serveurs et de client seront réalisées pour tester la robustesse du système dans des conditions réelles. Ce projet vise à fournir une solution robuste et fiable pour la gestion des données partagées entre processus répartis sur des machines distantes

## Guide d'installation :
Ce projet est 100% réalisé en Java, veuillez préparer votre environnement Java avant de l'utiliser, et comme ce projet utilise certaines des dernières versions de Java, il est recommandé d'utiliser l'environnement JDK21 (et plus).
Ce projet est 100% réalisé en Java, veuillez préparer votre environnement Java avant de l'utiliser, et comme ce projet utilise certaines des dernières versions de Java, il est recommandé d'utiliser l'environnement JDK21 (et plus).
NB : La communication entre les machines sont réalisées par des sockets, donc assurez-vous que les ports locale 8080,6060-6069 n'est pas occupée. Le port 8080 est le port du serveur, et les ports 6060-6069 sont des ports du client

## Guide d'installation :
Les fichiers de démarrage sont situés dans le dossier src/test. Avant le début des tests, il a besion d'activer StartMirror puis StartMirror.

## Introduction du répertoire :
Le dossier src contient cinq sous-répertoire : annotation, machine, rmi, test et utils, qui contiennent tous le code source du projet,sauf que test.

### 1. annotation
Ensembles d'interfaces annotation définie pour l'indiquer les fonctions spécifiques, appliqués au mécanisme de réflexion en Java. 
### 2. machine
L'entité base du system, inclu le serveur, le client et le mirror laucher. Chaque machine occupe un port pour recevoir les messages envoyées par les autre machines
### 3. rmi
Il existe trois inteface RMI pour réaliser une invocation à distance en besoin de simuler les erreus déplacé dans le system. 
### 4. utils
Certaine structure de données et mécanism utilisé dans le projet. 
### 5. test
Pour tester la fiabilité et l'efficacité du système, nous avons conçu plusieurs fichiers d'essai pour effectuer différents tests.
  - StartMirror / StartServer : démarrage du mirror laucher et serveur. 
  - Test : contrôler plusieurs clients dans une seule console. Le système traitera la demande sur l'entrée standard. Grâce aux fonctions de l'affichage, l'utilisateur peut observer la situation de chaque client et de chaque serveur pour vérifier la fonctionalité du programme.
  - AutoTest : Outre la manipulation manuelle des utilisateurs, nous mettons également en œuvre une exemple de tests automatisés. Cette programme créera automatiquement 10 clients et automatisera le processus Malloc->Write->Release->Read->Free. Par la suite, vous pouvez contrôler les clients individuels via la console.
  - ControlError : Un contrôleur d'erreurs pour déclencher activement les erreurs. Pandant ce test, vous pouvez sélectionner l'erreur souhaitée en tapant un numéro dans la console, y compris, mais sans s'y limiter, le dépassement du délai de transmission du message, la réponse erronée au message, etc.
  - Evaluation de la performance : pour vérifier la validité du system, correspond le section de l'expérience de calculation incrémentale dans le rapport
    1. CPTselfTest: automatise 100 tâches, 5 clients et calcule le cpt automatiquement
    2. CPTtime_ChangeNumberOfClients: un test dans lequel le nombre total de tâches reste inchangé et le nombre de clients exécutant les tâches change afin de tester la relation entre le temps d'exécution et le nombre de clients
    3. CPTtime_ChangNumberOfJobs : un test dans lequel le nombre de clients reste le même et le nombre total de tâches change

## FAQ
Q : Est-il possible de faire fonctionner le serveur sans faire fonctionner l'initiateur du serveur miroir ?

A : Oui, il est possible de faire fonctionner le serveur indépendamment du serveur miroir, mais veuillez noter que dans ce cas, le serveur ne sera pas en mesure de basculer instantanément vers le serveur miroir en cas d'erreur.

Q : Que dois-je faire s'il indique que le port est occupé ?

A: Si un port est occupé/une adresse est occupée, veuillez vérifier si le port 8080 et les ports 6060-6069 (et les ports 16060-16100 si vous effectuez des calculs de temps) sont occupés sur votre appareil. Si nécessaire, vous pouvez vérifier les ports 8080 et 6060-6069 (et 16060-16100 si vous effectuez des calculs de temps) dans StartServer, CPTime_ChangeNumberOfClients, CPTime_ChangNumberOfJobs, Test_client_1, Test_client_2, Test_client_3, Test. AutoTest, MirrorInitiator pour modifier les ports correspondants.

Q：Que dois-je faire si le message "Unrecognised identifier xxx" apparaît pendant la compilation ?

A：Veuillez mettre à jour votre version du JDK et assurez-vous que vous utilisez le JDK21 ou une version supérieure. Si le problème persiste après la mise à jour, veuillez contacter les membres de notre équipe.

## Membres de l'équipe :
Université Sorbonne，Informatique，SAR，M1 :

Runlin ZHOU : runlin.zhou@etu.sorbonne-universite.fr

Xiaoquan LI : xiaoquan.li@etu.sorbonne-universite.fr

Weida LIU : weida.liu@etu.sorbonne-universite.fr


## Remerciements :
Un grand merci au Professeur Pierre Sens de l'Université Sorbonne pour son précieux soutien et ses conseils tout au long de ce projet. Son expertise et son accompagnement ont été essentiels pour notre développement.
