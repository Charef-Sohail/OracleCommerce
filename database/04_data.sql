
-- =========================================================
-- FICHIER : 04_data.sql
-- PROJET  : OracleCommerce
-- DONNEES DE TEST
-- =========================================================


-- =========================================================
-- 1. NETTOYAGE DES DONNEES
-- ATTENTION : SUPPRESSION DE TOUTES LES DONNEES EXISTANTES
-- =========================================================

DELETE FROM UTILISATEUR_ROLE;
DELETE FROM PAIEMENT;
DELETE FROM LIGNE_COMMANDE;
DELETE FROM COMMANDE;
DELETE FROM MOUVEMENT_STOCK;
DELETE FROM UTILISATEUR;
DELETE FROM PRODUIT;
DELETE FROM CATEGORIE;
DELETE FROM CLIENT;
DELETE FROM ROLE;

COMMIT;


-- =========================================================
-- 2. INSERTION DES ROLES
-- =========================================================

INSERT INTO ROLE (ID, NOM, DESCRIPTION)
VALUES (
           SEQ_ROLE.NEXTVAL,
           'ADMIN',
           'Administrateur de la plateforme'
       );

INSERT INTO ROLE (ID, NOM, DESCRIPTION)
VALUES (
           SEQ_ROLE.NEXTVAL,
           'CLIENT',
           'Utilisateur ayant le role client'
       );


-- =========================================================
-- 3. INSERTION DES CLIENTS
-- =========================================================

INSERT INTO CLIENT (
    ID, NOM, PRENOM, EMAIL, TELEPHONE, ADRESSE
)
VALUES (
           SEQ_CLIENT.NEXTVAL,
           'Oulakbir',
           'Ilham',
           'ilham.oulakbir@enset.ma',
           '0600000001',
           'Mohammedia'
       );

INSERT INTO CLIENT (
    ID, NOM, PRENOM, EMAIL, TELEPHONE, ADRESSE
)
VALUES (
           SEQ_CLIENT.NEXTVAL,
           'Sohail',
           'Mohammed',
           'mohammed.sohail@enset.ma',
           '0600000002',
           'Casablanca'
       );

INSERT INTO CLIENT (
    ID, NOM, PRENOM, EMAIL, TELEPHONE, ADRESSE
)
VALUES (
           SEQ_CLIENT.NEXTVAL,
           'Alami',
           'Fatima',
           'fatima.alami@enset.ma',
           '0600000003',
           'Rabat'
       );


-- =========================================================
-- 4. INSERTION DES UTILISATEURS
-- Les mots de passe doivent être des hash BCrypt valides.
-- Remplacer les valeurs provisoires avant toute connexion.
-- =========================================================

INSERT INTO UTILISATEUR (
    ID,
    NOM_UTILISATEUR,
    EMAIL,
    MOT_DE_PASSE,
    NOM,
    PRENOM,
    ACTIF
)
VALUES (
           SEQ_UTILISATEUR.NEXTVAL,
           'admin',
           'admin@oraclecommerce.ma',
           'HASH_BCRYPT_ADMIN_A_REMPLACER',
           'Admin',
           'OracleCommerce',
           1
       );

INSERT INTO UTILISATEUR (
    ID,
    NOM_UTILISATEUR,
    EMAIL,
    MOT_DE_PASSE,
    NOM,
    PRENOM,
    ACTIF
)
VALUES (
           SEQ_UTILISATEUR.NEXTVAL,
           'msohail',
           'mohammed.sohail@enset.ma',
           'HASH_BCRYPT_CLIENT_A_REMPLACER',
           'Sohail',
           'Mohammed',
           1
       );

INSERT INTO UTILISATEUR (
    ID,
    NOM_UTILISATEUR,
    EMAIL,
    MOT_DE_PASSE,
    NOM,
    PRENOM,
    ACTIF
)
VALUES (
           SEQ_UTILISATEUR.NEXTVAL,
           'falami',
           'fatima.alami@enset.ma',
           'HASH_BCRYPT_CLIENT_A_REMPLACER',
           'Alami',
           'Fatima',
           1
       );


-- =========================================================
-- 5. ASSOCIATION UTILISATEURS / ROLES
-- =========================================================

INSERT INTO UTILISATEUR_ROLE (UTILISATEUR_ID, ROLE_ID)
SELECT U.ID, R.ID
FROM UTILISATEUR U
         CROSS JOIN ROLE R
WHERE U.NOM_UTILISATEUR = 'admin'
  AND R.NOM = 'ADMIN';

INSERT INTO UTILISATEUR_ROLE (UTILISATEUR_ID, ROLE_ID)
SELECT U.ID, R.ID
FROM UTILISATEUR U
         CROSS JOIN ROLE R
WHERE U.NOM_UTILISATEUR IN ('msohail', 'falami')
  AND R.NOM = 'CLIENT';


-- =========================================================
-- 6. INSERTION DES CATEGORIES
-- =========================================================

INSERT INTO CATEGORIE (ID, NOM, DESCRIPTION, ACTIVE)
VALUES (
           SEQ_CATEGORIE.NEXTVAL,
           'Informatique & High-Tech',
           'Ordinateurs et equipements informatiques',
           1
       );

INSERT INTO CATEGORIE (ID, NOM, DESCRIPTION, ACTIVE)
VALUES (
           SEQ_CATEGORIE.NEXTVAL,
           'Smartphones & Tablettes',
           'Telephones et tablettes',
           1
       );

INSERT INTO CATEGORIE (ID, NOM, DESCRIPTION, ACTIVE)
VALUES (
           SEQ_CATEGORIE.NEXTVAL,
           'Accessoires Bureau',
           'Accessoires et peripheriques informatiques',
           1
       );


-- =========================================================
-- 7. INSERTION DES PRODUITS
-- =========================================================

INSERT INTO PRODUIT (
    ID, REFERENCE, NOM, DESCRIPTION,
    PRIX, QUANTITE_STOCK, SEUIL_ALERTE,
    ACTIF, CATEGORIE_ID
)
SELECT
    SEQ_PRODUIT.NEXTVAL,
    'PC-001',
    'PC Portable Gamer',
    'Ordinateur portable gamer',
    12500.00,
    10,
    3,
    1,
    ID
FROM CATEGORIE
WHERE NOM = 'Informatique & High-Tech';

INSERT INTO PRODUIT (
    ID, REFERENCE, NOM, DESCRIPTION,
    PRIX, QUANTITE_STOCK, SEUIL_ALERTE,
    ACTIF, CATEGORIE_ID
)
SELECT
    SEQ_PRODUIT.NEXTVAL,
    'ECR-001',
    'Ecran 27 pouces 4K',
    'Ecran haute resolution',
    3200.50,
    15,
    5,
    1,
    ID
FROM CATEGORIE
WHERE NOM = 'Informatique & High-Tech';

INSERT INTO PRODUIT (
    ID, REFERENCE, NOM, DESCRIPTION,
    PRIX, QUANTITE_STOCK, SEUIL_ALERTE,
    ACTIF, CATEGORIE_ID
)
SELECT
    SEQ_PRODUIT.NEXTVAL,
    'SM-001',
    'Smartphone Pro Max',
    'Smartphone haut de gamme',
    9800.00,
    8,
    2,
    1,
    ID
FROM CATEGORIE
WHERE NOM = 'Smartphones & Tablettes';

INSERT INTO PRODUIT (
    ID, REFERENCE, NOM, DESCRIPTION,
    PRIX, QUANTITE_STOCK, SEUIL_ALERTE,
    ACTIF, CATEGORIE_ID
)
SELECT
    SEQ_PRODUIT.NEXTVAL,
    'CL-001',
    'Clavier Mecanique RGB',
    'Clavier mecanique retroeclaire',
    750.00,
    25,
    5,
    1,
    ID
FROM CATEGORIE
WHERE NOM = 'Accessoires Bureau';


-- =========================================================
-- 8. CREATION DES COMMANDES DE TEST
-- La procedure CREER_COMMANDE cree une commande avec
-- un seul produit et met automatiquement le stock a jour.
-- =========================================================

DECLARE
    V_ID_CLIENT   NUMBER;
V_ID_PC       NUMBER;
V_ID_CLAVIER  NUMBER;
V_ID_COMMANDE NUMBER;

BEGIN

SELECT ID
INTO V_ID_CLIENT
FROM CLIENT
WHERE EMAIL = 'mohammed.sohail@enset.ma';

SELECT ID
INTO V_ID_PC
FROM PRODUIT
WHERE REFERENCE = 'PC-001';

-- Premiere commande : 1 PC Gamer
CREER_COMMANDE(
                  P_ID_CLIENT   => V_ID_CLIENT,
                  P_ID_PRODUIT  => V_ID_PC,
                  P_QUANTITE    => 1,
                  P_ID_COMMANDE => V_ID_COMMANDE
              );

DBMS_OUTPUT.PUT_LINE(
        'Premiere commande creee : ' || V_ID_COMMANDE
    );

SELECT ID
INTO V_ID_CLAVIER
FROM PRODUIT
WHERE REFERENCE = 'CL-001';

-- Deuxieme commande : 2 claviers
CREER_COMMANDE(
                  P_ID_CLIENT   => V_ID_CLIENT,
                  P_ID_PRODUIT  => V_ID_CLAVIER,
                  P_QUANTITE    => 2,
                  P_ID_COMMANDE => V_ID_COMMANDE
              );

DBMS_OUTPUT.PUT_LINE(
        'Deuxieme commande creee : ' || V_ID_COMMANDE
    );

END;
/


-- =========================================================
-- 9. INSERTION DES PAIEMENTS DE TEST
-- =========================================================

INSERT INTO PAIEMENT (
    ID,
    REFERENCE_TRANSACTION,
    MONTANT,
    MODE_PAIEMENT,
    STATUT,
    DATE_PAIEMENT,
    COMMANDE_ID
)
SELECT
    SEQ_PAIEMENT.NEXTVAL,
    'PAY-TEST-001',
    MONTANT_TOTAL,
    'CARTE_BANCAIRE',
    'ACCEPTE',
    SYSDATE,
    ID
FROM COMMANDE
WHERE NUMERO IS NOT NULL
  AND ROWNUM = 1;

INSERT INTO PAIEMENT (
    ID,
    REFERENCE_TRANSACTION,
    MONTANT,
    MODE_PAIEMENT,
    STATUT,
    DATE_PAIEMENT,
    COMMANDE_ID
)
SELECT
    SEQ_PAIEMENT.NEXTVAL,
    'PAY-TEST-002',
    MONTANT_TOTAL,
    'ESPECES',
    'ACCEPTE',
    SYSDATE,
    ID
FROM COMMANDE
WHERE NUMERO IS NOT NULL
  AND ID <> (
    SELECT MIN(ID) FROM COMMANDE
);


-- =========================================================
-- 10. VALIDATION
-- =========================================================

COMMIT;


-- =========================================================
-- 11. VERIFICATION DES DONNEES
-- =========================================================

SELECT * FROM CLIENT ORDER BY ID;

SELECT * FROM CATEGORIE ORDER BY ID;

SELECT
    ID, REFERENCE, NOM, PRIX,
    QUANTITE_STOCK, SEUIL_ALERTE, CATEGORIE_ID
FROM PRODUIT
ORDER BY ID;

SELECT
    ID, NUMERO, DATE_COMMANDE,
    STATUT, MONTANT_TOTAL, CLIENT_ID
FROM COMMANDE
ORDER BY ID;

SELECT
    ID, REFERENCE_TRANSACTION, MONTANT,
    MODE_PAIEMENT, STATUT, COMMANDE_ID
FROM PAIEMENT
ORDER BY ID;

SELECT
    U.NOM_UTILISATEUR,
    R.NOM AS ROLE
FROM UTILISATEUR U
         JOIN UTILISATEUR_ROLE UR ON UR.UTILISATEUR_ID = U.ID
         JOIN ROLE R ON R.ID = UR.ROLE_ID
ORDER BY U.NOM_UTILISATEUR;


-- =========================================================
-- FIN DU SCRIPT
-- =========================================================
