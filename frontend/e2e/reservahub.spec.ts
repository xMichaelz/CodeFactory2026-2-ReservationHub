import { expect, test, type Page } from '@playwright/test';

/**
 * Recorrido completo por la UI: cada paso usa un endpoint del backend (HU-01 .. HU-21).
 * Los datos son únicos por ejecución, así que se puede correr varias veces sobre la misma base.
 */
const run = Date.now().toString(36);
const PASSWORD = 'Secreta123';
const provider = {
  firstName: 'Paula',
  lastName: 'Proveedora',
  email: `prov-${run}@e2e.test`,
  business: `Barbería E2E ${run}`,
};
const client = { firstName: 'Carlos', lastName: 'Cliente', email: `cli-${run}@e2e.test` };
const client2 = { firstName: 'Diana', lastName: 'Directa', email: `cli2-${run}@e2e.test` };
const SERVICE = `Corte ${run}`;
const SERVICE_2 = `Barba ${run}`;
const DAYS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

test.describe.configure({ mode: 'serial' });

/** Etiqueta exacta, admitiendo el asterisco de campo obligatorio ("Nombre *"). */
const field = (label: string) => new RegExp(`^${label}\\s*\\*?$`);

async function expectToast(page: Page, text: string | RegExp) {
  await expect(page.getByTestId('toast-success').filter({ hasText: text }).first()).toBeVisible();
}

async function login(page: Page, email: string, password = PASSWORD) {
  await page.goto('/login');
  await page.getByLabel('Correo electrónico').fill(email);
  await page.getByLabel('Contraseña').fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();
}

async function logout(page: Page) {
  await page.getByRole('button', { name: 'Cerrar sesión' }).click();
  await expect(page).toHaveURL(/\/login$/);
}

async function registerClient(page: Page, c: typeof client) {
  await page.goto('/register');
  await page.getByLabel(field('Nombre')).fill(c.firstName);
  await page.getByLabel('Apellido').fill(c.lastName);
  await page.getByLabel('Correo electrónico').fill(c.email);
  await page.getByLabel('Teléfono').fill('3001234567');
  await page.getByLabel(field('Contraseña')).fill(PASSWORD);
  await page.getByLabel('Confirmar contraseña').fill(PASSWORD);
  await page.getByRole('button', { name: 'Crear cuenta' }).click();
  await expect(page).toHaveURL(/\/client\/catalog$/);
}

test('portada y Swagger accesibles a través del frontend', async ({ page, request }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: /Reserva servicios/ })).toBeVisible();
  const health = await request.get('/actuator/health');
  expect(health.ok()).toBeTruthy();
  expect((await health.json()).status).toBe('UP');
  const docs = await request.get('/v3/api-docs');
  expect(docs.ok()).toBeTruthy();
  const swagger = await request.get('/swagger-ui/index.html');
  expect(swagger.ok()).toBeTruthy();
});

test('HU-02 registro de proveedor + validaciones del formulario', async ({ page }) => {
  await page.goto('/register/provider');
  await page.getByLabel(field('Nombre')).fill(provider.firstName);
  await page.getByLabel('Apellido').fill(provider.lastName);
  await page.getByLabel('Correo electrónico').fill(provider.email);
  await page.getByLabel(field('Contraseña')).fill('corta');
  await page.getByLabel('Confirmar contraseña').fill('corta');
  await page.getByLabel('Nombre del negocio').fill(provider.business);
  await page.getByLabel('Tipo de servicio').fill('Barbería');
  await page.getByLabel('Dirección').fill('Calle 1 # 2-3');
  await page.getByRole('button', { name: 'Registrar negocio' }).click();
  await expect(page.getByTestId('alert-error')).toContainText('al menos 8 caracteres');

  await page.getByLabel(field('Contraseña')).fill(PASSWORD);
  await page.getByLabel('Confirmar contraseña').fill(PASSWORD);
  await page.getByRole('button', { name: 'Registrar negocio' }).click();
  await expect(page).toHaveURL(/\/provider$/);
  await expect(page.getByRole('heading', { name: 'Panel del negocio' })).toBeVisible();
  await expect(page.getByText('Completa tu configuración')).toBeVisible();
});

test('HU-03/04 login incorrecto, login correcto y logout', async ({ page }) => {
  await login(page, provider.email, 'Incorrecta999');
  await expect(page.getByTestId('alert-error')).toContainText('Correo o contraseña incorrectos');
  await login(page, provider.email);
  await expect(page).toHaveURL(/\/provider$/);
  await logout(page);
  // Tras el logout una ruta protegida redirige al login
  await page.goto('/provider/services');
  await expect(page).toHaveURL(/\/login$/);
});

test('HU-10 recursos, HU-05/06 servicios y asociación de recursos', async ({ page }) => {
  await login(page, provider.email);
  await page.getByRole('link', { name: 'Recursos' }).click();
  await page.getByLabel(field('Nombre')).fill('Silla 1');
  await page.getByLabel('Tipo').fill('Equipo');
  await page.getByLabel('Cantidad').fill('2');
  await page.getByRole('button', { name: 'Registrar recurso' }).click();
  await expectToast(page, 'Silla 1');
  await expect(page.getByTestId('resources-table')).toContainText('Silla 1');

  // Nombre repetido -> 409 traducido
  await page.getByLabel(field('Nombre')).fill('Silla 1');
  await page.getByLabel('Tipo').fill('Equipo');
  await page.getByRole('button', { name: 'Registrar recurso' }).click();
  await expect(page.getByTestId('alert-error')).toContainText('Ya tienes un recurso con ese nombre');

  await page.getByRole('link', { name: 'Servicios' }).click();
  for (const name of [SERVICE, SERVICE_2]) {
    await page.getByRole('button', { name: /Nuevo servicio|Crear el primero/ }).first().click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('Nombre').fill(name);
    await dialog.getByLabel('Descripción').fill('Servicio de prueba E2E');
    await dialog.getByLabel('Duración (minutos)').fill('30');
    await dialog.getByLabel('Precio (COP)').fill('25000');
    await dialog.getByRole('button', { name: 'Crear servicio' }).click();
    await expectToast(page, name);
  }
  const table = page.getByTestId('services-table');
  await expect(table).toContainText(SERVICE);

  // HU-06 editar
  const row = table.getByRole('row').filter({ hasText: SERVICE });
  await row.getByRole('button', { name: 'Editar' }).click();
  await page.getByRole('dialog').getByLabel('Precio (COP)').fill('30000');
  await page.getByRole('dialog').getByRole('button', { name: 'Guardar cambios' }).click();
  await expectToast(page, 'actualizado');
  await expect(row).toContainText('30.000');

  // Asociar recurso
  await row.getByRole('button', { name: 'Recursos' }).click();
  await page.getByRole('dialog').getByRole('checkbox').first().check();
  await page.getByRole('dialog').getByRole('button', { name: /Asociar/ }).click();
  await expectToast(page, 'Recursos asociados');
  await expect(row).toContainText('Silla 1');
});

test('HU-08 horarios para toda la semana y HU-09 bloqueo', async ({ page }) => {
  await login(page, provider.email);
  await page.getByRole('link', { name: 'Horarios' }).click();
  for (const day of DAYS) {
    await page.getByLabel('Día').selectOption({ label: day });
    await page.getByLabel('Desde').fill('00:00');
    await page.getByLabel('Hasta').fill('23:30');
    await page.getByRole('button', { name: 'Agregar franja' }).click();
    await expectToast(page, 'Franja');
  }
  await expect(page.getByTestId('schedule-window')).toHaveCount(7);

  // Franja solapada -> 409
  await page.getByLabel('Día').selectOption({ label: 'Lunes' });
  await page.getByLabel('Desde').fill('10:00');
  await page.getByLabel('Hasta').fill('11:00');
  await page.getByRole('button', { name: 'Agregar franja' }).click();
  await expect(page.getByTestId('alert-error')).toContainText('se cruza');

  // Bloqueo en la próxima fecha del lunes
  await page.getByTestId('schedule-window').first().getByRole('button', { name: '+ Bloqueo' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Desde').fill('12:00');
  await dialog.getByLabel('Hasta').fill('13:00');
  await dialog.getByLabel('Motivo').fill('Almuerzo E2E');
  await dialog.getByRole('button', { name: 'Registrar bloqueo' }).click();
  await expectToast(page, 'Bloqueo registrado');
  await expect(page.getByText('Almuerzo E2E')).toBeVisible();
});

test('HU-01 cliente se registra, HU-11 ve disponibilidad y HU-12 reserva', async ({ page }) => {
  await registerClient(page, client);
  await page.getByPlaceholder(/Buscar por servicio/).fill(run);
  const card = page.getByTestId('catalog-item').filter({ hasText: SERVICE });
  await expect(card).toBeVisible();
  await card.getByRole('link', { name: 'Reservar' }).click();
  await expect(page.getByTestId('slot').first()).toBeVisible();
  await page.getByTestId('slot').first().click();
  await page.getByRole('button', { name: 'Confirmar reserva' }).click();
  await expect(page.getByTestId('booking-confirmation')).toContainText(provider.business);

  // Ese hueco ya no está libre: otra reserva en el mismo horario falla con 409
  await page.getByRole('button', { name: 'Ver mis reservas' }).click();
  await expect(page.getByTestId('reservation-row')).toHaveCount(1);
});

test('HU-16 reprogramar y HU-14 cancelar (cliente), HU-17 historial', async ({ page }) => {
  await login(page, client.email);
  await page.getByRole('link', { name: 'Mis reservas' }).click();
  const row = page.getByTestId('reservation-row').first();
  const before = await row.textContent();

  await row.getByRole('button', { name: 'Reprogramar' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByTestId('slot').nth(2).click();
  await dialog.getByRole('button', { name: 'Confirmar nuevo horario' }).click();
  await expectToast(page, 'reprogramada');
  await expect(page.getByTestId('reservation-row').first()).not.toHaveText(before ?? '');

  await page.getByTestId('reservation-row').first().getByRole('button', { name: 'Cancelar' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Confirmar cancelación' }).click();
  await expectToast(page, 'cancelada');
  await expect(page.getByTestId('reservation-row').first()).toContainText('Cancelada por el cliente');

  // Una nueva reserva para que el proveedor tenga agenda
  await page.getByRole('link', { name: 'Catálogo' }).click();
  await page.getByTestId('catalog-item').filter({ hasText: SERVICE }).getByRole('link', { name: 'Reservar' }).click();
  await page.getByTestId('slot').nth(4).click();
  await page.getByRole('button', { name: 'Confirmar reserva' }).click();
  await expect(page.getByTestId('booking-confirmation')).toBeVisible();

  await page.getByRole('link', { name: 'Historial' }).click();
  await expect(page.getByTestId('history-row')).toHaveCount(2);
  await expect(page.getByTestId('history-table')).toContainText('Pendiente');
  await expect(page.getByTestId('history-table')).toContainText('Cancelada');

  // Un cliente no puede entrar a rutas del proveedor
  await page.goto('/provider/services');
  await expect(page).toHaveURL(/\/client$/);
});

test('HU-13 proveedor reserva a nombre de un cliente y HU-15 cancela con motivo', async ({ page, browser }) => {
  const other = await browser.newPage();
  await registerClient(other, client2);
  await other.close();

  await login(page, provider.email);
  await page.getByRole('link', { name: 'Reservar a cliente' }).click();
  await page.getByLabel('Servicio').selectOption({ label: `${SERVICE_2} · 30 min` });
  await page.getByLabel('Correo del cliente').fill('no-existe@e2e.test');
  await page.getByRole('button', { name: 'Buscar' }).click();
  await expect(page.getByTestId('alert-error')).toContainText('No se encontró');
  await page.getByLabel('Correo del cliente').fill(client2.email);
  await page.getByRole('button', { name: 'Buscar' }).click();
  await expect(page.getByTestId('client-name')).toHaveText(`${client2.firstName} ${client2.lastName}`);
  await page.getByTestId('slot').nth(1).click();
  await page.getByRole('button', { name: 'Crear reserva' }).click();
  await expect(page.getByTestId('booking-confirmation')).toContainText(client2.firstName);

  await page.getByRole('link', { name: 'Ver agenda' }).click();
  await expect(page.getByTestId('reservations-table')).toContainText(`${client.firstName} ${client.lastName}`);
  await expect(page.getByTestId('reservations-table')).toContainText(`${client2.firstName} ${client2.lastName}`);

  const row = page.getByTestId('reservation-row').filter({ hasText: client2.firstName });
  await row.getByRole('button', { name: 'Cancelar' }).click();
  const dialog = page.getByRole('dialog');
  const reason = dialog.getByLabel('Motivo de la cancelación');
  // Vacío: lo frena la validación nativa del navegador (campo required) y el modal sigue abierto
  await dialog.getByRole('button', { name: 'Confirmar cancelación' }).click();
  expect(await reason.evaluate((el: HTMLTextAreaElement) => el.validity.valueMissing)).toBe(true);
  // Solo espacios: lo frena la validación de la app (igual que strip() en la API)
  await reason.fill('   ');
  await dialog.getByRole('button', { name: 'Confirmar cancelación' }).click();
  await expect(dialog.getByTestId('alert-error')).toContainText('motivo');
  await dialog.getByLabel('Motivo de la cancelación').fill('El barbero está enfermo');
  await dialog.getByRole('button', { name: 'Confirmar cancelación' }).click();
  await expectToast(page, 'cancelada');
  await expect(row).toContainText('Cancelada por el proveedor');
});

test('HU-18 historial del negocio y HU-19/20/21 reportes', async ({ page }) => {
  await login(page, provider.email);
  await page.getByRole('link', { name: 'Historial' }).click();
  await expect(page.getByTestId('history-row')).toHaveCount(3);

  await page.getByRole('link', { name: 'Reportes' }).click();
  const occupancy = page.getByTestId('occupancy-report');
  await expect(occupancy).toContainText('Tasa de ocupación');
  await expect(occupancy).toContainText(SERVICE);

  await page.getByRole('tab', { name: /Demanda/ }).click();
  await expect(page.getByTestId('demand-report')).toContainText(SERVICE);

  await page.getByRole('tab', { name: /Cancelaciones/ }).click();
  const cancellations = page.getByTestId('cancellations-report');
  await expect(cancellations.locator('.stat-card').first()).toContainText('2');

  // Filtro por servicio
  await page.getByLabel('Servicio').selectOption({ label: SERVICE_2 });
  await expect(cancellations.locator('.stat-card').first()).toContainText('1');

  // Rango inválido (> 31 días) validado en el cliente
  await page.locator('input[type="date"]').first().fill('2026-01-01');
  await page.locator('input[type="date"]').nth(1).fill('2026-03-01');
  await page.getByRole('button', { name: 'Consultar' }).click();
  await expect(page.getByText('no puede superar 31 días')).toBeVisible();
});

test('HU-07 desactivar servicio lo quita del catálogo', async ({ page }) => {
  await login(page, provider.email);
  await page.getByRole('link', { name: 'Servicios' }).click();
  const row = page.getByTestId('services-table').getByRole('row').filter({ hasText: SERVICE_2 });
  await row.getByRole('button', { name: 'Desactivar' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Desactivar' }).click();
  await expectToast(page, 'inactivo');
  await expect(row).toContainText('Inactivo');
  await logout(page);

  await login(page, client.email);
  await page.getByRole('link', { name: 'Catálogo' }).click();
  await page.getByPlaceholder(/Buscar por servicio/).fill(run);
  await expect(page.getByTestId('catalog-item').filter({ hasText: SERVICE })).toBeVisible();
  await expect(page.getByTestId('catalog-item').filter({ hasText: SERVICE_2 })).toHaveCount(0);
});

test('la sesión sobrevive a recargar la página (F5) en una ruta protegida', async ({ page }) => {
  await login(page, client.email);
  await expect(page).toHaveURL(/\/client$/);
  await page.goto('/client/reservations');
  await expect(page.getByTestId('reservation-row').first()).toBeVisible();
  await page.reload();
  await expect(page.getByTestId('reservation-row').first()).toBeVisible();
  await expect(page.getByTestId('alert-error')).toHaveCount(0);
});
