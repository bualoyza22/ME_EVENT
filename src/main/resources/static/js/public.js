/* BAKBAN9 public pages: form checks and the Catalog preview. Works without JS for everything except the preview. */
(function () {
  'use strict';

  var EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  // ----- client-side validation (the server validates again) -----
  function validateForm(form) {
    var ok = true;
    form.querySelectorAll('input[required], textarea[required]').forEach(function (el) {
      var v = el.value.trim();
      var bad = v === '' || (el.type === 'email' && !EMAIL.test(v));
      el.setAttribute('aria-invalid', bad ? 'true' : 'false');
      if (bad && ok) { el.focus(); }
      if (bad) { ok = false; }
    });
    form.querySelectorAll('[data-phone]').forEach(function (el) {
      var v = el.value.trim();
      var bad = v !== '' && !/^[0-9+\-\s().]{6,30}$/.test(v);
      el.setAttribute('aria-invalid', bad ? 'true' : 'false');
      if (bad && ok) { el.focus(); }
      if (bad) { ok = false; }
    });
    var msg = form.querySelector('[data-form-error]');
    if (msg) {
      msg.hidden = ok;
      if (!ok) { msg.textContent = form.getAttribute('data-error'); }
    }
    return ok;
  }

  document.querySelectorAll('form[data-validate]').forEach(function (form) {
    form.addEventListener('submit', function (e) {
      if (!validateForm(form)) { e.preventDefault(); }
    });
    form.querySelectorAll('input, textarea').forEach(function (el) {
      el.addEventListener('input', function () {
        if (el.getAttribute('aria-invalid') === 'true') { el.removeAttribute('aria-invalid'); }
      });
    });
  });

  // move focus to the thank-you / result message after a redirect
  var result = document.querySelector('[data-autofocus]');
  if (result) { result.focus({ preventScroll: false }); }

  // ----- Catalog preview -----
  var dialog = document.getElementById('catalog');
  var dataEl = document.getElementById('catalog-data');
  if (!dialog || !dataEl || typeof dialog.showModal !== 'function') { return; }

  var products = [];
  try { products = JSON.parse(dataEl.textContent || '[]'); } catch (e) { products = []; }
  if (!products.length) { return; }

  var q = function (sel) { return dialog.querySelector(sel); };
  var mainImg = q('[data-main-img]');
  var thumbs = q('[data-thumbs]');
  var imgPrev = q('[data-img-prev]');
  var imgNext = q('[data-img-next]');
  var cta = q('[data-cta]');
  var lineUrl = dialog.getAttribute('data-line');
  var contactUrl = dialog.getAttribute('data-contact') || '/contact';
  var idx = 0;
  var imgIdx = 0;
  var opener = null;

  function setImage(p) {
    var img = p.images[imgIdx];
    if (img) {
      mainImg.src = img.src;
      if (img.srcset) { mainImg.srcset = img.srcset; mainImg.sizes = '(max-width: 720px) 90vw, 460px'; }
      else { mainImg.removeAttribute('srcset'); mainImg.removeAttribute('sizes'); }
      mainImg.alt = img.alt;
    } else {
      mainImg.removeAttribute('srcset');
      mainImg.src = 'data:,';
      mainImg.alt = '';
    }
    thumbs.querySelectorAll('button').forEach(function (b, j) {
      b.setAttribute('aria-current', j === imgIdx ? 'true' : 'false');
    });
  }

  function detailRow(dl, label, value) {
    if (!value) { return; }
    var row = document.createElement('div');
    var dt = document.createElement('dt');
    var dd = document.createElement('dd');
    dt.textContent = label;
    dd.textContent = value;
    row.appendChild(dt);
    row.appendChild(dd);
    dl.appendChild(row);
  }

  function render() {
    var p = products[idx];
    var multi = p.images.length > 1;
    q('[data-brand]').textContent = p.brand;
    q('[data-brand]').hidden = !p.brand;
    q('[data-name]').textContent = p.name;
    q('[data-price]').textContent = p.price;
    var desc = q('[data-desc]');
    desc.textContent = p.description;
    desc.hidden = !p.description;

    var dl = q('[data-details]');
    dl.textContent = '';
    detailRow(dl, 'แบรนด์', p.brand);
    detailRow(dl, 'สภาพ', p.condition);
    detailRow(dl, 'ขนาด', p.size);
    detailRow(dl, 'อุปกรณ์', p.accessories);
    dl.hidden = !dl.children.length;

    thumbs.textContent = '';
    thumbs.hidden = !multi;
    if (multi) {
      p.images.forEach(function (img, j) {
        var b = document.createElement('button');
        b.type = 'button';
        b.setAttribute('aria-label', 'ดูรูปที่ ' + (j + 1));
        var t = document.createElement('img');
        t.src = img.src;
        t.alt = '';
        t.loading = 'lazy';
        b.appendChild(t);
        b.addEventListener('click', function () { imgIdx = j; setImage(p); });
        thumbs.appendChild(b);
      });
    }
    imgPrev.hidden = !multi;
    imgNext.hidden = !multi;
    setImage(p);

    cta.href = lineUrl ? lineUrl : contactUrl + '?product=' + encodeURIComponent(p.id);
    if (lineUrl) { cta.target = '_blank'; cta.rel = 'noopener noreferrer'; } else { cta.removeAttribute('target'); }

    var counter = (idx + 1) + ' / ' + products.length;
    dialog.querySelectorAll('[data-counter]').forEach(function (c) { c.textContent = counter; });
    var single = products.length < 2;
    q('[data-prod-prev]').hidden = single;
    q('[data-prod-next]').hidden = single;
  }

  function stepProduct(d) { idx = (idx + d + products.length) % products.length; imgIdx = 0; render(); }
  function stepImage(d) {
    var p = products[idx];
    imgIdx = (imgIdx + d + p.images.length) % p.images.length;
    setImage(p);
  }

  document.querySelectorAll('[data-preview]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var i = parseInt(btn.getAttribute('data-preview'), 10);
      if (isNaN(i) || !products[i]) { return; }
      opener = btn;
      idx = i;
      imgIdx = 0;
      render();
      dialog.showModal();
      q('[data-close]').focus();
    });
  });

  q('[data-close]').addEventListener('click', function () { dialog.close(); });
  q('[data-prod-prev]').addEventListener('click', function () { stepProduct(-1); });
  q('[data-prod-next]').addEventListener('click', function () { stepProduct(1); });
  imgPrev.addEventListener('click', function () { stepImage(-1); });
  imgNext.addEventListener('click', function () { stepImage(1); });

  // click on the backdrop closes
  dialog.addEventListener('click', function (e) { if (e.target === dialog) { dialog.close(); } });
  // Esc is handled by <dialog>; give focus back to the Preview button
  dialog.addEventListener('close', function () { if (opener) { opener.focus(); } });

  // keyboard: arrows step through images/products while the dialog is open
  dialog.addEventListener('keydown', function (e) {
    if (e.target.tagName === 'A') { return; }
    if (e.key === 'ArrowRight') { (products[idx].images.length > 1 ? stepImage : stepProduct)(1); }
    if (e.key === 'ArrowLeft') { (products[idx].images.length > 1 ? stepImage : stepProduct)(-1); }
  });

  // swipe on phones: change image (or product when it has only one image)
  var swipe = q('[data-swipe]');
  var startX = null;
  swipe.addEventListener('touchstart', function (e) { startX = e.touches[0].clientX; }, { passive: true });
  swipe.addEventListener('touchend', function (e) {
    if (startX === null) { return; }
    var dx = e.changedTouches[0].clientX - startX;
    startX = null;
    if (Math.abs(dx) > 40) { (products[idx].images.length > 1 ? stepImage : stepProduct)(dx < 0 ? 1 : -1); }
  });
})();
