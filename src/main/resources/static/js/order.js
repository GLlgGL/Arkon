// Order page: live price preview and contact details.
// Replaces the AutoPostBack round trips of the original pages; the server
// recalculates everything again when the line is saved.
(function () {
  var T = window.T || function (k) { return k; };
  var form = document.getElementById('line-form');
  if (!form) return;

  var type = form.dataset.type;
  var currency = form.dataset.currency;
  var quoteUrl = form.dataset.quoteUrl;
  var partyUrl = form.dataset.partyUrl;

  var provider = document.getElementById('provider');
  var buyer = document.getElementById('buyer');
  var product = document.getElementById('product');
  var quantity = document.getElementById('quantity');
  var manual = document.getElementById('manualDiscount');
  var manualField = document.getElementById('manual-discount-field');
  var prospect = document.getElementById('prospect');
  // Offers only: tick "New client" to type a name that isn't in the client list.
  var newClient = document.getElementById('new-client');
  var newClientField = document.getElementById('new-client-field');
  var buyerNew = document.getElementById('buyer-new');
  var buyerNewHint = document.getElementById('buyer-new-hint');
  var quoteBox = document.getElementById('quote');
  var scan = document.getElementById('scan');
  var scanHint = document.getElementById('scan-hint');
  var unitPrice = document.getElementById('unitPrice');   // purchases only
  var shelf = document.getElementById('shelfId');          // sales: take from; purchases: put on
  var warnBox = document.getElementById('quote-warn');
  var stockHint = document.getElementById('stock-hint');
  var addBtn = document.getElementById('add-btn');

  var money = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  function setQ(key, text) {
    var el = quoteBox.querySelector('[data-q="' + key + '"]');
    if (el) el.textContent = text;
  }

  // Message under the price row. "block" also disables Add product.
  function setWarning(text, block) {
    warnBox.textContent = text;
    warnBox.classList.toggle('blocking', !!block);
    addBtn.disabled = !!block;
  }

  function clearQuote() {
    ['basePrice', 'unit', 'discountPercent', 'unitPrice', 'valueNoVat', 'vatValue', 'valueWithVat']
      .forEach(function (k) { setQ(k, '–'); });
    setWarning('', false);
    if (stockHint) stockHint.textContent = '';
    quantity.removeAttribute('max');
  }

  function isNewClient() {
    return !!(newClient && newClient.checked);
  }

  // The buyer name that will be saved: the typed name for a new client, else the drop-down.
  function buyerName() {
    if (isNewClient()) return buyerNew.value.trim();
    return buyer ? buyer.value.trim() : '';
  }

  function isKnownClient(name) {
    if (!buyer || !buyer.options) return false;
    return Array.prototype.some.call(buyer.options, function (o) { return o.value !== '' && o.value === name; });
  }

  // Offers: the checkbox swaps the client drop-down for a name box and shows the
  // contact fields and manual discount. Only the active one is submitted
  // (disabled fields are not sent), so the server receives one buyer name.
  function updateBuyerMode() {
    if (!newClient) return;
    var fresh = isNewClient();
    buyer.disabled = fresh;
    buyer.required = !fresh;
    buyerNew.disabled = !fresh;
    buyerNew.required = fresh;
    newClientField.hidden = !fresh;
    if (prospect) prospect.hidden = !fresh;
    if (manualField) manualField.hidden = !fresh;
    if (buyerNewHint) buyerNewHint.hidden = !(fresh && isKnownClient(buyerNew.value.trim()));
  }

  var timer;
  function refreshQuote() {
    clearTimeout(timer);
    timer = setTimeout(loadQuote, 200);
  }

  function loadQuote() {
    if (!product.value) { clearQuote(); return; }
    var params = new URLSearchParams({ type: type, product: product.value, qty: quantity.value || '1' });
    if (buyerName()) params.set('buyer', buyerName());
    if (unitPrice && unitPrice.value !== '') params.set('price', unitPrice.value);
    if (manual && manual.value && manualField && !manualField.hidden) params.set('discount', manual.value);

    fetch(quoteUrl + '?' + params.toString(), { headers: { 'Accept': 'application/json' } })
      .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
      .then(function (q) {
        setQ('basePrice', money.format(q.basePrice) + ' ' + currency);
        setQ('discountPercent', q.discountPercent + '%');
        setQ('unitPrice', money.format(q.unitPrice) + ' ' + currency);
        setQ('unit', q.unit);
        if (unitPrice && unitPrice.value === '') unitPrice.placeholder = String(q.basePrice);
        setQ('valueNoVat', money.format(q.valueNoVat) + ' ' + currency);
        setQ('vatValue', money.format(q.vatValue) + ' ' + currency);
        setQ('valueWithVat', money.format(q.valueWithVat) + ' ' + currency);

        // Remaining stock under the Quantity field (sales and offers)
        if (stockHint && q.available !== null) {
          var left = Math.max(0, q.available);
          stockHint.textContent = left > 0 ? T('inStock', left, q.unit) : T('outOfStockShort');
          stockHint.classList.toggle('out', left === 0);
          quantity.max = Math.max(1, left);
        }

        if (q.available !== null && q.available <= 0) {
          setWarning(T('outOfStock', q.productName), true);
        } else if (q.exceedsStock) {
          setWarning(T('notEnough', q.available, q.unit, q.productName), true);
        } else if (q.overLimit) {
          setWarning(T('belowLimit', q.remainingAfter, q.unit, q.productName, q.quantityLimit), false);
        } else {
          setWarning('', false);
        }
      })
      .catch(function () { clearQuote(); setWarning(T('priceError'), false); });
  }

  function loadParty(select, role, cardId) {
    var card = document.getElementById(cardId);
    if (!card || !select || select.type === 'hidden') return;
    var name = role === 'buyer' ? buyerName() : select.value.trim();
    card.textContent = '';
    if (!name) return;
    var params = new URLSearchParams({ type: type, role: role, name: name });
    fetch(partyUrl + '?' + params.toString(), { headers: { 'Accept': 'application/json' } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (p) {
        if (!p || (role === 'buyer' && !p.registered)) return;
        // [css class, icon, text]; icons come from the sprite in fragments/icons.html
        // Layout (CSS grid): name centred on top; TIN and address on the left,
        // email and phone on the right.
        [['name', 'idcard', p.name], ['p-tin', 'user', p.tin], ['p-addr', 'pin', p.address],
         ['p-mail', 'mail', p.email], ['p-phone', 'phone', p.phone]].forEach(function (row) {
          if (!row[2]) return;
          var d = document.createElement('div');
          if (row[0]) d.className = row[0];
          d.appendChild(icon(row[1]));
          var t = document.createElement('span');
          t.textContent = row[2];
          d.appendChild(t);
          card.appendChild(d);
        });
      });
  }

  function icon(name) {
    var NS = 'http://www.w3.org/2000/svg';
    var svg = document.createElementNS(NS, 'svg');
    svg.setAttribute('class', 'ic');
    svg.setAttribute('aria-hidden', 'true');
    var use = document.createElementNS(NS, 'use');
    use.setAttribute('href', '#i-' + name);
    svg.appendChild(use);
    return svg;
  }

  if (provider) provider.addEventListener('change', function () { loadParty(provider, 'provider', 'provider-card'); });
  if (buyer) {
    buyer.addEventListener('change', function () { loadParty(buyer, 'buyer', 'buyer-card'); refreshQuote(); });
  }
  if (newClient) {
    newClient.addEventListener('change', function () {
      updateBuyerMode();
      if (isNewClient()) buyerNew.focus();
      loadParty(buyer, 'buyer', 'buyer-card');
      refreshQuote();
    });
    buyerNew.addEventListener('input', function () { updateBuyerMode(); refreshQuote(); });
  }
  product.addEventListener('change', function () {
    if (unitPrice) unitPrice.value = '';   // a new product starts from its own buy price
    loadShelves();
    refreshQuote();
  });

  // Sales (shelves switched on): each shelf holding the product with its
  // quantity, plus "Not on a shelf". If the product is in more than one
  // place, a choice is required before adding the line.
  function loadShelves() {
    if (!shelf || shelf.dataset.mode !== 'take') return;
    shelf.innerHTML = '';
    if (!product.value) {
      shelf.add(new Option(T('notOnShelf', '–'), ''));
      return;
    }
    fetch(shelf.dataset.shelvesUrl + '?' + new URLSearchParams({ product: product.value }), { headers: { 'Accept': 'application/json' } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (ps) {
        if (!ps) return;
        var options = [];
        ps.shelves.forEach(function (o) {
          options.push(new Option(T('shelfOption', o.code, o.quantity, ps.unit), o.shelfId));
        });
        if (ps.notOnShelf > 0 || ps.shelves.length === 0) {
          options.push(new Option(T('notOnShelf', Math.max(0, ps.notOnShelf) + ' ' + ps.unit), ''));
        }
        options.forEach(function (o) { shelf.add(o); });
        if (options.length > 1) {
          shelf.insertBefore(new Option(T('chooseShelf'), '__choose'), shelf.firstChild);
          shelf.value = '__choose';
        }
      });
  }
  if (shelf) {
    form.addEventListener('submit', function (e) {
      if (shelf.value === '__choose') {
        e.preventDefault();
        setWarning(T('chooseShelf'), false);
        shelf.focus();
      }
    });
  }
  if (unitPrice) unitPrice.addEventListener('input', refreshQuote);

  // Barcode scanners type the code and press Enter. Find the product with that
  // code (or exact name), select it and jump to the quantity.
  if (scan) {
    scan.addEventListener('keydown', function (e) {
      if (e.key !== 'Enter') return;
      e.preventDefault();                  // don't submit the form
      var code = scan.value.trim().toLowerCase();
      if (!code) return;
      var match = Array.prototype.find.call(product.options, function (o) {
        return (o.dataset.code && o.dataset.code.toLowerCase() === code) || o.value.toLowerCase() === code;
      });
      if (match) {
        product.value = match.value;
        product.dispatchEvent(new Event('change'));
        scanHint.textContent = T('found', match.value);
        scanHint.classList.remove('out');
        scan.value = '';
        quantity.focus();
        quantity.select();
      } else {
        scanHint.textContent = T('noCode', scan.value.trim());
        scanHint.classList.add('out');
        scan.select();
      }
    });
  }
  quantity.addEventListener('input', refreshQuote);
  if (manual) manual.addEventListener('input', refreshQuote);

  // Don't submit while the preview says the quantity is over stock.
  form.addEventListener('submit', function (e) {
    if (addBtn.disabled) e.preventDefault();
  });

  // Initial state (e.g. after a validation error re-shows the form)
  updateBuyerMode();
  loadShelves();
  loadParty(provider, 'provider', 'provider-card');
  loadParty(buyer, 'buyer', 'buyer-card');
  refreshQuote();
})();
