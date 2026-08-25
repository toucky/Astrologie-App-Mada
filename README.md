# Astrologie APP MADA — préparation à l’intégration

Statut actuel : **Bientôt disponible / ne pas publier comme application payante pour le moment**.

## Ce qui est déjà présent

- interface FR / MG ;
- thème astral et synthèse ;
- compatibilité / synastrie ;
- cycles et prévisions ;
- sauvegarde locale du profil ;
- version web autonome.

## Points à terminer avant publication APP MADA

1. **Brancher le compte APP MADA commun** (même compte que RHAY Studio et Numérologie Mada).
2. **Définir le modèle commercial** de l’application avant d’ajouter un débit : prix, nombre de crédits et quelles analyses sont facturées.
3. **Créer `astrologie-api`** sur le backend APP MADA uniquement après validation de ces règles commerciales.
4. **Ajouter Astrologie à l’administration centrale** pour les ventes, demandes de paiement et statistiques.
5. **Corriger les données astronomiques simulées** avant publication : la source actuelle contient notamment des rétrogrades et éclipses marqués comme fictifs dans le code. Ils ne doivent pas être présentés comme événements astronomiques réels.
6. **Vérifier les calculs astronomiques et les textes** avant de rendre l’application disponible.
7. Générer ensuite l’APK signée et renseigner son `apk_path` dans le registre `app_mada_apps`.

## Architecture APP MADA à respecter

La boutique et l’administration centrales sont maintenues dans :

`toucky/Rhay-studio-mix-2`

Backend central : projet Supabase `RHAY-Studio-Credits`.

Astrologie est déjà enregistrée dans le registre central `app_mada_apps` avec le statut `coming_soon`.
