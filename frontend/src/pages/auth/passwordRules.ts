/** Mismas reglas que la API: 8-72 caracteres, al menos una letra y un dígito. */
export function validatePassword(password: string): string | null {
  if (password.length < 8) return 'La contraseña debe tener al menos 8 caracteres.';
  if (password.length > 72) return 'La contraseña no puede superar 72 caracteres.';
  if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) {
    return 'La contraseña debe contener al menos una letra y un número.';
  }
  return null;
}

export const PASSWORD_HINT = 'Mínimo 8 caracteres, con al menos una letra y un número.';
