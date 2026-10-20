// Statistics charts (Chart.js, bundled in /js/vendor). Data comes from the page.
(function () {
  var T = window.T || function (k) { return k; };
  var stats = window.FMS_STATS;
  var currency = window.FMS_CURRENCY || '';
  if (!stats || typeof Chart === 'undefined') return;

  var NAVY = '#0b4779', AMBER = '#ffa100', GREY = '#9aa5b1';
  // Enough distinct colours for a pie of 10 slices plus "Others".
  var PALETTE = ['#0b4779', '#ffa100', '#3ba834', '#d32f2f', '#6f42c1', '#17a2b8',
                 '#e8590c', '#5c7cfa', '#a0522d', '#2f9e44', '#9aa5b1'];

  var money = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  var whole = new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 });
  function fmtMoney(v) { return money.format(v) + ' ' + currency; }

  Chart.defaults.font.family = '"Segoe UI", system-ui, -apple-system, Arial, sans-serif';
  // Text and grid colours follow the light / dark theme.
  var dark = document.documentElement.getAttribute('data-theme') === 'dark';
  Chart.defaults.color = dark ? '#d5dde6' : '#1d2733';
  Chart.defaults.borderColor = dark ? 'rgba(255,255,255,0.10)' : 'rgba(0,0,0,0.10)';
  document.addEventListener('ims-theme-change', function () { window.location.reload(); });
  Chart.defaults.maintainAspectRatio = false;
  Chart.defaults.plugins.legend.position = 'bottom';
  if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    Chart.defaults.animation = false;
  }

  function el(id) { return document.getElementById(id); }
  function hideIfEmpty(id, series) {
    if (!series || !series.labels || series.labels.length === 0) {
      var c = el(id);
      if (c) c.parentElement.style.display = 'none';
      return true;
    }
    return false;
  }

  // Sales, purchases, offers over time (grouped columns)
  var t = stats.trend;
  new Chart(el('chart-trend'), {
    type: 'bar',
    data: {
      labels: t.labels,
      datasets: [
        { label: T('sales'), data: t.sales, backgroundColor: NAVY },
        { label: T('purchases'), data: t.purchases, backgroundColor: AMBER },
        { label: T('offers'), data: t.offers, backgroundColor: GREY }
      ]
    },
    options: {
      scales: { y: { beginAtZero: true, ticks: { callback: function (v) { return whole.format(v); } } } },
      plugins: { tooltip: { callbacks: { label: function (c) { return c.dataset.label + ': ' + fmtMoney(c.parsed.y); } } } }
    }
  });

  // Which client bought more (pie, as in the original)
  if (!hideIfEmpty('chart-clients', stats.topClients)) {
    new Chart(el('chart-clients'), {
      type: 'pie',
      data: { labels: stats.topClients.labels,
              datasets: [{ data: stats.topClients.values, backgroundColor: PALETTE, borderColor: '#fff', borderWidth: 2 }] },
      options: { plugins: { tooltip: { callbacks: { label: pieLabel } } } }
    });
  }

  // Which product was sold more (columns by units, as in the original)
  if (!hideIfEmpty('chart-products-units', stats.topProductsByUnits)) {
    new Chart(el('chart-products-units'), {
      type: 'bar',
      data: { labels: stats.topProductsByUnits.labels,
              datasets: [{ label: T('unitsSold'), data: stats.topProductsByUnits.values, backgroundColor: AMBER }] },
      options: {
        plugins: { legend: { display: false },
                   tooltip: { callbacks: { label: function (c) { return T('units', whole.format(c.parsed.y)); } } } },
        scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
      }
    });
  }

  // Who sold more (was a funnel: bars sorted largest first)
  if (!hideIfEmpty('chart-sellers', stats.sellers)) {
    horizontalBars('chart-sellers', stats.sellers, T('sold'), NAVY);
  }

  // Products that earned the most
  if (!hideIfEmpty('chart-products-revenue', stats.topProductsByRevenue)) {
    horizontalBars('chart-products-revenue', stats.topProductsByRevenue, T('revenue'), '#3ba834');
  }

  // Sales by product class
  if (!hideIfEmpty('chart-classes', stats.byClass)) {
    new Chart(el('chart-classes'), {
      type: 'doughnut',
      data: { labels: stats.byClass.labels,
              datasets: [{ data: stats.byClass.values, backgroundColor: PALETTE, borderColor: '#fff', borderWidth: 2 }] },
      options: { plugins: { tooltip: { callbacks: { label: pieLabel } } } }
    });
  }

  // Returns
  var RED = '#d32f2f';
  if (!hideIfEmpty('chart-ret-products', stats.returnProducts)) {
    horizontalBars('chart-ret-products', stats.returnProducts, T('unitsReturned'), RED);
  }
  if (!hideIfEmpty('chart-ret-clients', stats.returnClients)) {
    horizontalBars('chart-ret-clients', stats.returnClients, T('returnedValue'), '#e07b00');
  }
  if (!hideIfEmpty('chart-ret-merchants', stats.merchantReturns)) {
    horizontalBars('chart-ret-merchants', stats.merchantReturns, T('returnedValue'), NAVY);
  }
  ['chart-ret-reasons', 'chart-ret-mreasons'].forEach(function (id, i) {
    var series = i === 0 ? stats.returnReasons : stats.merchantReasons;
    if (hideIfEmpty(id, series)) return;
    new Chart(el(id), {
      type: 'doughnut',
      data: { labels: series.labels,
              datasets: [{ data: series.values, backgroundColor: PALETTE, borderColor: dark ? '#1b222c' : '#fff', borderWidth: 2 }] },
      options: { plugins: { legend: { position: 'right' } } }
    });
  });

  function horizontalBars(id, series, label, colour) {
    new Chart(el(id), {
      type: 'bar',
      data: { labels: series.labels, datasets: [{ label: label, data: series.values, backgroundColor: colour }] },
      options: {
        indexAxis: 'y',
        plugins: { legend: { display: false },
                   tooltip: { callbacks: { label: function (c) { return fmtMoney(c.parsed.x); } } } },
        scales: { x: { beginAtZero: true, ticks: { callback: function (v) { return whole.format(v); } } } }
      }
    });
  }

  // "Client: 12,000.00 ALL (35.2%)"
  function pieLabel(c) {
    var total = c.dataset.data.reduce(function (a, b) { return a + Number(b); }, 0);
    var pct = total ? (Number(c.parsed) * 100 / total).toFixed(1) : '0.0';
    return c.label + ': ' + fmtMoney(c.parsed) + ' (' + pct + '%)';
  }
})();
