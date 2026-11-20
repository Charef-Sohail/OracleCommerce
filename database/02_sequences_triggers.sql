
-- =========================================================
-- FICHIER : 02_sequences_triggers.sql
-- PROJET  : OracleCommerce
-- =========================================================

-- =========================================================
-- TRIGGER : Enregistrement automatique des mouvements
-- =========================================================

CREATE OR REPLACE TRIGGER TRG_MOUVEMENT_STOCK
    AFTER INSERT OR UPDATE OF QUANTITE_STOCK
    ON PRODUIT
    FOR EACH ROW

DECLARE
    V_TYPE       VARCHAR2(20);
V_DIFF       NUMBER;
V_QUANTITE_AVANT NUMBER;
V_QUANTITE_APRES NUMBER;

BEGIN

    -- Cas 1 : Insertion d'un nouveau produit
IF INSERTING THEN

        IF :NEW.QUANTITE_STOCK > 0 THEN
            V_TYPE := 'ENTREE';
V_DIFF := :NEW.QUANTITE_STOCK;
V_QUANTITE_AVANT := 0;
V_QUANTITE_APRES := :NEW.QUANTITE_STOCK;
ELSE
            RETURN;
END IF;

-- Cas 2 : Modification du stock
ELSIF UPDATING THEN

        IF :NEW.QUANTITE_STOCK > :OLD.QUANTITE_STOCK THEN

            V_TYPE := 'ENTREE';
V_DIFF := :NEW.QUANTITE_STOCK - :OLD.QUANTITE_STOCK;

ELSIF :NEW.QUANTITE_STOCK < :OLD.QUANTITE_STOCK THEN

            V_TYPE := 'SORTIE';
V_DIFF := :OLD.QUANTITE_STOCK - :NEW.QUANTITE_STOCK;

ELSE
            RETURN;
END IF;

V_QUANTITE_AVANT := :OLD.QUANTITE_STOCK;
V_QUANTITE_APRES := :NEW.QUANTITE_STOCK;

END IF;

-- Enregistrement du mouvement de stock
INSERT INTO MOUVEMENT_STOCK (
    ID,
    TYPE_MOUVEMENT,
    QUANTITE,
    QUANTITE_AVANT,
    QUANTITE_APRES,
    MOTIF,
    DATE_MOUVEMENT,
    PRODUIT_ID
)
VALUES (
           SEQ_MOUVEMENT_STOCK.NEXTVAL,
           V_TYPE,
           V_DIFF,
           V_QUANTITE_AVANT,
           V_QUANTITE_APRES,
           'Mouvement automatique par trigger',
           SYSDATE,
           :NEW.ID
       );

END;
/
