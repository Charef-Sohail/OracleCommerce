/* OracleCommerce — SQL editor (no dependencies) */
(function () {
  'use strict';

  var $ = function (id) { return document.getElementById(id); };
  var input = $('sql'), results = $('results');
  var runBtn = $('run-btn'), commitBtn = $('commit-btn'), rollbackBtn = $('rollback-btn');
  var busy = false;

  var SAMPLES = {
    insert: "INSERT INTO categorie (id, nom, description, active)\nVALUES (seq_categorie.NEXTVAL, 'Demo COMMIT', 'ligne non validée', 1);",
    select: "SELECT id, nom, description, active FROM categorie ORDER BY id;",
    delete: "DELETE FROM categorie WHERE nom = 'Demo COMMIT';",
    commit: "COMMIT;"
  };

  function mode() { return document.querySelector('input[name="mode"]:checked').value; }
  function esc(s) { return String(s).replace(/[&<>"]/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]; }); }
  function bytes(n) {
    if (n < 1024) return n + ' o';
    if (n < 1048576) return (n / 1024).toFixed(1) + ' Ko';
    return (n / 1048576).toFixed(2) + ' Mo';
  }

  // ------------------------------------------------------------------ rendering
  function renderResult(r) {
    var cls = r.ok ? (r.kind === 'COMMIT' ? 'ok' : r.kind === 'ROLLBACK' ? 'warn' : 'ok') : 'bad';
    var html = '<div class="card res"><div class="res-head ' + cls + '">'
      + '<span class="kind">' + esc(r.kind) + '</span>'
      + '<span class="msg">' + esc(r.message) + '</span>'
      + (r.ms ? '<span class="ms mono">' + r.ms + ' ms</span>' : '') + '</div>'
      + '<pre class="res-sql">' + esc(r.sql) + '</pre>';
    if (r.columns && r.columns.length) {
      html += '<div class="table-wrap"><table class="table"><thead><tr>'
        + r.columns.map(function (c) { return '<th>' + esc(c) + '</th>'; }).join('')
        + '</tr></thead><tbody>'
        + r.rows.map(function (row) {
            return '<tr>' + row.map(function (v) {
              return v === null ? '<td class="null">NULL</td>' : '<td>' + esc(v) + '</td>';
            }).join('') + '</tr>';
          }).join('')
        + '</tbody></table></div>';
    }
    return html + '</div>';
  }

  function renderResults(list) {
    var html = list.map(renderResult).join('');
    results.insertAdjacentHTML('afterbegin', html);
    // keep the last 8 result blocks
    while (results.children.length > 8) results.lastElementChild.remove();
  }

  function renderStatus(s) {
    if (!s) return;
    var open = !!s.open;
    var st = $('tx-state');
    st.dataset.open = open;
    st.textContent = open ? 'Transaction ouverte' : 'Aucune';

    $('k-redo').textContent = bytes(s.redoBytes);
    $('k-blocks').textContent = s.blockChanges;
    $('k-undo').textContent = s.undoBlocks + ' / ' + s.undoRecords;
    $('k-xid').textContent = open ? s.xid : '—';
    $('k-start').textContent = open ? s.startTime : '—';
    $('k-synch').textContent = s.redoSynchWrites;
    $('k-commits').textContent = s.commits;

    var flow = $('flow');
    flow.dataset.open = open;
    $('flow-cache').textContent = open
      ? s.blockChanges + ' modification(s) de blocs dans le buffer cache (blocs « dirty »)'
      : 'Aucun bloc modifié par cette transaction';
    $('flow-redo').textContent = open
      ? bytes(s.redoBytes) + ' de redo générés dans le redo log buffer'
      : 'Rien en attente';
    $('flow-file').textContent = open
      ? 'Pas de COMMIT : écriture non forcée (le LGWR écrit aussi en arrière-plan, mais la transaction reste annulable)'
      : 'Écrit par le LGWR au COMMIT (redo synch writes +1)';

    var err = $('tx-error');
    err.hidden = !s.error;
    err.textContent = s.error ? 'Statistiques indisponibles : ' + s.error : '';
  }

  // ------------------------------------------------------------------ calls
  function setBusy(b) {
    busy = b;
    [runBtn, commitBtn, rollbackBtn].forEach(function (x) { x.disabled = b; });
  }

  function call(url, body) {
    if (busy) return;
    setBusy(true);
    var opts = { method: body === undefined ? 'GET' : 'POST', headers: { 'Accept': 'application/json' } };
    if (body !== undefined && body !== null) {
      opts.headers['Content-Type'] = 'application/json';
      opts.body = JSON.stringify(body);
    }
    return fetch(url, opts)
      .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
      .then(function (data) {
        if (data.results) renderResults(data.results);
        renderStatus(data.status || (data.open !== undefined ? data : null));
      })
      .catch(function (e) {
        renderResults([{ kind: 'ERROR', ok: false, message: 'Appel impossible : ' + e.message, sql: '', ms: 0, columns: [], rows: [] }]);
      })
      .then(function () { setBusy(false); });
  }

  function run() {
    var sql = input.value.trim();
    if (!sql) return;
    call('/api/sql/execute', { sql: sql, mode: mode() });
  }

  runBtn.addEventListener('click', run);
  commitBtn.addEventListener('click', function () { call('/api/sql/commit', null); });
  rollbackBtn.addEventListener('click', function () { call('/api/sql/rollback', null); });

  input.addEventListener('keydown', function (e) {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); run(); }
    if (e.key === 'Tab') {            // tab inserts two spaces instead of leaving the editor
      e.preventDefault();
      var s = input.selectionStart;
      input.value = input.value.slice(0, s) + '  ' + input.value.slice(input.selectionEnd);
      input.selectionStart = input.selectionEnd = s + 2;
    }
  });

  document.querySelectorAll('[data-sample]').forEach(function (b) {
    b.addEventListener('click', function () { input.value = SAMPLES[b.dataset.sample]; input.focus(); });
  });

  document.querySelectorAll('input[name="mode"]').forEach(function (r) {
    r.addEventListener('change', function () {
      $('mode-hint').hidden = mode() !== 'other';
      commitBtn.disabled = rollbackBtn.disabled = false;
    });
  });

  // ------------------------------------------------------------------ status polling
  function poll() {
    if (busy || document.hidden) return;
    fetch('/api/sql/status', { headers: { 'Accept': 'application/json' } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(renderStatus)
      .catch(function () { });
  }
  poll();
  setInterval(poll, 4000);

  // closing the tab with an open transaction: it will be rolled back when the HTTP session expires
  window.addEventListener('beforeunload', function (e) {
    if ($('tx-state').dataset.open === 'true') { e.preventDefault(); e.returnValue = ''; }
  });
})();
