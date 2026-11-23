/* ==========================================================
   Oracle en direct — YOUR requests
   Polls /api/oracle/events and makes each user action (adding a product,
   a category…) travel through the diagram as a labelled white dot, following
   the real SQL statements Hibernate sent for that request.
   ========================================================== */
(function () {
  'use strict';
  if (/[?&]embed\b/.test(location.search)) document.documentElement.classList.add('embed');
  var S = window.OracleStage;
  if (!S) return;

  var SVGNS = 'http://www.w3.org/2000/svg';
  var API = '/api/oracle/events';
  var POLL_MS = 1000;

  var lastId = -1, apiOk = null, showReads = false;
  var feed = [];            // all known events (newest last)
  var queue = [], running = false, currentId = null;
  var userDirty = 0;

  function $(s) { return document.querySelector(s); }
  function el(tag, attrs, parent, text) {
    var n = document.createElementNS(SVGNS, tag);
    Object.keys(attrs || {}).forEach(function (k) { n.setAttribute(k, attrs[k]); });
    if (text != null) n.textContent = text;
    if (parent) parent.appendChild(n);
    return n;
  }
  function sleep(ms) { return new Promise(function (r) { setTimeout(r, S.reduced ? Math.min(ms, 150) : ms); }); }

  /* ------------------------------------------------------- The dot */
  function makeDot(label, small) {
    var g = el('g', { class: 'udot' + (small ? ' small' : ''), opacity: 0 }, S.layer);
    el('circle', { class: 'ring', r: 10 }, g);
    el('circle', { class: 'core', r: small ? 4.5 : 6 }, g);
    var pill = el('g', { class: 'pill' }, g);
    var rect = el('rect', { rx: 6, height: 32, y: 0 }, pill);
    var l1 = el('text', { class: 'l1', x: 8, y: 14 }, pill, label);
    var l2 = el('text', { class: 'l2', x: 8, y: 26 }, pill, '');
    return { g: g, pill: pill, rect: rect, l1: l1, l2: l2, label: label, w: 100, x: 0, y: 0 };
  }
  function setPhase(dot, phase) {
    dot.l2.textContent = phase || '';
    dot.w = Math.max(dot.label.length * 6.9, (phase || '').length * 5.9) + 16;
    dot.rect.setAttribute('width', dot.w.toFixed(0));
    place(dot, dot.x, dot.y);
  }
  function place(dot, x, y) {
    dot.x = x; dot.y = y;
    dot.g.setAttribute('transform', 'translate(' + x.toFixed(1) + ',' + y.toFixed(1) + ')');
    var right = x + dot.w + 24 < 1195, below = y < 60;
    dot.pill.setAttribute('transform', 'translate(' + (right ? 14 : -14 - dot.w) + ',' + (below ? 8 : -40) + ')');
  }
  function move(dot, pathId, rev, dur, phase) {
    setPhase(dot, phase);
    var pi = S.pathInfo(pathId);
    return new Promise(function (resolve) {
      var t0 = null, total = (S.reduced ? 0.2 : dur) * 1000;
      (function step(ts) {
        if (t0 === null) t0 = ts;
        var t = Math.min(1, (ts - t0) / total), e = t < .5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
        var pt = pi.p.getPointAtLength((rev ? 1 - e : e) * pi.len);
        place(dot, pt.x, pt.y); dot.g.setAttribute('opacity', 1);
        if (t < 1) requestAnimationFrame(step); else resolve();
      })(performance.now());
    });
  }
  function fade(dot) {
    dot.g.style.transition = 'opacity .5s'; dot.g.setAttribute('opacity', 0);
    setTimeout(function () { dot.g.remove(); }, 600);
  }
  function hot(nodes) {
    S.clearHot();
    (nodes || []).forEach(function (k) { var n = S.nodeEl(k); if (n) n.classList.add('hot'); });
  }

  /* ------------------------------------------------------- Choreography */
  var TXT = {
    conn: "Votre requête quitte l'application, traverse le listener et arrive au processus serveur dédié à la session.",
    parse: "Oracle cherche chaque ordre SQL dans la library cache : déjà connu = soft parse, sinon hard parse.",
    seq: "Le prochain identifiant est pris dans la séquence (dictionary cache), sans verrou sur la table.",
    select: "Lecture des blocs de la table dans le buffer cache (lecture logique).",
    write: "Le bloc de la table est modifié en mémoire : il devient « dirty ». Rien n'est écrit dans le datafile.",
    redo: "Le changement est décrit dans le redo log buffer, avant d'être écrit.",
    commit: "COMMIT : LGWR écrit le redo dans le groupe CURRENT et attend le disque. La transaction est maintenant durable.",
    reply: "La réponse repart vers l'application."
  };

  var DML = { INSERT: 1, UPDATE: 1, DELETE: 1, MERGE: 1 };
  function isSql(ev) { return ev.source === 'sql' || ev.source === 'sql-other'; }
  function sqlType(ev) { return (ev.label || '').replace(/^SQL · /, '').split(' ')[0]; }
  function holdLevel(tx) { return 0.15 + 0.6 * Math.min(1, ((tx && tx.txRedoBytes) || 0) / 4096); }
  function bytes(n) { return n >= 1024 ? (n / 1024).toFixed(1) + ' Ko' : n + ' o'; }

  /* Events coming from the SQL editor: the numbers shown are the ones measured by Oracle (V$MYSTAT, V$TRANSACTION). */
  function buildSqlPhases(ev) {
    var P = [], stm = ev.statements || [], tx = ev.tx || {}, type = sqlType(ev), other = ev.source === 'sql-other';
    P.push({ path: 'p-conn', nodes: ['app', 'listener', 'server'], phase: other ? 'SQL · autre session' : 'SQL · session éditeur', title: 'La requête arrive au processus serveur',
             text: other ? "Une autre session (nouvelle connexion du pool) envoie son SELECT : un autre processus serveur le traite." : TXT.conn });
    if (type === 'COMMIT') {
      P.push({ path: 'p-parse', nodes: ['server', 'sharedpool'], phase: 'COMMIT', title: 'COMMIT', text: "Le processus serveur reçoit COMMIT : un enregistrement de validation est placé dans le redo log buffer.", dur: .9 });
      P.push({ path: 'p-lgwr', nodes: ['bg-LGWR', 'redobuf'], phase: 'LGWR vide le redo buffer', title: 'COMMIT · LGWR', fx: 'flush', dur: .8,
               text: "LGWR écrit tout le redo de la transaction (" + bytes(tx.txRedoBytes || 0) + ", " + (tx.txBlockChanges || 0) + " changements de blocs) et la session attend : c'est le « log file sync »." });
      P.push({ path: 'p-lgwr2', nodes: ['bg-LGWR', 'redologs'], phase: 'redo synch writes +' + (tx.redoSynchWrites || 0), title: 'COMMIT · écriture disque',
               text: "Le redo est dans le groupe CURRENT. La statistique « redo synch writes » a augmenté de " + (tx.redoSynchWrites || 0) + " : la transaction " + (tx.xid || '') + " est maintenant durable. Les blocs, eux, seront écrits plus tard par DBWn.", dur: 1.2 });
    } else if (type === 'ROLLBACK') {
      P.push({ path: 'p-lread', nodes: ['server', 'buffercache'], phase: 'undo : ' + (tx.undoRecords || 0) + ' enregistrement(s)', title: 'ROLLBACK', fx: 'undo',
               text: "Le processus serveur relit l'undo (" + (tx.undoBlocks || 0) + " bloc(s), " + (tx.undoRecords || 0) + " enregistrement(s)) et remet les blocs du buffer cache dans leur état d'avant. Le redo en attente est abandonné." });
    } else {
      P.push({ path: 'p-parse', nodes: ['server', 'sharedpool'], phase: 'parse · ' + stm.length + ' ordre(s)', title: 'Parse', text: TXT.parse });
      stm.forEach(function (s) {
        var n = s.count > 1 ? ' ×' + s.count : '';
        if (s.type === 'SEQ') P.push({ path: 'p-parse', nodes: ['sharedpool'], phase: 'séquence ' + s.table, title: 'Séquence ' + s.table, text: TXT.seq, dur: .9 });
        else if (s.type === 'SELECT') P.push({ path: 'p-lread', nodes: ['server', 'buffercache'], phase: 'SELECT ' + s.table + n, title: 'SELECT sur ' + s.table,
                 text: other ? "Lecture cohérente : cette session ne voit que les données validées" + (tx.open ? " (une transaction non validée existe dans l'éditeur : l'undo sert à reconstruire l'ancienne image)." : ".") : TXT.select, dur: .9 });
        else P.push({ path: 'p-lread', nodes: ['server', 'buffercache'], phase: s.type + ' ' + s.table + n + ' · ' + (tx.stmtBlockChanges || 0) + ' bloc(s)', title: s.type + ' sur ' + s.table, fx: 'dirty', n: Math.min(4, Math.max(1, tx.stmtBlockChanges || 1)),
                 text: "Le bloc est modifié dans le buffer cache (" + (tx.stmtBlockChanges || 0) + " changement(s) de bloc mesurés), l'ancienne valeur est copiée dans l'undo (" + (tx.undoRecords || 0) + " enregistrement(s) au total)." });
      });
      if (DML[type]) {
        P.push({ path: 'p-redo', nodes: ['server', 'redobuf'], phase: 'redo +' + bytes(tx.stmtRedoBytes || 0) + ' · en attente', title: 'Redo log buffer', fx: 'hold',
                 text: "Le changement est décrit dans le redo log buffer (+" + bytes(tx.stmtRedoBytes || 0) + ", transaction " + (tx.xid || '?') + ", " + tx.pending + " ordre(s) en attente). Aucun COMMIT : la transaction n'est pas validée et le redo n'est pas forcé sur disque. (LGWR peut en écrire une partie toutes les 3 s, mais seul COMMIT valide la transaction.)" });
      }
    }
    var tail = type === 'COMMIT' ? 'validé ✓' : type === 'ROLLBACK' ? 'annulé' : DML[type] ? 'NON VALIDÉ' : 'OK';
    P.push({ path: 'p-conn', rev: true, nodes: ['server', 'app'], phase: tail + ' · ' + ev.durationMs + ' ms', title: 'Réponse', text: TXT.reply + ' Durée : ' + ev.durationMs + ' ms.' });
    return P;
  }

  function buildPhases(ev) {
    if (isSql(ev)) return buildSqlPhases(ev);
    var P = [], stm = ev.statements || [];
    P.push({ path: 'p-conn', nodes: ['app', 'listener', 'server'], phase: ev.method + ' ' + ev.path, title: 'La requête part de l\'application', text: TXT.conn });
    P.push({ path: 'p-parse', nodes: ['server', 'sharedpool'], phase: 'parse · ' + stm.length + ' type(s) de SQL', title: 'Parse', text: TXT.parse });
    stm.forEach(function (s) {
      var n = s.count > 1 ? ' ×' + s.count : '';
      if (s.type === 'SEQ') P.push({ path: 'p-parse', nodes: ['sharedpool'], phase: 'séquence ' + s.table, title: 'Séquence ' + s.table, text: TXT.seq, dur: .9 });
      else if (s.type === 'SELECT') P.push({ path: 'p-lread', nodes: ['server', 'buffercache'], phase: 'SELECT ' + s.table + n, title: 'SELECT sur ' + s.table, text: TXT.select, dur: .9 });
      else P.push({ path: 'p-lread', nodes: ['server', 'buffercache'], phase: s.type + ' ' + s.table + n, title: s.type + ' sur ' + s.table, text: TXT.write, fx: 'dirty' });
    });
    if (ev.write) {
      P.push({ path: 'p-redo', nodes: ['server', 'redobuf'], phase: 'redo généré', title: 'Redo log buffer', text: TXT.redo, fx: 'redo' });
      P.push({ path: 'p-lgwr', nodes: ['bg-LGWR', 'redobuf'], phase: 'COMMIT · LGWR', title: 'COMMIT', text: TXT.commit, fx: 'flush', dur: .7 });
      P.push({ path: 'p-lgwr2', nodes: ['bg-LGWR', 'redologs'], phase: 'écriture du redo log', title: 'COMMIT', text: TXT.commit, dur: 1.2 });
    }
    P.push({ path: 'p-conn', rev: true, nodes: ['server', 'app'], phase: 'OK · ' + ev.durationMs + ' ms', title: 'Réponse', text: TXT.reply + ' Durée côté serveur : ' + ev.durationMs + ' ms.' });
    return P;
  }

  function cleanDirty() {
    S.blocks.filter(function (b) { return b.s === 'dirty'; }).forEach(function (b, j) {
      setTimeout(function () { S.setBlock(b, 'clean'); }, j * 120);
    });
    userDirty = 0;
  }
  function applyFx(fx, p, ev) {
    if (fx === 'dirty') {
      for (var k = 0; k < ((p && p.n) || 1); k++) S.setBlock(S.pickBlock('clean') || S.pickBlock('free'), 'dirty');
      userDirty += (p && p.n) || 1;
    } else if (fx === 'redo') S.bumpRedo(0.35);
    else if (fx === 'hold') S.setRedoHold(holdLevel(ev.tx));
    else if (fx === 'undo') { cleanDirty(); S.dropRedo(); }
    else if (fx === 'flush') S.flushRedo();
  }

  function playEvent(ev) {
    currentId = ev.id; renderFeed();
    var dot = makeDot(ev.label, false), phases = buildPhases(ev), i = 0;
    return (function next() {
      if (i >= phases.length) {
        fade(dot); S.clearHot(); deferredWrite(ev);
        return sleep(500);
      }
      var p = phases[i++];
      hot(p.nodes);
      if (!S.busy()) S.showCaption('Votre action · ' + (i) + ' / ' + phases.length, ev.label + ' — ' + p.title, p.text);
      return move(dot, p.path, !!p.rev, p.dur || 1.15, p.phase).then(function () {
        applyFx(p.fx, p, ev); return sleep(260);
      }).then(next);
    })();
  }

  /* DBWn writes the dirty blocks later, on its own time */
  function deferredWrite(ev) {
    if (isSql(ev) ? sqlType(ev) !== 'COMMIT' : !ev.write) return;   // editor DML stays in the buffer until COMMIT
    setTimeout(function () {
      var d = makeDot('DBWn', true);
      move(d, 'p-dbwr', false, 1.4, 'écrit les blocs dirty').then(function () {
        S.pulseChip('DBW');
        return move(d, 'p-write', false, 1.3, 'vers les datafiles');
      }).then(function () {
        fade(d); cleanDirty();
      });
    }, 2600);
  }

  /* ------------------------------------------------------- Queue */
  function enqueue(ev) {
    if (!ev.write && !isSql(ev) && !showReads) return;
    queue.push(ev); if (queue.length > 6) queue.shift();
    pump();
  }
  function pump() {
    if (running) return;
    if (!queue.length) return;
    if (S.busy() || S.isPaused()) { setTimeout(pump, 800); return; }   // don't fight a guided scenario
    running = true;
    playEvent(queue.shift()).then(function () { running = false; currentId = null; renderFeed(); pump(); });
  }

  /* ------------------------------------------------------- Feed */
  function esc(s) { return String(s).replace(/[&<>"]/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]; }); }
  function badge(op) {
    var cls = { Ajout: 'badge-actif', Modification: 'badge-info', Suppression: 'badge-bad', Annulation: 'badge-bad', COMMIT: 'badge-actif', ROLLBACK: 'badge-bad' }[op] || '';
    return '<span class="badge ' + cls + '">' + esc(op) + '</span>';
  }
  function txLine(e) {
    var t = e.tx || {}, ty = sqlType(e);
    if (ty === 'COMMIT') return 'COMMIT · redo synch writes +' + (t.redoSynchWrites || 0) + ' · ' + bytes(t.txRedoBytes || 0) + ' durables';
    if (ty === 'ROLLBACK') return 'ROLLBACK · ' + (t.undoRecords || 0) + ' enreg. undo relus';
    if (DML[ty]) return 'non validé · redo ' + bytes(t.txRedoBytes || 0) + ' · ' + (t.txBlockChanges || 0) + ' blocs · undo ' + (t.undoRecords || 0);
    return ev_src(e);
  }
  function ev_src(e) { return e.source === 'sql-other' ? 'autre session · données validées' : 'session éditeur'; }
  function renderFeed() {
    var ol = $('#user-feed'); if (!ol) return;
    var items = feed.filter(function (e) { return e.write || isSql(e) || showReads; }).slice(-30).reverse();
    if (!items.length) {
      ol.innerHTML = '<li class="feed-empty">Ajoutez un produit ou une catégorie dans l\'application : l\'action apparaît ici et traverse le schéma sous forme de point lumineux.</li>';
      return;
    }
    ol.innerHTML = items.map(function (e) {
      var d = new Date(e.timestamp), t = d.toTimeString().slice(0, 8);
      var sql = e.statements.map(function (s) {
        var w = s.type === 'INSERT' || s.type === 'UPDATE' || s.type === 'DELETE';
        return '<code class="' + (w ? 'w' : '') + '" title="' + esc(s.sql) + '">' + s.type + ' ' + esc(s.table) + (s.count > 1 ? ' ×' + s.count : '') + '</code>';
      }).join('');
      return '<li data-id="' + e.id + '" class="' + (e.id === currentId ? 'playing' : '') + '" title="Cliquer pour rejouer">' +
        '<span class="t">' + t + '</span>' +
        '<span>' + badge(e.operation) + '</span>' +
        '<span class="what"><b>' + esc(e.label) + '</b><small>' + (isSql(e) ? txLine(e) : esc(e.method) + ' ' + esc(e.path)) + '</small><div class="sql" style="margin-top:6px">' + sql + '</div></span>' +
        '<span class="ms">' + e.durationMs + ' ms</span></li>';
    }).join('');
  }
  $('#user-feed').addEventListener('click', function (e) {
    var li = e.target.closest('li[data-id]'); if (!li) return;
    var ev = feed.filter(function (x) { return String(x.id) === li.getAttribute('data-id'); })[0];
    if (ev) { queue.push(ev); pump(); }
  });

  /* ------------------------------------------------------- Toggles */
  $('#ambient-btn').addEventListener('click', function () {
    var on = !S.ambient(); S.setAmbient(on);
    this.setAttribute('aria-pressed', on); this.textContent = 'Activité de fond : ' + (on ? 'ON' : 'OFF');
  });
  $('#show-reads').addEventListener('change', function () { showReads = this.checked; renderFeed(); });

  /* ------------------------------------------------------- Polling */
  function setApi(ok) {
    if (apiOk === ok) return; apiOk = ok;
    var b = $('#sim-action'); if (b) b.hidden = ok;
  }
  /* page opened while an editor transaction is still uncommitted: show what is really still pending */
  function restorePending(evs) {
    var last = null;
    evs.forEach(function (e) { if (e.source === 'sql') last = e; });
    if (!last || !last.tx || !last.tx.open || sqlType(last) === 'COMMIT' || sqlType(last) === 'ROLLBACK') return;
    S.setRedoHold(holdLevel(last.tx));
    for (var k = 0; k < Math.min(6, Math.max(1, last.tx.txBlockChanges || 1)); k++) S.setBlock(S.pickBlock('clean') || S.pickBlock('free'), 'dirty');
  }
  function poll() {
    fetch(API + '?since=' + lastId, { headers: { Accept: 'application/json' } })
      .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
      .then(function (page) {
        var first = lastId < 0; lastId = page.last; setApi(true);
        (page.events || []).forEach(function (ev) {
          if (feed.some(function (x) { return x.id === ev.id; })) return;
          feed.push(ev); if (!first) enqueue(ev);
        });
        if (first) restorePending(page.events || []);
        renderFeed();
      })
      .catch(function () { setApi(false); })
      .then(function () { setTimeout(poll, POLL_MS); });
  }

  /* ------------------------------------------------------- Simulation (API unreachable) */
  var simId = 1000, simN = 0;
  var SIM = [
    { method: 'POST', path: '/produits', operation: 'Ajout', entity: 'produit', write: true, status: 302, durationMs: 38, statements: [
      { type: 'SELECT', table: 'PRODUIT', sql: 'select p1_0.id from produit p1_0 where p1_0.reference=?', count: 1 },
      { type: 'SELECT', table: 'CATEGORIE', sql: 'select c1_0.id,c1_0.nom from categorie c1_0 where c1_0.id=?', count: 1 },
      { type: 'SEQ', table: 'SEQ_PRODUIT', sql: 'select SEQ_PRODUIT.nextval from dual', count: 1 },
      { type: 'INSERT', table: 'PRODUIT', sql: 'insert into produit (actif,categorie_id,description,nom,prix,quantite_stock,reference,seuil_alerte,id) values (?,?,?,?,?,?,?,?,?)', count: 1 }] },
    { method: 'POST', path: '/categories', operation: 'Ajout', entity: 'catégorie', write: true, status: 302, durationMs: 24, statements: [
      { type: 'SELECT', table: 'CATEGORIE', sql: 'select count(*) from categorie where nom=?', count: 1 },
      { type: 'SEQ', table: 'SEQ_CATEGORIE', sql: 'select SEQ_CATEGORIE.nextval from dual', count: 1 },
      { type: 'INSERT', table: 'CATEGORIE', sql: 'insert into categorie (active,description,nom,id) values (?,?,?,?)', count: 1 }] },
    { method: 'POST', path: '/produits/4/edit', operation: 'Modification', entity: 'produit', write: true, status: 302, durationMs: 31, statements: [
      { type: 'SELECT', table: 'PRODUIT', sql: 'select p1_0.id from produit p1_0 where p1_0.id=?', count: 2 },
      { type: 'UPDATE', table: 'PRODUIT', sql: 'update produit set nom=?,prix=? where id=?', count: 1 }] },
    { method: 'GET', path: '/produits', operation: 'Consultation', entity: 'produit', write: false, status: 200, durationMs: 18, statements: [
      { type: 'SELECT', table: 'PRODUIT', sql: 'select p1_0.id from produit p1_0', count: 1 },
      { type: 'SELECT', table: 'CATEGORIE', sql: 'select c1_0.id from categorie c1_0 where c1_0.id=?', count: 6 }] }
  ];
  var simBtn = $('#sim-action');
  simBtn.addEventListener('click', function () {
    var base = SIM[simN++ % SIM.length], ev = JSON.parse(JSON.stringify(base));
    ev.id = ++simId; ev.timestamp = Date.now(); ev.label = ev.operation + ' ' + ev.entity;
    feed.push(ev); renderFeed(); enqueue(ev);
  });

  renderFeed();
  poll();
})();
