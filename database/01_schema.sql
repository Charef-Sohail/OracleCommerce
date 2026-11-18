
------------------------------------------------------------
-- ORACLE COMMERCE
-- Schema compatible avec les entites Spring Boot
-- Oracle Database 19c
------------------------------------------------------------

------------------------------------------------------------
-- 1. SUPPRESSION DES ANCIENNES TABLES
-- Attention : ce script supprime les données existantes.
------------------------------------------------------------

BEGIN
FOR t IN (
        SELECT table_name
        FROM user_tables
        WHERE table_name IN (
            'UTILISATEUR_ROLE',
            'MOUVEMENT_STOCK',
            'PAIEMENT',
            'LIGNE_COMMANDE',
            'COMMANDE',
            'PRODUIT',
            'UTILISATEUR',
            'ROLE',
            'CATEGORIE',
            'CLIENT'
        )
    ) LOOP
EXECUTE IMMEDIATE
    'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS';
END LOOP;
END;
/

------------------------------------------------------------
-- 2. SUPPRESSION DES ANCIENNES SEQUENCES
------------------------------------------------------------

BEGIN
FOR s IN (
        SELECT sequence_name
        FROM user_sequences
        WHERE sequence_name IN (
            'SEQ_CLIENT',
            'SEQ_CATEGORIE',
            'SEQ_PRODUIT',
            'SEQ_COMMANDE',
            'SEQ_LIGNE_COMMANDE',
            'SEQ_PAIEMENT',
            'SEQ_MOUVEMENT_STOCK',
            'SEQ_ROLE',
            'SEQ_UTILISATEUR'
        )
    ) LOOP
EXECUTE IMMEDIATE
    'DROP SEQUENCE ' || s.sequence_name;
END LOOP;
END;
/

------------------------------------------------------------
-- 3. TABLE CLIENT
------------------------------------------------------------

CREATE TABLE CLIENT (
                        ID NUMBER(19) PRIMARY KEY,
                        NOM VARCHAR2(100) NOT NULL,
                        PRENOM VARCHAR2(100) NOT NULL,
                        EMAIL VARCHAR2(150) NOT NULL UNIQUE,
                        TELEPHONE VARCHAR2(20),
                        ADRESSE VARCHAR2(255),
                        DATE_CREATION TIMESTAMP
);

------------------------------------------------------------
-- 4. TABLE CATEGORIE
------------------------------------------------------------

CREATE TABLE CATEGORIE (
                           ID NUMBER(19) PRIMARY KEY,
                           NOM VARCHAR2(100) NOT NULL UNIQUE,
                           DESCRIPTION VARCHAR2(500),
                           ACTIVE NUMBER(1) DEFAULT 1 NOT NULL,

                           CONSTRAINT CK_CATEGORIE_ACTIVE
                               CHECK (ACTIVE IN (0, 1))
);

------------------------------------------------------------
-- 5. TABLE PRODUIT
------------------------------------------------------------

CREATE TABLE PRODUIT (
                         ID NUMBER(19) PRIMARY KEY,
                         REFERENCE VARCHAR2(50) NOT NULL UNIQUE,
                         NOM VARCHAR2(150) NOT NULL,
                         DESCRIPTION VARCHAR2(1000),
                         PRIX NUMBER(12,2) NOT NULL,
                         QUANTITE_STOCK NUMBER(10) DEFAULT 0 NOT NULL,
                         SEUIL_ALERTE NUMBER(10) DEFAULT 5 NOT NULL,
                         ACTIF NUMBER(1) DEFAULT 1 NOT NULL,
                         CATEGORIE_ID NUMBER(19) NOT NULL,

                         CONSTRAINT CK_PRODUIT_PRIX
                             CHECK (PRIX >= 0),

                         CONSTRAINT CK_PRODUIT_STOCK
                             CHECK (QUANTITE_STOCK >= 0),

                         CONSTRAINT CK_PRODUIT_SEUIL
                             CHECK (SEUIL_ALERTE >= 0),

                         CONSTRAINT CK_PRODUIT_ACTIF
                             CHECK (ACTIF IN (0, 1)),

                         CONSTRAINT FK_PRODUIT_CATEGORIE
                             FOREIGN KEY (CATEGORIE_ID)
                                 REFERENCES CATEGORIE(ID)
);

------------------------------------------------------------
-- 6. TABLE COMMANDE
------------------------------------------------------------

CREATE TABLE COMMANDE (
                          ID NUMBER(19) PRIMARY KEY,
                          NUMERO VARCHAR2(50) NOT NULL UNIQUE,
                          DATE_COMMANDE TIMESTAMP,
                          STATUT VARCHAR2(30) NOT NULL,
                          MONTANT_TOTAL NUMBER(12,2) DEFAULT 0 NOT NULL,
                          CLIENT_ID NUMBER(19) NOT NULL,

                          CONSTRAINT CK_COMMANDE_STATUT
                              CHECK (STATUT IN (
                                                'EN_ATTENTE',
                                                'VALIDEE',
                                                'EXPEDIEE',
                                                'LIVREE',
                                                'ANNULEE'
                                  )),

                          CONSTRAINT CK_COMMANDE_MONTANT
                              CHECK (MONTANT_TOTAL >= 0),

                          CONSTRAINT FK_COMMANDE_CLIENT
                              FOREIGN KEY (CLIENT_ID)
                                  REFERENCES CLIENT(ID)
);

------------------------------------------------------------
-- 7. TABLE LIGNE_COMMANDE
------------------------------------------------------------

CREATE TABLE LIGNE_COMMANDE (
                                ID NUMBER(19) PRIMARY KEY,
                                QUANTITE NUMBER(10) NOT NULL,
                                PRIX_UNITAIRE NUMBER(12,2) NOT NULL,
                                COMMANDE_ID NUMBER(19) NOT NULL,
                                PRODUIT_ID NUMBER(19) NOT NULL,

                                CONSTRAINT CK_LIGNE_QUANTITE
                                    CHECK (QUANTITE > 0),

                                CONSTRAINT CK_LIGNE_PRIX
                                    CHECK (PRIX_UNITAIRE >= 0),

                                CONSTRAINT UK_COMMANDE_PRODUIT
                                    UNIQUE (COMMANDE_ID, PRODUIT_ID),

                                CONSTRAINT FK_LIGNE_COMMANDE
                                    FOREIGN KEY (COMMANDE_ID)
                                        REFERENCES COMMANDE(ID)
                                        ON DELETE CASCADE,

                                CONSTRAINT FK_LIGNE_PRODUIT
                                    FOREIGN KEY (PRODUIT_ID)
                                        REFERENCES PRODUIT(ID)
);

------------------------------------------------------------
-- 8. TABLE PAIEMENT
------------------------------------------------------------

CREATE TABLE PAIEMENT (
                          ID NUMBER(19) PRIMARY KEY,
                          REFERENCE_TRANSACTION VARCHAR2(100) NOT NULL UNIQUE,
                          MONTANT NUMBER(12,2) NOT NULL,
                          MODE_PAIEMENT VARCHAR2(30) NOT NULL,
                          STATUT VARCHAR2(30) NOT NULL,
                          DATE_PAIEMENT TIMESTAMP,
                          COMMANDE_ID NUMBER(19) NOT NULL UNIQUE,

                          CONSTRAINT CK_PAIEMENT_MONTANT
                              CHECK (MONTANT >= 0),

                          CONSTRAINT CK_PAIEMENT_MODE
                              CHECK (MODE_PAIEMENT IN (
                                                       'ESPECES',
                                                       'CARTE_BANCAIRE',
                                                       'VIREMENT',
                                                       'CHEQUE'
                                  )),

                          CONSTRAINT CK_PAIEMENT_STATUT
                              CHECK (STATUT IN (
                                                'EN_ATTENTE',
                                                'ACCEPTE',
                                                'REFUSE',
                                                'REMBOURSE'
                                  )),

                          CONSTRAINT FK_PAIEMENT_COMMANDE
                              FOREIGN KEY (COMMANDE_ID)
                                  REFERENCES COMMANDE(ID)
);

------------------------------------------------------------
-- 9. TABLE MOUVEMENT_STOCK
------------------------------------------------------------

CREATE TABLE MOUVEMENT_STOCK (
                                 ID NUMBER(19) PRIMARY KEY,
                                 TYPE_MOUVEMENT VARCHAR2(20) NOT NULL,
                                 QUANTITE NUMBER(10) NOT NULL,
                                 QUANTITE_AVANT NUMBER(10) NOT NULL,
                                 QUANTITE_APRES NUMBER(10) NOT NULL,
                                 MOTIF VARCHAR2(500),
                                 DATE_MOUVEMENT TIMESTAMP,
                                 PRODUIT_ID NUMBER(19) NOT NULL,

                                 CONSTRAINT CK_MOUVEMENT_TYPE
                                     CHECK (TYPE_MOUVEMENT IN (
                                                               'ENTREE',
                                                               'SORTIE',
                                                               'AJUSTEMENT'
                                         )),

                                 CONSTRAINT CK_MOUVEMENT_QUANTITE
                                     CHECK (QUANTITE > 0),

                                 CONSTRAINT CK_MOUVEMENT_STOCK_AVANT
                                     CHECK (QUANTITE_AVANT >= 0),

                                 CONSTRAINT CK_MOUVEMENT_STOCK_APRES
                                     CHECK (QUANTITE_APRES >= 0),

                                 CONSTRAINT FK_MOUVEMENT_PRODUIT
                                     FOREIGN KEY (PRODUIT_ID)
                                         REFERENCES PRODUIT(ID)
);

------------------------------------------------------------
-- 10. TABLE ROLE
------------------------------------------------------------

CREATE TABLE ROLE (
                      ID NUMBER(19) PRIMARY KEY,
                      NOM VARCHAR2(50) NOT NULL UNIQUE,
                      DESCRIPTION VARCHAR2(255)
);

------------------------------------------------------------
-- 11. TABLE UTILISATEUR
------------------------------------------------------------

CREATE TABLE UTILISATEUR (
                             ID NUMBER(19) PRIMARY KEY,
                             NOM_UTILISATEUR VARCHAR2(100) NOT NULL UNIQUE,
                             EMAIL VARCHAR2(150) NOT NULL UNIQUE,
                             MOT_DE_PASSE VARCHAR2(255) NOT NULL,
                             NOM VARCHAR2(100),
                             PRENOM VARCHAR2(100),
                             ACTIF NUMBER(1) DEFAULT 1 NOT NULL,
                             DATE_CREATION TIMESTAMP,

                             CONSTRAINT CK_UTILISATEUR_ACTIF
                                 CHECK (ACTIF IN (0, 1))
);

------------------------------------------------------------
-- 12. TABLE ASSOCIATIVE UTILISATEUR_ROLE
-- Relation ManyToMany
------------------------------------------------------------

CREATE TABLE UTILISATEUR_ROLE (
                                  UTILISATEUR_ID NUMBER(19) NOT NULL,
                                  ROLE_ID NUMBER(19) NOT NULL,

                                  CONSTRAINT PK_UTILISATEUR_ROLE
                                      PRIMARY KEY (UTILISATEUR_ID, ROLE_ID),

                                  CONSTRAINT FK_UR_UTILISATEUR
                                      FOREIGN KEY (UTILISATEUR_ID)
                                          REFERENCES UTILISATEUR(ID)
                                          ON DELETE CASCADE,

                                  CONSTRAINT FK_UR_ROLE
                                      FOREIGN KEY (ROLE_ID)
                                          REFERENCES ROLE(ID)
);

------------------------------------------------------------
-- 13. SEQUENCES ORACLE
------------------------------------------------------------

CREATE SEQUENCE SEQ_CLIENT START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_CATEGORIE START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_PRODUIT START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_COMMANDE START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_LIGNE_COMMANDE START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_PAIEMENT START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_MOUVEMENT_STOCK START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_ROLE START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_UTILISATEUR START WITH 1 INCREMENT BY 1 NOCACHE;

------------------------------------------------------------
-- FIN DU SCRIPT
------------------------------------------------------------

COMMIT;
