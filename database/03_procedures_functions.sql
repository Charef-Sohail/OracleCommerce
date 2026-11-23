
-- =========================================================
-- FICHIER : 03_procedures_functions.sql
-- PROJET  : OracleCommerce
-- =========================================================


-- =========================================================
-- 1. FONCTION : CALCULER LE MONTANT D'UNE LIGNE
-- =========================================================

CREATE OR REPLACE FUNCTION CALCULER_MONTANT_LIGNE (
    P_QUANTITE IN NUMBER,
    P_PRIX     IN NUMBER
)
RETURN NUMBER
IS
BEGIN

IF P_QUANTITE IS NULL OR P_QUANTITE <= 0 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'La quantite doit etre strictement positive.'
        );
END IF;

IF P_PRIX IS NULL OR P_PRIX < 0 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Le prix doit etre positif ou nul.'
        );
END IF;

RETURN ROUND(P_QUANTITE * P_PRIX, 2);

END;
/

-- =========================================================
-- 2. PROCEDURE : CREER UNE COMMANDE
-- =========================================================

CREATE OR REPLACE PROCEDURE CREER_COMMANDE (
    P_ID_CLIENT   IN NUMBER,
    P_ID_PRODUIT  IN NUMBER,
    P_QUANTITE    IN NUMBER,
    P_ID_COMMANDE OUT NUMBER
)
IS
    V_STOCK_ACTUEL NUMBER;
V_PRIX_PRODUIT NUMBER;
V_MONTANT      NUMBER;
V_NUMERO       VARCHAR2(50);

BEGIN

    -- 1. Vérification des paramètres
IF P_ID_CLIENT IS NULL OR P_ID_PRODUIT IS NULL THEN
        RAISE_APPLICATION_ERROR(
            -20001,
            'Le client et le produit sont obligatoires.'
        );
END IF;

IF P_QUANTITE IS NULL OR P_QUANTITE <= 0 THEN
        RAISE_APPLICATION_ERROR(
            -20002,
            'La quantite doit etre strictement positive.'
        );
END IF;

-- 2. Vérification de l'existence du client
DECLARE
    V_CLIENT NUMBER;
BEGIN
SELECT ID
INTO V_CLIENT
FROM CLIENT
WHERE ID = P_ID_CLIENT;
EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(
                -20004,
                'Client introuvable.'
            );
END;

-- 3. Lecture et verrouillage du produit
BEGIN
SELECT QUANTITE_STOCK, PRIX
INTO V_STOCK_ACTUEL, V_PRIX_PRODUIT
FROM PRODUIT
WHERE ID = P_ID_PRODUIT
    FOR UPDATE;
EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(
                -20005,
                'Produit introuvable.'
            );
END;

-- 4. Vérification du stock
IF V_STOCK_ACTUEL < P_QUANTITE THEN
        RAISE_APPLICATION_ERROR(
            -20003,
            'Stock insuffisant pour ce produit.'
        );
END IF;

-- 5. Calcul du montant
V_MONTANT := CALCULER_MONTANT_LIGNE(
        P_QUANTITE,
        V_PRIX_PRODUIT
    );

    -- 6. Génération du numéro de commande
V_NUMERO := 'CMD-' ||
                TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') ||
                '-' || SEQ_COMMANDE.NEXTVAL;

    -- 7. Création de la commande
INSERT INTO COMMANDE (
    ID,
    NUMERO,
    DATE_COMMANDE,
    STATUT,
    MONTANT_TOTAL,
    CLIENT_ID
)
VALUES (
           SEQ_COMMANDE.CURRVAL,
           V_NUMERO,
           SYSDATE,
           'EN_ATTENTE',
           V_MONTANT,
           P_ID_CLIENT
       )
RETURNING ID INTO P_ID_COMMANDE;

-- 8. Insertion de la ligne de commande
INSERT INTO LIGNE_COMMANDE (
    ID,
    QUANTITE,
    PRIX_UNITAIRE,
    COMMANDE_ID,
    PRODUIT_ID
)
VALUES (
           SEQ_LIGNE_COMMANDE.NEXTVAL,
           P_QUANTITE,
           V_PRIX_PRODUIT,
           P_ID_COMMANDE,
           P_ID_PRODUIT
       );

-- 9. Mise à jour du stock
UPDATE PRODUIT
SET QUANTITE_STOCK = QUANTITE_STOCK - P_QUANTITE
WHERE ID = P_ID_PRODUIT;

-- Pas de COMMIT ici :
-- La transaction est contrôlée par l'appelant.

EXCEPTION
    WHEN OTHERS THEN
        RAISE;

END;
/
