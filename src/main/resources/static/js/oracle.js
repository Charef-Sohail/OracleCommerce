/* ==========================================================
   Oracle en direct — live architecture view
   - polls /api/oracle/internals (real V$ counters)
   - falls back to a simulation if the API is unreachable
   - particles flow along SVG paths at rates derived from real deltas
   ========================================================== */
(function () {
  'use strict';

  var SVGNS = 'http://www.w3.org/2000/svg';
  var API = (document.querySelector('base') ? '' : '') + '/api/oracle/internals';
  var POLL_MS = 2500;
  var reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  var C = { sql: '#f1ece4', parse: '#b69cf5', read: '#4fb3d9', write: '#e8a33d', redo: '#e0533d' };

  function $(s, r) { return (r || document).querySelector(s); }
  function $$(s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); }
  function el(tag, attrs, parent, text) {
    var n = document.createElementNS(SVGNS, tag);
    Object.keys(attrs || {}).forEach(function (k) { n.setAttribute(k, attrs[k]); });
    if (text != null) n.textContent = text;
    if (parent) parent.appendChild(n);
    return n;
  }
  function fmtBytes(b) {
    if (b == null || isNaN(b)) return '—';
    var u = ['o', 'Ko', 'Mo', 'Go', 'To'], i = 0;
    while (b >= 1024 && i < u.length - 1) { b /= 1024; i++; }
    return (b >= 100 || i === 0 ? Math.round(b) : b.toFixed(1)).toString().replace('.', ',') + ' ' + u[i];
  }
  function fmtNum(n) { return Math.round(n).toLocaleString('fr-FR'); }
  function fmtUptime(s) {
    var d = Math.floor(s / 86400), h = Math.floor(s % 86400 / 3600), m = Math.floor(s % 3600 / 60);
    return d ? d + ' j ' + h + ' h' : h ? h + ' h ' + m + ' min' : m + ' min ' + Math.floor(s % 60) + ' s';
  }
  function rnd(a, b) { return a + Math.random() * (b - a); }

  /* ------------------------------------------------------- Static explanations */
  var EXPLAIN = {
    app: ['Application Spring Boot', "Votre application ouvre un pool de connexions JDBC (HikariCP). Chaque requête SQL envoyée par Hibernate ou JdbcTemplate emprunte une de ces connexions vers Oracle."],
    listener: ['Listener', "Processus qui écoute sur le port 1521. Il reçoit la demande de connexion, la rattache au service (ici GLSID) et crée un processus serveur dédié à la session. Une fois la session établie, il n'intervient plus."],
    instance: ['Instance Oracle', "Une instance = la mémoire (SGA) + les processus d'arrière-plan. Elle ouvre et gère les fichiers de la base. Sans instance, les fichiers ne sont que des octets au repos."],
    sga: ['SGA — System Global Area', "Mémoire partagée par tous les processus de l'instance. Elle contient le buffer cache, la shared pool et le redo log buffer. Sa taille se règle avec SGA_TARGET."],
    sharedpool: ['Shared Pool', "Garde les plans d'exécution déjà calculés (library cache) et les métadonnées du dictionnaire. Une requête déjà connue = soft parse, rapide. Une requête inédite = hard parse, coûteux. Les variables bind évitent les hard parses."],
    buffercache: ['Database Buffer Cache', "Copie en mémoire des blocs de données. Lire un bloc en cache coûte des microsecondes, sur disque des millisecondes. Les blocs modifiés mais pas encore écrits sont « dirty »."],
    redobuf: ['Redo Log Buffer', "Tampon circulaire où chaque modification est décrite avant tout. LGWR le vide vers les redo logs à chaque COMMIT, toutes les 3 secondes, ou quand il est rempli au tiers."],
    server: ['Processus serveur + PGA', "Chaque session a son processus serveur et sa PGA (Program Global Area) : mémoire privée pour les tris (sort area), jointures hash, variables et curseurs. Contrairement à la SGA, elle n'est pas partagée. Sa taille se règle avec PGA_AGGREGATE_TARGET."],
    bgzone: ['Processus d\'arrière-plan', "Les ouvriers de l'instance : ils écrivent, surveillent, nettoient et récupèrent, sans qu'aucune session ne les appelle."],
    'bg-DBW': ['DBWn — Database Writer', "Écrit les blocs dirty du buffer cache vers les datafiles, en différé : au checkpoint, quand le cache manque de place, ou périodiquement. Écrire tard est permis grâce au redo."],
    'bg-LGWR': ['LGWR — Log Writer', "Écrit le redo buffer dans les redo logs en ligne. Un COMMIT n'est terminé que lorsque LGWR a confirmé l'écriture disque : c'est ce qui rend la transaction durable (attente « log file sync »)."],
    'bg-CKPT': ['CKPT — Checkpoint', "Signale à DBWn de vider les blocs dirty et met à jour les en-têtes des datafiles et le control file. Plus le checkpoint est récent, plus la reprise après panne est courte."],
    'bg-SMON': ['SMON — System Monitor', "Fait la récupération d'instance au redémarrage (rejoue le redo, annule l'undo) et nettoie les segments temporaires."],
    'bg-PMON': ['PMON — Process Monitor', "Surveille les processus utilisateur : si une session meurt, il annule sa transaction et libère ses verrous et ses ressources."],
    'bg-ARC': ['ARCn — Archiver', "Copie chaque redo log plein vers l'archive. Actif seulement en mode ARCHIVELOG, indispensable pour restaurer à un instant précis."],
    'bg-MMON': ['MMON — Manageability Monitor', "Collecte les statistiques (AWR), déclenche les alertes et calcule les métriques utilisées par les assistants d'optimisation."],
    'bg-RECO': ['RECO — Recoverer', "Résout les transactions distribuées en doute entre plusieurs bases."],
    controlfile: ['Control files', "Petit fichier critique : noms et emplacements des datafiles et redo logs, numéro de checkpoint, historique des sauvegardes. Sans lui, la base ne s'ouvre pas — il est multiplexé."],
    archive: ['Archived redo logs', "Copies des redo logs pleins. En mode NOARCHIVELOG ils sont simplement écrasés : pratique pour un TP, insuffisant pour la production."],
    datafiles: ['Datafiles et tablespaces', "Les données réelles, stockées dans des tablespaces : SYSTEM (dictionnaire), SYSAUX, UNDOTBS (annulation), USERS… Vos tables CLIENT, PRODUIT, COMMANDE y résident."],
    redologs: ['Online redo log groups', "Groupes de fichiers écrits en rotation par LGWR. Un seul est CURRENT. Quand il est plein : log switch, et on passe au suivant. Le journal de tout ce qui n'est pas encore garanti dans les datafiles."]
  };

  /* ------------------------------------------------------- Scenarios */
  var SCENARIOS = [
    { id: 'select', label: 'SELECT (en cache)', steps: [
      { t: 'La requête part de l\'application', x: 'Spring Boot emprunte une connexion du pool HikariCP et envoie : SELECT * FROM produit WHERE actif = 1. La demande traverse le listener jusqu\'au processus serveur dédié à la session.', n: ['app', 'listener', 'server'], f: [['p-conn', 'sql']] },
      { t: 'Parse : la requête est-elle connue ?', x: 'Le processus serveur calcule le hash du texte SQL et le cherche dans la library cache de la shared pool. Trouvé : soft parse, on réutilise le plan. Absent : hard parse (optimiseur), bien plus coûteux.', n: ['server', 'sharedpool'], f: [['p-parse', 'parse']] },
      { t: 'Lecture des blocs en mémoire', x: 'Le plan demande les blocs de la table PRODUIT. Ils sont déjà dans le buffer cache : c\'est une lecture logique, quelques microsecondes, sans aucun accès disque.', n: ['server', 'buffercache'], f: [['p-lread', 'read']] },
      { t: 'Résultat renvoyé au client', x: 'Les lignes sont assemblées dans la PGA (sort area si ORDER BY) puis renvoyées à l\'application, par paquets (fetch size).', n: ['server', 'app'], f: [['p-conn', 'sql', true]] }
    ]},
    { id: 'miss', label: 'Cache miss (lecture disque)', steps: [
      { t: 'Même début : parse puis recherche', x: 'La requête arrive, le plan est trouvé ou construit, puis le serveur cherche les blocs nécessaires dans le buffer cache.', n: ['server', 'sharedpool', 'buffercache'], f: [['p-conn', 'sql'], ['p-parse', 'parse']] },
      { t: 'Le bloc n\'est pas en cache', x: 'Le bloc demandé n\'est pas présent : lecture physique. C\'est l\'opération la plus lente du trajet, d\'où l\'intérêt d\'un buffer cache bien dimensionné.', n: ['buffercache'], f: [['p-lread', 'read']] },
      { t: 'Lecture depuis le datafile', x: 'Le processus serveur lit le bloc dans le datafile du tablespace concerné et le place dans un buffer libre du cache (physical read).', n: ['datafiles', 'buffercache'], f: [['p-read', 'read']] },
      { t: 'Le bloc est en cache, résultat renvoyé', x: 'Le prochain accès à ce bloc sera une lecture logique. Le résultat est renvoyé à l\'application.', n: ['buffercache', 'server', 'app'], f: [['p-conn', 'sql', true]] }
    ]},
    { id: 'insert', label: 'INSERT + COMMIT', steps: [
      { t: 'L\'INSERT arrive', x: 'Spring Boot envoie INSERT INTO commande (…) VALUES (…) dans une transaction. Elle passe par le listener puis le processus serveur.', n: ['app', 'listener', 'server'], f: [['p-conn', 'sql']] },
      { t: 'Parse', x: 'Même vérification dans la shared pool : l\'ordre existe-t-il déjà ? Les variables bind permettent de réutiliser le plan pour chaque nouvelle commande.', n: ['server', 'sharedpool'], f: [['p-parse', 'parse']] },
      { t: 'Le bloc est modifié en mémoire', x: 'Oracle charge le bloc dans le buffer cache et y ajoute la ligne. Le bloc devient « dirty » : il diffère du disque. Rien n\'est encore écrit dans le datafile, et c\'est voulu.', n: ['buffercache'], f: [['p-lread', 'read']], fx: 'dirty' },
      { t: 'Le changement est décrit dans le redo', x: 'Avant la modification, le serveur écrit dans le redo log buffer un enregistrement qui décrit le changement (et un undo pour pouvoir l\'annuler). C\'est le write-ahead logging.', n: ['redobuf'], f: [['p-redo', 'redo']], fx: 'redoup' },
      { t: 'COMMIT : LGWR écrit sur disque', x: 'Le COMMIT réveille LGWR : il vide le redo buffer dans le groupe de redo log CURRENT et attend l\'écriture disque. Dès qu\'elle est confirmée, la transaction est durable, même si le bloc dirty n\'est pas encore dans le datafile.', n: ['bg-LGWR', 'redologs', 'redobuf'], f: [['p-lgwr', 'redo'], ['p-lgwr2', 'redo']], fx: 'flush' },
      { t: 'Acquittement à l\'application', x: 'Le serveur répond « commit complete ». L\'utilisateur voit sa commande enregistrée.', n: ['server', 'app'], f: [['p-conn', 'sql', true]] },
      { t: 'Plus tard : DBWn écrit le bloc', x: 'En arrière-plan, DBWn écrit les blocs dirty vers les datafiles (checkpoint, manque de place…). Aucune urgence : le redo garantit qu\'on peut les reconstruire.', n: ['bg-DBW', 'datafiles'], f: [['p-dbwr', 'write'], ['p-write', 'write']], fx: 'clean' }
    ]},
    { id: 'crash', label: 'Panne et récupération', steps: [
      { t: 'Coupure brutale', x: 'Le serveur s\'arrête : la SGA disparaît, avec des blocs dirty jamais écrits dans les datafiles. Les datafiles sont en retard, mais les redo logs contiennent tout ce qui a été commité.', n: ['sga'], f: [], fx: 'crash' },
      { t: 'Redémarrage : SMON prend la main', x: 'Au démarrage, la SGA est recréée et SMON lance l\'instance recovery, automatiquement, sans intervention du DBA.', n: ['bg-SMON', 'instance'], f: [], fx: 'revive' },
      { t: 'Roll forward : rejouer le redo', x: 'Les changements enregistrés dans les redo logs depuis le dernier checkpoint sont rejoués sur les datafiles. On retrouve toutes les transactions commitées.', n: ['redologs', 'datafiles'], f: [['p-lgwr2', 'redo', true], ['p-write', 'write']] },
      { t: 'Rollback : annuler l\'inachevé', x: 'Les transactions qui n\'avaient pas commité sont annulées grâce aux données d\'undo. La base est de nouveau cohérente : rien commité n\'est perdu, rien d\'incomplet n\'est visible.', n: ['datafiles', 'bg-SMON'], f: [] }
    ]}
  ];

  /* ------------------------------------------------------- Mock data */
  var mock = { c: { 'user calls': 5200, 'user commits': 310, 'redo size': 8.2e6, 'redo writes': 1400, 'physical reads': 2100, 'physical writes': 900,
                    'session logical reads': 1.4e6, 'parse count (total)': 8000, 'parse count (hard)': 420, 'execute count': 9000, 'db block changes': 52000 },
               seq: 42, cur: 1, t0: Date.now() - 3 * 3600e3, tick: 0 };
  function mockSnapshot() {
    var burst = Math.random() < 0.3 ? rnd(3, 6) : 1, c = mock.c;
    c['user calls'] += rnd(4, 30) * burst; c['user commits'] += rnd(0, 6) * burst; c['redo size'] += rnd(2e3, 60e3) * burst;
    c['redo writes'] += rnd(0, 6) * burst; c['physical reads'] += rnd(0, 25) * burst; c['physical writes'] += rnd(0, 18) * burst;
    c['session logical reads'] += rnd(300, 4000) * burst; c['parse count (total)'] += rnd(4, 30) * burst; c['parse count (hard)'] += Math.random() < .3 ? rnd(1, 4) : 0;
    c['execute count'] += rnd(5, 40) * burst; c['db block changes'] += rnd(20, 400) * burst;
    if (++mock.tick % 14 === 0) { mock.cur = mock.cur % 3 + 1; mock.seq++; }
    var stat = {}; Object.keys(c).forEach(function (k) { stat[k] = Math.round(c[k]); });
    return {
      timestamp: Date.now(), simulated: true,
      instance: { instanceName: 'GLSID', dbName: 'GLSID', hostName: 'localhost', version: '19.0.0.0.0', status: 'OPEN', logMode: 'NOARCHIVELOG', openMode: 'READ WRITE', uptimeSeconds: Math.round((Date.now() - mock.t0) / 1000) },
      sga: { 'Fixed Size': 9.1e6, 'Variable Size': 4.2e8, 'Database Buffers': 1.6e9, 'Redo Buffers': 7.9e6 },
      sgaComponents: [{ name: 'DEFAULT buffer cache', bytes: 1.6e9 }, { name: 'shared pool', bytes: 3.5e8 }, { name: 'large pool', bytes: 3.3e7 }],
      pga: { 'aggregate PGA target parameter': 5.4e8, 'total PGA allocated': 1.9e8 + rnd(0, 1.2e7), 'total PGA inuse': 1.4e8, 'maximum PGA allocated': 2.6e8 },
      stats: stat,
      sessions: { total: 38 + Math.round(rnd(0, 3)), active: 1 + Math.round(rnd(0, 3)), processes: 71 },
      backgroundProcesses: ['DBW0', 'LGWR', 'CKPT', 'SMON', 'PMON', 'MMON', 'MMNL', 'RECO'].map(function (n) { return { name: n, description: '' }; }),
      redoLogs: [1, 2, 3].map(function (g) { return { group: g, status: g === mock.cur ? 'CURRENT' : (g === (mock.cur + 1) % 3 + 0 || false ? 'ACTIVE' : 'INACTIVE'), bytes: 2e8, members: 1, sequence: mock.seq - ((mock.cur - g + 3) % 3), archived: 'NO' }; }),
      tablespaces: [{ name: 'SYSTEM', bytes: 9.4e8, usedBytes: 9.1e8 }, { name: 'SYSAUX', bytes: 6.3e8, usedBytes: 5.6e8 }, { name: 'UNDOTBS1', bytes: 4.0e8, usedBytes: 6.5e7 }, { name: 'USERS', bytes: 1.0e8, usedBytes: 3.4e7 }],
      parameters: { sga_target: '1932735283', pga_aggregate_target: '536870912', memory_target: '0', db_block_size: '8192', processes: '300' },
      topSql: [
        { sqlId: '8fz2k1q0a7m3c', text: 'select p1_0.id,p1_0.actif,p1_0.categorie_id,p1_0.description,p1_0.nom,p1_0.prix,p1_0.quantite_stock from produit p1_0', executions: 214, bufferGets: 1284, diskReads: 3, elapsedMs: 96 },
        { sqlId: '3tq8w9d5n2v1b', text: 'insert into commande (client_id,date_commande,montant_total,numero,statut,id) values (:1,:2,:3,:4,:5,:6)', executions: 41, bufferGets: 305, diskReads: 0, elapsedMs: 58 },
        { sqlId: 'b6c4m7x0e9p2r', text: 'select c1_0.id,c1_0.adresse,c1_0.date_creation,c1_0.email,c1_0.nom,c1_0.prenom from client c1_0', executions: 87, bufferGets: 512, diskReads: 1, elapsedMs: 40 },
        { sqlId: '1k5h3j8s6d4fa', text: 'update produit set quantite_stock=:1 where id=:2', executions: 63, bufferGets: 190, diskReads: 0, elapsedMs: 22 }
      ]
    };
  }

  /* ------------------------------------------------------- State */
  var STATE = { ambient: false };   // background particles (from V$ counters): OFF by default
  var snapPrev = null, snapCur = null, paused = false, mode = 'connecting', rates = {}, lastRedoCur = null;
  var flows = {};     // ambient particle emitters
  var parts = [];     // live particles
  var pathCache = {};
  var layer = $('#particles'), svg = $('#diagram');

  /* ------------------------------------------------------- Diagram: generated parts */
  var BG = [
    ['DBW', 'DBWn', 'Database Writer', 'write'], ['LGWR', 'LGWR', 'Log Writer', 'redo'], ['CKPT', 'CKPT', 'Checkpoint'],
    ['SMON', 'SMON', 'System Monitor'], ['PMON', 'PMON', 'Process Monitor'], ['ARC', 'ARCn', 'Archiver'],
    ['MMON', 'MMON', 'Manageability Monitor'], ['RECO', 'RECO', 'Recoverer']
  ];
  var chips = {};
  var chipLayer = $('#bg-chips');
  BG.forEach(function (b, i) {
    var y = 90 + i * 44, g = el('g', { class: 'chip node', 'data-node': 'bg-' + b[0], 'data-kind': b[3] || '' }, chipLayer);
    el('rect', { x: 925, y: y, width: 230, height: 34, rx: 6 }, g);
    el('circle', { cx: 942, cy: y + 17, r: 4 }, g);
    el('text', { class: 'n', x: 957, y: y + 21 }, g, b[1]);
    el('text', { class: 'd', x: 1010, y: y + 21 }, g, b[2]);
    chips[b[0]] = g;
  });

  var blocks = [], blockLayer = $('#blocks');
  for (var r = 0; r < 4; r++) for (var q = 0; q < 10; q++) {
    var rect = el('rect', { x: 492 + q * 24, y: 132 + r * 26, width: 18, height: 18, rx: 3 }, blockLayer);
    blocks.push({ el: rect, s: Math.random() < 0.68 ? 'clean' : 'free' });
  }
  function paintBlock(b) { b.el.setAttribute('class', b.s === 'free' ? '' : b.s); }
  blocks.forEach(paintBlock);
  function pickBlock(state) {
    var c = blocks.filter(function (b) { return b.s === state; });
    return c.length ? c[Math.floor(Math.random() * c.length)] : null;
  }
  function setBlock(b, s, fresh) {
    if (!b) return; b.s = s; paintBlock(b);
    if (fresh) { b.el.classList.add('fresh'); setTimeout(function () { paintBlock(b); }, 500); }
  }
  var dirtyTarget = 2;
  function tickBlocks() {
    if (!STATE.ambient) return;
    var dirty = blocks.filter(function (b) { return b.s === 'dirty'; }).length;
    if (dirty < dirtyTarget) setBlock(pickBlock('clean') || pickBlock('free'), 'dirty');
    else if (dirty > dirtyTarget) setBlock(pickBlock('dirty'), 'clean');
    else if (Math.random() < 0.3) { var b = pickBlock('clean'); if (b) setBlock(b, 'free'); }
  }

  /* ------------------------------------------------------- Particles */
  function pathInfo(id) {
    if (!pathCache[id]) { var p = document.getElementById(id); pathCache[id] = { p: p, len: p.getTotalLength() }; }
    return pathCache[id];
  }
  function spawn(id, color, opt) {
    if (reduced || parts.length > 150) { if (opt && opt.done) opt.done(); return; }
    opt = opt || {};
    var pi = pathInfo(id), c = el('circle', { r: opt.big ? 5.5 : 2.8, fill: color }, layer);
    if (opt.big) c.setAttribute('filter', 'url(#glow)');
    parts.push({ el: c, pi: pi, t: 0, dur: opt.dur || 1.7, rev: !!opt.rev, done: opt.done });
  }
  function frame(now) {
    var dt = Math.min(0.05, (now - (frame.last || now)) / 1000); frame.last = now;
    if (!paused) {
      Object.keys(flows).forEach(function (k) {
        var f = flows[k]; f.acc += f.rate * dt;
        while (f.acc >= 1) { f.acc -= 1; emit(f); }
      });
      redoStep(dt);
    }
    for (var i = parts.length - 1; i >= 0; i--) {
      var p = parts[i]; p.t += dt / p.dur;
      if (p.t >= 1) { p.el.remove(); parts.splice(i, 1); if (p.done) p.done(); continue; }
      var d = (p.rev ? 1 - p.t : p.t) * p.pi.len, pt = p.pi.p.getPointAtLength(d);
      p.el.setAttribute('cx', pt.x); p.el.setAttribute('cy', pt.y);
      p.el.setAttribute('opacity', Math.min(1, p.t * 6, (1 - p.t) * 6));
    }
    requestAnimationFrame(frame);
  }
  function emit(f) {
    var chain = f.paths, k = 0;
    (function go() {
      spawn(chain[k], f.color, { done: function () {
        if (k === 0 && f.onStart) f.onStart();
        if (++k < chain.length) go(); else if (f.onEnd) f.onEnd();
      }});
    })();
    if (f.pulse) pulseChip(f.pulse);
  }
  function pulseChip(key) {
    var c = chips[key]; if (!c) return;
    c.classList.add('busy'); clearTimeout(c._t); c._t = setTimeout(function () { c.classList.remove('busy'); }, 450);
  }
  function setFlow(name, paths, color, rate, extra) {
    var f = flows[name] || (flows[name] = { acc: 0, rate: 0 });
    f.paths = paths; f.color = color; f.raw = rate; f.rate = (reduced || !STATE.ambient) ? 0 : rate;
    Object.keys(extra || {}).forEach(function (k) { f[k] = extra[k]; });
  }
  /* log-ish mapping: 0/s -> idle trickle, 1000/s -> ~7 particles/s */
  function rateOf(perSec, scale) { return Math.min(9, 0.25 + 2.3 * Math.log10(1 + (perSec || 0) / (scale || 1))); }

  /* redo buffer sawtooth */
  var redoLevel = 0.1, redoFlushing = 0, redoHold = 0;   // redoHold: level kept while an editor transaction is uncommitted
  function redoStep(dt) {
    var fill = $('#redo-fill'); if (!fill) return;
    var kbs = STATE.ambient ? (rates['redo size'] || 0) / 1024 : 0;
    redoLevel += dt * (STATE.ambient ? 0.03 + Math.min(0.5, kbs / 120) : -0.05);
    redoLevel = Math.max(0.04, redoLevel);
    if (redoHold > 0 && redoFlushing <= 0) redoLevel = Math.max(redoLevel, redoHold);
    if (redoLevel >= 0.9 || (redoFlushing > 0)) {
      if (redoFlushing <= 0) { redoFlushing = 0.6; redoHold = 0; flushRedoVisual(); }
      redoFlushing -= dt; redoLevel = Math.max(0.06, redoLevel - dt * 1.6);
    }
    var h = 96 * Math.min(1, redoLevel);
    fill.setAttribute('height', h.toFixed(1)); fill.setAttribute('y', (228 - h).toFixed(1));
  }
  function flushRedoVisual() { pulseChip('LGWR'); if (STATE.ambient && flows.lgwr) emit(flows.lgwr); }

  /* ------------------------------------------------------- Snapshot -> UI */
  function setText(id, v) { var n = document.getElementById(id); if (n) n.textContent = v; }

  function computeRates(prev, cur) {
    rates = {};
    if (!prev) return;
    var dt = (cur.timestamp - prev.timestamp) / 1000; if (dt <= 0) return;
    Object.keys(cur.stats || {}).forEach(function (k) {
      var d = cur.stats[k] - (prev.stats[k] || 0); rates[k] = d > 0 ? d / dt : 0;
    });
  }

  function applySnapshot(s) {
    snapPrev = snapCur; snapCur = s; computeRates(snapPrev, s);
    var i = s.instance || {}, sga = s.sga || {}, pga = s.pga || {}, st = s.stats || {}, se = s.sessions || {};
    var sgaTotal = Object.keys(sga).reduce(function (a, k) { return a + sga[k]; }, 0);
    var pgaAlloc = pga['total PGA allocated'], pgaTarget = pga['aggregate PGA target parameter'];
    var hit = st['session logical reads'] ? Math.max(0, (1 - (st['physical reads'] || 0) / st['session logical reads']) * 100) : null;

    // KPIs
    setText('k-instance', i.instanceName || '—'); setText('k-version', i.version ? 'Oracle ' + i.version : '—');
    setText('k-uptime', i.uptimeSeconds != null ? fmtUptime(i.uptimeSeconds) : '—'); setText('k-status', [i.status, i.openMode].filter(Boolean).join(' · ') || '—');
    setText('k-sessions', se.total != null ? se.total : '—'); setText('k-active', se.active != null ? se.active + ' actives · ' + (se.processes || 0) + ' proc.' : '—');
    setText('k-sga', sgaTotal ? fmtBytes(sgaTotal) : '—');
    setText('k-sga-t', (s.parameters && s.parameters.sga_target && s.parameters.sga_target !== '0') ? 'cible ' + fmtBytes(+s.parameters.sga_target) : 'dimension totale');
    setText('k-pga', pgaAlloc ? fmtBytes(pgaAlloc) : '—'); setText('k-pga-t', pgaTarget ? 'cible ' + fmtBytes(pgaTarget) : 'mémoire privée');
    setText('k-hit', hit != null ? hit.toFixed(1).replace('.', ',') + ' %' : '—');

    // Diagram text
    setText('inst-label', 'INSTANCE ORACLE · ' + (i.instanceName || ''));
    setText('app-sessions', se.total != null ? se.total + ' session(s)' : '— sessions');
    var comps = s.sgaComponents || [], pick = function (re) { var c = comps.filter(function (c) { return re.test(c.name); })[0]; return c ? fmtBytes(c.bytes) : null; };
    setText('sga-meta', fmtBytes(sgaTotal) + (pick(/large/i) ? ' · large ' + pick(/large/i) : ''));
    setText('hit-text', 'hit ratio ' + (hit != null ? hit.toFixed(1).replace('.', ',') + ' %' : '—') + (pick(/buffer cache/i) ? ' · ' + pick(/buffer cache/i) : ''));
    setText('lib-stat', st['parse count (total)'] ? fmtNum(st['parse count (hard)'] || 0) + ' hard / ' + fmtNum(st['parse count (total)']) + ' parses' : 'plans SQL / PL-SQL');
    setText('redo-size', fmtBytes(sga['Redo Buffers']));
    var pfill = $('#pga-fill'), den = Math.max(pgaTarget || 0, pga['maximum PGA allocated'] || 0, pgaAlloc || 0);
    if (pfill && den) pfill.setAttribute('width', Math.max(4, 350 * (pgaAlloc || 0) / den).toFixed(1));
    setText('pga-text', pgaAlloc ? 'PGA ' + fmtBytes(pgaAlloc) + (pgaTarget ? ' / ' + fmtBytes(pgaTarget) : '') : '—');

    // Background processes
    var names = (s.backgroundProcesses || []).map(function (b) { return b.name; });
    BG.forEach(function (b) {
      var up = names.some(function (n) { return n.indexOf(b[0]) === 0; });
      chips[b[0]].classList.toggle('up', up); chips[b[0]].classList.toggle('off', !up && names.length > 0);
    });

    // Redo log groups
    var rg = $('#redo-groups'); rg.textContent = '';
    (s.redoLogs || []).slice(0, 3).forEach(function (g, idx) {
      var x = 915 + idx * 92, grp = el('g', { class: 'rg ' + (g.status === 'CURRENT' ? 'current' : g.status === 'ACTIVE' ? 'active' : '') }, rg);
      el('rect', { x: x, y: 572, width: 82, height: 112, rx: 6 }, grp);
      el('text', { x: x + 10, y: 592 }, grp, 'GROUP ' + g.group);
      el('text', { class: 'big', x: x + 10, y: 618 }, grp, g.status === 'CURRENT' ? 'CURRENT' : g.status === 'ACTIVE' ? 'ACTIVE' : 'INACTIVE');
      el('text', { x: x + 10, y: 640 }, grp, 'seq ' + g.sequence);
      el('text', { x: x + 10, y: 658 }, grp, fmtBytes(g.bytes));
      el('text', { x: x + 10, y: 674 }, grp, g.archived === 'YES' ? 'archivé' : '');
    });
    var cur = (s.redoLogs || []).filter(function (g) { return g.status === 'CURRENT'; })[0];
    if (cur && lastRedoCur != null && cur.group !== lastRedoCur) {
      $$('.rg', rg).forEach(function (n) { n.classList.add('flash'); setTimeout(function () { n.classList.remove('flash'); }, 900); });
      flushRedoVisual();
    }
    lastRedoCur = cur ? cur.group : lastRedoCur;
    setText('log-mode', (i.logMode || '—') + (cur ? ' · groupe ' + cur.group + ' courant' : ''));
    setText('arch-mode', i.logMode || '—');
    setText('arch-sub', i.logMode === 'ARCHIVELOG' ? 'redo logs pleins copiés' : 'redo logs simplement écrasés');
    chips.ARC.dataset.kind = '';
    $('[data-node=archive]').classList.toggle('dim', i.logMode === 'NOARCHIVELOG');

    // Tablespaces
    var ts = $('#ts-rows'); ts.textContent = '';
    (s.tablespaces || []).slice(0, 5).forEach(function (t, idx) {
      var y = 574 + idx * 29, pct = t.bytes ? t.usedBytes / t.bytes : 0;
      el('text', { class: 'ts-name', x: 495, y: y + 9 }, ts, t.name);
      el('rect', { class: 'ts-bar', x: 600, y: y, width: 150, height: 10, rx: 5 }, ts);
      el('rect', { class: 'ts-fill' + (pct > .9 ? ' bad' : pct > .75 ? ' warn' : ''), x: 600, y: y, width: Math.max(3, 150 * pct), height: 10, rx: 5 }, ts);
      el('text', { class: 'ts-size', x: 868, y: y + 9, 'text-anchor': 'end' }, ts, fmtBytes(t.usedBytes) + ' / ' + fmtBytes(t.bytes));
    });

    // Flows driven by real deltas
    var rr = rates;
    setFlow('conn', ['p-conn'], C.sql, rateOf(rr['user calls'], 1));
    setFlow('parse', ['p-parse'], C.parse, rateOf(rr['parse count (total)'], 1));
    setFlow('lread', ['p-lread'], C.read, rateOf(rr['session logical reads'], 40));
    setFlow('read', ['p-read'], C.read, rateOf(rr['physical reads'], 1), { onEnd: function () { setBlock(pickBlock('free') || pickBlock('clean'), 'clean', true); } });
    setFlow('write', ['p-dbwr', 'p-write'], C.write, rateOf(rr['physical writes'], 1), { pulse: 'DBW' });
    setFlow('redo', ['p-redo'], C.redo, rateOf((rr['redo size'] || 0) / 1024, 2));
    setFlow('lgwr', ['p-lgwr', 'p-lgwr2'], C.redo, 0, { pulse: 'LGWR' });
    dirtyTarget = Math.max(1, Math.min(12, Math.round(1 + 1.4 * Math.log2(1 + (rr['db block changes'] || 0) / 20))));

    renderPanels(s);
  }

  function renderPanels(s) {
    var tb = $('#sql-body');
    if (!(s.topSql || []).length) tb.innerHTML = '<tr><td colspan="5" class="muted">Aucune requête récente. Naviguez dans l\'application puis revenez.</td></tr>';
    else {
      tb.textContent = '';
      s.topSql.forEach(function (q) {
        var tr = document.createElement('tr');
        var td = document.createElement('td'); td.innerHTML = '<div class="sql"></div><div class="sid mono"></div>';
        td.firstChild.textContent = q.text; td.lastChild.textContent = q.sqlId; tr.appendChild(td);
        [q.executions, q.bufferGets, q.diskReads, q.elapsedMs].forEach(function (v) {
          var c = document.createElement('td'); c.className = 'num mono'; c.textContent = fmtNum(v); tr.appendChild(c);
        });
        tb.appendChild(tr);
      });
    }
    var defs = [['session logical reads', 'Lectures logiques', 5000], ['physical reads', 'Lectures physiques', 100], ['physical writes', 'Écritures physiques', 100],
                ['redo size', 'Redo généré (Ko)', 500], ['parse count (total)', 'Parses', 100], ['parse count (hard)', 'dont hard parses', 20],
                ['user commits', 'Commits', 20], ['execute count', 'Exécutions', 200]];
    var rb = $('#rates-body'); rb.textContent = '';
    defs.forEach(function (d) {
      var v = rates[d[0]] || 0; if (d[0] === 'redo size') v /= 1024;
      var tr = document.createElement('tr'); tr.className = 'rate-row';
      tr.innerHTML = '<td></td><td class="num mono"><span class="bar"><i></i></span><b></b></td>';
      tr.firstChild.textContent = d[1];
      tr.querySelector('i').style.width = Math.min(100, 100 * Math.log10(1 + v) / Math.log10(1 + d[2] * 10)) + '%';
      tr.querySelector('b').textContent = v < 10 ? v.toFixed(1).replace('.', ',') : fmtNum(v);
      rb.appendChild(tr);
    });
    var pb = $('#params-body'); pb.textContent = '';
    var labels = { sga_target: 'SGA_TARGET', sga_max_size: 'SGA_MAX_SIZE', pga_aggregate_target: 'PGA_AGGREGATE_TARGET', memory_target: 'MEMORY_TARGET', db_block_size: 'DB_BLOCK_SIZE', processes: 'PROCESSES' };
    Object.keys(s.parameters || {}).forEach(function (k) {
      var v = s.parameters[k], big = /target|size/.test(k) && k !== 'db_block_size' && +v > 0;
      var tr = document.createElement('tr'); tr.innerHTML = '<td class="mono"></td><td class="num mono"></td>';
      tr.firstChild.textContent = labels[k] || k; tr.lastChild.textContent = big ? fmtBytes(+v) : (v === '0' ? 'automatique' : v);
      pb.appendChild(tr);
    });
  }

  /* ------------------------------------------------------- Polling */
  function setMode(m) {
    mode = m; var chip = $('#mode-chip');
    chip.dataset.mode = m; setText('mode-text', m === 'live' ? 'EN DIRECT · Oracle' : m === 'sim' ? 'SIMULATION' : 'Connexion…');
    $('#sim-notice').hidden = m !== 'sim';
  }
  function isUsable(s) { return s && s.instance && s.sga && Object.keys(s.sga).length; }

  function poll() {
    if (paused) return schedule();
    var ctl = ('AbortController' in window) ? new AbortController() : null;
    var to = setTimeout(function () { if (ctl) ctl.abort(); }, 4000);
    fetch(API, { headers: { Accept: 'application/json' }, signal: ctl ? ctl.signal : undefined })
      .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
      .then(function (s) {
        if (!isUsable(s)) throw new Error('empty');
        setMode('live'); applySnapshot(s);
      })
      .catch(function () {
        if (mode !== 'sim') { snapCur = null; snapPrev = null; }
        setMode('sim'); applySnapshot(mockSnapshot());
      })
      .then(function () { clearTimeout(to); schedule(); });
  }
  function schedule() { setTimeout(poll, POLL_MS); }

  /* ------------------------------------------------------- Interaction: explain + scenarios */
  var selected = null, sc = null, stepIdx = -1, timer = null, playing = false;

  function clearHot() { $$('#diagram .hot').forEach(function (n) { n.classList.remove('hot'); }); }
  function nodeEl(key) { return $('#diagram [data-node="' + key + '"]'); }
  function showCaption(no, title, text) { setText('cap-no', no); setText('cap-title', title); setText('cap-text', text); }

  svg.addEventListener('click', function (e) {
    var n = e.target.closest('.node'); if (!n) return;
    var key = n.getAttribute('data-node'), info = EXPLAIN[key]; if (!info) return;
    if (selected) selected.classList.remove('selected');
    selected = n; n.classList.add('selected');
    stopScenario(true);
    showCaption('Composant', info[0], info[1]);
  });

  var scBox = $('#scenarios'), dots = $('#dots');
  SCENARIOS.forEach(function (s) {
    var b = document.createElement('button'); b.type = 'button'; b.textContent = s.label; b.setAttribute('role', 'tab'); b.setAttribute('aria-selected', 'false');
    b.addEventListener('click', function () { startScenario(s, b); }); scBox.appendChild(b);
  });

  function startScenario(s, btn) {
    stopScenario(true); sc = s; stepIdx = -1;
    $$('button', scBox).forEach(function (b) { b.setAttribute('aria-selected', b === btn ? 'true' : 'false'); });
    if (selected) selected.classList.remove('selected');
    dots.textContent = ''; s.steps.forEach(function (_, i) { var d = document.createElement('i'); d.addEventListener('click', function () { goStep(i); }); dots.appendChild(d); });
    $('#prev').disabled = $('#next').disabled = $('#play').disabled = false;
    playing = !reduced; goStep(0);
  }
  function stopScenario(keepUi) {
    clearTimeout(timer); playing = false; clearHot(); undoFx();
    if (!keepUi) { sc = null; }
    $('#play').textContent = 'Lecture';
  }
  function goStep(i) {
    if (!sc) return; clearTimeout(timer); clearHot();
    stepIdx = Math.max(0, Math.min(sc.steps.length - 1, i));
    var st = sc.steps[stepIdx];
    showCaption('Étape ' + (stepIdx + 1) + ' / ' + sc.steps.length + ' · ' + sc.label, st.t, st.x);
    st.n.forEach(function (k) { var n = nodeEl(k); if (n) n.classList.add('hot'); });
    st.f.forEach(function (f, j) { setTimeout(function () { spawn(f[0], C[f[1]], { big: true, rev: !!f[2], dur: 1.5 }); }, j * 450); });
    applyFx(st.fx);
    $$('i', dots).forEach(function (d, k) { d.className = k === stepIdx ? 'on' : k < stepIdx ? 'done' : ''; });
    $('#prev').disabled = stepIdx === 0; $('#next').disabled = stepIdx === sc.steps.length - 1;
    $('#play').textContent = playing ? 'Pause' : 'Lecture';
    if (playing) {
      timer = setTimeout(function () {
        if (stepIdx < sc.steps.length - 1) goStep(stepIdx + 1); else { playing = false; $('#play').textContent = 'Rejouer'; }
      }, 5200);
    }
  }
  $('#next').addEventListener('click', function () { playing = false; goStep(stepIdx + 1); });
  $('#prev').addEventListener('click', function () { playing = false; goStep(stepIdx - 1); });
  $('#play').addEventListener('click', function () {
    if (!sc) return;
    if (playing) { playing = false; clearTimeout(timer); $('#play').textContent = 'Lecture'; }
    else { playing = true; goStep(stepIdx >= sc.steps.length - 1 ? 0 : stepIdx); }
  });

  /* special effects tied to steps */
  function applyFx(fx) {
    if (fx === 'dirty') { for (var k = 0; k < 2; k++) setBlock(pickBlock('clean') || pickBlock('free'), 'dirty'); dirtyTarget = Math.max(dirtyTarget, 3); }
    else if (fx === 'clean') { blocks.filter(function (b) { return b.s === 'dirty'; }).forEach(function (b, j) { setTimeout(function () { setBlock(b, 'clean'); }, 600 + j * 150); }); }
    else if (fx === 'redoup') { redoLevel = Math.min(0.85, redoLevel + 0.35); }
    else if (fx === 'flush') { redoLevel = Math.max(redoLevel, 0.9); }
    else if (fx === 'crash') { ['sharedpool', 'buffercache', 'redobuf', 'server'].forEach(function (k) { nodeEl(k).classList.add('dim'); });
      blocks.forEach(function (b) { b.el.style.opacity = .25; }); }
    else if (fx === 'revive') { undoFx(); }
  }
  function undoFx() {
    $$('#diagram .dim').forEach(function (n) { if (n.getAttribute('data-node') !== 'archive') n.classList.remove('dim'); });
    blocks.forEach(function (b) { b.el.style.opacity = ''; });
    if (snapCur && snapCur.instance) $('[data-node=archive]').classList.toggle('dim', snapCur.instance.logMode === 'NOARCHIVELOG');
  }

  $('#pause-btn').addEventListener('click', function () {
    paused = !paused; this.textContent = paused ? 'Reprendre' : 'Pause';
  });

  /* ------------------------------------------------------- Public hooks (used by oracle-events.js) */
  window.OracleStage = {
    layer: svg, reduced: reduced, pathInfo: pathInfo, nodeEl: nodeEl, clearHot: clearHot, showCaption: showCaption,
    setBlock: setBlock, pickBlock: pickBlock, blocks: blocks, pulseChip: pulseChip,
    bumpRedo: function (d) { redoLevel = Math.min(1, redoLevel + d); },
    flushRedo: function () { redoHold = 0; redoLevel = Math.max(redoLevel, 0.9); },
    setRedoHold: function (lvl) { redoHold = Math.max(0, Math.min(0.85, lvl || 0)); if (redoHold) redoLevel = Math.max(redoLevel, redoHold); },
    dropRedo: function () { redoHold = 0; redoLevel = 0.06; },
    busy: function () { return !!(sc && playing); },     // a guided scenario is playing
    isPaused: function () { return paused; },
    isLive: function () { return mode === 'live'; },
    ambient: function () { return STATE.ambient; },
    setAmbient: function (on) {
      STATE.ambient = !!on;
      Object.keys(flows).forEach(function (k) { flows[k].rate = (reduced || !STATE.ambient) ? 0 : (flows[k].raw || 0); });
    }
  };

  /* ------------------------------------------------------- Go */
  setInterval(function () { if (!paused) tickBlocks(); }, 520);
  requestAnimationFrame(frame);
  poll();
})();
