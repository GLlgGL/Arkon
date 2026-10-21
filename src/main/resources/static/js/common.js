// Shared page behaviour.
(function () {
  // Ask before destructive actions: <form data-confirm="Delete this?">
  document.addEventListener('submit', function (e) {
    var msg = e.target.getAttribute('data-confirm');
    if (msg && !window.confirm(msg)) e.preventDefault();
  });

  // Whole table rows open their record (the original used double-click).
  document.addEventListener('click', function (e) {
    var row = e.target.closest('tr[data-href]');
    if (!row || e.target.closest('a, button, form, input, select')) return;
    window.location.href = row.getAttribute('data-href');
  });
})();

// Live search: forms marked data-live-search refresh their results while you
// type, without reloading the page. Text fields wait for a short pause;
// dates and drop-downs apply at once. The Search button still submits normally.
// The server renders the same page; only elements marked data-live-region
// (matched by id) are swapped in, so the search box keeps focus and cursor.
(function () {
  document.querySelectorAll('form[data-live-search]').forEach(function (form) {
    var timer;
    var controller;

    function url() {
      var params = new URLSearchParams();
      new FormData(form).forEach(function (value, key) {
        if (String(value).trim() !== '') params.append(key, value);
      });
      var action = form.getAttribute('action') || window.location.pathname;
      var qs = params.toString();
      return action + (qs ? '?' + qs : '');
    }

    function run() {
      if (controller) controller.abort();           // drop a slower, older request
      controller = window.AbortController ? new AbortController() : null;
      var target = url();
      var main = form.closest('main') || document;
      main.setAttribute('aria-busy', 'true');

      fetch(target, { headers: { 'X-Requested-With': 'fetch' }, signal: controller ? controller.signal : undefined })
        .then(function (r) {
          // Session expired: the server sent the login page instead.
          if (r.redirected && /\/login/.test(r.url)) { window.location.href = r.url; return null; }
          return r.ok ? r.text() : null;
        })
        .then(function (html) {
          if (html === null) return;
          var doc = new DOMParser().parseFromString(html, 'text/html');
          main.querySelectorAll('[data-live-region]').forEach(function (el) {
            var fresh = doc.getElementById(el.id);
            if (fresh) el.replaceWith(fresh);
          });
          document.dispatchEvent(new CustomEvent('ims-content-updated'));
          history.replaceState(null, '', target);
        })
        .catch(function (e) { if (e.name !== 'AbortError') form.submit(); })   // fall back to a normal search
        .finally(function () { main.removeAttribute('aria-busy'); });
    }

    form.addEventListener('input', function (e) {
      var t = e.target;
      if (t.matches('input[type=search], input[type=text]')) {
        clearTimeout(timer);
        timer = setTimeout(run, 300);
      }
    });
    form.addEventListener('change', function (e) {
      if (e.target.matches('select, input[type=date]')) {
        clearTimeout(timer);
        run();
      }
    });
  });
})();

// Columns: every grid gets a "Columns" button to choose which columns to show.
// The choice is remembered per page in this browser. Footer cells that span
// several columns shrink with them, so totals stay under the right column.
(function () {
  var T = window.T || function (k) { return k; };

  function headerCells(table) {
    var row = table.tHead && table.tHead.rows[0];
    return row ? Array.prototype.slice.call(row.cells) : [];
  }
  function label(th) {
    var c = th.cloneNode(true);
    c.querySelectorAll('.visually-hidden').forEach(function (n) { n.remove(); });
    return c.textContent.trim();
  }
  function storageKey(table, index) {
    return 'ims-cols:' + window.location.pathname + ':' + index;
  }
  function load(key) {
    try { return JSON.parse(localStorage.getItem(key) || '[]'); } catch (e) { return []; }
  }
  function save(key, hidden) {
    try { localStorage.setItem(key, JSON.stringify(hidden)); } catch (e) {}
  }

  /** Hide the given column indexes in every row, adjusting colspans. */
  function apply(table, hidden) {
    var set = {};
    hidden.forEach(function (i) { set[i] = true; });
    Array.prototype.forEach.call(table.rows, function (row) {
      var col = 0;
      Array.prototype.forEach.call(row.cells, function (cell) {
        if (!cell.dataset.span) cell.dataset.span = cell.colSpan || 1;
        var span = +cell.dataset.span, visible = 0;
        for (var i = col; i < col + span; i++) if (!set[i]) visible++;
        cell.hidden = visible === 0;
        if (span > 1) cell.colSpan = Math.max(1, visible);
        col += span;
      });
    });
  }

  function setup(table, index) {
    var ths = headerCells(table);
    if (ths.length < 3) return;                     // tiny tables don't need it
    var key = storageKey(table, index);
    var hidden = load(key).filter(function (i) { return i < ths.length; });
    apply(table, hidden);

    var wrap = table.closest('.table-wrap');
    if (!wrap || wrap.previousElementSibling && wrap.previousElementSibling.classList.contains('col-tools')) {
      return;                                        // menu already there (live search refresh)
    }
    var tools = document.createElement('div');
    tools.className = 'col-tools no-print';
    var details = document.createElement('details');
    var summary = document.createElement('summary');
    summary.className = 'btn ghost small';
    summary.textContent = T('columns');
    details.appendChild(summary);
    var panel = document.createElement('div');
    panel.className = 'col-panel';
    details.appendChild(panel);
    tools.appendChild(details);
    wrap.parentNode.insertBefore(tools, wrap);

    // Live search replaces the table; always work on the one currently after the menu.
    function currentTable() {
      var n = tools.nextElementSibling;
      return n ? n.querySelector('table.data') : table;
    }

    function build() {
      var t = currentTable();
      var hs = headerCells(t);
      var hid = load(key);
      panel.innerHTML = '';
      hs.forEach(function (th, i) {
        var text = label(th);
        if (!text) return;                           // action columns have no name
        var l = document.createElement('label');
        var cb = document.createElement('input');
        cb.type = 'checkbox';
        cb.checked = hid.indexOf(i) === -1;
        cb.addEventListener('change', function () {
          var h = load(key).filter(function (x) { return x !== i; });
          if (!cb.checked) h.push(i);
          save(key, h);
          apply(currentTable(), h);
        });
        l.appendChild(cb);
        l.appendChild(document.createTextNode(' ' + text));
        panel.appendChild(l);
      });
      var all = document.createElement('button');
      all.type = 'button';
      all.className = 'btn small';
      all.textContent = T('showAll');
      all.addEventListener('click', function () {
        save(key, []);
        apply(currentTable(), []);
        build();
      });
      panel.appendChild(all);
    }
    details.addEventListener('toggle', function () { if (details.open) build(); });
    document.addEventListener('click', function (e) { if (!details.contains(e.target)) details.removeAttribute('open'); });
  }

  function init() {
    document.querySelectorAll('table.data').forEach(function (t, i) { setup(t, i); });
  }
  init();
  document.addEventListener('ims-content-updated', init);
})();

// Payments: choosing Cash selects the default cash account, Bank transfer or
// Card the default bank account. The account can still be changed by hand.
(function () {
  document.querySelectorAll('select.pay-account').forEach(function (acct) {
    var form = acct.closest('form');
    var method = form && form.querySelector('select[name=method]');
    if (!method) return;
    function sync() {
      var kind = method.value === 'CASH' ? 'CASH' : 'BANK';
      var opts = Array.prototype.filter.call(acct.options, function (o) { return o.dataset.kind === kind; });
      var pick = opts.filter(function (o) { return o.dataset.default === 'true'; })[0] || opts[0];
      if (pick) acct.value = pick.value;
    }
    method.addEventListener('change', sync);
    sync();
  });
})();

// Notifications bell: loads alerts for this user when the page opens.
(function () {
  var btn = document.getElementById('bell-btn');
  if (!btn) return;
  var panel = document.getElementById('bell-panel'), list = document.getElementById('bell-list'),
      count = document.getElementById('bell-count'), empty = document.getElementById('bell-empty');
  fetch('/api/notifications', { headers: { 'Accept': 'application/json' } })
    .then(function (r) { return r.ok ? r.json() : []; })
    .then(function (notes) {
      list.innerHTML = '';
      notes.forEach(function (n) {
        var li = document.createElement('li'); li.className = 'bell-' + n.level;
        var a = document.createElement('a'); a.href = n.href; a.textContent = n.text;
        li.appendChild(a); list.appendChild(li);
      });
      empty.hidden = notes.length > 0;
      count.hidden = notes.length === 0;
      count.textContent = notes.length;
      btn.classList.toggle('has-alert', notes.some(function (n) { return n.level === 'alert'; }));
    }).catch(function () {});
  btn.addEventListener('click', function (e) {
    e.stopPropagation();
    panel.hidden = !panel.hidden;
    btn.setAttribute('aria-expanded', String(!panel.hidden));
  });
  document.addEventListener('click', function (e) { if (!panel.hidden && !panel.contains(e.target)) { panel.hidden = true; btn.setAttribute('aria-expanded', 'false'); } });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') { panel.hidden = true; btn.setAttribute('aria-expanded', 'false'); } });
})();
