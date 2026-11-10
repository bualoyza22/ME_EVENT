/* BAKBAN9 admin: confirm before delete, image ordering, live previews. */
(function () {
  'use strict';

  // ask before destructive actions (spec: every delete asks first)
  document.querySelectorAll('form[data-confirm]').forEach(function (form) {
    form.addEventListener('submit', function (e) {
      if (!window.confirm(form.getAttribute('data-confirm'))) { e.preventDefault(); }
    });
  });

  document.querySelectorAll('select[data-autosubmit]').forEach(function (sel) {
    sel.addEventListener('change', function () { if (sel.form) { sel.form.requestSubmit(); } });
  });

  // ----- page sections: show the picked image -----
  document.querySelectorAll('[data-slot-select]').forEach(function (sel) {
    sel.addEventListener('change', function () {
      var box = sel.closest('.slot').querySelector('.preview');
      box.textContent = '';
      var opt = sel.options[sel.selectedIndex];
      var src = opt && opt.getAttribute('data-src');
      if (src) {
        var img = document.createElement('img');
        img.src = src;
        img.alt = '';
        box.appendChild(img);
      }
    });
  });

  // ----- product editor -----
  var list = document.querySelector('[data-img-list]');
  if (list) {
    var empty = document.querySelector('[data-img-empty]');
    var previewImg = document.querySelector('[data-preview-img]');

    var refresh = function () {
      var items = list.querySelectorAll('[data-img-item]');
      items.forEach(function (it, i) {
        var tag = it.querySelector('[data-main-tag]');
        if (tag) { tag.hidden = i !== 0; }
      });
      if (empty) { empty.hidden = items.length > 0; }
      if (previewImg) {
        var first = list.querySelector('[data-img-item] img');
        previewImg.src = first ? first.src : 'data:,';
      }
    };

    list.addEventListener('click', function (e) {
      var btn = e.target.closest('button');
      if (!btn) { return; }
      var item = btn.closest('[data-img-item]');
      if (btn.hasAttribute('data-img-remove')) {
        item.remove();
      } else if (btn.hasAttribute('data-img-move')) {
        var d = parseInt(btn.getAttribute('data-img-move'), 10);
        if (d < 0 && item.previousElementSibling) { list.insertBefore(item, item.previousElementSibling); }
        if (d > 0 && item.nextElementSibling) { list.insertBefore(item.nextElementSibling, item); }
        btn.focus();
      }
      refresh();
    });

    var addBtn = document.querySelector('[data-picker-add]');
    if (addBtn) {
      addBtn.addEventListener('click', function () {
        var existing = {};
        list.querySelectorAll('input[name="imageIds"]').forEach(function (i) { existing[i.value] = true; });
        document.querySelectorAll('[data-picker] input:checked').forEach(function (cb) {
          cb.checked = false;
          if (existing[cb.value]) { return; }
          var name = cb.getAttribute('data-name');
          var item = document.createElement('div');
          item.className = 'img-item';
          item.setAttribute('data-img-item', '');
          var hidden = document.createElement('input');
          hidden.type = 'hidden'; hidden.name = 'imageIds'; hidden.value = cb.value;
          var prev = document.createElement('div');
          prev.className = 'preview';
          var img = document.createElement('img');
          img.src = cb.getAttribute('data-src'); img.alt = '';
          var tag = document.createElement('span');
          tag.className = 'badge solid main-tag'; tag.setAttribute('data-main-tag', ''); tag.textContent = 'รูปหลัก';
          prev.appendChild(img); prev.appendChild(tag);
          var cap = document.createElement('span');
          cap.className = 'caption'; cap.textContent = name;
          var tools = document.createElement('div');
          tools.className = 'tools';
          [['←', 'data-img-move', '-1', 'เลื่อนรูป ' + name + ' ไปก่อน'],
           ['→', 'data-img-move', '1', 'เลื่อนรูป ' + name + ' ไปหลัง'],
           ['ลบ', 'data-img-remove', '', 'เอารูป ' + name + ' ออก']].forEach(function (b) {
            var el = document.createElement('button');
            el.type = 'button'; el.className = 'btn-ghost sm'; el.textContent = b[0];
            el.setAttribute(b[1], b[2]); el.setAttribute('aria-label', b[3]);
            tools.appendChild(el);
          });
          item.appendChild(hidden); item.appendChild(prev); item.appendChild(cap); item.appendChild(tools);
          list.appendChild(item);
        });
        refresh();
      });
    }

    var bind = function (inSel, outSel, fmt) {
      var input = document.querySelector(inSel);
      var out = document.querySelector(outSel);
      if (!input || !out) { return; }
      var update = function () { out.textContent = fmt(input); };
      input.addEventListener('input', update);
      input.addEventListener('change', update);
      update();
    };
    bind('[data-preview-name]', '[data-preview-name-out]', function (i) { return i.value.trim() || '[ชื่อสินค้า]'; });
    bind('[data-preview-brand]', '[data-preview-brand-out]', function (i) { return i.value.trim() || '[แบรนด์]'; });
    bind('[data-preview-price]', '[data-preview-price-out]', function (i) {
      var digits = i.value.replace(/[,\s]/g, '');
      if (digits === '') { return 'สอบถามราคา'; }
      if (!/^\d+$/.test(digits)) { return 'ราคาต้องเป็นตัวเลข'; }
      return Number(digits).toLocaleString('en-US') + ' บาท';
    });
    bind('[data-preview-visible]', '[data-preview-visible-out]', function (i) {
      return i.checked ? 'สินค้านี้จะแสดงในส่วน สินค้าแนะนำ' : 'สินค้านี้ถูกซ่อน จะไม่แสดงบนหน้าเว็บ';
    });
  }

  // ----- Footer live preview -----
  var footerForm = document.querySelector('[data-footer-form]');
  if (footerForm) {
    var update = function () {
      footerForm.querySelectorAll('[data-fp]').forEach(function (input) {
        var out = footerForm.querySelector('[data-fp-out="' + input.getAttribute('data-fp') + '"]');
        if (out) { out.textContent = input.value; }
      });
      var socials = footerForm.querySelector('[data-fs-out]');
      socials.textContent = '';
      footerForm.querySelectorAll('[data-fs]').forEach(function (input) {
        var chip = document.createElement('span');
        chip.className = 'chip' + (/^https:\/\/\S+/.test(input.value.trim()) ? '' : ' missing');
        chip.textContent = input.getAttribute('data-fs');
        socials.appendChild(chip);
      });
    };
    footerForm.addEventListener('input', update);
    update();
  }
})();
