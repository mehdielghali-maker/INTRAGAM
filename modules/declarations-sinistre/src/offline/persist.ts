// Stockage PERSISTANT : demande au navigateur de ne pas évincer les données (photos).
// Si refusé, l'appelant avertit l'utilisateur (photos moins protégées → synchroniser vite).

export async function demanderPersistance(): Promise<boolean> {
  if (!navigator.storage?.persist) {
    return false;
  }
  if (await navigator.storage.persisted()) {
    return true;
  }
  return navigator.storage.persist();
}
