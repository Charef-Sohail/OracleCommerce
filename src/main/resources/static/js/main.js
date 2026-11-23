/* OracleCommerce — shared behaviour (no dependencies) */
(function () {
  'use strict';

  // Mobile sidebar
  var side = document.getElementById('sidebar');
  document.querySelectorAll('[data-toggle-sidebar]').forEach(function (b) {
    b.addEventListener('click', function () { side && side.classList.toggle('open'); });
  });
  document.addEventListener('click', function (e) {
    if (side && side.classList.contains('open') && !side.contains(e.target) && !e.target.closest('[data-toggle-sidebar]')) {
      side.classList.remove('open');
    }
  });

  // Confirm before destructive POSTs: <form data-confirm="Supprimer ?">
  document.addEventListener('submit', function (e) {
    var msg = e.target.getAttribute && e.target.getAttribute('data-confirm');
    if (msg && !window.confirm(msg)) e.preventDefault();
  });

  // Auto-dismiss toasts
  document.querySelectorAll('.toast').forEach(function (t) {
    setTimeout(function () { t.classList.add('out'); setTimeout(function () { t.remove(); }, 350); }, 4200);
  });

  // Highlight tab links matching the current path (data-tab)
  var path = location.pathname.replace(/\/$/, '') || '/';
  document.querySelectorAll('.tabs a[data-tab]').forEach(function (a) {
    var p = a.getAttribute('data-tab');
    if (p === path) a.classList.add('is-active');
  });

  // Client-side table filter: <input data-filter="#tableId">
  document.querySelectorAll('input[data-filter]').forEach(function (input) {
    var table = document.querySelector(input.getAttribute('data-filter'));
    if (!table) return;
    var counter = document.querySelector('[data-count-for="' + input.getAttribute('data-filter') + '"]');
    function apply() {
      var q = input.value.trim().toLowerCase(), shown = 0;
      table.querySelectorAll('tbody tr').forEach(function (tr) {
        var ok = !q || tr.textContent.toLowerCase().indexOf(q) !== -1;
        tr.hidden = !ok; if (ok) shown++;
      });
      if (counter) counter.textContent = shown + ' résultat' + (shown > 1 ? 's' : '');
    }
    input.addEventListener('input', apply); apply();
  });

  // Animated counters on the dashboard: <span data-count="123">
  document.querySelectorAll('[data-count]').forEach(function (el) {
    var target = parseFloat(el.getAttribute('data-count')) || 0;
    var decimals = parseInt(el.getAttribute('data-decimals') || '0', 10);
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || target === 0) { return; }
    var start = performance.now(), dur = 900;
    function fmt(v) { return v.toLocaleString('fr-FR', { minimumFractionDigits: decimals, maximumFractionDigits: decimals }); }
    (function tick(now) {
      var p = Math.min(1, (now - start) / dur), eased = 1 - Math.pow(1 - p, 3);
      el.textContent = fmt(target * eased);
      if (p < 1) requestAnimationFrame(tick);
    })(start);
  });
})();
