
-- =========================================================
-- FICHIER : 02_sequences_triggers.sql
-- PROJET : OracleCommerce
-- =========================================================

CREATE OR REPLACE TRIGGER trg_mouvement_stock
    AFTER INSERT OR UPDATE OF quantite_stock
    ON produit
    FOR EACH ROW

DECLARE
    v_type VARCHAR2(20);
    v_diff NUMBER;

BEGIN

    -- Cas 1 : Nouveau produit
    IF INSERTING THEN

        IF :NEW.quantite_stock > 0 THEN
            v_type := 'ENTREE';
            v_diff := :NEW.quantite_stock;
        ELSE
            RETURN;
        END IF;

        -- Cas 2 : Modification du stock
    ELSIF UPDATING THEN

        IF :NEW.quantite_stock > :OLD.quantite_stock THEN

            v_type := 'ENTREE';
            v_diff := :NEW.quantite_stock - :OLD.quantite_stock;

        ELSIF :NEW.quantite_stock < :OLD.quantite_stock THEN

            v_type := 'SORTIE';
            v_diff := :OLD.quantite_stock - :NEW.quantite_stock;

        ELSE
            RETURN;
        END IF;

    END IF;

    -- Enregistrement automatique du mouvement
    INSERT INTO mouvement_stock (
        id_produit,
        type_mouvement,
        quantite,
        date_mouvement
    )
    VALUES (
               :NEW.id_produit,
               v_type,
               v_diff,
               SYSDATE
           );

END;
/
