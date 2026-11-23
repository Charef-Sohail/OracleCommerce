/* Dynamic order lines: add/remove rows, reindex lignes[i].*, live totals,
   no duplicate product (DB has UNIQUE(COMMANDE_ID, PRODUIT_ID)). */
(function () {
  'use strict';
  var body = document.getElementById('lines');
  var tpl = document.getElementById('line-tpl');
  var totalEl = document.getElementById('grand-total');
  if (!body || !tpl) return;

  var money = function (n) {
    return n.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  function addRow(produitId, quantite) {
    var row = tpl.content.firstElementChild.cloneNode(true);
    body.appendChild(row);
    var sel = row.querySelector('[data-role=produit]');
    if (produitId) sel.value = String(produitId);
    if (quantite) row.querySelector('[data-role=quantite]').value = quantite;
    refresh();
  }

  function refresh() {
    var rows = body.querySelectorAll('tr.line');
    var taken = {};
    rows.forEach(function (r) { var v = r.querySelector('[data-role=produit]').value; if (v) taken[v] = true; });
    var total = 0;

    rows.forEach(function (row, i) {
      var sel = row.querySelector('[data-role=produit]');
      var qty = row.querySelector('[data-role=quantite]');
      sel.name = 'lignes[' + i + '].produitId';
      qty.name = 'lignes[' + i + '].quantite';

      // disable products already used in another row
      Array.prototype.forEach.call(sel.options, function (o) {
        o.disabled = !!o.value && taken[o.value] && o.value !== sel.value;
      });

      var opt = sel.options[sel.selectedIndex];
      var prix = opt && opt.dataset.prix ? parseFloat(opt.dataset.prix) : null;
      var stock = opt && opt.dataset.stock ? parseInt(opt.dataset.stock, 10) : null;
      qty.max = stock != null ? stock : '';
      var q = parseInt(qty.value, 10) || 0;
      var sub = prix != null ? prix * q : null;

      row.querySelector('[data-role=unit]').textContent = prix != null ? money(prix) : '—';
      row.querySelector('[data-role=sub]').textContent = sub != null ? money(sub) : '—';
      if (sub != null) total += sub;
    });
    totalEl.textContent = money(total);
  }

  body.addEventListener('input', refresh);
  body.addEventListener('change', refresh);
  body.addEventListener('click', function (e) {
    var btn = e.target.closest('[data-remove]');
    if (!btn) return;
    btn.closest('tr').remove();
    if (!body.querySelector('tr.line')) addRow();
    else refresh();
  });
  document.getElementById('add-line').addEventListener('click', function () { addRow(); });

  var initial = Array.isArray(window.INITIAL_LINES) ? window.INITIAL_LINES : [];
  if (initial.length) initial.forEach(function (l) { addRow(l.produitId, l.quantite); });
  else addRow();
})();
