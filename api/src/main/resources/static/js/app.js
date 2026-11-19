(function () {
  'use strict';

  const toggle = document.querySelector('[data-sidebar-toggle]');
  const sidebar = document.getElementById('sidebar');
  const overlay = document.getElementById('sidebar-overlay');

  function abrirMenu() {
    if (!sidebar || !overlay) return;
    sidebar.classList.add('sidebar-open');
    overlay.hidden = false;
    if (toggle) toggle.setAttribute('aria-expanded', 'true');
  }

  function cerrarMenu(devolverFoco) {
    if (!sidebar || !overlay) return;
    sidebar.classList.remove('sidebar-open');
    overlay.hidden = true;
    if (toggle) {
      toggle.setAttribute('aria-expanded', 'false');
      if (devolverFoco) toggle.focus();
    }
  }

  toggle?.addEventListener('click', function () {
    if (sidebar?.classList.contains('sidebar-open')) cerrarMenu(true);
    else abrirMenu();
  });
  overlay?.addEventListener('click', function () { cerrarMenu(true); });
  window.addEventListener('resize', function () {
    if (window.innerWidth > 760) cerrarMenu(false);
  });

  function enfocables(dialogo) {
    return Array.from(dialogo.querySelectorAll('button, input, select, textarea, a[href]'))
      .filter(function (elemento) {
        return !elemento.disabled && elemento.type !== 'hidden'
          && !elemento.closest('fieldset:disabled');
      });
  }

  function registrarDialogo(dialogo) {
    let anterior = null;

    dialogo.addEventListener('keydown', function (evento) {
      if (evento.key === 'Escape') {
        evento.preventDefault();
        dialogo.close();
        return;
      }
      if (evento.key !== 'Tab') return;

      const lista = enfocables(dialogo);
      if (!lista.length) {
        evento.preventDefault();
        return;
      }
      const primero = lista[0];
      const ultimo = lista[lista.length - 1];
      if (evento.shiftKey && (document.activeElement === primero || document.activeElement === dialogo)) {
        evento.preventDefault();
        ultimo.focus();
      } else if (!evento.shiftKey && document.activeElement === ultimo) {
        evento.preventDefault();
        primero.focus();
      }
    });

    dialogo.addEventListener('close', function () {
      if (anterior && typeof anterior.focus === 'function') anterior.focus();
    });

    return function abrir() {
      anterior = document.activeElement;
      if (typeof dialogo.showModal !== 'function') return;
      dialogo.showModal();
      const primero = enfocables(dialogo)[0];
      if (primero) primero.focus();
    };
  }

  const abrirDialogos = new Map();
  document.querySelectorAll('dialog').forEach(function (dialogo) {
    abrirDialogos.set(dialogo.id, registrarDialogo(dialogo));
  });

  document.querySelectorAll('[data-dialog-open]').forEach(function (boton) {
    boton.addEventListener('click', function () {
      abrirDialogos.get(boton.dataset.dialogOpen)?.();
    });
  });

  document.querySelectorAll('[data-dialog-close]').forEach(function (boton) {
    boton.addEventListener('click', function () { boton.closest('dialog')?.close(); });
  });

  document.querySelectorAll('dialog').forEach(function (dialogo) {
    dialogo.addEventListener('click', function (evento) {
      if (evento.target === dialogo) dialogo.close();
    });
  });

  const confirmar = document.getElementById('dialogo-estado');
  if (confirmar) {
    const formulario = confirmar.querySelector('form');
    const mensaje = confirmar.querySelector('[data-confirmar-mensaje]');
    const campoEstado = confirmar.querySelector('[name="activo"]');
    document.querySelectorAll('[data-confirmar-estado]').forEach(function (boton) {
      boton.addEventListener('click', function () {
        formulario.action = boton.dataset.confirmarEstado;
        campoEstado.value = boton.dataset.activo;
        if (mensaje) mensaje.textContent = boton.dataset.mensaje;
        abrirDialogos.get('dialogo-estado')?.();
      });
    });
  }

  document.addEventListener('keydown', function (evento) {
    if (evento.key === 'Escape' && sidebar?.classList.contains('sidebar-open')) cerrarMenu(true);
  });

  const ventaForm = document.querySelector('[data-venta-form]');
  if (ventaForm) {
    const contenedor = document.getElementById('items-container');
    const botonAgregar = document.getElementById('agregar-item');
    const formato = new Intl.NumberFormat('es-CO', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });

    function filtrarOpciones(fila) {
      const tipo = fila.querySelector('.item-tipo')?.value;
      const referencia = fila.querySelector('.item-referencia');
      if (!referencia) return;
      Array.from(referencia.options).forEach(function (opcion) {
        if (!opcion.value) return;
        const visible = opcion.dataset.tipo === tipo;
        opcion.hidden = !visible;
        opcion.disabled = !visible;
        if (!visible && opcion.selected) referencia.value = '';
      });
    }

    function subtotalDe(fila) {
      const referencia = fila.querySelector('.item-referencia');
      const opcion = referencia?.selectedOptions[0];
      const precio = Number.parseFloat(opcion?.dataset.precio || '0');
      const cantidad = Number.parseInt(fila.querySelector('.item-cantidad')?.value || '0', 10);
      if (!Number.isFinite(precio) || !Number.isFinite(cantidad) || cantidad <= 0) return 0;
      return precio * cantidad;
    }

    function actualizarTotales() {
      let total = 0;
      document.querySelectorAll('.item-row').forEach(function (fila) {
        filtrarOpciones(fila);
        const subtotal = subtotalDe(fila);
        total += subtotal;
        const destino = fila.querySelector('[data-subtotal]');
        if (destino) destino.textContent = '$ ' + formato.format(subtotal);
      });
      const destinoTotal = document.querySelector('[data-total-provisional]');
      if (destinoTotal) destinoTotal.textContent = '$ ' + formato.format(total);
    }

    ventaForm.addEventListener('change', function (evento) {
      if (evento.target.matches('.item-tipo')) {
        const referencia = evento.target.closest('.item-row')?.querySelector('.item-referencia');
        if (referencia) referencia.value = '';
      }
      actualizarTotales();
    });

    ventaForm.addEventListener('input', function (evento) {
      if (evento.target.matches('.item-cantidad')) actualizarTotales();
    });

    ventaForm.addEventListener('click', function (evento) {
      const botonEliminar = evento.target.closest('.item-eliminar');
      if (!botonEliminar) return;
      if (document.querySelectorAll('.item-row').length > 1) {
        botonEliminar.closest('.item-row').remove();
        actualizarTotales();
      }
    });

    botonAgregar?.addEventListener('click', function () {
      if (!contenedor) return;
      const filas = contenedor.querySelectorAll('.item-row');
      const plantilla = filas[0];
      if (!plantilla) return;
      const indice = filas.length;
      const copia = plantilla.cloneNode(true);
      copia.innerHTML = copia.innerHTML
        .replace(/\[\d+\]/g, '[' + indice + ']')
        .replace(/items\d+\./g, 'items' + indice + '.');
      const referencia = copia.querySelector('.item-referencia');
      if (referencia) referencia.value = '';
      const cantidad = copia.querySelector('.item-cantidad');
      if (cantidad) cantidad.value = '';
      contenedor.appendChild(copia);
      actualizarTotales();
    });

    actualizarTotales();
  }
})();
