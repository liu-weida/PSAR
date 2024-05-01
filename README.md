# Réalisation d’un gestionnaire de tas réparti partagé

## Description du projet :
Le projet met en œuvre la gestion des données des segments de mémoire partagée entre plusieurs processus sur différentes machines en développant un gestionnaire de tas partagé distribué (Distributed Shared Heap Manager - DSHM). Les principales fonctions comprennent la création de données, le contrôle d'accès en lecture/écriture et la synchronisation des données pour assurer la cohérence des données entre les processus. En plus, le projet prend en charge l'allocation et la libération dynamiques des données et améliore la tolérance aux pannes du système en utilisant un serveur miroir.

## Guide d'installation :
Ce projet est 100% réalisé en Java, veuillez préparer votre environnement Java avant de l'utiliser, et comme ce projet utilise certaines des dernières versions de Java, il est recommandé d'utiliser l'environnement JDK21 (et plus).

## Mode d'emploi
### 1. préparation
Téléchargez ce projet et compilez-le, et assurez-vous que les ports locale 8080,6060-6069 n'est pas occupée.
### 2. démarrer le serveur
Démarrer StartServer dans le package de test.
### 3. démarrer le lanceur du serveur miroir
Démarrer StartMirror dans le package de test, Il peut arrêter activement le serveur et remplacer le serveur en cas de panne du serveur.
### 4. démarrer les clients
a. Vous pouvez exécuter "Test_client_1", "Test_client_2" et "Test_client_3" contrôler chaque client dans sa propre console.
b. Vous pouvez exécuter "Test" et contrôler plusieurs clients dans une seule console.
c. Vous pouvez lancer AutoTest, qui créera automatiquement 10 clients et automatisera le processus Malloc->Write->Release->Read->Free. Par la suite, vous pouvez contrôler les clients individuels via la console.
### 5. démarrer le contrôleur d'erreurs
Étant donné que l'apparition naturelle d'erreurs et de défaillances du serveur et du client est un événement à faible probabilité, nous utilisons un contrôleur d'erreurs pour déclencher activement les erreurs. Veuillez exécuter "ControlError" à partir du paquet de test, puis vous pouvez sélectionner l'erreur souhaitée en tapant un numéro dans la console, y compris, mais sans s'y limiter, le dépassement du délai de transmission du message, la réponse erronée au message, etc.
### 6. vérification de l'exactitude
Vous pouvez exécuter le fichier "CPTselfTest" dans le paquet de test, qui automatise 100 tâches, 5 clients et calcule le cpt automatiquement.
### 7. calcul du temps
a. Vous pouvez exécuter "CPTtime_ChangeNumberOfClients" pour effectuer un test dans lequel le nombre total de tâches reste inchangé et le nombre de clients exécutant les tâches change afin de tester la relation entre le temps d'exécution et le nombre de clients.
b. Vous pouvez tester la relation entre le temps d'exécution et le nombre total de tâches en exécutant "CPTtime_ChangNumberOfJobs" pour effectuer un test dans lequel le nombre de clients reste le même et le nombre total de tâches change.

## FAQ
Q : Est-il possible de faire fonctionner le serveur sans faire fonctionner l'initiateur du serveur miroir ?
A : Oui, il est possible de faire fonctionner le serveur indépendamment du serveur miroir, mais veuillez noter que dans ce cas, le serveur ne sera pas en mesure de basculer instantanément vers le serveur miroir en cas d'erreur.
Q : Que dois-je faire s'il indique que le port est occupé ?
A: Si un port est occupé/une adresse est occupée, veuillez vérifier si le port 8080 et les ports 6060-6069 (et les ports 16060-16100 si vous effectuez des calculs de temps) sont occupés sur votre appareil. Si nécessaire, vous pouvez vérifier les ports 8080 et 6060-6069 (et 16060-16100 si vous effectuez des calculs de temps) dans StartServer, CPTime_ChangeNumberOfClients, CPTime_ChangNumberOfJobs, Test_client_1, Test_client_2, Test_client_3, Test. AutoTest, MirrorInitiator pour modifier les ports correspondants.
Q：Que dois-je faire si le message "Unrecognised identifier xxx" apparaît pendant la compilation ?
A：Veuillez mettre à jour votre version du JDK et assurez-vous que vous utilisez le JDK21 ou une version supérieure. Si le problème persiste après la mise à jour, veuillez contacter les membres de notre équipe.

## Membres de l'équipe :
Étudiants en M1, spécialité SAR, à l'Université Sorbonne :
Runlin ZHOU : runlin.zhou@etu.sorbonne-universite.fr
Xiaoquan LI : xiaoquan.li@etu.sorbonne-universite.fr
Weida LIU : weida.liu@etu.sorbonne-universite.fr


## Remerciements :
Un grand merci au Professeur Pierre Sens de l'Université Sorbonne pour son précieux soutien et ses conseils tout au long de ce projet. Son expertise et son accompagnement ont été essentiels pour notre développement.
