# ift-api-java

Quelques exemples d'implémentation de l'API REST IFT en Java :

- /api/hello
- /api/ift/traitement : calcul d'un ift
- /api/ift/traitement/certifie : calcul et signature d'un ift. Utilisation du Jwks (/.well-known/jwks.json) pour vérifier la signature.

Pour plus d'information sur Jwks, se référer à la [RFC7517](https://tools.ietf.org/html/rfc7517)

En cas de problème de vérification du certificat SSL, utiliser le script `addPprdCert.sh` pour ajouter le certificat au store java.  