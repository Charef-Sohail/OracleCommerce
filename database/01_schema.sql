-- Suppression des tables si elles existent (pour pouvoir relancer le script)
-- BEGIN
--     FOR cur_rec IN (SELECT table_name FROM user_tables) LOOP
--             EXECUTE IMMEDIATE 'DROP TABLE ' || cur_rec.table_name || ' CASCADE CONSTRAINTS';
--         END LOOP;
-- END;
-- /

-- 1. Table ROLE
CREATE TABLE role (
                      id_role NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      nom VARCHAR2(50) NOT NULL UNIQUE
);

-- 2. Table CLIENT
CREATE TABLE client (
                        id_client NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        nom VARCHAR2(100) NOT NULL,
                        prenom VARCHAR2(100) NOT NULL,
                        email VARCHAR2(150) NOT NULL UNIQUE,
                        telephone VARCHAR2(20)
);

-- 3. Table UTILISATEUR
CREATE TABLE utilisateur (
                             id_utilisateur NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             id_client NUMBER,
                             id_role NUMBER NOT NULL,
                             username VARCHAR2(100) NOT NULL UNIQUE,
                             password_hash VARCHAR2(255) NOT NULL,
                             CONSTRAINT fk_user_client FOREIGN KEY (id_client) REFERENCES client(id_client),
                             CONSTRAINT fk_user_role FOREIGN KEY (id_role) REFERENCES role(id_role)
);

-- 4. Table CATEGORIE
CREATE TABLE categorie (
                           id_categorie NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           nom VARCHAR2(100) NOT NULL UNIQUE
);

-- 5. Table PRODUIT
CREATE TABLE produit (
                         id_produit NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         id_categorie NUMBER NOT NULL,
                         nom VARCHAR2(150) NOT NULL,
                         prix NUMBER(10,2) NOT NULL CHECK (prix >= 0),
                         quantite_stock NUMBER NOT NULL CHECK (quantite_stock >= 0),
                         CONSTRAINT fk_prod_cat FOREIGN KEY (id_categorie) REFERENCES categorie(id_categorie)
);

-- 6. Table COMMANDE
CREATE TABLE commande (
                          id_commande NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          id_client NUMBER NOT NULL,
                          date_commande DATE DEFAULT SYSDATE NOT NULL,
                          statut VARCHAR2(50) DEFAULT 'EN_ATTENTE' NOT NULL,
                          montant_total NUMBER(12,2) DEFAULT 0,
                          CONSTRAINT fk_cmd_client FOREIGN KEY (id_client) REFERENCES client(id_client)
);

-- 7. Table LIGNE_COMMANDE
CREATE TABLE ligne_commande (
                                id_ligne NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                id_commande NUMBER NOT NULL,
                                id_produit NUMBER NOT NULL,
                                quantite NUMBER NOT NULL CHECK (quantite > 0),
                                prix_unitaire NUMBER(10,2) NOT NULL,
                                CONSTRAINT fk_ligne_cmd FOREIGN KEY (id_commande) REFERENCES commande(id_commande) ON DELETE CASCADE,
                                CONSTRAINT fk_ligne_prod FOREIGN KEY (id_produit) REFERENCES produit(id_produit)
);

-- 8. Table PAIEMENT
CREATE TABLE paiement (
                          id_paiement NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          id_commande NUMBER NOT NULL UNIQUE,
                          montant NUMBER(12,2) NOT NULL,
                          statut VARCHAR2(50) DEFAULT 'VALIDE' NOT NULL,
                          CONSTRAINT fk_paiement_cmd FOREIGN KEY (id_commande) REFERENCES commande(id_commande) ON DELETE CASCADE
);

-- 9. Table MOUVEMENT_STOCK
CREATE TABLE mouvement_stock (
                                 id_mouvement NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 id_produit NUMBER NOT NULL,
                                 type_mouvement VARCHAR2(20) NOT NULL CHECK (type_mouvement IN ('ENTREE', 'SORTIE')),
                                 quantite NUMBER NOT NULL CHECK (quantite > 0),
                                 date_mouvement DATE DEFAULT SYSDATE NOT NULL,
                                 CONSTRAINT fk_mvt_prod FOREIGN KEY (id_produit) REFERENCES produit(id_produit)
);