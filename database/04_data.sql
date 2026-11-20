
-- =========================================================
-- FICHIER : 04_data.sql
-- PROJET : OracleCommerce
-- DONNEES DE TEST
-- =========================================================

-- =========================================================
-- 1. NETTOYAGE DES DONNEES
-- =========================================================

DELETE FROM mouvement_stock;
DELETE FROM paiement;
DELETE FROM ligne_commande;
DELETE FROM commande;
DELETE FROM utilisateur;
DELETE FROM produit;
DELETE FROM categorie;
DELETE FROM client;
DELETE FROM role;

COMMIT;

-- =========================================================
-- 2. INSERTION DES ROLES
-- =========================================================

INSERT INTO role (nom) VALUES ('ADMIN');
INSERT INTO role (nom) VALUES ('CLIENT');

-- =========================================================
-- 3. INSERTION DES CLIENTS
-- =========================================================

INSERT INTO client (
    nom, prenom, email, telephone
)
VALUES (
           'Oulakbir',
           'Ilham',
           'ilham.oulakbir@enset.ma',
           '0600000001'
       );

INSERT INTO client (
    nom, prenom, email, telephone
)
VALUES (
           'Sohail',
           'Mohammed',
           'mohammed.sohail@enset.ma',
           '0600000002'
       );

INSERT INTO client (
    nom, prenom, email, telephone
)
VALUES (
           'Alami',
           'Fatima',
           'fatima.alami@enset.ma',
           '0600000003'
       );

-- =========================================================
-- 4. INSERTION DES UTILISATEURS
-- =========================================================

-- Administrateur
INSERT INTO utilisateur (
    id_client,
    id_role,
    username,
    password_hash
)
SELECT
    NULL,
    r.id_role,
    'admin',
    'HASH_BCRYPT_ADMIN_A_REMPLACER'
FROM role r
WHERE r.nom = 'ADMIN';

-- Client Mohammed
INSERT INTO utilisateur (
    id_client,
    id_role,
    username,
    password_hash
)
SELECT
    c.id_client,
    r.id_role,
    'msohail',
    'HASH_BCRYPT_CLIENT_A_REMPLACER'
FROM client c
         CROSS JOIN role r
WHERE c.email = 'mohammed.sohail@enset.ma'
  AND r.nom = 'CLIENT';

-- Client Fatima
INSERT INTO utilisateur (
    id_client,
    id_role,
    username,
    password_hash
)
SELECT
    c.id_client,
    r.id_role,
    'falami',
    'HASH_BCRYPT_CLIENT_A_REMPLACER'
FROM client c
         CROSS JOIN role r
WHERE c.email = 'fatima.alami@enset.ma'
  AND r.nom = 'CLIENT';

-- =========================================================
-- 5. INSERTION DES CATEGORIES
-- =========================================================

INSERT INTO categorie (nom)
VALUES ('Informatique & High-Tech');

INSERT INTO categorie (nom)
VALUES ('Smartphones & Tablettes');

INSERT INTO categorie (nom)
VALUES ('Accessoires Bureau');

-- =========================================================
-- 6. INSERTION DES PRODUITS
-- =========================================================

INSERT INTO produit (
    id_categorie,
    nom,
    prix,
    quantite_stock
)
SELECT
    id_categorie,
    'PC Portable Gamer',
    12500.00,
    10
FROM categorie
WHERE nom = 'Informatique & High-Tech';

INSERT INTO produit (
    id_categorie,
    nom,
    prix,
    quantite_stock
)
SELECT
    id_categorie,
    'Ecran 27 pouces 4K',
    3200.50,
    15
FROM categorie
WHERE nom = 'Informatique & High-Tech';

INSERT INTO produit (
    id_categorie,
    nom,
    prix,
    quantite_stock
)
SELECT
    id_categorie,
    'Smartphone Pro Max',
    9800.00,
    8
FROM categorie
WHERE nom = 'Smartphones & Tablettes';

INSERT INTO produit (
    id_categorie,
    nom,
    prix,
    quantite_stock
)
SELECT
    id_categorie,
    'Clavier Mecanique RGB',
    750.00,
    25
FROM categorie
WHERE nom = 'Accessoires Bureau';

-- =========================================================
-- 7. COMMANDE DE TEST
-- =========================================================

DECLARE
    v_id_client   NUMBER;
    v_id_pc       NUMBER;
    v_id_clavier  NUMBER;
    v_id_commande NUMBER;

BEGIN

    -- Récupération du client
    SELECT id_client
    INTO v_id_client
    FROM client
    WHERE email = 'mohammed.sohail@enset.ma';

    -- Récupération du PC
    SELECT id_produit
    INTO v_id_pc
    FROM produit
    WHERE nom = 'PC Portable Gamer';

    -- Création d'une première commande
    creer_commande(
            p_id_client   => v_id_client,
            p_id_produit  => v_id_pc,
            p_quantite    => 1,
            p_id_commande => v_id_commande
    );

    DBMS_OUTPUT.PUT_LINE(
            'Commande creee : ' || v_id_commande
    );

    -- Récupération du clavier
    SELECT id_produit
    INTO v_id_clavier
    FROM produit
    WHERE nom = 'Clavier Mecanique RGB';

    -- Ajout du deuxième produit :
    -- Cette version simple crée une deuxième commande.
    creer_commande(
            p_id_client   => v_id_client,
            p_id_produit  => v_id_clavier,
            p_quantite    => 1,
            p_id_commande => v_id_commande
    );

    DBMS_OUTPUT.PUT_LINE(
            'Deuxieme commande creee : ' || v_id_commande
    );

END;
/

-- =========================================================
-- 8. PAIEMENT SIMULE
-- =========================================================

INSERT INTO paiement (
    id_commande,
    montant,
    statut
)
SELECT
    c.id_commande,
    c.montant_total,
    'VALIDE'
FROM commande c
WHERE c.id_client = (
    SELECT id_client
    FROM client
    WHERE email = 'mohammed.sohail@enset.ma'
);

-- =========================================================
-- 9. VALIDATION
-- =========================================================

COMMIT;

-- =========================================================
-- FIN DU SCRIPT
-- =========================================================
