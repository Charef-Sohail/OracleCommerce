
-- =========================================================
-- FICHIER : 03_procedures_functions.sql
-- PROJET : OracleCommerce
-- =========================================================

-- =========================================================
-- 1. FONCTION : CALCULER LE MONTANT D'UNE LIGNE
-- =========================================================

CREATE OR REPLACE FUNCTION calculer_montant_ligne(
    p_quantite IN NUMBER,
    p_prix IN NUMBER
)
    RETURN NUMBER
    IS
BEGIN

    IF p_quantite IS NULL OR p_quantite <= 0 THEN
        RAISE_APPLICATION_ERROR(
                -20010,
                'La quantite doit etre strictement positive.'
        );
    END IF;

    IF p_prix IS NULL OR p_prix < 0 THEN
        RAISE_APPLICATION_ERROR(
                -20011,
                'Le prix doit etre positif ou nul.'
        );
    END IF;

    RETURN ROUND(p_quantite * p_prix, 2);

END;
/

-- =========================================================
-- 2. PROCEDURE : CREER UNE COMMANDE
-- =========================================================

CREATE OR REPLACE PROCEDURE creer_commande(
    p_id_client   IN NUMBER,
    p_id_produit  IN NUMBER,
    p_quantite    IN NUMBER,
    p_id_commande OUT NUMBER
)
    IS

    v_stock_actuel NUMBER;
    v_prix_produit NUMBER;
    v_montant      NUMBER;
    v_client       NUMBER;

BEGIN

    -- Vérification des paramètres
    IF p_id_client IS NULL OR p_id_produit IS NULL THEN
        RAISE_APPLICATION_ERROR(
                -20001,
                'Le client et le produit sont obligatoires.'
        );
    END IF;

    IF p_quantite IS NULL OR p_quantite <= 0 THEN
        RAISE_APPLICATION_ERROR(
                -20002,
                'La quantite doit etre strictement positive.'
        );
    END IF;

    -- Vérification de l'existence du client
    SELECT id_client
    INTO v_client
    FROM client
    WHERE id_client = p_id_client;

    -- Lecture et verrouillage du produit
    SELECT quantite_stock, prix
    INTO v_stock_actuel, v_prix_produit
    FROM produit
    WHERE id_produit = p_id_produit
        FOR UPDATE;

    -- Vérification du stock
    IF v_stock_actuel < p_quantite THEN
        RAISE_APPLICATION_ERROR(
                -20003,
                'Stock insuffisant pour ce produit.'
        );
    END IF;

    -- Calcul du montant total
    v_montant := calculer_montant_ligne(
            p_quantite,
            v_prix_produit
                 );

    -- Création de la commande
    INSERT INTO commande (
        id_client,
        date_commande,
        statut,
        montant_total
    )
    VALUES (
               p_id_client,
               SYSDATE,
               'CONFIRMEE',
               v_montant
           )
    RETURNING id_commande INTO p_id_commande;

    -- Insertion de la ligne de commande
    INSERT INTO ligne_commande (
        id_commande,
        id_produit,
        quantite,
        prix_unitaire
    )
    VALUES (
               p_id_commande,
               p_id_produit,
               p_quantite,
               v_prix_produit
           );

    -- Mise à jour du stock
    -- Le trigger enregistre automatiquement le mouvement
    UPDATE produit
    SET quantite_stock = quantite_stock - p_quantite
    WHERE id_produit = p_id_produit;

    -- Pas de COMMIT ici :
    -- Spring Boot contrôle la transaction.

EXCEPTION

    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(
                -20004,
                'Client ou produit introuvable.'
        );

    WHEN OTHERS THEN
        RAISE;

END;
/
